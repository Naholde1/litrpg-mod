"""
Этап 2 · Паучья чаща (ур. 10–20). Огромная подземная чаща: гигантские грибы, светящиеся корни, паутина.

  Привал в дупле исполинского пня → Опушка (грибной лес; загадка «Пять жаровен» — зажечь все, пока не погасли)
  → Корневые мосты над пропастью (осыпаются; пауки на паутине) → АРЕНА 1: Логово Матки пауков
  → Гнилое болото (ядовитый туман, островки воздуха; логово Альфа-волка) → Лунное святилище (зеркала и луч)
  → АРЕНА 2: Роща Древня-гнили → Овраг (два рычага по разные стороны — опустить мост)
  → АРЕНА 3: Хижина Ведьмы Тьмы (во второй фазе — Изнанка под хижиной) → Разлом.
"""
import math

from .build import Map, Theme, Stage, apply_gates, h3, noise2
from . import decor as D

OX, OZ = 2048, 0
Y = 64      # привал
YF = 60     # чаща (пол)
YB = 58     # болото


def X(x):
    return OX + x


def Z(z):
    return OZ + z


def build():
    m = Map(seed=2002)
    st = Stage(2, 'thicket', 'Паучья чаща', 'Здесь плетут не только паутину', (10, 20), music='thicket')

    stump = m.theme(Theme('s2_stump', [('minecraft:stripped_dark_oak_wood', 5), ('minecraft:dark_oak_wood', 2)],
                          floor=[('minecraft:rooted_dirt', 2), ('minecraft:moss_block', 1), ('minecraft:dark_oak_planks', 2)],
                          ceil='minecraft:stripped_dark_oak_wood', seed=21))
    cave = m.theme(Theme('s2_cave', [('minecraft:deepslate', 4), ('minecraft:moss_block', 2), ('minecraft:tuff', 2), ('minecraft:cobbled_deepslate', 1)],
                         floor=[('minecraft:moss_block', 5), ('minecraft:rooted_dirt', 2), ('minecraft:coarse_dirt', 1), ('minecraft:mossy_cobblestone', 1)],
                         ceil=[('minecraft:deepslate', 4), ('minecraft:moss_block', 1)], seed=22))
    web = m.theme(Theme('s2_web', [('minecraft:deepslate', 3), ('minecraft:tuff', 2), ('minecraft:smooth_basalt', 1)],
                        floor=[('minecraft:mud', 2), ('minecraft:podzol', 2), ('minecraft:moss_block', 1)], seed=23))
    bog = m.theme(Theme('s2_bog', [('minecraft:mud_bricks', 2), ('minecraft:deepslate', 3), ('minecraft:moss_block', 2)],
                        floor=[('minecraft:mud', 5), ('minecraft:packed_mud', 2), ('minecraft:moss_block', 1)], seed=24))
    moon = m.theme(Theme('s2_moon', [('minecraft:calcite', 3), ('minecraft:smooth_quartz', 1), ('minecraft:diorite', 1)],
                         floor=[('minecraft:polished_diorite', 3), ('minecraft:calcite', 1)], ceil='minecraft:calcite', seed=25))
    grove = m.theme(Theme('s2_grove', [('minecraft:deepslate', 3), ('minecraft:dark_oak_wood', 1), ('minecraft:moss_block', 2)],
                          floor=[('minecraft:podzol', 3), ('minecraft:rooted_dirt', 2), ('minecraft:moss_block', 2)], seed=26))
    under = m.theme(Theme('s2_under', [('minecraft:blackstone', 4), ('minecraft:obsidian', 1), ('minecraft:crying_obsidian', 0.3)],
                          floor=[('minecraft:soul_soil', 3), ('minecraft:blackstone', 2)], ceil='minecraft:blackstone', seed=27))

    # ======================= пустоты =======================
    # дупло-привал
    m.round_room(X(0), Z(8), 9, Y, 9, stump, dome=4)
    m.corridor([(X(0), Z(-1), Y), (X(0), Z(-6), Y), (X(0), Z(-12), YF)], 5, 6, cave)
    # Опушка — грибной лес
    m.cave(X(0), YF + 8, Z(-36), 30, 14, 24, cave, rough=0.3, seed=31, floor=YF)
    # тайное дупло-лаз (запад)
    m.corridor([(X(-28), Z(-30), YF), (X(-40), Z(-34), YF)], 3, 3, stump)
    m.round_room(X(-46), Z(-36), 5, YF, 5, stump, dome=2)
    # Корневые мосты над пропастью (северо-восток)
    m.corridor([(X(22), Z(-56), YF), (X(30), Z(-64), YF)], 5, 6, web)
    m.cave(X(34), YF + 8, Z(-86), 18, 16, 20, web, rough=0.35, seed=32, floor=YF)
    m.pit(X(20), Z(-102), X(48), Z(-70), YF - 1, 40, web)
    # АРЕНА 1 — логово Матки (круглая пещера с высоким сводом)
    m.corridor([(X(34), Z(-104), YF), (X(34), Z(-112), YF)], 5, 6, web)
    m.round_room(X(34), Z(-132), 18, YF, 14, web, dome=8)
    # к болоту (на запад вниз)
    m.corridor([(X(16), Z(-138), YF), (X(8), Z(-140), YF), (X(0), Z(-146), YB)], 5, 6, bog)
    m.cave(X(-14), YB + 6, Z(-168), 32, 10, 24, bog, rough=0.3, seed=33, floor=YB)
    # логово Альфа-волка (юго-запад болота)
    m.cave(X(-48), YB + 4, Z(-150), 10, 6, 9, cave, rough=0.3, seed=34, floor=YB)
    # Лунное святилище
    m.corridor([(X(-14), Z(-192), YB), (X(-14), Z(-200), YB)], 5, 6, moon)
    m.room(X(-28), Z(-224), X(0), Z(-201), YB, 9, moon, vault=4)
    # АРЕНА 2 — Роща Древня
    m.corridor([(X(-14), Z(-225), YB), (X(-14), Z(-232), YB), (X(-14), Z(-240), YF)], 5, 6, grove)
    m.cave(X(-14), YF + 9, Z(-264), 22, 15, 22, grove, rough=0.2, seed=35, floor=YF)
    # Овраг с подъёмным мостом
    m.corridor([(X(-14), Z(-287), YF), (X(-14), Z(-294), YF)], 5, 6, cave)
    m.cave(X(-14), YF + 6, Z(-310), 20, 10, 15, cave, rough=0.3, seed=36, floor=YF)
    m.pit(X(-34), Z(-314), X(6), Z(-306), YF - 1, 40, cave)
    # уступы с рычагами: западный и восточный (к ним — свои тропы)
    m.corridor([(X(-28), Z(-300), YF), (X(-40), Z(-300), YF), (X(-40), Z(-322), YF), (X(-30), Z(-322), YF)], 3, 4, cave)
    m.corridor([(X(0), Z(-300), YF), (X(12), Z(-300), YF), (X(12), Z(-322), YF), (X(2), Z(-322), YF)], 3, 4, cave)
    # АРЕНА 3 — болотная поляна с хижиной Ведьмы
    m.corridor([(X(-14), Z(-325), YF), (X(-14), Z(-334), YF)], 5, 6, bog)
    m.cave(X(-14), YF + 10, Z(-362), 26, 16, 26, bog, rough=0.2, seed=37, floor=YF)
    # Изнанка — под хижиной
    m.round_room(X(-14), Z(-362), 16, YF - 30, 12, under, dome=5)
    # Разлом за поляной
    m.carve(X(-18), YF, Z(-396), X(-10), YF + 8, Z(-388), bog)

    # ======================= отделка =======================
    camp_decor(m)
    glade(m, st)
    bridges(m, st)
    arena1(m, st)
    bogland(m, st)
    moonshrine(m, st)
    arena2(m, st)
    ravine(m, st)
    arena3(m, st)
    packs(st)
    glowing_roots(m)

    st.box = [X(-70), 10, Z(-400), X(60), 110, Z(24)]
    st.camp = [X(0), Y, Z(10), 180]
    st.trader = [X(-5), Y, Z(4), 135]
    st.exit = [X(-17), YF, Z(-395), X(-11), YF + 6, Z(-391)]
    m.auto_lights(spacing=9, soul=False, themes={'s2_stump', 's2_moon'}, min_h=5)
    m.glow(spacing=5, level=9)
    apply_gates(m, st)
    m.shell(thick=2)
    return m, st


