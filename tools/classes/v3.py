"""
Классы 3.0 (подземелье): 50 классов по 16 навыков — каждый собран из двух прежних, плюс динамичные навыки:
  поддержка — таргетное быстрое лечение, лечение с подготовкой, лечение со временем, цепное лечение;
  маги — частая таргетная атака, мощный удар с подготовкой, проклятие-«дот».
Подключается последним (gen_classes.py): строит новый набор классов, остальные уходят в ALIASES (замена у игроков).

Порядок навыков = порядок открытия (уровни UNLOCK16 в gen_classes.py): сначала основа роли, потом остальное.
"""
from .dsl import K, SKILLS, CLASSES, ORDER, ALIASES

# ============================ динамичные навыки ============================
# поддержка: (ключ, класс-источник, стихия, [быстрое, с подготовкой, со временем, цепное])
HEAL_SETS = {
    'cleric': ('HOLY', [('holy_mend', 'Слово исцеления', 'Healing Word'), ('greater_heal', 'Святое исцеление', 'Holy Heal'),
                        ('renew', 'Обновление', 'Renew'), ('prayer_of_light', 'Молитва света', 'Prayer of Light')]),
    'druid': ('NATURE', [('sprout_mend', 'Живительный росток', 'Living Sprout'), ('bloom', 'Цветение жизни', 'Lifebloom'),
                         ('rejuvenation', 'Омоложение', 'Rejuvenation'), ('wild_growth', 'Буйный рост', 'Wild Growth')]),
    'shaman': ('NATURE', [('spirit_touch', 'Касание духов', 'Spirit Touch'), ('ancestral_heal', 'Исцеление предков', 'Ancestral Heal'),
                          ('riptide', 'Прилив духа', 'Riptide'), ('chain_heal', 'Цепное исцеление', 'Chain Heal')]),
    'bard': ('ARCANE', [('soothing_note', 'Утешающая нота', 'Soothing Note'), ('ballad', 'Целебная баллада', 'Healing Ballad'),
                        ('lingering_tune', 'Протяжный мотив', 'Lingering Tune'), ('chorus', 'Хор', 'Chorus')]),
    'healer': ('HOLY', [('flash_heal', 'Быстрое исцеление', 'Flash Heal'), ('surgery', 'Операция', 'Surgery'),
                        ('bandage', 'Перевязка', 'Bandage'), ('triage_wave', 'Волна помощи', 'Triage Wave')]),
    'oracle': ('ARCANE', [('fate_touch', 'Касание судьбы', 'Touch of Fate'), ('foresight', 'Предвидение', 'Foresight'),
                          ('fated_mend', 'Предначертание', 'Fated Mend'), ('threads', 'Нити судеб', 'Threads of Fate')]),
    'guardian_angel': ('HOLY', [('angel_touch', 'Касание ангела', 'Angelic Touch'), ('divine_grace', 'Божья милость', 'Divine Grace'),
                                ('halo', 'Нимб', 'Halo'), ('wings_of_mercy', 'Крылья милосердия', 'Wings of Mercy')]),
    'sun_priestess': ('HOLY', [('sunbeam', 'Луч солнца', 'Sunbeam'), ('dawn', 'Рассвет', 'Dawn'),
                               ('warm_glow', 'Тёплое сияние', 'Warm Glow'), ('solar_arc', 'Солнечная дуга', 'Solar Arc')]),
    'shadow_priest': ('SHADOW', [('shadow_mend', 'Целение тенью', 'Shadow Mend'), ('dark_mercy', 'Тёмная милость', 'Dark Mercy'),
                                 ('penance', 'Покаяние', 'Penance'), ('dark_chain', 'Цепь сумерек', 'Twilight Chain')]),
    'chronomancer': ('ARCANE', [('rewind', 'Перемотка', 'Rewind'), ('time_loop', 'Петля времени', 'Time Loop'),
                                ('echo_heal', 'Эхо исцеления', 'Echoing Heal'), ('chrono_chain', 'Цепь мгновений', 'Chain of Moments')]),
    'tidecaller': ('FROST', [('healing_stream', 'Целебная струя', 'Healing Stream'), ('tidal_wave', 'Приливная волна', 'Tidal Wave'),
                             ('mist', 'Целебный туман', 'Healing Mist'), ('wave_chain', 'Бегущая волна', 'Running Wave')]),
    'alchemist': ('NATURE', [('tonic', 'Тоник', 'Tonic'), ('elixir', 'Великий эликсир', 'Great Elixir'),
                             ('salve', 'Бальзам', 'Salve'), ('splash_potion', 'Брызги зелья', 'Splash Potion')]),
    'skald': ('ARCANE', [('war_song', 'Песнь стойкости', 'Song of Grit'), ('saga', 'Сага о героях', 'Saga of Heroes'),
                         ('hymn', 'Гимн', 'Hymn'), ('drum_chain', 'Барабанный бой', 'War Drums')]),
    'moon_priestess': ('ARCANE', [('moon_touch', 'Лунное касание', 'Moon Touch'), ('moonfall', 'Лунный свет', 'Moonlight'),
                                  ('starlight', 'Звёздный покров', 'Starlight'), ('moon_chain', 'Лунная дорожка', 'Moon Path')]),
}

