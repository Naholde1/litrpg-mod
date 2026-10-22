"""
Блоки арен хранителей: «Узел силы» (litrpg:relic, 9 видов) и «Рунная преграда» (litrpg:seal_door),
урон хранителей (damage_type litrpg:boss) и названия.
Запуск: python tools/gen_arena_blocks.py [--preview build/preview/relics.png]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png  # noqa: E402
from gen_bosses import fbm, h2, mix, mul  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', 'litrpg')
DATA = os.path.join(ROOT, 'data')

# вид: (оправа тёмная, оправа светлая, ядро, сияние, узор)
KINDS = {
    'soul': ((30, 26, 40), (70, 62, 88), (60, 220, 255), (200, 250, 255), 'skull'),
    'frost': ((40, 70, 100), (150, 200, 230), (120, 220, 255), (240, 255, 255), 'flake'),
    'fire': ((40, 20, 16), (90, 50, 36), (255, 120, 20), (255, 240, 140), 'flame'),
    'nature': ((50, 36, 22), (100, 76, 44), (140, 230, 60), (230, 255, 160), 'leaf'),
    'web': ((60, 60, 64), (180, 180, 186), (200, 60, 90), (255, 200, 210), 'web'),
    'rune': ((44, 44, 54), (96, 96, 112), (80, 170, 255), (210, 240, 255), 'rune'),
    'time': ((70, 56, 30), (190, 160, 90), (255, 220, 120), (255, 255, 230), 'glass'),
    'gold': ((90, 64, 20), (220, 180, 60), (255, 230, 90), (255, 255, 200), 'coin'),
    'void': ((14, 6, 24), (50, 30, 70), (170, 60, 255), (240, 200, 255), 'eye'),
}


def pattern(kind, x, y):
    cx, cy = x - 7.5, y - 7.5
    r = math.hypot(cx, cy)
    a = math.atan2(cy, cx)
    if kind == 'skull':
        return (r < 3.6 and not (abs(cx + 1.5) < 1 and abs(cy + 0.5) < 1) and not (abs(cx - 1.5) < 1 and abs(cy + 0.5) < 1)
                and not (cy > 1.5 and abs(cx) % 2 < 1))
    if kind == 'flake':
        return r < 5 and (abs(cx) < 0.7 or abs(cy) < 0.7 or abs(abs(cx) - abs(cy)) < 0.8)
    if kind == 'flame':
        return r < 5.2 and cy > -5 + abs(cx) * 1.2 - (1 if (x + y) % 3 == 0 else 0)
    if kind == 'leaf':
        return abs(cx) < 4 - abs(cy) * 0.6 and abs(cy) < 5
    if kind == 'web':
        return r < 6 and (abs(math.sin(a * 4)) < 0.25 or abs(r - 2.5) < 0.5 or abs(r - 4.8) < 0.5)
    if kind == 'rune':
        return (abs(cx) < 0.7 and abs(cy) < 5) or (abs(cy - cx * 0.8 + 1) < 0.8 and abs(cx) < 3.5) or abs(r - 5.3) < 0.5
    if kind == 'glass':
        return abs(cy) < 5.5 and abs(cx) < 0.5 + abs(cy) * 0.7 and not (abs(cy) < 0.6 and abs(cx) > 0.5)
    if kind == 'coin':
        return r < 4.5 and not (r < 3 and abs(cx) > 0.6)
    if kind == 'eye':
        return (abs(cy) < 2.8 - cx * cx / 9 and abs(cx) < 5) and not (r < 1.2)
    return r < 3


def relic(kind):
    dark, light, core, shine, pat = KINDS[kind]
    side = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    top = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 31 + len(kind))
            frame = mix(dark, light, n)
            edge = x in (0, 15) or y in (0, 15)
            inner = 2 <= x <= 13 and 2 <= y <= 13
            if edge:
                c = mul(light, 0.85)
            elif not inner:
                c = mix(frame, light, 0.35) if (x + y) % 4 == 0 else frame
            else:
                r = math.hypot(x - 7.5, y - 7.5)
                c = mix(core, mul(core, 0.35), r / 8)
                if pattern(pat, x, y):
                    c = mix(shine, core, r / 9)
                if h2(x, y, 7) > 0.92:
                    c = shine
            side[y][x] = c + (255,)
            tr = math.hypot(x - 7.5, y - 7.5)
            tc = c if inner else frame
            if 4.5 < tr < 5.6:
                tc = core
            top[y][x] = tc + (255,)
    return side, top


def seal_door():
    img = [[(0, 0, 0, 255)] * 16 for _ in range(16)]
    base, dark, light, rune = (40, 44, 60), (20, 22, 34), (80, 90, 120), (90, 210, 255)
    for y in range(16):
        for x in range(16):
            n = fbm(x, y, 77)
            c = mix(dark, base, n * 1.6) if n < 0.6 else mix(base, light, (n - 0.6) * 2)
            if x in (0, 15) or y in (0, 15):
                c = mul(c, 0.6)
            if (x - 7.5) ** 2 / 30 + (y - 7.5) ** 2 / 40 < 1 and abs(math.hypot(x - 7.5, y - 7.5) - 4.6) < 0.6:
                c = rune
            if abs(x - 7.5) < 0.6 and 3 < y < 12 or abs(y - 7.5) < 0.6 and 5 < x < 10:
                c = mix(rune, (220, 250, 255), 0.4)
            img[y][x] = c + (255,)
    return img


def main():
    block_dir = os.path.join(ASSETS, 'textures', 'block')
    os.makedirs(block_dir, exist_ok=True)
    variants = {}
    for kind in KINDS:
        side, top = relic(kind)
        write_png(os.path.join(block_dir, f'relic_{kind}.png'), side, 16, 16)
        write_png(os.path.join(block_dir, f'relic_{kind}_top.png'), top, 16, 16)
        json.dump({"parent": "minecraft:block/cube_column", "textures": {
            "side": f"litrpg:block/relic_{kind}", "end": f"litrpg:block/relic_{kind}_top"}},
            open(os.path.join(ASSETS, 'models', 'block', f'relic_{kind}.json'), 'w'), indent=1)
        variants[f'kind={kind}'] = {"model": f"litrpg:block/relic_{kind}"}
    json.dump({"variants": variants}, open(os.path.join(ASSETS, 'blockstates', 'relic.json'), 'w'), indent=1)
    json.dump({"parent": "litrpg:block/relic_soul"}, open(os.path.join(ASSETS, 'models', 'item', 'relic.json'), 'w'))

    write_png(os.path.join(block_dir, 'seal_door.png'), seal_door(), 16, 16)
    json.dump({"parent": "minecraft:block/cube_all", "textures": {"all": "litrpg:block/seal_door"}},
              open(os.path.join(ASSETS, 'models', 'block', 'seal_door.json'), 'w'), indent=1)
    json.dump({"variants": {f"stage={i}": {"model": "litrpg:block/seal_door"} for i in range(4)}},
              open(os.path.join(ASSETS, 'blockstates', 'seal_door.json'), 'w'), indent=1)
    json.dump({"parent": "litrpg:block/seal_door"}, open(os.path.join(ASSETS, 'models', 'item', 'seal_door.json'), 'w'))

    # урон хранителей: мимо брони и щита (их учитывает мод сам — BossDamage)
    dt = os.path.join(DATA, 'litrpg', 'damage_type')
    os.makedirs(dt, exist_ok=True)
    json.dump({"message_id": "litrpg.boss", "exhaustion": 0.1, "scaling": "never"}, open(os.path.join(dt, 'boss.json'), 'w'), indent=1)
    tags = os.path.join(DATA, 'minecraft', 'tags', 'damage_type')
    os.makedirs(tags, exist_ok=True)
    for tag in ('bypasses_armor', 'bypasses_shield'):
        json.dump({"replace": False, "values": ["litrpg:boss"]}, open(os.path.join(tags, tag + '.json'), 'w'), indent=1)

    ru_path, en_path = os.path.join(ASSETS, 'lang', 'ru_ru.json'), os.path.join(ASSETS, 'lang', 'en_us.json')
    ru = json.load(open(ru_path, encoding='utf-8'))
    en = json.load(open(en_path, encoding='utf-8'))
    ru['block.litrpg.relic'], en['block.litrpg.relic'] = 'Узел силы', 'Power Node'
    ru['block.litrpg.seal_door'], en['block.litrpg.seal_door'] = 'Рунная преграда', 'Rune Barrier'
    ru['death.attack.litrpg.boss'] = '%1$s пал от руки %2$s'
    ru['death.attack.litrpg.boss.player'] = '%1$s пал от руки %2$s'
    en['death.attack.litrpg.boss'] = '%1$s was crushed by %2$s'
    en['death.attack.litrpg.boss.player'] = '%1$s was crushed by %2$s'
    json.dump(ru, open(ru_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(en_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        S = 8
        W = 16 * S * (len(KINDS) + 1)
        img = [[(20, 20, 24, 255)] * W for _ in range(16 * S * 2)]
        tiles = [relic(k) for k in KINDS] + [(seal_door(), seal_door())]
        for i, (a, b) in enumerate(tiles):
            for row, t in ((0, a), (1, b)):
                for y in range(16 * S):
                    for x in range(16 * S):
                        img[row * 16 * S + y][i * 16 * S + x] = t[y // S][x // S]
        write_png(out, img, W, 16 * S * 2)
    print('relics', len(KINDS), 'ok')


if __name__ == '__main__':
    main()
