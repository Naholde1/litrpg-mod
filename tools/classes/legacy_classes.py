"""Классы версий до 2.15: те же ключи, теперь с ролью, чертой и восемью навыками."""
from .dsl import K, C

# ---------- новые навыки старых классов ----------
K('sunder', 'Раскол брони', 'Sunder', 'STRIKE', '⚒', m=20, cd=8, pw=1.2, r=4.5, foe='weak 6 1; slow 3 0',
  f='Тяжёлый удар крушит доспех')
K('rallying_banner', 'Знамя сбора', 'Rallying Banner', 'BUFF_ALLIES', '⚑', m=45, cd=45, r=10,
  me='str 15 0; res 15 0; regen 6 0', f='Воин втыкает знамя — отряд воспрял')
K('ground_breaker', 'Землелом', 'Ground Breaker', 'LAUNCH', '⇈', m=40, cd=16, pw=1.1, r=4.5, foe='slow 3 2',
  f='Удар сотрясает землю и подкидывает врагов')

K('bloodbath', 'Кровавая баня', 'Bloodbath', 'NOVA', '✺', m=40, cd=14, pw=0.9, r=4.5, fl='steal perfoe',
  f='Чем больше врагов, тем слаще кровь')
K('unstoppable', 'Неудержимость', 'Unstoppable', 'BUFF_SELF', '➠', m=0, cd=40, hp=0.15, me='res 8 1; spd 8 1; str 8 1',
  f='Боль лишь подгоняет')
K('rampage', 'Бойня', 'Rampage', 'DASH_STRIKE', '⇶', m=35, cd=12, pw=1.6, r=3.0, fl='steal', f='Сокрушительный рывок сквозь толпу')

K('bulwark_slam', 'Удар бастиона', 'Bulwark Slam', 'PUSH', '◙', m=25, cd=10, pw=0.8, r=6, foe='slow 3 2',
  f='Стена щита отбрасывает строй')
K('guardian_ward', 'Заслон', 'Ward of the Guardian', 'SHIELD', '⛉', m=35, cd=20, pw=2.0, r=10,
  f='Страж прикрывает самого раненого союзника')
K('fortress', 'Крепость', 'Fortress', 'WALL', '▥', m=40, cd=35, r=7, d=12, wall='minecraft:cobblestone',
  f='Каменный вал вырастает перед стражем')

K('hammer_of_justice', 'Молот правосудия', 'Hammer of Justice', 'STUN', '⚖', m=25, cd=14, el='HOLY', st='STR', pw=0.8, r=16, d=2.5,
  f='Брошенный молот света оглушает')
K('lay_on_hands', 'Возложение рук', 'Lay on Hands', 'HEAL_TARGET', '☩', m=50, cd=60, pw=4.0, r=12,
  f='Паладин отдаёт свет раненому')
K('avenging_wrath', 'Гнев небес', 'Avenging Wrath', 'BUFF_SELF', '✵', m=60, cd=75, me='str 15 1; res 15 0; regen 15 0',
  f='За спиной вспыхивают крылья света')

K('chains_of_ice', 'Ледяные цепи', 'Chains of Ice', 'STUN', '⛓', m=20, cd=12, el='FROST', st='STR', pw=0.6, r=20, d=2,
  foe='slow 5 3', f='Цепи холода сковывают жертву')
K('death_and_decay', 'Смерть и тлен', 'Death and Decay', 'ZONE_AT', '☠', m=45, cd=25, el='SHADOW', st='STR', pw=0.4, r=5, d=10,
  fl='steal', f='Земля гниёт и питает рыцаря')
K('anti_magic_shell', 'Антимагический панцирь', 'Anti-Magic Shell', 'BUFF_SELF', '◍', m=35, cd=40, me='abs 8 3; res 8 1',
  fl='reflect', d=5, f='Тёмный кокон глушит удары')

K('trident_toss', 'Бросок трезубца', 'Trident Toss', 'ARROWS', '⫛', m=20, cd=6, st='STR', pw=2.4, n=1, fl='pierce',
  f='Трезубец пробивает строй')
K('crowd_pleaser', 'Любимец толпы', 'Crowd Pleaser', 'BUFF_SELF', '♛', m=30, cd=30, me='regen 8 1; str 8 0; abs 8 1',
  f='Рёв трибун придаёт сил')
K('sand_toss', 'Песок в глаза', 'Sand Toss', 'CONE', '∴', m=15, cd=10, pw=0.3, r=5, foe='blind 4 0; slow 3 1',
  f='Грязный приём арены')

