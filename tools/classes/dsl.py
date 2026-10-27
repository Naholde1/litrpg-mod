"""
Язык описания навыков и классов для gen_classes.py.

Навык:  K(key, 'Название', 'Name', 'ВИД', 'иконка', m=мана, cd=сек, ...параметры, f='краткое описание')
Класс:  C(key, 'Название', 'Name', 'РОЛЬ', 0xЦВЕТ, 'ОСН', 'ВТОР', 'стиль вес ...', skills='k1 k2 ...', trait=(...), desc='...')

Описание механики навыка строится само по параметрам (чтобы текст всегда совпадал с игрой),
а f= — короткая художественная часть перед ним.
"""

SKILLS = {}
CLASSES = {}
ORDER = []  # порядок классов

KINDS = set("""DASH DASH_STRIKE LEAP_SLAM NOVA MULTI_NOVA STRIKE MULTI_STRIKE BOLT CHAIN LIGHTNING METEOR ZONE ZONE_AT
ARROWS BARRAGE ARROW_RAIN BUFF_SELF BUFF_ALLIES HEAL HEAL_SELF MEDITATE TAUNT PULL BLINK SHADOW_STEP STEALTH ON_HIT MARK
PURIFY SUMMON BEAM CONE VORTEX LAUNCH PUSH STUN STASIS WALL SWAP RECALL GRAPPLE CHARM DOMINATE REVEAL STEAL RESET
SACRIFICE MINE TETHER WARD SHIELD HEAL_TARGET GLOBAL RALLY GATHER ORB INVOKE INTERRUPT REZ
TARGET_HEAL CAST_HEAL HOT CHAIN_HEAL TARGET_BOLT CAST_BOLT DOT""".split())
ALIASES = {}  # удалённый класс → замена (у игроков, выбравших его раньше)

# Навыки, которые по самой своей сути колдуются, а не бьются: лечение, щиты, воскрешение,
# очищение. У них нет стихии, поэтому раньше они по умолчанию считались от Силы.
MAGIC_KINDS = {'HEAL', 'HEAL_SELF', 'HEAL_TARGET', 'TARGET_HEAL', 'CAST_HEAL', 'CHAIN_HEAL', 'HOT',
               'SHIELD', 'REZ', 'PURIFY'}

ELEMENTS = {'ARCANE', 'FIRE', 'FROST', 'HOLY', 'SHADOW', 'LIGHTNING', 'POISON', 'NATURE', 'EARTH'}
STATS = {'STR': 'STRENGTH', 'AGI': 'AGILITY', 'VIT': 'VITALITY', 'INT': 'INTELLIGENCE', 'LUCK': 'LUCK'}
STAT_GEN = {'STRENGTH': 'Силы', 'AGILITY': 'Ловкости', 'VITALITY': 'Выносливости', 'INTELLIGENCE': 'Интеллекта', 'LUCK': 'Удачи'}
EL_INS = {'FIRE': 'огнём', 'FROST': 'холодом', 'HOLY': 'светом', 'SHADOW': 'тьмой', 'LIGHTNING': 'молнией',
          'POISON': 'ядом', 'NATURE': 'природой', 'EARTH': 'камнем', 'ARCANE': 'магией'}

