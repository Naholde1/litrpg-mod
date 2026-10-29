"""Тринадцать бойцов версии 4.0. У каждого свой ресурс и свой способ наносить урон.

Шаг открытия тот же: два дешёвых основных, прерывание, пять тактических, два ультимативных.
"""
from .dsl import K, C

# уровни открытия сочетаний у заклинателя сфер
_COMBO_LEVELS = [10, 10, 10, 13, 16, 19, 22, 26, 30, 34]


def _invoke(spheres, cast, combos):
    return {'spheres': spheres, 'cast': cast,
            'combos': [[p, k, _COMBO_LEVELS[i]] for i, (p, k) in enumerate(combos)]}

# =============================== 1. Стриж ===============================
# Ресурс: никакой — только откаты (так просил автор класса). Все навыки бесплатны,
# глубина в отменах анимаций, инерции и выборе шести из двенадцати.
K('sw_sickles', 'Парные серпы', 'Twin Sickles', 'MULTI_STRIKE', '✂', m=0, cd=3, pw=0.75, r=4, n=3, combo=1,
  f='Серия из трёх: третий удар уносит на два блока вперёд')
K('sw_dash', 'Срыв', 'Breakaway', 'DASH_STRIKE', '⇶', m=0, cd=6, pw=1.1, r=3.2, fl='phase speed',
  f='Рывок сквозь врага отменяет любую начатую анимацию')
K('sw_kick', 'Подсечка', 'Low Kick', 'INTERRUPT', '✖', m=0, cd=14, r=4, f='Нога под колено сбивает чтение')
K('sw_slide', 'Скольжение', 'Slide', 'BUFF_SELF', '⌁', m=0, cd=10, d=3, me='spd 3 2; dolphin 3 0',
  f='Присед на бегу: хитбокс ниже, снаряды идут над головой')
K('sw_wallbounce', 'Отскок от стены', 'Wall Bounce', 'BLINK', '⤴', m=0, cd=8, r=8, f='Толчок от стены переносит за спину врагу')
K('sw_crossing', 'Перекрестье', 'Crossing', 'DASH_STRIKE', '✕', m=0, cd=12, pw=1.4, r=3.4, fl='back speed',
  f='Проход насквозь: удар считается ударом в спину')
K('sw_flurry', 'Вихрь серпов', 'Sickle Flurry', 'MULTI_NOVA', '✺', m=0, cd=18, pw=0.8, r=4, n=3,
  f='Три оборота вокруг себя, не сходя с места')
K('sw_mark', 'Засечка', 'Notch', 'MARK', '◉', m=0, cd=20, r=14, d=8, pw=1.2, f='Отметка на броне: туда и будут бить')
K('sw_hook', 'Зацеп', 'Hook', 'GRAPPLE', '⇡', m=0, cd=16, r=18, f='Крюк за выступ — и Стриж уже наверху')
K('sw_storm', 'Три стороны', 'Three Sides', 'MULTI_STRIKE', '⚡', m=0, cd=45, pw=1.3, r=4.5, n=6, fl='speed',
  f='Шесть ударов с трёх сторон быстрее, чем успеваешь повернуться')
K('sw_ghost', 'Призрачный шаг', 'Ghost Step', 'SHADOW_STEP', '≫', m=0, cd=60, r=20, fl='back',
  f='Стриж оказывается за спиной цели и бьёт из-за плеча')
K('sw_edge', 'На грани', 'On the Edge', 'BUFF_SELF', '✦', m=0, cd=70, d=10, me='spd 10 2; haste 10 2; jump 10 1',
  f='Пока держится — ни одна анимация не доигрывает до конца')
C('swift', 'Стриж', 'Swift', 'DAMAGE', 0x48C9B0, 'AGI', 'STR', 'melee 4 stealth 3 solo 2',
  'sw_sickles sw_dash sw_kick sw_slide sw_wallbounce sw_crossing sw_flurry sw_mark sw_hook sw_storm sw_ghost sw_edge',
  ('Цена мобильности', 'dodge 12 move_speed 12'),
  'Мобильный дуэлянт без шкал: за один заход бьёт с трёх сторон. Здоровья — 70% от обычного бойца, '
  'и пока он стоит на месте, он слаб и быстро умирает.',
  res='mana', hp=0.7)

# =============================== 2. Берсерк ===============================
# Ресурс: ярость. Копится от ударов, утекает, если остановился.
K('br_cleave', 'Размах', 'Cleave', 'NOVA', '⚒', m=0, cd=4, pw=1.0, r=4.5, combo=1, f='Широкий замах задевает всех рядом')
K('br_rush', 'Наскок', 'Rush', 'DASH_STRIKE', '⇶', m=0, cd=7, pw=1.2, r=3.4, f='Разбег и удар с ходу')
K('br_headbutt', 'Лбом', 'Headbutt', 'INTERRUPT', '✖', m=20, cd=14, r=4, f='Лоб в лоб — чтение сбито')
K('br_frenzy', 'Исступление', 'Frenzy', 'BUFF_SELF', '✦', m=25, cd=24, d=10, me='str 10 1; haste 10 1; vuln 10 1',
  f='Бьёт быстрее и сильнее, но и получает больше')
