package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Король гоблинов. Фазы:
 *  - прыжок с ударом по земле (каждые ~7 с);
 *  - при 50% HP зовёт свиту и впадает в ярость.
 */
public class GoblinKingEntity extends BossBase {
    private int leapCooldown = 100;
    private boolean leaping = false;
    private int leapTicks = 0;
    private boolean summoned = false;

    public GoblinKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override public int getBossLevel() { return 15; }
    @Override public int getXpReward() { return 900; }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) return;
        LivingEntity target = this.getTarget();

        if (!summoned && this.getHealth() < this.getMaxHealth() * 0.5f) {
            summoned = true;
            summonGuards(sl);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 0));
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.0f, 0.7f);
        }

        if (leaping) {
            leapTicks++;
            if (leapTicks > 6 && this.onGround()) {
                leaping = false;
                slam(sl);
            } else if (leapTicks > 60) {
                leaping = false;
            }
            return;
        }

        if (leapCooldown > 0) leapCooldown--;
        if (target != null && leapCooldown <= 0 && this.onGround()) {
            double dist = this.distanceTo(target);
            if (dist > 3.5 && dist < 18) {
                Vec3 dir = target.position().subtract(this.position());
                Vec3 h = new Vec3(dir.x, 0, dir.z).normalize().scale(Math.min(dist * 0.12, 1.6));
                this.setDeltaMovement(h.x, 0.95, h.z);
                this.hasImpulse = true;
                leaping = true;
                leapTicks = 0;
                leapCooldown = summoned ? 90 : 140;
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0f, 0.6f);
            }
        }
    }

    private void slam(ServerLevel sl) {
        sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.3, getZ(), 8, 2.5, 0.3, 2.5, 0.0);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.3, getZ(), 20, 3.0, 0.3, 3.0, 0.02);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.6f);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(6.0, 2.0, 6.0))) {
            if (e == this || e instanceof GoblinEntity) continue;
            e.hurt(this.damageSources().mobAttack(this), 9.0f);
            Vec3 push = e.position().subtract(this.position()).normalize().scale(1.2);
            e.push(push.x, 0.7, push.z);
            e.hurtMarked = true;
        }
    }

    private void summonGuards(ServerLevel sl) {
        for (int i = 0; i < 4; i++) {
            EntityType<? extends GoblinEntity> type = i == 3 ? ModEntities.GOBLIN_SHAMAN.get() : ModEntities.GOBLIN.get();
            GoblinEntity g = type.create(sl);
            if (g == null) continue;
            double a = Math.PI * 2 * i / 4;
            g.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360, 0);
            g.finalizeSpawn(sl, sl.getCurrentDifficultyAt(g.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            if (getTarget() != null) g.setTarget(getTarget());
            sl.addFreshEntity(g);
            sl.sendParticles(ParticleTypes.POOF, g.getX(), g.getY() + 0.5, g.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Summoned", summoned);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        summoned = tag.getBoolean("Summoned");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PIGLIN_BRUTE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PIGLIN_BRUTE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PIGLIN_BRUTE_DEATH; }
    @Override public float getVoicePitch() { return 0.8f; }
}
