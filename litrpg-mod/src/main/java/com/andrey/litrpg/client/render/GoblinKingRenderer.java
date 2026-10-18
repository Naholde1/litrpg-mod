package com.andrey.litrpg.client.render;

import com.andrey.litrpg.entity.GoblinKingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GoblinKingRenderer extends GoblinRenderer<GoblinKingEntity> {
    public GoblinKingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "goblin_king", 1.85f);
    }
}
