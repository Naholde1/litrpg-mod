"""Бойцы ближнего боя."""
from .dsl import K, C

# 1. Самурай
K('iaijutsu', 'Иайдзюцу', 'Iaijutsu', 'STRIKE', '—', m=20, cd=6, st='AGI', pw=1.8, r=4.5, f='Клинок выходит из ножен и сразу возвращается')
K('bushido', 'Бусидо', 'Bushido', 'BUFF_SELF', '武', m=25, cd=30, me='str 15 1; res 15 0', f='Путь воина: без страха и сомнений')
K('counter_cut', 'Встречный разрез', 'Counter Cut', 'BUFF_SELF', '⇌', m=20, cd=14, d=3, me='res 3 2', fl='reflect', f='Самурай ждёт удара и отвечает')
K('final_draw', 'Последнее обнажение', 'Final Draw', 'BEAM', '一', m=70, cd=50, st='AGI', pw=3.2, r=12, f='Один удар, рассекающий всё')
C('samurai', 'Самурай', 'Samurai', 'DAMAGE', 0xB03A2E, 'AGI', 'STR', 'melee 3 solo 2',
  'iaijutsu bushido counter_cut iai stance sever crossing_slash final_draw',
  ('Честь', 'crit_damage 20 attack_speed 6'), 'Воин чести: один точный удар из ножен стоит десятка.', races='human')

# 2. Ниндзя
K('substitution', 'Замена', 'Substitution', 'SWAP', '⇄', m=15, cd=12, r=16, me='invis 2 0', f='На месте ниндзя — бревно')
K('fuma_shuriken', 'Фума-сюрикен', 'Fuma Shuriken', 'CHAIN', '✴', m=20, cd=6, st='AGI', pw=1.1, r=6, n=3, f='Огромная звезда скачет по врагам')
K('ninja_smoke', 'Дым ниндзя', 'Ninja Smoke', 'STEALTH', '☁', m=25, cd=20, d=5, r=6, foe='blind 3 0; slow 3 1', me='spd 5 2', f='Хлопок — и никого')
K('thousand_kunai', 'Тысяча кунаев', 'Thousand Kunai', 'BARRAGE', '⁂', m=60, cd=45, st='AGI', pw=0.7, n=20, f='Стальной дождь')
C('ninja', 'Ниндзя', 'Ninja', 'DAMAGE', 0x1C2833, 'AGI', 'INT', 'stealth 4 night 2',
  'shuriken fuma_shuriken substitution ninja_smoke shadow_step assassinate vanish thousand_kunai',
  ('Тень', 'dodge 8 move_speed 5'), 'Неуловимая тень: замена, дым и тысяча кунаев.', races='human elf')

# 3. Призрачная убийца
K('stifling_dagger', 'Удушающий кинжал', 'Stifling Dagger', 'ARROWS', '†', m=15, cd=5, st='AGI', pw=1.4, n=1, foe='slow 2 3', f='Брошенный кинжал замедляет')
K('phantom_strike', 'Призрачный удар', 'Phantom Strike', 'SHADOW_STEP', '◐', m=20, cd=8, r=16, me='haste 3 3', f='Миг — и она уже за спиной')
K('blur', 'Размытие', 'Blur', 'BUFF_SELF', '≈', m=25, cd=30, me='spd 15 0; invis 4 0', f='Её почти не видно')
K('coup', 'Удар милосердия', 'Coup de Grace', 'STRIKE', '✟', m=40, cd=12, st='AGI', pw=2.6, r=4.5, fl='exec', f='Редкий, но смертельный')
C('phantom_assassin', 'Призрачная убийца', 'Phantom Assassin', 'DAMAGE', 0x5B2C6F, 'AGI', 'LUCK', 'stealth 3 melee 2 solo 1',
  'stifling_dagger phantom_strike blur eviscerate poison_blade shadow_veil death_mark coup',
  ('Призрачность', 'dodge 12 crit_chance 6'), 'Почти невидима: уклоняется и наносит смертельные удары.', races='elf demonkin')

# 4. Неистовый клинок
K('blade_fury', 'Ярость клинка', 'Blade Fury', 'MULTI_NOVA', '@', m=40, cd=16, st='AGI', pw=0.5, r=4, n=8, me='res 2 2', f='Клинок кружит, а воин неуязвим к чарам')
K('healing_ward_blade', 'Целебный идол', 'Healing Ward', 'ZONE', '♥', m=35, cd=30, el='NATURE', st='AGI', pw=-0.4, r=6, d=10, f='Маленький идол лечит рядом')
K('blade_dance_crit', 'Танец лезвия', 'Blade Dance', 'BUFF_SELF', '✧', m=20, cd=25, me='luck 15 2; str 15 0', f='Каждый удар может стать критом')
K('swiftslash', 'Стремительный разрез', 'Swiftslash', 'MULTI_STRIKE', '✶', m=70, cd=50, st='AGI', pw=0.6, r=6, n=10, f='Вихрь клинка вокруг жертвы')
C('bladefury', 'Неистовый клинок', 'Bladefury', 'DAMAGE', 0xE59866, 'AGI', 'STR', 'melee 3 solo 1 risk 1',
  'blade_fury blade_dance_crit healing_ward_blade iai stance wind_dance sever swiftslash',
  ('Маска', 'crit_chance 8 res_magic 10'), 'Воин в маске: вихрь клинка, целебный идол и стремительный разрез.', races='orc beastkin')

