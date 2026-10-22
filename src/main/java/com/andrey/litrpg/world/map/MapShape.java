package com.andrey.litrpg.world.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Плотность рельефа «Мира Системы». Смешивает ванильную плотность с
 * плотностью нашей карты: поверхность на высоте H(x,z), долей W(x,z).
 * Режимы: cheese — основная толща (пещеры остаются ванильными),
 * initial — предварительная поверхность, nether — площадки в Преисподней.
 */
public record MapShape(DensityFunction input, String mode) implements DensityFunction {
    public static final Codec<MapShape> DATA_CODEC = RecordCodecBuilder.create(i -> i.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(MapShape::input),
            Codec.STRING.optionalFieldOf("mode", "cheese").forGetter(MapShape::mode)
    ).apply(i, MapShape::new));
    public static final KeyDispatchDataCodec<MapShape> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** Плотность на блок от поверхности (1.0 — на глубине 24 блока). */
    private static final double K = 1.0 / 24.0;
    /** Буфер на поток: плотность считается миллионы раз при генерации — без лишних массивов. */
    private static final ThreadLocal<double[]> HW = ThreadLocal.withInitial(() -> new double[2]);

    @Override
    public double compute(FunctionContext ctx) {
        int x = ctx.blockX(), y = ctx.blockY(), z = ctx.blockZ();
        WorldMap map = WorldMap.get();
        if (mode.charAt(0) == 'n') return nether(map, input.compute(ctx), x, y, z);
        double[] hw = HW.get();
        map.sample(x, z, hw);
        double t = (hw[0] - y) * K;
        // карта управляет рельефом целиком (города, площадки, Край Мира) — дорогой ванильный шум не нужен
        if (hw[1] >= 0.999) return t;
        double d = input.compute(ctx);
        return d + (t - d) * hw[1];
    }

    /** Пустая пещера с ровным полом под постройками Преисподней. */
    private static double nether(WorldMap map, double d, int x, int y, int z) {
        for (double[] f : map.netherFlats) {
            double dist = Math.hypot(x - f[0], z - f[1]);
            if (dist > f[2] + 32) continue;
            double k = WorldMap.sstep(f[2] + 32, f[2], dist);
            double floor = f[3], ceil = f[3] + 36;
            double t;
            if (y < floor) t = (floor - y) / 6.0;
            else if (y < ceil) t = -Math.min(y - floor + 1, ceil - y) / 6.0 - 0.3;
            else t = d;
            d = d + (t - d) * k;
        }
        return d;
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        provider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new MapShape(input.mapAll(visitor), mode));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), -20);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), 20);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
