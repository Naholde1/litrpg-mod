"""
Заклинатели сфер (механика Инвокера): три сферы (Z, X, C) — над игроком держатся последние три
и дают усиления; Призыв (V) сплетает их в одно из десяти заклинаний, оно ложится на B, прежнее — на G.
Сочетания задаются номерами сфер по возрастанию: '012' — по одной каждой.
"""
from .dsl import K, C

ORDER_LEVELS = [10, 10, 10, 12, 15, 18, 21, 25, 30, 35]


def invoke(spheres, cast, combos):
    """combos: [(шаблон, навык)] в порядке открытия."""
    return {'spheres': spheres, 'cast': cast,
            'combos': [[p, k, ORDER_LEVELS[i]] for i, (p, k) in enumerate(combos)]}


# ===== 1. Призыватель стихий: Квас (лёд), Векс (буря), Экзорт (пламя) =====
K('orb_quas', 'Квас', 'Quas', 'ORB', '❄', m=0, cd=0, el='FROST', me='regen 3 0', f='Сфера льда: регенерация')
K('orb_wex', 'Векс', 'Wex', 'ORB', 'ϟ', m=0, cd=0, el='LIGHTNING', me='spd 3 0', f='Сфера бури: скорость')
K('orb_exort', 'Экзорт', 'Exort', 'ORB', '☀', m=0, cd=0, el='FIRE', me='str 3 0', f='Сфера пламени: сила')
K('invoke', 'Призыв', 'Invoke', 'INVOKE', '✪', m=10, cd=3, f='Сплетает три сферы')
K('cold_snap', 'Холодная хватка', 'Cold Snap', 'STUN', '❄', m=25, cd=14, el='FROST', pw=0.8, r=20, d=3, f='Квас-Квас-Квас: лёд сковывает цель')
K('ghost_walk', 'Призрачная поступь', 'Ghost Walk', 'STEALTH', '◌', m=25, cd=20, d=10, r=6, foe='slow 4 2', me='regen 10 0', f='Квас-Квас-Векс: невидимость, враги вокруг вязнут')
K('invoker_ice_wall', 'Стена льда', 'Ice Wall', 'WALL', '▦', m=30, cd=20, r=9, d=10, wall='minecraft:packed_ice', f='Квас-Квас-Экзорт: стена вечного льда')
K('tornado', 'Торнадо', 'Tornado', 'LAUNCH', '@', m=30, cd=16, el='LIGHTNING', pw=0.8, r=7, f='Квас-Векс-Векс: смерч подбрасывает врагов')
K('deafening_blast', 'Оглушающая волна', 'Deafening Blast', 'PUSH', '≫', m=35, cd=20, el='ARCANE', pw=1.2, r=9, foe='weak 4 2', f='Квас-Векс-Экзорт: волна всех трёх стихий')
K('forge_spirit', 'Дух кузни', 'Forge Spirit', 'SUMMON', '♨', m=40, cd=30, sm='minecraft:blaze', n=2, d=40, f='Квас-Экзорт-Экзорт: огненные духи')
K('emp', 'Электромагнитный импульс', 'EMP', 'ZONE_AT', '⚡', m=40, cd=20, el='LIGHTNING', pw=1.0, r=5, d=3, f='Векс-Векс-Векс: разряды бьют в области')
K('alacrity', 'Живость', 'Alacrity', 'BUFF_ALLIES', '⇉', m=30, cd=20, r=10, me='haste 12 2; str 12 1', f='Векс-Векс-Экзорт: скорость и сила союзникам')
K('chaos_meteor', 'Хаотический метеор', 'Chaos Meteor', 'METEOR', '☄', m=60, cd=40, el='FIRE', pw=3.0, r=6, f='Векс-Экзорт-Экзорт: метеор катится с неба')
K('sun_strike', 'Солнечный удар', 'Sun Strike', 'LIGHTNING', '☀', m=45, cd=25, el='FIRE', pw=3.2, r=60, f='Экзорт-Экзорт-Экзорт: столп солнца в цель')
C('invoker', 'Призыватель стихий', 'Invoker', 'DAMAGE', 0xF5B041, 'INT', 'INT', 'magic 4 explore 1 solo 1', '',
  ('Безграничное знание', 'mana 40 spell_power 8 cooldown 5'),
  'Заклинатель сфер: Квас, Векс и Экзорт сплетаются в десять заклинаний — от ледяной хватки до солнечного удара.',
  races='human elf',
  invoke=invoke(['orb_quas', 'orb_wex', 'orb_exort'], 'invoke', [
      ('000', 'cold_snap'), ('111', 'emp'), ('222', 'sun_strike'), ('001', 'ghost_walk'), ('011', 'tornado'),
      ('002', 'invoker_ice_wall'), ('112', 'alacrity'), ('022', 'forge_spirit'), ('122', 'chaos_meteor'), ('012', 'deafening_blast')]))