K('skull_splitter', 'Раскол черепа', 'Skull Splitter', 'STRIKE', '☗', m=30, cd=10, pw=1.7, r=4.5, fl='exec',
  f='Секира опускается со всей силы')
K('ancient_leap', 'Прыжок предков', 'Leap of the Ancients', 'LEAP_SLAM', '⇑', m=50, cd=22, pw=2.0, r=6, foe='slow 4 2',
  f='Варвар взмывает и обрушивается на врагов')
K('war_stomp', 'Боевой топот', 'War Stomp', 'LAUNCH', 'Ш', m=30, cd=14, pw=0.8, r=5, f='Топот подкидывает мелочь')

K('crossing_slash', 'Крестовый разрез', 'Crossing Slash', 'BEAM', '✕', m=30, cd=9, st='AGI', pw=1.6, r=8,
  f='Удар настолько быстр, что режет воздух')
K('mirror_blades', 'Зеркальные клинки', 'Mirror Blades', 'STEALTH', '◫', m=30, cd=25, d=4, r=0, me='spd 4 2; str 4 1',
  f='Мастер растворяется в бликах стали')
K('omnislash', 'Всесечение', 'Omnislash', 'MULTI_STRIKE', '✶', m=80, cd=60, st='AGI', pw=0.55, r=6, n=14,
  f='Вихрь ударов, от которого нельзя уйти')

K('flying_kick', 'Удар в полёте', 'Flying Kick', 'DASH_STRIKE', '➚', m=20, cd=7, st='AGI', pw=1.3, r=2.6, foe='slow 2 4',
  f='Прыжок с ногой вперёд')
K('iron_body', 'Железное тело', 'Iron Body', 'BUFF_SELF', '▣', m=30, cd=35, me='res 10 1; abs 10 1; spd 10 0',
  f='Ци закаляет плоть')
K('pressure_point', 'Точка давления', 'Pressure Point', 'STUN', '•', m=20, cd=12, st='AGI', pw=0.8, r=4.5, d=2.5,
  f='Точный тычок обездвиживает')

K('sinister_strike', 'Коварный удар', 'Sinister Strike', 'STRIKE', '⚔', m=12, cd=3, st='AGI', pw=1.0, r=4.5, combo=1,
  f='Быстрый удар готовит добивание')
K('cheap_shot', 'Подлый приём', 'Cheap Shot', 'STUN', '◒', m=20, cd=14, st='AGI', pw=0.4, r=4.5, d=2, combo=2,
  f='Удар под дых — и цель не дышит')
K('kidney_shot', 'Удар по почкам', 'Kidney Shot', 'STRIKE', '✖', m=25, cd=8, st='AGI', pw=1.2, r=4.5, fl='finish',
  foe='slow 3 3', f='Добивание серии')

K('garrote', 'Удавка', 'Garrote', 'STRIKE', '∽', m=15, cd=6, st='AGI', pw=0.9, r=4.5, combo=2, foe='poison 6 1; slow 3 1',
  fl='back', f='Тонкая струна на шее')
K('shadow_veil', 'Пелена тени', 'Shadow Veil', 'STEALTH', '◌', m=25, cd=20, d=5, r=6, foe='blind 3 0', me='spd 5 1',
  f='Миг — и ассасин исчез')
K('coup_de_grace', 'Последний удар', 'Coup de Grace', 'STRIKE', '☨', m=40, cd=14, st='AGI', pw=1.6, r=4.5, fl='finish exec',
  f='Добивание серии: чем дольше ассасин бил, тем страшнее финал')

K('disarm', 'Обезоруживание', 'Disarm', 'STRIKE', '⊘', m=15, cd=12, st='AGI', pw=0.6, r=4.5, foe='weak 6 2', combo=1,
  f='Выбить оружие из рук')
K('pinning_lunge', 'Пригвождающий выпад', 'Pinning Lunge', 'DASH_STRIKE', '➵', m=20, cd=8, st='AGI', pw=1.3, r=2.2,
  foe='slow 3 4', combo=1, f='Укол пригвождает к месту')
K('grand_finale', 'Финальный акт', 'Grand Finale', 'MULTI_STRIKE', '❂', m=35, cd=16, st='AGI', pw=0.5, r=5, n=5,
  fl='finish', f='Дуэлянт завершает серию эффектно')

