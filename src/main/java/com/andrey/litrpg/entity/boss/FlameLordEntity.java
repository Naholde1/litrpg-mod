package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
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
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Игнис, Владыка пламени (ур. 45). Идеи: «пол — это лава» и «остуди его».
 * Каждые 45 с Прилив магмы заливает арену — выживут только те, кто стоит на обсидиановых островах.
 * Игнис раскаляется (+8% урона каждые 10 с); снежки и холод сбивают жар. Метеор надо принять вдвоём,
 * иначе он ударит по всем.
 */
public class FlameLordEntity extends BossBase {
    private int heat;

    public FlameLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Огненный залп", 90, 12, 1).silent()
                .act(t -> {
                    for (int i = 0; i < 3; i++) shoot(BossProjectile.Kind.FIRE, randomOther(), 0.3f, 1.3f, 4f);
                    sound(SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
                });
        skill("Сокрушающий жар", 130, 18, 1).first(50)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 40, 18, 1.0f).color(Telegraph.ORANGE)
                        .then(e -> {
                            e.setSecondsOnFire(6);
                            BossDamage.sunder(e, 1);
                        }).fx(c -> burst(ParticleTypes.FLAME, c.add(getLookAngle().scale(3)).add(0, 1, 0), 60, 2, 0.1)));
        skill("Прилив магмы", 900, 20, 2).first(300).unstoppable()
                .act(t -> {
                    circle(arena(), 30, 100, 1.3f).color(Telegraph.ORANGE).spare(this::onIsland)
                            .then(e -> e.setSecondsOnFire(8))
                            .fx(c -> {
                                for (int i = 0; i < 12; i++) burst(ParticleTypes.LAVA, randomSpot(18).add(0, 0.5, 0), 20, 2, 0.2);
                                sound(SoundEvents.LAVA_EXTINGUISH, 3f, 0.4f);
                            });
                    shout("Пусть пол станет огнём!");
                    shoutAll("ПРИЛИВ МАГМЫ через 5 с! Встаньте на обсидиан — на острова арены!");
                    sound(SoundEvents.LAVA_AMBIENT, 3f, 0.5f);
                });
        skill("Метеор", 360, 20, 2).first(200).unstoppable()
                .act(t -> {
                    var at = randomSpot(12);
                    circle(at, 3.2, 90, 1.6f).color(Telegraph.GOLD).soak(2).then(e -> e.setSecondsOnFire(5))
                            .fx(c -> {
                                burst(ParticleTypes.EXPLOSION_EMITTER, c.add(0, 0.5, 0), 1, 0, 0);
                                soundAt(c, SoundEvents.GENERIC_EXPLODE, 2.5f, 0.6f);
                            });
                    shoutAll("Падает метеор! Двое или больше — встаньте в золотой круг, разделите удар!");
                });
        skill("Огненное кольцо", 280, 30, 2).first(150)
                .begin(t -> donut(position(), 3.5, 15, 30, 0.6f).color(Telegraph.ORANGE).then(e -> e.setSecondsOnFire(4))
                        .fx(c -> ring(ParticleTypes.FLAME, c, 9, 60)));
        skill("Призыв ифритов", 600, 30, 2).fromPhase(1).first(100)
                .act(t -> {
                    summon(EntityType.BLAZE, 3, 5, 43, null);
                    sound(SoundEvents.BLAZE_AMBIENT, 2f, 0.6f);
                });
    }

    private boolean onIsland(LivingEntity e) {
        for (int d = 1; d <= 3; d++) {
            BlockState st = level().getBlockState(e.blockPosition().below(d));
            if (st.is(Blocks.OBSIDIAN) || st.is(Blocks.CRYING_OBSIDIAN)) return true;
            if (!st.isAir()) return false;
        }
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 45; }
    @Override public int getXpReward() { return 6800; }

    @Override
    protected float toughness() {
        return 2.6f;
    }

    @Override
    protected List<String> intro() {
        return List.of("«Прилив магмы»: через 5 с после крика весь пол сожжёт. Стойте на обсидиане (острова арены).",
                "Игнис раскаляется: +8% урона каждые 10 с. Каждый снежок в него сбивает жар. Холод бьёт его вдвое.",
                "«Метеор» (золотой круг) — встаньте в него вдвоём и больше, иначе он ударит по всем.",
                "Огонь его не ранит.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case FROST -> 2.0f;
            case FIRE -> 0f;
            default -> 1f;
        };
    }

    @Override
    protected float powerMult() {
        return 1f + 0.08f * heat;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof Snowball && !level().isClientSide) {
            if (heat > 0) {
                heat--;
                burst(ParticleTypes.CLOUD, position().add(0, 1.5, 0), 12, 0.5, 0.05);
                sound(SoundEvents.FIRE_EXTINGUISH, 1f, 1.2f);
                tip(source.getEntity(), "Жар Игниса: " + heat, net.minecraft.ChatFormatting.AQUA);
            }
            return super.hurt(source, Math.max(amount, 2f));
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void bossTick() {
        if (tickCount % 4 == 0) burst(ParticleTypes.FLAME, position().add(0, 1.2, 0), 2 + heat / 2, 0.6, 0.02);
        if (getTarget() != null && fightTicks > 0 && fightTicks % 200 == 0 && heat < 20) {
            heat++;
            if (heat % 3 == 0) shoutAll("Жар Игниса: " + heat + " (+" + heat * 8 + "% урона). Остудите его снежками!");
        }
        if (phase > 0 && tickCount % 20 == 0) {
            hitAll(position(), 4.5, 0.08f, e -> e.setSecondsOnFire(3));
            ring(ParticleTypes.SMALL_FLAME, position(), 4, 20);
        }
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Гори! ГОРИ ВМЕСТЕ СО МНОЙ!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        burst(ParticleTypes.FLAME, position().add(0, 1.5, 0), 120, 2, 0.3);
        sound(SoundEvents.BLAZE_DEATH, 2f, 0.4f);
    }

    @Override
    protected void onReset() {
        heat = 0;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) target.setSecondsOnFire(5);
        return hit;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Heat", heat);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        heat = tag.getInt("Heat");
    }

    @Override public boolean isOnFire() { return false; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_BURN; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
