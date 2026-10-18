"""
Подземелья: 10 тематических данжей с хранителями.
Каждое — вход на поверхности, лестница вниз, залы с монстрами и сундуками
и большая арена с Печатью подземелья (block litrpg:boss_altar), которая
призывает босса. Генерирует .nbt-шаблоны, worldgen JSON и таблицы добычи.
Запуск: python tools/gen_dungeons.py
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_structures import Build, TInt, ROOT  # noqa: E402

SX, SY, SZ = 41, 32, 41
G = 17          # уровень пола поверхности внутри шаблона (совпадает с землёй)
F = 2           # уровень пола подземелья
C = 20          # центр по x и z


class Theme:
    def __init__(self, key, level, wall, floor, ceil, accent, light, stair, mobs, spacing, salt, biomes, **kw):
        self.key, self.level = key, level
        self.wall, self.floor, self.ceil, self.accent = wall, floor, ceil, accent
        self.light, self.stair, self.mobs = light, stair, mobs
        self.spacing, self.salt, self.biomes = spacing, salt, biomes
        self.kw = kw


def pick(b, opts, x, y, z):
    """Случайный блок из списка (с весами как повторами) — стабильно от координат."""
    if isinstance(opts, str):
        return opts
    i = (x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ b.seed) % len(opts)
    return opts[i]


class Dungeon(Build):
    def __init__(self, theme):
        super().__init__(SX, SY, SZ, theme.key)
        self.t = theme
        self.seed = sum(ord(c) for c in theme.key) * 7919

    # ---------- примитивы ----------
    def wallb(self, x, y, z):
        self.set(x, y, z, pick(self, self.t.wall, x, y, z))

    def room(self, x1, z1, x2, z2, h, floor=None, ceil=None):
        """Комната: стены, пол, потолок; внутри — воздух. Пол на высоте F-1, воздух F..F+h-1."""
        y1, y2 = F - 1, F + h
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                self.set(x, F - 2, z, pick(self, self.t.wall, x, F - 2, z))
                self.set(x, y1, z, pick(self, floor or self.t.floor, x, y1, z) if not edge else pick(self, self.t.wall, x, y1, z))
                self.set(x, y2, z, pick(self, ceil or self.t.ceil, x, y2, z))
                for y in range(F, y2):
                    if edge:
                        self.wallb(x, y, z)
                    else:
                        self.set(x, y, z, 'minecraft:air')

    def opening(self, x1, z1, x2, z2, h=4):
        """Проход сквозь стены (воздух) — дверные проёмы и коридоры."""
        for x in range(x1, x2 + 1):
            for z in range(z1, z2 + 1):
                for y in range(F, F + h):
                    self.set(x, y, z, 'minecraft:air')

    def corridor(self, x1, z1, x2, z2, h=4):
        """Коридор с толстыми стенами (прямоугольник, внутри воздух)."""
        self.room(x1 - 1, z1 - 1, x2 + 1, z2 + 1, h)

    def light_at(self, x, yc, z, chain=0):
        """Светильник в потолке на высоте yc: фонарь висит под ним (на цепи), блок света — встраивается."""
        l = self.t.light
        if l in ('lantern', 'soul_lantern'):
            for i in range(chain):
                self.set(x, yc - 1 - i, z, 'minecraft:chain', axis='y', waterlogged='false')
            self.set(x, yc - 1 - chain, z, 'minecraft:' + l, hanging='true', waterlogged='false')
        else:
            self.set(x, yc, z, l)

    def post_light(self, x, y, z):
        l = self.t.light
        if l in ('lantern', 'soul_lantern'):
            self.set(x, y, z, 'minecraft:' + l, hanging='false', waterlogged='false')
        else:
            self.set(x, y, z, l)

    def ceiling_lights(self, x1, z1, x2, z2, h, step=4):
        for x in range(x1 + 2, x2 - 1, step):
            for z in range(z1 + 2, z2 - 1, step):
                self.light_at(x, F + h, z)

    def mob(self, x, z, kind, y=None, lvl=None):
        eid = kind if ':' in kind else 'minecraft:' + kind
        self.entities.append((x, F if y is None else y, z, eid, {'ForgeData': {'litrpg_dlvl': TInt(lvl or self.t.level)}}))

    def mobs_in(self, x1, z1, x2, z2, n):
        kinds = self.t.mobs
        for i in range(n):
            x = x1 + 1 + (i * 5 + self.seed) % max(1, x2 - x1 - 1)
            z = z1 + 1 + (i * 3 + self.seed // 7) % max(1, z2 - z1 - 1)
            self.mob(x, z, kinds[i % len(kinds)], lvl=self.t.level - 2 + i % 3)

    def chest_at(self, x, z, facing='north', y=None):
        self.chest(x, F if y is None else y, z, 'litrpg:chests/dungeon_' + self.t.key, facing)

    # ---------- общий план ----------
    def layout(self):
        t = self.t
        # фундамент под входом
        for x in range(13, 28):
            for z in range(12, 28):
                for y in range(14, G):
                    self.wallb(x, y, z)
        # коридоры (сначала — залы потом перекроют их стены)
        self.corridor(9, 5, 15, 7)                     # прихожая -> запад
        self.corridor(9, 8, 11, 12)
        self.corridor(5, 25, 7, 30)                    # запад -> юго-запад
        self.corridor(14, 34, 17, 36)                  # юго-запад -> арена
        self.corridor(26, 5, 26, 7)                    # прихожая -> восток
        self.corridor(32, 12, 34, 17)                  # восток -> арена
        # залы
        self.room(15, 1, 25, 11, 5)                    # прихожая (сюда ведёт лестница)
        self.room(0, 13, 12, 25, 5)                    # западный зал
        self.room(0, 30, 14, 40, 5)                    # юго-западный зал
        self.room(27, 0, 40, 12, 5)                    # восточный зал
        self.room(17, 17, 40, 40, 11, floor=t.kw.get('arena_floor'))   # арена
        # проходы
        self.opening(9, 5, 15, 7)
        self.opening(9, 5, 11, 13)
        self.opening(5, 25, 7, 30)
        self.opening(14, 34, 17, 36)
        self.opening(25, 5, 27, 7)
        self.opening(32, 12, 34, 17)
        # лестница с поверхности
        self.staircase()
        # свет
        self.ceiling_lights(15, 1, 25, 11, 5)
        self.ceiling_lights(0, 13, 12, 25, 5)
        self.ceiling_lights(0, 30, 14, 40, 5)
        self.ceiling_lights(27, 0, 40, 12, 5)
        for x, z in ((24, 24), (32, 24), (24, 32), (32, 32), (28, 20), (28, 37), (20, 28), (37, 28)):
            self.light_at(x, F + 11, z, chain=2)
        # светильники в стенах арены и залов
        wl = t.kw.get('wall_light', 'minecraft:ochre_froglight')
        for i in range(19, 40, 4):
            for (x, z) in ((i, 17), (i, 40), (17, i), (40, i)):
                if self.blocks.get((x, F + 5, z), ('',))[0] != 'minecraft:air':
                    self.set(x, F + 5, z, wl)
        for (x1, z1, x2, z2) in ((15, 1, 25, 11), (0, 13, 12, 25), (0, 30, 14, 40), (27, 0, 40, 12)):
            for x in range(x1 + 2, x2 - 1, 5):
                for z in (z1, z2):
                    if self.blocks.get((x, F + 3, z), ('',))[0] != 'minecraft:air':
                        self.set(x, F + 3, z, wl)
        # арена: колонны, печать, награда
        for x, z in ((21, 21), (35, 21), (21, 35), (35, 35)):
            for y in range(F, F + 11):
                self.set(x, y, z, pick(self, t.accent, x, y, z))
        ax, az = 28, 28
        self.set(ax, F, az, 'litrpg:boss_altar')
        self.nbt[(ax, F, az)] = {'id': 'litrpg:boss_altar', 'Dungeon': t.key}
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 or abs(dz) == 2:
                    self.set(ax + dx, F - 1, az + dz, pick(self, t.accent, ax + dx, 0, az + dz))
        self.chest_at(28, 38, 'north')
        self.chest_at(27, 38, 'north')
        # сундуки в залах
        self.chest_at(1, 14, 'east')
        self.chest_at(1, 39, 'east')
        self.chest_at(39, 1, 'west')
        # монстры
        self.mobs_in(1, 14, 11, 24, 4)
        self.mobs_in(1, 31, 13, 39, 4)
        self.mobs_in(28, 1, 39, 11, 3)
        self.mobs_in(16, 2, 24, 7, 2)
        # оформление темы
        fn = getattr(self, 'deco_' + t.key, None)
        if fn:
            fn()
        self.surface()

    def staircase(self):
        t = self.t
        for k in range(15):
            y, z = G - 1 - k, G + 1 - k
            in_room = z <= 11
            for x in range(19, 22):
                self.set(x, y, z, t.stair, facing='south', half='bottom', shape='straight', waterlogged='false')
                for yy in range(F - 1 if in_room else y - 1, y):
                    self.wallb(x, yy, z)
                for h in range(1, 5):
                    if y + h <= SY - 1:
                        self.set(x, y + h, z, 'minecraft:air')
                if y + 5 < G and not in_room:
                    self.wallb(x, y + 5, z)
            if not in_room:
                for x in (18, 22):
                    for h in range(-1, 6):
                        if y + h < G:
                            self.wallb(x, y + h, z)
                if k % 4 == 2 and y + 5 < G:
                    self.light_at(20, y + 5, z)
        # площадка внизу
        for z in range(2, 4):
            for x in range(19, 22):
                self.set(x, F - 1, z, pick(self, t.floor, x, F - 1, z))

    # ---------- поверхность ----------
    def surface(self):
        t = self.t
        # расчистка над входом и пол
        for x in range(13, 28):
            for z in range(12, 28):
                if 19 <= x <= 21 and 14 <= z <= 18:
                    continue  # проём лестницы
                self.set(x, G - 1, z, pick(self, t.kw.get('ground', t.floor), x, G - 1, z))
        fn = getattr(self, 'top_' + t.key, None)
        (fn or self.top_default)()
        # над входом — чистое небо (убираем деревья и холмы)
        for x in range(13, 28):
            for z in range(12, 28):
                for y in range(G, SY):
                    if (x, y, z) not in self.blocks:
                        self.set(x, y, z, 'minecraft:air')

    def arch(self, x1, x2, z, h, block, top=None):
        for x in (x1, x2):
            for y in range(G, G + h):
                self.set(x, y, z, pick(self, block, x, y, z))
        for x in range(x1, x2 + 1):
            self.set(x, G + h, z, pick(self, top or block, x, G + h, z))

    def top_default(self):
        t = self.t
        self.arch(18, 22, 13, 5, t.wall, t.accent)
        self.arch(18, 22, 19, 5, t.wall, t.accent)
        for z in range(13, 20):
            self.set(18, G + 5, z, pick(self, t.accent, 18, 0, z))
            self.set(22, G + 5, z, pick(self, t.accent, 22, 0, z))
        for x, z in ((15, 15), (25, 15), (15, 24), (25, 24)):
            for y in range(G, G + 3):
                self.wallb(x, y, z)
            self.post_light(x, G + 3, z)

    # ---------- темы ----------
    def deco_ruins(self):
        for x, z in ((3, 16), (9, 22), (5, 33), (11, 37)):
            self.set(x, F, z, 'minecraft:cobblestone')
            self.set(x, F + 1, z, 'minecraft:cobblestone_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
        for x in range(18, 40, 3):
            self.set(x, F + 10, 18, 'minecraft:cobweb')

    def top_ruins(self):
        self.top_default()
        for x in range(14, 27):
            if x % 3 != 0:
                self.wallb(x, G, 25)
                if x % 2:
                    self.wallb(x, G + 1, 25)
        for z in range(14, 25):
            if z % 4 != 1:
                self.wallb(14, G, z)
                self.wallb(26, G, z)
        self.set(16, G, 22, 'minecraft:mossy_cobblestone')
        self.set(24, G, 21, 'minecraft:mossy_cobblestone_slab', type='bottom', waterlogged='false')

    def deco_rotgrove(self):
        for x in range(18, 40, 2):
            for z in range(18, 40, 5):
                if (x + z) % 3 == 0:
                    self.set(x, F + 10, z, 'minecraft:hanging_roots', waterlogged='false')
        for x, z in ((3, 15), (8, 20), (4, 34), (12, 38), (30, 4), (37, 9), (20, 22), (36, 37)):
            self.set(x, F, z, 'minecraft:brown_mushroom' if (x + z) % 2 else 'minecraft:red_mushroom')
        self.set(28, F + 10, 28, 'minecraft:spore_blossom')

    def top_rotgrove(self):
        self.top_default()
        for tx, tz, h in ((15, 14, 7), (25, 25, 6), (14, 25, 5)):
            for y in range(G, G + h):
                self.set(tx, y, tz, 'minecraft:dark_oak_log', axis='y')
            self.set(tx + 1, G + h - 2, tz, 'minecraft:dark_oak_log', axis='x')
            self.set(tx - 1, G + h - 3, tz, 'minecraft:dark_oak_log', axis='x')
        for x in range(13, 28):
            for z in range(12, 28):
                if (x * 7 + z * 3) % 11 == 0:
                    self.set(x, G, z, 'minecraft:brown_mushroom')

    def deco_spider_nest(self):
        for x in range(0, 41):
            for z in range(0, 41):
                for y in (F + 3, F + 4, F + 8, F + 9):
                    if self.blocks.get((x, y, z), ('',))[0] == 'minecraft:air' and (x * 31 + z * 17 + y * 7) % 9 == 0:
                        self.set(x, y, z, 'minecraft:cobweb')
        # яйца-коконы
        for x, z in ((24, 24), (32, 33), (23, 34), (34, 23)):
            self.set(x, F, z, 'minecraft:white_wool')
            self.set(x, F + 1, z, 'minecraft:cobweb')

    def top_spider_nest(self):
        self.top_default()
        for x in range(13, 28):
            for z in range(12, 28):
                if (x * 13 + z * 7) % 8 == 0:
                    self.set(x, G, z, 'minecraft:cobweb')

    def deco_goblin_fort(self):
        for x, z in ((3, 23), (10, 15), (2, 37), (38, 10), (30, 2)):
            self.set(x, F, z, 'minecraft:hay_block', axis='y')
            self.set(x + 1, F, z, 'minecraft:barrel', facing='up', open='false')
        self.set(28, F + 1, 22, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')

    def top_goblin_fort(self):
        # частокол
        for x in range(13, 28):
            for z in (12, 27):
                for y in range(G, G + 4 + (x % 2)):
                    self.set(x, y, z, 'minecraft:spruce_log', axis='y')
        for z in range(12, 28):
            for x in (13, 27):
                for y in range(G, G + 4 + (z % 2)):
                    self.set(x, y, z, 'minecraft:spruce_log', axis='y')
        for x in range(19, 22):
            for y in range(G, G + 4):
                self.set(x, y, 12, 'minecraft:air')
        self.set(18, G + 4, 12, 'minecraft:skeleton_skull', rotation='8')
        self.set(22, G + 4, 12, 'minecraft:skeleton_skull', rotation='8')
        self.set(16, G, 22, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        self.arch(18, 22, 19, 4, 'minecraft:spruce_log', 'minecraft:spruce_planks')
        for kind, x, z in (('litrpg:goblin', 16, 16), ('litrpg:goblin', 24, 23), ('litrpg:goblin_shaman', 24, 15)):
            self.entities.append((x, G, z, kind, {}))

    def deco_mire(self):
        for x in range(1, 40):
            for z in range(1, 40):
                if self.blocks.get((x, F, z), ('',))[0] == 'minecraft:air' and (x * 5 + z * 11) % 13 == 0:
                    self.set(x, F - 1, z, 'minecraft:water', level='0')
        for x, z in ((4, 18), (8, 36), (34, 6)):
            self.set(x, F, z, 'minecraft:cauldron')
        for x in range(18, 40, 4):
            self.set(x, F + 10, 30, 'minecraft:vine', north='false', south='true', east='false', west='false', up='true')

    def top_mire(self):
        # хижина ведьмы на сваях
        for x, z in ((15, 14), (25, 14), (15, 25), (25, 25)):
            for y in range(G - 1, G + 2):
                self.set(x, y, z, 'minecraft:mangrove_log', axis='y')
        for x in range(15, 26):
            for z in range(14, 26):
                if not (19 <= x <= 21 and 13 <= z <= 19):
                    self.set(x, G + 2, z, 'minecraft:mangrove_planks')
        for x in range(16, 25):
            for z in (15, 24):
                for y in range(G + 3, G + 6):
                    if x in (16, 24):
                        self.set(x, y, z, 'minecraft:mangrove_log', axis='y')
                    elif not (z == 24 and x == 20 and y < G + 5):
                        self.set(x, y, z, 'minecraft:mangrove_planks')
        for x in range(15, 26):
            for z in range(14, 26):
                if (x + z) % 2 == 0 and not (19 <= x <= 21 and 13 <= z <= 19):
                    self.set(x, G + 6, z, 'minecraft:mud_brick_slab', type='bottom', waterlogged='false')
        self.arch(18, 22, 13, 3, 'minecraft:mangrove_log', 'minecraft:mangrove_planks')
        self.set(23, G + 3, 22, 'minecraft:cauldron')
        for x in range(13, 28):
            for z in range(12, 28):
                if (x * 3 + z * 5) % 7 == 0 and self.blocks.get((x, G, z), ('',))[0] == 'minecraft:air':
                    self.set(x, G - 1, z, 'minecraft:water', level='0')

    def deco_labyrinth(self):
        # лабиринт в западном и юго-западном залах
        for (x1, z1, x2, z2) in ((1, 14, 11, 24), (1, 31, 13, 39)):
            for x in range(x1 + 1, x2, 2):
                for z in range(z1 + 1, z2):
                    if (x * 7 + z * 3) % 5 != 0:
                        for y in range(F, F + 5):
                            self.set(x, y, z, pick(self, self.t.wall, x, y, z))
        for x, z in ((24, 22), (32, 22), (24, 34), (32, 34)):
            self.set(x, F, z, 'minecraft:skeleton_skull', rotation=str((x + z) % 16))

    def top_labyrinth(self):
        self.top_default()
        for x, z in ((14, 13), (26, 13), (14, 26), (26, 26)):
            for y in range(G, G + 7):
                self.set(x, y, z, 'minecraft:cut_sandstone' if y % 3 else 'minecraft:chiseled_sandstone')
            self.set(x, G + 7, z, 'minecraft:orange_terracotta')
        for x in range(15, 26):
            self.set(x, G, 26, 'minecraft:sandstone_wall', up='true', north='none', south='none', east='low', west='low', waterlogged='false')

    def deco_crypt(self):
        # саркофаги
        for (x, z) in ((3, 16), (3, 20), (8, 16), (8, 20), (3, 33), (8, 33), (3, 37), (8, 37)):
            self.set(x, F, z, 'minecraft:polished_deepslate')
            self.set(x, F, z + 1, 'minecraft:polished_deepslate')
            self.set(x, F + 1, z, 'minecraft:deepslate_tile_slab', type='bottom', waterlogged='false')
            self.set(x, F + 1, z + 1, 'minecraft:deepslate_tile_slab', type='bottom', waterlogged='false')
        for x in range(18, 40, 3):
            self.set(x, F + 10, 39, 'minecraft:cobweb')
        for x, z in ((25, 25), (31, 31), (25, 31), (31, 25)):
            self.set(x, F, z, 'minecraft:candle', candles='3', lit='true', waterlogged='false')

    def top_crypt(self):
        # мавзолей
        for x in range(15, 26):
            for z in range(13, 24):
                edge = x in (15, 25) or z in (13, 23)
                for y in range(G, G + 6):
                    if edge and not (19 <= x <= 21 and z == 13 and y < G + 4):
                        self.set(x, y, z, pick(self, self.t.wall, x, y, z))
                self.set(x, G + 6, z, 'minecraft:deepslate_tile_slab', type='bottom', waterlogged='false')
        for x in (16, 24):
            self.set(x, G + 7, 14, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
        self.set(20, G + 6, 18, 'minecraft:chiseled_deepslate')
        self.set(17, G, 14, 'minecraft:skeleton_skull', rotation='8')
        self.set(23, G, 14, 'minecraft:skeleton_skull', rotation='8')

    def deco_frost_citadel(self):
        for x in range(18, 40, 2):
            for z in (18, 39):
                self.set(x, F + 10, z, 'minecraft:ice')
        for x, z in ((23, 24), (33, 25), (25, 33), (33, 32)):
            for y in range(F, F + 3):
                self.set(x, y, z, 'minecraft:blue_ice')

    def top_frost_citadel(self):
        self.top_default()
        for (x, z, h) in ((14, 13, 9), (26, 13, 11), (14, 26, 8), (26, 26, 10), (20, 25, 12)):
            for y in range(G, G + h):
                r = 1 if y < G + h - 3 else 0
                for dx in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        self.set(x + dx, y, z + dz, 'minecraft:packed_ice' if (y + dx) % 4 else 'minecraft:blue_ice')
        for x in range(13, 28):
            for z in range(12, 28):
                if self.blocks.get((x, G, z), ('',))[0] == 'minecraft:air':
                    self.set(x, G, z, 'minecraft:snow', layers='1')

    def deco_crucible(self):
        # лавовые каналы под решётками
        for x in range(19, 39):
            for z in (24, 32):
                self.set(x, F - 1, z, 'minecraft:lava', level='0')
                self.set(x, F, z, 'minecraft:iron_bars', north='false', south='false', east='true', west='true', waterlogged='false')
        for x, z in ((24, 27), (32, 29)):
            self.set(x, F, z, 'minecraft:anvil', facing='north')
        for x, z in ((3, 15), (10, 23), (3, 38), (38, 3)):
            self.set(x, F, z, 'minecraft:blast_furnace', facing='east', lit='true')

    def top_crucible(self):
        # кратер с лавой и обсидиановым входом
        for x in range(13, 28):
            for z in range(12, 28):
                d = math.hypot(x - 20, z - 20)
                if 6 < d < 7.5:
                    for y in range(G, G + 2 + int((x + z) % 3)):
                        self.set(x, y, z, 'minecraft:basalt', axis='y')
                elif d <= 6 and not (19 <= x <= 21 and 13 <= z <= 19) and (x + z) % 5 == 0:
                    self.set(x, G - 1, z, 'minecraft:magma_block')
        self.arch(18, 22, 13, 5, 'minecraft:obsidian', 'minecraft:crying_obsidian')

    def deco_ash_citadel(self):
        for x in range(18, 40):
            for z in range(18, 40):
                if (x + z) % 9 == 0 and (x, F, z) in self.blocks and self.blocks[(x, F, z)][0] == 'minecraft:air' and abs(x - 28) + abs(z - 28) > 4:
                    self.set(x, F - 1, z, 'minecraft:soul_soil')
                    self.set(x, F, z, 'minecraft:soul_fire')
        for x, z in ((24, 24), (32, 24), (24, 32), (32, 32)):
            for y in range(F, F + 4):
                self.set(x, y, z, 'minecraft:crying_obsidian' if y == F + 3 else 'minecraft:polished_blackstone_bricks')

    def top_ash_citadel(self):
        # врата Пепла
        for x in range(14, 27):
            for z in range(13, 27):
                if x in (14, 26) or z in (13, 26):
                    for y in range(G, G + 4 + (x + z) % 3):
                        self.set(x, y, z, pick(self, self.t.wall, x, y, z))
        for x in range(19, 22):
            for y in range(G, G + 4):
                self.set(x, y, 13, 'minecraft:air')
        self.arch(17, 23, 13, 8, 'minecraft:red_nether_bricks', 'minecraft:crying_obsidian')
        for x, z in ((15, 14), (25, 14), (15, 25), (25, 25)):
            self.set(x, G + 5, z, 'minecraft:netherrack')
            self.set(x, G + 6, z, 'minecraft:fire', age='0', north='false', south='false', east='false', west='false', up='false')


THEMES = [
    Theme('ruins', 8, ['minecraft:stone_bricks', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks'],
          ['minecraft:stone_bricks', 'minecraft:cobblestone', 'minecraft:mossy_cobblestone'], 'minecraft:stone_bricks',
          'minecraft:chiseled_stone_bricks', 'lantern', 'minecraft:stone_brick_stairs',
          ['zombie', 'skeleton', 'zombie', 'spider'], (34, 12), 812331,
          ['minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:meadow', 'minecraft:forest', 'minecraft:birch_forest', 'minecraft:savanna'],
          ground=['minecraft:grass_block', 'minecraft:mossy_cobblestone', 'minecraft:cobblestone', 'minecraft:gravel']),
    Theme('rotgrove', 11, ['minecraft:mossy_cobblestone', 'minecraft:rooted_dirt', 'minecraft:mossy_stone_bricks', 'minecraft:dark_oak_log'],
          ['minecraft:moss_block', 'minecraft:coarse_dirt', 'minecraft:podzol', 'minecraft:rooted_dirt'], 'minecraft:rooted_dirt',
          'minecraft:dark_oak_log', 'minecraft:shroomlight', 'minecraft:mossy_cobblestone_stairs',
          ['zombie', 'spider', 'zombie', 'witch'], (36, 12), 812332,
          ['#minecraft:is_forest'], wall_light='minecraft:verdant_froglight', ground=['minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:moss_block'],
          arena_floor=['minecraft:moss_block', 'minecraft:rooted_dirt']),
    Theme('spider_nest', 14, ['minecraft:deepslate', 'minecraft:cobbled_deepslate', 'minecraft:tuff'],
          ['minecraft:tuff', 'minecraft:deepslate', 'minecraft:cobbled_deepslate'], 'minecraft:deepslate',
          'minecraft:cobbled_deepslate_wall', 'soul_lantern', 'minecraft:cobbled_deepslate_stairs',
          ['spider', 'cave_spider', 'spider', 'cave_spider'], (38, 14), 812333,
          ['#minecraft:is_jungle', 'minecraft:dark_forest', 'minecraft:old_growth_pine_taiga', 'minecraft:old_growth_spruce_taiga'],
          wall_light='minecraft:verdant_froglight', ground=['minecraft:coarse_dirt', 'minecraft:cobbled_deepslate', 'minecraft:podzol']),
    Theme('goblin_fort', 15, ['minecraft:cobblestone', 'minecraft:spruce_planks', 'minecraft:mossy_cobblestone', 'minecraft:stripped_spruce_log'],
          ['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'], 'minecraft:spruce_planks',
          'minecraft:spruce_log', 'lantern', 'minecraft:spruce_stairs',
          ['litrpg:goblin', 'litrpg:goblin', 'litrpg:goblin_shaman', 'skeleton'], (36, 12), 812334,
          ['minecraft:plains', 'minecraft:savanna', 'minecraft:meadow', 'minecraft:sunflower_plains', 'minecraft:windswept_savanna'],
          ground=['minecraft:coarse_dirt', 'minecraft:dirt_path', 'minecraft:packed_mud']),
    Theme('mire', 20, ['minecraft:mud_bricks', 'minecraft:packed_mud', 'minecraft:mangrove_roots', 'minecraft:mud_bricks'],
          ['minecraft:mud', 'minecraft:packed_mud', 'minecraft:mud_bricks'], 'minecraft:mangrove_planks',
          'minecraft:mangrove_log', 'minecraft:verdant_froglight', 'minecraft:mud_brick_stairs',
          ['drowned', 'witch', 'zombie', 'slime'], (40, 14), 812335,
          ['minecraft:swamp', 'minecraft:mangrove_swamp'], wall_light='minecraft:verdant_froglight', ground=['minecraft:mud', 'minecraft:grass_block']),
    Theme('labyrinth', 25, ['minecraft:sandstone', 'minecraft:cut_sandstone', 'minecraft:smooth_sandstone', 'minecraft:chiseled_sandstone'],
          ['minecraft:smooth_sandstone', 'minecraft:cut_sandstone'], 'minecraft:sandstone',
          'minecraft:chiseled_sandstone', 'lantern', 'minecraft:sandstone_stairs',
          ['husk', 'skeleton', 'husk', 'husk'], (44, 16), 812336,
          ['minecraft:desert', '#minecraft:is_badlands'], ground=['minecraft:sand', 'minecraft:smooth_sandstone', 'minecraft:sandstone']),
    Theme('crypt', 30, ['minecraft:deepslate_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:polished_blackstone_bricks'],
          ['minecraft:deepslate_tiles', 'minecraft:cracked_deepslate_tiles'], 'minecraft:deepslate_bricks',
          'minecraft:chiseled_deepslate', 'soul_lantern', 'minecraft:deepslate_brick_stairs',
          ['skeleton', 'zombie', 'litrpg:skeleton_warden', 'skeleton'], (44, 16), 812337,
          ['#minecraft:is_taiga', 'minecraft:dark_forest', 'minecraft:snowy_taiga'], wall_light='minecraft:pearlescent_froglight', ground=['minecraft:podzol', 'minecraft:deepslate_tiles']),
    Theme('frost_citadel', 38, ['minecraft:packed_ice', 'minecraft:packed_ice', 'minecraft:blue_ice', 'minecraft:snow_block'],
          ['minecraft:packed_ice', 'minecraft:snow_block'], 'minecraft:packed_ice',
          'minecraft:blue_ice', 'minecraft:sea_lantern', 'minecraft:polished_diorite_stairs',
          ['stray', 'stray', 'zombie', 'skeleton'], (48, 18), 812338,
          ['minecraft:snowy_plains', 'minecraft:ice_spikes', 'minecraft:snowy_taiga', 'minecraft:grove', 'minecraft:snowy_slopes'],
          wall_light='minecraft:sea_lantern', ground=['minecraft:snow_block', 'minecraft:packed_ice']),
    Theme('crucible', 45, ['minecraft:blackstone', 'minecraft:polished_blackstone_bricks', 'minecraft:basalt'],
          ['minecraft:polished_basalt', 'minecraft:blackstone', 'minecraft:polished_blackstone'], 'minecraft:blackstone',
          'minecraft:polished_blackstone_bricks', 'minecraft:shroomlight', 'minecraft:polished_blackstone_brick_stairs',
          ['blaze', 'magma_cube', 'wither_skeleton', 'blaze'], (52, 20), 812339,
          ['#minecraft:is_badlands', 'minecraft:savanna_plateau', 'minecraft:windswept_savanna'],
          ground=['minecraft:blackstone', 'minecraft:basalt', 'minecraft:coarse_dirt']),
    Theme('ash_citadel', 55, ['minecraft:nether_bricks', 'minecraft:red_nether_bricks', 'minecraft:polished_blackstone_bricks'],
          ['minecraft:blackstone', 'minecraft:polished_blackstone', 'minecraft:nether_bricks'], 'minecraft:nether_bricks',
          'minecraft:crying_obsidian', 'soul_lantern', 'minecraft:nether_brick_stairs',
          ['wither_skeleton', 'blaze', 'vindicator', 'wither_skeleton'], (64, 24), 812340,
          ['#minecraft:is_badlands', '#minecraft:is_savanna', 'minecraft:desert', '#minecraft:is_taiga', 'minecraft:plains',
           '#minecraft:is_forest', 'minecraft:snowy_plains'],
          wall_light='minecraft:pearlescent_froglight', ground=['minecraft:blackstone', 'minecraft:netherrack', 'minecraft:soul_soil']),
]

# что лежит в сундуках подземелий (кроме предметов Системы и монет — их добавляет мод)
LOOT_TIERS = [
    (0, [('minecraft:bread', 4, 1, 4), ('minecraft:iron_ingot', 4, 1, 4), ('minecraft:gold_ingot', 2, 1, 3), ('minecraft:arrow', 3, 4, 12),
         ('litrpg:mana_potion', 2, 1, 2), ('minecraft:experience_bottle', 2, 1, 3)]),
    (15, [('minecraft:cooked_beef', 3, 2, 5), ('minecraft:iron_ingot', 3, 2, 6), ('minecraft:gold_ingot', 3, 2, 5), ('minecraft:diamond', 1, 1, 2),
          ('litrpg:mana_potion', 3, 1, 3), ('minecraft:experience_bottle', 3, 2, 5), ('minecraft:golden_apple', 1, 1, 1)]),
    (30, [('minecraft:golden_carrot', 3, 2, 6), ('minecraft:diamond', 2, 1, 3), ('minecraft:emerald', 2, 2, 6), ('litrpg:mana_potion', 3, 2, 4),
          ('minecraft:experience_bottle', 3, 3, 8), ('minecraft:golden_apple', 2, 1, 2), ('litrpg:tome_of_experience', 1, 1, 1)]),
    (45, [('minecraft:diamond', 3, 2, 4), ('minecraft:netherite_scrap', 1, 1, 2), ('minecraft:emerald', 2, 3, 8), ('litrpg:mana_potion', 3, 2, 5),
          ('minecraft:experience_bottle', 3, 4, 10), ('minecraft:enchanted_golden_apple', 1, 1, 1), ('litrpg:tome_of_experience', 2, 1, 1)]),
]


def loot_table(level):
    entries = None
    for lvl, items in LOOT_TIERS:
        if level >= lvl:
            entries = items
    pool = {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": [
        {"type": "minecraft:item", "name": n, "weight": w,
         "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
        for n, w, lo, hi in entries]}
    return {"type": "minecraft:chest", "pools": [pool]}


def main():
    wg = os.path.join(ROOT, 'worldgen')
    tags = os.path.join(ROOT, 'tags', 'worldgen', 'biome', 'has_structure')
    loot_dir = os.path.join(ROOT, 'loot_tables', 'chests')
    for d in (os.path.join(wg, 'structure'), os.path.join(wg, 'structure_set'), tags, loot_dir):
        os.makedirs(d, exist_ok=True)
    for t in THEMES:
        name = 'dungeon_' + t.key
        b = Dungeon(t)
        b.layout()
        n_blocks, n_ents = b.save(name)
        json.dump(loot_table(t.level), open(os.path.join(loot_dir, name + '.json'), 'w'), indent=1)
        json.dump({"replace": False, "values": t.biomes}, open(os.path.join(tags, name + '.json'), 'w'), indent=2)
        os.makedirs(os.path.join(wg, 'template_pool', name), exist_ok=True)
        json.dump({"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": "litrpg:" + name,
            "projection": "rigid", "processors": "minecraft:empty"}}]},
            open(os.path.join(wg, 'template_pool', name, 'start.json'), 'w'), indent=2)
        json.dump({"type": "minecraft:jigsaw", "biomes": "#litrpg:has_structure/" + name, "step": "surface_structures",
                   "spawn_overrides": {}, "terrain_adaptation": "none", "start_pool": f"litrpg:{name}/start", "size": 1,
                   "start_height": {"absolute": -G}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                   "max_distance_from_center": 80, "use_expansion_hack": False},
                  open(os.path.join(wg, 'structure', name + '.json'), 'w'), indent=2)
        spacing, sep = t.spacing
        json.dump({"structures": [{"structure": "litrpg:" + name, "weight": 1}],
                   "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": t.salt}},
                  open(os.path.join(wg, 'structure_set', name + '.json'), 'w'), indent=2)
        print(f'{name}: {n_blocks} blocks, {n_ents} mobs')


if __name__ == '__main__':
    main()
