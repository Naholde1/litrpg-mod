package com.andrey.litrpg.stats;

import java.util.EnumMap;
import java.util.Map;

import static com.andrey.litrpg.stats.Stat.*;
import static com.andrey.litrpg.stats.Style.*;

/**
 * Классы. Выбираются на 10 уровне из трёх, которые Система подобрала
 * по стилю игры. Скрытые классы предлагаются только при особом условии.
 */
public enum PlayerClass {
    // ---- ближний бой ----
    WARRIOR("warrior", 0xD9534F, STRENGTH, VITALITY, w(MELEE, 3, TANK, 2),
            "charge", "war_cry", "slam", "iron_skin", "berserk"),
    BERSERKER("berserker", 0xC0392B, STRENGTH, STRENGTH, w(MELEE, 4, FIRE, 1),
            "blood_rage", "whirlwind", "rage_leap", "execute", "frenzy"),
    GUARDIAN("guardian", 0x7F8C8D, VITALITY, STRENGTH, w(TANK, 4, MELEE, 1),
            "shield_bash", "taunt", "shield_wall", "quake", "last_stand"),
    PALADIN("paladin", 0xF4D03F, STRENGTH, VITALITY, w(MELEE, 2, TANK, 2, SUPPORT, 2, UNDEAD, 1),
            "holy_strike", "aura_of_protection", "holy_light", "consecration", "divine_shield"),
    DEATH_KNIGHT("death_knight", 0x5D6D7E, STRENGTH, VITALITY, w(MELEE, 2, UNDEAD, 3, TANK, 1),
            "frost_strike", "death_grip", "aura_of_decay", "raise_dead", "army_of_darkness"),
    GLADIATOR("gladiator", 0xCA6F1E, STRENGTH, AGILITY, w(MELEE, 3, TANK, 1, STEALTH, 1),
            "net_throw", "lunge", "parry", "hamstring", "arena_fury"),
    BARBARIAN("barbarian", 0xA04000, STRENGTH, VITALITY, w(MELEE, 3, NATURE, 2),
            "throwing_axe", "roar", "rage_leap", "bloodletting", "ancestral_spirit"),
    BLADEMASTER("blademaster", 0xE5E8E8, AGILITY, STRENGTH, w(MELEE, 2, STEALTH, 2),
            "iai", "stance", "wind_dance", "sever", "thousand_cuts"),
    MONK("monk", 0xF5B041, AGILITY, VITALITY, w(MELEE, 2, SUPPORT, 1, STEALTH, 1),
            "palm_strike", "meditation", "swift_step", "chi_wave", "hundred_fists"),
    // ---- ловкость и скрытность ----
    ROGUE("rogue", 0x9B6BD6, AGILITY, STRENGTH, w(STEALTH, 3, MELEE, 1),
            "eviscerate", "shadow_step", "poison_blade", "smoke_bomb", "blade_dance"),
    ASSASSIN("assassin", 0x4A235A, AGILITY, LUCK, w(STEALTH, 4),
            "death_mark", "poison_blade", "shadow_step", "vanish", "assassinate"),
    DUELIST("duelist", 0xE74C3C, AGILITY, LUCK, w(STEALTH, 2, MELEE, 2),
            "lunge", "feint", "riposte", "flurry", "heart_thrust"),
    SHADOWBLADE("shadowblade", 0x2C3E50, AGILITY, INTELLIGENCE, w(STEALTH, 3, MAGIC, 1),
            "shuriken", "shadow_step", "shadow_clone", "shadow_burst", "dance_of_shadows"),
    // ---- стрелки ----
    RANGER("ranger", 0x5BD16A, AGILITY, LUCK, w(RANGED, 3, NATURE, 1),
            "volley", "poison_arrow", "leap_back", "hunters_mark", "arrow_rain"),
    SNIPER("sniper", 0x839192, AGILITY, LUCK, w(RANGED, 4),
            "aimed_shot", "piercing_arrow", "camouflage", "explosive_arrow", "headshot"),
    BEASTMASTER("beastmaster", 0x8E6E3B, AGILITY, VITALITY, w(NATURE, 3, RANGED, 2),
            "call_wolf", "pack", "beast_fury", "leap_back", "hunters_mark"),
    ARBALEST("arbalest", 0x6E2C00, AGILITY, VITALITY, w(RANGED, 3, TANK, 1),
            "heavy_bolt", "buckshot", "bear_trap", "leap_back", "barrage"),
    // ---- маги ----
    MAGE("mage", 0x5AA0F0, INTELLIGENCE, INTELLIGENCE, w(MAGIC, 4),
            "mana_bolt", "fireball", "frost_nova", "blink", "meteor"),
    PYROMANCER("pyromancer", 0xFF5722, INTELLIGENCE, LUCK, w(MAGIC, 2, FIRE, 3),
            "fire_arrow", "fireball", "ring_of_fire", "fire_field", "meteor"),
    CRYOMANCER("cryomancer", 0x85C1E9, INTELLIGENCE, VITALITY, w(MAGIC, 3, TANK, 1),
            "ice_shard", "frost_nova", "ice_armor", "blizzard", "ice_prison"),
    STORMCALLER("stormcaller", 0xF7DC6F, INTELLIGENCE, AGILITY, w(MAGIC, 3, RANGED, 1),
            "spark", "chain_lightning", "lightning_strike", "blink", "thunderstorm"),
    NECROMANCER("necromancer", 0x6C3483, INTELLIGENCE, VITALITY, w(MAGIC, 2, UNDEAD, 3),
            "shadow_bolt", "raise_skeleton", "drain_life", "plague_cloud", "army_of_the_dead"),
    WARLOCK("warlock", 0x7D3C98, INTELLIGENCE, LUCK, w(MAGIC, 3, STEALTH, 1),
            "curse", "chaos_bolt", "summon_imp", "soul_siphon", "gates_of_darkness"),
    DRUID("druid", 0x27AE60, INTELLIGENCE, VITALITY, w(NATURE, 3, SUPPORT, 1, MAGIC, 1),
            "thorn_bolt", "healing_bloom", "bear_form", "entangle", "wrath_of_nature"),
    SHAMAN("shaman", 0x1ABC9C, INTELLIGENCE, VITALITY, w(NATURE, 2, SUPPORT, 2, MAGIC, 1),
            "spirit_lightning", "healing_totem", "fury_totem", "quake", "spirit_wolf"),
    GEOMANCER("geomancer", 0x935116, INTELLIGENCE, VITALITY, w(MAGIC, 2, TANK, 2, CRAFT, 1),
            "stone_bolt", "stoneskin", "quake", "earth_spikes", "rock_fall"),
    SPELLBLADE("spellblade", 0x3498DB, INTELLIGENCE, STRENGTH, w(MELEE, 2, MAGIC, 2),
            "enchanted_blade", "blink", "mana_strike", "arcane_shield", "blade_storm"),
    // ---- поддержка ----
    CLERIC("cleric", 0xF2D16B, INTELLIGENCE, VITALITY, w(SUPPORT, 3, UNDEAD, 1, MAGIC, 1),
            "heal", "smite", "blessing", "purify", "sanctuary"),
    BARD("bard", 0xEC7063, INTELLIGENCE, LUCK, w(SUPPORT, 3, NATURE, 1),
            "song_of_valor", "lullaby", "song_of_healing", "dissonance", "ballad_of_heroes"),
    INQUISITOR("inquisitor", 0xFDFEFE, INTELLIGENCE, STRENGTH, w(UNDEAD, 3, MAGIC, 1, MELEE, 1),
            "smite", "chains_of_light", "purify", "holy_fire", "judgment"),
    ALCHEMIST("alchemist", 0x58D68D, INTELLIGENCE, LUCK, w(CRAFT, 3, SUPPORT, 1, MAGIC, 1),
            "acid_flask", "fire_flask", "elixir_of_might", "healing_flask", "grand_transmutation");