# маги: (стихия, [частая атака, с подготовкой, проклятие])
BOLT_SETS = {
    'mage': ('ARCANE', [('arcane_missile', 'Чародейская стрела', 'Arcane Missile'), ('arcane_blast', 'Чародейский взрыв', 'Arcane Blast'),
                        ('mana_burn', 'Выжигание маны', 'Mana Burn')]),
    'pyromancer': ('FIRE', [('fire_dart', 'Огненный дротик', 'Fire Dart'), ('pyroblast', 'Огненная глыба', 'Pyroblast'),
                            ('ignite', 'Возгорание', 'Ignite')]),
    'cryomancer': ('FROST', [('frost_shard', 'Ледяной осколок', 'Frost Shard'), ('glacial_spike', 'Ледяное копьё', 'Glacial Spike'),
                             ('frostbite', 'Обморожение', 'Frostbite')]),
    'stormcaller': ('LIGHTNING', [('spark', 'Искра', 'Spark'), ('thunderbolt', 'Удар грома', 'Thunderbolt'),
                                  ('static', 'Статика', 'Static Charge')]),
    'necromancer': ('SHADOW', [('shadow_bolt', 'Стрела тьмы', 'Shadow Bolt'), ('soul_rend', 'Разрыв души', 'Soul Rend'),
                               ('decay', 'Тление', 'Decay')]),
    'warlock': ('SHADOW', [('fel_bolt', 'Скверна', 'Fel Bolt'), ('chaos_bolt', 'Стрела хаоса', 'Chaos Bolt'),
                           ('corruption', 'Порча', 'Corruption')]),
    'astromancer': ('ARCANE', [('starfire', 'Звёздный огонь', 'Starfire'), ('starsurge', 'Звездопад', 'Starsurge'),
                               ('moonfire', 'Лунный огонь', 'Moonfire')]),
    'venomancer': ('POISON', [('venom_spit', 'Ядовитый плевок', 'Venom Spit'), ('toxic_burst', 'Токсичный взрыв', 'Toxic Burst'),
                              ('plague', 'Мор', 'Plague')]),
}

for src, (el, names) in HEAL_SETS.items():
    (k1, r1, e1), (k2, r2, e2), (k3, r3, e3), (k4, r4, e4) = names
    k1, k2, k3, k4 = 'd_' + k1, 'd_' + k2, 'd_' + k3, 'd_' + k4
    K(k1, r1, e1, 'TARGET_HEAL', '✚', m=12, cd=1.5, el=el, pw=1.0, r=24, f='Быстро, точно, по выбранному союзнику')
    K(k2, r2, e2, 'CAST_HEAL', '✙', m=28, cd=2, el=el, pw=2.6, r=24, d=1.6, f='Долгое заклинание — стойте на месте')
    K(k3, r3, e3, 'HOT', '❀', m=16, cd=5, el=el, pw=1.6, r=24, d=12, f='Лечит понемногу, но долго')
    K(k4, r4, e4, 'CHAIN_HEAL', '☘', m=30, cd=7, el=el, pw=1.2, r=24, n=4, f='Перескакивает на раненых рядом')

