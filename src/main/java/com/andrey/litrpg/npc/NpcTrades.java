package com.andrey.litrpg.npc;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.rep.Faction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;

/**
 * Ассортимент торговцев рас. Валюта — монеты Системы. Чем выше репутация,
 * тем больше товаров открыто и тем ниже цены. Предметы снаряжения
 * генерируются один раз на торговца (по его UUID), так что ассортимент постоянный.
 */
public final class NpcTrades {
    private NpcTrades() {}

    /** Множитель цены от репутации. */
    public static float repPriceMult(int rep) {
        return switch (Faction.Tier.of(rep)) {
            case HATED, HOSTILE -> 2.0f;
            case UNFRIENDLY -> 1.5f;
            case NEUTRAL -> 1.2f;
            case FRIENDLY -> 1.0f;
            case HONORED -> 0.85f;
            case REVERED -> 0.7f;
        };
    }

    public static boolean isToken(ItemStack st) {
        return st.is(ModItems.ELF_TOKEN.get()) || st.is(ModItems.DWARF_TOKEN.get()) || st.is(ModItems.ORC_TOKEN.get())
                || st.is(ModItems.BEAST_TOKEN.get()) || st.is(ModItems.DEMON_TOKEN.get());
    }

    /** Товар: что продаётся, базовая цена в меди, с какой ступени отношения доступен. */
    private record Ware(ItemStack stack, int copper, Faction.Tier from) {}

    public static MerchantOffers offers(RaceNpcEntity npc, int rep) {
        RandomSource rnd = RandomSource.create(npc.getUUID().getLeastSignificantBits());
        Faction.Tier tier = Faction.Tier.of(rep);
        float mult = repPriceMult(rep);
        MerchantOffers out = new MerchantOffers();
        for (Ware w : wares(npc, rnd)) {
            if (tier.ordinal() < w.from.ordinal()) continue;
            int price = Math.max(1, Math.round(w.copper * mult));
            ItemStack[] cost = price(price);
            out.add(new MerchantOffer(cost[0], cost[1], w.stack.copy(), 99, 0, 0f));
        }
        return out;
    }

    /** Цена в монетах: до двух стопок (золото + серебро или серебро + медь). */
    private static ItemStack[] price(int copper) {
        int g = copper / 81, s = copper % 81 / 9, c = copper % 9;
        if (g > 0) {
            return new ItemStack[]{new ItemStack(ModItems.GOLD_COIN.get(), Math.min(64, g)),
                    s > 0 ? new ItemStack(ModItems.SILVER_COIN.get(), s) : ItemStack.EMPTY};
        }
        if (s > 0) {
            return new ItemStack[]{new ItemStack(ModItems.SILVER_COIN.get(), s),
                    c > 0 ? new ItemStack(ModItems.COPPER_COIN.get(), c) : ItemStack.EMPTY};
        }
        return new ItemStack[]{new ItemStack(ModItems.COPPER_COIN.get(), Math.max(1, c)), ItemStack.EMPTY};
    }

    private static Ware item(Item item, int count, int copper, Faction.Tier from) {
        return new Ware(new ItemStack(item, count), copper, from);
    }

    /** Снаряжение по шаблону с заданной редкостью; цена — от его стоимости. */
    private static Ware gear(String id, int level, Rarity rarity, RandomSource rnd, Faction.Tier from) {
        ItemStack st = new ItemStack(ModItems.TEMPLATE_ITEMS.get(id).get());
        GearGen.apply(st, rnd, level, rarity);
        GearData d = GearData.get(st);
        return new Ware(st, d == null ? 50 : Math.round(d.value() * 1.6f), from);
    }

