package com.andrey.litrpg.world.map;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** Плитка карты: готовый шаблон без поворота на заданной высоте (только в «Мире Системы»). */
public class MapTileStructure extends Structure {
    public static final Codec<MapTileStructure> CODEC = RecordCodecBuilder.create(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("layer").forGetter(s -> s.layer)
    ).apply(i, MapTileStructure::new));

    private final String layer;

    public MapTileStructure(StructureSettings settings, String layer) {
        super(settings);
        this.layer = layer;
    }

    public static boolean isSystem(ChunkGenerator gen, boolean nether) {
        return gen instanceof NoiseBasedChunkGenerator ng && ng.generatorSettings().is(LitRPG.id(nether ? "system_nether" : "system"));
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        boolean nether = layer.equals("nether");
        if (!isSystem(ctx.chunkGenerator(), nether)) return Optional.empty();
        WorldMap.Tile t = WorldMap.get().tile(nether, ctx.chunkPos().x, ctx.chunkPos().z);
        if (t == null) return Optional.empty();
        BlockPos pos = new BlockPos(ctx.chunkPos().getMinBlockX(), t.y(), ctx.chunkPos().getMinBlockZ());
        ResourceLocation tpl = new ResourceLocation(t.template());
        return Optional.of(new GenerationStub(pos, b -> b.addPiece(new MapTilePiece(ctx.structureTemplateManager(), tpl, pos))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.MAP_TILE.get();
    }
}
