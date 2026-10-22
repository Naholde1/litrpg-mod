package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.map.MapCache;
import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.world.map.WorldMap;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Мини-карта в духе JourneyMap/Xaero: круглая (или квадратная) в бронзовом кольце
 * с компасом С/В/Ю/З, цвета — по настоящим текстурам блоков с тенью рельефа, плавно
 * едет за игроком. Значки: враги, звери, жители, игроки, боссы, метки, города и
 * подземелья Мира Системы. Под картой — координаты, биом, опасность земли и время.
 * Клавиша N — масштаб, Shift+N — спрятать/показать; форма — кнопкой на карте мира (M).
 */
public class Minimap {
    /** Сколько блоков видно в поперечнике на каждом масштабе. */
    private static final int[] ZOOMS = {64, 128, 256};
    /** Сторона картинки-подложки: поперечник самого мелкого масштаба + запас на ходьбу. */
    private static final int COMP = 384;
    private static final ResourceLocation TEX_ID = LitRPG.id("dynamic/minimap");

    /** Своя мини-карта выключена по умолчанию, если стоит Xaero's Minimap или JourneyMap. */
    public static boolean enabled = !net.minecraftforge.fml.ModList.get().isLoaded("xaerominimap")
            && !net.minecraftforge.fml.ModList.get().isLoaded("journeymap");
    public static boolean round = true;
    public static int zoom = 1;
    private static boolean loaded;

    private static DynamicTexture texture;
    private static int compX, compZ;
    private static int compVersion = -1;
    private static String compLayer = "";
    private static int composeCooldown;

