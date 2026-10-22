"""Стрелки и метатели."""
from .dsl import K, C

# 1. Ледяная лучница
K('frost_arrows', 'Ледяные стрелы', 'Frost Arrows', 'ARROWS', '❅', m=10, cd=2, st='AGI', pw=1.1, n=1, foe='slow 3 2', f='Стрела с инеем')
K('gust', 'Порыв', 'Gust', 'PUSH', '≋', m=20, cd=12, st='AGI', pw=0.3, r=8, foe='slow 2 1', f='Ветер отталкивает врагов')
K('multishot', 'Мультивыстрел', 'Multishot', 'ARROWS', '⋙', m=30, cd=10, st='AGI', pw=0.9, n=7, foe='slow 2 1', f='Веер ледяных стрел')
K('marksmanship', 'Меткость', 'Marksmanship', 'BUFF_SELF', '◎', m=40, cd=50, me='str 20 1; luck 20 1', f='Каждая стрела в цель')
C('frost_archer', 'Ледяная лучница', 'Frost Archer', 'DAMAGE', 0x85C1E9, 'AGI', 'INT', 'ranged 4 water 1',
  'frost_arrows gust multishot leap_back hunters_mark marksmanship deep_freeze arrow_rain',
  ('Холодный глаз', 'arrow_damage 15', 'frost_touch'), 'Ледяные стрелы замедляют, порыв держит врагов на расстоянии.', races='elf')

# 2. Лунная лучница
K('sacred_arrow', 'Священная стрела', 'Sacred Arrow', 'STUN', '➵', m=25, cd=14, el='HOLY', st='AGI', pw=1.5, r=40, d=3, f='Чем дальше летит, тем дольше оглушает')
K('starstorm', 'Звёздная буря', 'Starstorm', 'NOVA', '✧', m=30, cd=10, el='ARCANE', st='AGI', pw=1.0, r=7, f='Звёзды падают вокруг')
K('leap_moon', 'Прыжок пантеры', 'Leap', 'DASH', '⇗', m=15, cd=8, r=2.4, me='spd 4 1', f='Ездовая кошка прыгает вперёд')
K('moonlight_shadow', 'Лунная тень', 'Moonlight Shadow', 'BUFF_ALLIES', '☾', m=70, cd=80, r=16, me='invis 10 0; spd 10 1', f='Отряд исчезает в лунном свете')
C('moon_archer', 'Лунная лучница', 'Moon Archer', 'DAMAGE', 0xD6EAF8, 'AGI', 'INT', 'ranged 3 night 2 party 1',
  'volley sacred_arrow starstorm leap_moon hunters_mark piercing_arrow arrow_rain moonlight_shadow',
  ('Благословение Луны', 'arrow_damage 10', 'night_eye'), 'Жрица Луны со стрелами: священная стрела оглушает издалека.', races='elf', faction='elves')

# 3. Повелительница ветра
K('shackleshot', 'Сковывающий выстрел', 'Shackleshot', 'STUN', '⛓', m=25, cd=14, st='AGI', pw=0.8, r=24, d=3, f='Стрела с цепью привязывает врага')
K('powershot', 'Мощный выстрел', 'Powershot', 'BEAM', '➳', m=30, cd=10, st='AGI', pw=2.0, r=32, f='Натянутая до предела тетива')
K('windrun', 'Бег ветра', 'Windrun', 'BUFF_SELF', '≫', m=20, cd=14, me='spd 6 3', d=6, f='Стрелы проходят мимо')
K('focus_fire', 'Сосредоточенный огонь', 'Focus Fire', 'BARRAGE', '⫸', m=60, cd=50, st='AGI', pw=0.8, n=18, f='Стрелы одна за другой')
C('wind_ranger', 'Повелительница ветра', 'Wind Ranger', 'DAMAGE', 0x82E0AA, 'AGI', 'INT', 'ranged 3 explore 2',
  'powershot shackleshot windrun volley leap_back eagle_eye wind_arrow focus_fire',
  ('Лёгкие ноги', 'move_speed 8 dodge 5'), 'Ветер — её союзник: мощный выстрел, оковы и шквал стрел.', races='elf human')

