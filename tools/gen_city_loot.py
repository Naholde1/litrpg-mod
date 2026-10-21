"""
Лут городских сундуков и бочек: дома, бочки-кладовые, мастерские по ремёслам, арсенал замка.
Пишет data/litrpg/loot_tables/chests/city_*.json. Запуск: python tools/gen_city_loot.py
Каждый такой сундук — чужое имущество: брать можно, но если заметят — наказание (CityLaw).
"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'litrpg', 'loot_tables', 'chests')


def it(name, w, lo=1, hi=1, potion=None):
    e = {'type': 'minecraft:item', 'name': name if ':' in name else 'minecraft:' + name, 'weight': w}
    f = []
    if hi > 1:
        f.append({'function': 'minecraft:set_count', 'count': {'min': lo, 'max': hi}})
    if potion:
        f.append({'function': 'minecraft:set_potion', 'id': 'minecraft:' + potion})
    if f:
        e['functions'] = f
    return e


def table(rolls, entries, coins=(0, 0), empty=0):
    pools = [{'rolls': {'min': rolls[0], 'max': rolls[1]},
              'entries': entries + ([{'type': 'minecraft:empty', 'weight': empty}] if empty else [])}]
    if coins[1]:
        pools.append({'rolls': 1, 'entries': [it('litrpg:copper_coin', 6, coins[0], coins[1]), it('litrpg:silver_coin', 1, 1, 2),
                                              {'type': 'minecraft:empty', 'weight': 4}]})
    return {'type': 'minecraft:chest', 'pools': pools}


T = {
    'city_home': table((2, 4), [it('bread', 8, 1, 3), it('apple', 6, 1, 3), it('cookie', 3, 2, 5), it('carrot', 4, 1, 4), it('baked_potato', 3, 1, 3),
                                it('white_wool', 3, 1, 3), it('string', 3, 1, 4), it('candle', 3, 1, 2), it('torch', 4, 2, 6), it('coal', 3, 1, 4),
                                it('book', 2), it('paper', 2, 1, 4), it('leather', 2, 1, 2), it('shears', 1), it('flower_pot', 1),
                                it('emerald', 1), it('litrpg:mana_potion', 1), it('potion', 1, potion='healing')], coins=(1, 8), empty=4),
    'city_barrel': table((1, 4), [it('wheat', 6, 2, 8), it('potato', 5, 2, 6), it('carrot', 5, 2, 6), it('beetroot', 3, 2, 6), it('apple', 4, 1, 4),
                                  it('cod', 3, 1, 3), it('salmon', 2, 1, 3), it('coal', 4, 2, 6), it('stick', 4, 2, 8), it('oak_planks', 3, 2, 8),
                                  it('leather', 2, 1, 3), it('egg', 2, 1, 4), it('sugar', 2, 1, 4), it('hay_block', 1), it('bone_meal', 2, 1, 4)],
                       coins=(1, 4), empty=6),
    'city_smith': table((3, 5), [it('iron_ingot', 6, 1, 4), it('iron_nugget', 6, 3, 9), it('coal', 6, 2, 8), it('gold_nugget', 3, 2, 6),
                                 it('copper_ingot', 4, 2, 6), it('iron_sword', 1), it('iron_axe', 1), it('iron_pickaxe', 1), it('chainmail_helmet', 1),
                                 it('shield', 1), it('flint', 2, 1, 3), it('bucket', 1)], coins=(3, 12)),
    'city_alchemy': table((3, 5), [it('glass_bottle', 6, 1, 4), it('redstone', 4, 2, 6), it('glowstone_dust', 4, 1, 4), it('nether_wart', 3, 1, 4),
                                   it('spider_eye', 3, 1, 3), it('sugar', 3, 1, 4), it('litrpg:mana_potion', 4, 1, 2), it('potion', 3, potion='healing'),
                                   it('potion', 2, potion='swiftness'), it('potion', 1, potion='night_vision'), it('blaze_powder', 1),
                                   it('rabbit_foot', 1), it('fermented_spider_eye', 1), it('brown_mushroom', 2, 1, 3), it('red_mushroom', 2, 1, 3)],
                         coins=(2, 10)),
    'city_heal': table((2, 4), [it('golden_carrot', 4, 1, 3), it('glistering_melon_slice', 3, 1, 2), it('honey_bottle', 4, 1, 2),
                                it('litrpg:mana_potion', 4, 1, 2), it('potion', 5, potion='healing'), it('potion', 2, potion='regeneration'),
                                it('paper', 3, 1, 3), it('white_wool', 3, 1, 3), it('golden_apple', 1)], coins=(2, 8)),
    'city_food': table((3, 6), [it('bread', 8, 2, 6), it('cooked_beef', 4, 1, 4), it('cooked_porkchop', 4, 1, 4), it('cooked_chicken', 4, 1, 3),
                                it('cooked_cod', 3, 1, 4), it('cooked_salmon', 3, 1, 3), it('pumpkin_pie', 3, 1, 2), it('cake', 1),
                                it('egg', 4, 2, 6), it('sugar', 3, 2, 6), it('wheat', 4, 3, 9), it('milk_bucket', 1), it('honey_bottle', 2)],
                      coins=(2, 9)),
    'city_cloth': table((3, 5), [it('white_wool', 5, 2, 6), it('red_wool', 3, 1, 4), it('blue_wool', 3, 1, 4), it('yellow_wool', 2, 1, 4),
                                 it('string', 6, 2, 8), it('leather', 5, 1, 4), it('rabbit_hide', 3, 1, 3), it('red_dye', 2, 1, 3),
                                 it('blue_dye', 2, 1, 3), it('leather_chestplate', 1), it('leather_boots', 1), it('shears', 1)], coins=(2, 9)),
    'city_jewel': table((2, 4), [it('gold_nugget', 6, 3, 9), it('gold_ingot', 3, 1, 3), it('emerald', 3, 1, 3), it('amethyst_shard', 4, 1, 4),
                                 it('lapis_lazuli', 3, 2, 6), it('quartz', 3, 2, 6), it('diamond', 1), it('golden_apple', 1)], coins=(5, 18)),
    'city_bank': table((2, 3), [it('gold_ingot', 4, 1, 4), it('emerald', 4, 1, 4), it('gold_nugget', 5, 4, 12), it('paper', 3, 2, 6),
                                it('litrpg:silver_coin', 6, 2, 8), it('litrpg:gold_coin', 2, 1, 2)], coins=(10, 30)),
    'city_wood': table((3, 5), [it('arrow', 6, 4, 16), it('stick', 6, 4, 12), it('feather', 4, 2, 8), it('flint', 3, 1, 4), it('oak_log', 4, 2, 6),
                                it('spruce_planks', 4, 4, 12), it('string', 3, 1, 4), it('bow', 1), it('crossbow', 1), it('iron_axe', 1),
                                it('lead', 2, 1, 2), it('saddle', 1), it('hay_block', 2)], coins=(2, 8)),
    'city_scribe': table((2, 5), [it('paper', 6, 2, 8), it('book', 5, 1, 3), it('writable_book', 2), it('ink_sac', 4, 1, 3), it('feather', 4, 1, 4),
                                  it('map', 2), it('compass', 1), it('enchanted_book', 1), it('candle', 3, 1, 3), it('litrpg:scroll_of_oblivion', 1)],
                        coins=(2, 10)),
    'city_mining': table((3, 5), [it('coal', 6, 3, 10), it('raw_iron', 5, 1, 5), it('raw_copper', 5, 2, 8), it('raw_gold', 2, 1, 3),
                                  it('lapis_lazuli', 3, 2, 6), it('torch', 5, 4, 12), it('iron_pickaxe', 1), it('stone_pickaxe', 2),
                                  it('diamond', 1), it('rail', 2, 2, 8)], coins=(2, 9)),
    'city_armory': table((3, 6), [it('arrow', 6, 6, 24), it('iron_sword', 2), it('shield', 2), it('crossbow', 1), it('iron_helmet', 1),
                                  it('iron_chestplate', 1), it('chainmail_chestplate', 2), it('iron_ingot', 4, 1, 4), it('bread', 4, 2, 5),
                                  it('cooked_beef', 3, 1, 4), it('potion', 2, potion='healing')], coins=(4, 16)),
    # пиратские корабли и грот контрабандистов: награбленное, ром (мёд), порох, редкости с морского дна
    'pirate_treasure': table((4, 7), [it('gold_ingot', 5, 1, 4), it('gold_nugget', 6, 4, 12), it('emerald', 5, 1, 4), it('diamond', 2, 1, 2),
                                      it('gunpowder', 6, 2, 8), it('honey_bottle', 4, 1, 3), it('cooked_cod', 4, 2, 6), it('sugar', 3, 2, 6),
                                      it('nautilus_shell', 2), it('heart_of_the_sea', 1), it('compass', 2), it('spyglass', 2), it('map', 2),
                                      it('litrpg:reforge_stone', 1), it('litrpg:mana_potion', 3, 1, 2), it('potion', 2, potion='water_breathing'),
                                      it('trident', 1), it('crossbow', 2)], coins=(8, 30)),
    'pirate_captain': table((3, 5), [it('gold_ingot', 5, 2, 6), it('emerald', 5, 2, 5), it('diamond', 3, 1, 3), it('spyglass', 3),
                                     it('litrpg:tome_of_experience', 2), it('litrpg:ascension_stone', 1), it('litrpg:reforge_stone', 2),
                                     it('golden_apple', 2), it('heart_of_the_sea', 1), it('music_disc_pigstep', 1), it('filled_map', 2)],
                            coins=(16, 50)),
}

CRAFT_GROUP = {'blacksmith': 'city_smith', 'armorer': 'city_smith', 'weaponsmith': 'city_smith', 'jeweler': 'city_jewel', 'banker': 'city_bank',
               'tailor': 'city_cloth', 'leatherworker': 'city_cloth', 'herbalist': 'city_alchemy', 'alchemist': 'city_alchemy',
               'healer': 'city_heal', 'baker': 'city_food', 'butcher': 'city_food', 'fishmonger': 'city_food', 'innkeeper': 'city_food',
               'farmer': 'city_food', 'builder': 'city_wood', 'fletcher': 'city_wood', 'stablemaster': 'city_wood', 'scribe': 'city_scribe',
               'miner': 'city_mining'}

if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, t in T.items():
        with open(os.path.join(OUT, name + '.json'), 'w', encoding='utf-8') as f:
            json.dump(t, f, ensure_ascii=False)
    print('loot tables:', len(T))
