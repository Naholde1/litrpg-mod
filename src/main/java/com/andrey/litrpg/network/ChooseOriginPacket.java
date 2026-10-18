package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выбор расы. Принимается, только если раса ещё не выбрана. */
public class ChooseOriginPacket {
    private final int race;

    public ChooseOriginPacket(Race race) { this.race = race.ordinal(); }
    public ChooseOriginPacket(FriendlyByteBuf buf) { this.race = buf.readVarInt(); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(race); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || race < 0 || race >= Race.values().length) return;
        PlayerStats s = StatsManager.get(player);
        if (s.hasOrigin()) return;
        StatsManager.chooseOrigin(player, Race.values()[race], true);
    }
}