# короткое имя эффекта: (id, по-русски)
EFFECTS = {
    'str': ('minecraft:strength', 'Сила'), 'res': ('minecraft:resistance', 'Сопротивление'),
    'spd': ('minecraft:speed', 'Скорость'), 'slow': ('minecraft:slowness', 'Замедление'),
    'weak': ('minecraft:weakness', 'Слабость'), 'regen': ('minecraft:regeneration', 'Регенерация'),
    'abs': ('minecraft:absorption', 'Поглощение'), 'haste': ('minecraft:haste', 'Спешка'),
    'blind': ('minecraft:blindness', 'Слепота'), 'wither': ('minecraft:wither', 'Иссушение'),
    'poison': ('minecraft:poison', 'Отравление'), 'hp': ('minecraft:health_boost', 'Прилив здоровья'),
    'luck': ('minecraft:luck', 'Удача'), 'fireres': ('minecraft:fire_resistance', 'Огнестойкость'),
    'nausea': ('minecraft:nausea', 'Тошнота'), 'glow': ('minecraft:glowing', 'Свечение'),
    'jump': ('minecraft:jump_boost', 'Прыгучесть'), 'invis': ('minecraft:invisibility', 'Невидимость'),
    'nightv': ('minecraft:night_vision', 'Ночное зрение'), 'water': ('minecraft:water_breathing', 'Подводное дыхание'),
    'slowfall': ('minecraft:slow_falling', 'Плавное падение'), 'levit': ('minecraft:levitation', 'Левитация'),
    'dolphin': ('minecraft:dolphins_grace', 'Грация дельфина'), 'darkness': ('minecraft:darkness', 'Тьма'),
    'fatigue': ('minecraft:mining_fatigue', 'Усталость'), 'hunger': ('minecraft:hunger', 'Голод'),
    'hero': ('minecraft:hero_of_the_village', 'Герой деревни'), 'conduit': ('minecraft:conduit_power', 'Сила источника'),
    'satur': ('minecraft:saturation', 'Насыщение'),
    # эффекты отряда (мод): каждый уровень — 10%
    'might': ('litrpg:might', 'Воодушевление'), 'bulwark': ('litrpg:bulwark', 'Заслон'),
    'vuln': ('litrpg:vulnerability', 'Уязвимость'), 'focus': ('litrpg:focus', 'Сосредоточенность'),
}

SUMMON_RU = {
    'minecraft:zombie': 'зомби', 'minecraft:husk': 'кадавр', 'minecraft:drowned': 'утопленник',
    'minecraft:skeleton': 'скелет', 'minecraft:stray': 'зимогор', 'minecraft:wither_skeleton': 'скелет-иссушитель',
    'minecraft:wolf': 'волк', 'minecraft:vex': 'бес', 'minecraft:iron_golem': 'железный голем',
    'minecraft:snow_golem': 'снежный голем', 'minecraft:blaze': 'дух пламени', 'minecraft:spider': 'паук',
    'minecraft:cave_spider': 'пещерный паук', 'minecraft:polar_bear': 'медведь', 'minecraft:vindicator': 'поборник',
    'minecraft:piglin_brute': 'громила', 'minecraft:silverfish': 'чешуйница', 'minecraft:ravager': 'разоритель',
    'minecraft:endermite': 'эндермит', 'minecraft:zombified_piglin': 'зомби-пиглин', 'minecraft:magma_cube': 'магмовый куб',
    'minecraft:slime': 'слизень', 'minecraft:bee': 'пчела', 'minecraft:fox': 'лис', 'minecraft:goat': 'горный козёл',
    'minecraft:pillager': 'разбойник', 'minecraft:panda': 'панда',
}

WALL_RU = {'minecraft:packed_ice': 'льда', 'minecraft:magma_block': 'магмы', 'minecraft:oak_leaves': 'живых ветвей',
           'minecraft:white_stained_glass': 'света', 'minecraft:blackstone': 'чёрного камня', 'minecraft:cobblestone': 'камня',
           'minecraft:cobweb': 'паутины', 'minecraft:bone_block': 'костей', 'minecraft:amethyst_block': 'кристаллов',
           'minecraft:sand': 'песка', 'minecraft:iron_bars': 'решёток', 'minecraft:mud_bricks': 'глины',
           'minecraft:prismarine': 'кораллового камня', 'minecraft:obsidian': 'обсидиана'}

ONHIT_RU = {'POISON': 'отравляют', 'FIRE': 'поджигают', 'FROST': 'замораживают', 'LIFESTEAL': 'лечат на четверть урона',
            'ARCANE': 'наносят доп. магический урон', 'BLEED': 'вызывают кровотечение', 'STUN': 'оглушают на миг',
            'MANA': 'восстанавливают ману', 'CHAIN': 'бьют молнией соседей'}

ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']


def _num(v):
    return ('%g' % round(v, 1)).replace('.', ',')


def _pct(p):
    return f"{round(p * 100)}%"


def parse_fx(s):
    """'str 10 1; slow 4 2' → [(id, тики, ур.)]"""
    out = []
    if not s:
        return out
    for part in s.split(';'):
        part = part.split()
        if not part:
            continue
        name, sec, amp = part[0], float(part[1]), int(part[2]) if len(part) > 2 else 0
        if name not in EFFECTS:
            raise ValueError('unknown effect ' + name)
        out.append((EFFECTS[name][0], int(round(sec * 20)), amp, name))
    return out


