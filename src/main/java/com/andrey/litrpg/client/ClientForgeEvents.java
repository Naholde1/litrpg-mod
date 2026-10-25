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

    /**
     * Подземелье — своё измерение, и Майнкрафт помечает такие миры как
     * «экспериментальные»: при создании и при входе висит окно-предупреждение.
     * Подтверждаем его сами — это ожидаемо для мира мода.
     */
    @SubscribeEvent
    public static void onScreenOpen(net.minecraftforge.client.event.ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof net.minecraft.client.gui.screens.ConfirmScreen screen)) return;
        if (!(screen.getTitle().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents tc)) return;
        if (!tc.getKey().contains("experimental")) return;
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {
            for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                if (!it.unimi.dsi.fastutil.booleans.BooleanConsumer.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    var cb = (it.unimi.dsi.fastutil.booleans.BooleanConsumer) f.get(screen);
                    if (cb == null) return;
                    event.setCanceled(true);
                    Minecraft.getInstance().tell(() -> cb.accept(true));
                } catch (Exception ignored) {
                }
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        devScreenshot(mc);
        if (mc.player == null || mc.level == null) return;

        ClientData.tick();
        Minimap.tick(mc);

        while (ModKeys.CHARACTER.consumeClick()) {
            if (ClientData.received && ClientData.STATS.getRole() == null) mc.setScreen(new OriginScreen());
            else if (ClientData.received && ClientData.STATS.canChooseClass() && !ClientData.STATS.getOffers().isEmpty()) mc.setScreen(new ClassChoiceScreen());
            else mc.setScreen(new CharacterScreen());
        }
        while (ModKeys.WORLD_MAP.consumeClick()) {
            mc.setScreen(new WorldMapScreen());
        }
        while (ModKeys.MINIMAP.consumeClick()) {
            if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) Minimap.toggle();
            else Minimap.cycleZoom();
        }
        for (int i = 0; i < ModKeys.ABILITIES.length; i++) {
            while (ModKeys.ABILITIES[i].consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new UseAbilityPacket(i));
            }
        }
        while (ModKeys.DASH.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new UseAbilityPacket(com.andrey.litrpg.ability.Ability.DASH_SLOT));
        }
        for (int i = 0; i < ModKeys.FOCUS.length; i++) {
            while (ModKeys.FOCUS[i].consumeClick()) {
                java.util.UUID id = null;
                if (i < 5 && i < ClientData.party.size()) id = ClientData.party.get(i).id();
                if (id != null && id.equals(ClientData.focus)) id = null; // повторное нажатие — снять
                ClientData.focus = id;
                ModNetwork.CHANNEL.sendToServer(new com.andrey.litrpg.network.FocusPacket(id == null ? new java.util.UUID(0, 0) : id));
                if (mc.player != null) {
                    String who = id == null ? "по взгляду" : ClientData.party.get(i).name();
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Цель союзных навыков: " + who)
                            .withStyle(net.minecraft.ChatFormatting.GREEN), true);
                }
            }
        }
    }

    /** Только в среде разработки: файл dev_screenshot.txt → снимок экрана в screenshots/. */
    private static int devTicks;

    private static void devScreenshot(Minecraft mc) {
        if (net.minecraftforge.fml.loading.FMLLoader.isProduction()) return;
        if (mc.player != null && mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) mc.player.respawn();
        if (++devTicks % 10 != 0) return;
        java.nio.file.Path open = java.nio.file.Path.of("dev_open.txt");
        if (java.nio.file.Files.exists(open)) {
            try {
                String what = java.nio.file.Files.readString(open).trim();
                java.nio.file.Files.delete(open);
                if (what.startsWith("char")) {
                    CharacterScreen.setTab(Integer.parseInt(what.substring(4).trim()));
                    mc.setScreen(new CharacterScreen());
                } else if (what.equals("map")) {
                    mc.setScreen(new WorldMapScreen());
                } else if (what.equals("zoom")) {
                    Minimap.cycleZoom();
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
        ClientData.focus = null;
        Waypoints.clearLoaded();
        com.andrey.litrpg.client.map.MapCache.reset();
    }
}
