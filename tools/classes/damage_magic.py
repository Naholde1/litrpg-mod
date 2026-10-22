"""Маги и заклинатели урона."""
from .dsl import K, C

# 1. Драконья колдунья
K('dragon_slave', 'Драконий огонь', 'Dragon Slave', 'CONE', '♨', m=25, cd=8, el='FIRE', pw=1.3, r=9, f='Волна драконьего пламени')
K('light_strike', 'Огненный столп', 'Light Strike Array', 'STASIS', '◎', m=30, cd=12, r=3, d=1.5, f='Столп огня оглушает в точке')
K('fiery_soul', 'Огненная душа', 'Fiery Soul', 'BUFF_SELF', '♨', m=15, cd=20, me='haste 10 2; spd 10 1; fireres 10 0', f='Каждое заклинание разжигает душу')
K('laguna_blade', 'Клинок лагуны', 'Laguna Blade', 'BEAM', '⚡', m=90, cd=60, el='LIGHTNING', pw=4.0, r=20, f='Чудовищный разряд в одну цель')
C('dragonfire_sorceress', 'Драконья колдунья', 'Dragonfire Sorceress', 'DAMAGE', 0xEC7063, 'INT', 'LUCK', 'magic 3 fire 3',
  'fire_arrow dragon_slave light_strike fiery_soul fireball blink living_bomb laguna_blade',
  ('Кровь дракона', 'spell_power 8', 'fireproof'), 'Огонь и молния: столп пламени, драконий огонь и клинок лагуны.', races='demonkin human')

# 2. Демонолог
K('earth_spike', 'Земляной шип', 'Earth Spike', 'BEAM', '▲', m=25, cd=10, el='EARTH', pw=1.2, r=16, foe='slow 2 6', f='Шипы из земли подбрасывают строй')
K('demon_hex', 'Порча демона', 'Voodoo', 'STUN', '❦', m=30, cd=16, el='SHADOW', pw=0.2, r=18, d=3, f='Враг превращается в беспомощную тварь')
K('mana_drain', 'Похищение маны', 'Mana Drain', 'TETHER', '⇋', m=0, cd=15, pw=0.8, r=14, d=4, f='Сила течёт к колдуну')
K('finger_of_death', 'Палец смерти', 'Finger of Death', 'STRIKE', '☝', m=100, cd=70, el='SHADOW', pw=4.5, r=24, fl='exec', f='Одна цель. Одна смерть')
C('demonologist', 'Демонолог', 'Demonologist', 'DAMAGE', 0x6E2C00, 'INT', 'LUCK', 'magic 3 night 1 risk 1',
  'shadow_bolt earth_spike demon_hex mana_drain summon_imp curse life_tap finger_of_death',
  ('Договор крови', 'spell_power 6 mana 20'), 'Повелевает демонами: шипы, порча и палец смерти.', races='demonkin human')

# 3. Небесный маг
K('sky_bolt', 'Чародейский удар', 'Arcane Bolt', 'BOLT', '✦', m=15, cd=2, el='ARCANE', pw=1.4, f='Медленный, но мощный снаряд')
K('concussive_shot', 'Оглушающий выстрел', 'Concussive Shot', 'STRIKE', '◉', m=20, cd=8, el='ARCANE', pw=1.0, r=28, foe='slow 4 3', f='Бьёт ближайшего врага')
K('ancient_seal', 'Древняя печать', 'Ancient Seal', 'MARK', '⊗', m=20, cd=14, r=28, d=10, f='Печать ослабляет защиту')
K('mystic_flare', 'Мистическая вспышка', 'Mystic Flare', 'ZONE_AT', '✸', m=90, cd=60, el='ARCANE', pw=1.6, r=3, d=3, f='Звёзды бьют в малый круг')
C('skywrath', 'Небесный маг', 'Skywrath Mage', 'DAMAGE', 0xF7DC6F, 'INT', 'AGI', 'magic 4 explore 1',
  'sky_bolt concussive_shot ancient_seal mana_bolt arcane_beam blink arcane_missiles mystic_flare',
  ('Небесная кровь', 'mana_regen 15 spell_power 6'), 'Крылатый маг: печати, вспышки и град чародейских ударов.', races='elf')

# 4. Тектонический маг
K('split_earth', 'Раскол земли', 'Split Earth', 'STASIS', '⋕', m=30, cd=10, r=3, d=1.5, f='Земля раскалывается под врагами')
K('diabolic_edict', 'Дьявольский указ', 'Diabolic Edict', 'ZONE', '✶', m=40, cd=20, el='EARTH', pw=0.6, r=6, d=8, f='Взрывы бьют всех рядом')
K('lightning_storm_l', 'Цепная гроза', 'Lightning Storm', 'CHAIN', '≷', m=30, cd=6, el='LIGHTNING', pw=1.1, r=8, n=6, f='Молния прыгает по врагам')
K('pulse_nova', 'Импульсная нова', 'Pulse Nova', 'MULTI_NOVA', '◎', m=90, cd=60, el='ARCANE', pw=0.6, r=7, n=12, f='Волны чистой силы')
C('tectonic_mage', 'Тектонический маг', 'Tectonic Mage', 'DAMAGE', 0xCD6155, 'INT', 'VIT', 'magic 3 mining 2 tank 1',
  'split_earth lightning_storm_l diabolic_edict stone_bolt earth_wall magnetic_pull rock_fall pulse_nova',
  ('Каменная воля', 'res_explosion 20 spell_power 5'), 'Демон камня: раскол земли, указы и импульсная нова.', races='demonkin dwarf')

