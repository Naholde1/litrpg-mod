package com.andrey.litrpg.network;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.client.ClientData;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncStatsPacket {
    private final CompoundTag data;
    private final int[] cooldownLeft;

    public SyncStatsPacket(PlayerStats stats, long now) {
        this.data = stats.save();
        Ability[] all = Ability.values();
        this.cooldownLeft = new int[all.length];
        for (Ability a : all) {
            cooldownLeft[a.ordinal()] = (int) Math.max(0, stats.getCooldownEnd(a) - now);
        }
    }

    public SyncStatsPacket(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
        this.cooldownLeft = buf.readVarIntArray();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(data);
        buf.writeVarIntArray(cooldownLeft);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientData.update(data, cooldownLeft));
    }
}
