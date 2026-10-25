package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Полоса каста: навык (номер) и сколько тиков подготовки; -1 — каст окончен или сбит. */
public class CastPacket {
    public final int ability;
    public final int ticks;

    public CastPacket(int ability, int ticks) {
        this.ability = ability;
        this.ticks = ticks;
    }

    public CastPacket(FriendlyByteBuf buf) {
        ability = buf.readVarInt();
        ticks = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(ability);
        buf.writeVarInt(ticks);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.andrey.litrpg.client.HudOverlay.cast(ability, ticks));
    }
}
