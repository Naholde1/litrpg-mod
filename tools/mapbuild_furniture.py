"""
Обстановка комнат «по-строительски»: мебель из ванильных блоков с хитростями.
- стул: ступенька лицом к столу + открытые люки по бокам (подлокотники);
- стол: забор + нажимная плита (или ступенька-перевёртыш), на столе свеча, торт, цветок;
- кухня: коптильня, тумбы из перевёрнутых ступенек, мойка-котёл с краном из крюка, бочки;
- камин: кирпичная рамка, костёр, полка из плит;
- люстра: цепь с потолка и фонарь; ковёр 3×3 узором;
- полки: люки-перевёртыши на стене с горшками и свечами; шкаф: дверь вплотную к стене;
- тумбочка у кровати: бочка со свечой; сундук в ногах; картины 1×1 на стенах.
"""
from mapbuild_lib import DIRS, OPP

SMALL_PAINTINGS = ['minecraft:kebab', 'minecraft:aztec', 'minecraft:alban', 'minecraft:aztec2', 'minecraft:bomb', 'minecraft:plant',
                   'minecraft:wasteland']
WOOD = {'human': 'spruce', 'elf': 'birch', 'dwarf': 'dark_oak', 'orc': 'spruce', 'beast': 'spruce', 'demon': 'crimson'}
CARPETS = {'human': ['red', 'brown', 'yellow'], 'elf': ['lime', 'green', 'white'], 'dwarf': ['orange', 'brown', 'black'],
           'orc': ['brown', 'red', 'black'], 'beast': ['brown', 'white', 'light_gray'], 'demon': ['black', 'red', 'purple']}


