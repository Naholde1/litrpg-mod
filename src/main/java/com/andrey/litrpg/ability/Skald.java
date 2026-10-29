package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEffects;
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

/**
 * Скальд: песни, которые слышит весь отряд.
 * <p>
 * Какофония путает врагов: они бьют слабее и иногда — друг друга. Знамя ставится на пол, и
 * пока отряд рядом с ним, он бьёт сильнее. Сага на всё время усиливает каждого, кто её
 * слышит. Финал лечит тем сильнее, чем больше разных песен сейчас звучит на скальде.
 * Протяжный мотив перескакивает на другого раненого, когда первый уже здоров.
 */
public final class Skald {
    private static final int SONG = 0xD0FFD080;
    private static final int NOTE = 0xC090E0FF;

    private Skald() {}

    static void register() {
        Special.put("sk_discord", Skald::discord);
        Special.put("sk_banner", Skald::banner);
        Special.put("sk_saga", Skald::saga);
        Special.put("sk_finale", Skald::finale);
        Special.put("sk_tune", Skald::tune);
    }

    private static void notes(ServerLevel sl, Vec3 at, int n) {
        sl.sendParticles(ParticleTypes.NOTE, at.x, at.y + 1.6, at.z, n, 0.6, 0.4, 0.6, 1);
    }

    /** Какофония: враги в круге бьют на четверть слабее и порой набрасываются на соседа. */
    private static boolean discord(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 20);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.3f;
        Fx.runes(sl, c, a.radius, 7, life, NOTE);
        Kit.sound(sl, c, SoundEvents.NOTE_BLOCK_DIDGERIDOO.get(), 2f, 0.5f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            notes(sl, c, 6);
            var list = Kit.enemiesIn(p, c, a.radius);
            for (LivingEntity e : list) {
                Kit.until(e, Kit.DEALT, 0.75f, 25);
                Kit.dmg(p, a, e, tick);
                if (e instanceof Mob m && !Kit.boss(e) && sl.random.nextFloat() < 0.3f && list.size() > 1) {
                    LivingEntity other = list.get(sl.random.nextInt(list.size()));
                    if (other != e) {
                        m.setTarget(other);
                        Fx.beam(sl, e.position().add(0, 1.5, 0), other.position().add(0, 1.5, 0), 0.06, 10, NOTE);
                    }
                }
            }
            return true;
        });
        return true;
    }

    /** Знамя: столб у ног скальда; свои рядом с ним бьют на 15% сильнее и восстанавливаются, враги — слабеют. */
    private static boolean banner(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        Fx.pillar(sl, c, 0.25, 4, life, SONG);
        Fx.runes(sl, c, a.radius, 6, life, SONG);
        Kit.sound(sl, c, SoundEvents.RAID_HORN.get(), 0.7f, 1.4f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.alliesIn(p, c, a.radius)) {
                Kit.until(e, Kit.DEALT, 1.15f, 25);
                AbilityHandler.applySelf(a, e);
            }
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0));
            notes(sl, c, 3);
            return true;
        });
        return true;
    }

    /** Сага о героях: бафы из данных всему отряду и +15% к урону на всё её время. */
    private static boolean saga(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int life = Kit.dur(s, a);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            AbilityHandler.applySelf(a, e);
            Kit.until(e, Kit.DEALT, 1.15f, life);
            Fx.orbit(sl, e, 0.9, 3, 2.2, 0.12, life, SONG);
            notes(sl, e.position(), 5);
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 18, SONG, 0.5f);
        Kit.sound(sl, p.position(), SoundEvents.NOTE_BLOCK_BELL.get(), 2f, 0.8f);
        return true;
    }

    /** Финал: лечение всем — +25% за каждую звучащую песнь (сила, стойкость, ветер) на скальде. */
    private static boolean finale(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int songs = 0;
        if (p.hasEffect(ModEffects.MIGHT.get())) songs++;
        if (p.hasEffect(ModEffects.BULWARK.get())) songs++;
        if (p.hasEffect(MobEffects.MOVEMENT_SPEED) && p.hasEffect(MobEffects.DIG_SPEED)) songs++;
        float heal = Dynamic.healAmount(s, a, 1f + 0.25f * songs);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            Kit.heal(p, e, heal);
            Fx.pillar(sl, e.position(), 0.6, 4, 18, SONG);
            notes(sl, e.position(), 8);
        }
        Fx.burst(sl, p.position().add(0, 1.5, 0), 4, 20, 14, SONG);
        Kit.sound(sl, p.position(), SoundEvents.NOTE_BLOCK_CHIME.get(), 2f, 1.2f);
        if (songs > 0) Kit.msg(p, "Финал: звучит песен — " + songs + ", лечение +" + songs * 25 + "%", ChatFormatting.GOLD);
        return true;
    }

    /** Протяжный мотив: лечит каждую секунду; когда цель здорова — мотив переходит к другому раненому. */
    private static boolean tune(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity first = Dynamic.healTarget(p, a.radius);
        int secs = Math.max(2, Kit.dur(s, a) / 20);
        float per = Dynamic.healAmount(s, a, 1f) / secs;
        ServerLevel sl = p.serverLevel();
        LivingEntity[] cur = {first};
        Fx.orbit(sl, first, 0.7, 2, 2.1, 0.1, 40, NOTE);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!p.isAlive()) return false;
            LivingEntity t = cur[0];
            if (!t.isAlive() || t.getHealth() >= t.getMaxHealth()) {
                LivingEntity next = null;
                float worst = 0.999f;
                for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
                    float f = e.getHealth() / e.getMaxHealth();
                    if (f < worst) {
                        worst = f;
                        next = e;
                    }
                }
                if (next == null) return true;
                Fx.beam(sl, t.position().add(0, 1.5, 0), next.position().add(0, 1.5, 0), 0.08, 10, NOTE);
                Fx.orbit(sl, next, 0.7, 2, 2.1, 0.1, 40, NOTE);
                cur[0] = t = next;
            }
            Kit.heal(p, t, per);
            notes(sl, t.position(), 1);
            return true;
        });
        Kit.sound(sl, first.position(), SoundEvents.NOTE_BLOCK_FLUTE.get(), 1.4f, 1.2f);
        return true;
    }
}
