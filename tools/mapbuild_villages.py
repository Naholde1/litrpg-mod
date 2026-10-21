"""
Деревни, придорожные хутора, лесопилки и рыбацкие посёлки (2.8.0).
Деревня — улочки-кварталы вокруг зелёной площади с колодцем и старым дубом, часовня, таверна, кузня,
дома с огородами, за околицей — поля, мельница, пастбища. Главная улица смотрит на тракт.
Придорожные хутора стоят прямо у трактов: постоялый двор, мельница, кузня, часовня путника.
"""
import math
import random

from mapbuild_lib import City, DIRS, OPP, TByte, TInt  # noqa: F401
import mapbuild_layout as ML
from mapbuild_towns import Town, cardinal, _ground_rect, _fence_rect, _animals

HAMLET_NAMES = {
    'inn': ['Постоялый двор «Сытый гусь»', 'Постоялый двор «Три подковы»', 'Постоялый двор «Тихая гавань»', 'Постоялый двор «Хромой мул»',
            'Постоялый двор «Добрый путь»', 'Постоялый двор «Седой ворон»', 'Постоялый двор «Медный котёл»', 'Постоялый двор «Красный фонарь»'],
    'mill': ['Мельница у брода', 'Мельничий хутор', 'Мельница Старого Ганса', 'Ветряной хутор', 'Мельница на холме'],
    'smithy': ['Придорожная кузня', 'Кузня у развилки', 'Кузня Железной Марты', 'Подкова и молот'],
    'shrine': ['Часовня Путника', 'Часовня Светлой Дороги', 'Приют паломников', 'Часовня Тихой Молитвы'],
}


def _tavern_sign_yard(b, x0, z0, x1, z1):
    """Двор у таверны: столы под навесом, бочки, коновязь."""
    G = b.G
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, b.bp(['minecraft:cobblestone', 'minecraft:gravel', 'minecraft:cobblestone', 'minecraft:coarse_dirt'], x, 0, z))
    for x in range(x0 + 1, x1, 4):
        b.set(x, G, z0 + 1, 'minecraft:spruce_fence', waterlogged='false')
        b.set(x, G + 1, z0 + 1, 'minecraft:spruce_pressure_plate', powered='false')
        b.stair(x - 1, G, z0 + 1, 'minecraft:spruce_stairs', 'west')
        b.stair(x + 1, G, z0 + 1, 'minecraft:spruce_stairs', 'east')
    b.set(x1, G, z1, 'minecraft:barrel', facing='up', open='false')
    b.set(x1 - 1, G, z1, 'minecraft:barrel', facing='up', open='false')
    b.set(x1, G + 1, z1, 'minecraft:barrel', facing='up', open='false')


def seg_cells(p, q, half):
    """Клетки полосы шириной 2*half+1 вдоль отрезка p-q."""
    (ax, az), (bx, bz) = p, q
    L = math.hypot(bx - ax, bz - az) or 1
    ux, uz = (bx - ax) / L, (bz - az) / L
    out = set()
    t = 0.0
    while t <= L:
        for w2 in range(-half * 2, half * 2 + 1):
            out.add((int(round(ax + ux * t - uz * w2 / 2)), int(round(az + uz * t + ux * w2 / 2))))
        t += 0.5
    return out