K('br_rend', 'Рваная рана', 'Rend', 'DOT', '〰', m=20, cd=12, pw=0.55, r=5, d=10, f='Рана не затягивается, пока он рядом')
K('br_throw', 'Швырнуть', 'Hurl', 'LAUNCH', '⬆', m=25, cd=18, pw=1.1, r=5, f='Берсерк подбрасывает всех вокруг')
K('br_roar', 'Рёв', 'Roar', 'TAUNT', '❗', m=20, cd=20, r=10, d=6, foe='weak 6 1', f='От рёва у врагов подгибаются ноги')
K('br_whirl', 'Мясорубка', 'Meat Grinder', 'MULTI_NOVA', '✺', m=35, cd=22, pw=0.9, r=5, n=4, f='Четыре оборота без остановки')
K('br_leap', 'Прыжок зверя', 'Beast Leap', 'LEAP_SLAM', '⇓', m=30, cd=20, pw=1.5, r=4.5, foe='slow 3 2',
  f='Приземление вминает землю')
K('br_bloodbath', 'Кровавая баня', 'Bloodbath', 'MULTI_STRIKE', '🩸', m=60, cd=60, pw=1.4, r=4.5, n=5, fl='steal finish',
  f='Добивание: чем больше приёмов накоплено, тем страшнее')
K('br_unstoppable', 'Неостановимый', 'Unstoppable', 'BUFF_SELF', '♆', m=70, cd=110, d=12,
  me='str 12 2; res 12 1; spd 12 1; regen 12 0', f='Пока идёт — его не свалить')
K('br_execute', 'Добить', 'Execute', 'STRIKE', '☠', m=30, cd=10, pw=1.8, r=4.5, fl='exec',
  f='Раненых добивает с одного замаха')
C('berserker', 'Берсерк', 'Berserker', 'DAMAGE', 0xE74C3C, 'STR', 'VIT', 'melee 5 risk 4 solo 2',
  'br_cleave br_rush br_headbutt br_frenzy br_rend br_throw br_roar br_whirl br_leap br_bloodbath br_unstoppable br_execute',
  ('Чем больнее, тем злее', 'damage 4 lifesteal 8'),
  'Копит ярость ударами и теряет её, если остановился. Единственный, кого наказывает пауза.', res='rage')

# =============================== 3. Монах ===============================
# Ресурс: заряды (печати ци). Бьёт не руками, а волнами на расстоянии; комбо из трёх ступеней.
K('mk_palm', 'Ладонь', 'Open Palm', 'BEAM', '✋', m=0, cd=3.5, el='ARCANE', st='AGI', pw=0.9, r=7, combo=1,
  f='Волна ци уходит с ладони на семь блоков')
K('mk_step', 'Шаг ветра', 'Wind Step', 'DASH', '⇶', m=0, cd=7, fl='phase', f='Монах проходит сквозь строй')
K('mk_pressure', 'Точка давления', 'Pressure Point', 'INTERRUPT', '✖', m=20, cd=14, r=6, f='Палец в точку — и заклинание рассыпалось')
K('mk_seal_one', 'Первая печать', 'First Seal', 'ZONE_AT', '①', m=20, cd=12, el='ARCANE', st='AGI', pw=0.8, r=4, d=6,
  f='Печать висит в воздухе и бьёт тех, кто рядом')
K('mk_seal_two', 'Вторая печать', 'Second Seal', 'ZONE_AT', '②', m=20, cd=16, el='ARCANE', st='AGI', pw=1.1, r=4, d=6,
  foe='slow 6 2', f='Вторая печать тяжелее первой и держит на месте')
K('mk_seal_three', 'Третья печать', 'Third Seal', 'ZONE_AT', '③', m=40, cd=24, el='ARCANE', st='AGI', pw=1.6, r=5, d=5,
  foe='weak 5 1', f='Третья печать смыкает круг')
K('mk_chain', 'Цепь ци', 'Chain of Chi', 'CHAIN', '⚯', m=20, cd=14, el='ARCANE', st='AGI', pw=1.0, r=12, n=4,
  f='Волна перескакивает с врага на врага')
K('mk_calm', 'Покой', 'Stillness', 'BUFF_SELF', '◌', m=20, cd=26, d=10, me='res 10 1; regen 10 1',
  f='Между ударами монах стоит неподвижно и восстанавливается')
K('mk_sweep', 'Подсечка круга', 'Circle Sweep', 'LAUNCH', '⬆', m=20, cd=18, el='ARCANE', st='AGI', pw=1.0, r=5,
  f='Низкая волна сбивает всех с ног')
K('mk_hundred', 'Сто ладоней', 'Hundred Palms', 'MULTI_STRIKE', '✺', m=40, cd=55, el='ARCANE', st='AGI', pw=1.2, r=6, n=8,
  fl='finish', f='Восемь волн подряд в одну цель')
K('mk_mountain', 'Гора падает', 'The Mountain Falls', 'METEOR', '⛰', m=40, cd=100, el='ARCANE', st='AGI', pw=2.6, r=5,
  f='Ци обрушивается с неба в выбранную точку')
K('mk_mirror', 'Отвести удар', 'Turn the Blow', 'BUFF_SELF', '↩', m=20, cd=30, d=6, me='res 6 2', fl='reflect',
  f='Монах принимает удар и возвращает его')
C('monk', 'Монах', 'Monk', 'DAMAGE', 0xF39C12, 'AGI', 'INT', 'melee 3 magic 3 solo 2 party 1',
  'mk_palm mk_step mk_pressure mk_seal_one mk_seal_two mk_seal_three mk_chain mk_calm mk_sweep mk_hundred mk_mountain mk_mirror',
  ('Внутренняя сила', 'spell_power 10 dodge 8'),
  'Рукопашник, который достаёт издалека: ставит печати и бьёт волнами ци по порядку, а не вразнобой.',
  res='charges')

