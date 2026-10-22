package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выбор класса из предложенных Системой. */
public class ChooseClassPacket {
    private final String clazz;

    public ChooseClassPacket(PlayerClass clazz) { this.clazz = clazz.key; }
    public ChooseClassPacket(FriendlyByteBuf buf) { this.clazz = buf.readUtf(64); }

    public void encode(FriendlyByteBuf buf) { buf.writeUtf(clazz, 64); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        PlayerClass c = PlayerClass.byKey(clazz);
        if (player == null || c == null) return;
        PlayerStats s = StatsManager.get(player);
        if (!s.canChooseClass() || !s.getOffers().contains(c)) return;
        StatsManager.chooseClass(player, c, true);
    }
}
