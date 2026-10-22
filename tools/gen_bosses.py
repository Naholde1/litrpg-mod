"""
Боссы подземелий: 3D-модели (кубы) + текстуры + светящийся слой.
Пишет:
  src/main/resources/assets/litrpg/boss_models/<имя>.json   (читает Java: client/model/DataModel)
  src/main/resources/assets/litrpg/textures/entity/boss/<имя>.png и <имя>_glow.png
  текстуры алтаря подземелья и Искры Божества, названия (ru/en)
Запуск: python tools/gen_bosses.py [--preview build/preview/bosses.png]

Координаты моделей задаются «по-человечески»: в пикселях, y — вверх от земли,
перед модели смотрит в сторону -z. Кубы — в абсолютных координатах.
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', 'litrpg')


# ======================= цвет и шум =======================

def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def clamp(v):
    return max(0, min(255, int(round(v))))


def mul(c, f):
    return (clamp(c[0] * f), clamp(c[1] * f), clamp(c[2] * f))


def mix(a, b, t):
    t = max(0.0, min(1.0, t))
    return (clamp(a[0] + (b[0] - a[0]) * t), clamp(a[1] + (b[1] - a[1]) * t), clamp(a[2] + (b[2] - a[2]) * t))


def h2(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, s, cell=3.0):
    gx, gy = x / cell, y / cell
    x0, y0 = int(math.floor(gx)), int(math.floor(gy))
    fx, fy = gx - x0, gy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = h2(x0, y0, s), h2(x0 + 1, y0, s)
    c, d = h2(x0, y0 + 1, s), h2(x0 + 1, y0 + 1, s)
    return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy


def fbm(x, y, s):
    return vnoise(x, y, s, 4.0) * 0.6 + vnoise(x, y, s + 7, 2.0) * 0.3 + h2(x, y, s + 3) * 0.1


# ======================= материалы =======================
# Каждый материал: функция (x, y, w, h, face, seed) -> (r,g,b,a), glow(bool)
# face: 'top', 'bottom', 'front', 'back', 'west', 'east'

def m_stone(base, dark, light, moss=0.0, mossc=0x5E7A34, cracks=True):
    B, D, L, M = rgb(base), rgb(dark), rgb(light), rgb(mossc)

    def f(x, y, w, h, face, s):
        n = fbm(x, y, s)
        c = mix(D, B, n * 1.6) if n < 0.6 else mix(B, L, (n - 0.6) * 2.5)
        # швы кладки
        if cracks and (y % 5 == 4 or (x + (y // 5) * 3) % 7 == 6):
            c = mul(c, 0.78)
        if cracks and h2(x // 2, y // 2, s + 11) > 0.93:
            c = mul(c, 0.6)
        top_bias = 1.0 if face == 'top' else max(0.0, 1 - y / 4.0) if face != 'bottom' else 0
        if moss > 0 and vnoise(x, y, s + 5, 2.5) * top_bias > 1 - moss:
            c = mix(M, mul(M, 1.3), h2(x, y, s + 9))
        return c + (255,), False
    return f


def m_bark(base, dark, rot=0.0, fungus=0x000000):
    B, D, F = rgb(base), rgb(dark), rgb(fungus)

    def f(x, y, w, h, face, s):
        streak = vnoise(x * 3.0, y * 0.35, s, 3.0)
        c = mix(D, B, streak * 1.4)
        if (x + int(vnoise(0, y, s + 2, 4) * 3)) % 4 == 0:
            c = mul(c, 0.72)
        if face in ('top', 'bottom'):
            r = math.hypot(x - w / 2, y - h / 2)
            c = mix(rgb(0x8A6A3A), rgb(0x6A4A26), (r % 2) / 2) if r < min(w, h) / 2 else c
        if rot > 0 and vnoise(x, y, s + 4, 2) > 1 - rot:
            c = mix(c, rgb(0x3A3A22), 0.6)
        if fungus and h2(x, y, s + 8) > 0.965:
            return F + (255,), True
        return c + (255,), False
    return f


def m_leaf(base, dark, dead=0x8A6A2A, holes=0.18, deadness=0.35):
    B, D, X = rgb(base), rgb(dark), rgb(dead)

    def f(x, y, w, h, face, s):
        n = vnoise(x, y, s, 2.0)
        if face != 'top' and h2(x, y, s + 1) < holes:
            return (0, 0, 0, 0), False
        c = mix(D, B, n * 1.5)
        if vnoise(x, y, s + 3, 3) > 1 - deadness:
            c = mix(c, X, 0.75)
        if h2(x, y, s + 2) > 0.9:
            c = mul(c, 1.25)
        return c + (255,), False
    return f


def m_chitin(base, light, band=4):
    B, L = rgb(base), rgb(light)

    def f(x, y, w, h, face, s):
        c = mix(B, mul(B, 0.7), vnoise(x, y, s, 3))
        if y % band == band - 1:
            c = mul(c, 0.6)
        if face in ('top', 'front') and (x + y) % 9 == 0 and y < h / 2:
            c = mix(c, L, 0.6)
        if face == 'top' and abs(x - y - w * 0.2) < 1:
            c = mix(c, L, 0.4)
        return c + (255,), False
    return f


def m_fur(base, dark, light=None):
    B, D = rgb(base), rgb(dark)
    L = rgb(light) if light else mul(B, 1.2)

    def f(x, y, w, h, face, s):
        n = vnoise(x * 2.5, y * 0.6, s, 2.0)
        c = mix(D, B, n * 1.5)
        if h2(x, y // 2, s + 6) > 0.85:
            c = mix(c, L, 0.6)
        return c + (255,), False
    return f


def m_skin(base, shade, spots=0x000000, spot_chance=0.0):
    B, S, P = rgb(base), rgb(shade), rgb(spots)

    def f(x, y, w, h, face, s):
        c = mix(B, S, vnoise(x, y, s, 4) * 0.8)
        if spot_chance and vnoise(x, y, s + 9, 1.5) > 1 - spot_chance:
            c = mix(c, P, 0.5)
        return c + (255,), False
    return f


def m_metal(base, light, dark, rivets=True):
    B, L, D = rgb(base), rgb(light), rgb(dark)

    def f(x, y, w, h, face, s):
        c = mix(B, D, vnoise(x, y, s, 5) * 0.5)
        if y == 1 or (face == 'top'):
            c = mix(c, L, 0.5)
        if rivets and (x in (1, w - 2)) and y % 4 == 2:
            c = L
        return c + (255,), False
    return f


def m_cloth(base, dark, tatter=False):
    B, D = rgb(base), rgb(dark)

    def f(x, y, w, h, face, s):
        if tatter and face not in ('top', 'bottom') and y >= h - 3 and h2(x, 0, s) * 3 < y - (h - 4):
            return (0, 0, 0, 0), False
        fold = 0.5 + 0.5 * math.sin(x * 1.3 + vnoise(x, y, s, 4) * 3)
        c = mix(D, B, fold * 0.7 + 0.3)
        if (x + y) % 2 == 0:
            c = mul(c, 0.94)
        return c + (255,), False
    return f


def m_ice(base=0x9ED8F0, light=0xEAF8FF, deep=0x4A90B8, glow_chance=0.0):
    B, L, Dp = rgb(base), rgb(light), rgb(deep)

    def f(x, y, w, h, face, s):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else 0.2
        c = mix(L, B, t * 0.9 + vnoise(x, y, s, 3) * 0.3)
        if abs((x * 2 - y + int(h2(y, 0, s) * 3)) % 9) == 0:
            c = mix(c, Dp, 0.4)
        if h2(x, y, s + 4) > 0.95:
            c = L
        glow = glow_chance > 0 and h2(x, y, s + 12) < glow_chance
        return c + (255,), glow
    return f


def m_magma(rock=0x2A1C18, crack=0xFF8A20, hot=0xFFE070, crack_amt=0.09):
    R, C, H = rgb(rock), rgb(crack), rgb(hot)

    def f(x, y, w, h, face, s):
        n = vnoise(x, y, s, 3.5)
        if abs(n - 0.5) < crack_amt * 0.5:
            return mix(C, H, 1 - abs(n - 0.5) / (crack_amt * 0.5)) + (255,), True
        c = mix(R, mul(R, 1.6), h2(x, y, s) * 0.6)
        return c + (255,), False
    return f


def m_fire(top=0xFFF0A0, mid=0xFF8A20, low=0xC02810):
    T, M, L = rgb(top), rgb(mid), rgb(low)

    def f(x, y, w, h, face, s):
        t = y / max(1, h - 1) if face not in ('top', 'bottom') else 0.3
        t += (vnoise(x, y, s, 2) - 0.5) * 0.5
        c = mix(T, M, t * 2) if t < 0.5 else mix(M, L, (t - 0.5) * 2)
        return c + (255,), True
    return f


def m_bone(base=0xE8E0C8, dark=0xA89A78):
    B, D = rgb(base), rgb(dark)

    def f(x, y, w, h, face, s):
        c = mix(B, D, vnoise(x, y, s, 3) * 0.7)
        if y % 5 == 0 and h2(x, y, s) > 0.6:
            c = mul(c, 0.85)
        return c + (255,), False
    return f


def m_glow(color, edge=None):
    C = rgb(color)
    E = rgb(edge) if edge else mul(C, 0.6)

    def f(x, y, w, h, face, s):
        e = x in (0, w - 1) or y in (0, h - 1)
        return (E if e else C) + (255,), True
    return f


def m_hair(base, dark):
    B, D = rgb(base), rgb(dark)

    def f(x, y, w, h, face, s):
        if face not in ('top',) and y > h - 3 and h2(x, 0, s) < 0.5:
            return (0, 0, 0, 0), False
        c = mix(D, B, vnoise(x * 3, y * 0.4, s, 2))
        return c + (255,), False
    return f


def m_membrane(base, vein):
    B, V = rgb(base), rgb(vein)

    def f(x, y, w, h, face, s):
        # нижний край крыла — фестонами
        if face in ('front', 'back'):
            edge = h - 1 - int(3 * abs(math.sin((x + 1) * math.pi / 6.5)))
            if y > edge:
                return (0, 0, 0, 0), False
            if x % 6 == 5 or y == 0:
                return V + (255,), False
        c = mix(B, mul(B, 0.7), vnoise(x, y, s, 3))
        return c + (255,), False
    return f


MATS = {
    # голем
    'stone': m_stone(0x8A9096, 0x5A6066, 0xB4BAC0, moss=0.35),
    'stone_dark': m_stone(0x6A7076, 0x44484C, 0x8A9096, moss=0.15),
    'stone_moss': m_stone(0x7A8086, 0x50565A, 0xA0A6AC, moss=0.85),
    # энт
    'bark': m_bark(0x6A4A2A, 0x3E2A16, rot=0.25),
    'bark_rot': m_bark(0x5A4426, 0x2E2214, rot=0.45, fungus=0xC8F060),
    'bark_dark': m_bark(0x4A3420, 0x2A1C10),
    'leaf_rot': m_leaf(0x5E7A2A, 0x34481A, dead=0x8A6A2A, holes=0.2, deadness=0.45),
    'mushroom': m_glow(0xD8F070, 0x8AB040),
    # паучиха
    'chitin': m_chitin(0x2A2030, 0x8A70A8),
    'chitin_abd': m_chitin(0x241A2A, 0x9A70B8, band=5),
    'chitin_leg': m_chitin(0x302838, 0x7A6A90, band=3),
    'gold': m_metal(0xE8B830, 0xFFF0A0, 0x9A6A12, rivets=False),
    'bone': m_bone(),
    'bone_dark': m_bone(0xB8A890, 0x6A5A48),
    'horn': m_bone(0x3A3030, 0x1A1414),
    # ведьма
    'cloth_green': m_cloth(0x3E5A3A, 0x22341E, tatter=True),
    'cloth_black': m_cloth(0x2A2630, 0x16141A),
    'skin_hag': m_skin(0x8AA070, 0x5E7450, spots=0x4A5A3A, spot_chance=0.15),
    'hair': m_hair(0xB0B0A8, 0x6A6A64),
    'wood': m_bark(0x8A6238, 0x5A3E22),
    'glass_green': m_glow(0x90F070, 0x3A8A3A),
    # минотавр
    'fur_brown': m_fur(0x7A5030, 0x4A2E1A),
    'fur_dark': m_fur(0x3E2A1A, 0x22160E),
    'skin_bull': m_skin(0x8A5A3A, 0x6A4028),
    'skin_dark': m_skin(0x3A2A24, 0x241A16),
    'hoof': m_stone(0x2E2A26, 0x1A1816, 0x4A4440, cracks=False),
    'leather': m_cloth(0x6A4424, 0x452A14),
    'leather_dark': m_cloth(0x3E2E22, 0x261A12),
    'axe_metal': m_metal(0x9AA4AE, 0xE8EEF4, 0x5A646E, rivets=False),
    # великан
    'skin_frost': m_skin(0x9CC4D8, 0x6A94B0, spots=0xD8F0FF, spot_chance=0.1),
    'fur_white': m_fur(0xE8ECEE, 0xA8B0B8),
    'hair_white': m_hair(0xF0F8FF, 0xA8C8D8),
    'ice': m_ice(glow_chance=0.12),
    # пламя
    'magma': m_magma(),
    'magma_hot': m_magma(0x3A2018, 0xFF9A30, 0xFFF0A0, crack_amt=0.25),
    'fire': m_fire(),
    'obsidian': m_stone(0x2A2034, 0x140E1C, 0x4A3A60, cracks=False),
    # демон
    'skin_demon': m_skin(0x8A2228, 0x5A1418, spots=0x3A0A0E, spot_chance=0.12),
    'skin_demon_dark': m_skin(0x5A1418, 0x3A0A0E),
    'membrane': m_membrane(0x5A1A24, 0x2A0A10),
    'fire_soul': m_fire(0xC8F8FF, 0x40C8E8, 0x2060A0),
    # монстры регионов
    'troll_skin': m_skin(0x7A8A70, 0x55634E, spots=0x3E4A38, spot_chance=0.2),
    'troll_stone': m_stone(0x7A7E80, 0x50545A, 0x9AA0A4, moss=0.55),
    'wraith_robe': m_cloth(0xBFE4F2, 0x6A9AB8, tatter=True),
    'wraith_face': m_glow(0x1A2A3A, 0x0A1420),
    'wisp_core': m_glow(0xE8FFB0, 0xA8E060),
    'wisp_leaf': m_leaf(0x6ACF5A, 0x3E8A3A, dead=0x8ACF5A, holes=0.3, deadness=0.1),
    'imp_skin': m_skin(0xB03828, 0x7A2018, spots=0x401008, spot_chance=0.15),
    'bog_skin': m_skin(0x4E6A3A, 0x2E4224, spots=0x8A9A40, spot_chance=0.25),
    'scorp_chitin': m_chitin(0xA87A40, 0xE8C080, band=3),
    'scorp_dark': m_chitin(0x5A3A1A, 0xA87A40, band=2),
    'knight_armor': m_metal(0x4A4A52, 0x8A8A9A, 0x24242A),
    'knight_cloth': m_cloth(0x4A2A5A, 0x2A1430, tatter=True),
    # крысы из городских поручений
    'rat_fur': m_fur(0x6E6058, 0x463C36, 0x8E8078),
    'rat_king_fur': m_fur(0x3A302A, 0x1E1814, 0x5A4C44),
    'rat_skin': m_skin(0xD8A0A0, 0xB07A7A),
}

FACE_SEED = {'top': 1, 'bottom': 2, 'front': 3, 'back': 4, 'west': 5, 'east': 6}
FACE_SHADE = {'top': 1.12, 'bottom': 0.62, 'front': 1.0, 'back': 0.82, 'west': 0.9, 'east': 0.9}


# ======================= описание модели =======================

class Cube:
    def __init__(self, x, y, z, w, h, d, mat, decals=(), infl=0.0):
        self.x, self.y, self.z, self.w, self.h, self.d = x, y, z, w, h, d
        self.mat = mat
        self.decals = list(decals)
        self.infl = infl
        self.uv = (0, 0)


class Part:
    def __init__(self, name, parent, pivot, cubes, rot=(0, 0, 0), anim=()):
        self.name, self.parent = name, parent
        self.pivot = pivot
        self.cubes = cubes
        self.rot = rot
        self.anim = list(anim)


def mirror(p, name, parent=None):
    """Зеркальная копия части (правая рука из левой): x -> -x, повороты и размахи по y/z — со знаком минус."""
    cubes = [Cube(-(c.x + c.w), c.y, c.z, c.w, c.h, c.d, c.mat, [mirror_decal(dc, c) for dc in c.decals], c.infl) for c in p.cubes]
    anim = []
    for a in p.anim:
        a = dict(a)
        if a.get('axis', 'x') in ('y', 'z') and a['type'] not in ('spin',):
            a['amp'] = -a.get('amp', 1.0)
        if a['type'] in ('walk', 'walkabs') and a.get('flip', True):
            a['phase'] = a.get('phase', 0.0) + math.pi
        a.pop('flip', None)
        anim.append(a)
    return Part(name, parent or p.parent, (-p.pivot[0], p.pivot[1], p.pivot[2]), cubes,
                (p.rot[0], -p.rot[1], -p.rot[2]), anim)


def mirror_decal(dc, c):
    face, x, y, w, h, color, glow = dc
    if face in ('front', 'back', 'top', 'bottom'):
        size = c.w
        return (face, size - x - w, y, w, h, color, glow)
    swap = {'west': 'east', 'east': 'west'}
    return (swap.get(face, face), x, y, w, h, color, glow)


def A(type, axis='x', amp=1.0, speed=1.0, phase=0.0, flip=True):
    return dict(type=type, axis=axis, amp=amp, speed=speed, phase=phase, flip=flip)


def eyes(face_w, y, color, gap=2, size=(2, 1), glow=True, x_off=0):
    """Пара глаз на передней грани шириной face_w."""
    w, h = size
    cx = face_w // 2 + x_off
    return [('front', cx - gap // 2 - w, y, w, h, color, glow), ('front', cx + (gap + 1) // 2, y, w, h, color, glow)]


# ----------------------- 1. Голем Руин -----------------------

def ruin_golem():
    leg = Part('leg_l', 'root', (5, 14, 0), [Cube(1, 0, -4, 8, 14, 8, 'stone_dark')], anim=[A('walk', 'x', 0.7)])
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-9, 11, -5, 18, 6, 10, 'stone_dark'),
        Cube(-12, 17, -7, 24, 16, 14, 'stone', decals=[('front', 9, 4, 6, 6, 0x2A7A8A, True), ('front', 10, 5, 4, 4, 0x7FF0FF, True),
                                                      ('front', 11, 6, 2, 2, 0xE0FFFF, True), ('back', 4, 3, 2, 8, 0x5FD3E8, True),
                                                      ('back', 18, 3, 2, 8, 0x5FD3E8, True)]),
        Cube(-16, 28, -6, 7, 7, 12, 'stone_moss'),
        Cube(9, 28, -6, 7, 7, 12, 'stone_moss'),
    ], anim=[A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 33, -3), [
        Cube(-5, 31, -11, 10, 9, 9, 'stone', decals=eyes(10, 3, 0x7FF0FF, gap=2, size=(2, 2)) + [('front', 2, 1, 6, 1, 0x3A3E42, False)]),
        Cube(-4, 29, -10, 8, 3, 6, 'stone_dark'),
    ], anim=[A('look', 'y'), A('look', 'x')])
    arm = Part('arm_l', 'body', (14, 31, 0), [
        Cube(12, 17, -4, 7, 15, 8, 'stone'),
        Cube(11, 7, -5, 9, 10, 10, 'stone_dark', decals=[('front', 2, 3, 1, 4, 0x5FD3E8, True), ('front', 6, 3, 1, 4, 0x5FD3E8, True)]),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 1.8), A('cast', 'x', -2.6), A('cast2', 'z', -0.9)])
    return dict(name='ruin_golem', scale=1.0, shadow=1.2,
                parts=[leg, mirror(leg, 'leg_r'), body, head, arm, mirror(arm, 'arm_r')])


# ----------------------- 2. Гниющий Энт -----------------------

def rotting_ent():
    leg = Part('leg_l', 'root', (4, 14, 0), [Cube(1, 0, -4, 7, 15, 8, 'bark_dark'), Cube(1, 0, -8, 5, 3, 4, 'bark_dark')],
               anim=[A('walk', 'x', 0.5)])
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-7, 13, -6, 14, 30, 12, 'bark_rot', decals=[('front', 3, 12, 3, 3, 0xC8F060, True), ('east', 6, 20, 2, 2, 0xC8F060, True)]),
        Cube(-8, 26, -2, 2, 6, 4, 'mushroom'),
        Cube(6, 18, -3, 3, 2, 5, 'mushroom'),
    ], anim=[A('idle', 'z', 0.04, 0.07)])
    head = Part('head', 'body', (0, 43, 0), [
        Cube(-6, 42, -7, 12, 9, 10, 'bark', decals=eyes(12, 3, 0xD8FF60, gap=4, size=(2, 2)) +
             [('front', 3, 6, 6, 2, 0x140C06, False), ('front', 4, 8, 4, 1, 0x140C06, False)]),
        Cube(-13, 50, -11, 26, 10, 22, 'leaf_rot'),
        Cube(-8, 58, -7, 16, 5, 14, 'leaf_rot'),
        Cube(-17, 47, -2, 6, 3, 3, 'bark'),
        Cube(11, 48, -1, 6, 3, 3, 'bark'),
    ], anim=[A('look', 'y', 0.6), A('look', 'x', 0.5), A('idle', 'z', 0.05, 0.05, 1.0)])
    arm = Part('arm_l', 'body', (9, 40, 0), [
        Cube(7, 20, -3, 5, 20, 6, 'bark'),
        Cube(6, 13, -4, 7, 7, 8, 'bark_dark'),
        Cube(12, 32, -3, 4, 2, 5, 'mushroom'),
    ], anim=[A('walk', 'x', 0.4, phase=math.pi), A('attack', 'x', 1.6), A('cast', 'x', -2.2), A('cast2', 'z', -1.1),
             A('idle', 'z', -0.06, 0.08)])
    return dict(name='rotting_ent', scale=1.0, shadow=1.1,
                parts=[leg, mirror(leg, 'leg_r'), body, head, arm, mirror(arm, 'arm_r')])


# ----------------------- 3. Королева Арахна -----------------------

def spider_queen():
    thorax = Part('thorax', 'root', (0, 12, 0), [Cube(-7, 7, -8, 14, 10, 12, 'chitin')])
    abdomen = Part('abdomen', 'thorax', (0, 14, 4), [
        Cube(-11, 9, 4, 22, 17, 24, 'chitin_abd', decals=[
            ('top', 9, 4, 4, 3, 0xD02040, True), ('top', 10, 7, 2, 4, 0xD02040, True), ('top', 9, 11, 4, 3, 0xD02040, True),
            ('back', 9, 5, 4, 2, 0xD02040, True), ('back', 10, 7, 2, 3, 0xD02040, True), ('back', 9, 10, 4, 2, 0xD02040, True)]),
    ], anim=[A('idle', 'x', 0.05, 0.12)])
    head = Part('head', 'thorax', (0, 12, -8), [
        Cube(-5, 8, -16, 10, 9, 8, 'chitin', decals=[
            ('front', 2, 2, 2, 2, 0xFF3040, True), ('front', 6, 2, 2, 2, 0xFF3040, True),
            ('front', 1, 5, 1, 1, 0xFF3040, True), ('front', 4, 4, 1, 1, 0xFF3040, True),
            ('front', 5, 4, 1, 1, 0xFF3040, True), ('front', 8, 5, 1, 1, 0xFF3040, True),
            ('front', 3, 6, 1, 1, 0xFF6070, True), ('front', 6, 6, 1, 1, 0xFF6070, True)]),
        Cube(-4, 3, -17, 2, 6, 2, 'bone'),
        Cube(2, 3, -17, 2, 6, 2, 'bone'),
        Cube(-4, 17, -14, 8, 2, 5, 'gold', decals=[('front', 1, 0, 1, 1, 0xFF3040, True), ('front', 6, 0, 1, 1, 0xFF3040, True)]),
        Cube(-4, 19, -13, 1, 3, 1, 'gold'), Cube(-1, 19, -14, 2, 4, 1, 'gold'), Cube(3, 19, -13, 1, 3, 1, 'gold'),
    ], anim=[A('look', 'y', 0.7), A('look', 'x', 0.5)])
    parts = [thorax, abdomen, head]
    zs = [-6, -2, 2, 6]
    yr = [40, 18, -18, -40]
    zr = [38, 32, 32, 38]
    ph = [0, math.pi, 0, math.pi]
    for i, z in enumerate(zs):
        leg = Part(f'leg_l{i}', 'thorax', (6, 12, z), [Cube(6, 11, z - 1, 18, 3, 3, 'chitin_leg')],
                   rot=(0, yr[i], zr[i]), anim=[A('walk', 'y', 0.45, 1.5, ph[i], flip=False), A('walkabs', 'z', -0.4, 1.5, ph[i], flip=False)])
        parts.append(leg)
        r = mirror(leg, f'leg_r{i}')
        for a in r.anim:
            a['phase'] = ph[i] + math.pi
        parts.append(r)
    return dict(name='spider_queen', scale=1.0, shadow=1.5, parts=parts)


# ----------------------- 4. Морра, Болотная ведьма -----------------------

def swamp_hag():
    body = Part('body', 'root', (0, 16, 0), [Cube(-7, 0, -5, 14, 17, 10, 'cloth_green',
                                                  decals=[('front', 3, 1, 3, 4, 0x90F070, True)])],
                anim=[A('idle', 'x', 0.03, 0.09)])
    torso = Part('torso', 'body', (0, 16, 0), [Cube(-6, 16, -4, 12, 12, 8, 'cloth_green')], rot=(18, 0, 0))
    head = Part('head', 'body', (0, 27, -4), [
        Cube(-4, 26, -10, 8, 8, 8, 'skin_hag', decals=eyes(8, 3, 0xF0E040, gap=2, size=(1, 1)) +
             [('front', 2, 6, 4, 1, 0x2A2A1A, False), ('front', 1, 1, 2, 1, 0x4A5A3A, False), ('front', 5, 1, 2, 1, 0x4A5A3A, False)]),
        Cube(-1, 28, -13, 2, 4, 3, 'skin_hag', decals=[('front', 0, 3, 1, 1, 0x4A5A3A, False)]),
        Cube(-5, 24, -9, 10, 8, 10, 'hair'),
        Cube(-9, 34, -14, 18, 1, 18, 'cloth_black'),
        Cube(-5, 35, -10, 10, 6, 10, 'cloth_black', decals=[('front', 0, 4, 10, 1, 0x6A4A8A, False), ('front', 4, 4, 2, 1, 0xF0E040, True)]),
        Cube(-3, 41, -8, 6, 4, 6, 'cloth_black'),
        Cube(-2, 44, -6, 3, 4, 3, 'cloth_black'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (7, 26, -2), [
        Cube(6, 18, -4, 4, 9, 5, 'cloth_green'),
        Cube(6, 12, -3, 4, 6, 4, 'skin_hag'),
    ], anim=[A('walk', 'x', 0.3, phase=math.pi), A('attack', 'x', 1.4), A('cast', 'x', -2.3), A('cast2', 'z', -1.0),
             A('idle', 'z', -0.05, 0.1)])
    arm_r = mirror(arm, 'arm_r')
    staff = Part('staff', 'arm_r', (-8, 14, -2), [
        Cube(-9, 0, -2, 2, 34, 2, 'wood'),
        Cube(-10, 34, -3, 4, 4, 4, 'bone', decals=eyes(4, 1, 0x90F070, gap=0, size=(1, 1))),
        Cube(-10, 31, -2, 1, 3, 1, 'glass_green'),
    ])
    return dict(name='swamp_hag', scale=1.0, shadow=0.6, parts=[body, torso, head, arm, arm_r, staff])


# ----------------------- 5. Астерион, Минотавр -----------------------

def minotaur():
    leg = Part('leg_l', 'root', (5, 17, 0), [
        Cube(2, 9, -4, 7, 9, 8, 'fur_brown'),
        Cube(2, 3, -1, 6, 7, 6, 'fur_brown'),
        Cube(2, 0, -3, 6, 3, 7, 'hoof'),
    ], anim=[A('walk', 'x', 0.7)])
    body = Part('body', 'root', (0, 17, 0), [
        Cube(-8, 16, -5, 16, 5, 10, 'leather', decals=[('front', 6, 1, 4, 3, 0xE8B830, False)]),
        Cube(-11, 21, -6, 22, 14, 12, 'skin_bull', decals=[('front', 4, 2, 14, 6, 0x5A3A22, False), ('front', 10, 8, 2, 5, 0x6A4028, False)]),
        Cube(-10, 33, -1, 20, 6, 9, 'fur_dark'),
    ], anim=[A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 35, -3), [
        Cube(-6, 34, -13, 12, 11, 10, 'fur_brown', decals=eyes(12, 4, 0xFF3020, gap=6, size=(2, 1)) + [('front', 3, 2, 6, 1, 0x3A2214, False)]),
        Cube(-4, 33, -18, 8, 7, 5, 'skin_dark', decals=[('front', 1, 2, 2, 1, 0x0A0606, False), ('front', 5, 2, 2, 1, 0x0A0606, False),
                                                        ('front', 3, 4, 2, 2, 0xE8B830, False)]),
        Cube(6, 41, -9, 6, 3, 3, 'bone'), Cube(10, 43, -9, 3, 6, 3, 'bone'), Cube(11, 48, -9, 2, 3, 2, 'bone'),
        Cube(-12, 41, -9, 6, 3, 3, 'bone'), Cube(-13, 43, -9, 3, 6, 3, 'bone'), Cube(-13, 48, -9, 2, 3, 2, 'bone'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (13, 33, 0), [
        Cube(11, 19, -4, 7, 15, 8, 'skin_bull'),
        Cube(11, 12, -4, 7, 7, 8, 'skin_bull', decals=[('front', 0, 0, 7, 3, 0x8A949E, False), ('west', 0, 0, 8, 3, 0x8A949E, False),
                                                       ('east', 0, 0, 8, 3, 0x8A949E, False), ('back', 0, 0, 7, 3, 0x8A949E, False)]),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.4), A('cast2', 'z', -1.3)])
    arm_r = mirror(arm, 'arm_r')
    axe = Part('axe', 'arm_r', (-14, 14, 0), [
        Cube(-15, 3, -1, 2, 32, 2, 'wood'),
        Cube(-24, 22, -1, 9, 12, 2, 'axe_metal', decals=[('front', 0, 0, 1, 12, 0xF4F8FC, False), ('back', 8, 0, 1, 12, 0xF4F8FC, False)]),
        Cube(-16, 35, -2, 4, 2, 4, 'axe_metal'),
    ])
    return dict(name='minotaur', scale=1.0, shadow=1.0, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, axe])


# ----------------------- 6. Имир, Ледяной великан -----------------------

def frost_giant():
    leg = Part('leg_l', 'root', (5, 16, 0), [
        Cube(1, 5, -4, 8, 11, 8, 'leather_dark'),
        Cube(0, 0, -5, 10, 6, 10, 'fur_white'),
    ], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 16, 0), [
        Cube(-10, 15, -5, 20, 4, 10, 'leather', decals=[('front', 8, 0, 4, 4, 0x9ED8F0, True)]),
        Cube(-12, 19, -7, 24, 16, 14, 'skin_frost', decals=[
            ('front', 5, 3, 1, 8, 0x7FE8FF, True), ('front', 18, 3, 1, 8, 0x7FE8FF, True), ('front', 6, 10, 12, 1, 0x7FE8FF, True),
            ('front', 11, 4, 2, 2, 0xCFF8FF, True)]),
        Cube(-13, 13, 7, 26, 23, 2, 'fur_white'),
        Cube(-17, 33, -3, 5, 8, 5, 'ice'), Cube(12, 33, -3, 5, 8, 5, 'ice'),
        Cube(-15, 41, -1, 3, 5, 3, 'ice'), Cube(12, 41, -1, 3, 5, 3, 'ice'),
    ], anim=[A('idle', 'x', 0.025, 0.08)])
    head = Part('head', 'body', (0, 35, -2), [
        Cube(-5, 35, -9, 10, 10, 9, 'skin_frost', decals=eyes(10, 4, 0x8FF4FF, gap=2, size=(2, 1)) + [('front', 2, 3, 6, 1, 0xE8F4FA, False)]),
        Cube(-6, 29, -11, 12, 8, 3, 'hair_white'),
        Cube(-4, 25, -11, 8, 4, 3, 'hair_white'),
        Cube(-5, 44, -7, 10, 2, 8, 'hair_white'),
        Cube(-5, 46, -7, 2, 4, 2, 'ice'), Cube(-1, 46, -8, 2, 6, 2, 'ice'), Cube(3, 46, -7, 2, 4, 2, 'ice'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (13, 33, 0), [
        Cube(12, 17, -4, 7, 17, 8, 'skin_frost'),
        Cube(11, 16, -5, 9, 5, 10, 'fur_white'),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.5), A('cast2', 'z', -1.2)])
    arm_r = mirror(arm, 'arm_r')
    club = Part('club', 'arm_r', (-15, 18, 0), [
        Cube(-17, 9, -1, 3, 10, 3, 'wood'),
        Cube(-19, 1, -3, 7, 9, 7, 'ice'),
    ])
    return dict(name='frost_giant', scale=1.15, shadow=1.2, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, club])


# ----------------------- 7. Игнис, Владыка пламени -----------------------

def flame_lord():
    body = Part('body', 'root', (0, 20, 0), [
        Cube(-9, 20, -6, 18, 15, 12, 'magma', decals=[('front', 7, 4, 4, 4, 0xFFF0A0, True)]),
        Cube(-6, 10, -4, 12, 10, 8, 'fire'),
        Cube(-4, 4, -3, 8, 6, 6, 'fire'),
        Cube(-2, 1, -2, 4, 3, 4, 'fire'),
        Cube(-13, 32, -4, 5, 4, 8, 'obsidian'), Cube(8, 32, -4, 5, 4, 8, 'obsidian'),
        Cube(-12, 36, -1, 2, 3, 2, 'obsidian'), Cube(10, 36, -1, 2, 3, 2, 'obsidian'),
    ], anim=[A('bob', 'y', 1.5, 0.1), A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 35, -1), [
        Cube(-5, 35, -6, 10, 9, 10, 'magma', decals=eyes(10, 3, 0xFFFFE0, gap=2, size=(2, 2)) + [('front', 3, 6, 4, 1, 0xFFA030, True)]),
        Cube(-9, 41, -2, 4, 3, 3, 'obsidian'), Cube(-10, 43, -2, 3, 5, 3, 'obsidian'),
        Cube(5, 41, -2, 4, 3, 3, 'obsidian'), Cube(7, 43, -2, 3, 5, 3, 'obsidian'),
        Cube(-4, 44, -3, 8, 5, 8, 'fire'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (10, 32, 0), [
        Cube(9, 17, -3, 6, 15, 6, 'magma'),
        Cube(8, 11, -4, 8, 6, 8, 'magma_hot'),
    ], anim=[A('idle', 'x', 0.12, 0.08), A('attack', 'x', 1.8), A('cast', 'x', -2.4), A('cast2', 'z', -1.0)])
    ring1 = Part('ring1', 'body', (0, 24, 0), [
        Cube(14, 22, -2, 4, 4, 4, 'magma'), Cube(-18, 24, -2, 4, 4, 4, 'magma'),
        Cube(-2, 26, 14, 4, 4, 4, 'magma'), Cube(-2, 20, -18, 4, 4, 4, 'magma'),
    ], anim=[A('spin', 'y', 1.0, 0.06)])
    ring2 = Part('ring2', 'body', (0, 38, 0), [
        Cube(9, 37, -1, 2, 2, 2, 'fire'), Cube(-11, 39, -1, 2, 2, 2, 'fire'),
        Cube(-1, 38, 9, 2, 2, 2, 'fire'), Cube(-1, 36, -11, 2, 2, 2, 'fire'),
    ], anim=[A('spin', 'y', 1.0, -0.09)])
    return dict(name='flame_lord', scale=1.15, shadow=0.9, parts=[body, head, arm, mirror(arm, 'arm_r'), ring1, ring2])


# ----------------------- 8. Баал'Зар, Архидемон -----------------------

def archdemon():
    leg = Part('leg_l', 'root', (5, 18, 0), [
        Cube(2, 10, -4, 7, 9, 8, 'skin_demon'),
        Cube(2, 3, 0, 6, 8, 6, 'skin_demon'),
        Cube(2, 0, -3, 6, 3, 7, 'obsidian'),
    ], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 18, 0), [
        Cube(-8, 17, -5, 16, 6, 10, 'obsidian', decals=[('front', 3, 2, 2, 2, 0xFF5020, True), ('front', 11, 2, 2, 2, 0xFF5020, True),
                                                       ('front', 7, 1, 2, 4, 0xFFA040, True)]),
        Cube(-12, 23, -7, 24, 14, 14, 'skin_demon', decals=[
            ('front', 10, 4, 4, 5, 0xFF6020, True), ('front', 11, 5, 2, 3, 0xFFE070, True),
            ('front', 7, 6, 2, 1, 0xFF6020, True), ('front', 15, 6, 2, 1, 0xFF6020, True), ('front', 11, 10, 2, 2, 0xFF6020, True)]),
        Cube(-1, 33, 7, 2, 5, 3, 'obsidian'), Cube(-1, 27, 7, 2, 4, 3, 'obsidian'),
        Cube(-17, 33, -5, 6, 5, 10, 'obsidian'), Cube(11, 33, -5, 6, 5, 10, 'obsidian'),
        Cube(-15, 38, -1, 2, 4, 2, 'obsidian'), Cube(13, 38, -1, 2, 4, 2, 'obsidian'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    tail = Part('tail', 'body', (0, 19, 5), [
        Cube(-2, 17, 5, 4, 4, 12, 'skin_demon'),
        Cube(-3, 16, 17, 6, 6, 4, 'obsidian'),
    ], rot=(-20, 0, 0), anim=[A('idle', 'y', 0.3, 0.1)])
    head = Part('head', 'body', (0, 37, -2), [
        Cube(-5, 37, -9, 10, 10, 10, 'skin_demon', decals=eyes(10, 3, 0xFFC020, gap=2, size=(2, 1)) +
             [('front', 2, 2, 6, 1, 0x3A0A0E, False), ('front', 3, 7, 4, 1, 0xFF6020, True)]),
        Cube(-4, 35, -10, 8, 3, 7, 'skin_demon_dark', decals=[('front', 1, 0, 1, 2, 0xE8E0C8, False), ('front', 6, 0, 1, 2, 0xE8E0C8, False)]),
        Cube(5, 44, -6, 6, 4, 4, 'horn'), Cube(9, 47, -5, 4, 7, 3, 'horn'), Cube(10, 54, -5, 3, 4, 3, 'horn'),
        Cube(-11, 44, -6, 6, 4, 4, 'horn'), Cube(-13, 47, -5, 4, 7, 3, 'horn'), Cube(-13, 54, -5, 3, 4, 3, 'horn'),
        Cube(-4, 47, -7, 8, 3, 8, 'fire'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (14, 35, 0), [
        Cube(12, 20, -4, 6, 15, 8, 'skin_demon'),
        Cube(11, 14, -5, 8, 6, 10, 'obsidian', decals=[('bottom', 1, 1, 1, 8, 0xFF5020, True), ('bottom', 6, 1, 1, 8, 0xFF5020, True)]),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.5), A('cast2', 'z', -1.1)])
    wing = Part('wing_l', 'body', (6, 33, 7), [
        Cube(6, 32, 7, 26, 3, 3, 'bone_dark'),
        Cube(7, 14, 8, 25, 18, 1, 'membrane'),
    ], rot=(0, -25, -18), anim=[A('idle', 'y', 0.3, 0.12), A('idle', 'z', 0.12, 0.12, 0.5)])
    return dict(name='archdemon', scale=1.3, shadow=1.4,
                parts=[leg, mirror(leg, 'leg_r'), body, tail, head, arm, mirror(arm, 'arm_r'), wing, mirror(wing, 'wing_r')])


def stone_troll():
    leg = Part('leg_l', 'root', (4, 12, 0), [Cube(1, 0, -3, 6, 12, 6, 'troll_skin')], anim=[A('walk', 'x', 0.7)])
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-8, 11, -5, 16, 14, 11, 'troll_skin', decals=[('front', 6, 3, 4, 2, 0x3E4A38, False)]),
        Cube(-9, 18, 2, 18, 9, 6, 'troll_stone'),
        Cube(-4, 26, 3, 8, 3, 4, 'troll_stone'),
    ], rot=(15, 0, 0), anim=[A('idle', 'x', 0.04, 0.08)])
    head = Part('head', 'body', (0, 25, -5), [
        Cube(-4, 22, -12, 8, 7, 7, 'troll_skin', decals=eyes(8, 2, 0xFFD040, gap=2, size=(1, 1)) + [('front', 2, 5, 4, 1, 0xE8E0C8, False)]),
        Cube(-1, 24, -14, 2, 3, 2, 'troll_skin'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (9, 23, -1), [
        Cube(8, 6, -3, 5, 17, 6, 'troll_skin'),
        Cube(7, 2, -4, 7, 5, 7, 'troll_stone'),
    ], anim=[A('walk', 'x', 0.6, phase=math.pi), A('attack', 'x', 1.8), A('cast', 'x', -2.6)])
    return dict(name='stone_troll', scale=1.0, shadow=0.9, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, mirror(arm, 'arm_r')])


def frost_wraith():
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-5, 2, -3, 10, 14, 6, 'wraith_robe'),
        Cube(-4, 16, -3, 8, 8, 6, 'wraith_robe'),
    ], anim=[A('bob', 'y', 1.2, 0.12), A('idle', 'x', 0.05, 0.1)])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -4, 8, 8, 8, 'wraith_robe', decals=[('front', 1, 2, 6, 5, 0x1A2A3A, False)] + eyes(8, 4, 0x8FF4FF, gap=2, size=(1, 1))),
        Cube(-2, 32, -2, 4, 3, 4, 'ice'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (5, 22, 0), [Cube(5, 10, -2, 3, 12, 3, 'wraith_robe'), Cube(5, 8, -2, 3, 2, 3, 'ice')],
               rot=(-20, 0, 0), anim=[A('idle', 'x', 0.15, 0.1), A('cast2', 'z', -1.2), A('attack', 'x', 1.5)])
    return dict(name='frost_wraith', scale=1.0, shadow=0.5, parts=[body, head, arm, mirror(arm, 'arm_r')])


def forest_wisp():
    core = Part('core', 'root', (0, 6, 0), [
        Cube(-2, 4, -2, 4, 4, 4, 'wisp_core'),
    ], anim=[A('bob', 'y', 1.5, 0.15), A('spin', 'y', 1.0, 0.05)])
    leaves = Part('leaves', 'core', (0, 6, 0), [
        Cube(-4, 5, -1, 2, 2, 2, 'wisp_leaf'), Cube(2, 5, -1, 2, 2, 2, 'wisp_leaf'),
        Cube(-1, 5, -4, 2, 2, 2, 'wisp_leaf'), Cube(-1, 5, 2, 2, 2, 2, 'wisp_leaf'),
        Cube(-1, 9, -1, 2, 2, 2, 'wisp_leaf'),
    ], anim=[A('spin', 'y', 1.0, -0.12)])
    return dict(name='forest_wisp', scale=1.0, shadow=0.2, parts=[core, leaves])


def ash_imp():
    leg = Part('leg_l', 'root', (2, 7, 0), [Cube(1, 0, -1, 2, 7, 3, 'imp_skin')], anim=[A('walk', 'x', 0.9)])
    body = Part('body', 'root', (0, 7, 0), [Cube(-3, 7, -2, 6, 7, 4, 'imp_skin')], anim=[A('idle', 'x', 0.05, 0.15)])
    head = Part('head', 'body', (0, 14, 0), [
        Cube(-3, 14, -3, 6, 6, 6, 'imp_skin', decals=eyes(6, 2, 0xFFE040, gap=2, size=(1, 1)) + [('front', 2, 4, 2, 1, 0x2A0808, False)]),
        Cube(-3, 20, -1, 1, 3, 1, 'obsidian'), Cube(2, 20, -1, 1, 3, 1, 'obsidian'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6)])
    arm = Part('arm_l', 'body', (4, 13, 0), [Cube(3, 7, -1, 2, 6, 2, 'imp_skin')], anim=[A('walk', 'x', 0.7, phase=math.pi), A('cast', 'x', -2.4)])
    wing = Part('wing_l', 'body', (2, 13, 2), [Cube(2, 8, 2, 7, 6, 1, 'membrane')], rot=(0, -30, 0), anim=[A('idle', 'y', 0.5, 0.5)])
    tail = Part('tail', 'body', (0, 8, 2), [Cube(-1, 7, 2, 1, 1, 6, 'imp_skin'), Cube(-1, 6, 7, 2, 2, 2, 'fire')], anim=[A('idle', 'y', 0.4, 0.2)])
    return dict(name='ash_imp', scale=1.0, shadow=0.4,
                parts=[leg, mirror(leg, 'leg_r'), body, head, arm, mirror(arm, 'arm_r'), wing, mirror(wing, 'wing_r'), tail])


def bog_lurker():
    body = Part('body', 'root', (0, 6, 0), [
        Cube(-7, 3, -6, 14, 8, 14, 'bog_skin', decals=[('top', 3, 3, 2, 2, 0x8A9A40, False), ('top', 9, 7, 2, 2, 0x8A9A40, False)]),
    ], anim=[A('idle', 'x', 0.05, 0.12)])
    head = Part('head', 'body', (0, 8, -6), [
        Cube(-6, 4, -12, 12, 6, 7, 'bog_skin', decals=[('top', 1, 1, 2, 2, 0xF0E040, True), ('top', 9, 1, 2, 2, 0xF0E040, True),
                                                      ('front', 1, 4, 10, 1, 0x1A2410, False)]),
    ], anim=[A('look', 'y', 0.6), A('cast', 'x', -0.5)])
    parts = [body, head]
    for i, z in enumerate((-4, 4)):
        leg = Part(f'leg_l{i}', 'body', (6, 5, z), [Cube(6, 0, z - 1, 4, 5, 3, 'bog_skin')], rot=(0, 0, 20),
                   anim=[A('walk', 'x', 0.8, phase=i * math.pi)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return dict(name='bog_lurker', scale=1.0, shadow=0.8, parts=parts)


def sand_scorpion():
    body = Part('body', 'root', (0, 6, 0), [
        Cube(-5, 3, -8, 10, 5, 14, 'scorp_chitin'),
    ])
    head = Part('head', 'body', (0, 5, -8), [
        Cube(-3, 3, -12, 6, 4, 4, 'scorp_chitin', decals=eyes(6, 1, 0x1A0A00, gap=2, size=(1, 1))),
    ], anim=[A('look', 'y', 0.4)])
    parts = [body, head]
    for side in (1, -1):
        claw = Part(f'claw_{"l" if side > 0 else "r"}', 'body', (3 * side, 5, -8), [
            Cube(3 if side > 0 else -6, 4, -16, 3, 2, 8, 'scorp_dark'),
            Cube(2 if side > 0 else -7, 3, -19, 5, 3, 4, 'scorp_dark'),
        ], rot=(0, -20 * side, 0), anim=[A('idle', 'y', 0.12 * side, 0.2), A('attack', 'x', 0.8)])
        parts.append(claw)
    tail = Part('tail', 'body', (0, 7, 6), [
        Cube(-1, 7, 6, 3, 3, 6, 'scorp_chitin'),
        Cube(-1, 10, 10, 3, 6, 3, 'scorp_chitin'),
        Cube(-1, 15, 6, 3, 3, 5, 'scorp_dark'),
        Cube(0, 13, 4, 1, 3, 2, 'bone'),
    ], rot=(-15, 0, 0), anim=[A('idle', 'x', 0.1, 0.15), A('cast', 'x', -0.8)])
    parts.append(tail)
    for i, z in enumerate((-5, -1, 3)):
        leg = Part(f'leg_l{i}', 'body', (5, 5, z), [Cube(5, 4, z, 8, 2, 2, 'scorp_dark')], rot=(0, 0, 35),
                   anim=[A('walk', 'y', 0.4, 1.5, i * math.pi, flip=False)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return dict(name='sand_scorpion', scale=1.0, shadow=0.9, parts=parts)


def grave_knight():
    leg = Part('leg_l', 'root', (2, 12, 0), [Cube(0, 0, -2, 4, 12, 4, 'knight_armor')], anim=[A('walk', 'x', 0.7)])
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -2, 8, 12, 4, 'knight_armor', decals=[('front', 3, 2, 2, 6, 0x8E44AD, True)]),
        Cube(-5, 4, 2, 10, 18, 1, 'knight_cloth'),
        Cube(-6, 21, -3, 3, 3, 6, 'knight_armor'), Cube(3, 21, -3, 3, 3, 6, 'knight_armor'),
    ])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -4, 8, 8, 8, 'knight_armor', decals=[('front', 1, 3, 6, 1, 0x0A0A10, False)] + eyes(8, 3, 0xC070FF, gap=2, size=(1, 1))),
        Cube(-1, 32, -3, 2, 3, 6, 'knight_cloth'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (5, 22, 0), [Cube(4, 12, -2, 3, 11, 4, 'knight_armor')],
               anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 1.8), A('cast', 'x', -2.2)])
    arm_r = mirror(arm, 'arm_r')
    sword = Part('sword', 'arm_r', (-6, 13, 0), [
        Cube(-6, 12, -10, 1, 2, 10, 'axe_metal'), Cube(-7, 12, -2, 3, 2, 1, 'gold'),
    ])
    shield = Part('shield', 'arm_l', (7, 16, 0), [Cube(7, 12, -4, 1, 9, 8, 'knight_armor', decals=[('east', 3, 3, 2, 3, 0x8E44AD, True)])])
    return dict(name='grave_knight', scale=1.0, shadow=0.5, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, sword, shield])


def _rat(name, fur, eye, scale, crown=False):
    body = Part('body', 'root', (0, 3, 0), [
        Cube(-2, 1, -4, 4, 3, 8, fur, decals=[('top', 1, 2, 2, 4, 0x2A2220, False)]),
    ], anim=[A('idle', 'x', 0.04, 0.3)])
    head_cubes = [
        Cube(-2, 1, -8, 4, 3, 4, fur, decals=eyes(4, 0, eye, gap=2, size=(1, 1), glow=crown)),
        Cube(-1, 1, -10, 2, 2, 2, fur, decals=[('front', 0, 0, 2, 1, 0xE08A9A, False)]),
        Cube(-3, 4, -6, 2, 2, 1, 'rat_skin'), Cube(1, 4, -6, 2, 2, 1, 'rat_skin'),
    ]
    if crown:
        head_cubes += [Cube(-2, 4, -8, 4, 1, 3, 'gold'), Cube(-2, 5, -8, 1, 1, 1, 'gold'), Cube(1, 5, -8, 1, 1, 1, 'gold'),
                       Cube(-1, 5, -6, 2, 1, 1, 'gold')]
    head = Part('head', 'body', (0, 3, -4), head_cubes, anim=[A('look', 'y'), A('look', 'x', 0.5), A('attack', 'x', 0.6)])
    tail = Part('tail', 'body', (0, 3, 4), [Cube(0, 2, 4, 1, 1, 9, 'rat_skin')], rot=(-10, 0, 0),
                anim=[A('idle', 'y', 0.5, 0.25), A('walk', 'y', 0.4, 1.2)])
    parts = [body, head, tail]
    for i, z in enumerate((-3, 2)):
        leg = Part(f'leg_l{i}', 'body', (2, 1, z), [Cube(1, 0, z, 1, 1, 2, 'rat_skin')], anim=[A('walk', 'x', 1.0, phase=i * math.pi)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return dict(name=name, scale=scale, shadow=0.3 * scale, parts=parts)


def rat():
    return _rat('rat', 'rat_fur', 0x1A0A0A, 1.0)


def rat_king():
    return _rat('rat_king', 'rat_king_fur', 0xFF3020, 2.1, crown=True)


# ======================= хранители больших подземелий (2.16) =======================

def bronzebeard():
    """Бронзобород — механический страж дварфов: печь в груди, трубы с паром, молот-кузнец."""
    leg = Part('leg_l', 'root', (6, 14, 0), [
        Cube(2, 6, -4, 8, 9, 8, 'bronze'),
        Cube(1, 0, -6, 10, 6, 11, 'bronze_dark', decals=[('front', 1, 1, 8, 1, 0xFFB040, True)]),
    ], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-10, 14, -6, 20, 7, 12, 'bronze_dark'),
        Cube(-13, 21, -8, 26, 16, 15, 'bronze', decals=[
            ('front', 9, 4, 8, 7, 0x2A1A10, False), ('front', 10, 5, 6, 5, 0xFF7A20, True), ('front', 11, 6, 4, 3, 0xFFE070, True),
            ('front', 2, 2, 3, 3, 0xD8A040, False), ('front', 21, 2, 3, 3, 0xD8A040, False)]),
        Cube(-9, 37, 3, 4, 10, 4, 'bronze_dark'), Cube(5, 37, 3, 4, 10, 4, 'bronze_dark'),
        Cube(-9, 47, 3, 4, 2, 4, 'steam'), Cube(5, 47, 3, 4, 2, 4, 'steam'),
    ], anim=[A('idle', 'x', 0.02, 0.1)])
    head = Part('head', 'body', (0, 37, -3), [
        Cube(-5, 36, -10, 10, 8, 9, 'bronze', decals=[('front', 2, 3, 6, 1, 0x60E8FF, True)]),
        Cube(-7, 30, -11, 14, 7, 3, 'beard_metal'),
        Cube(-5, 26, -10, 10, 4, 2, 'beard_metal'),
        Cube(-6, 44, -8, 12, 2, 7, 'bronze_dark'),
    ], anim=[A('look', 'y', 0.7), A('look', 'x', 0.4)])
    arm = Part('arm_l', 'body', (16, 35, 0), [
        Cube(13, 22, -5, 8, 14, 10, 'bronze'),
        Cube(12, 13, -6, 10, 9, 12, 'bronze_dark', decals=[('front', 2, 3, 6, 2, 0xFFB040, True)]),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 1.8), A('cast', 'x', -2.6), A('cast2', 'z', -1.0)])
    arm_r = mirror(arm, 'arm_r')
    hammer = Part('hammer', 'arm_r', (-17, 14, 0), [
        Cube(-18, -6, -1, 2, 24, 2, 'wood'),
        Cube(-23, -14, -5, 12, 9, 10, 'bronze_dark', decals=[('front', 4, 3, 4, 3, 0xFF8A30, True)]),
    ])
    return dict(name='bronzebeard', scale=1.15, shadow=1.3, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, hammer])


def ghaz():
    """Гхаз, Глубинный Червь — вставшее на дыбы тело из колец, пасть с тремя рядами зубов."""
    s0 = Part('seg0', 'root', (0, 6, 6), [Cube(-10, 0, 0, 20, 12, 14, 'worm_skin')], anim=[A('idle', 'y', 0.06, 0.08)])
    s1 = Part('seg1', 'seg0', (0, 10, 2), [Cube(-9, 10, -4, 18, 12, 14, 'worm_skin')], rot=(-25, 0, 0), anim=[A('idle', 'z', 0.08, 0.1)])
    s2 = Part('seg2', 'seg1', (0, 20, -2), [Cube(-8, 20, -8, 16, 12, 13, 'worm_plate')], rot=(-15, 0, 0), anim=[A('idle', 'z', -0.1, 0.1, 1)])
    head = Part('head', 'seg2', (0, 30, -6), [
        Cube(-9, 30, -16, 18, 14, 14, 'worm_skin', decals=[('front', 3, 3, 12, 8, 0x3A0A0E, False), ('front', 5, 5, 8, 4, 0x8A1A1A, True)]
             + [('front', 3 + i * 2, 10, 1, 2, 0xE8E0C8, False) for i in range(6)] + [('front', 3 + i * 2, 2, 1, 2, 0xE8E0C8, False) for i in range(6)]),
        Cube(-11, 40, -12, 4, 6, 4, 'worm_plate'), Cube(7, 40, -12, 4, 6, 4, 'worm_plate'),
        Cube(-3, 44, -10, 6, 4, 6, 'worm_plate'),
    ], anim=[A('look', 'y', 0.5), A('look', 'x', 0.4), A('attack', 'x', 0.8), A('cast', 'x', -0.6)])
    tail = Part('tail', 'seg0', (0, 4, 18), [Cube(-7, 0, 14, 14, 9, 14, 'worm_skin'), Cube(-4, 0, 28, 8, 6, 8, 'worm_plate')],
                anim=[A('idle', 'y', 0.25, 0.12)])
    return dict(name='ghaz', scale=1.3, shadow=1.6, parts=[s0, s1, s2, head, tail])


def torbald():
    """Торбальд Проклятый, Король-под-Горой: дух дварфа в проклятом золоте, корона, двуручная секира."""
    leg = Part('leg_l', 'root', (4, 10, 0), [Cube(1, 0, -4, 6, 10, 8, 'king_armor')], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-9, 10, -6, 18, 16, 12, 'king_armor', decals=[('front', 7, 4, 4, 4, 0x80FFB0, True)]),
        Cube(-10, 8, -7, 20, 4, 14, 'gold'),
        Cube(-12, 20, -2, 24, 8, 10, 'cape_green'),
    ], anim=[A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 26, -2), [
        Cube(-5, 26, -8, 10, 9, 9, 'ghost_skin', decals=eyes(10, 4, 0x80FFB0, gap=2, size=(2, 1))),
        Cube(-6, 17, -9, 12, 10, 3, 'beard_ghost'),
        Cube(-6, 35, -8, 12, 3, 10, 'gold', decals=[('front', 2, 0, 1, 2, 0x40FF80, True), ('front', 9, 0, 1, 2, 0x40FF80, True)]),
        Cube(-6, 38, -8, 2, 3, 2, 'gold'), Cube(4, 38, -8, 2, 3, 2, 'gold'), Cube(-1, 38, -8, 2, 4, 2, 'gold'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (11, 24, 0), [
        Cube(9, 12, -4, 6, 13, 8, 'king_armor'),
        Cube(8, 20, -5, 8, 6, 10, 'gold'),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.4), A('cast2', 'z', -1.0)])
    arm_r = mirror(arm, 'arm_r')
    axe = Part('axe', 'arm_r', (-12, 12, 0), [
        Cube(-13, -4, -1, 2, 30, 2, 'bone_dark'),
        Cube(-21, 16, -1, 8, 10, 2, 'cursed_steel', decals=[('front', 1, 1, 1, 8, 0x80FFB0, True)]),
        Cube(-11, 16, -1, 5, 8, 2, 'cursed_steel'),
    ])
    return dict(name='torbald', scale=1.2, shadow=1.0, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, axe])


def _twin(name, armor, trim, halo, eye, blade):
    leg = Part('leg_l', 'root', (3, 14, 0), [Cube(1, 0, -3, 5, 14, 6, armor)], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-6, 14, -4, 12, 16, 8, armor, decals=[('front', 4, 6, 4, 4, eye, True)]),
        Cube(-7, 12, -5, 14, 3, 10, trim),
        Cube(-8, 26, -5, 16, 4, 10, trim),
    ], anim=[A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 30, 0), [
        Cube(-4, 30, -4, 8, 8, 8, armor, decals=eyes(8, 3, eye, gap=2, size=(2, 1))),
        Cube(-6, 29, 4, 12, 12, 1, halo),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (8, 28, 0), [Cube(6, 14, -2, 4, 14, 4, armor), Cube(6, 24, -3, 5, 4, 6, trim)],
               anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.6), A('cast2', 'z', -1.2)])
    arm_r = mirror(arm, 'arm_r')
    sword = Part('sword', 'arm_r', (-8, 14, 0), [Cube(-9, -10, -1, 2, 24, 1, blade), Cube(-11, 13, -2, 6, 1, 3, trim)])
    return dict(name=name, scale=1.25, shadow=0.8, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, sword])


def sol_twin():
    return _twin('sol_twin', 'sol_armor', 'gold', 'sun_halo', 0xFFF0A0, 'sun_blade')


def luna_twin():
    return _twin('luna_twin', 'luna_armor', 'luna_trim', 'moon_halo', 0xA0E0FF, 'moon_blade')


def chronophage():
    """Хронофаг, Пожиратель Времени: парящий балахон, песочные часы в груди, стрелки-клинки."""
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-7, 2, -5, 14, 20, 10, 'chrono_robe'),
        Cube(-8, 22, -6, 16, 8, 12, 'chrono_robe', decals=[('front', 6, 1, 4, 6, 0xFFE080, True)]),
        Cube(-3, 18, -7, 6, 10, 2, 'hourglass'),
    ], anim=[A('bob', 'y', 1.4, 0.1), A('idle', 'x', 0.04, 0.1)])
    head = Part('head', 'body', (0, 30, 0), [
        Cube(-5, 30, -5, 10, 10, 10, 'chrono_robe', decals=[('front', 2, 2, 6, 6, 0x0A0810, False)] + eyes(10, 5, 0xFFE080, gap=2, size=(1, 1))),
        Cube(-6, 38, -5, 12, 3, 11, 'chrono_robe'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (9, 28, 0), [Cube(7, 12, -2, 4, 16, 4, 'chrono_robe'), Cube(8, -2, -1, 2, 14, 1, 'clock_hand')],
               rot=(-15, 0, 0), anim=[A('idle', 'x', 0.15, 0.1), A('attack', 'x', 1.8), A('cast', 'x', -2.4), A('cast2', 'z', -1.2)])
    ring = Part('ring', 'body', (0, 26, 0), [
        Cube(-13, 25, -1, 2, 2, 2, 'hourglass'), Cube(11, 25, -1, 2, 2, 2, 'hourglass'),
        Cube(-1, 25, -13, 2, 2, 2, 'hourglass'), Cube(-1, 25, 11, 2, 2, 2, 'hourglass'),
    ], anim=[A('spin', 'y', 1.0, 0.06)])
    return dict(name='chronophage', scale=1.3, shadow=0.6, parts=[body, head, arm, mirror(arm, 'arm_r'), ring])


def xalathar():
    """Ксал'Атар, Глаз Бездны: парящий глаз, веко, щупальца."""
    eye = Part('eye', 'root', (0, 30, 0), [
        Cube(-12, 18, -12, 24, 24, 24, 'void_flesh', decals=[
            ('front', 5, 5, 14, 14, 0xE8E0D0, False), ('front', 8, 8, 8, 8, 0xC02040, True), ('front', 10, 10, 4, 4, 0x100008, False),
            ('front', 11, 11, 1, 1, 0xFFFFFF, True)]),
        Cube(-14, 38, -10, 28, 4, 20, 'void_plate'),
        Cube(-14, 16, -10, 28, 4, 20, 'void_plate'),
    ], anim=[A('bob', 'y', 2.0, 0.08), A('look', 'y', 0.8), A('look', 'x', 0.6), A('cast', 'x', -0.3)])
    parts = [eye]
    for i, (x, z) in enumerate(((-8, -6), (8, -6), (-8, 6), (8, 6), (0, 9))):
        t = Part(f'tent{i}', 'eye', (x, 18, z), [
            Cube(x - 2, 6, z - 2, 4, 12, 4, 'void_flesh'),
            Cube(x - 1, -4, z - 1, 3, 10, 3, 'void_flesh', decals=[('front', 1, 2, 1, 1, 0xC060FF, True)]),
        ], anim=[A('idle', 'x', 0.35, 0.12, i * 1.3), A('idle', 'z', 0.25, 0.1, i * 0.7)])
        parts.append(t)
    return dict(name='xalathar', scale=1.4, shadow=1.0, parts=parts)


