package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер просит открыть окно выбора класса (предложения уже лежат в синхронизированных данных). */
public class OpenClassChoicePacket {
    public OpenClassChoicePacket() {}
    public OpenClassChoicePacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openClassChoice);
    }
}
