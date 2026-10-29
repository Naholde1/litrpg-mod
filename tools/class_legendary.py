# -*- coding: utf-8 -*-
"""По одной легендарной вещи на класс — с хранителей четвёртого этапа.

Их всего двадцать семь на всю игру, больше легендарного в подземелье нет. Каждая
висит на своём хранителе: танковые — на Големе-плавильщике, бойцовские — на Огненных
близнецах, поддержки — на Пепельном владыке, так что убивать стоит всех троих.
"""

BOSS_BY_ROLE = {'TANK': 'smelter_golem', 'DAMAGE': 'fire_twin_ignis', 'SUPPORT': 'ash_lord'}

# врождённые проценты по роли (чистых характеристик легендарки тоже не дают)
IMPLICIT = {
    'TANK': {'resist': 6, 'health_regen': 2},
    'DAMAGE': {'crit_damage': 20, 'dmg_elite': 12},
    'SUPPORT': {'heal_power': 15, 'cooldown': 8},
}

# оружию нужны урон и скорость
WEAPON = {'sword': (9, -2.4), 'axe': (11, -3.0), 'dagger': (6, -1.6), 'staff': (7, -2.6)}

TIER = 38

# класс → (русское имя, английское, вид, фигура, палитра, пассивка)
# Пассивка здесь всегда другая, чем у классового облачения: собрав и то и другое,
# получаешь два разных свойства, а не одно дважды.
LEGENDARY = {
    # ---- танки: Голем-плавильщик ----
    'warden': ('Эгида Последнего Рубежа', 'Aegis of the Last Line', 'charm', 'idol', ('mithril', 'ice', 'iron'), 'thorns'),
    'bloodknight': ('Кубок Багровой Клятвы', 'Chalice of the Crimson Oath', 'charm', 'crystal', ('obsidian', 'blood', 'black'), 'berserker'),
    'lawgiver': ('Скрижаль Последнего Закона', 'Tablet of the Final Law', 'charm', 'rune', ('gold', 'topaz', 'bone'), 'mana_shield'),
    'totemist': ('Сердце Горы', 'Heart of the Mountain', 'charm', 'totem', ('stone', 'emerald', 'brown'), 'regeneration'),
    'symbiote': ('Узел Общей Крови', 'Knot of Shared Blood', 'necklace', 'rune', ('copper', 'blood', 'leather'), 'phoenix'),
    'graviton': ('Ядро Сингулярности', 'Singularity Core', 'charm', 'crystal', ('ender', 'void', 'black'), 'giant_slayer'),
    'mirrorknight': ('Осколок Истинного Зерцала', 'Shard of the True Mirror', 'charm', 'crystal', ('silver', 'glass', 'white'), 'thorns'),
    # ---- бойцы: Огненные близнецы ----
    'swift': ('Парные Серпы Ветра', 'Twin Sickles of the Wind', 'dagger', 'claw', ('mithril', 'emerald', 'leather'), 'fury_swipes'),
    'berserker': ('Секира Безумия', 'Axe of Madness', 'axe', 'axe', ('iron', 'blood', 'black'), 'executioner'),
    'monk': ('Печать Тысячи Ладоней', 'Seal of a Thousand Palms', 'charm', 'rune', ('gold', 'amethyst', 'wood'), 'chain_spark'),
    'summoner': ('Череп Первого Слуги', 'Skull of the First Thrall', 'charm', 'skull', ('bone', 'void', 'black'), 'soul_eater'),
    'werebeast': ('Клык Первобытного', 'Primal Fang', 'charm', 'fang', ('beast', 'blood', 'brown'), 'momentum'),
    'machinist': ('Чертёж Вечного Станка', 'Blueprint of the Eternal Engine', 'charm', 'rune', ('copper', 'topaz', 'iron'), 'treasure_hunter'),
    'plaguelord': ('Сосуд Первой Заразы', 'Vessel of the First Plague', 'charm', 'crystal', ('green', 'green', 'bone'), 'regeneration'),
    'spirit': ('Пустая Маска', 'The Empty Mask', 'charm', 'eye', ('white', 'ender', 'bone'), 'mana_shield'),
    'shade': ('Нож, Которого Не Было', "The Knife That Wasn't", 'dagger', 'dagger', ('black', 'void', 'leather'), 'venom'),
    'astromancer': ('Четвёртая Сфера', 'The Fourth Orb', 'charm', 'crystal', ('mithril', 'star', 'blue'), 'chain_spark'),
    'sniper': ('Глаз, Что Не Моргает', 'The Unblinking Eye', 'charm', 'eye', ('iron', 'sapphire', 'leather'), 'executioner'),
    'executioner': ('Плаха', 'The Block', 'axe', 'axe', ('black', 'blood', 'wood'), 'fury_swipes'),
    'pyromancer': ('Уголь Первого Костра', 'Ember of the First Fire', 'charm', 'crystal', ('fire', 'fire', 'black'), 'fireproof'),
    # ---- поддержка: Пепельный владыка ----
    'healer': ('Руки, Что Не Дрожат', 'Hands That Do Not Shake', 'hands', 'gloves', ('white', 'pearl', 'leather'), 'phoenix'),
    'chronologist': ('Песок, Что Течёт Вспять', 'Sand That Runs Backward', 'charm', 'crystal', ('gold', 'glass', 'bone'), 'focus_aura'),
    'skald': ('Струна, Что Не Рвётся', 'The String That Holds', 'charm', 'bell', ('gold', 'ruby', 'wood'), 'mana_aura'),
    'wellspring': ('Неиссякаемый Сосуд', 'The Unfailing Vessel', 'charm', 'shell', ('silver', 'sapphire', 'white'), 'mana_shield'),
    'darkpriest': ('Чаша Чужой Боли', "Cup of Another's Pain", 'charm', 'crystal', ('obsidian', 'amethyst', 'black'), 'thorns'),
    'oracle': ('Глаз Грядущего', 'Eye of What Comes', 'charm', 'eye', ('gold', 'star', 'white'), 'night_eye'),
    'alchemist': ('Фляга Без Дна', 'The Bottomless Flask', 'charm', 'shell', ('copper', 'emerald', 'leather'), 'regeneration'),
}


def build(classes):
    """classes: [(key, ru, en, role, resource)] → список предметов для генератора."""
    out = []
    for key, ru, en, role, res in classes:
        row = LEGENDARY.get(key)
        if row is None:
            continue
        rname, ename, kind, shape, pal, passive = row
        item = dict(id='legend_%s' % key, ru=rname, en=ename, kind=kind, shape=shape, pal=pal,
                    tier=TIER, boss=BOSS_BY_ROLE[role], cls=key, passive=passive,
                    implicit=dict(IMPLICIT[role]), roles=[role.lower()])
        if kind in WEAPON:
            item['damage'], item['speed'] = WEAPON[kind]
        out.append(item)
    return out
