package com.andrey.litrpg.world;

import com.andrey.litrpg.registry.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Зоны мира, как кольца вокруг Пристанища (точки спавна).
 * Чем дальше от центра, тем выше уровень монстров.
 */
public enum Zone {
    HAVEN("haven", 0, 1, 2, ChatFormatting.GREEN),
    MEADOWS("meadows", 48, 2, 5, ChatFormatting.GREEN),
    WILDWOOD("wildwood", 160, 5, 10, ChatFormatting.YELLOW),
    BORDERLANDS("borderlands", 320, 10, 17, ChatFormatting.GOLD),
    CURSED("cursed", 550, 17, 26, ChatFormatting.RED),
    DEADLANDS("deadlands", 850, 26, 38, ChatFormatting.DARK_RED),
    ABYSS("abyss", 1250, 38, 60, ChatFormatting.DARK_PURPLE),
    SHADOW("shadow", -1, 15, 30, ChatFormatting.DARK_PURPLE),
    NETHER("nether", -1, 25, 40, ChatFormatting.DARK_RED),
    END("end", -1, 45, 60, ChatFormatting.LIGHT_PURPLE);

    public final String key;
    /** Начало кольца в блоках от спавна (только для Верхнего мира). */
    public final int from;
    public final int minLevel;
    public final int maxLevel;
    public final ChatFormatting color;

    Zone(String key, int from, int minLevel, int maxLevel, ChatFormatting color) {
        this.key = key;
        this.from = from;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.color = color;
    }

    public String nameKey() { return "zone.litrpg." + key; }

    /**
     * Уровни монстров в точке: в «Мире Системы» — от ближайшего города
     * (чем дальше, тем опаснее), в обычном мире — по кольцам вокруг спавна.
     */
    public static int[] levels(ServerLevel level, BlockPos pos) {
        if (com.andrey.litrpg.world.map.SystemWorld.isOverworld(level)) {
            int l = com.andrey.litrpg.world.map.WorldMap.get().levelAt(pos.getX(), pos.getZ());
            return new int[]{Math.max(1, l - 1), l + 2};
        }
        if (level.dimension() == Level.NETHER && com.andrey.litrpg.world.map.SystemWorld.is(level)) {
            double best = 1e9;
            for (double[] f : com.andrey.litrpg.world.map.WorldMap.get().netherFlats) {
                best = Math.min(best, Math.hypot(pos.getX() - f[0], pos.getZ() - f[1]));
            }
            int l = (int) Math.min(60, 26 + (best > 1e8 ? 300 : best) / 18);
            return new int[]{l - 1, l + 2};
        }
        Zone z = at(level, pos);
        return new int[]{z.minLevel, z.maxLevel};
    }

    public static Zone at(ServerLevel level, BlockPos pos) {
        if (level.dimension() == ModDimensions.SHADOW_REALM) return SHADOW;
        if (level.dimension() == Level.NETHER) return NETHER;
        if (level.dimension() == Level.END) return END;
        BlockPos spawn = level.getSharedSpawnPos();
        double dx = pos.getX() - spawn.getX();
        double dz = pos.getZ() - spawn.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        Zone result = HAVEN;
        for (Zone z : values()) {
            if (z.from >= 0 && dist >= z.from) result = z;
        }
        return result;
    }
}
