"""
Строитель карты подземелья: «вырезаем» пустоты (залы, коридоры, пещеры, лестницы) в воображаемой толще,
а стены, пол и потолок вокруг пустот достраиваются сами — материалом темы того места.
Потом расставляем отделку, механизмы и схему этапа (отряды, хранители, преграды, загадки).

Координаты абсолютные (мир litrpg:dungeon). Сохранение режет карту на плитки 32×32 (по сетке чанков).
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
from gen_structures import TByte, TInt, TDouble, TList, write_nbt  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', '..', 'src', 'main', 'resources')
STRUCT = os.path.join(ROOT, 'data', 'litrpg', 'structures', 'dungeon')

AIR = 'minecraft:air'
FACE_OPP = {'north': 'south', 'south': 'north', 'east': 'west', 'west': 'east'}
DIRS = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}


def h3(x, y, z, seed=0):
    """Детерминированный хеш точки (0..1)."""
    n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ (seed * 2654435761)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFFFF) / 0xFFFFFF


def noise2(x, z, scale=8.0, seed=0):
    """Гладкий шум 2D (билинейная интерполяция хешей решётки)."""
    fx, fz = x / scale, z / scale
    x0, z0 = math.floor(fx), math.floor(fz)
    tx, tz = fx - x0, fz - z0
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    a, b = h3(x0, 0, z0, seed), h3(x0 + 1, 0, z0, seed)
    c, d = h3(x0, 0, z0 + 1, seed), h3(x0 + 1, 0, z0 + 1, seed)
    return (a * (1 - tx) + b * tx) * (1 - tz) + (c * (1 - tx) + d * tx) * tz


class Theme:
    """Материалы места: стены, пол, потолок, отделка, свет. Списки — [(блок, вес)] или 'блок'."""

    def __init__(self, name, wall, floor=None, ceil=None, trim=None, light='minecraft:lantern', pillar=None, seed=1):
        self.name = name
        self.wall = self._norm(wall)
        self.floor = self._norm(floor or wall)
        self.ceil = self._norm(ceil or wall)
        self.trim = trim or self.wall[0][0]
        self.light = light
        self.pillar = pillar or self.trim
        self.seed = seed

    @staticmethod
    def _norm(p):
        if isinstance(p, str):
            return [(p, 1)]
        return [(b, w) for b, w in p]

    @staticmethod
    def pick(pal, x, y, z, seed):
        total = sum(w for _, w in pal)
        r = h3(x, y, z, seed) * total
        for b, w in pal:
            r -= w
            if r <= 0:
                return b
        return pal[-1][0]


class Map:
    def __init__(self, seed=7):
        self.blocks = {}       # (x,y,z) -> (name, props)
        self.nbt = {}          # блок-сущности
        self.open = {}         # пустоты: (x,y,z) -> тема
        self.nowall = set()    # пустоты без оболочки (пропасти, наружу)
        self.entities = []     # картины и т.п.
        self.seed = seed
        self.rnd = random.Random(seed)
        self.themes = {}

    # ---------------- блоки ----------------

    def set(self, x, y, z, block, **props):
        self.blocks[(x, y, z)] = (block, tuple(sorted((k, str(v).lower() if isinstance(v, bool) else str(v)) for k, v in props.items())))

    def get(self, x, y, z):
        v = self.blocks.get((x, y, z))
        return v[0] if v else None

    def clear(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def fill(self, x1, y1, z1, x2, y2, z2, block, **props):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, block, **props)

    def fillp(self, x1, y1, z1, x2, y2, z2, pal, seed=0):
        pal = Theme._norm(pal)
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, Theme.pick(pal, x, y, z, seed + self.seed))

    def is_open(self, x, y, z):
        return (x, y, z) in self.open and (x, y, z) not in self.blocks

    def solid_at(self, x, y, z):
        b = self.blocks.get((x, y, z))
        return b is not None and b[0] != AIR

    # ---------------- пустоты ----------------

    def theme(self, t):
        self.themes[t.name] = t
        return t

    def carve(self, x1, y1, z1, x2, y2, z2, theme, wall=True):
        """Пустота-параллелепипед; y1 — уровень пола (пол — блок y1-1)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.open[(x, y, z)] = theme.name
                    if not wall:
                        self.nowall.add((x, y, z))

    def room(self, x1, z1, x2, z2, y, h, theme, vault=0):
        """Зал: пол на y-1, высота h, сводчатый потолок (vault — на сколько выше в середине)."""
        self.carve(x1, y, z1, x2, y + h - 1, z2, theme)
        if vault > 0:
            cx, cz = (x1 + x2) / 2, (z1 + z2) / 2
            hx, hz = max(1, (x2 - x1) / 2), max(1, (z2 - z1) / 2)
            for x in range(x1, x2 + 1):
                for z in range(z1, z2 + 1):
                    k = 1 - max(((x - cx) / hx) ** 2, ((z - cz) / hz) ** 2)
                    extra = int(round(vault * math.sqrt(max(0, k))))
                    for dy in range(extra):
                        self.open[(x, y + h + dy, z)] = theme.name

    def round_room(self, cx, cz, r, y, h, theme, dome=0):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                d = math.hypot(x - cx, z - cz)
                if d <= r:
                    top = h + (int(round(dome * math.sqrt(max(0, 1 - (d / r) ** 2)))) if dome else 0)
                    for yy in range(y, y + top):
                        self.open[(x, yy, z)] = theme.name

    def cave(self, cx, cy, cz, rx, ry, rz, theme, rough=0.25, seed=0, floor=None):
        """Пещера-капля с шумной стенкой; floor — ровный пол на этой высоте (ниже не вырезать)."""
        for x in range(int(cx - rx) - 2, int(cx + rx) + 3):
            for z in range(int(cz - rz) - 2, int(cz + rz) + 3):
                n = noise2(x, z, 6, seed + 11) - 0.5
                for y in range(int(cy - ry) - 2, int(cy + ry) + 3):
                    if floor is not None and y < floor:
                        continue
                    d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                    if d <= 1 + n * rough * 2 + (h3(x, y, z, seed) - 0.5) * rough * 0.4:
                        self.open[(x, y, z)] = theme.name

    def corridor(self, points, w, h, theme, y=None):
        """Коридор по ломаной (x, z[, y]) шириной w и высотой h. Подъёмы — ровными ступенями."""
        pts = [(p[0], p[1], p[2] if len(p) > 2 else y) for p in points]
        for (ax, az, ay), (bx, bz, by) in zip(pts, pts[1:]):
            n = max(abs(bx - ax), abs(bz - az), 1)
            for i in range(n + 1):
                t = i / n
                x = round(ax + (bx - ax) * t)
                z = round(az + (bz - az) * t)
                yy = round(ay + (by - ay) * t)
                for dx in range(-(w // 2), w - w // 2):
                    for dz in range(-(w // 2), w - w // 2):
                        for dy in range(h):
                            self.open[(x + dx, yy + dy, z + dz)] = theme.name

    def pit(self, x1, z1, x2, z2, ytop, depth, theme):
        """Пропасть: пустота вниз без дна и стен (упавший возвращается на край)."""
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(ytop - depth, ytop):
                    self.open[(x, y, z)] = theme.name
                    self.nowall.add((x, y, z))

    # ---------------- свет ----------------

    def auto_lights(self, spacing=7, soul=True, themes=None, min_h=4, chain=2, offset=0):
        """Фонари на цепях под потолком по сетке (там, где потолок не ниже min_h и место свободно)."""
        cols = {}
        for (x, y, z), t in self.open.items():
            if themes and t not in themes:
                continue
            if (x + offset) % spacing or (z + offset) % spacing:
                continue
            if (x, y - 1, z) in self.open:
                continue
            cols[(x, z, y)] = t
        n = 0
        for (x, z, y) in cols:
            top = y
            while (x, top + 1, z) in self.open and top - y < 40:
                top += 1
            if top - y + 1 < min_h or (x, top + 1, z) in self.nowall:
                continue
            if any(self.solid_at(x, yy, z) for yy in range(top - chain, top + 1)):
                continue
            ln = min(chain, max(0, top - y - 3))
            self.chain_lantern(x, top, z, length=ln, soul=soul) if ln > 0 else self.lantern(x, top, z, soul=soul, hanging=True)
            n += 1
        return n

    def glow(self, spacing=6, level=11, themes=None, height=2):
        """Невидимые источники света (minecraft:light) по сетке над полом — ровная полутьма без лишних фонарей."""
        n = 0
        for (x, y, z), t in list(self.open.items()):
            if themes and t not in themes:
                continue
            if x % spacing or z % spacing or (x, y - 1, z) in self.open and not self.solid_at(x, y - 1, z):
                continue
            ly = y + height
            if (x, ly, z) not in self.open or self.solid_at(x, ly, z) or (x, ly, z) in self.blocks:
                continue
            self.set(x, ly, z, 'minecraft:light', level=level, waterlogged=False)
            n += 1
        return n

    # ---------------- оболочка ----------------

    def shell(self, thick=2):
        """Стены, пол и потолок вокруг всех пустот (материал — тема ближайшей пустоты)."""
        opn = self.open
        blocks = self.blocks
        # граница: пустоты, у которых хоть один сосед — не пустота
        border = []
        for (x, y, z) in opn:
            if ((x + 1, y, z) not in opn or (x - 1, y, z) not in opn or (x, y + 1, z) not in opn or (x, y - 1, z) not in opn
                    or (x, y, z + 1) not in opn or (x, y, z - 1) not in opn):
                border.append((x, y, z))
        rng = range(-thick, thick + 1)
        made = 0
        for (x, y, z) in border:
            t = self.themes[opn[(x, y, z)]]
            pit = (x, y, z) in self.nowall
            for dy in rng:
                if pit and dy < 0:
                    continue
                for dx in rng:
                    for dz in rng:
                        p = (x + dx, y + dy, z + dz)
                        if p in opn or p in blocks:
                            continue
                        if dy < 0 and abs(dx) <= 1 and abs(dz) <= 1 and -dy >= max(abs(dx), abs(dz)):
                            pal = t.floor
                        elif dy > 0 and abs(dx) <= 1 and abs(dz) <= 1 and dy >= max(abs(dx), abs(dz)):
                            pal = t.ceil
                        else:
                            pal = t.wall
                        blocks[p] = (Theme.pick(pal, p[0], p[1], p[2], t.seed), ())
                        made += 1
        return made

    # ---------------- отделка ----------------

    def floor_y(self, x, z, ytop):
        """Уровень пола под точкой (первая открытая клетка над твёрдым)."""
        for y in range(ytop, ytop - 64, -1):
            if (x, y, z) in self.open and not self.solid_at(x, y, z) and ((x, y - 1, z) not in self.open or self.solid_at(x, y - 1, z)):
                return y
        return ytop

    def pillar(self, x, z, y, h, block, cap=None, base=None):
        for yy in range(y, y + h):
            self.set(x, yy, z, block)
        if base:
            self.set(x, y, z, base)
        if cap:
            self.set(x, y + h - 1, z, cap)

    def big_pillar(self, x, z, y, h, block, trim=None):
        for dx in (0, 1):
            for dz in (0, 1):
                for yy in range(y, y + h):
                    self.set(x + dx, yy, z + dz, block)
        if trim:
            for dx in (0, 1):
                for dz in (0, 1):
                    self.set(x + dx, y, z + dz, trim)
                    self.set(x + dx, y + h - 1, z + dz, trim)

    def lantern(self, x, y, z, soul=False, hanging=False):
        self.set(x, y, z, 'minecraft:soul_lantern' if soul else 'minecraft:lantern', hanging=hanging, waterlogged=False)

    def chain_lantern(self, x, ytop, z, length=2, soul=False):
        for i in range(length):
            self.set(x, ytop - i, z, 'minecraft:chain', axis='y', waterlogged=False)
        self.lantern(x, ytop - length, z, soul, hanging=True)

    def candle(self, x, y, z, n=3, color=None):
        name = f'minecraft:{color}_candle' if color else 'minecraft:candle'
        self.set(x, y, z, name, candles=n, lit=True, waterlogged=False)

    def torch_wall(self, x, y, z, facing, soul=False):
        self.set(x, y, z, 'minecraft:soul_wall_torch' if soul else 'minecraft:wall_torch', facing=facing)

    def stairs(self, x, y, z, block, facing, half='bottom', shape='straight'):
        self.set(x, y, z, block, facing=facing, half=half, shape=shape, waterlogged=False)

    def slab(self, x, y, z, block, half='bottom'):
        self.set(x, y, z, block, type=half, waterlogged=False)

    def skull(self, x, y, z, kind='skeleton_skull', rot=0):
        self.set(x, y, z, 'minecraft:' + kind, rotation=rot)

    def sign(self, x, y, z, lines, facing='north', wall=True, wood='dark_oak', glow=True, color='white'):
        """Табличка с надписью (до 4 строк по ~15 символов)."""
        if wall:
            self.set(x, y, z, f'minecraft:{wood}_wall_sign', facing=facing, waterlogged=False)
        else:
            rot = {'south': 0, 'west': 4, 'north': 8, 'east': 12}[facing]
            self.set(x, y, z, f'minecraft:{wood}_sign', rotation=rot, waterlogged=False)
        msgs = [json.dumps({'text': (lines[i] if i < len(lines) else '')}, ensure_ascii=False) for i in range(4)]
        side = {'messages': TList(8, msgs), 'color': color, 'has_glowing_text': TByte(1 if glow else 0)}
        self.nbt[(x, y, z)] = {'front_text': side, 'back_text': {'messages': TList(8, [json.dumps({'text': ''})] * 4),
                                                                   'color': 'black', 'has_glowing_text': TByte(0)},
                               'is_waxed': TByte(1)}

    def cache(self, x, y, z, tier=0, facing='south'):
        self.set(x, y, z, 'litrpg:cache', facing=facing)
        self.nbt[(x, y, z)] = {'id': 'litrpg:cache', 'Tier': TInt(tier)}

    def banner(self, x, y, z, facing, color='black'):
        self.set(x, y, z, f'minecraft:{color}_wall_banner', facing=facing)

    def door_frame(self, x, y, z, axis, w, h, block):
        """Арка проёма шириной w по оси axis ('x' — проём вдоль x)."""
        for i in range(-1, w + 1):
            for j in range(h + 1):
                if i in (-1, w) or j == h:
                    if axis == 'x':
                        self.set(x + i, y + j, z, block)
                    else:
                        self.set(x, y + j, z + i, block)


class Stage:
    """Схема этапа для litrpg_dungeon.json."""

    def __init__(self, n, key, name, sub, lvl, music=''):
        self.n, self.key, self.name, self.sub, self.lvl = n, key, name, sub, lvl
        self.music = music
        self.box = None
        self.camp = None
        self.trader = None
        self.exit = None
        self.packs, self.bosses, self.gates, self.mechs, self.caches, self.lore = [], [], [], [], [], []

    def pack(self, pid, at, mobs, r=18, mini=False, ambush=False):
        """mobs: [(тип, x, y, z, ур., ранг, имя, роль)]."""
        out = []
        for m in mobs:
            t, x, y, z, lv = m[:5]
            o = {'type': t if ':' in t else 'minecraft:' + t, 'at': [x, y, z], 'lvl': lv}
            if len(m) > 5 and m[5]:
                o['rank'] = m[5]
            if len(m) > 6 and m[6]:
                o['name'] = m[6]
            if len(m) > 7 and m[7]:
                o['role'] = m[7]
            out.append(o)
        p = {'id': pid, 'at': list(at), 'r': r, 'mobs': out}
        if mini:
            p['mini'] = True
        if ambush:
            p['ambush'] = True
        self.packs.append(p)
        return p

    def boss(self, bid, etype, at, arena, doors=(), unlock=(), extra=None):
        b = {'id': bid, 'type': etype, 'at': list(at), 'arena': list(arena), 'doors': list(doors), 'unlock': list(unlock)}
        if extra:
            b['extra'] = extra
        self.bosses.append(b)

    def gate(self, gid, box, block, open_=False):
        self.gates.append({'id': gid, 'box': list(box), 'block': block, 'open': open_})

    def mech(self, mid, mtype, **p):
        p['id'] = mid
        p['type'] = mtype
        self.mechs.append(p)
        return p

    def lore_at(self, box, title='', text=''):
        self.lore.append({'box': list(box), 'title': title, 'text': text})

    def json(self):
        o = {'n': self.n, 'key': self.key, 'name': self.name, 'sub': self.sub, 'lvl': list(self.lvl), 'box': list(self.box),
             'camp': list(self.camp), 'trader': list(self.trader), 'exit': list(self.exit), 'packs': self.packs,
             'bosses': self.bosses, 'gates': self.gates, 'mechs': self.mechs, 'caches': self.caches, 'lore': self.lore,
             'music': self.music}
        return o


def apply_gates(m, stage):
    """Преграды в исходном положении: закрытые — блоком, открытые — пусто."""
    for g in stage.gates:
        x1, y1, z1, x2, y2, z2 = g['box']
        name = g['block'].split('[')[0]
        props = {}
        if '[' in g['block']:
            for kv in g['block'][g['block'].index('[') + 1:-1].split(','):
                k, v = kv.split('=')
                props[k] = v
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    if g['open']:
                        m.blocks.pop((x, y, z), None)
                    else:
                        m.set(x, y, z, name, **props)


def save_tiles(m, prefix, tile=32):
    """Порезать карту на плитки tile×tile по сетке чанков. -> [(cx, cz, y, шаблон)]."""
    by_tile = {}
    for (x, y, z), v in m.blocks.items():
        if v[0] == AIR:
            continue
        by_tile.setdefault((x // tile, z // tile), []).append((x, y, z, v))
    os.makedirs(STRUCT, exist_ok=True)
    out = []
    for (tx, tz), lst in sorted(by_tile.items()):
        ymin = min(b[1] for b in lst)
        ymax = max(b[1] for b in lst)
        palette, index = [], {}
        blocks = TList(10)
        for x, y, z, (name, props) in sorted(lst, key=lambda b: (b[1], b[0], b[2])):
            key = (name, props)
            if key not in index:
                index[key] = len(palette)
                e = {'Name': name}
                if props:
                    e['Properties'] = {k: v for k, v in props}
                palette.append(e)
            e = {'pos': TList(3, [TInt(x - tx * tile), TInt(y - ymin), TInt(z - tz * tile)]), 'state': TInt(index[key])}
            if (x, y, z) in m.nbt:
                e['nbt'] = m.nbt[(x, y, z)]
            blocks.append(e)
        ents = TList(10)
        for (x, y, z, eid, extra) in m.entities:
            if x // tile == tx and z // tile == tz:
                nbt = {'id': eid}
                nbt.update(extra)
                ents.append({'pos': TList(6, [TDouble(x + 0.5), TDouble(y - ymin + 0.5), TDouble(z + 0.5)]),
                             'blockPos': TList(3, [TInt(x - tx * tile), TInt(y - ymin), TInt(z - tz * tile)]), 'nbt': nbt})
        name = f'{prefix}_{tx}_{tz}'.replace('-', 'm')
        root = {'DataVersion': TInt(3465), 'size': TList(3, [TInt(tile), TInt(ymax - ymin + 1), TInt(tile)]),
                'palette': TList(10, palette), 'blocks': blocks, 'entities': ents}
        write_nbt(os.path.join(STRUCT, name + '.nbt'), root)
        out.append([tx * tile // 16, tz * tile // 16, ymin, 'litrpg:dungeon/' + name])
    return out


# ---------------- превью ----------------

COLORS = [('lava', (230, 100, 20)), ('water', (50, 100, 200)), ('cobweb', (230, 230, 230)), ('moss', (80, 130, 60)), ('leaves', (60, 130, 50)),
          ('grass', (90, 150, 70)), ('lantern', (255, 220, 120)), ('torch', (255, 200, 90)), ('candle', (255, 230, 150)),
          ('froglight', (250, 240, 200)), ('glowstone', (250, 220, 140)), ('sea_lantern', (200, 240, 240)), ('shroomlight', (250, 160, 80)),
          ('campfire', (255, 140, 40)), ('gold', (240, 200, 60)), ('amethyst', (160, 100, 220)), ('prismarine', (60, 140, 140)),
          ('bone', (225, 220, 200)), ('skull', (220, 220, 210)), ('iron_bars', (160, 160, 170)), ('chain', (120, 120, 130)),
          ('deepslate', (70, 70, 80)), ('blackstone', (45, 40, 50)), ('basalt', (80, 80, 90)), ('nether', (100, 30, 35)),
          ('crimson', (140, 40, 60)), ('warped', (40, 120, 110)), ('soul', (90, 70, 50)), ('mud', (90, 80, 70)), ('tuff', (100, 100, 90)),
          ('dark_oak', (70, 50, 30)), ('spruce', (110, 80, 50)), ('oak', (160, 130, 80)), ('mangrove', (120, 50, 40)),
          ('stone_brick', (125, 125, 125)), ('cobble', (115, 115, 115)), ('stone', (130, 130, 130)), ('andesite', (140, 140, 140)),
          ('sand', (220, 210, 160)), ('dirt', (130, 100, 70)), ('quartz', (235, 230, 225)), ('purpur', (170, 120, 170)),
          ('end_stone', (220, 220, 160)), ('obsidian', (30, 20, 40)), ('magma', (160, 60, 20)), ('ice', (160, 200, 250)),
          ('rift', (220, 60, 220)), ('cache', (255, 210, 60)), ('carpet', (150, 40, 40)), ('wool', (150, 40, 40))]


def color_of(name):
    n = name.split(':')[1]
    for k, c in COLORS:
        if k in n:
            return c
    return (150, 150, 150)


def preview(m, path, box, layer=None, scale=2):
    """Вид сверху на коробку box (x1, z1, x2, z2); layer=(y0, y1) — только этот срез высот (поэтажно)."""
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))
    from png import write_png
    x1, z1, x2, z2 = box
    w, h = x2 - x1 + 1, z2 - z1 + 1
    top = {}
    for (x, y, z), (name, _) in m.blocks.items():
        if name == AIR or not (x1 <= x <= x2 and z1 <= z <= z2):
            continue
        if layer and not (layer[0] <= y <= layer[1]):
            continue
        k = (x, z)
        if k not in top or top[k][0] < y:
            top[k] = (y, name)
    opens = {}
    for (x, y, z) in m.open:
        if x1 <= x <= x2 and z1 <= z <= z2 and (not layer or layer[0] <= y <= layer[1]):
            opens[(x, z)] = max(opens.get((x, z), -999), y)
    pix = []
    for z in range(z1, z2 + 1):
        row = []
        for x in range(x1, x2 + 1):
            if (x, z) in top:
                y, name = top[(x, z)]
                c = color_of(name)
                k = 0.55 + 0.45 * max(0, min(1, (y - 40) / 80))
                if (x, z) in opens and opens[(x, z)] > y:
                    k *= 0.75
                row.append((int(c[0] * k), int(c[1] * k), int(c[2] * k), 255))
            else:
                row.append((10, 10, 14, 255))
        for _ in range(scale):
            pass
        pix.append([p for p in row for _ in range(scale)])
    pix = [r for r in pix for _ in range(scale)]
    write_png(path, pix, w * scale, h * scale)
