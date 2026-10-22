"""Танки: принимают удары, держат врагов и прикрывают отряд."""
from .dsl import K, C

# 1. Латник
K('plate_slam', 'Удар латной перчаткой', 'Gauntlet Slam', 'STRIKE', '✊', m=15, cd=5, pw=1.1, r=4.5, foe='slow 2 1', f='Кулак в стальной перчатке')
K('steel_bulwark', 'Стальной оплот', 'Steel Bulwark', 'BUFF_SELF', '▣', m=35, cd=30, me='res 10 2; slow 10 0', fl='reflect', d=10,
  f='Латник встаёт столбом — железо отражает удары')
K('rivet_storm', 'Град заклёпок', 'Rivet Storm', 'CONE', '⁙', m=25, cd=9, pw=0.9, r=6, foe='slow 3 1', f='Из сочленений брони летят заклёпки')
K('juggernaut_march', 'Поступь махины', 'Juggernaut March', 'DASH_STRIKE', '⇶', m=45, cd=25, pw=1.6, r=3.4, foe='slow 4 3; weak 4 1',
  f='Ничто не остановит идущего латника')
C('ironclad', 'Латник', 'Ironclad', 'TANK', 0x95A5A6, 'VIT', 'STR', 'tank 4 melee 2 craft 1',
  'plate_slam taunt rivet_storm iron_skin steel_bulwark sunder juggernaut_march last_stand',
  ('Сплав и пот', 'armor 4 res_projectile 20'), 'Ходячая кузница: чем тяжелее броня, тем спокойнее отряд.',
  races='dwarf human', profs='blacksmith')

# 2. Храмовник
K('templar_oath', 'Обет храмовника', 'Templar Oath', 'TAUNT', '✠', m=25, cd=14, r=10, d=8, me='res 8 0; regen 8 0', foe='glow 8 0',
  f='Клятва вызывает нечисть на бой')
K('sacred_ground', 'Святая земля', 'Sacred Ground', 'ZONE', '۞', m=50, cd=35, el='HOLY', st='VIT', pw=0.35, r=6, d=12,
  f='Камень под ногами светится')
K('crusader_strike', 'Удар крестоносца', 'Crusader Strike', 'STRIKE', '✝', m=18, cd=5, el='HOLY', st='STR', pw=1.3, r=4.5, fl='steal',
  f='Свет возвращает силы бьющему')
K('wings_of_faith', 'Крылья веры', 'Wings of Faith', 'BUFF_ALLIES', '♆', m=70, cd=80, r=12, me='res 12 1; regen 12 1; slowfall 12 0',
  f='Над отрядом раскрываются крылья света')
C('templar', 'Храмовник', 'Templar', 'TANK', 0xFFF8DC, 'VIT', 'STR', 'tank 3 undead 3 party 1',
  'crusader_strike templar_oath holy_strike sacred_ground shield_wall reveal_heresy divine_shield wings_of_faith',
  ('Броня веры', 'res_undead 20 health 6'), 'Монах-воин ордена: держит нежить на себе и освящает землю.',
  races='human dwarf', faction='kingdom')

# 3. Фалангист
K('spear_thrust', 'Укол копьём', 'Spear Thrust', 'BEAM', '↗', m=12, cd=3, pw=1.1, r=5, f='Длинное древко бьёт через плечо союзника')
K('phalanx', 'Фаланга', 'Phalanx', 'BUFF_ALLIES', '⫼', m=40, cd=35, r=8, me='res 12 1; abs 12 1', f='Щиты смыкаются стеной')
K('spear_wall', 'Частокол копий', 'Spear Wall', 'PUSH', '⋔', m=30, cd=12, pw=0.9, r=6, foe='slow 3 2', f='Ряд наконечников отбрасывает врага')
K('impale', 'Насадить', 'Impale', 'STUN', '⫯', m=30, cd=16, pw=1.4, r=5, d=2, f='Удар пригвождает врага к земле')
C('phalangite', 'Фалангист', 'Phalangite', 'TANK', 0xB7950B, 'VIT', 'STR', 'tank 3 melee 2 party 2',
  'spear_thrust taunt spear_wall phalanx impale shield_bash fortress last_stand',
  ('Сомкнуть ряды', 'armor 2 res_projectile 25'), 'Копьё и большой щит: держит линию и не пропускает никого.', races='human')

# 4. Бастион
K('siege_mode', 'Осадный режим', 'Siege Mode', 'BUFF_SELF', '⛫', m=40, cd=30, me='res 8 3; slow 8 4; str 8 1', f='Врасти в землю и стать крепостью')
K('mortar_shot', 'Мортирный выстрел', 'Mortar Shot', 'METEOR', '◍', m=40, cd=14, st='VIT', pw=1.6, r=4, f='Ядро по навесной')
K('barricade', 'Баррикада', 'Barricade', 'WALL', '▥', m=30, cd=22, r=6, d=15, wall='minecraft:iron_bars', f='Решётки из ниоткуда')
K('last_bastion', 'Последний бастион', 'Last Bastion', 'ZONE', '⛨', m=80, cd=90, el='HOLY', st='VIT', pw=-0.3, r=7, d=10,
  me='res 10 1', f='Крепость защищает всех внутри')
C('bastion', 'Бастион', 'Bastion', 'TANK', 0x5D6D7E, 'VIT', 'INT', 'tank 4 craft 1 party 1',
  'siege_mode taunt mortar_shot barricade quake guardian_ward fortress last_bastion',
  ('Крепостная кладка', 'armor 3 res_explosion 30'), 'Человек-крепость: стоит на месте и не сдвигается ни на шаг.',
  races='dwarf')

# 5. Каменный страж
K('stone_fist', 'Каменный кулак', 'Stone Fist', 'STRIKE', '◼', m=15, cd=5, el='EARTH', st='VIT', pw=1.2, r=4.5, foe='slow 2 1',
  f='Рука на миг становится скалой')
K('golem_call', 'Пробуждение голема', 'Awaken Golem', 'SUMMON', '♜', m=60, cd=70, sm='minecraft:iron_golem', n=1, d=60,
  f='Камень встаёт и идёт за стражем')
K('granite_skin', 'Гранитная кожа', 'Granite Skin', 'BUFF_SELF', '▩', m=35, cd=30, me='res 12 1; abs 12 3', f='Кожа серая и твёрдая')
K('rock_tomb', 'Каменная гробница', 'Rock Tomb', 'STASIS', '⛰', m=60, cd=45, r=3, d=3, f='Камни смыкаются над врагами')
C('stone_warden', 'Каменный страж', 'Stone Warden', 'TANK', 0x7B7D7D, 'VIT', 'INT', 'tank 3 mining 3 magic 1',
  'stone_fist taunt earth_wall granite_skin quake rock_tomb golem_call rock_fall',
  ('Сердце горы', 'armor 3 health 6'), 'Говорит с камнем: каменная кожа, гробницы и големы.', races='dwarf', profs='miner')

# 6. Вождь клана
K('clan_horn', 'Рог клана', 'Clan Horn', 'BUFF_ALLIES', '♉', m=30, cd=25, r=14, me='str 12 0; spd 12 0', f='Протяжный рог зовёт родичей')
K('head_butt', 'Удар головой', 'Head Butt', 'STUN', '◕', m=20, cd=12, pw=0.9, r=4, d=1.5, f='Грубо, но действенно')
K('chieftain_challenge', 'Вызов вождя', 'Chieftain\'s Challenge', 'TAUNT', '⚔', m=30, cd=18, r=10, d=8, me='str 8 1; res 8 0',
  foe='weak 6 1', f='Вождь вызывает сильнейшего')
