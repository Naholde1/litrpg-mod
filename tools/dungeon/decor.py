"""Отделка залов подземелья: саркофаги, статуи, кости, жаровни, люстры, полки, обломки."""
import math

from .build import h3


def coffin(m, x, y, z, axis='z', lid='minecraft:polished_blackstone_slab', body='minecraft:polished_blackstone', candles=True, seed=0):
    """Саркофаг 1×2 (по оси axis) с крышкой-плитой и свечами у изголовья."""
    dx, dz = (0, 1) if axis == 'z' else (1, 0)
    for i in range(2):
        m.set(x + dx * i, y, z + dz * i, body)
        m.set(x + dx * i, y + 1, z + dz * i, lid.replace('_slab', '_slab'), type='bottom', waterlogged=False)
    if candles:
        cx, cz = x - dx, z - dz
        if not m.solid_at(cx, y, cz):
            m.candle(cx, y, cz, n=1 + int(h3(x, y, z, seed) * 3))


def statue(m, x, y, z, facing='south', block='minecraft:stone', head='minecraft:chiseled_stone_bricks', weeping=True):
    """Фигура в плаще (3 блока) на постаменте; плачущая — склонённая голова-стенка."""
    m.set(x, y, z, 'minecraft:chiseled_stone_bricks')
    m.set(x, y + 1, z, block)
    m.set(x, y + 2, z, block)
    m.set(x, y + 3, z, head)
    fx, fz = {'north': (0, -1), 'south': (0, 1), 'east': (1, 0), 'west': (-1, 0)}[facing]
    if weeping:
        m.set(x + fx, y + 3, z + fz, 'minecraft:stone_brick_stairs', facing=facing, half='top', shape='straight', waterlogged=False)
        # руки-крылья
        sx, sz = -fz, fx
        for s in (-1, 1):
            m.set(x + sx * s, y + 2, z + sz * s, 'minecraft:stone_brick_wall', up=True, north='none', south='none', east='none', west='none',
                  waterlogged=False)


def bones(m, x1, z1, x2, z2, y, density=0.08, seed=0):
    """Россыпь костей и черепов по полу."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            if not m.is_open(x, y, z) or m.solid_at(x, y, z) or not m.solid_at(x, y - 1, z) and (x, y - 1, z) in m.open:
                continue
            r = h3(x, y, z, seed + 77)
            if r < density * 0.25:
                m.skull(x, y, z, rot=int(h3(x, z, y, seed) * 16))
            elif r < density * 0.45:
                m.set(x, y, z, 'minecraft:bone_block', axis='x' if r < density * 0.35 else 'z')
            elif r < density:
                m.set(x, y, z, 'minecraft:white_carpet' if r < density * 0.7 else 'minecraft:light_gray_carpet')


def cobwebs(m, x1, y1, z1, x2, y2, z2, chance=0.25, seed=0):
    """Паутина по верхним углам (у стен и потолка)."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                if not m.is_open(x, y, z):
                    continue
                walls = sum(1 for d in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0)) if (x + d[0], y + d[1], z + d[2]) not in m.open)
                if walls >= 2 and h3(x, y, z, seed + 5) < chance:
                    m.set(x, y, z, 'minecraft:cobweb')


def brazier(m, x, y, z, soul=True, base='minecraft:polished_blackstone_wall'):
    m.set(x, y, z, base, up=True, north='none', south='none', east='none', west='none', waterlogged=False)
    m.set(x, y + 1, z, 'minecraft:soul_campfire' if soul else 'minecraft:campfire', lit=True, facing='north', signal_fire=False,
          waterlogged=False)


def chandelier(m, x, ytop, z, r=2, soul=True, drop=3):
    """Люстра: цепь, обод из свечей-фонарей."""
    for i in range(drop):
        m.set(x, ytop - i, z, 'minecraft:chain', axis='y', waterlogged=False)
    y = ytop - drop
    m.set(x, y, z, 'minecraft:polished_blackstone_wall' if soul else 'minecraft:spruce_fence', up=True, north='none', south='none',
          east='none', west='none', waterlogged=False)
    for dx, dz in ((r, 0), (-r, 0), (0, r), (0, -r)):
        m.set(x + dx, y + 1, z + dz, 'minecraft:chain', axis='y', waterlogged=False)
        m.lantern(x + dx, y, z + dz, soul=soul, hanging=True)


def shelf_wall(m, x1, z1, x2, z2, y, h, seed=0):
    """Стена книжных полок с паутиной и черепами."""
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            for yy in range(y, y + h):
                r = h3(x, yy, z, seed)
                m.set(x, yy, z, 'minecraft:bookshelf' if r > 0.2 else 'minecraft:chiseled_bookshelf' if r > 0.1 else 'minecraft:cobweb')


def rubble(m, x1, z1, x2, z2, y, block='minecraft:cobblestone', chance=0.06, seed=0):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            if m.is_open(x, y, z) and not m.solid_at(x, y, z) and h3(x, y, z, seed + 9) < chance:
                r = h3(z, y, x, seed)
                if r < 0.4:
                    m.set(x, y, z, block.replace('cobblestone', 'cobblestone_slab') if 'cobblestone' in block else block, type='bottom',
                          waterlogged=False) if 'cobblestone' in block else m.set(x, y, z, block)
                elif r < 0.7:
                    m.set(x, y, z, 'minecraft:cobblestone_stairs', facing=['north', 'south', 'east', 'west'][int(r * 40) % 4], half='bottom',
                          shape='straight', waterlogged=False)
                else:
                    m.set(x, y, z, block)


def pillar_row(m, xs, z, y, h, block, trim=None, big=False):
    for x in xs:
        if big:
            m.big_pillar(x, z, y, h, block, trim)
        else:
            m.pillar(x, z, y, h, block, cap=trim, base=trim)


def arch(m, x, y, z, axis, w, h, block, stair=None):
    """Арка проёма: столбы и перемычка (по оси axis: 'x' — проём смотрит вдоль z)."""
    for j in range(h + 1):
        for s in (-1, w):
            if axis == 'x':
                m.set(x + s, y + j, z, block)
            else:
                m.set(x, y + j, z + s, block)
    for i in range(-1, w + 1):
        if axis == 'x':
            m.set(x + i, y + h, z, block)
        else:
            m.set(x, y + h, z + i, block)


def ring_floor(m, cx, cz, r0, r1, y, block):
    for x in range(int(cx - r1) - 1, int(cx + r1) + 2):
        for z in range(int(cz - r1) - 1, int(cz + r1) + 2):
            d = math.hypot(x - cx, z - cz)
            if r0 <= d <= r1:
                m.set(x, y, z, block)


def gravestone(m, x, y, z, facing='south'):
    m.set(x, y, z, 'minecraft:mossy_stone_brick_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
    m.set(x, y + 1, z, 'minecraft:stone_brick_slab', type='bottom', waterlogged=False)


def niche_skulls(m, x, y, z, facing, n=3, seed=0):
    """Ниша в стене со стопкой черепов (катакомбы)."""
    for i in range(n):
        if h3(x, y + i, z, seed) < 0.7:
            m.skull(x, y + i, z, rot={'north': 8, 'south': 0, 'east': 12, 'west': 4}[facing])
