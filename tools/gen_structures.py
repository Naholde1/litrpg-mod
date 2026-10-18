"""
Поселения рас: строим кодом и сохраняем как шаблоны структур (.nbt)
+ JSON для генерации мира (structure, structure_set, template_pool, теги биомов).
Запуск: python tools/gen_structures.py
"""
import gzip
import io
import json
import math
import os
import random
import struct

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'litrpg')

# ======================= NBT =======================


class TByte(int): pass
class TInt(int): pass
class TDouble(float): pass
class TList(list):
    def __init__(self, tag, items=()):
        super().__init__(items)
        self.tag = tag


def _payload(buf, v):
    if isinstance(v, TByte):
        buf.write(struct.pack('>b', v))
    elif isinstance(v, TInt):
        buf.write(struct.pack('>i', v))
    elif isinstance(v, TDouble):
        buf.write(struct.pack('>d', v))
    elif isinstance(v, str):
        b = v.encode('utf-8')
        buf.write(struct.pack('>H', len(b)) + b)
    elif isinstance(v, TList):
        buf.write(struct.pack('>bi', v.tag, len(v)))
        for it in v:
            _payload(buf, it)
    elif isinstance(v, dict):
        for k, val in v.items():
            buf.write(struct.pack('>b', _tag(val)))
            _payload(buf, k)
            _payload(buf, val)
        buf.write(b'\x00')
    else:
        raise TypeError(type(v))


def _tag(v):
    if isinstance(v, TByte): return 1
    if isinstance(v, TInt): return 3
    if isinstance(v, TDouble): return 6
    if isinstance(v, str): return 8
    if isinstance(v, TList): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))


def write_nbt(path, root):
    buf = io.BytesIO()
    buf.write(b'\x0a')
    _payload(buf, '')
    _payload(buf, root)
    with gzip.open(path, 'wb') as f:
        f.write(buf.getvalue())


# ======================= строитель =======================

class Build:
    def __init__(self, sx, sy, sz, seed):
        self.size = (sx, sy, sz)
        self.blocks = {}
        self.nbt = {}
        self.entities = []
        self.rnd = random.Random(seed)

    def set(self, x, y, z, block, **props):
        if 0 <= x < self.size[0] and 0 <= y < self.size[1] and 0 <= z < self.size[2]:
            self.blocks[(x, y, z)] = (block, tuple(sorted(props.items())))

    def fill(self, x1, y1, z1, x2, y2, z2, block, **props):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, block, **props)

    def walls(self, x1, y1, z1, x2, y2, z2, block, **props):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    if x in (x1, x2) or z in (z1, z2):
                        self.set(x, y, z, block, **props)

    def clear_air(self, y_from=1):
        sx, sy, sz = self.size
        for x in range(sx):
            for y in range(y_from, sy):
                for z in range(sz):
                    if (x, y, z) not in self.blocks:
                        self.blocks[(x, y, z)] = ('minecraft:air', ())

    def chest(self, x, y, z, table, facing='north'):
        self.set(x, y, z, 'minecraft:chest', facing=facing, type='single', waterlogged='false')
        self.nbt[(x, y, z)] = {'LootTable': table}

    def npc(self, x, y, z, kind):
        self.entities.append((x, y, z, 'litrpg:' + kind))

    def sphere(self, cx, cy, cz, r, block, hollow=False, chance=1.0, **props):
        for x in range(int(cx - r), int(cx + r) + 1):
            for y in range(int(cy - r), int(cy + r) + 1):
                for z in range(int(cz - r), int(cz + r) + 1):
                    d = math.sqrt((x - cx) ** 2 + (y - cy) ** 2 * 1.4 + (z - cz) ** 2)
                    if d <= r and (not hollow or d > r - 1.2) and self.rnd.random() < chance:
                        if (x, y, z) not in self.blocks or self.blocks[(x, y, z)][0] == 'minecraft:air':
                            self.set(x, y, z, block, **props)

    def save(self, name):
        palette, index = [], {}
        blocks = TList(10)
        for (x, y, z), (b, props) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][0], kv[0][2])):
            key = (b, props)
            if key not in index:
                index[key] = len(palette)
                entry = {'Name': b}
                if props:
                    entry['Properties'] = {k: str(v) for k, v in props}
                palette.append(entry)
            e = {'pos': TList(3, [TInt(x), TInt(y), TInt(z)]), 'state': TInt(index[key])}
            if (x, y, z) in self.nbt:
                e['nbt'] = self.nbt[(x, y, z)]
            blocks.append(e)
        ents = TList(10)
        for e in self.entities:
            x, y, z, eid = e[:4]
            extra = e[4] if len(e) > 4 else {}
            ents.append({'pos': TList(6, [TDouble(x + 0.5), TDouble(y), TDouble(z + 0.5)]),
                         'blockPos': TList(3, [TInt(x), TInt(y), TInt(z)]),
                         'nbt': {'id': eid, 'PersistenceRequired': TByte(1), **extra}})
        root = {'DataVersion': TInt(3465), 'size': TList(3, [TInt(v) for v in self.size]),
                'palette': TList(10, palette), 'blocks': blocks, 'entities': ents}
        os.makedirs(os.path.join(ROOT, 'structures'), exist_ok=True)
        write_nbt(os.path.join(ROOT, 'structures', name + '.nbt'), root)
        return len(self.blocks), len(self.entities)