# 5. Глубинный налётчик
K('essence_shift', 'Перетекание сущности', 'Essence Shift', 'STEAL', '⇋', m=15, cd=6, st='AGI', pw=1.0, r=5, me='str 15 0', f='Каждый удар крадёт силу')
K('dark_pact', 'Тёмный договор', 'Dark Pact', 'NOVA', '◉', m=0, cd=8, el='SHADOW', st='AGI', pw=1.3, r=4, hp=0.05, f='Вспышка тьмы очищает и ранит всех рядом')
K('pounce', 'Прыжок', 'Pounce', 'DASH_STRIKE', '➚', m=20, cd=10, st='AGI', pw=1.0, r=3.5, foe='slow 3 6', f='Налётчик прыгает и держит добычу')
K('shadow_dance_slark', 'Пляска теней', 'Shadow Dance', 'STEALTH', '◑', m=50, cd=60, d=4, r=0, me='regen 4 3; spd 4 2', f='Тень скрывает и лечит')
C('deep_raider', 'Глубинный налётчик', 'Deep Raider', 'DAMAGE', 0x1F3A93, 'AGI', 'VIT', 'stealth 2 water 3 solo 1',
  'essence_shift pounce dark_pact eviscerate shadow_step smoke_bomb blade_dance shadow_dance_slark',
  ('Чешуя глубин', 'lifesteal 4', 'water_breathing'), 'Ловец из морской бездны: крадёт силу и исчезает в тенях.', races='beastkin')

# 6. Ткач пустоты
K('time_walk', 'Шаг во времени', 'Time Walk', 'RECALL', '⟲', m=20, cd=12, pw=0.3, f='Уходит назад во времени и лечит раны')
K('time_lock_blade', 'Замок времени', 'Time Lock', 'ON_HIT', '⌛', m=25, cd=25, n=8, oh='STUN', f='Удары останавливают время у врага')
K('time_dilation', 'Растяжение времени', 'Time Dilation', 'NOVA', '◌', m=30, cd=16, el='ARCANE', st='AGI', pw=0.5, r=6, foe='slow 6 3; fatigue 6 2', f='Время для врагов тянется')
K('chronosphere', 'Хроносфера', 'Chronosphere', 'STASIS', '◯', m=90, cd=90, r=6, d=4, f='Всё внутри сферы застывает')
C('void_walker', 'Ткач пустоты', 'Void Walker', 'DAMAGE', 0x4A235A, 'AGI', 'STR', 'melee 2 magic 1 solo 2',
  'time_walk time_lock_blade time_dilation flurry sever blade_dance thousand_cuts chronosphere',
  ('Вне времени', 'dodge 6 attack_speed 8'), 'Шагает сквозь время: возвращается назад и запирает врагов в хроносфере.', races='demonkin')

# 7. Призрачная мстительница
K('spectral_dagger', 'Призрачный кинжал', 'Spectral Dagger', 'BEAM', '†', m=20, cd=10, el='SHADOW', st='AGI', pw=1.2, r=16, foe='slow 3 1', f='Кинжал оставляет теневой след')
K('desolate', 'Опустошение', 'Desolate', 'ON_HIT', '☾', m=20, cd=20, n=10, oh='ARCANE', f='Одиночки страдают сильнее')
K('dispersion', 'Рассеивание', 'Dispersion', 'BUFF_SELF', '◎', m=25, cd=25, d=8, me='res 8 0', fl='reflect', f='Боль уходит к обидчику')
K('haunt', 'Наваждение', 'Haunt', 'SUMMON', '♅', m=80, cd=90, sm='minecraft:vex', n=5, d=20, f='Призраки нападают на всех врагов')
C('vengeful_specter', 'Призрачная мстительница', 'Vengeful Specter', 'DAMAGE', 0x76448A, 'AGI', 'VIT', 'melee 2 night 2 solo 2',
  'spectral_dagger desolate dispersion shadow_step blur vanish assassinate haunt',
  ('Мстительность', 'lifesteal 3 crit_damage 15'), 'Тень возмездия: кинжал, рассеивание и наваждение.', races='demonkin elf')

# 8. Тролль-берсерк
K('whirling_axes', 'Вращающиеся топоры', 'Whirling Axes', 'NOVA', '⚒', m=25, cd=9, st='STR', pw=1.0, r=4.5, foe='blind 2 0', f='Два топора вокруг тролля')
K('throwing_axes', 'Метательные топоры', 'Axe Throw', 'ARROWS', '⚒', m=20, cd=6, st='STR', pw=1.2, n=3, foe='slow 3 2', f='Пять топоров веером')
K('berserk_rage', 'Ярость тролля', 'Berserker\'s Rage', 'BUFF_SELF', 'Ж', m=20, cd=15, me='haste 15 2; res 15 0', f='Ближний бой — только ближний')
K('battle_trance', 'Боевой транс', 'Battle Trance', 'BUFF_SELF', '♨', m=60, cd=70, me='haste 10 4; spd 10 2; regen 10 1', f='Неуязвимый к смерти угар')
C('troll_berserker', 'Тролль-берсерк', 'Troll Berserker', 'DAMAGE', 0x229954, 'STR', 'AGI', 'melee 3 ranged 1 risk 2',
  'throwing_axes whirling_axes berserk_rage blood_rage rampage unstoppable execute battle_trance',
  ('Кровь тролля', 'attack_speed 12 health_regen 1'), 'Тролль с двумя топорами: метает, кружит и впадает в транс.', races='orc')

