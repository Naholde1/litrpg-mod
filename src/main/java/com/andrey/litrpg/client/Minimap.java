package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.LitBoss;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** Мини-карта: 128×128 блоков вокруг игрока, север сверху. */
public class Minimap {
    public static final int SIZE = 128;           // блоков по стороне
    private static final int ROWS_PER_TICK = 16;  // полное обновление за 8 тиков
    private static final ResourceLocation TEX_ID = LitRPG.id("dynamic/minimap");

    public static boolean enabled = true;

    private static DynamicTexture texture;
    private static int scanRow = 0;
    private static int centerX, centerZ;
    private static boolean dirty = false;

    private static void ensureTexture(Minecraft mc) {
        if (texture == null) {
            texture = new DynamicTexture(SIZE, SIZE, true);
            NativeImage img = texture.getPixels();
            if (img != null) img.fillRect(0, 0, SIZE, SIZE, MapColors.UNKNOWN);
            mc.getTextureManager().register(TEX_ID, texture);
        }
    }

    public static void tick(Minecraft mc) {
        if (!enabled || mc.level == null || mc.player == null) return;
        ensureTexture(mc);
        NativeImage img = texture.getPixels();
        if (img == null) return;

        if (scanRow == 0) {
            centerX = Mth.floor(mc.player.getX());
            centerZ = Mth.floor(mc.player.getZ());
        }
        int py = Mth.floor(mc.player.getY());
        int half = SIZE / 2;
        for (int r = 0; r < ROWS_PER_TICK; r++) {
            int row = scanRow + r;
            if (row >= SIZE) break;
            int z = centerZ - half + row;
            for (int col = 0; col < SIZE; col++) {
                int x = centerX - half + col;
                img.setPixelRGBA(col, row, MapColors.columnColor(mc.level, x, z, py));
            }
        }
        scanRow += ROWS_PER_TICK;
        if (scanRow >= SIZE) scanRow = 0;
        dirty = true;
    }

    public static void render(GuiGraphics g, int x, int y, int drawSize) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled || mc.player == null || texture == null) return;
        if (dirty) {
            texture.upload();
            dirty = false;
        }

        // рамка
        g.fill(x - 2, y - 2, x + drawSize + 2, y + drawSize + 2, 0xFF2A2140);
        g.fill(x - 1, y - 1, x + drawSize + 1, y + drawSize + 1, 0xFF7F5FD0);
        g.blit(TEX_ID, x, y, drawSize, drawSize, 0, 0, SIZE, SIZE, SIZE, SIZE);

        float scale = drawSize / (float) SIZE;
        double ox = centerX - SIZE / 2.0;
        double oz = centerZ - SIZE / 2.0;

        // мобы и игроки
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            int color;
            int s;
            if (e instanceof LitBoss) { color = 0xFFE040FB; s = 3; }
            else if (e instanceof Player) { color = 0xFF4FC3F7; s = 2; }
            else if (e instanceof Enemy) { color = 0xFFFF3B30; s = 1; }
            else continue;
            int ex = x + (int) ((e.getX() - ox) * scale);
            int ez = y + (int) ((e.getZ() - oz) * scale);
            if (ex < x || ez < y || ex >= x + drawSize || ez >= y + drawSize) continue;
            g.fill(ex - s, ez - s, ex + s, ez + s, color);
        }

        // метки
        for (Waypoints.Waypoint w : Waypoints.forCurrentDimension()) {
            int wx = x + (int) ((w.x() - ox) * scale);
            int wz = y + (int) ((w.z() - oz) * scale);
            wx = Mth.clamp(wx, x + 1, x + drawSize - 2);
            wz = Mth.clamp(wz, y + 1, y + drawSize - 2);
            g.fill(wx - 2, wz - 2, wx + 2, wz + 2, 0xFF000000);
            g.fill(wx - 1, wz - 1, wx + 1, wz + 1, 0xFFFFD54F);
        }

        // игрок: точка + направление взгляда
        int px = x + (int) ((mc.player.getX() - ox) * scale);
        int pz = y + (int) ((mc.player.getZ() - oz) * scale);
        drawPlayerArrow(g, px, pz, mc.player.getYRot());

        g.drawString(mc.font, "N", x + drawSize / 2 - 2, y + 2, 0xFFFFFFFF);
    }

    public static void drawPlayerArrow(GuiGraphics g, int px, int pz, float yaw) {
        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        for (int i = 1; i <= 5; i++) {
            int lx = px + (int) Math.round(dx * i);
            int lz = pz + (int) Math.round(dz * i);
            g.fill(lx, lz, lx + 1, lz + 1, 0xFFFFFFFF);
        }
        g.fill(px - 2, pz - 2, px + 2, pz + 2, 0xFF000000);
        g.fill(px - 1, pz - 1, px + 1, pz + 1, 0xFFFFFFFF);
    }
}