# 5. Изобретатель
K('laser', 'Лазер', 'Laser', 'BEAM', '⟿', m=25, cd=6, el='LIGHTNING', pw=1.5, r=16, foe='blind 2 0', f='Луч ослепляет')
K('heat_missiles', 'Тепловые ракеты', 'Heat-Seeking Missiles', 'CHAIN', '➹', m=30, cd=8, el='FIRE', pw=1.1, r=12, n=4, f='Ракеты находят врагов')
K('march_of_machines', 'Марш машин', 'March of the Machines', 'ZONE_AT', '⚙', m=50, cd=25, el='LIGHTNING', pw=0.6, r=6, d=6, f='Сотни роботов в атаку')
K('rearm', 'Перезарядка', 'Rearm', 'RESET', '⟳', m=60, cd=25, f='Все устройства снова готовы')
C('inventor', 'Изобретатель', 'Inventor', 'DAMAGE', 0x5DADE2, 'INT', 'LUCK', 'craft 3 magic 2',
  'laser heat_missiles march_of_machines blink deploy_turret overload crossbow_turret rearm',
  ('Гений', 'cooldown 8 mining 10'), 'Гном-гений: лазер, ракеты, марш машин и перезарядка всего.', races='dwarf', profs='blacksmith miner')

# 6. Разлом
K('malefice', 'Злодейство', 'Malefice', 'STUN', '◑', m=25, cd=14, el='SHADOW', pw=0.8, r=18, d=3, f='Тьма бьёт трижды')
K('demonic_conversion', 'Демоническое обращение', 'Demonic Conversion', 'SUMMON', '♨', m=30, cd=30, sm='minecraft:endermite', n=3, d=40, f='Мелкие твари из разлома')
K('midnight_pulse', 'Полуночный импульс', 'Midnight Pulse', 'ZONE_AT', '◌', m=40, cd=25, el='SHADOW', pw=0.5, r=5, d=10, f='Тёмная лужа пожирает жизнь')
K('black_hole', 'Чёрная дыра', 'Black Hole', 'VORTEX', '●', m=100, cd=90, el='SHADOW', pw=0.6, r=8, d=5, foe='slow 2 4', f='Всё падает внутрь')
C('riftcaller', 'Разлом', 'Riftcaller', 'DAMAGE', 0x2C3E50, 'INT', 'VIT', 'magic 3 night 1 party 1',
  'shadow_bolt malefice midnight_pulse demonic_conversion blink curse magnetic_pull black_hole',
  ('Порождение бездны', 'res_magic 10 spell_power 6'), 'Сгусток бездны: злодейство, импульсы и чёрная дыра.', races='demonkin')

# 7. Разрушитель
K('nether_blast', 'Взрыв Нижнего мира', 'Nether Blast', 'ZONE_AT', '✹', m=25, cd=6, el='ARCANE', pw=1.4, r=3.5, d=1, f='Мгновенный взрыв в точке')
K('decrepify', 'Бесплотность', 'Decrepify', 'MARK', '◌', m=20, cd=12, r=20, d=6, foe='slow 6 2', f='Враг становится уязвим к магии')
K('nether_ward', 'Страж Нижнего мира', 'Nether Ward', 'ZONE', '☖', m=40, cd=30, el='ARCANE', pw=0.6, r=7, d=12, f='Идол наказывает колдующих врагов')
K('life_drain', 'Выкачивание жизни', 'Life Drain', 'TETHER', '♁', m=50, cd=30, pw=1.5, r=12, d=8, f='Тянет жизнь из союзника… то есть из врага')
C('oblivion', 'Разрушитель', 'Oblivion', 'DAMAGE', 0x58D68D, 'INT', 'LUCK', 'magic 4 undead 1',
  'nether_blast decrepify nether_ward mana_bolt drain_life soul_siphon chaos_bolt life_drain',
  ('Пустая плоть', 'spell_power 10 health -4'), 'Хрупкий, но страшный: взрывы Нижнего мира и выкачивание жизни.', races='demonkin elf')

