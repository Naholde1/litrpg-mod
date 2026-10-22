package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Эффекты отряда (как баффы и дебаффы в MMO): каждый уровень — 10%.
 * Воодушевление — больше урона; Заслон — меньше получаемого урона;
 * Уязвимость (на враге) — враг получает больше урона; Сосредоточенность — навыки быстрее перезаряжаются.
 */
public final class ModEffects {
    private ModEffects() {}

    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, LitRPG.MODID);

    public static final RegistryObject<MobEffect> MIGHT = EFFECTS.register("might", () -> new Simple(MobEffectCategory.BENEFICIAL, 0xE74C3C));
    public static final RegistryObject<MobEffect> BULWARK = EFFECTS.register("bulwark", () -> new Simple(MobEffectCategory.BENEFICIAL, 0x5DADE2));
    public static final RegistryObject<MobEffect> VULNERABILITY = EFFECTS.register("vulnerability", () -> new Simple(MobEffectCategory.HARMFUL, 0x8E44AD));
    public static final RegistryObject<MobEffect> FOCUS = EFFECTS.register("focus", () -> new Simple(MobEffectCategory.BENEFICIAL, 0xF1C40F));

    /** Уровень эффекта (1, 2…) или 0, если его нет. */
    public static int level(LivingEntity e, RegistryObject<MobEffect> eff) {
        var inst = e.getEffect(eff.get());
        return inst == null ? 0 : inst.getAmplifier() + 1;
    }

    static final class Simple extends MobEffect {
        Simple(MobEffectCategory cat, int color) {
            super(cat, color);
        }
    }
}
