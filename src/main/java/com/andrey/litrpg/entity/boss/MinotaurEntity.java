package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Астерион, Минотавр (ур. 25). Идея: «обмани быка». Он копит ярость (+10% урона каждые 20 с),
 * сбросить её можно, только если он врежется в стену или колонну: игрок, на которого нацелен таран,
 * встаёт перед колонной и уходит в последний миг. Минотавр оглушён, ярость сброшена, колонна рушится —
 * колонн на арене мало, так что каждую надо использовать с толком.
 */
public class MinotaurEntity extends BossBase {
    private int chargeTicks = 0;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<UUID> chargeHit = new HashSet<>();
    private int rage;

    public MinotaurEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        var sand = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState());
        skill("Таранный рывок", 300, 40, 1).first(160)
                .begin(t -> {
                    LivingEntity o = randomOther();
                    chargeDir = dirTo(o);
                    line(position(), chargeDir, 26, 1.6, 40, 0).color(Telegraph.RED);
                    tip(o, "Минотавр нацелился на вас! Встаньте перед колонной и отскочите!", net.minecraft.ChatFormatting.RED);
                })
                .during(left -> burst(sand, position(), 4, 0.6, 0.05))
                .act(t -> {
                    chargeTicks = 40;
                    chargeHit.clear();
                    sound(SoundEvents.RAVAGER_ROAR, 1.6f, 0.8f);
                });
        skill("Вихрь секиры", 260, 30, 2).first(100)
                .begin(t -> circle(position(), 6.5, 30, 0.6f).then(e -> knockFrom(e, position(), 1.4, 0.5))
                        .fx(c -> {
                            burst(ParticleTypes.SWEEP_ATTACK, c.add(0, 1.2, 0), 16, 3, 0);
                            sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.4f);
                        }));
        skill("Сокрушающий удар", 120, 18, 1).first(50)
                .when(t -> distanceTo(t) < 5)
                .begin(t -> cone(position(), dirTo(t), 5.5, 35, 18, 1.0f)
                        .then(e -> {
                            BossDamage.sunder(e, 2);
                            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1));
                        })
                        .fx(c -> {
                            burst(sand, c.add(getLookAngle().scale(2)), 40, 1, 0.15);
                            sound(SoundEvents.ANVIL_LAND, 1.3f, 0.5f);
                        }));
        skill("Топот", 340, 24, 2).first(220).fromPhase(1)
                .act(t -> {
                    hitAll(arena(), 30, 0.28f, e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1)));
                    burst(ParticleTypes.EXPLOSION, position(), 8, 3, 0);
                    sound(SoundEvents.RAVAGER_STUNNED, 2.5f, 0.5f);
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
    @Override public int getXpReward() { return 2800; }

    @Override
    protected float toughness() {
        return 1.6f;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.4f};
    }

    @Override
    protected float powerMult() {
        return 1f + 0.1f * rage;
    }

    @Override
    protected List<String> intro() {
        return List.of("Минотавр копит ярость: +10% урона каждые 20 с.",
                "Ярость сбрасывается, только если он врежется в стену или колонну. Цель тарана — встань перед колонной и отпрыгни.",
                "Удар тараном почти наверняка убьёт не-танка. Колонны рушатся — их мало.",
                "«Сокрушающий удар» раскалывает броню танка (+24% урона на 20 с).");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case AXE -> 1.25f;
            case ARROW -> 0.8f;
            default -> 1f;
        };
    }

    @Override
    protected boolean canCast() {
        return super.canCast() && chargeTicks <= 0;
    }

    @Override
    protected void bossTick() {
        if (getTarget() != null && fightTicks > 0 && fightTicks % 400 == 0) {
            rage++;
            shoutAll("Ярость Минотавра: +" + rage * 10 + "% урона.");
            burst(ParticleTypes.ANGRY_VILLAGER, position().add(0, 3, 0), 6, 1, 0);
        }
        if (chargeTicks > 0) {
            chargeTicks--;
            getNavigation().stop();
            double speed = phase > 0 ? 1.1 : 0.95;
            setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
            setYRot((float) (Math.atan2(chargeDir.z, chargeDir.x) * 180 / Math.PI) - 90f);
            yBodyRot = getYRot();
            yHeadRot = getYRot();
            for (LivingEntity e : enemiesAround(position().add(chargeDir), 1.9)) {
                if (chargeHit.add(e.getUUID())) {
                    hit(e, 1.15f);
                    knockFrom(e, position(), 2.2, 0.8);
                }
            }
            if (tickCount % 2 == 0) burst(ParticleTypes.CAMPFIRE_COSY_SMOKE, position(), 2, 0.4, 0.01);
            if (horizontalCollision && chargeTicks < 38) {
                chargeTicks = 0;
                smashInto();
            }
        }
    }

    /** Врезался: оглушение, сброс ярости; колонна из резного красного песчаника рушится. */
    private void smashInto() {
        BlockPos front = BlockPos.containing(position().add(chargeDir.scale(1.6)));
        int broke = 0;
        for (BlockPos p : BlockPos.betweenClosed(front.offset(-1, 0, -1), front.offset(1, 6, 1))) {
            var st = level().getBlockState(p);
            if (st.is(Blocks.CUT_RED_SANDSTONE) || st.is(Blocks.CHISELED_RED_SANDSTONE) || st.is(Blocks.SMOOTH_RED_SANDSTONE)) {
                level().destroyBlock(p, false);
                broke++;
            }
        }
        rage = 0;
        hurt(damageSources().flyIntoWall(), getMaxHealth() * 0.04f);
        stagger(140, broke > 0 ? "Минотавр снёс колонну и оглушён на 7 с — ярость сброшена!" : "Минотавр врезался в стену — оглушён на 7 с, ярость сброшена!");
        shout("Р-р-р... стена!");
        burst(ParticleTypes.EXPLOSION, position().add(chargeDir).add(0, 1.5, 0), 3, 0.5, 0);
        sound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 2f, 0.5f);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Никто... не выходит... из Лабиринта!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        sound(SoundEvents.RAVAGER_ROAR, 3f, 0.4f);
    }

    @Override
    protected void onReset() {
        chargeTicks = 0;
        rage = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rage", rage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rage = tag.getInt("Rage");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }
}
