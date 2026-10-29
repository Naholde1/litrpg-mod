package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Монах: печати по порядку.
 * <p>
 * Первая печать висит в воздухе и бьёт. Вторая рядом с первой — в полтора раза сильнее и
 * стягивает врагов к первой. Третья, если обе прежние ещё горят, смыкает круг: все три
 * печати взрываются разом и оглушают. Ладонь — волна ци по прямой, которая проходит сквозь
 * всех. Сто ладоней — восемь волн в одну цель, последняя тяжелее. Гора падает — ладонь ци
 * с неба: сначала тень на полу, потом удар.
 */
public final class Monk {
    private static final int CHI = 0xE0FFC040;
    private static final int SEAL = 0xD0FFE080;
    private static final String[] SEALS = {"litrpg_mk_s1", "litrpg_mk_s2", "litrpg_mk_s3"};

    private Monk() {}

    static void register() {
        Special.put("mk_seal_one", (p, s, a) -> seal(p, s, a, 0));
        Special.put("mk_seal_two", (p, s, a) -> seal(p, s, a, 1));
        Special.put("mk_seal_three", (p, s, a) -> seal(p, s, a, 2));
        Special.put("mk_palm", Monk::palm);
        Special.put("mk_hundred", Monk::hundred);
        Special.put("mk_mountain", Monk::mountain);
    }

    private static Vec3 sealAt(ServerPlayer p, int i) {
        var d = p.getPersistentData();
        if (!Kit.active(p, SEALS[i])) return null;
        return new Vec3(d.getDouble(SEALS[i] + "x"), d.getDouble(SEALS[i] + "y"), d.getDouble(SEALS[i] + "z"));
    }

    private static boolean seal(ServerPlayer p, PlayerStats s, Ability a, int idx) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 16);
        int life = Kit.dur(s, a);
        float mult = 1f;
        Vec3 first = sealAt(p, 0), second = sealAt(p, 1);
        if (idx == 2 && first != null && second != null) {
            closeCircle(p, s, a, first, second, c);
            return true;
        }
        if (idx == 1 && first != null && first.distanceTo(c) < 10) {
            mult = 1.5f;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius + 1)) Kit.pull(e, first, 0.8);
            Fx.beam(sl, c.add(0, 1.5, 0), first.add(0, 1.5, 0), 0.15, life, SEAL);
            Kit.msg(p, "Вторая печать рядом с первой: сильнее и стягивает врагов.", ChatFormatting.GOLD);
        }
        Kit.until(p, SEALS[idx], life);
        var d = p.getPersistentData();
        d.putDouble(SEALS[idx] + "x", c.x);
        d.putDouble(SEALS[idx] + "y", c.y);
        d.putDouble(SEALS[idx] + "z", c.z);
        float tick = Kit.power(s, a) * 0.35f * mult;
        Fx.runes(sl, c.add(0, 1.5, 0), a.radius * 0.5, 3 + idx * 2 + 1, life, SEAL);
        Fx.runes(sl, c, a.radius, 3 + idx * 2 + 1, life, 0x80FFC040);
        Kit.sound(sl, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f + idx * 0.2f);
        Scheduler.repeat(10, 20, life / 20, i -> {
            if (!p.isAlive() || !Kit.active(p, SEALS[idx])) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, tick);
                AbilityHandler.applyFoe(a, e, p);
            }
            Fx.ring(sl, c.add(0, 0.1, 0), 0.3, a.radius, 10, CHI, 0.3f);
            return true;
        });
        return true;
    }

    /** Третья печать при двух горящих: все три взрываются, по врагам у каждой — удар и оглушение. */
    private static void closeCircle(ServerPlayer p, PlayerStats s, Ability a, Vec3 one, Vec3 two, Vec3 three) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a) * 1.5f;
        Vec3[] pts = {one, two, three};
        for (int i = 0; i < 3; i++) Fx.beam(sl, pts[i].add(0, 1.5, 0), pts[(i + 1) % 3].add(0, 1.5, 0), 0.3, 16, CHI);
        Kit.clear(p, SEALS[0]);
        Kit.clear(p, SEALS[1]);
        Scheduler.later(8, () -> {
            for (Vec3 c : pts) {
                Fx.sphere(sl, c.add(0, 1, 0), 0.5, a.radius + 1, 12, CHI);
                Fx.burst(sl, c.add(0, 1.5, 0), a.radius, 16, 12, SEAL);
                for (LivingEntity e : Kit.enemiesIn(p, c, a.radius + 1)) {
                    Kit.dmg(p, a, e, dmg);
                    Kit.stun(e, 30);
                }
            }
            Kit.sound(sl, three, SoundEvents.BELL_RESONATE, 2f, 1.2f);
        });
        Kit.msg(p, "Круг сомкнулся!", ChatFormatting.GOLD);
    }

    /** Ладонь: волна ци по прямой на всю дальность — бьёт всех на пути. */
    private static boolean palm(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        Vec3 from = p.position();
        Vec3 to = from.add(dir.scale(a.radius));
        float dmg = Kit.power(s, a);
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(from, to).inflate(1.2, 1.5, 1.2))) {
            Vec3 rel = e.position().subtract(from);
            double along = rel.dot(dir);
            if (along < -0.5 || along > a.radius + 0.5 || rel.subtract(dir.scale(along)).multiply(1, 0, 1).length() > 1.4) continue;
            Kit.dmg(p, a, e, dmg);
        }
        Fx.wave(sl, from, dir, a.radius, 1.3, 1.6, 8, CHI);
        Kit.sound(sl, from, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 1.2f);
        return true;
    }

    /** Сто ладоней: восемь волн ци в одну цель подряд; последняя — вдвое. */
    private static boolean hundred(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius + 2);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int n = Math.max(4, a.count);
        float dmg = Kit.power(s, a) * 0.35f;
        Scheduler.repeat(0, 3, n, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            Vec3 off = new Vec3((sl.random.nextDouble() - 0.5) * 0.8, 1 + (sl.random.nextDouble() - 0.5) * 0.6, (sl.random.nextDouble() - 0.5) * 0.8);
            Fx.beam(sl, p.position().add(0, 1.2, 0), t.position().add(off), 0.18, 5, CHI);
            Fx.burst(sl, t.position().add(off), 0.8, 5, 5, SEAL);
            Kit.dmg(p, a, t, dmg * (i == n - 1 ? 2f : 1f));
            Kit.sound(sl, t.position(), SoundEvents.PLAYER_ATTACK_WEAK, 1f, 1f + i * 0.1f);
            return true;
        });
        return true;
    }

    /** Гора падает: тень ладони на полу, через секунду — удар с неба. */
    private static boolean mountain(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 24);
        float dmg = Kit.power(s, a);
        Fx.disc(sl, c, a.radius, 0, 20, 0x90FFC040);
        Fx.runes(sl, c, a.radius, 5, 24, SEAL);
        Kit.sound(sl, c, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.5f);
        Scheduler.later(20, () -> {
            if (!p.isAlive()) return;
            Fx.pillar(sl, c, a.radius * 0.8, 12, 14, CHI);
            Fx.shards(sl, c, a.radius, 14, 1.4, 20, SEAL);
            Fx.ring(sl, c, 0.5, a.radius + 2, 12, CHI, 0.8f);
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, dmg);
                Kit.stun(e, 20);
            }
            Kit.sound(sl, c, SoundEvents.GENERIC_EXPLODE, 1.6f, 0.6f);
        });
        return true;
    }
}
