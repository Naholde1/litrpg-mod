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
        event.registerEntityRenderer(ModEntities.RUIN_GOLEM.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "ruin_golem"));
        event.registerEntityRenderer(ModEntities.ROTTING_ENT.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "rotting_ent"));
        event.registerEntityRenderer(ModEntities.SPIDER_QUEEN.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "spider_queen"));
        event.registerEntityRenderer(ModEntities.SWAMP_HAG.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "swamp_hag"));
        event.registerEntityRenderer(ModEntities.MINOTAUR.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "minotaur"));
        event.registerEntityRenderer(ModEntities.FROST_GIANT.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "frost_giant"));
        event.registerEntityRenderer(ModEntities.FLAME_LORD.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "flame_lord"));
        event.registerEntityRenderer(ModEntities.ARCHDEMON.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "archdemon"));
        event.registerEntityRenderer(ModEntities.BOSS_PROJECTILE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0f, true));
        event.registerEntityRenderer(ModEntities.STONE_TROLL.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "stone_troll"));
        event.registerEntityRenderer(ModEntities.FROST_WRAITH.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "frost_wraith"));
        event.registerEntityRenderer(ModEntities.FOREST_WISP.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "forest_wisp"));
        event.registerEntityRenderer(ModEntities.ASH_IMP.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "ash_imp"));
        event.registerEntityRenderer(ModEntities.BOG_LURKER.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "bog_lurker"));
        event.registerEntityRenderer(ModEntities.SAND_SCORPION.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "sand_scorpion"));
        event.registerEntityRenderer(ModEntities.GRAVE_KNIGHT.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "grave_knight"));
        event.registerEntityRenderer(ModEntities.RAT.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "rat"));
        event.registerEntityRenderer(ModEntities.RAT_KING.get(), ctx -> new com.andrey.litrpg.client.render.DataBossRenderer<>(ctx, "rat_king"));
        com.andrey.litrpg.npc.NpcRegistry.TYPES.values().forEach(t ->
                event.registerEntityRenderer(t.get(), com.andrey.litrpg.client.render.NpcRenderer::new));
    }

    public static final String[] BOSS_MODELS = {"ruin_golem", "rotting_ent", "spider_queen", "swamp_hag", "minotaur", "frost_giant", "flame_lord", "archdemon", "stone_troll", "frost_wraith", "forest_wisp", "ash_imp", "bog_lurker", "sand_scorpion", "grave_knight", "rat", "rat_king"};

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (String name : BOSS_MODELS) {
            event.registerLayerDefinition(com.andrey.litrpg.client.render.DataBossRenderer.layer(name),
                    () -> com.andrey.litrpg.client.model.DataModel.createLayer(name));
        }
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
