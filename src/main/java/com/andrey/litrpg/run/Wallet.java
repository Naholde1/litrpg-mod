/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Wallet {
    private Wallet() {
    }

    private static int value(ItemStack s) {
        if (s.is((Item)ModItems.COPPER_COIN.get())) {
            return 1;
        } else if (s.is((Item)ModItems.SILVER_COIN.get())) {
            return 9;
        } else {
            return s.is((Item)ModItems.GOLD_COIN.get()) ? 81 : 0;
        }
    }

    public static int total(Player p) {
        int sum = 0;
        Inventory inv = p.getInventory();

        for(int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack s = inv.getItem(i);
            sum += value(s) * s.getCount();
        }

        return sum;
    }

    public static String format(int copper) {
        int g = copper / 81;
        int s = copper % 81 / 9;
        int c = copper % 9;
        StringBuilder sb = new StringBuilder();
        if (g > 0) {
            sb.append(g).append(" зол.");
        }

        if (s > 0) {
            sb.append(sb.length() > 0 ? " " : "").append(s).append(" сер.");
        }

        if (c > 0 || sb.length() == 0) {
            sb.append(sb.length() > 0 ? " " : "").append(c).append(" мед.");
        }

        return sb.toString();
    }

    public static boolean pay(ServerPlayer p, int copper) {
        if (copper <= 0) {
            return true;
        } else {
            int have = total(p);
            if (have < copper) {
                return false;
            } else {
                Inventory inv = p.getInventory();

                for(int i = 0; i < inv.getContainerSize(); ++i) {
                    if (value(inv.getItem(i)) > 0) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }
                }

                for(ItemStack c : RunLoot.coins(have - copper)) {
                    if (!inv.add(c)) {
                        p.drop(c, false);
                    }
                }

                p.containerMenu.broadcastChanges();
                return true;
            }
        }
    }
}