# 9. Медвежий воитель
K('earthshock', 'Земной удар', 'Earthshock', 'NOVA', '⊚', m=20, cd=8, st='STR', pw=0.9, r=4, foe='slow 3 3', f='Медведь бьёт лапами о землю')
K('overpower', 'Подавление', 'Overpower', 'BUFF_SELF', '⫸', m=20, cd=10, me='haste 5 4', f='Серия быстрых ударов')
K('fury_swipes', 'Яростные взмахи', 'Fury Swipes', 'ON_HIT', '⋒', m=15, cd=15, n=12, oh='BLEED', f='Каждый удар глубже предыдущего')
K('enrage', 'Бешенство', 'Enrage', 'BUFF_SELF', 'ʘ', m=60, cd=60, me='str 12 3; res 12 2', f='Медведь в бешенстве')
C('ursa_warrior', 'Медвежий воитель', 'Ursa Warrior', 'DAMAGE', 0x784212, 'STR', 'AGI', 'melee 3 beast 2 solo 1',
  'earthshock overpower fury_swipes maul swipe rampage thick_hide enrage',
  ('Когти', 'damage 2 attack_speed 6'), 'Зверь-воин: подавление, яростные взмахи и бешенство.', races='beastkin')

# 10. Отражающий клинок
K('swift_strike', 'Стремительный выпад', 'Swift Strike', 'DASH_STRIKE', '⇒', m=20, cd=6, st='AGI', pw=1.2, r=3.2, f='Рывок с разрезом')
K('deflect', 'Отражение', 'Deflect', 'BUFF_SELF', '⛨', m=20, cd=12, d=2, me='res 2 3', fl='reflect', f='Клинок отбивает всё')
K('wall_climb', 'Бег по стене', 'Wall Run', 'GRAPPLE', '⇡', m=10, cd=6, r=12, f='Вверх по стене')
K('dragonblade', 'Клинок дракона', 'Dragonblade', 'BUFF_SELF', '龍', m=70, cd=70, me='str 12 3; spd 12 1; haste 12 2', f='Дух дракона в клинке')
C('reflector', 'Отражающий клинок', 'Reflector', 'DAMAGE', 0x58D68D, 'AGI', 'STR', 'melee 3 stealth 1 explore 1',
  'shuriken swift_strike deflect wall_climb flurry crossing_slash blade_dance dragonblade',
  ('Кибер-тело', 'dodge 6 move_speed 6', 'featherfall'), 'Ловкий мечник: отбивает снаряды, бегает по стенам, призывает клинок дракона.',
  races='human elf')

# 11. Хронобегунья
K('blink_dash', 'Скачок', 'Blink', 'BLINK', '»', m=8, cd=2.5, r=7, f='Короткий скачок в пространстве')
K('dual_pistols', 'Пистолеты', 'Pulse Pistols', 'BARRAGE', '⁘', m=10, cd=3, st='AGI', pw=0.35, n=8, f='Очередь из двух стволов')
K('pulse_bomb', 'Импульсная бомба', 'Pulse Bomb', 'MINE', '◉', m=40, cd=30, st='AGI', pw=2.5, r=4, d=20, f='Липкая бомба')
K('chrono_recall', 'Возврат', 'Recall', 'RECALL', '⟲', m=20, cd=14, pw=0.4, f='Назад во времени, раны как не бывало')
C('chronorunner', 'Хронобегунья', 'Chronorunner', 'DAMAGE', 0xF39C12, 'AGI', 'LUCK', 'ranged 2 stealth 1 solo 1 explore 2',
  'dual_pistols blink_dash chrono_recall leap_back flurry tripwire smoke_bomb pulse_bomb',
  ('Ускорители', 'move_speed 10 dodge 5'), 'Скачет в пространстве и времени: скачки, возврат и бомба.', races='human')

# 12. Жнец теней
K('wraith_form', 'Облик призрака', 'Wraith Form', 'STEALTH', '◌', m=20, cd=14, d=3, r=0, me='spd 3 2; res 3 3', f='Неуязвим, но не бьёт')
K('hellfire_shotguns', 'Адские обрезы', 'Hellfire Shotguns', 'CONE', '⁘', m=12, cd=3, el='SHADOW', st='STR', pw=1.2, r=6, fl='steal', f='Каждый выстрел лечит жнеца')
K('shadow_portal', 'Теневой портал', 'Shadow Portal', 'BLINK', '◐', m=20, cd=10, r=16, f='Жнец выходит из тени в новом месте')
K('death_blossom', 'Цветок смерти', 'Death Blossom', 'MULTI_NOVA', '✺', m=80, cd=60, el='SHADOW', st='STR', pw=0.7, r=6, n=10, fl='steal',
  f='Жнец кружит, паля во все стороны')
