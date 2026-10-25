"""
Модели хранителей пяти этапов подземелья (каркас и материалы — из gen_bosses.py).
Пишет boss_models/ID.json, textures/entity/boss/ID.png (+ _glow), названия хранителей и яиц.
Запуск: python tools/gen_stage_bosses.py [--preview build/preview/stage_bosses.png] [--only id,id]
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_bosses as G  # noqa: E402
from gen_bosses import Cube, Part, A, eyes, mirror, MATS  # noqa: E402
from png import write_png  # noqa: E402

MATS.update({
    'rust_metal': G.m_metal(0x6A5240, 0xA88A64, 0x34261C),
    'chain': G.m_metal(0x55555E, 0x9A9AA8, 0x26262C, rivets=False),
    'bone_old': G.m_bone(0xD8CCA8, 0x8A7A58),
    'robe_violet': G.m_cloth(0x3A2448, 0x1C1024, tatter=True),
    'robe_crimson': G.m_cloth(0x5A1418, 0x2A080C, tatter=True),
    'skin_pale': G.m_skin(0xB8B0A8, 0x8A8078, spots=0x6A6058, spot_chance=0.05),
    'hair_black': G.m_hair(0x1E1A22, 0x0A080C),
    'hair_red': G.m_hair(0x7A1418, 0x3A080A),
    'soul_glow': G.m_glow(0x90FFB0, 0x3A9A60),
    'blood_glow': G.m_glow(0xFF4040, 0x8A1010),
    'lich_plate': G.m_metal(0x26303E, 0x6A80A0, 0x10141C),
    'frost_crown': G.m_ice(0xA8E0F8, 0xF0FAFF, 0x4A90C0, glow_chance=0.25),
    'lich_cloak': G.m_cloth(0x1A2030, 0x0A0E18, tatter=True),
    'runeblade': G.m_glow(0xC8F0FF, 0x4AA0E0),
    'skull': G.m_bone(0xE0D8C0, 0x9A8E70),
    # этап 3
    'coral_plate': G.m_metal(0x2E7A78, 0x6AD0C0, 0x123A3C),
    'coral_red': G.m_chitin(0xB0405A, 0xF08A9A, band=3),
    'scale_teal': G.m_chitin(0x1E5A66, 0x48A8B0, band=2),
    'scale_belly': G.m_chitin(0xA8B890, 0xE0E8C0, band=2),
    'skin_sea': G.m_skin(0x5A8A90, 0x3A6068, spots=0x2A4A50, spot_chance=0.08),
    'fin': G.m_membrane(0x3A8AA0, 0x1A4A60),
    'sea_glow': G.m_glow(0x80F0FF, 0x2A90B0),
    'robe_sea': G.m_cloth(0x1A3A5A, 0x0A1A2C, tatter=True),
    'pearl': G.m_glow(0xF0F0FF, 0xA0B0D0),
    'gold_trim': G.m_metal(0xB08A30, 0xF0D070, 0x5A4010, rivets=False),
    # этап 4
    'forge_iron': G.m_metal(0x3A3438, 0x7A7078, 0x1A1618),
    'molten': G.m_magma(0x2A1C18, 0xFF8A20, 0xFFE070, crack_amt=0.14),
    'molten_hot': G.m_glow(0xFFB040, 0xC04010),
    'ember_plate': G.m_metal(0x5A2010, 0xC86030, 0x2A0C06),
    'flame': G.m_fire(),
    'robe_ash': G.m_cloth(0x3A3434, 0x1A1616, tatter=True),
    'hair_ember': G.m_hair(0xE05A10, 0x7A2008),
    'skin_ash': G.m_skin(0x8A7A70, 0x5A4C44, spots=0x3A302A, spot_chance=0.1),
    'obsid': G.m_stone(0x1A1022, 0x0A0612, 0x3A2A4A, cracks=True),
})


# ======================= Этап 1 =======================

def gatekeeper():
    """Привратник Костей: огромный скелет в ржавых латах, обмотанный цепями, с цепным кистенём."""
    leg = Part('leg_l', 'root', (3, 16, 0), [
        Cube(1, 0, -2, 3, 16, 3, 'bone_old'),
        Cube(0, 8, -3, 5, 5, 5, 'rust_metal'),
        Cube(0, 0, -4, 5, 3, 6, 'rust_metal'),
    ], anim=[A('walk', 'x', 0.6)])
    body = Part('body', 'root', (0, 16, 0), [
        Cube(-5, 16, -2, 10, 4, 4, 'bone_old'),
        Cube(-7, 20, -4, 14, 14, 8, 'rust_metal', decals=[('front', 5, 4, 4, 6, 0x1A1410, False), ('front', 6, 5, 2, 2, 0x90FFB0, True)]),
        Cube(-8, 22, -5, 16, 2, 10, 'chain'),
        Cube(-8, 28, -5, 16, 2, 10, 'chain'),
        Cube(-10, 31, -5, 6, 5, 10, 'rust_metal'),
        Cube(4, 31, -5, 6, 5, 10, 'rust_metal'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 34, 0), [
        Cube(-4, 34, -5, 8, 8, 8, 'skull', decals=eyes(8, 3, 0x90FFB0, gap=2, size=(2, 2)) + [('front', 2, 6, 4, 1, 0x1A1410, False)]),
        Cube(-5, 40, -6, 10, 3, 10, 'rust_metal'),
        Cube(-8, 41, -2, 3, 2, 2, 'bone_old'), Cube(-10, 43, -2, 2, 4, 2, 'bone_old'),
        Cube(5, 41, -2, 3, 2, 2, 'bone_old'), Cube(8, 43, -2, 2, 4, 2, 'bone_old'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (9, 33, 0), [
        Cube(8, 20, -2, 3, 13, 3, 'bone_old'),
        Cube(7, 16, -3, 5, 5, 5, 'rust_metal'),
        Cube(7, 26, -3, 5, 2, 5, 'chain'),
    ], anim=[A('walk', 'x', 0.5, phase=math.pi), A('attack', 'x', 1.9), A('cast', 'x', -2.5), A('cast2', 'z', -1.0)])
    arm_r = mirror(arm, 'arm_r')
    flail = Part('flail', 'arm_r', (-9, 17, 0), [
        Cube(-10, 4, -1, 2, 13, 2, 'chain'),
        Cube(-12, -2, -3, 6, 6, 6, 'rust_metal', decals=[('front', 2, 2, 2, 2, 0x90FFB0, True)]),
    ], anim=[A('idle', 'z', 0.2, 0.15)])
    cape_chain = Part('chains', 'body', (0, 30, 4), [Cube(-6, 12, 4, 2, 18, 1, 'chain'), Cube(4, 14, 4, 2, 16, 1, 'chain')],
                      anim=[A('idle', 'x', 0.1, 0.12)])
    return dict(name='gatekeeper', scale=1.45, shadow=1.0, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, flail, cape_chain])


def _sister(name, robe, hair, glow, weapon_mat, weapon):
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-6, 0, -4, 12, 16, 8, robe, decals=[('front', 5, 2, 2, 10, glow, True)]),
        Cube(-5, 16, -3, 10, 10, 6, robe),
        Cube(-6, 24, -4, 12, 3, 8, robe),
    ], anim=[A('idle', 'x', 0.03, 0.09), A('bob', 'y', 0.4, 0.08)])
    head = Part('head', 'body', (0, 27, 0), [
        Cube(-4, 27, -4, 8, 8, 8, 'skin_pale', decals=eyes(8, 3, glow, gap=2, size=(2, 1)) + [('front', 3, 6, 2, 1, 0x3A2028, False)]),
        Cube(-5, 26, -1, 10, 10, 6, hair),
        Cube(-4, 35, -4, 8, 1, 8, hair),
        Cube(-4, 36, -4, 1, 2, 1, 'bone_old'), Cube(-1, 36, -5, 2, 3, 1, 'bone_old'), Cube(3, 36, -4, 1, 2, 1, 'bone_old'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (7, 25, 0), [Cube(6, 13, -2, 3, 12, 4, robe), Cube(6, 10, -2, 3, 3, 3, 'skin_pale')],
               anim=[A('walk', 'x', 0.3, phase=math.pi), A('attack', 'x', 1.6), A('cast', 'x', -2.6), A('cast2', 'z', -1.1),
                     A('idle', 'z', -0.05, 0.1)])
    arm_r = mirror(arm, 'arm_r')
    parts = [body, head, arm, arm_r]
    if weapon == 'staff':
        parts.append(Part('staff', 'arm_r', (-8, 12, -1), [
            Cube(-9, -6, -2, 2, 36, 2, 'bone_old'),
            Cube(-11, 28, -4, 6, 6, 6, 'skull', decals=eyes(6, 2, glow, gap=0, size=(1, 1))),
            Cube(-9, 34, -2, 2, 3, 2, weapon_mat),
        ]))
    else:
        parts.append(Part('sickle', 'arm_r', (-8, 12, -1), [
            Cube(-9, -2, -1, 2, 16, 2, 'bone_old'),
            Cube(-9, 14, -9, 2, 2, 10, weapon_mat), Cube(-9, 10, -10, 2, 4, 2, weapon_mat),
        ]))
    parts.append(Part('veil', 'body', (0, 26, 4), [Cube(-6, 2, 4, 12, 24, 1, robe)], anim=[A('idle', 'x', 0.08, 0.1)]))
    return dict(name=name, scale=1.2, shadow=0.6, parts=parts)


def twin_morra():
    return _sister('twin_morra', 'robe_violet', 'hair_black', 0x90FFB0, 'soul_glow', 'staff')


def twin_vessa():
    return _sister('twin_vessa', 'robe_crimson', 'hair_red', 0xFF4040, 'blood_glow', 'sickle')


def lich_king():
    """Король-лич: высокий, в тёмных латах и рваном плаще, ледяная корона, рунный клинок."""
    leg = Part('leg_l', 'root', (3, 16, 0), [Cube(1, 0, -3, 4, 16, 5, 'lich_plate')], anim=[A('walk', 'x', 0.5)])
    body = Part('body', 'root', (0, 16, 0), [
        Cube(-6, 16, -3, 12, 16, 7, 'lich_plate', decals=[('front', 4, 5, 4, 5, 0x60C8FF, True), ('front', 5, 6, 2, 3, 0xE0F8FF, True)]),
        Cube(-9, 29, -4, 6, 5, 9, 'lich_plate', decals=[('top', 1, 2, 4, 1, 0x60C8FF, True)]),
        Cube(3, 29, -4, 6, 5, 9, 'lich_plate', decals=[('top', 1, 2, 4, 1, 0x60C8FF, True)]),
        Cube(-7, 2, 3, 14, 30, 2, 'lich_cloak'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 32, 0), [
        Cube(-4, 32, -4, 8, 9, 8, 'skull', decals=eyes(8, 4, 0x60C8FF, gap=2, size=(2, 1)) + [('front', 2, 7, 4, 1, 0x101820, False)]),
        Cube(-5, 39, -5, 10, 2, 10, 'frost_crown'),
        Cube(-5, 41, -5, 2, 5, 2, 'frost_crown'), Cube(-1, 41, -6, 2, 7, 2, 'frost_crown'), Cube(3, 41, -5, 2, 5, 2, 'frost_crown'),
        Cube(-5, 41, 3, 2, 4, 2, 'frost_crown'), Cube(3, 41, 3, 2, 4, 2, 'frost_crown'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (8, 31, 0), [Cube(6, 17, -2, 4, 14, 4, 'lich_plate'), Cube(6, 14, -2, 4, 3, 4, 'skull')],
               anim=[A('walk', 'x', 0.4, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.6), A('cast2', 'z', -1.2)])
    arm_r = mirror(arm, 'arm_r')
    blade = Part('blade', 'arm_r', (-8, 15, 0), [
        Cube(-9, -12, -1, 2, 26, 2, 'runeblade'),
        Cube(-11, 13, -2, 6, 2, 4, 'lich_plate'),
        Cube(-9, 15, -1, 2, 4, 2, 'lich_plate'),
    ])
    aura = Part('aura', 'body', (0, 24, 0), [
        Cube(-12, 30, -1, 2, 2, 2, 'frost_crown'), Cube(10, 30, -1, 2, 2, 2, 'frost_crown'),
        Cube(-1, 30, -12, 2, 2, 2, 'frost_crown'), Cube(-1, 30, 10, 2, 2, 2, 'frost_crown'),
    ], anim=[A('spin', 'y', 1.0, 0.05)])
    return dict(name='lich_king', scale=1.35, shadow=0.7, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, blade, aura])


def dark_witch():
    """Ведьма Тьмы: сгорбленная, в чёрно-фиолетовом рубище, остроконечная шляпа, посох с вороньим черепом."""
    m = G.swamp_hag()
    m['name'] = 'dark_witch'
    for p in m['parts']:
        for c in p.cubes:
            c.mat = {'cloth_green': 'robe_violet', 'skin_hag': 'skin_pale', 'hair': 'hair_black', 'glass_green': 'soul_glow'}.get(c.mat, c.mat)
            c.decals = [(d[0], d[1], d[2], d[3], d[4], 0xB060FF if d[5] in (0x90F070, 0xF0E040) else d[5], d[6]) for d in c.decals]
    # вороньи крылья за спиной
    wing = Part('wing_l', 'body', (4, 26, 4), [Cube(4, 14, 4, 1, 14, 10, 'hair_black')], rot=(0, 20, 0), anim=[A('idle', 'y', 0.25, 0.15)])
    m['parts'] += [wing, mirror(wing, 'wing_r')]
    m['scale'] = 1.2
    return m


# ======================= Этап 3 =======================

def tide_warden():
    """Страж приливов: рыболюд-рыцарь в коралловых латах, плавники на голове и руках, трезубец."""
    leg = Part('leg_l', 'root', (3, 16, 0), [Cube(1, 0, -3, 5, 16, 6, 'coral_plate'), Cube(1, 0, -5, 5, 2, 3, 'scale_teal')],
               anim=[A('walk', 'x', 0.55)])
    body = Part('body', 'root', (0, 16, 0), [
        Cube(-7, 16, -4, 14, 16, 8, 'coral_plate', decals=[('front', 5, 5, 4, 4, 0x80F0FF, True)]),
        Cube(-6, 14, -3, 12, 3, 6, 'scale_teal'),
        Cube(-10, 28, -5, 6, 6, 10, 'coral_red'), Cube(4, 28, -5, 6, 6, 10, 'coral_red'),
        Cube(-1, 20, 4, 2, 14, 6, 'fin'),
    ], anim=[A('idle', 'x', 0.03, 0.08)])
    head = Part('head', 'body', (0, 32, 0), [
        Cube(-4, 32, -5, 8, 8, 9, 'skin_sea', decals=eyes(8, 4, 0x80F0FF, gap=2, size=(2, 2)) + [('front', 1, 7, 6, 1, 0x10282C, False)]),
        Cube(-5, 32, -6, 10, 3, 3, 'scale_teal'),
        Cube(-1, 40, -4, 2, 6, 9, 'fin'),
        Cube(-6, 35, -2, 2, 5, 4, 'fin'), Cube(4, 35, -2, 2, 5, 4, 'fin'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    arm = Part('arm_l', 'body', (9, 31, 0), [Cube(7, 17, -2, 4, 14, 4, 'skin_sea'), Cube(6, 22, -3, 6, 6, 6, 'coral_plate'),
                                            Cube(11, 18, -1, 2, 8, 3, 'fin')],
               anim=[A('walk', 'x', 0.45, phase=math.pi), A('attack', 'x', 2.0), A('cast', 'x', -2.6), A('cast2', 'z', -1.1)])
    arm_r = mirror(arm, 'arm_r')
    trident = Part('trident', 'arm_r', (-9, 18, 0), [
        Cube(-10, -14, -1, 2, 42, 2, 'gold_trim'),
        Cube(-14, 26, -1, 10, 2, 2, 'gold_trim'),
        Cube(-14, 28, -1, 2, 7, 2, 'sea_glow'), Cube(-10, 28, -1, 2, 9, 2, 'sea_glow'), Cube(-6, 28, -1, 2, 7, 2, 'sea_glow'),
    ])
    return dict(name='tide_warden', scale=1.45, shadow=0.9, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, arm_r, trident])


def leviathan():
    """Левиафан: морской змей — шея поднимается из воды кольцами, голова с челюстями и гребнем, плавники."""
    parts = []
    segs = [(0, 0, 14, 'base'), (0, 12, 13, 'neck1'), (-2, 24, 12, 'neck2'), (-1, 35, 11, 'neck3')]
    parent = 'root'
    for i, (dx, y, w, name) in enumerate(segs):
        h = 13
        cubes = [Cube(-w // 2 + dx, y, -w // 2, w, h, w, 'scale_teal'),
                 Cube(-w // 2 + dx + 2, y, -w // 2 - 1, w - 4, h, 1, 'scale_belly'),
                 Cube(dx - 1, y + 2, w // 2, 2, h - 2, 4, 'fin')]
        parts.append(Part(name, parent, (dx, y, 0), cubes, anim=[A('idle', 'z', 0.06, 0.07, phase=i * 0.8), A('bob', 'x', 0.05, 0.06, phase=i)]))
        parent = name
    head = Part('head', parent, (-1, 46, 0), [
        Cube(-8, 46, -14, 16, 10, 18, 'scale_teal', decals=eyes(16, 3, 0x80F0FF, gap=6, size=(3, 2))),
        Cube(-7, 42, -18, 14, 4, 16, 'scale_belly'),
        Cube(-7, 46, -19, 14, 1, 5, 'bone_old'),
        Cube(-1, 56, -12, 2, 8, 16, 'fin'),
        Cube(-10, 50, -2, 3, 8, 6, 'fin'), Cube(7, 50, -2, 3, 8, 6, 'fin'),
        Cube(-6, 56, -6, 2, 5, 2, 'coral_red'), Cube(4, 56, -6, 2, 5, 2, 'coral_red'),
    ], anim=[A('look', 'y', 0.7), A('look', 'x', 0.5), A('attack', 'x', 0.8), A('cast', 'x', -0.5)])
    jaw = Part('jaw', 'head', (0, 44, 2), [Cube(-6, 40, -17, 12, 3, 15, 'scale_teal'), Cube(-6, 43, -17, 12, 1, 4, 'bone_old')],
               anim=[A('attack', 'x', 0.7), A('cast', 'x', 0.9), A('idle', 'x', 0.08, 0.1)])
    fin_l = Part('fin_l', 'base', (7, 10, 0), [Cube(7, 4, -6, 14, 2, 12, 'fin')], anim=[A('idle', 'z', 0.25, 0.1)])
    return dict(name='leviathan', scale=1.7, shadow=1.4, parts=parts + [head, jaw, fin_l, mirror(fin_l, 'fin_r')])


def deep_priestess():
    """Жрица глубин: высокая, в синих одеждах с золотом, волосы-щупальца, коралловый венец, посох с жемчужиной."""
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-7, 0, -5, 14, 16, 10, 'robe_sea', decals=[('front', 6, 1, 2, 14, 0xF0D070, False)]),
        Cube(-5, 16, -3, 10, 11, 6, 'robe_sea', decals=[('front', 3, 4, 4, 3, 0x80F0FF, True)]),
        Cube(-7, 25, -4, 14, 3, 8, 'gold_trim'),
    ], anim=[A('idle', 'x', 0.03, 0.08), A('bob', 'y', 0.6, 0.07)])
    head = Part('head', 'body', (0, 28, 0), [
        Cube(-4, 28, -4, 8, 8, 8, 'skin_sea', decals=eyes(8, 3, 0xF0F0FF, gap=2, size=(2, 1))),
        Cube(-5, 35, -5, 10, 2, 10, 'coral_red'),
        Cube(-5, 37, -2, 2, 4, 2, 'coral_red'), Cube(-1, 37, -5, 2, 6, 2, 'coral_red'), Cube(3, 37, -2, 2, 4, 2, 'coral_red'),
        Cube(-1, 39, -5, 2, 2, 1, 'pearl'),
    ], anim=[A('look', 'y'), A('look', 'x', 0.5)])
    hair = []
    for i, (x, z) in enumerate(((-4, 3), (-1, 4), (2, 3), (-5, 0), (4, 0))):
        hair.append(Part(f'tentacle{i}', 'head', (x, 30, z), [Cube(x - 1, 14, z - 1, 2, 16, 2, 'scale_teal'), Cube(x - 1, 12, z - 1, 2, 2, 2, 'sea_glow')],
                         anim=[A('idle', 'x', 0.2, 0.08, phase=i), A('idle', 'z', 0.12, 0.06, phase=i * 1.7)]))
    arm = Part('arm_l', 'body', (7, 26, 0), [Cube(6, 13, -2, 3, 13, 4, 'robe_sea'), Cube(6, 10, -2, 3, 3, 3, 'skin_sea')],
               anim=[A('walk', 'x', 0.3, phase=math.pi), A('attack', 'x', 1.6), A('cast', 'x', -2.6), A('cast2', 'z', -1.1)])
    arm_r = mirror(arm, 'arm_r')
    staff = Part('staff', 'arm_r', (-8, 12, -1), [
        Cube(-9, -8, -2, 2, 40, 2, 'gold_trim'),
        Cube(-11, 30, -4, 6, 2, 6, 'coral_red'),
        Cube(-10, 32, -3, 4, 4, 4, 'pearl'),
    ])
    orbs = Part('orbs', 'body', (0, 22, 0), [Cube(-13, 26, -1, 2, 2, 2, 'sea_glow'), Cube(11, 26, -1, 2, 2, 2, 'sea_glow'),
                                             Cube(-1, 26, 11, 2, 2, 2, 'sea_glow')], anim=[A('spin', 'y', 1.0, 0.04)])
    return dict(name='deep_priestess', scale=1.35, shadow=0.7, parts=[body, head, arm, arm_r, staff, orbs] + hair)


# ======================= Этап 4 =======================

def smelter_golem():
    """Голем-плавильщик: широкий чугунный корпус-печь с раскалённым горном в груди, руки-ковши, труба на спине."""
    leg = Part('leg_l', 'root', (5, 14, 0), [Cube(2, 0, -4, 7, 14, 8, 'forge_iron'), Cube(1, 0, -6, 9, 3, 11, 'forge_iron')],
               anim=[A('walk', 'x', 0.45)])
    body = Part('body', 'root', (0, 14, 0), [
        Cube(-11, 14, -7, 22, 20, 14, 'forge_iron', decals=[('front', 6, 5, 10, 8, 0xFF9A30, True), ('front', 8, 7, 6, 4, 0xFFE070, True)]),
        Cube(-9, 12, -6, 18, 3, 12, 'molten'),
        Cube(-12, 30, -8, 24, 4, 16, 'forge_iron'),
        Cube(-3, 34, 3, 6, 12, 6, 'forge_iron'), Cube(-2, 46, 4, 4, 2, 4, 'molten_hot'),
    ], anim=[A('idle', 'x', 0.02, 0.07)])
    head = Part('head', 'body', (0, 34, -2), [
        Cube(-5, 34, -9, 10, 7, 8, 'forge_iron', decals=[('front', 2, 2, 6, 2, 0xFFB040, True)]),
        Cube(-6, 39, -10, 12, 2, 10, 'molten'),
    ], anim=[A('look', 'y', 0.6), A('look', 'x', 0.3)])
    arm = Part('arm_l', 'body', (13, 31, 0), [
        Cube(11, 16, -4, 7, 16, 8, 'forge_iron'),
        Cube(10, 6, -6, 9, 10, 12, 'forge_iron', decals=[('front', 2, 5, 5, 4, 0xFF8A20, True)]),
        Cube(11, 14, -5, 7, 2, 10, 'molten'),
    ], anim=[A('walk', 'x', 0.35, phase=math.pi), A('attack', 'x', 1.6), A('cast', 'x', -2.2), A('cast2', 'z', -0.9)])
    return dict(name='smelter_golem', scale=1.5, shadow=1.3, parts=[leg, mirror(leg, 'leg_r'), body, head, arm, mirror(arm, 'arm_r')])


def _recolor(m, name, swap, glow_from=(), glow_to=0):
    m['name'] = name
    for p in m['parts']:
        for c in p.cubes:
            c.mat = swap.get(c.mat, c.mat)
            if glow_from:
                c.decals = [(d[0], d[1], d[2], d[3], d[4], glow_to if d[5] in glow_from else d[5], d[6]) for d in c.decals]
    return m


def fire_twin_ignis():
    """Игнис: рыцарь в раскалённых латах, корона-пламя, огненный клинок."""
    m = _recolor(lich_king(), 'fire_twin_ignis',
                 {'lich_plate': 'ember_plate', 'frost_crown': 'flame', 'lich_cloak': 'robe_crimson', 'runeblade': 'molten_hot', 'skull': 'skin_ash'},
                 (0x60C8FF, 0xE0F8FF), 0xFFB040)
    m['scale'] = 1.25
    return m


def fire_twin_cinder():
    """Синдер: чародейка в пепельном рубище, волосы-угли, посох с огненным черепом."""
    m = _sister('fire_twin_cinder', 'robe_ash', 'hair_ember', 0xFFA030, 'flame', 'staff')
    for p in m['parts']:
        for c in p.cubes:
            if c.mat == 'skin_pale':
                c.mat = 'skin_ash'
    return m


def ash_lord():
    """Повелитель Пепла: огненный владыка в обсидиановых наплечниках с пепельной короной и двуручным клинком."""
    m = _recolor(G.flame_lord(), 'ash_lord', {'obsidian': 'obsid'})
    crown = Part('crown', 'head', (0, 44, -1), [Cube(-6, 44, -6, 12, 2, 12, 'obsid'), Cube(-6, 46, -6, 2, 4, 2, 'molten_hot'),
                                                Cube(4, 46, -6, 2, 4, 2, 'molten_hot'), Cube(-1, 46, -7, 2, 6, 2, 'molten_hot')])
    sword = Part('sword', 'arm_r', (-12, 12, 0), [Cube(-13, -22, -1, 3, 34, 2, 'molten_hot'), Cube(-16, 11, -2, 9, 2, 4, 'obsid'),
                                                  Cube(-13, 13, -1, 3, 6, 2, 'obsid')])
    m['parts'] += [crown, sword]
    m['scale'] = 1.35
    return m


MODELS = [gatekeeper, twin_morra, twin_vessa, lich_king, dark_witch, tide_warden, leviathan, deep_priestess,
          smelter_golem, fire_twin_ignis, fire_twin_cinder, ash_lord]

NAMES = {
    'gatekeeper': ('Привратник Костей', 'Bone Gatekeeper'),
    'twin_morra': ('Морра, Повелительница Костей', 'Morra, Mistress of Bones'),
    'twin_vessa': ('Весса, Кровавая Ведьма', 'Vessa, the Blood Witch'),
    'lich_king': ('Король-лич', 'The Lich King'),
    'spider_queen': ('Матка пауков', 'Spider Matriarch'),
    'rot_ent': ('Древень-гниль', 'Rot Ent'),
    'dark_witch': ('Ведьма Тьмы', 'Witch of Darkness'),
    'tide_warden': ('Страж приливов', 'Tide Warden'),
    'leviathan': ('Левиафан', 'Leviathan'),
    'deep_priestess': ('Жрица глубин', 'Priestess of the Deep'),
    'smelter_golem': ('Голем-плавильщик', 'Smelter Golem'),
    'fire_twin_ignis': ('Игнис', 'Ignis'),
    'fire_twin_cinder': ('Синдер', 'Cinder'),
    'ash_lord': ('Повелитель Пепла', 'Ash Lord'),
}


def main():
    model_dir = os.path.join(G.ASSETS, 'boss_models')
    tex_dir = os.path.join(G.ASSETS, 'textures', 'entity', 'boss')
    only = None
    if '--only' in sys.argv:
        only = set(sys.argv[sys.argv.index('--only') + 1].split(','))
    previews = []
    for fn in MODELS:
        m = fn()
        if only and m['name'] not in only:
            continue
        cubes = [c for p in m['parts'] for c in p.cubes]
        W = 128
        H = G.pack(cubes, W)
        if H > 128:
            W = 256
            H = G.pack(cubes, W)
        img, glow = G.paint(m, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '.png'), img, W, H)
        write_png(os.path.join(tex_dir, m['name'] + '_glow.png'), glow, W, H)
        json.dump(G.to_java(m, W, H), open(os.path.join(model_dir, m['name'] + '.json'), 'w'), indent=1)
        print(f"{m['name']}: {len(cubes)} cubes, texture {W}x{H}")
        if '--preview' in sys.argv:
            G.preview(m, img, previews)
    item_models = os.path.join(G.ASSETS, 'models', 'item')
    ru_path, en_path = os.path.join(G.ASSETS, 'lang', 'ru_ru.json'), os.path.join(G.ASSETS, 'lang', 'en_us.json')
    ru, en = json.load(open(ru_path, encoding='utf-8')), json.load(open(en_path, encoding='utf-8'))
    for key, (r, e) in NAMES.items():
        ru['entity.litrpg.' + key], en['entity.litrpg.' + key] = r, e
        ru['item.litrpg.' + key + '_spawn_egg'] = 'Яйцо призыва: ' + r
        en['item.litrpg.' + key + '_spawn_egg'] = e + ' Spawn Egg'
        json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(os.path.join(item_models, key + '_spawn_egg.json'), 'w'))
    json.dump(ru, open(ru_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    if previews:
        out = sys.argv[sys.argv.index('--preview') + 1]
        cols = 4
        rows = (len(previews) + cols - 1) // cols
        size = len(previews[0])
        sheet = [[(20, 22, 28, 255)] * (cols * size) for _ in range(rows * size)]
        for i, pv in enumerate(previews):
            ox, oy = (i % cols) * size, (i // cols) * size
            for y in range(size):
                sheet[oy + y][ox:ox + size] = pv[y]
        write_png(out, sheet, cols * size, rows * size)
        print('preview ->', out)


if __name__ == '__main__':
    random.seed(11)
    main()
