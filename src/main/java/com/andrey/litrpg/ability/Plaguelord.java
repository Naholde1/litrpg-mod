package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Чумной владыка: заражение, которое расходится само.
 * <p>
 * Заражение — это слои (до пяти) на враге; каждый слой понемногу гноит его. Заражённый,
 * умирая, передаёт заразу двум ближайшим. Плевок и облако добавляют слои, «Разрыв» взрывает
 * их все разом — чем больше слоёв, тем страшнее. Увядание запрещает лечиться. Мор заражает
 * всех вокруг сразу.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Plaguelord {
    private static final int SPORE = 0xC080E040;
    private static final int ROT = 0xC0506020;
    /** Слои заражения на враге (значение — число слоёв) и кто заразил. */
    private static final String INFECT = "litrpg_pl_infect";
    private static final String WITHER = "litrpg_pl_wither";
    private static final int MAX = 5;
    private static final int LIFE = 240;

    private Plaguelord() {}

    static void register() {
        Special.put("pl_infect", (p, s, a) -> infectTarget(p, s, a, 2));
        Special.put("pl_spit", Plaguelord::spit);
        Special.put("pl_cloud", Plaguelord::cloud);
        Special.put("pl_burst", Plaguelord::burst);
        Special.put("pl_wither", Plaguelord::wither);
        Special.put("pl_plague", Plaguelord::plague);
    }

    static int stacks(LivingEntity e) {
        return (int) Kit.value(e, INFECT, 0);
    }

    /** Добавить слои заражения. Новое заражение запускает гниение (раз в секунду по слоям). */
    static void infect(ServerPlayer p, LivingEntity e, int add, float perStack) {
        int was = stacks(e);
        int now = Math.min(MAX, was + add);
        Kit.until(e, INFECT, now, LIFE);
        e.getPersistentData().putUUID(INFECT + "_by", p.getUUID());
        e.getPersistentData().putFloat(INFECT + "_dmg", perStack);
        Fx.mark(p.level(), e, 0.25 + 0.08 * now, 1, 40, SPORE);
        if (was > 0) return;
        Ability a = Ability.byKey("pl_infect");
        Scheduler.repeat(20, 20, 60, i -> {
            if (!e.isAlive() || !p.isAlive() || !Kit.active(e, INFECT)) return false;
            Kit.dmg(p, a == null ? Ability.byKey("pl_spit") : a, e, stacks(e) * e.getPersistentData().getFloat(INFECT + "_dmg"));
            if (e.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, e.getX(), e.getY() + 1, e.getZ(), 2 + stacks(e), 0.3, 0.4, 0.3, 0.02);
            }
            return true;
        });
    }

    private static float per(PlayerStats s, Ability a) {
        return AbilityHandler.base(s, a) * 0.08f;
    }

    private static boolean infectTarget(ServerPlayer p, PlayerStats s, Ability a, int n) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        infect(p, t, n, per(s, a) * Math.max(1f, a.power * 1.5f));
        Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, 1, 0), 0.12, 8, SPORE);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.SLIME_SQUISH, 1f, 0.6f);
        return true;
    }

    /** Плевок: урон и слой заражения. */
    private static boolean spit(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        Kit.dmg(p, a, t, Kit.power(s, a));
        infect(p, t, 1, per(s, a));
        Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, 1, 0), 0.1, 6, SPORE);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.LLAMA_SPIT, 1f, 0.8f);
        return true;
    }

    /** Споровое облако: каждую секунду урон и слой заражения всем внутри. */
    private static boolean cloud(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 20);
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.25f;
        Fx.disc(sl, c, a.radius, 0, life, 0x4080E040);
        Fx.spiralAt(sl, c, a.radius * 0.7, 2.5, 2, life, ROT);
        Kit.sound(sl, c, SoundEvents.BREWING_STAND_BREW, 1f, 0.5f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, c.x, c.y + 1, c.z, 30, a.radius / 2, 0.8, a.radius / 2, 0);
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                Kit.dmg(p, a, e, tick);
                infect(p, e, 1, per(s, a));
            }
            return true;
        });
        return true;
    }

    /** Разрыв: все слои на врагах вокруг лопаются разом — урон растёт с числом слоёв, заражение снято. */
    private static boolean burst(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float base = Kit.power(s, a);
        int total = 0;
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius + 4)) {
            int st = stacks(e);
            boolean inside = e.distanceTo(p) <= a.radius;
            if (st == 0 && !inside) continue;
            Kit.dmg(p, a, e, (inside ? base : 0) + base * 0.4f * st);
            Kit.clear(e, INFECT);
            total += st;
            if (st > 0) Fx.burst(sl, e.position().add(0, 1, 0), 1 + 0.3 * st, 6 + st * 2, 12, SPORE);
        }
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 12, ROT);
        Kit.sound(sl, p.position(), SoundEvents.SLIME_BLOCK_BREAK, 1.6f, 0.5f);
        if (total > 0) Kit.msg(p, "Разорвано слоёв заразы: " + total, ChatFormatting.GREEN);
        return true;
    }

    /** Увядание: цель гниёт — получает больше, не может лечиться и получает два слоя заразы. */
    private static boolean wither(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        Kit.until(t, WITHER, life);
        infect(p, t, 2, per(s, a));
        Fx.mark(p.level(), t, 0.7, 1, life, ROT);
        Fx.spiral(p.level(), t, 0.6, t.getBbHeight(), 2, life, SPORE);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.WITHER_SHOOT, 0.6f, 0.6f);
        return true;
    }

    /** Мор: всех врагов вокруг — урон и три слоя заразы разом. */
    private static boolean plague(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            Kit.dmg(p, a, e, dmg);
            infect(p, e, 3, per(s, a));
            Fx.pillar(sl, e.position(), 0.5, 3, 16, SPORE);
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 20, SPORE, 1f);
        Fx.runes(sl, p.position(), 4, 5, 30, ROT);
        Kit.sound(sl, p.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 1f, 0.5f);
        return true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity dead = e.getEntity();
        int st = stacks(dead);
        if (st <= 0 || !(dead.level() instanceof ServerLevel sl) || !dead.getPersistentData().hasUUID(INFECT + "_by")) return;
        if (!(sl.getEntity(dead.getPersistentData().getUUID(INFECT + "_by")) instanceof ServerPlayer p)) return;
        float per = dead.getPersistentData().getFloat(INFECT + "_dmg");
        int jumped = 0;
        for (LivingEntity n : Kit.enemiesIn(p, dead.position(), 6)) {
            if (n == dead || jumped >= 2) continue;
            infect(p, n, Math.max(1, st - 1), per);
            Fx.beam(sl, dead.position().add(0, 1, 0), n.position().add(0, 1, 0), 0.12, 10, SPORE);
            jumped++;
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, dead.getX(), dead.getY() + 1, dead.getZ(), 30, 0.6, 0.6, 0.6, 0);
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent e) {
        if (Kit.active(e.getEntity(), WITHER)) e.setCanceled(true);
    }
}
