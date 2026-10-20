"""
Мастерские городов: здания под каждое ремесло (кузница, ювелирная лавка, таверна,
лечебница, торговая палата…), их обстановка, вывески, задние дворы с загонами
и огородами, прилавки рынка. Каждая мастерская записывается в b.workshops —
gen_mapbuild.py переносит их в litrpg_world.json (для поручений и найма мастеров).
"""
import math
import random

from mapbuild_lib import DIRS, OPP, TByte, TList

RACE_NPC = {'human': 'litrpg:human_artisan', 'elf': 'litrpg:elf_artisan', 'dwarf': 'litrpg:dwarf_artisan',
            'orc': 'litrpg:orc_artisan', 'beast': 'litrpg:beast_artisan', 'demon': 'litrpg:demon_artisan'}

TITLES = {
    'blacksmith': 'Кузница', 'armorer': 'Бронная', 'weaponsmith': 'Оружейная', 'jeweler': 'Ювелир', 'tailor': 'Портной',
    'herbalist': 'Травы', 'alchemist': 'Алхимия', 'baker': 'Пекарня', 'butcher': 'Мясная лавка', 'fishmonger': 'Рыба',
    'innkeeper': 'Таверна', 'builder': 'Строители', 'leatherworker': 'Кожевник', 'fletcher': 'Луки и стрелы',
    'scribe': 'Книги и карты', 'farmer': 'Ферма', 'healer': 'Лечебница', 'banker': 'Торговая палата',
    'stablemaster': 'Конюшня', 'miner': 'Руда и уголь',
}

# обстановка первого этажа: по кругу вдоль стен
INSIDE = {
    'blacksmith': ['anvil', 'blast_furnace', 'smithing_table', 'lava_cauldron', 'grindstone', 'barrel', 'anvil', 'chest'],
    'armorer': ['blast_furnace', 'armor_stand', 'anvil', 'smithing_table', 'armor_stand', 'barrel'],
    'weaponsmith': ['grindstone', 'armor_stand', 'anvil', 'weapon_rack', 'smithing_table', 'grindstone'],
    'jeweler': ['display_amethyst', 'display_gold', 'lectern', 'display_emerald', 'chest', 'display_amethyst'],
    'tailor': ['loom', 'wool_red', 'loom', 'wool_blue', 'barrel', 'wool_white', 'cartography_table'],
    'herbalist': ['pot', 'composter', 'brewing_stand', 'pot', 'barrel', 'pot', 'flower_box'],
    'alchemist': ['brewing_stand', 'water_cauldron', 'bookshelf', 'brewing_stand', 'candle', 'chest', 'brewing_stand'],
    'baker': ['smoker', 'furnace', 'hay', 'table_cake', 'barrel', 'smoker', 'crafting_table'],
    'butcher': ['smoker', 'barrel', 'hay', 'chest', 'water_cauldron', 'smoker'],
    'fishmonger': ['barrel', 'ice_box', 'water_cauldron', 'barrel', 'ice_box', 'chest'],
    'innkeeper': ['barrel', 'barrel', 'smoker', 'table', 'table', 'barrel', 'table'],
    'builder': ['crafting_table', 'stonecutter', 'log_pile', 'scaffolding', 'chest', 'stone_pile'],
    'leatherworker': ['water_cauldron', 'barrel', 'crafting_table', 'hay', 'water_cauldron', 'chest'],
    'fletcher': ['fletching_table', 'target', 'barrel', 'crafting_table', 'fletching_table'],
    'scribe': ['lectern', 'bookshelf', 'bookshelf', 'cartography_table', 'bookshelf', 'chest', 'lectern'],
    'farmer': ['composter', 'barrel', 'hay', 'crafting_table', 'hay', 'chest'],
    'healer': ['bed_white', 'brewing_stand', 'water_cauldron', 'pot', 'bed_white', 'chest'],
    'banker': ['display_gold', 'lectern', 'chest', 'display_gold', 'barrel', 'lectern', 'display_emerald'],
    'stablemaster': ['hay', 'barrel', 'hay', 'chest', 'hay'],
    'miner': ['blast_furnace', 'ore_iron', 'ore_coal', 'grindstone', 'ore_copper', 'chest'],
}

SIZES = {'innkeeper': (14, 10, 2), 'banker': (14, 11, 2), 'healer': (12, 9, 1), 'builder': (10, 8, 1)}