for src, (el, names) in BOLT_SETS.items():
    (k1, r1, e1), (k2, r2, e2), (k3, r3, e3) = names
    k1, k2, k3 = 'd_' + k1, 'd_' + k2, 'd_' + k3
    K(k1, r1, e1, 'TARGET_BOLT', '✦', m=6, cd=1.0, el=el, pw=0.85, r=32, f='Снаряд сам летит в цель под прицелом')
    K(k2, r2, e2, 'CAST_BOLT', '✸', m=22, cd=6, el=el, pw=3.2, r=32, d=1.6, f='Долгое заклинание — стойте на месте')
    K(k3, r3, e3, 'DOT', '☠', m=14, cd=4, el=el, pw=0.45, r=32, d=12, f='Проклятие точит цель каждую секунду')

# базовые навыки новичка
K('mend', 'Лечение', 'Mend', 'TARGET_HEAL', '✚', m=14, cd=3, el='HOLY', pw=0.8, r=20,
  f='Простейшее лечение союзника под прицелом (или выбранного Ctrl+1…5; иначе — себя)')
K('magic_missile', 'Магическая стрела', 'Magic Missile', 'TARGET_BOLT', '✦', m=5, cd=1.2, el='ARCANE', pw=0.7, r=28,
  f='Простейшая стрела маны — летит в цель под прицелом')

