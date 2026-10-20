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


SLAB = {'minecraft:stone_bricks': 'minecraft:stone_brick_slab', 'minecraft:mossy_stone_bricks': 'minecraft:mossy_stone_brick_slab',
        'minecraft:andesite': 'minecraft:andesite_slab', 'minecraft:polished_andesite': 'minecraft:polished_andesite_slab',
        'minecraft:cobblestone': 'minecraft:cobblestone_slab', 'minecraft:mossy_cobblestone': 'minecraft:mossy_cobblestone_slab',
        'minecraft:calcite': 'minecraft:diorite_slab', 'minecraft:moss_block': 'minecraft:mossy_cobblestone_slab',
        'minecraft:deepslate_tiles': 'minecraft:deepslate_tile_slab', 'minecraft:polished_deepslate': 'minecraft:polished_deepslate_slab',
        'minecraft:blackstone': 'minecraft:blackstone_slab', 'minecraft:polished_blackstone': 'minecraft:polished_blackstone_slab',
        'minecraft:basalt': 'minecraft:polished_blackstone_slab', 'minecraft:packed_mud': 'minecraft:mud_brick_slab',
        'minecraft:mud_bricks': 'minecraft:mud_brick_slab', 'minecraft:coarse_dirt': 'minecraft:mud_brick_slab',
        'minecraft:dirt_path': 'minecraft:mud_brick_slab', 'minecraft:podzol': 'minecraft:spruce_slab', 'minecraft:gravel': 'minecraft:cobblestone_slab'}


