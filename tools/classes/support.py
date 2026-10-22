"""Поддержка: лечит, защищает, усиливает и управляет боем."""
from .dsl import K, C

# 1. Целитель
K('mending_touch', 'Целящее касание', 'Mending Touch', 'HEAL_TARGET', '✚', m=20, cd=4, pw=1.6, r=16, f='Тёплый свет из ладоней')
K('renew', 'Обновление', 'Renew', 'TETHER', '❧', m=25, cd=10, el='HOLY', pw=1.0, r=16, d=12, f='Свет медленно лечит раны')
K('circle_of_healing', 'Круг исцеления', 'Circle of Healing', 'HEAL', '◎', m=40, cd=14, pw=1.4, r=10, f='Свет волной расходится по отряду')
K('miracle', 'Чудо', 'Miracle', 'ZONE', '✺', m=100, cd=120, el='HOLY', st='INT', pw=-0.8, r=12, d=6, me='res 6 1', f='Небо отвечает на молитву')
C('healer', 'Целитель', 'Healer', 'SUPPORT', 0xFDEBD0, 'INT', 'VIT', 'support 4 party 3',
  'mending_touch renew heal circle_of_healing word_of_shield purify greater_heal miracle',
  ('Призвание', 'mana_regen 15 mana 20'), 'Чистый лекарь: точечное, долгое и массовое исцеление.', races='human elf')

# 2. Полевой медик
K('regen_burst', 'Всплеск регенерации', 'Regenerative Burst', 'BUFF_ALLIES', '✚', m=30, cd=14, r=10, me='regen 5 2', f='Укол стимулятора всем рядом')
K('immortality_field', 'Поле бессмертия', 'Immortality Field', 'WARD', '◈', m=60, cd=60, r=12, d=6, f='Генератор не даёт союзнику умереть')
K('field_dressing', 'Перевязка', 'Field Dressing', 'HEAL_TARGET', '✙', m=20, cd=6, pw=1.4, r=8, f='Бинт и спирт')
K('amplifier', 'Усиливающая матрица', 'Amplification Matrix', 'BUFF_ALLIES', '▤', m=70, cd=80, r=12, me='str 12 2; haste 12 1', f='Окно, сквозь которое пули вдвое злее')
C('field_medic', 'Полевой медик', 'Field Medic', 'SUPPORT', 0x48C9B0, 'INT', 'AGI', 'support 3 party 2 ranged 1',
  'field_dressing regen_burst volley leap_back healing_flask immortality_field purify amplifier',
  ('Аптечка', 'health_regen 1 mana_cost 8'), 'Лечит и стреляет: стимуляторы, перевязки и поле бессмертия.', races='human dwarf')

# 3. Оракул
K('fortunes_end', 'Конец судьбы', 'Fortune\'s End', 'STUN', '◐', m=25, cd=12, el='ARCANE', pw=0.8, r=20, d=2, f='Сфера судьбы замораживает врага')
K('purifying_flames', 'Очищающее пламя', 'Purifying Flames', 'HEAL_TARGET', '♆', m=20, cd=3, pw=1.3, r=18, f='Огонь, что лечит')
K('fates_edict', 'Указ судьбы', 'Fate\'s Edict', 'SHIELD', '⚖', m=30, cd=14, pw=1.8, r=18, f='Союзнику запрещено получать урон')
K('false_promise', 'Ложное обещание', 'False Promise', 'WARD', '☥', m=60, cd=70, r=20, d=8, f='Смерть отсрочена — а там посмотрим')
C('oracle', 'Оракул', 'Oracle', 'SUPPORT', 0xF5CBA7, 'INT', 'LUCK', 'support 3 magic 2 party 1',
  'purifying_flames fortunes_end fates_edict mana_bolt purify blessing false_promise time_warp',
  ('Взгляд вперёд', 'dodge 6 mana_regen 10'), 'Видит будущее: оглушает сферой судьбы и обманывает смерть.', races='human elf')

# 4. Ангел-хранитель
K('caduceus_beam', 'Луч кадуцея', 'Caduceus Beam', 'TETHER', '☤', m=15, cd=8, el='HOLY', pw=1.4, r=18, d=10, f='Золотой луч к союзнику')
K('guardian_flight', 'Полёт к союзнику', 'Guardian Flight', 'RALLY', '➶', m=15, cd=8, r=40, me='slowfall 3 0', f='Ангел летит к другу')
K('angelic_descent', 'Ангельский спуск', 'Angelic Descent', 'BUFF_SELF', '♆', m=15, cd=15, me='slowfall 8 0; spd 8 0', f='Крылья замедляют падение')
K('valkyrie', 'Валькирия', 'Valkyrie', 'BUFF_ALLIES', '♕', m=80, cd=90, r=16, me='regen 15 2; slowfall 15 0; res 15 0', f='Крылья раскрываются над отрядом')
C('guardian_angel', 'Ангел-хранитель', 'Guardian Angel', 'SUPPORT', 0xFFF9C4, 'INT', 'AGI', 'support 4 party 3',
  'caduceus_beam guardian_flight angelic_descent word_of_shield blessing guardian_spirit greater_heal valkyrie',
  ('Крылья', 'move_speed 6 mana_regen 10', 'featherfall'), 'Летает к раненым и держит их золотым лучом.', races='human elf')

# 5. Аптекарь
K('sleep_dart', 'Усыпляющий дротик', 'Sleep Dart', 'STUN', '➹', m=20, cd=14, st='AGI', pw=0.3, r=28, d=4, f='Дротик валит с ног')
K('biotic_grenade', 'Биотическая граната', 'Biotic Grenade', 'ZONE_AT', '◉', m=30, cd=12, el='NATURE', pw=-0.4, r=4, d=3, f='Брызги лекарства')
K('healing_dart', 'Целебный дротик', 'Healing Dart', 'HEAL_TARGET', '➶', m=15, cd=3, st='AGI', pw=1.2, r=30, f='Выстрел, который лечит')
K('nano_boost', 'Нано-стимулятор', 'Nano Boost', 'BUFF_ALLIES', '⚡', m=60, cd=70, r=16, me='str 10 2; res 10 1; spd 10 0', f='Сыворотка силы для отряда')
C('apothecary', 'Аптекарь', 'Apothecary', 'SUPPORT', 0x5499C7, 'AGI', 'INT', 'support 3 ranged 2 stealth 1',
  'healing_dart sleep_dart biotic_grenade camouflage poison_arrow healing_flask nano_boost headshot',
  ('Меткий лекарь', 'arrow_damage 10 crit_chance 4'), 'Снайпер-врач: лечит выстрелами и усыпляет врагов.', races='human beastkin')

