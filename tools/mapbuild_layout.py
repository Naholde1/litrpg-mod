"""
Живая застройка города: кварталы неправильной формы (диаграмма Вороного), кривые улочки
и переулки, террасы на разной высоте с подпорными стенками и лестницами, дома вплотную
вдоль улиц дверями на улицу, внутренние дворы с садами, сараями, бельём на верёвках,
кустами и тайниками. Тесный рынок. Поля и огороды за стенами.
"""
import math
import random

from mapbuild_lib import DIRS, OPP, TByte

STASH_LOOT = 'litrpg:chests/city_stash'
D4 = [('north', 0, -1), ('south', 0, 1), ('west', -1, 0), ('east', 1, 0)]


def _air(b, x, y, z):
    return b.blocks.get((x, y, z), ('minecraft:air',))[0] == 'minecraft:air'


def stash(b, x, y, z, kind='chest'):
    """Тайник: сундук под люком в земле или бочка в кустах. Ничей — брать можно без штрафа."""
    name = '{"text":"Тайник","color":"gold"}'
    if kind == 'chest':
        b.set(x, y - 1, z, 'minecraft:chest', facing='north', type='single', waterlogged='false')
        b.nbt[(x, y - 1, z)] = {'LootTable': STASH_LOOT, 'CustomName': name}
        b.set(x, y, z, 'minecraft:spruce_trapdoor', facing='north', half='bottom', open='false', powered='false', waterlogged='false')
    else:
        b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
        b.nbt[(x, y, z)] = {'LootTable': STASH_LOOT, 'CustomName': name}
        for dx, dz in ((1, 0), (-1, 0), (0, 1)):
            if _air(b, x + dx, y, z + dz):
                b.set(x + dx, y, z + dz, 'minecraft:azalea_leaves', persistent='true', distance='1', waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:azalea_leaves', persistent='true', distance='1', waterlogged='false')


class Organic:
    def __init__(self, b, C, R, seed, plaza_r=8, terrace=2, street_w=1.35, block_area=560):
        self.b, self.C, self.R = b, C, R
        self.rnd = random.Random(str(seed) + 'org')
        self.terrace = terrace
        self.plaza_r = plaza_r
        b0 = b.G
        self.G0 = b0
        S = b.size[0]
        # особые места (уже зарезервированы до нас): замок, храм, проспекты — не трогаем
        special = list(b.reserved)

        def is_special(x, z):
            for (a1, c1, a2, c2) in special:
                if a1 <= x <= a2 and c1 <= z <= c2:
                    return True
            return False

        n = max(6, int(math.pi * R * R / block_area))
        seeds = []
        for _ in range(n * 6):
            if len(seeds) >= n:
                break
            a = self.rnd.random() * 2 * math.pi
            r = math.sqrt(self.rnd.random()) * R * 0.98
            x, z = C + math.cos(a) * r, C + math.sin(a) * r
            if r < plaza_r + 6:
                continue
            if all(math.hypot(x - sx, z - sz) > 11 for sx, sz in seeds):
                seeds.append((x, z))
        self.seeds = seeds
        self.cell = {}      # (x,z) -> id квартала
        self.street = set()
        self.special = set()
        for x in range(S):
            for z in range(S):
                r = math.hypot(x - C, z - C)
                if r > R:
                    continue
                if is_special(x, z):
                    self.special.add((x, z))
                    continue
                if r < plaza_r:
                    self.street.add((x, z))
                    continue
                # улица — где два квартала почти на равном расстоянии (кривые извилистые переулки)
                wob = 0.6 * math.sin(x * 0.31 + z * 0.17) + 0.4 * math.sin(z * 0.27 - x * 0.11)
                best = second = 1e9
                bi = -1
                for i, (sx, sz) in enumerate(seeds):
                    d = math.hypot(x - sx, z - sz)
                    if d < best:
                        second, best, bi = best, d, i
                    elif d < second:
                        second = d
                if second - best < street_w + 0.35 * wob:
                    self.street.add((x, z))
                else:
                    self.cell[(x, z)] = bi
        # высота кварталов: террасы
        self.h = {}
        for i, (sx, sz) in enumerate(seeds):
            if terrace <= 0:
                self.h[i] = 0
            else:
                v = 0.5 + 0.5 * math.sin(sx * 0.045 + 1.3) * math.cos(sz * 0.038 + 0.4)
                self.h[i] = min(terrace, int(v * (terrace + 1) + self.rnd.random() * 0.8))

    # ------------------------------------------------------------------
    def is_streetish(self, x, z):
        return (x, z) in self.street or (x, z) in self.special and _air(self.b, x, self.G0, z)

    def paint_ground(self):
        b, G = self.b, self.G0
        st = b.st
        for (x, z) in self.street:
            b.set(x, G - 1, z, b.bp(st['street'], x, 0, z))
            b.set(x, G - 2, z, 'minecraft:stone')
        found = st['found']
        for (x, z), i in self.cell.items():
            h = self.h[i]
            for y in range(G - 1, G - 1 + h):
                b.set(x, y, z, b.bp(found, x, y, z))
            top = G - 1 + h
            b.set(x, top, z, 'minecraft:grass_block' if b.style not in ('demon', 'dwarf') else b.bp(st['path'], x, 0, z),
                  **({'snowy': 'false'} if b.style not in ('demon', 'dwarf') else {}))
            # бордюр у улицы на террасе
            if h > 0 and any((x + dx, z + dz) in self.street for _, dx, dz in D4):
                b.set(x, top, z, b.bp(found, x, top, z))
                if self.rnd.random() < 0.18:
                    b.set(x, top + 1, z, st['fence'], waterlogged='false')

    # ------------------------------------------------------------------
    def _rect_ok(self, bid, x0, z0, x1, z1):
        b = self.b
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.cell.get((x, z)) != bid:
                    return False
        for (a1, c1, a2, c2) in b.reserved:
            if x0 <= a2 and x1 >= a1 and z0 <= c2 and z1 >= c1:
                return False
        return True

    def lots(self, place_fn, sizes, max_lots=None, prefer_center=False):
        """Пройтись по фасадам кварталов и ставить здания (place_fn(x0,z0,w,d,door,h) -> bool)."""
        fronts = []
        for (x, z), i in self.cell.items():
            for side, dx, dz in D4:
                if (x + dx, z + dz) in self.street:
                    fronts.append((x, z, i, side))
                    break
        if prefer_center:
            fronts.sort(key=lambda f: math.hypot(f[0] - self.C, f[1] - self.C) + self.rnd.random() * 8)
        else:
            self.rnd.shuffle(fronts)
        placed = 0
        for (x, z, i, side) in fronts:
            if max_lots is not None and placed >= max_lots:
                break
            h = self.h[i]
            sb = h  # отступ от улицы под лестницу
            rect = None
            # несколько размеров и сдвигов вдоль улицы — плотнее заполняем квартал
            for attempt in range(4):
                w, d = sizes()
                for shift in (w // 2, 1, w - 2):
                    if side in ('north', 'south'):
                        x0 = x - shift
                        z0 = z + sb if side == 'north' else z - sb - d + 1
                        cand = (x0, z0, x0 + w - 1, z0 + d - 1)
                    else:
                        z0 = z - shift
                        x0 = x + sb if side == 'west' else x - sb - d + 1
                        cand = (x0, z0, x0 + d - 1, z0 + w - 1)
                    if self._rect_ok(i, *cand):
                        rect = cand
                        break
                if rect:
                    break
            if rect is None:
                continue
            G0 = self.G0
            self.b.G = G0 + h
            try:
                ok = place_fn(rect, side, h)
            finally:
                self.b.G = G0
            if ok:
                placed += 1
                if h > 0:
                    self.steps(rect, side, h)
        return placed

    def steps(self, rect, side, h):
        """Лестница от двери вниз к улице."""
        b = self.b
        if not hasattr(b, 'last_door'):
            return
        dx_, dz_, door = b.last_door
        if door != side:
            return
        ox, oz = DIRS[door]
        stair = b.st['plank'].replace('_planks', '_stairs') if 'planks' in b.st['plank'] else 'minecraft:stone_brick_stairs'
        if 'crimson' in stair or 'birch' in stair or 'spruce' in stair or 'dark_oak' in stair:
            pass
        G0 = self.G0
        for k in range(1, h + 1):
            x, z = dx_ + ox * k, dz_ + oz * k
            y = G0 + h - k
            b.set(x, y, z, 'minecraft:stone_brick_stairs' if b.style in ('human', 'dwarf') else stair, facing=OPP[door],
                  half='bottom', shape='straight', waterlogged='false')
            for yy in range(y + 1, G0 + h + 2):
                b.set(x, yy, z, 'minecraft:air')

    # ------------------------------------------------------------------
    def courtyards(self, citizen=None, density=1.0, stashes=3):
        """Дворы внутри кварталов: сады, кусты, деревья, сараи, бельё, стога, тайники."""
        b = self.b
        st = b.st
        rnd = self.rnd
        cells = [c for c, i in self.cell.items()]
        rnd.shuffle(cells)
        made_stash = 0
        log, leaves = {'elf': ('minecraft:birch_log', 'minecraft:azalea_leaves'), 'demon': ('minecraft:crimson_stem', 'minecraft:nether_wart_block'),
                       'dwarf': ('minecraft:spruce_log', 'minecraft:spruce_leaves'), 'beast': ('minecraft:spruce_log', 'minecraft:spruce_leaves'),
                       'orc': ('minecraft:acacia_log', 'minecraft:acacia_leaves')}.get(b.style, ('minecraft:oak_log', 'minecraft:oak_leaves'))
        kinds = ['tree', 'bush', 'bush', 'flowers', 'flowers', 'garden', 'shed', 'laundry', 'hay', 'crates', 'woodpile', 'bench', 'tree', 'bush']
        for (x, z) in cells[:int(len(cells) * 0.16 * density)]:
            i = self.cell[(x, z)]
            h = self.h[i]
            y = self.G0 + h
            kind = rnd.choice(kinds)
            size = {'tree': 2, 'garden': 2, 'shed': 2, 'laundry': 3}.get(kind, 0)
            area = [(x + dx, z + dz) for dx in range(-size, size + 1) for dz in range(-size, size + 1)]
            if any(self.cell.get(c) != i for c in area):
                continue
            if any(not b.free(c[0], c[1], c[0], c[1]) for c in area):
                continue
            if any(not _air(b, c[0], y, c[1]) for c in area):
                continue
            G0 = self.G0
            b.G = y
            try:
                if kind == 'tree':
                    if leaves.endswith('block'):
                        b.tree(x, z, rnd.randint(4, 6), log, leaves, 2.0)
                    else:
                        b.tree(x, z, rnd.randint(4, 7), log, leaves, rnd.choice([2.0, 2.4, 2.8]))
                elif kind == 'bush':
                    for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
                        if rnd.random() < 0.8:
                            b.set(x + dx, y, z + dz, rnd.choice(['minecraft:azalea_leaves', 'minecraft:oak_leaves', 'minecraft:flowering_azalea_leaves',
                                                                  leaves if not leaves.endswith('block') else 'minecraft:azalea_leaves']),
                                  persistent='true', distance='1', waterlogged='false')
                    if rnd.random() < 0.35 and made_stash < stashes:
                        stash(b, x - 1, y, z, 'barrel')
                        made_stash += 1
                elif kind == 'flowers':
                    for dx in (-1, 0, 1):
                        for dz in (-1, 0, 1):
                            if rnd.random() < 0.6:
                                b.set(x + dx, y, z + dz, 'minecraft:' + rnd.choice(['poppy', 'dandelion', 'cornflower', 'allium', 'oxeye_daisy',
                                                                                     'azure_bluet', 'lily_of_the_valley', 'fern', 'red_tulip', 'white_tulip']))
                elif kind == 'garden':
                    for dx in range(-2, 3):
                        for dz in range(-2, 3):
                            if abs(dx) == 2 or abs(dz) == 2:
                                b.set(x + dx, y, z + dz, st['fence'], waterlogged='false') if (dx + dz) % 2 else \
                                    b.set(x + dx, y, z + dz, 'minecraft:' + rnd.choice(['rose_bush', 'lilac', 'peony']), half='lower')
                            else:
                                b.set(x + dx, y - 1, z + dz, 'minecraft:farmland', moisture='7')
                                crop = rnd.choice(['minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots', 'minecraft:wheat'])
                                b.set(x + dx, y, z + dz, crop, age='3' if crop.endswith('beetroots') else '7')
                    b.set(x, y - 1, z, 'minecraft:water', level='0')
                    b.set(x, y, z, 'minecraft:oak_trapdoor', facing='north', half='bottom', open='false', powered='false', waterlogged='true')
                elif kind == 'shed':
                    plank = st['plank']
                    for dx in range(-2, 3):
                        for dz in range(-1, 2):
                            edge = abs(dx) == 2 or abs(dz) == 1
                            for yy in range(y, y + 3):
                                if edge and not (dz == 1 and dx == 0 and yy < y + 2):
                                    b.set(x + dx, yy, z + dz, plank)
                            b.slab(x + dx, y + 3, z + dz, plank.replace('_planks', '_slab') if 'planks' in plank else 'minecraft:spruce_slab')
                    b.set(x - 1, y, z, 'minecraft:barrel', facing='up', open='false')
                    b.set(x + 1, y, z, 'minecraft:hay_block', axis='y')
                    if made_stash < stashes and rnd.random() < 0.6:
                        stash(b, x, y, z, 'chest')
                        made_stash += 1
                elif kind == 'laundry':
                    for dx in (-3, 3):
                        for yy in range(y, y + 3):
                            b.set(x + dx, yy, z, st['fence'], waterlogged='false')
                    for dx in range(-2, 3):
                        b.set(x + dx, y + 2, z, 'minecraft:chain', axis='x', waterlogged='false')
                        if dx % 2:
                            b.set(x + dx, y + 1, z, 'minecraft:' + rnd.choice(['white', 'light_blue', 'red', 'yellow', 'lime']) + '_wool')
                elif kind == 'hay':
                    b.set(x, y, z, 'minecraft:hay_block', axis='y')
                    b.set(x + 1, y, z, 'minecraft:hay_block', axis='x')
                    b.set(x, y + 1, z, 'minecraft:hay_block', axis='z')
                elif kind == 'crates':
                    b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
                    b.set(x + 1, y, z, 'minecraft:barrel', facing='north', open='false')
                    b.set(x, y + 1, z, 'minecraft:barrel', facing='up', open='false')
                    b.set(x, y, z + 1, 'minecraft:composter', level='4')
                elif kind == 'woodpile':
                    for dx in range(0, 3):
                        b.set(x + dx, y, z, 'minecraft:spruce_log', axis='z')
                        if dx < 2:
                            b.set(x + dx, y + 1, z, 'minecraft:spruce_log', axis='z')
                    b.set(x + 2, y + 1, z, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
                elif kind == 'bench':
                    b.bench(x, z, 'south')
                    b.bench(x + 1, z, 'south')
                    b.set(x - 1, y, z, 'minecraft:potted_' + rnd.choice(['fern', 'red_tulip', 'azure_bluet']))
                b.reserve(x - size, z - size, x + size, z + size)
            finally:
                b.G = G0
            # жители во дворах
            if citizen and rnd.random() < 0.05:
                b.entities.append((x, y, z, citizen, {}))
        # мелочь: куры и кошки во дворах
        for (x, z) in cells[:int(len(cells) * 0.004 * density) + 2]:
            i = self.cell[(x, z)]
            y = self.G0 + self.h[i]
            if _air(b, x, y, z) and _air(b, x, y + 1, z):
                b.entities.append((x, y, z, rnd.choice(['minecraft:chicken', 'minecraft:cat', 'minecraft:chicken', 'minecraft:rabbit']), {}))

    # ------------------------------------------------------------------
    def street_life(self, lamp_every=11):
        """Фонари, кадки с цветами, бочки у стен вдоль улиц; маленькие площади на перекрёстках."""
        b = self.b
        rnd = self.rnd
        G = self.G0
        st = b.st
        streets = sorted(self.street)
        rnd.shuffle(streets)
        k = 0
        for (x, z) in streets:
            # клетка на краю улицы (рядом квартал), чтобы не перегораживать проход
            edge = [(dx, dz) for _, dx, dz in D4 if (x + dx, z + dz) in self.cell]
            if not edge or not _air(b, x, G, z) or not b.free(x, z, x, z):
                continue
            # не ставить в узких местах (оба соседа поперёк — кварталы)
            open_n = sum(1 for _, dx, dz in D4 if (x + dx, z + dz) in self.street)
            if open_n < 3:
                continue
            k += 1
            roll = rnd.random()
            if k % lamp_every == 0:
                b.lamp_post(x, z, 3)
            elif roll < 0.06:
                b.set(x, G, z, 'minecraft:barrel', facing='up', open='false')
            elif roll < 0.11:
                b.set(x, G, z, 'minecraft:spruce_trapdoor', facing='north', half='top', open='false', powered='false', waterlogged='false')
                b.set(x, G + 1, z, 'minecraft:potted_' + rnd.choice(['red_tulip', 'oxeye_daisy', 'fern', 'azure_bluet', 'cornflower']))
            elif roll < 0.13:
                b.set(x, G, z, 'minecraft:composter', level='5')
            else:
                continue
            b.reserve(x, z, x, z)
        # площади на перекрёстках: где улица широкая — колодец или дерево в кадке
        squares = 0
        for (x, z) in streets:
            if squares > len(self.seeds) // 4:
                break
            if all((x + dx, z + dz) in self.street for dx in range(-3, 4) for dz in range(-3, 4)) and \
                    math.hypot(x - self.C, z - self.C) > self.plaza_r + 4 and b.free(x - 2, z - 2, x + 2, z + 2):
                if rnd.random() < 0.5:
                    b.well(x, z)
                else:
                    for dx in range(-1, 2):
                        for dz in range(-1, 2):
                            b.set(x + dx, G, z + dz, b.bp(st['found'], x + dx, G, z + dz)) if (dx or dz) else None
                    b.set(x, G, z, 'minecraft:grass_block', snowy='false')
                    b.G = G + 1
                    b.tree(x, z, 4, 'minecraft:oak_log' if b.style == 'human' else 'minecraft:birch_log', 'minecraft:oak_leaves', 2.0)
                    b.G = G
                b.reserve(x - 2, z - 2, x + 2, z + 2)
                squares += 1


# ======================================================================
#                                РЫНОК
# ======================================================================

def market(b, x0, z0, x1, z1, crafts, merchant, citizen, seed, ground=None):
    """Тесный рынок: ряды лавок вплотную, узкие проходы, ящики, телеги, толпа."""
    from mapbuild_crafts import craft_stall_compact
    rnd = random.Random(str(seed) + 'mkt')
    G = b.G
    st = b.st
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, G - 1, z, ground or b.bp(st['street'], x, z, 0))
            for y in range(G, G + 5):
                if (x, y, z) in b.blocks and b.blocks[(x, y, z)][0] != 'minecraft:air':
                    b.set(x, y, z, 'minecraft:air')
    colors = ['red', 'yellow', 'blue', 'green', 'orange', 'purple', 'white', 'cyan', 'lime', 'magenta', 'brown', 'light_blue', 'pink']
    crafts = list(crafts)
    k = 0
    # ряды: лавка 5×3 вплотную друг к другу, между рядами проход 2-3 блока
    z = z0 + 1
    row = 0
    while z + 2 <= z1 - 1:
        facing = 'south' if row % 2 == 0 else 'north'
        x = x0 + 2 + (row % 2) * 2
        while x + 2 <= x1 - 1:
            if rnd.random() < 0.12:
                # прореха с бочками и ящиками
                for dx in range(-2, 3):
                    if rnd.random() < 0.6:
                        b.set(x + dx, G, z + 1, 'minecraft:barrel', facing='up', open='false')
                        if rnd.random() < 0.35:
                            b.set(x + dx, G + 1, z + 1, 'minecraft:barrel', facing='up', open='false')
            else:
                craft = crafts[k % len(crafts)] if crafts and rnd.random() < 0.75 else None
                craft_stall_compact(b, x, z + 1, craft, colors[k % len(colors)], facing, merchant)
                k += 1
            x += 5
        z += 3 + (2 if row % 2 == 0 else 3)
        row += 1
    # толпа покупателей
    for i in range(max(4, (x1 - x0) * (z1 - z0) // 90)):
        px, pz = rnd.randint(x0 + 1, x1 - 1), rnd.randint(z0 + 1, z1 - 1)
        if _air(b, px, G, pz) and _air(b, px, G + 1, pz):
            b.entities.append((px, G, pz, citizen, {}))
    for i in range(3):
        px, pz = rnd.randint(x0 + 1, x1 - 1), rnd.randint(z0 + 1, z1 - 1)
        if _air(b, px, G, pz):
            b.entities.append((px, G, pz, 'minecraft:chicken', {}))
    b.reserve(x0, z0, x1, z1)


# ======================================================================
#                          ЗА СТЕНАМИ
# ======================================================================

def outskirts(b, C, wall_r, seed, citizen):
    """Поля, огороды и сельские домики в свободных углах плитки за стенами."""
    rnd = random.Random(str(seed) + 'out')
    S = b.size[0]
    G = b.G
    st = b.st
    crops = ['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots']
    for _ in range(60):
        x, z = rnd.randint(4, S - 12), rnd.randint(4, S - 12)
        w, d = rnd.randint(7, 12), rnd.randint(5, 8)
        corners = [(x, z), (x + w, z), (x, z + d), (x + w, z + d)]
        if any(math.hypot(cx - C, cz - C) < wall_r + 6 for cx, cz in corners):
            continue
        if any(not (0 <= cx < S and 0 <= cz < S) for cx, cz in corners):
            continue
        if not b.free(x - 1, z - 1, x + w + 1, z + d + 1):
            continue
        if rnd.random() < 0.3:
            # сельский домик с огородом
            door = rnd.choice(['north', 'south', 'east', 'west'])
            b.house(x, z, min(w, 8), min(d + 1, 7), floors=1, door=door, interior='home', npc=citizen if rnd.random() < 0.6 else None)
            continue
        crop = rnd.choice(crops)
        for dx in range(w):
            for dz in range(d):
                if dz % 4 == 2:
                    b.set(x + dx, G - 1, z + dz, 'minecraft:water', level='0')
                else:
                    b.set(x + dx, G - 1, z + dz, 'minecraft:farmland', moisture='7')
                    b.set(x + dx, G, z + dz, crop, age='3' if crop.endswith('beetroots') else '7')
        for dx in range(-1, w + 1):
            for dz in (-1, d):
                b.set(x + dx, G, z + dz, st['fence'], waterlogged='false') if (dx + dz) % 3 else None
        if rnd.random() < 0.5:
            # пугало
            sx, sz = x + w // 2, z + 1
            b.set(sx, G, sz, st['fence'], waterlogged='false')
            b.set(sx, G + 1, sz, 'minecraft:hay_block', axis='y')
            b.set(sx, G + 2, sz, 'minecraft:carved_pumpkin', facing='south')
        b.reserve(x - 1, z - 1, x + w + 1, z + d + 1)
