"""
Города народов (2.8.0): Солнечная Гавань, Сильвендар, Кхазад-Бар, Гхар'Дум, Стойбище Белого Волка,
Пеплград и три пограничные крепости. Каркас и окрестности — mapbuild_towns.py.
Каждый город возвращает City; в b.fit — параметры площадки рельефа для gen_world (если нужны особые).
"""
import math
import random

from mapbuild_lib import DIRS, OPP, TByte, TInt  # noqa: F401
import mapbuild_layout as ML
from mapbuild_towns import Town, cardinal, ML_blob, _ground_rect


def _crafts(cid, drop=()):
    import mapbuild_crafts as MCR
    out = list(MCR.CITY_CRAFTS[cid])
    for c in drop:
        if c in out:
            out.remove(c)
    return out


def map_wall(t, x, z, facing='south', frame='minecraft:stone_bricks', face='minecraft:smooth_stone', cap='minecraft:stone_brick_wall'):
    """Стенка с картой мира рядом с (x, z) — ищет свободное место."""
    from gen_mapbuild import map_board
    map_board(t.b, x, z, frame=frame, face=face, cap=cap)


def bell_tower(b, x, z, h, roof_block, base=None):
    """Колокольня/часовая башня: квадратная, с открытым ярусом и колоколом."""
    G = b.G
    base = base or b.st['found']
    for y in range(G - 1, G + h):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                edge = abs(dx) == 2 or abs(dz) == 2
                open_tier = y >= G + h - 4 and edge and not (abs(dx) == 2 and abs(dz) == 2)
                if edge:
                    b.set(x + dx, y, z + dz, 'minecraft:air' if open_tier and y < G + h - 1 else b.bp(base, x + dx, y, z + dz))
                elif (y - G) % 5 == 0:
                    b.set(x + dx, y, z + dz, b.st['plank'])
                else:
                    b.set(x + dx, y, z + dz, 'minecraft:air')
    for y in range(G, G + h - 4):
        b.set(x, y, z - 1, 'minecraft:ladder', facing='south', waterlogged='false')
    b.set(x, G + h - 2, z, 'minecraft:bell', attachment='ceiling', facing='north', powered='false')
    b.set(x, G + h - 1, z, b.bp(base, x, 0, z))
    for i in range(4):
        for dx in range(-2 + i, 3 - i):
            for dz in range(-2 + i, 3 - i):
                if max(abs(dx), abs(dz)) == 2 - i:
                    b.set(x + dx, G + h + i, z + dz, roof_block)
    b.set(x, G + h + 3, z, roof_block)
    b.set(x, G + h + 4, z, 'minecraft:lightning_rod', facing='up', powered='false', waterlogged='false')
    b.reserve(x - 3, z - 3, x + 3, z + 3)


def hall(b, x0, z0, w, d, door, floors=2, roof=None, interior='library', npc=None, tower=None):
    """Большое общественное здание (ратуша, гильдия, дом совета): дом + башня на углу."""
    b.house(x0, z0, w, d, floors=floors, door=door, roof=roof, interior=interior, npc=npc)
    if tower:
        tx, tz = (x0 - 1, z0 - 1) if door in ('south', 'east') else (x0 + w, z0 + d)
        bell_tower(b, tx, tz, floors * 4 + 8, tower)


# =====================================================================
#                      СОЛНЕЧНАЯ ГАВАНЬ (порт)
# =====================================================================

def i_end(x, x0, L):
    """Торцы корпуса — всегда обшивка."""
    return x == x0 or x == x0 + L - 1


