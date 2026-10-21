"""
Города народов (2.8.0) — на уровне Аркадии, у каждого свой характер.
Каркас Town: стены-многоугольник с воротами точно на дорогах (каменные, частокол, живая изгородь),
проспекты от ворот к главной площади, подъездные дороги до края плитки (там начинается тракт),
особые места (резервируются до застройки), кварталы-Вороного с мастерскими и домами,
окрестности со своим укладом у каждого народа (поля, сады, рудники, загоны, лагеря...).
Сами города — в mapbuild_towns2.py.
"""
import math
import random

from mapbuild_lib import City, DIRS, OPP, TByte, TInt  # noqa: F401
import mapbuild_layout as ML


def cardinal(a):
    """Сторона света по углу (0 = восток, pi/2 = юг)."""
    k = int(round((a % (2 * math.pi)) / (math.pi / 2))) % 4
    return ['east', 'south', 'west', 'north'][k]


def _patch(x, z, seed, cell=9):
    """Пятна земли: плавный шум + немного россыпи, чтобы не было «шахматки»."""
    from worldmath import vn
    j = ((x * 73856093 ^ z * 83492791 ^ seed * 19349663) & 1023) / 1023.0
    return vn(x, z, cell, seed) * 0.82 + j * 0.18


def _pick(v, table):
    for lim, blk in table:
        if v < lim:
            return blk
    return table[-1][1]


GRASS = ('minecraft:grass_block', {'snowy': 'false'})
GROUND = {
    'human': lambda b, x, z: GRASS,
    'elf': lambda b, x, z: ('minecraft:moss_block', {}) if _patch(x, z, 3) < 0.22 else GRASS,
    'dwarf': lambda b, x, z: _pick(_patch(x, z, 5, 11), [(0.2, ('minecraft:stone', {})), (0.32, ('minecraft:gravel', {})),
                                                          (0.46, ('minecraft:coarse_dirt', {})), (1.1, GRASS)]),
    'orc': lambda b, x, z: _pick(_patch(x, z, 7), [(0.26, ('minecraft:coarse_dirt', {})), (0.36, ('minecraft:packed_mud', {})),
                                                   (0.44, ('minecraft:rooted_dirt', {})), (0.5, ('minecraft:red_sand', {})), (1.1, GRASS)]),
    'beast': lambda b, x, z: _pick(_patch(x, z, 9), [(0.3, ('minecraft:podzol', {'snowy': 'false'})), (0.4, ('minecraft:coarse_dirt', {})),
                                                     (1.1, GRASS)]),
    'demon': lambda b, x, z: _pick(_patch(x, z, 11), [(0.3, ('minecraft:blackstone', {})), (0.45, ('minecraft:basalt', {'axis': 'y'})),
                                                      (0.6, ('minecraft:soul_soil', {})), (0.75, ('minecraft:coarse_dirt', {})),
                                                      (1.1, ('minecraft:gravel', {}))]),
}