def _put(b, kind, x, y, z, inward):
    """Поставить предмет обстановки kind в (x,y,z); inward — сторона к центру комнаты."""
    rnd = b.rnd
    if kind in ('anvil',):
        b.set(x, y, z, 'minecraft:anvil', facing='north' if inward in ('east', 'west') else 'east')
    elif kind in ('blast_furnace', 'furnace', 'smoker'):
        b.set(x, y, z, 'minecraft:' + kind, facing=inward, lit='true' if rnd.random() < 0.5 else 'false')
    elif kind == 'grindstone':
        b.set(x, y, z, 'minecraft:grindstone', face='floor', facing=inward)
    elif kind == 'barrel':
        b.set(x, y, z, 'minecraft:barrel', facing='up', open='false')
    elif kind == 'chest':
        b.set(x, y, z, 'minecraft:chest', facing=inward, type='single', waterlogged='false')
    elif kind == 'lava_cauldron':
        b.set(x, y, z, 'minecraft:lava_cauldron')
    elif kind == 'water_cauldron':
        b.set(x, y, z, 'minecraft:water_cauldron', level='3')
    elif kind == 'brewing_stand':
        b.set(x, y, z, 'minecraft:brewing_stand', has_bottle_0='true', has_bottle_1='false', has_bottle_2='true')
    elif kind == 'lectern':
        b.set(x, y, z, 'minecraft:lectern', facing=inward, has_book='false', powered='false')
    elif kind == 'loom':
        b.set(x, y, z, 'minecraft:loom', facing=inward)
    elif kind == 'stonecutter':
        b.set(x, y, z, 'minecraft:stonecutter', facing=inward)
    elif kind == 'composter':
        b.set(x, y, z, 'minecraft:composter', level=str(rnd.randint(2, 7)))
    elif kind == 'hay':
        b.set(x, y, z, 'minecraft:hay_block', axis='y')
        if rnd.random() < 0.5:
            b.set(x, y + 1, z, 'minecraft:hay_block', axis='x')
    elif kind == 'pot':
        b.set(x, y, z, 'minecraft:spruce_slab', type='top', waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:potted_' + rnd.choice(['fern', 'red_tulip', 'blue_orchid', 'allium', 'azure_bluet', 'cactus',
                                                             'red_mushroom', 'brown_mushroom', 'oxeye_daisy', 'lily_of_the_valley']))
    elif kind == 'flower_box':
        b.set(x, y, z, 'minecraft:moss_block')
        b.set(x, y + 1, z, 'minecraft:' + rnd.choice(['poppy', 'dandelion', 'cornflower', 'allium']))
    elif kind == 'candle':
        b.set(x, y, z, 'minecraft:dark_oak_slab', type='top', waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:purple_candle', candles='3', lit='true', waterlogged='false')
    elif kind == 'display_amethyst':
        b.set(x, y, z, 'minecraft:smooth_stone_slab', type='top', waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:amethyst_cluster', facing='up', waterlogged='false')
    elif kind in ('display_gold', 'display_emerald'):
        b.set(x, y, z, 'minecraft:gold_block' if kind == 'display_gold' else 'minecraft:emerald_block')
        b.set(x, y + 1, z, 'minecraft:glass')
    elif kind.startswith('wool_'):
        b.set(x, y, z, 'minecraft:' + kind.split('_')[1] + '_wool')
        b.set(x, y + 1, z, 'minecraft:' + rnd.choice(['white', 'yellow', 'light_blue']) + '_carpet')
    elif kind == 'weapon_rack':
        b.set(x, y, z, 'minecraft:spruce_fence', waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:tripwire_hook', facing=inward, attached='false', powered='false')
    elif kind == 'armor_stand':
        b.entities.append((x, y, z, 'minecraft:armor_stand', {'ShowArms': TByte(1)}))
    elif kind == 'table':
        b.set(x, y, z, b.st['fence'], waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:spruce_pressure_plate', powered='false')
    elif kind == 'table_cake':
        b.set(x, y, z, b.st['fence'], waterlogged='false')
        b.set(x, y + 1, z, 'minecraft:cake', bites='0')
    elif kind == 'ice_box':
        b.set(x, y, z, 'minecraft:packed_ice')
    elif kind == 'log_pile':
        b.set(x, y, z, 'minecraft:spruce_log', axis='x')
        b.set(x, y + 1, z, 'minecraft:spruce_log', axis='x')
    elif kind == 'stone_pile':
        b.set(x, y, z, 'minecraft:stone_bricks')
        b.set(x, y + 1, z, 'minecraft:stone_brick_slab', type='bottom', waterlogged='false')
    elif kind == 'scaffolding':
        for i in range(3):
            b.set(x, y + i, z, 'minecraft:scaffolding', bottom='false', distance='0', waterlogged='false')
    elif kind == 'bed_white':
        dx, dz = DIRS[inward]
        if b.blocks.get((x + dx, y, z + dz), ('minecraft:air',))[0] == 'minecraft:air':
            b.bed(x + dx, y, z + dz, OPP[inward], 'white')
    elif kind.startswith('ore_'):
        blk = {'ore_iron': 'minecraft:raw_iron_block', 'ore_coal': 'minecraft:coal_block', 'ore_copper': 'minecraft:raw_copper_block'}[kind]
        b.set(x, y, z, blk)
    else:
        b.set(x, y, z, 'minecraft:' + kind)


def _sign(b, x, y, z, facing, text):
    wood = {'human': 'spruce', 'elf': 'birch', 'dwarf': 'dark_oak', 'orc': 'spruce', 'beast': 'spruce', 'demon': 'crimson'}[b.style]
    b.set(x, y, z, f'minecraft:{wood}_wall_sign', facing=facing, waterlogged='false')
    words = text.split(' ')
    lines = ['', text, '', ''] if len(text) <= 14 else ['', ' '.join(words[:len(words) // 2]), ' '.join(words[len(words) // 2:]), '']
    msg = TList(8, ['{"text":"%s"}' % ln for ln in lines])
    empty = TList(8, ['{"text":""}'] * 4)
    b.nbt[(x, y, z)] = {'id': f'minecraft:sign', 'is_waxed': TByte(1),
                        'front_text': {'messages': msg, 'color': 'black', 'has_glowing_text': TByte(1 if b.style == 'demon' else 0)},
                        'back_text': {'messages': empty, 'color': 'black', 'has_glowing_text': TByte(0)}}


def workshop(b, x0, z0, craft, door, floors=None, roof=None):
    """Мастерская ремесла craft с левым-верхним углом (x0, z0). Возвращает False, если не влезла."""
    w, d, fl = SIZES.get(craft, (b.rnd.randint(9, 11), b.rnd.randint(8, 9), b.rnd.choice((1, 2))))
    if floors:
        fl = floors
    x1, z1 = x0 + w - 1, z0 + d - 1
    if not b.free(x0 - 1, z0 - 1, x1 + 1, z1 + 1):
        return False
    G = b.G
    b.house(x0, z0, w, d, floors=fl, door=door, roof=roof, interior='workshop', npc=None)
    dx_, dz_, _ = b.last_door
    ox, oz = DIRS[door]
    # вывеска над дверью
    _sign(b, dx_ + ox, G + 2, dz_ + oz, door, TITLES[craft])
    # обстановка вдоль стен первого этажа
    inner = []
    for x in range(x0 + 1, x1):
        inner.append((x, z0 + 1, 'south'))
        inner.append((x, z1 - 1, 'north'))
    for z in range(z0 + 2, z1 - 1):
        inner.append((x0 + 1, z, 'east'))
        inner.append((x1 - 1, z, 'west'))
    ix, iz = dx_ - ox, dz_ - oz  # клетка у двери изнутри
    items = INSIDE[craft]
    k = 0
    for (x, z, inward) in inner:
        if abs(x - ix) <= 1 and abs(z - iz) <= 1:
            continue
        if fl > 1 and x == x0 + 1 and z == z0 + 1:
            continue  # лестница
        if (x + z) % 2 == 1 and len(inner) > 14:
            continue  # проходы между станками
        if b.blocks.get((x, G, z), ('minecraft:air',))[0] != 'minecraft:air':
            continue
        _put(b, items[k % len(items)], x, G, z, inward)
        k += 1
    # прилавок: полоса плит напротив двери, мастер за ним
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    if craft in ('innkeeper', 'banker', 'jeweler', 'baker', 'butcher', 'fishmonger', 'herbalist', 'alchemist', 'scribe', 'tailor'):
        for t in range(-1, 2):
            px, pz = (cx + t, cz + oz * -1) if door in ('north', 'south') else (cx + ox * -1, cz + t)
            if b.blocks.get((px, G, pz), ('minecraft:air',))[0] == 'minecraft:air':
                b.set(px, G, pz, b.st['plank'].replace('planks', 'slab') if 'planks' in b.st['plank'] else 'minecraft:spruce_slab',
                      type='top', waterlogged='false')
    nx, nz = cx - ox * 2, cz - oz * 2
    if b.blocks.get((nx, G, nz), ('minecraft:air',))[0] != 'minecraft:air':
        nx, nz = cx, cz
    npc = RACE_NPC[b.style]
    b.entities.append((nx, G, nz, npc, {'Craft': craft}))
    if craft == 'innkeeper':
        # посетители таверны
        b.entities.append((cx + 1, G, cz + 1, npc.replace('artisan', 'citizen'), {}))
    b.workshops.append((craft, npc, nx, G, nz))
    yard(b, x0, z0, x1, z1, door, craft)
    return True


def yard(b, x0, z0, x1, z1, door, craft):
    """Задний двор мастерской (со стороны, противоположной двери)."""
    G = b.G
    back = OPP[door]
    bx, bz = DIRS[back]
    depth = 6
    if back == 'north':
        ya = (x0, z0 - depth - 1, x1, z0 - 2)
    elif back == 'south':
        ya = (x0, z1 + 2, x1, z1 + depth + 1)
    elif back == 'west':
        ya = (x0 - depth - 1, z0, x0 - 2, z1)
    else:
        ya = (x1 + 2, z0, x1 + depth + 1, z1)
    ax0, az0, ax1, az1 = ya
    if not b.free(ax0, az0, ax1, az1):
        return
    kind = {'innkeeper': 'pigs', 'butcher': 'pigs', 'farmer': 'farm', 'stablemaster': 'horses', 'blacksmith': 'forge',
            'weaponsmith': 'forge', 'armorer': 'forge', 'builder': 'materials', 'herbalist': 'garden', 'healer': 'garden',
            'fletcher': 'range', 'tailor': 'sheep', 'miner': 'ore', 'fishmonger': 'nets', 'leatherworker': 'tannery',
            'alchemist': 'garden', 'baker': 'chickens'}.get(craft)
    if not kind:
        return
    b.reserve(ax0 - 1, az0 - 1, ax1 + 1, az1 + 1)
    rnd = b.rnd
    for x in range(ax0, ax1 + 1):
        for z in range(az0, az1 + 1):
            b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false') if kind in ('garden', 'farm', 'pigs', 'horses', 'sheep', 'chickens') \
                else b.set(x, G - 1, z, rnd.choice(b.st['path']))
    animals = {'pigs': ('minecraft:pig', 3), 'horses': ('minecraft:horse', 2), 'sheep': ('minecraft:sheep', 3),
               'chickens': ('minecraft:chicken', 4), 'farm': ('minecraft:cow', 2)}
    if kind in animals:
        # загон с животными
        for x in range(ax0, ax1 + 1):
            for z in range(az0, az1 + 1):
                if x in (ax0, ax1) or z in (az0, az1):
                    b.set(x, G, z, b.st['fence'] if b.st['fence'].endswith('fence') else 'minecraft:spruce_fence', waterlogged='false')
        mob, n = animals[kind]
        for i in range(n):
            b.entities.append((rnd.randint(ax0 + 1, ax1 - 1), G, rnd.randint(az0 + 1, az1 - 1), mob, {}))
        # кормушка — низкая (плиты и сено ниже забора), чтобы животные не перепрыгнули ограду
        b.set(ax0 + 1, G - 1, az0 + 1, 'minecraft:hay_block', axis='y')
        b.set(ax1 - 1, G - 1, az1 - 1, 'minecraft:water', level='0')
    elif kind == 'garden':
        for x in range(ax0, ax1 + 1):
            for z in range(az0, az1 + 1):
                if (x + z) % 3 == 0:
                    b.set(x, G, z, 'minecraft:' + rnd.choice(['poppy', 'dandelion', 'cornflower', 'allium', 'azure_bluet', 'oxeye_daisy',
                                                              'lily_of_the_valley', 'fern']))
                elif (x * 7 + z) % 11 == 0:
                    b.set(x, G, z, 'minecraft:sweet_berry_bush', age='3')
        b.set(ax0, G, az0, 'minecraft:bee_nest', facing='south', honey_level='3')
    elif kind == 'forge':
        b.set(ax0 + 1, G, az0 + 1, 'minecraft:anvil', facing='north')
        b.set(ax0 + 2, G, az0 + 1, 'minecraft:campfire', lit='true', signal_fire='false', facing='north', waterlogged='false')
        b.set(ax0 + 3, G, az0 + 1, 'minecraft:water_cauldron', level='3')
        for x in range(ax0 + 1, ax0 + 4):
            b.set(x, G, az1 - 1, 'minecraft:iron_bars', waterlogged='false')
        b.set(ax1 - 1, G, az0 + 1, 'minecraft:coal_block')
    elif kind == 'materials':
        for x in range(ax0, ax1 + 1, 2):
            b.set(x, G, az0 + 1, 'minecraft:spruce_log', axis='x')
            b.set(x, G + 1, az0 + 1, 'minecraft:spruce_log', axis='x') if rnd.random() < 0.6 else None
            b.set(x, G, az1 - 1, 'minecraft:stone_bricks')
        for i in range(4):
            b.set(ax1 - 1, G + i, (az0 + az1) // 2, 'minecraft:scaffolding', bottom='false', distance='0', waterlogged='false')
        b.set(ax0 + 1, G, (az0 + az1) // 2, 'minecraft:crafting_table')
    elif kind == 'range':
        for z in range(az0, az1 + 1, 2):
            b.set(ax1, G, z, 'minecraft:hay_block', axis='y')
            b.set(ax1, G + 1, z, 'minecraft:target', power='0')
    elif kind == 'ore':
        for i, blk in enumerate(['minecraft:raw_iron_block', 'minecraft:coal_block', 'minecraft:raw_copper_block', 'minecraft:cobblestone']):
            b.set(ax0 + 1 + i, G, az0 + 1, blk)
        for x in range(ax0, ax1 + 1):
            b.set(x, G, az1 - 1, 'minecraft:rail', shape='east_west', waterlogged='false')
        b.entities.append((ax0 + 2, G, az1 - 1, 'minecraft:minecart', {}))
    elif kind == 'nets':
        for x in range(ax0, ax1 + 1, 3):
            for y in range(G, G + 3):
                b.set(x, y, az0 + 1, b.st['fence'], waterlogged='false')
            b.set(x, G + 1, az0 + 2, 'minecraft:barrel', facing='up', open='false')
        b.set(ax1 - 1, G, az1 - 1, 'minecraft:water_cauldron', level='3')
    elif kind == 'tannery':
        for x in range(ax0 + 1, ax1, 2):
            b.set(x, G, az0 + 1, 'minecraft:water_cauldron', level='3')
        for x in range(ax0, ax1 + 1, 3):
            for y in range(G, G + 2):
                b.set(x, y, az1 - 1, b.st['fence'], waterlogged='false')
            b.set(x, G + 2, az1 - 1, 'minecraft:brown_wool')


def place_workshops(b, C, rmin, rmax, crafts, seed, door_to_center=True, avoid_center=0):
    """Расставить мастерские по кольцу rmin..rmax вокруг центра C. Возвращает список поставленных."""
    rnd = random.Random(str(seed) + 'ws')
    done = []
    for craft in crafts:
        w, d, _ = SIZES.get(craft, (11, 9, 2))
        for attempt in range(220):
            a = rnd.random() * 2 * math.pi
            r = rnd.uniform(rmin, rmax)
            cx, cz = C + math.cos(a) * r, C + math.sin(a) * r
            x0, z0 = int(cx - w / 2), int(cz - d / 2)
            if not b.free(x0 - 2, z0 - 2, x0 + w + 1, z0 + d + 1):
                continue
            dx, dz = C - cx, C - cz
            door = ('east' if dx > 0 else 'west') if abs(dx) > abs(dz) else ('south' if dz > 0 else 'north')
            if workshop(b, x0, z0, craft, door):
                done.append(craft)
                break
    return done


def craft_stall(b, x, z, craft, color, facing):
    """Прилавок рынка с мастером-торговцем (пекарь, мясник, рыботорговец, фермер, травница)."""
    G = b.G
    for dx in range(-2, 3):
        for dz in range(-1, 2):
            if abs(dx) == 2 and abs(dz) == 1:
                for y in range(G, G + 3):
                    b.set(x + dx, y, z + dz, b.st['fence'], waterlogged='false')
            b.set(x + dx, G + 3, z + dz, f'minecraft:{color}_wool')
    front = 1 if facing == 'south' else -1
    goods = {'baker': ('minecraft:hay_block', 'minecraft:cake'), 'butcher': ('minecraft:smoker', 'minecraft:hay_block'),
             'fishmonger': ('minecraft:barrel', 'minecraft:packed_ice'), 'farmer': ('minecraft:hay_block', 'minecraft:pumpkin'),
             'herbalist': ('minecraft:composter', 'minecraft:moss_block'), 'tailor': ('minecraft:loom', 'minecraft:white_wool'),
             'jeweler': ('minecraft:amethyst_block', 'minecraft:gold_block')}.get(craft, ('minecraft:barrel', 'minecraft:barrel'))
    for dx in range(-1, 2):
        b.slab(x + dx, G, z + front, 'minecraft:spruce_slab', 'top')
    a, c = goods
    b.set(x - 1, G, z, a, **({'facing': facing, 'lit': 'false'} if a.endswith('smoker') else {'facing': 'up', 'open': 'false'} if a.endswith('barrel')
                             else {'facing': facing} if a.endswith('loom') else {'axis': 'y'} if a.endswith('hay_block') else {}))
    b.set(x + 1, G, z, c, **({'axis': 'y'} if c.endswith('hay_block') else {'bites': '0'} if c.endswith('cake') else {}))
    npc = RACE_NPC[b.style]
    b.entities.append((x, G, z, npc, {'Craft': craft}))
    b.workshops.append((craft, npc, x, G, z))
    b.reserve(x - 3, z - 2, x + 3, z + 2)


# какие мастерские у какого города
CITY_CRAFTS = {
    'arcadia': ['banker', 'innkeeper', 'blacksmith', 'armorer', 'weaponsmith', 'jeweler', 'tailor', 'herbalist', 'alchemist',
                'healer', 'scribe', 'builder', 'builder', 'leatherworker', 'fletcher', 'stablemaster', 'miner', 'innkeeper', 'baker'],
    'haven': ['innkeeper', 'fishmonger', 'blacksmith', 'tailor', 'herbalist', 'scribe', 'banker', 'builder', 'healer', 'baker',
              'leatherworker', 'stablemaster'],
    'fort': ['blacksmith', 'armorer', 'weaponsmith', 'innkeeper', 'healer', 'builder', 'fletcher', 'stablemaster', 'baker', 'herbalist'],
    'sylvendar': ['herbalist', 'alchemist', 'tailor', 'fletcher', 'jeweler', 'scribe', 'healer', 'innkeeper', 'builder', 'baker',
                  'leatherworker'],
    'khazad': ['blacksmith', 'armorer', 'weaponsmith', 'jeweler', 'miner', 'builder', 'innkeeper', 'banker', 'alchemist', 'baker',
               'butcher', 'leatherworker', 'builder'],
    'ghardum': ['butcher', 'leatherworker', 'weaponsmith', 'blacksmith', 'innkeeper', 'herbalist', 'builder', 'fletcher', 'armorer'],
    'wolfden': ['leatherworker', 'fletcher', 'herbalist', 'butcher', 'healer', 'builder', 'tailor', 'innkeeper'],
    'ashgrad': ['alchemist', 'jeweler', 'blacksmith', 'weaponsmith', 'scribe', 'banker', 'healer', 'builder', 'innkeeper', 'tailor',
                'armorer'],
}


def fill_life(b, C, R, seed, citizen=None, density=1.0):
    """Мелочи городской жизни в свободных местах: колодцы, огороды, телеги, скамейки, клумбы, стога, кошки и куры."""
    rnd = random.Random(str(seed) + 'life')
    G = b.G
    st = b.st
    kinds = ['garden', 'bench', 'flowers', 'hay', 'tree', 'well', 'cart', 'garden', 'flowers', 'lamp', 'crates', 'trough']
    tries = int(900 * density)
    for _ in range(tries):
        a = rnd.random() * 2 * math.pi
        r = rnd.uniform(10, R)
        x, z = int(C + math.cos(a) * r), int(C + math.sin(a) * r)
        kind = rnd.choice(kinds)
        size = {'garden': 3, 'well': 2, 'cart': 4, 'tree': 3, 'trough': 2}.get(kind, 1)
        if not b.free(x - size, z - size, x + size, z + size):
            continue
        if any(b.blocks.get((x + dx, G, z + dz), ('minecraft:air',))[0] != 'minecraft:air'
               for dx in range(-size, size + 1) for dz in range(-size, size + 1)):
            continue
        if kind == 'garden':
            for dx in range(-3, 4):
                for dz in range(-2, 3):
                    if abs(dx) == 3 or abs(dz) == 2:
                        b.set(x + dx, G, z + dz, st['fence'], waterlogged='false')
                    else:
                        b.set(x + dx, G - 1, z + dz, 'minecraft:farmland', moisture='7')
                        crop = rnd.choice(['minecraft:wheat', 'minecraft:carrots', 'minecraft:potatoes', 'minecraft:beetroots'])
                        b.set(x + dx, G, z + dz, crop, age='3' if crop.endswith('beetroots') else '7')
            b.set(x, G - 1, z, 'minecraft:water', level='0')
            b.set(x, G, z, 'minecraft:oak_trapdoor', facing='north', half='bottom', open='false', powered='false', waterlogged='true')
            if rnd.random() < 0.5:
                b.entities.append((x + 2, G, z - 3, 'minecraft:chicken', {}))
        elif kind == 'bench':
            facing = rnd.choice(['north', 'south', 'east', 'west'])
            b.bench(x, z, facing)
            dx, dz = DIRS[facing]
            b.bench(x + dz, z + dx, facing)
        elif kind == 'flowers':
            b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
            b.set(x, G, z, 'minecraft:' + rnd.choice(['poppy', 'dandelion', 'cornflower', 'allium', 'oxeye_daisy', 'rose_bush', 'peony',
                                                      'lilac']), **({'half': 'lower'} if False else {}))
        elif kind == 'hay':
            b.set(x, G, z, 'minecraft:hay_block', axis='y')
            if rnd.random() < 0.5:
                b.set(x, G + 1, z, 'minecraft:hay_block', axis='x')
        elif kind == 'tree':
            b.set(x, G - 1, z, 'minecraft:grass_block', snowy='false')
            log, leaves = {'elf': ('minecraft:birch_log', 'minecraft:azalea_leaves'), 'demon': ('minecraft:crimson_stem', 'minecraft:nether_wart_block'),
                           'dwarf': ('minecraft:spruce_log', 'minecraft:spruce_leaves')}.get(b.style, ('minecraft:oak_log', 'minecraft:oak_leaves'))
            if leaves.endswith('block'):
                b.tree(x, z, rnd.randint(4, 6), log, leaves, 2.2)
            else:
                b.tree(x, z, rnd.randint(4, 6), log, leaves, 2.4)
        elif kind == 'well':
            b.well(x, z)
        elif kind == 'cart':
            from mapbuild_pois import wagon
            wagon(b, x, z, rnd.choice(['x', 'z']), rnd.choice(['white', 'brown', 'red', 'light_gray']))
            continue
        elif kind == 'lamp':
            b.lamp_post(x, z, 3)
        elif kind == 'crates':
            b.set(x, G, z, 'minecraft:barrel', facing='up', open='false')
            b.set(x + 1, G, z, 'minecraft:barrel', facing='up', open='false') if rnd.random() < 0.6 else None
            b.set(x, G + 1, z, 'minecraft:barrel', facing='up', open='false') if rnd.random() < 0.4 else None
        elif kind == 'trough':
            for dx in (-1, 0, 1):
                b.set(x + dx, G, z, 'minecraft:water_cauldron', level='3')
        b.reserve(x - size, z - size, x + size, z + size)
    # кошки и собаки на улицах
    for i in range(int(6 * density)):
        a = rnd.random() * 2 * math.pi
        r = rnd.uniform(12, R * 0.8)
        x, z = int(C + math.cos(a) * r), int(C + math.sin(a) * r)
        if b.blocks.get((x, G, z), ('minecraft:air',))[0] == 'minecraft:air':
            b.entities.append((x, G, z, 'minecraft:cat' if i % 3 else 'minecraft:wolf', {} if i % 3 else {}))
