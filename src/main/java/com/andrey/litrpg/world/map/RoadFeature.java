package com.andrey.litrpg.world.map;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Дороги между городами: мощёные тракты и грунтовые пути, мосты через
 * реки, фонари на столбах. Рисуются в каждом чанке по ломаным из WorldMap.
 */
public class RoadFeature extends Feature<NoneFeatureConfiguration> {
    private static final int POST_STEP = 44;

    public RoadFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    private record Seg(double ax, double az, double bx, double bz, double startLen, WorldMap.Road road) {}

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!MapTileStructure.isSystem(ctx.chunkGenerator(), false)) return false;
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        WorldMap map = WorldMap.get();
        List<Seg> segs = new ArrayList<>();
        for (WorldMap.Road r : map.roads) {
            double len = 0;
            for (int i = 0; i < r.points().length - 1; i++) {
                double[] a = r.points()[i], b = r.points()[i + 1];
                double minX = Math.min(a[0], b[0]) - 8, maxX = Math.max(a[0], b[0]) + 8;
                double minZ = Math.min(a[1], b[1]) - 8, maxZ = Math.max(a[1], b[1]) + 8;
                if (maxX >= x0 && minX <= x0 + 15 && maxZ >= z0 && minZ <= z0 + 15) segs.add(new Seg(a[0], a[1], b[0], b[1], len, r));
                len += Math.hypot(b[0] - a[0], b[1] - a[1]);
            }
        }
        if (segs.isEmpty()) return false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                double best = 1e9;
                Seg bs = null;
                for (Seg s : segs) {
                    double d = WorldMap.segDist(x + 0.5, z + 0.5, s.ax, s.az, s.bx, s.bz);
                    if (d < best) {
                        best = d;
                        bs = s;
                    }
                }
                if (bs == null) continue;
                boolean stone = bs.road.style().equals("stone");
                double half = stone ? 2.6 : 2.0;
                if (best > half + 0.5) continue;
                if (inSettlement(map, x, z)) continue;
                any |= paint(level, pos, x, z, best, half, stone);
            }
        }
        // фонари вдоль трактов
        for (Seg s : segs) {
            double segLen = Math.hypot(s.bx - s.ax, s.bz - s.az);
            if (segLen < 1) continue;
            double first = Math.ceil(s.startLen / POST_STEP) * POST_STEP - s.startLen;
            for (double t = first; t < segLen; t += POST_STEP) {
                double nx = -(s.bz - s.az) / segLen, nz = (s.bx - s.ax) / segLen;
                double side = s.road.style().equals("stone") ? 4.0 : 3.5;
                int px = (int) Math.floor(s.ax + (s.bx - s.ax) * t / segLen + nx * side);
                int pz = (int) Math.floor(s.az + (s.bz - s.az) * t / segLen + nz * side);
                if (px < x0 || px > x0 + 15 || pz < z0 || pz > z0 + 15 || inSettlement(map, px, pz)) continue;
                int gy = ground(level, pos, px, pz);
                pos.set(px, gy, pz);
                BlockState g = level.getBlockState(pos);
                if (!g.isSolidRender(level, pos) || g.getFluidState().isSource()) continue;
                BlockState fence = (s.road.style().equals("stone") ? Blocks.DARK_OAK_FENCE : Blocks.SPRUCE_FENCE).defaultBlockState();
                level.setBlock(pos.set(px, gy + 1, pz), fence, 2);
                level.setBlock(pos.set(px, gy + 2, pz), fence, 2);
                level.setBlock(pos.set(px, gy + 3, pz), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false), 2);
            }
        }
        return any;
    }

    /** Внутри городов и поселений дорогу не рисуем — у них свои улицы. */
    private static boolean inSettlement(WorldMap map, int x, int z) {
        for (WorldMap.City c : map.cities) {
            double dx = x - c.x(), dz = z - c.z(), r = c.r() - 20;
            if (dx * dx + dz * dz < r * r) return true;
        }
        for (WorldMap.Poi p : map.pois) {
            if (p.r() <= 0) continue;
            double dx = x - p.x(), dz = z - p.z(), r = p.r() - 2;
            if (dx * dx + dz * dz < r * r) return true;
        }
        return false;
    }

    /** Высота земли под деревьями и травой. */
    private static int ground(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
        while (y > level.getMinBuildHeight()) {
            BlockState s = level.getBlockState(pos.set(x, y, z));
            if (s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced() || s.is(Blocks.SNOW)
                    || s.is(BlockTags.FLOWERS) || s.is(Blocks.BAMBOO) || s.is(Blocks.CACTUS)) {
                if (!s.getFluidState().isEmpty()) return y; // вода — мост
                y--;
            } else {
                return y;
            }
        }
        return y;
    }

    private static boolean paint(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, double dist, double half, boolean stone) {
        int y = ground(level, pos, x, z);
        BlockState top = level.getBlockState(pos.set(x, y, z));
        long h = (x * 341873128712L + z * 132897987541L) >>> 17;
        if (!top.getFluidState().isEmpty()) {
            // мост: настил над водой и перила
            int deck = Math.max(y + 1, 63);
            boolean edge = dist > half - 0.6;
            level.setBlock(pos.set(x, deck, z), (stone ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS).defaultBlockState(), 2);
            if (edge) {
                level.setBlock(pos.set(x, deck + 1, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
            }
            for (int i = 1; i <= 3 && !edge; i++) level.setBlock(pos.set(x, deck + i, z), Blocks.AIR.defaultBlockState(), 2);
            if ((x + z) % 7 == 0) {
                for (int yy = deck - 1; yy > deck - 12; yy--) {
                    pos.set(x, yy, z);
                    if (level.getBlockState(pos).getFluidState().isEmpty() && !level.getBlockState(pos).isAir()) break;
                    level.setBlock(pos, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState(), 2);
                }
            }
            return true;
        }
        BlockState road;
        if (stone) {
            int r = (int) (h % 10);
            road = dist > half - 0.7 ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                    : r < 5 ? Blocks.COBBLESTONE.defaultBlockState() : r < 8 ? Blocks.STONE_BRICKS.defaultBlockState()
                    : Blocks.GRAVEL.defaultBlockState();
            if (road.is(Blocks.MOSSY_COBBLESTONE) && h % 3 == 0) road = Blocks.COBBLESTONE.defaultBlockState();
        } else {
            int r = (int) (h % 10);
            road = r < 6 ? Blocks.DIRT_PATH.defaultBlockState() : r < 8 ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.GRAVEL.defaultBlockState();
            if (dist > half - 0.5 && h % 2 == 0) return false; // неровные обочины
        }
        if (top.is(Blocks.SAND) || top.is(Blocks.RED_SAND)) {
            road = stone ? Blocks.SMOOTH_SANDSTONE.defaultBlockState() : Blocks.SANDSTONE.defaultBlockState();
        }
        level.setBlock(pos.set(x, y, z), road, 2);
        // под дорогой — опора, над дорогой — расчистить
        BlockState below = level.getBlockState(pos.set(x, y - 1, z));
        if (below.isAir() || below.canBeReplaced()) level.setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        for (int i = 1; i <= 4; i++) {
            BlockState s = level.getBlockState(pos.set(x, y + i, z));
            if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced() || s.is(Blocks.CACTUS) || s.is(Blocks.BAMBOO)) {
                if (s.getFluidState().isEmpty()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        return true;
    }

}
