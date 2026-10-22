"""
Города народов (2.8.0), часть 2: Сильвендар, Кхазад-Бар, Гхар'Дум, Стойбище Белого Волка, Пеплград,
пограничные крепости. Каркас — mapbuild_towns.py, Гавань и общие здания — mapbuild_towns2.py.
"""
import math
import random

from mapbuild_lib import DIRS, OPP, TByte, TInt  # noqa: F401
import mapbuild_layout as ML
from mapbuild_towns import Town, ML_blob, _ground_rect, _fence_rect, _animals, cardinal
from mapbuild_towns2 import _crafts, map_wall, bell_tower, hall


# =====================================================================
#                       СИЛЬВЕНДАР (эльфы)
# =====================================================================

def treehouse(b, cx, cz, y, r=3, wood='minecraft:birch', roof='minecraft:flowering_azalea_leaves'):
    """Домик на дереве: круглая площадка, стены из досок с окнами, крыша-крона, фонарь, кровать."""
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.4:
                continue
            b.set(x, y, z, wood + '_planks')
            for yy in range(y + 1, y + 4):
                if d > r - 0.6:
                    win = yy == y + 2 and (x == cx or z == cz)
                    if win:
                        b.set(x, yy, z, 'minecraft:light_blue_stained_glass_pane', waterlogged='false')
                    else:
                        b.set(x, yy, z, wood + '_planks')
                else:
                    b.set(x, yy, z, 'minecraft:air')
            b.set(x, y + 4, z, wood + '_planks')
    ML_blob(b, cx, y + 6, cz, r + 1.6, roof)
    b.set(cx, y + 3, cz, 'minecraft:pearlescent_froglight', axis='y')
    b.bed(cx - 1, y + 1, cz + 1, 'north', 'green')
    b.set(cx + 1, y + 1, cz - 1, 'minecraft:barrel', facing='up', open='false')


def suspension_bridge(b, ax, az, bx, bz, y):
    """Подвесной мост: настил из плит, перила из забора, лёгкий провис к середине."""
    n = int(max(abs(bx - ax), abs(bz - az), 1))
    horiz = abs(bx - ax) >= abs(bz - az)
    for s in range(n + 1):
        u = s / n
        x = round(ax + (bx - ax) * u)
        z = round(az + (bz - az) * u)
        yy = y - int(round(1.4 * math.sin(u * math.pi)))
        b.set(x, yy, z, 'minecraft:birch_slab', type='top', waterlogged='false')
        for s2 in (-1, 1):
            px, pz = (x, z + s2) if horiz else (x + s2, z)
            if (px, yy, pz) not in b.blocks:
                b.set(px, yy + 1, pz, 'minecraft:birch_fence', waterlogged='false')
        for k in range(1, 3):
            v = b.blocks.get((x, yy + k, z))
            if v and ('leaves' in v[0] or 'fence' in v[0]):
                b.set(x, yy + k, z, 'minecraft:air')


