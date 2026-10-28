"""
Классы 4.0: двадцать семь классов по двенадцать навыков, у каждого свой ресурс.

Пятьдесят классов версии 3.0 были вариациями друг друга: шесть стихийных магов с одинаковой
механикой и разным цветом частиц, четыре лучника, пять «просто крепких» танков. Здесь каждый
класс отличается тем, как он играет, а не названием навыков.

Подключается последним (gen_classes.py): стирает прежний состав, ставит новый и уводит
все старые ключи в ALIASES, чтобы у тех, кто уже выбрал класс, он сменился на ближайший.
"""
from .dsl import CLASSES, ORDER, ALIASES

# ---------------------------------------------------------------- старое → новое
# Чем заменить класс у того, кто уже играет. Роль сохраняется.
REPLACE = {
    # танки
    'warrior': 'warden', 'guardian': 'warden', 'ironclad': 'warden', 'phalangite': 'warden', 'bastion': 'warden',
    'paladin': 'lawgiver', 'templar': 'lawgiver',
    'death_knight': 'bloodknight', 'blood_knight': 'bloodknight', 'dragon_knight': 'bloodknight',
    'stone_warden': 'totemist', 'ursine_guardian': 'totemist', 'treant': 'totemist',
    'frost_giant': 'graviton', 'graviton': 'graviton',
    # бойцы
    'berserker': 'berserker', 'executioner': 'executioner', 'monk': 'monk',
    'blademaster': 'swift', 'wind_ranger': 'swift', 'chronorunner': 'swift', 'demon_hunter': 'swift',
    'assassin': 'shade', 'shadowblade': 'shade',
    'ranger': 'sniper', 'sniper': 'sniper', 'frost_archer': 'sniper',
    'gunslinger': 'machinist', 'bomber': 'machinist',
    'beastmaster': 'werebeast',
    'necromancer': 'summoner',
    'warlock': 'plaguelord', 'venomancer': 'plaguelord',
    'mage': 'astromancer', 'cryomancer': 'astromancer', 'stormcaller': 'astromancer', 'astromancer': 'astromancer',
    'pyromancer': 'pyromancer', 'spellblade': 'pyromancer',
    # поддержка
    'cleric': 'healer', 'healer': 'healer', 'guardian_angel': 'healer', 'sun_priestess': 'healer',
    'chronomancer': 'chronologist',
    'bard': 'skald', 'skald': 'skald',
    'druid': 'wellspring', 'shaman': 'wellspring', 'tidecaller': 'wellspring',
    'shadow_priest': 'darkpriest',
    'oracle': 'oracle', 'moon_priestess': 'oracle',
    'alchemist': 'alchemist',
}

# на что менять, если класса нет в списке (по роли)
BY_ROLE = {'TANK': 'warden', 'DAMAGE': 'berserker', 'SUPPORT': 'healer'}


def build():
    old_roles = {k: c.role for k, c in CLASSES.items()}
    old_keys = list(ORDER)
    CLASSES.clear()
    ORDER.clear()

    # составы объявляются только теперь, когда прежние ключи освобождены
    from . import v4_tanks, v4_damage, v4_support  # noqa: F401

    new_keys = set(ORDER)
    for k in old_keys:
        target = REPLACE.get(k) or BY_ROLE[old_roles.get(k, 'DAMAGE')]
        if target not in new_keys:
            target = BY_ROLE[old_roles.get(k, 'DAMAGE')]
        ALIASES[k] = target
    # прежние замены вели на классы 3.0 — довести их до новых
    for k, v in list(ALIASES.items()):
        seen = set()
        while v in ALIASES and v not in new_keys and v not in seen:
            seen.add(v)
            v = ALIASES[v]
        if v not in new_keys:
            v = BY_ROLE[old_roles.get(v, 'DAMAGE')]
        ALIASES[k] = v
    for k in new_keys:
        ALIASES.pop(k, None)


build()
