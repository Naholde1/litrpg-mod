"""
Места в духе литРПГ для «Мира Системы» (tools/gen_mapbuild.py ставит их на карту):
обелиски Системы (точка возрождения), башни магов, гнёзда виверн, ведьмины избушки
на курьих ножках, арены испытаний (колокол зовёт волны чудовищ) и упавшие звёзды.
Механики — в Java: world/Lore (возрождение у обелиска, открытие мест), world/Arena.
"""
import math
import random

from mapbuild_lib import City, TByte

STONE = ['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks']


def disc(b, cx, cz, r, y, block, ring=None):
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            d = math.hypot(x - cx, z - cz)
            if d <= r:
                b.set(x, y, z, b.bp(block, x, y, z) if ring is None or d < r - 1 else ring)


def ground(b, cx, cz, r, top='minecraft:grass_block', under='minecraft:dirt', depth=3):
    """Подсыпка: постройка не повиснет на склоне."""
    for x in range(int(cx - r - 1), int(cx + r + 2)):
        for z in range(int(cz - r - 1), int(cz + r + 2)):
            if math.hypot(x - cx, z - cz) <= r:
                for y in range(b.G - depth, b.G - 1):
                    b.set(x, y, z, under)
                if top == 'minecraft:grass_block':
                    b.set(x, b.G - 1, z, top, snowy='false')
                else:
                    b.set(x, b.G - 1, z, b.bp(top, x, b.G - 1, z))


# ---------------- Обелиск Системы ----------------

