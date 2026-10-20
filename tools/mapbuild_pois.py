"""Поселения, лагеря, караваны, заставы, руины и чудеса света для gen_mapbuild.py."""
import math
import random

from mapbuild_lib import City, DIRS, TByte
from mapbuild_cities import tent, bonfire, totem, giant_tree, blob, angle_dir, docks

VILLAGE_NAMES = ['Ольховка', 'Медовый Брод', 'Рябиновка', 'Каменка', 'Тихий Луг', 'Заречье', 'Вересково', 'Липовка',
                 'Ясная Поляна', 'Сосновый Бор', 'Мельничье', 'Гусиный Пруд']


def village(seed, style='human', size=64):
    rnd = random.Random(seed)
    b = City(size, 40, size, 'village%d' % seed, style, ground=4)
    C = size // 2
    G = b.G
    for x in range(size):
        for z in range(size):
            r = math.hypot(x - C, z - C)
            if r < C - 2:
                path = abs(x - C) <= 1 or abs(z - C) <= 1 or 4 < r < 6.5
                b.set(x, G - 1, z, b.bp(b.st['path'], x, 0, z) if path else 'minecraft:grass_block', **({} if path else {'snowy': 'false'}))
    b.well(C, C)
    b.reserve(C - 6, C - 6, C + 6, C + 6)
    b.reserve(C - 2, 0, C + 2, size - 1)
    b.reserve(0, C - 2, size - 1, C + 2)
    # часовня
    if rnd.random() < 0.6:
        b.house(C + 6, C - 20, 8, 12, floors=1, door='south', interior='library', npc='litrpg:human_mage', roof=b.st['roofs'][0])
        for y in range(G + 4, G + 12):
            b.set(C + 10, y, C - 18, b.st['frame'], axis='y')
        b.set(C + 10, G + 12, C - 18, 'minecraft:bell', attachment='floor', facing='north', powered='false')
    # дома
    placed = 0
    for k in range(40):
        a = rnd.random() * 2 * math.pi
        r = rnd.uniform(11, C - 10)
        w, d = rnd.randint(6, 9), rnd.randint(5, 8)
        x0 = int(C + math.cos(a) * r - w / 2)
        z0 = int(C + math.sin(a) * r - d / 2)
        if not b.free(x0 - 2, z0 - 2, x0 + w + 1, z0 + d + 1):
            continue
        door = angle_dir(math.atan2(C - (z0 + d / 2), C - (x0 + w / 2)))
        b.house(x0, z0, w, d, floors=rnd.choice([1, 1, 2]), door=door, interior=rnd.choice(['home', 'home', 'smith', 'shop']),
                npc=rnd.choice(['litrpg:human_citizen', 'minecraft:villager', None, 'minecraft:villager']))
        placed += 1
        if placed >= 9:
            break
    # поля
    for k in range(6):
        fx, fz = rnd.randint(3, size - 14), rnd.randint(3, size - 12)
        fw, fd = rnd.randint(7, 11), rnd.randint(5, 8)
        if not b.free(fx - 1, fz - 1, fx + fw, fz + fd) or math.hypot(fx + fw / 2 - C, fz + fd / 2 - C) > C - 6:
            continue
        crop = rnd.choice(['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots'])
        for x in range(fx, fx + fw):
            for z in range(fz, fz + fd):
                edge = x in (fx, fx + fw - 1) or z in (fz, fz + fd - 1)
                if edge:
                    b.set(x, G - 1, z, 'minecraft:oak_log', axis='x')
                elif z == fz + fd // 2:
                    b.set(x, G - 1, z, 'minecraft:water', level='0')
                else:
                    b.set(x, G - 1, z, 'minecraft:farmland', moisture='7')
                    b.set(x, G, z, crop, age='3' if crop == 'minecraft:beetroots' else '7')
        b.reserve(fx - 1, fz - 1, fx + fw, fz + fd)
    # загон
    px, pz = rnd.randint(4, size - 14), rnd.randint(4, size - 14)
    if b.free(px, pz, px + 9, pz + 7):
        for x in range(px, px + 10):
            for z in range(pz, pz + 8):
                if x in (px, px + 9) or z in (pz, pz + 7):
                    b.set(x, G, z, b.st['fence'], waterlogged='false')
        for k in range(4):
            b.entities.append((px + 2 + k * 2, G, pz + 3, rnd.choice(['minecraft:cow', 'minecraft:sheep', 'minecraft:pig', 'minecraft:chicken']), {}))
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = int(C + math.cos(a) * 9), int(C + math.sin(a) * 9)
        if b.free(x, z, x, z):
            b.lamp_post(x, z, 3)
    b.entities.append((C + 3, G, C + 3, 'litrpg:human_merchant', {}))
    for k in range(8):
        x, z = rnd.randint(4, size - 5), rnd.randint(4, size - 5)
        if b.free(x - 2, z - 2, x + 2, z + 2) and math.hypot(x - C, z - C) < C - 4:
            b.tree(x, z, rnd.randint(4, 6), 'minecraft:oak_log', 'minecraft:oak_leaves', 2.4)
            b.reserve(x - 2, z - 2, x + 2, z + 2)
    return b


