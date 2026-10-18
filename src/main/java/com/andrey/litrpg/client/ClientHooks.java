package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

/** Точки входа клиента, вызываемые из сетевых пакетов. */
public final class ClientHooks {
    private ClientHooks() {}

    /** Не перекрываем экран смерти и не открываем окна мёртвому игроку. */
    private static boolean busy(Minecraft mc) {
        return mc.player == null || !mc.player.isAlive() || mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen;
    }

    public static void openOriginScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (!busy(mc) && !(mc.screen instanceof OriginScreen)) mc.setScreen(new OriginScreen());
    }

    public static void openClassChoice() {
        Minecraft mc = Minecraft.getInstance();
        // данные с предложениями приходят отдельным пакетом — откроем на следующем тике
        mc.tell(() -> {
            if (!busy(mc) && !(mc.screen instanceof ClassChoiceScreen) && !(mc.screen instanceof OriginScreen)) mc.setScreen(new ClassChoiceScreen());
        });
    }
}
