"""
Подземелье Системы: карта (шаблоны-плитки 32×32) и схема прохождения → src/main/resources/litrpg_dungeon.json.
Этапы — tools/dungeon/stageN.py, Преддверие — tools/dungeon/lobby.py, строитель — tools/dungeon/build.py.

Запуск: python tools/gen_dungeon.py [--only 1,2] [--preview build/preview/dungeon]
  --only — пересобрать только эти этапы (остальные плитки и схема берутся из прошлого json).
"""
import json
import os
import shutil
import sys
import time

sys.path.insert(0, os.path.dirname(__file__))
from dungeon import build as B  # noqa: E402
from dungeon import lobby  # noqa: E402

STAGES = {}
for n in range(1, 6):
    try:
        STAGES[n] = __import__(f'dungeon.stage{n}', fromlist=['build'])
    except ImportError:
        pass

OUT = os.path.join(B.ROOT, 'litrpg_dungeon.json')


def main():
    args = sys.argv[1:]
    only = None
    prev_dir = None
    if '--only' in args:
        only = {int(x) for x in args[args.index('--only') + 1].split(',')}
    if '--preview' in args:
        prev_dir = args[args.index('--preview') + 1]
        os.makedirs(prev_dir, exist_ok=True)
    old = json.load(open(OUT, encoding='utf-8')) if os.path.exists(OUT) and only else None
    if not only and os.path.isdir(B.STRUCT):
        shutil.rmtree(B.STRUCT)

    data = {'tiles': [], 'stages': []}
    t0 = time.time()
    if not only or 0 in only:
        m, lb = lobby.build()
        if only:
            remove_tiles('lobby')
        data['tiles'] += B.save_tiles(m, 'lobby')
        data['lobby'] = lb
        print(f'lobby: {len(m.blocks)} blocks, {time.time() - t0:.1f}s')
        if prev_dir:
            B.preview(m, os.path.join(prev_dir, 'lobby.png'), (-24, -46, 24, 24), scale=4)
    else:
        data['lobby'] = old['lobby']
        data['tiles'] += [t for t in old['tiles'] if '/lobby_' in t[3]]
    for n, mod in sorted(STAGES.items()):
        if only and n not in only:
            if old:
                data['stages'] += [s for s in old['stages'] if s['n'] == n]
                data['tiles'] += [t for t in old['tiles'] if f'/s{n}_' in t[3]]
            continue
        t1 = time.time()
        m, st = mod.build()
        if only:
            remove_tiles(f's{n}')
        tiles = B.save_tiles(m, f's{n}')
        data['tiles'] += tiles
        data['stages'].append(st.json())
        mobs = sum(len(p['mobs']) for p in st.packs)
        print(f'stage {n} «{st.name}»: {len(m.blocks)} blocks, {len(tiles)} tiles, {len(st.packs)} packs/{mobs} mobs, '
              f'{len(st.bosses)} bosses, {len(st.mechs)} mechs, {time.time() - t1:.1f}s')
        if prev_dir:
            x1, _, z1, x2, _, z2 = st.box
            B.preview(m, os.path.join(prev_dir, f'stage{n}.png'), (x1, z1, x2, z2), scale=2)
    data['stages'].sort(key=lambda s: s['n'])
    with open(OUT, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, separators=(',', ':'))
    print(f'done: {len(data["tiles"])} tiles, {time.time() - t0:.1f}s')


def remove_tiles(prefix):
    if not os.path.isdir(B.STRUCT):
        return
    for f in os.listdir(B.STRUCT):
        if f.startswith(prefix + '_'):
            os.remove(os.path.join(B.STRUCT, f))


if __name__ == '__main__':
    main()