# ===== 2. Рунный ткач (танк): Камень, Кровь, Сталь =====
K('orb_stone', 'Сфера камня', 'Stone Orb', 'ORB', '◼', m=0, cd=0, el='EARTH', me='res 3 0', f='Сфера камня: сопротивление')
K('orb_blood', 'Сфера крови', 'Blood Orb', 'ORB', '♦', m=0, cd=0, el='SHADOW', me='regen 3 0', f='Сфера крови: регенерация')
K('orb_steel', 'Сфера стали', 'Steel Orb', 'ORB', '⚙', m=0, cd=0, el='ARCANE', me='haste 3 0', f='Сфера стали: скорость ударов')
K('rune_weave', 'Сплетение рун', 'Weave', 'INVOKE', 'ᛝ', m=10, cd=3, f='Три руны становятся одной')
K('stone_fortress', 'Каменная твердыня', 'Stone Fortress', 'BUFF_SELF', '▣', m=30, cd=30, me='res 10 2; abs 10 3', f='Камень-Камень-Камень: несокрушимость')
K('blood_reaping', 'Кровавый покос', 'Blood Reaping', 'NOVA', '✺', m=30, cd=12, el='SHADOW', st='VIT', pw=1.0, r=5, fl='steal perfoe', f='Кровь-Кровь-Кровь: чем больше врагов, тем больше крови')
K('steel_cyclone', 'Стальной вихрь', 'Steel Cyclone', 'MULTI_NOVA', '@', m=35, cd=14, st='STR', pw=0.5, r=4.5, n=6, f='Сталь-Сталь-Сталь: вихрь клинков')
K('stone_blood', 'Каменная кровь', 'Stoneblood', 'HEAL_SELF', '✚', m=30, cd=30, pw=0.4, me='res 6 1', f='Камень-Камень-Кровь: раны затягиваются коркой')
K('rune_bulwark', 'Рунный вал', 'Rune Bulwark', 'WALL', '▙', m=30, cd=25, r=9, d=12, wall='minecraft:mud_bricks', f='Камень-Камень-Сталь: стена встаёт из земли')
K('blood_call', 'Зов крови', 'Blood Call', 'TAUNT', '♁', m=25, cd=14, r=12, d=8, me='regen 8 1', f='Камень-Кровь-Кровь: враги чуют кровь и идут к ткачу')
K('triple_oath', 'Тройная клятва', 'Triple Oath', 'SHIELD', '⛉', m=30, cd=14, pw=2.0, r=14, me='res 6 1', f='Камень-Кровь-Сталь: щит раненому союзнику')
K('steel_rush', 'Стальной натиск', 'Steel Rush', 'DASH_STRIKE', '⇶', m=25, cd=10, st='STR', pw=1.4, r=3.5, foe='slow 3 3', f='Камень-Сталь-Сталь: таран сквозь строй')
K('blood_steel', 'Кровавая сталь', 'Bloodsteel', 'ON_HIT', '♰', m=20, cd=20, n=10, oh='LIFESTEAL', f='Кровь-Кровь-Сталь: удары пьют кровь')
K('steel_grip', 'Стальная хватка', 'Steel Grip', 'STUN', '✊', m=30, cd=16, st='STR', pw=1.0, r=12, d=3, f='Кровь-Сталь-Сталь: руна сжимает врага')
C('rune_weaver', 'Рунный ткач', 'Rune Weaver', 'TANK', 0x839192, 'VIT', 'STR', 'tank 3 magic 2 craft 1', '',
  ('Рунная кожа', 'armor 3 health 6'),
  'Заклинатель сфер-танк: руны Камня, Крови и Стали сплетаются в стены, щиты, вызовы и вихри.',
  races='dwarf orc',
  invoke=invoke(['orb_stone', 'orb_blood', 'orb_steel'], 'rune_weave', [
      ('000', 'stone_fortress'), ('111', 'blood_reaping'), ('222', 'steel_cyclone'), ('011', 'blood_call'), ('001', 'stone_blood'),
      ('002', 'rune_bulwark'), ('022', 'steel_rush'), ('112', 'blood_steel'), ('122', 'steel_grip'), ('012', 'triple_oath')]))

