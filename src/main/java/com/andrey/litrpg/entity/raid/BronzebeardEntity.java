package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Бронзобород, Механический страж (Глубины Кхаз-Дарума, ур. 32). Идея: «погаси горны».
 * Три горна (узлы силы) каждые 25 с раскаляют его — щит, а сбитый щит перегревает механизм (оглушение).
 * Пока горны целы, щит возвращается. Заводные мины под каждым, паровые выбросы, раскол брони танка.
 */
public class BronzebeardEntity extends BossBase {
    public BronzebeardEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        skill("Молот-кузнец", 120, 18, 1).first(50)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 40, 18, 1.05f).then(e -> BossDamage.sunder(e, 2))
                        .fx(c -> sound(SoundEvents.ANVIL_LAND, 2f, 0.5f)));
        skill("Паровой выброс", 240, 26, 2).first(120)
                .begin(t -> {
                    LivingEntity o = randomOther();
                    cone(position(), dirTo(o), 12, 30, 26, 0.6f).color(Telegraph.BLUE)
                            .then(e -> knockFrom(e, position(), 1.8, 0.5))
                            .fx(c -> burst(ParticleTypes.CLOUD, c.add(dirTo(o).scale(5)).add(0, 1, 0), 80, 2.5, 0.1));
                });
        skill("Заводные мины", 360, 20, 2).first(200)
                .act(t -> {
                    for (LivingEntity e : fighters()) {
                        circle(e.position(), 3, 70, 0.65f).color(Telegraph.ORANGE)
                                .fx(c -> {
                                    burst(ParticleTypes.EXPLOSION, c.add(0, 0.5, 0), 2, 0.5, 0);
                                    soundAt(c, SoundEvents.GENERIC_EXPLODE, 1.2f, 1.2f);
                                });
                    }
                    shoutAll("Под каждым — заводная мина! Отойдите от своей метки и не стойте на чужих.");
                    sound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 2f, 0.6f);
                });
        skill("Расплавленная броня", 420, 30, 2).fromPhase(1).first(150)
                .act(t -> {
                    hitAll(arena(), 30, 0.3f, e -> e.setSecondsOnFire(3));
                    burst(ParticleTypes.LAVA, position().add(0, 3, 0), 40, 3, 0.2);
                    sound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override public int getBossLevel() { return 32; }
    @Override public int getXpReward() { return 3800; }

    @Override
    protected float toughness() {
        return 2.2f;
    }

    @Override
    protected List<String> intro() {
        return List.of("Три горна (огненные узлы) каждые 25 с одевают стража в раскалённый щит. Разбейте горны!",
                "Сбитый щит перегревает механизм — Бронзобород оглушён на 5 с, бейте изо всех сил.",
                "«Заводные мины» — у каждого под ногами. Разойдитесь.",
                "Холод бьёт его сильнее, кирки — тоже; стрелы отскакивают от бронзы.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case FROST -> 1.5f;
            case PICKAXE -> 1.3f;
            case ARROW -> 0.6f;
            case FIRE -> 0.5f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        if (tickCount % 5 == 0) burst(ParticleTypes.CLOUD, position().add(0, getBbHeight() + 0.5, 0), 1, 0.4, 0.02);
        if (getTarget() == null || fightTicks == 0 || fightTicks % 500 != 0) return;
        int forges = relicsAlive();
        if (forges > 0) {
            setBarrier(getMaxHealth() * 0.07f * forges);
            shoutAll("Горны раскалили Бронзоборода (" + forges + ") — сбейте щит!");
            sound(SoundEvents.BLASTFURNACE_FIRE_CRACKLE, 2f, 0.5f);
        }
    }

    @Override
    protected void onBarrierBroken() {
        stagger(100, "Механизм перегрелся! Бронзобород оглушён на 5 с.");
    }

    @Override
    protected void onRelicBroken(int alive) {
        shoutAll(alive > 0 ? "Горн погашен. Осталось: " + alive : "Все горны погашены — щит больше не вернётся!");
        sound(SoundEvents.FIRE_EXTINGUISH, 2f, 0.5f);
    }

    @Override
    protected void onPhase(int phase) {
        shout("ПРОТОКОЛ... ЗАЩИТЫ... ХОЛДА...");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ANVIL_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
