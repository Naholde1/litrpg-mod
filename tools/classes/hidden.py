"""Скрытые классы: Система предлагает их только тем, кто выполнил особое условие."""
from .dsl import K, C

# 1. Истребитель чудовищ
K('silver_sign', 'Знак Аард', 'Aard Sign', 'PUSH', '⋀', m=20, cd=8, el='ARCANE', st='STR', pw=0.7, r=7, foe='slow 2 3', f='Ведьмачий знак сбивает с ног')
K('quen_sign', 'Знак Квен', 'Quen Sign', 'BUFF_SELF', '◇', m=25, cd=16, me='abs 10 3', f='Щит, что держит один удар')
K('igni_sign', 'Знак Игни', 'Igni Sign', 'CONE', '△', m=20, cd=8, el='FIRE', st='STR', pw=1.2, r=6, f='Поток огня из ладони')
K('monster_oil', 'Масло против чудищ', 'Monster Oil', 'ON_HIT', '⚗', m=20, cd=25, n=12, oh='ARCANE', f='Клинок смазан против тварей')
K('whirl_witcher', 'Вихрь ведьмака', 'Whirl', 'MULTI_NOVA', '@', m=40, cd=16, st='STR', pw=0.6, r=4.5, n=6, f='Танец со стальным и серебряным')
K('trial_of_grasses', 'Испытание травами', 'Trial of the Grasses', 'BUFF_SELF', '☠', m=70, cd=90, me='str 20 2; regen 20 2; nightv 20 0; res 20 1',
  f='Эликсиры, которые убили бы человека')
C('monster_slayer', 'Истребитель чудовищ', 'Monster Slayer', 'DAMAGE', 0xBDC3C7, 'STR', 'AGI', 'melee 3 solo 2 night 1',
  'silver_stake silver_sign igni_sign quen_sign monster_oil whirl_witcher elixir_of_might trial_of_grasses',
  ('Мутант', 'dmg_elite 25 dmg_beast 15', 'night_eye'), 'Мутант-наёмник: знаки, масла и эликсиры против чудовищ.',
  hidden=['counter:elites>=40', 'counter:giant_kills>=15'], hint='убить 40 элитных монстров и 15 монстров выше себя уровнем')

# 2. Гоблинский вождь
K('goblin_bombs', 'Гоблинские бомбы', 'Goblin Bombs', 'ARROWS', '●', m=15, cd=5, el='FIRE', st='LUCK', pw=1.0, n=3, f='Иногда взрываются в руках')
K('goblin_rabble', 'Сброд', 'Goblin Rabble', 'SUMMON', '♟', m=40, cd=40, sm='minecraft:pillager', n=3, d=40, f='Шайка за пару монет')
K('shiny_bribe', 'Блестяшка', 'Something Shiny', 'CHARM', '¤', m=20, cd=20, r=8, d=8, f='Все засмотрелись на монету')
K('goblin_rally', 'Гоблинский гвалт', 'Goblin Racket', 'BUFF_ALLIES', '♫', m=30, cd=25, r=12, me='spd 12 1; haste 12 1; luck 12 1', f='Шум, гам и бодрость')
K('rocket_cart', 'Тележка с ракетой', 'Rocket Cart', 'DASH_STRIKE', '⛟', m=35, cd=20, el='FIRE', st='LUCK', pw=1.8, r=5, f='Что может пойти не так?')
K('mega_bomb', 'Мега-бомба', 'Mega Bomb', 'METEOR', '☢', m=90, cd=90, el='FIRE', st='LUCK', pw=3.6, r=8, f='Гоблины сами не знают, что внутри')
C('goblin_chief', 'Гоблинский вождь', 'Goblin Chief', 'SUPPORT', 0x6FA34A, 'LUCK', 'INT', 'trade 3 craft 2 party 2',
  'goblin_bombs shiny_bribe goblin_rally steel_trap rocket_cart goblin_rabble hired_blades mega_bomb',
  ('Гоблинская удача', 'coin_find 40 loot 20'), 'Свой среди гоблинов: сброд, блестяшки, бомбы и мега-бомба.',
  faction='goblins', hidden=['rep:goblins>=300'], hint='добиться репутации 300 у племён гоблинов')

