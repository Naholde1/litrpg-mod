"""
Контент экипировки: 100+ предметов по шаблонам. Генерирует:
  - src/main/resources/litrpg_items.json   (данные для Java: предметы, наборы, материалы брони)
  - текстуры предметов и брони, модели предметов
  - названия (ru/en), теги слотов Curios, рецепты
Запуск: python tools/gen_gear_content.py [--preview out.png]
"""
import json
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from png import write_png, hex_rgba, scale  # noqa: E402
import gen_extra_textures as gx  # noqa: E402

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', 'litrpg')
DATA = os.path.join(ROOT, 'data')

# ---------------------------------------------------------------------
# Формы 16x16. 1–3 основной материал (светлый→тёмный), 4–5 камень/акцент,
# 6–7 второй материал (кожа, дерево), W — блик, K — контур.
# ---------------------------------------------------------------------
SHAPES = {
    'bracelet': [
        "................", "................", "................", "................",
        "....KKKKKKKK....", "...K11144111K...", "..K1KKK55KKK3K..", "..K2K......K3K..",
        "..K2K......K3K..", "..K22KKKKKK33K..", "...K22222333K...", "....KKKKKKKK....",
        "................", "................", "................", "................"],
    'cloak': [
        "......KKKK......", ".....K4554K.....", "....K112223K....", "...K11222233K...",
        "...K11222233K...", "..K1122222233K..", "..K1122222233K..", ".K112222222233K.",
        ".K122222222233K.", ".K122222222333K.", "K12222222223333K", "K22222222233333K",
        "KKKKKKKKKKKKKKKK", "................", "................", "................"],
    'gloves': [
        "................", "....K.K.K.......", "...K1K1K1K......", "...K1K1K2K.K....",
        "...K12K22KK1K...", "...K1222222K....", "...K1222223K....", "...K1222233K....",
        "...K4444445K....", "...K6666667K....", "...K6666677K....", "....KKKKKKK.....",
        "................", "................", "................", "................"],
    'fang': [
        "................", "......K..K......", "......KKKK......", ".....K1111K.....",
        ".....K1122K.....", "......K122K.....", "......K12K......", ".......K2K......",
        ".......K3K......", "........KK......", "................", "................",
        "................", "................", "................", "................"],
    'claw': [
        "................", "...........KK...", "..........K1K...", ".........K12K...",
        "........K122K...", ".......K1223K...", "......K1223K....", ".....K1223K.....",
        "....K2233K......", "...K3333K.......", "...KKKKK........", "................",
        "................", "................", "................", "................"],
    'feather': [
        "............KKK.", "..........KK112K", ".........K11122K", "........K111223K",
        ".......K111223K.", "......K111223K..", ".....K11223KK...", "....K1223K......",
        "...K223K........", "..KK3K..........", ".K6K............", "K6K.............",
        ".K..............", "................", "................", "................"],
    'horseshoe': [
        "................", "................", "....KKKKKKKK....", "...K11111222K...",
        "..K11KKKKKK22K..", "..K1K......K2K..", "..K1K......K2K..", "..K1K......K3K..",
        "..K14K....K53K..", "..K14K....K53K..", "..KKKK....KKKK..", "................",
        "................", "................", "................", "................"],
    'rabbit_foot': [
        "................", "......KKK.......", ".....K111K......", ".....K112K......",
        "....K1122K......", "....K1122K......", "....K1222K......", "...K12222K......",
        "...K12223K......", "..K122233K......", "..K4K4K4KK......", "...KKKKK........",
        "................", "................", "................", "................"],
    'coin': [
        "................", "................", ".....KKKKKK.....", "....K111122K....",
        "...K1144442 2K..", "...K14411442K...", "...K14422442K...", "...K14455442K...",
        "...K12444423K...", "...K12222233K...", "....K222333K....", ".....KKKKKK.....",
        "................", "................", "................", "................"],
    'eye': [
        "................", "................", "................", "....KKKKKKKK....",
        "...K11111111K...", "..K1114444111K..", "..K1145KK5411K..", "..K1145KK5411K..",
        "..K1114444111K..", "...K11111111K...", "....KKKKKKKK....", "................",
        "................", "................", "................", "................"],
    'skull': [
        "................", "....KKKKKKKK....", "...K11111112K...", "..K1111111122K..",
        "..K1KK11KK122K..", "..K1K4K1K4K22K..", "..K1KK11KK122K..", "..K111KK11122K..",
        "...K11111122K...", "....K1K1K1KK....", "....KKKKKKK.....", "................",
        "................", "................", "................", "................"],
    'idol': [
        "................", "......KKKK......", ".....K1111K.....", ".....K4K4KK.....",
        ".....K1221K.....", "......KKKK......", ".....K1222K.....", "....K112223K....",
        "....K122223K....", ".....K1223K.....", ".....K1K3K......", "....KKKKKKK.....",
        "................", "................", "................", "................"],
    'rune': [
        "................", "................", "....KKKKKKK.....", "...K1111112K....",
        "...K1144412K....", "...K1141412K....", "...K1144412K....", "...K1141112K....",
        "...K1141112K....", "...K1222223K....", "....KKKKKKK.....", "................",
        "................", "................", "................", "................"],
    'totem': [
        ".....KKKKK......", "....K44444K.....", "....K4K4K4K.....", "....K55555K.....",
        ".....K111K......", "....K11112K.....", "....K1KK12K.....", "....K11112K.....",
        ".....K122K......", "....K66667K.....", "....K6KK67K.....", "....K66677K.....",
        ".....KKKKK......", "................", "................", "................"],
    'bell': [
        "................", ".......KK.......", "......K11K......", ".....K1112K.....",
        "....K111122K....", "....K112222K....", "...K11222223K...", "...K12222223K...",
        "..K1222222233K..", "..KKKKKKKKKKKK..", "......K44K......", ".......KK.......",
        "................", "................", "................", "................"],
    'acorn': [
        "................", ".......K........", "......KKK.......", "....K66666K.....",
        "...K6666677K....", "...KKKKKKKKK....", "....K11112K.....", "....K11122K.....",
        "....K11222K.....", ".....K1223K.....", "......K23K......", ".......KK.......",
        "................", "................", "................", "................"],
    'shell': [
        "................", "................", "......KKKK......", "....KK1111KK....",
        "...K11K11K12K...", "..K112K11K212K..", "..K12K1112K22K..", "...K2K122K22K...",
        "....KK2222KK....", "......KKKK......", "................", "................",
        "................", "................", "................", "................"],
    'crystal': [
        "................", ".......KK.......", "......K44K......", ".....K4445K.....",
        ".....K4455K.....", "....K444455K....", "....K445555K....", "....K445555K....",
        ".....K4555K.....", ".....K4555K.....", "......K55K......", ".......KK.......",
        "................", "................", "................", "................"],
    'sword': [
        "............KKK.", "...........K1W1K", "..........K1W1K.", ".........K1W12K.",
        "........K1W12K..", ".......K1W12K...", "......K1W12K....", "..KK.K1W12K.....",
        "..K4KKW12K......", "...K4422K.......", "...K5KK4K.......", "..K67K.K4K......",
        ".K67K...KK......", "K67K............", "K7K.............", ".K.............."],
    'axe': [
        "................", "......KKKK......", ".....K1112K.....", "....K11W122K....",
        "....K1W1223K....", ".....K1KK3K.....", "......KK6K......", ".......K6K......",
        ".......K6K......", ".......K7K......", ".......K6K......", ".......K7K......",
        ".......K6K......", ".......KKK......", "................", "................"],
    'mace': [
        "................", "....KKKKKKK.....", "...K1W11112K....", "...K1111122K....",
        "...K1122233K....", "....KKK6KKK.....", "......K6K.......", "......K6K.......",
        "......K7K.......", "......K6K.......", "......K7K.......", "......K6K.......",
        "......K4K.......", "......KKK.......", "................", "................"],
    'spear': [
        "............KK..", "...........K1WK.", "..........K11K..", ".........K12K...",
        "........K4KK....", ".......K6K......", "......K6K.......", ".....K7K........",
        "....K6K.........", "...K7K..........", "..K6K...........", ".K7K............",
        "K6K.............", "KK..............", "................", "................"],
    'dagger': [
        "................", "................", "..........KK....", ".........KW1K...",
        "........KW12K...", ".......KW12K....", "......KW12K.....", "....KKW12K......",
        "...K54K2K.......", "....K54K........", "...K6KK5K.......", "..K67K.KK.......",
        ".K67K...........", ".K7K............", "..K.............", "................"],
    'staff': [
        "............KKK.", "...........K4W4K", "..........K5445K", "..........K5545K",
        ".........K1K55K.", "........K67K....", ".......K67K.....", "......K67K......",
        ".....K67K.......", "....K67K........", "...K1K..........", "..K67K..........",
        ".K67K...........", "K67K............", "K7K.............", ".K.............."],
    'leaf': [
        "................", "..........KKK...", "........KK112K..", ".......K111122K.",
        "......K1112222K.", ".....K1112223K..", "....K1122233K...", "...K122233KK....",
        "...K23333K......", "..K6KKKK........", ".K6K............", "K6K.............",
        "................", "................", "................", "................"],
}
# опечатки в рисунке выше не допускаются — проверим длины
SHAPES['coin'][4] = "...K11444422K..."

