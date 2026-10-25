package com.andrey.litrpg.world;

import com.andrey.litrpg.registry.ModWorldgen;
import com.andrey.litrpg.run.Layout;
import com.mojang.serialization.Codec;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/** Плитки карты подземелья стоят в заранее заданных чанках (список — в /litrpg_dungeon.json). */
public class TilePlacement extends StructurePlacement {
    public static final Codec<TilePlacement> CODEC = Codec.unit(TilePlacement::new);

    public TilePlacement() {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0f, 0, Optional.empty());
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return Layout.get().tile(x, z) != null;
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorldgen.TILES.get();
    }
}
