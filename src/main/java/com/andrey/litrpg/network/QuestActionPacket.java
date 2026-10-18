package com.andrey.litrpg.network;

import com.andrey.litrpg.quest.QuestEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Действие игрока в окне заданий: принять, отказаться, сдать, найти подземелье. */
public class QuestActionPacket {
    public enum Action { ACCEPT, ABANDON, TURN_IN, LOCATE }

    private final Action action;
    private final String source;
    private final String id;

    public QuestActionPacket(Action action, String source, String id) {
        this.action = action;
        this.source = source;
        this.id = id;
    }

    public QuestActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.source = buf.readUtf(64);
        this.id = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeUtf(source, 64);
        buf.writeUtf(id, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer p = ctx.get().getSender();
        if (p == null) return;
        switch (action) {
            case ACCEPT -> QuestEvents.accept(p, source, id);
            case ABANDON -> QuestEvents.abandon(p, id);
            case TURN_IN -> QuestEvents.turnIn(p, id);
            case LOCATE -> QuestEvents.locate(p, id);
        }
    }
}