def houses_along(t, p, q, half, rnd, citizen, n_max=99, setback=2, kinds=('home', 'home', 'home', 'shop'), floors=(1, 1, 2),
                 gardens=True, skip_near=None, streets=()):
    """Дома по обе стороны улицы p-q дверью на улицу, за домом — огород за плетнём."""
    b, G = t.b, t.G
    (ax, az), (bx, bz) = p, q
    L = math.hypot(bx - ax, bz - az) or 1
    ux, uz = (bx - ax) / L, (bz - az) / L
    nx, nz = -uz, ux
    made = 0
    for s in (-1, 1):
        tt = 3.0
        while tt < L - 3 and made < n_max:
            w, d = rnd.randint(6, 8), rnd.randint(6, 7)
            door = cardinal(math.atan2(-nz * s, -nx * s))
            W, D = (w, d) if door in ('north', 'south') else (d, w)
            # на косой улице прямоугольник дома выступает углом — отступ по проекции на нормаль
            ext = W / 2 * abs(nx) + D / 2 * abs(nz) + 0.6
            px = ax + ux * tt + nx * s * (half + setback + ext)
            pz = az + uz * tt + nz * s * (half + setback + ext)
            x0, z0 = int(round(px - W / 2)), int(round(pz - D / 2))
            x1, z1 = x0 + W - 1, z0 + D - 1
            ok = all(t.land(x, z, 3) for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1))) and b.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1)                 and not any((x, z) in streets for x in range(x0 - 1, x1 + 2) for z in range(z0 - 1, z1 + 2))
            if ok and skip_near and any(math.hypot((x0 + x1) / 2 - sx, (z0 + z1) / 2 - sz) < sr for sx, sz, sr in skip_near):
                ok = False
            if ok:
                fl = rnd.choice(floors)
                b.house(x0, z0, W, D, floors=fl, door=door, interior=rnd.choice(kinds) if fl > 1 else 'home',
                        npc=citizen if rnd.random() < 0.55 else None)
                made += 1
                # дорожка от двери к улице
                dx_, dz_, _ = b.last_door
                ox, oz = DIRS[door]
                for k in range(1, setback + 2):
                    cx, cz = dx_ + ox * k, dz_ + oz * k
                    if b.blocks.get((cx, G, cz), ('minecraft:air',))[0] == 'minecraft:air':
                        b.set(cx, G - 1, cz, b.bp(b.st['path'], cx, 0, cz))
                if gardens:
                    _garden_behind(t, x0, z0, x1, z1, door, rnd, streets)
                tt += max(W, D) + rnd.randint(2, 4)
            else:
                tt += 2
    return made


