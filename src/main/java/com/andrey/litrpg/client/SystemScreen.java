package com.andrey.litrpg.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Окно фиксированного размера W×H, которое само уменьшается, если не
 * помещается на экране (маленькое окно игры или крупный масштаб GUI).
 * Наследники рисуют в {@link #renderContent} в координатах окна.
 */
public abstract class SystemScreen extends Screen {
    protected final int W;
    protected final int H;
    protected int left, top;
    private float scale = 1f;

    protected SystemScreen(Component title, int w, int h) {
        super(title);
        this.W = w;
        this.H = h;
    }

    @Override
    protected void init() {
        scale = Math.min(1f, Math.min((width - 8) / (float) W, (height - 8) / (float) H));
        // окно рисуется в своей системе координат с началом в (left, top)
        left = (int) ((width / scale - W) / 2);
        top = (int) ((height / scale - H) / 2);
    }

    protected abstract void renderContent(GuiGraphics g, int mx, int my, float pt);

    protected abstract boolean clickContent(double mx, double my, int button);

    protected boolean scrollContent(double mx, double my, double delta) { return false; }

    @Override
    public final void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1f);
        renderContent(g, (int) (mx / scale), (int) (my / scale), pt);
        g.pose().popPose();
    }

    @Override
    public final boolean mouseClicked(double mx, double my, int button) {
        return clickContent(mx / scale, my / scale, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public final boolean mouseScrolled(double mx, double my, double delta) {
        return scrollContent(mx / scale, my / scale, delta) || super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
