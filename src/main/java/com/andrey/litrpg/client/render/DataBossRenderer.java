package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.model.DataModel;
import com.andrey.litrpg.entity.BossBase;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Рендер боссов с моделью из данных и светящимся слоем (глаза, руны, пламя). */
public class DataBossRenderer<T extends BossBase> extends MobRenderer<T, DataModel<T>> {
    private final ResourceLocation texture;

    public DataBossRenderer(EntityRendererProvider.Context ctx, String name) {
        this(ctx, name, new DataModel<>(ctx.bakeLayer(layer(name)), name));
    }

    private DataBossRenderer(EntityRendererProvider.Context ctx, String name, DataModel<T> model) {
        super(ctx, model, model.shadow);
        this.texture = LitRPG.id("textures/entity/boss/" + name + ".png");
        ResourceLocation glow = LitRPG.id("textures/entity/boss/" + name + "_glow.png");
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbAmount,
                               float partialTick, float age, float yaw, float pitch) {
                if (entity.isInvisible()) return;
                VertexConsumer vc = buffers.getBuffer(RenderType.eyes(glow));
                float pulse = entity.isCasting() ? 1f : 0.85f + 0.15f * (float) Math.sin(age * 0.15f);
                getParentModel().renderToBuffer(pose, vc, 0xF000F0, OverlayTexture.NO_OVERLAY, pulse, pulse, pulse, 1f);
            }
        });
    }

    public static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(LitRPG.id(name), "main");
    }

    @Override
    protected void scale(T entity, PoseStack pose, float partialTick) {
        float s = getModel().scale;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
