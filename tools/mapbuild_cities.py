"""Города народов и пограничные крепости для gen_mapbuild.py."""
import math
import random

from mapbuild_lib import City, DIRS, OPP


def angle_dir(a):
    """Сторона света по углу (радианы, 0 = восток, pi/2 = юг)."""
    a = a % (2 * math.pi)
    k = int(round(a / (math.pi / 2))) % 4
    return ['east', 'south', 'west', 'north'][k]


# ---------------- общие элементы ----------------

def ground_disk(b, C, R, street_fn, grass='minecraft:grass_block'):
    st = b.st
    for x in range(b.size[0]):
        for z in range(b.size[2]):
            r = math.hypot(x - C, z - C)
            if r > R:
                continue
            if street_fn(x, z):
                b.set(x, b.G - 1, z, b.bp(st['street'], x, 0, z))
            else:
                if grass in ('minecraft:grass_block', 'minecraft:podzol'):
                    b.set(x, b.G - 1, z, grass, snowy='false')
                else:
                    b.set(x, b.G - 1, z, grass)


def approach(b, C, a, from_r, width=2):
    """Подъездная дорога от ворот (угол a) до края плитки."""
    st = b.st
    for t in range(int(from_r), int(C * 1.42)):
        cx, cz = C + math.cos(a) * t, C + math.sin(a) * t
        for w in range(-width, width + 1):
            x = int(round(cx - math.sin(a) * w))
            z = int(round(cz + math.cos(a) * w))
            if 0 <= x < b.size[0] and 0 <= z < b.size[2]:
                b.set(x, b.G - 1, z, b.bp(st['path'], x, 0, z))


