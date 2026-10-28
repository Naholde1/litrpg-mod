"""Семь танков версии 4.0: у каждого свой ресурс и своя идея, пересечений нет.

Форма набора у всех одна: два дешёвых основных навыка, прерывание, пять тактических
с откатом 12–30 с и два ультимативных на 60–120 с. Спамить нечем — выбирать есть что.
"""
from .dsl import K, C

# ============================== 1. Стражник ==============================
# Ресурс: заряды. Идея: ставит барьеры, за которыми прячется группа.
K('wd_strike', 'Удар щитом', 'Shield Strike', 'STRIKE', '🛡', m=0, cd=4, pw=1.1, r=4.5, foe='slow 2 1', f='Кромкой щита в челюсть')
K('wd_taunt', 'Вызов', 'Challenge', 'TAUNT', '❗', m=0, cd=8, r=12, d=6, me='res 6 0', f='Стражник окликает врагов по имени')
K('wd_break', 'Разбить чары', 'Break the Spell', 'INTERRUPT', '✖', m=20, cd=14, r=6, f='Удар щитом сбивает подготовку')
K('wd_guard', 'Под прикрытие', 'Under Guard', 'SHIELD', '◈', m=20, cd=12, r=16, pw=1.4, d=8, f='Барьер ложится на раненого союзника')
K('wd_barrier', 'Стена света', 'Wall of Light', 'WALL', '▤', m=20, cd=20, r=6, d=10, wall='minecraft:white_stained_glass',
  f='Из воздуха встаёт светящаяся преграда')
K('wd_bash', 'Оглушающий навал', 'Crushing Bash', 'STUN', '✧', m=20, cd=20, pw=1.3, r=4.5, d=2, f='Щит впечатывает врага в пол')
K('wd_pull', 'Крюк стражи', 'Warden Hook', 'PULL', '⟲', m=20, cd=16, r=14, pw=0.6, foe='slow 3 1', f='Цепь выдёргивает врага из строя')
K('wd_push', 'Отбросить', 'Shove Back', 'PUSH', '⇨', m=20, cd=14, pw=0.8, r=6, foe='slow 3 2', f='Ряд щитов сносит всех перед собой')
K('wd_brace', 'Упор', 'Brace', 'BUFF_SELF', '▣', m=20, cd=26, d=10, me='res 10 2; slow 10 0', fl='reflect',
  f='Стражник врастает в пол и отражает удары')
K('wd_dome', 'Купол', 'The Dome', 'ZONE', '◯', m=40, cd=70, el='HOLY', st='VIT', pw=-0.35, r=7, d=10, me='res 10 1',
  f='Светлый купол накрывает арену: внутри бьют слабее')
K('wd_aegis', 'Эгида', 'Aegis', 'BUFF_ALLIES', '⛨', m=40, cd=90, r=14, d=12, me='abs 12 3; res 12 1',
  f='Щит стражника становится общим')
K('wd_rally', 'За мной!', 'To Me!', 'GATHER', '⇶', m=40, cd=75, r=20, f='Отряд рывком оказывается за спиной стражника')
C('warden', 'Стражник', 'Warden', 'TANK', 0xC8D6E5, 'VIT', 'STR', 'tank 5 party 3 craft 1',
  'wd_strike wd_taunt wd_break wd_guard wd_barrier wd_bash wd_pull wd_push wd_brace wd_dome wd_aegis wd_rally',
  ('Бастион', 'armor 4 res_projectile 25'),
  'Держит не себя, а место: барьеры, за которыми прячется вся группа.', res='charges')

# ============================ 2. Кровавый рыцарь ============================
# Ресурс: здоровье. Идея: платит жизнью и лечится нанесённым уроном.
K('bk_strike', 'Кровавый удар', 'Blood Strike', 'STRIKE', '🩸', m=0, cd=4, el='SHADOW', st='STR', pw=1.2, r=4.5, fl='steal',
  f='Клинок пьёт то, что отнял')
