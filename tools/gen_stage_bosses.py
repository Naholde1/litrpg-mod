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


MODELS = [gatekeeper, twin_morra, twin_vessa, lich_king]

NAMES = {
    'gatekeeper': ('Привратник Костей', 'Bone Gatekeeper'),
    'twin_morra': ('Морра, Повелительница Костей', 'Morra, Mistress of Bones'),
    'twin_vessa': ('Весса, Кровавая Ведьма', 'Vessa, the Blood Witch'),
    'lich_king': ('Король-лич', 'The Lich King'),
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
