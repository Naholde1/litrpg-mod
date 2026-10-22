"""
Переработка классов под PvE (2.17): у каждого класса — настоящая польза в бою с хранителем.
Подключается последним (gen_classes.py) и правит классы поверх их описаний.

Правила роли (проверка — tools/class_audit.py):
  Боец      — сильный урон по цели и по площади, польза отряду (прерывание, клич, метка), выживаемость.
  Поддержка — минимум два лечащих навыка, спасение (воскрешение/оберег/щит/заслон), бафф отряду.
  Танк      — провокация, личная защита, угроза по площади.

Порядок: 1) новые навыки; 2) удаление дублей (с заменой у игроков); 3) ручные переработки знаковых классов;
4) автоправка остальных: навык по мотивам класса встаёт вместо самого слабого или повторяющегося.
"""
from .dsl import K, C, SKILLS, CLASSES, ORDER, ALIASES

# ============================ 1. Новые навыки ============================

# --- прерывания (сбивают подготовку навыка хранителя) ---
K('kick', 'Пинок', 'Kick', 'INTERRUPT', '✋', m=10, cd=12, st='AGI', pw=0.4, r=4, f='Быстрый удар по руке, что творит заклятье')
K('pummel', 'Сбивающий удар', 'Pummel', 'INTERRUPT', '✊', m=10, cd=14, pw=0.5, r=4, f='Рукоятью — прямо в челюсть')
K('counter_shot', 'Сбивающий выстрел', 'Counter Shot', 'INTERRUPT', '➹', m=15, cd=16, st='AGI', pw=0.5, r=30, f='Стрела в ладонь колдующего')
K('silence_word', 'Безмолвие', 'Silence', 'INTERRUPT', '⊘', m=20, cd=18, el='ARCANE', pw=0.4, r=26, f='Слово, что гасит чужие чары')
K('mind_freeze', 'Ледяной разум', 'Mind Freeze', 'INTERRUPT', '❄', m=15, cd=15, el='FROST', pw=0.4, r=18, f='Холод сковывает мысль врага')
K('spell_lock', 'Запрет чар', 'Spell Lock', 'INTERRUPT', '⛧', m=15, cd=18, el='SHADOW', pw=0.4, r=26, f='Тень хватает колдуна за горло')
K('wind_shear', 'Пронизывающий ветер', 'Wind Shear', 'INTERRUPT', '≋', m=15, cd=14, el='NATURE', pw=0.3, r=26, f='Порыв сбивает дыхание и заклятье')
K('skull_bash', 'Удар черепом', 'Skull Bash', 'INTERRUPT', '✹', m=15, cd=14, pw=0.6, r=8, f='Зверь бьёт лбом с разбега')
K('rebuke', 'Укор', 'Rebuke', 'INTERRUPT', '☩', m=15, cd=15, el='HOLY', pw=0.4, r=6, f='Свет обрывает нечестивую молитву')
K('flash_bang', 'Ослепляющая вспышка', 'Flashbang', 'INTERRUPT', '✶', m=15, cd=16, st='AGI', pw=0.2, r=20, f='Хлопок и вспышка — не до заклинаний')

# --- боевые кличи и эффекты отряда ---
K('battle_shout', 'Боевой клич', 'Battle Shout', 'BUFF_ALLIES', '📯', m=30, cd=45, r=16, me='might 20 0', f='Голос, от которого крепнет рука')
K('bloodlust', 'Жажда крови', 'Bloodlust', 'BUFF_ALLIES', '♨', m=80, cd=150, r=20, me='might 15 1; haste 15 1; focus 15 0',
  f='Барабаны древней Орды: отряд бьёт вдвое злее')
K('rallying_cry', 'Ободряющий клич', 'Rallying Cry', 'BUFF_ALLIES', '⚑', m=50, cd=90, r=16, me='bulwark 8 1; abs 8 2',
  f='Перед страшным ударом — все вместе')
K('hunters_call', 'Зов охотника', 'Hunter\'s Call', 'BUFF_ALLIES', '➶', m=30, cd=50, r=16, me='might 20 0; spd 20 0', f='Стая идёт за вожаком')
K('arcane_focus', 'Чародейское сосредоточение', 'Arcane Focus', 'BUFF_ALLIES', '✧', m=40, cd=60, r=16, me='focus 15 1',
  f='Мысли отряда звенят, как натянутая струна')
