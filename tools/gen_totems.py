# -*- coding: utf-8 -*-
"""Модели и текстуры тотемов Каменного тотемщика — по одной на вид.

Собираются той же оснасткой, что и хранители (gen_bosses): кубы, материалы, светящиеся
узоры. Свечение у каждого вида своё, по цвету ауры, — чтобы с другого конца зала было
видно, какой столб стоит.

Запуск: python tools/gen_totems.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import gen_bosses as gb  # noqa: E402

ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg')

# вид → цвет свечения (тот же, что у ауры в игре)
GLOW = {
    'totem_guard': 0x5DADE2,
    'totem_fury': 0xE74C3C,
    'totem_mending': 0x7CFC66,
    'totem_wind': 0xF5E6A8,
}


def base_pillar(name, glow, top_cubes, face_decals, moss=False):
    """Общий ствол: приземистое основание, резной столб, навершие своего вида."""
    body_mat = 'stone_moss' if moss else 'stone'
    foot = gb.Part('foot', 'root', (0, 0, 0), [
        gb.Cube(-6, 0, -6, 12, 4, 12, 'stone_dark'),
        gb.Cube(-5, 4, -5, 10, 3, 10, body_mat),
    ])
    shaft = gb.Part('shaft', 'root', (0, 7, 0), [
        gb.Cube(-4, 7, -4, 8, 10, 8, body_mat, decals=face_decals),
        gb.Cube(-4.5, 17, -4.5, 9, 2, 9, 'stone_dark'),
    ], anim=[gb.A('idle', 'y', 0.02, 0.25)])
    head = gb.Part('head', 'shaft', (0, 19, 0), top_cubes, anim=[gb.A('idle', 'y', 0.05, 0.4)])
    return dict(name=name, scale=1.0, shadow=0.6, parts=[foot, shaft, head])


def totem_guard():
    g = GLOW['totem_guard']
    top = [
        # навершие-щит: широкая плита с горящим кругом
        gb.Cube(-6, 19, -3, 12, 10, 6, 'stone', decals=[
            ('front', 3, 2, 6, 6, g, True), ('front', 4, 3, 4, 4, 0xE8F6FF, True),
            ('back', 3, 2, 6, 6, g, True)]),
        gb.Cube(-7, 21, -2, 1, 6, 4, 'stone_dark'),
        gb.Cube(6, 21, -2, 1, 6, 4, 'stone_dark'),
    ]
    face = [('front', 2, 2, 4, 1, g, True), ('front', 2, 5, 4, 1, g, True), ('front', 2, 8, 4, 1, g, True)]
    return base_pillar('totem_guard', g, top, face)


def totem_fury():
    g = GLOW['totem_fury']
    top = [
        gb.Cube(-4, 19, -4, 8, 8, 8, 'stone', decals=[
            ('front', 2, 2, 4, 4, g, True), ('front', 3, 3, 2, 2, 0xFFE0C0, True)]),
        # рога по бокам
        gb.Cube(-7, 25, -2, 3, 7, 4, 'horn'),
        gb.Cube(4, 25, -2, 3, 7, 4, 'horn'),
        gb.Cube(-2, 27, -2, 4, 4, 4, 'stone_dark', decals=[('front', 1, 1, 2, 2, g, True)]),
    ]
    face = [('front', 1, 3, 6, 1, g, True), ('front', 2, 6, 4, 3, g, True)]
    return base_pillar('totem_fury', g, top, face)


def totem_mending():
    g = GLOW['totem_mending']
    top = [
        gb.Cube(-4, 19, -4, 8, 7, 8, 'stone_moss', decals=[
            ('front', 2, 2, 4, 3, g, True)]),
        # побеги, обвившие навершие
        gb.Cube(-6, 22, -1, 2, 8, 2, 'bark'),
        gb.Cube(4, 22, -1, 2, 8, 2, 'bark'),
        gb.Cube(-3, 26, -3, 6, 3, 6, 'leaf_rot', decals=[('top', 1, 1, 4, 4, g, True)]),
    ]
    face = [('front', 2, 2, 1, 8, g, True), ('front', 5, 2, 1, 8, g, True)]
    return base_pillar('totem_mending', g, top, face, moss=True)


def totem_wind():
    g = GLOW['totem_wind']
    top = [
        gb.Cube(-3, 19, -3, 6, 12, 6, 'stone', decals=[
            ('front', 1, 2, 4, 2, g, True), ('front', 1, 6, 4, 2, g, True)]),
        # перья по бокам: тонкие пластины
        gb.Cube(-7, 24, -1, 4, 2, 2, 'bone'),
        gb.Cube(3, 24, -1, 4, 2, 2, 'bone'),
        gb.Cube(-6, 28, -1, 3, 2, 2, 'bone'),
        gb.Cube(3, 28, -1, 3, 2, 2, 'bone'),
        gb.Cube(-2, 31, -2, 4, 3, 4, 'stone_dark', decals=[('top', 1, 1, 2, 2, g, True)]),
    ]
    face = [('front', 2, 3, 4, 1, g, True), ('front', 1, 7, 6, 1, g, True)]
    return base_pillar('totem_wind', g, top, face)


MODELS = [totem_guard, totem_fury, totem_mending, totem_wind]


def main():
    model_dir = os.path.join(ASSETS, 'boss_models')
    tex_dir = os.path.join(ASSETS, 'textures', 'entity', 'boss')
    os.makedirs(model_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)
    for fn in MODELS:
        m = fn()
        cubes = [c for p in m['parts'] for c in p.cubes]
        W = 64
        H = gb.pack(cubes, W)
        if H > 64:
            W = 128
            H = gb.pack(cubes, W)
        img, glow = gb.paint(m, W, H)
        gb.write_png(os.path.join(tex_dir, m['name'] + '.png'), img, W, H)
        gb.write_png(os.path.join(tex_dir, m['name'] + '_glow.png'), glow, W, H)
        json.dump(gb.to_java(m, W, H), open(os.path.join(model_dir, m['name'] + '.json'), 'w'), indent=1)
        print(f"{m['name']}: {len(cubes)} кубов, текстура {W}x{H}")


if __name__ == '__main__':
    main()
