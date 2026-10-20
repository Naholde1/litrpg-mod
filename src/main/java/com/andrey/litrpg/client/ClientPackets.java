package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

/** Обработчики пакетов, которые трогают клиентские классы (вызываются только на клиенте). */
public final class ClientPackets {
    private ClientPackets() {}

    public static void openQuestBoard(String board, String faction, String npcName, String workshop, int entityId) {
        Minecraft.getInstance().setScreen(new QuestBoardScreen(board, faction, npcName, workshop, entityId));
    }
}
