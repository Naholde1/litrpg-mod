package com.andrey.litrpg.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

/** Общая отрисовка «интерфейса Системы»: голубые рамки, тёмное стекло. */
public final class UiKit {
    public static final int FRAME = 0xFF4FC3F7;
    public static final int FRAME_DIM = 0xFF1E5A7A;
    public static final int GLASS = 0xE60A1220;
    public static final int HEADER = 0xF0102A3E;
    public static final int TEXT = 0xFFE6F4FF;
    public static final int MUTED = 0xFF8AA4B8;
    public static final int GOLD = 0xFFFFD54F;

    private UiKit() {}

    public static void window(GuiGraphics g, int x, int y, int w, int h, String title, Font font) {
        // тень
        g.fill(x + 3, y + 3, x + w + 3, y + h + 3, 0x80000000);
        g.fill(x, y, x + w, y + h, GLASS);
        frame(g, x, y, w, h, FRAME);
        frame(g, x + 2, y + 2, w - 4, h - 4, FRAME_DIM);
        // уголки
        int c = 6;
        g.fill(x - 1, y - 1, x + c, y + 1, FRAME);
        g.fill(x - 1, y - 1, x + 1, y + c, FRAME);
        g.fill(x + w - c, y - 1, x + w + 1, y + 1, FRAME);
        g.fill(x + w - 1, y - 1, x + w + 1, y + c, FRAME);
        g.fill(x - 1, y + h - 1, x + c, y + h + 1, FRAME);
        g.fill(x - 1, y + h - c, x + 1, y + h + 1, FRAME);
        g.fill(x + w - c, y + h - 1, x + w + 1, y + h + 1, FRAME);
        g.fill(x + w - 1, y + h - c, x + w + 1, y + h + 1, FRAME);
        if (title != null) {
            g.fill(x + 3, y + 3, x + w - 3, y + 17, HEADER);
            g.fill(x + 3, y + 17, x + w - 3, y + 18, FRAME_DIM);
            g.drawString(font, title, x + w / 2 - font.width(title) / 2, y + 6, 0xFF8FE3FF);
        }
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int bg, int fg) {
        frac = Mth.clamp(frac, 0f, 1f);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, bg);
        int fw = (int) (w * frac);
        g.fill(x, y, x + fw, y + h, fg);
        // блик сверху
        if (fw > 0 && h > 2) g.fill(x, y, x + fw, y + 1, 0x50FFFFFF);
    }

    /** Текст с переносом строк. @return высота в пикселях */
    public static int wrapped(GuiGraphics g, Font font, Component text, int x, int y, int width, int color) {
        List<FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), x, y + i * 10, color, false);
        }
        return lines.size() * 10;
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Кнопка-плашка без ванильной текстуры. */
    public static void tile(GuiGraphics g, Font font, int x, int y, int w, int h, String label, boolean selected,
                            boolean hovered, int accent) {
        int bg = selected ? 0xF0183A52 : hovered ? 0xE0122636 : 0xC00C1824;
        g.fill(x, y, x + w, y + h, bg);
        frame(g, x, y, w, h, selected ? (0xFF000000 | accent) : hovered ? FRAME : FRAME_DIM);
        if (selected) g.fill(x + 1, y + 1, x + 3, y + h - 1, 0xFF000000 | accent);
        g.drawString(font, label, x + 7, y + (h - 8) / 2, selected ? 0xFFFFFFFF : TEXT, false);
    }
}
