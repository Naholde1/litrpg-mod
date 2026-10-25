"""
Блоки подземелья: разлом (переход между этапами), рунный диск (6 символов), поворотное зеркало, тайник.
Текстуры 16×16 рисуются кодом, плюс blockstates, модели блоков и предметов, названия.
Запуск: python tools/gen_dungeon_blocks.py [--preview build/preview/blocks.png]
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png, scale  # noqa: E402

ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg')
TEX = os.path.join(ASSETS, 'textures', 'block')


def canvas(c=(0, 0, 0, 0)):
    return [[c for _ in range(16)] for _ in range(16)]


def h(x, y, s=0):
    n = (x * 374761393 + y * 668265263 + s * 2654435761) & 0xFFFFFFFF
    n = (n ^ (n >> 13)) * 1274126177 & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000 / 1000


def shade(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


def stone_frame(seed, base=(46, 44, 54, 255)):
    """Полированный чёрный камень с рамкой."""
    p = canvas()
    for y in range(16):
        for x in range(16):
            k = 0.85 + h(x, y, seed) * 0.3
            c = shade(base, k)
            if x in (0, 15) or y in (0, 15):
                c = shade(base, 0.6)
            elif x in (1, 14) or y in (1, 14):
                c = shade(base, 1.25)
            p[y][x] = c
    return p


# 6 символов 8×8 (по строкам): огонь, вода, луна, солнце, череп, око
SYMBOLS = [
    ['...#....', '..##....', '..###.#.', '.#####..', '.######.', '########', '.######.', '..####..'],
    ['........', '##..##..', '.##..##.', '..##..##', '........', '##..##..', '.##..##.', '..##..##'],
    ['..####..', '.##.....', '##......', '##......', '##......', '##......', '.##.....', '..####..'],
    ['#..#..#.', '.#.#.#..', '..###...', '#######.', '..###...', '.#.#.#..', '#..#..#.', '........'],
    ['.######.', '########', '##.##.##', '########', '.######.', '..#..#..', '..####..', '........'],
    ['........', '..####..', '.#....#.', '#..##..#', '#..##..#', '.#....#.', '..####..', '........'],
]
SYM_COLORS = [(255, 120, 40), (80, 170, 255), (200, 210, 255), (255, 220, 80), (230, 230, 220), (180, 90, 255)]


def dial_face(i):
    p = stone_frame(10 + i)
    col = SYM_COLORS[i]
    for yy, row in enumerate(SYMBOLS[i]):
        for xx, ch in enumerate(row):
            if ch == '#':
                p[4 + yy][4 + xx] = (col[0], col[1], col[2], 255)
    return p


def rift():
    p = canvas()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            sw = math.sin(d * 1.3 - math.atan2(y - 7.5, x - 7.5) * 2) * 0.5 + 0.5
            k = 0.4 + 0.6 * sw * (0.7 + 0.3 * h(x, y, 3))
            p[y][x] = (int(120 + 120 * k), int(30 + 60 * k * k), int(180 + 70 * k), 190)
    return p


def mirror_tex():
    p = canvas()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                p[y][x] = (90, 70, 40, 255)
            else:
                k = 0.75 + 0.25 * math.sin((x + y) * 0.6) + h(x, y, 7) * 0.1
                p[y][x] = (int(170 * k), int(200 * k), int(225 * k), 255)
    for i in range(3, 13):
        p[i][i] = (250, 255, 255, 255)
        if i + 1 < 15:
            p[i][i + 1] = (230, 245, 255, 255)
    return p


def stand_tex():
    p = canvas()
    for y in range(16):
        for x in range(16):
            p[y][x] = shade((110, 80, 45, 255), 0.85 + h(x, y, 9) * 0.3)
    return p


def cache_side():
    p = canvas()
    for y in range(16):
        for x in range(16):
            c = shade((95, 60, 30, 255), 0.8 + h(x, y, 11) * 0.4)
            if y in (3, 12) or x in (0, 15):
                c = (60, 55, 50, 255)
            if y in (4, 11) and x % 5 == 2:
                c = (210, 170, 60, 255)
            p[y][x] = c
    for x in (6, 7, 8, 9):
        for y in (6, 7, 8):
            p[y][x] = (230, 190, 70, 255)
    p[7][7] = p[7][8] = (40, 30, 20, 255)
    return p


def cache_top():
    p = canvas()
    for y in range(16):
        for x in range(16):
            c = shade((105, 68, 34, 255), 0.8 + h(x, y, 13) * 0.4)
            if x in (0, 15) or y in (0, 15):
                c = (60, 55, 50, 255)
            if (x - 7.5) ** 2 + (y - 7.5) ** 2 < 6:
                c = (240, 200, 80, 255)
            p[y][x] = c
    return p


def save(name, px):
    os.makedirs(TEX, exist_ok=True)
    write_png(os.path.join(TEX, name + '.png'), px, 16, 16)


def jdump(path, obj):
    path = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=1)


def main():
    side = stone_frame(5)
    save('rune_dial_side', side)
    for i in range(6):
        save(f'rune_dial_{i}', dial_face(i))
        jdump(f'models/block/rune_dial_{i}.json', {'parent': 'minecraft:block/orientable', 'textures': {
            'front': f'litrpg:block/rune_dial_{i}', 'side': 'litrpg:block/rune_dial_side', 'top': 'litrpg:block/rune_dial_side'}})
    rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
    jdump('blockstates/rune_dial.json', {'variants': {f'facing={f},symbol={i}': {'model': f'litrpg:block/rune_dial_{i}', 'y': r}
                                                      for f, r in rot.items() for i in range(6)}})
    jdump('models/item/rune_dial.json', {'parent': 'litrpg:block/rune_dial_2'})

    save('rift', rift())
    jdump('models/block/rift.json', {'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                                     'textures': {'all': 'litrpg:block/rift'}})
    jdump('blockstates/rift.json', {'variants': {'': {'model': 'litrpg:block/rift'}}})
    jdump('models/item/rift.json', {'parent': 'litrpg:block/rift'})

    save('mirror', mirror_tex())
    save('mirror_stand', stand_tex())
    # зеркало: подставка + диагональная пластина (две ориентации)
    for t, ang in ((0, 45), (1, -45)):
        jdump(f'models/block/mirror_{t}.json', {
            'parent': 'minecraft:block/block', 'render_type': 'minecraft:cutout',
            'textures': {'particle': 'litrpg:block/mirror', 'glass': 'litrpg:block/mirror', 'wood': 'litrpg:block/mirror_stand'},
            'elements': [
                {'from': [3, 0, 3], 'to': [13, 2, 13], 'faces': {f: {'texture': '#wood'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
                {'from': [7, 2, 7], 'to': [9, 4, 9], 'faces': {f: {'texture': '#wood'} for f in ('north', 'south', 'east', 'west', 'up')}},
                {'from': [0, 4, 7.5], 'to': [16, 15, 8.5], 'rotation': {'origin': [8, 8, 8], 'axis': 'y', 'angle': ang},
                 'faces': {f: {'texture': '#glass'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}},
            ]})
    jdump('blockstates/mirror.json', {'variants': {f'turn={t}': {'model': f'litrpg:block/mirror_{t}'} for t in (0, 1)}})
    jdump('models/item/mirror.json', {'parent': 'litrpg:block/mirror_0'})

    save('cache_side', cache_side())
    save('cache_top', cache_top())
    jdump('models/block/cache.json', {
        'parent': 'minecraft:block/block', 'textures': {'particle': 'litrpg:block/cache_side', 'side': 'litrpg:block/cache_side',
                                                        'top': 'litrpg:block/cache_top'},
        'elements': [{'from': [1, 0, 1], 'to': [15, 14, 15], 'faces': {
            'north': {'texture': '#side'}, 'south': {'texture': '#side'}, 'east': {'texture': '#side'}, 'west': {'texture': '#side'},
            'up': {'texture': '#top'}, 'down': {'texture': '#top'}}}]})
    jdump('blockstates/cache.json', {'variants': {f'facing={f}': {'model': 'litrpg:block/cache', 'y': r} for f, r in rot.items()}})
    jdump('models/item/cache.json', {'parent': 'litrpg:block/cache'})

    for lang in ('ru_ru', 'en_us'):
        p = os.path.join(ASSETS, 'lang', lang + '.json')
        d = json.load(open(p, encoding='utf-8'))
        ru = lang == 'ru_ru'
        d['block.litrpg.rune_dial'] = 'Рунный диск' if ru else 'Rune Dial'
        d['block.litrpg.mirror'] = 'Поворотное зеркало' if ru else 'Turning Mirror'
        d['block.litrpg.cache'] = 'Тайник' if ru else 'Cache'
        d['block.litrpg.rift'] = 'Разлом' if ru else 'Rift'
        d['entity.litrpg.trader'] = 'Торговец Привала' if ru else 'Camp Trader'
        d['item.litrpg.trader_spawn_egg'] = 'Яйцо призыва торговца' if ru else 'Trader Spawn Egg'
        d['item.litrpg.health_potion'] = 'Эликсир жизни' if ru else 'Elixir of Life'
        d['key.litrpg.dash'] = 'Рывок' if ru else 'Dash'
        for i in range(7, 9):
            d[f'key.litrpg.ability{i}'] = f'Навык {i}' if ru else f'Skill {i}'
        for i in range(1, 6):
            d[f'key.litrpg.focus{i}'] = f'Выбрать союзника {i}' if ru else f'Focus ally {i}'
        d['key.litrpg.focus_clear'] = 'Снять выбор союзника' if ru else 'Clear ally focus'
        names = {'vestibule': ('Преддверие', 'Vestibule'), 'crypt': ('Склеп Забытых', 'Crypt of the Forgotten'),
                 'thicket': ('Паучья чаща', 'Spider Thicket'), 'temple': ('Затопленный храм', 'Sunken Temple'),
                 'forges': ('Кузни Пепла', 'Ash Forges'), 'spire': ('Шпиль Бездны', 'Abyss Spire')}
        for k, (r, e) in names.items():
            d[f'biome.litrpg.{k}'] = r if ru else e
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(d, f, ensure_ascii=False, indent=2)
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        tiles = [rift(), mirror_tex(), cache_side(), cache_top()] + [dial_face(i) for i in range(6)]
        row = [[px for t in tiles for px in t[y]] for y in range(16)]
        write_png(out, scale(row, 6), 16 * len(tiles) * 6, 16 * 6)
    print('dungeon blocks ok')


if __name__ == '__main__':
    main()
