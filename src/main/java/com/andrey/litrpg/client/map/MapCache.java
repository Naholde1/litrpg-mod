package com.andrey.litrpg.client.map;

import com.andrey.litrpg.LitRPG;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Память карты (как у JourneyMap): всё, что игрок видел, хранится областями 256×256
 * блоков — картинкой в памяти и PNG-файлом на диске (папка litrpg_map). Чанки вокруг
 * игрока пересматриваются по очереди: ближние часто, дальние редко. Под землёй и в
 * Преисподней — отдельный «пещерный» слой на каждые 16 блоков высоты.
 */
public final class MapCache {
    private MapCache() {}

    public static final int REGION = 256;
    /** Сколько областей держать в памяти (по 256 КБ). */
    private static final int MAX_LOADED = 120;

    public static final class Region {
        final String layer;
        public final int rx, rz;
        NativeImage img;
        DynamicTexture tex;
        ResourceLocation texId;
        boolean texDirty, unsaved;
        long used;

        Region(String layer, int rx, int rz, NativeImage img) {
            this.layer = layer;
            this.rx = rx;
            this.rz = rz;
            this.img = img;
        }

        /** Текстура для полноэкранной карты (загружается в видеокарту только при изменениях). */
        public ResourceLocation texture() {
            if (tex == null) {
                tex = new DynamicTexture(img);
                texId = LitRPG.id("map/" + layer + "/" + (rx & 0xFFFF) + "_" + (rz & 0xFFFF) + "_" + System.identityHashCode(this));
                Minecraft.getInstance().getTextureManager().register(texId, tex);
                texDirty = false;
            } else if (texDirty) {
                tex.upload();
                texDirty = false;
            }
            used = tick;
            return texId;
        }

        void close() {
            if (tex != null) {
                Minecraft.getInstance().getTextureManager().release(texId);
                tex = null; // DynamicTexture закрыла и картинку
            } else if (img != null) {
                img.close();
            }
            img = null;
        }
    }

    private record Key(String layer, int rx, int rz) {}

    private static final Map<Key, Region> LOADED = new HashMap<>();
    private static final Set<Key> ABSENT = new HashSet<>();
    /** Когда чанк последний раз осмотрен (для текущего слоя). */
    private static final Map<Long, Long> SCANNED = new HashMap<>();
    private static ClientLevel lastLevel;
    private static String layer = "surface";
    private static long tick;
    /** Растёт при каждом изменении карты — мини-карта по нему понимает, что пора перерисоваться. */
    public static int version;
    private static Path dir;

    public static String layer() {
        return layer;
    }

    public static boolean caveLayer() {
        return !layer.equals("surface");
    }

    // ================= обход =================

