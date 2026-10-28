"""
Классы и навыки Системы: данные в tools/classes/*.py → src/main/resources/litrpg_classes.json,
названия и описания → lang (ru_ru/en_us), сводка → docs/CLASSES.md.
Запуск: python tools/gen_classes.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from classes import dsl  # noqa: E402
from classes import legacy_skills, legacy_classes, tanks, damage_melee, damage_ranged, damage_magic, support, hidden, invokers  # noqa: E402,F401
from classes import pve  # noqa: E402,F401  — переработка под PvE (2.17): правки поверх классов
from classes import v3  # noqa: E402,F401  — 3.0: 50 классов по 16 навыков + динамичные навыки
from classes import v4  # noqa: E402,F401  — 4.0: 27 классов по 12 навыков, у каждого свой ресурс

ROOT = os.path.join(os.path.dirname(__file__), '..')
RES = os.path.join(ROOT, 'src', 'main', 'resources')
LANG = os.path.join(RES, 'assets', 'litrpg', 'lang')

NOVICE = ['mend', 'magic_missile']
NOVICE_UNLOCK = [1, 1]
# уровни открытия навыков класса по порядку в списке (12 навыков, с 10 по 46 уровень)
SKILLS_PER_CLASS = 12
UNLOCK = [10, 10, 10, 13, 16, 19, 22, 26, 30, 34, 40, 46]
ROLE_RU = {'TANK': 'Танк', 'DAMAGE': 'Боец', 'SUPPORT': 'Поддержка'}


def check():
    errors = []
    used = set(NOVICE) | {'dash'}
    for key in dsl.ORDER:
        c = dsl.CLASSES[key]
        if c.invoke:
            sk = c.invoke['spheres'] + [c.invoke['cast']] + [x[1] for x in c.invoke['combos']]
        else:
            sk = c.skills
            if len(sk) != SKILLS_PER_CLASS:
                errors.append(f'{key}: {len(sk)} навыков (нужно {SKILLS_PER_CLASS})')
            if len(set(sk)) != len(sk):
                errors.append(f'{key}: повтор навыка')
        for s in sk:
            if s not in dsl.SKILLS:
                errors.append(f'{key}: нет навыка {s}')
            used.add(s)
        if not c.styles:
            errors.append(f'{key}: нет стилей')
    unused = [k for k in dsl.SKILLS if k not in used]
    return errors, unused


def main():
    errors, unused = check()
    if errors:
        print('\n'.join(errors))
        sys.exit(1)
    unused_set = set(unused)
    skills = [s.json() for s in dsl.SKILLS.values() if s.key not in unused_set]
    classes = []
    for key in dsl.ORDER:
        c = dsl.CLASSES[key]
        if c.invoke:
            o = c.json(UNLOCK)
            o['skills'] = c.invoke['spheres'] + [c.invoke['cast']]
            o['unlock'] = [10, 10, 10, 10]
            classes.append(o)
        else:
            classes.append(c.json(UNLOCK))
    data = {'novice': NOVICE, 'noviceUnlock': NOVICE_UNLOCK, 'skills': skills, 'classes': classes, 'aliases': dsl.ALIASES}
    with open(os.path.join(RES, 'litrpg_classes.json'), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, separators=(',', ':'))

    ru_path, en_path = os.path.join(LANG, 'ru_ru.json'), os.path.join(LANG, 'en_us.json')
    ru = json.load(open(ru_path, encoding='utf-8'))
    en = json.load(open(en_path, encoding='utf-8'))
    for d in (ru, en):
        for k in [k for k in d if k.startswith('ability.litrpg.') or k.startswith('class.litrpg.')]:
            del d[k]
    for s in dsl.SKILLS.values():
        if s.key in unused_set:
            continue
        desc = s.desc()
        ru[f'ability.litrpg.{s.key}'], ru[f'ability.litrpg.{s.key}.desc'] = s.ru, desc
        en[f'ability.litrpg.{s.key}'], en[f'ability.litrpg.{s.key}.desc'] = s.en, desc
    for key in dsl.ORDER:
        c = dsl.CLASSES[key]
        for d, name in ((ru, c.ru), (en, c.en)):
            d[f'class.litrpg.{key}'] = name
            d[f'class.litrpg.{key}.desc'] = c.desc
            d[f'class.litrpg.{key}.trait'] = c.trait_name
            d[f'class.litrpg.{key}.trait.desc'] = c.trait_text()
            if c.hint:
                d[f'class.litrpg.{key}.hint'] = c.hint
    json.dump(ru, open(ru_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

    # сводка для владельца
    os.makedirs(os.path.join(ROOT, 'docs'), exist_ok=True)
    lines = ['# Классы LitRPG', '']
    for role in ('TANK', 'DAMAGE', 'SUPPORT'):
        lst = [dsl.CLASSES[k] for k in dsl.ORDER if dsl.CLASSES[k].role == role and not dsl.CLASSES[k].hidden]
        lines += [f'## {ROLE_RU[role]} ({len(lst)})', '']
        lines += [f'- **{c.ru}** — {c.desc}' for c in lst]
        lines.append('')
    hid = [dsl.CLASSES[k] for k in dsl.ORDER if dsl.CLASSES[k].hidden]
    lines += [f'## Скрытые ({len(hid)})', '']
    lines += [f'- **{c.ru}** ({ROLE_RU[c.role]}) — {c.desc} *Как открыть: {c.hint}*' for c in hid]
    open(os.path.join(ROOT, 'docs', 'CLASSES.md'), 'w', encoding='utf-8').write('\n'.join(lines) + '\n')

    roles = {r: sum(1 for k in dsl.ORDER if dsl.CLASSES[k].role == r and not dsl.CLASSES[k].hidden) for r in ROLE_RU}
    print(f'classes {len(dsl.ORDER)} ({roles}, hidden {len(hid)}), skills {len(dsl.SKILLS)}')
    print(f'в игре навыков: {len(skills)} (не используются и не выгружены: {len(unused)})')


if __name__ == '__main__':
    main()