class Town:
    """Каркас города. Координаты локальные, C — центр, WR — расстояние от центра до середины грани стены."""

    def __init__(self, cid, S, style, WR, gates, sides=8, ground=4, height=80, cut=None, main=None):
        self.cid = cid
        self.S = S
        self.C = C = S // 2
        self.b = City(S, height, S, cid, style, ground=ground)
        self.st = self.b.st
        self.G = ground
        self.WR = WR
        self.gates = list(gates)
        self.main = self.gates[0] if main is None else main
        self.sides = sides
        self.cut = cut  # (ux, uz, Q): дальше Q по направлению u — море/обрыв, там не строим
        self.rnd = random.Random(cid + 'town')
        self.outer = S // 2 - 3
        m = self.main
        self.normals = [(math.cos(m + 2 * math.pi * k / sides), math.sin(m + 2 * math.pi * k / sides)) for k in range(sides)]
        Rv = WR / math.cos(math.pi / sides)
        self.pts = [(C + Rv * math.cos(m + math.pi / sides + 2 * math.pi * k / sides),
                     C + Rv * math.sin(m + math.pi / sides + 2 * math.pi * k / sides)) for k in range(sides)]
        self.avenues = []

    # ---------------- геометрия ----------------
    def pr(self, x, z, n):
        return (x - self.C) * n[0] + (z - self.C) * n[1]

    def in_walls(self, x, z, margin=0):
        for n in self.normals:
            if self.pr(x, z, n) > self.WR - margin:
                return False
        if self.cut and self.pr(x, z, self.cut[:2]) > self.cut[2] - margin:
            return False
        return True

    def land(self, x, z, margin=0):
        """Внутри круга плитки и на суше."""
        if math.hypot(x - self.C, z - self.C) > self.outer - margin:
            return False
        if self.cut and self.pr(x, z, self.cut[:2]) > self.cut[2] - margin:
            return False
        return True

    def ray(self, a):
        d = (math.cos(a), math.sin(a))
        t = 1e9
        for n in self.normals:
            k = d[0] * n[0] + d[1] * n[1]
            if k > 1e-6:
                t = min(t, self.WR / k)
        return t

    def gate_xy(self, a):
        t = self.ray(a)
        return self.C + math.cos(a) * t, self.C + math.sin(a) * t

    def polar(self, a, r):
        return int(round(self.C + math.cos(a) * r)), int(round(self.C + math.sin(a) * r))

    def _clip(self, p, q):
        if not self.cut:
            return p, q
        u = self.cut[:2]
        Q = self.cut[2] + 1
        a, c = self.pr(*p, u), self.pr(*q, u)
        if a > Q and c > Q:
            return None
        if a > Q:
            t = (Q - a) / (c - a)
            p = (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
        elif c > Q:
            t = (Q - a) / (c - a)
            q = (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
        return p, q

    # ---------------- земля ----------------
    def paint_base(self, fn=None):
        """Земля плитки (круг): трава/подзол/камень по народу. Дальше её перекрашивают улицы и постройки."""
        b, G = self.b, self.G
        fn = fn or GROUND.get(b.style, GROUND['human'])
        for x in range(self.S):
            for z in range(self.S):
                if self.land(x, z):
                    blk, props = fn(b, x, z)
                    b.set(x, G - 1, z, blk, **props)

    # ---------------- стены ----------------
    def walls(self, kind='stone', h=10, thick=3, tower_r=4, tower_h=None, roof='minecraft:dark_prismarine', gate_r=4, banners=True):
        b, G = self.b, self.G
        gates = [(gx, gz, gate_r) for gx, gz in (self.gate_xy(a) for a in self.gates)]
        tower_h = tower_h or h + 5
        ends = []
        self.cut_ends = []
        for k in range(self.sides):
            seg = self._clip(self.pts[k], self.pts[(k + 1) % self.sides])
            if not seg:
                continue
            (ax, az), (cx, cz) = seg
            for (px, pz), q in (((ax, az), self.pts[k]), ((cx, cz), self.pts[(k + 1) % self.sides])):
                if abs(px - q[0]) + abs(pz - q[1]) > 0.5:
                    self.cut_ends.append((int(round(px)), int(round(pz))))
            ax, az, cx, cz = int(round(ax)), int(round(az)), int(round(cx)), int(round(cz))
            if kind == 'stone':
                b.wall_line(ax, az, cx, cz, h=h, thick=thick, gates=gates)
            elif kind == 'palisade':
                self.palisade_line(ax, az, cx, cz, h, gates)
            elif kind == 'hedge':
                self.hedge_line(ax, az, cx, cz, h, gates)
            ends += [(ax, az), (cx, cz)]
        # башни на углах (и на концах у моря)
        seen = set()
        for (x, z) in ends:
            if (x, z) in seen:
                continue
            seen.add((x, z))
            if any(math.hypot(x - gx, z - gz) < gate_r + tower_r + 2 for gx, gz, _ in gates):
                continue
            if kind == 'stone':
                b.round_tower(x, z, tower_r, tower_h, roof_block=roof)
            elif kind == 'palisade':
                self.log_tower(x, z, tower_h)
            elif kind == 'hedge':
                self.tree_post(x, z, h + 4)
        # ворота: башни по бокам, знамёна
        for a in self.gates:
            gx, gz = self.gate_xy(a)
            # касательная к грани, которую пересёк луч
            n = max(self.normals, key=lambda n: math.cos(a) * n[0] + math.sin(a) * n[1])
            tx_, tz_ = -n[1], n[0]
            for s in (-1, 1):
                x, z = int(round(gx + tx_ * s * (gate_r + 3))), int(round(gz + tz_ * s * (gate_r + 3)))
                if kind == 'stone':
                    b.round_tower(x, z, 3, h + 4, roof_block=roof, top_light=True)
                elif kind == 'palisade':
                    self.log_tower(x, z, h + 4)
                elif kind == 'hedge':
                    self.tree_post(x, z, h + 6)
            if banners and kind == 'stone':
                face = cardinal(a)
                for s in (-2, 2):
                    x, z = int(round(gx + tx_ * s - math.cos(a) * 2)), int(round(gz + tz_ * s - math.sin(a) * 2))
                    b.banner_wall(x, G + 7, z, OPP[face])
            if kind == 'hedge':
                self.hedge_arch(gx, gz, a, gate_r)

    def palisade_line(self, ax, az, bx, bz, h, gates):
        """Частокол: заострённые брёвна, изнутри помост на столбах, черепа и факелы."""
        b, G = self.b, self.G
        n = int(max(abs(bx - ax), abs(bz - az), 1))
        cx, cz = self.C, self.C
        for s in range(n + 1):
            x = round(ax + (bx - ax) * s / n)
            z = round(az + (bz - az) * s / n)
            if any(math.hypot(x - gx, z - gz) < gr for gx, gz, gr in gates):
                continue
            hh = h + (x * 7 + z * 3) % 2
            for y in range(G - 1, G + hh):
                b.set(x, y, z, 'minecraft:spruce_log', axis='y')
            b.set(x, G + hh, z, 'minecraft:spruce_fence', waterlogged='false')
            if (x + z) % 9 == 0:
                b.set(x, G + hh + 1, z, 'minecraft:skeleton_skull', rotation=str((x * 3 + z) % 16))
            # помост изнутри: на шаг к центру
            dx, dz = cx - x, cz - z
            ln = math.hypot(dx, dz) or 1
            px, pz = int(round(x + dx / ln * 1.5)), int(round(z + dz / ln * 1.5))
            if (px, G + hh - 3, pz) not in b.blocks:
                b.slab(px, G + hh - 3, pz, 'minecraft:spruce_slab', 'top')
                if (px + pz) % 6 == 0:
                    for y in range(G, G + hh - 3):
                        b.set(px, y, pz, 'minecraft:stripped_spruce_log', axis='y')
                if (px * 5 + pz) % 13 == 0:
                    b.set(px, G + hh - 2, pz, 'minecraft:lantern', hanging='false', waterlogged='false')

    def log_tower(self, cx, cz, h):
        """Сторожевая вышка из брёвен с площадкой, крышей и лестницей."""
        b, G = self.b, self.G
        b.reserve(cx - 3, cz - 3, cx + 3, cz + 3)
        for dx in (-2, 2):
            for dz in (-2, 2):
                for y in range(G - 1, G + h + 3):
                    b.set(cx + dx, y, cz + dz, 'minecraft:spruce_log', axis='y')
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                b.set(cx + dx, G + h - 1, cz + dz, 'minecraft:spruce_planks')
                if abs(dx) == 2 or abs(dz) == 2:
                    if not (abs(dx) == 2 and abs(dz) == 2):
                        b.set(cx + dx, G + h, cz + dz, 'minecraft:spruce_fence', waterlogged='false')
                b.slab(cx + dx, G + h + 3, cz + dz, 'minecraft:spruce_slab')
        b.set(cx, G + h + 4, cz, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
        for y in range(G, G + h):
            b.set(cx - 1, y, cz - 1, 'minecraft:ladder', facing='south', waterlogged='false')
        b.set(cx - 1, G + h - 1, cz - 1, 'minecraft:ladder', facing='south', waterlogged='false')
        b.set(cx + 1, G + h, cz + 1, 'minecraft:lantern', hanging='false', waterlogged='false')
        b.set(cx, G + h + 2, cz, 'minecraft:lantern', hanging='true', waterlogged='false')

    def hedge_line(self, ax, az, bx, bz, h, gates):
        """Живая изгородь эльфов: густая листва в два ряда, берёзовые столбы с лунными фонарями."""
        b, G = self.b, self.G
        n = int(max(abs(bx - ax), abs(bz - az), 1))
        horiz = abs(bx - ax) >= abs(bz - az)
        for s in range(n + 1):
            x = round(ax + (bx - ax) * s / n)
            z = round(az + (bz - az) * s / n)
            if any(math.hypot(x - gx, z - gz) < gr for gx, gz, gr in gates):
                continue
            for t in (0, 1):
                px, pz = (x, z + t) if horiz else (x + t, z)
                for y in range(G, G + h - (1 if (px * 3 + pz) % 4 == 0 else 0)):
                    leaf = 'minecraft:flowering_azalea_leaves' if (px + y * 3 + pz * 7) % 5 == 0 else 'minecraft:azalea_leaves'
                    b.set(px, y, pz, leaf, persistent='true', distance='1', waterlogged='false')
            if s % 8 == 0:
                for y in range(G - 1, G + h + 1):
                    b.set(x, y, z, 'minecraft:birch_log', axis='y')
                b.set(x, G + h + 1, z, 'minecraft:pearlescent_froglight', axis='y')

    def tree_post(self, x, z, h):
        b, G = self.b, self.G
        for y in range(G - 1, G + h):
            b.set(x, y, z, 'minecraft:birch_log', axis='y')
        ML_blob(b, x, G + h, z, 2.6, 'minecraft:flowering_azalea_leaves')
        b.set(x, G + h - 3, z + 1, 'minecraft:pearlescent_froglight', axis='y')

    def hedge_arch(self, gx, gz, a, r):
        """Арка ворот эльфов: два ствола и свод из ветвей с цветами и фонарём."""
        b, G = self.b, self.G
        n = max(self.normals, key=lambda n: math.cos(a) * n[0] + math.sin(a) * n[1])
        tx, tz = -n[1], n[0]
        for k in range(-r, r + 1):
            x, z = int(round(gx + tx * k)), int(round(gz + tz * k))
            top = G + 6 + int(2 * math.cos(k / r * math.pi / 2))
            if abs(k) >= r - 1:
                for y in range(G - 1, top):
                    b.set(x, y, z, 'minecraft:birch_log', axis='y')
            b.set(x, top, z, 'minecraft:stripped_birch_wood', axis='y')
            b.set(x, top + 1, z, 'minecraft:flowering_azalea_leaves', persistent='true', distance='1', waterlogged='false')
            for y in range(G, top):
                if abs(k) < r - 1:
                    b.set(x, y, z, 'minecraft:air')
        b.set(int(round(gx)), G + 6, int(round(gz)), 'minecraft:pearlescent_froglight', axis='y')

    # ---------------- проспекты, подъезды, площадь ----------------
    def strip(self, a, r0, r1, half=2):
        """Клетки полосы вдоль луча a от r0 до r1 шириной 2*half+1."""
        C = self.C
        ca, sa = math.cos(a), math.sin(a)
        cells = set()
        t = r0
        while t <= r1:
            for w2 in range(-half * 2, half * 2 + 1):
                x = int(round(C + ca * t - sa * w2 / 2))
                z = int(round(C + sa * t + ca * w2 / 2))
                if 0 <= x < self.S and 0 <= z < self.S:
                    cells.add((x, z))
            t += 0.5
        return cells

    def reserve_cells(self, cells, pad=0):
        """Зарезервировать множество клеток прямоугольниками по 4×4 (чтобы b.free не тормозил)."""
        buckets = {}
        for (x, z) in cells:
            k = (x // 4, z // 4)
            x0, z0, x1, z1 = buckets.get(k, (x, z, x, z))
            buckets[k] = (min(x0, x), min(z0, z), max(x1, x), max(z1, z))
        for (x0, z0, x1, z1) in buckets.values():
            self.b.reserve(x0 - pad, z0 - pad, x1 + pad, z1 + pad)

    def pave(self, cells, blocks=None, under='minecraft:stone'):
        b, G = self.b, self.G
        blocks = blocks or self.st['street']
        for (x, z) in cells:
            b.set(x, G - 1, z, b.bp(blocks, x, 0, z))
            b.set(x, G - 2, z, under)

    def avenue(self, a, r0, half=2, blocks=None):
        """Проспект от площади (r0) до ворот."""
        r1 = self.ray(a) + 1
        cells = self.strip(a, r0, r1, half)
        self.pave(cells, blocks)
        self.reserve_cells(cells)
        self.avenues.append((a, r0, r1, half))
        return cells

    def approach(self, a, half=2, blocks=None):
        """Дорога от ворот до края плитки — там её подхватывает тракт."""
        r0 = self.ray(a) - 1
        cells = {c for c in self.strip(a, r0, self.S // 2 - 1, half) if self.land(*c, margin=-2)}
        self.pave(cells, blocks or self.st['path'], under='minecraft:dirt')
        self.reserve_cells(cells, pad=1)
        return cells

    def plaza(self, r, pattern=None, cx=None, cz=None):
        b, G = self.b, self.G
        cx = self.C if cx is None else cx
        cz = self.C if cz is None else cz
        for x in range(cx - r, cx + r + 1):
            for z in range(cz - r, cz + r + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= r + 0.4:
                    blk = pattern(x, z, d) if pattern else b.bp(self.st['street'], x, 0, z)
                    b.set(x, G - 1, z, blk)
                    b.set(x, G - 2, z, 'minecraft:stone')
        b.reserve(cx - r, cz - r, cx + r, cz + r)

    def lamps_on_avenues(self, every=9, fence=None):
        b, G = self.b, self.G
        for (a, r0, r1, half) in self.avenues:
            ca, sa = math.cos(a), math.sin(a)
            t = r0 + 4
            while t < r1 - 5:
                for s in (-1, 1):
                    x = int(round(self.C + ca * t - sa * (half + 1) * s))
                    z = int(round(self.C + sa * t + ca * (half + 1) * s))
                    if ML._air(b, x, G, z) and ML._air(b, x, G + 1, z) and b.blocks.get((x, G - 1, z), ('',))[0] != 'minecraft:air':
                        b.lamp_post(x, z, 3, fence)
                t += every

    # ---------------- место под особые здания ----------------
    def spot(self, w, d, a, r, margin=2, search=26, inner=5):
        """Свободный прямоугольник w×d как можно ближе к точке (угол a, расстояние r) внутри стен."""
        px, pz = self.polar(a, r)
        b = self.b
        for rr in range(search):
            ring = [(dx, dz) for dx in range(-rr, rr + 1) for dz in range(-rr, rr + 1) if max(abs(dx), abs(dz)) == rr]
            for dx, dz in ring:
                x0, z0 = px + dx - w // 2, pz + dz - d // 2
                x1, z1 = x0 + w - 1, z0 + d - 1
                if not all(self.in_walls(x, z, inner) for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1))):
                    continue
                if b.free(x0 - margin, z0 - margin, x1 + margin, z1 + margin):
                    return x0, z0, x1, z1
        return None

    def facing_center(self, x0, z0, x1, z1):
        dx, dz = self.C - (x0 + x1) / 2, self.C - (z0 + z1) / 2
        return ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')

    # ---------------- кварталы ----------------
    def quarters(self, crafts, citizen, merchant, terrace=2, market=(18, 13), market_crafts=(), kinds=('home', 'home', 'shop', 'library'),
                 floors=(1, 2, 2, 3), npc_chance=0.3, sizes=((6, 10), (7, 10)), stashes=6, density=1.6, inside=None, block_area=560):
        import mapbuild_crafts as MCR
        b, C = self.b, self.C
        inside = inside or (lambda x, z: self.in_walls(x, z, 4))
        org = ML.Organic(b, C, self.WR, self.cid, plaza_r=0, terrace=terrace, inside=inside, rim=3, block_area=block_area)
        mk = None
        if market:
            mw, md = market
            rnd = random.Random(self.cid + 'mk')
            best = None
            for attempt in range(600):
                a = rnd.random() * 2 * math.pi
                r = rnd.uniform(10, self.WR * 0.6)
                x0, z0 = int(C + math.cos(a) * r - mw / 2), int(C + math.sin(a) * r - md / 2)
                cells = [(x, z) for x in range(x0, x0 + mw) for z in range(z0, z0 + md)]
                if all(c in org.cell or c in org.street for c in cells) and b.free(x0, z0, x0 + mw - 1, z0 + md - 1):
                    if best is None or r < best[0]:
                        best = (r, x0, z0, cells)
                    if attempt > 200:
                        break
            if best:
                _, x0, z0, cells = best
                for c in cells:
                    org.cell.pop(c, None)
                    org.street.add(c)
                    org.flat.add(c)
                mk = (x0, z0, x0 + mw - 1, z0 + md - 1)
        org.paint_ground()
        if mk:
            ML.market(b, *mk, list(market_crafts) or ['baker', 'butcher', 'farmer', 'herbalist', 'tailor'], merchant, citizen, self.cid)
        placed, homes = MCR.fill_city(b, org, crafts, citizen, kinds=kinds, floors=floors, npc_chance=npc_chance, sizes=sizes)
        print(f'  {self.cid}: мастерских {placed}/{len(crafts)}, домов {homes}, рынок {"есть" if mk else "нет"}')
        org.street_life()
        org.courtyards(citizen, density=density, stashes=stashes)
        self.org = org
        return org

    # ---------------- окрестности ----------------
    def countryside(self, citizen, theme=None, n_fields=30, wall_pad=6, mix=None, crops=None):
        b = self.b
        theme = theme or b.style
        extra, mix0, crops0 = THEMES.get(theme, THEMES['human'])(self)
        Rv = self.WR / math.cos(math.pi / self.sides)

        def allow(x, z):
            return self.land(x, z, 2) and not self.in_walls(x, z, -wall_pad)
        return ML.outskirts(b, self.C, Rv * 0.0, self.cid, citizen, outer=self.outer - 1, allow=allow, n_fields=n_fields,
                            mix=mix or mix0, extra=extra, crops=crops or crops0)


def ML_blob(b, cx, cy, cz, r, leaves):
    R = int(r) + 1
    for x in range(cx - R, cx + R + 1):
        for y in range(cy - R // 2, cy + R // 2 + 2):
            for z in range(cz - R, cz + R + 1):
                d = math.sqrt((x - cx) ** 2 + ((y - cy) * 1.6) ** 2 + (z - cz) ** 2)
                if d <= r and ((x * 31 + y * 17 + z * 13) % 9) != 0 and (x, y, z) not in b.blocks:
                    b.set(x, y, z, leaves, persistent='true', distance='1', waterlogged='false')


# =====================================================================
#                ОКРЕСТНОСТИ: свой уклад у каждого народа
# =====================================================================

def _ground_rect(b, x0, z0, x1, z1, blk, **props):
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            b.set(x, b.G - 1, z, blk, **props)


def _fence_rect(b, x0, z0, x1, z1, fence, gate_side='south'):
    G = b.G
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if x in (x0, x1) or z in (z0, z1):
                b.set(x, G, z, fence, waterlogged='false')
    gx, gz = {'south': ((x0 + x1) // 2, z1), 'north': ((x0 + x1) // 2, z0), 'west': (x0, (z0 + z1) // 2), 'east': (x1, (z0 + z1) // 2)}[gate_side]
    gate = fence.replace('_fence', '_fence_gate') if fence.endswith('_fence') and 'nether' not in fence else 'minecraft:spruce_fence_gate'
    b.set(gx, G, gz, gate, facing='north' if gate_side in ('north', 'south') else 'east', in_wall='false', open='false', powered='false')


def _animals(b, rnd, x0, z0, x1, z1, kinds, n):
    for i in range(n):
        b.entities.append((rnd.randint(x0 + 1, x1 - 1), b.G, rnd.randint(z0 + 1, z1 - 1), rnd.choice(kinds), {}))


def _windmill(t):
    def size(rnd, attempt):
        return (9, 10)

    def build(x0, z0, x1, z1):
        import mapbuild_pois as MP
        b = t.b
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:grass_block', snowy='false')
        MP.windmill(b, x0 + 4, z0 + 4, h=12)
        for x in range(x0, x1 + 1):
            b.set(x, b.G - 1, z1, b.bp(b.st['path'], x, 0, z1))
        b.set(x0 + 1, b.G, z0 + 7, 'minecraft:hay_block', axis='y')
        b.set(x0 + 7, b.G, z0 + 7, 'minecraft:barrel', facing='up', open='false')
    return size, build


def _vineyard(t):
    def size(rnd, attempt):
        return (rnd.randint(12, 18) - attempt * 2, rnd.randint(9, 13) - attempt)

    def build(x0, z0, x1, z1):
        b = t.b
        G = b.G
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                if (z - z0) % 3 == 1:
                    b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                    if (x - x0) % 4 == 0:
                        b.set(x, G, z, 'minecraft:spruce_fence', waterlogged='false')
                        b.set(x, G + 1, z, 'minecraft:spruce_fence', waterlogged='false')
                    else:
                        b.set(x, G, z, 'minecraft:sweet_berry_bush', age='3')
                    b.set(x, G + 2, z, 'minecraft:oak_leaves' if (x + z) % 3 else 'minecraft:flowering_azalea_leaves',
                          persistent='true', distance='1', waterlogged='false')
                else:
                    b.set(x, G - 1, z, 'minecraft:coarse_dirt' if (x * 5 + z) % 4 else 'minecraft:rooted_dirt')
        b.set(x0, G, z1 + 1, 'minecraft:barrel', facing='up', open='false')
        b.set(x0 + 1, G, z1 + 1, 'minecraft:barrel', facing='up', open='false')
    return size, build


def _pond(t):
    def size(rnd, attempt):
        return (rnd.randint(8, 12), rnd.randint(7, 10))

    def build(x0, z0, x1, z1):
        b = t.b
        G = b.G
        cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
        rx, rz = (x1 - x0) / 2, (z1 - z0) / 2
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                d = ((x - cx) / rx) ** 2 + ((z - cz) / rz) ** 2
                if d < 0.55:
                    b.set(x, G - 3, z, 'minecraft:mud')
                    b.set(x, G - 2, z, 'minecraft:water', level='0')
                    b.set(x, G - 1, z, 'minecraft:water', level='0')
                    if (x * 3 + z * 5) % 7 == 0:
                        b.set(x, G, z, 'minecraft:lily_pad')
                elif d < 0.95:
                    b.set(x, G - 1, z, 'minecraft:mud' if (x + z) % 3 == 0 else 'minecraft:grass_block', **({} if (x + z) % 3 == 0 else {'snowy': 'false'}))
                    if (x * 7 + z) % 5 == 0:
                        b.set(x, G, z, 'minecraft:sugar_cane') if b.blocks.get((x, G - 1, z), ('',))[0] == 'minecraft:grass_block' and \
                            any(((x + dx - cx) / rx) ** 2 + ((z + dz - cz) / rz) ** 2 < 0.55 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))) \
                            else b.set(x, G, z, 'minecraft:fern' if (x + z) % 2 else 'minecraft:grass')
        b.set(x0, G, z0, 'minecraft:spruce_fence', waterlogged='false')
    return size, build


def _beehives(t):
    def size(rnd, attempt):
        return (7, 6)

    def build(x0, z0, x1, z1):
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                if (x + z) % 2 == 0 and (x, z) != (x0 + 3, z0 + 2):
                    b.set(x, G, z, 'minecraft:' + t.rnd.choice(['cornflower', 'allium', 'oxeye_daisy', 'poppy', 'azure_bluet', 'lilac']),
                          **({'half': 'lower'} if False else {}))
        for x in (x0 + 1, x0 + 3, x0 + 5):
            b.set(x, G, z0 + 2, 'minecraft:spruce_fence', waterlogged='false')
            b.set(x, G + 1, z0 + 2, 'minecraft:beehive', facing='south', honey_level=str(t.rnd.randint(2, 5)))
        b.set(x0 + 6, G, z1, 'minecraft:barrel', facing='up', open='false')
    return size, build


def _human_theme(t):
    extra = {'windmill': _windmill(t), 'vineyard': _vineyard(t), 'pond': _pond(t), 'bees': _beehives(t)}
    mix = {'pasture': 3, 'orchard': 2, 'house': 3, 'melons': 1, 'field': 3, 'windmill': 1, 'vineyard': 1, 'pond': 1, 'bees': 1}
    return extra, mix, None


def _elf_theme(t):
    def meadow_size(rnd, attempt):
        return (rnd.randint(10, 16) - attempt * 2, rnd.randint(9, 14) - attempt * 2)

    def meadow(x0, z0, x1, z1):
        b = t.b
        G = b.G
        rnd = t.rnd
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                r = rnd.random()
                if r < 0.45:
                    b.set(x, G, z, 'minecraft:' + rnd.choice(['lily_of_the_valley', 'allium', 'blue_orchid', 'azure_bluet', 'oxeye_daisy',
                                                              'cornflower', 'pink_tulip', 'white_tulip']))
                elif r < 0.55:
                    b.set(x, G, z, 'minecraft:grass')
        b.tree((x0 + x1) // 2, (z0 + z1) // 2, 6, 'minecraft:birch_log', 'minecraft:flowering_azalea_leaves', 2.6)
        b.set(x0 + 1, G, z0 + 1, 'minecraft:beehive', facing='south', honey_level='4')

    def grove_size(rnd, attempt):
        return (13, 13)

    def grove(x0, z0, x1, z1):
        """Лунная поляна: кольцо цветущих деревьев вокруг родника с перламутровым светом."""
        b = t.b
        G = b.G
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:moss_block' if math.hypot(x - cx, z - cz) < 3.5 else 'minecraft:grass_block',
                      **({} if math.hypot(x - cx, z - cz) < 3.5 else {'snowy': 'false'}))
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                b.set(x, G - 2, z, 'minecraft:pearlescent_froglight', axis='y')
                b.set(x, G - 1, z, 'minecraft:water', level='0')
        for k in range(6):
            a = 2 * math.pi * k / 6
            b.tree(cx + round(math.cos(a) * 5), cz + round(math.sin(a) * 5), 5, 'minecraft:birch_log',
                   'minecraft:flowering_azalea_leaves' if k % 2 else 'minecraft:azalea_leaves', 2.2)
        b.set(cx + 2, G, cz, 'minecraft:white_tulip')
        b.set(cx - 2, G, cz, 'minecraft:lily_of_the_valley')

    def range_size(rnd, attempt):
        return (16, 7)

    def archery(x0, z0, x1, z1):
        b = t.b
        G = b.G
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:grass_block', snowy='false')
        for z in range(z0 + 1, z1, 2):
            b.set(x1, G, z, 'minecraft:hay_block', axis='y')
            b.set(x1, G + 1, z, 'minecraft:target', power='0')
        for z in range(z0, z1 + 1):
            b.set(x0 + 1, G, z, 'minecraft:birch_fence', waterlogged='false')
        b.set(x0, G, z0, 'minecraft:fletching_table')
        b.entities.append((x0 + 3, G, (z0 + z1) // 2, 'litrpg:elf_citizen', {}))

    extra = {'meadow': (meadow_size, meadow), 'grove': (grove_size, grove), 'archery': (range_size, archery), 'vineyard': _vineyard(t),
             'pond': _pond(t), 'bees': _beehives(t)}
    mix = {'orchard': 3, 'meadow': 3, 'grove': 1, 'archery': 1, 'vineyard': 2, 'house': 2, 'pond': 1, 'bees': 1, 'field': 1}
    return extra, mix, ['minecraft:wheat', 'minecraft:carrots', 'minecraft:beetroots']


def _dwarf_theme(t):
    def quarry_size(rnd, attempt):
        return (rnd.randint(11, 15) - attempt, rnd.randint(10, 13) - attempt)

    def quarry(x0, z0, x1, z1):
        """Каменоломня: ступенчатая выработка, блоки на поддонах, кран-журавль, вагонетка."""
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                step = min(x - x0, x1 - x, z - z0, z1 - z)
                depth = min(4, step)
                for y in range(G - 1 - depth, G):
                    b.set(x, y, z, 'minecraft:air' if y > G - 1 - depth else b.bp(['minecraft:stone', 'minecraft:andesite',
                                                                                     'minecraft:cobblestone', 'minecraft:stone'], x, y, z))
                b.set(x, G - 1 - depth, z, b.bp(['minecraft:stone', 'minecraft:andesite', 'minecraft:gravel', 'minecraft:stone'], x, 0, z))
                for y in range(G - 6, G - 1 - depth):
                    b.set(x, y, z, 'minecraft:stone')
        for x in range(x0, x1 + 1, 3):
            b.set(x, G, z0 - 1, 'minecraft:spruce_fence', waterlogged='false')
        cx = x1 + 1
        for y in range(G, G + 6):
            b.set(cx, y, z0 + 2, 'minecraft:spruce_log', axis='y')
        for k in range(1, 4):
            b.set(cx - k, G + 5, z0 + 2, 'minecraft:spruce_fence', waterlogged='false')
        for y in range(G + 2, G + 5):
            b.set(cx - 3, y, z0 + 2, 'minecraft:chain', axis='y', waterlogged='false')
        b.set(cx - 3, G + 1, z0 + 2, 'minecraft:stone')
        for z in range(z0 + 4, z0 + 8):
            b.set(cx, G, z, 'minecraft:rail', shape='north_south', waterlogged='false')
        b.entities.append((cx, G, z0 + 5, 'minecraft:minecart', {}))
        for z in (z1 - 1, z1 - 3):
            b.set(cx, G, z, 'minecraft:stone_bricks')
            b.set(cx, G + 1, z, 'minecraft:stone_brick_slab', type='bottom', waterlogged='false')
        b.entities.append(((x0 + x1) // 2, G - 4, (z0 + z1) // 2, 'litrpg:dwarf_citizen', {}))
        b.reserve(x0 - 1, z0 - 1, x1 + 2, z1 + 1)

    def shrooms_size(rnd, attempt):
        return (rnd.randint(9, 12), rnd.randint(8, 10))

    def shrooms(x0, z0, x1, z1):
        """Грибная ферма: тёмный навес над грядками мицелия и подзола, большой гриб, бочки."""
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                b.set(x, G - 1, z, 'minecraft:mycelium' if (x + z) % 2 else 'minecraft:podzol')
                if edge and (x - x0) % 3 == 0 and (z - z0) % 3 == 0 or edge and x in (x0, x1) and z in (z0, z1):
                    for y in range(G, G + 3):
                        b.set(x, y, z, 'minecraft:dark_oak_log', axis='y')
                elif not edge and (x + z) % 2:
                    b.set(x, G, z, 'minecraft:brown_mushroom' if (x * 3 + z) % 3 else 'minecraft:red_mushroom')
                b.set(x, G + 3, z, 'minecraft:dark_oak_slab', type='bottom', waterlogged='false')
        b.set(x0 + 1, G, z1 - 1, 'minecraft:barrel', facing='up', open='false')
        b.set(x0 + 2, G + 2, (z0 + z1) // 2, 'minecraft:lantern', hanging='true', waterlogged='false')

    def goats_size(rnd, attempt):
        return (rnd.randint(12, 16) - attempt, rnd.randint(10, 13) - attempt)

    def goats(x0, z0, x1, z1):
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, b.bp(['minecraft:grass_block', 'minecraft:coarse_dirt', 'minecraft:grass_block', 'minecraft:stone'],
                                       x // 2, 0, z // 2))
        # скалы для коз
        for k in range(3):
            sx, sz = t.rnd.randint(x0 + 2, x1 - 3), t.rnd.randint(z0 + 2, z1 - 3)
            for dx in range(2):
                for dz in range(2):
                    b.set(sx + dx, G, sz + dz, 'minecraft:mossy_cobblestone' if dx else 'minecraft:stone')
            b.set(sx, G + 1, sz, 'minecraft:cobblestone')
        _fence_rect(b, x0, z0, x1, z1, 'minecraft:dark_oak_fence')
        _animals(b, t.rnd, x0 + 1, z0 + 1, x1 - 1, z1 - 1, ['minecraft:goat'], 4)

    def mine_size(rnd, attempt):
        return (11, 11)

    def minehead(x0, z0, x1, z1):
        """Устье шахты: копёр над стволом, ступени вниз в штрек с рудой, рельсы и вагонетки."""
        b = t.b
        G = b.G
        cx, cz = x0 + 5, z0 + 5
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:gravel')
        # ствол 3×3 с лестницей
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                for y in range(G - 14, G):
                    b.set(x, y, z, 'minecraft:air')
        for y in range(G - 14, G):
            b.set(cx, y, cz - 1, 'minecraft:ladder', facing='south', waterlogged='false')
            for x in range(cx - 2, cx + 3):
                for z in (cz - 2, cz + 2):
                    b.set(x, y, z, 'minecraft:stone')
            for z in range(cz - 2, cz + 3):
                for x in (cx - 2, cx + 2):
                    b.set(x, y, z, 'minecraft:stone')
        for x in range(cx - 1, cx + 2):
            for z in range(cz - 1, cz + 2):
                b.set(x, G - 15, z, 'minecraft:cobblestone')
        # штрек с рудой внизу
        for x in range(cx + 2, cx + 9):
            for y in range(G - 14, G - 11):
                for z in range(cz - 1, cz + 2):
                    b.set(x, y, z, 'minecraft:air')
            b.set(x, G - 15, cz, 'minecraft:cobblestone')
            b.set(x, G - 14, cz, 'minecraft:rail', shape='east_west', waterlogged='false')
            for z in (cz - 2, cz + 2):
                for y in range(G - 15, G - 10):
                    ore = (x * 7 + y * 3 + z) % 9
                    b.set(x, y, z, 'minecraft:iron_ore' if ore == 0 else 'minecraft:coal_ore' if ore == 1 else 'minecraft:copper_ore' if ore == 2
                          else 'minecraft:stone')
            for z in range(cz - 2, cz + 3):
                b.set(x, G - 11, z, 'minecraft:stone')
            if (x - cx) % 3 == 0:
                for y in range(G - 14, G - 11):
                    b.set(x, y, cz - 1, 'minecraft:spruce_log', axis='y')
                    b.set(x, y, cz + 1, 'minecraft:spruce_log', axis='y')
                b.set(x, G - 12, cz, 'minecraft:lantern', hanging='true', waterlogged='false')
        for z in range(cz - 2, cz + 3):
            for y in range(G - 15, G - 10):
                b.set(cx + 9, y, z, 'minecraft:stone' if (y + z) % 5 else 'minecraft:gold_ore')
        b.entities.append((cx + 6, G - 14, cz, 'minecraft:minecart', {}))
        # копёр
        for dx in (-2, 2):
            for dz in (-2, 2):
                for y in range(G, G + 7):
                    b.set(cx + dx, y, cz + dz, 'minecraft:spruce_log', axis='y')
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if abs(x - cx) == 2 or abs(z - cz) == 2:
                    b.set(x, G + 7, z, 'minecraft:spruce_planks')
        b.set(cx, G + 8, cz, 'minecraft:spruce_planks')
        for y in range(G + 1, G + 7):
            b.set(cx, y, cz, 'minecraft:chain', axis='y', waterlogged='false')
        b.set(cx, G + 7, cz, 'minecraft:spruce_planks')
        for x in range(cx - 1, cx + 2):
            b.set(x, G, cz + 2, 'minecraft:dark_oak_fence', waterlogged='false') if x != cx else None
        # отвал руды и вагонетка наверху
        for z in range(cz - 1, cz + 2):
            b.set(x0 + 9, G, z, 'minecraft:rail', shape='north_south', waterlogged='false')
        b.entities.append((x0 + 9, G, cz, 'minecraft:minecart', {}))
        b.set(x0 + 1, G, z0 + 1, 'minecraft:coal_block')
        b.set(x0 + 2, G, z0 + 1, 'minecraft:raw_iron_block')
        b.set(x0 + 1, G, z0 + 2, 'minecraft:cobblestone')
        b.set(x0 + 1, G + 1, z0 + 1, 'minecraft:cobblestone_slab', type='bottom', waterlogged='false')
        b.lantern(x0 + 1, G, z1 - 1)
        b.entities.append((x0 + 2, G, z1 - 2, 'litrpg:dwarf_citizen', {}))

    extra = {'quarry': (quarry_size, quarry), 'shrooms': (shrooms_size, shrooms), 'goats': (goats_size, goats),
             'mine': (mine_size, minehead), 'bees': _beehives(t)}
    mix = {'quarry': 2, 'shrooms': 2, 'goats': 2, 'mine': 2, 'house': 2, 'field': 2, 'pasture': 1}
    return extra, mix, ['minecraft:potatoes', 'minecraft:potatoes', 'minecraft:carrots', 'minecraft:wheat', 'minecraft:beetroots']


def _orc_theme(t):
    def pen_size(rnd, attempt):
        return (rnd.randint(10, 14) - attempt, rnd.randint(8, 11) - attempt)

    def pigs(x0, z0, x1, z1):
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:mud' if (x * 3 + z) % 4 else 'minecraft:coarse_dirt')
        b.set(x0 + 2, G, z0 + 2, 'minecraft:composter', level='5')
        b.set(x0 + 3, G, z0 + 2, 'minecraft:hay_block', axis='y')
        _fence_rect(b, x0, z0, x1, z1, 'minecraft:spruce_fence')
        _animals(b, t.rnd, x0 + 1, z0 + 1, x1 - 1, z1 - 1, ['minecraft:pig', 'minecraft:pig', 'minecraft:hoglin'] if False else ['minecraft:pig'], 5)

    def camp_size(rnd, attempt):
        return (15, 13)

    def warcamp(x0, z0, x1, z1):
        """Военный лагерь: шатры, костёр, стойки с оружием, мишени-чучела."""
        from mapbuild_cities import tent, bonfire
        b = t.b
        G = b.G
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        bonfire(b, cx, cz)
        tent(b, x0 + 3, z0 + 3, 2, 'red', npc='litrpg:orc_citizen', door='south')
        tent(b, x1 - 3, z0 + 3, 2, 'brown', npc=None, door='south')
        tent(b, x0 + 3, z1 - 3, 2, 'black', npc=None, door='north')
        for k in range(3):
            x = x1 - 1 - k * 2
            b.set(x, G, z1 - 1, 'minecraft:spruce_fence', waterlogged='false')
            b.set(x, G + 1, z1 - 1, 'minecraft:hay_block', axis='y')
            b.set(x, G + 2, z1 - 1, 'minecraft:carved_pumpkin', facing='north')
        b.entities.append((cx + 2, G, cz, 'minecraft:armor_stand', {'ShowArms': TByte(1)}))

    def bones_size(rnd, attempt):
        return (7, 7)

    def bones(x0, z0, x1, z1):
        b = t.b
        G = b.G
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
        for dx, dz in ((0, 0), (1, 0), (0, 1), (-1, 0), (0, -1)):
            b.set(cx + dx, G, cz + dz, 'minecraft:bone_block', axis='y' if dx == 0 else 'x')
        b.set(cx, G + 1, cz, 'minecraft:bone_block', axis='y')
        b.set(cx, G + 2, cz, 'minecraft:skeleton_skull', rotation='8')
        for (x, z) in ((x0, z0), (x1, z0), (x0, z1), (x1, z1)):
            for y in range(G, G + 3):
                b.set(x, y, z, 'minecraft:spruce_fence', waterlogged='false')
            b.set(x, G + 3, z, 'minecraft:skeleton_skull', rotation=str((x + z) % 16))

    def kennel_size(rnd, attempt):
        return (10, 8)

    def kennel(x0, z0, x1, z1):
        b = t.b
        G = b.G
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
        for x in range(x0 + 1, x0 + 4):
            for z in range(z0 + 1, z0 + 3):
                b.set(x, G + 2, z, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
        for (x, z) in ((x0 + 1, z0 + 1), (x0 + 3, z0 + 1)):
            b.set(x, G, z, 'minecraft:spruce_log', axis='y')
            b.set(x, G + 1, z, 'minecraft:spruce_log', axis='y')
        b.set(x0 + 2, G, z0 + 2, 'minecraft:bone_block', axis='x')
        _fence_rect(b, x0, z0, x1, z1, 'minecraft:spruce_fence')
        _animals(b, t.rnd, x0 + 1, z0 + 3, x1 - 1, z1 - 1, ['minecraft:wolf'], 3)

    extra = {'pigs': (pen_size, pigs), 'warcamp': (camp_size, warcamp), 'bones': (bones_size, bones), 'kennel': (kennel_size, kennel)}
    mix = {'pigs': 3, 'warcamp': 2, 'bones': 1, 'kennel': 1, 'pasture': 1, 'field': 2, 'house': 1}
    return extra, mix, ['minecraft:beetroots', 'minecraft:wheat', 'minecraft:potatoes']


def _beast_theme(t):
    def racks_size(rnd, attempt):
        return (9, 7)

    def racks(x0, z0, x1, z1):
        """Сушильни: рамы из жердей с распятыми шкурами, коптильня."""
        b = t.b
        G = b.G
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:coarse_dirt')
        for k, x in enumerate(range(x0 + 1, x1, 3)):
            for y in range(G, G + 3):
                b.set(x, y, z0 + 1, 'minecraft:spruce_fence', waterlogged='false')
                b.set(x, y, z0 + 4, 'minecraft:spruce_fence', waterlogged='false')
            for z in range(z0 + 2, z0 + 4):
                b.set(x, G + 2, z, 'minecraft:spruce_fence', waterlogged='false')
                b.set(x, G + 1, z, 'minecraft:' + ['brown', 'white', 'light_gray'][k % 3] + '_wool')
        b.set(x1 - 1, G, z1 - 1, 'minecraft:smoker', facing='north', lit='true')
        b.set(x1 - 2, G, z1 - 1, 'minecraft:barrel', facing='up', open='false')
        b.set(x0 + 1, G, z1 - 1, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')

    def tents_size(rnd, attempt):
        return (14, 12)

    def tents(x0, z0, x1, z1):
        from mapbuild_cities import tent, bonfire
        b = t.b
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:podzol')
        bonfire(b, (x0 + x1) // 2, (z0 + z1) // 2)
        tent(b, x0 + 3, z0 + 3, 2, 'white', npc='litrpg:beast_citizen', door='south')
        tent(b, x1 - 3, z1 - 3, 2, 'brown', npc=None, door='north')

    def berries_size(rnd, attempt):
        return (rnd.randint(8, 12), rnd.randint(6, 9))

    def berries(x0, z0, x1, z1):
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                if (x + 2 * z) % 3:
                    b.set(x, G, z, 'minecraft:sweet_berry_bush', age=str(2 + (x + z) % 2))
        b.set(x0, G, z0, 'minecraft:spruce_fence', waterlogged='false')

    def sheep_size(rnd, attempt):
        return (rnd.randint(12, 16) - attempt, rnd.randint(10, 13) - attempt)

    def sheep(x0, z0, x1, z1):
        b = t.b
        _ground_rect(b, x0, z0, x1, z1, 'minecraft:grass_block', snowy='false')
        _fence_rect(b, x0, z0, x1, z1, 'minecraft:spruce_fence')
        b.set(x0 + 2, b.G, z0 + 2, 'minecraft:hay_block', axis='y')
        _animals(b, t.rnd, x0 + 1, z0 + 1, x1 - 1, z1 - 1, ['minecraft:sheep', 'minecraft:sheep', 'minecraft:goat'], 5)

    extra = {'racks': (racks_size, racks), 'tents': (tents_size, tents), 'berries': (berries_size, berries), 'sheep': (sheep_size, sheep)}
    mix = {'racks': 2, 'tents': 2, 'berries': 2, 'sheep': 3, 'field': 1, 'house': 2}
    return extra, mix, ['minecraft:potatoes', 'minecraft:beetroots', 'minecraft:carrots']


def _demon_theme(t):
    def spire_size(rnd, attempt):
        return (7, 7)

    def spire(x0, z0, x1, z1):
        """Шпиль из базальта и обсидиана с огнём душ — им демониды отмечают межи."""
        b = t.b
        G = b.G
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        h = 9 + t.rnd.randint(0, 5)
        for y in range(G - 1, G + h):
            r = 2.4 * (1 - (y - G) / (h + 2))
            for dx in range(-3, 4):
                for dz in range(-3, 4):
                    if math.hypot(dx, dz) <= r:
                        b.set(cx + dx, y, cz + dz, 'minecraft:obsidian' if (dx * dz + y) % 5 == 0 else 'minecraft:polished_basalt',
                              **({} if (dx * dz + y) % 5 == 0 else {'axis': 'y'}))
        b.set(cx, G + h, cz, 'minecraft:soul_soil')
        b.set(cx, G + h + 1, cz, 'minecraft:soul_fire')
        for dx, dz in ((3, 0), (-3, 0), (0, 3), (0, -3)):
            b.set(cx + dx, G, cz + dz, 'minecraft:soul_lantern', hanging='false', waterlogged='false')

    def fungus_size(rnd, attempt):
        return (rnd.randint(10, 14), rnd.randint(9, 12))

    def fungus(x0, z0, x1, z1):
        """Роща багровых и искажённых грибов на нилии."""
        b = t.b
        G = b.G
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                warped = (x // 5 + z // 5) % 2
                b.set(x, G - 1, z, 'minecraft:warped_nylium' if warped else 'minecraft:crimson_nylium')
                if (x * 7 + z * 3) % 6 == 0:
                    b.set(x, G, z, 'minecraft:warped_roots' if warped else 'minecraft:crimson_roots')
        for k in range(3):
            x, z = t.rnd.randint(x0 + 2, x1 - 2), t.rnd.randint(z0 + 2, z1 - 2)
            warped = (x // 5 + z // 5) % 2
            b.tree(x, z, t.rnd.randint(5, 7), 'minecraft:warped_stem' if warped else 'minecraft:crimson_stem',
                   'minecraft:warped_wart_block' if warped else 'minecraft:nether_wart_block', 2.4)
            b.set(x, G + 3, z + 1, 'minecraft:shroomlight')

    def magma_size(rnd, attempt):
        return (9, 9)

    def magma(x0, z0, x1, z1):
        """Магмовая купель: кольцо из чернокамня, в середине — магма и котлы с лавой за решёткой."""
        b = t.b
        G = b.G
        cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                d = math.hypot(x - cx, z - cz)
                b.set(x, G - 1, z, 'minecraft:magma_block' if d < 2.5 else 'minecraft:polished_blackstone' if d < 3.6 else 'minecraft:blackstone')
                if 2.5 <= d < 3.6:
                    b.set(x, G, z, 'minecraft:polished_blackstone_wall')
        b.set(cx, G, cz, 'minecraft:lava_cauldron')
        b.set(cx, G + 1, cz, 'minecraft:iron_bars', waterlogged='false')

    extra = {'spire': (spire_size, spire), 'fungus': (fungus_size, fungus), 'magma': (magma_size, magma)}
    mix = {'field': 3, 'spire': 1, 'fungus': 2, 'magma': 1, 'house': 2}
    return extra, mix, ['minecraft:nether_wart']


THEMES = {'human': _human_theme, 'elf': _elf_theme, 'dwarf': _dwarf_theme, 'orc': _orc_theme, 'beast': _beast_theme, 'demon': _demon_theme}
