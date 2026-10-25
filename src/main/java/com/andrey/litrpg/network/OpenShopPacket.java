package com.andrey.litrpg.network;

import com.andrey.litrpg.run.Shop;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Открыть лавку торговца привала: товары, цены, сколько денег у игрока. */
public class OpenShopPacket {
    public final int entityId;
    public final int stage;
    public final int money;
    public final List<Shop.Offer> offers;

    public OpenShopPacket(int entityId, int stage, int money, List<Shop.Offer> offers) {
        this.entityId = entityId;
        this.stage = stage;
        this.money = money;
        this.offers = offers;
    }

    public OpenShopPacket(FriendlyByteBuf buf) {
        entityId = buf.readInt();
        stage = buf.readVarInt();
        money = buf.readVarInt();
        int n = Math.min(64, buf.readVarInt());
        offers = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            offers.add(new Shop.Offer(buf.readVarInt(), buf.readItem(), buf.readVarInt(), buf.readBoolean(), buf.readUtf(32), buf.readUtf(256)));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeVarInt(stage);
        buf.writeVarInt(money);
        buf.writeVarInt(Math.min(64, offers.size()));
        for (int i = 0; i < Math.min(64, offers.size()); i++) {
            Shop.Offer o = offers.get(i);
            buf.writeVarInt(o.id());
            buf.writeItem(o.stack());
            buf.writeVarInt(o.price());
            buf.writeBoolean(o.once());
            buf.writeUtf(o.action(), 32);
            buf.writeUtf(o.label(), 256);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.andrey.litrpg.client.ClientPackets.openShop(this));
    }
}
