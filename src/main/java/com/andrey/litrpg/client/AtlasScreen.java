package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Атлас «Мира Системы»: вся карта 10000×10000 — земли, города, дороги, подземелья и чудеса. */
public class AtlasScreen extends Screen {
    private static final ResourceLocation ATLAS = LitRPG.id("textures/gui/atlas.png");
    private int mapX, mapY, mapSize;

    public AtlasScreen() {
        super(Component.literal("Атлас мира"));
    }

    @Override
    protected void init() {
        mapSize = Math.min(width - 40, height - 40);
        mapX = (width - mapSize) / 2;
        mapY = (height - mapSize) / 2 + 6;
    }

    private int px(double x) {
        return mapX + (int) ((x + 5000) / 10000.0 * mapSize);
    }

    private int pz(double z) {
        return mapY + (int) ((z + 5000) / 10000.0 * mapSize);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.fill(mapX - 3, mapY - 3, mapX + mapSize + 3, mapY + mapSize + 3, 0xFF3A2A18);
        g.blit(ATLAS, mapX, mapY, mapSize, mapSize, 0, 0, 512, 512, 512, 512);
        String title = "◆ АТЛАС МИРА СИСТЕМЫ ◆";
        g.drawString(font, title, width / 2 - font.width(title) / 2, mapY - 14, 0xFFE8D0A0);
        Minecraft mc = Minecraft.getInstance();
        List<Component> tip = new ArrayList<>();
        WorldMap map = WorldMap.get();
        for (WorldMap.Poi p : map.pois) {
            if (p.type().startsWith("nether") || p.type().equals("spawn")) continue;
            int x = px(p.x()), z = pz(p.z());
            if (Math.abs(mx - x) <= 3 && Math.abs(my - z) <= 3) {
                String kind = switch (p.type()) {
                    case "city" -> "город";
                    case "dungeon" -> "подземелье";
                    case "wonder" -> "чудо света";
                    case "village" -> "деревня";
                    case "farm" -> "хутор";
                    case "hamlet" -> "придорожный хутор";
                    case "fishing" -> "рыбацкий посёлок";
                    case "lumber" -> "лесопилка";
                    case "caravan" -> "стоянка караванщиков";
                    case "outpost" -> "застава";
                    case "ruins" -> "руины";
                    case "pirates" -> "пиратский корабль — опасно!";
                    case "camp" -> "лагерь";
                    default -> p.type();
                };
                tip.add(Component.literal(p.name()).withStyle(s -> s.withColor(0xFFE8B0)));
                tip.add(Component.literal(kind + " • X " + p.x() + ", Z " + p.z()).withStyle(s -> s.withColor(0xB0A080)));
                if (p.type().equals("dungeon")) {
                    var d = com.andrey.litrpg.world.Dungeon.byKey(p.extra());
                    if (d != null) tip.add(Component.literal("Уровень " + d.level).withStyle(s -> s.withColor(0xFF8060)));
                }
            }
        }
        // метки игрока
        for (Waypoints.Waypoint w : Waypoints.forCurrentDimension()) {
            int x = px(w.x()), z = pz(w.z());
            g.fill(x - 2, z - 2, x + 2, z + 2, 0xFF000000);
            g.fill(x - 1, z - 1, x + 1, z + 1, 0xFFFFD54F);
        }
        if (mc.player != null) {
            int x = px(mc.player.getX()), z = pz(mc.player.getZ());
            Minimap.drawPlayerArrow(g, x, z, mc.player.getYRot());
            String here = "Вы здесь: X " + (int) mc.player.getX() + ", Z " + (int) mc.player.getZ();
            g.drawString(font, here, mapX, mapY + mapSize + 6, 0xFFE8D0A0);
        }
        String hint = "Наведите на значок — подробности • Tab/M — назад";
        g.drawString(font, hint, mapX + mapSize - font.width(hint), mapY + mapSize + 6, 0xFFB0A080);
        if (!tip.isEmpty()) g.renderComponentTooltip(font, tip, mx, my);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_TAB || ModKeys.WORLD_MAP.matches(key, scan)) {
            Minecraft.getInstance().setScreen(new WorldMapScreen());
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
