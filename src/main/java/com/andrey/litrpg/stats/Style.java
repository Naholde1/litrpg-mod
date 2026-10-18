package com.andrey.litrpg.stats;

/**
 * Стиль игры до 10 уровня. Система копит очки за каждое занятие
 * и по ним подбирает три подходящих класса.
 */
public enum Style {
    MELEE("melee"),       // урон в ближнем бою
    RANGED("ranged"),     // урон стрелами и трезубцем
    MAGIC("magic"),       // магический урон, потраченная мана
    TANK("tank"),         // полученный урон, блоки щитом
    STEALTH("stealth"),   // удары крадучись и из невидимости, криты
    SUPPORT("support"),   // лечение, зелья, торговля
    NATURE("nature"),     // животные, урожай, рыбалка
    UNDEAD("undead"),     // упокоенная нежить
    FIRE("fire"),         // урон по горящим врагам, поджоги
    CRAFT("craft");       // крафт, плавка, зельеварение, зачарования

    public final String key;

    Style(String key) {
        this.key = key;
    }

    public String nameKey() { return "style.litrpg." + key; }
}
