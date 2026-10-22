package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Ксал'Атар, Глаз Бездны (ур. 62) — вершина Шпиля. Идеи: «спрячься от взгляда» и «бей, пока открыт».
 * Парит над ареной. Глаз открыт 20 с и закрыт 8 с (неуязвим, зовёт отродий).
 * «Взгляд Бездны» — луч в игрока: спасает только колонна между вами и Глазом.
 * Тяжесть Бездны давит на танка, сферы — на остальных; на 30% тьма сжимает арену.
 */
public class XalatharEntity extends BossBase {
    private int cycle;
    private double squeeze = 22;

    public XalatharEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        setNoGravity(true);
        skill("Тяжесть Бездны", 80, 10, 1).silent()
                .act(t -> {
                    hit(t, 0.55f);
                    BossDamage.sunder(t, 1);
                    burst(ParticleTypes.REVERSE_PORTAL, t.position().add(0, 1, 0), 30, 0.5, 0.1);
                });
        skill("Пустотные сферы", 100, 12, 1).silent()
                .act(t -> {
                    for (int i = 0; i < 3; i++) shoot(BossProjectile.Kind.SHADOW, randomOther(), 0.35f, 1.1f, 5f);
                    sound(SoundEvents.WITHER_SHOOT, 1.2f, 0.5f);
                });
        skill("Взгляд Бездны", 400, 50, 1).first(160).unstoppable()
                .begin(t -> {
                    LivingEntity o = randomOther();
                    Vec3 base = new Vec3(getX(), o.getY(), getZ());
                    line(base, o.position().subtract(base), 34, 1.8, 50, 1.4f).color(Telegraph.PURPLE)
                            .spare(this::hidden)
                            .fx(c -> {
                                for (int i = 0; i < 30; i++) burst(ParticleTypes.DRAGON_BREATH, c.add(o.position().subtract(base).normalize().scale(i)).add(0, 1, 0), 3, 0.3, 0.01);
                                sound(SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.7f);
                            });
                    tip(o, "Глаз смотрит на вас — спрячьтесь за колонну!", ChatFormatting.DARK_PURPLE);
                    shoutAll("ВЗГЛЯД БЕЗДНЫ на " + o.getName().getString() + "! Спрячьтесь за колонну.");
                });
        skill("Безумие", 360, 20, 2).first(220)
                .act(t -> {
                    for (LivingEntity e : fighters()) {
                        if (random.nextBoolean()) e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 160, 0));
                    }
                    hitAll(arena(), 30, 0.28f, null);
                    sound(SoundEvents.WARDEN_ROAR, 2f, 0.8f);
                });
    }

    /** Колонна между Глазом и игроком — луч не достанет. */
    private boolean hidden(LivingEntity e) {
        var hit = level().clip(new ClipContext(getEyePosition(), e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FLYING_SPEED, 0.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 62; }
    @Override public int getXpReward() { return 16000; }

    @Override
    protected float toughness() {
        return 4.2f;
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 7;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.3f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Глаз открыт 20 с, затем закрывается на 8 с — неуязвим и зовёт отродий Бездны. Перебейте их.",
                "«Взгляд Бездны» — луч в одного: встаньте так, чтобы между вами и Глазом была колонна.",
                "«Тяжесть Бездны» давит на того, кто держит Глаз — это работа танка.",
                "На 30% тьма сжимает арену. Стрелы и святое оружие бьют по Глазу сильнее.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case ARROW -> 1.4f;
            case HOLY -> 1.3f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        // висит над центром арены
        Vec3 c = arena().add(0, 4, 0);
        if (position().distanceToSqr(c) > 1) teleportTo(c.x, c.y, c.z);
        setDeltaMovement(Vec3.ZERO);
        LivingEntity t = getTarget();
        if (t != null) getLookControl().setLookAt(t, 30, 30);
        if (t == null) return;
        cycle++;
        if (cycle == 400) {
            immune = "Глаз закрыт — бейте отродий!";
            summon(EntityType.VEX, 3, 8, 58, null);
            summon(EntityType.ENDERMITE, 3, 8, 58, null);
            shoutAll("Глаз закрылся! Отродья Бездны — уничтожьте их.");
            sound(SoundEvents.ENDER_EYE_DEATH, 2f, 0.5f);
        } else if (cycle >= 560) {
            cycle = 0;
            immune = null;
            shoutAll("Глаз открылся — бейте!");
        }
        if (phase >= 1 && fightTicks % 200 == 0) {
            squeeze = Math.max(7, squeeze - 3);
            donut(arena(), squeeze, 34, 60, 0.6f).color(Telegraph.PURPLE)
                    .fx(x -> burst(ParticleTypes.REVERSE_PORTAL, x.add(0, 1, 0), 120, squeeze + 4, 0.05));
        }
    }

    @Override
    protected void onPhase(int phase) {
        shout("ВЫ... ВИДЕЛИ... СЛИШКОМ МНОГО.");
        shoutAll("Тьма сжимает арену — к центру!");
    }

    @Override
    protected void onReset() {
        cycle = 0;
        squeeze = 22;
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ENDER_EYE_LAUNCH; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SLIME_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_GROWL; }
    @Override public float getVoicePitch() { return 0.4f; }
}