RAID_MATS = {
    'bronze': m_metal(0xB07A3A, 0xE8B870, 0x6A4418),
    'bronze_dark': m_metal(0x7A5228, 0xB07A3A, 0x3E2A12),
    'beard_metal': m_hair(0xC88A40, 0x7A5228),
    'steam': m_glow(0xE8F0F8, 0x8A98A8),
    'worm_skin': m_skin(0x8A6A5A, 0x5A3E34, spots=0x3E2A22, spot_chance=0.2),
    'worm_plate': m_chitin(0x5A4A40, 0xA88A70, band=3),
    'king_armor': m_metal(0x3E4A44, 0x7A9A88, 0x1E2622),
    'ghost_skin': m_glow(0x8AE0B0, 0x3A8A6A),
    'beard_ghost': m_hair(0xB0F0D0, 0x5AA088),
    'cape_green': m_cloth(0x1E4A30, 0x0E2418, tatter=True),
    'cursed_steel': m_metal(0x4A5A54, 0x9AD8B8, 0x1E2A26, rivets=False),
    'sol_armor': m_metal(0xE8C060, 0xFFF4C0, 0x9A7020, rivets=False),
    'sun_halo': m_glow(0xFFE070, 0xFF9A30),
    'sun_blade': m_glow(0xFFF8D0, 0xFFC040),
    'luna_armor': m_metal(0xA8B8D0, 0xE8F0FF, 0x5A6A88, rivets=False),
    'luna_trim': m_metal(0x5A6A9A, 0xA8B8E8, 0x2A3458, rivets=False),
    'moon_halo': m_glow(0xD0E8FF, 0x6A9AE0),
    'moon_blade': m_glow(0xE0F4FF, 0x80B8FF),
    'chrono_robe': m_cloth(0x3A2E4A, 0x1E1628, tatter=True),
    'hourglass': m_glow(0xFFE8A0, 0xC89A40),
    'clock_hand': m_metal(0xD8C890, 0xFFF8D0, 0x8A7A4A, rivets=False),
    'void_flesh': m_skin(0x3A1A4A, 0x1E0A28, spots=0x6A2A8A, spot_chance=0.18),
    'void_plate': m_chitin(0x1A1024, 0x5A3A7A, band=4),
}
MATS.update(RAID_MATS)


