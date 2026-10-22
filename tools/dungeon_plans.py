"""
Планы подземелий (2.16): у каждого — своё устройство. Шаблон 41×41 сначала целиком заливается
камнем темы, затем в нём «вырезаются» залы, пещеры, лабиринт, катакомбы. Арена — всегда 18..39 × 18..39,
Печать в (28, F, 28); вход на арену — западная дверь (x=17) и/или северная (z=17).
Арены подогнаны под механики хранителей: узлы силы (litrpg:relic), костры Имира, острова Игниса,
колонны Минотавра, вода Морры.
"""
import math
import random

F = 2           # пол подземелья (воздух начинается с F)
TOP = F + 11    # потолок арены


class Plans:
    # ================= примитивы вырезания =================

    def solid(self):
        """Залить весь объём подземелья камнем темы."""
        for x in range(0, 41):
            for z in range(0, 41):
                for y in range(F - 2, TOP + 1):
                    self.wallb(x, y, z)

    def carve(self, x1, z1, x2, z2, h=5, y0=F):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(y0, y0 + h):
                    self.set(x, y, z, 'minecraft:air')
                self.open_cells.add((x, z))

    def carve_round(self, cx, cz, r, h=6, dome=True):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz)
                if d > r or not (1 <= x <= 39 and 1 <= z <= 39):
                    continue
                hh = h if not dome else max(3, int(h - (d / r) ** 2 * (h - 3)))
                for y in range(F, F + hh):
                    self.set(x, y, z, 'minecraft:air')
                self.open_cells.add((x, z))

    def tunnel(self, pts, r=2.2, h=4):
        """Извилистый пещерный ход по точкам (с небольшим «шумом» ширины)."""
        for (ax, az), (bx, bz) in zip(pts, pts[1:]):
            n = int(math.hypot(bx - ax, bz - az) * 2) + 1
            for i in range(n + 1):
                t = i / n
                x, z = ax + (bx - ax) * t, az + (bz - az) * t
                rr = r + math.sin((x + z) * 0.9 + self.seed) * 0.5
                self.carve_round(x, z, rr, h, dome=False)

    def floor_all(self, block=None):
        """Пол под всеми вырезанными клетками (если тема не положила свой)."""
        for (x, z) in self.open_cells:
            if (x, F - 1, z) in self.keep:
                continue
            if self.blocks.get((x, F, z), ('',))[0] == 'minecraft:air':
                self.set(x, F - 1, z, pick(self, block or self.t.floor, x, F - 1, z))

    def lights(self, step=5):
        """Светильники в потолке над открытыми клетками."""
        for (x, z) in sorted(self.open_cells):
            if (x * 3 + z * 7) % (step * 2 + 1) != 0 or (x, z) in self.no_light:
                continue
            y = F
            while self.blocks.get((x, y + 1, z), ('',))[0] == 'minecraft:air' and y < TOP:
                y += 1
            if y <= F + 1:
                continue
            l = self.t.light
            if l in ('lantern', 'soul_lantern'):
                self.set(x, y, z, 'minecraft:' + l, hanging='true', waterlogged='false')
            else:
                self.set(x, y + 1, z, l)

    def keepb(self, x, y, z, block, **props):
        self.set(x, y, z, block, **props)
        self.keep.add((x, y, z))

    def mobs_at(self, spots, kinds, lvl=None):
        for i, (x, z) in enumerate(spots):
            self.mob(x, z, kinds[i % len(kinds)], lvl=(lvl or self.t.level) - 2 + i % 3)

    def scatter_mobs(self, x1, z1, x2, z2, n, kinds=None):
        r = random.Random(self.seed + x1 * 31 + z1)
        spots = []
        for _ in range(n * 6):
            x, z = r.randint(x1, x2), r.randint(z1, z2)
            if (x, z) in self.open_cells and self.blocks.get((x, F, z), ('',))[0] == 'minecraft:air' and (x, z) not in spots:
                spots.append((x, z))
            if len(spots) >= n:
                break
        self.mobs_at(spots, kinds or self.t.mobs)

    def arrow_trap(self, px, pz, dx, dz, item='minecraft:arrow'):
        """Нажимная плита в (px,pz); раздатчик в стене рядом, смотрит на плиту. dx,dz — где стена относительно плиты."""
        face = {(1, 0): 'west', (-1, 0): 'east', (0, 1): 'north', (0, -1): 'south'}[(dx, dz)]
        self.keepb(px, F, pz, 'minecraft:stone_pressure_plate', powered='false')
        self.set(px + dx, F, pz + dz, 'minecraft:dispenser', facing=face, triggered='false')
        self.nbt[(px + dx, F, pz + dz)] = {'id': 'minecraft:dispenser', 'Items': TList(10, [
            {'Slot': TByte(i), 'id': item, 'Count': TByte(16)} for i in range(4)])}

    def relic(self, x, z, kind, pedestal=None):
        y = F
        if pedestal:
            self.set(x, F, z, pedestal)
            y = F + 1
        self.set(x, y, z, 'litrpg:relic', kind=kind)

    def door_west(self, z1=27, z2=29, h=4):
        self.carve(14, z1, 17, z2, h)

    def door_north(self, x1=33, x2=35, h=4):
        self.carve(x1, 14, x2, 17, h)

    def entrance(self, x1=16, z1=1, x2=24, z2=10, h=5):
        self.carve(x1, z1, x2, z2, h)

    def arena_box(self, floor=None, h=11):
        self.carve(18, 18, 39, 39, h)
        fl = floor or self.t.kw.get('arena_floor') or self.t.floor
        for x in range(18, 40):
            for z in range(18, 40):
                self.keepb(x, F - 1, z, pick(self, fl, x, F - 1, z))

    def altar(self):
        t = self.t
        ax, az = 28, 28
        self.set(ax, F, az, 'litrpg:boss_altar')
        self.nbt[(ax, F, az)] = {'id': 'litrpg:boss_altar', 'Dungeon': t.key,
                                 'Box': TList(3, [TInt(v) for v in (-28, -3, -28, 12, 11, 12)])}
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                if abs(dx) == 2 or abs(dz) == 2:
                    self.keepb(ax + dx, F - 1, az + dz, pick(self, t.accent, ax + dx, 0, az + dz))
        self.no_light.update({(ax + dx, az + dz) for dx in range(-1, 2) for dz in range(-1, 2)})
        self.chest_at(27, 38, 'north')
        self.chest_at(29, 38, 'north')

    def pillar(self, x, z, block, w=2, h=11):
        for dx in range(w):
            for dz in range(w):
                for y in range(F, F + h):
                    self.set(x + dx, y, z + dz, pick(self, block, x + dx, y, z + dz))
                self.no_light.add((x + dx, z + dz))

    # ================= 1. Руины Стражей =================

    def plan_ruins(self):
        self.entrance()
        self.carve(8, 5, 15, 7)                 # к западному залу — коридор с ловушкой
        self.carve(5, 5, 7, 12)
        self.carve(1, 12, 12, 24)               # западный зал (обвалы)
        self.carve(5, 24, 7, 29)
        self.carve(1, 29, 14, 39)               # юго-западный зал
        self.carve(14, 32, 17, 34)              # → арена
        self.carve(24, 5, 27, 7)
        self.carve(27, 1, 39, 12)               # восточный зал
        self.carve(33, 14, 38, 16, 4)           # тайник за треснувшей стеной
        for x in range(34, 37):
            for y in range(F, F + 3):
                self.set(x, y, 13, 'minecraft:cracked_stone_bricks')
        self.chest_at(35, 15, 'north')
        self.chest_at(36, 15, 'north')
        self.set(34, F, 15, 'minecraft:skeleton_skull', rotation='0')
        self.arrow_trap(11, 5, 0, -1)
        self.arrow_trap(13, 7, 0, 1)
        r = random.Random(5)
        for _ in range(14):                     # завалы
            x, z = r.randint(2, 11), r.randint(13, 23)
            for y in range(F, F + r.randint(1, 3)):
                self.set(x, y, z, r.choice(['minecraft:mossy_cobblestone', 'minecraft:cobblestone', 'minecraft:cracked_stone_bricks']))
        for x, z in ((3, 31), (9, 35), (12, 30)):
            self.set(x, F, z, 'minecraft:cobblestone')
            self.set(x, F + 1, z, 'minecraft:cobblestone_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
        self.chest_at(1, 14, 'east')
        self.chest_at(39, 1, 'west')
        self.scatter_mobs(1, 12, 12, 24, 5)
        self.scatter_mobs(1, 29, 14, 39, 5)
        self.scatter_mobs(27, 1, 39, 12, 4)
        self.arena_box(['minecraft:stone_bricks', 'minecraft:cracked_stone_bricks', 'minecraft:mossy_stone_bricks'])
        for x, z in ((22, 22), (33, 22), (22, 33), (33, 33)):
            self.pillar(x, z, ['minecraft:stone_bricks', 'minecraft:cracked_stone_bricks', 'minecraft:mossy_stone_bricks'])
        r = random.Random(7)
        for _ in range(10):
            x, z = r.randint(19, 38), r.randint(19, 38)
            if abs(x - 28) + abs(z - 28) > 4:
                self.set(x, F, z, r.choice(['minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:stone_brick_slab']),
                         **({'type': 'bottom', 'waterlogged': 'false'} if r.random() < 0.3 else {}))
        for x in range(19, 39, 3):
            self.set(x, F + 10, 19, 'minecraft:cobweb')

    # ================= 2. Гниющая Роща =================

    def plan_rotgrove(self):
        self.entrance()
        self.tunnel([(18, 6), (10, 5), (6, 12), (5, 19), (9, 25), (7, 32), (12, 35), (20, 33)], 2.3, 5)
        self.carve_round(6, 18, 4.5, 6)          # грибная пещера
        self.carve_round(8, 33, 4, 6)
        self.tunnel([(22, 6), (30, 5), (35, 10)], 2.0, 4)
        self.carve_round(35, 10, 4.5, 6)        # гнилой колодец
        self.carve_round(28.5, 28.5, 10.8, 11)  # роща-арена
        for (x, z) in list(self.open_cells):
            if (x * 7 + z * 13) % 9 == 0 and self.blocks.get((x, F, z), ('',))[0] == 'minecraft:air' and not (18 <= x <= 39 and 18 <= z <= 39):
                self.set(x, F, z, 'minecraft:brown_mushroom' if (x + z) % 2 else 'minecraft:red_mushroom')
        for x, z in ((5, 17), (7, 20), (35, 9)):
            self.set(x, F, z, 'minecraft:moss_carpet')
        self.chest_at(4, 16, 'east')
        self.chest_at(37, 12, 'west')
        self.scatter_mobs(1, 12, 13, 26, 5)
        self.scatter_mobs(3, 28, 14, 38, 4)
        self.scatter_mobs(30, 5, 39, 14, 3)
        for x in range(18, 40):
            for z in range(18, 40):
                if (x, z) in self.open_cells:
                    self.keepb(x, F - 1, z, pick(self, ['minecraft:moss_block', 'minecraft:rooted_dirt', 'minecraft:moss_block', 'minecraft:podzol'], x, 0, z))
        for i, a in enumerate((0.785, 2.356, 3.927, 5.498)):
            x, z = int(28.5 + math.cos(a) * 7), int(28.5 + math.sin(a) * 7)
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                self.set(x + dx, F, z + dz, 'minecraft:mangrove_roots', waterlogged='false')
            self.relic(x, z, 'nature', pedestal='minecraft:rooted_dirt')
            self.set(x, F + 2, z, 'minecraft:azalea_leaves', persistent='true', distance='1', waterlogged='false')
        for x in range(19, 39, 2):
            for z in range(19, 39, 4):
                if (x + z) % 3 == 0:
                    self.set(x, F + 9, z, 'minecraft:hanging_roots', waterlogged='false')
        self.set(28, F + 10, 28, 'minecraft:spore_blossom')

    # ================= 3. Паучье Логово =================

    def plan_spider_nest(self):
        self.entrance()
        self.tunnel([(18, 7), (12, 9), (8, 6), (4, 11), (6, 17)], 1.8, 4)
        self.carve_round(6, 21, 5, 7)            # кладка яиц 1
        self.tunnel([(8, 25), (5, 30), (9, 35), (15, 33), (20, 32)], 1.8, 4)
        self.carve_round(9, 35, 4, 6)            # кладка яиц 2
        self.tunnel([(22, 7), (29, 9), (34, 6)], 1.7, 4)
        self.carve_round(34, 7, 4.5, 6)          # гнездо охотников
        self.carve_round(28.5, 28.5, 11, 11)
        for (x, z) in list(self.open_cells):
            for y in (F + 2, F + 3, F + 5, F + 7, F + 9):
                if self.blocks.get((x, y, z), ('',))[0] == 'minecraft:air' and (x * 31 + z * 17 + y * 7) % 7 == 0 \
                        and math.hypot(x - 28.5, z - 28.5) > 5:
                    self.set(x, y, z, 'minecraft:cobweb')
        for cx, cz in ((6, 21), (9, 35)):
            for a in range(6):
                x, z = int(cx + math.cos(a) * 3), int(cz + math.sin(a) * 3)
                self.set(x, F, z, 'minecraft:white_wool')
                self.set(x, F + 1, z, 'minecraft:cobweb')
            self.mobs_at([(cx + dx, cz + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (2, 0))],
                         ['cave_spider'])
        self.scatter_mobs(28, 3, 39, 12, 4, ['spider'])
        self.chest_at(4, 20, 'east')
        self.chest_at(36, 8, 'west')
        for i in range(5):
            a = i * 2 * math.pi / 5 + 0.3
            x, z = int(28.5 + math.cos(a) * 8), int(28.5 + math.sin(a) * 8)
            self.relic(x, z, 'web', pedestal='minecraft:white_wool')
            self.set(x, F + 2, z, 'minecraft:cobweb')

    # ================= 4. Гоблинская Крепость =================

    def plan_goblin_fort(self):
        self.entrance()
        self.carve(10, 4, 15, 6)
        self.carve(1, 2, 13, 15)                 # казармы: орда гоблинов
        for x in range(2, 13, 3):
            self.set(x, F, 3, 'minecraft:hay_block', axis='y')
            self.set(x, F, 14, 'minecraft:hay_block', axis='y')
        self.set(7, F, 8, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        self.mobs_at([(3, 6), (5, 9), (9, 5), (10, 11), (4, 12), (11, 8), (8, 13), (6, 4), (12, 3)],
                     ['litrpg:goblin', 'litrpg:goblin', 'litrpg:goblin_shaman'])
        self.carve(5, 15, 8, 19)
        self.carve(1, 19, 14, 39)                # яма с кольями и мостик
        for x in range(2, 14):
            for z in range(23, 35):
                if x == 8:
                    continue
                self.keepb(x, F - 1, z, 'minecraft:pointed_dripstone', vertical_direction='up', thickness='tip', waterlogged='false')
                self.keepb(x, F - 2, z, 'minecraft:magma_block')
        for z in range(23, 35):
            self.keepb(8, F - 1, z, 'minecraft:spruce_planks')
            self.set(7, F, z, 'minecraft:spruce_fence', north='true', south='true', east='false', west='false', waterlogged='false')
        self.carve(14, 30, 17, 32)
        self.mobs_at([(4, 21), (11, 21), (5, 37), (11, 36)], ['litrpg:goblin', 'skeleton'])
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 12)                # склад награбленного
        for x, z in ((29, 2), (31, 2), (33, 2), (37, 4), (37, 7)):
            self.set(x, F, z, 'minecraft:barrel', facing='up', open='false')
            self.set(x, F + 1, z, 'minecraft:barrel', facing='up', open='false')
        self.chest_at(38, 10, 'west')
        self.chest_at(30, 10, 'north')
        self.scatter_mobs(28, 3, 38, 11, 4, ['litrpg:goblin', 'litrpg:goblin_shaman'])
        self.arena_box(['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'])
        for x in range(26, 31):                  # трон
            self.set(x, F, 37, 'minecraft:gold_block')
        self.set(28, F + 1, 37, 'minecraft:gold_block')
        self.set(28, F + 1, 36, 'minecraft:spruce_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
        for x in (26, 30):
            for y in range(F + 1, F + 4):
                self.set(x, y, 37, 'minecraft:spruce_log', axis='y')
            self.set(x, F + 4, 37, 'minecraft:skeleton_skull', rotation='0')
        for x, z in ((21, 21), (35, 21), (21, 35), (35, 35)):
            for y in range(F, F + 11):
                self.set(x, y, z, 'minecraft:spruce_log', axis='y')

    # ================= 5. Проклятая Топь =================

    def plan_mire(self):
        self.entrance()
        self.carve(1, 1, 15, 39, 6)              # затопленный зал
        self.carve(15, 4, 16, 7)
        for x in range(1, 16):
            for z in range(1, 40):
                self.keepb(x, F - 2, z, 'minecraft:mud')
                self.keepb(x, F - 1, z, 'minecraft:water', level='0')
        path = [(15, 5), (12, 5), (12, 12), (5, 12), (5, 22), (11, 22), (11, 30), (14, 30), (14, 34)]
        for (ax, az), (bx, bz) in zip(path, path[1:]):
            for x in range(min(ax, bx), max(ax, bx) + 1):
                for z in range(min(az, bz), max(az, bz) + 1):
                    self.keepb(x, F - 1, z, 'minecraft:mangrove_planks')
        for x, z in ((3, 4), (9, 17), (3, 28), (9, 36), (13, 20)):
            for y in range(F, F + 6):
                self.set(x, y, z, 'minecraft:mangrove_log', axis='y')
        for x in range(2, 7):                     # хижина на островке
            for z in range(33, 38):
                self.keepb(x, F - 1, z, 'minecraft:mud_bricks')
        self.set(4, F, 35, 'minecraft:cauldron')
        self.chest_at(2, 37, 'east')
        self.set(5, F, 37, 'minecraft:brewing_stand', has_bottle_0='false', has_bottle_1='false', has_bottle_2='false')
        self.mobs_at([(3, 8), (9, 9), (2, 18), (8, 26), (3, 24), (12, 37), (7, 15)], ['drowned', 'drowned', 'witch', 'slime'])
        self.carve(14, 33, 17, 35)
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 12)                 # гнилые погреба
        for x, z in ((30, 3), (34, 3), (38, 6)):
            self.set(x, F, z, 'minecraft:cauldron')
        self.chest_at(38, 11, 'west')
        self.scatter_mobs(28, 2, 38, 11, 3, ['witch', 'zombie'])
        self.carve(18, 18, 39, 39, 11)
        for x in range(18, 40):
            for z in range(18, 40):
                self.keepb(x, F - 2, z, 'minecraft:mud')
                self.keepb(x, F - 1, z, 'minecraft:water', level='0')
        for cx, cz in ((22, 22), (34, 22), (22, 34), (34, 34), (28, 28)):
            r = 2 if (cx, cz) != (28, 28) else 3
            for x in range(cx - r, cx + r + 1):
                for z in range(cz - r, cz + r + 1):
                    self.keepb(x, F - 1, z, pick(self, ['minecraft:mud_bricks', 'minecraft:packed_mud', 'minecraft:moss_block'], x, 0, z))
        self.set(34, F, 21, 'minecraft:cauldron')
        for x in range(19, 39, 4):
            self.set(x, F + 10, 30, 'minecraft:vine', north='false', south='true', east='false', west='false', up='true')

    # ================= 6. Лабиринт Астериона =================

    def plan_labyrinth(self):
        self.entrance()
        r = random.Random(self.seed)
        # два лабиринта: западный (x 1..15, z 1..39) и восточный (x 26..39, z 1..15); клетка — 2×2, стены по 1
        for ox, oz, nx, nz, exits in ((1, 1, 5, 13, 'west'), (26, 1, 5, 5, 'east')):
            cells = {(i, j) for i in range(nx) for j in range(nz)}
            start = (0, 0) if exits == 'west' else (0, 1)
            seen, stack = {start}, [start]
            deg = {c: 0 for c in cells}
            while stack:
                c = stack[-1]
                nb = [(c[0] + dx, c[1] + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (c[0] + dx, c[1] + dz) in cells
                      and (c[0] + dx, c[1] + dz) not in seen]
                if not nb:
                    stack.pop()
                    continue
                n = r.choice(nb)
                seen.add(n)
                deg[c] += 1
                deg[n] += 1
                x1, z1 = ox + 3 * c[0], oz + 3 * c[1]
                x2, z2 = ox + 3 * n[0], oz + 3 * n[1]
                self.carve(min(x1, x2), min(z1, z2), max(x1, x2) + 1, max(z1, z2) + 1, 4)
                stack.append(n)
            dead = sorted(c for c in cells if deg[c] == 1 and c != start)
            for c in cells:
                x, z = ox + 3 * c[0], oz + 3 * c[1]
                self.carve(x, z, x + 1, z + 1, 4)
            # тупики: сундуки, ловушки, стражи
            for k, c in enumerate(dead):
                x, z = ox + 3 * c[0], oz + 3 * c[1]
                if k % 3 == 0:
                    self.chest_at(x, z, 'north')
                elif k % 3 == 1:
                    self.mob(x, z, r.choice(self.t.mobs))
                else:
                    self.mob(x + 1, z + 1, 'husk')
            # ловушки-плиты на развилках
            for c in r.sample(sorted(cells), 6):
                x, z = ox + 3 * c[0], oz + 3 * c[1]
                if self.blocks.get((x - 1, F, z), ('',))[0] != 'minecraft:air' and x - 1 >= 1:
                    self.arrow_trap(x, z, -1, 0)
        self.carve(15, 4, 16, 6, 4)          # вход в западный лабиринт
        self.carve(24, 4, 26, 6, 4)          # вход в восточный
        self.carve(14, 37, 17, 38, 4)        # западный лабиринт → арена
        self.carve(38, 14, 39, 17, 4)        # восточный → арена
        self.arena_box(['minecraft:smooth_sandstone', 'minecraft:cut_sandstone', 'minecraft:sandstone'])
        # колонны, которые Минотавр рушит тараном
        for x, z in ((22, 22), (33, 22), (22, 33), (33, 33), (27, 20), (27, 36)):
            self.pillar(x, z, ['minecraft:cut_red_sandstone', 'minecraft:cut_red_sandstone', 'minecraft:chiseled_red_sandstone'])
        for x, z in ((24, 26), (32, 26), (24, 30), (32, 30)):
            self.set(x, F, z, 'minecraft:skeleton_skull', rotation=str((x + z) % 16))

    # ================= 7. Склеп Лича =================

    def plan_crypt(self):
        self.entrance()
        # катакомбы: сетка ходов с погребальными нишами
        for x1 in (3, 10):
            self.carve(x1, 2, x1 + 2, 38, 4)
        for z1 in (3, 13, 23, 33):
            self.carve(2, z1, 14, z1 + 2, 4)
        self.carve(13, 4, 16, 6, 4)
        for x1 in (3, 10):
            for z in range(3, 38, 3):
                for side in (-1, 3):
                    x = x1 + side
                    if 1 <= x <= 14 and self.blocks.get((x, F + 1, z), ('',))[0] != 'minecraft:air':
                        self.set(x, F + 1, z, 'minecraft:air')
                        self.set(x, F + 2, z, 'minecraft:air')
                        self.set(x, F, z, 'minecraft:bone_block', axis='y')
                        if (x + z) % 4 == 0:
                            self.set(x, F + 1, z, 'minecraft:skeleton_skull', rotation='4' if side < 0 else '12')
        self.mobs_at([(4, 8), (11, 10), (4, 18), (11, 20), (4, 28), (11, 30), (7, 14), (7, 24), (7, 34)],
                     ['skeleton', 'zombie', 'litrpg:skeleton_warden'])
        self.carve(14, 33, 17, 35, 4)
        # оссуарий и тайная библиотека за книжными полками
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 9, 5)
        for x in range(28, 39):
            for y in range(F, F + 2):
                if (x + y) % 2 == 0:
                    self.set(x, y, 1, 'minecraft:bone_block', axis='y')
        self.scatter_mobs(28, 2, 38, 8, 4, ['skeleton', 'litrpg:skeleton_warden'])
        for x in range(29, 37):
            for y in range(F, F + 3):
                self.set(x, y, 10, 'minecraft:bookshelf')
        self.carve(29, 11, 37, 15, 4)
        for x in range(29, 38):
            self.set(x, F, 15, 'minecraft:bookshelf')
        self.set(33, F, 13, 'minecraft:lectern', facing='south', has_book='false', powered='false')
        self.chest_at(30, 12, 'east')
        self.chest_at(36, 12, 'west')
        self.chest_at(2, 37, 'east')
        self.arena_box(['minecraft:deepslate_tiles', 'minecraft:cracked_deepslate_tiles', 'minecraft:polished_deepslate'])
        for x, z in ((22, 22), (34, 22), (22, 34), (34, 34)):
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    self.keepb(x + dx, F - 1, z + dz, 'minecraft:polished_blackstone_bricks')
            self.relic(x, z, 'soul', pedestal='minecraft:polished_blackstone')
            self.set(x + 1, F, z, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
        for (x, z) in ((20, 26), (20, 30), (36, 26), (36, 30)):   # саркофаги
            self.set(x, F, z, 'minecraft:polished_deepslate')
            self.set(x, F, z + 1, 'minecraft:polished_deepslate')
            self.set(x, F + 1, z, 'minecraft:deepslate_tile_slab', type='bottom', waterlogged='false')
            self.set(x, F + 1, z + 1, 'minecraft:deepslate_tile_slab', type='bottom', waterlogged='false')

    # ================= 8. Ледяная Цитадель =================

    def plan_frost_citadel(self):
        self.entrance()
        self.carve(10, 4, 15, 6)
        self.carve(1, 2, 13, 16, 6)               # зал статуй
        for x, z in ((3, 4), (3, 9), (3, 14), (11, 4), (11, 14)):
            for y in range(F, F + 6):
                self.set(x, y, z, 'minecraft:packed_ice' if y % 2 else 'minecraft:blue_ice')
        self.mobs_at([(6, 6), (8, 11), (5, 13), (9, 3)], ['stray', 'stray', 'skeleton'])
        self.carve(5, 16, 8, 20)
        self.carve(1, 20, 15, 39, 6)              # расселина с ледяным мостом
        for x in range(1, 16):
            for z in range(26, 33):
                if x == 8:
                    self.keepb(x, F - 1, z, 'minecraft:blue_ice')
                else:
                    self.keepb(x, F - 1, z, 'minecraft:powder_snow')
                    self.keepb(x, F - 2, z, 'minecraft:powder_snow')
        self.mobs_at([(4, 22), (12, 23), (4, 36), (12, 37)], ['stray', 'zombie'])
        self.carve(14, 34, 17, 36)
        self.chest_at(1, 38, 'east')
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 12, 5)              # кладовая льда
        self.chest_at(38, 2, 'west')
        self.scatter_mobs(28, 2, 38, 11, 3, ['stray'])
        self.arena_box(['minecraft:packed_ice', 'minecraft:snow_block', 'minecraft:packed_ice'])
        for x, z in ((22, 22), (34, 22), (22, 34), (34, 34)):    # костры — единственное тепло
            self.keepb(x, F - 1, z, 'minecraft:stone_bricks')
            self.set(x, F, z, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        for x, z in ((28, 20), (28, 37)):                          # сердца стужи
            self.relic(x, z, 'frost', pedestal='minecraft:blue_ice')
        for x, z in ((19, 19), (38, 19), (19, 38), (38, 38)):
            for y in range(F, F + 8):
                self.set(x, y, z, 'minecraft:blue_ice')

    # ================= 9. Пламенное Горнило =================

    def plan_crucible(self):
        self.entrance()
        self.carve(10, 4, 15, 6)
        self.carve(1, 2, 14, 39, 6)               # кузнечный зал с рекой лавы
        for x in range(1, 15):
            for z in range(17, 21):
                if x in (7, 8):
                    self.keepb(x, F - 1, z, 'minecraft:basalt', axis='y')
                else:
                    self.keepb(x, F - 1, z, 'minecraft:lava', level='0')
                    self.no_light.add((x, z))
        for x in range(1, 15):
            for z in (16, 21):
                if x not in (7, 8):
                    self.set(x, F, z, 'minecraft:blackstone_wall', up='true', north='none', south='none', east='none', west='none', waterlogged='false')
        for z in range(27, 36, 2):                # огненные ловушки
            self.arrow_trap(2, z, -1, 0, 'minecraft:fire_charge')
            self.arrow_trap(13, z + 1, 1, 0, 'minecraft:fire_charge')
        for x, z in ((3, 4), (11, 9), (3, 38), (12, 37)):
            self.set(x, F, z, 'minecraft:blast_furnace', facing='east', lit='true')
        self.set(6, F, 10, 'minecraft:anvil', facing='north')
        self.mobs_at([(5, 7), (10, 12), (4, 24), (11, 25), (6, 31), (9, 37)], self.t.mobs)
        self.carve(14, 31, 17, 33)
        self.chest_at(1, 13, 'east')
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 12, 5)
        self.chest_at(38, 11, 'west')
        self.scatter_mobs(28, 2, 38, 11, 3)
        self.arena_crucible()

    def arena_crucible(self):
        self.arena_box(['minecraft:blackstone', 'minecraft:magma_block', 'minecraft:polished_blackstone', 'minecraft:blackstone'])
        for x, z in ((22, 22), (34, 22), (22, 34), (34, 34)):      # острова — спасение от Прилива магмы
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    self.set(x + dx, F, z + dz, 'minecraft:crying_obsidian' if dx == dz == 0 else 'minecraft:obsidian')
        for x in range(19, 39):                                     # лава за решётками вдоль стен
            for z in (18, 39):
                self.keepb(x, F - 1, z, 'minecraft:lava', level='0')
                self.set(x, F, z, 'minecraft:iron_bars', north='false', south='false', east='true', west='true', waterlogged='false')

    def plan_nether_heart(self):
        self.plan_crucible()

    # ================= 10. Цитадель Пепла =================

    def plan_ash_citadel(self):
        self.entrance()
        self.carve(10, 4, 15, 6)
        self.carve(1, 2, 14, 39, 7)               # зал испытания: волна за волной
        for z in range(4, 38, 4):
            for x in (2, 13):
                self.keepb(x, F - 1, z, 'minecraft:soul_soil')
                self.set(x, F, z, 'minecraft:soul_fire')
        for z in range(6, 38, 8):
            for x in (4, 11):
                for y in range(F, F + 7):
                    self.set(x, y, z, 'minecraft:polished_blackstone_bricks' if y % 3 else 'minecraft:gilded_blackstone')
        self.mobs_at([(5, 8), (9, 9), (6, 14), (10, 16), (5, 21), (9, 22), (6, 27), (10, 29), (5, 33), (9, 35), (7, 19), (8, 31)],
                     self.t.mobs)
        self.carve(14, 34, 17, 36)
        self.chest_at(1, 3, 'east')
        self.carve(24, 4, 27, 6)
        self.carve(27, 1, 39, 12, 6)               # тронный покой
        self.scatter_mobs(28, 2, 38, 11, 5)
        self.chest_at(38, 2, 'west')
        self.arena_box(['minecraft:blackstone', 'minecraft:polished_blackstone', 'minecraft:nether_bricks'])
        for x, z in ((22, 22), (34, 22), (22, 34), (34, 34)):      # якоря душ
            self.relic(x, z, 'void', pedestal='minecraft:crying_obsidian')
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                self.keepb(x + dx, F - 1, z + dz, 'minecraft:soul_soil')
                self.set(x + dx, F, z + dz, 'minecraft:soul_fire')
        for x, z in ((19, 28), (38, 28), (28, 19), (28, 38)):
            for y in range(F, F + 11):
                self.set(x, y, z, 'minecraft:crying_obsidian' if y == F + 5 else 'minecraft:polished_blackstone_bricks')


# NBT-типы берём из gen_structures (импорт здесь, чтобы не было цикла)
from gen_structures import TList, TInt, TByte  # noqa: E402


def pick(b, opts, x, y, z):
    if isinstance(opts, str):
        return opts
    i = (x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ b.seed) % len(opts)
    return opts[i]
