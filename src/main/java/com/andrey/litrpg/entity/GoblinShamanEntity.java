package com.andrey.litrpg.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Гоблин-шаман: держится на расстоянии, кидает магические снаряды и лечит сородичей. */
public class GoblinShamanEntity extends GoblinEntity implements RangedAttackMob {

    public GoblinShamanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 7;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // вместо ближнего боя — магия
        this.goalSelector.getAvailableGoals().removeIf(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.MeleeAttackGoal);
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 14.0f));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        double dx = target.getX() - this.getX();
        double dy = target.getY(0.5) - this.getEyeY();
        double dz = target.getZ() - this.getZ();
        ManaBoltEntity bolt = new ManaBoltEntity(this.level(), this, dx, dy, dz);
        bolt.setPos(this.getX(), this.getEyeY() - 0.1, this.getZ());
        bolt.setDamage(4.0f + this.getMaxHealth() * 0.05f);
        this.level().addFreshEntity(bolt);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.5f);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.tickCount % 100 == 0) {
            // лечит раненых гоблинов рядом
            for (GoblinEntity g : this.level().getEntitiesOfClass(GoblinEntity.class, this.getBoundingBox().inflate(8))) {
                if (g.getHealth() < g.getMaxHealth()) {
                    g.heal(4.0f);
                    if (this.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, g.getX(), g.getY() + 1, g.getZ(), 6, 0.3, 0.4, 0.3, 0.0);
                    }
                }
            }
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 0.5f, 1.4f);
        }
    }
}