class Organic:
    """
    Кварталы по Вороного внутри области (круг R или своя фигура inside(x, z)), кривые улочки,
    кольцевая улица вдоль стены. Рельеф — плавные холмы высотой до relief блоков: улицы поднимаются
    полублоками (плиты — ходить без прыжков), у особых мест (площадь, проспекты, рынок, стена) — ровно.
    Дом ставится на уровне своей улицы, под ним и двором — ровная площадка на фундаменте.
    """

    def __init__(self, b, C, R, seed, plaza_r=8, terrace=2, street_w=1.7, block_area=560, inside=None, rim=3):
        self.b, self.C, self.R = b, C, R
        self.rnd = random.Random(str(seed) + 'org')
        self.terrace = terrace  # высота холмов в блоках
        self.plaza_r = plaza_r
        self.G0 = b.G
        S = b.size[0]
        self.S = S
        inside = inside or (lambda x, z: math.hypot(x - C, z - C) <= R)
        self.inside = inside
        special = list(b.reserved)

        def is_special(x, z):
            for (a1, c1, a2, c2) in special:
                if a1 <= x <= a2 and c1 <= z <= c2:
                    return True
            return False

        region = set()
        for x in range(S):
            for z in range(S):
                if inside(x, z):
                    region.add((x, z))
        self.region = region
        # расстояние до края области — кольцевая улица вдоль стены
        dout = self._dist(lambda p: p not in region, region)
        n = max(6, int(len(region) / block_area))
        seeds = []
        span = int(max(math.hypot(x - C, z - C) for (x, z) in region)) + 1 if region else R
        for _ in range(n * 12):
            if len(seeds) >= n:
                break
            a = self.rnd.random() * 2 * math.pi
            r = math.sqrt(self.rnd.random()) * span
            x, z = C + math.cos(a) * r, C + math.sin(a) * r
            if r < plaza_r + 6 or (int(x), int(z)) not in region or dout.get((int(x), int(z)), 0) < rim + 4:
                continue
            if all(math.hypot(x - sx, z - sz) > 11 for sx, sz in seeds):
                seeds.append((x, z))
        self.seeds = seeds
        self.cell = {}      # (x,z) -> id квартала
        self.street = set()
        self.special = set()
        self.flat = set()   # ровные места (рынок)
        for (x, z) in region:
            if is_special(x, z):
                self.special.add((x, z))
                continue
            r = math.hypot(x - C, z - C)
            if r < plaza_r or dout[(x, z)] <= rim:
                self.street.add((x, z))
                continue
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
        # тротуар в 2 клетки вдоль проспектов, площадей и особых зданий — на нём мостовая успевает подняться к кварталу
        sp = self.special
        for (x, z) in list(self.cell):
            if any((x + dx, z + dz) in sp for dx in range(-2, 3) for dz in range(-2, 3)):
                del self.cell[(x, z)]
                self.street.add((x, z))
        self.hh = None      # высота в полублоках
        self.foot = set()   # клетки под зданиями (фундамент)

    # ------------------------------------------------------------------
    def _dist(self, is_src, area):
        """Расстояние (шаги по 8 соседям) от клеток area до ближайшего источника."""
        from collections import deque
        dist = {}
        q = deque()
        for (x, z) in area:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if (dx or dz) and is_src((x + dx, z + dz)):
                        dist[(x, z)] = 1
                        q.append((x, z))
                        break
                else:
                    continue
                break
        while q:
            x, z = q.popleft()
            d = dist[(x, z)]
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    p = (x + dx, z + dz)
                    if p in area and p not in dist:
                        dist[p] = d + 1
                        q.append(p)
        return dist

    def _heights(self):
        """
        Высоты: у каждого квартала своя ровная высота (в блоках), соседние кварталы отличаются
        не больше чем на блок, у площадей/проспектов/стены — ноль. Улица у края квартала — вровень
        с ним, между кварталами разной высоты мостовая плавно поднимается полублоками.
        """
        if self.hh is not None:
            return
        area = set(self.cell) | self.street
        area -= self.flat
        d = self._dist(lambda p: p not in area, area)
        amp = self.terrace
        ph = (self.C * 0.37) % 6.28
        dq = {}
        for p_, i in self.cell.items():
            dq[i] = min(dq.get(i, 1e9), d.get(p_, 0))
        H = {}
        for i, (sx, sz) in enumerate(self.seeds):
            v = 0.5 + 0.32 * math.sin(sx * 0.041 + ph) * math.cos(sz * 0.035 + 0.4) + 0.18 * math.sin((sx + sz) * 0.023 + 1.1)
            # чтобы мостовая поднималась не круче полублока на клетку: не выше (расстояние до ровного места − 1) / 2
            H[i] = max(0, min(int(round(amp * v)), (dq.get(i, 0) - 1) // 2))
        # соседние кварталы (через улицу) — не больше блока разницы
        pairs = set()
        for (x, z) in self.street:
            ids = {self.cell[(x + dx, z + dz)] for dx in range(-4, 5) for dz in range(-4, 5) if (x + dx, z + dz) in self.cell}
            ids = sorted(ids)
            for a in range(len(ids)):
                for c in range(a + 1, len(ids)):
                    pairs.add((ids[a], ids[c]))
        changed = True
        while changed:
            changed = False
            for a, c in pairs:
                if H[a] > H[c] + 1:
                    H[a] = H[c] + 1
                    changed = True
                elif H[c] > H[a] + 1:
                    H[c] = H[a] + 1
                    changed = True
        self.H = H
        # улицы: у кварталов — вровень, у особых мест — ноль, остальное — плавно
        fixed = {}
        for (x, z) in self.street:
            if (x, z) in self.flat:
                continue
            vals = []
            for _, dx, dz in D4:
                n = (x + dx, z + dz)
                if n in self.cell:
                    vals.append(2 * H[self.cell[n]])
                elif n not in area:
                    vals.append(0)
            if vals:
                fixed[(x, z)] = min(vals)
        val = dict(fixed)
        # начальное приближение — ближайшая закреплённая клетка
        from collections import deque
        q = deque(fixed)
        while q:
            c = q.popleft()
            for _, dx, dz in D4:
                n = (c[0] + dx, c[1] + dz)
                if n in self.street and n not in val and n not in self.flat:
                    val[n] = val[c]
                    q.append(n)
        free = [c for c in val if c not in fixed]
        vf = {c: float(v) for c, v in val.items()}
        for _ in range(60):
            for c in free:
                nb = [vf[(c[0] + dx, c[1] + dz)] for _, dx, dz in D4 if (c[0] + dx, c[1] + dz) in vf]
                if nb:
                    vf[c] = sum(nb) / len(nb)
        hh = {c: int(round(v)) for c, v in vf.items()}
        # сгладить остатки: соседние клетки мостовой — не больше полублока разницы
        for _ in range(20):
            bad = 0
            for (x, z), v in list(hh.items()):
                for dx, dz in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                    n = (x + dx, z + dz)
                    if n in hh and v - hh[n] > 1:
                        bad += 1
                        if (x, z) not in fixed or n in fixed:
                            # край мостовой у квартала может опуститься на полблока — ступенька в полблока ходится
                            hh[(x, z)] = v = hh[n] + 1
                        else:
                            hh[n] = v - 1
            if not bad:
                break
        self.hh = hh

    def street_level(self, x, z):
        """Высота улицы в полублоках."""
        return self.hh.get((x, z), 0) if self.hh else 0

    def ground(self, x, z):
        """Высота земли квартала в блоках над G0."""
        i = self.cell.get((x, z))
        if i is None:
            return (self.street_level(x, z) + 1) // 2
        return self.H.get(i, 0)

    def street_y(self, x, z):
        """y, где стоят ноги на улице (для полублока — над плитой, такие клетки лучше не занимать)."""
        return self.G0 + (self.street_level(x, z) + 1) // 2

    def is_streetish(self, x, z):
        return (x, z) in self.street or (x, z) in self.special and _air(self.b, x, self.G0, z)

    def paint_ground(self):
        """Улицы: мостовая с подъёмами из плит. Дворы красятся позже (paint_lots) — после площадок под дома."""
        self._heights()
        b, G = self.b, self.G0
        st = b.st
        for (x, z) in self.street:
            h = self.street_level(x, z)
            k = h // 2
            top = b.bp(st['street'], x, 0, z)
            for y in range(G - 2, G - 1 + k):
                b.set(x, y, z, 'minecraft:stone')
            b.set(x, G - 1 + k, z, top)
            if h % 2:
                b.set(x, G + k, z, SLAB.get(top, 'minecraft:cobblestone_slab'), type='bottom', waterlogged='false')
            elif (x, G + k, z) in b.blocks and b.blocks[(x, G + k, z)][0].endswith('_slab'):
                b.set(x, G + k, z, 'minecraft:air')
        self._lots_painted = False

    def paint_lots(self):
        if getattr(self, '_lots_painted', True) is not False:
            return
        self._lots_painted = True
        b, G = self.b, self.G0
        st = b.st
        found = st['found']
        soft = b.style not in ('demon', 'dwarf')
        for (x, z) in self.cell:
            h = self.ground(x, z)
            under = (x, z) in self.foot
            for y in range(G - 1, G - 1 + h):
                if (x, y, z) not in b.blocks or b.blocks[(x, y, z)][0] == 'minecraft:air':
                    b.set(x, y, z, b.bp(found, x, y, z) if under else 'minecraft:dirt')
            top = G - 1 + h
            if (x, top, z) in b.blocks and b.blocks[(x, top, z)][0] != 'minecraft:air':
                continue
            if under:
                b.set(x, top, z, b.bp(found, x, top, z))
            elif soft:
                b.set(x, top, z, 'minecraft:grass_block', snowy='false')
            else:
                b.set(x, top, z, b.bp(st['path'], x, 0, z))

    # ------------------------------------------------------------------
    def _rect_ok(self, bid, x0, z0, x1, z1):
        b = self.b
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if self.cell.get((x, z)) != bid or (x, z) in self.foot:
                    return False
        for (a1, c1, a2, c2) in b.reserved:
            if x0 <= a2 and x1 >= a1 and z0 <= c2 and z1 >= c1:
                return False
        return True

    def lots(self, place_fn, sizes, max_lots=None, prefer_center=False):
        """Пройтись по фасадам кварталов и ставить здания (place_fn(rect, door, h) -> bool) на уровне улицы."""
        self._heights()
        fronts = []
        for (x, z), i in self.cell.items():
            if (x, z) in self.foot:
                continue
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
            if (x, z) in self.foot:
                continue
            rect = None
            for attempt in range(4):
                w, d = sizes()
                for shift in (w // 2, 1, w - 2):
                    if side in ('north', 'south'):
                        x0 = x - shift
                        z0 = z if side == 'north' else z - d + 1
                        cand = (x0, z0, x0 + w - 1, z0 + d - 1)
                    else:
                        z0 = z - shift
                        x0 = x if side == 'west' else x - d + 1
                        cand = (x0, z0, x0 + d - 1, z0 + w - 1)
                    if self._rect_ok(i, *cand):
                        rect = cand
                        break
                if rect:
                    break
            if rect is None:
                continue
            x0, z0, x1, z1 = rect
            h = self.H[i]  # пол — на уровне квартала, мостовая у фасада вровень
            G0 = self.G0
            self.b.G = G0 + h
            try:
                ok = place_fn(rect, side, h)
            finally:
                self.b.G = G0
            if ok:
                placed += 1
                for xx in range(x0, x1 + 1):
                    for zz in range(z0, z1 + 1):
                        self.foot.add((xx, zz))
                self.steps(side, h)
        return placed

    def steps(self, side, h):
        """Ступени от двери вниз к улице, если пол выше мостовой больше чем на полблока."""
        b = self.b
        if not hasattr(b, 'last_door'):
            return
        dx_, dz_, door = b.last_door
        if door != side:
            return
        ox, oz = DIRS[door]
        G0 = self.G0
        stair = 'minecraft:stone_brick_stairs' if b.style in ('human', 'dwarf') else \
            b.st['plank'].replace('_planks', '_stairs') if 'planks' in b.st['plank'] else 'minecraft:stone_brick_stairs'
        level = 2 * h  # полублоки
        for k in range(1, 5):
            x, z = dx_ + ox * k, dz_ + oz * k
            s = self.street_level(x, z) if (x, z) in self.street else 2 * self.ground(x, z)
            if level - s <= 1:
                break
            y = G0 + level // 2 - 1
            b.set(x, y, z, stair, facing=OPP[door], half='bottom', shape='straight', waterlogged='false')
            for yy in range(y - 1, G0 - 2, -1):
                if (x, yy, z) in b.blocks and b.blocks[(x, yy, z)][0] not in ('minecraft:air',) and not b.blocks[(x, yy, z)][0].endswith('_slab'):
                    break
                b.set(x, yy, z, b.bp(b.st['found'], x, yy, z))
            for yy in range(y + 1, y + 4):
                b.set(x, yy, z, 'minecraft:air')
            level -= 2

    # ------------------------------------------------------------------
    def courtyards(self, citizen=None, density=1.0, stashes=3):
        """Дворы внутри кварталов: сады, кусты, деревья, сараи, бельё, стога, тайники."""
        self.paint_lots()
        b = self.b
        st = b.st
        rnd = self.rnd
        cells = [c for c, i in self.cell.items() if c not in self.foot]
        rnd.shuffle(cells)
        made_stash = 0
        log, leaves = {'elf': ('minecraft:birch_log', 'minecraft:azalea_leaves'), 'demon': ('minecraft:crimson_stem', 'minecraft:nether_wart_block'),
                       'dwarf': ('minecraft:spruce_log', 'minecraft:spruce_leaves'), 'beast': ('minecraft:spruce_log', 'minecraft:spruce_leaves'),
                       'orc': ('minecraft:acacia_log', 'minecraft:acacia_leaves')}.get(b.style, ('minecraft:oak_log', 'minecraft:oak_leaves'))
        kinds = ['tree', 'bush', 'bush', 'flowers', 'flowers', 'garden', 'shed', 'laundry', 'hay', 'crates', 'woodpile', 'bench', 'tree', 'bush']
        for (x, z) in cells[:int(len(cells) * 0.16 * density)]:
            i = self.cell[(x, z)]
            h = self.ground(x, z)
            y = self.G0 + h
            kind = rnd.choice(kinds)
            size = {'tree': 2, 'garden': 2, 'shed': 2, 'laundry': 3}.get(kind, 0)
            area = [(x + dx, z + dz) for dx in range(-size, size + 1) for dz in range(-size, size + 1)]
            if any(self.cell.get(c) != i or self.ground(*c) != h or c in self.foot for c in area):
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
            y = self.G0 + self.ground(x, z)
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
            if self.street_level(x, z) % 2:
                continue  # на плитах-подъёмах ничего не ставим
            G = self.street_y(x, z)
            # клетка на краю улицы (рядом квартал), чтобы не перегораживать проход
            edge = [(dx, dz) for _, dx, dz in D4 if (x + dx, z + dz) in self.cell]
            if not edge or not _air(b, x, G, z) or not b.free(x, z, x, z):
                continue
            # не ставить в узких местах (оба соседа поперёк — кварталы)
            open_n = sum(1 for _, dx, dz in D4 if (x + dx, z + dz) in self.street)
            if open_n < 3:
                continue
            # только у стены широкой улицы: поперёк ещё минимум 3 клетки мостовой для прохода
            ex, ez = edge[0]
            if not all((x - ex * k, z - ez * k) in self.street for k in range(1, 4)):
                continue
            k += 1
            roll = rnd.random()
            if k % lamp_every == 0:
                b.G = G
                b.lamp_post(x, z, 3)
                b.G = self.G0
            elif roll < 0.07:
                b.set(x, G, z, 'minecraft:spruce_trapdoor', facing='north', half='top', open='false', powered='false', waterlogged='false')
                b.set(x, G + 1, z, 'minecraft:potted_' + rnd.choice(['red_tulip', 'oxeye_daisy', 'fern', 'azure_bluet', 'cornflower']))
            else:
                continue
            b.reserve(x, z, x, z)
        # площади на перекрёстках: где улица широкая — колодец или дерево в кадке
        squares = 0
        for (x, z) in streets:
            if squares > len(self.seeds) // 4:
                break
            G = self.street_y(x, z)
            lv = self.street_level(x, z)
            if lv % 2 == 0 and all((x + dx, z + dz) in self.street and self.street_level(x + dx, z + dz) == lv
                                   for dx in range(-3, 4) for dz in range(-3, 4)) and \
                    math.hypot(x - self.C, z - self.C) > self.plaza_r + 4 and b.free(x - 2, z - 2, x + 2, z + 2):
                b.G = G
                if rnd.random() < 0.5:
                    b.well(x, z)
                else:
                    for dx in range(-1, 2):
                        for dz in range(-1, 2):
                            b.set(x + dx, G, z + dz, b.bp(st['found'], x + dx, G, z + dz)) if (dx or dz) else None
                    b.set(x, G, z, 'minecraft:grass_block', snowy='false')
                    b.G = G + 1
                    b.tree(x, z, 4, 'minecraft:oak_log' if b.style == 'human' else 'minecraft:birch_log', 'minecraft:oak_leaves', 2.0)
                b.G = self.G0
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

def outskirts(b, C, wall_r, seed, citizen, outer=None):
    """
    Земли за стенами: большие наделы из длинных полос (как средневековые поля) с разными
    культурами, оросительными канавами и межами-тропинками, стога, пугала, полевые сараи,
    сады, пастбища с навесом и сельские домики с огородами.
    """
    rnd = random.Random(str(seed) + 'out')
    S = b.size[0]
    G = b.G
    st = b.st
    outer = outer or (S // 2 - 2)
    crops = ['minecraft:wheat', 'minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots']
    if b.style == 'demon':
        crops = ['minecraft:nether_wart']

    def ok_rect(x0, z0, x1, z1):
        corners = [(x0, z0), (x1, z0), (x0, z1), (x1, z1), ((x0 + x1) // 2, z0), ((x0 + x1) // 2, z1), (x0, (z0 + z1) // 2), (x1, (z0 + z1) // 2)]
        if any(math.hypot(cx - C, cz - C) < wall_r + 4 or math.hypot(cx - C, cz - C) > outer for cx, cz in corners):
            return False
        if x0 < 1 or z0 < 1 or x1 >= S - 1 or z1 >= S - 1:
            return False
        return b.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1)

    def plant(x, z, crop):
        if crop.endswith('nether_wart'):
            b.set(x, G - 1, z, 'minecraft:soul_sand')
            b.set(x, G, z, crop, age='3')
        else:
            b.set(x, G - 1, z, 'minecraft:farmland', moisture='7')
            b.set(x, G, z, crop, age='3' if crop.endswith('beetroots') else '7')

    def scarecrow(x, z, facing):
        b.set(x, G, z, st['fence'], waterlogged='false')
        b.set(x, G + 1, z, 'minecraft:hay_block', axis='y')
        b.set(x, G + 2, z, 'minecraft:carved_pumpkin', facing=facing)
        for d in (-1, 1):
            ax, az = (x + d, z) if facing in ('north', 'south') else (x, z + d)
            b.set(ax, G + 1, az, 'minecraft:spruce_fence', waterlogged='false')

    def field(x0, z0, x1, z1):
        """Надел: полосы по 4-6 рядов вдоль длинной стороны, между ними канавы; межа по краю."""
        along_x = (x1 - x0) >= (z1 - z0)
        lo, hi = (z0, z1) if along_x else (x0, x1)
        pos = lo
        while pos <= hi:
            wdt = rnd.randint(4, 6)
            crop = rnd.choice(crops)
            for t in range(pos, min(hi, pos + wdt - 1) + 1):
                for u in range((x0 if along_x else z0), (x1 if along_x else z1) + 1):
                    x, z = (u, t) if along_x else (t, u)
                    plant(x, z, crop)
            ch = pos + wdt
            if ch <= hi:
                for u in range((x0 if along_x else z0), (x1 if along_x else z1) + 1):
                    x, z = (u, ch) if along_x else (ch, u)
                    if crop.endswith('nether_wart'):
                        b.set(x, G - 1, z, 'minecraft:soul_soil')
                    else:
                        b.set(x, G - 1, z, 'minecraft:water', level='0')
                        if u % 9 == 4:
                            # мостки через канаву
                            b.set(x, G - 1, z, 'minecraft:spruce_slab', type='top', waterlogged='true')
            pos = ch + 1
        # межа-тропинка и редкий плетень вокруг, вход по тропе
        for x in range(x0 - 1, x1 + 2):
            for z in (z0 - 1, z1 + 1):
                b.set(x, G - 1, z, b.bp(st['path'], x, 0, z))
                if (x * 7 + z) % 5 == 0:
                    b.set(x, G, z, st['fence'], waterlogged='false')
                    if (x + z) % 3 == 0:
                        b.set(x, G + 1, z, 'minecraft:lantern', hanging='false', waterlogged='false')
        for z in range(z0, z1 + 1):
            for x in (x0 - 1, x1 + 1):
                b.set(x, G - 1, z, b.bp(st['path'], x, 0, z))
                if (z * 7 + x) % 5 == 0:
                    b.set(x, G, z, st['fence'], waterlogged='false')
        # пугала и стога на меже
        for k in range(rnd.randint(1, 2)):
            sx, sz = rnd.randint(x0 + 2, x1 - 2), rnd.randint(z0 + 2, z1 - 2)
            if b.blocks.get((sx, G - 1, sz), ('',))[0] == 'minecraft:farmland':
                b.set(sx, G - 1, sz, 'minecraft:coarse_dirt')
                scarecrow(sx, sz, rnd.choice(['north', 'south', 'east', 'west']))
        for k in range(rnd.randint(1, 3)):
            hx, hz = (rnd.randint(x0, x1), z1 + 2) if rnd.random() < 0.5 else (x1 + 2, rnd.randint(z0, z1))
            if b.free(hx, hz, hx, hz) and 0 < hx < S - 2 and 0 < hz < S - 2:
                b.set(hx, G, hz, 'minecraft:hay_block', axis='y')
                b.set(hx + 1, G, hz, 'minecraft:hay_block', axis='x')
                b.set(hx, G + 1, hz, 'minecraft:hay_block', axis='z')
        b.reserve(x0 - 2, z0 - 2, x1 + 2, z1 + 2)

    def pasture(x0, z0, x1, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    b.set(x, G, z, st['fence'], waterlogged='false')
                elif rnd.random() < 0.12:
                    b.set(x, G, z, 'minecraft:grass')
        # навес-укрытие в углу
        for x in range(x0 + 1, x0 + 5):
            for z in range(z0 + 1, z0 + 4):
                if (x in (x0 + 1, x0 + 4)) and (z in (z0 + 1, z0 + 3)):
                    for y in range(G, G + 3):
                        b.set(x, y, z, 'minecraft:spruce_log', axis='y')
                b.set(x, G + 3, z, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
        b.set(x0 + 2, G - 1, z0 + 2, 'minecraft:water', level='0')
        b.set(x0 + 3, G, z0 + 2, 'minecraft:hay_block', axis='y') if False else None
        animals = rnd.choice([['minecraft:cow'] * 3 + ['minecraft:sheep'] * 3, ['minecraft:sheep'] * 5, ['minecraft:pig'] * 4 + ['minecraft:cow'] * 2])
        for a in animals:
            b.entities.append((rnd.randint(x0 + 2, x1 - 2), G, rnd.randint(min(z0 + 5, z1 - 2), z1 - 2), a, {}))
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)

    def orchard(x0, z0, x1, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
        for x in range(x0 + 2, x1 - 1, 5):
            for z in range(z0 + 2, z1 - 1, 5):
                b.tree(x, z, rnd.randint(3, 5), 'minecraft:oak_log', rnd.choice(['minecraft:oak_leaves', 'minecraft:flowering_azalea_leaves']), 2.0)
        for k in range(3):
            bx, bz = rnd.randint(x0, x1), rnd.randint(z0, z1)
            if (bx, G, bz) not in b.blocks:
                b.set(bx, G, bz, 'minecraft:beehive', facing='south', honey_level='3')
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)

    def melons(x0, z0, x1, z1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:coarse_dirt' if (z - z0) % 3 == 1 else 'minecraft:grass_block', **({} if (z - z0) % 3 == 1 else {'snowy': 'false'}))
                if (z - z0) % 3 != 1 and rnd.random() < 0.35:
                    b.set(x, G, z, rnd.choice(['minecraft:pumpkin', 'minecraft:melon', 'minecraft:pumpkin']))
        b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)

    made = {'field': 0, 'pasture': 0, 'orchard': 0, 'house': 0, 'melons': 0}
    spots = [(x, z) for x in range(2, S - 8, 4) for z in range(2, S - 8, 4)]
    seen = set()
    rnd.shuffle(spots)
    # сначала большие поля, потом пастбища, сады и домики в промежутках
    for (x, z) in spots + spots:
        kind = 'field' if made['field'] < 42 and (x, z) not in seen else rnd.choice(['pasture', 'orchard', 'house', 'melons', 'field'])
        seen.add((x, z))
        found = None
        for attempt in range(4):
            if kind == 'field':
                w, d = rnd.randint(22, 40) - attempt * 5, rnd.randint(14, 24) - attempt * 3
                if rnd.random() < 0.5:
                    w, d = d, w
            elif kind == 'house':
                w, d = rnd.randint(7, 9), rnd.randint(6, 7)
            elif kind == 'melons':
                w, d = rnd.randint(7, 12), rnd.randint(5, 9)
            else:
                w, d = rnd.randint(12, 20) - attempt * 2, max(9, rnd.randint(10, 16) - attempt * 2)
            if ok_rect(x, z, x + w - 1, z + d - 1):
                found = (w, d)
                break
        if not found:
            continue
        w, d = found
        if kind == 'field':
            field(x, z, x + w - 1, z + d - 1)
        elif kind == 'pasture':
            pasture(x, z, x + w - 1, z + d - 1)
        elif kind == 'orchard':
            orchard(x, z, x + w - 1, z + d - 1)
        elif kind == 'melons':
            melons(x, z, x + w - 1, z + d - 1)
        else:
            dx, dz = C - (x + w / 2), C - (z + d / 2)
            door = ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')
            b.house(x, z, w, d, floors=rnd.choice([1, 1, 2]), door=door, interior='home', npc=citizen if rnd.random() < 0.6 else None)
            # огородик у дома
            gx0, gz0 = (x, z + d + 1) if door != 'south' else (x, z - 5)
            if b.free(gx0, gz0, gx0 + w - 1, gz0 + 3) and 0 < gz0 and gz0 + 4 < S:
                for gx in range(gx0, gx0 + w):
                    for gz in range(gz0, gz0 + 3):
                        if gz == gz0 + 1 and gx == gx0 + w // 2:
                            b.set(gx, G - 1, gz, 'minecraft:water', level='0')
                        else:
                            plant(gx, gz, rnd.choice(crops))
                b.reserve(gx0, gz0, gx0 + w - 1, gz0 + 3)
        made[kind] += 1
    print('  за стенами:', made)
    return made