def ring_wall(b, C, R, gate_angles, h=10, sides=8, kind='stone', tower_roof='minecraft:dark_prismarine', skip=None):
    """Стена-многоугольник с воротами в направлениях gate_angles. kind: stone | palisade."""
    pts = []
    for k in range(sides):
        a = 2 * math.pi * (k + 0.5) / sides
        pts.append((C + round(math.cos(a) * R), C + round(math.sin(a) * R)))
    gates = [(C + round(math.cos(a) * R * math.cos(math.pi / sides)), C + round(math.sin(a) * R * math.cos(math.pi / sides)), 4)
             for a in gate_angles]
    if kind == 'palisade':
        n = 720
        for i in range(n):
            a = 2 * math.pi * i / n
            if skip and skip(a):
                continue
            x, z = C + round(math.cos(a) * R), C + round(math.sin(a) * R)
            if any(math.hypot(x - gx, z - gz) < 4 for gx, gz, _ in gates):
                continue
            hh = h + (x * 7 + z * 3) % 2
            for y in range(b.G - 1, b.G + hh):
                b.set(x, y, z, 'minecraft:spruce_log', axis='y')
            b.set(x, b.G + hh, z, 'minecraft:spruce_fence', waterlogged='false')
        for gx, gz, _ in gates:
            for dx in (-4, 4):
                for dz in (-4, 4):
                    pass
            ga = math.atan2(gz - C, gx - C)
            for s in (-4, 4):
                tx = int(round(gx - math.sin(ga) * s))
                tz = int(round(gz + math.cos(ga) * s))
                for y in range(b.G - 1, b.G + h + 4):
                    for dx in range(-1, 2):
                        for dz in range(-1, 2):
                            b.set(tx + dx, y, tz + dz, 'minecraft:stripped_spruce_log', axis='y')
                b.set(tx, b.G + h + 4, tz, 'minecraft:skeleton_skull', rotation='0')
                b.set(tx + 1, b.G + h + 3, tz, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        return gates
    for k in range(sides):
        if skip and skip(2 * math.pi * (k + 1) / sides):
            continue
        ax, az = pts[k]
        bx, bz = pts[(k + 1) % sides]
        b.wall_line(ax, az, bx, bz, h=h, thick=3, gates=gates)
    for k, (x, z) in enumerate(pts):
        if skip and skip(2 * math.pi * (k + 0.5) / sides):
            continue
        b.round_tower(x, z, 4, h + 5, roof_block=tower_roof)
    for gx, gz, _ in gates:
        ga = math.atan2(gz - C, gx - C)
        for s in (-6, 6):
            tx = int(round(gx - math.sin(ga) * s))
            tz = int(round(gz + math.cos(ga) * s))
            b.round_tower(tx, tz, 3, h + 3, roof_block=tower_roof)
    return gates


def tent(b, cx, cz, r=3, color='brown', npc=None, door='south'):
    """Шатёр-купол из шерсти на каркасе."""
    G = b.G
    b.reserve(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1)
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.4:
                continue
            b.set(x, G - 1, z, 'minecraft:spruce_planks' if d < r - 0.5 else 'minecraft:coarse_dirt')
            h = int(round((r + 0.5 - d) * 1.4)) + 1
            for y in range(G, G + h + 1):
                shell = y == G + h or d > r - 0.6
                if shell:
                    b.set(x, y, z, f'minecraft:{color}_wool' if (x + z + y) % 5 else 'minecraft:spruce_log', **({'axis': 'y'} if (x + z + y) % 5 == 0 else {}))
                else:
                    b.set(x, y, z, 'minecraft:air')
    dx, dz = DIRS[door]
    for y in range(G, G + 2):
        b.set(cx + dx * r, y, cz + dz * r, 'minecraft:air')
    b.set(cx, G, cz, 'minecraft:white_carpet' if color != 'white' else 'minecraft:brown_carpet')
    b.lantern(cx, G + int(r * 1.4), cz, hanging=True)
    if npc:
        b.entities.append((cx, G, cz, npc, {}))


def bonfire(b, x, z):
    G = b.G
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, G - 1, z + dz, 'minecraft:cobblestone')
    b.set(x, G, z, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
        b.stair(x + dx, G, z + dz, 'minecraft:spruce_stairs', {(-2, 0): 'east', (2, 0): 'west', (0, -2): 'south', (0, 2): 'north'}[(dx, dz)])


def totem(b, x, z, h=8):
    G = b.G
    for y in range(G, G + h):
        b.set(x, y, z, 'minecraft:stripped_dark_oak_log' if y % 3 else 'minecraft:carved_pumpkin', **({'axis': 'y'} if y % 3 else {'facing': 'south'}))
    b.set(x, G + h, z, 'minecraft:skeleton_skull', rotation='8')
    for dx in (-1, 1):
        b.set(x + dx, G + h - 2, z, 'minecraft:bone_block', axis='x')


def giant_tree(b, cx, cz, r, h, log='minecraft:oak_log', leaves='minecraft:oak_leaves', canopy=12, platforms=(), npc=None):
    """Исполинское дерево с корнями, ветвями, кроной и площадками вокруг ствола."""
    G = b.G
    for y in range(G - 1, G + h):
        rr = r * (1 - (y - G) / (h * 2.2))
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= rr:
                    b.set(x, y, z, log, axis='y')
    # корни
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        for t in range(r, r + 7):
            x, z = cx + round(math.cos(a) * t), cz + round(math.sin(a) * t)
            hh = max(0, 3 - (t - r) // 2)
            for y in range(G - 1, G + hh):
                b.set(x, y, z, log, axis='x' if abs(math.cos(a)) > 0.7 else 'z')
    # ветви и крона
    top = G + h
    for k in range(5):
        a = 2 * math.pi * k / 5
        by = top - 6 - (k % 3) * 3
        for t in range(r, r + canopy // 2 + 2):
            x, z = cx + round(math.cos(a) * t), cz + round(math.sin(a) * t)
            b.set(x, by + (t - r) // 3, z, log, axis='x' if abs(math.cos(a)) > 0.7 else 'z')
        lx, lz = cx + round(math.cos(a) * (r + canopy // 2 + 2)), cz + round(math.sin(a) * (r + canopy // 2 + 2))
        blob(b, lx, by + canopy // 6 + 2, lz, canopy * 0.5, leaves)
    blob(b, cx, top + 2, cz, canopy, leaves)
    # площадки со спиральными ступенями
    for (py, pr) in platforms:
        for x in range(cx - pr, cx + pr + 1):
            for z in range(cz - pr, cz + pr + 1):
                d = math.hypot(x - cx, z - cz)
                if r + 0.5 < d <= pr:
                    b.set(x, py, z, 'minecraft:birch_planks')
                    if pr - 1 < d:
                        b.set(x, py + 1, z, 'minecraft:birch_fence', waterlogged='false')
        b.lantern(cx + r + 1, py + 1, cz, hanging=False)
    # винтовая лестница вокруг ствола
    if platforms:
        maxy = max(p[0] for p in platforms)
        for i in range(0, maxy - G + 1):
            a = i * 0.32
            x, z = cx + round(math.cos(a) * (r + 1.5)), cz + round(math.sin(a) * (r + 1.5))
            b.set(x, G + i, z, 'minecraft:birch_slab', type='top', waterlogged='false')
            for k in range(1, 3):
                if (x, G + i + k, z) in b.blocks and 'leaves' in b.blocks[(x, G + i + k, z)][0]:
                    b.set(x, G + i + k, z, 'minecraft:air')
    if npc:
        py = platforms[0][0] + 1 if platforms else G
        b.entities.append((cx + r + 2, py, cz + 1, npc, {}))


def blob(b, cx, cy, cz, r, leaves):
    R = int(r) + 1
    for x in range(cx - R, cx + R + 1):
        for y in range(cy - R // 2, cy + R // 2 + 2):
            for z in range(cz - R, cz + R + 1):
                d = math.sqrt((x - cx) ** 2 + ((y - cy) * 1.8) ** 2 + (z - cz) ** 2)
                if d <= r and ((x * 31 + y * 17 + z * 13) % 11) != 0:
                    if (x, y, z) not in b.blocks:
                        b.set(x, y, z, leaves, persistent='true', distance='1', waterlogged='false')


def docks(b, C, a, from_r, deck_y, n=3, length=26):
    """Причалы в направлении a над водой, сваи до дна шаблона, лодки."""
    for k in range(n):
        off = (k - (n - 1) / 2) * 12
        for t in range(int(from_r), int(from_r) + length):
            cx = C + math.cos(a) * t - math.sin(a) * off
            cz = C + math.sin(a) * t + math.cos(a) * off
            for w in range(-2, 2):
                x = int(round(cx - math.sin(a) * w))
                z = int(round(cz + math.cos(a) * w))
                if 0 <= x < b.size[0] and 0 <= z < b.size[2]:
                    b.set(x, deck_y, z, 'minecraft:spruce_planks')
                    if w in (-2, 1) and t % 4 == 0:
                        for y in range(0, deck_y):
                            b.set(x, y, z, 'minecraft:stripped_spruce_log', axis='y')
                        b.set(x, deck_y + 1, z, 'minecraft:spruce_fence', waterlogged='false')
                        if t % 8 == 0:
                            b.lantern(x, deck_y + 2, z)
        # лодка у причала
        bx = C + math.cos(a) * (from_r + length * 0.6) - math.sin(a) * (off + 4)
        bz = C + math.sin(a) * (from_r + length * 0.6) + math.cos(a) * (off + 4)
        b.entities.append((int(bx), deck_y - 1, int(bz), 'minecraft:boat', {'Type': 'spruce'}))


def lighthouse(b, x, z, base_y, h=26):
    for y in range(base_y, base_y + h):
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                d = math.hypot(dx, dz)
                if d <= 3.4:
                    shell = d > 2.4
                    stripe = ((y - base_y) // 4) % 2
                    if shell:
                        b.set(x + dx, y, z + dz, 'minecraft:red_terracotta' if stripe else 'minecraft:white_terracotta')
                    else:
                        b.set(x + dx, y, z + dz, 'minecraft:air')
        b.set(x, y, z - 2, 'minecraft:ladder', facing='south', waterlogged='false')
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            if math.hypot(dx, dz) <= 3.4:
                b.set(x + dx, base_y + h, z + dz, 'minecraft:polished_andesite')
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            b.set(x + dx, base_y + h + 1, z + dz, 'minecraft:glowstone' if dx == 0 and dz == 0 else 'minecraft:glass')
            b.set(x + dx, base_y + h + 2, z + dz, 'minecraft:glass')
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            b.slab(x + dx, base_y + h + 3, z + dz, 'minecraft:dark_prismarine_slab')


def ziggurat(b, cx, cz, levels=5, base=14):
    G = b.G
    y = G - 1
    for l in range(levels):
        r = base - l * 3
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                for yy in range(y, y + 3):
                    b.set(x, yy, z, 'minecraft:polished_blackstone_bricks' if (x + yy + z) % 7 else 'minecraft:gilded_blackstone')
        for x in (cx - r, cx + r):
            for z in (cz - r, cz + r):
                b.set(x, y + 3, z, 'minecraft:soul_campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        # ступени с юга
        for k in range(3):
            for x in range(cx - 2, cx + 3):
                b.stair(x, y + k, cz + r + 1 - k + 0, 'minecraft:polished_blackstone_brick_stairs', 'north') if k == 0 else None
        y += 3
    for x in range(cx - 2, cx + 3):
        for z in range(cz - 2, cz + 3):
            b.set(x, y, z, 'minecraft:crying_obsidian')
    for k in range(levels * 3 + 1):
        for x in range(cx - 2, cx + 3):
            b.stair(x, G - 1 + k, cz + base + 1 - k, 'minecraft:polished_blackstone_brick_stairs', 'north')
    b.set(cx, y + 1, cz, 'minecraft:soul_fire')
    b.set(cx, y, cz, 'minecraft:soul_soil')
    return y + 1


def arena(b, cx, cz, r=14):
    G = b.G
    for x in range(cx - r - 4, cx + r + 5):
        for z in range(cz - r - 4, cz + r + 5):
            d = math.hypot(x - cx, z - cz)
            if d <= r:
                b.set(x, G - 3, z, 'minecraft:sand')
                b.set(x, G - 2, z, 'minecraft:air')
                b.set(x, G - 1, z, 'minecraft:air')
            elif d <= r + 4:
                lvl = int(d - r)
                for y in range(G - 3, G - 2 + lvl):
                    b.set(x, y, z, 'minecraft:mud_bricks')
                b.stair(x, G - 2 + lvl, z, 'minecraft:spruce_stairs', angle_dir(math.atan2(cz - z, cx - x)))
    b.reserve(cx - r - 5, cz - r - 5, cx + r + 5, cz + r + 5)
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, z = cx + round(math.cos(a) * (r + 5)), cz + round(math.sin(a) * (r + 5))
        for y in range(G, G + 4):
            b.set(x, y, z, 'minecraft:spruce_fence', waterlogged='false')
        b.set(x, G + 4, z, 'minecraft:skeleton_skull', rotation=str(k * 2))
        b.set(x, G + 3, z + 1 if k % 2 else z - 1, 'minecraft:lantern', hanging='false', waterlogged='false')


# =====================================================================

def town(city, style, size, seed, wall='stone', wall_r=None, gate_angles=(), tower_roof='minecraft:dark_prismarine',
         street_grass='minecraft:grass_block', ground=4, houses_fn=None, skip=None):
    b = City(size, 72, size, seed, style, ground=ground)
    C = size // 2
    WR = wall_r or C - 10

    def streets(x, z):
        dx, dz = x - C, z - C
        r = math.hypot(dx, dz)
        for a in gate_angles:
            # проспект от ворот к центру
            px = -math.sin(a) * dx + math.cos(a) * dz
            along = math.cos(a) * dx + math.sin(a) * dz
            if abs(px) <= 2 and along > 0:
                return True
        return abs(r - WR * 0.6) <= 1.5 or r < 9

    ground_disk(b, C, WR + 3, streets, street_grass)
    if wall:
        ring_wall(b, C, WR, gate_angles, h=9, kind=wall, tower_roof=tower_roof, skip=skip)
    for a in gate_angles:
        approach(b, C, a, WR + 2)
    return b, C, WR


def houses_ring(b, C, R, seed, npc, min_size=7, max_size=10, floors=(1, 2), kinds=('home', 'home', 'shop'), step=11, npc_chance=0.25):
    rnd = random.Random(seed)
    cells = []
    for st, lo, hi in ((step, min_size, max_size), (7, max(5, min_size - 1), max(6, min_size + 1))):
        pc = [(gx, gz, lo, hi) for gx in range(C - R, C + R, st) for gz in range(C - R, C + R, st)]
        rnd.shuffle(pc)
        cells += pc
    for gx, gz, lo, hi in cells:
        w = rnd.randint(lo, hi)
        d = rnd.randint(lo - 1, hi - 1)
        x0, z0 = gx + rnd.randint(0, 3), gz + rnd.randint(0, 3)
        x1, z1 = x0 + w - 1, z0 + d - 1
        cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
        if math.hypot(cx - C, cz - C) > R - max(w, d) / 2 or math.hypot(cx - C, cz - C) < 12:
            continue
        if not b.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1):
            continue
        dx, dz = C - cx, C - cz
        door = ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')
        fl = rnd.choice(floors)
        b.house(x0, z0, w, d, floors=fl, door=door, interior=rnd.choice(kinds), npc=npc if rnd.random() < npc_chance else None)


def living(b, C, R, seed, crafts, citizen, merchant, terrace=1, market=(22, 15), market_crafts=(), kinds=('home', 'home', 'shop', 'library'),
           floors=(1, 2, 2, 3), npc_chance=0.3, sizes=((6, 10), (7, 10)), outskirts_r=None, stashes=4):
    """Живой город: кварталы с кривыми улочками и террасами, тесный рынок, мастерские, дома, дворы, поля за стенами."""
    import mapbuild_crafts as MCR
    import mapbuild_layout as ML
    org = ML.Organic(b, C, R, seed, plaza_r=0, terrace=terrace)
    mk = None
    if market:
        mw, md = market
        rnd = random.Random(str(seed) + 'mk')
        for attempt in range(400):
            a = rnd.random() * 2 * math.pi
            r = rnd.uniform(8, R * 0.55)
            x0, z0 = int(C + math.cos(a) * r - mw / 2), int(C + math.sin(a) * r - md / 2)
            cells = [(x, z) for x in range(x0, x0 + mw) for z in range(z0, z0 + md)]
            if all(c in org.cell or c in org.street for c in cells) and b.free(x0, z0, x0 + mw - 1, z0 + md - 1):
                for c in cells:
                    org.cell.pop(c, None)
                    org.street.add(c)
                mk = (x0, z0, x0 + mw - 1, z0 + md - 1)
                break
    org.paint_ground()
    if mk:
        ML.market(b, *mk, list(market_crafts) or ['baker', 'butcher', 'farmer', 'herbalist', 'tailor'], merchant, citizen, seed)
    placed, homes = MCR.fill_city(b, org, crafts, citizen, kinds=kinds, floors=floors, npc_chance=npc_chance, sizes=sizes)
    print(f'  {seed}: мастерских {placed}/{len(crafts)}, домов {homes}, рынок {"есть" if mk else "нет"}')
    org.street_life()
    org.courtyards(citizen, density=1.2, stashes=stashes)
    if outskirts_r:
        ML.outskirts(b, C, outskirts_r, seed, citizen)
    return org


def reserve_avenues(b, C, angles, half=3):
    for a in angles:
        for t in range(0, C):
            x, z = C + math.cos(a) * t, C + math.sin(a) * t
            b.reserve(int(x) - half, int(z) - half, int(x) + half, int(z) + half)


# ---------------- Солнечная Гавань ----------------

def haven(city, gates, sea_a):
    gate_a = gates[0]
    G = 12
    b, C, WR = town(city, 'human', 128, 'haven', wall='stone', wall_r=54, gate_angles=gates, ground=G,
                    skip=lambda a: math.cos(a - sea_a) > 0.35)
    reserve_avenues(b, C, gates)
    # причалы и маяк к морю
    deck = G - 4  # настил на высоте 64, над водой 63 (город на 68)
    docks(b, C, sea_a, 50, deck, n=3, length=26)
    lx = int(C + math.cos(sea_a) * 62)
    lz = int(C + math.sin(sea_a) * 62)
    lighthouse(b, max(4, min(123, lx)), max(4, min(123, lz)), 0, deck + 22)
    b.fountain(C, C, 3)
    b.reserve(C - 5, C - 5, C + 5, C + 5)
    import mapbuild_crafts as MCR
    living(b, C, WR - 5, 'haven', MCR.CITY_CRAFTS['haven'], 'litrpg:human_citizen', 'litrpg:human_merchant', terrace=2, market=(20, 13),
           market_crafts=['fishmonger', 'baker', 'farmer', 'tailor', 'herbalist'],
           outskirts_r=WR + 4)
    return b


# ---------------- Сильвендар ----------------

def sylvendar(city, gates):
    gate_a = gates[0]
    b, C, WR = town(city, 'elf', 128, 'sylvendar', wall=None, wall_r=56, gate_angles=gates, street_grass='minecraft:grass_block')
    reserve_avenues(b, C, gates)
    giant_tree(b, C, C, 4, 46, 'minecraft:oak_log', 'minecraft:flowering_azalea_leaves', canopy=16,
               platforms=[(b.G + 12, 11), (b.G + 24, 9), (b.G + 34, 8)], npc='litrpg:elf_matriarch')
    b.reserve(C - 18, C - 18, C + 18, C + 18)
    # зал совета на нижней площадке: карта мира
    from gen_mapbuild import painting_map
    for x in range(C - 4, C + 4):
        for y in range(b.G + 13, b.G + 22):
            b.set(x, y, C + 11, 'minecraft:birch_planks' if y in (b.G + 13, b.G + 21) else 'minecraft:stripped_birch_wood', axis='y')
    painting_map(b, C + 3, b.G + 13, C + 10, 'north')
    for k in range(4):
        a = gate_a + math.pi / 4 + k * math.pi / 2
        tx, tz = int(C + math.cos(a) * 34), int(C + math.sin(a) * 34)
        giant_tree(b, tx, tz, 2, 30, 'minecraft:birch_log', 'minecraft:azalea_leaves', canopy=9, platforms=[(b.G + 12, 7)],
                   npc='litrpg:elf_moonsage' if k == 0 else None)
        b.reserve(tx - 9, tz - 9, tx + 9, tz + 9)
        # подвесной мост к большому дереву
        for t in range(9, 26):
            x = int(round(C + math.cos(a) * t))
            z = int(round(C + math.sin(a) * t))
            b.set(x, b.G + 12, z, 'minecraft:birch_slab', type='top', waterlogged='false')
    # лунный колодец
    mw = (int(C + math.cos(gate_a) * 24), int(C + math.sin(gate_a) * 24))
    for x in range(mw[0] - 4, mw[0] + 5):
        for z in range(mw[1] - 4, mw[1] + 5):
            d = math.hypot(x - mw[0], z - mw[1])
            if d <= 4.5:
                if d > 3.4:
                    b.set(x, b.G, z, 'minecraft:calcite')
                else:
                    b.set(x, b.G - 2, z, 'minecraft:pearlescent_froglight', axis='y')
                    b.set(x, b.G - 1, z, 'minecraft:water', level='0')
    from gen_mapbuild import stall
    for k in range(4):
        a = gate_a + (k - 1.5) * 0.5
        stall_elf(b, int(C + math.cos(a) * 44), int(C + math.sin(a) * 44))
    import mapbuild_crafts as MCR
    living(b, C, WR - 2, 'sylvendar', MCR.CITY_CRAFTS['sylvendar'], 'litrpg:elf_citizen', 'litrpg:elf_trader', terrace=1, market=(18, 13),
           market_crafts=['herbalist', 'tailor', 'jeweler', 'alchemist', 'fletcher'], kinds=('home', 'library'), floors=(1, 2),
           sizes=((6, 9), (6, 9)), outskirts_r=WR + 4)
    for k in range(40):
        rnd = random.Random(k)
        x, z = rnd.randint(8, 120), rnd.randint(8, 120)
        if b.free(x - 1, z - 1, x + 1, z + 1) and math.hypot(x - C, z - C) < 60:
            b.set(x, b.G, z, rnd.choice(['minecraft:lily_of_the_valley', 'minecraft:allium', 'minecraft:blue_orchid', 'minecraft:azure_bluet']))
    return b


def stall_elf(b, x, z):
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 and abs(dz) == 1:
                for y in range(b.G, b.G + 3):
                    b.set(x + dx, y, z + dz, 'minecraft:birch_fence', waterlogged='false')
            b.set(x + dx, b.G + 3, z + dz, 'minecraft:azalea_leaves', persistent='true', distance='1', waterlogged='false')
    b.set(x, b.G, z - 1, 'minecraft:barrel', facing='up', open='false')
    b.entities.append((x, b.G, z, 'litrpg:elf_trader', {}))
    b.reserve(x - 3, z - 2, x + 3, z + 2)


# ---------------- Кхазад-Бар ----------------

def khazad(city, gates):
    gate_a = gates[0]
    b, C, WR = town(city, 'dwarf', 128, 'khazad', wall='stone', wall_r=58, gate_angles=gates,
                    tower_roof='minecraft:polished_blackstone_bricks', street_grass='minecraft:stone')
    reserve_avenues(b, C, gates)
    G = b.G
    # Великий зал напротив ворот
    ha = gate_a + math.pi
    hx, hz = int(C + math.cos(ha) * 24), int(C + math.sin(ha) * 24)
    x0, z0, w, d = hx - 16, hz - 12, 32, 24
    b.reserve(x0 - 1, z0 - 1, x0 + w, z0 + d)
    for x in range(x0, x0 + w):
        for z in range(z0, z0 + d):
            edge = x in (x0, x0 + w - 1) or z in (z0, z0 + d - 1)
            b.set(x, G - 1, z, 'minecraft:polished_deepslate' if (x + z) % 2 else 'minecraft:deepslate_tiles')
            for y in range(G, G + 14):
                if edge:
                    b.set(x, y, z, 'minecraft:deepslate_bricks' if (y - G) % 5 else 'minecraft:chiseled_deepslate')
                elif (x - x0) % 6 == 3 and (z - z0) % 6 == 3:
                    b.set(x, y, z, 'minecraft:polished_basalt', axis='y') if y < G + 13 else b.set(x, y, z, 'minecraft:gold_block')
                else:
                    b.set(x, y, z, 'minecraft:air')
            b.set(x, G + 14, z, 'minecraft:polished_blackstone_bricks')
            if edge and (x + z) % 2 == 0:
                b.set(x, G + 15, z, 'minecraft:polished_blackstone_brick_wall')
    door_side = angle_dir(gate_a)
    ddx, ddz = DIRS[door_side]
    for s in range(-2, 3):
        for y in range(G, G + 6):
            x = hx + (ddx * 16 if ddx else s)
            z = hz + (ddz * 12 if ddz else s)
            b.set(x - (1 if ddx > 0 else 0), y, z - (1 if ddz > 0 else 0), 'minecraft:air')
    # трон, кузня, лава
    b.set(hx - ddx * 10, G, hz - ddz * 10, 'minecraft:gold_block')
    b.entities.append((hx - ddx * 9, G, hz - ddz * 9, 'litrpg:dwarf_thane', {}))
    for k in range(-1, 2):
        lx, lz = hx + k * 8 * (1 if ddz else 0), hz + k * 8 * (1 if ddx else 0)
        b.set(lx, G - 1, lz, 'minecraft:lava', level='0')
        b.set(lx + 1, G, lz, 'minecraft:anvil', facing='north')
    b.entities.append((hx + 6, G, hz + 2, 'litrpg:dwarf_smith', {}))
    b.entities.append((hx - 6, G, hz - 2, 'litrpg:dwarf_smith', {}))
    from gen_mapbuild import painting_map
    # карта мира у трона
    from gen_mapbuild import map_board
    map_board(b, hx - ddx * 20, hz - ddz * 20, frame='minecraft:polished_deepslate', face='minecraft:smooth_basalt',
              cap='minecraft:polished_deepslate_wall')
    # статуи королей у ворот
    for s in (-1, 1):
        sx = int(C + math.cos(gate_a) * (WR - 12) - math.sin(gate_a) * 9 * s)
        sz = int(C + math.sin(gate_a) * (WR - 12) + math.cos(gate_a) * 9 * s)
        b.statue(sx, sz, h=18, block='minecraft:deepslate_bricks', accent='minecraft:gold_block')
        b.reserve(sx - 3, sz - 3, sx + 3, sz + 3)
    # руны-светильники
    for k in range(10):
        a = 2 * math.pi * k / 10
        x, z = int(C + math.cos(a) * 40), int(C + math.sin(a) * 40)
        if b.free(x, z, x, z):
            for y in range(G, G + 4):
                b.set(x, y, z, 'minecraft:chiseled_deepslate' if y % 2 else 'minecraft:polished_deepslate')
            b.set(x, G + 4, z, 'minecraft:lantern', hanging='false', waterlogged='false')
    import mapbuild_crafts as MCR
    living(b, C, WR - 5, 'khazad', MCR.CITY_CRAFTS['khazad'], 'litrpg:dwarf_citizen', 'litrpg:dwarf_smith', terrace=3, market=(20, 13),
           market_crafts=['jeweler', 'butcher', 'baker', 'alchemist', 'leatherworker'], kinds=('home', 'smith', 'shop'), floors=(1, 2, 2),
           outskirts_r=WR + 4)
    return b


# ---------------- Гхар'Дум ----------------

def ghardum(city, gates):
    gate_a = gates[0]
    b, C, WR = town(city, 'orc', 128, 'ghardum', wall='palisade', wall_r=56, gate_angles=gates, street_grass='minecraft:coarse_dirt')
    reserve_avenues(b, C, gates)
    arena(b, C, C, 13)
    ha = gate_a + math.pi
    lx, lz = int(C + math.cos(ha) * 36), int(C + math.sin(ha) * 36)
    b.house(lx - 11, lz - 6, 22, 12, floors=1, door=angle_dir(gate_a), interior='guard', npc='litrpg:orc_warlord')
    from gen_mapbuild import painting_map
    from gen_mapbuild import map_board
    map_board(b, lx, lz + 12, frame='minecraft:stripped_dark_oak_log', face='minecraft:spruce_planks', cap='minecraft:dark_oak_fence')
    import mapbuild_crafts as MCR
    living(b, C, WR - 5, 'ghardum', MCR.CITY_CRAFTS['ghardum'], 'litrpg:orc_citizen', 'litrpg:orc_trader', terrace=1, market=(18, 12),
           market_crafts=['butcher', 'leatherworker', 'herbalist', 'fletcher'], floors=(1, 1, 2), sizes=((6, 9), (6, 8)), npc_chance=0.35,
           outskirts_r=WR + 4)
    rnd = random.Random(7)
    colors = ['brown', 'red', 'brown', 'black', 'gray']
    for k in range(26):
        a = rnd.random() * 2 * math.pi
        r = rnd.uniform(24, WR - 8)
        x, z = int(C + math.cos(a) * r), int(C + math.sin(a) * r)
        if b.free(x - 5, z - 5, x + 5, z + 5):
            tent(b, x, z, rnd.choice([3, 3, 4]), rnd.choice(colors),
                 npc=rnd.choice(['litrpg:orc_citizen', None, 'litrpg:orc_trader', 'litrpg:orc_citizen']))
    for k in range(8):
        a = 2 * math.pi * k / 8 + 0.3
        x, z = int(C + math.cos(a) * 21), int(C + math.sin(a) * 21)
        if b.free(x - 2, z - 2, x + 2, z + 2):
            (bonfire if k % 2 else totem)(b, x, z)
            b.reserve(x - 2, z - 2, x + 2, z + 2)
    for k in range(4):
        b.entities.append((C + 20 + k, b.G, C - 30, 'minecraft:wolf', {}))
    return b


# ---------------- Стойбище Белого Волка ----------------

def wolfden(city, gates):
    gate_a = gates[0]
    b, C, WR = town(city, 'beast', 112, 'wolfden', wall='palisade', wall_r=46, gate_angles=gates, street_grass='minecraft:podzol')
    reserve_avenues(b, C, gates)
    totem(b, C, C, 14)
    for k in range(4):
        a = k * math.pi / 2
        bonfire(b, int(C + math.cos(a) * 6), int(C + math.sin(a) * 6))
    b.reserve(C - 9, C - 9, C + 9, C + 9)
    ha = gate_a + math.pi
    lx, lz = int(C + math.cos(ha) * 28), int(C + math.sin(ha) * 28)
    b.house(lx - 10, lz - 5, 20, 10, floors=1, door=angle_dir(gate_a), interior='home', npc='litrpg:beast_alpha')
    from gen_mapbuild import painting_map
    from gen_mapbuild import map_board
    map_board(b, lx, lz + 10, frame='minecraft:stripped_spruce_log', face='minecraft:spruce_planks', cap='minecraft:spruce_fence')
    import mapbuild_crafts as MCR
    living(b, C, WR - 4, 'wolfden', MCR.CITY_CRAFTS['wolfden'], 'litrpg:beast_citizen', 'litrpg:beast_trader', terrace=1, market=(14, 10),
           market_crafts=['leatherworker', 'herbalist', 'butcher'], floors=(1, 1, 2), sizes=((5, 8), (6, 8)), npc_chance=0.35,
           outskirts_r=WR + 4)
    for k in range(12):
        a = 2 * math.pi * k / 12 + 0.2
        x, z = int(C + math.cos(a) * 20), int(C + math.sin(a) * 20)
        if b.free(x - 4, z - 4, x + 4, z + 4):
            tent(b, x, z, 3, ['brown', 'white', 'light_gray'][k % 3],
                 npc=['litrpg:beast_citizen', 'litrpg:beast_trader', None][k % 3])
    for k in range(10):
        a = 2 * math.pi * k / 10
        x, z = int(C + math.cos(a) * 36), int(C + math.sin(a) * 36)
        if b.free(x - 4, z - 4, x + 4, z + 4):
            tent(b, x, z, 3, ['brown', 'white'][k % 2], npc='litrpg:beast_citizen' if k % 3 == 0 else None)
    return b


# ---------------- Пеплград ----------------

def ashgrad(city, gates):
    gate_a = gates[0]
    b, C, WR = town(city, 'demon', 128, 'ashgrad', wall='stone', wall_r=54, gate_angles=gates,
                    tower_roof='minecraft:nether_bricks', street_grass='minecraft:blackstone')
    G = b.G
    # лавовый ров за стеной, мост у ворот
    for x in range(128):
        for z in range(128):
            r = math.hypot(x - C, z - C)
            if WR + 4 <= r <= WR + 7:
                ang = math.atan2(z - C, x - C)
                bridge = abs(((ang - gate_a + math.pi) % (2 * math.pi)) - math.pi) < 0.09
                b.set(x, G - 2, z, 'minecraft:blackstone')
                b.set(x, G - 1, z, 'minecraft:nether_bricks' if bridge else 'minecraft:lava', **({} if bridge else {'level': '0'}))
    reserve_avenues(b, C, gates)
    top = ziggurat(b, C, C, 5, 15)
    b.reserve(C - 17, C - 17, C + 17, C + 17)
    b.entities.append((C, top, C + 2, 'litrpg:demon_archfiend', {}))
    from gen_mapbuild import painting_map
    for x in range(C - 4, C + 4):
        for y in range(G, G + 9):
            b.set(x, y, C + 19, 'minecraft:polished_blackstone_bricks')
    painting_map(b, C + 3, G, C + 20, 'south')
    for k in range(12):
        a = 2 * math.pi * k / 12
        x, z = int(C + math.cos(a) * 24), int(C + math.sin(a) * 24)
        if b.free(x - 1, z - 1, x + 1, z + 1):
            b.tree(x, z, 6, 'minecraft:crimson_stem', 'minecraft:nether_wart_block', 2.6)
    import mapbuild_crafts as MCR
    living(b, C, WR - 5, 'ashgrad', MCR.CITY_CRAFTS['ashgrad'], 'litrpg:demon_citizen', 'litrpg:demon_trader', terrace=2, market=(18, 13),
           market_crafts=['alchemist', 'jeweler', 'tailor', 'butcher'], kinds=('home', 'library', 'shop'), outskirts_r=None)
    return b


# ---------------- пограничные крепости ----------------

def fort(city, gates, seed, banner):
    gate_a = gates[0]
    b, C, WR = town(city, 'human', 112, seed, wall='stone', wall_r=46, gate_angles=gates, tower_roof='minecraft:spruce_planks')
    reserve_avenues(b, C, gates)
    ha = gate_a + math.pi
    kx, kz = int(C + math.cos(ha) * 18), int(C + math.sin(ha) * 18)
    b.house(kx - 9, kz - 7, 18, 14, floors=3, door=angle_dir(gate_a), roof=b.st['roofs'][0], interior='guard', npc='litrpg:human_knight')
    from gen_mapbuild import painting_map, stall
    from gen_mapbuild import map_board
    map_board(b, kx, kz + 12)
    b.round_tower(C, C, 4, 22, roof_block='minecraft:spruce_planks')
    b.reserve(C - 6, C - 6, C + 6, C + 6)
    for k in range(3):
        a = gate_a + (k - 1) * 0.7
        stall(b, int(C + math.cos(a) * 16), int(C + math.sin(a) * 16), ['red', 'blue', 'yellow'][k], 'south')
    import mapbuild_crafts as MCR
    living(b, C, WR - 5, seed, MCR.CITY_CRAFTS['fort'], 'litrpg:human_citizen', 'litrpg:human_merchant', terrace=1, market=(14, 10),
           market_crafts=['baker', 'butcher', 'farmer'], kinds=('home', 'guard', 'smith'), floors=(1, 2), outskirts_r=WR + 4)
    return b
