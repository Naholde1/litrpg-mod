package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.world.map.MapShape;
import com.andrey.litrpg.world.map.MapTilePiece;
import com.andrey.litrpg.world.map.MapTilePlacement;
import com.andrey.litrpg.world.map.MapTileStructure;
import com.andrey.litrpg.world.map.RoadFeature;
import com.andrey.litrpg.world.map.TerritoryBiomeSource;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Всё для «Мира Системы»: рельеф, биомы, плитки построек, дороги, картины. */
public final class ModWorldgen {
    private ModWorldgen() {}

    public static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, LitRPG.MODID);
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, LitRPG.MODID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, LitRPG.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, LitRPG.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, LitRPG.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, LitRPG.MODID);
    public static final DeferredRegister<PaintingVariant> PAINTINGS = DeferredRegister.create(Registries.PAINTING_VARIANT, LitRPG.MODID);

    public static final RegistryObject<Codec<? extends DensityFunction>> MAP_SHAPE = DENSITY.register("map_shape", () -> MapShape.DATA_CODEC);
    public static final RegistryObject<Codec<? extends BiomeSource>> TERRITORIES = BIOME_SOURCES.register("territories", () -> TerritoryBiomeSource.CODEC);
    public static final RegistryObject<StructurePlacementType<MapTilePlacement>> MAP_TILES = PLACEMENTS.register("map_tiles", () -> () -> MapTilePlacement.CODEC);
    public static final RegistryObject<StructureType<MapTileStructure>> MAP_TILE = STRUCTURES.register("map_tile", () -> () -> MapTileStructure.CODEC);
    public static final RegistryObject<StructurePieceType> MAP_TILE_PIECE = PIECES.register("map_tile", () -> (StructurePieceType) MapTilePiece::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> MAP_ROADS = FEATURES.register("map_roads",
            () -> new RoadFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<PaintingVariant> WORLD_MAP = PAINTINGS.register("world_map", () -> new PaintingVariant(128, 128));

    public static void register(IEventBus bus) {
        DENSITY.register(bus);
        BIOME_SOURCES.register(bus);
        PLACEMENTS.register(bus);
        STRUCTURES.register(bus);
        PIECES.register(bus);
        FEATURES.register(bus);
        PAINTINGS.register(bus);
    }
}
