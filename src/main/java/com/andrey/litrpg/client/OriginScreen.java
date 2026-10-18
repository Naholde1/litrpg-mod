package com.andrey.litrpg.client;

import com.andrey.litrpg.network.ChooseOriginPacket;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * Выбор расы при первом входе. Класс выбирается позже — на 10 уровне,
 * когда Система поймёт стиль игры.
 */
public class OriginScreen extends SystemScreen {
    private static final int TILE_W = 110;
    private static final int TILE_H = 22;

    private Race race = Race.HUMAN;
    private boolean confirmHover;

    public OriginScreen() {
        super(Component.literal("Выбор расы"), 380, 230);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        UiKit.window(g, left, top, W, H, "◆ СИСТЕМА: ВЫБОР РАСЫ ◆", font);

        int x0 = left + 12;
        int y0 = top + 28;
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            int y = y0 + i * (TILE_H + 4);
            boolean hov = UiKit.inside(mx, my, x0, y, TILE_W, TILE_H);
            UiKit.tile(g, font, x0, y, TILE_W, TILE_H, Component.translatable(races[i].nameKey()).getString(),
                    races[i] == race, hov, races[i].color);
        }

        int dx = x0 + TILE_W + 14;
        int dw = left + W - 12 - dx;
        g.fill(dx - 7, y0, dx - 6, top + H - 36, UiKit.FRAME_DIM);
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
        y += UiKit.wrapped(g, font, Component.literal("✦ ").append(Component.translatable(race.passiveKey())), dx, y, dw, 0xFFB8E0FF) + 6;
        UiKit.wrapped(g, font, Component.literal("Класс ты получишь на 10 уровне: Система предложит три класса под твой стиль игры."),
                dx, y, dw, 0xFF7A8A96);

        int bw = 140, bh = 20;
        int bx = left + W / 2 - bw / 2, by = top + H - 28;
        confirmHover = UiKit.inside(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, confirmHover ? 0xFF1F6F4A : 0xFF16503A);
        UiKit.frame(g, bx, by, bw, bh, confirmHover ? 0xFF9CFFB0 : 0xFF4FC38A);
        String ok = "ПОДТВЕРДИТЬ";
        g.drawString(font, ok, bx + bw / 2 - font.width(ok) / 2, by + 6, 0xFFFFFFFF, false);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        int x0 = left + 12;
        int y0 = top + 28;
        Race[] races = Race.values();
        for (int i = 0; i < races.length; i++) {
            if (UiKit.inside(mx, my, x0, y0 + i * (TILE_H + 4), TILE_W, TILE_H)) {
                race = races[i];
                click();
                return true;
            }
        }
        if (confirmHover) {
            ModNetwork.CHANNEL.sendToServer(new ChooseOriginPacket(race));
            click();
            onClose();
            return true;
        }
        return false;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }
}
