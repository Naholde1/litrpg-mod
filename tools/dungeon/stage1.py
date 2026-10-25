"""
Этап 1 · Склеп Забытых (ур. 1–10). Путь идёт на север (к −z) и вниз.

  Привал (костёр, торговец) → Зал Плача (статуи, саркофаги; боковая капелла — Плакальщица и первая фреска)
  → Коридор Ловушек (шипы, стрелы; треснувшая стена — тайник) → Оссуарий с тремя плитами (нужно трое)
  → АРЕНА 1: Привратник Костей → развилка:
      запад — Катакомбы (узкие ходы, ниши черепов; вторая фреска) → Зал Огней (загадка: лампы и рычаги)
               → Яма Костоправа (мини-босс) — загадка открывает решётку моста;
      север — Мост над Бездной (пол осыпается, лучники на уступах)
  → Дверь Загадки (кнопки-ответы; неверно — засада) → АРЕНА 2: Морра и Весса (река душ; третья фреска)
  → Зал Дисков (три рунных диска — символы с фресок) → Тропа Королей (пол памяти над пропастью)
  → АРЕНА 3: Тронный зал Короля-лича (во второй фазе — Фиал под залом) → Разлом.
"""
import math

from .build import Map, Theme, Stage, apply_gates
from . import decor as D

OX, OZ = 1024, 0
Y = 64          # пол привала
YH = 60         # Зал Плача и дальше
YC = 56         # катакомбы и мост
YT = 52         # тронный зал


def X(x):
    return OX + x


def Z(z):
    return OZ + z


