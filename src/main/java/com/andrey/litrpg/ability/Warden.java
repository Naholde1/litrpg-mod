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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Стражник: ставит преграды, за которыми прячется отряд.
 * <p>
 * Стена света — не стекло, а полоса света поперёк пути: чужие снаряды в ней сгорают, а враги
 * отскакивают назад. Купол над отрядом гасит всё, что в него влетает. «Под прикрытие» вешает
 * на союзника щит и забирает треть его урона на стражника; крюк выдёргивает врага под щит,
 * «Упор» возвращает удары, навал впечатывает в пол.
 */
public final class Warden {
    private static final int LIGHT = 0xD0FFE8A0;
    private static final int GOLD = 0xE0FFD060;

    private Warden() {}

    static void register() {
        Special.put("wd_barrier", Warden::barrier);
        Special.put("wd_dome", Warden::dome);
        Special.put("wd_guard", Warden::guard);
        Special.put("wd_pull", Warden::hook);
        Special.put("wd_brace", Warden::brace);
        Special.put("wd_bash", Warden::bash);
    }

    /** Стена света: шесть блоков поперёк взгляда, в трёх блоках впереди. Снаряды сгорают, враги отлетают. */
    private static boolean barrier(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 c = p.position().add(dir.scale(3));
        double half = Math.max(3, a.radius / 2 + 1);
        int life = Kit.dur(s, a);
        Vec3 from = c.add(side.scale(-half)), to = c.add(side.scale(half));
        AABB box = new AABB(from, to).inflate(0.7, 0, 0.7).expandTowards(0, 4, 0);
        Kit.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 1f, 1.6f);
        Fx.line(sl, from, side, half * 2, 0.5, life, 0x60FFE8A0);
        Scheduler.repeat(0, 5, life / 5, i -> {
            if (!p.isAlive()) return false;
            if (i % 4 == 0) {
                // столбики света вдоль стены — сама стена
                for (double d = -half; d <= half + 0.01; d += 1) {
                    Vec3 b = c.add(side.scale(d));
                    Fx.beam(sl, b, b.add(0, 3.2, 0), 0.22, 22, LIGHT);
                }
            }
            for (Projectile pr : Kit.hostileShots(sl, box)) {
                sl.sendParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                pr.discard();
            }
            for (LivingEntity e : AbilityHandler.enemiesAround(p, box)) {
                // отбросить на ту сторону, откуда пришёл
                double sideOf = e.position().subtract(c).dot(dir);
                Kit.push(e, e.position().subtract(dir.scale(sideOf >= 0 ? -1 : 1)), 0.7, 0.2);
                if (i % 4 == 0) Kit.dmg(p, a, e, AbilityHandler.base(s, a) * 0.15f);
            }
            return true;
        });
        return true;
    }

    /** Купол: сфера света над отрядом. Влетевшие снаряды гаснут, свои внутри получают на четверть меньше. */
    private static boolean dome(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        double r = a.radius;
        int life = Kit.dur(s, a);
        Fx.sphere(sl, c, r, r, life, 0x50FFE8A0);
        Fx.runes(sl, c, r, 8, life, GOLD);
        Kit.sound(sl, c, SoundEvents.BEACON_ACTIVATE, 1.2f, 1.1f);
        Scheduler.repeat(0, 4, life / 4, i -> {
            if (!p.isAlive()) return false;
            for (Projectile pr : Kit.hostileShots(sl, new AABB(c, c).inflate(r))) {
                if (pr.position().distanceTo(c) > r + 0.5 || pr.position().distanceTo(c) < r - 2) continue;
                sl.sendParticles(ParticleTypes.END_ROD, pr.getX(), pr.getY(), pr.getZ(), 8, 0.1, 0.1, 0.1, 0.06);
                Fx.ring(sl, pr.position(), 0.1, 0.8, 8, LIGHT);
                pr.discard();
            }
            if (i % 5 == 0) {
                for (LivingEntity e : Kit.alliesIn(p, c, r)) Kit.until(e, Kit.TAKEN, 0.75f, 25);
            }
            return true;
        });
        return true;
    }

    /** Под прикрытие: щит на союзника и цепь к стражнику — треть урона по союзнику уходит в стражника. */
    private static boolean guard(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.ally(p, a.radius);
        ServerLevel sl = p.serverLevel();
        float amount = Kit.power(s, a) * AbilityHandler.healMult(s);
        t.setAbsorptionAmount(Math.max(t.getAbsorptionAmount(), amount));
        int life = Kit.dur(s, a);
        Fx.shield(sl, t, 1.2, life, 0x60FFE8A0);
        if (t != p) {
            Kit.until(t, Kit.GUARD, 0.33f, life);
            t.getPersistentData().putUUID(Kit.GUARD + "_by", p.getUUID());
            Fx.chains(sl, p, t, 0.45, life, GOLD);
        }
        Kit.sound(sl, t.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1f, 1.2f);
        return true;
    }

    /** Крюк стражи: цепь выдёргивает врага к ногам стражника, и тот бьёт уже по стражнику. */
    private static boolean hook(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Fx.chains(sl, p, t, 0.4, 12, GOLD);
        Kit.sound(sl, p.position(), SoundEvents.CHAIN_PLACE, 1.2f, 0.7f);
        Kit.pull(t, p.position().add(AbilityHandler.flatLook(p).scale(1.5)), Math.min(2.4, 0.35 + t.distanceTo(p) * 0.16));
        Kit.dmg(p, a, t, Kit.power(s, a));
        Kit.taunt(p, t);
        return true;
    }

    /** Упор: баф из данных и отражение — 40% каждого удара вблизи возвращается бьющему. */
    private static boolean brace(ServerPlayer p, PlayerStats s, Ability a) {
        AbilityHandler.applySelf(a, p);
        int life = Kit.dur(s, a);
        Kit.until(p, Kit.REFLECT, 0.4f, life);
        Fx.shield(p.serverLevel(), p, 1.3, life, 0x50FFD060);
        Fx.runes(p.serverLevel(), p.position(), 1.6, 4, life, GOLD);
        Kit.sound(p.serverLevel(), p.position(), SoundEvents.ANVIL_PLACE, 0.8f, 0.6f);
        return true;
    }

    /** Оглушающий навал: рывок на четыре блока; первый, в кого врезался, оглушён и отлетает. У стены — вдвое дольше. */
    private static boolean bash(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        LivingEntity hit = null;
        for (double d = 0.5; d <= 4.5 && hit == null; d += 0.5) {
            Vec3 at = p.position().add(dir.scale(d));
            for (LivingEntity e : Kit.enemiesIn(p, at.add(0, 1, 0), 1.2)) {
                hit = e;
                break;
            }
        }
        Vec3 stop = hit != null ? hit.position().subtract(dir.scale(1.2)) : p.position().add(dir.scale(4));
        p.teleportTo(stop.x, p.getY(), stop.z);
        Fx.wave(sl, p.position().subtract(dir.scale(4)), dir, 4, 0.8, 2, 8, LIGHT);
        if (hit == null) return true;
        // впечатать: если за спиной цели стена — оглушение вдвое
        boolean wall = !sl.getBlockState(net.minecraft.core.BlockPos.containing(hit.position().add(dir.scale(1.2)).add(0, 0.5, 0))).isAir();
        int st = (int) (a.duration * (wall ? 2 : 1) * s.rankDuration(a));
        Kit.dmg(p, a, hit, Kit.power(s, a) * (wall ? 1.4f : 1f));
        Kit.stun(hit, st);
        Kit.push(hit, p.position(), wall ? 0.1 : 1.0, 0.15);
        Fx.burst(sl, hit.position().add(0, 1, 0), 2.2, 12, 10, GOLD);
        Kit.sound(sl, hit.position(), SoundEvents.SHIELD_BLOCK, 1.2f, 0.6f);
        if (wall) Kit.msg(p, "Впечатан в стену! Оглушение вдвое.", ChatFormatting.GOLD);
        hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, st + 20, 1));
        return true;
    }
}