# ===== 3. Целитель сфер (поддержка): Жизнь, Свет, Вода =====
K('orb_life', 'Сфера жизни', 'Life Orb', 'ORB', '❀', m=0, cd=0, el='NATURE', me='regen 3 0', f='Сфера жизни: регенерация')
K('orb_light', 'Сфера света', 'Light Orb', 'ORB', '✧', m=0, cd=0, el='HOLY', me='res 3 0', f='Сфера света: сопротивление')
K('orb_water', 'Сфера воды', 'Water Orb', 'ORB', '≈', m=0, cd=0, el='FROST', me='spd 3 0', f='Сфера воды: скорость')
K('mend_weave', 'Сплетение жизни', 'Lifeweave', 'INVOKE', '✪', m=10, cd=3, f='Сферы сливаются в чудо')
K('bloom_of_life', 'Расцвет жизни', 'Bloom of Life', 'HEAL', '❀', m=40, cd=12, pw=1.8, r=12, f='Жизнь-Жизнь-Жизнь: исцеление всех вокруг')
K('radiance', 'Сияние', 'Radiance', 'PUSH', '✷', m=25, cd=12, el='HOLY', pw=0.8, r=8, foe='blind 3 0', f='Свет-Свет-Свет: вспышка слепит и отбрасывает')
K('purifying_tide', 'Очищающий поток', 'Purifying Tide', 'PURIFY', '♒', m=25, cd=14, el='HOLY', pw=0.5, r=10, f='Вода-Вода-Вода: смывает все хвори')
K('living_light', 'Живой свет', 'Living Light', 'TETHER', '☀', m=25, cd=10, el='HOLY', pw=1.5, r=18, d=10, f='Жизнь-Жизнь-Свет: луч лечит союзника')
K('spring_bloom', 'Живой родник', 'Living Spring', 'ZONE', '♒', m=40, cd=25, el='NATURE', pw=-0.45, r=7, d=10, f='Жизнь-Жизнь-Вода: родник лечит отряд')
K('light_shield', 'Щит света', 'Shield of Light', 'SHIELD', '◈', m=25, cd=10, pw=2.0, r=16, f='Жизнь-Свет-Свет: щит раненому')
K('rebirth_ward', 'Оберег возрождения', 'Rebirth Ward', 'WARD', '☥', m=50, cd=50, r=20, d=10, f='Жизнь-Свет-Вода: смерть отступает')
K('tide_of_life', 'Прилив жизни', 'Tide of Life', 'BUFF_ALLIES', '≋', m=35, cd=30, r=12, me='regen 12 1; spd 12 1; dolphin 12 0', f='Жизнь-Вода-Вода: волна бодрости')
K('shining_rain', 'Сияющий дождь', 'Shining Rain', 'ZONE_AT', '☂', m=45, cd=25, el='HOLY', pw=-0.5, r=7, d=8, f='Свет-Свет-Вода: дождь из света лечит в точке')
K('holy_torrent', 'Святой поток', 'Holy Torrent', 'BEAM', '⟿', m=35, cd=12, el='HOLY', pw=1.8, r=20, f='Свет-Вода-Вода: луч жжёт нежить и всех на пути')
C('orb_healer', 'Целитель сфер', 'Orb Healer', 'SUPPORT', 0x76D7C4, 'INT', 'VIT', 'support 4 magic 1 water 1', '',
  ('Три источника', 'mana_regen 20 mana 30'),
  'Заклинатель сфер-лекарь: Жизнь, Свет и Вода сплетаются в исцеления, щиты, обереги и очищение.',
  races='elf human',
  invoke=invoke(['orb_life', 'orb_light', 'orb_water'], 'mend_weave', [
      ('000', 'bloom_of_life'), ('111', 'radiance'), ('222', 'purifying_tide'), ('001', 'living_light'), ('002', 'spring_bloom'),
      ('011', 'light_shield'), ('022', 'tide_of_life'), ('112', 'shining_rain'), ('122', 'holy_torrent'), ('012', 'rebirth_ward')]))

