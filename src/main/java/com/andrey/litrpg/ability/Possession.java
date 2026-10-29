package com.andrey.litrpg.ability;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Resource;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.UUID;

/**
 * Вселение Духа. Сам Дух почти не бьёт — он входит в монстра и дерётся его телом:
 * садится в него, пропадает из виду и наводит его на то, на что смотрит сам.
 * <p>
 * Держится, пока есть шкала одержимости: три в секунду. Кончилась шкала, погиб носитель
 * или Дух сошёл сам — вселение обрывается.
 */
public final class Possession {
    /** В ком сидит Дух (UUID носителя). */
    public static final String TAG = "litrpg_possess";
    /** Сколько шкалы уходит в секунду. */
    public static final double DRAIN = 3;
    /** На сколько далеко Дух наводит носителя. */
    private static final double AIM = 24;

    private Possession() {}

    /** Войти в монстра, которого уже подчинили. */
    static void start(ServerPlayer p, Mob host, int ticks) {
        p.getPersistentData().putUUID(TAG, host.getUUID());
        p.startRiding(host, true);
        host.setCustomName(Component.literal("Одержимый").withStyle(ChatFormatting.DARK_PURPLE));
        host.setCustomNameVisible(true);
        p.serverLevel().sendParticles(ParticleTypes.SOUL, host.getX(), host.getY() + 1, host.getZ(), 40, 0.4, 0.7, 0.4, 0.03);
        p.level().playSound(null, host.getX(), host.getY(), host.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.4f, 0.6f);
        p.displayClientMessage(Component.literal("Вы внутри. Смотрите на врага — носитель пойдёт за ним.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    /** Раз в секунду: тратим шкалу, наводим носителя, проверяем, жив ли он. */
    public static void tick(ServerPlayer p, PlayerStats s) {
        if (!p.getPersistentData().hasUUID(TAG)) return;
        UUID id = p.getPersistentData().getUUID(TAG);
        Entity raw = p.level() instanceof ServerLevel sl ? sl.getEntity(id) : null;
        if (!(raw instanceof Mob host) || !host.isAlive()) {
            end(p, "Носитель пал — вас выбросило наружу.");
            return;
        }
        if (s.resource() != Resource.FORM) {
            end(p, null);
            return;
        }
        double left = s.getResource() - DRAIN;
        if (left <= 0) {
            s.setResource(0);
            end(p, "Одержимость иссякла.");
            return;
        }
        s.setResource(left);
        if (p.getVehicle() != host) p.startRiding(host, true);
        // Дух наводит тело: носитель идёт на то, на что смотрит сам Дух
        LivingEntity look = AbilityHandler.lookTarget(p, AIM);
        if (look != null && look != p && AbilityHandler.isEnemy(p, look) && Minions.ownerOf(look) == null) {
            host.setTarget(look);
        }
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, host.getX(), host.getY() + host.getBbHeight(), host.getZ(),
                    6, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** Выйти из носителя. */
    public static void end(ServerPlayer p, String why) {
        if (!p.getPersistentData().hasUUID(TAG)) return;
        UUID id = p.getPersistentData().getUUID(TAG);
        p.getPersistentData().remove(TAG);
        p.stopRiding();
        if (p.level() instanceof ServerLevel sl && sl.getEntity(id) instanceof Mob host) {
            host.setCustomNameVisible(false);
            sl.sendParticles(ParticleTypes.SOUL, host.getX(), host.getY() + 1, host.getZ(), 25, 0.4, 0.6, 0.4, 0.03);
        }
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1f, 1.4f);
        }
        StatsManager.sync(p);
    }

    /** Сидит ли Дух в носителе: пока сидит, шкала только тратится. */
    public static boolean active(ServerPlayer p) {
        return p.getPersistentData().hasUUID(TAG);
    }

    /** Для отладки: в ком сидит Дух. */
    public static String debug(ServerPlayer p) {
        return p.getPersistentData().hasUUID(TAG) ? p.getPersistentData().getUUID(TAG).toString().substring(0, 8) : "-";
    }
}