K('dark_pact_group', 'Тёмный договор', 'Dark Pact', 'BUFF_ALLIES', '⛧', m=0, cd=70, r=16, hp=0.15, me='might 15 1',
  f='Кровь заклинателя — сила отряду')
K('aura_mastery', 'Купол веры', 'Aura Mastery', 'BUFF_ALLIES', '◎', m=60, cd=110, r=14, me='bulwark 6 2; res 6 0',
  f='Свет встаёт куполом над отрядом')
K('expose_armor', 'Вскрыть броню', 'Expose Armor', 'STRIKE', '⚔', m=20, cd=14, pw=1.0, r=4, foe='vuln 15 1',
  f='Удар в щель доспеха — и все бьют туда')
K('weakpoint_shot', 'Выстрел в слабину', 'Weakpoint Shot', 'ARROWS', '◎', m=20, cd=16, st='AGI', pw=1.2, foe='vuln 15 1',
  f='Стрела находит трещину в панцире')
K('curse_of_frailty', 'Проклятие хрупкости', 'Curse of Frailty', 'STRIKE', '☠', m=25, cd=18, el='SHADOW', pw=0.6, r=24, foe='vuln 15 1',
  f='Плоть врага трескается, как сухая глина')

# --- боевое воскрешение ---
K('resurrection', 'Воскрешение', 'Resurrection', 'REZ', '✝', m=60, cd=90, r=6, me='regen 6 1', f='Свет возвращает душу в тело')
K('rebirth', 'Перерождение', 'Rebirth', 'REZ', '❀', m=60, cd=90, r=6, f='Семя жизни прорастает в павшем')
K('spirit_call', 'Зов духа', 'Spirit Call', 'REZ', '☁', m=60, cd=90, r=6, f='Дух предков ведёт душу назад')
K('defibrillator', 'Дефибриллятор', 'Defibrillator', 'REZ', '⚡', m=50, cd=90, r=5, f='Разряд — и сердце снова бьётся')
K('soul_return', 'Возврат души', 'Soul Return', 'REZ', '⟲', m=60, cd=90, r=8, f='Время отматывается назад — к живому')
K('song_of_return', 'Песнь возвращения', 'Song of Return', 'REZ', '♫', m=60, cd=90, r=8, f='Мелодия, которую слышат и мёртвые')
K('soul_bargain', 'Сделка с душой', 'Soul Bargain', 'REZ', '⛧', m=0, cd=90, r=6, hp=0.25, f='Своя кровь — за чужую жизнь')
K('ancestors_call', 'Зов предков', 'Ancestors\' Call', 'REZ', '♅', m=60, cd=90, r=6, f='Предки не отпускают своих')

# --- лечение по мотивам ---
K('rune_mend', 'Руна исцеления', 'Rune of Mending', 'HEAL_TARGET', 'ᚱ', m=20, cd=4, pw=1.4, r=16, f='Руна вспыхивает на ране')
K('fate_mend', 'Нить судьбы', 'Thread of Fate', 'TETHER', '∞', m=20, cd=9, el='ARCANE', pw=1.2, r=18, d=10, f='Судьба не даёт оборваться нити')
K('chrono_mend', 'Перемотка ран', 'Rewind Wounds', 'HEAL', '⟲', m=35, cd=12, pw=1.3, r=10, f='Раны отряда отматываются назад')
K('mending_chord', 'Целебный аккорд', 'Mending Chord', 'HEAL_TARGET', '♪', m=18, cd=4, pw=1.3, r=16, f='Нота, что затягивает раны')
K('battlefield_triage', 'Полевой лазарет', 'Battlefield Triage', 'ZONE', '✚', m=45, cd=20, el='HOLY', pw=-0.7, r=8, d=6, f='Носилки, бинты и твёрдая рука')
K('war_medic', 'Перевязка в строю', 'Field Bandage', 'HEAL_TARGET', '✙', m=18, cd=5, pw=1.3, r=10, f='Ремень, жгут — и снова в бой')
K('mana_spring', 'Родник маны', 'Mana Spring', 'ZONE', '≈', m=40, cd=18, el='NATURE', pw=-0.6, r=8, d=8, f='Холодная вода лечит и бодрит')
K('gilded_salve', 'Золотая мазь', 'Gilded Salve', 'HEAL', '✦', m=35, cd=10, pw=1.2, r=10, f='Дорогое лекарство для всех')
K('ward_glyph', 'Глиф защиты', 'Ward Glyph', 'HEAL_TARGET', '◇', m=20, cd=5, pw=1.3, r=18, f='Знак, вычерченный светом, затягивает раны')
K('spirit_mend_wave', 'Волна духов', 'Spirit Wave', 'HEAL', '☁', m=35, cd=10, pw=1.2, r=10, f='Духи касаются ран отряда')
K('stone_mend', 'Каменная повязка', 'Stone Mend', 'HEAL_TARGET', '◆', m=20, cd=5, pw=1.3, r=12, f='Камень стягивает раны')

