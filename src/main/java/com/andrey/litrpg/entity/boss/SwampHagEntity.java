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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Морра, Болотная ведьма (ур. 20). Держится на расстоянии и мечет проклятия,
 * отравляет землю, ускользает болотным шагом и поднимает утопленников.
 * На половине здоровья пьёт из котла и насылает порчу на всех вокруг.
 */
public class SwampHagEntity extends BossBase implements RangedAttackMob {
    private boolean drankPotion = false;

    public SwampHagEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Залп проклятий", 110, 16, 1)
                .act(t -> {
                    int n = phase > 0 ? 5 : 3;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.HEX, t, 12f, 1.1f, 4f + i * 5f);
                    sound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, 1.3f);
                });
        skill("Ядовитая трясина", 180, 20, 2)
                .act(t -> {
                    AreaEffectCloud cloud = new AreaEffectCloud(level(), t.getX(), t.getY(), t.getZ());
                    cloud.setOwner(this);
                    cloud.setRadius(4f);
                    cloud.setDuration(200);
                    cloud.setParticle(ParticleTypes.WITCH);
                    cloud.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                    cloud.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    level().addFreshEntity(cloud);
                    sound(SoundEvents.BREWING_STAND_BREW, 1.5f, 0.6f);
                });
        skill("Болотный шаг", 160, 0, 0).silent()
                .when(t -> distanceTo(t) < 5)
                .act(t -> {
                    burst(ParticleTypes.SQUID_INK, position().add(0, 1, 0), 30, 0.6, 0.05);
                    if (blinkNear(t.position(), 12)) {
                        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
                        sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.6f);
                    }
                });
        skill("Зов утопленников", 360, 30, 2)
                .during(left -> { if (left % 5 == 0) burst(ParticleTypes.BUBBLE_POP, position(), 14, 2, 0.1); })
                .act(t -> {
                    summon(EntityType.DROWNED, 2, 5, 20, null);
                    sound(SoundEvents.DROWNED_AMBIENT, 1.5f, 0.6f);
                });
        skill("Порча", 260, 30, 2).fromPhase(1)
                .during(left -> { if (left % 4 == 0) for (LivingEntity e : enemiesAround(20)) burst(ParticleTypes.WITCH, e.position().add(0, 2, 0), 4, 0.3, 0); })
                .act(t -> {
                    for (LivingEntity e : enemiesAround(20)) {
                        e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                        e.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    }
                    sound(SoundEvents.WITCH_CELEBRATE, 1.5f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 20; }
    @Override public int getXpReward() { return 1800; }

    @Override
    protected float toughness() {
        return 1.2f;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 35, 50, 18.0f));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (isCasting()) return;
        shoot(BossProjectile.Kind.HEX, target, 10f, 1.2f, 2f);
        sound(SoundEvents.WITCH_THROW, 1f, 0.8f);
    }

    @Override
    protected void onPhase(int phase) {
        if (!drankPotion) {
            drankPotion = true;
            heal(getMaxHealth() * 0.15f);
            shout("Хе-хе... котёл ещё полон, деточки!");
            burst(ParticleTypes.HAPPY_VILLAGER, position().add(0, 1.5, 0), 30, 0.8, 0.1);
            sound(SoundEvents.WITCH_DRINK, 1.5f, 0.7f);
        }
    }

    @Override
    protected void onReset() {
        drankPotion = false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && effect.getEffect() != MobEffects.MOVEMENT_SLOWDOWN && super.canBeAffected(effect);
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Drank", drankPotion);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        drankPotion = tag.getBoolean("Drank");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITCH_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }
}