    private static Ware potion(net.minecraft.world.item.alchemy.Potion pot, int copper, Faction.Tier from) {
        ItemStack st = net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.POTION), pot);
        return new Ware(st, copper, from);
    }

    /** Товары мастера по его ремеслу (раса добавляет своё снаряжение). */
    private static List<Ware> craftWares(RaceNpcEntity npc, Craft c, RandomSource r) {
        Faction.Tier N = Faction.Tier.NEUTRAL, F = Faction.Tier.FRIENDLY, H = Faction.Tier.HONORED, R = Faction.Tier.REVERED;
        Rarity U = Rarity.UNCOMMON, RA = Rarity.RARE, E = Rarity.EPIC;
        String set = switch (npc.npcType().race) {
            case ELF -> "elven";
            case DWARF -> "dwarven";
            case ORC -> "orcish";
            case BEASTKIN -> "beast";
            case DEMONKIN -> "demonic";
            default -> "knight";
        };
        String weapon = switch (npc.npcType().race) {
            case ELF -> "elven_blade";
            case DWARF -> "dwarven_axe";
            case ORC -> "orc_cleaver";
            case BEASTKIN -> "beast_spear";
            case DEMONKIN -> "demon_blade";
            default -> "knight_sword";
        };
        int lv = switch (npc.npcType().race) {
            case DEMONKIN -> 24;
            case DWARF, ORC -> 16;
            case BEASTKIN, ELF -> 14;
            default -> 10;
        };
        List<Ware> w = new java.util.ArrayList<>();
        switch (c) {
            case BLACKSMITH -> w.addAll(List.of(item(Items.IRON_INGOT, 4, 10, N), item(Items.IRON_NUGGET, 18, 4, N), item(Items.CHAIN, 4, 6, N),
                    item(Items.SHEARS, 1, 8, N), item(Items.IRON_PICKAXE, 1, 30, N), item(Items.IRON_AXE, 1, 28, N), item(Items.IRON_SHOVEL, 1, 14, N),
                    item(Items.BUCKET, 1, 12, N), item(Items.ANVIL, 1, 90, F), item(Items.DIAMOND_PICKAXE, 1, 220, H),
                    item(ModItems.REFORGE_STONE.get(), 1, 115, H), gear("gloves_smith", lv, RA, r, H), item(Items.NETHERITE_SCRAP, 1, 300, R)));
            case ARMORER -> w.addAll(List.of(item(Items.IRON_HELMET, 1, 40, N), item(Items.IRON_BOOTS, 1, 34, N), item(Items.SHIELD, 1, 20, N),
                    item(Items.CHAINMAIL_CHESTPLATE, 1, 50, F), gear(set + "_helmet", lv, U, r, N), gear(set + "_boots", lv, U, r, N),
                    gear(set + "_leggings", lv, U, r, F), gear(set + "_chestplate", lv, U, r, F), gear("gloves_plate", lv, U, r, F),
                    gear(set + "_chestplate", lv + 6, RA, r, H), gear(set + "_helmet", lv + 6, RA, r, H), gear("cloak_guard", lv + 4, RA, r, H),
                    gear(set + "_chestplate", lv + 12, E, r, R)));
            case WEAPONSMITH -> w.addAll(List.of(item(Items.IRON_SWORD, 1, 32, N), item(Items.CROSSBOW, 1, 30, N), item(Items.TRIDENT, 1, 400, R),
                    gear("hunter_dagger", 6, U, r, N), gear(weapon, lv, U, r, N), gear("knight_sword", lv, U, r, F), gear("dwarven_hammer", lv, U, r, F),
                    gear(weapon, lv + 6, RA, r, H), gear("orc_club", lv, RA, r, H), gear(weapon, lv + 12, E, r, R)));
            case JEWELER -> w.addAll(List.of(gear("ring_iron", 4, U, r, N), gear("amulet_wooden", 3, U, r, N), gear("bracelet_copper", 6, U, r, N),
                    gear("charm_coin", 6, U, r, N), gear("ring_luck", lv, U, r, F), gear("amulet_light", lv, U, r, F), gear("bracelet_silver", lv, RA, r, F),
                    gear("ring_sage", lv, RA, r, H), gear("amulet_manakeeper", lv + 4, RA, r, H), gear("charm_stardust", lv + 6, RA, r, H),
                    gear("ring_storm", lv + 10, E, r, R), gear("amulet_shadow", lv + 10, E, r, R), item(Items.AMETHYST_SHARD, 4, 10, N),
                    item(Items.EMERALD, 1, 18, F), item(Items.DIAMOND, 1, 95, H)));
            case TAILOR -> w.addAll(List.of(item(Items.WHITE_WOOL, 8, 6, N), item(Items.RED_WOOL, 8, 7, N), item(Items.BLUE_WOOL, 8, 7, N),
                    item(Items.STRING, 12, 5, N), item(Items.LEATHER_HELMET, 1, 8, N), item(Items.WHITE_BANNER, 1, 10, N),
                    gear("cloak_travel", 4, U, r, N), gear("cloak_student", 6, U, r, N), gear("belt_rope", 3, U, r, N), gear("mage_helmet", lv, U, r, F),
                    gear("mage_chestplate", lv, U, r, F), gear("cloak_hunter", lv, RA, r, H), gear("cloak_night", lv + 8, RA, r, H),
                    gear("cloak_archmage", lv + 12, E, r, R)));
            case HERBALIST -> w.addAll(List.of(item(Items.WHEAT_SEEDS, 16, 2, N), item(Items.BEETROOT_SEEDS, 16, 3, N), item(Items.PUMPKIN_SEEDS, 8, 4, N),
                    item(Items.MELON_SEEDS, 8, 4, N), item(Items.SWEET_BERRIES, 12, 4, N), item(Items.GLOW_BERRIES, 6, 6, N), item(Items.HONEY_BOTTLE, 2, 8, N),
                    item(Items.SUGAR_CANE, 8, 4, N), item(Items.BROWN_MUSHROOM, 6, 4, N), item(Items.RED_MUSHROOM, 6, 4, N), item(Items.CACTUS, 4, 4, N),
                    item(Items.OAK_SAPLING, 4, 4, N), item(Items.CHERRY_SAPLING, 2, 12, F), item(Items.NETHER_WART, 4, 14, F), item(Items.MOSS_BLOCK, 8, 6, N),
                    potion(Potions.REGENERATION, 22, F), gear("charm_acorn", 6, U, r, N), gear("belt_alchemist", lv, RA, r, H)));
            case ALCHEMIST -> w.addAll(List.of(item(Items.GLASS_BOTTLE, 6, 3, N), item(Items.BREWING_STAND, 1, 40, N), item(Items.BLAZE_POWDER, 2, 14, N),
                    item(Items.GLOWSTONE_DUST, 8, 8, N), item(Items.REDSTONE, 12, 6, N), item(Items.FERMENTED_SPIDER_EYE, 2, 10, F),
                    item(ModItems.MANA_POTION.get(), 1, 12, N), potion(Potions.HEALING, 18, N), potion(Potions.STRENGTH, 28, F),
                    potion(Potions.SWIFTNESS, 22, F), potion(Potions.FIRE_RESISTANCE, 30, F), potion(Potions.NIGHT_VISION, 20, N),
                    potion(Potions.STRONG_HEALING, 45, H), potion(Potions.INVISIBILITY, 40, H), gear("belt_flasks", lv, RA, r, H),
                    item(ModItems.SCROLL_OF_OBLIVION.get(), 1, 300, R), item(ModItems.TOME_OF_EXPERIENCE.get(), 1, 150, H)));
            case BAKER -> w.addAll(List.of(item(Items.BREAD, 6, 3, N), item(Items.COOKIE, 8, 3, N), item(Items.PUMPKIN_PIE, 3, 5, N),
                    item(Items.CAKE, 1, 12, N), item(Items.WHEAT, 16, 3, N), item(Items.SUGAR, 8, 3, N), item(Items.EGG, 8, 3, N), item(Items.HONEY_BOTTLE, 2, 8, F)));
            case BUTCHER -> w.addAll(List.of(item(Items.COOKED_BEEF, 6, 5, N), item(Items.COOKED_PORKCHOP, 6, 5, N), item(Items.COOKED_MUTTON, 6, 4, N),
                    item(Items.COOKED_CHICKEN, 6, 4, N), item(Items.RABBIT_STEW, 1, 6, N), item(Items.LEATHER, 4, 6, N), item(Items.LEAD, 1, 8, N),
                    item(Items.WHEAT, 16, 3, N), item(Items.CARROT, 12, 3, N)));
            case FISHMONGER -> w.addAll(List.of(item(Items.COOKED_COD, 6, 4, N), item(Items.COOKED_SALMON, 6, 5, N), item(Items.FISHING_ROD, 1, 10, N),
                    item(Items.TROPICAL_FISH, 2, 6, N), item(Items.PUFFERFISH, 1, 8, N), item(Items.KELP, 16, 2, N), item(Items.NAUTILUS_SHELL, 1, 60, H),
                    item(Items.OAK_BOAT, 1, 10, N), gear("charm_shell", 6, U, r, N), item(Items.HEART_OF_THE_SEA, 1, 600, R)));
            case INNKEEPER -> w.addAll(List.of(item(Items.MUSHROOM_STEW, 1, 4, N), item(Items.BEETROOT_SOUP, 1, 4, N), item(Items.BAKED_POTATO, 6, 3, N),
                    item(Items.COOKED_PORKCHOP, 4, 4, N), item(Items.HONEY_BOTTLE, 2, 8, N), item(Items.MILK_BUCKET, 1, 8, N), item(Items.RED_BED, 1, 18, N),
                    item(Items.FILLED_MAP, 1, 20, N), item(Items.GOLDEN_CARROT, 4, 18, F), item(Items.GOLDEN_APPLE, 1, 60, H)));
            case BUILDER -> w.addAll(List.of(item(Items.STONE_BRICKS, 32, 8, N), item(Items.COBBLESTONE, 64, 6, N), item(Items.OAK_PLANKS, 32, 6, N),
                    item(Items.SPRUCE_LOG, 16, 8, N), item(Items.GLASS, 16, 8, N), item(Items.BRICKS, 16, 10, N), item(Items.LANTERN, 4, 8, N),
                    item(Items.SCAFFOLDING, 16, 6, N), item(Items.LADDER, 16, 4, N), item(Items.OAK_DOOR, 2, 4, N), item(Items.DEEPSLATE_TILES, 32, 12, F),
                    item(Items.QUARTZ_BLOCK, 16, 24, H)));
            case LEATHERWORKER -> w.addAll(List.of(item(Items.LEATHER, 6, 8, N), item(Items.SADDLE, 1, 40, F), item(Items.ITEM_FRAME, 2, 6, N),
                    item(Items.LEATHER_CHESTPLATE, 1, 14, N), item(Items.BOOK, 2, 8, N), gear("gloves_leather", 6, U, r, N), gear("belt_hunter", lv, U, r, N),
                    gear("hunter_chestplate", lv, U, r, F), gear("hunter_boots", lv, U, r, F), gear("cloak_wolfhide", lv, RA, r, H),
                    gear("gloves_thief", lv + 6, RA, r, H), gear("belt_treasure", lv + 10, E, r, R)));
            case FLETCHER -> w.addAll(List.of(item(Items.ARROW, 16, 4, N), item(Items.BOW, 1, 14, N), item(Items.CROSSBOW, 1, 26, N),
                    item(Items.FEATHER, 12, 4, N), item(Items.FLINT, 8, 4, N), item(Items.SPECTRAL_ARROW, 8, 14, F), gear("ring_archer", lv, U, r, N),
                    gear("gloves_archer", lv, U, r, N), gear("bracelet_archer", lv, RA, r, F), gear("charm_hawk_feather", lv, RA, r, H),
                    gear("hunter_helmet", lv + 6, RA, r, H)));
            case SCRIBE -> w.addAll(List.of(item(Items.PAPER, 16, 4, N), item(Items.BOOK, 3, 8, N), item(Items.WRITABLE_BOOK, 1, 10, N),
                    item(Items.MAP, 1, 10, N), item(Items.FILLED_MAP, 1, 20, N), item(Items.COMPASS, 1, 20, N), item(Items.INK_SAC, 6, 4, N),
                    item(Items.BOOKSHELF, 1, 18, F), item(Items.LECTERN, 1, 20, F), item(ModItems.TOME_OF_EXPERIENCE.get(), 1, 135, H),
                    gear("amulet_scholar", lv, RA, r, H), gear("cloak_student", lv, U, r, F)));
            case FARMER -> w.addAll(List.of(item(Items.WHEAT, 20, 3, N), item(Items.CARROT, 12, 3, N), item(Items.POTATO, 12, 3, N),
                    item(Items.APPLE, 6, 4, N), item(Items.MELON_SLICE, 12, 3, N), item(Items.EGG, 8, 3, N), item(Items.MILK_BUCKET, 1, 8, N),
                    item(Items.HAY_BLOCK, 4, 6, N), item(Items.BONE_MEAL, 12, 4, N), item(Items.COMPOSTER, 1, 6, N), item(Items.IRON_HOE, 1, 12, N)));
            case HEALER -> w.addAll(List.of(potion(Potions.HEALING, 16, N), potion(Potions.REGENERATION, 22, N),
                    item(Items.GOLDEN_APPLE, 1, 55, F), item(Items.GLISTERING_MELON_SLICE, 2, 12, N), item(Items.HONEY_BOTTLE, 2, 7, N),
                    item(Items.TOTEM_OF_UNDYING, 1, 500, R), gear("amulet_light", lv, RA, r, F), gear("charm_bell", lv, U, r, N),
                    gear("cloak_paladin", lv + 8, RA, r, H)));
            case BANKER -> w.addAll(List.of(item(ModItems.SILVER_COIN.get(), 1, 9, N), item(ModItems.GOLD_COIN.get(), 1, 81, N),
                    item(Items.EMERALD, 1, 18, N), item(Items.GOLD_INGOT, 1, 16, N), item(Items.LAPIS_LAZULI, 8, 8, N), item(Items.DIAMOND, 1, 90, F),
                    item(ModItems.ELF_TOKEN.get(), 1, 34, F), item(ModItems.DWARF_TOKEN.get(), 1, 34, F), item(ModItems.ORC_TOKEN.get(), 1, 34, F),
                    item(ModItems.BEAST_TOKEN.get(), 1, 34, F), item(ModItems.DEMON_TOKEN.get(), 1, 44, F), gear("ring_merchant", lv, RA, r, H),
                    gear("charm_coin", lv, RA, r, H), gear("belt_treasure", lv + 8, E, r, R), item(ModItems.ASCENSION_STONE.get(), 1, 510, R)));
            case STABLEMASTER -> w.addAll(List.of(item(Items.SADDLE, 1, 36, N), item(Items.LEAD, 2, 8, N), item(Items.HAY_BLOCK, 4, 6, N),
                    item(Items.APPLE, 6, 4, N), item(Items.GOLDEN_CARROT, 4, 18, F), item(Items.IRON_HORSE_ARMOR, 1, 60, F), item(Items.GOLDEN_HORSE_ARMOR, 1, 110, H),
                    item(Items.DIAMOND_HORSE_ARMOR, 1, 260, R), item(Items.NAME_TAG, 1, 30, F), gear("charm_horseshoe", lv, U, r, N)));
            case MINER -> w.addAll(List.of(item(Items.COAL, 16, 5, N), item(Items.TORCH, 16, 3, N), item(Items.RAW_IRON, 6, 12, N), item(Items.RAW_COPPER, 12, 8, N),
                    item(Items.RAW_GOLD, 3, 14, F), item(Items.IRON_PICKAXE, 1, 30, N), item(Items.RAIL, 16, 10, N), item(Items.TNT, 2, 20, H),
                    gear("ring_miner", lv, U, r, N), gear("gloves_miner", lv, U, r, N), gear("dwarven_helmet", lv, RA, r, H)));
        }
        return w;
    }


    private static List<Ware> wares(RaceNpcEntity npc, RandomSource r) {
        if (npc.npcType().isArtisan() && npc.craft() != null) return craftWares(npc, npc.craft(), r);
        Faction.Tier N = Faction.Tier.NEUTRAL, F = Faction.Tier.FRIENDLY, H = Faction.Tier.HONORED, R = Faction.Tier.REVERED;
        Rarity U = Rarity.UNCOMMON, RA = Rarity.RARE, E = Rarity.EPIC;
        return switch (npc.npcType().race) {
            case ELF -> List.of(
                    item(Items.ARROW, 16, 4, N), item(ModItems.MANA_POTION.get(), 1, 12, N), item(ModItems.ELF_TOKEN.get(), 1, 30, F),
                    gear("ring_archer", 8, U, r, N), gear("charm_hawk_feather", 6, U, r, N), gear("cloak_hunter", 8, U, r, F),
                    gear("elven_helmet", 18, U, r, F), gear("elven_boots", 18, U, r, F), gear("elven_leggings", 18, U, r, H),
                    gear("elven_chestplate", 18, U, r, H), gear("elven_blade", 20, RA, r, H), gear("elven_staff", 20, RA, r, H),
                    gear("amulet_wind", 12, RA, r, H), gear("ring_storm", 24, E, r, R), gear("cloak_shadow", 20, E, r, R));
            case DWARF -> List.of(
                    item(Items.TORCH, 16, 3, N), item(Items.IRON_INGOT, 4, 10, N), item(Items.DIAMOND, 1, 90, F), item(ModItems.DWARF_TOKEN.get(), 1, 30, F),
                    gear("ring_miner", 8, U, r, N), gear("gloves_miner", 10, U, r, N), gear("belt_apprentice", 5, U, r, N),
                    gear("dwarven_helmet", 18, U, r, F), gear("dwarven_boots", 18, U, r, F), gear("dwarven_leggings", 18, U, r, H),
                    gear("dwarven_chestplate", 18, U, r, H), gear("dwarven_axe", 20, RA, r, H), gear("dwarven_hammer", 22, RA, r, H),
                    gear("charm_rune_guard", 12, RA, r, H), gear("gloves_smith", 16, E, r, R), gear("belt_titan", 28, E, r, R),
                    item(ModItems.REFORGE_STONE.get(), 1, 120, H), item(ModItems.ASCENSION_STONE.get(), 1, 500, R));
            case ORC -> List.of(
                    item(Items.COOKED_BEEF, 6, 5, N), item(Items.LEATHER, 4, 6, N), item(ModItems.ORC_TOKEN.get(), 1, 30, F),
                    gear("ring_brute", 12, U, r, N), gear("charm_bear_claw", 8, U, r, N), gear("bracelet_spiked", 12, U, r, F),
                    gear("orcish_helmet", 16, U, r, F), gear("orcish_boots", 16, U, r, F), gear("orcish_leggings", 16, U, r, H),
                    gear("orcish_chestplate", 16, U, r, H), gear("orc_club", 16, RA, r, F), gear("orc_cleaver", 18, RA, r, H),
                    gear("belt_warrior", 14, RA, r, H), gear("amulet_warlord", 22, E, r, R), gear("ring_vampire", 20, E, r, R));
            case HUMAN -> List.of(
                    item(Items.BREAD, 6, 3, N), item(Items.ARROW, 16, 4, N), item(Items.TORCH, 16, 3, N),
                    item(ModItems.MANA_POTION.get(), 1, 12, N), item(Items.FILLED_MAP, 1, 20, N), item(Items.IRON_INGOT, 4, 10, F),
                    gear("ring_iron", 4, U, r, N), gear("amulet_wooden", 2, U, r, N), gear("belt_wide", 8, U, r, N),
                    gear("knight_helmet", 12, U, r, F), gear("knight_boots", 12, U, r, F), gear("knight_leggings", 12, U, r, F),
                    gear("knight_chestplate", 12, U, r, H), gear("knight_sword", 12, RA, r, F), gear("hunter_dagger", 6, U, r, N),
                    gear("cloak_guard", 12, RA, r, H), gear("amulet_light", 10, RA, r, H), gear("mage_chestplate", 10, U, r, F),
                    item(ModItems.TOME_OF_EXPERIENCE.get(), 1, 140, H), item(ModItems.REFORGE_STONE.get(), 1, 110, H),
                    item(ModItems.ASCENSION_STONE.get(), 1, 520, R));
            case BEASTKIN -> List.of(
                    item(Items.RABBIT_HIDE, 4, 4, N), item(Items.COOKED_CHICKEN, 6, 4, N), item(ModItems.BEAST_TOKEN.get(), 1, 30, F),
                    gear("charm_wolf_fang", 6, U, r, N), gear("charm_rabbit", 3, U, r, N), gear("amulet_trapper", 10, U, r, F),
                    gear("beast_helmet", 16, U, r, F), gear("beast_boots", 16, U, r, F), gear("beast_leggings", 16, U, r, H),
                    gear("beast_chestplate", 16, U, r, H), gear("beast_spear", 18, RA, r, H), gear("beast_claws", 18, RA, r, H),
                    gear("cloak_wolfhide", 12, RA, r, H), gear("gloves_claw", 18, E, r, R), gear("bracelet_fangs", 16, E, r, R));
            default -> List.of(
                    item(Items.BLAZE_POWDER, 2, 12, N), item(Items.MAGMA_CREAM, 2, 10, N), item(ModItems.DEMON_TOKEN.get(), 1, 40, F),
                    gear("ring_netherforged", 25, U, r, N), gear("charm_fire_crystal", 16, U, r, N), gear("amulet_flame", 14, RA, r, F),
                    gear("demonic_helmet", 26, RA, r, F), gear("demonic_boots", 26, RA, r, F), gear("demonic_leggings", 26, RA, r, H),
                    gear("demonic_chestplate", 26, RA, r, H), gear("demon_blade", 28, RA, r, H), gear("demon_staff", 28, RA, r, H),
                    gear("amulet_void", 30, E, r, R), gear("charm_phoenix_feather", 30, E, r, R),
                    item(ModItems.TOME_OF_EXPERIENCE.get(), 1, 150, H));
        };
    }
}