# 8. Глушитель
K('arcane_curse', 'Чародейское проклятие', 'Arcane Curse', 'ZONE_AT', '⊗', m=25, cd=14, el='ARCANE', pw=0.5, r=5, d=8, f='Враги ранятся, пока колдуют')
K('glaives_of_wisdom', 'Глефы мудрости', 'Glaives of Wisdom', 'BOLT', '☽', m=15, cd=1.5, el='ARCANE', pw=1.1, f='Чем умнее, тем больнее')
K('last_word', 'Последнее слово', 'Last Word', 'STUN', '✕', m=25, cd=14, el='ARCANE', pw=1.2, r=20, d=3, f='Молчать!')
K('global_silence', 'Всеобщее безмолвие', 'Global Silence', 'CHARM', '∅', m=80, cd=90, r=24, d=6, f='Тишина накрывает поле боя')
C('silencer', 'Глушитель', 'Silencer', 'DAMAGE', 0x7D3C98, 'INT', 'AGI', 'magic 3 party 1 solo 1',
  'glaives_of_wisdom arcane_curse last_word mana_bolt dispel blink counterspell global_silence',
  ('Похититель разума', 'spell_power 5 mana 30'), 'Отнимает голос: проклятия, последнее слово и всеобщее безмолвие.', races='elf human')

# 9. Гравимант
K('vacuum', 'Вакуум', 'Vacuum', 'VORTEX', '◍', m=30, cd=14, el='ARCANE', pw=0.4, r=5, d=1.5, f='Враги стягиваются в точку')
K('ion_shell', 'Ионная оболочка', 'Ion Shell', 'ZONE', '◎', m=25, cd=12, el='LIGHTNING', pw=0.4, r=3, d=10, f='Разряды вокруг колдуна')
K('surge', 'Ускорение', 'Surge', 'BUFF_ALLIES', '⇉', m=20, cd=12, r=8, me='spd 4 3', f='Союзники мчатся вперёд')
K('gravity_well', 'Гравитационный колодец', 'Gravity Well', 'STASIS', '⊙', m=80, cd=70, r=5, d=4, f='Тяжесть прижимает врагов к земле')
C('gravimancer', 'Гравимант', 'Gravimancer', 'DAMAGE', 0x5B2C6F, 'INT', 'VIT', 'magic 3 party 2',
  'mana_bolt vacuum ion_shell surge blink magnetic_pull arcane_beam gravity_well',
  ('Тяжёлая рука', 'spell_power 6 res_fall 30'), 'Владеет тяжестью: вакуум, ионы и колодец гравитации.', races='human dwarf')

# 10. Кошмар
K('nightmare', 'Кошмар', 'Nightmare', 'STUN', '☾', m=25, cd=16, el='SHADOW', pw=0.4, r=18, d=4, f='Враг засыпает и видит ужасы')
K('brain_sap', 'Высасывание разума', 'Brain Sap', 'STRIKE', '◉', m=25, cd=10, el='SHADOW', pw=1.4, r=18, fl='steal', f='Мысли врага — пища кошмара')
K('enfeeble', 'Изнурение', 'Enfeeble', 'STRIKE', '▽', m=15, cd=8, el='SHADOW', pw=0.3, r=20, foe='weak 8 2', f='Сила уходит из рук')
K('fiends_grip', 'Хватка исчадия', 'Fiend\'s Grip', 'MULTI_STRIKE', '✋', m=80, cd=60, el='SHADOW', pw=0.6, r=16, n=10, foe='slow 4 6', f='Кошмар держит и душит')
C('nightmare_weaver', 'Ткач кошмаров', 'Nightmare Weaver', 'DAMAGE', 0x4A235A, 'INT', 'VIT', 'magic 3 night 3',
  'shadow_bolt nightmare enfeeble brain_sap fear curse drain_life fiends_grip',
  ('Сон наяву', 'mana_regen 10', 'night_eye'), 'Повелитель снов: кошмары, изнурение и хватка исчадия.', races='demonkin')

# 11. Водяной элементаль
K('waveform', 'Волновая форма', 'Waveform', 'DASH_STRIKE', '≈', m=20, cd=8, el='FROST', pw=1.0, r=3.5, f='Маг превращается в волну')
K('adaptive_strike', 'Приспособленный удар', 'Adaptive Strike', 'STUN', '◒', m=25, cd=10, el='FROST', pw=1.4, r=20, d=1.5, f='Струя воды бьёт и оглушает')
K('morph_strength', 'Перетекание', 'Morph', 'BUFF_SELF', '⇅', m=20, cd=20, me='str 15 1; hp 15 2', f='Вода становится плотнее')
K('tidal_surge', 'Великий прилив', 'Tidal Surge', 'PUSH', '♒', m=70, cd=50, el='FROST', pw=1.8, r=10, foe='slow 4 2', f='Стена воды смывает всё')
C('water_elemental', 'Водяной элементаль', 'Water Elemental', 'DAMAGE', 0x2E86C1, 'INT', 'AGI', 'magic 2 water 4',
  'gush waveform adaptive_strike morph_strength bubble torrent tidal_wave tidal_surge',
  ('Текучесть', 'dodge 6', 'water_breathing'), 'Живая вода: волновая форма, струи и великий прилив.', races='elf human')

