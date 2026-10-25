package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.Role;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выбор роли (пока нет класса). */
public class ChooseOriginPacket {
    private final int role;

    public ChooseOriginPacket(Role role) {
        this.role = role == null ? -1 : role.ordinal();
    }

    public ChooseOriginPacket(FriendlyByteBuf buf) {
        this.role = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(role);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        Role[] roles = Role.values();
        if (role >= 0 && role < roles.length) StatsManager.chooseRole(player, roles[role]);
    }
}
