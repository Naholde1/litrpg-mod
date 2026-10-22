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
    'human': dict(skin=0xF0C8A0, hair=0x6A4A2A, eyes=0x4A7AC0, cloth=0x3A5AA0, cloth2=0xC8A040, leather=0x6A4422,
                  metal=0xD8DCE0, metal2=0x3A5AA0, magic=0x3A3A8A, trim=0xF2D060, hair_style='short'),
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


# одежда мастеров: туника, штаны, фартук, головной убор, детали
CRAFTS = {
    'blacksmith': dict(tunic=0x5A4A3A, pants=0x3A3028, apron=0x3A2A1A, bare=True, soot=True, gloves=0x2A2A2A),
    'armorer': dict(tunic=0x8A8A94, mail=True, pants=0x3A3A44, apron=0x4A3020, gloves=0x5A5A64),
    'weaponsmith': dict(tunic=0x7A2A2A, pants=0x3A3028, apron=0x4A3020, hat=('band', 0x7A2A2A), soot=True),
    'jeweler': dict(tunic=0x5A2A7A, pants=0x2A2A3A, trim=0xF2D060, hat=('cap', 0x3A1A4A), monocle=True),
    'tailor': dict(tunic=0xC84A6A, pants=0x3A3A6A, sash=0xE8D040, hat=('cap', 0xC84A6A), sleeves=True),
    'herbalist': dict(robe=0x5A8A3A, hat=('hood', 0x4A7A30), flower=0xE04040),
    'alchemist': dict(robe=0x4A2A6A, goggles=0xC8A040, stains=0x60E060, trim=0x9A6AD0),
    'baker': dict(tunic=0xE8E0D0, pants=0x8A7A60, apron=0xF4F0E8, hat=('chef', 0xF8F8F4), flour=True, sleeves=True),
    'butcher': dict(tunic=0xC8C0B0, pants=0x4A3A2A, apron=0xE8E0D8, stains=0xA02020, bare=True),
    'fishmonger': dict(tunic=0x3A6A8A, pants=0x2A3A4A, apron=0xD8C040, hat=('brim', 0xD8C040), sleeves=True),
    'innkeeper': dict(tunic=0xE8E0D0, vest=0x7A3A2A, pants=0x3A2A1A, apron=0xF0ECE0, sleeves=True),
    'builder': dict(tunic=0xC87A2A, pants=0x5A4A3A, belt_tools=True, hat=('cap', 0xE8A030), sleeves=True),
    'leatherworker': dict(tunic=0x8A5A2A, pants=0x4A3020, apron=0x5A3A1A, sleeves=True),
    'fletcher': dict(tunic=0x3A6A3A, pants=0x5A4A2A, hat=('brim', 0x2E5A2E), feather=0xE04020, sleeves=True),
    'scribe': dict(robe=0x2A3A6A, stains=0x101018, hat=('cap', 0x1A2A4A), trim=0xC8B070),
    'farmer': dict(tunic=0xC8A060, overalls=0x3A5A8A, hat=('straw', 0xE0C060), sleeves=False),
    'healer': dict(robe=0xF0F0EC, cross=0xD02020, hat=('hood', 0xF4F4F0)),
    'banker': dict(tunic=0x2A1A4A, pants=0x1A1A2A, trim=0xF2D060, hat=('cap', 0x14141C), sleeves=True, rich=True),
    'stablemaster': dict(tunic=0x6A5A3A, pants=0x3A3A2A, boots=0x4A2A14, hat=('brim', 0x5A3A1A), sleeves=True),
    'miner': dict(tunic=0x5A5A5A, pants=0x3A3A3A, hat=('helmet', 0xD0A040), soot=True, sleeves=True),
    'hosteler': dict(tunic=0xF0E8D8, vest=0x2A5A3A, pants=0x2A2018, apron=0xE8DCC0, trim=0xC8A040, hat=('cap', 0x2A5A3A), sleeves=True),
    'teleporter': dict(robe=0x3A1A6A, trim=0xE0C060, hat=('hood', 0x2A1050), stains=0xB060FF),
}


