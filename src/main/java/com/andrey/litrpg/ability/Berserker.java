package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Берсерк: чем больнее ему, тем злее он бьёт.
 * <p>
 * Общие виды делали из него обычного рубаку: размах — круг вокруг себя, рана — простое
 * проклятие, прыжок — рывок с ударом. Здесь каждый приём завязан на две вещи, о которых
 * говорит класс: потерянное здоровье (размах и «Добить» бьют тем сильнее, чем ниже его жизнь)
 * и близость к цели (рана течёт, только пока он рядом; «Мясорубка» затягивает врагов к нему).
 * «Кровавая баня» сжигает всю накопленную ярость — сколько накопил, столько и ударов.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Berserker {
    /** До какого тика идёт исступление: удары лечат берсерка. */
    private static final String FRENZY = "litrpg_br_frenzy";
    private static final int BLOOD = 0xE0D02020;
    private static final int RAGE = 0xE0FF5020;
    /** Насколько далеко можно отойти от раненого, чтобы рана не затянулась. */
    private static final double REND_RANGE = 6;

    private Berserker() {}

    static void register() {
        Special.put("br_cleave", Berserker::cleave);
        Special.put("br_rend", Berserker::rend);
        Special.put("br_whirl", Berserker::whirl);
        Special.put("br_leap", Berserker::leap);
        Special.put("br_bloodbath", Berserker::bloodbath);
        Special.put("br_execute", Berserker::execute);
        Special.put("br_frenzy", Berserker::frenzy);
    }

    /** Множитель от потерянного здоровья: на полной жизни 1, у края — до 1 + bonus. */
    private static float pain(ServerPlayer p, float bonus) {
        return 1f + bonus * (1f - p.getHealth() / Math.max(1f, p.getMaxHealth()));
    }

    private static void addRage(ServerPlayer p, PlayerStats s, double v) {
        s.setResource(s.getResource() + v);
        StatsManager.syncMana(p);
    }

    /** Враги в полукруге перед берсерком. */
    private static List<LivingEntity> inFront(ServerPlayer p, double r, double cosHalf) {
        Vec3 dir = AbilityHandler.flatLook(p);
        return AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(r, 1.5, r)).stream().filter(e -> {
            Vec3 d = e.position().subtract(p.position());
            Vec3 flat = new Vec3(d.x, 0, d.z);
            return flat.length() <= r + e.getBbWidth() / 2 && (flat.lengthSqr() < 1 || flat.normalize().dot(dir) >= cosHalf);
        }).toList();
    }

    // ---------------- размах и добивание ----------------

    /** Размах: широкая дуга перед собой. Чем меньше у берсерка жизни, тем тяжелее удар; каждый задетый — ярость. */
    private static boolean cleave(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power * pain(p, 0.6f);
        int hit = 0;
        for (LivingEntity e : inFront(p, a.radius, Math.cos(Math.toRadians(75)))) {
            if (AbilityHandler.hurt(p, a, e, dmg)) hit++;
        }
        Fx.slash(sl, p.position().add(0, 1, 0), AbilityHandler.flatLook(p), a.radius, 150, 0, 8, p.getHealth() < p.getMaxHealth() / 2 ? BLOOD : RAGE);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        if (hit > 0) addRage(p, s, 4 * hit);
        return true;
    }

    /**
     * Добить: удар тем сильнее, чем меньше жизни у цели (до втрое), у раненого ниже пятой части —
     * ещё в полтора раза. Если цель упала, ярость возвращается, а приём почти сразу готов снова.
     */
    private static boolean execute(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.lookEnemy(p, a.radius + 1);
        if (t == null) {
            List<LivingEntity> near = inFront(p, a.radius, 0.5);
            t = near.stream().min((x, y) -> Float.compare(x.getHealth() / x.getMaxHealth(), y.getHealth() / y.getMaxHealth())).orElse(null);
        }
        if (t == null) return AbilityHandler.noTarget(p);
        float frac = t.getHealth() / Math.max(1f, t.getMaxHealth());
        float dmg = AbilityHandler.base(s, a) * a.power * (1f + 2f * (1f - frac)) * (frac < 0.2f ? 1.5f : 1f);
        ServerLevel sl = p.serverLevel();
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), AbilityHandler.flatLook(p), 2.2, 80, 70, 8, BLOOD);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 0.6f);
        AbilityHandler.hurt(p, a, t, dmg);
        if (!t.isAlive()) {
            addRage(p, s, 30);
            long now = sl.getGameTime();
            // откат выставляется после самого приёма — сбрасываем его на следующем тике
            Scheduler.later(1, () -> {
                s.setCooldownEnd(a, now + 20);
                StatsManager.sync(p);
            });
            p.displayClientMessage(Component.literal("Добит! Ярость кипит — «Добить» снова готово.").withStyle(ChatFormatting.RED), true);
        }
        return true;
    }

    // ---------------- рана ----------------

    /** Рваная рана течёт, пока берсерк рядом с раненым. Отошёл дальше шести блоков — рана затянулась. */
    private static boolean rend(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.lookEnemy(p, a.radius + 2);
        if (t == null) {
            List<LivingEntity> near = inFront(p, a.radius, 0.3);
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) return AbilityHandler.noTarget(p);
        final LivingEntity target = t;
        ServerLevel sl = p.serverLevel();
        float tick = AbilityHandler.base(s, a) * a.power * 0.35f;
        int secs = Math.max(4, a.duration / 20);
        AbilityHandler.hurt(p, a, target, tick);
        Fx.slash(sl, target.position().add(0, target.getBbHeight() * 0.5, 0), AbilityHandler.flatLook(p), 1.6, 120, 40, 8, BLOOD);
        Fx.tether(sl, p, target, 0.08, 20, 0x90C01010);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!target.isAlive() || !p.isAlive()) return false;
            if (p.distanceTo(target) > REND_RANGE) {
                p.displayClientMessage(Component.literal("Рана затянулась — вы отошли слишком далеко.").withStyle(ChatFormatting.GRAY), true);
                return false;
            }
            Fx.tether(sl, p, target, 0.08, 20, 0x90C01010);
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(), 3, 0.2, 0.2, 0.2, 0.05);
            target.invulnerableTime = 0;
            AbilityHandler.hurt(p, a, target, tick * pain(p, 0.5f));
            return true;
        });
        return true;
    }

    // ---------------- мясорубка и прыжок ----------------

    /** Мясорубка: несколько оборотов подряд; каждый бьёт вокруг и подтягивает врагов к берсерку. */
    private static boolean whirl(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int spins = Math.max(2, a.count);
        float dmg = AbilityHandler.base(s, a) * a.power;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, spins * 8 + 10, 1, false, false, true));
        Scheduler.repeat(0, 8, spins, i -> {
            if (!p.isAlive()) return false;
            Vec3 c = p.position();
            Fx.slash(sl, c.add(0, 1, 0), AbilityHandler.flatLook(p).yRot(i * 1.9f), a.radius, 360, 0, 6, i % 2 == 0 ? RAGE : BLOOD);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.6f + i * 0.15f);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(a.radius, 1.5, a.radius))) {
                if (e.distanceTo(p) > a.radius + 0.5) continue;
                e.invulnerableTime = 0;
                AbilityHandler.hurt(p, a, e, dmg);
                if (!(e instanceof com.andrey.litrpg.entity.BossBase)) {
                    Vec3 pull = c.subtract(e.position()).multiply(1, 0, 1);
                    if (pull.lengthSqr() > 2.5) e.setDeltaMovement(e.getDeltaMovement().add(pull.normalize().scale(0.35)));
                    e.hurtMarked = true;
                }
            }
            return true;
        });
        return true;
    }

    /** Прыжок зверя: в точку под прицелом по дуге; приземление вминает землю, а не наносит удар в воздухе. */
    private static boolean leap(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = AbilityHandler.lookPoint(p, 16);
        Vec3 d = to.subtract(p.position());
        Vec3 flat = new Vec3(d.x, 0, d.z);
        double dist = Math.min(14, flat.length());
        if (dist < 1.5) flat = AbilityHandler.flatLook(p).scale(dist = 4);
        Vec3 v = flat.normalize().scale(0.22 + dist * 0.085).add(0, 0.75 + Math.max(0, d.y) * 0.08, 0);
        p.setDeltaMovement(v);
        p.hurtMarked = true;
        p.fallDistance = 0;
        ServerLevel sl = p.serverLevel();
        Fx.disc(sl, p.position().add(flat.normalize().scale(dist)), a.radius, 0, 18, 0x60FF4020);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.7f, 1.3f);
        float dmg = AbilityHandler.base(s, a) * a.power * pain(p, 0.4f);
        Set<Integer> landed = new HashSet<>();
        Scheduler.repeat(4, 1, 50, i -> {
            if (!p.isAlive()) return false;
            p.fallDistance = 0;
            if (!p.onGround() && i < 49) return true;
            if (!landed.add(1)) return false;
            Vec3 c = p.position();
            Fx.ring(sl, c.add(0, 0.1, 0), 0.3, a.radius, 12, RAGE, 0.6f);
            Fx.sphere(sl, c, 0.3, a.radius * 0.6, 8, BLOOD);
            sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 2, 0.6, 0, 0.6, 0);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1f, 0.7f);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(a.radius, 2, a.radius))) {
                AbilityHandler.hurt(p, a, e, dmg);
            }
            return false;
        });
        return true;
    }

    // ---------------- кровавая баня и исступление ----------------

    /** Кровавая баня: сжигает всю оставшуюся ярость — каждые 20 ярости дают ещё удар; каждый удар лечит. */
    private static boolean bloodbath(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.lookEnemy(p, a.radius + 2);
        if (t == null) {
            List<LivingEntity> near = inFront(p, a.radius, 0.3);
            t = near.isEmpty() ? null : near.get(0);
        }
        if (t == null) return AbilityHandler.noTarget(p);
        final LivingEntity target = t;
        // цена навыка ещё не списана: всё, что сверх неё, — топливо для лишних ударов
        double extra = Math.max(0, s.getResource() - AbilityHandler.cost(s, a));
        int bonus = (int) Math.min(5, extra / 20);
        s.setResource(s.getResource() - bonus * 20);
        StatsManager.syncMana(p);
        int strikes = Math.max(3, a.count) + bonus;
        float dmg = AbilityHandler.base(s, a) * a.power * 0.6f;
        ServerLevel sl = p.serverLevel();
        p.displayClientMessage(Component.literal("Кровавая баня: ударов " + strikes + (bonus > 0 ? " (+" + bonus + " от ярости)" : ""))
                .withStyle(ChatFormatting.DARK_RED), true);
        Scheduler.repeat(0, 5, strikes, i -> {
            if (!p.isAlive() || !target.isAlive() || p.distanceTo(target) > a.radius + 3) return false;
            Vec3 dir = target.position().subtract(p.position()).multiply(1, 0, 1);
            Fx.slash(sl, target.position().add(0, target.getBbHeight() * 0.6, 0), dir.lengthSqr() < 1e-3 ? AbilityHandler.flatLook(p) : dir.normalize(),
                    1.8, 110, i % 2 == 0 ? 45 : -45, 6, BLOOD);
            sl.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 0.7f + i * 0.08f);
            target.invulnerableTime = 0;
            float before = target.getHealth();
            AbilityHandler.hurt(p, a, target, dmg * (i == strikes - 1 ? 2f : 1f));
            p.heal(Math.max(0, before - target.getHealth()) * 0.2f);
            return true;
        });
        return true;
    }

    /** Исступление: баф из данных, и пока он держится, каждый удар берсерка возвращает ему часть урона здоровьем. */
    private static boolean frenzy(ServerPlayer p, PlayerStats s, Ability a) {
        AbilityHandler.applySelf(a, p);
        p.getPersistentData().putLong(FRENZY, p.level().getGameTime() + Math.max(100, a.duration));
        Fx.spiral(p.level(), p, 0.8, 2.2, 3, Math.max(100, a.duration), BLOOD);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.literal("Исступление: удары пьют кровь, но и бьют по вам больнее.").withStyle(ChatFormatting.RED), true);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || e.getEntity() == p) return;
        if (p.level().getGameTime() >= p.getPersistentData().getLong(FRENZY)) return;
        p.heal(e.getAmount() * 0.12f);
    }
}
