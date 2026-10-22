package com.andrey.litrpg.stats;

import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.PlayerGear;

/**
 * Роль в отряде. Выбирается в начале пути вместе с расой: Система предлагает
 * классы только этой роли (скрытые классы — исключение). Роль даёт небольшой бонус.
 */
public enum Role {
    TANK("tank", "Танк", 0x5DADE2, "Принимает удары на себя и держит врагов",
            "+здоровье и броня (растут с уровнем), враги злее на вас (угроза ×1,5)"),
    DAMAGE("damage", "Боец", 0xE74C3C, "Наносит урон: клинком, стрелой или чарами",
            "+8% силы навыков, +3% шанса крита"),
    SUPPORT("support", "Поддержка", 0x58D68D, "Лечит, защищает и усиливает союзников",
            "+25% к лечению, +15% восстановления маны, +8% экономии маны");

    public final String key;
    public final String title;
    public final int color;
    public final String desc;
    public final String perk;

    Role(String key, String title, int color, String desc, String perk) {
        this.key = key;
        this.title = title;
        this.color = color;
        this.desc = desc;
        this.perk = perk;
    }

    /** Бонус роли — добавляется к сводке экипировки. */
    public void apply(PlayerGear g, int level) {
        float lv = 1 + level / 25f;
        switch (this) {
            case TANK -> {
                g.add(GearBonus.HEALTH, 4 * lv);
                g.add(GearBonus.ARMOR, 1 * lv);
            }
            case DAMAGE -> {
                g.add(GearBonus.SPELL_POWER, 8);
                g.add(GearBonus.CRIT_CHANCE, 3);
            }
            case SUPPORT -> {
                g.add(GearBonus.MANA_REGEN, 15);
                g.add(GearBonus.MANA_COST, 8);
            }
        }
    }

    public static Role byKey(String key) {
        for (Role r : values()) if (r.key.equalsIgnoreCase(key)) return r;
        return null;
    }
}