K('ancestors_rally', 'Сбор предков', 'Rally of Ancestors', 'BUFF_ALLIES', '♖', m=80, cd=90, r=14, me='str 15 1; res 15 1; hp 15 2; regen 15 0',
  f='Духи предков встают рядом с живыми')
C('chieftain', 'Вождь клана', 'Clan Chieftain', 'TANK', 0x8E5A2B, 'VIT', 'STR', 'tank 2 party 3 melee 2',
  'head_butt chieftain_challenge clan_horn roar war_stomp rallying_banner unstoppable ancestors_rally',
  ('Голос рода', 'health 8 xp 5'), 'Ведёт клан в бой: рог, вызов и сбор предков.', races='orc beastkin', faction='orcs')

# 7. Мясник
K('meat_hook', 'Мясной крюк', 'Meat Hook', 'PULL', '⚓', m=25, cd=12, r=22, foe='slow 2 3', f='Цепь со свистом летит в толпу')
K('rot', 'Гниль', 'Rot', 'ZONE', '☣', m=0, cd=12, el='POISON', st='VIT', pw=0.5, r=4, d=6, hp=0.06, foe='slow 2 1',
  f='Вонь, от которой слезятся глаза')
K('dismember', 'Расчленение', 'Dismember', 'MULTI_STRIKE', '✂', m=40, cd=25, st='VIT', pw=0.5, r=4.5, n=6, fl='steal',
  f='Мясник пирует прямо в бою')
K('flesh_heap', 'Груда плоти', 'Flesh Heap', 'BUFF_SELF', '♥', m=30, cd=40, me='hp 20 3; regen 10 1', f='Каждая жертва делает толще')
C('butcher', 'Мясник', 'Butcher', 'TANK', 0x7B241C, 'VIT', 'STR', 'tank 3 melee 2 risk 2',
  'meat_hook rot sunder flesh_heap dismember roar iron_skin last_stand',
  ('Толстая шкура', 'health 12 lifesteal 3'), 'Цепь, крюк и запах гнили. Затягивает врагов и пожирает их.', races='orc')

# 8. Колосс
K('titan_grip', 'Хватка титана', 'Titan Grip', 'STUN', '☝', m=25, cd=14, st='VIT', pw=1.0, r=5, d=2, f='Огромная ладонь сжимает врага')
K('colossus_stomp', 'Топот колосса', 'Colossus Stomp', 'NOVA', '⊚', m=30, cd=10, st='VIT', pw=1.0, r=6, foe='slow 3 2',
  f='Земля дрожит под ногой')
K('giant_growth', 'Рост гиганта', 'Giant Growth', 'BUFF_SELF', '⇧', m=50, cd=60, me='hp 20 4; str 20 1; res 20 0; slow 20 0',
  f='Колосс становится ещё больше')
K('avalanche_fall', 'Падение колосса', 'Colossus Fall', 'LEAP_SLAM', '⇓', m=60, cd=35, st='VIT', pw=2.4, r=6, foe='slow 4 3',
  f='Он падает сверху, как скала')
C('colossus', 'Колосс', 'Colossus', 'TANK', 0x6E2C00, 'VIT', 'VIT', 'tank 4 melee 1 risk 1',
  'colossus_stomp taunt titan_grip ground_breaker iron_skin giant_growth avalanche_fall last_stand',
  ('Неподъёмный', 'health 16'), 'Гора мышц: вдвое больше здоровья и толчки, сшибающие с ног.', races='orc dwarf')

# 9. Медвежий страж
K('maul', 'Трёпка', 'Maul', 'STRIKE', 'ʘ', m=12, cd=4, st='VIT', pw=1.2, r=4.5, foe='weak 3 0', f='Медвежья лапа')
K('thick_hide', 'Толстая шкура', 'Thick Hide', 'BUFF_SELF', '◒', m=30, cd=30, me='res 12 1; hp 12 2', f='Шерсть как кольчуга')
K('swipe', 'Размах когтей', 'Swipe', 'NOVA', '⋒', m=20, cd=7, st='VIT', pw=0.8, r=4, fl='perfoe', f='Медведь машет лапами во все стороны')
K('great_roar', 'Великий рёв', 'Great Roar', 'TAUNT', 'Ѫ', m=40, cd=25, r=14, d=10, me='res 10 1; regen 10 1', foe='weak 8 1; slow 4 1',
  f='Рёв, от которого стынет кровь')
C('ursine_guardian', 'Медвежий страж', 'Ursine Guardian', 'TANK', 0x8B5A2B, 'VIT', 'STR', 'tank 3 nature 3 beast 1',
  'maul swipe great_roar thick_hide bear_form entangle call_bear tranquility',
  ('Дух медведя', 'health 10 health_regen 1'), 'Друид-оборотень, что держит строй в медвежьей шкуре.', races='beastkin elf', faction='wilds')

# 10. Древень
K('root_snare', 'Корни-путы', 'Root Snare', 'STUN', '❦', m=25, cd=14, el='NATURE', st='VIT', pw=0.4, r=18, d=3, f='Корни хватают за ноги')
K('bark_armor', 'Кора', 'Barkskin', 'BUFF_SELF', '♣', m=30, cd=25, me='res 12 1; regen 12 1', f='Кожа становится корой')
K('living_armor', 'Живая броня', 'Living Armor', 'SHIELD', '❧', m=30, cd=14, pw=1.6, r=16, me='regen 8 0', f='Ветви обвивают союзника')
K('overgrowth', 'Буйный рост', 'Overgrowth', 'STASIS', '❀', m=70, cd=60, r=7, d=4, f='Лес вырастает за миг и держит всех')
C('treant', 'Древень', 'Treant', 'TANK', 0x4E7F2A, 'VIT', 'INT', 'tank 3 nature 4',
  'root_snare bark_armor living_armor thorn_wall entangle healing_bloom tranquility overgrowth',
  ('Древесная плоть', 'health 8 res_fire -10 health_regen 2'), 'Живое дерево: корни держат врагов, ветви берегут союзников. Огня боится.',
  races='elf', faction='elves')

# 11. Ледяной великан
K('glacier_club', 'Ледяная дубина', 'Glacier Club', 'STRIKE', '❄', m=15, cd=5, el='FROST', st='VIT', pw=1.2, r=4.5, foe='slow 3 2',
  f='Глыба льда вместо оружия')
K('frost_skin', 'Иней на коже', 'Rime Skin', 'BUFF_SELF', '❆', m=30, cd=30, me='res 10 1; abs 10 2', fl='reflect', d=10,
  f='Удары по великану морозят руки')
K('avalanche', 'Лавина', 'Avalanche', 'CONE', '⛰', m=35, cd=12, el='FROST', st='VIT', pw=1.1, r=8, foe='slow 4 3', f='Снег и лёд сходят на врагов')
K('permafrost', 'Вечная мерзлота', 'Permafrost', 'ZONE', '✱', m=60, cd=45, el='FROST', st='VIT', pw=0.4, r=7, d=10, foe='slow 2 3',
  f='Земля вокруг промерзает насквозь')
C('frost_giant', 'Ледяной великан', 'Frost Giant', 'TANK', 0xAED6F1, 'VIT', 'INT', 'tank 3 magic 1 water 1 solo 1',
  'glacier_club frost_skin avalanche ice_wall taunt deep_freeze permafrost ice_prison',
  ('Холодная кровь', 'health 8 res_fire -15', 'frost_touch'), 'Сын вечных льдов: морозит всё, что к нему прикасается.',
  races='dwarf orc')