# --- защита отряда (большие кулдауны поддержки) ---
K('spirit_link_totem', 'Тотем духовной связи', 'Spirit Link Totem', 'BUFF_ALLIES', '♅', m=70, cd=120, r=14, me='bulwark 8 2; regen 8 1',
  f='Духи делят боль поровну')
K('time_shelter', 'Застывший миг', 'Frozen Moment', 'BUFF_ALLIES', '⧗', m=70, cd=120, r=14, me='bulwark 6 3', f='Удар растягивается во времени и слабеет')

# ============================ 2. Удаление дублей ============================
# класс → на что заменить у игроков, уже выбравших его
REMOVE = {
    'samurai': 'blademaster', 'bladefury': 'blademaster', 'raider': 'barbarian', 'swashbuckler': 'duelist',
    'arquebusier': 'gunslinger', 'knife_thrower': 'shadowblade', 'javelineer': 'dragoon', 'frost_lich': 'cryomancer',
    'twin_wyrm': 'dragonfire_sorceress', 'turret_engineer': 'inventor', 'sutler': 'alchemist',
}
for k, to in REMOVE.items():
    if k in CLASSES:
        del CLASSES[k]
        ORDER.remove(k)
        ALIASES[k] = to


def swap(cls, old, new):
    c = CLASSES[cls]
    c.skills[c.skills.index(old)] = new


def passive(cls, *names):
    for n in names:
        if n not in CLASSES[cls].trait_passives:
            CLASSES[cls].trait_passives.append(n)


# ============================ 3. Ручные переработки ============================

# Хронобегунья: полубоец-полуподдержка. Урон от скорости, призрачный прорыв сквозь босса,
# откат времени лечит всех вокруг — спасение отряда после сильных ударов хранителя.
K('chrono_pistols', 'Хронопистоли', 'Chrono Pistols', 'BARRAGE', '⁑', m=10, cd=3, st='AGI', pw=0.45, n=6, fl='speed',
  f='Пули летят тем быстрее, чем быстрее она')
K('phase_dash', 'Призрачный прорыв', 'Phase Dash', 'DASH_STRIKE', '⇶', m=20, cd=8, st='AGI', pw=1.5, r=2.8, fl='phase speed',
  f='Она проходит сквозь врага, как сквозь туман')
K('chrono_surge', 'Хроноускорение', 'Chrono Surge', 'BUFF_SELF', '⏩', m=25, cd=25, me='spd 10 2; might 10 0; focus 10 0',
  f='Время вокруг неё течёт медленнее')
K('stop_frame', 'Стоп-кадр', 'Stop Frame', 'INTERRUPT', '⏸', m=15, cd=14, st='AGI', pw=0.5, r=24, f='Выстрел в застывшую секунду')
K('rewind_strike', 'Откат времени', 'Rewind', 'RECALL', '⟲', m=40, cd=35, pw=0.35, r=10, fl='group', f='Назад во времени — и для неё, и для отряда')
K('momentum_fan', 'Веер на бегу', 'Running Fan', 'ARROWS', '⋙', m=20, cd=8, st='AGI', pw=0.8, n=5, fl='speed', f='Очередь во все стороны на полном ходу')
K('great_rewind', 'Великий откат', 'Great Rewind', 'RECALL', '⧖', m=90, cd=120, pw=0.6, r=18, fl='group', me='bulwark 6 1',
  f='Весь бой отматывается на три секунды')