# 4. Лучник-дракон
K('storm_arrows', 'Штормовые стрелы', 'Storm Bow', 'BARRAGE', '⇶', m=25, cd=8, st='AGI', pw=0.7, n=5, f='Пять стрел за секунду')
K('sonic_arrow', 'Звуковая стрела', 'Sonic Arrow', 'REVEAL', '◉', m=15, cd=14, r=20, d=8, f='Стрела слышит врагов за стеной')
K('lunge_dash', 'Рывок лучника', 'Lunge', 'DASH', '»', m=10, cd=5, r=2.2, f='Рывок в сторону посреди схватки')
K('dragonstrike', 'Удар драконов', 'Dragonstrike', 'BEAM', '龍', m=80, cd=60, el='LIGHTNING', st='AGI', pw=3.0, r=40, f='Два духа-дракона пронзают всё')
C('dragon_archer', 'Лучник-дракон', 'Dragon Archer', 'DAMAGE', 0x48C9B0, 'AGI', 'LUCK', 'ranged 4 stealth 1',
  'aimed_shot storm_arrows sonic_arrow lunge_dash wall_climb piercing_arrow headshot dragonstrike',
  ('Наследник рода', 'crit_damage 20 arrow_damage 5'), 'Лучник древнего рода: звуковые стрелы и удар драконов.', races='human')

# 5. Вдова
K('venom_mine', 'Ядовитая мина', 'Venom Mine', 'MINE', '☣', m=20, cd=14, el='POISON', st='AGI', pw=1.0, r=4, d=60, foe='poison 5 1', f='Капкан с ядовитым газом')
K('widow_grapple', 'Крюк вдовы', 'Grappling Hook', 'GRAPPLE', '⚓', m=10, cd=8, r=28, f='На крышу — к лучшей позиции')
K('infra_sight', 'Инфракрасный взгляд', 'Infra-Sight', 'REVEAL', '◎', m=40, cd=50, r=48, d=15, f='Тепло тел видно сквозь стены')
K('widow_kiss', 'Поцелуй вдовы', 'Widow\'s Kiss', 'ARROWS', '♥', m=35, cd=10, st='AGI', pw=4.0, n=1, fl='pierce', f='Заряженный выстрел')
C('widow', 'Вдова', 'Widowmaker', 'DAMAGE', 0x8E44AD, 'AGI', 'LUCK', 'ranged 4 solo 2 stealth 1',
  'aimed_shot venom_mine widow_grapple camouflage infra_sight deadeye widow_kiss headshot',
  ('Хладнокровие', 'crit_damage 35'), 'Снайпер-одиночка: крюк на крышу, ядовитые мины и поцелуй вдовы.', races='elf demonkin')

# 6. Наёмник
K('helix_rockets', 'Спиральные ракеты', 'Helix Rockets', 'BOLT', '✹', m=20, cd=6, el='FIRE', st='AGI', pw=1.5, f='Три ракеты по спирали')
K('sprint', 'Спринт', 'Sprint', 'BUFF_SELF', '⇉', m=10, cd=8, me='spd 6 2', f='Бегом!')
K('biotic_field', 'Биотическое поле', 'Biotic Field', 'ZONE', '✚', m=30, cd=16, el='NATURE', pw=-0.35, r=5, d=5, f='Лечебная станция под ногами')
K('tactical_visor', 'Тактический визор', 'Tactical Visor', 'BARRAGE', '⌖', m=70, cd=60, st='AGI', pw=1.0, n=24, f='Каждая пуля находит цель')
C('mercenary', 'Наёмник', 'Mercenary', 'DAMAGE', 0x2E4053, 'AGI', 'VIT', 'ranged 3 solo 1 trade 1 party 1',
  'barrage helix_rockets sprint biotic_field leap_back hunters_mark explosive_arrow tactical_visor',
  ('Профессионал', 'arrow_damage 10 coin_find 15'), 'Старый солдат: ракеты, спринт, биополе и визор с автоприцелом.', races='human')