def fx_text(lst):
    return ', '.join(f"{EFFECTS[n][1]} {ROMAN[a]} ({_num(t / 20)} с)" for _, t, a, n in lst)


class Skill:
    def __init__(self, key, ru, en, kind, icon, m=20, cd=10, el=None, st=None, pw=1.0, r=4, d=10, n=1, me='', foe='',
                 fl='', oh=None, sm=None, wall=None, f='', raw=None, hp=0.0, combo=0):
        if kind not in KINDS:
            raise ValueError(f'{key}: unknown kind {kind}')
        if el is not None and el not in ELEMENTS:
            raise ValueError(f'{key}: unknown element {el}')
        self.key, self.ru, self.en, self.kind, self.icon = key, ru, en, kind, icon
        self.mana, self.cd = m, cd
        self.el = el
        # по умолчанию навык считается от Силы, а со стихией — от Интеллекта.
        # Лечение, щиты и воскрешение стихии не имеют, и 43 навыка лекарей из-за этого
        # считались от Силы: интеллект поддержке ничего не давал. Теперь они тоже от Интеллекта;
        # явный st= по-прежнему сильнее (целебный дротик следопыта так и остаётся от Ловкости).
        self.stat = STATS[st] if st else ('INTELLIGENCE' if el or kind in MAGIC_KINDS else 'STRENGTH')
        self.pow, self.rad, self.dur, self.cnt = pw, r, d, n
        self.me, self.foe = parse_fx(me), parse_fx(foe)
        self.flags = fl.split()
        for x in self.flags:
            if x.split(':')[0] not in ('exec', 'back', 'steal', 'pierce', 'reflect', 'finish', 'perfoe', 'nostyle', 'group', 'phase', 'speed'):
                raise ValueError(f'{key}: unknown flag {x}')
        self.onhit, self.summon, self.wall = oh, sm, wall
        if sm and sm not in SUMMON_RU:
            raise ValueError(f'{key}: unknown summon {sm}')
        self.flavor, self.raw = f, raw
        self.hp, self.combo = hp, combo

    # ---------- описание ----------
    def dmg(self, k=1.0):
        t = f"урон {_pct(self.pow * k)} от {STAT_GEN[self.stat]}"
        if self.el:
            t += ' ' + EL_INS[self.el]
        return t

    def mechanics(self):
        r, d, n = _num(abs(self.rad)), _num(self.dur), self.cnt
        heals = self.el in ('HOLY', 'NATURE') or self.pow < 0
        k = self.kind
        t = {
            'DASH': (f"Отскок назад на {r} бл." if self.rad < 0 else f"Рывок на {r} бл. вперёд"),
            'DASH_STRIKE': f"Рывок вперёд: {self.dmg()} всем на пути",
            'LEAP_SLAM': f"Прыжок и удар при приземлении: {self.dmg()} в радиусе {r}",
            'NOVA': (f"Волна вокруг (радиус {r})" if self.pow == 0 else f"Волна вокруг: {self.dmg()}, радиус {r}"),
            'MULTI_NOVA': f"{n} волн вокруг, каждая — {self.dmg()}, радиус {r}",
            'STRIKE': f"Удар по цели (до {r} бл.): {self.dmg()}",
            'MULTI_STRIKE': f"{n} ударов по цели, каждый — {self.dmg()}",
            'BOLT': f"Снаряд: {self.dmg()}",
            'CHAIN': f"Разряд скачет по {n} целям: {self.dmg()}, каждый следующий слабее",
            'LIGHTNING': f"Молния в цель: {self.dmg()} рядом с ударом",
            'METEOR': f"Удар с неба в точку прицела: {self.dmg()}, радиус {r}",
            'ZONE': (f"Зона вокруг на {d} с: каждую секунду " + (f"{self.dmg()}, " if self.pow > 0 else '')
                     + ("лечит союзников" if heals else "по врагам")).rstrip(', '),
            'ZONE_AT': (f"Зона в точке прицела на {d} с: каждую секунду " + (f"{self.dmg()}, " if self.pow > 0 else '')
                        + ("лечит союзников" if heals else "по врагам")).rstrip(', '),
            'ARROWS': (f"Выстрел: {self.dmg(0.12 * 8)}" if n == 1 else f"{n} снарядов веером, каждый — {self.dmg(0.12 * 8)}"),
            'BARRAGE': f"Очередь из {n} выстрелов, каждый — {self.dmg(0.12 * 8)}",
            'ARROW_RAIN': f"Дождь стрел в точке прицела {d} с, радиус {r}",
            'BUFF_SELF': "Усиливает вас",
            'BUFF_ALLIES': f"Усиливает союзников в {r} бл.",
            'HEAL': f"Лечит вас и союзников в {r} бл.: {_pct(self.pow * 0.8)} силы навыка",
            'HEAL_SELF': f"Восстанавливает {_pct(self.pow)} здоровья",
            'MEDITATE': f"Восстанавливает {_pct(self.pow)} маны и 20% здоровья",
            'TAUNT': f"Враги в {r} бл. нападают на вас",
            'PULL': f"Притягивает цель (до {r} бл.) к вам",
            'BLINK': f"Телепорт на {r} бл. вперёд",
            'SHADOW_STEP': f"Телепорт за спину цели (до {r} бл.)",
            'STEALTH': f"Скрытность на {d} с: монстры теряют вас из виду",
            'ON_HIT': f"Следующие {n} ударов {ONHIT_RU.get(self.onhit, '')}",
            'MARK': f"Метка на {d} с: цель получает +30% урона",
            'PURIFY': f"Снимает вредные эффекты с союзников в {r} бл., жжёт нежить",
            'SUMMON': f"Призывает: {SUMMON_RU.get(self.summon, '?')} ×{n} на {d} с",
            'BEAM': f"Луч на {r} бл.: {self.dmg()} всем на линии",
            'CONE': f"Конус перед вами ({r} бл.): {self.dmg()}",
            'VORTEX': f"Воронка в точке прицела на {d} с: затягивает врагов в радиусе {r}"
                      + (f", каждую секунду {self.dmg()}" if self.pow > 0 else ''),
            'LAUNCH': f"Подбрасывает врагов в радиусе {r}" + (f": {self.dmg()}" if self.pow > 0 else ''),
            'PUSH': f"Отбрасывает врагов перед вами ({r} бл.)" + (f": {self.dmg()}" if self.pow > 0 else ''),
            'STUN': f"Оглушает цель (до {r} бл.) на {d} с" + (f": {self.dmg()}" if self.pow > 0 else ''),
            'STASIS': f"Время замирает: враги в радиусе {r} у точки прицела застывают на {d} с",
            'WALL': f"Стена из {WALL_RU.get(self.wall, 'камня')} шириной {r} бл. на {d} с",
            'SWAP': f"Меняется местами с целью (до {r} бл.)" + (f": {self.dmg()}" if self.pow > 0 else ''),
            'RECALL': f"Возвращает туда, где вы были 3 с назад, и лечит {_pct(self.pow)} здоровья",
            'GRAPPLE': f"Крюк: притягивает вас к блоку под прицелом (до {r} бл.)",
            'CHARM': f"Усмиряет врагов в {r} бл. на {d} с: они не нападают",
            'DOMINATE': f"Подчиняет цель (до {r} бл.) на {d} с: она сражается за вас. Не действует на боссов",
            'REVEAL': f"Подсвечивает и метит врагов в {r} бл. на {d} с: +30% получаемого урона",
            'STEAL': f"Крадёт усиления цели (до {r} бл.)" + (f" и наносит {self.dmg()}" if self.pow > 0 else ''),
            'RESET': "Мгновенно сбрасывает перезарядку остальных навыков",
            'SACRIFICE': f"Жертвует {_pct(self.pow)} здоровья и восполняет вдвое больше маны (в долях)",
            'MINE': f"Невидимая ловушка на {d} с: взрыв {self.dmg()} в радиусе {r}",
            'TETHER': f"Связь с союзником (до {r} бл.) на {d} с: лечит его каждую секунду на {_pct(self.pow * 0.25)} силы",
            'WARD': f"Оберег на союзника (до {r} бл.) на {d} с: спасает от смертельного удара и лечит 30%",
            'SHIELD': f"Щит самому раненому союзнику в {r} бл.: поглощает {_pct(self.pow)} силы навыка",
            'HEAL_TARGET': f"Сильное лечение самого раненого союзника в {r} бл.: {_pct(self.pow)} силы навыка",
            'GLOBAL': f"Молнии бьют всех врагов в {r} бл.: {self.dmg()}",
            'RALLY': f"Телепорт к ближайшему союзнику-игроку (до {r} бл.)",
            'GATHER': f"Переносит союзников-игроков из {r} бл. к вам",
            'ORB': "Призывает сферу (до трёх). Сферы дают усиления и задают заклинание для Призыва",
            'INVOKE': "Сплетает три сферы в заклинание и кладёт его в ячейку",
            'INTERRUPT': f"Прерывание (до {r} бл.): сбивает подготовку навыка хранителя, обычных врагов оглушает"
                         + (f"; {self.dmg()}" if self.pow > 0 else ''),
            'REZ': f"Воскрешает павшего в бою союзника (до {r} бл.) сразу, без обряда",
            'TARGET_HEAL': f"Мгновенно лечит союзника на {_pct(self.pow)} силы навыка: выбранного (Ctrl+1…5), того, на кого смотрите, "
                           f"или самого раненого в {r} бл.",
            'CAST_HEAL': f"Подготовка {d} с (двигаться нельзя): лечит союзника на {_pct(self.pow)} силы навыка",
            'HOT': f"Лечение со временем: союзник получает {_pct(self.pow)} силы навыка за {d} с",
            'CHAIN_HEAL': f"Лечит союзника на {_pct(self.pow)} силы навыка и перескакивает ещё на {n - 1} раненых рядом",
            'TARGET_BOLT': f"Самонаводящийся снаряд в цель под прицелом (до {r} бл.): {self.dmg()}",
            'CAST_BOLT': f"Подготовка {d} с (двигаться нельзя): мощный снаряд — {self.dmg()}",
            'DOT': f"Проклятие на цель под прицелом на {d} с: каждую секунду {self.dmg()}",
        }[k]
        if k == 'RECALL' and 'group' in self.flags:
            t = f"Откат времени: вы возвращаетесь на место 3 с назад и лечите {_pct(self.pow)} здоровья, союзники в {r} бл. — {_pct(self.pow * 0.6)}"
        parts = [t]
        fl = self.flags
        if 'exec' in fl:
            parts.append("по цели ниже 35% здоровья ×2,5")
        if 'back' in fl:
            parts.append("в спину ×2")
        if 'steal' in fl:
            parts.append("лечит на часть урона")
        if 'pierce' in fl:
            parts.append("пробивает насквозь")
        if 'reflect' in fl:
            parts.append("возвращает половину полученного урона")
        if 'phase' in fl:
            parts.append("сквозь врагов, на миг неуязвим")
        if 'speed' in fl:
            parts.append("урон растёт со скоростью бега")
        if 'perfoe' in fl:
            parts.append("+20% за каждого врага рядом")
        if 'finish' in fl:
            parts.append("тратит приёмы серии: +35% урона за каждый")
        if self.combo:
            parts.append(f"+{self.combo} приём серии")
        if self.me:
            parts.append(("союзникам: " if k in ('BUFF_ALLIES', 'TETHER', 'SHIELD', 'HEAL_TARGET', 'WARD') or 'group' in fl else "вам: ") + fx_text(self.me))
        if self.foe:
            parts.append("врагам: " + fx_text(self.foe))
        if self.hp:
            parts.append(f"цена — {_pct(self.hp)} здоровья вместо маны")
        return '; '.join(parts)

    def desc(self):
        if self.raw:
            return self.raw
        m = self.mechanics()
        return (self.flavor.rstrip('.') + '. ' + m[0].upper() + m[1:]) if self.flavor else m[0].upper() + m[1:]

    def json(self):
        o = {'key': self.key, 'kind': self.kind, 'icon': self.icon, 'mana': self.mana, 'cd': int(round(self.cd * 20)),
             'stat': self.stat, 'pow': self.pow, 'rad': self.rad, 'dur': int(round(self.dur * 20)), 'cnt': self.cnt}
        if self.el:
            o['el'] = self.el
        if self.flags:
            o['flags'] = self.flags
        if self.onhit:
            o['onHit'] = self.onhit
        if self.summon:
            o['summon'] = self.summon
        if self.wall:
            o['wall'] = self.wall
        if self.me:
            o['self'] = [[i, t, a] for i, t, a, _ in self.me]
        if self.foe:
            o['foe'] = [[i, t, a] for i, t, a, _ in self.foe]
        if self.hp:
            o['hp'] = self.hp
        if self.combo:
            o['combo'] = self.combo
        return o


