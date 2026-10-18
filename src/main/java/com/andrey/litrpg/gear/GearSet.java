package com.andrey.litrpg.gear;

import com.andrey.litrpg.stats.Stat;

/** Наборы брони: бонус за 2 и за 4 надетых предмета. */
public enum GearSet {
    GOBLIN("goblin", "Гоблинский набор",
            "+10% скорости бега", "+3 Ловкость, +3 Удача, гоблины не нападают первыми"),
    BONEWARDEN("bonewarden", "Доспех Костяного стража",
            "+4 брони", "+4 Выносливость, нежить наносит на 25% меньше урона"),
    SHADOW("shadow", "Облачение Тени",
            "+5% уклонения", "+10% шанса крита, удар из скрытности x2 вместо x1.5");

    public final String key;
    public final String title;
    public final String bonus2;
    public final String bonus4;

    GearSet(String key, String title, String bonus2, String bonus4) {
        this.key = key;
        this.title = title;
        this.bonus2 = bonus2;
        this.bonus4 = bonus4;
    }

    /** Применить бонусы набора к сводке экипировки. */
    void apply(PlayerGear g, int pieces) {
        if (pieces >= 2) {
            switch (this) {
                case GOBLIN -> g.add(GearBonus.MOVE_SPEED, 10);
                case BONEWARDEN -> g.add(GearBonus.ARMOR, 4);
                case SHADOW -> g.add(GearBonus.DODGE, 5);
            }
        }
        if (pieces >= 4) {
            g.fullSets.add(this);
            switch (this) {
                case GOBLIN -> { g.addStat(Stat.AGILITY, 3); g.addStat(Stat.LUCK, 3); }
                case BONEWARDEN -> g.addStat(Stat.VITALITY, 4);
                case SHADOW -> g.add(GearBonus.CRIT_CHANCE, 10);
            }
        }
    }

    public static GearSet byKey(String key) {
        for (GearSet s : values()) if (s.key.equals(key)) return s;
        return null;
    }
}