    public final String key;
    public final int color;
    /** Растёт на 1 каждый уровень. */
    public final Stat primary;
    /** Растёт на 1 каждые 2 уровня. */
    public final Stat secondary;
    public final Map<Style, Integer> styles;
    public final String[] skills;

    PlayerClass(String key, int color, Stat primary, Stat secondary, Map<Style, Integer> styles, String... skills) {
        this.key = key;
        this.color = color;
        this.primary = primary;
        this.secondary = secondary;
        this.styles = styles;
        this.skills = skills;
    }

    private static Map<Style, Integer> w(Object... pairs) {
        Map<Style, Integer> m = new EnumMap<>(Style.class);
        for (int i = 0; i < pairs.length; i += 2) m.put((Style) pairs[i], (Integer) pairs[i + 1]);
        return m;
    }

    /** Сколько очков характеристики класс дал к этому уровню. */
    public int growth(Stat s, int level) {
        int v = 0;
        if (s == primary) v += level - 1;
        if (s == secondary) v += (level - 1) / 2;
        return v;
    }

    /** Скрытый класс: предлагается только при выполнении условия. */
    public boolean isHidden() {
        return this == DEATH_KNIGHT || this == NECROMANCER || this == WARLOCK;
    }

    public boolean hiddenUnlocked(PlayerStats s) {
        return switch (this) {
            case DEATH_KNIGHT -> s.getCounter(Title.Counter.DEATHS) >= 3 && s.getCounter(Title.Counter.UNDEAD) >= 30;
            case NECROMANCER -> s.getCounter(Title.Counter.UNDEAD) >= 60;
            case WARLOCK -> s.getCounter(Title.Counter.REALM_VISITS) >= 1;
            default -> true;
        };
    }

    public String nameKey() { return "class.litrpg." + key; }
    public String descKey() { return "class.litrpg." + key + ".desc"; }
    public String hintKey() { return "class.litrpg." + key + ".hint"; }

    public static PlayerClass byKey(String key) {
        for (PlayerClass c : values()) if (c.key.equals(key)) return c;
        return null;
    }
}
