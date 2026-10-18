package com.andrey.litrpg.gear;

import net.minecraft.ChatFormatting;

/**
 * Редкость предметов Системы. Чем выше, тем больше характеристик, бонусов
 * и пассивок, а с легендарной у оружия появляется активная способность.
 */
public enum Rarity {
    COMMON("common", "Обычный", ChatFormatting.WHITE, 0xC8C8C8, 1, 0, 0, false, 1.00f),
    UNCOMMON("uncommon", "Необычный", ChatFormatting.GREEN, 0x55FF55, 2, 1, 0, false, 1.15f),
    RARE("rare", "Редкий", ChatFormatting.BLUE, 0x5599FF, 3, 1, 0, false, 1.30f),
    EPIC("epic", "Эпический", ChatFormatting.LIGHT_PURPLE, 0xC060FF, 3, 2, 1, false, 1.50f),
    LEGENDARY("legendary", "Легендарный", ChatFormatting.GOLD, 0xFFAA00, 4, 2, 1, true, 1.75f),
    DIVINE("divine", "Божественный", ChatFormatting.RED, 0xFF4040, 5, 3, 2, true, 2.10f);

    public final String key;
    public final String title;
    public final ChatFormatting color;
    public final int rgb;
    public final int stats;
    public final int bonuses;
    public final int passives;
    public final boolean ability;
    public final float mult;

    Rarity(String key, String title, ChatFormatting color, int rgb, int stats, int bonuses, int passives, boolean ability, float mult) {
        this.key = key;
        this.title = title;
        this.color = color;
        this.rgb = rgb;
        this.stats = stats;
        this.bonuses = bonuses;
        this.passives = passives;
        this.ability = ability;
        this.mult = mult;
    }

    public Rarity next() {
        return this == DIVINE ? DIVINE : values()[ordinal() + 1];
    }

    public static Rarity byId(int id) {
        Rarity[] v = values();
        return id >= 0 && id < v.length ? v[id] : COMMON;
    }
}
