"""
«Мир Системы» — карта 10000×10000 блоков с территориями народов.
Пишет:
  src/main/resources/litrpg_world.json        — описание карты (его читает Java: world/map/WorldMap)
  data/litrpg/worldgen/biome/*.json           — свои биомы (на основе ванильных из run/dump)
  data/litrpg/worldgen/noise_settings/*.json  — рельеф (ванильный + наша форма карты)
  data/litrpg/worldgen/world_preset/system_world.json и тег для списка типов мира
  теги биомов, названия, карта-свиток (painting + атлас)
Запуск: python tools/gen_world.py [--art]    (--art: перерисовать карту-свиток, ~1 мин)
Нужна выгрузка ванильных данных: run/dump (dev-команда devdump).
"""
import copy
import json
import math
import random
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from worldmath import World, vn, poly_dist, sstep  # noqa: E402
from png import write_png  # noqa: E402

TOOLS = os.path.dirname(__file__)
RES = os.path.join(TOOLS, '..', 'src', 'main', 'resources')
DATA = os.path.join(RES, 'data')
ASSETS = os.path.join(RES, 'assets', 'litrpg')
DUMP = os.path.join(TOOLS, '..', 'run', 'dump')

# =====================================================================
#                         ОПИСАНИЕ КАРТЫ
# =====================================================================


def P(base, amp, cell, ridge, tame):
    return dict(base=base, amp=amp, cell=cell, ridge=ridge, tame=tame)


TERRITORIES = [
    dict(id='kingdom', name='Королевство Аркадия', faction='kingdom', color=0xE8C766, level_bonus=0,
         seeds=[[0, 0, 1450], [700, -1150, 750], [-1000, 1150, 750], [1100, 650, 600], [-1100, -350, 600]],
         profile=P(70, 12, 320, 0.0, 0.62),
         biomes=[['litrpg:arcadia_fields', 3], ['litrpg:royal_meadow', 2], ['litrpg:oak_dales', 3]], cold=False),
    dict(id='elves', name='Сильвендар', faction='elves', color=0x7BC86A, level_bonus=0,
         seeds=[[-2800, -350, 1250], [-2300, -1350, 800], [-3550, 650, 750]],
         profile=P(74, 16, 260, 0.0, 0.55),
         biomes=[['litrpg:sylvan_wildwood', 4], ['litrpg:moon_grove', 3], ['litrpg:silver_glade', 2]], cold=False),
    dict(id='dwarves', name='Подгорье Кхазад', faction='dwarves', color=0xB08A5A, level_bonus=0,
         seeds=[[2700, -2550, 1250], [3700, -1700, 700], [1700, -3300, 700]],
         profile=P(104, 40, 230, 1.3, 0.62),
         biomes=[['litrpg:iron_peaks', 4], ['litrpg:ore_slopes', 3], ['litrpg:dwarf_vales', 2]], cold=True),
    dict(id='orcs', name='Степи Орды', faction='orcs', color=0xD07A3A, level_bonus=0,
         seeds=[[2900, 2600, 1250], [3800, 1500, 650], [1900, 3550, 700]],
         profile=P(73, 16, 210, 0.3, 0.6),
         biomes=[['litrpg:scorched_steppe', 4], ['litrpg:red_canyons', 2], ['litrpg:dust_wastes', 2]], cold=False),
    dict(id='beastkin', name='Земли Клыка', faction='beastkin', color=0x9A7A50, level_bonus=0,
         seeds=[[-2600, -3050, 1050], [-1300, -3500, 700], [-3600, -2200, 700]],
         profile=P(82, 24, 250, 0.25, 0.55),
         biomes=[['litrpg:fang_taiga', 4], ['litrpg:snow_hunt', 3]], cold=True),
    dict(id='demonkin', name='Пепельные Пустоши', faction='demonkin', color=0x9A3040, level_bonus=6,
         seeds=[[-2850, 2950, 1200], [-3700, 1900, 700], [-1800, 3800, 650]],
         profile=P(70, 16, 200, 0.12, 0.6),
         biomes=[['litrpg:ash_wastes', 4], ['litrpg:charred_forest', 3], ['litrpg:smolder_craters', 2]], cold=False),
    dict(id='goblins', name='Гоблинские Топи', faction='goblins', color=0x5E8A3A, level_bonus=2,
         seeds=[[150, 3400, 900], [-850, 3750, 600], [900, 3800, 500]],
         profile=P(64.5, 3, 150, 0.0, 0.82),
         biomes=[['litrpg:rot_swamp', 3], ['litrpg:goblin_mangroves', 2]], cold=False),
    dict(id='frost', name='Ледяной Предел', faction=None, color=0xCFE8F5, level_bonus=6,
         seeds=[[200, -3950, 1050], [1350, -4050, 700], [-700, -4100, 500]],
         profile=P(92, 32, 250, 0.6, 0.55),
         biomes=[['litrpg:frozen_reach', 3], ['litrpg:glacier_spikes', 2], ['litrpg:frost_peaks', 2]], cold=True),
    dict(id='deadlands', name='Мёртвые Земли', faction=None, color=0x7A7A70, level_bonus=6,
         seeds=[[3950, 250, 720], [3400, -750, 520], [4100, 900, 450]],
         profile=P(76, 18, 220, 0.2, 0.55),
         biomes=[['litrpg:blight', 3], ['litrpg:bone_wastes', 2]], cold=False),
]

WILD = dict(id='wild', name='Пограничье', profile=P(76, 22, 280, 0.25, 0.35))

# ---- вода ----
SEAS = [[2080, 1380, 640, 28]]                     # Изумрудный залив
LAKES = [[-3150, -1080, 230, 44], [-700, 930, 150, 50], [900, -3650, 250, 42],
         [-2050, 1250, 170, 48], [3150, 450, 140, 50], [-1650, -2600, 160, 46]]
RIVERS = [
    # Великая Серебрянка: от озера Сильвендара мимо севера Аркадии в Изумрудный залив
    dict(name='Серебрянка', width=48, meander=110, points=[[-3060, -1000], [-2500, -900], [-1800, -760], [-1150, -640],
                                                            [-500, -470], [100, -420], [700, -150], [1150, 330], [1550, 760], [1820, 1080]]),
    dict(name='Рудная', width=30, meander=70, points=[[2350, -1900], [2150, -1350], [1950, -800], [1780, -200], [1650, 400], [1700, 900]]),
    dict(name='Волчья', width=26, meander=80, points=[[-1640, -2470], [-1500, -1900], [-1320, -1350], [-1180, -900], [-1150, -650]]),
    dict(name='Ледяная', width=26, meander=70, points=[[860, -3420], [800, -2800], [720, -2100], [640, -1400], [560, -800], [450, -430]]),
    dict(name='Топкая', width=34, meander=90, points=[[-700, 1080], [-520, 1600], [-260, 2150], [-120, 2900], [100, 3500], [260, 4250]]),
    dict(name='Кровавая', width=30, meander=90, points=[[3220, 600], [3420, 1300], [3560, 2100], [3600, 2900], [3520, 3700], [3420, 4300]]),
    dict(name='Пепельная', width=22, meander=60, points=[[-2050, 1420], [-2350, 1900], [-2200, 2400], [-2420, 3300], [-2300, 4300]]),
]


