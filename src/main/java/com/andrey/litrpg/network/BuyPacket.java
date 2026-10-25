package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Купить товар у торговца привала. */
public class BuyPacket {
    private final int entityId;
    private final int offer;

    public BuyPacket(int entityId, int offer) {
        this.entityId = entityId;
        this.offer = offer;
    }

    public BuyPacket(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        offer = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(offer);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var p = ctx.get().getSender();
        if (p != null) com.andrey.litrpg.run.Shop.buy(p, entityId, offer);
    }
}
