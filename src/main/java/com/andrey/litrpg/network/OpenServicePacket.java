package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Окно услуг жителя (маг порталов — города, хозяин постоялого двора — комнаты). */
public class OpenServicePacket {
    /** Строка меню: подпись, пояснение, цена и кнопка (пустая кнопка — просто строка-сведение). */
    public record Entry(String key, String label, String detail, String price, String button, int accent, boolean enabled) {}

    public final int entityId;
    public final String title;
    public final String intro;
    public final List<Entry> entries;

    public OpenServicePacket(int entityId, String title, String intro, List<Entry> entries) {
        this.entityId = entityId;
        this.title = title;
        this.intro = intro;
        this.entries = entries;
    }

    public OpenServicePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.title = buf.readUtf(128);
        this.intro = buf.readUtf(512);
        int n = Math.min(64, buf.readVarInt());
        this.entries = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            entries.add(new Entry(buf.readUtf(64), buf.readUtf(128), buf.readUtf(256), buf.readUtf(64), buf.readUtf(32), buf.readInt(), buf.readBoolean()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(title, 128);
        buf.writeUtf(intro, 512);
        buf.writeVarInt(Math.min(64, entries.size()));
        for (int i = 0; i < Math.min(64, entries.size()); i++) {
            Entry e = entries.get(i);
            buf.writeUtf(e.key(), 64);
            buf.writeUtf(e.label(), 128);
            buf.writeUtf(e.detail(), 256);
            buf.writeUtf(e.price(), 64);
            buf.writeUtf(e.button(), 32);
            buf.writeInt(e.accent());
            buf.writeBoolean(e.enabled());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.andrey.litrpg.client.ClientPackets.openService(this));
    }
}