def artisan_outfit(s, p, craft):
    o = CRAFTS[craft]
    leather = p['leather']
    if 'robe' in o:
        robe = o['robe']
        s.fill('body', robe)
        s.fill('jacket', robe, rows=range(0, 12))
        for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
            s.fill(arm, robe, rows=range(0, 10))
            s.fill(sleeve, mul(robe, 0.9), rows=range(7, 10))
        for leg, pants in (('rleg', 'rpants'), ('lleg', 'lpants')):
            s.fill(leg, robe, rows=range(0, 10))
            s.fill(pants, robe, rows=range(0, 9))
            s.fill(leg, mul(leather, 0.8), rows=range(10, 12))
        s.row('body', 8, mul(robe, 0.6))
    else:
        tunic = o['tunic']
        s.fill('body', tunic)
        arm_rows = range(0, 10) if o.get('sleeves') else range(0, 4) if not o.get('bare') else range(0, 2)
        s.fill('rarm', tunic, rows=arm_rows)
        s.fill('larm', tunic, rows=arm_rows)
        pants = o.get('pants', 0x4A3A2A)
        for leg in ('rleg', 'lleg'):
            s.fill(leg, pants, rows=range(0, 10))
            s.fill(leg, o.get('boots', mul(leather, 0.8)), rows=range(9 if 'boots' in o else 10, 12))
        s.row('body', 8, leather)
        if o.get('mail'):
            for y in range(12):
                for x in range(8):
                    if (x + y) % 2 == 0:
                        s.face_px('body', 'front', x, y, mul(tunic, 0.75))
                        s.face_px('body', 'back', x, y, mul(tunic, 0.75))
    if 'overalls' in o:
        ov = o['overalls']
        for y in range(4, 12):
            for x in range(1, 7):
                s.face_px('body', 'front', x, y, ov)
        for x in (1, 6):
            for y in range(0, 4):
                s.face_px('body', 'front', x, y, ov)
        for leg in ('rleg', 'lleg'):
            s.fill(leg, ov, rows=range(0, 10))
    if 'apron' in o:
        ap = o['apron']
        for y in range(2, 12):
            for x in range(1, 7):
                s.face_px('body', 'front', x, y, ap)
        for x in (1, 6):
            s.face_px('body', 'front', x, 1, mul(ap, 0.8))
        for leg in ('rleg', 'lleg'):
            for y in range(0, 6):
                for x in range(4):
                    if not (leg == 'rleg' and x == 0 or leg == 'lleg' and x == 3):
                        s.face_px(leg, 'front', x, y, ap)
    if 'vest' in o:
        for y in range(0, 9):
            for x in (0, 1, 6, 7):
                s.face_px('jacket', 'front', x, y, o['vest'])
        s.fill('jacket', o['vest'], rows=range(0, 9), face_names=('right', 'left', 'back'))
    if 'sash' in o:
        for i in range(8):
            s.face_px('body', 'front', i, min(11, i + 2), o['sash'])
    if 'trim' in o:
        for y in range(12):
            s.face_px('body', 'front', 3, y, o['trim']) if o.get('rich') or 'robe' in o else None
        s.row('body', 8, o['trim'])
    if 'cross' in o:
        for y in range(3, 8):
            s.face_px('body', 'front', 3, y, o['cross'])
            s.face_px('body', 'front', 4, y, o['cross'])
        for x in range(2, 6):
            s.face_px('body', 'front', x, 4, o['cross'])
            s.face_px('body', 'front', x, 5, o['cross'])
    if o.get('belt_tools'):
        s.row('body', 8, 0x4A3018)
        for x in (1, 3, 6):
            s.face_px('body', 'front', x, 9, 0x9A9AA4)
    if 'gloves' in o:
        for arm in ('rarm', 'larm'):
            s.fill(arm, o['gloves'], rows=range(9, 12))
    if 'stains' in o:
        for _ in range(7):
            s.face_px('body', 'front', s.rnd.randrange(1, 7), s.rnd.randrange(3, 12), o['stains'])
    if o.get('soot'):
        for _ in range(6):
            part = s.rnd.choice(['rarm', 'larm', 'body'])
            s.face_px(part, 'front', s.rnd.randrange(0, 4), s.rnd.randrange(2, 12), 0x2A2420)
        s.face_px('head', 'front', 1, 6, mul(p['skin'], 0.55))
    if o.get('flour'):
        for _ in range(8):
            s.face_px(s.rnd.choice(['rarm', 'larm']), 'front', s.rnd.randrange(0, 4), s.rnd.randrange(4, 12), 0xF4F0E8)
    # головной убор (слой hat)
    hat = o.get('hat')
    if hat:
        kind, col = hat
        if kind in ('cap', 'band'):
            s.fill('hat', col, face_names=('top',)) if kind == 'cap' else None
            s.fill('hat', col, rows=range(0, 2 if kind == 'cap' else 1), face_names=('front', 'right', 'left', 'back'))
            if kind == 'band':
                s.fill('hat', col, rows=range(1, 2), face_names=('front', 'right', 'left', 'back'))
        elif kind == 'chef':
            s.fill('hat', col, face_names=('top',))
            s.fill('hat', col, rows=range(0, 3), face_names=('front', 'right', 'left', 'back'))
        elif kind in ('brim', 'straw'):
            s.fill('hat', col, face_names=('top',))
            s.fill('hat', col, rows=range(0, 2), face_names=('front', 'right', 'left', 'back'))
            s.row('hat', 2, mul(col, 0.75))
            if kind == 'straw':
                for x in range(0, 8, 2):
                    s.face_px('hat', 'front', x, 1, mul(col, 0.8))
        elif kind == 'hood':
            s.fill('hat', col, face_names=('top', 'right', 'left', 'back'))
            s.fill('hat', col, rows=range(0, 2), face_names=('front',))
            for y in range(2, 8):
                s.face_px('hat', 'front', 0, y, col)
                s.face_px('hat', 'front', 7, y, col)
        elif kind == 'helmet':
            s.fill('hat', col, face_names=('top',))
            s.fill('hat', col, rows=range(0, 2), face_names=('front', 'right', 'left', 'back'))
            s.face_px('hat', 'front', 3, 0, 0xFFF0A0)
            s.face_px('hat', 'front', 4, 0, 0xFFE070)
    if 'feather' in o:
        s.face_px('hat', 'right', 1, 0, o['feather'])
        s.face_px('hat', 'right', 2, 0, o['feather'])
    if 'flower' in o:
        s.face_px('hat', 'left', 5, 2, o['flower'])
        s.face_px('hat', 'left', 6, 2, 0xF0E040)
    if o.get('monocle'):
        s.face_px('head', 'front', 5, 4, 0xF2D060)
        s.face_px('head', 'front', 6, 5, 0xF2D060)
    if 'goggles' in o:
        for x in range(1, 7):
            s.face_px('hat', 'front', x, 1, o['goggles'])
        s.face_px('hat', 'front', 2, 1, 0x80E0FF)
        s.face_px('hat', 'front', 5, 1, 0x80E0FF)


