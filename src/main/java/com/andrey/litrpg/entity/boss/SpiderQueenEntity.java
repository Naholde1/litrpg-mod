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
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
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

/**
 * Королева Арахна (ур. 14). Опутывает паутиной, плюётся ядом, прыгает
 * на добычу и выводит паучат. Под конец боя проливает кислотный дождь.
 */
public class SpiderQueenEntity extends BossBase {
    private int leapTicks = 0;

    public SpiderQueenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Паутина", 90, 10, 1).silent()
                .when(t -> distanceTo(t) > 3)
                .act(t -> {
                    shoot(BossProjectile.Kind.WEB, t, 4f, 1.3f, 1f);
                    sound(SoundEvents.SPIDER_AMBIENT, 1.2f, 0.6f);
                });
        skill("Ядовитый плевок", 120, 14, 1)
                .act(t -> {
                    for (int i = 0; i < 3; i++) shoot(BossProjectile.Kind.POISON, t, 7f, 1.2f, 6f + i * 4f);
                    sound(SoundEvents.LLAMA_SPIT, 1.4f, 0.5f);
                });
        skill("Выводок", 320, 30, 2)
                .during(left -> { if (left % 6 == 0) burst(ParticleTypes.ITEM_SLIME, position().add(0, 0.5, 0), 10, 1.2, 0.05); })
                .act(t -> {
                    summon(EntityType.CAVE_SPIDER, phase > 0 ? 4 : 3, 7, 13, null);
                    sound(SoundEvents.TURTLE_EGG_HATCH, 1.5f, 0.7f);
                });
        skill("Прыжок охотницы", 150, 8, 1)
                .when(t -> distanceTo(t) > 5 && distanceTo(t) < 18 && onGround())
                .act(t -> {
                    Vec3 d = t.position().subtract(position());
                    Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(Math.min(d.horizontalDistance() * 0.13, 1.7));
                    setDeltaMovement(h.x, 0.85, h.z);
                    hasImpulse = true;
                    leapTicks = 1;
                    sound(SoundEvents.SPIDER_STEP, 2f, 0.5f);
                });
        skill("Кислотный дождь", 280, 30, 2).fromPhase(1)
                .during(left -> { if (left % 5 == 0) for (LivingEntity e : enemiesAround(20)) ring(ParticleTypes.ITEM_SLIME, e.position(), 2.5, 14); })
                .act(t -> {
                    for (LivingEntity e : enemiesAround(20)) {
                        AreaEffectCloud cloud = new AreaEffectCloud(level(), e.getX(), e.getY(), e.getZ());
                        cloud.setOwner(this);
                        cloud.setRadius(2.8f);
                        cloud.setDuration(120);
                        cloud.setParticle(ParticleTypes.ITEM_SLIME);
                        cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 2));
                        level().addFreshEntity(cloud);
                    }
                    sound(SoundEvents.WEATHER_RAIN, 1.5f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 780.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 14; }
    @Override public int getXpReward() { return 1200; }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.4f};
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (leapTicks > 0 && ++leapTicks > 5 && onGround()) {
            leapTicks = 0;
            aoe(position(), 3.2, 12f, e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)));
            burst(ParticleTypes.CLOUD, position(), 20, 1.5, 0.05);
            sound(SoundEvents.SPIDER_DEATH, 1f, 0.5f);
        } else if (leapTicks > 60) {
            leapTicks = 0;
        }
    }

    @Override
    protected void onPhase(int phase) {
        shout("Мои дети... пожрут вас!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        summon(EntityType.SPIDER, 2, 7, 14, null);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
    @Override public float getVoicePitch() { return 0.55f; }
}
