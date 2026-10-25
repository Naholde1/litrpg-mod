"""
Преддверие: круглый зал у Врат Подземелья. Здесь появляются искатели, выбирают роль, читают наставления
и собираются на печати у врат (нужно от 3 до 5 игроков). Врата переносят группу на привал первого этапа.
"""
import math

from .build import Map, Theme

Y = 64


def build():
    m = Map(seed=101)
    hall = m.theme(Theme('vestibule', [('minecraft:deepslate_bricks', 5), ('minecraft:polished_deepslate', 2), ('minecraft:cracked_deepslate_bricks', 1)],
                         floor=[('minecraft:polished_deepslate', 3), ('minecraft:deepslate_tiles', 2)], ceil='minecraft:deepslate_tiles',
                         trim='minecraft:chiseled_deepslate', seed=3))
    # круглый зал с куполом
    m.round_room(0, 0, 16, Y, 9, hall, dome=7)
    # галерея к вратам (север)
    m.carve(-4, Y, -30, 4, Y + 7, -16, hall)
    m.room(-9, -40, 9, -30, Y, 12, hall, vault=4)

    # пол: узор-звезда из светлого камня
    for x in range(-16, 17):
        for z in range(-16, 17):
            d = math.hypot(x, z)
            if d <= 16:
                a = math.atan2(z, x)
                if abs(d - 10) < 0.6 or abs(d - 5) < 0.5 or (d < 10 and abs(math.sin(a * 4)) < 0.08):
                    m.set(x, Y - 1, z, 'minecraft:polished_blackstone_bricks')
    m.fill(-1, Y - 1, -1, 1, Y - 1, 1, 'minecraft:gilded_blackstone')
    # колонны по кругу с огнями душ
    for i in range(10):
        a = math.pi * 2 * i / 10
        x, z = round(math.cos(a) * 13.5), round(math.sin(a) * 13.5)
        if z < -10 and abs(x) < 6:
            continue
        m.pillar(x, z, Y, 9, 'minecraft:polished_deepslate', cap='minecraft:chiseled_deepslate', base='minecraft:chiseled_deepslate')
        m.set(x, Y + 9, z, 'minecraft:deepslate_brick_slab', type='bottom', waterlogged=False)
        m.lantern(round(math.cos(a) * 12.4), Y + 4, round(math.sin(a) * 12.4), soul=True)
    # люстра под куполом
    for i in range(6):
        a = math.pi * 2 * i / 6
        m.chain_lantern(round(math.cos(a) * 4), Y + 14, round(math.sin(a) * 4), length=3, soul=False)
    m.chain_lantern(0, Y + 15, 0, length=4)

    # скамьи по бокам галереи
    for z in range(-28, -18, 3):
        m.stairs(-3, Y, z, 'minecraft:dark_oak_stairs', 'east')
        m.stairs(3, Y, z, 'minecraft:dark_oak_stairs', 'west')
    # Врата Подземелья: арка из чёрного камня с разломом внутри (переход — по печати)
    for x in range(-5, 6):
        for y in range(Y, Y + 10):
            edge = abs(x) == 5 or y == Y + 9
            if edge:
                m.set(x, y, -40, 'minecraft:polished_blackstone_bricks')
            elif abs(x) <= 3 and y <= Y + 7:
                m.set(x, y, -40, 'litrpg:rift')
    for x in (-6, 6):
        m.pillar(x, -39, Y, 11, 'minecraft:chiseled_polished_blackstone')
        m.set(x, Y + 11, -39, 'minecraft:soul_lantern', hanging=False, waterlogged=False)
    # печать у врат: круг золотого камня (сюда встают все)
    for x in range(-4, 5):
        for z in range(-37, -31):
            if math.hypot(x, z + 34) <= 3.6:
                m.set(x, Y - 1, z, 'minecraft:gilded_blackstone' if math.hypot(x, z + 34) > 2.5 else 'minecraft:crying_obsidian')

    # наставления на стенах
    signs = [
        (-11, 0, 'east', ['ПОДЗЕМЕЛЬЕ', 'СИСТЕМЫ', 'пять этапов,', '15 хранителей']),
        (-10, -5, 'east', ['Группа: 3–5', 'Нужны танк', 'и поддержка', '(клавиша K)']),
        (-10, 5, 'east', ['Пали все —', 'этап заново.', 'Вещи и опыт', 'остаются.']),
        (11, 0, 'west', ['Alt — рывок', 'Z X C V B G R H', '— навыки', 'K — персонаж']),
        (10, -5, 'west', ['Ctrl+1…5 —', 'выбрать союзника', 'для лечения', '(рамки слева)']),
        (10, 5, 'west', ['Павший — дух.', 'Поддержка', 'поднимет: присесть', 'у тела 3 с']),
        (0, 11, 'north', ['Торговец ждёт', 'на привале', 'каждого этапа.', 'Монеты — с врагов']),
    ]
    for x, z, f, lines in signs:
        m.set(x, Y, z, 'minecraft:polished_blackstone_wall', up=True, north='none', south='none', east='none', west='none', waterlogged=False)
        m.sign(x, Y + 1, z, lines, facing=f, wall=False)
    m.shell(thick=2)
    lobby = {
        'spawn': [0, Y, 8, 180],
        'box': [-20, Y - 4, -44, 20, Y + 24, 20],
        'start': [-4, Y - 1, -37, 4, Y + 3, -31],
        'gates': [],
        'lore': [
            {'box': [-16, Y - 2, -16, 16, Y + 10, 16], 'title': 'Преддверие', 'text': 'Система: «Собери группу и встань на печать у Врат. Дороги назад не будет.»'},
        ],
    }
    return m, lobby