K('shadow_swap', 'Обмен тенями', 'Shadow Swap', 'SWAP', '⇆', m=20, cd=12, r=20, foe='blind 2 0', f='Тени меняют вас местами')
K('kunai_rain', 'Ливень кунаев', 'Kunai Rain', 'ARROWS', '⁂', m=30, cd=10, st='AGI', pw=0.7, n=7, f='Веер стальных жал')
K('umbral_prison', 'Теневая темница', 'Umbral Prison', 'STASIS', '◘', m=60, cd=45, r=3.5, d=3, f='Тень сжимается вокруг врагов')

K('eagle_eye', 'Орлиный глаз', 'Eagle Eye', 'REVEAL', '◉', m=25, cd=30, r=32, d=12, f='Следопыт видит всё')
K('trackers_snare', 'Силок', 'Tracker\'s Snare', 'MINE', '⊓', m=20, cd=12, pw=0.6, r=2.5, d=60, foe='slow 4 5',
  f='Петля, спрятанная в траве')
K('wind_arrow', 'Стрела ветра', 'Wind Arrow', 'BEAM', '➳', m=30, cd=9, st='AGI', pw=1.5, r=24, f='Выстрел, что пронзает всё на пути')

K('grappling_hook', 'Крюк-кошка', 'Grappling Hook', 'GRAPPLE', '⚓', m=15, cd=10, r=28, f='Снайпер взлетает на позицию')
K('tripwire', 'Растяжка', 'Tripwire', 'MINE', '⌇', m=25, cd=15, pw=1.2, r=3, d=60, f='Тонкая нить и заряд пороха')
K('deadeye', 'Мёртвый глаз', 'Deadeye', 'BUFF_SELF', '◎', m=30, cd=40, me='str 10 1; luck 10 2', f='Время замедляется для стрелка')

K('call_bear', 'Зов медведя', 'Call of the Bear', 'SUMMON', 'ʘ', m=50, cd=60, sm='minecraft:polar_bear', n=1, d=60,
  f='Из чащи выходит матёрый медведь')
K('pack_fury', 'Ярость стаи', 'Pack Fury', 'BUFF_ALLIES', 'Ѫ', m=30, cd=30, r=14, me='str 10 1; spd 10 1; regen 10 0',
  f='Звери чуют кровь')
K('stampede', 'Топот стада', 'Stampede', 'CONE', '♉', m=35, cd=14, pw=1.2, r=7, foe='slow 3 2', f='Дух стада сметает врагов')

K('crossbow_turret', 'Станковый арбалет', 'Crossbow Turret', 'SUMMON', '⌂', m=40, cd=45, sm='minecraft:snow_golem', n=2, d=40,
  f='Ставит две самострельные установки')
K('grappling_bolt', 'Болт с тросом', 'Grappling Bolt', 'GRAPPLE', '↗', m=15, cd=12, r=24, f='Болт впивается в стену и тянет за собой')
K('scatter_mine', 'Шрапнельная мина', 'Scatter Mine', 'MINE', '✳', m=25, cd=16, pw=1.4, r=3.5, d=60, f='Мина набита гвоздями')

K('arcane_missiles', 'Чародейские стрелы', 'Arcane Missiles', 'MULTI_STRIKE', '✧', m=25, cd=6, el='ARCANE', pw=0.45, r=24, n=5,
  f='Пять самонаводящихся сгустков')
K('arcane_beam', 'Чародейский луч', 'Arcane Beam', 'BEAM', '⟿', m=35, cd=10, el='ARCANE', pw=1.6, r=20, f='Поток чистой маны')
K('time_warp', 'Искажение времени', 'Time Warp', 'RESET', '⌛', m=80, cd=150, f='Маг поворачивает время вспять')

K('flame_breath', 'Огненное дыхание', 'Flame Breath', 'CONE', '♨', m=30, cd=8, el='FIRE', pw=1.2, r=7, f='Поток пламени изо рта')
K('living_bomb', 'Живая бомба', 'Living Bomb', 'MINE', '✹', m=30, cd=14, el='FIRE', pw=1.6, r=4, d=45, f='Огненная руна на земле')
K('firewall', 'Стена огня', 'Firewall', 'WALL', '▤', m=40, cd=30, r=7, d=10, wall='minecraft:magma_block',
  f='Раскалённая стена жжёт всех, кто на неё ступит')