def lantern(b, x, y, z, soul=False):
    b.set(x, y, z, 'minecraft:soul_lantern' if soul else 'minecraft:lantern', hanging='false', waterlogged='false')


def post_lantern(b, x, y, z, fence='minecraft:spruce_fence', h=2, soul=False):
    for i in range(h):
        b.set(x, y + i, z, fence, waterlogged='false', north='false', south='false', east='false', west='false')
    lantern(b, x, y + h, z, soul)


# ======================= роща эльфов =======================

def elf_grove():
    b = Build(29, 26, 29, 'elf')
    c = 14
    b.fill(0, 0, 0, 28, 0, 28, 'minecraft:grass_block', snowy='false')
    # великое дерево
    for y in range(1, 17):
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                b.set(c + dx, y, c + dz, 'minecraft:oak_log', axis='y')
    for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):  # корни
        b.set(c + dx, 1, c + dz, 'minecraft:oak_log', axis='x' if dx else 'z')
        b.set(c + dx, 2, c + dz, 'minecraft:oak_log', axis='y')
    for y in range(1, 9):
        b.set(c, y, c - 2, 'minecraft:ladder', facing='north', waterlogged='false')
    # площадка на 9-м уровне
    for x in range(c - 6, c + 7):
        for z in range(c - 6, c + 7):
            d = math.hypot(x - c, z - c)
            if d <= 6.2 and not (abs(x - c) <= 1 and abs(z - c) <= 1):
                b.set(x, 9, z, 'minecraft:birch_planks')
                if 5.2 < d <= 6.2:
                    b.set(x, 10, z, 'minecraft:birch_fence', waterlogged='false', north='false', south='false', east='false', west='false')
    b.set(c, 9, c - 2, 'minecraft:air')
    b.set(c, 10, c - 2, 'minecraft:ladder', facing='north', waterlogged='false')
    for x, z in ((c - 4, c - 4), (c + 4, c - 4), (c - 4, c + 4), (c + 4, c + 4)):
        lantern(b, x, 10, z)
    # крона
    b.sphere(c, 19, c, 8, 'minecraft:oak_leaves', chance=0.92, persistent='true', distance='1', waterlogged='false')
    b.sphere(c - 4, 16, c + 3, 4, 'minecraft:flowering_azalea_leaves', chance=0.8, persistent='true', distance='1', waterlogged='false')
    # хижины с лиственной крышей
    for hx, hz in ((4, 4), (24, 6), (5, 23)):
        b.walls(hx - 2, 1, hz - 2, hx + 2, 3, hz + 2, 'minecraft:birch_planks')
        for x, z in ((hx - 2, hz - 2), (hx + 2, hz - 2), (hx - 2, hz + 2), (hx + 2, hz + 2)):
            b.fill(x, 1, z, x, 3, z, 'minecraft:stripped_oak_log', axis='y')
        b.set(hx, 1, hz + 2, 'minecraft:air')
        b.set(hx, 2, hz + 2, 'minecraft:air')
        b.sphere(hx, 4, hz, 3.2, 'minecraft:oak_leaves', chance=0.95, persistent='true', distance='1', waterlogged='false')
        b.set(hx, 1, hz - 1, 'minecraft:crafting_table')
        lantern(b, hx + 1, 1, hz)
    b.chest(5, 1, 3, 'minecraft:chests/village/village_fletcher')
    # лунный колодец
    wx, wz = 14, 23
    b.walls(wx - 2, 1, wz - 2, wx + 2, 1, wz + 2, 'minecraft:mossy_stone_bricks')
    b.fill(wx - 1, 1, wz - 1, wx + 1, 1, wz + 1, 'minecraft:water', level='0')
    b.fill(wx - 1, 0, wz - 1, wx + 1, 0, wz + 1, 'minecraft:sea_lantern')
    for x, z in ((wx - 2, wz - 2), (wx + 2, wz - 2), (wx - 2, wz + 2), (wx + 2, wz + 2)):
        post_lantern(b, x, 2, z, 'minecraft:birch_fence')
    # цветы и тропинки
    for _ in range(40):
        x, z = b.rnd.randrange(29), b.rnd.randrange(29)
        if (x, 1, z) not in b.blocks:
            b.set(x, 1, z, b.rnd.choice(['minecraft:allium', 'minecraft:azure_bluet', 'minecraft:lily_of_the_valley', 'minecraft:fern', 'minecraft:grass']))
    for i in range(3, 26):
        b.set(c, 0, i if i < c - 1 or i > c + 1 else c + 2, 'minecraft:moss_block')
    b.clear_air()
    for kind, x, y, z in (('elf_warden', 10, 10, 10), ('elf_warden', 18, 10, 18), ('elf_bladedancer', 9, 1, 14),
                          ('elf_moonsage', 19, 1, 22), ('elf_trader', 12, 1, 22), ('elf_matriarch', 18, 10, 12),
                          ('elf_warden', 24, 1, 12)):
        b.npc(x, y, z, kind)
    return b


