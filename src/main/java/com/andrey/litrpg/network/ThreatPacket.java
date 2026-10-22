package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Агр моба для рамки цели: кого он бьёт и сколько угрозы у игрока (в % от лидера). */
public class ThreatPacket {
    private final int entityId;
    private final String target;
    private final boolean onMe;
    private final int percent;

    public ThreatPacket(int entityId, String target, boolean onMe, int percent) {
        this.entityId = entityId;
        this.target = target;
        this.onMe = onMe;
        this.percent = percent;
    }

    public ThreatPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.target = buf.readUtf(64);
        this.onMe = buf.readBoolean();
        this.percent = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(target, 64);
        buf.writeBoolean(onMe);
        buf.writeVarInt(percent);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.andrey.litrpg.client.HudOverlay.threat(entityId, target, onMe, percent));
    }
}
