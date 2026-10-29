/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.OpenShopPacket;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.network.PacketDistributor;

public final class Shop {
    public static final int GEAR = 6;

    private Shop() {
    }

    private static String boughtKey(int stage) {
        return "litrpg_shop_" + stage;
    }

    private static boolean bought(ServerPlayer p, int stage, int id) {
        for(int i : p.getPersistentData().getIntArray(boughtKey(stage))) {
            if (i == id) {
                return true;
            }
        }

        return false;
    }

    private static void markBought(ServerPlayer p, int stage, int id) {
        int[] old = p.getPersistentData().getIntArray(boughtKey(stage));
        int[] nw = Arrays.copyOf(old, old.length + 1);
        nw[old.length] = id;
        p.getPersistentData().put(boughtKey(stage), new IntArrayTag(nw));
    }

    public static List<Offer> offers(ServerPlayer p, int stage) {
        Layout.Stage s = Layout.get().stage(stage);
        int lvl = s == null ? 5 : s.maxLevel();
        List<Offer> out = new ArrayList();
        PlayerStats ps = StatsManager.get(p);
        RandomSource rnd = RandomSource.create(p.getUUID().getLeastSignificantBits() * 31L + (long)stage * 7919L + (long)(ps.getPlayerClass() == null ? 0 : ps.getPlayerClass().key.hashCode()));
        Rarity[] rar = new Rarity[]{Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.RARE, Rarity.RARE, Rarity.EPIC};
        int[] base = new int[]{25, 25, 70, 70, 70, 220};

        for(int i = 0; i < 6; ++i) {
            ItemStack st = GearGen.personal(rnd, lvl - rnd.nextInt(3), rar[i], ps);
            int price = base[i] * stage + rnd.nextInt(10) * stage;
            out.add(new Offer(i, bought(p, stage, i) ? ItemStack.EMPTY : st, price, true, "", ""));
        }

        out.add(new Offer(10, new ItemStack((ItemLike)ModItems.HEALTH_POTION.get()), 45 * stage, false, "", ""));
        out.add(new Offer(11, new ItemStack((ItemLike)ModItems.MANA_POTION.get()), 35 * stage, false, "", ""));
        out.add(new Offer(12, new ItemStack((ItemLike)ModItems.REFORGE_STONE.get()), 60 * stage, false, "", ""));
        out.add(new Offer(13, new ItemStack((ItemLike)ModItems.ASCENSION_STONE.get()), 180 * stage, false, "", ""));
        out.add(new Offer(14, new ItemStack((ItemLike)ModItems.SCROLL_OF_OBLIVION.get()), 90 * stage, false, "", ""));
        out.add(new Offer(15, new ItemStack(Items.ARROW, 64), 6 * stage, false, "", ""));
        out.add(new Offer(16, new ItemStack((ItemLike)ModItems.DIVINE_SPARK.get()), 1200 * stage, false, "", ""));

        // услуги привала: работают с вещью в руке
        ItemStack held = p.getMainHandItem();
        com.andrey.litrpg.gear.GearData hd = com.andrey.litrpg.gear.GearData.get(held);
        out.add(new Offer(21, ItemStack.EMPTY, 0, false, "anvil", "Наковальня: камни перековки и возвышения"));
        if (hd != null) {
            out.add(new Offer(22, ItemStack.EMPTY, reforgePrice(held, hd), false, "reforge",
                    "Перековать «" + held.getHoverName().getString() + "»: все свойства заменятся новыми"));
            if (hd.level < ps.getLevel()) {
                out.add(new Offer(23, ItemStack.EMPTY, upgradePrice(hd), false, "upgrade",
                        "Поднять уровень вещи до " + Math.min(ps.getLevel(), hd.level + LEVEL_STEP) + " (дорого)"));
            }
        }
        if (ps.hasClass()) {
            out.add(new Offer(20, ItemStack.EMPTY, 150 * stage, false, "class", "Сменить класс и роль (уровень и вещи остаются)"));
        }

        return out;
    }

    /** На сколько поднимается уровень вещи за один раз. */
    public static final int LEVEL_STEP = 5;
    /** Сколько раз вещь уже перековывали (цена растёт). */
    private static final String REFORGE_TAG = "litrpg_reforges";

    private static int reforges(ItemStack st) {
        return st.getOrCreateTag().getInt(REFORGE_TAG);
    }

