package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
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
import net.minecraft.world.level.block.Blocks;

/**
 * Гниющий Энт (ур. 11). Опутывает корнями всех вокруг, распускает
 * ядовитые споры, бьёт ветвями. Ранен — зовёт гнилых слуг и регенерирует.
 */
public class RottingEntEntity extends BossBase {
    public RottingEntEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        var dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
        skill("Хватка корней", 170, 28, 2)
                .during(left -> {
                    if (left % 4 == 0) for (LivingEntity e : enemiesAround(14)) burst(dirt, e.position(), 6, 0.6, 0.05);
                })
                .act(t -> {
                    for (LivingEntity e : enemiesAround(14)) {
                        e.hurt(damageSources().mobAttack(this), 6f);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                        e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                        burst(dirt, e.position(), 30, 0.5, 0.15);
                    }
                    sound(SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.5f);
                });
        skill("Гнилые споры", 200, 20, 1)
                .act(t -> {
                    AreaEffectCloud cloud = new AreaEffectCloud(level(), t.getX(), t.getY(), t.getZ());
                    cloud.setOwner(this);
                    cloud.setRadius(3.5f);
                    cloud.setRadiusPerTick(-0.01f);
                    cloud.setDuration(140);
                    cloud.setParticle(ParticleTypes.SPORE_BLOSSOM_AIR);
                    cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    cloud.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    level().addFreshEntity(cloud);
                    sound(SoundEvents.AZALEA_LEAVES_BREAK, 1.5f, 0.6f);
                });
        skill("Удар ветвями", 80, 12, 1)
                .when(t -> distanceTo(t) < 5)
                .act(t -> {
                    aoe(position(), 4.5, 12f, e -> knockFrom(e, position(), 1.2, 0.4));
                    burst(ParticleTypes.SWEEP_ATTACK, position().add(0, 1.5, 0), 6, 2, 0);
                    sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.4f, 0.5f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 640.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.21)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0);
    }

    @Override public int getBossLevel() { return 11; }
    @Override public int getXpReward() { return 900; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.5));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void onPhase(int phase) {
        shout("Лес... помнит... каждую срубленную ветвь!");
        addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 600, 0));
        summon(EntityType.ZOMBIE, 3, 5, 11, z -> z.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0)));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && effect.getEffect() != MobEffects.CONFUSION && super.canBeAffected(effect);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOOD_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_BREAK; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
