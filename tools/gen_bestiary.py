"""
Бестиарий Мира Системы: 45 существ со своими моделями, уровнями, повадками и навыками.
Пишет:
  src/main/resources/litrpg_bestiary.json          — данные (читает Java: entity/wild/Species, WildSpawner)
  assets/litrpg/boss_models/<id>.json, textures/entity/boss/<id>.png (+_glow) — модели (рисует client/model/DataModel)
  названия (ru/en), модели яиц призыва
Запуск: python tools/gen_bestiary.py [--preview build/preview/bestiary.png] [--only id,id]

Уровень у каждого вида свой: базовый ±5. Где водится вид — земли (`lands`: id территорий карты,
`wild` — Пограничье, `any` — везде, `nether`), время (`any/day/night`) и место (`surface/under/both`).
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_bosses as GB  # noqa: E402
from gen_bosses import Cube, Part, A, mirror, eyes, MATS  # noqa: E402
from gen_bosses import m_stone, m_bark, m_leaf, m_chitin, m_fur, m_skin, m_metal, m_cloth, m_ice, m_magma, m_fire, m_bone, m_glow, m_hair, m_membrane  # noqa: E402
from gen_bosses import rgb, mix, mul, vnoise, h2, fbm  # noqa: E402
from png import write_png  # noqa: E402

ROOT = GB.ROOT
ASSETS = GB.ASSETS


# ======================= новые материалы =======================

def m_scales(base, dark, light):
    B, D, L = rgb(base), rgb(dark), rgb(light)

    def f(x, y, w, h, face, s):
        # чешуя «черепицей»: ряды полукругов со сдвигом
        row = y // 2
        cx = (x + (row % 2) * 1.5) % 3
        edge = cx < 0.6 or y % 2 == 1 and cx > 2.2
        c = mix(B, L, vnoise(x, y, s, 3) * 0.5)
        if edge:
            c = mix(c, D, 0.55)
        if h2(x, y, s + 5) > 0.94:
            c = mix(c, L, 0.6)
        return c + (255,), False
    return f


def m_spots(base, dark, spot, density=0.22, cell=1.6):
    B, D, P = rgb(base), rgb(dark), rgb(spot)

    def f(x, y, w, h, face, s):
        n = vnoise(x * 2.0, y * 0.7, s, 2.0)
        c = mix(D, B, 0.4 + n * 0.8)
        if vnoise(x, y, s + 21, cell) > 1 - density:
            c = mix(c, P, 0.8)
        return c + (255,), False
    return f


def m_wraps(base, dark, gap=0x2A2018):
    B, D, G = rgb(base), rgb(dark), rgb(gap)

    def f(x, y, w, h, face, s):
        band = (y + (x // 4) % 2) % 3
        c = mix(B, D, vnoise(x, y, s, 3) * 0.6)
        if band == 2:
            c = mul(c, 0.82)
        if h2(x, y, s + 3) > 0.96:
            c = G
        if face not in ('top', 'bottom') and h2(x, y // 3, s + 7) > 0.93:
            return (0, 0, 0, 0), False
        return c + (255,), False
    return f


def m_planks(base, dark, band=0x4A4A52):
    B, D, Bd = rgb(base), rgb(dark), rgb(band)

    def f(x, y, w, h, face, s):
        c = mix(D, B, vnoise(x * 3, y * 0.4, s, 3) * 1.2)
        if y % 4 == 3:
            c = mul(c, 0.7)
        if x in (0, w - 1) or (face in ('front', 'back') and y in (0, h - 1)):
            c = Bd if face != 'top' else mul(Bd, 1.1)
        return c + (255,), False
    return f


def m_straw(base=0xD8B860, dark=0x9A7A30):
    B, D = rgb(base), rgb(dark)

    def f(x, y, w, h, face, s):
        c = mix(D, B, vnoise(x * 4, y * 0.3, s, 2) * 1.3)
        if face not in ('top',) and y >= h - 2 and h2(x, 0, s) < 0.5:
            return (0, 0, 0, 0), False
        return c + (255,), False
    return f


def m_crystal(base, light, deep):
    B, L, Dp = rgb(base), rgb(light), rgb(deep)

    def f(x, y, w, h, face, s):
        t = (x + y) % 6 / 6.0
        c = mix(Dp, B, 0.4 + vnoise(x, y, s, 2) * 0.6)
        if t < 0.17:
            c = mix(c, L, 0.7)
        glow = h2(x, y, s + 4) > 0.82
        return (L if glow else c) + (255,), glow
    return f


def m_mud(base, dark, wet=0x3A4A2A, leaf=0x4E7A30):
    B, D, W, Lf = rgb(base), rgb(dark), rgb(wet), rgb(leaf)

    def f(x, y, w, h, face, s):
        c = mix(D, B, fbm(x, y, s) * 1.3)
        if vnoise(x, y, s + 4, 2.2) > 0.72:
            c = mix(c, W, 0.6)
        if face in ('top',) and h2(x, y, s + 9) > 0.7 or h2(x, y, s + 11) > 0.92:
            c = mix(Lf, mul(Lf, 1.3), h2(x, y, s))
        return c + (255,), False
    return f


def m_warts(base, dark, wart):
    B, D, Wt = rgb(base), rgb(dark), rgb(wart)

    def f(x, y, w, h, face, s):
        c = mix(B, D, vnoise(x, y, s, 3) * 0.9)
        if face == 'bottom':
            c = mix(c, rgb(0xD8D0A0), 0.7)
        if h2(x, y, s + 2) > 0.9:
            c = mix(c, Wt, 0.8)
        return c + (255,), False
    return f


def m_feather(base, dark, tip=None):
    B, D = rgb(base), rgb(dark)
    T = rgb(tip) if tip else mul(B, 1.25)

    def f(x, y, w, h, face, s):
        k = (y * 2 + (x % 3)) % 5
        c = mix(D, B, 0.5 + vnoise(x, y, s, 2) * 0.6)
        if k == 0:
            c = mul(c, 0.75)
        if face not in ('top', 'bottom') and y >= h - 2:
            c = mix(c, T, 0.7)
        return c + (255,), False
    return f


def m_charred(base=0x2A2224, ember=0xFF6A20, amt=0.06):
    B, E = rgb(base), rgb(ember)

    def f(x, y, w, h, face, s):
        n = vnoise(x, y, s, 2.6)
        if abs(n - 0.5) < amt:
            return mix(E, rgb(0xFFD070), 1 - abs(n - 0.5) / amt) + (255,), True
        return mix(B, mul(B, 1.5), h2(x, y, s) * 0.5) + (255,), False
    return f


def m_ghost(base, dark, tatter=True):
    B, D = rgb(base), rgb(dark)

    def f(x, y, w, h, face, s):
        if tatter and face not in ('top', 'bottom') and y >= h - 4 and h2(x, 0, s) * 4 < y - (h - 5):
            return (0, 0, 0, 0), False
        c = mix(B, D, vnoise(x, y * 0.5, s, 3) * 0.8 + y / max(1, h) * 0.3)
        return c + (255,), h2(x, y, s + 5) > 0.97
    return f


NEW_MATS = {
    # олень, кабан
    'deer_fur': m_fur(0x9A6438, 0x6A4022, 0xB88050),
    'deer_belly': m_fur(0xE8DCC0, 0xC8B898),
    'antler': m_bone(0xD8C8A0, 0x8A7450),
    'boar_fur': m_fur(0x4A3628, 0x2A1E16, 0x6A5444),
    'snout': m_skin(0xC88A80, 0x9A6058),
    'tusk': m_bone(0xF0E8D0, 0xB8A880),
    # студень
    'jelly': m_ice(0x7AD86A, 0xC8FFB8, 0x3E8A3A),
    'jelly_dark': m_skin(0x3E8A3A, 0x2A6A2A),
    # пугало
    'straw': m_straw(),
    'sack': m_cloth(0xC8B088, 0x8A7450),
    'scare_cloth': m_cloth(0x5A3A6A, 0x3A2440, tatter=True),
    'pumpkin': m_skin(0xE08A20, 0xB06010),
    'hat_brown': m_cloth(0x4A3A2A, 0x2A2016),
    # жук
    'beetle': m_chitin(0x1E3A34, 0x5AA890, band=5),
    'beetle_horn': m_chitin(0x2A1E14, 0x8A6A40, band=3),
    # волки, медведи, кошки
    'direwolf_fur': m_fur(0x5A5A60, 0x2E2E34, 0x8A8A94),
    'direwolf_dark': m_fur(0x2E2E34, 0x18181C),
    'bear_fur': m_fur(0x6A4A2E, 0x3A2818, 0x8A6A48),
    'panther_fur': m_fur(0x1E1A24, 0x0E0C12, 0x3A3044),
    'leopard_fur': m_spots(0xD8D8D0, 0xA8A8A0, 0x3A3A3A, density=0.25),
    'hyena_fur': m_spots(0xA08A5A, 0x6A5A38, 0x3A2E1E, density=0.3),
    'yeti_fur': m_fur(0xF0F4F8, 0xB8C4D0, 0xFFFFFF),
    'yeti_skin': m_skin(0x7A8AA0, 0x5A6A80),
    'mammoth_fur': m_hair(0x6A4A30, 0x3A2818),
    'mammoth_body': m_fur(0x5A3E28, 0x3A2818, 0x7A5A3E),
    'ivory': m_bone(0xF4ECD8, 0xC8B898),
    'lion_fur': m_fur(0xC88A48, 0x8A5A28, 0xE0A860),
    'lion_mane': m_hair(0x8A4A20, 0x4A2410),
    # летучие
    'bat_fur': m_fur(0x3A2A2A, 0x1E1414),
    'bat_wing': m_membrane(0x4A2A30, 0x241014),
    'griffin_feather': m_feather(0xA87A40, 0x6A4A20, 0xD8B070),
    'griffin_head': m_feather(0xF0EEE8, 0xC0BCB0),
    'beak': m_bone(0xF0C030, 0xB08010),
    'harpy_feather': m_feather(0x6A7A8A, 0x3A4450, 0xA8B4C0),
    'harpy_skin': m_skin(0xC8A890, 0xA08068),
    'vulture_feather': m_feather(0x3A3430, 0x1E1A18, 0x5A524C),
    'vulture_head': m_skin(0xD89090, 0xA86868),
    'wyvern_scale': m_scales(0x3A6A4A, 0x1E3A28, 0x6AA078),
    'wyvern_wing': m_membrane(0x4A6A3A, 0x22341C),
    # нежить
    'wight_robe': m_ghost(0x3A4038, 0x1E221C),
    'wight_skin': m_skin(0x8A9A90, 0x5E6A64),
    'ghoul_skin': m_skin(0x7A8A6A, 0x4E5A44, spots=0x3A4230, spot_chance=0.2),
    'banshee_robe': m_ghost(0xC8D8E0, 0x7A90A0),
    'banshee_hair': m_hair(0xE8F0F4, 0x9AAAB8),
    'necro_robe': m_cloth(0x2A1E34, 0x140E1C, tatter=True),
    'necro_trim': m_metal(0x6A3A8A, 0xB070E0, 0x3A1A4A, rivets=False),
    'mummy_wrap': m_wraps(0xD8C8A0, 0xA89870),
    'skull': m_bone(0xE8E0C8, 0x9A8E70),
    # огр, тролли, великаны
    'ogre_skin': m_skin(0x8A9A5A, 0x5E6E3A, spots=0x4A5A2A, spot_chance=0.15),
    'grim_skin': m_skin(0xC8C0B8, 0x9A928A),
    'kobold_scale': m_scales(0xB8642A, 0x7A3A14, 0xE0904A),
    'leather_brown': m_cloth(0x7A5030, 0x4A3018),
    'cloth_red': m_cloth(0x9A2A2A, 0x5A1414),
    # растения
    'treant_bark': m_bark(0x6A5034, 0x3A2A18, rot=0.1),
    'treant_moss': m_stone(0x6A6A4A, 0x3A3A2A, 0x8A8A60, moss=0.9, cracks=False),
    'leaf_green': m_leaf(0x4E8A34, 0x2E5A20, dead=0x7A8A30, holes=0.25, deadness=0.15),
    'elder_bark': m_bark(0x4A3E30, 0x2A2016, rot=0.3, fungus=0x9AF0FF),
    # кристаллы, магма, лёд
    'crystal': m_crystal(0x9A6AD8, 0xE8D0FF, 0x4A2A80),
    'crystal_rock': m_stone(0x5A5460, 0x3A3640, 0x7A7480, moss=0.0),
    'packed_snow': m_stone(0xE8EEF4, 0xB8C4D0, 0xFFFFFF, cracks=False),
    'charred': m_charred(),
    'salamander': m_scales(0xD8501E, 0x8A2A10, 0xFFA040),
    'demon_skin': m_skin(0x4A2A5A, 0x2A1434, spots=0x8A3AAA, spot_chance=0.1),
    # болото
    'toad_skin': m_warts(0x6A7A3A, 0x4A5A28, 0xA0A060),
    'mire': m_mud(0x4A4030, 0x2A2418),
    'hydra_scale': m_scales(0x2E5A3A, 0x16301E, 0x5A9A6A),
    # пустыня
    'basilisk_scale': m_scales(0x6A7A3A, 0x3A4420, 0xA8B060),
    'worm_plate': m_scales(0xB89A68, 0x6A5434, 0xE0C890),
    'worm_flesh': m_skin(0xA85A50, 0x7A3A34),
    'teeth': m_bone(0xF4F0E0, 0xC8C0A0),
    'horse_coat': m_fur(0x8A5030, 0x5A3018, 0xA86A44),
    'human_skin': m_skin(0xC8946A, 0xA8744A),
    'bronze': m_metal(0xB0803A, 0xF0C070, 0x6A4818),
    # демоны
    'horn_dark': m_bone(0x3A3030, 0x1A1414),
    'goat_fur': m_fur(0x8A8070, 0x5A5244),
    'snake_scale': m_scales(0x4A6A2A, 0x2A3A16, 0x8AAA4A),
    # мимик
    'chest_wood': m_planks(0xA0702A, 0x6A4818),
    'chest_dark': m_planks(0x7A5420, 0x4A3010),
    'tongue': m_skin(0xC8506A, 0x8A2A44),
    'mouth': m_skin(0x5A1422, 0x2A0810),
    'iron': m_metal(0x9AA0A8, 0xE0E4E8, 0x5A6068, rivets=True),
    # вендиго
    'wendigo_fur': m_fur(0x4A4440, 0x22201E, 0x6A6460),
    'bone_dark2': m_bone(0xC8BCA0, 0x6A5E48),
}
MATS.update(NEW_MATS)


# ======================= помощники моделей =======================

def legs4(mat, x, zf, zb, h, w=3, d=3, swing=0.7, parent='root', y0=0, foot=None):
    """Четыре ноги: левые на +x (правые — зеркалом), передняя пара на zf, задняя на zb."""
    parts = []
    for i, z in enumerate((zf, zb)):
        cubes = [Cube(x, y0, z, w, h, d, mat)]
        if foot:
            cubes.append(Cube(x, y0, z - 1, w, 2, d + 1, foot))
        leg = Part(f'leg_l{i}', parent, (x + w / 2, y0 + h, z + d / 2), cubes, anim=[A('walk', 'x', swing, phase=i * math.pi)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return parts


def legs2(mat, x, h, w=4, d=4, swing=0.7, z=-2, foot=None):
    cubes = [Cube(x, 0, z, w, h, d, mat)]
    if foot:
        cubes.append(Cube(x, 0, z - 1, w, 2, d + 1, foot))
    leg = Part('leg_l', 'root', (x + w / 2, h, z + d / 2), cubes, anim=[A('walk', 'x', swing)])
    return [leg, mirror(leg, 'leg_r')]


def arm(mat, x, y_top, length, w=3, d=3, z=-1, parent='body', extra=(), swing=0.6, attack=1.6, cast=-2.4, cast2=None, rot=(0, 0, 0)):
    cubes = [Cube(x, y_top - length, z, w, length, d, mat)] + list(extra)
    anim = [A('walk', 'x', swing, phase=math.pi), A('attack', 'x', attack), A('cast', 'x', cast)]
    if cast2:
        anim.append(A('cast2', 'z', cast2))
    a = Part('arm_l', parent, (x + w / 2, y_top - 1, z + d / 2), cubes, rot=rot, anim=anim)
    return [a, mirror(a, 'arm_r')]


def wings(mat, x, y, z, span, chord, root_mat=None, flap=0.5, speed=0.6, parent='body', thick=1, rot_y=-10):
    cubes = [Cube(x, y - chord, z, span, chord, thick, mat)]
    if root_mat:
        cubes.append(Cube(x, y - 1, z - 1, span, 2, 2, root_mat))
    wg = Part('wing_l', parent, (x, y, z), cubes, rot=(0, rot_y, 0), anim=[A('idle', 'z', flap, speed), A('cast2', 'z', -0.8)])
    return [wg, mirror(wg, 'wing_r')]


def horiz_wings(mat, x, y, z, span, depth, root_mat=None, flap=0.6, speed=0.7, parent='body'):
    """Крылья плашмя (летучая мышь, грифон в полёте): машут вверх-вниз вокруг оси z."""
    cubes = [Cube(x, y, z, span, 1, depth, mat)]
    if root_mat:
        cubes.append(Cube(x, y - 1, z, span, 2, 2, root_mat))
    wg = Part('wing_l', parent, (x, y, z + depth / 2), cubes, anim=[A('idle', 'z', flap, speed)])
    return [wg, mirror(wg, 'wing_r')]


def hood(y, mat):
    """Капюшон вокруг головы 8×8 (низ на высоте y): верх, бока и спина — лицо открыто."""
    return [Cube(-5, y + 9, -5, 10, 1, 10, mat), Cube(-5, y, -5, 1, 9, 10, mat), Cube(4, y, -5, 1, 9, 10, mat),
            Cube(-4, y, 4, 8, 9, 1, mat), Cube(-4, y + 7, -5, 8, 2, 1, mat)]


def M(name, scale, shadow, parts):
    return dict(name=name, scale=scale, shadow=shadow, parts=parts)


# ======================= 1. звери равнин =======================

def deer():
    parts = legs4('deer_fur', 2, -7, 6, 12, w=2, d=2, swing=0.8)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -8, 8, 8, 17, 'deer_fur', decals=[('bottom', 1, 2, 6, 13, 0xE8DCC0, False)]),
        Cube(-1, 17, 9, 2, 3, 2, 'deer_belly'),
    ], anim=[A('idle', 'x', 0.02, 0.1)])
    head = Part('head', 'body', (0, 19, -8), [
        Cube(-2, 16, -11, 4, 9, 4, 'deer_fur'),
        Cube(-3, 23, -16, 6, 5, 7, 'deer_fur', decals=eyes(6, 1, 0x1A1008, gap=4, size=(1, 1), glow=False)),
        Cube(-2, 23, -19, 4, 3, 3, 'deer_fur', decals=[('front', 1, 0, 2, 1, 0x1A1008, False)]),
        Cube(-5, 27, -11, 2, 2, 1, 'deer_fur'), Cube(3, 27, -11, 2, 2, 1, 'deer_fur'),
        Cube(-3, 28, -12, 1, 6, 1, 'antler'), Cube(2, 28, -12, 1, 6, 1, 'antler'),
        Cube(-7, 33, -12, 4, 1, 1, 'antler'), Cube(3, 33, -12, 4, 1, 1, 'antler'),
        Cube(-7, 34, -12, 1, 4, 1, 'antler'), Cube(6, 34, -12, 1, 4, 1, 'antler'),
        Cube(-4, 35, -12, 1, 3, 1, 'antler'), Cube(3, 35, -12, 1, 3, 1, 'antler'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6), A('idle', 'x', 0.06, 0.07)])
    return M('deer', 1.0, 0.6, parts + [body, head])


def boar():
    parts = legs4('boar_fur', 2, -6, 4, 7, w=3, d=3, swing=0.9)
    body = Part('body', 'root', (0, 7, 0), [
        Cube(-5, 7, -8, 10, 9, 16, 'boar_fur'),
        Cube(-1, 16, -8, 2, 2, 12, 'direwolf_dark'),   # щетина по хребту
        Cube(-1, 12, 8, 1, 3, 2, 'boar_fur'),
    ], anim=[A('idle', 'x', 0.02, 0.15)])
    head = Part('head', 'body', (0, 12, -8), [
        Cube(-4, 8, -15, 8, 8, 7, 'boar_fur', decals=eyes(8, 2, 0x200808, gap=4, size=(1, 1), glow=False)),
        Cube(-3, 8, -18, 6, 4, 3, 'snout', decals=[('front', 1, 1, 1, 2, 0x4A2020, False), ('front', 4, 1, 1, 2, 0x4A2020, False)]),
        Cube(-5, 10, -17, 1, 4, 1, 'tusk'), Cube(4, 10, -17, 1, 4, 1, 'tusk'),
        Cube(-5, 14, -17, 1, 1, 2, 'tusk'), Cube(4, 14, -17, 1, 1, 2, 'tusk'),
        Cube(-4, 16, -10, 2, 2, 1, 'boar_fur'), Cube(2, 16, -10, 2, 2, 1, 'boar_fur'),
    ], anim=[A('look', 'y', 0.6), A('look', 'x', 0.4), A('cast', 'x', 0.4)])
    return M('boar', 1.0, 0.6, parts + [body, head])


def jelly():
    body = Part('body', 'root', (0, 0, 0), [
        Cube(-6, 0, -6, 12, 11, 12, 'jelly', decals=eyes(12, 3, 0x0A2A0A, gap=4, size=(2, 2), glow=False) + [('front', 4, 7, 4, 1, 0x0A2A0A, False)]),
        Cube(-3, 2, -3, 6, 6, 6, 'jelly_dark', infl=-0.1),
    ], anim=[A('bob', 'y', 0.6, 0.25)])
    return M('jelly', 1.0, 0.5, [body])


def scarecrow():
    pole = Part('pole', 'root', (0, 0, 0), [Cube(-1, 0, -1, 2, 12, 2, 'wood')], anim=[A('walkabs', 'x', 0.15)])
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -2, 8, 11, 4, 'scare_cloth', decals=[('front', 3, 3, 2, 2, 0xC8B088, False)]),
        Cube(-4, 9, -2, 8, 4, 4, 'straw'),
        Cube(-1, 22, -1, 2, 2, 2, 'wood'),
    ], anim=[A('walk', 'z', 0.12), A('idle', 'z', 0.04, 0.05)])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -4, 8, 8, 8, 'sack', decals=[('front', 1, 2, 2, 2, 0x1A0A00, False), ('front', 5, 2, 2, 2, 0x1A0A00, False),
                                                 ('front', 2, 5, 4, 1, 0x1A0A00, False), ('front', 2, 2, 1, 1, 0xFF8A20, True),
                                                 ('front', 5, 2, 1, 1, 0xFF8A20, True)]),
        Cube(-6, 31, -6, 12, 1, 12, 'hat_brown'), Cube(-3, 32, -3, 6, 4, 6, 'hat_brown'),
    ], anim=[A('look', 'y', 0.5), A('idle', 'z', 0.08, 0.06)])
    arm_l = Part('arm_l', 'body', (4, 22, 0), [Cube(4, 20, -1, 10, 2, 2, 'wood'), Cube(12, 17, -2, 3, 4, 3, 'straw')],
                 anim=[A('attack', 'x', 1.6), A('cast', 'z', 0.6), A('idle', 'z', 0.05, 0.08)])
    return M('scarecrow', 1.0, 0.4, [pole, body, head, arm_l, mirror(arm_l, 'arm_r')])


def stag_beetle():
    body = Part('body', 'root', (0, 5, 0), [
        Cube(-6, 3, -6, 12, 6, 15, 'beetle', decals=[('top', 5, 0, 2, 15, 0x0E1E1A, False)]),
        Cube(-4, 4, -10, 8, 5, 4, 'beetle'),
    ], anim=[A('idle', 'x', 0.02, 0.15)])
    head = Part('head', 'body', (0, 6, -10), [
        Cube(-4, 4, -14, 8, 4, 4, 'beetle_horn', decals=eyes(8, 1, 0xC0F0A0, gap=4, size=(1, 1))),
        Cube(-4, 5, -22, 2, 2, 8, 'beetle_horn'), Cube(2, 5, -22, 2, 2, 8, 'beetle_horn'),
        Cube(-3, 5, -23, 2, 2, 2, 'beetle_horn'), Cube(1, 5, -23, 2, 2, 2, 'beetle_horn'),
        Cube(-5, 7, -18, 1, 3, 1, 'beetle_horn'), Cube(4, 7, -18, 1, 3, 1, 'beetle_horn'),
    ], anim=[A('look', 'y', 0.4), A('cast', 'x', 0.35), A('attack', 'x', -0.5)])
    parts = [body, head]
    for i, z in enumerate((-5, 0, 5)):
        leg = Part(f'leg_l{i}', 'body', (6, 5, z), [Cube(6, 4, z, 6, 1, 1, 'beetle_horn'), Cube(11, 0, z, 1, 5, 1, 'beetle_horn')],
                   anim=[A('walk', 'y', 0.4, 1.5, i * math.pi, flip=False)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return M('stag_beetle', 1.0, 0.6, parts)


def dire_wolf():
    parts = legs4('direwolf_fur', 2, -7, 6, 10, w=3, d=3, swing=0.9)
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-4, 10, -8, 8, 8, 17, 'direwolf_fur', decals=[('bottom', 2, 2, 4, 12, 0x9A9AA0, False)]),
        Cube(-5, 13, -9, 10, 8, 7, 'direwolf_dark'),   # грива
    ], anim=[A('idle', 'x', 0.02, 0.15)])
    head = Part('head', 'body', (0, 16, -9), [
        Cube(-4, 12, -15, 8, 7, 6, 'direwolf_fur', decals=eyes(8, 2, 0xFFD040, gap=4, size=(1, 1))),
        Cube(-2, 12, -19, 4, 4, 4, 'direwolf_fur', decals=[('front', 1, 0, 2, 1, 0x101010, False), ('front', 0, 3, 4, 1, 0xF0F0E0, False)]),
        Cube(-4, 19, -12, 2, 3, 1, 'direwolf_dark'), Cube(2, 19, -12, 2, 3, 1, 'direwolf_dark'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6), A('cast', 'x', -0.7)])
    tail = Part('tail', 'body', (0, 17, 9), [Cube(-1, 10, 9, 3, 8, 3, 'direwolf_fur')], rot=(35, 0, 0), anim=[A('idle', 'z', 0.15, 0.2), A('walk', 'z', 0.2)])
    return M('dire_wolf', 1.0, 0.6, parts + [body, head, tail])


def bloodbat():
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-3, 7, -3, 6, 7, 6, 'bat_fur'),
        Cube(-1, 4, -1, 2, 3, 2, 'bat_fur'),
    ], anim=[A('bob', 'y', 1.0, 0.3)])
    head = Part('head', 'body', (0, 14, 0), [
        Cube(-3, 14, -4, 6, 5, 5, 'bat_fur', decals=eyes(6, 1, 0xFF2020, gap=2, size=(1, 1)) + [('front', 2, 3, 1, 2, 0xF0F0F0, False), ('front', 3, 3, 1, 2, 0xF0F0F0, False)]),
        Cube(-4, 19, -2, 2, 4, 1, 'bat_fur'), Cube(2, 19, -2, 2, 4, 1, 'bat_fur'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    wg = Part('wing_l', 'body', (3, 13, 0), [Cube(3, 4, 0, 14, 9, 1, 'bat_wing'), Cube(3, 12, -1, 14, 1, 2, 'bat_fur')],
              rot=(0, 10, 0), anim=[A('idle', 'y', 0.9, 0.9)])
    return M('bloodbat', 1.0, 0.4, [body, head, wg, mirror(wg, 'wing_r')])


def barrow_wight():
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-5, 0, -3, 10, 14, 6, 'wight_robe'),
        Cube(-5, 14, -3, 10, 12, 6, 'wight_robe', decals=[('front', 4, 2, 2, 6, 0xC0A040, False)]),
        Cube(-6, 24, -3, 12, 3, 6, 'wight_robe'),
    ], anim=[A('idle', 'x', 0.04, 0.08)])
    head = Part('head', 'body', (0, 27, 0), [
        Cube(-4, 27, -4, 8, 8, 8, 'wight_skin', decals=[('front', 1, 2, 6, 4, 0x101814, False)] + eyes(8, 3, 0x70FFC0, gap=2, size=(1, 1))),
        Cube(-5, 26, -5, 1, 7, 10, 'wight_robe'), Cube(4, 26, -5, 1, 7, 10, 'wight_robe'), Cube(-4, 26, 4, 8, 9, 1, 'wight_robe'),
        Cube(-4, 35, -4, 8, 2, 8, 'gold'),
        Cube(-4, 37, -4, 1, 2, 1, 'gold'), Cube(3, 37, -4, 1, 2, 1, 'gold'), Cube(-1, 37, -4, 2, 2, 1, 'gold'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('wight_robe', 5, 26, 13, w=3, d=3, extra=[Cube(5, 11, -1, 3, 2, 3, 'wight_skin')], cast2=-1.2, rot=(-25, 0, 0))
    sword = Part('sword', 'arm_r', (-6, 13, 0), [Cube(-7, 12, -12, 1, 2, 13, 'axe_metal'), Cube(-8, 12, -1, 3, 2, 1, 'bone_dark')])
    return M('barrow_wight', 1.0, 0.5, [body, head] + arms + [sword])


def ogre():
    parts = legs2('ogre_skin', 2, 16, w=7, d=7, swing=0.6, z=-3, foot='leather_dark')
    body = Part('body', 'root', (0, 16, 0), [
        Cube(-9, 14, -6, 18, 10, 12, 'leather_brown'),
        Cube(-10, 22, -6, 20, 14, 12, 'ogre_skin', decals=[('front', 6, 6, 8, 6, 0x6E7E4A, False)]),
        Cube(-11, 33, -4, 22, 5, 9, 'ogre_skin'),
        Cube(-9, 14, -7, 18, 3, 1, 'leather_dark'),
    ], rot=(8, 0, 0), anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 37, -2), [
        Cube(-5, 36, -9, 10, 9, 9, 'ogre_skin', decals=eyes(10, 3, 0xFFE060, gap=4, size=(1, 1)) + [('front', 2, 6, 6, 1, 0x2A2010, False),
                                                                                                 ('front', 3, 1, 4, 1, 0x4A5A2A, False)]),
        Cube(-3, 36, -10, 1, 2, 1, 'tusk'), Cube(2, 36, -10, 1, 2, 1, 'tusk'),
        Cube(-1, 39, -11, 2, 3, 2, 'ogre_skin'),
        Cube(-7, 41, -5, 2, 3, 2, 'ogre_skin'), Cube(5, 41, -5, 2, 3, 2, 'ogre_skin'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('ogre_skin', 10, 36, 20, w=6, d=7, z=-4, extra=[Cube(9, 14, -5, 8, 3, 9, 'leather_brown')], attack=1.9, cast=-2.8)
    club = Part('club', 'arm_r', (-13, 17, 0), [Cube(-14, 16, -16, 3, 3, 16, 'wood'), Cube(-15, 15, -24, 5, 5, 9, 'bark_dark'),
                                                Cube(-16, 17, -21, 1, 1, 1, 'iron'), Cube(-11, 17, -19, 1, 1, 1, 'iron')])
    return M('ogre', 1.0, 1.0, parts + [body, head] + arms + [club])


def griffin():
    parts = legs4('lion_fur', 2, -8, 7, 10, w=3, d=3, swing=0.8)
    # передние лапы — орлиные
    for p in parts[:2]:
        c = p.cubes[0]
        p.cubes = [Cube(c.x, 3, c.z, c.w, c.h - 3, c.d, 'griffin_feather'), Cube(c.x, 0, c.z - 1, c.w, 3, c.d + 1, 'beak')]
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-5, 10, -9, 10, 9, 19, 'lion_fur'),
        Cube(-6, 13, -10, 12, 9, 8, 'griffin_feather'),
    ], anim=[A('idle', 'x', 0.02, 0.12)])
    head = Part('head', 'body', (0, 19, -10), [
        Cube(-4, 18, -16, 8, 8, 7, 'griffin_head', decals=eyes(8, 2, 0xFFB020, gap=4, size=(1, 1))),
        Cube(-2, 19, -20, 4, 4, 4, 'beak'), Cube(-1, 17, -20, 2, 2, 2, 'beak'),
        Cube(-3, 26, -12, 2, 3, 1, 'griffin_head'), Cube(1, 26, -12, 2, 3, 1, 'griffin_head'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6), A('cast', 'x', -0.6)])
    tail = Part('tail', 'body', (0, 17, 10), [Cube(-1, 9, 10, 2, 9, 2, 'lion_fur'), Cube(-2, 6, 10, 4, 4, 3, 'lion_mane')], rot=(30, 0, 0),
                anim=[A('idle', 'z', 0.2, 0.15)])
    wg = wings('griffin_feather', 5, 21, -4, 22, 12, root_mat='griffin_head', flap=0.35, speed=0.15, rot_y=-15)
    return M('griffin', 1.0, 0.9, parts + [body, head, tail] + wg)


# ======================= 2. леса =======================

def weaver_spider():
    body = Part('body', 'root', (0, 7, 0), [
        Cube(-4, 5, -6, 8, 6, 7, 'chitin_leg'),
        Cube(-6, 5, 1, 12, 10, 13, 'chitin_abd', decals=[('top', 4, 3, 4, 6, 0xC8A060, False), ('back', 4, 3, 4, 3, 0xC8A060, False)]),
    ], anim=[A('idle', 'x', 0.03, 0.2)])
    head = Part('head', 'body', (0, 8, -6), [
        Cube(-3, 5, -11, 6, 5, 5, 'chitin_leg', decals=[('front', 1, 1, 1, 1, 0xFF3030, True), ('front', 4, 1, 1, 1, 0xFF3030, True),
                                                        ('front', 2, 0, 2, 1, 0xFF3030, True), ('front', 2, 2, 2, 1, 0xFF3030, True)]),
        Cube(-2, 4, -12, 1, 3, 1, 'bone'), Cube(1, 4, -12, 1, 3, 1, 'bone'),
    ], anim=[A('look', 'y', 0.6), A('look', 'x', 0.4)])
    parts = [body, head]
    for i, z in enumerate((-6, -4, -2, 0)):
        leg = Part(f'leg_l{i}', 'body', (4, 8, z), [Cube(4, 7, z, 10, 2, 2, 'chitin_leg'), Cube(13, 0, z, 2, 9, 2, 'chitin_leg')],
                   rot=(0, (i - 1.5) * 18, 0), anim=[A('walk', 'y', 0.35, 1.4, i * math.pi / 2, flip=False), A('walk', 'z', 0.25, 1.4, i * math.pi / 2 + 1)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    return M('weaver_spider', 1.0, 0.8, parts)


def _treant(name, bark, top_mat, scale, height=26, moss=False):
    parts = legs2(bark, 1, 12, w=5, d=5, swing=0.45, z=-3)
    body_cubes = [
        Cube(-6, 12, -4, 12, height - 12, 8, bark, decals=[('front', 3, 4, 2, 2, 0x1A1008, False), ('front', 7, 4, 2, 2, 0x1A1008, False),
                                                           ('front', 3, 4, 1, 1, 0xC8F070, True), ('front', 8, 4, 1, 1, 0xC8F070, True),
                                                           ('front', 4, 8, 4, 1, 0x1A1008, False)]),
        Cube(-9, height - 2, -6, 18, 10, 12, top_mat),
        Cube(-6, height + 8, -4, 12, 6, 9, top_mat),
        Cube(-3, height + 13, -2, 5, 4, 5, top_mat),
    ]
    if moss:
        body_cubes += [Cube(-7, 18, -5, 3, 6, 10, 'treant_moss'), Cube(-11, height + 2, -2, 3, 6, 4, 'bark_dark'),
                       Cube(8, height + 4, -2, 4, 5, 4, 'bark_dark')]
    body = Part('body', 'root', (0, 12, 0), body_cubes, anim=[A('idle', 'z', 0.03, 0.05), A('walk', 'z', 0.06)])
    arms = arm(bark, 6, height - 2, 16, w=4, d=4, z=-2, extra=[Cube(7, height - 21, -3, 4, 4, 6, 'bark_dark'),
                                                                 Cube(9, height - 23, -4, 1, 3, 1, 'bark_dark'), Cube(7, height - 23, -1, 1, 3, 1, 'bark_dark')],
               cast2=-1.0, rot=(0, 0, -10))
    leaf = Part('leaf_l', 'arm_l', (9, height - 8, 0), [Cube(9, height - 9, -3, 6, 5, 6, top_mat)])
    return M(name, scale, 0.9 * scale, parts + [body] + arms + [leaf, mirror(leaf, 'leaf_r', 'arm_r')])


def treant():
    return _treant('treant', 'treant_bark', 'leaf_green', 1.0)


def elder_treant():
    return _treant('elder_treant', 'elder_bark', 'leaf_rot', 1.35, height=28, moss=True)


def shadow_panther():
    parts = legs4('panther_fur', 2, -7, 6, 9, w=3, d=3, swing=0.9)
    body = Part('body', 'root', (0, 9, 0), [Cube(-4, 9, -8, 8, 7, 17, 'panther_fur')], anim=[A('idle', 'x', 0.02, 0.12)])
    head = Part('head', 'body', (0, 14, -8), [
        Cube(-4, 11, -14, 8, 6, 6, 'panther_fur', decals=eyes(8, 2, 0xB060FF, gap=4, size=(1, 1))),
        Cube(-2, 11, -16, 4, 3, 2, 'panther_fur', decals=[('front', 1, 0, 2, 1, 0x3A2A44, False)]),
        Cube(-4, 17, -11, 2, 2, 1, 'panther_fur'), Cube(2, 17, -11, 2, 2, 1, 'panther_fur'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.5)])
    tail = Part('tail', 'body', (0, 15, 9), [Cube(-1, 14, 9, 2, 2, 14, 'panther_fur')], rot=(-25, 0, 0), anim=[A('idle', 'y', 0.3, 0.12)])
    return M('shadow_panther', 1.0, 0.6, parts + [body, head, tail])


def cave_bear():
    parts = legs4('bear_fur', 3, -9, 6, 10, w=5, d=5, swing=0.6)
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-7, 10, -10, 14, 12, 21, 'bear_fur'),
        Cube(-6, 20, -9, 12, 4, 8, 'bear_fur'),   # горб
    ], anim=[A('idle', 'x', 0.02, 0.08)])
    head = Part('head', 'body', (0, 17, -10), [
        Cube(-5, 13, -18, 10, 9, 8, 'bear_fur', decals=eyes(10, 2, 0x1A0A00, gap=4, size=(1, 1), glow=False)),
        Cube(-3, 13, -22, 6, 5, 4, 'bear_fur', decals=[('front', 2, 0, 2, 1, 0x101010, False), ('front', 1, 4, 4, 1, 0xF0F0E0, False)]),
        Cube(-5, 22, -14, 3, 3, 2, 'bear_fur'), Cube(2, 22, -14, 3, 3, 2, 'bear_fur'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.6)])
    return M('cave_bear', 1.0, 1.0, parts + [body, head])


def kobold():
    parts = legs2('kobold_scale', 1, 7, w=3, d=3, swing=0.9, z=-1)
    body = Part('body', 'root', (0, 7, 0), [
        Cube(-3, 7, -2, 6, 7, 4, 'leather_brown'),
        Cube(-4, 13, -2, 8, 2, 4, 'cloth_red'),
    ], rot=(8, 0, 0))
    head = Part('head', 'body', (0, 15, 0), [
        Cube(-3, 15, -4, 6, 5, 6, 'kobold_scale', decals=eyes(6, 1, 0xFFE040, gap=2, size=(1, 1))),
        Cube(-2, 15, -7, 4, 3, 3, 'kobold_scale', decals=[('front', 0, 2, 4, 1, 0xF0F0E0, False)]),
        Cube(-3, 20, 0, 1, 2, 3, 'horn'), Cube(2, 20, 0, 1, 2, 3, 'horn'),
        Cube(-3, 19, -4, 6, 2, 6, 'iron'), Cube(-1, 21, -2, 2, 1, 2, 'fire'),   # каска со свечой
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('kobold_scale', 3, 14, 7, w=2, d=2, z=-1, cast=-2.6)
    bomb = Part('bomb', 'arm_l', (4, 7, 0), [Cube(3, 4, -2, 3, 3, 3, 'obsidian'), Cube(4, 7, -1, 1, 2, 1, 'fire')])
    tail = Part('tail', 'body', (0, 9, 2), [Cube(-1, 7, 2, 2, 2, 7, 'kobold_scale')], rot=(-20, 0, 0), anim=[A('idle', 'y', 0.3, 0.2)])
    return M('kobold', 1.0, 0.4, parts + [body, head] + arms + [bomb, tail])


def crystal_golem():
    parts = legs2('crystal_rock', 2, 12, w=6, d=6, swing=0.5, z=-3)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-9, 12, -5, 18, 16, 10, 'crystal_rock', decals=[('front', 7, 5, 4, 4, 0xE8D0FF, True)]),
        Cube(-6, 27, 2, 3, 7, 3, 'crystal'), Cube(2, 28, 1, 4, 9, 4, 'crystal'), Cube(-2, 26, 4, 2, 5, 2, 'crystal'),
        Cube(-10, 24, -3, 4, 5, 4, 'crystal'), Cube(7, 23, -2, 3, 6, 3, 'crystal'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 28, -3), [
        Cube(-4, 27, -8, 8, 7, 7, 'crystal_rock', decals=eyes(8, 3, 0xE8D0FF, gap=2, size=(2, 1))),
        Cube(-1, 34, -6, 2, 5, 2, 'crystal'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('crystal_rock', 9, 27, 17, w=6, d=7, z=-3, extra=[Cube(9, 10, -4, 7, 6, 9, 'crystal')], attack=1.8, cast=-2.6)
    return M('crystal_golem', 1.0, 0.9, parts + [body, head] + arms)


# ======================= 3. горы и небо =======================

def harpy():
    parts = legs2('harpy_feather', 1, 10, w=3, d=3, swing=0.6, z=-1, foot='beak')
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-3, 10, -2, 6, 10, 4, 'harpy_skin', decals=[('front', 0, 0, 6, 4, 0x6A7A8A, False)]),
        Cube(-4, 9, -2, 8, 4, 5, 'harpy_feather'),
    ], anim=[A('bob', 'y', 0.8, 0.2)])
    head = Part('head', 'body', (0, 20, 0), [
        Cube(-3, 20, -3, 6, 6, 6, 'harpy_skin', decals=eyes(6, 2, 0xFFE040, gap=2, size=(1, 1)) + [('front', 2, 4, 2, 1, 0x5A2A2A, False)]),
        Cube(-4, 22, -4, 8, 6, 9, 'hair', infl=0.1),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    wg = Part('wing_l', 'body', (3, 19, 1), [Cube(3, 6, 1, 4, 14, 2, 'harpy_feather'), Cube(7, 4, 1, 12, 14, 1, 'harpy_feather')],
              rot=(0, -10, -10), anim=[A('idle', 'z', 0.7, 0.6), A('cast', 'z', -1.0)])
    return M('harpy', 1.0, 0.4, parts + [body, head, wg, mirror(wg, 'wing_r')])


def wyvern():
    leg = Part('leg_l', 'root', (5, 12, 4), [Cube(3, 0, 2, 4, 12, 5, 'wyvern_scale'), Cube(3, 0, 0, 4, 2, 3, 'horn')], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-6, 12, -10, 12, 11, 20, 'wyvern_scale', decals=[('bottom', 3, 2, 6, 16, 0xA8C890, False)]),
        Cube(-1, 23, -8, 2, 2, 16, 'horn'),
    ], anim=[A('bob', 'y', 0.8, 0.12)])
    neck = Part('neck', 'body', (0, 20, -10), [Cube(-3, 18, -18, 6, 6, 9, 'wyvern_scale')], rot=(-25, 0, 0), anim=[A('look', 'y', 0.5)])
    head = Part('head', 'neck', (0, 22, -18), [
        Cube(-4, 19, -27, 8, 6, 9, 'wyvern_scale', decals=eyes(8, 1, 0xFFE020, gap=4, size=(1, 1))),
        Cube(-3, 17, -27, 6, 2, 7, 'wyvern_scale', decals=[('front', 0, 0, 6, 1, 0xF0F0E0, False)]),
        Cube(-4, 25, -20, 1, 2, 5, 'horn'), Cube(3, 25, -20, 1, 2, 5, 'horn'),
    ], rot=(25, 0, 0), anim=[A('look', 'x', 0.5), A('cast', 'x', -0.5)])
    tail = Part('tail', 'body', (0, 16, 10), [Cube(-2, 14, 10, 4, 4, 14, 'wyvern_scale'), Cube(-1, 15, 24, 2, 2, 10, 'wyvern_scale'),
                                             Cube(-3, 15, 33, 6, 2, 4, 'horn')], rot=(-10, 0, 0), anim=[A('idle', 'y', 0.25, 0.12)])
    wg = wings('wyvern_wing', 6, 23, -6, 30, 16, root_mat='wyvern_scale', flap=0.5, speed=0.25, rot_y=-12)
    return M('wyvern', 1.0, 1.1, [leg, mirror(leg, 'leg_r'), body, neck, head, tail] + wg)


# ======================= 4. степь и пустыня =======================

def hyena():
    parts = legs4('hyena_fur', 2, -6, 5, 10, w=2, d=3, swing=0.9)
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-4, 9, -7, 8, 8, 15, 'hyena_fur'),
        Cube(-1, 17, -7, 2, 2, 9, 'direwolf_dark'),
    ], rot=(-8, 0, 0), anim=[A('idle', 'x', 0.02, 0.2)])
    head = Part('head', 'body', (0, 15, -7), [
        Cube(-3, 11, -13, 6, 6, 6, 'hyena_fur', decals=eyes(6, 2, 0xE0C030, gap=2, size=(1, 1))),
        Cube(-2, 11, -16, 4, 3, 3, 'direwolf_dark', decals=[('front', 0, 2, 4, 1, 0xF0F0E0, False)]),
        Cube(-4, 17, -10, 2, 3, 1, 'hyena_fur'), Cube(2, 17, -10, 2, 3, 1, 'hyena_fur'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.6)])
    tail = Part('tail', 'body', (0, 14, 8), [Cube(-1, 8, 8, 2, 6, 2, 'direwolf_dark')], rot=(20, 0, 0), anim=[A('idle', 'z', 0.2, 0.3)])
    return M('hyena', 1.0, 0.5, parts + [body, head, tail])


def vulture():
    parts = legs2('vulture_head', 1, 5, w=2, d=2, swing=0.6, z=-1, foot='beak')
    body = Part('body', 'root', (0, 5, 0), [
        Cube(-4, 5, -4, 8, 9, 9, 'vulture_feather'),
        Cube(-3, 4, 5, 6, 4, 5, 'vulture_feather'),
    ], rot=(20, 0, 0), anim=[A('bob', 'y', 0.6, 0.2)])
    head = Part('head', 'body', (0, 14, -3), [
        Cube(-3, 13, -5, 6, 3, 5, 'griffin_head'),
        Cube(-1, 15, -7, 2, 5, 2, 'vulture_head'),
        Cube(-2, 19, -10, 4, 3, 4, 'vulture_head', decals=eyes(4, 0, 0x1A0A0A, gap=2, size=(1, 1), glow=False)),
        Cube(-1, 18, -12, 2, 3, 2, 'beak'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('idle', 'x', 0.1, 0.1)])
    wg = Part('wing_l', 'body', (4, 13, -2), [Cube(4, 4, -3, 2, 9, 10, 'vulture_feather'), Cube(6, 6, -3, 12, 1, 9, 'vulture_feather')],
              rot=(0, 0, 10), anim=[A('idle', 'z', 0.6, 0.5)])
    return M('vulture', 1.0, 0.4, parts + [body, head, wg, mirror(wg, 'wing_r')])


def centaur():
    parts = legs4('horse_coat', 3, -9, 7, 12, w=3, d=3, swing=0.8, foot='hoof')
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-5, 12, -10, 10, 10, 20, 'horse_coat'),
        Cube(-1, 18, 10, 2, 7, 3, 'hair'),
        Cube(-6, 19, -9, 12, 2, 10, 'leather_brown'),
    ], anim=[A('idle', 'x', 0.02, 0.1)])
    torso = Part('torso', 'body', (0, 22, -7), [
        Cube(-4, 22, -10, 8, 12, 5, 'human_skin'),
        Cube(-5, 26, -11, 10, 9, 7, 'bronze', decals=[('front', 4, 2, 2, 3, 0x6A2A1A, False)]),
    ], anim=[A('walk', 'x', 0.05)])
    head = Part('head', 'torso', (0, 35, -8), [
        Cube(-3, 35, -11, 6, 7, 6, 'human_skin', decals=eyes(6, 3, 0x2A1A0A, gap=2, size=(1, 1), glow=False) + [('front', 1, 5, 4, 1, 0x5A3A20, False)]),
        Cube(-4, 39, -12, 8, 4, 8, 'bronze'), Cube(-1, 43, -10, 2, 2, 6, 'cloth_red'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('human_skin', 4, 34, 11, w=3, d=3, z=-10, parent='torso', cast=-1.6)
    bow = Part('bow', 'arm_l', (5, 24, -9), [Cube(5, 18, -10, 1, 14, 1, 'wood'), Cube(5, 19, -9, 1, 12, 1, 'hair')])
    return M('centaur', 1.0, 0.9, parts + [body, torso, head] + arms + [bow])


def basilisk():
    parts = []
    for i, z in enumerate((-9, -1, 7)):
        leg = Part(f'leg_l{i}', 'root', (6, 6, z), [Cube(5, 0, z - 1, 4, 7, 3, 'basilisk_scale'), Cube(5, 0, z - 3, 4, 2, 2, 'horn')],
                   rot=(0, 0, 0), anim=[A('walk', 'x', 0.7, phase=i * math.pi)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    body = Part('body', 'root', (0, 7, 0), [
        Cube(-6, 6, -12, 12, 9, 24, 'basilisk_scale', decals=[('bottom', 3, 2, 6, 20, 0xC8C890, False)]),
        Cube(-1, 15, -10, 2, 4, 2, 'horn'), Cube(-1, 15, -4, 2, 5, 2, 'horn'), Cube(-1, 15, 2, 2, 4, 2, 'horn'),
    ], anim=[A('idle', 'y', 0.04, 0.1)])
    head = Part('head', 'body', (0, 11, -12), [
        Cube(-4, 7, -21, 8, 7, 9, 'basilisk_scale', decals=[('front', 1, 1, 2, 2, 0xFFE020, True), ('front', 5, 1, 2, 2, 0xFFE020, True),
                                                            ('front', 0, 5, 8, 1, 0x2A1A0A, False)]),
        Cube(-5, 13, -17, 10, 2, 4, 'horn'), Cube(-1, 14, -21, 2, 4, 2, 'horn'),
    ], anim=[A('look', 'y', 0.7), A('look', 'x', 0.4), A('cast', 'x', -0.4)])
    tail = Part('tail', 'body', (0, 10, 12), [Cube(-3, 8, 12, 6, 5, 12, 'basilisk_scale'), Cube(-2, 9, 24, 4, 3, 10, 'basilisk_scale')],
                anim=[A('idle', 'y', 0.25, 0.1), A('walk', 'y', 0.2)])
    return M('basilisk', 1.0, 1.0, parts + [body, head, tail])


def sandworm():
    seg = Part('seg0', 'root', (0, 0, 0), [Cube(-9, 0, -8, 18, 10, 16, 'worm_plate'), Cube(-1, 4, 8, 2, 4, 3, 'horn')], anim=[A('idle', 'z', 0.04, 0.1)])
    seg1 = Part('seg1', 'seg0', (0, 10, 0), [Cube(-8, 10, -7, 16, 10, 14, 'worm_plate'), Cube(-1, 14, 7, 2, 4, 3, 'horn'),
                                             Cube(-8, 9, -7, 16, 1, 14, 'worm_flesh')], rot=(12, 0, 0), anim=[A('idle', 'x', 0.08, 0.1)])
    seg2 = Part('seg2', 'seg1', (0, 20, 0), [Cube(-7, 20, -6, 14, 9, 12, 'worm_plate'), Cube(-1, 23, 6, 2, 4, 3, 'horn'),
                                             Cube(-7, 19, -6, 14, 1, 12, 'worm_flesh')], rot=(18, 0, 0),
                anim=[A('idle', 'x', 0.1, 0.1, 0.8), A('cast', 'x', -0.4)])
    head = Part('head', 'seg2', (0, 29, 0), [
        Cube(-7, 29, -6, 14, 6, 12, 'worm_flesh'),
        Cube(-8, 35, -7, 4, 6, 14, 'worm_plate'), Cube(4, 35, -7, 4, 6, 14, 'worm_plate'),
        Cube(-4, 35, -7, 8, 6, 4, 'worm_plate'), Cube(-4, 35, 3, 8, 6, 4, 'worm_plate'),
        Cube(-4, 33, -4, 8, 2, 8, 'mouth'),
        Cube(-3, 38, -4, 1, 4, 1, 'teeth'), Cube(2, 38, -4, 1, 4, 1, 'teeth'), Cube(-3, 38, 3, 1, 4, 1, 'teeth'), Cube(2, 38, 3, 1, 4, 1, 'teeth'),
        Cube(-1, 39, -5, 2, 3, 1, 'teeth'), Cube(-1, 39, 4, 2, 3, 1, 'teeth'),
    ], rot=(25, 0, 0), anim=[A('look', 'y', 0.5), A('idle', 'x', 0.1, 0.1, 1.6), A('attack', 'x', -0.6)])
    return M('sandworm', 1.0, 1.2, [seg, seg1, seg2, head])


def manticore():
    parts = legs4('lion_fur', 3, -9, 7, 11, w=4, d=4, swing=0.8)
    body = Part('body', 'root', (0, 11, 0), [Cube(-6, 11, -10, 12, 10, 21, 'lion_fur')], anim=[A('idle', 'x', 0.02, 0.1)])
    head = Part('head', 'body', (0, 18, -10), [
        Cube(-7, 12, -14, 14, 14, 6, 'lion_mane'),
        Cube(-4, 14, -18, 8, 8, 6, 'human_skin', decals=eyes(8, 2, 0xFF3020, gap=2, size=(2, 1)) + [('front', 1, 5, 6, 1, 0x3A0A0A, False),
                                                                                                    ('front', 2, 6, 1, 1, 0xF0F0E0, False), ('front', 5, 6, 1, 1, 0xF0F0E0, False)]),
        Cube(-6, 26, -14, 2, 3, 2, 'horn'), Cube(4, 26, -14, 2, 3, 2, 'horn'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.5)])
    tail = Part('tail', 'body', (0, 19, 10), [
        Cube(-1, 19, 10, 3, 3, 8, 'scorp_chitin'), Cube(-1, 21, 16, 3, 10, 3, 'scorp_chitin'),
        Cube(-1, 29, 10, 3, 3, 7, 'scorp_dark'), Cube(-3, 28, 5, 7, 5, 6, 'scorp_dark'),
        Cube(-2, 31, 3, 1, 3, 1, 'bone'), Cube(1, 31, 3, 1, 3, 1, 'bone'), Cube(-1, 33, 4, 1, 3, 1, 'bone'), Cube(0, 30, 2, 1, 2, 1, 'bone'),
    ], rot=(-10, 0, 0), anim=[A('idle', 'x', 0.1, 0.15), A('cast2', 'x', -0.6)])
    wg = wings('bat_wing', 6, 22, -4, 22, 13, root_mat='lion_fur', flap=0.3, speed=0.2, rot_y=-20)
    return M('manticore', 1.0, 1.1, parts + [body, head, tail] + wg)


# ======================= 5. север =======================

def snow_leopard():
    parts = legs4('leopard_fur', 2, -7, 6, 9, w=3, d=3, swing=0.9)
    body = Part('body', 'root', (0, 9, 0), [Cube(-4, 9, -8, 8, 7, 17, 'leopard_fur')], anim=[A('idle', 'x', 0.02, 0.12)])
    head = Part('head', 'body', (0, 14, -8), [
        Cube(-4, 11, -14, 8, 6, 6, 'leopard_fur', decals=eyes(8, 2, 0x70D0FF, gap=4, size=(1, 1))),
        Cube(-2, 11, -16, 4, 3, 2, 'yeti_fur', decals=[('front', 1, 0, 2, 1, 0xC07080, False)]),
        Cube(-4, 17, -11, 2, 2, 1, 'leopard_fur'), Cube(2, 17, -11, 2, 2, 1, 'leopard_fur'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.5)])
    tail = Part('tail', 'body', (0, 15, 9), [Cube(-2, 13, 9, 4, 4, 16, 'leopard_fur')], rot=(-15, 0, 0), anim=[A('idle', 'y', 0.3, 0.12)])
    return M('snow_leopard', 1.0, 0.6, parts + [body, head, tail])


def yeti():
    parts = legs2('yeti_fur', 2, 13, w=7, d=7, swing=0.6, z=-3, foot='yeti_skin')
    body = Part('body', 'root', (0, 13, 0), [
        Cube(-9, 13, -6, 18, 20, 12, 'yeti_fur', decals=[('front', 5, 3, 8, 8, 0xC8D4E0, False)]),
        Cube(-10, 28, -6, 20, 7, 13, 'yeti_fur'),
    ], rot=(12, 0, 0), anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 34, -4), [
        Cube(-5, 32, -11, 10, 10, 9, 'yeti_fur'),
        Cube(-4, 33, -12, 8, 6, 1, 'yeti_skin', decals=eyes(8, 1, 0x40C0FF, gap=2, size=(2, 1)) + [('front', 1, 4, 6, 1, 0x1A2A3A, False),
                                                                                                ('front', 1, 5, 1, 1, 0xF0F0F0, False), ('front', 6, 5, 1, 1, 0xF0F0F0, False)]),
        Cube(-7, 40, -6, 3, 4, 3, 'horn'), Cube(4, 40, -6, 3, 4, 3, 'horn'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('yeti_fur', 9, 34, 22, w=6, d=7, z=-4, extra=[Cube(9, 10, -4, 6, 3, 7, 'yeti_skin')], attack=1.8, cast=-2.8)
    return M('yeti', 1.0, 1.0, parts + [body, head] + arms)


def mammoth():
    parts = legs4('mammoth_body', 5, -12, 9, 18, w=7, d=7, swing=0.5, foot='hoof')
    body = Part('body', 'root', (0, 18, 0), [
        Cube(-10, 18, -14, 20, 20, 30, 'mammoth_body'),
        Cube(-11, 14, -15, 22, 8, 32, 'mammoth_fur'),
        Cube(-8, 38, -12, 16, 6, 14, 'mammoth_body'),   # горб
        Cube(-1, 26, 16, 2, 8, 2, 'mammoth_fur'),
    ], anim=[A('idle', 'x', 0.02, 0.06)])
    head = Part('head', 'body', (0, 34, -14), [
        Cube(-7, 26, -24, 14, 16, 11, 'mammoth_body', decals=eyes(14, 6, 0x1A0A00, gap=10, size=(1, 1), glow=False)),
        Cube(-9, 28, -20, 3, 11, 7, 'mammoth_fur'), Cube(6, 28, -20, 3, 11, 7, 'mammoth_fur'),
        Cube(-6, 40, -22, 12, 5, 9, 'mammoth_fur'),
    ], anim=[A('look', 'y', 0.5), A('look', 'x', 0.3), A('cast', 'x', 0.3)])
    trunk = Part('trunk', 'head', (0, 28, -23), [Cube(-2, 16, -26, 4, 12, 4, 'mammoth_body'), Cube(-2, 8, -27, 4, 8, 3, 'mammoth_body')],
                 anim=[A('idle', 'x', 0.15, 0.08), A('attack', 'x', -1.0)])
    tusk = Part('tusk_l', 'head', (5, 28, -22), [Cube(4, 22, -26, 3, 3, 4, 'ivory'), Cube(5, 20, -31, 2, 3, 6, 'ivory'), Cube(4, 22, -36, 2, 3, 5, 'ivory'),
                                                 Cube(3, 25, -38, 2, 3, 3, 'ivory')])
    return M('mammoth', 1.0, 1.6, parts + [body, head, trunk, tusk, mirror(tusk, 'tusk_r')])


def ice_golem():
    parts = legs2('packed_snow', 2, 13, w=7, d=7, swing=0.5, z=-3, foot='ice')
    body = Part('body', 'root', (0, 13, 0), [
        Cube(-10, 13, -6, 20, 18, 12, 'packed_snow', decals=[('front', 7, 5, 6, 6, 0x9ED8F0, True)]),
        Cube(-11, 28, -7, 22, 6, 14, 'ice'),
        Cube(-9, 33, -2, 3, 6, 3, 'ice'), Cube(6, 33, -3, 4, 8, 4, 'ice'), Cube(-2, 34, 2, 3, 5, 3, 'ice'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 33, -4), [
        Cube(-4, 32, -9, 8, 7, 7, 'ice', decals=eyes(8, 3, 0xD8FFFF, gap=2, size=(2, 1))),
        Cube(-1, 39, -7, 2, 4, 2, 'ice'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('packed_snow', 10, 33, 18, w=6, d=7, z=-3, extra=[Cube(10, 13, -4, 7, 6, 9, 'ice')], attack=1.8, cast=-2.6)
    return M('ice_golem', 1.0, 1.0, parts + [body, head] + arms)


def wendigo():
    parts = legs2('wendigo_fur', 2, 18, w=3, d=3, swing=0.7, z=-1, foot='bone_dark2')
    body = Part('body', 'root', (0, 18, 0), [
        Cube(-5, 18, -3, 10, 16, 6, 'wendigo_fur', decals=[('front', 2, 4, 6, 1, 0xC8BCA0, False), ('front', 2, 6, 6, 1, 0xC8BCA0, False),
                                                           ('front', 2, 8, 6, 1, 0xC8BCA0, False)]),
    ], rot=(18, 0, 0), anim=[A('idle', 'x', 0.04, 0.07)])
    head = Part('head', 'body', (0, 34, -1), [
        Cube(-4, 33, -7, 8, 9, 7, 'skull', decals=[('front', 1, 2, 2, 3, 0x000000, False), ('front', 5, 2, 2, 3, 0x000000, False),
                                                   ('front', 2, 3, 1, 1, 0xC0F0FF, True), ('front', 5, 3, 1, 1, 0xC0F0FF, True),
                                                   ('front', 1, 7, 6, 1, 0x2A2A2A, False)]),
        Cube(-4, 42, -4, 1, 6, 1, 'bone_dark2'), Cube(3, 42, -4, 1, 6, 1, 'bone_dark2'),
        Cube(-8, 46, -4, 5, 1, 1, 'bone_dark2'), Cube(3, 46, -4, 5, 1, 1, 'bone_dark2'),
        Cube(-8, 47, -4, 1, 4, 1, 'bone_dark2'), Cube(7, 47, -4, 1, 4, 1, 'bone_dark2'),
        Cube(-5, 48, -4, 1, 3, 1, 'bone_dark2'), Cube(4, 48, -4, 1, 3, 1, 'bone_dark2'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast2', 'x', -0.5)])
    arms = arm('wendigo_fur', 5, 33, 20, w=2, d=3, z=-1, extra=[Cube(5, 11, -2, 1, 3, 1, 'bone_dark2'), Cube(6, 11, 0, 1, 3, 1, 'bone_dark2')],
               cast2=-1.2, rot=(-15, 0, 0))
    return M('wendigo', 1.0, 0.5, parts + [body, head] + arms)


# ======================= 6. пепел и пламя =======================

def hellhound():
    parts = legs4('charred', 2, -7, 6, 10, w=3, d=3, swing=0.9)
    body = Part('body', 'root', (0, 10, 0), [
        Cube(-4, 10, -8, 8, 8, 17, 'charred'),
        Cube(-1, 18, -8, 2, 3, 14, 'fire'),
    ], anim=[A('idle', 'x', 0.02, 0.15)])
    head = Part('head', 'body', (0, 16, -8), [
        Cube(-4, 12, -14, 8, 7, 6, 'charred', decals=eyes(8, 2, 0xFF6020, gap=4, size=(1, 1))),
        Cube(-2, 12, -18, 4, 4, 4, 'charred', decals=[('front', 0, 3, 4, 1, 0xFFA040, True)]),
        Cube(-4, 19, -11, 1, 4, 2, 'horn'), Cube(3, 19, -11, 1, 4, 2, 'horn'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.6), A('cast', 'x', -0.6)])
    tail = Part('tail', 'body', (0, 17, 9), [Cube(-1, 10, 9, 2, 8, 2, 'charred'), Cube(-1, 7, 9, 2, 3, 2, 'fire')], rot=(30, 0, 0),
                anim=[A('idle', 'z', 0.2, 0.25)])
    return M('hellhound', 1.0, 0.6, parts + [body, head, tail])


def salamander():
    parts = []
    for i, z in enumerate((-6, 5)):
        leg = Part(f'leg_l{i}', 'root', (5, 4, z), [Cube(4, 0, z - 1, 5, 2, 3, 'salamander'), Cube(4, 2, z - 1, 2, 3, 2, 'salamander')],
                   rot=(0, 0, 0), anim=[A('walk', 'y', 0.5, 1.2, i * math.pi)])
        parts += [leg, mirror(leg, f'leg_r{i}')]
    body = Part('body', 'root', (0, 4, 0), [
        Cube(-4, 3, -8, 8, 5, 17, 'salamander', decals=[('top', 2, 2, 1, 1, 0xFFE070, True), ('top', 5, 6, 1, 1, 0xFFE070, True),
                                                       ('top', 3, 11, 1, 1, 0xFFE070, True), ('top', 6, 14, 1, 1, 0xFFE070, True)]),
        Cube(-1, 8, -6, 2, 2, 12, 'fire'),
    ], anim=[A('walk', 'y', 0.15)])
    head = Part('head', 'body', (0, 5, -8), [
        Cube(-3, 3, -14, 6, 5, 6, 'salamander', decals=eyes(6, 1, 0xFFF080, gap=4, size=(1, 1))),
        Cube(-2, 3, -16, 4, 2, 2, 'salamander'),
    ], anim=[A('look', 'y', 0.6), A('cast', 'x', -0.5)])
    tail = Part('tail', 'body', (0, 5, 9), [Cube(-2, 3, 9, 4, 4, 9, 'salamander'), Cube(-1, 4, 18, 2, 2, 8, 'salamander'), Cube(-1, 4, 25, 2, 3, 3, 'fire')],
                anim=[A('idle', 'y', 0.3, 0.2), A('walk', 'y', 0.35)])
    return M('salamander', 1.0, 0.6, parts + [body, head, tail])


def magma_golem():
    parts = legs2('magma', 2, 12, w=7, d=7, swing=0.5, z=-3)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-10, 12, -6, 20, 18, 12, 'magma_hot', decals=[('front', 7, 6, 6, 5, 0xFFE070, True)]),
        Cube(-11, 26, -7, 22, 7, 14, 'magma'),
        Cube(-6, 33, -2, 4, 4, 4, 'fire'), Cube(3, 33, -3, 4, 5, 4, 'fire'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 32, -4), [
        Cube(-4, 31, -9, 8, 7, 7, 'magma', decals=eyes(8, 3, 0xFFF0A0, gap=2, size=(2, 1)) + [('front', 2, 5, 4, 1, 0xFF8A20, True)]),
        Cube(-3, 38, -7, 6, 3, 4, 'fire'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('magma', 10, 32, 18, w=6, d=7, z=-3, extra=[Cube(10, 12, -4, 7, 6, 9, 'magma_hot')], attack=1.8, cast=-2.6)
    return M('magma_golem', 1.0, 1.0, parts + [body, head] + arms)


def demon_stalker():
    parts = legs2('demon_skin', 1, 18, w=3, d=4, swing=0.8, z=-2, foot='horn_dark')
    body = Part('body', 'root', (0, 18, 0), [
        Cube(-5, 18, -3, 10, 14, 6, 'demon_skin', decals=[('front', 4, 3, 2, 6, 0xC060FF, True)]),
        Cube(-6, 30, -3, 12, 4, 6, 'demon_skin'),
    ], rot=(10, 0, 0), anim=[A('idle', 'x', 0.03, 0.1)])
    head = Part('head', 'body', (0, 34, 0), [
        Cube(-3, 34, -4, 6, 7, 6, 'demon_skin', decals=eyes(6, 2, 0xE060FF, gap=2, size=(1, 1)) + [('front', 1, 5, 4, 1, 0x1A0A20, False)]),
        Cube(-5, 40, -2, 2, 2, 2, 'horn_dark'), Cube(3, 40, -2, 2, 2, 2, 'horn_dark'),
        Cube(-6, 42, 0, 2, 4, 2, 'horn_dark'), Cube(4, 42, 0, 2, 4, 2, 'horn_dark'),
        Cube(-6, 45, 2, 1, 3, 2, 'horn_dark'), Cube(5, 45, 2, 1, 3, 2, 'horn_dark'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('demon_skin', 5, 33, 18, w=3, d=3, z=-1, extra=[Cube(5, 13, -3, 1, 4, 1, 'horn_dark'), Cube(7, 13, -3, 1, 4, 1, 'horn_dark'),
                                                                Cube(6, 13, -4, 1, 5, 1, 'horn_dark')], cast2=-1.3)
    tail = Part('tail', 'body', (0, 20, 3), [Cube(-1, 19, 3, 2, 2, 14, 'demon_skin'), Cube(-2, 18, 16, 4, 4, 2, 'horn_dark')], rot=(-20, 0, 0),
                anim=[A('idle', 'y', 0.3, 0.15)])
    return M('demon_stalker', 1.0, 0.5, parts + [body, head, tail] + arms)


def chimera():
    parts = legs4('lion_fur', 4, -11, 8, 13, w=5, d=5, swing=0.7)
    body = Part('body', 'root', (0, 13, 0), [
        Cube(-7, 13, -12, 14, 12, 25, 'lion_fur'),
        Cube(-8, 18, -13, 16, 12, 9, 'lion_mane'),
    ], anim=[A('idle', 'x', 0.02, 0.1)])
    head = Part('head', 'body', (-3, 22, -13), [
        Cube(-8, 18, -21, 10, 10, 8, 'lion_fur', decals=eyes(10, 2, 0xFFC020, gap=4, size=(2, 1)) + [('front', 3, 7, 4, 1, 0x3A0A0A, False)]),
        Cube(-6, 18, -24, 6, 5, 3, 'lion_fur', decals=[('front', 2, 0, 2, 1, 0x2A1010, False)]),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.5)])
    goat = Part('goat', 'body', (4, 28, -10), [
        Cube(2, 27, -16, 7, 7, 7, 'goat_fur', decals=eyes(7, 2, 0xFF3020, gap=3, size=(1, 1))),
        Cube(3, 26, -19, 5, 4, 3, 'goat_fur'),
        Cube(2, 34, -12, 2, 4, 2, 'horn_dark'), Cube(7, 34, -12, 2, 4, 2, 'horn_dark'),
        Cube(1, 37, -10, 2, 2, 4, 'horn_dark'), Cube(8, 37, -10, 2, 2, 4, 'horn_dark'),
    ], anim=[A('look', 'y', 0.6), A('idle', 'z', 0.1, 0.1), A('cast2', 'x', -0.6)])
    tail = Part('tail', 'body', (0, 22, 13), [
        Cube(-2, 22, 13, 4, 4, 10, 'snake_scale'), Cube(-2, 24, 22, 4, 10, 4, 'snake_scale'),
        Cube(-3, 33, 19, 6, 5, 7, 'snake_scale', decals=[('front', 1, 1, 1, 1, 0xFFE020, True), ('front', 4, 1, 1, 1, 0xFFE020, True)]),
    ], rot=(-10, 0, 0), anim=[A('idle', 'y', 0.25, 0.12), A('idle', 'x', 0.1, 0.17)])
    wg = wings('bat_wing', 7, 28, -6, 24, 14, root_mat='lion_fur', flap=0.3, speed=0.18, rot_y=-20)
    return M('chimera', 1.0, 1.4, parts + [body, head, goat, tail] + wg)


# ======================= 7. топи =======================

def giant_toad():
    body = Part('body', 'root', (0, 3, 0), [
        Cube(-7, 3, -7, 14, 9, 14, 'toad_skin'),
    ], rot=(-12, 0, 0), anim=[A('idle', 'y', 0.02, 0.2)])
    head = Part('head', 'body', (0, 9, -7), [
        Cube(-7, 7, -12, 14, 6, 6, 'toad_skin', decals=[('front', 0, 4, 14, 1, 0x2A3010, False)]),
        Cube(-6, 13, -11, 4, 3, 4, 'toad_skin', decals=[('front', 1, 0, 2, 2, 0xF0D030, True)]),
        Cube(2, 13, -11, 4, 3, 4, 'toad_skin', decals=[('front', 1, 0, 2, 2, 0xF0D030, True)]),
    ], anim=[A('look', 'y', 0.4), A('cast', 'x', -0.5)])
    tongue = Part('tongue', 'head', (0, 8, -12), [Cube(-1, 7, -15, 2, 1, 4, 'tongue')], anim=[A('cast', 'x', 0.2)])
    legs = []
    fl = Part('leg_l0', 'body', (6, 6, -5), [Cube(5, 0, -7, 3, 6, 3, 'toad_skin'), Cube(5, 0, -9, 4, 1, 3, 'toad_skin')], anim=[A('walk', 'x', 0.6)])
    bl = Part('leg_l1', 'body', (7, 6, 5), [Cube(6, 2, 2, 4, 6, 7, 'toad_skin'), Cube(6, 0, -1, 5, 2, 6, 'toad_skin')], anim=[A('walk', 'x', 0.6, phase=math.pi)])
    legs += [fl, mirror(fl, 'leg_r0'), bl, mirror(bl, 'leg_r1')]
    return M('giant_toad', 1.0, 0.8, legs + [body, head, tongue])


def mire_horror():
    body = Part('body', 'root', (0, 0, 0), [
        Cube(-11, 0, -10, 22, 12, 20, 'mire'),
        Cube(-9, 12, -8, 18, 9, 16, 'mire'),
        Cube(-6, 21, -5, 12, 5, 10, 'mire'),
        Cube(-10, 18, -2, 3, 6, 3, 'bark_dark'), Cube(7, 20, 1, 3, 7, 3, 'bark_dark'),
    ], anim=[A('idle', 'x', 0.03, 0.06), A('idle', 'z', 0.03, 0.05, 1.0)])
    face = Part('face', 'body', (0, 14, -10), [
        Cube(-6, 10, -11, 12, 8, 1, 'mire', decals=[('front', 2, 2, 2, 2, 0xD0F060, True), ('front', 8, 2, 2, 2, 0xD0F060, True),
                                                   ('front', 3, 5, 6, 2, 0x101808, False)]),
    ])
    arms = []
    for i, (x, y) in enumerate(((10, 16), (-13, 14))):
        a = Part(f'vine{i}', 'body', (x + 1.5, y, -3), [Cube(x, y - 14, -4, 3, 14, 3, 'leaf_green'), Cube(x - 1, y - 16, -5, 5, 3, 5, 'mire')],
                 rot=(-15, 0, 0), anim=[A('attack', 'x', 1.5), A('idle', 'x', 0.15, 0.1, i * 2.0), A('cast', 'x', -2.0)])
        arms.append(a)
    return M('mire_horror', 1.0, 1.0, [body, face] + arms)


def hydra():
    parts = legs4('hydra_scale', 5, -9, 7, 11, w=5, d=5, swing=0.6, foot='horn')
    body = Part('body', 'root', (0, 11, 0), [
        Cube(-9, 10, -11, 18, 13, 23, 'hydra_scale', decals=[('bottom', 4, 2, 10, 18, 0x9AC890, False)]),
        Cube(-1, 23, -8, 2, 3, 16, 'horn'),
    ], anim=[A('idle', 'x', 0.02, 0.08)])
    tail = Part('tail', 'body', (0, 16, 12), [Cube(-3, 13, 12, 6, 6, 14, 'hydra_scale'), Cube(-2, 14, 26, 4, 4, 10, 'hydra_scale')],
                anim=[A('idle', 'y', 0.2, 0.1)])
    heads = []
    for i, (x, ry) in enumerate(((-6, 25), (0, 0), (6, -25))):
        nk = Part(f'neck{i}', 'body', (x, 22, -9), [Cube(x - 2, 22, -11, 4, 16, 4, 'hydra_scale')], rot=(-20, ry, 0),
                  anim=[A('idle', 'x', 0.15, 0.09, i * 1.7), A('idle', 'z', 0.1, 0.07, i * 2.3), A('cast', 'x', -0.5)])
        hd = Part(f'head{i}', f'neck{i}', (x, 38, -9), [
            Cube(x - 3, 36, -17, 6, 5, 8, 'hydra_scale', decals=eyes(6, 1, 0xF0FF40, gap=2, size=(1, 1)) + [('front', 0, 4, 6, 1, 0x2A0A0A, False)]),
            Cube(x - 2, 41, -12, 1, 3, 2, 'horn'), Cube(x + 1, 41, -12, 1, 3, 2, 'horn'),
        ], rot=(30, 0, 0), anim=[A('look', 'y', 0.5), A('attack', 'x', 0.6)])
        heads += [nk, hd]
    return M('hydra', 1.0, 1.4, parts + [body, tail] + heads)


# ======================= 8. мёртвые земли =======================

def ghoul():
    parts = legs2('ghoul_skin', 1, 11, w=3, d=3, swing=0.9, z=-1)
    body = Part('body', 'root', (0, 11, 0), [
        Cube(-4, 11, -2, 8, 11, 4, 'ghoul_skin', decals=[('front', 1, 3, 6, 1, 0x4E5A44, False), ('front', 1, 5, 6, 1, 0x4E5A44, False)]),
        Cube(-4, 10, -2, 8, 3, 4, 'cloth_black'),
    ], rot=(25, 0, 0), anim=[A('idle', 'x', 0.05, 0.12)])
    head = Part('head', 'body', (0, 22, -1), [
        Cube(-3, 21, -6, 6, 6, 6, 'ghoul_skin', decals=eyes(6, 2, 0xE0FF60, gap=2, size=(1, 1)) + [('front', 1, 4, 4, 2, 0x2A0A0A, False),
                                                                                                ('front', 1, 4, 1, 1, 0xE8E0C8, False), ('front', 4, 4, 1, 1, 0xE8E0C8, False)]),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('idle', 'z', 0.1, 0.13)])
    arms = arm('ghoul_skin', 4, 21, 14, w=2, d=2, z=-1, extra=[Cube(4, 6, -2, 1, 2, 1, 'bone'), Cube(5, 6, -2, 1, 2, 1, 'bone')], rot=(-40, 0, 0))
    return M('ghoul', 1.0, 0.5, parts + [body, head] + arms)


def banshee():
    body = Part('body', 'root', (0, 6, 0), [
        Cube(-5, 0, -3, 10, 12, 6, 'banshee_robe'),
        Cube(-4, 12, -2, 8, 10, 4, 'banshee_robe'),
    ], anim=[A('bob', 'y', 1.4, 0.1), A('idle', 'x', 0.05, 0.1)])
    head = Part('head', 'body', (0, 22, 0), [
        Cube(-3, 22, -3, 6, 7, 6, 'banshee_robe', decals=[('front', 1, 2, 1, 2, 0x203040, False), ('front', 4, 2, 1, 2, 0x203040, False),
                                                          ('front', 2, 4, 2, 3, 0x101820, False)]),
        Cube(-4, 22, -2, 8, 9, 6, 'banshee_hair', infl=0.2),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('cast', 'x', -0.4)])
    arms = arm('banshee_robe', 4, 21, 13, w=2, d=2, z=-1, cast2=-1.4, rot=(-30, 0, 10))
    return M('banshee', 1.0, 0.4, [body, head] + arms)


def necromancer():
    parts = legs2('necro_robe', 0, 12, w=4, d=4, swing=0.5, z=-2)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -2, 8, 12, 4, 'necro_robe', decals=[('front', 3, 0, 2, 12, 0x6A3A8A, False)]),
        Cube(-5, 4, -3, 10, 10, 6, 'necro_robe'),
        Cube(-5, 22, -3, 10, 3, 6, 'necro_trim'),
    ])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -4, 8, 8, 8, 'skull', decals=[('front', 1, 3, 2, 2, 0x200830, False), ('front', 5, 3, 2, 2, 0x200830, False),
                                                   ('front', 2, 3, 1, 1, 0xC070FF, True), ('front', 5, 3, 1, 1, 0xC070FF, True)]),
    ] + hood(23, 'necro_robe') + [
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('necro_robe', 4, 23, 12, w=3, d=4, z=-2, cast2=-1.3)
    staff = Part('staff', 'arm_r', (-6, 12, 0), [Cube(-6, 2, -2, 1, 22, 1, 'bone_dark'), Cube(-7, 23, -3, 3, 3, 3, 'skull'),
                                                 Cube(-7, 26, -3, 3, 1, 3, 'necro_trim')])
    return M('necromancer', 1.0, 0.5, parts + [body, head] + arms + [staff])


def bone_colossus():
    parts = legs2('bone', 3, 20, w=6, d=6, swing=0.5, z=-3, foot='bone_dark')
    body = Part('body', 'root', (0, 20, 0), [
        Cube(-2, 20, -2, 4, 22, 4, 'bone_dark'),
        Cube(-11, 26, -7, 22, 2, 14, 'bone'), Cube(-11, 31, -7, 22, 2, 14, 'bone'), Cube(-11, 36, -7, 22, 2, 14, 'bone'),
        Cube(-12, 41, -8, 24, 7, 16, 'bone'),
        Cube(-9, 18, -6, 18, 5, 12, 'bone_dark'),
        Cube(-3, 28, -3, 6, 6, 6, 'fire_soul'),
    ], anim=[A('idle', 'x', 0.03, 0.06)])
    head = Part('head', 'body', (0, 48, -2), [
        Cube(-6, 47, -10, 12, 11, 11, 'skull', decals=[('front', 2, 3, 3, 3, 0x0A0A10, False), ('front', 7, 3, 3, 3, 0x0A0A10, False),
                                                       ('front', 3, 4, 1, 1, 0x60F0FF, True), ('front', 8, 4, 1, 1, 0x60F0FF, True),
                                                       ('front', 2, 8, 8, 1, 0x2A2A2A, False)]),
        Cube(-9, 54, -6, 3, 3, 3, 'horn'), Cube(6, 54, -6, 3, 3, 3, 'horn'), Cube(-10, 57, -5, 2, 5, 2, 'horn'), Cube(8, 57, -5, 2, 5, 2, 'horn'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.4)])
    arms = arm('bone', 12, 47, 26, w=5, d=5, z=-2, extra=[Cube(11, 17, -4, 7, 6, 9, 'bone_dark'), Cube(12, 14, -4, 2, 3, 2, 'bone'),
                                                           Cube(15, 14, -4, 2, 3, 2, 'bone')], attack=1.8, cast=-2.6)
    return M('bone_colossus', 1.0, 1.2, parts + [body, head] + arms)


def mummy():
    parts = legs2('mummy_wrap', 0, 12, w=4, d=4, swing=0.5, z=-2)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -2, 8, 12, 4, 'mummy_wrap', decals=[('front', 2, 2, 4, 3, 0xC8A040, False)]),
    ], anim=[A('idle', 'z', 0.04, 0.08)])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -4, 8, 8, 8, 'mummy_wrap', decals=[('front', 1, 3, 6, 1, 0x1A1008, False), ('front', 2, 3, 1, 1, 0x40FF80, True),
                                                       ('front', 5, 3, 1, 1, 0x40FF80, True)]),
        Cube(-5, 30, -5, 10, 2, 10, 'gold'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arms = arm('mummy_wrap', 4, 23, 12, w=4, d=4, z=-2, rot=(-80, 0, 0), swing=0.1)
    return M('mummy', 1.0, 0.5, parts + [body, head] + arms)


# ======================= 9. подземелья =======================

def grimlock():
    parts = legs2('grim_skin', 1, 12, w=3, d=3, swing=0.8, z=-1)
    body = Part('body', 'root', (0, 12, 0), [
        Cube(-4, 12, -2, 8, 12, 4, 'grim_skin', decals=[('front', 2, 2, 1, 4, 0x9A928A, False), ('front', 5, 2, 1, 4, 0x9A928A, False)]),
        Cube(-4, 11, -2, 8, 3, 4, 'leather_dark'),
    ], rot=(15, 0, 0), anim=[A('idle', 'x', 0.04, 0.1)])
    head = Part('head', 'body', (0, 24, 0), [
        Cube(-4, 24, -5, 8, 7, 7, 'grim_skin', decals=[('front', 1, 1, 6, 2, 0xA8A098, False), ('front', 1, 4, 6, 2, 0x3A0A0A, False),
                                                       ('front', 1, 4, 1, 1, 0xF0F0E0, False), ('front', 3, 4, 1, 1, 0xF0F0E0, False),
                                                       ('front', 5, 4, 1, 1, 0xF0F0E0, False)]),
        Cube(-5, 26, -3, 1, 3, 2, 'grim_skin'), Cube(4, 26, -3, 1, 3, 2, 'grim_skin'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5), A('idle', 'y', 0.2, 0.2)])
    arms = arm('grim_skin', 4, 23, 13, w=3, d=3, z=-1, extra=[Cube(4, 8, -2, 3, 3, 4, 'bone_dark')], rot=(-20, 0, 0))
    return M('grimlock', 1.0, 0.5, parts + [body, head] + arms)


def mimic():
    base = Part('base', 'root', (0, 0, 0), [
        Cube(-7, 0, -7, 14, 9, 14, 'chest_wood', decals=[('front', 6, 6, 2, 3, 0xD8D8E0, False)]),
        Cube(-6, 8, -6, 12, 1, 12, 'mouth'),
        Cube(-6, 8, -6, 1, 2, 1, 'teeth'), Cube(-3, 8, -6, 1, 2, 1, 'teeth'), Cube(0, 8, -6, 1, 2, 1, 'teeth'), Cube(3, 8, -6, 1, 2, 1, 'teeth'),
        Cube(5, 8, -6, 1, 2, 1, 'teeth'), Cube(-6, 8, -2, 1, 2, 1, 'teeth'), Cube(5, 8, -1, 1, 2, 1, 'teeth'),
    ], anim=[A('walkabs', 'x', 0.15, 2.0)])
    lid = Part('lid', 'base', (0, 9, 7), [
        Cube(-7, 9, -7, 14, 5, 14, 'chest_dark', decals=[('front', 6, 0, 2, 2, 0xD8D8E0, False)]),
        Cube(-6, 8, -6, 1, 2, 1, 'teeth'), Cube(-3, 8, -6, 1, 2, 1, 'teeth'), Cube(1, 8, -6, 1, 2, 1, 'teeth'), Cube(4, 8, -6, 1, 2, 1, 'teeth'),
        Cube(-4, 11, -7, 2, 1, 1, 'mouth', decals=[('front', 0, 0, 2, 1, 0xFF3020, True)]),
        Cube(2, 11, -7, 2, 1, 1, 'mouth', decals=[('front', 0, 0, 2, 1, 0xFF3020, True)]),
    ], anim=[A('awake', 'x', -0.55), A('idle', 'x', -0.12, 0.25), A('attack', 'x', -0.5)])
    tongue = Part('tongue', 'base', (0, 9, 2), [Cube(-2, 8, -8, 4, 1, 10, 'tongue')], anim=[A('awake', 'x', 0.3), A('idle', 'y', 0.2, 0.3)])
    legs = []
    for i, z in enumerate((-5, 3)):
        leg = Part(f'leg_l{i}', 'base', (6, 2, z), [Cube(6, -1, z, 4, 2, 2, 'mouth')], anim=[A('awake', 'z', 0.4), A('walk', 'x', 0.8, phase=i * math.pi)])
        legs += [leg, mirror(leg, f'leg_r{i}')]
    return M('mimic', 1.0, 0.6, [base, lid, tongue] + legs)


MODELS = [deer, boar, jelly, scarecrow, stag_beetle, dire_wolf, bloodbat, barrow_wight, ogre, griffin,
          weaver_spider, treant, elder_treant, shadow_panther, cave_bear, kobold, crystal_golem,
          harpy, wyvern, hyena, vulture, centaur, basilisk, sandworm, manticore,
          snow_leopard, yeti, mammoth, ice_golem, wendigo,
          hellhound, salamander, magma_golem, demon_stalker, chimera,
          giant_toad, mire_horror, hydra,
          ghoul, banshee, necromancer, bone_colossus, mummy,
          grimlock, mimic]


# ======================= запись =======================

from bestiary_data import SPECIES, EXTERNAL, BANDS, BEASTS  # noqa: E402


def write_models(only=None):
    model_dir = os.path.join(ASSETS, 'boss_models')
    tex_dir = os.path.join(ASSETS, 'textures', 'entity', 'boss')
    previews = []
    for fn in MODELS:
        m = fn()
        if only and m['name'] not in only:
            continue
        cubes = [c for p in m['parts'] for c in p.cubes]
        W = 64
        H = GB.pack(cubes, W)
        while H > W and W < 512:
            W *= 2
            H = GB.pack(cubes, W)
        img, glow = GB.paint(m, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '.png'), img, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '_glow.png'), glow, W, H)
        json.dump(GB.to_java(m, W, H), open(os.path.join(model_dir, m['name'] + '.json'), 'w'), indent=1)
        print(f"{m['name']}: {len(cubes)} cubes, texture {W}x{H}")
        if '--preview' in sys.argv:
            GB.preview(m, img, previews, size=220)
    if previews:
        out = sys.argv[sys.argv.index('--preview') + 1]
        cols = 6
        rows = (len(previews) + cols - 1) // cols
        size = len(previews[0])
        sheet = [[(20, 22, 28, 255)] * (cols * size) for _ in range(rows * size)]
        for i, pv in enumerate(previews):
            ox, oy = (i % cols) * size, (i // cols) * size
            for y in range(size):
                sheet[oy + y][ox:ox + size] = pv[y]
        write_png(out, sheet, cols * size, rows * size)
        print('preview ->', out)


def main():
    only = None
    if '--only' in sys.argv:
        only = set(sys.argv[sys.argv.index('--only') + 1].split(','))
    ids = [s['id'] for s in SPECIES]
    names = [fn.__name__ for fn in MODELS]
    assert ids == names, (set(ids) ^ set(names))
    write_models(only)
    for s in SPECIES:
        s['beast'] = s['id'] in BEASTS
    data = dict(species=SPECIES, external=EXTERNAL, bands=BANDS)
    json.dump(data, open(os.path.join(ROOT, 'litrpg_bestiary.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    ru_path, en_path = os.path.join(ASSETS, 'lang', 'ru_ru.json'), os.path.join(ASSETS, 'lang', 'en_us.json')
    ru, en = json.load(open(ru_path, encoding='utf-8')), json.load(open(en_path, encoding='utf-8'))
    for s in SPECIES:
        ru['entity.litrpg.' + s['id']] = s['ru']
        en['entity.litrpg.' + s['id']] = s['en']
        ru['item.litrpg.' + s['id'] + '_spawn_egg'] = 'Яйцо призыва: ' + s['ru']
        en['item.litrpg.' + s['id'] + '_spawn_egg'] = s['en'] + ' Spawn Egg'
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(ASSETS, 'models', 'item', s['id'] + '_spawn_egg.json'), 'w'))
    json.dump(ru, open(ru_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    print(len(SPECIES), 'species')


if __name__ == '__main__':
    main()
