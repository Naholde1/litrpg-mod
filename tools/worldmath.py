"""
Математика «Мира Системы». ТОЧНО та же логика реализована в Java
(src/main/java/com/andrey/litrpg/world/map/WorldMap.java) — при изменении
формул правьте оба файла. Здесь она нужна для планирования карты
(куда ставить города и поселения) и для рисования карты-свитка.
"""
import math


# ---------------- шум (целочисленный хэш, совпадает с Java) ----------------

def h32(ix, iz, seed):
    n = (ix * 374761393 + iz * 668265263 + seed * 1442695041) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n = n ^ (n >> 16)
    return (n & 0xFFFF) / 65535.0


def smooth(t):
    return t * t * (3 - 2 * t)


def vn(x, z, cell, seed):
    gx, gz = x / cell, z / cell
    x0, z0 = math.floor(gx), math.floor(gz)
    fx, fz = smooth(gx - x0), smooth(gz - z0)
    a, b = h32(x0, z0, seed), h32(x0 + 1, z0, seed)
    c, d = h32(x0, z0 + 1, seed), h32(x0 + 1, z0 + 1, seed)
    return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz


def fbm(x, z, cell, seed):
    return (vn(x, z, cell, seed) * 0.5 + vn(x, z, cell / 2.3, seed + 1) * 0.25 + vn(x, z, cell / 5.5, seed + 2) * 0.15
            + vn(x, z, cell / 13.0, seed + 3) * 0.1)


def sstep(e0, e1, x):
    if e0 == e1:
        return 1.0 if x >= e1 else 0.0
    t = max(0.0, min(1.0, (x - e0) / (e1 - e0)))
    return t * t * (3 - 2 * t)


def lerp(a, b, t):
    return a + (b - a) * t


def seg_dist(px, pz, ax, az, bx, bz):
    dx, dz = bx - ax, bz - az
    l2 = dx * dx + dz * dz
    t = 0.0 if l2 == 0 else max(0.0, min(1.0, ((px - ax) * dx + (pz - az) * dz) / l2))
    cx, cz = ax + dx * t, az + dz * t
    return math.hypot(px - cx, pz - cz)


def poly_dist(px, pz, pts):
    best = 1e18
    for i in range(len(pts) - 1):
        d = seg_dist(px, pz, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1])
        if d < best:
            best = d
    return best


# ---------------- карта ----------------

def city_level(c, dist, lv):
    """Уровень от города: за стенами — пояс безопасности, дальше рост, после far — вдвое быстрее.
    (В WorldMap.java у дорог и поселений рост ещё и замедляется — здесь только основа.)"""
    d = max(0.0, dist - c.get('r', 0) - lv.get('safe', 0))
    return c['level'] + d / lv['slope'] + max(0.0, d - lv.get('far', 1e9)) / lv.get('far_slope', 1e9)