c = CLASSES['chronorunner']
c.skills = 'chrono_pistols phase_dash rewind_strike chrono_surge stop_frame momentum_fan pulse_bomb great_rewind'.split()
c.trait_passives = ['momentum']
c.trait_bonus = {'move_speed': 10.0, 'dodge': 5.0}
c.desc = ('Бегунья во времени: чем быстрее, тем больнее бьёт; проходит сквозь врагов призраком '
          'и откатывает время назад — раны всего отряда затягиваются после страшных ударов хранителя.')

# Медвежий воитель: «ярость когтей» как у Урсы — каждый удар по той же цели сильнее.
passive('ursa_warrior', 'fury_swipes')
swap('ursa_warrior', 'fury_swipes', 'skull_bash')
CLASSES['ursa_warrior'].desc = 'Зверь-воин: каждый удар по той же цели сильнее прежнего (ярость когтей), подавление и бешенство.'

# Ликантроп — тоже копит ярость на жертве.
passive('lycanthrope', 'fury_swipes')

# Шаман и вожак Орды — «Жажда крови» (бафф всего отряда, как в MMO).
swap('shaman', 'spirit_wolf', 'bloodlust')
swap('horde_warlord', 'horde_call', 'bloodlust')
passive('horde_warlord', 'war_aura')

# Ауры — «рейдовые баффы» классов-опор.
passive('warrior', 'war_aura')
passive('paladin', 'guard_aura')
passive('templar', 'guard_aura')
passive('chieftain', 'war_aura')
passive('snow_maiden', 'mana_aura')
passive('lightbearer', 'mana_aura')
passive('hermit', 'mana_aura')
passive('clockmaker', 'focus_aura')
passive('chronomancer', 'focus_aura')

# Поддержки без лечения становятся бойцами-командирами (как «Аугментация» в MMO): урон и баффы отряду.
for k in ('warlord_tactician', 'bannerman', 'ogre_magi', 'illusionist', 'windlord', 'goblin_chief'):
    if k in CLASSES:
        CLASSES[k].role = 'DAMAGE'
passive('warlord_tactician', 'war_aura')
passive('bannerman', 'war_aura')
passive('windlord', 'focus_aura')

# Тематические лечения и спасения поддержек (до автоправки, чтобы она не подставила безликие).
THEMED = {
    'clockmaker': [('mana_bolt', 'chrono_mend'), ('blink', 'soul_return'), ('time_warp', 'time_shelter')],
    'chronomancer': [('temporal_bolt', 'chrono_mend'), ('time_warp', 'soul_return'), ('haste_time', 'time_shelter')],
    'seer': [('mana_bolt', 'fate_mend'), ('time_warp', 'ward_glyph'), ('fate_thread', 'soul_return')],
    'worldwalker': [('worldwalk', 'spirit_mend_wave'), ('time_warp', 'soul_return'), ('astral_shelter', 'ward_glyph')],
    'enchanter': [('mana_bolt', 'rune_mend'), ('dispel', 'ward_glyph'), ('arcane_shield', 'resurrection')],
    'runesmith': [('sparks', 'rune_mend'), ('anvil_drop', 'ancestors_call')],
    'snow_maiden': [('ice_shard', 'mana_spring'), ('ice_wall', 'ward_glyph'), ('ice_armor', 'rebirth')],
    'street_musician': [('swift_step', 'mending_chord'), ('encore', 'song_of_return')],
    'bard': [('lullaby', 'mending_chord'), ('encore', 'song_of_return')],
    'skald': [('roar', 'mending_chord'), ('throwing_axe', 'song_of_return')],
    'golden_magnate': [('market_crash', 'gilded_salve'), ('buyout', 'defibrillator')],
    'armorsmith': [('whip_shot', 'war_medic'), ('bulwark_slam', 'rallying_cry')],
    'shaman': [('quake', 'spirit_link_totem')],
    'voodoo_healer': [('curse', 'spirit_call')],
    'totem_carver': [('mana_totem', 'spirit_link_totem'), ('flame_totem', 'ancestors_call')],
    'field_medic': [('volley', 'defibrillator')],
    'apothecary': [('camouflage', 'defibrillator')],
    'cleric': [('smite', 'resurrection')],
    'healer': [('purify', 'resurrection')],
    'saint': [('martyrdom', 'resurrection'), ('purify', 'aura_mastery')],
    'druid': [('thorn_wall', 'rebirth')],
    'lifeweaver': [('petal_platform', 'rebirth')],
    'grove_keeper': [('thorn_bolt', 'rebirth')],
    'enchantress': [('enchant_beast', 'rebirth')],
    'herbalist': [('calming_tea', 'rebirth'), ('thorn_bolt', 'blessing')],
    'medium': [('banish', 'spirit_call')],
    'mourner': [('wail', 'spirit_call')],
    'shadow_priest': [('bad_juju', 'soul_bargain')],
    'dark_patron': [('curse_of_avernus', 'soul_bargain'), ('frost_strike', 'spirit_mend_wave')],
    'sun_priestess': [('sunburst_flare', 'resurrection')],
    'preacher': [('holy_persuasion', 'resurrection')],
    'faith_defender': [('hammer_of_justice', 'aura_mastery')],
    'kitsune_priestess': [('kunai_rain', 'ward_glyph'), ('shadow_step', 'spirit_call')],
    'hermit': [('swift_step', 'spirit_call')],
    'zen_master': [('palm_strike', 'spirit_call')],
    'tidecaller': [('gush', 'rebirth')],
    'moon_priestess': [('leap_back', 'resurrection')],
    'guardian_angel': [('angelic_descent', 'resurrection')],
    'oracle': [('mana_bolt', 'fate_mend')],
    'lightbearer': [('reveal_heresy', 'greater_heal'), ('guardian_flight', 'resurrection')],
    'abjurer': [('mana_bolt', 'ward_glyph')],
    'alchemist': [('elixir_of_might', 'war_medic')],
    'camp_cook': [('ladle_bonk', 'defibrillator')],
    'shepherd': [('shepherd_crook', 'ancestors_call'), ('bear_trap', 'mending_touch')],
    'snake_charmer': [('snake_charm', 'rejuvenation'), ('hex', 'rebirth')],
    'biomancer': [('fade', 'defibrillator')],
    'wisp': [('blink', 'rebirth')],
}
for cls, pairs in THEMED.items():
    if cls not in CLASSES:
        continue
    for old, new in pairs:
        if old in CLASSES[cls].skills and new not in CLASSES[cls].skills:
            swap(cls, old, new)

