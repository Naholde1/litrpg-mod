/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import java.util.UUID;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class Minibosses {
    private static final UUID KB_ID = UUID.fromString("0d1f6a52-77b0-4c4e-a1c2-9b3e5d7f2202");

    private Minibosses() {
    }

    static void setup(Mob m, Layout.Mob mo, Layout.Stage s) {
        int inf = -1;
        switch (mo.role()) {
            case "brute":
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, inf, 1, false, false));
                knockback(m, 0.8);
                break;
            case "swift":
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, inf, 1, false, false));
                break;
            case "guard":
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, inf, 1, false, false));
                knockback(m, (double)1.0F);
                break;
            case "shade":
                m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, inf, 0, false, false));
                break;
            case "giant":
                knockback(m, (double)1.0F);
                m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, inf, 2, false, false));
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, inf, 0, false, false));
        }

        if (!mo.name().isEmpty()) {
            // именной — мини-хранитель: у роли появляется свой приём (MiniFights)
            m.getPersistentData().putString(MiniFights.ROLE, mo.role().isEmpty() ? "giant" : mo.role());
            knockback(m, (double)1.0F);
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, inf, 0, false, false));
        }

    }

    private static void knockback(Mob m, double v) {
        AttributeInstance a = m.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (a != null) {
            a.removeModifier(KB_ID);
            a.addPermanentModifier(new AttributeModifier(KB_ID, "litrpg_role", v, Operation.ADDITION));
        }
    }
}