K('bk_taunt', 'Запах крови', 'Scent of Blood', 'TAUNT', '☠', m=0, cd=9, r=12, d=6, foe='weak 6 0', f='Враги чуют свою кровь и идут на неё')
K('bk_break', 'Вскрыть', 'Open Up', 'INTERRUPT', '✖', m=10, cd=14, r=5, f='Короткий укол сбивает чтение')
K('bk_leech', 'Кровоток', 'Bloodflow', 'DOT', '〰', m=15, cd=12, el='SHADOW', pw=0.5, r=18, d=10, fl='steal',
  f='Рана не закрывается и кормит рыцаря')
K('bk_nova', 'Алый вихрь', 'Crimson Whirl', 'NOVA', '✺', m=25, cd=14, el='SHADOW', st='STR', pw=1.2, r=5, fl='steal',
  f='Круговой росчерк вытягивает жизнь со всех вокруг')
K('bk_chains', 'Багровые цепи', 'Crimson Chains', 'PULL', '⛓', m=15, cd=16, r=14, pw=0.7, foe='slow 4 1', f='Цепи из застывшей крови')
K('bk_mark', 'Клеймо', 'Blood Brand', 'MARK', '◉', m=15, cd=22, r=16, d=10, pw=1.25, f='Помеченный получает больше от всех')
K('bk_pact', 'Договор', 'The Pact', 'BUFF_SELF', '✦', m=20, cd=30, d=12, me='str 12 1; res 12 0; regen 12 0',
  f='Рыцарь отдаёт кровь и получает силу')
K('bk_shelter', 'Покров крови', 'Blood Shelter', 'SHIELD', '◈', m=25, cd=22, r=14, pw=1.5, d=8, f='Своей кровью закрывает союзника')
K('bk_harvest', 'Жатва', 'Harvest', 'ZONE', '۞', m=35, cd=65, el='SHADOW', st='VIT', pw=0.45, r=7, d=10, fl='steal',
  f='Круг, что пьёт всех внутри и возвращает рыцарю')
K('bk_undying', 'Неумирающий', 'Undying', 'WARD', '♁', m=40, cd=120, r=0, d=10, pw=1.0,
  f='Смертельный удар не убивает — рыцарь остаётся на одном здоровье')
K('bk_crimson', 'Багряный пир', 'Crimson Feast', 'HEAL_SELF', '♥', m=0, cd=45, pw=0.3, f='Выпитая кровь возвращается разом')
C('bloodknight', 'Кровавый рыцарь', 'Blood Knight', 'TANK', 0x8B1A1A, 'VIT', 'STR', 'tank 4 risk 4 melee 2',
  'bk_strike bk_taunt bk_break bk_leech bk_nova bk_chains bk_mark bk_pact bk_shelter bk_harvest bk_undying bk_crimson',
  ('Жизнь за жизнь', 'lifesteal 12 health 4'),
  'Платит за всё собственной кровью и забирает её обратно у врага. Чем ближе к смерти, тем опаснее.', res='health')

# ============================== 3. Законник ==============================
# Ресурс: заряды (печати). Идея: объявляет законы, которые меняют правила боя.
K('lw_strike', 'Удар буквой закона', 'Letter of the Law', 'STRIKE', '§', m=0, cd=4, el='HOLY', st='VIT', pw=1.1, r=4.5,
  f='Печать на кулаке жжёт нарушителя')
K('lw_summon', 'Повестка', 'Summons', 'TAUNT', '✉', m=0, cd=9, r=12, d=6, foe='glow 6 0', me='res 6 0',
  f='Закон требует явиться — и враги являются')
K('lw_silence', 'Закон тишины', 'Law of Silence', 'INTERRUPT', '✖', m=20, cd=14, r=8, f='Чтение заклинаний объявлено вне закона')
K('lw_stillness', 'Закон покоя', 'Law of Stillness', 'ZONE', '◌', m=20, cd=28, el='HOLY', st='VIT', pw=0.3, r=6, d=10,
  foe='slow 10 2', f='Внутри круга бегать запрещено — и это касается всех')
