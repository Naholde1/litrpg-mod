package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Палач: медленно, тяжело и окончательно.
 * <p>
 * Рубящий вдвое сильнее по тем, у кого меньше трети жизни. Подвесить вздёргивает врага цепью
 * над полом. Отсечь — рана, которая с каждой секундой глубже. Обезглавить добивает обычного
 * врага ниже 15% жизни сразу. Жатва голов вертится дольше за каждую упавшую голову. Подтащить
 * тянет к плахе и на миг ставит на колени.
 */
public final class Executioner {
    private static final int STEEL = 0xE0D0D8E0;
    private static final int BLOOD = 0xE0B01010;

    private Executioner() {}

    static void register() {
        Special.put("ex_chop", Executioner::chop);
        Special.put("ex_hang", Executioner::hang);
        Special.put("ex_sever", Executioner::sever);
        Special.put("ex_behead", Executioner::behead);
        Special.put("ex_harvest", Executioner::harvest);
        Special.put("ex_drag", Executioner::drag);
        Special.put("ex_sentence", Executioner::sentence);
    }

    private static float frac(LivingEntity e) {
        return e.getHealth() / Math.max(1f, e.getMaxHealth());
    }

    private static boolean chop(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        boolean low = frac(t) < 0.33f;
        Kit.dmg(p, a, t, Kit.power(s, a) * (low ? 2f : 1f));
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.8, 0), AbilityHandler.flatLook(p), 2.4, 90, 85, 10, low ? BLOOD : STEEL);
        Kit.sound(sl, t.position(), SoundEvents.ANVIL_LAND, 0.6f, low ? 0.5f : 0.8f);
        return true;
    }

    /** Подвесить: цепь вздёргивает врага над полом — он висит, оглушённый, и получает урон. */
    private static boolean hang(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int life = Math.round(a.duration * s.rankDuration(a));
        Kit.dmg(p, a, t, Kit.power(s, a));
        Kit.stun(t, life);
        if (!Kit.boss(t)) {
            Vec3 at = t.position();
            Scheduler.repeat(0, 1, life, i -> {
                if (!t.isAlive()) return false;
                t.setPos(at.x, at.y + Math.min(1.6, i * 0.2), at.z);
                t.setDeltaMovement(Vec3.ZERO);
                t.fallDistance = 0;
                return true;
            });
        }
        Fx.beam(sl, t.position().add(0, 6, 0), t.position().add(0, t.getBbHeight() + 1.6, 0), 0.15, life, STEEL);
        Fx.mark(sl, t, 0.6, 1, life, BLOOD);
        Kit.sound(sl, t.position(), SoundEvents.CHAIN_PLACE, 1.5f, 0.6f);
        return true;
    }

    /** Отсечь: рана, которая каждую секунду кровит сильнее (+25% за секунду). */
    private static boolean sever(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float base = Kit.power(s, a) * 0.12f;
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.5, 0), AbilityHandler.flatLook(p), 1.8, 130, 10, 8, BLOOD);
        Fx.mark(sl, t, 0.5, 1, secs * 20, BLOOD);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            Kit.dmg(p, a, t, base * (1f + 0.25f * i));
            return true;
        });
        return true;
    }

    /** Обезглавить: обычный враг ниже 15% жизни — сразу; остальным — тяжёлый удар, по раненым вдвое. */
    private static boolean behead(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.9, 0), AbilityHandler.flatLook(p), 2.8, 180, 0, 12, BLOOD);
        Fx.burst(sl, t.position().add(0, t.getBbHeight() * 0.9, 0), 2.2, 14, 12, BLOOD);
        Kit.sound(sl, t.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.5f, 0.5f);
        if (!Kit.boss(t) && frac(t) < 0.15f) {
            Kit.dmg(p, a, t, t.getHealth() + 1000);
            Kit.msg(p, "Голова с плеч!", ChatFormatting.DARK_RED);
            return true;
        }
        Kit.dmg(p, a, t, Kit.power(s, a) * (frac(t) < 0.4f ? 2f : 1f));
        return true;
    }

    /** Жатва голов: обороты вокруг себя; каждая упавшая голова добавляет ещё оборот (до +4). */
    private static boolean harvest(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        int[] left = {Math.max(2, a.count)};
        int[] extra = {0};
        Scheduler.repeat(0, 10, 12, i -> {
            if (!p.isAlive() || left[0] <= 0) return false;
            left[0]--;
            Fx.slash(sl, p.position().add(0, 1, 0), AbilityHandler.flatLook(p).yRot(i * 1.7f), a.radius, 360, 0, 8, i % 2 == 0 ? STEEL : BLOOD);
            Kit.sound(sl, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
            for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
                Kit.dmg(p, a, e, dmg);
                if (!e.isAlive() && extra[0] < 4) {
                    extra[0]++;
                    left[0]++;
                    Fx.burst(sl, e.position().add(0, 1.5, 0), 1.5, 8, 10, BLOOD);
                }
            }
            return true;
        });
        return true;
    }

    /** Подтащить: крюк тянет к плахе — враг на секунду падает на колени (оглушение) и получает урон. */
    private static boolean drag(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Fx.chains(sl, p, t, 0.4, 14, STEEL);
        Kit.pull(t, p.position().add(AbilityHandler.flatLook(p).scale(1.5)), Math.min(2.4, 0.4 + t.distanceTo(p) * 0.16));
        Scheduler.later(8, () -> {
            if (!t.isAlive() || !p.isAlive()) return;
            Kit.dmg(p, a, t, Kit.power(s, a));
            Kit.stun(t, 20);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        });
        Kit.sound(sl, p.position(), SoundEvents.CHAIN_BREAK, 1.4f, 0.6f);
        return true;
    }

    /** Приговор: названный получает от всех на 40% больше и светится. */
    private static boolean sentence(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, life, 0, false, false));
        Fx.mark(p.level(), t, 0.8, 1, life, BLOOD);
        Fx.beam(p.level(), t.position().add(0, t.getBbHeight() + 4, 0), t.position().add(0, t.getBbHeight() + 0.8, 0), 0.25, 14, STEEL);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.BELL_BLOCK, 1f, 0.5f);
        return true;
    }
}