def paint(race, role, seed, craft=None):
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
    elif role == 'guard':
        # полные латы, шлем с прорезью, накидка цветов народа
        for part in ('body', 'rarm', 'larm', 'rleg', 'lleg'):
            s.fill(part, metal)
        s.fill('hat', metal, face_names=('top', 'right', 'left', 'back'))
        for y in range(8):
            for x in range(8):
                if not (y in (3, 4) and 1 <= x <= 6):
                    s.face_px('hat', 'front', x, y, mul(metal, 0.92))
        for x in range(8):
            s.face_px('hat', 'front', x, 0, trim)
        s.fill('jacket', cloth2, face_names=('front', 'back'))
        for y in range(12):
            for x in (0, 7):
                s.face_px('jacket', 'front', x, y, mul(cloth2, 0.7))
        for y in range(3, 7):
            for x in range(3, 5):
                s.face_px('jacket', 'front', x, y, trim)
        s.face_px('jacket', 'front', 2, 4, trim)
        s.face_px('jacket', 'front', 5, 4, trim)
        for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
            s.fill(sleeve, metal2, rows=range(0, 3))
            s.fill(arm, mul(metal, 0.8), rows=range(10, 12))
        for leg, pants in (('rleg', 'rpants'), ('lleg', 'lpants')):
            s.fill(pants, mul(metal, 0.85), rows=range(9, 12))
        s.row('body', 9, leather)
    elif role == 'artisan':
        artisan_outfit(s, p, craft)
    elif role == 'citizen':
        import random as _r
        rr = _r.Random(seed)
        tunic = rr.choice([0x8A5A3A, 0x5A7A4A, 0x7A4A6A, 0x4A6A8A, 0xA08A5A, 0x6A6A6A, cloth, cloth2])
        pants = rr.choice([0x4A3A2A, 0x3A3A4A, 0x5A4A3A, leather])
        s.fill('body', tunic)
        s.fill('rarm', tunic, rows=range(0, 5))
        s.fill('larm', tunic, rows=range(0, 5))
        for leg in ('rleg', 'lleg'):
            s.fill(leg, pants, rows=range(0, 10))
            s.fill(leg, mul(leather, 0.8), rows=range(10, 12))
        s.row('body', 8, leather)
        if rr.random() < 0.5:
            s.fill('hat', mul(tunic, 0.8), face_names=('top',))
            s.fill('hat', mul(tunic, 0.8), rows=range(0, 1), face_names=('front', 'right', 'left', 'back'))
        if rr.random() < 0.5:
            for y in range(3, 11):
                for x in range(2, 6):
                    s.face_px('body', 'front', x, y, 0xD8D0C0)
    elif role in ('pirate', 'pirate_captain'):
        # полосатая рубаха, бандана, кушак, широкие штаны и сапоги; у капитана — камзол, треуголка и повязка на глазу
        import random as _r
        rr = _r.Random(seed)
        stripe = rr.choice([0x2A3A6A, 0x8A1A1A, 0x2A2A2A])
        for part in ('body', 'rarm', 'larm'):
            for y in range(12):
                s.row(part, y, 0xE8E4DA if y % 2 == 0 else stripe) if part == 'body' or y < 6 else None
        sash = rr.choice([0xB02020, 0xC08020, 0x6A2A8A])
        s.row('body', 8, sash)
        s.row('body', 9, mul(sash, 0.8))
        for leg in ('rleg', 'lleg'):
            s.fill(leg, rr.choice([0x4A3A2A, 0x3A3A4A, 0x5A4632]), rows=range(0, 8))
            s.fill(leg, 0x2A1E14, rows=range(8, 12))
        if role == 'pirate_captain':
            coat = 0x5A1414
            s.fill('jacket', coat, face_names=('right', 'left', 'back', 'top'))
            for y in range(12):
                for x in (0, 1, 6, 7):
                    s.face_px('jacket', 'front', x, y, coat)
                s.face_px('jacket', 'front', 2, y, 0xD8B040) if y % 3 == 1 else None
            for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
                s.fill(sleeve, coat, rows=range(0, 10))
                s.fill(sleeve, 0xD8B040, rows=range(9, 10))
            s.fill('hat', 0x1A1A1A, face_names=('top', 'right', 'left', 'back'))
            s.fill('hat', 0x1A1A1A, rows=range(0, 2), face_names=('front',))
            s.face_px('hat', 'front', 3, 1, 0xF0F0F0)
            s.face_px('hat', 'front', 4, 1, 0xF0F0F0)
            # повязка через глаз
            for x in range(8):
                s.face_px('head', 'front', x, 3, 0x111111) if x < 4 else None
            s.face_px('head', 'front', 1, 4, 0x111111)
            s.face_px('head', 'front', 2, 4, 0x111111)
            for y in range(5, 8):
                for x in range(1, 7):
                    s.face_px('head', 'front', x, y, 0x2A1A10) if not (y == 6 and 2 < x < 5) else None
        else:
            band = rr.choice([0xB02020, 0x2A4A9A, 0x1A1A1A])
            s.fill('hat', band, face_names=('top', 'right', 'left', 'back'))
            s.fill('hat', band, rows=range(0, 2), face_names=('front',))
            s.face_px('hat', 'back', 3, 2, band)
            s.face_px('hat', 'back', 4, 3, band)
            if rr.random() < 0.5:
                for y in range(5, 8):
                    for x in range(1, 7):
                        s.face_px('head', 'front', x, y, mul(p['hair'], 0.8)) if not (y == 6 and 2 < x < 5) else None
    elif role == 'thug':
        # разбойник: кожаная куртка, шарф-маска до глаз, серый капюшон
        s.fill('body', 0x4A3A2A)
        s.fill('rarm', 0x4A3A2A, rows=range(0, 8))
        s.fill('larm', 0x4A3A2A, rows=range(0, 8))
        for leg in ('rleg', 'lleg'):
            s.fill(leg, 0x3A3A3A, rows=range(0, 9))
            s.fill(leg, 0x2A1E14, rows=range(9, 12))
        s.row('body', 9, 0x2A1E14)
        for y in range(5, 8):
            for x in range(8):
                s.face_px('head', 'front', x, y, 0x3A3A44)
        s.fill('hat', 0x55555F, face_names=('top', 'right', 'left', 'back'))
        s.fill('hat', 0x55555F, rows=range(0, 2), face_names=('front',))
        for i in range(8):
            s.face_px('body', 'front', i, i + 1, 0x2A1E14)
    elif role == 'cultist':
        # сектант Пепла: чёрная ряса с багровой каймой, глубокий капюшон, лицо в тени
        robe, red = 0x1C1A1E, 0x8A1A22
        for part in ('body', 'rarm', 'larm', 'rleg', 'lleg'):
            s.fill(part, robe)
        s.fill('jacket', robe)
        for arm, sleeve in (('rarm', 'rsleeve'), ('larm', 'lsleeve')):
            s.fill(sleeve, robe, rows=range(0, 10))
            s.fill(sleeve, red, rows=range(9, 10))
        for leg, pants in (('rleg', 'rpants'), ('lleg', 'lpants')):
            s.fill(pants, robe, rows=range(0, 12))
            s.fill(pants, red, rows=range(11, 12))
        for y in range(12):
            s.face_px('jacket', 'front', 3, y, red)
            s.face_px('jacket', 'front', 4, y, red)
        s.face_px('jacket', 'front', 3, 3, 0xE07020)
        s.face_px('jacket', 'front', 4, 3, 0xE07020)
        s.fill('hat', robe, face_names=('top', 'right', 'left', 'back'))
        s.fill('hat', robe, rows=range(0, 3), face_names=('front',))
        for y in range(3, 8):
            s.face_px('hat', 'front', 0, y, robe)
            s.face_px('hat', 'front', 7, y, robe)
        for y in range(0, 4):
            for x in range(8):
                s.face_px('head', 'front', x, y, 0x101010)
        s.face_px('head', 'front', 2, 4, 0xFF5020)
        s.face_px('head', 'front', 5, 4, 0xFF5020)
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
    'human': [('human_knight', 'warrior'), ('human_ranger', 'archer'), ('human_mage', 'mage'), ('human_merchant', 'trader'),
              ('human_lord', 'chief'), ('human_guard', 'guard'), ('human_citizen', 'citizen')],
    'elf_extra': [('elf_sentinel', 'guard'), ('elf_citizen', 'citizen')],
    'dwarf_extra': [('dwarf_ironguard', 'guard'), ('dwarf_citizen', 'citizen')],
    'orc_extra': [('orc_bloodguard', 'guard'), ('orc_citizen', 'citizen')],
    'beastkin_extra': [('beast_fangguard', 'guard'), ('beast_citizen', 'citizen')],
    'demonkin_extra': [('demon_hellguard', 'guard'), ('demon_citizen', 'citizen')],
    'outlaw_human': [('outlaw_thug', 'thug'), ('outlaw_cultist', 'cultist'), ('pirate_cutthroat', 'pirate'), ('pirate_gunner', 'pirate'),
                     ('pirate_captain', 'pirate_captain')],
    'demonkin': [('demon_cultist', 'mage'), ('demon_guard', 'warrior'), ('demon_hellbow', 'archer'), ('demon_trader', 'trader'), ('demon_archfiend', 'chief')],
}