K('lw_temperance', 'Закон воздержания', 'Law of Temperance', 'BUFF_ALLIES', '⚖', m=20, cd=30, r=12, d=14,
  me='res 14 1; fatigue 14 0', f='Отряд бьёт реже, но держит куда больше')
K('lw_wrath', 'Закон возмездия', 'Law of Retribution', 'BUFF_ALLIES', '⚔', m=20, cd=30, r=12, d=14,
  me='str 14 1; vuln 14 1', f='Отряд бьёт сильнее, но и получает больше')
K('lw_seal', 'Печать на уста', 'Seal of Lips', 'STUN', '✧', m=20, cd=20, el='HOLY', st='VIT', pw=1.2, r=5, d=2,
  f='Приговор оглушает')
K('lw_verdict', 'Приговор', 'Verdict', 'MARK', '⊘', m=20, cd=22, r=16, d=10, pw=1.3, f='Названный виновным получает больше')
K('lw_shield', 'Право на защиту', 'Right of Defence', 'SHIELD', '◈', m=20, cd=18, r=16, pw=1.5, d=8, f='Закон закрывает союзника')
K('lw_court', 'Суд', 'The Court', 'STASIS', '⌛', m=40, cd=70, r=5, d=4, f='Время для подсудимых останавливается')
K('lw_codex', 'Полный свод', 'Full Codex', 'BUFF_ALLIES', '📜', m=40, cd=110, r=14, d=16,
  me='res 16 2; str 16 1; regen 16 0', f='Все законы разом — и все в пользу отряда')
K('lw_exile', 'Изгнание', 'Exile', 'PUSH', '⇨', m=20, cd=16, el='HOLY', st='VIT', pw=0.9, r=7, foe='slow 4 2',
  f='Нарушители выдворяются за пределы круга')
C('lawgiver', 'Законник', 'Lawgiver', 'TANK', 0xF5E6A8, 'VIT', 'INT', 'tank 4 party 3 magic 2',
  'lw_strike lw_summon lw_silence lw_stillness lw_temperance lw_wrath lw_seal lw_verdict lw_shield lw_court lw_codex lw_exile',
  ('Буква закона', 'res_magic 20 health 5'),
  'Объявляет законы, по которым идёт бой: они связывают и врагов, и свой отряд.', res='charges')

# ============================ 4. Каменный тотемщик ============================
# Ресурс: заряды (камни). Идея: танкует расстановкой столбов, а не телом.
K('tt_strike', 'Каменный кулак', 'Stone Fist', 'STRIKE', '◼', m=0, cd=4, el='EARTH', st='VIT', pw=1.15, r=4.5, foe='slow 2 1',
  f='Рука на миг становится скалой')
K('tt_call', 'Зов камня', 'Call of Stone', 'TAUNT', '❗', m=0, cd=9, r=12, d=6, me='res 6 0', f='Земля гудит, и враги идут на звук')
K('tt_break', 'Трещина', 'Fissure', 'INTERRUPT', '✖', m=20, cd=14, r=6, f='Пол уходит из-под чтеца')
K('tt_totem_guard', 'Тотем стража', 'Guardian Totem', 'SUMMON', '♜', m=20, cd=35, sm='minecraft:iron_golem', n=1, d=40,
  f='Столб оживает и принимает удары на себя')
K('tt_totem_quake', 'Тотем дрожи', 'Quake Totem', 'ZONE_AT', '◍', m=20, cd=24, el='EARTH', st='VIT', pw=0.5, r=5, d=10,
  foe='slow 10 2', f='Столб трясёт землю вокруг себя')
K('tt_totem_ward', 'Тотем оберега', 'Warding Totem', 'ZONE', '۞', m=20, cd=30, el='EARTH', st='VIT', pw=-0.25, r=6, d=10,
  me='res 10 1', f='Внутри круга камней бьют слабее')