K('ice_wall', 'Ледяная стена', 'Ice Wall', 'WALL', '▦', m=35, cd=25, r=8, d=10, wall='minecraft:packed_ice', f='Стена из вечного льда')
K('deep_freeze', 'Глубокая заморозка', 'Deep Freeze', 'STUN', '❆', m=30, cd=16, el='FROST', pw=1.0, r=20, d=3,
  f='Цель вмерзает в глыбу льда')
K('glacial_spike', 'Ледяной шип', 'Glacial Spike', 'BEAM', '➶', m=40, cd=10, el='FROST', pw=1.8, r=18, foe='slow 4 2',
  f='Копьё льда пронзает ряд врагов')

K('ball_lightning', 'Шаровая молния', 'Ball Lightning', 'DASH_STRIKE', '◎', m=30, cd=6, el='LIGHTNING', pw=1.0, r=3.0,
  f='Маг становится молнией и проносится сквозь врагов')
K('static_field', 'Статическое поле', 'Static Field', 'NOVA', '⌁', m=30, cd=10, el='LIGHTNING', pw=0.9, r=6, fl='perfoe',
  f='Разряд бьёт всех вокруг')
K('wrath_of_thunder', 'Гнев громовержца', 'Thundergod\'s Wrath', 'GLOBAL', 'ϟ', m=100, cd=90, el='LIGHTNING', pw=1.6, r=40,
  f='Небо отвечает на зов')

K('bone_spear', 'Костяное копьё', 'Bone Spear', 'BEAM', '➴', m=30, cd=8, el='SHADOW', pw=1.5, r=20, f='Копьё из костей мертвецов')
K('corpse_explosion', 'Взрыв трупа', 'Corpse Explosion', 'NOVA', '✸', m=35, cd=12, el='SHADOW', pw=1.1, r=5, fl='perfoe',
  f='Плоть взрывается костяной шрапнелью')
K('lich_form', 'Облик лича', 'Lich Form', 'BUFF_SELF', '☠', m=60, cd=90, me='res 20 1; regen 20 1; abs 20 3; nightv 20 0',
  f='Некромант на время становится личом')

K('fear', 'Страх', 'Fear', 'PUSH', '⚇', m=25, cd=16, el='SHADOW', pw=0.3, r=7, foe='weak 5 1; blind 3 0; slow 3 1',
  f='Ужас гонит врагов прочь')
K('life_tap', 'Жизнеотвод', 'Life Tap', 'SACRIFICE', '♁', m=0, cd=15, pw=0.2, f='Плоть в обмен на ману')
K('dominate_soul', 'Подчинение души', 'Dominate Soul', 'DOMINATE', '⛧', m=60, cd=60, r=16, d=40, f='Чужая воля становится вашей')

K('rejuvenation', 'Омоложение', 'Rejuvenation', 'TETHER', '❧', m=30, cd=12, pw=1.0, r=16, d=10, el='NATURE',
  f='Сок жизни перетекает в союзника')
K('thorn_wall', 'Стена шипов', 'Thorn Wall', 'WALL', '▒', m=35, cd=30, r=8, d=12, wall='minecraft:oak_leaves',
  f='Колючая живая изгородь')
K('tranquility', 'Умиротворение', 'Tranquility', 'ZONE', '❀', m=90, cd=90, el='NATURE', pw=-0.4, r=10, d=8,
  f='Тишина леса исцеляет всех вокруг')

K('spirit_link', 'Связь духов', 'Spirit Link', 'TETHER', '∞', m=30, cd=14, pw=1.2, r=16, d=8, el='NATURE',
  f='Духи соединяют шамана с союзником')
K('hex', 'Порча', 'Hex', 'STUN', '❦', m=30, cd=18, el='NATURE', pw=0.2, r=16, d=3.5, f='Цель превращается в беспомощную тварь')
K('storm_totem', 'Тотем грозы', 'Storm Totem', 'ZONE_AT', '♈', m=50, cd=30, el='LIGHTNING', pw=0.8, r=6, d=10,
  f='Тотем притягивает молнии')

K('earth_wall', 'Земляной вал', 'Earth Wall', 'WALL', '▙', m=30, cd=25, r=8, d=12, wall='minecraft:mud_bricks', f='Земля поднимается стеной')
K('tectonic_slam', 'Тектонический удар', 'Tectonic Slam', 'LAUNCH', '⛰', m=40, cd=16, el='EARTH', st='VIT', pw=1.0, r=5,
  f='Плиты земли подбрасывают врагов')
