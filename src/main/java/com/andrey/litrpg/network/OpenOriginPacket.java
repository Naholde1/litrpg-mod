package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер просит клиента открыть окно выбора расы и класса. */
public class OpenOriginPacket {
    public OpenOriginPacket() {}
    public OpenOriginPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openOriginScreen);
    }
}
