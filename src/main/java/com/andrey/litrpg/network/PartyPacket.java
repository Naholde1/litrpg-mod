package com.andrey.litrpg.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Рамки группы: здоровье, мана, роль, состояние каждого участника (раз в 4 тика). */
public class PartyPacket {
    /** role: 0 — нет, 1 — танк, 2 — боец, 3 — поддержка; flags: 1 — дух, 2 — в бою. */
    public record Member(UUID id, String name, float hp, float maxHp, float absorb, float mana, int role, int color, int flags) {}

    public final List<Member> members;
    public final int stage;

    public PartyPacket(List<Member> members, int stage) {
        this.members = members;
        this.stage = stage;
    }

    public PartyPacket(FriendlyByteBuf buf) {
        stage = buf.readVarInt();
        int n = Math.min(8, buf.readVarInt());
        members = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            members.add(new Member(buf.readUUID(), buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readVarInt(), buf.readInt(), buf.readVarInt()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(stage);
        buf.writeVarInt(Math.min(8, members.size()));
        for (int i = 0; i < Math.min(8, members.size()); i++) {
            Member m = members.get(i);
            buf.writeUUID(m.id());
            buf.writeUtf(m.name(), 32);
            buf.writeFloat(m.hp());
            buf.writeFloat(m.maxHp());
            buf.writeFloat(m.absorb());
            buf.writeFloat(m.mana());
            buf.writeVarInt(m.role());
            buf.writeInt(m.color());
            buf.writeVarInt(m.flags());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.andrey.litrpg.client.ClientData.setParty(this));
    }
}
