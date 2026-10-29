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
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Законник: законы меняют правила боя, а суд выносит приговор.
 * <p>
 * Суд останавливает подсудимых — и всё, что по ним успели нанести за время суда, в конце
 * взыскивается ещё раз наполовину. Приговорённый получает больше, а если умрёт до срока,
 * отряд получает «возмещение»: лечение и печать законнику. Изгнание выталкивает за круг,
 * и вернувшийся раньше срока платит. Повестка тянет всех к законнику цепями.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Lawgiver {
    private static final int GOLD = 0xE0F0D060;
    private static final int WHITE = 0xD0FFF8E0;
    private static final String VERDICT = "litrpg_lw_verdict";
    private static final String EXILE = "litrpg_lw_exile";

    private Lawgiver() {}

    static void register() {
        Special.put("lw_court", Lawgiver::court);
        Special.put("lw_verdict", Lawgiver::verdict);
        Special.put("lw_exile", Lawgiver::exile);
        Special.put("lw_summon", Lawgiver::summons);
        Special.put("lw_codex", Lawgiver::codex);
    }

    /** Суд: подсудимые в круге застывают; в конце с них взыскивается половина того, что они получили за суд. */
    private static boolean court(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        LivingEntity center = Kit.enemy(p, 20);
        Vec3 c = center != null ? center.position() : AbilityHandler.lookPoint(p, 16);
        List<LivingEntity> accused = Kit.enemiesIn(p, c, a.radius);
        if (accused.isEmpty()) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Map<LivingEntity, Float> hp = new HashMap<>();
        for (LivingEntity e : accused) {
            hp.put(e, e.getHealth());
            Kit.stun(e, life);
            Fx.chains(sl, p, e, 0.35, life, GOLD);
        }
        Fx.runes(sl, c, a.radius, 7, life, GOLD);
        Fx.pillar(sl, c, a.radius, 4, life, 0x30F0D060);
        Kit.sound(sl, c, SoundEvents.BELL_BLOCK, 1.5f, 0.6f);
        Kit.msg(p, "Суд идёт: подсудимых " + accused.size() + ". Бейте — приговор удвоит наказание.", ChatFormatting.GOLD);
        Scheduler.later(life, () -> {
            Kit.sound(sl, c, SoundEvents.ANVIL_LAND, 1.4f, 0.5f);
            for (var en : hp.entrySet()) {
                LivingEntity e = en.getKey();
                if (!e.isAlive()) continue;
                float lost = Math.max(0, en.getValue() - e.getHealth());
                Fx.pillar(sl, e.position(), 0.6, 6, 12, WHITE);
                Fx.burst(sl, e.position().add(0, 1, 0), 2, 10, 10, GOLD);
                if (lost > 0 && p.isAlive()) Kit.dmg(p, a, e, lost * 0.5f);
            }
        });
        return true;
    }

    /** Приговор: виновный получает больше; умрёт до срока — отряду лечение, законнику печать. */
    private static boolean verdict(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        Kit.until(t, VERDICT, life);
        t.getPersistentData().putUUID(VERDICT + "_by", p.getUUID());
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, life, 0, false, false));
        Fx.mark(p.level(), t, 0.8, 2, life, GOLD);
        Fx.pillar(p.level(), t.position(), 0.7, 5, 14, WHITE);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.BELL_BLOCK, 1.2f, 1.2f);
        return true;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || !Kit.active(t, VERDICT) || !t.getPersistentData().hasUUID(VERDICT + "_by")) return;
        if (!(t.level() instanceof ServerLevel sl) || !(sl.getEntity(t.getPersistentData().getUUID(VERDICT + "_by")) instanceof ServerPlayer p)) return;
        PlayerStats s = StatsManager.get(p);
        s.setResource(s.getResource() + 1);
        StatsManager.syncMana(p);
        for (LivingEntity al : Kit.alliesIn(p, p.position(), 20)) {
            Kit.heal(p, al, al.getMaxHealth() * 0.1f);
            Fx.pillar(sl, al.position(), 0.5, 3, 12, GOLD);
        }
        Kit.msg(p, "Приговор исполнен: отряду возмещение, вам — печать.", ChatFormatting.GOLD);
    }

    /** Изгнание: выдворяет всех из круга; вернувшийся в круг раньше срока получает урон и замедление. */
    private static boolean exile(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        double r = a.radius;
        float dmg = Kit.power(s, a);
        for (LivingEntity e : Kit.enemiesIn(p, c, r)) {
            Kit.dmg(p, a, e, dmg);
            Kit.push(e, c, 1.4, 0.35);
            Kit.until(e, EXILE, 100);
        }
        Fx.ring(sl, c, 0.5, r, 12, GOLD, 0.8f);
        Fx.runes(sl, c, r, 5, 100, 0x90F0D060);
        Kit.sound(sl, c, SoundEvents.EVOKER_CAST_SPELL, 1f, 1.4f);
        Scheduler.repeat(10, 10, 9, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, r)) {
                if (!Kit.active(e, EXILE)) continue;
                Kit.dmg(p, a, e, dmg * 0.4f);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                Kit.push(e, c, 0.8, 0.2);
                Fx.pillar(sl, e.position(), 0.5, 3, 8, WHITE);
            }
            return true;
        });
        return true;
    }

    /** Повестка: цепи от законника к каждому врагу рядом — их тянет на шаг ближе, и все бьют законника. */
    private static boolean summons(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        AbilityHandler.applySelf(a, p);
        int n = 0;
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) {
            AbilityHandler.applyFoe(a, e, p);
            Kit.taunt(p, e);
            Kit.pull(e, p.position(), 0.6);
            Fx.chains(sl, p, e, 0.35, 20, GOLD);
            n++;
        }
        Fx.runes(sl, p.position(), 2.2, 5, 30, GOLD);
        Kit.sound(sl, p.position(), SoundEvents.BELL_BLOCK, 1f, 0.9f);
        if (n == 0) Kit.msg(p, "Явиться некому.", ChatFormatting.GRAY);
        return true;
    }

    /** Полный свод: бафы из данных всем вокруг и круг закона у каждого под ногами. */
    private static boolean codex(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            AbilityHandler.applySelf(a, e);
            Fx.runes(sl, e.position(), 1.3, 5, 60, GOLD);
            Fx.pillar(sl, e.position(), 0.6, 3.5, 20, WHITE);
        }
        Fx.ring(sl, p.position(), 0.5, a.radius, 18, GOLD, 0.6f);
        Kit.sound(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.8f);
        return true;
    }
}
