package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Музыка из файлов (sounds.json, tools/gen_music.py): litrpg:music.NAME. */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LitRPG.MODID);
    public static final Map<String, RegistryObject<SoundEvent>> MUSIC = new LinkedHashMap<>();

    static {
        for (String n : new String[]{"red", "jane_hoodtrap", "blud", "kurayami", "as_time_flies", "wutiwant", "next_opponent",
                "no_time_for_caution", "ecstasy_of_gold", "aura_of_glory", "fight_club", "fragment", "woops", "fear", "blue_lock"}) {
            MUSIC.put(n, SOUNDS.register("music." + n, () -> SoundEvent.createVariableRangeEvent(LitRPG.id("music." + n))));
        }
    }

    public static SoundEvent music(String name) {
        RegistryObject<SoundEvent> ro = MUSIC.get(name);
        return ro == null ? null : ro.get();
    }
}
