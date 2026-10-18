package com.andrey.litrpg.ability;

import com.andrey.litrpg.entity.ManaBoltEntity.Element;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Навык. Все навыки описаны данными (вид + параметры), а исполняет их
 * {@link AbilityHandler}. Классы ссылаются на навыки по ключу, один навык
 * может встречаться у нескольких классов.
 */
public final class Ability {
    /** Что делает навык. */
    public enum Kind {
        DASH,           // рывок без урона
        DASH_STRIKE,    // рывок с уроном по пути
        LEAP_SLAM,      // прыжок и удар при приземлении
        NOVA,           // волна вокруг себя
        MULTI_NOVA,     // серия волн (вихрь, танец клинков)
        STRIKE,         // удар по цели под прицелом
        MULTI_STRIKE,   // серия ударов по одной цели
        BOLT,           // магический снаряд
        CHAIN,          // цепная молния
        LIGHTNING,      // удар молнии по цели
        METEOR,         // падение с неба в точку прицела
        ZONE,           // зона вокруг себя на время
        ZONE_AT,        // зона в точке прицела
        ARROWS,         // стрелы веером
        BARRAGE,        // очередь стрел
        ARROW_RAIN,     // дождь стрел
        BUFF_SELF,
        BUFF_ALLIES,
        HEAL,           // лечение себя и союзников
        HEAL_SELF,      // процент от максимума здоровья
        MEDITATE,       // мана и здоровье
        TAUNT,          // агрессия врагов + баффы/дебаффы
        PULL,           // притянуть цель
        BLINK,
        SHADOW_STEP,
        STEALTH,
        ON_HIT,         // зарядить оружие эффектом на N ударов
        MARK,           // метка: цель получает больше урона
        PURIFY,
        SUMMON
    }

    /** Эффект при попадании для ON_HIT. */
    public enum OnHit { POISON, FIRE, FROST, LIFESTEAL, ARCANE, BLEED }

    public record Fx(MobEffect effect, int duration, int amplifier) {}

    private static final List<Ability> ALL = new ArrayList<>();
    private static final Map<String, Ability> BY_KEY = new HashMap<>();

    /** Навыки новичка (до выбора класса). */
    public static final String[] NOVICE = {"dash", "second_wind"};
    public static final int[] NOVICE_UNLOCK = {1, 5};
    /** Уровни открытия навыков класса по слотам. */
    public static final int[] UNLOCK = {10, 13, 17, 22, 28};
    public static final int SLOTS = 5;

    public final String key;
    public final Kind kind;
    public final String icon;
    private int index;
    int mana = 20;
    int cooldown = 100;
    Element element = null;
    Stat scale = Stat.STRENGTH;
    float power = 1f;
    float radius = 4f;
    int duration = 200;
    int count = 1;
    boolean execute = false;
    boolean backstab = false;
    boolean lifesteal = false;
    boolean pierce = false;
    boolean reflect = false;
    OnHit onHit = null;
    String summon = null;
    final List<Fx> self = new ArrayList<>();
    final List<Fx> enemy = new ArrayList<>();

    private Ability(String key, Kind kind, String icon) {
        this.key = key;
        this.kind = kind;
        this.icon = icon;
    }

    public int ordinal() { return index; }
    public int manaCost() { return mana; }
    public int cooldown() { return cooldown; }
    public String nameKey() { return "ability.litrpg." + key; }
    public String descKey() { return "ability.litrpg." + key + ".desc"; }

    public static Ability[] values() { return ALL.toArray(new Ability[0]); }
    public static int count() { return ALL.size(); }
    public static Ability byKey(String key) { return BY_KEY.get(key); }

    public static Ability byId(int id) {
        return id >= 0 && id < ALL.size() ? ALL.get(id) : null;
    }

    /** Навык в слоте: у новичка два навыка, у класса — пять. */
    public static Ability forSlot(PlayerClass c, int slot) {
        if (c == null) return slot < NOVICE.length ? byKey(NOVICE[slot]) : null;
        return slot < c.skills.length ? byKey(c.skills[slot]) : null;
    }

    public static int unlockLevel(PlayerClass c, int slot) {
        if (c == null) return slot < NOVICE_UNLOCK.length ? NOVICE_UNLOCK[slot] : 999;
        return UNLOCK[slot];
    }