# 7. Ганфайтер
K('fan_hammer', 'Веер выстрелов', 'Fan the Hammer', 'BARRAGE', '⁙', m=20, cd=6, st='AGI', pw=0.6, n=6, f='Шесть пуль за миг')
K('combat_roll', 'Перекат', 'Combat Roll', 'DASH', '↻', m=8, cd=6, r=2.0, me='str 2 1', f='Перекат и перезарядка')
K('flashbang', 'Светошумовая', 'Flashbang', 'STUN', '✷', m=20, cd=12, st='AGI', pw=0.3, r=8, d=1.5, foe='blind 2 0', f='Бах — и враг ничего не видит')
K('high_noon', 'Полдень', 'High Noon', 'GLOBAL', '☀', m=90, cd=80, st='AGI', pw=2.4, r=24, f='Пора бы и пообедать')
C('gunslinger', 'Ганфайтер', 'Gunslinger', 'DAMAGE', 0xA04000, 'AGI', 'LUCK', 'ranged 3 solo 2 risk 1',
  'flintlock fan_hammer combat_roll flashbang deadeye lucky_shot smoke_bomb high_noon',
  ('Быстрая рука', 'crit_chance 8'), 'Стрелок с Дикого Запада: веер выстрелов и смертельный полдень.', races='human beastkin')

# 8. Ракетчица
K('rocket', 'Ракета', 'Rocket', 'BOLT', '✷', m=15, cd=3, el='FIRE', st='AGI', pw=1.4, f='Ракета с взрывом')
K('jump_jet', 'Реактивный прыжок', 'Jump Jet', 'BUFF_SELF', '⇑', m=15, cd=10, me='levit 1 8; slowfall 6 0', f='Вертикальный взлёт')
K('concussive', 'Оглушающий заряд', 'Concussive Blast', 'PUSH', '◉', m=20, cd=10, el='FIRE', st='AGI', pw=0.6, r=6, f='Взрывная волна отбрасывает')
K('rocket_barrage', 'Ракетный шквал', 'Rocket Barrage', 'ZONE_AT', '☄', m=80, cd=70, el='FIRE', st='AGI', pw=1.0, r=6, d=4, f='Ливень ракет с неба')
C('rocketeer', 'Ракетчица', 'Rocketeer', 'DAMAGE', 0x2874A6, 'AGI', 'INT', 'ranged 3 fire 2 explore 1',
  'rocket jump_jet concussive explosive_arrow combat_roll grappling_hook meteor rocket_barrage',
  ('Ранец', 'res_fall 50 res_explosion 30', 'featherfall'), 'Летает на ранце и сверху засыпает врагов ракетами.', races='human dwarf')

# 9. Подрывник
K('frag_launcher', 'Гранатомёт', 'Frag Launcher', 'BOLT', '●', m=12, cd=2.5, el='FIRE', st='AGI', pw=1.3, f='Отскакивающие гранаты')
K('concussion_mine', 'Фугас', 'Concussion Mine', 'MINE', '◍', m=20, cd=10, el='FIRE', st='AGI', pw=1.6, r=4, d=60, f='Бум под ногами')
K('steel_trap', 'Стальной капкан', 'Steel Trap', 'MINE', '⊓', m=15, cd=12, st='AGI', pw=0.6, r=2, d=60, foe='slow 4 6', f='Зубастый капкан')
K('rip_tire', 'Покрышка-бомба', 'RIP-Tire', 'METEOR', '◎', m=80, cd=70, el='FIRE', st='AGI', pw=3.2, r=6, f='Катится, прыгает, взрывается')
C('bomber', 'Подрывник', 'Bomber', 'DAMAGE', 0xD68910, 'AGI', 'LUCK', 'ranged 2 fire 2 craft 2 risk 1',
  'frag_launcher concussion_mine steel_trap scatter_mine living_bomb tripwire leap_back rip_tire',
  ('Псих с бомбами', 'res_explosion 40 crit_chance 4'), 'Безумный подрывник: гранаты, фугасы и покрышка-бомба.', races='human demonkin')

# 10. Инженер-турельщик
K('rivet_gun', 'Заклёпочник', 'Rivet Gun', 'ARROWS', '•', m=10, cd=2, st='AGI', pw=1.0, n=1, f='Раскалённая заклёпка')
K('deploy_turret', 'Турель', 'Deploy Turret', 'SUMMON', '⌂', m=30, cd=25, sm='minecraft:snow_golem', n=1, d=60, f='Турель стреляет сама')
K('overload', 'Перегрузка', 'Overload', 'BUFF_SELF', '⚡', m=30, cd=30, me='res 8 1; haste 8 2; spd 8 1; abs 8 2', f='Броня и мощь')
K('turret_nest', 'Гнездо турелей', 'Turret Nest', 'SUMMON', '⌂', m=70, cd=70, sm='minecraft:snow_golem', n=4, d=45, f='Четыре турели сразу')
C('turret_engineer', 'Инженер-турельщик', 'Turret Engineer', 'DAMAGE', 0xE59866, 'INT', 'AGI', 'craft 4 ranged 2',
  'rivet_gun deploy_turret overload barricade molten_core iron_sentinel anvil_drop turret_nest',
  ('Изобретатель', 'arrow_damage 10 mining 15'), 'Ставит турели и железных часовых, плавит врагов.', races='dwarf', profs='blacksmith')