# 12. Хранитель глубин
K('anchor_smash', 'Удар якорем', 'Anchor Smash', 'NOVA', '⚓', m=25, cd=8, st='VIT', pw=1.1, r=4.5, foe='weak 4 1', f='Ржавый якорь сносит всех')
K('gush', 'Струя', 'Gush', 'BOLT', '≈', m=15, cd=4, el='FROST', st='VIT', pw=1.0, foe='slow 3 2', f='Поток солёной воды')
K('kraken_shell', 'Панцирь кракена', 'Kraken Shell', 'BUFF_SELF', '◎', m=30, cd=35, me='res 15 1; water 15 0; dolphin 15 0',
  f='Ракушки нарастают на плечах')
K('ravage', 'Опустошение', 'Ravage', 'LAUNCH', 'ψ', m=80, cd=60, el='FROST', st='VIT', pw=1.4, r=9, foe='slow 4 3',
  f='Щупальца из-под земли подбрасывают врагов')
C('tidewarden', 'Хранитель глубин', 'Tidewarden', 'TANK', 0x1F618D, 'VIT', 'STR', 'tank 3 water 4',
  'gush anchor_smash kraken_shell taunt magnetic_pull fortress ravage last_stand',
  ('Сын моря', 'health 6', 'water_breathing'), 'Бродит по дну: якорь, струя и щупальца кракена.', races='beastkin human')

# 13. Живой доспех
K('rune_plating', 'Рунные пластины', 'Rune Plating', 'BUFF_SELF', '᛭', m=30, cd=30, me='abs 15 3; res 15 0', f='Руны вспыхивают на латах')
K('hollow_charge', 'Пустой натиск', 'Hollow Charge', 'DASH_STRIKE', '»', m=20, cd=8, st='VIT', pw=1.1, r=2.4, foe='slow 2 2',
  f='Пустой доспех летит вперёд')
K('soul_bind', 'Узы души', 'Soul Bind', 'SHIELD', '∞', m=35, cd=16, pw=1.8, r=14, f='Дух доспеха прикрывает союзника')
K('rebuild', 'Пересборка', 'Reassemble', 'HEAL_SELF', '⟲', m=40, cd=60, pw=0.6, me='res 4 2', f='Латы собираются заново')
C('living_armor', 'Живой доспех', 'Living Armor', 'TANK', 0x566573, 'VIT', 'INT', 'tank 4 magic 1 undead 1',
  'hollow_charge taunt rune_plating soul_bind iron_skin anti_magic_shell rebuild last_stand',
  ('Пустота внутри', 'armor 3 res_magic 15', 'mana_shield'), 'Доспех, в котором живёт душа. Мана гасит удары.',
  races='demonkin human')

# 14. Кровавый рыцарь
K('blood_strike', 'Кровавый удар', 'Blood Strike', 'STRIKE', '♦', m=0, cd=4, st='STR', pw=1.5, r=4.5, hp=0.04, fl='steal',
  f='Платит кровью и забирает её же')
K('crimson_aura', 'Багровая аура', 'Crimson Aura', 'ZONE', '◉', m=0, cd=20, el='SHADOW', st='VIT', pw=0.4, r=5, d=8, fl='steal', hp=0.08,
  f='Кровь врагов течёт к рыцарю')
K('vampiric_taunt', 'Зов крови', 'Call of Blood', 'TAUNT', '♁', m=20, cd=14, r=10, d=8, me='regen 6 1', f='Враги чуют свежую кровь')
K('blood_pact', 'Кровавый пакт', 'Blood Pact', 'BUFF_SELF', '♰', m=0, cd=60, hp=0.2, me='str 15 2; res 15 1; regen 15 2',
  f='Пятая часть крови — за силу демона')
C('blood_knight', 'Кровавый рыцарь', 'Blood Knight', 'TANK', 0x922B21, 'VIT', 'STR', 'tank 2 melee 2 risk 3',
  'blood_strike vampiric_taunt crimson_aura bloodbath unstoppable death_grip blood_pact last_stand',
  ('Жажда жизни', 'lifesteal 6 health 6'), 'Платит здоровьем за силу и тут же пьёт его из врагов.', races='demonkin human')

# 15. Страж ужаса
K('dread_strike', 'Удар ужаса', 'Dread Strike', 'STRIKE', '☗', m=15, cd=5, el='SHADOW', st='STR', pw=1.2, r=4.5, foe='weak 4 1',
  f='Тьма давит на врага')
K('terror', 'Ужас', 'Terror', 'TAUNT', '☠', m=30, cd=18, r=10, d=8, foe='weak 8 1; slow 4 1; darkness 4 0', f='Враги цепенеют, но идут на стража')
K('shadow_armor', 'Доспех тьмы', 'Shadow Armor', 'BUFF_SELF', '◘', m=30, cd=30, me='abs 12 3; res 12 0; invis 3 0', f='Тени облекают тело')
K('eclipse', 'Затмение', 'Eclipse', 'NOVA', '◑', m=60, cd=45, el='SHADOW', st='VIT', pw=1.4, r=8, foe='blind 4 0; weak 6 2',
  f='На миг гаснет солнце')
C('dread_guardian', 'Страж ужаса', 'Dread Guardian', 'TANK', 0x283747, 'VIT', 'STR', 'tank 3 night 3 stealth 1',
  'dread_strike terror shadow_armor fear death_grip aura_of_decay anti_magic_shell eclipse',
  ('Ночная броня', 'armor 2 res_magic 10', 'night_eye'), 'Внушает ужас: враги слабеют, но бегут прямо на него.',
  races='demonkin elf')

# 16. Королевский гвардеец
K('guard_swap', 'Подмена', 'Bodyguard Swap', 'SWAP', '⇆', m=20, cd=14, r=18, me='res 4 1', f='Гвардеец встаёт на место союзника')
K('royal_shield', 'Королевский щит', 'Royal Shield', 'SHIELD', '♔', m=30, cd=12, pw=2.0, r=16, f='Щит с гербом закрывает раненого')
K('halberd_sweep', 'Взмах алебарды', 'Halberd Sweep', 'CONE', '⚚', m=20, cd=6, pw=1.1, r=5, f='Широкий удар древковым оружием')
K('kings_guard', 'Стража короля', 'King\'s Guard', 'WARD', '♚', m=60, cd=70, r=16, d=12, f='Гвардеец клянётся, что союзник не умрёт')
C('royal_guard', 'Королевский гвардеец', 'Royal Guard', 'TANK', 0x2E86C1, 'VIT', 'STR', 'tank 3 party 3 support 1',
  'halberd_sweep taunt royal_shield guard_swap shield_wall sunder fortress kings_guard',
  ('Присяга', 'armor 2 health 4 res_illager 15'), 'Телохранитель: подменяет раненых, ставит щиты и клянётся спасти.',
  races='human', faction='kingdom')

# 17. Центурион
K('pilum', 'Пилум', 'Pilum', 'ARROWS', '↟', m=15, cd=6, st='STR', pw=2.0, n=1, foe='slow 3 1', f='Тяжёлый дротик застревает в щите')
K('testudo', 'Черепаха', 'Testudo', 'BUFF_ALLIES', '⊞', m=40, cd=40, r=8, me='res 10 2; slow 10 1', f='Строй закрылся щитами со всех сторон')
K('gladius_jab', 'Укол гладиусом', 'Gladius Jab', 'STRIKE', '†', m=10, cd=3, pw=1.0, r=4, combo=1, f='Короткий меч из-за щита')
K('legion_charge', 'Натиск легиона', 'Legion Charge', 'DASH_STRIKE', '⇉', m=40, cd=20, pw=1.4, r=3, fl='finish', foe='slow 3 2',
  f='Добивание серии: весь строй идёт вперёд')