def obelisk(seed):
    """Чёрный шпиль с фиолетовыми рунами. ПКМ по камню-якорю (lodestone) — здесь возрождение."""
    rnd = random.Random(seed)
    b = City(32, 44, 32, 'obelisk%d' % seed, 'human', ground=6)
    G, C = b.G, 16
    ground(b, C, C, 12, top=['minecraft:grass_block', 'minecraft:moss_block', 'minecraft:grass_block'])
    for x in range(32):
        for z in range(32):
            if math.hypot(x - C, z - C) <= 12 and b.blocks.get((x, G - 1, z), ('',))[0] == 'minecraft:grass_block':
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
    # площадка: кольца, по краю — руны
    disc(b, C, C, 9, G - 1, ['minecraft:polished_blackstone_bricks', 'minecraft:polished_blackstone_bricks',
                             'minecraft:cracked_polished_blackstone_bricks'], ring='minecraft:chiseled_polished_blackstone')
    disc(b, C, C, 5, G - 1, 'minecraft:polished_blackstone')
    for k in range(12):
        a = 2 * math.pi * k / 12
        b.set(int(round(C + math.cos(a) * 7)), G - 1, int(round(C + math.sin(a) * 7)), 'minecraft:amethyst_block')
    # сам обелиск: сужается кверху, по граням — светящиеся руны
    H = 22
    for y in range(G, G + H):
        t = (y - G) / H
        hw = 2 if t < 0.35 else 1 if t < 0.8 else 0
        for x in range(C - hw, C + hw + 1):
            for z in range(C - hw, C + hw + 1):
                edge = abs(x - C) == hw or abs(z - C) == hw
                rune = edge and (y - G) % 4 == 2 and (x == C or z == C) and hw > 0
                b.set(x, y, z, 'minecraft:purple_stained_glass' if rune else
                      'minecraft:crying_obsidian' if (x + y + z) % 5 == 0 else 'minecraft:obsidian')
    b.set(C, G + H, C, 'minecraft:end_rod', facing='up')
    # парящее кольцо вокруг вершины
    for k in range(16):
        a = 2 * math.pi * k / 16
        b.set(int(round(C + math.cos(a) * 3.6)), G + H - 4, int(round(C + math.sin(a) * 3.6)),
              'minecraft:amethyst_block' if k % 2 else 'minecraft:purple_stained_glass')
    # четыре малых столба с огнями
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        px, pz = int(round(C + math.cos(a) * 7)), int(round(C + math.sin(a) * 7))
        for y in range(G, G + 3):
            b.set(px, y, pz, 'minecraft:polished_blackstone_brick_wall', up='true', east='none', west='none', north='none', south='none',
                  waterlogged='false')
        b.set(px, G + 3, pz, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
        b.set(px, G - 1, pz, 'minecraft:sea_lantern')
    # камень-якорь перед обелиском
    b.set(C, G, C + 4, 'minecraft:lodestone')
    b.set(C, G - 1, C + 5, 'minecraft:chiseled_polished_blackstone')
    b.set(C - 1, G, C + 4, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
    b.set(C + 1, G, C + 4, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
    for k in range(5):
        x, z = rnd.randint(4, 27), rnd.randint(4, 27)
        if math.hypot(x - C, z - C) > 10:
            b.set(x, G, z, 'minecraft:small_amethyst_bud', facing='up', waterlogged='false')
    return b


# ---------------- Башня мага ----------------

def mage_tower(seed, npc='litrpg:human_mage'):
    rnd = random.Random(seed)
    b = City(32, 64, 32, 'magetower%d' % seed, 'human', ground=4)
    G, C, R = b.G, 16, 6
    ground(b, C, C, 13)
    # колдовской круг вокруг башни
    for k in range(48):
        a = 2 * math.pi * k / 48
        b.set(int(round(C + math.cos(a) * 11)), G - 1, int(round(C + math.sin(a) * 11)), 'minecraft:purpur_block')
    for k in range(6):
        a = 2 * math.pi * k / 6
        px, pz = int(round(C + math.cos(a) * 11)), int(round(C + math.sin(a) * 11))
        b.set(px, G, pz, 'minecraft:purpur_pillar', axis='y')
        b.set(px, G + 1, pz, 'minecraft:end_rod', facing='up')
    b.round_tower(C, C, R, 36, roof_block='minecraft:purpur_block', top_light=True,
                  base=['minecraft:stone_bricks', 'minecraft:stone_bricks', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks',
                        'minecraft:stone_bricks', 'minecraft:cracked_stone_bricks', 'minecraft:stone_bricks'])
    # пояса из резного камня между этажами
    for y in range(G + 5, G + 36, 6):
        for x in range(C - R, C + R + 1):
            for z in range(C - R, C + R + 1):
                if R - 1.0 < math.hypot(x - C, z - C) <= R + 0.45:
                    b.set(x, y, z, 'minecraft:chiseled_stone_bricks')
    # дверь на юг
    for y in (G, G + 1):
        b.set(C, y, C + R, 'minecraft:air')
    b.door(C, G, C + R, 'south', 'minecraft:dark_oak_door')
    b.set(C, G - 1, C + R + 1, 'minecraft:polished_andesite')

    def ring_spot(k, n, y, rr=R - 2):
        a = 2 * math.pi * k / n + 0.3
        return int(round(C + math.cos(a) * rr)), y, int(round(C + math.sin(a) * rr))

    def ok(x, z):
        return not (x == C and z <= C - R + 2) and not (x == C and z >= C + R - 1)

    floors = [G + 6 * i + 1 for i in range(6)]
    # 1 — алхимия
    y = floors[0] - 1
    for k, blk in enumerate(['minecraft:brewing_stand', 'minecraft:cauldron', 'minecraft:barrel', 'minecraft:brewing_stand',
                             'minecraft:crafting_table', 'minecraft:barrel', 'minecraft:flower_pot']):
        x, yy, z = ring_spot(k, 9, y)
        if ok(x, z):
            if blk == 'minecraft:barrel':
                b.set(x, yy, z, blk, facing='up', open='false')
            elif blk == 'minecraft:brewing_stand':
                b.set(x, yy, z, blk, has_bottle_0='true', has_bottle_1='false', has_bottle_2='true')
            else:
                b.set(x, yy, z, blk)
    b.chest(C + 2, floors[0] - 1, C - 2, 'minecraft:chests/simple_dungeon')
    # 2 — библиотека: полки по кругу
    y = floors[1]
    for k in range(20):
        x, _, z = ring_spot(k, 20, y, R - 1.4)
        if ok(x, z):
            for dy in range(3):
                b.set(x, y + dy, z, 'minecraft:bookshelf')
    b.set(C, y, C + 1, 'minecraft:lectern', facing='south', has_book='false', powered='false')
    b.chest(C - 2, y, C + 2, 'minecraft:chests/stronghold_library')
    # 3 — зачарование
    y = floors[2]
    b.set(C, y, C + 1, 'minecraft:enchanting_table')
    for k in range(14):
        x, _, z = ring_spot(k, 14, y, R - 1.4)
        if ok(x, z):
            b.set(x, y, z, 'minecraft:bookshelf')
            b.set(x, y + 1, z, 'minecraft:bookshelf')
    # 4 — покои
    y = floors[3]
    b.bed(C + 2, y, C + 1, 'south', 'purple')
    b.set(C - 3, y, C + 1, 'minecraft:barrel', facing='up', open='false')
    b.set(C - 3, y, C - 1, 'minecraft:purple_carpet')
    b.chest(C - 2, y, C + 3, 'minecraft:chests/village/village_temple')
    # 5 — кладовая чудес
    y = floors[4]
    for k in range(6):
        x, _, z = ring_spot(k, 6, y)
        if ok(x, z):
            blk = rnd.choice(['minecraft:amethyst_block', 'minecraft:budding_amethyst', 'minecraft:barrel'])
            if blk == 'minecraft:barrel':
                b.set(x, y, z, blk, facing='up', open='false')
            else:
                b.set(x, y, z, blk)
    b.chest(C + 2, y, C + 2, 'minecraft:chests/stronghold_corridor')
    # 6 — обсерватория: сам маг
    y = floors[5]
    b.set(C + 2, y, C, 'minecraft:lightning_rod', facing='up', powered='false', waterlogged='false')
    b.entities.append((C - 1, y, C + 1, npc, {}))
    # ковры этажей
    for fy in floors[1:]:
        for x in range(C - 1, C + 2):
            for z in range(C - 1, C + 2):
                if (x, fy, z) not in b.blocks or b.blocks[(x, fy, z)][0] == 'minecraft:air':
                    b.set(x, fy, z, 'minecraft:purple_carpet')
    return b


# ---------------- Гнездо виверн ----------------

def wyvern_nest(seed):
    rnd = random.Random(seed)
    b = City(48, 36, 48, 'wyvernnest%d' % seed, 'human', ground=6)
    G, C = b.G, 24
    rocks = ['minecraft:stone', 'minecraft:andesite', 'minecraft:tuff', 'minecraft:cobblestone', 'minecraft:stone']
    # скала-утёс
    for x in range(48):
        for z in range(48):
            d = math.hypot(x - C, z - C) + 2.5 * math.sin(math.atan2(z - C, x - C) * 5 + seed)
            if d < 19:
                top = G + int(max(0, 12 - d * 0.62)) + rnd.randint(0, 1)
                for y in range(G - 3, top):
                    b.set(x, y, z, b.bp(rocks, x, y, z))
                b.set(x, top, z, 'minecraft:gravel' if (x * z) % 7 == 0 else b.bp(rocks, x, top, z))
    top = G + 12
    # чаша гнезда
    for x in range(C - 8, C + 9):
        for z in range(C - 8, C + 9):
            d = math.hypot(x - C, z - C)
            if d < 6:
                for y in range(top - 1, top + 3):
                    b.set(x, y, z, 'minecraft:air')
                b.set(x, top - 2, z, 'minecraft:hay_block', axis='y' if d > 2 else 'x')
            elif d < 8:
                h = 2 if (x + z) % 3 else 3
                for y in range(top - 1, top - 1 + h):
                    blk = rnd.choice(['minecraft:hay_block', 'minecraft:mangrove_roots', 'minecraft:spruce_log',
                                      'minecraft:bone_block', 'minecraft:hay_block'])
                    if blk == 'minecraft:mangrove_roots':
                        b.set(x, y, z, blk, waterlogged='false')
                    else:
                        b.set(x, y, z, blk, axis='y')
    # кладка яиц, кости и золото
    for (dx, dz) in ((0, 0), (1, 1), (-1, 1)):
        b.set(C + dx, top - 1, C + dz, 'minecraft:sniffer_egg', hatch='0')
    for k in range(10):
        a = rnd.random() * 2 * math.pi
        rr = rnd.uniform(2, 5)
        x, z = int(C + math.cos(a) * rr), int(C + math.sin(a) * rr)
        if (x, top - 1, z) not in b.blocks or b.blocks[(x, top - 1, z)][0] == 'minecraft:air':
            blk = rnd.choice(['minecraft:raw_gold_block', 'minecraft:bone_block', 'minecraft:gold_block', 'minecraft:skeleton_skull'])
            if blk == 'minecraft:skeleton_skull':
                b.set(x, top - 1, z, blk, rotation=str(rnd.randint(0, 15)))
            elif blk == 'minecraft:bone_block':
                b.set(x, top - 1, z, blk, axis='y')
            else:
                b.set(x, top - 1, z, blk)
    b.chest(C + 3, top - 1, C - 2, 'minecraft:chests/bastion_treasure', facing='west')
    # стражи
    b.entities.append((C - 3, top, C - 3, 'litrpg:wyvern', {}))
    b.entities.append((C + 4, top + 1, C + 4, 'litrpg:wyvern', {}))
    b.entities.append((C - 10, G + 6, C + 2, 'litrpg:harpy', {}))
    # тропа наверх: ступени по склону с юга
    for i in range(12):
        z = C + 18 - i
        y = G + int(i * 0.95)
        for x in (C - 1, C, C + 1):
            b.set(x, y, z, 'minecraft:cobblestone_stairs', facing='north', half='bottom', shape='straight', waterlogged='false')
            for yy in range(y + 1, y + 4):
                b.set(x, yy, z, 'minecraft:air')
    return b


# ---------------- Избушка на курьих ножках ----------------

def witch_hut(seed):
    rnd = random.Random(seed)
    b = City(32, 34, 32, 'witchhut%d' % seed, 'human', ground=4)
    G, C = b.G, 16
    ground(b, C, C, 12, top=['minecraft:mud', 'minecraft:grass_block', 'minecraft:moss_block', 'minecraft:mud'], under='minecraft:mud')
    for x in range(32):
        for z in range(32):
            if b.blocks.get((x, G - 1, z), ('',))[0] == 'minecraft:grass_block':
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
    # лужи
    for k in range(6):
        x, z = rnd.randint(4, 27), rnd.randint(4, 27)
        if math.hypot(x - C, z - C) > 6:
            for dx in range(-1, 2):
                for dz in range(-1, 1):
                    b.set(x + dx, G - 1, z + dz, 'minecraft:water', level='0')
            b.set(x, G, z, 'minecraft:lily_pad')
    # две ноги: бедро, колено назад, голень, пальцы
    for side in (-2, 2):
        lx = C + side
        pts = [(lx, G + 9, C), (lx, G + 7, C + 1), (lx, G + 5, C + 2), (lx, G + 3, C + 1), (lx, G + 1, C), (lx, G, C)]
        for (x, y, z) in pts:
            b.set(x, y, z, 'minecraft:stripped_mangrove_log', axis='y')
            b.set(x, y + 1, z, 'minecraft:stripped_mangrove_log', axis='y')
        for (dx, dz) in ((0, -1), (0, -2), (-1, -1), (1, -1), (0, 1)):
            b.set(lx + dx, G, C + dz, 'minecraft:mangrove_roots', waterlogged='false')
    # избушка
    y0 = G + 10
    for x in range(C - 4, C + 5):
        for z in range(C - 3, C + 4):
            b.set(x, y0, z, 'minecraft:spruce_planks')
            edge = x in (C - 4, C + 4) or z in (C - 3, C + 3)
            for y in range(y0 + 1, y0 + 5):
                if edge:
                    corner = x in (C - 4, C + 4) and z in (C - 3, C + 3)
                    win = not corner and y == y0 + 2 and (x == C or z == C) and not (z == C + 3 and x == C)
                    b.set(x, y, z, 'minecraft:spruce_log' if corner else 'minecraft:green_stained_glass_pane' if win else
                          b.bp(['minecraft:dark_oak_planks', 'minecraft:mossy_cobblestone', 'minecraft:dark_oak_planks'], x, y, z))
                else:
                    b.set(x, y, z, 'minecraft:air')
    # дверь и лестница с крыльцом
    b.door(C, y0 + 1, C + 3, 'south', 'minecraft:spruce_door')
    for y in range(G, y0 + 1):
        b.set(C, y, C + 4, 'minecraft:ladder', facing='south', waterlogged='false')
    b.set(C, y0, C + 4, 'minecraft:spruce_planks')
    b.set(C, y0 + 1, C + 4, 'minecraft:air')
    # двускатная замшелая крыша
    for i in range(6):
        for x in range(C - 5, C + 6):
            for z in (C - 4 + i, C + 4 - i):
                b.set(x, y0 + 5 + i, z, 'minecraft:mossy_cobblestone_stairs' if i < 5 else 'minecraft:mossy_cobblestone',
                      **({'facing': 'south' if z < C else 'north', 'half': 'bottom', 'shape': 'straight', 'waterlogged': 'false'} if i < 5 else {}))
        if i < 4:
            for z in range(C - 3 + i, C + 4 - i):
                for x in (C - 4, C + 4):
                    b.set(x, y0 + 5 + i, z, 'minecraft:dark_oak_planks')
    b.set(C + 3, y0 + 9, C, 'minecraft:cobblestone_wall', up='true', east='none', west='none', north='none', south='none', waterlogged='false')
    # убранство ведьмы
    b.set(C - 3, y0 + 1, C - 2, 'minecraft:cauldron')
    b.set(C - 2, y0 + 1, C - 2, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='true', has_bottle_2='false')
    b.set(C + 3, y0 + 1, C - 2, 'minecraft:barrel', facing='up', open='false')
    b.set(C + 3, y0 + 1, C - 1, 'minecraft:potted_red_mushroom')
    b.set(C + 3, y0 + 2, C - 2, 'minecraft:skeleton_skull', rotation='8')
    b.set(C - 3, y0 + 4, C + 1, 'minecraft:cobweb')
    b.set(C + 3, y0 + 4, C + 2, 'minecraft:cobweb')
    b.set(C, y0 + 4, C, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.chest(C - 3, y0 + 1, C + 2, 'minecraft:chests/simple_dungeon', facing='east')
    b.entities.append((C, y0 + 1, C - 1, 'minecraft:witch', {'PersistenceRequired': TByte(1)}))
    b.entities.append((C + 2, y0 + 1, C + 1, 'minecraft:cat', {'variant': 'minecraft:all_black', 'PersistenceRequired': TByte(1)}))
    # котёл с зельем во дворе
    b.set(C + 6, G, C + 6, 'minecraft:water_cauldron', level='3')
    b.set(C + 6, G - 1, C + 6, 'minecraft:magma_block')
    return b


# ---------------- Арена испытаний ----------------

def arena(seed):
    b = City(64, 26, 64, 'arena%d' % seed, 'human', ground=4)
    G, C = b.G, 32
    ground(b, C, C, 30, top=['minecraft:stone_bricks', 'minecraft:andesite', 'minecraft:polished_andesite'], under='minecraft:stone')
    R0, R1 = 15, 25
    # песок ристалища
    disc(b, C, C, R0, G - 1, ['minecraft:sand', 'minecraft:sand', 'minecraft:smooth_sandstone'])
    # трибуны: ряды ступеней вверх и наружу
    for x in range(64):
        for z in range(64):
            d = math.hypot(x - C + 0.5, z - C + 0.5)
            if R0 <= d < R1:
                row = int(d - R0)
                h = 2 + row
                gate = min(abs(x - C), abs(z - C)) <= 2
                if gate:
                    for y in range(G, G + 5):
                        b.set(x, y, z, 'minecraft:air')
                    b.set(x, G - 1, z, 'minecraft:polished_andesite')
                    for y in range(G + 5, G + 2 + R1 - R0):
                        b.set(x, y, z, b.bp(STONE, x, y, z))
                    continue
                for y in range(G, G + h):
                    b.set(x, y, z, b.bp(STONE, x, y, z))
                # ступень-сиденье: смотрит к центру
                fx, fz = C - x, C - z
                facing = ('east' if fx > 0 else 'west') if abs(fx) > abs(fz) else ('south' if fz > 0 else 'north')
                if row % 2 == 0:
                    b.set(x, G + h, z, 'minecraft:stone_brick_stairs', facing=facing, half='bottom', shape='straight', waterlogged='false')
            elif R1 <= d < R1 + 1.2:
                gate = min(abs(x - C), abs(z - C)) <= 2
                for y in range(G, G + 2 + R1 - R0 + 3):
                    if gate and y < G + 5:
                        b.set(x, y, z, 'minecraft:air')
                    else:
                        b.set(x, y, z, 'minecraft:chiseled_stone_bricks' if (y - G) % 4 == 0 else b.bp(STONE, x, y, z))
                if (x + z) % 2 == 0 and not gate:
                    b.set(x, G + 2 + R1 - R0 + 3, z, b.bp(STONE, x, G, z))
    # наружная стена: два яруса арок с тёмной глубиной, как у колизея
    for k in range(64):
        a = 2 * math.pi * (k + 0.5) / 64
        for tier in (G + 2, G + 8):
            x, z = int(round(C + math.cos(a) * (R1 + 0.4))), int(round(C + math.sin(a) * (R1 + 0.4)))
            if min(abs(x - C), abs(z - C)) <= 3 or k % 2:
                continue
            ix, iz = int(round(C + math.cos(a) * (R1 - 0.6))), int(round(C + math.sin(a) * (R1 - 0.6)))
            for y in range(tier, tier + 3):
                b.set(x, y, z, 'minecraft:air')
                b.set(ix, y, iz, 'minecraft:polished_deepslate')
            b.set(x, tier + 3, z, 'minecraft:stone_brick_slab', type='top', waterlogged='false')
    # низкая стенка ристалища и решётки ворот
    for k in range(120):
        a = 2 * math.pi * k / 120
        x, z = int(round(C + math.cos(a) * (R0 - 0.5))), int(round(C + math.sin(a) * (R0 - 0.5)))
        if min(abs(x - C), abs(z - C)) <= 2:
            continue
        b.set(x, G, z, 'minecraft:polished_andesite')
        b.set(x, G + 1, z, 'minecraft:iron_bars', east='true', west='true', north='true', south='true', waterlogged='false')
    # знамёна и огни на стене
    for k in range(16):
        a = 2 * math.pi * k / 16 + 0.2
        x, z = int(round(C + math.cos(a) * (R1 + 0.2))), int(round(C + math.sin(a) * (R1 + 0.2)))
        b.set(x, G + 2 + R1 - R0 + 3, z, 'minecraft:lantern', hanging='false', waterlogged='false')
    # центр: помост с колоколом испытаний
    disc(b, C, C, 2.5, G - 1, 'minecraft:chiseled_stone_bricks')
    b.set(C, G, C, 'minecraft:polished_andesite')
    b.set(C, G + 1, C, 'minecraft:bell', attachment='floor', facing='north', powered='false')
    for (dx, dz) in ((3, 3), (-3, 3), (3, -3), (-3, -3)):
        b.set(C + dx, G, C + dz, 'minecraft:stone_brick_wall', up='true', east='none', west='none', north='none', south='none', waterlogged='false')
        b.set(C + dx, G + 1, C + dz, 'minecraft:lantern', hanging='false', waterlogged='false')
    # распорядитель у южных ворот
    b.entities.append((C + 3, G, C + R1 + 3, 'litrpg:human_knight', {}))
    b.chest(C - 3, G, C + R1 + 3, 'minecraft:chests/village/village_weaponsmith')
    return b


# ---------------- Упавшая звезда ----------------

def fallen_star(seed):
    rnd = random.Random(seed)
    b = City(32, 30, 32, 'fallenstar%d' % seed, 'human', ground=10)
    G, C = b.G, 16
    scorch = ['minecraft:blackstone', 'minecraft:basalt', 'minecraft:coarse_dirt', 'minecraft:tuff', 'minecraft:blackstone']
    for x in range(32):
        for z in range(32):
            d = math.hypot(x - C, z - C)
            if d > 14:
                continue
            depth = max(0.0, 6 - d * d / 22) if d < 11 else 0
            floor = G - 1 - int(depth)
            for y in range(G - 6, floor):
                b.set(x, y, z, 'minecraft:stone')
            b.set(x, floor, z, b.bp(scorch, x, floor, z) if d < 13 else 'minecraft:coarse_dirt')
            for y in range(floor + 1, G + 4):
                b.set(x, y, z, 'minecraft:air')
            # вал из выброшенной породы
            if 10.5 <= d < 13:
                for y in range(G - 1, G + int(2 - abs(d - 11.7))):
                    b.set(x, y, z, b.bp(scorch, x, y, z))
    # сама звезда — светится и звенит
    sy = G - 4
    for x in range(C - 4, C + 5):
        for y in range(sy - 2, sy + 5):
            for z in range(C - 4, C + 5):
                d = math.sqrt((x - C) ** 2 + (y - sy) ** 2 * 1.3 + (z - C) ** 2)
                if d < 3.4:
                    b.set(x, y, z, rnd.choice(['minecraft:amethyst_block', 'minecraft:crying_obsidian', 'minecraft:budding_amethyst',
                                               'minecraft:glowstone', 'minecraft:amethyst_block']))
    for k in range(18):
        a = rnd.random() * 2 * math.pi
        rr = rnd.uniform(3.5, 8)
        x, z = int(C + math.cos(a) * rr), int(C + math.sin(a) * rr)
        for y in range(G + 3, G - 8, -1):
            if (x, y, z) in b.blocks and b.blocks[(x, y, z)][0] != 'minecraft:air':
                b.set(x, y + 1, z, rnd.choice(['minecraft:amethyst_cluster', 'minecraft:large_amethyst_bud']), facing='up', waterlogged='false')
                break
    b.chest(C + 3, sy - 1, C + 3, 'minecraft:chests/ruined_portal')
    b.entities.append((C - 5, G - 2, C + 4, 'litrpg:crystal_golem', {}))
    return b
