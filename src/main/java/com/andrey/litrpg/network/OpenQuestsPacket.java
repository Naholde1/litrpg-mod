package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Открыть доску поручений народа (после разговора с жителем). */
public class OpenQuestsPacket {
    private final String faction;
    private final String npcName;

    public OpenQuestsPacket(String faction, String npcName) {
        this.faction = faction;
        this.npcName = npcName;
    }

    public OpenQuestsPacket(FriendlyByteBuf buf) {
        this.faction = buf.readUtf(64);
        this.npcName = buf.readUtf(128);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(faction, 64);
        buf.writeUtf(npcName, 128);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                net.minecraft.client.Minecraft.getInstance().setScreen(new com.andrey.litrpg.client.QuestBoardScreen(faction, npcName)));
    }
}
