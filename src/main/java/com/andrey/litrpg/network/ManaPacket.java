package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Только текущая мана — 8 байт вместо полного набора данных персонажа. */
public class ManaPacket {
    private final double mana;

    public ManaPacket(double mana) { this.mana = mana; }
    public ManaPacket(FriendlyByteBuf buf) { this.mana = buf.readDouble(); }

    public void encode(FriendlyByteBuf buf) { buf.writeDouble(mana); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.STATS.setMana(mana));
    }
}
