"""
Тайные места городов для сюжетов (CityStories.java): неприметный дом с люком в полу, который выглядит как доска,
подвал с уликами, треснувшая кладка (её можно разбить без наказания — CityLaw.secretWall) и святилище за ней.
Все важные точки записываются в b.anchors: имя → (x, y, z) — gen_mapbuild переводит их в мировые координаты.
"""
import math
import random

# стиль города → неприметный дом и подземелье под ним
PAL = {
    'human': dict(wall=['minecraft:cobblestone', 'minecraft:mossy_cobblestone', 'minecraft:cobblestone'], beam='minecraft:dark_oak_log',
                  floor='minecraft:spruce_planks', hatch='minecraft:spruce_trapdoor', roof='minecraft:spruce_slab',
                  under='minecraft:stone_bricks', crack='minecraft:cracked_stone_bricks', door='minecraft:spruce_door'),
    'elf': dict(wall=['minecraft:birch_planks', 'minecraft:stripped_birch_log', 'minecraft:birch_planks'], beam='minecraft:birch_log',
                floor='minecraft:birch_planks', hatch='minecraft:birch_trapdoor', roof='minecraft:dark_oak_slab',
                under='minecraft:mossy_stone_bricks', crack='minecraft:cracked_stone_bricks', door='minecraft:birch_door'),
    'dwarf': dict(wall=['minecraft:polished_deepslate', 'minecraft:deepslate_bricks', 'minecraft:cobbled_deepslate'], beam='minecraft:spruce_log',
                  floor='minecraft:spruce_planks', hatch='minecraft:spruce_trapdoor', roof='minecraft:deepslate_tile_slab',
                  under='minecraft:deepslate_bricks', crack='minecraft:cracked_deepslate_bricks', door='minecraft:spruce_door'),
    'orc': dict(wall=['minecraft:spruce_planks', 'minecraft:packed_mud', 'minecraft:spruce_planks'], beam='minecraft:spruce_log',
                floor='minecraft:spruce_planks', hatch='minecraft:spruce_trapdoor', roof='minecraft:mud_brick_slab',
                under='minecraft:mud_bricks', crack='minecraft:cracked_stone_bricks', door='minecraft:spruce_door'),
    'beast': dict(wall=['minecraft:spruce_log', 'minecraft:stripped_spruce_log', 'minecraft:spruce_log'], beam='minecraft:spruce_log',
                  floor='minecraft:spruce_planks', hatch='minecraft:spruce_trapdoor', roof='minecraft:spruce_slab',
                  under='minecraft:mossy_cobblestone', crack='minecraft:cracked_stone_bricks', door='minecraft:spruce_door'),
    'demon': dict(wall=['minecraft:blackstone', 'minecraft:polished_blackstone_bricks', 'minecraft:blackstone'], beam='minecraft:crimson_stem',
                  floor='minecraft:crimson_planks', hatch='minecraft:crimson_trapdoor', roof='minecraft:blackstone_slab',
                  under='minecraft:polished_blackstone_bricks', crack='minecraft:cracked_polished_blackstone_bricks', door='minecraft:crimson_door'),
}


def _wall_block(p, x, y, z):
    w = p['wall']
    return w[(x * 3 + y * 7 + z * 5) % len(w)]


def walk_spot(b, x, z, y, r=6):
    """Ближайшая клетка, где можно стоять: твёрдый блок под ногами и два воздуха (или пусто в шаблоне)."""
    def solid(px, py, pz):
        v = b.blocks.get((px, py, pz))
        return v is not None and v[0] not in ('minecraft:air', 'minecraft:water') and 'slab' not in v[0] and 'carpet' not in v[0]

    def free(px, py, pz):
        v = b.blocks.get((px, py, pz))
        return v is None or v[0] == 'minecraft:air'
    best = None
    for rr in range(r + 1):
        for dx in range(-rr, rr + 1):
            for dz in range(-rr, rr + 1):
                if max(abs(dx), abs(dz)) != rr:
                    continue
                for dy in (0, 1, -1, 2, -2):
                    px, py, pz = x + dx, y + dy, z + dz
                    if solid(px, py - 1, pz) and free(px, py, pz) and free(px, py + 1, pz):
                        return (px, py, pz)
        if best:
            return best
    return (x, y, z)


