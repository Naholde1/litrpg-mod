package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossProjectile;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Баал'Зар, Архидемон (ур. 55) — последний хранитель. Три фазы:
 * теневые сферы, адское пламя, шаг сквозь тень и легион пепла;
 * затем метеоры Преисподней и похищение жизни; в конце — Тьма Бездны.
 */
public class ArchdemonEntity extends BossBase {
    private final List<Vec3> marks = new ArrayList<>();

    public ArchdemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Теневые сферы", 70, 12, 1).silent()
                .act(t -> {
                    int n = phase >= 2 ? 5 : 3;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.SHADOW, t, 18f, 1.25f, 3f + i * 5f);
                    sound(SoundEvents.EVOKER_CAST_SPELL, 1.5f, 0.5f);
                });
        skill("Адское пламя", 200, 30, 2)
                .when(t -> distanceTo(t) < 10)
                .during(left -> { if (left % 3 == 0) ring(ParticleTypes.SOUL_FIRE_FLAME, position(), 9, 44); })
                .act(t -> {
                    aoe(position(), 9.5, 28f, e -> {
                        e.setSecondsOnFire(7);
                        knockFrom(e, position(), 1.4, 0.6);
                    });
                    for (int r = 2; r <= 9; r += 2) ring(ParticleTypes.SOUL_FIRE_FLAME, position(), r, r * 9);
                    sound(SoundEvents.WITHER_SHOOT, 2f, 0.4f);
                });
        skill("Шаг сквозь тень", 180, 0, 0).silent()
                .when(t -> distanceTo(t) > 6)
                .act(t -> {
                    burst(ParticleTypes.REVERSE_PORTAL, position().add(0, 2, 0), 50, 1, 0.1);
                    Vec3 behind = t.position().subtract(t.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
                    if (randomTeleport(behind.x, t.getY(), behind.z, false) || blinkNear(t.position(), 4)) {
                        sound(SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.4f);
                        burst(ParticleTypes.REVERSE_PORTAL, position().add(0, 2, 0), 50, 1, 0.1);
                    }
                });
        skill("Легион Пепла", 420, 35, 2)
                .during(left -> { if (left % 5 == 0) burst(ParticleTypes.SOUL, position().add(0, 1, 0), 10, 3, 0.05); })
                .act(t -> {
                    summon(EntityType.WITHER_SKELETON, 2, 7, 50, null);
                    summon(EntityType.VEX, 3, 7, 48, null);
                    sound(SoundEvents.EVOKER_PREPARE_SUMMON, 2f, 0.5f);
                });
        skill("Метеоры Преисподней", 260, 36, 2).fromPhase(1)
                .during(left -> {
                    if (left == 35) {
                        marks.clear();
                        for (LivingEntity e : enemiesAround(28)) {
                            for (int i = 0; i < 4; i++) {
                                marks.add(e.position().add((random.nextDouble() - 0.5) * 6, 0, (random.nextDouble() - 0.5) * 6));
                            }
                        }
                    }
                    if (left % 4 == 0) for (Vec3 m : marks) ring(ParticleTypes.SOUL_FIRE_FLAME, m, 2.5, 14);
                })
                .act(t -> {
                    for (Vec3 m : marks) {
                        BossProjectile p = new BossProjectile(level(), this, BossProjectile.Kind.METEOR, 26f);
                        p.setPos(m.x, m.y + 18, m.z);
                        p.shoot(0, -1, 0, 1.0f, 0);
                        level().addFreshEntity(p);
                    }
                    marks.clear();
                    sound(SoundEvents.GHAST_SHOOT, 2f, 0.3f);
                });
        skill("Похищение жизни", 240, 22, 2).fromPhase(1)
                .act(t -> {
                    float healed = 0;
                    for (LivingEntity e : enemiesAround(16)) {
                        if (e.hurt(damageSources().indirectMagic(this, this), 12f)) healed += 0.02f;
                        burst(ParticleTypes.DAMAGE_INDICATOR, e.position().add(0, 1, 0), 8, 0.3, 0.1);
                    }
                    heal(getMaxHealth() * healed);
                    sound(SoundEvents.WITHER_AMBIENT, 1.5f, 0.6f);
                });
        skill("Тьма Бездны", 360, 30, 2).fromPhase(2)
                .act(t -> {
                    for (LivingEntity e : enemiesAround(30)) {
                        e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 200, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                    }
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                    sound(SoundEvents.WARDEN_SONIC_CHARGE, 2f, 0.5f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 20.0)
                .add(Attributes.ARMOR_TOUGHNESS, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override public int getBossLevel() { return 55; }
    @Override public int getXpReward() { return 12000; }

    @Override
    protected float toughness() {
        return 3.5f;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 6 == 0) burst(phase >= 2 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.SMOKE, position().add(0, 2.5, 0), 2, 0.8, 0.02);
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Вы думали, это конец? Это лишь начало!");
            summon(EntityType.WITHER_SKELETON, 2, 7, 50, null);
        } else {
            shout("Пепел ко пеплу. Тьма... ко мне!");
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 600, 0));
        }
        burst(ParticleTypes.SOUL_FIRE_FLAME, position().add(0, 2, 0), 150, 2.5, 0.3);
        sound(SoundEvents.WITHER_SPAWN, 1.5f, 0.6f);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setSecondsOnFire(4);
            heal(4f);
        }
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.WITHER && effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override public boolean isOnFire() { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
