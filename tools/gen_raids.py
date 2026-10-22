"""
Большие подземелья (рейды) — по три хранителя подряд, между ними рунные преграды (litrpg:seal_door, ступень = stage).
  raid_khazdarum   — «Глубины Кхаз-Дарума» (ур. 32–35): врата холда → кузня Бронзоборода → шахты → нора Гхаза → трон Торбальда.
  raid_abyss_spire — «Шпиль Бездны» (ур. 58–62): башня в три яруса — Близнецы, Хронофаг, Око Ксал'Атара.
Печати рейдов держат только на своей арене и только пока хранитель пробуждён (ArenaOnly).
Ставятся на карту Мира Системы (gen_mapbuild), проверить: /place template litrpg:raid_khazdarum ~ ~ ~
Запуск: python tools/gen_raids.py [--preview build/preview/raids.png]
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_structures import Build, TInt, TByte, TList, ROOT  # noqa: E402


def pick(seed, opts, x, y, z):
    if isinstance(opts, str):
        return opts
    return opts[(x * 73856093 ^ y * 19349663 ^ z * 83492791 ^ seed) % len(opts)]


class Raid(Build):
    def __init__(self, key, sx, sy, sz, wall, floor, light):
        super().__init__(sx, sy, sz, key)
        self.key = key
        self.seed = sum(ord(c) for c in key) * 104729
        self.wall, self.floor, self.light = wall, floor, light
        self.open = set()

    def wb(self, x, y, z, mat=None):
        self.set(x, y, z, pick(self.seed, mat or self.wall, x, y, z))

    def solid(self, x1, y1, z1, x2, y2, z2, mat=None):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.wb(x, y, z, mat)

    def carve(self, x1, z1, x2, z2, y0, h, floor=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(y0, y0 + h):
                    self.set(x, y, z, 'minecraft:air')
                self.set(x, y0 - 1, z, pick(self.seed, floor or self.floor, x, y0 - 1, z))
                self.open.add((x, y0, z))

    def carve_round(self, cx, cz, r, y0, h, floor=None):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            for z in range(int(cz - r) - 1, int(cz + r) + 2):
                if math.hypot(x - cx, z - cz) <= r:
                    self.carve(x, z, x, z, y0, h, floor)

    def lights(self, y0, h, step=6):
        for (x, y, z) in list(self.open):
            if y != y0 or (x * 3 + z * 7) % (step * 2 + 1):
                continue
            top = y0 + h
            if self.blocks.get((x, top, z), ('',))[0] != 'minecraft:air':
                self.set(x, top, z, self.light)

    def mob(self, x, y, z, kind, lvl):
        eid = kind if ':' in kind else 'minecraft:' + kind
        self.entities.append((x, y, z, eid, {'ForgeData': {'litrpg_dlvl': TInt(lvl)}}))

    def mobs(self, spots, y, kinds, lvl):
        for i, (x, z) in enumerate(spots):
            self.mob(x, y, z, kinds[i % len(kinds)], lvl - 1 + i % 3)

    def chest_at(self, x, y, z, table, facing='north'):
        self.chest(x, y, z, 'litrpg:chests/dungeon_' + table, facing)

    def altar(self, x, y, z, dungeon, stage, box):
        """box — границы арены в абсолютных координатах шаблона (x1, y1, z1, x2, y2, z2)."""
        self.set(x, y, z, 'litrpg:boss_altar')
        rel = (box[0] - x, box[1] - y, box[2] - z, box[3] - x, box[4] - y, box[5] - z)
        self.nbt[(x, y, z)] = {'id': 'litrpg:boss_altar', 'Dungeon': dungeon, 'Stage': TInt(stage), 'ArenaOnly': TByte(1),
                               'Box': TList(3, [TInt(v) for v in rel])}

    def door(self, x1, y1, z1, x2, y2, z2, stage):
        for x in range(x1, x2 + 1):
            for y in range(y1, y2 + 1):
                for z in range(z1, z2 + 1):
                    self.set(x, y, z, 'litrpg:seal_door', stage=str(stage))

    def relic(self, x, y, z, kind, pedestal=None):
        if pedestal:
            self.set(x, y, z, pedestal)
            y += 1
        self.set(x, y, z, 'litrpg:relic', kind=kind)


# ============================ Глубины Кхаз-Дарума ============================

def khazdarum():
    SX, SY, SZ = 96, 30, 64
    G, F = 20, 2
    b = Raid('khazdarum', SX, SY, SZ,
             ['minecraft:deepslate_bricks', 'minecraft:deepslate_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:polished_deepslate'],
             ['minecraft:deepslate_tiles', 'minecraft:polished_deepslate', 'minecraft:deepslate_tiles'], 'minecraft:ochre_froglight')
    b.solid(0, 0, 0, SX - 1, G - 2, SZ - 1)
    # ---- вход: врата холда на поверхности и лестница вниз ----
    for x in range(40, 57):
        for z in range(0, 9):
            b.set(x, G - 1, z, pick(b.seed, ['minecraft:polished_deepslate', 'minecraft:deepslate_tiles'], x, 0, z))
            for y in range(G, SY):
                b.set(x, y, z, 'minecraft:air')
    for x in (42, 54):
        for y in range(G, G + 8):
            b.set(x, y, 2, 'minecraft:chiseled_deepslate' if y % 3 == 0 else 'minecraft:polished_deepslate')
    for x in range(42, 55):
        b.set(x, G + 8, 2, 'minecraft:deepslate_brick_slab', type='bottom', waterlogged='false')
    for x in range(43, 54):
        for y in range(G + 5, G + 8):
            if not (45 <= x <= 51 and y < G + 7):
                b.set(x, y, 2, 'minecraft:polished_deepslate')
    for x, z in ((41, 6), (55, 6)):
        b.set(x, G, z, 'minecraft:lantern', hanging='false', waterlogged='false')
    for k in range(G - F):
        y, z = G - 1 - k, 3 + k
        for x in range(46, 51):
            b.set(x, y, z, 'minecraft:deepslate_brick_stairs', facing='south', half='bottom', shape='straight', waterlogged='false')
            for h in range(1, 6):
                b.set(x, y + h, z, 'minecraft:air')
        if k % 4 == 1:
            b.set(48, y + 5, z, 'minecraft:ochre_froglight')
    # ---- врата холда (зал со статуями) ----
    b.carve(38, 21, 58, 34, F, 8)
    for x in (40, 56):
        for z in (23, 28, 32):
            for y in range(F, F + 6):
                b.set(x, y, z, 'minecraft:polished_deepslate' if y < F + 5 else 'minecraft:chiseled_deepslate')
    b.mobs([(44, 25), (52, 25), (44, 31), (52, 31), (48, 28), (46, 33), (50, 23)], F,
           ['skeleton', 'litrpg:grave_knight', 'zombie', 'litrpg:skeleton_warden'], 31)
    b.chest_at(39, F, 33, 'khaz_forge', 'east')
    # ---- арена 1: Кузня Бронзоборода ----
    b.carve(4, 20, 34, 50, F, 12)
    b.carve(35, 26, 37, 30, F, 5)                      # проход из врат
    for (x, z) in ((6, 24), (6, 46), (32, 35)):        # три горна (узлы силы)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                b.set(x + dx, F, z + dz, 'minecraft:polished_blackstone_bricks')
        b.relic(x, F + 1, z, 'fire')
        b.set(x, F + 2, z, 'minecraft:campfire', lit='true', signal_fire='true', facing='north', waterlogged='false')
    for z in range(22, 49):                            # лава под решётками вдоль стен
        for x in (5, 33):
            if not (z in range(33, 38) and x == 33):
                b.set(x, F - 1, z, 'minecraft:lava', level='0')
                b.set(x, F, z, 'minecraft:iron_bars', north='true', south='true', east='false', west='false', waterlogged='false')
    for x, z in ((12, 28), (26, 28), (12, 42), (26, 42)):
        b.set(x, F, z, 'minecraft:anvil', facing='north')
    b.altar(19, F, 35, 'khaz_forge', 0, (4, F - 1, 20, 34, F + 12, 50))
    b.chest_at(18, F, 49, 'khaz_forge', 'north')
    b.chest_at(20, F, 49, 'khaz_forge', 'north')
    # ---- рунная преграда 0 → шахты ----
    b.carve(17, 51, 21, 56, F, 5)
    b.door(17, F, 51, 21, F + 4, 51, 0)
    b.carve(17, 56, 31, 60, F, 5)                      # шахтный ход с рельсами и мобами
    for x in range(17, 32):
        b.set(x, F, 58, 'minecraft:rail', shape='east_west', waterlogged='false')
    for x in range(18, 31, 4):
        for z in (56, 60):
            for y in range(F, F + 5):
                b.set(x, y, z, 'minecraft:stripped_spruce_log', axis='y')
    b.mobs([(20, 57), (24, 59), (28, 57), (30, 59)], F, ['zombie', 'litrpg:stone_troll', 'skeleton'], 32)
    # ---- арена 2: Нора Гхаза ----
    b.carve_round(46, 49, 13, F, 12, ['minecraft:tuff', 'minecraft:cobbled_deepslate', 'minecraft:rooted_dirt', 'minecraft:tuff'])
    b.carve(31, 56, 34, 60, F, 5)
    r = random.Random(3)
    for _ in range(14):
        x, z = r.randint(36, 56), r.randint(39, 60)
        if math.hypot(x - 46, z - 49) < 12 and math.hypot(x - 46, z - 49) > 4:
            b.set(x, F, z, r.choice(['minecraft:cobbled_deepslate', 'minecraft:tuff', 'minecraft:pointed_dripstone']),
                  **({'vertical_direction': 'up', 'thickness': 'tip', 'waterlogged': 'false'} if r.random() < 0.0 else {}))
    for (x, z) in ((40, 40), (53, 44), (42, 57)):
        b.set(x, F + 11, z, 'minecraft:pointed_dripstone', vertical_direction='down', thickness='tip', waterlogged='false')
    b.altar(46, F, 49, 'khaz_deep', 1, (33, F - 1, 36, 59, F + 12, 62))
    b.chest_at(46, F, 61, 'khaz_deep', 'north')
    # ---- рунная преграда 1 → трон ----
    b.carve(59, 47, 66, 51, F, 6)
    b.door(60, F, 47, 60, F + 5, 51, 1)
    # ---- арена 3: Тронный зал Торбальда ----
    b.carve(66, 26, 94, 62, F, 13, ['minecraft:polished_deepslate', 'minecraft:deepslate_tiles'])
    for (x, z) in ((72, 34), (88, 34), (72, 54), (88, 54)):      # рунные плиты
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                b.set(x + dx, F - 1, z + dz, 'minecraft:chiseled_stone_bricks')
        b.set(x, F + 12, z, 'minecraft:sea_lantern')
    for (x, z) in ((70, 44), (90, 44), (80, 30), (80, 58)):      # груды проклятого золота
        b.relic(x, F, z, 'gold', pedestal='minecraft:gold_block')
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            b.set(x + dx, F, z + dz, 'minecraft:raw_gold_block')
    for x in range(77, 84):                                      # трон
        b.set(x, F, 60, 'minecraft:gold_block')
        b.set(x, F + 1, 61, 'minecraft:gold_block')
    b.set(80, F + 1, 60, 'minecraft:polished_deepslate_stairs', facing='north', half='bottom', shape='straight', waterlogged='false')
    for x in (68, 92):
        for z in range(28, 62, 6):
            for y in range(F, F + 13):
                b.set(x, y, z, 'minecraft:polished_deepslate' if y % 4 else 'minecraft:chiseled_deepslate')
    b.altar(80, F, 44, 'khaz_throne', 2, (66, F - 1, 26, 94, F + 13, 62))
    for x in (78, 82):
        b.chest_at(x, F + 1, 59, 'khaz_throne', 'south')
    for y0, h in ((F, 8), (F, 12), (F, 13), (F, 5)):
        b.lights(y0, h)
    b.save('raid_khazdarum')
    return b, G


# ============================ Шпиль Бездны ============================

def spire():
    SX, SY, SZ = 52, 70, 56
    B = 5                     # пол первого яруса (y), под ним фундамент
    H = 16                    # высота яруса
    R = 18                    # радиус арены
    CX, CZ = 24, 24
    b = Raid('abyss_spire', SX, SY, SZ,
             ['minecraft:purpur_block', 'minecraft:obsidian', 'minecraft:crying_obsidian', 'minecraft:purpur_block', 'minecraft:obsidian'],
             ['minecraft:polished_blackstone', 'minecraft:obsidian', 'minecraft:polished_blackstone'], 'minecraft:end_rod')
    # корпус башни: круг R+2 и лестничная шахта (x 44..49, z 3..21) на всю высоту
    top = B + 3 * H
    for x in range(SX):
        for z in range(SZ):
            d = math.hypot(x - CX, z - CZ)
            shaft = 43 <= x <= 49 and 2 <= z <= 22
            if d <= R + 2 or shaft:
                for y in range(0, top + 1):
                    b.wb(x, y, z)
    # вестибюль у подножия (юг)
    b.solid(17, 0, 42, 31, B + 6, 53)
    b.carve(19, 44, 29, 53, B, 6)
    for x in range(19, 30):
        b.set(x, B - 1, 54, 'minecraft:polished_blackstone')
    b.carve(22, 40, 26, 44, B, 5)
    for x in (19, 29):
        for y in range(B, B + 6):
            b.set(x, y, 53, 'minecraft:crying_obsidian')
    b.mobs([(21, 47), (27, 47), (24, 50), (21, 51), (27, 51)], B, ['enderman', 'vex', 'wither_skeleton'], 57)
    b.chest_at(20, B, 45, 'spire_twins', 'east')
    arenas = [('spire_twins', 0, 'void'), ('spire_time', 1, 'time'), ('spire_eye', 2, None)]
    for i, (key, stage, relic) in enumerate(arenas):
        y0 = B + i * H
        b.carve_round(CX, CZ, R, y0, H - 2)
        b.lights(y0, H - 2, 4)
        for k in range(16):                        # светящееся кольцо у стен яруса
            a = k * math.pi / 8
            x, z = int(round(CX + math.cos(a) * (R - 1))), int(round(CZ + math.sin(a) * (R - 1)))
            b.set(x, y0 - 1, z, 'minecraft:pearlescent_froglight', axis='y')
            b.set(x, y0 + 4, z + (0 if abs(math.sin(a)) < 0.7 else 0), 'minecraft:end_rod', facing='up')
        b.altar(CX, y0, CZ, key, stage, (CX - R - 1, y0 - 1, CZ - R - 1, CX + R + 1, y0 + H - 2, CZ + R + 1))
        b.chest_at(CX - 1, y0, CZ + R - 1, key, 'north')
        b.chest_at(CX + 1, y0, CZ + R - 1, key, 'north')
        if key == 'spire_time':                    # песочные часы
            for a in (0.5, 2.6, 4.7):
                x, z = int(CX + math.cos(a) * 12), int(CZ + math.sin(a) * 12)
                b.relic(x, y0, z, 'time', pedestal='minecraft:chiseled_quartz_block')
        if key == 'spire_eye':                     # колонны — укрытие от Взгляда
            for k in range(6):
                a = k * math.pi / 3 + 0.4
                x, z = int(CX + math.cos(a) * 11), int(CZ + math.sin(a) * 11)
                for dx in (0, 1):
                    for dz in (0, 1):
                        for y in range(y0, y0 + H - 2):
                            b.set(x + dx, y, z + dz, 'minecraft:obsidian' if y % 4 else 'minecraft:crying_obsidian')
        if key == 'spire_twins':                   # два края арены — светлый и тёмный
            for x in range(CX - R + 2, CX - R + 6):
                for z in range(CZ - 3, CZ + 4):
                    b.set(x, y0 - 1, z, 'minecraft:gold_block')
            for x in range(CX + R - 5, CX + R - 1):
                for z in range(CZ - 3, CZ + 4):
                    b.set(x, y0 - 1, z, 'minecraft:lapis_block')
    # лестница: ярус 1 (z 20 → 5), ярус 2 (z 5 → 20); вход из арены 1 у z 18..20, в арену 2 — у z 4..6, в арену 3 — у z 18..20
    for i in range(2):
        y0 = B + i * H
        for k in range(H):
            z = 20 - k if i == 0 else 5 + k
            y = y0 + k
            for x in range(45, 48):
                b.set(x, y, z, 'minecraft:purpur_stairs', facing='north' if i == 0 else 'south', half='bottom', shape='straight', waterlogged='false')
                for h in range(1, 5):
                    b.set(x, y + h, z, 'minecraft:air')
            if k % 5 == 2:
                b.set(48, y + 2, z, 'minecraft:end_rod', facing='west')
        # площадки
    for (y0, zs) in ((B, (18, 21)), (B + H, (3, 6)), (B + 2 * H, (18, 21))):
        for x in range(44, 49):
            for z in range(zs[0], zs[1] + 1):
                b.set(x, y0 - 1, z, 'minecraft:purpur_block')
                for y in range(y0, y0 + 4):
                    b.set(x, y, z, 'minecraft:air')
    # проходы из арен в шахту и преграды
    b.carve(41, 18, 44, 20, B, 4)
    b.door(43, B, 18, 43, B + 3, 20, 0)               # из зала Близнецов наверх — после победы
    b.carve(41, 4, 44, 6, B + H, 4)
    b.door(44, B + H, 7, 48, B + H + 4, 7, 1)         # с яруса 2 выше — после Хронофага
    b.carve(41, 18, 44, 20, B + 2 * H, 4)
    # крыша и шпиль
    for x in range(SX):
        for z in range(SZ):
            d = math.hypot(x - CX, z - CZ)
            if d <= R + 2:
                b.set(x, top, z, 'minecraft:purple_stained_glass' if d < R - 1 else 'minecraft:obsidian')
    for y in range(top + 1, SY):
        r = max(0, (SY - y) / (SY - top) * 4)
        for x in range(CX - 5, CX + 6):
            for z in range(CZ - 5, CZ + 6):
                if math.hypot(x - CX, z - CZ) <= r:
                    b.set(x, y, z, 'minecraft:crying_obsidian' if (y + x) % 5 == 0 else 'minecraft:obsidian')
    b.set(CX, SY - 1, CZ, 'minecraft:end_rod', facing='up')
    b.save('raid_abyss_spire')
    return b, B


def loot():
    tiers = {
        'khaz_forge': (32, 3, 6), 'khaz_deep': (33, 3, 6), 'khaz_throne': (35, 4, 8),
        'spire_twins': (58, 4, 7), 'spire_time': (60, 4, 7), 'spire_eye': (62, 5, 9)}
    d = os.path.join(ROOT, 'loot_tables', 'chests')
    os.makedirs(d, exist_ok=True)
    for key, (lvl, lo, hi) in tiers.items():
        items = [('minecraft:diamond', 3, 2, 5), ('minecraft:emerald', 3, 3, 9), ('litrpg:mana_potion', 3, 2, 5),
                 ('minecraft:experience_bottle', 3, 5, 12), ('minecraft:golden_apple', 2, 1, 3), ('litrpg:tome_of_experience', 2, 1, 2)]
        if lvl >= 50:
            items += [('minecraft:netherite_scrap', 2, 1, 3), ('minecraft:enchanted_golden_apple', 1, 1, 1)]
        pool = {"rolls": {"type": "minecraft:uniform", "min": lo, "max": hi}, "entries": [
            {"type": "minecraft:item", "name": n, "weight": w,
             "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": a, "max": c}}]}
            for n, w, a, c in items]}
        json.dump({"type": "minecraft:chest", "pools": [pool]}, open(os.path.join(d, 'dungeon_' + key + '.json'), 'w'), indent=1)


def preview(out):
    from png import write_png
    S = 4
    imgs = []
    for fn in (khazdarum, spire):
        b, base = fn()
        sx, sy, sz = b.size
        layers = [base] if fn is khazdarum else [base + i * 16 for i in range(3)]
        if fn is khazdarum:
            layers = [2]
        for y in layers:
            img = [[(0, 0, 0, 255)] * (sx * S) for _ in range(sz * S)]
            for x in range(sx):
                for z in range(sz):
                    bl = b.blocks.get((x, y + 1, z), ('',))[0]
                    f0 = b.blocks.get((x, y, z), ('',))[0]
                    c = (200, 190, 160) if bl in ('minecraft:air',) else (60, 60, 70) if bl else (20, 20, 20)
                    if 'relic' in f0 or 'relic' in bl: c = (200, 60, 255)
                    if 'seal_door' in f0: c = (0, 160, 255)
                    if 'boss_altar' in f0: c = (0, 255, 255)
                    if 'chest' in f0: c = (255, 220, 0)
                    if 'chiseled_stone' in b.blocks.get((x, y - 1, z), ('',))[0]: c = (120, 220, 120)
                    for dy in range(S):
                        for dx in range(S):
                            img[z * S + dy][x * S + dx] = c + (255,)
            imgs.append(img)
    W = sum(len(i[0]) for i in imgs) + 4 * len(imgs)
    H = max(len(i) for i in imgs)
    canvas = [[(30, 30, 30, 255)] * W for _ in range(H)]
    ox = 0
    for img in imgs:
        for y, row in enumerate(img):
            canvas[y][ox:ox + len(row)] = row
        ox += len(img[0]) + 4
    write_png(out, canvas, W, H)


def main():
    a, _ = khazdarum()
    s, _ = spire()
    loot()
    print('raid_khazdarum', len(a.blocks), 'blocks', len(a.entities), 'mobs')
    print('raid_abyss_spire', len(s.blocks), 'blocks', len(s.entities), 'mobs')
    if '--preview' in sys.argv:
        preview(sys.argv[sys.argv.index('--preview') + 1])


if __name__ == '__main__':
    main()