# 11. Рельсотронщица
K('rail_shot', 'Выстрел рельсотрона', 'Railgun Shot', 'BEAM', '⟿', m=25, cd=8, el='LIGHTNING', st='AGI', pw=2.2, r=36, f='Сгусток энергии насквозь')
K('power_slide', 'Силовое скольжение', 'Power Slide', 'DASH', '⇢', m=10, cd=6, r=2.4, me='jump 2 2', f='Скольжение и прыжок')
K('disruptor_shot', 'Подрывной заряд', 'Disruptor Shot', 'VORTEX', '◎', m=30, cd=14, el='LIGHTNING', st='AGI', pw=0.4, r=4, d=4, f='Сфера тянет и жжёт')
K('overclock', 'Разгон', 'Overclock', 'BUFF_SELF', '⚡', m=60, cd=60, me='str 12 2; haste 12 2', f='Рельсотрон раскален добела')
C('railgunner', 'Рельсотронщица', 'Railgunner', 'DAMAGE', 0x3498DB, 'AGI', 'INT', 'ranged 3 magic 1 explore 1',
  'rail_shot power_slide disruptor_shot aimed_shot piercing_arrow overclock deadeye headshot',
  ('Киберглаз', 'crit_damage 20 crit_chance 3'), 'Рельсотрон бьёт насквозь всё, что стоит на линии.', races='human')

# 12. Эхо
K('sticky_bombs', 'Липкие бомбы', 'Sticky Bombs', 'ARROWS', '●', m=15, cd=6, el='FIRE', st='AGI', pw=0.8, n=4, f='Бомбы прилипают и взрываются')
K('flight', 'Полёт', 'Flight', 'BUFF_SELF', '✈', m=20, cd=12, me='levit 2 3; slowfall 8 0; spd 8 1', f='Эхо взмывает вверх')
K('focus_beam', 'Фокус-луч', 'Focusing Beam', 'BEAM', '⟿', m=25, cd=8, el='ARCANE', st='AGI', pw=1.4, r=14, fl='exec', f='Луч добивает раненых')
K('duplicate', 'Дублирование', 'Duplicate', 'STEAL', '♊', m=60, cd=60, el='ARCANE', pw=1.0, r=20, me='str 15 1; res 15 0', f='Эхо копирует силу врага')
C('echo', 'Эхо', 'Echo', 'DAMAGE', 0x76D7C4, 'INT', 'AGI', 'ranged 2 magic 2 explore 1',
  'sticky_bombs flight focus_beam blink arcane_missiles eagle_eye time_warp duplicate',
  ('Адаптивность', 'spell_power 5', 'featherfall'), 'Живая машина: летает, копирует врагов и добивает лучом.', races='human elf')

# 13. Воздухоплаватель
K('flak_cannon', 'Зенитка', 'Flak Cannon', 'NOVA', '✳', m=25, cd=10, el='FIRE', st='AGI', pw=0.9, r=7, f='Шрапнель во всех вокруг')
K('homing_missile', 'Самонаводящаяся ракета', 'Homing Missile', 'STUN', '➹', m=25, cd=14, el='FIRE', st='AGI', pw=1.2, r=30, d=2, f='Ракета догонит')
K('rocket_volley', 'Залп ракет', 'Rocket Volley', 'ARROWS', '⫸', m=20, cd=6, el='FIRE', st='AGI', pw=0.9, n=6, f='Ракеты по ближайшим')
K('call_down', 'Удар с воздуха', 'Call Down', 'METEOR', '⇓', m=80, cd=60, el='FIRE', st='AGI', pw=3.0, r=7, f='Два залпа сверху')
C('aeronaut', 'Воздухоплаватель', 'Aeronaut', 'DAMAGE', 0xBA4A00, 'AGI', 'INT', 'ranged 3 fire 1 explore 2 craft 1',
  'rocket_volley flak_cannon homing_missile jump_jet barrage grappling_hook arrow_rain call_down',
  ('Пилот', 'res_fall 60 arrow_damage 5', 'featherfall'), 'Пилот гирокоптера: зенитка, самонаводящиеся ракеты, удар с воздуха.', races='dwarf')