# 6. Уличный музыкант
K('speed_track', 'Быстрый ритм', 'Speed Track', 'BUFF_ALLIES', '♪', m=20, cd=12, r=12, me='spd 8 1', f='Музыка подгоняет')
K('heal_track', 'Целебный мотив', 'Healing Track', 'HEAL', '♬', m=25, cd=8, pw=0.9, r=12, f='Мелодия латает раны')
K('sound_wave', 'Звуковая волна', 'Sound Wave', 'PUSH', '≫', m=15, cd=6, el='ARCANE', pw=0.5, r=6, f='Бас сносит врагов')
K('sound_barrier', 'Звуковой барьер', 'Sound Barrier', 'BUFF_ALLIES', '♫', m=80, cd=90, r=14, me='abs 8 6; res 8 0', f='Громкость на максимум — щит всем')
C('street_musician', 'Уличный музыкант', 'Street Musician', 'SUPPORT', 0x2ECC71, 'AGI', 'INT', 'support 3 party 3 trade 1',
  'heal_track speed_track sound_wave swift_step tempo dissonance encore sound_barrier',
  ('Ритм', 'move_speed 8 sell_price 10'), 'Музыка лечит и ускоряет, бас отбрасывает, а барьер спасает всех.', races='human beastkin')

# 7. Дзен-мастер
K('harmony_orb', 'Сфера гармонии', 'Orb of Harmony', 'TETHER', '◯', m=15, cd=6, el='HOLY', pw=1.2, r=24, d=12, f='Сфера кружит над союзником')
K('discord_orb', 'Сфера раздора', 'Orb of Discord', 'MARK', '●', m=15, cd=8, r=30, d=12, f='Враг уязвим')
K('destruction_orbs', 'Сферы разрушения', 'Orbs of Destruction', 'MULTI_STRIKE', '∴', m=20, cd=4, el='ARCANE', pw=0.6, r=24, n=3, f='Три сферы летят в цель')
K('transcendence', 'Просветление', 'Transcendence', 'ZONE', '☸', m=90, cd=100, el='HOLY', st='INT', pw=-0.9, r=8, d=5, me='res 5 4', f='Мастер парит над болью')
C('zen_master', 'Дзен-мастер', 'Zen Master', 'SUPPORT', 0xF7DC6F, 'INT', 'AGI', 'support 3 magic 2 solo 1',
  'destruction_orbs harmony_orb discord_orb meditation chi_wave palm_strike iron_body transcendence',
  ('Невозмутимость', 'mana_regen 20 dodge 4'), 'Парит в медитации: сферы гармонии и раздора, просветление спасает всех.',
  races='human')

# 8. Биомантка
K('biotic_grasp', 'Биотическая хватка', 'Biotic Grasp', 'CONE', '✋', m=15, cd=3, el='SHADOW', pw=0.7, r=6, fl='steal', f='Высасывает жизнь, лечит себя')
K('fade', 'Растворение', 'Fade', 'STEALTH', '◌', m=20, cd=10, d=1.5, r=0, me='spd 2 3; res 2 4', f='Тело на миг исчезает')
K('biotic_orb', 'Биотическая сфера', 'Biotic Orb', 'ZONE', '◑', m=35, cd=14, el='NATURE', pw=-0.35, r=6, d=6, f='Сфера лечит всех, кого касается')
K('coalescence', 'Слияние', 'Coalescence', 'BEAM', '☲', m=80, cd=80, el='NATURE', pw=1.8, r=22, me='res 4 1', f='Луч лечит друзей и жжёт врагов')
C('biomancer', 'Биомантка', 'Biomancer', 'SUPPORT', 0xA569BD, 'INT', 'VIT', 'support 2 magic 2 risk 1 solo 1',
  'biotic_grasp biotic_orb fade drain_life soul_siphon healing_bloom tranquility coalescence',
  ('Генетика', 'lifesteal 5 health_regen 1'), 'Учёная, что крадёт жизнь у врагов и отдаёт её друзьям.', races='demonkin human')

# 9. Оружейница
K('repair_pack', 'Ремнабор', 'Repair Pack', 'HEAL_TARGET', '⚙', m=20, cd=6, pw=1.4, r=20, me='abs 6 1', f='Броня союзника чинится на лету')
K('whip_shot', 'Удар-кнут', 'Whip Shot', 'PUSH', '↯', m=15, cd=6, pw=0.8, r=8, f='Цеп на цепи откидывает врага')
K('inspire', 'Воодушевление', 'Inspire', 'BUFF_ALLIES', '♥', m=25, cd=16, r=10, me='regen 8 1', f='Каждый удар бодрит отряд')
K('rally_armor', 'Сплочение', 'Rally', 'BUFF_ALLIES', '♖', m=70, cd=80, r=14, me='abs 20 4; res 20 0; spd 20 0', f='Броня всему отряду')
C('armorsmith', 'Оружейница', 'Armorsmith', 'SUPPORT', 0xE59866, 'STR', 'INT', 'support 2 craft 3 melee 2',
  'repair_pack whip_shot inspire shield_bash temper_armor guardian_ward bulwark_slam rally_armor',
  ('Мастерская', 'armor 2 sell_price 10'), 'Боевая мастерица: чинит броню друзьям прямо в бою.', races='dwarf human', profs='blacksmith')

# 10. Жрица кицунэ
K('suzu', 'Колокольчик-оберег', 'Protection Suzu', 'PURIFY', '♩', m=25, cd=14, el='HOLY', pw=0.4, r=8, me='res 2 3', f='Звон снимает порчу')
K('fox_step', 'Лисий шаг', 'Swift Step', 'RALLY', '⇢', m=15, cd=10, r=40, f='Жрица появляется рядом с другом')
K('ofuda', 'Офуда', 'Healing Ofuda', 'HEAL', '▯', m=15, cd=3, pw=0.8, r=12, f='Бумажные талисманы летят к раненым')
K('kitsune_rush', 'Бег кицунэ', 'Kitsune Rush', 'BUFF_ALLIES', '♢', m=80, cd=90, r=14, me='spd 12 2; haste 12 2; regen 12 1', f='Дух лисы мчит отряд вперёд')
C('kitsune_priestess', 'Жрица кицунэ', 'Kitsune Priestess', 'SUPPORT', 0xF1948A, 'AGI', 'INT', 'support 3 stealth 1 party 2',
  'ofuda suzu fox_step kunai_rain shadow_step blessing purify kitsune_rush',
  ('Лисья удача', 'dodge 6 crit_chance 4'), 'Талисманы, оберег-колокольчик и дух лисы, что ускоряет отряд.', races='beastkin elf')

# 11. Цветочный ткач
K('petal_platform', 'Цветочная платформа', 'Petal Platform', 'BUFF_SELF', '✿', m=15, cd=10, me='jump 5 3; slowfall 5 0', f='Лепестки поднимают ввысь')
K('life_grip', 'Хватка жизни', 'Life Grip', 'GATHER', '❦', m=30, cd=30, r=24, me='res 3 2', f='Лоза вытягивает союзников к ткачу')
K('blossom', 'Цветение', 'Healing Blossom', 'HEAL_TARGET', '❀', m=15, cd=4, pw=1.3, r=20, f='Бутон распускается на союзнике')
K('tree_of_life', 'Древо жизни', 'Tree of Life', 'ZONE_AT', '♣', m=80, cd=80, el='NATURE', pw=-0.6, r=8, d=10, f='Древо вырастает и лечит всех вокруг')
C('lifeweaver', 'Цветочный ткач', 'Lifeweaver', 'SUPPORT', 0xF8C471, 'INT', 'AGI', 'support 3 nature 3 party 1',
  'blossom petal_platform life_grip rejuvenation entangle thorn_wall healing_bloom tree_of_life',
  ('Пыльца', 'health_regen 1 mana_regen 10'), 'Ткёт жизнь из цветов: лечит бутонами и растит Древо жизни.', races='elf', faction='elves')