    // ================= настройки =================

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("litrpg_minimap.txt");
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        try {
            if (!Files.exists(file())) return;
            for (String line : Files.readAllLines(file(), StandardCharsets.UTF_8)) {
                String[] kv = line.split("=", 2);
                if (kv.length < 2) continue;
                switch (kv[0].trim()) {
                    case "enabled" -> enabled = Boolean.parseBoolean(kv[1].trim());
                    case "round" -> round = Boolean.parseBoolean(kv[1].trim());
                    case "zoom" -> zoom = Mth.clamp(Integer.parseInt(kv[1].trim()), 0, ZOOMS.length - 1);
                    default -> { }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        try {
            Files.writeString(file(), "enabled=" + enabled + "\nround=" + round + "\nzoom=" + zoom + "\n", StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
    }

    public static void cycleZoom() {
        load();
        zoom = (zoom + 1) % ZOOMS.length;
        save();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal("Мини-карта: " + ZOOMS[zoom] + " блоков в поперечнике"), true);
    }

    public static void toggle() {
        load();
        enabled = !enabled;
        save();
    }

    // ================= обновление =================

    public static void tick(Minecraft mc) {
        load();
        MapCache.tick(mc);
        if (!enabled || mc.player == null) return;
        if (texture == null) {
            texture = new DynamicTexture(COMP, COMP, true);
            mc.getTextureManager().register(TEX_ID, texture);
            compVersion = -1;
        }
        int px = Mth.floor(mc.player.getX()), pz = Mth.floor(mc.player.getZ());
        boolean moved = Math.abs(px - (compX + COMP / 2)) > 40 || Math.abs(pz - (compZ + COMP / 2)) > 40;
        boolean changed = compVersion != MapCache.version || !compLayer.equals(MapCache.layer());
        if (--composeCooldown > 0 && !moved) return;
        if (moved || changed) {
            compX = (px - COMP / 2) & ~7;
            compZ = (pz - COMP / 2) & ~7;
            NativeImage img = texture.getPixels();
            if (img == null) return;
            MapCache.copyTo(img, compX, compZ, COMP, COMP);
            texture.upload();
            compVersion = MapCache.version;
            compLayer = MapCache.layer();
            composeCooldown = 5;
        }
    }

    // ================= рисование =================

    /** @return высота занятого места (карта + подписи) */
    public static int render(GuiGraphics g, int x, int y, int size, float pt) {
        Minecraft mc = Minecraft.getInstance();
        load();
        if (!enabled || mc.player == null || texture == null) return 0;
        float r = size / 2f;
        float cx = x + r, cy = y + r;
        double wx = Mth.lerp(pt, mc.player.xo, mc.player.getX()), wz = Mth.lerp(pt, mc.player.zo, mc.player.getZ());
        float blocks = ZOOMS[zoom];
        float scale = size / blocks; // пикселей на блок
        float uc = (float) ((wx - compX) / COMP), vc = (float) ((wz - compZ) / COMP);
        float ur = blocks / 2f / COMP;
        Matrix4f m = g.pose().last().pose();

        // подложка (неразведанное) и сама карта
        if (round) {
            disk(m, cx, cy, r + 1, 0xFF15171D);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEX_ID);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_TEX);
            bb.vertex(m, cx, cy, 0).uv(uc, vc).endVertex();
            for (int i = 0; i <= 72; i++) {
                double a = -i * Math.PI * 2 / 72;
                float c = (float) Math.cos(a), s = (float) Math.sin(a);
                bb.vertex(m, cx + c * r, cy + s * r, 0).uv(uc + c * ur, vc + s * ur).endVertex();
            }
            BufferUploader.drawWithShader(bb.end());
        } else {
            g.fill(x, y, x + size, y + size, 0xFF15171D);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TEX_ID);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bb.vertex(m, x, y, 0).uv(uc - ur, vc - ur).endVertex();
            bb.vertex(m, x, y + size, 0).uv(uc - ur, vc + ur).endVertex();
            bb.vertex(m, x + size, y + size, 0).uv(uc + ur, vc + ur).endVertex();
            bb.vertex(m, x + size, y, 0).uv(uc + ur, vc - ur).endVertex();
            BufferUploader.drawWithShader(bb.end());
        }
        RenderSystem.disableBlend();

        // значки Мира Системы: города, подземелья, чудеса, поселения
        if (systemWorld(mc)) {
            for (WorldMap.Poi p : WorldMap.get().pois) {
                float ix = cx + (float) ((p.x() - wx) * scale), iy = cy + (float) ((p.z() - wz) * scale);
                if (!inside(ix, iy, cx, cy, r - 3, x, y, size)) continue;
                poiIcon(g, p.type(), Math.round(ix), Math.round(iy));
            }
        }

        // существа
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || !le.isAlive() || e.isInvisible()) continue;
            if (e instanceof com.andrey.litrpg.entity.wild.WildMob w && w.isDormant()) continue; // мимик притворяется сундуком
            float ex = cx + (float) ((Mth.lerp(pt, e.xo, e.getX()) - wx) * scale), ez = cy + (float) ((Mth.lerp(pt, e.zo, e.getZ()) - wz) * scale);
            if (!inside(ex, ez, cx, cy, r - 2, x, y, size)) continue;
            int ix = Math.round(ex), iz = Math.round(ez);
            if (e instanceof Player) arrow(m, ix, iz, e.getYRot(), 0.75f, 0xFF4FC3F7);
            else if (e instanceof LitBoss) dot(g, ix, iz, 3, 0xFFE040FB);
            else if (e instanceof Enemy) dot(g, ix, iz, 2, 0xFFFF4A3D);
            else if (e instanceof RaceNpcEntity npc) dot(g, ix, iz, 1, npc.npcType().outlaw() ? 0xFFFF4A3D : 0xFFFFD54F);
            else if (e instanceof Animal || e instanceof com.andrey.litrpg.entity.wild.WildMob) dot(g, ix, iz, 1, 0xFFC8F0B0);
        }

        // метки игрока: за краем — прижаты к кольцу
        for (Waypoints.Waypoint w : Waypoints.forCurrentDimension()) {
            float ex = cx + (float) ((w.x() - wx) * scale), ez = cy + (float) ((w.z() - wz) * scale);
            if (!inside(ex, ez, cx, cy, r - 4, x, y, size)) {
                float dx = ex - cx, dz = ez - cy;
                float k = round ? (r - 4) / Mth.sqrt(dx * dx + dz * dz) : (r - 4) / Math.max(Math.abs(dx), Math.abs(dz));
                ex = cx + dx * k;
                ez = cy + dz * k;
            }
            diamond(g, Math.round(ex), Math.round(ez), 0xFFFFD54F);
        }

        // игрок
        arrow(m, cx, cy, mc.player.getYRot(), 1f, 0xFFFFFFFF);

        // рамка и компас
        if (round) ring(g, m, cx, cy, r, mc.font);
        else squareFrame(g, x, y, size, mc.font);

        RenderSystem.enableCull();
        return size + 6 + info(g, mc, x, y + size + (round ? 11 : 7), size);
    }

    private static boolean inside(float px, float py, float cx, float cy, float r, int x, int y, int size) {
        if (round) return (px - cx) * (px - cx) + (py - cy) * (py - cy) <= r * r;
        return px >= x + 2 && py >= y + 2 && px <= x + size - 2 && py <= y + size - 2;
    }

    private static Object sysLevel;
    private static boolean sys;

    /** Мир Системы: в Верхнем мире под ногами хоть раз был биом мода (запоминается до смены мира). */
    public static boolean systemWorld(Minecraft mc) {
        if (mc.level == null || mc.player == null || mc.level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
        if (sysLevel != mc.level) {
            sysLevel = mc.level;
            sys = false;
        }
        if (!sys) {
            sys = mc.level.getBiome(mc.player.blockPosition()).unwrapKey()
                    .map(k -> k.location().getNamespace().equals(LitRPG.MODID) && !k.location().getPath().equals("shadow_lands")).orElse(false);
        }
        return sys;
    }

    /** Подписи под картой. @return их высота */
    private static int info(GuiGraphics g, Minecraft mc, int x, int y, int size) {
        Font font = mc.font;
        BlockPos p = mc.player.blockPosition();
        String coords = p.getX() + "  " + p.getY() + "  " + p.getZ();
        String biome = mc.level.getBiome(p).unwrapKey()
                .map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()).getString()).orElse("");
        if (MapCache.caveLayer()) biome = "Пещеры • " + biome;
        long day = mc.level.getDayTime() % 24000;
        int hh = (int) ((day / 1000 + 6) % 24), mm = (int) (day % 1000 * 60 / 1000);
        boolean night = day >= 13000 && day < 23000;
        String time = (night ? "☾ " : "☀ ") + String.format("%02d:%02d", hh, mm);
        String danger = null;
        int dangerColor = 0xFFB0BEC5;
        if (systemWorld(mc)) {
            int lv = WorldMap.get().levelAt(p.getX(), p.getZ());
            int diff = lv - ClientData.STATS.getLevel();
            danger = "Земли ур. " + lv;
            dangerColor = diff <= -4 ? 0xFF8BC34A : diff <= 3 ? 0xFFFFE082 : diff <= 7 ? 0xFFFFA040 : 0xFFFF5040;
        }
        int lines = danger == null ? 3 : 4;
        int h = lines * 10 + 3;
        int w = size + 8;
        for (String s : new String[]{coords, biome, time, danger == null ? "" : danger}) w = Math.max(w, font.width(s) + 8);
        int bx = Math.max(2, x + size / 2 - w / 2);
        g.fill(bx, y - 2, bx + w, y + h - 2, 0x90080A10);
        g.fill(bx, y - 2, bx + w, y - 1, 0x60C9A45C);
        int cx = bx + w / 2;
        g.drawString(font, coords, cx - font.width(coords) / 2, y, 0xFFE8E8E8);
        g.drawString(font, biome, cx - font.width(biome) / 2, y + 10, 0xFFE6C870);
        int ty = y + 20;
        if (danger != null) {
            g.drawString(font, danger, cx - font.width(danger) / 2, ty, dangerColor);
            ty += 10;
        }
        g.drawString(font, time, cx - font.width(time) / 2, ty, night ? 0xFF9FB8FF : 0xFFFFE9A0);
        return h + 6;
    }

    // ================= примитивы =================

    private static void disk(Matrix4f m, float cx, float cy, float r, int color) {
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0).color(color).endVertex();
        for (int i = 0; i <= 72; i++) {
            double a = -i * Math.PI * 2 / 72;
            bb.vertex(m, cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r, 0).color(color).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    /** Кольцо от r0 до r1 с разными цветами краёв (объём). */
    private static void annulus(Matrix4f m, float cx, float cy, float r0, float r1, int c0, int c1) {
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i <= 96; i++) {
            double a = -i * Math.PI * 2 / 96;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            // свет сверху-слева: верх кольца светлее
            float light = 0.85f + 0.15f * (float) -Math.sin(a + Math.PI / 4);
            bb.vertex(m, cx + c * r1, cy + s * r1, 0).color(shade(c1, light)).endVertex();
            bb.vertex(m, cx + c * r0, cy + s * r0, 0).color(shade(c0, light)).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static int shade(int argb, float k) {
        int r = Mth.clamp((int) ((argb >> 16 & 0xFF) * k), 0, 255), gg = Mth.clamp((int) ((argb >> 8 & 0xFF) * k), 0, 255),
                b = Mth.clamp((int) ((argb & 0xFF) * k), 0, 255);
        return argb & 0xFF000000 | r << 16 | gg << 8 | b;
    }

    private static void ring(GuiGraphics g, Matrix4f m, float cx, float cy, float r, Font font) {
        annulus(m, cx, cy, r + 5, r + 7, 0x70000000, 0x00000000);   // мягкая тень
        annulus(m, cx, cy, r, r + 1.2f, 0xFF1C140A, 0xFF1C140A);     // тёмная кромка
        annulus(m, cx, cy, r + 1.2f, r + 4.2f, 0xFFF0D080, 0xFF8A6428); // бронза
        annulus(m, cx, cy, r + 4.2f, r + 5.2f, 0xFF3A2810, 0xFF2A1C0A);
        // насечки через 45°
        for (int i = 0; i < 8; i++) {
            if (i % 2 == 0) continue;
            double a = i * Math.PI / 4 - Math.PI / 2;
            int tx = Math.round(cx + (float) Math.cos(a) * (r + 2.7f)), ty = Math.round(cy + (float) Math.sin(a) * (r + 2.7f));
            g.fill(tx - 1, ty - 1, tx + 1, ty + 1, 0xFF3A2810);
        }
        String[] letters = {"С", "В", "Ю", "З"};
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 - Math.PI / 2;
            int tx = Math.round(cx + (float) Math.cos(a) * (r + 2.7f)), ty = Math.round(cy + (float) Math.sin(a) * (r + 2.7f));
            disk(g.pose().last().pose(), tx, ty, 5.2f, 0xFF2A1C0A);
            disk(g.pose().last().pose(), tx, ty, 4.4f, i == 0 ? 0xFF8A1A14 : 0xFF4A3418);
            String s = letters[i];
            g.drawString(font, s, tx - font.width(s) / 2 + 1, ty - 3, i == 0 ? 0xFFFFE0D0 : 0xFFE8D8B0, false);
        }
    }

    private static void squareFrame(GuiGraphics g, int x, int y, int size, Font font) {
        g.fill(x - 4, y - 4, x + size + 4, y - 1, 0xFF8A6428);
        g.fill(x - 4, y + size + 1, x + size + 4, y + size + 4, 0xFF6A4A1C);
        g.fill(x - 4, y - 1, x - 1, y + size + 1, 0xFF8A6428);
        g.fill(x + size + 1, y - 1, x + size + 4, y + size + 1, 0xFF6A4A1C);
        g.fill(x - 4, y - 4, x + size + 4, y - 3, 0xFFF0D080);
        g.fill(x - 1, y - 1, x + size + 1, y, 0xFF1C140A);
        g.fill(x - 1, y + size, x + size + 1, y + size + 1, 0xFF1C140A);
        g.fill(x - 1, y, x, y + size, 0xFF1C140A);
        g.fill(x + size, y, x + size + 1, y + size, 0xFF1C140A);
        int nx = x + size / 2;
        g.fill(nx - 5, y - 6, nx + 5, y + 4, 0xFF8A1A14);
        g.drawString(font, "С", nx - font.width("С") / 2 + 1, y - 4, 0xFFFFE0D0, false);
    }

    private static void dot(GuiGraphics g, int x, int y, int s, int color) {
        g.fill(x - s - 1, y - s - 1, x + s + 1, y + s + 1, 0xC0000000);
        g.fill(x - s, y - s, x + s, y + s, color);
    }

    private static void diamond(GuiGraphics g, int x, int y, int color) {
        for (int i = -3; i <= 3; i++) {
            int w = 3 - Math.abs(i);
            g.fill(x - w - 1, y + i, x + w + 2, y + i + 1, 0xFF000000);
        }
        for (int i = -2; i <= 2; i++) {
            int w = 2 - Math.abs(i);
            g.fill(x - w, y + i, x + w + 1, y + i + 1, color);
        }
    }

    /** Стрелка взгляда (треугольник с выемкой и тёмной обводкой). */
    public static void arrow(Matrix4f m, float x, float y, float yaw, float k, int color) {
        float a = (float) Math.toRadians(yaw + 180);
        tri(m, x, y, a, 7.5f * k, 0xFF000000);
        tri(m, x, y, a, 5.5f * k, color);
    }

    private static void tri(Matrix4f m, float x, float y, float a, float s, int color) {
        float sin = Mth.sin(a), cos = Mth.cos(a);
        float[][] pts = {{0, -s}, {-s * 0.75f, s * 0.8f}, {0, s * 0.35f}, {s * 0.75f, s * 0.8f}};
        float[][] w = new float[4][2];
        for (int i = 0; i < 4; i++) {
            w[i][0] = x + pts[i][0] * cos - pts[i][1] * sin;
            w[i][1] = y + pts[i][0] * sin + pts[i][1] * cos;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        for (int[] t : new int[][]{{0, 1, 2}, {0, 2, 3}}) {
            for (int i : t) bb.vertex(m, w[i][0], w[i][1], 0).color(color).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
    }

    /** Значок места Мира Системы. */
    public static void poiIcon(GuiGraphics g, String type, int x, int y) {
        switch (type) {
            case "city" -> {
                g.fill(x - 4, y - 3, x + 5, y + 4, 0xFF000000);
                g.fill(x - 3, y - 2, x + 4, y + 3, 0xFFC8443A);
                g.fill(x - 3, y - 5, x - 1, y - 2, 0xFF000000);
                g.fill(x + 2, y - 5, x + 4, y - 2, 0xFF000000);
                g.fill(x, y + 1, x + 1, y + 3, 0xFF3A1A10);
            }
            case "dungeon" -> {
                g.fill(x - 3, y - 3, x + 4, y + 4, 0xFF000000);
                g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF7A1010);
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFF6040);
            }
            case "wonder" -> diamond(g, x, y, 0xFFD070FF);
            case "obelisk" -> {
                g.fill(x - 2, y - 5, x + 3, y + 4, 0xFF000000);
                g.fill(x - 1, y - 4, x + 2, y + 3, 0xFF7A3FD0);
                g.fill(x, y - 3, x + 1, y + 1, 0xFFE0B0FF);
            }
            case "magetower" -> {
                g.fill(x - 3, y - 2, x + 4, y + 5, 0xFF000000);
                g.fill(x - 2, y - 1, x + 3, y + 4, 0xFFB0B0C0);
                g.fill(x - 2, y - 5, x + 3, y - 1, 0xFF000000);
                g.fill(x - 1, y - 4, x + 2, y - 1, 0xFF8A50E0);
            }
            case "arena" -> {
                g.fill(x - 4, y - 4, x + 5, y + 5, 0xFF000000);
                g.fill(x - 3, y - 3, x + 4, y + 4, 0xFFB04030);
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFE8D090);
            }
            case "nest" -> dot(g, x, y, 2, 0xFFE09030);
            case "witch" -> dot(g, x, y, 2, 0xFF60A040);
            case "star" -> diamond(g, x, y, 0xFFF0C0FF);
            case "pirates" -> dot(g, x, y, 2, 0xFF202020);
            case "spawn" -> { }
            default -> {
                if (type.startsWith("nether")) return;
                g.fill(x - 2, y - 2, x + 3, y + 3, 0xFF000000);
                g.fill(x - 1, y - 1, x + 2, y + 2, 0xFFC09060);
            }
        }
    }

    /** Старая точка-стрелка (для атласа). */
    public static void drawPlayerArrow(GuiGraphics g, int px, int pz, float yaw) {
        arrow(g.pose().last().pose(), px, pz, yaw, 0.8f, 0xFFFFFFFF);
    }
}
