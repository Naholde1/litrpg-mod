package com.andrey.litrpg.world.map;

import com.andrey.litrpg.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class MapTilePiece extends TemplateStructurePiece {
    public MapTilePiece(StructureTemplateManager mgr, ResourceLocation tpl, BlockPos pos) {
        super(ModWorldgen.MAP_TILE_PIECE.get(), 0, mgr, tpl, tpl.toString(), settings(), pos);
    }

    public MapTilePiece(StructurePieceSerializationContext ctx, CompoundTag tag) {
        super(ModWorldgen.MAP_TILE_PIECE.get(), tag, ctx.structureTemplateManager(), id -> settings());
    }

    private static StructurePlaceSettings settings() {
        return new StructurePlaceSettings().setFinalizeEntities(true);
    }

    /** Посевы: при генерации Майнкрафт сносит их (свет ещё не посчитан) — досаживаем из шаблона. */
    private static final net.minecraft.world.level.block.Block[] CROPS = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS,
            Blocks.SWEET_BERRY_BUSH, Blocks.PUMPKIN_STEM, Blocks.MELON_STEM, Blocks.TORCHFLOWER_CROP};

    @Override
    public void postProcess(net.minecraft.world.level.WorldGenLevel level, net.minecraft.world.level.StructureManager mgr,
                            net.minecraft.world.level.chunk.ChunkGenerator gen, RandomSource random, BoundingBox box,
                            net.minecraft.world.level.ChunkPos chunk, BlockPos pos) {
        super.postProcess(level, mgr, gen, random, box, chunk, pos);
        for (var crop : CROPS) {
            for (var info : template.filterBlocks(templatePosition, placeSettings, crop)) {
                if (box.isInside(info.pos()) && level.getBlockState(info.pos()).isAir()) level.setBlock(info.pos(), info.state(), 2);
            }
        }
        if (level.getLevel().dimension() == net.minecraft.world.level.Level.OVERWORLD) fillBelow(level, box);
    }

    /** Глубина засыпки под постройкой: пещеры и лавовые озёра не подмывают город. */
    private static final int FILL_DEPTH = 24;
    /** Для каждого шаблона — нижний заданный блок в каждом столбце (или MIN_VALUE, если там воздух/пусто). */
    private static final java.util.Map<String, int[]> LOWEST = new java.util.concurrent.ConcurrentHashMap<>();

    private int[] lowest() {
        return LOWEST.computeIfAbsent(templateName, k -> {
            var size = template.getSize();
            int sx = size.getX(), sz = size.getZ();
            int[] low = new int[sx * sz];
            java.util.Arrays.fill(low, Integer.MAX_VALUE);
            boolean[] solid = new boolean[sx * sz];
            CompoundTag tag = template.save(new CompoundTag());
            var palette = tag.getList("palette", 10);
            var blocks = tag.getList("blocks", 10);
            for (int i = 0; i < blocks.size(); i++) {
                CompoundTag b = blocks.getCompound(i);
                var p = b.getList("pos", 3);
                int x = p.getInt(0), y = p.getInt(1), z = p.getInt(2);
                if (x < 0 || z < 0 || x >= sx || z >= sz) continue;
                int c = x * sz + z;
                if (y < low[c]) {
                    low[c] = y;
                    String name = palette.getCompound(b.getInt("state")).getString("Name");
                    solid[c] = !name.equals("minecraft:air") && !name.equals("minecraft:cave_air") && !name.equals("minecraft:structure_void");
                }
            }
            for (int c = 0; c < low.length; c++) if (low[c] == Integer.MAX_VALUE || !solid[c]) low[c] = Integer.MIN_VALUE;
            return low;
        });
    }

    private void fillBelow(net.minecraft.world.level.WorldGenLevel level, BoundingBox box) {
        int[] low = lowest();
        var size = template.getSize();
        int sz = size.getZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight() + 1;
        for (int x = Math.max(box.minX(), templatePosition.getX()); x <= Math.min(box.maxX(), templatePosition.getX() + size.getX() - 1); x++) {
            for (int z = Math.max(box.minZ(), templatePosition.getZ()); z <= Math.min(box.maxZ(), templatePosition.getZ() + sz - 1); z++) {
                int ly = low[(x - templatePosition.getX()) * sz + (z - templatePosition.getZ())];
                if (ly == Integer.MIN_VALUE) continue;
                int top = templatePosition.getY() + ly - 1;
                for (int y = top; y > top - FILL_DEPTH && y >= minY; y--) {
                    m.set(x, y, z);
                    var st = level.getBlockState(m);
                    // пустоты и лава — засыпать; воду не трогаем (под причалами и мостами — море и реки)
                    if (st.isAir() || st.getFluidState().is(net.minecraft.tags.FluidTags.LAVA)) {
                        level.setBlock(m, y < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level, RandomSource random, BoundingBox box) {
        // маркер «painting <вариант> <сторона>»: картины из шаблона Майнкрафт не создаёт, вешаем сами
        String[] p = name.split(" ");
        if (p.length != 3 || !p[0].equals("painting") || !box.isInside(pos)) return;
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        Direction dir = Direction.byName(p[2]);
        ResourceLocation id = ResourceLocation.tryParse(p[1]);
        if (dir == null || id == null) return;
        level.registryAccess().registryOrThrow(Registries.PAINTING_VARIANT)
                .getHolder(ResourceKey.create(Registries.PAINTING_VARIANT, id))
                .ifPresent(v -> level.addFreshEntity(new Painting(level.getLevel(), pos, dir, v)));
    }
}
