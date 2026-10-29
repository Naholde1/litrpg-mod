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
import net.minecraft.world.phys.Vec3;

/**
 * Источник: питает отряд не только здоровьем, но и ресурсом.
 * <p>
 * Колодец силы — место, где у своих восстанавливается и жизнь, и ресурс класса. «Отвести
 * силу» тянет силу из врага и раздаёт её ресурсом отряду. Разлив катится волной от
 * Источника. Капель лечит всё сильнее с каждой каплей. Половодье наполняет ресурс всем.
 */
public final class Wellspring {
    private static final int WATER = 0xC060C0FF;
    private static final int FOAM = 0xD0C0F0FF;

    private Wellspring() {}

    static void register() {
        Special.put("sr_well", Wellspring::well);
        Special.put("sr_drain", Wellspring::divert);
        Special.put("sr_surge", Wellspring::surge);
        Special.put("sr_drip", Wellspring::drip);
        Special.put("sr_tide", Wellspring::tide);
    }

    /** Доля ресурса класса союзнику-игроку (у ярости — в единицах). */
    private static void feed(LivingEntity e, double frac) {
        if (!(e instanceof ServerPlayer sp)) return;
        PlayerStats st = StatsManager.get(sp);
        if (st.resource().mirrorsHealth()) return;
        st.setResource(st.getResource() + Math.max(1, st.getMaxResource() * frac));
        StatsManager.syncMana(sp);
    }

    /** Колодец силы: круг на 14 с; свои внутри каждую секунду лечатся и получают 4% ресурса. */
    private static boolean well(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        float heal = AbilityHandler.base(s, a) * AbilityHandler.healMult(s) * 0.2f;
        Fx.runes(sl, c, a.radius, 6, life, WATER);
        Fx.disc(sl, c, a.radius, 0, life, 0x3060C0FF);
        Kit.sound(sl, c, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 1.5f, 1f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.alliesIn(p, c, a.radius)) {
                AbilityHandler.applySelf(a, e);
                Kit.heal(p, e, heal);
                feed(e, 0.04);
            }
            sl.sendParticles(ParticleTypes.BUBBLE_POP, c.x, c.y + 0.3, c.z, 20, a.radius / 2, 0.2, a.radius / 2, 0.05);
            return true;
        });
        return true;
    }

    /** Отвести силу: нить к врагу; каждую секунду урон и 3% ресурса всем своим в 16 блоках. */
    private static boolean divert(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float tick = Kit.power(s, a) * 2f / secs;
        Fx.tether(sl, p, t, 0.12, secs * 20, WATER);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive() || t.distanceTo(p) > a.radius + 4) return false;
            Kit.dmg(p, a, t, tick);
            for (LivingEntity e : Kit.alliesIn(p, p.position(), 16)) feed(e, 0.03);
            sl.sendParticles(ParticleTypes.DRIPPING_WATER, t.getX(), t.getY() + 1, t.getZ(), 4, 0.3, 0.4, 0.3, 0);
            return true;
        });
        return true;
    }

    /** Разлив: волна катится от Источника; кого накрыла — лечит и даёт 10% ресурса. */
    private static boolean surge(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        float heal = Dynamic.healAmount(s, a, 1f);
        Fx.ring(sl, c, 0.5, a.radius, 16, WATER, 1.2f);
        Kit.sound(sl, c, SoundEvents.GENERIC_SPLASH, 1.5f, 0.8f);
        java.util.Set<LivingEntity> done = new java.util.HashSet<>();
        Scheduler.repeat(0, 2, 8, i -> {
            double r = a.radius * (i + 1) / 8.0;
            for (LivingEntity e : Kit.alliesIn(p, c, r)) {
                if (!done.add(e)) continue;
                Kit.heal(p, e, heal);
                feed(e, 0.1);
                Fx.pillar(sl, e.position(), 0.5, 2.2, 10, FOAM);
            }
            return p.isAlive();
        });
        return true;
    }

    /** Капель: лечит каждую секунду, и каждая следующая капля на 15% сильнее предыдущей. */
    private static boolean drip(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        int secs = Math.max(2, Kit.dur(s, a) / 20);
        float base = Dynamic.healAmount(s, a, 1f) / (secs * 1.7f);
        ServerLevel sl = p.serverLevel();
        Fx.mark(sl, t, 0.35, 3, secs * 20, WATER);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            Kit.heal(p, t, base * (1f + 0.15f * i));
            sl.sendParticles(ParticleTypes.DRIPPING_WATER, t.getX(), t.getY() + t.getBbHeight() + 0.5, t.getZ(), 4, 0.2, 0, 0.2, 0);
            return true;
        });
        return true;
    }

    /** Половодье: бафы из данных и половина ресурса класса всем вокруг. */
    private static boolean tide(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            AbilityHandler.applySelf(a, e);
            feed(e, 0.5);
            Fx.spiral(sl, e, 0.8, 2.4, 3, 24, WATER);
        }
        Fx.wave(sl, p.position(), AbilityHandler.flatLook(p), a.radius, a.radius, 2.5, 20, FOAM);
        Fx.ring(sl, p.position(), 0.5, a.radius, 20, WATER, 1f);
        Kit.sound(sl, p.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.5f, 0.8f);
        return true;
    }
}
