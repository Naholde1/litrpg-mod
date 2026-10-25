package com.andrey.litrpg.world;

import com.andrey.litrpg.run.Layout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * Биомы подземелья: Преддверие и пять этапов — у каждого свой туман, частицы, звуки и музыка.
 * Список: [преддверие, этап 1 … этап 5]; этап определяется по схеме подземелья.
 */
public class StageBiomeSource extends BiomeSource {
    public static final Codec<StageBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes)
    ).apply(i, StageBiomeSource::new));

    private final List<Holder<Biome>> biomes;

    public StageBiomeSource(List<Holder<Biome>> biomes) {
        this.biomes = biomes;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int x = qx << 2, z = qz << 2;
        for (Layout.Stage s : Layout.get().stages) {
            if (x >= s.box().x1() - 16 && x <= s.box().x2() + 16 && z >= s.box().z1() - 16 && z <= s.box().z2() + 16) {
                return biomes.get(Math.min(s.n(), biomes.size() - 1));
            }
        }
        return biomes.get(0);
    }
}
