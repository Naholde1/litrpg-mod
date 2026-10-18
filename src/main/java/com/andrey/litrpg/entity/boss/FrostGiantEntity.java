package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Имир, Ледяной великан (ур. 38). Ледяная нова, волна ледяных шипов,
 * глыбы льда и стужа, призывающая зимних стрелков. Во второй фазе —
 * буран над всей ареной, в третьей — ледяная броня.
 */
public class FrostGiantEntity extends BossBase {
    private int blizzard = 0;

    public FrostGiantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        var ice = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState());
        skill("Ледяная нова", 180, 30, 2)
                .when(t -> distanceTo(t) < 8)
                .during(left -> { if (left % 4 == 0) ring(ParticleTypes.SNOWFLAKE, position(), 7, 36); })
                .act(t -> {
                    aoe(position(), 7.5, 18f, e -> {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                        e.setTicksFrozen(e.getTicksRequiredToFreeze() + 140);
                        knockFrom(e, position(), 0.8, 0.3);
                    });
                    burst(ice, position().add(0, 0.5, 0), 120, 4, 0.2);
                    burst(ParticleTypes.SNOWFLAKE, position().add(0, 1, 0), 80, 4, 0.1);
                    sound(SoundEvents.GLASS_BREAK, 2f, 0.5f);
                });
        skill("Ледяные шипы", 140, 20, 1)
                .act(t -> {
                    Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1).normalize();
                    Set<UUID> hit = new HashSet<>();
                    for (int i = 1; i <= 18; i++) {
                        final Vec3 p = position().add(dir.scale(i * 1.1));
                        Scheduler.later(i, () -> {
                            if (!isAlive()) return;
                            burst(ice, p.add(0, 0.6, 0), 14, 0.3, 0.1);
                            burst(ParticleTypes.SNOWFLAKE, p.add(0, 1, 0), 6, 0.2, 0.05);
                            for (LivingEntity e : enemiesAround(p, 1.6)) {
                                if (hit.add(e.getUUID())) {
                                    e.hurt(damageSources().mobAttack(this), 20f);
                                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                                    e.push(0, 0.9, 0);
                                    e.hurtMarked = true;
                                }
                            }
                        });
                    }
                    sound(SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 0.5f);
                });
        skill("Глыба льда", 100, 15, 1)
                .when(t -> distanceTo(t) > 6)
                .act(t -> {
                    int n = phase > 1 ? 3 : 1;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.ICE, t, 16f, 1.2f, i == 0 ? 1f : 8f);
                    sound(SoundEvents.SNOW_GOLEM_SHOOT, 1.5f, 0.4f);
                });
        skill("Зов зимы", 400, 30, 2)
                .act(t -> {
                    summon(EntityType.STRAY, 3, 6, 36, null);
                    sound(SoundEvents.POLAR_BEAR_WARNING, 2f, 0.6f);
                });
        skill("Буран", 320, 40, 2).fromPhase(1)
                .during(left -> burst(ParticleTypes.SNOWFLAKE, position().add(0, 4, 0), 10, 6, 0.05))
                .act(t -> {
                    blizzard = 200;
                    sound(SoundEvents.ELYTRA_FLYING, 1.5f, 0.4f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 2.0);
    }

    @Override public int getBossLevel() { return 38; }
    @Override public int getXpReward() { return 4500; }

    @Override
    protected float toughness() {
        return 2.0f;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.6f, 0.3f};
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (blizzard > 0) {
            blizzard--;
            if (blizzard % 4 == 0) burst(ParticleTypes.SNOWFLAKE, position().add(0, 3, 0), 40, 9, 0.08);
            if (blizzard % 20 == 0) {
                for (LivingEntity e : enemiesAround(18)) {
                    e.hurt(damageSources().freeze(), 4f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 60, e.getTicksFrozen() + 40));
                }
            }
        }
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Зима... приходит за всеми.");
        } else {
            shout("Лёд... крепче стали!");
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 600, 1));
            summon(EntityType.STRAY, 2, 6, 36, null);
        }
        sound(SoundEvents.POLAR_BEAR_WARNING, 3f, 0.4f);
    }

    @Override
    protected void onReset() {
        blizzard = 0;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.POLAR_BEAR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.POLAR_BEAR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.POLAR_BEAR_DEATH; }
    @Override public float getVoicePitch() { return 0.4f; }
}
