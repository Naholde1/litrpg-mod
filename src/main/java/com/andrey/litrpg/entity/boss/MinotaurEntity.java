package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Астерион, Минотавр (ур. 25). Таранит рогами — если врежется в стену,
 * оглушён и получает больше урона. Вихрь секиры, сокрушающий удар,
 * а в ярости — боевой рёв, от которого стынет кровь.
 */
public class MinotaurEntity extends BossBase {
    private int chargeTicks = 0;
    private int stunTicks = 0;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<UUID> chargeHit = new HashSet<>();

    public MinotaurEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        var sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState());
        skill("Таранный рывок", 170, 22, 1)
                .when(t -> distanceTo(t) > 5 && distanceTo(t) < 24 && stunTicks <= 0)
                .during(left -> burst(sand, position(), 4, 0.6, 0.05))
                .act(t -> {
                    Vec3 d = t.position().subtract(position());
                    chargeDir = new Vec3(d.x, 0, d.z).normalize();
                    chargeTicks = 34;
                    chargeHit.clear();
                    sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.8f);
                });
        skill("Вихрь секиры", 200, 24, 2)
                .when(t -> distanceTo(t) < 6)
                .during(left -> { if (left % 3 == 0) ring(ParticleTypes.SWEEP_ATTACK, position().add(0, 1, 0), 4.5, 10); })
                .act(t -> {
                    aoe(position(), 5.2, 20f, e -> knockFrom(e, position(), 1.4, 0.5));
                    burst(ParticleTypes.SWEEP_ATTACK, position().add(0, 1.2, 0), 16, 3, 0);
                    sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.4f);
                });
        skill("Сокрушающий удар", 120, 18, 1)
                .when(t -> distanceTo(t) < 4.5)
                .act(t -> {
                    if (distanceTo(t) < 5.5) {
                        t.hurt(damageSources().mobAttack(this), 28f);
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
                        t.push(0, 0.8, 0);
                        t.hurtMarked = true;
                    }
                    burst(sand, t.position(), 40, 1, 0.15);
                    sound(SoundEvents.ANVIL_LAND, 1.3f, 0.5f);
                });
        skill("Боевой рёв", 300, 20, 2).fromPhase(1)
                .act(t -> {
                    for (LivingEntity e : enemiesAround(16)) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                    }
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1));
                    burst(ParticleTypes.ANGRY_VILLAGER, position().add(0, 3, 0), 12, 1.5, 0);
                    sound(SoundEvents.RAVAGER_ROAR, 2.5f, 0.5f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.6);
    }

    @Override public int getBossLevel() { return 25; }
    @Override public int getXpReward() { return 2600; }

    @Override
    protected float toughness() {
        return stunTicks > 0 ? 0.9f : 1.3f;
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
        if (stunTicks > 0) {
            stunTicks--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (stunTicks % 5 == 0) burst(ParticleTypes.CRIT, position().add(0, getBbHeight() + 0.3, 0), 6, 0.5, 0.1);
            bossEvent.setProgress(getHealth() / getMaxHealth());
            return;
        }
        super.customServerAiStep();
        if (chargeTicks > 0) {
            chargeTicks--;
            getNavigation().stop();
            double speed = phase > 0 ? 1.05 : 0.9;
            setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
            setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180 / Math.PI) - 90f);
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            for (LivingEntity e : enemiesAround(position().add(chargeDir), 1.8)) {
                if (chargeHit.add(e.getUUID())) {
                    e.hurt(damageSources().mobAttack(this), 24f);
                    knockFrom(e, position(), 2.0, 0.7);
                }
            }
            if (tickCount % 2 == 0) burst(ParticleTypes.CAMPFIRE_COSY_SMOKE, position(), 2, 0.4, 0.01);
            if (horizontalCollision && chargeTicks < 32) {
                chargeTicks = 0;
                stunTicks = 60;
                shout("Р-р-р... стена!");
                burst(ParticleTypes.EXPLOSION, position().add(chargeDir).add(0, 1.5, 0), 3, 0.5, 0);
                sound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 2f, 0.5f);
                hurt(damageSources().flyIntoWall(), 15f);
            }
        }
    }

    @Override
    protected void onPhase(int phase) {
        shout("Никто... не выходит... из Лабиринта!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 0));
        sound(SoundEvents.RAVAGER_ROAR, 3f, 0.4f);
    }

    @Override
    protected void onReset() {
        chargeTicks = 0;
        stunTicks = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stun", stunTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stunTicks = tag.getInt("Stun");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }
}