def K(key, ru, en, kind, icon, **kw):
    if key in SKILLS:
        raise ValueError('duplicate skill ' + key)
    s = Skill(key, ru, en, kind, icon, **kw)
    SKILLS[key] = s
    return s


STYLE_KEYS = ['melee', 'ranged', 'magic', 'tank', 'stealth', 'support', 'nature', 'undead', 'fire', 'craft',
              'party', 'solo', 'trade', 'explore', 'night', 'water', 'mining', 'risk', 'beast']
ROLES = ('TANK', 'DAMAGE', 'SUPPORT')
RACES = ('human', 'elf', 'dwarf', 'orc', 'beastkin', 'demonkin')

# бонусы черты класса: ключ GearBonus → (текст, процент?, растёт с уровнем?)
BONUS = {
    'health': ('здоровья', False, True), 'armor': ('брони', False, True), 'damage': ('урона оружием', False, True),
    'attack_speed': ('скорости атаки', True, False), 'move_speed': ('скорости бега', True, False),
    'crit_chance': ('шанса крита', True, False), 'crit_damage': ('силы крита', True, False), 'dodge': ('уклонения', True, False),
    'lifesteal': ('вампиризма', True, False), 'mana': ('маны', False, True), 'mana_regen': ('восстановления маны', True, False),
    'spell_power': ('силы навыков', True, False), 'cooldown': ('сокращения перезарядки', True, False), 'xp': ('опыта', True, False),
    'loot': ('шанса добычи', True, False), 'dmg_undead': ('урона по нежити', True, False),
    'dmg_arthropod': ('урона по паукам и насекомым', True, False), 'dmg_illager': ('урона по иллагерам и разбойникам', True, False),
    'dmg_goblin': ('урона по гоблинам', True, False), 'dmg_beast': ('урона по зверям', True, False),
    'dmg_nether': ('урона по существам Преисподней', True, False), 'dmg_elite': ('урона по элитам и боссам', True, False),
    'dmg_humanoid': ('урона по разумным расам', True, False), 'res_undead': ('защиты от нежити', True, False),
    'res_arthropod': ('защиты от пауков', True, False), 'res_illager': ('защиты от иллагеров', True, False),
    'res_goblin': ('защиты от гоблинов', True, False), 'res_fire': ('защиты от огня', True, False),
    'res_fall': ('защиты от падения', True, False), 'res_projectile': ('защиты от снарядов', True, False),
    'res_explosion': ('защиты от взрывов', True, False), 'res_magic': ('защиты от магии', True, False),
    'mining': ('скорости добычи', True, False), 'arrow_damage': ('урона стрелами', True, False),
    'mana_cost': ('экономии маны', True, False), 'health_regen': ('здоровья каждые 5 с', False, True),
    'coin_find': ('монет с монстров', True, False), 'sell_price': ('к цене продажи', True, False),
}
PASSIVES = {
    'flame_touch': 'удары поджигают', 'frost_touch': 'удары сильно замедляют', 'venom': 'удары отравляют',
    'chain_spark': '15% шанс цепной молнии при ударе', 'executioner': '+50% урона по врагам ниже 30% здоровья',
    'soul_eater': 'убийство лечит 15% здоровья и 10% маны', 'giant_slayer': '+25% урона по врагам выше уровнем',
    'thorns': 'возвращает 25% урона ближнего боя', 'phoenix': 'смертельный удар оставляет 30% здоровья (раз в 10 мин)',
    'fireproof': 'иммунитет к огню', 'night_eye': 'видит в темноте', 'featherfall': 'нет урона от падения',
    'swiftness': 'постоянная Скорость I', 'regeneration': 'постоянная Регенерация I', 'mana_shield': '20% урона поглощает мана',
    'berserker': '+30% урона при здоровье ниже 40%', 'water_breathing': 'дышит под водой',
    'arcane_echo': '15% шанс, что навык не уйдёт на перезарядку', 'treasure_hunter': 'вдвое выше шанс добычи',
    'fury_swipes': 'каждый удар по той же цели +8% урона (до +80%)', 'momentum': 'урон растёт со скоростью бега',
    'war_aura': 'аура: союзники рядом +10% урона', 'guard_aura': 'аура: союзники рядом −10% получаемого урона',
    'mana_aura': 'аура: союзники рядом восполняют ману', 'focus_aura': 'аура: союзники рядом −10% перезарядки',
}
PROFS = ('miner', 'blacksmith', 'alchemist', 'hunter', 'herbalist')
FACTIONS = ('kingdom', 'goblins', 'piglins', 'illagers', 'undead', 'wilds', 'elves', 'dwarves', 'orcs', 'beastkin', 'demonkin')