# ============================== 4. Призыватель ==============================
# Ресурс: души (сферы). Играет не собой, а армией.
K('sm_bolt', 'Зов души', 'Soul Call', 'TARGET_BOLT', '✦', m=0, cd=1.4, el='SHADOW', pw=0.7, r=28,
  f='Слабый снаряд, зато душа остаётся призывателю')
K('sm_shield', 'Щит из душ', 'Soul Shield', 'SHIELD', '◈', m=0, cd=14, el='SHADOW', r=14, pw=1.3, d=8,
  f='Души встают вокруг союзника')
K('sm_silence', 'Оборвать', 'Cut Short', 'INTERRUPT', '✖', m=0, cd=14, r=10, f='Души глушат чужое заклинание')
K('sm_skeletons', 'Поднять костяк', 'Raise Bones', 'SUMMON', '☠', m=0, cd=22, sm='minecraft:skeleton', n=2, d=45,
  f='Двое поднимаются из пола')
K('sm_wolves', 'Свора теней', 'Shadow Pack', 'SUMMON', '🐺', m=0, cd=26, sm='minecraft:wolf', n=3, d=40,
  f='Три тени бросаются вперёд')
K('sm_golem', 'Костяной голем', 'Bone Golem', 'SUMMON', '♜', m=0, cd=70, sm='minecraft:iron_golem', n=1, d=60,
  f='Тяжёлая тварь, что держит удар за хозяина')
K('sm_command', 'Приказ', 'Command', 'BUFF_ALLIES', '❗', m=0, cd=20, r=16, d=12, me='str 12 1; haste 12 1',
  f='Слуги бьют быстрее и злее')
K('sm_sacrifice', 'Разменять', 'Expend', 'SACRIFICE', '⇅', m=0, cd=18, pw=0.35, f='Слуга рассыпается, и сила возвращается хозяину')
K('sm_drain', 'Вытянуть душу', 'Drain Soul', 'DOT', '〰', m=0, cd=12, el='SHADOW', pw=0.5, r=20, d=10, fl='steal',
  f='Душа цели понемногу переходит призывателю')
K('sm_legion', 'Легион', 'Legion', 'SUMMON', '⚑', m=0, cd=110, sm='minecraft:vindicator', n=4, d=50,
  f='Четверо встают разом')
K('sm_burst', 'Разрыв душ', 'Soul Burst', 'NOVA', '✺', m=0, cd=50, el='SHADOW', pw=1.7, r=7,
  f='Все слуги лопаются, и это больно всем вокруг')
K('sm_bind', 'Привязь', 'Bind', 'DOMINATE', '⛓', m=0, cd=80, r=14, d=20, f='Чужой монстр переходит на сторону призывателя')
C('summoner', 'Призыватель', 'Summoner', 'DAMAGE', 0x9B59B6, 'INT', 'VIT', 'magic 4 undead 3 party 2',
  'sm_bolt sm_shield sm_silence sm_skeletons sm_wolves sm_golem sm_command sm_sacrifice sm_drain sm_legion sm_burst sm_bind',
  ('Хозяин душ', 'spell_power 12 health 4'),
  'Единственный, кто играет не собой: армия слуг, которой он командует.', res='orbs')

# =============================== 5. Оборотень ===============================
# Ресурс: шкала облика. Три формы, у каждой свои удары; шкала тратится, пока держишь.
K('ws_claw', 'Когти', 'Claws', 'STRIKE', '🐾', m=0, cd=3.5, st='AGI', pw=1.0, r=4, combo=1, f='Короткий замах, зато без паузы')
K('ws_pounce', 'Прыжок', 'Pounce', 'DASH_STRIKE', '⇶', m=0, cd=7, st='AGI', pw=1.2, r=3.4, f='Зверь накрывает цель с разбега')
K('ws_snap', 'Хватка челюстей', 'Jaw Snap', 'INTERRUPT', '✖', m=10, cd=14, r=4, f='Зубы на горле сбивают чтение')
K('ws_wolf', 'Облик волка', 'Wolf Form', 'BUFF_SELF', '🐺', m=15, cd=20, d=14, me='spd 14 2; haste 14 1',
  fl='form',
  f='Быстрый и злой: бьёт часто, держит плохо')
K('ws_bear', 'Облик медведя', 'Bear Form', 'BUFF_SELF', '🐻', m=15, cd=20, d=14, me='str 14 2; res 14 1; slow 14 0',
  fl='form',
  f='Тяжёлый и медленный: бьёт редко и страшно')
K('ws_raven', 'Облик ворона', 'Raven Form', 'BUFF_SELF', '🐦', m=15, cd=20, d=14, me='slowfall 14 0; jump 14 2; spd 14 1',
  fl='form',
  f='Лёгкий: прыгает далеко и падает мягко')
K('ws_rend', 'Рвать', 'Rend', 'DOT', '〰', m=15, cd=12, pw=0.55, r=4.5, d=10, f='Рваная рана кровит')
K('ws_howl', 'Вой', 'Howl', 'TAUNT', '❗', m=15, cd=20, r=12, d=6, foe='weak 6 1', f='Вой достаёт до костей')
K('ws_frenzy', 'Звериный раж', 'Beast Frenzy', 'MULTI_NOVA', '✺', m=25, cd=24, st='AGI', pw=0.85, r=4.5, n=3,
  f='Зверь крутится волчком среди врагов')
K('ws_alpha', 'Вожак', 'Alpha', 'SUMMON', '🐺', m=35, cd=70, sm='minecraft:wolf', n=3, d=45, f='Стая выходит на зов')
K('ws_primal', 'Первобытный облик', 'Primal Form', 'BUFF_SELF', '♆', m=60, cd=110, d=14, fl='form',
  me='str 14 2; spd 14 2; res 14 1; regen 14 1', f='Все три облика разом, пока хватает шкалы')