def _garden_behind(t, x0, z0, x1, z1, door, rnd, streets=()):
    b, G = t.b, t.G
    bx, bz = DIRS[OPP[door]]
    depth = rnd.randint(4, 6)
    if bx:
        gx0, gx1 = (x1 + 2, x1 + 1 + depth) if bx > 0 else (x0 - 1 - depth, x0 - 2)
        gz0, gz1 = z0, z1
    else:
        gz0, gz1 = (z1 + 2, z1 + 1 + depth) if bz > 0 else (z0 - 1 - depth, z0 - 2)
        gx0, gx1 = x0, x1
    if not b.free(gx0 - 1, gz0 - 1, gx1 + 1, gz1 + 1) or not all(t.land(x, z, 3) for x, z in ((gx0, gz0), (gx1, gz1), (gx0, gz1), (gx1, gz0))):
        return
    if any((x, z) in streets for x in range(gx0 - 1, gx1 + 2) for z in range(gz0 - 1, gz1 + 2)):
        return
    crops = ['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots']
    kind = rnd.random()
    for x in range(gx0, gx1 + 1):
        for z in range(gz0, gz1 + 1):
            edge = x in (gx0, gx1) or z in (gz0, gz1)
            if edge:
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                b.set(x, G, z, b.st['fence'], waterlogged='false')
            elif kind < 0.7:
                if (x * 3 + z) % 7 == 0:
                    b.set(x, G - 1, z, 'minecraft:water', level='0')
                    b.set(x, G, z, 'minecraft:oak_trapdoor', facing='north', half='bottom', open='false', powered='false', waterlogged='true')
                else:
                    c = crops[((x - gx0) // 2 + (z - gz0)) % len(crops)] if bx else crops[((z - gz0) // 2 + (x - gx0)) % len(crops)]
                    b.set(x, G - 1, z, 'minecraft:farmland', moisture='7')
                    b.set(x, G, z, c, age='3' if c.endswith('beetroots') else '7')
            else:
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                if (x + z) % 3 == 0:
                    b.set(x, G, z, 'minecraft:' + rnd.choice(['poppy', 'cornflower', 'oxeye_daisy', 'azure_bluet', 'grass']))
    # калитка со стороны дома
    gx, gz = (gx0 if bx > 0 else gx1, (gz0 + gz1) // 2) if bx else ((gx0 + gx1) // 2, gz0 if bz > 0 else gz1)
    gate = b.st['fence'].replace('_fence', '_fence_gate')
    b.set(gx, G, gz, gate, facing='east' if bx else 'north', in_wall='false', open='false', powered='false')
    if kind >= 0.7:
        b.tree((gx0 + gx1) // 2, (gz0 + gz1) // 2, 4, 'minecraft:oak_log', rnd.choice(['minecraft:oak_leaves', 'minecraft:flowering_azalea_leaves']), 2.0)
    elif rnd.random() < 0.4:
        b.entities.append(((gx0 + gx1) // 2, G, (gz0 + gz1) // 2, 'minecraft:chicken', {}))
    b.reserve(gx0 - 1, gz0 - 1, gx1 + 1, gz1 + 1)


def village(seed, style='human', road_a=None, name=None):
    """Деревня: главная улица от тракта через зелёную площадь (колодец, старый дуб), два переулка,
    часовня с погостом, таверна, кузня, дома дверями на улицу с огородами, за околицей — мельница и поля."""
    import mapbuild_crafts as MCR
    import mapbuild_castle as MCS
    from mapbuild_towns2 import map_wall
    rnd = random.Random(seed * 13 + 1)
    S, G = 128, 4
    road_a = road_a if road_a is not None else rnd.random() * 2 * math.pi
    back = road_a + math.pi + rnd.uniform(-0.25, 0.25)
    t = Town('village%d' % seed, S, style, 36, [road_a, back], sides=7, ground=G, height=48)
    b, C = t.b, t.C
    st = b.st
    t.outer = S // 2 - 3
    t.paint_base()
    # улицы: главная насквозь, два переулка поперёк
    R = 44
    main_p, main_q = t.polar(road_a, R), t.polar(back, R)
    lanes = []
    for k, off in enumerate((rnd.uniform(12, 16), -rnd.uniform(12, 16), rnd.choice((1, -1)) * rnd.uniform(26, 30))):
        bx_, bz_ = C + math.cos(road_a) * off, C + math.sin(road_a) * off
        side = road_a + math.pi / 2 * (1 if k % 2 == 0 else -1) + rnd.uniform(-0.2, 0.2)
        L = rnd.uniform(24, 34)
        lanes.append(((round(bx_), round(bz_)), (round(bx_ + math.cos(side) * L), round(bz_ + math.sin(side) * L))))
    streets = seg_cells(main_p, main_q, 1)
    for p, q in lanes:
        streets |= seg_cells(p, q, 1)
    streets = {c for c in streets if t.land(*c, 1)}
    t.pave(streets, st['path'], under='minecraft:dirt')
    t.approach(road_a, half=1)
    # зелёная площадь
    PR = 7
    for x in range(C - PR, C + PR + 1):
        for z in range(C - PR, C + PR + 1):
            d = math.hypot(x - C, z - C)
            if d <= PR + 0.4:
                if d > PR - 1.3:
                    b.set(x, G - 1, z, b.bp(st['path'], x, 0, z))
                else:
                    b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
    b.reserve(C - PR, C - PR, C + PR, C + PR)
    b.well(C + 3, C + 1)
    b.tree(C - 3, C - 1, 7, 'minecraft:dark_oak_log', 'minecraft:oak_leaves', 3.4)
    for (x, z, f) in ((C - 3, C + 3, 'north'), (C - 1, C + 3, 'north'), (C + 4, C - 3, 'west')):
        b.bench(x, z, f)
    for k in range(4):
        b.lamp_post(*t.polar(math.pi / 4 + k * math.pi / 2, PR - 1), 3)
    b.entities.append((C + 1, G, C - 3, 'litrpg:human_merchant', {}))
    # особые дома у площади: часовня, таверна, кузня
    near = []
    sp = t.spot(11, 15, road_a + math.pi * 0.62, 18, inner=-4, margin=1)
    if sp:
        MCS.chapel(b, sp[0], sp[1], 11, 15)
        near.append(((sp[0] + sp[2]) / 2, (sp[1] + sp[3]) / 2, 9))
        gx0, gz0 = sp[2] + 2, sp[1] + 2
        if b.free(gx0, gz0, gx0 + 5, gz0 + 8) and t.land(gx0 + 5, gz0 + 8, 3):
            for x in range(gx0, gx0 + 6):
                for z in range(gz0, gz0 + 9):
                    edge = x in (gx0, gx0 + 5) or z in (gz0, gz0 + 8)
                    b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                    if edge:
                        b.set(x, G, z, 'minecraft:cobblestone_wall')
                    elif (x - gx0) % 2 == 1 and (z - gz0) % 3 == 1:
                        b.set(x, G, z, 'minecraft:mossy_stone_brick_wall' if (x + z) % 2 else 'minecraft:stone_brick_wall')
                        b.set(x, G - 1, z, 'minecraft:coarse_dirt')
            b.set(gx0 + 2, G, gz0 + 8, 'minecraft:air')
            b.reserve(gx0, gz0, gx0 + 5, gz0 + 8)
    sp = t.spot(13, 10, road_a + 0.75, 16, inner=-4, margin=1)
    if sp:
        MCR.workshop(b, sp[0], sp[1], 'innkeeper', t.facing_center(*sp), floors=2, w=13, d=10, check=False)
    sp = t.spot(10, 9, road_a - 0.75, 16, inner=-4, margin=1)
    if sp:
        MCR.workshop(b, sp[0], sp[1], 'blacksmith', t.facing_center(*sp), floors=1, w=10, d=9, check=False)
    sp = t.spot(11, 9, back + 0.8, 18, inner=-4, margin=1)
    if sp:
        MCR.workshop(b, sp[0], sp[1], 'farmer' if rnd.random() < 0.7 else 'herbalist', t.facing_center(*sp), floors=1, w=11, d=9, check=False)
    mx, mz = t.polar(road_a + 0.22, 47)
    map_wall(t, mx, mz, frame='minecraft:spruce_planks', face='minecraft:stripped_spruce_wood', cap='minecraft:spruce_fence')
    # дома вдоль улиц
    homes = houses_along(t, main_p, main_q, 1, rnd, 'litrpg:human_citizen', streets=streets)
    for p, q in lanes:
        homes += houses_along(t, p, q, 1, rnd, 'litrpg:human_citizen', streets=streets)
    t.reserve_cells(streets)
    # фонари вдоль главной улицы
    L = math.hypot(main_q[0] - main_p[0], main_q[1] - main_p[1])
    for k in range(4, int(L) - 3, 11):
        x = int(round(main_p[0] + (main_q[0] - main_p[0]) * k / L + math.sin(road_a) * 2))
        z = int(round(main_p[1] + (main_q[1] - main_p[1]) * k / L - math.cos(road_a) * 2))
        if b.free(x, z, x, z) and ML._air(b, x, G, z) and t.land(x, z, 3):
            b.lamp_post(x, z, 3)
            b.reserve(x, z, x, z)
    for k in range(3):
        b.entities.append((C + rnd.randint(-4, 4), G, C + rnd.randint(-4, 4), rnd.choice(['minecraft:villager', 'litrpg:human_citizen',
                                                                                         'minecraft:chicken']), {}))
    print(f'  деревня {seed}: домов {homes}')
    t.countryside('litrpg:human_citizen', 'human', n_fields=8, wall_pad=6,
                  mix={'pasture': 3, 'orchard': 2, 'house': 2, 'melons': 1, 'field': 2, 'windmill': 2, 'pond': 1, 'bees': 1})
    return b



def lumber_camp(seed):
    """Лесопилка: пильный навес с «пилой», штабели брёвен и досок, дома лесорубов, телега, пни, питомник."""
    import mapbuild_crafts as MCR
    rnd = random.Random(seed * 7 + 5)
    S, G = 64, 4
    b = City(S, 40, S, 'lumber%d' % seed, 'human', ground=G)
    C = S // 2
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - C, z - C)
            if r < C - 2:
                n = (x * 7 + z * 13) % 9
                b.set(x, G - 1, z, 'minecraft:coarse_dirt' if n == 0 else 'minecraft:podzol' if n == 1 else 'minecraft:grass_block',
                      **({} if n in (0, 1) else {'snowy': 'false'}))
    # пильный навес
    x0, z0 = C - 8, C - 4
    for x in range(x0, x0 + 14):
        for z in range(z0, z0 + 8):
            b.set(x, G - 1, z, 'minecraft:spruce_planks')
            if (x - x0) % 4 == 0 and z in (z0, z0 + 7):
                for y in range(G, G + 4):
                    b.set(x, y, z, 'minecraft:spruce_log', axis='y')
    b.roof(x0, z0, x0 + 13, z0 + 7, G + 4, ('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks'))
    for x in range(x0 + 1, x0 + 13):
        b.set(x, G, C, 'minecraft:stripped_oak_log', axis='x') if x < C else None
    b.set(C, G, C, 'minecraft:stonecutter', facing='east')
    b.set(C + 1, G, C, 'minecraft:stonecutter', facing='east')
    for x in range(C + 2, x0 + 13):
        b.set(x, G, C, 'minecraft:oak_slab', type='bottom', waterlogged='false')
        b.set(x, G, C + 2, 'minecraft:oak_planks')
        b.set(x, G + 1, C + 2, 'minecraft:oak_slab', type='bottom', waterlogged='false')
    b.set(x0 + 2, G + 3, C, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.entities.append((C - 1, G, C + 1, 'litrpg:human_citizen', {}))
    b.reserve(x0 - 1, z0 - 1, x0 + 14, z0 + 8)
    # дома и мастерская плотника
    MCR.workshop(b, C - 14, C - 18, 'builder', 'south', floors=1, w=10, d=8, check=False)
    b.house(C + 4, C - 17, 8, 7, floors=2, door='south', interior='home', npc='litrpg:human_citizen')
    b.house(C + 6, C + 9, 7, 7, floors=1, door='north', interior='home', npc='litrpg:human_ranger')
    # штабели брёвен
    for k in range(8):
        lx, lz = rnd.randint(5, S - 12), rnd.randint(5, S - 8)
        if b.free(lx - 1, lz - 1, lx + 6, lz + 3) and math.hypot(lx + 3 - C, lz + 1 - C) < C - 6:
            wood = rnd.choice(['minecraft:spruce_log', 'minecraft:oak_log', 'minecraft:birch_log', 'minecraft:dark_oak_log'])
            for x in range(lx, lx + 6):
                for z in range(lz, lz + 3):
                    for y in range(G, G + (2 if z == lz + 1 else 1)):
                        b.set(x, y, z, wood, axis='x')
            b.reserve(lx - 1, lz - 1, lx + 6, lz + 3)
    # пни вокруг — вырубка, и молодые саженцы
    for k in range(40):
        x, z = rnd.randint(3, S - 4), rnd.randint(3, S - 4)
        r = math.hypot(x - C, z - C)
        if 14 < r < C - 3 and b.free(x, z, x, z) and (x, G, z) not in b.blocks:
            if k % 3:
                b.set(x, G, z, 'minecraft:spruce_log', axis='y')
                if rnd.random() < 0.5:
                    b.set(x, G + 1, z, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
            else:
                b.set(x, G, z, rnd.choice(['minecraft:spruce_sapling', 'minecraft:oak_sapling', 'minecraft:birch_sapling']), stage='0')
    # телега с брёвнами и лошадь
    from mapbuild_pois import wagon
    wagon(b, C - 2, C + 12, 'x', 'brown')
    b.entities.append((C - 8, G, C + 12, 'minecraft:horse', {'Variant': TInt(rnd.choice([0, 1, 2, 3])), 'Tame': TByte(1)}))
    from mapbuild_cities import bonfire
    bonfire(b, C + 13, C + 2)
    b.entities.append((C + 11, G, C + 4, 'litrpg:human_citizen', {}))
    return b


def fishing_village(seed, sea_a):
    """Рыбацкий посёлок у воды: хижины, сушилки сетей, коптильня, мостки на сваях, лодки.
    sea_a — направление к воде; площадка рельефа срезана по берегу (b.fit)."""
    rnd = random.Random(seed * 3 + 9)
    S, G = 80, 10
    wl = G - 3          # вода y=63 при площадке 66
    b = City(S, 48, S, 'fish%d' % seed, 'human', ground=G)
    C = S // 2
    ux, uz = math.cos(sea_a), math.sin(sea_a)
    Q = 12  # берег — на 12 блоков от центра в сторону воды
    for x in range(S):
        for z in range(S):
            r = math.hypot(x - C, z - C)
            p = (x - C) * ux + (z - C) * uz
            if r < C - 3 and p < Q:
                sand = p > Q - 6
                b.set(x, G - 1, z, 'minecraft:sand' if sand else 'minecraft:grass_block', **({} if sand else {'snowy': 'false'}))
                if sand and p > Q - 3:
                    for y in range(G - 4, G - 1):
                        b.set(x, y, z, 'minecraft:sand')
    # мостки на сваях
    for k, off in enumerate((-10, 0, 10)):
        L = 20 if k != 1 else 26
        for t in range(Q - 3, Q + L):
            cx = C + ux * t - uz * off
            cz = C + uz * t + ux * off
            for w in (-1, 0, 1):
                x, z = int(round(cx - uz * w)), int(round(cz + ux * w))
                if 0 <= x < S and 0 <= z < S:
                    b.set(x, G - 1, z, 'minecraft:spruce_planks')
            if t % 4 == 0:
                for w in (-2, 2):
                    x, z = int(round(cx - uz * w)), int(round(cz + ux * w))
                    if 0 <= x < S and 0 <= z < S:
                        for y in range(1, G):
                            b.set(x, y, z, 'minecraft:stripped_spruce_log', axis='y')
                        b.set(x, G, z, 'minecraft:spruce_fence', waterlogged='false')
                        if t % 8 == 0:
                            b.set(x, G + 1, z, 'minecraft:lantern', hanging='false', waterlogged='false')
        bx, bz = int(C + ux * (Q + L * 0.6) - uz * (off + 4)), int(C + uz * (Q + L * 0.6) + ux * (off + 4))
        if 0 <= bx < S and 0 <= bz < S:
            b.entities.append((bx, wl + 1, bz, 'minecraft:boat', {'Type': rnd.choice(['spruce', 'oak', 'birch'])}))
    # хижины полукругом, дверью к воде
    homes = 0
    for k in range(9):
        a = sea_a + math.pi + (k - 4) * 0.42
        r = rnd.uniform(14, 22)
        w, d = rnd.randint(6, 8), rnd.randint(5, 7)
        x0, z0 = int(C + math.cos(a) * r - w / 2), int(C + math.sin(a) * r - d / 2)
        if b.free(x0 - 2, z0 - 2, x0 + w + 1, z0 + d + 1) and all(math.hypot(px - C, pz - C) < C - 5 for px, pz in
                                                                  ((x0, z0), (x0 + w, z0), (x0, z0 + d), (x0 + w, z0 + d))):
            b.house(x0, z0, w, d, floors=rnd.choice([1, 1, 2]), door=cardinal(sea_a), interior='home',
                    npc=rnd.choice(['litrpg:human_citizen', 'minecraft:villager', None]))
            homes += 1
    # коптильня-мастерская и сушилки сетей
    import mapbuild_crafts as MCR
    sx, sz = int(C - ux * 6 - 5), int(C - uz * 6 - 4)
    if b.free(sx - 1, sz - 1, sx + 10, sz + 9):
        MCR.workshop(b, sx, sz, 'fishmonger', cardinal(sea_a), floors=1, w=10, d=9, check=False)
    for k in range(5):
        a = sea_a + (k - 2) * 0.5
        x, z = int(C + math.cos(a) * 7), int(C + math.sin(a) * 7)
        if b.free(x - 1, z - 1, x + 3, z + 1):
            for dx in range(3):
                b.set(x + dx, G, z, 'minecraft:spruce_fence', waterlogged='false')
                b.set(x + dx, G + 1, z, 'minecraft:cobweb' if dx == 1 else 'minecraft:spruce_fence', waterlogged='false') if dx != 1 else \
                    b.set(x + dx, G + 1, z, 'minecraft:cobweb')
            b.set(x, G, z + 1, 'minecraft:barrel', facing='up', open='false')
            b.reserve(x - 1, z - 1, x + 3, z + 1)
    from mapbuild_cities import bonfire
    bonfire(b, int(C - ux * 2), int(C - uz * 2))
    b.entities.append((C, G, C, 'litrpg:human_merchant', {}))
    # площадка рельефа: суша до берега, дальше дно у мостков (y=59) — вода подходит сама
    b.fit = dict(cut=[ux, uz, Q + 1], extra=[[0, 0, S // 2 + 6, 59, 40, -ux, -uz, -(Q + 1)]])
    return b


# =====================================================================
#                      ПРИДОРОЖНЫЕ ХУТОРА
# =====================================================================

def hamlet(seed, kind, road_a, style='human'):
    """Хутор у тракта: главный дом смотрит на дорогу, перед ним двор с колодцем и телегой, по бокам — службы,
    позади — огороды и поле. road_a — направление от центра хутора к дороге."""
    import mapbuild_crafts as MCR
    import mapbuild_castle as MCS
    rnd = random.Random(seed * 17 + 3)
    S, G = 64, 4
    t = Town('hamlet%d' % seed, S, style, 18, [road_a], sides=6, ground=G, height=48)
    b, C = t.b, t.C
    st = b.st
    t.outer = S // 2 - 2
    t.paint_base()
    ux, uz = math.cos(road_a), math.sin(road_a)
    side = road_a + math.pi / 2
    # двор между домом и дорогой, тропа к дороге
    YX, YZ = C + ux * 14, C + uz * 14
    yard = {(x, z) for x in range(int(YX) - 8, int(YX) + 9) for z in range(int(YZ) - 8, int(YZ) + 9)
            if math.hypot(x - YX, z - YZ) <= 7.5 and t.land(x, z, 1)}
    t.pave(yard, st['path'], under='minecraft:dirt')
    b.reserve(int(YX) - 6, int(YZ) - 6, int(YX) + 6, int(YZ) + 6)
    lane = {c for c in t.strip(road_a, 20, S // 2 - 1, 1) if t.land(*c, -2)}
    t.pave(lane, st['path'], under='minecraft:dirt')
    t.reserve_cells(lane, pad=1)

    def put(w, d, a, r):
        return t.spot(w, d, a, r, inner=-30, margin=1)

    def facing_road(sp):
        cx, cz = (sp[0] + sp[2]) / 2, (sp[1] + sp[3]) / 2
        return cardinal(math.atan2(YZ - cz, YX - cx))
    if kind == 'inn':
        sp = put(15, 11, road_a + math.pi, 1)
        if sp:
            MCR.workshop(b, sp[0], sp[1], 'innkeeper', facing_road(sp), floors=2, w=15, d=11, check=False)
        sp = put(10, 13, side, 15)
        if sp:
            MCS.stable(b, sp[0], sp[1], 10, 13, open_side='east' if (sp[0] + sp[2]) / 2 < YX else 'west')
        sp = put(9, 7, side + math.pi, 15)
        if sp:
            _tavern_sign_yard(b, *sp)
            b.reserve(*sp)
        from mapbuild_pois import wagon
        wagon(b, int(YX + math.cos(side) * 3), int(YZ + math.sin(side) * 3), 'x' if abs(ux) < 0.7 else 'z',
              rnd.choice(['red', 'blue', 'green', 'yellow']))
        b.well(int(YX - math.cos(side) * 3), int(YZ - math.sin(side) * 3))
        b.entities.append((int(YX), G, int(YZ), 'litrpg:human_merchant', {}))
        b.entities.append((int(YX) + 1, G, int(YZ) + 2, 'litrpg:human_knight', {}))
    elif kind == 'mill':
        import mapbuild_pois as MP
        mx, mz = t.polar(road_a + math.pi + 0.5, 10)
        MP.windmill(b, mx, mz, h=13)
        sp = put(11, 9, side, 13)
        if sp:
            MCR.workshop(b, sp[0], sp[1], 'farmer', facing_road(sp), floors=1, w=11, d=9, check=False)
        sp = put(8, 7, side + math.pi, 13)
        if sp:
            b.house(sp[0], sp[1], 8, 7, floors=2, door=facing_road(sp), interior='home', npc='litrpg:human_citizen')
        for k in range(3):
            x, z = int(YX + math.cos(side) * (k - 1) * 3), int(YZ + math.sin(side) * (k - 1) * 3)
            b.set(x, G, z, 'minecraft:hay_block', axis='y')
            if k == 1:
                b.set(x, G + 1, z, 'minecraft:hay_block', axis='x')
        b.entities.append((int(YX) + 2, G, int(YZ), 'litrpg:human_citizen', {}))
    elif kind == 'smithy':
        sp = put(11, 9, road_a + math.pi, 1)
        if sp:
            MCR.workshop(b, sp[0], sp[1], 'blacksmith', facing_road(sp), floors=1, w=11, d=9, check=False)
        sp = put(8, 7, side, 14)
        if sp:
            b.house(sp[0], sp[1], 8, 7, floors=2, door=facing_road(sp), interior='home', npc='litrpg:human_citizen')
        sp = put(9, 7, side + math.pi, 14)
        if sp:
            x0, z0, x1, z1 = sp
            _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
            _fence_rect(b, x0, z0, x1, z1, st['fence'], gate_side=facing_road(sp))
            _animals(b, rnd, x0, z0, x1, z1, ['minecraft:horse', 'minecraft:donkey'], 2)
            b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
        b.set(int(YX) - 1, G, int(YZ), 'minecraft:anvil', facing='north')
        b.set(int(YX) + 1, G, int(YZ), 'minecraft:grindstone', face='floor', facing='north')
        b.entities.append((int(YX), G, int(YZ) + 2, 'litrpg:human_knight', {}))
    else:  # часовня путника
        sp = put(11, 15, road_a + math.pi, 1)
        if sp:
            MCS.chapel(b, sp[0], sp[1], 11, 15)
        sp = put(8, 7, side, 14)
        if sp:
            b.house(sp[0], sp[1], 8, 7, floors=1, door=facing_road(sp), interior='library', npc='litrpg:human_mage')
        sp = put(11, 11, side + math.pi, 15)
        if sp:
            MCS.garden(b, *sp)
        for y in range(G, G + 3):
            b.set(int(YX), y, int(YZ), 'minecraft:stone_brick_wall')
        b.set(int(YX), G + 3, int(YZ), 'minecraft:lantern', hanging='false', waterlogged='false')
        b.entities.append((int(YX) - 2, G, int(YZ), 'litrpg:human_citizen', {}))
    t.countryside('litrpg:human_citizen', 'human', n_fields=3, wall_pad=-30,
                  mix={'pasture': 2, 'orchard': 1, 'melons': 1, 'field': 2, 'bees': 1, 'pond': 1})
    return b