# ============================ 4. Автоправка ============================

MOBILITY = {'DASH', 'BLINK', 'SHADOW_STEP', 'GRAPPLE', 'STEALTH', 'RALLY'}
DROP_PRIORITY = {'RESET': 10, 'GRAPPLE': 9, 'GATHER': 8, 'RALLY': 7, 'CHARM': 7, 'SWAP': 7, 'SACRIFICE': 6, 'MEDITATE': 5,
                 'WALL': 5, 'STEAL': 4, 'MINE': 3, 'REVEAL': 3, 'DOMINATE': 4, 'PURIFY': 2}


def theme(c):
    st = c.styles
    els = [SKILLS[s].el for s in c.skills if SKILLS[s].el]
    el = max(set(els), key=els.count) if els else None
    return st, el


def pick_for(c, need):
    st, el = theme(c)
    magic, ranged, melee = st.get('magic', 0), st.get('ranged', 0), st.get('melee', 0)
    if need == 'util':
        if st.get('beast', 0) >= 2:
            return 'skull_bash'
        if st.get('craft', 0) >= 2:
            return 'flash_bang'
        if magic >= 2 or (el and melee < 2 and ranged < 2):
            return {'FROST': 'mind_freeze', 'SHADOW': 'spell_lock', 'NATURE': 'wind_shear', 'LIGHTNING': 'wind_shear',
                    'POISON': 'curse_of_frailty', 'HOLY': 'rebuke', 'EARTH': 'silence_word'}.get(el, 'silence_word')
        if ranged >= 2:
            return 'counter_shot' if st.get('party', 0) < 2 else 'hunters_call'
        if st.get('stealth', 0) >= 2:
            return 'kick'
        if st.get('party', 0) >= 2:
            return 'battle_shout'
        return 'pummel'
    if need == 'aoe':
        if magic >= 2:
            return {'FIRE': 'ring_of_fire', 'FROST': 'frost_nova', 'SHADOW': 'corpse_explosion', 'LIGHTNING': 'static_field',
                    'POISON': 'plague_cloud', 'NATURE': 'entangle', 'HOLY': 'consecration'}.get(el, 'starfall')
        if ranged >= 2:
            return 'arrow_rain'
        if st.get('stealth', 0) >= 2:
            return 'fan_of_knives'
        return 'whirlwind'
    if need == 'st':
        if magic >= 2:
            return {'FIRE': 'fireball', 'FROST': 'ice_shard', 'SHADOW': 'chaos_bolt', 'LIGHTNING': 'spark'}.get(el, 'mana_bolt')
        if ranged >= 2:
            return 'aimed_shot'
        return 'execute'
    if need == 'surv':
        if magic >= 2:
            return 'blink'
        if ranged >= 2:
            return 'leap_back'
        return 'iron_skin'
    if need == 'heal':
        if el in ('HOLY',):
            return 'greater_heal'
        if el in ('NATURE', 'POISON'):
            return 'rejuvenation'
        if st.get('undead', 0) >= 1 or el == 'SHADOW':
            return 'spirit_mend_wave'
        if st.get('craft', 0) >= 2:
            return 'war_medic'
        if magic >= 2:
            return 'ward_glyph'
        return 'mending_touch'
    if need == 'save':
        if el == 'HOLY':
            return 'resurrection'
        if el in ('NATURE', 'POISON') or st.get('nature', 0) >= 2:
            return 'rebirth'
        if el == 'SHADOW' or st.get('undead', 0) >= 1:
            return 'soul_bargain'
        if st.get('craft', 0) >= 2 or st.get('ranged', 0) >= 2:
            return 'defibrillator'
        if st.get('beast', 0) >= 1:
            return 'ancestors_call'
        return 'spirit_call'
    if need == 'buff':
        return 'blessing'
    return {'taunt': 'taunt', 'guard': 'iron_skin', 'tankaoe': 'quake'}[need]