K('ws_leap', 'Полёт ворона', 'Raven Flight', 'BLINK', '⤴', m=15, cd=16, r=14, f='Короткий перелёт над строем')
C('werebeast', 'Оборотень', 'Werebeast', 'DAMAGE', 0x7D6608, 'AGI', 'STR', 'melee 4 beast 4 night 2',
  'ws_claw ws_pounce ws_snap ws_wolf ws_bear ws_raven ws_rend ws_howl ws_frenzy ws_alpha ws_primal ws_leap',
  ('Три шкуры', 'move_speed 10 damage 3'),
  'Три облика с разными ударами. Шкала тратится, пока держишь форму.', res='form')

# =============================== 6. Механик ===============================
# Ресурс: заряды (детали). Урон идёт от конструкций, а не от него самого.
K('mc_bolt', 'Болт', 'Bolt Shot', 'TARGET_BOLT', '✦', m=0, cd=1.4, st='AGI', pw=0.75, r=28, f='Короткий выстрел из наручного арбалета')
K('mc_roll', 'Откат', 'Roll Out', 'DASH', '⇶', m=0, cd=7, f='Перекат с колена')
K('mc_jam', 'Заклинить', 'Jam', 'INTERRUPT', '✖', m=20, cd=14, r=12, f='Болт в руку — и чтение сорвано')
K('mc_turret', 'Турель', 'Turret', 'SUMMON', '⚙', m=20, cd=30, sm='minecraft:snow_golem', n=1, d=40, fl='turret',
  f='Станок сам выбирает цель и бьёт очередями')
K('mc_mine', 'Мина', 'Mine', 'MINE', '◍', m=20, cd=16, pw=1.5, r=3.5, d=30, foe='slow 4 2',
  f='Невидимая пластина под ногами')
K('mc_harpoon', 'Гарпун', 'Harpoon', 'PULL', '⟲', m=20, cd=16, r=16, pw=0.7, f='Трос выдёргивает цель из строя')
K('mc_barrage', 'Очередь', 'Burst Fire', 'BARRAGE', '⁙', m=20, cd=12, st='AGI', pw=0.6, r=22, n=5,
  f='Пять болтов подряд из многозарядного ложа')
K('mc_net', 'Сеть', 'Net', 'ZONE_AT', '⁂', m=20, cd=20, st='AGI', pw=0.3, r=4.5, d=8, foe='slow 8 3',
  f='Сеть накрывает участок и путает ноги')
K('mc_oil', 'Масло и искра', 'Oil and Spark', 'ZONE_AT', '🔥', m=20, cd=22, el='FIRE', st='AGI', pw=0.9, r=5, d=8,
  f='Лужа масла вспыхивает от искры')
K('mc_mortar', 'Мортира', 'Mortar', 'METEOR', '◉', m=40, cd=60, st='AGI', pw=2.2, r=5, f='Навесной выстрел в отмеченную точку')
K('mc_arsenal', 'Полный арсенал', 'Full Arsenal', 'SUMMON', '⚑', m=40, cd=110, sm='minecraft:snow_golem', n=3, d=45,
  fl='turret', f='Три станка разом по краям арены')
K('mc_overclock', 'Разгон', 'Overclock', 'BUFF_SELF', '✦', m=20, cd=30, d=10, me='haste 10 2; spd 10 1',
  f='Механизмы работают вдвое быстрее, пока не перегрелись')
C('machinist', 'Механик', 'Machinist', 'DAMAGE', 0xB7950B, 'AGI', 'INT', 'ranged 4 craft 4 solo 2',
  'mc_bolt mc_roll mc_jam mc_turret mc_mine mc_harpoon mc_barrage mc_net mc_oil mc_mortar mc_arsenal mc_overclock',
  ('Станок вместо рук', 'arrow_damage 12 cooldown 8'),
  'Урон идёт от того, что он поставил: турели, мины и гарпуны. Сам он почти не бьёт.', res='charges')

# ============================ 7. Чумной владыка ============================
# Ресурс: мана (споры). Превращает толпу врагов в свою армию.
K('pl_spit', 'Плевок', 'Spit', 'TARGET_BOLT', '✦', m=8, cd=1.4, el='POISON', pw=0.75, r=28, f='Комок спор летит в цель')
K('pl_step', 'Шаг в тумане', 'Mist Step', 'DASH', '⇶', m=10, cd=7, f='Владыка растворяется и собирается на шаг дальше')
K('pl_choke', 'Удушье', 'Choke', 'INTERRUPT', '✖', m=14, cd=14, r=14, f='Споры в горле обрывают заклинание')
K('pl_infect', 'Заразить', 'Infect', 'DOT', '☣', m=16, cd=10, el='POISON', pw=0.55, r=20, d=12,
  f='Больной разносит заразу на соседей')
K('pl_convert', 'Обратить', 'Turn', 'CHARM', '☯', m=30, cd=26, r=14, d=14,
  f='Заражённый перестаёт видеть в вас врага и бьёт своих')
K('pl_thrall', 'Подчинить', 'Enthrall', 'DOMINATE', '⛓', m=45, cd=60, r=14, d=24,
  f='Заражённый переходит под руку владыки целиком')
K('pl_cloud', 'Споровое облако', 'Spore Cloud', 'ZONE_AT', '◍', m=28, cd=20, el='POISON', pw=0.7, r=6, d=10,
  foe='slow 10 1; nausea 10 0', f='Облако висит и травит всех внутри')
