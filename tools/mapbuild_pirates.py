"""
Пиратские корабли в открытом море: чёрные паруса, «Весёлый Роджер», пушечные порты, команда головорезов
и капитан в каюте, сундук с награбленным в трюме. Корабль стоит на якоре — воды под килем хватает (≥ 6 блоков).
"""
import random

from mapbuild_lib import City, TList, TInt
from mapbuild_towns2 import ship


def pirate_ship(seed):
    rnd = random.Random(seed * 13 + 5)
    S = 48
    wl = 12                         # уровень воды в шаблоне (y=63 мира)
    b = City(S, 48, S, 'pirates%d' % seed, 'human', ground=wl + 1)
    L = 32
    x0, zc = 6, S // 2
    bow = 1 if seed % 2 == 0 else -1
    sail = 'minecraft:black_wool'
    stripe = rnd.choice(['minecraft:red_wool', 'minecraft:gray_wool', 'minecraft:black_wool'])
    ship(b, x0, zc, wl, L=L, bow=bow, sail=sail, stripe=stripe, flag='minecraft:black_banner')
    deck = wl + 2
    # «Весёлый Роджер»: белый череп на чёрных флагах
    for (x, y, z), (bl, _) in list(b.blocks.items()):
        if bl == 'minecraft:black_banner':
            b.nbt[(x, y, z)] = {'Patterns': TList(10, [{'Pattern': 'sku', 'Color': TInt(0)}, {'Pattern': 'bo', 'Color': TInt(14)}])}
    # сундуки: в трюме — награбленное, в каюте — капитанский
    for pos, v in list(b.nbt.items()):
        lt = v.get('LootTable', '')
        if lt == 'litrpg:chests/city_barrel':
            v['LootTable'] = 'litrpg:chests/pirate_treasure'
        elif lt == 'litrpg:chests/city_home':
            v['LootTable'] = 'litrpg:chests/pirate_captain'
    # пушки вдоль бортов: раструбы-раздатчики в портах
    for i in range(6, L - 8, 4):
        x = x0 + i if bow > 0 else x0 + L - 1 - i
        for side in (-1, 1):
            for dz in range(5, 1, -1):
                z = zc + side * dz
                if (x, deck + 1, z) in b.blocks and 'fence' in b.blocks[(x, deck + 1, z)][0]:
                    b.set(x, deck + 1, z, 'minecraft:dispenser', facing='south' if side > 0 else 'north', triggered='false')
                    b.set(x, deck + 1, z - side, 'minecraft:polished_blackstone_slab', type='bottom', waterlogged='false')
                    break
    # бочки рома и ядра на палубе
    for k in range(4):
        x = x0 + 9 + k * 4 if bow > 0 else x0 + L - 10 - k * 4
        b.set(x, deck + 1, zc + (2 if k % 2 else -2), 'minecraft:barrel', facing='up', open='false')
    b.set(x0 + L // 2, deck + 1, zc - 1, 'minecraft:coal_block')
    # команда: головорезы на палубе, стрелки у бортов, капитан в каюте
    crew = [('litrpg:pirate_cutthroat', 0.45, -1), ('litrpg:pirate_cutthroat', 0.55, 1), ('litrpg:pirate_cutthroat', 0.7, 0),
            ('litrpg:pirate_gunner', 0.35, 2), ('litrpg:pirate_gunner', 0.62, -2)]
    if seed % 2:
        crew.append(('litrpg:pirate_cutthroat', 0.8, 0))
    for eid, u, dz in crew:
        x = x0 + int(L * u) if bow > 0 else x0 + L - 1 - int(L * u)
        b.entities.append((x, deck + 1, zc + dz, eid, {}))
    cx = x0 + 2 if bow > 0 else x0 + L - 3
    b.entities.append((cx, deck + 1, zc, 'litrpg:pirate_captain', {}))
    # шлюпка за кормой
    b.entities.append((x0 - 3 if bow > 0 else x0 + L + 2, wl + 1, zc + 4, 'minecraft:boat', {'Type': 'dark_oak'}))
    b.wl = wl
    return b
