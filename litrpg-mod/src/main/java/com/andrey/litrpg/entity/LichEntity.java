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
import net.minecraft.world.entity.MobType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import com.andrey.litrpg.registry.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Лич-архимаг. Залпы магических снарядов, телепорт к цели,
 * призыв скелетов-стражей, после 30% HP — ярость.
 */
public class LichEntity extends BossBase implements RangedAttackMob {
    private int teleportCooldown = 200;
    private int summonCooldown = 300;
    private boolean enraged = false;

    public LichEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 30; }
    @Override public int getXpReward() { return 2500; }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 50, 22.0f));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LICH_STAFF.get()));
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
    public void performRangedAttack(LivingEntity target, float power) {
        int shots = enraged ? 3 : 1 + random.nextInt(2);
        for (int i = 0; i < shots; i++) {
            double spread = (i - (shots - 1) / 2.0) * 1.5;
            double dx = target.getX() - this.getX() + spread;
            double dy = target.getY(0.5) - this.getEyeY();
            double dz = target.getZ() - this.getZ() - spread;
            ManaBoltEntity bolt = new ManaBoltEntity(this.level(), this, dx, dy, dz);
            bolt.setPos(this.getX(), this.getEyeY() - 0.2, this.getZ());
            bolt.setDamage(enraged ? 11.0f : 8.0f);
            this.level().addFreshEntity(bolt);
        }
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.7f);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) return;
        LivingEntity target = getTarget();

        if (!enraged && getHealth() < getMaxHealth() * 0.3f) {
            enraged = true;
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.3f);
            summon(sl, 3);
        }

        if (target == null) return;

        if (--teleportCooldown <= 0) {
            teleportCooldown = enraged ? 120 : 200;
            for (int i = 0; i < 10; i++) {
                double x = target.getX() + (random.nextDouble() - 0.5) * 16;
                double z = target.getZ() + (random.nextDouble() - 0.5) * 16;
                double y = target.getY() + random.nextInt(4) - 1;
                double ox = getX(), oy = getY(), oz = getZ();
                if (this.randomTeleport(x, y, z, true)) {
                    sl.sendParticles(ParticleTypes.PORTAL, ox, oy + 1, oz, 40, 0.5, 1.0, 0.5, 0.2);
                    sl.playSound(null, ox, oy, oz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 0.7f);
                    break;
                }
            }
        }

        if (--summonCooldown <= 0) {
            summonCooldown = enraged ? 300 : 400;
            summon(sl, 2);
        }

        if (tickCount % 10 == 0) {
            sl.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.5, getZ(), 2, 0.4, 0.6, 0.4, 0.01);
        }
    }

    private void summon(ServerLevel sl, int count) {
        long nearby = sl.getEntitiesOfClass(SkeletonWardenEntity.class, getBoundingBox().inflate(24)).size();
        if (nearby >= 6) return;
        for (int i = 0; i < count; i++) {
            SkeletonWardenEntity s = ModEntities.SKELETON_WARDEN.get().create(sl);
            if (s == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            s.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360, 0);
            s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(s.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            if (getTarget() != null) s.setTarget(getTarget());
            sl.addFreshEntity(s);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, s.getX(), s.getY() + 1, s.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ManaBoltEntity bolt && bolt.getOwner() == this) return false;
        return super.hurt(source, amount);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
