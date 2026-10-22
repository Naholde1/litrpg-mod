package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выбор расы и роли. Раса принимается, только если ещё не выбрана; роль — пока нет класса. */
public class ChooseOriginPacket {
    private final int race;
    private final int role;

    public ChooseOriginPacket(Race race, com.andrey.litrpg.stats.Role role) {
        this.race = race == null ? -1 : race.ordinal();
        this.role = role == null ? -1 : role.ordinal();
    }

    public ChooseOriginPacket(FriendlyByteBuf buf) {
        this.race = buf.readVarInt();
        this.role = buf.readVarInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(race);
        buf.writeVarInt(role);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        PlayerStats s = StatsManager.get(player);
        var roles = com.andrey.litrpg.stats.Role.values();
        if (role >= 0 && role < roles.length && !s.hasClass()) s.setRole(roles[role]);
        if (race >= 0 && race < Race.values().length && !s.hasOrigin()) {
            StatsManager.chooseOrigin(player, Race.values()[race], true);
        }
        if (role >= 0 && role < roles.length && !s.hasClass()) StatsManager.chooseRole(player, roles[role]);
    }
}
