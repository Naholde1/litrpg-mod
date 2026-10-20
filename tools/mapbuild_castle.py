"""
Двор королевского замка (бейли): мощёная дорога от ворот к донжону, конюшня с лошадьми,
кузница-навес, казарма, часовня, плац с мишенями и чучелами, сад с фонтаном, склады и
стога у стен, колодец, дозорные. Всё плотно, как во дворах настоящих замков.
"""
import random

from mapbuild_lib import TByte, TInt


def _fence_line(b, x0, z0, x1, z1, fence):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, b.G, z, fence, waterlogged='false')


def stable(b, x0, z0, w, d, open_side='east'):
    """Конюшня: навес на столбах, стойла с перегородками, сено, кормушки-компостеры, лошади."""
    G = b.G
    x1, z1 = x0 + w - 1, z0 + d - 1
    log = 'minecraft:spruce_log'
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, 'minecraft:coarse_dirt' if (x + z) % 3 else 'minecraft:packed_mud')
            back = x == x0 if open_side == 'east' else x == x1
            side = z in (z0, z1)
            for y in range(G, G + 4):
                if back or side:
                    pillar = z in (z0, z1) and x in (x0, x1) or (back and (z - z0) % 4 == 0)
                    b.set(x, y, z, log if pillar else 'minecraft:spruce_planks', **({'axis': 'y'} if pillar else {}))
                elif (x in (x0, x1)) and (z - z0) % 4 == 0:
                    b.set(x, y, z, log, axis='y')
                else:
                    b.set(x, y, z, 'minecraft:air')
    # стойла: перегородки через 4 блока, кормушки и сено у задней стены
    bx = x0 + 1 if open_side == 'east' else x1 - 1
    step = 1 if open_side == 'east' else -1
    for z in range(z0 + 1, z1):
        if (z - z0) % 4 == 0:
            for k in range(3):
                b.set(bx + step * k, G, z, 'minecraft:spruce_fence', waterlogged='false')
        else:
            b.set(bx, G, z, 'minecraft:hay_block' if (z - z0) % 4 == 2 else 'minecraft:composter', **({'axis': 'y'} if (z - z0) % 4 == 2 else {'level': '6'}))
    # калитки стойл с фасада
    fx = x1 if open_side == 'east' else x0
    for z in range(z0 + 1, z1):
        if (z - z0) % 4 != 0:
            b.set(fx, G, z, 'minecraft:spruce_fence', waterlogged='false')
    b.roof(x0, z0, x1, z1, G + 4, ('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks'))
    for z in range(z0 + 2, z1 - 1, 4):
        b.set((x0 + x1) // 2, G + 3, z, 'minecraft:lantern', hanging='true', waterlogged='false')
        b.entities.append(((x0 + x1) // 2, G, z, 'minecraft:horse', {'Variant': TInt(b.rnd.choice([0, 1, 2, 3, 4, 256, 513, 770])), 'Tame': TByte(1)}))
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)


def forge(b, x0, z0, w, d):
    """Кузница-навес: горн из кирпича с лавой за решёткой, наковальни, бочки с водой, стойки с бронёй."""
    G = b.G
    x1, z1 = x0 + w - 1, z0 + d - 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, 'minecraft:cobblestone' if (x * 7 + z) % 4 else 'minecraft:gravel')
            for y in range(G, G + 4):
                if x in (x0, x1) and z in (z0, z1):
                    b.set(x, y, z, 'minecraft:stripped_dark_oak_log', axis='y')
                else:
                    b.set(x, y, z, 'minecraft:air')
    # горн у северной стены
    for x in range(x0 + 1, x0 + 4):
        b.set(x, G, z0, 'minecraft:bricks')
        b.set(x, G + 1, z0, 'minecraft:bricks')
    b.set(x0 + 2, G, z0, 'minecraft:lava')
    b.set(x0 + 2, G, z0 + 1, 'minecraft:iron_bars', north='true', south='false', east='false', west='false', waterlogged='false')
    b.set(x0 + 1, G, z0 + 1, 'minecraft:blast_furnace', facing='south', lit='true')
    b.set(x0 + 3, G, z0 + 1, 'minecraft:smithing_table')
    for y in range(G + 2, G + 9):
        b.set(x0 + 2, y, z0, 'minecraft:bricks')
    b.set(x0 + 2, G + 9, z0, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    b.set(x0 + 5, G, z0 + 2, 'minecraft:anvil', facing='east')
    b.set(x0 + 5, G, z0 + 4, 'minecraft:damaged_anvil', facing='north')
    b.set(x1 - 1, G, z0 + 1, 'minecraft:water_cauldron', level='3')
    b.set(x1 - 1, G, z1 - 1, 'minecraft:grindstone', face='floor', facing='west')
    b.set(x0 + 1, G, z1 - 1, 'minecraft:barrel', facing='up', open='false')
    b.set(x0 + 1, G + 1, z1 - 1, 'minecraft:barrel', facing='up', open='false')
    for z in (z0 + 3, z1 - 2):
        b.entities.append((x1 - 1, G, z, 'minecraft:armor_stand', {'ShowArms': TByte(1)}))
    b.roof(x0, z0, x1, z1, G + 4, ('minecraft:deepslate_tile_stairs', 'minecraft:deepslate_tile_slab', 'minecraft:deepslate_tiles'))
    b.set((x0 + x1) // 2, G + 3, (z0 + z1) // 2, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.workshops.append(('blacksmith', 'litrpg:human_artisan', x0 + 4, G, z0 + 3))
    b.entities.append((x0 + 4, G, z0 + 3, 'litrpg:human_artisan', {'Craft': 'blacksmith'}))
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)


def chapel(b, x0, z0, w, d):
    """Часовня: стрельчатые витражи, скамьи, алтарь со свечами, колокол в звоннице."""
    G = b.G
    x1, z1 = x0 + w - 1, z0 + d - 1
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G - 1, z, 'minecraft:polished_diorite' if (x + z) % 2 else 'minecraft:smooth_quartz')
            for y in range(G, G + 8):
                if edge:
                    win = (z - z0) % 3 == 1 and x in (x0, x1) and 2 <= y - G <= 5 and z not in (z0, z1)
                    b.set(x, y, z, 'minecraft:light_blue_stained_glass' if win and (y - G) % 2 else
                          'minecraft:yellow_stained_glass' if win else
                          'minecraft:chiseled_stone_bricks' if (x in (x0, x1) and z in (z0, z1)) else 'minecraft:stone_bricks')
                else:
                    b.set(x, y, z, 'minecraft:air')
    b.roof(x0, z0, x1, z1, G + 8, ('minecraft:deepslate_tile_stairs', 'minecraft:deepslate_tile_slab', 'minecraft:deepslate_tiles'))
    cx = (x0 + x1) // 2
    b.door(cx, G, z1, 'south')
    for z in range(z0 + 4, z1 - 1, 2):
        for x in range(x0 + 1, x1):
            if x != cx:
                b.set(x, G, z, 'minecraft:spruce_stairs', facing='north', half='bottom', shape='straight', waterlogged='false')
    for z in range(z0 + 2, z1):
        b.set(cx, G, z, 'minecraft:red_carpet')
    b.set(cx, G, z0 + 1, 'minecraft:quartz_block')
    b.set(cx, G + 1, z0 + 1, 'minecraft:white_candle', candles='4', lit='true', waterlogged='false')
    for dx in (-1, 1):
        b.set(cx + dx, G, z0 + 1, 'minecraft:quartz_stairs', facing='south', half='top', shape='straight', waterlogged='false')
        b.set(cx + dx, G + 1, z0 + 1, 'minecraft:yellow_candle', candles='2', lit='true', waterlogged='false')
    b.set(cx, G + 4, z0, 'minecraft:light_blue_stained_glass')
    b.set(cx, G + 5, z0, 'minecraft:light_blue_stained_glass')
    b.set(cx, G + 6, z0, 'minecraft:light_blue_stained_glass')
    # звонница над входом
    for y in range(G + 8, G + 14):
        for dx in (-1, 1):
            b.set(cx + dx, y, z1, 'minecraft:stone_bricks' if y < G + 13 else 'minecraft:stone_brick_wall')
    b.set(cx, G + 12, z1, 'minecraft:stone_brick_slab', type='top', waterlogged='false')
    b.set(cx, G + 11, z1, 'minecraft:bell', attachment='ceiling', facing='south', powered='false')
    b.set(cx, G + 7, (z0 + z1) // 2, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.entities.append((cx, G, z0 + 3, 'litrpg:human_mage', {}))
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)


def training_yard(b, x0, z0, x1, z1):
    """Плац: песок, мишени на сене, чучела-стойки, стойка с оружием, лавки для зрителей."""
    G = b.G
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, 'minecraft:sand' if (x * 3 + z) % 5 else 'minecraft:coarse_dirt')
    _fence_line(b, x0, z0, x1, z1, 'minecraft:spruce_fence')
    b.set((x0 + x1) // 2, G, z1, 'minecraft:air')
    b.set((x0 + x1) // 2, G, z0, 'minecraft:air')
    # мишени у северного края
    for x in range(x0 + 2, x1 - 1, 3):
        b.set(x, G, z0 + 1, 'minecraft:hay_block', axis='y')
        b.set(x, G + 1, z0 + 1, 'minecraft:target', power='0')
    # чучела
    for x in range(x0 + 3, x1 - 2, 4):
        b.entities.append((x, G, (z0 + z1) // 2 + 1, 'minecraft:armor_stand', {'ShowArms': TByte(1)}))
    # стойка с оружием
    b.set(x1 - 1, G, z1 - 1, 'minecraft:barrel', facing='up', open='false')
    b.set(x1 - 2, G, z1 - 1, 'minecraft:spruce_trapdoor', facing='north', half='top', open='false', powered='false', waterlogged='false')
    b.entities.append(((x0 + x1) // 2 - 2, G, z1 - 2, 'litrpg:human_knight', {}))
    b.entities.append(((x0 + x1) // 2 + 2, G, z0 + 3, 'litrpg:human_guard', {}))
    b.reserve(x0, z0, x1, z1)


def garden(b, x0, z0, x1, z1):
    """Сад: живые изгороди, клумбы, деревья, фонтан посредине, скамейки."""
    G = b.G
    rnd = b.rnd
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
            edge = x in (x0, x1) or z in (z0, z1)
            if edge and not (abs(x - cx) <= 1 or abs(z - cz) <= 1):
                b.set(x, G, z, 'minecraft:oak_leaves', persistent='true', distance='1', waterlogged='false')
            elif abs(x - cx) <= 1 or abs(z - cz) <= 1:
                b.set(x, G - 1, z, 'minecraft:gravel' if (x + z) % 2 else 'minecraft:dirt_path')
            elif not edge and rnd.random() < 0.55:
                b.set(x, G, z, 'minecraft:' + rnd.choice(['rose_bush', 'peony', 'lilac', 'poppy', 'cornflower', 'allium', 'oxeye_daisy',
                                                          'white_tulip', 'pink_tulip']), **({'half': 'lower'} if False else {}))
                blk = b.blocks[(x, G, z)][0]
                if blk.endswith(('rose_bush', 'peony', 'lilac')):
                    b.set(x, G, z, blk, half='lower')
                    b.set(x, G + 1, z, blk, half='upper')
    old = b.G
    b.fountain(cx, cz, 2, 'minecraft:quartz_bricks')
    for (tx, tz) in ((x0 + 3, z0 + 3), (x1 - 3, z0 + 3), (x0 + 3, z1 - 3), (x1 - 3, z1 - 3)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                b.set(tx + dx, G, tz + dz, 'minecraft:air')
                b.set(tx + dx, G + 1, tz + dz, 'minecraft:air')
        b.tree(tx, tz, 5, 'minecraft:birch_log', 'minecraft:flowering_azalea_leaves', 2.2)
    for (bx, bz, f) in ((cx - 4, cz, 'east'), (cx + 4, cz, 'west')):
        b.set(bx, G, bz, 'minecraft:spruce_stairs', facing=f, half='bottom', shape='straight', waterlogged='false') if False else \
            b.stair(bx, G, bz, 'minecraft:spruce_stairs', {'east': 'west', 'west': 'east'}[f])
    b.G = old
    b.reserve(x0, z0, x1, z1)


def wall_sheds(b, x0, z0, x1, z1, rnd):
    """Склады и навесы вплотную к стенам двора: бочки, ящики, дрова, стога."""
    G = b.G
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            if not b.free(x, z, x, z):
                continue
            b.set(x, G - 1, z, 'minecraft:coarse_dirt' if rnd.random() < 0.4 else 'minecraft:cobblestone')
            r = rnd.random()
            if r < 0.25:
                b.set(x, G, z, 'minecraft:barrel', facing='up', open='false')
                if rnd.random() < 0.4:
                    b.set(x, G + 1, z, 'minecraft:barrel', facing='up', open='false')
            elif r < 0.4:
                b.set(x, G, z, 'minecraft:hay_block', axis='y')
            elif r < 0.5:
                b.set(x, G, z, 'minecraft:spruce_log', axis='x')
            elif r < 0.55:
                b.set(x, G, z, 'minecraft:chest', facing='south', type='single', waterlogged='false')
            # навес
            b.set(x, G + 3, z, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
    b.reserve(x0, z0, x1, z1)


def bailey(b, cx0, cz0, cx1, cz1, keep):
    """Заполнить двор замка (внутри стен cx0..cx1 × cz0..cz1, донжон keep=(x0,z0,x1,z1))."""
    rnd = random.Random('bailey')
    G = b.G
    C = (cx0 + cx1) // 2
    kx0, kz0, kx1, kz1 = keep
    # дорога от ворот к донжону
    for z in range(kz1 + 1, cz1 + 1):
        for x in range(C - 2, C + 3):
            b.set(x, G - 1, z, b.bp(['minecraft:stone_bricks', 'minecraft:polished_andesite', 'minecraft:stone_bricks',
                                     'minecraft:cracked_stone_bricks'], x, 0, z))
        if z % 6 == 0:
            for x in (C - 3, C + 3):
                b.lamp_post(x, z, 3, 'minecraft:dark_oak_fence')
    b.reserve(C - 3, kz1 + 1, C + 3, cz1)
    # площадка перед донжоном
    for x in range(kx0, kx1 + 1):
        for z in range(kz1 + 1, kz1 + 4):
            b.set(x, G - 1, z, 'minecraft:polished_andesite' if (x + z) % 2 else 'minecraft:stone_bricks')
    for x in (kx0 + 2, kx1 - 2):
        b.set(x, G, kz1 + 2, 'minecraft:potted_azalea_bush')
    b.reserve(kx0, kz1 + 1, kx1, kz1 + 3)
    ix0, iz0, ix1, iz1 = cx0 + 3, cz0 + 3, cx1 - 3, cz1 - 3   # внутри стен
    # запад: конюшня и кузница, восток: казарма и часовня
    stable(b, ix0 + 1, kz1 + 6, 10, 21, open_side='east')
    forge(b, ix0 + 1, kz1 + 30, 10, 9)
    b.house(ix1 - 12, kz1 + 6, 12, 10, floors=2, door='west', interior='guard', npc='litrpg:human_knight')
    chapel(b, ix1 - 11, kz1 + 19, 11, 15)
    # плац и сад в середине
    training_yard(b, C + 5, kz1 + 6, ix1 - 15, kz1 + 22)
    garden(b, ix0 + 13, kz1 + 6, C - 5, kz1 + 22)
    # колодец и телега у ворот
    b.well(C - 6, cz1 - 5)
    b.reserve(C - 8, cz1 - 7, C - 4, cz1 - 3)
    for k in range(3):
        b.entities.append((C + rnd.randint(-2, 2), G, rnd.randint(kz1 + 8, cz1 - 4), 'litrpg:human_guard', {}))
    b.entities.append((C - 1, G, kz1 + 4, 'litrpg:human_citizen', {}))
    # северная полоса за донжоном и боковые углы — сады и склады
    garden(b, ix0 + 1, iz0 + 1, kx0 - 3, kz0 + 9) if kx0 - 3 - ix0 > 8 else None
    garden(b, kx1 + 3, iz0 + 1, ix1 - 1, kz0 + 9) if ix1 - kx1 - 3 > 8 else None
    wall_sheds(b, ix0, iz0, ix1, iz0, rnd)
    wall_sheds(b, ix0, iz0, ix0, iz1, rnd)
    wall_sheds(b, ix1, iz0, ix1, iz1, rnd)
    wall_sheds(b, ix0, iz1, C - 4, iz1, rnd)
    wall_sheds(b, C + 4, iz1, ix1, iz1, rnd)
    # остатки свободной земли — трава с цветами и кустами
    for x in range(ix0, ix1 + 1):
        for z in range(iz0, iz1 + 1):
            if b.free(x, z, x, z) and (x, G, z) not in b.blocks:
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                r = rnd.random()
                if r < 0.05:
                    b.set(x, G, z, 'minecraft:azalea_leaves', persistent='true', distance='1', waterlogged='false')
                elif r < 0.18:
                    b.set(x, G, z, 'minecraft:' + rnd.choice(['grass', 'grass', 'fern', 'poppy', 'dandelion']))
