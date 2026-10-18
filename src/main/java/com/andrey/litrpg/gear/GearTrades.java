package com.andrey.litrpg.gear;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Торговля предметами Системы: оружейники, бронники и инструментальщики
 * продают случайную экипировку (чем опытнее житель — тем лучше),
 * священник — украшения, странствующий торговец — камни улучшения.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class GearTrades {
    private GearTrades() {}

    /** Сделка: житель продаёт случайный предмет Системы. */
    private record GearListing(int villagerLevel, int itemLevel, float boost) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource rnd) {
            int lvl = itemLevel + rnd.nextInt(4);
            Rarity r = GearGen.rollRarity(rnd, boost, false);
            ItemStack item = GearGen.random(rnd, lvl, r);
            int price = Math.min(64, 3 + r.ordinal() * r.ordinal() * 5 + lvl / 2 + rnd.nextInt(4));
            return new MerchantOffer(new ItemStack(Items.EMERALD, price), item, 2, 10 + villagerLevel * 5, 0.05f);
        }
    }

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession prof = event.getType();
        if (prof == VillagerProfession.WEAPONSMITH || prof == VillagerProfession.ARMORER || prof == VillagerProfession.TOOLSMITH) {
            event.getTrades().get(2).add(new GearListing(2, 4, 0.2f));
            event.getTrades().get(3).add(new GearListing(3, 10, 0.5f));
            event.getTrades().get(4).add(new GearListing(4, 16, 0.8f));
            event.getTrades().get(5).add(new GearListing(5, 24, 1.3f));
        }
        if (prof == VillagerProfession.CLERIC) {
            event.getTrades().get(3).add((trader, rnd) -> new MerchantOffer(new ItemStack(Items.EMERALD, 12),
                    new ItemStack(rnd.nextBoolean() ? ModItems.COPPER_RING.get() : ModItems.JADE_AMULET.get()), 2, 15, 0.05f));
            event.getTrades().get(5).add((trader, rnd) -> new MerchantOffer(new ItemStack(Items.EMERALD, 28),
                    new ItemStack(ModItems.SOUL_SHARD.get(), 2), new ItemStack(ModItems.ASCENSION_STONE.get()), 2, 30, 0.05f));
        }
        if (prof == VillagerProfession.LIBRARIAN) {
            event.getTrades().get(4).add((trader, rnd) -> new MerchantOffer(new ItemStack(Items.EMERALD, 24),
                    new ItemStack(ModItems.TOME_OF_EXPERIENCE.get()), 3, 20, 0.05f));
        }
    }

    @SubscribeEvent
    public static void onWanderer(WandererTradesEvent event) {
        event.getGenericTrades().add((trader, rnd) -> new MerchantOffer(new ItemStack(Items.EMERALD, 10),
                new ItemStack(ModItems.REFORGE_STONE.get()), 3, 10, 0.05f));
        event.getRareTrades().add((trader, rnd) -> new MerchantOffer(new ItemStack(Items.EMERALD, 40),
                new ItemStack(ModItems.ASCENSION_STONE.get()), 1, 20, 0.05f));
        event.getRareTrades().add(new GearListing(5, 12, 1.0f));
    }
}