C('shadow_reaper', 'Жнец теней', 'Shadow Reaper', 'DAMAGE', 0x17202A, 'STR', 'VIT', 'melee 2 night 2 risk 2',
  'hellfire_shotguns wraith_form shadow_portal shadow_step drain_life smoke_bomb fear death_blossom',
  ('Жатва душ', 'lifesteal 6', 'soul_eater'), 'Наёмник в маске: обрезы, облик призрака и цветок смерти.', races='human demonkin')

# 13. Драконоборец
K('dragon_lance', 'Копьё драконоборца', 'Dragonlance', 'BEAM', '↗', m=20, cd=6, st='STR', pw=1.5, r=7, f='Длинное копьё для толстой шкуры')
K('scale_breaker', 'Ломатель чешуи', 'Scalebreaker', 'STRIKE', '⚒', m=25, cd=10, st='STR', pw=1.6, r=4.5, foe='weak 6 2', f='Удар по чешуе')
K('dragon_bane', 'Погибель драконов', 'Dragonbane', 'BUFF_SELF', '♞', m=30, cd=30, me='str 15 1; fireres 15 0; res 15 0', f='Огонь не страшен')
K('slayer_leap', 'Прыжок истребителя', 'Slayer\'s Leap', 'LEAP_SLAM', '⇑', m=60, cd=40, st='STR', pw=2.8, r=4, f='Прыжок на шею чудовищу')
C('dragonslayer', 'Драконоборец', 'Dragonslayer', 'DAMAGE', 0x922B21, 'STR', 'VIT', 'melee 3 fire 1 explore 1',
  'dragon_lance scale_breaker dragon_bane charge execute ancient_leap ground_breaker slayer_leap',
  ('Охотник на великанов', 'dmg_elite 20 res_fire 20', 'giant_slayer'), 'Создан бить тех, кто больше: элиты, боссы, драконы.',
  races='human dwarf')

# 14. Кулачный боец
K('jab', 'Джеб', 'Jab', 'STRIKE', '✊', m=6, cd=1.5, st='STR', pw=0.7, r=4, combo=1, f='Быстрый прямой')
K('hook_punch', 'Хук', 'Hook', 'STRIKE', '⤾', m=12, cd=4, st='STR', pw=1.0, r=4, combo=1, foe='slow 1 2', f='Боковой в челюсть')
K('uppercut', 'Апперкот', 'Uppercut', 'LAUNCH', '⤒', m=20, cd=8, st='STR', pw=1.0, r=3, combo=1, f='Снизу вверх — в воздух')
K('knockout', 'Нокаут', 'Knockout', 'STUN', '✹', m=30, cd=12, st='STR', pw=1.4, r=4.5, d=2.5, fl='finish', f='Добивание серии: свет гаснет')
C('brawler', 'Кулачный боец', 'Brawler', 'DAMAGE', 0xDC7633, 'STR', 'VIT', 'melee 4 solo 1 risk 1',
  'jab hook_punch uppercut knockout iron_body flying_kick unstoppable hundred_fists',
  ('Крепкие кулаки', 'damage 2 health 4'), 'Джеб, хук, апперкот — и нокаут, сила которого растёт от серии.', races='human orc beastkin')

# 15. Корсар
K('cutlass_slash', 'Удар абордажной саблей', 'Cutlass Slash', 'STRIKE', '⚔', m=12, cd=3, st='AGI', pw=1.1, r=4.5, combo=1, f='Удар как на палубе')
K('flintlock', 'Кремнёвый пистоль', 'Flintlock', 'ARROWS', '•', m=15, cd=6, st='AGI', pw=2.0, n=1, f='Один выстрел — но какой')
K('torrent', 'Поток', 'Torrent', 'LAUNCH', '≈', m=30, cd=14, el='FROST', st='AGI', pw=1.0, r=4, f='Гейзер морской воды подбрасывает врагов')
K('ghost_ship', 'Корабль-призрак', 'Ghostship', 'METEOR', '⚓', m=80, cd=70, el='FROST', st='AGI', pw=2.6, r=6, me='res 6 1', f='Призрачный корабль проходит сквозь врагов')
C('corsair', 'Корсар', 'Corsair', 'DAMAGE', 0x2471A3, 'AGI', 'LUCK', 'melee 2 ranged 1 water 3 trade 1',
  'cutlass_slash flintlock torrent grappling_hook kidney_shot smoke_bomb crowd_pleaser ghost_ship',
  ('Морская удача', 'coin_find 25 crit_chance 4', 'water_breathing'), 'Пират: сабля, пистоль, гейзер и корабль-призрак.',
  races='human orc beastkin')