# 12. Жрица Солнца
K('solar_rifle', 'Солнечный луч', 'Solar Beam', 'BEAM', '☀', m=15, cd=3, el='HOLY', pw=1.1, r=24, f='Сгусток солнечного света')
K('healing_pylon', 'Целебный пилон', 'Healing Pylon', 'ZONE_AT', '♈', m=35, cd=20, el='HOLY', pw=-0.4, r=6, d=12, f='Кристалл лечит всех рядом')
K('sunburst_flare', 'Вспышка', 'Outburst', 'PUSH', '✹', m=15, cd=7, el='HOLY', pw=0.6, r=5, f='Жрица отталкивается волной света')
K('captive_sun', 'Пленённое солнце', 'Captive Sun', 'METEOR', '☼', m=90, cd=90, el='HOLY', pw=3.0, r=6, f='Солнце падает на врагов')
C('sun_priestess', 'Жрица Солнца', 'Sun Priestess', 'SUPPORT', 0xF9E79F, 'INT', 'AGI', 'support 2 magic 2 fire 1 party 1',
  'solar_rifle healing_pylon sunburst_flare holy_light smite consecration sanctuary captive_sun',
  ('Сияние', 'dmg_undead 10 mana_regen 10'), 'Солнце в руках: пилоны лечат, лучи жгут, пленённое солнце падает.', races='human elf')

# 13. Знахарь вуду
K('paralyzing_cask', 'Парализующий бочонок', 'Paralyzing Cask', 'CHAIN', '⚱', m=25, cd=14, el='POISON', pw=0.6, r=6, n=5, foe='slow 2 8',
  f='Бочонок скачет от врага к врагу')
K('voodoo_restoration', 'Восстановление вуду', 'Voodoo Restoration', 'ZONE', '♅', m=30, cd=20, el='NATURE', pw=-0.3, r=6, d=10, f='Шаман лечит всех вокруг')
K('maledict', 'Проклятие', 'Maledict', 'ZONE_AT', '☿', m=35, cd=16, el='SHADOW', pw=0.5, r=4, d=6, f='Раны врагов гноятся')
K('death_ward', 'Страж смерти', 'Death Ward', 'SUMMON', '☠', m=70, cd=70, sm='minecraft:skeleton', n=2, d=20, f='Черепа на посохах стреляют')
C('voodoo_healer', 'Знахарь вуду', 'Voodoo Healer', 'SUPPORT', 0x16A085, 'INT', 'LUCK', 'support 3 magic 2 undead 1',
  'paralyzing_cask voodoo_restoration maledict hex curse healing_totem spirit_link death_ward',
  ('Куклы и травы', 'mana_regen 10 coin_find 15'), 'Знахарь из болот: бочонки, проклятия и черепа-стражи.', races='orc beastkin')

# 14. Заклинатель змей
K('ether_shock', 'Эфирный удар', 'Ether Shock', 'CHAIN', '≷', m=20, cd=7, el='LIGHTNING', pw=0.9, r=6, n=4, f='Разряд по нескольким целям')
K('shackles', 'Оковы', 'Shackles', 'STUN', '⌁', m=30, cd=16, el='NATURE', pw=0.8, r=14, d=3.5, f='Змеи держат врага')
K('serpent_ward', 'Змеиные стражи', 'Serpent Wards', 'SUMMON', '∫', m=60, cd=60, sm='minecraft:cave_spider', n=4, d=25, f='Ядовитые твари из корзины')
K('snake_charm', 'Змеиные чары', 'Snake Charm', 'CHARM', '∿', m=30, cd=25, r=8, d=6, f='Флейта усыпляет злобу')
C('snake_charmer', 'Заклинатель змей', 'Snake Charmer', 'SUPPORT', 0x1E8449, 'INT', 'AGI', 'support 2 beast 2 magic 2',
  'ether_shock shackles snake_charm hex poison_arrow healing_totem serpent_ward plague_cloud',
  ('Змеиная кровь', 'res_arthropod 20 mana_regen 10', 'venom'), 'Флейта, змеи и разряды: держит врагов в оковах.', races='beastkin human')

# 15. Снежная дева
K('frost_bite', 'Обморожение', 'Frostbite', 'STUN', '❅', m=25, cd=12, el='FROST', pw=1.0, r=20, d=2.5, f='Лёд сковывает цель')
K('crystal_nova', 'Кристальная нова', 'Crystal Nova', 'ZONE_AT', '✲', m=30, cd=10, el='FROST', pw=0.7, r=5, d=2, foe='slow 4 3', f='Взрыв снежинок')
K('arcane_aura', 'Чародейская аура', 'Arcane Aura', 'MEDITATE', '✧', m=0, cd=40, pw=0.35, f='Мана течёт сама')
K('freezing_field', 'Ледяное поле', 'Freezing Field', 'ZONE', '❄', m=90, cd=80, el='FROST', pw=0.9, r=8, d=8, foe='slow 2 3', f='Буран вокруг девы')
C('snow_maiden', 'Снежная дева', 'Snow Maiden', 'SUPPORT', 0xD6EAF8, 'INT', 'VIT', 'support 2 magic 3 water 1',
  'ice_shard frost_bite crystal_nova arcane_aura ice_wall blessing ice_armor freezing_field',
  ('Холодное сердце', 'mana_regen 25'), 'Дарит отряду ману и морозит врагов ледяным полем.', races='elf human')

# 16. Светоносец
K('illuminate', 'Озарение', 'Illuminate', 'BEAM', '☼', m=30, cd=10, el='HOLY', pw=1.7, r=24, f='Волна света бьёт насквозь')
K('chakra_magic', 'Магия чакры', 'Chakra Magic', 'MEDITATE', '◉', m=0, cd=25, pw=0.3, me='haste 6 0', f='Мана возвращается к светоносцу')
K('blinding_light', 'Слепящий свет', 'Blinding Light', 'PUSH', '✷', m=25, cd=14, el='HOLY', pw=0.4, r=7, foe='blind 3 0; weak 4 1', f='Вспышка отбрасывает и ослепляет')
K('spirit_form', 'Облик духа', 'Spirit Form', 'BUFF_ALLIES', '♆', m=60, cd=70, r=14, me='regen 15 1; spd 15 1; glow 15 0', f='Светоносец становится духом света')
C('lightbearer', 'Светоносец', 'Lightbearer', 'SUPPORT', 0xFCF3CF, 'INT', 'LUCK', 'support 3 magic 2 night 1',
  'illuminate chakra_magic blinding_light holy_light reveal_heresy guardian_flight purify spirit_form',
  ('Фонарь', 'mana 30', 'night_eye'), 'Носит свет во тьму: озаряет, слепит и даёт ману.', races='human elf')

