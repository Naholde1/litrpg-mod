"""Семь саппортов версии 4.0. Воскрешение есть у всех, но у этих — втрое быстрее."""
from .dsl import K, C

# =============================== 1. Лекарь ===============================
# Ресурс: мана. Эталон, по которому балансируются остальные.
K('hl_mend', 'Быстрое исцеление', 'Flash Heal', 'TARGET_HEAL', '✚', m=14, cd=1.6, el='HOLY', pw=1.0, r=24,
  f='Быстро, точно, по ближайшему к прицелу')
K('hl_surgery', 'Операция', 'Surgery', 'CAST_HEAL', '✙', m=30, cd=3, el='HOLY', pw=2.8, r=24, d=1.8,
  f='Долгое заклинание — стойте на месте')
K('hl_bandage', 'Тугая повязка', 'Tight Bandage', 'HOT', '❀', m=18, cd=6, el='HOLY', pw=1.7, r=24, d=12,
  f='Лечит понемногу, но долго')
K('hl_triage', 'Волна помощи', 'Triage Wave', 'CHAIN_HEAL', '☘', m=32, cd=9, el='HOLY', pw=1.3, r=24, n=4,
  f='Перескакивает на самых раненых рядом')
K('hl_rez', 'Поднять павшего', 'Raise the Fallen', 'REZ', '♰', m=40, cd=30, el='HOLY', r=10, d=2,
  f='Лекарь поднимает товарища втрое быстрее обычного')
K('hl_purify', 'Очищение', 'Cleanse', 'PURIFY', '✧', m=16, cd=12, el='HOLY', r=20, f='Снимает яды, проклятия и горение')
K('hl_shield', 'Оберег', 'Aegis', 'SHIELD', '◈', m=24, cd=14, el='HOLY', r=20, pw=1.6, d=8, f='Щит на раненого союзника')
K('hl_ward', 'Печать жизни', 'Seal of Life', 'WARD', '♁', m=40, cd=90, el='HOLY', r=20, d=10, pw=1.0,
  f='Смертельный удар оставляет союзника на одном здоровье')
K('hl_prayer', 'Общая молитва', 'Common Prayer', 'HEAL', '۞', m=45, cd=26, el='HOLY', pw=1.8, r=12,
  f='Лечит всех вокруг разом')
K('hl_sanctuary', 'Святилище', 'Sanctuary', 'ZONE', '⛪', m=55, cd=70, el='HOLY', pw=-0.3, r=7, d=10, me='regen 10 1',
  f='Круг, в котором и лечат лучше, и бьют слабее')
K('hl_miracle', 'Чудо', 'Miracle', 'HEAL', '✹', m=80, cd=120, el='HOLY', pw=4.0, r=16, f='Весь отряд поднимается разом')
K('hl_smite', 'Кара', 'Smite', 'TARGET_BOLT', '✦', m=10, cd=2.0, el='HOLY', pw=0.7, r=26, f='Не только лечит — иногда и бьёт')
C('healer', 'Лекарь', 'Healer', 'SUPPORT', 0xF7F7F7, 'INT', 'VIT', 'support 5 party 3 magic 1',
  'hl_mend hl_surgery hl_bandage hl_triage hl_rez hl_purify hl_shield hl_ward hl_prayer hl_sanctuary hl_miracle hl_smite',
  ('Твёрдая рука', 'spell_power 10 mana_regen 15'),
  'Чистое лечение без хитростей. По нему меряются все остальные.', res='mana')

# =============================== 2. Хронолог ===============================
# Ресурс: заряды (песчинки). Отменяет ошибку, а не лечит её последствия.
K('ch_mend', 'Сшить мгновение', 'Stitch a Moment', 'TARGET_HEAL', '✚', m=0, cd=1.8, el='ARCANE', pw=0.95, r=24,
  f='Рана отматывается на секунду назад')
