package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Открыть доску поручений: народа (после разговора с жителем) или мастера города. */
public class OpenQuestsPacket {
    private final String board;
    private final String faction;
    private final String npcName;
    /** Название мастерской (для досок мастеров) или пусто. */
    private final String workshop;
    /** Id сущности мастера (для кнопки «Торговать») или -1. */
    private final int entityId;

    public OpenQuestsPacket(String board, String faction, String npcName, String workshop, int entityId) {
        this.board = board;
        this.faction = faction;
        this.npcName = npcName;
        this.workshop = workshop;
        this.entityId = entityId;
    }

    public OpenQuestsPacket(FriendlyByteBuf buf) {
        this.board = buf.readUtf(64);
        this.faction = buf.readUtf(64);
        this.npcName = buf.readUtf(128);
        this.workshop = buf.readUtf(128);
        this.entityId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(board, 64);
        buf.writeUtf(faction, 64);
        buf.writeUtf(npcName, 128);
        buf.writeUtf(workshop, 128);
        buf.writeInt(entityId);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.andrey.litrpg.client.ClientPackets.openQuestBoard(board, faction, npcName, workshop, entityId));
    }
}
