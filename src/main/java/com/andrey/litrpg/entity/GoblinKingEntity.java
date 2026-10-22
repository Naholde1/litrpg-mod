package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Король гоблинов (ур. 15). Идея: «пороховой король». Он раскидывает пороховые бочки, которые
 * взрываются через 6 с — танк должен подвести Короля к бочке: взрыв оглушает его и ранит.
 * Дважды зовёт свиту: пока жив хоть один телохранитель, Король получает вдвое меньше урона.
 */
public class GoblinKingEntity extends BossBase {
    private boolean guarded;

    public GoblinKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Удар секиры", 100, 16, 1).first(40)
                .when(t -> distanceTo(t) < 5)
                .begin(t -> cone(position(), dirTo(t), 5, 45, 16, 0.85f)
                        .then(e -> BossDamage.sunder(e, 1)).fx(c -> sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5f, 0.6f)));
        skill("Королевский прыжок", 220, 8, 1).first(120)
                .act(t -> {
                    LivingEntity o = randomOther();
                    circle(o.position(), 4, 24, 0.6f).color(Telegraph.ORANGE)
                            .then(e -> knockFrom(e, position(), 1.2, 0.6))
                            .fx(c -> {
                                teleportTo(c.x, c.y, c.z);
                                burst(ParticleTypes.EXPLOSION, c.add(0, 0.3, 0), 6, 2, 0);
                                sound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.7f);
                            });
                    setDeltaMovement(0, 0.9, 0);
                    hasImpulse = true;
                });
        skill("Пороховые бочки", 300, 20, 2).first(160)
                .act(t -> {
                    for (int i = 0; i < 2 + phase; i++) keg(randomSpot(12));
                    shout("Ха! Подарочки!");
                    shoutAll("Пороховые бочки взорвутся через 6 с. Танк — подведи Короля к бочке: взрыв его оглушит!");
                });
        skill("Горсть золота", 180, 12, 1).first(90)
                .begin(t -> cone(position(), dirTo(t), 9, 30, 12, 0.35f).color(Telegraph.GOLD)
                        .then(e -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)))
                        .fx(c -> burst(ParticleTypes.WAX_ON, position().add(getLookAngle().scale(3)).add(0, 1, 0), 40, 2, 0.2)));
    }

    /** Бочка: блок на 6 с, потом взрыв по кругу — бьёт и игроков, и Короля. */
    private void keg(Vec3 at) {
        BlockPos p = BlockPos.containing(at);
        tempBlock(p, Blocks.BARREL.defaultBlockState(), 125);
        circle(Vec3.atBottomCenterOf(p), 4.5, 120, 0.9f).color(Telegraph.ORANGE)
                .fx(c -> {
                    level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    burst(ParticleTypes.EXPLOSION_EMITTER, c.add(0, 0.5, 0), 1, 0, 0);
                    soundAt(c, SoundEvents.GENERIC_EXPLODE, 2f, 0.8f);
                    if (position().distanceTo(c) < 5) {
                        hurt(damageSources().explosion(null, null), getMaxHealth() * 0.06f);
                        stagger(120, "Король попал под взрыв бочки — оглушён на 6 с!");
                    }
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 860.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override public int getBossLevel() { return 15; }
    @Override public int getXpReward() { return 1400; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.7f, 0.35f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Пороховые бочки взрываются через 6 с: игроки — прочь, а Короля — к бочке! Взрыв оглушит и ранит его.",
                "На 70% и 35% Король зовёт свиту. Пока жив телохранитель — урон по Королю вдвое меньше.",
                "«Горсть золота» ослепляет всех перед ним.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return guarded ? 0.5f : 1f;
    }

    @Override
    protected void bossTick() {
        if (tickCount % 20 == 0) {
            boolean g = !minions().isEmpty();
            if (guarded && !g) shoutAll("Свита пала — Король беззащитен!");
            guarded = g;
        }
    }

    @Override
    protected void onPhase(int phase) {
        shout(phase == 1 ? "Стража! Ко мне!" : "Все сюда! Защищайте короля!");
        summon(ModEntities.GOBLIN.get(), 3 + phase, 8, 15, g -> g.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 600, 0)));
        summon(ModEntities.GOBLIN_SHAMAN.get(), 1, 9, 15, null);
        guarded = true;
        shoutAll("Свита! Пока жив телохранитель, Король получает вдвое меньше урона.");
        sound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.7f);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return result;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.PIGLIN_BRUTE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PIGLIN_BRUTE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PIGLIN_BRUTE_DEATH; }
    @Override public float getVoicePitch() { return 0.8f; }
}
