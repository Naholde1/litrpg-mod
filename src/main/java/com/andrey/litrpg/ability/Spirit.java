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

import java.util.List;

/**
 * Дух: холод мёртвых.
 * <p>
 * Каждое касание оставляет иней — слои холода (до трёх); на третьем враг промерзает и на
 * секунду застывает. Могильный холод морозит всех в круге. Вытянуть — нить, по которой жизнь
 * течёт к духу. Страх гонит врагов прочь. Хор мёртвых — призрачные столбы падают на врагов
 * один за другим.
 */
public final class Spirit {
    private static final int PALE = 0xC0B0D8FF;
    private static final int GHOST = 0xA0E0F0FF;
    private static final String FROST = "litrpg_sp_frost";

    private Spirit() {}

    static void register() {
        Special.put("sp_touch", Spirit::touch);
        Special.put("sp_chill", Spirit::chill);
        Special.put("sp_siphon", Spirit::siphon);
        Special.put("sp_fear", Spirit::fear);
        Special.put("sp_legion", Spirit::chorus);
    }

    /** Слой инея; третий — промерзание на секунду. */
    private static void frost(ServerPlayer p, LivingEntity e) {
        int st = (int) Kit.value(e, FROST, 0) + 1;
        if (st >= 3) {
            Kit.clear(e, FROST);
            Kit.stun(e, 20);
            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 100));
            Fx.shards(p.level(), e.position(), 0.9, 8, e.getBbHeight() * 0.8, 20, PALE);
            Kit.sound(p.serverLevel(), e.position(), SoundEvents.GLASS_BREAK, 1f, 1.6f);
            return;
        }
        Kit.until(e, FROST, st, 120);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, st - 1));
    }

    private static boolean touch(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Fx.beam(sl, p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.1, 8, GHOST);
        Kit.dmg(p, a, t, Kit.power(s, a));
        frost(p, t);
        Kit.sound(sl, t.position(), SoundEvents.SOUL_ESCAPE, 1f, 1.2f);
        return true;
    }

    /** Могильный холод: круг вокруг духа; раз в секунду урон и слой инея всем внутри. */
    private static boolean chill(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.3f;
        Fx.runes(sl, c, a.radius, 6, life, PALE);
        Fx.disc(sl, c, a.radius, 0, life, 0x3090C0FF);
        Kit.sound(sl, c, SoundEvents.POWDER_SNOW_PLACE, 1.5f, 0.6f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 1, c.z, 20, a.radius / 2, 0.6, a.radius / 2, 0.01);
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, tick);
                frost(p, e);
            }
            return true;
        });
        return true;
    }

    /** Вытянуть: нить к цели; каждую секунду жизнь течёт по ней к духу. */
    private static boolean siphon(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float tick = Kit.power(s, a) * 2f / secs;
        Fx.tether(sl, p, t, 0.1, secs * 20, PALE);
        Scheduler.repeat(0, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive() || t.distanceTo(p) > a.radius + 4) return false;
            float before = t.getHealth();
            Kit.dmg(p, a, t, tick);
            p.heal(Math.max(0, before - t.getHealth()) * 0.5f);
            sl.sendParticles(ParticleTypes.SOUL, t.getX(), t.getY() + 1, t.getZ(), 3, 0.2, 0.3, 0.2, 0.02);
            return true;
        });
        return true;
    }

    /** Страх: обычные враги бегут прочь на несколько секунд; хранитель лишь слабеет. */
    private static boolean fear(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int life = Kit.dur(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            if (Kit.boss(e)) {
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, life / 2, 1));
                continue;
            }
            SkillEffects.charm(e, life);
            Kit.push(e, p.position(), 1.2, 0.25);
            Fx.mark(sl, e, 0.5, 1, life, GHOST);
        }
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 14, 0x70B0D8FF);
        Kit.sound(sl, p.position(), SoundEvents.GHAST_SCREAM, 0.8f, 0.6f);
        return true;
    }

    /** Хор мёртвых: призрачные столбы падают на врагов вокруг один за другим (каждого — по разу). */
    private static boolean chorus(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        List<LivingEntity> list = Kit.enemiesIn(p, p.position(), a.radius);
        if (list.isEmpty()) return AbilityHandler.noTarget(p);
        float dmg = Kit.power(s, a);
        Fx.runes(sl, p.position(), 3, 7, list.size() * 4 + 20, GHOST);
        Kit.sound(sl, p.position(), SoundEvents.SOUL_ESCAPE, 2f, 0.5f);
        Scheduler.repeat(6, 4, list.size(), i -> {
            LivingEntity e = list.get(i);
            if (!e.isAlive() || !p.isAlive()) return true;
            Fx.pillar(sl, e.position(), 0.7, 7, 14, GHOST);
            Fx.bolt(sl, e.position().add(0, 8, 0), e.position().add(0, 1, 0), 0.18, 8, PALE);
            Kit.dmg(p, a, e, dmg);
            frost(p, e);
            Kit.sound(sl, e.position(), SoundEvents.SOUL_ESCAPE, 1.2f, 0.8f + i * 0.05f);
            return true;
        });
        return true;
    }
}
