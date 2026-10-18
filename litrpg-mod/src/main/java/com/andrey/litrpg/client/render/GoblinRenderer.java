package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/** Общий рендер человекоподобных мобов мода с собственной текстурой и масштабом. */
public class GoblinRenderer<T extends Mob> extends HumanoidMobRenderer<T, HumanoidModel<T>> {
    private final ResourceLocation texture;
    private final float scale;

    public GoblinRenderer(EntityRendererProvider.Context ctx, String textureName, float scale) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4f * scale);
        this.texture = LitRPG.id("textures/entity/" + textureName + ".png");
        this.scale = scale;
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        pose.scale(scale, scale, scale);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
