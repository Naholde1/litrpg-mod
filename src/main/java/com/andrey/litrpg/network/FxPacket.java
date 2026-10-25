package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Визуальный эффект (рисует клиент, {@link com.andrey.litrpg.client.fx.FxRenderer}): форма, длительность, цвет,
 * числа формы (координаты, радиусы, направление) и до двух привязанных сущностей. См. {@link com.andrey.litrpg.fx.Fx}.
 */
public class FxPacket {
    public final int shape;
    public final int duration;
    public final int color;
    public final float[] p;
    public final int a, b;

    public FxPacket(int shape, int duration, int color, float[] p, int a, int b) {
        this.shape = shape;
        this.duration = duration;
        this.color = color;
        this.p = p;
        this.a = a;
        this.b = b;
    }

    public FxPacket(FriendlyByteBuf buf) {
        shape = buf.readByte();
        duration = buf.readVarInt();
        color = buf.readInt();
        int n = Math.min(16, buf.readByte());
        p = new float[n];
        for (int i = 0; i < n; i++) p[i] = buf.readFloat();
        a = buf.readVarInt();
        b = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByte(shape);
        buf.writeVarInt(duration);
        buf.writeInt(color);
        buf.writeByte(Math.min(16, p.length));
        for (int i = 0; i < Math.min(16, p.length); i++) buf.writeFloat(p[i]);
        buf.writeVarInt(a);
        buf.writeVarInt(b);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.andrey.litrpg.client.fx.FxRenderer.add(this));
    }
}