# ============================ состав классов ============================
# новый класс = первый источник (его имя, черта, цвет) + второй; desc — общее описание
MERGE = {
    # ---- танки ----
    'warrior': ('chieftain', 'TANK', 'Воин-вождь: держит строй, ревёт кличем и сам ведёт отряд в бой'),
    'guardian': ('royal_guard', 'TANK', 'Щит и стена: прикрывает союзников, ставит преграды, хранит жизни'),
    'paladin': ('templar', 'TANK', 'Святой рыцарь: держит удар, лечит себя и жжёт нежить светом'),
    'death_knight': ('dread_guardian', 'TANK', 'Рыцарь смерти: притягивает врагов цепями, пьёт их жизнь и сеет ужас'),
    'ironclad': ('bastion', 'TANK', 'Латник-бастион: тяжёлые латы, стены и огневые точки'),
    'stone_warden': ('crystal_warden', 'TANK', 'Каменный страж: кристальные щиты, земля под ногами врага'),
    'ursine_guardian': ('rhino', 'TANK', 'Медвежий страж: звериная ярость, таран и непробиваемая шкура'),
    'treant': ('myconid', 'TANK', 'Древень: корни, споры и кора, что крепнет от ударов'),
    'frost_giant': ('tidewarden', 'TANK', 'Ледяной великан: стужа и прилив, замедление и водовороты'),
    'blood_knight': ('undying', 'TANK', 'Кровавый рыцарь: живёт за счёт чужой крови и не умирает с первого раза'),
    'dragon_knight': ('flame_warden', 'TANK', 'Драконий рыцарь: огненное дыхание и чешуя дракона'),
    'graviton': ('aegis_knight', 'TANK', 'Гравитон: стягивает врагов и держит небесный щит над отрядом'),
    # ---- бойцы: ближний бой ----
    'berserker': ('troll_berserker', 'DAMAGE', 'Берсерк: чем больнее — тем злее'),
    'blademaster': ('blade_dancer', 'DAMAGE', 'Мастер клинка: вихрь стали и танец между врагами'),
    'monk': ('staff_master', 'DAMAGE', 'Монах: шест, кулаки и внутренняя сила'),
    'assassin': ('rogue', 'DAMAGE', 'Ассасин: тень, яд и удар в спину'),
    'shadowblade': ('phantom_assassin', 'DAMAGE', 'Теневой клинок: исчезает и появляется у цели с ножом'),
    'spellblade': ('ember_spirit', 'DAMAGE', 'Боевой маг: огненные клинки и рывки сквозь пламя'),
    'executioner': ('reaper', 'DAMAGE', 'Палач: добивает раненых, косит толпы'),
    'demon_hunter': ('vengeful_specter', 'DAMAGE', 'Охотник на демонов: призрачные клинки и метки охоты'),
    # ---- бойцы: стрелки ----
    'ranger': ('falconer', 'DAMAGE', 'Следопыт: лук, сокол и ловушки'),
    'sniper': ('railgunner', 'DAMAGE', 'Снайпер: один выстрел — одна цель'),
    'frost_archer': ('moon_archer', 'DAMAGE', 'Ледяная лучница: стрелы стужи и лунного света'),
    'wind_ranger': ('aeronaut', 'DAMAGE', 'Повелительница ветра: стрелы на ветру и полёт над полем'),
    'gunslinger': ('mercenary', 'DAMAGE', 'Ганфайтер: очереди и быстрые выстрелы'),
    'bomber': ('rocketeer', 'DAMAGE', 'Подрывник: мины, ракеты и взрывы'),
    'beastmaster': ('trapper', 'DAMAGE', 'Повелитель зверей: звери-помощники и капканы'),
    'chronorunner': ('echo', 'DAMAGE', 'Хронобегунья: скорость, откат времени и эхо выстрелов'),
    # ---- бойцы: маги ----
    'mage': ('skywrath', 'DAMAGE', 'Маг: чистая чародейская сила'),
    'pyromancer': ('dragonfire_sorceress', 'DAMAGE', 'Пиромант: огонь и драконье пламя'),
    'cryomancer': ('water_elemental', 'DAMAGE', 'Криомант: лёд и вода'),
    'stormcaller': ('storm_spirit', 'DAMAGE', 'Повелитель бурь: молнии и грозовые рывки'),
    'necromancer': ('death_prophet', 'DAMAGE', 'Некромант: тьма, души и мёртвые слуги'),
    'warlock': ('demonologist', 'DAMAGE', 'Чернокнижник: порча, демоны и сделки'),
    'astromancer': ('gravimancer', 'DAMAGE', 'Астромант: звёзды и тяготение'),
    'venomancer': ('poisoner', 'DAMAGE', 'Ядовитый маг: яды, мор и змеи'),
    # ---- поддержка ----
    'cleric': ('saint', 'SUPPORT', 'Жрец: свет исцеляет, оберегает и поднимает павших'),
    'druid': ('lifeweaver', 'SUPPORT', 'Друид: жизнь природы, цветы и корни'),
    'shaman': ('totem_carver', 'SUPPORT', 'Шаман: духи, тотемы и цепное исцеление'),
    'bard': ('street_musician', 'SUPPORT', 'Бард: песни, что лечат и вдохновляют'),
    'healer': ('field_medic', 'SUPPORT', 'Целитель: быстрое лечение и полевая хирургия'),
    'oracle': ('seer', 'SUPPORT', 'Оракул: видит удар до того, как он случится'),
    'guardian_angel': ('wisp', 'SUPPORT', 'Ангел-хранитель: щиты, связи и прыжки к союзникам'),
    'sun_priestess': ('lightbearer', 'SUPPORT', 'Жрица Солнца: тёплый свет лечит своих и жжёт врагов'),
    'shadow_priest': ('dark_patron', 'SUPPORT', 'Жрец теней: лечит болью и щитами тьмы'),
    'chronomancer': ('clockmaker', 'SUPPORT', 'Хрономант: перематывает раны назад'),
    'tidecaller': ('snow_maiden', 'SUPPORT', 'Целитель вод: приливы, туманы и ледяные щиты'),
    'alchemist': ('apothecary', 'SUPPORT', 'Алхимик: зелья, бальзамы и брызги лекарства'),
    'skald': ('bannerman', 'SUPPORT', 'Скальд: песни и знамёна поднимают дух отряда'),
    'moon_priestess': ('kitsune_priestess', 'SUPPORT', 'Жрица Луны: лунный свет и лисьи чары'),
}

# что выкидывать первым, если навыков больше 16 (наименее полезное в подземелье)
LOW_VALUE = ['CHARM', 'DOMINATE', 'STEAL', 'GRAPPLE', 'REVEAL', 'GATHER', 'SWAP', 'RECALL', 'RALLY', 'RESET', 'MEDITATE',
             'STEALTH', 'SACRIFICE', 'MINE', 'WALL', 'BUFF_SELF', 'PURIFY', 'BLINK', 'DASH']
