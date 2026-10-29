# -*- coding: utf-8 -*-
"""Классовые сеты: по четыре части на класс, каждая — с конкретного хранителя этапов 1–3.

Собирается весь набор к концу третьего этапа, и ради него есть смысл убивать именно
этих хранителей. Две части дают проценты под роль и ресурс класса, четыре — ещё и
пассивное свойство, подобранное под саму идею класса.

Подключается из gen_gear_content.py: он берёт отсюда готовые строки предметов и наборов.
"""

# четыре части: ключ слота, фигура для текстуры, с кого падает, уровень предмета, как зовётся
PIECES = [
    ('necklace', 'rune', 'gatekeeper', 8, 'Знак', 'Sign'),
    ('bracelet', 'bracelet', 'lich_king', 14, 'Наруч', 'Bracer'),
    ('belt', 'totem', 'dark_witch', 22, 'Пояс', 'Belt'),
    ('charm', 'idol', 'deep_priestess', 30, 'Оберег', 'Charm'),
]

# палитра по ресурсу класса: металл, камень, второй металл
PALETTE = {
    'mana': ('silver', 'ice', 'bone'),
    'rage': ('iron', 'blood', 'black'),
    'charges': ('iron', 'onyx', 'leather'),
    'form': ('bone', 'green', 'leather'),
    'orbs': ('mithril', 'void', 'black'),
    'health': ('obsidian', 'blood', 'black'),
}

# что даёт роль за две части
ROLE_BONUS = {
    'TANK': {'resist': 4},
    'DAMAGE': {'crit_chance': 5},
    'SUPPORT': {'heal_power': 10},
}

# что даёт ресурс за две части — ровно то, чего этому ресурсу не хватает
RESOURCE_BONUS = {
    'mana': {'mana_regen': 12},
    'rage': {'lifesteal': 4},
    'charges': {'cooldown': 8},
    'form': {'move_speed': 6},
    'orbs': {'spell_power': 10},
    'health': {'health_regen': 1.5},
}

# что даёт роль за четыре части
ROLE_BONUS_4 = {
    'TANK': {'resist': 6, 'res_magic': 12},
    'DAMAGE': {'crit_damage': 25, 'dmg_elite': 10},
    'SUPPORT': {'heal_power': 15, 'cooldown': 8},
}

# пассивное свойство за полный набор — под идею класса
PASSIVE = {
    # танки
    'warden': 'guard_aura',          # барьеры прикрывают отряд
    'bloodknight': 'soul_eater',     # живёт чужой смертью
    'lawgiver': 'focus_aura',        # законы ускоряют отряд
    'totemist': 'thorns',            # столбы отвечают за него
    'symbiote': 'regeneration',      # держит чужую боль и затягивает её
    'graviton': 'mana_shield',       # тяжесть гасит удар
    'mirrorknight': 'phoenix',       # отражает даже смертельный удар
    # бойцы
    'swift': 'momentum',             # сила от движения
    'berserker': 'berserker',        # злее на грани
    'monk': 'arcane_echo',           # печать иногда не уходит
    'summoner': 'mana_aura',         # души кормят отряд
    'werebeast': 'fury_swipes',      # рвёт одну цель
    'machinist': 'chain_spark',      # разряд с механизмов
    'plaguelord': 'venom',           # всё, чего касается, травит
    'spirit': 'night_eye',           # видит в темноте чужими глазами
    'shade': 'executioner',          # добивает раненых
    'astromancer': 'arcane_echo',    # сочетание иногда держится
    'sniper': 'giant_slayer',        # берёт тех, кто выше уровнем
    'executioner': 'soul_eater',     # каждая голова возвращает силы
    'pyromancer': 'flame_touch',     # всё горит
    # поддержка
    'healer': 'regeneration',
    'chronologist': 'arcane_echo',
    'skald': 'war_aura',
    'wellspring': 'mana_aura',
    'darkpriest': 'soul_eater',
    'oracle': 'focus_aura',
    'alchemist': 'treasure_hunter',
}


def build(classes):
    """classes: [(key, ru, en, role, resource)] → (список предметов, список наборов)."""
    items, sets = [], []
    for key, ru, en, role, res in classes:
        pal = PALETTE.get(res, PALETTE['mana'])
        for slot, shape, boss, tier, ru_part, en_part in PIECES:
            items.append(dict(
                id='set_%s_%s' % (key, slot),
                ru='%s %s' % (ru_part, ru.lower()),
                en="%s of the %s" % (en_part, en),
                kind=slot, shape=shape, pal=pal, tier=tier, boss=boss,
                set=key, roles=[role.lower()],
            ))
        two = dict(ROLE_BONUS[role])
        two.update(RESOURCE_BONUS.get(res, {}))
        four = dict(ROLE_BONUS_4[role])
        sets.append(dict(
            id=key,
            title='Облачение: %s' % ru,
            tiers=[dict(pieces=2, mods=two),
                   dict(pieces=4, mods=four, passive=PASSIVE.get(key))],
        ))
    return items, sets
