package com.andrey.litrpg.quest;

import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.world.Dungeon;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/** Генератор поручений: ежедневные задания Системы и просьбы народов. */
public final class QuestGen {
    private QuestGen() {}

    private record Want(String item, int count, int minLevel) {}

    // ---------------- что нужно Системе ----------------

    private static final QuestTarget[] EARLY = {QuestTarget.ANY, QuestTarget.ZOMBIE, QuestTarget.SKELETON, QuestTarget.SPIDER,
            QuestTarget.CREEPER, QuestTarget.SLIME};
    private static final QuestTarget[] MID = {QuestTarget.UNDEAD, QuestTarget.WITCH, QuestTarget.RAIDER, QuestTarget.GOBLIN,
            QuestTarget.PHANTOM, QuestTarget.ELITE, QuestTarget.SPIDER};
    private static final QuestTarget[] LATE = {QuestTarget.ENDER, QuestTarget.NETHER, QuestTarget.ELITE, QuestTarget.UNDEAD, QuestTarget.RAIDER};

    private static final Want[] SYSTEM_WANTS = {
            new Want("minecraft:rotten_flesh", 16, 1), new Want("minecraft:bone", 12, 1), new Want("minecraft:string", 12, 1),
            new Want("minecraft:coal", 16, 1), new Want("minecraft:oak_log", 32, 1), new Want("minecraft:wheat", 24, 1),
            new Want("minecraft:leather", 6, 1), new Want("minecraft:iron_ingot", 10, 10), new Want("minecraft:gunpowder", 8, 10),
            new Want("minecraft:spider_eye", 8, 10), new Want("minecraft:gold_ingot", 6, 12), new Want("minecraft:redstone", 24, 12),
            new Want("minecraft:lapis_lazuli", 16, 14), new Want("minecraft:ender_pearl", 4, 18), new Want("minecraft:diamond", 3, 25),
            new Want("minecraft:blaze_rod", 6, 28), new Want("minecraft:emerald", 6, 22), new Want("minecraft:ghast_tear", 2, 32),
            new Want("minecraft:quartz", 24, 26), new Want("minecraft:obsidian", 10, 24),
    };

    // ---------------- что нужно народам ----------------

    private static QuestTarget[] enemies(Faction f) {
        return switch (f) {
            case ELVES -> new QuestTarget[]{QuestTarget.SPIDER, QuestTarget.UNDEAD, QuestTarget.RAIDER, QuestTarget.WITCH, QuestTarget.PHANTOM};
            case DWARVES -> new QuestTarget[]{QuestTarget.GOBLIN, QuestTarget.CREEPER, QuestTarget.SPIDER, QuestTarget.UNDEAD, QuestTarget.NETHER};
            case ORCS -> new QuestTarget[]{QuestTarget.ELITE, QuestTarget.RAIDER, QuestTarget.ANY, QuestTarget.SKELETON};
            case BEASTKIN -> new QuestTarget[]{QuestTarget.RAIDER, QuestTarget.WITCH, QuestTarget.PHANTOM, QuestTarget.ZOMBIE, QuestTarget.SLIME};
            case DEMONKIN -> new QuestTarget[]{QuestTarget.ENDER, QuestTarget.RAIDER, QuestTarget.WITCH, QuestTarget.ELITE};
            default -> new QuestTarget[]{QuestTarget.ANY, QuestTarget.UNDEAD};
        };
    }

    private static Want[] wants(Faction f) {
        return switch (f) {
            case ELVES -> new Want[]{new Want("minecraft:oak_sapling", 8, 1), new Want("minecraft:bone_meal", 16, 1),
                    new Want("minecraft:glow_berries", 8, 5), new Want("minecraft:string", 16, 1), new Want("minecraft:honeycomb", 4, 8),
                    new Want("minecraft:amethyst_shard", 8, 12)};
            case DWARVES -> new Want[]{new Want("minecraft:iron_ingot", 12, 1), new Want("minecraft:coal", 32, 1),
                    new Want("minecraft:gold_ingot", 8, 8), new Want("minecraft:copper_ingot", 24, 1), new Want("minecraft:diamond", 2, 18),
                    new Want("minecraft:lapis_lazuli", 16, 10)};
            case ORCS -> new Want[]{new Want("minecraft:beef", 12, 1), new Want("minecraft:leather", 10, 1),
                    new Want("minecraft:iron_ingot", 8, 6), new Want("minecraft:bone", 16, 1), new Want("minecraft:gunpowder", 8, 10)};
            case BEASTKIN -> new Want[]{new Want("minecraft:rabbit_hide", 6, 1), new Want("minecraft:leather", 10, 1),
                    new Want("minecraft:feather", 16, 1), new Want("minecraft:salmon", 8, 1), new Want("minecraft:sweet_berries", 16, 1)};
            case DEMONKIN -> new Want[]{new Want("minecraft:netherrack", 64, 1), new Want("minecraft:soul_sand", 16, 10),
                    new Want("minecraft:blaze_rod", 4, 18), new Want("minecraft:magma_cream", 6, 18), new Want("minecraft:gold_ingot", 8, 5),
                    new Want("minecraft:ghast_tear", 1, 25)};
            default -> new Want[]{new Want("minecraft:bread", 16, 1)};
        };
    }

    // ---------------- генерация ----------------

