package com.andrey.litrpg.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Монстры регионов «Мира Системы». У каждого — своя модель (tools/gen_bosses.py)
 * и маленькая особенность: бросок, заклинание, яд, щит.
 */
public final class Creatures {
    private Creatures() {}

    /** Общая основа: ближний бой + особый приём с перезарядкой и позой подготовки. */
    public abstract static class Base extends Monster implements Animated {
        private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(Base.class, EntityDataSerializers.INT);
        protected int special = 60;
        private int poseTicks;
        private float anim, animO;

        protected Base(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData() {
            super.defineSynchedData();
            entityData.define(POSE, 0);
        }

        @Override
        public int castPose() {
            return entityData.get(POSE);
        }

        @Override
        public float castAnim(float pt) {
            return Mth.lerp(pt, animO, anim);
        }

        protected void pose(int p, int ticks) {
            entityData.set(POSE, p);
            poseTicks = ticks;
        }

        /** Особый приём: вызывается, когда перезарядка прошла и цель есть. @return перезарядка в тиках или 0, если не сработал */
        protected abstract int special(LivingEntity target);

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide) {
                animO = anim;
                anim = Mth.approach(anim, castPose() > 0 ? 1f : 0f, 0.2f);
            }
        }

        @Override
        protected void customServerAiStep() {
            super.customServerAiStep();
            if (poseTicks > 0 && --poseTicks == 0) entityData.set(POSE, 0);
            LivingEntity t = getTarget();
            if (t != null && t.isAlive() && --special <= 0) {
                int cd = special(t);
                special = cd > 0 ? cd : 20;
            }
        }

        protected void shoot(BossProjectile.Kind kind, LivingEntity target, float dmg, float speed) {
            BossProjectile p = new BossProjectile(level(), this, kind, dmg);
            p.setPos(getX(), getEyeY() - 0.2, getZ());
            double dx = target.getX() - getX(), dy = target.getY(0.5) - p.getY(), dz = target.getZ() - getZ();
            if (kind.gravity > 0) dy += Math.sqrt(dx * dx + dz * dz) * kind.gravity * 2.2;
            p.shoot(dx, dy, dz, speed, 3f);
            level().addFreshEntity(p);
        }

        protected float dmg() {
            return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        }

