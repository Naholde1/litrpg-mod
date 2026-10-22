package com.andrey.litrpg.network;

import com.andrey.litrpg.city.Services;
import com.andrey.litrpg.npc.RaceNpcEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Игрок выбрал услугу в окне жителя (или открыл окно услуг с доски мастера: key = «open»). */
public class ServicePacket {
    private final int entityId;
    private final String key;

    public ServicePacket(int entityId, String key) {
        this.entityId = entityId;
        this.key = key;
    }

    public ServicePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.key = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(key, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        if (p.level().getEntity(entityId) instanceof RaceNpcEntity npc && npc.isAlive() && npc.distanceToSqr(p) < 10 * 10) {
            if (key.equals("open")) Services.open(p, npc);
            else Services.choose(p, npc, key);
        }
    }
}
