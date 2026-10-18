package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Скрытность навыков (Дымовая шашка, Плащ теней…). В отличие от ванильной
 * невидимости, монстры вообще не могут выбрать скрытого игрока целью, даже
 * если он в броне. Первый удар из скрытности наносит x1.5 урона и снимает её.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Stealth {
    public static final String TAG = "litrpg_hidden_until";

    private Stealth() {}

    public static void hide(ServerPlayer p, int ticks) {
        p.getPersistentData().putLong(TAG, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticks, 0, false, false, true));
        // сбрасываем агрессию всех, кто уже охотится на игрока
        for (Mob m : p.level().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48), m -> m.getTarget() == p)) {
            m.setTarget(null);
            m.setLastHurtByMob(null);
            m.getNavigation().stop();
        }
    }

    public static boolean isHidden(Player p) {
        return p.getPersistentData().getLong(TAG) > p.level().getGameTime();
    }

    public static void reveal(ServerPlayer p) {
        if (!p.getPersistentData().contains(TAG)) return;
        p.getPersistentData().remove(TAG);
        p.removeEffect(MobEffects.INVISIBILITY);
        p.displayClientMessage(Component.literal("Скрытность раскрыта").withStyle(ChatFormatting.GRAY), true);
    }

    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player p && isHidden(p)) {
            event.setCanceled(true);
        }
    }

    /** Удар из скрытности: бонус к урону и выход из неё. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p && isHidden(p)) {
            boolean shadow = com.andrey.litrpg.stats.StatsManager.get(p).gear().hasSet(com.andrey.litrpg.gear.GearSet.SHADOW);
            event.setAmount(event.getAmount() * (shadow ? 2f : 1.5f));
            reveal(p);
        }
    }
}
