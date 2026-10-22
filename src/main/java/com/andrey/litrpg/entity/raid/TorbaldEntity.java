package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Торбальд Проклятый, Король-под-Горой (ур. 35) — последний хранитель Глубин. Идеи: «проклятое золото»
 * и «руны предков». Груды золота (узлы силы) питают его: каждая — минус 15% урона по нему, но разбивший
 * проклят. Почётная стража (две) делит с ним удары — пока стоит, он получает вдвое меньше.
 * «Гнев Подгорья» каждые 30 с бьёт всех, кроме стоящих на рунных плитах (резной камень) арены.
 */
public class TorbaldEntity extends BossBase {
    private boolean guarded;

    public TorbaldEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Проклятый удар", 120, 18, 1).first(50)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 40, 18, 1.0f)
                        .then(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                            BossDamage.sunder(e, 1);
                        }).fx(c -> sound(SoundEvents.WITHER_SKELETON_HURT, 2f, 0.5f)));
        skill("Призрачный рывок", 260, 22, 1).first(140)
                .begin(t -> {
                    LivingEntity o = randomOther();
                    line(position(), dirTo(o), 20, 1.4, 22, 0.75f).color(Telegraph.GREEN)
                            .fx(c -> {
                                var to = o.position();
                                teleportTo(to.x, to.y, to.z);
                                burst(ParticleTypes.SOUL, to.add(0, 1, 0), 40, 1, 0.05);
                            });
                });
        skill("Дрожь камня", 220, 28, 2).first(90)
                .begin(t -> circle(position(), 7, 28, 0.6f).then(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2)))
                        .fx(c -> sound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.6f)));
        skill("Гнев Подгорья", 600, 50, 2).first(300)
                .begin(t -> {
                    circle(arena(), 30, 50, 0.95f).color(Telegraph.GREEN).spare(this::onRune)
                            .fx(c -> {
                                burst(ParticleTypes.SOUL_FIRE_FLAME, c.add(0, 1, 0), 200, 12, 0.05);
                                sound(SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.6f);
                            });
                    shout("Гора помнит своих королей!");
                    shoutAll("ГНЕВ ПОДГОРЬЯ через 2,5 с! Встаньте на рунные плиты (резной камень на полу)!");
                });
    }

    private boolean onRune(LivingEntity e) {
        BlockState st = under(e);
        return st.is(Blocks.CHISELED_STONE_BRICKS) || st.is(Blocks.CHISELED_DEEPSLATE);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override public int getBossLevel() { return 35; }
    @Override public int getXpReward() { return 5200; }

    @Override
    protected float toughness() {
        return 2.6f;
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 6;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.4f};
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected List<String> intro() {
        return List.of("Груды проклятого золота (золотые узлы): каждая — минус 15% урона по Королю. Разбившего настигнет проклятие.",
                "Почётная стража делит удары: пока жив хоть один страж, Король получает вдвое меньше.",
                "«Гнев Подгорья» каждые 30 с бьёт всех, кроме стоящих на рунных плитах (резной камень).",
                "На 40% стража встаёт вновь. Святое оружие ранит Короля сильнее.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        float m = 1f - 0.15f * relicsAlive();
        if (guarded) m *= 0.5f;
        if (w == BossDamage.Weapon.HOLY) m *= 1.5f;
        return Math.max(0.1f, m);
    }

    @Override
    protected void onEngage() {
        guards();
    }

    private void guards() {
        summon(ModEntities.GRAVE_KNIGHT.get(), 2, 4, 34, g -> g.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 600, 0)));
        guarded = true;
        shoutAll("Почётная стража Короля! Пока она стоит — Король получает вдвое меньше урона.");
    }

    @Override
    protected void bossTick() {
        if (tickCount % 20 == 0) {
            boolean g = !minions().isEmpty();
            if (guarded && !g) shoutAll("Стража пала!");
            guarded = g;
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        Player p = level().getNearestPlayer(this, 40);
        for (var pos : relics.keySet()) {
            if (!level().getBlockState(pos).isAir()) continue;
            var near = level().getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 6, false);
            if (near != null) p = near;
        }
        if (p != null) {
            p.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 1));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
            tip(p, "Проклятие золота!", net.minecraft.ChatFormatting.DARK_GREEN);
        }
        shoutAll("Груда золота рассыпалась. Осталось: " + alive + " (−" + alive * 15 + "% урона по Королю).");
    }

    @Override
    protected void onPhase(int phase) {
        shout("Встаньте, братья! Последний рубеж!");
        guards();
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.05);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_SKELETON_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
