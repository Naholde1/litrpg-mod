package com.andrey.litrpg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;

/** Скелет-страж: модель скелета, текстура скелета-иссушителя, увеличенный размер. */
public class SkeletonWardenRenderer extends SkeletonRenderer {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/skeleton/wither_skeleton.png");

    public SkeletonWardenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected void scale(AbstractSkeleton entity, PoseStack pose, float partialTick) {
        pose.scale(1.2f, 1.2f, 1.2f);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
        return TEXTURE;
    }
}
