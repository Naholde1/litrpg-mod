package com.andrey.litrpg.network;

import com.andrey.litrpg.LitRPG;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String VERSION = "32";
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
        CHANNEL.messageBuilder(OpenOriginPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenOriginPacket::encode).decoder(OpenOriginPacket::new)
                .consumerMainThread(OpenOriginPacket::handle).add();
        CHANNEL.messageBuilder(ChooseOriginPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChooseOriginPacket::encode).decoder(ChooseOriginPacket::new)
                .consumerMainThread(ChooseOriginPacket::handle).add();
        CHANNEL.messageBuilder(OpenClassChoicePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenClassChoicePacket::encode).decoder(OpenClassChoicePacket::new)
                .consumerMainThread(OpenClassChoicePacket::handle).add();
        CHANNEL.messageBuilder(ChooseClassPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChooseClassPacket::encode).decoder(ChooseClassPacket::new)
                .consumerMainThread(ChooseClassPacket::handle).add();
        CHANNEL.messageBuilder(FormPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FormPacket::encode).decoder(FormPacket::new)
                .consumerMainThread(FormPacket::handle).add();
        CHANNEL.messageBuilder(ManaPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ManaPacket::encode).decoder(ManaPacket::new)
                .consumerMainThread(ManaPacket::handle).add();
        CHANNEL.messageBuilder(ThreatPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ThreatPacket::encode).decoder(ThreatPacket::new)
                .consumerMainThread(ThreatPacket::handle).add();
        CHANNEL.messageBuilder(SetBarPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetBarPacket::encode).decoder(SetBarPacket::new)
                .consumerMainThread(SetBarPacket::handle).add();
        CHANNEL.messageBuilder(RankSkillPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RankSkillPacket::encode).decoder(RankSkillPacket::new)
                .consumerMainThread(RankSkillPacket::handle).add();
        CHANNEL.messageBuilder(OpenShopPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenShopPacket::encode).decoder(OpenShopPacket::new)
                .consumerMainThread(OpenShopPacket::handle).add();
        CHANNEL.messageBuilder(BuyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(BuyPacket::encode).decoder(BuyPacket::new)
                .consumerMainThread(BuyPacket::handle).add();
        CHANNEL.messageBuilder(PartyPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PartyPacket::encode).decoder(PartyPacket::new)
                .consumerMainThread(PartyPacket::handle).add();
        CHANNEL.messageBuilder(FocusPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(FocusPacket::encode).decoder(FocusPacket::new)
                .consumerMainThread(FocusPacket::handle).add();
        CHANNEL.messageBuilder(CastPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CastPacket::encode).decoder(CastPacket::new)
                .consumerMainThread(CastPacket::handle).add();
        CHANNEL.messageBuilder(FxPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FxPacket::encode).decoder(FxPacket::new)
                .consumerMainThread(FxPacket::handle).add();
    }
}
