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
        devScreenshot(mc);
        Minimap.tick(mc);

        while (ModKeys.CHARACTER.consumeClick()) {
            if (ClientData.received && !ClientData.STATS.hasOrigin()) mc.setScreen(new OriginScreen());
            else if (ClientData.received && ClientData.STATS.canChooseClass() && !ClientData.STATS.getOffers().isEmpty()) mc.setScreen(new ClassChoiceScreen());
            else mc.setScreen(new CharacterScreen());
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

    /** Только в среде разработки: файл dev_screenshot.txt → снимок экрана в screenshots/. */
    private static void devScreenshot(Minecraft mc) {
        if (net.minecraftforge.fml.loading.FMLLoader.isProduction()) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) mc.player.respawn();
        if (mc.player.tickCount % 10 != 0) return;
        java.nio.file.Path open = java.nio.file.Path.of("dev_open.txt");
        if (java.nio.file.Files.exists(open)) {
            try {
                String what = java.nio.file.Files.readString(open).trim();
                java.nio.file.Files.delete(open);
                if (what.startsWith("char")) {
                    CharacterScreen.setTab(Integer.parseInt(what.substring(4).trim()));
                    mc.setScreen(new CharacterScreen());
                } else if (what.equals("close")) {
                    mc.setScreen(null);
                }
            } catch (Exception ignored) {
            }
        }
        java.nio.file.Path trigger = java.nio.file.Path.of("dev_screenshot.txt");
        if (!java.nio.file.Files.exists(trigger)) return;
        try {
            String name = java.nio.file.Files.readString(trigger).trim();
            java.nio.file.Files.delete(trigger);
            net.minecraft.client.Screenshot.grab(mc.gameDirectory, name.isEmpty() ? "dev.png" : name + ".png",
                    mc.getMainRenderTarget(), msg -> {});
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientData.received = false;
        ClientData.party = java.util.List.of();
        Waypoints.clearLoaded();
    }
}
