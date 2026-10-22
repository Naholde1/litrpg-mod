package com.andrey.litrpg.network;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.SkillEffects;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Поставить навык класса в ячейку панели (пустой ключ — убрать). Только вне боя. */
public class SetBarPacket {
    private final int slot;
    private final String key;

    public SetBarPacket(int slot, String key) {
        this.slot = slot;
        this.key = key == null ? "" : key;
    }

    public SetBarPacket(FriendlyByteBuf buf) {
        this.slot = buf.readVarInt();
        this.key = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeUtf(key, 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || slot < 0 || slot >= Ability.BAR) return;
        PlayerStats s = StatsManager.get(player);
        if (!s.hasClass()) return;
        if (SkillEffects.inCombat(player)) {
            player.displayClientMessage(Component.literal("В бою навыки не сменить — отойдите от схватки").withStyle(ChatFormatting.RED), true);
            return;
        }
        String k = key.isEmpty() ? null : key;
        if (k != null && s.getPlayerClass().unlockOf(k) > s.getLevel()) return;
        if (s.setBar(slot, k)) StatsManager.sync(player);
    }
}
