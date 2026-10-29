"""
Этап 5 · Шпиль Бездны (ур. 40–50). Последний путь: острова над пустотой, порталы и вершина, где ждёт Ксал'Атар.

  Последний привал → Мост над Бездной (парящие острова, осыпающийся мост, тайный островок)
  → Зал Порталов (четыре разлома в комнаты-видения; три знака открывают путь, четвёртый — ловушка)
  → АРЕНА 1: Часовой зал Хронофага (песочные часы) → Маятники → Сад парящих камней (Страж Бездны)
  → АРЕНА 2: Пасть Пожирателя миров (бой переносится в ледяной и огненный осколки миров)
  → Подъём по шпилю → АРЕНА 3: Вершина — площадка над пустотой (Ксал'Атар; в конце — Сердце Бездны) → Разлом → титры.
"""
import math

from .build import Map, Theme, Stage, apply_gates, h3
from . import decor as D
from . import secrets

OX, OZ = 5120, 0
Y = 64       # привал
YF = 60      # основной уровень
YT = 72      # вершина
YH = 72      # Сердце Бездны


def X(x):
    return OX + x


def Z(z):
    return OZ + z


def build():
    m = Map(seed=5005)
    st = Stage(5, 'spire', 'Шпиль Бездны', 'Отсюда не возвращаются — отсюда выходят', (40, 50), music='spire')

    camp = m.theme(Theme('s5_camp', [('minecraft:purpur_block', 3), ('minecraft:obsidian', 2), ('minecraft:purpur_pillar', 1)],
                         floor=[('minecraft:polished_blackstone', 2), ('minecraft:purpur_block', 1)], ceil='minecraft:obsidian', seed=51))
    abyss = m.theme(Theme('s5_abyss', [('minecraft:end_stone', 3), ('minecraft:obsidian', 2), ('minecraft:end_stone_bricks', 2), ('minecraft:crying_obsidian', 0.15)],
                          floor=[('minecraft:end_stone_bricks', 3), ('minecraft:end_stone', 1)], ceil=[('minecraft:obsidian', 3), ('minecraft:crying_obsidian', 0.3)],
                          seed=52))
    hall = m.theme(Theme('s5_hall', [('minecraft:purpur_block', 4), ('minecraft:purpur_pillar', 1), ('minecraft:end_stone_bricks', 1)],
                         floor=[('minecraft:purpur_block', 2), ('minecraft:end_stone_bricks', 2), ('minecraft:amethyst_block', 0.2)],
                         ceil='minecraft:purpur_block', seed=53))
    vision = m.theme(Theme('s5_vision', [('minecraft:amethyst_block', 3), ('minecraft:calcite', 2), ('minecraft:budding_amethyst', 0.2)],
                           floor=[('minecraft:calcite', 3), ('minecraft:amethyst_block', 1)], seed=54))
    clock = m.theme(Theme('s5_clock', [('minecraft:quartz_bricks', 3), ('minecraft:obsidian', 2), ('minecraft:chiseled_quartz_block', 0.3)],
                          floor=[('minecraft:polished_blackstone', 3), ('minecraft:quartz_bricks', 1)], ceil='minecraft:obsidian', seed=55))
    ice = m.theme(Theme('s5_ice', [('minecraft:packed_ice', 3), ('minecraft:blue_ice', 2), ('minecraft:snow_block', 1)],
                        floor=[('minecraft:blue_ice', 3), ('minecraft:packed_ice', 2)], ceil='minecraft:packed_ice', seed=56))
    fire = m.theme(Theme('s5_fire', [('minecraft:basalt', 3), ('minecraft:magma_block', 1), ('minecraft:blackstone', 2)],
                         floor=[('minecraft:blackstone', 3), ('minecraft:basalt', 2)], seed=57))
    void = m.theme(Theme('s5_void', [('minecraft:black_concrete', 4), ('minecraft:obsidian', 2), ('minecraft:crying_obsidian', 0.25)],
                         floor=[('minecraft:end_stone_bricks', 2), ('minecraft:obsidian', 1)], ceil='minecraft:black_concrete', seed=58))

    # ======================= пустоты =======================
    m.room(X(-9), Z(0), X(9), Z(15), Y, 7, camp, vault=3)
    m.corridor([(X(0), Z(-1), Y), (X(0), Z(-6), Y), (X(0), Z(-12), YF)], 5, 6, abyss)
    # Мост над Бездной: огромный провал с островами
    m.carve(X(-32), YF, Z(-85), X(32), YF + 22, Z(-13), abyss)
    m.pit(X(-32), Z(-85), X(32), Z(-13), YF, 30, abyss)
    # Зал Порталов и комнаты-видения
    m.carve(X(-2), YF, Z(-93), X(2), YF + 5, Z(-86), hall)
    m.room(X(-12), Z(-114), X(12), Z(-94), YF, 9, hall, vault=3)
    for cx in (-42, 42):
        for cz in (-99, -110):
            m.room(X(cx - 4), Z(cz - 3), X(cx + 4), Z(cz + 3), YF, 6, vision, vault=2)
    # АРЕНА 1 — Часовой зал
    m.carve(X(-2), YF, Z(-123), X(2), YF + 5, Z(-115), hall)
    m.round_room(X(0), Z(-145), 21, YF, 14, clock, dome=8)
    # маятники и Сад парящих камней
    m.carve(X(-2), YF, Z(-175), X(2), YF + 6, Z(-167), clock)
    m.room(X(-15), Z(-206), X(15), Z(-176), YF, 16, abyss, vault=4)
    # АРЕНА 2 — Пасть Пожирателя миров и осколки миров
    m.carve(X(-2), YF, Z(-214), X(2), YF + 5, Z(-207), abyss)
    m.room(X(-18), Z(-249), X(18), Z(-215), YF, 14, void, vault=5)
    m.round_room(X(-62), Z(-232), 13, YF, 10, ice, dome=5)
    m.round_room(X(62), Z(-232), 13, YF, 10, fire, dome=5)
    # подъём по шпилю
    m.corridor([(X(0), Z(-250), YF), (X(0), Z(-256), YF), (X(0), Z(-274), YT)], 5, 7, hall)
    # АРЕНА 3 — Вершина: площадка над пустотой
    m.round_room(X(0), Z(-300), 24, YT, 20, void, dome=10)
    m.pit(X(-24), Z(-324), X(24), Z(-276), YT, 30, void)
    # Сердце Бездны
    m.round_room(X(0), Z(-352), 18, YH, 18, void, dome=8)
    m.pit(X(-18), Z(-370), X(18), Z(-334), YH, 30, void)

    # ======================= отделка =======================
    camp_decor(m)
    bridge(m, st)
    portals(m, st)
    arena1(m, st)
    garden(m, st)
    arena2(m, st)
    arena3(m, st)
    packs(st)

    st.box = [X(-80), 10, Z(-376), X(80), 120, Z(22)]
    st.camp = [X(0), Y, Z(12), 180]
    st.trader = [X(-5), Y, Z(3), 135]
    m.auto_lights(spacing=8, soul=True, themes={'s5_camp', 's5_hall', 's5_clock'}, min_h=6)
    m.glow(spacing=5, level=11)
    # секретные комнаты с пасхалками (ищутся сами — tools/dungeon/secrets.py)
    secrets.add(m, st, 2, (st.box[0], st.box[2], st.box[3], st.box[5]), seed=501)
    apply_gates(m, st)
    m.shell(thick=2)
    return m, st