C('centurion', 'Центурион', 'Centurion', 'TANK', 0xA93226, 'VIT', 'STR', 'tank 3 party 2 melee 2',
  'gladius_jab pilum taunt shield_bash testudo legion_charge rallying_banner last_stand',
  ('Выучка легиона', 'armor 2 res_projectile 20'), 'Командир строя: черепаха из щитов, пилумы и натиск легиона.', races='human')

# 18. Огненный страж
K('ember_shield', 'Тлеющий щит', 'Ember Shield', 'BUFF_SELF', '◉', m=25, cd=25, me='fireres 15 0; res 10 0', fl='reflect', d=10,
  f='Щит раскалён докрасна')
K('blazing_taunt', 'Пылающий вызов', 'Blazing Taunt', 'TAUNT', '♨', m=25, cd=14, r=10, d=8, f='Враги бегут на свет огня')
K('cinder_slam', 'Пепельный удар', 'Cinder Slam', 'NOVA', '✸', m=30, cd=10, el='FIRE', st='VIT', pw=1.0, r=5, f='Взрыв углей вокруг стража')
K('inferno_wall', 'Огненный бастион', 'Inferno Bastion', 'ZONE', '♒', m=60, cd=45, el='FIRE', st='VIT', pw=0.5, r=6, d=10,
  me='fireres 10 0; res 10 1', f='Страж стоит в кольце огня')
C('flame_warden', 'Огненный страж', 'Flame Warden', 'TANK', 0xE67E22, 'VIT', 'INT', 'tank 3 fire 4',
  'cinder_slam blazing_taunt ember_shield firewall fire_field iron_skin inferno_wall meteor',
  ('Огнеупорный', 'health 6', 'fireproof flame_touch'), 'Не горит сам и поджигает всех, кто бьёт его вблизи.',
  races='demonkin dwarf')

# 19. Вечный
K('undying_will', 'Несгибаемая воля', 'Undying Will', 'BUFF_SELF', '∞', m=30, cd=30, me='regen 10 2; res 10 0', f='Смерть подождёт')
K('grave_strike', 'Могильный удар', 'Grave Strike', 'STRIKE', '✟', m=15, cd=5, st='VIT', pw=1.3, r=4.5, fl='steal', f='Удар, что лечит бьющего')
K('deathless', 'Бессмертие', 'Deathless', 'WARD', '☥', m=40, cd=90, r=1, d=15, f='Вечный защищает сам себя от смерти')
K('resurgence', 'Второе рождение', 'Resurgence', 'HEAL_SELF', '✺', m=50, cd=90, pw=0.7, me='str 10 1; res 10 1', f='Вечный встаёт снова')
C('undying', 'Вечный', 'Undying', 'TANK', 0xD4AC0D, 'VIT', 'VIT', 'tank 3 risk 3 undead 1',
  'grave_strike taunt undying_will iron_skin deathless second_wind resurgence last_stand',
  ('Не умирает', 'health 8 health_regen 2', 'phoenix'), 'Поднимается снова и снова: оберег от смерти и феникс в крови.',
  races='human orc')

# 20. Страж стены
K('wall_spike', 'Шипы стены', 'Wall Spikes', 'ZONE_AT', '▲', m=25, cd=14, st='VIT', pw=0.5, r=3.5, d=6, foe='slow 2 2', f='Из камня торчат колья')
K('sentry_horn', 'Сигнальный рог', 'Sentry Horn', 'REVEAL', '♬', m=20, cd=25, r=28, d=10, f='Дозорный видит лазутчиков')
K('crossbow_volley', 'Залп со стены', 'Wall Volley', 'ARROWS', '⫸', m=20, cd=7, st='AGI', pw=0.8, n=5, f='Арбалетчики на стене стреляют разом')
K('great_wall', 'Великая стена', 'Great Wall', 'WALL', '▙', m=60, cd=40, r=13, d=20, wall='minecraft:cobblestone', f='Стена на весь проход')
C('wallwarden', 'Страж стены', 'Wallwarden', 'TANK', 0x7F8C8D, 'VIT', 'AGI', 'tank 3 ranged 1 explore 1 craft 1',
  'crossbow_volley taunt wall_spike sentry_horn earth_wall bear_trap great_wall last_stand',
  ('Дозорный', 'armor 2 res_projectile 15', 'night_eye'), 'Защищает проходы: стены, колья и дозор.', races='human dwarf')

# 21. Боевой механик
K('rivet_turret', 'Клёпаная турель', 'Rivet Turret', 'SUMMON', '⌂', m=35, cd=30, sm='minecraft:snow_golem', n=1, d=45, f='Самострел на треноге')
K('iron_sentinel', 'Железный часовой', 'Iron Sentinel', 'SUMMON', '♜', m=60, cd=75, sm='minecraft:iron_golem', n=1, d=50,
  f='Механик собирает часового')
K('wrench_whack', 'Удар ключом', 'Wrench Whack', 'STRIKE', '⚙', m=10, cd=3, st='STR', pw=1.1, r=4.5, f='Гаечный ключ — тоже оружие')
K('overdrive', 'Форсаж', 'Overdrive', 'BUFF_ALLIES', '⚡', m=40, cd=40, r=10, me='haste 12 2; spd 12 1; str 12 0', f='Все механизмы на полную')
C('battle_mechanic', 'Боевой механик', 'Battle Mechanic', 'TANK', 0xB9770E, 'VIT', 'INT', 'tank 2 craft 4 mining 1',
  'wrench_whack taunt rivet_turret barricade overdrive iron_skin iron_sentinel fortress',
  ('Мастеровой', 'armor 2 mining 15'), 'Строит турели и железных часовых, чинит и держит удар.', races='dwarf', profs='blacksmith miner')

# 22. Драконий рыцарь
K('dragon_tail', 'Удар хвостом', 'Dragon Tail', 'STUN', '∂', m=20, cd=12, st='STR', pw=0.9, r=4.5, d=2, f='Чешуйчатый хвост сбивает с ног')
K('dragon_breath', 'Дыхание дракона', 'Breathe Fire', 'CONE', '♨', m=30, cd=9, el='FIRE', st='STR', pw=1.2, r=7, foe='weak 4 0',
  f='Рыцарь выдыхает пламя')
K('dragon_blood', 'Драконья кровь', 'Dragon Blood', 'BUFF_SELF', '♦', m=25, cd=30, me='regen 15 1; res 15 0', f='Кровь кипит и лечит')
K('elder_form', 'Облик дракона', 'Elder Dragon Form', 'BUFF_SELF', '♞', m=80, cd=90, me='str 20 2; res 20 1; hp 20 4; fireres 20 0; jump 20 1',
  f='Рыцарь становится драконом')
C('dragon_knight', 'Драконий рыцарь', 'Dragon Knight', 'TANK', 0xC0392B, 'VIT', 'STR', 'tank 3 fire 2 melee 2',
  'dragon_tail dragon_breath dragon_blood taunt rage_leap flame_breath war_cry elder_form',
  ('Чешуя', 'armor 3 res_fire 30'), 'Рыцарь с кровью дракона: хвост, огонь и истинный облик.', races='human demonkin')

# 23. Иглобрюх
K('quill_spray', 'Залп игл', 'Quill Spray', 'NOVA', '✳', m=15, cd=4, st='VIT', pw=0.7, r=5, f='Иглы во все стороны')
K('viscous_goo', 'Липкая слизь', 'Viscous Goo', 'STRIKE', '◌', m=10, cd=4, st='VIT', pw=0.3, r=12, foe='slow 4 2; weak 4 0', f='Плевок клейкой слизью')
K('bristle_back', 'Колючая спина', 'Bristleback', 'BUFF_SELF', '⋇', m=25, cd=25, me='res 10 2', fl='reflect', d=10,
  f='Иглы встают дыбом — бить в спину больно')
