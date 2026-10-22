package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.rep.Faction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.EnumMap;
import java.util.Map;

/**
 * Подземелья мира: у каждого свой уровень, хранитель-босс и последствия
 * его гибели для народов. Структуры генерирует tools/gen_dungeons.py.
 */
public enum Dungeon {
    RUINS("ruins", "Руины Стражей", 8, "ruin_golem", 0x7FB3C8,
            "Развалины древней крепости. Её до сих пор охраняет каменный голем, пробуждённый чужой магией.",
            Faction.DWARVES, 30, Faction.KINGDOM, 25),
    ROTGROVE("rotgrove", "Гниющая Роща", 11, "rotting_ent", 0x8DA34A,
            "Священная роща, отравленная скверной. Её древний хранитель обезумел и душит корнями всё живое.",
            Faction.ELVES, 45, Faction.WILDS, 35, Faction.BEASTKIN, 15),
    SPIDER_NEST("spider_nest", "Паучье Логово", 14, "spider_queen", 0x7A3F9A,
            "Пещеры, затянутые паутиной. В глубине откладывает яйца Королева Арахна.",
            Faction.ELVES, 40, Faction.BEASTKIN, 25, Faction.KINGDOM, 15),
    GOBLIN_FORT("goblin_fort", "Гоблинская Крепость", 15, "goblin_king", 0x6FA34A,
            "Частокол и норы гоблинов. Король гоблинов собирает здесь награбленное.",
            Faction.DWARVES, 50, Faction.KINGDOM, 30, Faction.GOBLINS, -120),
    MIRE("mire", "Проклятая Топь", 20, "swamp_hag", 0x4E7D5B,
            "Гнилые мостки над чёрной водой. Ведьма Морра варит здесь проклятия для целых деревень.",
            Faction.KINGDOM, 40, Faction.BEASTKIN, 30, Faction.ELVES, 15),
    LABYRINTH("labyrinth", "Лабиринт Астериона", 25, "minotaur", 0xC8A060,
            "Древний лабиринт под песками. Его страж — минотавр, не знавший поражений.",
            Faction.ORCS, 60, Faction.KINGDOM, 20, Faction.DWARVES, 20),
    CRYPT("crypt", "Склеп Лича", 30, "lich", 0x8E44AD,
            "Усыпальница забытых королей. Лич копит здесь армию мёртвых.",
            Faction.KINGDOM, 60, Faction.ELVES, 40, Faction.UNDEAD, -200),
    FROST_CITADEL("frost_citadel", "Ледяная Цитадель", 38, "frost_giant", 0x9ED8F0,
            "Крепость изо льда на краю мира. Ледяной великан Имир ждёт конца времён.",
            Faction.DWARVES, 60, Faction.BEASTKIN, 45, Faction.KINGDOM, 25),
    CRUCIBLE("crucible", "Пламенное Горнило", 45, "flame_lord", 0xFF7A30,
            "Кузня первородного огня. Владыка Игнис плавит в ней души смельчаков.",
            Faction.DWARVES, 70, Faction.KINGDOM, 40, Faction.DEMONKIN, -60),
    ASH_CITADEL("ash_citadel", "Цитадель Пепла", 55, "archdemon", 0xC0303A,
            "Твердыня Архидемона Баал'Зара. Отсюда Пепел расползается по миру. Последний рубеж.",
            Faction.KINGDOM, 120, Faction.ELVES, 120, Faction.DWARVES, 120, Faction.ORCS, 100,
            Faction.BEASTKIN, 120, Faction.DEMONKIN, -300),
    NETHER_HEART("nether_heart", "Сердце Преисподней", 50, "flame_lord", 0xE04020,
            "Раскалённое сердце Преисподней под городом демонидов. Здесь первородное пламя рождает своих владык.",
            Faction.DWARVES, 60, Faction.PIGLINS, 40, Faction.DEMONKIN, -40),

    // ---- большие подземелья (рейды): несколько хранителей подряд, за каждым — рунная преграда ----
    KHAZ_FORGE("khaz_forge", "Глубины Кхаз-Дарума: Кузня", 32, "bronzebeard", 0xD8A040,
            "Затерянный холд дварфов. У входа ещё стучит механический страж, охраняя погасшие горны.",
            Faction.DWARVES, 60, Faction.KINGDOM, 20),
    KHAZ_DEEP("khaz_deep", "Глубины Кхаз-Дарума: Нора", 33, "ghaz", 0x8A6A5A,
            "Глубже — нора червя, прогрызшего холд насквозь.",
            Faction.DWARVES, 70),
    KHAZ_THRONE("khaz_throne", "Глубины Кхаз-Дарума: Трон", 35, "torbald", 0x80FFB0,
            "Тронный зал. Король-под-Горой не отдал своё золото даже смерти.",
            Faction.DWARVES, 150, Faction.KINGDOM, 40, Faction.UNDEAD, -100),
    SPIRE_TWINS("spire_twins", "Шпиль Бездны: Зал Близнецов", 58, "sol_twin", 0xFFE070,
            "Нижний ярус Шпиля, павшего с неба. Его стерегут близнецы света и тьмы.",
            Faction.KINGDOM, 60, Faction.ELVES, 60),
    SPIRE_TIME("spire_time", "Шпиль Бездны: Часовня", 60, "chronophage", 0xC8A0FF,
            "Средний ярус: время здесь течёт вспять, и кто-то им питается.",
            Faction.ELVES, 80, Faction.DWARVES, 40),
    SPIRE_EYE("spire_eye", "Шпиль Бездны: Око", 62, "xalathar", 0xC02040,
            "Вершина Шпиля. Отсюда Бездна смотрит на мир.",
            Faction.KINGDOM, 150, Faction.ELVES, 150, Faction.DWARVES, 150, Faction.ORCS, 120, Faction.BEASTKIN, 150);

    public final String key;
    public final String title;
    public final int level;
    public final String boss;
    public final int color;
    public final String desc;
    public final Map<Faction, Integer> rep = new EnumMap<>(Faction.class);

    Dungeon(String key, String title, int level, String boss, int color, String desc, Object... rep) {
        this.key = key;
        this.title = title;
        this.level = level;
        this.boss = boss;
        this.color = color;
        this.desc = desc;
        for (int i = 0; i + 1 < rep.length; i += 2) this.rep.put((Faction) rep[i], (Integer) rep[i + 1]);
    }

    /** Большое подземелье, в которое входит этот хранитель (null — обычное). */
    public String raid() {
        if (key.startsWith("khaz_")) return "khazdarum";
        if (key.startsWith("spire_")) return "abyss_spire";
        return null;
    }

    /** Ключ места на карте (у больших подземелий — общий для всех хранителей). */
    public String poiKey() {
        return raid() != null ? raid() : key;
    }

    public ResourceKey<Structure> structure() {
        return ResourceKey.create(Registries.STRUCTURE, LitRPG.id(raid() != null ? "raid_" + raid() : "dungeon_" + key));
    }

    /** Подземелье по ключу места на карте (у больших — первый хранитель). */
    public static Dungeon byPoi(String extra) {
        for (Dungeon d : values()) if (d.poiKey().equals(extra)) return d;
        return null;
    }

    public static Dungeon byKey(String key) {
        for (Dungeon d : values()) if (d.key.equals(key)) return d;
        return null;
    }

    public static Dungeon byBoss(String bossId) {
        for (Dungeon d : values()) if (d.boss.equals(bossId)) return d;
        return null;
    }
}
