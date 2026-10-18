package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Расы всех игроков на сервере — чтобы клиент рисовал уши, рога и рост. */
public class RacesPacket {
    private final Map<UUID, Integer> races;

    public RacesPacket(Map<UUID, Integer> races) { this.races = races; }

    public RacesPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        races = new HashMap<>();
        for (int i = 0; i < n; i++) races.put(buf.readUUID(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(races.size());
        races.forEach((id, r) -> {
            buf.writeUUID(id);
            buf.writeVarInt(r);
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            ClientData.RACES.clear();
            ClientData.RACES.putAll(races);
        });
    }
}
