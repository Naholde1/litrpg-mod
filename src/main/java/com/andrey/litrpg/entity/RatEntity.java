package com.andrey.litrpg.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Крыса — городской вредитель из поручений («крысы в погребе таверны»).
 * Не считается монстром, поэтому стража её не трогает: разбираться игроку.
 * Крысиный король — крупный вожак стаи.
 */
public class RatEntity extends PathfinderMob implements Animated {
    private final boolean king;

    public RatEntity(EntityType<? extends RatEntity> type, Level level, boolean king) {
        super(type, level);
        this.king = king;
        this.xpReward = king ? 12 : 2;
    }

    public static AttributeSupplier.Builder attributes(boolean king) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, king ? 40 : 6)
                .add(Attributes.ATTACK_DAMAGE, king ? 4 : 1)
                .add(Attributes.MOVEMENT_SPEED, king ? 0.28 : 0.3)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.3f));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> !((Player) p).isCreative() && !p.isSpectator()));
    }

    public boolean isKing() {
        return king;
    }

    @Override public int castPose() { return 0; }
    @Override public float castAnim(float partialTick) { return 0; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SILVERFISH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SILVERFISH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SILVERFISH_DEATH; }
    @Override public float getVoicePitch() { return king ? 0.7f : 1.4f; }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
