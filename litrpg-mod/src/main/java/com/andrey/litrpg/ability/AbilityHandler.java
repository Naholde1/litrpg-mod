package com.andrey.litrpg.ability;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AbilityHandler {

    public static void tryUse(ServerPlayer player, Ability ability) {
        PlayerStats s = StatsManager.get(player);
        long now = player.level().getGameTime();

        if (s.getLevel() < ability.unlockLevel) {
            player.displayClientMessage(Component.literal("Навык откроется на уровне " + ability.unlockLevel)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        long end = s.getCooldownEnd(ability);
        if (now < end) {
            player.displayClientMessage(Component.literal("Перезарядка: " + String.format("%.1f", (end - now) / 20.0) + " с")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!player.isCreative() && s.getMana() < ability.manaCost) {
            player.displayClientMessage(Component.literal("Недостаточно маны!").withStyle(ChatFormatting.BLUE), true);
            return;
        }

        boolean ok = switch (ability) {
            case DASH -> dash(player);
            case MANA_BOLT -> manaBolt(player, s);
            case HEAL -> heal(player, s);
            case SLAM -> slam(player, s);
            case BERSERK -> berserk(player, s);
        };

        if (ok) {
            if (!player.isCreative()) s.setMana(s.getMana() - ability.manaCost);
            s.setCooldownEnd(ability, now + ability.cooldown);
            StatsManager.sync(player);
        }
    }

    private static boolean dash(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize().scale(1.9);
        player.setDeltaMovement(dir.x, 0.35, dir.z);
        player.hurtMarked = true;
        player.fallDistance = 0;
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.3, player.getZ(), 15, 0.3, 0.1, 0.3, 0.05);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0f, 1.4f);
        return true;
    }

    private static boolean manaBolt(ServerPlayer player, PlayerStats s) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        ManaBoltEntity bolt = new ManaBoltEntity(level, player, look.x, look.y, look.z);
        bolt.setPos(player.getX() + look.x * 0.8, player.getEyeY() - 0.15 + look.y * 0.8, player.getZ() + look.z * 0.8);
        bolt.setDamage(6.0f + s.getStat(Stat.INTELLIGENCE) * 0.6f + s.getLevel() * 0.2f);
        level.addFreshEntity(bolt);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.6f);
        return true;
    }

    private static boolean heal(ServerPlayer player, PlayerStats s) {
        if (player.getHealth() >= player.getMaxHealth()) {
            player.displayClientMessage(Component.literal("Здоровье уже полное").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        float amount = 6.0f + s.getStat(Stat.INTELLIGENCE) * 0.5f;
        player.heal(amount);
        player.removeEffect(MobEffects.POISON);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2, player.getZ(), 8, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.6, 0.8, 0.6, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.2f);
        return true;
    }

    private static boolean slam(ServerPlayer player, PlayerStats s) {
        ServerLevel level = player.serverLevel();
        float damage = 6.0f + s.getStat(Stat.STRENGTH) * 0.5f + s.getLevel() * 0.25f;
        AABB box = player.getBoundingBox().inflate(5.0, 2.0, 5.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == player || e instanceof Player) continue;
            if (e instanceof TamableAnimal t && t.isOwnedBy(player)) continue;
            e.hurt(player.damageSources().playerAttack(player), damage);
            Vec3 push = e.position().subtract(player.position()).normalize().scale(0.9);
            e.push(push.x, 0.55, push.z);
            e.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 0.2, player.getZ(), 6, 2.0, 0.2, 2.0, 0.0);
        level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2, player.getZ(), 60, 3.0, 0.3, 3.0, 0.3);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.3f);
        return true;
    }

    private static boolean berserk(ServerPlayer player, PlayerStats s) {
        int duration = 200 + s.getStat(Stat.VITALITY) * 4;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.5, player.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}