K('tt_wall', 'Частокол камня', 'Stone Palisade', 'WALL', '▥', m=20, cd=22, r=6, d=12, wall='minecraft:cobblestone',
  f='Из пола встаёт ряд камней')
K('tt_skin', 'Гранитная кожа', 'Granite Skin', 'BUFF_SELF', '▩', m=20, cd=28, d=10, me='res 10 1; abs 10 3',
  f='Кожа серая и твёрдая')
K('tt_drag', 'Хватка земли', 'Grip of Earth', 'PULL', '⟲', m=20, cd=16, el='EARTH', st='VIT', r=14, pw=0.6, foe='slow 4 2',
  f='Камень тянет врага к тотему')
K('tt_tomb', 'Каменная гробница', 'Rock Tomb', 'STASIS', '⛰', m=40, cd=65, r=4, d=3, f='Камни смыкаются над врагами')
K('tt_grove', 'Круг предков', 'Circle of Ancestors', 'BUFF_ALLIES', '⬟', m=40, cd=100, r=14, d=16,
  me='res 16 2; regen 16 1', f='Все столбы вспыхивают разом')
K('tt_shatter', 'Обрушить столбы', 'Shatter the Pillars', 'NOVA', '✺', m=20, cd=26, el='EARTH', st='VIT', pw=1.5, r=7,
  foe='slow 4 2', f='Тотемщик разбивает свои камни, и осколки летят во все стороны')
C('totemist', 'Каменный тотемщик', 'Stone Totemist', 'TANK', 0x8C7B6B, 'VIT', 'INT', 'tank 4 mining 3 nature 2',
  'tt_strike tt_call tt_break tt_totem_guard tt_totem_quake tt_totem_ward tt_wall tt_skin tt_drag tt_tomb tt_grove tt_shatter',
  ('Сердце горы', 'armor 3 health 6'),
  'Держит арену расстановкой столбов: они бьют, тормозят и принимают удары вместо людей.', res='charges')

# ============================== 5. Симбиот ==============================
# Ресурс: шкала связи (облик). Идея: делит чужой урон и вытаскивает одного из механики.
K('sy_strike', 'Касание связи', 'Bond Touch', 'STRIKE', '∞', m=0, cd=4, el='ARCANE', st='VIT', pw=1.1, r=4.5,
  f='Удар отдаётся в том, кто связан с тобой')
K('sy_call', 'Зов связи', 'Call of the Bond', 'TAUNT', '❗', m=0, cd=9, r=12, d=6, me='res 6 0', f='Враг чувствует нить и идёт по ней')
K('sy_break', 'Разрыв', 'Severance', 'INTERRUPT', '✖', m=12, cd=14, r=6, f='Нить хлещет по чтецу и сбивает чтение')
K('sy_link', 'Нить жизни', 'Lifeline', 'TETHER', '⌇', m=15, cd=14, el='ARCANE', pw=1.2, r=18, d=12,
  f='Урон по союзнику делится пополам с симбиотом')
K('sy_absorb', 'Впитать', 'Absorb', 'WARD', '◉', m=25, cd=60, r=16, d=5, pw=1.0,
  f='Союзник на пять секунд неуязвим — всё летит в симбиота')
K('sy_share', 'Общая боль', 'Shared Pain', 'MARK', '◈', m=15, cd=22, r=16, d=10, pw=1.25, f='Что получает симбиот, получает и помеченный')
K('sy_swap', 'Подмена', 'Swap', 'SWAP', '⇄', m=15, cd=24, r=18, f='Симбиот меняется местом с союзником')
K('sy_shell', 'Панцирь', 'Carapace', 'BUFF_SELF', '▩', m=15, cd=26, d=10, me='res 10 2; abs 10 2', f='Кожа твердеет под нагрузкой')
K('sy_pull', 'Притянуть нитью', 'Reel In', 'PULL', '⟲', m=15, cd=16, r=14, pw=0.6, foe='slow 3 1', f='Нить выдёргивает врага')
K('sy_web', 'Общая сеть', 'Common Web', 'BUFF_ALLIES', '⁂', m=35, cd=80, r=14, d=14, me='res 14 1; regen 14 1',
  f='Все в отряде связаны: урон размазывается по всем')