# ===== 4. Чародей пустоты: Тень, Яд, Пустота =====
K('orb_shadow', 'Сфера тени', 'Shadow Orb', 'ORB', '◐', m=0, cd=0, el='SHADOW', me='spd 3 0', f='Сфера тени: скорость')
K('orb_venom', 'Сфера яда', 'Venom Orb', 'ORB', '☣', m=0, cd=0, el='POISON', me='regen 3 0', f='Сфера яда: регенерация')
K('orb_void', 'Сфера пустоты', 'Void Orb', 'ORB', '●', m=0, cd=0, el='ARCANE', me='res 3 0', f='Сфера пустоты: сопротивление')
K('void_weave', 'Сплетение бездны', 'Abyssal Weave', 'INVOKE', '✪', m=10, cd=3, f='Сферы тонут друг в друге')
K('umbral_step', 'Теневой прыжок', 'Umbral Step', 'SHADOW_STEP', '◐', m=20, cd=8, r=20, f='Тень-Тень-Тень: за спину цели')
K('plague_vortex', 'Чумной вихрь', 'Plague Vortex', 'ZONE_AT', '☣', m=35, cd=16, el='POISON', pw=0.6, r=5, d=8, foe='poison 4 1', f='Яд-Яд-Яд: ядовитый смерч')
K('void_rift', 'Разрыв пустоты', 'Void Rift', 'VORTEX', '●', m=50, cd=30, el='ARCANE', pw=0.5, r=7, d=4, f='Пустота-Пустота-Пустота: всё втягивается в разрыв')
K('venom_shade', 'Ядовитая тень', 'Venom Shade', 'STEALTH', '◌', m=25, cd=20, d=6, r=6, foe='poison 6 1; blind 3 0', f='Тень-Тень-Яд: исчезнуть в облаке яда')
K('hollow_shade', 'Тень пустоты', 'Hollow Shade', 'STASIS', '◘', m=50, cd=40, r=4, d=3, f='Тень-Тень-Пустота: враги застывают во тьме')
K('toxic_shadow_bolt', 'Токсичная стрела', 'Toxic Bolt', 'CHAIN', '≷', m=25, cd=8, el='POISON', pw=1.0, r=7, n=5, f='Тень-Яд-Яд: яд скачет по врагам')
K('triple_doom', 'Тройная погибель', 'Triple Doom', 'BEAM', '⟿', m=45, cd=16, el='SHADOW', pw=2.4, r=24, foe='wither 4 1; poison 4 1', f='Тень-Яд-Пустота: луч всех трёх сил')
K('consume', 'Поглощение', 'Consume', 'STEAL', '⊘', m=30, cd=14, el='ARCANE', pw=1.2, r=20, f='Тень-Пустота-Пустота: забрать силу врага')
K('venom_abyss', 'Ядовитая бездна', 'Venom Abyss', 'NOVA', '✺', m=35, cd=14, el='POISON', pw=1.2, r=6, foe='poison 6 2', f='Яд-Яд-Пустота: взрыв яда вокруг')
K('collapse', 'Коллапс', 'Collapse', 'METEOR', '◉', m=70, cd=50, el='SHADOW', pw=3.2, r=6, f='Яд-Пустота-Пустота: пустота обрушивается в точку')
C('void_invoker', 'Чародей пустоты', 'Void Invoker', 'DAMAGE', 0x512E5F, 'INT', 'AGI', 'magic 3 night 2 stealth 1', '',
  ('Шёпот бездны', 'spell_power 8 dodge 4'),
  'Заклинатель сфер тьмы: Тень, Яд и Пустота сплетаются в разрывы, коллапсы и ядовитые вихри.',
  races='demonkin elf',
  invoke=invoke(['orb_shadow', 'orb_venom', 'orb_void'], 'void_weave', [
      ('000', 'umbral_step'), ('111', 'plague_vortex'), ('222', 'void_rift'), ('001', 'venom_shade'), ('002', 'hollow_shade'),
      ('011', 'toxic_shadow_bolt'), ('112', 'venom_abyss'), ('022', 'consume'), ('122', 'collapse'), ('012', 'triple_doom')]))
