package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

/** Обработчики пакетов, которые трогают клиентские классы (вызываются только на клиенте). */
public final class ClientPackets {
    private ClientPackets() {}

    public static void openShop(com.andrey.litrpg.network.OpenShopPacket data) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ShopScreen s && s.entityId == data.entityId) s.refresh(data);
        else mc.setScreen(new ShopScreen(data));
    }
}
