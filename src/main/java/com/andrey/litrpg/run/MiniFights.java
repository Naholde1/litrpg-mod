package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Мини-хранители как «идейные» бои: у каждой роли свой приём с разметкой, от которого можно уйти.
 * <ul>
 *   <li>brute — Сокрушение: круг под собой, через секунду удар и отброс; на половине жизни — ярость (чаще и шире);</li>
 *   <li>swift — Бросок: полоса к игроку, через секунду рывок по ней — всех на пути задевает;</li>
 *   <li>guard — Стойка: три секунды щит — снаряды отскакивают, удары вблизи возвращаются наполовину;</li>
 *   <li>shade — Из тени: метка над игроком, через секунду удар со спины;</li>
 *   <li>giant — Дрожь земли: три кольца расходятся по очереди — стоять на кольце больно.</li>
 * </ul>
 * Урон — доля здоровья игрока, поэтому приём одинаково читается на любом этапе.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class MiniFights {
    public static final String ROLE = "litrpg_mini";
    private static final String NEXT = "litrpg_mini_next";
    private static final String RAGE = "litrpg_mini_rage";
    private static final String GUARD = "litrpg_mini_guard";

    private MiniFights() {}

    private static List<ServerPlayer> near(Mob m, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : ((ServerLevel) m.level()).getEntitiesOfClass(ServerPlayer.class, m.getBoundingBox().inflate(r),
                p -> p.isAlive() && !p.isSpectator() && !p.isCreative())) out.add(p);
        return out;
    }

    private static void hurt(Mob m, ServerPlayer p, float frac) {
        p.invulnerableTime = 0;
        p.hurt(m.damageSources().mobAttack(m), p.getMaxHealth() * frac);
    }

    private static void say(Mob m, String text) {
        for (ServerPlayer p : near(m, 24)) {
            p.displayClientMessage(Component.literal(m.getName().getString() + ": " + text).withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent e) {
        if (!(e.getEntity() instanceof Mob m) || m.level().isClientSide || !m.getPersistentData().contains(ROLE)) return;
        if (!(m.level() instanceof ServerLevel sl) || m.tickCount % 5 != 0 || m.getTarget() == null) return;
        var d = m.getPersistentData();
        long now = sl.getGameTime();
        if (d.getLong(NEXT) == 0) d.putLong(NEXT, now + 80);
        // ярость на половине жизни — один раз
        if (!d.getBoolean(RAGE) && m.getHealth() < m.getMaxHealth() / 2) {
            d.putBoolean(RAGE, true);
            Fx.ring(sl, m.position(), 0.5, 6, 16, 0xD0FF3020);
            sl.playSound(null, m.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4f, 0.8f);
            say(m, "в ярости!");
        }
        if (now < d.getLong(NEXT)) return;
        boolean rage = d.getBoolean(RAGE);
        String role = d.getString(ROLE);
        int period = switch (role) {
            case "brute" -> 160;
            case "swift" -> 120;
            case "guard" -> 200;
            case "shade" -> 140;
            default -> 180;
        };
        d.putLong(NEXT, now + (rage ? period * 2 / 3 : period));
        switch (role) {
            case "brute" -> smash(m, sl, rage);
            case "swift" -> charge(m, sl);
            case "guard" -> stance(m, sl);
            case "shade" -> ambush(m, sl);
            default -> quake(m, sl);
        }
    }

    private static void smash(Mob m, ServerLevel sl, boolean rage) {
        double r = rage ? 5 : 3.5;
        Vec3 c = m.position();
        Fx.disc(sl, c, r, 0, 24, 0xB0FF4020);
        sl.playSound(null, m.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.2f, 0.6f);
        Scheduler.later(24, () -> {
            if (!m.isAlive()) return;
            Fx.ring(sl, c, 0.3, r + 1, 10, 0xE0FF6020, 0.8f);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, 0.8, 0, 0.8, 0);
            sl.playSound(null, m.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1f, 0.7f);
            for (ServerPlayer p : near(m, r + 1)) {
                if (p.position().distanceTo(c) > r) continue;
                hurt(m, p, 0.3f);
                Vec3 k = p.position().subtract(c).multiply(1, 0, 1).normalize().scale(1.2);
                p.setDeltaMovement(k.x, 0.5, k.z);
                p.hurtMarked = true;
            }
        });
    }

    private static void charge(Mob m, ServerLevel sl) {
        LivingEntity t = m.getTarget();
        Vec3 from = m.position();
        Vec3 dir = t.position().subtract(from).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1) return;
        double len = Math.min(14, dir.length() + 3);
        Vec3 u = dir.normalize();
        Fx.line(sl, from, u, len, 1.2, 20, 0xB0FF8020);
        sl.playSound(null, m.blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 1.4f, 0.8f);
        Scheduler.later(20, () -> {
            if (!m.isAlive()) return;
            Vec3 to = from.add(u.scale(len));
            m.teleportTo(to.x, m.getY(), to.z);
            Fx.wave(sl, from, u, len, 1.2, 1.4, 8, 0xD0FFB040);
            for (ServerPlayer p : near(m, len + 2)) {
                Vec3 rel = p.position().subtract(from);
                double along = rel.dot(u);
                if (along < 0 || along > len || rel.subtract(u.scale(along)).multiply(1, 0, 1).length() > 1.4) continue;
                hurt(m, p, 0.25f);
            }
        });
    }

    private static void stance(Mob m, ServerLevel sl) {
        m.getPersistentData().putLong(GUARD, sl.getGameTime() + 60);
        Fx.shield(sl, m, m.getBbWidth() + 0.8, 60, 0x80A0C0FF);
        sl.playSound(null, m.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.5f, 0.6f);
        say(m, "поднимает щит — бейте позже!");
    }

    private static boolean guarding(LivingEntity e) {
        return e.level().getGameTime() < e.getPersistentData().getLong(GUARD);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !guarding(t)) return;
        if (e.getSource().getEntity() instanceof LivingEntity att && att != t && att.distanceTo(t) < 5) {
            att.invulnerableTime = 0;
            att.hurt(t.damageSources().thorns(t), e.getAmount() * 0.5f);
        }
        e.setAmount(e.getAmount() * 0.3f);
    }

    @SubscribeEvent
    public static void onProjectile(ProjectileImpactEvent e) {
        if (!(e.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity t)) return;
        if (t.level().isClientSide || !guarding(t)) return;
        Projectile pr = e.getProjectile();
        pr.setDeltaMovement(pr.getDeltaMovement().scale(-0.4));
        e.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);
    }

    private static void ambush(Mob m, ServerLevel sl) {
        List<ServerPlayer> ps = near(m, 20);
        if (ps.isEmpty()) return;
        ServerPlayer p = ps.get(sl.random.nextInt(ps.size()));
        Fx.mark(sl, p, 0.8, 1, 20, 0xD0A040FF);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 0.6f, 1.6f);
        Scheduler.later(20, () -> {
            if (!m.isAlive() || !p.isAlive()) return;
            Vec3 face = Vec3.directionFromRotation(0, p.getYRot());
            Vec3 behind = p.position().subtract(face.scale(1.4));
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 1, m.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
            m.teleportTo(behind.x, p.getY(), behind.z);
            Fx.slash(sl, p.position().add(0, 1, 0), face, 1.6, 120, 0, 8, 0xE0A040FF);
            // кто успел обернуться (смотрит на тень) — получает вдвое меньше
            boolean facing = p.getLookAngle().dot(behind.subtract(p.position()).normalize()) > 0.3;
            hurt(m, p, facing ? 0.12f : 0.25f);
            m.setTarget(p);
        });
    }

    private static void quake(Mob m, ServerLevel sl) {
        Vec3 c = m.position();
        sl.playSound(null, m.blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 1.5f, 0.5f);
        for (int i = 0; i < 3; i++) {
            final double r = 3 + i * 3;
            Fx.disc(sl, c, r + 0.8, r - 0.8, 20 + i * 12, 0x90FF9020);
            Scheduler.later(20 + i * 12, () -> {
                if (!m.isAlive()) return;
                Fx.ring(sl, c, r - 0.8, r + 0.8, 8, 0xE0FFB040, 0.6f);
                sl.playSound(null, m.blockPosition(), SoundEvents.DEEPSLATE_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
                for (ServerPlayer p : near(m, r + 2)) {
                    double d = p.position().distanceTo(c);
                    if (Math.abs(d - r) <= 1 && p.onGround()) hurt(m, p, 0.2f);
                }
            });
        }
    }
}