# ---- города: x, z, высота площадки, радиус охраны, базовый уровень округи ----
CITIES = [
    dict(id='arcadia', name='Аркадия', faction='kingdom', x=0, z=0, y=72, r=150, flat=185, level=1, guard='human_guard',
         desc='Столица Королевства. Здесь начинается путь каждого Избранного.'),
    dict(id='haven', name='Солнечная Гавань', faction='kingdom', x=1535, z=925, y=68, r=100, flat=52, level=3, guard='human_guard',
         desc='Портовый город на берегу Изумрудного залива.'),
    dict(id='sylvendar', name='Сильвендар', faction='elves', x=-2800, z=-380, y=76, r=100, flat=96, level=7, guard='elf_sentinel',
         desc='Древний город эльфов среди исполинских деревьев.'),
    dict(id='khazad', name='Кхазад-Бар', faction='dwarves', x=2700, z=-2560, y=168, r=100, flat=96, level=10, guard='dwarf_ironguard',
         desc='Твердыня дварфов, высеченная в сердце гор.'),
    dict(id='ghardum', name="Гхар'Дум", faction='orcs', x=2900, z=2620, y=90, r=100, flat=92, level=12, guard='orc_bloodguard',
         desc='Военный город Орды за стенами из брёвен и костей.'),
    dict(id='wolfden', name='Стойбище Белого Волка', faction='beastkin', x=-2600, z=-3040, y=86, r=80, flat=72, level=9, guard='beast_fangguard',
         desc='Сердце земель зверолюдов.'),
    dict(id='ashgrad', name='Пеплград', faction='demonkin', x=-2850, z=2950, y=72, r=100, flat=96, level=16, guard='demon_hellguard',
         desc='Город демонидов среди пепла и лавы.'),
    dict(id='dawnfort', name='Форт Рассвет', faction='kingdom', x=-80, z=2560, y=70, r=70, flat=64, level=10, guard='human_guard',
         desc='Пограничная крепость Королевства у Гоблинских топей.'),
    dict(id='northwatch', name='Северный Дозор', faction='kingdom', x=350, z=-3250, y=96, r=70, flat=64, level=20, guard='human_guard',
         desc='Застава у Ледяного Предела. Отсюда уходят смельчаки на север.'),
    dict(id='blackbastion', name='Чёрный Бастион', faction='kingdom', x=3150, z=-350, y=84, r=70, flat=64, level=18, guard='human_guard',
         desc='Последний оплот живых у Мёртвых Земель.'),
]

LEVELS = dict(slope=40, max=60)

# ---- дороги (точки) ----
ROADS = [
    dict(name='Западный тракт', style='stone', points=[[-130, 0], [-650, -120], [-1350, -150], [-2100, -260], [-2800, -380]]),
    dict(name='Северная тропа', style='dirt', points=[[-1350, -150], [-1500, -900], [-1750, -1700], [-2200, -2400], [-2600, -3040]]),
    dict(name='Горный тракт', style='stone', points=[[0, -130], [150, -500], [700, -1050], [1500, -1700], [2150, -2150], [2700, -2560]]),
    dict(name='Портовый тракт', style='stone', points=[[130, 0], [600, 260], [1050, 600], [1535, 925]]),
    dict(name='Степной путь', style='dirt', points=[[1535, 925], [1300, 1700], [1700, 2250], [2300, 2500], [2900, 2620]]),
    dict(name='Южный тракт', style='stone', points=[[0, 130], [-200, 700], [-350, 1500], [-200, 2100], [-80, 2560]]),
    dict(name='Пепельный путь', style='dirt', points=[[-350, 1500], [-1100, 2000], [-1900, 2500], [-2850, 2950]]),
    dict(name='Дозорная тропа', style='dirt', points=[[150, -500], [100, -1500], [250, -2400], [350, -3250]]),
    dict(name='Восточный тракт', style='stone', points=[[1535, 925], [2200, 400], [2700, -50], [3150, -350]]),
    dict(name='Топкая гать', style='dirt', points=[[-80, 2560], [150, 3000], [300, 3350]]),
]


# =====================================================================
#                         БИОМЫ
# =====================================================================
# id: (имя ru, имя en, ванильный образец, правки)

