/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class Rewards {
    private static final Map<Integer, double[]> WEIGHTS = new HashMap();
    static final float TRASH = 0.5F;
    static final float BOSSES = 0.45F;
    static final float PUZZLES = 0.05F;
    private static final float[] BOSS_W = new float[]{1.0F, 1.3F, 1.8F, 2.2F};

    private Rewards() {
    }

    public static long budget(int n) {
        int from = n == 1 ? 1 : 10 * (n - 1);
        long sum = 0L;

        for(int l = from; l < 10 * n; ++l) {
            sum += (long)PlayerStats.xpForNextLevel(l);
        }

        return sum;
    }

    static double mobWeight(String rank, boolean named) {
        if (named) {
            return (double)15.0F;
        } else {
            double var10000;
            switch (rank) {
                case "ELITE" -> var10000 = (double)3.0F;
                case "CHAMPION" -> var10000 = (double)8.0F;
                default -> var10000 = (double)1.0F;
            }

            return var10000;
        }
    }

    private static double[] weights(Layout.Stage s) {
        return (double[])WEIGHTS.computeIfAbsent(s.n(), (k) -> {
            double mobs = (double)0.0F;
            double bosses = (double)0.0F;
            double puzzles = (double)0.0F;

            for(Layout.Pack p : s.packs()) {
                for(Layout.Mob m : p.mobs()) {
                    mobs += mobWeight(m.rank(), p.miniboss() && !m.name().isEmpty());
                }
            }

            for(Layout.Boss b : s.bosses()) {
                bosses += (double)BOSS_W[Math.min(b.order(), BOSS_W.length - 1)];
            }

            for(Layout.Mech m : s.mechs()) {
                if (m.p().has("xp") && m.p().get("xp").getAsBoolean()) {
                    ++puzzles;
                }
            }

            return new double[]{Math.max((double)1.0F, mobs), Math.max((double)1.0F, bosses), Math.max((double)1.0F, puzzles)};
        });
    }

    private static void claim(ServerPlayer p, String key, long xp) {
        PlayerStats s = StatsManager.get(p);
        if (s.claim(key)) {
            StatsManager.giveXp(p, (int)Math.max(1L, xp));
        }
    }

    public static void mobKilled(MinecraftServer server, Layout.Stage s, LivingEntity dead, String packId) {
        Layout.Pack pack = s.pack(packId);
        if (pack != null) {
            int idx = dead.getPersistentData().getInt("litrpg_pidx");
            boolean named = dead.getPersistentData().getBoolean("litrpg_miniboss");
            double w = mobWeight(MobLevels.getRank(dead).name(), named);
            long xp = Math.round((double)((float)budget(s.n()) * 0.5F) * w / weights(s)[0]);
            String key = "s" + s.n() + ":" + packId + "#" + idx;

            for(ServerPlayer p : Run.party(server)) {
                boolean first = !StatsManager.get(p).claimed(key);
                claim(p, key, xp);
                if (first || !named) {
                    RunLoot.mob(p, s, dead);
                }
            }

        }
    }

    public static void bossKilled(MinecraftServer server, Layout.Stage s, Layout.Boss b, BossBase boss) {
        long xp = Math.round((double)((float)budget(s.n()) * 0.45F * BOSS_W[Math.min(b.order(), BOSS_W.length - 1)]) / weights(s)[1]);
        int var10000 = s.n();
        String key = "s" + var10000 + ":boss:" + b.id();
        boolean finalBoss = b.order() == s.bosses().size() - 1;

        for(ServerPlayer p : Run.party(server)) {
            boolean first = !StatsManager.get(p).claimed(key);
            claim(p, key, xp);
            if (first) {
                RunLoot.boss(p, s, b, b.type(), finalBoss);
            } else {
                p.sendSystemMessage(Component.literal("Трофеи этого хранителя вы уже получили.").withStyle(ChatFormatting.GRAY));
            }
        }

    }

    public static void puzzleSolved(MinecraftServer server, Layout.Stage s, String mechId, String title) {
        long xp = Math.round((double)((float)budget(s.n()) * 0.05F) / weights(s)[2]);
        int var10000 = s.n();
        String key = "s" + var10000 + ":mech:" + mechId;

        for(ServerPlayer p : Run.party(server)) {
            p.sendSystemMessage(Component.literal("✦ " + title).withStyle(ChatFormatting.AQUA));
            claim(p, key, xp);
        }

    }
}
