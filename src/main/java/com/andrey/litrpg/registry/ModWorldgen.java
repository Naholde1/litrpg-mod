package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.world.TilePiece;
import com.andrey.litrpg.world.TilePlacement;
import com.andrey.litrpg.world.TileStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Карта подземелья: готовые шаблоны-плитки на заданных местах в пустом измерении litrpg:dungeon. */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, LitRPG.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, LitRPG.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, LitRPG.MODID);
    public static final DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraft.world.level.biome.BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, LitRPG.MODID);

    public static final RegistryObject<StructurePlacementType<TilePlacement>> TILES = PLACEMENTS.register("dungeon_tiles", () -> () -> TilePlacement.CODEC);
    public static final RegistryObject<StructureType<TileStructure>> TILE = STRUCTURES.register("dungeon_tile", () -> () -> TileStructure.CODEC);
    public static final RegistryObject<StructurePieceType> TILE_PIECE = PIECES.register("dungeon_tile", () -> (StructurePieceType) TilePiece::new);

    public static final RegistryObject<com.mojang.serialization.Codec<? extends net.minecraft.world.level.biome.BiomeSource>> STAGES =
            BIOME_SOURCES.register("stages", () -> com.andrey.litrpg.world.StageBiomeSource.CODEC);

    public static void register(IEventBus bus) {
        BIOME_SOURCES.register(bus);
        PLACEMENTS.register(bus);
        STRUCTURES.register(bus);
        PIECES.register(bus);
    }
}
