package com.andrey.litrpg.quest;

import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.world.Dungeon;

/**
 * Сюжетная цепочка «Путь Избранного»: Система ведёт игрока от первых
 * шагов через все подземелья к Цитадели Пепла.
 */
public final class Story {
    private Story() {}

    private record Step(String title, String desc, Quest.Type type, String target, int need, int xp, int coins, int gear, int gearLvl,
                        String bonus, int bonusN) {}

    private static Step boss(Dungeon d, String title, String desc, int gear, String bonus, int bonusN) {
        return new Step(title, desc, Quest.Type.BOSS, d.boss, 1, d.level * 70, d.level * 12, gear, d.level, bonus, bonusN);
    }

    private static final Step[] STEPS = {
            new Step("Пробуждение", "Система приветствует тебя, Избранный. Докажи, что способен выжить в этом мире: убей пятерых монстров.",
                    Quest.Type.KILL, "monsters", 5, 120, 20, -1, 1, "litrpg:mana_potion", 2),
            new Step("Первые шаги", "Сила приходит с опытом. Охоться, исследуй, становись сильнее — достигни 5 уровня.",
                    Quest.Type.LEVEL, "", 5, 200, 40, Rarity.UNCOMMON.ordinal(), 5, "", 0),
            new Step("Народы мира", "В лесах, горах и пустошах живут эльфы, дварфы, орки, зверолюды и демониды. Найди их поселение и поговори с жителем — у народов есть свои поручения.",
                    Quest.Type.MEET, "", 1, 300, 60, -1, 1, "litrpg:tome_of_experience", 1),
            new Step("Испытание силы", "Элитные монстры и чемпионы — достойные противники. Одолей троих.",
                    Quest.Type.KILL, "elites", 3, 400, 80, Rarity.RARE.ordinal(), 8, "", 0),
            boss(Dungeon.RUINS, "Руины Стражей", "Древний каменный голем пробудился в руинах крепости. Найди подземелье (ур. 8) и упокой стража. Кнопка «Где?» укажет путь.",
                    Rarity.RARE.ordinal(), "litrpg:reforge_stone", 1),
            new Step("Путь класса", "На 10 уровне Система предложит тебе класс, подходящий твоему стилю. Достигни 10 уровня и сделай выбор.",
                    Quest.Type.LEVEL, "", 10, 600, 100, Rarity.RARE.ordinal(), 10, "", 0),
            boss(Dungeon.ROTGROVE, "Гниль в лесу", "Скверна отравила священную рощу, и её хранитель обезумел. Победи Гниющего Энта (ур. 11).",
                    Rarity.RARE.ordinal(), "litrpg:ascension_stone", 1),
            boss(Dungeon.SPIDER_NEST, "Королева паутины", "Пауки расползаются по округе. Их мать, Королева Арахна, ждёт в глубине логова (ур. 14).",
                    Rarity.EPIC.ordinal(), "", 0),
            boss(Dungeon.GOBLIN_FORT, "Гоблинский трон", "Король гоблинов собрал орду в своей крепости (ур. 15). Сверши правосудие.",
                    Rarity.EPIC.ordinal(), "", 0),
            boss(Dungeon.MIRE, "Проклятая Топь", "Ведьма Морра насылает порчу на деревни. Найди её хижину на болотах (ур. 20).",
                    Rarity.EPIC.ordinal(), "litrpg:ascension_stone", 1),
            boss(Dungeon.LABYRINTH, "Лабиринт", "Под песками скрыт лабиринт минотавра Астериона (ур. 25). Никто ещё не выходил оттуда живым.",
                    Rarity.EPIC.ordinal(), "", 0),
            boss(Dungeon.CRYPT, "Склеп Лича", "Лич копит армию мёртвых в усыпальнице королей (ур. 30). Останови его.",
                    Rarity.EPIC.ordinal(), "litrpg:tome_of_experience", 2),
            boss(Dungeon.FROST_CITADEL, "Вечная мерзлота", "Ледяной великан Имир пробуждается в своей цитадели (ур. 38). Зима не должна прийти.",
                    Rarity.LEGENDARY.ordinal(), "", 0),
            boss(Dungeon.CRUCIBLE, "Горнило", "Владыка Игнис раздувает первородный огонь (ур. 45). Погаси его пламя.",
                    Rarity.LEGENDARY.ordinal(), "litrpg:ascension_stone", 2),
            boss(Dungeon.ASH_CITADEL, "Повелитель Пепла", "Последний рубеж. Архидемон Баал'Зар ждёт в Цитадели Пепла (ур. 55). Судьба мира — в твоих руках.",
                    Rarity.LEGENDARY.ordinal(), "litrpg:divine_spark", 1),
    };

    public static int length() {
        return STEPS.length;
    }

    /** Задание сюжета номер step или null, если сюжет пройден. */
    public static Quest make(int step) {
        if (step < 0 || step >= STEPS.length) return null;
        Step s = STEPS[step];
        Quest q = new Quest();
        q.id = "story_" + step;
        q.source = Quest.Source.STORY;
        q.type = s.type;
        q.target = s.target;
        q.need = s.need;
        q.title = "Глава " + (step + 1) + ". " + s.title;
        q.desc = s.desc;
        q.xp = s.xp;
        q.coins = s.coins;
        q.gearRarity = s.gear;
        q.gearLevel = s.gearLvl;
        q.bonusItem = s.bonus;
        q.bonusCount = s.bonusN;
        return q;
    }
}