def build():
    m = Map(seed=1001)
    st = Stage(1, 'crypt', 'Склеп Забытых', 'Мёртвые здесь не спят', (1, 10), music='crypt')

    camp = m.theme(Theme('s1_camp', [('minecraft:stone_bricks', 5), ('minecraft:mossy_stone_bricks', 2), ('minecraft:cracked_stone_bricks', 1)],
                         floor=[('minecraft:spruce_planks', 4), ('minecraft:stripped_spruce_wood', 1)], ceil='minecraft:dark_oak_planks', seed=11))
    hall = m.theme(Theme('s1_hall', [('minecraft:deepslate_bricks', 5), ('minecraft:cracked_deepslate_bricks', 2), ('minecraft:deepslate_tiles', 1),
                                     ('minecraft:chiseled_deepslate', 0.3)],
                         floor=[('minecraft:polished_deepslate', 4), ('minecraft:deepslate_tiles', 2), ('minecraft:cracked_deepslate_tiles', 1)],
                         ceil=[('minecraft:deepslate_bricks', 3), ('minecraft:deepslate_tiles', 1)], seed=12))
    tomb = m.theme(Theme('s1_tomb', [('minecraft:stone_bricks', 4), ('minecraft:mossy_cobblestone', 2), ('minecraft:cobblestone', 2),
                                     ('minecraft:mossy_stone_bricks', 2), ('minecraft:bone_block', 0.4)],
                         floor=[('minecraft:cobblestone', 3), ('minecraft:gravel', 1), ('minecraft:coarse_dirt', 1), ('minecraft:mossy_cobblestone', 1)],
                         ceil=[('minecraft:stone_bricks', 3), ('minecraft:cobblestone', 1)], seed=13))
    trap = m.theme(Theme('s1_trap', [('minecraft:stone_bricks', 3), ('minecraft:cracked_stone_bricks', 2), ('minecraft:andesite', 1)],
                         floor=[('minecraft:polished_andesite', 3), ('minecraft:stone_bricks', 1)], seed=14))
    abyss = m.theme(Theme('s1_abyss', [('minecraft:blackstone', 4), ('minecraft:polished_blackstone_bricks', 2),
                                       ('minecraft:cracked_polished_blackstone_bricks', 1), ('minecraft:basalt', 1)],
                          floor=[('minecraft:polished_blackstone_bricks', 3), ('minecraft:blackstone', 1)], seed=15))
    soul = m.theme(Theme('s1_soul', [('minecraft:polished_blackstone_bricks', 4), ('minecraft:cracked_polished_blackstone_bricks', 1),
                                     ('minecraft:deepslate_bricks', 2)],
                         floor=[('minecraft:polished_blackstone', 3), ('minecraft:soul_soil', 1)], ceil='minecraft:blackstone', seed=16))
    throne = m.theme(Theme('s1_throne', [('minecraft:polished_blackstone_bricks', 5), ('minecraft:chiseled_polished_blackstone', 1),
                                         ('minecraft:gilded_blackstone', 0.3)],
                           floor=[('minecraft:polished_blackstone', 3), ('minecraft:obsidian', 1)], ceil='minecraft:blackstone', seed=17))
    vault = m.theme(Theme('s1_vault', [('minecraft:crying_obsidian', 1), ('minecraft:obsidian', 3), ('minecraft:blackstone', 2)],
                          floor='minecraft:polished_blackstone_bricks', seed=18))

    # ======================= вырезаем пустоты =======================
    # Привал
    m.room(X(-10), Z(0), X(10), Z(16), Y, 7, camp, vault=3)
    # спуск к Залу Плача
    m.corridor([(X(0), Z(-1), Y), (X(0), Z(-6), Y), (X(0), Z(-14), YH)], 5, 6, hall)
    # Зал Плача
    m.room(X(-17), Z(-60), X(17), Z(-15), YH, 12, hall, vault=7)
    # боковая капелла (восток)
    m.corridor([(X(17), Z(-38)), (X(26), Z(-38))], 4, 5, hall, y=YH)
    m.room(X(26), Z(-48), X(40), Z(-28), YH, 9, hall, vault=4)
    # Коридор Ловушек
    m.carve(X(-2), YH, Z(-90), X(2), YH + 5, Z(-61), trap)
    # тайник за треснувшей стеной (запад)
    m.room(X(-14), Z(-80), X(-6), Z(-70), YH, 5, tomb)
    # Оссуарий с плитами
    m.room(X(-10), Z(-108), X(10), Z(-91), YH, 8, tomb, vault=3)
    # АРЕНА 1 — круглый зал Привратника
    m.round_room(X(0), Z(-127), 16, YH, 12, hall, dome=6)
    # выход из арены и развилка
    m.carve(X(-2), YH, Z(-150), X(2), YH + 5, Z(-143), hall)
    m.room(X(-7), Z(-163), X(7), Z(-150), YH, 9, hall, vault=3)
    # Катакомбы (запад): лабиринт узких ходов на уровне YC
    m.corridor([(X(-7), Z(-157), YH), (X(-14), Z(-157), YH), (X(-22), Z(-157), YC)], 3, 4, tomb)
    cat = [
        [(X(-22), Z(-157)), (X(-50), Z(-157))],
        [(X(-30), Z(-157)), (X(-30), Z(-185))],
        [(X(-42), Z(-157)), (X(-42), Z(-135))],
        [(X(-42), Z(-135)), (X(-62), Z(-135))],
        [(X(-50), Z(-157)), (X(-50), Z(-175)), (X(-62), Z(-175))],
        [(X(-30), Z(-185)), (X(-48), Z(-185))],
        [(X(-22), Z(-157)), (X(-22), Z(-135)), (X(-34), Z(-135))],
    ]
    for c in cat:
        m.corridor(c, 3, 4, tomb, y=YC)
    # Зал Огней (загадка лампы-рычаги) — запад
    m.room(X(-76), Z(-145), X(-62), Z(-125), YC, 8, tomb, vault=3)
    # Яма Костоправа — юго-запад
    m.room(X(-74), Z(-186), X(-60), Z(-166), YC - 2, 9, tomb, vault=3)
    m.carve(X(-62), YC - 2, Z(-176), X(-61), YC + 2, Z(-174), tomb)
    # Мост над Бездной: пещера-пропасть к северу от развилки
    m.carve(X(-2), YH, Z(-170), X(2), YH + 5, Z(-164), abyss)
    m.cave(X(0), YH + 4, Z(-195), 20, 13, 24, abyss, rough=0.35, seed=31, floor=YH - 1)
    m.pit(X(-18), Z(-216), X(18), Z(-174), YH - 1, 40, abyss)
    # уступы лучников по бокам пропасти
    m.carve(X(-17), YH + 5, Z(-205), X(-12), YH + 9, Z(-185), abyss)
    m.carve(X(12), YH + 5, Z(-205), X(17), YH + 9, Z(-185), abyss)
    # Дверь Загадки
    m.carve(X(-2), YH, Z(-226), X(2), YH + 5, Z(-218), abyss)
    m.room(X(-8), Z(-240), X(8), Z(-227), YH, 7, soul)
    # АРЕНА 2 — Морра и Весса: зал с рекой душ
    m.carve(X(-2), YH, Z(-246), X(2), YH + 5, Z(-241), soul)
    m.room(X(-18), Z(-280), X(18), Z(-247), YH, 13, soul, vault=5)
    # Зал Дисков
    m.carve(X(-2), YH, Z(-290), X(2), YH + 5, Z(-281), soul)
    m.room(X(-9), Z(-305), X(9), Z(-291), YH, 9, throne, vault=3)
    # спуск к Тропе Королей
    m.corridor([(X(0), Z(-306), YH), (X(0), Z(-310), YH), (X(0), Z(-318), YT)], 5, 6, throne)
    # Тропа Королей — пол над пропастью
    m.room(X(-8), Z(-338), X(8), Z(-319), YT, 8, throne, vault=3)
    m.pit(X(-5), Z(-334), X(5), Z(-322), YT, 30, throne)
    # АРЕНА 3 — тронный зал
    m.carve(X(-3), YT, Z(-346), X(3), YT + 7, Z(-339), throne)
    m.room(X(-20), Z(-390), X(20), Z(-347), YT, 18, throne, vault=8)
    # Фиал (вторая фаза) — под тронным залом, отдельный зал над бездной
    m.round_room(X(0), Z(-368), 14, YT - 28, 14, vault, dome=6)
    m.pit(X(-14), Z(-382), X(14), Z(-354), YT - 28, 30, vault)
    # Разлом за троном
    m.carve(X(-4), YT, Z(-400), X(4), YT + 8, Z(-391), throne)

    # ======================= отделка =======================
    camp_decor(m)
    hall_decor(m)
    traps_decor(m, st)
    ossuary_decor(m, st)
    arena1(m, st)
    catacombs(m, st)
    bridge(m, st)
    riddle(m, st)
    arena2(m, st)
    dials(m, st)
    memory(m, st)
    arena3(m, st)
    packs(st)
    m.auto_lights(spacing=8, soul=True, themes={'s1_hall', 's1_tomb', 's1_trap', 's1_soul', 's1_throne'}, min_h=5)
    m.auto_lights(spacing=6, soul=False, themes={'s1_camp'}, min_h=5, offset=3)
    m.glow(spacing=5, level=10)

    st.box = [X(-90), 10, Z(-410), X(50), 110, Z(24)]
    st.camp = [X(0), Y, Z(10), 180]
    st.trader = [X(-6), Y, Z(3), 135]
    st.exit = [X(-3), YT, Z(-399), X(3), YT + 6, Z(-395)]
    apply_gates(m, st)
    m.shell(thick=2)
    return m, st


# ---------------------------------------------------------------- привал