# 3. Наследник Архидемона
K('hellfire', 'Адское пламя', 'Hellfire', 'BOLT', '♨', m=15, cd=2, el='FIRE', pw=1.6, f='Пламя Бездны')
K('demon_wings', 'Крылья демона', 'Demon Wings', 'BUFF_SELF', '⋎', m=25, cd=20, me='levit 1 6; slowfall 10 0; spd 10 1', f='Кожистые крылья раскрываются')
K('soul_brand', 'Клеймо души', 'Soul Brand', 'MARK', '⛧', m=15, cd=10, r=30, d=15, foe='wither 6 1', f='Душа помечена для Бездны')
K('infernal_gate', 'Врата Бездны', 'Infernal Gate', 'SUMMON', '♨', m=60, cd=60, sm='minecraft:blaze', n=2, d=40, f='Из врат выходят огненные духи')
K('brimstone_rain', 'Серный дождь', 'Brimstone Rain', 'ZONE_AT', '☄', m=50, cd=30, el='FIRE', pw=0.8, r=6, d=8, f='С неба падает горящая сера')
K('archdemon_form', 'Облик Архидемона', 'Archdemon Form', 'BUFF_SELF', '♆', m=100, cd=120, me='str 25 3; res 25 2; hp 25 6; fireres 25 0; regen 25 1',
  f='Наследие пробуждается')
C('archdemon_heir', 'Наследник Архидемона', 'Archdemon Heir', 'DAMAGE', 0xB03A2E, 'INT', 'STR', 'magic 2 fire 3 risk 1',
  'hellfire soul_brand demon_wings brimstone_rain chaos_bolt infernal_gate meteor archdemon_form',
  ('Кровь Бездны', 'spell_power 10 dmg_nether 20', 'fireproof'), 'Пробуждённая кровь Архидемона: адское пламя, крылья и врата Бездны.',
  races='demonkin', hidden=['race:demonkin', 'counter:archdemon>=1'], hint='быть демоном и сразить Архидемона')

# 4. Странник миров
K('waypoint', 'Путеводная звезда', 'Waypoint', 'RALLY', '✦', m=20, cd=20, r=300, f='Странник появляется рядом с другом, где бы тот ни был')
K('pathfinder', 'Следопыт миров', 'Pathfinder', 'BUFF_ALLIES', '⇉', m=25, cd=25, r=14, me='spd 20 1; jump 20 1; nightv 20 0', f='Дорога сама ложится под ноги')
K('portal_gather', 'Общий портал', 'Shared Portal', 'GATHER', '◎', m=50, cd=60, r=200, f='Портал собирает отряд')
K('worldwalk', 'Шаг между мирами', 'Worldstep', 'BLINK', '◐', m=20, cd=6, r=16, f='Шаг — и вы на другом краю')
K('astral_shelter', 'Астральное укрытие', 'Astral Shelter', 'STEALTH', '☁', m=40, cd=40, d=8, r=8, me='regen 8 1', foe='blind 3 0', f='Отряд уходит в межмирье')
C('worldwalker', 'Странник миров', 'Worldwalker', 'SUPPORT', 0x48C9B0, 'AGI', 'INT', 'explore 4 party 2',
  'worldwalk pathfinder waypoint astral_shelter recall_anchor portal_gather blessing time_warp',
  ('Видавший виды', 'move_speed 10 xp 10', 'featherfall'), 'Обошёл весь мир: телепорты к друзьям, порталы и шаги между мирами.',
  hidden=['found>=25'], hint='открыть 25 мест Мира Системы')
K('recall_anchor', 'Якорь возврата', 'Recall Anchor', 'RECALL', '⚓', m=20, cd=15, pw=0.3, f='Возврат на три секунды назад')

# 5. Хрономант
K('temporal_bolt', 'Временной снаряд', 'Temporal Bolt', 'BOLT', '⌛', m=12, cd=1.5, el='ARCANE', pw=1.1, foe='slow 2 1', f='Снаряд стареет врага')
K('age', 'Старение', 'Age', 'STRIKE', '⏳', m=25, cd=12, el='ARCANE', pw=0.8, r=20, foe='weak 8 2; slow 8 2; fatigue 8 2', f='Годы за секунды')
K('rewind_ally', 'Откат союзника', 'Rewind Ally', 'HEAL_TARGET', '⟲', m=30, cd=14, pw=2.0, r=20, f='Раны союзника отматываются назад')
K('haste_time', 'Ускоренное время', 'Accelerate', 'BUFF_ALLIES', '⏩', m=40, cd=40, r=12, me='haste 15 3; spd 15 2', f='Для друзей время бежит вдвое быстрее')
K('epoch_end', 'Конец эпохи', 'End of an Era', 'STASIS', '◉', m=100, cd=100, r=9, d=5, f='Время останавливается для всех врагов')
C('chronomancer', 'Хрономант', 'Chronomancer', 'SUPPORT', 0xD4AC0D, 'INT', 'LUCK', 'support 2 magic 3 explore 1',
  'temporal_bolt age rewind_ally rewind haste_field haste_time time_warp epoch_end',
  ('Вне времени', 'cooldown 12 mana_regen 10'), 'Властелин времени: старит врагов, отматывает раны и останавливает эпоху.',
  hidden=['counter:story>=10'], hint='пройти 10 глав сюжета «Путь Избранного»')

