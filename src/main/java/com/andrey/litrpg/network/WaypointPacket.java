package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Поставить метку на карте игрока (например, найденное подземелье). */
public class WaypointPacket {
    private final int x, z;

    public WaypointPacket(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public WaypointPacket(FriendlyByteBuf buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(z);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            com.andrey.litrpg.client.Waypoints.removeNear(x, z, 16);
            com.andrey.litrpg.client.Waypoints.add(x, z);
        });
    }
}
