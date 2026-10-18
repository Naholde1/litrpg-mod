package com.andrey.litrpg.npc;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.stats.Race;

/**
 * Жители рас: по пять ролей на народ. Роль определяет поведение
 * (ближний бой, стрельба, магия, торговля, вождь), раса — внешность и фракцию.
 */
public enum NpcType {
    // ---- эльфы ----
    ELF_WARDEN("elf_warden", Race.ELF, Faction.ELVES, Role.ARCHER, 28, 4, 0.30),
    ELF_BLADEDANCER("elf_bladedancer", Race.ELF, Faction.ELVES, Role.WARRIOR, 30, 6, 0.33),
    ELF_MOONSAGE("elf_moonsage", Race.ELF, Faction.ELVES, Role.MAGE, 24, 5, 0.28),
    ELF_TRADER("elf_trader", Race.ELF, Faction.ELVES, Role.TRADER, 24, 2, 0.28),
    ELF_MATRIARCH("elf_matriarch", Race.ELF, Faction.ELVES, Role.CHIEF, 160, 9, 0.30),
    // ---- дварфы ----
    DWARF_WARRIOR("dwarf_warrior", Race.DWARF, Faction.DWARVES, Role.WARRIOR, 40, 7, 0.25),
    DWARF_CROSSBOWMAN("dwarf_crossbowman", Race.DWARF, Faction.DWARVES, Role.ARCHER, 34, 4, 0.24),
    DWARF_RUNEPRIEST("dwarf_runepriest", Race.DWARF, Faction.DWARVES, Role.MAGE, 30, 5, 0.24),
    DWARF_SMITH("dwarf_smith", Race.DWARF, Faction.DWARVES, Role.TRADER, 32, 3, 0.24),
    DWARF_THANE("dwarf_thane", Race.DWARF, Faction.DWARVES, Role.CHIEF, 220, 11, 0.25),
    // ---- орки ----
    ORC_RAIDER("orc_raider", Race.ORC, Faction.ORCS, Role.WARRIOR, 40, 8, 0.29),
    ORC_HUNTER("orc_hunter", Race.ORC, Faction.ORCS, Role.ARCHER, 32, 5, 0.29),
    ORC_SHAMAN("orc_shaman", Race.ORC, Faction.ORCS, Role.MAGE, 30, 6, 0.27),
    ORC_TRADER("orc_trader", Race.ORC, Faction.ORCS, Role.TRADER, 34, 3, 0.27),
    ORC_WARLORD("orc_warlord", Race.ORC, Faction.ORCS, Role.CHIEF, 240, 13, 0.28),
    // ---- зверолюды ----
    BEAST_STALKER("beast_stalker", Race.BEASTKIN, Faction.BEASTKIN, Role.ARCHER, 28, 4, 0.34),
    BEAST_RAVAGER("beast_ravager", Race.BEASTKIN, Faction.BEASTKIN, Role.WARRIOR, 34, 7, 0.36),
    BEAST_SPIRITCALLER("beast_spiritcaller", Race.BEASTKIN, Faction.BEASTKIN, Role.MAGE, 26, 5, 0.31),
    BEAST_TRADER("beast_trader", Race.BEASTKIN, Faction.BEASTKIN, Role.TRADER, 26, 2, 0.31),
    BEAST_ALPHA("beast_alpha", Race.BEASTKIN, Faction.BEASTKIN, Role.CHIEF, 200, 12, 0.36),
    // ---- демониды ----
    DEMON_CULTIST("demon_cultist", Race.DEMONKIN, Faction.DEMONKIN, Role.MAGE, 30, 6, 0.28),
    DEMON_GUARD("demon_guard", Race.DEMONKIN, Faction.DEMONKIN, Role.WARRIOR, 40, 8, 0.29),
    DEMON_HELLBOW("demon_hellbow", Race.DEMONKIN, Faction.DEMONKIN, Role.ARCHER, 32, 5, 0.29),
    DEMON_TRADER("demon_trader", Race.DEMONKIN, Faction.DEMONKIN, Role.TRADER, 30, 3, 0.27),
    DEMON_ARCHFIEND("demon_archfiend", Race.DEMONKIN, Faction.DEMONKIN, Role.CHIEF, 260, 14, 0.29),
    // ---- люди Королевства ----
    HUMAN_KNIGHT("human_knight", Race.HUMAN, Faction.KINGDOM, Role.WARRIOR, 60, 9, 0.3),
    HUMAN_RANGER("human_ranger", Race.HUMAN, Faction.KINGDOM, Role.ARCHER, 40, 6, 0.3),
    HUMAN_MAGE("human_mage", Race.HUMAN, Faction.KINGDOM, Role.MAGE, 34, 6, 0.28),
    HUMAN_MERCHANT("human_merchant", Race.HUMAN, Faction.KINGDOM, Role.TRADER, 30, 2, 0.28),
    HUMAN_LORD("human_lord", Race.HUMAN, Faction.KINGDOM, Role.CHIEF, 300, 14, 0.3),
    // ---- стража городов: почти неуязвима ----
    HUMAN_GUARD("human_guard", Race.HUMAN, Faction.KINGDOM, Role.GUARD, 1000, 40, 0.36),
    ELF_SENTINEL("elf_sentinel", Race.ELF, Faction.ELVES, Role.GUARD, 1000, 38, 0.38),
    DWARF_IRONGUARD("dwarf_ironguard", Race.DWARF, Faction.DWARVES, Role.GUARD, 1000, 44, 0.32),
    ORC_BLOODGUARD("orc_bloodguard", Race.ORC, Faction.ORCS, Role.GUARD, 1000, 46, 0.36),
    BEAST_FANGGUARD("beast_fangguard", Race.BEASTKIN, Faction.BEASTKIN, Role.GUARD, 1000, 40, 0.4),
    DEMON_HELLGUARD("demon_hellguard", Race.DEMONKIN, Faction.DEMONKIN, Role.GUARD, 1000, 46, 0.36),
    // ---- горожане ----
    HUMAN_CITIZEN("human_citizen", Race.HUMAN, Faction.KINGDOM, Role.CITIZEN, 24, 2, 0.3),
    ELF_CITIZEN("elf_citizen", Race.ELF, Faction.ELVES, Role.CITIZEN, 24, 2, 0.3),
    DWARF_CITIZEN("dwarf_citizen", Race.DWARF, Faction.DWARVES, Role.CITIZEN, 30, 3, 0.26),
    ORC_CITIZEN("orc_citizen", Race.ORC, Faction.ORCS, Role.CITIZEN, 34, 4, 0.3),
    BEAST_CITIZEN("beast_citizen", Race.BEASTKIN, Faction.BEASTKIN, Role.CITIZEN, 26, 3, 0.33),
    DEMON_CITIZEN("demon_citizen", Race.DEMONKIN, Faction.DEMONKIN, Role.CITIZEN, 28, 3, 0.3);

