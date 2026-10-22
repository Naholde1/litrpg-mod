package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Голем Руин (ур. 8) — каменный страж. Идея: камень бьют кирками, а не стрелами.
 * Каменная кожа — щит, который надо сбить за 8 секунд, иначе голем «вбирает» камень и лечится.
 * Обвалы с потолка, удар по земле (бегом из круга), тяжёлое сокрушение в танка.
 */
public class RuinGolemEntity extends BossBase {
    private int skinTimer;

    public RuinGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        var dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        skill("Сотрясение земли", 180, 34, 1).first(120)
                .begin(t -> circle(position(), 6.5, 34, 0.55f).color(Telegraph.ORANGE)
                        .then(e -> {
                            knockFrom(e, position(), 1.0, 0.6);
                            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                        })
                        .fx(c -> {
                            burst(dust, c.add(0, 0.3, 0), 80, 2.5, 0.15);
                            burst(ParticleTypes.EXPLOSION, c.add(0, 0.3, 0), 6, 2, 0);
                            sound(SoundEvents.GENERIC_EXPLODE, 1.4f, 0.6f);
                        }));
        skill("Сокрушение", 140, 22, 1).first(60)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 40, 22, 0.95f)
                        .then(e -> BossDamage.sunder(e, 1)).fx(c -> sound(SoundEvents.ANVIL_LAND, 1.5f, 0.5f)));
        skill("Бросок валуна", 110, 18, 1)
                .act(t -> {
                    LivingEntity o = randomOther();
                    int n = phase > 0 ? 3 : 1;
                    for (int i = 0; i < n; i++) shoot(BossProjectile.Kind.ROCK, o, 0.4f, 1.0f, i == 0 ? 1f : 9f);
                    sound(SoundEvents.IRON_GOLEM_ATTACK, 1.2f, 0.6f);
                });
        skill("Обвал", 260, 20, 2).first(200)
                .act(t -> {
                    List<LivingEntity> all = fighters();
                    for (LivingEntity e : all) {
                        circle(e.position(), 2.6, 50, 0.7f).color(Telegraph.ORANGE)
                                .fx(c -> {
                                    burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), c.add(0, 2, 0), 50, 1.2, 0.1);
                                    soundAt(c, SoundEvents.STONE_BREAK, 1.5f, 0.6f);
                                });
                    }
                    for (int i = 0; i < 3; i++) circle(randomSpot(16), 2.6, 50, 0.7f).color(Telegraph.ORANGE)
                            .fx(c -> burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()), c.add(0, 2, 0), 50, 1.2, 0.1));
                    shoutAll("С потолка сыплются камни — уходите из кругов!");
                    sound(SoundEvents.IRON_GOLEM_DAMAGE, 2f, 0.3f);
                });
        skill("Каменная кожа", 520, 20, 2).first(300)
                .act(t -> {
                    setBarrier(getMaxHealth() * 0.12f);
                    skinTimer = 160;
                    shoutAll("Каменная кожа! Сбейте щит за 8 секунд — кирками быстрее, иначе голем вберёт камень.");
                    burst(ParticleTypes.WAX_ON, position().add(0, 1.5, 0), 40, 1.2, 0.1);
                    sound(SoundEvents.ANVIL_PLACE, 1f, 0.5f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override public int getBossLevel() { return 8; }
    @Override public int getXpReward() { return 700; }

    @Override
    protected List<String> intro() {
        return List.of("Голем каменный: кирки бьют вдвое, стрелы почти не ранят.",
                "«Каменная кожа» — щит. Не сбили за 8 с — голем лечится.",
                "«Сокрушение» бьёт перед собой — танк, отверни голема от группы.",
                "«Обвал» — круги под каждым: разбегитесь.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case PICKAXE -> 2.0f;
            case BLUNT, AXE -> 1.3f;
            case ARROW -> 0.35f;
            case MAGIC -> 0.8f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        if (skinTimer > 0 && --skinTimer == 0 && barrier > 0) {
            barrier = 0;
            heal(getMaxHealth() * 0.12f);
            shout("Камень... к камню.");
            burst(ParticleTypes.HAPPY_VILLAGER, position().add(0, 2, 0), 20, 1, 0);
        }
    }

    @Override
    protected void onBarrierBroken() {
        skinTimer = 0;
        stagger(60, "Каменная кожа раскололась — голем оглушён!");
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Руины... не падут!");
        summon(EntityType.SILVERFISH, 4, 6, 8, null);
        sound(SoundEvents.IRON_GOLEM_DAMAGE, 1.5f, 0.4f);
    }

    @Override
    protected void onReset() {
        skinTimer = 0;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
