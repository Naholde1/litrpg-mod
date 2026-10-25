"""
Измерение «Подземелье Системы» (litrpg:dungeon): пустота без рельефа, карта — плитки-шаблоны (gen_dungeon.py),
у каждого этапа свой биом — туман, частицы в воздухе, фоновые звуки и музыка.
Запуск: python tools/gen_dimension.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'data', 'litrpg')

# id: (туман, вода, частица, вероятность, звук-петля, настроение, музыка)
BIOMES = {
    'vestibule': (0x14141F, 0x2A3A5A, 'minecraft:white_ash', 0.004, None, 'minecraft:ambient.cave', 'litrpg:music.as_time_flies'),
    'crypt': (0x16201A, 0x1F3A30, 'minecraft:white_ash', 0.012, 'minecraft:ambient.soul_sand_valley.loop', 'minecraft:ambient.soul_sand_valley.mood',
              'minecraft:music.nether.soul_sand_valley'),
    'thicket': (0x101A0C, 0x2E4A1A, 'minecraft:spore_blossom_air', 0.006, 'minecraft:ambient.warped_forest.loop', 'minecraft:ambient.warped_forest.mood',
                'minecraft:music.nether.warped_forest'),
    'temple': (0x0A2230, 0x0E4A6A, 'minecraft:underwater', 0.02, 'minecraft:ambient.underwater.loop', 'minecraft:ambient.underwater.loop.additions.rare',
               'minecraft:music.under_water'),
    'forges': (0x2A1208, 0x3AA8E0, 'minecraft:ash', 0.06, 'minecraft:ambient.basalt_deltas.loop', 'minecraft:ambient.basalt_deltas.mood',
               'minecraft:music.nether.basalt_deltas'),
    'spire': (0x160A24, 0x3A1A5A, 'minecraft:reverse_portal', 0.008, 'minecraft:ambient.crimson_forest.loop', 'minecraft:ambient.crimson_forest.mood',
              'minecraft:music.end'),
}


def biome(fog, water, particle, prob, loop, mood, music):
    eff = {
        'fog_color': fog, 'sky_color': fog, 'water_color': water, 'water_fog_color': water,
        'mood_sound': {'sound': mood, 'tick_delay': 6000, 'block_search_extent': 8, 'offset': 2.0},
        'music': {'sound': music, 'min_delay': 1200, 'max_delay': 3600, 'replace_current_music': False},
    }
    if particle:
        eff['particle'] = {'options': {'type': particle}, 'probability': prob}
    if loop:
        eff['ambient_sound'] = loop
    return {'has_precipitation': False, 'temperature': 0.5, 'downfall': 0.0, 'effects': eff,
            'spawners': {}, 'spawn_costs': {}, 'carvers': {}, 'features': []}


def write(path, obj):
    path = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def main():
    for k, v in BIOMES.items():
        write(f'worldgen/biome/{k}.json', biome(*v))
    write('tags/worldgen/biome/dungeon.json', {'values': [f'litrpg:{k}' for k in BIOMES]})
    write('dimension_type/dungeon.json', {
        'ultrawarm': False, 'natural': False, 'coordinate_scale': 1.0, 'has_skylight': False, 'has_ceiling': True,
        'ambient_light': 0.12, 'fixed_time': 18000, 'monster_spawn_light_level': 0, 'monster_spawn_block_light_limit': 0,
        'piglin_safe': True, 'bed_works': False, 'respawn_anchor_works': False, 'has_raids': False,
        'logical_height': 256, 'min_y': 0, 'height': 256, 'infiniburn': '#minecraft:infiniburn_overworld',
        'effects': 'minecraft:overworld'})
    zero = {k: 0 for k in ('barrier', 'fluid_level_floodedness', 'fluid_level_spread', 'lava', 'temperature', 'vegetation', 'continents',
                           'erosion', 'depth', 'ridges', 'initial_density_without_jaggedness', 'vein_toggle', 'vein_ridged', 'vein_gap')}
    zero['final_density'] = -1
    write('worldgen/noise_settings/void.json', {
        'sea_level': -64, 'disable_mob_generation': True, 'aquifers_enabled': False, 'ore_veins_enabled': False,
        'legacy_random_source': False, 'default_block': {'Name': 'minecraft:air'}, 'default_fluid': {'Name': 'minecraft:air'},
        'noise': {'min_y': 0, 'height': 256, 'size_horizontal': 1, 'size_vertical': 2},
        'noise_router': zero, 'spawn_target': [], 'surface_rule': {'type': 'minecraft:sequence', 'sequence': []}})
    write('dimension/dungeon.json', {
        'type': 'litrpg:dungeon',
        'generator': {'type': 'minecraft:noise', 'settings': 'litrpg:void',
                      'biome_source': {'type': 'litrpg:stages', 'biomes': [f'litrpg:{k}' for k in BIOMES]}}})
    write('worldgen/structure/dungeon_tile.json', {
        'type': 'litrpg:dungeon_tile', 'biomes': '#litrpg:dungeon', 'step': 'surface_structures',
        'spawn_overrides': {}, 'terrain_adaptation': 'none'})
    write('worldgen/structure_set/dungeon_tiles.json', {
        'structures': [{'structure': 'litrpg:dungeon_tile', 'weight': 1}], 'placement': {'type': 'litrpg:dungeon_tiles'}})
    # что можно ломать в подземелье (узлы силы, коконы, ледяные гробницы, треснувшие стены тайников)
    # (в отделке карты packed_ice и infested_cracked_stone_bricks не использовать — только механики)
    write('tags/blocks/breakable.json', {'values': ['litrpg:relic', 'minecraft:cobweb', 'minecraft:packed_ice',
                                                    'minecraft:infested_cracked_stone_bricks']})
    print('dimension ok')


if __name__ == '__main__':
    main()
