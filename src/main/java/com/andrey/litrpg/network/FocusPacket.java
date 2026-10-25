package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Привязка союзника (рамки группы, клавиши 1–5 с Ctrl): таргетные навыки летят в него. Нулевой UUID — снять. */
public class FocusPacket {
    private final UUID target;

    public FocusPacket(UUID target) {
        this.target = target;
    }

    public FocusPacket(FriendlyByteBuf buf) {
        target = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(target);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        var p = ctx.get().getSender();
        if (p == null) return;
        if (target.getLeastSignificantBits() == 0 && target.getMostSignificantBits() == 0) p.getPersistentData().remove(FOCUS);
        else p.getPersistentData().putUUID(FOCUS, target);
    }

    public static final String FOCUS = "litrpg_focus";
}