MODELS = [ruin_golem, rotting_ent, spider_queen, swamp_hag, minotaur, frost_giant, flame_lord, archdemon,
          stone_troll, frost_wraith, forest_wisp, ash_imp, bog_lurker, sand_scorpion, grave_knight, rat, rat_king,
          bronzebeard, ghaz, torbald, sol_twin, luna_twin, chronophage, xalathar]


# ======================= развёртка и рисование =======================

def pack(cubes, width):
    """Раскладка кубов по полкам. Возвращает высоту текстуры."""
    order = sorted(cubes, key=lambda c: -(c.d + c.h))
    x = y = shelf = 0
    for c in order:
        cw, ch = 2 * (c.d + c.w), c.d + c.h
        if x + cw > width:
            x, y, shelf = 0, y + shelf, 0
        c.uv = (x, y)
        x += cw
        shelf = max(shelf, ch)
    h = y + shelf
    size = 32
    while size < h:
        size *= 2
    return size


def face_rects(c):
    u, v = c.uv
    w, h, d = c.w, c.h, c.d
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'west': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'east': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h),
    }


def paint(model, W, H):
    img = [[(0, 0, 0, 0)] * W for _ in range(H)]
    glow = [[(0, 0, 0, 0)] * W for _ in range(H)]
    seed = 1
    for part in model['parts']:
        for c in part.cubes:
            seed += 17
            mat = MATS[c.mat]
            for face, (fx, fy, fw, fh) in face_rects(c).items():
                shade = FACE_SHADE[face]
                for py in range(fh):
                    for px in range(fw):
                        col, g = mat(px, py, fw, fh, face, seed + FACE_SEED[face])
                        if col[3] == 0:
                            continue
                        f = shade
                        if face not in ('top', 'bottom'):
                            f *= 1.06 - 0.16 * py / max(1, fh - 1)
                        edge = px in (0, fw - 1) or py in (0, fh - 1)
                        if edge and not g:
                            f *= 0.8
                        rgb_ = mul(col[:3], f) if not g else col[:3]
                        img[fy + py][fx + px] = rgb_ + (255,)
                        if g:
                            glow[fy + py][fx + px] = col[:3] + (255,)
                for (dface, dx, dy, dw, dh, color, dg) in c.decals:
                    if dface != face:
                        continue
                    for yy in range(dy, dy + dh):
                        for xx in range(dx, dx + dw):
                            if 0 <= xx < fw and 0 <= yy < fh:
                                col = rgb(color)
                                img[fy + yy][fx + xx] = (col if dg else mul(col, shade)) + (255,)
                                if dg:
                                    glow[fy + yy][fx + xx] = col + (255,)
    return img, glow


