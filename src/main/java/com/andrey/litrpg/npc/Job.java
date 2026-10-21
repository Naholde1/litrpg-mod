package com.andrey.litrpg.npc;

import com.andrey.litrpg.stats.Race;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Занятие горожанина (не мастера): пахарь, пастушка, гончар, углежог…
 * От занятия зависит, что житель держит в руках и чем торгует (см. NpcTrades.jobWares).
 */
public enum Job {
    PLOWMAN("пахарь", "крестьянка", Items.IRON_HOE, Race.HUMAN),
    SHEPHERD("пастух", "пастушка", Items.SHEARS, Race.HUMAN, Race.BEASTKIN),
    WOODCUTTER("дровосек", "сборщица хвороста", Items.IRON_AXE, Race.HUMAN, Race.DWARF),
    FISHER("рыбак", "рыбачка", Items.FISHING_ROD, Race.HUMAN, Race.BEASTKIN, Race.ELF),
    LAUNDRESS("водонос", "прачка", Items.BUCKET, Race.HUMAN),
    POTTER("гончар", "гончарша", Items.FLOWER_POT, Race.HUMAN, Race.DWARF),
    COOPER("бондарь", "бондарша", Items.BARREL, Race.HUMAN, Race.DWARF),
    CHANDLER("свечник", "свечница", Items.CANDLE, Race.HUMAN, Race.DEMONKIN),
    MINSTREL("менестрель", "певунья", Items.NOTE_BLOCK, Race.HUMAN),
    FLOWER("садовник", "цветочница", Items.POPPY, Race.HUMAN, Race.ELF),
    CHARCOAL("углежог", "торговка углём", Items.CHARCOAL, Race.HUMAN, Race.DWARF, Race.ORC, Race.DEMONKIN),
    MASON("каменотёс", "торговка камнем", Items.STONE_BRICKS, Race.HUMAN, Race.DWARF),
    BREWER("пивовар", "пивоварша", Items.HONEY_BOTTLE, Race.HUMAN, Race.DWARF),
    BEEKEEPER("пчеловод", "пчеловодка", Items.HONEYCOMB, Race.HUMAN, Race.ELF),
    BEGGAR("нищий", "нищенка", Items.BOWL, Race.HUMAN),
    PEDDLER("коробейник", "коробейница", Items.LEAD, Race.HUMAN, Race.ORC, Race.BEASTKIN),
    GARDENER("садовник", "садовница", Items.OAK_SAPLING, Race.ELF),
    BERRYPICKER("сборщик ягод", "сборщица ягод", Items.SWEET_BERRIES, Race.ELF, Race.BEASTKIN),
    WOODCARVER("резчик по дереву", "резчица по дереву", Items.BOWL, Race.ELF, Race.BEASTKIN),
    WEAVER("ткач", "ткачиха", Items.STRING, Race.ELF, Race.HUMAN),
    BARD("бард", "сказительница", Items.NOTE_BLOCK, Race.ELF),
    PROSPECTOR("рудознатец", "рудознатица", Items.IRON_PICKAXE, Race.DWARF),
    LAMPLIGHTER("фонарщик", "фонарщица", Items.LANTERN, Race.DWARF, Race.HUMAN),
    MUSHROOM("грибовод", "грибоводка", Items.BROWN_MUSHROOM, Race.DWARF, Race.BEASTKIN),
    SKINNER("свежевальщик", "свежевальщица", Items.IRON_AXE, Race.ORC, Race.BEASTKIN),
    SWINEHERD("свинопас", "свинарка", Items.CARROT_ON_A_STICK, Race.ORC),
    BONECARVER("костерез", "костерезка", Items.BONE, Race.ORC, Race.BEASTKIN),
    DRUMMER("барабанщик", "барабанщица", Items.STICK, Race.ORC),
    TRAPPER("зверолов", "звероловка", Items.LEAD, Race.BEASTKIN),
    SULFUR("серодобытчик", "серодобытчица", Items.IRON_PICKAXE, Race.DEMONKIN),
    FUNGUS("грибовод", "грибоводка", Items.CRIMSON_FUNGUS, Race.DEMONKIN),
    OBSIDIAN("резчик обсидиана", "резчица обсидиана", Items.DIAMOND_PICKAXE, Race.DEMONKIN),
    GLASS("стеклодув", "стеклодувша", Items.GLASS_BOTTLE, Race.DEMONKIN);

    public final String title;
    public final String titleF;
    public final Item tool;
    private final EnumSet<Race> races;

    Job(String title, String titleF, Item tool, Race first, Race... more) {
        this.title = title;
        this.titleF = titleF;
        this.tool = tool;
        this.races = EnumSet.of(first, more);
    }

    public String title(boolean female) {
        return female ? titleF : title;
    }

    public static Job random(Race race, RandomSource r) {
        List<Job> ok = new ArrayList<>();
        for (Job j : values()) if (j.races.contains(race)) ok.add(j);
        if (ok.isEmpty()) return PEDDLER;
        return ok.get(r.nextInt(ok.size()));
    }

    public static Job byKey(String key) {
        for (Job j : values()) if (j.name().equalsIgnoreCase(key)) return j;
        return null;
    }
}
