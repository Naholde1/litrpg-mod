package com.andrey.litrpg.client;

import com.andrey.litrpg.network.BuyPacket;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.OpenShopPacket;
import com.andrey.litrpg.run.Shop;
import com.andrey.litrpg.run.Wallet;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Лавка торговца привала: снаряжение под ваш класс (каждая вещь — один раз за этап), зелья, камни,
 * свиток и смена класса. Наведите на вещь — подсказка со свойствами. Продать вещь: Shift+ПКМ по торговцу.
 */
public class ShopScreen extends SystemScreen {
    private static final int ROW = 24;
    public final int entityId;
    private OpenShopPacket data;
    private final List<Hit> hits = new ArrayList<>();
    private int scroll;

    private record Hit(int x, int y, int w, int h, int offer) {}

    public ShopScreen(OpenShopPacket data) {
        super(Component.literal("Торговец Привала"), 360, 260);
        this.entityId = data.entityId;
        this.data = data;
    }

    public void refresh(OpenShopPacket d) {
        this.data = d;
    }

    private int listTop() {
        return top + 46;
    }

    private int visibleRows() {
        return (top + H - 22 - listTop()) / ROW;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        UiKit.window(g, left, top, W, H, "◆ ТОРГОВЕЦ ПРИВАЛА · ЭТАП " + data.stage + " ◆", font);
        int x = left + 10, y = top + 24;
        g.drawString(font, "«Снаряжение — под твой класс. Остальное — кому что нужно.»", x, y, UiKit.MUTED, false);
        int money = minecraft.player == null ? data.money : Wallet.total(minecraft.player);
        String purse = "Монеты: " + Wallet.format(money);
        g.drawString(font, purse, left + W - 10 - font.width(purse), y + 11, UiKit.GOLD, false);
        y = listTop();
        g.fill(x, y - 4, left + W - 10, y - 3, UiKit.FRAME_DIM);
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, data.offers.size() - rows));
        ItemStack tip = null;
        String tipText = null;
        for (int i = scroll; i < Math.min(data.offers.size(), scroll + rows); i++) {
            Shop.Offer o = data.offers.get(i);
            boolean sold = o.once() && o.stack().isEmpty() && o.action().isEmpty();
            boolean hov = UiKit.inside(mx, my, x, y, W - 20, ROW - 2);
            g.fill(x, y, left + W - 10, y + ROW - 2, hov ? 0x70183A52 : 0x400C1824);
            int accent = o.action().isEmpty() ? (o.once() ? 0xFFC060FF : 0xFF4FC3F7) : 0xFFFFB040;
            g.fill(x, y, x + 2, y + ROW - 2, accent);
            if (!o.stack().isEmpty()) {
                g.renderItem(o.stack(), x + 6, y + 3);
                g.renderItemDecorations(font, o.stack(), x + 6, y + 3);
                if (hov) tip = o.stack();
            }
            String label = sold ? "— продано —" : !o.action().isEmpty() ? o.label() : o.stack().getHoverName().getString();
            int bw = 52;
            int textW = W - 60 - bw - font.width(Wallet.format(o.price()));
            if (font.width(label) > textW) {
                if (hov && o.stack().isEmpty()) tipText = label;
                label = font.plainSubstrByWidth(label, textW - 6) + "…";
            }
            int color = sold ? UiKit.MUTED : o.stack().isEmpty() ? 0xFFFFD080 : 0xFF000000 | (o.stack().getRarity().color.getColor() == null
                    ? 0xFFFFFF : o.stack().getRarity().color.getColor());
            if (!o.stack().isEmpty() && o.stack().getHoverName().getStyle().getColor() != null) {
                color = 0xFF000000 | o.stack().getHoverName().getStyle().getColor().getValue();
            }
            g.drawString(font, label, x + 28, y + 7, color, false);
            if (!sold) {
                String price = Wallet.format(o.price());
                boolean afford = money >= o.price();
                g.drawString(font, price, left + W - 18 - bw - font.width(price), y + 7, afford ? UiKit.GOLD : 0xFF905040, false);
                int bx = left + W - 14 - bw, by = y + 3;
                boolean bh = afford && UiKit.inside(mx, my, bx, by, bw, 16);
                g.fill(bx, by, bx + bw, by + 16, !afford ? 0x80101820 : bh ? 0xF0204A30 : 0xE0122636);
                UiKit.frame(g, bx, by, bw, 16, afford ? accent : UiKit.FRAME_DIM);
                String btn = o.action().isEmpty() ? "Купить" : o.action().equals("ranks") ? "Забыть" : "Сменить";
                g.drawString(font, btn, bx + bw / 2 - font.width(btn) / 2, by + 4, afford ? 0xFFFFFFFF : UiKit.MUTED, false);
                if (afford) hits.add(new Hit(bx, by, bw, 16, o.id()));
            }
            y += ROW;
        }
        if (data.offers.size() > rows) {
            int sx = left + W - 7, sy = listTop(), sh = rows * ROW - 2;
            g.fill(sx, sy, sx + 2, sy + sh, 0x60FFFFFF);
            int th = Math.max(10, sh * rows / data.offers.size());
            int ty = sy + (sh - th) * scroll / Math.max(1, data.offers.size() - rows);
            g.fill(sx, ty, sx + 2, ty + th, UiKit.FRAME);
        }
        g.drawString(font, "Продать добычу: Shift+ПКМ по торговцу с вещью в руке.", x, top + H - 14, UiKit.MUTED, false);
        if (tip != null) g.renderTooltip(font, tip, mx, my);
        else if (tipText != null) g.renderTooltip(font, font.split(Component.literal(tipText), 240), mx, my);
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        for (Hit h : hits) {
            if (UiKit.inside(mx, my, h.x, h.y, h.w, h.h)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                ModNetwork.CHANNEL.sendToServer(new BuyPacket(entityId, h.offer));
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