# ======================= экспорт для Java =======================

def to_java(model, W, H):
    parts_abs = {p.name: p for p in model['parts']}
    out = []
    for p in model['parts']:
        px, py, pz = p.pivot
        jp = (px, 24 - py, pz)
        if p.parent == 'root':
            rel = jp
        else:
            q = parts_abs[p.parent]
            qj = (q.pivot[0], 24 - q.pivot[1], q.pivot[2])
            rel = (jp[0] - qj[0], jp[1] - qj[1], jp[2] - qj[2])
        cubes = []
        for c in p.cubes:
            cubes.append({'uv': list(c.uv), 'o': [c.x - px, py - c.y - c.h, c.z - pz], 's': [c.w, c.h, c.d],
                          **({'infl': c.infl} if c.infl else {})})
        e = {'name': p.name, 'parent': p.parent, 'pivot': list(rel),
             'rot': [round(math.radians(r), 4) for r in p.rot], 'cubes': cubes}
        if p.anim:
            e['anim'] = [{k: (round(v, 4) if isinstance(v, float) else v) for k, v in a.items() if k != 'flip'} for a in p.anim]
        out.append(e)
    return {'tex': [W, H], 'scale': model['scale'], 'shadow': model['shadow'], 'parts': out}


# ======================= превью (простая 3D-отрисовка) =======================