# 14. Метатель ножей
K('knife_toss', 'Бросок ножа', 'Knife Toss', 'ARROWS', '†', m=8, cd=1.5, st='AGI', pw=1.0, n=1, combo=1, f='Нож в цель')
K('fan_of_knives', 'Веер ножей', 'Fan of Knives', 'NOVA', '✴', m=25, cd=8, st='AGI', pw=0.9, r=6, combo=1, f='Ножи во все стороны')
K('ricochet', 'Рикошет', 'Ricochet', 'CHAIN', '⤨', m=20, cd=6, st='AGI', pw=1.0, r=7, n=5, f='Нож отскакивает от врага к врагу')
K('blade_storm_ranged', 'Буря лезвий', 'Storm of Blades', 'BARRAGE', '⁂', m=40, cd=20, st='AGI', pw=0.6, n=14, fl='finish', f='Добивание серии: ливень ножей')
C('knife_thrower', 'Метатель ножей', 'Knife Thrower', 'DAMAGE', 0x7F8C8D, 'AGI', 'LUCK', 'ranged 3 stealth 2 trade 1',
  'knife_toss fan_of_knives ricochet shuriken poison_blade leap_back smoke_bomb blade_storm_ranged',
  ('Ловкая рука', 'crit_chance 6 attack_speed 5'), 'Циркач с поясом ножей: рикошеты, веер и буря лезвий.', races='human beastkin')

# 15. Лесная разбойница
K('acorn_shot', 'Выстрел жёлудем', 'Acorn Shot', 'CHAIN', '●', m=12, cd=4, st='AGI', pw=0.9, r=6, n=3, f='Жёлудь отскакивает')
K('bushwhack', 'Засада в кустах', 'Bushwhack', 'STASIS', '♣', m=30, cd=20, r=3.5, d=2, f='Сеть из кустов держит врагов')
K('scurry', 'Шмыг', 'Scurry', 'BUFF_SELF', '»', m=10, cd=12, me='spd 5 2; invis 2 0', f='Юрк — и нет её')
K('sharpshooter', 'Меткий стрелок', 'Sharpshooter', 'ARROWS', '➵', m=60, cd=40, st='AGI', pw=5.0, n=1, fl='pierce', foe='slow 3 3', f='Долгий прицел — страшный выстрел')
C('forest_rogue', 'Лесная разбойница', 'Forest Rogue', 'DAMAGE', 0x7DCEA0, 'AGI', 'LUCK', 'ranged 2 nature 2 stealth 2',
  'acorn_shot bushwhack scurry camouflage poison_arrow trackers_snare hunters_mark sharpshooter',
  ('Лесная шустрость', 'dodge 8 loot 10'), 'Белка-стрелок из леса: жёлуди, засады в кустах и меткий выстрел.', races='beastkin elf')

# 16. Звероловка
K('throw_net', 'Ловчая сеть', 'Throw Net', 'STUN', '#', m=15, cd=10, st='AGI', pw=0.2, r=14, d=2.5, f='Сеть сверху')
K('bait', 'Приманка', 'Bait', 'CHARM', '♨', m=20, cd=25, r=8, d=5, f='Звери отвлекаются на кусок мяса')
K('spike_pit', 'Волчья яма', 'Spike Pit', 'MINE', '▼', m=25, cd=15, st='AGI', pw=1.6, r=3, d=60, foe='slow 4 4', f='Колья на дне')
K('trap_field', 'Поле капканов', 'Trap Field', 'ZONE_AT', '⊓', m=60, cd=50, st='AGI', pw=0.5, r=6, d=10, foe='slow 2 5', f='Капканы повсюду')
C('trapper', 'Звероловка', 'Trapper', 'DAMAGE', 0x9A7D0A, 'AGI', 'VIT', 'ranged 2 beast 2 nature 1 craft 1',
  'volley throw_net bait spike_pit bear_trap trackers_snare call_wolf trap_field',
  ('Капканщица', 'dmg_beast 20 loot 10'), 'Знает повадки зверей: сети, ямы, капканы и приманка.', races='beastkin human', profs='hunter')

