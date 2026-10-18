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
            Faction.BEASTKIN, 120, Faction.DEMONKIN, -300);

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

    public ResourceKey<Structure> structure() {
        return ResourceKey.create(Registries.STRUCTURE, LitRPG.id("dungeon_" + key));
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
