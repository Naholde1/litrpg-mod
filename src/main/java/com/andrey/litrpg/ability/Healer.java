package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Лекарь: эталонный целитель, но не «зелёная вспышка на всех».
 * <p>
 * Повязка лечит тем сильнее, чем хуже раненому; молитва отдаёт больше тем, кто ближе к краю;
 * операция спасает того, кто уже почти пал. Святилище — место на полу: внутри и лечат лучше,
 * и враги бьют слабее, а выйти из него — значит потерять защиту. «Кара» бьёт врага и тут же
 * переливает часть урона самому раненому рядом. «Чудо» поднимает всех павших вокруг разом.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Healer {
    /** До какого тика союзник стоит в святилище (метка обновляется каждую секунду). */
    private static final String SANCT = "litrpg_hl_sanct";
    private static final int HOLY = 0xE0FFF0A0;
    private static final int LIGHT = 0xC0FFFFFF;

    private Healer() {}

    static void register() {
        Special.put("hl_bandage", Healer::bandage);
        Special.put("hl_surgery", Healer::surgery);
        Special.put("hl_prayer", Healer::prayer);
        Special.put("hl_sanctuary", Healer::sanctuary);
        Special.put("hl_miracle", Healer::miracle);
        Special.put("hl_smite", Healer::smite);
    }

    private static float missing(LivingEntity e) {
        return 1f - e.getHealth() / Math.max(1f, e.getMaxHealth());
    }

    /** Тугая повязка: каждую секунду лечит, и тем сильнее, чем меньше у раненого жизни (до двух раз). */
    private static boolean bandage(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        int secs = Math.max(1, a.duration / 20);
        float per = Dynamic.healAmount(s, a, 1f) / secs;
        int c = Dynamic.healColor(a);
        Fx.mark(p.level(), t, 0.35, 3, a.duration, c);
        Fx.spiral(p.level(), t, 0.6, 1.6, 3, 16, LIGHT);
        if (t != p) Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.15, 8, LIGHT);
        Scheduler.repeat(20, 20, secs, i -> {
            if (!t.isAlive() || !p.isAlive()) return false;
            Dynamic.doHeal(p, t, per * (1f + missing(t)));
            if (p.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 1, t.getZ(), 3, 0.3, 0.4, 0.3, 0);
            }
            return true;
        });
        AbilityHandler.sound(p, SoundEvents.WOOL_PLACE, 1f, 1.2f);
        return true;
    }

    /** Операция: долгое лечение; тому, у кого меньше трети жизни, — в полтора раза больше и короткая защита. */
    private static boolean surgery(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        boolean critical = missing(t) > 0.66f;
        Dynamic.doHeal(p, t, Dynamic.healAmount(s, a, critical ? 1.5f : 1f));
        int c = Dynamic.healColor(a);
        Fx.pillar(p.level(), t.position(), 0.9, 3.5, 16, c);
        Fx.ring(p.level(), t.position(), 0.3, 2.5, 14, c);
        if (t != p) Fx.beam(p.level(), p.getEyePosition().subtract(0, 0.3, 0), t.position().add(0, t.getBbHeight() * 0.6, 0), 0.4, 10, c);
        if (critical) {
            t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false, true));
            Fx.shield(p.level(), t, 1.2, 60, HOLY);
            p.displayClientMessage(Component.literal("Операция спасла жизнь: " + t.getName().getString()).withStyle(ChatFormatting.GOLD), true);
        }
        AbilityHandler.sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 1.5f);
        return true;
    }

    /** Общая молитва: лечит всех вокруг, раненым больше — от половины силы на полных до полуторной у края. */
    private static boolean prayer(ServerPlayer p, PlayerStats s, Ability a) {
        float base = Dynamic.healAmount(s, a, 1f);
        ServerLevel sl = p.serverLevel();
        int n = 0;
        for (LivingEntity e : AbilityHandler.alliesAround(p, a.radius)) {
            if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) continue;
            if (e instanceof ServerPlayer sp && com.andrey.litrpg.run.Spirits.isSpirit(sp)) continue;
            Dynamic.doHeal(p, e, base * (0.5f + missing(e)));
            Fx.pillar(sl, e.position(), 0.6, 2.6, 12, HOLY);
            n++;
        }
        Fx.ring(sl, p.position().add(0, 0.1, 0), 0.4, a.radius, 16, HOLY, 0.4f);
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, 2.4, 12, LIGHT);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
        return n > 0;
    }

    /**
     * Святилище: круг на полу. Каждую секунду лечит своих внутри, а враги внутри слабеют.
     * Союзник в святилище получает на 25% меньше урона — но только пока стоит в круге.
     */
    private static boolean sanctuary(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        double r = a.radius;
        int secs = Math.max(4, a.duration / 20);
        // сила святилища в данных отрицательная (ослабление врагов) — лечение считаем от базы
        float per = AbilityHandler.base(s, a) * AbilityHandler.healMult(s) * 0.35f;
        Fx.disc(sl, c, r, r - 0.4, secs * 20, HOLY);
        Fx.pillar(sl, c, r, 3, secs * 20, 0x30FFF0A0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.1f);
        Scheduler.repeat(0, 20, secs, i -> {
            if (!p.isAlive()) return false;
            AABB box = new AABB(c, c).inflate(r, 2.5, r);
            long until = sl.getGameTime() + 25;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.position().distanceTo(c) <= r)) {
                if (AbilityHandler.isAlly(p, e)) {
                    if (e instanceof ServerPlayer sp && com.andrey.litrpg.run.Spirits.isSpirit(sp)) continue;
                    e.getPersistentData().putLong(SANCT, until);
                    Dynamic.doHeal(p, e, per);
                } else if (AbilityHandler.isEnemy(p, e)) {
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 30, 1));
                    if (e.getMobType() == MobType.UNDEAD) e.setSecondsOnFire(1);
                }
            }
            sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z, 8, r / 2, 0.2, r / 2, 0.01);
            return true;
        });
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || t.level().getGameTime() >= t.getPersistentData().getLong(SANCT)) return;
        e.setAmount(e.getAmount() * 0.75f);
    }

    /** Чудо: лечит всех вокруг почти досыта и поднимает всех павших, чьи тела в круге. */
    private static boolean miracle(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float heal = Dynamic.healAmount(s, a, 1f);
        int raised = com.andrey.litrpg.run.Spirits.reviveAll(p, p.position(), a.radius, 0.6f, "Чудо: вас поднял(а) " + p.getName().getString());
        for (LivingEntity e : AbilityHandler.alliesAround(p, a.radius)) {
            if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) continue;
            Dynamic.doHeal(p, e, heal);
            e.removeEffect(MobEffects.POISON);
            e.removeEffect(MobEffects.WITHER);
            Fx.pillar(sl, e.position(), 0.8, 6, 24, HOLY);
        }
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 20, LIGHT);
        Fx.ring(sl, p.position().add(0, 0.1, 0), 0.5, a.radius, 24, HOLY, 0.6f);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.3f);
        p.displayClientMessage(Component.literal(raised > 0 ? "Чудо! Поднято павших: " + raised : "Чудо: отряд исцелён.")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Кара: луч света во врага; половина нанесённого урона тут же уходит лечением самому раненому рядом. */
    private static boolean smite(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
        Fx.pillar(sl, t.position(), 0.5, 5, 10, HOLY);
        Fx.beam(sl, p.getEyePosition().subtract(0, 0.3, 0), at, 0.2, 8, HOLY);
        float before = t.getHealth();
        AbilityHandler.hurt(p, a, t, AbilityHandler.base(s, a) * a.power);
        float dealt = Math.max(0, before - t.getHealth());
        LivingEntity hurt = null;
        float worst = 0.999f;
        List<LivingEntity> allies = AbilityHandler.alliesAround(p, 16);
        for (LivingEntity e : allies) {
            if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) continue;
            float f = e.getHealth() / Math.max(1f, e.getMaxHealth());
            if (f < worst) {
                worst = f;
                hurt = e;
            }
        }
        if (hurt != null && dealt > 0) {
            Dynamic.doHeal(p, hurt, dealt * 0.5f * AbilityHandler.healMult(s));
            Fx.beam(sl, at, hurt.position().add(0, hurt.getBbHeight() * 0.6, 0), 0.12, 10, LIGHT);
        }
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);
        return true;
    }
}