def rot_matrix(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    X = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Y = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Z = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]

    def mm(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mm(mm(Z, Y), X)


def apply(m, v):
    return [m[0][0] * v[0] + m[0][1] * v[1] + m[0][2] * v[2],
            m[1][0] * v[0] + m[1][1] * v[1] + m[1][2] * v[2],
            m[2][0] * v[0] + m[2][1] * v[1] + m[2][2] * v[2]]


def preview(model, img, out, size=300, yaw=-35, pitch=15):
    """Рисует модель в координатах Java (y вниз) с поворотами частей, точками-текселями с z-буфером."""
    jm = to_java(model, len(img[0]), len(img))
    parts = {p['name']: p for p in jm['parts']}

    def world(pname):
        p = parts[pname]
        m = rot_matrix(*p['rot'])
        if p['parent'] == 'root':
            return p['pivot'], m
        pt, pr = world(p['parent'])
        R = [[sum(pr[i][k] * m[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
        t = apply(pr, p['pivot'])
        return [pt[0] + t[0], pt[1] + t[1], pt[2] + t[2]], R

    view = rot_matrix(math.radians(pitch), math.radians(yaw), 0)
    zbuf = {}
    pts = []
    for pname, p in parts.items():
        T, R = world(pname)
        for c in p['cubes']:
            ox, oy, oz = c['o']
            w, h, d = c['s']
            u, v = c['uv']
            faces = {
                'front': (u + d, v + d, w, h, lambda a, b: (ox + w - a, oy + b, oz)),
                'back': (u + 2 * d + w, v + d, w, h, lambda a, b: (ox + a, oy + b, oz + d)),
                'west': (u, v + d, d, h, lambda a, b: (ox + w, oy + b, oz + d - a)),
                'east': (u + d + w, v + d, d, h, lambda a, b: (ox, oy + b, oz + a)),
                'top': (u + d, v, w, d, lambda a, b: (ox + w - a, oy, oz + b)),
            }
            for name, (fu, fv, fw, fh, pos) in faces.items():
                for b in range(fh * 2):
                    for a in range(fw * 2):
                        col = img[fv + b // 2][fu + a // 2]
                        if col[3] == 0:
                            continue
                        lp = pos(a / 2 + 0.25, b / 2 + 0.25)
                        wp = apply(R, lp)
                        wp = [wp[0] + T[0], wp[1] + T[1], wp[2] + T[2]]
                        # модель в мире отражена по x и y (как в игре)
                        vp = apply(view, [-wp[0], -wp[1] + 24, wp[2]])
                        pts.append((vp, col))
    if not pts:
        return
    xs = [p[0][0] for p in pts]
    ys = [p[0][1] for p in pts]
    span = max(max(xs) - min(xs), max(ys) - min(ys), 1)
    k = (size - 20) / span
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    canvas = [[(36, 40, 48, 255)] * size for _ in range(size)]
    for vp, col in pts:
        sx = int(size / 2 + (vp[0] - cx) * k)
        sy = int(size / 2 - (vp[1] - cy) * k)
        z = vp[2]
        r = max(1, int(k * 0.5))
        for yy in range(sy - r, sy + r):
            for xx in range(sx - r, sx + r):
                if 0 <= xx < size and 0 <= yy < size and zbuf.get((xx, yy), 1e9) > z:
                    zbuf[(xx, yy)] = z
                    canvas[yy][xx] = col
    out.append(canvas)


# ======================= прочие текстуры =======================

def item_spark():
    img = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = math.hypot(dx, dy)
            star = abs(dx) * abs(dy) < 2.2 and r < 7.5
            if r < 3.2 or star:
                t = min(1, r / 7.5)
                c = mix((255, 255, 240), (255, 70, 60), t)
                img[y][x] = c + (255,)
            elif r < 4.6 and (x + y) % 2 == 0:
                img[y][x] = (255, 160, 90, 200)
    return img


def altar_textures():
    side = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    top = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    bottom = [[(0, 0, 0, 0)] * 16 for _ in range(16)]
    base, dark, light = (44, 40, 52), (24, 20, 30), (74, 68, 86)
    rune = (120, 220, 255)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 5)
            c = mix(dark, base, n * 1.5) if n < 0.6 else mix(base, light, (n - 0.6) * 2)
            if y in (0, 4, 15) or x in (0, 15):
                c = mul(c, 0.7)
            side[y][x] = c + (255,)
            bottom[y][x] = mul(c, 0.8) + (255,)
            r = math.hypot(x - 7.5, y - 7.5)
            tc = mix(dark, base, h2(x, y, 9))
            if 5.5 < r < 6.6 or 2.2 < r < 3.1:
                tc = rune
            elif abs(x - 7.5) < 0.6 and r < 5.5 or abs(y - 7.5) < 0.6 and r < 5.5:
                tc = mix(rune, (40, 120, 200), 0.4)
            elif x in (0, 15) or y in (0, 15):
                tc = light
            top[y][x] = tc + (255,)
    # руны на боках
    glyphs = [(3, 7), (4, 8), (5, 7), (4, 9), (4, 10), (10, 7), (11, 8), (12, 9), (10, 10), (12, 7), (7, 8), (8, 9), (7, 10), (8, 7)]
    for x, y in glyphs:
        side[y][x] = rune + (255,)
    for x in range(1, 15):
        side[5][x] = (60, 150, 200, 255) if x % 3 else light + (255,)
    return side, top, bottom


# ======================= названия =======================

NAMES = {
    'ruin_golem': ('Голем Руин', 'Ruin Golem'),
    'rotting_ent': ('Гниющий Энт', 'Rotting Ent'),
    'spider_queen': ('Королева Арахна', 'Queen Arachna'),
    'swamp_hag': ('Морра, Болотная ведьма', 'Morra the Swamp Hag'),
    'minotaur': ('Астерион, Минотавр', 'Asterion the Minotaur'),
    'frost_giant': ('Имир, Ледяной великан', 'Ymir the Frost Giant'),
    'flame_lord': ('Игнис, Владыка пламени', 'Ignis the Flame Lord'),
    'archdemon': ("Баал'Зар, Архидемон", "Baal'zar the Archdemon"),
    'stone_troll': ('Каменный тролль', 'Stone Troll'),
    'frost_wraith': ('Ледяной призрак', 'Frost Wraith'),
    'forest_wisp': ('Лесной огонёк', 'Forest Wisp'),
    'ash_imp': ('Пепельный бес', 'Ash Imp'),
    'bog_lurker': ('Болотный ползун', 'Bog Lurker'),
    'sand_scorpion': ('Песчаный скорпион', 'Sand Scorpion'),
    'grave_knight': ('Могильный рыцарь', 'Grave Knight'),
    'rat': ('Крыса', 'Rat'),
    'rat_king': ('Крысиный король', 'Rat King'),
    'bronzebeard': ('Бронзобород, Механический страж', 'Bronzebeard the Construct'),
    'ghaz': ('Гхаз, Глубинный Червь', 'Ghaz the Deep Worm'),
    'torbald': ('Торбальд Проклятый, Король-под-Горой', 'Torbald the Accursed'),
    'sol_twin': ('Сол, Близнец Рассвета', 'Sol, Twin of Dawn'),
    'luna_twin': ('Луна, Близнец Сумерек', 'Luna, Twin of Dusk'),
    'chronophage': ('Хронофаг, Пожиратель Времени', 'Chronophage the Time-Eater'),
    'xalathar': ("Ксал'Атар, Глаз Бездны", "Xal'athar, Eye of the Abyss"),
}


TITLES = [
    ('boss_hunter', 'Охотник на хранителей', 'Победить 5 хранителей подземелий', 'Guardian Hunter'),
    ('dungeon_master', 'Покоритель подземелий', 'Победить 30 хранителей подземелий', 'Dungeon Master'),
    ('demon_slayer', 'Убийца Демонов', "Победить Архидемона Баал'Зара", 'Demon Slayer'),
    ('helper', 'Помощник', 'Выполнить 10 заданий', 'Helper'),
    ('hero_of_peoples', 'Герой народов', 'Выполнить 75 заданий', 'Hero of the Peoples'),
    ('chosen_one', 'Избранный Системы', 'Пройти сюжет «Путь Избранного»', 'Chosen of the System'),
]


def main():
    model_dir = os.path.join(ASSETS, 'boss_models')
    tex_dir = os.path.join(ASSETS, 'textures', 'entity', 'boss')
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)
    previews = []
    for fn in MODELS:
        m = fn()
        cubes = [c for p in m['parts'] for c in p.cubes]
        W = 128
        H = pack(cubes, W)
        if H > 128:
            W = 256
            H = pack(cubes, W)
        img, glow = paint(m, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '.png'), img, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '_glow.png'), glow, W, H)
        json.dump(to_java(m, W, H), open(os.path.join(model_dir, m['name'] + '.json'), 'w'), indent=1)
        print(f"{m['name']}: {len(cubes)} cubes, texture {W}x{H}")
        if '--preview' in sys.argv:
            preview(m, img, previews)

    # предметы и блок
    item_dir = os.path.join(ASSETS, 'textures', 'item')
    write_png(os.path.join(item_dir, 'divine_spark.png'), item_spark(), 16, 16)
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "litrpg:item/divine_spark"}},
              open(os.path.join(ASSETS, 'models', 'item', 'divine_spark.json'), 'w'))
    block_dir = os.path.join(ASSETS, 'textures', 'block')
    os.makedirs(block_dir, exist_ok=True)
    side, top, bottom = altar_textures()
    write_png(os.path.join(block_dir, 'boss_altar_side.png'), side, 16, 16)
    write_png(os.path.join(block_dir, 'boss_altar_top.png'), top, 16, 16)
    write_png(os.path.join(block_dir, 'boss_altar_bottom.png'), bottom, 16, 16)
    os.makedirs(os.path.join(ASSETS, 'models', 'block'), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, 'blockstates'), exist_ok=True)
    json.dump({"parent": "minecraft:block/block", "textures": {
        "particle": "litrpg:block/boss_altar_side", "side": "litrpg:block/boss_altar_side",
        "top": "litrpg:block/boss_altar_top", "bottom": "litrpg:block/boss_altar_bottom"},
        "elements": [{"from": [0, 0, 0], "to": [16, 12, 16], "faces": {
            "down": {"uv": [0, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "north": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "north"},
            "south": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "south"},
            "west": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "west"},
            "east": {"uv": [0, 4, 16, 16], "texture": "#side", "cullface": "east"}}}]},
        open(os.path.join(ASSETS, 'models', 'block', 'boss_altar.json'), 'w'), indent=1)
    json.dump({"variants": {"": {"model": "litrpg:block/boss_altar"}}}, open(os.path.join(ASSETS, 'blockstates', 'boss_altar.json'), 'w'))
    json.dump({"parent": "litrpg:block/boss_altar"}, open(os.path.join(ASSETS, 'models', 'item', 'boss_altar.json'), 'w'))
    for name in NAMES:
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(ASSETS, 'models', 'item', name + '_spawn_egg.json'), 'w'))

    # названия
    ru_path, en_path = os.path.join(ASSETS, 'lang', 'ru_ru.json'), os.path.join(ASSETS, 'lang', 'en_us.json')
    ru, en = json.load(open(ru_path, encoding='utf-8')), json.load(open(en_path, encoding='utf-8'))
    for key, (r, e) in NAMES.items():
        ru['entity.litrpg.' + key] = r
        en['entity.litrpg.' + key] = e
        ru['item.litrpg.' + key + '_spawn_egg'] = 'Яйцо призыва: ' + r
        en['item.litrpg.' + key + '_spawn_egg'] = e + ' Spawn Egg'
    ru['entity.litrpg.boss_projectile'] = 'Снаряд босса'
    en['entity.litrpg.boss_projectile'] = 'Boss Projectile'
    ru['block.litrpg.boss_altar'] = 'Печать подземелья'
    en['block.litrpg.boss_altar'] = 'Dungeon Seal'
    ru['item.litrpg.divine_spark'] = 'Искра Божества'
    en['item.litrpg.divine_spark'] = 'Divine Spark'
    ru['item.litrpg.divine_spark.tooltip'] = 'В наковальне: легендарный предмет → божественный. Выпадает только с сильнейших хранителей подземелий.'
    en['item.litrpg.divine_spark.tooltip'] = 'Anvil: legendary item → divine. Dropped only by the mightiest dungeon guardians.'
    ru['item.litrpg.ascension_stone.tooltip'] = 'В наковальне: повышает редкость предмета Системы (до легендарной)'
    en['item.litrpg.ascension_stone.tooltip'] = 'Anvil: raises the rarity of a System item (up to legendary)'
    for key, r, d, e in TITLES:
        ru['title.litrpg.' + key] = r
        ru['title.litrpg.' + key + '.desc'] = d
        en['title.litrpg.' + key] = e
        en['title.litrpg.' + key + '.desc'] = d
    json.dump(ru, open(ru_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

    if previews:
        out = sys.argv[sys.argv.index('--preview') + 1]
        n = len(previews)
        cols = 4
        rows = (n + cols - 1) // cols
        size = len(previews[0])
        sheet = [[(20, 22, 28, 255)] * (cols * size) for _ in range(rows * size)]
        for i, pv in enumerate(previews):
            ox, oy = (i % cols) * size, (i // cols) * size
            for y in range(size):
                sheet[oy + y][ox:ox + size] = pv[y]
        write_png(out, sheet, cols * size, rows * size)
        print('preview ->', out)


if __name__ == '__main__':
    random.seed(7)
    main()