METALS = {
    'copper': (0xF0A070, 0xC06A3A, 0x7A3A1A), 'iron': (0xE8ECF0, 0xA8B0B8, 0x5A6470),
    'silver': (0xFFFFFF, 0xC8D0D8, 0x7A8490), 'gold': (0xFFF0A0, 0xF2C33A, 0x9A6A12),
    'bone': (0xF4EEDC, 0xC9BE9E, 0x8A7F62), 'obsidian': (0x6A5A8E, 0x3A2C58, 0x1C142C),
    'wood': (0xC8945A, 0x9A6A3A, 0x6A4422), 'leather': (0xB0804A, 0x8A5A2A, 0x5A3A1A),
    'stone': (0xB4B4B4, 0x808080, 0x505050), 'mithril': (0xE0F6FF, 0xA8D8E8, 0x5A8EA0),
    'elven': (0xE0F4C8, 0x8CC870, 0x4A7A30), 'dwarven': (0xE8D098, 0xB08A4A, 0x6A4A20),
    'orcish': (0xA8988A, 0x6A5A4A, 0x3A2E24), 'beast': (0xD0AA70, 0x8E6E3B, 0x5A4220),
    'demonic': (0xF07050, 0xA02818, 0x501008), 'purple': (0xC090F0, 0x7040A0, 0x402060),
    'red': (0xE86A6A, 0xA03030, 0x601818), 'blue': (0x80A0F0, 0x4060B0, 0x203060),
    'green': (0x90D090, 0x4A8A4A, 0x2A4A2A), 'black': (0x6A6A7A, 0x3A3A48, 0x1C1C24),
    'brown': (0xB08A60, 0x7A5A3A, 0x4A3420), 'white': (0xFFFFFF, 0xDCDCE6, 0xA0A0B0),
    'feather': (0xFFFFFF, 0xD8D8E0, 0x9A9AA8), 'fire': (0xFFE080, 0xFF9020, 0xC03010),
    'ice': (0xF0FFFF, 0xA0E0F0, 0x50A0C0), 'ender': (0xA0F0D0, 0x40A090, 0x1A5A50),
    'leafgreen': (0xB0F090, 0x58B040, 0x2E6A20),
}
GEMS = {
    'ruby': (0xFF9A9A, 0xC01818), 'sapphire': (0xBFE8FF, 0x2A6FD6), 'emerald': (0xA8FFB0, 0x1F8A3A),
    'amethyst': (0xE6B3FF, 0x8A44C8), 'topaz': (0xFFE680, 0xD09010), 'onyx': (0x8A8A9A, 0x2A2A34),
    'pearl': (0xFFFFFF, 0xD8D0E0), 'fire': (0xFFD080, 0xFF5010), 'ice': (0xE0FFFF, 0x60C0E0),
    'ender': (0x80E0C0, 0x1A6A5A), 'star': (0xFFFFFF, 0x6FF0E6), 'blood': (0xFF6060, 0x800010),
    'glass': (0xF0FAFF, 0xA8C8D8), 'gold': (0xFFF0A0, 0xD09010), 'bone': (0xFFF8E6, 0xBFB090),
    'green': (0xC0FF80, 0x5AA020), 'void': (0xB080FF, 0x30106A),
}


def palette(metal, gem='ruby', second='leather'):
    m, g, s = METALS[metal], GEMS[gem], METALS[second]
    return {'1': m[0], '2': m[1], '3': m[2], '4': g[0], '5': g[1], '6': s[1], '7': s[2], 'W': 0xFFFFFF, 'K': 0x1A1020}


def render(shape, pal):
    rows = SHAPES[shape] if shape in SHAPES else gx.TEMPLATES[shape]
    out = []
    for r in rows:
        assert len(r) == 16, f"{shape}: '{r}' len {len(r)}"
        out.append([(0, 0, 0, 0) if c == '.' else hex_rgba(pal.get(c, pal.get('2'))) for c in r])
    return out


# ---------------------------------------------------------------------
# Содержимое: (id, рус., англ., вид слота, форма, (металл, камень, второй), уровень, врождённое, набор, мин. редкость)
# ---------------------------------------------------------------------
J = []


def j(id, ru, en, kind, shape, pal, tier, implicit, set=None, minr=None):
    J.append(dict(id=id, ru=ru, en=en, kind=kind, shape=shape, pal=pal, tier=tier, implicit=implicit, set=set, min=minr))