K('pl_burst', 'Разрыв', 'Burst', 'NOVA', '✺', m=26, cd=16, el='POISON', pw=1.3, r=6,
  f='Заражённые лопаются и обдают соседей')
K('pl_wither', 'Увядание', 'Wither', 'MARK', '◉', m=18, cd=22, r=18, d=10, pw=1.3, f='Помеченный гниёт заживо')
K('pl_plague', 'Мор', 'Plague', 'GLOBAL', '☠', m=55, cd=70, el='POISON', pw=1.4, r=16,
  f='Зараза прокатывается по всем врагам вокруг')
K('pl_swarm', 'Рой', 'The Swarm', 'SUMMON', '🐝', m=60, cd=110, sm='minecraft:bee', n=5, d=40,
  f='Пять носителей заразы вылетают из владыки')
K('pl_shield', 'Корка', 'Crust', 'BUFF_SELF', '▩', m=20, cd=28, d=10, me='res 10 1; abs 10 2',
  f='Тело покрывается плотной коркой гриба')
C('plaguelord', 'Чумной владыка', 'Plague Lord', 'DAMAGE', 0x7F8C45, 'INT', 'VIT', 'magic 4 nature 3 party 2',
  'pl_spit pl_step pl_choke pl_infect pl_convert pl_thrall pl_cloud pl_burst pl_wither pl_plague pl_swarm pl_shield',
  ('Носитель', 'spell_power 10 res_magic 10'),
  'Превращает толпу врагов в свою силу: заражает, обращает и подчиняет. На волнах мобов опаснее всех.',
  res='mana')

# ================================ 8. Дух ================================
# Ресурс: шкала одержимости. Сам почти ничего не умеет — берёт чужое тело.
K('sp_touch', 'Касание', 'Touch', 'TARGET_BOLT', '✦', m=0, cd=1.6, el='ARCANE', pw=0.6, r=24,
  f='Слабое касание: сам дух почти не бьёт')
K('sp_drift', 'Просочиться', 'Drift', 'BLINK', '≫', m=0, cd=8, r=14, fl='phase', f='Дух проходит сквозь тела и камень')
K('sp_wail', 'Вой духа', 'Spirit Wail', 'INTERRUPT', '✖', m=10, cd=14, r=12, f='Голос из ниоткуда обрывает чтение')
K('sp_possess', 'Вселиться', 'Possess', 'DOMINATE', '☯', m=35, cd=35, r=14, d=25, fl='possess',
  f='Дух входит в монстра и дерётся его телом и его ударами')
K('sp_mimic', 'Чужая повадка', 'Borrowed Trick', 'ON_HIT', '✦', m=15, cd=20, d=12, n=5, oh='ARCANE',
  f='Дух повторяет приём того, в ком сидел')
K('sp_fear', 'Страх', 'Dread', 'CHARM', '☠', m=25, cd=28, r=12, d=10, f='Враги разбегаются от холода')
K('sp_chill', 'Могильный холод', 'Grave Chill', 'ZONE', '❄', m=24, cd=22, el='FROST', pw=0.6, r=6, d=10,
  foe='slow 10 2', f='Вокруг духа стынет воздух')
K('sp_siphon', 'Вытянуть', 'Siphon', 'DOT', '〰', m=16, cd=12, el='SHADOW', pw=0.5, r=20, d=10, fl='steal',
  f='Дух пьёт чужую жизнь на расстоянии')
K('sp_shell', 'Пустая оболочка', 'Empty Shell', 'SUMMON', '♟', m=30, cd=50, sm='minecraft:vex', n=2, d=35,
  f='Брошенные тела встают и идут за духом')
K('sp_legion', 'Хор мёртвых', 'Chorus of the Dead', 'GLOBAL', '☠', m=55, cd=75, el='SHADOW', pw=1.3, r=16,
  f='Все, в ком дух успел побывать, кричат разом')
K('sp_deep', 'Глубокое вселение', 'Deep Possession', 'DOMINATE', '⛓', m=70, cd=120, r=16, d=40, fl='possess',
  f='Дух берёт даже мини-хранителя, если уже убивал его')
K('sp_fade', 'Истончиться', 'Thin Out', 'STEALTH', '◌', m=20, cd=30, d=8, me='invis 8 0; spd 8 1',
  f='Дух почти исчезает и проходит мимо')
C('spirit', 'Дух', 'Spirit', 'DAMAGE', 0xAAB7B8, 'INT', 'AGI', 'magic 3 stealth 3 night 3',
  'sp_touch sp_drift sp_wail sp_possess sp_mimic sp_fear sp_chill sp_siphon sp_shell sp_legion sp_deep sp_fade',
  ('Чужими руками', 'spell_power 8 dodge 10'),
  'Самый странный класс: сам слаб, но вселяется в монстров и дерётся их телами. Его сила — бестиарий.',
  res='form')

# ================================ 9. Тень ================================
# Ресурс: заряды (тени). Живёт на входе и выходе из боя.
K('sh_stab', 'Удар в бок', 'Side Stab', 'STRIKE', '🗡', m=0, cd=3.5, st='AGI', pw=1.0, r=4, combo=1, f='Короткий точный укол')
K('sh_slip', 'Ускользнуть', 'Slip Away', 'DASH', '⇶', m=0, cd=7, fl='phase', f='Тень проскальзывает мимо')
K('sh_gag', 'Зажать рот', 'Gag', 'INTERRUPT', '✖', m=20, cd=14, r=4, f='Ладонь на рот — заклинание не дочитано')
K('sh_vanish', 'Исчезнуть', 'Vanish', 'STEALTH', '◌', m=20, cd=22, d=8, me='invis 8 0; spd 8 1',
  f='Тень пропадает из виду, пока не ударит')
