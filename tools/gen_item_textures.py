"""
Генератор текстур предметов 16x16. Каждый спрайт — рисунок из символов
палитры. Запуск: python tools/gen_item_textures.py [--preview out.png]
"""
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png, hex_rgba, scale  # noqa: E402

PALETTE = {
    '.': None,
    'K': 0x1A1020,  # контур
    'H': 0xFFFFFF,
    # золото
    'Y': 0xFFE680, 'y': 0xF2C33A, 'o': 0xC08A1E, 'O': 0x7A5212,
    # фиолетовый (самоцветы, магия)
    'P': 0xE6B3FF, 'p': 0xB36BE6, 'q': 0x7A33B3, 'Q': 0x4A1A73,
    # синий (мана)
    'B': 0xBFE8FF, 'b': 0x5AB4FF, 'n': 0x2A6FD6, 'N': 0x163A80,
    # стекло
    'G': 0xF0FAFF, 'g': 0xA8C8D8, 'h': 0x6A8A9A,
    # дерево
    'W': 0xC8945A, 'w': 0x9A6A3A, 'v': 0x6A4422, 'V': 0x40280F,
    # сталь
    'S': 0xF4F8FC, 's': 0xC8D0D8, 'i': 0x8E98A4, 'I': 0x5A6470,
    # кость
    'E': 0xFFF8E6, 'e': 0xE6DCC0, 'u': 0xBFB090, 'U': 0x8A7A5A,
    # гоблинская кожа
    'L': 0xA8E07A, 'l': 0x6FB040, 'm': 0x4A8028, 'M': 0x2E5218,
    # красный
    'R': 0xFF8A8A, 'r': 0xE03A3A, 'd': 0xA01818, 'D': 0x5A0A0A,
    # пергамент
    'A': 0xFFF4D6, 'a': 0xEAD8A8, 'c': 0xC8B07A, 'C': 0x8A7040,
    # души (бирюза)
    'T': 0xD6FFFF, 't': 0x6FF0E6, 'x': 0x2AB0B8, 'X': 0x146A7A,
    # тьма
    'Z': 0x241632, 'z': 0x3E2A56,
    # кожа переплёта
    'F': 0x8A3A2A, 'f': 0x5A2018,
}