    public static List<Quest> system(RandomSource rnd, int level, QuestLog log) {
        List<Quest> out = new ArrayList<>();
        QuestTarget[] pool = level < 10 ? EARLY : level < 25 ? concat(EARLY, MID) : concat(MID, LATE);
        out.add(kill(rnd, level, pool[rnd.nextInt(pool.length)], log, null));
        out.add(rnd.nextBoolean() ? kill(rnd, level, pool[rnd.nextInt(pool.length)], log, null) : collect(rnd, level, SYSTEM_WANTS, log, null));
        out.add(collect(rnd, level, SYSTEM_WANTS, log, null));
        return out;
    }

    public static List<Quest> faction(RandomSource rnd, int level, Faction f, QuestLog log) {
        List<Quest> out = new ArrayList<>();
        QuestTarget[] e = enemies(f);
        out.add(kill(rnd, level, e[rnd.nextInt(e.length)], log, f));
        out.add(collect(rnd, level, wants(f), log, f));
        Quest boss = bossHunt(rnd, level, f, log);
        out.add(boss != null ? boss : kill(rnd, level, e[rnd.nextInt(e.length)], log, f));
        return out;
    }

    private static Quest base(QuestLog log, Faction f) {
        Quest q = new Quest();
        q.id = "q" + log.nextId++;
        q.source = f == null ? Quest.Source.SYSTEM : Quest.Source.FACTION;
        q.faction = f;
        return q;
    }

    private static Quest kill(RandomSource rnd, int level, QuestTarget t, QuestLog log, Faction f) {
        Quest q = base(log, f);
        q.type = Quest.Type.KILL;
        q.target = t.key;
        boolean rare = t == QuestTarget.ELITE || t == QuestTarget.ENDER || t == QuestTarget.WITCH || t == QuestTarget.PHANTOM;
        q.need = rare ? 3 + rnd.nextInt(3) : 8 + rnd.nextInt(8) + level / 6;
        q.title = f == null ? pickTitle(rnd, "Зачистка", "Охота Системы", "Истребление", "Угроза миру")
                : pickTitle(rnd, "Просьба народа", "Защита земель", "Кровная месть", "Охота для своих");
        q.desc = f == null ? "Система отмечает угрозу в округе. Уничтожь врагов — награда начислится сразу."
                : "«Эти твари не дают нам житья. Помоги — и мы отблагодарим».";
        reward(rnd, q, level, rare ? 1.3f : 1f);
        return q;
    }

    private static Quest collect(RandomSource rnd, int level, Want[] wants, QuestLog log, Faction f) {
        List<Want> ok = new ArrayList<>();
        for (Want w : wants) if (w.minLevel <= level) ok.add(w);
        Want w = ok.get(rnd.nextInt(ok.size()));
        Quest q = base(log, f);
        q.type = Quest.Type.COLLECT;
        q.target = w.item;
        q.need = w.count;
        q.title = f == null ? pickTitle(rnd, "Запасы", "Ресурсы для Системы", "Сбор") : pickTitle(rnd, "Припасы", "Торговая просьба", "Нужда поселения");
        q.desc = f == null ? "Системе нужны ресурсы. Собери и сдай их кнопкой «Сдать» — предметы будут изъяты."
                : "«Помоги нам припасами — в долгу не останемся». Сдать можно кнопкой «Сдать».";
        reward(rnd, q, level, 0.8f);
        return q;
    }

    private static Quest bossHunt(RandomSource rnd, int level, Faction f, QuestLog log) {
        List<Dungeon> ok = new ArrayList<>();
        for (Dungeon d : Dungeon.values()) {
            if (d.rep.getOrDefault(f, 0) > 0 && d.level <= level + 8 && d.level >= level - 10) ok.add(d);
        }
        if (ok.isEmpty() || rnd.nextFloat() < 0.35f) return null;
        Dungeon d = ok.get(rnd.nextInt(ok.size()));
        Quest q = base(log, f);
        q.type = Quest.Type.BOSS;
        q.target = d.boss;
        q.need = 1;
        q.title = "Голова хранителя";
        q.desc = "«Хранитель подземелья «" + d.title + "» (ур. " + d.level + ") угрожает нам. Принеси весть о его гибели — и мы не забудем».";
        q.xp = d.level * 50;
        q.coins = d.level * 10;
        q.rep = 60 + rnd.nextInt(30);
        q.gearRarity = Rarity.EPIC.ordinal();
        q.gearLevel = d.level;
        return q;
    }

    private static void reward(RandomSource rnd, Quest q, int level, float mult) {
        q.xp = Math.round((60 + level * 30) * mult);
        q.coins = Math.round((10 + level * 4) * mult);
        if (q.source == Quest.Source.FACTION) q.rep = 15 + rnd.nextInt(16);
        if (rnd.nextFloat() < 0.5f) {
            Rarity r = GearGen.rollRarity(rnd, 0.6f + level * 0.01f, false);
            if (r == Rarity.COMMON) r = Rarity.UNCOMMON;
            q.gearRarity = r.ordinal();
            q.gearLevel = Math.max(1, level);
        }
    }

    private static String pickTitle(RandomSource rnd, String... options) {
        return options[rnd.nextInt(options.length)];
    }

    private static QuestTarget[] concat(QuestTarget[] a, QuestTarget[] b) {
        QuestTarget[] out = new QuestTarget[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