K('warpath', 'Тропа войны', 'Warpath', 'BUFF_SELF', '⇶', m=40, cd=40, me='spd 15 2; str 15 1', f='Чем больше игл, тем быстрее')
C('bristleback', 'Иглобрюх', 'Bristleback', 'TANK', 0x8D6E63, 'VIT', 'STR', 'tank 4 beast 1 solo 1',
  'quill_spray viscous_goo bristle_back taunt warpath static_field iron_skin last_stand',
  ('Иглы', 'armor 2', 'thorns'), 'Колючий зверолюд: иглы во все стороны и спина, по которой бить больно.', races='beastkin')

# 24. Кентавр-воевода
K('hoof_stomp', 'Удар копытами', 'Hoof Stomp', 'NOVA', '♘', m=30, cd=12, st='STR', pw=0.9, r=4.5, foe='slow 2 6', f='Копыта оглушают всех рядом')
K('double_edge', 'Обоюдоострый', 'Double Edge', 'STRIKE', '⚔', m=0, cd=6, st='STR', pw=2.2, r=4.5, hp=0.08, f='Удар ранит и самого кентавра')
K('retaliate', 'Ответный удар', 'Retaliate', 'BUFF_SELF', '⇌', m=25, cd=25, d=8, me='res 8 0', fl='reflect', f='Каждый удар возвращается')
K('great_stampede', 'Великий топот', 'Great Stampede', 'BUFF_ALLIES', '♞', m=70, cd=80, r=14, me='spd 6 3; res 6 0', f='Отряд несётся табуном')
C('centaur_warlord', 'Кентавр-воевода', 'Centaur Warlord', 'TANK', 0xA0522D, 'STR', 'VIT', 'tank 3 melee 2 explore 1 risk 1',
  'double_edge hoof_stomp retaliate charge stampede war_cry great_stampede last_stand',
  ('Мощь степи', 'health 8 move_speed 6'), 'Полуконь-полувоин: топот, обоюдоострые удары и табун в атаку.', races='beastkin')

# 25. Землетряс
K('fissure', 'Разлом', 'Fissure', 'WALL', '⋕', m=35, cd=16, r=9, d=8, wall='minecraft:blackstone', f='Земля трескается и встаёт гребнем')
K('enchant_totem', 'Заговорённый тотем', 'Enchant Totem', 'BUFF_SELF', '♈', m=20, cd=8, me='str 5 2', f='Следующий удар — сокрушительный')
K('aftershock', 'Отголосок', 'Aftershock', 'NOVA', '≋', m=15, cd=5, el='EARTH', st='STR', pw=0.6, r=4, foe='slow 1 4', f='Каждое заклинание сотрясает землю')
K('echo_slam', 'Эхо удара', 'Echo Slam', 'NOVA', '◎', m=90, cd=75, el='EARTH', st='STR', pw=1.2, r=8, fl='perfoe', f='Чем больше врагов, тем громче эхо')
C('earthshaker', 'Землетряс', 'Earthshaker', 'TANK', 0x935116, 'STR', 'INT', 'tank 2 mining 2 melee 1 party 1',
  'aftershock fissure enchant_totem taunt quake tectonic_slam ground_breaker echo_slam',
  ('Сын земли', 'armor 2 res_explosion 20'), 'Разломы, тотем и эхо удара, что сильнее с каждым врагом.', races='beastkin dwarf')

# 26. Таран
K('skewer', 'Насадить на рог', 'Skewer', 'DASH_STRIKE', '⇛', m=30, cd=12, st='STR', pw=1.1, r=3.5, foe='slow 2 2', f='Рог тащит врагов за собой')
K('empower_horn', 'Укрепление', 'Empower', 'BUFF_ALLIES', '♖', m=30, cd=25, r=10, me='str 12 1', f='Союзники бьют сильнее')
K('horn_toss', 'Подброс рогом', 'Horn Toss', 'LAUNCH', '⇈', m=25, cd=12, st='STR', pw=0.8, r=3.5, f='Таран подкидывает мелочь')
K('reverse_polarity', 'Обратная полярность', 'Reverse Polarity', 'VORTEX', '⊛', m=80, cd=70, st='STR', pw=0.6, r=8, d=2.5,
  foe='slow 3 6', f='Всё вокруг стягивается к тарану')
C('rampart_ram', 'Таран', 'Rampart Ram', 'TANK', 0x6C3483, 'STR', 'VIT', 'tank 3 melee 2 party 1',
  'skewer horn_toss empower_horn taunt shield_wall iron_skin reverse_polarity last_stand',
  ('Крепкий лоб', 'armor 2 health 6'), 'Рогатый зверолюд: тащит врагов на рогах и стягивает их в кучу.', races='beastkin')

# 27. Рыцарь хаоса
K('chaos_strike', 'Удар хаоса', 'Chaos Strike', 'STRIKE', '✪', m=12, cd=4, st='LUCK', pw=1.4, r=4.5, fl='steal', f='Удача решает, насколько больно')
K('chaos_stun', 'Снаряд хаоса', 'Chaos Bolt', 'STUN', '✦', m=25, cd=12, el='SHADOW', st='LUCK', pw=1.0, r=18, d=2.5, f='Непредсказуемый снаряд')
K('reality_rift', 'Разрыв реальности', 'Reality Rift', 'PULL', '⌇', m=20, cd=12, r=16, foe='weak 3 1', f='Рыцарь и враг сходятся в разрыве')
K('phantasm', 'Фантазм', 'Phantasm', 'SUMMON', '♞', m=70, cd=80, sm='minecraft:vex', n=3, d=25, f='Отражения рыцаря из других миров')
C('chaos_knight', 'Рыцарь хаоса', 'Chaos Knight', 'TANK', 0x641E16, 'STR', 'LUCK', 'tank 2 melee 2 risk 2',
  'chaos_strike chaos_stun reality_rift taunt anti_magic_shell death_grip phantasm last_stand',
  ('Капризы судьбы', 'crit_chance 6 crit_damage 20 health 4'), 'Всадник безумия: удача решает исход удара.',
  races='demonkin human')

# 28. Шаолиньский страж
K('deflect_palm', 'Отводящая ладонь', 'Deflecting Palm', 'BUFF_SELF', '☯', m=20, cd=14, d=3, me='res 3 2', fl='reflect', f='Мягко возвращает удар')
K('staff_spin', 'Вращение шеста', 'Staff Spin', 'MULTI_NOVA', '◎', m=25, cd=10, st='AGI', pw=0.4, r=3.5, n=4, f='Шест превращается в круг')
K('golden_bell', 'Золотой колокол', 'Golden Bell', 'BUFF_SELF', '♙', m=40, cd=45, me='res 8 2; abs 8 4', f='Тело звенит, как колокол')
K('mountain_stance', 'Стойка горы', 'Mountain Stance', 'TAUNT', '⛰', m=25, cd=15, r=10, d=8, me='res 8 1; slow 8 1', f='Монах замирает — враги идут к нему')
C('shaolin_guard', 'Шаолиньский страж', 'Shaolin Guard', 'TANK', 0xF39C12, 'AGI', 'VIT', 'tank 2 melee 2 support 1 solo 1',
  'palm_strike staff_spin deflect_palm mountain_stance pressure_point meditation golden_bell hundred_fists',
  ('Железная рубашка', 'dodge 10 armor 1'), 'Монах-защитник: уклонение, отражение и золотой колокол.', races='human beastkin')

