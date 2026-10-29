"""
Этап 4 · Кузни Пепла (ур. 30–40). Заброшенные кузни древнего народа: лава, пресс и пламя.

  Привал в караульне мастеров → Литейный цех (огнемёты по краям, лавовые жёлобы) → Коридор прессов
  → АРЕНА 1: Плавильня (Голем-плавильщик; закалочные купели) → Лавовая река (мост из плит по такту)
  → Зал Мехов (порядок огней по колоколу) → АРЕНА 2: Горнило близнецов (Игнис и Синдер)
  → Горн надсмотрщика (мини-хранитель, тайник за кнопкой) → АРЕНА 3: Трон Повелителя Пепла
  (пол рушится в лаву; в конце — кальдера: островки в озере лавы) → Разлом в кальдере.
"""
import math

from .build import Map, Theme, Stage, apply_gates, h3
from . import decor as D
from . import secrets

OX, OZ = 4096, 0
Y = 64       # привал
YF = 60      # кузни
YC = 50      # кальдера (пол островов)


def X(x):
    return OX + x


def Z(z):
    return OZ + z


def build():
    m = Map(seed=4004)
    st = Stage(4, 'forges', 'Кузни Пепла', 'Здесь ковали богов — и сожгли', (30, 40), music='forges')

    guard = m.theme(Theme('s4_guard', [('minecraft:polished_blackstone_bricks', 5), ('minecraft:cracked_polished_blackstone_bricks', 1),
                                       ('minecraft:gilded_blackstone', 0.1)],
                          floor=[('minecraft:polished_blackstone', 3), ('minecraft:smooth_basalt', 1)], ceil='minecraft:polished_blackstone_bricks', seed=41))
    forge = m.theme(Theme('s4_forge', [('minecraft:deepslate_bricks', 4), ('minecraft:polished_blackstone_bricks', 3), ('minecraft:magma_block', 0.06),
                                       ('minecraft:cracked_deepslate_bricks', 1)],
                          floor=[('minecraft:polished_basalt', 2), ('minecraft:deepslate_tiles', 3), ('minecraft:smooth_basalt', 1)],
                          ceil=[('minecraft:deepslate_bricks', 3), ('minecraft:blackstone', 1)], seed=42))
    rock = m.theme(Theme('s4_rock', [('minecraft:basalt', 3), ('minecraft:blackstone', 3), ('minecraft:magma_block', 0.1), ('minecraft:netherrack', 1)],
                         floor=[('minecraft:blackstone', 3), ('minecraft:basalt', 2), ('minecraft:smooth_basalt', 1)], seed=43))
    hall = m.theme(Theme('s4_hall', [('minecraft:polished_blackstone_bricks', 4), ('minecraft:gilded_blackstone', 0.2), ('minecraft:chiseled_polished_blackstone', 0.3)],
                         floor=[('minecraft:polished_blackstone', 3), ('minecraft:chiseled_polished_blackstone', 0.2)],
                         ceil='minecraft:polished_blackstone_bricks', seed=44))
    ash = m.theme(Theme('s4_ash', [('minecraft:basalt', 4), ('minecraft:blackstone', 2), ('minecraft:magma_block', 0.2)],
                        floor=[('minecraft:basalt', 3), ('minecraft:magma_block', 0.3)], seed=45))

    # ======================= пустоты =======================
    m.room(X(-9), Z(0), X(9), Z(15), Y, 7, guard, vault=3)
    m.corridor([(X(0), Z(-1), Y), (X(0), Z(-6), Y), (X(0), Z(-12), YF)], 5, 6, forge)
    # Литейный цех
    m.room(X(-16), Z(-66), X(16), Z(-13), YF, 13, forge, vault=4)
    m.carve(X(-16), YF - 3, Z(-64), X(-14), YF - 1, Z(-15), forge)      # лавовые жёлобы вдоль стен
    m.carve(X(14), YF - 3, Z(-64), X(16), YF - 1, Z(-15), forge)
    # Коридор прессов
    m.carve(X(-2), YF, Z(-97), X(2), YF + 5, Z(-67), forge)
    # АРЕНА 1 — Плавильня
    m.room(X(-18), Z(-132), X(18), Z(-98), YF, 14, forge, vault=4)
    # к лавовой реке
    m.carve(X(-2), YF, Z(-148), X(2), YF + 5, Z(-133), rock)
    m.cave(X(0), YF + 8, Z(-165), 22, 12, 22, rock, rough=0.25, seed=46, floor=YF)
    m.carve(X(-22), YF - 4, Z(-172), X(22), YF - 1, Z(-158), rock)     # русло лавы
    # Зал Мехов
    m.carve(X(-2), YF, Z(-192), X(2), YF + 5, Z(-182), hall)
    m.room(X(-10), Z(-206), X(10), Z(-193), YF, 9, hall, vault=3)
    m.carve(X(-2), YF, Z(-220), X(2), YF + 5, Z(-207), hall)
    # АРЕНА 2 — Горнило близнецов
    m.round_room(X(0), Z(-238), 20, YF, 14, forge, dome=6)
    # Горн надсмотрщика
    m.carve(X(-2), YF, Z(-268), X(2), YF + 5, Z(-259), forge)
    m.room(X(-9), Z(-290), X(9), Z(-269), YF, 9, forge, vault=3)
    m.carve(X(-17), YF, Z(-282), X(-10), YF + 3, Z(-280), guard)       # тайный лаз (за преградой)
    m.room(X(-25), Z(-286), X(-18), Z(-276), YF, 5, guard)
    # к трону
    m.carve(X(-2), YF, Z(-306), X(2), YF + 6, Z(-291), hall)
    # АРЕНА 3 — Трон Повелителя Пепла (пол над лавой)
    m.room(X(-22), Z(-350), X(22), Z(-307), YF, 18, hall, vault=6)
    m.carve(X(-21), YF - 4, Z(-349), X(21), YF - 1, Z(-308), ash)
    # Кальдера (смена места в бою): озеро лавы с островками
    m.cave(X(0), YC + 10, Z(-400), 27, 16, 27, ash, rough=0.2, seed=47, floor=YC - 1)

    # ======================= отделка =======================
    camp_decor(m)
    foundry(m, st)
    presses(m, st)
    arena1(m, st)
    river(m, st)
    bellows(m, st)
    arena2(m, st)
    overseer(m, st)
    arena3(m, st)
    caldera(m, st)
    packs(st)

    st.box = [X(-45), 20, Z(-432), X(45), 110, Z(22)]
    st.camp = [X(0), Y, Z(12), 180]
    st.trader = [X(-5), Y, Z(3), 135]
    m.auto_lights(spacing=8, soul=False, themes={'s4_guard', 's4_forge', 's4_hall'}, min_h=6)
    m.glow(spacing=5, level=11)
    # секретные комнаты с пасхалками (ищутся сами — tools/dungeon/secrets.py)
    secrets.add(m, st, 2, (st.box[0], st.box[2], st.box[3], st.box[5]), seed=401)
    apply_gates(m, st)
    m.shell(thick=2)
    return m, st


