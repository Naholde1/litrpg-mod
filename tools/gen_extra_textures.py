"""
Дополнительные текстуры: украшения и броня наборов (шаблоны со сменой
палитры), уникальные предметы, камни и текстуры брони на модели (64x32).
Вызывается из gen_item_textures.py.

Шаблоны: 1–3 основной цвет (светлый→тёмный), 4–5 акцент, K — контур.
"""
import os
import random

from png import write_png, hex_rgba, blank

TEMPLATES = {
    'helmet': [
        "................",
        "................",
        "....KKKKKKKK....",
        "...K11111222K...",
        "..K1111222223K..",
        "..K1122222233K..",
        "..K4444444455K..",
        "..K2K555555K3K..",
        "..K2K.....K3K...",
        "..K3K.....K3K...",
        "...KK.....KK....",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    'chestplate': [
        "................",
        ".KKKK......KKKK.",
        "K1112K....K2223K",
        "K1122KKKKKK2233K",
        "K11222222222233K",
        ".KK2224554222KK.",
        "..K2224554223K..",
        "..K1222552223K..",
        "..K1222222223K..",
        "..K1222222233K..",
        "..K4444444455K..",
        "..K2222222233K..",
        "...KKKKKKKKKK...",
        "................",
        "................",
        "................",
    ],
    'leggings': [
        "................",
        "................",
        "...KKKKKKKKKK...",
        "...K44444455K...",
        "...K11222223K...",
        "...K112KK223K...",
        "...K112K.K23K...",
        "...K112K.K23K...",
        "...K122K.K23K...",
        "...K122K.K33K...",
        "...K122K.K33K...",
        "...K222K.K33K...",
        "...KKKKK.KKKK...",
        "................",
        "................",
        "................",
    ],
    'boots': [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..KKKK....KKKK..",
        "..K12K....K23K..",
        "..K12K....K23K..",
        "..K12K....K23K..",
        ".K1122K..K1223K.",
        "K44455K..K44455K",
        "KKKKKKK..KKKKKKK",
        "................",
        "................",
        "................",
    ],
    'ring': [
        "................",
        "................",
        "................",
        "......KKKK......",
        ".....K4554K.....",
        ".....KK55KK.....",
        "....K1K..K3K....",
        "...K1K....K3K...",
        "...K2K....K3K...",
        "...K2K....K3K...",
        "....K2K..K3K....",
        ".....K2223K.....",
        "......KKKK......",
        "................",
        "................",
        "................",
    ],
    'amulet': [
        "....K......K....",
        ".....K....K.....",
        "......K..K......",
        ".......KK.......",
        "......K44K......",
        ".....K4455K.....",
        "....K144553K....",
        "....K144553K....",
        ".....K4553K.....",
        "......K53K......",
        ".......KK.......",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    'belt': [
        "................",
        "................",
        "................",
        "................",
        "................",
        "KKKKKKKKKKKKKKKK",
        "K1111KKKKK11111K",
        "K2222K444K22222K",
        "K2222K4K4K22222K",
        "K3333K444K33333K",
        "KKKKKKKKKKKKKKKK",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
}

SET_PALETTES = {
    'goblin': {'1': 0xA8B87A, '2': 0x6F8A3E, '3': 0x3E5222, '4': 0xD8B070, '5': 0x7A5A2A},
    'bonewarden': {'1': 0xF4EEDC, '2': 0xC9BE9E, '3': 0x8A7F62, '4': 0xC03A3A, '5': 0x5A1414},
    'shadow': {'1': 0x6A5A8E, '2': 0x3A2C58, '3': 0x1C142C, '4': 0xC080FF, '5': 0x6A30B0},
}

TEMPLATED = {
    'copper_ring': ('ring', {'1': 0xF0A070, '2': 0xC06A3A, '3': 0x7A3A1A, '4': 0xE6B3FF, '5': 0x8A44C8}),
    'silver_ring': ('ring', {'1': 0xFFFFFF, '2': 0xC8D0D8, '3': 0x7A8490, '4': 0xBFE8FF, '5': 0x2A6FD6}),
    'gold_ring': ('ring', {'1': 0xFFF0A0, '2': 0xF2C33A, '3': 0x9A6A12, '4': 0xFF9A9A, '5': 0xC01818}),
    'kings_ring': ('ring', {'1': 0xFFF0A0, '2': 0xF2C33A, '3': 0x9A6A12, '4': 0xA8FFB0, '5': 0x1F8A3A}),
    'jade_amulet': ('amulet', {'1': 0xE0E6EC, '3': 0x7A8490, '4': 0x9CF0B0, '5': 0x2E9A5A}),
    'ruby_amulet': ('amulet', {'1': 0xFFF0A0, '3': 0x9A6A12, '4': 0xFF8A8A, '5': 0xB01020}),
    'star_amulet': ('amulet', {'1': 0xFFF0A0, '3': 0x9A6A12, '4': 0xFFFFFF, '5': 0x6FF0E6}),
    'lich_amulet': ('amulet', {'1': 0xF4EEDC, '3': 0x8A7F62, '4': 0xE6B3FF, '5': 0x6A20B0}),
    'heart_of_system': ('amulet', {'1': 0x8FE3FF, '3': 0x2A6FD6, '4': 0xFFFFFF, '5': 0x4FC3F7}),
    'leather_belt': ('belt', {'1': 0xB0804A, '2': 0x8A5A2A, '3': 0x5A3A1A, '4': 0xF2C33A}),
}
for _set, _pal in SET_PALETTES.items():
    for _piece in ('helmet', 'chestplate', 'leggings', 'boots'):
        TEMPLATED[f'{_set}_{_piece}'] = (_piece, _pal)

# Рисунки в общей палитре gen_item_textures.PALETTE
SPRITES = {
    'dawn_blade': [
        "............KKK.",
        "...........KYHYK",
        "..........KYHYK.",
        ".........KYHYoK.",
        "........KYHYoK..",
        ".......KYHYoK...",
        "......KYHYoK....",
        "..KK.KYHYoK.....",
        "..KYKKHYoK......",
        "...KYYooK.......",
        "...KrKKYK.......",
        "..KwvK.KYK......",
        ".KwvK...KK......",
        "KwvK............",
        "KvK.............",
        ".K..............",
    ],
    'frost_sorrow': [
        "............KKK.",
        "...........KTBTK",
        "..........KTBbK.",
        ".........KTBbnK.",
        "........KTBbnK..",
        ".......KTBTnK...",
        "......KTBbnK....",
        "..KK.KTBbnK.....",
        "..KzKKBbnK......",
        "...KzqnnK.......",
        "...KPKKzK.......",
        "..KZzK.KzK......",
        ".KZzK...KK......",
        "KZzK............",
        "KzK.............",
        ".K..............",
    ],
    'archmage_staff': [
        "..........KKKKK.",
        ".........KPHPpqK",
        "........KyKPpqQK",
        "........KyKpqQKK",
        ".......KyK.KKKy.",
        "......KWvKy.....",
        ".....KWvK.......",
        "....KWvK........",
        "...KWvK.........",
        "...KyK..........",
        "..KWvK..........",
        ".KWvK...........",
        ".KyK............",
        "KWvK............",
        "KvK.............",
        ".K..............",
    ],
    'ascension_stone': [
        "................",
        "......KKKK......",
        ".....KTTtxK.....",
        "....KTHTtxXK....",
        "...KTTtyYtxXK...",
        "...KTtyYYytxK...",
        "..KTtxyYyttxXK..",
        "..KttxxytxxXXK..",
        "..KxtxxxxxXXXK..",
        "...KxxxxxXXXK...",
        "...KXxxxXXXXK...",
        "....KXXXXXXK....",
        ".....KKKKKK.....",
        "................",
        "................",
        "................",
    ],
    'reforge_stone': [
        "................",
        "................",
        ".....KKKKKK.....",
        "....KsssiiIK....",
        "...KssoiiiIIK...",
        "...KsiYoiiiIK...",
        "..KsiiiyoiiIIK..",
        "..KsiiiiYoiIIK..",
        "..KiiiiiiyoIIK..",
        "...KiiiiiiIIK...",
        "...KIiiiiIIIK...",
        "....KIIIIIIK....",
        ".....KKKKKK.....",
        "................",
        "................",
        "................",
    ],
}

_FALLBACK = {'2': '1', '4': '5', '5': '4', '1': '2', '3': '2'}


def render_template(name, palette):
    tmpl, pal = TEMPLATED[name]
    out = []
    for r in TEMPLATES[tmpl]:
        assert len(r) == 16, f"{tmpl}: row '{r}' has {len(r)} chars"
        line = []
        for c in r:
            if c == '.':
                line.append((0, 0, 0, 0))
            elif c in pal:
                line.append(hex_rgba(pal[c]))
            elif c in _FALLBACK:
                line.append(hex_rgba(pal.get(_FALLBACK[c], 0x888888)))
            else:
                line.append(hex_rgba(palette[c]))
        out.append(line)
    return out


# ---------------- броня на модели игрока ----------------

def _shade(rgb, k):
    r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
    return (min(255, max(0, int(r * k))), min(255, max(0, int(g * k))), min(255, max(0, int(b * k))), 255)


def _face(img, x0, y0, w, h, pal, rnd, rows=None, trim_row=None, rivets=True):
    """Грань: основной цвет с шумом, светлый верх/лево, тёмный низ/право, полоса отделки, заклёпки."""
    for y in range(h):
        if rows is not None and y not in rows:
            continue
        for x in range(w):
            k = 1.0 + (rnd.random() - 0.5) * 0.12
            if y == 0 or x == 0:
                c = _shade(pal['1'], k)
            elif y == h - 1 or x == w - 1:
                c = _shade(pal['3'], k)
            else:
                c = _shade(pal['2'], k)
            if trim_row is not None and y == trim_row:
                c = _shade(pal['4'], k)
            img[y0 + y][x0 + x] = c
    if rivets and w >= 4 and h >= 4:
        for (rx, ry) in ((1, 1), (w - 2, 1)):
            if rows is None or ry in rows:
                img[y0 + ry][x0 + rx] = _shade(pal['5'], 1.0)


def _box(img, u, v, w, h, d, pal, rnd, rows=None, trim_row=None):
    """Развёртка коробки модели Minecraft: (u, v) — левый верхний угол развёртки."""
    _face(img, u + d, v, w, d, pal, rnd, rivets=False)
    _face(img, u + d + w, v, w, d, pal, rnd, rivets=False)
    _face(img, u, v + d, d, h, pal, rnd, rows, trim_row)
    _face(img, u + d, v + d, w, h, pal, rnd, rows, trim_row)
    _face(img, u + d + w, v + d, d, h, pal, rnd, rows, trim_row)
    _face(img, u + d + w + d, v + d, w, h, pal, rnd, rows, trim_row)


def armor_layers(setn, pal):
    rnd = random.Random(setn)
    l1 = blank(64, 32)
    l2 = blank(64, 32)
    _box(l1, 0, 0, 8, 8, 8, pal, rnd, trim_row=5)                       # шлем
    for x in (10, 11, 12, 13):
        l1[12][x] = _shade(pal['5'], 0.6)                                # смотровая щель
    _box(l1, 16, 16, 8, 12, 4, pal, rnd, trim_row=9)                    # нагрудник
    _box(l1, 40, 16, 4, 12, 4, pal, rnd, rows=range(0, 6), trim_row=0)  # наплечники
    for (x, y) in ((23, 23), (24, 23), (23, 24), (24, 24)):
        l1[y][x] = _shade(pal['4'], 1.1)                                 # эмблема
    _box(l1, 0, 16, 4, 12, 4, pal, rnd, rows=range(8, 12), trim_row=8)  # ботинки
    _box(l2, 16, 16, 8, 12, 4, pal, rnd, rows=range(8, 12), trim_row=8) # пояс поножей
    _box(l2, 0, 16, 4, 12, 4, pal, rnd, rows=range(0, 10))              # поножи
    return l1, l2


def generate(palette, rendered, render_sprite):
    base = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg', 'textures')
    for name in TEMPLATED:
        img = render_template(name, palette)
        write_png(os.path.join(base, 'item', name + '.png'), img, 16, 16)
        rendered[name] = img
    for name, rows in SPRITES.items():
        img = render_sprite(rows)
        write_png(os.path.join(base, 'item', name + '.png'), img, 16, 16)
        rendered[name] = img
    armor_dir = os.path.join(base, 'models', 'armor')
    os.makedirs(armor_dir, exist_ok=True)
    for setn, pal in SET_PALETTES.items():
        l1, l2 = armor_layers(setn, pal)
        write_png(os.path.join(armor_dir, setn + '_layer_1.png'), l1, 64, 32)
        write_png(os.path.join(armor_dir, setn + '_layer_2.png'), l2, 64, 32)
