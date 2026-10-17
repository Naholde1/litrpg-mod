package com.andrey.litrpg.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;

/** Цвет «пикселя карты» для столбца блоков (общий для мини-карты и карты мира). */
public class MapColors {
    public static final int UNKNOWN = 0xFF101014;

    /** @return цвет в формате ABGR (для NativeImage) */
    public static int columnColor(ClientLevel level, int x, int z, int playerY) {
        if (!level.hasChunk(x >> 4, z >> 4)) return UNKNOWN;

        int y;
        if (level.dimensionType().hasCeiling()) {
            y = findFloorBelow(level, x, z, playerY + 2);
            if (y == Integer.MIN_VALUE) return UNKNOWN;
        } else {
            y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        }
        if (y < level.getMinBuildHeight()) return UNKNOWN;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        // пропускаем прозрачное без цвета (стекло и т.п.)
        int guard = 0;
        while (state.getMapColor(level, pos) == MapColor.NONE && pos.getY() > level.getMinBuildHeight() && guard++ < 16) {
            pos.move(0, -1, 0);
            state = level.getBlockState(pos);
        }
        MapColor mc = state.getMapColor(level, pos);
        if (mc == MapColor.NONE) return UNKNOWN;

        int rgb = mc.col;
        float shade = 1.0f;
        FluidState fluid = state.getFluidState();
        if (!fluid.isEmpty() && mc == MapColor.WATER) {
            // глубина воды — темнее
            int depth = 0;
            BlockPos.MutableBlockPos p2 = pos.mutable();
            while (depth < 12 && !level.getFluidState(p2).isEmpty()) {
                p2.move(0, -1, 0);
                depth++;
            }
            shade = 1.0f - depth * 0.04f;
        } else {
            // рельеф: сравниваем с северным соседом
            int northY = level.dimensionType().hasCeiling()
                    ? pos.getY()
                    : level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
            if (northY > pos.getY()) shade = 0.82f;
            else if (northY < pos.getY()) shade = 1.12f;
        }
        return toAbgr(rgb, shade);
    }

    private static int findFloorBelow(ClientLevel level, int x, int z, int startY) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
        boolean seenAir = false;
        for (int i = 0; i < 48 && pos.getY() > level.getMinBuildHeight(); i++) {
            BlockState s = level.getBlockState(pos);
            if (s.isAir()) {
                seenAir = true;
            } else if (seenAir) {
                return pos.getY();
            }
            pos.move(0, -1, 0);
        }
        return Integer.MIN_VALUE;
    }

    public static int toAbgr(int rgb, float shade) {
        int r = clamp((int) (((rgb >> 16) & 0xFF) * shade));
        int g = clamp((int) (((rgb >> 8) & 0xFF) * shade));
        int b = clamp((int) ((rgb & 0xFF) * shade));
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}
