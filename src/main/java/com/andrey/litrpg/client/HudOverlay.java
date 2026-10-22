package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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

        // --- мини-карта (если не стоит Xaero's Minimap) ---
        if (Minimap.enabled) {
            Minimap.render(g, x, y, MAP_SIZE);
            String coords = Mth.floor(mc.player.getX()) + " " + Mth.floor(mc.player.getY()) + " " + Mth.floor(mc.player.getZ());
            g.drawString(font, coords, x + MAP_SIZE / 2 - font.width(coords) / 2, y + MAP_SIZE + 4, 0xFFE0E0E0);
            y += MAP_SIZE + 16;
        }

        // --- панель уровня ---
        int panelW = 140;
        g.fill(x - 3, y - 3, x + panelW + 3, y + 33, 0xA0050A12);
        UiKit.frame(g, x - 3, y - 3, panelW + 6, 36, UiKit.FRAME_DIM);
        String lvl = "Ур. " + s.getLevel();
        g.drawString(font, lvl, x, y, UiKit.GOLD);
        String cls = s.getPlayerClass() != null ? Component.translatable(s.getPlayerClass().nameKey()).getString()
                : s.canChooseClass() ? "Выбор класса!" : "Новичок";
        g.drawString(font, cls, x + font.width(lvl) + 6, y, s.getPlayerClass() != null ? 0xFF000000 | s.getPlayerClass().color
                : s.canChooseClass() ? 0xFFFFD54F : 0xFFB0BEC5);
        if (s.getFreePoints() > 0) {
            String pts = "+" + s.getFreePoints();
            g.drawString(font, pts, x + panelW - font.width(pts), y, 0xFF7CFC00);
        }
        y += 11;
        float xpFrac = s.getLevel() >= PlayerStats.MAX_LEVEL ? 1f : s.getXp() / (float) s.xpForNext();
        UiKit.bar(g, x, y, panelW, 4, xpFrac, 0xFF1F3A1F, 0xFF66DD44);
        y += 7;
        float manaFrac = (float) (s.getMana() / Math.max(1, s.getMaxMana()));
        UiKit.bar(g, x, y, panelW, 5, manaFrac, 0xFF1A2340, 0xFF3D8BFF);
        y += 8;
        g.drawString(font, (int) s.getMana() + " / " + s.getMaxMana() + " MP", x, y, 0xFF9EC5FF, false);

        abilities(g, font, s, height);
        target(g, font, mc, width);
        party(g, font, y + 14);
    }

    /** Панель навыков класса (снизу слева). */
    private static void abilities(GuiGraphics g, Font font, PlayerStats s, int height) {
        int slot = 22;
        int slots = s.getPlayerClass() == null ? Ability.NOVICE.length : Ability.SLOTS;
        int ax = 6;
        int ay = height - slot - 6;
        for (int i = 0; i < slots; i++) {
            Ability a = Ability.forSlot(s.getPlayerClass(), i);
            if (a == null) continue;
            int sx = ax + i * (slot + 4);
            int need = Ability.unlockLevel(s.getPlayerClass(), i);
            boolean unlocked = s.getLevel() >= need;
            int accent = s.getPlayerClass() != null ? s.getPlayerClass().color : 0x4FC3F7;
            int cd = ClientData.COOLDOWN[a.ordinal()];
            boolean enoughMana = s.getMana() >= a.manaCost();

            g.fill(sx - 1, ay - 1, sx + slot + 1, ay + slot + 1, 0xFF000000);
            int bg = !unlocked ? 0xFF303030 : enoughMana ? 0xFF2A1E4A : 0xFF1C1C34;
            g.fill(sx, ay, sx + slot, ay + slot, bg);
            UiKit.frame(g, sx, ay, slot, slot, unlocked && cd <= 0 && enoughMana ? 0xFF000000 | accent : 0xFF444444);

            int iconColor = !unlocked ? 0xFF666666 : enoughMana ? 0xFFFFFFFF : 0xFF7A8AFF;
            g.drawString(font, a.icon, sx + slot / 2 - font.width(a.icon) / 2, ay + 7, iconColor);

            if (unlocked && cd > 0) {
                float frac = cd / (float) a.cooldown();
                int h = (int) (slot * Math.min(1f, frac));
                g.fill(sx, ay + slot - h, sx + slot, ay + slot, 0xB0000000);
                String sec = String.valueOf((cd + 19) / 20);
                g.drawString(font, sec, sx + slot / 2 - font.width(sec) / 2, ay + 7, 0xFFFF6060);
            }
            String key = ModKeys.ABILITIES[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) key = key.substring(0, 1);
            g.drawString(font, key, sx + 1, ay - 9, 0xFFBBBBBB);
            if (!unlocked) {
                String lv = String.valueOf(need);
                g.drawString(font, lv, sx + slot - font.width(lv) - 1, ay + slot - 8, 0xFFFF9800, false);
            }
        }
    }

    /** Панель группы: имена, уровни, здоровье и мана товарищей. */
    private static void party(GuiGraphics g, Font font, int y) {
        var list = ClientData.party;
        if (list.isEmpty()) return;
        int x = 6, w = 110;
        g.drawString(font, "ГРУППА", x, y, UiKit.FRAME, false);
        y += 10;
        for (var m : list) {
            g.fill(x - 2, y - 2, x + w + 2, y + 20, 0x90050A12);
            g.drawString(font, m.name(), x, y, 0xFF000000 | m.color(), false);
            String lv = String.valueOf(m.level());
            g.drawString(font, lv, x + w - font.width(lv), y, UiKit.GOLD, false);
            UiKit.bar(g, x, y + 10, w, 4, m.hp() / Math.max(1f, m.maxHp()), 0xFF2A0A0A, 0xFFD03A3A);
            UiKit.bar(g, x, y + 15, w, 2, m.mana(), 0xFF1A2340, 0xFF3D8BFF);
            y += 25;
        }
    }

    /** Рамка цели: имя, уровень и здоровье моба под прицелом. */
    private static void target(GuiGraphics g, Font font, Minecraft mc, int width) {
        Entity e = mc.crosshairPickEntity;
        if (!(e instanceof LivingEntity le) || e instanceof Player || !le.isAlive() || le.isInvisible()) return;
        if (e instanceof com.andrey.litrpg.entity.Animated a && a.awakeAnim(1f) < 0.5f) return;   // мимик притворяется сундуком
        Component name = le.getDisplayName();
        int w = Math.max(140, font.width(name) + 16);
        int x = width / 2 - w / 2;
        int y = 4;
        g.fill(x, y, x + w, y + 24, 0xB0050A12);
        UiKit.frame(g, x, y, w, 24, UiKit.FRAME_DIM);
        g.drawString(font, name, x + w / 2 - font.width(name) / 2, y + 3, 0xFFFFFFFF);
        float frac = le.getHealth() / Math.max(1f, le.getMaxHealth());
        int color = frac > 0.5f ? 0xFFD03A3A : frac > 0.25f ? 0xFFE0822A : 0xFFFF2020;
        UiKit.bar(g, x + 6, y + 14, w - 12, 6, frac, 0xFF2A0A0A, color);
        String hp = (int) Math.ceil(le.getHealth()) + " / " + (int) le.getMaxHealth();
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 14.5f, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, hp, -font.width(hp) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int bg, int fg) {
        UiKit.bar(g, x, y, w, h, frac, bg, fg);
    }
}
