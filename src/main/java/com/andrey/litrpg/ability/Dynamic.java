package com.andrey.litrpg.ability;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.event.StyleEvents;
import com.andrey.litrpg.event.Threat;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.network.FocusPacket;
import com.andrey.litrpg.run.Spirits;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Динамичные навыки 3.0 — то, что жмут часто, а не раз в полминуты:
 * <ul>
 *   <li>TARGET_HEAL — мгновенное лечение союзника (выбранного Ctrl+1…5 → под взглядом → самого раненого);</li>
 *   <li>CAST_HEAL / CAST_BOLT — заклинания с подготовкой ({@link Casting}): стоять на месте;</li>
 *   <li>HOT — лечение со временем; CHAIN_HEAL — перескакивает на раненых рядом;</li>
 *   <li>TARGET_BOLT — самонаводящийся снаряд в цель (с доводкой прицела); DOT — проклятие на цель.</li>
 * </ul>
 */
public final class Dynamic {
    private Dynamic() {}

    // ================= цели =================

    /** Союзник для лечения: выбранный в рамках → под взглядом → самый раненый в группе; иначе сам игрок. */
    public static LivingEntity healTarget(ServerPlayer p, double range) {
        var data = p.getPersistentData();
        if (data.hasUUID(FocusPacket.FOCUS)) {
            Player f = p.level().getPlayerByUUID(data.getUUID(FocusPacket.FOCUS));
            if (f instanceof ServerPlayer fp && fp.isAlive() && !Spirits.isSpirit(fp) && fp.distanceTo(p) <= range) return fp;
        }
        // под прицелом: союзник-игрок или свой прислужник
        LivingEntity look = AbilityHandler.lookTarget(p, range);
        if (look != null && look != p && look.isAlive() && !look.isSpectator()
                && (look instanceof Player || AbilityHandler.isAlly(p, look))) {
            return look;
        }
        // автонаводки нет: не выбрал и ни на кого не смотришь — лечишь себя
        return p;
    }

    /** Враг под прицелом; если промах — ближайший к линии взгляда в конусе 14° (видимый). */
    public static LivingEntity aimEnemy(ServerPlayer p, double range) {
        LivingEntity t = AbilityHandler.lookEnemy(p, range);
        if (t != null) return t;
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double cos = Math.cos(Math.toRadians(14));
        LivingEntity best = null;
        double bd = -1;
        for (LivingEntity e : AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(range))) {
            Vec3 to = e.position().add(0, e.getBbHeight() * 0.6, 0).subtract(eye);
            double d = to.length();
            if (d > range || d < 0.5) continue;
            double dot = to.scale(1 / d).dot(look);
            if (dot < cos || dot < bd) continue;
            var hit = p.level().clip(new ClipContext(eye, eye.add(to), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            if (hit.getType() == HitResult.Type.BLOCK) continue;
            bd = dot;
            best = e;
        }
        return best;
    }

    private static int color(Ability a) {
        return Fx.color(a.element == null ? "HEAL" : a.element.name());
    }

    static int healColor(Ability a) {
        if (a.element == null) return Fx.color("HEAL");
        return switch (a.element) {
            case HOLY -> 0xD0FFE890;
            case SHADOW -> 0xD0B070FF;
            case FROST -> 0xC070D8FF;
            case ARCANE -> 0xD0D090FF;
            default -> Fx.color("HEAL");
        };
    }

    // ================= лечение =================

    static float healAmount(PlayerStats s, Ability a, float mult) {
        return AbilityHandler.base(s, a) * a.power * AbilityHandler.healMult(s) * mult;
    }

    static void doHeal(ServerPlayer p, LivingEntity t, float amount) {
        float before = t.getHealth();
        t.heal(amount);
        float done = t.getHealth() - before;
        Threat.healed(p, t, done);
        StyleEvents.healed(p, t, done);
        if (done > 0.5f && p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + t.getBbHeight() + 0.3, t.getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
    }

    public static boolean targetHeal(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = healTarget(p, a.radius);
        doHeal(p, t, healAmount(s, a, 1f));
        int c = healColor(a);
        if (t != p) Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.25, 8, c);
        Fx.spiral(p.level(), t, 0.7, t.getBbHeight() + 0.4, 1.5, 14, c);
        AbilityHandler.sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.6f);
        return true;
    }

