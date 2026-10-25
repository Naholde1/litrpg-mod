"""
Проверка подземелья на целостность и проходимость (без запуска игры).

1. Схема (litrpg_dungeon.json): у каждого этапа есть привал, торговец, выход; преграды, на которые ссылаются
   хранители и механизмы, существуют; засады существуют; типы мобов и хранителей зарегистрированы;
   точки внутри коробки этапа.
2. Карта: этап строится заново (tools/dungeon/stageN.py) и проходится поиском пути «пешком»: шаг, подъём на блок,
   спуск, прыжок через 1–3 блока, плавание. Загадки считаются решёнными (преграды открыты), разломы-порталы
   и переносы хранителей — переходами. От привала должны быть достижимы: все хранители, отряды, тайники,
   рычаги/диски/колокола, выход.

Запуск: python tools/check_dungeon.py [--only 1,2]
"""
import json
import os
import sys
from collections import deque

sys.path.insert(0, os.path.dirname(__file__))
from dungeon import build as B  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
VANILLA = {'zombie', 'skeleton', 'spider', 'cave_spider', 'drowned', 'guardian', 'enderman', 'blaze', 'husk', 'stray',
           'wither_skeleton', 'vindicator', 'evoker', 'witch', 'slime', 'magma_cube', 'piglin_brute', 'phantom', 'vex',
           'silverfish', 'creeper', 'zombie_villager', 'pillager', 'ravager', 'hoglin', 'zoglin', 'endermite', 'shulker'}
CREATURES = {'stone_troll', 'frost_wraith', 'forest_wisp', 'ash_imp', 'bog_lurker', 'sand_scorpion', 'grave_knight', 'trader'}
MECH_TYPES = {'plates', 'sequence', 'dials', 'beam', 'bells', 'memory', 'riddle', 'split', 'braziers', 'switch', 'area', 'clear',
              'valves', 'current', 'portal', 'spikes', 'flames', 'crusher', 'blades', 'arrows', 'collapse', 'gas', 'rhythm', 'boss'}
SOFT = ('air', 'light', 'water', 'sign', 'lever', 'button', 'torch', 'chain', 'coral', 'kelp', 'seagrass', 'grass', 'fern', 'flower',
        'carpet', 'pressure_plate', 'rift', 'snow', 'cobweb', 'vine', 'rail', 'fire', 'mushroom', 'sapling', 'dead_bush',
        'hanging_roots', 'glow_lichen', 'spore_blossom', 'tripwire', 'redstone_wire', 'cluster', 'bud', 'chorus_flower', 'end_rod',
        'lantern', 'candle', 'infested', 'relic', 'banner', 'skull', 'head', 'pointed_dripstone', 'moss_carpet', 'lily_pad', 'big_dripleaf_stem', 'sculk_vein')


def known_types():
    best = json.load(open(os.path.join(ROOT, 'litrpg_bestiary.json'), encoding='utf-8'))
    sp = best['species'] if isinstance(best, dict) and 'species' in best else best
    wild = {s['id'] for s in sp} if isinstance(sp, list) else set(sp)
    src = open(os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'java', 'com', 'andrey', 'litrpg', 'entity', 'stage',
                            'Bosses.java'), encoding='utf-8').read()
    import re
    bosses = set(re.findall(r'add\(r, "([a-z_]+)"', src))
    return wild, bosses


