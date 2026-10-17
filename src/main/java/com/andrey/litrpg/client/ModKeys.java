package com.andrey.litrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final String CATEGORY = "key.categories.litrpg";

    public static final KeyMapping CHARACTER = new KeyMapping("key.litrpg.character", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping WORLD_MAP = new KeyMapping("key.litrpg.world_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping MINIMAP = new KeyMapping("key.litrpg.minimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping[] ABILITIES = {
            new KeyMapping("key.litrpg.ability1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.litrpg.ability2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.litrpg.ability3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.litrpg.ability4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.litrpg.ability5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY),
    };
}