def ship(b, x0, zc, wl, L=26, bow=1, sail='minecraft:white_wool', stripe='minecraft:blue_wool', flag='minecraft:blue_banner', crew=None):
    """Корабль вдоль оси x: x0..x0+L-1, середина по zc, wl — y поверхности воды. Сухой трюм, палуба, ют с каютой,
    две мачты с парусами и марсом, бушприт, якорная цепь."""
    keel = wl - 3
    deck = wl + 2
    hull = 'minecraft:dark_oak_planks'
    # корпус: сначала объём, потом обшивка — оболочкой служит любая клетка объёма, у которой есть сосед снаружи,
    # так что вода не просочится ни снизу, ни с торцов, ни на подъёме носа
    inside = set()
    cols = {}
    for i in range(L):
        u = i / (L - 1)  # 0 — корма, 1 — нос
        x = x0 + i if bow > 0 else x0 + L - 1 - i
        taper = 1.0
        if u > 0.7:
            taper = max(0.12, 1 - (u - 0.7) / 0.3 * 0.88)
        elif u < 0.1:
            taper = 0.8 + u / 0.1 * 0.2
        lift = int((u - 0.75) / 0.25 * 3) if u > 0.75 else 0
        cols[x] = (u, taper)
        for y in range(keel + lift, deck + 1):
            hrel = (y - keel) / (deck - keel)
            half = (1.2 + 3.0 * math.sqrt(hrel)) * taper
            for dz in range(-5, 6):
                if abs(dz) <= half + 0.35:
                    inside.add((x, y, zc + dz))
    for (x, y, z) in inside:
        ad = abs(z - zc)
        shell = y == deck or i_end(x, x0, L) or any((x + dx, y + dy, z + dz) not in inside
                                                     for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
        if y == deck:
            b.set(x, y, z, hull if (x, y, z + 1) not in inside or (x, y, z - 1) not in inside else 'minecraft:spruce_planks')
        elif shell:
            b.set(x, y, z, stripe.replace('_wool', '_terracotta') if y == wl + 1 and 'blue' not in stripe else
                  ('minecraft:spruce_planks' if y == wl + 1 else hull))
        else:
            b.set(x, y, z, 'minecraft:air')
    for x, (u, taper) in cols.items():
        # борт над палубой
        top_half = (1.2 + 3.0) * taper
        for dz in range(-5, 6):
            if top_half - 1.0 < abs(dz) <= top_half + 0.35 and u > 0.12:
                b.set(x, deck + 1, zc + dz, 'minecraft:dark_oak_fence', waterlogged='false')
    # трюм: бочки и сундук
    hx = x0 + (L // 3 if bow > 0 else 2 * L // 3)
    for dz in (-2, 2):
        b.set(hx, keel + 1, zc + dz, 'minecraft:barrel', facing='up', open='false')
        b.set(hx + 1, keel + 1, zc + dz, 'minecraft:barrel', facing='up', open='false')
    b.chest(hx + 2, keel + 1, zc, 'litrpg:chests/city_barrel')
    b.set(hx + 2, deck - 1, zc - 1, 'minecraft:lantern', hanging='true', waterlogged='false')   # свет в трюме
    b.set(hx, deck, zc, 'minecraft:spruce_trapdoor', facing='north', half='top', open='false', powered='false', waterlogged='false')
    for y in range(keel + 1, deck):
        b.set(hx, y, zc + 1, 'minecraft:ladder', facing='north', waterlogged='false')
    # ют: каюта капитана на корме
    sx0 = x0 if bow > 0 else x0 + L - 6
    sx1 = sx0 + 5
    for x in range(sx0, sx1 + 1):
        for dz in range(-3, 4):
            z = zc + dz
            edge = x in (sx0, sx1) or abs(dz) == 3
            for y in range(deck + 1, deck + 4):
                if edge:
                    win = abs(dz) == 3 and y == deck + 2 and x not in (sx0, sx1)
                    b.set(x, y, z, 'minecraft:glass_pane' if win else 'minecraft:spruce_planks', waterlogged='false') if win else \
                        b.set(x, y, z, 'minecraft:spruce_planks')
                else:
                    b.set(x, y, z, 'minecraft:air')
            b.set(x, deck + 4, z, 'minecraft:dark_oak_planks')
            if edge:
                b.set(x, deck + 5, z, 'minecraft:dark_oak_fence', waterlogged='false')
    dx_ = sx1 if bow > 0 else sx0
    b.door(dx_, deck + 1, zc, 'east' if bow > 0 else 'west', 'minecraft:spruce_door')
    ix = dx_ - (2 if bow > 0 else -2)
    b.bed(ix - (1 if bow > 0 else -1), deck + 1, zc - 2, 'east' if bow > 0 else 'west', 'blue')
    b.chest(ix, deck + 1, zc + 2, 'litrpg:chests/city_home')
    b.set(ix, deck + 3, zc, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.set(sx0 + 1 if bow > 0 else sx1 - 1, deck + 5, zc, 'minecraft:lantern', hanging='false', waterlogged='false')
    # штурвал на юте
    b.set(sx0 + 2 if bow > 0 else sx1 - 2, deck + 5, zc, 'minecraft:spruce_fence', waterlogged='false')
    # мачты, реи, паруса, марс
    for k, (pos, hgt) in enumerate(((0.38, 17), (0.66, 15))):
        mx = x0 + int(L * pos) if bow > 0 else x0 + L - 1 - int(L * pos)
        for y in range(keel + 1, deck + hgt):
            b.set(mx, y, zc, 'minecraft:spruce_log', axis='y')
        for yy, w in ((deck + 6, 4), (deck + hgt - 5, 3)):
            for dz in range(-w, w + 1):
                b.set(mx, yy, zc + dz, 'minecraft:stripped_spruce_log', axis='z')
            sx = mx + (1 if bow > 0 else -1)
            for y in range(yy - (4 if w == 4 else 3), yy):
                ww = w - (1 if y == yy - 1 else 0)
                for dz in range(-ww, ww + 1):
                    bulge = 1 if abs(dz) < ww - 1 and yy - y in (2, 3) else 0
                    b.set(sx + (bulge if bow > 0 else -bulge), y, zc + dz, stripe if (k == 0 and abs(dz) <= 1 and y == yy - 3) else sail)
        nest = deck + hgt - 3
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if dx or dz:
                    b.set(mx + dx, nest, zc + dz, 'minecraft:spruce_slab', type='top', waterlogged='false')
                    b.set(mx + dx, nest + 1, zc + dz, 'minecraft:spruce_fence', waterlogged='false')
        b.set(mx, deck + hgt, zc, flag, rotation='4' if bow > 0 else '12')
    # бушприт и якорь
    bx = x0 + L - 1 if bow > 0 else x0
    s = 1 if bow > 0 else -1
    for k in range(1, 6):
        b.set(bx + s * k, deck + 1 + k // 2, zc, 'minecraft:spruce_fence', waterlogged='false')
    for y in range(wl - 1, deck):
        b.set(bx - s * 2, y, zc + 3, 'minecraft:chain', axis='y', waterlogged='true' if y <= wl else 'false')
    if crew:
        b.entities.append((x0 + L // 2, deck + 1, zc, crew, {}))


def shipyard_ribs(b, x0, zc, ground_y, L=20):
    """Корабль на стапеле: киль на подпорках и голые шпангоуты."""
    for i in range(L):
        x = x0 + i
        b.set(x, ground_y, zc, 'minecraft:stripped_spruce_log', axis='x')
        if i % 3 == 0:
            for y in range(ground_y - 2, ground_y):
                b.set(x, y, zc, 'minecraft:spruce_fence', waterlogged='false')
        if i % 3 == 1 and 2 < i < L - 3:
            half = 3 if 4 < i < L - 6 else 2
            for k in range(1, 5):
                hz = min(half, k)
                for s in (-1, 1):
                    b.set(x, ground_y + k, zc + s * hz, 'minecraft:dark_oak_fence', waterlogged='false')
        if i < L // 2 and i % 2 == 0:
            for s in (-1, 1):
                b.set(x, ground_y + 1, zc + s, 'minecraft:dark_oak_planks')


def haven(city, gates):
    S, G = 256, 12
    wl = 7      # вода: y=63 при площадке 68 (G=12)
    Q = 58      # набережная: x = C + Q
    t = Town('haven', S, 'human', 96, gates, sides=10, ground=G, height=96, cut=(1.0, 0.0, Q))
    b, C = t.b, t.C
    st = b.st
    QX = C + Q
    t.paint_base()
    t.walls('stone', h=10, tower_r=4, roof='minecraft:dark_prismarine')
    # концы стены у набережной — бастионы
    ends = sorted(t.cut_ends, key=lambda p: p[1])
    zn, zs = ends[0][1], ends[-1][1]
    for (x, z) in (ends[0], ends[-1]):
        b.round_tower(x - 2, z, 5, 16, roof_block='minecraft:dark_prismarine')

    # --- набережная: стенка, променад, кнехты, лестницы к воде ---
    for z in range(0, S):
        if not (t.in_walls(QX - 6, z) or t.land(QX - 1, z, 1)):
            continue
        for y in range(2, G):
            b.set(QX, y, z, b.bp(['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:stone_bricks'], QX, y, z))
        b.set(QX, G - 1, z, 'minecraft:smooth_stone')
        if z % 6 == 0:
            b.set(QX, G, z, 'minecraft:stone_brick_wall')
            b.set(QX, G + 1, z, 'minecraft:spruce_fence', waterlogged='false')
            b.set(QX, G + 2, z, 'minecraft:lantern', hanging='false', waterlogged='false') if z % 12 == 0 else None
    prom = set()
    for x in range(QX - 7, QX):
        for z in range(zn + 4, zs - 3):
            prom.add((x, z))
    t.pave(prom, ['minecraft:smooth_stone', 'minecraft:polished_andesite', 'minecraft:smooth_stone', 'minecraft:stone_bricks'])
    t.reserve_cells(prom)

    # --- причалы и корабли ---
    piers = list(range(zn + 16, zs - 10, 24))
    for k, pz in enumerate(piers):
        L = 36 if k % 2 == 0 else 28
        for x in range(QX + 1, QX + L):
            for dz in (-1, 0, 1):
                b.set(x, wl + 2, pz + dz, 'minecraft:spruce_planks')
            if (x - QX) % 4 == 0:
                for dz in (-2, 2):
                    for y in range(2, wl + 4):
                        b.set(x, y, pz + dz, 'minecraft:stripped_spruce_log', axis='y')
                if (x - QX) % 8 == 0:
                    b.set(x, wl + 4, pz + 2, 'minecraft:lantern', hanging='false', waterlogged='false')
        b.stair(QX + 1, wl + 3, pz, 'minecraft:stone_brick_stairs', 'west')
        for dz in (-1, 1):
            b.stair(QX + 1, wl + 3, pz + dz, 'minecraft:stone_brick_stairs', 'west')
        # бочки и сети на конце
        b.set(QX + L - 2, wl + 3, pz - 1, 'minecraft:barrel', facing='up', open='false')
        b.set(QX + L - 3, wl + 3, pz - 1, 'minecraft:barrel', facing='up', open='false')
        b.set(QX + L - 2, wl + 3, pz + 1, 'minecraft:cobweb')
        # корабль у южной стороны причала
        if k % 2 == 0:
            ship(b, QX + 5, pz + 7, wl, L=27, bow=1, crew='litrpg:human_merchant')
        else:
            b.entities.append((QX + 8, wl + 1, pz + 4, 'minecraft:boat', {'Type': 'spruce'}))
            b.entities.append((QX + 14, wl + 1, pz - 4, 'minecraft:boat', {'Type': 'oak'}))
    # корабль на рейде
    ship(b, QX + 44, (zn + zs) // 2 + 4, wl, L=30, bow=-1, sail='minecraft:white_wool', stripe='minecraft:red_wool', flag='minecraft:red_banner')

    # --- волнорез с маяком (север) ---
    by = zn - 3
    for x in range(QX + 1, QX + 52):
        for dz in range(-2, 3):
            for y in range(2, wl + 3):
                b.set(x, y, by + dz, b.bp(['minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cobblestone'], x, y, by + dz))
        b.set(x, wl + 3, by - 2, 'minecraft:stone_brick_wall')
        if (x - QX) % 10 == 5:
            b.set(x, wl + 4, by - 2, 'minecraft:lantern', hanging='false', waterlogged='false')
    from mapbuild_cities import lighthouse
    lighthouse(b, QX + 52, by, wl + 3, 26)
    for dx in range(-4, 5):
        for dz in range(-4, 5):
            if math.hypot(dx, dz) <= 4.5:
                for y in range(2, wl + 3):
                    b.set(QX + 52 + dx, y, by + dz, 'minecraft:stone_bricks')
    b.door(QX + 49, wl + 3, by, 'west')

    # --- верфь (юг): спуск к воде, корабль на стапеле, брёвна ---
    yz = zs + 7
    if yz + 6 < S - 4:
        for x in range(QX - 14, QX + 16):
            y_top = G - 1 - max(0, (x - QX + 2) // 3)
            if y_top < 3:
                break
            for dz in range(-5, 6):
                z = yz + dz
                for y in range(2, y_top + 1):
                    b.set(x, y, z, 'minecraft:stone' if y < y_top else ('minecraft:spruce_planks' if abs(dz) <= 2 else 'minecraft:gravel'))
                for y in range(max(y_top + 1, wl + 1), G + 1):
                    b.set(x, y, z, 'minecraft:air')
        shipyard_ribs(b, QX - 11, yz, G, L=18)
        for k in range(2):
            for dx in range(5):
                b.set(QX - 14 + dx, G, yz + 4 - k, 'minecraft:spruce_log', axis='x')
        b.entities.append((QX - 6, G, yz - 3, 'litrpg:human_citizen', {}))
        b.reserve(QX - 15, yz - 6, QX + 16, yz + 6)

    # --- складской ряд у набережной ---
    import mapbuild_crafts as MCR
    wx0 = QX - 17
    z = zn + 6
    made = 0
    while z + 9 < zs - 4:
        w = 10 if made % 3 != 1 else 9
        if made == 2:
            MCR.workshop(b, wx0, z, 'innkeeper', 'east', floors=2, w=11, d=13, check=False)   # «Пьяный якорь»
            z += 13 + 2
        elif made == 4:
            MCR.workshop(b, wx0, z, 'fishmonger', 'east', floors=1, w=10, d=9, check=False)
            z += 9 + 2
        else:
            b.house(wx0, z, 10, w, floors=2, door='east', roof=st['roofs'][2], interior='shop', npc='litrpg:human_merchant' if made % 2 else None)
            # подъёмник: балка над дверью с цепью и бочкой
            b.set(QX - 6, G + 6, z + w // 2, 'minecraft:spruce_fence', waterlogged='false')
            b.set(QX - 7, G + 6, z + w // 2, 'minecraft:spruce_fence', waterlogged='false')
            for y in range(G + 3, G + 6):
                b.set(QX - 6, y, z + w // 2, 'minecraft:chain', axis='y', waterlogged='false')
            for k in range(3):
                b.set(QX - 5 + (k % 2), G, z + 1 + k * 3 % max(1, w - 2), 'minecraft:barrel', facing='up', open='false') if k < 2 else None
            z += w + 2
        made += 1
    b.reserve(wx0 - 1, zn + 5, QX - 6, zs - 4)
    # переулки между складами
    for zz in range(zn + 6, zs - 4):
        if ML._air(b, wx0, G + 2, zz) and ML._air(b, wx0 + 5, G + 2, zz):
            for x in range(wx0 - 1, QX - 7):
                b.set(x, G - 1, zz, b.bp(st['street'], x, 0, zz))

    # --- проспекты, площадь, особые здания ---
    PR = 16

    def plaza_pat(x, z, d):
        ring = int(d) % 5
        return 'minecraft:polished_diorite' if ring == 0 else 'minecraft:smooth_stone' if ring in (1, 4) else 'minecraft:polished_andesite'
    t.plaza(PR, plaza_pat)
    b.fountain(C, C, 4, 'minecraft:prismarine_bricks')
    for k in range(8):
        a = 2 * math.pi * k / 8 + 0.2
        x, z = t.polar(a, PR - 2)
        b.lamp_post(x, z, 3) if k % 2 else b.bench(x, z, cardinal(a + math.pi))
    for a in t.gates:
        t.avenue(a, PR, half=2)
        t.approach(a, half=2)
    # бульвар к гавани
    cells = {c for c in t.strip(0.0, PR, Q - 6, 3)}
    t.pave(cells, ['minecraft:smooth_stone', 'minecraft:polished_andesite', 'minecraft:stone_bricks'])
    t.reserve_cells(cells)
    t.avenues.append((0.0, PR, Q - 6, 3))
    # ратуша с часовой башней — к северу от площади
    sp = t.spot(24, 16, -math.pi / 2, PR + 14)
    if sp:
        x0, z0, x1, z1 = sp
        hall(b, x0, z0, 24, 16, t.facing_center(*sp), floors=3, roof=st['roofs'][0], interior='library', npc='litrpg:human_knight',
             tower='minecraft:dark_prismarine')
        map_wall(t, (x0 + x1) // 2, z1 + 4)
    # храм Моря — к югу
    sp = t.spot(18, 28, math.pi / 2 + 0.5, PR + 22)
    if sp:
        sea_temple(b, *sp)
    # казармы у западных ворот
    sp = t.spot(18, 11, t.gates[0] + 0.35, t.WR - 22)
    if sp:
        b.house(sp[0], sp[1], 18, 11, floors=2, door=t.facing_center(*sp), interior='guard', npc='litrpg:human_knight')
    # гильдия мореходов — у бульвара
    sp = t.spot(16, 12, -0.45, Q - 26)
    if sp:
        b.house(sp[0], sp[1], 16, 12, floors=2, door=t.facing_center(*sp), roof=st['roofs'][3], interior='library', npc='litrpg:human_ranger')

    # сюжет «Чёрный парус»: грот контрабандистов под набережной и дом начальника порта с тайным подвалом
    import mapbuild_secrets as SEC
    if len(piers) > 2:
        SEC.sea_cave(b, QX, piers[1] + 12, wl, G)
    SEC.town_secret(t, 'smugglers', -0.9, Q - 30)
    SEC.anchor(b, 'quay', *SEC.walk_spot(b, QX - 3, (zn + zs) // 2, G, 6))
    import mapbuild_inns as MI
    MI.town_extras(t)   # постоялый двор и круг телепорта у проспектов
    t.quarters(_crafts('haven', drop=('innkeeper', 'fishmonger')), 'litrpg:human_citizen', 'litrpg:human_merchant', terrace=2,
               market=(22, 14), market_crafts=['fishmonger', 'baker', 'farmer', 'tailor', 'herbalist', 'jeweler'],
               inside=lambda x, z: t.in_walls(x, z, 4) and x < QX - 19)
    t.lamps_on_avenues()
    t.countryside('litrpg:human_citizen', 'human', n_fields=12)
    # площадка рельефа: суша до набережной, за ней — гавань, вырытая до y=58 (море подходит само)
    b.fit = dict(cut=[1.0, 0.0, Q + 1],
                 extra=[[0, 0, S // 2 + 8, 58, 48, -1.0, 0.0, -(Q + 1)]])
    SEC.finish(t, ['litrpg:human_knight'])
    return b


def sea_temple(b, x0, z0, x1, z1):
    """Храм Моря: призмарин и кварц, витражи цвета волны, своды, алтарь с маяком-сердцем."""
    G = b.G
    w, d = x1 - x0 + 1, z1 - z0 + 1
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            b.set(x, G - 1, z, 'minecraft:prismarine_bricks' if (x + z) % 2 else 'minecraft:smooth_quartz')
            for y in range(G, G + 12):
                if edge:
                    win = (z - z0) % 4 in (1, 2) and 3 <= y - G <= 9 and x in (x0, x1) and z0 < z < z1
                    if win:
                        b.set(x, y, z, 'minecraft:light_blue_stained_glass_pane' if (y - G) % 3 else 'minecraft:cyan_stained_glass_pane',
                              waterlogged='false')
                    else:
                        b.set(x, y, z, 'minecraft:quartz_bricks' if (z - z0) % 4 else 'minecraft:prismarine_bricks')
                else:
                    b.set(x, y, z, 'minecraft:air')
    b.roof(x0, z0, x1, z1, G + 12, ('minecraft:dark_prismarine_stairs', 'minecraft:dark_prismarine_slab', 'minecraft:dark_prismarine'))
    ax = (x0 + x1) // 2
    for z in range(z0 + 4, z1 - 3, 2):
        for x in list(range(x0 + 2, ax - 1)) + list(range(ax + 2, x1 - 1)):
            b.stair(x, G, z, 'minecraft:spruce_stairs', 'south')
    for z in range(z0 + 1, z1):
        b.set(ax, G, z, 'minecraft:light_blue_carpet') if z > z0 + 3 else None
    for x in range(ax - 2, ax + 3):
        b.set(x, G, z0 + 2, 'minecraft:prismarine_bricks')
    b.set(ax, G + 1, z0 + 2, 'minecraft:conduit', waterlogged='false')
    for dx in (-2, 2):
        b.set(ax + dx, G + 1, z0 + 2, 'minecraft:sea_lantern')
    for z in range(z0 + 3, z1 - 2, 5):
        b.set(ax, G + 10, z, 'minecraft:chain', axis='y', waterlogged='false')
        b.set(ax, G + 9, z, 'minecraft:sea_lantern')
    b.door(ax, G, z1, 'south')
    b.door(ax - 1, G, z1, 'south')
    for y in range(G + 12, G + 22):
        b.set(ax, y, z1 - 1, 'minecraft:prismarine_wall' if y < G + 21 else 'minecraft:sea_lantern')
    b.entities.append((ax, G, z0 + 5, 'litrpg:human_mage', {}))
