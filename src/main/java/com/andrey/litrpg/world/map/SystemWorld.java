package com.andrey.litrpg.world.map;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Правила «Мира Системы»: граница, точка появления, безопасные города, плотность монстров. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class SystemWorld {
    public static final String EXTRA_TAG = "litrpg_pack";
    /** Граница мира: ±4750 — за горами Края Мира. */
    public static final int BORDER = 9500;

    private SystemWorld() {}

    public static boolean is(LevelAccessor level) {
        return level instanceof ServerLevel sl && MapTileStructure.isSystem(sl.getChunkSource().getGenerator(),
                sl.dimension() == Level.NETHER);
    }

    public static boolean isOverworld(LevelAccessor level) {
        return level instanceof ServerLevel sl && sl.dimension() == Level.OVERWORLD && is(sl);
    }

    /** Точка появления — площадь столицы. */
    public static BlockPos spawnPoint() {
        WorldMap map = WorldMap.get();
        WorldMap.Poi spawn = map.poi("spawn");
        if (spawn != null) return new BlockPos(spawn.x(), spawn.y(), spawn.z());
        WorldMap.City c = map.cities.get(0);
        return new BlockPos(c.x(), c.y() + 1, c.z() + 8);
    }

    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !isOverworld(sl)) return;
        event.getSettings().setSpawn(spawnPoint(), 0f);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent event) {
        ServerLevel ow = event.getServer().overworld();
        if (!isOverworld(ow)) return;
        var border = ow.getWorldBorder();
        if (border.getSize() > BORDER + 1) {
            border.setCenter(0, 0);
            border.setSize(BORDER);
            border.setWarningBlocks(32);
        }
        LitRPG.LOGGER.info("World of the System: border {}", border.getSize());
    }

    /** Города безопасны; чем дальше от них, тем больше монстров ходит стаями. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy)) return;
        MobSpawnType type = event.getSpawnType();
        if (type != MobSpawnType.NATURAL && type != MobSpawnType.CHUNK_GENERATION) return;
        ServerLevel sl = event.getLevel().getLevel();
        if (!isOverworld(sl)) return;
        WorldMap map = WorldMap.get();
        double x = event.getX(), z = event.getZ();
        for (WorldMap.City c : map.cities) {
            double dx = x - c.x(), dz = z - c.z();
            double r = c.r() + 48;
            if (dx * dx + dz * dz < r * r) {
                event.setSpawnCancelled(true);
                return;
            }
        }
        if (type != MobSpawnType.NATURAL || mob.getPersistentData().getBoolean(EXTRA_TAG)) return;
        double danger = map.danger(x, z);
        if (sl.random.nextDouble() >= danger * 0.7) return;
        int extra = 1 + (danger > 0.6 && sl.random.nextBoolean() ? 1 : 0);
        EntityType<?> et = mob.getType();
        for (int i = 0; i < extra; i++) {
            if (!(et.create(sl) instanceof Mob m)) continue;
            double ox = x + sl.random.nextInt(7) - 3, oz = z + sl.random.nextInt(7) - 3;
            BlockPos p = BlockPos.containing(ox, event.getY(), oz);
            if (!sl.noCollision(m, et.getAABB(ox + 0.5, event.getY(), oz + 0.5)) || !sl.getBlockState(p.below()).isSolid()) continue;
            m.moveTo(ox + 0.5, event.getY(), oz + 0.5, sl.random.nextFloat() * 360, 0);
            m.getPersistentData().putBoolean(EXTRA_TAG, true);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p), MobSpawnType.NATURAL, null, null);
            sl.addFreshEntity(m);
        }
    }
}
