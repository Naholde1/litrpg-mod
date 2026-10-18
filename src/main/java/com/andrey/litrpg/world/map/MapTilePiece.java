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