class World:
    """Загружается из того же JSON, что читает Java."""

    def __init__(self, data):
        self.d = data
        self.terr = data['territories']
        self.wild = data['wild']
        self.wall0, self.wall1 = data['wall']

    def warp(self, x, z):
        wx = x + 240 * (vn(x, z, 1000, 11) * 2 - 1) + 60 * (vn(x, z, 180, 13) * 2 - 1)
        wz = z + 240 * (vn(x, z, 1000, 12) * 2 - 1) + 60 * (vn(x, z, 180, 14) * 2 - 1)
        return wx, wz

    def scores(self, x, z):
        wx, wz = self.warp(x, z)
        out = []
        for t in self.terr:
            s = 1e9
            for sx, sz, r in t['seeds']:
                v = math.hypot(wx - sx, wz - sz) / r
                if v < s:
                    s = v
            out.append(s)
        return out

    def territory(self, x, z):
        """Индекс территории или -1 (дикие земли)."""
        sc = self.scores(x, z)
        best = min(range(len(sc)), key=lambda i: sc[i])
        return best if sc[best] < 1.0 else -1

    @staticmethod
    def prof_height(p, x, z, seed):
        h = p['base'] + p['amp'] * (fbm(x, z, p['cell'], seed) * 2 - 1)
        if p['ridge'] > 0:
            r = 1 - abs(vn(x, z, p['cell'] * 0.8, seed + 5) * 2 - 1)
            r2 = 1 - abs(vn(x, z, p['cell'] * 0.35, seed + 6) * 2 - 1)
            h += p['ridge'] * (r * r * r * 110 + r2 * r2 * 34)
        return h

    def base(self, x, z):
        """Высота и «вес управления» до воды и площадок. -> (H, W, territory)"""
        sc = self.scores(x, z)
        ws = [math.exp(-7 * s) for s in sc]
        ww = math.exp(-7 * 1.0)
        tot = sum(ws) + ww
        H = W = 0.0
        for i, t in enumerate(self.terr):
            w = ws[i] / tot
            if w < 0.002:
                continue
            H += w * self.prof_height(t['profile'], x, z, 100 + i * 17)
            W += w * t['profile']['tame']
        w = ww / tot
        H += w * self.prof_height(self.wild['profile'], x, z, 977)
        W += w * self.wild['profile']['tame']
        # нормировка пропущенных малых весов
        best = min(range(len(sc)), key=lambda i: sc[i])
        return H, W, (best if sc[best] < 1.0 else -1)

    def sample(self, x, z):
        """-> (H, W, territory, water) где water: 0 нет, 1 река, 2 озеро, 3 море."""
        H, W, t = self.base(x, z)
        water = 0
        # моря и озёра
        for kind, lst in ((3, self.d['seas']), (2, self.d['lakes'])):
            for lx, lz, r, floor in lst:
                d = math.hypot(x - lx, z - lz) / r
                d *= 1 + 0.18 * (vn(x, z, r * 0.6, 31) * 2 - 1)
                if d < 1.35:
                    k = sstep(1.35, 0.9, d)
                    target = floor + (64.5 - floor) * min(1.0, d) ** 2
                    H = lerp(H, target, k)
                    W = max(W, 0.92 * k)
                    if d < 1.0:
                        water = kind
        # реки
        for rv in self.d['rivers']:
            pts, width = rv['points'], rv['width']
            # быстрая отсечка по рамке
            if x < rv['box'][0] - 80 or x > rv['box'][2] + 80 or z < rv['box'][1] - 80 or z > rv['box'][3] + 80:
                continue
            dr = poly_dist(x, z, pts)
            half = width / 2
            if dr < half + 40:
                core = sstep(half + 40, half, dr)
                t2 = 55 + 8 * (dr / half) ** 2 if dr < half else 63 + (dr - half) * 0.35
                H = lerp(H, min(H, t2), core)
                W = max(W, 0.92 * core)
                if dr < half and water == 0:
                    water = 1
        # площадки городов и поселений
        for f in self.d['flats']:
            fx, fz, r, y = f[:4]
            bl = f[4] if len(f) > 4 else 48
            dd = math.hypot(x - fx, z - fz)
            if dd < r + bl:
                k = sstep(r + bl, r, dd)
                if len(f) > 7:
                    # срез полуплоскостью: площадка только там, где (p - центр)·n < d (гавань, берег)
                    k *= sstep(f[7] + 5, f[7], (x - fx) * f[5] + (z - fz) * f[6])
                if k <= 0:
                    continue
                H = lerp(H, y, k)
                W = lerp(W, 1.0, k)
                if dd < r and k > 0.99 and y >= 63:
                    water = 0
        # край мира
        c = max(abs(x), abs(z))
        if c > self.wall0:
            k = sstep(self.wall0, self.wall1, c)
            rr = 1 - abs(vn(x, z, 160, 78) * 2 - 1)
            wallH = 205 + 55 * vn(x, z, 140, 77) + 40 * rr * rr
            H = lerp(H, wallH, k)
            W = lerp(W, 0.96, k)
            if k > 0.3:
                water = 0
        return H, W, t, water

    def city_at(self, x, z):
        for c in self.d['cities']:
            if math.hypot(x - c['x'], z - c['z']) < c['r']:
                return c
        return None

    def level(self, x, z, t=None):
        lv = self.d['levels']
        best = 1e9
        for c in self.d['cities']:
            v = city_level(c, math.hypot(x - c['x'], z - c['z']), lv)
            if v < best:
                best = v
        if t is None:
            t = self.territory(x, z)
        if t >= 0:
            best += self.terr[t].get('level_bonus', 0)
        c = max(abs(x), abs(z))
        if c > self.wall0:
            best += 20 * sstep(self.wall0, self.wall1, c)
        return int(max(1, min(lv['max'], round(best))))
