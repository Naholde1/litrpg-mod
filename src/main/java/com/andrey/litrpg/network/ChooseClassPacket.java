package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выбор класса из трёх предложенных Системой. */
public class ChooseClassPacket {
    private final int clazz;

    public ChooseClassPacket(PlayerClass clazz) { this.clazz = clazz.ordinal(); }
    public ChooseClassPacket(FriendlyByteBuf buf) { this.clazz = buf.readVarInt(); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(clazz); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || clazz < 0 || clazz >= PlayerClass.values().length) return;
        PlayerStats s = StatsManager.get(player);
        PlayerClass c = PlayerClass.values()[clazz];
        if (!s.canChooseClass() || !s.getOffers().contains(c)) return;
        StatsManager.chooseClass(player, c, true);
    }
}