NEED_OF = {
    'нет пользы группе': 'util', 'нет урона по площади': 'aoe', 'нет сильного урона по цели': 'st', 'нет выживаемости': 'surv',
    'мало атакующих': 'st', 'мало лечения': 'heal', 'нет спасения': 'save', 'нет баффа группе': 'buff',
    'нет провокации': 'taunt', 'нет личной защиты': 'guard', 'нет угрозы по площади': 'tankaoe',
}


def victim_score(c, i):
    s = SKILLS[c.skills[i]]
    score = DROP_PRIORITY.get(s.kind, 0)
    kinds = [SKILLS[x].kind for x in c.skills]
    if s.kind in MOBILITY and sum(k in MOBILITY for k in kinds) > 1:
        score += 6
    if s.kind == 'BUFF_SELF' and not any(n in ('res', 'abs') for *_, n in s.me) and kinds.count('BUFF_SELF') > 1:
        score += 5
    if s.kind == 'SUMMON' and kinds.count('SUMMON') > 1:
        score += 4
    if s.kind == 'BOLT' and c.role == 'SUPPORT':
        score += 3
    if kinds.count(s.kind) > 1:
        score += 2
    heals = s.kind in ('HEAL', 'HEAL_TARGET', 'TETHER') or (s.kind in ('ZONE', 'ZONE_AT') and (s.pow < 0 or s.el in ('HOLY', 'NATURE')))
    if c.role == 'SUPPORT' and (heals or s.kind in ('REZ', 'WARD', 'SHIELD')):
        score -= 8    # лечение и спасение — суть поддержки
    if s.kind == 'BUFF_ALLIES' and any(n in ('str', 'might', 'res', 'abs', 'bulwark', 'focus') for *_, n in s.me):
        score -= 4    # бафф отряду — ценность класса
    if c.role == 'DAMAGE' and s.pow >= 1.2 and s.kind not in ('BUFF_SELF', 'BUFF_ALLIES'):
        score -= 3    # сильная атака — костяк бойца
    if s.kind in ('INTERRUPT', 'REZ'):
        score -= 10
    if i == 0:
        score -= 20   # первый — основной удар
    if i == 7:
        score -= 6    # последний — коронный навык
    return score


MANUAL = {'chronorunner'}   # переработаны вручную целиком — автоправка не трогает