BIOMES = {
    # Королевство
    'arcadia_fields': ('Златые поля', 'Golden Fields', 'sunflower_plains', dict(grass=0x9DC855, foliage=0x8CB848)),
    'royal_meadow': ('Королевские луга', 'Royal Meadows', 'meadow', dict(temperature=0.7)),
    'oak_dales': ('Дубравы Аркадии', 'Arcadian Oak Dales', 'forest', dict()),
    # Эльфы
    'sylvan_wildwood': ('Пуща Сильвендара', 'Sylvan Wildwood', 'dark_forest',
                        dict(grass=0x5FBF6A, foliage=0x4FBF5A, fog=0xB8E8D0, water=0x3FD8C8, particle=('minecraft:spore_blossom_air', 0.004))),
    'moon_grove': ('Лунная роща', 'Moon Grove', 'old_growth_birch_forest', dict(grass=0x8FD8A0, foliage=0x9FE0B0, fog=0xD8E0FF)),
    'silver_glade': ('Серебряные поляны', 'Silver Glades', 'cherry_grove', dict()),
    # Дварфы
    'iron_peaks': ('Железные пики', 'Iron Peaks', 'jagged_peaks', dict(surface='peaks')),
    'ore_slopes': ('Рудные склоны', 'Ore Slopes', 'windswept_gravelly_hills', dict(surface='ore_slopes')),
    'dwarf_vales': ('Горные долы', 'Mountain Vales', 'windswept_forest', dict()),
    # Орки
    'scorched_steppe': ('Выжженная степь', 'Scorched Steppe', 'savanna', dict(grass=0xBFA858, foliage=0xAFA048, surface='steppe')),
    'red_canyons': ('Красные каньоны', 'Red Canyons', 'badlands', dict(surface='canyon')),
    'dust_wastes': ('Пыльные пустоши', 'Dust Wastes', 'desert', dict(surface='sand', fog=0xE8D8A8)),
    # Зверолюды
    'fang_taiga': ('Клыкастая тайга', 'Fang Taiga', 'old_growth_spruce_taiga', dict()),
    'snow_hunt': ('Снежные угодья', 'Snowy Hunting Grounds', 'snowy_taiga', dict()),
    # Демониды
    'ash_wastes': ('Пепельная пустошь', 'Ash Wastes', 'desert',
                   dict(surface='ash', fog=0x5A4A48, sky=0x7A4A40, water=0x5A3A30, grass=0x6A6050, foliage=0x5A5040,
                        particle=('minecraft:white_ash', 0.06), rain=False, strip_veg=True, music='minecraft:music.nether.basalt_deltas')),
    'charred_forest': ('Обугленный лес', 'Charred Forest', 'savanna',
                       dict(surface='char', fog=0x4A3A38, sky=0x6A3A30, grass=0x5A5040, foliage=0x4A4030,
                            particle=('minecraft:ash', 0.03), rain=False, strip_veg=True, columns=True)),
    'smolder_craters': ('Тлеющие кратеры', 'Smoldering Craters', 'badlands',
                        dict(surface='crater', fog=0x6A3020, sky=0x8A3A20, particle=('minecraft:lava', 0.0015), rain=False,
                             strip_veg=True, lava=True)),
    # Гоблины
    'rot_swamp': ('Гнилые топи', 'Rotting Mire', 'swamp', dict(water=0x4A5A2A, fog=0x8A9A70, surface='mud')),
    'goblin_mangroves': ('Мангровые заросли', 'Goblin Mangroves', 'mangrove_swamp', dict(surface='mud')),
    # Ледяной Предел
    'frozen_reach': ('Ледяной Предел', 'Frozen Reach', 'snowy_plains', dict(surface='snow')),
    'glacier_spikes': ('Ледниковые иглы', 'Glacier Spikes', 'ice_spikes', dict(surface='snow')),
    'frost_peaks': ('Морозные вершины', 'Frost Peaks', 'frozen_peaks', dict(surface='peaks')),
    # Мёртвые земли
    'blight': ('Мёртвые земли', 'Blightlands', 'plains',
               dict(surface='blight', grass=0x8A8A6A, foliage=0x7A7A5A, fog=0x9A9A90, sky=0x8A8A88, strip_veg=True,
                    particle=('minecraft:ash', 0.01))),
    'bone_wastes': ('Костяные пустоши', 'Bone Wastes', 'desert', dict(surface='sand', fog=0xB8B0A0, sky=0xA0A098)),
    # Пограничье (дикие земли)
    'frontier_plains': ('Пограничные равнины', 'Frontier Plains', 'plains', dict()),
    'frontier_woods': ('Пограничные леса', 'Frontier Woods', 'forest', dict()),
    'frontier_hills': ('Пограничные холмы', 'Frontier Hills', 'windswept_hills', dict()),
    'frontier_taiga': ('Северное пограничье', 'Northern Frontier', 'taiga', dict()),
    'frontier_savanna': ('Южное пограничье', 'Southern Frontier', 'savanna', dict()),
    # Особые
    'world_wall': ('Край Мира', 'World\'s Edge', 'stony_peaks', dict(surface='peaks', fog=0xC8D0E0)),
    'city_lands': ('Городские земли', 'City Lands', 'plains', dict(strip_veg=True, no_monsters=True)),
    'city_stone': ('Каменный город', 'Stone City', 'windswept_hills', dict(strip_veg=True, no_monsters=True, surface='citystone')),
    'city_ash': ('Город пепла', 'Ash City', 'desert', dict(strip_veg=True, no_monsters=True, surface='ash', fog=0x5A4A48, sky=0x7A4A40,
                                                          rain=False, particle=('minecraft:white_ash', 0.02))),
    'city_steppe': ('Город степей', 'Steppe City', 'savanna', dict(strip_veg=True, no_monsters=True, surface='steppe', grass=0xBFA858)),
    'city_snow': ('Северный город', 'Northern City', 'snowy_taiga', dict(strip_veg=True, no_monsters=True)),
    'city_grove': ('Город-роща', 'Grove City', 'forest', dict(strip_veg=True, no_monsters=True, grass=0x5FBF6A, foliage=0x4FBF5A)),
    'emerald_bay': ('Изумрудный залив', 'Emerald Bay', 'lukewarm_ocean', dict(water=0x2FD8A8, water_fog=0x0A5A48)),
    'silver_lake': ('Серебряное озеро', 'Silver Lake', 'river', dict(water=0x5AB8E8)),
}

SURFACES = {}  # группа -> список биомов (заполняется ниже)

# Сколько мобов: монстры в своих краях (добавляются к ванильным)
REGION_MOBS = {
    'iron_peaks': [('litrpg:stone_troll', 18, 1, 2)], 'ore_slopes': [('litrpg:stone_troll', 14, 1, 1)],
    'frost_peaks': [('litrpg:frost_wraith', 20, 1, 2)], 'frozen_reach': [('litrpg:frost_wraith', 15, 1, 2)],
    'glacier_spikes': [('litrpg:frost_wraith', 15, 1, 2)],
    'sylvan_wildwood': [('litrpg:forest_wisp', 20, 1, 3)], 'moon_grove': [('litrpg:forest_wisp', 15, 1, 2)],
    'ash_wastes': [('litrpg:ash_imp', 40, 2, 4)], 'charred_forest': [('litrpg:ash_imp', 30, 1, 3)],
    'smolder_craters': [('litrpg:ash_imp', 40, 2, 4), ('minecraft:magma_cube', 20, 1, 3)],
    'rot_swamp': [('litrpg:bog_lurker', 30, 1, 2), ('litrpg:goblin', 40, 2, 4)],
    'goblin_mangroves': [('litrpg:bog_lurker', 25, 1, 2), ('litrpg:goblin', 40, 2, 4), ('litrpg:goblin_shaman', 10, 1, 1)],
    'dust_wastes': [('litrpg:sand_scorpion', 30, 1, 2)], 'red_canyons': [('litrpg:sand_scorpion', 25, 1, 2)],
    'scorched_steppe': [('litrpg:sand_scorpion', 15, 1, 1)],
    'blight': [('litrpg:grave_knight', 25, 1, 2), ('litrpg:skeleton_warden', 15, 1, 1)],
    'bone_wastes': [('litrpg:grave_knight', 25, 1, 2), ('minecraft:husk', 40, 2, 4)],
    'frontier_hills': [('litrpg:stone_troll', 6, 1, 1)], 'frontier_woods': [('litrpg:goblin', 15, 2, 3)],
    'world_wall': [('litrpg:stone_troll', 30, 1, 2), ('litrpg:frost_wraith', 20, 1, 2)],
}


