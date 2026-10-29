package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Гравитон: собирает толпу в точку, чтобы по ней ударили остальные.
 * <p>
 * Воронка тянет всех к центру несколько секунд, пресс прибивает к полу (прыгать нельзя,
 * удары по придавленным больнее), схлопывание сначала втягивает, потом сминает.
 * «Подбросить» — вверх, а через миг тяжесть возвращается и роняет всех обратно с уроном.
 * Сингулярность останавливает всё в круге. Отталкивание — волна тяжести вперёд.
 */
public final class Graviton {
    private static final int VOID = 0xD07050E0;
    private static final int DEEP = 0xC03020A0;

    private Graviton() {}

    static void register() {
        Special.put("gv_vortex", Graviton::vortex);
        Special.put("gv_press", Graviton::press);
        Special.put("gv_collapse", Graviton::collapse);
        Special.put("gv_launch", Graviton::launch);
        Special.put("gv_singular", Graviton::singularity);
        Special.put("gv_push", Graviton::repulse);
    }

    /** Воронка в точке прицела: несколько секунд тянет всех к центру и понемногу мнёт. */
    private static boolean vortex(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 18);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.25f;
        Fx.runes(sl, c, a.radius, 9, life, VOID);
        Fx.spiralAt(sl, c, a.radius * 0.6, 3, 4, life, DEEP);
        Kit.sound(sl, c, SoundEvents.PORTAL_TRIGGER, 0.7f, 1.6f);
        Scheduler.repeat(0, 4, life / 4, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius + 1)) {
                Kit.pull(e, c, 0.32);
                if (i % 5 == 0) {
                    Kit.dmg(p, a, e, tick);
                    AbilityHandler.applyFoe(a, e, p);
                }
            }
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.5, c.z, 12, a.radius / 2, 0.4, a.radius / 2, 0.02);
            return true;
        });
        return true;
    }

    /** Пресс: в круге всё весит втрое — враги не могут прыгать, медленнее и получают на 20% больше. */
    private static boolean press(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 16);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.5f;
        Fx.disc(sl, c, a.radius, 0, life, 0x503020A0);
        Fx.runes(sl, c, a.radius, 4, life, VOID);
        Kit.sound(sl, c, SoundEvents.ANVIL_LAND, 1f, 0.4f);
        Scheduler.repeat(0, 5, life / 5, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Vec3 v = e.getDeltaMovement();
                if (v.y > 0) e.setDeltaMovement(v.x * 0.5, -0.3, v.z * 0.5);
                e.hurtMarked = true;
                Kit.until(e, Kit.TAKEN, 1.2f, 10);
                if (i % 4 == 0) {
                    AbilityHandler.applyFoe(a, e, p);
                    Kit.dmg(p, a, e, tick);
                }
            }
            if (i % 4 == 0) sl.sendParticles(ParticleTypes.SQUID_INK, c.x, c.y + 2.5, c.z, 10, a.radius / 2, 0.1, a.radius / 2, -0.1);
            return true;
        });
        return true;
    }

    /** Схлопывание: метка, 1,5 с втягивания — и удар по всем, кто остался внутри. */
    private static boolean collapse(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 20);
        float dmg = Kit.power(s, a);
        Fx.disc(sl, c, a.radius, 0, 30, 0x907050E0);
        Fx.sphere(sl, c.add(0, 1, 0), a.radius, 0.3, 30, DEEP);
        Kit.sound(sl, c, SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.5f);
        Scheduler.repeat(0, 3, 10, i -> {
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius + 2)) Kit.pull(e, c, 0.45);
            return p.isAlive();
        });
        Scheduler.later(30, () -> {
            if (!p.isAlive()) return;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, dmg);
                AbilityHandler.applyFoe(a, e, p);
            }
            Fx.burst(sl, c.add(0, 1, 0), a.radius, 20, 12, VOID);
            Fx.ring(sl, c, a.radius, 0.3, 10, VOID, 0.8f);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 3, 0.8, 0.3, 0.8, 0);
            Kit.sound(sl, c, SoundEvents.GENERIC_EXPLODE, 1.3f, 0.6f);
        });
        return true;
    }

    /** Подбросить: тяжесть пропадает — враги взлетают; через миг возвращается втрое и роняет их с уроном. */
    private static boolean launch(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        var list = Kit.enemiesIn(p, p.position(), a.radius);
        for (LivingEntity e : list) {
            if (Kit.boss(e)) continue;
            e.setDeltaMovement(e.getDeltaMovement().x, 1.1, e.getDeltaMovement().z);
            e.hurtMarked = true;
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 10, VOID, 0.5f);
        Kit.sound(sl, p.position(), SoundEvents.SHULKER_SHOOT, 1.2f, 0.6f);
        Scheduler.later(14, () -> {
            for (LivingEntity e : list) {
                if (!e.isAlive()) continue;
                if (!Kit.boss(e)) {
                    e.setDeltaMovement(0, -2.2, 0);
                    e.hurtMarked = true;
                }
                Fx.beam(sl, e.position().add(0, 3, 0), e.position(), 0.4, 6, DEEP);
            }
        });
        Scheduler.later(20, () -> {
            if (!p.isAlive()) return;
            for (LivingEntity e : list) {
                if (!e.isAlive()) continue;
                Kit.dmg(p, a, e, dmg);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                Fx.ring(sl, e.position(), 0.2, 1.8, 8, VOID);
            }
            Kit.sound(sl, p.position(), SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
        });
        return true;
    }

    /** Сингулярность в точке прицела: всё в круге застывает; хранитель — замедлен и прижат. */
    private static boolean singularity(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 20);
        int life = Kit.dur(s, a);
        var list = Kit.enemiesIn(p, c, a.radius);
        if (list.isEmpty()) return AbilityHandler.noTarget(p);
        for (LivingEntity e : list) {
            Kit.stun(e, life);
            Kit.pull(e, c, 0.5);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, life, 4));
            Fx.chains(sl, p, e, 0.3, 10, VOID);
        }
        Fx.sphere(sl, c.add(0, 1, 0), 0.8, 0.8, life, 0xF0100020);
        Fx.sphere(sl, c.add(0, 1, 0), a.radius, a.radius * 0.9, life, 0x403020A0);
        Fx.runes(sl, c, a.radius, 11, life, VOID);
        Kit.sound(sl, c, SoundEvents.END_PORTAL_SPAWN, 0.6f, 1.6f);
        return true;
    }

    /** Отталкивание: волна тяжести вперёд — сносит всех на пути. */
    private static boolean repulse(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        float dmg = Kit.power(s, a);
        Fx.wave(sl, p.position(), dir, a.radius, 2.5, 2.2, 12, VOID);
        Kit.sound(sl, p.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.6f, 1.4f);
        for (LivingEntity e : Kit.enemiesIn(p, p.position().add(dir.scale(a.radius / 2)), a.radius / 2 + 1.5)) {
            Kit.dmg(p, a, e, dmg);
            Kit.push(e, p.position(), 1.5, 0.3);
        }
        return true;
    }
}
