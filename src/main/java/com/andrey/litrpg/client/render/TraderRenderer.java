package com.andrey.litrpg.client.render;

import com.andrey.litrpg.run.TraderEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;

/** Торговец привала — в облике странствующего торговца. */
public class TraderRenderer extends MobRenderer<TraderEntity, VillagerModel<TraderEntity>> {
    private static final ResourceLocation TEX = new ResourceLocation("minecraft", "textures/entity/wandering_trader.png");

    public TraderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new VillagerModel<>(ctx.bakeLayer(ModelLayers.WANDERING_TRADER)), 0.5f);
        addLayer(new CrossedArmsItemLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(TraderEntity e) {
        return TEX;
    }
}
