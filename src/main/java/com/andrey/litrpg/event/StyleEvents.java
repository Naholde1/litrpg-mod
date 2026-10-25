package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.AbilityHandler;
import com.andrey.litrpg.ability.Stealth;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Наблюдение за стилем игры до 10 уровня: Система запоминает, чем игрок
 * занимается, и на 10 уровне предлагает подходящие классы.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class StyleEvents {

    private static void add(Player p, Style st, float amount) {
        if (p instanceof ServerPlayer sp) {
            PlayerStats s = StatsManager.get(sp);
            if (!s.hasClass()) s.addStyle(st, Math.max(1, Math.round(amount)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource src = event.getSource();
        float amount = Math.min(event.getAmount(), target.getHealth() + 4);

        if (target instanceof ServerPlayer victim && src.getEntity() instanceof LivingEntity && !(src.getEntity() instanceof Player)) {
            add(victim, Style.TANK, amount);
            return;
        }
        if (!(src.getEntity() instanceof ServerPlayer p) || target instanceof Player) return;
        if (src.getDirectEntity() instanceof AbstractArrow || src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
            add(p, Style.RANGED, amount);
        } else if (src.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) || src.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            add(p, Style.MAGIC, amount);
        } else {
            add(p, Style.MELEE, amount);
        }
        // с кем и как сражается
        boolean friends = false;
        for (ServerPlayer o : p.serverLevel().players()) if (o != p && o.distanceToSqr(p) < 32 * 32) friends = true;
        add(p, friends ? Style.PARTY : Style.SOLO, amount * (friends ? 0.6f : 0.3f));
        if (p.level().isNight() || p.level().getMaxLocalRawBrightness(p.blockPosition()) < 6) add(p, Style.NIGHT, amount * 0.5f);
        if (p.isInWater() || target.isInWater()) add(p, Style.WATER, amount * 0.6f);
        if (p.getHealth() < p.getMaxHealth() * 0.35f) add(p, Style.RISK, amount);
        if (p.isCrouching() || Stealth.isHidden(p) || p.isInvisible() || AbilityHandler.isBehind(p, target)) {
            add(p, Style.STEALTH, amount * 0.8f);
        }
        if (target.isOnFire()) add(p, Style.FIRE, amount * 0.7f);
    }

    @SubscribeEvent
    public static void onBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof Player p) add(p, Style.TANK, event.getBlockedDamage());
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        LivingEntity dead = event.getEntity();
        if (dead.getMobType() == MobType.UNDEAD) add(p, Style.UNDEAD, 6);
        if (dead instanceof Enemy && (p.isCrouching() || Stealth.isHidden(p))) add(p, Style.STEALTH, 5);
    }

    /** Лечение союзников (зовут навыки лечения) — главный признак поддержки. */
    public static void healed(ServerPlayer p, net.minecraft.world.entity.LivingEntity target, float amount) {
        if (amount <= 0) return;
        add(p, Style.SUPPORT, amount * (target == p ? 0.4f : 1.2f));
        if (target != p) add(p, Style.PARTY, amount * 0.3f);
    }
}
