package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;

/**
 * Чем класс платит за способности. Шесть семейств вместо общей маны у всех:
 * внутри семейства классы отличаются навыками, но правила накопления и траты одни.
 * <p>
 * Общая мана делала бой одинаковым: у лекаря её хватало на безостановочный спам,
 * а воину, у которого нет интеллекта, не хватало на собственные удары.
 */
public enum Resource {
    /** Копится сама. Маги и часть саппортов. */
    MANA("mana", "Мана", "МП", "маны", 0xFF3D8BFF, 0xFF1A2340),
    /** Копится от ударов и утекает вне боя. Берсерк, Палач. */
    RAGE("rage", "Ярость", "ЯР", "ярости", 0xFFE05A47, 0xFF3A1A16),
    /** Платит жизнью: шкала показывает здоровье. Кровавый рыцарь, Теневой жрец. */
    HEALTH("health", "Жизнь", "ЖЗ", "здоровья", 0xFFC03030, 0xFF3A1414),
    /** Ячейки, восстанавливаются по одной. Монах, Стражник, Механик. */
    CHARGES("charges", "Заряды", "ЗР", "зарядов", 0xFFE8C04A, 0xFF3A3014),
    /** Сферы и печати: копишь и сплетаешь. Астромант, Призыватель. */
    ORBS("orbs", "Сферы", "СФ", "сфер", 0xFFB060E0, 0xFF2A1640),
    /** Шкала облика: тратится, пока держишь форму. Оборотень, Дух. */
    FORM("form", "Облик", "ОБ", "облика", 0xFF5BD16A, 0xFF163A1C);

    /** Сколько секунд восстанавливается один заряд. */
    private static final double CHARGE_SECONDS = 6;
    /** Ярость за свой удар и за полученный. */
    public static final float RAGE_ON_HIT = 6;
    public static final float RAGE_ON_TAKEN = 4;
    /** Через сколько тиков без боя ярость начинает утекать и по сколько в секунду. */
    public static final int RAGE_CALM_TICKS = 20 * 5;
    public static final double RAGE_DECAY = 3;

    public final String key;
    /** Как называется шкала. */
    public final String title;
    /** Две буквы рядом с числом на панели. */
    public final String tag;
    /** «Не хватает …» */
    public final String genitive;
    public final int color;
    public final int back;

    Resource(String key, String title, String tag, String genitive, int color, int back) {
        this.key = key;
        this.title = title;
        this.tag = tag;
        this.genitive = genitive;
        this.color = color;
        this.back = back;
    }

    public static Resource byKey(String key) {
        for (Resource r : values()) if (r.key.equals(key)) return r;
        return MANA;
    }

    /**
     * Размер шкалы. Растить её можно всем, а не только магам: уровень добавляет понемногу,
     * а бонус «маны» с экипировки работает на любой ресурс — иначе у классов с ровной
     * шкалой не было бы ни одного способа её увеличить.
     */
    public int max(PlayerStats s) {
        float gear = s.gear().get(com.andrey.litrpg.gear.GearBonus.MANA);
        return switch (this) {
            case MANA -> (int) Math.round(StatBreakdown.maxMana(s).value());
            // сферы — механика сплетения, их всегда три
            case ORBS -> 3;
            // ячейка сверх четырёх — только за очень весомую экипировку, и не больше двух
            case CHARGES -> 6 + (int) Math.min(2, gear / 120);
            default -> 100 + s.getLevel() / 2 + (int) (gear / 4);
        };
    }

    /** Сколько прибавляется само по себе за секунду. */
    public double regen(PlayerStats s) {
        return switch (this) {
            case MANA -> StatBreakdown.manaRegen(s).value();
            case CHARGES -> 1.0 / CHARGE_SECONDS;
            case FORM -> 6;
            // ярость копится в бою, сферы — от навыков, здоровье — это само здоровье
            case RAGE, HEALTH, ORBS -> 0;
        };
    }

    /**
     * Цена способности в этом ресурсе. В данных навыка цена записана в мане,
     * остальные семейства пересчитывают её по-своему.
     */
    public int cost(Ability a) {
        return switch (this) {
            // сферы тратит сам навык сплетения, здоровье списывается долей жизни
            case ORBS, HEALTH -> 0;
            case CHARGES -> a.manaCost() >= 35 ? 2 : 1;
            default -> a.manaCost();
        };
    }

    /** Доля максимального здоровья за применение — только у классов на здоровье. */
    public float healthCost(Ability a) {
        return this == HEALTH ? a.manaCost() * 0.002f : 0;
    }

    /** Снижение цены от экипировки работает там, где ресурс копится сам. */
    public boolean cheapened() {
        return this == MANA || this == FORM;
    }

    /**
     * Восстанавливается ли сам на привале и вне боя. Ярость — наоборот, вне боя утекает;
     * заряды и сферы идут своим счётом, здоровье лечится отдельно.
     */
    public boolean restoresOutOfCombat() {
        return this == MANA || this == FORM;
    }

    /** Шкала показывает здоровье, а не отдельный запас. */
    public boolean mirrorsHealth() {
        return this == HEALTH;
    }

    /** Целыми числами (заряды, сферы) или с округлением вниз. */
    public boolean discrete() {
        return this == CHARGES || this == ORBS;
    }
}