# 12. Горгона
K('stone_gaze', 'Каменный взгляд', 'Stone Gaze', 'STASIS', '◉', m=60, cd=60, r=7, d=3, f='Кто посмотрит — каменеет')
K('mystic_snake', 'Мистическая змея', 'Mystic Snake', 'CHAIN', '∫', m=25, cd=8, el='POISON', pw=1.0, r=7, n=5, f='Змея прыгает с врага на врага')
K('split_shot', 'Раздвоенный выстрел', 'Split Shot', 'ARROWS', '⋙', m=15, cd=4, st='AGI', pw=0.8, n=4, f='Стрелы во всех рядом')
K('gorgon_gaze', 'Взор горгоны', 'Petrifying Glare', 'STUN', '◕', m=30, cd=16, el='EARTH', pw=0.6, r=18, d=3, f='Взгляд обращает в камень одного')
C('gorgon', 'Горгона', 'Gorgon', 'DAMAGE', 0x1ABC9C, 'AGI', 'INT', 'ranged 2 magic 2 water 1 tank 1',
  'split_shot mystic_snake gorgon_gaze poison_arrow leap_back volley venom_mine stone_gaze',
  ('Змеиная чешуя', 'arrow_damage 10', 'mana_shield'), 'Змеедева: раздвоенные стрелы, мистическая змея и каменный взгляд.', races='beastkin demonkin')

# 13. Ведьма
K('hex_bolt', 'Сглаз', 'Hex Bolt', 'BOLT', '⛧', m=12, cd=2, el='SHADOW', pw=1.0, f='Тёмный сгусток')
K('cauldron', 'Котёл', 'Bubbling Cauldron', 'ZONE_AT', '⚱', m=35, cd=20, el='POISON', pw=0.5, r=4, d=10, foe='slow 2 1', f='Варево брызжет на врагов')
K('broom_dash', 'Полёт на метле', 'Broomstick', 'BLINK', '⌇', m=15, cd=8, r=12, me='slowfall 4 0', f='Ведьма взмывает на метле')
K('witchs_curse', 'Ведьмино проклятье', 'Witch\'s Curse', 'NOVA', '☠', m=60, cd=45, el='SHADOW', pw=1.2, r=8, foe='weak 10 2; slow 6 2; poison 6 1', f='Чёрное слово на всех')
C('witch', 'Ведьма', 'Witch', 'DAMAGE', 0x6C3483, 'INT', 'LUCK', 'magic 3 craft 2 night 1',
  'hex_bolt cauldron broom_dash curse acid_flask hex fear witchs_curse',
  ('Ведьмин котёл', 'spell_power 5 loot 10'), 'Варит, проклинает и летает на метле.', races='human elf', profs='alchemist herbalist')

# 14. Дух бури
K('static_remnant', 'Статический след', 'Static Remnant', 'MINE', '⌁', m=20, cd=4, el='LIGHTNING', pw=1.2, r=3.5, d=20, f='Копия из молний ждёт врага')
K('electric_vortex', 'Электрический вихрь', 'Electric Vortex', 'VORTEX', '@', m=30, cd=16, el='LIGHTNING', pw=0.2, r=4, d=1.5, f='Вихрь тянет врагов к духу')
K('overload_storm', 'Перегрузка', 'Overload', 'ON_HIT', 'ϟ', m=15, cd=10, n=4, oh='CHAIN', f='Удары бьют молнией всех рядом')
K('storm_ride', 'Полёт молнии', 'Ball Lightning Ride', 'BLINK', '◎', m=40, cd=4, r=18, me='spd 2 2', f='Дух становится молнией и летит')
C('storm_spirit', 'Дух бури', 'Storm Spirit', 'DAMAGE', 0x3498DB, 'INT', 'AGI', 'magic 3 explore 1 solo 1',
  'spark static_remnant electric_vortex overload_storm storm_ride chain_lightning ball_lightning wrath_of_thunder',
  ('Живая молния', 'move_speed 8 mana_regen 15'), 'Летает молнией, оставляет статические следы и тянет вихрем.', races='human elf')

# 15. Дух пустоты
K('aether_remnant', 'Эфирный след', 'Aether Remnant', 'MINE', '◌', m=25, cd=8, el='ARCANE', pw=1.2, r=4, d=20, foe='slow 2 4', f='Тень из эфира хватает врагов')
K('dissimilate', 'Растворение', 'Dissimilate', 'BLINK', '◐', m=25, cd=10, r=10, me='invis 1 0', f='Исчезнуть и появиться в другом месте')
K('resonant_pulse', 'Резонанс', 'Resonant Pulse', 'NOVA', '◎', m=25, cd=10, el='ARCANE', pw=1.0, r=5, me='abs 6 2', f='Волна и щит')
K('astral_step', 'Астральный шаг', 'Astral Step', 'DASH_STRIKE', '⇶', m=40, cd=12, el='ARCANE', pw=1.8, r=4, foe='slow 2 3', f='Рывок сквозь пространство')
C('void_spirit', 'Дух пустоты', 'Void Spirit', 'DAMAGE', 0x8E44AD, 'INT', 'AGI', 'magic 3 stealth 1 solo 1',
  'resonant_pulse aether_remnant dissimilate blink arcane_lunge umbral_prison runic_burst astral_step',
  ('Межмирье', 'dodge 6 spell_power 5'), 'Ходит сквозь пространство: эфирные следы и астральные шаги.', races='demonkin elf')