def main():
    os.makedirs(OUT, exist_ok=True)
    out = {}
    for race, items in ROLES.items():
        for i, (key, role) in enumerate(items):
            img = paint(race.replace('_extra', '').replace('outlaw_', ''), role, key)
            write_png(os.path.join(OUT, key + '.png'), img, 64, 64)
            out[key] = img
            models = os.path.join(OUT, '..', '..', '..', 'models', 'item')
            with open(os.path.join(models, key + '_spawn_egg.json'), 'w') as f:
                f.write('{"parent": "minecraft:item/template_spawn_egg"}')
    art_dir = os.path.join(OUT, 'artisan')
    os.makedirs(art_dir, exist_ok=True)
    races = {'human': 'human', 'elf': 'elf', 'dwarf': 'dwarf', 'orc': 'orc', 'beast': 'beastkin', 'demon': 'demonkin'}
    for short, race in races.items():
        for craft in CRAFTS:
            img = paint(race, 'artisan', short + craft, craft)
            write_png(os.path.join(art_dir, short + '_' + craft + '.png'), img, 64, 64)
            if short == 'human':
                out['art_' + craft] = img
        models = os.path.join(OUT, '..', '..', '..', 'models', 'item')
        with open(os.path.join(models, short + '_artisan_spawn_egg.json'), 'w') as f:
            f.write('{"parent": "minecraft:item/template_spawn_egg"}')
    # посылка из городских поручений
    tex = blank(16, 16)
    paper, dark, string, seal = (0xB8, 0x8A, 0x52), (0x7A, 0x56, 0x30), (0xE8, 0xDC, 0xB0), (0xC0, 0x20, 0x20)
    for y in range(3, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (3, 13)
            c = dark if edge else paper
            k = 1 + ((x * 7 + y * 13) % 5 - 2) * 0.03
            tex[y][x] = (min(255, int(c[0] * k)), min(255, int(c[1] * k)), min(255, int(c[2] * k)), 255)
    for x in range(2, 14):
        tex[8][x] = string + (255,)
    for y in range(3, 14):
        tex[y][7] = string + (255,)
    for (x, y) in ((6, 7), (8, 7), (6, 9), (8, 9), (7, 8), (6, 8), (8, 8), (7, 7), (7, 9)):
        tex[y][x] = seal + (255,)
    tex[2][6], tex[2][8], tex[1][5], tex[1][9] = (string + (255,),) * 4
    item_tex = os.path.join(OUT, '..', '..', 'item')
    write_png(os.path.join(item_tex, 'parcel.png'), tex, 16, 16)
    with open(os.path.join(OUT, '..', '..', '..', 'models', 'item', 'parcel.json'), 'w') as f:
        f.write('{"parent": "minecraft:item/generated", "textures": {"layer0": "litrpg:item/parcel"}}')
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