K('sh_ambush', 'Из темноты', 'Out of the Dark', 'SHADOW_STEP', '≫', m=20, cd=18, st='AGI', pw=1.6, r=18, fl='back',
  f='Выход из невидимости сразу за спиной цели')
K('sh_mark', 'Метка жертвы', 'Quarry Mark', 'MARK', '◉', m=20, cd=20, r=16, d=10, pw=1.3, f='Помеченный виден сквозь стены')
K('sh_poison', 'Яд на клинке', 'Blade Venom', 'ON_HIT', '☠', m=20, cd=24, d=14, n=6, oh='POISON',
  f='Шесть следующих ударов травят')
K('sh_smoke', 'Дымовая', 'Smoke', 'ZONE', '◍', m=20, cd=24, pw=0.2, r=5, d=8, foe='blind 8 0',
  f='Облако дыма: враги внутри не видят')
K('sh_throw', 'Веер ножей', 'Knife Fan', 'ARROWS', '⁙', m=20, cd=12, st='AGI', pw=0.7, r=16, n=5, f='Пять ножей веером')
K('sh_execute', 'Перерезать', 'Cut the Throat', 'STRIKE', '☠', m=40, cd=45, st='AGI', pw=2.4, r=4, fl='exec back',
  f='Добивание со спины — одним движением')
K('sh_night', 'Ночь', 'The Night', 'ZONE', '🌑', m=40, cd=100, el='SHADOW', st='AGI', pw=1.1, r=8, d=10,
  foe='blind 10 0; slow 10 1', f='Тьма накрывает арену, и в ней видит только тень')
K('sh_trap', 'Растяжка', 'Tripwire', 'MINE', '◍', m=20, cd=18, st='AGI', pw=1.4, r=3, d=30, foe='slow 4 2',
  f='Тонкая нить поперёк прохода')
C('shade', 'Тень', 'Shade', 'DAMAGE', 0x34495E, 'AGI', 'LUCK', 'stealth 5 melee 3 night 2',
  'sh_stab sh_slip sh_gag sh_vanish sh_ambush sh_mark sh_poison sh_smoke sh_throw sh_execute sh_night sh_trap',
  ('Из-за спины', 'crit_chance 10 crit_damage 20'),
  'Играет на входе и выходе из боя: исчезает, бьёт со спины и уходит обратно.', res='charges')

# =============================== 10. Астромант ===============================
# Ресурс: сферы. Три сферы висят над головой и дают усиления; Призыв сплетает их в одно
# из десяти заклинаний — какое, решает сочетание. Урон идёт от сочетаний, а не от кнопок.
K('as_orb_fire', 'Сфера огня', 'Orb of Fire', 'ORB', '🔥', m=0, cd=0, el='FIRE', me='str 3 0', f='Жар: сила')
K('as_orb_frost', 'Сфера льда', 'Orb of Frost', 'ORB', '❄', m=0, cd=0, el='FROST', me='res 3 0', f='Стужа: стойкость')
K('as_orb_storm', 'Сфера бури', 'Orb of Storm', 'ORB', '⚡', m=0, cd=0, el='LIGHTNING', me='spd 3 0', f='Разряд: скорость')
K('as_weave', 'Сплести', 'Weave', 'INVOKE', '✪', m=0, cd=4, f='Три сферы складываются в заклинание')
K('as_supernova', 'Сверхновая', 'Supernova', 'ZONE_AT', '✹', m=0, cd=50, el='FIRE', pw=3.0, r=8, d=4,
  f='Огонь-Огонь-Огонь: точка вспыхивает так, что смотреть нельзя')
K('as_zero', 'Абсолютный ноль', 'Absolute Zero', 'STUN', '❄', m=0, cd=24, el='FROST', pw=1.0, r=20, d=3,
  f='Лёд-Лёд-Лёд: цель вмерзает в пол')
K('as_thunder', 'Столп грома', 'Thunder Pillar', 'LIGHTNING', '⚡', m=0, cd=26, el='LIGHTNING', pw=3.0, r=40,
  f='Буря-Буря-Буря: разряд бьёт с неба в одну точку')
K('as_comet', 'Комета', 'Comet', 'METEOR', '☄', m=0, cd=26, el='FIRE', pw=2.2, r=5,
  f='Огонь-Огонь-Лёд: ледяное ядро в огненной оболочке')
K('as_flare', 'Вспышка', 'Flare', 'NOVA', '✸', m=0, cd=18, el='FIRE', pw=1.5, r=7,
  f='Огонь-Огонь-Буря: свет бьёт во все стороны разом')
K('as_hail', 'Град', 'Hail', 'ZONE_AT', '◍', m=0, cd=20, el='FROST', pw=1.2, r=6, d=8, foe='slow 8 2',
  f='Огонь-Лёд-Лёд: осколки сыплются на участок')
K('as_eclipse', 'Затмение', 'Eclipse', 'ZONE_AT', '🌑', m=0, cd=40, el='ARCANE', pw=1.2, r=7, d=10,
  foe='blind 10 0; slow 10 1', f='Огонь-Лёд-Буря: тень накрывает участок, и всё в нём слепнет')
K('as_storm', 'Буря', 'Storm', 'CHAIN', '⚯', m=0, cd=20, el='LIGHTNING', pw=1.3, r=16, n=5,
  f='Огонь-Буря-Буря: разряд скачет с врага на врага')