K('magnetic_pull', 'Магнитная воронка', 'Magnetic Pull', 'VORTEX', '⊛', m=45, cd=30, el='EARTH', pw=0.3, r=7, d=4,
  f='Камни и враги стягиваются к центру')

K('arcane_lunge', 'Чародейский выпад', 'Arcane Lunge', 'DASH_STRIKE', '⇛', m=25, cd=7, el='ARCANE', st='STR', pw=1.3, r=2.4,
  f='Клинок и магия единым рывком')
K('spell_parry', 'Парирование чар', 'Spell Parry', 'BUFF_SELF', '⛨', m=25, cd=20, d=4, me='res 4 1; abs 4 2', fl='reflect',
  f='Клинок отбивает удар обратно')
K('runic_burst', 'Рунный взрыв', 'Runic Burst', 'NOVA', '᛭', m=40, cd=12, el='ARCANE', st='STR', pw=1.4, r=5,
  f='Руны на клинке взрываются')

K('guardian_spirit', 'Дух-хранитель', 'Guardian Spirit', 'WARD', '☥', m=50, cd=60, r=16, d=10, f='Ангел над плечом союзника')
K('word_of_shield', 'Слово щита', 'Word of Shield', 'SHIELD', '◈', m=30, cd=10, pw=1.6, r=16, f='Слово силы закрывает союзника')
K('greater_heal', 'Великое исцеление', 'Greater Heal', 'HEAL_TARGET', '✚', m=45, cd=12, pw=2.6, r=16, f='Мощная молитва над раненым')

K('charming_melody', 'Чарующий напев', 'Charming Melody', 'CHARM', '♪', m=35, cd=30, r=8, d=8, f='Враги заслушались и опустили оружие')
K('tempo', 'Темп', 'Tempo', 'BUFF_ALLIES', '♩', m=30, cd=25, r=12, me='haste 12 1; spd 12 1', f='Ритм ускоряет руки и ноги')
K('encore', 'На бис', 'Encore', 'RESET', '❦', m=70, cd=120, f='Публика требует продолжения')

K('reveal_heresy', 'Обличение ереси', 'Reveal Heresy', 'REVEAL', '⊙', m=25, cd=25, r=24, d=12, f='Свет вскрывает нечисть')
K('shackle', 'Оковы', 'Shackle', 'STUN', '⛓', m=25, cd=14, el='HOLY', pw=0.6, r=20, d=3, f='Цепи света держат грешника')
K('pyre', 'Костёр', 'Pyre', 'ZONE_AT', '♨', m=45, cd=22, el='FIRE', pw=0.5, r=4, d=8, f='Очищающее пламя')

K('philosophers_shield', 'Щит философа', 'Philosopher\'s Shield', 'SHIELD', '⚛', m=30, cd=15, pw=1.5, r=12,
  f='Склянка застывает защитной плёнкой')
K('mutagen', 'Мутаген', 'Mutagen', 'BUFF_ALLIES', '⚗', m=40, cd=40, r=10, me='str 15 0; haste 15 1; hp 15 1', f='Странное, но действенное зелье')
K('chemical_rage', 'Химическая ярость', 'Chemical Rage', 'BUFF_SELF', '☢', m=50, cd=60, me='str 15 1; regen 15 2; spd 15 1',
  f='Алхимик пьёт всё сразу')

# ---------- классы ----------
C('warrior', 'Воин', 'Warrior', 'TANK', 0xD9534F, 'STR', 'VIT', 'melee 3 tank 3 party 1',
  'charge war_cry sunder slam iron_skin ground_breaker rallying_banner berserk',
  ('Закалка в боях', 'health 6 armor 1.5'), 'Надёжный боец ближнего боя: врывается в толпу, держит удар и ведёт отряд.',
  races='human orc')
C('berserker', 'Берсерк', 'Berserker', 'DAMAGE', 0xC0392B, 'STR', 'STR', 'melee 4 risk 3 fire 1',
  'blood_rage whirlwind rage_leap execute bloodbath unstoppable rampage frenzy',
  ('Жажда крови', 'lifesteal 4', 'berserker'), 'Ярость вместо брони: вампиризм, вихри и казни. Чем ближе смерть, тем сильнее.',
  races='orc beastkin')