# 16. Налётчик
K('raider_axe', 'Топор налётчика', 'Raider\'s Axe', 'STRIKE', '⚒', m=12, cd=4, pw=1.3, r=4.5, f='Тяжёлый северный топор')
K('shield_rush', 'Таран щитом', 'Shield Rush', 'DASH_STRIKE', '◘', m=20, cd=8, pw=1.0, r=3, foe='slow 2 3', f='Щит вперёд — в строй врага')
K('plunder', 'Грабёж', 'Plunder', 'STEAL', '¤', m=15, cd=10, pw=0.8, r=5, f='Всё ценное — себе')
K('valhalla_rage', 'Ярость Вальхаллы', 'Valhalla Rage', 'BUFF_SELF', '⚔', m=60, cd=60, me='str 15 2; res 15 1; spd 15 1', f='Валькирии смотрят — надо постараться')
C('raider', 'Налётчик', 'Raider', 'DAMAGE', 0x5D6D7E, 'STR', 'VIT', 'melee 3 explore 2 water 1',
  'raider_axe shield_rush plunder throwing_axe war_cry whirlwind rage_leap valhalla_rage',
  ('Северная кровь', 'health 6 coin_find 15'), 'Приходит с моря на ладье: топор, щит и грабёж.', races='human dwarf orc')

# 17. Драгун
K('dragoon_jump', 'Прыжок драгуна', 'Jump', 'LEAP_SLAM', '⇑', m=25, cd=8, st='STR', pw=1.6, r=3.5, f='Высоко в небо — и копьём вниз')
K('pierce_line', 'Прошивающий удар', 'Piercing Thrust', 'BEAM', '↗', m=15, cd=4, st='STR', pw=1.2, r=7, f='Копьё пробивает двоих')
K('lance_charge', 'Атака с копьём', 'Lance Charge', 'DASH_STRIKE', '➽', m=25, cd=10, st='STR', pw=1.5, r=4, f='Разбег и удар')
K('stardiver', 'Звёздный нырок', 'Stardiver', 'LEAP_SLAM', '✦', m=80, cd=60, el='FIRE', st='STR', pw=3.0, r=6, f='Драгун падает, как звезда')
C('dragoon', 'Драгун', 'Dragoon', 'DAMAGE', 0x1F618D, 'STR', 'AGI', 'melee 3 explore 1 risk 1',
  'pierce_line dragoon_jump lance_charge spear_thrust hamstring impale ancient_leap stardiver',
  ('Прыгучесть', 'res_fall 60 damage 1', 'featherfall'), 'Копейщик, что прыгает выше всех и падает на врага сверху.', races='human elf')

# 18. Танцовщица клинков
K('veil_dance', 'Танец покрывал', 'Veil Dance', 'BUFF_SELF', '∿', m=20, cd=20, me='spd 10 1; luck 10 1', f='Шаги не угадать')
K('spinning_blades', 'Кружащиеся клинки', 'Spinning Blades', 'MULTI_NOVA', '✺', m=30, cd=10, st='AGI', pw=0.4, r=4, n=5, f='Платки и лезвия')
K('twirl', 'Пируэт', 'Pirouette', 'DASH', '↻', m=8, cd=3, r=1.8, me='spd 2 1', f='Пируэт в сторону')
K('crescendo', 'Крещендо', 'Crescendo', 'MULTI_NOVA', '♬', m=70, cd=50, st='AGI', pw=0.6, r=5, n=12, fl='perfoe', f='Танец достигает пика')
C('blade_dancer', 'Танцовщица клинков', 'Blade Dancer', 'DAMAGE', 0xF1948A, 'AGI', 'LUCK', 'melee 2 stealth 1 party 1 trade 1',
  'twirl spinning_blades veil_dance feint flurry blade_dance wind_dance crescendo',
  ('Грация', 'dodge 10 attack_speed 5'), 'Танец — её оружие: пируэты, кружащиеся клинки и крещендо.', races='elf human')

# 19. Мастер шеста
K('boundless_strike', 'Безграничный удар', 'Boundless Strike', 'BEAM', '|', m=25, cd=10, st='AGI', pw=1.6, r=10, foe='slow 2 4', f='Шест вытягивается и бьёт по линии')
K('tree_dance', 'Танец на ветвях', 'Tree Dance', 'GRAPPLE', '♣', m=10, cd=5, r=20, f='Прыжок на дерево')
K('mischief', 'Проказа', 'Mischief', 'STEALTH', '❓', m=20, cd=20, d=4, r=0, me='spd 4 1', f='Царь обезьян превращается во что-то безобидное')
K('wukongs_command', 'Приказ Укуна', 'Wukong\'s Command', 'ZONE', '◎', m=80, cd=70, st='AGI', pw=0.8, r=7, d=8, f='Армия двойников бьёт всех внутри круга')
C('staff_master', 'Мастер шеста', 'Staff Master', 'DAMAGE', 0xD4AC0D, 'AGI', 'STR', 'melee 3 stealth 1 explore 1',
  'boundless_strike tree_dance mischief staff_spin palm_strike jump_pack swift_step wukongs_command',
  ('Ловкость обезьяны', 'crit_chance 6 move_speed 6', 'featherfall'), 'Царь обезьян: шест вытягивается, а армия двойников бьёт всех.',
  races='beastkin')