def camp_decor(m):
    # костёр в круге камней
    m.set(X(0), Y, Z(8), 'minecraft:campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
        m.set(X(dx), Y - 1, Z(8 + dz), 'minecraft:cobblestone')
    # брёвна-скамьи
    for dz in (5, 11):
        for dx in (-2, -1, 0, 1, 2):
            m.set(X(dx), Y, Z(dz), 'minecraft:stripped_spruce_log', axis='x')
    for dx in (-4, 4):
        for dz in (7, 8, 9):
            m.set(X(dx), Y, Z(dz), 'minecraft:stripped_spruce_log', axis='z')
    # прилавок торговца
    for dz in range(1, 6):
        m.set(X(-8), Y, Z(dz), 'minecraft:spruce_planks')
        m.slab(X(-8), Y + 1, Z(dz), 'minecraft:spruce_slab')
    m.set(X(-9), Y, Z(1), 'minecraft:barrel', facing='up', open=False)
    m.set(X(-9), Y, Z(2), 'minecraft:barrel', facing='up', open=False)
    m.set(X(-9), Y + 1, Z(1), 'minecraft:barrel', facing='up', open=False)
    m.lantern(X(-8), Y + 2, Z(3))
    # спальные места
    for i, dx in enumerate((5, 7, 9)):
        m.set(X(dx), Y, Z(14), 'minecraft:red_wool' if i != 1 else 'minecraft:brown_wool')
        m.set(X(dx), Y, Z(13), 'minecraft:white_carpet')
    # стены: щиты и знамёна павших отрядов
    for dx in (-6, 0, 6):
        m.banner(X(dx), Y + 4, Z(16), 'north', color='gray')
    for dz in (3, 9, 13):
        m.lantern(X(9), Y + 3, Z(dz), hanging=False) if False else m.torch_wall(X(10), Y + 3, Z(dz), 'west')
        m.torch_wall(X(-10), Y + 3, Z(dz), 'east')
    m.sign(X(0), Y + 3, Z(16), ['ПРИВАЛ', 'Отдых. Торговец.', 'Здесь вы', 'начнёте снова'], facing='north')
    # врата к Залу Плача
    D.arch(m, X(-3), Y, Z(-1), 'x', 5, 5, 'minecraft:chiseled_stone_bricks')


# ---------------------------------------------------------------- Зал Плача

def hall_decor(m):
    # два ряда массивных колонн
    for z in range(-55, -18, 8):
        m.big_pillar(X(-11), Z(z), YH, 12, 'minecraft:deepslate_tiles', trim='minecraft:chiseled_deepslate')
        m.big_pillar(X(10), Z(z), YH, 12, 'minecraft:deepslate_tiles', trim='minecraft:chiseled_deepslate')
    # плачущие статуи между колоннами (смотрят в центр)
    for z in range(-51, -20, 8):
        D.statue(m, X(-15), YH, Z(z), facing='east')
        D.statue(m, X(15), YH, Z(z), facing='west')
    # саркофаги вдоль центрального прохода
    for z in range(-52, -22, 6):
        D.coffin(m, X(-5), YH, Z(z), axis='z')
        D.coffin(m, X(5), YH, Z(z), axis='z')
    # ковровая дорожка
    for z in range(-60, -14):
        for dx in (-1, 0, 1):
            m.set(X(dx), YH, Z(z), 'minecraft:red_carpet' if dx == 0 else 'minecraft:black_carpet')
    # люстры под сводом
    for z in (-50, -38, -26):
        D.chandelier(m, X(0), YH + 18, Z(z), r=2, soul=True, drop=4)
    D.bones(m, X(-17), Z(-60), X(17), Z(-15), YH, density=0.04, seed=1)
    D.cobwebs(m, X(-17), YH + 8, Z(-60), X(17), YH + 18, Z(-15), chance=0.12, seed=2)
    # капелла Плакальщицы
    D.coffin(m, X(33), YH, Z(-44), axis='x', body='minecraft:chiseled_deepslate')
    for dz in range(-46, -29, 4):
        m.candle(X(39), YH, Z(dz), n=4)
        m.candle(X(27), YH, Z(dz), n=3)
    D.statue(m, X(38), YH, Z(-38), facing='west')
    m.sign(X(40), YH + 3, Z(-36), ['Фреска:', 'Серп Луны', 'над спящими.', '«Запомни знак»'], facing='west')
    m.cache(X(33), YH, Z(-31), tier=1, facing='south')
    D.chandelier(m, X(33), YH + 12, Z(-38), r=2, soul=True, drop=3)
    # выход на север: решётчатые ворота-арка
    D.arch(m, X(-3), YH, Z(-61), 'x', 5, 6, 'minecraft:chiseled_deepslate')


# ---------------------------------------------------------------- Коридор Ловушек и тайник

def traps_decor(m, st):
    # шипы: три пояса в полу (тёмные плиты-«отверстия»), бьют по очереди
    for i, z0 in enumerate((-66, -74, -82)):
        for x in range(-2, 3):
            for z in range(z0 - 2, z0 + 1):
                m.set(X(x), YH - 1, Z(z), 'minecraft:polished_blackstone_pressure_plate'.replace('_pressure_plate', ''))
        st.mech(f's1_spikes{i}', 'spikes', box=[X(-2), YH, Z(z0 - 2), X(2), YH + 1, Z(z0)], period=50, offset=i * 16, dmg=0.22,
                near=[X(-2), YH, Z(-90), X(2), YH + 5, Z(-61)])
    # стрелки в восточной стене — бьют на запад поперёк коридора
    spots = []
    for z in (-70, -78, -86):
        for y in (YH, YH + 1):
            m.set(X(3), y, Z(z), 'minecraft:dispenser', facing='west', triggered=False)
            spots.append([X(2), y, Z(z)])
    st.mech('s1_arrows', 'arrows', spots=spots, dir='west', period=30, near=[X(-2), YH, Z(-90), X(2), YH + 5, Z(-61)])
    for z in range(-88, -62, 6):
        m.torch_wall(X(-2), YH + 3, Z(z), 'east', soul=True)
    # треснувшая стена (ломается!) — тайник на западе
    for z in range(-76, -73):
        for y in range(YH, YH + 3):
            m.set(X(-3), y, Z(z), 'minecraft:infested_cracked_stone_bricks')
    # проход за стеной к комнате тайника
    m.carve(X(-5), YH, Z(-76), X(-4), YH + 2, Z(-74), m.themes['s1_tomb'])
    m.cache(X(-10), YH, Z(-79), tier=2, facing='south')
    D.bones(m, X(-14), Z(-80), X(-6), Z(-70), YH, density=0.12, seed=3)
    m.sign(X(-13), YH + 2, Z(-70), ['Здесь лежит', 'вор, что нашёл', 'выход. Возьми', 'его долю.'], facing='south')
    for z in (-79, -71):
        m.candle(X(-7), YH, Z(z), n=4)
    st.mech('s1_secret1', 'area', box=[X(-14), YH, Z(-80), X(-6), YH + 3, Z(-70)], xp=True, solved='Тайное место найдено')
    st.caches.append([X(-10), YH, Z(-79), 2])
    st.caches.append([X(33), YH, Z(-31), 1])


# ---------------------------------------------------------------- Оссуарий: три плиты

def ossuary_decor(m, st):
    # стены из черепов (ниши)
    for z in range(-106, -92, 2):
        D.niche_skulls(m, X(-10), YH + 1, Z(z), 'east', n=3, seed=z)
        D.niche_skulls(m, X(10), YH + 1, Z(z), 'west', n=3, seed=z + 1)
    # три плиты-печати
    plates = [[X(-6), YH - 1, Z(-100)], [X(0), YH - 1, Z(-104)], [X(6), YH - 1, Z(-100)]]
    for p in plates:
        m.set(p[0], p[1], p[2], 'minecraft:chiseled_polished_blackstone')
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            m.set(p[0] + dx, p[1], p[2] + dz, 'minecraft:gilded_blackstone')
        m.lantern(p[0], YH + 4, p[2], soul=True, hanging=True) if False else m.chain_lantern(p[0], YH + 8, p[2], length=2, soul=True)
    m.sign(X(0), YH + 2, Z(-91), ['Трое держали', 'врата Склепа.', 'Встаньте на', 'печати вместе.'], facing='south')
    # костяные врата на север (к арене)
    st.gate('s1_bonegate', [X(-2), YH, Z(-109), X(2), YH + 5, Z(-109)], 'minecraft:bone_block[axis=y]')
    m.carve(X(-2), YH, Z(-111), X(2), YH + 5, Z(-109), m.themes['s1_tomb'])
    st.mech('s1_plates', 'plates', plates=plates, hold=30, opens=['s1_bonegate'], xp=True, solved='Печати приняли троих — врата открыты',
            near=[X(-10), YH, Z(-108), X(10), YH + 6, Z(-91)])
    D.bones(m, X(-10), Z(-108), X(10), Z(-91), YH, density=0.07, seed=4)


# ---------------------------------------------------------------- Арена 1

def arena1(m, st):
    cz = -127
    # кольцо колонн-цепей и пол-узор
    for i in range(8):
        a = math.pi * 2 * i / 8 + math.pi / 8
        x, z = round(math.cos(a) * 10), round(math.sin(a) * 10) + cz
        m.big_pillar(X(x), Z(z), YH, 12, 'minecraft:deepslate_tiles', trim='minecraft:chiseled_deepslate')
    D.ring_floor(m, X(0), Z(cz), 5.5, 6.4, YH - 1, 'minecraft:polished_blackstone_bricks')
    D.ring_floor(m, X(0), Z(cz), 12.5, 13.4, YH - 1, 'minecraft:polished_blackstone_bricks')
    for i in range(16):
        a = math.pi * 2 * i / 16
        m.lantern(X(round(math.cos(a) * 15)), YH + 4, Z(round(math.sin(a) * 15) + cz), soul=True)
    D.chandelier(m, X(0), YH + 17, Z(cz), r=3, soul=True, drop=4)
    # ворота: вход (юг, открыт; в бою закрывается), выход (север, откроется после победы)
    st.gate('s1_a1_in', [X(-2), YH, Z(-112), X(2), YH + 4, Z(-112)], 'minecraft:iron_bars', open_=True)
    st.gate('s1_a1_out', [X(-2), YH, Z(-143), X(2), YH + 4, Z(-143)], 'minecraft:iron_bars')
    st.boss('gatekeeper', 'litrpg:gatekeeper', [X(0), YH, Z(cz - 6), 0], [X(-16), YH - 1, Z(cz - 16), X(16), YH + 16, Z(cz + 16)],
            doors=['s1_a1_in'], unlock=['s1_a1_out'],
            extra={'pillars': [[X(round(math.cos(math.pi * 2 * i / 8 + math.pi / 8) * 10)), YH,
                                 Z(round(math.sin(math.pi * 2 * i / 8 + math.pi / 8) * 10) + cz)] for i in range(8)]})
    st.lore_at([X(-15), YH - 1, Z(cz - 15), X(15), YH + 8, Z(cz + 15)], 'Арена Привратника',
               'Привратник Костей не пропускает живых. Его цепи не рвутся — но колонны держат их не вечно.')
    # развилка: указатели
    m.sign(X(-5), YH + 2, Z(-163), ['← Катакомбы', '(Зал Огней)'], facing='south')
    m.sign(X(5), YH + 2, Z(-163), ['Мост над', 'Бездной ↑'], facing='south')
    D.coffin(m, X(-5), YH, Z(-156), axis='x')
    D.coffin(m, X(4), YH, Z(-156), axis='x')


# ---------------------------------------------------------------- Катакомбы, Зал Огней, Костоправ

def catacombs(m, st):
    tomb = m.themes['s1_tomb']
    # ниши с черепами по стенам ходов (там, где стена)
    for (x, y, z), t in list(m.open.items()):
        if t != 's1_tomb' or y != YC + 1:
            continue
        for dx, dz, f in ((1, 0, 'west'), (-1, 0, 'east'), (0, 1, 'north'), (0, -1, 'south')):
            if (x + dx, y, z + dz) not in m.open and (x * 7 + z * 3) % 9 == 0:
                D.niche_skulls(m, x, y - 1, z, f, n=2, seed=x + z)
    D.cobwebs(m, X(-62), YC + 2, Z(-190), X(-20), YC + 3, Z(-130), chance=0.2, seed=6)
    for c in ((-30, -157), (-42, -157), (-30, -185), (-42, -135), (-50, -175)):
        m.torch_wall(X(c[0]) - 1, YC + 2, Z(c[1]), 'east', soul=True) if False else m.set(X(c[0]), YC + 3, Z(c[1]), 'minecraft:soul_lantern',
                                                                                              hanging=True, waterlogged=False)
    m.sign(X(-36), YC + 2, Z(-186), ['Фреска:', 'Череп в венце.', '«Запомни знак»'], facing='north')
    m.cache(X(-61), YC, Z(-135), tier=0, facing='west')
    st.caches.append([X(-61), YC, Z(-135), 0])

    # ---- Зал Огней: 4 лампы на северной стене, 4 рычага у южной, колокол в центре
    lamps, levers = [], []
    for i, x in enumerate((-74, -71, -67, -64)):
        m.set(X(x), YC + 3, Z(-145), 'minecraft:redstone_lamp', lit=False)
        m.set(X(x), YC + 2, Z(-145), 'minecraft:chiseled_stone_bricks')
        lamps.append([X(x), YC + 3, Z(-145)])
        m.set(X(x), YC, Z(-126), 'minecraft:chiseled_stone_bricks')
        m.set(X(x), YC + 1, Z(-126), 'minecraft:lever', face='floor', facing='north', powered=False)
        levers.append([X(x), YC + 1, Z(-126)])
    m.set(X(-69), YC, Z(-135), 'minecraft:bell', attachment='floor', facing='north', powered=False)
    m.set(X(-69), YC - 1, Z(-135), 'minecraft:gilded_blackstone')
    m.sign(X(-69), YC + 2, Z(-125), ['Ударь в колокол.', 'Огни покажут', 'порядок. Повтори', 'его рычагами.'], facing='north')
    st.mech('s1_fires', 'sequence', lamps=lamps, levers=levers, show=[X(-69), YC, Z(-135)], order=[2, 0, 3, 1, 2], opens=['s1_bridgegate'],
            xp=True, solved='Огни сошлись — решётка моста поднята!', punish=0.12, ambush=['s1_fires_amb'],
            near=[X(-76), YC, Z(-145), X(-62), YC + 6, Z(-125)])
    D.chandelier(m, X(-69), YC + 10, Z(-135), r=2, soul=False, drop=2)

    # ---- Яма Костоправа
    D.bones(m, X(-74), Z(-186), X(-60), Z(-166), YC - 2, density=0.25, seed=7)
    for x in (-72, -62):
        for z in (-184, -168):
            D.brazier(m, X(x), YC - 2, Z(z), soul=True)
    st.caches.append([X(-67), YC - 2, Z(-185), 1])
    m.cache(X(-67), YC - 2, Z(-185), tier=1, facing='south')


# ---------------------------------------------------------------- Мост над Бездной

def bridge(m, st):
    # мост 3 в ширину, с осыпающимся участком посередине
    for z in range(-216, -173):
        for x in range(-1, 2):
            crack = -205 <= z <= -197
            m.set(X(x), YH - 1, Z(z), 'minecraft:cracked_stone_bricks' if crack else 'minecraft:polished_blackstone_bricks')
        if z % 4 == 0:
            for x in (-2, 2):
                m.set(X(x), YH - 1, Z(z), 'minecraft:polished_blackstone_bricks')
                m.set(X(x), YH, Z(z), 'minecraft:polished_blackstone_wall', up=True, north='none', south='none', east='none', west='none',
                      waterlogged=False)
                m.set(X(x), YH + 1, Z(z), 'minecraft:soul_lantern', hanging=False, waterlogged=False)
    st.mech('s1_collapse', 'collapse', box=[X(-1), YH - 1, Z(-205), X(1), YH - 1, Z(-197)], tile='minecraft:cracked_stone_bricks',
            near=[X(-18), YH - 1, Z(-216), X(18), YH + 8, Z(-174)])
    # уступы лучников: площадки по бокам
    for x1, x2 in ((-17, -12), (12, 17)):
        for z in range(-205, -184):
            for x in range(x1, x2 + 1):
                m.set(X(x), YH + 4, Z(z), 'minecraft:polished_blackstone_bricks')
    # решётка перед мостом (открывает загадка Зала Огней)
    st.gate('s1_bridgegate', [X(-2), YH, Z(-171), X(2), YH + 4, Z(-171)], 'minecraft:iron_bars')
    m.sign(X(3), YH + 2, Z(-166), ['Решётку держат', 'огни Катакомб.', 'Запад → Зал Огней'], facing='south')
    st.lore_at([X(-18), YH - 1, Z(-216), X(18), YH + 10, Z(-174)], 'Мост над Бездной', 'Не стойте на треснувших плитах — Бездна голодна.')


# ---------------------------------------------------------------- Дверь Загадки

def riddle(m, st):
    buttons = []
    answers = ['Яма', 'Тень', 'Время', 'Огонь']
    for i, x in enumerate((-6, -2, 2, 6)):
        m.set(X(x), YH, Z(-240), 'minecraft:chiseled_polished_blackstone')
        m.set(X(x), YH + 1, Z(-240), 'minecraft:chiseled_polished_blackstone')
        m.set(X(x), YH + 1, Z(-239), 'minecraft:polished_blackstone_button', face='wall', facing='south', powered=False)
        buttons.append([X(x), YH + 1, Z(-239)])
        m.sign(X(x), YH + 2, Z(-239), [answers[i]], facing='south')
    m.sign(X(0), YH + 3, Z(-227), ['«Чем больше', 'из меня берёшь,', 'тем больше', 'я становлюсь»'], facing='north')
    m.sign(X(-4), YH + 2, Z(-227), ['Ответь кнопкой', 'у северной стены.', 'Ошибка будит', 'мёртвых.'], facing='north')
    st.gate('s1_riddlegate', [X(-2), YH, Z(-241), X(2), YH + 4, Z(-241)], 'minecraft:polished_deepslate')
    m.carve(X(-2), YH, Z(-242), X(2), YH + 4, Z(-241), m.themes['s1_soul'])
    st.mech('s1_riddle', 'riddle', buttons=buttons, answer=0, opens=['s1_riddlegate'], ambush=['s1_riddle_amb'], xp=True,
            solved='Верно: яма. Дверь отворилась.', wrong='Неверно! Мёртвые поднимаются…', punish=0.0)
    for x in (-7, 7):
        D.brazier(m, X(x), YH, Z(-229), soul=True)


# ---------------------------------------------------------------- Арена 2: река душ

def arena2(m, st):
    # река душ поперёк зала (огонь на песке душ) с тремя мостиками
    for x in range(-18, 19):
        for z in (-264, -263):
            if abs(x) in (0, 1) or abs(x - 10) <= 1 or abs(x + 10) <= 1:
                m.set(X(x), YH - 1, Z(z), 'minecraft:polished_blackstone_bricks')
                continue
            m.set(X(x), YH - 1, Z(z), 'minecraft:soul_sand')
            m.set(X(x), YH, Z(z), 'minecraft:soul_fire')
    # два помоста (юг — Морра, север — Весса)
    for cz in (-255, -272):
        for x in range(-5, 6):
            for z in range(cz - 3, cz + 4):
                if abs(x) + abs(z - cz) <= 6:
                    m.set(X(x), YH, Z(z), 'minecraft:polished_blackstone_slab', type='bottom', waterlogged=False)
        D.brazier(m, X(-6), YH, Z(cz), soul=True)
        D.brazier(m, X(6), YH, Z(cz), soul=True)
    for z in range(-278, -248, 6):
        m.big_pillar(X(-16), Z(z), YH, 13, 'minecraft:polished_blackstone_bricks', trim='minecraft:chiseled_polished_blackstone')
        m.big_pillar(X(15), Z(z), YH, 13, 'minecraft:polished_blackstone_bricks', trim='minecraft:chiseled_polished_blackstone')
    D.chandelier(m, X(0), YH + 17, Z(-263), r=3, soul=True, drop=3)
    m.sign(X(17), YH + 3, Z(-276), ['Фреска:', 'Око, что видит', 'обе стороны.', '«Запомни знак»'], facing='west')
    st.gate('s1_a2_in', [X(-2), YH, Z(-246), X(2), YH + 4, Z(-246)], 'minecraft:iron_bars', open_=True)
    st.gate('s1_a2_out', [X(-2), YH, Z(-281), X(2), YH + 4, Z(-281)], 'minecraft:iron_bars')
    st.boss('twins', 'litrpg:twin_morra', [X(0), YH, Z(-255), 0], [X(-18), YH - 1, Z(-280), X(18), YH + 17, Z(-247)],
            doors=['s1_a2_in'], unlock=['s1_a2_out'],
            extra={'south': [X(0), YH, Z(-255)], 'north': [X(0), YH, Z(-272)],
                   'bridges': [[X(0), YH, Z(-263)], [X(10), YH, Z(-263)], [X(-10), YH, Z(-263)]]})
    st.lore_at([X(-18), YH - 1, Z(-280), X(18), YH + 10, Z(-247)], 'Сёстры Морра и Весса',
               'Одна поднимает мёртвых, другая пьёт жизнь. Убейте их почти разом — иначе выжившая вернёт сестру.')


# ---------------------------------------------------------------- Зал Дисков

def dials(m, st):
    pos = []
    for i, x in enumerate((-4, 0, 4)):
        m.set(X(x), YH, Z(-305), 'minecraft:chiseled_polished_blackstone')
        m.set(X(x), YH + 1, Z(-305), 'litrpg:rune_dial', facing='south', symbol=0)
        pos.append([X(x), YH + 1, Z(-305)])
    m.sign(X(0), YH + 3, Z(-305), ['Три знака', 'трёх залов.', 'Луна, Череп, Око —', 'в том же порядке'], facing='south')
    st.gate('s1_dialgate', [X(-2), YH, Z(-306), X(2), YH + 4, Z(-306)], 'minecraft:polished_blackstone_bricks')
    # символы: 0 огонь, 1 вода, 2 луна, 3 солнце, 4 череп, 5 око
    st.mech('s1_dials', 'dials', dials=pos, answer=[2, 4, 5], start=[0, 0, 0], opens=['s1_dialgate'], xp=True,
            solved='Знаки сошлись. Путь к Тропе Королей открыт.')
    for x in (-8, 8):
        m.chain_lantern(X(x), YH + 11, Z(-298), length=3, soul=True)


# ---------------------------------------------------------------- Тропа Королей (пол памяти)

def memory(m, st):
    # поле 9×11 над пропастью; тропа светится, если встать на стартовую плиту
    ox, oz = X(-4), Z(-333)
    path = [(4, 10), (4, 9), (3, 9), (2, 9), (2, 8), (2, 7), (3, 7), (4, 7), (5, 7), (6, 7), (6, 6), (6, 5), (5, 5), (4, 5), (3, 5),
            (2, 5), (2, 4), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (6, 2), (6, 1), (5, 1), (4, 1), (4, 0)]
    for x in range(9):
        for z in range(11):
            m.set(ox + x, YT - 1, oz + z, 'minecraft:polished_deepslate')
    m.set(X(0), YT - 1, Z(-321), 'minecraft:gilded_blackstone')
    for x in range(-8, 9):
        m.set(X(x), YT - 1, Z(-337), 'minecraft:polished_blackstone_bricks')
        m.set(X(x), YT - 1, Z(-338), 'minecraft:polished_blackstone_bricks')
    for x in range(-8, 9):
        for z in (-321, -320, -319):
            m.set(X(x), YT - 1, Z(z), 'minecraft:polished_blackstone_bricks')
    m.set(X(0), YT - 1, Z(-321), 'minecraft:gilded_blackstone')
    m.sign(X(-3), YT + 1, Z(-319), ['Встань на', 'золотую плиту —', 'тропа вспыхнет.', 'Иди только по ней'], facing='north', wall=False)
    st.mech('s1_memory', 'memory', origin=[ox, YT - 1, oz], w=9, h=11, path=[[a, b] for a, b in path], start=[X(0), YT - 1, Z(-321)],
            tile='minecraft:polished_deepslate', glow='minecraft:sea_lantern', xp=True, solved='Тропа Королей пройдена',
            near=[X(-8), YT - 1, Z(-338), X(8), YT + 6, Z(-319)])
    for z in (-336, -322):
        for x in (-7, 7):
            m.chain_lantern(X(x), YT + 9, Z(z), length=3, soul=True)


# ---------------------------------------------------------------- Арена 3: Тронный зал и Фиал

def arena3(m, st):
    # колоннада
    for z in range(-386, -350, 7):
        for x in (-16, 15):
            m.big_pillar(X(x), Z(z), YT, 18, 'minecraft:polished_blackstone_bricks', trim='minecraft:gilded_blackstone')
    # трон
    tz = -386
    for x in range(-4, 5):
        for z in range(tz - 1, tz + 4):
            m.set(X(x), YT, Z(z), 'minecraft:polished_blackstone_bricks')
    for x in range(-2, 3):
        m.set(X(x), YT + 1, Z(tz), 'minecraft:polished_blackstone_bricks')
        m.set(X(x), YT + 2, Z(tz - 1), 'minecraft:chiseled_polished_blackstone')
    m.stairs(X(0), YT + 1, Z(tz + 1), 'minecraft:polished_blackstone_brick_stairs', 'south')
    for y in range(YT + 1, YT + 8):
        m.set(X(-3), y, Z(tz - 1), 'minecraft:crying_obsidian')
        m.set(X(3), y, Z(tz - 1), 'minecraft:crying_obsidian')
    # дорожка к трону, жаровни
    for z in range(-384, -346):
        m.set(X(0), YT, Z(z), 'minecraft:purple_carpet')
    for z in range(-380, -352, 7):
        D.brazier(m, X(-6), YT, Z(z), soul=True)
        D.brazier(m, X(6), YT, Z(z), soul=True)
    for z in (-378, -362):
        D.chandelier(m, X(0), YT + 25, Z(z), r=3, soul=True, drop=5)
    # Фиал: кольцо платформ над бездной вокруг кристалла-филактерии
    vz, vy = -368, YT - 28
    for i in range(6):
        a = math.pi * 2 * i / 6
        px, pz = round(math.cos(a) * 9), round(math.sin(a) * 9) + vz
        for x in range(px - 2, px + 3):
            for z in range(pz - 2, pz + 3):
                if math.hypot(x - px, z - pz) <= 2.6:
                    m.set(X(x), vy - 1, Z(z), 'minecraft:polished_blackstone_bricks')
        m.set(X(px), vy - 1, Z(pz), 'minecraft:crying_obsidian')
    for x in range(-2, 3):
        for z in range(vz - 2, vz + 3):
            m.set(X(x), vy - 1, Z(z), 'minecraft:obsidian')
    m.set(X(0), vy, Z(vz), 'litrpg:relic', kind='soul')
    # мостики между платформами (узкие)
    for i in range(6):
        a0, a1 = math.pi * 2 * i / 6, math.pi * 2 * (i + 1) / 6
        for k in range(1, 8):
            t = k / 8
            x = round(math.cos(a0) * 9 * (1 - t) + math.cos(a1) * 9 * t)
            z = round(math.sin(a0) * 9 * (1 - t) + math.sin(a1) * 9 * t) + vz
            m.set(X(x), vy - 1, Z(z), 'minecraft:polished_blackstone_bricks')
    # Разлом: преграда из плачущего обсидиана до победы, за ней — пелена разлома
    st.gate('s1_rift', [X(-3), YT, Z(-391), X(3), YT + 6, Z(-391)], 'minecraft:crying_obsidian')
    for x in range(-3, 4):
        for y in range(YT, YT + 7):
            m.set(X(x), y, Z(-399), 'litrpg:rift')
    st.gate('s1_a3_in', [X(-3), YT, Z(-346), X(3), YT + 6, Z(-346)], 'minecraft:iron_bars', open_=True)
    phyl = [[X(round(math.cos(math.pi * 2 * i / 6) * 9)), vy, Z(round(math.sin(math.pi * 2 * i / 6) * 9) + vz)] for i in range(6)]
    st.boss('lich', 'litrpg:lich_king', [X(0), YT + 1, Z(-383), 0], [X(-20), YT - 1, Z(-390), X(20), YT + 25, Z(-347)],
            doors=['s1_a3_in'], unlock=['s1_rift'],
            extra={'vault': [X(0), vy, Z(vz)], 'vault_box': [X(-14), vy - 2, Z(vz - 14), X(14), vy + 14, Z(vz + 14)], 'platforms': phyl,
                   'throne': [X(0), YT + 1, Z(-384)]})
    st.lore_at([X(-20), YT - 1, Z(-390), X(20), YT + 10, Z(-347)], 'Тронный зал Короля-лича',
               'Король-лич бессмертен, пока цела его филактерия. Он сам приведёт вас к ней.')


# ---------------------------------------------------------------- отряды

def packs(st):
    S = st.pack
    # Зал Плача
    S('s1_h1', (X(0), YH, Z(-22)), [('zombie', X(-3), YH, Z(-24), 1), ('zombie', X(3), YH, Z(-24), 1), ('zombie', X(0), YH, Z(-27), 2)], r=16)
    S('s1_h2', (X(0), YH, Z(-36)), [('skeleton', X(-7), YH, Z(-36), 2), ('skeleton', X(7), YH, Z(-36), 2),
                                    ('litrpg:barrow_wight', X(0), YH, Z(-38), 2, 'ELITE')], r=16)
    S('s1_h3', (X(0), YH, Z(-51)), [('litrpg:ghoul', X(-4), YH, Z(-52), 3), ('litrpg:ghoul', X(0), YH, Z(-54), 3),
                                    ('litrpg:ghoul', X(4), YH, Z(-52), 3), ('zombie', X(0), YH, Z(-49), 2)], r=16)
    S('s1_banshee', (X(33), YH, Z(-38)), [('litrpg:banshee', X(33), YH, Z(-40), 3, 'CHAMPION', 'Плакальщица', 'shade'),
                                          ('litrpg:bloodbat', X(30), YH + 3, Z(-36), 2), ('litrpg:bloodbat', X(36), YH + 3, Z(-36), 2)],
      r=10, mini=True)
    # Коридор Ловушек и Оссуарий
    S('s1_t1', (X(0), YH, Z(-88)), [('skeleton', X(-1), YH, Z(-89), 3), ('skeleton', X(1), YH, Z(-89), 3)], r=12)
    S('s1_o1', (X(0), YH, Z(-99)), [('zombie', X(-5), YH, Z(-96), 3, 'ELITE'), ('skeleton', X(5), YH, Z(-96), 3),
                                    ('litrpg:mummy', X(0), YH, Z(-106), 4), ('skeleton', X(-6), YH, Z(-106), 3)], r=14)
    # развилка
    S('s1_f1', (X(0), YH, Z(-157)), [('litrpg:grave_knight', X(0), YH, Z(-160), 5, 'ELITE'), ('skeleton', X(-4), YH, Z(-160), 4),
                                     ('skeleton', X(4), YH, Z(-160), 4)], r=14)
    # Катакомбы
    S('s1_c1', (X(-30), YC, Z(-157)), [('litrpg:grimlock', X(-28), YC, Z(-157), 5), ('litrpg:grimlock', X(-32), YC, Z(-157), 5),
                                       ('litrpg:grimlock', X(-30), YC, Z(-160), 5)], r=10)
    S('s1_c2', (X(-42), YC, Z(-140)), [('litrpg:barrow_wight', X(-42), YC, Z(-140), 5), ('zombie', X(-42), YC, Z(-137), 5),
                                       ('zombie', X(-45), YC, Z(-135), 5)], r=10)
    S('s1_c3', (X(-50), YC, Z(-170)), [('litrpg:mummy', X(-50), YC, Z(-170), 6), ('litrpg:ghoul', X(-50), YC, Z(-166), 5),
                                       ('litrpg:ghoul', X(-53), YC, Z(-175), 5)], r=10)
    S('s1_c4', (X(-38), YC, Z(-185)), [('litrpg:mimic', X(-46), YC, Z(-185), 6, 'ELITE'), ('skeleton', X(-36), YC, Z(-185), 5)], r=9)
    S('s1_fires_amb', (X(-69), YC, Z(-135)), [('zombie', X(-73), YC, Z(-130), 5), ('zombie', X(-65), YC, Z(-130), 5),
                                              ('skeleton', X(-69), YC, Z(-142), 5)], ambush=True)
    S('s1_bonesetter', (X(-67), YC - 2, Z(-176)), [('litrpg:bone_colossus', X(-67), YC - 2, Z(-178), 6, 'CHAMPION', 'Костоправ', 'brute'),
                                                   ('skeleton', X(-71), YC - 2, Z(-172), 5), ('skeleton', X(-63), YC - 2, Z(-172), 5)],
      r=12, mini=True)
    # Мост
    S('s1_b1', (X(0), YH, Z(-195)), [('skeleton', X(-14), YH + 5, Z(-192), 6), ('skeleton', X(-14), YH + 5, Z(-200), 6),
                                     ('skeleton', X(14), YH + 5, Z(-192), 6), ('skeleton', X(14), YH + 5, Z(-200), 6),
                                     ('litrpg:bloodbat', X(0), YH + 6, Z(-196), 6), ('litrpg:bloodbat', X(3), YH + 6, Z(-200), 6)], r=22)
    S('s1_b2', (X(0), YH, Z(-214)), [('litrpg:grave_knight', X(0), YH, Z(-214), 6, 'ELITE'), ('zombie', X(-1), YH, Z(-212), 6)], r=12)
    # Дверь Загадки — засада
    S('s1_riddle_amb', (X(0), YH, Z(-233)), [('litrpg:barrow_wight', X(-6), YH, Z(-230), 7, 'ELITE'), ('skeleton', X(6), YH, Z(-230), 7),
                                             ('zombie', X(0), YH, Z(-236), 7)], ambush=True)
    # за Ареной 2
    S('s1_d1', (X(0), YH, Z(-286)), [('litrpg:grave_knight', X(-1), YH, Z(-286), 8, 'ELITE'), ('litrpg:barrow_wight', X(1), YH, Z(-288), 8)], r=10)
    S('s1_d2', (X(0), YH, Z(-298)), [('skeleton', X(-6), YH, Z(-296), 8), ('skeleton', X(6), YH, Z(-296), 8),
                                     ('litrpg:mummy', X(0), YH, Z(-300), 8, 'ELITE')], r=12)
    # перед троном
    S('s1_k1', (X(0), YT, Z(-343)), [('litrpg:grave_knight', X(-2), YT, Z(-343), 9, 'ELITE'), ('litrpg:grave_knight', X(2), YT, Z(-343), 9, 'ELITE')],
      r=10)
