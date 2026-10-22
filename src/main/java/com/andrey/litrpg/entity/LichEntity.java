package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Лич-архимаг (ур. 30). Идея: «душа не здесь». Пока на арене целы филактерии (узлы душ),
 * смерть ему не страшна — он восстаёт с 40% здоровья. Разбейте филактерии, потом добивайте.
 * Ледяная гробница запирает союзника, «Смертельный рок» — метка: отнеси взрыв подальше от группы.
 * Святое оружие жжёт его вдвое.
 */
public class LichEntity extends BossBase implements RangedAttackMob {
    private int rises;

    public LichEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Ледяная гробница", 280, 20, 2).first(120)
                .act(t -> {
                    LivingEntity o = randomOther();
                    entomb(o, Blocks.PACKED_ICE.defaultBlockState(), 200, 0.08f, true);
                    shoutAll(o.getName().getString() + " во льду! Разбейте гробницу.");
                    sound(SoundEvents.GLASS_PLACE, 2f, 0.5f);
                });
        skill("Смертельный рок", 340, 10, 1).first(200)
                .act(t -> {
                    LivingEntity o = randomOther();
                    circle(o.position(), 4.5, 110, 1.0f).color(Telegraph.PURPLE).follow(o, 25)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)))
                            .fx(c -> {
                                burst(ParticleTypes.SOUL, c.add(0, 1, 0), 50, 2, 0.05);
                                soundAt(c, SoundEvents.WITHER_BREAK_BLOCK, 1.5f, 0.6f);
                            });
                    tip(o, "На вас «Смертельный рок»! Отбегите от группы!", ChatFormatting.DARK_PURPLE);
                    shoutAll("«Смертельный рок» на " + o.getName().getString() + " — не стойте рядом.");
                });
        skill("Взрыв некроэнергии", 300, 40, 2).first(260)
                .begin(t -> {
                    donut(position(), 5, 26, 40, 0.55f).color(Telegraph.PURPLE)
                            .fx(c -> burst(ParticleTypes.SCULK_SOUL, c.add(0, 1, 0), 80, 8, 0.05));
                    shoutAll("Некроэнергия! Ближе к Личу — у его ног безопасно.");
                });
        skill("Армия мёртвых", 480, 30, 2).first(300)
                .act(t -> {
                    summon(ModEntities.SKELETON_WARDEN.get(), 3, 6, 28, null);
                    sound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5f, 0.8f);
                });
        skill("Сдвиг", 240, 0, 0).silent().first(120)
                .act(t -> {
                    double ox = getX(), oy = getY(), oz = getZ();
                    if (blinkNear(arena(), 14)) {
                        burst(ParticleTypes.PORTAL, new net.minecraft.world.phys.Vec3(ox, oy + 1, oz), 40, 0.5, 0.2);
                        sound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.7f);
                    }
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 30; }
    @Override public int getXpReward() { return 3200; }

    @Override
    protected float toughness() {
        return 1.4f;
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEAD;
    }

    @Override
    protected List<String> intro() {
        return List.of("Филактерии (голубые узлы душ) возвращают Лича из мёртвых — разбейте их все.",
                "«Ледяная гробница» — союзник во льду. Разбейте лёд.",
                "«Смертельный рок» — метка взорвётся через 5 с. Помеченный — прочь от группы!",
                "«Взрыв некроэнергии» бьёт всех вдали от Лича — подойдите к нему.",
                "Святое оружие (Небесная кара) бьёт вдвое, стрелы — слабо.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case HOLY -> 2.0f;
            case FIRE -> 1.3f;
            case ARROW -> 0.6f;
            default -> 1f;
        };
    }

    @Override
    protected boolean onLethal(DamageSource src) {
        int left = relicsAlive();
        if (left <= 0) return false;
        rises++;
        setHealth(getMaxHealth() * 0.4f);
        hitAll(position(), 30, 0.3f, null);
        summon(ModEntities.SKELETON_WARDEN.get(), 2, 6, 28, null);
        burst(ParticleTypes.SOUL_FIRE_FLAME, position().add(0, 1, 0), 80, 1.5, 0.1);
        sound(SoundEvents.WITHER_SPAWN, 1.5f, 1.2f);
        shout("Смерть? Моя душа... не здесь!");
        shoutAll("Лич восстал (" + rises + "-й раз)! Филактерий осталось: " + left + ". Разбейте их!");
        return true;
    }

    @Override
    protected void onRelicBroken(int alive) {
        shout(alive > 0 ? "Нет! Мои сосуды!" : "Последний сосуд... разбит...");
        shoutAll(alive > 0 ? "Филактерия разбита. Осталось: " + alive : "Все филактерии разбиты — Лич смертен!");
        sound(SoundEvents.SOUL_ESCAPE, 2f, 0.6f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 50, 22.0f));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LICH_STAFF.get()));
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

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (isCasting()) return;
        int shots = phase > 0 ? 3 : 2;
        for (int i = 0; i < shots; i++) shoot(BossProjectile.Kind.SHADOW, i == 0 ? target : randomOther(), 0.2f, 1.3f, 3f);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2f, 0.7f);
    }

    @Override
    protected void onReset() {
        rises = 0;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Rises", rises);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rises = tag.getInt("Rises");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