    // ---------------- построитель ----------------

    private static Ability s(String key, Kind kind, String icon) {
        Ability a = new Ability(key, kind, icon);
        if (BY_KEY.put(key, a) != null) throw new IllegalStateException("duplicate skill " + key);
        a.index = ALL.size();
        ALL.add(a);
        return a;
    }

    Ability mana(int m) { mana = m; return this; }
    Ability cd(double seconds) { cooldown = (int) Math.round(seconds * 20); return this; }
    Ability el(Element e) { element = e; scale = Stat.INTELLIGENCE; return this; }
    Ability stat(Stat st) { scale = st; return this; }
    Ability pow(float p) { power = p; return this; }
    Ability rad(float r) { radius = r; return this; }
    Ability dur(double seconds) { duration = (int) Math.round(seconds * 20); return this; }
    Ability cnt(int c) { count = c; return this; }
    Ability exec() { execute = true; return this; }
    Ability back() { backstab = true; return this; }
    Ability steal() { lifesteal = true; return this; }
    Ability pierce() { pierce = true; return this; }
    Ability reflect() { reflect = true; return this; }
    Ability onHit(OnHit h) { onHit = h; return this; }
    Ability summon(String entity) { summon = entity; return this; }
    Ability me(MobEffect e, double seconds, int amp) { self.add(new Fx(e, (int) (seconds * 20), amp)); return this; }
    Ability foe(MobEffect e, double seconds, int amp) { enemy.add(new Fx(e, (int) (seconds * 20), amp)); return this; }

    // ---------------- все навыки ----------------