# 20. Отравитель
K('venom_dart', 'Отравленный дротик', 'Venom Dart', 'ARROWS', '➶', m=10, cd=3, st='AGI', pw=0.8, n=1, foe='poison 6 1', f='Тонкий дротик с ядом')
K('toxic_cloud', 'Ядовитое облако', 'Toxic Cloud', 'ZONE_AT', '☁', m=30, cd=16, el='POISON', st='AGI', pw=0.5, r=4, d=8, foe='nausea 3 0', f='Зелёный туман')
K('neurotoxin', 'Нейротоксин', 'Neurotoxin', 'STUN', '☣', m=25, cd=14, el='POISON', st='AGI', pw=0.6, r=6, d=2, foe='weak 6 2', f='Яд парализует нервы')
K('plague_blade', 'Чумной клинок', 'Plague Blade', 'MULTI_STRIKE', '☠', m=50, cd=35, el='POISON', st='AGI', pw=0.6, r=5, n=6, foe='poison 8 2', f='Каждая рана гниёт')
C('poisoner', 'Отравитель', 'Poisoner', 'DAMAGE', 0x7DCEA0, 'AGI', 'INT', 'stealth 2 craft 2 nature 1',
  'venom_dart poison_blade toxic_cloud neurotoxin garrote acid_flask vanish plague_blade',
  ('Иммунитет к ядам', 'crit_chance 4', 'venom'), 'Знает сотню ядов: дротики, облака, нейротоксин.', races='elf demonkin', profs='alchemist herbalist')

# 21. Палач
K('headsman_chop', 'Рубка', 'Headsman\'s Chop', 'STRIKE', '⚒', m=15, cd=5, st='STR', pw=1.4, r=4.5, fl='exec', f='Топор палача знает своё дело')
K('verdict', 'Приговор', 'Verdict', 'MARK', '⚖', m=15, cd=12, r=30, d=15, f='Виновен')
K('gallows', 'Виселица', 'Gallows', 'PULL', '⍑', m=20, cd=12, r=16, foe='slow 3 3', f='Петля тянет к эшафоту')
K('guillotine', 'Гильотина', 'Guillotine', 'STRIKE', '▼', m=60, cd=40, st='STR', pw=3.5, r=5, fl='exec', f='Последнее, что слышит приговорённый')
C('executioner', 'Палач', 'Executioner', 'DAMAGE', 0x1B2631, 'STR', 'VIT', 'melee 3 solo 2 night 1',
  'headsman_chop verdict gallows sever skull_splitter execute unstoppable guillotine',
  ('Рука правосудия', 'crit_damage 20', 'executioner'), 'Добивает: казни, приговор и гильотина по ослабленным врагам.', races='human orc')

# 22. Охотник на демонов
K('fel_rush', 'Рывок Скверны', 'Fel Rush', 'DASH_STRIKE', '⇶', m=15, cd=5, el='SHADOW', st='AGI', pw=1.0, r=3.5, f='Рывок в зелёном пламени')
K('eye_beam', 'Пронзающий взгляд', 'Eye Beam', 'BEAM', '◉', m=30, cd=10, el='FIRE', st='AGI', pw=1.8, r=14, f='Глаза горят зелёным огнём')
K('spectral_sight', 'Призрачное зрение', 'Spectral Sight', 'REVEAL', '◎', m=20, cd=30, r=32, d=12, f='Видит сквозь стены и тьму')
K('metamorphosis', 'Метаморфоза', 'Metamorphosis', 'BUFF_SELF', '♆', m=80, cd=90, me='str 20 2; spd 20 1; jump 20 2; hp 20 3; nightv 20 0',
  f='Охотник становится демоном')
C('demon_hunter', 'Охотник на демонов', 'Demon Hunter', 'DAMAGE', 0x1D8348, 'AGI', 'STR', 'melee 2 night 2 fire 1 risk 1',
  'fel_rush eye_beam spectral_sight blade_dance chaos_strike shadow_step whirlwind metamorphosis',
  ('Слепой охотник', 'dmg_nether 25', 'night_eye'), 'Пожертвовал зрением ради силы демонов: взгляд-луч и метаморфоза.',
  races='elf demonkin')

# 23. Мечник крови
K('crimson_cut', 'Багровый разрез', 'Crimson Cut', 'CONE', '⟋', m=0, cd=4, st='STR', pw=1.5, r=5, hp=0.04, fl='steal', f='Кровь на лезвии')
K('blood_boil', 'Кипящая кровь', 'Blood Boil', 'NOVA', '♨', m=0, cd=10, el='SHADOW', st='STR', pw=1.2, r=5, hp=0.06, f='Кровь врагов закипает')
K('hemorrhage', 'Кровоизлияние', 'Hemorrhage', 'ON_HIT', '♦', m=0, cd=20, n=10, oh='BLEED', hp=0.05, f='Раны не закрываются')
K('scarlet_moon', 'Алая луна', 'Scarlet Moon', 'MULTI_NOVA', '☾', m=0, cd=60, st='STR', pw=0.7, r=6, n=8, hp=0.15, fl='steal', f='Небо краснеет')
C('blood_swordsman', 'Мечник крови', 'Blood Swordsman', 'DAMAGE', 0x943126, 'STR', 'VIT', 'melee 3 risk 3',
  'crimson_cut blood_boil hemorrhage blood_strike bloodbath unstoppable blood_pact scarlet_moon',
  ('Кровавый клинок', 'lifesteal 8', 'berserker'), 'Не тратит ману — только кровь. И тут же пьёт её из врагов.', races='demonkin human')