C('guardian', 'Страж', 'Guardian', 'TANK', 0x7F8C8D, 'VIT', 'STR', 'tank 4 party 2 melee 1',
  'shield_bash taunt bulwark_slam guardian_ward quake shield_wall fortress last_stand',
  ('Несокрушимость', 'armor 3 res_projectile 15', 'thorns'), 'Живая крепость: притягивает врагов, прикрывает союзников и строит валы.',
  races='dwarf human')
C('paladin', 'Паладин', 'Paladin', 'TANK', 0xF4D03F, 'STR', 'VIT', 'melee 2 tank 3 support 2 undead 2',
  'holy_strike aura_of_protection hammer_of_justice holy_light consecration lay_on_hands avenging_wrath divine_shield',
  ('Свет веры', 'dmg_undead 20 health_regen 1'), 'Святой воин: свет, лечение и щиты. Гроза нежити и опора отряда.',
  races='human', faction='kingdom')
C('death_knight', 'Рыцарь смерти', 'Death Knight', 'TANK', 0x5D6D7E, 'STR', 'VIT', 'melee 2 undead 3 tank 2 night 1',
  'frost_strike death_grip chains_of_ice aura_of_decay death_and_decay raise_dead anti_magic_shell army_of_darkness',
  ('Нечестивая стойкость', 'lifesteal 5 res_undead 25'), 'Павший воин, повелевающий холодом и мертвецами.',
  hidden=['counter:deaths>=3', 'counter:undead>=30'], hint='погибнуть 3 раза и упокоить 30 мертвецов')
C('gladiator', 'Гладиатор', 'Gladiator', 'DAMAGE', 0xCA6F1E, 'STR', 'AGI', 'melee 3 tank 1 solo 1 risk 1',
  'net_throw lunge sand_toss parry hamstring trident_toss crowd_pleaser arena_fury',
  ('Боец арены', 'dmg_humanoid 15 crit_damage 15'), 'Боец арены: сети, трезубец, грязные приёмы и рёв трибун.',
  races='human orc')
C('barbarian', 'Варвар', 'Barbarian', 'DAMAGE', 0xA04000, 'STR', 'VIT', 'melee 3 nature 2 solo 1',
  'throwing_axe roar skull_splitter war_stomp rage_leap bloodletting ancient_leap ancestral_spirit',
  ('Дикая сила', 'health 8 damage 1'), 'Дикий воин: метает топоры, рычит, топчет и пускает кровь.', races='orc beastkin')
C('blademaster', 'Мастер клинка', 'Blademaster', 'DAMAGE', 0xE5E8E8, 'AGI', 'STR', 'melee 2 stealth 2 solo 1',
  'iai stance crossing_slash wind_dance sever mirror_blades thousand_cuts omnislash',
  ('Путь меча', 'crit_chance 6 attack_speed 8'), 'Молниеносные удары, стойки и танец ветра. Финал — Всесечение.',
  races='human elf')
C('monk', 'Монах', 'Monk', 'DAMAGE', 0xF5B041, 'AGI', 'VIT', 'melee 2 support 1 stealth 1 solo 1',
  'palm_strike swift_step flying_kick pressure_point meditation chi_wave iron_body hundred_fists',
  ('Гармония ци', 'dodge 8 move_speed 6'), 'Кулаки, ци и медитация. Быстр, неутомим и бьёт по нервным точкам.',
  races='human beastkin')
C('rogue', 'Разбойник', 'Rogue', 'DAMAGE', 0x9B6BD6, 'AGI', 'STR', 'stealth 3 melee 1 trade 1',
  'sinister_strike eviscerate cheap_shot poison_blade kidney_shot shadow_step smoke_bomb blade_dance',
  ('Ловкие пальцы', 'crit_chance 5 coin_find 20'), 'Копит приёмы серии и добивает. Удары в спину, яд и дым.',
  races='human beastkin')
C('assassin', 'Ассасин', 'Assassin', 'DAMAGE', 0x4A235A, 'AGI', 'LUCK', 'stealth 4 solo 2 night 1',
  'garrote death_mark shadow_step poison_blade shadow_veil vanish coup_de_grace assassinate',
  ('Тихая смерть', 'crit_damage 25', 'executioner'), 'Метит жертву, исчезает и убивает одним ударом.', races='elf demonkin')
C('duelist', 'Дуэлянт', 'Duelist', 'DAMAGE', 0xE74C3C, 'AGI', 'LUCK', 'stealth 2 melee 2 solo 2',
  'lunge disarm pinning_lunge feint riposte flurry heart_thrust grand_finale',
  ('Фехтовальщик', 'dodge 6 crit_chance 4'), 'Финты, контрудары и серии уколов с эффектным финалом.', races='human elf')
