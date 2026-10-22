package com.andrey.litrpg.client;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.OpenServicePacket;
import com.andrey.litrpg.network.ServicePacket;
import com.andrey.litrpg.npc.Wallet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** Окно услуг жителя: список строк с ценой и кнопкой (круг телепорта, комнаты постоялого двора). */
public class ServiceScreen extends SystemScreen {
    private static final int ROW = 26;
    private final OpenServicePacket data;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;

    private record Hit(int x, int y, int w, int h, String key) {}

    public ServiceScreen(OpenServicePacket data) {
        super(Component.literal(data.title), 340, 250);
        this.data = data;
    }

    private int listTop() {
        return top + 24 + 10 * Math.max(1, font.split(Component.literal(data.intro), W - 20).size()) + 18;
    }

    private int visibleRows() {
        return (top + H - 10 - listTop()) / ROW;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        UiKit.window(g, left, top, W, H, "◆ " + data.title.toUpperCase() + " ◆", font);
        int x = left + 10, y = top + 24;
        y += UiKit.wrapped(g, font, Component.literal(data.intro), x, y, W - 20, UiKit.TEXT);
        String purse = "В кошельке: " + GearData.coins(minecraft.player == null ? 0 : Wallet.total(minecraft.player));
        g.drawString(font, purse, left + W - 10 - font.width(purse), y + 3, UiKit.GOLD, false);
        y = listTop();
        g.fill(x, y - 4, left + W - 10, y - 3, UiKit.FRAME_DIM);
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, data.entries.size() - rows));
        String tip = null;
        for (int i = scroll; i < Math.min(data.entries.size(), scroll + rows); i++) {
            OpenServicePacket.Entry e = data.entries.get(i);
            boolean hov = UiKit.inside(mx, my, x, y, W - 20, ROW - 2);
            g.fill(x, y, left + W - 10, y + ROW - 2, hov ? 0x70183A52 : 0x400C1824);
            g.fill(x, y, x + 2, y + ROW - 2, 0xFF000000 | e.accent());
            int bw = e.button().isEmpty() ? 0 : Math.max(48, font.width(e.button()) + 12);
            int textW = W - 34 - bw - (e.price().isEmpty() ? 0 : font.width(e.price()) + 8);
            String label = e.label();
            if (font.width(label) > textW) label = font.plainSubstrByWidth(label, textW - 6) + "…";
            g.drawString(font, label, x + 7, y + 3, e.enabled() ? 0xFFFFFFFF : UiKit.MUTED, false);
            String detail = e.detail();
            if (font.width(detail) > textW) {
                if (hov) tip = detail;
                detail = font.plainSubstrByWidth(detail, textW - 6) + "…";
            }
            g.drawString(font, detail, x + 7, y + 14, UiKit.MUTED, false);
            if (!e.price().isEmpty()) {
                g.drawString(font, e.price(), left + W - 16 - bw - font.width(e.price()), y + 8, e.enabled() ? UiKit.GOLD : 0xFF7A6A40, false);
            }
            if (bw > 0) {
                int bx = left + W - 14 - bw, by = y + 4;
                boolean bh = e.enabled() && UiKit.inside(mx, my, bx, by, bw, 16);
                g.fill(bx, by, bx + bw, by + 16, !e.enabled() ? 0x80101820 : bh ? 0xF0204A30 : 0xE0122636);
                UiKit.frame(g, bx, by, bw, 16, e.enabled() ? 0xFF000000 | e.accent() : UiKit.FRAME_DIM);
                g.drawString(font, e.button(), bx + bw / 2 - font.width(e.button()) / 2, by + 4, e.enabled() ? 0xFFFFFFFF : UiKit.MUTED, false);
                if (e.enabled()) hits.add(new Hit(bx, by, bw, 16, e.key()));
            }
            y += ROW;
        }
        if (data.entries.size() > rows) {
            // полоса прокрутки
            int sx = left + W - 7, sy = listTop(), sh = rows * ROW - 2;
            g.fill(sx, sy, sx + 2, sy + sh, 0x60FFFFFF);
            int th = Math.max(10, sh * rows / data.entries.size());
            int ty = sy + (sh - th) * scroll / Math.max(1, data.entries.size() - rows);
            g.fill(sx, ty, sx + 2, ty + th, UiKit.FRAME);
        }
        if (tip != null) g.renderTooltip(font, font.split(Component.literal(tip), 240), mx, my);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        for (Hit h : hits) {
            if (UiKit.inside(mx, my, h.x, h.y, h.w, h.h)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                ModNetwork.CHANNEL.sendToServer(new ServicePacket(data.entityId, h.key));
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean scrollContent(double mx, double my, double delta) {
        scroll -= (int) Math.signum(delta);
        return true;
    }
}
