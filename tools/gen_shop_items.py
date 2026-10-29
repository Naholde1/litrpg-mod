"""
Текстуры и модели предметов лавки привала: ключ тайника, свитки следа и свитки титула.

Рисуются пиксель-артом 16×16 на палитре gen_item_textures (тот же контур и те же оттенки),
модели — простые item/generated. Запуск: python tools/gen_shop_items.py [--preview out.png]
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png, hex_rgba, scale  # noqa: E402
from gen_item_textures import PALETTE  # noqa: E402

SPRITES = {
    # костяной ключ с бирюзовым камнем в кольце
    'cache_key': [
        "................",
        "................",
        "....KKKK........",
        "...KEeeEK.......",
        "..KEKKKKeK......",
        "..KeKttKeK......",
        "..KeKTxKuK......",
        "..KEKKKKuK......",
        "...KeuuUKK......",
        "....KKKKuUK.....",
        "........KuUK....",
        ".........KuUKKK.",
        "..........KuUuK.",
        "...........KUKK.",
        "............KuK.",
        ".............K..",
    ],
    # свиток следа: свёрнутый пергамент, на ленте — искры
    'trail_scroll': [
        "................",
        "..........Y.....",
        "...KKKKKKKK..y..",
        "..KcAaaaaaAK....",
        "..KCcaaaaacK.Y..",
        "...KKKKKKKKKK...",
        "....KAaaaaaaK...",
        "....KaapPpaaK...",
        "....KapPYPpaK...",
        "....KaapPpaaK...",
        "....KAaaaaaaK...",
        "...KKKKKKKKKK...",
        "..KcAaaaaaAK....",
        "..KCcaaaaacK....",
        "...KKKKKKKK.....",
        "................",
    ],
    # свиток титула: печать золотом и алая лента
    'title_scroll': [
        "................",
        "...KKKKKKKKK....",
        "..KcAaaaaaaAK...",
        "..KCcaaaaaacK...",
        "...KKKKKKKKKK...",
        "....KAaaaaaaK...",
        "....KaaYYYaaK...",
        "....KaYyoyYaK...",
        "....KaaYYYaaK...",
        "....KAaaraaaK...",
        "....KaarRraaK...",
        "...KKKKrdrKKK...",
        "..KcAaKrdrKAK...",
        "..KCcaKd.dKcK...",
        "...KKKK...KK....",
        "................",
    ],
}


def render(rows):
    assert len(rows) == 16, f"{len(rows)} rows"
    out = []
    for r in rows:
        assert len(r) == 16, f"row '{r}' has {len(r)} chars"
        out.append([(0, 0, 0, 0) if PALETTE[c] is None else hex_rgba(PALETTE[c]) for c in r])
    return out


def main():
    res = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg')
    rendered = {}
    for name, rows in SPRITES.items():
        rendered[name] = render(rows)
        write_png(os.path.join(res, 'textures', 'item', name + '.png'), rendered[name], 16, 16)
        with open(os.path.join(res, 'models', 'item', name + '.json'), 'w', encoding='utf-8') as f:
            json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'litrpg:item/' + name}}, f)
    names = {'cache_key': ('Ключ тайника', 'Cache Key'), 'trail_scroll': ('Свиток следа', 'Trail Scroll'),
             'title_scroll': ('Свиток титула', 'Title Scroll')}
    for lang, idx in (('ru_ru', 0), ('en_us', 1)):
        path = os.path.join(res, 'lang', lang + '.json')
        with open(path, encoding='utf-8') as f:
            data = json.load(f)
        for k, v in names.items():
            data['item.litrpg.' + k] = v[idx]
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        cell = 16 * 8 + 8
        sheet = [[(30, 32, 38, 255)] * (len(rendered) * cell) for _ in range(cell)]
        for i, n in enumerate(rendered):
            big = scale(rendered[n], 8)
            for y, row in enumerate(big):
                for x, p in enumerate(row):
                    sheet[4 + y][i * cell + 4 + x] = p
        write_png(out, sheet, len(rendered) * cell, cell)
    print(f"written {len(rendered)} shop item textures")


if __name__ == '__main__':
    main()
