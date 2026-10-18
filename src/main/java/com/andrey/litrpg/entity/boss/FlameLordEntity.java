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
 * Игнис, Владыка пламени (ур. 45). Огненные залпы, метеоритный дождь
 * с предупреждающими кругами, огненное кольцо и призыв ифритов (ифриты —
 * ванильные всполохи). Во второй фазе раскаляется: жжёт всех рядом.
 */
public class FlameLordEntity extends BossBase {
    private final List<Vec3> marks = new ArrayList<>();

    public FlameLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Огненный залп", 80, 12, 1).silent()
                .act(t -> {
                    int n = phase > 0 ? 5 : 3;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.FIRE, t, 14f, 1.3f, 3f + i * 4f);
                    sound(SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
                });
        skill("Метеоритный дождь", 260, 36, 2)
                .during(left -> {
                    if (left == 35) {
                        marks.clear();
                        for (LivingEntity e : enemiesAround(26)) {
                            for (int i = 0; i < 3; i++) {
                                marks.add(e.position().add((random.nextDouble() - 0.5) * 5, 0, (random.nextDouble() - 0.5) * 5));
                            }
                        }
                    }
                    if (left % 4 == 0) for (Vec3 m : marks) ring(ParticleTypes.FLAME, m, 2.5, 14);
                })
                .act(t -> {
                    for (Vec3 m : marks) {
                        BossProjectile p = new BossProjectile(level(), this, BossProjectile.Kind.METEOR, 22f);
                        p.setPos(m.x + (random.nextDouble() - 0.5), m.y + 16, m.z + (random.nextDouble() - 0.5));
                        p.shoot(0, -1, 0, 0.9f, 0);
                        level().addFreshEntity(p);
                    }
                    marks.clear();
                    sound(SoundEvents.GHAST_SHOOT, 2f, 0.4f);
                });
        skill("Огненное кольцо", 200, 26, 2)
                .when(t -> distanceTo(t) < 9)
                .during(left -> { if (left % 3 == 0) ring(ParticleTypes.FLAME, position(), 8, 40); })
                .act(t -> {
                    aoe(position(), 8.5, 20f, e -> {
                        e.setSecondsOnFire(6);
                        knockFrom(e, position(), 1.2, 0.5);
                    });
                    for (int r = 2; r <= 8; r += 2) ring(ParticleTypes.FLAME, position(), r, r * 8);
                    burst(ParticleTypes.LAVA, position().add(0, 1, 0), 30, 3, 0.2);
                    sound(SoundEvents.FIRECHARGE_USE, 2f, 0.5f);
                });
        skill("Призыв ифритов", 380, 30, 2)
                .act(t -> {
                    summon(EntityType.BLAZE, 2, 4, 43, null);
                    sound(SoundEvents.BLAZE_AMBIENT, 2f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 18.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 45; }
    @Override public int getXpReward() { return 6500; }

    @Override
    protected float toughness() {
        return 2.3f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 4 == 0) burst(ParticleTypes.FLAME, position().add(0, 1.2, 0), 3, 0.6, 0.02);
        if (phase > 0 && tickCount % 20 == 0) {
            for (LivingEntity e : enemiesAround(4.5)) {
                e.hurt(damageSources().inFire(), 4f);
                e.setSecondsOnFire(3);
            }
            ring(ParticleTypes.SMALL_FLAME, position(), 4, 20);
        }
    }

    @Override
    protected void onPhase(int phase) {
        shout("Гори! ГОРИ ВМЕСТЕ СО МНОЙ!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        burst(ParticleTypes.FLAME, position().add(0, 1.5, 0), 120, 2, 0.3);
        sound(SoundEvents.BLAZE_DEATH, 2f, 0.4f);
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.setSecondsOnFire(5);
        return hit;
    }

    @Override public boolean isOnFire() { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_BURN; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
