package com.andrey.litrpg.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import java.io.InputStream;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Цвета блоков для карты «как в JourneyMap»: средний цвет верхней грани по настоящей
 * текстуре (с учётом ресурспаков) и окраска биома (трава, листва, вода). Считается один
 * раз на состояние блока, текстуры читаются из ресурсов, а не из атласа видеокарты.
 */
public final class BlockColors {
    private BlockColors() {}

    /** Средний цвет текстуры (RGB) и индекс окраски (-1 — без окраски). */
    private record Entry(int rgb, int tint) {}

    private static final Map<BlockState, Entry> BY_STATE = new IdentityHashMap<>();
    private static final Map<ResourceLocation, Integer> BY_TEXTURE = new HashMap<>();
    private static final RandomSource RND = RandomSource.create(42);

    public static void clear() {
        BY_STATE.clear();
        BY_TEXTURE.clear();
    }

    /** Цвет блока в этой точке мира (RGB). */
    public static int color(BlockAndTintGetter level, BlockState state, BlockPos pos) {
        Entry e = BY_STATE.get(state);
        if (e == null) {
            e = compute(state);
            BY_STATE.put(state, e);
        }
        if (e.tint < 0) return e.rgb;
        int t = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, e.tint);
        return t == -1 ? e.rgb : multiply(e.rgb, t);
    }

    /** Вода: серая текстура × цвет воды биома. */
    public static int water(BlockAndTintGetter level, BlockPos pos) {
        return multiply(texture(new ResourceLocation("minecraft", "block/water_still"), 0x3F76E4), BiomeColors.getAverageWaterColor(level, pos));
    }

    public static int lava() {
        return texture(new ResourceLocation("minecraft", "block/lava_still"), 0xD05010);
    }

    private static Entry compute(BlockState state) {
        if (state.getFluidState().is(Fluids.LAVA) || state.getFluidState().is(Fluids.FLOWING_LAVA)) return new Entry(lava(), -1);
        try {
            BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            TextureAtlasSprite sprite = null;
            int tint = -1;
            List<BakedQuad> up = model.getQuads(state, Direction.UP, RND);
            if (up.isEmpty()) up = model.getQuads(state, null, RND);
            if (!up.isEmpty()) {
                BakedQuad q = up.get(0);
                sprite = q.getSprite();
                tint = q.isTinted() ? q.getTintIndex() : -1;
            }
            if (sprite == null) sprite = model.getParticleIcon();
            int fallback = state.getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col;
            return new Entry(texture(sprite.contents().name(), fallback), tint);
        } catch (Exception ex) {
            return new Entry(0x808080, -1);
        }
    }

    /** Средний цвет непрозрачных пикселей текстуры (block/xxx → textures/block/xxx.png). */
    private static int texture(ResourceLocation sprite, int fallback) {
        Integer c = BY_TEXTURE.get(sprite);
        if (c != null) return c;
        int out = fallback;
        ResourceLocation file = new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png");
        var res = Minecraft.getInstance().getResourceManager().getResource(file);
        if (res.isPresent()) {
            try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                long r = 0, g = 0, b = 0, n = 0;
                for (int y = 0; y < img.getHeight(); y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        int p = img.getPixelRGBA(x, y); // ABGR
                        int a = p >>> 24;
                        if (a < 32) continue;
                        r += (p & 0xFF) * a;
                        g += (p >> 8 & 0xFF) * a;
                        b += (p >> 16 & 0xFF) * a;
                        n += a;
                    }
                }
                if (n > 0) out = (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
            } catch (Exception ignored) {
            }
        }
        BY_TEXTURE.put(sprite, out);
        return out;
    }

    public static int multiply(int rgb, int tint) {
        int r = (rgb >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255;
        int g = (rgb >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255;
        int b = (rgb & 0xFF) * (tint & 0xFF) / 255;
        return r << 16 | g << 8 | b;
    }

    public static int mix(int a, int b, float t) {
        int r = (int) ((a >> 16 & 0xFF) * (1 - t) + (b >> 16 & 0xFF) * t);
        int g = (int) ((a >> 8 & 0xFF) * (1 - t) + (b >> 8 & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return r << 16 | g << 8 | bl;
    }
}