# ---------------------------------------------------------------- помощники

def island(m, cx, cz, r, y, top='minecraft:end_stone_bricks', body='minecraft:end_stone', depth=6, seed=0):
    """Парящий островок: диск на уровне y и конус-сталактит вниз."""
    for x in range(int(cx - r) - 1, int(cx + r) + 2):
        for z in range(int(cz - r) - 1, int(cz + r) + 2):
            d = math.hypot(x - cx, z - cz)
            if d > r + 0.3:
                continue
            m.set(X(x), y, Z(z), top if d < r - 0.7 else body)
            down = int((1 - d / (r + 0.5)) * depth + h3(x, 0, z, seed) * 2)
            for k in range(1, down + 1):
                m.set(X(x), y - k, Z(z), 'minecraft:obsidian' if k > depth - 2 else body)


def path(m, a, b, y, w=1, block='minecraft:purpur_block'):
    """Мостик шириной 2w+1 между точками (x, z)."""
    (ax, az), (bx, bz) = a, b
    n = max(abs(bx - ax), abs(bz - az), 1)
    out = []
    for i in range(n + 1):
        t = i / n
        x, z = round(ax + (bx - ax) * t), round(az + (bz - az) * t)
        for dx in range(-w, w + 1):
            for dz in range(-w, w + 1):
                m.set(X(x + dx), y, Z(z + dz), block)
                out.append((x + dx, z + dz))
    return out


