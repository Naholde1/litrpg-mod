package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> SHADOW_REALM = ResourceKey.create(Registries.DIMENSION, LitRPG.id("shadow_realm"));
}
