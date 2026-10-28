package com.andrey.litrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class ModKeys {
    public static final String CATEGORY = "key.categories.litrpg";

    public static final KeyMapping CHARACTER = new KeyMapping("key.litrpg.character", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping WORLD_MAP = new KeyMapping("key.litrpg.world_map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);
    public static final KeyMapping MINIMAP = new KeyMapping("key.litrpg.minimap", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    /** Рывок — всегда под рукой, отдельно от панели. */
    public static final KeyMapping DASH = new KeyMapping("key.litrpg.dash", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    /** Восемь ячеек панели навыков. */
    public static final KeyMapping[] ABILITIES = {
            new KeyMapping("key.litrpg.ability1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY),
            new KeyMapping("key.litrpg.ability2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, CATEGORY),
            new KeyMapping("key.litrpg.ability3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY),
            new KeyMapping("key.litrpg.ability4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.litrpg.ability5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY),
            new KeyMapping("key.litrpg.ability6", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY),
    };
    /** Ctrl+1…5 — выбрать союзника из рамок группы (таргетные лечения и щиты летят в него). Ctrl+` — снять выбор. */
    public static final KeyMapping[] FOCUS = new KeyMapping[6];

    static {
        for (int i = 0; i < 5; i++) {
            FOCUS[i] = new KeyMapping("key.litrpg.focus" + (i + 1), KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + i, CATEGORY);
        }
        FOCUS[5] = new KeyMapping("key.litrpg.focus_clear", KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, CATEGORY);
    }
}
