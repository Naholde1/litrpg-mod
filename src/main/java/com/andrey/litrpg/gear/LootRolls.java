package com.andrey.litrpg.gear;

import com.andrey.litrpg.entity.GoblinKingEntity;
import com.andrey.litrpg.entity.LichEntity;
import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Сколько и каких предметов падает с моба. */
public final class LootRolls {
    private LootRolls() {}

    /** Разложить сумму в медных монетах на золото, серебро и медь. */
    public static List<ItemStack> coinStacks(int copper) {
        List<ItemStack> out = new ArrayList<>();
        int g = copper / 81, s = copper % 81 / 9, c = copper % 9;
        if (g > 0) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.GOLD_COIN.get(), Math.min(64, g)));
        if (s > 0) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.SILVER_COIN.get(), s));
        if (c > 0) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.COPPER_COIN.get(), c));
        return out;
    }

    public static List<ItemStack> mobDrops(LivingEntity dead, ServerPlayer player) {
        List<ItemStack> out = new ArrayList<>();
        if (!(dead instanceof Enemy) && !(dead instanceof com.andrey.litrpg.npc.RaceNpcEntity)) return out;
        RandomSource rnd = dead.getRandom();
        PlayerStats s = StatsManager.get(player);
        PlayerGear g = s.gear();
        int lvl = MobLevels.getLevel(dead);
        float findMult = (1 + g.get(GearBonus.LOOT) / 100f + s.getStat(Stat.LUCK) * 0.01f) * (g.has(Passive.TREASURE_HUNTER) ? 2f : 1f);
        float luckBoost = s.getStat(Stat.LUCK) * 0.01f;

        if (dead instanceof LitBoss) {
            for (int i = 0; i < 3; i++) {
                Rarity r = GearGen.rollRarity(rnd, 2.5f + luckBoost);
                if (r.ordinal() < Rarity.EPIC.ordinal()) r = Rarity.EPIC;
                out.add(GearGen.random(rnd, lvl, r));
            }
            if (dead instanceof GoblinKingEntity && rnd.nextFloat() < 0.5f * findMult) out.add(GearGen.unique("kings_ring"));
            if (dead instanceof LichEntity && rnd.nextFloat() < 0.5f * findMult) out.add(GearGen.unique("lich_amulet"));
            if (dead instanceof LichEntity && rnd.nextFloat() < 0.25f * findMult) out.add(GearGen.unique("archmage_staff"));
            return out;
        }

        MobLevels.Rank rank = MobLevels.getRank(dead);
        float chance;
        float boost;
        switch (rank) {
            case CHAMPION -> { chance = 1f; boost = 1.2f; }
            case ELITE -> { chance = 0.4f; boost = 0.6f; }
            default -> { chance = 0.025f + lvl * 0.001f; boost = 0f; }
        }
        boost += luckBoost + lvl * 0.01f;
        int rolls = rank == MobLevels.Rank.CHAMPION ? 2 : 1;
        for (int i = 0; i < rolls; i++) {
            if (rnd.nextFloat() < chance * findMult) {
                int itemLvl = Math.max(1, lvl + rnd.nextInt(5) - 2);
                out.add(GearGen.random(rnd, itemLvl, GearGen.rollRarity(rnd, boost)));
            }
            chance *= 0.4f;
        }
        // монеты: медь, с элит и чемпионов — серебро и золото
        float coinMult = 1 + g.get(GearBonus.COIN_FIND) / 100f;
        if (rnd.nextFloat() < 0.35f) {
            int copper = Math.round((1 + rnd.nextInt(3) + lvl / 4f) * coinMult * (rank == MobLevels.Rank.CHAMPION ? 12 : rank == MobLevels.Rank.ELITE ? 4 : 1));
            out.addAll(coinStacks(copper));
        }
        // «мелочёвка»: украшения падают часто — их можно носить или продать
        float trinket = (rank == MobLevels.Rank.CHAMPION ? 0.6f : rank == MobLevels.Rank.ELITE ? 0.3f : 0.06f + lvl * 0.001f) * findMult;
        if (rnd.nextFloat() < trinket) {
            ItemStack t = GearGen.trinket(rnd, lvl, boost * 0.5f);
            if (!t.isEmpty()) out.add(t);
        }
        // редчайшие уникальные предметы с чемпионов
        if (rank == MobLevels.Rank.CHAMPION) {
            if (lvl >= 22 && rnd.nextFloat() < 0.02f * findMult) out.add(GearGen.unique("dawn_blade"));
            if (lvl >= 28 && rnd.nextFloat() < 0.015f * findMult) out.add(GearGen.unique("archmage_staff"));
            if (lvl >= 35 && rnd.nextFloat() < 0.01f * findMult) out.add(GearGen.unique("frost_sorrow"));
            if (lvl >= 40 && rnd.nextFloat() < 0.005f * findMult) out.add(GearGen.unique("heart_of_system"));
        }
        return out;
    }
}
