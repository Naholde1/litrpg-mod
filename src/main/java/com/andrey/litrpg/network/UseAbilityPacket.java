package com.andrey.litrpg.network;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.AbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Нажатие клавиши навыка: передаётся номер ячейки 0–5. */
public class UseAbilityPacket {
    private final int slot;

    public UseAbilityPacket(int slot) { this.slot = slot; }
    public UseAbilityPacket(FriendlyByteBuf buf) { this.slot = buf.readVarInt(); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(slot); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        if (player != null && slot >= 0 && slot < Ability.BAR && player.isAlive()) {
            AbilityHandler.tryUse(player, slot);
        }
    }
}
