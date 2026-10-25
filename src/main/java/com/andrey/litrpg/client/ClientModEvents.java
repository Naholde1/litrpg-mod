package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.render.DataBossRenderer;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = LitRPG.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {

    /** Существа с моделями из данных (tools/gen_bosses.py, gen_bestiary.py). */
    public static final String[] CREATURE_MODELS = {"stone_troll", "frost_wraith", "forest_wisp", "ash_imp", "bog_lurker", "sand_scorpion", "grave_knight"};

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MANA_BOLT.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.4f, true));
        event.registerEntityRenderer(ModEntities.BOSS_PROJECTILE.get(), ctx -> new ThrownItemRenderer<>(ctx, 1.0f, true));
        event.registerEntityRenderer(ModEntities.TRADER.get(), com.andrey.litrpg.client.render.TraderRenderer::new);
        event.registerEntityRenderer(ModEntities.STONE_TROLL.get(), ctx -> new DataBossRenderer<>(ctx, "stone_troll"));
        event.registerEntityRenderer(ModEntities.FROST_WRAITH.get(), ctx -> new DataBossRenderer<>(ctx, "frost_wraith"));
        event.registerEntityRenderer(ModEntities.FOREST_WISP.get(), ctx -> new DataBossRenderer<>(ctx, "forest_wisp"));
        event.registerEntityRenderer(ModEntities.ASH_IMP.get(), ctx -> new DataBossRenderer<>(ctx, "ash_imp"));
        event.registerEntityRenderer(ModEntities.BOG_LURKER.get(), ctx -> new DataBossRenderer<>(ctx, "bog_lurker"));
        event.registerEntityRenderer(ModEntities.SAND_SCORPION.get(), ctx -> new DataBossRenderer<>(ctx, "sand_scorpion"));
        event.registerEntityRenderer(ModEntities.GRAVE_KNIGHT.get(), ctx -> new DataBossRenderer<>(ctx, "grave_knight"));
        ModEntities.WILD.forEach((id, t) -> event.registerEntityRenderer(t.get(), ctx -> new DataBossRenderer<>(ctx, id)));
        ModEntities.BOSSES.forEach((id, t) -> event.registerEntityRenderer(t.get(),
                ctx -> new DataBossRenderer<>(ctx, com.andrey.litrpg.entity.stage.Bosses.model(id))));
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        List<String> names = new ArrayList<>(List.of(CREATURE_MODELS));
        names.addAll(ModEntities.WILD.keySet());
        for (String id : ModEntities.BOSSES.keySet()) {
            String model = com.andrey.litrpg.entity.stage.Bosses.model(id);
            if (!names.contains(model)) names.add(model);
        }
        for (String name : names) {
            event.registerLayerDefinition(DataBossRenderer.layer(name), () -> com.andrey.litrpg.client.model.DataModel.createLayer(name));
        }
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(ModKeys.CHARACTER);
        event.register(ModKeys.WORLD_MAP);
        event.register(ModKeys.MINIMAP);
        event.register(ModKeys.DASH);
        for (KeyMapping k : ModKeys.ABILITIES) event.register(k);
        for (KeyMapping k : ModKeys.FOCUS) event.register(k);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("litrpg_hud", HudOverlay::render);
    }
}