    public enum Role { WARRIOR, ARCHER, MAGE, TRADER, CHIEF, GUARD, CITIZEN }

    public final String key;
    public final Race race;
    public final Faction faction;
    public final Role role;
    public final double health;
    public final double damage;
    public final double speed;

    NpcType(String key, Race race, Faction faction, Role role, double health, double damage, double speed) {
        this.key = key;
        this.race = race;
        this.faction = faction;
        this.role = role;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
    }

    public boolean fights() { return role != Role.TRADER && role != Role.CITIZEN; }

    public boolean isGuard() { return role == Role.GUARD; }

    /** Стража народа (для городов). */
    public static NpcType guardOf(Faction f) {
        for (NpcType t : values()) if (t.role == Role.GUARD && t.faction == f) return t;
        return HUMAN_GUARD;
    }

    public static NpcType citizenOf(Faction f) {
        for (NpcType t : values()) if (t.role == Role.CITIZEN && t.faction == f) return t;
        return HUMAN_CITIZEN;
    }

    /** Стихия магов и вождей. */
    public ManaBoltEntity.Element element() {
        return switch (race) {
            case ELF -> ManaBoltEntity.Element.NATURE;
            case DWARF -> ManaBoltEntity.Element.EARTH;
            case ORC -> ManaBoltEntity.Element.FIRE;
            case BEASTKIN -> ManaBoltEntity.Element.LIGHTNING;
            case DEMONKIN -> ManaBoltEntity.Element.SHADOW;
            default -> ManaBoltEntity.Element.ARCANE;
        };
    }

    /** Масштаб модели. */
    public float scale() {
        float base = switch (race) {
            case DWARF -> 0.82f;
            case ORC -> 1.12f;
            case ELF -> 1.04f;
            default -> 1f;
        };
        return role == Role.CHIEF ? base * 1.25f : role == Role.GUARD ? base * 1.15f : base;
    }

    public String nameKey() { return "entity.litrpg." + key; }
}
