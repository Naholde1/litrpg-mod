package com.andrey.litrpg.ability;

public enum Ability {
    DASH("dash", 1, 15, 60),
    MANA_BOLT("mana_bolt", 3, 20, 30),
    HEAL("heal", 5, 30, 240),
    SLAM("slam", 8, 35, 160),
    BERSERK("berserk", 12, 50, 600);

    public final String key;
    public final int unlockLevel;
    public final int manaCost;
    /** в тиках */
    public final int cooldown;

    Ability(String key, int unlockLevel, int manaCost, int cooldown) {
        this.key = key;
        this.unlockLevel = unlockLevel;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
    }

    public String nameKey() { return "ability.litrpg." + key; }
    public String descKey() { return "ability.litrpg." + key + ".desc"; }

    public static Ability byId(int id) {
        Ability[] v = values();
        return id >= 0 && id < v.length ? v[id] : null;
    }
}