# 16. Маг крови
K('blood_bolt', 'Кровавая стрела', 'Blood Bolt', 'BOLT', '♦', m=0, cd=1.5, el='SHADOW', pw=1.4, hp=0.03, f='Платит каплей своей крови')
K('sanguine_ritual', 'Ритуал крови', 'Sanguine Ritual', 'SACRIFICE', '♰', m=0, cd=20, pw=0.15, me='str 8 0', f='Кровь становится маной')
K('hemoplague', 'Гемоплегия', 'Hemoplague', 'ZONE_AT', '☣', m=0, cd=20, el='SHADOW', pw=0.6, r=5, d=8, hp=0.08, fl='steal', f='Кровь врагов течёт к магу')
K('crimson_tide', 'Багровый прилив', 'Crimson Tide', 'NOVA', '☾', m=0, cd=60, el='SHADOW', pw=2.6, r=8, hp=0.2, fl='steal', f='Волна крови сносит всё')
C('blood_mage', 'Маг крови', 'Blood Mage', 'DAMAGE', 0x7B241C, 'INT', 'VIT', 'magic 2 risk 4',
  'blood_bolt sanguine_ritual hemoplague drain_life life_tap blood_boil blood_pact crimson_tide',
  ('Кровавая магия', 'lifesteal 6 health 6', 'berserker'), 'Колдует кровью, а не маной — и пьёт её из врагов.', races='demonkin human')

# 17. Руновед
K('rune_bolt', 'Рунный снаряд', 'Rune Bolt', 'BOLT', 'ᚠ', m=12, cd=1.5, el='ARCANE', pw=1.1, f='Руна срывается с пальцев')
K('rune_trap', 'Рунная западня', 'Rune Trap', 'MINE', 'ᛟ', m=20, cd=10, el='ARCANE', pw=1.6, r=4, d=60, foe='slow 3 3', f='Руна на земле')
K('rune_circle', 'Рунный круг', 'Rune Circle', 'ZONE_AT', 'ᛝ', m=40, cd=20, el='ARCANE', pw=0.6, r=5, d=8, foe='weak 2 1', f='Круг рун жжёт врагов')
K('word_of_power', 'Слово силы', 'Word of Power', 'GLOBAL', 'ᛞ', m=90, cd=80, el='ARCANE', pw=1.8, r=24, f='Древнее слово поражает всех')
C('runecaster', 'Руновед', 'Runecaster', 'DAMAGE', 0x5499C7, 'INT', 'LUCK', 'magic 3 craft 2',
  'rune_bolt rune_trap rune_circle runic_burst rune_of_ward blink arcane_beam word_of_power',
  ('Знаток рун', 'spell_power 6 cooldown 5'), 'Пишет руны в воздухе: снаряды, западни, круги и Слово силы.', races='dwarf elf')

# 18. Астромант
K('star_bolt', 'Звёздная стрела', 'Star Bolt', 'BOLT', '✦', m=12, cd=1.5, el='ARCANE', pw=1.1, f='Осколок звезды')
K('comet', 'Комета', 'Comet', 'METEOR', '☄', m=40, cd=14, el='FIRE', pw=1.8, r=4, f='Небесный камень с хвостом')
K('constellation', 'Созвездие', 'Constellation', 'BUFF_ALLIES', '✧', m=30, cd=30, r=10, me='luck 15 2; regen 15 0; nightv 15 0', f='Звёзды выстраиваются для отряда')
K('supernova', 'Сверхновая', 'Supernova', 'NOVA', '✺', m=100, cd=90, el='FIRE', pw=3.0, r=10, f='Звезда взрывается рядом с магом')
C('astromancer', 'Астромант', 'Astromancer', 'DAMAGE', 0x1A237E, 'INT', 'LUCK', 'magic 3 night 2 explore 1',
  'star_bolt comet starfall constellation blink gravity_well meteor supernova',
  ('Под звёздами', 'spell_power 6', 'night_eye'), 'Маг звёзд: кометы, созвездия и сверхновая.', races='elf human')