K('ch_loop', 'Петля времени', 'Time Loop', 'CAST_HEAL', '✙', m=20, cd=3, el='ARCANE', pw=2.6, r=24, d=1.8,
  f='Долгая петля возвращает союзника к себе здоровому')
K('ch_echo', 'Эхо', 'Echo', 'HOT', '❀', m=0, cd=6, el='ARCANE', pw=1.6, r=24, d=12, f='Лечение повторяется само ещё долго')
K('ch_rewind', 'Обратный ход', 'Rewind', 'RECALL', '⟲', m=20, cd=40, r=24, d=3, fl='group',
  f='Союзник возвращается туда, где был три секунды назад, и с тем здоровьем')
K('ch_rez', 'Отмотать смерть', 'Undo the Death', 'REZ', '♰', m=20, cd=30, el='ARCANE', r=10, d=2,
  f='Павшего возвращают в миг до удара')
K('ch_haste', 'Ускорить', 'Hasten', 'BUFF_ALLIES', '⏩', m=20, cd=30, r=14, d=12, me='haste 12 1; spd 12 1',
  f='Для отряда время идёт быстрее')
K('ch_slow', 'Замедлить', 'Slow Down', 'ZONE_AT', '⏪', m=20, cd=24, el='ARCANE', pw=0.3, r=6, d=10,
  foe='slow 10 3', f='Для врагов внутри время густеет')
K('ch_stop', 'Остановка', 'Time Stop', 'STASIS', '⌛', m=40, cd=70, r=5, d=4, f='В круге время стоит')
K('ch_shield', 'Щит мгновения', 'Shield of the Moment', 'SHIELD', '◈', m=0, cd=16, el='ARCANE', r=20, pw=1.5, d=8,
  f='Удар приходится в прошлое союзника')
K('ch_reset', 'Сбросить откаты', 'Reset', 'RESET', '↻', m=20, cd=100, r=14, f='Отряду возвращают потраченные навыки')
K('ch_rollback', 'Полный откат', 'Full Rollback', 'HEAL', '✹', m=40, cd=120, el='ARCANE', pw=3.6, r=16,
  f='Последние секунды боя отменяются для всего отряда')
K('ch_still', 'Остановить слово', 'Still the Word', 'INTERRUPT', '✖', m=0, cd=14, r=14, f='Чтение откатывается к началу')
C('chronologist', 'Хронолог', 'Chronologist', 'SUPPORT', 0xD2B4DE, 'INT', 'LUCK', 'support 4 magic 3 party 2',
  'ch_mend ch_loop ch_echo ch_rewind ch_rez ch_haste ch_slow ch_stop ch_shield ch_reset ch_rollback ch_still',
  ('Песок в ладони', 'cooldown 10 spell_power 8'),
  'Единственный, кто отменяет чужую ошибку, а не лечит её последствия.', res='charges')

# =============================== 3. Скальд ===============================
# Ресурс: шкала песни. Песни работают, пока он их держит и его не прервали.
K('sk_note', 'Утешающая нота', 'Soothing Note', 'TARGET_HEAL', '✚', m=10, cd=1.8, el='ARCANE', pw=0.95, r=24,
  f='Одна нота — и рана затягивается')
K('sk_ballad', 'Целебная баллада', 'Healing Ballad', 'CAST_HEAL', '✙', m=22, cd=3, el='ARCANE', pw=2.6, r=24, d=1.8,
  f='Длинная баллада — скальд стоит и поёт')
K('sk_tune', 'Протяжный мотив', 'Lingering Tune', 'HOT', '❀', m=14, cd=6, el='ARCANE', pw=1.6, r=24, d=12,
  f='Мотив держится и лечит сам собой')
K('sk_rez', 'Песнь возвращения', 'Song of Return', 'REZ', '♰', m=25, cd=30, el='ARCANE', r=10, d=2,
  f='Павший встаёт на припеве')
