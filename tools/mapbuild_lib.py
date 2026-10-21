"""
Библиотека построек для карты «Мира Системы»: дома с интерьерами, башни,
стены с воротами, рынки, фонтаны, деревья, фонари, статуи. Стили народов.
Используется tools/gen_mapbuild.py.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_structures import Build, TInt, TByte, TDouble, TList, write_nbt, ROOT as DATA_LITRPG  # noqa: E402,F401

DIRS = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}
OPP = {'north': 'south', 'south': 'north', 'west': 'east', 'east': 'west'}
FACING_BYTE = {'south': 0, 'west': 1, 'north': 2, 'east': 3}

STYLES = {
    'human': dict(found=['minecraft:stone_bricks', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks'],
                  wall=['minecraft:calcite', 'minecraft:calcite', 'minecraft:polished_diorite'],
                  frame='minecraft:dark_oak_log', floor='minecraft:spruce_planks', plank='minecraft:spruce_planks',
                  roofs=[('minecraft:deepslate_tile_stairs', 'minecraft:deepslate_tile_slab', 'minecraft:deepslate_tiles'),
                         ('minecraft:brick_stairs', 'minecraft:brick_slab', 'minecraft:bricks'),
                         ('minecraft:dark_oak_stairs', 'minecraft:dark_oak_slab', 'minecraft:dark_oak_planks'),
                         ('minecraft:dark_prismarine_stairs', 'minecraft:dark_prismarine_slab', 'minecraft:dark_prismarine')],
                  door='minecraft:spruce_door', glass='minecraft:glass_pane', fence='minecraft:spruce_fence',
                  light='minecraft:lantern', street=['minecraft:stone_bricks', 'minecraft:andesite', 'minecraft:polished_andesite', 'minecraft:cobblestone'],
                  path=['minecraft:dirt_path', 'minecraft:coarse_dirt', 'minecraft:gravel'], banner='minecraft:blue_banner',
                  carpet='minecraft:red_carpet', wool='minecraft:blue_wool', accent='minecraft:gold_block', chimney='minecraft:cobblestone'),
    'elf': dict(found=['minecraft:calcite', 'minecraft:mossy_stone_bricks', 'minecraft:calcite'],
                wall=['minecraft:birch_planks', 'minecraft:stripped_birch_wood'], frame='minecraft:birch_log', floor='minecraft:birch_planks',
                plank='minecraft:birch_planks',
                roofs=[('minecraft:birch_stairs', 'minecraft:birch_slab', 'minecraft:birch_planks'),
                       ('minecraft:prismarine_brick_stairs', 'minecraft:prismarine_brick_slab', 'minecraft:prismarine_bricks')],
                door='minecraft:birch_door', glass='minecraft:light_blue_stained_glass_pane', fence='minecraft:birch_fence',
                light='minecraft:pearlescent_froglight', street=['minecraft:moss_block', 'minecraft:calcite', 'minecraft:mossy_cobblestone'],
                path=['minecraft:moss_block', 'minecraft:grass_block'], banner='minecraft:lime_banner', carpet='minecraft:green_carpet',
                wool='minecraft:lime_wool', accent='minecraft:emerald_block', chimney='minecraft:mossy_cobblestone'),
    'dwarf': dict(found=['minecraft:polished_deepslate', 'minecraft:deepslate_bricks', 'minecraft:deepslate_tiles'],
                  wall=['minecraft:deepslate_bricks', 'minecraft:polished_deepslate', 'minecraft:stone_bricks'], frame='minecraft:polished_basalt',
                  floor='minecraft:polished_blackstone', plank='minecraft:dark_oak_planks',
                  roofs=[('minecraft:deepslate_brick_stairs', 'minecraft:deepslate_brick_slab', 'minecraft:deepslate_bricks'),
                         ('minecraft:polished_blackstone_brick_stairs', 'minecraft:polished_blackstone_brick_slab', 'minecraft:polished_blackstone_bricks')],
                  door='minecraft:dark_oak_door', glass='minecraft:orange_stained_glass_pane', fence='minecraft:dark_oak_fence',
                  light='minecraft:lantern', street=['minecraft:polished_andesite', 'minecraft:deepslate_tiles', 'minecraft:stone_bricks'],
                  path=['minecraft:gravel', 'minecraft:cobblestone'], banner='minecraft:orange_banner', carpet='minecraft:orange_carpet',
                  wool='minecraft:orange_wool', accent='minecraft:gold_block', chimney='minecraft:polished_blackstone_bricks'),
    'orc': dict(found=['minecraft:mud_bricks', 'minecraft:cobblestone', 'minecraft:packed_mud'],
                wall=['minecraft:spruce_log', 'minecraft:stripped_spruce_log'], frame='minecraft:spruce_log', floor='minecraft:coarse_dirt',
                plank='minecraft:spruce_planks',
                roofs=[('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks'),
                       ('minecraft:mud_brick_stairs', 'minecraft:mud_brick_slab', 'minecraft:mud_bricks')],
                door='minecraft:spruce_door', glass='minecraft:air', fence='minecraft:spruce_fence', light='minecraft:lantern',
                street=['minecraft:coarse_dirt', 'minecraft:packed_mud', 'minecraft:dirt_path'], path=['minecraft:coarse_dirt'],
                banner='minecraft:red_banner', carpet='minecraft:red_carpet', wool='minecraft:brown_wool', accent='minecraft:bone_block',
                chimney='minecraft:cobblestone'),
    'beast': dict(found=['minecraft:cobblestone', 'minecraft:mossy_cobblestone'], wall=['minecraft:spruce_log'], frame='minecraft:spruce_log',
                  floor='minecraft:spruce_planks', plank='minecraft:spruce_planks',
                  roofs=[('minecraft:spruce_stairs', 'minecraft:spruce_slab', 'minecraft:spruce_planks')],
                  door='minecraft:spruce_door', glass='minecraft:air', fence='minecraft:spruce_fence', light='minecraft:lantern',
                  street=['minecraft:podzol', 'minecraft:coarse_dirt', 'minecraft:dirt_path'], path=['minecraft:podzol'],
                  banner='minecraft:brown_banner', carpet='minecraft:brown_carpet', wool='minecraft:brown_wool', accent='minecraft:bone_block',
                  chimney='minecraft:cobblestone'),
    'demon': dict(found=['minecraft:polished_blackstone_bricks', 'minecraft:cracked_polished_blackstone_bricks', 'minecraft:blackstone'],
                  wall=['minecraft:red_nether_bricks', 'minecraft:nether_bricks', 'minecraft:polished_blackstone_bricks'],
                  frame='minecraft:crimson_stem', floor='minecraft:crimson_planks', plank='minecraft:crimson_planks',
                  roofs=[('minecraft:nether_brick_stairs', 'minecraft:nether_brick_slab', 'minecraft:nether_bricks'),
                         ('minecraft:polished_blackstone_brick_stairs', 'minecraft:polished_blackstone_brick_slab', 'minecraft:polished_blackstone_bricks')],
                  door='minecraft:crimson_door', glass='minecraft:red_stained_glass_pane', fence='minecraft:nether_brick_fence',
                  light='minecraft:soul_lantern', street=['minecraft:blackstone', 'minecraft:polished_blackstone', 'minecraft:basalt'],
                  path=['minecraft:soul_soil'], banner='minecraft:black_banner', carpet='minecraft:black_carpet', wool='minecraft:red_wool',
                  accent='minecraft:crying_obsidian', chimney='minecraft:blackstone'),
}


class City(Build):
    """Большая постройка с удобными помощниками. Координаты локальные, y=G — уровень земли (по нему ходят)."""

    def __init__(self, sx, sy, sz, seed, style, ground=4):
        super().__init__(sx, sy, sz, seed)
        self.st = STYLES[style]
        self.style = style
        self.G = ground
        self.rnd = random.Random(seed)
        self.reserved = []
        self.workshops = []  # (ремесло, житель, x, y, z) — мастерские для заданий и найма

    def bp(self, opts, x, y, z):
        """Детерминированный выбор блока по координатам."""
        if isinstance(opts, str):
            return opts
        n = (x * 73856093 ^ y * 19349663 ^ z * 83492791) & 0x7FFFFFFF
        return opts[n % len(opts)]

    def free(self, x1, z1, x2, z2):
        for (a1, b1, a2, b2) in self.reserved:
            if x1 <= a2 and x2 >= a1 and z1 <= b2 and z2 >= b1:
                return False
        return 0 <= x1 and 0 <= z1 and x2 < self.size[0] and z2 < self.size[2]

    def reserve(self, x1, z1, x2, z2):
        self.reserved.append((x1, z1, x2, z2))

    # ---------- простые блоки ----------
    def stair(self, x, y, z, block, facing, half='bottom'):
        self.set(x, y, z, block, facing=facing, half=half, shape='straight', waterlogged='false')

    def slab(self, x, y, z, block, half='bottom'):
        self.set(x, y, z, block, type=half, waterlogged='false')

    def lantern(self, x, y, z, hanging=False):
        l = self.st['light']
        if 'lantern' in l:
            self.set(x, y, z, l, hanging='true' if hanging else 'false', waterlogged='false')
        else:
            self.set(x, y, z, l, axis='y') if 'froglight' in l else self.set(x, y, z, l)

    def door(self, x, y, z, facing, block=None):
        b = block or self.st['door']
        self.set(x, y, z, b, facing=facing, half='lower', hinge='left', open='false', powered='false')
        self.set(x, y + 1, z, b, facing=facing, half='upper', hinge='left', open='false', powered='false')

    def bed(self, x, y, z, facing, color='red'):
        dx, dz = DIRS[facing]
        self.set(x, y, z, f'minecraft:{color}_bed', facing=facing, part='foot', occupied='false')
        self.set(x + dx, y, z + dz, f'minecraft:{color}_bed', facing=facing, part='head', occupied='false')

    def lamp_post(self, x, z, h=3, fence=None):
        f = fence or self.st['fence']
        for i in range(h):
            self.set(x, self.G + i, z, f, waterlogged='false')
        self.lantern(x, self.G + h, z)

    def banner_wall(self, x, y, z, facing, color=None):
        b = (color or self.st['banner']).replace('_banner', '_wall_banner')
        self.set(x, y, z, b, facing=facing)

    def tree(self, x, z, h=6, log='minecraft:oak_log', leaves='minecraft:oak_leaves', r=3.0, y=None):
        y0 = self.G if y is None else y
        for i in range(h):
            self.set(x, y0 + i, z, log, axis='y')
        cy = y0 + h
        rr = int(r) + 1
        for dx in range(-rr, rr + 1):
            for dy in range(-2, rr + 1):
                for dz in range(-rr, rr + 1):
                    d = math.sqrt(dx * dx + (dy * 1.3) ** 2 + dz * dz)
                    if d <= r and self.rnd.random() < 0.9 and (dx, dz) != (0, 0) or (dx == 0 and dz == 0 and dy > 0 and d <= r):
                        if (x + dx, cy + dy, z + dz) not in self.blocks:
                            self.set(x + dx, cy + dy, z + dz, leaves, persistent='true', distance='1', waterlogged='false')

    def fountain(self, cx, cz, r=4, block=None):
        blk = block or self.bp(self.st['found'], 0, 0, 0)
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.5:
                    if d > r - 0.7:
                        self.set(x, self.G, z, blk)
                        self.slab(x, self.G + 1, z, 'minecraft:smooth_stone_slab')
                    else:
                        self.set(x, self.G - 1, z, blk)
                        self.set(x, self.G, z, 'minecraft:water', level='0')
        for y in range(self.G, self.G + 4):
            self.set(cx, y, cz, 'minecraft:chiseled_stone_bricks' if y < self.G + 3 else 'minecraft:sea_lantern')
        self.set(cx, self.G + 4, cz, 'minecraft:water', level='0')

    def bench(self, x, z, facing):
        self.stair(x, self.G, z, 'minecraft:spruce_stairs', OPP[facing])

    def well(self, cx, cz):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                if dx == 0 and dz == 0:
                    for y in range(self.G - 6, self.G):
                        self.set(cx, y, cz, 'minecraft:water', level='0')
                    continue
                self.set(cx + dx, self.G, cz + dz, 'minecraft:cobblestone_wall')
        for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
            self.set(cx + dx, self.G + 1, cz + dz, self.st['fence'], waterlogged='false')
            self.set(cx + dx, self.G + 2, cz + dz, self.st['fence'], waterlogged='false')
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                self.slab(cx + dx, self.G + 3, cz + dz, 'minecraft:spruce_slab')

    # ---------- дом ----------
    def house(self, x0, z0, w, d, floors=1, door='south', roof=None, interior='home', npc=None, lit=True):
        """Дом w×d (внешние стены), дверь на стороне door. Возвращает центр."""
        st = self.st
        G = self.G
        roof = roof or st['roofs'][self.rnd.randrange(len(st['roofs']))]
        fh = 4
        top = G + floors * fh
        x1, z1 = x0 + w - 1, z0 + d - 1
        self.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                self.set(x, G - 1, z, self.bp(st['found'], x, G - 1, z))
                edge = x in (x0, x1) or z in (z0, z1)
                for y in range(G, top):
                    lvl = (y - G) % fh
                    if lvl == 0 and y > G and not edge:
                        self.set(x, y, z, st['floor'])
                        continue
                    if edge:
                        corner = x in (x0, x1) and z in (z0, z1)
                        mid = (x - x0) % 4 == 0 if z in (z0, z1) else (z - z0) % 4 == 0
                        if corner or (mid and lvl != 0) or lvl == 0 and y > G:
                            self.set(x, y, z, st['frame'], axis='y') if 'log' in st['frame'] or 'stem' in st['frame'] or 'basalt' in st['frame'] \
                                else self.set(x, y, z, st['frame'])
                        elif y - G < 4 and self.style == 'human':
                            self.set(x, y, z, self.bp(st['found'], x, y, z))
                        else:
                            self.set(x, y, z, self.bp(st['wall'], x, y, z))
                    else:
                        self.set(x, y, z, 'minecraft:air')
                if not edge:
                    self.set(x, G - 1, z, st['floor'])
        # окна (у орков и зверолюдов — просто проёмы с решёткой из забора)
        pane = st['glass'] if st['glass'] != 'minecraft:air' else st['fence']
        # место двери считаем заранее: рядом с дверью окон не делаем (панель не стыкуется с дверью — дыры)
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        if (cx - x0) % 4 == 0:
            cx += 1
        if (cz - z0) % 4 == 0:
            cz += 1
        ddx, ddz = {'south': (cx, z1), 'north': (cx, z0), 'west': (x0, cz)}.get(door, (x1, cz))

        def near_door(x, z):
            return abs(x - ddx) + abs(z - ddz) <= 1 or (x == ddx and abs(z - ddz) <= 1) or (z == ddz and abs(x - ddx) <= 1)

        for f in range(floors):
            wy = G + f * fh + 1
            for x in range(x0 + 2, x1 - 1, 3):
                for z in (z0, z1):
                    if (x - x0) % 4 != 0 and not (f == 0 and near_door(x, z)):
                        self.set(x, wy, z, pane, waterlogged='false')
                        if pane == st['glass']:
                            self.set(x, wy + 1, z, pane, waterlogged='false')
            for z in range(z0 + 2, z1 - 1, 3):
                for x in (x0, x1):
                    if (z - z0) % 4 != 0 and not (f == 0 and near_door(x, z)):
                        self.set(x, wy, z, pane, waterlogged='false')
        # ящики с цветами под окнами второго этажа (ступенька-перевёртыш + горшок), на улице ничего не торчит
        if pane == st['glass'] and floors > 1:
            wy = G + fh + 1
            for x in range(x0 + 2, x1 - 1, 3):
                for z, oz in ((z0, -1), (z1, 1)):
                    if (x - x0) % 4 != 0 and self.rnd.random() < 0.5 and (x, wy, z + oz) not in self.blocks:
                        self.set(x, wy - 1, z + oz, self.st['plank'].replace('_planks', '_stairs') if '_planks' in self.st['plank'] else 'minecraft:spruce_stairs',
                                 facing='north' if oz > 0 else 'south', half='top', shape='straight', waterlogged='false')
                        self.set(x, wy, z + oz, 'minecraft:potted_' + self.rnd.choice(['red_tulip', 'poppy', 'dandelion', 'cornflower',
                                                                                         'azure_bluet', 'allium', 'fern']))
        # дверь
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        if (cx - x0) % 4 == 0:
            cx += 1
        if (cz - z0) % 4 == 0:
            cz += 1
        if door == 'south':
            dx_, dz_ = cx, z1
        elif door == 'north':
            dx_, dz_ = cx, z0
        elif door == 'west':
            dx_, dz_ = x0, cz
        else:
            dx_, dz_ = x1, cz
        self.door(dx_, G, dz_, door)
        self.last_door = (dx_, dz_, door)
        if not hasattr(self, 'doors'):
            self.doors = []
        self.doors.append((dx_, G, dz_, door))
        ox, oz = DIRS[door]
        self.set(dx_ + ox, G - 1, dz_ + oz, self.bp(st['found'], dx_, G, dz_))
        if lit:
            self.lantern(dx_ + ox + (1 if door in ('north', 'south') else 0), G + 2, dz_ + oz + (0 if door in ('north', 'south') else 1))
        # между этажами: в больших зданиях — марш из ступеней с проёмом и перилами, в остальных — приставная лестница
        self._stairs = None
        self._partitions = []
        if floors > 1 and (w - 2) * (d - 2) >= 110:
            self._stairs = self.stair_flights(x0, z0, x1, z1, floors, fh, door)
        elif floors > 1:
            for f in range(floors - 1):
                # лестница проходит и сквозь проём в перекрытии — иначе с неё не выбраться наверх
                for i in range(fh + 1):
                    self.set(x0 + 1, G + f * fh + i, z0 + 1, 'minecraft:ladder', facing='south', waterlogged='false')
        self.furnish(x0, z0, x1, z1, floors, interior, door)
        self.roof(x0, z0, x1, z1, top, roof)
        self.under_roof(x0, z0, x1, z1, top)
        if npc:
            self.entities.append((cx, G, cz if door in ('east', 'west') else (z0 + z1) // 2, npc, {}))
        return cx, cz

    def stair_flights(self, x0, z0, x1, z1, floors, fh, door):
        """Марши из ступеней вдоль стены: 5 ступеней на этаж (последняя — в перекрытии), под ними кладка,
        проём в полу над маршем с перилами. Этажи чередуют стену. -> {этаж: занятые клетки}."""
        G = self.G
        plank = self.st['plank']
        stair = plank.replace('_planks', '_stairs') if '_planks' in plank else 'minecraft:spruce_stairs'
        fence = plank.replace('_planks', '_fence') if '_planks' in plank else 'minecraft:spruce_fence'
        taken = {}
        for f in range(floors - 1):
            north = (f % 2 == 0) != (door == 'north')
            row = z0 + 1 if north else z1 - 1
            inn = 1 if north else -1
            base = G + f * fh
            for k in range(fh + 1):
                x, y = x0 + 1 + k, base + k
                for yy in range(base, y):
                    self.set(x, yy, row, plank)
                self.set(x, y, row, stair, facing='east', half='bottom', shape='straight', waterlogged='false')
                for yy in range(y + 1, y + 3):
                    if yy != base + fh or k == fh:
                        self.set(x, yy, row, 'minecraft:air')
            for k in range(fh):
                self.set(x0 + 1 + k, base + fh, row, 'minecraft:air')          # проём в перекрытии
                self.set(x0 + 1 + k, base + fh + 1, row + inn, fence, waterlogged='false')  # перила наверху
            self.set(x0 + 1, base + fh + 1, row, fence, waterlogged='false')    # перила у торца проёма
            low = taken.setdefault(f, set())
            for k in range(fh + 2):
                low.add((x0 + 1 + k, row))
            low |= {(x0 + 1, row + inn), (x0 + 2, row + inn), (x0 + 1, row + 2 * inn)}
            up = taken.setdefault(f + 1, set())
            for k in range(fh + 3):
                up.add((x0 + 1 + k, row))
                up.add((x0 + 1 + k, row + inn))
            up |= {(x0 + fh + 1, row + 2 * inn), (x0 + fh + 2, row + 2 * inn)}
        return taken

    def under_roof(self, x0, z0, x1, z1, top):
        """После крыши: перегородки верхнего этажа доводятся до скатов, фонари вешаются на цепях к коньку."""
        for (x, z, y, blk) in getattr(self, '_partitions', []):
            yy = y
            while yy < y + 16 and self.blocks.get((x, yy, z), ('minecraft:air',))[0] == 'minecraft:air':
                self.set(x, yy, z, blk)
                yy += 1
        self._partitions = []
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                for y in range(top - 3, top + 2):
                    v = self.blocks.get((x, y, z))
                    if v and 'lantern' in v[0] and dict(v[1]).get('hanging') == 'true':
                        yy = y + 1
                        while yy < y + 14 and self.blocks.get((x, yy, z), ('minecraft:air',))[0] == 'minecraft:air':
                            yy += 1
                        if yy < y + 14:
                            for c in range(y + 1, yy):
                                self.set(x, c, z, 'minecraft:chain', axis='y', waterlogged='false')

    def furnish(self, x0, z0, x1, z1, floors, kind, door):
        import mapbuild_furniture as MF
        return MF.furnish(self, x0, z0, x1, z1, floors, kind, door)

    def furnish_old(self, x0, z0, x1, z1, floors, kind, door):
        G = self.G
        r = self.rnd
        base_kind = kind
        for f in range(floors):
            y = G + f * 4
            if base_kind == 'workshop':
                # мастерская: первый этаж обставляет mapbuild_crafts, наверху — жильё мастера
                if f == 0:
                    self.lantern((x0 + x1) // 2, y + 3, (z0 + z1) // 2, hanging=True)
                    continue
                kind = 'home'
            spots = [(x1 - 1, z0 + 1), (x1 - 1, z1 - 1), (x0 + 1, z1 - 1), (x0 + 2, z0 + 1)]
            r.shuffle(spots)
            if kind in ('home', 'tavern') and f == floors - 1 or kind == 'home' and floors == 1:
                bx, bz = spots[0]
                fac = 'north' if bz == z1 - 1 else 'south'
                if self.blocks.get((bx, y, bz + (-1 if fac == 'north' else 1)), ('',))[0] == 'minecraft:air':
                    self.bed(bx, y, bz, fac, r.choice(['red', 'blue', 'white', 'green', 'brown']))
            opts = {'home': ['minecraft:crafting_table', 'minecraft:barrel', 'minecraft:bookshelf', 'minecraft:furnace', 'flowerpot', 'table'],
                    'shop': ['minecraft:barrel', 'minecraft:barrel', 'minecraft:chest', 'table', 'minecraft:bookshelf'],
                    'smith': ['minecraft:anvil', 'minecraft:smithing_table', 'minecraft:blast_furnace', 'minecraft:grindstone', 'minecraft:barrel'],
                    'tavern': ['table', 'table', 'minecraft:barrel', 'minecraft:barrel', 'minecraft:smoker'],
                    'library': ['minecraft:bookshelf', 'minecraft:bookshelf', 'minecraft:lectern', 'minecraft:enchanting_table', 'table'],
                    'guard': ['minecraft:armor_stand_marker', 'minecraft:barrel', 'table', 'minecraft:grindstone']}[kind]
            for (sx, sz) in spots[1:]:
                o = r.choice(opts)
                if self.blocks.get((sx, y, sz), ('',))[0] != 'minecraft:air':
                    continue
                if o == 'table':
                    self.set(sx, y, sz, self.st['fence'], waterlogged='false')
                    self.set(sx, y + 1, sz, 'minecraft:spruce_pressure_plate', powered='false')
                elif o == 'flowerpot':
                    self.set(sx, y, sz, 'minecraft:potted_' + r.choice(['red_tulip', 'oxeye_daisy', 'fern', 'azure_bluet', 'cornflower']))
                elif o == 'minecraft:armor_stand_marker':
                    self.entities.append((sx, y, sz, 'minecraft:armor_stand', {'ShowArms': TByte(1)}))
                elif o in ('minecraft:furnace', 'minecraft:blast_furnace', 'minecraft:smoker'):
                    self.set(sx, y, sz, o, facing='west' if sx == x1 - 1 else 'east', lit='false')
                elif o == 'minecraft:anvil':
                    self.set(sx, y, sz, o, facing='north')
                elif o == 'minecraft:chest':
                    self.set(sx, y, sz, o, facing='west' if sx == x1 - 1 else 'east', type='single', waterlogged='false')
                elif o == 'minecraft:barrel':
                    self.set(sx, y, sz, o, facing='up', open='false')
                elif o == 'minecraft:lectern':
                    self.set(sx, y, sz, o, facing='north', has_book='false', powered='false')
                elif o == 'minecraft:grindstone':
                    self.set(sx, y, sz, o, face='floor', facing='north')
                else:
                    self.set(sx, y, sz, o)
            self.lantern((x0 + x1) // 2, y + 3, (z0 + z1) // 2, hanging=True)
            if kind != 'shop':
                self.set((x0 + x1) // 2, y, (z0 + z1) // 2, self.st['carpet'])

    def roof(self, x0, z0, x1, z1, top, roof):
        stairs, slab, full = roof
        along_x = (x1 - x0) >= (z1 - z0)
        if along_x:
            half = (z1 - z0 + 2) // 2
            for i in range(half + 1):
                y = top + i
                za, zb = z0 - 1 + i, z1 + 1 - i
                if za > zb:
                    break
                for x in range(x0 - 1, x1 + 2):
                    if za == zb:
                        self.slab(x, y, za, slab)
                    else:
                        self.stair(x, y, za, stairs, 'south')
                        self.stair(x, y, zb, stairs, 'north')
                for z in range(za + 1, zb):
                    for x in (x0, x1):
                        self.set(x, y, z, full)
            if (z1 - z0) % 2 == 0:
                for x in range(x0 - 1, x1 + 2):
                    self.slab(x, top + half, (z0 + z1) // 2, slab)
        else:
            half = (x1 - x0 + 2) // 2
            for i in range(half + 1):
                y = top + i
                xa, xb = x0 - 1 + i, x1 + 1 - i
                if xa > xb:
                    break
                for z in range(z0 - 1, z1 + 2):
                    if xa == xb:
                        self.slab(xa, y, z, slab)
                    else:
                        self.stair(xa, y, z, stairs, 'east')
                        self.stair(xb, y, z, stairs, 'west')
                for x in range(xa + 1, xb):
                    for z in (z0, z1):
                        self.set(x, y, z, full)
        # труба с дымом
        if self.rnd.random() < 0.6:
            hx, hz = x1 - 1, z0 + 1
            for y in range(top, top + (z1 - z0) // 2 + 3):
                self.set(hx, y, hz, self.st['chimney'])
            self.set(hx, top + (z1 - z0) // 2 + 3, hz, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')

    # ---------- башни и стены ----------
    def round_tower(self, cx, cz, r, h, roof_block='minecraft:dark_prismarine', top_light=True, base=None):
        G = self.G
        blk = base or self.st['found']
        self.reserve(cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1)
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.hypot(x - cx, z - cz)
                if d > r + 0.45:
                    continue
                shell = d > r - 1.0
                for y in range(G - 1, G + h):
                    if shell:
                        win = (y - G) % 6 == 3 and abs((x - cx)) <= 0 or (y - G) % 6 == 3 and abs(z - cz) <= 0
                        self.set(x, y, z, 'minecraft:air' if win and y > G + 2 else self.bp(blk, x, y, z))
                    elif (y - G) % 6 == 0:
                        self.set(x, y, z, self.st['plank'])
                    else:
                        self.set(x, y, z, 'minecraft:air')
                # зубцы
                if shell:
                    self.set(x, G + h, z, self.bp(blk, x, G + h, z))
                    if (x + z) % 2 == 0:
                        self.set(x, G + h + 1, z, self.bp(blk, x, G + h + 1, z))
                else:
                    self.set(x, G + h, z, self.st['plank'])
        for y in range(G, G + h):
            self.set(cx, y, cz - r + 1, 'minecraft:ladder', facing='south', waterlogged='false')
        # конусная крыша
        if roof_block:
            for i in range(r + 3):
                rr = r + 1 - i * 0.75
                if rr < 0.3:
                    self.set(cx, G + h + 2 + i, cz, roof_block)
                    break
                for x in range(cx - r - 1, cx + r + 2):
                    for z in range(cz - r - 1, cz + r + 2):
                        d = math.hypot(x - cx, z - cz)
                        if rr - 1 < d <= rr:
                            self.set(x, G + h + 2 + i, z, roof_block)
            self.set(cx, G + h + 2 + r + 3, cz, 'minecraft:lightning_rod', facing='up', powered='false', waterlogged='false')
        if top_light:
            self.lantern(cx, G + h + 1, cz)

    def wall_line(self, ax, az, bx, bz, h=10, thick=3, gates=()):
        """Крепостная стена с дозорным ходом и зубцами по отрезку."""
        n = int(max(abs(bx - ax), abs(bz - az)))
        G = self.G
        for s in range(n + 1):
            x = round(ax + (bx - ax) * s / max(1, n))
            z = round(az + (bz - az) * s / max(1, n))
            gate = any(math.hypot(x - gx, z - gz) < gr for gx, gz, gr in gates)
            for t in range(-(thick // 2), thick // 2 + 1):
                px, pz = (x + t, z) if abs(bz - az) > abs(bx - ax) else (x, z + t)
                for y in range(G - 1, G + h):
                    if gate and y < G + 7:
                        self.set(px, y, pz, 'minecraft:air') if y >= G else self.set(px, y, pz, self.bp(self.st['street'], px, y, pz))
                    else:
                        self.set(px, y, pz, self.bp(self.st['found'], px, y, pz))
                edge = t in (-(thick // 2), thick // 2)
                if edge and (s % 2 == 0):
                    self.set(px, G + h, pz, self.bp(self.st['found'], px, G + h, pz))
                if gate:
                    for y in range(G + 7, G + h):
                        self.set(px, y, pz, self.bp(self.st['found'], px, y, pz))
                    self.set(px, G + 6, pz, 'minecraft:iron_bars') if t == 0 else None

    def statue(self, cx, cz, h=12, block='minecraft:smooth_stone', accent='minecraft:gold_block'):
        """Простая статуя воина с мечом на постаменте."""
        G = self.G
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                for y in range(G, G + 2):
                    self.set(x, y, z, 'minecraft:chiseled_stone_bricks' if y == G + 1 and (x + z) % 2 else 'minecraft:stone_bricks')
        b = G + 2
        k = h / 12
        # ноги
        for y in range(b, b + int(4 * k)):
            self.set(cx - 1, y, cz, block)
            self.set(cx + 1, y, cz, block)
        # тело
        for y in range(b + int(4 * k), b + int(8 * k)):
            for x in range(cx - 1, cx + 2):
                self.set(x, y, cz, block)
                self.set(x, y, cz - 1, block) if y > b + int(6 * k) else None
        # руки
        for y in range(b + int(5 * k), b + int(8 * k)):
            self.set(cx - 2, y, cz, block)
            self.set(cx + 2, y, cz, block)
        # голова
        for y in range(b + int(8 * k), b + int(10 * k)):
            self.set(cx, y, cz, block)
        self.set(cx, b + int(10 * k), cz, accent)
        # меч
        for y in range(b + int(4 * k), b + int(12 * k)):
            self.set(cx + 2, y, cz - 1, 'minecraft:iron_block' if y > b + int(6 * k) else accent)



_CCW = {'south': (1, 0), 'north': (-1, 0), 'east': (0, -1), 'west': (0, 1)}
_DIR = {'south': (0, 1), 'north': (0, -1), 'east': (1, 0), 'west': (-1, 0)}
_SOFT = ('air', 'pane', 'torch', 'lantern', 'fence', '_wall', 'door', 'slab', 'stairs', 'sign', 'banner', 'carpet',
         'chain', 'bars', 'button', 'lever', 'flower', 'sapling', 'vine', 'ladder', 'rail', 'water', 'lava', 'campfire',
         'candle', 'head', 'skull', 'pot', 'fire')


def fix_paintings(b, name=""):
    """Картина 8×8 в Майнкрафте висит не от левого нижнего угла, а вокруг якоря (-3..+4).
    Подбираем якорь рядом с задуманным местом так, чтобы позади была сплошная стена, а спереди — пусто."""
    inv = {v: k for k, v in FACING_BYTE.items()}
    out = []
    for e in b.entities:
        if e[3] != 'minecraft:painting':
            out.append(e)
            continue
        if e[4].get('small'):
            x, y, z = e[:3]
            face = e[4]['facing']
            dx, dz = _DIR[face]
            behind = b.blocks.get((x - dx, y, z - dz))
            here = b.blocks.get((x, y, z))
            if behind and not any(t in behind[0] for t in _SOFT) and (here is None or here[0] == 'minecraft:air'):
                b.set(x, y, z, 'minecraft:structure_block', mode='data')
                b.nbt[(x, y, z)] = {'id': 'minecraft:structure_block', 'mode': 'DATA', 'metadata': 'painting ' + e[4]['variant'] + ' ' + face}
            continue
        x, y, z = e[:3]
        x, y, z = x - 4 * _CCW[inv[int(getattr(e[4]['facing'], 'v', e[4]['facing']))]][0], y + 3, z - 4 * _CCW[inv[int(getattr(e[4]['facing'], 'v', e[4]['facing']))]][1]
        face = inv[int(e[4]['facing'].v if hasattr(e[4]['facing'], 'v') else e[4]['facing'])]
        cx, cz = _CCW[face]
        dx, dz = _DIR[face]

        def solid(px, py, pz):
            v = b.blocks.get((px, py, pz))
            return v is not None and not any(t in v[0] for t in _SOFT)

        def free(px, py, pz):
            v = b.blocks.get((px, py, pz))
            return v is None or v[0] == 'minecraft:air'

        def ok(ax, ay, az):
            for k in range(-3, 5):
                for l in range(-3, 5):
                    px, py, pz = ax + cx * k, ay + l, az + cz * k
                    if not free(px, py, pz) or not solid(px - dx, py, pz - dz):
                        return False
            return True

        best = None
        for d in range(0, 9):
            for sk in range(-d, d + 1):
                for sl in range(-d, d + 1):
                    if max(abs(sk), abs(sl)) != d:
                        continue
                    ax, ay, az = x + cx * sk, y + sl, z + cz * sk
                    if ok(ax, ay, az):
                        best = (ax, ay, az)
                        break
                if best:
                    break
            if best:
                break
        if best is None:
            print("  ! картина не помещается:", name, (x, y, z), face)
            continue
        # Сущность-картину из шаблона Майнкрафт создать не может (нет якоря TileX) —
        # кладём маркер данных, а картину вешает MapTilePiece при генерации.
        ax, ay, az = best
        b.set(ax, ay, az, 'minecraft:structure_block', mode='data')
        b.nbt[(ax, ay, az)] = {'id': 'minecraft:structure_block', 'mode': 'DATA',
                               'metadata': 'painting ' + e[4]['variant'] + ' ' + face}
    b.entities[:] = out




_SIDES = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}


def _kind(name):
    if name.endswith('_fence_gate'):
        return 'gate'
    if name.endswith('_fence'):
        return 'fence'
    if name.endswith('_pane') or name == 'minecraft:iron_bars':
        return 'pane'
    if name.endswith('_wall') and 'sign' not in name and 'banner' not in name and 'torch' not in name:
        return 'wall'
    return None


def connect_blocks(b):
    """Заборы, стеклянные панели, решётки и стенки соединяются с соседями
    (без этого в шаблоне они стоят отдельными столбиками, а животные проходят насквозь)."""
    blocks = b.blocks
    changes = {}
    for (x, y, z), (name, props) in blocks.items():
        k = _kind(name)
        if not k or k == 'gate':
            continue
        pd = dict(props)
        for side, (dx, dz) in _SIDES.items():
            nb = blocks.get((x + dx, y, z + dz))
            ok = False
            if nb is not None and nb[0] != 'minecraft:air':
                nk = _kind(nb[0])
                if nk == k or (k in ('pane', 'wall') and nk in ('pane', 'wall')):
                    ok = not (k == 'fence' and ('nether' in name) != ('nether' in nb[0]))
                elif nk == 'gate':
                    gfacing = dict(nb[1]).get('facing', 'north')
                    ok = k in ('fence', 'wall') and (gfacing in ('north', 'south')) == (side in ('east', 'west'))
                elif nk is None:
                    ok = not any(t in nb[0] for t in _SOFT) and 'leaves' not in nb[0]
            if k == 'wall':
                pd[side] = 'low' if ok else 'none'
            else:
                pd[side] = 'true' if ok else 'false'
        if k == 'wall':
            pd.setdefault('up', 'true')
        pd.setdefault('waterlogged', 'false')
        changes[(x, y, z)] = (name, tuple(sorted(pd.items())))
    blocks.update(changes)

def save_tiles(b, name, tile=128):
    """Сохранить постройку как набор плиток не больше tile×tile. -> список (dx, dz, имя)."""
    finalize(b, name)
    fix_paintings(b, name)
    connect_blocks(b)
    sx, sy, sz = b.size
    out = []
    for tx in range(0, sx, tile):
        for tz in range(0, sz, tile):
            sub = Build(min(tile, sx - tx), sy, min(tile, sz - tz), name)
            for (x, y, z), v in b.blocks.items():
                if tx <= x < tx + tile and tz <= z < tz + tile:
                    sub.blocks[(x - tx, y, z - tz)] = v
                    if (x, y, z) in b.nbt:
                        sub.nbt[(x - tx, y, z - tz)] = b.nbt[(x, y, z)]
            for e in b.entities:
                x, y, z = e[0], e[1], e[2]
                if tx <= x < tx + tile and tz <= z < tz + tile:
                    sub.entities.append((x - tx, y, z - tz) + tuple(e[3:]))
            if not sub.blocks and not sub.entities:
                continue
            tname = f'{name}_{tx // tile}_{tz // tile}' if (sx > tile or sz > tile) else name
            save_map(sub, tname)
            out.append((tx, tz, tname))
    return out


def save_map(b, name):
    """Как Build.save, но в structures/map/ и с NBT сущностей (картины)."""
    palette, index = [], {}
    blocks = TList(10)
    for (x, y, z), (bl, props) in sorted(b.blocks.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
        key = (bl, props)
        if key not in index:
            index[key] = len(palette)
            entry = {'Name': bl}
            if props:
                entry['Properties'] = {k: str(v) for k, v in props}
            palette.append(entry)
        e = {'pos': TList(3, [TInt(x), TInt(y), TInt(z)]), 'state': TInt(index[key])}
        if (x, y, z) in b.nbt:
            e['nbt'] = b.nbt[(x, y, z)]
        blocks.append(e)
    ents = TList(10)
    for e in b.entities:
        x, y, z, eid = e[:4]
        extra = e[4] if len(e) > 4 else {}
        nbt = {'id': eid, 'PersistenceRequired': TByte(1)}
        nbt.update(extra)
        pos = TList(6, [TDouble(x + 0.5), TDouble(y), TDouble(z + 0.5)])
        if eid == 'minecraft:painting':
            pos = TList(6, [TDouble(x + 0.5), TDouble(y + 0.5), TDouble(z + 0.5)])
            nbt.pop('PersistenceRequired', None)
        ents.append({'pos': pos, 'blockPos': TList(3, [TInt(x), TInt(y), TInt(z)]), 'nbt': nbt})
    root = {'DataVersion': TInt(3465), 'size': TList(3, [TInt(v) for v in b.size]),
            'palette': TList(10, palette), 'blocks': blocks, 'entities': ents}
    d = os.path.join(DATA_LITRPG, 'structures', 'map')
    os.makedirs(d, exist_ok=True)
    write_nbt(os.path.join(d, name + '.nbt'), root)


# ---------------- превью сверху ----------------

_COL = [('water', (60, 110, 200)), ('lava', (230, 100, 20)), ('leaves', (60, 130, 50)), ('grass', (90, 150, 70)), ('moss', (80, 130, 60)),
        ('farmland', (120, 80, 40)), ('wheat', (200, 180, 60)), ('carrots', (120, 160, 60)), ('potatoes', (120, 160, 60)),
        ('wool', (200, 60, 60)), ('glass', (170, 210, 230)), ('terracotta', (210, 190, 170)), ('quartz', (235, 230, 225)),
        ('amethyst', (160, 100, 220)), ('gold', (240, 200, 60)), ('beacon', (120, 240, 240)), ('prismarine', (60, 140, 140)),
        ('deepslate', (70, 70, 80)), ('blackstone', (40, 35, 45)), ('nether', (100, 30, 35)), ('crimson', (140, 40, 60)),
        ('brick', (160, 80, 60)), ('dark_oak', (70, 50, 30)), ('spruce', (110, 80, 50)), ('birch', (215, 205, 160)),
        ('oak', (160, 130, 80)), ('mangrove', (120, 50, 40)), ('stone', (130, 130, 130)), ('andesite', (140, 140, 140)),
        ('diorite', (200, 200, 200)), ('cobble', (120, 120, 120)), ('sand', (220, 210, 160)), ('dirt', (130, 100, 70)),
        ('mud', (90, 80, 70)), ('basalt', (80, 80, 90)), ('bone', (225, 220, 200)), ('lantern', (255, 220, 120)), ('snow', (245, 250, 255)),
        ('ice', (160, 200, 250)), ('calcite', (225, 225, 220)), ('froglight', (250, 240, 200))]


def preview_top(b, path, scale=2):
    from png import write_png
    sx, sy, sz = b.size
    top = {}
    for (x, y, z), (bl, _) in b.blocks.items():
        if bl == 'minecraft:air':
            continue
        if (x, z) not in top or top[(x, z)][0] < y:
            top[(x, z)] = (y, bl)
    img = [[(25, 28, 34, 255)] * (sx * scale) for _ in range(sz * scale)]
    for (x, z), (y, bl) in top.items():
        col = (150, 150, 150)
        for k, c in _COL:
            if k in bl:
                col = c
                break
        sh = 0.7 + 0.6 * min(1.0, y / max(1, sy * 0.6))
        col = tuple(min(255, int(c * sh)) for c in col)
        for dy in range(scale):
            for dx in range(scale):
                img[z * scale + dy][x * scale + dx] = col + (255,)
    for e in b.entities:
        x, z = e[0], e[2]
        for dy in range(scale):
            for dx in range(scale):
                if 0 <= z * scale + dy < len(img) and 0 <= x * scale + dx < len(img[0]):
                    img[z * scale + dy][x * scale + dx] = (255, 0, 255, 255)
    write_png(path, img, sx * scale, sz * scale)


# ---------------- финальная проверка постройки ----------------

_OBSTACLE = ('barrel', 'composter', 'hay_block', 'fence', 'leaves', 'potted_', 'flower_pot', 'trapdoor', '_log', 'chest', 'cauldron',
             'crafting_table', 'wool', 'chain', 'rose_bush', 'lilac', 'peony', 'pumpkin', 'melon', 'beehive', 'grindstone', 'anvil',
             'lantern', 'campfire', 'bell', 'target', 'poppy', 'dandelion', 'tulip', 'allium', 'cornflower', 'daisy', 'fern', 'bluet')
# то, что текущая вода/лава смывает и куда затекает (заборы, стенки, ступеньки и плиты её держат)
_NONSOLID = ('air', 'grass', 'fern', 'poppy', 'dandelion', 'tulip', 'allium', 'cornflower', 'daisy', 'bluet', 'orchid', 'lily_of',
             'wheat', 'carrots', 'potatoes', 'beetroots', 'sapling', 'torch', 'carpet', 'rail', 'vine', 'rose_bush', 'lilac', 'peony',
             'mushroom', 'snow', 'dead_bush', 'sweet_berry')


def _solid(b, x, y, z):
    v = b.blocks.get((x, y, z))
    if v is None:
        return y < getattr(b, 'G', 0)  # ниже уровня земли плитки — естественный грунт
    name = v[0].replace('minecraft:', '')
    if name in ('grass_block', 'mud', 'soul_sand', 'farmland', 'dirt_path'):
        return True
    return not any(k in name for k in _NONSOLID)


def finalize(b, name=''):
    """
    Перед сохранением: расчистить входы в дома, не дать воде/лаве вытечь,
    раздать лут сундукам и бочкам без своего лута.
    """
    import gen_city_loot as GL
    cleared = 0
    for (x, y, z, side) in getattr(b, 'doors', []):
        ox, oz = DIRS[side]
        for k in (1, 2):
            cx, cz = x + ox * k, z + oz * k
            for yy in (y, y + 1):
                v = b.blocks.get((cx, yy, cz))
                if v and v[0] != 'minecraft:air' and any(t in v[0] for t in _OBSTACLE) and 'door' not in v[0]:
                    b.set(cx, yy, cz, 'minecraft:air')
                    b.nbt.pop((cx, yy, cz), None)
                    cleared += 1
            v = b.blocks.get((cx, y + 2, cz))
            if v and 'lantern' in v[0] and dict(v[1]).get('hanging') == 'false' and b.blocks.get((cx, y + 1, cz), ('minecraft:air',))[0] == 'minecraft:air':
                b.set(cx, y + 2, cz, 'minecraft:air')
    # жидкости: источник не должен течь наружу (лава подожгла бы дома, вода — залила бы улицы)
    sealed = 0
    for (x, y, z), (bl, props) in list(b.blocks.items()):
        if bl not in ('minecraft:water', 'minecraft:lava'):
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
            n = (x + dx, y + dy, z + dz)
            nv = b.blocks.get(n)
            if nv and nv[0] in ('minecraft:water', 'minecraft:lava') or nv and 'waterlogged' in dict(nv[1]) and dict(nv[1])['waterlogged'] == 'true':
                continue
            if not _solid(b, *n):
                if bl == 'minecraft:lava':
                    b.set(x, y, z, 'minecraft:magma_block')
                    break
                b.set(*n, 'minecraft:mud' if dy < 0 or n[1] < b.G else _patch_block(b, n))
                b.nbt.pop(n, None)
                sealed += 1
    # лут
    loot = 0
    for (x, y, z), (bl, props) in b.blocks.items():
        if bl in ('minecraft:chest', 'minecraft:barrel', 'minecraft:trapped_chest'):
            nbt = b.nbt.setdefault((x, y, z), {})
            if 'LootTable' not in nbt:
                nbt['LootTable'] = 'litrpg:chests/' + ('city_barrel' if bl == 'minecraft:barrel' else 'city_home')
                loot += 1
    # жители и животные не должны появляться внутри полных блоков (иначе «погребён заживо»)
    moved = 0
    ents = []
    for e in b.entities:
        x, y, z, eid = e[0], e[1], e[2], e[3]
        if eid in ('minecraft:boat', 'minecraft:minecart', 'minecraft:painting', 'minecraft:armor_stand') or                 not (_suffocates(b, x, y, z) or _suffocates(b, x, y + 1, z)):
            ents.append(e)
            continue
        spot = None
        for dy in (0, 1, -1):
            for r in range(1, 5):
                for dx in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        if max(abs(dx), abs(dz)) != r:
                            continue
                        px, py, pz = x + dx, y + dy, z + dz
                        if not _suffocates(b, px, py, pz) and not _suffocates(b, px, py + 1, pz) and _floor(b, px, py - 1, pz):
                            spot = (px, py, pz)
                            break
                    if spot:
                        break
                if spot:
                    break
            if spot:
                break
        if spot:
            ents.append((spot[0], spot[1], spot[2]) + tuple(e[3:]))
            moved += 1
    b.entities = ents
    if cleared or sealed or moved:
        print(f'  {name}: входов расчищено {cleared}, утечек закрыто {sealed}, лут в {loot} ящиках, жителей сдвинуто {moved}')


_NOSUFF = ('air', 'slab', 'stairs', 'fence', '_wall', 'pane', 'bars', 'door', 'chest', 'lantern', 'chain', 'leaves', 'carpet', '_bed',
           'anvil', 'cauldron', 'campfire', 'grass', 'fern', 'sapling', 'rail', 'torch', 'sign', 'banner', 'pressure_plate', 'button', 'lever',
           'ladder', 'vine', 'water', 'lava', 'snow', 'cake', 'candle', 'pot', 'skull', 'head', 'bell', 'lectern', 'enchanting', 'brewing',
           'grindstone', 'stonecutter', 'composter', 'scaffolding', 'cobweb', 'mushroom', 'wheat', 'carrots', 'potatoes', 'beetroots', 'wart',
           'sweet_berry', 'sugar_cane', 'lily', 'dead_bush', 'end_rod', 'lightning_rod', 'conduit', 'cluster', 'poppy', 'dandelion', 'tulip',
           'orchid', 'allium', 'bluet', 'daisy', 'cornflower', 'rose', 'lilac', 'peony', 'roots', 'fungus', 'flower', 'azalea', 'hopper')


def _suffocates(b, x, y, z):
    v = b.blocks.get((x, y, z))
    if v is None:
        return y < getattr(b, 'G', 0) - 1
    if v[0].endswith(('grass_block', 'snow_block', 'mushroom_block', 'wart_block', 'moss_block', 'hay_block')):
        return True
    return not any(k in v[0] for k in _NOSUFF)


def _floor(b, x, y, z):
    v = b.blocks.get((x, y, z))
    if v is None:
        return y < getattr(b, 'G', 0)
    return v[0] != 'minecraft:air' and 'water' not in v[0] and 'lava' not in v[0]


def _patch_block(b, n):
    """Чем заделать дыру у воды: мостовая на улице, иначе фундамент."""
    return b.bp(b.st['found'], *n) if hasattr(b, 'st') else 'minecraft:stone_bricks'