class Room:
    def __init__(self, b, x0, z0, x1, z1, y, door=None, ladder=None, height=4):
        self.b, self.y = b, y
        self.height = height  # от пола до потолка (верхние этажи ниже — над ними перекрытие)
        self.ix0, self.iz0, self.ix1, self.iz1 = x0 + 1, z0 + 1, x1 - 1, z1 - 1
        self.wood = WOOD.get(b.style, 'spruce')
        self.taken = set()
        if ladder:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    self.taken.add((ladder[0] + dx, ladder[1] + dz))
        if door:
            dx_, dz_, side = door
            ox, oz = DIRS[side]
            ix, iz = dx_ - ox, dz_ - oz
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    self.taken.add((ix + dx, iz + dz))
            self.taken.add((ix - ox, iz - oz))

    @property
    def w(self):
        return self.ix1 - self.ix0 + 1

    @property
    def d(self):
        return self.iz1 - self.iz0 + 1

    def free(self, x, z, h=2):
        if (x, z) in self.taken or not (self.ix0 <= x <= self.ix1 and self.iz0 <= z <= self.iz1):
            return False
        return all(self.b.blocks.get((x, self.y + k, z), ('minecraft:air',))[0] == 'minecraft:air' for k in range(h))

    def take(self, *cells):
        for c in cells:
            self.taken.add(c)

    def walls(self):
        """Клетки вдоль стен: (x, z, сторона-внутрь, сторона-стены)."""
        out = []
        for x in range(self.ix0, self.ix1 + 1):
            out.append((x, self.iz0, 'south', 'north'))
            out.append((x, self.iz1, 'north', 'south'))
        for z in range(self.iz0 + 1, self.iz1):
            out.append((self.ix0, z, 'east', 'west'))
            out.append((self.ix1, z, 'west', 'east'))
        return out

    def runs(self, n):
        """Отрезки из n свободных клеток подряд вдоль одной стены."""
        res = []
        sides = {'north': [(x, self.iz0) for x in range(self.ix0, self.ix1 + 1)],
                 'south': [(x, self.iz1) for x in range(self.ix0, self.ix1 + 1)],
                 'west': [(self.ix0, z) for z in range(self.iz0, self.iz1 + 1)],
                 'east': [(self.ix1, z) for z in range(self.iz0, self.iz1 + 1)]}
        for wall, cells in sides.items():
            for i in range(len(cells) - n + 1):
                seg = cells[i:i + n]
                if all(self.free(*c, h=3) for c in seg):
                    res.append((wall, seg))
        self.b.rnd.shuffle(res)
        return res

    # ---------------- предметы ----------------
    def chandelier(self):
        b, y = self.b, self.y
        cx, cz = (self.ix0 + self.ix1) // 2, (self.iz0 + self.iz1) // 2
        if self.height >= 4:
            b.set(cx, y + 3, cz, 'minecraft:chain', axis='y', waterlogged='false')
        b.set(cx, y + 2, cz, b.st['light'] if 'lantern' in b.st['light'] else 'minecraft:lantern', hanging='true', waterlogged='false')

    def rug(self, cx, cz):
        b = self.b
        cols = CARPETS.get(b.style, ['red', 'brown', 'yellow'])
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if self.free(cx + dx, cz + dz, 1):
                    c = cols[0] if (dx == 0 and dz == 0) else cols[1] if (dx == 0 or dz == 0) else cols[2]
                    b.set(cx + dx, self.y, cz + dz, f'minecraft:{c}_carpet')

    def chair(self, x, z, facing):
        """Стул: ступенька спинкой от стола + подлокотники-люки."""
        b, y = self.b, self.y
        b.set(x, y, z, f'minecraft:{self.wood}_stairs', facing=OPP[facing], half='bottom', shape='straight', waterlogged='false')
        self.take((x, z))

    def armrests(self, x, z, facing):
        b, y = self.b, self.y
        dx, dz = DIRS[facing]
        for s in (-1, 1):
            ax, az = x + dz * s, z + dx * s  # по бокам
            if self.free(ax, az, 1):
                side = 'east' if (ax - x) > 0 else 'west' if (ax - x) < 0 else 'south' if (az - z) > 0 else 'north'
                b.set(ax, y, az, f'minecraft:{self.wood}_trapdoor', facing=side, half='bottom', open='true', powered='false',
                      waterlogged='false')
                self.take((ax, az))

    def table_set(self):
        """Стол посреди комнаты со стульями и свечой; ковёр под ним."""
        b, y = self.b, self.y
        rnd = b.rnd
        cx, cz = (self.ix0 + self.ix1) // 2, (self.iz0 + self.iz1) // 2
        cells = [(cx, cz), (cx + 1, cz)] if self.w >= self.d else [(cx, cz), (cx, cz + 1)]
        if not all(self.free(*c) for c in cells):
            return False
        self.rug(cx, cz)
        for (tx, tz) in cells:
            b.set(tx, y, tz, f'minecraft:{self.wood}_fence', waterlogged='false')
            b.set(tx, y + 1, tz, f'minecraft:{self.wood}_pressure_plate', powered='false')
            self.take((tx, tz))
        top = rnd.choice(['minecraft:candle', 'minecraft:cake', 'potted', 'minecraft:candle'])
        tx, tz = cells[0]
        if top == 'minecraft:candle':
            b.set(tx, y + 1, tz, 'minecraft:white_candle', candles=str(rnd.randint(1, 3)), lit='true', waterlogged='false')
        elif top == 'minecraft:cake':
            b.set(tx, y + 1, tz, 'minecraft:cake', bites=str(rnd.randint(0, 3)))
        else:
            b.set(tx, y + 1, tz, 'minecraft:potted_' + rnd.choice(['red_tulip', 'azure_bluet', 'cornflower', 'fern']))
        # стулья по длинным сторонам
        horiz = cells[1][0] != cells[0][0]
        for (tx, tz) in cells:
            for s in (-1, 1):
                px, pz = (tx, tz + s) if horiz else (tx + s, tz)
                if self.free(px, pz):
                    facing = ('north' if s > 0 else 'south') if horiz else ('west' if s > 0 else 'east')
                    self.chair(px, pz, facing)
        # торцевые стулья с подлокотниками
        ends = [(cells[0][0] - 1, cells[0][1], 'east'), (cells[-1][0] + 1, cells[-1][1], 'west')] if horiz else \
            [(cells[0][0], cells[0][1] - 1, 'south'), (cells[-1][0], cells[-1][1] + 1, 'north')]
        for (px, pz, f) in ends:
            if self.free(px, pz) and rnd.random() < 0.7:
                self.chair(px, pz, f)
                self.armrests(px, pz, f)
        return True

    def kitchen(self):
        b, y = self.b, self.y
        runs = self.runs(4)
        if not runs:
            return False
        wall, seg = runs[0]
        inward = OPP[wall]
        items = ['smoker', 'counter', 'sink', 'counter'] if b.rnd.random() < 0.5 else ['counter', 'sink', 'counter', 'smoker']
        for (x, z), it in zip(seg, items):
            if it == 'smoker':
                b.set(x, y, z, 'minecraft:smoker', facing=inward, lit='true')
                b.set(x, y + 1, z, 'minecraft:stone_brick_wall', up='true', north='none', south='none', east='none', west='none',
                      waterlogged='false')
            elif it == 'counter':
                b.set(x, y, z, f'minecraft:{self.wood}_stairs', facing=wall, half='top', shape='straight', waterlogged='false')
                if b.rnd.random() < 0.5:
                    b.set(x, y + 1, z, b.rnd.choice(['minecraft:white_candle', 'minecraft:brown_candle']), candles='1', lit='false',
                          waterlogged='false')
                # навесной шкафчик
                b.set(x, y + 2, z, f'minecraft:{self.wood}_trapdoor', facing=inward, half='top', open='true', powered='false',
                      waterlogged='false')
            elif it == 'sink':
                b.set(x, y, z, 'minecraft:water_cauldron', level='3')
                wx, wz = x + DIRS[wall][0], z + DIRS[wall][1]
                if b.blocks.get((wx, y + 1, wz), ('',))[0] not in ('', 'minecraft:air'):
                    b.set(x, y + 1, z, 'minecraft:tripwire_hook', facing=inward, attached='false', powered='false')
            self.take((x, z))
        return True

    def fireplace(self):
        b, y = self.b, self.y
        runs = self.runs(3)
        if not runs:
            return False
        wall, seg = runs[0]
        inward = OPP[wall]
        brick = 'minecraft:bricks' if b.style not in ('dwarf', 'demon') else 'minecraft:polished_blackstone_bricks'
        stair = 'minecraft:brick_stairs' if b.style not in ('dwarf', 'demon') else 'minecraft:polished_blackstone_brick_stairs'
        (ax, az), (cx, cz), (bx, bz) = seg
        for (x, z) in (seg[0], seg[2]):
            b.set(x, y, z, brick)
            b.set(x, y + 1, z, brick)
        b.set(cx, y, cz, 'minecraft:campfire', lit='true', signal_fire='false', facing=inward, waterlogged='false')
        b.set(cx, y + 1, cz, 'minecraft:air')
        for (x, z) in seg:
            b.set(x, y + 2, z, stair, facing=wall, half='top', shape='straight', waterlogged='false')
        # на каминной полке
        b.set(ax, y + 3, az, 'minecraft:potted_' + b.rnd.choice(['fern', 'red_tulip', 'oxeye_daisy']))
        b.set(bx, y + 3, bz, 'minecraft:candle', candles='2', lit='true', waterlogged='false')
        for c in seg:
            self.take(c)
        fx, fz = cx + DIRS[inward][0], cz + DIRS[inward][1]
        self.take((fx, fz))
        return True

    def shelf(self):
        """Полка на стене: люк-перевёртыш с горшком или свечой, внизу бочка или книжный шкаф."""
        b, y = self.b, self.y
        runs = self.runs(2)
        if not runs:
            return False
        wall, seg = runs[0]
        inward = OPP[wall]
        for (x, z) in seg:
            b.set(x, y, z, b.rnd.choice(['minecraft:bookshelf', 'minecraft:barrel', 'minecraft:bookshelf']),
                  **({} if b.rnd.random() < 0.6 else {}))
            if b.blocks[(x, y, z)][0] == 'minecraft:barrel':
                b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
            b.set(x, y + 1, z, 'minecraft:' + b.rnd.choice(['potted_fern', 'potted_red_tulip', 'flower_pot', 'brown_candle']),
                  **({'candles': '1', 'lit': 'true', 'waterlogged': 'false'} if False else {}))
            if b.blocks[(x, y + 1, z)][0] == 'minecraft:brown_candle':
                b.set(x, y + 1, z, 'minecraft:brown_candle', candles='1', lit='true', waterlogged='false')
            b.set(x, y + 2, z, f'minecraft:{self.wood}_trapdoor', facing=inward, half='top', open='false', powered='false',
                  waterlogged='false')
            self.take((x, z))
        return True

    def wardrobe(self):
        """Шкаф: дверь (2 блока) вплотную к стене — выглядит как резной шкаф."""
        b, y = self.b, self.y
        runs = self.runs(2)
        if not runs:
            return False
        wall, seg = runs[0]
        inward = OPP[wall]
        for i, (x, z) in enumerate(seg):
            door = f'minecraft:{self.wood}_door'
            b.set(x, y, z, door, facing=inward, half='lower', hinge='left' if i == 0 else 'right', open='false', powered='false')
            b.set(x, y + 1, z, door, facing=inward, half='upper', hinge='left' if i == 0 else 'right', open='false', powered='false')
            b.set(x, y + 2, z, f'minecraft:{self.wood}_slab', type='bottom', waterlogged='false')
            self.take((x, z))
        return True

    def bed_corner(self, color):
        """Кровать головой к стене, тумбочка со свечой и сундук в ногах."""
        b, y = self.b, self.y
        for (x, z, inward, wall) in self.b.rnd.sample(self.walls(), len(self.walls())):
            dx, dz = DIRS[inward]
            foot = (x + dx, z + dz)
            if not (self.free(x, z) and self.free(*foot)):
                continue
            b.bed(foot[0], y, foot[1], wall, color)  # голова у стены
            self.take((x, z), foot)
            # тумбочка рядом с изголовьем
            sx, sz = x + dz, z + dx
            if self.free(sx, sz):
                b.set(sx, y, sz, 'minecraft:barrel', facing='up', open='false')
                b.set(sx, y + 1, sz, 'minecraft:' + b.rnd.choice(['white_candle', 'yellow_candle']), candles='1', lit='true',
                      waterlogged='false')
                self.take((sx, sz))
            cx, cz = foot[0] + dx, foot[1] + dz
            if self.free(cx, cz) and b.rnd.random() < 0.6:
                b.set(cx, y, cz, 'minecraft:chest', facing=inward, type='single', waterlogged='false')
                self.take((cx, cz))
            return True
        return False

    def painting(self):
        b, y = self.b, self.y
        for (x, z, inward, wall) in b.rnd.sample(self.walls(), len(self.walls())):
            if not self.free(x, z, 3) or (x, z) in self.taken:
                continue
            wx, wz = x + DIRS[wall][0], z + DIRS[wall][1]
            behind = b.blocks.get((wx, y + 2, wz), ('',))[0]
            if behind in ('', 'minecraft:air') or 'pane' in behind or 'glass' in behind or 'door' in behind:
                continue
            b.entities.append((x, y + 2, z, 'minecraft:painting', {'variant': b.rnd.choice(SMALL_PAINTINGS), 'facing': inward, 'small': 1}))
            return True
        return False

    def bookshelves(self, n=6):
        b, y = self.b, self.y
        k = 0
        for (x, z, inward, wall) in self.walls():
            if k >= n:
                break
            if self.free(x, z, 3):
                b.set(x, y, z, 'minecraft:bookshelf')
                b.set(x, y + 1, z, 'minecraft:bookshelf')
                b.set(x, y + 2, z, 'minecraft:chiseled_bookshelf' if False else f'minecraft:{self.wood}_slab', type='top', waterlogged='false')
                self.take((x, z))
                k += 1

    def corner_plant(self):
        b, y = self.b, self.y
        for (x, y_, z) in (((self.ix0, 0, self.iz0)), (self.ix1, 0, self.iz1), (self.ix1, 0, self.iz0), (self.ix0, 0, self.iz1)):
            if self.free(x, z):
                b.set(x, y, z, 'minecraft:' + b.rnd.choice(['potted_azalea_bush', 'potted_flowering_azalea_bush', 'potted_fern',
                                                             'potted_bamboo', 'potted_cactus']))
                self.take((x, z))
                return True
        return False


