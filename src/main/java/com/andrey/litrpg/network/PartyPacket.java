package com.andrey.litrpg.network;

import com.andrey.litrpg.client.ClientData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Состояние товарищей по группе для панели в HUD. */
public class PartyPacket {
    public record Member(String name, float hp, float maxHp, float mana, int level, int color) {}

    private final List<Member> members;

    public PartyPacket(List<Member> members) { this.members = members; }

    public PartyPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        members = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            members.add(new Member(buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readInt()));
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(members.size());
        for (Member m : members) {
            buf.writeUtf(m.name(), 32);
            buf.writeFloat(m.hp());
            buf.writeFloat(m.maxHp());
            buf.writeFloat(m.mana());
            buf.writeVarInt(m.level());
            buf.writeInt(m.color());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.party = members);
    }
}