# 29. Щит небес
K('barrier_field', 'Барьерное поле', 'Barrier Field', 'BUFF_ALLIES', '▦', m=40, cd=25, r=8, me='abs 6 4', f='Голубой щит перед отрядом')
K('fire_strike', 'Огненный удар', 'Fire Strike', 'BEAM', '➽', m=25, cd=8, el='FIRE', st='STR', pw=1.3, r=14, f='Молот бросает волну пламени')
K('pinning_charge', 'Таранный натиск', 'Pinning Charge', 'DASH_STRIKE', '⇒', m=35, cd=16, st='STR', pw=1.6, r=4, foe='slow 3 3', f='Рыцарь несётся, сметая всё')
K('earthshatter', 'Раскол земли', 'Earthshatter', 'LAUNCH', '⚡', m=70, cd=60, el='EARTH', st='STR', pw=1.3, r=8, foe='slow 3 4',
  f='Молот обрушивается и валит всех')
C('aegis_knight', 'Щит небес', 'Aegis Knight', 'TANK', 0xAAB7B8, 'STR', 'VIT', 'tank 3 party 3 melee 1',
  'fire_strike barrier_field pinning_charge taunt shield_wall guardian_ward earthshatter last_stand',
  ('Огромный щит', 'armor 3 res_projectile 25'), 'Старый рыцарь с молотом и барьером, за которым прячется весь отряд.',
  races='human dwarf')

# 30. Грозовой громила
K('tesla_arc', 'Тесла-дуга', 'Tesla Arc', 'CONE', 'ϟ', m=15, cd=3, el='LIGHTNING', st='STR', pw=0.8, r=6, f='Разряд бьёт всё перед собой')
K('jump_pack', 'Прыжковый ранец', 'Jump Pack', 'LEAP_SLAM', '⇑', m=25, cd=8, st='STR', pw=1.2, r=4, f='Громила взлетает и бьёт оземь')
K('bubble_dome', 'Купол', 'Bubble Dome', 'SHIELD', '◯', m=35, cd=15, pw=2.4, r=12, f='Купол над раненым')
K('primal_rage', 'Первобытная ярость', 'Primal Rage', 'BUFF_SELF', 'Ж', m=80, cd=90, me='hp 15 6; str 15 2; jump 15 2; res 15 1',
  f='Зверь внутри вырывается наружу')
C('storm_brute', 'Грозовой громила', 'Storm Brute', 'TANK', 0x5DADE2, 'STR', 'INT', 'tank 2 magic 1 melee 2 beast 1',
  'tesla_arc jump_pack bubble_dome taunt static_field iron_skin primal_rage last_stand',
  ('Шкура учёного', 'health 10 res_magic 10'), 'Учёный-зверолюд: тесла-разряды, прыжки и купол над друзьями.',
  races='beastkin')

# 31. Гравитон
K('graviton_surge', 'Гравитонный всплеск', 'Graviton Surge', 'VORTEX', '◍', m=70, cd=60, el='ARCANE', st='VIT', pw=0.4, r=7, d=4,
  f='Чёрная точка втягивает врагов')
K('particle_barrier', 'Барьер частиц', 'Particle Barrier', 'BUFF_SELF', '◈', m=25, cd=12, me='abs 4 4; str 6 1', f='Чем больше ударов, тем злее')
K('projected_barrier', 'Проекция барьера', 'Projected Barrier', 'SHIELD', '◇', m=25, cd=10, pw=1.8, r=18, f='Барьер летит к союзнику')
K('kinetic_grasp', 'Кинетическая хватка', 'Kinetic Grasp', 'BUFF_SELF', '✋', m=30, cd=14, d=3, me='abs 3 3', fl='reflect', f='Ладонь ловит снаряды')
C('graviton', 'Гравитон', 'Graviton', 'TANK', 0xEC407A, 'VIT', 'INT', 'tank 3 magic 2 party 1',
  'particle_barrier projected_barrier kinetic_grasp taunt arcane_beam magnetic_pull fortress graviton_surge',
  ('Масса', 'health 6 res_magic 15'), 'Играет с тяжестью: барьеры союзникам и гравитонная воронка.', races='human dwarf')

# 32. Кузнец войны
K('forge_hammer', 'Кузнечный молот', 'Forge Hammer', 'STRIKE', '⚒', m=12, cd=4, st='STR', pw=1.2, r=4.5, foe='weak 3 0', f='Удар как по наковальне')
K('temper_armor', 'Закалка брони', 'Temper Armor', 'BUFF_ALLIES', '▣', m=35, cd=30, r=10, me='res 12 1; abs 12 1', f='Кузнец подтягивает ремни и правит пластины')
K('molten_core', 'Расплавленное ядро', 'Molten Core', 'ZONE_AT', '♨', m=50, cd=35, el='FIRE', st='STR', pw=0.5, r=4, d=8, f='Лужа жидкого металла')
K('anvil_drop', 'Падение наковальни', 'Anvil Drop', 'METEOR', '⊥', m=40, cd=20, st='STR', pw=2.0, r=3, f='Наковальня с неба — классика')
C('warsmith', 'Кузнец войны', 'Warsmith', 'TANK', 0xD35400, 'STR', 'VIT', 'tank 2 craft 4 fire 1',
  'forge_hammer temper_armor anvil_drop taunt crossbow_turret iron_skin molten_core fortress',
  ('Горн', 'armor 2 res_fire 20 sell_price 10'), 'Кует прямо в бою: закаляет броню союзников и роняет наковальни.',
  races='dwarf', profs='blacksmith')

# 33. Живодёр
K('chain_hook', 'Цепной крюк', 'Chain Hook', 'PULL', '⛓', m=20, cd=10, r=20, foe='slow 2 2', f='Цепь, крюк и рывок')
K('scrap_gun', 'Обрез с хламом', 'Scrap Gun', 'CONE', '⁘', m=15, cd=4, st='STR', pw=1.1, r=6, f='Гвозди, гайки и битое стекло')
K('take_breather', 'Передышка', 'Take a Breather', 'HEAL_SELF', '☁', m=20, cd=14, pw=0.35, me='res 3 1', f='Глоток из баллона')
K('whole_hog', 'Свиной разгул', 'Whole Hog', 'PUSH', '⫸', m=60, cd=60, st='STR', pw=1.4, r=9, f='Шквал хлама сметает всех')
C('scrapper', 'Живодёр', 'Scrapper', 'TANK', 0x6E2C00, 'VIT', 'STR', 'tank 3 solo 2 risk 1',
  'scrap_gun chain_hook take_breather roar sunder iron_skin whole_hog last_stand',
  ('Шкура свина', 'health 14'), 'Огромный бродяга: крюк, обрез и передышка в самый разгар боя.', races='orc human')

# 34. Кристальный страж
K('amethyst_wall', 'Аметистовая стена', 'Amethyst Wall', 'WALL', '◆', m=30, cd=22, r=8, d=12, wall='minecraft:amethyst_block', f='Звенящая стена кристаллов')
K('crystal_shell', 'Кристальная оболочка', 'Crystal Shell', 'BUFF_SELF', '◈', m=30, cd=30, d=8, me='res 8 1; abs 8 2', fl='reflect', f='Грани отражают удары')
K('shard_burst', 'Взрыв осколков', 'Shard Burst', 'NOVA', '✶', m=25, cd=8, el='ARCANE', st='VIT', pw=1.0, r=5, f='Кристаллы разлетаются вокруг')
K('prism_ward', 'Призма', 'Prism', 'BUFF_ALLIES', '◇', m=60, cd=60, r=10, me='abs 12 3; res 12 0', f='Свет преломляется в щиты')
C('crystal_warden', 'Кристальный страж', 'Crystal Warden', 'TANK', 0xAF7AC5, 'VIT', 'INT', 'tank 3 mining 3 magic 1',
  'shard_burst taunt crystal_shell amethyst_wall quake guardian_ward prism_ward last_stand',
  ('Гранёный', 'armor 3 res_magic 10'), 'Покрыт аметистом: стены звенящих кристаллов и призмы-щиты.', races='dwarf elf', profs='miner')