# 17. Чаровница
K('enchant_beast', 'Зачаровать зверя', 'Enchant', 'DOMINATE', '♡', m=40, cd=35, r=16, d=45, f='Дикая тварь становится другом')
K('natures_attendants', 'Духи природы', 'Nature\'s Attendants', 'ZONE', '✿', m=40, cd=25, el='NATURE', pw=-0.35, r=6, d=10, f='Светлячки лечат отряд')
K('untouchable', 'Неприкосновенная', 'Untouchable', 'CHARM', '♥', m=30, cd=30, r=6, d=6, f='Рука не поднимается на неё')
K('impetus', 'Порыв', 'Impetus', 'BOLT', '➹', m=20, cd=3, el='NATURE', pw=1.3, f='Чем дальше цель, тем больнее копьё')
C('enchantress', 'Чаровница', 'Enchantress', 'SUPPORT', 0xF5B7B1, 'INT', 'LUCK', 'support 3 beast 2 nature 2',
  'impetus enchant_beast natures_attendants untouchable entangle call_wolf healing_bloom tranquility',
  ('Дриада', 'dodge 6 mana_regen 10'), 'Дриада: зачаровывает зверей и монстров, светлячки лечат отряд.', races='elf beastkin', faction='wilds')

# 18. Проповедник
K('holy_persuasion', 'Святое убеждение', 'Holy Persuasion', 'DOMINATE', '✞', m=40, cd=30, r=16, d=60, f='Враг обращается в веру')
K('penitence', 'Покаяние', 'Penitence', 'STRIKE', '✟', m=20, cd=10, el='HOLY', pw=0.6, r=20, foe='slow 6 2; weak 6 1', f='Грешник замедлен и слаб')
K('divine_favor', 'Божья милость', 'Divine Favor', 'BUFF_ALLIES', '✧', m=30, cd=25, r=12, me='regen 10 1', f='Благодать над паствой')
K('hand_of_god', 'Длань божья', 'Hand of God', 'HEAL', '☝', m=90, cd=90, pw=4.0, r=30, f='Все союзники вокруг исцелены')
C('preacher', 'Проповедник', 'Preacher', 'SUPPORT', 0xFFF5D6, 'INT', 'VIT', 'support 3 party 2 trade 1',
  'smite penitence divine_favor holy_persuasion heal purify sanctuary hand_of_god',
  ('Паства', 'xp 5 mana_regen 10'), 'Обращает врагов в веру и лечит всех вокруг Дланью божьей.',
  races='human', faction='kingdom')

# 19. Защитник веры
K('purification', 'Очищение светом', 'Purification', 'HEAL_TARGET', '✙', m=25, cd=8, pw=1.5, r=14, f='Свет лечит и жжёт врагов вокруг')
K('heavenly_grace', 'Небесная благодать', 'Heavenly Grace', 'PURIFY', '❖', m=25, cd=16, el='HOLY', pw=0.3, r=10, me='regen 6 1; res 6 0', f='Снимает все хвори')
K('degen_aura', 'Аура тяжести', 'Degen Aura', 'ZONE', '◌', m=30, cd=25, el='HOLY', pw=0.2, r=6, d=12, foe='slow 2 2', f='Враги вязнут у защитника')
K('guardian_angel_aura', 'Ангельская защита', 'Guardian Angel', 'BUFF_ALLIES', '♆', m=90, cd=110, r=14, me='res 6 4; regen 6 1', f='Отряд почти неуязвим')
C('faith_defender', 'Защитник веры', 'Faith Defender', 'SUPPORT', 0xFDFEFE, 'STR', 'INT', 'support 3 melee 1 undead 2 tank 1',
  'purification holy_strike heavenly_grace degen_aura hammer_of_justice holy_light divine_shield guardian_angel_aura',
  ('Щит веры', 'armor 2 dmg_undead 15'), 'Боевой жрец: очищение светом, снятие хворей и ангельская защита.', races='human dwarf')

# 20. Тёмный покровитель
K('mist_coil', 'Туманный вихрь', 'Mist Coil', 'HEAL_TARGET', '♁', m=0, cd=5, pw=1.6, r=18, hp=0.05, f='Покровитель отдаёт свою кровь')
K('aphotic_shield', 'Тёмный щит', 'Aphotic Shield', 'SHIELD', '◘', m=30, cd=10, pw=2.0, r=16, f='Тьма укрывает союзника')
K('curse_of_avernus', 'Проклятие Аверна', 'Curse of Avernus', 'ON_HIT', '☾', m=20, cd=20, n=8, oh='FROST', f='Удары морозят')
K('borrowed_time', 'Взятое взаймы', 'Borrowed Time', 'BUFF_SELF', '⌛', m=50, cd=90, me='regen 6 4; res 6 2', f='Удары не вредят, а лечат')
C('dark_patron', 'Тёмный покровитель', 'Dark Patron', 'SUPPORT', 0x34495E, 'VIT', 'STR', 'support 2 undead 2 tank 1 risk 2',
  'mist_coil aphotic_shield curse_of_avernus frost_strike death_grip anti_magic_shell deathless borrowed_time',
  ('Тёмный дар', 'health 6 lifesteal 3'), 'Рыцарь смерти-лекарь: платит своей кровью за жизнь союзников.', races='human demonkin')

# 21. Блуждающий огонёк
K('wisp_tether', 'Привязь огонька', 'Tether', 'TETHER', '∞', m=20, cd=8, el='HOLY', pw=1.3, r=18, d=12, me='spd 12 1', f='Огонёк делит силы с союзником')
K('spirits', 'Духи', 'Spirits', 'MULTI_NOVA', '∘', m=30, cd=16, el='ARCANE', pw=0.5, r=4.5, n=5, f='Огоньки кружат и бьют врагов')
K('overcharge', 'Перегрузка', 'Overcharge', 'BUFF_ALLIES', '⚡', m=30, cd=25, r=8, me='haste 10 1; regen 10 1', f='Огонёк отдаёт свет')
K('relocate', 'Перенос', 'Relocate', 'GATHER', '⊕', m=60, cd=60, r=40, f='Огонёк собирает отряд к себе')
C('wisp', 'Блуждающий огонёк', 'Wisp', 'SUPPORT', 0xE8F8F5, 'INT', 'AGI', 'support 3 party 3 explore 1',
  'wisp_tether spirits overcharge guardian_flight blink purify relocate tranquility',
  ('Путеводный свет', 'move_speed 8 mana_regen 10'), 'Живой свет: привязывается к союзнику и переносит отряд.', races='elf')

# 22. Жрец теней
K('shadow_wave', 'Теневая волна', 'Shadow Wave', 'HEAL', '◑', m=25, cd=8, pw=1.1, r=10, f='Волна лечит друзей и ранит врагов рядом')
K('shallow_grave', 'Неглубокая могила', 'Shallow Grave', 'WARD', '⚰', m=40, cd=35, r=20, d=5, f='Союзник не может умереть')
K('poison_touch', 'Ядовитое касание', 'Poison Touch', 'STUN', '☣', m=20, cd=12, el='POISON', pw=0.6, r=16, d=1.5, foe='poison 6 1', f='Яд парализует')
K('bad_juju', 'Дурной знак', 'Bad Juju', 'RESET', '⌘', m=60, cd=90, f='Все навыки снова готовы')
C('shadow_priest', 'Жрец теней', 'Shadow Priest', 'SUPPORT', 0x6C3483, 'INT', 'LUCK', 'support 3 magic 1 night 2',
  'poison_touch shadow_wave shallow_grave curse spirit_link drain_life plague_cloud bad_juju',
  ('Тайные ритуалы', 'mana_regen 12 dmg_humanoid 5'), 'Тёмный жрец: неглубокая могила спасает, теневая волна лечит.', races='demonkin orc')

