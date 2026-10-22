"""
Постоялые дворы и круги телепорта (2.13.0).

Постоялый двор — у проспекта, дверью на улицу: внизу трапезная (стойка, бочки, очаг, столы,
хозяин за стойкой), лестница в угловом холле, наверху коридор и комнаты по обе стороны.
В каждой комнате кровать, сундук Края, сундук, верстак, печь, коврик, фонарь, окно и табличка
с номером. Комнаты записываются точками (anchors): inn_room_N_lo / _hi (внутренний объём) и
inn_room_N_door — по ним Java (city/Inns) сдаёт комнаты, запирает двери и разрешает строить внутри.
Облик — по народу: материалы, крыша, вывеска и убранство снаружи.

Круг телепорта — площадка у главной площади: кольца узора, четыре столба с огнями,
камень-якорь в центре (точка прибытия, anchor «portal») и маг порталов (ремесло teleporter).
"""
import math

from mapbuild_lib import DIRS, OPP, TByte, TList
import mapbuild_secrets as SEC

NAMES = {'arcadia': 'Золотой грифон', 'haven': 'Пьяный кракен', 'sylvendar': 'Лунный приют', 'khazad': 'Наковальня и кружка',
         'ghardum': 'Кабаний клык', 'wolfden': 'Тёплая шкура', 'ashgrad': 'Тлеющий уголь', 'dawnfort': 'Рассветный рог',
         'northwatch': 'Ледяной очаг', 'blackbastion': 'Чёрный ворон'}

# этажей с комнатами, комнат на сторону коридора
LAYOUT = {'arcadia': (2, 3), 'haven': (1, 3), 'sylvendar': (1, 3), 'khazad': (2, 2), 'ghardum': (1, 3), 'wolfden': (1, 2),
          'ashgrad': (1, 3), 'dawnfort': (1, 2), 'northwatch': (1, 2), 'blackbastion': (1, 2)}

RACE_NPC = {'human': 'litrpg:human_artisan', 'elf': 'litrpg:elf_artisan', 'dwarf': 'litrpg:dwarf_artisan',
            'orc': 'litrpg:orc_artisan', 'beast': 'litrpg:beast_artisan', 'demon': 'litrpg:demon_artisan'}

# облик постоялого двора по народу
LOOK = {
    'human': dict(low=['minecraft:stone_bricks', 'minecraft:stone_bricks', 'minecraft:mossy_stone_bricks', 'minecraft:cracked_stone_bricks'],
                  up=['minecraft:calcite'], beam='minecraft:dark_oak_log', floor='minecraft:spruce_planks',
                  inner='minecraft:spruce_planks', counter='minecraft:stripped_dark_oak_log', wood='spruce', roof=0, carpet='red', bed='red',
                  rug='red', glass='minecraft:glass_pane', hearth='minecraft:bricks', chimney='minecraft:cobblestone'),
    'elf': dict(low=['minecraft:calcite', 'minecraft:mossy_stone_bricks'], up=['minecraft:birch_planks', 'minecraft:stripped_birch_wood'],
                beam='minecraft:birch_log', floor='minecraft:birch_planks', inner='minecraft:stripped_birch_wood', counter='minecraft:stripped_birch_log',
                wood='birch', roof=1, carpet='green', bed='lime', rug='lime', glass='minecraft:light_blue_stained_glass_pane',
                hearth='minecraft:mossy_stone_bricks', chimney='minecraft:mossy_cobblestone'),
    'dwarf': dict(low=['minecraft:polished_deepslate', 'minecraft:deepslate_bricks', 'minecraft:deepslate_tiles'],
                  up=['minecraft:deepslate_bricks', 'minecraft:polished_deepslate'], beam='minecraft:polished_basalt', floor='minecraft:spruce_planks',
                  inner='minecraft:dark_oak_planks', counter='minecraft:polished_blackstone', wood='dark_oak', roof=0, carpet='orange', bed='orange',
                  rug='brown', glass='minecraft:orange_stained_glass_pane', hearth='minecraft:polished_blackstone_bricks',
                  chimney='minecraft:polished_blackstone_bricks'),
    'orc': dict(low=['minecraft:mud_bricks', 'minecraft:packed_mud', 'minecraft:cobblestone'], up=['minecraft:spruce_log', 'minecraft:stripped_spruce_log'],
                beam='minecraft:stripped_spruce_log', floor='minecraft:spruce_planks', inner='minecraft:spruce_planks', counter='minecraft:spruce_log',
                wood='spruce', roof=1, carpet='red', bed='brown', rug='red', glass='minecraft:spruce_fence', hearth='minecraft:mud_bricks',
                chimney='minecraft:cobblestone'),
    'beast': dict(low=['minecraft:cobblestone', 'minecraft:mossy_cobblestone'], up=['minecraft:spruce_log'], beam='minecraft:stripped_spruce_log',
                  floor='minecraft:spruce_planks', inner='minecraft:spruce_planks', counter='minecraft:stripped_spruce_log', wood='spruce', roof=0,
                  carpet='brown', bed='brown', rug='white', glass='minecraft:spruce_fence', hearth='minecraft:cobblestone',
                  chimney='minecraft:mossy_cobblestone'),
    'demon': dict(low=['minecraft:polished_blackstone_bricks', 'minecraft:cracked_polished_blackstone_bricks', 'minecraft:blackstone'],
                  up=['minecraft:red_nether_bricks', 'minecraft:nether_bricks'], beam='minecraft:crimson_stem', floor='minecraft:crimson_planks',
                  inner='minecraft:crimson_planks', counter='minecraft:polished_blackstone', wood='crimson', roof=0, carpet='black', bed='red',
                  rug='red', glass='minecraft:red_stained_glass_pane', hearth='minecraft:nether_bricks', chimney='minecraft:blackstone'),
}

