package com.andrey.litrpg.npc;

import com.andrey.litrpg.gear.LootRolls;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Кошелёк игрока: монеты в инвентаре (1 серебряк = 9 медяков, 1 золотой = 81). Плата за услуги — со сдачей. */
public final class Wallet {
    private Wallet() {}

    private static int value(ItemStack s) {
        if (s.is(ModItems.COPPER_COIN.get())) return 1;
        if (s.is(ModItems.SILVER_COIN.get())) return 9;
        if (s.is(ModItems.GOLD_COIN.get())) return 81;
        return 0;
    }

    /** Сколько у игрока денег в медяках. */
    public static int total(Player p) {
        int sum = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            sum += value(s) * s.getCount();
        }
        return sum;
    }

    /** Заплатить: монеты забираются, сдача возвращается мелкими. @return false — денег не хватает */
    public static boolean pay(ServerPlayer p, int copper) {
        if (copper <= 0) return true;
        int have = total(p);
        if (have < copper) return false;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (value(inv.getItem(i)) > 0) inv.setItem(i, ItemStack.EMPTY);
        }
        for (ItemStack c : LootRolls.coinStacks(have - copper)) {
            if (!inv.add(c)) p.drop(c, false);
        }
        p.containerMenu.broadcastChanges();
        return true;
    }
}