def autofix():
    import class_audit  # noqa: E402 — правила ревизии общие
    for key in ORDER:
        c = CLASSES[key]
        if c.invoke or key in MANUAL:
            continue
        for _ in range(5):
            probs = class_audit.audit(c)
            if not probs:
                break
            need = None
            for p in probs:
                for k, v in NEED_OF.items():
                    if p.startswith(k):
                        need = v
                        break
                if need:
                    break
            if not need:
                break
            new = pick_for(c, need)
            if need == 'heal' and new in c.skills:
                new = next((h for h in ('mending_touch', 'ward_glyph', 'rune_mend', 'chrono_mend', 'gilded_salve', 'spirit_mend_wave')
                            if h not in c.skills), new)
            if new in c.skills:
                alt = {'blink': 'leap_back', 'iron_skin': 'last_stand', 'whirlwind': 'blade_dance', 'execute': 'expose_armor',
                       'pummel': 'expose_armor', 'blessing': 'battle_shout', 'quake': 'thunderclap'}.get(new)
                if not alt or alt in c.skills:
                    break
                new = alt
            best, best_bad = None, None
            for i in sorted(range(8), key=lambda i: -victim_score(c, i)):
                old = c.skills[i]
                c.skills[i] = new
                bad = class_audit.score(c)
                c.skills[i] = old
                if best is None or bad < best_bad or (bad == best_bad and victim_score(c, i) > victim_score(c, best)):
                    best, best_bad = i, bad
            if best_bad is not None and best_bad >= class_audit.score(c):
                break
            c.skills[best] = new


autofix()

# ---- второй проход: у каждого бойца — вклад в урон отряда (бафф отряду или уязвимость врага), как в MMO ----
GROUP_DPS = {'might', 'vuln'}


def has_group_damage(c):
    for k in c.skills:
        sk = SKILLS[k]
        if sk.kind in ('MARK', 'REVEAL'):
            return True
        if any(n in GROUP_DPS for *_, n in sk.me) and sk.kind == 'BUFF_ALLIES' or any(n == 'vuln' for *_, n in sk.foe):
            return True
    return any(p in ('war_aura', 'focus_aura') for p in c.trait_passives)


def group_pick(c):
    st, el = theme(c)
    if st.get('melee', 0) >= 2 and (st.get('party', 0) >= 1 or st.get('risk', 0) >= 2 or st.get('tank', 0) >= 1):
        return 'battle_shout'
    if st.get('beast', 0) >= 2 or st.get('nature', 0) >= 2 and st.get('ranged', 0) >= 2:
        return 'hunters_call'
    if el in ('SHADOW',) and st.get('magic', 0) >= 2:
        return 'dark_pact_group'
    if st.get('magic', 0) >= 2:
        return 'curse_of_frailty' if el in ('POISON', 'SHADOW', 'EARTH') else 'arcane_focus'
    if st.get('ranged', 0) >= 2:
        return 'weakpoint_shot'
    return 'expose_armor'


def second_pass():
    import class_audit
    for key in ORDER:
        c = CLASSES[key]
        if c.invoke or c.role != 'DAMAGE' or has_group_damage(c) or key in MANUAL:
            continue
        new = group_pick(c)
        if new in c.skills:
            continue
        best, best_bad = None, None
        for i in sorted(range(8), key=lambda i: -victim_score(c, i)):
            old = c.skills[i]
            c.skills[i] = new
            bad = class_audit.score(c)
            c.skills[i] = old
            if best is None or bad < best_bad:
                best, best_bad = i, bad
        if best_bad == 0:
            c.skills[best] = new


second_pass()

# точечно: стихийные прерывания по душе класса
for cls, old, new in (('death_knight', 'chains_of_ice', 'mind_freeze'), ('paladin', 'hammer_of_justice', 'rebuke'),
                      ('templar', 'reveal_heresy', 'rebuke'), ('inquisitor', 'shackle', 'rebuke'),
                      ('cryomancer', 'deep_freeze', 'mind_freeze'), ('witch_hunter', 'burn_the_witch', 'rebuke')):
    if cls in CLASSES and old in CLASSES[cls].skills and new not in CLASSES[cls].skills:
        swap(cls, old, new)
for cls, old, new in (('healer', 'circle_of_healing', 'circle_of_healing'), ('camp_cook', 'fire_flask', 'battlefield_triage'),
                      ('geomancer', 'stone_bolt', 'stone_bolt')):
    if cls in CLASSES and old in CLASSES[cls].skills and new not in CLASSES[cls].skills:
        swap(cls, old, new)
