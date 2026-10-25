"""
Этап 3 · Затопленный храм (ур. 20–30). Храм морского народа ушёл под воду; вода — главный враг и главный ключ.

  Привал в сухой часовне → Притвор (колоннада, каналы, утопленники)
  → Зал Вентилей (три вентиля поднимают воду — всплыть к верхней двери) → АРЕНА 1: Страж приливов
  → Галерея течений (потоки сносят в стороны) → три зала-фрески → Зал Дисков (вода, солнце, череп)
  → АРЕНА 2: Бассейн Левиафана (отлив и прилив) → Звонница (колокола приливов по стиху)
  → АРЕНА 3: Святилище Жрицы глубин (во второй фазе — стеклянный купол на дне моря) → Разлом.
"""
import math

from .build import Map, Theme, Stage, apply_gates, h3
from . import decor as D

OX, OZ = 3072, 0
Y = 64
YT = 60      # пол храма
YL = 52      # нижний ярус (бассейн, течения)


def X(x):
    return OX + x


def Z(z):
    return OZ + z


def build():
    m = Map(seed=3003)
    st = Stage(3, 'temple', 'Затопленный храм', 'Море помнит своих богов', (20, 30), music='temple')

    chapel = m.theme(Theme('s3_chapel', [('minecraft:prismarine_bricks', 5), ('minecraft:prismarine', 2)],
                           floor=[('minecraft:dark_prismarine', 3), ('minecraft:prismarine_bricks', 1)], ceil='minecraft:dark_prismarine', seed=31))
    temple = m.theme(Theme('s3_temple', [('minecraft:prismarine_bricks', 4), ('minecraft:prismarine', 3), ('minecraft:dark_prismarine', 1)],
                          floor=[('minecraft:prismarine_bricks', 3), ('minecraft:dark_prismarine', 2), ('minecraft:sea_lantern', 0.08)],
                          ceil=[('minecraft:dark_prismarine', 3), ('minecraft:prismarine_bricks', 1)], seed=32))
    deep = m.theme(Theme('s3_deep', [('minecraft:dark_prismarine', 3), ('minecraft:deepslate_tiles', 2), ('minecraft:tube_coral_block', 0.3),
                                     ('minecraft:brain_coral_block', 0.3)],
                         floor=[('minecraft:sand', 2), ('minecraft:gravel', 1), ('minecraft:dark_prismarine', 2)], seed=33))
    sanct = m.theme(Theme('s3_sanct', [('minecraft:prismarine_bricks', 5), ('minecraft:dark_prismarine', 1), ('minecraft:chiseled_quartz_block', 0.08)],
                          floor=[('minecraft:dark_prismarine', 3), ('minecraft:prismarine_bricks', 2), ('minecraft:smooth_quartz', 0.25)],
                          ceil='minecraft:dark_prismarine', seed=34))
    dome = m.theme(Theme('s3_dome', [('minecraft:light_blue_stained_glass', 4), ('minecraft:glass', 2)],
                         floor=[('minecraft:sand', 3), ('minecraft:prismarine_bricks', 1)], ceil='minecraft:light_blue_stained_glass', seed=35))

    # ======================= пустоты =======================
    m.room(X(-9), Z(0), X(9), Z(15), Y, 7, chapel, vault=3)
    m.corridor([(X(0), Z(-1), Y), (X(0), Z(-6), Y), (X(0), Z(-12), YT)], 5, 6, temple)
    # Притвор — колоннада с каналами
    m.room(X(-12), Z(-60), X(12), Z(-13), YT, 11, temple, vault=5)
    # Зал Вентилей — высокая шахта, дверь наверху
    m.carve(X(-2), YT, Z(-66), X(2), YT + 5, Z(-61), temple)
    m.room(X(-10), Z(-86), X(10), Z(-67), YT - 4, 20, temple)
    m.carve(X(-2), YT + 8, Z(-94), X(2), YT + 13, Z(-87), temple)
    # АРЕНА 1 — Страж приливов (на верхнем ярусе)
    m.room(X(-18), Z(-128), X(18), Z(-95), YT + 8, 14, temple, vault=5)
    # Галерея течений — вниз к нижнему ярусу
    m.corridor([(X(0), Z(-129), YT + 8), (X(0), Z(-134), YT + 8), (X(0), Z(-152), YL)], 5, 6, deep)
    m.room(X(-8), Z(-196), X(8), Z(-153), YL, 7, deep)
    # три зала-фрески по бокам галереи
    for i, z in enumerate((-162, -176, -190)):
        side = -1 if i % 2 == 0 else 1
        m.corridor([(X(8 * side), Z(z)), (X(16 * side), Z(z))], 3, 4, deep, y=YL)
        m.room(X(min(16 * side, 26 * side)), Z(z - 5), X(max(16 * side, 26 * side)), Z(z + 5), YL, 6, sanct)
    # Зал Дисков
    m.carve(X(-2), YL, Z(-204), X(2), YL + 5, Z(-197), deep)
    m.room(X(-9), Z(-218), X(9), Z(-205), YL, 8, sanct, vault=3)
    # АРЕНА 2 — Бассейн Левиафана: огромная пещера с бассейном и кольцевой дорожкой
    m.carve(X(-2), YL, Z(-226), X(2), YL + 5, Z(-219), deep)
    m.cave(X(0), YL + 9, Z(-252), 25, 14, 25, deep, rough=0.15, seed=36, floor=YL)
    m.carve(X(-15), YL - 8, Z(-267), X(15), YL - 1, Z(-237), deep)     # чаша бассейна (8 глубиной)
    # Звонница
    m.carve(X(-2), YL, Z(-284), X(2), YL + 5, Z(-277), sanct)
    m.room(X(-10), Z(-300), X(10), Z(-285), YL, 10, sanct, vault=4)
    # подъём к Святилищу
    m.corridor([(X(0), Z(-301), YL), (X(0), Z(-305), YL), (X(0), Z(-318), YT)], 5, 6, sanct)
    # АРЕНА 3 — Святилище Жрицы
    m.room(X(-20), Z(-360), X(20), Z(-319), YT, 16, sanct, vault=8)
    # Купол на дне — под святилищем (вода вокруг стекла)
    m.round_room(X(0), Z(-340), 13, YT - 34, 9, dome, dome=6)
    # Разлом
    m.carve(X(-4), YT, Z(-370), X(4), YT + 8, Z(-361), sanct)

    # ======================= отделка =======================
    camp_decor(m)
    narthex(m, st)
    valves(m, st)
    arena1(m, st)
    currents(m, st)
    dials(m, st)
    arena2(m, st)
    bells(m, st)
    arena3(m, st)
    packs(st)

    st.box = [X(-40), 10, Z(-376), X(40), 110, Z(22)]
    st.camp = [X(0), Y, Z(9), 180]
    st.trader = [X(-5), Y, Z(3), 135]
    st.exit = [X(-3), YT, Z(-369), X(3), YT + 6, Z(-365)]
    m.auto_lights(spacing=8, soul=False, themes={'s3_chapel', 's3_temple', 's3_sanct'}, min_h=6)
    m.glow(spacing=5, level=10)
    apply_gates(m, st)
    m.shell(thick=2)
    sea_around_dome(m)
    return m, st


