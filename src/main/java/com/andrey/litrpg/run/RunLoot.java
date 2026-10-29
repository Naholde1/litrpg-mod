/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.event.MobLevels.Rank;
import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.ItemTemplates;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;

public final class RunLoot {
    private RunLoot() {
    }

    public static List<ItemStack> coins(int copper) {
        List<ItemStack> out = new ArrayList();
        int g = copper / 81;
        int s = copper % 81 / 9;

        int c;
        for(c = copper % 9; g > 0; g -= 64) {
            out.add(new ItemStack((ItemLike)ModItems.GOLD_COIN.get(), Math.min(64, g)));
        }

        if (s > 0) {
            out.add(new ItemStack((ItemLike)ModItems.SILVER_COIN.get(), s));
        }

        if (c > 0) {
            out.add(new ItemStack((ItemLike)ModItems.COPPER_COIN.get(), c));
        }

        return out;
    }

    public static void give(ServerPlayer p, ItemStack st) {
        if (!st.isEmpty()) {
            if (!p.getInventory().add(st.copy())) {
                p.drop(st, false);
                p.displayClientMessage(Component.literal("Сумка полна — добыча у ваших ног.").withStyle(ChatFormatting.RED), true);
            }

        }
    }

    public static void item(ServerPlayer p, ItemStack st, String from) {
        if (!st.isEmpty()) {
            p.sendSystemMessage(Component.literal("✦ ").withStyle(ChatFormatting.GOLD).append(Component.literal(from + ": ").withStyle(ChatFormatting.GRAY)).append(st.getHoverName()));
            give(p, st);
        }
    }

    private static float luck(ServerPlayer p) {
        PlayerStats s = StatsManager.get(p);
        return (float)s.getStat(Stat.LUCK) * 0.01F + s.gear().get(GearBonus.LOOT) / 100.0F;
    }

    private static Rarity atLeast(Rarity r, Rarity min) {
        return r.ordinal() < min.ordinal() ? min : r;
    }

    private static int itemLevel(Layout.Stage s, RandomSource rnd) {
        return s.minLevel() + rnd.nextInt(Math.max(1, s.maxLevel() - s.minLevel() + 1));
    }

    public static void mob(ServerPlayer p, Layout.Stage s, LivingEntity dead) {
        RandomSource rnd = p.getRandom();
        MobLevels.Rank rank = MobLevels.getRank(dead);
        boolean mini = dead.getPersistentData().getBoolean("litrpg_miniboss");
        float luck = luck(p);
        int copper = (1 + rnd.nextInt(3)) * (1 + s.n());
        copper *= mini ? 25 : (rank == Rank.CHAMPION ? 12 : (rank == Rank.ELITE ? 4 : 1));

        for(ItemStack c : coins(copper)) {
            give(p, c);
        }

        float chance = mini ? 1.0F : (rank == Rank.CHAMPION ? 1.0F : (rank == Rank.ELITE ? 0.3F : 0.04F));
        chance *= 1.0F + luck;
        if (rnd.nextFloat() < chance) {
            float boost = 0.2F * (float)s.n() + (mini ? 1.6F : (rank == Rank.CHAMPION ? 1.2F : (rank == Rank.ELITE ? 0.6F : 0.0F))) + luck;
            Rarity r = GearGen.rollRarity(rnd, boost, false);
            if (mini) {
                r = atLeast(r, Rarity.RARE);
            }

            item(p, GearGen.personal(rnd, itemLevel(s, rnd), r, StatsManager.get(p)), dead.getName().getString());
        }

        if (rnd.nextFloat() < (mini ? 0.5F : (rank != Rank.NORMAL ? 0.08F : 0.01F)) * (1.0F + luck)) {
            give(p, new ItemStack(rnd.nextBoolean() ? (ItemLike)ModItems.HEALTH_POTION.get() : (ItemLike)ModItems.MANA_POTION.get()));
        }

        if (mini && rnd.nextFloat() < 0.35F) {
            give(p, new ItemStack((ItemLike)ModItems.REFORGE_STONE.get()));
        }

    }

    public static void boss(ServerPlayer p, Layout.Stage s, Layout.Boss b, String bossType, boolean finalBoss) {
        RandomSource rnd = p.getRandom();
        PlayerStats ps = StatsManager.get(p);
        int level = s.maxLevel();

        for(ItemStack c : coins(60 * s.n() * (finalBoss ? 3 : 2) + rnd.nextInt(40 * s.n()))) {
            give(p, c);
        }

        String id = bossType.contains(":") ? bossType.substring(bossType.indexOf(58) + 1) : bossType;
        List<ItemTemplates.Def> pool = new ArrayList();

        for(ItemTemplates.Def d : ItemTemplates.ITEMS) {
            if (id.equals(d.boss)) {
                pool.add(d);
            }
        }

        Rarity r = GearGen.rollRarity(rnd, 2.2F + 0.3F * (float)s.n() + luck(p), true);
        r = atLeast(r, finalBoss ? Rarity.EPIC : Rarity.RARE);
        ItemStack loot;
        if (!pool.isEmpty()) {
            ItemTemplates.Def d = pickFor(pool, ps, rnd);
            loot = new ItemStack((ItemLike)((RegistryObject)ModItems.TEMPLATE_ITEMS.get(d.id)).get());
            GearGen.apply(loot, rnd, level, atLeast(r, d.minRarity));
        } else {
            loot = GearGen.personal(rnd, level, r, ps);
        }

        item(p, loot, b.id().isEmpty() ? "Хранитель" : "Трофей хранителя");
        // часть классового набора — сверх обычного трофея: ради неё этого хранителя и бьют
        ItemTemplates.Def piece = classSetPiece(pool, ps);
        if (piece != null) {
            ItemStack part = new ItemStack((ItemLike) ((RegistryObject) ModItems.TEMPLATE_ITEMS.get(piece.id)).get());
            GearGen.apply(part, rnd, level, atLeast(r, piece.minRarity));
            item(p, part, piece.cls != null ? "Легендарная вещь класса" : "Часть облачения класса");
        }
        if (finalBoss && rnd.nextFloat() < 0.5F + luck(p)) {
            item(p, GearGen.personal(rnd, level, atLeast(GearGen.rollRarity(rnd, 2.0F, false), Rarity.RARE), ps), "Сокровищница хранителя");
        }

        if (rnd.nextFloat() < 0.4F) {
            give(p, new ItemStack((ItemLike)ModItems.ASCENSION_STONE.get()));
        }

        give(p, new ItemStack((ItemLike)ModItems.HEALTH_POTION.get()));
    }

