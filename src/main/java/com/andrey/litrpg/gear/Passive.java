package com.andrey.litrpg.gear;

/** Особые свойства предметов (эпические и выше). */
public enum Passive {
    // оружие
    FLAME_TOUCH("flame_touch", "Пламя", "удары поджигают врага", true),
    FROST_TOUCH("frost_touch", "Иней", "удары сильно замедляют", true),
    VENOM("venom", "Яд", "удары отравляют", true),
    CHAIN_SPARK("chain_spark", "Грозовой клинок", "15% шанс цепной молнии при ударе", true),
    EXECUTIONER("executioner", "Палач", "+50% урона по врагам ниже 30% здоровья", true),
    SOUL_EATER("soul_eater", "Пожиратель душ", "убийство лечит 15% здоровья и 10% маны", true),
    GIANT_SLAYER("giant_slayer", "Убийца великанов", "+25% урона по врагам выше вас уровнем", true),
    // броня и украшения
    THORNS("thorns", "Шипы", "возвращает 25% урона в ближнем бою", false),
    PHOENIX("phoenix", "Феникс", "смертельный удар оставляет 30% здоровья (раз в 10 минут)", false),
    FIREPROOF("fireproof", "Огнеупорность", "иммунитет к огню и лаве", false),
    NIGHT_EYE("night_eye", "Ночное зрение", "видите в темноте", false),
    FEATHERFALL("featherfall", "Пёрышко", "нет урона от падения", false),
    SWIFTNESS("swiftness", "Стремительность", "постоянная Скорость I", false),
    REGENERATION("regeneration", "Регенерация", "постоянная Регенерация I", false),
    MANA_SHIELD("mana_shield", "Щит маны", "20% урона поглощает мана", false),
    BERSERKER("berserker", "Берсерк", "+30% урона при здоровье ниже 40%", false),
    WATER_BREATHING("water_breathing", "Жабры", "дыхание под водой", false),
    ARCANE_ECHO("arcane_echo", "Магическое эхо", "15% шанс, что навык не уйдёт на перезарядку", false),
    TREASURE_HUNTER("treasure_hunter", "Кладоискатель", "вдвое выше шанс выпадения предметов", false),
    // только у классов (на предметах не выпадают)
    FURY_SWIPES("fury_swipes", "Ярость когтей", "каждый удар по той же цели +8% урона (до +80%)", true, true),
    MOMENTUM("momentum", "Инерция", "урон растёт со скоростью бега", true, true),
    WAR_AURA("war_aura", "Аура войны", "союзники в 12 бл. — Воодушевление I (+10% урона)", false, true),
    GUARD_AURA("guard_aura", "Аура защиты", "союзники в 12 бл. — Заслон I (−10% урона)", false, true),
    MANA_AURA("mana_aura", "Источник маны", "союзники в 12 бл. восполняют 2% маны раз в 2 с", false, true),
    FOCUS_AURA("focus_aura", "Аура сосредоточенности", "союзники в 12 бл. — Сосредоточенность I (−10% перезарядки)", false, true);

    public final String key;
    public final String title;
    public final String desc;
    public final boolean weapon;
    /** Свойство класса — на предметах не встречается. */
    public final boolean classOnly;

    Passive(String key, String title, String desc, boolean weapon) {
        this(key, title, desc, weapon, false);
    }

    Passive(String key, String title, String desc, boolean weapon, boolean classOnly) {
        this.key = key;
        this.title = title;
        this.desc = desc;
        this.weapon = weapon;
        this.classOnly = classOnly;
    }

    public static Passive byKey(String key) {
        for (Passive p : values()) if (p.key.equals(key)) return p;
        return null;
    }
}
