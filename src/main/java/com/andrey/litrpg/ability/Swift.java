package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Стриж — дуэлянт без шкал: у него только откаты, инерция и отмены.
 * <p>
 * Общие виды движка этого не давали: серия была одним ударом «на троих», рывок — простым
 * перемещением, а «Три стороны» — обычной очередью на месте. Здесь серия действительно идёт
 * тремя взмахами и третий уносит вперёд, рывок обрывает начатое и возвращает серпы, а «Три
 * стороны» обходит цель кругом. В этом весь класс: он не стоит.
 */
public final class Swift {
    private static final int EDGE = 0xE048C9B0;
    /** Метка скольжения: пока держится, снаряды проходят мимо. */
    public static final String SLIDE_TAG = "litrpg_slide";

    private Swift() {}

    static void register() {
        Special.put("sw_sickles", Swift::sickles);
        Special.put("sw_dash", Swift::breakaway);
        Special.put("sw_crossing", Swift::crossing);
        Special.put("sw_storm", Swift::threeSides);
        Special.put("sw_slide", Swift::slide);
    }

    private static void hitAt(ServerPlayer p, Ability a, Vec3 c, double r, float dmg, int color, double arc) {
        ServerLevel sl = p.serverLevel();
        Fx.slash(sl, c, p.getViewVector(1f), r, arc, 0, 7, color);
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r))) {
            AbilityHandler.hurt(p, a, e, dmg);
        }
    }

    /**
     * Парные серпы: три взмаха подряд, а третий — выпад на два блока вперёд.
     * Серия не доигрывается, если прервать её рывком, — на этом и строится игра.
     */
    private static boolean sickles(ServerPlayer p, PlayerStats s, Ability a) {
        float dmg = AbilityHandler.base(s, a) * a.power;
        ServerLevel sl = p.serverLevel();
        Scheduler.repeat(0, 4, 3, i -> {
            if (!p.isAlive()) return false;
            if (i == 2) {
                // выпад: Стриж уходит вперёд вместе с ударом
                Vec3 dir = AbilityHandler.flatLook(p);
                p.setDeltaMovement(dir.x * 0.9, 0.12, dir.z * 0.9);
                p.hurtMarked = true;
                hitAt(p, a, p.position().add(dir.scale(1.6)).add(0, 1, 0), a.radius + 0.8, dmg * 1.5f, 0xE0FFFFFF, 150);
                sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.1f);
            } else {
                hitAt(p, a, p.position().add(AbilityHandler.flatLook(p).scale(1.1)).add(0, 1, 0), a.radius, dmg, EDGE, 110);
                sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9f, 1.4f + i * 0.2f);
            }
            return true;
        });
        return true;
    }

    /**
     * Срыв: рывок сквозь строй, который обрывает начатое и возвращает серпы.
     * Отмена анимации — не красивое слово: откат серии действительно сбрасывается.
     */
    private static boolean breakaway(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        Vec3 from = p.position();
        Vec3 to = from.add(dir.scale(5.5));
        float dmg = AbilityHandler.base(s, a) * a.power;
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(from, to).inflate(1.6))) {
            AbilityHandler.hurt(p, a, e, dmg);
        }
        p.setDeltaMovement(dir.x * 1.5, 0.18, dir.z * 1.5);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, 4, false, false, false));
        Fx.beam(sl, from.add(0, 1, 0), to.add(0, 1, 0), 0.5, 8, EDGE);
        Fx.ring(sl, from, 0.2, 1.8, 8, EDGE);
        // серпы снова готовы: серия начинается заново
        Ability sickles = Ability.byKey("sw_sickles");
        if (sickles != null) s.setCooldownEnd(sickles, 0);
        p.getPersistentData().putLong(AbilityHandler.GCD_TAG, 0);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1f, 1.6f);
        return true;
    }

    /** Перекрестье: проход насквозь — Стриж оказывается за спиной и бьёт оттуда. */
    private static boolean crossing(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, 7);
        if (t == null) {
            p.displayClientMessage(Component.literal("Нет цели для прохода.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 behind = t.position().add(t.position().subtract(from).normalize().scale(1.6));
        p.teleportTo(behind.x, behind.y, behind.z);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        float dmg = AbilityHandler.base(s, a) * a.power * 1.6f;
        AbilityHandler.hurt(p, a, t, dmg);
        Fx.beam(sl, from.add(0, 1, 0), behind.add(0, 1, 0), 0.4, 8, EDGE);
        Fx.slash(sl, t.position().add(0, 1, 0), p.getViewVector(1f), 2.2, 160, 0, 8, 0xE0FFFFFF);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.3f);
        return true;
    }

    /**
     * Три стороны: шесть ударов с трёх точек вокруг цели — Стриж обходит её по кругу
     * быстрее, чем она успевает повернуться.
     */
    private static boolean threeSides(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, 10);
        if (t == null) {
            p.displayClientMessage(Component.literal("Некого обходить.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power * 0.6f;
        Vec3 c = t.position();
        Scheduler.repeat(0, 5, 6, i -> {
            if (!p.isAlive() || !t.isAlive()) return false;
            double ang = Math.PI * 2 * i / 3.0;
            Vec3 at = c.add(Math.cos(ang) * 2.0, 0, Math.sin(ang) * 2.0);
            p.teleportTo(at.x, at.y, at.z);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
            AbilityHandler.hurt(p, a, t, dmg);
            Fx.slash(sl, c.add(0, 1, 0), t.position().subtract(at).normalize(), 2.4, 140, 0, 6, EDGE);
            Fx.beam(sl, at.add(0, 1, 0), c.add(0, 1, 0), 0.22, 6, 0xE0FFFFFF);
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f + i * 0.1f);
            return true;
        });
        return true;
    }

    /** Скольжение: секунда, в которую снаряды проходят над головой. */
    private static boolean slide(ServerPlayer p, PlayerStats s, Ability a) {
        int ticks = Math.max(20, a.duration);
        p.getPersistentData().putLong(SLIDE_TAG, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2, false, false, true));
        Vec3 dir = AbilityHandler.flatLook(p);
        p.setDeltaMovement(dir.x * 1.1, -0.1, dir.z * 1.1);
        p.hurtMarked = true;
        Fx.line(p.level(), p.position().add(0, 0.2, 0), dir, 4.5, 0.5, 10, EDGE);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.6f);
        p.displayClientMessage(Component.literal("Скольжение: снаряды идут над головой")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Идёт ли сейчас скольжение (снаряды мимо). */
    public static boolean sliding(ServerPlayer p) {
        return p.level().getGameTime() < p.getPersistentData().getLong(SLIDE_TAG);
    }
}
