package com.andrey.litrpg.world;

import com.andrey.litrpg.registry.ModWorldgen;
import com.andrey.litrpg.run.Layout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Плитка карты подземелья: готовый шаблон без поворота на заданной высоте. */
public class TileStructure extends Structure {
    public static final Codec<TileStructure> CODEC = RecordCodecBuilder.create(i -> i.group(settingsCodec(i)).apply(i, TileStructure::new));

    public TileStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Layout.Tile t = Layout.get().tile(ctx.chunkPos().x, ctx.chunkPos().z);
        if (t == null) return Optional.empty();
        BlockPos pos = new BlockPos(ctx.chunkPos().getMinBlockX(), t.y(), ctx.chunkPos().getMinBlockZ());
        ResourceLocation tpl = new ResourceLocation(t.template());
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new TilePiece(ctx.structureTemplateManager(), tpl, pos))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.TILE.get();
    }
}
