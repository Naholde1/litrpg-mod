package com.andrey.litrpg.client.render;

import com.andrey.litrpg.entity.LichEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LichRenderer extends GoblinRenderer<LichEntity> {
    public LichRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "lich", 1.55f);
    }
}
