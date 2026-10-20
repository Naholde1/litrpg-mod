package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.RaceLook;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.stats.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Житель расы: модель игрока со своим скином, ростом и деталями расы (уши, рога, клыки, борода). */
public class NpcRenderer extends HumanoidMobRenderer<RaceNpcEntity, HumanoidModel<RaceNpcEntity>> {

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new Features(this));
    }

    @Override
    protected void scale(RaceNpcEntity e, PoseStack pose, float pt) {
        float s = e.npcType().scale();
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(RaceNpcEntity e) {
        var craft = e.craft();
        if (e.npcType().isArtisan() && craft != null) {
            return LitRPG.id("textures/entity/npc/artisan/" + e.npcType().key.substring(0, e.npcType().key.indexOf('_')) + "_" + craft.key + ".png");
        }
        return LitRPG.id("textures/entity/npc/" + e.npcType().key + ".png");
    }

    /** Детали расы поверх головы. */
    static class Features extends RenderLayer<RaceNpcEntity, HumanoidModel<RaceNpcEntity>> {
        Features(RenderLayerParent<RaceNpcEntity, HumanoidModel<RaceNpcEntity>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, RaceNpcEntity e,
                           float limb, float amount, float pt, float age, float yaw, float pitch) {
            Race race = e.npcType().race;
            String[] parts = RaceLook.partsFor(race);
            if (parts.length == 0 || e.isInvisible()) return;
            pose.pushPose();
            getParentModel().getHead().translateAndRotate(pose);
            var vc = buffers.getBuffer(RenderType.entityCutoutNoCull(RaceLook.TEXTURE));
            int overlay = LivingEntityRenderer.getOverlayCoords(e, 0f);
            for (String p : parts) RaceLook.part(p).render(pose, vc, light, overlay);
            pose.popPose();
        }
    }
}