        @Override
        protected void registerGoals() {
            goalSelector.addGoal(0, new FloatGoal(this));
            goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
            goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
            goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12f));
            goalSelector.addGoal(8, new RandomLookAroundGoal(this));
            targetSelector.addGoal(1, new HurtByTargetGoal(this));
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        }
    }

    // ---------------- Каменный тролль ----------------
    public static class StoneTroll extends Base {
        public StoneTroll(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50).add(Attributes.ATTACK_DAMAGE, 8)
                    .add(Attributes.MOVEMENT_SPEED, 0.22).add(Attributes.ARMOR, 8).add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                    .add(Attributes.ATTACK_KNOCKBACK, 1.2).add(Attributes.FOLLOW_RANGE, 32);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) < 4 || distanceTo(t) > 22) return 0;
            pose(1, 12);
            shoot(BossProjectile.Kind.ROCK, t, dmg() * 0.9f, 1.0f);
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1f, 0.6f);
            return 100;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (!level().isClientSide && tickCount % 40 == 0 && getHealth() < getMaxHealth()) heal(2f); // тролли регенерируют
        }

        @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
        @Override public float getVoicePitch() { return 0.6f; }
    }

    // ---------------- Ледяной призрак ----------------
    public static class FrostWraith extends Base {
        public FrostWraith(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 26).add(Attributes.ATTACK_DAMAGE, 5)
                    .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 32);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) > 20) return 0;
            pose(2, 14);
            shoot(BossProjectile.Kind.ICE, t, dmg(), 1.2f);
            playSound(SoundEvents.GLASS_BREAK, 0.8f, 1.6f);
            return 60;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.SNOWFLAKE, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, -0.02, 0);
            }
        }

        @Override public boolean canFreeze() { return false; }
        @Override public MobType getMobType() { return MobType.UNDEAD; }
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.STRAY_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.STRAY_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.STRAY_DEATH; }
    }

    // ---------------- Лесной огонёк ----------------
    public static class ForestWisp extends Base {
        public ForestWisp(EntityType<? extends Monster> type, Level level) {
            super(type, level);
            this.moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 14).add(Attributes.ATTACK_DAMAGE, 4)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.5).add(Attributes.FOLLOW_RANGE, 28);
        }

        @Override
        protected PathNavigation createNavigation(Level level) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            return nav;
        }

        @Override
        protected void registerGoals() {
            super.registerGoals();
            goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) > 18) return 0;
            pose(2, 10);
            shoot(BossProjectile.Kind.HEX, t, dmg(), 1.0f);
            if (random.nextInt(3) == 0) t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
            return 50;
        }

        @Override
        public void aiStep() {
            super.aiStep();
            if (level().isClientSide && random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.GLOW, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.01, 0);
            }
        }

        @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
        @Override protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {}
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ALLAY_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.ALLAY_DEATH; }
    }

    // ---------------- Пепельный бес ----------------
    public static class AshImp extends Base {
        public AshImp(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18).add(Attributes.ATTACK_DAMAGE, 5)
                    .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 30);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) > 18) return 0;
            pose(1, 10);
            shoot(BossProjectile.Kind.FIRE, t, dmg(), 1.2f);
            playSound(SoundEvents.BLAZE_SHOOT, 0.8f, 1.4f);
            return 70;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit) target.setSecondsOnFire(3);
            return hit;
        }

        @Override public boolean fireImmune() { return true; }
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BLAZE_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
        @Override public float getVoicePitch() { return 1.6f; }
    }

    // ---------------- Болотный ползун ----------------
    public static class BogLurker extends Base {
        public BogLurker(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 6)
                    .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 24);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) < 3 || distanceTo(t) > 14) return 0;
            pose(1, 10);
            shoot(BossProjectile.Kind.POISON, t, dmg() * 0.8f, 1.1f);
            playSound(SoundEvents.LLAMA_SPIT, 1f, 0.6f);
            return 80;
        }

        @Override
        public boolean doHurtTarget(Entity target) {
            boolean hit = super.doHurtTarget(target);
            if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
            return hit;
        }

        @Override public boolean canBreatheUnderwater() { return true; }
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.FROG_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.FROG_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.FROG_DEATH; }
        @Override public float getVoicePitch() { return 0.5f; }
    }

    // ---------------- Песчаный скорпион ----------------
    public static class SandScorpion extends Base {
        public SandScorpion(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 34).add(Attributes.ATTACK_DAMAGE, 7)
                    .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ARMOR, 6).add(Attributes.FOLLOW_RANGE, 24);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) > 3.2) return 0;
            pose(1, 12);
            t.hurt(damageSources().mobAttack(this), dmg() * 1.2f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            playSound(SoundEvents.SPIDER_HURT, 1f, 0.5f);
            return 90;
        }

        @Override public MobType getMobType() { return MobType.ARTHROPOD; }
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SPIDER_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
        @Override public float getVoicePitch() { return 0.7f; }
    }

    // ---------------- Могильный рыцарь ----------------
    public static class GraveKnight extends Base {
        public GraveKnight(EntityType<? extends Monster> type, Level level) { super(type, level); }

        public static AttributeSupplier.Builder attributes() {
            return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 44).add(Attributes.ATTACK_DAMAGE, 9)
                    .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.ARMOR, 12).add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                    .add(Attributes.FOLLOW_RANGE, 32);
        }

        /** Щит: удары спереди ослаблены вдвое. */
        @Override
        public boolean hurt(DamageSource source, float amount) {
            if (source.getEntity() != null && source.getSourcePosition() != null) {
                var to = source.getSourcePosition().subtract(position()).normalize();
                var look = getLookAngle();
                if (to.x * look.x + to.z * look.z > 0.5) {
                    amount *= 0.5f;
                    playSound(SoundEvents.SHIELD_BLOCK, 1f, 0.8f);
                }
            }
            return super.hurt(source, amount);
        }

        @Override
        protected int special(LivingEntity t) {
            if (distanceTo(t) > 4) return 0;
            pose(1, 14);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3), e -> e != this && !(e instanceof Monster))) {
                e.hurt(damageSources().mobAttack(this), dmg());
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            }
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1, getZ(), 4, 1.5, 0.2, 1.5, 0);
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
            return 120;
        }

        @Override public MobType getMobType() { return MobType.UNDEAD; }
        @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    }
}