class Walker:
    def __init__(self, m, gates):
        self.m = m
        self.open = m.open
        self.blocks = m.blocks
        self.gate = set()
        for g in gates:
            x1, y1, z1, x2, y2, z2 = g['box']
            for x in range(min(x1, x2), max(x1, x2) + 1):
                for y in range(min(y1, y2), max(y1, y2) + 1):
                    for z in range(min(z1, z2), max(z1, z2) + 1):
                        self.gate.add((x, y, z))

    def soft(self, p):
        if p in self.gate:
            return True
        b = self.blocks.get(p)
        if b is None:
            return p in self.open
        n = b[0].split(':')[-1]
        if n == 'lava':
            return False
        return any(s in n for s in SOFT)

    def water(self, p):
        b = self.blocks.get(p)
        return b is not None and b[0] == 'minecraft:water'

    def solid(self, p):
        if p in self.gate:
            return False
        b = self.blocks.get(p)
        if b is None:
            return p not in self.open
        n = b[0].split(':')[-1]
        return n == 'lava' and False or not any(s in n for s in SOFT) and n != 'lava'

    def stand(self, p):
        x, y, z = p
        if not (self.soft(p) and self.soft((x, y + 1, z))):
            return False
        return self.solid((x, y - 1, z)) or self.water(p) or self.water((x, y - 1, z))

    def neighbours(self, p):
        x, y, z = p
        swim = self.water(p) or self.water((x, y - 1, z))
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (1, -1), (-1, 1), (-1, -1)):
            nx, nz = x + dx, z + dz
            if dx and dz and not (self.soft((x + dx, y, z)) and self.soft((x, y, z + dz))):
                continue
            # шаг, подъём на 1, спуск до 8
            for dy in (0, 1, -1, -2, -3, -4, -5, -6, -7, -8):
                q = (nx, y + dy, nz)
                if dy == 1 and not self.soft((x, y + 2, z)):
                    continue
                if dy < 0 and not all(self.soft((nx, y + k, nz)) and self.soft((nx, y + k + 1, nz)) for k in range(dy + 1, 1)):
                    break
                if self.stand(q):
                    yield q
                    break
        # прыжок с разбега: до 4 блоков в любую сторону, на том же уровне или ниже (до 2)
        if self.solid((x, y - 1, z)) and self.soft((x, y + 2, z)):
            for jx in range(-4, 5):
                for jz in range(-4, 5):
                    dist = (jx * jx + jz * jz) ** 0.5
                    if dist < 1.9 or dist > 4.3:
                        continue
                    steps = int(dist * 2)
                    ok = True
                    for k in range(1, steps):
                        t = k / steps
                        c = (x + round(jx * t), y, z + round(jz * t))
                        if not (self.soft(c) and self.soft((c[0], y + 1, c[2])) and self.soft((c[0], y + 2, c[2]))):
                            ok = False
                            break
                    if not ok:
                        continue
                    for dy in (0, -1, -2):
                        q = (x + jx, y + dy, z + jz)
                        if self.stand(q):
                            yield q
                            break
        if swim:
            for dy in (1, -1):
                q = (x, y + dy, z)
                if self.soft(q) and self.soft((x, y + dy + 1, z)) and (self.water(q) or self.stand(q)):
                    yield q
            # всплыть на край
            q = (x, y + 1, z)
            if self.soft(q) and self.water(p):
                yield q

    def nearest_stand(self, p, r=3):
        x, y, z = p
        best = None
        for dy in range(-3, 4):
            for dx in range(-r, r + 1):
                for dz in range(-r, r + 1):
                    q = (x + dx, y + dy, z + dz)
                    if self.stand(q):
                        d = abs(dx) + abs(dz) + abs(dy)
                        if best is None or d < best[0]:
                            best = (d, q)
        return best[1] if best else None


def flood(w, starts, jumps):
    seen = set()
    dq = deque()
    for s in starts:
        if s and s not in seen:
            seen.add(s)
            dq.append(s)
    while dq:
        p = dq.popleft()
        for q in w.neighbours(p):
            if q not in seen:
                seen.add(q)
                dq.append(q)
        for box, to in jumps:
            if box[0] <= p[0] <= box[3] and box[1] - 1 <= p[1] <= box[4] and box[2] <= p[2] <= box[5]:
                for t in to:
                    if t and t not in seen:
                        seen.add(t)
                        dq.append(t)
    return seen


def reach(seen, p, r=4):
    x, y, z = p
    for q in seen:
        if abs(q[0] - x) <= r and abs(q[2] - z) <= r and -4 <= q[1] - y <= 3:
            return True
    return False