    public static void tick(Minecraft mc) {
        tick++;
        ClientLevel level = mc.level;
        Player p = mc.player;
        if (level != lastLevel) {
            reset();
            lastLevel = level;
            dir = level == null ? null : folder(level);
        }
        if (level == null || p == null) return;
        String want = pickLayer(level, p);
        if (!want.equals(layer)) {
            layer = want;
            SCANNED.clear();
            version++;
        }
        int pcx = p.chunkPosition().x, pcz = p.chunkPosition().z;
        int radius = Math.min(12, mc.options.getEffectiveRenderDistance());
        int budget = caveLayer() ? 3 : 5;
        for (int r = 0; r <= radius && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = pcx + dx, cz = pcz + dz;
                    long key = (long) cx << 32 | (cz & 0xFFFFFFFFL);
                    Long t = SCANNED.get(key);
                    long age = t == null ? Long.MAX_VALUE : tick - t;
                    long every = r <= 1 ? 60 : r <= 3 ? 200 : r <= 6 ? 600 : 2400;
                    if (age < every) continue;
                    LevelChunk ch = level.getChunkSource().getChunk(cx, cz, false);
                    if (ch == null) continue;
                    scan(level, ch, p);
                    SCANNED.put(key, tick);
                    budget--;
                }
            }
        }
        if (tick % 600 == 0) saveAll(false);
        if (LOADED.size() > MAX_LOADED) evict();
    }

    /** Поверхность — если над игроком небо; иначе пещерный слой его высоты (в Преисподней всегда). */
    private static String pickLayer(ClientLevel level, Player p) {
        int y = Mth.floor(p.getY());
        if (level.dimensionType().hasCeiling()) return "cave" + (y >> 4);
        BlockPos head = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, head.getX(), head.getZ());
        if (y < surface - 8 && !level.canSeeSky(head) && level.getBrightness(net.minecraft.world.level.LightLayer.SKY, head) == 0) {
            return "cave" + (y >> 4);
        }
        return "surface";
    }

    // ================= осмотр чанка =================

    private static void scan(ClientLevel level, LevelChunk ch, Player p) {
        int bx = ch.getPos().getMinBlockX(), bz = ch.getPos().getMinBlockZ();
        Region r = region(layer, Math.floorDiv(bx, REGION), Math.floorDiv(bz, REGION), true);
        if (r == null) return;
        int ox = Math.floorMod(bx, REGION), oz = Math.floorMod(bz, REGION);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        if (caveLayer()) {
            int top = (Integer.parseInt(layer.substring(4)) << 4) + 18;
            for (int dz = 0; dz < 16; dz++) {
                for (int dx = 0; dx < 16; dx++) {
                    r.img.setPixelRGBA(ox + dx, oz + dz, caveColumn(level, ch, bx + dx, bz + dz, top, pos));
                }
            }
        } else {
            int[] h = new int[17 * 17];
            for (int dz = -1; dz < 16; dz++) {
                for (int dx = -1; dx < 16; dx++) {
                    int y = dx >= 0 && dz >= 0 ? ch.getHeight(Heightmap.Types.WORLD_SURFACE, dx, dz)
                            : level.getHeight(Heightmap.Types.WORLD_SURFACE, bx + dx, bz + dz) - 1;
                    h[(dz + 1) * 17 + dx + 1] = y;
                }
            }
            for (int dz = 0; dz < 16; dz++) {
                for (int dx = 0; dx < 16; dx++) {
                    int y = h[(dz + 1) * 17 + dx + 1];
                    int hw = h[(dz + 1) * 17 + dx], hn = h[dz * 17 + dx + 1];
                    if (hw <= level.getMinBuildHeight()) hw = y;
                    if (hn <= level.getMinBuildHeight()) hn = y;
                    r.img.setPixelRGBA(ox + dx, oz + dz, surfaceColumn(level, ch, bx + dx, bz + dz, y, hw, hn, pos));
                }
            }
        }
        r.texDirty = true;
        r.unsaved = true;
        r.used = tick;
        version++;
    }

    private static int surfaceColumn(ClientLevel level, LevelChunk ch, int x, int z, int y, int hw, int hn, BlockPos.MutableBlockPos pos) {
        if (y <= level.getMinBuildHeight()) return 0;
        pos.set(x, y, z);
        BlockState s = ch.getBlockState(pos);
        int guard = 0;
        // стекло, барьеры и прочее без цвета — смотрим сквозь
        while ((s.isAir() || s.getMapColor(level, pos) == MapColor.NONE && s.getFluidState().isEmpty()) && pos.getY() > level.getMinBuildHeight() && guard++ < 24) {
            pos.move(0, -1, 0);
            s = ch.getBlockState(pos);
        }
        FluidState f = s.getFluidState();
        if (f.is(Fluids.WATER) || f.is(Fluids.FLOWING_WATER)) {
            int water = BlockColors.water(level, pos);
            int depth = 0;
            while (depth < 24 && pos.getY() > level.getMinBuildHeight()) {
                pos.move(0, -1, 0);
                s = ch.getBlockState(pos);
                depth++;
                if (!s.getFluidState().is(Fluids.WATER) && !s.getFluidState().is(Fluids.FLOWING_WATER)) break;
            }
            int floor = BlockColors.color(level, s, pos);
            float k = Math.min(0.94f, 0.5f + depth * 0.045f);
            int c = BlockColors.mix(floor, water, k);
            return abgr(c, 1.02f - Math.min(0.3f, depth * 0.012f));
        }
        int c = BlockColors.color(level, s, pos);
        // рельеф: свет с северо-запада, как на бумажной карте
        int diff = (y - hw) + (y - hn);
        float shade = 1f + Mth.clamp(diff * 0.07f, -0.3f, 0.22f);
        // высота чуть осветляет вершины
        shade *= 0.94f + Mth.clamp((y - 62) / 400f, 0f, 0.12f);
        return abgr(c, shade);
    }

    /** Пещеры: пол под игроком — первая твердь ниже воздуха, чем глубже — тем темнее. */
    private static int caveColumn(ClientLevel level, LevelChunk ch, int x, int z, int top, BlockPos.MutableBlockPos pos) {
        pos.set(x, top, z);
        boolean air = false;
        for (int i = 0; i < 40 && pos.getY() > level.getMinBuildHeight(); i++) {
            BlockState s = ch.getBlockState(pos);
            if (s.isAir()) {
                air = true;
            } else if (air) {
                FluidState f = s.getFluidState();
                int c = f.is(Fluids.LAVA) || f.is(Fluids.FLOWING_LAVA) ? BlockColors.lava()
                        : !f.isEmpty() ? BlockColors.water(level, pos) : BlockColors.color(level, s, pos);
                float shade = 1.08f - Math.min(0.55f, (top - pos.getY()) / 40f);
                return abgr(c, shade);
            }
            pos.move(0, -1, 0);
        }
        return 0;
    }

    public static int abgr(int rgb, float shade) {
        int r = Mth.clamp((int) ((rgb >> 16 & 0xFF) * shade), 0, 255);
        int g = Mth.clamp((int) ((rgb >> 8 & 0xFF) * shade), 0, 255);
        int b = Mth.clamp((int) ((rgb & 0xFF) * shade), 0, 255);
        return 0xFF000000 | b << 16 | g << 8 | r;
    }

    // ================= области =================

    /** Область текущего слоя (с диска, если её уже видели), или null. */
    public static Region region(int rx, int rz) {
        return region(layer, rx, rz, false);
    }

    private static Region region(String layer, int rx, int rz, boolean create) {
        Key k = new Key(layer, rx, rz);
        Region r = LOADED.get(k);
        if (r != null) {
            r.used = tick;
            return r;
        }
        if (!ABSENT.contains(k) || create) {
            NativeImage img = read(k);
            if (img == null && create) img = new NativeImage(REGION, REGION, true);
            if (img != null) {
                ABSENT.remove(k);
                r = new Region(layer, rx, rz, img);
                r.used = tick;
                LOADED.put(k, r);
                return r;
            }
            ABSENT.add(k);
        }
        return null;
    }

    /** Скопировать карту прямоугольника мира [x0, x0+w) × [z0, z0+h) в картинку (неизвестное — прозрачно). */
    public static void copyTo(NativeImage dest, int x0, int z0, int w, int h) {
        dest.fillRect(0, 0, w, h, 0);
        for (int rz = Math.floorDiv(z0, REGION); rz <= Math.floorDiv(z0 + h - 1, REGION); rz++) {
            for (int rx = Math.floorDiv(x0, REGION); rx <= Math.floorDiv(x0 + w - 1, REGION); rx++) {
                Region r = region(rx, rz);
                if (r == null) continue;
                int ax = Math.max(x0, rx * REGION), az = Math.max(z0, rz * REGION);
                int bx = Math.min(x0 + w, rx * REGION + REGION), bz = Math.min(z0 + h, rz * REGION + REGION);
                r.img.copyRect(dest, ax - rx * REGION, az - rz * REGION, ax - x0, az - z0, bx - ax, bz - az, false, false);
            }
        }
    }

    private static void evict() {
        LOADED.values().stream().sorted((a, b) -> Long.compare(a.used, b.used)).limit(LOADED.size() - MAX_LOADED + 16)
                .filter(r -> tick - r.used > 40).toList().forEach(r -> {
                    if (r.unsaved) write(r);
                    r.close();
                    LOADED.remove(new Key(r.layer, r.rx, r.rz));
                });
    }

    public static void saveAll(boolean close) {
        for (Iterator<Region> it = LOADED.values().iterator(); it.hasNext(); ) {
            Region r = it.next();
            if (r.unsaved) write(r);
            if (close) {
                r.close();
                it.remove();
            }
        }
    }

    public static void reset() {
        saveAll(true);
        LOADED.clear();
        ABSENT.clear();
        SCANNED.clear();
        layer = "surface";
        version++;
    }

    // ================= диск =================

    private static Path folder(ClientLevel level) {
        String world = com.andrey.litrpg.client.Waypoints.worldKey().replaceAll("[^A-Za-z0-9._-]", "_");
        String dim = level.dimension().location().toString().replaceAll("[^A-Za-z0-9._-]", "_");
        return Minecraft.getInstance().gameDirectory.toPath().resolve("litrpg_map").resolve(world).resolve(dim);
    }

    private static Path file(Key k) {
        return dir == null ? null : dir.resolve(k.layer).resolve("r." + k.rx + "." + k.rz + ".png");
    }

    private static NativeImage read(Key k) {
        Path f = file(k);
        if (f == null || !Files.exists(f)) return null;
        try (var in = Files.newInputStream(f)) {
            NativeImage img = NativeImage.read(NativeImage.Format.RGBA, in);
            if (img.getWidth() == REGION && img.getHeight() == REGION) return img;
            img.close();
        } catch (Exception e) {
            LitRPG.LOGGER.warn("map region {} unreadable", f);
        }
        return null;
    }

    private static void write(Region r) {
        Path f = file(new Key(r.layer, r.rx, r.rz));
        if (f == null || r.img == null) return;
        try {
            Files.createDirectories(f.getParent());
            r.img.writeToFile(f);
            r.unsaved = false;
        } catch (Exception e) {
            LitRPG.LOGGER.warn("map region {} not saved", f);
        }
    }
}