def anchor(b, name, x, y, z):
    if not hasattr(b, 'anchors'):
        b.anchors = {}
    b.anchors[name] = (int(x), int(y), int(z))


def entity_anchor(b, name, ids, fallback):
    """Место по сущности шаблона (вождь, капитан): берём первую подходящую."""
    for e in b.entities:
        if e[3] in ids:
            anchor(b, name, *walk_spot(b, e[0], e[2], e[1], 3))
            return
    anchor(b, name, *fallback)


# ---------------- убранство тайных комнат ----------------

def _candles(b, x, y, z, color='black', n=3):
    b.set(x, y, z, 'minecraft:%s_candle' % color, candles=str(n), lit='true', waterlogged='false')


def _decor(b, theme, x0, z0, x1, z1, y, rnd):
    """Святилище: x0..x1, z0..z1 — пол на y-1, воздух y..y+3."""
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    if theme in ('ash_cult', 'void'):
        floor = 'minecraft:polished_blackstone_bricks' if theme == 'ash_cult' else 'minecraft:obsidian'
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, floor if (x + z) % 4 else 'minecraft:crying_obsidian' if theme == 'void' else 'minecraft:blackstone')
        # алтарь
        for dx in (-1, 0, 1):
            b.set(cx + dx, y, z0 + 1, 'minecraft:polished_blackstone_brick_stairs' if dx else 'minecraft:chiseled_polished_blackstone',
                  **({'facing': 'south', 'half': 'bottom', 'shape': 'straight', 'waterlogged': 'false'} if dx else {}))
        b.set(cx, y + 1, z0 + 1, 'minecraft:soul_campfire', lit='true', facing='south', signal_fire='false', waterlogged='false')
        b.set(cx - 1, y + 1, z0 + 1, 'minecraft:skeleton_skull', rotation='8')
        b.set(cx + 1, y + 1, z0 + 1, 'minecraft:wither_skeleton_skull', rotation='8')
        for z in range(z0 + 3, z1, 2):
            for x in (x0 + 1, x1 - 1):
                _candles(b, x, y, z, 'black' if theme == 'ash_cult' else 'purple', 1 + (z % 3))
        for x in range(cx - 1, cx + 2):
            for z in range(z0 + 3, z1):
                b.set(x, y, z, 'minecraft:red_carpet' if theme == 'ash_cult' else 'minecraft:purple_carpet')
        for x in (x0 + 1, x1 - 1):
            b.set(x, y + 3, z0 + 1, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
            b.set(x, y + 3, z1 - 1, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
        if theme == 'void':
            # недостроенная рамка врат
            for dz in range(-2, 3):
                for yy in range(y, y + 4):
                    if abs(dz) == 2 or yy in (y, y + 3):
                        b.set(cx, yy, cz + 1 + dz, 'minecraft:crying_obsidian' if (dz + yy) % 3 else 'minecraft:obsidian')
                    else:
                        b.set(cx, yy, cz + 1 + dz, 'minecraft:air')
            b.set(cx, y + 3, cz + 1, 'minecraft:air')  # рамка не сомкнута
    elif theme == 'smugglers':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:spruce_planks' if (x + z) % 3 else 'minecraft:stripped_spruce_wood', axis='y')
        for x in range(x0, x1 + 1, 2):
            for z in (z0, z1):
                b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
                if rnd.random() < 0.5:
                    b.set(x, y + 1, z, 'minecraft:barrel', facing='up', open='false')
        b.set(cx, y, cz, 'minecraft:spruce_fence', waterlogged='false')
        b.set(cx, y + 1, cz, 'minecraft:spruce_pressure_plate', powered='false')
        b.set(cx + 1, y, cz, 'minecraft:spruce_stairs', facing='west', half='bottom', shape='straight', waterlogged='false')
        b.set(cx - 1, y, cz, 'minecraft:spruce_stairs', facing='east', half='bottom', shape='straight', waterlogged='false')
        b.set(cx, y + 3, cz, 'minecraft:lantern', hanging='true', waterlogged='false')
        for z in range(z0 + 1, z1, 3):
            b.set(x1, y, z, 'minecraft:black_wool')
            b.set(x1, y + 1, z, 'minecraft:cobweb')
    elif theme == 'rot':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:mycelium' if (x * 5 + z * 3) % 4 else 'minecraft:rooted_dirt')
                if (x * 7 + z * 11) % 6 == 0:
                    b.set(x, y + 3, z, 'minecraft:hanging_roots', waterlogged='false')
                elif (x * 3 + z * 5) % 9 == 0:
                    b.set(x, y, z, 'minecraft:red_mushroom' if (x + z) % 2 else 'minecraft:brown_mushroom')
        for k in range(4):
            b.set(cx - 2 + k, y, z0 + 1, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='true', has_bottle_2='false')
        b.set(cx, y, cz, 'minecraft:cauldron')
        b.set(cx, y + 3, cz, 'minecraft:shroomlight')
        for x in (x0 + 1, x1 - 1):
            b.set(x, y + 3, cz, 'minecraft:glow_lichen', down='false', up='true', north='false', south='false', east='false', west='false',
                  waterlogged='false')
    elif theme in ('goblin_forge', 'goblin_gold'):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:deepslate_tiles' if (x + z) % 3 else 'minecraft:cracked_deepslate_tiles')
        b.set(cx, y, z0 + 1, 'minecraft:anvil', facing='east')
        b.set(cx - 2, y, z0 + 1, 'minecraft:blast_furnace', facing='south', lit='true')
        b.set(cx + 2, y, z0 + 1, 'minecraft:smithing_table')
        b.set(cx, y, cz + 1, 'minecraft:lava_cauldron')
        if theme == 'goblin_gold':
            for k, (dx, dz) in enumerate(((-2, 2), (2, 2), (-1, 3), (1, 3))):
                b.set(cx + dx, y, cz + dz, 'minecraft:raw_gold_block' if k % 2 else 'minecraft:gold_block')
        for x in (x0 + 1, x1 - 1):
            for z in (z0 + 1, z1 - 1):
                b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
            b.set(x, y + 3, cz, 'minecraft:lantern', hanging='true', waterlogged='false')
    elif theme == 'bloodcap':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:podzol' if (x + z) % 3 else 'minecraft:coarse_dirt')
                if (x * 5 + z * 7) % 4 == 0 and x not in (cx,) and z not in (cz,):
                    b.set(x, y, z, 'minecraft:red_mushroom')
        b.set(cx, y, z0 + 1, 'minecraft:crimson_fungus')
        b.set(cx - 1, y, z0 + 1, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='false', has_bottle_2='true')
        b.set(cx + 1, y, z0 + 1, 'minecraft:barrel', facing='up', open='false')
        b.set(cx, y + 3, cz, 'minecraft:lantern', hanging='true', waterlogged='false')
        for z in range(z0 + 2, z1, 3):
            b.set(x1, y, z, 'minecraft:bone_block', axis='y')
    elif theme == 'den':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:coarse_dirt' if (x + z) % 3 else 'minecraft:podzol')
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 1, cz + 2):
                b.set(x, y, z, 'minecraft:white_carpet' if (x + z) % 2 else 'minecraft:light_gray_carpet')
        # клетка из решёток
        for dx in range(-1, 2):
            for dz in range(-1, 2):
                for yy in (y, y + 1):
                    if abs(dx) == 1 or abs(dz) == 1:
                        b.set(x1 - 2 + dx, yy, z0 + 2 + dz, 'minecraft:iron_bars', waterlogged='false',
                              north='true' if dz > -1 else 'false', south='true' if dz < 1 else 'false',
                              east='true' if dx < 1 else 'false', west='true' if dx > -1 else 'false')
        b.set(cx, y, z0 + 1, 'minecraft:campfire', lit='true', facing='south', signal_fire='false', waterlogged='false')
        for x in (x0 + 1, x1 - 1):
            b.set(x, y, z1 - 1, 'minecraft:bone_block', axis='y')
            b.set(x, y + 1, z1 - 1, 'minecraft:skeleton_skull', rotation='0')
    elif theme == 'ice_crypt':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:packed_ice' if (x + z) % 3 else 'minecraft:blue_ice')
        for z in range(z0 + 1, z1, 3):
            for x in (x0 + 1, x1 - 1):
                b.set(x, y, z, 'minecraft:stone_brick_slab', type='bottom', waterlogged='false')
                b.set(x, y + 1, z, 'minecraft:skeleton_skull', rotation='4' if x == x0 + 1 else '12')
        b.set(cx, y, z0 + 1, 'minecraft:chiseled_stone_bricks')
        b.set(cx, y + 1, z0 + 1, 'minecraft:soul_lantern', hanging='false', waterlogged='false')
        for x in range(x0, x1 + 1, 3):
            b.set(x, y + 3, cz, 'minecraft:snow_block')
    elif theme == 'necro':
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                b.set(x, y - 1, z, 'minecraft:soul_soil' if (x + z) % 3 else 'minecraft:bone_block', **({} if (x + z) % 3 else {'axis': 'y'}))
        b.set(cx, y, cz, 'minecraft:soul_campfire', lit='true', facing='south', signal_fire='false', waterlogged='false')
        for dx, dz in ((-2, 0), (2, 0), (0, -2), (0, 2)):
            b.set(cx + dx, y, cz + dz, 'minecraft:skeleton_skull', rotation='0')
        for z in range(z0 + 1, z1, 2):
            b.set(x0 + 1, y, z, 'minecraft:bookshelf')
            b.set(x0 + 1, y + 1, z, 'minecraft:bookshelf')
        for x in (x0 + 1, x1 - 1):
            b.set(x, y + 3, z0 + 1, 'minecraft:soul_lantern', hanging='true', waterlogged='false')
    # сундук с добычей у дальней стены
    b.chest(x1 - 1, y, z1 - 1, 'litrpg:chests/city_stash')


def _box(b, x0, z0, x1, z1, yf, h, shell):
    """Пустая комната: пол на yf, воздух yf+1..yf+h, потолок, стены — shell."""
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            edge = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
            b.set(x, yf, z, shell)
            b.set(x, yf + h + 1, z, shell)
            for y in range(yf + 1, yf + h + 1):
                if edge:
                    if (x, y, z) not in b.blocks or b.blocks[(x, y, z)][0] == 'minecraft:air':
                        b.set(x, y, z, shell)
                else:
                    b.set(x, y, z, 'minecraft:air')


def secret(b, x0, z0, w, d, G, style, theme, door, prefix='', title_sign=None, inside_ok=None):
    """Неприметный дом w×d (двери door) с люком в полу, подвал с конторкой (улика) и треснувшая кладка,
    за которой — святилище theme. Якоря: <prefix>door, hatch, ledger, crack, sanctum."""
    p = PAL.get(style, PAL['human'])
    rnd = random.Random(x0 * 31 + z0 * 17)
    x1, z1 = x0 + w - 1, z0 + d - 1
    # ---- дом ----
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            corner = x in (x0, x1) and z in (z0, z1)
            b.set(x, G - 1, z, p['floor'])
            for y in range(G, G + 4):
                if corner:
                    b.set(x, y, z, p['beam'], axis='y')
                elif edge:
                    b.set(x, y, z, _wall_block(p, x, y, z))
                else:
                    b.set(x, y, z, 'minecraft:air')
            b.set(x, G + 4, z, p['roof'], type='bottom', waterlogged='false')
    # заколоченные окна
    for (wx, wz, f) in ((x0 + w // 2, z0, 'north'), (x0 + w // 2, z1, 'south'), (x0, z0 + d // 2, 'west'), (x1, z0 + d // 2, 'east')):
        if f == door:
            continue
        b.set(wx, G + 1, wz, 'minecraft:dark_oak_trapdoor', facing=f, half='bottom', open='true', powered='false', waterlogged='false')
    dx_, dz_ = {'north': (0, -1), 'south': (0, 1), 'west': (-1, 0), 'east': (1, 0)}[door]
    if door in ('north', 'south'):
        ddx, ddz = x0 + w // 2, (z0 if door == 'north' else z1)
    else:
        ddx, ddz = (x0 if door == 'west' else x1), z0 + d // 2
    b.door(ddx, G, ddz, door, p['door'])
    if theme == 'ash_cult':
        # знак культа над дверью: чёрное знамя и копоть
        b.set(ddx + dx_, G + 2, ddz + dz_, 'minecraft:black_wall_banner', facing=door)
        from mapbuild_lib import TList, TInt
        b.nbt[(ddx + dx_, G + 2, ddz + dz_)] = {'Patterns': TList(10, [{'Pattern': 'mc', 'Color': TInt(1)}, {'Pattern': 'flo', 'Color': TInt(14)}])}
        b.set(ddx, G + 3, ddz, 'minecraft:coal_block')
    anchor(b, prefix + 'door', ddx + dx_ * 2, G, ddz + dz_ * 2)
    # хлам внутри: бочки, паутина, разбитые ящики
    for (x, z) in ((x0 + 1, z1 - 1), (x1 - 1, z1 - 1), (x1 - 1, z0 + 1)):
        if (x, z) != (ddx - dx_, ddz - dz_):
            b.set(x, G, z, 'minecraft:barrel', facing='up', open='false')
            b.set(x, G + 3, z, 'minecraft:cobweb')
    b.set(x0 + w // 2, G + 3, z0 + d // 2, 'minecraft:lantern', hanging='true', waterlogged='false')
    # люк — в углу, той же породы, что и пол: «доска, которая скрипит иначе»
    hx, hz = x0 + 1, z0 + 1
    if (hx, hz) == (ddx - dx_, ddz - dz_):
        hx, hz = x1 - 2, z0 + 1
    b.set(hx, G - 1, hz, p['hatch'], facing='south', half='top', open='false', powered='false', waterlogged='false')
    b.set(hx + 1, G, hz, 'minecraft:barrel', facing='up', open='false')
    anchor(b, prefix + 'hatch', hx, G, hz)
    # ---- подвал ----
    yf = G - 8          # пол подвала, воздух yf+1..yf+4
    assert yf >= 1, 'тайник ниже дна шаблона: нужен ground >= 9'
    _box(b, x0 + 1, z0 + 1, x1 - 1, z1 - 1, yf, 4, p['under'])
    # шахта с лестницей (лестница держится за северную стенку)
    for y in range(yf + 5, G - 1):
        for (sx, sz) in ((hx - 1, hz), (hx + 1, hz), (hx, hz - 1), (hx, hz + 1)):
            b.set(sx, y, sz, p['under'])
    for y in range(yf + 1, G - 1):
        b.set(hx, y, hz, 'minecraft:ladder', facing='south', waterlogged='false')
    # конторка с уликой
    lx, lz = x0 + w // 2, z1 - 2
    b.set(lx, yf + 1, lz, 'minecraft:lectern', facing='north', has_book='false', powered='false')
    _candles(b, lx - 1, yf + 1, lz, 'white', 2)
    b.set(lx + 1, yf + 1, lz, 'minecraft:barrel', facing='up', open='false')
    b.set(lx, yf + 4, (z0 + z1) // 2, 'minecraft:lantern', hanging='true', waterlogged='false')
    anchor(b, prefix + 'ledger', lx, yf + 1, lz - 1)
    anchor(b, prefix + 'cellar', x0 + w // 2, yf + 1, z0 + d // 2)
    # ---- треснувшая кладка и святилище ----
    # святилище — дальше от двери, под соседней землёй
    sw, sd = 11, 9
    if door in ('north', 'south'):
        sgn = 1 if door == 'north' else -1
        cz_wall = z1 if sgn > 0 else z0
        sx0, sx1 = x0 + w // 2 - sw // 2, x0 + w // 2 + sw // 2
        sz0 = cz_wall + 1 if sgn > 0 else cz_wall - sd
        sz1 = sz0 + sd - 1
        crack = [(x0 + w // 2 + k, cz_wall) for k in (-1, 0, 1)]
        inner = (x0 + w // 2, cz_wall + sgn)
    else:
        sgn = 1 if door == 'west' else -1
        cx_wall = x1 if sgn > 0 else x0
        sz0, sz1 = z0 + d // 2 - sd // 2, z0 + d // 2 + sd // 2
        sx0 = cx_wall + 1 if sgn > 0 else cx_wall - sw
        sx1 = sx0 + sw - 1
        crack = [(cx_wall, z0 + d // 2 + k) for k in (-1, 0, 1)]
        inner = (cx_wall + sgn, z0 + d // 2)
    _box(b, sx0, sz0, sx1, sz1, yf, 4, p['under'])
    for (x, z) in crack:
        for y in (yf + 1, yf + 2):
            b.set(x, y, z, p['crack'])
        b.set(x, yf + 3, z, p['under'])
    cxw, czw = crack[1]
    # с внутренней стороны подвала — подсказка: паутина и следы копоти
    anchor(b, prefix + 'crack', cxw - (inner[0] - cxw), yf + 1, czw - (inner[1] - czw))
    _decor(b, theme, sx0, sz0, sx1, sz1, yf + 1, rnd)
    anchor(b, prefix + 'sanctum', (sx0 + sx1) // 2, yf + 1, (sz0 + sz1) // 2)
    b.reserve(x0 - 1, z0 - 1, x1 + 1, z1 + 1)
    return x0, z0, x1, z1


def sea_cave(b, QX, zc, wl, G, prefix=''):
    """Грот контрабандистов под набережной: проход из воды (с лодки или вплавь) и пещера под складами."""
    # проход от стенки набережной на запад
    for x in range(QX - 14, QX + 1):
        for dz in (-1, 0, 1):
            z = zc + dz
            b.set(x, wl, z, 'minecraft:stone')
            for y in range(wl + 1, wl + 4):
                b.set(x, y, z, 'minecraft:air')
        for z in (zc - 2, zc + 2):
            for y in range(wl, wl + 4):
                b.set(x, y, z, 'minecraft:stone' if (x + y) % 3 else 'minecraft:mossy_cobblestone')
        for dz in (-1, 0, 1):
            if (x, wl + 4, zc + dz) not in b.blocks:
                b.set(x, wl + 4, zc + dz, 'minecraft:stone')
    # вход прикрыт свисающими лианами и сетями
    for dz in (-1, 0, 1):
        b.set(QX, wl + 3, zc + dz, 'minecraft:cobweb')
    # пещера: 13×13, пол на wl
    x0, x1, z0, z1 = QX - 27, QX - 15, zc - 6, zc + 6
    for x in range(x0 - 1, x1 + 2):
        for z in range(z0 - 1, z1 + 2):
            edge = x in (x0 - 1, x1 + 1) or z in (z0 - 1, z1 + 1)
            b.set(x, wl, z, 'minecraft:stone' if (x * z) % 5 else 'minecraft:gravel')
            for y in range(wl + 1, wl + 4):
                if edge and not (x == x1 + 1 and abs(z - zc) <= 1):
                    b.set(x, y, z, 'minecraft:stone' if (x + y * 3 + z) % 4 else 'minecraft:andesite')
                elif not edge:
                    b.set(x, y, z, 'minecraft:air')
            if (x, wl + 4, z) not in b.blocks:
                b.set(x, wl + 4, z, 'minecraft:stone')
    rnd = random.Random(zc)
    for x in range(x0, x1 + 1):
        for z in (z0, z1):
            if rnd.random() < 0.6:
                b.set(x, wl + 1, z, 'minecraft:barrel', facing='up', open='false')
    for z in range(z0 + 2, z1 - 1, 3):
        b.set(x0, wl + 1, z, 'minecraft:chest', facing='east', type='single', waterlogged='false')
        b.nbt[(x0, wl + 1, z)] = {'LootTable': 'litrpg:chests/pirate_treasure'}
    b.set((x0 + x1) // 2, wl + 1, zc, 'minecraft:campfire', lit='true', facing='south', signal_fire='false', waterlogged='false')
    b.set((x0 + x1) // 2 + 2, wl + 1, zc + 2, 'minecraft:black_wool')
    b.set((x0 + x1) // 2 - 2, wl + 1, zc - 2, 'minecraft:black_banner', rotation='4')
    b.set((x0 + x1) // 2, wl + 3, zc - 4, 'minecraft:lantern', hanging='true', waterlogged='false')
    b.set((x0 + x1) // 2, wl + 3, zc + 4, 'minecraft:lantern', hanging='true', waterlogged='false')
    anchor(b, prefix + 'cave_mouth', QX + 3, wl + 1, zc)
    anchor(b, prefix + 'cave', (x0 + x1) // 2 + 2, wl + 1, zc)
    anchor(b, prefix + 'cave_chest', x0 + 1, wl + 1, zc + 1)


def poacher_camp(b, cx, cz, G, prefix=''):
    """Лагерь браконьеров за стенами: шатры, костёр, пустая клетка, шкуры на рамах."""
    from mapbuild_cities import tent, bonfire
    for x in range(cx - 9, cx + 10):
        for z in range(cz - 8, cz + 9):
            if math.hypot(x - cx, z - cz) < 9:
                b.set(x, G - 1, z, 'minecraft:coarse_dirt' if (x + z) % 3 else 'minecraft:podzol')
                for y in range(G, G + 6):
                    if (x, y, z) not in b.blocks:
                        b.set(x, y, z, 'minecraft:air')
    bonfire(b, cx, cz)
    tent(b, cx - 5, cz - 4, 2, 'brown', door='south')
    tent(b, cx + 5, cz - 4, 2, 'green', door='south')
    # клетка
    for dx in range(-1, 2):
        for dz in range(-1, 2):
            for y in (G, G + 1):
                if abs(dx) == 1 or abs(dz) == 1:
                    b.set(cx + dx, y, cz + 5 + dz, 'minecraft:iron_bars', waterlogged='false',
                          north='true' if dz > -1 else 'false', south='true' if dz < 1 else 'false',
                          east='true' if dx < 1 else 'false', west='true' if dx > -1 else 'false')
            b.set(cx + dx, G + 2, cz + 5 + dz, 'minecraft:spruce_slab', type='bottom', waterlogged='false')
    for k in range(3):
        b.set(cx - 7 + k, G, cz + 3, 'minecraft:spruce_fence', waterlogged='false')
        b.set(cx - 7 + k, G + 1, cz + 3, 'minecraft:brown_wool' if k % 2 else 'minecraft:white_wool')
    anchor(b, prefix + 'poachers', cx, G, cz + 2)
    anchor(b, prefix + 'cage', cx, G, cz + 3)


def town_secret(t, theme, a, r, w=11, d=9, prefix=''):
    """Тайник сюжета в городе на каркасе Town: ищем свободное место у (угол a, радиус r)."""
    sp = None
    for rr in (r, r + 8, r - 8, r + 16, r - 16):
        sp = t.spot(w, d, a, rr, inner=7, search=30)
        if sp:
            break
    if not sp:
        print('!! нет места для тайника', t.cid)
        return None
    print('  тайник', t.cid, theme, sp)
    return secret(t.b, sp[0], sp[1], w, d, t.G, t.b.style, theme, t.facing_center(*sp), prefix)


def finish(t, chief_ids):
    """Общие места встреч: площадь, главные ворота, вождь, мастерские."""
    b, C, G = t.b, t.C, t.G
    anchor(b, 'center', *walk_spot(b, C + 4, C + 4, G, 10))
    gx, gz = t.gate_xy(t.gates[0])
    anchor(b, 'gate', *walk_spot(b, int(C + (gx - C) * 0.85), int(C + (gz - C) * 0.85), G, 8))
    entity_anchor(b, 'chief', chief_ids, b.anchors['center'])
    for craft, npc, x, y, z in b.workshops:
        if 'craft_' + craft not in b.anchors:
            anchor(b, 'craft_' + craft, x, y, z)