# ---------------------------------------------------------------- отделка

def water_box(m, x1, y1, z1, x2, y2, z2):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                if m.is_open(x, y, z):
                    m.set(x, y, z, 'minecraft:water', level=0)


def camp_decor(m):
    m.set(X(0), Y, Z(8), 'minecraft:campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
    for dx in (-2, 2):
        for dz in range(6, 11):
            m.set(X(dx * 2), Y, Z(dz), 'minecraft:stripped_warped_stem', axis='z')
    for dz in range(1, 6):
        m.set(X(-7), Y, Z(dz), 'minecraft:prismarine_bricks')
        m.slab(X(-7), Y + 1, Z(dz), 'minecraft:prismarine_brick_slab')
    for x in (-8, 8):
        for z in (2, 13):
            m.set(X(x), Y, Z(z), 'minecraft:sea_lantern')
    m.sign(X(0), Y + 2, Z(15), ['ПРИВАЛ', 'Сухая часовня.', 'Дальше — вода.'], facing='north')


def narthex(m, st):
    # колоннада и каналы с водой вдоль стен
    for z in range(-58, -14, 6):
        for x in (-8, 7):
            m.big_pillar(X(x), Z(z), YT, 11, 'minecraft:prismarine_bricks', trim='minecraft:dark_prismarine')
    for z in range(-60, -12):
        for x in (-11, -10, 10, 11):
            m.set(X(x), YT - 1, Z(z), 'minecraft:water', level=0)
    for z in range(-56, -16, 8):
        m.set(X(0), YT - 1, Z(z), 'minecraft:sea_lantern')
        m.set(X(-3), YT, Z(z), 'minecraft:tube_coral_fan', waterlogged=False)
        m.set(X(3), YT, Z(z), 'minecraft:brain_coral_fan', waterlogged=False)
    st.lore_at([X(-12), YT - 1, Z(-60), X(12), YT + 8, Z(-13)], 'Притвор', 'Море отступило — но обещало вернуться.')


def valves(m, st):
    # шахта: пол на YT-4, вода поднимается слоями по 3; дверь на высоте YT+8 (4 уровня)
    y0 = YT - 4
    vs = []
    for i, x in enumerate((-8, 0, 8)):
        m.set(X(x), y0, Z(-86), 'minecraft:dark_prismarine')
        m.set(X(x), y0 + 1, Z(-86), 'minecraft:lever', face='floor', facing='north', powered=False)
        vs.append([X(x), y0 + 1, Z(-86)])
        m.sign(X(x), y0 + 2, Z(-86), [['Прилив', 'Волна', 'Отлив'][i], ['+2', '+1', '−1'][i]], facing='south')
    m.sign(X(0), y0 + 3, Z(-67), ['Дверь — высоко.', 'Поднимите воду', 'ровно на 4 меры', 'и всплывайте'], facing='south')
    # уступ у верхней двери
    for x in range(-2, 3):
        for z in range(-87, -84):
            m.set(X(x), YT + 7, Z(z), 'minecraft:prismarine_bricks')
    st.mech('s3_valves', 'valves', valves=vs, delta=[2, 1, -1], levels=4, step=3, target=4, startLevel=0,
            water=[X(-10), y0, Z(-86), X(10), y0 + 11, Z(-67)], xp=True, solved='Вода поднялась — к верхней двери!',
            near=[X(-10), y0, Z(-86), X(10), y0 + 20, Z(-67)])
    st.lore_at([X(-10), y0, Z(-86), X(10), y0 + 18, Z(-67)], 'Зал Вентилей', 'Три вентиля: прилив, волна и отлив. Нужна ровно четвёртая мера.')


def arena1(m, st):
    ya = YT + 8
    # четыре приподнятых помоста по углам (спасение от «Затопления»)
    plats = []
    for x, z in ((-13, -123), (13, -123), (-13, -100), (13, -100)):
        for dx in range(-3, 4):
            for dz in range(-3, 4):
                m.set(X(x + dx), ya, Z(z + dz), 'minecraft:dark_prismarine')
                m.set(X(x + dx), ya + 1, Z(z + dz), 'minecraft:prismarine_bricks')
        m.set(X(x), ya + 2, Z(z), 'minecraft:sea_lantern')
        plats.append([X(x), ya + 2, Z(z)])
    for x in (-6, 5):
        for z in (-118, -105):
            m.big_pillar(X(x), Z(z), ya, 14, 'minecraft:prismarine_bricks', trim='minecraft:dark_prismarine')
    st.gate('s3_a1_in', [X(-2), ya, Z(-94), X(2), ya + 4, Z(-94)], 'minecraft:iron_bars', open_=True)
    st.gate('s3_a1_out', [X(-2), ya, Z(-129), X(2), ya + 4, Z(-129)], 'minecraft:iron_bars')
    st.boss('tide_warden', 'litrpg:tide_warden', [X(0), ya, Z(-115), 0], [X(-18), ya - 1, Z(-128), X(18), ya + 18, Z(-95)],
            doors=['s3_a1_in'], unlock=['s3_a1_out'],
            extra={'platforms': plats, 'flood': [X(-17), ya, Z(-127), X(17), ya + 1, Z(-96)]})
    st.lore_at([X(-18), ya - 1, Z(-128), X(18), ya + 10, Z(-95)], 'Страж приливов',
               'Волны идут через весь зал — укрывайтесь за колоннами. Когда вода поднимется — на помосты!')


def currents(m, st):
    # три потока поперёк галереи — сносят в стороны (держитесь края)
    for i, z in enumerate((-165, -178, -191)):
        d = 'east' if i % 2 == 0 else 'west'
        for x in range(-8, 9):
            for dz in range(-1, 2):
                m.set(X(x), YL - 1, Z(z + dz), 'minecraft:dark_prismarine')
        st.mech(f's3_current{i}', 'current', box=[X(-8), YL, Z(z - 1), X(8), YL + 3, Z(z + 1)], dir=d, force=0.12,
                near=[X(-8), YL, Z(z - 1), X(8), YL + 3, Z(z + 1)])
    # фрески-подсказки в боковых залах
    symbols = [('Вода', 'Волна идёт первой'), ('Солнце', 'Солнце — вторым'), ('Череп', 'Смерть — последней')]
    for i, z in enumerate((-162, -176, -190)):
        side = -1 if i % 2 == 0 else 1
        x = 25 * side
        m.sign(X(x), YL + 2, Z(z), ['Фреска:', symbols[i][0], symbols[i][1]], facing='east' if side < 0 else 'west')
        m.cache(X(21 * side), YL, Z(z + 4), tier=0, facing='south')
        st.caches.append([X(21 * side), YL, Z(z + 4), 0])
    # кораллы и водоросли
    for z in range(-195, -154, 3):
        for x in (-7, 7):
            if h3(x, 0, z, 5) < 0.6:
                m.set(X(x), YL, Z(z), ['minecraft:tube_coral', 'minecraft:fire_coral', 'minecraft:horn_coral'][abs(z) % 3], waterlogged=False)
    st.lore_at([X(-8), YL - 1, Z(-196), X(8), YL + 6, Z(-153)], 'Галерея течений',
               'Течения сносят в сторону. Три фрески по бокам хранят знаки для Зала Дисков.')


def dials(m, st):
    pos = []
    for x in (-4, 0, 4):
        m.set(X(x), YL, Z(-218), 'minecraft:dark_prismarine')
        m.set(X(x), YL + 1, Z(-218), 'litrpg:rune_dial', facing='south', symbol=0)
        pos.append([X(x), YL + 1, Z(-218)])
    m.sign(X(0), YL + 3, Z(-218), ['Волна, свет', 'и смерть —', 'как на фресках'], facing='south')
    st.gate('s3_dialgate', [X(-2), YL, Z(-219), X(2), YL + 4, Z(-219)], 'minecraft:dark_prismarine')
    st.mech('s3_dials', 'dials', dials=pos, answer=[1, 3, 4], start=[0, 0, 0], opens=['s3_dialgate'], xp=True,
            solved='Знаки моря сошлись — путь к Бассейну открыт')


def arena2(m, st):
    # бассейн с водой; кольцевая дорожка; четыре точки всплытия
    water_box(m, X(-15), YL - 8, Z(-267), X(15), YL - 1, Z(-237))
    emerge = []
    for a in range(4):
        ang = math.pi / 2 * a
        emerge.append([X(round(math.cos(ang) * 12)), YL - 2, Z(-252 + round(math.sin(ang) * 12))])
    for x in range(-15, 16):
        for z in (-268, -236):
            m.set(X(x), YL - 1, Z(z), 'minecraft:prismarine_bricks')
    st.gate('s3_a2_in', [X(-2), YL, Z(-226), X(2), YL + 4, Z(-226)], 'minecraft:iron_bars', open_=True)
    st.gate('s3_a2_out', [X(-2), YL, Z(-277), X(2), YL + 4, Z(-277)], 'minecraft:iron_bars')
    st.boss('leviathan', 'litrpg:leviathan', [X(0), YL - 6, Z(-252), 0], [X(-25), YL - 9, Z(-277), X(25), YL + 22, Z(-227)],
            doors=['s3_a2_in'], unlock=['s3_a2_out'],
            extra={'pool': [X(-15), YL - 8, Z(-267), X(15), YL - 1, Z(-237)], 'emerge': emerge, 'center': [X(0), YL - 8, Z(-252)]})
    st.lore_at([X(-25), YL - 1, Z(-277), X(25), YL + 10, Z(-227)], 'Бассейн Левиафана',
               'Левиафан ныряет и всплывает у края. Когда вода уйдёт — он беспомощен на дне: бейте!')


def bells(m, st):
    bl = []
    names = ['Ракушка', 'Риф', 'Шторм', 'Штиль']
    for i, x in enumerate((-7, -3, 1, 5)):
        m.set(X(x + 1), YL, Z(-299), 'minecraft:dark_prismarine')
        m.set(X(x + 1), YL + 1, Z(-299), 'minecraft:bell', attachment='floor', facing='south', powered=False)
        bl.append([X(x + 1), YL + 1, Z(-299)])
        m.sign(X(x + 1), YL + 1, Z(-298), [names[i]], facing='south', wall=False)
    m.sign(X(-9), YL + 2, Z(-285), ['«Сперва Штиль,', 'потом Риф,', 'Ракушка — за ним,', 'Шторм — в конце»'], facing='south')
    st.gate('s3_bellgate', [X(-2), YL, Z(-301), X(2), YL + 4, Z(-301)], 'minecraft:dark_prismarine')
    st.mech('s3_bells', 'bells', bells=bl, order=[3, 1, 0, 2], opens=['s3_bellgate'], xp=True, solved='Колокола пропели прилив — путь открыт')


def arena3(m, st):
    # святилище: алтарь, статуи морских богов, бассейны
    for z in range(-356, -322, 7):
        for x in (-16, 15):
            m.big_pillar(X(x), Z(z), YT, 16, 'minecraft:prismarine_bricks', trim='minecraft:chiseled_quartz_block')
    for x in range(-4, 5):
        for z in range(-358, -352):
            m.set(X(x), YT, Z(z), 'minecraft:dark_prismarine')
    m.set(X(0), YT + 1, Z(-355), 'minecraft:conduit', waterlogged=True)
    for x in (-10, 10):
        for z in range(-350, -330):
            m.set(X(x), YT - 1, Z(z), 'minecraft:water', level=0)
    # купол: точки приземления
    dy = YT - 34
    spots = []
    for i in range(5):
        a = math.pi * 2 * i / 5
        spots.append([X(round(math.cos(a) * 7)), dy, Z(-340 + round(math.sin(a) * 7))])
    st.gate('s3_a3_in', [X(-2), YT, Z(-319), X(2), YT + 4, Z(-319)], 'minecraft:iron_bars', open_=True)
    st.gate('s3_rift', [X(-4), YT, Z(-361), X(4), YT + 7, Z(-361)], 'minecraft:dark_prismarine')
    for x in range(-3, 4):
        for y in range(YT, YT + 7):
            m.set(X(x), y, Z(-369), 'litrpg:rift')
    st.boss('deep_priestess', 'litrpg:deep_priestess', [X(0), YT, Z(-350), 0], [X(-20), YT - 1, Z(-360), X(20), YT + 24, Z(-319)],
            doors=['s3_a3_in'], unlock=['s3_rift'],
            extra={'dome': [X(0), dy, Z(-340)], 'dome_spots': spots, 'dome_box': [X(-14), dy - 1, Z(-354), X(14), dy + 16, Z(-326)],
                   'altar': [X(0), YT + 1, Z(-352)]})
    st.lore_at([X(-20), YT - 1, Z(-360), X(20), YT + 12, Z(-319)], 'Святилище Жрицы глубин',
               'Жрица утянет вас на дно — под стеклянный купол. Трещины в стекле пускают воду: не стойте под ними.')


def sea_around_dome(m):
    """Вода вокруг стеклянного купола (видно море за стеклом), снаружи — каменная скорлупа."""
    cx, cz, cy = X(0), Z(-340), YT - 34
    for x in range(cx - 21, cx + 22):
        for z in range(cz - 21, cz + 22):
            for y in range(cy - 6, cy + 22):
                if (x, y, z) in m.open or (x, y, z) in m.blocks:
                    continue
                d = math.sqrt((x - cx) ** 2 + (z - cz) ** 2 + max(0, y - cy) ** 2)
                if y < cy - 4:
                    if d < 21:
                        m.set(x, y, z, 'minecraft:dark_prismarine')
                elif d < 18:
                    m.set(x, y, z, 'minecraft:sand' if y == cy - 4 else 'minecraft:water', level=0) if y != cy - 4 else m.set(x, y, z, 'minecraft:sand')
                elif d < 20.5:
                    m.set(x, y, z, 'minecraft:dark_prismarine')
    for x in range(cx - 17, cx + 18, 3):
        for z in range(cz - 17, cz + 18, 3):
            if m.get(x, cy - 3, z) == 'minecraft:water' and h3(x, 0, z, 9) < 0.5:
                m.set(x, cy - 3, z, ['minecraft:tube_coral_block', 'minecraft:brain_coral_block', 'minecraft:sea_lantern'][abs(x + z) % 3])
                if m.get(x, cy - 2, z) == 'minecraft:water':
                    m.set(x, cy - 2, z, 'minecraft:kelp')


def packs(st):
    S = st.pack
    S('s3_n1', (X(0), YT, Z(-24)), [('drowned', X(-4), YT, Z(-24), 20), ('drowned', X(4), YT, Z(-24), 20), ('drowned', X(0), YT, Z(-28), 21)], r=16)
    S('s3_n2', (X(0), YT, Z(-40)), [('guardian', X(-9), YT, Z(-40), 21), ('guardian', X(9), YT, Z(-40), 21),
                                    ('drowned', X(0), YT, Z(-40), 21, 'ELITE')], r=16)
    S('s3_n3', (X(0), YT, Z(-54)), [('litrpg:bog_lurker', X(-3), YT, Z(-54), 22), ('litrpg:bog_lurker', X(3), YT, Z(-54), 22),
                                    ('litrpg:giant_toad', X(0), YT, Z(-57), 22, 'ELITE')], r=14)
    S('s3_v1', (X(0), YT - 4, Z(-76)), [('guardian', X(-6), YT - 4, Z(-76), 22), ('guardian', X(6), YT - 4, Z(-76), 22),
                                        ('drowned', X(0), YT - 4, Z(-80), 22)], r=14)
    S('s3_g1', (X(0), YL, Z(-158)), [('drowned', X(-3), YL, Z(-158), 23), ('drowned', X(3), YL, Z(-158), 23), ('litrpg:jelly', X(0), YL, Z(-161), 23)], r=12)
    S('s3_g2', (X(0), YL, Z(-172)), [('litrpg:mire_horror', X(0), YL, Z(-172), 24, 'ELITE'), ('drowned', X(-4), YL, Z(-174), 23)], r=12)
    S('s3_g3', (X(0), YL, Z(-186)), [('guardian', X(-5), YL, Z(-186), 24), ('guardian', X(5), YL, Z(-186), 24), ('drowned', X(0), YL, Z(-190), 24, 'ELITE')], r=12)
    S('s3_basilisk', (X(-21), YL, Z(-176)), [('litrpg:basilisk', X(-21), YL, Z(-176), 25, 'CHAMPION', 'Око Глубин', 'guard')], r=8, mini=True)
    S('s3_d1', (X(0), YL, Z(-210)), [('litrpg:mire_horror', X(-4), YL, Z(-210), 25), ('drowned', X(4), YL, Z(-210), 25),
                                     ('drowned', X(0), YL, Z(-214), 25)], r=10)
    S('s3_hydra', (X(0), YL, Z(-292)), [('litrpg:hydra', X(0), YL, Z(-292), 27, 'CHAMPION', 'Гидра Звонницы', 'brute'),
                                        ('drowned', X(-6), YL, Z(-290), 26), ('drowned', X(6), YL, Z(-290), 26)], r=10, mini=True)
    S('s3_s1', (X(0), YT, Z(-316)), [('litrpg:mire_horror', X(-1), YT, Z(-316), 28, 'ELITE'), ('guardian', X(2), YT, Z(-314), 27)], r=10)
