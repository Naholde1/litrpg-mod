package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.network.ChooseClassPacket;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.stats.ClassOffers;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.Map;

/** Выбор класса на 10 уровне: три карточки, подобранные по стилю игры. */
public class ClassChoiceScreen extends SystemScreen {
    private static final int CARD_W = 146;
    private static final int CARD_H = 176;
    private PlayerClass selected;
    private boolean confirmHover;

    public ClassChoiceScreen() {
        super(Component.literal("Выбор класса"), 470, 254);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        UiKit.window(g, left, top, W, H, "◆ СИСТЕМА: ВЫБОР КЛАССА ◆", font);
        PlayerStats s = ClientData.STATS;
        List<PlayerClass> offers = s.getOffers();

        // стиль игрока
        StringBuilder sb = new StringBuilder("Твой стиль: ");
        List<Map.Entry<Style, Integer>> top3 = ClassOffers.topStyles(s, 3);
        if (top3.isEmpty()) sb.append("пока не определён");
        for (int i = 0; i < top3.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(Component.translatable(top3.get(i).getKey().nameKey()).getString()).append(' ').append(top3.get(i).getValue()).append('%');
        }
        g.drawString(font, sb.toString(), left + 12, top + 24, UiKit.MUTED, false);

        String hovered = null;
        for (int i = 0; i < offers.size(); i++) {
            PlayerClass c = offers.get(i);
            int cx = left + 10 + i * (CARD_W + 6);
            int cy = top + 38;
            boolean hov = UiKit.inside(mx, my, cx, cy, CARD_W, CARD_H);
            boolean sel = c == selected;
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, sel ? 0xF0183A52 : hov ? 0xE0122636 : 0xC00C1824);
            UiKit.frame(g, cx, cy, CARD_W, CARD_H, sel || hov ? 0xFF000000 | c.color : UiKit.FRAME_DIM);
            g.fill(cx + 1, cy + 1, cx + CARD_W - 1, cy + 3, 0xFF000000 | c.color);

            int y = cy + 8;
            String name = Component.translatable(c.nameKey()).getString();
            g.drawString(font, Component.literal(name).withStyle(ChatFormatting.BOLD), cx + CARD_W / 2 - font.width(name) / 2 - 1, y,
                    0xFF000000 | c.color, false);
            y += 11;
            if (c.isHidden()) {
                String h = "★ СКРЫТЫЙ КЛАСС ★";
                g.drawString(font, h, cx + CARD_W / 2 - font.width(h) / 2, y, 0xFFE0B0FF, false);
                y += 11;
            }
            y += UiKit.wrapped(g, font, Component.translatable(c.descKey()), cx + 6, y, CARD_W - 12, UiKit.TEXT) + 4;
            String growth = "+" + Component.translatable(c.primary.translationKey()).getString() + " за уровень";
            g.drawString(font, growth, cx + 6, y, 0xFF9CFF9C, false);
            y += 13;
            for (int slot = 0; slot < Ability.SLOTS; slot++) {
                Ability a = Ability.forSlot(c, slot);
                if (a == null) continue;
                String line = a.icon + " " + Component.translatable(a.nameKey()).getString();
                String lv = String.valueOf(Ability.UNLOCK[slot]);
                g.drawString(font, line, cx + 6, y, 0xFFE0D0FF, false);
                g.drawString(font, lv, cx + CARD_W - 6 - font.width(lv), y, 0xFF7A8A96, false);
                if (UiKit.inside(mx, my, cx + 4, y - 1, CARD_W - 8, 10)) {
                    hovered = Component.translatable(a.nameKey()).getString() + " — " + Component.translatable(a.descKey()).getString()
                            + "\n" + a.manaCost() + " маны, перезарядка " + String.format("%.0f", a.cooldown() / 20.0) + " с";
                }
                y += 11;
            }
            y += 4;
            StringBuilder st = new StringBuilder();
            c.styles.entrySet().stream().sorted(Map.Entry.<Style, Integer>comparingByValue().reversed()).limit(2).forEach(e -> {
                if (st.length() > 0) st.append(", ");
                st.append(Component.translatable(e.getKey().nameKey()).getString());
            });
            UiKit.wrapped(g, font, Component.literal("Стиль: " + st), cx + 6, y, CARD_W - 12, 0xFF7A8A96);
        }

        int bw = 160, bh = 20;
        int bx = left + W / 2 - bw / 2, by = top + H - 30;
        boolean enabled = selected != null;
        confirmHover = enabled && UiKit.inside(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, !enabled ? 0xFF2A2A2A : confirmHover ? 0xFF1F6F4A : 0xFF16503A);
        UiKit.frame(g, bx, by, bw, bh, !enabled ? 0xFF444444 : confirmHover ? 0xFF9CFFB0 : 0xFF4FC38A);
        String ok = enabled ? "ВЫБРАТЬ: " + Component.translatable(selected.nameKey()).getString() : "Выбери класс";
        g.drawString(font, ok, bx + bw / 2 - font.width(ok) / 2, by + 6, enabled ? 0xFFFFFFFF : 0xFF888888, false);
        g.drawString(font, "Esc — решить позже (K)", left + W - 12 - font.width("Esc — решить позже (K)"), by + 6, 0xFF7A8A96, false);

        if (hovered != null) g.renderTooltip(font, font.split(Component.literal(hovered), 200), mx, my);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        List<PlayerClass> offers = ClientData.STATS.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            int cx = left + 10 + i * (CARD_W + 6);
            if (UiKit.inside(mx, my, cx, top + 38, CARD_W, CARD_H)) {
                selected = offers.get(i);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        if (confirmHover && selected != null) {
            ModNetwork.CHANNEL.sendToServer(new ChooseClassPacket(selected));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
            onClose();
            return true;
        }
        return false;
    }
}
