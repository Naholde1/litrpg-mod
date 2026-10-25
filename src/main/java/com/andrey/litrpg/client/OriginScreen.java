package com.andrey.litrpg.client;

import com.andrey.litrpg.network.ChooseOriginPacket;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.Role;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Начало пути: выбор роли (танк, боец, поддержка). Класс выбирается позже — на 10 уровне
 * Система предложит пять классов выбранной роли под стиль игры в первом этапе.
 * Это же окно открывается после смены класса у торговца.
 */
public class OriginScreen extends SystemScreen {
    private static final int TILE_W = 110;
    private static final int TILE_H = 22;

    private Role role = Role.DAMAGE;
    private boolean confirmHover;

    public OriginScreen() {
        super(Component.literal("Начало пути"), 400, 236);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        UiKit.window(g, left, top, W, H, "◆ СИСТЕМА: КЕМ ТЫ БУДЕШЬ В ГРУППЕ? ◆", font);
        int x0 = left + 12;
        int y0 = top + 28;
        int dx = x0 + TILE_W + 14;
        int dw = left + W - 12 - dx;
        g.fill(dx - 7, y0, dx - 6, top + H - 36, UiKit.FRAME_DIM);
        roles(g, mx, my, x0, y0, dx, dw);

        int bw = 140, bh = 20;
        int bx = left + W / 2 - bw / 2, by = top + H - 28;
        confirmHover = UiKit.inside(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, confirmHover ? 0xFF1F6F4A : 0xFF16503A);
        UiKit.frame(g, bx, by, bw, bh, confirmHover ? 0xFF9CFFB0 : 0xFF4FC38A);
        String ok = "ПОДТВЕРДИТЬ";
        g.drawString(font, ok, bx + bw / 2 - font.width(ok) / 2, by + 6, 0xFFFFFFFF, false);
    }

    private void roles(GuiGraphics g, int mx, int my, int x0, int y0, int dx, int dw) {
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            int y = y0 + i * (TILE_H + 8);
            boolean hov = UiKit.inside(mx, my, x0, y, TILE_W, TILE_H);
            UiKit.tile(g, font, x0, y, TILE_W, TILE_H, roles[i].title, roles[i] == role, hov, roles[i].color);
        }
        int y = y0;
        g.drawString(font, Component.literal(role.title).withStyle(ChatFormatting.BOLD), dx, y, 0xFF000000 | role.color, false);
        y += 13;
        y += UiKit.wrapped(g, font, Component.literal(role.desc), dx, y, dw, UiKit.TEXT) + 4;
        y += UiKit.wrapped(g, font, Component.literal("Бонус роли: " + role.perk), dx, y, dw, 0xFF9CFF9C) + 6;
        int n = 0;
        StringBuilder ex = new StringBuilder();
        for (PlayerClass c : PlayerClass.all()) {
            if (c.role != role || c.isHidden()) continue;
            if (n < 6) ex.append(n > 0 ? ", " : "").append(Component.translatable(c.nameKey()).getString());
            n++;
        }
        y += UiKit.wrapped(g, font, Component.literal("Классов этой роли: " + n + ". Например: " + ex + "…"), dx, y, dw, UiKit.MUTED) + 6;
        UiKit.wrapped(g, font, Component.literal("Группе из 3–5 нужен хотя бы один танк и одна поддержка. На 10 уровне "
                + "Система предложит пять классов этой роли — по тому, как вы сражались в Склепе."), dx, y, dw, 0xFF7A8A96);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        int x0 = left + 12;
        int y0 = top + 28;
        Role[] roles = Role.values();
        for (int i = 0; i < roles.length; i++) {
            if (UiKit.inside(mx, my, x0, y0 + i * (TILE_H + 8), TILE_W, TILE_H)) {
                role = roles[i];
                click();
                return true;
            }
        }
        if (confirmHover) {
            click();
            ModNetwork.CHANNEL.sendToServer(new ChooseOriginPacket(role));
            onClose();
            return true;
        }
        return false;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
