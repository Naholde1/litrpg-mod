package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

/** Обработчики пакетов, которые трогают клиентские классы (вызываются только на клиенте). */
public final class ClientPackets {
    private ClientPackets() {}

    public static void openQuestBoard(String faction, String npcName) {
        Minecraft.getInstance().setScreen(new QuestBoardScreen(faction, npcName));
    }
}
