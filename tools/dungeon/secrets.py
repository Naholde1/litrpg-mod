"""
Секретные комнаты и пасхалки: находятся сами.

Скрипт ищет в готовой карте этапа (до стен `shell`) место у пола коридора, за которым в одну
сторону на десяток блоков нет ни одной пустоты — туда можно спрятать комнату, не задев
соседние залы. В стене ставится треснувший кирпич (ломается — тег litrpg:breakable), за ним
короткий лаз и комната 5×5 с тайником высшей ступени, свечами и табличкой-пасхалкой.
Комнаты разносятся подальше друг от друга и от уже стоящих тайников.
"""
import random

from . import decor as D

DIRS = [(1, 0, 'east', 'west'), (-1, 0, 'west', 'east'), (0, 1, 'south', 'north'), (0, -1, 'north', 'south')]
WALL = 'minecraft:infested_cracked_stone_bricks'

# пасхалки: по четыре строки на табличку (до ~15 знаков)
EGGS = [
    ['Здесь был', 'Андрей.', 'Сломал стену —', 'молодец.'],
    ['Система:', 'секрет найден.', '+1 к чувству', 'собственной важности'],
    ['Уровень 99', 'был бы здесь,', 'если бы', 'не потолок.'],
    ['Если читаешь —', 'ты простучал', 'все стены.', 'Уважаю.'],
    ['Не забудь', 'покормить', 'кота. Кота', 'здесь нет.'],
    ['Ловушки чинит', 'гном. Гнома', 'не видели', 'три этапа.'],
    ['Предыдущий', 'герой оставил', 'лишь записку:', '«Не дёргай рычаг»'],
    ['Хранитель', 'просил передать:', 'он не злой,', 'это работа.'],
    ['Под этим', 'камнем лежит', 'баланс', 'урона. Тсс.'],
    ['ГГ всегда', 'находит', 'тайник. Так', 'устроены книги.'],
]


def _clear(m, cells, margin, origin, d):
    """Ни одной пустоты в клетках и вокруг них на margin — считая только то, что лежит за стеной (дальше 1 блока)."""
    ox, oz = origin
    for (x, y, z) in cells:
        for dx in range(-margin, margin + 1):
            for dy in range(-margin, margin + 1):
                for dz in range(-margin, margin + 1):
                    c = (x + dx, y + dy, z + dz)
                    if (c[0] - ox) * d[0] + (c[2] - oz) * d[1] < 2:
                        continue
                    if c in m.open:
                        return False
    return True


def _plan(x, y, z, d):
    """Клетки лаза (2) и комнаты (5×4×5) за стеной в направлении d от клетки коридора (x,y,z)."""
    dx, dz = d[0], d[1]
    sx, sz = -dz, dx  # поперёк
    passage = [(x + dx * k, y + h, z + dz * k) for k in (2, 3) for h in range(3)]
    room = []
    for k in range(4, 9):
        for w in range(-2, 3):
            for h in range(4):
                room.append((x + dx * k + sx * w, y + h, z + dz * k + sz * w))
    return passage, room


def add(m, st, n, box, seed, avoid=()):
    """Добавить до n секретных комнат. box — (x1, z1, x2, z2) коробки этапа; avoid — точки, от которых держаться подальше."""
    rnd = random.Random(seed)
    cand = []
    for (x, y, z), t in m.open.items():
        if (x, y - 1, z) in m.open or (x, y + 1, z) not in m.open or (x, y + 2, z) not in m.open:
            continue
        if (x, y, z) in m.blocks or (x, y, z) in m.nowall:
            continue  # дно пропасти — не пол
        cand.append((x, y, z, t))
    # вторые локации хранителей (Изнанка и т.п.) доступны только в бою — туда тайники не прячем
    zones = []
    for bo in st.bosses:
        for k, v in (bo.get('extra') or {}).items():
            if k.endswith('_box') and isinstance(v, list) and len(v) == 6:
                zones.append(v)
    cand = [c for c in cand if not any(z[0] - 6 <= c[0] <= z[3] + 6 and z[1] - 6 <= c[1] <= z[4] + 6 and z[2] - 6 <= c[2] <= z[5] + 6
                                       for z in zones)]
    rnd.shuffle(cand)
    taken = [tuple(a[:3]) for a in avoid] + [tuple(c[:3]) for c in st.caches]
    made = 0
    for (x, y, z, theme) in cand:
        if made >= n:
            break
        if any(abs(x - a[0]) + abs(z - a[2]) < 45 for a in taken):
            continue
        for d in DIRS:
            wall = [(x + d[0], y + h, z + d[1]) for h in range(3)]
            if any(c in m.open or c in m.blocks for c in wall):
                continue
            passage, room = _plan(x, y, z, d)
            far = [c for c in room + passage]
            if not all(box[0] + 4 <= c[0] <= box[2] - 4 and box[1] + 4 <= c[2] <= box[3] - 4 for c in far):
                continue
            # вокруг комнаты — 4 блока камня; лаз проверяем без клеток у самого коридора
            if not _clear(m, room, 4, (x, z), d) or not _clear(m, passage, 2, (x, z), d):
                continue
            th = m.themes[theme]
            for c in passage + room:
                m.open[c] = theme
            for c in wall:
                m.set(*c, WALL)
            # тайник у дальней стены, табличка рядом, свечи по углам
            dx, dz = d[0], d[1]
            sx, sz = -dz, dx
            cx, cz = x + dx * 8, z + dz * 8
            m.cache(cx, y, cz, tier=2, facing=d[3])
            egg = EGGS[(seed + made * 7) % len(EGGS)]
            m.sign(cx + sx * 2, y + 1, cz + sz * 2, egg, facing=d[3])
            for w in (-2, 2):
                m.candle(x + dx * 4 + sx * w, y, z + dz * 4 + sz * w, n=3)
                m.candle(x + dx * 7 + sx * w, y, z + dz * 7 + sz * w, n=2)
            D.bones(m, min(x + dx * 4, x + dx * 8) - 2, min(z + dz * 4, z + dz * 8) - 2,
                    max(x + dx * 4, x + dx * 8) + 2, max(z + dz * 4, z + dz * 8) + 2, y, density=0.08, seed=seed + made)
            xs = [c[0] for c in room]
            zs = [c[2] for c in room]
            st.mech(f's{st.n}_hidden{made}', 'area', box=[min(xs), y, min(zs), max(xs), y + 3, max(zs)], xp=True,
                    solved='Тайная комната найдена!')
            st.caches.append([cx, y, cz, 2])
            taken.append((cx, y, cz))
            made += 1
            break
    return made
