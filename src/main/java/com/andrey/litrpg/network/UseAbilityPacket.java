package com.andrey.litrpg.network;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.AbilityHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UseAbilityPacket {
    private final int id;

    public UseAbilityPacket(int id) { this.id = id; }
    public UseAbilityPacket(FriendlyByteBuf buf) { this.id = buf.readVarInt(); }

    public void encode(FriendlyByteBuf buf) { buf.writeVarInt(id); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer player = ctx.get().getSender();
        Ability a = Ability.byId(id);
        if (player != null && a != null && player.isAlive()) {
            AbilityHandler.tryUse(player, a);
        }
    }
}