# 6. Жнец
K('scythe_sweep', 'Взмах косы', 'Scythe Sweep', 'CONE', '☾', m=15, cd=4, el='SHADOW', st='STR', pw=1.3, r=6, fl='steal', f='Коса собирает урожай')
K('soul_harvest', 'Жатва душ', 'Soul Harvest', 'NOVA', '☠', m=30, cd=12, el='SHADOW', st='STR', pw=1.0, r=6, fl='steal perfoe', f='Души летят к жнецу')
K('deaths_door', 'Порог смерти', 'Death\'s Door', 'WARD', '⚰', m=40, cd=90, r=1, d=10, f='Жнец знает смерть в лицо — она его не берёт')
K('reapers_mark', 'Метка жнеца', 'Reaper\'s Mark', 'MARK', '✝', m=15, cd=10, r=30, d=15, foe='wither 8 1', f='Твоё время пришло')
K('final_harvest', 'Последняя жатва', 'Final Harvest', 'MULTI_NOVA', '☾', m=90, cd=90, el='SHADOW', st='STR', pw=0.9, r=7, n=10, fl='steal', f='Жнец кружит, и падают все')
C('reaper', 'Жнец', 'Reaper', 'DAMAGE', 0x17202A, 'STR', 'VIT', 'melee 3 risk 3 undead 1',
  'scythe_sweep reapers_mark soul_harvest shadow_step deaths_door unstoppable death_grip final_harvest',
  ('Знакомый смерти', 'lifesteal 8 crit_damage 20', 'soul_eater'), 'Тот, кто умирал слишком часто: коса, жатва душ и порог смерти.',
  hidden=['counter:deaths>=10', 'style:risk>=300'], hint='погибнуть 10 раз и часто сражаться на грани смерти')

# 7. Золотой магнат
K('golden_shower', 'Золотой дождь', 'Golden Rain', 'BUFF_ALLIES', '¤', m=30, cd=30, r=12, me='luck 20 3; hero 20 0', f='Монеты сыплются на отряд')
K('hire_guards', 'Нанять охрану', 'Hire Guards', 'SUMMON', '♜', m=50, cd=60, sm='minecraft:iron_golem', n=2, d=40, f='Лучшая охрана за золото')
K('buyout', 'Выкуп', 'Buyout', 'DOMINATE', '♦', m=50, cd=45, r=16, d=60, f='У каждого есть цена')
K('market_crash', 'Обвал рынка', 'Market Crash', 'NOVA', '⇊', m=40, cd=25, el='ARCANE', st='LUCK', pw=1.4, r=7, foe='weak 6 2', f='Монеты бьют больнее мечей')
K('golden_aegis', 'Золотая эгида', 'Golden Aegis', 'BUFF_ALLIES', '♔', m=90, cd=100, r=16, me='abs 20 5; res 20 1; regen 20 1', f='Лучшая защита, которую можно купить')
C('golden_magnate', 'Золотой магнат', 'Golden Magnate', 'SUPPORT', 0xF1C40F, 'LUCK', 'INT', 'trade 4 party 2',
  'market_crash shiny_bribe golden_shower buyout hire_guards hired_blades supply_drop golden_aegis',
  ('Капитал', 'coin_find 60 sell_price 30 loot 15'), 'Богач, которому служат наёмники: подкуп, выкуп и золотая эгида.',
  faction='kingdom', hidden=['style:trade>=400', 'rep:kingdom>=300'], hint='много торговать и заслужить 300 репутации у Королевства')

