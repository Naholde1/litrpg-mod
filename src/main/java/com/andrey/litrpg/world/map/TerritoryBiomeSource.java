package com.andrey.litrpg.world.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Биомы «Мира Системы»: по территориям народов (с их наборами биомов),
 * вода, берега, города и Край Мира. Под землёй — ванильные пещерные биомы.
 */
public class TerritoryBiomeSource extends BiomeSource {
    public static final Codec<TerritoryBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, i.stable(TerritoryBiomeSource::new)));

    private final HolderGetter<Biome> getter;
    private final Map<String, Holder<Biome>> cache = new HashMap<>();
    private final Set<Holder<Biome>> all = new LinkedHashSet<>();

    public TerritoryBiomeSource(HolderGetter<Biome> getter) {
        this.getter = getter;
        WorldMap map = WorldMap.get();
        for (WorldMap.Territory t : map.territories) for (String b : t.biomes) biome(b);
        for (WorldMap.City c : map.cities) biome(c.biome());
        for (String b : new String[]{"litrpg:frontier_plains", "litrpg:frontier_woods", "litrpg:frontier_hills", "litrpg:frontier_taiga",
                "litrpg:frontier_savanna", "litrpg:world_wall", "litrpg:city_lands", "litrpg:city_snow", "litrpg:city_steppe", "litrpg:city_ash",
                "litrpg:city_stone", "litrpg:city_grove", "litrpg:emerald_bay", "litrpg:silver_lake",
                "minecraft:river", "minecraft:frozen_river", "minecraft:beach", "minecraft:snowy_beach", "minecraft:lush_caves",
                "minecraft:dripstone_caves", "minecraft:deep_dark"}) {
            biome(b);
        }
    }

    private Holder<Biome> biome(String id) {
        return cache.computeIfAbsent(id, k -> {
            Holder<Biome> h = getter.getOrThrow(ResourceKey.create(Registries.BIOME, new ResourceLocation(k)));
            all.add(h);
            return h;
        });
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return all.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int x = qx << 2, y = qy << 2, z = qz << 2;
        return biome(biomeId(WorldMap.get(), x, y, z, sampler));
    }

    /** Общий выбор биома (используется и для карты). */
    public static String biomeId(WorldMap map, int x, int y, int z, Climate.Sampler sampler) {
        double c = Math.max(Math.abs(x), Math.abs(z));
        double surface = map.height(x, z);
        // пещеры
        if (y < surface - 26 && sampler != null) {
            if (y < 0 && WorldMap.vn(x, z, 260, 91) > 0.68) return "minecraft:deep_dark";
            Climate.TargetPoint tp = sampler.sample(x >> 2, y >> 2, z >> 2);
            if (tp.humidity() > 4000) return "minecraft:lush_caves";
            if (tp.continentalness() > 6000) return "minecraft:dripstone_caves";
        }
        if (c > map.wall0 + 170 + 70 * (WorldMap.vn(x, z, 90, 79) - 0.5)) return "litrpg:world_wall";
        WorldMap.City city = map.cityCoreAt(x, z);
        if (city != null) return city.biome();
        int t = map.territoryAt(x, z);
        WorldMap.Territory terr = map.territory(t);
        boolean cold = terr != null ? terr.cold : z < -2600;
        // площадка поселения — «обжитой» биом без леса: деревья и грибы мира не прорастают сквозь дворы и крыши
        if (map.settlementAt(x, z) != null && map.waterAt(x, z) == 0) {
            String id = terr == null ? "" : terr.id;
            if (cold || id.equals("frost")) return "litrpg:city_snow";
            return switch (id) {
                case "orcs" -> "litrpg:city_steppe";
                case "demonkin" -> "litrpg:city_ash";
                case "deadlands", "dwarves" -> "litrpg:city_stone";
                case "elves" -> "litrpg:city_grove";
                default -> "litrpg:city_lands";
            };
        }
        switch (map.waterAt(x, z)) {
            case 1 -> { return cold ? "minecraft:frozen_river" : "minecraft:river"; }
            case 2 -> { return "litrpg:silver_lake"; }
            case 3 -> { return "litrpg:emerald_bay"; }
            case 4 -> { return cold ? "minecraft:snowy_beach" : "minecraft:beach"; }
            default -> { }
        }
        if (surface < 62) return "litrpg:silver_lake";
        if (terr == null) {
            // дикие земли: север холоднее, юг суше
            double n = WorldMap.vn(x, z, 380, 501);
            if (z < -1900 + 300 * (WorldMap.vn(x, z, 600, 502) - 0.5)) return n > 0.6 ? "litrpg:frontier_hills" : "litrpg:frontier_taiga";
            if (z > 1900 + 300 * (WorldMap.vn(x, z, 600, 503) - 0.5)) return n > 0.65 ? "litrpg:frontier_hills" : "litrpg:frontier_savanna";
            return n < 0.42 ? "litrpg:frontier_plains" : n < 0.75 ? "litrpg:frontier_woods" : "litrpg:frontier_hills";
        }
        double n = WorldMap.vn(x, z, 420, 300 + t) * 0.8 + WorldMap.vn(x, z, 120, 330 + t) * 0.2;
        int pick = (int) (n * terr.weightSum);
        for (int i = 0; i < terr.biomes.length; i++) {
            pick -= terr.weights[i];
            if (pick < 0) return terr.biomes[i];
        }
        return terr.biomes[terr.biomes.length - 1];
    }
}