# 19. Чернильный художник
K('ink_swell', 'Чернильный прилив', 'Ink Swell', 'SHIELD', '◕', m=25, cd=12, pw=1.2, r=16, me='spd 4 1', f='Чернила окутывают союзника')
K('stroke_of_fate', 'Мазок судьбы', 'Stroke of Fate', 'BEAM', '∽', m=25, cd=8, el='SHADOW', pw=1.4, r=16, foe='slow 3 2', f='Кисть рисует по врагам')
K('phantom_embrace', 'Призрачные объятья', 'Phantom\'s Embrace', 'STUN', '◑', m=30, cd=16, el='SHADOW', pw=1.0, r=18, d=3, f='Нарисованный призрак держит врага')
K('soulbind', 'Связь душ', 'Soulbind', 'CHAIN', '∞', m=70, cd=60, el='SHADOW', pw=1.8, r=8, n=2, foe='slow 6 3', f='Две души связаны — боль на двоих')
C('ink_painter', 'Чернильный художник', 'Ink Painter', 'DAMAGE', 0x212F3C, 'INT', 'AGI', 'magic 3 craft 1 support 1',
  'stroke_of_fate phantom_embrace ink_swell shadow_bolt curse illusionist_brush mirror_veil soulbind',
  ('Кисть', 'spell_power 5 dodge 4'), 'Рисует тьмой: мазки судьбы, призраки и связь душ.', races='elf demonkin')
K('illusionist_brush', 'Нарисованный двойник', 'Painted Double', 'SUMMON', '♊', m=35, cd=35, sm='minecraft:vex', n=2, d=20, f='Нарисованные тени оживают')

# 20. Фея-проказница
K('bramble_maze', 'Лабиринт терний', 'Bramble Maze', 'WALL', '▒', m=30, cd=20, r=8, d=10, wall='minecraft:oak_leaves', f='Колючие кусты вокруг врагов')
K('shadow_realm', 'Царство теней', 'Shadow Realm', 'STEALTH', '◌', m=25, cd=16, d=4, r=0, me='str 4 1', f='Фея скрывается в тенях')
K('cursed_crown', 'Проклятый венец', 'Cursed Crown', 'STUN', '♔', m=30, cd=16, el='NATURE', pw=0.6, r=18, d=3, f='Венец сжимается через миг')
K('bedlam', 'Сумасбродство', 'Bedlam', 'ZONE', '✧', m=70, cd=60, el='ARCANE', pw=0.8, r=5, d=8, f='Огонёк носится вокруг и жалит')
C('mischief_fairy', 'Фея-проказница', 'Mischief Fairy', 'DAMAGE', 0xA569BD, 'INT', 'AGI', 'magic 3 nature 2 stealth 1',
  'thorn_bolt bramble_maze shadow_realm cursed_crown entangle charming_melody blink bedlam',
  ('Крылышки', 'dodge 10', 'featherfall'), 'Капризная фея: тернии, проклятый венец и сумасбродство.', races='elf')

# 21. Ледяной лич
K('frost_blast', 'Ледяной взрыв', 'Frost Blast', 'ZONE_AT', '❄', m=25, cd=8, el='FROST', pw=1.0, r=3.5, d=1, foe='slow 4 2', f='Холод бьёт в точку и вокруг')
K('frost_shield_lich', 'Ледяной щит', 'Frost Shield', 'SHIELD', '❆', m=25, cd=12, pw=1.6, r=16, f='Лёд на союзнике бьёт врагов')
K('sinister_gaze', 'Зловещий взгляд', 'Sinister Gaze', 'PULL', '◉', m=25, cd=16, r=16, foe='slow 3 3; weak 3 1', f='Враг сам идёт к личу')
K('chain_frost', 'Цепной мороз', 'Chain Frost', 'CHAIN', '❅', m=90, cd=60, el='FROST', pw=1.8, r=8, n=10, foe='slow 2 3', f='Ледяной шар прыгает десять раз')
C('frost_lich', 'Ледяной лич', 'Frost Lich', 'DAMAGE', 0xA9CCE3, 'INT', 'VIT', 'magic 3 undead 2 water 1',
  'ice_shard frost_blast sinister_gaze frost_shield_lich ice_wall blizzard ice_prison chain_frost',
  ('Мёртвый холод', 'res_undead 15', 'frost_touch'), 'Древний лич: взрывы холода, зловещий взгляд и цепной мороз.', races='human elf')

# 22. Пророчица смерти
K('crypt_swarm', 'Рой склепа', 'Crypt Swarm', 'CONE', '⁂', m=25, cd=8, el='SHADOW', pw=1.2, r=9, f='Рой летучих мышей')
K('death_silence', 'Безмолвие', 'Silence', 'ZONE_AT', '∅', m=25, cd=16, el='SHADOW', pw=0.2, r=4, d=5, foe='weak 5 2; slow 5 1', f='Тишина склепа')
K('spirit_siphon', 'Духовный отсос', 'Spirit Siphon', 'TETHER', '♁', m=20, cd=10, pw=1.0, r=12, d=6, f='Жизнь течёт к пророчице')
K('exorcism', 'Экзорцизм', 'Exorcism', 'SUMMON', '♅', m=90, cd=90, sm='minecraft:vex', n=6, d=30, f='Духи вырываются и мечутся по врагам')
C('death_prophet', 'Пророчица смерти', 'Death Prophet', 'DAMAGE', 0x76D7C4, 'INT', 'VIT', 'magic 3 undead 2 risk 1',
  'crypt_swarm death_silence spirit_siphon shadow_bolt drain_life plague_cloud lich_form exorcism',
  ('Полупризрак', 'lifesteal 4 res_undead 10'), 'Видит смерть: рой склепа, безмолвие и экзорцизм.', races='demonkin human')