# ---------------------------------------------------------------- отделка

def mushroom(m, x, y, z, h, r, cap='minecraft:red_mushroom_block', glow=False):
    """Исполинский гриб: ножка и шляпка-купол."""
    for yy in range(y, y + h):
        m.set(x, yy, z, 'minecraft:mushroom_stem')
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            d = math.hypot(dx, dz)
            if d <= r + 0.3:
                yy = y + h + (1 if d < r - 1.2 else 0)
                m.set(x + dx, yy, z + dz, cap)
                if d > r - 1.2:
                    m.set(x + dx, y + h - 1, z + dz, cap)
    if glow:
        m.set(x, y + h - 1, z, 'minecraft:shroomlight')


def tree(m, x, y, z, h, leaves='minecraft:azalea_leaves', log='minecraft:dark_oak_log', r=3):
    for yy in range(y, y + h):
        m.set(x, yy, z, log, axis='y')
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dy in range(-1, 2):
                if math.hypot(dx, dz) + abs(dy) * 1.2 <= r + 0.5 and h3(x + dx, y + dy, z + dz, 3) > 0.15:
                    m.set(x + dx, y + h + dy, z + dz, leaves, persistent=True, distance=1, waterlogged=False)


def camp_decor(m):
    m.set(X(0), Y, Z(10), 'minecraft:campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
    for dz in (6, 14):
        for dx in range(-2, 3):
            m.set(X(dx), Y, Z(dz), 'minecraft:stripped_spruce_log', axis='x')
    for dz in range(1, 7):
        m.set(X(-7), Y, Z(dz), 'minecraft:spruce_planks')
        m.slab(X(-7), Y + 1, Z(dz), 'minecraft:spruce_slab')
    for i in range(6):
        a = math.pi * 2 * i / 6
        m.set(X(round(math.cos(a) * 8)), Y + 3, Z(8 + round(math.sin(a) * 8)), 'minecraft:shroomlight')
    m.sign(X(0), Y + 2, Z(16), ['ПРИВАЛ', 'в дупле Древа', 'Отдых. Торговец.'], facing='north')
    D.cobwebs(m, X(-9), Y + 5, Z(-1), X(9), Y + 9, Z(17), chance=0.15, seed=1)


def glade(m, st):
    # исполинские грибы и деревья-азалии по опушке
    spots = [(-20, -28, 10, 4, 'minecraft:red_mushroom_block'), (18, -24, 8, 3, 'minecraft:brown_mushroom_block'),
             (-12, -50, 12, 5, 'minecraft:brown_mushroom_block'), (14, -46, 9, 4, 'minecraft:red_mushroom_block'),
             (0, -20, 7, 3, 'minecraft:red_mushroom_block'), (-24, -44, 7, 3, 'minecraft:brown_mushroom_block')]
    for x, z, hh, r, cap in spots:
        mushroom(m, X(x), YF, Z(z), hh, r, cap, glow=True)
    for x, z in ((8, -34), (-6, -40), (22, -38), (-18, -18)):
        tree(m, X(x), YF, Z(z), 6)
    # жаровни-костры по краям поляны — зажечь все пять за 12 с
    braz = []
    for x, z in ((-26, -34), (-16, -54), (10, -56), (26, -30), (0, -14)):
        m.set(X(x), YF, Z(z), 'minecraft:mossy_stone_bricks')
        m.set(X(x), YF + 1, Z(z), 'minecraft:campfire', lit=False, facing='north', signal_fire=False, waterlogged=False)
        braz.append([X(x), YF + 1, Z(z)])
    m.sign(X(4), YF + 1, Z(-14), ['Пять огней', 'отгоняют тьму.', 'Зажгите все разом', '(ПКМ по костру)'], facing='south', wall=False)
    st.gate('s2_rootgate', [X(20), YF, Z(-58), X(24), YF + 4, Z(-58)], 'minecraft:dark_oak_fence[north=true,south=true,waterlogged=false]')
    st.mech('s2_braziers', 'braziers', braziers=braz, burn=240, opens=['s2_rootgate'], xp=True, solved='Огни разгорелись — корни расступились',
            near=[X(-30), YF, Z(-60), X(30), YF + 10, Z(-12)])
    # тайное дупло: мимик и тайник
    for z in range(-32, -29):
        for y in range(YF, YF + 3):
            m.set(X(-29), y, Z(z), 'minecraft:infested_cracked_stone_bricks')
    m.cache(X(-48), YF, Z(-37), tier=2, facing='east')
    st.caches.append([X(-48), YF, Z(-37), 2])
    st.mech('s2_secret1', 'area', box=[X(-51), YF, Z(-41), X(-41), YF + 4, Z(-31)], xp=True, solved='Тайное дупло найдено')
    D.cobwebs(m, X(-30), YF + 6, Z(-60), X(30), YF + 22, Z(-12), chance=0.08, seed=2)


def bridges(m, st):
    # два корневых моста через пропасть: прямой (с осыпью) и в обход
    for z in range(-102, -69):
        for x in (33, 34, 35):
            crack = -94 <= z <= -86
            m.set(X(x), YF - 1, Z(z), 'minecraft:cracked_stone_bricks' if crack else 'minecraft:mangrove_roots', waterlogged=False)
    for x in range(21, 34):
        m.set(X(x), YF - 1, Z(-80), 'minecraft:mangrove_roots', waterlogged=False)
    for z in range(-100, -79):
        m.set(X(21), YF - 1, Z(z), 'minecraft:mangrove_roots', waterlogged=False)
    for x in range(21, 34):
        m.set(X(x), YF - 1, Z(-100), 'minecraft:mangrove_roots', waterlogged=False)
    st.mech('s2_collapse', 'collapse', box=[X(33), YF - 1, Z(-94), X(35), YF - 1, Z(-86)], tile='minecraft:cracked_stone_bricks',
            near=[X(18), YF - 1, Z(-104), X(50), YF + 10, Z(-68)])
    # паутина-сети над пропастью
    for i in range(30):
        x, z = 22 + int(h3(i, 1, 2) * 26), -100 + int(h3(i, 3, 4) * 30)
        m.set(X(x), YF + 8 + int(h3(i, 5, 6) * 6), Z(z), 'minecraft:cobweb')
    st.lore_at([X(18), YF - 1, Z(-104), X(50), YF + 10, Z(-68)], 'Корневые мосты', 'Прямой путь короче, но корни под ногами гнилые.')


def arena1(m, st):
    cx, cz = 34, -132
    # коконы на стенах и паутина у свода
    D.cobwebs(m, X(cx - 18), YF + 8, Z(cz - 18), X(cx + 18), YF + 22, Z(cz + 18), chance=0.35, seed=4)
    sacs = []
    for i in range(4):
        a = math.pi / 4 + math.pi / 2 * i
        x, z = round(cx + math.cos(a) * 15), round(cz + math.sin(a) * 15)
        m.set(X(x), YF, Z(z), 'minecraft:mud')
        m.set(X(x), YF + 1, Z(z), 'litrpg:relic', kind='web')
        sacs.append([X(x), YF + 1, Z(z)])
    for i in range(10):
        a = math.pi * 2 * i / 10
        x, z = round(cx + math.cos(a) * 11), round(cz + math.sin(a) * 11)
        m.set(X(x), YF, Z(z), 'minecraft:cobweb')
    st.gate('s2_a1_in', [X(32), YF, Z(-113), X(36), YF + 4, Z(-113)], 'minecraft:cobweb', open_=True)
    st.gate('s2_a1_out', [X(14), YF, Z(-140), X(14), YF + 4, Z(-136)], 'minecraft:mud_brick_wall[up=true,north=none,south=none,east=none,west=none,waterlogged=false]')
    m.carve(X(14), YF, Z(-140), X(16), YF + 4, Z(-136), m.themes['s2_web'])
    st.boss('spider_queen', 'litrpg:spider_queen', [X(cx), YF, Z(cz - 6), 0], [X(cx - 18), YF - 1, Z(cz - 18), X(cx + 18), YF + 22, Z(cz + 18)],
            doors=['s2_a1_in'], unlock=['s2_a1_out'],
            extra={'ceiling': [X(cx), YF + 16, Z(cz)], 'sacs': sacs})
    st.lore_at([X(cx - 17), YF - 1, Z(cz - 17), X(cx + 17), YF + 10, Z(cz + 17)], 'Логово Матки',
               'Мешки с яйцами у стен — если их не разбить, детёныши будут вылупляться без конца.')


def bogland(m, st):
    # вода-лужи и островки, туман
    for x in range(-46, 18):
        for z in range(-190, -146):
            n = noise2(x, z, 7, 41)
            if n < 0.38 and m.is_open(X(x), YB, Z(z)) and (X(x), YB - 1, Z(z)) not in m.open:
                m.set(X(x), YB - 1, Z(z), 'minecraft:water', level=0)
            elif n > 0.72 and m.is_open(X(x), YB, Z(z)):
                m.set(X(x), YB, Z(z), 'minecraft:moss_carpet')
    safes = []
    for x, z in ((-30, -160), (-10, -172), (6, -158), (-24, -182), (4, -184)):
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if dx * dx + dz * dz <= 5:
                    m.set(X(x + dx), YB, Z(z + dz), 'minecraft:packed_mud')
        m.set(X(x), YB + 1, Z(z), 'minecraft:mangrove_log', axis='y')
        m.set(X(x), YB + 2, Z(z), 'minecraft:mangrove_log', axis='y')
        m.set(X(x), YB + 3, Z(z), 'minecraft:shroomlight')
        safes.append([X(x - 2), YB + 1, Z(z - 2), X(x + 2), YB + 4, Z(z + 2)])
    st.mech('s2_gas', 'gas', box=[X(-44), YB, Z(-188), X(16), YB + 4, Z(-150)], safe=safes, dmg=0.035,
            near=[X(-44), YB, Z(-188), X(16), YB + 4, Z(-150)])
    for x in range(-40, 14, 6):
        tree(m, X(x), YB, Z(-150 - (x * 7) % 30), 5, leaves='minecraft:mangrove_leaves', log='minecraft:mangrove_log', r=2)
    st.lore_at([X(-44), YB - 1, Z(-190), X(16), YB + 8, Z(-146)], 'Гнилое болото',
               'Туман ядовит. Дышать можно только на сухих островках у светящихся пней.')
    m.cache(X(-52), YB, Z(-151), tier=1, facing='east')
    st.caches.append([X(-52), YB, Z(-151), 1])


def moonshrine(m, st):
    # излучатель — фонарь в западной стене, цель — лунный камень у северной; 4 поворотных зеркала
    ey = YB + 1
    m.set(X(-28), ey, Z(-212), 'minecraft:sea_lantern')
    target = [X(-4), ey, Z(-224)]
    m.set(target[0], target[1], target[2], 'minecraft:amethyst_block')
    mirrors = [[X(-20), ey, Z(-212)], [X(-20), ey, Z(-206)], [X(-10), ey, Z(-206)], [X(-10), ey, Z(-218)], [X(-4), ey, Z(-218)]]
    for mx_, my_, mz_ in mirrors:
        m.set(mx_, my_ - 1, mz_, 'minecraft:chiseled_quartz_block')
        m.set(mx_, my_, mz_, 'litrpg:mirror', turn=0)
    # решение: луч на восток → (−20,−212) «\» вверх-север … проверено вручную: turn = [1,0,0,1,0]
    st.gate('s2_moongate', [X(-16), YB, Z(-225), X(-12), YB + 4, Z(-225)], 'minecraft:calcite')
    m.carve(X(-16), YB, Z(-226), X(-12), YB + 4, Z(-225), m.themes['s2_moon'])
    st.mech('s2_beam', 'beam', emitter=[X(-28), ey, Z(-212)], dir='east', target=target, mirrors=mirrors, start=[0, 1, 1, 0, 1],
            opens=['s2_moongate'], xp=True, solved='Лунный луч коснулся камня — дверь Рощи открыта',
            near=[X(-28), YB, Z(-224), X(0), YB + 8, Z(-201)])
    m.sign(X(-14), YB + 2, Z(-201), ['Направь свет луны', 'на аметист.', 'Зеркала вращаются', '(ПКМ)'], facing='south')
    for x in (-26, -2):
        for z in (-222, -203):
            m.pillar(X(x), Z(z), YB, 8, 'minecraft:quartz_pillar')


def arena2(m, st):
    cx, cz = -14, -264
    # мёртвые деревья по кругу, гнилые пни
    for i in range(9):
        a = math.pi * 2 * i / 9
        x, z = round(cx + math.cos(a) * 16), round(cz + math.sin(a) * 16)
        tree(m, X(x), YF, Z(z), 7, leaves='minecraft:dark_oak_leaves', log='minecraft:dark_oak_log', r=2)
    saplings = []
    for i in range(6):
        a = math.pi * 2 * i / 6 + 0.3
        x, z = round(cx + math.cos(a) * 9), round(cz + math.sin(a) * 9)
        m.set(X(x), YF - 1, Z(z), 'minecraft:rooted_dirt')
        saplings.append([X(x), YF, Z(z)])
    st.gate('s2_a2_in', [X(-16), YF, Z(-241), X(-12), YF + 4, Z(-241)], 'minecraft:dark_oak_fence[north=true,south=true,waterlogged=false]',
            open_=True)
    st.gate('s2_a2_out', [X(-16), YF, Z(-287), X(-12), YF + 4, Z(-287)], 'minecraft:dark_oak_fence[north=true,south=true,waterlogged=false]')
    st.boss('rot_ent', 'litrpg:rot_ent', [X(cx), YF, Z(cz - 4), 0], [X(cx - 22), YF - 1, Z(cz - 22), X(cx + 22), YF + 24, Z(cz + 22)],
            doors=['s2_a2_in'], unlock=['s2_a2_out'], extra={'saplings': saplings})
    st.lore_at([X(cx - 21), YF - 1, Z(cz - 21), X(cx + 21), YF + 10, Z(cz + 21)], 'Роща Древня-гнили',
               'Древень сажает ростки — каждый, что успеет вырасти, станет новым древнем. Огонь жжёт его кору.')


def ravine(m, st):
    # подъёмный мост (появится, когда оба рычага дёрнут почти разом)
    st.gate('s2_bridge', [X(-16), YF - 1, Z(-314), X(-12), YF - 1, Z(-306)], 'minecraft:dark_oak_planks', open_=True)
    levers = []
    for x in (-34, 6):
        m.set(X(x), YF, Z(-322), 'minecraft:mossy_cobblestone')
        m.set(X(x), YF + 1, Z(-322), 'minecraft:lever', face='floor', facing='north', powered=False)
        levers.append([X(x), YF + 1, Z(-322)])
    st.mech('s2_split', 'split', levers=levers, window=40, closes=['s2_bridge'], xp=True,
            solved='Ворот скрипнул — мост через овраг опущен', near=[X(-40), YF, Z(-326), X(12), YF + 6, Z(-296)])
    m.sign(X(-14), YF + 1, Z(-299), ['Мост поднят.', 'Рычаги — за оврагом', 'слева и справа.', 'Дёрните ВМЕСТЕ'], facing='south', wall=False)


def arena3(m, st):
    cx, cz = -14, -362
    # хижина ведьмы на сваях посреди поляны
    for x in range(-5, 6):
        for z in range(-5, 6):
            m.set(X(cx + x), YF + 2, Z(cz + z), 'minecraft:dark_oak_planks')
    for x, z in ((-5, -5), (5, -5), (-5, 5), (5, 5)):
        for y in range(YF, YF + 2):
            m.set(X(cx + x), y, Z(cz + z), 'minecraft:dark_oak_log', axis='y')
    for x in range(-5, 6):
        for z in range(-5, 6):
            if abs(x) == 5 or abs(z) == 5:
                for y in range(YF + 3, YF + 7):
                    if not (z == 5 and abs(x) <= 1 and y < YF + 6):
                        m.set(X(cx + x), y, Z(cz + z), 'minecraft:spruce_planks' if y < YF + 6 else 'minecraft:dark_oak_planks')
    for i in range(7):
        for x in range(-6 + i, 7 - i):
            for z in (-6 + i, 6 - i):
                m.set(X(cx + x), YF + 7 + i, Z(cz + z), 'minecraft:dark_oak_stairs', facing='south' if z > 0 else 'north', half='bottom',
                      shape='straight', waterlogged=False)
        for z in range(-6 + i, 7 - i):
            m.set(X(cx - 6 + i), YF + 7 + i, Z(cz + z), 'minecraft:dark_oak_planks')
            m.set(X(cx + 6 - i), YF + 7 + i, Z(cz + z), 'minecraft:dark_oak_planks')
    for x in (-1, 0, 1):
        m.stairs(X(cx + x), YF + 2, Z(cz + 6), 'minecraft:dark_oak_stairs', 'north')
        m.stairs(X(cx + x), YF + 1, Z(cz + 7), 'minecraft:dark_oak_stairs', 'north')
        m.stairs(X(cx + x), YF, Z(cz + 8), 'minecraft:dark_oak_stairs', 'north')
    m.set(X(cx), YF + 3, Z(cz), 'minecraft:cauldron')
    m.set(X(cx), YF + 2, Z(cz), 'minecraft:magma_block')
    # Изнанка: колодцы света (безопасные места) и тени
    wells = []
    uy = YF - 30
    for i in range(5):
        a = math.pi * 2 * i / 5
        x, z = round(cx + math.cos(a) * 10), round(cz + math.sin(a) * 10)
        m.set(X(x), uy - 1, Z(z), 'minecraft:gilded_blackstone')
        m.set(X(x), uy, Z(z), 'minecraft:soul_campfire', lit=True, facing='north', signal_fire=False, waterlogged=False)
        wells.append([X(x), uy, Z(z)])
    st.gate('s2_a3_in', [X(-16), YF, Z(-335), X(-12), YF + 4, Z(-335)], 'minecraft:mud_brick_wall[up=true,north=none,south=none,east=none,west=none,waterlogged=false]',
            open_=True)
    st.gate('s2_rift', [X(-18), YF, Z(-388), X(-10), YF + 6, Z(-388)], 'minecraft:mud_bricks')
    for x in range(-17, -10):
        for y in range(YF, YF + 7):
            m.set(X(x), y, Z(-395), 'litrpg:rift')
    st.boss('dark_witch', 'litrpg:dark_witch', [X(cx), YF + 3, Z(cz - 2), 0], [X(cx - 26), YF - 1, Z(cz - 26), X(cx + 26), YF + 26, Z(cz + 26)],
            doors=['s2_a3_in'], unlock=['s2_rift'],
            extra={'under': [X(cx), uy, Z(cz)], 'under_box': [X(cx - 16), uy - 1, Z(cz - 16), X(cx + 16), uy + 16, Z(cz + 16)], 'wells': wells,
                   'hut': [X(cx), YF + 3, Z(cz)]})
    st.lore_at([X(cx - 25), YF - 1, Z(cz - 25), X(cx + 25), YF + 12, Z(cz + 25)], 'Хижина Ведьмы Тьмы',
               'Ведьма утянет вас в Изнанку. Там свет — единственная защита: держитесь у огней.')


def glowing_roots(m):
    """Светящиеся лозы с потолка пещер (атмосфера)."""
    n = 0
    for (x, y, z), t in list(m.open.items()):
        if t not in ('s2_cave', 's2_web', 's2_grove', 's2_bog') or (x, y + 1, z) in m.open or h3(x, y, z, 7) > 0.025:
            continue
        ln = 2 + int(h3(z, x, y, 8) * 5)
        for i in range(ln):
            if (x, y - i, z) not in m.open or m.solid_at(x, y - i, z):
                break
            berries = i == ln - 1 or h3(x, y - i, z, 9) < 0.3
            m.set(x, y - i, z, 'minecraft:cave_vines' if i == ln - 1 else 'minecraft:cave_vines_plant', berries=berries,
                  **({'age': 25} if i == ln - 1 else {}))
        n += 1
    return n


def packs(st):
    S = st.pack
    # Опушка
    S('s2_g1', (X(0), YF, Z(-26)), [('spider', X(-4), YF, Z(-26), 10), ('spider', X(4), YF, Z(-26), 10), ('litrpg:forest_wisp', X(0), YF + 2, Z(-30), 11)], r=16)
    S('s2_g2', (X(-14), YF, Z(-42)), [('litrpg:dire_wolf', X(-16), YF, Z(-40), 11), ('litrpg:dire_wolf', X(-12), YF, Z(-44), 11),
                                      ('litrpg:dire_wolf', X(-14), YF, Z(-38), 11, 'ELITE')], r=16)
    S('s2_g3', (X(14), YF, Z(-44)), [('litrpg:weaver_spider', X(14), YF, Z(-44), 12, 'ELITE'), ('cave_spider', X(11), YF, Z(-41), 11),
                                     ('cave_spider', X(17), YF, Z(-41), 11)], r=14)
    S('s2_g4', (X(0), YF, Z(-52)), [('litrpg:treant', X(-3), YF, Z(-52), 12), ('litrpg:treant', X(3), YF, Z(-52), 12)], r=10)
    S('s2_mimic', (X(-46), YF, Z(-36)), [('litrpg:mimic', X(-45), YF, Z(-34), 12, 'ELITE')], r=6)
    # Мосты
    S('s2_b1', (X(34), YF, Z(-86)), [('litrpg:weaver_spider', X(30), YF + 10, Z(-90), 12), ('litrpg:weaver_spider', X(38), YF + 10, Z(-84), 12),
                                     ('spider', X(34), YF, Z(-74), 12), ('spider', X(22), YF, Z(-90), 12)], r=18)
    S('s2_b2', (X(34), YF, Z(-106)), [('litrpg:weaver_spider', X(34), YF, Z(-107), 13, 'ELITE'), ('cave_spider', X(32), YF, Z(-106), 13),
                                      ('cave_spider', X(36), YF, Z(-106), 13)], r=10)
    # Болото
    S('s2_m1', (X(0), YB, Z(-158)), [('litrpg:giant_toad', X(0), YB, Z(-158), 14), ('litrpg:bog_lurker', X(-4), YB, Z(-162), 14),
                                     ('litrpg:jelly', X(4), YB, Z(-160), 13)], r=16)
    S('s2_m2', (X(-22), YB, Z(-170)), [('litrpg:bog_lurker', X(-22), YB, Z(-170), 15, 'ELITE'), ('litrpg:giant_toad', X(-26), YB, Z(-166), 14),
                                       ('witch', X(-18), YB, Z(-174), 14)], r=16)
    S('s2_m3', (X(-6), YB, Z(-184)), [('litrpg:jelly', X(-8), YB, Z(-184), 15), ('litrpg:jelly', X(-4), YB, Z(-184), 15),
                                      ('litrpg:giant_toad', X(-6), YB, Z(-180), 15, 'ELITE')], r=14)
    S('s2_alpha', (X(-48), YB, Z(-150)), [('litrpg:dire_wolf', X(-48), YB, Z(-152), 16, 'CHAMPION', 'Альфа Серая Грива', 'swift'),
                                          ('litrpg:dire_wolf', X(-45), YB, Z(-148), 15), ('litrpg:dire_wolf', X(-51), YB, Z(-148), 15)],
      r=12, mini=True)
    # перед Рощей
    S('s2_s1', (X(-14), YB, Z(-206)), [('litrpg:shadow_panther', X(-18), YB, Z(-208), 16), ('litrpg:forest_wisp', X(-10), YB + 2, Z(-206), 16)], r=12)
    # после Рощи / овраг
    S('s2_r1', (X(-14), YF, Z(-300)), [('litrpg:treant', X(-18), YF, Z(-298), 17, 'ELITE'), ('spider', X(-10), YF, Z(-298), 17),
                                       ('spider', X(-14), YF, Z(-302), 17)], r=14)
    S('s2_r2', (X(-34), YF, Z(-318)), [('litrpg:weaver_spider', X(-34), YF, Z(-318), 17), ('cave_spider', X(-36), YF, Z(-320), 17)], r=8)
    S('s2_r3', (X(6), YF, Z(-318)), [('litrpg:shadow_panther', X(6), YF, Z(-318), 17), ('cave_spider', X(8), YF, Z(-320), 17)], r=8)
    S('s2_r4', (X(-14), YF, Z(-330)), [('litrpg:scarecrow', X(-17), YF, Z(-330), 18, 'ELITE'), ('witch', X(-11), YF, Z(-330), 18)], r=10)
    S('s2_shade', (X(-14), YF, Z(-320)), [('litrpg:shadow_panther', X(-14), YF, Z(-318), 18, 'CHAMPION', 'Безлунная', 'shade'),
                                          ('litrpg:shadow_panther', X(-16), YF, Z(-322), 17)], r=8, mini=True)
