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
 * Оборотень: одни и те же кнопки в разных обликах бьют по-разному.
 * <p>
 * Когти волка — два быстрых взмаха, медведя — тяжёлая лапа по всем перед собой, ворона —
 * клевок издалека. Прыжок волка рвёт, медведя — сносит. Рвать — рана с новыми слоями при
 * каждом применении (до трёх). Вой пугает слабых и злит сильных. Звериный раж крутится
 * дольше, если облик надет.
 */
public final class Werebeast {
    private static final int WILD = 0xD0C08040;
    private static final int BLOOD = 0xE0C02020;
    private static final int SKY = 0xC0A0B0FF;
    private static final String REND = "litrpg_ws_rend";

    private Werebeast() {}

    static void register() {
        Special.put("ws_claw", Werebeast::claw);
        Special.put("ws_pounce", Werebeast::pounce);
        Special.put("ws_rend", Werebeast::rend);
        Special.put("ws_howl", Werebeast::howl);
        Special.put("ws_frenzy", Werebeast::frenzy);
    }

    private static String form(PlayerStats s) {
        String f = s.getForm();
        return f == null ? "" : f;
    }

    /** Когти: волк — два быстрых взмаха, медведь — лапа по всем перед собой, ворон — клевок до 10 блоков. */
    private static boolean claw(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        String f = form(s);
        if (f.equals("ws_raven")) {
            LivingEntity t = Kit.enemy(p, 10);
            if (t == null) return AbilityHandler.noTarget(p);
            Fx.beam(sl, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.7, 0), 0.12, 6, SKY);
            Kit.dmg(p, a, t, dmg * 0.9f);
            Kit.sound(sl, t.position(), SoundEvents.PARROT_HURT, 1f, 0.6f);
            return true;
        }
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        Vec3 dir = AbilityHandler.flatLook(p);
        if (f.equals("ws_bear")) {
            for (LivingEntity e : Kit.enemiesIn(p, p.position().add(dir.scale(1.5)), 2.8)) Kit.dmg(p, a, e, dmg * 1.3f);
            Fx.slash(sl, p.position().add(0, 1.1, 0), dir, 3, 150, 60, 10, WILD);
            Fx.ring(sl, p.position().add(dir.scale(2)), 0.2, 2.5, 8, WILD);
            Kit.sound(sl, p.position(), SoundEvents.POLAR_BEAR_WARNING, 1f, 0.8f);
            return true;
        }
        int hits = f.equals("ws_wolf") ? 2 : 1;
        Scheduler.repeat(0, 4, hits, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            Kit.dmg(p, a, t, dmg * (hits == 2 ? 0.65f : 1f));
            Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), dir, 1.6, 100, i == 0 ? 50 : -50, 6, BLOOD);
            Kit.sound(sl, t.position(), SoundEvents.WOLF_GROWL, 0.6f, 1.4f);
            return true;
        });
        return true;
    }

    /** Прыжок: на цель под прицелом. Волк вешает кровотечение, медведь сшибает и оглушает. */
    private static boolean pounce(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, 12);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 to = t.position().subtract(t.position().subtract(p.position()).multiply(1, 0, 1).normalize().scale(1.2));
        p.teleportTo(to.x, t.getY(), to.z);
        Fx.wave(sl, from, to.subtract(from), from.distanceTo(to), 0.6, 1.2, 8, WILD);
        float dmg = Kit.power(s, a);
        Kit.dmg(p, a, t, dmg);
        String f = form(s);
        if (f.equals("ws_bear")) {
            Kit.stun(t, 30);
            Kit.push(t, p.position(), 0.9, 0.3);
            Fx.burst(sl, t.position().add(0, 1, 0), 2, 12, 10, WILD);
        } else if (f.equals("ws_wolf")) {
            addRend(p, s, a, t, dmg * 0.15f);
        }
        Kit.sound(sl, t.position(), SoundEvents.WOLF_GROWL, 1.2f, 0.7f);
        return true;
    }

    /** Рвать: рана с новым слоем при каждом применении (до трёх) — каждый слой кровит отдельно. */
    private static boolean rend(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        addRend(p, s, a, t, Kit.power(s, a) * 0.2f);
        Fx.slash(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.5, 0), AbilityHandler.flatLook(p), 1.6, 140, 70, 8, BLOOD);
        return true;
    }

    private static void addRend(ServerPlayer p, PlayerStats s, Ability a, LivingEntity t, float tick) {
        int prev = (int) Kit.value(t, REND, 0);
        int stacks = Math.min(3, prev + 1);
        int life = Kit.dur(s, a);
        Kit.until(t, REND, stacks, life);
        Fx.mark(p.level(), t, 0.3 + 0.15 * stacks, 1, life, BLOOD);
        Kit.msg(p, "Рваная рана: слоёв " + stacks, ChatFormatting.RED);
        // третий слой — предел: дальше только продлевается срок
        if (prev >= 3) return;
        Scheduler.repeat(20, 20, life / 20, i -> {
            if (!t.isAlive() || !p.isAlive() || !Kit.active(t, REND)) return false;
            Kit.dmg(p, a, t, tick);
            return true;
        });
    }

    /** Вой: слабых (меньше половины жизни) обращает в бегство, сильных злит на себя и ослабляет. */
    private static boolean howl(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int fled = 0;
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            AbilityHandler.applyFoe(a, e, p);
            if (!Kit.boss(e) && e.getHealth() < e.getMaxHealth() / 2) {
                SkillEffects.charm(e, 80);
                Kit.push(e, p.position(), 1.0, 0.2);
                fled++;
            } else {
                Kit.taunt(p, e);
            }
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 14, WILD, 0.8f);
        Fx.ring(sl, p.position(), 0.5, a.radius * 0.6, 10, WILD, 0.5f);
        Kit.sound(sl, p.position(), SoundEvents.WOLF_HOWL, 1.6f, 0.8f);
        if (fled > 0) Kit.msg(p, "Разбежались от воя: " + fled, ChatFormatting.GOLD);
        return true;
    }

    /** Звериный раж: обороты вокруг себя; в облике — на два больше и с вампиризмом. */
    private static boolean frenzy(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        boolean formed = !form(s).isEmpty();
        int spins = Math.max(2, a.count) + (formed ? 2 : 0);
        float dmg = Kit.power(s, a);
        if (formed) Kit.until(p, Kit.LEECH, 0.15f, spins * 6 + 10);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, spins * 6 + 10, 1, false, false, true));
        Scheduler.repeat(0, 6, spins, i -> {
            if (!p.isAlive()) return false;
            Fx.slash(sl, p.position().add(0, 1, 0), AbilityHandler.flatLook(p).yRot(i * 2.1f), a.radius, 360, i % 2 == 0 ? 20 : -20, 6, i % 2 == 0 ? WILD : BLOOD);
            for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) Kit.dmg(p, a, e, dmg);
            Kit.sound(sl, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.9f, 0.8f + i * 0.1f);
            return true;
        });
        return true;
    }
}
