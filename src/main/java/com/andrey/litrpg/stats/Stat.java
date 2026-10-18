package com.andrey.litrpg.stats;

public enum Stat {
    STRENGTH("strength", 0xE05A47),
    AGILITY("agility", 0x5BD16A),
    VITALITY("vitality", 0xE8A33C),
    INTELLIGENCE("intelligence", 0x5AA0F0),
    LUCK("luck", 0xD9C34A);

    public final String key;
    public final int color;

    Stat(String key, int color) {
        this.key = key;
        this.color = color;
    }

    public String translationKey() {
        return "stat.litrpg." + key;
    }

    public String descKey() {
        return "stat.litrpg." + key + ".desc";
    }
}
