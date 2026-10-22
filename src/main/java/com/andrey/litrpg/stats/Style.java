package com.andrey.litrpg.stats;

/**
 * Стиль игры до 10 уровня. Система копит очки за каждое занятие
 * и по ним (а ещё по вложенным очкам, расе, профессиям и репутации) подбирает классы.
 */
public enum Style {
    MELEE("melee"),       // урон в ближнем бою
    RANGED("ranged"),     // урон стрелами и трезубцем
    MAGIC("magic"),       // магический урон, потраченная мана
    TANK("tank"),         // полученный урон, блоки щитом
    STEALTH("stealth"),   // удары крадучись и из невидимости, криты
    SUPPORT("support"),   // лечение, зелья
    NATURE("nature"),     // урожай, рыбалка, природа
    UNDEAD("undead"),     // упокоенная нежить
    FIRE("fire"),         // урон по горящим врагам, поджоги
    CRAFT("craft"),       // крафт, плавка, зельеварение, зачарования
    PARTY("party"),       // бой рядом с товарищами по группе
    SOLO("solo"),         // победы в одиночку
    TRADE("trade"),       // торговля с жителями и скупщиками
    EXPLORE("explore"),   // открытые места и дальние походы
    NIGHT("night"),       // бой ночью и во тьме
    WATER("water"),       // бой в воде, плавание, рыбалка
    MINING("mining"),     // руда и камень
    RISK("risk"),         // бой на грани смерти
    BEAST("beast");       // приручение и разведение животных

    public final String key;

    Style(String key) {
        this.key = key;
    }

    public String nameKey() { return "style.litrpg." + key; }

    public static Style byKey(String key) {
        for (Style s : values()) if (s.key.equals(key)) return s;
        return null;
    }
}