# --- кольца (24) ---
j('ring_novice', 'Кольцо новичка', 'Novice Ring', 'ring', 'ring', ('copper', 'glass'), 1, {'luck': 1})
j('ring_iron', 'Железное кольцо', 'Iron Ring', 'ring', 'ring', ('iron', 'onyx'), 3, {'armor': 0.5})
j('ring_trickster', 'Кольцо ловкача', "Trickster's Ring", 'ring', 'ring', ('silver', 'topaz'), 4, {'dodge': 2})
j('ring_wolf', 'Кольцо волчьего клыка', 'Wolf Fang Ring', 'ring', 'ring', ('bone', 'bone'), 5, {'dmg_beast': 15}, 'hunter_trophies')
j('ring_gravedigger', 'Кольцо могильщика', "Gravedigger's Ring", 'ring', 'ring', ('iron', 'bone'), 6, {'dmg_undead': 15}, 'deadmans_legacy')
j('ring_spider', 'Кольцо паукобоя', 'Spiderbane Ring', 'ring', 'ring', ('copper', 'emerald'), 6, {'dmg_arthropod': 15})
j('ring_archer', 'Кольцо стрелка', "Archer's Ring", 'ring', 'ring', ('wood', 'emerald'), 7, {'arrow_damage': 8})
j('ring_miser', 'Кольцо скряги', "Miser's Ring", 'ring', 'ring', ('gold', 'gold'), 5, {'coin_find': 15}, 'gifts_of_fortune')
j('ring_merchant', 'Кольцо торговца', "Merchant's Ring", 'ring', 'ring', ('gold', 'sapphire'), 8, {'sell_price': 10})
j('ring_spark', 'Кольцо искры', 'Spark Ring', 'ring', 'ring', ('silver', 'sapphire'), 9, {'mana_regen': 10})
j('ring_goblinbane', 'Кольцо гоблинобоя', 'Goblinbane Ring', 'ring', 'ring', ('iron', 'green'), 8, {'dmg_goblin': 20})
j('ring_guard', 'Кольцо стража', "Guard's Ring", 'ring', 'ring', ('iron', 'ruby'), 10, {'health': 6})
j('ring_brute', 'Кольцо громилы', "Brute's Ring", 'ring', 'ring', ('orcish', 'blood'), 12, {'damage': 1.5})
j('ring_sage', 'Кольцо мудреца', "Sage's Ring", 'ring', 'ring', ('silver', 'amethyst'), 12, {'intelligence': 2})
j('ring_strongman', 'Кольцо силача', "Strongman's Ring", 'ring', 'ring', ('copper', 'ruby'), 12, {'strength': 2})
j('ring_witchhunter', 'Кольцо охотника на ведьм', "Witch Hunter's Ring", 'ring', 'ring', ('silver', 'pearl'), 14, {'dmg_illager': 15, 'res_magic': 5})
j('ring_flame', 'Огненное кольцо', 'Flame Ring', 'ring', 'ring', ('gold', 'fire'), 16, {'res_fire': 15}, 'tears_of_flame')
j('ring_feather', 'Кольцо пёрышка', 'Feather Ring', 'ring', 'ring', ('white', 'glass'), 10, {'res_fall': 20})
j('ring_vampire', 'Кольцо вампира', 'Vampire Ring', 'ring', 'ring', ('obsidian', 'blood'), 18, {'lifesteal': 2})
j('ring_luck', 'Кольцо удачи', 'Ring of Luck', 'ring', 'ring', ('gold', 'emerald'), 15, {'luck': 3}, 'gifts_of_fortune')
j('ring_miner', 'Кольцо шахтёра', "Miner's Ring", 'ring', 'ring', ('iron', 'topaz'), 6, {'mining': 10})
j('ring_storm', 'Кольцо бури', 'Storm Ring', 'ring', 'ring', ('mithril', 'sapphire'), 22, {'crit_chance': 3})
j('ring_netherforged', 'Кольцо Преисподней', 'Netherforged Ring', 'ring', 'ring', ('obsidian', 'fire'), 25, {'dmg_nether': 15, 'res_fire': 5})
j('ring_bounty', 'Кольцо охотника за головами', "Bounty Hunter's Ring", 'ring', 'ring', ('black', 'blood'), 28, {'dmg_elite': 8})
# --- амулеты (18) ---
j('amulet_wooden', 'Деревянный оберег', 'Wooden Charm Amulet', 'necklace', 'amulet', ('wood', 'green'), 1, {'health': 2})
j('amulet_fang', 'Амулет из клыка', 'Fang Amulet', 'necklace', 'amulet', ('bone', 'bone'), 4, {'crit_damage': 5}, 'hunter_trophies')
j('amulet_light', 'Амулет света', 'Amulet of Light', 'necklace', 'amulet', ('gold', 'pearl'), 8, {'dmg_undead': 10, 'res_undead': 5}, 'deadmans_legacy')
j('amulet_apprentice', 'Амулет ученика', "Apprentice's Amulet", 'necklace', 'amulet', ('copper', 'sapphire'), 5, {'mana': 15})
j('amulet_traveler', 'Амулет путника', "Traveler's Amulet", 'necklace', 'amulet', ('leather', 'topaz'), 6, {'move_speed': 3})
j('amulet_wind', 'Медальон ветра', 'Wind Medallion', 'necklace', 'amulet', ('silver', 'ice'), 10, {'agility': 2})
j('amulet_stone', 'Медальон камня', 'Stone Medallion', 'necklace', 'amulet', ('stone', 'onyx'), 10, {'vitality': 2})
j('amulet_flame', 'Амулет пламени', 'Flame Amulet', 'necklace', 'amulet', ('gold', 'fire'), 14, {'spell_power': 6}, 'tears_of_flame')
j('amulet_trapper', 'Амулет зверолова', "Trapper's Amulet", 'necklace', 'amulet', ('leather', 'bone'), 9, {'dmg_beast': 12, 'xp': 3}, 'hunter_trophies')
j('amulet_leech', 'Амулет пиявки', 'Leech Amulet', 'necklace', 'amulet', ('obsidian', 'blood'), 15, {'health_regen': 0.5})
j('amulet_scholar', 'Амулет учёного', "Scholar's Amulet", 'necklace', 'amulet', ('silver', 'amethyst'), 12, {'xp': 6})
j('amulet_plague', 'Чумной амулет', 'Plague Amulet', 'necklace', 'amulet', ('bone', 'green'), 13, {'res_arthropod': 10, 'res_undead': 5})
j('amulet_shield', 'Амулет щита', 'Shield Amulet', 'necklace', 'amulet', ('iron', 'sapphire'), 16, {'armor': 2})
j('amulet_manakeeper', 'Амулет хранителя маны', "Manakeeper's Amulet", 'necklace', 'amulet', ('mithril', 'amethyst'), 18, {'mana': 30, 'mana_cost': 3})
j('amulet_warlord', 'Амулет полководца', "Warlord's Amulet", 'necklace', 'amulet', ('gold', 'ruby'), 22, {'strength': 2, 'vitality': 2})
j('amulet_shadow', 'Амулет тени', 'Shadow Amulet', 'necklace', 'amulet', ('black', 'onyx'), 24, {'dodge': 3, 'crit_chance': 2}, 'assassins_night')
j('amulet_void', 'Амулет пустоты', 'Void Amulet', 'necklace', 'amulet', ('obsidian', 'void'), 30, {'res_magic': 10, 'spell_power': 8})
j('amulet_firstlight', 'Амулет первого света', 'Amulet of First Light', 'necklace', 'amulet', ('gold', 'star'), 35, {'luck': 2, 'intelligence': 2, 'health': 10}, minr='rare')
# --- пояса (12) ---
j('belt_rope', 'Верёвочный пояс', 'Rope Belt', 'belt', 'belt', ('brown', 'topaz'), 1, {'health': 1})
j('belt_apprentice', 'Пояс подмастерья', "Apprentice's Belt", 'belt', 'belt', ('leather', 'topaz'), 3, {'mining': 5})
j('belt_crafter', 'Пояс ремесленника', "Crafter's Belt", 'belt', 'belt', ('leather', 'gold'), 6, {'sell_price': 6})
j('belt_hunter', 'Пояс охотника', "Hunter's Belt", 'belt', 'belt', ('leather', 'bone'), 6, {'arrow_damage': 5}, 'hunter_gear')
j('belt_wide', 'Широкий кожаный пояс', 'Wide Leather Belt', 'belt', 'belt', ('brown', 'onyx'), 8, {'armor': 1})
j('belt_runner', 'Пояс бегуна', "Runner's Belt", 'belt', 'belt', ('green', 'gold'), 10, {'move_speed': 4})
j('belt_alchemist', 'Пояс зельевара', "Alchemist's Belt", 'belt', 'belt', ('purple', 'emerald'), 12, {'mana_regen': 8})
j('belt_warrior', 'Пояс воителя', "Warrior's Belt", 'belt', 'belt', ('red', 'gold'), 14, {'health': 8}, 'knights_oath')
j('belt_flasks', 'Пояс с флягами', 'Belt of Flasks', 'belt', 'belt', ('leather', 'ruby'), 15, {'health_regen': 0.4})
j('belt_climber', 'Пояс скалолаза', "Climber's Belt", 'belt', 'belt', ('brown', 'iron' if False else 'onyx'), 12, {'res_fall': 20})
j('belt_treasure', 'Пояс кладоискателя', "Treasure Hunter's Belt", 'belt', 'belt', ('leather', 'gold'), 18, {'loot': 6, 'coin_find': 8}, 'gifts_of_fortune')
j('belt_titan', 'Пояс титана', "Titan's Belt", 'belt', 'belt', ('black', 'ruby'), 28, {'strength': 3, 'health': 10}, minr='uncommon')
# --- браслеты (14) ---
j('bracelet_woven', 'Плетёный браслет', 'Woven Bracelet', 'bracelet', 'bracelet', ('green', 'topaz'), 1, {'luck': 1})
j('bracelet_copper', 'Медный браслет', 'Copper Bracelet', 'bracelet', 'bracelet', ('copper', 'emerald'), 3, {'attack_speed': 2})
j('bracelet_bone', 'Костяной браслет', 'Bone Bracelet', 'bracelet', 'bracelet', ('bone', 'onyx'), 6, {'dmg_undead': 8}, 'deadmans_legacy')
j('bracelet_archer', 'Браслет лучника', "Archer's Bracer", 'bracelet', 'bracelet', ('leather', 'emerald'), 5, {'arrow_damage': 6}, 'hunter_gear')
j('bracelet_scale', 'Браслет из чешуи', 'Scale Bracelet', 'bracelet', 'bracelet', ('ender', 'ender'), 9, {'res_projectile': 8})
j('bracelet_might', 'Браслет силы', 'Bracelet of Might', 'bracelet', 'bracelet', ('iron', 'ruby'), 10, {'damage': 1})
j('bracelet_focus', 'Браслет концентрации', 'Bracelet of Focus', 'bracelet', 'bracelet', ('silver', 'sapphire'), 10, {'cooldown': 3})
j('bracelet_mage', 'Браслет мага', "Mage's Bracelet", 'bracelet', 'bracelet', ('purple', 'amethyst'), 12, {'spell_power': 5}, 'archmage_vestments')
j('bracelet_spiked', 'Шипастый браслет', 'Spiked Bracelet', 'bracelet', 'bracelet', ('black', 'blood'), 12, {'crit_damage': 8})
j('bracelet_fangs', 'Браслет из клыков', 'Fang Bracelet', 'bracelet', 'bracelet', ('bone', 'bone'), 14, {'dmg_beast': 10, 'dmg_goblin': 10}, 'hunter_trophies')
j('bracelet_blood', 'Браслет крови', 'Blood Bracelet', 'bracelet', 'bracelet', ('red', 'blood'), 18, {'lifesteal': 1.5})
j('bracelet_obsidian', 'Обсидиановый браслет', 'Obsidian Bracelet', 'bracelet', 'bracelet', ('obsidian', 'fire'), 20, {'res_explosion': 15})
j('bracelet_silver', 'Серебряный браслет', 'Silver Bracelet', 'bracelet', 'bracelet', ('silver', 'pearl'), 16, {'res_undead': 10})
j('bracelet_storm', 'Браслет бури', 'Storm Bracelet', 'bracelet', 'bracelet', ('mithril', 'ice'), 26, {'attack_speed': 5})
# --- талисманы (24) ---
j('charm_rabbit', 'Кроличья лапка', "Rabbit's Foot", 'charm', 'rabbit_foot', ('white', 'pearl', 'brown'), 1, {'luck': 2}, 'gifts_of_fortune')
j('charm_horseshoe', 'Подкова', 'Horseshoe', 'charm', 'horseshoe', ('iron', 'onyx'), 2, {'move_speed': 2}, 'gifts_of_fortune')
j('charm_coin', 'Счастливая монета', 'Lucky Coin', 'charm', 'coin', ('gold', 'gold'), 3, {'coin_find': 10})
j('charm_wolf_fang', 'Волчий клык', 'Wolf Fang', 'charm', 'fang', ('bone', 'bone'), 4, {'dmg_beast': 10}, 'hunter_trophies')
j('charm_bear_claw', 'Медвежий коготь', 'Bear Claw', 'charm', 'claw', ('bone', 'bone'), 6, {'damage': 1})
j('charm_hawk_feather', 'Перо ястреба', 'Hawk Feather', 'charm', 'feather', ('brown', 'topaz', 'wood'), 5, {'res_fall': 15})
j('charm_spider_eye', 'Паучий глаз', 'Spider Eye Charm', 'charm', 'eye', ('red', 'blood'), 6, {'dmg_arthropod': 12})
j('charm_skull', 'Череп мертвеца', "Dead Man's Skull", 'charm', 'skull', ('bone', 'blood'), 8, {'dmg_undead': 12}, 'deadmans_legacy')
j('charm_goblin_idol', 'Гоблинский идол', 'Goblin Idol', 'charm', 'idol', ('green', 'topaz'), 8, {'dmg_goblin': 15})
j('charm_rune_guard', 'Руна защиты', 'Rune of Guarding', 'charm', 'rune', ('stone', 'sapphire'), 10, {'armor': 1.5}, 'elemental_runes')
j('charm_rune_might', 'Руна силы', 'Rune of Might', 'charm', 'rune', ('stone', 'ruby'), 10, {'strength': 2}, 'elemental_runes')
j('charm_rune_mind', 'Руна разума', 'Rune of Mind', 'charm', 'rune', ('stone', 'amethyst'), 10, {'intelligence': 2}, 'elemental_runes')
j('charm_rune_wind', 'Руна ветра', 'Rune of Wind', 'charm', 'rune', ('stone', 'emerald'), 10, {'agility': 2}, 'elemental_runes')
j('charm_totem', 'Тотем предков', 'Ancestral Totem', 'charm', 'totem', ('wood', 'ruby', 'leather'), 14, {'health': 6})
j('charm_bell', 'Колокольчик', 'Little Bell', 'charm', 'bell', ('gold', 'ruby'), 9, {'xp': 4})
j('charm_acorn', 'Желудь друида', "Druid's Acorn", 'charm', 'acorn', ('wood', 'green', 'brown'), 7, {'health_regen': 0.3})
j('charm_shell', 'Морская ракушка', 'Sea Shell', 'charm', 'shell', ('white', 'pearl'), 6, {'mana_regen': 6})
j('charm_fire_crystal', 'Огненный кристалл', 'Fire Crystal', 'charm', 'crystal', ('fire', 'fire'), 16, {'res_fire': 12}, 'tears_of_flame')
j('charm_ice_crystal', 'Ледяной кристалл', 'Ice Crystal', 'charm', 'crystal', ('ice', 'ice'), 16, {'spell_power': 5})
j('charm_phoenix_feather', 'Перо феникса', 'Phoenix Feather', 'charm', 'feather', ('fire', 'fire', 'red'), 30, {'res_fire': 15, 'health_regen': 0.8}, minr='rare')
j('charm_ender_eye', 'Око Края', 'Eye of the End', 'charm', 'eye', ('ender', 'ender'), 26, {'dmg_elite': 6})
j('charm_piglin_trinket', 'Безделушка пиглина', 'Piglin Trinket', 'charm', 'idol', ('gold', 'blood'), 20, {'dmg_nether': 12})
j('charm_warden_bone', 'Кость стража', "Warden's Bone", 'charm', 'fang', ('bone', 'blood'), 15, {'res_undead': 8})
j('charm_stardust', 'Звёздная пыль', 'Stardust', 'charm', 'crystal', ('white', 'star'), 32, {'luck': 3, 'crit_chance': 2}, minr='uncommon')
# --- плащи (10) ---
j('cloak_travel', 'Дорожный плащ', 'Travel Cloak', 'back', 'cloak', ('brown', 'iron' if False else 'onyx'), 2, {'move_speed': 2})
j('cloak_hunter', 'Плащ охотника', "Hunter's Cloak", 'back', 'cloak', ('green', 'bone'), 6, {'arrow_damage': 5, 'dodge': 1}, 'hunter_gear')
j('cloak_student', 'Плащ ученика', "Student's Cloak", 'back', 'cloak', ('blue', 'sapphire'), 6, {'mana': 15}, 'archmage_vestments')
j('cloak_wolfhide', 'Плащ из волчьей шкуры', 'Wolfhide Cloak', 'back', 'cloak', ('feather', 'bone'), 9, {'health': 4, 'dmg_beast': 6}, 'hunter_trophies')
j('cloak_guard', 'Плащ стражника', "Guardsman's Cloak", 'back', 'cloak', ('red', 'gold'), 12, {'armor': 1.5}, 'knights_oath')
j('cloak_shadow', 'Плащ тени', 'Shadow Cloak', 'back', 'cloak', ('black', 'onyx'), 18, {'dodge': 3}, 'assassins_night')
j('cloak_fireproof', 'Огнеупорный плащ', 'Fireproof Cloak', 'back', 'cloak', ('fire', 'fire'), 16, {'res_fire': 15}, 'tears_of_flame')
j('cloak_paladin', 'Плащ паладина', "Paladin's Cloak", 'back', 'cloak', ('white', 'gold'), 22, {'res_undead': 10, 'health': 6})
j('cloak_archmage', 'Мантия архимага', "Archmage's Mantle", 'back', 'cloak', ('purple', 'star'), 30, {'spell_power': 10, 'mana': 30}, 'archmage_vestments', 'rare')
j('cloak_night', 'Плащ ночи', 'Cloak of Night', 'back', 'cloak', ('obsidian', 'void'), 34, {'crit_chance': 4, 'dodge': 2}, 'assassins_night', 'rare')
# --- перчатки (10) ---
j('gloves_work', 'Рабочие перчатки', 'Work Gloves', 'hands', 'gloves', ('brown', 'topaz', 'leather'), 1, {'mining': 6})
j('gloves_leather', 'Кожаные перчатки', 'Leather Gloves', 'hands', 'gloves', ('leather', 'onyx', 'brown'), 3, {'attack_speed': 2})
j('gloves_archer', 'Перчатки лучника', "Archer's Gloves", 'hands', 'gloves', ('green', 'emerald', 'leather'), 5, {'arrow_damage': 6}, 'hunter_gear')
j('gloves_thief', 'Перчатки вора', "Thief's Gloves", 'hands', 'gloves', ('black', 'gold', 'leather'), 8, {'loot': 4, 'coin_find': 6})
j('gloves_plate', 'Латные рукавицы', 'Plate Gauntlets', 'hands', 'gloves', ('iron', 'ruby', 'iron'), 12, {'armor': 1, 'damage': 0.5}, 'knights_oath')
j('gloves_miner', 'Перчатки шахтёра', "Miner's Gloves", 'hands', 'gloves', ('dwarven', 'topaz', 'leather'), 10, {'mining': 15})
j('gloves_mage', 'Перчатки мага', "Mage's Gloves", 'hands', 'gloves', ('purple', 'amethyst', 'purple'), 12, {'cooldown': 4}, 'archmage_vestments')
j('gloves_claw', 'Когтистые перчатки', 'Clawed Gloves', 'hands', 'gloves', ('beast', 'bone', 'leather'), 16, {'crit_damage': 10})
j('gloves_smith', 'Перчатки кузнеца', "Blacksmith's Gloves", 'hands', 'gloves', ('red', 'fire', 'leather'), 14, {'res_fire': 8, 'mining': 8})
j('gloves_assassin', 'Перчатки убийцы', "Assassin's Gloves", 'hands', 'gloves', ('obsidian', 'blood', 'black'), 28, {'crit_chance': 4, 'dmg_humanoid': 8}, 'assassins_night', 'uncommon')