# 23. Огр-чародей
K('ogre_ignite', 'Поджог', 'Ignite', 'BOLT', '☄', m=20, cd=5, el='FIRE', st='STR', pw=1.2, f='Огр плюётся огнём')
K('fireblast', 'Огненный взрыв', 'Fireblast', 'STUN', '✹', m=25, cd=10, el='FIRE', st='STR', pw=1.3, r=8, d=1.5, f='Удар огнём в лицо')
K('ogre_bloodlust', 'Жажда крови', 'Bloodlust', 'BUFF_ALLIES', 'Ѫ', m=30, cd=20, r=12, me='haste 15 2; spd 15 1', f='Огр колдует ярость')
K('multicast', 'Мультикаст', 'Multicast', 'RESET', '⁂', m=40, cd=60, me='luck 10 3', f='Вторая голова повторяет заклинание')
C('ogre_magi', 'Огр-чародей', 'Ogre Magi', 'SUPPORT', 0x5DADE2, 'STR', 'INT', 'support 2 magic 2 tank 1 risk 1',
  'ogre_ignite fireblast ogre_bloodlust iron_skin fire_field war_stomp multicast fury_totem',
  ('Две головы', 'health 10 crit_chance 5'), 'Двухголовый огр: одна голова колдует, вторая повторяет.', races='orc')

# 24. Тотемный резчик
K('ward_totem', 'Тотем защиты', 'Warding Totem', 'BUFF_ALLIES', '♜', m=30, cd=25, r=10, me='res 15 0; abs 15 1', f='Деревянный идол прикрывает отряд')
K('mana_totem', 'Тотем маны', 'Mana Totem', 'MEDITATE', '♈', m=0, cd=40, pw=0.4, f='Идол возвращает силы')
K('flame_totem', 'Огненный тотем', 'Flame Totem', 'ZONE_AT', '♨', m=35, cd=16, el='FIRE', pw=0.45, r=5, d=10, f='Тотем жжёт подходящих')
K('totem_circle', 'Круг тотемов', 'Totem Circle', 'ZONE', '۞', m=80, cd=80, el='NATURE', pw=-0.5, r=9, d=12, me='res 12 0', f='Все тотемы разом')
C('totem_carver', 'Тотемный резчик', 'Totem Carver', 'SUPPORT', 0x9C640C, 'INT', 'VIT', 'support 3 craft 2 nature 1',
  'spirit_lightning ward_totem flame_totem healing_totem mana_totem fury_totem storm_totem totem_circle',
  ('Резец', 'mana_regen 12 sell_price 5'), 'Режет идолов: тотемы защиты, маны, огня и круг тотемов.', races='orc beastkin', profs='herbalist')

# 25. Скальд
K('saga', 'Сага', 'Saga', 'BUFF_ALLIES', '♪', m=30, cd=25, r=14, me='str 15 0; res 15 0', f='Песнь о подвигах предков')
K('war_chant', 'Боевой напев', 'War Chant', 'TAUNT', '♫', m=20, cd=14, r=8, d=6, me='regen 6 0', foe='weak 6 1', f='Грубый напев злит врагов')
K('heroes_echo', 'Отзвук героев', 'Heroes\' Echo', 'HEAL', '♬', m=30, cd=12, pw=1.0, r=12, f='Песня, от которой затягиваются раны')
K('valhalla_call', 'Зов Вальхаллы', 'Call of Valhalla', 'WARD', '⚔', m=70, cd=80, r=16, d=10, f='Не сегодня, брат. Вальхалла подождёт')
C('skald', 'Скальд', 'Skald', 'SUPPORT', 0xD68910, 'STR', 'INT', 'support 3 party 2 melee 2',
  'throwing_axe war_chant saga heroes_echo song_of_valor roar valhalla_call ballad_of_heroes',
  ('Голос севера', 'health 4 xp 5'), 'Поёт в первом ряду: сага, боевой напев и зов Вальхаллы.', races='human dwarf orc')

# 26. Знаменосец
K('banner_courage', 'Знамя отваги', 'Banner of Courage', 'BUFF_ALLIES', '⚑', m=30, cd=25, r=12, me='str 15 1', f='Знамя поднято — руки крепче')
K('banner_iron', 'Знамя стойкости', 'Banner of Iron', 'BUFF_ALLIES', '⚐', m=30, cd=25, r=12, me='res 15 1', f='Под знаменем не страшно')
K('banner_swift', 'Знамя быстроты', 'Banner of Swiftness', 'BUFF_ALLIES', '⚲', m=30, cd=25, r=12, me='spd 15 1; haste 15 1', f='Лёгкие ноги и быстрые руки')
K('standard_bearer', 'Последнее знамя', 'Last Standard', 'ZONE', '♖', m=80, cd=90, el='HOLY', st='VIT', pw=-0.5, r=10, d=12, me='str 12 1; res 12 1',
  f='Пока знамя стоит, отряд живёт')
C('bannerman', 'Знаменосец', 'Bannerman', 'SUPPORT', 0xC0392B, 'VIT', 'STR', 'support 3 party 4',
  'banner_courage banner_iron banner_swift charge rallying_banner taunt war_cry standard_bearer',
  ('Древко', 'health 6 armor 1'), 'Несёт знамёна отряда: отвага, стойкость, быстрота.', races='human orc')

# 27. Полководец
K('command_attack', 'Приказ: в атаку!', 'Order: Attack', 'BUFF_ALLIES', '⚔', m=25, cd=20, r=16, me='str 10 1; spd 10 0', f='Отряд бросается вперёд')
K('command_mark', 'Приказ: цель!', 'Order: Target', 'MARK', '⊕', m=15, cd=10, r=40, d=15, f='Все бьют туда')
K('command_retreat', 'Приказ: отход!', 'Order: Fall Back', 'BUFF_ALLIES', '⇠', m=25, cd=25, r=16, me='spd 8 2; res 8 1', f='Организованный отход')
K('battle_plan', 'План битвы', 'Battle Plan', 'GATHER', '⚐', m=50, cd=60, r=48, me='res 6 1; str 6 0', f='Все ко мне — по плану')
C('warlord_tactician', 'Полководец', 'Tactician', 'SUPPORT', 0x1A5276, 'INT', 'STR', 'support 2 party 4 explore 1',
  'command_mark command_attack command_retreat eagle_eye drill_sergeant war_cry battle_plan rallying_banner',
  ('Стратег', 'xp 10 crit_chance 3'), 'Командует боем: приказы, метки целей и сбор отряда по плану.', races='human elf')

