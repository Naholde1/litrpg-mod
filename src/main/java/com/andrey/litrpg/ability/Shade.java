package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * Тень: со спины и из темноты.
 * <p>
 * «Из темноты» переносит за спину цели — удар, которого не ждут, в полтора раза больнее. Веер ножей — пять настоящих лезвий веером: каждое бьёт первого
 * на своём пути. «Перерезать» со спины добивает. Ночь — круг тьмы: враги слепнут и бьют
 * слабее, а Тень внутри сильнее. Дым сбивает с врагов их цели.
 */
public final class Shade {
    private static final int DUSK = 0xD06040A0;
    private static final int EDGE = 0xE0C0B0FF;

    private Shade() {}

    static void register() {
        Special.put("sh_ambush", Shade::ambush);
        Special.put("sh_throw", Shade::knives);
        Special.put("sh_execute", Shade::cut);
        Special.put("sh_night", Shade::night);
        Special.put("sh_smoke", Shade::smoke);
        Special.put("sh_mark", Shade::mark);
    }

    /** Цель не видит Тень: стоит спиной или занята другим. */
    private static boolean unaware(ServerPlayer p, LivingEntity t) {
        boolean back = AbilityHandler.isBehind(p, t);
        boolean busy = t instanceof Mob m && m.getTarget() != null && m.getTarget() != p;
        return back || busy;
    }

    /** Из темноты: за спину цели и удар в полтора раза сильнее — его не ждут. */
    private static boolean ambush(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 face = Vec3.directionFromRotation(0, t.getYRot());
        Vec3 behind = t.position().subtract(face.scale(1.3));
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y + 1, from.z, 20, 0.3, 0.5, 0.3, 0.02);
        p.teleportTo(behind.x, t.getY(), behind.z);
        p.setYRot(t.getYRot());
        // после переноса Тень всегда за спиной — первый удар неожиданный
        float dmg = Kit.power(s, a) * 1.5f;
        Kit.dmg(p, a, t, dmg);
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), face, 1.6, 120, -40, 8, DUSK);
        Fx.burst(sl, t.position().add(0, 1, 0), 1.4, 8, 8, EDGE);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, behind.x, behind.y + 1, behind.z, 20, 0.3, 0.5, 0.3, 0.02);
        Kit.sound(sl, behind, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.4f);
        return true;
    }

    /** Веер ножей: пять лезвий веером на 16 блоков; каждое бьёт первого на своём пути. */
    private static boolean knives(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition().subtract(0, 0.3, 0);
        Vec3 look = p.getViewVector(1f);
        int n = Math.max(3, a.count);
        float dmg = Kit.power(s, a);
        Set<LivingEntity> hit = new HashSet<>();
        for (int i = 0; i < n; i++) {
            float yaw = (i - (n - 1) / 2f) * 9f;
            Vec3 dir = look.yRot((float) Math.toRadians(yaw));
            Vec3 end = eye.add(dir.scale(a.radius));
            LivingEntity first = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity e : AbilityHandler.enemiesAround(p, new net.minecraft.world.phys.AABB(eye, end).inflate(1))) {
                var c = e.getBoundingBox().inflate(0.3).clip(eye, end);
                if (c.isPresent() && c.get().distanceToSqr(eye) < best) {
                    best = c.get().distanceToSqr(eye);
                    first = e;
                }
            }
            Vec3 stop = first != null ? first.position().add(0, first.getBbHeight() * 0.6, 0) : end;
            Fx.beam(sl, eye.add(dir.scale(0.8)), stop, 0.07, 6, EDGE);
            if (first != null) {
                Kit.dmg(p, a, first, dmg * (hit.contains(first) ? 0.6f : 1f) * (unaware(p, first) ? 1.3f : 1f));
                hit.add(first);
            }
        }
        Kit.sound(sl, p.position(), SoundEvents.TRIDENT_THROW, 1f, 1.8f);
        return true;
    }

    /** Перерезать: со спины — вдвое, а ниже четверти жизни обычного врага добивает сразу. */
    private static boolean cut(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        boolean un = unaware(p, t);
        float frac = t.getHealth() / Math.max(1f, t.getMaxHealth());
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.85, 0), AbilityHandler.flatLook(p), 1.4, 160, 0, 8, 0xE0C01020);
        Kit.sound(sl, t.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.4f);
        if (un && !Kit.boss(t) && frac < 0.25f) {
            Kit.dmg(p, a, t, t.getHealth() + 1000);
            Kit.msg(p, "Тихо и насмерть.", ChatFormatting.DARK_PURPLE);
            return true;
        }
        Kit.dmg(p, a, t, Kit.power(s, a) * (un ? 2f : 1f));
        return true;
    }

    /** Ночь: круг тьмы. Враги внутри слепнут и бьют на 30% слабее; Тень внутри бьёт на 30% сильнее. */
    private static boolean night(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.2f;
        Fx.sphere(sl, c, a.radius, a.radius, life, 0x60100020);
        Fx.runes(sl, c, a.radius, 5, life, DUSK);
        Kit.sound(sl, c, SoundEvents.WARDEN_EMERGE, 0.6f, 1.4f);
        Scheduler.repeat(0, 10, life / 10, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 30, 0));
                Kit.until(e, Kit.DEALT, 0.7f, 14);
                if (i % 2 == 0) Kit.dmg(p, a, e, tick);
            }
            if (p.position().distanceTo(c) <= a.radius) Kit.until(p, Kit.DEALT, 1.3f, 14);
            return true;
        });
        return true;
    }

    /** Дымовая: облако у ног; враги внутри слепнут и теряют цель, а целившиеся в своих в дыму — тоже. */
    private static boolean smoke(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        Fx.disc(sl, c, a.radius, 0, life, 0x50404040);
        Kit.sound(sl, c, SoundEvents.FIRE_EXTINGUISH, 1.2f, 0.6f);
        Scheduler.repeat(0, 10, life / 10, i -> {
            if (!p.isAlive()) return false;
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.6, c.z, 8, a.radius / 2, 0.6, a.radius / 2, 0.01);
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                AbilityHandler.applyFoe(a, e, p);
                if (e instanceof Mob m && !Kit.boss(e)) m.setTarget(null);
            }
            for (LivingEntity al : Kit.alliesIn(p, c, a.radius)) {
                for (Mob m : sl.getEntitiesOfClass(Mob.class, al.getBoundingBox().inflate(20), m -> m.getTarget() == al && !Kit.boss(m))) {
                    m.setTarget(null);
                }
            }
            return true;
        });
        return true;
    }

    /** Метка жертвы: цель светится сквозь стены и получает на 30% больше. */
    private static boolean mark(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, life, 0, false, false));
        Fx.mark(p.level(), t, 0.6, 2, life, DUSK);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.ENDER_EYE_DEATH, 1f, 1.2f);
        return true;
    }
}
