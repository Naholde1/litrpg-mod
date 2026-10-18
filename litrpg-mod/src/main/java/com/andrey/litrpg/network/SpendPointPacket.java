package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SpendPointPacket {
    private final int stat;
    private final int amount;

    public SpendPointPacket(int stat, int amount) { this.stat = stat; this.amount = amount; }
    public SpendPointPacket(FriendlyByteBuf buf) { this.stat = buf.readVarInt(); this.amount = buf.readVarInt(); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(stat); buf.writeVarInt(amount); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || stat < 0 || stat >= Stat.values().length) return;
        PlayerStats s = StatsManager.get(player);
        Stat st = Stat.values()[stat];
        int n = Math.max(1, Math.min(amount, 100));
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            if (!s.spendPoint(st)) break;
            changed = true;
        }
        if (changed) {
            StatsManager.applyAttributes(player);
            StatsManager.sync(player);
        }
    }
}