# 28. Маркитант
K('supply_drop', 'Припасы', 'Supply Drop', 'HEAL', '⛟', m=25, cd=12, pw=1.0, r=12, me='satur 2 0', f='Еда и бинты всем')
K('hired_blades', 'Наёмники', 'Hired Blades', 'SUMMON', '⚔', m=60, cd=70, sm='minecraft:vindicator', n=2, d=40, f='Деньги решают всё')
K('bribe', 'Подкуп', 'Bribe', 'CHARM', '¤', m=25, cd=25, r=8, d=10, f='Золото успокаивает лучше песни')
K('war_profiteer', 'Военная наживa', 'War Profits', 'BUFF_ALLIES', '♦', m=40, cd=45, r=12, me='luck 20 2; haste 20 0', f='Удача любит торговцев')
C('sutler', 'Маркитант', 'Sutler', 'SUPPORT', 0xB7950B, 'LUCK', 'INT', 'support 2 trade 4 party 1',
  'supply_drop bribe healing_flask war_profiteer elixir_of_might acid_flask hired_blades grand_transmutation',
  ('Торговая жилка', 'coin_find 30 sell_price 20'), 'Торговец при войске: припасы, подкуп и наёмники за звонкую монету.',
  races='human dwarf', profs='alchemist')

# 29. Полевой кашевар
K('hearty_stew', 'Сытная похлёбка', 'Hearty Stew', 'BUFF_ALLIES', '♨', m=25, cd=25, r=10, me='regen 10 1; satur 3 0; hp 30 1', f='Горячее для всех')
K('spicy_pepper', 'Острый перец', 'Hot Pepper', 'BUFF_ALLIES', '♣', m=20, cd=20, r=10, me='str 12 0; fireres 12 0', f='Жжёт язык и придаёт сил')
K('ladle_bonk', 'Половником по лбу', 'Ladle Bonk', 'STUN', '♠', m=15, cd=10, pw=0.7, r=4.5, d=1.5, f='Кашевар не любит, когда лезут к котлу')
K('feast', 'Пир', 'Feast', 'HEAL', '☕', m=70, cd=70, pw=3.0, r=14, me='regen 20 1; satur 5 0', f='Стол накрыт — раны забыты')
C('camp_cook', 'Полевой кашевар', 'Camp Cook', 'SUPPORT', 0xE67E22, 'VIT', 'LUCK', 'support 3 party 2 nature 2',
  'ladle_bonk hearty_stew spicy_pepper healing_flask fire_flask song_of_healing iron_skin feast',
  ('Сытый отряд', 'health 6 health_regen 1'), 'Лучший друг отряда: похлёбка лечит, перец придаёт сил, пир спасает.',
  races='dwarf human orc', profs='herbalist')

# 30. Травница
K('herbal_poultice', 'Травяная примочка', 'Herbal Poultice', 'TETHER', '❧', m=20, cd=8, el='NATURE', pw=1.2, r=12, d=10, f='Листья на рану')
K('nettle_patch', 'Крапива', 'Nettle Patch', 'ZONE_AT', '✵', m=25, cd=14, el='NATURE', pw=0.4, r=4, d=8, foe='slow 2 1', f='Жгучие заросли')
K('calming_tea', 'Успокаивающий чай', 'Calming Tea', 'CHARM', '☕', m=30, cd=30, r=7, d=8, f='Аромат ромашки')
K('wild_remedy', 'Дикое снадобье', 'Wild Remedy', 'HEAL', '❀', m=60, cd=45, pw=2.4, r=14, me='regen 10 1', f='Секретный рецепт бабки')
C('herbalist', 'Травница', 'Herbalist', 'SUPPORT', 0x52BE80, 'INT', 'LUCK', 'support 3 nature 3 craft 1',
  'thorn_bolt herbal_poultice nettle_patch calming_tea healing_flask purify healing_bloom wild_remedy',
  ('Знахарка', 'health_regen 1 loot 10'), 'Знает каждую траву: примочки, крапива и успокаивающий чай.',
  races='elf human', profs='herbalist')

# 31. Зачарователь
K('rune_of_power', 'Руна силы', 'Rune of Power', 'BUFF_ALLIES', 'ᚱ', m=30, cd=25, r=10, me='str 15 1', f='Руна вспыхивает на оружии')
K('rune_of_ward', 'Руна защиты', 'Rune of Warding', 'SHIELD', 'ᛉ', m=25, cd=10, pw=1.6, r=14, f='Руна на доспехе')
K('dispel', 'Развеять', 'Dispel', 'STEAL', '⊘', m=20, cd=10, el='ARCANE', pw=0.6, r=20, f='Снимает с врага чары')
K('grand_enchant', 'Великое зачарование', 'Grand Enchantment', 'BUFF_ALLIES', '✶', m=80, cd=90, r=14, me='str 20 1; res 20 1; haste 20 1; luck 20 1',
  f='Всё снаряжение отряда светится')
C('enchanter', 'Зачарователь', 'Enchanter', 'SUPPORT', 0x9B59B6, 'INT', 'LUCK', 'support 3 craft 3 magic 1',
  'mana_bolt rune_of_ward rune_of_power dispel enchanted_blade arcane_shield blessing grand_enchant',
  ('Рунная вязь', 'spell_power 5 mana 20'), 'Накладывает руны на оружие и доспехи всего отряда.', races='elf dwarf')

# 32. Часовщик
K('rewind', 'Перемотка', 'Rewind', 'RECALL', '⟲', m=30, cd=20, pw=0.25, f='Часовщик возвращается на три секунды назад')
K('haste_field', 'Поле ускорения', 'Haste Field', 'BUFF_ALLIES', '⏩', m=30, cd=25, r=10, me='haste 12 2; spd 12 1', f='Время для друзей бежит быстрее')
K('slow_field', 'Поле замедления', 'Slow Field', 'ZONE_AT', '⏪', m=30, cd=20, el='ARCANE', pw=0.2, r=5, d=8, foe='slow 2 3; fatigue 2 2', f='Время для врагов вязнет')
K('time_lock', 'Остановка часов', 'Time Lock', 'STASIS', '⌛', m=70, cd=70, r=5, d=4, f='Стрелки замирают')
C('clockmaker', 'Часовщик', 'Clockmaker', 'SUPPORT', 0xB8860B, 'INT', 'AGI', 'support 2 craft 2 magic 2',
  'mana_bolt haste_field slow_field rewind blink tempo time_lock time_warp',
  ('Точность', 'cooldown 10'), 'Шестерёнки времени: ускоряет друзей, замедляет врагов, перематывает себя.', races='dwarf human')

# 33. Отшельник
K('inner_peace', 'Внутренний покой', 'Inner Peace', 'HEAL', '☯', m=20, cd=8, pw=1.0, r=10, f='Покой передаётся другим')
K('chi_shield', 'Щит ци', 'Chi Shield', 'SHIELD', '◎', m=25, cd=10, pw=1.6, r=14, f='Ци обволакивает союзника')
K('hermits_staff', 'Посох отшельника', 'Hermit\'s Staff', 'STRIKE', '|', m=10, cd=3, st='AGI', pw=1.0, r=4.5, foe='slow 2 1', f='Лёгкий удар посохом')
K('enlightenment', 'Нирвана', 'Nirvana', 'ZONE', '☸', m=80, cd=90, el='HOLY', st='INT', pw=-0.6, r=10, d=8, me='regen 8 1', f='Мир вокруг затихает')
C('hermit', 'Отшельник', 'Hermit', 'SUPPORT', 0xD4E6F1, 'AGI', 'INT', 'support 3 solo 2 explore 1',
  'hermits_staff inner_peace chi_shield meditation swift_step chi_wave iron_body enlightenment',
  ('Аскеза', 'mana_regen 15 dodge 5'), 'Мудрец с горы: покой, щит ци и нирвана для всего отряда.', races='human beastkin')