# 17. Метатель дротиков
K('javelin', 'Дротик', 'Javelin', 'ARROWS', '↟', m=12, cd=3, st='STR', pw=1.6, n=1, f='Тяжёлый дротик')
K('javelin_volley', 'Залп дротиков', 'Javelin Volley', 'ARROWS', '⇈', m=25, cd=8, st='STR', pw=1.1, n=4, f='Четыре дротика веером')
K('pinning_javelin', 'Пригвождение', 'Pinning Javelin', 'STUN', '⫯', m=25, cd=14, st='STR', pw=1.2, r=24, d=2.5, f='Дротик пригвождает к земле')
K('thunder_javelin', 'Громовой дротик', 'Thunder Javelin', 'LIGHTNING', 'ϟ', m=60, cd=40, el='LIGHTNING', st='STR', pw=3.0, r=40, f='Молния бьёт туда, куда воткнулся дротик')
C('javelineer', 'Метатель дротиков', 'Javelineer', 'DAMAGE', 0xB9770E, 'STR', 'AGI', 'ranged 3 melee 1 explore 1',
  'javelin javelin_volley pinning_javelin pilum lunge leap_back trident_toss thunder_javelin',
  ('Сильная рука', 'arrow_damage 10 damage 1'), 'Метает тяжёлые дротики: пригвождает и вызывает молнии.', races='orc beastkin human')

# 18. Пращник
K('sling_stone', 'Камень из пращи', 'Sling Stone', 'BOLT', '●', m=8, cd=1.5, el='EARTH', st='AGI', pw=1.0, f='Свист и удар')
K('skull_stone', 'В голову!', 'Skull Shot', 'STUN', '◕', m=20, cd=12, el='EARTH', st='AGI', pw=0.9, r=24, d=2, f='Камень точно в лоб')
K('scatter_stones', 'Горсть камней', 'Scatter Shot', 'CONE', '∴', m=15, cd=6, el='EARTH', st='AGI', pw=0.8, r=8, f='Россыпь мелких камней')
K('giant_killer', 'Победитель великанов', 'Giant Killer', 'ARROWS', '◉', m=50, cd=35, st='AGI', pw=4.0, n=1, foe='slow 3 3; weak 5 2', f='Как Давид против Голиафа')
C('slinger', 'Пращник', 'Slinger', 'DAMAGE', 0xA9A9A9, 'AGI', 'LUCK', 'ranged 3 solo 1 explore 1 mining 1',
  'sling_stone scatter_stones skull_stone leap_back hunters_mark lucky_shot deadeye giant_killer',
  ('Простое оружие', 'dmg_elite 15', 'giant_slayer'), 'Праща и камни: простое оружие, что валит великанов.', races='human beastkin')

# 19. Охотник за головами
K('track', 'Выслеживание', 'Track', 'MARK', '⊕', m=15, cd=12, r=48, d=30, f='Цель отмечена — и награда за неё')
K('shuriken_toss', 'Бросок сюрикена', 'Shuriken Toss', 'CHAIN', '✴', m=20, cd=6, st='AGI', pw=1.0, r=8, n=4, f='Сюрикен скачет по меченым')
K('jinada', 'Джинада', 'Jinada', 'ON_HIT', '⚔', m=15, cd=15, n=4, oh='ARCANE', f='Удары крадут и бьют сильнее')
K('bounty', 'Награда за голову', 'Bounty', 'BUFF_SELF', '¤', m=30, cd=40, me='luck 30 3; spd 30 0', f='Чем больше трофеев, тем богаче')
C('bounty_hunter', 'Охотник за головами', 'Bounty Hunter', 'DAMAGE', 0xCA6F1E, 'AGI', 'LUCK', 'stealth 2 trade 2 ranged 1 solo 1',
  'shuriken_toss track jinada shadow_walk_bh camouflage bounty eviscerate assassinate',
  ('Награда', 'coin_find 40 loot 10'), 'Работает за деньги: выслеживает, метит и получает награду.', races='beastkin human')