K('as_blizzard', 'Метель', 'Blizzard', 'ZONE_AT', '❅', m=0, cd=28, el='FROST', pw=1.0, r=7, d=10,
  foe='slow 10 3; blind 10 0', f='Лёд-Лёд-Буря: в метели не видно и не выйти')
K('as_gravity', 'Тяготение', 'Gravity Well', 'VORTEX', '◉', m=0, cd=30, el='ARCANE', pw=0.6, r=8, d=4,
  foe='slow 4 2', f='Лёд-Буря-Буря: звезда стягивает всех к себе')
C('astromancer', 'Астромант', 'Astromancer', 'DAMAGE', 0x5DADE2, 'INT', 'LUCK', 'magic 5 explore 2 night 2', '',
  ('Сочетания', 'spell_power 14 cooldown 6'),
  'Копит три сферы и сплетает их: урон идёт от сочетания, а не от одной кнопки. Самый зрелищный класс.',
  res='orbs',
  invoke=_invoke(['as_orb_fire', 'as_orb_frost', 'as_orb_storm'], 'as_weave', [
      ('000', 'as_supernova'), ('111', 'as_zero'), ('222', 'as_thunder'), ('001', 'as_comet'),
      ('002', 'as_flare'), ('011', 'as_hail'), ('012', 'as_eclipse'), ('022', 'as_storm'),
      ('112', 'as_blizzard'), ('122', 'as_gravity')]))

# =============================== 11. Снайпер ===============================
# Ресурс: заряды (выстрелы). Ждёт окно и бьёт один раз — противоположность спаму.
K('sn_shot', 'Выстрел', 'Shot', 'TARGET_BOLT', '✦', m=0, cd=1.8, st='AGI', pw=0.85, r=34, f='Обычный выстрел на вскидку')
K('sn_roll', 'Перекат', 'Roll', 'DASH', '⇶', m=0, cd=7, f='Уход с линии')
K('sn_knee', 'В колено', 'Kneecap', 'INTERRUPT', '✖', m=20, cd=14, r=18, f='Выстрел в колено сбивает чтение')
K('sn_aim', 'Прицелиться', 'Take Aim', 'BUFF_SELF', '◎', m=20, cd=18, d=8, me='slow 8 2; focus 8 2',
  f='Снайпер замирает: стоя на месте, он бьёт куда больнее')
K('sn_pierce', 'Навылет', 'Through and Through', 'BEAM', '➳', m=20, cd=14, st='AGI', pw=1.5, r=26, fl='pierce',
  f='Болт проходит сквозь строй')
K('sn_mark', 'Метка стрелка', 'Marksman Mark', 'MARK', '◉', m=20, cd=20, r=28, d=10, pw=1.35, f='Отмеченная цель видна отовсюду')
K('sn_trap', 'Капкан', 'Snare', 'MINE', '◍', m=20, cd=18, st='AGI', pw=1.2, r=3, d=30, foe='slow 5 3',
  f='Капкан держит того, кто подойдёт')
K('sn_volley', 'Залп', 'Volley', 'ARROW_RAIN', '⁙', m=20, cd=22, st='AGI', pw=0.8, r=6, n=8, f='Навесной дождь по площади')
K('sn_breath', 'Задержать дыхание', 'Hold Breath', 'BUFF_SELF', '◌', m=20, cd=26, d=6, me='focus 6 3; slow 6 1',
  f='Шесть секунд полной неподвижности ради одного выстрела')
K('sn_headshot', 'Выстрел в голову', 'Headshot', 'CAST_BOLT', '✸', m=40, cd=55, st='AGI', pw=4.0, r=36, d=2.2,
  f='Долгое прицеливание и один тяжёлый выстрел — успеть между механиками')
K('sn_finisher', 'Контрольный', 'Finisher', 'CAST_BOLT', '☠', m=40, cd=110, st='AGI', pw=5.0, r=36, d=2.6, fl='exec',
  f='По раненому — наверняка')
K('sn_scatter', 'Дробь', 'Scatter', 'CONE', '⁂', m=20, cd=12, st='AGI', pw=0.9, r=7, foe='slow 3 1', f='Накоротке — веером')
C('sniper', 'Снайпер', 'Sniper', 'DAMAGE', 0x566573, 'AGI', 'LUCK', 'ranged 5 solo 3 stealth 1',
  'sn_shot sn_roll sn_knee sn_aim sn_pierce sn_mark sn_trap sn_volley sn_breath sn_headshot sn_finisher sn_scatter',
  ('Один выстрел', 'arrow_damage 15 crit_damage 20'),
  'Готовится и бьёт один раз. Прямая противоположность тем, кто жмёт всё подряд.', res='charges')

