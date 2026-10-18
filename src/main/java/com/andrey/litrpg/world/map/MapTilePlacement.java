package com.andrey.litrpg.world.map;

import com.andrey.litrpg.registry.ModWorldgen;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/** Постройки карты стоят в заранее заданных чанках (список плиток из WorldMap). */
public class MapTilePlacement extends StructurePlacement {
    public static final Codec<MapTilePlacement> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("layer").forGetter(p -> p.layer)
    ).apply(i, MapTilePlacement::new));

    private final String layer;

    public MapTilePlacement(String layer) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
        this.layer = layer;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return WorldMap.get().tile(layer.equals("nether"), x, z) != null;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.MAP_TILES.get();
    }
}