# 8. Ликантроп
K('wolf_form', 'Облик волка', 'Wolf Form', 'BUFF_SELF', '♞', m=30, cd=40, me='spd 20 2; str 20 1; jump 20 1; nightv 20 0', f='Кости ломаются, вырастает шерсть')
K('feral_bite', 'Звериный укус', 'Feral Bite', 'STRIKE', '∀', m=12, cd=3, st='AGI', pw=1.3, r=4.5, fl='steal', combo=1, f='Клыки в горло')
K('howl', 'Вой', 'Howl', 'BUFF_ALLIES', '☾', m=30, cd=30, r=16, me='str 15 1; spd 15 0', f='Стая отзывается')
K('pack_call_lycan', 'Зов стаи', 'Call the Pack', 'SUMMON', '♞', m=50, cd=50, sm='minecraft:wolf', n=3, d=60, f='Волки приходят к своему')
K('rend', 'Разорвать', 'Rend', 'MULTI_STRIKE', '⋒', m=40, cd=14, st='AGI', pw=0.6, r=5, n=6, fl='finish steal', f='Добивание серии: клочья шерсти и крови')
K('blood_moon_rage', 'Ярость кровавой луны', 'Blood Moon Rage', 'BUFF_SELF', '◕', m=80, cd=90, me='str 20 3; haste 20 2; regen 20 2; res 20 1', f='Луна красная — зверь свободен')
C('lycanthrope', 'Ликантроп', 'Lycanthrope', 'DAMAGE', 0x6E2C00, 'AGI', 'STR', 'melee 2 night 3 beast 2',
  'feral_bite wolf_form howl pounce rend pack_call_lycan fury_swipes blood_moon_rage',
  ('Проклятая кровь', 'lifesteal 5 attack_speed 8', 'night_eye'), 'Оборотень: волчий облик, вой, стая и ярость кровавой луны.',
  races='beastkin', hidden=['race:beastkin', 'style:night>=300'], hint='быть зверолюдом и много сражаться ночью')

# 9. Святой
K('saints_touch', 'Касание святого', 'Saint\'s Touch', 'HEAL_TARGET', '✚', m=20, cd=3, pw=2.0, r=20, f='Простое касание исцеляет')
K('halo', 'Нимб', 'Halo', 'ZONE', '◯', m=40, cd=20, el='HOLY', pw=0.5, r=7, d=10, f='Свет вокруг святого лечит своих и жжёт нежить')
K('martyrdom', 'Мученичество', 'Martyrdom', 'SACRIFICE', '✝', m=0, cd=20, pw=0.15, me='regen 6 1', f='Святой отдаёт себя')
K('resurrection_ward', 'Воскрешение', 'Resurrection', 'WARD', '☩', m=50, cd=40, r=24, d=20, f='Душа союзника не покинет тело')
K('divine_intervention', 'Божье вмешательство', 'Divine Intervention', 'BUFF_ALLIES', '✺', m=100, cd=120, r=20, me='res 8 4; regen 8 3; abs 8 4',
  f='Небо вмешивается в бой')
C('saint', 'Святой', 'Saint', 'SUPPORT', 0xFFFDE7, 'INT', 'VIT', 'support 4 undead 3',
  'saints_touch halo martyrdom purify resurrection_ward greater_heal hand_of_god divine_intervention',
  ('Святость', 'mana_regen 25 dmg_undead 30 health_regen 2'), 'Победивший Лича становится светом: касания, нимб, воскрешение.',
  hidden=['counter:lich>=1', 'counter:undead>=100'], hint='сразить Лича и упокоить 100 мертвецов')

# 10. Аватар горы
K('mountain_heart', 'Сердце горы', 'Mountain Heart', 'BUFF_SELF', '⛰', m=40, cd=40, me='res 15 2; hp 15 4; slow 15 0', f='Гора бьётся в груди')
K('ore_burst', 'Рудный взрыв', 'Ore Burst', 'NOVA', '◆', m=25, cd=8, el='EARTH', st='VIT', pw=1.2, r=5, f='Самоцветы разлетаются осколками')
K('deep_tremor', 'Глубинный толчок', 'Deep Tremor', 'LAUNCH', '≋', m=30, cd=14, el='EARTH', st='VIT', pw=1.0, r=6, foe='slow 3 3', f='Толчок из самых недр')
K('obsidian_wall', 'Обсидиановая стена', 'Obsidian Wall', 'WALL', '▇', m=40, cd=30, r=9, d=15, wall='minecraft:obsidian', f='Стена, которую не пробить')
K('avatar_of_stone', 'Аватар камня', 'Avatar of Stone', 'BUFF_SELF', '♜', m=100, cd=120, me='res 20 3; str 20 2; hp 20 8; regen 20 1', f='Гора встаёт на защиту')
C('mountain_avatar', 'Аватар горы', 'Mountain Avatar', 'TANK', 0x6E6E6E, 'VIT', 'STR', 'tank 3 mining 4',
  'ore_burst taunt deep_tremor mountain_heart obsidian_wall golem_call rock_tomb avatar_of_stone',
  ('Плоть горы', 'armor 5 health 10 mining 30'), 'Горняк, которого признала гора: обсидиан, толчки и аватар камня.',
  races='dwarf', profs='miner', hidden=['counter:ores>=500', 'prof:miner>=25'], hint='добыть 500 руд и поднять Горное дело до 25')
