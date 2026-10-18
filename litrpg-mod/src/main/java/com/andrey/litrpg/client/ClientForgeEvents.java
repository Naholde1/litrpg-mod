package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.UseAbilityPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
public class ClientForgeEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ClientData.tick();
        Minimap.tick(mc);

        while (ModKeys.CHARACTER.consumeClick()) {
            mc.setScreen(new CharacterScreen());
        }
        while (ModKeys.WORLD_MAP.consumeClick()) {
            mc.setScreen(new WorldMapScreen());
        }
        while (ModKeys.MINIMAP.consumeClick()) {
            Minimap.enabled = !Minimap.enabled;
        }
        for (int i = 0; i < ModKeys.ABILITIES.length; i++) {
            while (ModKeys.ABILITIES[i].consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new UseAbilityPacket(i));
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.received = false;
        Waypoints.clearLoaded();
    }
}