def moon_temple(b, cx, cz, r=9):
    """Храм Луны: круглый зал из кальцита со стеклянным куполом, лунный камень-аметист в середине."""
    G = b.G
    for x in range(cx - r - 1, cx + r + 2):
        for z in range(cz - r - 1, cz + r + 2):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.5:
                continue
            b.set(x, G - 1, z, 'minecraft:calcite' if int(d) % 3 else 'minecraft:polished_diorite')
            for y in range(G, G + 7):
                if d > r - 0.6:
                    col = (round(math.degrees(math.atan2(z - cz, x - cx))) // 30) % 2 == 0
                    if 2 <= y - G <= 5 and not col:
                        b.set(x, y, z, 'minecraft:white_stained_glass_pane', waterlogged='false')
                    else:
                        b.set(x, y, z, 'minecraft:calcite')
                else:
                    b.set(x, y, z, 'minecraft:air')
    for i in range(r + 1):
        rr = r * math.cos(i / r * math.pi / 2)
        y = G + 7 + int(i * 0.7)
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                if rr - 1.2 < d <= rr + 0.2:
                    b.set(x, y, z, 'minecraft:light_blue_stained_glass' if (x + z) % 3 else 'minecraft:calcite')
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            b.set(x, G, z, 'minecraft:quartz_bricks')
    b.set(cx, G + 1, cz, 'minecraft:amethyst_block')
    b.set(cx, G + 2, cz, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, z = cx + round(math.cos(a) * (r - 3)), cz + round(math.sin(a) * (r - 3))
        b.set(x, G, z, 'minecraft:pearlescent_froglight', axis='y')
        b.set(x, G + 1, z, 'minecraft:potted_azure_bluet')
    for s in (0, 1):
        b.door(cx - s, G, cz + r, 'south', 'minecraft:birch_door')
    if not hasattr(b, 'doors'):
        b.doors = []
    b.doors.append((cx, G, cz + r, 'south'))
    b.entities.append((cx + 2, G, cz, 'litrpg:elf_moonsage', {}))
    b.reserve(cx - r - 2, cz - r - 2, cx + r + 2, cz + r + 2)


def sylvendar(city, gates):
    from mapbuild_cities import giant_tree
    S, G = 256, 12   # запас под землёй: тайный подвал сюжета
    gates = list(gates) + [gates[0] + math.pi]
    t = Town('sylvendar', S, 'elf', 92, gates, sides=9, ground=G, height=118)
    b, C = t.b, t.C
    t.paint_base()
    t.walls('hedge', h=5, gate_r=4)
    PR = 20

    def glade(x, z, d):
        return 'minecraft:moss_block' if int(d) % 4 else 'minecraft:calcite'
    t.plaza(PR, glade)
    # кольцо родников вокруг Древа
    for k in range(10):
        a = 2 * math.pi * k / 10
        x, z = t.polar(a, PR - 3)
        b.set(x, G - 2, z, 'minecraft:pearlescent_froglight', axis='y')
        b.set(x, G - 1, z, 'minecraft:water', level='0')
        b.set(x, G, z, 'minecraft:lily_pad')
    giant_tree(b, C, C, 5, 54, 'minecraft:oak_log', 'minecraft:flowering_azalea_leaves', canopy=18,
               platforms=[(G + 14, 12), (G + 27, 10), (G + 39, 8)], npc='litrpg:elf_matriarch')
    from gen_mapbuild import painting_map
    for x in range(C - 4, C + 4):
        for y in range(G + 15, G + 24):
            b.set(x, y, C + 12, 'minecraft:birch_planks' if y in (G + 15, G + 23) else 'minecraft:stripped_birch_wood', axis='y')
    painting_map(b, C + 3, G + 15, C + 11, 'north')
    # проспекты-каналы: ручей с кувшинками посередине, моховые дорожки, мостики
    for a in t.gates:
        t.avenue(a, PR, half=3, blocks=['minecraft:moss_block', 'minecraft:mossy_cobblestone', 'minecraft:calcite'])
        r1 = t.ray(a) - 7
        for (x, z) in t.strip(a, PR + 2, r1, 0):
            b.set(x, G - 2, z, 'minecraft:mud')
            b.set(x, G - 1, z, 'minecraft:water', level='0')
            if (x * 7 + z * 3) % 9 == 0:
                b.set(x, G, z, 'minecraft:lily_pad')
        ca, sa = math.cos(a), math.sin(a)
        for tt in range(PR + 10, int(r1), 14):
            for w in range(-2, 3):
                x = int(round(C + ca * tt - sa * w))
                z = int(round(C + sa * tt + ca * w))
                b.set(x, G, z, 'minecraft:birch_slab', type='bottom', waterlogged='false')
        t.approach(a, half=2, blocks=['minecraft:moss_block', 'minecraft:grass_block', 'minecraft:moss_block'])
    # четыре древа-дома с площадками и подвесными мостами к Великому Древу
    for k in range(4):
        a = t.gates[0] + math.pi / 4 + k * math.pi / 2
        tx, tz = t.polar(a, 44)
        giant_tree(b, tx, tz, 2, 32, 'minecraft:birch_log', 'minecraft:azalea_leaves', canopy=10, platforms=[(G + 14, 8)],
                   npc='litrpg:elf_moonsage' if k == 0 else 'litrpg:elf_citizen')
        b.reserve(tx - 11, tz - 11, tx + 11, tz + 11)
        hx, hz = t.polar(a + 0.12, 44 + 7)
        treehouse(b, hx, hz, G + 15, 3)
        ax_, az_ = t.polar(a, 13)
        bx_, bz_ = t.polar(a, 44 - 9)
        suspension_bridge(b, ax_, az_, bx_, bz_, G + 14)
    sp = t.spot(21, 21, t.gates[0] + math.pi * 0.62, 66, inner=8)
    if sp:
        moon_temple(b, (sp[0] + sp[2]) // 2, (sp[1] + sp[3]) // 2, 9)
    sp = t.spot(18, 13, t.gates[0] - math.pi * 0.62, 64, inner=6)
    if sp:
        hall(b, sp[0], sp[1], 18, 13, t.facing_center(*sp), floors=2, roof=b.st['roofs'][1], interior='library', npc='litrpg:elf_moonsage')
    # хрустальный сад
    sp = t.spot(11, 11, t.gates[0] + math.pi * 0.9, 64)
    if sp:
        x0, z0, x1, z1 = sp
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:moss_block' if (x + z) % 3 else 'minecraft:calcite')
                r = (x * 7 + z * 13) % 10
                if r == 0:
                    b.set(x, G, z, 'minecraft:amethyst_block')
                    b.set(x, G + 1, z, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
                elif r in (3, 6):
                    b.set(x, G, z, 'minecraft:' + ('lily_of_the_valley' if r == 3 else 'blue_orchid'))
        b.set((x0 + x1) // 2, G, (z0 + z1) // 2, 'minecraft:budding_amethyst')
        b.reserve(*sp)
    import mapbuild_secrets as SEC
    SEC.town_secret(t, 'rot', t.gates[0] + math.pi / 2, 32)   # «Увядающее Древо»: хижина садовника у самого Древа
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('sylvendar'), 'litrpg:elf_citizen', 'litrpg:elf_trader', terrace=1, market=(18, 13),
               market_crafts=['herbalist', 'tailor', 'jeweler', 'alchemist', 'fletcher'], kinds=('home', 'library', 'home'), floors=(1, 2, 2),
               sizes=((6, 9), (6, 9)), density=2.4)
    t.lamps_on_avenues(every=10, fence='minecraft:birch_fence')
    t.countryside('litrpg:elf_citizen', 'elf', n_fields=4)
    rnd = random.Random('sylv-fl')
    for x in range(S):
        for z in range(S):
            if t.in_walls(x, z, 2) and b.blocks.get((x, G - 1, z), ('',))[0] == 'minecraft:grass_block' and (x, G, z) not in b.blocks:
                if rnd.random() < 0.08:
                    b.set(x, G, z, 'minecraft:' + rnd.choice(['lily_of_the_valley', 'allium', 'blue_orchid', 'azure_bluet', 'grass', 'fern']))
    SEC.finish(t, ['litrpg:elf_moonsage'])
    SEC.anchor(b, 'tree', *SEC.walk_spot(b, C + 8, C, G, 6))
    return b


# =====================================================================
#                       КХАЗАД-БАР (дварфы)
# =====================================================================

def great_hall(b, x0, z0, w, d, door, npc='litrpg:dwarf_thane'):
    """Великий зал дварфов: колонны из базальта с золотыми капителями, трон, горны, наковальни, знамёна."""
    G = b.G
    x1, z1 = x0 + w - 1, z0 + d - 1
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G - 1, z, 'minecraft:polished_deepslate' if (x + z) % 2 else 'minecraft:deepslate_tiles')
            for y in range(G, G + 14):
                if edge:
                    b.set(x, y, z, 'minecraft:deepslate_bricks' if (y - G) % 5 else 'minecraft:chiseled_deepslate')
                elif (x - x0) % 6 == 3 and (z - z0) % 6 == 3:
                    if y < G + 13:
                        b.set(x, y, z, 'minecraft:polished_basalt', axis='y')
                    else:
                        b.set(x, y, z, 'minecraft:gold_block')
                else:
                    b.set(x, y, z, 'minecraft:air')
            b.set(x, G + 14, z, 'minecraft:polished_blackstone_bricks')
            if edge and (x + z) % 2 == 0:
                b.set(x, G + 15, z, 'minecraft:polished_blackstone_brick_wall')
    ddx, ddz = DIRS[door]
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    # главный вход: проём 5 в ширину и 6 в высоту
    fx = x1 if ddx > 0 else x0 if ddx < 0 else None
    fz = z1 if ddz > 0 else z0 if ddz < 0 else None
    for s in range(-2, 3):
        for y in range(G, G + 6):
            b.set(fx if fx is not None else cx + s, y, fz if fz is not None else cz + s, 'minecraft:air')
    if not hasattr(b, 'doors'):
        b.doors = []
    b.doors.append((fx if fx is not None else cx, G, fz if fz is not None else cz, door))
    tx, tz = cx - ddx * (w // 2 - 3), cz - ddz * (d // 2 - 3)
    b.set(tx, G, tz, 'minecraft:gold_block')
    b.stair(tx + ddx, G, tz + ddz, 'minecraft:polished_blackstone_brick_stairs', OPP[door])
    b.set(tx - ddx, G + 1, tz - ddz, 'minecraft:gold_block')
    b.entities.append((tx + ddx * 2, G, tz + ddz * 2, npc, {}))
    k = 3
    while True:
        x, z = tx + ddx * k, tz + ddz * k
        if not (x0 < x < x1 and z0 < z < z1):
            break
        b.set(x, G, z, 'minecraft:orange_carpet')
        k += 1
    for s in (-1, 1):
        px = cx + (s * (w // 2 - 3) if ddz else 0)
        pz = cz + (s * (d // 2 - 3) if ddx else 0)
        b.set(px, G, pz, 'minecraft:lava_cauldron')
        b.set(px, G + 1, pz, 'minecraft:iron_bars', waterlogged='false')
        b.set(px + (1 if ddz else 0), G, pz + (1 if ddx else 0), 'minecraft:anvil', facing='north')
        b.entities.append((px + (2 if ddz else 0), G, pz + (2 if ddx else 0), 'litrpg:dwarf_smith', {}))
    for x in range(x0 + 3, x1 - 2, 6):
        for z in (z0 + 1, z1 - 1):
            b.set(x, G + 8, z, 'minecraft:orange_wall_banner', facing='south' if z == z0 + 1 else 'north')
    for x in range(x0 + 3, x1 - 2, 6):
        for z in range(z0 + 6, z1 - 2, 6):
            b.set(x, G + 13, z, 'minecraft:chain', axis='y', waterlogged='false')
            b.set(x, G + 12, z, 'minecraft:lantern', hanging='true', waterlogged='false')


def deep_mine(b, cx, cz, depth=16):
    """Рудник под городом: копёр над стволом, лестница вниз, крестовина штреков с рудой, рельсы, вагонетки, горняки."""
    G = b.G
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            b.set(cx + dx, G - 1, cz + dz, 'minecraft:polished_andesite' if (dx + dz) % 2 else 'minecraft:gravel')
    for y in range(G - depth, G):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if abs(x - cx) == 2 or abs(z - cz) == 2:
                    b.set(x, y, z, 'minecraft:deepslate_bricks' if y % 4 else 'minecraft:spruce_planks')
                else:
                    b.set(x, y, z, 'minecraft:air')
        b.set(cx, y, cz - 1, 'minecraft:ladder', facing='south', waterlogged='false')
    fy = G - depth
    for x in range(cx - 1, cx + 2):
        for z in range(cz - 1, cz + 2):
            b.set(x, fy - 1, z, 'minecraft:cobblestone')
    ores = ['minecraft:deepslate_iron_ore', 'minecraft:deepslate_coal_ore', 'minecraft:deepslate_copper_ore', 'minecraft:deepslate_gold_ore',
            'minecraft:deepslate_redstone_ore', 'minecraft:deepslate_lapis_ore', 'minecraft:deepslate_diamond_ore']
    for (ddx, ddz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in range(2, 16):
            x, z = cx + ddx * k, cz + ddz * k
            for w in (-1, 0, 1):
                px, pz = (x, z + w) if ddx else (x + w, z)
                for y in range(fy, fy + 3):
                    b.set(px, y, pz, 'minecraft:air')
                b.set(px, fy - 1, pz, 'minecraft:cobblestone')
                b.set(px, fy + 3, pz, 'minecraft:deepslate')
            for w in (-2, 2):
                px, pz = (x, z + w) if ddx else (x + w, z)
                for y in range(fy - 1, fy + 4):
                    n = (px * 7 + y * 13 + pz * 5) % 23
                    b.set(px, y, pz, ores[n % len(ores)] if n < 6 else 'minecraft:deepslate' if n % 3 else 'minecraft:tuff')
            b.set(x, fy, z, 'minecraft:rail', shape='east_west' if ddx else 'north_south', waterlogged='false')
            if k % 4 == 0:
                for w in (-1, 1):
                    px, pz = (x, z + w) if ddx else (x + w, z)
                    for y in range(fy, fy + 3):
                        b.set(px, y, pz, 'minecraft:spruce_log', axis='y')
                b.set(x, fy + 2, z, 'minecraft:lantern', hanging='true', waterlogged='false')
        ex, ez = cx + ddx * 16, cz + ddz * 16
        for w in range(-2, 3):
            for y in range(fy - 1, fy + 4):
                px, pz = (ex, ez + w) if ddx else (ex + w, ez)
                b.set(px, y, pz, ores[(w + y) % len(ores)] if (w + y) % 3 == 0 else 'minecraft:deepslate')
        b.entities.append((cx + ddx * 9, fy, cz + ddz * 9, 'minecraft:minecart', {}))
        b.entities.append((cx + ddx * 12, fy, cz + ddz * 12, 'litrpg:dwarf_citizen', {}))
    # копёр над стволом
    for dx in (-3, 3):
        for dz in (-3, 3):
            for y in range(G, G + 10):
                b.set(cx + dx, y, cz + dz, 'minecraft:dark_oak_log', axis='y')
    for x in range(cx - 3, cx + 4):
        for z in range(cz - 3, cz + 4):
            if abs(x - cx) == 3 or abs(z - cz) == 3:
                b.set(x, G + 10, z, 'minecraft:dark_oak_planks')
    for x in range(cx - 1, cx + 2):
        for z in (cz - 2, cz + 2):
            b.set(x, G, z, 'minecraft:dark_oak_fence', waterlogged='false')
    for z in range(cz - 1, cz + 2):
        b.set(cx + 2, G, z, 'minecraft:dark_oak_fence', waterlogged='false')
    for dx in range(-3, 4):
        b.set(cx + dx, G + 11, cz, 'minecraft:dark_oak_log', axis='x')
    for y in range(G + 2, G + 11):
        b.set(cx, y, cz, 'minecraft:chain', axis='y', waterlogged='false')
    b.set(cx + 4, G, cz + 2, 'minecraft:raw_iron_block')
    b.set(cx + 4, G, cz + 3, 'minecraft:coal_block')
    b.set(cx + 5, G, cz + 3, 'minecraft:raw_copper_block')
    b.entities.append((cx - 4, G, cz + 4, 'litrpg:dwarf_citizen', {}))
    b.reserve(cx - 5, cz - 5, cx + 5, cz + 5)


def rune_pillar(b, x, z, h=4):
    G = b.G
    for y in range(G, G + h):
        b.set(x, y, z, 'minecraft:chiseled_deepslate' if y % 2 else 'minecraft:polished_deepslate')
    b.set(x, G + h, z, 'minecraft:lantern', hanging='false', waterlogged='false')


def khazad(city, gates):
    S, G = 256, 20
    t = Town('khazad', S, 'dwarf', 92, gates, sides=8, ground=G, height=110)
    b, C = t.b, t.C
    t.paint_base()
    t.walls('stone', h=14, thick=5, tower_r=6, tower_h=20, roof='minecraft:polished_blackstone_bricks')
    gate_a = t.gates[0]
    PR = 16

    def forge_sq(x, z, d):
        return 'minecraft:polished_deepslate' if int(d) % 4 == 0 else 'minecraft:deepslate_tiles' if (x + z) % 2 else 'minecraft:polished_andesite'
    t.plaza(PR, forge_sq)
    # великая наковальня на площади
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            b.set(C + dx, G, C + dz, 'minecraft:polished_blackstone_bricks')
    for dx in range(-1, 2):
        b.set(C + dx, G + 1, C, 'minecraft:polished_blackstone_bricks')
    for dx in range(-3, 4):
        for dz in range(-1, 2):
            b.set(C + dx, G + 2, C + dz, 'minecraft:iron_block')
    b.set(C + 4, G + 2, C, 'minecraft:iron_block')
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        x, z = t.polar(a, 9)
        b.set(x, G, z, 'minecraft:lava_cauldron')
        b.set(x, G + 1, z, 'minecraft:iron_bars', waterlogged='false')
        rune_pillar(b, *t.polar(a, PR - 2), h=5)
    for a in t.gates:
        t.avenue(a, PR, half=3, blocks=['minecraft:polished_andesite', 'minecraft:deepslate_tiles', 'minecraft:stone_bricks'])
        t.approach(a, half=2, blocks=['minecraft:gravel', 'minecraft:cobblestone', 'minecraft:andesite'])
        for s in (-1, 1):
            r = t.ray(a) - 14
            sx = int(C + math.cos(a) * r - math.sin(a) * 9 * s)
            sz = int(C + math.sin(a) * r + math.cos(a) * 9 * s)
            if b.free(sx - 3, sz - 3, sx + 3, sz + 3):
                b.statue(sx, sz, h=20, block='minecraft:deepslate_bricks', accent='minecraft:gold_block')
                b.reserve(sx - 3, sz - 3, sx + 3, sz + 3)
    sp = t.spot(34, 24, gate_a + math.pi, 46, inner=6)
    if sp:
        door = t.facing_center(*sp)
        great_hall(b, sp[0], sp[1], 34, 24, door)
        ddx, ddz = DIRS[door]
        map_wall(t, (sp[0] + sp[2]) // 2 + ddx * 22, (sp[1] + sp[3]) // 2 + ddz * 17, frame='minecraft:polished_deepslate',
                 face='minecraft:smooth_basalt', cap='minecraft:polished_deepslate_wall')
    sp = t.spot(13, 13, gate_a + math.pi / 2, 52, inner=8)
    if sp:
        deep_mine(b, (sp[0] + sp[2]) // 2, (sp[1] + sp[3]) // 2, depth=17)
    import mapbuild_crafts as MCR
    sp = t.spot(18, 12, gate_a - math.pi / 2, 52)
    if sp:
        MCR.workshop(b, sp[0], sp[1], 'innkeeper', t.facing_center(*sp), floors=2, w=18, d=12, check=False)
    sp = t.spot(20, 11, gate_a + 0.5, t.WR - 24)
    if sp:
        b.house(sp[0], sp[1], 20, 11, floors=2, door=t.facing_center(*sp), interior='guard', npc='litrpg:dwarf_ironguard')
    import mapbuild_secrets as SEC
    SEC.town_secret(t, 'goblin_forge', gate_a - 0.7, 60)    # «Рунный ключ»: сторожка старой штольни
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('khazad', drop=('innkeeper',)), 'litrpg:dwarf_citizen', 'litrpg:dwarf_smith', terrace=2, market=(20, 13),
               market_crafts=['jeweler', 'butcher', 'baker', 'alchemist', 'leatherworker'], kinds=('home', 'smith', 'shop', 'home'),
               floors=(1, 2, 2, 3), density=1.4)
    for (a, r0, r1, half) in t.avenues:
        for tt in range(int(r0) + 5, int(r1) - 6, 10):
            for s in (-1, 1):
                x = int(round(C + math.cos(a) * tt - math.sin(a) * (half + 1) * s))
                z = int(round(C + math.sin(a) * tt + math.cos(a) * (half + 1) * s))
                if ML._air(b, x, G, z) and ML._air(b, x, G + 1, z):
                    rune_pillar(b, x, z, 4)
    t.countryside('litrpg:dwarf_citizen', 'dwarf', n_fields=6)
    SEC.finish(t, ['litrpg:dwarf_thane', 'litrpg:dwarf_runepriest'])
    return b


# =====================================================================
#                       ГХАР'ДУМ (орки)
# =====================================================================

def longhouse(b, x0, z0, w, d, npc=None, wall='minecraft:spruce_log', trim='minecraft:stripped_spruce_log',
              roof=('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks'), banner='minecraft:red_wall_banner',
              fur='minecraft:brown_carpet'):
    """Длинный дом: бревенчатые стены, крутая крыша, двери в торцах, очаги вдоль середины, лавки, шкуры, знамёна."""
    G = b.G
    x1, z1 = x0 + w - 1, z0 + d - 1
    along_x = w >= d
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G - 1, z, 'minecraft:cobblestone' if edge else 'minecraft:spruce_planks')
            for y in range(G, G + 4):
                if edge:
                    corner = x in (x0, x1) and z in (z0, z1)
                    if corner:
                        b.set(x, y, z, trim, axis='y')
                    else:
                        b.set(x, y, z, wall, axis='x' if z in (z0, z1) else 'z')
                else:
                    b.set(x, y, z, 'minecraft:air')
    b.roof(x0, z0, x1, z1, G + 4, roof)
    if not hasattr(b, 'doors'):
        b.doors = []
    if along_x:
        cz = (z0 + z1) // 2
        for x, f in ((x0, 'west'), (x1, 'east')):
            b.door(x, G, cz, f)
            b.doors.append((x, G, cz, f))
        hearths = [(x, cz) for x in range(x0 + 4, x1 - 3, 5)]
    else:
        cx = (x0 + x1) // 2
        for z, f in ((z0, 'north'), (z1, 'south')):
            b.door(cx, G, z, f)
            b.doors.append((cx, G, z, f))
        hearths = [(cx, z) for z in range(z0 + 4, z1 - 3, 5)]
    for (x, z) in hearths:
        b.set(x, G - 1, z, 'minecraft:cobblestone')
        b.set(x, G, z, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            near_wall = (z in (z0 + 1, z1 - 1)) if along_x else (x in (x0 + 1, x1 - 1))
            near_door = abs(x - (x0 + x1) // 2) <= 1 and not along_x or abs(z - (z0 + z1) // 2) <= 1 and along_x
            if near_wall and (x + z) % 3 and not (near_door and (x in (x0 + 1, x1 - 1) if along_x else z in (z0 + 1, z1 - 1))):
                facing = ('south' if z == z0 + 1 else 'north') if along_x else ('east' if x == x0 + 1 else 'west')
                b.stair(x, G, z, roof[0], OPP[facing])
            elif not near_wall and (x, z) not in hearths and (x * 3 + z) % 5 == 0:
                b.set(x, G, z, fur)
    for (x, z) in ((x0 + 2, z0 + 1), (x1 - 2, z0 + 1), (x0 + 2, z1 - 1), (x1 - 2, z1 - 1)):
        b.set(x, G + 2, z, banner, facing='south' if z == z0 + 1 else 'north')
    for (x, z) in hearths:
        hx, hz = (x, z + 2) if along_x else (x + 2, z)
        b.set(hx, G + 3, hz, 'minecraft:lantern', hanging='true', waterlogged='false')
    if npc:
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        b.entities.append((cx + (1 if along_x else 2), G, cz + (2 if along_x else 1), npc, {}))


def tent_camp(t, w, d, a, r, colors, npcs, ground, rack='minecraft:spruce_fence'):
    """Становище шатров внутри стен: кольцо шатров вокруг костра, стойки с оружием/шкурами, бочки, дрова."""
    from mapbuild_cities import tent, bonfire
    b, G = t.b, t.G
    sp = t.spot(w, d, a, r, inner=6)
    if not sp:
        return 0
    x0, z0, x1, z1 = sp
    rnd = random.Random(t.cid + 'camp')
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, b.bp(ground, x // 2, 0, z // 2))
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    bonfire(b, cx, cz)
    rx, rz = (x1 - x0) / 2 - 4, (z1 - z0) / 2 - 4
    n = 0
    k = 0
    while k < 8:
        ang = 2 * math.pi * k / 8 + 0.2
        tx, tz = int(round(cx + math.cos(ang) * rx)), int(round(cz + math.sin(ang) * rz))
        door = cardinal(math.atan2(cz - tz, cx - tx))
        if b.free(tx - 3, tz - 3, tx + 3, tz + 3):
            tent(b, tx, tz, 3, rnd.choice(colors), npc=rnd.choice(npcs), door=door)
            n += 1
        k += 1
    for (dx, dz) in ((-3, 0), (3, 0)):
        x, z = cx + dx, cz + dz + 3
        for y in range(G, G + 2):
            b.set(x, y, z, rack, waterlogged='false')
        b.set(x, G + 2, z, rack, waterlogged='false')
    b.set(cx - 1, G, cz - 4, 'minecraft:barrel', facing='up', open='false')
    b.set(cx + 1, G, cz - 4, 'minecraft:spruce_log', axis='x')
    b.set(cx + 2, G, cz - 4, 'minecraft:spruce_log', axis='x')
    b.reserve(x0, z0, x1, z1)
    return n


def _tents_in_yards(t, n, colors, npcs, seed):
    from mapbuild_cities import tent
    b, C, G = t.b, t.C, t.G
    rnd = random.Random(seed)
    made = 0
    for k in range(n * 6):
        if made >= n:
            break
        a = rnd.random() * 2 * math.pi
        r = rnd.uniform(18, t.WR - 8)
        x, z = int(C + math.cos(a) * r), int(C + math.sin(a) * r)
        pts = [(x + dx, z + dz) for dx in (-4, 0, 4) for dz in (-4, 0, 4)]
        if not t.in_walls(x, z, 8) or not b.free(x - 4, z - 4, x + 4, z + 4) or not all(p in t.org.cell and p not in t.org.foot for p in pts):
            continue
        hh = t.org.ground(x, z)
        if any(t.org.ground(*p) != hh for p in pts):
            continue
        if not all(ML._air(b, px, G + hh, pz) for px, pz in pts):
            continue
        b.G = G + hh
        tent(b, x, z, 3, rnd.choice(colors), npc=rnd.choice(npcs))
        b.G = G
        made += 1
    return made


def ghardum(city, gates):
    from mapbuild_cities import arena, bonfire, totem
    S, G = 224, 12
    t = Town('ghardum', S, 'orc', 80, gates, sides=9, ground=G, height=80)
    b, C = t.b, t.C
    t.paint_base()
    t.walls('palisade', h=8, gate_r=4)
    gate_a = t.gates[0]
    for x in range(C - 21, C + 22):
        for z in range(C - 21, C + 22):
            if math.hypot(x - C, z - C) <= 21.4:
                b.set(x, G - 1, z, b.bp(['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'], x, 0, z))
    arena(b, C, C, 13)
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        x, z = t.polar(a, 20)
        (totem if k % 2 else bonfire)(b, x, z)
    b.reserve(C - 21, C - 21, C + 21, C + 21)
    for a in t.gates:
        t.avenue(a, 21, half=3, blocks=['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'])
        t.approach(a, half=2)
    sp = t.spot(28, 14, gate_a + math.pi, 44, inner=6)
    if sp:
        longhouse(b, sp[0], sp[1], 28, 14, npc='litrpg:orc_warlord')
        cx = (sp[0] + sp[2]) // 2
        cz = (sp[1] + sp[3]) // 2
        side = 1 if cz < C else -1
        for s in (-1, 1):
            totem(b, cx + s * 6, cz + side * 10, 9)
        b.reserve(sp[0], min(cz, cz + side * 11), sp[2], max(cz, cz + side * 11))
        map_wall(t, cx, cz + side * 16, frame='minecraft:stripped_dark_oak_log', face='minecraft:spruce_planks', cap='minecraft:dark_oak_fence')
    # загон варгов
    sp = t.spot(14, 10, gate_a + math.pi / 2, 50)
    if sp:
        x0, z0, x1, z1 = sp
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
        _fence_rect(b, x0, z0, x1, z1, 'minecraft:spruce_fence', gate_side=t.facing_center(*sp))
        _animals(b, t.rnd, x0, z0, x1, z1, ['minecraft:wolf'], 5)
        for x in range(x0 + 1, x0 + 4):
            b.set(x, G, z0 + 1, 'minecraft:bone_block', axis='x')
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    # кузня-яма под навесом
    sp = t.spot(13, 13, gate_a - math.pi / 2, 48)
    if sp:
        x0, z0, x1, z1 = sp
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:packed_mud')
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                b.set(cx + dx, G - 1, cz + dz, 'minecraft:magma_block')
        b.set(cx, G, cz, 'minecraft:lava_cauldron')
        b.set(cx, G + 1, cz, 'minecraft:iron_bars', waterlogged='false')
        for (dx, dz) in ((-3, 0), (3, 0), (0, 3)):
            b.set(cx + dx, G, cz + dz, 'minecraft:anvil', facing='north')
        b.set(cx, G, cz - 3, 'minecraft:grindstone', face='floor', facing='north')
        for (dx, dz) in ((-5, -5), (5, -5), (-5, 5), (5, 5)):
            for y in range(G, G + 4):
                b.set(cx + dx, y, cz + dz, 'minecraft:spruce_log', axis='y')
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                if not (abs(dx) <= 1 and abs(dz) <= 1):
                    b.set(cx + dx, G + 4, cz + dz, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
        b.entities.append((cx + 2, G, cz + 2, 'litrpg:orc_citizen', {}))
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    n1 = tent_camp(t, 30, 26, gate_a + math.pi * 0.7, 52, ['brown', 'red', 'black', 'gray'], ['litrpg:orc_citizen', None, 'litrpg:orc_trader'],
                   ['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'])
    n2 = tent_camp(t, 26, 24, gate_a - math.pi * 0.75, 54, ['brown', 'red', 'black'], ['litrpg:orc_citizen', None],
                   ['minecraft:coarse_dirt', 'minecraft:packed_mud'])
    print('  шатров:', n1 + n2)
    import mapbuild_secrets as SEC
    SEC.town_secret(t, 'bloodcap', gate_a + math.pi * 0.35, 40)   # «Кровь на арене»: лавка менялы с ямой под полом
    SEC.anchor(b, 'arena', *SEC.walk_spot(b, C + 15, C, G, 5))
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('ghardum'), 'litrpg:orc_citizen', 'litrpg:orc_trader', terrace=1, market=(18, 12),
               market_crafts=['butcher', 'leatherworker', 'herbalist', 'fletcher'], floors=(1, 1, 2), sizes=((6, 9), (6, 8)), npc_chance=0.35,
               kinds=('home', 'guard', 'home'), density=1.2)
    t.lamps_on_avenues(every=11)
    t.countryside('litrpg:orc_citizen', 'orc', n_fields=6)
    SEC.finish(t, ['litrpg:orc_warlord'])
    return b


# =====================================================================
#                 СТОЙБИЩЕ БЕЛОГО ВОЛКА (зверолюды)
# =====================================================================

def wolfden(city, gates):
    from mapbuild_cities import tent, bonfire, totem
    S, G = 192, 12
    gates = list(gates) + [gates[0] + math.pi]
    t = Town('wolfden', S, 'beast', 68, gates, sides=8, ground=G, height=72)
    b, C = t.b, t.C
    t.paint_base()
    t.walls('palisade', h=7, gate_r=4)
    gate_a = t.gates[0]
    PR = 14

    def ring(x, z, d):
        return 'minecraft:podzol' if int(d) % 3 else 'minecraft:coarse_dirt'
    t.plaza(PR, ring)
    totem(b, C, C, 16)
    for k in range(4):
        bonfire(b, *t.polar(k * math.pi / 2 + math.pi / 4, 7))
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, z = t.polar(a, PR - 1)
        for y in range(G, G + 3):
            b.set(x, y, z, 'minecraft:spruce_fence', waterlogged='false')
        b.set(x, G + 3, z, 'minecraft:skeleton_skull', rotation=str((k * 2 + 8) % 16))
    for a in t.gates:
        t.avenue(a, PR, half=2, blocks=['minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:dirt_path'])
        t.approach(a, half=2)
    sp = t.spot(24, 12, gate_a + math.pi * 0.75, 36, inner=6)
    if sp:
        longhouse(b, sp[0], sp[1], 24, 12, npc='litrpg:beast_alpha', banner='minecraft:brown_wall_banner', fur='minecraft:white_carpet')
        map_wall(t, (sp[0] + sp[2]) // 2, (sp[1] + sp[3]) // 2 + 12, frame='minecraft:stripped_spruce_log', face='minecraft:spruce_planks',
                 cap='minecraft:spruce_fence')
    sp = t.spot(12, 20, gate_a + math.pi * 0.35, 38, inner=6)
    if sp:
        longhouse(b, sp[0], sp[1], 12, 20, npc='litrpg:beast_trader', banner='minecraft:white_wall_banner', fur='minecraft:light_gray_carpet')
    sp = t.spot(9, 9, gate_a - math.pi / 2, 34)
    if sp:
        x0, z0, x1, z1 = sp
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        tent(b, cx, cz, 4, 'green', npc='litrpg:beast_citizen', door=t.facing_center(*sp))
        b.set(cx + 1, G, cz - 1, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='false', has_bottle_2='true')
        b.set(cx - 1, G, cz - 1, 'minecraft:cauldron')
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    n1 = tent_camp(t, 26, 24, gate_a - math.pi * 0.3, 40, ['brown', 'white', 'light_gray'], ['litrpg:beast_citizen', None],
                   ['minecraft:podzol', 'minecraft:coarse_dirt'])
    print('  шатров:', n1)
    import mapbuild_secrets as SEC
    SEC.town_secret(t, 'den', gate_a - math.pi * 0.7, 36)   # «Белый волк»: нора шамана
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('wolfden'), 'litrpg:beast_citizen', 'litrpg:beast_trader', terrace=1, market=(14, 10),
               market_crafts=['leatherworker', 'herbalist', 'butcher'], floors=(1, 1, 2), sizes=((5, 8), (6, 8)), npc_chance=0.35,
               kinds=('home', 'home', 'guard'), density=1.2)
    t.lamps_on_avenues(every=10)
    t.countryside('litrpg:beast_citizen', 'beast', n_fields=4)
    SEC.finish(t, ['litrpg:beast_alpha'])
    SEC.anchor(b, 'totem', *SEC.walk_spot(b, C + 4, C + 4, G, 6))
    return b


# =====================================================================
#                         ПЕПЛГРАД (демониды)
# =====================================================================

def obsidian_spire(b, x, z, h=14):
    G = b.G
    for y in range(G - 1, G + h):
        r = 1.8 * (1 - (y - G) / (h + 3))
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if math.hypot(dx, dz) <= r + 0.3:
                    b.set(x + dx, y, z + dz, 'minecraft:crying_obsidian' if (y + dx) % 6 == 0 else 'minecraft:obsidian')
    b.set(x, G + h, z, 'minecraft:soul_soil')
    b.set(x, G + h + 1, z, 'minecraft:soul_fire')
    b.reserve(x - 2, z - 2, x + 2, z + 2)


def ashgrad(city, gates):
    from mapbuild_cities import ziggurat
    S, G = 224, 12
    t = Town('ashgrad', S, 'demon', 78, gates, sides=8, ground=G, height=86)
    b, C = t.b, t.C
    t.paint_base()
    # лавовый ров за стенами (мосты у ворот), под лавой — магма и чернокамень; ограда по внешнему краю
    for x in range(S):
        for z in range(S):
            if not t.in_walls(x, z, -4) and t.in_walls(x, z, -8):
                a = math.atan2(z - C, x - C)
                bridge = any(abs(((a - g + math.pi) % (2 * math.pi)) - math.pi) < 0.07 for g in t.gates)
                for y in range(G - 4, G - 1):
                    b.set(x, y, z, 'minecraft:blackstone')
                if bridge:
                    b.set(x, G - 1, z, 'minecraft:nether_bricks')
                else:
                    b.set(x, G - 2, z, 'minecraft:magma_block')
                    b.set(x, G - 1, z, 'minecraft:lava', level='0')
            elif not t.in_walls(x, z, -9) and t.in_walls(x, z, -10) and (x + z) % 2 == 0:
                a = math.atan2(z - C, x - C)
                if not any(abs(((a - g + math.pi) % (2 * math.pi)) - math.pi) < 0.09 for g in t.gates):
                    b.set(x, G, z, 'minecraft:nether_brick_fence', waterlogged='false')
    t.walls('stone', h=12, thick=3, tower_r=4, roof='minecraft:nether_bricks')
    PR = 20
    for a in t.gates:
        t.avenue(a, PR, half=3, blocks=['minecraft:polished_blackstone', 'minecraft:blackstone', 'minecraft:polished_blackstone_bricks'])
        t.approach(a, half=2, blocks=['minecraft:soul_soil', 'minecraft:blackstone', 'minecraft:basalt'])

    def ash_sq(x, z, d):
        return 'minecraft:polished_blackstone_bricks' if int(d) % 5 else 'minecraft:gilded_blackstone'
    t.plaza(PR, ash_sq)
    top = ziggurat(b, C, C, 5, 15)
    b.entities.append((C, top, C + 2, 'litrpg:demon_archfiend', {}))
    for k in range(6):
        obsidian_spire(b, *t.polar(2 * math.pi * k / 6 + 0.25, PR - 1), h=12 + (k % 2) * 4)
    from gen_mapbuild import painting_map
    mx, mz = t.polar(t.gates[0] + 0.6, PR + 4)
    for x in range(mx - 4, mx + 4):
        for y in range(G, G + 9):
            b.set(x, y, mz, 'minecraft:polished_blackstone_bricks')
    painting_map(b, mx, G, mz + 1, 'south')
    b.reserve(mx - 5, mz - 1, mx + 4, mz + 2)
    import mapbuild_crafts as MCR
    sp = t.spot(16, 12, t.gates[0] + math.pi * 0.6, 48)
    if sp:
        MCR.workshop(b, sp[0], sp[1], 'blacksmith', t.facing_center(*sp), floors=2, w=16, d=12, check=False)
    sp = t.spot(18, 14, t.gates[0] - math.pi * 0.6, 48)
    if sp:
        hall(b, sp[0], sp[1], 18, 14, t.facing_center(*sp), floors=3, roof=b.st['roofs'][1], interior='library', npc='litrpg:demon_trader',
             tower='minecraft:nether_bricks')
    import mapbuild_secrets as SEC
    SEC.town_secret(t, 'void', t.gates[0] + math.pi * 0.3, 46)   # «Шёпот из обсидиана»: часовня с комнатой шёпота
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('ashgrad', drop=('blacksmith',)), 'litrpg:demon_citizen', 'litrpg:demon_trader', terrace=2, market=(18, 13),
               market_crafts=['alchemist', 'jeweler', 'tailor', 'butcher'], kinds=('home', 'library', 'shop'), density=1.4)
    for (a, r0, r1, half) in t.avenues:
        for tt in range(int(r0) + 5, int(r1) - 6, 9):
            for s in (-1, 1):
                x = int(round(C + math.cos(a) * tt - math.sin(a) * (half + 1) * s))
                z = int(round(C + math.sin(a) * tt + math.cos(a) * (half + 1) * s))
                if ML._air(b, x, G, z) and ML._air(b, x, G + 1, z):
                    b.set(x, G, z, 'minecraft:nether_brick_fence', waterlogged='false')
                    b.set(x, G + 1, z, 'minecraft:nether_brick_fence', waterlogged='false')
                    b.set(x, G + 2, z, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
    t.countryside('litrpg:demon_citizen', 'demon', n_fields=10, wall_pad=12)
    SEC.finish(t, ['litrpg:demon_cultist'])
    return b


# =====================================================================
#                   ПОГРАНИЧНЫЕ КРЕПОСТИ (люди)
# =====================================================================

FORTS = {
    'dawnfort': dict(banner='minecraft:blue_banner', roof='minecraft:deepslate_tiles', found=None),
    'northwatch': dict(banner='minecraft:light_blue_banner', roof='minecraft:spruce_planks', found=None),
    'blackbastion': dict(banner='minecraft:black_banner', roof='minecraft:polished_blackstone_bricks',
                         found=['minecraft:deepslate_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:polished_deepslate']),
}


def fort(city, gates, cid):
    import mapbuild_castle as MCS
    cfg = FORTS[cid]
    S, G = 192, 12
    t = Town(cid, S, 'human', 68, gates, sides=8, ground=G, height=80)
    b, C = t.b, t.C
    b.st = dict(b.st, banner=cfg['banner'])
    if cfg['found']:
        b.st['found'] = cfg['found']
    t.st = b.st
    t.paint_base()
    t.walls('stone', h=11, thick=3, tower_r=4, roof=cfg['roof'])
    gate_a = t.gates[0]
    PR = 10
    t.plaza(PR)
    b.well(C, C)
    for k in range(4):
        b.lamp_post(*t.polar(k * math.pi / 2 + math.pi / 4, PR - 2), 3)
    for a in t.gates:
        t.avenue(a, PR, half=2)
        t.approach(a, half=2)
    # цитадель: внутренняя стена, донжон с колокольней, конюшня, кузня, плац
    ha = gate_a + math.pi
    sp = t.spot(46, 40, ha, 34, margin=1, inner=6, search=40)
    if sp:
        cx0, cz0, cx1, cz1 = sp
        for x in range(cx0, cx1 + 1):
            for z in range(cz0, cz1 + 1):
                b.set(x, G - 1, z, 'minecraft:stone_bricks' if (x + z) % 3 else 'minecraft:polished_andesite')
        door = t.facing_center(*sp)
        gx, gz = {'south': ((cx0 + cx1) // 2, cz1), 'north': ((cx0 + cx1) // 2, cz0), 'east': (cx1, (cz0 + cz1) // 2),
                  'west': (cx0, (cz0 + cz1) // 2)}[door]
        for (ax, az, bx, bz) in ((cx0, cz0, cx1, cz0), (cx0, cz1, cx1, cz1), (cx0, cz0, cx0, cz1), (cx1, cz0, cx1, cz1)):
            b.wall_line(ax, az, bx, bz, h=9, thick=1, gates=[(gx, gz, 3)])
        for (x, z) in ((cx0, cz0), (cx1, cz0), (cx0, cz1), (cx1, cz1)):
            b.round_tower(x, z, 3, 14, roof_block=cfg['roof'])
        ddx, ddz = DIRS[door]
        kw, kd = (18, 14) if door in ('south', 'north') else (14, 18)
        kx = (cx0 + cx1) // 2 - kw // 2 - ddx * 9
        kz = (cz0 + cz1) // 2 - kd // 2 - ddz * 8
        b.house(kx, kz, kw, kd, floors=3, door=door, roof=b.st['roofs'][0], interior='guard', npc='litrpg:human_knight')
        b.banner_wall(kx + kw // 2 + (kw // 2 + 1) * ddx, G + 9, kz + kd // 2 + (kd // 2 + 1) * ddz, door)
        if door in ('south', 'north'):
            yz = cz1 - 12 if door == 'south' else cz0 + 3
            MCS.stable(b, cx0 + 3, yz, 9, 10, open_side='east')
            MCS.forge(b, cx1 - 11, yz, 9, 8)
        else:
            yx = cx1 - 12 if door == 'east' else cx0 + 3
            MCS.stable(b, yx, cz0 + 3, 9, 10, open_side='east' if door == 'west' else 'west')
            MCS.forge(b, yx, cz1 - 10, 9, 8)
        for (x, y, z), (bl, _) in list(b.blocks.items()):
            if cx0 <= x <= cx1 and cz0 <= z <= cz1 and bl in ('minecraft:chest', 'minecraft:barrel'):
                b.nbt.setdefault((x, y, z), {}).setdefault('LootTable', 'litrpg:chests/city_armory' if bl == 'minecraft:chest' else 'litrpg:chests/city_barrel')
        for k in range(3):
            b.entities.append((gx - ddx * 4 + (k - 1) * abs(ddz), G, gz - ddz * 4 + (k - 1) * abs(ddx), 'litrpg:human_guard', {}))
        # остаток двора — трава с кустами
        for x in range(cx0 + 1, cx1):
            for z in range(cz0 + 1, cz1):
                if (x, G, z) not in b.blocks and b.blocks.get((x, G - 1, z), ('',))[0] in ('minecraft:stone_bricks', 'minecraft:polished_andesite'):
                    if abs(x - gx) > 2 and abs(z - gz) > 2 and (x * 7 + z * 3) % 11 == 0:
                        b.set(x, G, z, 'minecraft:barrel', facing='up', open='false') if (x + z) % 2 else None
        b.reserve(cx0 - 1, cz0 - 1, cx1 + 1, cz1 + 1)
        map_wall(t, gx + ddx * 6, gz + ddz * 6)
    for k, a in enumerate(t.gates):
        sp = t.spot(16, 10, a + 0.45, t.WR - 18)
        if sp:
            b.house(sp[0], sp[1], 16, 10, floors=2, door=t.facing_center(*sp), interior='guard', npc='litrpg:human_knight')
    import mapbuild_secrets as SEC
    SEC.town_secret(t, {'dawnfort': 'goblin_gold', 'northwatch': 'ice_crypt', 'blackbastion': 'necro'}[cid], gate_a + math.pi / 2, 40)
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('fort'), 'litrpg:human_citizen', 'litrpg:human_merchant', terrace=1, market=(14, 10),
               market_crafts=['baker', 'butcher', 'farmer'], kinds=('home', 'guard', 'smith'), floors=(1, 2, 2), density=1.6)
    t.lamps_on_avenues(every=9)
    t.countryside('litrpg:human_citizen', 'human', n_fields=10)
    SEC.finish(t, ['litrpg:human_knight'])
    return b