    /** Лечение после подготовки (цель выбирается в момент завершения). */
    public static boolean castHeal(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = healTarget(p, a.radius);
        doHeal(p, t, healAmount(s, a, 1f));
        int c = healColor(a);
        Fx.pillar(p.level(), t.position(), 0.9, 3.5, 16, c);
        Fx.ring(p.level(), t.position(), 0.3, 2.5, 14, c);
        if (t != p) Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.4, 10, c);
        AbilityHandler.sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 1.5f);
        return true;
    }

    public static boolean hot(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = healTarget(p, a.radius);
        int secs = Math.max(1, a.duration / 20);
        float per = healAmount(s, a, 1f) / secs;
        int c = healColor(a);
        Fx.mark(p.level(), t, 0.35, 3, a.duration, c);
        Fx.spiral(p.level(), t, 0.8, 2.2, 2, 20, c);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            doHeal(p, t, per);
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 1, t.getZ(), 3, 0.3, 0.4, 0.3, 0);
            }
            return true;
        });
        AbilityHandler.sound(p, SoundEvents.AZALEA_LEAVES_PLACE, 1f, 1.2f);
        return true;
    }

    public static boolean chainHeal(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity first = healTarget(p, a.radius);
        List<LivingEntity> done = new ArrayList<>();
        LivingEntity cur = first;
        Vec3 from = p.getEyePosition().subtract(0, 0.3, 0);
        float mult = 1f;
        int c = healColor(a);
        for (int i = 0; i < Math.max(1, a.count) && cur != null; i++) {
            final LivingEntity t = cur;
            final float m = mult;
            final Vec3 src = from;
            Scheduler.later(1 + i * 4, () -> {
                if (!t.isAlive()) return;
                doHeal(p, t, healAmount(s, a, m));
                Fx.beam(p.level(), src, t.position().add(0, t.getBbHeight() * 0.6, 0), 0.3, 10, c);
                Fx.ring(p.level(), t.position(), 0.2, 1.6, 10, c);
            });
            done.add(cur);
            from = cur.position().add(0, cur.getBbHeight() * 0.6, 0);
            mult *= 0.8f;
            cur = nextWounded(p, cur, done, 10);
        }
        AbilityHandler.sound(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 1f, 1.5f);
        return true;
    }

    private static LivingEntity nextWounded(ServerPlayer p, LivingEntity from, List<LivingEntity> skip, double r) {
        LivingEntity best = null;
        float bf = 2f;
        for (Player o : p.level().players()) {
            if (skip.contains(o) || o.isSpectator() || !o.isAlive() || o.distanceTo(from) > r) continue;
            float f = o.getHealth() / Math.max(1f, o.getMaxHealth());
            if (f < bf) {
                bf = f;
                best = o;
            }
        }
        return best;
    }

    // ================= урон =================

    private static ManaBoltEntity launch(ServerPlayer p, PlayerStats s, Ability a, LivingEntity target, float mult, double speed) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        ManaBoltEntity.Element el = a.element == null ? ManaBoltEntity.Element.ARCANE : a.element;
        ManaBoltEntity bolt = new ManaBoltEntity(level, p, look.x, look.y, look.z, el);
        bolt.setPos(p.getX() + look.x * 0.8, p.getEyeY() - 0.15 + look.y * 0.8, p.getZ() + look.z * 0.8);
        bolt.setDeltaMovement(look.scale(speed));
        bolt.xPower = look.x * 0.08;
        bolt.yPower = look.y * 0.08;
        bolt.zPower = look.z * 0.08;
        bolt.setDamage(AbilityHandler.base(s, a) * a.power * mult);
        if (target != null) bolt.setHoming(target, speed);
        level.addFreshEntity(bolt);
        return bolt;
    }

    public static boolean targetBolt(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = aimEnemy(p, a.radius);
        launch(p, s, a, t, 1f, 1.9);
        Fx.ring(p.level(), p.position().add(0, 1.4, 0).add(p.getLookAngle().scale(0.8)), 0.1, 0.8, 6, color(a));
        AbilityHandler.sound(p, SoundEvents.EVOKER_CAST_SPELL, 0.6f, 1.8f);
        return true;
    }

    public static boolean castBolt(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = aimEnemy(p, a.radius);
        ManaBoltEntity b = launch(p, s, a, t, 1f, 1.5);
        b.setSplash(2.5f);
        Fx.sphere(p.level(), p.getEyePosition(), 0.3, 1.6, 10, color(a));
        AbilityHandler.sound(p, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
        return true;
    }

    public static boolean dot(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = aimEnemy(p, a.radius);
        if (t == null) {
            p.displayClientMessage(Component.literal("Нет цели").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        int secs = Math.max(1, a.duration / 20);
        float per = AbilityHandler.base(s, a) * a.power;
        int c = color(a);
        Fx.mark(p.level(), t, 0.4, 1, a.duration, c);
        Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.15, 6, c);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().indirectMagic(p, p), per);
            if (p.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.WITCH, t.getX(), t.getY() + 1, t.getZ(), 4, 0.3, 0.5, 0.3, 0);
            return true;
        });
        AbilityHandler.sound(p, SoundEvents.WITCH_CELEBRATE, 0.6f, 1.4f);
        return true;
    }
}
