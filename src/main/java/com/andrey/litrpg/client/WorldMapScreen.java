package com.andrey.litrpg.client;

import com.andrey.litrpg.client.map.MapCache;
import com.andrey.litrpg.entity.LitBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Карта мира в духе JourneyMap: всё разведанное (память карты хранится на диске),
 * ЛКМ — тянуть, колесо — масштаб, ПКМ — метка, Пробел — к себе.
 * В Мире Системы — города, подземелья, чудеса и поселения с названиями.
 */
public class WorldMapScreen extends Screen {
    private double centerX, centerZ;
    /** Пикселей экрана на блок. */
    private double scale = 1.0;
    private boolean dragging;
    private Button shapeBtn, showBtn;

    public WorldMapScreen() {
        super(Component.literal("Карта мира"));
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            centerX = mc.player.getX();
            centerZ = mc.player.getZ();
        }
    }

    @Override
    protected void init() {
        shapeBtn = addRenderableWidget(Button.builder(shapeText(), b -> {
            Minimap.round = !Minimap.round;
            Minimap.save();
            b.setMessage(shapeText());
        }).bounds(width - 136, 4, 130, 16).build());
        showBtn = addRenderableWidget(Button.builder(showText(), b -> {
            Minimap.toggle();
            b.setMessage(showText());
        }).bounds(width - 270, 4, 130, 16).build());
    }

    private static Component shapeText() {
        return Component.literal("Мини-карта: " + (Minimap.round ? "круглая" : "квадратная"));
    }

    private static Component showText() {
        return Component.literal("Мини-карта: " + (Minimap.enabled ? "видна" : "скрыта"));
    }

    private int sx(double wx) {
        return (int) Math.floor(width / 2.0 + (wx - centerX) * scale);
    }

    private int sz(double wz) {
        return (int) Math.floor(height / 2.0 + (wz - centerZ) * scale);
    }

    private double wx(double sx) {
        return centerX + (sx - width / 2.0) / scale;
    }

    private double wz(double sy) {
        return centerZ + (sy - height / 2.0) / scale;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        Minecraft mc = Minecraft.getInstance();
        g.fill(0, 0, width, height, 0xFF0E1014);
        if (mc.player == null || mc.level == null) {
            super.render(g, mouseX, mouseY, pt);
            return;
        }
        int top = 24, bottom = height - 16;
        g.enableScissor(0, top, width, bottom);
        // сетка-подложка: неразведанное
        int step = (int) Math.max(16, 64 * scale);
        for (int gx = Math.floorMod(sx(0), step); gx < width; gx += step) g.fill(gx, top, gx + 1, bottom, 0xFF161A20);
        for (int gz = Math.floorMod(sz(0), step); gz < height; gz += step) g.fill(0, gz, width, gz + 1, 0xFF161A20);
        // области памяти карты
        int R = MapCache.REGION;
        int rx0 = Math.floorDiv((int) Math.floor(wx(0)), R), rx1 = Math.floorDiv((int) Math.floor(wx(width)), R);
        int rz0 = Math.floorDiv((int) Math.floor(wz(top)), R), rz1 = Math.floorDiv((int) Math.floor(wz(bottom)), R);
        for (int rz = rz0; rz <= rz1; rz++) {
            for (int rx = rx0; rx <= rx1; rx++) {
                MapCache.Region r = MapCache.region(rx, rz);
                if (r == null) continue;
                int x0 = sx(rx * R), x1 = sx(rx * R + R), z0 = sz(rz * R), z1 = sz(rz * R + R);
                g.blit(r.texture(), x0, z0, x1 - x0, z1 - z0, 0, 0, R, R, R, R);
            }
        }

        List<Component> tip = new ArrayList<>();
        // существа рядом
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || e.isInvisible()) continue;
            int x = sx(e.getX()), z = sz(e.getZ());
            if (e instanceof Player) Minimap.arrow(g.pose().last().pose(), x, z, e.getYRot(), 0.8f, 0xFF4FC3F7);
            else if (e instanceof LitBoss) g.fill(x - 3, z - 3, x + 3, z + 3, 0xFFE040FB);
            else if (e instanceof Enemy && !(e instanceof com.andrey.litrpg.entity.wild.WildMob w && w.isDormant())) g.fill(x - 1, z - 1, x + 2, z + 2, 0xFFFF4A3D);
        }
        // метки
        for (Waypoints.Waypoint w : Waypoints.forCurrentDimension()) {
            int x = sx(w.x()), z = sz(w.z());
            g.fill(x - 3, z - 3, x + 4, z + 4, 0xFF000000);
            g.fill(x - 2, z - 2, x + 3, z + 3, 0xFFFFD54F);
            g.drawString(font, w.name(), x + 6, z - 4, 0xFFFFD54F);
        }
        Minimap.arrow(g.pose().last().pose(), sx(Mth.lerp(pt, mc.player.xo, mc.player.getX())), sz(Mth.lerp(pt, mc.player.zo, mc.player.getZ())),
                mc.player.getYRot(), 1.1f, 0xFFFFFFFF);
        g.disableScissor();

        // рамка и заголовок
        g.fill(0, 0, width, top, 0xF0161210);
        g.fill(0, top - 1, width, top, 0xFF8A6428);
        g.fill(0, bottom, width, height, 0xF0161210);
        g.fill(0, bottom, width, bottom + 1, 0xFF8A6428);
        String title = "КАРТА — " + dimensionName(mc) + (MapCache.caveLayer() ? " • пещеры" : "");
        g.drawString(font, title, 8, 8, 0xFFF0D080);
        String sc = scale >= 1 ? "1:" + (int) scale : (int) Math.round(1 / scale) + ":1";
        g.drawString(font, "масштаб " + sc, 12 + font.width(title), 8, 0xFF9A8A6A);

        // координаты и биом под курсором
        if (mouseY > top && mouseY < bottom) {
            int x = (int) Math.floor(wx(mouseX)), z = (int) Math.floor(wz(mouseY));
            String c = "X " + x + "  Z " + z;
            if (mc.level.hasChunk(x >> 4, z >> 4)) {
                c += "  •  " + mc.level.getBiome(new BlockPos(x, mc.player.getBlockY(), z)).unwrapKey()
                        .map(k -> Component.translatable("biome." + k.location().getNamespace() + "." + k.location().getPath()).getString()).orElse("");
            }
            g.drawString(font, c, 8, bottom + 4, 0xFFE8E8E8);
        }
        String help = "ЛКМ — двигать • колесо — масштаб • ПКМ — метка • Пробел — к себе • N — масштаб мини-карты";
        g.drawString(font, help, width - font.width(help) - 8, bottom + 4, 0xFF8A8070);

        super.render(g, mouseX, mouseY, pt);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mouseX, mouseY);
    }

    private static String dimensionName(Minecraft mc) {
        String id = mc.level.dimension().location().toString();
        return switch (id) {
            case "minecraft:overworld" -> "Верхний мир";
            case "minecraft:the_nether" -> "Преисподняя";
            case "minecraft:the_end" -> "Энд";
            case "litrpg:shadow_realm" -> "Изнанка";
            default -> id;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (mouseY < 24 || mouseY > height - 16) return false;
        if (button == 0) {
            dragging = true;
            return true;
        }
        if (button == 1) {
            int x = (int) Math.floor(wx(mouseX)), z = (int) Math.floor(wz(mouseY));
            if (!Waypoints.removeNear(x, z, Math.max(4, (int) (6 / scale)))) Waypoints.add(x, z);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
            centerX -= dx / scale;
            centerZ -= dy / scale;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // масштаб вокруг курсора
        double bx = wx(mouseX), bz = wz(mouseY);
        scale = Mth.clamp(scale * (delta > 0 ? 1.25 : 0.8), 1 / 16.0, 8.0);
        centerX = bx - (mouseX - width / 2.0) / scale;
        centerZ = bz - (mouseY - height / 2.0) / scale;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.WORLD_MAP.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE && minecraft.player != null) {
            centerX = minecraft.player.getX();
            centerZ = minecraft.player.getZ();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
