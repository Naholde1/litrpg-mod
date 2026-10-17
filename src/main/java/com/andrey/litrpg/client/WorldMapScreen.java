package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.LitBoss;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Карта мира: всё, что загружено вокруг игрока.
 * ПКМ — поставить/убрать метку, колесо — масштаб.
 */
public class WorldMapScreen extends Screen {
    private static final ResourceLocation TEX_ID = LitRPG.id("dynamic/world_map");

    private DynamicTexture texture;
    private int radius;
    private int size;
    private int originX, originZ;
    private float zoom = 1.0f;

    private int mapX, mapY, mapDraw;

    public WorldMapScreen() {
        super(Component.literal("Карта мира"));
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (texture == null) {
            int view = mc.options.getEffectiveRenderDistance();
            radius = Mth.clamp((view + 1) * 16, 64, 320);
            size = radius * 2;
            originX = Mth.floor(mc.player.getX()) - radius;
            originZ = Mth.floor(mc.player.getZ()) - radius;
            texture = new DynamicTexture(size, size, true);
            NativeImage img = texture.getPixels();
            if (img != null) {
                int py = Mth.floor(mc.player.getY());
                for (int row = 0; row < size; row++) {
                    for (int col = 0; col < size; col++) {
                        img.setPixelRGBA(col, row, MapColors.columnColor(mc.level, originX + col, originZ + row, py));
                    }
                }
            }
            texture.upload();
            mc.getTextureManager().register(TEX_ID, texture);
        }
        layout();
    }

    private void layout() {
        int base = Math.min(width, height) - 50;
        mapDraw = (int) (base * zoom);
        mapX = width / 2 - mapDraw / 2;
        mapY = height / 2 - mapDraw / 2 + 6;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        Minecraft mc = Minecraft.getInstance();
        if (texture == null || mc.player == null || mc.level == null) {
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        String title = "КАРТА — " + dimensionName(mc);
        g.drawString(font, title, width / 2 - font.width(title) / 2, 8, 0xFF8FE3FF);

        g.enableScissor(0, 22, width, height - 14);
        g.fill(mapX - 2, mapY - 2, mapX + mapDraw + 2, mapY + mapDraw + 2, 0xFF7F5FD0);
        g.blit(TEX_ID, mapX, mapY, mapDraw, mapDraw, 0, 0, size, size, size, size);

        float scale = mapDraw / (float) size;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            int color;
            int s;
            if (e instanceof LitBoss) { color = 0xFFE040FB; s = 4; }
            else if (e instanceof Player) { color = 0xFF4FC3F7; s = 3; }
            else if (e instanceof Enemy) { color = 0xFFFF3B30; s = 1; }
            else continue;
            int ex = mapX + (int) ((e.getX() - originX) * scale);
            int ez = mapY + (int) ((e.getZ() - originZ) * scale);
            g.fill(ex - s, ez - s, ex + s, ez + s, color);
        }

        for (Waypoints.Waypoint w : Waypoints.forCurrentDimension()) {
            int wx = mapX + (int) ((w.x() - originX) * scale);
            int wz = mapY + (int) ((w.z() - originZ) * scale);
            g.fill(wx - 3, wz - 3, wx + 3, wz + 3, 0xFF000000);
            g.fill(wx - 2, wz - 2, wx + 2, wz + 2, 0xFFFFD54F);
            g.drawString(font, w.name(), wx + 5, wz - 4, 0xFFFFD54F);
        }

        int px = mapX + (int) ((mc.player.getX() - originX) * scale);
        int pz = mapY + (int) ((mc.player.getZ() - originZ) * scale);
        Minimap.drawPlayerArrow(g, px, pz, mc.player.getYRot());
        g.disableScissor();

        // координаты под курсором
        if (mouseX >= mapX && mouseX < mapX + mapDraw && mouseY >= mapY && mouseY < mapY + mapDraw) {
            int wx = originX + (int) ((mouseX - mapX) / scale);
            int wz = originZ + (int) ((mouseY - mapY) / scale);
            String c = "X: " + wx + "  Z: " + wz;
            g.drawString(font, c, mouseX + 10, mouseY + 4, 0xFFFFFFFF);
        }

        String help = "ПКМ — метка  •  колесо — масштаб  •  красные — монстры, фиолетовые — боссы";
        g.drawString(font, help, width / 2 - font.width(help) / 2, height - 12, 0xFF999999);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static String dimensionName(Minecraft mc) {
        String id = mc.level.dimension().location().toString();
        return switch (id) {
            case "minecraft:overworld" -> "Верхний мир";
            case "minecraft:the_nether" -> "Незер";
            case "minecraft:the_end" -> "Энд";
            case "litrpg:shadow_realm" -> "Изнанка";
            default -> id;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && texture != null && mouseX >= mapX && mouseX < mapX + mapDraw
                && mouseY >= mapY && mouseY < mapY + mapDraw) {
            float scale = mapDraw / (float) size;
            int wx = originX + (int) ((mouseX - mapX) / scale);
            int wz = originZ + (int) ((mouseY - mapY) / scale);
            int removeRadius = Math.max(4, (int) (6 / scale));
            if (!Waypoints.removeNear(wx, wz, removeRadius)) {
                Waypoints.add(wx, wz);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        zoom = Mth.clamp(zoom + (float) delta * 0.25f, 0.5f, 4.0f);
        layout();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.WORLD_MAP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEX_ID);
            texture = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