# 35. Песчаный король
K('burrowstrike', 'Подкоп', 'Burrowstrike', 'DASH_STRIKE', '⇣', m=25, cd=12, el='EARTH', st='VIT', pw=1.2, r=3.5, foe='slow 2 6',
  f='Король ныряет под песок и выныривает под врагами')
K('sand_storm', 'Песчаная буря', 'Sand Storm', 'ZONE', '∴', m=40, cd=30, el='EARTH', st='VIT', pw=0.4, r=5, d=8, foe='blind 2 0',
  f='Песок скрывает короля и режет врагов')
K('caustic_finale', 'Едкий финал', 'Caustic Finale', 'ON_HIT', '☣', m=20, cd=20, n=8, oh='POISON', f='Яд скорпиона в каждом ударе')
K('epicenter', 'Эпицентр', 'Epicenter', 'MULTI_NOVA', '◎', m=80, cd=70, el='EARTH', st='VIT', pw=0.6, r=7, n=8, foe='slow 2 2',
  f='Волны землетрясения расходятся одна за другой')
C('sand_king', 'Песчаный король', 'Sand King', 'TANK', 0xD4AC0D, 'VIT', 'AGI', 'tank 2 stealth 1 explore 2 mining 1',
  'burrowstrike sand_storm caustic_finale taunt quake sand_toss iron_skin epicenter',
  ('Хитин', 'armor 3 res_arthropod 25'), 'Скорпион пустыни: подкопы, буря и эпицентр землетрясения.', races='beastkin')

# 36. Повелитель слизи
K('slime_spit', 'Плевок слизью', 'Slime Spit', 'BOLT', '●', m=12, cd=3, el='POISON', st='VIT', pw=0.9, foe='slow 3 1', f='Зелёный сгусток')
K('slime_call', 'Стая слизней', 'Slime Brood', 'SUMMON', '◍', m=35, cd=30, sm='minecraft:slime', n=3, d=30, f='Слизни отвлекают врагов')
K('gel_body', 'Студенистое тело', 'Gelatinous Body', 'BUFF_SELF', '◎', m=30, cd=30, me='res 12 1; regen 12 1; jump 12 1', f='Удары вязнут')
K('magma_brood', 'Магмовый выводок', 'Magma Brood', 'SUMMON', '◉', m=60, cd=60, sm='minecraft:magma_cube', n=3, d=30, f='Горячие родичи')
C('slime_lord', 'Повелитель слизи', 'Slime Lord', 'TANK', 0x58D68D, 'VIT', 'INT', 'tank 3 nature 1 beast 2',
  'slime_spit taunt gel_body slime_call viscous_goo plague_cloud magma_brood last_stand',
  ('Вязкость', 'health 8 res_fall 50', 'featherfall'), 'Студенистый страж: слизни отвлекают, удары вязнут.', races='beastkin demonkin')

# 37. Ветеран
K('old_wound', 'Старая рана', 'Old Wounds', 'HEAL_SELF', '✚', m=20, cd=25, pw=0.25, me='res 6 0', f='Ветеран знает, как терпеть')
K('veteran_feint', 'Приём ветерана', 'Veteran\'s Trick', 'STRIKE', '⚔', m=15, cd=5, pw=1.3, r=4.5, foe='weak 3 0', f='Удар, отработанный сотни раз')
K('drill_sergeant', 'Муштра', 'Drill', 'BUFF_ALLIES', '♫', m=30, cd=30, r=12, me='res 15 0; haste 15 0', f='Сержант гаркает — строй собирается')
K('hold_the_line', 'Ни шагу назад', 'Hold the Line', 'TAUNT', '⚑', m=50, cd=50, r=12, d=10, me='res 10 2; abs 10 3', f='Ветеран встаёт насмерть')
C('veteran', 'Ветеран', 'Veteran', 'TANK', 0x7E5109, 'VIT', 'STR', 'tank 2 melee 2 party 1 explore 1',
  'veteran_feint taunt old_wound shield_bash drill_sergeant second_wind war_cry hold_the_line',
  ('Опыт сотен битв', 'health 4 armor 1 xp 10'), 'Видел всё: старые раны, муштра и «ни шагу назад». Учится быстрее других.',
  races='human dwarf orc')

# 38. Скальный тролль
K('boulder_toss', 'Бросок валуна', 'Boulder Toss', 'BOLT', '◉', m=20, cd=6, el='EARTH', st='STR', pw=1.5, f='Тролль швыряет камень')
K('troll_regen', 'Тролличья живучесть', 'Troll Regeneration', 'BUFF_SELF', '♻', m=30, cd=30, me='regen 15 2', f='Раны затягиваются на глазах')
K('club_smash', 'Удар дубиной', 'Club Smash', 'STRIKE', '☗', m=15, cd=5, st='STR', pw=1.5, r=4.5, foe='slow 2 2', f='Просто, тяжело, больно')
K('stone_rage', 'Каменная ярость', 'Stone Rage', 'BUFF_SELF', '♜', m=60, cd=70, me='str 15 2; res 15 1; hp 15 3', f='Тролль каменеет от злости')
C('rock_troll', 'Скальный тролль', 'Rock Troll', 'TANK', 0x7D6608, 'VIT', 'STR', 'tank 3 melee 2 mining 1 solo 1',
  'club_smash boulder_toss troll_regen taunt war_stomp roar stone_rage last_stand',
  ('Живучесть', 'health 8 health_regen 3 res_fire -10'), 'Огромный и живучий: раны затягиваются сами, но огня он боится.',
  races='orc')

# 39. Вожак Орды
K('war_drums', 'Боевые барабаны', 'War Drums', 'BUFF_ALLIES', '♬', m=35, cd=30, r=14, me='str 12 0; haste 12 1', f='Барабаны гремят по всей округе')
K('horde_call', 'Зов Орды', 'Call the Horde', 'SUMMON', '⚔', m=60, cd=70, sm='minecraft:vindicator', n=2, d=40, f='Двое воинов Орды встают рядом')
K('brutal_cleave', 'Жестокий взмах', 'Brutal Cleave', 'CONE', '⫽', m=20, cd=6, st='STR', pw=1.2, r=5, f='Тесак сносит первый ряд')
K('bloodlust_roar', 'Рык кровожадности', 'Bloodlust Roar', 'TAUNT', 'Ж', m=40, cd=25, r=12, d=10, me='str 10 1; spd 10 0', foe='weak 6 0',
  f='Вожак злит врагов и себя')
C('horde_warlord', 'Вожак Орды', 'Horde Warlord', 'TANK', 0x6FA34A, 'STR', 'VIT', 'tank 2 party 3 melee 2',
  'brutal_cleave bloodlust_roar war_drums charge iron_skin rallying_banner horde_call last_stand',
  ('Кровь Орды', 'health 8 dmg_humanoid 10'), 'Ведёт Орду: барабаны, рык и воины, что приходят на зов.', races='orc', faction='orcs')

