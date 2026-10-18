package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.Race;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Внешность рас: рост (дварфы ниже, орки крупнее) и детали на голове —
 * звериные и эльфийские уши, рога демонида, клыки орка, борода дварфа.
 * Работает поверх любого скина, видно и другим игрокам.
 */
public final class RaceLook {
    public static final ResourceLocation TEXTURE = LitRPG.id("textures/entity/race_features.png");
    private static ModelPart root;

    private RaceLook() {}

    public static float scale(Race r) {
        if (r == null) return 1f;
        return switch (r) {
            case DWARF -> 0.84f;
            case ORC -> 1.12f;
            case ELF -> 1.04f;
            case DEMONKIN -> 1.03f;
            default -> 1f;
        };
    }

    /** Ширина отдельно: дварфы коренастые, эльфы стройные. */
    public static float widthScale(Race r) {
        if (r == null) return 1f;
        return switch (r) {
            case DWARF -> 1.08f;
            case ORC -> 1.1f;
            case ELF -> 0.94f;
            default -> 1f;
        };
    }

    public static ModelPart part(String name) {
        return model().getChild(name);
    }

    public static String[] partsFor(Race r) {
        if (r == null) return new String[0];
        return switch (r) {
            case BEASTKIN -> new String[]{"beast_left", "beast_right"};
            case ELF -> new String[]{"elf_left", "elf_right"};
            case DEMONKIN -> new String[]{"horn_left", "horn_right"};
            case ORC -> new String[]{"tusk_left", "tusk_right"};
            case DWARF -> new String[]{"beard"};
            default -> new String[0];
        };
    }

    static ModelPart model() {
        if (root == null) root = create().bakeRoot();
        return root;
    }

    private static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        // звериные уши (мех + розовое внутри)
        p.addOrReplaceChild("beast_left", CubeListBuilder.create().texOffs(0, 0).addBox(-1.5f, -4f, -0.5f, 3, 4, 1),
                PartPose.offsetAndRotation(-3f, -8f, 0f, 0f, 0f, -0.35f));
        p.addOrReplaceChild("beast_right", CubeListBuilder.create().texOffs(0, 0).mirror().addBox(-1.5f, -4f, -0.5f, 3, 4, 1),
                PartPose.offsetAndRotation(3f, -8f, 0f, 0f, 0f, 0.35f));
        // эльфийские острые уши
        p.addOrReplaceChild("elf_left", CubeListBuilder.create().texOffs(0, 8).addBox(-0.5f, -4f, 0f, 1, 4, 2),
                PartPose.offsetAndRotation(-4.3f, -3f, 0.5f, -0.5f, 0f, -0.35f));
        p.addOrReplaceChild("elf_right", CubeListBuilder.create().texOffs(0, 8).mirror().addBox(-0.5f, -4f, 0f, 1, 4, 2),
                PartPose.offsetAndRotation(4.3f, -3f, 0.5f, -0.5f, 0f, 0.35f));
        // рога демонида: основание и загнутый кончик
        PartDefinition hl = p.addOrReplaceChild("horn_left", CubeListBuilder.create().texOffs(16, 0).addBox(-1f, -3f, -1f, 2, 3, 2),
                PartPose.offsetAndRotation(-2.5f, -8f, -1f, -0.3f, 0f, -0.25f));
        hl.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 6).addBox(-0.5f, -3f, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(0f, -2.5f, 0f, -0.6f, 0f, 0f));
        PartDefinition hr = p.addOrReplaceChild("horn_right", CubeListBuilder.create().texOffs(16, 0).mirror().addBox(-1f, -3f, -1f, 2, 3, 2),
                PartPose.offsetAndRotation(2.5f, -8f, -1f, -0.3f, 0f, 0.25f));
        hr.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(16, 6).addBox(-0.5f, -3f, -0.5f, 1, 3, 1),
                PartPose.offsetAndRotation(0f, -2.5f, 0f, -0.6f, 0f, 0f));
        // клыки орка
        p.addOrReplaceChild("tusk_left", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5f, -2f, -0.5f, 1, 2, 1),
                PartPose.offsetAndRotation(-1.8f, -1.2f, -4.3f, 0.2f, 0f, 0f));
        p.addOrReplaceChild("tusk_right", CubeListBuilder.create().texOffs(28, 0).addBox(-0.5f, -2f, -0.5f, 1, 2, 1),
                PartPose.offsetAndRotation(1.8f, -1.2f, -4.3f, 0.2f, 0f, 0f));
        // борода дварфа
        p.addOrReplaceChild("beard", CubeListBuilder.create().texOffs(0, 16).addBox(-4f, 0f, -1f, 8, 5, 2),
                PartPose.offsetAndRotation(0f, -2f, -4f, 0.15f, 0f, 0f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Слой с деталями расы, крепится к голове модели. */
    public static class FeatureLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        public FeatureLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                           float limb, float limbAmount, float pt, float age, float yaw, float pitch) {
            Race race = ClientData.raceOf(player.getUUID());
            if (race == null || race == Race.HUMAN || player.isInvisible()) return;
            ModelPart m = model();
            pose.pushPose();
            getParentModel().getHead().translateAndRotate(pose);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0f);
            for (String part : partsFor(race)) m.getChild(part).render(pose, vc, light, overlay);
            pose.popPose();
        }

    }

    @Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Setup {
        @SubscribeEvent
        public static void addLayers(EntityRenderersEvent.AddLayers event) {
            for (String skin : event.getSkins()) {
                if (event.getSkin(skin) instanceof PlayerRenderer pr) pr.addLayer(new FeatureLayer(pr));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
    public static final class Scaling {
        @SubscribeEvent
        public static void pre(RenderPlayerEvent.Pre event) {
            Race race = ClientData.raceOf(event.getEntity().getUUID());
            float s = scale(race);
            float w = widthScale(race);
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(s * w, s, s * w);
        }

        @SubscribeEvent
        public static void post(RenderPlayerEvent.Post event) {
            event.getPoseStack().popPose();
        }
    }
}