K('sk_song_might', 'Песнь силы', 'Song of Might', 'BUFF_ALLIES', '⚔', m=18, cd=24, r=16, d=16, me='might 16 2; str 16 1',
  f='Пока звучит — отряд бьёт сильнее')
K('sk_song_guard', 'Песнь стойкости', 'Song of Grit', 'BUFF_ALLIES', '⛨', m=18, cd=24, r=16, d=16, me='bulwark 16 2; res 16 0',
  f='Пока звучит — отряд держит больше')
K('sk_song_speed', 'Песнь ветра', 'Song of Wind', 'BUFF_ALLIES', '⇶', m=18, cd=24, r=16, d=16, me='spd 16 1; haste 16 1',
  f='Пока звучит — отряд быстрее')
K('sk_discord', 'Какофония', 'Discord', 'ZONE_AT', '♫', m=20, cd=22, el='ARCANE', pw=0.6, r=6, d=10,
  foe='nausea 10 0; slow 10 1; vuln 10 1', f='Фальшивая нота сбивает врагов с толку')
K('sk_silence', 'Оборвать струну', 'Cut the String', 'INTERRUPT', '✖', m=14, cd=14, r=14, f='Резкий аккорд обрывает чужое слово')
K('sk_saga', 'Сага о героях', 'Saga of Heroes', 'BUFF_ALLIES', '📖', m=40, cd=90, r=18, d=20,
  me='might 20 3; bulwark 20 2; regen 20 1', f='Долгая сага: все три песни сразу')
K('sk_finale', 'Финал', 'Finale', 'HEAL', '✹', m=40, cd=120, el='ARCANE', pw=3.4, r=16,
  f='Последний аккорд поднимает весь отряд')
K('sk_banner', 'Знамя', 'Banner', 'ZONE', '⚑', m=22, cd=40, el='ARCANE', pw=-0.2, r=7, d=14, me='regen 14 0',
  f='Знамя воткнуто в пол: рядом с ним легче')
C('skald', 'Скальд', 'Skald', 'SUPPORT', 0xE8DAEF, 'INT', 'VIT', 'support 4 party 4 explore 1',
  'sk_note sk_ballad sk_tune sk_rez sk_song_might sk_song_guard sk_song_speed sk_discord sk_silence sk_saga sk_finale sk_banner',
  ('Голос, что держит строй', 'spell_power 8 health 4'),
  'Три песни, которые работают, пока он их держит. Усиления требуют внимания весь бой, а не одной кнопки.',
  res='form')

# =============================== 4. Источник ===============================
# Ресурс: мана. Кормит отряд ресурсом — после нерфа маны незаменим.
K('sr_mend', 'Струя жизни', 'Lifestream', 'TARGET_HEAL', '✚', m=14, cd=1.8, el='NATURE', pw=0.95, r=24,
  f='Поток силы перетекает в раненого')
K('sr_flow', 'Полный поток', 'Full Flow', 'CAST_HEAL', '✙', m=30, cd=3, el='NATURE', pw=2.6, r=24, d=1.8,
  f='Долгое вливание силы')
K('sr_drip', 'Капель', 'Dripping Spring', 'HOT', '❀', m=18, cd=6, el='NATURE', pw=1.6, r=24, d=12, f='Течёт понемногу, но долго')
K('sr_rez', 'Вернуть в поток', 'Back into the Flow', 'REZ', '♰', m=40, cd=30, el='NATURE', r=10, d=2,
  f='Павшего возвращает течение')
K('sr_share', 'Поделиться', 'Share', 'MEDITATE', '◉', m=0, cd=26, pw=0.35, r=16, fl='group',
  f='Источник отдаёт часть своего запаса всему отряду')
K('sr_well', 'Колодец силы', 'Well of Power', 'ZONE', '۞', m=35, cd=45, el='NATURE', pw=-0.2, r=7, d=14, me='regen 14 1',
  f='Кто стоит в круге — восстанавливается быстрее')