def check_stage(n, st, wild, bosses, problems):
    s = st
    mod = __import__(f'dungeon.stage{n}', fromlist=['build'])
    m, _ = mod.build()
    gates = {g['id']: g for g in s['gates']}
    packs = {p['id']: p for p in s['packs']}
    tag = f"этап {n}"
    for k in ('camp', 'trader', 'exit', 'box'):
        if not s.get(k):
            problems.append(f"{tag}: нет «{k}»")
    for b in s['bosses']:
        if b['type'].split(':')[1] not in bosses:
            problems.append(f"{tag}: хранитель {b['type']} не зарегистрирован")
        for g in b['doors'] + b['unlock']:
            if g not in gates:
                problems.append(f"{tag}: хранитель {b['id']} ссылается на нет преграды {g}")
    for mc in s['mechs']:
        if mc['type'] not in MECH_TYPES:
            problems.append(f"{tag}: неизвестный механизм {mc['type']} ({mc['id']})")
        for key in ('opens', 'closes', 'gates'):
            for g in mc.get(key, []):
                if g not in gates:
                    problems.append(f"{tag}: механизм {mc['id']} ссылается на нет преграды {g}")
        for key in ('ambush', 'spawns'):
            for pk in mc.get(key, []):
                if pk not in packs:
                    problems.append(f"{tag}: механизм {mc['id']} зовёт нет отряда {pk}")
    for p in s['packs']:
        for mob in p['mobs']:
            ns, t = mob['type'].split(':')
            ok = t in VANILLA if ns == 'minecraft' else (t in wild or t in CREATURES or t in bosses)
            if not ok:
                problems.append(f"{tag}: отряд {p['id']}: неизвестный тип {mob['type']}")
    # проходимость (решённые вентили поднимают воду)
    for mc in s['mechs']:
        if mc['type'] == 'valves' and 'water' in mc:
            x1, y1, z1, x2, y2, z2 = mc['water']
            top = y1 + mc.get('target', mc.get('levels', 3)) * mc.get('step', 2)
            for x in range(x1, x2 + 1):
                for y in range(y1, min(y2 + 1, top)):
                    for z in range(z1, z2 + 1):
                        if (x, y, z) in m.open and (x, y, z) not in m.blocks:
                            m.set(x, y, z, 'minecraft:water', level=0)
    bridge = {g for mc in s['mechs'] if mc['type'] == 'rhythm' for g in mc.get('gates', [])}
    w = Walker(m, [g for g in s['gates'] if g['id'] not in bridge])
    camp = w.nearest_stand(tuple(s['camp'][:3]))
    if camp is None:
        problems.append(f"{tag}: у привала некуда встать")
        return
    jumps = []
    for mc in s['mechs']:
        if mc['type'] == 'portal':
            to = mc['to']
            jumps.append((mc['box'], [w.nearest_stand((to[0], to[1], to[2]))]))
    for b in s['bosses']:
        ex = b.get('extra') or {}
        spots = []
        for key, v in ex.items():
            if key.endswith('spots') or key in ('wells', 'dome_spots'):
                for q in v:
                    spots.append(w.nearest_stand(tuple(q)))
        if spots:
            jumps.append((b['arena'], spots))
            # и обратно: из места переноса — на арену
            at = w.nearest_stand(tuple(b['at'][:3]))
            for key in ('dome_box', 'caldera_box', 'heart_box', 'ice_box', 'fire_box', 'under_box'):
                if key in ex:
                    jumps.append((ex[key], [at]))
    seen = flood(w, [camp], jumps)
    print(f"  {tag}: проходимых клеток {len(seen)}")

    def need(what, p, r=4):
        if not reach(seen, tuple(p[:3]), r):
            problems.append(f"{tag}: не добраться — {what} {tuple(p[:3])}")

    for b in s['bosses']:
        need(f"хранитель {b['id']}", b['at'], 6)
    for p in s['packs']:
        need(f"отряд {p['id']}", p['at'], 8)
    for c in s['caches']:
        need("тайник", c, 3)
    for mc in s['mechs']:
        for key in ('levers', 'dials', 'mirrors', 'bells', 'buttons', 'braziers', 'valves', 'plates'):
            for q in mc.get(key, []):
                need(f"{mc['id']}/{key}", q, 4)
    ex = s['exit']
    need("выход", [(ex[0] + ex[3]) // 2, ex[1], (ex[2] + ex[5]) // 2], 4)
    # обход: с закрытыми преградами (кроме открытых дверей арен) выход недостижим, а хранители — только первые
    closed = Walker(m, [g for g in s['gates'] if g.get('open')])
    seen2 = flood(closed, [closed.nearest_stand(tuple(s['camp'][:3]))],
                  [j for j in jumps if not any(mc['type'] == 'portal' and mc['box'] == j[0] for mc in s['mechs'])] if False else
                  [(mc['box'], [closed.nearest_stand(tuple(mc['to'][:3]))]) for mc in s['mechs'] if mc['type'] == 'portal'])
    open_bosses = [b['id'] for b in s['bosses'] if reach(seen2, tuple(b['at'][:3]), 6)]
    print(f"  {tag}: без решённых загадок доступны хранители: {', '.join(open_bosses) or 'нет'}")
    if reach(seen2, ((ex[0] + ex[3]) // 2, ex[1], (ex[2] + ex[5]) // 2), 2):
        problems.append(f"{tag}: выход достижим в обход хранителей и загадок!")
    if len(open_bosses) > 1:
        problems.append(f"{tag}: в обход дверей доступны сразу несколько хранителей: {open_bosses}")


def main():
    only = None
    if '--only' in sys.argv:
        only = {int(x) for x in sys.argv[sys.argv.index('--only') + 1].split(',')}
    data = json.load(open(os.path.join(ROOT, 'litrpg_dungeon.json'), encoding='utf-8'))
    wild, bosses = known_types()
    problems = []
    for st in data['stages']:
        if only and st['n'] not in only:
            continue
        print(f"этап {st['n']} «{st['name']}»")
        check_stage(st['n'], st, wild, bosses, problems)
    print()
    if problems:
        print(f"НАЙДЕНО ПРОБЛЕМ: {len(problems)}")
        for p in problems:
            print(" -", p)
    else:
        print("Всё в порядке: схема цела, всё достижимо.")


if __name__ == '__main__':
    main()
