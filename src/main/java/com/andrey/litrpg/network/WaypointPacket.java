package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Поставить метку на карте игрока (например, найденное подземелье). */
public class WaypointPacket {
    private final int x, z;
    /** Id задания и подпись метки (пустой id — обычная метка). */
    private final String tag, name;

    public WaypointPacket(int x, int z) {
        this(x, z, "", "");
    }

    public WaypointPacket(int x, int z, String tag, String name) {
        this.x = x;
        this.z = z;
        this.tag = tag;
        this.name = name;
    }

    public WaypointPacket(FriendlyByteBuf buf) {
        this.x = buf.readInt();
        this.z = buf.readInt();
        this.tag = buf.readUtf();
        this.name = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(z);
        buf.writeUtf(tag);
        buf.writeUtf(name);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (!tag.isEmpty()) {
                com.andrey.litrpg.client.Waypoints.addQuest(x, z, name, tag);
                return;
            }
            com.andrey.litrpg.client.Waypoints.removeNear(x, z, 16);
            com.andrey.litrpg.client.Waypoints.add(x, z);
        });
    }
}