NEW_CREATURES = {'stone_troll', 'frost_wraith', 'forest_wisp', 'ash_imp', 'bog_lurker', 'sand_scorpion', 'grave_knight'}
CREATURES_READY = True


def meander(points, amp, seed):
    """Извилистое русло: делим ломаную на шаги ~40 бл. и сдвигаем поперёк синусами разной длины."""
    if amp <= 0:
        return points
    rnd = random.Random(seed)
    ph1, ph2 = rnd.random() * 6.28, rnd.random() * 6.28
    out = []
    dist = 0.0
    total = sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(points, points[1:]))
    for (ax, az), (bx, bz) in zip(points, points[1:]):
        L = math.hypot(bx - ax, bz - az)
        n = max(1, int(L / 40))
        nx, nz = -(bz - az) / L, (bx - ax) / L
        for k in range(n):
            t = k / n
            x, z = ax + (bx - ax) * t, az + (bz - az) * t
            d = dist + L * t
            fade = min(1.0, d / 300.0, (total - d) / 300.0)  # исток и устье — без смещения
            off = amp * fade * (0.7 * math.sin(d / 420.0 + ph1) + 0.3 * math.sin(d / 170.0 + ph2))
            out.append([int(round(x + nx * off)), int(round(z + nz * off))])
        dist += L
    out.append(list(points[-1]))
    return out


