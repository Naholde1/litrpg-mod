package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Хронолог: время как ресурс отряда.
 * <p>
 * Система помнит здоровье каждого игрока за последние секунды, и «Полный откат» возвращает
 * отряд туда, где он был пять секунд назад (если тогда было лучше). Остановка замораживает
 * врагов и даже снаряды в воздухе. Замедление вязнет — и враги, и их стрелы. Ускорить
 * сокращает идущие откаты союзников. Эхо в конце повторяет часть лечения. Петля времени
 * через три секунды повторяет себя.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Chronologist {
    private static final int TIME = 0xD0C0A8FF;
    private static final int SAND = 0xD0F0D890;
    /** Здоровье игрока по секундам: последние 6 значений. */
    private static final String HIST = "litrpg_ch_hist";

    private Chronologist() {}

    static void register() {
        Special.put("ch_rollback", Chronologist::rollback);
        Special.put("ch_stop", Chronologist::stop);
        Special.put("ch_slow", Chronologist::slow);
        Special.put("ch_haste", Chronologist::hasten);
        Special.put("ch_echo", Chronologist::echo);
        Special.put("ch_loop", Chronologist::loop);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        float[] h = history(p);
        System.arraycopy(h, 0, h, 1, h.length - 1);
        h[0] = p.getHealth();
        int[] bits = new int[h.length];
        for (int i = 0; i < h.length; i++) bits[i] = Float.floatToIntBits(h[i]);
        p.getPersistentData().putIntArray(HIST, bits);
    }

    private static float[] history(LivingEntity p) {
        int[] bits = p.getPersistentData().getIntArray(HIST);
        float[] h = new float[6];
        for (int i = 0; i < Math.min(bits.length, h.length); i++) h[i] = Float.intBitsToFloat(bits[i]);
        return h;
    }

    /** Полный откат: лечит всех и возвращает каждому игроку здоровье пятисекундной давности, если оно было больше. */
    private static boolean rollback(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float heal = Kit.power(s, a) * AbilityHandler.healMult(s) * 0.5f;
        int restored = 0;
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            float before = e.getHealth();
            if (e instanceof ServerPlayer sp) {
                float past = history(sp)[5];
                if (past > e.getHealth()) {
                    e.setHealth(Math.min(e.getMaxHealth(), past));
                    restored++;
                }
            }
            Kit.heal(p, e, heal);
            Fx.spiral(sl, e, 0.8, 2.2, -3, 20, TIME);
            if (e.getHealth() > before + 1) Fx.pillar(sl, e.position(), 0.5, 3, 14, SAND);
        }
        Fx.runes(sl, p.position(), a.radius * 0.5, 12, 30, TIME);
        Fx.ring(sl, p.position(), a.radius, 0.5, 20, SAND, 0.6f);
        Kit.sound(sl, p.position(), SoundEvents.BEACON_DEACTIVATE, 1.4f, 1.6f);
        if (restored > 0) Kit.msg(p, "Время отмотано назад для " + restored + " бойцов", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** Остановка: враги в круге застывают, снаряды в воздухе повисают и падают, когда время пойдёт. */
    private static boolean stop(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 18);
        int life = Kit.dur(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
            Kit.stun(e, life);
            Fx.mark(sl, e, 0.5, 2, life, TIME);
        }
        List<Projectile> frozen = new ArrayList<>(Kit.hostileShots(sl, new AABB(c, c).inflate(a.radius + 2)));
        for (Projectile pr : frozen) pr.setNoGravity(true);
        Fx.sphere(sl, c.add(0, 1, 0), a.radius, a.radius, life, 0x40C0A8FF);
        Fx.runes(sl, c, a.radius, 12, life, TIME);
        Kit.sound(sl, c, SoundEvents.BELL_RESONATE, 1.5f, 0.5f);
        Scheduler.repeat(0, 1, life, i -> {
            for (Projectile pr : frozen) if (pr.isAlive()) pr.setDeltaMovement(Vec3.ZERO);
            for (Projectile pr : Kit.hostileShots(sl, new AABB(c, c).inflate(a.radius))) {
                if (!frozen.contains(pr)) {
                    pr.setNoGravity(true);
                    frozen.add(pr);
                }
            }
            return true;
        });
        Scheduler.later(life + 1, () -> {
            for (Projectile pr : frozen) {
                if (!pr.isAlive()) continue;
                pr.setNoGravity(false);
                pr.setDeltaMovement(0, -0.2, 0);
            }
        });
        return true;
    }

    /** Замедлить: круг вязкого времени — враги вязнут, бьют медленнее и слабее, снаряды в нём ползут. */
    private static boolean slow(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 20);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.5f;
        Fx.disc(sl, c, a.radius, 0, life, 0x30C0A8FF);
        Fx.runes(sl, c, a.radius, 8, life, TIME);
        Kit.sound(sl, c, SoundEvents.BELL_BLOCK, 1f, 0.4f);
        Scheduler.repeat(0, 5, life / 5, i -> {
            if (!p.isAlive()) return false;
            for (Projectile pr : Kit.hostileShots(sl, new AABB(c, c).inflate(a.radius))) {
                if (pr.position().distanceTo(c) <= a.radius) pr.setDeltaMovement(pr.getDeltaMovement().scale(0.6));
            }
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
                e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 2));
                Kit.until(e, Kit.DEALT, 0.8f, 10);
                if (i % 4 == 0) Kit.dmg(p, a, e, tick);
            }
            return true;
        });
        return true;
    }

    /** Ускорить: бафы из данных, и все идущие откаты союзников-игроков короче на 3 секунды. */
    private static boolean hasten(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            AbilityHandler.applySelf(a, e);
            Fx.spiral(sl, e, 0.7, 2.2, 4, 20, SAND);
            if (!(e instanceof ServerPlayer sp)) continue;
            PlayerStats st = StatsManager.get(sp);
            for (Ability ab : Ability.values()) {
                long end = st.getCooldownEnd(ab);
                if (end > now) st.setCooldownEnd(ab, Math.max(now, end - 60));
            }
            StatsManager.sync(sp);
        }
        Kit.sound(sl, p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.8f);
        return true;
    }

    /** Эхо: лечение со временем, а в конце треть всего лечения повторяется разом. */
    private static boolean echo(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        int secs = Math.max(2, Kit.dur(s, a) / 20);
        float total = Dynamic.healAmount(s, a, 1f);
        float per = total / secs;
        ServerLevel sl = p.serverLevel();
        Fx.mark(sl, t, 0.35, 3, secs * 20, TIME);
        Scheduler.repeat(20, 20, secs + 1, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            if (i < secs) {
                Kit.heal(p, t, per);
                return true;
            }
            Kit.heal(p, t, total * 0.33f);
            Fx.ring(sl, t.position(), 0.2, 1.8, 12, TIME);
            Fx.spiral(sl, t, 0.6, 2, -2, 14, SAND);
            Kit.sound(sl, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.4f);
            return false;
        });
        return true;
    }

    /** Петля времени: сильное лечение сейчас — и через три секунды оно повторяется на 40%. */
    private static boolean loop(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        float heal = Dynamic.healAmount(s, a, 1f);
        ServerLevel sl = p.serverLevel();
        Kit.heal(p, t, heal);
        Fx.runes(sl, t.position(), 1.4, 6, 60, TIME);
        Fx.pillar(sl, t.position(), 0.8, 3, 14, SAND);
        Kit.sound(sl, t.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 1.6f);
        Scheduler.later(60, () -> {
            if (!t.isAlive() || !p.isAlive()) return;
            Kit.heal(p, t, heal * 0.4f);
            Fx.pillar(sl, t.position(), 0.6, 2.5, 12, TIME);
            Kit.sound(sl, t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.8f);
        });
        return true;
    }
}
