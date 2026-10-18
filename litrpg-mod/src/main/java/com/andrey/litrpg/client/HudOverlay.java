package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class HudOverlay {
    private static final int MAP_SIZE = 96;

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        Font font = mc.font;
        PlayerStats s = ClientData.STATS;

        int x = 6;
        int y = 6;

        // --- мини-карта ---
        if (Minimap.enabled) {
            Minimap.render(g, x, y, MAP_SIZE);
            String coords = Mth.floor(mc.player.getX()) + " " + Mth.floor(mc.player.getY()) + " " + Mth.floor(mc.player.getZ());
            g.drawString(font, coords, x + MAP_SIZE / 2 - font.width(coords) / 2, y + MAP_SIZE + 4, 0xFFE0E0E0);
            y += MAP_SIZE + 16;
        }

        // --- панель уровня ---
        int panelW = 110;
        g.fill(x - 2, y - 2, x + panelW + 2, y + 31, 0x90000000);
        String lvl = "Ур. " + s.getLevel();
        g.drawString(font, lvl, x, y, 0xFFFFD54F);
        if (s.getFreePoints() > 0) {
            String pts = "+" + s.getFreePoints() + " [K]";
            g.drawString(font, pts, x + panelW - font.width(pts), y, 0xFF7CFC00);
        }
        y += 11;

        // опыт
        float xpFrac = s.getLevel() >= PlayerStats.MAX_LEVEL ? 1f : s.getXp() / (float) s.xpForNext();
        bar(g, x, y, panelW, 5, xpFrac, 0xFF1F3A1F, 0xFF66DD44);
        y += 7;
        // мана
        float manaFrac = (float) (s.getMana() / Math.max(1, s.getMaxMana()));
        bar(g, x, y, panelW, 5, manaFrac, 0xFF1A2340, 0xFF3D8BFF);
        String manaText = (int) s.getMana() + "/" + s.getMaxMana();
        y += 7;
        g.drawString(font, manaText, x, y - 1, 0xFF9EC5FF, false);

        // --- панель навыков (снизу слева) ---
        int slot = 20;
        int ax = 6;
        int ay = height - slot - 6;
        Ability[] abilities = Ability.values();
        for (int i = 0; i < abilities.length; i++) {
            Ability a = abilities[i];
            int sx = ax + i * (slot + 3);
            boolean unlocked = s.getLevel() >= a.unlockLevel;
            int cd = ClientData.COOLDOWN[i];
            boolean enoughMana = s.getMana() >= a.manaCost;

            g.fill(sx - 1, ay - 1, sx + slot + 1, ay + slot + 1, 0xFF000000);
            int bg = !unlocked ? 0xFF303030 : enoughMana ? 0xFF3A2A66 : 0xFF2A2A44;
            g.fill(sx, ay, sx + slot, ay + slot, bg);

            String icon = iconFor(a);
            int iconColor = unlocked ? 0xFFFFFFFF : 0xFF777777;
            g.drawString(font, icon, sx + slot / 2 - font.width(icon) / 2, ay + 6, iconColor);

            if (unlocked && cd > 0) {
                float frac = cd / (float) a.cooldown;
                int h = (int) (slot * Math.min(1f, frac));
                g.fill(sx, ay + slot - h, sx + slot, ay + slot, 0xB0000000);
                String sec = String.valueOf((cd + 19) / 20);
                g.drawString(font, sec, sx + slot / 2 - font.width(sec) / 2, ay + 6, 0xFFFF6060);
            }
            String key = ModKeys.ABILITIES[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) key = key.substring(0, 1);
            g.drawString(font, key, sx + 1, ay - 9, 0xFFBBBBBB);
            if (!unlocked) {
                String need = String.valueOf(a.unlockLevel);
                g.drawString(font, need, sx + slot - font.width(need) - 1, ay + slot - 8, 0xFFFF9800, false);
            }
        }
    }

    public static String iconFor(Ability a) {
        return switch (a) {
            case DASH -> "»";
            case MANA_BOLT -> "✦";
            case HEAL -> "✚";
            case SLAM -> "✸";
            case BERSERK -> "⚔";
        };
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int bg, int fg) {
        frac = Mth.clamp(frac, 0f, 1f);
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + (int) (w * frac), y + h, fg);
    }
}
