package com.andrey.litrpg.gear;

/** Дополнительные бонусы предметов (кроме пяти основных характеристик). */
public enum GearBonus {
    HEALTH("health", "здоровья", false, Slot.ARMOR),
    ARMOR("armor", "брони", false, Slot.ARMOR),
    DAMAGE("damage", "урона оружием", false, Slot.WEAPON),
    ATTACK_SPEED("attack_speed", "скорости атаки", true, Slot.WEAPON),
    MOVE_SPEED("move_speed", "скорости бега", true, Slot.ARMOR),
    CRIT_CHANCE("crit_chance", "шанса крита", true, Slot.ANY),
    CRIT_DAMAGE("crit_damage", "силы крита", true, Slot.WEAPON),
    DODGE("dodge", "уклонения", true, Slot.ARMOR),
    LIFESTEAL("lifesteal", "вампиризма", true, Slot.WEAPON),
    MANA("mana", "маны", false, Slot.JEWEL),
    MANA_REGEN("mana_regen", "восстановления маны", true, Slot.JEWEL),
    SPELL_POWER("spell_power", "силы навыков", true, Slot.ANY),
    COOLDOWN("cooldown", "сокращения перезарядки", true, Slot.JEWEL),
    XP("xp", "опыта", true, Slot.JEWEL),
    LOOT("loot", "шанса добычи", true, Slot.JEWEL);

    public enum Slot { WEAPON, ARMOR, JEWEL, ANY }

    public final String key;
    public final String text;
    public final boolean percent;
    public final Slot slot;

    GearBonus(String key, String text, boolean percent, Slot slot) {
        this.key = key;
        this.text = text;
        this.percent = percent;
        this.slot = slot;
    }

    public boolean fits(GearData.Category c) {
        return switch (slot) {
            case ANY -> true;
            case WEAPON -> c == GearData.Category.WEAPON;
            case ARMOR -> c == GearData.Category.ARMOR || c == GearData.Category.JEWEL;
            case JEWEL -> c == GearData.Category.JEWEL || c == GearData.Category.WEAPON && this == SPELL_POWER;
        };
    }

    /** Значение бонуса для предмета уровня lvl и множителя редкости. */
    public float roll(int lvl, float mult, float rnd) {
        float v = switch (this) {
            case HEALTH -> 2 + lvl * 0.6f;
            case ARMOR -> 0.5f + lvl * 0.08f;
            case DAMAGE -> 0.5f + lvl * 0.15f;
            case MANA -> 8 + lvl * 1.5f;
            case ATTACK_SPEED, MOVE_SPEED -> 3 + lvl * 0.08f;
            case CRIT_CHANCE, DODGE -> 2 + lvl * 0.06f;
            case CRIT_DAMAGE -> 8 + lvl * 0.3f;
            case LIFESTEAL -> 2 + lvl * 0.05f;
            case MANA_REGEN, SPELL_POWER, XP, LOOT -> 5 + lvl * 0.2f;
            case COOLDOWN -> 3 + lvl * 0.1f;
        };
        v *= mult * (0.75f + rnd * 0.5f);
        return percent || this == HEALTH || this == MANA ? Math.round(v) : Math.round(v * 10) / 10f;
    }

    public static GearBonus byKey(String key) {
        for (GearBonus b : values()) if (b.key.equals(key)) return b;
        return null;
    }
}