    static {
        final Stat STR = Stat.STRENGTH, AGI = Stat.AGILITY, INT = Stat.INTELLIGENCE, VIT = Stat.VITALITY;
        final MobEffect STRONG = MobEffects.DAMAGE_BOOST, RES = MobEffects.DAMAGE_RESISTANCE, SPEED = MobEffects.MOVEMENT_SPEED,
                SLOW = MobEffects.MOVEMENT_SLOWDOWN, WEAK = MobEffects.WEAKNESS, REGEN = MobEffects.REGENERATION,
                ABSORB = MobEffects.ABSORPTION, HASTE = MobEffects.DIG_SPEED, BLIND = MobEffects.BLINDNESS,
                WITHER = MobEffects.WITHER, POISON = MobEffects.POISON, HP = MobEffects.HEALTH_BOOST, LUCK = MobEffects.LUCK,
                FIRE_RES = MobEffects.FIRE_RESISTANCE, NAUSEA = MobEffects.CONFUSION, GLOW = MobEffects.GLOWING;

        // --- новичок ---
        s("dash", Kind.DASH, "»").mana(10).cd(4).rad(1.6f);
        s("second_wind", Kind.HEAL_SELF, "♥").mana(15).cd(40).pow(0.3f).me(REGEN, 5, 0);

        // --- Воин ---
        s("charge", Kind.DASH_STRIKE, "»").mana(15).cd(6).pow(1.0f).rad(2.2f).foe(SLOW, 2, 3);
        s("war_cry", Kind.TAUNT, "!").mana(25).cd(20).rad(8).dur(10).me(STRONG, 10, 0).me(RES, 10, 0).foe(WEAK, 8, 0);
        s("slam", Kind.NOVA, "✸").mana(35).cd(10).pow(1.4f).rad(5);
        s("iron_skin", Kind.BUFF_SELF, "▣").mana(40).cd(30).me(RES, 8, 1).me(ABSORB, 8, 2);
        s("berserk", Kind.BUFF_SELF, "⚔").mana(60).cd(60).me(STRONG, 12, 2).me(SPEED, 12, 1).me(HASTE, 12, 1).me(REGEN, 6, 0);
        // --- Берсерк ---
        s("blood_rage", Kind.ON_HIT, "♨").mana(25).cd(20).cnt(10).onHit(OnHit.LIFESTEAL).me(STRONG, 10, 0);
        s("whirlwind", Kind.MULTI_NOVA, "@").mana(40).cd(14).pow(0.5f).rad(3.5f).cnt(6);
        s("rage_leap", Kind.LEAP_SLAM, "⇑").mana(35).cd(12).pow(1.5f).rad(4);
        s("execute", Kind.STRIKE, "☩").mana(30).cd(10).pow(1.6f).rad(4.5f).exec();
        s("frenzy", Kind.BUFF_SELF, "☠").mana(60).cd(60).me(STRONG, 15, 1).me(HASTE, 15, 2).me(SPEED, 15, 1).me(REGEN, 8, 1);
        // --- Страж ---
        s("shield_bash", Kind.DASH_STRIKE, "◘").mana(20).cd(8).pow(0.8f).rad(2.0f).foe(SLOW, 3, 5).foe(WEAK, 4, 1);
        s("taunt", Kind.TAUNT, "‼").mana(20).cd(12).rad(12).dur(6).me(RES, 6, 0);
        s("shield_wall", Kind.BUFF_ALLIES, "▦").mana(45).cd(40).rad(8).me(RES, 10, 1).me(ABSORB, 10, 2);
        s("quake", Kind.NOVA, "≋").mana(35).cd(14).pow(0.8f).rad(6).foe(SLOW, 4, 3);
        s("last_stand", Kind.BUFF_SELF, "♜").mana(60).cd(90).me(RES, 10, 2).me(ABSORB, 10, 4).me(REGEN, 10, 1);
        // --- Паладин ---
        s("holy_strike", Kind.STRIKE, "✝").mana(20).cd(6).el(Element.HOLY).stat(STR).pow(1.3f).rad(4.5f);
        s("aura_of_protection", Kind.BUFF_ALLIES, "◇").mana(35).cd(45).rad(10).me(RES, 20, 0).me(REGEN, 20, 0);
        s("holy_light", Kind.HEAL, "✚").mana(30).cd(12).pow(1.0f).rad(6);
        s("consecration", Kind.ZONE, "۞").mana(45).cd(25).el(Element.HOLY).pow(0.35f).rad(5).dur(10);
        s("divine_shield", Kind.BUFF_SELF, "◈").mana(50).cd(90).me(RES, 4, 3).me(REGEN, 6, 2);
        // --- Рыцарь смерти ---
        s("frost_strike", Kind.STRIKE, "❅").mana(20).cd(6).el(Element.FROST).stat(STR).pow(1.3f).rad(4.5f).foe(SLOW, 3, 2);
        s("death_grip", Kind.PULL, "↩").mana(20).cd(12).rad(20).foe(SLOW, 2, 3);
        s("aura_of_decay", Kind.ZONE, "☣").mana(40).cd(25).el(Element.SHADOW).stat(STR).pow(0.35f).rad(5).dur(10).foe(WITHER, 2, 0);
        s("raise_dead", Kind.SUMMON, "☥").mana(45).cd(40).summon("minecraft:zombie").cnt(1).dur(60);
        s("army_of_darkness", Kind.SUMMON, "☠").mana(90).cd(120).summon("minecraft:skeleton").cnt(3).dur(45);
        // --- Гладиатор ---
        s("net_throw", Kind.STRIKE, "#").mana(15).cd(10).pow(0.4f).rad(12).foe(SLOW, 3, 6);
        s("lunge", Kind.DASH_STRIKE, "➶").mana(15).cd(6).pow(1.2f).rad(1.8f);
        s("parry", Kind.BUFF_SELF, "⛨").mana(25).cd(15).dur(4).me(RES, 4, 1).reflect();
        s("hamstring", Kind.STRIKE, "⌁").mana(20).cd(8).pow(1.0f).rad(4.5f).foe(SLOW, 5, 3).foe(WEAK, 5, 0);
        s("arena_fury", Kind.BUFF_SELF, "♛").mana(55).cd(60).me(STRONG, 12, 1).me(SPEED, 12, 0).me(REGEN, 12, 0).me(ABSORB, 12, 2);
        // --- Варвар ---
        s("throwing_axe", Kind.ARROWS, "⚒").mana(15).cd(5).stat(STR).pow(2.2f).cnt(1);
        s("roar", Kind.TAUNT, "Ж").mana(25).cd(18).rad(8).dur(8).foe(WEAK, 6, 1).foe(SLOW, 4, 1);
        s("ancestral_spirit", Kind.BUFF_SELF, "Ψ").mana(55).cd(60).me(STRONG, 15, 1).me(RES, 15, 0).me(HP, 15, 2);
        s("bloodletting", Kind.ON_HIT, "♦").mana(25).cd(20).cnt(8).onHit(OnHit.BLEED);
        // --- Мастер клинка ---
        s("iai", Kind.DASH_STRIKE, "—").mana(25).cd(8).stat(AGI).pow(2.0f).rad(2.5f);
        s("stance", Kind.BUFF_SELF, "☯").mana(25).cd(30).me(SPEED, 15, 0).me(RES, 15, 0).me(LUCK, 15, 1);
        s("wind_dance", Kind.MULTI_NOVA, "≈").mana(45).cd(16).stat(AGI).pow(0.45f).rad(4).cnt(8);
        s("sever", Kind.STRIKE, "⟋").mana(25).cd(9).stat(AGI).pow(1.4f).rad(4.5f).exec();
        s("thousand_cuts", Kind.MULTI_STRIKE, "⁂").mana(60).cd(40).stat(AGI).pow(0.35f).rad(5).cnt(10);
        // --- Монах ---
        s("palm_strike", Kind.STRIKE, "✋").mana(15).cd(5).pow(1.2f).rad(4.5f).foe(SLOW, 1, 2);
        s("meditation", Kind.MEDITATE, "☸").mana(0).cd(45).pow(0.4f);
        s("swift_step", Kind.BLINK, "⇢").mana(15).cd(8).rad(6).me(SPEED, 3, 1);
        s("chi_wave", Kind.NOVA, "◎").mana(30).cd(10).el(Element.ARCANE).stat(STR).pow(1.0f).rad(5);
        s("hundred_fists", Kind.MULTI_STRIKE, "✊").mana(55).cd(35).pow(0.3f).rad(5).cnt(12);
        // --- Разбойник ---
        s("eviscerate", Kind.STRIKE, "✂").mana(15).cd(4).stat(AGI).pow(1.2f).rad(4.5f).back();
        s("shadow_step", Kind.SHADOW_STEP, "◐").mana(25).cd(10).rad(18);
        s("poison_blade", Kind.ON_HIT, "♆").mana(25).cd(20).cnt(6).onHit(OnHit.POISON);
        s("smoke_bomb", Kind.STEALTH, "☁").mana(35).cd(25).dur(6).rad(8).foe(BLIND, 5, 0).me(SPEED, 6, 1);
        s("blade_dance", Kind.MULTI_NOVA, "✺").mana(60).cd(45).stat(AGI).pow(0.45f).rad(4).cnt(8);
        // --- Ассасин ---
        s("death_mark", Kind.MARK, "⊕").mana(20).cd(15).rad(40).dur(15);
        s("vanish", Kind.STEALTH, "◌").mana(40).cd(30).dur(10).rad(0).me(SPEED, 10, 1);
        s("assassinate", Kind.STRIKE, "†").mana(45).cd(20).stat(AGI).pow(2.2f).rad(4.5f).exec().back();
        // --- Дуэлянт ---
        s("feint", Kind.BUFF_SELF, "↯").mana(15).cd(12).me(SPEED, 4, 2).me(RES, 2, 1);
        s("riposte", Kind.BUFF_SELF, "⇌").mana(25).cd(16).dur(4).me(RES, 4, 0).reflect();
        s("flurry", Kind.MULTI_STRIKE, "⋰").mana(30).cd(12).stat(AGI).pow(0.5f).rad(5).cnt(6);
        s("heart_thrust", Kind.STRIKE, "♡").mana(40).cd(18).stat(AGI).pow(2.0f).rad(4.5f).back();
        // --- Теневой клинок ---
        s("shuriken", Kind.ARROWS, "✴").mana(12).cd(3).stat(AGI).pow(0.9f).cnt(3);
        s("shadow_clone", Kind.STEALTH, "♊").mana(30).cd(25).dur(5).rad(0).me(SPEED, 5, 2);
        s("shadow_burst", Kind.NOVA, "✹").mana(35).cd(12).el(Element.SHADOW).stat(AGI).pow(1.2f).rad(5);
        s("dance_of_shadows", Kind.MULTI_NOVA, "◑").mana(60).cd(45).el(Element.SHADOW).stat(AGI).pow(0.5f).rad(4).cnt(8);
        // --- Следопыт ---
        s("volley", Kind.ARROWS, "⋙").mana(15).cd(3).stat(AGI).pow(1.0f).cnt(5);
        s("poison_arrow", Kind.ARROWS, "☠").mana(20).cd(7).stat(AGI).pow(1.5f).cnt(1).foe(POISON, 6, 1).foe(SLOW, 5, 1);
        s("leap_back", Kind.DASH, "«").mana(15).cd(8).rad(-1.6f).foe(SLOW, 4, 3);
        s("hunters_mark", Kind.MARK, "◎").mana(25).cd(15).rad(40).dur(15);
        s("arrow_rain", Kind.ARROW_RAIN, "☂").mana(70).cd(45).stat(AGI).pow(1.0f).rad(4.5f).dur(3);
        // --- Снайпер ---
        s("aimed_shot", Kind.ARROWS, "⌖").mana(20).cd(6).stat(AGI).pow(3.0f).cnt(1);
        s("piercing_arrow", Kind.ARROWS, "➵").mana(25).cd(8).stat(AGI).pow(2.0f).cnt(1).pierce();
        s("camouflage", Kind.STEALTH, "♣").mana(30).cd(30).dur(8).rad(0);
        s("explosive_arrow", Kind.BOLT, "✷").mana(35).cd(10).el(Element.FIRE).stat(AGI).pow(1.6f);
        s("headshot", Kind.STRIKE, "⊗").mana(50).cd(25).stat(AGI).pow(2.5f).rad(48).exec();
        // --- Повелитель зверей ---
        s("call_wolf", Kind.SUMMON, "♞").mana(30).cd(30).summon("minecraft:wolf").cnt(1).dur(120);
        s("pack", Kind.SUMMON, "♘").mana(70).cd(90).summon("minecraft:wolf").cnt(3).dur(60);
        s("beast_fury", Kind.BUFF_ALLIES, "Ѫ").mana(40).cd(40).rad(12).me(STRONG, 12, 1).me(SPEED, 12, 1);
        // --- Арбалетчик ---
        s("heavy_bolt", Kind.ARROWS, "➽").mana(20).cd(6).stat(AGI).pow(2.4f).cnt(1).pierce();
        s("buckshot", Kind.ARROWS, "⁘").mana(25).cd(8).stat(AGI).pow(0.6f).cnt(8);
        s("bear_trap", Kind.ZONE_AT, "⊓").mana(25).cd(15).pow(0.5f).rad(2.5f).dur(10).foe(SLOW, 3, 5);
        s("barrage", Kind.BARRAGE, "⫸").mana(55).cd(35).stat(AGI).pow(0.8f).cnt(12);
        // --- Маг ---
        s("mana_bolt", Kind.BOLT, "✦").mana(12).cd(1).el(Element.ARCANE).pow(1.0f);
        s("fireball", Kind.BOLT, "☀").mana(30).cd(5).el(Element.FIRE).pow(1.6f);
        s("frost_nova", Kind.NOVA, "❄").mana(40).cd(12).el(Element.FROST).pow(0.9f).rad(6).foe(SLOW, 5, 4).foe(WEAK, 5, 1);
        s("blink", Kind.BLINK, "⇢").mana(25).cd(8).rad(10);
        s("meteor", Kind.METEOR, "☄").mana(90).cd(45).el(Element.FIRE).pow(3.0f).rad(5);
        // --- Пиромант ---
        s("fire_arrow", Kind.BOLT, "♨").mana(14).cd(1.5).el(Element.FIRE).pow(1.1f);
        s("ring_of_fire", Kind.NOVA, "◯").mana(40).cd(12).el(Element.FIRE).pow(1.2f).rad(6);
        s("fire_field", Kind.ZONE_AT, "♒").mana(45).cd(20).el(Element.FIRE).pow(0.4f).rad(4).dur(8);
        // --- Криомант ---
        s("ice_shard", Kind.BOLT, "◆").mana(14).cd(1.5).el(Element.FROST).pow(1.1f);
        s("ice_armor", Kind.BUFF_SELF, "❆").mana(40).cd(35).dur(10).me(RES, 10, 1).me(ABSORB, 10, 3).reflect();
        s("blizzard", Kind.ZONE_AT, "❄").mana(60).cd(30).el(Element.FROST).pow(0.4f).rad(6).dur(10).foe(SLOW, 2, 3);
        s("ice_prison", Kind.STRIKE, "▢").mana(30).cd(15).el(Element.FROST).pow(1.0f).rad(24).foe(SLOW, 4, 9).foe(WEAK, 4, 2);
        // --- Повелитель бурь ---
        s("spark", Kind.BOLT, "ϟ").mana(12).cd(1).el(Element.LIGHTNING).pow(0.9f);
        s("chain_lightning", Kind.CHAIN, "≷").mana(35).cd(8).el(Element.LIGHTNING).pow(1.2f).cnt(5).rad(7);
        s("lightning_strike", Kind.LIGHTNING, "⚡").mana(40).cd(10).el(Element.LIGHTNING).pow(2.0f).rad(32);
        s("thunderstorm", Kind.ZONE_AT, "☈").mana(90).cd(50).el(Element.LIGHTNING).pow(1.0f).rad(7).dur(10);
        // --- Некромант ---
        s("shadow_bolt", Kind.BOLT, "●").mana(14).cd(1.5).el(Element.SHADOW).pow(1.1f);
        s("raise_skeleton", Kind.SUMMON, "☥").mana(40).cd(30).summon("minecraft:skeleton").cnt(1).dur(60);
        s("drain_life", Kind.STRIKE, "♁").mana(30).cd(8).el(Element.SHADOW).pow(1.2f).rad(20).steal();
        s("plague_cloud", Kind.ZONE_AT, "☣").mana(45).cd(25).el(Element.POISON).pow(0.3f).rad(5).dur(10).foe(POISON, 3, 1);
        s("army_of_the_dead", Kind.SUMMON, "⚰").mana(100).cd(120).summon("minecraft:zombie").cnt(4).dur(45);
        // --- Чернокнижник ---
        s("curse", Kind.STRIKE, "⛧").mana(20).cd(10).el(Element.SHADOW).pow(0.6f).rad(24).foe(WEAK, 8, 1).foe(WITHER, 6, 1).foe(SLOW, 6, 1);
        s("chaos_bolt", Kind.BOLT, "✪").mana(40).cd(7).el(Element.SHADOW).pow(2.0f);
        s("summon_imp", Kind.SUMMON, "♅").mana(50).cd(45).summon("minecraft:vex").cnt(2).dur(40);
        s("soul_siphon", Kind.STRIKE, "♄").mana(25).cd(12).el(Element.SHADOW).pow(1.0f).rad(20).steal();
        s("gates_of_darkness", Kind.METEOR, "⛬").mana(90).cd(50).el(Element.SHADOW).pow(2.8f).rad(6);
        // --- Друид ---
        s("thorn_bolt", Kind.BOLT, "❦").mana(14).cd(1.5).el(Element.NATURE).pow(1.0f);
        s("healing_bloom", Kind.ZONE, "❀").mana(40).cd(30).el(Element.HOLY).pow(-0.15f).rad(6).dur(8);
        s("bear_form", Kind.BUFF_SELF, "ʘ").mana(50).cd(60).me(STRONG, 20, 1).me(RES, 20, 1).me(HP, 20, 4).me(REGEN, 20, 0);
        s("entangle", Kind.NOVA, "⌘").mana(30).cd(15).el(Element.NATURE).pow(0.4f).rad(6).foe(SLOW, 4, 6);
        s("wrath_of_nature", Kind.ZONE_AT, "♣").mana(80).cd(45).el(Element.NATURE).pow(0.6f).rad(6).dur(10);
        // --- Шаман ---
        s("spirit_lightning", Kind.BOLT, "ϟ").mana(14).cd(1.5).el(Element.LIGHTNING).pow(1.0f);
        s("healing_totem", Kind.ZONE, "♇").mana(45).cd(35).el(Element.HOLY).pow(-0.12f).rad(7).dur(12);
        s("fury_totem", Kind.BUFF_ALLIES, "♈").mana(40).cd(40).rad(10).me(STRONG, 15, 0).me(HASTE, 15, 1);
        s("spirit_wolf", Kind.SUMMON, "♞").mana(50).cd(60).summon("minecraft:wolf").cnt(2).dur(60);
        // --- Геомант ---
        s("stone_bolt", Kind.BOLT, "⬢").mana(16).cd(2).el(Element.EARTH).pow(1.4f);
        s("stoneskin", Kind.BUFF_SELF, "▩").mana(40).cd(35).me(RES, 12, 1).me(ABSORB, 12, 3).me(HP, 12, 2);
        s("earth_spikes", Kind.ZONE_AT, "▲").mana(45).cd(20).el(Element.EARTH).pow(0.6f).rad(4).dur(5).foe(SLOW, 2, 2);
        s("rock_fall", Kind.METEOR, "◍").mana(80).cd(45).el(Element.EARTH).pow(2.8f).rad(5);
        // --- Боевой маг ---
        s("enchanted_blade", Kind.ON_HIT, "✧").mana(25).cd(20).cnt(10).onHit(OnHit.ARCANE);
        s("mana_strike", Kind.STRIKE, "✶").mana(25).cd(6).el(Element.ARCANE).pow(1.5f).rad(5);
        s("arcane_shield", Kind.BUFF_SELF, "◉").mana(40).cd(35).me(ABSORB, 10, 4).me(RES, 10, 0);
        s("blade_storm", Kind.MULTI_NOVA, "✵").mana(60).cd(40).el(Element.ARCANE).pow(0.5f).rad(4).cnt(8);
        // --- Жрец ---
        s("heal", Kind.HEAL, "✚").mana(20).cd(5).pow(0.8f).rad(6);
        s("smite", Kind.STRIKE, "⚡").mana(25).cd(4).el(Element.HOLY).pow(1.1f).rad(24);
        s("blessing", Kind.BUFF_ALLIES, "✧").mana(40).cd(25).rad(10).me(REGEN, 15, 0).me(ABSORB, 15, 1).me(LUCK, 15, 0);
        s("purify", Kind.PURIFY, "❖").mana(30).cd(20).el(Element.HOLY).pow(0.5f).rad(8);
        s("sanctuary", Kind.ZONE, "۞").mana(80).cd(60).el(Element.HOLY).pow(0.3f).rad(7).dur(10);
        // --- Бард ---
        s("song_of_valor", Kind.BUFF_ALLIES, "♪").mana(30).cd(30).rad(12).me(STRONG, 20, 0).me(SPEED, 20, 0);
        s("lullaby", Kind.NOVA, "♫").mana(30).cd(18).pow(0f).rad(8).foe(SLOW, 5, 4).foe(WEAK, 5, 2).foe(BLIND, 3, 0);
        s("song_of_healing", Kind.ZONE, "♬").mana(45).cd(35).el(Element.HOLY).pow(-0.12f).rad(8).dur(10);
        s("dissonance", Kind.NOVA, "♭").mana(35).cd(12).el(Element.ARCANE).pow(1.0f).rad(6).foe(NAUSEA, 5, 0);
        s("ballad_of_heroes", Kind.BUFF_ALLIES, "♔").mana(70).cd(75).rad(12).me(STRONG, 20, 1).me(RES, 20, 0).me(REGEN, 20, 0).me(ABSORB, 20, 2);
        // --- Инквизитор ---
        s("chains_of_light", Kind.STRIKE, "⛓").mana(25).cd(12).el(Element.HOLY).pow(0.8f).rad(20).foe(SLOW, 3, 7).foe(GLOW, 6, 0);
        s("holy_fire", Kind.ZONE_AT, "♆").mana(45).cd(22).el(Element.HOLY).pow(0.45f).rad(5).dur(8);
        s("judgment", Kind.METEOR, "⚖").mana(90).cd(50).el(Element.HOLY).pow(2.8f).rad(5);
        // --- Алхимик ---
        s("acid_flask", Kind.BOLT, "⚗").mana(15).cd(2).el(Element.POISON).pow(1.0f);
        s("fire_flask", Kind.ZONE_AT, "⚱").mana(30).cd(12).el(Element.FIRE).pow(0.4f).rad(3.5f).dur(6);
        s("elixir_of_might", Kind.BUFF_SELF, "⚕").mana(35).cd(40).me(STRONG, 20, 1).me(SPEED, 20, 0).me(HASTE, 20, 1).me(FIRE_RES, 20, 0);
        s("healing_flask", Kind.HEAL, "⚘").mana(25).cd(10).pow(0.9f).rad(6);
        s("grand_transmutation", Kind.BUFF_ALLIES, "⚛").mana(80).cd(90).rad(12).me(STRONG, 25, 0).me(RES, 25, 0).me(REGEN, 25, 1).me(LUCK, 25, 1);
    }
}