def make_data():
    d = dict(size=5000, wall=[4430, 4880], territories=TERRITORIES, wild=WILD, seas=SEAS, lakes=LAKES,
             rivers=[], cities=[], flats=[], roads=[], pois=[], tiles=[], levels=LEVELS)
    for rv in RIVERS:
        pts = meander(rv['points'], rv.get('meander', 0), rv['name'])
        xs = [p[0] for p in pts]
        zs = [p[1] for p in pts]
        d['rivers'].append(dict(name=rv['name'], width=rv['width'], points=pts, box=[min(xs), min(zs), max(xs), max(zs)]))
    sizes = {'arcadia': 256, 'wolfden': 112, 'dawnfort': 112, 'northwatch': 112, 'blackbastion': 112}
    for c in CITIES:
        # центр города — так, чтобы шаблон начинался на границе чанка
        S = sizes.get(c['id'], 128)
        c['core'] = S // 2 - 4
        c['biome'] = {'khazad': 'litrpg:city_stone', 'ashgrad': 'litrpg:city_ash', 'ghardum': 'litrpg:city_steppe',
                      'wolfden': 'litrpg:city_snow', 'northwatch': 'litrpg:city_snow', 'sylvendar': 'litrpg:city_grove',
                      'blackbastion': 'litrpg:city_stone'}.get(c['id'], 'litrpg:city_lands')
        ox, oz = c['x'], c['z']
        c['x'] = int(round((c['x'] - S // 2) / 16)) * 16 + S // 2
        c['z'] = int(round((c['z'] - S // 2) / 16)) * 16 + S // 2
        for rd in ROADS:
            for p in rd['points']:
                if abs(p[0] - ox) < 5 and abs(p[1] - oz) < 5:
                    p[0], p[1] = c['x'], c['z']
        d['cities'].append(c)
        d['flats'].append([c['x'], c['z'], c['flat'], c['y']])
    d['roads'] = ROADS
    return d


# =====================================================================
#                 ПОВЕРХНОСТИ (surface rules) своих биомов
# =====================================================================

def blk(name, **props):
    s = {"Name": name}
    if props:
        s["Properties"] = props
    return {"type": "minecraft:block", "result_state": s}


def cond(c, then):
    return {"type": "minecraft:condition", "if_true": c, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": False, "secondary_depth_range": 0, "surface_type": "floor"}
UNDER = {"type": "minecraft:stone_depth", "offset": 0, "add_surface_depth": True, "secondary_depth_range": 0, "surface_type": "floor"}
WATER_OK = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}


def noise(lo, hi=1e9, name="minecraft:surface"):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": hi}


def above(y):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": 0, "add_stone_depth": False}


def surface_group(top, under):
    """Верхний блок (если не под водой — иначе гравий) и подслой."""
    return seq(cond(FLOOR, seq(cond(WATER_OK, top), blk("minecraft:gravel"))), cond(UNDER, under))


SURFACE_RULES = {
    'citystone': surface_group(seq(cond(noise(0.2), blk("minecraft:andesite")), cond(noise(-0.4, 0.0), blk("minecraft:gravel")),
                                   blk("minecraft:stone")), blk("minecraft:stone")),
    'sand': surface_group(blk("minecraft:sand"), blk("minecraft:sandstone")),
    'steppe': surface_group(seq(cond(noise(0.15), blk("minecraft:coarse_dirt")), cond(noise(-0.95, -0.6), blk("minecraft:red_sand")),
                                blk("minecraft:grass_block", snowy="false")), blk("minecraft:dirt")),
    'canyon': seq(cond(FLOOR, seq(cond(above(80), {"type": "minecraft:bandlands"}), cond(noise(0.0), blk("minecraft:red_sand")),
                                  blk("minecraft:terracotta"))),
                  cond(UNDER, {"type": "minecraft:bandlands"})),
    'peaks': seq(cond(FLOOR, seq(cond(above(165), blk("minecraft:snow_block")), cond(noise(0.3), blk("minecraft:gravel")),
                                 blk("minecraft:stone"))), cond(UNDER, blk("minecraft:stone"))),
    'ore_slopes': surface_group(seq(cond(noise(0.25), blk("minecraft:gravel")), cond(noise(-0.3, 0.0), blk("minecraft:andesite")),
                                    cond(noise(-1, -0.6), blk("minecraft:stone")), blk("minecraft:grass_block", snowy="false")),
                                blk("minecraft:dirt")),
    'ash': surface_group(seq(cond(noise(0.2), blk("minecraft:basalt", axis="y")), cond(noise(-0.4, 0.0), blk("minecraft:soul_soil")),
                             blk("minecraft:blackstone")), blk("minecraft:basalt", axis="y")),
    'char': surface_group(seq(cond(noise(0.3), blk("minecraft:soul_soil")), cond(noise(-0.2, 0.3), blk("minecraft:coarse_dirt")),
                              blk("minecraft:podzol", snowy="false")), blk("minecraft:dirt")),
    'crater': surface_group(seq(cond(noise(0.45), blk("minecraft:magma_block")), cond(noise(0.0), blk("minecraft:blackstone")),
                                blk("minecraft:basalt", axis="y")), blk("minecraft:blackstone")),
    'mud': seq(cond(FLOOR, seq(cond(noise(0.0), blk("minecraft:mud")), blk("minecraft:grass_block", snowy="false"))),
               cond(UNDER, blk("minecraft:mud"))),
    'snow': surface_group(blk("minecraft:snow_block"), seq(cond(noise(0.2), blk("minecraft:packed_ice")), blk("minecraft:dirt"))),
    'blight': surface_group(seq(cond(noise(0.3), blk("minecraft:gravel")), cond(noise(-0.2, 0.3), blk("minecraft:coarse_dirt")),
                                cond(noise(-1, -0.5), blk("minecraft:podzol", snowy="false")), blk("minecraft:grass_block", snowy="false")),
                            blk("minecraft:dirt")),
}


def surface_rule(vanilla_rule):
    """Наши правила — перед ванильными, только у поверхности."""
    groups = {}
    for bid, (_, _, _, opt) in BIOMES.items():
        if 'surface' in opt:
            groups.setdefault(opt['surface'], []).append('litrpg:' + bid)
    mine = []
    for g, biomes in groups.items():
        mine.append(cond({"type": "minecraft:biome", "biome_is": biomes}, SURFACE_RULES[g]))
    rule = cond({"type": "minecraft:above_preliminary_surface"}, seq(*mine))
    return seq(rule, vanilla_rule)


# =====================================================================
#                 БИОМЫ: копии ванильных с правками
# =====================================================================

VEG_STEPS = 9   # индекс шага растительности


def biome_json(bid, analog, opt):
    src = json.load(open(os.path.join(DUMP, 'biome', 'minecraft', analog + '.json'), encoding='utf-8'))
    b = copy.deepcopy(src)
    e = b['effects']
    if 'grass' in opt: e['grass_color'] = opt['grass']
    if 'foliage' in opt: e['foliage_color'] = opt['foliage']
    if 'fog' in opt: e['fog_color'] = opt['fog']
    if 'sky' in opt: e['sky_color'] = opt['sky']
    if 'water' in opt: e['water_color'] = opt['water']
    if 'water_fog' in opt: e['water_fog_color'] = opt['water_fog']
    if 'particle' in opt:
        e['particle'] = {"options": {"type": opt['particle'][0]}, "probability": opt['particle'][1]}
    if 'music' in opt:
        e['music'] = {"sound": opt['music'], "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}
    if 'temperature' in opt: b['temperature'] = opt['temperature']
    if opt.get('rain') is False:
        b['has_precipitation'] = False
        b['temperature'] = 2.0
        b['downfall'] = 0.0
    feats = b['features']
    while len(feats) < 11:
        feats.append([])
    if opt.get('strip_veg'):
        # без деревьев и цветов (пепел, мёртвые земли, город)
        feats[VEG_STEPS] = [f for f in feats[VEG_STEPS] if not any(k in f for k in ('tree', 'flower', 'grass', 'cactus', 'bamboo', 'sugar', 'pumpkin', 'melon', 'berry'))]
        if bid in ('blight', 'ash_wastes', 'charred_forest', 'smolder_craters'):
            feats[VEG_STEPS].append('minecraft:patch_dead_bush_badlands')
    if opt.get('columns'):
        feats[VEG_STEPS].append('litrpg:charred_columns')
    if opt.get('lava'):
        feats[1] = list(feats[1]) + ['minecraft:lake_lava_surface']
    if bid == 'bone_wastes':
        feats[3] = list(feats[3]) + ['minecraft:fossil_upper']
    # дороги — последним шагом во всех биомах карты
    if bid not in ('emerald_bay',):
        feats[10] = list(feats[10]) + ['litrpg:map_roads']
    b['features'] = sort_features(feats)
    # мобы
    sp = b['spawners']
    if opt.get('no_monsters'):
        sp['monster'] = []
    for mob, w, lo, hi in REGION_MOBS.get(bid, []):
        if mob.split(':')[1] in NEW_CREATURES and not CREATURES_READY:
            continue
        sp.setdefault('monster', []).append({"type": mob, "weight": w, "minCount": lo, "maxCount": hi})
    return b


_ORDER = None


def feature_order():
    """Единый порядок фич на каждом шаге — как во всех ванильных биомах (иначе Minecraft падает)."""
    global _ORDER
    if _ORDER is not None:
        return _ORDER
    bdir = os.path.join(DUMP, 'biome', 'minecraft')
    steps = [dict() for _ in range(11)]   # шаг -> {фича: множество предшественников}
    for f in sorted(os.listdir(bdir)):
        b = json.load(open(os.path.join(bdir, f), encoding='utf-8'))
        for i, lst in enumerate(b.get('features', [])):
            for j, feat in enumerate(lst):
                pre = steps[i].setdefault(feat, set())
                pre.update(lst[:j])
    order = []
    for st in steps:
        res, done = [], set()

        def visit(n, stack=()):
            if n in done or n in stack:
                return
            for p in sorted(st.get(n, ())):
                visit(p, stack + (n,))
            done.add(n)
            res.append(n)
        for n in sorted(st):
            visit(n)
        order.append({n: k for k, n in enumerate(res)})
    _ORDER = order
    return order


def sort_features(feats):
    order = feature_order()
    out = []
    for i, lst in enumerate(feats):
        seen, known, mine = set(), [], []
        for f in lst:
            if f in seen:
                continue
            seen.add(f)
            (known if f in order[i] else mine).append(f)
        known.sort(key=lambda f: order[i][f])
        mine.sort()
        out.append(known + mine)
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)


def write_worldgen(data):
    wg = os.path.join(DATA, 'litrpg', 'worldgen')
    # биомы
    for bid, (ru, en, analog, opt) in BIOMES.items():
        write_json(os.path.join(wg, 'biome', bid + '.json'), biome_json(bid, analog, opt))
    # обугленные стволы
    write_json(os.path.join(wg, 'configured_feature', 'charred_columns.json'), {
        "type": "minecraft:block_column", "config": {
            "direction": "up", "allowed_placement": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
            "prioritize_tip": False,
            "layers": [{"height": {"type": "minecraft:uniform", "value": {"min_inclusive": 2, "max_inclusive": 6}},
                        "provider": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:polished_basalt", "Properties": {"axis": "y"}}}}]}})
    write_json(os.path.join(wg, 'placed_feature', 'charred_columns.json'), {
        "feature": "litrpg:charred_columns", "placement": [
            {"type": "minecraft:count", "count": 3}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:matching_blocks", "offset": [0, -1, 0],
                                                                         "blocks": ["minecraft:podzol", "minecraft:coarse_dirt", "minecraft:soul_soil", "minecraft:grass_block"]}},
            {"type": "minecraft:biome"}]})
    # дороги (Java-фича) — один раз на чанк
    write_json(os.path.join(wg, 'configured_feature', 'map_roads.json'), {"type": "litrpg:map_roads", "config": {}})
    write_json(os.path.join(wg, 'placed_feature', 'map_roads.json'), {"feature": "litrpg:map_roads", "placement": []})

    # рельеф: ванильный overworld, но «толща» (sloped_cheese) и предварительная поверхность — из нашей карты
    ns = json.load(open(os.path.join(DUMP, 'noise_settings', 'overworld.json'), encoding='utf-8'))
    router = ns['noise_router']
    fd = json.dumps(router['final_density']).replace('"minecraft:overworld/sloped_cheese"', '"litrpg:map/sloped_cheese"')
    fd = fd.replace('"from_y": 240, "to_y": 256', '"from_y": 300, "to_y": 316')
    router['final_density'] = json.loads(fd)
    init = json.dumps(router['initial_density_without_jaggedness']).replace('"from_y": 240, "to_y": 256', '"from_y": 300, "to_y": 316')
    router['initial_density_without_jaggedness'] = {"type": "litrpg:map_shape", "mode": "initial", "input": json.loads(init)}
    ns['surface_rule'] = surface_rule(ns['surface_rule'])
    write_json(os.path.join(wg, 'noise_settings', 'system.json'), ns)
    write_json(os.path.join(wg, 'density_function', 'map', 'sloped_cheese.json'),
               {"type": "litrpg:map_shape", "mode": "cheese", "input": "minecraft:overworld/sloped_cheese"})

    # Преисподняя: ванильная, но с пещерой-площадкой под городом демонидов
    nn = json.load(open(os.path.join(DUMP, 'noise_settings', 'nether.json'), encoding='utf-8'))
    nn['noise_router']['final_density'] = {"type": "litrpg:map_shape", "mode": "nether", "input": nn['noise_router']['final_density']}
    write_json(os.path.join(wg, 'noise_settings', 'system_nether.json'), nn)
    nether_list = json.load(open(os.path.join(DUMP, 'parameter_list', 'nether.json'), encoding='utf-8'))

    # плитки построек карты (города, поселения, подземелья, чудеса)
    for layer, tag in (('overworld', '#litrpg:map_overworld'), ('nether', '#litrpg:map_nether')):
        name = 'map_tiles' if layer == 'overworld' else 'map_tiles_nether'
        write_json(os.path.join(wg, 'structure', name + '.json'), {
            "type": "litrpg:map_tile", "layer": layer, "biomes": tag, "step": "surface_structures",
            "spawn_overrides": {}, "terrain_adaptation": "none"})
        write_json(os.path.join(wg, 'structure_set', name + '.json'), {
            "structures": [{"structure": "litrpg:" + name, "weight": 1}],
            "placement": {"type": "litrpg:map_tiles", "layer": layer}})

    # тип мира
    preset = {"dimensions": {
        "minecraft:overworld": {"type": "minecraft:overworld", "generator": {
            "type": "minecraft:noise", "settings": "litrpg:system", "biome_source": {"type": "litrpg:territories"}}},
        "minecraft:the_nether": {"type": "minecraft:the_nether", "generator": {
            "type": "minecraft:noise", "settings": "litrpg:system_nether",
            "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"}}},
        "minecraft:the_end": {"type": "minecraft:the_end", "generator": {
            "type": "minecraft:noise", "settings": "minecraft:end", "biome_source": {"type": "minecraft:the_end"}}}}}
    write_json(os.path.join(wg, 'world_preset', 'system_world.json'), preset)
    write_json(os.path.join(DATA, 'minecraft', 'tags', 'worldgen', 'world_preset', 'normal.json'),
               {"replace": False, "values": ["litrpg:system_world"]})
    _ = nether_list

    # теги биомов
    mine = ['litrpg:' + b for b in BIOMES]
    tagdir = os.path.join(DATA, 'litrpg', 'tags', 'worldgen', 'biome')
    write_json(os.path.join(tagdir, 'map_overworld.json'), {"replace": False, "values": mine + [
        "minecraft:river", "minecraft:frozen_river", "minecraft:beach", "minecraft:snowy_beach",
        "minecraft:lush_caves", "minecraft:dripstone_caves", "minecraft:deep_dark"]})
    write_json(os.path.join(tagdir, 'map_nether.json'), {"replace": False, "values": [
        "minecraft:nether_wastes", "minecraft:soul_sand_valley", "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:basalt_deltas"]})
    vt = os.path.join(DATA, 'minecraft', 'tags', 'worldgen', 'biome')
    land = [b for b in mine if b not in ('litrpg:emerald_bay', 'litrpg:silver_lake')]
    write_json(os.path.join(vt, 'is_overworld.json'), {"replace": False, "values": mine})
    write_json(os.path.join(vt, 'has_structure', 'mineshaft.json'), {"replace": False, "values": land})
    write_json(os.path.join(vt, 'is_ocean.json'), {"replace": False, "values": ['litrpg:emerald_bay']})
    write_json(os.path.join(vt, 'is_river.json'), {"replace": False, "values": ['litrpg:silver_lake']})
    write_json(os.path.join(vt, 'spawns_snow_foxes.json'), {"replace": False, "values": ['litrpg:snow_hunt', 'litrpg:frozen_reach']})
    write_json(os.path.join(vt, 'spawns_white_rabbits.json'), {"replace": False, "values": ['litrpg:snow_hunt', 'litrpg:frozen_reach', 'litrpg:glacier_spikes']})
    write_json(os.path.join(vt, 'stronghold_biased_to.json'), {"replace": False, "values": land})


NPC_NAMES = {
    'human_knight': ('Рыцарь Королевства', 'Kingdom Knight'), 'human_ranger': ('Королевский следопыт', 'Royal Ranger'),
    'human_mage': ('Придворный маг', 'Court Mage'), 'human_merchant': ('Торговец', 'Merchant'),
    'human_lord': ('Лорд-регент Аркадии', 'Lord Regent of Arcadia'),
    'human_guard': ('Страж Королевства', 'Kingdom Guard'), 'elf_sentinel': ('Страж Сильвендара', 'Sylvendar Sentinel'),
    'dwarf_ironguard': ('Железный страж', 'Ironguard'), 'orc_bloodguard': ('Кровавый страж Орды', 'Horde Bloodguard'),
    'beast_fangguard': ('Клык-страж', 'Fangguard'), 'demon_hellguard': ('Адский страж', 'Hellguard'),
    'human_citizen': ('Горожанин', 'Citizen'), 'elf_citizen': ('Эльф-горожанин', 'Elven Citizen'),
    'dwarf_citizen': ('Дварф-горожанин', 'Dwarven Citizen'), 'orc_citizen': ('Орк-работяга', 'Orc Worker'),
    'beast_citizen': ('Зверолюд из племени', 'Beastkin Tribesman'), 'demon_citizen': ('Демонид-горожанин', 'Demonkin Citizen'),
    'human_artisan': ('Мастер Королевства', 'Kingdom Artisan'), 'elf_artisan': ('Эльф-мастер', 'Elven Artisan'),
    'dwarf_artisan': ('Дварф-мастер', 'Dwarven Artisan'), 'orc_artisan': ('Орк-мастер', 'Orc Artisan'),
    'beast_artisan': ('Мастер-зверолюд', 'Beastkin Artisan'), 'demon_artisan': ('Демонид-мастер', 'Demonkin Artisan'),
}


def write_lang():
    ru_p, en_p = os.path.join(ASSETS, 'lang', 'ru_ru.json'), os.path.join(ASSETS, 'lang', 'en_us.json')
    ru, en = json.load(open(ru_p, encoding='utf-8')), json.load(open(en_p, encoding='utf-8'))
    for bid, (r, e, _, _) in BIOMES.items():
        ru['biome.litrpg.' + bid] = r
        en['biome.litrpg.' + bid] = e
    for key, (r, e) in NPC_NAMES.items():
        ru['entity.litrpg.' + key] = r
        en['entity.litrpg.' + key] = e
        ru['item.litrpg.' + key + '_spawn_egg'] = 'Яйцо призыва: ' + r
        en['item.litrpg.' + key + '_spawn_egg'] = e + ' Spawn Egg'
    ru['item.litrpg.parcel'] = 'Посылка'
    en['item.litrpg.parcel'] = 'Parcel'
    ru['generator.litrpg.system_world'] = 'Мир Системы'
    en['generator.litrpg.system_world'] = 'World of the System'
    ru['generator.litrpg.system_world.info'] = 'Карта 10000×10000: земли народов, города, дороги и подземелья.'
    en['generator.litrpg.system_world.info'] = 'A 10000×10000 map: lands of the peoples, cities, roads and dungeons.'
    json.dump(ru, open(ru_p, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_p, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)


# =====================================================================
#                 КАРТА-СВИТОК (картина и атлас)
# =====================================================================

FONT = {  # 5×7, заглавные кириллические и латинские нужные буквы
    'А': ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    'Б': ["11111", "10000", "10000", "11110", "10001", "10001", "11110"],
    'В': ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    'Г': ["11111", "10000", "10000", "10000", "10000", "10000", "10000"],
    'Д': ["00110", "01010", "01010", "01010", "01010", "11111", "10001"],
    'Е': ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    'Ж': ["10101", "10101", "01110", "00100", "01110", "10101", "10101"],
    'З': ["01110", "10001", "00001", "00110", "00001", "10001", "01110"],
    'И': ["10001", "10001", "10011", "10101", "11001", "10001", "10001"],
    'Й': ["01010", "00100", "10011", "10101", "11001", "10001", "10001"],
    'К': ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    'Л': ["00111", "01001", "01001", "01001", "01001", "01001", "10001"],
    'М': ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    'Н': ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    'О': ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    'П': ["11111", "10001", "10001", "10001", "10001", "10001", "10001"],
    'Р': ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    'С': ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    'Т': ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    'У': ["10001", "10001", "10001", "01111", "00001", "10001", "01110"],
    'Ф': ["00100", "01110", "10101", "10101", "01110", "00100", "00100"],
    'Х': ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    'Ц': ["10010", "10010", "10010", "10010", "10010", "11111", "00001"],
    'Ч': ["10001", "10001", "10001", "01111", "00001", "00001", "00001"],
    'Ш': ["10101", "10101", "10101", "10101", "10101", "10101", "11111"],
    'Щ': ["10101", "10101", "10101", "10101", "10101", "11111", "00001"],
    'Ъ': ["11000", "01000", "01000", "01110", "01001", "01001", "01110"],
    'Ы': ["10001", "10001", "10001", "11101", "10011", "10011", "11101"],
    'Ь': ["10000", "10000", "10000", "11110", "10001", "10001", "11110"],
    'Э': ["01110", "10001", "00001", "00111", "00001", "10001", "01110"],
    'Ю': ["10010", "10101", "10101", "11101", "10101", "10101", "10010"],
    'Я': ["01111", "10001", "10001", "01111", "00101", "01001", "10001"],
    "'": ["00100", "00100", "00000", "00000", "00000", "00000", "00000"],
    '-': ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    ' ': ["00000"] * 7,
}


def text_px(img, x, y, s, color, scale=1, shadow=None):
    s = s.upper().replace('Ё', 'Е')
    cx = x
    for ch in s:
        g = FONT.get(ch, FONT[' '])
        for gy, row in enumerate(g):
            for gx, c in enumerate(row):
                if c == '1':
                    for sy in range(scale):
                        for sx in range(scale):
                            px, py = cx + gx * scale + sx, y + gy * scale + sy
                            if shadow and 0 <= px + 1 < len(img[0]) and 0 <= py + 1 < len(img):
                                img[py + 1][px + 1] = shadow
                            if 0 <= px < len(img[0]) and 0 <= py < len(img):
                                img[py][px] = color
        cx += 6 * scale


def text_width(s, scale=1):
    return len(s) * 6 * scale


# где писать названия земель (координаты мира), чтобы не налезали на города
LABELS = {'deadlands': (3950, 750), 'goblins': (250, 3850), 'dwarves': (2900, -3100), 'frost': (300, -4200),
          'kingdom': (0, -500), 'orcs': (3000, 2150)}


def render_art(world, size=512):
    """Карта-свиток: пергамент, территории, рельеф, вода, дороги, города."""
    k = 10000 / size
    H = [[0.0] * size for _ in range(size)]
    T = [[-1] * size for _ in range(size)]
    Wt = [[0] * size for _ in range(size)]
    for py in range(size):
        z = -5000 + (py + 0.5) * k
        for px in range(size):
            x = -5000 + (px + 0.5) * k
            h, _, t, w = world.sample(x, z)
            H[py][px], T[py][px], Wt[py][px] = h, t, w
    parch = (226, 206, 160)
    img = [[parch + (255,)] * size for _ in range(size)]
    for py in range(size):
        for px in range(size):
            x = -5000 + (px + 0.5) * k
            z = -5000 + (py + 0.5) * k
            h = H[py][px]
            base = parch
            t = T[py][px]
            if t >= 0:
                tc = TERRITORIES[t]['color']
                tcol = ((tc >> 16) & 255, (tc >> 8) & 255, tc & 255)
                base = tuple(int(base[i] * 0.62 + tcol[i] * 0.38) for i in range(3))
            # бумага: пятна
            n = vn(x, z, 600, 5) * 0.08 + vn(x, z, 90, 6) * 0.05
            base = tuple(int(c * (0.94 + n)) for c in base)
            if h < 63 or Wt[py][px] in (1, 2, 3):
                deep = max(0.0, min(1.0, (63 - h) / 30))
                base = (int(120 - 40 * deep), int(160 - 30 * deep), int(170 - 10 * deep))
            else:
                # отмывка рельефа (свет с северо-запада)
                hl = H[py][max(0, px - 1)]
                hu = H[max(0, py - 1)][px]
                slope = (h - hl) + (h - hu)
                sh = max(-0.35, min(0.35, slope * 0.04))
                base = tuple(int(max(0, min(255, c * (1 + sh)))) for c in base)
                if h > 150:
                    m = min(1.0, (h - 150) / 80)
                    base = tuple(int(c * (1 - 0.35 * m) + 90 * 0.35 * m) for c in base)
                if h > 175:
                    base = tuple(min(255, int(c + 60 * min(1.0, (h - 175) / 40))) for c in base)
            img[py][px] = base + (255,)
    # береговая линия и границы территорий — тёмной тушью
    ink = (70, 50, 30, 255)
    out = [row[:] for row in img]
    for py in range(1, size - 1):
        for px in range(1, size - 1):
            wa = H[py][px] < 63
            if wa != (H[py][px + 1] < 63) or wa != (H[py + 1][px] < 63):
                out[py][px] = (50, 70, 80, 255)
            elif T[py][px] != T[py][px + 1] or T[py][px] != T[py + 1][px]:
                if (px + py) % 3 != 0:
                    out[py][px] = (110, 80, 50, 255)
    img = out

    def to_px(x, z):
        return int((x + 5000) / k), int((z + 5000) / k)

    # дороги: пунктир
    for rd in ROADS:
        pts = rd['points']
        for i in range(len(pts) - 1):
            ax, az = to_px(*pts[i])
            bx, bz = to_px(*pts[i + 1])
            n = max(abs(bx - ax), abs(bz - az), 1)
            for s in range(n + 1):
                if (s // 3) % 2 == 0:
                    x = ax + (bx - ax) * s // n
                    z = az + (bz - az) * s // n
                    if 0 <= x < size and 0 <= z < size:
                        img[z][x] = (120, 60, 30, 255)
    # поселения и подземелья — значки
    icons = {'village': ((120, 80, 40), 1), 'farm': ((150, 120, 40), 1), 'dungeon': ((150, 20, 20), 2), 'wonder': ((150, 40, 160), 2),
             'camp': ((90, 60, 30), 1), 'fishing': ((40, 80, 130), 1), 'lumber': ((60, 100, 40), 1), 'caravan': ((170, 120, 30), 1),
             'outpost': ((80, 80, 80), 1), 'settlement': ((90, 60, 30), 1)}
    for poi in world.d['pois']:
        col, r = icons.get(poi['type'], ((60, 60, 60), 1))
        x, z = to_px(poi['x'], poi['z'])
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if 0 <= x + dx < size and 0 <= z + dz < size:
                    img[z + dz][x + dx] = col + (255,)
        if poi['type'] == 'dungeon':
            for d in range(-3, 4):
                for (xx, zz) in ((x + d, z + d), (x + d, z - d)):
                    if 0 <= xx < size and 0 <= zz < size:
                        img[zz][xx] = (150, 20, 20, 255)
    # города: башенка и название
    for c in CITIES:
        x, z = to_px(c['x'], c['z'])
        big = c['id'] == 'arcadia'
        r = 5 if big else 4
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                edge = abs(dx) == r or abs(dz) == r
                if 0 <= x + dx < size and 0 <= z + dz < size:
                    img[z + dz][x + dx] = (60, 30, 20, 255) if edge else (200, 50, 40, 255)
        for dx in range(-r, r + 1, 2):  # зубцы
            if 0 <= z - r - 1 < size:
                img[z - r - 1][x + dx] = (60, 30, 20, 255)
        name = c['name']
        tw = text_width(name)
        text_px(img, max(2, min(size - tw - 2, x - tw // 2)), z + r + 3, name, (50, 25, 15, 255), 1, (230, 210, 170, 255))
    # названия территорий — крупно
    for t in TERRITORIES:
        sx, sz = LABELS.get(t['id'], (t['seeds'][0][0], t['seeds'][0][1] - 430))
        x, z = to_px(sx, sz)
        z += 22
        name = t['name']
        tw = text_width(name)
        text_px(img, max(2, min(size - tw - 2, x - tw // 2)), z - 22, name, (90, 40, 20, 255), 1, (235, 220, 185, 255))
    # рамка
    for i in range(size):
        for j in range(4):
            col = (90, 60, 30, 255) if j in (0, 3) else (200, 170, 110, 255)
            img[j][i] = img[size - 1 - j][i] = img[i][j] = img[i][size - 1 - j] = col
    # роза ветров (С наверху)
    cx, cz = size - 36, 36
    for d in range(-14, 15):
        img[cz + d][cx] = (70, 40, 20, 255)
        img[cz][cx + d] = (70, 40, 20, 255)
    text_px(img, cx - 2, cz - 24, 'С', (150, 20, 20, 255), 1)
    return img


def main():
    data = make_data()
    world = World(data)
    # нужные Java поля сохранены; POI и плитки добавит gen_mapbuild.py
    old_path = os.path.join(RES, 'litrpg_world.json')
    if os.path.exists(old_path):
        old = json.load(open(old_path, encoding='utf-8'))
        data['pois'] = old.get('pois', [])
        data['tiles'] = old.get('tiles', [])
        data['nether'] = old.get('nether', {})
        data['workshops'] = old.get('workshops', [])
    # площадки поселений (их радиус и высоту выбрал gen_mapbuild.py)
    for p in data['pois']:
        if p.get('r'):
            data['flats'].append([p['x'], p['z'], p['r'], p['y']])
    # высоты городов: проверка суши
    for c in CITIES:
        h, _, t, _ = World(dict(data, flats=[])).sample(c['x'], c['z'])
        print(f"{c['name']:24s} рельеф {h:6.1f}, площадка {c['y']}, территория {TERRITORIES[t]['id'] if t >= 0 else 'wild'}")
    write_json(old_path, data)
    write_worldgen(data)
    write_lang()
    if '--art' in sys.argv:
        art = render_art(World(data), 512)
        os.makedirs(os.path.join(ASSETS, 'textures', 'painting'), exist_ok=True)
        os.makedirs(os.path.join(ASSETS, 'textures', 'gui'), exist_ok=True)
        write_png(os.path.join(ASSETS, 'textures', 'gui', 'atlas.png'), art, 512, 512)
        # картина 8×8 блоков — та же карта
        write_png(os.path.join(ASSETS, 'textures', 'painting', 'world_map.png'), art, 512, 512)
        print('art written')
    print('world data written')


if __name__ == '__main__':
    main()