# --- наборы брони (8 × 4) ---
ARMOR_SETS = [
    # id, рус. прилагательное (м/ж/мн), англ., материал, палитра (1,2,3,4,5), уровень, набор, врождённое на предмет, мин.
    ('hunter', ('Охотничий', 'Охотничьи', 'Охотничьи'), 'Hunter', 'hunter', (0x8CB070, 0x5A8040, 0x34502A, 0xC8A060, 0x6A4A20), 6, 'hunter_gear', {'arrow_damage': 2}, None),
    ('knight', ('Рыцарский', 'Рыцарские', 'Рыцарские'), 'Knight', 'knight', (0xE8ECF0, 0xA8B0B8, 0x5A6470, 0xC03A3A, 0x7A1A1A), 12, 'knights_oath', {'armor': 0.5}, None),
    ('mage', ('Магический', 'Магические', 'Магические'), 'Magus', 'magus', (0xA080E0, 0x6040A0, 0x302060, 0xF2C33A, 0x9A6A12), 10, 'archmage_vestments', {'mana': 8}, None),
    ('elven', ('Эльфийский', 'Эльфийские', 'Эльфийские'), 'Elven', 'elven', (0xE0F4C8, 0x8CC870, 0x4A7A30, 0xE8ECF0, 0x8A94A0), 18, 'elven_grace', {'dodge': 1}, 'uncommon'),
    ('dwarven', ('Дварфийский', 'Дварфийские', 'Дварфийские'), 'Dwarven', 'dwarven', (0xE8D098, 0xB08A4A, 0x6A4A20, 0x5A6470, 0x2A2A34), 18, 'dwarven_bulwark', {'armor': 1}, 'uncommon'),
    ('orcish', ('Орочий', 'Орочьи', 'Орочьи'), 'Orcish', 'orcish', (0xA8988A, 0x6A5A4A, 0x3A2E24, 0xC03A3A, 0x6A1010), 16, 'orcish_fury', {'damage': 0.5}, 'uncommon'),
    ('beast', ('Звериный', 'Звериные', 'Звериные'), 'Beasthide', 'beasthide', (0xD0AA70, 0x8E6E3B, 0x5A4220, 0xF4EEDC, 0x8A7F62), 16, 'beast_instinct', {'move_speed': 1}, 'uncommon'),
    ('demonic', ('Демонический', 'Демонические', 'Демонические'), 'Demonic', 'demonic', (0xF07050, 0xA02818, 0x501008, 0x2A2A34, 0xFF9020), 26, 'infernal_pact', {'res_fire': 4}, 'rare'),
]
MATERIALS = [
    dict(id='hunter', durability=12, defense=[1, 4, 3, 1], enchant=15, toughness=0, knockback=0, repair='minecraft:leather', sound='leather'),
    dict(id='knight', durability=25, defense=[2, 6, 5, 2], enchant=10, toughness=1, knockback=0, repair='minecraft:iron_ingot', sound='iron'),
    dict(id='magus', durability=10, defense=[1, 3, 2, 1], enchant=25, toughness=0, knockback=0, repair='minecraft:purple_wool', sound='leather'),
    dict(id='elven', durability=22, defense=[2, 5, 5, 2], enchant=20, toughness=1, knockback=0, repair='litrpg:elf_token', sound='chain'),
    dict(id='dwarven', durability=33, defense=[3, 7, 6, 3], enchant=10, toughness=2, knockback=0.1, repair='litrpg:dwarf_token', sound='iron'),
    dict(id='orcish', durability=28, defense=[3, 7, 6, 2], enchant=8, toughness=1, knockback=0.05, repair='litrpg:orc_token', sound='iron'),
    dict(id='beasthide', durability=22, defense=[2, 5, 5, 2], enchant=15, toughness=0.5, knockback=0, repair='litrpg:beast_token', sound='leather'),
    dict(id='demonic', durability=35, defense=[3, 7, 6, 3], enchant=18, toughness=2.5, knockback=0.05, repair='litrpg:demon_token', sound='netherite'),
]
PIECES = [('helmet', 'шлем', 'Helmet', 0), ('chestplate', 'нагрудник', 'Chestplate', 0), ('leggings', 'поножи', 'Leggings', 1), ('boots', 'сапоги', 'Boots', 2)]