K('sr_reset', 'Второе дыхание', 'Second Wind', 'RESET', '↻', m=55, cd=110, r=14, f='Откаты отряда обнуляются')
K('sr_purify', 'Промыть', 'Wash Clean', 'PURIFY', '✧', m=16, cd=12, el='NATURE', r=20, f='Поток смывает яд и проклятие')
K('sr_shield', 'Водяной щит', 'Water Shield', 'SHIELD', '◈', m=24, cd=14, el='FROST', r=20, pw=1.5, d=8,
  f='Плёнка воды гасит удар')
K('sr_drain', 'Отвести силу', 'Divert', 'DOT', '〰', m=18, cd=14, el='ARCANE', pw=0.5, r=20, d=10,
  f='Из врага вытекает его собственная сила')
K('sr_surge', 'Разлив', 'Surge', 'HEAL', '✹', m=45, cd=28, el='NATURE', pw=1.9, r=12, f='Волна силы проходит по всем')
K('sr_tide', 'Половодье', 'High Water', 'BUFF_ALLIES', '🌊', m=70, cd=120, r=18, d=18,
  me='regen 18 1; haste 18 1; focus 18 2', f='Отряд перестаёт считать ресурс на восемнадцать секунд')
C('wellspring', 'Источник', 'Wellspring', 'SUPPORT', 0x76D7C4, 'INT', 'VIT', 'support 4 nature 3 party 2',
  'sr_mend sr_flow sr_drip sr_rez sr_share sr_well sr_reset sr_purify sr_shield sr_drain sr_surge sr_tide',
  ('Полный сосуд', 'mana 25 mana_regen 20'),
  'Кормит отряд ресурсом и снимает откаты. Чем строже цены, тем он нужнее.', res='mana')

# =============================== 5. Теневой жрец ===============================
# Ресурс: здоровье. Лечит, нанося урон, — отойти и лечить издалека не выйдет.
K('dp_mend', 'Целение тенью', 'Shadow Mend', 'TARGET_HEAL', '✚', m=0, cd=1.8, el='SHADOW', pw=0.95, r=22,
  f='Чужая боль закрывает чужую рану')
K('dp_mercy', 'Тёмная милость', 'Dark Mercy', 'CAST_HEAL', '✙', m=20, cd=3, el='SHADOW', pw=2.6, r=22, d=1.8,
  f='Долгая молитва не тому богу')
K('dp_penance', 'Покаяние', 'Penance', 'HOT', '❀', m=0, cd=6, el='SHADOW', pw=1.6, r=22, d=12, f='Исцеление в рассрочку')
K('dp_rez', 'Вернуть из тьмы', 'Back from the Dark', 'REZ', '♰', m=25, cd=30, el='SHADOW', r=10, d=2,
  f='Павшего вытаскивают оттуда, куда он уже шагнул')
K('dp_lash', 'Плеть тьмы', 'Shadow Lash', 'TARGET_BOLT', '✦', m=0, cd=1.6, el='SHADOW', pw=0.9, r=26, fl='steal',
  f='Удар по врагу лечит того, кто ранен сильнее всех')
K('dp_chain', 'Цепь сумерек', 'Twilight Chain', 'CHAIN_HEAL', '☘', m=25, cd=9, el='SHADOW', pw=1.3, r=22, n=4,
  f='Лечение перескакивает по раненым')
K('dp_pact', 'Сделка', 'The Bargain', 'SACRIFICE', '⇅', m=0, cd=24, pw=0.3, f='Жрец отдаёт своё здоровье за чужое')
K('dp_veil', 'Покров тьмы', 'Veil', 'SHIELD', '◈', m=20, cd=14, el='SHADOW', r=20, pw=1.6, d=8, f='Тень закрывает союзника')
K('dp_curse', 'Проклятие слабости', 'Curse of Frailty', 'DOT', '〰', m=0, cd=14, el='SHADOW', pw=0.55, r=22, d=12,
  fl='steal', f='Чем дольше горит проклятие, тем больше утекает в отряд')
