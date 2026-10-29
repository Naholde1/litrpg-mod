package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Кровавый рыцарь: платит жизнью и лечится тем, что отнял.
 * <p>
 * Всё завязано на кровь: удар лечит тем больше, чем ниже его жизнь; кровоток кормит, пока
 * цель рядом; цепи держат врага на привязи; клеймо заставляет всех бить помеченного сильнее,
 * а каждый удар по нему поит рыцаря. Пир возвращает долю всего, что он нанёс за десять секунд.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class BloodKnight {
    private static final int BLOOD = 0xE0C01020;
    private static final int DARK = 0xD0700818;
    /** Сколько урона рыцарь нанёс недавно (тает со временем) и когда это обновлялось. */
    private static final String FEAST = "litrpg_bk_feast";
    private static final String FEAST_T = "litrpg_bk_feast_t";
    /** Клеймо: UUID рыцаря на цели. */
    private static final String BRAND = "litrpg_bk_brand";

    private BloodKnight() {}

    static void register() {
        Special.put("bk_strike", BloodKnight::strike);
        Special.put("bk_leech", BloodKnight::leech);
        Special.put("bk_chains", BloodKnight::chains);
        Special.put("bk_mark", BloodKnight::brand);
        Special.put("bk_harvest", BloodKnight::harvest);
        Special.put("bk_crimson", BloodKnight::feast);
        Special.put("bk_pact", BloodKnight::pact);
    }

    private static float lowHp(LivingEntity e) {
        return 1f - e.getHealth() / Math.max(1f, e.getMaxHealth());
    }

    private static void blood(ServerLevel sl, Vec3 at, int n) {
        sl.sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0.02f, 0.05f), 1.4f), at.x, at.y, at.z, n, 0.3, 0.3, 0.3, 0);
    }

    /** Кровавый удар: лечит на 30% нанесённого, а на последней половине жизни — до 70%. */
    private static boolean strike(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        float before = t.getHealth();
        Kit.dmg(p, a, t, Kit.power(s, a));
        float done = Math.max(0, before - t.getHealth());
        p.heal(done * (0.3f + 0.4f * Math.max(0, lowHp(p) * 2 - 1)));
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), AbilityHandler.flatLook(p), 2, 120, 30, 8, BLOOD);
        Fx.beam(sl, t.position().add(0, 1, 0), p.position().add(0, 1, 0), 0.1, 8, DARK);
        blood(sl, t.position().add(0, 1, 0), 12);
        Kit.sound(sl, t.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.6f);
        return true;
    }

    /** Кровоток: каждую секунду рана тянет жизнь из цели в рыцаря — пока цель в 18 блоках. */
    private static boolean leech(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int secs = Math.max(3, Kit.dur(s, a) / 20);
        float tick = Kit.power(s, a) * 2f / secs;
        Fx.mark(sl, t, 0.5, 1, secs * 20, BLOOD);
        Scheduler.repeat(0, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive() || t.distanceTo(p) > a.radius) return false;
            float before = t.getHealth();
            Kit.dmg(p, a, t, tick);
            p.heal(Math.max(0, before - t.getHealth()) * 0.6f);
            Fx.beam(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), p.position().add(0, 1, 0), 0.12, 14, BLOOD);
            blood(sl, t.position().add(0, 1, 0), 5);
            return true;
        });
        return true;
    }

    /** Багровые цепи: подтягивают врага и держат на привязи — отойти дальше шести блоков он не может. */
    private static boolean chains(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        int life = 80;
        Kit.pull(t, p.position(), Math.min(2.2, 0.4 + t.distanceTo(p) * 0.15));
        Kit.dmg(p, a, t, Kit.power(s, a));
        Fx.chains(sl, p, t, 0.42, life, BLOOD);
        Kit.taunt(p, t);
        Kit.sound(sl, p.position(), SoundEvents.CHAIN_BREAK, 1.2f, 0.5f);
        Scheduler.repeat(10, 5, life / 5, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            if (t.distanceTo(p) > 6) Kit.pull(t, p.position(), 0.6);
            return true;
        });
        return true;
    }

    /** Клеймо: помеченный получает на четверть больше от всех, а каждый удар по нему поит рыцаря. */
    private static boolean brand(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        Kit.until(t, BRAND, life);
        t.getPersistentData().putUUID(BRAND + "_by", p.getUUID());
        Fx.mark(p.level(), t, 0.7, 1, life, BLOOD);
        Fx.runes(p.level(), t.position(), 1.4, 3, life, DARK);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.FIRECHARGE_USE, 1f, 0.5f);
        return true;
    }

    /** Жатва: круг пьёт всех внутри каждую секунду и отдаёт всё рыцарю. */
    private static boolean harvest(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        float tick = Kit.power(s, a) * 0.5f;
        Fx.runes(sl, c, a.radius, 6, life, BLOOD);
        Fx.disc(sl, c, a.radius, 0, life, 0x40800010);
        Kit.sound(sl, c, SoundEvents.WITHER_AMBIENT, 0.6f, 1.4f);
        Scheduler.repeat(20, 20, life / 20, i -> {
            if (!p.isAlive()) return false;
            float total = 0;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) {
                float before = e.getHealth();
                Kit.dmg(p, a, e, tick);
                total += Math.max(0, before - e.getHealth());
                Fx.beam(sl, e.position().add(0, 1, 0), c.add(0, 0.2, 0), 0.08, 12, BLOOD);
            }
            if (total > 0 && p.position().distanceTo(c) <= a.radius + 2) p.heal(total * 0.5f);
            return true;
        });
        return true;
    }

    /** Багряный пир: возвращает долю всего урона, нанесённого за последние ~10 секунд. */
    private static boolean feast(ServerPlayer p, PlayerStats s, Ability a) {
        float pool = feastPool(p);
        if (pool < 1) {
            Kit.msg(p, "Пировать нечем — сначала пролейте кровь.", ChatFormatting.GRAY);
            return false;
        }
        float heal = pool * Math.max(0.2f, a.power) * s.rankPower(a);
        p.heal(heal);
        p.getPersistentData().putFloat(FEAST, 0);
        ServerLevel sl = p.serverLevel();
        Fx.spiral(sl, p, 0.9, 2.4, 3, 24, BLOOD);
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.4, 2.2, 14, DARK);
        blood(sl, p.position().add(0, 1, 0), 30);
        Kit.sound(sl, p.position(), SoundEvents.GENERIC_DRINK, 1.2f, 0.6f);
        Kit.msg(p, String.format("Багряный пир: +%.0f здоровья", heal), ChatFormatting.DARK_RED);
        return true;
    }

    private static float feastPool(LivingEntity p) {
        var d = p.getPersistentData();
        long dt = Kit.now(p) - d.getLong(FEAST_T);
        return (float) (d.getFloat(FEAST) * Math.exp(-dt / 200.0));
    }

    /** Договор: рыцарь отдаёт 15% жизни — бафы из данных и вампиризм 20% на всё время. */
    private static boolean pact(ServerPlayer p, PlayerStats s, Ability a) {
        p.setHealth(Math.max(1f, p.getHealth() - p.getMaxHealth() * 0.15f));
        AbilityHandler.applySelf(a, p);
        int life = Kit.dur(s, a);
        Kit.until(p, Kit.LEECH, 0.2f, life);
        Fx.spiral(p.level(), p, 0.8, 2.2, 3, life, BLOOD);
        blood(p.serverLevel(), p.position().add(0, 1, 0), 25);
        Kit.sound(p.serverLevel(), p.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.5f);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        // пир копит всё, что нанёс кровавый рыцарь
        if (e.getSource().getEntity() instanceof ServerPlayer p && p != t
                && StatsManager.get(p).getPlayerClass() != null && "bloodknight".equals(StatsManager.get(p).getPlayerClass().key)) {
            var d = p.getPersistentData();
            d.putFloat(FEAST, Math.min(p.getMaxHealth() * 4, feastPool(p) + e.getAmount()));
            d.putLong(FEAST_T, Kit.now(p));
        }
        // клеймо: удары по помеченному поят рыцаря
        if (Kit.active(t, BRAND) && t.getPersistentData().hasUUID(BRAND + "_by")
                && t.level() instanceof ServerLevel sl && sl.getEntity(t.getPersistentData().getUUID(BRAND + "_by")) instanceof ServerPlayer k
                && k.isAlive() && k.distanceTo(t) < 30) {
            k.heal(e.getAmount() * 0.08f);
        }
    }
}
