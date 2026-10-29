package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Пиромант: жар копится, пока не рванёт.
 * <p>
 * Огонь оставляет на врагах жар — слои до пяти. Пятый слой взрывается: урон по самому врагу
 * и по всем рядом, жар сброшен. Горящая земля, цепь огня и выдох добавляют жар. Клеймо
 * огня каждую секунду поджигает всех вокруг помеченного. Огненный шторм — метеоры, которые
 * сначала отмечаются на полу. Пекло бьёт сильнее за каждого горящего врага.
 */
public final class Pyromancer {
    private static final int FIRE = 0xE0FF7020;
    private static final int EMBER = 0xD0FFB040;
    private static final String HEAT = "litrpg_py_heat";

    private Pyromancer() {}

    static void register() {
        Special.put("py_burn", Pyromancer::ground);
        Special.put("py_chain", Pyromancer::chain);
        Special.put("py_cone", Pyromancer::exhale);
        Special.put("py_blast", Pyromancer::blast);
        Special.put("py_mark", Pyromancer::brand);
        Special.put("py_storm", Pyromancer::storm);
        Special.put("py_inferno", Pyromancer::inferno);
    }

    /** Добавить слой жара; на пятом — взрыв по врагу и соседям. */
    private static void heat(ServerPlayer p, Ability a, LivingEntity e, float boom) {
        int st = (int) Kit.value(e, HEAT, 0) + 1;
        e.setSecondsOnFire(4);
        if (st < 5) {
            Kit.until(e, HEAT, st, 160);
            return;
        }
        Kit.clear(e, HEAT);
        ServerLevel sl = p.serverLevel();
        Fx.sphere(sl, e.position().add(0, 1, 0), 0.4, 3, 10, FIRE);
        Fx.burst(sl, e.position().add(0, 1, 0), 3, 14, 10, EMBER);
        sl.sendParticles(ParticleTypes.EXPLOSION, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
        Kit.sound(sl, e.position(), SoundEvents.GENERIC_EXPLODE, 1f, 1.3f);
        for (LivingEntity n : Kit.enemiesIn(p, e.position(), 3)) Kit.dmg(p, a, n, n == e ? boom : boom * 0.5f);
    }

    private static float boom(PlayerStats s, Ability a) {
        return AbilityHandler.base(s, a) * 1.2f;
    }

    /** Горящая земля в точке прицела: каждую секунду урон и слой жара тем, кто в огне. */
    private static boolean ground(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 22);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.25f;
        Fx.disc(sl, c, a.radius, 0, life, 0x60FF6010);
        Fx.runes(sl, c, a.radius, 5, life, FIRE);
        Kit.sound(sl, c, SoundEvents.FIRECHARGE_USE, 1.2f, 0.7f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, 25, a.radius / 2, 0.1, a.radius / 2, 0.02);
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, tick);
                heat(p, a, e, boom(s, a));
            }
            return true;
        });
        return true;
    }

    /** Цепь огня: молния пламени перескакивает с врага на врага, каждому — слой жара. */
    private static boolean chain(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity first = Kit.enemy(p, a.radius + 10);
        if (first == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> done = new ArrayList<>();
        LivingEntity cur = first;
        Vec3 from = p.getEyePosition().subtract(0, 0.3, 0);
        float dmg = Kit.power(s, a);
        for (int i = 0; i < Math.max(2, a.count) && cur != null; i++) {
            Vec3 at = cur.position().add(0, cur.getBbHeight() * 0.6, 0);
            Fx.bolt(sl, from, at, 0.16, 10, FIRE);
            Kit.dmg(p, a, cur, dmg * (1f - 0.12f * i));
            heat(p, a, cur, boom(s, a));
            done.add(cur);
            from = at;
            LivingEntity next = null;
            double best = a.radius * a.radius;
            for (LivingEntity e : Kit.enemiesIn(p, cur.position(), a.radius)) {
                if (done.contains(e)) continue;
                double d = e.distanceToSqr(cur);
                if (d < best) {
                    best = d;
                    next = e;
                }
            }
            cur = next;
        }
        Kit.sound(sl, first.position(), SoundEvents.BLAZE_SHOOT, 1.2f, 1.2f);
        return true;
    }

    /** Выдох: струя пламени конусом — урон и слой жара всем внутри. */
    private static boolean exhale(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        float dmg = Kit.power(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            Vec3 d = e.position().subtract(p.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1 && d.normalize().dot(dir) < 0.6) continue;
            Kit.dmg(p, a, e, dmg);
            heat(p, a, e, boom(s, a));
        }
        Fx.cone(sl, p.position(), dir, a.radius, 32, 8, 0x80FF7020);
        Fx.wave(sl, p.position(), dir, a.radius, 2.5, 1.6, 10, FIRE);
        for (int k = 1; k <= 6; k++) {
            Vec3 at = p.position().add(dir.scale(a.radius * k / 6.0)).add(0, 1, 0);
            sl.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 8, 0.3 * k / 2.0, 0.3, 0.3 * k / 2.0, 0.03);
        }
        Kit.sound(sl, p.position(), SoundEvents.BLAZE_SHOOT, 1.4f, 0.6f);
        return true;
    }

    /** Взрыв: волна жара вокруг — урон, отбрасывание и слой жара. */
    private static boolean blast(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            Kit.dmg(p, a, e, dmg);
            heat(p, a, e, boom(s, a));
            Kit.push(e, p.position(), 1.1, 0.3);
        }
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 12, FIRE);
        Fx.ring(sl, p.position(), 0.5, a.radius, 12, EMBER, 0.6f);
        Fx.burst(sl, p.position().add(0, 1, 0), a.radius, 18, 10, EMBER);
        Kit.sound(sl, p.position(), SoundEvents.GENERIC_EXPLODE, 1.2f, 0.9f);
        return true;
    }

    /** Клеймо огня: каждую секунду помеченный поджигает всех в 3 блоках и добавляет им жар. */
    private static boolean brand(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.2f;
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        Fx.mark(sl, t, 0.7, 1, life, FIRE);
        Fx.spiral(sl, t, 0.8, t.getBbHeight(), 2, life, EMBER);
        Scheduler.repeat(20, 20, life / 20, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, t.position(), 3)) {
                Kit.dmg(p, a, e, tick);
                heat(p, a, e, boom(s, a));
            }
            Fx.ring(sl, t.position(), 0.3, 3, 10, FIRE);
            return true;
        });
        Kit.sound(sl, t.position(), SoundEvents.FIRECHARGE_USE, 1f, 1.4f);
        return true;
    }

    /** Огненный шторм: в круге у точки прицела падают метеоры — каждый сначала отмечается на полу. */
    private static boolean storm(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 26);
        int life = Kit.dur(s, a);
        float dmg = Kit.power(s, a) * 0.45f;
        Fx.runes(sl, c, a.radius, 7, life + 20, FIRE);
        Scheduler.repeat(0, 10, life / 10, i -> {
            if (!p.isAlive()) return false;
            double ang = sl.random.nextDouble() * Math.PI * 2, d = Math.sqrt(sl.random.nextDouble()) * a.radius;
            Vec3 at = c.add(Math.cos(ang) * d, 0, Math.sin(ang) * d);
            Fx.disc(sl, at, 2.2, 0, 16, 0xA0FF5010);
            Scheduler.later(16, () -> {
                Fx.beam(sl, at.add(4, 14, 2), at, 0.7, 6, FIRE);
                Fx.sphere(sl, at.add(0, 0.5, 0), 0.3, 2.4, 10, EMBER);
                sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 10, 0.6, 0.2, 0.6, 0);
                Kit.sound(sl, at, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.2f);
                for (LivingEntity e : Kit.enemiesIn(p, at, 2.4)) {
                    Kit.dmg(p, a, e, dmg);
                    heat(p, a, e, boom(s, a));
                }
            });
            return true;
        });
        return true;
    }

    /** Пекло: огонь по всем вокруг — на 20% сильнее за каждого уже горящего врага (до +100%). */
    private static boolean inferno(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> list = Kit.enemiesIn(p, p.position(), a.radius);
        long burning = list.stream().filter(e -> e.isOnFire() || Kit.active(e, HEAT)).count();
        float dmg = Kit.power(s, a) * (1f + 0.2f * Math.min(5, burning));
        for (LivingEntity e : list) {
            Kit.dmg(p, a, e, dmg);
            heat(p, a, e, boom(s, a));
            Fx.pillar(sl, e.position(), 0.8, 5, 16, FIRE);
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 18, FIRE, 1f);
        Fx.runes(sl, p.position(), 5, 8, 30, EMBER);
        Kit.sound(sl, p.position(), SoundEvents.BLAZE_DEATH, 1f, 0.6f);
        if (burning > 0) Kit.msg(p, "Пекло: горящих " + burning + " — урон +" + Math.min(5, burning) * 20 + "%", ChatFormatting.GOLD);
        return true;
    }
}