    private static ItemTemplates.Def pickFor(List<ItemTemplates.Def> pool, PlayerStats ps, RandomSource rnd) {
        List<ItemTemplates.Def> fit = new ArrayList();
        String role = ps.getRole() == null ? "" : ps.getRole().key;

        for(ItemTemplates.Def d : pool) {
            // чужое классовое облачение не предлагаем никому: оно выдаётся отдельно и только своему
            if (isClassSet(d)) {
                continue;
            }

            if (d.roles == null || d.roles.isEmpty() || d.roles.contains(role)) {
                fit.add(d);
            }
        }

        List<ItemTemplates.Def> from = fit.isEmpty() ? pool : fit;
        return (ItemTemplates.Def)from.get(rnd.nextInt(from.size()));
    }

    /** Вещь, привязанная к классу: часть облачения (набор назван ключом класса) или легендарка. */
    private static boolean isClassSet(ItemTemplates.Def d) {
        return d.cls != null || d.set != null && com.andrey.litrpg.stats.PlayerClass.byKey(d.set) != null;
    }

    /** Чья это классовая вещь. */
    private static String classOf(ItemTemplates.Def d) {
        if (d.cls != null) {
            return d.cls;
        }

        return d.set != null && com.andrey.litrpg.stats.PlayerClass.byKey(d.set) != null ? d.set : null;
    }

    /** Классовая вещь этого игрока среди добычи хранителя, если она там есть. */
    private static ItemTemplates.Def classSetPiece(List<ItemTemplates.Def> pool, PlayerStats ps) {
        if (ps.getPlayerClass() == null) {
            return null;
        }

        String key = ps.getPlayerClass().key;

        for(ItemTemplates.Def d : pool) {
            if (key.equals(classOf(d))) {
                return d;
            }
        }

        return null;
    }

    /** Ячейка с ключом тайника в инвентаре, или -1. */
    private static int keySlot(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (p.getInventory().getItem(i).is(ModItems.CACHE_KEY.get())) return i;
        }
        return -1;
    }

    public static boolean cache(ServerPlayer p, int tier, BlockPos pos) {
        Layout.Stage s = Layout.get().stageAt((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        if (s == null) {
            p.displayClientMessage(Component.literal("Тайник пуст.").withStyle(ChatFormatting.GRAY), true);
            return false;
        } else {
            RandomSource rnd = p.getRandom();
            PlayerStats ps = StatsManager.get(p);
            Rarity min = tier >= 2 ? Rarity.EPIC : (tier == 1 ? Rarity.RARE : Rarity.UNCOMMON);
            Rarity r = atLeast(GearGen.rollRarity(rnd, 0.6F + 0.25F * (float)s.n() + (float)tier * 0.6F + luck(p), tier >= 2), min);
            item(p, GearGen.personal(rnd, itemLevel(s, rnd), r, ps), tier >= 2 ? "Секретный тайник" : "Тайник");

            for(ItemStack c : coins((10 + rnd.nextInt(15)) * s.n() * (1 + tier))) {
                give(p, c);
            }

            if (tier >= 1) {
                give(p, new ItemStack(rnd.nextBoolean() ? (ItemLike)ModItems.HEALTH_POTION.get() : (ItemLike)ModItems.MANA_POTION.get()));
            }

            if (tier >= 2) {
                give(p, new ItemStack(rnd.nextBoolean() ? (ItemLike)ModItems.ASCENSION_STONE.get() : (ItemLike)ModItems.SCROLL_OF_OBLIVION.get()));
            }

            // ключ тайника из лавки: ещё одна вещь, на ступень реже, и вдвое монет
            int slot = keySlot(p);
            if (slot >= 0) {
                p.getInventory().getItem(slot).shrink(1);
                Rarity better = atLeast(GearGen.rollRarity(rnd, 1.2F + 0.25F * (float)s.n() + (float)tier * 0.6F + luck(p), true),
                        tier >= 1 ? Rarity.EPIC : Rarity.RARE);
                item(p, GearGen.personal(rnd, itemLevel(s, rnd), better, ps), "Ключ тайника");
                for(ItemStack c : coins((10 + rnd.nextInt(15)) * s.n() * (1 + tier))) {
                    give(p, c);
                }
                p.level().playSound(null, pos, net.minecraft.sounds.SoundEvents.CHAIN_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1.4f);
                p.displayClientMessage(Component.literal("Ключ провернулся и сломался — тайник отдал больше.").withStyle(ChatFormatting.GOLD), true);
            }

            return true;
        }
    }
}
