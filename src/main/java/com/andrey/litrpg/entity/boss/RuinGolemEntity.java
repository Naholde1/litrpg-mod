package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossProjectile;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
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
 * Голем Руин (ур. 8). Медленный и крепкий. Сотрясает землю, швыряет валуны,
 * на половине здоровья покрывается каменной кожей и будит стражей-чешуйниц.
 */
public class RuinGolemEntity extends BossBase {
    public RuinGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        var dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        skill("Сотрясение земли", 140, 30, 1)
                .when(t -> distanceTo(t) < 7)
                .during(left -> { if (left % 5 == 0) ring(dust, position(), 5.5, 28); })
                .act(t -> {
                    aoe(position(), 5.5, 14f, e -> {
                        knockFrom(e, position(), 0.9, 0.6);
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    });
                    burst(dust, position().add(0, 0.3, 0), 80, 2.5, 0.15);
                    burst(ParticleTypes.EXPLOSION, position().add(0, 0.3, 0), 6, 2, 0);
                    sound(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.6f);
                });
        skill("Бросок валуна", 90, 18, 1)
                .when(t -> distanceTo(t) > 4.5)
                .act(t -> {
                    int n = phase > 0 ? 3 : 1;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.ROCK, t, 10f, 1.0f, i == 0 ? 1f : 9f);
                    sound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.6f);
                });
        skill("Каменная кожа", 360, 20, 2).fromPhase(1)
                .act(t -> {
                    addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 2));
                    burst(ParticleTypes.WAX_ON, position().add(0, 1.5, 0), 40, 1.2, 0.1);
                    sound(SoundEvents.ANVIL_PLACE, 1f, 0.5f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override public int getBossLevel() { return 8; }
    @Override public int getXpReward() { return 600; }

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
    protected void onPhase(int phase) {
        shout("Руины... не падут!");
        summon(EntityType.SILVERFISH, 4, 6, 8, null);
        sound(SoundEvents.IRON_GOLEM_DAMAGE, 1.5f, 0.4f);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
