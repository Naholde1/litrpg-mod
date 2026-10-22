"""
Постройки «Мира Системы»: города, деревни, лагеря, караваны, заставы,
чудеса света, подземелья — всё на заранее выбранных местах карты.
Читает и дополняет src/main/resources/litrpg_world.json (поля pois, tiles, nether).
Шаблоны — data/litrpg/structures/map/*.nbt.
Запуск (после gen_world.py): python tools/gen_mapbuild.py, затем python tools/gen_world.py --art
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from worldmath import World  # noqa: E402
from mapbuild_lib import City, save_tiles, save_map, TByte, TInt, FACING_BYTE, DIRS, OPP  # noqa: E402,F401

RES = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
MAP_JSON = os.path.join(RES, 'litrpg_world.json')
ASSETS = os.path.join(RES, 'assets', 'litrpg')

POIS = []
TILES = []
WORKSHOPS = []
ANCHORS = {}   # сюжетные места: город → имя → [x, y, z] (мировые)


def place(build, name, ox, oy, oz, dim='overworld', tile=32):
    """Сохранить постройку плитками и записать их места (ox, oz — кратны 16)."""
    assert ox % 16 == 0 and oz % 16 == 0, (name, ox, oz)
    for craft, npc, x, y, z in getattr(build, 'workshops', []):
        WORKSHOPS.append(dict(city=name, craft=craft, npc=npc, x=ox + x, y=oy + y, z=oz + z))
    owner = getattr(build, 'anchor_city', name)
    for k, (x, y, z) in getattr(build, 'anchors', {}).items():
        ANCHORS.setdefault(owner, {})[k] = [ox + x, oy + y, oz + z]
    for dx, dz, tname in save_tiles(build, name, tile):
        TILES.append(dict(dim=dim, cx=(ox + dx) // 16, cz=(oz + dz) // 16, template='litrpg:map/' + tname, y=oy))


def painting_map(b, x, y, z, facing):
    """Карта мира 8×8 блоков на стене (x,y,z — примерное место перед стеной; точный якорь подбирает fix_paintings)."""
    b.entities.append((x, y, z, 'minecraft:painting', {'variant': 'litrpg:world_map', 'facing': TByte(FACING_BYTE[facing])}))


def map_board(b, x, z, frame='minecraft:stone_bricks', face='minecraft:smooth_stone', cap='minecraft:stone_brick_wall', r=40):
    """Отдельно стоящая стена-доска с картой мира (смотрит на юг) — ищет свободное место рядом с (x, z)."""
    G = b.G

    def empty(x0, z0, x1, z1):
        if not b.free(x0, z0, x1, z1) or x0 < 2 or z0 < 2 or x1 > b.size[0] - 3 or z1 > b.size[2] - 3:
            return False
        for px in range(x0, x1 + 1):
            for pz in range(z0, z1 + 1):
                for y in range(G, G + 12):
                    v = b.blocks.get((px, y, pz))
                    if v is not None and v[0] != 'minecraft:air':
                        return False
        return True

    for d in range(r):
        for px in range(x - d, x + d + 1):
            for pz in range(z - d, z + d + 1):
                if max(abs(px - x), abs(pz - z)) != d or not empty(px - 7, pz - 1, px + 6, pz + 3):
                    continue
                for bx in range(px - 6, px + 6):
                    for y in range(G - 1, G + 11):
                        b.set(bx, y, pz, frame if y in (G - 1, G + 10) or bx in (px - 6, px + 5) else face)
                    b.set(bx, G + 11, pz, cap, up='true', north='none', south='none', east='low', west='low', waterlogged='false') if cap.endswith('_wall') else b.set(bx, G + 11, pz, cap)
                    b.set(bx, G - 1, pz + 1, frame)
                    b.set(bx, G - 1, pz + 2, frame)
                painting_map(b, px + 3, G + 1, pz + 1, 'south')
                b.lantern(px - 5, G + 10, pz + 1)
                b.lantern(px + 4, G + 10, pz + 1)
                b.reserve(px - 7, pz - 1, px + 6, pz + 3)
                return
    print('  ! нет места для доски с картой', x, z)


# =====================================================================
#                              АРКАДИЯ
# =====================================================================

FLAT_R = 182  # ровная земля вокруг Аркадии (gen_world: flat=185)


def arcadia(city):
    S = 384
    C = S // 2
    b = City(S, 72, S, 'arcadia', 'human', ground=12)   # запас под землёй: тайный подвал сюжета
    G = b.G
    st = b.st
    WR = 112  # радиус стен

    # --- земля: мостовые улиц и газоны ---
    def is_street(x, z):
        dx, dz = x - C, z - C
        if abs(dx) <= 3 or abs(dz) <= 3:
            return True
        r = math.hypot(dx, dz)
        if abs(r - 96) <= 2:
            return True
        if (x % 22 in (0, 1, 2)) or (z % 22 in (0, 1, 2)):
            return r < 104
        return False

    for x in range(S):
        for z in range(S):
            r = math.hypot(x - C, z - C)
            if r > FLAT_R:
                continue
            if r > WR + 2:
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                continue
            b.set(x, G - 1, z, b.bp(st['street'], x, 0, z) if is_street(x, z) else 'minecraft:grass_block', **({} if is_street(x, z) else {'snowy': 'false'}))
            if is_street(x, z):
                b.set(x, G - 2, z, 'minecraft:stone')
    # подъездные дороги от ворот до края плитки
    for t in range(WR, FLAT_R):
        for w in range(-2, 3):
            for (x, z) in ((C + w, C - t), (C + w, C + t - 1), (C - t, C + w), (C + t - 1, C + w)):
                b.set(x, G - 1, z, b.bp(st['path'], x, 0, z))

    # --- стены-восьмиугольник с башнями и воротами ---
    pts = []
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        pts.append((C + round(math.cos(a) * WR / math.cos(math.radians(22.5))), C + round(math.sin(a) * WR / math.cos(math.radians(22.5)))))
    gates = [(C, C - WR, 4), (C, C + WR, 4), (C - WR, C, 4), (C + WR, C, 4)]
    for k in range(8):
        ax, az = pts[k]
        bx, bz = pts[(k + 1) % 8]
        b.wall_line(ax, az, bx, bz, h=11, thick=3, gates=gates)
    for (x, z) in pts:
        b.round_tower(x, z, 5, 17, roof_block='minecraft:dark_prismarine')
    for gx, gz, _ in gates:
        horiz = gz in (C - WR, C + WR)
        for s in (-7, 7):
            tx, tz = (gx + s, gz) if horiz else (gx, gz + s)
            b.round_tower(tx, tz, 3, 15, roof_block='minecraft:dark_prismarine', top_light=True)
        for s in (-2, 2):
            bx_, bz_ = (gx + s, gz + (1 if gz < C else -1) * 2) if horiz else (gx + (1 if gx < C else -1) * 2, gz + s)
            facing = ('south' if gz < C else 'north') if horiz else ('east' if gx < C else 'west')
            b.banner_wall(bx_, G + 8, bz_, facing)

    # --- центральная площадь с Обелиском Системы ---
    PR = 22
    for x in range(C - PR, C + PR + 1):
        for z in range(C - PR, C + PR + 1):
            r = math.hypot(x - C, z - C)
            if r <= PR:
                ring = int(r) % 6
                blk = 'minecraft:polished_diorite' if ring == 0 else 'minecraft:smooth_stone' if ring in (1, 5) else 'minecraft:polished_andesite'
                if int(r) % 6 == 0 and (x + z) % 2 == 0:
                    blk = 'minecraft:chiseled_stone_bricks'
                b.set(x, G - 1, z, blk)
    b.reserve(C - PR, C - PR, C + PR, C + PR)
    # бассейн вокруг обелиска
    for x in range(C - 8, C + 9):
        for z in range(C - 8, C + 9):
            r = math.hypot(x - C, z - C)
            if 6 < r <= 8.4:
                b.set(x, G, z, 'minecraft:quartz_bricks')
                b.slab(x, G + 1, z, 'minecraft:smooth_quartz_slab')
            elif 3 < r <= 6:
                b.set(x, G - 1, z, 'minecraft:prismarine_bricks')
                b.set(x, G, z, 'minecraft:water', level='0')
    # обелиск
    for x in range(C - 2, C + 3):
        for z in range(C - 2, C + 3):
            for y in range(G - 1, G + 3):
                b.set(x, y, z, 'minecraft:chiseled_quartz_block' if y == G + 2 else 'minecraft:quartz_bricks')
    OB = 30
    for y in range(G + 3, G + OB):
        for x in range(C - 1, C + 2):
            for z in range(C - 1, C + 2):
                edge = x != C or z != C
                if edge:
                    k = (y + (x - C) * 3 + (z - C) * 5) % 9
                    b.set(x, y, z, 'minecraft:amethyst_block' if k == 0 else 'minecraft:quartz_pillar' if k < 6 else 'minecraft:light_blue_stained_glass',
                          **({'axis': 'y'} if k and k < 6 else {}))
                else:
                    b.set(x, y, z, 'minecraft:sea_lantern')
    for x in range(C - 1, C + 2):
        for z in range(C - 1, C + 2):
            b.set(x, G + OB, z, 'minecraft:gold_block')
    b.set(C, G + OB + 1, C, 'minecraft:beacon')
    b.set(C, G + OB + 2, C, 'minecraft:light_blue_stained_glass')
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        b.set(C + dx, G + OB + 1, C + dz, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
    # фонари вокруг площади, скамейки, деревья в кадках
    for k in range(12):
        a = 2 * math.pi * k / 12
        x, z = C + round(math.cos(a) * (PR - 2)), C + round(math.sin(a) * (PR - 2))
        if abs(x - C) > 4 and abs(z - C) > 4:
            if k % 2 == 0:
                b.lamp_post(x, z, 3)
            else:
                b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
                b.tree(x, z, 5, 'minecraft:birch_log', 'minecraft:birch_leaves', 2.2)
    # стена с картой мира (северный край площади, смотрит на юг)
    mz = C - PR - 1
    for x in range(C - 6, C + 6):
        for y in range(G - 1, G + 11):
            b.set(x, y, mz, 'minecraft:stone_bricks' if y in (G - 1, G + 10) or x in (C - 6, C + 5) else 'minecraft:smooth_stone')
        b.set(x, G + 11, mz, 'minecraft:stone_brick_wall')
    painting_map(b, C + 3, G + 1, mz + 1, 'south')
    b.lantern(C - 5, G + 10, mz + 1, hanging=False)
    b.lantern(C + 4, G + 10, mz + 1, hanging=False)
    POIS.append(dict(type='spawn', id='spawn', name='Площадь Обелиска', x=city['x'], z=city['z'] + 26, y=city['y'] + 1, r=0))

    # --- королевский замок (север) ---
    # северная стена замка отступает от городской: между ними — улица от северных ворот в обход замка
    cx0, cz0, cx1, cz1 = C - 30, C - 100, C + 30, C - PR - 6
    lz0, lz1 = C - WR + 3, C - WR + 6          # поперечная улица вдоль городской стены
    lx0, lx1 = C - 40, C - 36                  # и продольная — к западному проспекту
    for x in range(lx0, C + 5):
        for z in range(lz0, lz1 + 1):
            b.set(x, G - 1, z, b.bp(st['street'], x, 0, z))
            b.set(x, G - 2, z, 'minecraft:stone')
    for z in range(lz0, C - 3):
        for x in range(lx0, lx1 + 1):
            b.set(x, G - 1, z, b.bp(st['street'], x, 0, z))
            b.set(x, G - 2, z, 'minecraft:stone')
    for z in range(C - WR, lz0):
        for x in range(C - 4, C + 5):
            b.set(x, G - 1, z, b.bp(st['street'], x, 0, z))
    b.reserve(lx0, lz0, C + 4, lz1)
    b.reserve(lx0, lz0, lx1, C - 4)
    b.reserve(C - 4, C - WR, C + 4, lz1)
    for t in range(lz0 + 8, C - 8, 10):
        b.lamp_post(lx1, t, 4)
    for t in range(lx0 + 6, C - 12, 10):
        b.lamp_post(t, lz1, 4)
    b.reserve(cx0, cz0, cx1, cz1)
    for x in range(cx0, cx1 + 1):
        for z in range(cz0, cz1 + 1):
            b.set(x, G - 1, z, 'minecraft:stone_bricks' if (x + z) % 3 else 'minecraft:polished_andesite')
    b.wall_line(cx0, cz0, cx1, cz0, h=10, thick=2)
    b.wall_line(cx0, cz0, cx0, cz1, h=10, thick=2)
    b.wall_line(cx1, cz0, cx1, cz1, h=10, thick=2)
    b.wall_line(cx0, cz1, cx1, cz1, h=10, thick=2, gates=[(C, cz1, 3)])
    for (x, z) in ((cx0, cz0), (cx1, cz0), (cx0, cz1), (cx1, cz1)):
        b.round_tower(x, z, 4, 16, roof_block='minecraft:blue_terracotta')
    b.reserved = [r for r in b.reserved if r != (cx0, cz0, cx1, cz1)]
    kx, kz = C - 13, cz0 + 8
    b.house(kx, kz, 27, 19, floors=3, door='south', roof=st['roofs'][3], interior='library', npc='litrpg:human_lord')
    # тронный зал: красная дорожка к трону
    for z in range(kz + 3, kz + 18):
        b.set(C, G, z, 'minecraft:red_carpet')
    b.set(C, G, kz + 2, 'minecraft:gold_block')
    b.stair(C, G + 1, kz + 2, 'minecraft:quartz_stairs', 'south')
    for s in (-1, 1):
        b.banner_wall(C + s * 3, G + 3, kz + 1, 'south', 'minecraft:blue_banner')
    for y in range(G + 12, G + 22):
        b.set(C, y, kz + 9, 'minecraft:dark_oak_fence' if y < G + 21 else 'minecraft:gold_block', waterlogged='false')
    b.set(C, G + 20, kz + 10, 'minecraft:blue_wall_banner', facing='south')
    import mapbuild_castle as MCS
    MCS.bailey(b, cx0, cz0, cx1, cz1, (kx, kz, kx + 26, kz + 18))

    # --- Гильдия искателей (запад) с картой мира ---
    gx0, gz0 = C - 98, C - 18
    b.house(gx0, gz0, 30, 22, floors=2, door='east', roof=st['roofs'][0], interior='library', npc='litrpg:human_knight')
    painting_map(b, gx0 + 1, G + 1, gz0 + 12, 'east')
    b.entities.append((gx0 + 10, G, gz0 + 5, 'litrpg:human_mage', {}))
    b.entities.append((gx0 + 18, G, gz0 + 15, 'litrpg:human_ranger', {}))

    # --- храм Света (юго-запад) ---
    tx0, tz0, tw, td = C - 70, C + 34, 24, 44
    b.reserve(tx0 - 1, tz0 - 1, tx0 + tw, tz0 + td)
    for x in range(tx0, tx0 + tw):
        for z in range(tz0, tz0 + td):
            edge = x in (tx0, tx0 + tw - 1) or z in (tz0, tz0 + td - 1)
            b.set(x, G - 1, z, 'minecraft:polished_diorite' if (x + z) % 2 else 'minecraft:smooth_quartz')
            for y in range(G, G + 14):
                if edge:
                    win = (z - tz0) % 5 in (2, 3) and 3 <= y - G <= 10 and x in (tx0, tx0 + tw - 1)
                    b.set(x, y, z, ('minecraft:yellow_stained_glass_pane' if (y - G) % 3 else 'minecraft:light_blue_stained_glass_pane')
                          if win else 'minecraft:stone_bricks' if (z - tz0) % 5 else 'minecraft:chiseled_stone_bricks', waterlogged='false')
                else:
                    b.set(x, y, z, 'minecraft:air')
    b.roof(tx0, tz0, tx0 + tw - 1, tz0 + td - 1, G + 14, ('minecraft:deepslate_tile_stairs', 'minecraft:deepslate_tile_slab', 'minecraft:deepslate_tiles'))
    for z in range(tz0 + 4, tz0 + td - 8, 2):
        for x in list(range(tx0 + 3, tx0 + 10)) + list(range(tx0 + 14, tx0 + tw - 3)):
            b.stair(x, G, z, 'minecraft:spruce_stairs', 'south')
    ax = tx0 + tw // 2
    for x in range(ax - 3, ax + 4):
        b.set(x, G, tz0 + td - 4, 'minecraft:quartz_block')
    b.set(ax, G + 1, tz0 + td - 4, 'minecraft:enchanting_table')
    for dx in (-3, 3):
        for y in range(G + 1, G + 4):
            b.set(ax + dx, y, tz0 + td - 4, 'minecraft:end_rod', facing='up')
    for z in range(tz0 + 1, tz0 + td - 1):
        b.set(ax, G, z, 'minecraft:light_blue_carpet') if z < tz0 + td - 5 else None
    b.door(ax, G, tz0, 'north')
    b.door(ax - 1, G, tz0, 'north')
    # шпиль
    for y in range(G, G + 34):
        for x in range(ax - 3, ax + 4):
            for z in range(tz0 - 4, tz0 + 3):
                r = max(abs(x - ax), abs(z - (tz0 - 1)))
                lim = 3 if y < G + 22 else 2 if y < G + 28 else 1 if y < G + 32 else 0
                if r == lim:
                    b.set(x, y, z, 'minecraft:stone_bricks' if y < G + 22 else 'minecraft:deepslate_tiles')
                elif r < lim:
                    if (x, y, z) not in b.blocks:
                        b.set(x, y, z, 'minecraft:air')
    b.set(ax, G + 34, tz0 - 1, 'minecraft:gold_block')
    b.set(ax, G + 35, tz0 - 1, 'minecraft:end_rod', facing='up')
    b.set(ax, G + 20, tz0 - 1, 'minecraft:bell', attachment='ceiling', facing='north', powered='false')
    b.entities.append((ax, G, tz0 + td - 7, 'litrpg:human_mage', {}))

    # --- рынок (восток): тесные ряды лавок вдоль проспекта ---
    import mapbuild_crafts as MCR
    import mapbuild_layout as ML
    mx0, mz0, mx1, mz1 = C + 28, C - 30, C + 78, C - 7
    ML.market(b, mx0, mz0, mx1, mz1, ['baker', 'butcher', 'fishmonger', 'farmer', 'herbalist', 'tailor', 'jeweler', 'alchemist',
                                      'fletcher', 'leatherworker'], 'litrpg:human_merchant', 'litrpg:human_citizen', 'arcadia')
    # казармы стражи и башня магов
    b.house(C + 30, C + 60, 20, 10, floors=2, door='west', interior='guard', npc='litrpg:human_knight')
    b.round_tower(C + 64, C - 64, 6, 28, roof_block='minecraft:purple_terracotta')
    b.entities.append((C + 64, G, C - 64, 'litrpg:human_mage', {}))

    # --- сюжет «Пепел под мостовой»: заброшенный склад с люком, подвал культа и святилище за треснувшей кладкой ---
    import mapbuild_secrets as SEC
    done = None
    for rr in range(64, 100, 6):
        for k in range(16):
            a = math.pi * 1.1 + k * 2 * math.pi / 16
            x0, z0 = int(C + math.cos(a) * rr) - 5, int(C + math.sin(a) * rr) - 4
            if math.hypot(x0 + 5 - C, z0 + 4 - C) < WR - 14 and b.free(x0 - 3, z0 - 3, x0 + 13, z0 + 11) and \
                    not any(abs(x - C) <= 5 or abs(z - C) <= 5 for x in (x0, x0 + 10) for z in (z0, z0 + 8)):
                door = 'east' if x0 + 5 < C else 'west'
                done = SEC.secret(b, x0, z0, 11, 9, G, 'human', 'ash_cult', door)
                break
        if done:
            break
    print('  тайник Аркадии:', done)
    SEC.anchor(b, 'captain', *SEC.walk_spot(b, C + 27, C + 65, G, 5))
    SEC.anchor(b, 'market', *SEC.walk_spot(b, C + 53, C - 18, G, 6))
    SEC.anchor(b, 'gate', *SEC.walk_spot(b, C, C + WR - 10, G, 6))
    # --- жилые кварталы: кривые улочки, террасы, дома вдоль улиц ---
    b.reserve(C - 4, 0, C + 4, S - 1)
    b.reserve(0, C - 4, S - 1, C + 4)
    b.reserve(cx0, cz0, cx1, cz1)
    import math as _m
    octa = [(_m.cos(_m.radians(45 * k)), _m.sin(_m.radians(45 * k))) for k in range(8)]

    def inside(x, z):
        return max((x - C) * cx + (z - C) * cz for cx, cz in octa) <= WR - 2

    org = ML.Organic(b, C, WR - 2, 'arcadia', plaza_r=0, terrace=3, inside=inside, rim=3)
    org.paint_ground()
    crafts = [c for c in MCR.CITY_CRAFTS['arcadia']]
    placed, homes = MCR.fill_city(b, org, crafts, 'litrpg:human_citizen', npc_chance=0.3)
    print('  мастерские Аркадии:', placed, '/', len(crafts), ' домов:', homes)
    org.street_life()
    org.courtyards('litrpg:human_citizen', density=2.0, stashes=8)
    # фонари вдоль проспектов
    for t in range(PR + 4, WR - 4, 9):
        for (x, z) in ((C + 4, C - t), (C - 4, C + t), (C - t, C - 4), (C + t, C + 4)):
            if b.free(x, z, x, z) and ML._air(b, x, G, z):
                b.lamp_post(x, z, 4)
    ML.outskirts(b, C, WR + 10, 'arcadia', 'litrpg:human_citizen', outer=FLAT_R - 4)
    SEC.entity_anchor(b, 'chief', ['litrpg:human_lord'], b.anchors['gate'])
    SEC.anchor(b, 'center', *SEC.walk_spot(b, C + 6, C + PR + 4, G, 8))
    for craft, npc, x, y, z in b.workshops:
        if 'craft_' + craft not in b.anchors:
            SEC.anchor(b, 'craft_' + craft, x, y, z)
    return b


def stall(b, x, z, color, facing):
    """Торговая палатка с торговцем."""
    G = b.G
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 and abs(dz) == 1:
                for y in range(G, G + 3):
                    b.set(x + dx, y, z + dz, b.st['fence'], waterlogged='false')
            b.set(x + dx, G + 3, z + dz, f'minecraft:{color}_wool')
    for dx in range(-1, 2):
        b.slab(x + dx, G, z + (1 if facing == 'south' else -1), 'minecraft:spruce_slab', 'top')
    b.set(x - 1, G, z, 'minecraft:barrel', facing='up', open='false')
    b.set(x + 1, G, z, 'minecraft:composter', level='0') if color in ('green', 'lime') else b.set(x + 1, G, z, 'minecraft:barrel', facing='up', open='false')
    b.entities.append((x, G, z, 'litrpg:human_merchant', {}))
    b.reserve(x - 3, z - 2, x + 3, z + 2)


def houses(b, C, R, seed, door_to_center=True, min_size=7, max_size=11, floors=(1, 2), npc_chance=0.25, npc='litrpg:human_citizen'):
    """Заполнить свободные участки внутри круга радиуса R домами."""
    rnd = random.Random(seed)
    cells = []
    # два прохода: крупные дома, потом домики поменьше в оставшихся промежутках
    for step, lo, hi in ((11, min_size, max_size), (7, 6, 8)):
        pass_cells = [(gx, gz, lo, hi) for gx in range(C - R, C + R, step) for gz in range(C - R, C + R, step)]
        rnd.shuffle(pass_cells)
        cells += pass_cells
    for gx, gz, lo, hi in cells:
        w = rnd.randint(lo, hi)
        d = rnd.randint(lo - 1, max(lo - 1, hi - 2))
        x0 = gx + rnd.randint(0, 4)
        z0 = gz + rnd.randint(0, 4)
        x1, z1 = x0 + w - 1, z0 + d - 1
        cx, cz = (x0 + x1) / 2, (z0 + z1) / 2
        if math.hypot(cx - C, cz - C) > R - max(w, d) / 2:
            continue
        if not b.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1):
            continue
        dx, dz = C - cx, C - cz
        door = ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')
        fl = rnd.choice(floors)
        kind = rnd.choice(['home', 'home', 'home', 'shop', 'library']) if fl > 1 else 'home'
        b.house(x0, z0, w, d, floors=fl, door=door, interior=kind, npc=npc if rnd.random() < npc_chance else None)
        # садик
        if rnd.random() < 0.4:
            fx = x0 - 3 if door != 'west' else x1 + 2
            if b.free(fx, z0, fx + 1, z1):
                for z in range(z0, z1 + 1):
                    b.set(fx, b.G - 1, z, 'minecraft:farmland', moisture='7')
                    b.set(fx, b.G, z, rnd.choice(['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes']), age='7')


# =====================================================================
#                              ЗАПУСК
# =====================================================================

def chunk_origin(x, z, size):
    ox = int(math.floor((x - size / 2) / 16)) * 16
    oz = int(math.floor((z - size / 2) / 16)) * 16
    return ox, oz


class Planner:
    """Ищет места под постройки: суша, ровно, не у воды, далеко от других построек."""

    def __init__(self, data):
        self.data = data
        flats = []
        for c in data['cities']:
            f = FIT.get(c['id'], {})
            flats.append([c['x'], c['z'], f.get('flat', c['flat']), c['y'], 60] + list(f.get('cut', [])))
            flats += f.get('extra', [])
        self.world = World(dict(data, flats=flats))
        self.taken = [(c['x'], c['z'], FIT.get(c['id'], {}).get('flat', c['flat']) + 50) for c in data['cities']]

    def ok(self, x, z, r, terr=None):
        if max(abs(x), abs(z)) > 4300:
            return None
        for tx, tz, tr in self.taken:
            if math.hypot(x - tx, z - tz) < tr + r + 30:
                return None
        hs = []
        for dx, dz in ((0, 0), (r, 0), (-r, 0), (0, r), (0, -r), (r * 0.7, r * 0.7), (-r * 0.7, -r * 0.7), (r * 0.7, -r * 0.7), (-r * 0.7, r * 0.7)):
            h, _, t, w = self.world.sample(x + dx, z + dz)
            if w != 0 or h < 64.5 or h > 150:
                return None
            hs.append(h)
        if max(hs) - min(hs) > 16:
            return None
        if terr is not None:
            t = self.world.territory(x, z)
            tid = self.data['territories'][t]['id'] if t >= 0 else 'wild'
            if tid not in terr:
                return None
        return int(round(sum(hs) / len(hs)))

    def find(self, x, z, r, terr=None, search=420):
        for d in range(0, search, 16):
            n = max(1, int(2 * math.pi * d / 24))
            for k in range(n):
                a = 2 * math.pi * k / n
                px, pz = x + math.cos(a) * d, z + math.sin(a) * d
                y = self.ok(px, pz, r, terr)
                if y is not None:
                    return int(px), int(pz), y
        return None

    def find_level(self, cx, cz, level, r, terr, tol=1):
        best = None
        for d in range(140, 2200, 20):
            for k in range(36):
                a = 2 * math.pi * k / 36
                px, pz = cx + math.cos(a) * d, cz + math.sin(a) * d
                if abs(self.world.level(px, pz) - level) > tol:
                    continue
                y = self.ok(px, pz, r, terr)
                if y is not None:
                    return int(px), int(pz), y
        return best

    def take(self, x, z, r):
        self.taken.append((x, z, r))


def footprint_r(b):
    """Радиус следа постройки от центра плитки: все колонны, где есть блоки на уровне земли и выше
    (за срезом b.fit['cut'] — вода, там площадка не нужна)."""
    G = b.G
    cx, cz = b.size[0] / 2, b.size[2] / 2
    cut = getattr(b, 'fit', {}).get('cut')
    cols = set()
    for (x, y, z), v in b.blocks.items():
        if y >= G - 1 and v[0] != 'minecraft:air':
            cols.add((x, z))
    best = 0.0
    for (x, z) in cols:
        dx, dz = x + 0.5 - cx, z + 0.5 - cz
        if cut and dx * cut[0] + dz * cut[1] > cut[2]:
            continue
        best = max(best, math.hypot(dx, dz))
    return int(math.ceil(best))


FIT = {}


def fit_of(b, wx, wz, flat):
    """Параметры площадки для gen_world: радиус, срез и доп. площадки (в мировых координатах)."""
    f = dict(flat=flat)
    bf = getattr(b, 'fit', None)
    if bf:
        if bf.get('cut'):
            f['cut'] = [round(v, 4) for v in bf['cut']]
        if bf.get('extra'):
            f['extra'] = [[wx + e[0], wz + e[1]] + list(e[2:]) for e in bf['extra']]
    return f


def put_poi(plan, build, name, kind, title, x, z, y, r, extra='', ground=None, tile_name=None):
    size = build.size[0]
    ox, oz = chunk_origin(x, z, size)
    cx, cz = ox + size // 2, oz + build.size[2] // 2
    g = build.G if ground is None else ground
    place(build, tile_name or name, ox, y - g, oz)
    r = max(r, footprint_r(build) + 2)
    POIS.append(dict(type=kind, id=name, name=title, x=cx, z=cz, y=y, r=r, extra=extra))
    if getattr(build, 'fit', None):
        FIT[name] = fit_of(build, cx, cz, r)
    plan.take(cx, cz, r)
    return cx, cz


def nearest_road(data, x, z):
    """Ближайшая точка трактов: (x, z, расстояние)."""
    best = (x, z, 1e9)
    for rd in data['roads']:
        if rd.get('style') == 'path':
            continue
        pts = rd['points']
        for i in range(len(pts) - 1):
            (ax, az), (bx, bz) = pts[i], pts[i + 1]
            dx, dz = bx - ax, bz - az
            L2 = dx * dx + dz * dz or 1
            u = max(0.0, min(1.0, ((x - ax) * dx + (z - az) * dz) / L2))
            px, pz = ax + dx * u, az + dz * u
            d = math.hypot(x - px, z - pz)
            if d < best[2]:
                best = (px, pz, d)
    return best


def shore_spot(plan, wx, wz, x, z):
    """Место для рыбацкого посёлка: идём от воды (wx, wz) к точке (x, z) и дальше, пока не выйдем на сушу;
    центр — в 12 блоках от кромки воды. Пробуем соседние направления."""
    a0 = math.atan2(z - wz, x - wx)
    for da in (0, 0.2, -0.2, 0.4, -0.4, 0.7, -0.7, 1.0, -1.0):
        a = a0 + da
        prev_w = None
        for t in range(40, 1400, 4):
            px, pz = wx + math.cos(a) * t, wz + math.sin(a) * t
            h, _, _, w = plan.world.sample(px, pz)
            if prev_w and not w and h >= 63.5:
                cx, cz = px + math.cos(a) * 12, pz + math.sin(a) * 12
                if all(math.hypot(cx - tx, cz - tz) > tr + 60 for tx, tz, tr in plan.taken):
                    return int(cx), int(cz)
                break
            prev_w = w
    return None


def road_angles(data, c):
    out = []
    for rd in data['roads']:
        if rd.get('style') == 'path':
            continue
        pts = rd['points']
        for i, (x, z) in enumerate(pts):
            if math.hypot(x - c['x'], z - c['z']) < 5:
                nb = pts[i - 1] if i > 0 else pts[i + 1]
                out.append(math.atan2(nb[1] - z, nb[0] - x))
    return out or [math.pi / 2]


def main():
    import mapbuild_cities as MC
    import mapbuild_pois as MP
    import mapbuild_towns2 as T2
    import mapbuild_towns3 as T3
    import mapbuild_villages as MV
    data = json.load(open(MAP_JSON, encoding='utf-8'))
    cities = {c['id']: c for c in data['cities']}
    # ---------------- города ----------------
    c = cities['arcadia']
    b = arcadia(c)
    place(b, 'arcadia', c['x'] - b.size[0] // 2, c['y'] - b.G, c['z'] - b.size[2] // 2)
    FIT['arcadia'] = fit_of(b, c['x'], c['z'], footprint_r(b) + 3)
    print('Аркадия:', len(b.blocks), 'blocks, площадка', FIT['arcadia']['flat'])
    builders = {
        'haven': T2.haven, 'sylvendar': T3.sylvendar, 'khazad': T3.khazad, 'ghardum': T3.ghardum, 'wolfden': T3.wolfden,
        'ashgrad': T3.ashgrad,
        'dawnfort': lambda c, g: T3.fort(c, g, 'dawnfort'),
        'northwatch': lambda c, g: T3.fort(c, g, 'northwatch'),
        'blackbastion': lambda c, g: T3.fort(c, g, 'blackbastion'),
    }
    for cid, fn in builders.items():
        c = cities[cid]
        gates = road_angles(data, c)
        b = fn(c, gates)
        S = b.size[0]
        place(b, cid, c['x'] - S // 2, c['y'] - b.G, c['z'] - S // 2)
        FIT[cid] = fit_of(b, c['x'], c['z'], footprint_r(b) + 3)
        print(f"{c['name']}: {len(b.blocks)} blocks, {len(b.entities)} entities, площадка {FIT[cid]['flat']}")
    plan = Planner(data)
    for c in data['cities']:
        POIS.append(dict(type='city', id=c['id'], name=c['name'], x=c['x'], z=c['z'], y=c['y'], r=0))

    # ---------------- хутора вокруг городов ----------------
    styles = {'arcadia': 'human', 'haven': 'human', 'sylvendar': 'elf', 'khazad': 'dwarf', 'ghardum': 'orc', 'wolfden': 'beast',
              'ashgrad': 'demon', 'dawnfort': 'human', 'northwatch': 'human', 'blackbastion': 'human'}
    fk = 0
    frnd = random.Random(77)
    for c in data['cities']:
        n = 5 if c['id'] == 'arcadia' else 3
        base_a = frnd.random() * 2 * math.pi
        for i in range(n):
            a = base_a + 2 * math.pi * i / n + frnd.uniform(-0.3, 0.3)
            dist = FIT.get(c['id'], {}).get('flat', c['flat']) + frnd.uniform(110, 170)
            spot = plan.find(c['x'] + math.cos(a) * dist, c['z'] + math.sin(a) * dist, 30, search=140)
            if not spot:
                continue
            b = MP.farmstead(fk, styles.get(c['id'], 'human'))
            put_poi(plan, b, f'farm_{fk}', 'farm', MP.FARM_NAMES[fk % len(MP.FARM_NAMES)], *spot, 30)
            fk += 1
    print('хуторов:', fk)

    # ---------------- чудеса света ----------------
    for name, title, (x, z), fn, r, terr in [
        ('world_tree', 'Древо Мира', (-3380, -660), MP.world_tree, 50, None),
        ('colossus', 'Колосс Аркадии', (330, -300), MP.colossus, 20, None),
        ('titan_bones', 'Кости Титана', (3350, 3250), MP.titan_bones, 48, None),
        ('abyss_gate', 'Врата Бездны', (-3550, 3480), MP.abyss_gate, 22, None)]:
        spot = plan.find(x, z, r, terr)
        if not spot:
            print('!! нет места для', title)
            continue
        b = fn()
        put_poi(plan, b, name, 'wonder', title, spot[0], spot[1], spot[2], r)
        print('чудо:', title, spot)
    # Небесная Кузня парит над горами дварфов
    b = MP.sky_forge()
    fx, fz = 3300, -3250
    fy = int(max(plan.world.sample(fx + dx, fz + dz)[0] for dx in (-20, 0, 20) for dz in (-20, 0, 20))) + 45
    ox, oz = chunk_origin(fx, fz, 48)
    place(b, 'sky_forge', ox, fy - b.G, oz)
    POIS.append(dict(type='wonder', id='sky_forge', name='Небесная Кузня', x=ox + 24, z=oz + 24, y=fy, r=0))
    print('чудо: Небесная Кузня', fy)

    # ---------------- подземелья ----------------
    DUN = [('ruins', 8, 'arcadia', ['kingdom', 'wild']), ('rotgrove', 11, 'sylvendar', ['elves', 'wild']),
           ('spider_nest', 14, 'wolfden', ['beastkin', 'elves', 'wild']), ('goblin_fort', 15, 'dawnfort', ['goblins', 'wild']),
           ('mire', 20, 'dawnfort', ['goblins']), ('labyrinth', 25, 'ghardum', ['orcs', 'wild']),
           ('crypt', 30, 'blackbastion', ['deadlands', 'wild']), ('frost_citadel', 38, 'northwatch', ['frost']),
           ('crucible', 45, 'ashgrad', ['demonkin', 'orcs']), ('ash_citadel', 55, 'ashgrad', ['demonkin', 'wild'])]
    import gen_dungeons as GD
    titles = {'ruins': 'Руины Стражей', 'rotgrove': 'Гниющая Роща', 'spider_nest': 'Паучье Логово', 'goblin_fort': 'Гоблинская Крепость',
              'mire': 'Проклятая Топь', 'labyrinth': 'Лабиринт Астериона', 'crypt': 'Склеп Лича', 'frost_citadel': 'Ледяная Цитадель',
              'crucible': 'Пламенное Горнило', 'ash_citadel': 'Цитадель Пепла'}
    for key, lvl, anchor, terr in DUN:
        a = cities[anchor]
        spot = plan.find_level(a['x'], a['z'], lvl, 24, terr)
        if not spot:
            spot = plan.find_level(a['x'], a['z'], lvl, 24, None)
        if not spot:
            spot = plan.find_level(a['x'], a['z'], lvl, 24, terr, tol=2)
        if not spot:
            print('!! нет места для подземелья', key)
            continue
        x, z, y = spot
        ox, oz = chunk_origin(x, z, 41)
        TILES.append(dict(dim='overworld', cx=ox // 16, cz=oz // 16, template='litrpg:dungeon_' + key, y=y - GD.G))
        POIS.append(dict(type='dungeon', id='dungeon_' + key, name=titles[key], x=ox + 20, z=oz + 20, y=y, r=24, extra=key))
        ANCHORS.setdefault('world', {})['dungeon_' + key] = [ox + 20, y, oz + 20]
        plan.take(ox + 20, oz + 20, 24)
        print(f'подземелье {titles[key]} ур.{lvl}: ({ox + 20}, {oz + 20}) y={y}, уровень места {plan.world.level(ox + 20, oz + 20)}')

    # ---------------- деревни и лагеря ----------------
    rnd = random.Random(42)
    vill = [(520, 260), (-620, -420), (-1000, 480), (330, -880), (980, -360), (-420, 1060), (-260, -1100), (1150, -980),
            (-1650, -320), (1100, 1350), (-380, 2150), (250, -2650)]
    spurs = []
    for i, (x, z) in enumerate(vill):
        spot = plan.find(x, z, 56)
        if not spot:
            print('!! нет места для деревни', i)
            continue
        rx, rz, rd_ = nearest_road(data, spot[0], spot[1])
        road_a = math.atan2(rz - spot[1], rx - spot[0])
        b = MV.village(i, 'human', road_a)
        cx, cz = put_poi(plan, b, f'village_{i}', 'village', 'Деревня ' + MP.VILLAGE_NAMES[i % len(MP.VILLAGE_NAMES)], *spot, 50)
        if rd_ < 420:
            ex, ez = cx + math.cos(road_a) * (b.size[0] // 2 - 2), cz + math.sin(road_a) * (b.size[0] // 2 - 2)
            spurs.append(dict(name='Тропа', style='path', points=[[round(ex), round(ez)], [round(rx), round(rz)]]))
    for i, (x, z, terr) in enumerate([(-1650, -760, None), (-1950, -2150, None), (900, -1550, None), (-760, -1560, None),
                                      (2050, -1350, None), (-2300, 900, None)]):
        spot = plan.find(x, z, 22)
        if spot:
            put_poi(plan, MV.lumber_camp(i), f'lumber_{i}', 'lumber', 'Лесопилка', *spot, 30)
    # рыбацкие посёлки: по берегам залива и озёр
    shores = []
    bx, bz, br, _ = data['seas'][0]
    for a in (200, 250, 300, 340):
        ar = math.radians(a)
        shores.append((bx + math.cos(ar) * (br + 36), bz + math.sin(ar) * (br + 36), math.atan2(-math.sin(ar), -math.cos(ar))))
    for lx, lz, lr, _ in data['lakes'][:4]:
        shores.append((lx + lr + 32, lz, math.pi))
    for i, (x, z, sea_a) in enumerate(shores):
        wx_, wz_ = (bx, bz) if i < 4 else data['lakes'][i - 4][:2]
        spot = shore_spot(plan, wx_, wz_, x, z)
        if spot:
            sa = math.atan2(wz_ - spot[1], wx_ - spot[0])
            put_poi(plan, MV.fishing_village(i, sa), f'fishing_{i}', 'fishing', 'Рыбацкий посёлок', spot[0], spot[1], 66, 30)
        else:
            print('!! нет берега для рыбацкого посёлка', i)
    # стоянки караванов вдоль трактов
    traders = ['litrpg:dwarf_smith', 'litrpg:elf_trader', 'litrpg:orc_trader', 'litrpg:beast_trader', 'litrpg:demon_trader']
    k = 0
    for rd in data['roads']:
        if rd.get('style') == 'path':
            continue
        pts = rd['points']
        segs = [(pts[i], pts[i + 1]) for i in range(len(pts) - 1)]
        (ax, az), (bx2, bz2) = segs[len(segs) // 2]
        mx, mz = (ax + bx2) / 2, (az + bz2) / 2
        nx, nz = -(bz2 - az), (bx2 - ax)
        ln = math.hypot(nx, nz) or 1
        x, z = mx + nx / ln * 34, mz + nz / ln * 34
        spot = plan.find(x, z, 18, search=100)
        if spot:
            put_poi(plan, MP.caravan_camp(k, traders[k % len(traders)]), f'caravan_{k}', 'caravan', 'Стоянка караванщиков', *spot, 18)
            k += 1
    # придорожные хутора: постоялые дворы, мельницы, кузни, часовни — у самых трактов
    kinds = ['inn', 'mill', 'smithy', 'inn', 'shrine', 'mill', 'inn', 'smithy', 'shrine']
    used = {k: 0 for k in MV.HAMLET_NAMES}
    hk = 0
    for rd in data['roads']:
        if rd.get('style') == 'path':
            continue
        pts = rd['points']
        segs = []
        acc = 0.0
        for i in range(len(pts) - 1):
            L = math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1])
            segs.append((acc, L, pts[i], pts[i + 1]))
            acc += L
        s_ = 300.0
        side = 1
        while s_ < acc - 300:
            done = False
            for shift in (0, 70, -70, 140, -140):
                pos = s_ + shift
                for (a0, L, p, q) in segs:
                    if a0 <= pos <= a0 + L:
                        u = (pos - a0) / L
                        mx, mz = p[0] + (q[0] - p[0]) * u, p[1] + (q[1] - p[1]) * u
                        tx, tz = (q[0] - p[0]) / L, (q[1] - p[1]) / L
                        break
                else:
                    continue
                if any(math.hypot(mx - c['x'], mz - c['z']) < c['r'] + 140 for c in data['cities']):
                    continue
                for sd in (side, -side):
                    cx, cz = mx - tz * sd * 34, mz + tx * sd * 34
                    _, _, dr = nearest_road(data, cx, cz)
                    if dr < 33:
                        continue
                    y = plan.ok(cx, cz, 34)
                    if y is None:
                        continue
                    kind = kinds[hk % len(kinds)]
                    names = MV.HAMLET_NAMES[kind]
                    title = names[used[kind] % len(names)]
                    used[kind] += 1
                    road_a = math.atan2(-tx * sd, tz * sd)
                    b = MV.hamlet(hk, kind, road_a)
                    put_poi(plan, b, f'hamlet_{hk}', 'hamlet', title, int(cx), int(cz), y, 30)
                    hk += 1
                    side = -side
                    done = True
                    break
                if done:
                    break
            s_ += 560 if done else 200
    print('придорожных хуторов:', hk)
    data['spurs'] = spurs

    # сторожевые башни
    for i, (x, z) in enumerate([(-1380, -100), (180, -560), (-380, 1460), (1560, 1720), (-1880, 2520), (2200, 360), (130, -1600), (-1760, -1660)]):
        spot = plan.find(x + 40, z + 40, 14, search=160)
        if spot:
            put_poi(plan, MP.watchtower(i), f'tower_{i}', 'outpost', 'Сторожевая башня', *spot, 14)
    # руины в диких землях
    for i in range(16):
        a = rnd.random() * 2 * math.pi
        d = rnd.uniform(1500, 3800)
        spot = plan.find(math.cos(a) * d, math.sin(a) * d, 14, search=200)
        if spot:
            put_poi(plan, MP.ruins(i), f'ruins_{i}', 'ruins', 'Древние руины', *spot, 14)

    # ---------------- пиратские корабли в открытом море ----------------
    import mapbuild_pirates as MPI
    sx, sz, sr, _ = data['seas'][0]
    hv = cities['haven']
    ships = []
    k = 0
    for deg in (330, 20, 75, 125, 175, 285):
        if len(ships) >= 4:
            break
        a = math.radians(deg)
        for rr in (sr * 0.55, sr * 0.45, sr * 0.65):
            px, pz = sx + math.cos(a) * rr, sz + math.sin(a) * rr
            if math.hypot(px - hv['x'], pz - hv['z']) < 380:
                continue
            deep = all(plan.world.sample(px + dx, pz + dz)[3] != 0 and plan.world.sample(px + dx, pz + dz)[0] < 55
                       for dx in (-24, 0, 24) for dz in (-24, 0, 24))
            if not deep:
                continue
            b = MPI.pirate_ship(k)
            ox, oz = chunk_origin(px, pz, b.size[0])
            place(b, f'pirates_{k}', ox, 63 - b.wl, oz)
            title = ['«Чёрная Вдова»', '«Морской Змей»', '«Кровавая Мэри»', '«Ржавый Якорь»'][k % 4]
            POIS.append(dict(type='pirates', id=f'pirates_{k}', name='Пиратский корабль ' + title, x=ox + b.size[0] // 2,
                             z=oz + b.size[2] // 2, y=63, r=0))
            ships.append((ox + b.size[0] // 2, oz + b.size[2] // 2))
            print('пиратский корабль', title, ships[-1])
            k += 1
            break
    if ships:
        bx_, bz_ = min(ships, key=lambda p: math.hypot(p[0] - hv['x'], p[1] - hv['z']))
        ANCHORS.setdefault('haven', {})['pirate_ship'] = [bx_, 66, bz_]

    # ---------------- лагерь браконьеров у Стойбища (сюжет «Белый волк») ----------------
    import mapbuild_secrets as SEC
    wd = cities['wolfden']
    for deg in range(0, 360, 30):
        a = math.radians(deg + 15)
        dist = FIT.get('wolfden', {}).get('flat', wd['flat']) + 90
        spot = plan.find(wd['x'] + math.cos(a) * dist, wd['z'] + math.sin(a) * dist, 14, search=60)
        if spot:
            cb = City(32, 24, 32, 'poachers', 'beast', ground=4)
            SEC.poacher_camp(cb, 16, 16, 4)
            cb.anchor_city = 'wolfden'
            put_poi(plan, cb, 'poachers', 'camp', 'Лагерь браконьеров', *spot, 14)
            print('лагерь браконьеров', spot)
            break

    # ---------------- Преисподняя (координаты ада) ----------------
    nether_flats = []
    b = MP.infernalis()
    nx, nz, ny = -352, 368, 40
    place(b, 'infernalis', nx - 64, ny - b.G, nz - 64, dim='nether')
    nether_flats.append([nx, nz, 66, ny])
    POIS.append(dict(type='nether_city', id='infernalis', name='Инферналис', x=nx, z=nz, y=ny, r=0))
    b = MP.ash_throne()
    tx, tz = 40, 456
    place(b, 'ash_throne', tx - 24, ny - b.G, tz - 24, dim='nether')
    nether_flats.append([tx, tz, 30, ny])
    POIS.append(dict(type='nether_wonder', id='ash_throne', name='Трон Пепла', x=tx, z=tz, y=ny, r=0))
    hx, hz = 324, -220
    ox, oz = chunk_origin(hx, hz, 41)
    TILES.append(dict(dim='nether', cx=ox // 16, cz=oz // 16, template='litrpg:dungeon_nether_heart', y=ny - GD.G))
    nether_flats.append([ox + 20, oz + 20, 30, ny])
    POIS.append(dict(type='nether_dungeon', id='dungeon_nether_heart', name='Сердце Преисподней', x=ox + 20, z=oz + 20, y=ny, r=0,
                     extra='nether_heart'))
    data['nether'] = dict(flats=nether_flats)

    data['pois'] = POIS
    data['fit'] = FIT
    data['tiles'] = TILES
    data['workshops'] = WORKSHOPS
    data['anchors'] = ANCHORS
    for cid_, m in ANCHORS.items():
        print('сюжетные места', cid_, ', '.join(sorted(m)))
    print('мастерских:', len(WORKSHOPS))
    json.dump(data, open(MAP_JSON, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
    from collections import Counter
    print(Counter(p['type'] for p in POIS), f'{len(TILES)} tiles')


if __name__ == '__main__':
    main()