# =============================== 12. Палач ===============================
# Ресурс: ярость. Редко, медленно и очень больно; добивает раненых.
K('ex_chop', 'Рубящий', 'Chop', 'STRIKE', '🪓', m=0, cd=5, pw=1.5, r=5, combo=1, f='Замах от плеча, без спешки')
K('ex_step', 'Тяжёлый шаг', 'Heavy Step', 'DASH_STRIKE', '⇶', m=0, cd=9, pw=1.3, r=3.4, f='Переступил — и уже рядом')
K('ex_hook', 'Крюком', 'Hook Him', 'INTERRUPT', '✖', m=20, cd=14, r=5, f='Крюк за шею обрывает чтение')
K('ex_cleave', 'Через двоих', 'Through Two', 'CONE', '⚔', m=25, cd=12, pw=1.6, r=6, fl='pierce', f='Замах достаёт сразу нескольких')
K('ex_hang', 'Подвесить', 'String Up', 'STUN', '⛓', m=30, cd=20, pw=1.5, r=5, d=2.5, f='Врага вздёргивает над полом')
K('ex_sentence', 'Приговор', 'Sentence', 'MARK', '◉', m=25, cd=22, r=14, d=10, pw=1.4, f='Названный получает вдвойне')
K('ex_sever', 'Отсечь', 'Sever', 'DOT', '〰', m=25, cd=14, pw=0.6, r=5, d=10, f='Глубокий разрез не закрывается')
K('ex_dread', 'Страх плахи', 'Dread of the Block', 'TAUNT', '☠', m=25, cd=20, r=10, d=8, foe='weak 8 1; slow 8 1',
  f='Враги не могут отвести взгляд от топора')
K('ex_stance', 'Стойка палача', 'Headsman Stance', 'BUFF_SELF', '▣', m=30, cd=28, d=10, me='str 10 2; res 10 1; slow 10 1',
  f='Пока стоит в стойке, его не сбить с замаха')
K('ex_behead', 'Обезглавить', 'Behead', 'STRIKE', '☠', m=45, cd=50, pw=3.2, r=5, fl='exec finish',
  f='Один удар по тому, кто уже ранен')
K('ex_harvest', 'Жатва голов', 'Harvest of Heads', 'MULTI_NOVA', '✺', m=70, cd=115, pw=1.4, r=6, n=4, fl='perfoe',
  f='Четыре круговых замаха: чем больше врагов, тем сильнее каждый')
K('ex_drag', 'Подтащить', 'Drag Over', 'PULL', '⟲', m=20, cd=16, r=12, pw=0.8, foe='slow 3 2', f='Крюк тянет к плахе')
C('executioner', 'Палач', 'Executioner', 'DAMAGE', 0x641E16, 'STR', 'VIT', 'melee 5 risk 3 undead 1',
  'ex_chop ex_step ex_hook ex_cleave ex_hang ex_sentence ex_sever ex_dread ex_stance ex_behead ex_harvest ex_drag',
  ('Тяжёлая рука', 'damage 6 crit_damage 25'),
  'Бьёт редко, медленно и очень больно, а раненых снимает с одного удара. Антипод Стрижа.', res='rage')

# =============================== 13. Пиромант ===============================
# Ресурс: мана. Единственный класс, собранный под волны мобов.
K('py_dart', 'Огненный дротик', 'Fire Dart', 'TARGET_BOLT', '✦', m=8, cd=1.4, el='FIRE', pw=0.8, r=28, f='Короткий язык пламени')
K('py_step', 'Шаг в пламени', 'Flame Step', 'BLINK', '≫', m=10, cd=8, r=12, f='Пиромант исчезает в огне и выходит дальше')
K('py_snuff', 'Затушить', 'Snuff Out', 'INTERRUPT', '✖', m=14, cd=14, r=14, f='Хлопок пламени сбивает чтение')
K('py_burn', 'Горящая земля', 'Burning Ground', 'ZONE_AT', '🔥', m=26, cd=16, el='FIRE', pw=0.8, r=6, d=10,
  f='Пол горит и жжёт всех, кто на нём стоит')
K('py_chain', 'Цепь огня', 'Chain of Fire', 'CHAIN', '⚯', m=24, cd=12, el='FIRE', pw=1.1, r=14, n=5,
  f='Пламя перекидывается с горящего на соседа')
K('py_cone', 'Выдох', 'Exhale', 'CONE', '⌇', m=22, cd=10, el='FIRE', pw=1.2, r=7, f='Струя пламени перед собой')
K('py_blast', 'Взрыв', 'Blast', 'NOVA', '✺', m=28, cd=16, el='FIRE', pw=1.4, r=6, f='Всё вокруг отбрасывает жаром')
K('py_mark', 'Клеймо огня', 'Fire Brand', 'MARK', '◉', m=18, cd=22, r=18, d=10, pw=1.3, f='Горящая метка разносит пламя дальше')
K('py_wall', 'Стена пламени', 'Wall of Flame', 'WALL', '▥', m=24, cd=24, r=6, d=10, wall='minecraft:magma_block',
  f='Поперёк прохода встаёт раскалённый ряд')
K('py_storm', 'Огненный шторм', 'Firestorm', 'ZONE_AT', '✹', m=55, cd=65, el='FIRE', pw=1.6, r=8, d=8,
  f='Пламя падает сверху на весь участок')
K('py_inferno', 'Пекло', 'Inferno', 'GLOBAL', '☄', m=70, cd=115, el='FIRE', pw=1.8, r=16, fl='perfoe',
  f='Горит всё: чем больше врагов, тем сильнее каждый удар')
K('py_cloak', 'Огненный плащ', 'Flame Cloak', 'BUFF_SELF', '▩', m=20, cd=28, d=10, me='fireres 10 0; res 10 0',
  fl='reflect', f='Пламя вокруг пироманта жжёт тех, кто подошёл')
C('pyromancer', 'Пиромант', 'Pyromancer', 'DAMAGE', 0xE67E22, 'INT', 'AGI', 'magic 4 fire 4 party 2',
  'py_dart py_step py_snuff py_burn py_chain py_cone py_blast py_mark py_wall py_storm py_inferno py_cloak',
  ('Разносчик пламени', 'spell_power 12 res_fire 25'),
  'Горящая земля и цепные взрывы. Единственный класс, собранный под волны мобов.', res='mana')