# у отдельных городов — свой облик поверх облика народа
CITY_LOOK = {
    # портовый трактир: смолёные доски, морская крыша, голубое бельё, бочки и якорная цепь у входа
    'haven': dict(up=['minecraft:stripped_spruce_wood', 'minecraft:spruce_planks'], beam='minecraft:dark_oak_log', roof=3, carpet='cyan',
                  bed='light_blue', rug='blue', counter='minecraft:stripped_spruce_log', extra='harbor'),
    # крепостные дворы: камень до крыши, тёмный дуб, солдатский порядок
    'dawnfort': dict(up=['minecraft:stone_bricks', 'minecraft:cobblestone'], beam='minecraft:spruce_log', roof=2, carpet='orange', bed='orange',
                     rug='brown', extra='fort'),
    'northwatch': dict(up=['minecraft:stone_bricks', 'minecraft:cobblestone'], beam='minecraft:spruce_log', roof=2, carpet='light_blue',
                       bed='light_blue', rug='white', extra='fort'),
    'blackbastion': dict(low=['minecraft:deepslate_bricks', 'minecraft:cobbled_deepslate'], up=['minecraft:deepslate_bricks', 'minecraft:polished_deepslate'],
                         beam='minecraft:dark_oak_log', roof=0, carpet='black', bed='black', rug='gray', extra='fort'),
    # столица: золото на вывеске и балконы с цветами — в _decor (human)
    'arcadia': dict(carpet='red', bed='red', rug='red', extra='capital'),
}

ROT = {'south': 'north', 'north': 'south', 'east': 'west', 'west': 'east'}


class Frame:
    """Локальные координаты здания: u — вдоль фасада, v — вглубь от фасада (v=0 — стена с дверью)."""

    def __init__(self, b, x0, z0, W, D, front):
        self.b, self.x0, self.z0, self.W, self.D, self.front = b, x0, z0, W, D, front
        if front in ('south', 'north'):
            self.x1, self.z1 = x0 + W - 1, z0 + D - 1
        else:
            self.x1, self.z1 = x0 + D - 1, z0 + W - 1
        f = front
        # стороны в мировых названиях: F — наружу через фасад, B — вглубь, R — +u, L — −u
        self.F = f
        self.B = OPP[f]
        self.R = {'south': 'east', 'north': 'west', 'east': 'south', 'west': 'north'}[f]
        self.L = OPP[self.R]

    def xz(self, u, v):
        f = self.front
        if f == 'south':
            return self.x0 + u, self.z1 - v
        if f == 'north':
            return self.x1 - u, self.z0 + v
        if f == 'east':
            return self.x1 - v, self.z0 + u
        return self.x0 + v, self.z1 - u

    def set(self, u, y, v, block, **props):
        x, z = self.xz(u, v)
        self.b.set(x, y, z, block, **props)

    def get(self, u, y, v):
        x, z = self.xz(u, v)
        return self.b.blocks.get((x, y, z))

    def at(self, u, y, v):
        x, z = self.xz(u, v)
        return x, y, z


def _axis_u(fr):
    return 'x' if fr.front in ('south', 'north') else 'z'


def _axis_v(fr):
    return 'z' if fr.front in ('south', 'north') else 'x'


def sign_text(b, x, y, z, facing, wood, lines, glow=False):
    lines = (list(lines) + ['', '', '', ''])[:4]
    b.set(x, y, z, f'minecraft:{wood}_wall_sign', facing=facing, waterlogged='false')
    msg = TList(8, ['{"text":"%s"}' % ln for ln in lines])
    empty = TList(8, ['{"text":""}'] * 4)
    b.nbt[(x, y, z)] = {'id': 'minecraft:sign', 'is_waxed': TByte(1),
                        'front_text': {'messages': msg, 'color': 'black', 'has_glowing_text': TByte(1 if glow else 0)},
                        'back_text': {'messages': empty, 'color': 'black', 'has_glowing_text': TByte(0)}}


def inn_size(cid):
    nf, k = LAYOUT.get(cid, (1, 2))
    return 7 * k + 5, 16, nf, k


def inn_footprint(cid, front):
    W, D, _, _ = inn_size(cid)
    # + место под трубу сбоку и под крыльцо спереди
    return (W + 2, D + 3) if front in ('south', 'north') else (D + 3, W + 2)