# 40. Призрачный страж
K('spectral_strike', 'Призрачный удар', 'Spectral Strike', 'STRIKE', '◌', m=15, cd=5, el='SHADOW', st='VIT', pw=1.2, r=4.5, f='Клинок проходит сквозь доспех')
K('phase_shift', 'Сдвиг фазы', 'Phase Shift', 'STEALTH', '◐', m=30, cd=20, d=3, r=0, me='res 3 4', f='Страж уходит в мир духов')
K('haunting', 'Наваждение', 'Haunting', 'TAUNT', '☠', m=30, cd=18, r=12, d=8, foe='slow 4 1; nausea 4 0', f='Призраки шепчут врагам')
K('ghost_legion', 'Призрачный легион', 'Ghost Legion', 'SUMMON', '♅', m=70, cd=80, sm='minecraft:vex', n=4, d=25, f='Павшие воины встают в строй')
C('spectral_guardian', 'Призрачный страж', 'Spectral Guardian', 'TANK', 0x85929E, 'VIT', 'INT', 'tank 3 undead 2 night 2',
  'spectral_strike haunting phase_shift death_grip anti_magic_shell soul_bind ghost_legion last_stand',
  ('Бесплотность', 'dodge 8 res_undead 15'), 'Полупризрак: уходит в мир духов и зовёт павших в строй.', races='elf demonkin')

# 41. Цепной страж
K('binding_chain', 'Цепь связывания', 'Binding Chain', 'STUN', '⛓', m=25, cd=14, st='VIT', pw=0.6, r=16, d=2.5, f='Цепь обвивает врага')
K('chain_whirl', 'Вихрь цепей', 'Chain Whirl', 'MULTI_NOVA', '∞', m=35, cd=14, st='VIT', pw=0.45, r=5, n=6, f='Цепи свистят вокруг стража')
K('anchor_chain', 'Якорная цепь', 'Anchor Chain', 'GRAPPLE', '⚓', m=15, cd=10, r=24, f='Страж подтягивается на цепи')
K('chain_prison', 'Цепная темница', 'Chain Prison', 'STASIS', '#', m=60, cd=50, r=4, d=3.5, f='Цепи опутывают всех врагов')
C('chain_warden', 'Цепной страж', 'Chain Warden', 'TANK', 0x616A6B, 'VIT', 'STR', 'tank 3 melee 2 undead 1',
  'chain_hook binding_chain chain_whirl anchor_chain taunt iron_skin chain_prison last_stand',
  ('Кованые звенья', 'armor 3 dmg_undead 10'), 'Тюремщик: цепи тянут, держат и запирают врагов.', races='human orc')

# 42. Чародейский оплот
K('arcane_barrier', 'Чародейский барьер', 'Arcane Barrier', 'BUFF_ALLIES', '◈', m=40, cd=30, r=10, me='abs 10 2; res 10 0', f='Сфера маны над отрядом')
K('spell_mirror', 'Зеркало чар', 'Spell Mirror', 'BUFF_SELF', '◫', m=25, cd=18, d=4, me='res 4 1', fl='reflect', f='Чары возвращаются к хозяину')
K('mana_burn_strike', 'Выжигание маны', 'Mana Burn', 'STRIKE', '✷', m=15, cd=5, el='ARCANE', st='INT', pw=1.2, r=10, foe='weak 4 1', f='Магия врага сгорает')
K('null_field', 'Поле пустоты', 'Null Field', 'ZONE', '◯', m=60, cd=50, el='ARCANE', st='VIT', pw=0.4, r=7, d=10, me='res 10 1', f='Магия гаснет вокруг')
C('arcane_bulwark', 'Чародейский оплот', 'Arcane Bulwark', 'TANK', 0x7FB3D5, 'INT', 'VIT', 'tank 3 magic 3',
  'mana_burn_strike taunt spell_mirror arcane_barrier arcane_shield blink null_field last_stand',
  ('Щит маны', 'mana 30 res_magic 20', 'mana_shield'), 'Маг-защитник: мана гасит удары, барьер укрывает отряд.', races='elf human')

# 43. Громовой страж
K('thunderclap', 'Раскат грома', 'Thunderclap', 'NOVA', '≋', m=25, cd=10, el='LIGHTNING', st='STR', pw=0.9, r=5, foe='slow 3 2', f='Гром глушит врагов')
K('storm_shield', 'Грозовой щит', 'Storm Shield', 'BUFF_SELF', '⚡', m=30, cd=25, me='res 10 1; abs 10 2', fl='reflect', d=10, f='Щит искрит и бьёт током')
K('lightning_rod', 'Громоотвод', 'Lightning Rod', 'TAUNT', '⚲', m=25, cd=14, r=10, d=8, foe='glow 6 0', f='Все молнии и враги — к нему')
K('storm_hammer', 'Молот бури', 'Storm Hammer', 'STUN', 'ϟ', m=30, cd=14, el='LIGHTNING', st='STR', pw=1.2, r=18, d=2, f='Брошенный молот бьёт как молния')
C('thunder_warden', 'Громовой страж', 'Thunder Warden', 'TANK', 0xF4D03F, 'STR', 'VIT', 'tank 3 magic 1 melee 2',
  'thunderclap lightning_rod storm_hammer storm_shield chain_lightning iron_skin thunderstorm last_stand',
  ('Заземление', 'armor 2 res_magic 10', 'chain_spark'), 'Притягивает молнии и врагов, глушит раскатами грома.', races='dwarf human')

# 44. Грибной страж
K('spore_cloud', 'Облако спор', 'Spore Cloud', 'ZONE', '⁂', m=30, cd=20, el='POISON', st='VIT', pw=0.35, r=5, d=8, foe='nausea 3 0',
  f='Грибница выдыхает яд')
K('mycelium_bind', 'Грибница', 'Mycelium', 'STUN', '⌘', m=25, cd=16, el='NATURE', st='VIT', pw=0.3, r=14, d=3, f='Нити опутывают ноги')
K('fungal_regrowth', 'Грибное восстановление', 'Fungal Regrowth', 'TETHER', '❀', m=30, cd=16, el='NATURE', pw=1.0, r=14, d=10, f='Мицелий сшивает раны')
K('sporeburst', 'Споровый взрыв', 'Sporeburst', 'NOVA', '✺', m=60, cd=40, el='POISON', st='VIT', pw=1.4, r=7, foe='poison 6 1; weak 6 1',
  f='Гриб лопается целиком')
C('myconid', 'Грибной страж', 'Myconid', 'TANK', 0x8E44AD, 'VIT', 'INT', 'tank 3 nature 3 night 1',
  'spore_cloud mycelium_bind fungal_regrowth taunt thick_hide rot sporeburst last_stand',
  ('Грибница', 'health 8 health_regen 1 res_arthropod 20'), 'Ходячий гриб из глубоких пещер: споры, грибница и ядовитые взрывы.',
  races='beastkin elf')

# 45. Носорог-таран
K('rhino_charge', 'Рывок носорога', 'Rhino Charge', 'DASH_STRIKE', '➠', m=25, cd=10, st='VIT', pw=1.3, r=4, foe='slow 2 4', f='Рог вперёд — с дороги!')
K('thick_plates', 'Роговые пластины', 'Horn Plates', 'BUFF_SELF', '▤', m=30, cd=30, me='res 12 2', f='Кожа толщиной в ладонь')
K('trample', 'Растоптать', 'Trample', 'NOVA', '♉', m=25, cd=9, st='VIT', pw=1.0, r=4, foe='slow 2 2', f='Носорог топчет всё вокруг')
K('unstoppable_herd', 'Неудержимое стадо', 'Unstoppable Herd', 'DASH_STRIKE', '⇶', m=60, cd=50, st='VIT', pw=2.2, r=5, foe='slow 4 4',
  f='Длинный разгон — и ничто не устоит')
C('rhino', 'Носорог', 'Rhino', 'TANK', 0x839192, 'VIT', 'STR', 'tank 3 beast 2 melee 1',
  'rhino_charge trample thick_plates taunt horn_toss roar unstoppable_herd last_stand',
  ('Шкура носорога', 'armor 4 move_speed 4'), 'Зверолюд-носорог: таранит, топчет и держит удар толстой шкурой.', races='beastkin')
