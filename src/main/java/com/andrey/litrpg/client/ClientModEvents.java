package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.render.GoblinKingRenderer;
import com.andrey.litrpg.client.render.GoblinRenderer;
import com.andrey.litrpg.client.render.LichRenderer;
import com.andrey.litrpg.client.render.SkeletonWardenRenderer;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LitRPG.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GOBLIN.get(),
                ctx -> new GoblinRenderer<>(ctx, "goblin", 0.75f));
        event.registerEntityRenderer(ModEntities.GOBLIN_SHAMAN.get(),
                ctx -> new GoblinRenderer<>(ctx, "goblin_shaman", 0.75f));
        event.registerEntityRenderer(ModEntities.GOBLIN_KING.get(), GoblinKingRenderer::new);
        event.registerEntityRenderer(ModEntities.LICH.get(), LichRenderer::new);
        event.registerEntityRenderer(ModEntities.SKELETON_WARDEN.get(), SkeletonWardenRenderer::new);
        event.registerEntityRenderer(ModEntities.MANA_BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4f, true));
        com.andrey.litrpg.npc.NpcRegistry.TYPES.values().forEach(t ->
                event.registerEntityRenderer(t.get(), com.andrey.litrpg.client.render.NpcRenderer::new));
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.CHARACTER);
        event.register(ModKeys.WORLD_MAP);
        event.register(ModKeys.MINIMAP);
        for (KeyMapping k : ModKeys.ABILITIES) event.register(k);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("litrpg_hud", HudOverlay::render);
    }
}