# 24. Псионическая убийца
K('refraction', 'Преломление', 'Refraction', 'BUFF_SELF', '◇', m=25, cd=20, me='abs 10 3; str 10 1', f='Несколько ударов проходят мимо')
K('meld', 'Слияние с тенью', 'Meld', 'STEALTH', '◌', m=15, cd=10, d=4, r=0, me='str 4 2', f='Невидима, пока стоит — и бьёт вдвое')
K('psi_blades', 'Пси-клинки', 'Psi Blades', 'BEAM', '⟿', m=20, cd=6, el='ARCANE', st='AGI', pw=1.3, r=9, f='Удар пробивает стоящих позади')
K('psionic_trap', 'Пси-ловушка', 'Psionic Trap', 'MINE', '◬', m=20, cd=10, el='ARCANE', st='AGI', pw=1.4, r=4, d=60, foe='slow 4 4', f='Невидимая печать')
C('psionic_assassin', 'Псионическая убийца', 'Psionic Assassin', 'DAMAGE', 0xBB8FCE, 'AGI', 'INT', 'stealth 3 magic 2',
  'psi_blades meld refraction psionic_trap shadow_step blink assassinate umbral_prison',
  ('Чистый разум', 'crit_damage 20 res_magic 10'), 'Невидима, пока стоит; пси-клинки бьют насквозь; ловушки повсюду.', races='elf')

# 25. Огненный дух
K('searing_chains', 'Опаляющие цепи', 'Searing Chains', 'NOVA', '⛓', m=25, cd=12, el='FIRE', st='AGI', pw=0.6, r=4, foe='slow 2 6', f='Цепи огня держат ближайших')
K('sleight_of_fist', 'Ловкость кулака', 'Sleight of Fist', 'MULTI_NOVA', '✊', m=30, cd=8, el='FIRE', st='AGI', pw=0.5, r=5, n=4, f='Мгновенные удары по всем вокруг')
K('flame_guard', 'Огненная защита', 'Flame Guard', 'BUFF_SELF', '◍', m=30, cd=25, me='abs 12 3; fireres 12 0', f='Пламя гасит удары')
K('fire_remnant', 'Огненный след', 'Fire Remnant', 'BLINK', '♨', m=40, cd=12, r=14, me='fireres 3 0', f='Дух оставляет копию и прыгает к ней')
C('ember_spirit', 'Огненный дух', 'Ember Spirit', 'DAMAGE', 0xE74C3C, 'AGI', 'INT', 'melee 2 fire 3 magic 1',
  'sleight_of_fist searing_chains flame_guard fire_remnant flame_breath fire_field ring_of_fire meteor',
  ('Пламенная кровь', 'attack_speed 6', 'fireproof flame_touch'), 'Неуловимый огонь: цепи, мгновенные удары и огненные следы.', races='demonkin')

# 26. Хранитель клятвы
K('vow_of_war', 'Клятва битвы', 'Vow of War', 'BUFF_SELF', '⚔', m=0, cd=30, hp=0.1, me='str 20 2', f='Клянусь — враг падёт')
K('vow_of_iron', 'Клятва стали', 'Vow of Iron', 'BUFF_SELF', '▣', m=0, cd=30, hp=0.1, me='res 20 1; abs 20 2', f='Клянусь — я выстою')
K('oath_strike', 'Удар клятвы', 'Oath Strike', 'STRIKE', '✠', m=15, cd=5, el='HOLY', st='STR', pw=1.4, r=4.5, f='Удар во имя клятвы')
K('oathkeeper', 'Сдержанное слово', 'Oathkeeper', 'BUFF_SELF', '✦', m=60, cd=90, me='str 15 2; res 15 1; regen 15 1; spd 15 1', f='Все клятвы исполнены разом')
C('oathsworn', 'Хранитель клятвы', 'Oathsworn', 'DAMAGE', 0xD5D8DC, 'STR', 'VIT', 'melee 3 undead 1 risk 1',
  'oath_strike vow_of_war vow_of_iron holy_strike lunge avenging_wrath judgment oathkeeper',
  ('Слово чести', 'damage 1 health 4 dmg_humanoid 5'), 'Даёт клятвы, платя здоровьем, — и становится сильнее.', races='human dwarf')