SPRITES = {
    'system_key': [
        "..........KKKK..",
        ".........KyYYyK.",
        "........KyoKKoyK",
        "........KYKPpKyK",
        "........KyKpqKoK",
        "........KyoKKoOK",
        ".......KyyyooOK.",
        "......KyoKKKK...",
        ".....KyoK.......",
        "....KyoK........",
        "...KyoK.........",
        "..KyoKK.........",
        ".KyoKoyK........",
        "KyoK.KoK........",
        ".KK.KyK.........",
        ".....K..........",
    ],
    'mana_potion': [
        "......KKKK......",
        ".....KWwwvK.....",
        ".....KwvvVK.....",
        "......KggK......",
        "......KGgK......",
        ".....KgGghK.....",
        "....KgbbbbhK....",
        "...KGBbbbbnhK...",
        "..KgBbbbbbbnhK..",
        "..KGbbbbbbnnhK..",
        "..KgbbHbbnnnhK..",
        "..KgbbbbnnnNhK..",
        "...KhnnnnnNhK...",
        "....KhhNNhhK....",
        ".....KKKKKK.....",
        "................",
    ],
    'tome_of_experience': [
        "................",
        "..KKKKKKKKKKK...",
        "..KyFFFFFFFFyK..",
        "..KFfFFFFFFfFKA.",
        "..KFFFlLlFFFFKa.",
        "..KFFlLHLlFFFKa.",
        "..KFFLHLHLFFFKa.",
        "..KFFlLHLlFFFKa.",
        "..KFFFlLlFFFFKa.",
        "..KFfFFFFFFfFKa.",
        "..KyFFFFFFFFyKa.",
        "..KKKKKKKKKKKKa.",
        "...KAaaaaaaaaac.",
        "....KccccccccC..",
        "................",
        "................",
    ],
    'soul_shard': [
        "................",
        ".........KK...T.",
        "........KTtK....",
        ".......KTttxK...",
        ".......KTtxxK...",
        "......KTttxXK...",
        "......KTtxxXK...",
        ".....KTTtxxXK...",
        ".....KTtttxXK...",
        "....KTttxxXK....",
        "....KTtxxXXK....",
        "...KTttxXXK.....",
        "...KTtxxXK......",
        ".T..KtxXK.......",
        ".....KKK........",
        "................",
    ],
    'scroll_of_oblivion': [
        "................",
        "...KKK..........",
        "..KcaAK.........",
        "..KCcaAKKKKKK...",
        "...KCAaaaaaaaK..",
        "....KaAaaaaacK..",
        "....KAaqqqaacK..",
        "....KaqPpqaacK..",
        "....KAaqpqaacK..",
        "....KaAaqaaacK..",
        "....KAaaaaaacK..",
        "....KKKKKKKKAcK.",
        "...........KaAcK",
        "...........KCcK.",
        "............KK..",
        "................",
    ],
    'apprentice_staff': [
        "............KKK.",
        "...........KbBBK",
        "..........KnbBbK",
        "..........KNnbnK",
        ".........KyKNNK.",
        "........KWvK....",
        ".......KWvK.....",
        "......KWvK......",
        ".....KWvK.......",
        "....KWvK........",
        "...KWvK.........",
        "..KWvK..........",
        ".KWvK...........",
        "KWvK............",
        "KvK.............",
        ".K..............",
    ],
    'lich_staff': [
        "..........K.K.K.",
        ".........KpKqKpK",
        ".........KqPpPqK",
        "..........KpPpK.",
        ".........KUKqK..",
        "........KeuK....",
        ".......KEuK.....",
        "......KeUK......",
        ".....KEuK.......",
        "....KeuK........",
        "...KEUK.........",
        "..KeuK..........",
        ".KEuK...........",
        "KeUK............",
        "KUK.............",
        ".K..............",
    ],
    'rogue_dagger': [
        "................",
        "................",
        "..........KK....",
        ".........KSsK...",
        "........KSsiK...",
        ".......KSsiK....",
        "......KSsiK.....",
        "....KKSsiK......",
        "...KoyKiK.......",
        "....KoyK........",
        "...KwKKoK.......",
        "..KwvK.KK.......",
        ".KwvK...........",
        ".KvK............",
        "..K.............",
        "................",
    ],
    'shadow_dagger': [
        "................",
        "...........K....",
        "..........KPK...",
        ".........KPqK...",
        "........KPqzK...",
        ".......KPqzK....",
        "......KPqzK.....",
        "....KKPqzK......",
        "...KqpKzK.......",
        "....KqpK........",
        "...KZKKqK.......",
        "..KZzK.KK.......",
        ".KZzK...........",
        ".KzK............",
        "..K.............",
        "................",
    ],
    'king_cleaver': [
        "......KKKKK.....",
        ".....KsSSSsK....",
        "....KsSSSSiK....",
        "....KsSSSiiK....",
        "...KsSSSiiIK....",
        "...KsSSiiIK.....",
        "..KsSSiiIK......",
        "..KsSiiIK.......",
        "..KKsiIK........",
        "..KyKKK.........",
        ".KyoK...........",
        ".KwvK...........",
        "KwvK............",
        "KvvK............",
        "KVK.............",
        ".K..............",
    ],
    'goblin_ear': [
        "................",
        ".............K..",
        "............KlK.",
        "...........KLlK.",
        "..........KLlmK.",
        ".........KLlrmK.",
        "........KLlrrmK.",
        ".......KLlrdrmK.",
        "......KLlrdrmK..",
        ".....KLlrdrmK...",
        "....KLlrrrmK....",
        "...KLllrmmK.....",
        "...KlllmmK......",
        "...KmmmMK.......",
        "....KKKK........",
        "................",
    ],
    'bone_shard': [
        "................",
        "................",
        "..........KKK...",
        ".........KEeEK..",
        "........KEeeuK..",
        ".......KEedeK...",
        "......KEeeuK....",
        ".....KEeduK.....",
        "....KEeeuUK.....",
        "...KEeEuUK......",
        "..KEeuuUK.......",
        "..KeuKUK........",
        "..KuK.KK........",
        "...K............",
        "................",
        "................",
    ],
    'goblin_seal': [
        "................",
        ".....KKKKKK.....",
        "...KKYYYYyyKK...",
        "..KYYyooooyyOK..",
        "..KYyoMmmmMoyOK.",
        ".KYyoMlLLlMoyOK.",
        ".KYyoMrlLrMoyOK.",
        ".KYyoMlllllMyOK.",
        ".KYyoMlKKKlMyOK.",
        ".KyyoMmlllmMyOK.",
        "..KyoMMMMMMoyOK.",
        "..KyyooooooyOK..",
        "...KKyyyyyOKK...",
        ".....KKKKKK.....",
        "................",
        "................",
    ],
    'lich_phylactery': [
        "................",
        ".......KK.......",
        "......KyyK......",
        ".....KyOOyK.....",
        ".....KKyyKK.....",
        "....KzPqqqzK....",
        "...KzPppqqQzK...",
        "...KyPpPpqQyK...",
        "...KzppPpqQzK...",
        "...KzpppqqQzK...",
        "...KyqppqQQyK...",
        "....KzqqqQzK....",
        ".....KyzzyK.....",
        "......KyyK......",
        ".......KK.......",
        "................",
    ],
    'mana_orb': [
        "................",
        "................",
        "................",
        "......bnnb......",
        ".....bBBbnb.....",
        "....bBHBbbnb....",
        "....nBBbbbnn....",
        "....nbbbbnnN....",
        "....nbbbnnNN....",
        "....bnnnnNNb....",
        ".....bnNNNb.....",
        "......bnnb......",
        "................",
        "................",
        "................",
        "................",
    ],
}


def render(rows):
    assert len(rows) == 16, f"{len(rows)} rows"
    out = []
    for r in rows:
        assert len(r) == 16, f"row '{r}' has {len(r)} chars"
        out.append([(0, 0, 0, 0) if PALETTE[c] is None else hex_rgba(PALETTE[c]) for c in r])
    return out


def main():
    root = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg', 'textures', 'item')
    rendered = {}
    for name, rows in SPRITES.items():
        try:
            rendered[name] = render(rows)
        except AssertionError as e:
            sys.exit(f"{name}: {e}")
        write_png(os.path.join(root, name + '.png'), rendered[name], 16, 16)
    import gen_extra_textures
    gen_extra_textures.generate(PALETTE, rendered, render)
    if '--preview' in sys.argv:
        out = sys.argv[sys.argv.index('--preview') + 1]
        names = list(rendered)
        cols = 5
        cell = 16 * 8 + 8
        rows_n = (len(names) + cols - 1) // cols
        sheet = [[(30, 32, 38, 255)] * (cols * cell) for _ in range(rows_n * cell)]
        for i, n in enumerate(names):
            big = scale(rendered[n], 8)
            ox, oy = (i % cols) * cell + 4, (i // cols) * cell + 4
            for y, row in enumerate(big):
                for x, p in enumerate(row):
                    sheet[oy + y][ox + x] = p
        write_png(out, sheet, cols * cell, rows_n * cell)
    print(f"written {len(rendered)} item textures")


if __name__ == '__main__':
    main()