def lumber_camp(seed):
    rnd = random.Random(seed)
    b = City(48, 32, 48, 'lumber%d' % seed, 'human', ground=4)
    C = 24
    G = b.G
    for x in range(48):
        for z in range(48):
            if math.hypot(x - C, z - C) < 22:
                b.set(x, G - 1, z, 'minecraft:coarse_dirt' if (x * 7 + z) % 5 == 0 else 'minecraft:grass_block', **({} if (x * 7 + z) % 5 == 0 else {'snowy': 'false'}))
    b.house(C - 8, C - 12, 12, 8, floors=1, door='south', interior='smith', npc='litrpg:human_citizen')
    # пила: каменотёс и брёвна
    b.set(C + 6, G, C - 6, 'minecraft:stonecutter', facing='north')
    for k in range(5):
        lx, lz = rnd.randint(6, 40), rnd.randint(20, 40)
        if b.free(lx, lz, lx + 5, lz + 2):
            for x in range(lx, lx + 5):
                for z in range(lz, lz + 2):
                    for y in range(G, G + 1 + (x + z) % 2):
                        b.set(x, y, z, rnd.choice(['minecraft:spruce_log', 'minecraft:oak_log', 'minecraft:birch_log']), axis='x')
            b.reserve(lx, lz, lx + 5, lz + 2)
    for k in range(12):
        x, z = rnd.randint(3, 44), rnd.randint(3, 44)
        if b.free(x, z, x, z) and math.hypot(x - C, z - C) < 21:
            b.set(x, G, z, 'minecraft:spruce_log', axis='y')
            b.slab(x, G + 1, z, 'minecraft:spruce_slab')
    bonfire(b, C + 4, C + 4)
    tent(b, C - 12, C + 8, 3, 'green', npc='litrpg:human_citizen')
    tent(b, C + 12, C + 12, 3, 'brown', npc='litrpg:human_ranger')
    b.entities.append((C + 8, G, C - 2, 'minecraft:horse', {}))
    return b


def fishing_village(seed, sea_a):
    rnd = random.Random(seed)
    G = 10
    b = City(64, 40, 64, 'fish%d' % seed, 'human', ground=G)
    C = 32
    for x in range(64):
        for z in range(64):
            r = math.hypot(x - C, z - C)
            if r < 20:
                b.set(x, G - 1, z, 'minecraft:sand' if (x + z) % 4 == 0 else 'minecraft:grass_block', **({} if (x + z) % 4 == 0 else {'snowy': 'false'}))
    deck = G - 6  # мостки на 64 при площадке 70
    docks(b, C, sea_a, 16, deck, n=2, length=16)
    for k in range(5):
        a = sea_a + math.pi + (k - 2) * 0.6
        x0, z0 = int(C + math.cos(a) * 12) - 3, int(C + math.sin(a) * 12) - 3
        if b.free(x0 - 1, z0 - 1, x0 + 7, z0 + 6):
            b.house(x0, z0, 7, 6, floors=1, door=angle_dir(sea_a), interior='home', npc=rnd.choice(['litrpg:human_citizen', 'minecraft:villager']))
    for k in range(4):
        x, z = int(C + math.cos(sea_a + (k - 1.5) * 0.5) * 8), int(C + math.sin(sea_a + (k - 1.5) * 0.5) * 8)
        if b.free(x, z, x + 2, z):
            for dx in range(3):
                b.set(x + dx, G, z, 'minecraft:spruce_fence', waterlogged='false')
            b.set(x + 1, G + 1, z, 'minecraft:spruce_fence', waterlogged='false')
            b.set(x, G, z + 1, 'minecraft:barrel', facing='up', open='false')
    b.entities.append((C, G, C, 'litrpg:human_merchant', {}))
    return b


