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

    private static List<Ware> wares(RaceNpcEntity npc, RandomSource r) {
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