C('shadowblade', 'Теневой клинок', 'Shadowblade', 'DAMAGE', 0x2C3E50, 'AGI', 'INT', 'stealth 3 magic 1 night 2',
  'shuriken shadow_step kunai_rain shadow_swap shadow_clone shadow_burst umbral_prison dance_of_shadows',
  ('Дитя сумрака', 'dodge 5', 'night_eye'), 'Сюрикены, обмен тенями, теневые клоны и темница из мрака.', races='elf demonkin')
C('ranger', 'Следопыт', 'Ranger', 'DAMAGE', 0x5BD16A, 'AGI', 'LUCK', 'ranged 3 nature 1 explore 2',
  'volley poison_arrow trackers_snare leap_back hunters_mark eagle_eye wind_arrow arrow_rain',
  ('Лесная тропа', 'arrow_damage 15 move_speed 4'), 'Стрелок и охотник: залпы, силки, метки и дождь стрел.', races='elf beastkin')
C('sniper', 'Снайпер', 'Sniper', 'DAMAGE', 0x839192, 'AGI', 'LUCK', 'ranged 4 solo 1',
  'aimed_shot piercing_arrow grappling_hook camouflage tripwire deadeye explosive_arrow headshot',
  ('Холодный расчёт', 'crit_damage 30'), 'Один выстрел — один труп. Крюк-кошка, растяжки и выстрел в голову.', races='elf dwarf')
C('beastmaster', 'Повелитель зверей', 'Beastmaster', 'DAMAGE', 0x8E6E3B, 'AGI', 'VIT', 'beast 3 nature 2 ranged 1',
  'call_wolf beast_fury stampede leap_back hunters_mark call_bear pack_fury pack',
  ('Вожак', 'dmg_beast 20 health 4'), 'Сражается вместе со стаей волков и медведем.', races='beastkin elf')
C('arbalest', 'Арбалетчик', 'Arbalest', 'DAMAGE', 0x6E2C00, 'AGI', 'VIT', 'ranged 3 tank 1 craft 1',
  'heavy_bolt buckshot bear_trap grappling_bolt scatter_mine leap_back crossbow_turret barrage',
  ('Тяжёлый болт', 'arrow_damage 10 armor 1'), 'Тяжёлые болты, картечь, мины и станковые арбалеты.', races='dwarf human',
  profs='blacksmith')
C('mage', 'Маг', 'Mage', 'DAMAGE', 0x5AA0F0, 'INT', 'INT', 'magic 4 explore 1',
  'mana_bolt fireball arcane_missiles frost_nova blink arcane_beam meteor time_warp',
  ('Учёность', 'mana 30 spell_power 8'), 'Классический маг: стрелы маны, огонь, лёд, метеор и игры со временем.',
  races='human elf')
C('pyromancer', 'Пиромант', 'Pyromancer', 'DAMAGE', 0xFF5722, 'INT', 'LUCK', 'magic 2 fire 4',
  'fire_arrow fireball flame_breath ring_of_fire living_bomb fire_field firewall meteor',
  ('Сердце пламени', 'spell_power 6', 'fireproof'), 'Всё горит: огненное дыхание, живые бомбы и стены огня.',
  races='demonkin human')
C('cryomancer', 'Криомант', 'Cryomancer', 'DAMAGE', 0x85C1E9, 'INT', 'VIT', 'magic 3 tank 1 water 1',
  'ice_shard frost_nova ice_armor ice_wall deep_freeze glacial_spike blizzard ice_prison',
  ('Вечная мерзлота', 'res_magic 10', 'frost_touch'), 'Лёд и холод: замедляет, вмораживает и строит стены.', races='elf dwarf')
C('stormcaller', 'Повелитель бурь', 'Stormcaller', 'DAMAGE', 0xF7DC6F, 'INT', 'AGI', 'magic 3 ranged 1 explore 1',
  'spark chain_lightning ball_lightning static_field lightning_strike blink thunderstorm wrath_of_thunder',
  ('Дитя грозы', 'move_speed 5', 'chain_spark'), 'Молнии, шаровые разряды и гнев громовержца.', races='human elf')
