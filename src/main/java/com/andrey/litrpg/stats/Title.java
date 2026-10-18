package com.andrey.litrpg.stats;

/**
 * Титулы Системы. Выдаются за достижения, каждый полученный титул
 * навсегда даёт небольшой бонус к характеристикам.
 * Порядок бонусов: Сила, Ловкость, Выносливость, Интеллект, Удача.
 */
public enum Title {
    FIRST_BLOOD("first_blood", Counter.KILLS, 1, 1, 0, 0, 0, 0),
    HUNTER_50("hunter_50", Counter.KILLS, 50, 0, 1, 1, 0, 0),
    CENTURION("centurion", Counter.KILLS, 500, 2, 2, 2, 2, 2),
    GOBLIN_BANE("goblin_bane", Counter.GOBLINS, 100, 2, 1, 0, 0, 0),
    UNDEAD_HUNTER("undead_hunter", Counter.UNDEAD, 100, 0, 0, 2, 1, 0),
    ELITE_HUNTER("elite_hunter", Counter.ELITES, 25, 1, 1, 1, 1, 2),
    GIANT_SLAYER("giant_slayer", Counter.GIANT_KILLS, 1, 2, 1, 0, 0, 1),
    KINGSLAYER("kingslayer", Counter.GOBLIN_KING, 1, 3, 0, 3, 0, 0),
    LICHBANE("lichbane", Counter.LICH, 1, 0, 0, 2, 4, 2),
    REALM_WALKER("realm_walker", Counter.REALM_VISITS, 1, 0, 1, 0, 1, 0),
    UNLUCKY("unlucky", Counter.DEATHS, 10, 0, 0, 1, 0, 3),
    DEEP_DELVER("deep_delver", Counter.ORES, 500, 1, 0, 2, 0, 1),
    // за Почтение у народов (выдаются вручную при достижении ступени)
    ELF_FRIEND("elf_friend", Counter.REPUTATION, Integer.MAX_VALUE, 0, 2, 0, 1, 1),
    DWARF_FRIEND("dwarf_friend", Counter.REPUTATION, Integer.MAX_VALUE, 1, 0, 2, 0, 1),
    ORC_FRIEND("orc_friend", Counter.REPUTATION, Integer.MAX_VALUE, 2, 0, 1, 0, 1),
    BEAST_FRIEND("beast_friend", Counter.REPUTATION, Integer.MAX_VALUE, 0, 2, 0, 0, 2),
    DEMON_FRIEND("demon_friend", Counter.REPUTATION, Integer.MAX_VALUE, 1, 0, 0, 2, 1),
    KINGDOM_FRIEND("kingdom_friend", Counter.REPUTATION, Integer.MAX_VALUE, 1, 1, 1, 1, 1);

    public enum Counter {
        KILLS, GOBLINS, UNDEAD, ELITES, GIANT_KILLS, GOBLIN_KING, LICH, REALM_VISITS, DEATHS, ORES, REPUTATION;

        public String key() { return name().toLowerCase(); }
    }

    public final String key;
    public final Counter counter;
    public final int threshold;
    private final int[] bonus;

    Title(String key, Counter counter, int threshold, int... bonus) {
        this.key = key;
        this.counter = counter;
        this.threshold = threshold;
        this.bonus = bonus;
    }

    public int bonus(Stat s) {
        return s.ordinal() < bonus.length ? bonus[s.ordinal()] : 0;
    }

    public String nameKey() { return "title.litrpg." + key; }
    public String descKey() { return "title.litrpg." + key + ".desc"; }

    public static Title byKey(String key) {
        for (Title t : values()) if (t.key.equals(key)) return t;
        return null;
    }
}
