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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сплетённые заклинания Астроманта.
 * <p>
 * Десять сочетаний раньше были десятью одинаковыми вспышками разного цвета. Здесь у каждого
 * своя механика и своё время: сверхновая копится и бьёт с задержкой, ноль сковывает намертво,
 * гром перескакивает, град сыплется долго, буря крепчает с каждым прыжком, тяготение сперва
 * стягивает и только потом давит. Сочетание выбирается не ради цвета, а ради того, что оно делает.
 */
public final class Astromancer {
    private static final int FIRE = 0xE0FF8A20;
    private static final int ICE = 0xE09ED8F0;
    private static final int STORM = 0xE0B0C8FF;
    private static final int VOID = 0xD08A30E0;
    /** Сколько секунд цель простояла под затмением. */
    private static final String DEEP_TAG = "litrpg_eclipse";
    /** Сколько раз метель успела прихватить цель. */
    private static final String COLD_TAG = "litrpg_blizzard";

    private Astromancer() {}

    static void register() {
        Special.put("as_supernova", Astromancer::supernova);
        Special.put("as_zero", Astromancer::zero);
        Special.put("as_thunder", Astromancer::thunder);
        Special.put("as_hail", Astromancer::hail);
        Special.put("as_storm", Astromancer::storm);
        Special.put("as_gravity", Astromancer::gravity);
        Special.put("as_comet", Astromancer::comet);
        Special.put("as_flare", Astromancer::flare);
        Special.put("as_eclipse", Astromancer::eclipse);
        Special.put("as_blizzard", Astromancer::blizzard);
    }