K('sy_graft', 'Пересадка', 'Graft', 'HEAL_TARGET', '✚', m=25, cd=30, el='ARCANE', pw=2.2, r=18,
  f='Симбиот отдаёт свою плоть раненому')
K('sy_rupture', 'Разрыв нитей', 'Rupture', 'NOVA', '✺', m=30, cd=55, el='ARCANE', st='VIT', pw=1.6, r=6,
  f='Все нити лопаются разом и бьют по врагам')
C('symbiote', 'Симбиот', 'Symbiote', 'TANK', 0x7FB8A4, 'VIT', 'INT', 'tank 4 support 3 party 2',
  'sy_strike sy_call sy_break sy_link sy_absorb sy_share sy_swap sy_shell sy_pull sy_web sy_graft sy_rupture',
  ('Общая кровь', 'health 6 res_magic 15'),
  'Связывает себя с товарищем и забирает его боль. Единственный, кто вытащит одного человека из механики.', res='form')

# ============================== 6. Гравитон ==============================
# Ресурс: заряды. Идея: собирает толпу в точку, чтобы по ней ударили остальные.
K('gv_strike', 'Тяжёлая длань', 'Heavy Hand', 'STRIKE', '⊙', m=0, cd=4, el='ARCANE', st='VIT', pw=1.1, r=4.5, foe='slow 2 1',
  f='Рука весит как наковальня')
K('gv_call', 'Притяжение', 'Pull of Mass', 'TAUNT', '❗', m=0, cd=9, r=12, d=6, me='res 6 0', f='Враги не могут не идти к нему')
K('gv_break', 'Придавить', 'Press Down', 'INTERRUPT', '✖', m=20, cd=14, r=6, f='Тяжесть ломает чтение на полуслове')
K('gv_vortex', 'Воронка', 'Vortex', 'VORTEX', '◉', m=20, cd=20, el='ARCANE', st='VIT', pw=0.5, r=8, d=4,
  foe='slow 4 2', f='Всех вокруг стягивает в одну точку')
K('gv_press', 'Пресс', 'The Press', 'ZONE', '▼', m=20, cd=26, el='ARCANE', st='VIT', pw=0.45, r=6, d=10,
  foe='slow 10 3; fatigue 10 1', f='В круге всё весит втрое')
K('gv_launch', 'Подбросить', 'Launch', 'LAUNCH', '⬆', m=20, cd=18, pw=1.0, r=6, f='Тяжесть на миг исчезает, и врагов швыряет вверх')
K('gv_slam', 'Приземление', 'Ground Slam', 'LEAP_SLAM', '⇓', m=20, cd=20, el='EARTH', st='VIT', pw=1.5, r=5, foe='slow 3 2',
  f='Прыжок и удар всем весом')
K('gv_shield', 'Небесный щит', 'Sky Shield', 'SHIELD', '◈', m=20, cd=18, r=16, pw=1.5, d=8, f='Над союзником повисает плита света')
K('gv_heavy', 'Своя тяжесть', 'Own Weight', 'BUFF_SELF', '▩', m=20, cd=26, d=10, me='res 10 2; slow 10 1',
  f='Гравитон врастает в пол и становится неподъёмным')
K('gv_collapse', 'Схлопывание', 'Collapse', 'ZONE_AT', '✸', m=40, cd=70, el='ARCANE', st='VIT', pw=1.6, r=7, d=3,
  foe='slow 6 3', f='Точка схлопывается и сминает всё внутри')
K('gv_singular', 'Сингулярность', 'Singularity', 'STASIS', '⬤', m=40, cd=110, r=6, d=4,
  f='В центре арены на четыре секунды останавливается всё')