def caravan_camp(seed, trader2='litrpg:dwarf_smith'):
    rnd = random.Random(seed)
    b = City(40, 24, 40, 'caravan%d' % seed, 'human', ground=4)
    C = 20
    G = b.G
    for x in range(40):
        for z in range(40):
            if math.hypot(x - C, z - C) < 18:
                b.set(x, G - 1, z, 'minecraft:dirt_path' if (x + 2 * z) % 3 == 0 else 'minecraft:grass_block', **({} if (x + 2 * z) % 3 == 0 else {'snowy': 'false'}))
    bonfire(b, C, C)
    colors = ['red', 'yellow', 'blue', 'green']
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.4
        wagon(b, int(C + math.cos(a) * 10), int(C + math.sin(a) * 10), 'x' if k % 2 else 'z', colors[k])
    tent(b, C - 11, C + 9, 3, 'white', npc='litrpg:human_merchant')
    tent(b, C + 10, C - 10, 3, 'orange', npc=trader2)
    for k in range(3):
        b.entities.append((C + 6 + k, G, C + 6, 'minecraft:llama', {'DecorItem': {'id': 'minecraft:' + colors[k] + '_carpet', 'Count': TByte(1)}}))
    b.entities.append((C - 4, G, C - 6, 'litrpg:human_knight', {}))
    b.entities.append((C + 4, G, C + 3, 'litrpg:human_knight', {}))
    return b


def wagon(b, cx, cz, axis, color):
    G = b.G
    L, W = (3, 1) if axis == 'x' else (1, 3)
    for dx in range(-L, L + 1):
        for dz in range(-W, W + 1):
            b.set(cx + dx, G, cz + dz, 'minecraft:spruce_slab', type='top', waterlogged='false')
            edge = abs(dx) == L if axis == 'z' else abs(dz) == W
            if axis == 'x' and abs(dz) == W or axis == 'z' and abs(dx) == L:
                b.set(cx + dx, G + 1, cz + dz, 'minecraft:spruce_fence', waterlogged='false')
                b.set(cx + dx, G + 2, cz + dz, f'minecraft:{color}_wool')
            else:
                b.set(cx + dx, G + 3, cz + dz, f'minecraft:{color}_wool')
            _ = edge
    for s in (-L + 1, L - 1) if axis == 'x' else (-W + 1, W - 1):
        for side in (-W - 1, W + 1) if axis == 'x' else (-L - 1, L + 1):
            x, z = (cx + s, cz + side) if axis == 'x' else (cx + side, cz + s)
            b.set(x, G, z, 'minecraft:dark_oak_trapdoor', facing='north' if axis == 'x' else 'east', half='bottom', open='true',
                  powered='false', waterlogged='false')
    b.set(cx + (L + 1 if axis == 'x' else 0), G, cz + (W + 1 if axis == 'z' else 0), 'minecraft:barrel', facing='up', open='false')
    b.reserve(cx - L - 2, cz - W - 2, cx + L + 2, cz + W + 2)


def watchtower(seed, banner='minecraft:blue_banner'):
    b = City(32, 40, 32, 'tower%d' % seed, 'human', ground=4)
    C = 16
    for x in range(32):
        for z in range(32):
            if math.hypot(x - C, z - C) < 14:
                b.set(x, b.G - 1, z, 'minecraft:grass_block', snowy='false')
    b.round_tower(C, C, 4, 22, roof_block='minecraft:spruce_planks')
    b.house(C + 5, C + 2, 8, 6, floors=1, door='west', interior='guard', npc='litrpg:human_knight')
    b.entities.append((C, b.G + 18, C, 'litrpg:human_ranger', {}))
    b.entities.append((C - 6, b.G, C - 4, 'litrpg:human_knight', {}))
    for y in range(b.G + 26, b.G + 33):
        b.set(C, y, C, 'minecraft:spruce_fence', waterlogged='false')
    b.set(C + 1, b.G + 31, C, banner.replace('_banner', '_wall_banner'), facing='east')
    return b