    private static Vec3 aim(ServerPlayer p, double range) {
        LivingEntity t = Dynamic.aimEnemy(p, range);
        if (t != null) return t.position();
        Vec3 eye = p.getEyePosition();
        return eye.add(p.getViewVector(1f).scale(range * 0.6));
    }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 c, double r) {
        return AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r));
    }

    /**
     * Сверхновая: точка копит свет полторы секунды, кольцо сжимается — и всё внутри сгорает.
     * Видно заранее, и уйти можно: это не мгновенный сплеш, а решение, где стоять.
     */
    private static boolean supernova(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 30);
        float dmg = AbilityHandler.base(s, a) * a.power;
        double r = a.radius;
        // предупреждение: кольцо стягивается к центру
        Scheduler.repeat(0, 6, 5, i -> {
            double k = r * (1 - i * 0.18);
            Fx.ring(sl, c, k - 0.3, k, 8, FIRE);
            return true;
        });
        Fx.pillar(sl, c, 0.6, 2.5, 30, FIRE);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.4f, 0.5f);
        Scheduler.later(32, () -> {
            Fx.sphere(sl, c.add(0, 1, 0), 0.4, r, 14, FIRE);
            Fx.ring(sl, c, 0.2, r + 1, 18, 0xE0FFE070);
            for (LivingEntity e : around(p, c, r)) {
                AbilityHandler.hurt(p, a, e, dmg);
                e.setSecondsOnFire(6);
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.2f, 0.7f);
        });
        p.displayClientMessage(Component.literal("Сверхновая копится — отойдите от точки!")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /**
     * Абсолютный ноль: цель вмерзает в пол и не может сдвинуться, пока держится лёд,
     * а следующий удар по ней проходит куда больнее.
     */
    private static boolean zero(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, a.radius);
        if (t == null) {
            p.displayClientMessage(Component.literal("Нет цели.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        int ticks = Math.max(20, a.duration);
        AbilityHandler.hurt(p, a, t, AbilityHandler.base(s, a) * a.power);
        t.setTicksFrozen(ticks + 60);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 9, false, false, true));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false, true));
        // лёд держит буквально: каждый тик гасим попытку сдвинуться
        Scheduler.repeat(0, 2, ticks / 2, i -> {
            if (!t.isAlive()) return false;
            t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
            t.hurtMarked = true;
            return true;
        });
        Fx.shield(sl, t, 1.0, ticks, ICE);
        Fx.pillar(sl, t.position(), 0.8, 2.6, ticks, ICE);
        // пока вмёрз — получает больше от всех
        t.addEffect(new MobEffectInstance(com.andrey.litrpg.registry.ModEffects.VULNERABILITY.get(),
                ticks, 1, false, false, true));
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.6f, 0.6f);
        p.displayClientMessage(Component.literal(t.getName().getString() + " вмёрз — бейте, пока держит лёд")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Столп грома: бьёт в цель, а разряд перескакивает на двух ближайших. */
    private static boolean thunder(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, a.radius);
        if (t == null) return false;
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power;
        Fx.pillar(sl, t.position(), 0.7, 14, 14, STORM);
        Fx.ring(sl, t.position(), 0.2, 2.4, 12, STORM);
        AbilityHandler.hurt(p, a, t, dmg);
        List<LivingEntity> near = new ArrayList<>(around(p, t.position(), 7));
        near.remove(t);
        near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(t)));
        LivingEntity from = t;
        for (int i = 0; i < Math.min(2, near.size()); i++) {
            LivingEntity e = near.get(i);
            Fx.beam(sl, from.position().add(0, 1, 0), e.position().add(0, 1, 0), 0.25, 10, STORM);
            AbilityHandler.hurt(p, a, e, dmg * 0.6f);
            from = e;
        }
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.4f, 1.2f);
        return true;
    }

    /** Град: восемь ударов за четыре секунды по площади — стоять там нельзя. */
    private static boolean hail(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 28);
        float dmg = AbilityHandler.base(s, a) * a.power * 0.35f;
        double r = a.radius;
        Fx.ring(sl, c, r - 0.3, r, 90, ICE);
        Scheduler.repeat(0, 10, 8, i -> {
            double ang = i * 2.4;
            Vec3 at = c.add(Math.cos(ang) * r * 0.6, 0, Math.sin(ang) * r * 0.6);
            Fx.pillar(sl, at, 0.35, 6, 8, ICE);
            Fx.ring(sl, at, 0.1, 1.6, 8, ICE);
            for (LivingEntity e : around(p, at, 2.0)) {
                AbilityHandler.hurt(p, a, e, dmg);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            }
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.8f, 1.6f);
            return true;
        });
        return true;
    }

    /** Буря: разряд скачет и с каждым прыжком крепчает — в толпе последний удар самый злой. */
    private static boolean storm(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, a.radius);
        if (t == null) return false;
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power * 0.7f;
        List<LivingEntity> chain = new ArrayList<>();
        LivingEntity cur = t;
        for (int i = 0; i < Math.max(2, a.count); i++) {
            chain.add(cur);
            List<LivingEntity> near = new ArrayList<>(around(p, cur.position(), 8));
            near.removeAll(chain);
            if (near.isEmpty()) break;
            near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(chain.get(chain.size() - 1))));
            cur = near.get(0);
        }
        Vec3 from = p.getEyePosition();
        for (int i = 0; i < chain.size(); i++) {
            LivingEntity e = chain.get(i);
            float grown = dmg * (1 + 0.25f * i);
            Fx.beam(sl, from, e.position().add(0, e.getBbHeight() * 0.6, 0), 0.22 + i * 0.05, 10, STORM);
            Fx.ring(sl, e.position(), 0.2, 1.4 + i * 0.3, 10, STORM);
            AbilityHandler.hurt(p, a, e, grown);
            from = e.position().add(0, e.getBbHeight() * 0.6, 0);
        }
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.1f, 1.4f);
        p.displayClientMessage(Component.literal("Буря прошла по " + chain.size() + " целям")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Тяготение: две секунды стягивает всех в точку, а потом сминает то, что стянуло. */
    private static boolean gravity(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 26);
        float dmg = AbilityHandler.base(s, a) * a.power;
        double r = a.radius;
        Fx.sphere(sl, c.add(0, 1, 0), r, 0.5, 44, VOID);
        Fx.ring(sl, c, r - 0.3, r, 44, VOID);
        Scheduler.repeat(0, 4, 10, i -> {
            for (LivingEntity e : around(p, c, r)) {
                Vec3 pull = c.subtract(e.position()).normalize().scale(0.35);
                e.setDeltaMovement(e.getDeltaMovement().add(pull.x, pull.y * 0.3, pull.z));
                e.hurtMarked = true;
            }
            return true;
        });
        Scheduler.later(44, () -> {
            Fx.sphere(sl, c.add(0, 1, 0), 0.3, 3.0, 12, VOID);
            Fx.ring(sl, c, 0.1, 3.4, 14, 0xE0FFFFFF);
            for (LivingEntity e : around(p, c, 3.4)) {
                AbilityHandler.hurt(p, a, e, dmg);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false, true));
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.2f, 1.4f);
        });
        p.displayClientMessage(Component.literal("Тяготение стягивает — через две секунды схлопнется")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /**
     * Комета: ледяное ядро в огненной оболочке. Падает с неба почти секунду — видно, куда,
     * и видно, что успеешь отойти. На земле оболочка сгорает кольцом, а ядро бьёт в центре
     * и вмораживает: место попадания опаснее края, и это решает, куда бежать.
     */
    private static boolean comet(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 30);
        float dmg = AbilityHandler.base(s, a) * a.power;
        double r = Math.max(3.5, a.radius);
        double core = r * 0.45;
        // куда упадёт: кольцо оболочки и пятно ядра видны сразу
        Fx.ring(sl, c, r - 0.4, r, 20, FIRE);
        Fx.disc(sl, c, core, 0, 20, ICE);
        Scheduler.repeat(0, 2, 9, i -> {
            double h1 = 22 - i * 2.4;
            double h2 = Math.max(0.4, h1 - 2.4);
            Fx.beam(sl, c.add(0, h1, 0), c.add(0, h2, 0), 0.9 - i * 0.05, 4, FIRE);
            Fx.spiralAt(sl, c.add(0, h2, 0), 1.1, 1.6, 1.5, 4, ICE);
            return true;
        });
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2f, 0.5f);
        Scheduler.later(20, () -> {
            Fx.sphere(sl, c.add(0, 1, 0), 0.5, r, 12, FIRE);
            Fx.ring(sl, c, core, r, 14, FIRE);
            Fx.sphere(sl, c.add(0, 1, 0), 0.2, core, 16, ICE);
            for (LivingEntity e : around(p, c, r)) {
                boolean inCore = e.position().distanceTo(c) <= core;
                AbilityHandler.hurt(p, a, e, inCore ? dmg * 1.6f : dmg * 0.7f);
                if (inCore) {
                    // ядро: лёд под огнём — цель стоит и горит одновременно
                    e.setTicksFrozen(260);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5, false, false, true));
                    Fx.pillar(sl, e.position(), 0.7, 2.4, 60, ICE);
                } else {
                    e.setSecondsOnFire(8);
                }
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2f, 0.8f);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.8f, 0.5f);
        });
        p.displayClientMessage(Component.literal("Комета идёт — ядро бьёт в центр")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /**
     * Вспышка: свет бьёт во все стороны разом — и бьёт трижды, всё слабее.
     * Каждый всполох тянет луч к каждому врагу, и чем ближе он стоит, тем больнее:
     * вплотную к Астроманту находиться нельзя, а издали вспышка почти не трогает.
     */
    private static boolean flare(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power;
        double r = Math.max(4, a.radius);
        Scheduler.repeat(0, 16, 3, i -> {
            if (!p.isAlive()) return false;
            float fade = (float) Math.pow(0.7, i);
            Vec3 eye = p.getEyePosition();
            Fx.sphere(sl, eye, 0.3, r, 8, i == 0 ? 0xE0FFF0C0 : FIRE);
            Fx.ring(sl, p.position(), 0.2, r, 8, FIRE);
            for (LivingEntity e : around(p, p.position(), r)) {
                double d = e.position().distanceTo(p.position());
                float near = (float) Math.max(0.25, 1 - d / r);
                AbilityHandler.hurt(p, a, e, dmg * fade * near);
                Fx.beam(sl, eye, e.position().add(0, e.getBbHeight() * 0.6, 0), 0.18, 6, 0xE0FFF0C0);
                if (d <= r * 0.5) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
                }
            }
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.PLAYERS, 1.4f, 1f + i * 0.2f);
            return true;
        });
        p.displayClientMessage(Component.literal("Вспышка: три всполоха, ближним больнее")
                .withStyle(ChatFormatting.YELLOW), true);
        return true;
    }

    /**
     * Затмение: тень накрывает участок, и чем дольше в ней стоять, тем хуже.
     * Каждая секунда под куполом добавляет врагу ступень тьмы, и урон следующей секунды
     * растёт. Шагнул из тени — счёт начинается заново: зона не про урон, а про то,
     * чтобы противник не стоял, где ему удобно.
     */
    private static boolean eclipse(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 26);
        float dmg = AbilityHandler.base(s, a) * a.power * 0.3f;
        double r = Math.max(4, a.radius);
        int ticks = Math.max(60, a.duration);
        Fx.disc(sl, c, r, 0, ticks, VOID);
        Fx.ring(sl, c, r - 0.4, r, ticks, VOID);
        Scheduler.repeat(0, 20, ticks / 20, i -> {
            Fx.sphere(sl, c.add(0, 2.2, 0), r * 0.8, r, 20, VOID);
            Fx.spiralAt(sl, c, r * 0.7, 4.5, 2, 20, VOID);
            List<LivingEntity> inside = around(p, c, r);
            for (LivingEntity e : inside) {
                int deep = e.getPersistentData().getInt(DEEP_TAG) + 1;
                e.getPersistentData().putInt(DEEP_TAG, deep);
                AbilityHandler.hurt(p, a, e, dmg * (1 + 0.35f * Math.min(8, deep - 1)));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
                if (deep >= 3) Fx.mark(sl, e, 0.7, 2, 20, VOID);
            }
            // счёт идёт только тем, кто остался под куполом
            for (LivingEntity e : around(p, c, r + 12)) {
                if (!inside.contains(e)) e.getPersistentData().putInt(DEEP_TAG, 0);
            }
            if (i == 0) {
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.4f, 0.5f);
            }
            return true;
        });
        p.displayClientMessage(Component.literal("Затмение: тьма густеет на тех, кто стоит")
                .withStyle(ChatFormatting.DARK_PURPLE), true);
        return true;
    }

    /**
     * Метель: в ней не видно и из неё не выйти. Ветер возвращает врага к центру — чем
     * ближе он к краю, тем сильнее, — а холод копится: три захода метели, и цель встаёт
     * намертво. Это не зона урона, а ловушка, которая держит чужой отряд на месте.
     */
    private static boolean blizzard(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = aim(p, 26);
        float dmg = AbilityHandler.base(s, a) * a.power * 0.22f;
        double r = Math.max(4, a.radius);
        int ticks = Math.max(60, a.duration);
        Fx.disc(sl, c, r, 0, ticks, ICE);
        Scheduler.repeat(0, 5, ticks / 5, i -> {
            if (i % 4 == 0) {
                Fx.ring(sl, c, r - 0.5, r, 20, ICE);
                Fx.spiralAt(sl, c, r * 0.8, 5, 3, 20, 0xE0FFFFFF);
            }
            for (LivingEntity e : around(p, c, r)) {
                Vec3 off = e.position().subtract(c);
                double d = Math.max(0.1, off.horizontalDistance());
                // ветер к центру: у края он почти не пускает, в середине едва заметен
                double force = 0.08 + 0.3 * (d / r);
                Vec3 back = off.normalize().scale(-force);
                e.setDeltaMovement(e.getDeltaMovement().add(back.x, 0, back.z));
                e.hurtMarked = true;
                if (i % 4 != 0) continue;
                AbilityHandler.hurt(p, a, e, dmg);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0, false, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false, true));
                int cold = e.getPersistentData().getInt(COLD_TAG) + 1;
                e.getPersistentData().putInt(COLD_TAG, cold);
                if (cold < 3) continue;
                // холод добрал: цель встаёт на полторы секунды
                e.getPersistentData().putInt(COLD_TAG, 0);
                e.setTicksFrozen(300);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 9, false, false, true));
                Fx.shield(sl, e, 1.1, 30, ICE);
                Scheduler.repeat(0, 2, 15, j -> {
                    if (!e.isAlive()) return false;
                    e.setDeltaMovement(0, Math.min(0, e.getDeltaMovement().y), 0);
                    e.hurtMarked = true;
                    return true;
                });
                sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
            }
            return true;
        });
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2f, 0.6f);
        p.displayClientMessage(Component.literal("Метель: ветер не пускает наружу")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }
}
