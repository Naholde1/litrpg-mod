"""
Ревизия классов для PvE (бои с хранителями): у каждого класса должна быть польза своей роли.
  Боец:      урон по цели (ST), урон по площади (AOE), польза группе (прерывание/контроль боссов, баффы, метки),
             выживаемость (защита/уход/лечение себя).
  Поддержка: сильное лечение (≥2 лечащих навыка), спасение (воскрешение, оберег, щит, снижение урона), бафф.
  Танк:      провокация, личная защита, угроза по площади, полезность группе.
Запуск: python tools/class_audit.py [--all]  → сводка проблем.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from classes import dsl  # noqa: E402
from classes import legacy_skills, legacy_classes, tanks, damage_melee, damage_ranged, damage_magic, support, hidden, invokers  # noqa: E402,F401
if '--before' not in sys.argv:
    from classes import pve  # noqa: E402,F401

ST = {'STRIKE', 'MULTI_STRIKE', 'BOLT', 'LIGHTNING', 'BEAM', 'BARRAGE', 'ARROWS', 'SHADOW_STEP', 'DASH_STRIKE', 'STEAL', 'CHAIN',
      'INTERRUPT'}
AOE = {'NOVA', 'MULTI_NOVA', 'METEOR', 'ZONE', 'ZONE_AT', 'ARROW_RAIN', 'CONE', 'GLOBAL', 'LEAP_SLAM', 'CHAIN', 'VORTEX', 'MINE',
       'LAUNCH', 'BEAM'}
HEAL = {'HEAL', 'HEAL_TARGET', 'TETHER'}
SAVE = {'WARD', 'SHIELD', 'REZ'}
CC = {'STUN', 'INTERRUPT', 'PUSH', 'LAUNCH', 'STASIS'}
DEF_SELF = {'HEAL_SELF', 'RECALL', 'DASH', 'BLINK', 'STEALTH', 'SHADOW_STEP', 'GRAPPLE'}


def group_fx(s):
    return any(n in ('might', 'bulwark', 'str', 'res', 'abs', 'regen', 'haste') for *_, n in s.me) and s.kind in ('BUFF_ALLIES', 'TETHER', 'ZONE', 'ZONE_AT', 'RECALL')


def audit(c):
    sk = [dsl.SKILLS[k] for k in c.skills]
    kinds = [s.kind for s in sk]
    dmg = [s for s in sk if s.pow > 0 and s.kind not in HEAL | SAVE | {'HEAL_SELF', 'RECALL', 'SHIELD', 'SACRIFICE', 'MEDITATE'}]
    st = any(s.kind in ST and s.pow >= 0.9 for s in sk)
    aoe = any(s.kind in AOE and s.pow > 0 for s in sk)
    util = any(s.kind in CC or s.kind in ('MARK', 'REVEAL') or group_fx(s) or 'group' in s.flags
               or any(n == 'vuln' for *_, n in s.foe) for s in sk)
    surv = any(s.kind in DEF_SELF or (s.kind == 'BUFF_SELF' and any(n in ('res', 'abs') for *_, n in s.me)) for s in sk)
    heals = [s for s in sk if s.kind in HEAL or (s.kind in ('ZONE', 'ZONE_AT') and (s.pow < 0 or s.el in ('HOLY', 'NATURE')))]
    save = any(s.kind in SAVE or (s.kind == 'BUFF_ALLIES' and any(n in ('res', 'abs', 'bulwark') for *_, n in s.me)) for s in sk)
    taunt = 'TAUNT' in kinds
    problems = []
    if c.role == 'DAMAGE':
        if not st: problems.append('нет сильного урона по цели')
        if not aoe: problems.append('нет урона по площади')
        if not util: problems.append('нет пользы группе')
        if not surv: problems.append('нет выживаемости')
        if len(dmg) < 4: problems.append(f'мало атакующих навыков ({len(dmg)})')
    elif c.role == 'SUPPORT':
        if len(heals) < 2: problems.append(f'мало лечения ({len(heals)})')
        if not save: problems.append('нет спасения (оберег/щит/воскрешение/снижение урона)')
        if not util and not any(s.kind == 'BUFF_ALLIES' for s in sk): problems.append('нет баффа группе')
    else:
        if not taunt: problems.append('нет провокации')
        if not any(s.kind == 'BUFF_SELF' and any(n in ('res', 'abs') for *_, n in s.me) for s in sk) and 'reflect' not in sum((s.flags for s in sk), []):
            problems.append('нет личной защиты')
        if not aoe: problems.append('нет угрозы по площади')
    return problems


def score(c):
    """Насколько класс далёк от нормы (для автоправки): проблемы + нехватка лечения у поддержки."""
    p = audit(c)
    if c.role == 'SUPPORT':
        heals = sum(1 for k in c.skills if dsl.SKILLS[k].kind in HEAL
                    or (dsl.SKILLS[k].kind in ('ZONE', 'ZONE_AT') and (dsl.SKILLS[k].pow < 0 or dsl.SKILLS[k].el in ('HOLY', 'NATURE'))))
        return len(p) + max(0, 2 - heals)
    return len(p)


def main():
    bad = 0
    by_role = {}
    for key in dsl.ORDER:
        c = dsl.CLASSES[key]
        if c.invoke:
            continue
        p = audit(c)
        by_role.setdefault(c.role, [0, 0])
        by_role[c.role][0] += 1
        if p:
            bad += 1
            by_role[c.role][1] += 1
            print(f'{c.role:8} {key:22} {c.ru:28} — ' + '; '.join(p))
        elif '--all' in sys.argv:
            print(f'{c.role:8} {key:22} {c.ru:28} ok')
    print('проблемных', bad, 'из', len(dsl.ORDER), by_role)


if __name__ == '__main__':
    main()
