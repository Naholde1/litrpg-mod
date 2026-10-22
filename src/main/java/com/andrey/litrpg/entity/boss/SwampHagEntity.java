package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Морра, Болотная ведьма (ур. 20). Идеи: «найди настоящую» и «сорви варево».
 * На 70% и 40% она рассыпается на двойников — настоящая среди них, двойники лопаются от удара.
 * Каждые ~35 с ведьма варит зелье у котла: нанесите ей 8% здоровья за 6 с — варево сорвётся,
 * иначе она исцелится и обретёт силу. «Зов трясины» бьёт всех, кто стоит в воде.
 */
public class SwampHagEntity extends BossBase implements RangedAttackMob {
    /** Двойник: лопается от первого удара, без полосы здоровья и награды. */
    private boolean illusion;
    private int brew;
    private float brewStart;

    public SwampHagEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Ядовитая трясина", 200, 16, 2).first(80)
                .act(t -> {
                    LivingEntity o = randomOther();
                    Vec3 at = o.position();
                    for (int i = 0; i < 10; i++) {
                        circle(at, 3.2, 20 + i * 20, 0.12f).color(Telegraph.GREEN)
                                .then(e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1)))
                                .fx(c -> burst(ParticleTypes.ITEM_SLIME, c.add(0, 0.3, 0), 12, 1.2, 0.05));
                    }
                    tip(o, "Под вами трясина — отойдите!", ChatFormatting.RED);
                    sound(SoundEvents.SLIME_SQUISH, 1.5f, 0.5f);
                });
        skill("Зов трясины", 360, 40, 2).first(220).unstoppable()
                .begin(t -> {
                    circle(arena(), 28, 40, 0.5f).color(Telegraph.GREEN).spare(e -> !e.isInWater())
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3)))
                            .fx(c -> sound(SoundEvents.DROWNED_AMBIENT_WATER, 2f, 0.5f));
                    shoutAll("Зов трясины! Выйдите из воды — на сухое!");
                });
        skill("Проклятие жабы", 260, 20, 1).first(140)
                .act(t -> {
                    LivingEntity o = randomOther();
                    o.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 3));
                    o.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 2));
                    o.addEffect(new MobEffectInstance(MobEffects.JUMP, 160, 3));
                    burst(ParticleTypes.WITCH, o.position().add(0, 1, 0), 30, 0.6, 0.1);
                    tip(o, "Вы прокляты: ква! Слабость на 8 с.", ChatFormatting.DARK_GREEN);
                    sound(SoundEvents.FROG_AMBIENT, 2f, 0.6f);
                });
        skill("Варево", 700, 10, 2).first(500).unstoppable()
                .act(t -> startBrew());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 20; }
    @Override public int getXpReward() { return illusion ? 0 : 2000; }

    @Override
    protected float toughness() {
        return 1.4f;
    }

    @Override
    protected float[] phaseThresholds() {
        return illusion ? new float[0] : new float[]{0.7f, 0.4f};
    }

    @Override
    protected List<String> intro() {
        return illusion ? List.of() : List.of(
                "«Варево»: ведьма варит зелье 6 с. Снимите с неё 8% здоровья — варево сорвётся. Иначе исцелится и усилится.",
                "На 70% и 40% Морра рассыпается на двойников. Двойники лопаются от удара — найдите настоящую.",
                "«Зов трясины» бьёт всех, кто стоит в воде. «Трясина» — уходите из зелёных кругов.",
                "Святое и огонь бьют её сильнее, магия — слабее.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case HOLY -> 1.6f;
            case FIRE -> 1.4f;
            case MAGIC -> 0.7f;
            default -> 1f;
        };
    }

    private void startBrew() {
        Vec3 pot = null;
        BlockPos c = BlockPos.containing(arena());
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-20, -3, -20), c.offset(20, 4, 20))) {
            if (level().getBlockState(p).is(Blocks.CAULDRON) || level().getBlockState(p).is(Blocks.WATER_CAULDRON)) {
                pot = Vec3.atBottomCenterOf(p.relative(net.minecraft.core.Direction.NORTH));
                break;
            }
        }
        if (pot != null) teleportTo(pot.x, pot.y, pot.z);
        brew = 120;
        brewStart = getHealth();
        shout("Котёл кипит... ещё щепотку жабьей икры!");
        shoutAll("Морра варит зелье! Снимите 8% её здоровья за 6 с, чтобы сорвать варево!");
        sound(SoundEvents.BREWING_STAND_BREW, 2f, 0.6f);
    }

    @Override
    protected boolean canCast() {
        return super.canCast() && brew <= 0;
    }

    @Override
    protected void bossTick() {
        if (illusion) return;
        if (brew > 0) {
            brew--;
            getNavigation().stop();
            if (brew % 4 == 0) burst(ParticleTypes.BUBBLE_POP, position().add(0, 1.2, 0), 8, 0.6, 0.05);
            if (getHealth() < brewStart - getMaxHealth() * 0.08f) {
                brew = 0;
                stagger(80, "Варево сорвано! Морра оглушена.");
                burst(ParticleTypes.SMOKE, position().add(0, 1.5, 0), 30, 0.6, 0.05);
                sound(SoundEvents.GLASS_BREAK, 2f, 0.8f);
            } else if (brew == 0) {
                heal(getMaxHealth() * 0.15f);
                addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 30, 1));
                hitAll(position(), 30, 0.25f, e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)));
                shout("Хе-хе... вкусно!");
                shoutAll("Варево удалось: Морра исцелилась и усилилась, а всех окатило ядом.");
                sound(SoundEvents.WITCH_DRINK, 1.5f, 0.7f);
            }
        }
    }

    /** Рассыпаться на двойников: настоящая меняется местами с одним из них. */
    private void split() {
        if (!(level() instanceof ServerLevel sl)) return;
        List<Mob> made = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            SwampHagEntity copy = ModEntities.SWAMP_HAG.get().create(sl);
            if (copy == null) continue;
            copy.illusion = true;
            Vec3 at = randomSpot(12);
            copy.moveTo(at.x, at.y, at.z, random.nextFloat() * 360, 0);
            copy.getPersistentData().putUUID(MINION_TAG, getUUID());
            copy.setHealth(1f);
            copy.setCustomName(getName());
            if (getTarget() != null) copy.setTarget(randomOther());
            sl.addFreshEntity(copy);
            made.add(copy);
        }
        if (!made.isEmpty()) {
            Mob swap = made.get(random.nextInt(made.size()));
            Vec3 mine = position(), theirs = swap.position();
            teleportTo(theirs.x, theirs.y, theirs.z);
            swap.teleportTo(mine.x, mine.y, mine.z);
        }
        burst(ParticleTypes.WITCH, position().add(0, 1, 0), 60, 6, 0.1);
        shout("Которая из нас настоящая, а?");
        shoutAll("Морра рассыпалась на двойников! Двойники лопаются от удара.");
        sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 2f, 0.8f);
    }

    @Override
    protected void onPhase(int phase) {
        split();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (illusion && !level().isClientSide && source.getEntity() instanceof LivingEntity) {
            burst(ParticleTypes.POOF, position().add(0, 1, 0), 20, 0.4, 0.05);
            sound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1.4f);
            discard();
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        if (!illusion) super.startSeenByPlayer(player);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 35, 50, 18.0f));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20.0f));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (isCasting() || brew > 0) return;
        shoot(BossProjectile.Kind.HEX, target, illusion ? 0.12f : 0.3f, 1.2f, 2f);
        sound(SoundEvents.WITCH_THROW, 1f, 0.8f);
    }

    @Override
    protected void onReset() {
        brew = 0;
        for (Mob m : minions()) if (m instanceof SwampHagEntity) m.discard();
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect() != MobEffects.POISON && effect.getEffect() != MobEffects.MOVEMENT_SLOWDOWN && super.canBeAffected(effect);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Illusion", illusion);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        illusion = tag.getBoolean("Illusion");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITCH_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }
}
