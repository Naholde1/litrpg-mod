package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.SpendPointPacket;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Окно персонажа: уровень, характеристики, навыки. */
public class CharacterScreen extends Screen {
    private static final int W = 300;
    private static final int H = 210;
    private int left, top;

    public CharacterScreen() {
        super(Component.literal("Персонаж"));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            final int idx = i;
            int by = top + 62 + i * 18;
            addRenderableWidget(Button.builder(Component.literal("+"), b -> spend(idx, hasShiftDown() ? 5 : 1))
                    .bounds(left + 122, by - 3, 16, 14)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Shift — +5")))
                    .build());
        }
    }

    private void spend(int stat, int amount) {
        if (ClientData.STATS.getFreePoints() > 0) {
            ModNetwork.CHANNEL.sendToServer(new SpendPointPacket(stat, amount));
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        PlayerStats s = ClientData.STATS;
        Minecraft mc = Minecraft.getInstance();

        // окно в стиле «интерфейса Системы»
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF7F5FD0);
        g.fill(left, top, left + W, top + H, 0xF0101020);
        g.fill(left, top, left + W, top + 18, 0xFF241A44);
        String title = "◆ СТАТУС ИГРОКА ◆";
        g.drawString(font, title, left + W / 2 - font.width(title) / 2, top + 5, 0xFF8FE3FF);

        String name = mc.player != null ? mc.player.getGameProfile().getName() : "?";
        g.drawString(font, "Имя: " + name, left + 10, top + 24, 0xFFFFFFFF);
        g.drawString(font, "Уровень: " + s.getLevel(), left + 10, top + 35, 0xFFFFD54F);

        int xpNeed = s.xpForNext();
        String xpText = s.getLevel() >= PlayerStats.MAX_LEVEL ? "МАКС." : s.getXp() + " / " + xpNeed;
        g.drawString(font, "Опыт: " + xpText, left + 150, top + 24, 0xFF88EE66);
        HudOverlay.bar(g, left + 150, top + 35, 140, 6,
                s.getLevel() >= PlayerStats.MAX_LEVEL ? 1f : s.getXp() / (float) xpNeed, 0xFF1F3A1F, 0xFF66DD44);

        int pointsColor = s.getFreePoints() > 0 ? 0xFF7CFC00 : 0xFFAAAAAA;
        g.drawString(font, "Свободные очки: " + s.getFreePoints(), left + 10, top + 47, pointsColor);

        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            int by = top + 62 + i * 18;
            Stat st = stats[i];
            g.drawString(font, Component.translatable(st.translationKey()), left + 10, by, 0xFF000000 | st.color);
            String v = String.valueOf(s.getStat(st));
            g.drawString(font, v, left + 112 - font.width(v), by, 0xFFFFFFFF);
        }

        // производные характеристики
        int dy = top + 62;
        int dx = left + 150;
        g.drawString(font, "Здоровье: " + (mc.player != null ? (int) mc.player.getMaxHealth() : 0), dx, dy, 0xFFFF6B6B);
        double atk = mc.player != null ? mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
        g.drawString(font, String.format("Урон: %.1f", atk), dx, dy + 11, 0xFFE05A47);
        double armor = mc.player != null ? mc.player.getAttributeValue(Attributes.ARMOR) : 0;
        g.drawString(font, String.format("Броня: %.1f", armor), dx, dy + 22, 0xFFBBBBBB);
        g.drawString(font, "Мана: " + (int) s.getMana() + " / " + s.getMaxMana(), dx, dy + 33, 0xFF5AA0F0);
        g.drawString(font, String.format("Реген маны: %.1f/с", s.getManaRegenPerSecond()), dx, dy + 44, 0xFF5AA0F0);
        double spd = mc.player != null ? mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1 * 100 : 100;
        g.drawString(font, String.format("Скорость: %.0f%%", spd), dx, dy + 55, 0xFF5BD16A);

        // навыки
        int sy = top + 140;
        g.fill(left + 6, sy - 4, left + W - 6, sy - 3, 0xFF7F5FD0);
        g.drawString(font, "НАВЫКИ", left + 10, sy, 0xFF8FE3FF);
        Ability[] all = Ability.values();
        String hovered = null;
        for (int i = 0; i < all.length; i++) {
            Ability a = all[i];
            int col = i % 2;
            int row = i / 2;
            int ax = left + 10 + col * 145;
            int ay = sy + 12 + row * 11;
            boolean unlocked = s.getLevel() >= a.unlockLevel;
            String key = ModKeys.ABILITIES[i].getTranslatedKeyMessage().getString();
            String line = "[" + key + "] " + Component.translatable(a.nameKey()).getString();
            String tail = unlocked ? " (" + a.manaCost + " MP)" : " — ур. " + a.unlockLevel;
            g.drawString(font, line + tail, ax, ay, unlocked ? 0xFFE0D0FF : 0xFF666666);
            if (mouseX >= ax && mouseX <= ax + 140 && mouseY >= ay - 1 && mouseY <= ay + 9) {
                hovered = Component.translatable(a.descKey()).getString()
                        + "  •  перезарядка " + (a.cooldown / 20) + " с";
            }
        }

        super.render(g, mouseX, mouseY, partialTick);

        if (hovered != null) {
            g.renderTooltip(font, Component.literal(hovered), mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.CHARACTER.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
