package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Какой облик надет на игроке. Уходит всем, кто его видит, а не только ему самому:
 * без этого товарищи видели бы человека там, где стоит медведь.
 */
public class FormPacket {
    private final UUID who;
    private final String form;

    public FormPacket(UUID who, String form) {
        this.who = who;
        this.form = form == null ? "" : form;
    }

    public FormPacket(FriendlyByteBuf buf) {
        this.who = buf.readUUID();
        this.form = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(who);
        buf.writeUtf(form, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (form.isEmpty()) ClientData.FORMS.remove(who);
            else ClientData.FORMS.put(who, form);
        }));
        ctx.get().setPacketHandled(true);
    }
}