# ======================= зал дварфов =======================

def dwarf_hold():
    b = Build(25, 16, 23, 'dwarf')
    b.fill(0, 0, 0, 24, 0, 22, 'minecraft:stone_bricks')
    x1, z1, x2, z2 = 2, 2, 22, 18
    b.walls(x1, 1, z1, x2, 7, z2, 'minecraft:deepslate_bricks')
    b.fill(x1 + 1, 0, z1 + 1, x2 - 1, 0, z2 - 1, 'minecraft:polished_deepslate')
    # крыша ступенями
    for i in range(5):
        b.walls(x1 + i, 8 + i, z1 + i, x2 - i, 8 + i, z2 - i, 'minecraft:deepslate_tiles')
    b.fill(x1 + 5, 13, z1 + 5, x2 - 5, 13, z2 - 5, 'minecraft:deepslate_tiles')
    # колонны
    for x in (6, 12, 18):
        for z in (6, 14):
            b.fill(x, 1, z, x, 7, z, 'minecraft:polished_deepslate')
            b.set(x, 7, z, 'minecraft:chiseled_deepslate')
            lantern(b, x, 8, z)
            b.set(x + 1, 1, z, 'minecraft:lantern', hanging='false', waterlogged='false')
    # ворота
    b.fill(11, 1, z2, 13, 4, z2, 'minecraft:air')
    b.fill(10, 5, z2, 14, 5, z2, 'minecraft:gold_block')
    for x in (10, 14):
        b.fill(x, 1, z2 + 1, x, 3, z2 + 1, 'minecraft:polished_deepslate')
        lantern(b, x, 4, z2 + 1)
    # кузница: лава в решётке, горны, наковальня
    b.fill(4, 0, 4, 6, 0, 6, 'minecraft:lava', level='0')
    b.walls(4, 1, 4, 6, 1, 6, 'minecraft:iron_bars', north='false', south='false', east='false', west='false', waterlogged='false')
    b.set(4, 1, 8, 'minecraft:blast_furnace', facing='east', lit='true')
    b.set(5, 1, 8, 'minecraft:blast_furnace', facing='east', lit='true')
    b.set(8, 1, 5, 'minecraft:anvil', facing='north')
    b.set(8, 1, 7, 'minecraft:smithing_table')
    b.set(9, 1, 5, 'minecraft:grindstone', face='floor', facing='north')
    # длинный стол
    for x in range(10, 17):
        b.set(x, 1, 10, 'minecraft:spruce_planks')
        b.set(x, 1, 9, 'minecraft:spruce_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
        b.set(x, 1, 11, 'minecraft:spruce_stairs', facing='north', half='bottom', shape='straight', waterlogged='false')
        if x % 2 == 0:
            lantern(b, x, 2, 10)
    # бочки и сундуки
    for z in range(4, 9):
        b.set(20, 1, z, 'minecraft:barrel', facing='up', open='false')
        b.set(20, 2, z, 'minecraft:barrel', facing='up', open='false') if z % 2 == 0 else None
    b.chest(19, 1, 16, 'minecraft:chests/village/village_toolsmith', 'west')
    b.chest(19, 1, 15, 'minecraft:chests/simple_dungeon', 'west')
    # спуск в шахту
    for i in range(6):
        b.set(17 - i, 0, 4, 'minecraft:air')
    for x, z in ((3, 16), (21, 16), (3, 3), (21, 3)):
        b.set(x, 5, z, 'minecraft:wall_torch', facing='south' if z < 10 else 'north')
    b.clear_air()
    for kind, x, y, z in (('dwarf_warrior', 11, 1, 20), ('dwarf_warrior', 13, 1, 20), ('dwarf_crossbowman', 9, 1, 14),
                          ('dwarf_runepriest', 16, 1, 14), ('dwarf_smith', 7, 1, 6), ('dwarf_thane', 12, 1, 7)):
        b.npc(x, y, z, kind)
    return b


# ======================= военный лагерь орков =======================

def tent(b, cx, cz, half, h, wool, wool2):
    for y in range(h):
        w = half - y * half // h
        for x in range(cx - w, cx + w + 1):
            b.set(x, 1 + y, cz - half, wool)
            b.set(x, 1 + y, cz + half, wool)
        for z in range(cz - half, cz + half + 1):
            b.set(cx - w, 1 + y, z, wool if y % 2 == 0 else wool2)
            b.set(cx + w, 1 + y, z, wool if y % 2 == 0 else wool2)
    b.fill(cx - half + 1, 1, cz - half, cx + half - 1, 2, cz - half, 'minecraft:air')


def orc_camp():
    b = Build(33, 12, 33, 'orc')
    b.fill(0, 0, 0, 32, 0, 32, 'minecraft:coarse_dirt')
    c = 16
    # частокол
    for i in range(360):
        a = math.radians(i)
        x, z = round(c + 15 * math.cos(a)), round(c + 15 * math.sin(a))
        if abs(z - (c + 15)) <= 1 and abs(x - c) <= 2:
            continue  # ворота
        h = 4 + (x + z) % 2
        b.fill(x, 1, z, x, h, z, 'minecraft:spruce_log', axis='y')
        b.set(x, h + 1, z, 'minecraft:pointed_dripstone', thickness='tip', vertical_direction='up', waterlogged='false')
    for x in (c - 3, c + 3):
        post_lantern(b, x, 1, c + 16, 'minecraft:spruce_fence', 3)
    # шатры
    for tx, tz, big in ((8, 8, False), (24, 8, False), (8, 22, False), (24, 22, False), (16, 9, True)):
        tent(b, tx, tz, 4 if big else 3, 5 if big else 4, 'minecraft:brown_wool', 'minecraft:red_wool')
        b.set(tx, 1, tz, 'minecraft:red_carpet')
    b.chest(16, 1, 11, 'minecraft:chests/pillager_outpost')
    b.chest(8, 1, 9, 'minecraft:chests/village/village_butcher')
    # тотем и костёр
    b.fill(c, 1, c, c, 6, c, 'minecraft:stripped_dark_oak_log', axis='y')
    b.set(c, 7, c, 'minecraft:skeleton_skull', rotation='8')
    b.set(c + 1, 5, c, 'minecraft:red_wool')
    b.set(c - 1, 5, c, 'minecraft:red_wool')
    b.set(c, 4, c + 1, 'minecraft:bone_block', axis='y')
    b.set(c + 3, 1, c + 3, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    # стойки оружия
    for x in range(10, 23, 3):
        b.set(x, 1, 27, 'minecraft:spruce_fence', waterlogged='false', north='false', south='false', east='false', west='false')
        b.set(x, 2, 27, 'minecraft:skeleton_skull', rotation='0')
    b.clear_air()
    for kind, x, y, z in (('orc_raider', 14, 1, 26), ('orc_raider', 18, 1, 26), ('orc_raider', 12, 1, 16), ('orc_hunter', 20, 1, 16),
                          ('orc_hunter', 16, 1, 4), ('orc_shaman', 19, 1, 19), ('orc_trader', 10, 1, 13), ('orc_warlord', 16, 1, 7)):
        b.npc(x, y, z, kind)
    return b


# ======================= стойбище зверолюдов =======================

def round_hut(b, cx, cz, r, h):
    for x in range(cx - r, cx + r + 1):
        for z in range(cz - r, cz + r + 1):
            d = math.hypot(x - cx, z - cz)
            if r - 0.7 <= d <= r + 0.3:
                b.fill(x, 1, z, x, h, z, 'minecraft:spruce_log', axis='y')
            if d <= r + 0.3:
                b.set(x, 0, z, 'minecraft:spruce_planks')
    for y in range(h + 1, h + r + 2):
        rr = r + 1 - (y - h)
        for x in range(cx - r - 1, cx + r + 2):
            for z in range(cz - r - 1, cz + r + 2):
                if abs(math.hypot(x - cx, z - cz) - rr) < 0.7:
                    b.set(x, y, z, 'minecraft:hay_block', axis='y')
    b.set(cx, 1, cz + r, 'minecraft:air')
    b.set(cx, 2, cz + r, 'minecraft:air')
    b.set(cx, 1, cz, 'minecraft:brown_carpet')


def beast_lodge():
    b = Build(27, 12, 27, 'beast')
    b.fill(0, 0, 0, 26, 0, 26, 'minecraft:podzol', snowy='false')
    for cx, cz in ((6, 6), (20, 6), (6, 20), (20, 20)):
        round_hut(b, cx, cz, 3, 3)
    b.chest(6, 1, 5, 'minecraft:chests/village/village_butcher')
    b.chest(20, 1, 5, 'minecraft:chests/village/village_tannery')
    # костёр и брёвна-лавки
    b.set(13, 1, 13, 'minecraft:campfire', lit='true', signal_fire='true', facing='north', waterlogged='false')
    for x, z, ax in ((11, 13, 'z'), (15, 13, 'z'), (13, 11, 'x'), (13, 15, 'x')):
        b.set(x, 1, z, 'minecraft:stripped_spruce_log', axis=ax)
    # сушилки шкур
    for x in (10, 16):
        b.fill(x, 1, 22, x, 3, 22, 'minecraft:spruce_fence', waterlogged='false', north='false', south='false', east='false', west='false')
    b.fill(11, 3, 22, 15, 3, 22, 'minecraft:spruce_fence', waterlogged='false', north='false', south='false', east='true', west='true')
    b.fill(11, 2, 22, 15, 2, 22, 'minecraft:brown_wool')
    # тотем-череп
    b.fill(13, 1, 4, 13, 4, 4, 'minecraft:bone_block', axis='y')
    b.set(13, 5, 4, 'minecraft:skeleton_skull', rotation='8')
    for x, z in ((2, 13), (24, 13)):
        post_lantern(b, x, 1, z)
    b.clear_air()
    for kind, x, y, z in (('beast_stalker', 9, 1, 13), ('beast_stalker', 17, 1, 9), ('beast_ravager', 13, 1, 18),
                          ('beast_spiritcaller', 16, 1, 16), ('beast_trader', 11, 1, 10), ('beast_alpha', 13, 1, 8)):
        b.npc(x, y, z, kind)
    return b


# ======================= святилище демонидов =======================

def demon_shrine():
    b = Build(23, 15, 23, 'demon')
    c = 11
    b.fill(0, 0, 0, 22, 0, 22, 'minecraft:blackstone')
    b.fill(2, 1, 2, 20, 1, 20, 'minecraft:polished_blackstone_bricks')
    # лавовые каналы крестом
    for i in range(3, 20):
        if abs(i - c) > 2:
            b.set(i, 1, c, 'minecraft:lava', level='0')
            b.set(c, 1, i, 'minecraft:lava', level='0')
    # колонны с огнём
    for x, z in ((4, 4), (18, 4), (4, 18), (18, 18), (4, 11), (18, 11), (11, 4), (11, 18)):
        b.fill(x, 2, z, x, 7, z, 'minecraft:polished_blackstone')
        b.set(x, 8, z, 'minecraft:magma_block')
        b.set(x, 9, z, 'minecraft:fire', age='0', north='false', south='false', east='false', west='false', up='false')
    # алтарь
    b.fill(c - 2, 2, c - 2, c + 2, 2, c + 2, 'minecraft:crying_obsidian')
    b.fill(c - 1, 3, c - 1, c + 1, 3, c + 1, 'minecraft:polished_blackstone_bricks')
    b.set(c, 4, c, 'minecraft:soul_campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    for x, z in ((c - 2, c - 2), (c + 2, c - 2), (c - 2, c + 2), (c + 2, c + 2)):
        b.set(x, 3, z, 'minecraft:chain', axis='y', waterlogged='false')
        lantern(b, x, 4, z, soul=True)
    # арка входа
    for x in (9, 13):
        b.fill(x, 2, 20, x, 6, 20, 'minecraft:nether_bricks')
    b.fill(9, 7, 20, 13, 7, 20, 'minecraft:red_nether_bricks')
    b.chest(17, 2, 15, 'minecraft:chests/ruined_portal', 'west')
    b.chest(5, 2, 7, 'minecraft:chests/nether_bridge', 'east')
    b.clear_air(2)
    for kind, x, y, z in (('demon_cultist', 8, 2, 8), ('demon_cultist', 14, 2, 14), ('demon_guard', 10, 2, 19),
                          ('demon_guard', 12, 2, 19), ('demon_hellbow', 16, 2, 6), ('demon_trader', 6, 2, 15),
                          ('demon_archfiend', 11, 4, 9)):
        b.npc(x, y, z, kind)
    return b


# ======================= лагерь гоблинов =======================

def goblin_camp():
    b = Build(19, 9, 19, 'goblin')
    b.fill(0, 0, 0, 18, 0, 18, 'minecraft:coarse_dirt')
    for cx, cz in ((4, 4), (14, 4), (4, 14)):
        b.walls(cx - 2, 1, cz - 2, cx + 2, 2, cz + 2, 'minecraft:mossy_cobblestone')
        b.fill(cx - 2, 3, cz - 2, cx + 2, 3, cz + 2, 'minecraft:oak_slab', type='bottom', waterlogged='false')
        b.set(cx, 1, cz + 2, 'minecraft:air')
        b.set(cx, 2, cz + 2, 'minecraft:air')
        b.set(cx, 1, cz, 'minecraft:hay_block', axis='y')
    b.chest(14, 1, 3, 'minecraft:chests/simple_dungeon')
    b.set(10, 1, 10, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    for x, z in ((13, 13), (15, 12), (12, 15)):
        b.set(x, 1, z, 'minecraft:barrel', facing='up', open='false')
        b.set(x, 2, z, 'minecraft:cobweb') if b.rnd.random() < 0.5 else None
    b.fill(16, 1, 16, 16, 3, 16, 'minecraft:oak_fence', waterlogged='false', north='false', south='false', east='false', west='false')
    b.set(16, 4, 16, 'minecraft:skeleton_skull', rotation='8')
    b.clear_air()
    for kind, x, y, z in (('goblin', 9, 1, 8), ('goblin', 11, 1, 12), ('goblin', 7, 1, 11), ('goblin_shaman', 12, 1, 9)):
        b.npc(x, y, z, kind)
    return b


# ======================= JSON генерации мира =======================

STRUCTURES = [
    # имя, функция, биомы, разреженность (spacing, separation), соль, адаптация к рельефу
    ('elf_grove', elf_grove, ["#minecraft:is_forest", "litrpg:sylvan_wildwood", "litrpg:moon_grove", "litrpg:silver_glade"], (30, 12), 718231, 'beard_thin'),
    ('dwarf_hold', dwarf_hold, ["#minecraft:is_mountain", "#minecraft:is_hill", "minecraft:windswept_forest", "litrpg:dwarf_vales", "litrpg:ore_slopes"], (32, 12), 512331, 'beard_box'),
    ('orc_camp', orc_camp, ["#minecraft:is_savanna", "#minecraft:is_badlands", "minecraft:plains", "litrpg:scorched_steppe", "litrpg:dust_wastes"], (32, 12), 913221, 'beard_thin'),
    ('beast_lodge', beast_lodge, ["#minecraft:is_taiga", "#minecraft:is_jungle", "litrpg:fang_taiga", "litrpg:snow_hunt"], (30, 12), 431127, 'beard_thin'),
    ('demon_shrine', demon_shrine, ["#minecraft:is_badlands", "minecraft:desert", "minecraft:savanna_plateau", "litrpg:ash_wastes", "litrpg:charred_forest"], (40, 16), 666013, 'beard_box'),
    ('goblin_camp', goblin_camp, ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "#minecraft:is_forest", "litrpg:rot_swamp", "litrpg:goblin_mangroves", "litrpg:frontier_woods"], (22, 8), 224411, 'beard_thin'),
]


def main():
    wg = os.path.join(ROOT, 'worldgen')
    for d in ('structure', 'structure_set', 'template_pool'):
        os.makedirs(os.path.join(wg, d), exist_ok=True)
    tags = os.path.join(ROOT, 'tags', 'worldgen', 'biome', 'has_structure')
    os.makedirs(tags, exist_ok=True)
    for name, fn, biomes, (spacing, sep), salt, adapt in STRUCTURES:
        n_blocks, n_ents = fn().save(name)
        json.dump({"replace": False, "values": biomes}, open(os.path.join(tags, name + '.json'), 'w'), indent=2)
        os.makedirs(os.path.join(wg, 'template_pool', name), exist_ok=True)
        json.dump({"fallback": "minecraft:empty", "elements": [{"weight": 1, "element": {
            "element_type": "minecraft:single_pool_element", "location": "litrpg:" + name,
            "projection": "rigid", "processors": "minecraft:empty"}}]},
            open(os.path.join(wg, 'template_pool', name, 'start.json'), 'w'), indent=2)
        json.dump({"type": "minecraft:jigsaw", "biomes": "#litrpg:has_structure/" + name, "step": "surface_structures",
                   "spawn_overrides": {}, "terrain_adaptation": adapt, "start_pool": f"litrpg:{name}/start", "size": 1,
                   "start_height": {"absolute": 0}, "project_start_to_heightmap": "WORLD_SURFACE_WG",
                   "max_distance_from_center": 80, "use_expansion_hack": False},
                  open(os.path.join(wg, 'structure', name + '.json'), 'w'), indent=2)
        json.dump({"structures": [{"structure": "litrpg:" + name, "weight": 1}],
                   "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": sep, "salt": salt}},
                  open(os.path.join(wg, 'structure_set', name + '.json'), 'w'), indent=2)
        print(f'{name}: {n_blocks} blocks, {n_ents} npc')


if __name__ == '__main__':
    main()