# 23. Ядовитый маг
K('venomous_gale', 'Ядовитый вихрь', 'Venomous Gale', 'BEAM', '☣', m=25, cd=10, el='POISON', pw=1.0, r=12, foe='poison 6 2; slow 4 2', f='Волна яда')
K('plague_wards', 'Чумные стражи', 'Plague Wards', 'SUMMON', '☠', m=30, cd=20, sm='minecraft:cave_spider', n=2, d=40, f='Ядовитые твари')
K('poison_sting', 'Ядовитое жало', 'Poison Sting', 'ON_HIT', '♆', m=10, cd=10, n=12, oh='POISON', f='Каждый удар отравляет')
K('poison_nova', 'Ядовитая нова', 'Poison Nova', 'NOVA', '✺', m=80, cd=60, el='POISON', pw=1.0, r=10, foe='poison 12 3; weak 8 1', f='Яд накрывает всё вокруг')
C('venomancer', 'Ядовитый маг', 'Venomancer', 'DAMAGE', 0x52BE80, 'INT', 'AGI', 'magic 2 nature 2 beast 1',
  'acid_flask venomous_gale poison_sting plague_wards plague_cloud toxic_cloud serpent_ward poison_nova',
  ('Ядовитая кровь', 'res_arthropod 20', 'venom'), 'Ящер-отравитель: ядовитый вихрь, чумные стражи и нова.', races='beastkin')

# 24. Феникс
K('icarus_dive', 'Пике Икара', 'Icarus Dive', 'DASH_STRIKE', '⇘', m=0, cd=10, el='FIRE', pw=1.2, r=4, hp=0.08, f='Феникс ныряет в огне')
K('fire_spirits', 'Духи огня', 'Fire Spirits', 'ARROWS', '♨', m=25, cd=10, el='FIRE', pw=1.2, n=4, f='Четыре огонька летят к врагам')
K('sun_ray', 'Солнечный луч', 'Sun Ray', 'BEAM', '☀', m=0, cd=12, el='FIRE', pw=2.0, r=18, hp=0.06, f='Луч жжёт врагов, лечит союзников')
K('supernova_phoenix', 'Перерождение', 'Rebirth', 'NOVA', '✹', m=80, cd=120, el='FIRE', pw=2.0, r=8, me='regen 6 4; res 6 2', f='Феникс сгорает и возрождается')
C('phoenix', 'Феникс', 'Phoenix', 'DAMAGE', 0xFF7043, 'INT', 'VIT', 'magic 2 fire 4 risk 1',
  'fire_spirits icarus_dive sun_ray fiery_soul ring_of_fire fire_field flame_guard supernova_phoenix',
  ('Вечное пламя', 'health 4', 'phoenix fireproof'), 'Огненная птица: пике, солнечный луч и перерождение.', races='demonkin')

# 25. Двуглавый змий
K('dual_breath', 'Двойное дыхание', 'Dual Breath', 'CONE', '⫽', m=30, cd=9, el='FIRE', pw=1.0, r=8, foe='slow 3 2', f='Огонь и лёд сразу')
K('ice_path', 'Ледяная тропа', 'Ice Path', 'BEAM', '❆', m=30, cd=12, el='FROST', pw=0.8, r=14, foe='slow 3 8', f='Лёд сковывает строй')
K('liquid_fire', 'Жидкий огонь', 'Liquid Fire', 'ON_HIT', '♨', m=15, cd=15, n=8, oh='FIRE', f='Удары поджигают')
K('macropyre', 'Великий пожар', 'Macropyre', 'ZONE_AT', '▤', m=80, cd=60, el='FIRE', pw=0.9, r=7, d=10, f='Огненная полоса на земле')
C('twin_wyrm', 'Двуглавый змий', 'Twin Wyrm', 'DAMAGE', 0x48C9B0, 'INT', 'VIT', 'magic 3 fire 2 water 1',
  'fire_arrow ice_shard dual_breath ice_path liquid_fire frost_nova fire_field macropyre',
  ('Две головы', 'health 6 res_fire 15'), 'Две головы — огонь и лёд: двойное дыхание и великий пожар.', races='beastkin demonkin')

