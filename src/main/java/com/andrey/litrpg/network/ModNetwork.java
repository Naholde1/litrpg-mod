package com.andrey.litrpg.network;

import com.andrey.litrpg.LitRPG;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            LitRPG.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(SyncStatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncStatsPacket::encode).decoder(SyncStatsPacket::new)
                .consumerMainThread(SyncStatsPacket::handle).add();
        CHANNEL.messageBuilder(UseAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseAbilityPacket::encode).decoder(UseAbilityPacket::new)
                .consumerMainThread(UseAbilityPacket::handle).add();
        CHANNEL.messageBuilder(SpendPointPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendPointPacket::encode).decoder(SpendPointPacket::new)
                .consumerMainThread(SpendPointPacket::handle).add();
    }
}
