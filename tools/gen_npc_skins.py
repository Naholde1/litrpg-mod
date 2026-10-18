"""
Скины жителей рас (64x64, раскладка скина игрока). Каждый скин собирается
из палитры расы (кожа, волосы, глаза) и одежды по роли (латы, капюшон,
мантия, фартук, корона). Запуск: python tools/gen_npc_skins.py [--preview out.png]
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png, blank, scale  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg', 'textures', 'entity', 'npc')

# (u, v, w, h, d) развёрток частей модели игрока
BOX = {
    'head': (0, 0, 8, 8, 8), 'hat': (32, 0, 8, 8, 8),
    'body': (16, 16, 8, 12, 4), 'jacket': (16, 32, 8, 12, 4),
    'rarm': (40, 16, 4, 12, 4), 'rsleeve': (40, 32, 4, 12, 4),
    'larm': (32, 48, 4, 12, 4), 'lsleeve': (48, 48, 4, 12, 4),
    'rleg': (0, 16, 4, 12, 4), 'rpants': (0, 32, 4, 12, 4),
    'lleg': (16, 48, 4, 12, 4), 'lpants': (0, 48, 4, 12, 4),
}


def faces(part):
    """Грани части: имя → (x0, y0, w, h)."""
    u, v, w, h, d = BOX[part]
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h), 'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h),
    }


class Skin:
    def __init__(self, seed):
        self.img = blank(64, 64)
        self.rnd = random.Random(seed)

    def px(self, x, y, rgb, noise=0.08, alpha=255):
        k = 1 + (self.rnd.random() - 0.5) * 2 * noise
        r, g, b = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
        self.img[y][x] = (min(255, max(0, int(r * k))), min(255, max(0, int(g * k))), min(255, max(0, int(b * k))), alpha)

    def fill(self, part, rgb, rows=None, face_names=None, shade=True, noise=0.08):
        """Залить часть цветом с объёмом: верх светлее, низ и бока темнее."""
        for name, (x0, y0, w, h) in faces(part).items():
            if face_names and name not in face_names:
                continue
            for y in range(h):
                if rows is not None and name not in ('top', 'bottom') and y not in rows:
                    continue
                if rows is not None and name == 'top' and 0 not in rows:
                    continue
                if rows is not None and name == 'bottom' and (h - 1) not in rows:
                    continue
                for x in range(w):
                    c = rgb
                    if shade:
                        f = 1.0
                        if name in ('left', 'back'):
                            f = 0.86
                        if name == 'bottom':
                            f = 0.75
                        if name == 'top':
                            f = 1.08
                        if name not in ('top', 'bottom'):
                            f *= 1.05 - 0.12 * (y / max(1, h - 1))
                        c = mul(rgb, f)
                    self.px(x0 + x, y0 + y, c, noise)

    def face_px(self, part, face, x, y, rgb, noise=0.03):
        x0, y0, w, h = faces(part)[face]
        if 0 <= x < w and 0 <= y < h:
            self.px(x0 + x, y0 + y, rgb, noise)

    def row(self, part, y, rgb, face_names=('front', 'right', 'left', 'back')):
        for name in face_names:
            x0, y0, w, h = faces(part)[name]
            for x in range(w):
                self.px(x0 + x, y0 + y, rgb, 0.06)


def mul(rgb, f):
    r, g, b = ((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255)
    return (min(255, int(r * f)) << 16) | (min(255, int(g * f)) << 8) | min(255, int(b * f))


RACES = {
    'elf': dict(skin=0xF2D3B5, hair=0xF0E0A0, eyes=0x3AA04A, cloth=0x4A8A4A, cloth2=0x8CC870, leather=0x8A5A2A,
                metal=0xD8E8D0, metal2=0x8CC870, magic=0x2E7A5A, trim=0xF2E080, hair_style='long'),
    'dwarf': dict(skin=0xE0A880, hair=0xB0602A, eyes=0x3A6AC0, cloth=0x3A5A9A, cloth2=0x6A8AC0, leather=0x6A4422,
                  metal=0xB8C0C8, metal2=0xD0A040, magic=0x5A3A8A, trim=0xF2C33A, hair_style='beard'),
    'orc': dict(skin=0x6FA34A, hair=0x1C1C24, eyes=0xE04020, cloth=0x7A3A2A, cloth2=0xA05A3A, leather=0x5A3A1A,
                metal=0x5A5A64, metal2=0xA02020, magic=0x8A5A2A, trim=0xC8B070, hair_style='mohawk'),
    'beastkin': dict(skin=0xC9A36B, hair=0x8E6E3B, eyes=0xF0A020, cloth=0x6A5232, cloth2=0x9A7A4A, leather=0x5A4220,
                     metal=0xE8DCC0, metal2=0x8A7F62, magic=0x3A6A8A, trim=0xE8DCC0, hair_style='fur'),
    'demonkin': dict(skin=0xB8404A, hair=0x18141C, eyes=0xFFD040, cloth=0x2A2030, cloth2=0x6A1A2A, leather=0x2A1A1A,
                     metal=0x3A3440, metal2=0xC03020, magic=0x5A1030, trim=0xFF9020, hair_style='short'),
}


def paint(race, role, seed):
    p = RACES[race]
    s = Skin(seed)
    # кожа
    for part in ('head', 'body', 'rarm', 'larm', 'rleg', 'lleg'):
        s.fill(part, p['skin'], noise=0.05)
    if race == 'beastkin':
        # светлая морда
        for x in range(2, 6):
            for y in range(4, 8):
                s.face_px('head', 'front', x, y, mul(p['skin'], 1.25))
        s.face_px('head', 'front', 3, 5, 0x2A1A10)
        s.face_px('head', 'front', 4, 5, 0x2A1A10)
    # глаза и брови
    for ex in (1, 5):
        s.face_px('head', 'front', ex, 4, 0xFFFFFF)
        s.face_px('head', 'front', ex + 1, 4, p['eyes'])
        s.face_px('head', 'front', ex, 3, mul(p['hair'], 0.8))
        s.face_px('head', 'front', ex + 1, 3, mul(p['hair'], 0.8))
    if race != 'beastkin':
        s.face_px('head', 'front', 3, 6, mul(p['skin'], 0.75))
        s.face_px('head', 'front', 4, 6, mul(p['skin'], 0.75))
    # волосы
    s.fill('head', p['hair'], face_names=('top',))
    style = p['hair_style']
    hair_rows = {'long': range(0, 8), 'beard': range(0, 2), 'mohawk': range(0, 1), 'fur': range(0, 3), 'short': range(0, 2)}[style]
    for name in ('right', 'left', 'back'):
        for y in hair_rows:
            x0, y0, w, h = faces('head')[name]
            for x in range(w):
                if style == 'mohawk' and name != 'back':
                    continue
                s.px(x0 + x, y0 + y, p['hair'], 0.1)
    for x in range(8):
        s.face_px('head', 'front', x, 0, p['hair'])
        if style in ('long', 'fur'):
            s.face_px('head', 'front', x, 1, p['hair']) if x in (0, 7) else None
    if style == 'beard':
        for y in range(5, 8):
            for x in range(8):
                if not (y == 5 and 2 < x < 5):
                    s.face_px('head', 'front', x, y, p['hair'])
        for name in ('right', 'left'):
            for y in range(4, 8):
                for x in range(8):
                    s.face_px('head', name, x, y, p['hair'])
    # одежда по роли
    cloth, cloth2, leather, metal, metal2, magic, trim = (p['cloth'], p['cloth2'], p['leather'], p['metal'], p['metal2'], p['magic'], p['trim'])
    if role in ('warrior', 'chief'):
        s.fill('body', metal)
        s.fill('jacket', metal2, rows=range(0, 2))
        for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
            s.fill(arm, metal, rows=range(0, 5))
            s.fill(sleeve, metal2, rows=range(0, 2))
            s.fill(arm, leather, rows=range(9, 12))
        for leg, pants in (('rleg', 'rpants'), ('lleg', 'lpants')):
            s.fill(leg, cloth, rows=range(0, 8))
            s.fill(leg, metal, rows=range(8, 12))
        s.row('body', 9, leather)
        for x in range(2, 6):
            s.face_px('body', 'front', x, 3, metal2)
            s.face_px('body', 'front', x, 4, metal2)
        if role == 'chief':
            # корона / рогатый шлем и плащ-оторочка
            s.fill('hat', trim, rows=range(0, 2), face_names=('front', 'right', 'left', 'back'))
            for x in (0, 2, 5, 7):
                s.face_px('hat', 'front', x, 0, 0xFF4040 if race == 'orc' else 0xFFFFFF)
            s.fill('jacket', mul(trim, 0.8), rows=range(10, 12))
    elif role == 'archer':
        s.fill('body', leather)
        s.fill('rarm', leather, rows=range(0, 4))
        s.fill('larm', leather, rows=range(0, 4))
        s.fill('rarm', cloth2, rows=range(8, 12))
        s.fill('larm', cloth2, rows=range(8, 12))
        for leg in ('rleg', 'lleg'):
            s.fill(leg, cloth, rows=range(0, 9))
            s.fill(leg, leather, rows=range(9, 12))
        s.row('body', 9, mul(leather, 0.7))
        # перевязь колчана
        for i in range(8):
            s.face_px('body', 'front', i, i + 1, mul(leather, 0.6))
        # капюшон
        s.fill('hat', cloth, face_names=('top', 'right', 'left', 'back'))
        s.fill('hat', cloth, rows=range(0, 2), face_names=('front',))
        for y in range(2, 8):
            s.face_px('hat', 'front', 0, y, cloth)
            s.face_px('hat', 'front', 7, y, cloth)
    elif role == 'mage':
        s.fill('body', magic)
        s.fill('jacket', magic)
        for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
            s.fill(arm, magic, rows=range(0, 10))
            s.fill(sleeve, magic, rows=range(6, 10))
        for leg, pants in (('rleg', 'rpants'), ('lleg', 'lpants')):
            s.fill(leg, magic)
            s.fill(pants, magic, rows=range(0, 11))
            s.fill(pants, trim, rows=range(10, 11))
        for y in range(12):
            s.face_px('jacket', 'front', 3, y, trim)
            s.face_px('jacket', 'front', 4, y, trim)
        s.fill('hat', magic, face_names=('top', 'right', 'left', 'back'))
        s.fill('hat', magic, rows=range(0, 1), face_names=('front',))
    else:  # торговец
        s.fill('body', 0xE8E0D0)
        s.fill('jacket', cloth2, face_names=('right', 'left', 'back', 'top'))
        for y in range(12):
            for x in (0, 1, 6, 7):
                s.face_px('jacket', 'front', x, y, cloth2)
        for y in range(4, 12):
            for x in range(2, 6):
                s.face_px('body', 'front', x, y, mul(leather, 1.2))
        s.fill('rarm', 0xE8E0D0, rows=range(0, 6))
        s.fill('larm', 0xE8E0D0, rows=range(0, 6))
        for leg in ('rleg', 'lleg'):
            s.fill(leg, cloth, rows=range(0, 9))
            s.fill(leg, leather, rows=range(9, 12))
        s.row('body', 8, leather)
        s.face_px('body', 'front', 6, 9, trim)
        s.face_px('body', 'front', 6, 10, trim)
    return s.img


ROLES = {
    'elf': [('elf_warden', 'archer'), ('elf_bladedancer', 'warrior'), ('elf_moonsage', 'mage'), ('elf_trader', 'trader'), ('elf_matriarch', 'chief')],
    'dwarf': [('dwarf_warrior', 'warrior'), ('dwarf_crossbowman', 'archer'), ('dwarf_runepriest', 'mage'), ('dwarf_smith', 'trader'), ('dwarf_thane', 'chief')],
    'orc': [('orc_raider', 'warrior'), ('orc_hunter', 'archer'), ('orc_shaman', 'mage'), ('orc_trader', 'trader'), ('orc_warlord', 'chief')],
    'beastkin': [('beast_stalker', 'archer'), ('beast_ravager', 'warrior'), ('beast_spiritcaller', 'mage'), ('beast_trader', 'trader'), ('beast_alpha', 'chief')],
    'demonkin': [('demon_cultist', 'mage'), ('demon_guard', 'warrior'), ('demon_hellbow', 'archer'), ('demon_trader', 'trader'), ('demon_archfiend', 'chief')],
}


def main():
    os.makedirs(OUT, exist_ok=True)
    out = {}
    for race, items in ROLES.items():
        for i, (key, role) in enumerate(items):
            img = paint(race, role, key)
            write_png(os.path.join(OUT, key + '.png'), img, 64, 64)
            out[key] = img
            models = os.path.join(OUT, '..', '..', '..', 'models', 'item')
            with open(os.path.join(models, key + '_spawn_egg.json'), 'w') as f:
                f.write('{"parent": "minecraft:item/template_spawn_egg"}')
    print(f'{len(out)} skins')
    if '--preview' in sys.argv:
        path = sys.argv[sys.argv.index('--preview') + 1]
        cols, cell = 5, 64 * 3 + 6
        names = list(out)
        rows_n = (len(names) + cols - 1) // cols
        sheet = [[(30, 32, 38, 255)] * (cols * cell) for _ in range(rows_n * cell)]
        for i, n in enumerate(names):
            big = scale(out[n], 3, bg=(60, 64, 72, 255))
            ox, oy = (i % cols) * cell + 3, (i // cols) * cell + 3
            for y, row in enumerate(big):
                for x, px in enumerate(row):
                    sheet[oy + y][ox + x] = px
        write_png(path, sheet, cols * cell, rows_n * cell)


if __name__ == '__main__':
    main()