# --- оружие рас и прочее (12) ---
WEAPONS = [
    # id, рус., англ., вид, форма, палитра, уровень, урон, скорость, врождённое, набор, мин.
    ('elven_blade', 'Эльфийский клинок', 'Elven Blade', 'sword', 'sword', ('elven', 'emerald', 'wood'), 18, 3, -2.2, {'agility': 2}, 'elven_grace', 'uncommon'),
    ('elven_staff', 'Посох лесной магии', 'Sylvan Staff', 'staff', 'staff', ('elven', 'green', 'wood'), 18, 0, 0, {'mana_regen': 10}, 'elven_grace', 'uncommon'),
    ('dwarven_axe', 'Дварфийский топор', 'Dwarven Axe', 'axe', 'axe', ('dwarven', 'topaz', 'wood'), 18, 7, -3.0, {'mining': 15}, 'dwarven_bulwark', 'uncommon'),
    ('dwarven_hammer', 'Рунный молот', 'Rune Hammer', 'sword', 'mace', ('dwarven', 'sapphire', 'brown'), 22, 6, -3.0, {'dmg_goblin': 15}, 'dwarven_bulwark', 'uncommon'),
    ('orc_cleaver', 'Орочий тесак', 'Orc Cleaver', 'axe', 'axe', ('orcish', 'blood', 'leather'), 16, 8, -3.2, {'damage': 1}, 'orcish_fury', 'uncommon'),
    ('orc_club', 'Шипастая дубина', 'Spiked Club', 'sword', 'mace', ('orcish', 'bone', 'wood'), 14, 5, -2.8, {'crit_damage': 6}, 'orcish_fury', None),
    ('beast_spear', 'Копьё охотника', "Hunter's Spear", 'sword', 'spear', ('beast', 'bone', 'wood'), 16, 4, -2.6, {'dmg_beast': 15}, 'beast_instinct', 'uncommon'),
    ('beast_claws', 'Звериные когти', 'Beast Claws', 'dagger', 'dagger', ('bone', 'blood', 'beast'), 16, 2, 0, {'crit_chance': 2}, 'beast_instinct', 'uncommon'),
    ('demon_blade', 'Клинок инферно', 'Inferno Blade', 'sword', 'sword', ('demonic', 'fire', 'black'), 26, 5, -2.4, {'res_fire': 5}, 'infernal_pact', 'rare'),
    ('demon_staff', 'Посох серы', 'Brimstone Staff', 'staff', 'staff', ('demonic', 'fire', 'black'), 26, 0, 0, {'spell_power': 6}, 'infernal_pact', 'rare'),
    ('knight_sword', 'Рыцарский меч', "Knight's Sword", 'sword', 'sword', ('iron', 'ruby', 'leather'), 12, 4, -2.4, {'health': 3}, 'knights_oath', None),
    ('hunter_dagger', 'Нож охотника', "Hunter's Knife", 'dagger', 'dagger', ('iron', 'bone', 'leather'), 6, 1, 0, {'dmg_beast': 8}, 'hunter_gear', None),
]