def ruins(seed):
    rnd = random.Random(seed)
    b = City(32, 20, 32, 'ruin%d' % seed, 'human', ground=4)
    G = b.G
    for k in range(9):
        x, z = rnd.randint(3, 28), rnd.randint(3, 28)
        h = rnd.randint(2, 7)
        horiz = rnd.random() < 0.5
        L = rnd.randint(3, 8)
        for t in range(L):
            hh = max(1, h - abs(t - L // 2) - rnd.randint(0, 2))
            for y in range(G, G + hh):
                px, pz = (x + t, z) if horiz else (x, z + t)
                if px < 32 and pz < 32:
                    b.set(px, y, pz, rnd.choice(['minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks', 'minecraft:stone_bricks',
                                                 'minecraft:mossy_cobblestone']))
    for k in range(4):
        x, z = rnd.randint(4, 27), rnd.randint(4, 27)
        for y in range(G, G + rnd.randint(3, 8)):
            b.set(x, y, z, 'minecraft:chiseled_stone_bricks' if y % 3 == 0 else 'minecraft:stone_bricks')
    b.chest(16, G, 16, 'minecraft:chests/simple_dungeon')
    b.set(15, G, 16, 'minecraft:cobweb')
    return b


# ---------------- чудеса света ----------------

def world_tree():
    b = City(112, 130, 112, 'worldtree', 'elf', ground=4)
    C = 56
    for x in range(112):
        for z in range(112):
            if math.hypot(x - C, z - C) < 50:
                b.set(x, b.G - 1, z, 'minecraft:moss_block' if (x + z) % 3 else 'minecraft:grass_block', **({} if (x + z) % 3 else {'snowy': 'false'}))
    giant_tree(b, C, C, 8, 92, 'minecraft:oak_log', 'minecraft:flowering_azalea_leaves', canopy=34,
               platforms=[(b.G + 20, 15), (b.G + 45, 13), (b.G + 70, 12)], npc='litrpg:elf_moonsage')
    for k in range(60):
        rnd = random.Random(k)
        x, y, z = rnd.randint(20, 92), rnd.randint(b.G + 70, b.G + 110), rnd.randint(20, 92)
        if (x, y, z) in b.blocks and 'leaves' in b.blocks[(x, y, z)][0] and (x, y - 1, z) not in b.blocks:
            n = rnd.randint(3, 9)
            for t in range(1, n):
                b.set(x, y - t, z, 'minecraft:cave_vines_plant', berries='true')
            b.set(x, y - n, z, 'minecraft:cave_vines', berries='true', age='25')
    b.set(C, b.G + 70 + 1, C + 13, 'minecraft:enchanting_table')
    return b


def colossus():
    b = City(48, 70, 48, 'colossus', 'human', ground=4)
    C = 24
    for x in range(48):
        for z in range(48):
            if math.hypot(x - C, z - C) < 20:
                b.set(x, b.G - 1, z, 'minecraft:stone_bricks' if (x + z) % 2 else 'minecraft:polished_andesite')
    # постамент
    for y in range(b.G, b.G + 6):
        r = 9 - (y - b.G) // 2
        for x in range(C - r, C + r + 1):
            for z in range(C - r, C + r + 1):
                b.set(x, y, z, 'minecraft:chiseled_stone_bricks' if y == b.G + 5 else 'minecraft:stone_bricks')
    b.G += 6
    b.statue(C, C, h=48, block='minecraft:smooth_stone', accent='minecraft:gold_block')
    b.G -= 6
    for k in range(8):
        a = 2 * math.pi * k / 8
        b.lamp_post(int(C + math.cos(a) * 14), int(C + math.sin(a) * 14), 4)
    return b


def titan_bones():
    b = City(112, 40, 64, 'titanbones', 'orc', ground=4)
    G = b.G
    for x in range(112):
        for z in range(64):
            if ((x - 56) / 54) ** 2 + ((z - 32) / 30) ** 2 < 1:
                b.set(x, G - 1, z, 'minecraft:sand' if (x * 3 + z) % 4 else 'minecraft:red_sand')
    # позвоночник
    for x in range(14, 96):
        y = G + int(2 + 3 * math.sin(x / 14))
        for dy in range(2):
            b.set(x, y + dy, 32, 'minecraft:bone_block', axis='x')
        # рёбра
        if x % 6 == 0 and 30 < x < 80:
            for side in (-1, 1):
                for t in range(1, 14):
                    rz = 32 + side * t
                    ry = y + int(10 * math.sin(t / 14 * math.pi))
                    b.set(x, ry, rz, 'minecraft:bone_block', axis='z')
    # череп
    for x in range(0, 16):
        for z in range(22, 43):
            for y in range(G, G + 12):
                d = math.sqrt(((x - 8) / 8) ** 2 + ((z - 32) / 10) ** 2 + ((y - G - 5) / 6) ** 2)
                if 0.8 < d < 1.0:
                    b.set(x, y, z, 'minecraft:bone_block', axis='y')
    for z in (28, 36):
        for y in range(G + 5, G + 8):
            for x in range(1, 4):
                b.set(x, y, z, 'minecraft:air')
    b.chest(8, G, 32, 'minecraft:chests/desert_pyramid')
    for k in range(5):
        b.entities.append((40 + k * 8, G, 20, 'minecraft:husk', {}))
    return b


def sky_forge():
    b = City(48, 60, 48, 'skyforge', 'dwarf', ground=30)
    C = 24
    G = b.G
    for x in range(48):
        for z in range(48):
            d = math.hypot(x - C, z - C)
            if d < 20:
                depth = int((20 - d) * 1.4)
                for y in range(G - depth, G):
                    b.set(x, y, z, 'minecraft:stone' if (x + y) % 5 else 'minecraft:iron_ore')
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
    b.house(C - 9, C - 7, 18, 14, floors=2, door='south', interior='smith', npc='litrpg:dwarf_smith', roof=b.st['roofs'][0])
    for x in (C - 6, C + 6):
        b.set(x, G - 1, C + 10, 'minecraft:lava', level='0')
        b.set(x, G, C + 11, 'minecraft:anvil', facing='north')
    # лавопад с края
    for y in range(0, G):
        b.set(C, y, C + 19, 'minecraft:magma_block')
    for k in range(4):
        a = 2 * math.pi * k / 4 + 0.7
        x, z = int(C + math.cos(a) * 17), int(C + math.sin(a) * 17)
        for y in range(G - 18, G):
            b.set(x, y, z, 'minecraft:chain', axis='y', waterlogged='false')
    b.chest(C, G, C - 4, 'minecraft:chests/bastion_other')
    return b


def abyss_gate():
    b = City(48, 48, 48, 'abyssgate', 'demon', ground=4)
    C = 24
    G = b.G
    for x in range(48):
        for z in range(48):
            if math.hypot(x - C, z - C) < 20:
                b.set(x, G - 1, z, 'minecraft:blackstone' if (x + z) % 3 else 'minecraft:soul_soil')
    R = 15
    for t in range(360):
        a = math.radians(t)
        x = C + round(math.cos(a) * R)
        y = G + R + round(math.sin(a) * R)
        for dz in range(-1, 2):
            b.set(x, y, C + dz, 'minecraft:crying_obsidian' if t % 30 < 6 else 'minecraft:obsidian')
    for x in range(C - R + 2, C + R - 1):
        for y in range(G + 2, G + 2 * R - 1):
            if math.hypot(x - C, y - G - R) < R - 1.5:
                b.set(x, y, C, 'minecraft:purple_stained_glass' if (x + y) % 5 else 'minecraft:crying_obsidian')
    for s in (-1, 1):
        for y in range(G, G + 2 * R):
            b.set(C + s * (R + 3), y, C, 'minecraft:chain', axis='y', waterlogged='false') if y > G + 20 else None
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = int(C + math.cos(a) * 18), int(C + math.sin(a) * 18)
        for y in range(G, G + 6):
            b.set(x, y, z, 'minecraft:polished_blackstone_bricks')
        b.set(x, G + 6, z, 'minecraft:soul_soil')
        b.set(x, G + 7, z, 'minecraft:soul_fire')
    for k in range(4):
        b.entities.append((C + 6, G, C + 4 + k, 'litrpg:ash_imp', {}))
    return b


# ---------------- Преисподняя ----------------

def infernalis():
    from mapbuild_cities import ziggurat, houses_ring
    b = City(128, 40, 128, 'infernalis', 'demon', ground=4)
    C = 64
    G = b.G
    for x in range(128):
        for z in range(128):
            r = math.hypot(x - C, z - C)
            if r < 58:
                canal = abs(r - 40) < 1.6 and not (abs(x - C) < 3 or abs(z - C) < 3)
                b.set(x, G - 1, z, 'minecraft:lava' if canal else ('minecraft:polished_blackstone' if (x // 4 + z // 4) % 2 else 'minecraft:blackstone'),
                      **({'level': '0'} if canal else {}))
                for y in range(G, G + 24):
                    if (x, y, z) not in b.blocks:
                        b.set(x, y, z, 'minecraft:air')
    b.reserve(C - 3, 0, C + 3, 127)
    b.reserve(0, C - 3, 127, C + 3)
    top = ziggurat(b, C, C, 4, 12)
    b.reserve(C - 14, C - 14, C + 14, C + 14)
    b.entities.append((C, top, C + 1, 'litrpg:demon_archfiend', {}))
    from gen_mapbuild import painting_map
    for x in range(C - 4, C + 4):
        for y in range(G, G + 9):
            b.set(x, y, C + 16, 'minecraft:polished_blackstone_bricks')
    painting_map(b, C + 3, G, C + 17, 'south')
    for k in range(8):
        a = 2 * math.pi * k / 8 + 0.4
        x, z = int(C + math.cos(a) * 26), int(C + math.sin(a) * 26)
        if b.free(x - 2, z - 2, x + 2, z + 2):
            for y in range(G, G + 4):
                b.set(x, y, z, 'minecraft:polished_blackstone_bricks')
            b.set(x, G + 4, z, 'minecraft:soul_soil')
            b.set(x, G + 5, z, 'minecraft:soul_fire')
            b.reserve(x - 2, z - 2, x + 2, z + 2)
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = int(C + math.cos(a) * 33), int(C + math.sin(a) * 33)
        stall_demon(b, x, z)
    houses_ring(b, C, 54, 'infernalis', 'litrpg:demon_citizen', kinds=('home', 'library', 'shop'), floors=(1, 2))
    # рамка портала домой (не зажжена)
    px, pz = C - 50, C
    for y in range(G, G + 5):
        for dz in (-2, 1):
            b.set(px, y, pz + dz, 'minecraft:obsidian')
    for dz in range(-2, 2):
        b.set(px, G - 1, pz + dz, 'minecraft:obsidian')
        b.set(px, G + 4, pz + dz, 'minecraft:obsidian')
    for k in range(4):
        b.entities.append((C + 20 + k * 2, G, C - 20, 'litrpg:demon_guard', {}))
    b.entities.append((C - 20, G, C + 20, 'litrpg:demon_cultist', {}))
    return b


def stall_demon(b, x, z):
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 and abs(dz) == 1:
                for y in range(b.G, b.G + 3):
                    b.set(x + dx, y, z + dz, 'minecraft:nether_brick_fence', waterlogged='false')
            b.set(x + dx, b.G + 3, z + dz, 'minecraft:red_wool')
    b.set(x, b.G, z - 1, 'minecraft:barrel', facing='up', open='false')
    b.entities.append((x, b.G, z, 'litrpg:demon_trader', {}))
    b.reserve(x - 3, z - 2, x + 3, z + 2)


def ash_throne():
    b = City(48, 48, 48, 'ashthrone', 'demon', ground=4)
    C = 24
    G = b.G
    for x in range(48):
        for z in range(48):
            if math.hypot(x - C, z - C) < 22:
                b.set(x, G - 1, z, 'minecraft:blackstone' if (x + z) % 3 else 'minecraft:magma_block')
    # трон: сиденье, спинка, подлокотники
    for x in range(C - 8, C + 9):
        for z in range(C - 6, C + 7):
            for y in range(G, G + 6):
                b.set(x, y, z, 'minecraft:polished_blackstone_bricks')
    for x in range(C - 8, C + 9):
        for y in range(G + 6, G + 30):
            w = 8 - max(0, (y - G - 22))
            if abs(x - C) <= w:
                for z in range(C + 3, C + 7):
                    b.set(x, y, z, 'minecraft:crying_obsidian' if (x + y) % 7 == 0 else 'minecraft:obsidian')
    for s in (-1, 1):
        for y in range(G + 6, G + 11):
            for z in range(C - 6, C + 3):
                b.set(C + s * 8, y, z, 'minecraft:polished_blackstone_bricks')
        b.set(C + s * 8, G + 11, C - 5, 'minecraft:soul_campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    for y in range(G + 30, G + 36):
        b.set(C - 5, y, C + 5, 'minecraft:obsidian')
        b.set(C + 5, y, C + 5, 'minecraft:obsidian')
    for k in range(0, 360, 30):
        a = math.radians(k)
        x, z = int(C + math.cos(a) * 18), int(C + math.sin(a) * 18)
        for y in range(G, G + 12):
            b.set(x, y, z, 'minecraft:chain', axis='y', waterlogged='false') if y > G + 4 else b.set(x, y, z, 'minecraft:blackstone')
    b.chest(C, G + 6, C - 4, 'minecraft:chests/bastion_treasure')
    return b


# ---------------- хутора у городов ----------------

FARM_NAMES = ['Хутор Колосок', 'Хутор у Мельницы', 'Хутор Ясный', 'Хутор Медовый', 'Хутор Дубки', 'Хутор Ржаной', 'Хутор Луговой',
              'Хутор Гусиный', 'Хутор Тихий', 'Хутор Яблоневый', 'Хутор Три Стога', 'Хутор Ключи']


def windmill(b, cx, cz, h=11):
    """Мельница: каменное основание, деревянный верх и крылья из заборов и шерсти."""
    G = b.G
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            edge = abs(x - cx) == 2 or abs(z - cz) == 2
            for y in range(G, G + h):
                if edge:
                    blk = b.bp(['minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:stone_bricks'], x, y, z) if y < G + 6 \
                        else 'minecraft:spruce_planks'
                    b.set(x, y, z, blk)
                else:
                    b.set(x, y, z, 'minecraft:air')
    b.door(cx, G, cz + 2, 'south', 'minecraft:spruce_door')
    for y in range(G + 1, G + h):
        b.set(cx - 1, y, cz - 1, 'minecraft:ladder', facing='south', waterlogged='false')
    b.roof(cx - 2, cz - 2, cx + 2, cz + 2, G + h, ('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks'))
    hy, hz = G + h - 2, cz + 3
    b.set(cx, hy, hz, 'minecraft:stripped_spruce_log', axis='z')
    for k in range(1, 7):
        for (dx, dy) in ((k, 0), (-k, 0), (0, k), (0, -k)):
            b.set(cx + dx, hy + dy, hz, 'minecraft:spruce_fence', waterlogged='false')
            # парус крыла
            sx, sy = (dx, dy + (1 if dx else 0)) if dx else (dx + 1, dy)
            if k > 1:
                b.set(cx + sx, hy + sy, hz, 'minecraft:white_wool')
    b.reserve(cx - 3, cz - 3, cx + 3, cz + 4)


def farmstead(seed, style='human'):
    """Хутор: дом, амбар с фермером, поля с каналами, пастбище, сад, мельница, пугало."""
    rnd = random.Random(seed * 7 + 3)
    S = 64
    b = City(S, 48, S, 'farm%d' % seed, style, ground=4)
    G = b.G
    C = S // 2
    st = b.st
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - C, z - C)
            if r < C - 1:
                path = abs(x - C) <= 1 and z > C - 6
                b.set(x, G - 1, z, b.bp(st['path'], x, 0, z) if path else 'minecraft:grass_block', **({} if path else {'snowy': 'false'}))
    from mapbuild_crafts import workshop
    # дом и амбар-мастерская фермера
    b.house(C - 13, C - 12, 9, 7, floors=2, door='south', interior='home', npc=f'litrpg:{style}_citizen')
    workshop(b, C + 3, C - 13, 'farmer', 'south', floors=1, w=12, d=9)
    b.well(C, C)
    b.reserve(C - 2, C - 2, C + 2, C + 2)
    if style in ('human', 'dwarf', 'elf'):
        windmill(b, C - 18, C + 10)
    crops = ['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots', 'minecraft:wheat']
    if style == 'demon':
        crops = ['minecraft:nether_wart']
    placed = 0
    for attempt in range(40):
        w, d = rnd.randint(9, 13), rnd.randint(6, 9)
        x0, z0 = rnd.randint(4, S - w - 4), rnd.randint(4, S - d - 4)
        if any(math.hypot(px - C, pz - C) > C - 3 for px, pz in ((x0, z0), (x0 + w, z0), (x0, z0 + d), (x0 + w, z0 + d))):
            continue
        if not b.free(x0 - 1, z0 - 1, x0 + w + 1, z0 + d + 1):
            continue
        kind = 'pasture' if placed == 1 else 'orchard' if placed == 3 else 'field'
        if kind == 'field':
            crop = rnd.choice(crops)
            for x in range(x0, x0 + w):
                for z in range(z0, z0 + d):
                    if crop.endswith('nether_wart'):
                        b.set(x, G - 1, z, 'minecraft:soul_sand')
                        b.set(x, G, z, crop, age='3')
                    elif (z - z0) % 4 == 2:
                        b.set(x, G - 1, z, 'minecraft:water', level='0')
                    else:
                        b.set(x, G - 1, z, 'minecraft:farmland', moisture='7')
                        b.set(x, G, z, crop, age='3' if crop.endswith('beetroots') else '7')
            for x in range(x0 - 1, x0 + w + 1):
                for z in (z0 - 1, z0 + d):
                    b.set(x, G, z, st['fence'], waterlogged='false')
            for z in range(z0, z0 + d):
                for x in (x0 - 1, x0 + w):
                    b.set(x, G, z, st['fence'], waterlogged='false')
            b.set(x0 + w // 2, G, z0 - 1, 'minecraft:air')  # проход
            if rnd.random() < 0.7:
                sx, sz = x0 + rnd.randint(1, w - 2), z0 + 1 + (1 if (1 - z0) % 4 == 2 else 0)
                b.set(sx, G, sz, st['fence'], waterlogged='false')
                b.set(sx, G + 1, sz, 'minecraft:hay_block', axis='y')
                b.set(sx, G + 2, sz, 'minecraft:carved_pumpkin', facing='south')
                b.set(sx - 1, G + 1, sz, 'minecraft:spruce_fence', waterlogged='false')
                b.set(sx + 1, G + 1, sz, 'minecraft:spruce_fence', waterlogged='false')
        elif kind == 'pasture':
            for x in range(x0, x0 + w):
                for z in range(z0, z0 + d):
                    if x in (x0, x0 + w - 1) or z in (z0, z0 + d - 1):
                        b.set(x, G, z, st['fence'], waterlogged='false')
            for i in range(4):
                b.entities.append((rnd.randint(x0 + 1, x0 + w - 2), G, rnd.randint(z0 + 1, z0 + d - 2),
                                   rnd.choice(['minecraft:cow', 'minecraft:sheep', 'minecraft:sheep']), {}))
            b.set(x0 + 1, G - 1, z0 + 1, 'minecraft:water', level='0')
        else:
            for x in range(x0 + 1, x0 + w - 1, 4):
                for z in range(z0 + 1, z0 + d - 1, 4):
                    b.tree(x, z, rnd.randint(4, 5), 'minecraft:oak_log', 'minecraft:oak_leaves', 2.0)
        b.reserve(x0 - 1, z0 - 1, x0 + w + 1, z0 + d + 1)
        placed += 1
        if placed >= 5:
            break
    # стога, телега, куры
    for i in range(6):
        x, z = rnd.randint(8, S - 8), rnd.randint(8, S - 8)
        if b.free(x - 1, z - 1, x + 1, z + 1) and math.hypot(x - C, z - C) < C - 4:
            b.set(x, G, z, 'minecraft:hay_block', axis='y')
            b.set(x + 1, G, z, 'minecraft:hay_block', axis='x')
            b.set(x, G + 1, z, 'minecraft:hay_block', axis='z')
            b.reserve(x - 1, z - 1, x + 1, z + 1)
    for i in range(4):
        b.entities.append((C + rnd.randint(-6, 6), G, C + rnd.randint(3, 8), 'minecraft:chicken', {}))
    return b
