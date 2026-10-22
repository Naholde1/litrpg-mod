package com.andrey.litrpg.stats;

import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.PlayerGear;

/**
 * Роль в отряде. Выбирается в начале пути вместе с расой: Система предлагает
 * классы только этой роли (скрытые классы — исключение). Роль даёт небольшой бонус.
 */
public enum Role {
    TANK("tank", "Танк", 0x5DADE2, "Принимает удары на себя и держит врагов",
            "+здоровье и броня, угроза ×5, хранители бьют вдвое слабее; свой урон ×0,6"),
    DAMAGE("damage", "Боец", 0xE74C3C, "Наносит урон: клинком, стрелой или чарами",
            "урон ×1,15, +8% силы навыков, +3% шанса крита"),
    SUPPORT("support", "Поддержка", 0x58D68D, "Лечит, защищает и усиливает союзников",
            "лечение ×2,2 (у других ролей — вдвое слабее), воскрешение павших в бою, +15% маны; свой урон ×0,75");

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

    /** Множитель урона по врагам: танк и лекарь бьют заметно слабее бойца (как в MMO). */
    public float damageMult() {
        return switch (this) {
            case TANK -> 0.6f;
            case DAMAGE -> 1.15f;
            case SUPPORT -> 0.75f;
        };
    }

    public static Role byKey(String key) {
        for (Role r : values()) if (r.key.equalsIgnoreCase(key)) return r;
        return null;
    }
}