# 34. Жрица Луны
K('moonlight', 'Лунный свет', 'Moonlight', 'HEAL', '☾', m=20, cd=6, pw=1.0, r=12, f='Луна лечит своих детей')
K('starfall', 'Звездопад', 'Starfall', 'NOVA', '✧', m=35, cd=12, el='ARCANE', pw=1.0, r=7, f='Звёзды падают вокруг')
K('moon_glaive', 'Лунная глефа', 'Moon Glaive', 'CHAIN', '☽', m=20, cd=5, el='ARCANE', st='AGI', pw=1.0, r=6, n=4, f='Глефа отскакивает от врагов')
K('eclipse_shroud', 'Лунное затмение', 'Lunar Eclipse', 'BUFF_ALLIES', '◐', m=80, cd=80, r=14, me='invis 10 0; regen 10 1; res 10 1', f='Ночь укрывает отряд')
C('moon_priestess', 'Жрица Луны', 'Moon Priestess', 'SUPPORT', 0xAED6F1, 'INT', 'AGI', 'support 3 night 3 ranged 1',
  'moon_glaive moonlight starfall leap_back hunters_mark blessing healing_bloom eclipse_shroud',
  ('Дитя ночи', 'mana_regen 10', 'night_eye'), 'Сильна под луной: глефа, звездопад и затмение для отряда.', races='elf', faction='elves')

# 35. Хранительница рощи
K('grove_wolves', 'Волки рощи', 'Grove Wolves', 'SUMMON', '♞', m=40, cd=40, sm='minecraft:wolf', n=2, d=60, f='Серые защитники леса')
K('regrowth', 'Восстановление', 'Regrowth', 'HEAL_TARGET', '❧', m=25, cd=6, pw=1.5, r=16, me='regen 6 0', f='Росток на ране')
K('grove_bear', 'Медведь рощи', 'Grove Bear', 'SUMMON', 'ʘ', m=60, cd=70, sm='minecraft:polar_bear', n=1, d=60, f='Старый медведь приходит на зов')
K('wild_growth', 'Буйство жизни', 'Wild Growth', 'HEAL', '✿', m=50, cd=30, pw=1.8, r=14, me='regen 8 1', f='Всё вокруг цветёт')
C('grove_keeper', 'Хранительница рощи', 'Grove Keeper', 'SUPPORT', 0x229954, 'INT', 'VIT', 'support 2 beast 3 nature 3',
  'thorn_bolt regrowth grove_wolves entangle wild_growth pack_fury grove_bear tranquility',
  ('Зов рощи', 'dmg_beast 10 health_regen 1'), 'Звери леса сражаются за неё, а она лечит и их, и отряд.', races='elf beastkin', faction='wilds')

# 36. Плакальщица
K('mourning_song', 'Плач', 'Lament', 'HEAL', '♭', m=25, cd=8, pw=1.0, r=12, f='Скорбная песнь лечит живых')
K('spirit_ward', 'Духовная завеса', 'Spirit Veil', 'SHIELD', '☁', m=25, cd=10, pw=1.5, r=16, f='Духи прикрывают союзника')
K('wail', 'Вой баньши', 'Banshee Wail', 'NOVA', '♯', m=30, cd=14, el='SHADOW', pw=0.8, r=6, foe='weak 5 1; slow 3 1', f='Вопль леденит кровь')
K('last_rites', 'Последний обряд', 'Last Rites', 'WARD', '✝', m=60, cd=60, r=20, d=10, f='Обряд не пускает душу уйти')
C('mourner', 'Плакальщица', 'Mourner', 'SUPPORT', 0x5D6D7E, 'INT', 'VIT', 'support 3 undead 2 night 1',
  'shadow_bolt mourning_song spirit_ward wail soul_siphon purify last_rites sanctuary',
  ('Говорящая с мёртвыми', 'res_undead 20 mana_regen 10'), 'Слышит мёртвых: плач лечит, вопль ослабляет, обряд держит душу.',
  races='human elf')

# 37. Провидец
K('foresight', 'Предвидение', 'Foresight', 'BUFF_ALLIES', '◉', m=25, cd=25, r=12, me='luck 15 1; res 15 0', f='Провидец видит удар раньше врага')
K('omen', 'Знамение', 'Omen', 'REVEAL', '⊙', m=20, cd=20, r=32, d=15, f='Тайное становится явным')
K('fate_thread', 'Нить судьбы', 'Thread of Fate', 'SWAP', '∿', m=25, cd=16, r=24, f='Провидец меняет судьбы местами')
K('prophecy', 'Пророчество', 'Prophecy', 'STASIS', '✡', m=70, cd=70, r=5, d=3.5, f='Всё случится, как сказано')
C('seer', 'Провидец', 'Seer', 'SUPPORT', 0xBB8FCE, 'INT', 'LUCK', 'support 2 magic 2 explore 2',
  'mana_bolt foresight omen fate_thread blessing false_promise prophecy time_warp',
  ('Третий глаз', 'loot 15 crit_chance 3'), 'Видит судьбы: знамения, предвидение и пророчество, которое замораживает время.',
  races='elf human')

# 38. Иллюзионист
K('mirror_veil', 'Зеркальная завеса', 'Mirror Veil', 'BUFF_ALLIES', '◫', m=30, cd=30, r=10, me='invis 6 0; spd 6 1', f='Отряд исчезает в отражениях')
K('phantom_double', 'Фантом', 'Phantom Double', 'SUMMON', '♊', m=40, cd=40, sm='minecraft:vex', n=2, d=20, f='Призрачные двойники отвлекают')
K('confusion', 'Морок', 'Confusion', 'NOVA', '⁇', m=25, cd=14, el='ARCANE', pw=0.2, r=7, foe='nausea 6 0; slow 4 1; blind 2 0', f='Враги не понимают, где они')
K('grand_illusion', 'Великая иллюзия', 'Grand Illusion', 'CHARM', '✺', m=60, cd=60, r=12, d=10, f='Враги видят друзей вместо отряда')
C('illusionist', 'Иллюзионист', 'Illusionist', 'SUPPORT', 0xD2B4DE, 'INT', 'AGI', 'support 2 stealth 2 magic 2',
  'mana_bolt confusion mirror_veil shadow_swap phantom_double blink grand_illusion time_warp',
  ('Обман зрения', 'dodge 10'), 'Мастер обмана: морок, фантомы, завеса и великая иллюзия.', races='elf demonkin')

