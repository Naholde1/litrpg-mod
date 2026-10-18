package com.andrey.litrpg.stats;

/**
 * Расы. Дают стартовые характеристики и постоянный пассивный бонус.
 * Порядок бонусов: Сила, Ловкость, Выносливость, Интеллект, Удача.
 */
public enum Race {
    HUMAN("human", 0xE8C39E, 1, 1, 1, 1, 2),
    ELF("elf", 0x9BE38C, 0, 3, 0, 3, 0),
    DWARF("dwarf", 0xD08A4A, 2, 0, 4, 0, 0),
    ORC("orc", 0x6FA34A, 4, 0, 3, -1, 0),
    BEASTKIN("beastkin", 0xC9A36B, 1, 4, 1, 0, 0),
    DEMONKIN("demonkin", 0xC0405A, 2, 1, 0, 3, -1);

    public final String key;
    public final int color;
    private final int[] bonus;

    Race(String key, int color, int... bonus) {
        this.key = key;
        this.color = color;
        this.bonus = bonus;
    }

    public int bonus(Stat s) {
        return s.ordinal() < bonus.length ? bonus[s.ordinal()] : 0;
    }

    public String nameKey() { return "race.litrpg." + key; }
    public String descKey() { return "race.litrpg." + key + ".desc"; }
    public String passiveKey() { return "race.litrpg." + key + ".passive"; }

    public static Race byKey(String key) {
        for (Race r : values()) if (r.key.equals(key)) return r;
        return null;
    }
}
