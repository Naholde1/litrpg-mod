package com.andrey.litrpg.network;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.SkillEffects;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Поднять ранг навыка за очки класса (окно персонажа, вкладка «Навыки»). */
public class RankSkillPacket {
    private final String key;

    public RankSkillPacket(String key) { this.key = key; }
    public RankSkillPacket(FriendlyByteBuf buf) { this.key = buf.readUtf(64); }

    public void encode(FriendlyByteBuf buf) { buf.writeUtf(key, 64); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null) return;
        PlayerStats s = StatsManager.get(player);
        if (SkillEffects.inCombat(player)) {
            player.displayClientMessage(Component.literal("В бою навыки не прокачать — отойдите от схватки").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!s.rankUp(key)) return;
        Ability a = Ability.byKey(key);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.6f);
        player.displayClientMessage(Component.translatable(a.nameKey()).append(" — ранг " + s.getRank(key))
                .withStyle(ChatFormatting.GOLD), true);
        StatsManager.sync(player);
    }
}
