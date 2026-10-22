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
        for (; g > 0; g -= 64) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.GOLD_COIN.get(), Math.min(64, g)));
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
            // два предмета не ниже редкого; эпик и выше — уже удача
            for (int i = 0; i < 2; i++) {
                Rarity r = GearGen.rollRarity(rnd, 2.5f + luckBoost);
                if (r.ordinal() < Rarity.RARE.ordinal()) r = Rarity.RARE;
                out.add(GearGen.random(rnd, lvl, r));
            }
            if (dead instanceof GoblinKingEntity && rnd.nextFloat() < 0.3f * findMult) out.add(GearGen.unique("kings_ring"));
            if (dead instanceof LichEntity && rnd.nextFloat() < 0.3f * findMult) out.add(GearGen.unique("lich_amulet"));
            if (dead instanceof LichEntity && rnd.nextFloat() < 0.1f * findMult) out.add(GearGen.unique("archmage_staff"));
            if (dead instanceof com.andrey.litrpg.entity.BossBase boss) {
                // трофеи хранителя: каждый — со своим шансом, редкость не ниже эпической
                for (ItemTemplates.Def def : ItemTemplates.bossLoot(boss.bossId())) {
                    if (rnd.nextFloat() < 0.18f * findMult) {
                        ItemStack t = new ItemStack(com.andrey.litrpg.registry.ModItems.TEMPLATE_ITEMS.get(def.id).get());
                        Rarity r = GearGen.rollRarity(rnd, 2.5f + luckBoost);
                        if (r.ordinal() < Rarity.EPIC.ordinal()) r = Rarity.EPIC;
                        GearGen.apply(t, rnd, Math.max(def.tier, lvl), r);
                        out.add(t);
                    }
                }
                out.addAll(coinStacks(Math.round(lvl * (30 + rnd.nextInt(30)) * (1 + g.get(GearBonus.COIN_FIND) / 100f))));
                if (rnd.nextFloat() < 0.15f) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.ASCENSION_STONE.get()));
                // Искра Божества — только у сильнейших
                float spark = switch (boss.bossId()) {
                    case "archdemon" -> 0.25f;
                    case "flame_lord" -> 0.08f;
                    case "frost_giant" -> 0.05f;
                    case "lich" -> 0.03f;
                    default -> 0f;
                };
                if (rnd.nextFloat() < spark) out.add(new ItemStack(com.andrey.litrpg.registry.ModItems.DIVINE_SPARK.get()));
                if (boss.bossId().equals("archdemon") && rnd.nextFloat() < 0.04f) out.add(GearGen.random(rnd, lvl, Rarity.DIVINE));
            }
            return out;
        }

        MobLevels.Rank rank = MobLevels.getRank(dead);
        float chance;
        float boost;
        switch (rank) {
            case CHAMPION -> { chance = 0.75f; boost = 1.2f; }
            case ELITE -> { chance = 0.25f; boost = 0.6f; }
            default -> { chance = 0.015f + lvl * 0.0005f; boost = 0f; }
        }
        boost += luckBoost + lvl * 0.006f;
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
            if (lvl >= 22 && rnd.nextFloat() < 0.008f * findMult) out.add(GearGen.unique("dawn_blade"));
            if (lvl >= 28 && rnd.nextFloat() < 0.005f * findMult) out.add(GearGen.unique("archmage_staff"));
            if (lvl >= 35 && rnd.nextFloat() < 0.002f * findMult) out.add(GearGen.unique("frost_sorrow"));
            if (lvl >= 40 && rnd.nextFloat() < 0.0008f * findMult) out.add(GearGen.unique("heart_of_system"));
        }
        return out;
    }
}