K('gv_push', 'Отталкивание', 'Repulse', 'PUSH', '⇨', m=20, cd=16, el='ARCANE', st='VIT', pw=0.9, r=7, f='Волна тяжести сносит всех перед собой')
C('graviton', 'Гравитон', 'Graviton', 'TANK', 0x6C5CE7, 'VIT', 'INT', 'tank 4 magic 3 party 2',
  'gv_strike gv_call gv_break gv_vortex gv_press gv_launch gv_slam gv_shield gv_heavy gv_collapse gv_singular gv_push',
  ('Своя орбита', 'armor 3 res_explosion 25'),
  'Собирает толпу в одну точку и держит её там. Без него урон по площади бьёт по воздуху.', res='mana')

# ============================== 7. Зерцало ==============================
# Ресурс: ярость. Она и копится от полученных ударов — ровно то, чем живёт Зерцало.
K('zr_strike', 'Зеркальный удар', 'Mirror Strike', 'STRIKE', '◇', m=0, cd=4, el='ARCANE', st='VIT', pw=1.1, r=4.5,
  f='Клинок повторяет чужой удар')
K('zr_call', 'Отблеск', 'Glare', 'TAUNT', '❗', m=0, cd=9, r=12, d=6, me='res 6 0', foe='glow 6 0', f='Блик слепит и злит')
K('zr_reflect', 'Отразить чары', 'Reflect the Spell', 'INTERRUPT', '↩', m=20, cd=16, r=8,
  f='Сбитое заклинание возвращается тому, кто его читал')
K('zr_facet', 'Грань', 'Facet', 'BUFF_SELF', '▣', m=20, cd=24, d=8, me='res 8 2', fl='reflect',
  f='Пока грань держится, удары возвращаются бьющему')
K('zr_store', 'Копить', 'Store Up', 'BUFF_SELF', '◫', m=20, cd=30, d=10, me='abs 10 3; slow 10 0',
  f='Зерцало впитывает удары вместо того, чтобы их гасить')
K('zr_release', 'Вернуть сполна', 'Give It Back', 'NOVA', '✺', m=20, cd=20, el='ARCANE', st='VIT', pw=1.6, r=6,
  f='Всё накопленное выплёскивается разом')
K('zr_shard', 'Осколок', 'Shard', 'BOLT', '✦', m=20, cd=10, el='ARCANE', st='VIT', pw=1.2, r=20, f='Отколотая грань летит в цель')
K('zr_shield', 'Зеркальный щит', 'Mirror Shield', 'SHIELD', '◈', m=20, cd=18, r=16, pw=1.5, d=8, f='Союзник закрыт стеклом')
K('zr_pull', 'Притянуть отражением', 'Draw Reflection', 'PULL', '⟲', m=20, cd=16, r=14, pw=0.6, f='Отражение тянет к себе оригинал')
K('zr_hall', 'Зеркальный зал', 'Hall of Mirrors', 'ZONE', '۞', m=40, cd=70, el='ARCANE', st='VIT', pw=-0.3, r=7, d=10,
  me='res 10 1', f='Арена множится, и удары теряются в отражениях')
K('zr_double', 'Двойник', 'The Double', 'SUMMON', '♟', m=40, cd=100, sm='minecraft:vex', n=2, d=30,
  f='Два отражения зерцала выходят из стекла')
K('zr_shatter', 'Расколоть', 'Shatter', 'STUN', '✧', m=20, cd=22, el='ARCANE', st='VIT', pw=1.3, r=5, d=2,
  f='Зеркало лопается, и звон оглушает')
C('mirrorknight', 'Зерцало', 'Mirror Knight', 'TANK', 0xAED6F1, 'VIT', 'INT', 'tank 4 magic 3 risk 2',
  'zr_strike zr_call zr_reflect zr_facet zr_store zr_release zr_shard zr_shield zr_pull zr_hall zr_double zr_shatter',
  ('Полированная сталь', 'res_magic 25 armor 2'),
  'Копит полученный урон и возвращает его. Единственный, кто наказывает врага за его же удар.', res='rage')