# 39. Медиум
K('seance', 'Сеанс', 'Seance', 'SUMMON', '♅', m=40, cd=40, sm='minecraft:vex', n=2, d=30, f='Духи откликаются на зов')
K('banish', 'Изгнание', 'Banish', 'PUSH', '⊗', m=25, cd=12, el='HOLY', pw=0.6, r=7, foe='weak 4 1', f='Прочь отсюда!')
K('possession', 'Одержимость', 'Possession', 'DOMINATE', '☯', m=50, cd=50, r=14, d=30, f='Дух вселяется во врага')
K('spirit_mend', 'Целение духов', 'Spirit Mending', 'TETHER', '☁', m=20, cd=8, el='HOLY', pw=1.2, r=16, d=10, f='Духи шьют раны союзника')
C('medium', 'Медиум', 'Medium', 'SUPPORT', 0x7FB3D5, 'INT', 'LUCK', 'support 2 undead 2 magic 2',
  'shadow_bolt spirit_mend banish seance possession spirit_link purify ghost_legion',
  ('Шёпот духов', 'res_magic 10 mana_regen 10'), 'Говорит с духами: они лечат, служат и вселяются во врагов.', races='human demonkin')

# 40. Абъюратор
K('ward_of_force', 'Силовой оберег', 'Ward of Force', 'SHIELD', '◇', m=20, cd=6, pw=1.5, r=18, f='Невидимая стенка перед союзником')
K('mass_ward', 'Общий оберег', 'Mass Ward', 'BUFF_ALLIES', '◈', m=40, cd=30, r=12, me='abs 10 3', f='Щит на каждого')
K('counterspell', 'Контрзаклятье', 'Counterspell', 'STEAL', '⊘', m=20, cd=12, el='ARCANE', pw=0.5, r=20, f='Чары врага рассыпаются')
K('sanctum', 'Неприступное убежище', 'Sanctum', 'ZONE', '◯', m=80, cd=90, el='HOLY', st='INT', pw=-0.4, r=7, d=10, me='res 10 2', f='Купол, в который не войти')
C('abjurer', 'Абъюратор', 'Abjurer', 'SUPPORT', 0x85C1E9, 'INT', 'VIT', 'support 3 magic 2 tank 1',
  'mana_bolt ward_of_force counterspell mass_ward arcane_shield guardian_spirit spell_parry sanctum',
  ('Мастер оберегов', 'res_magic 15 mana 20'), 'Маг защиты: силовые обереги, контрзаклятья и купол-убежище.', races='elf human')

# 41. Пастух
K('herd_call', 'Зов стада', 'Herd Call', 'SUMMON', '♉', m=35, cd=40, sm='minecraft:goat', n=3, d=40, f='Козлы бодают врагов')
K('shepherd_crook', 'Пастуший посох', 'Crook Pull', 'PULL', '⌐', m=15, cd=8, r=12, f='Посох тянет к себе')
K('beast_mend', 'Целение зверей', 'Mend Beasts', 'HEAL', '♥', m=25, cd=10, pw=1.2, r=14, f='Лечит и людей, и зверей')
K('good_shepherd', 'Добрый пастырь', 'Good Shepherd', 'BUFF_ALLIES', '♖', m=60, cd=60, r=16, me='res 15 1; regen 15 1; spd 15 0', f='Никто из стада не пропадёт')
C('shepherd', 'Пастух', 'Shepherd', 'SUPPORT', 0xC5A572, 'VIT', 'INT', 'support 2 beast 4 nature 1',
  'shepherd_crook beast_mend herd_call call_wolf pack_fury bear_trap good_shepherd pack',
  ('Чутьё на зверей', 'dmg_beast 10 health 4'), 'Ведёт стадо и стаю: козлы бодают, волки кусают, все целы.',
  races='beastkin human', profs='hunter')

# 42. Повелитель ветров
K('tailwind', 'Попутный ветер', 'Tailwind', 'BUFF_ALLIES', '≫', m=20, cd=16, r=12, me='spd 10 2; jump 10 1', f='Ветер в спину')
K('gale_push', 'Шквал', 'Gale', 'PUSH', '≋', m=20, cd=8, el='ARCANE', pw=0.5, r=8, f='Порыв ветра сносит врагов')
K('cyclone', 'Циклон', 'Cyclone', 'LAUNCH', '@', m=30, cd=14, el='LIGHTNING', pw=0.6, r=5, f='Вихрь поднимает врагов')
K('eye_of_storm', 'Око бури', 'Eye of the Storm', 'VORTEX', '◎', m=80, cd=80, el='LIGHTNING', pw=0.6, r=9, d=6, f='Буря кружит, а в центре — тишина')
C('windlord', 'Повелитель ветров', 'Windlord', 'SUPPORT', 0xA9CCE3, 'INT', 'AGI', 'support 2 magic 2 explore 2',
  'spark tailwind gale_push cyclone blink swift_step chain_lightning eye_of_storm',
  ('Лёгкость', 'move_speed 10', 'featherfall'), 'Ветер служит ему: попутный ветер друзьям, шквал и циклон врагам.', races='elf human')

# 43. Целитель вод
K('healing_rain', 'Целебный дождь', 'Healing Rain', 'ZONE_AT', '☂', m=40, cd=20, el='NATURE', pw=-0.35, r=7, d=10, f='Тёплый дождь лечит отряд')
K('bubble', 'Пузырь', 'Bubble', 'SHIELD', '○', m=20, cd=8, pw=1.5, r=16, me='water 20 0', f='Водяной пузырь вокруг союзника')
K('tidal_wave', 'Приливная волна', 'Tidal Wave', 'PUSH', '≈', m=30, cd=14, el='FROST', pw=0.8, r=8, foe='slow 3 1', f='Волна смывает врагов')
K('spring_of_life', 'Родник жизни', 'Spring of Life', 'HEAL', '♒', m=70, cd=70, pw=3.0, r=14, me='regen 10 1; dolphin 10 0', f='Чистая вода из земли')
C('tidecaller', 'Целитель вод', 'Tidecaller', 'SUPPORT', 0x3498DB, 'INT', 'VIT', 'support 3 water 4',
  'gush bubble healing_rain tidal_wave heal purify spring_of_life tranquility',
  ('Дитя воды', 'mana_regen 10', 'water_breathing'), 'Вода лечит и защищает: дождь, пузыри и родник жизни.', races='elf human')

# 44. Рунный кузнец
K('forge_rune', 'Кованая руна', 'Forged Rune', 'BUFF_ALLIES', 'ᚦ', m=30, cd=25, r=10, me='res 15 1; str 15 0', f='Руна, выбитая на металле')
K('sparks', 'Искры горна', 'Forge Sparks', 'CONE', '✶', m=15, cd=4, el='FIRE', st='STR', pw=0.9, r=5, f='Сноп искр в лицо')
K('quench', 'Закалка водой', 'Quench', 'HEAL_TARGET', '♒', m=20, cd=6, pw=1.3, r=14, me='res 4 0', f='Пар шипит — рана затянулась')
K('masterwork', 'Шедевр', 'Masterwork', 'BUFF_ALLIES', '⚒', m=80, cd=90, r=14, me='str 20 1; res 20 1; abs 20 3', f='Лучшая работа мастера — на каждом')
C('runesmith', 'Рунный кузнец', 'Runesmith', 'SUPPORT', 0xCA6F1E, 'STR', 'INT', 'support 2 craft 4 tank 1',
  'forge_hammer sparks forge_rune quench temper_armor rune_of_ward anvil_drop masterwork',
  ('Кузница рун', 'armor 1 sell_price 10 mining 10'), 'Кует руны прямо на бронях отряда и роняет наковальни.',
  races='dwarf', profs='blacksmith miner')