def rift(m, x1, y1, z1, x2, y2, z2, frame='minecraft:crying_obsidian'):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(y1, y2 + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                m.set(X(x), y, Z(z), 'litrpg:rift')
    # рамка
    if x1 == x2:
        for z in range(min(z1, z2) - 1, max(z1, z2) + 2):
            m.set(X(x1), y2 + 1, Z(z), frame)
        for y in range(y1, y2 + 2):
            m.set(X(x1), y, Z(min(z1, z2) - 1), frame)
            m.set(X(x1), y, Z(max(z1, z2) + 1), frame)
    else:
        for x in range(min(x1, x2) - 1, max(x1, x2) + 2):
            m.set(X(x), y2 + 1, Z(z1), frame)
        for y in range(y1, y2 + 2):
            m.set(X(min(x1, x2) - 1), y, Z(z1), frame)
            m.set(X(max(x1, x2) + 1), y, Z(z1), frame)


# ---------------------------------------------------------------- отделка

def camp_decor(m):
    m.set(X(0), Y, Z(8), 'minecraft:soul_campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
    for x in (-8, 8):
        for z in (2, 13):
            m.set(X(x), Y, Z(z), 'minecraft:end_rod', facing='up')
            m.set(X(x), Y + 3, Z(z), 'minecraft:amethyst_cluster', facing='down', waterlogged=False)
    for x in (-6, 6):
        m.set(X(x), Y, Z(12), 'minecraft:ender_chest', facing='south', waterlogged=False)
    m.sign(X(0), Y + 2, Z(15), ['ПОСЛЕДНИЙ ПРИВАЛ', 'Дальше — Бездна.', 'Удачи, герои.'], facing='north')


def bridge(m, st):
    y = YF - 1
    isl = {'A': (0, -19, 6), 'B': (-14, -36, 6), 'C': (10, -50, 6), 'D': (-4, -66, 5), 'E': (0, -79, 6)}
    for i, (cx, cz, r) in enumerate(isl.values()):
        island(m, cx, cz, r, y, seed=i)
    path(m, (0, -19), (-14, -36), y)
    crumb = path(m, (-14, -36), (10, -50), y, block='minecraft:cracked_stone_bricks')
    path(m, (10, -50), (-4, -66), y)
    path(m, (-4, -66), (0, -79), y)
    # осыпающийся мост B→C
    xs = [c[0] for c in crumb]
    zs = [c[1] for c in crumb]
    st.mech('s5_crumble', 'collapse', box=[X(min(xs) + 3), y, Z(min(zs) + 3), X(max(xs) - 3), y, Z(max(zs) - 3)],
            tile='minecraft:cracked_stone_bricks', near=[X(-20), YF - 2, Z(-56), X(16), YF + 6, Z(-30)])
    # тайный островок со ступеньками-камнями от острова C
    for sx, sz in ((16, -46), (18, -43), (20, -40), (22, -37), (24, -34)):
        m.set(X(sx), y, Z(sz), 'minecraft:end_stone_bricks')
        m.set(X(sx), y - 1, Z(sz), 'minecraft:obsidian')
    island(m, 26, -30, 3, y, top='minecraft:amethyst_block', seed=9)
    m.cache(X(26), YF, Z(-30), tier=2, facing='south')
    st.caches.append([X(26), YF, Z(-30), 2])
    st.mech('s5_secret1', 'area', box=[X(23), YF, Z(-33), X(29), YF + 3, Z(-27)], xp=True, solved='Тайный островок найден')
    # аметистовые кристаллы и стержни
    for cx, cz, r in isl.values():
        for a in range(0, 360, 90):
            x = round(cx + math.cos(math.radians(a + cz)) * (r - 1))
            z = round(cz + math.sin(math.radians(a + cz)) * (r - 1))
            m.set(X(x), YF, Z(z), 'minecraft:end_rod', facing='up')
    st.lore_at([X(-32), YF - 2, Z(-85), X(32), YF + 10, Z(-13)], 'Мост над Бездной',
               'Внизу — пустота. Упавшего Бездна вернёт на край — но ранит. Мост между вторым и третьим островом осыпается.')


def portals(m, st):
    hub = [X(0), YF, Z(-104), 180]
    names = {(-42, -99): ('Око', 'первым'), (-42, -110): ('Луна', 'вторым'), (42, -99): ('Ложный знак', ''), (42, -110): ('Череп', 'последним')}
    i = 0
    for (cx, cz), (sym, order) in names.items():
        side = -1 if cx < 0 else 1
        # разлом в зале (у стены)
        px = 10 * side
        pz = cz
        z1, z2 = pz - 1, pz + 1
        rift(m, px, YF, z1, px, YF + 3, z2)
        st.mech(f's5_portal{i}', 'portal', box=[X(px), YF, Z(z1), X(px), YF + 3, Z(z2)], to=[X(cx), YF, Z(cz), 90 if side < 0 else -90],
                **({'spawns': ['s5_false']} if not order else {}))
        # обратный разлом в комнате-видении
        bx = cx - 3 * side
        rift(m, bx, YF, cz - 1, bx, YF + 3, cz + 1, frame='minecraft:amethyst_block')
        st.mech(f's5_back{i}', 'portal', box=[X(bx), YF, Z(cz - 1), X(bx), YF + 3, Z(cz + 1)], to=hub)
        # фреска
        fx = cx + 4 * side
        if order:
            m.sign(X(fx), YF + 2, Z(cz), ['Видение:', sym, f'— {order}'], facing='west' if side > 0 else 'east')
        else:
            m.sign(X(fx), YF + 2, Z(cz), ['Видение лжёт.', 'Здесь нет знака.'], facing='west' if side > 0 else 'east')
        m.set(X(cx), YF, Z(cz), 'minecraft:amethyst_cluster', facing='up', waterlogged=False)
        i += 1
    # диски у северной стены
    pos = []
    for x in (-4, 0, 4):
        m.set(X(x), YF, Z(-114), 'minecraft:obsidian')
        m.set(X(x), YF + 1, Z(-114), 'litrpg:rune_dial', facing='south', symbol=0)
        pos.append([X(x), YF + 1, Z(-114)])
    m.sign(X(0), YF + 3, Z(-114), ['Знаки видений', 'по порядку.', 'Не верь лжецу.'], facing='south')
    st.gate('s5_dialgate', [X(-2), YF, Z(-115), X(2), YF + 4, Z(-115)], 'minecraft:obsidian')
    st.mech('s5_dials', 'dials', dials=pos, answer=[5, 2, 4], start=[0, 0, 0], opens=['s5_dialgate'], xp=True,
            solved='Знаки сошлись — Часовой зал открыт')
    st.pack('s5_false', (X(42), YF, Z(-99)), [('enderman', X(40), YF, Z(-99), 44, 'ELITE'), ('litrpg:banshee', X(44), YF, Z(-98), 44)], r=4, ambush=True)
    for x in (-8, 8):
        for z in (-96, -112):
            m.set(X(x), YF, Z(z), 'minecraft:end_rod', facing='up')
    st.lore_at([X(-12), YF - 1, Z(-114), X(12), YF + 8, Z(-94)], 'Зал Порталов',
               'Четыре разлома ведут в видения. Три хранят знаки, одно — лжёт. Выставьте знаки на дисках по порядку.')


def arena1(m, st):
    cz = -145
    # циферблат
    for k in range(12):
        a = math.pi * 2 * k / 12
        for rr in (16, 17):
            m.set(X(round(math.cos(a) * rr)), YF - 1, Z(cz + round(math.sin(a) * rr)), 'minecraft:chiseled_quartz_block')
    D.ring_floor(m, X(0), Z(cz), 18.5, 19.5, YF - 1, 'minecraft:quartz_bricks')
    for x in range(-1, 2):
        for z in range(-1, 2):
            m.set(X(x), YF - 1, Z(cz + z), 'minecraft:gold_block')
    # песочные часы — узлы силы
    for x, z in ((14, cz), (-14, cz), (0, cz - 14), (0, cz + 14)):
        m.set(X(x), YF, Z(z), 'minecraft:quartz_pillar', axis='y')
        m.set(X(x), YF + 1, Z(z), 'litrpg:relic', kind='time')
    st.gate('s5_a1_in', [X(-2), YF, Z(-123), X(2), YF + 4, Z(-123)], 'minecraft:iron_bars', open_=True)
    st.gate('s5_a1_out', [X(-2), YF, Z(-167), X(2), YF + 4, Z(-167)], 'minecraft:iron_bars')
    st.boss('chronophage', 'litrpg:chronophage', [X(0), YF, Z(cz), 0], [X(-21), YF - 1, Z(-166), X(21), YF + 22, Z(-124)],
            doors=['s5_a1_in'], unlock=['s5_a1_out'])
    st.lore_at([X(-21), YF - 1, Z(-166), X(21), YF + 12, Z(-124)], 'Часовой зал',
               'Хронофаг откатывает время. Разбейте песочные часы, пока он читает «Откат», — иначе он вернёт себе здоровье.')


def garden(m, st):
    # маятники в коридоре
    st.mech('s5_blades', 'blades', box=[X(-2), YF, Z(-172), X(2), YF + 2, Z(-170)], axis='x', period=46,
            near=[X(-2), YF, Z(-175), X(2), YF + 6, Z(-167)])
    # парящие камни
    for i, (x, z, yy) in enumerate(((-9, -184, 8), (8, -190, 11), (-6, -198, 6), (10, -200, 9), (-11, -194, 12), (2, -182, 13))):
        r = 2 + i % 2
        for dx in range(-r, r + 1):
            for dz in range(-r, r + 1):
                if dx * dx + dz * dz <= r * r:
                    m.set(X(x + dx), YF + yy, Z(z + dz), 'minecraft:end_stone')
                    if dx * dx + dz * dz <= (r - 1) ** 2:
                        m.set(X(x + dx), YF + yy - 1, Z(z + dz), 'minecraft:obsidian')
        m.set(X(x), YF + yy + 1, Z(z), 'minecraft:chorus_flower', age=5)
    for x in (-13, 12):
        for z in range(-204, -178, 6):
            m.set(X(x), YF, Z(z), 'minecraft:chorus_plant', down=True, up=True)
            m.set(X(x), YF + 1, Z(z), 'minecraft:chorus_flower', age=5)
    m.cache(X(13), YF, Z(-204), tier=1, facing='west')
    st.caches.append([X(13), YF, Z(-204), 1])
    st.lore_at([X(-15), YF - 1, Z(-206), X(15), YF + 10, Z(-176)], 'Сад парящих камней', 'Здесь камни забыли, что умеют падать.')


def arena2(m, st):
    # пасть: чёрный пол с трещинами
    for x in range(-17, 18):
        for z in range(-248, -215):
            if h3(x, 0, z, 61) < 0.08:
                m.set(X(x), YF - 1, Z(z), 'minecraft:crying_obsidian')
    for x in (-15, 14):
        for z in (-244, -232, -220):
            m.big_pillar(X(x), Z(z), YF, 14, 'minecraft:obsidian', trim='minecraft:crying_obsidian')
    # осколки миров: ледяной и огненный
    ice_spots, fire_spots = [], []
    for k in range(5):
        a = math.pi * 2 * k / 5
        ice_spots.append([X(-62 + round(math.cos(a) * 8)), YF, Z(-232 + round(math.sin(a) * 8))])
        fire_spots.append([X(62 + round(math.cos(a) * 8)), YF, Z(-232 + round(math.sin(a) * 8))])
    braz = []
    for k in range(3):
        a = math.pi * 2 * k / 3 + 0.5
        bx, bz = -62 + round(math.cos(a) * 9), -232 + round(math.sin(a) * 9)
        m.set(X(bx), YF, Z(bz), 'minecraft:campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
        braz.append([X(bx), YF, Z(bz)])
    for x in range(-74, -49):
        for z in range(-244, -219):
            if h3(x, 1, z, 62) < 0.06 and (X(x), YF, Z(z)) in m.open:
                m.set(X(x), YF, Z(z), 'minecraft:snow', layers=2)
    pits = []
    for k in range(6):
        a = math.pi * 2 * k / 6 + 0.3
        px, pz = 62 + round(math.cos(a) * 6), -232 + round(math.sin(a) * 6)
        for dx in range(-1, 1):
            for dz in range(-1, 1):
                m.set(X(px + dx), YF - 1, Z(pz + dz), 'minecraft:lava', level=0)
        pits.append([X(px), YF - 1, Z(pz)])
    st.gate('s5_a2_in', [X(-2), YF, Z(-214), X(2), YF + 4, Z(-214)], 'minecraft:iron_bars', open_=True)
    st.gate('s5_a2_out', [X(-2), YF, Z(-250), X(2), YF + 4, Z(-250)], 'minecraft:iron_bars')
    st.boss('world_eater', 'litrpg:world_eater', [X(0), YF, Z(-232), 0], [X(-18), YF - 1, Z(-249), X(18), YF + 20, Z(-215)],
            doors=['s5_a2_in'], unlock=['s5_a2_out'],
            extra={'ice': [X(-62), YF, Z(-232)], 'ice_spots': ice_spots, 'ice_box': [X(-76), YF - 2, Z(-246), X(-48), YF + 16, Z(-218)],
                   'fire': [X(62), YF, Z(-232)], 'fire_spots': fire_spots, 'fire_box': [X(48), YF - 2, Z(-246), X(76), YF + 16, Z(-218)],
                   'braziers': braz, 'home': [X(0), YF, Z(-240)]})
    st.lore_at([X(-18), YF - 1, Z(-249), X(18), YF + 10, Z(-215)], 'Пасть Пожирателя',
               'Пожиратель глотает целые миры — и вас вместе с ними. Во льду держитесь у костров, в огне — не стойте у лавы.')


def arena3(m, st):
    cz = -300
    # площадка над пустотой
    plat = []
    for x in range(-16, 17):
        for z in range(-16, 17):
            d = math.hypot(x, z)
            if d <= 16.3:
                ring = d > 12.3
                m.set(X(x), YT - 1, Z(cz + z), ('minecraft:end_stone_bricks' if ring else
                                                ('minecraft:obsidian' if (x + z) % 2 else 'minecraft:polished_blackstone')))
                for k in range(1, 4 if ring else 6):
                    m.set(X(x), YT - 1 - k, Z(cz + z), 'minecraft:obsidian')
    # вход: мостик от лестницы к площадке
    for z in range(-276, -285, -1):
        for x in range(-2, 3):
            m.set(X(x), YT - 1, Z(z), 'minecraft:purpur_block')
    for a in range(8):
        ang = math.pi * 2 * a / 8
        x, z = round(math.cos(ang) * 14), round(math.sin(ang) * 14)
        m.set(X(x), YT, Z(cz + z), 'minecraft:crying_obsidian')
        m.set(X(x), YT + 1, Z(cz + z), 'minecraft:end_rod', facing='up')
    # Сердце Бездны: островок, свет-маяки
    island(m, 0, -352, 12, YH - 1, top='minecraft:end_stone_bricks', body='minecraft:end_stone', depth=8, seed=7)
    heart_spots = []
    for k in range(5):
        a = math.pi * 2 * k / 5
        heart_spots.append([X(round(math.cos(a) * 7)), YH, Z(-352 + round(math.sin(a) * 7))])
    # выход: разлом на северном краю Сердца (заперт до победы)
    st.gate('s5_rift', [X(-2), YH, Z(-362), X(2), YH + 4, Z(-362)], 'minecraft:obsidian')
    for x in range(-1, 2):
        for y in range(YH, YH + 4):
            m.set(X(x), y, Z(-363), 'litrpg:rift')
    for x in (-2, 2):
        for y in range(YH, YH + 5):
            m.set(X(x), y, Z(-363), 'minecraft:crying_obsidian')
    for x in range(-2, 3):
        m.set(X(x), YH + 4, Z(-363), 'minecraft:crying_obsidian')
    st.exit = [X(-1), YH, Z(-364), X(1), YH + 3, Z(-363)]
    st.gate('s5_a3_in', [X(-2), YT, Z(-275), X(2), YT + 4, Z(-275)], 'minecraft:iron_bars', open_=True)
    st.boss('xalathar', 'litrpg:xalathar', [X(0), YT, Z(cz - 6), 0], [X(-24), YT - 8, Z(-324), X(24), YT + 30, Z(-276)],
            doors=['s5_a3_in'], unlock=['s5_rift'],
            extra={'ring': [X(-16), YT - 1, Z(cz - 16), X(16), YT - 1, Z(cz + 16)], 'center': [X(0), YT, Z(cz)],
                   'heart': [X(0), YH, Z(-352)], 'heart_spots': heart_spots,
                   'heart_box': [X(-18), YH - 6, Z(-370), X(18), YH + 26, Z(-334)]})
    st.lore_at([X(-24), YT - 2, Z(-324), X(24), YT + 14, Z(-276)], 'Вершина Шпиля',
               'Площадка над пустотой. Ксал\'Атар рушит её края и сталкивает вниз. Последний бой — в Сердце Бездны.')


def packs(st):
    S = st.pack
    S('s5_b1', (X(-14), YF, Z(-36)), [('enderman', X(-16), YF, Z(-36), 40), ('enderman', X(-12), YF, Z(-38), 40),
                                      ('litrpg:banshee', X(-14), YF, Z(-33), 41)], r=10)
    S('s5_b2', (X(10), YF, Z(-50)), [('litrpg:demon_stalker', X(8), YF, Z(-50), 41), ('litrpg:demon_stalker', X(12), YF, Z(-52), 41),
                                     ('litrpg:wyvern', X(10), YF + 4, Z(-48), 42, 'ELITE')], r=10)
    S('s5_b3', (X(-4), YF, Z(-66)), [('litrpg:shadow_panther', X(-6), YF, Z(-66), 42), ('litrpg:shadow_panther', X(-2), YF, Z(-66), 42),
                                     ('litrpg:banshee', X(-4), YF, Z(-69), 42, 'ELITE')], r=10)
    S('s5_h1', (X(0), YF, Z(-104)), [('enderman', X(-5), YF, Z(-104), 43), ('enderman', X(5), YF, Z(-104), 43),
                                     ('litrpg:necromancer', X(0), YF, Z(-110), 43, 'ELITE')], r=12)
    S('s5_g1', (X(0), YF, Z(-182)), [('litrpg:chimera', X(0), YF, Z(-184), 45, 'ELITE'), ('litrpg:shadow_panther', X(-5), YF, Z(-182), 45)], r=12)
    S('s5_guard', (X(0), YF, Z(-196)), [('litrpg:bone_colossus', X(0), YF, Z(-196), 46, 'CHAMPION', 'Страж Бездны', 'guard'),
                                        ('litrpg:banshee', X(-6), YF, Z(-198), 45), ('litrpg:banshee', X(6), YF, Z(-198), 45)], r=12, mini=True)
    S('s5_c1', (X(0), YF, Z(-262)), [('litrpg:demon_stalker', X(-1), YF, Z(-262), 48, 'ELITE'), ('enderman', X(1), YF + 2, Z(-264), 48)], r=10)
