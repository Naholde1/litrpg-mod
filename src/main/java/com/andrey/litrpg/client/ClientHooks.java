package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

/** Точки входа клиента, вызываемые из сетевых пакетов. */
public final class ClientHooks {
    private ClientHooks() {}

    /** Не перекрываем экран смерти и не открываем окна мёртвому игроку. */
    private static boolean busy(Minecraft mc) {
        return mc.player == null || !mc.player.isAlive() || mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen;
    }

    /** Окно роли попросили открыть, пока клиент ещё не готов (вход в мир) — откроем, как только сможем. */
    static boolean pendingOrigin;

    public static void openOriginScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (busy(mc) || mc.screen != null && !(mc.screen instanceof OriginScreen)) {
            pendingOrigin = true;
            return;
        }
        pendingOrigin = false;
        if (!(mc.screen instanceof OriginScreen)) mc.setScreen(new OriginScreen());
    }

    /** Каждый тик клиента: отложенное окно роли. */
    static void tick(Minecraft mc) {
        if (pendingOrigin && !busy(mc) && mc.screen == null && mc.level != null && mc.player.tickCount > 20) {
            pendingOrigin = false;
            if (!ClientData.received || ClientData.STATS.getRole() == null) mc.setScreen(new OriginScreen());
        }
    }

    public static void openClassChoice() {
        Minecraft mc = Minecraft.getInstance();
        // данные с предложениями приходят отдельным пакетом — откроем на следующем тике
        mc.tell(() -> {
            if (!busy(mc) && !(mc.screen instanceof ClassChoiceScreen) && !(mc.screen instanceof OriginScreen)) mc.setScreen(new ClassChoiceScreen());
        });
    }
}