# 26. Разряд
K('thunder_strike_d', 'Громовой удар', 'Thunder Strike', 'LIGHTNING', '⚡', m=25, cd=8, el='LIGHTNING', pw=1.4, r=24, f='Молния бьёт трижды')
K('glimpse', 'Отблеск', 'Glimpse', 'PULL', '⟲', m=30, cd=16, r=24, foe='slow 2 3', f='Враг возвращается туда, где был')
K('kinetic_field', 'Кинетическое поле', 'Kinetic Field', 'WALL', '◯', m=40, cd=20, r=9, d=8, wall='minecraft:white_stained_glass', f='Кольцо, из которого не выйти')
K('static_storm', 'Статическая буря', 'Static Storm', 'ZONE_AT', '☈', m=80, cd=60, el='LIGHTNING', pw=1.0, r=6, d=6, f='Буря в точке прицела')
C('disruptor', 'Разряд', 'Disruptor', 'DAMAGE', 0x5DADE2, 'INT', 'VIT', 'magic 3 party 2',
  'spark thunder_strike_d glimpse kinetic_field chain_lightning blink thunderstorm static_storm',
  ('Громовой голос', 'spell_power 5 res_magic 5'), 'Шаман бури: отблеск, кинетическое поле и статическая буря.', races='orc beastkin')

# 27. Песчаный маг
K('sand_burial', 'Погребение', 'Sand Burial', 'STUN', '∴', m=25, cd=14, el='EARTH', pw=0.8, r=18, d=3, f='Песок поглощает врага')
K('dune_wall', 'Стена дюн', 'Dune Wall', 'WALL', '▙', m=30, cd=20, r=9, d=10, wall='minecraft:sand', f='Песок вздымается стеной')
K('scour', 'Пескоструй', 'Scour', 'CONE', '≋', m=20, cd=6, el='EARTH', pw=1.1, r=7, foe='blind 2 0', f='Песок сдирает кожу')
K('desert_storm', 'Буря пустыни', 'Desert Storm', 'ZONE_AT', '☁', m=80, cd=60, el='EARTH', pw=0.8, r=8, d=10, foe='blind 2 0; slow 2 1', f='Самум накрывает врагов')
C('sand_mage', 'Песчаный маг', 'Sand Mage', 'DAMAGE', 0xF0B27A, 'INT', 'VIT', 'magic 3 explore 2 mining 1',
  'stone_bolt scour sand_burial dune_wall sand_storm blink rock_fall desert_storm',
  ('Сын пустыни', 'res_fire 15 move_speed 4'), 'Маг пустыни: пескоструй, погребение и самум.', races='human beastkin')

# 28. Повелительница пауков
K('spawn_spiders', 'Выводок', 'Spawn Spiderlings', 'SUMMON', '⋇', m=30, cd=20, sm='minecraft:cave_spider', n=3, d=40, f='Паучата из кокона')
K('web', 'Паутина', 'Spin Web', 'WALL', '#', m=20, cd=16, r=7, d=12, wall='minecraft:cobweb', f='Паутина опутывает проход')
K('silken_bola', 'Шёлковая бола', 'Silken Bola', 'STUN', '⊛', m=20, cd=12, el='POISON', pw=0.6, r=16, d=2, foe='slow 4 3', f='Комок паутины')
K('insatiable_hunger', 'Ненасытный голод', 'Insatiable Hunger', 'BUFF_SELF', '♆', m=50, cd=50, me='str 12 2; regen 12 2', f='Мать паучья голодна')
C('spider_queen', 'Повелительница пауков', 'Spider Queen', 'DAMAGE', 0x4D5656, 'INT', 'AGI', 'beast 3 night 2 stealth 1',
  'spawn_spiders web silken_bola poison_blade insatiable_hunger plague_cloud vanish serpent_ward',
  ('Восемь лап', 'lifesteal 4 res_arthropod 30', 'night_eye'), 'Мать паучья: выводки, паутина и ненасытный голод.', races='demonkin elf')

# 29. Големостроитель
K('clay_golem', 'Глиняный голем', 'Clay Golem', 'SUMMON', '♜', m=40, cd=40, sm='minecraft:iron_golem', n=1, d=40, f='Глина оживает по слову')
K('golem_shard', 'Осколок камня', 'Stone Shard', 'BOLT', '◆', m=12, cd=2, el='EARTH', pw=1.1, f='Каменный снаряд')
K('animate_earth', 'Оживить землю', 'Animate Earth', 'ZONE_AT', '⛰', m=40, cd=20, el='EARTH', pw=0.6, r=5, d=6, foe='slow 2 2', f='Земля хватает врагов')
K('golem_legion', 'Легион големов', 'Golem Legion', 'SUMMON', '♜', m=100, cd=120, sm='minecraft:iron_golem', n=3, d=40, f='Целая армия камня')
C('golemancer', 'Големостроитель', 'Golemancer', 'DAMAGE', 0x7E5109, 'INT', 'VIT', 'magic 2 craft 3 mining 2',
  'golem_shard clay_golem animate_earth earth_wall stoneskin magnetic_pull rock_fall golem_legion',
  ('Мастер глины', 'mining 15 armor 1'), 'Лепит големов из глины и камня, а земля сама хватает врагов.', races='dwarf', profs='miner')
