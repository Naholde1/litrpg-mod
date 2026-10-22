package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Баал'Зар, Архидемон (ур. 55) — последний хранитель. Три фазы.
 * I: Щит Бездны питают якоря душ (узлы силы) — пока они целы, щит возвращается каждые 20 с.
 * II (66%): метеоры Преисподней надо делить, похищение жизни бьёт всех.
 * III (25%): Сжатие Бездны — края арены поглощает тьма, и безопасный круг всё меньше.
 */
public class ArchdemonEntity extends BossBase {
    private double squeeze = 24;

    public ArchdemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Теневые сферы", 80, 12, 1).silent()
                .act(t -> {
                    for (int i = 0; i < 3; i++) shoot(BossProjectile.Kind.SHADOW, randomOther(), 0.3f, 1.2f, 4f);
                    sound(SoundEvents.WITHER_SHOOT, 1f, 0.7f);
                });
        skill("Адское пламя", 140, 20, 1).first(50)
                .when(t -> distanceTo(t) < 7)
                .begin(t -> cone(position(), dirTo(t), 8, 40, 20, 1.05f).color(Telegraph.ORANGE)
                        .then(e -> {
                            e.setSecondsOnFire(6);
                            BossDamage.sunder(e, 2);
                        }).fx(c -> burst(ParticleTypes.SOUL_FIRE_FLAME, c.add(getLookAngle().scale(4)).add(0, 1, 0), 80, 2.5, 0.1)));
        skill("Знак Бездны", 320, 10, 1).first(160)
                .act(t -> {
                    LivingEntity o = randomOther();
                    circle(o.position(), 5, 120, 0.95f).color(Telegraph.PURPLE).follow(o, 25)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0)))
                            .fx(c -> burst(ParticleTypes.REVERSE_PORTAL, c.add(0, 1, 0), 80, 2, 0.1));
                    tip(o, "Знак Бездны на вас! Унесите его от группы!", ChatFormatting.DARK_PURPLE);
                });
        skill("Шаг сквозь тень", 260, 0, 0).silent().first(200)
                .act(t -> {
                    LivingEntity o = randomOther();
                    Vec3 behind = o.position().subtract(o.getLookAngle().multiply(1, 0, 1).normalize().scale(2));
                    teleportTo(behind.x, o.getY(), behind.z);
                    burst(ParticleTypes.LARGE_SMOKE, position().add(0, 1, 0), 30, 0.8, 0.05);
                    cone(position(), dirTo(o), 6, 50, 16, 0.75f).color(Telegraph.PURPLE);
                    sound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
                });
        skill("Метеоры Преисподней", 380, 20, 2).fromPhase(1).first(120).unstoppable()
                .act(t -> {
                    for (int i = 0; i < 2; i++) {
                        circle(randomSpot(14), 3.2, 100, 1.8f).color(Telegraph.GOLD).soak(2).then(e -> e.setSecondsOnFire(6))
                                .fx(c -> {
                                    burst(ParticleTypes.EXPLOSION_EMITTER, c.add(0, 0.5, 0), 1, 0, 0);
                                    soundAt(c, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.5f);
                                });
                    }
                    shoutAll("Два метеора! Разделитесь на две группы — по двое в каждый золотой круг!");
                });
        skill("Похищение жизни", 300, 26, 2).fromPhase(1).first(200)
                .act(t -> {
                    int n = hitAll(arena(), 30, 0.3f, e -> burst(ParticleTypes.DAMAGE_INDICATOR, e.position().add(0, 1, 0), 6, 0.3, 0.1));
                    heal(getMaxHealth() * 0.02f * n);
                    sound(SoundEvents.WARDEN_HEARTBEAT, 2f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override public int getBossLevel() { return 55; }
    @Override public int getXpReward() { return 14000; }

    @Override
    protected float toughness() {
        return 4.0f;
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 7;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.25f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Якоря душ (фиолетовые узлы) каждые 20 с возвращают Щит Бездны. Разбейте все четыре.",
                "«Знак Бездны» — метку унесите от группы. «Адское пламя» раскалывает броню танка — держите его отвернутым.",
                "С 66%: метеоры делите по двое, «Похищение жизни» бьёт всех — лекарям быть начеку.",
                "С 25%: тьма сжимает арену. Держитесь ближе к центру.",
                "Святое бьёт его сильнее, огонь — почти не берёт.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case HOLY -> 1.6f;
            case FIRE -> 0.3f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        if (tickCount % 6 == 0) burst(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, position().add(0, 2.5, 0), 2, 0.8, 0.02);
        if (getTarget() == null) return;
        if (fightTicks % 400 == 0) {
            int a = relicsAlive();
            if (a > 0) {
                setBarrier(getMaxHealth() * 0.06f * a);
                shoutAll("Щит Бездны восстал (якорей: " + a + ").");
                sound(SoundEvents.BEACON_ACTIVATE, 2f, 0.5f);
            }
        }
        if (phase >= 2 && fightTicks % 200 == 0) {
            squeeze = Math.max(8, squeeze - 3);
            donut(arena(), squeeze, 34, 60, 0.6f).color(Telegraph.PURPLE)
                    .fx(c -> burst(ParticleTypes.REVERSE_PORTAL, c.add(0, 1, 0), 120, squeeze + 4, 0.05));
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        shout(alive > 0 ? "Мои якоря... вы пожалеете!" : "Без якорей... я всё равно сильнее вас!");
        shoutAll(alive > 0 ? "Якорь душ разбит. Осталось: " + alive : "Все якоря разбиты — Щит Бездны больше не вернётся!");
        stagger(60, null);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Вы думали, это конец? Это лишь начало!");
            summon(EntityType.WITHER_SKELETON, 2, 7, 50, null);
            summon(EntityType.VEX, 3, 7, 48, null);
        } else {
            shout("Пепел ко пеплу. Тьма... ко мне!");
            shoutAll("Тьма поглощает края арены — к центру!");
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            squeeze = 24;
        }
        burst(ParticleTypes.SOUL_FIRE_FLAME, position().add(0, 2, 0), 150, 2.5, 0.3);
        sound(SoundEvents.WITHER_SPAWN, 1.5f, 0.6f);
    }

    @Override
    protected void onReset() {
        squeeze = 24;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.setSecondsOnFire(4);
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.WITHER && effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("Squeeze", squeeze);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Squeeze")) squeeze = tag.getDouble("Squeeze");
    }

    @Override public boolean isOnFire() { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