# ---------------------------------------------------------------- отделка

def lava(m, x1, y1, z1, x2, y2, z2):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                m.set(x, y, z, 'minecraft:lava', level=0)


def camp_decor(m):
    m.set(X(0), Y, Z(8), 'minecraft:campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
    for x in (-7, 7):
        m.set(X(x), Y, Z(3), 'minecraft:anvil', facing='east')
        m.set(X(x), Y, Z(6), 'minecraft:blast_furnace', facing='east' if x < 0 else 'west', lit=True)
        m.set(X(x), Y, Z(9), 'minecraft:smithing_table')
        m.set(X(x), Y, Z(12), 'minecraft:barrel', facing='up', open=False)
    for x in (-8, 8):
        for z in (1, 14):
            m.set(X(x), Y, Z(z), 'minecraft:shroomlight')
    m.sign(X(0), Y + 2, Z(15), ['ПРИВАЛ', 'Караульня мастеров.', 'Дальше — жар.'], facing='north')


def foundry(m, st):
    # лавовые жёлобы за решётками
    for x0 in (-16, 14):
        lava(m, X(x0), YF - 3, Z(-64), X(x0 + 2), YF - 2, Z(-15))
        for z in range(-64, -14):
            m.set(X(x0 + (2 if x0 < 0 else 0)), YF, Z(z), 'minecraft:iron_bars')
    # печи и наковальни по рядам
    for z in range(-60, -16, 6):
        for x in (-11, 10):
            m.set(X(x), YF, Z(z), 'minecraft:blast_furnace', facing='east' if x < 0 else 'west', lit=True)
            m.set(X(x), YF + 1, Z(z), 'minecraft:blast_furnace', facing='east' if x < 0 else 'west', lit=True)
            m.set(X(x), YF + 2, Z(z), 'minecraft:polished_blackstone_brick_wall')
        m.set(X(-6), YF, Z(z + 3), 'minecraft:anvil', facing='north')
        m.set(X(6), YF, Z(z + 3), 'minecraft:anvil', facing='south')
    # огнемёты: левая и правая половины по очереди
    for i, z in enumerate((-26, -38, -50)):
        for side, (xa, xb) in enumerate(((-13, -1), (1, 13))):
            st.mech(f's4_flame{i}{side}', 'flames', box=[X(xa), YF, Z(z - 1), X(xb), YF + 2, Z(z + 1)], period=60, offset=(i * 20 + side * 30) % 60,
                    dmg=0.2, near=[X(-16), YF, Z(-66), X(16), YF + 8, Z(-13)])
            for x in range(xa, xb + 1, 3):
                m.set(X(x), YF - 1, Z(z), 'minecraft:magma_block')
    st.lore_at([X(-16), YF - 1, Z(-66), X(16), YF + 8, Z(-13)], 'Литейный цех',
               'Огнемёты бьют по очереди — то слева, то справа. Следите за искрами на полу.')


def presses(m, st):
    for i, z in enumerate((-72, -78, -84, -90)):
        st.mech(f's4_press{i}', 'crusher', box=[X(-2), YF, Z(z - 1), X(2), YF + 2, Z(z)], period=44, offset=i * 11, dmg=0.3,
                near=[X(-2), YF, Z(-97), X(2), YF + 5, Z(-67)])
        for x in range(-2, 3):
            for zz in (z - 1, z):
                m.set(X(x), YF + 5, Z(zz), 'minecraft:iron_block')
                m.set(X(x), YF - 1, Z(zz), 'minecraft:polished_blackstone_bricks')
    m.sign(X(2), YF + 1, Z(-68), ['Прессы бьют', 'один за другим.', 'Бегите в такт!'], facing='north')
    st.lore_at([X(-2), YF - 1, Z(-97), X(2), YF + 5, Z(-67)], 'Коридор прессов', 'Прессы бьют волной — проскакивайте сразу за ударом.')


def arena1(m, st):
    pools = []
    for x, z in ((-10, -106), (10, -106), (-10, -124), (10, -124)):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                m.set(X(x + dx), YF - 1, Z(z + dz), 'minecraft:water', level=0)
        for dx in (-2, 2):
            for dz in range(-2, 3):
                m.set(X(x + dx), YF - 1, Z(z + dz), 'minecraft:polished_blackstone_bricks')
                m.set(X(x + dz), YF - 1, Z(z + dx), 'minecraft:polished_blackstone_bricks')
        pools.append([X(x), YF - 1, Z(z)])
    # тигли и желоба
    for x in (-16, 15):
        for z in range(-128, -100, 7):
            m.set(X(x), YF, Z(z), 'minecraft:cauldron')
            m.set(X(x), YF + 1, Z(z), 'minecraft:chain', axis='y')
    st.gate('s4_a1_in', [X(-2), YF, Z(-97), X(2), YF + 4, Z(-97)], 'minecraft:iron_bars', open_=True)
    st.gate('s4_a1_out', [X(-2), YF, Z(-133), X(2), YF + 4, Z(-133)], 'minecraft:iron_bars')
    st.boss('smelter_golem', 'litrpg:smelter_golem', [X(0), YF, Z(-115), 0], [X(-18), YF - 1, Z(-132), X(18), YF + 18, Z(-98)],
            doors=['s4_a1_in'], unlock=['s4_a1_out'], extra={'pools': pools})
    st.lore_at([X(-18), YF - 1, Z(-132), X(18), YF + 10, Z(-98)], 'Плавильня',
               'Голем раскаляется. Танк — заведите его в закалочную купель, пока жар не сжёг группу.')


def river(m, st):
    lava(m, X(-22), YF - 4, Z(-172), X(22), YF - 3, Z(-158))
    gates = []
    for i, (z1, z2) in enumerate(((-160, -158), (-163, -161), (-166, -164), (-169, -167), (-172, -170))):
        gid = f's4_bridge{i}'
        st.gate(gid, [X(-1), YF - 1, Z(z1), X(1), YF - 1, Z(z2)], 'minecraft:polished_blackstone_bricks')
        gates.append(gid)
    st.mech('s4_rhythm', 'rhythm', gates=gates, period=100, on=60, near=[X(-22), YF - 4, Z(-176), X(22), YF + 6, Z(-154)])
    for x in (-3, 3):
        for z in (-157, -173):
            m.set(X(x), YF, Z(z), 'minecraft:polished_blackstone_brick_wall')
            m.set(X(x), YF + 1, Z(z), 'minecraft:lantern', hanging=False, waterlogged=False)
    m.sign(X(-3), YF + 1, Z(-156), ['Плиты моста', 'встают по очереди.', 'Не мешкайте!'], facing='south')
    # магмовые сталактиты над рекой
    for x in range(-18, 19, 4):
        for z in (-162, -168):
            if h3(x, 0, z, 41) < 0.5:
                m.set(X(x), YF + 12, Z(z), 'minecraft:magma_block')
    st.lore_at([X(-22), YF - 4, Z(-187), X(22), YF + 10, Z(-143)], 'Лавовая река',
               'Плиты моста поднимаются из лавы волной. Упавший сгорит и вернётся на берег — израненным.')


def bellows(m, st):
    lamps, levers = [], []
    for i, x in enumerate((-6, -2, 2, 6)):
        m.set(X(x), YF + 3, Z(-206), 'minecraft:redstone_lamp', lit=False)
        m.set(X(x), YF + 2, Z(-206), 'minecraft:chiseled_polished_blackstone')
        lamps.append([X(x), YF + 3, Z(-206)])
        m.set(X(x), YF, Z(-194), 'minecraft:polished_blackstone')
        m.set(X(x), YF + 1, Z(-194), 'minecraft:lever', face='floor', facing='north', powered=False)
        levers.append([X(x), YF + 1, Z(-194)])
    m.set(X(0), YF - 1, Z(-200), 'minecraft:gilded_blackstone')
    m.set(X(0), YF, Z(-200), 'minecraft:bell', attachment='floor', facing='north', powered=False)
    m.sign(X(-9), YF + 2, Z(-194), ['Ударь в колокол —', 'горны покажут,', 'в каком порядке', 'качать мехи.'], facing='east')
    for x in (-9, 9):
        for z in (-203, -197):
            m.set(X(x), YF, Z(z), 'minecraft:blast_furnace', facing='east' if x < 0 else 'west', lit=True)
    st.gate('s4_seqgate', [X(-2), YF, Z(-207), X(2), YF + 4, Z(-207)], 'minecraft:polished_blackstone_bricks')
    st.pack('s4_seq_amb', (X(0), YF, Z(-200)), [('litrpg:ash_imp', X(-5), YF, Z(-198), 34), ('litrpg:ash_imp', X(5), YF, Z(-198), 34),
                                                ('blaze', X(0), YF + 2, Z(-203), 34)], r=4, ambush=True)
    st.mech('s4_seq', 'sequence', lamps=lamps, levers=levers, show=[X(0), YF, Z(-200)], order=[1, 3, 0, 2, 3, 1], opens=['s4_seqgate'],
            xp=True, solved='Мехи раздули горны — путь к Горнилу открыт!', punish=0.12, ambush=['s4_seq_amb'],
            near=[X(-10), YF, Z(-206), X(10), YF + 6, Z(-193)])


def arena2(m, st):
    # кольцо наковален и горнов, две площадки-горна
    for a in range(12):
        ang = math.pi * 2 * a / 12
        x, z = round(math.cos(ang) * 17), round(math.sin(ang) * 17)
        m.set(X(x), YF, Z(-238 + z), 'minecraft:blast_furnace' if a % 2 else 'minecraft:anvil', facing='north', lit=True)
    for x, z in ((0, -226), (0, -250)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                m.set(X(x + dx), YF - 1, Z(z + dz), 'minecraft:magma_block' if (dx + dz) % 2 else 'minecraft:gilded_blackstone')
    st.gate('s4_a2_in', [X(-2), YF, Z(-215), X(2), YF + 4, Z(-215)], 'minecraft:iron_bars', open_=True)
    st.gate('s4_a2_out', [X(-2), YF, Z(-259), X(2), YF + 4, Z(-259)], 'minecraft:iron_bars')
    st.boss('fire_twins', 'litrpg:fire_twin_ignis', [X(0), YF, Z(-228), 0], [X(-20), YF - 1, Z(-258), X(20), YF + 20, Z(-218)],
            doors=['s4_a2_in'], unlock=['s4_a2_out'], extra={'north': [X(0), YF, Z(-250)]})
    st.lore_at([X(-20), YF - 1, Z(-258), X(20), YF + 12, Z(-218)], 'Горнило близнецов',
               'Игнис и Синдер сильнее рядом друг с другом. Держите их порознь и не пересекайте огненную нить между ними.')


def overseer(m, st):
    for z in range(-287, -271, 5):
        for x in (-7, 6):
            m.set(X(x), YF, Z(z), 'minecraft:blast_furnace', facing='east' if x < 0 else 'west', lit=True)
    st.mech('s4_flameX', 'flames', box=[X(-9), YF, Z(-279), X(9), YF + 2, Z(-277)], period=70, dmg=0.22,
            near=[X(-9), YF, Z(-290), X(9), YF + 6, Z(-269)])
    # тайник: кнопка за наковальней открывает лаз на запад
    st.gate('s4_secret', [X(-10), YF, Z(-282), X(-10), YF + 2, Z(-280)], 'minecraft:polished_blackstone_bricks')
    m.set(X(-8), YF, Z(-284), 'minecraft:anvil', facing='east')
    m.set(X(-9), YF + 1, Z(-285), 'minecraft:polished_blackstone_button', face='wall', facing='east', powered=False)
    st.mech('s4_secretbtn', 'switch', buttons=[[X(-9), YF + 1, Z(-285)]], opens=['s4_secret'], xp=True, solved='Тайный лаз открыт!')
    m.cache(X(-24), YF, Z(-281), tier=2, facing='east')
    st.caches.append([X(-24), YF, Z(-281), 2])
    for x in (-22, -20):
        m.set(X(x), YF, Z(-285), 'minecraft:barrel', facing='up', open=False)
        m.set(X(x), YF, Z(-277), 'minecraft:gold_block')
    m.cache(X(7), YF, Z(-289), tier=1, facing='north')
    st.caches.append([X(7), YF, Z(-289), 1])


def arena3(m, st):
    # пол над лавой: секции, которые обрушит Повелитель
    lava(m, X(-21), YF - 4, Z(-349), X(21), YF - 3, Z(-308))
    sections = []
    for i, (x1, x2) in enumerate(((-21, -8), (-7, 7), (8, 21))):
        for j, (z1, z2) in enumerate(((-349, -336), (-335, -322), (-321, -308))):
            pal = 'minecraft:polished_blackstone' if (i + j) % 2 else 'minecraft:polished_blackstone_bricks'
            for x in range(x1, x2 + 1):
                for z in range(z1, z2 + 1):
                    edge = x in (x1, x2) or z in (z1, z2)
                    m.set(X(x), YF - 1, Z(z), 'minecraft:chiseled_polished_blackstone' if edge else pal)
            if not (i == 1 and j == 1):
                sections.append([X(x1), YF - 1, Z(z1), X(x2), YF - 1, Z(z2)])
    # трон
    for x in range(-3, 4):
        for z in range(-349, -345):
            m.set(X(x), YF, Z(z), 'minecraft:polished_blackstone_bricks')
    m.set(X(0), YF + 1, Z(-348), 'minecraft:polished_blackstone_stairs', facing='south')
    for x in (-2, 2):
        m.set(X(x), YF + 1, Z(-348), 'minecraft:gilded_blackstone')
        m.set(X(x), YF + 2, Z(-348), 'minecraft:fire')
    for z in range(-344, -310, 8):
        for x in (-19, 18):
            m.big_pillar(X(x), Z(z), YF, 18, 'minecraft:polished_blackstone_bricks', trim='minecraft:gilded_blackstone')
    st.gate('s4_a3_in', [X(-2), YF, Z(-306), X(2), YF + 4, Z(-306)], 'minecraft:iron_bars', open_=True)
    spots = []
    for a in range(5):
        ang = math.pi * 2 * a / 5 + 0.3
        spots.append([X(round(math.cos(ang) * 14)), YC, Z(-400 + round(math.sin(ang) * 14))])
    st.boss('ash_lord', 'litrpg:ash_lord', [X(0), YF, Z(-338), 0], [X(-22), YF - 4, Z(-350), X(22), YF + 26, Z(-307)],
            doors=['s4_a3_in'], unlock=['s4_rift'],
            extra={'sections': sections, 'caldera': [X(0), YC, Z(-400)], 'caldera_spots': spots,
                   'caldera_box': [X(-28), YC - 2, Z(-428), X(28), YC + 26, Z(-372)], 'throne': [X(0), YF, Z(-340)]})
    st.lore_at([X(-22), YF - 1, Z(-350), X(22), YF + 14, Z(-307)], 'Трон Повелителя Пепла',
               'Под полом — лава. Повелитель рушит плиты; когда пол кончится — он утянет вас в кальдеру.')


def caldera(m, st):
    cx, cz = 0, -400
    # озеро лавы на уровне пола YC-1, островки
    islands = [(cx, cz, 6)]
    for a in range(5):
        ang = math.pi * 2 * a / 5 + 0.3
        islands.append((round(math.cos(ang) * 14), cz + round(math.sin(ang) * 14), 3))
    for a in range(5):
        ang = math.pi * 2 * a / 5 + 0.3 + math.pi / 5
        islands.append((round(math.cos(ang) * 9), cz + round(math.sin(ang) * 9), 3))
    islands.append((0, cz - 16, 3))     # северный остров — разлом
    for z in range(cz - 15, cz - 5):    # узкая гать к нему
        islands.append((0, z, 1))
    for x in range(-27, 28):
        for z in range(cz - 27, cz + 28):
            if (X(x), YC - 1, Z(z)) not in m.open:
                continue
            on = any(math.hypot(x - ix, z - iz) <= r + 0.4 for ix, iz, r in islands)
            m.set(X(x), YC - 1, Z(z), 'minecraft:basalt' if on else 'minecraft:lava', **({} if on else {'level': 0}))
            if on:
                m.set(X(x), YC - 2, Z(z), 'minecraft:blackstone')
    for ix, iz, r in islands:
        if r > 1:
            m.set(X(ix), YC - 1, Z(iz), 'minecraft:shroomlight' if r < 6 else 'minecraft:gilded_blackstone')
    # разлом на северном острове (заперт до победы)
    st.gate('s4_rift', [X(-2), YC, Z(cz - 18), X(2), YC + 4, Z(cz - 18)], 'minecraft:obsidian')
    for x in range(-1, 2):
        for y in range(YC, YC + 4):
            m.set(X(x), y, Z(cz - 19), 'litrpg:rift')
    for x in (-2, 2):
        for y in range(YC, YC + 5):
            m.set(X(x), y, Z(cz - 19), 'minecraft:crying_obsidian')
    for x in range(-2, 3):
        m.set(X(x), YC + 4, Z(cz - 19), 'minecraft:crying_obsidian')
    st.exit = [X(-1), YC, Z(cz - 20), X(1), YC + 3, Z(cz - 19)]


def packs(st):
    S = st.pack
    S('s4_f1', (X(0), YF, Z(-22)), [('litrpg:salamander', X(-4), YF, Z(-22), 30), ('litrpg:salamander', X(4), YF, Z(-22), 30),
                                    ('litrpg:ash_imp', X(0), YF, Z(-26), 31)], r=16)
    S('s4_f2', (X(0), YF, Z(-35)), [('litrpg:hellhound', X(-3), YF, Z(-35), 31), ('litrpg:hellhound', X(3), YF, Z(-35), 31),
                                    ('litrpg:hellhound', X(0), YF, Z(-38), 31, 'ELITE')], r=16)
    S('s4_f3', (X(0), YF, Z(-48)), [('litrpg:magma_golem', X(0), YF, Z(-48), 32, 'ELITE'), ('litrpg:ash_imp', X(-5), YF, Z(-46), 31),
                                    ('litrpg:ash_imp', X(5), YF, Z(-46), 31)], r=16)
    S('s4_f4', (X(0), YF, Z(-60)), [('blaze', X(-6), YF + 2, Z(-60), 32), ('blaze', X(6), YF + 2, Z(-60), 32), ('litrpg:salamander', X(0), YF, Z(-62), 32)], r=14)
    S('s4_r1', (X(0), YF, Z(-148)), [('litrpg:demon_stalker', X(-4), YF, Z(-148), 34), ('litrpg:hellhound', X(4), YF, Z(-148), 33),
                                     ('litrpg:hellhound', X(0), YF, Z(-150), 33)], r=14)
    S('s4_r2', (X(0), YF, Z(-180)), [('litrpg:magma_golem', X(-6), YF, Z(-180), 35), ('litrpg:salamander', X(6), YF, Z(-180), 34),
                                     ('blaze', X(0), YF + 3, Z(-182), 34)], r=14)
    S('s4_chimera', (X(16), YF, Z(-150)), [('litrpg:chimera', X(16), YF, Z(-150), 35, 'CHAMPION', 'Химера Горнов', 'brute')], r=10, mini=True)
    S('s4_o1', (X(0), YF, Z(-264)), [('litrpg:demon_stalker', X(-1), YF, Z(-264), 37), ('litrpg:demon_stalker', X(1), YF, Z(-266), 37)], r=10)
    S('s4_overseer', (X(0), YF, Z(-282)), [('litrpg:magma_golem', X(0), YF, Z(-282), 38, 'CHAMPION', 'Надсмотрщик Горна', 'guard'),
                                           ('litrpg:ash_imp', X(-5), YF, Z(-286), 37), ('litrpg:ash_imp', X(5), YF, Z(-286), 37)], r=10, mini=True)
    S('s4_t1', (X(0), YF, Z(-298)), [('litrpg:hellhound', X(-1), YF, Z(-298), 38, 'ELITE'), ('blaze', X(1), YF + 2, Z(-300), 38)], r=10)
