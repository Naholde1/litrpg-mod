package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Королева Арахна (ур. 14). Идея: логово. Коконы с яйцами (узлы силы) выводят паучат, пока их не разобьют.
 * Дважды за бой Королева уходит под своды — неуязвима, пока жив её выводок, а сверху льётся кислота.
 * Кокон из паутины запирает союзника — его надо вырезать. Огонь жжёт паутину и Королеву.
 */
public class SpiderQueenEntity extends BossBase {
    private int ceiling;
    private Vec3 ground;

    public SpiderQueenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Ядовитый укус", 110, 16, 1).first(40)
                .when(t -> distanceTo(t) < 5)
                .begin(t -> cone(position(), dirTo(t), 5, 40, 16, 0.8f)
                        .then(e -> {
                            e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
                            BossDamage.sunder(e, 1);
                        }).fx(c -> sound(SoundEvents.SPIDER_HURT, 1.5f, 0.4f)));
        skill("Ядовитый плевок", 90, 12, 1).silent()
                .act(t -> {
                    LivingEntity o = randomOther();
                    shoot(BossProjectile.Kind.POISON, o, 0.35f, 1.2f, 2f);
                    sound(SoundEvents.LLAMA_SPIT, 1.3f, 0.6f);
                });
        skill("Прыжок охотницы", 200, 6, 1).first(100)
                .act(t -> {
                    LivingEntity o = randomOther();
                    Vec3 at = o.position();
                    circle(at, 3.5, 22, 0.6f).color(Telegraph.PURPLE)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1)))
                            .fx(c -> {
                                teleportTo(c.x, c.y, c.z);
                                burst(ParticleTypes.CLOUD, c, 20, 1.5, 0.05);
                                sound(SoundEvents.SPIDER_DEATH, 1f, 0.5f);
                            });
                    tip(o, "Королева прыгает на вас — отбегите!", net.minecraft.ChatFormatting.RED);
                });
        skill("Кокон", 300, 20, 2).first(160)
                .act(t -> {
                    LivingEntity o = randomOther();
                    entomb(o, Blocks.COBWEB.defaultBlockState(), 200, 0.09f, false);
                    shoutAll(o.getName().getString() + " в коконе! Вырежьте паутину (меч рубит её быстро).");
                    sound(SoundEvents.SPIDER_STEP, 2f, 0.4f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 820.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 14; }
    @Override public int getXpReward() { return 1300; }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Коконы с яйцами (узлы силы) каждые 20 с выводят паучат — разбейте их.",
                "Дважды Королева уходит под своды: неуязвима, пока жив выводок. Перебейте паучат, уклоняясь от кислоты.",
                "«Кокон» — союзник в паутине. Вырежьте его, иначе задохнётся.",
                "Огонь бьёт Королеву почти вдвое.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case FIRE -> 1.8f;
            case SWORD -> 1.15f;
            default -> 1f;
        };
    }

    @Override
    protected void bossTick() {
        if (tickCount % 400 == 0 && getTarget() != null && ceiling <= 0) {
            int sacs = 0;
            for (BlockPos p : liveRelics()) {
                summonAt(EntityType.CAVE_SPIDER, Vec3.atBottomCenterOf(p.above()), 1, 10, 13, null);
                sacs++;
            }
            if (sacs > 0) shoutAll("Коконы лопаются — паучата! (коконов: " + sacs + ")");
        }
        if (ceiling > 0) {
            ceiling--;
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            boolean brood = !minions().isEmpty();
            if (ceiling % 30 == 0) {
                for (int i = 0; i < 4; i++) circle(randomSpot(15), 2.5, 30, 0.45f).color(Telegraph.GREEN)
                        .fx(c -> burst(ParticleTypes.ITEM_SLIME, c.add(0, 0.5, 0), 20, 1, 0.1));
            }
            if (!brood || ceiling <= 0) descend();
        }
    }

    private void climb() {
        ground = position();
        BlockPos b = blockPosition();
        int up = 0;
        while (up < 9 && level().getBlockState(b.above(up + 3)).isAir()) up++;
        setNoGravity(true);
        teleportTo(getX(), getY() + up, getZ());
        immune = "Королева под сводами — убейте её выводок!";
        ceiling = 20 * 25;
        summon(EntityType.CAVE_SPIDER, 4, 12, 14, null);
        summon(EntityType.SPIDER, 2, 12, 14, null);
        shout("Мои дети... пожрут вас!");
        shoutAll("Королева ушла под своды. Перебейте выводок — тогда она спустится.");
        sound(SoundEvents.SPIDER_AMBIENT, 2f, 0.4f);
    }

    private void descend() {
        ceiling = 0;
        immune = null;
        setNoGravity(false);
        if (ground != null) teleportTo(ground.x, ground.y, ground.z);
        circle(position(), 4, 1, 0.5f).fx(c -> burst(ParticleTypes.CLOUD, c, 30, 2, 0.05));
        stagger(60, "Королева рухнула вниз — бейте!");
    }

    @Override
    protected void onPhase(int phase) {
        climb();
    }

    @Override
    protected void onReset() {
        if (ceiling > 0) descend();
        stagger = 0;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        return hit;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && super.canBeAffected(effect);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Ceiling", ceiling);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        ceiling = tag.getInt("Ceiling");
        if (ceiling > 0) immune = "Королева под сводами — убейте её выводок!";
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
    @Override public float getVoicePitch() { return 0.55f; }
}