def furnish(b, x0, z0, x1, z1, floors, kind, door_side, base_kind=None):
    """Обставить все этажи дома (kind: home/shop/smith/tavern/library/guard/workshop)."""
    G = b.G
    rnd = b.rnd
    door = getattr(b, 'last_door', None)
    for f in range(floors):
        # на верхних этажах блок y = G + 4f — это перекрытие, ходят на уровень выше
        y = G + f * 4 + (1 if f else 0)
        ladder = (x0 + 1, z0 + 1) if floors > 1 else None
        r = Room(b, x0, z0, x1, z1, y, door=door if f == 0 else None, ladder=ladder, height=4 if f == 0 else 3)
        if r.w < 3 or r.d < 3:
            continue
        if r.w * r.d >= 110 and kind not in ('workshop', 'smith'):
            # большие здания: зал внизу, наверху — комнаты за перегородками
            if f == 0:
                big_hall(b, r, kind)
            else:
                split_floor(b, r, x0, z0, x1, z1, y, 3, ladder)
            continue
        top = f == floors - 1
        k = kind
        if kind == 'workshop':
            if f == 0:
                r.chandelier()
                r.painting()
                continue
            k = 'home'
        r.chandelier()
        if k == 'home':
            if f == 0 and (floors > 1 or rnd.random() < 0.6):
                r.kitchen()
                r.table_set()
                r.fireplace() if rnd.random() < 0.6 else r.shelf()
            if f > 0 and not top and rnd.random() < 0.5:
                r.table_set()  # кабинет / детская на среднем этаже
            if f > 0 or floors == 1:
                r.bed_corner(rnd.choice(['red', 'blue', 'white', 'green', 'brown', 'light_blue', 'yellow']))
                if r.w * r.d > 30:
                    r.bed_corner(rnd.choice(['red', 'blue', 'white']))
                r.wardrobe()
            r.shelf()
            r.corner_plant()
            r.painting()
            if not (f == 0):
                r.rug((r.ix0 + r.ix1) // 2, (r.iz0 + r.iz1) // 2)
        elif k == 'shop':
            r.shelf()
            r.shelf()
            r.table_set()
            r.corner_plant()
            r.painting()
            if f > 0:
                r.bed_corner('green')  # хозяин лавки живёт наверху
                r.wardrobe()
                r.rug((r.ix0 + r.ix1) // 2, (r.iz0 + r.iz1) // 2)
        elif k == 'library':
            r.bookshelves(10)
            r.table_set()
            cx, cz = (r.ix0 + r.ix1) // 2, (r.iz0 + r.iz1) // 2 + 2
            if r.free(cx, cz):
                b.set(cx, y, cz, 'minecraft:lectern', facing='north', has_book='false', powered='false')
                r.take((cx, cz))
            r.corner_plant()
        elif k == 'tavern':
            if f == 0:
                r.fireplace()
                r.kitchen()
                r.table_set()
                # ещё столики у стен
                for (x, z, inward, wall) in r.walls():
                    if rnd.random() < 0.25 and r.free(x, z):
                        dx, dz = DIRS[inward]
                        if r.free(x + dx, z + dz):
                            b.set(x, y, z, f'minecraft:{r.wood}_fence', waterlogged='false')
                            b.set(x, y + 1, z, f'minecraft:{r.wood}_pressure_plate', powered='false')
                            r.take((x, z))
                            r.chair(x + dx, z + dz, wall)
                r.shelf()
            else:
                r.bed_corner('red')
                r.bed_corner('blue')
                r.bed_corner('white')
                r.painting()
        elif k == 'smith':
            for it in ('anvil', 'blast_furnace', 'grindstone'):
                for (x, z, inward, wall) in r.walls():
                    if r.free(x, z):
                        if it == 'anvil':
                            b.set(x, y, z, 'minecraft:anvil', facing='north')
                        elif it == 'blast_furnace':
                            b.set(x, y, z, 'minecraft:blast_furnace', facing=inward, lit='true')
                        else:
                            b.set(x, y, z, 'minecraft:grindstone', face='floor', facing=inward)
                        r.take((x, z))
                        break
            r.shelf()
        elif k == 'guard':
            for (x, z, inward, wall) in r.walls()[:6]:
                if r.free(x, z) and rnd.random() < 0.6:
                    b.entities.append((x, y, z, 'minecraft:armor_stand', {'ShowArms': __import__('mapbuild_lib').TByte(1)}))
                    r.take((x, z))
            r.table_set()
            if top:
                r.bed_corner('gray')
                r.bed_corner('gray')
            r.painting()


# ---------------- большие здания ----------------

def big_hall(b, r, kind):
    """Большой зал первого этажа (ратуша, гильдия, донжон): длинные столы с проходами крестом,
    люстры сеткой, камин, знамёна или стеллажи по стенам, стойки с доспехами, цветы по углам."""
    rnd = b.rnd
    y = r.y
    wood = r.wood
    cx, cz = (r.ix0 + r.ix1) // 2, (r.iz0 + r.iz1) // 2
    along_x = r.w >= r.d
    r.fireplace()
    r.fireplace() if r.w * r.d > 250 else None
    # вдоль стен: книги у библиотек/гильдий, иначе бочки-сундуки и знамёна
    if kind == 'library':
        r.bookshelves(max(8, (r.w + r.d)))
    else:
        r.shelf()
        r.shelf()
    colors = ['blue', 'red', 'yellow', 'white', 'cyan']
    k = 0
    for (x, z, inward, wall) in r.walls():
        if (x + z) % 4 == 0 and r.free(x, z, 3):
            wx, wz = x + DIRS[wall][0], z + DIRS[wall][1]
            behind = b.blocks.get((wx, y + 2, wz), ('',))[0]
            if behind and 'pane' not in behind and 'glass' not in behind and 'door' not in behind and behind != 'minecraft:air':
                b.set(x, y + 2, z, f'minecraft:{colors[k % len(colors)]}_wall_banner', facing=inward)
                k += 1
    # длинные столы по обе стороны от центрального прохода, поперечный проход посредине
    rows = (cz - 2, cz + 2) if along_x else (cx - 2, cx + 2)
    span = range(r.ix0 + 2, r.ix1 - 1) if along_x else range(r.iz0 + 2, r.iz1 - 1)
    mid = cx if along_x else cz
    for row in rows:
        for t in span:
            if abs(t - mid) <= 1:
                continue
            x, z = (t, row) if along_x else (row, t)
            if not r.free(x, z):
                continue
            b.set(x, y, z, f'minecraft:{wood}_fence', waterlogged='false')
            top = rnd.random()
            if top < 0.2:
                b.set(x, y + 1, z, 'minecraft:white_candle', candles=str(rnd.randint(1, 3)), lit='true', waterlogged='false')
            elif top < 0.27:
                b.set(x, y + 1, z, 'minecraft:cake', bites=str(rnd.randint(0, 4)))
            else:
                b.set(x, y + 1, z, f'minecraft:{wood}_pressure_plate', powered='false')
            r.take((x, z))
            for s in (-1, 1):
                px, pz = (x, z + s) if along_x else (x + s, z)
                if r.free(px, pz):
                    facing = ('north' if s > 0 else 'south') if along_x else ('west' if s > 0 else 'east')
                    r.chair(px, pz, facing)
    # ковровая дорожка поперёк — от входа через зал
    for t in (range(r.iz0, r.iz1 + 1) if along_x else range(r.ix0, r.ix1 + 1)):
        x, z = (cx, t) if along_x else (t, cz)
        if r.free(x, z, 1):
            b.set(x, y, z, 'minecraft:red_carpet')
    # люстры сеткой
    for gx in range(r.ix0 + 2, r.ix1 - 1, 5):
        for gz in range(r.iz0 + 2, r.iz1 - 1, 5):
            if r.height >= 4:
                b.set(gx, y + 3, gz, 'minecraft:chain', axis='y', waterlogged='false')
            b.set(gx, y + 2, gz, 'minecraft:lantern', hanging='true', waterlogged='false')
    # доспехи и цветы по углам
    for (x, z) in ((r.ix0, r.iz0 + 2), (r.ix1, r.iz0 + 2), (r.ix0, r.iz1 - 2), (r.ix1, r.iz1 - 2)):
        if r.free(x, z):
            b.entities.append((x, y, z, 'minecraft:armor_stand', {'ShowArms': __import__('mapbuild_lib').TByte(1)}))
            r.take((x, z))
    for _ in range(4):
        r.corner_plant()
    r.painting()
    r.painting()


def split_floor(b, r, x0, z0, x1, z1, y, height, ladder):
    """Большой верхний этаж: перегородки с дверями — несколько комнат, каждая обставлена."""
    along_x = (x1 - x0) >= (z1 - z0)
    lo, hi = (x0, x1) if along_x else (z0, z1)
    cuts = list(range(lo + 7, hi - 4, 7))
    plank = f'minecraft:{WOOD.get(b.style, "spruce")}_planks'
    mid = (z0 + z1) // 2 if along_x else (x0 + x1) // 2
    for c in cuts:
        for t in range((z0 + 1) if along_x else (x0 + 1), (z1 if along_x else x1)):
            x, z = (c, t) if along_x else (t, c)
            for yy in range(y, y + height):
                b.set(x, yy, z, plank)
        dx, dz = (c, mid) if along_x else (mid, c)
        b.door(dx, y, dz, 'east' if along_x else 'south', f'minecraft:{WOOD.get(b.style, "spruce")}_door')
    bounds = [lo] + cuts + [hi]
    for i in range(len(bounds) - 1):
        a, c = bounds[i], bounds[i + 1]
        sub = Room(b, a, z0, c, z1, y, ladder=ladder, height=height) if along_x else Room(b, x0, a, x1, c, y, ladder=ladder, height=height)
        # проходы у дверей перегородок
        for cc in (a, c):
            for s in (-1, 0, 1):
                for e in (-1, 1):
                    sub.taken.add((cc + e, mid + s) if along_x else (mid + s, cc + e))
        sub.chandelier()
        if i % 2 == 0:
            sub.bed_corner(b.rnd.choice(['red', 'blue', 'white', 'green', 'light_blue']))
            sub.wardrobe()
            sub.shelf()
            sub.rug((sub.ix0 + sub.ix1) // 2, (sub.iz0 + sub.iz1) // 2)
        else:
            sub.bookshelves(6)
            sub.table_set()
            sub.shelf()
        sub.corner_plant()
        sub.painting()