# 27. Фехтовальщик-бродяга
K('swashbuckle', 'Удаль', 'Swashbuckle', 'CONE', '⟋', m=15, cd=5, st='AGI', pw=1.0, r=6, f='Серия выпадов перед собой')
K('shield_crash', 'Удар щитом с прыжка', 'Shield Crash', 'LEAP_SLAM', '◘', m=20, cd=10, st='AGI', pw=1.0, r=3.5, me='res 4 1', f='Прыжок и щитом оземь')
K('lucky_shot', 'Счастливый выстрел', 'Lucky Shot', 'ARROWS', '♣', m=15, cd=8, st='LUCK', pw=1.5, n=1, foe='weak 4 1; slow 2 1', f='Повезло — так повезло')
K('rolling_thunder', 'Катящийся гром', 'Rolling Thunder', 'DASH_STRIKE', '◎', m=60, cd=45, st='AGI', pw=2.0, r=5, foe='slow 3 3', f='Свернуться и катиться по врагам')
C('swashbuckler', 'Бродячий фехтовальщик', 'Swashbuckler', 'DAMAGE', 0xAF601A, 'AGI', 'LUCK', 'melee 2 explore 2 trade 1 solo 1',
  'swashbuckle lucky_shot shield_crash riposte feint flurry heart_thrust rolling_thunder',
  ('Бродяга', 'dodge 6 coin_find 15'), 'Весёлый бродяга: удаль, удача и катящийся гром.', races='beastkin human')

# 28. Тень-двойник
K('mirror_swap', 'Зеркальный обмен', 'Mirror Swap', 'SWAP', '⇄', m=20, cd=10, st='AGI', pw=0.9, r=18, f='Ты — это я, я — это ты')
K('doppel_walk', 'Шаг двойника', 'Doppelwalk', 'STEALTH', '♊', m=25, cd=20, d=4, r=0, me='spd 4 2', f='Двойник уходит, а ты остаёшься в тени')
K('twin_strike', 'Двойной удар', 'Twin Strike', 'MULTI_STRIKE', '⫽', m=20, cd=6, st='AGI', pw=0.8, r=5, n=2, combo=1, f='Ты и двойник бьёте разом')
K('legion_of_selves', 'Легион себя', 'Legion of Selves', 'SUMMON', '♊', m=70, cd=70, sm='minecraft:vex', n=4, d=20, f='Десяток двойников')
C('doppelganger', 'Двойник', 'Doppelganger', 'DAMAGE', 0x566573, 'AGI', 'INT', 'stealth 3 magic 1 solo 1',
  'twin_strike mirror_swap doppel_walk shadow_swap coup_de_grace mirror_blades shadow_clone legion_of_selves',
  ('Множественность', 'dodge 8 crit_chance 4'), 'Мастер подмены: меняется местами, бьёт с двойником и зовёт легион себя.',
  races='demonkin elf')

# 29. Охотник на ведьм
K('silver_stake', 'Серебряный кол', 'Silver Stake', 'STRIKE', '✝', m=15, cd=5, el='HOLY', st='AGI', pw=1.4, r=4.5, f='Серебро и осина')
K('witch_bane', 'Погибель ведьм', 'Witchbane', 'STEAL', '⊘', m=20, cd=12, el='HOLY', st='AGI', pw=1.0, r=16, f='Срывает чары с колдуна')
K('holy_water', 'Святая вода', 'Holy Water', 'ZONE_AT', '♒', m=25, cd=14, el='HOLY', st='AGI', pw=0.5, r=4, d=6, f='Брызги жгут нечисть')
K('burn_the_witch', 'Сжечь ведьму!', 'Burn the Witch', 'STUN', '♨', m=50, cd=40, el='FIRE', st='AGI', pw=2.0, r=20, d=3, f='Костёр уже сложен')
C('witch_hunter', 'Охотник на ведьм', 'Witch Hunter', 'DAMAGE', 0x7B7D7D, 'AGI', 'STR', 'undead 2 melee 2 ranged 1 night 1',
  'silver_stake witch_bane holy_water heavy_bolt reveal_heresy chains_of_light pyre burn_the_witch',
  ('Неподкупный', 'res_magic 20 dmg_illager 15'), 'Охотится на колдунов: срывает чары и сжигает ведьм.', races='human')

# 30. Викинг-берсерк? -> Гарпунщик
K('harpoon', 'Гарпун', 'Harpoon', 'PULL', '↩', m=15, cd=8, r=22, foe='slow 2 2', f='Гарпун на верёвке')
K('blubber_slam', 'Удар с размаху', 'Whale Slam', 'NOVA', '≋', m=25, cd=9, st='STR', pw=1.1, r=4.5, f='Удар, от которого колышется вода')
K('ice_fishing', 'Подлёдный лов', 'Ice Fishing', 'STUN', '⌇', m=20, cd=14, el='FROST', st='STR', pw=0.8, r=16, d=2, f='Леска, лёд и сюрприз')
K('leviathan', 'Левиафан', 'Leviathan', 'LAUNCH', '♒', m=80, cd=70, el='FROST', st='STR', pw=2.0, r=8, foe='slow 4 3', f='Из глубин поднимается чудище')
C('harpooner', 'Гарпунщик', 'Harpooner', 'DAMAGE', 0x5499C7, 'STR', 'AGI', 'melee 2 water 3 beast 1',
  'harpoon blubber_slam ice_fishing trident_toss torrent gush kraken_shell leviathan',
  ('Морской охотник', 'dmg_beast 15', 'water_breathing'), 'Китобой: гарпун тянет добычу, а из глубин встаёт левиафан.', races='human orc')