K('shadow_walk_bh', 'Шаг в тени', 'Shadow Walk', 'STEALTH', '◌', m=20, cd=15, d=8, r=0, me='spd 8 1', f='Охотник крадётся к цели')

# 20. Охотник на нечисть
K('silver_bolt', 'Серебряный болт', 'Silver Bolt', 'ARROWS', '➽', m=12, cd=3, el='HOLY', st='AGI', pw=1.3, n=1, f='Серебро против тварей ночи')
K('garlic_bomb', 'Чесночная бомба', 'Garlic Bomb', 'ZONE_AT', '✿', m=25, cd=14, el='HOLY', st='AGI', pw=0.4, r=4, d=6, foe='weak 3 1', f='Нечисть кашляет')
K('stake_trap', 'Осиновые колья', 'Stake Trap', 'MINE', '▲', m=20, cd=12, el='HOLY', st='AGI', pw=1.8, r=3, d=60, f='Ловушка для вампиров')
K('dawn_volley', 'Залп рассвета', 'Dawn Volley', 'ARROW_RAIN', '☀', m=70, cd=50, st='AGI', pw=1.3, r=6, d=4, f='Стрелы с первым лучом солнца')
C('monster_hunter', 'Охотник на нечисть', 'Monster Hunter', 'DAMAGE', 0x99A3A4, 'AGI', 'VIT', 'ranged 2 undead 3 night 2',
  'silver_bolt garlic_bomb stake_trap heavy_bolt reveal_heresy grappling_bolt holy_water dawn_volley',
  ('Охотник ночи', 'dmg_undead 25', 'night_eye'), 'Арбалет, серебро и чеснок: охотится на вампиров и мертвецов.', races='human dwarf')

# 21. Сокольник
K('hawk_dive', 'Пике сокола', 'Hawk Dive', 'STRIKE', '⇘', m=15, cd=5, st='AGI', pw=1.4, r=30, foe='blind 2 0', f='Сокол падает на цель')
K('hawk_eye', 'Соколиный глаз', 'Hawk Eye', 'REVEAL', '◎', m=20, cd=25, r=40, d=12, f='Сокол видит всё сверху')
K('talon_rake', 'Когти', 'Talon Rake', 'MULTI_STRIKE', '⋒', m=20, cd=8, st='AGI', pw=0.5, r=24, n=4, foe='weak 3 0', f='Сокол рвёт когтями')
K('sky_hunt', 'Небесная охота', 'Sky Hunt', 'GLOBAL', '♅', m=70, cd=60, st='AGI', pw=1.8, r=30, f='Стая соколов бьёт всех врагов')
C('falconer', 'Сокольник', 'Falconer', 'DAMAGE', 0xAF601A, 'AGI', 'INT', 'ranged 2 beast 3 explore 1',
  'hawk_dive talon_rake hawk_eye volley leap_back hunters_mark wind_arrow sky_hunt',
  ('Верный сокол', 'dmg_beast 10 crit_chance 4'), 'Сражается вместе с соколом: пике, когти и небесная охота.', races='beastkin human elf', profs='hunter')

# 22. Аркебузир
K('arquebus', 'Аркебуза', 'Arquebus', 'ARROWS', '◍', m=20, cd=5, st='AGI', pw=3.0, n=1, fl='pierce', f='Грохот и дым')
K('reload_drill', 'Перезарядка', 'Quick Reload', 'RESET', '⟳', m=30, cd=40, f='Отработанная до автоматизма')
K('powder_keg', 'Пороховая бочка', 'Powder Keg', 'MINE', '⛁', m=30, cd=20, el='FIRE', st='AGI', pw=2.2, r=5, d=60, f='Бочка пороха у ног врага')
K('volley_line', 'Залп шеренгой', 'Line Volley', 'BEAM', '⫸', m=60, cd=40, el='FIRE', st='AGI', pw=2.6, r=30, f='Вся шеренга стреляет разом')
C('arquebusier', 'Аркебузир', 'Arquebusier', 'DAMAGE', 0x6E2C00, 'AGI', 'VIT', 'ranged 3 craft 1 fire 1 party 1',
  'arquebus buckshot powder_keg reload_drill grappling_bolt scatter_mine deadeye volley_line',
  ('Порох', 'arrow_damage 15 res_explosion 20'), 'Стрелок с аркебузой: грохот, дым и пороховые бочки.', races='dwarf human', profs='blacksmith')