# сколько навыков одного вида — не больше
KIND_CAP = {'BUFF_SELF': 2, 'BUFF_ALLIES': 3, 'STRIKE': 3, 'NOVA': 2, 'ZONE': 2, 'ZONE_AT': 2, 'SUMMON': 2, 'TAUNT': 2,
            'ARROWS': 3, 'BOLT': 2, 'HEAL_TARGET': 2, 'HEAL': 2, 'TETHER': 1, 'REZ': 1, 'INTERRUPT': 1, 'SHIELD': 1, 'WARD': 1}


def _score(key):
    k = SKILLS[key].kind
    return LOW_VALUE.index(k) if k in LOW_VALUE else 100


def build():
    old = dict(CLASSES)
    new_order = []
    for main, (second, role, desc) in MERGE.items():
        a, b = old[main], old[second]
        skills = []
        # динамичные — в начало (основа роли)
        if main in HEAL_SETS:
            skills += ['d_' + n[0] for n in HEAL_SETS[main][1]]
        if main in BOLT_SETS:
            skills += ['d_' + n[0] for n in BOLT_SETS[main][1]]
        # чередуем навыки двух источников, без повторов
        for i in range(max(len(a.skills), len(b.skills))):
            for c in (a, b):
                if i < len(c.skills) and c.skills[i] not in skills and not c.invoke:
                    skills.append(c.skills[i])
        # не больше KIND_CAP одного вида (лишние — с конца)
        count = {}
        kept, capped = [], []
        for s in skills:
            k = SKILLS[s].kind
            count[k] = count.get(k, 0) + 1
            (kept if count[k] <= KIND_CAP.get(k, 4) else capped).append(s)
        skills = kept
        # обязательное: прерывание, воскрешение у поддержки
        if role == 'SUPPORT' and not any(SKILLS[s].kind == 'REZ' for s in skills):
            skills.append('resurrection')
        if role != 'SUPPORT' and not any(SKILLS[s].kind == 'INTERRUPT' for s in skills):
            skills.append({'TANK': 'pummel', 'DAMAGE': 'kick'}[role])
        # лишнее — убрать наименее полезное (кроме первых динамичных)
        protected = set(skills[:4]) if main in HEAL_SETS or main in BOLT_SETS else set()
        while len(skills) > 16:
            cand = [s for s in skills if s not in protected]
            worst = min(cand, key=lambda s: (_score(s), -skills.index(s)))
            skills.remove(worst)
        # не хватает — добрать из запаса роли
        reserve = capped + {'TANK': ['battle_shout', 'rallying_cry', 'expose_armor', 'pummel'],
                            'DAMAGE': ['expose_armor', 'battle_shout', 'weakpoint_shot', 'bloodlust', 'hunters_call', 'curse_of_frailty', 'rallying_cry', 'arcane_focus'],
                            'SUPPORT': ['arcane_focus', 'spirit_link_totem', 'time_shelter', 'aura_mastery']}[role]
        for r in reserve:
            if len(skills) >= 16:
                break
            if r not in skills:
                skills.append(r)
        a.skills = skills
        a.role = role
        a.desc = desc
        a.hidden = None
        a.hint = ''
        a.invoke = None
        # стиль — сумма стилей источников (для подбора на 10 уровне)
        for st, v in b.styles.items():
            a.styles[st] = a.styles.get(st, 0) + v
        new_order.append(main)
    # все прочие классы — замена у игроков
    for k in list(ORDER):
        if k in MERGE:
            continue
        target = next((m for m, (s, _, _) in MERGE.items() if s == k), None)
        if target is None:
            role = old[k].role
            target = {'TANK': 'warrior', 'DAMAGE': 'blademaster', 'SUPPORT': 'cleric'}[role]
        ALIASES[k] = target
        del CLASSES[k]
    for k, v in list(ALIASES.items()):
        while v in ALIASES:
            v = ALIASES[v]
        ALIASES[k] = v
    ORDER[:] = new_order


build()