def inn(b, x0, z0, front, cid):
    """Постоялый двор; x0, z0 — угол самого здания (без крыльца). Записывает мастерскую хозяина и точки комнат."""
    W, D, nf, k = inn_size(cid)
    style = b.style
    L = dict(LOOK[style], **CITY_LOOK.get(cid, {}))
    wood = L['wood']
    G = b.G
    fr = Frame(b, x0, z0, W, D, front)
    top_layer = G + 5 + 4 * nf          # потолок последнего этажа
    hu = W - 4                          # холл с лестницей: u = W-4 .. W-2
    dc = (W - 5) // 2                   # двустворчатая дверь: u = dc, dc+1
    b.reserve(min(fr.x0, fr.x1) - 2, min(fr.z0, fr.z1) - 2, max(fr.x0, fr.x1) + 2, max(fr.z0, fr.z1) + 2)

    def pick(opts, u, y, v):
        x, _, z = fr.at(u, y, v)
        return b.bp(opts, x, y, z)

    def floor_layer(y):
        return y == G + 5 or (y > G + 5 and (y - (G + 5)) % 4 == 0)

    def beam(u, y, v, vertical):
        blk = L['beam']
        if 'log' in blk or 'stem' in blk or 'basalt' in blk:
            fr.set(u, y, v, blk, axis='y' if vertical else (_axis_u(fr) if v in (0, D - 1) else _axis_v(fr)))
        else:
            fr.set(u, y, v, blk)

    # ---- коробка: фундамент, стены, перекрытия ----
    for u in range(W):
        for v in range(D):
            edge = u in (0, W - 1) or v in (0, D - 1)
            fr.set(u, G - 1, v, pick(L['low'], u, G - 1, v) if edge else L['floor'])
            fr.set(u, G - 2, v, 'minecraft:stone')
            for y in range(G, top_layer + 1):
                if edge:
                    corner = u in (0, W - 1) and v in (0, D - 1)
                    post = (u % 4 == 0) if v in (0, D - 1) else (v % 5 == 0)
                    if corner or post:
                        beam(u, y, v, True)
                    elif floor_layer(y):
                        beam(u, y, v, False)
                    elif y < G + 5:
                        fr.set(u, y, v, pick(L['low'], u, y, v))
                    else:
                        bl = pick(L['up'], u, y, v)
                        fr.set(u, y, v, bl, **({'axis': 'y'} if 'log' in bl or 'wood' in bl else {}))
                elif floor_layer(y):
                    fr.set(u, y, v, L['floor'])
                else:
                    fr.set(u, y, v, 'minecraft:air')
    # ---- окна ----
    pane = L['glass']

    def window(u, y, v):
        fr.set(u, y, v, pane, waterlogged='false')
    for u in list(range(2, dc - 1)) + list(range(dc + 3, hu)):
        if u % 4:
            window(u, G + 1, 0)
            window(u, G + 2, 0)
    for v in (2, 3):
        window(0, G + 2, v)
    # ---- дверь, ступень, вывеска ----
    for du in (0, 1):
        x, y, z = fr.at(dc + du, G, 0)
        b.set(x, G, z, f'minecraft:{wood}_door', facing=fr.F, half='lower', hinge='left' if du == 0 else 'right', open='false', powered='false')
        b.set(x, G + 1, z, f'minecraft:{wood}_door', facing=fr.F, half='upper', hinge='left' if du == 0 else 'right', open='false', powered='false')
        beam(dc + du, G + 2, 0, False)
    if not hasattr(b, 'doors'):
        b.doors = []
    dx_, _, dz_ = fr.at(dc, G, 0)
    b.doors.append((dx_, G, dz_, fr.F))
    # вывеска над дверью и фонари по бокам
    sx, sy, sz = fr.at(dc, G + 3, -1)
    sign_text(b, sx, sy, sz, fr.F, wood, ['Постоялый двор', '', '«' + NAMES.get(cid, 'Приют') + '»'], glow=style == 'demon')
    sx, sy, sz = fr.at(dc + 1, G + 3, -1)
    sign_text(b, sx, sy, sz, fr.F, wood, ['Комнаты', 'для путников', 'ночлег • еда', ''], glow=style == 'demon')
    fence = f'minecraft:{wood}_fence' if style != 'demon' else 'minecraft:nether_brick_fence'
    for du in (-1, 2):
        for y in (G, G + 1):
            x, _, z = fr.at(dc + du, y, -1)
            b.set(x, y, z, fence, waterlogged='false')
        x, _, z = fr.at(dc + du, G + 2, -1)
        b.lantern(x, G + 2, z)
    for du in (-2, 3):
        x, y, z = fr.at(dc + du, G + 3, -1)
        b.set(x, y, z, b.st['banner'].replace('_banner', '_wall_banner'), facing=fr.F)

    # ---- трапезная ----
    bx = min(8, W - 11)
    for u in range(2, bx + 1):
        c = L['counter']
        if 'log' in c:
            fr.set(u, G, 10, c, axis=_axis_u(fr))
        else:
            fr.set(u, G, 10, c)
        if u % 3 == 0:
            fr.set(u, G + 1, 10, 'minecraft:flower_pot') if u % 2 else fr.set(u, G + 1, 10, 'minecraft:candle', candles='2', lit='true',
                                                                                 waterlogged='false')
    # стулья у стойки
    for u in range(3, bx + 1, 2):
        x, y, z = fr.at(u, G, 9)
        b.stair(x, y, z, f'minecraft:{wood}_stairs' if style != 'dwarf' else 'minecraft:dark_oak_stairs', fr.F)
    # бочки-кадки за стойкой, на полке — бутылки (бочки на боку)
    for u in range(1, bx + 1):
        fr.set(u, G, 14, 'minecraft:barrel', facing=fr.F, open='false')
        fr.set(u, G + 1, 14, 'minecraft:barrel', facing=fr.F, open='false') if u % 2 else \
            fr.set(u, G + 1, 14, f'minecraft:{wood}_slab' if style != 'demon' else 'minecraft:crimson_slab', type='top', waterlogged='false')
        if u % 4 == 0:
            fr.set(u, G + 2, 14, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='true', has_bottle_2='false')
        elif u % 2 == 0:
            fr.set(u, G + 2, 14, 'minecraft:candle', candles='3', lit='true', waterlogged='false')
    c = L['counter']
    for v in (11, 13):
        fr.set(bx + 1, G, v, c, **({'axis': _axis_v(fr)} if 'log' in c else {}))
    fr.set(bx + 1, G, 12, 'minecraft:smoker', facing=fr.L, lit='true')
    fr.set(bx + 1, G, 14, 'minecraft:chest', facing=fr.L, type='single', waterlogged='false')
    fr.set(bx + 1, G + 1, 11, 'minecraft:flower_pot')
    hx, hy, hz = fr.at(4, G, 12)
    b.workshops.append(('hosteler', RACE_NPC[style], hx, G, hz))
    # очаг у левой стены (кладка, огонь, полка над ним) и труба снаружи
    for v in (4, 5, 6):
        fr.set(1, G + 2, v, L['hearth'])
        fr.set(1, G + 3, v, L['hearth'])
    fr.set(1, G, 4, L['hearth'])
    fr.set(1, G + 1, 4, L['hearth'])
    fr.set(1, G, 6, L['hearth'])
    fr.set(1, G + 1, 6, L['hearth'])
    fr.set(1, G, 5, 'minecraft:campfire' if style != 'demon' else 'minecraft:soul_campfire', lit='true', signal_fire='false', facing=fr.R,
           waterlogged='false')
    fr.set(1, G + 4, 5, L['hearth'])
    for u in (2, 3, 4):
        for v in (4, 5, 6):
            fr.set(u, G, v, f'minecraft:{L["rug"]}_carpet')
    # столы со стульями
    tables = [u for u in range(3, hu - 1, 4) if abs(u - dc) > 2 and abs(u - dc - 1) > 2]
    for tu in tables:
        for tv in (2, 7) if tu > 4 else (8,):
            if fr.get(tu, G, tv) and fr.get(tu, G, tv)[0] != 'minecraft:air':
                continue
            fr.set(tu, G, tv, fence, waterlogged='false')
            fr.set(tu, G + 1, tv, f'minecraft:{wood}_pressure_plate' if style != 'dwarf' else 'minecraft:polished_blackstone_pressure_plate',
                   powered='false')
            for du, side in ((-1, fr.L), (1, fr.R)):
                x, y, z = fr.at(tu + du, G, tv)
                b.stair(x, y, z, f'minecraft:{wood}_stairs', side)
            x, y, z = fr.at(tu, G + 4, tv)
            b.set(x, y, z, 'minecraft:chain', axis='y', waterlogged='false')
            x, y, z = fr.at(tu, G + 3, tv)
            b.lantern(x, y, z, hanging=True)
    # ---- лестницы в холле ----
    st_block = f'minecraft:{wood}_stairs' if style not in ('dwarf',) else 'minecraft:deepslate_brick_stairs'
    for kk in range(6):
        for u in (W - 3, W - 2):
            x, y, z = fr.at(u, G + kk, 14 - kk)
            b.stair(x, y, z, st_block, fr.F)
            for yy in range(G + kk + 1, G + kk + 3):
                if yy <= G + 5:
                    fr.set(u, yy, 14 - kk, 'minecraft:air')
    for v in range(9, 15):
        for u in (W - 3, W - 2):
            if fr.get(u, G + 5, v)[0] != 'minecraft:air' and not ('stairs' in fr.get(u, G + 5, v)[0]):
                fr.set(u, G + 5, v, 'minecraft:air')
        fr.set(hu, G + 6, v, fence, waterlogged='false')
    if nf == 2:
        for kk in range(4):
            for u in (W - 3, W - 2):
                x, y, z = fr.at(u, G + 6 + kk, 1 + kk)
                b.stair(x, y, z, st_block, fr.B)
        for v in range(1, 5):
            for u in (W - 3, W - 2):
                if 'stairs' not in fr.get(u, G + 9, v)[0]:
                    fr.set(u, G + 9, v, 'minecraft:air')
            fr.set(hu, G + 10, v, fence, waterlogged='false')
    # ---- этажи с комнатами ----
    n = 0
    room_inner = L['inner']
    for j in range(nf):
        Wj = G + 6 + 4 * j
        # стена холла с проёмом в коридор
        for v in range(1, D - 1):
            for y in range(Wj, Wj + 3):
                if v in (7, 8):
                    continue
                fr.set(hu - 1, y, v, room_inner, **({'axis': 'y'} if 'wood' in room_inner or 'log' in room_inner else {}))
        # стены коридора и перегородки
        for u in range(1, hu - 1):
            for y in range(Wj, Wj + 3):
                for v in (6, 9):
                    fr.set(u, y, v, room_inner, **({'axis': 'y'} if 'wood' in room_inner or 'log' in room_inner else {}))
        for i in range(k - 1):
            us = 1 + 7 * i + 6
            for y in range(Wj, Wj + 3):
                for v in list(range(1, 6)) + list(range(10, 15)):
                    fr.set(us, y, v, room_inner, **({'axis': 'y'} if 'wood' in room_inner or 'log' in room_inner else {}))
        # коридор: дорожка и фонари
        for u in range(1, hu - 1):
            for v in (7, 8):
                fr.set(u, Wj, v, f'minecraft:{L["carpet"]}_carpet')
            if u % 5 == 3:
                x, y, z = fr.at(u, Wj + 2, 7 + (u // 5) % 2)
                b.lantern(x, y, z, hanging=True)
        for side in (0, 1):
            for i in range(k):
                n += 1
                r0 = 1 + 7 * i

                def V(v):
                    return v if side == 0 else D - 1 - v
                # дверь в стене коридора
                dv = V(6)
                x, y, z = fr.at(r0 + 2, Wj, dv)
                b.set(x, Wj, z, f'minecraft:{wood}_door', facing=fr.F if side == 0 else fr.B, half='lower', hinge='left', open='false',
                      powered='false')
                b.set(x, Wj + 1, z, f'minecraft:{wood}_door', facing=fr.F if side == 0 else fr.B, half='upper', hinge='left', open='false',
                      powered='false')
                door = (x, Wj, z)
                # табличка с номером со стороны коридора
                x, y, z = fr.at(r0 + 3, Wj + 1, V(7))
                sign_text(b, x, y, z, fr.B if side == 0 else fr.F, wood, ['', 'Комната', '№ %d' % n, ''], glow=style == 'demon')
                # убранство
                bed_foot = fr.at(r0, Wj, V(2))
                b.bed(bed_foot[0], Wj, bed_foot[2], fr.F if side == 0 else fr.B, L['bed'])
                x, y, z = fr.at(r0 + 1, Wj, V(1))
                b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
                b.nbt[(x, y, z)] = {'LootTable': 'minecraft:empty'}
                b.set(x, y + 1, z, 'minecraft:candle', candles='1', lit='true', waterlogged='false')
                x, y, z = fr.at(r0 + 4, Wj, V(1))
                b.set(x, y, z, 'minecraft:furnace', facing=fr.B if side == 0 else fr.F, lit='false')
                x, y, z = fr.at(r0 + 5, Wj, V(1))
                b.set(x, y, z, 'minecraft:ender_chest', facing=fr.B if side == 0 else fr.F, waterlogged='false')
                x, y, z = fr.at(r0 + 5, Wj, V(2))
                b.set(x, y, z, 'minecraft:chest', facing=fr.L, type='single', waterlogged='false')
                b.nbt[(x, y, z)] = {'LootTable': 'minecraft:empty'}
                x, y, z = fr.at(r0 + 5, Wj, V(3))
                b.set(x, y, z, 'minecraft:crafting_table')
                for u in range(r0 + 1, r0 + 5):
                    for v in (3, 4):
                        fr.set(u, Wj, V(v), f'minecraft:{L["rug"]}_carpet')
                x, y, z = fr.at(r0 + 3, Wj + 2, V(3))
                b.lantern(x, y, z, hanging=True)
                # окно наружу
                for u in (r0 + 2, r0 + 3):
                    if (u % 4) != 0:
                        window(u, Wj + 1, V(0))
                lo = fr.at(r0, Wj, V(1))
                hi = fr.at(r0 + 5, Wj + 2, V(5))
                SEC.anchor(b, 'inn_room_%d_lo' % n, *lo)
                SEC.anchor(b, 'inn_room_%d_hi' % n, *hi)
                SEC.anchor(b, 'inn_room_%d_door' % n, *door)
        # уголок отдыха в холле верхнего этажа
        if j == nf - 1:
            Wt = Wj
            fr.set(W - 2, Wt, 1 if nf == 1 else 10, 'minecraft:bookshelf')
            fr.set(W - 3, Wt, 1 if nf == 1 else 10, 'minecraft:potted_fern' if style != 'demon' else 'minecraft:potted_crimson_roots')
    # ---- крыша ----
    roof = b.st['roofs'][L['roof'] % len(b.st['roofs'])]
    b.roof(min(fr.x0, fr.x1), min(fr.z0, fr.z1), max(fr.x0, fr.x1), max(fr.z0, fr.z1), top_layer + 1, roof)
    # труба очага — снаружи у левой стены, выше конька
    ridge = top_layer + 1 + (min(W, D) + 1) // 2
    for y in range(G, ridge + 2):
        fr.set(-1, y, 5, L['chimney'])
        if y < G + 3:
            fr.set(-1, y, 4, L['chimney'])
            fr.set(-1, y, 6, L['chimney'])
    fr.set(-1, ridge + 2, 5, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    _decor(b, fr, style, dc, G, top_layer)
    _city_decor(b, fr, L.get('extra'), dc, G, wood)
    print(f'  постоялый двор {cid}: {n} комнат, этажей {nf}')
    return n


def _decor(b, fr, style, dc, G, top):
    """Убранство снаружи по народу: у каждого двора — свой облик."""
    W, D = fr.W, fr.D

    def put(u, y, v, blk, **p):
        cur = fr.get(u, y, v)
        if cur is None or cur[0] == 'minecraft:air':
            fr.set(u, y, v, blk, **p)
    if style == 'human':
        # ящики с цветами под окнами второго этажа и бочки у входа
        for u in range(2, W - 2):
            if fr.get(u, G + 7, 0) and 'pane' in fr.get(u, G + 7, 0)[0]:
                put(u, G + 6, -1, 'minecraft:spruce_trapdoor', facing=fr.F, half='top', open='false', powered='false', waterlogged='false')
                put(u, G + 7, -1, 'minecraft:potted_red_tulip' if u % 2 else 'minecraft:potted_azure_bluet')
        put(1, G, -1, 'minecraft:barrel', facing='up', open='false')
        put(W - 2, G, -1, 'minecraft:barrel', facing='up', open='false')
        put(W - 2, G + 1, -1, 'minecraft:potted_oak_sapling')
    elif style == 'elf':
        # живые кусты азалии по углам, мох у стен
        for u in (-1, W):
            for v in (-1, D):
                put(u, G, v, 'minecraft:flowering_azalea')
                put(u, G + 1, v, 'minecraft:flowering_azalea_leaves', persistent='true', distance='1', waterlogged='false')
        for u in range(1, W - 1, 3):
            put(u, G, -1, 'minecraft:moss_carpet') if abs(u - dc) > 2 else None
    elif style == 'dwarf':
        # кованые фонари на цепях и наковальня-вывеска
        for u in (1, W - 2):
            put(u, G + 4, -1, 'minecraft:chain', axis='y', waterlogged='false')
            put(u, G + 3, -1, 'minecraft:lantern', hanging='true', waterlogged='false')
        put(W - 3, G, -1, 'minecraft:anvil', facing=fr.L)
        put(2, G, -1, 'minecraft:barrel', facing='up', open='false')
    elif style == 'orc':
        # черепа на кольях, костёр у входа
        for u in (dc - 3, dc + 4):
            put(u, G, -2, 'minecraft:spruce_fence', waterlogged='false')
            put(u, G + 1, -2, 'minecraft:skeleton_skull', rotation='8')
        put(W - 3, G - 1, -3, 'minecraft:mud_bricks')
        put(W - 3, G, -3, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
    elif style == 'beast':
        # оленьи рога (кости) над дверью, сено и шкуры
        for du in (0, 1):
            put(dc + du, G + 4, -1, 'minecraft:bone_block', axis='y')
        put(1, G, -1, 'minecraft:hay_block', axis='y')
        put(2, G, -1, 'minecraft:hay_block', axis='x')
        put(W - 2, G, -1, 'minecraft:white_wool')
    elif style == 'demon':
        # плачущий обсидиан и души в фонарях
        for u in (0, W - 1):
            for y in (G + 2, G + 6):
                fr.set(u, y, 0, 'minecraft:crying_obsidian')
        put(1, G, -1, 'minecraft:magma_block')
        put(W - 2, G, -1, 'minecraft:soul_campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')


def _city_decor(b, fr, extra, dc, G, wood):
    """Особые черты двора конкретного города."""
    W = fr.W

    def put(u, y, v, blk, **p):
        cur = fr.get(u, y, v)
        if cur is None or cur[0] == 'minecraft:air':
            fr.set(u, y, v, blk, **p)
    if extra == 'harbor':
        # якорь на цепи у входа, бочки с рыбой, спасательный круг-фонарь, сети из паутины под стрехой
        for du in (-3, 4):
            for y in range(G + 1, G + 4):
                put(dc + du, y, -1, 'minecraft:chain', axis='y', waterlogged='false')
            put(dc + du, G, -1, 'minecraft:anvil', facing=fr.R)
        for u in (1, 2, W - 3):
            put(u, G, -1, 'minecraft:barrel', facing='up', open='false')
        put(1, G + 1, -1, 'minecraft:barrel', facing=fr.F, open='false')
        put(W - 2, G, -1, 'minecraft:barrel', facing='up', open='false')
        put(W - 2, G + 1, -1, 'minecraft:lantern', hanging='false', waterlogged='false')
    elif extra == 'fort':
        # стойка с оружием и мишень у входа — постояльцы-солдаты
        put(1, G, -1, f'minecraft:{wood}_fence', waterlogged='false')
        put(1, G + 1, -1, 'minecraft:tripwire_hook', facing=fr.F, attached='false', powered='false')
        put(W - 2, G, -1, 'minecraft:target', power='0')
        put(W - 3, G, -1, 'minecraft:hay_block', axis='y')
        x, y, z = fr.at(dc - 1, G + 6, -1)
        b.set(x, y, z, b.st['banner'].replace('_banner', '_wall_banner'), facing=fr.F)
        x, y, z = fr.at(dc + 2, G + 6, -1)
        b.set(x, y, z, b.st['banner'].replace('_banner', '_wall_banner'), facing=fr.F)
    elif extra == 'capital':
        # золотые знамёна грифона над входом и дорожка из ковра до дверей
        for du in (-1, 2):
            x, y, z = fr.at(dc + du, G + 6, -1)
            b.set(x, y, z, 'minecraft:yellow_wall_banner', facing=fr.F)
        for v in (-1, -2):
            for du in (0, 1):
                put(dc + du, G, v, 'minecraft:red_carpet')


# ---------------- круг телепорта ----------------

CIRCLE = {
    'human': dict(base='minecraft:polished_andesite', ring='minecraft:chiseled_stone_bricks', inner='minecraft:smooth_stone',
                  pillar='minecraft:stone_bricks', cap='minecraft:chiseled_quartz_block', glow='minecraft:sea_lantern'),
    'elf': dict(base='minecraft:calcite', ring='minecraft:moss_block', inner='minecraft:polished_diorite', pillar='minecraft:birch_log',
                cap='minecraft:amethyst_block', glow='minecraft:pearlescent_froglight'),
    'dwarf': dict(base='minecraft:deepslate_tiles', ring='minecraft:gold_block', inner='minecraft:polished_deepslate',
                  pillar='minecraft:deepslate_bricks', cap='minecraft:chiseled_deepslate', glow='minecraft:shroomlight'),
    'orc': dict(base='minecraft:packed_mud', ring='minecraft:bone_block', inner='minecraft:mud_bricks', pillar='minecraft:spruce_log',
                cap='minecraft:bone_block', glow='minecraft:jack_o_lantern'),
    'beast': dict(base='minecraft:cobblestone', ring='minecraft:bone_block', inner='minecraft:mossy_cobblestone', pillar='minecraft:spruce_log',
                  cap='minecraft:carved_pumpkin', glow='minecraft:shroomlight'),
    'demon': dict(base='minecraft:polished_blackstone', ring='minecraft:crying_obsidian', inner='minecraft:blackstone',
                  pillar='minecraft:polished_blackstone_bricks', cap='minecraft:gilded_blackstone', glow='minecraft:magma_block'),
}


def portal_circle(b, cx, cz, R=6):
    """Круг телепорта радиуса R с центром (cx, cz). Камень-якорь в центре — точка прибытия."""
    G = b.G
    P = CIRCLE[b.style]
    b.reserve(cx - R - 1, cz - R - 1, cx + R + 1, cz + R + 1)
    restamp = []
    for x in range(cx - R, cx + R + 1):
        for z in range(cz - R, cz + R + 1):
            d = math.hypot(x - cx, z - cz)
            if d > R + 0.4:
                continue
            if d < 0.5:
                blk = 'minecraft:lodestone'
            elif d < 1.6:
                blk = 'minecraft:amethyst_block'
            elif abs(d - 3) < 0.55:
                blk = P['ring']
            elif abs(d - R + 0.6) < 0.6:
                blk = P['ring'] if (int(math.degrees(math.atan2(z - cz, x - cx))) // 15) % 2 else P['base']
            elif d < 3:
                blk = P['inner']
            else:
                blk = P['base']
            b.set(x, G - 1, z, blk)
            b.set(x, G - 2, z, 'minecraft:stone')
            restamp.append((x, z))
            for y in range(G, G + 7):
                if (x, y, z) in b.blocks and b.blocks[(x, y, z)][0] != 'minecraft:air':
                    b.set(x, y, z, 'minecraft:air')
    # свечение под кольцом
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = int(round(cx + math.cos(a) * 3)), int(round(cz + math.sin(a) * 3))
        b.set(x, G - 1, z, P['glow'], **({'axis': 'y'} if 'froglight' in P['glow'] else {}))
    # четыре столба с огнями
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        x, z = int(round(cx + math.cos(a) * (R - 1))), int(round(cz + math.sin(a) * (R - 1)))
        for y in range(G, G + 4):
            pb = P['pillar']
            b.set(x, y, z, pb, **({'axis': 'y'} if 'log' in pb else {}))
        b.set(x, G + 4, z, P['cap'], **({'facing': 'south'} if 'pumpkin' in P['cap'] or 'jack' in P['cap'] else {}))
        b.set(x, G + 5, z, 'minecraft:end_rod', facing='up')
        b.set(x, G, z, 'minecraft:amethyst_block')
        # кристаллы у подножия
        for (dx, dz, f) in ((1, 0, 'east'), (-1, 0, 'west'), (0, 1, 'south'), (0, -1, 'north')):
            if math.hypot(x + dx - cx, z + dz - cz) > R - 1:
                b.set(x + dx, G, z + dz, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
    # кафедра мага и сам маг
    mx, mz = cx, cz + 4
    b.set(cx, G, cz + 5, 'minecraft:lectern', facing='north', has_book='false', powered='false')
    b.workshops.append(('teleporter', RACE_NPC[b.style], mx, G, mz))
    SEC.anchor(b, 'portal', cx, G, cz)
    # кварталы потом красят землю вокруг — круг кладётся заново перед сохранением (gen_mapbuild.place → restamp)
    keep = {}
    for (x, z) in restamp:
        for y in range(G - 2, G + 7):
            if (x, y, z) in b.blocks:
                keep[(x, y, z)] = (b.blocks[(x, y, z)], b.nbt.get((x, y, z)))
    if not hasattr(b, 'restamp'):
        b.restamp = {}
    b.restamp.update(keep)


# ---------------- размещение в городе ----------------

def _cardinal(dx, dz):
    return ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')


def avenue_spot(b, C, a, r0, r1, wf, inside, half=3, gap=1):
    """
    Место под здание вдоль проспекта (луч из центра под углом a, от r0 до r1): здание стоит сбоку,
    фасадом к проспекту. wf(front) → (w, d) — размер по x и z. Возвращает (x0, z0, front) или None.
    """
    ca, sa = math.cos(a), math.sin(a)
    for t in range(int(r0), int(r1)):
        for s in (1, -1):
            nx, nz = -sa * s, ca * s           # от проспекта в сторону здания
            front = _cardinal(-nx, -nz)        # фасад смотрит на проспект
            w, d = wf(front)
            for g in range(gap, gap + 5):      # как можно ближе к проспекту
                off = half + g + (d if front in ('south', 'north') else w) / 2 + 0.5
                cx, cz = C + ca * t + nx * off, C + sa * t + nz * off
                x0, z0 = int(round(cx - w / 2)), int(round(cz - d / 2))
                x1, z1 = x0 + w - 1, z0 + d - 1
                if not all(inside(x, z) for x, z in ((x0, z0), (x1, z0), (x0, z1), (x1, z1))):
                    break
                if b.free(x0, z0, x1, z1):
                    return x0, z0, front
    return None


def place_inn(b, C, cid, angles, r0, r1, inside, half=3):
    """Найти место у одного из проспектов и построить постоялый двор."""
    for a in angles:
        sp = avenue_spot(b, C, a, r0, r1, lambda f: inn_footprint(cid, f), inside, half=half)
        if not sp:
            continue
        x0, z0, front = sp
        W, D, _, _ = inn_size(cid)
        w, d = inn_footprint(cid, front)
        # отступы: труба сбоку (−u, 1 клетка) и крыльцо спереди (2 клетки)
        if front == 'south':
            bx, bz = x0 + 1, z0
        elif front == 'north':
            bx, bz = x0 + 1, z0 + 3
        elif front == 'east':
            bx, bz = x0, z0 + 1
        else:
            bx, bz = x0 + 3, z0 + 1
        n = inn(b, bx, bz, front, cid)
        ax, az = Frame(b, bx, bz, W, D, front).xz((W - 5) // 2, -2)
        SEC.anchor(b, 'inn', *SEC.walk_spot(b, ax, az, b.G, 3))
        return n
    print('!! нет места для постоялого двора', cid)
    return 0


def place_circle(b, C, cid, angles, r0, r1, inside, R=6, half=3):
    for a in angles:
        sp = avenue_spot(b, C, a, r0, r1, lambda f: (2 * R + 3, 2 * R + 3), inside, half=half, gap=0)
        if sp:
            x0, z0, _ = sp
            portal_circle(b, x0 + R + 1, z0 + R + 1, R)
            print(f'  круг телепорта {cid}: {(x0 + R + 1, z0 + R + 1)}')
            return True
    print('!! нет места для круга телепорта', cid)
    return False


def town_extras(t, half=None):
    """Постоялый двор и круг телепорта в городе на каркасе Town — у проспектов, ближе к площади."""
    av = t.avenues or [(a, 12, t.ray(a), 2) for a in t.gates]
    inside = (lambda x, z: t.in_walls(x, z, 5))
    angles = [a for (a, _, _, _) in av]
    r_start = min(r0 for (_, r0, _, _) in av)
    h = half if half is not None else max(h for (_, _, _, h) in av)
    place_circle(t.b, t.C, t.cid, angles, r_start + 2, t.WR * 0.6, inside, half=h)
    place_inn(t.b, t.C, t.cid, angles[1:] + angles[:1], r_start + 4, t.WR - 8, inside, half=h)
