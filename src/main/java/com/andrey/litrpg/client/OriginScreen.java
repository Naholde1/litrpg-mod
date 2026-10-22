package com.andrey.litrpg.client;

import com.andrey.litrpg.network.ChooseOriginPacket;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.Role;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Начало пути: выбор расы, затем роли (танк, боец, поддержка). Класс выбирается позже —
 * на 10 уровне Система предложит пять классов выбранной роли под стиль игры.
 * У персонажей, созданных до ролей, окно открывается сразу на выборе роли.
 */
public class OriginScreen extends SystemScreen {
    private static final int TILE_W = 110;
    private static final int TILE_H = 22;

    private Race race = Race.HUMAN;
    private Role role = Role.DAMAGE;
    /** 0 — раса, 1 — роль. */
    private int step;
    private boolean confirmHover;

    public OriginScreen() {
        super(Component.literal("Начало пути"), 400, 236);
        step = ClientData.STATS.hasOrigin() ? 1 : 0;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        UiKit.window(g, left, top, W, H, step == 0 ? "◆ СИСТЕМА: ВЫБОР РАСЫ ◆" : "◆ СИСТЕМА: КЕМ ТЫ ХОЧЕШЬ БЫТЬ? ◆", font);
        int x0 = left + 12;
        int y0 = top + 28;
        int dx = x0 + TILE_W + 14;
        int dw = left + W - 12 - dx;
        g.fill(dx - 7, y0, dx - 6, top + H - 36, UiKit.FRAME_DIM);
        if (step == 0) races(g, mx, my, x0, y0, dx, dw);
        else roles(g, mx, my, x0, y0, dx, dw);

        int bw = 140, bh = 20;
        int bx = left + W / 2 - bw / 2, by = top + H - 28;
        confirmHover = UiKit.inside(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, confirmHover ? 0xFF1F6F4A : 0xFF16503A);
        UiKit.frame(g, bx, by, bw, bh, confirmHover ? 0xFF9CFFB0 : 0xFF4FC38A);
        String ok = step == 0 ? "ДАЛЕЕ" : "ПОДТВЕРДИТЬ";
        g.drawString(font, ok, bx + bw / 2 - font.width(ok) / 2, by + 6, 0xFFFFFFFF, false);
        String st = (step + 1) + " / 2";
        g.drawString(font, st, left + W - 14 - font.width(st), by + 6, UiKit.MUTED, false);
    }

    private void races(GuiGraphics g, int mx, int my, int x0, int y0, int dx, int dw) {
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            int y = y0 + i * (TILE_H + 4);
            boolean hov = UiKit.inside(mx, my, x0, y, TILE_W, TILE_H);
            UiKit.tile(g, font, x0, y, TILE_W, TILE_H, Component.translatable(races[i].nameKey()).getString(),
                    races[i] == race, hov, races[i].color);
        }
        int y = y0;
        g.drawString(font, Component.translatable(race.nameKey()).withStyle(ChatFormatting.BOLD), dx, y, 0xFF000000 | race.color, false);
        y += 13;
        y += UiKit.wrapped(g, font, Component.translatable(race.descKey()), dx, y, dw, UiKit.MUTED) + 4;
        for (Stat s : Stat.values()) {
            int b = race.bonus(s);
            if (b == 0) continue;
            String line = (b > 0 ? "+" : "") + b + " " + Component.translatable(s.translationKey()).getString();
            g.drawString(font, line, dx, y, b > 0 ? 0xFF9CFF9C : 0xFFFF8A8A, false);
            y += 10;
        }
        y += 4;
        UiKit.wrapped(g, font, Component.literal("✦ ").append(Component.translatable(race.passiveKey())), dx, y, dw, 0xFFB8E0FF);
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
            if (n < 8) ex.append(n > 0 ? ", " : "").append(Component.translatable(c.nameKey()).getString());
            n++;
        }
        y += UiKit.wrapped(g, font, Component.literal("Классов этой роли: " + n + ". Например: " + ex + "…"), dx, y, dw, UiKit.MUTED) + 6;
        UiKit.wrapped(g, font, Component.literal("На 10 уровне Система предложит пять классов этой роли — по стилю игры, вложенным очкам, расе, "
                + "профессиям и репутации. Скрытые классы могут быть любой роли."), dx, y, dw, 0xFF7A8A96);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        int x0 = left + 12;
        int y0 = top + 28;
        if (step == 0) {
            Race[] races = Race.values();
            for (int i = 0; i < races.length; i++) {
                if (UiKit.inside(mx, my, x0, y0 + i * (TILE_H + 4), TILE_W, TILE_H)) {
                    race = races[i];
                    click();
                    return true;
                }
            }
        } else {
            Role[] roles = Role.values();
            for (int i = 0; i < roles.length; i++) {
                if (UiKit.inside(mx, my, x0, y0 + i * (TILE_H + 8), TILE_W, TILE_H)) {
                    role = roles[i];
                    click();
                    return true;
                }
            }
        }
        if (confirmHover) {
            click();
            if (step == 0) {
                step = 1;
                return true;
            }
            ModNetwork.CHANNEL.sendToServer(new ChooseOriginPacket(ClientData.STATS.hasOrigin() ? null : race, role));
            onClose();
            return true;
        }
        return false;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
