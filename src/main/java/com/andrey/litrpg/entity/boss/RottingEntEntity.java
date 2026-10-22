package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Гниющий Энт (ур. 11). Идея: корни-сердца (узлы силы) питают его — пока живы, он зарастает раны.
 * Вырванное сердце оглушает Энта. Огонь и топоры — его беда. Цветение гнили бьёт всех вдали —
 * держитесь у его ног; споры — разбегайтесь; корни хватают и душат тех, кто стоит позади.
 */
public class RottingEntEntity extends BossBase {
    public RottingEntEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Удар ветвями", 100, 18, 1).first(50)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 45, 18, 0.85f).then(e -> knockFrom(e, position(), 1.2, 0.4))
                        .fx(c -> sound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.4f, 0.5f)));
        skill("Хватка корней", 220, 20, 2).first(140)
                .act(t -> {
                    LivingEntity o = randomOther();
                    entomb(o, net.minecraft.world.level.block.Blocks.MANGROVE_ROOTS.defaultBlockState(), 160, 0.07f, true);
                    burst(ParticleTypes.SPORE_BLOSSOM_AIR, o.position().add(0, 1, 0), 30, 0.8, 0.02);
                    sound(SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.5f);
                });
        skill("Гнилые споры", 200, 24, 1).first(90)
                .begin(t -> {
                    for (LivingEntity e : fighters()) circle(e.position(), 3, 24, 0.4f).color(Telegraph.GREEN)
                            .then(x -> x.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)))
                            .fx(c -> burst(ParticleTypes.SPORE_BLOSSOM_AIR, c.add(0, 1, 0), 40, 1.5, 0.02));
                });
        skill("Цветение гнили", 320, 40, 2).first(240).unstoppable()
                .begin(t -> {
                    donut(position(), 5, 22, 40, 0.6f).color(Telegraph.GREEN)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0)))
                            .fx(c -> {
                                for (int i = 0; i < 6; i++) burst(ParticleTypes.SPORE_BLOSSOM_AIR, randomSpot(16).add(0, 1, 0), 30, 2, 0.02);
                                sound(SoundEvents.AZALEA_LEAVES_BREAK, 2f, 0.4f);
                            });
                    shoutAll("Цветение гнили — бегите к ногам Энта, вдали погибнете!");
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 760.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override public int getBossLevel() { return 11; }
    @Override public int getXpReward() { return 1000; }

    @Override
    protected List<String> intro() {
        return List.of("Корни-сердца на арене (зелёные узлы) лечат Энта — разбейте их, это ещё и оглушит его.",
                "Огонь жжёт Энта вдвое, топоры рубят на 60% сильнее.",
                "«Цветение гнили» бьёт всех вдали — прижмитесь к нему. «Споры» — разойдитесь.",
                "«Хватка корней» запирает союзника в клетку корней — разрубите её.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case FIRE -> 2.0f;
            case AXE -> 1.6f;
            case ARROW -> 0.7f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        if (tickCount % 40 == 0 && getTarget() != null) {
            int hearts = relicsAlive();
            if (hearts > 0 && getHealth() < getMaxHealth()) {
                heal(getMaxHealth() * 0.006f * hearts);
                if (level() instanceof ServerLevel sl) {
                    for (BlockPos p : liveRelics()) {
                        Vec3 from = Vec3.atCenterOf(p);
                        Vec3 to = position().add(0, 2, 0);
                        for (int i = 0; i <= 8; i++) {
                            Vec3 q = from.lerp(to, i / 8.0);
                            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, q.x, q.y, q.z, 1, 0, 0, 0, 0);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        stagger(80, "Корень-сердце вырван! Энт оглушён на 4 с. Осталось сердец: " + alive);
        sound(SoundEvents.ROOTED_DIRT_BREAK, 2.5f, 0.4f);
        if (alive == 0) shout("Корни... мои корни!..");
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Лес... поглотит вас!");
        summon(EntityType.ZOMBIE, 3, 5, 11, z -> z.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0)));
        sound(SoundEvents.ZOMBIE_VILLAGER_AMBIENT, 1.5f, 0.4f);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOOD_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_BREAK; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR; }
    @Override public float getVoicePitch() { return 0.4f; }
}