    /** Цена перековки: каждая следующая дороже, чтобы не крутить рулетку бесконечно. */
    public static int reforgePrice(ItemStack st, com.andrey.litrpg.gear.GearData d) {
        int base = 40 + d.level * 6 + d.rarity.ordinal() * 45;
        return Math.round(base * (float)Math.pow(1.6, Math.min(6, reforges(st))));
    }

    /** Цена подъёма уровня: намеренно очень дорого — это долгая цель для монет. */
    public static int upgradePrice(com.andrey.litrpg.gear.GearData d) {
        return 300 + d.level * 90 + d.rarity.ordinal() * 400;
    }

    public static void open(ServerPlayer p, TraderEntity t) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenShopPacket(t.getId(), t.stage(), Wallet.total(p), offers(p, t.stage())));
        p.level().playSound((Player)null, t.blockPosition(), SoundEvents.WANDERING_TRADER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    public static void buy(ServerPlayer p, int entityId, int offerId) {
        Entity e = p.level().getEntity(entityId);
        if (e instanceof TraderEntity t) {
            if (!(e.distanceTo(p) > 8.0F)) {
                Offer o = null;

                for(Offer x : offers(p, t.stage())) {
                    if (x.id() == offerId) {
                        o = x;
                    }
                }

                if (o != null && (!o.once() || !o.stack().isEmpty())) {
                    if (!Wallet.pay(p, o.price())) {
                        p.displayClientMessage(Component.literal("Не хватает монет: нужно " + Wallet.format(o.price())).withStyle(ChatFormatting.RED), true);
                        return;
                    }

                    if (o.action().equals("anvil")) {
                        // наковальни в подземелье нет, а камни перековки и возвышения работают
                        // только на ней — торговец даёт свою
                        p.openMenu(new net.minecraft.world.SimpleMenuProvider(
                                (id, inv, pl) -> new net.minecraft.world.inventory.AnvilMenu(id, inv,
                                        net.minecraft.world.inventory.ContainerLevelAccess.create(p.level(), t.blockPosition())),
                                Component.literal("Наковальня привала")));
                        return;
                    }

                    if (o.action().equals("reforge")) {
                        ItemStack st = p.getMainHandItem();
                        com.andrey.litrpg.gear.GearData d = com.andrey.litrpg.gear.GearData.get(st);
                        if (d == null) {
                            return;
                        }

                        int done = st.getOrCreateTag().getInt(REFORGE_TAG);
                        st.removeTagKey(com.andrey.litrpg.gear.GearData.TAG);
                        st.resetHoverName();
                        GearGen.apply(st, p.getRandom(), d.level, d.rarity);
                        st.getOrCreateTag().putInt(REFORGE_TAG, done + 1);
                        p.level().playSound((Player)null, t.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 1.0F, 1.2F);
                        StatsManager.message(p, Component.literal("Перековано: ").append(st.getHoverName()).withStyle(ChatFormatting.GOLD));
                        open(p, t);
                        return;
                    }

                    if (o.action().equals("upgrade")) {
                        ItemStack st = p.getMainHandItem();
                        com.andrey.litrpg.gear.GearData d = com.andrey.litrpg.gear.GearData.get(st);
                        if (d == null) {
                            return;
                        }

                        int lvl = Math.min(StatsManager.get(p).getLevel(), d.level + LEVEL_STEP);
                        st.removeTagKey(com.andrey.litrpg.gear.GearData.TAG);
                        st.resetHoverName();
                        GearGen.apply(st, p.getRandom(), lvl, d.rarity);
                        p.level().playSound((Player)null, t.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 1.0F, 0.8F);
                        StatsManager.message(p, Component.literal("Уровень вещи поднят до " + lvl + ": ")
                                .append(st.getHoverName()).withStyle(ChatFormatting.GOLD));
                        open(p, t);
                        return;
                    }

                    if (o.action().equals("class")) {
                        StatsManager.resetClass(p);
                        StatsManager.message(p, Component.literal("Торговец сжигает ваш знак класса. Выберите роль и класс заново.").withStyle(ChatFormatting.GOLD));
                        p.closeContainer();
                        return;
                    }

                    if (o.once()) {
                        markBought(p, t.stage(), o.id());
                    }

                    RunLoot.give(p, o.stack().copy());
                    p.level().playSound((Player)null, t.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);
                    p.displayClientMessage(Component.literal("Куплено: ").append(o.stack().getHoverName()).withStyle(ChatFormatting.GOLD), true);
                    open(p, t);
                    return;
                }

                return;
            }
        }

    }

    public static record Offer(int id, ItemStack stack, int price, boolean once, String action, String label) {
    }
}