K('dp_zone', 'Сумерки', 'Twilight', 'ZONE', '◍', m=25, cd=45, el='SHADOW', pw=0.5, r=7, d=10, me='regen 10 1',
  f='В сумерках свои лечатся, чужие гниют')
K('dp_atone', 'Искупление', 'Atonement', 'HEAL', '✹', m=35, cd=110, el='SHADOW', pw=3.4, r=16,
  f='Вся накопленная боль разом уходит в исцеление')
K('dp_silence', 'Слово тьмы', 'Word of Dark', 'INTERRUPT', '✖', m=0, cd=14, r=14, f='Тьма затыкает чужое заклинание')
C('darkpriest', 'Теневой жрец', 'Dark Priest', 'SUPPORT', 0x6C3483, 'INT', 'VIT', 'support 4 magic 3 risk 2',
  'dp_mend dp_mercy dp_penance dp_rez dp_lash dp_chain dp_pact dp_veil dp_curse dp_zone dp_atone dp_silence',
  ('Боль в обмен', 'lifesteal 10 spell_power 8'),
  'Лечит через урон и платит собственной жизнью. Единственный саппорт, который обязан стоять в бою.',
  res='health')

# =============================== 6. Оракул ===============================
# Ресурс: заряды (видения). Даёт отряду знание, а не цифры.
K('or_touch', 'Касание судьбы', 'Touch of Fate', 'TARGET_HEAL', '✚', m=0, cd=1.8, el='ARCANE', pw=0.95, r=24,
  f='Рана закрывается раньше, чем появилась')
K('or_foresight', 'Взгляд вперёд', 'Glimpse Ahead', 'CAST_HEAL', '✙', m=20, cd=3, el='ARCANE', pw=2.6, r=24, d=1.8,
  f='Оракул смотрит, чем кончится, и правит')
K('or_fated', 'Предначертание', 'Fated Mend', 'HOT', '❀', m=0, cd=6, el='ARCANE', pw=1.6, r=24, d=12,
  f='Исцеление, которое уже случилось')
K('or_rez', 'Другая судьба', 'Another Fate', 'REZ', '♰', m=20, cd=30, el='ARCANE', r=10, d=2,
  f='Павшему достаётся исход получше')
K('or_warn', 'Предупреждение', 'Warning', 'REVEAL', '◎', m=0, cd=16, r=24, d=12,
  f='Отряд видит, кого хранитель выбрал и что сейчас прочтёт')
K('or_shield', 'Щит предвидения', 'Foreseen Shield', 'SHIELD', '◈', m=0, cd=14, el='ARCANE', r=22, pw=1.7, d=8,
  f='Щит ложится на того, по кому сейчас ударят')
K('or_ward', 'Иной исход', 'Other Outcome', 'WARD', '♁', m=20, cd=90, el='ARCANE', r=22, d=10, pw=1.0,
  f='Смертельный удар оказывается несмертельным')
K('or_mark', 'Дурной знак', 'Ill Omen', 'MARK', '◉', m=0, cd=22, r=20, d=10, pw=1.3, f='Помеченному не везёт ни в чём')
K('or_still', 'Этого не будет', 'It Will Not Be', 'INTERRUPT', '✖', m=0, cd=14, r=16, f='Оракул отменяет ещё не случившееся')
K('or_threads', 'Нити судеб', 'Threads of Fate', 'CHAIN_HEAL', '☘', m=20, cd=9, el='ARCANE', pw=1.3, r=24, n=4,
  f='Исцеление идёт по нитям к самым раненым')
K('or_vision', 'Полное видение', 'Full Vision', 'BUFF_ALLIES', '👁', m=40, cd=100, r=18, d=18,
  me='focus 18 3; luck 18 1; nightv 18 0', f='Отряд на восемнадцать секунд знает всё наперёд')