# --- трофеи боссов подземелий: падают только с них (набор «Регалии Покорителя») ---
BOSS_LOOT = [
    # id, рус., англ., вид, форма, палитра, уровень, урон, скорость, врождённое, босс, пассивка
    ('golem_core', 'Сердце Голема', 'Golem Core', 'charm', 'crystal', ('stone', 'sapphire'), 8, 0, 0, {'armor': 2, 'vitality': 2}, 'ruin_golem', None),
    ('golem_fist', 'Кулак Руин', 'Fist of the Ruins', 'sword', 'mace', ('stone', 'sapphire', 'stone'), 8, 6, -3.0, {'damage': 1, 'res_explosion': 10}, 'ruin_golem', None),
    ('ent_heartwood', 'Сердцевина Энта', 'Ent Heartwood', 'necklace', 'amulet', ('wood', 'green'), 11, 0, 0, {'health': 8, 'health_regen': 0.6}, 'rotting_ent', 'regeneration'),
    ('ent_staff', 'Посох Гнилого Корня', 'Rotroot Staff', 'staff', 'staff', ('wood', 'green', 'brown'), 11, 0, 0, {'spell_power': 6, 'mana_regen': 10}, 'rotting_ent', None),
    ('arachna_fang', 'Клык Арахны', "Arachna's Fang", 'dagger', 'dagger', ('black', 'green', 'bone'), 14, 2, 0, {'crit_chance': 3, 'dmg_arthropod': 20}, 'spider_queen', 'venom'),
    ('arachna_cloak', 'Шёлковый плащ Королевы', "Queen's Silk Cloak", 'back', 'cloak', ('white', 'emerald'), 14, 0, 0, {'dodge': 4, 'res_arthropod': 15}, 'spider_queen', None),
    ('morra_ring', 'Кольцо Проклятой Топи', 'Ring of the Cursed Mire', 'ring', 'ring', ('obsidian', 'green'), 20, 0, 0, {'spell_power': 8, 'mana': 20}, 'swamp_hag', None),
    ('morra_staff', 'Клюка Морры', "Morra's Crook", 'staff', 'staff', ('green', 'void', 'wood'), 20, 0, 0, {'spell_power': 10, 'cooldown': 4}, 'swamp_hag', None),
    ('minotaur_axe', 'Секира Астериона', "Asterion's Greataxe", 'axe', 'axe', ('iron', 'blood', 'brown'), 25, 9, -3.2, {'damage': 2, 'crit_damage': 15}, 'minotaur', 'berserker'),
    ('minotaur_horn', 'Рог Минотавра', 'Minotaur Horn', 'charm', 'fang', ('bone', 'blood'), 25, 0, 0, {'strength': 3, 'res_projectile': 10}, 'minotaur', None),
    ('ymir_heart', 'Вечный Лёд Имира', "Ymir's Eternal Ice", 'charm', 'crystal', ('ice', 'ice'), 38, 0, 0, {'vitality': 4, 'health': 15, 'res_fire': 10}, 'frost_giant', None),
    ('ymir_maul', 'Молот Вечной Зимы', 'Maul of Endless Winter', 'sword', 'mace', ('ice', 'ice', 'iron'), 38, 9, -3.0, {'damage': 3, 'crit_damage': 20}, 'frost_giant', 'frost_touch'),
    ('ignis_ember', 'Кольцо Живого Пламени', 'Ring of Living Flame', 'ring', 'ring', ('fire', 'fire'), 45, 0, 0, {'spell_power': 14, 'res_fire': 20}, 'flame_lord', 'fireproof'),
    ('ignis_blade', 'Клинок Горнила', 'Crucible Blade', 'sword', 'sword', ('fire', 'fire', 'black'), 45, 7, -2.4, {'damage': 3, 'crit_chance': 4}, 'flame_lord', 'flame_touch'),
    ('baalzar_horn', "Венец Баал'Зара", "Baal'zar's Crown", 'necklace', 'amulet', ('demonic', 'void'), 55, 0, 0, {'strength': 4, 'intelligence': 4, 'lifesteal': 3}, 'archdemon', 'phoenix'),
    ('baalzar_blade', 'Пожиратель Миров', 'Worldeater', 'sword', 'sword', ('obsidian', 'void', 'demonic'), 55, 9, -2.4, {'damage': 4, 'lifesteal': 4, 'crit_damage': 25}, 'archdemon', 'soul_eater'),
]

