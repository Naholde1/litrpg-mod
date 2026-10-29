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
    HEAL_POWER("heal_power", "силы лечения", true, Slot.ANY),
    RESIST("resist", "защиты от всего урона", true, Slot.ARMOR),
    COOLDOWN("cooldown", "сокращения перезарядки", true, Slot.JEWEL),
    XP("xp", "опыта", true, Slot.JEWEL),
    LOOT("loot", "шанса добычи", true, Slot.JEWEL),
    // против семейств монстров
    DMG_UNDEAD("dmg_undead", "урона по нежити", true, Slot.ANY),
    DMG_ARTHROPOD("dmg_arthropod", "урона по паукам и насекомым", true, Slot.ANY),
    DMG_ILLAGER("dmg_illager", "урона по иллагерам и разбойникам", true, Slot.ANY),
    DMG_GOBLIN("dmg_goblin", "урона по гоблинам", true, Slot.ANY),
    DMG_BEAST("dmg_beast", "урона по зверям", true, Slot.ANY),
    DMG_NETHER("dmg_nether", "урона по существам Преисподней", true, Slot.ANY),
    DMG_ELITE("dmg_elite", "урона по элитам и боссам", true, Slot.ANY),
    DMG_HUMANOID("dmg_humanoid", "урона по разумным расам", true, Slot.ANY),
    RES_UNDEAD("res_undead", "защиты от нежити", true, Slot.ARMOR),
    RES_ARTHROPOD("res_arthropod", "защиты от пауков и насекомых", true, Slot.ARMOR),
    RES_ILLAGER("res_illager", "защиты от иллагеров", true, Slot.ARMOR),
    RES_GOBLIN("res_goblin", "защиты от гоблинов", true, Slot.ARMOR),
    RES_FIRE("res_fire", "защиты от огня", true, Slot.ARMOR),
    RES_FALL("res_fall", "защиты от падения", true, Slot.ARMOR),
    RES_PROJECTILE("res_projectile", "защиты от снарядов", true, Slot.ARMOR),
    RES_EXPLOSION("res_explosion", "защиты от взрывов", true, Slot.ARMOR),
    RES_MAGIC("res_magic", "защиты от магии", true, Slot.ARMOR),
    // прочее
    MINING("mining", "скорости добычи", true, Slot.JEWEL),
    ARROW_DAMAGE("arrow_damage", "урона стрелами", true, Slot.ANY),
    MANA_COST("mana_cost", "экономии маны", true, Slot.JEWEL),
    HEALTH_REGEN("health_regen", "здоровья каждые 5 с", false, Slot.JEWEL),
    COIN_FIND("coin_find", "монет с монстров", true, Slot.JEWEL),
    SELL_PRICE("sell_price", "к цене продажи", true, Slot.JEWEL);

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
            case MANA_REGEN, SPELL_POWER, HEAL_POWER, XP, LOOT -> 5 + lvl * 0.2f;
            case RESIST -> 1.5f + lvl * 0.05f;
            case COOLDOWN -> 3 + lvl * 0.1f;
            case DMG_UNDEAD, DMG_ARTHROPOD, DMG_ILLAGER, DMG_GOBLIN, DMG_BEAST, DMG_NETHER, DMG_HUMANOID -> 8 + lvl * 0.3f;
            case DMG_ELITE -> 5 + lvl * 0.2f;
            case RES_UNDEAD, RES_ARTHROPOD, RES_ILLAGER, RES_GOBLIN -> 5 + lvl * 0.2f;
            case RES_FIRE, RES_FALL, RES_PROJECTILE, RES_EXPLOSION, RES_MAGIC -> 6 + lvl * 0.2f;
            case MINING, ARROW_DAMAGE -> 6 + lvl * 0.2f;
            case MANA_COST -> 3 + lvl * 0.1f;
            case HEALTH_REGEN -> 0.5f + lvl * 0.04f;
            case COIN_FIND, SELL_PRICE -> 8 + lvl * 0.3f;
        };
        v *= mult * (0.75f + rnd * 0.5f);
        if (this == MANA_COST || this == COOLDOWN) v = Math.min(v, 40);
        if (this == RESIST) v = Math.min(v, 12);
        return percent || this == HEALTH || this == MANA ? Math.round(v) : Math.round(v * 10) / 10f;
    }

    public static GearBonus byKey(String key) {
        for (GearBonus b : values()) if (b.key.equals(key)) return b;
        return null;
    }
}
