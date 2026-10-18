package com.andrey.litrpg.stats;

/**
 * Профессии. Игрок может освоить до двух. Опыт профессии идёт от
 * соответствующего занятия, уровень даёт пассивные бонусы.
 */
public enum Profession {
    MINER("miner", 0x9AA4B0),
    BLACKSMITH("blacksmith", 0xE0873C),
    ALCHEMIST("alchemist", 0xB05AE0),
    HUNTER("hunter", 0x8FB55A),
    HERBALIST("herbalist", 0x5ACB7A);

    public static final int MAX_LEVEL = 50;
    public static final int MAX_KNOWN = 2;

    public final String key;
    public final int color;

    Profession(String key, int color) {
        this.key = key;
        this.color = color;
    }

    public static int xpForNext(int level) {
        return 20 + (int) Math.round(12 * Math.pow(level, 1.45));
    }

    public String nameKey() { return "profession.litrpg." + key; }
    public String descKey() { return "profession.litrpg." + key + ".desc"; }

    public static Profession byKey(String key) {
        for (Profession p : values()) if (p.key.equals(key)) return p;
        return null;
    }
}