C('necromancer', 'Некромант', 'Necromancer', 'DAMAGE', 0x6C3483, 'INT', 'VIT', 'magic 2 undead 3 night 1',
  'shadow_bolt raise_skeleton bone_spear drain_life corpse_explosion plague_cloud lich_form army_of_the_dead',
  ('Хозяин костей', 'res_undead 15', 'soul_eater'), 'Поднимает мёртвых, мечет костяные копья и становится личом.',
  hidden=['counter:undead>=60'], hint='упокоить 60 мертвецов')
C('warlock', 'Чернокнижник', 'Warlock', 'DAMAGE', 0x7D3C98, 'INT', 'LUCK', 'magic 3 stealth 1 risk 1',
  'curse chaos_bolt fear life_tap summon_imp soul_siphon dominate_soul gates_of_darkness',
  ('Договор с тьмой', 'spell_power 6', 'mana_shield'), 'Проклятия, хаос, страх и подчинение чужой души.',
  hidden=['counter:realm_visits>=1'], hint='побывать в Изнанке')
C('druid', 'Друид', 'Druid', 'SUPPORT', 0x27AE60, 'INT', 'VIT', 'nature 3 support 2 magic 1',
  'thorn_bolt healing_bloom rejuvenation entangle bear_form thorn_wall wrath_of_nature tranquility',
  ('Дыхание леса', 'health_regen 1 mana_regen 10'), 'Сила природы: шипы, омоложение, облик медведя и умиротворение.',
  races='elf beastkin', faction='wilds')
C('shaman', 'Шаман', 'Shaman', 'SUPPORT', 0x1ABC9C, 'INT', 'VIT', 'nature 2 support 2 magic 1 party 1',
  'spirit_lightning healing_totem spirit_link fury_totem hex storm_totem quake spirit_wolf',
  ('Голос предков', 'mana_regen 15'), 'Тотемы, связь духов, порча и молнии предков.', races='orc beastkin')
C('geomancer', 'Геомант', 'Geomancer', 'TANK', 0x935116, 'INT', 'VIT', 'magic 2 tank 2 craft 1 mining 2',
  'stone_bolt stoneskin earth_wall tectonic_slam quake magnetic_pull earth_spikes rock_fall',
  ('Плоть горы', 'armor 3 res_explosion 20'), 'Камень и земля: валы, тектонические удары и каменная кожа.',
  races='dwarf', profs='miner')
C('spellblade', 'Боевой маг', 'Spellblade', 'DAMAGE', 0x3498DB, 'INT', 'STR', 'melee 2 magic 2',
  'enchanted_blade mana_strike arcane_lunge spell_parry blink runic_burst arcane_shield blade_storm',
  ('Клинок и руна', 'spell_power 6 damage 1'), 'Зачарованный клинок, рунные взрывы и магия в ближнем бою.',
  races='human elf demonkin')
C('cleric', 'Жрец', 'Cleric', 'SUPPORT', 0xF2D16B, 'INT', 'VIT', 'support 3 undead 1 party 2',
  'heal smite word_of_shield blessing greater_heal purify guardian_spirit sanctuary',
  ('Благодать', 'mana_cost 10 health_regen 1'), 'Целитель и бич нежити: щиты, великие исцеления и дух-хранитель.',
  races='human dwarf', faction='kingdom')
C('bard', 'Бард', 'Bard', 'SUPPORT', 0xEC7063, 'INT', 'LUCK', 'support 3 trade 2 party 2',
  'song_of_valor lullaby tempo song_of_healing charming_melody dissonance ballad_of_heroes encore',
  ('Обаяние', 'sell_price 15 xp 5'), 'Песни усиливают союзников, чаруют врагов, а публика просит «На бис!».',
  races='human elf')
C('inquisitor', 'Инквизитор', 'Inquisitor', 'DAMAGE', 0xFDFEFE, 'INT', 'STR', 'undead 3 magic 1 melee 1',
  'smite chains_of_light reveal_heresy shackle purify pyre holy_fire judgment',
  ('Клятва охотника', 'dmg_undead 15 dmg_nether 15'), 'Охотник на нечисть: обличение, оковы, костры и суд.',
  races='human', faction='kingdom')
C('alchemist', 'Алхимик', 'Alchemist', 'SUPPORT', 0x58D68D, 'INT', 'LUCK', 'craft 3 support 2 trade 1',
  'acid_flask healing_flask philosophers_shield fire_flask elixir_of_might mutagen chemical_rage grand_transmutation',
  ('Золотые руки', 'coin_find 25 sell_price 10'), 'Склянки с кислотой, огнём и лечением, мутагены и щиты.',
  profs='alchemist herbalist')
