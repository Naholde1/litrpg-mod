package com.andrey.litrpg.network;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Profession;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Изучить или забыть профессию из окна персонажа. */
public class ProfessionPacket {
    private final int prof;
    private final boolean learn;

    public ProfessionPacket(Profession prof, boolean learn) {
        this.prof = prof.ordinal();
        this.learn = learn;
    }

    public ProfessionPacket(FriendlyByteBuf buf) {
        this.prof = buf.readVarInt();
        this.learn = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(prof);
        buf.writeBoolean(learn);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player == null || prof < 0 || prof >= Profession.values().length) return;
        Profession p = Profession.values()[prof];
        PlayerStats s = StatsManager.get(player);
        if (learn) {
            if (s.learn(p)) {
                StatsManager.message(player, Component.literal("Изучена профессия: ")
                        .append(Component.translatable(p.nameKey()).withStyle(ChatFormatting.GOLD)));
            } else {
                StatsManager.message(player, Component.literal("Можно освоить не больше " + Profession.MAX_KNOWN + " профессий.")
                        .withStyle(ChatFormatting.RED));
            }
        } else if (s.knows(p)) {
            s.forget(p);
            StatsManager.message(player, Component.literal("Профессия забыта: ")
                    .append(Component.translatable(p.nameKey())).withStyle(ChatFormatting.GRAY));
        }
        StatsManager.sync(player);
    }
}