# --- наборы (эффекты за количество разных предметов) ---
SETS = [
    dict(id='hunter_gear', title='Снаряжение охотника', tiers=[
        dict(pieces=2, mods={'arrow_damage': 8}), dict(pieces=4, mods={'agility': 3, 'crit_chance': 4}),
        dict(pieces=6, mods={'move_speed': 6}, passive='swiftness')]),
    dict(id='knights_oath', title='Клятва рыцаря', tiers=[
        dict(pieces=2, mods={'armor': 3}), dict(pieces=4, mods={'health': 12, 'res_projectile': 10}),
        dict(pieces=6, mods={'vitality': 4}, passive='thorns')]),
    dict(id='archmage_vestments', title='Облачение архимага', tiers=[
        dict(pieces=2, mods={'mana': 30}), dict(pieces=4, mods={'spell_power': 12, 'cooldown': 6}),
        dict(pieces=6, mods={'intelligence': 5}, passive='arcane_echo')]),
    dict(id='elven_grace', title='Эльфийская грация', tiers=[
        dict(pieces=2, mods={'dodge': 4}), dict(pieces=4, mods={'agility': 4, 'arrow_damage': 10}),
        dict(pieces=6, mods={'crit_chance': 6}, passive='night_eye')]),
    dict(id='dwarven_bulwark', title='Дварфийский бастион', tiers=[
        dict(pieces=2, mods={'mining': 15}), dict(pieces=4, mods={'vitality': 4, 'armor': 4, 'res_explosion': 20}),
        dict(pieces=6, mods={'health': 20, 'dmg_goblin': 20})]),
    dict(id='orcish_fury', title='Орочья ярость', tiers=[
        dict(pieces=2, mods={'damage': 2}), dict(pieces=4, mods={'strength': 4}, passive='berserker'),
        dict(pieces=5, mods={'lifesteal': 3})]),
    dict(id='beast_instinct', title='Звериный инстинкт', tiers=[
        dict(pieces=2, mods={'move_speed': 6}), dict(pieces=4, mods={'crit_chance': 6, 'dmg_beast': 20}),
        dict(pieces=6, mods={'agility': 4}, passive='featherfall')]),
    dict(id='infernal_pact', title='Адский договор', tiers=[
        dict(pieces=2, mods={'res_fire': 25}), dict(pieces=4, mods={'spell_power': 15}, passive='fireproof'),
        dict(pieces=6, mods={'dmg_nether': 25, 'intelligence': 4})]),
    dict(id='deadmans_legacy', title='Наследие мертвеца', tiers=[
        dict(pieces=2, mods={'dmg_undead': 10}), dict(pieces=4, mods={'res_undead': 15}, passive='night_eye')]),
    dict(id='gifts_of_fortune', title='Дары удачи', tiers=[
        dict(pieces=2, mods={'luck': 2}), dict(pieces=3, mods={'coin_find': 15}),
        dict(pieces=5, mods={'loot': 10}, passive='treasure_hunter')]),
    dict(id='elemental_runes', title='Руны стихий', tiers=[
        dict(pieces=2, mods={'health': 6}), dict(pieces=4, mods={'strength': 2, 'agility': 2, 'intelligence': 2, 'vitality': 2})]),
    dict(id='hunter_trophies', title='Трофеи зверолова', tiers=[
        dict(pieces=2, mods={'dmg_beast': 10}), dict(pieces=4, mods={'crit_damage': 12, 'xp': 5}),
        dict(pieces=6, mods={'luck': 3})]),
    dict(id='tears_of_flame', title='Слёзы пламени', tiers=[
        dict(pieces=2, mods={'res_fire': 15}), dict(pieces=4, mods={'spell_power': 8}, passive='fireproof')]),
    dict(id='conqueror', title='Регалии Покорителя', tiers=[
        dict(pieces=2, mods={'health': 10, 'dmg_elite': 10}), dict(pieces=4, mods={'strength': 3, 'agility': 3, 'vitality': 3, 'intelligence': 3}),
        dict(pieces=6, mods={'xp': 10, 'loot': 10, 'crit_chance': 5}, passive='giant_slayer')]),
    dict(id='assassins_night', title='Ночь убийцы', tiers=[
        dict(pieces=2, mods={'dodge': 3}), dict(pieces=4, mods={'crit_damage': 20}, passive='executioner')]),
]

# жетоны рас и монеты (обычные предметы, регистрируются в Java)
TOKENS = [
    ('elf_token', 'Лунный лист', 'Moonleaf', 'leaf', ('leafgreen', 'pearl', 'wood')),
    ('dwarf_token', 'Рунная сталь', 'Runesteel', 'rune', ('dwarven', 'sapphire')),
    ('orc_token', 'Орочий трофей', 'Orc Trophy', 'fang', ('bone', 'blood')),
    ('beast_token', 'Шерсть альфы', 'Alpha Fur', 'rabbit_foot', ('beast', 'bone', 'brown')),
    ('demon_token', 'Адская сера', 'Brimstone', 'crystal', ('fire', 'fire')),
    ('copper_coin', 'Медная монета', 'Copper Coin', 'coin', ('copper', 'topaz')),
    ('silver_coin', 'Серебряная монета', 'Silver Coin', 'coin', ('silver', 'pearl')),
    ('gold_coin', 'Золотая монета', 'Gold Coin', 'coin', ('gold', 'gold')),
]

CURIO_TAG = {'ring': 'ring', 'necklace': 'necklace', 'belt': 'belt', 'bracelet': 'bracelet', 'charm': 'charm', 'back': 'back', 'hands': 'hands'}


