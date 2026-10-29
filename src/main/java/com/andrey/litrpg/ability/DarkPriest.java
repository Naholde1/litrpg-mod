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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Теневой жрец: лечит тем, что отнял у врага.
 * <p>
 * Плеть тьмы бьёт врага и переливает половину нанесённого самому раненому. Проклятие делает
 * то же каждую секунду. Сумерки — круг, где урон по врагам становится лечением своих.
 * Сделка — жрец платит своей кровью вдвое за чужую. Искупление лечит всех и добавляет
 * треть урона, нанесённого жрецом за последние секунды. Покров прячет союзника от взглядов.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DarkPriest {
    private static final int DUSK = 0xD07040C0;
    private static final int MEND = 0xC0C080FF;
    private static final String POOL = "litrpg_dp_pool";
    private static final String POOL_T = "litrpg_dp_pool_t";

    private DarkPriest() {}

    static void register() {
        Special.put("dp_lash", DarkPriest::lash);
        Special.put("dp_curse", DarkPriest::curse);
        Special.put("dp_zone", DarkPriest::twilight);
        Special.put("dp_pact", DarkPriest::bargain);
        Special.put("dp_atone", DarkPriest::atone);
        Special.put("dp_veil", DarkPriest::veil);
    }

    /** Самый раненый свой рядом (или сам жрец). */
    private static LivingEntity wounded(ServerPlayer p, double r) {
        LivingEntity best = p;
        float worst = p.getHealth() / p.getMaxHealth();
        for (LivingEntity e : Kit.alliesIn(p, p.position(), r)) {
            float f = e.getHealth() / e.getMaxHealth();
            if (f < worst) {
                worst = f;
                best = e;
            }
        }
        return best;
    }

    /** Урон по врагу и половину нанесённого — лечением самому раненому. */
    private static void transfuse(ServerPlayer p, PlayerStats s, Ability a, LivingEntity t, float dmg, float share) {
        float before = t.getHealth();
        Kit.dmg(p, a, t, dmg);
        float done = Math.max(0, before - t.getHealth());
        if (done <= 0) return;
        LivingEntity w = wounded(p, 20);
        Kit.heal(p, w, done * share * AbilityHandler.healMult(s));
        Fx.beam(p.level(), t.position().add(0, 1, 0), w.position().add(0, 1, 0), 0.08, 10, MEND);
    }

    private static boolean lash(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.14, 8, DUSK);
        transfuse(p, s, a, t, Kit.power(s, a), 0.5f);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.WITHER_SHOOT, 0.5f, 1.6f);
        return true;
    }

    /** Проклятие слабости: каждую секунду урон и половина его — лечением самому раненому. */
    private static boolean curse(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float tick = Kit.power(s, a) * 2f / secs;
        Fx.mark(p.level(), t, 0.6, 1, secs * 20, DUSK);
        Fx.spiral(p.level(), t, 0.6, t.getBbHeight(), 2, secs * 20, DUSK);
        AbilityHandler.applyFoe(a, t, p);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            transfuse(p, s, a, t, tick, 0.5f);
            return true;
        });
        return true;
    }

    /** Сумерки: круг на 10 с — урон по врагам внутри каждую секунду, и всё нанесённое лечит своих внутри. */
    private static boolean twilight(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.5f;
        Fx.runes(sl, c, a.radius, 5, life, DUSK);
        Fx.sphere(sl, c, a.radius, a.radius, life, 0x30402060);
        Kit.sound(sl, c, SoundEvents.SCULK_CATALYST_BLOOM, 1.5f, 0.6f);
        Scheduler.repeat(0, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            float total = 0;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                float before = e.getHealth();
                Kit.dmg(p, a, e, tick);
                total += Math.max(0, before - e.getHealth());
            }
            var allies = Kit.alliesIn(p, c, a.radius);
            for (LivingEntity e : allies) {
                AbilityHandler.applySelf(a, e);
                if (total > 0) Kit.heal(p, e, total / allies.size() * AbilityHandler.healMult(s));
            }
            return true;
        });
        return true;
    }

    /** Сделка: жрец отдаёт 15% своего здоровья — союзник получает вдвое больше. */
    private static boolean bargain(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.allyTarget(p, 20, false);
        if (t == null) return AbilityHandler.noTarget(p);
        float pay = p.getMaxHealth() * 0.15f;
        if (p.getHealth() <= pay + 1) {
            Kit.msg(p, "Слишком мало крови для сделки.", ChatFormatting.DARK_RED);
            return false;
        }
        p.setHealth(p.getHealth() - pay);
        Kit.heal(p, t, pay * 2 * AbilityHandler.healMult(s));
        Fx.beam(p.level(), p.position().add(0, 1, 0), t.position().add(0, 1, 0), 0.3, 12, DUSK);
        Fx.spiral(p.level(), t, 0.7, 2, 2, 16, MEND);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.SOUL_ESCAPE, 1.2f, 0.6f);
        return true;
    }

    /** Искупление: лечит всех вокруг и добавляет каждому треть урона, нанесённого жрецом за ~10 секунд. */
    private static boolean atone(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float pool = pool(p);
        float heal = Dynamic.healAmount(s, a, 1f) + pool * 0.33f;
        p.getPersistentData().putFloat(POOL, 0);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            Kit.heal(p, e, heal);
            Fx.pillar(sl, e.position(), 0.6, 4, 18, MEND);
        }
        Fx.runes(sl, p.position(), 4, 7, 30, DUSK);
        Kit.sound(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.6f);
        if (pool > 1) Kit.msg(p, String.format("Искупление: +%.0f к лечению от пролитой крови", pool * 0.33f), ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    private static float pool(LivingEntity p) {
        var d = p.getPersistentData();
        long dt = Kit.now(p) - d.getLong(POOL_T);
        return (float) (d.getFloat(POOL) * Math.exp(-dt / 200.0));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p == e.getEntity()) return;
        var st = StatsManager.get(p);
        if (st.getPlayerClass() == null || !"darkpriest".equals(st.getPlayerClass().key)) return;
        p.getPersistentData().putFloat(POOL, Math.min(p.getMaxHealth() * 4, pool(p) + e.getAmount()));
        p.getPersistentData().putLong(POOL_T, Kit.now(p));
    }

    /** Покров тьмы: щит на союзника, и враги на 3 секунды теряют его из виду. */
    private static boolean veil(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.ally(p, a.radius);
        ServerLevel sl = p.serverLevel();
        t.setAbsorptionAmount(Math.max(t.getAbsorptionAmount(), Kit.power(s, a) * AbilityHandler.healMult(s)));
        Fx.shield(sl, t, 1.2, Kit.dur(s, a), 0x60402060);
        Scheduler.repeat(0, 10, 6, i -> {
            for (Mob m : sl.getEntitiesOfClass(Mob.class, t.getBoundingBox().inflate(24), m -> m.getTarget() == t && !Kit.boss(m))) {
                m.setTarget(null);
            }
            return t.isAlive();
        });
        Kit.sound(sl, t.position(), SoundEvents.ENDERMAN_AMBIENT, 1f, 0.6f);
        return true;
    }
}
