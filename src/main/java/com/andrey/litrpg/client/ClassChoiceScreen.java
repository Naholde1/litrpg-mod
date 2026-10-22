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

/** Выбор класса на 10 уровне: пять классов, подобранных Системой; справа — подробности выбранного. */
public class ClassChoiceScreen extends SystemScreen {
    private static final int TILE_W = 132;
    private static final int TILE_H = 30;
    private PlayerClass selected;
    private boolean confirmHover;

    public ClassChoiceScreen() {
        super(Component.literal("Выбор класса"), 480, 262);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        UiKit.window(g, left, top, W, H, "◆ СИСТЕМА: ВЫБОР КЛАССА ◆", font);
        PlayerStats s = ClientData.STATS;
        List<PlayerClass> offers = s.getOffers();
        if (selected == null && !offers.isEmpty()) selected = offers.get(0);

        StringBuilder sb = new StringBuilder("Твой стиль: ");
        List<Map.Entry<Style, Integer>> top3 = ClassOffers.topStyles(s, 3);
        if (top3.isEmpty()) sb.append("пока не определён");
        for (int i = 0; i < top3.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(Component.translatable(top3.get(i).getKey().nameKey()).getString()).append(' ').append(top3.get(i).getValue()).append('%');
        }
        g.drawString(font, sb.toString(), left + 12, top + 22, UiKit.MUTED, false);
        if (s.getRole() != null) {
            String r = "Роль: " + s.getRole().title;
            g.drawString(font, r, left + W - 12 - font.width(r), top + 22, 0xFF000000 | s.getRole().color, false);
        }

        int x0 = left + 10, y0 = top + 36;
        for (int i = 0; i < offers.size(); i++) {
            PlayerClass c = offers.get(i);
            int y = y0 + i * (TILE_H + 4);
            boolean hov = UiKit.inside(mx, my, x0, y, TILE_W, TILE_H);
            boolean sel = c == selected;
            g.fill(x0, y, x0 + TILE_W, y + TILE_H, sel ? 0xF0183A52 : hov ? 0xE0122636 : 0xC00C1824);
            UiKit.frame(g, x0, y, TILE_W, TILE_H, sel || hov ? 0xFF000000 | c.color : UiKit.FRAME_DIM);
            g.fill(x0 + 1, y + 1, x0 + 3, y + TILE_H - 1, 0xFF000000 | c.color);
            g.drawString(font, Component.translatable(c.nameKey()).withStyle(ChatFormatting.BOLD), x0 + 7, y + 5, 0xFF000000 | c.color, false);
            String sub = c.isHidden() ? "★ скрытый класс" : c.role.title;
            g.drawString(font, sub, x0 + 7, y + 17, c.isHidden() ? 0xFFE0B0FF : UiKit.MUTED, false);
        }

        if (selected != null) details(g, s, selected, x0 + TILE_W + 10, y0, mx, my);

        int bw = 170, bh = 20;
        int bx = left + 10, by = top + H - 30;
        boolean enabled = selected != null;
        confirmHover = enabled && UiKit.inside(mx, my, bx, by, bw, bh);
        g.fill(bx, by, bx + bw, by + bh, !enabled ? 0xFF2A2A2A : confirmHover ? 0xFF1F6F4A : 0xFF16503A);
        UiKit.frame(g, bx, by, bw, bh, !enabled ? 0xFF444444 : confirmHover ? 0xFF9CFFB0 : 0xFF4FC38A);
        String ok = enabled ? "ВЫБРАТЬ: " + Component.translatable(selected.nameKey()).getString() : "Выбери класс";
        if (font.width(ok) > bw - 8) ok = "ВЫБРАТЬ";
        g.drawString(font, ok, bx + bw / 2 - font.width(ok) / 2, by + 6, enabled ? 0xFFFFFFFF : 0xFF888888, false);
        g.drawString(font, "Esc — решить позже (K)", left + W - 12 - font.width("Esc — решить позже (K)"), by + 6, 0xFF7A8A96, false);
    }

    private void details(GuiGraphics g, PlayerStats s, PlayerClass c, int x, int y, int mx, int my) {
        int w = left + W - 10 - x;
        g.fill(x - 5, y, x - 4, top + H - 36, UiKit.FRAME_DIM);
        y += UiKit.wrapped(g, font, Component.translatable(c.descKey()), x, y, w, UiKit.TEXT) + 3;
        String growth = "+1 " + Component.translatable(c.primary.translationKey()).getString() + " за уровень, +1 "
                + Component.translatable(c.secondary.translationKey()).getString() + " за 2 уровня";
        g.drawString(font, growth, x, y, 0xFF9CFF9C, false);
        y += 11;
        Component trait = Component.literal("✦ ").append(Component.translatable(c.traitKey())).append(": ")
                .append(Component.translatable(c.traitDescKey()));
        y += UiKit.wrapped(g, font, trait, x, y, w, 0xFFB8E0FF) + 3;
        g.drawString(font, "Почему: " + String.join(", ", ClassOffers.reasons(s, c)), x, y, 0xFFFFD54F, false);
        y += 13;
        String hovered = null;
        String[] list = c.isInvoker() ? combos(c) : c.skills;
        int colW = w / 2;
        for (int i = 0; i < list.length; i++) {
            Ability a = Ability.byKey(list[i]);
            if (a == null) continue;
            int cx = x + (i % 2) * colW;
            int cy = y + (i / 2) * 11;
            String line = a.icon + " " + Component.translatable(a.nameKey()).getString();
            String lv = String.valueOf(c.unlockOf(a.key) == 999 ? 10 : c.unlockOf(a.key));
            while (font.width(line) > colW - 18 && line.length() > 4) line = line.substring(0, line.length() - 2) + "…";
            g.drawString(font, line, cx, cy, 0xFFE0D0FF, false);
            g.drawString(font, lv, cx + colW - 6 - font.width(lv), cy, 0xFF7A8A96, false);
            if (UiKit.inside(mx, my, cx, cy - 1, colW - 4, 10)) {
                hovered = Component.translatable(a.nameKey()).getString() + " — " + Component.translatable(a.descKey()).getString()
                        + "\n" + (a.hpCost() > 0 ? Math.round(a.hpCost() * 100) + "% здоровья" : a.manaCost() + " маны")
                        + ", перезарядка " + String.format("%.0f", a.cooldown() / 20.0) + " с";
            }
        }
        if (c.isInvoker()) {
            int cy = y + ((list.length + 1) / 2) * 11 + 2;
            UiKit.wrapped(g, font, Component.literal("Заклинатель сфер: три сферы сплетаются в 10 заклинаний."), x, cy, w, UiKit.MUTED);
        }
        if (hovered != null) g.renderTooltip(font, font.split(Component.literal(hovered), 220), mx, my);
    }

    private static String[] combos(PlayerClass c) {
        var cs = c.invoke.combos();
        String[] out = new String[cs.size()];
        for (int i = 0; i < out.length; i++) out[i] = cs.get(i).skill();
        return out;
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        List<PlayerClass> offers = ClientData.STATS.getOffers();
        int x0 = left + 10, y0 = top + 36;
        for (int i = 0; i < offers.size(); i++) {
            if (UiKit.inside(mx, my, x0, y0 + i * (TILE_H + 4), TILE_W, TILE_H)) {
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