K('or_fate', 'Переписать', 'Rewrite', 'HEAL', '✹', m=40, cd=120, el='ARCANE', pw=3.4, r=16,
  f='Оракул переписывает последние секунды для всех')
C('oracle', 'Оракул', 'Oracle', 'SUPPORT', 0xF9E79F, 'INT', 'LUCK', 'support 4 magic 2 party 3',
  'or_touch or_foresight or_fated or_rez or_warn or_shield or_ward or_mark or_still or_threads or_vision or_fate',
  ('Видящий', 'dodge 8 spell_power 8'),
  'Даёт отряду знание: показывает каст хранителя до того, как он начался, и закрывает выбранную цель.',
  res='charges')

# =============================== 7. Алхимик ===============================
# Ресурс: заряды (фляги). Сила в подготовке до боя, и он один завязан на экономику.
K('al_tonic', 'Тоник', 'Tonic', 'TARGET_HEAL', '✚', m=0, cd=1.8, el='NATURE', pw=0.95, r=22, f='Фляга в руки раненому')
K('al_elixir', 'Великий эликсир', 'Great Elixir', 'CAST_HEAL', '✙', m=20, cd=3, el='NATURE', pw=2.7, r=22, d=1.8,
  f='Готовится на месте — стоять и не дёргаться')
K('al_salve', 'Бальзам', 'Salve', 'HOT', '❀', m=0, cd=6, el='NATURE', pw=1.6, r=22, d=12, f='Мазь работает долго')
K('al_rez', 'Нашатырь', 'Smelling Salts', 'REZ', '♰', m=20, cd=30, el='NATURE', r=8, d=2, f='Павшего приводят в чувство резко')
K('al_splash', 'Брызги зелья', 'Splash Potion', 'CHAIN_HEAL', '☘', m=20, cd=9, el='NATURE', pw=1.3, r=22, n=4,
  f='Разбитая склянка лечит всех рядом')
K('al_acid', 'Кислота', 'Acid Flask', 'ZONE_AT', '☣', m=20, cd=16, el='POISON', pw=1.0, r=5, d=8,
  foe='weak 8 1', f='Лужа кислоты разъедает броню')
K('al_smoke', 'Дымовая склянка', 'Smoke Flask', 'ZONE_AT', '◍', m=0, cd=22, pw=0.2, r=5, d=8, foe='blind 8 0',
  f='Дым закрывает отряд от глаз')
K('al_buff', 'Боевой настой', 'Combat Draught', 'BUFF_ALLIES', '⚗', m=20, cd=30, r=14, d=14,
  me='str 14 1; res 14 0; haste 14 1', f='Фляга на всех перед дракой')
K('al_antidote', 'Противоядие', 'Antidote', 'PURIFY', '✧', m=0, cd=12, el='NATURE', r=20, f='Снимает яд и проклятие разом')
K('al_bomb', 'Зажигательная', 'Firebomb', 'ZONE_AT', '🔥', m=20, cd=20, el='FIRE', pw=1.4, r=5, d=6,
  f='Склянка разбивается и поджигает участок')
K('al_panacea', 'Панацея', 'Panacea', 'HEAL', '✹', m=40, cd=115, el='NATURE', pw=3.4, r=16,
  f='Последняя склянка из запаса поднимает весь отряд')
K('al_cork', 'Пробкой в зубы', 'Cork It', 'INTERRUPT', '✖', m=0, cd=14, r=14, f='Склянка в лицо обрывает чтение')
C('alchemist', 'Алхимик', 'Alchemist', 'SUPPORT', 0xA9DFBF, 'INT', 'AGI', 'support 4 craft 4 nature 1',
  'al_tonic al_elixir al_salve al_rez al_splash al_acid al_smoke al_buff al_antidote al_bomb al_panacea al_cork',
  ('Полная сумка', 'spell_power 8 loot 10'),
  'Сила в том, что наварено заранее: фляги лечат, жгут и слепят. Единственный, кто живёт экономикой.',
  res='charges')