def main():
    items_json, sets_json = [], []
    ru = json.load(open(os.path.join(ASSETS, 'lang', 'ru_ru.json'), encoding='utf-8'))
    en = json.load(open(os.path.join(ASSETS, 'lang', 'en_us.json'), encoding='utf-8'))
    preview = {}
    tags = {v: [] for v in CURIO_TAG.values()}
    tex_dir = os.path.join(ASSETS, 'textures', 'item')
    model_dir = os.path.join(ASSETS, 'models', 'item')

    def model(id, handheld=False):
        json.dump({"parent": "minecraft:item/" + ("handheld" if handheld else "generated"),
                   "textures": {"layer0": "litrpg:item/" + id}}, open(os.path.join(model_dir, id + '.json'), 'w'))

    def texture(id, img):
        write_png(os.path.join(tex_dir, id + '.png'), img, 16, 16)
        preview[id] = img

    # украшения
    for it in J:
        p = it['pal']
        pal = palette(p[0], p[1] if len(p) > 1 else 'ruby', p[2] if len(p) > 2 else 'leather')
        texture(it['id'], render(it['shape'], pal))
        model(it['id'])
        ru['item.litrpg.' + it['id']] = it['ru']
        en['item.litrpg.' + it['id']] = it['en']
        tags[CURIO_TAG[it['kind']]].append('litrpg:' + it['id'])
        e = dict(id=it['id'], kind=it['kind'], tier=it['tier'], implicit=it['implicit'])
        if it['set']: e['set'] = it['set']
        if it['min']: e['min'] = it['min']
        items_json.append(e)

    # броня наборов
    for sid, (adj_m, adj_n, adj_pl), en_adj, mat, (c1, c2, c3, c4, c5), tier, set_id, implicit, minr in ARMOR_SETS:
        pal = {'1': c1, '2': c2, '3': c3, '4': c4, '5': c5, 'K': 0x1A1020, 'W': 0xFFFFFF}
        for piece, ru_piece, en_piece, form in PIECES:
            id = f'{sid}_{piece}'
            tmpl = gx.TEMPLATES[piece]
            img = [[(0, 0, 0, 0) if c == '.' else hex_rgba(pal.get(c, c2)) for c in r] for r in tmpl]
            texture(id, img)
            model(id)
            adj = adj_m if form == 0 else adj_pl
            ru['item.litrpg.' + id] = f'{adj} {ru_piece}'
            en['item.litrpg.' + id] = f'{en_adj} {en_piece}'
            e = dict(id=id, kind=piece, tier=tier, material=mat, implicit=implicit, set=set_id)
            if minr: e['min'] = minr
            items_json.append(e)
        # текстуры брони на модели
        apal = {'1': c1, '2': c2, '3': c3, '4': c4, '5': c5}
        l1, l2 = gx.armor_layers(mat, apal)
        armor_dir = os.path.join(ASSETS, 'textures', 'models', 'armor')
        write_png(os.path.join(armor_dir, mat + '_layer_1.png'), l1, 64, 32)
        write_png(os.path.join(armor_dir, mat + '_layer_2.png'), l2, 64, 32)

    # оружие
    for id, rname, ename, kind, shape, p, tier, dmg, speed, implicit, set_id, minr in WEAPONS:
        pal = palette(p[0], p[1], p[2])
        texture(id, render(shape, pal))
        model(id, handheld=True)
        ru['item.litrpg.' + id] = rname
        en['item.litrpg.' + id] = ename
        e = dict(id=id, kind=kind, tier=tier, damage=dmg, speed=speed, implicit=implicit, set=set_id)
        if minr: e['min'] = minr
        items_json.append(e)

    # трофеи боссов
    for id, rname, ename, kind, shape, p, tier, dmg, speed, implicit, boss, passive in BOSS_LOOT:
        pal = palette(p[0], p[1], p[2] if len(p) > 2 else 'leather')
        texture(id, render(shape, pal))
        model(id, handheld=kind in ('sword', 'axe', 'staff', 'dagger'))
        ru['item.litrpg.' + id] = rname
        en['item.litrpg.' + id] = ename
        e = dict(id=id, kind=kind, tier=tier, implicit=implicit, set='conqueror', min='epic', boss=boss)
        if kind in ('sword', 'axe', 'staff', 'dagger'):
            e['damage'] = dmg
            e['speed'] = speed
        if kind in CURIO_TAG:
            tags[CURIO_TAG[kind]].append('litrpg:' + id)
        if passive: e['passive'] = passive
        items_json.append(e)

    # жетоны и монеты
    for id, rname, ename, shape, p in TOKENS:
        pal = palette(p[0], p[1] if len(p) > 1 else 'ruby', p[2] if len(p) > 2 else 'leather')
        texture(id, render(shape, pal))
        model(id)
        ru['item.litrpg.' + id] = rname
        en['item.litrpg.' + id] = ename

    # наборы: список предметов
    for s in SETS:
        members = [i['id'] for i in items_json if i.get('set') == s['id']]
        sets_json.append(dict(id=s['id'], title=s['title'], items=members, tiers=s['tiers']))

    out = dict(items=items_json, sets=sets_json, materials=MATERIALS)
    json.dump(out, open(os.path.join(ROOT, 'litrpg_items.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

    ru['item.litrpg.template_staff.tooltip'] = 'ПКМ — магический снаряд за ману'
    en['item.litrpg.template_staff.tooltip'] = 'Right-click: magic bolt for mana'
    ru['curios.identifier.bracelet'] = 'Браслет'
    ru['curios.identifier.charm'] = 'Талисман'
    ru['curios.identifier.back'] = 'Плащ'
    ru['curios.identifier.hands'] = 'Перчатки'
    json.dump(ru, open(os.path.join(ASSETS, 'lang', 'ru_ru.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(en, open(os.path.join(ASSETS, 'lang', 'en_us.json'), 'w', encoding='utf-8'), ensure_ascii=False, indent=2)

    # теги и слоты Curios
    tag_dir = os.path.join(DATA, 'curios', 'tags', 'items')
    os.makedirs(tag_dir, exist_ok=True)
    base = {'ring': ["litrpg:copper_ring", "litrpg:silver_ring", "litrpg:gold_ring", "litrpg:kings_ring"],
            'necklace': ["litrpg:jade_amulet", "litrpg:ruby_amulet", "litrpg:star_amulet", "litrpg:lich_amulet", "litrpg:heart_of_system"],
            'belt': ["litrpg:leather_belt"]}
    for tag, vals in tags.items():
        json.dump({"replace": False, "values": base.get(tag, []) + vals}, open(os.path.join(tag_dir, tag + '.json'), 'w'), indent=2)
    slot_dir = os.path.join(DATA, 'litrpg', 'curios', 'slots')
    os.makedirs(slot_dir, exist_ok=True)
    for slot, size in (('ring', 2), ('bracelet', 2), ('charm', 2)):
        json.dump({"size": size, "operation": "SET"}, open(os.path.join(slot_dir, slot + '.json'), 'w'))
    ent_dir = os.path.join(DATA, 'litrpg', 'curios', 'entities')
    os.makedirs(ent_dir, exist_ok=True)
    json.dump({"entities": ["player"], "slots": ["ring", "necklace", "belt", "bracelet", "charm", "back", "hands"]},
              open(os.path.join(ent_dir, 'litrpg_player.json'), 'w'), indent=2)

    # рецепты: броня рас из жетонов, простые украшения, обмен монет
    rec = os.path.join(DATA, 'litrpg', 'recipes')

    def shaped(name, pattern, key, count=1, cat='equipment'):
        k = {c: {"item": v} for c, v in key.items()}
        json.dump({"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern, "key": k,
                   "result": {"item": "litrpg:" + name, "count": count}}, open(os.path.join(rec, name + '.json'), 'w'), indent=2)

    def shapeless(name, ingredients, count=1):
        json.dump({"type": "minecraft:crafting_shapeless", "category": "misc",
                   "ingredients": [{"item": i} for i in ingredients],
                   "result": {"item": "litrpg:" + name, "count": count}}, open(os.path.join(rec, name + '.json'), 'w'), indent=2)

    pats = {'helmet': ["XMX", "M M"], 'chestplate': ["M M", "XMX", "MMM"], 'leggings': ["MXM", "M M", "M M"], 'boots': ["X X", "M M"]}
    armor_mats = {'hunter': ('minecraft:rabbit_hide', 'minecraft:leather'), 'knight': ('minecraft:gold_ingot', 'minecraft:iron_ingot'),
                  'mage': ('minecraft:lapis_lazuli', 'minecraft:purple_wool'), 'elven': ('litrpg:elf_token', 'minecraft:iron_ingot'),
                  'dwarven': ('litrpg:dwarf_token', 'minecraft:iron_ingot'), 'orcish': ('litrpg:orc_token', 'minecraft:iron_ingot'),
                  'beast': ('litrpg:beast_token', 'minecraft:leather'), 'demonic': ('litrpg:demon_token', 'minecraft:netherite_scrap')}
    for sid, (x, m) in armor_mats.items():
        for piece, pat in pats.items():
            shaped(f'{sid}_{piece}', pat, {'X': x, 'M': m})
    shaped('ring_novice', [" C ", "C C", " C "], {'C': 'minecraft:copper_ingot'})
    shaped('amulet_wooden', ["SSS", "S S", " P "], {'S': 'minecraft:string', 'P': 'minecraft:oak_planks'})
    shaped('belt_rope', ["SSS", "S S"], {'S': 'minecraft:string'})
    shaped('bracelet_woven', [" S ", "S S", " G "], {'S': 'minecraft:string', 'G': 'minecraft:grass'})
    shaped('charm_horseshoe', ["I I", "I I", " I "], {'I': 'minecraft:iron_ingot'})
    shapeless('charm_rabbit', ['minecraft:rabbit_foot', 'minecraft:string'])
    shaped('gloves_work', ["L L", "L L"], {'L': 'minecraft:leather'})
    shaped('cloak_travel', ["W W", "WWW", "WWW"], {'W': 'minecraft:brown_wool'})
    for rune, gem in (('charm_rune_guard', 'minecraft:lapis_lazuli'), ('charm_rune_might', 'minecraft:redstone'),
                      ('charm_rune_mind', 'minecraft:amethyst_shard'), ('charm_rune_wind', 'minecraft:emerald')):
        shaped(rune, [" S ", "SGS", " S "], {'S': 'minecraft:stone', 'G': gem})
    shapeless('silver_coin', ['litrpg:copper_coin'] * 9)
    shapeless('gold_coin', ['litrpg:silver_coin'] * 9)
    json.dump({"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "litrpg:silver_coin"}],
               "result": {"item": "litrpg:copper_coin", "count": 9}}, open(os.path.join(rec, 'copper_coin_from_silver.json'), 'w'))
    json.dump({"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": "litrpg:gold_coin"}],
               "result": {"item": "litrpg:silver_coin", "count": 9}}, open(os.path.join(rec, 'silver_coin_from_gold.json'), 'w'))

    print(f"templates: {len(items_json)} items, {len(sets_json)} sets, tokens/coins {len(TOKENS)}")

    if '--preview' in sys.argv:
        out_path = sys.argv[sys.argv.index('--preview') + 1]
        names = list(preview)
        cols, cell = 12, 16 * 4 + 4
        rows_n = (len(names) + cols - 1) // cols
        sheet = [[(30, 32, 38, 255)] * (cols * cell) for _ in range(rows_n * cell)]
        for i, n in enumerate(names):
            big = scale(preview[n], 4)
            ox, oy = (i % cols) * cell + 2, (i // cols) * cell + 2
            for y, row in enumerate(big):
                for x, px in enumerate(row):
                    sheet[oy + y][ox + x] = px
        write_png(out_path, sheet, cols * cell, rows_n * cell)


if __name__ == '__main__':
    random.seed(1)
    main()
