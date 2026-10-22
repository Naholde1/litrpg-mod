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
        this.data = stats.saveForSync();
        // только идущие перезарядки: пары (номер навыка, тики)
        java.util.List<Integer> list = new java.util.ArrayList<>();
        for (Ability a : Ability.values()) {
            int left = (int) Math.max(0, stats.getCooldownEnd(a) - now);
            if (left > 0) {
                list.add(a.ordinal());
                list.add(left);
            }
        }
        this.cooldownLeft = list.stream().mapToInt(Integer::intValue).toArray();
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