class Klass:
    def __init__(self, key, ru, en, role, color, primary, secondary, styles, skills, trait, desc, races='', profs='',
                 faction=None, hidden=None, hint='', invoke=None):
        if role not in ROLES:
            raise ValueError(f'{key}: role {role}')
        self.key, self.ru, self.en, self.role, self.color = key, ru, en, role, color
        self.primary, self.secondary = STATS[primary], STATS[secondary]
        sw = styles.split()
        self.styles = {sw[i]: int(sw[i + 1]) for i in range(0, len(sw), 2)}
        for s in self.styles:
            if s not in STYLE_KEYS:
                raise ValueError(f'{key}: style {s}')
        self.skills = skills.split()
        self.trait_name, tb, tp = trait[0], trait[1], trait[2] if len(trait) > 2 else ''
        tw = tb.split()
        self.trait_bonus = {tw[i]: float(tw[i + 1]) for i in range(0, len(tw), 2)}
        for b in self.trait_bonus:
            if b not in BONUS:
                raise ValueError(f'{key}: bonus {b}')
        self.trait_passives = tp.split()
        for p in self.trait_passives:
            if p not in PASSIVES:
                raise ValueError(f'{key}: passive {p}')
        self.desc = desc
        self.races = races.split()
        self.profs = profs.split()
        self.faction = faction
        for x in self.races:
            if x not in RACES:
                raise ValueError(f'{key}: race {x}')
        for x in self.profs:
            if x not in PROFS:
                raise ValueError(f'{key}: prof {x}')
        if faction and faction not in FACTIONS:
            raise ValueError(f'{key}: faction {faction}')
        self.hidden = hidden  # список условий 'counter:undead>=60'
        self.hint = hint      # как открыть скрытый класс (по-русски)
        self.invoke = invoke

    def trait_text(self):
        parts = []
        for b, v in self.trait_bonus.items():
            t, pct, grows = BONUS[b]
            parts.append(f"{'+' if v >= 0 else '−'}{_num(abs(v))}{'%' if pct else ''} {t}" + (' (растёт с уровнем)' if grows else ''))
        parts += [PASSIVES[p] for p in self.trait_passives]
        return ', '.join(parts)

    def json(self, unlock):
        o = {'key': self.key, 'color': self.color, 'role': self.role, 'primary': self.primary, 'secondary': self.secondary,
             'styles': self.styles, 'skills': self.skills, 'unlock': unlock[:len(self.skills)],
             'trait': {'bonus': self.trait_bonus, 'passives': self.trait_passives}}
        if self.races:
            o['races'] = self.races
        if self.profs:
            o['profs'] = self.profs
        if self.faction:
            o['faction'] = self.faction
        if self.hidden:
            o['hidden'] = self.hidden
        if self.invoke:
            o['invoke'] = self.invoke
        return o


def C(key, ru, en, role, color, primary, secondary, styles, skills, trait, desc, **kw):
    if key in CLASSES:
        raise ValueError('duplicate class ' + key)
    c = Klass(key, ru, en, role, color, primary, secondary, styles, skills, trait, desc, **kw)
    CLASSES[key] = c
    ORDER.append(key)
    return c
