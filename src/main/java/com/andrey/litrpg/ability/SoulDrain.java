package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * «Вытянуть душу» Призывателя: душа цели по нити переходит к нему — каждые три секунды
 * одна сфера-душа в запас, а умершая под нитью цель отдаёт ещё две.
 */
public final class SoulDrain {
    private static final int SOUL = 0xC0B080FF;

    private SoulDrain() {}

    static void register() {
        Special.put("sm_drain", SoulDrain::drain);
    }

    private static boolean drain(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float tick = Kit.power(s, a) * 2f / secs;
        Fx.tether(sl, p, t, 0.12, secs * 20, SOUL);
        Fx.mark(sl, t, 0.5, 0, secs * 20, SOUL);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!p.isAlive() || t.distanceTo(p) > a.radius + 6) return false;
            if (!t.isAlive()) {
                s.setResource(s.getResource() + 2);
                StatsManager.syncMana(p);
                Fx.beam(sl, t.position().add(0, 1, 0), p.position().add(0, 1.2, 0), 0.3, 10, SOUL);
                return false;
            }
            float before = t.getHealth();
            Kit.dmg(p, a, t, tick);
            p.heal(Math.max(0, before - t.getHealth()) * 0.3f);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, t.getX(), t.getY() + 1, t.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
            if (i % 3 == 2) {
                s.setResource(s.getResource() + 1);
                StatsManager.syncMana(p);
                Kit.sound(sl, p.position(), SoundEvents.SOUL_ESCAPE, 1f, 1.4f);
            }
            return true;
        });
        return true;
    }
}
