package com.andrey.litrpg.entity.wild;

import com.andrey.litrpg.entity.Animated;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.entity.wild.Species.Skill;
import com.andrey.litrpg.entity.wild.Species.Trait;
import com.andrey.litrpg.entity.wild.Species.Use;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Существо бестиария. Всё поведение — из {@link Species}: нрав (нападает сам, охраняет
 * место, защищается, убегает, добивает раненых), передвижение (земля/полёт), особые
 * свойства (стая, засада, маскировка, вампиризм, щит, отражение...) и навыки с
 * перезарядкой (прыжок, таран, дыхание, рёв, топот, язык, норы, призыв, взгляд...).
 * Модель — данными (DataModel), позы: 1 — замах/рывок, 2 — заклинание.
 */
public class WildMob extends PathfinderMob implements Animated {
    private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(WildMob.class, EntityDataSerializers.BOOLEAN);
    public static final String SUMMONER_TAG = "litrpg_summoner";
    public static final String SPLIT_TAG = "litrpg_split";

    private Species spec;
    private int[] cds;
    private int gcd = 40;
    private int poseTicks;
    private float anim, animO, awakeAnim = 1f, awakeAnimO = 1f;

    // состояния навыков
    private int chargeWind, chargeTicks;
    private Vec3 chargeDir = Vec3.ZERO;
    private int leapTicks, swoopTicks, burrowTicks, breathTicks;
    private Skill breathKind;
    private float breathPower = 1f, burstPower = 1f;
    private boolean ambush, enraged, revived;
    private int idleTicks;
    private LivingEntity held;
    private int heldTicks;
    private float heldHealth;

    public WildMob(EntityType<? extends WildMob> type, Level level) {
        super(type, level);
        xpReward = 6;
        if (spec().flying()) {
            this.moveControl = new FlyingMoveControl(this, 20, true);
            setNoGravity(true);
        }
    }

    /** Вид-враг: нападает сам (Enemy — для квестов, стражи, бонусов экипировки). */
    public static class Hostile extends WildMob implements Enemy {
        public Hostile(EntityType<? extends WildMob> type, Level level) {
            super(type, level);
        }

        @Override
        public boolean shouldDespawnInPeaceful() {
            return true;
        }
    }

    public Species spec() {
        if (spec == null) spec = Species.of(getType());
        return spec;
    }

    public static AttributeSupplier.Builder attributes(Species s) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, s.hp).add(Attributes.ATTACK_DAMAGE, Math.max(1, s.dmg))
                .add(Attributes.MOVEMENT_SPEED, s.speed).add(Attributes.ARMOR, s.armor).add(Attributes.KNOCKBACK_RESISTANCE, s.kb)
                .add(Attributes.FOLLOW_RANGE, s.hostile() ? 32 : 20).add(Attributes.FLYING_SPEED, Math.max(0.4, s.fly))
                .add(Attributes.ATTACK_KNOCKBACK, s.w > 1.3 ? 1.0 : 0.2);
    }

    // ================= синхронизация и анимации =================

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(POSE, 0);
        entityData.define(AWAKE, true);
    }

    @Override
    public int castPose() {
        return entityData.get(POSE);
    }

    @Override
    public float castAnim(float pt) {
        return Mth.lerp(pt, animO, anim);
    }

    @Override
    public float awakeAnim(float pt) {
        return Mth.lerp(pt, awakeAnimO, awakeAnim);
    }

    public boolean isDormant() {
        return !entityData.get(AWAKE);
    }

    private void pose(int p, int ticks) {
        entityData.set(POSE, p);
        poseTicks = ticks;
    }

    // ================= цели и движение =================

    @Override
    protected PathNavigation createNavigation(Level level) {
        if (spec() != null && spec().flying()) {
            FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
            nav.setCanOpenDoors(false);
            nav.setCanFloat(true);
            return nav;
        }
        return super.createNavigation(level);
    }

    @Override
    protected void registerGoals() {
        Species s = spec();
        goalSelector.addGoal(0, new FloatGoal(this));
        if (s.temper == Species.Temper.TIMID) {
            goalSelector.addGoal(1, new PanicGoal(this, 1.7));
            // крадущегося охотника (Shift) олень подпускает близко
            goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 14f, 1.2, 1.7,
                    p -> !p.isCrouching() || p.distanceTo(this) < 4));
        } else {
            if (s.has(Trait.FLEE_LOW)) {
                goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 16f, 1.2, 1.5, p -> getHealth() < getMaxHealth() * 0.25f));
            }
            if (s.has(Trait.KITE)) goalSelector.addGoal(2, new KiteGoal(this));
            else goalSelector.addGoal(2, new MeleeAttackGoal(this, s.flying() ? 1.0 : 1.15, true));
        }
        if (s.flying()) goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        else if (s.has(Trait.WATER)) goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        else goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        if (s.temper != Species.Temper.TIMID) targetSelector.addGoal(1, new HurtByTargetGoal(this));
        if (s.hostile()) {
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, !s.has(Trait.BLIND), false, this::wantsToHunt));
        }
    }

    /** Кого вид готов атаковать сам (без повода). */
    private boolean wantsToHunt(LivingEntity e) {
        Species s = spec();
        if (isDormant()) return false;
        if (s.has(Trait.BLIND) && e instanceof Player p) {
            // слепые слышат только шум: крадущегося не замечают, стоящего — только вплотную
            boolean moving = p.walkDist != p.walkDistO || p.isSprinting();
            if (p.isCrouching() || !moving && distanceTo(p) > 2.5) return false;
        }
        return switch (s.temper) {
            case TERRITORIAL -> distanceTo(e) < s.zone;
            case SCAVENGER -> e.getHealth() < e.getMaxHealth() * 0.6f;
            default -> true;
        };
    }

    /** Держать дистанцию и стрелять навыками; в упор — бить. */
    static class KiteGoal extends Goal {
        private final WildMob mob;
        private int hitCd;

        KiteGoal(WildMob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = mob.getTarget();
            return t != null && t.isAlive();
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = mob.getTarget();
            if (t == null) return;
            mob.getLookControl().setLookAt(t, 30f, 30f);
            double d = mob.distanceTo(t);
            if (d < 6) {
                if (mob.getNavigation().isDone() || mob.tickCount % 10 == 0) {
                    Vec3 away = DefaultRandomPos.getPosAway(mob, 12, 5, t.position());
                    if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, 1.3);
                }
            } else if (d > 14 || !mob.hasLineOfSight(t)) {
                if (mob.tickCount % 10 == 0) mob.getNavigation().moveTo(t, 1.0);
            } else if (mob.tickCount % 20 == 0) {
                mob.getNavigation().stop();
            }
            if (--hitCd <= 0 && d < 2.4) {
                mob.swing(InteractionHand.MAIN_HAND);
                mob.doHurtTarget(t);
                hitCd = 20;
            }
        }
    }

    // ================= появление =================

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        // притворяются только «дикие» (не призванные заданием или яйцом)
        if (spec().has(Trait.DISGUISE) && (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION
                || reason == MobSpawnType.STRUCTURE || reason == MobSpawnType.COMMAND)) entityData.set(AWAKE, false);
        return super.finalizeSpawn(level, diff, reason, data, tag);
    }

    // ================= тик =================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            animO = anim;
            anim = Mth.approach(anim, castPose() > 0 ? 1f : 0f, 0.2f);
            awakeAnimO = awakeAnim;
            awakeAnim = Mth.approach(awakeAnim, isDormant() ? 0f : 1f, 0.15f);
            clientParticles();
        }
    }

    private void clientParticles() {
        if (isInvisible() || isDormant()) return;
        String p = spec().particle;
        if (p.isEmpty() || random.nextInt(3) != 0) return;
        ParticleOptions o = switch (p) {
            case "flame" -> ParticleTypes.FLAME;
            case "soul" -> ParticleTypes.SOUL_FIRE_FLAME;
            case "slime" -> ParticleTypes.ITEM_SLIME;
            case "lava" -> ParticleTypes.LAVA;
            case "portal" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.SMOKE;
        };
        level().addParticle(o, getRandomX(0.6), getRandomY(), getRandomZ(0.6), 0, 0.02, 0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        Species s = spec();
        if (s.has(Trait.SUN_BURN) && isAlive() && level().isDay()) {
            float f = getLightLevelDependentMagicValue();
            BlockPos bp = BlockPos.containing(getX(), getEyeY(), getZ());
            if (f > 0.5f && random.nextFloat() * 30f < (f - 0.4f) * 2f && level().canSeeSky(bp) && !isInWaterRainOrBubble()) setSecondsOnFire(8);
        }
        if (isDormant()) {
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            Player p = level().getNearestPlayer(this, s.temper == Species.Temper.TERRITORIAL ? s.zone * 0.6 : 3.5);
            if (p != null && !p.isCreative() && !p.isSpectator()) wake(p);
        }
    }

    private void wake(LivingEntity who) {
        if (!isDormant()) return;
        entityData.set(AWAKE, true);
        playSound(spec().sounds[0] != null ? sound(spec().sounds[0]) : SoundEvents.ZOMBIE_AMBIENT, 1.2f, getVoicePitch());
        if (who != null && !(who instanceof Player pl && (pl.isCreative() || pl.isSpectator()))) setTarget(who);
        idleTicks = 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        Species s = spec();
        if (cds == null) {
            cds = new int[s.skills.size()];
            for (int i = 0; i < cds.length; i++) cds[i] = 20 + random.nextInt(Math.max(1, s.skills.get(i).cd() / 2));
        }
        if (poseTicks > 0 && --poseTicks == 0) entityData.set(POSE, 0);
        for (int i = 0; i < cds.length; i++) if (cds[i] > 0) cds[i]--;
        if (gcd > 0) gcd--;
        passives(s);
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || t instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            setTarget(null);
            t = null;
        }
        stealth(s, t);
        if (t == null) {
            if (s.has(Trait.DISGUISE) && !isDormant() && ++idleTicks > 600) entityData.set(AWAKE, false);   // снова притворяется
            return;
        }
        idleTicks = 0;
        if (activeSkill(t)) return;
        if (gcd > 0) return;
        double dist = distanceTo(t);
        for (int i = 0; i < cds.length; i++) {
            if (cds[i] > 0) continue;
            Use u = s.skills.get(i);
            if (start(u, t, dist)) {
                cds[i] = u.cd();
                gcd = 30;
                break;
            }
        }
    }

    // ================= пассивные свойства =================

    private void passives(Species s) {
        if (tickCount % 20 == 0) {
            if (s.has(Trait.AURA_FIRE) || s.has(Trait.AURA_FROST)) {
                for (LivingEntity e : foes(3.5)) {
                    if (s.has(Trait.AURA_FIRE)) e.setSecondsOnFire(3);
                    else {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
                        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 60, e.getTicksFrozen() + 50));
                    }
                }
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(s.has(Trait.AURA_FIRE) ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 12, 1.6, 0.2, 1.6, 0.01);
                }
            }
        }
        if (s.has(Trait.REGEN) && tickCount % 40 == 0 && getHealth() < getMaxHealth() && tickCount - getLastHurtByMobTimestamp() > 80) {
            heal(getMaxHealth() * 0.02f);
        }
        if (s.has(Trait.ENRAGE) && !enraged && getHealth() < getMaxHealth() * 0.35f) {
            enraged = true;
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, MobEffectInstance.INFINITE_DURATION, 0, false, true));
            playSound(SoundEvents.RAVAGER_ROAR, 1.2f, getVoicePitch());
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getEyeY(), getZ(), 8, 0.6, 0.4, 0.6, 0);
        }
        // затянутая в трясинника жертва
        if (held != null) {
            if (!held.isAlive() || --heldTicks <= 0 || heldHealth - getHealth() > getMaxHealth() * 0.15f || held.distanceTo(this) > 6) {
                held = null;
            } else {
                Vec3 to = position().add(getLookAngle().scale(0.6)).subtract(held.position());
                held.setDeltaMovement(to.scale(0.35));
                held.hurtMarked = true;
                if (heldTicks % 20 == 0) {
                    held.hurt(damageSources().mobAttack(this), dmg() * 0.3f);
                    held.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0));
                }
            }
        }
    }

    /** Невидимка: прячется, пока цель далеко; первый удар из засады — двойной. */
    private void stealth(Species s, LivingEntity t) {
        if (!s.has(Trait.STEALTH) && burrowTicks <= 0) return;
        if (burrowTicks > 0) return;
        boolean hide = t == null ? tickCount - getLastHurtByMobTimestamp() > 100 : distanceTo(t) > 6 && tickCount - getLastHurtByMobTimestamp() > 60;
        if (hide != isInvisible()) {
            setInvisible(hide);
            if (!hide) {
                ambush = true;
                playSound(SoundEvents.PHANTOM_SWOOP, 0.8f, 1.4f);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            }
        }
    }

    // ================= навыки =================

    /** Продолжающиеся навыки (рывок, прыжок, пике, нора, дыхание). @return true — навык ещё идёт */
    private boolean activeSkill(LivingEntity t) {
        if (chargeWind > 0) {
            getNavigation().stop();
            getLookControl().setLookAt(t, 30f, 30f);
            if (level() instanceof ServerLevel sl && chargeWind % 3 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 4, 0.3, 0.05, 0.3, 0.05);
            }
            if (--chargeWind == 0) {
                Vec3 d = t.position().subtract(position());
                chargeDir = new Vec3(d.x, 0, d.z).normalize();
                chargeTicks = 26;
                playSound(SoundEvents.RAVAGER_ROAR, 0.8f, getVoicePitch() * 1.2f);
            }
            return true;
        }
        if (chargeTicks > 0) {
            chargeTicks--;
            getNavigation().stop();
            setYRot((float) (Mth.atan2(chargeDir.z, chargeDir.x) * Mth.RAD_TO_DEG) - 90f);
            yBodyRot = getYRot();
            setDeltaMovement(chargeDir.x * 0.8, getDeltaMovement().y, chargeDir.z * 0.8);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.5), this::isFoe)) {
                e.hurt(damageSources().mobAttack(this), dmg() * burstPower);
                e.knockback(2.0, -chargeDir.x, -chargeDir.z);
                e.hurtMarked = true;
                chargeTicks = 0;
                playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
            }
            if (horizontalCollision && chargeTicks > 0) {
                chargeTicks = 0;
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));   // оглушён ударом о стену
                playSound(SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 0.6f);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CRIT, getX(), getEyeY(), getZ(), 12, 0.4, 0.3, 0.4, 0.1);
            }
            if (chargeTicks == 0) entityData.set(POSE, 0);
            return true;
        }
        if (leapTicks > 0) {
            leapTicks--;
            if (distanceTo(t) < 1.8 + getBbWidth() / 2) {
                leapTicks = 0;
                swing(InteractionHand.MAIN_HAND);
                doHurtTarget(t);
            }
            return leapTicks > 0;
        }
        if (swoopTicks > 0) {
            swoopTicks--;
            Vec3 d = t.getEyePosition().subtract(position()).normalize();
            setDeltaMovement(d.scale(0.9));
            if (distanceTo(t) < 1.6 + getBbWidth() / 2) {
                swoopTicks = 0;
                t.hurt(damageSources().mobAttack(this), dmg() * burstPower);
                applyHit(t);
                setDeltaMovement(getDeltaMovement().x * -0.4, 0.7, getDeltaMovement().z * -0.4);
            }
            return swoopTicks > 0;
        }
        if (burrowTicks > 0) {
            burrowTicks--;
            getNavigation().moveTo(t, 1.7);
            if (level() instanceof ServerLevel sl) {
                BlockState on = getBlockStateOn().isAir() ? Blocks.SAND.defaultBlockState() : getBlockStateOn();
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, on), getX(), getY() + 0.1, getZ(), 8, 0.6, 0.05, 0.6, 0.1);
                if (burrowTicks % 8 == 0) playSound(on.getSoundType().getBreakSound(), 0.8f, 0.6f);
            }
            if (distanceTo(t) < 2.4 || burrowTicks == 0) {
                burrowTicks = 0;
                setInvisible(false);
                pose(1, 20);
                for (LivingEntity e : foes(3.0)) {
                    e.hurt(damageSources().mobAttack(this), dmg() * burstPower);
                    e.setDeltaMovement(e.getDeltaMovement().x, 1.0, e.getDeltaMovement().z);
                    e.hurtMarked = true;
                }
                playSound(SoundEvents.RAVAGER_ROAR, 1.3f, 0.7f);
                if (level() instanceof ServerLevel sl) {
                    BlockState on = getBlockStateOn().isAir() ? Blocks.SAND.defaultBlockState() : getBlockStateOn();
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, on), getX(), getY() + 0.5, getZ(), 60, 1.2, 0.8, 1.2, 0.2);
                }
            }
            return burrowTicks > 0;
        }
        if (breathTicks > 0) {
            breathTicks--;
            getNavigation().stop();
            getLookControl().setLookAt(t, 30f, 30f);
            breath(t);
            if (breathTicks == 0) entityData.set(POSE, 0);
            return true;
        }
        return false;
    }

    /** Начать навык, если условия подходят. */
    private boolean start(Use u, LivingEntity t, double dist) {
        float r = u.range();
        boolean sees = hasLineOfSight(t);
        switch (u.skill()) {
            case BOLT -> {
                if (dist > r || dist < 2.5 || !sees) return false;
                pose(2, 12);
                shoot(kind(u.arg()), t, dmg() * u.power(), 1.2f, 0);
                return true;
            }
            case VOLLEY -> {
                if (dist > r || dist < 2.5 || !sees) return false;
                pose(2, 14);
                int n = Math.max(1, u.count());
                for (int i = 0; i < n; i++) shoot(kind(u.arg()), t, dmg() * u.power(), 1.2f, (i - (n - 1) / 2f) * 9f);
                return true;
            }
            case ARROWS -> {
                if (dist > r || dist < 3 || !sees) return false;
                pose(2, 12);
                int n = Math.max(1, u.count());
                for (int i = 0; i < n; i++) arrow(t, dmg() * u.power(), n > 1 ? 7f : 1.5f);
                playSound(SoundEvents.SKELETON_SHOOT, 1f, 0.8f + random.nextFloat() * 0.3f);
                return true;
            }
            case LEAP -> {
                if (dist > r || dist < 3 || !onGround() || !sees) return false;
                Vec3 d = t.position().subtract(position());
                Vec3 h = new Vec3(d.x, 0, d.z).normalize().scale(Math.min(1.3, dist * 0.16));
                setDeltaMovement(h.x, 0.45 + Math.min(0.3, d.y * 0.1), h.z);
                leapTicks = 20;
                burstPower = u.power();
                pose(1, 14);
                playSound(SoundEvents.RAVAGER_STEP, 0.8f, 1.4f);
                return true;
            }
            case CHARGE -> {
                if (dist > r || dist < 4 || !onGround() || !sees) return false;
                chargeWind = 14;
                burstPower = u.power();
                pose(1, 40);
                return true;
            }
            case SWOOP -> {
                if (dist > r || dist < 3 || !sees) return false;
                swoopTicks = 24;
                burstPower = u.power();
                pose(1, 20);
                playSound(SoundEvents.PHANTOM_SWOOP, 1.2f, getVoicePitch());
                return true;
            }
            case BREATH_FIRE, BREATH_FROST, BREATH_POISON -> {
                if (dist > r || !sees) return false;
                breathTicks = 30;
                breathKind = u.skill();
                breathPower = u.power();
                pose(1, 30);
                playSound(u.skill() == Skill.BREATH_FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.LLAMA_SPIT, 1.2f, 0.6f);
                return true;
            }
            case BURROW -> {
                if (dist > r || dist < 5) return false;
                burrowTicks = 100;
                burstPower = u.power();
                setInvisible(true);
                playSound(SoundEvents.SAND_BREAK, 1.5f, 0.5f);
                return true;
            }
            case ROAR -> {
                if (dist > r) return false;
                pose(2, 16);
                for (LivingEntity e : foes(r)) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                }
                for (WildMob a : kin(16)) a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0));
                playSound(SoundEvents.RAVAGER_ROAR, 1.5f, getVoicePitch());
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getEyeY(), getZ(), 1, 0, 0, 0, 0);
                return true;
            }
            case HOWL -> {
                List<WildMob> pack = kin(u.range());
                pose(2, 30);
                for (WildMob a : pack) {
                    if (a.getTarget() == null) a.setTarget(t);
                    a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                }
                addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
                playSound(spec().temper == Species.Temper.SCAVENGER ? SoundEvents.FOX_SCREECH : SoundEvents.WOLF_HOWL, 2f, getVoicePitch());
                return true;
            }
            case STOMP -> {
                if (dist > r || !onGround()) return false;
                pose(1, 14);
                for (LivingEntity e : foes(r)) {
                    e.hurt(damageSources().mobAttack(this), dmg() * u.power());
                    Vec3 away = e.position().subtract(position()).normalize();
                    e.setDeltaMovement(away.x * 0.8, 0.6, away.z * 0.8);
                    e.hurtMarked = true;
                }
                playSound(SoundEvents.GENERIC_EXPLODE, 0.7f, 0.6f);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, getBlockStateOn()), getX(), getY() + 0.1, getZ(), 50, r * 0.5, 0.1, r * 0.5, 0.15);
                    sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 2, 1, 0, 1, 0);
                }
                return true;
            }
            case TONGUE -> {
                if (dist > r || dist < 3 || !sees) return false;
                pose(1, 10);
                Vec3 pull = position().subtract(t.position()).normalize().scale(1.2);
                t.setDeltaMovement(pull.x, 0.35, pull.z);
                t.hurtMarked = true;
                t.hurt(damageSources().mobAttack(this), dmg() * u.power());
                playSound(SoundEvents.FROG_TONGUE, 1.5f, 0.6f);
                if (level() instanceof ServerLevel sl) {
                    Vec3 a = getEyePosition(), b = t.getEyePosition();
                    for (int i = 0; i <= 10; i++) {
                        Vec3 p = a.lerp(b, i / 10.0);
                        sl.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    }
                }
                return true;
            }
            case SUMMON -> {
                if (dist > 24) return false;
                return summon(u, t);
            }
            case GAZE -> {
                return gaze(u);
            }
            case SCREAM -> {
                // Вопль плакальщицы раньше бил мгновенно и сквозь стены — ответить было нечем.
                // Теперь это замах на 1 с: нужна линия взгляда, крик слышно заранее,
                // его можно прервать оглушением или просто уйти за угол.
                if (dist > r || !sees) return false;
                pose(2, 20);
                playSound(SoundEvents.GHAST_WARN, 1.4f, 1.2f);
                if (level() instanceof ServerLevel warn) {
                    warn.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 12, 0.6, 0.4, 0.6, 0.01);
                }
                for (LivingEntity e : foes(r)) {
                    if (e instanceof ServerPlayer sp && hasLineOfSight(sp)) {
                        sp.displayClientMessage(Component.literal(getName().getString() + " набирает воздух — в укрытие!")
                                .withStyle(ChatFormatting.GOLD), true);
                    }
                }
                com.andrey.litrpg.util.Scheduler.later(20, () -> {
                    if (!isAlive() || com.andrey.litrpg.ability.SkillEffects.stunned(this)) return;
                    if (!(level() instanceof ServerLevel sl)) return;
                    for (LivingEntity e : foes(r)) {
                        if (!hasLineOfSight(e)) continue;
                        e.hurt(damageSources().magic(), dmg() * u.power());
                        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
                        e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
                    }
                    playSound(SoundEvents.GHAST_SCREAM, 1.6f, 1.3f);
                    sl.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getEyeY(), getZ(), 20, 1.5, 0.6, 1.5, 0.02);
                });
                return true;
            }
            case BLINK -> {
                if (dist > r || dist < 4) return false;
                Vec3 look = t.getLookAngle();
                Vec3 to = t.position().subtract(new Vec3(look.x, 0, look.z).normalize().scale(2.2));
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 30, 0.4, 0.8, 0.4, 0.2);
                if (!randomTeleport(to.x, t.getY(), to.z, true)) return false;
                playSound(SoundEvents.ENDERMAN_TELEPORT, 1f, 0.7f);
                ambush = true;
                return true;
            }
            case ROOTS -> {
                if (dist > r || !t.onGround()) return false;
                pose(2, 18);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
                t.addEffect(new MobEffectInstance(MobEffects.JUMP, 50, 128));
                t.hurt(damageSources().mobAttack(this), dmg() * u.power());
                playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5f, 0.6f);
                if (level() instanceof ServerLevel sl) {
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), t.getX(), t.getY() + 0.2, t.getZ(),
                            30, 0.5, 0.2, 0.5, 0.05);
                    if (t instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Корни держат ноги!").withStyle(ChatFormatting.DARK_GREEN), true);
                }
                return true;
            }
        }
        return false;
    }

    private void breath(LivingEntity t) {
        Vec3 eye = getEyePosition();
        Vec3 dir = t.getEyePosition().subtract(eye).normalize();
        ParticleOptions part = breathKind == Skill.BREATH_FIRE ? ParticleTypes.FLAME : breathKind == Skill.BREATH_FROST ? ParticleTypes.SNOWFLAKE : ParticleTypes.ITEM_SLIME;
        if (level() instanceof ServerLevel sl) {
            for (int i = 1; i < 7; i++) {
                Vec3 p = eye.add(dir.scale(i));
                sl.sendParticles(part, p.x, p.y, p.z, 3, 0.15 * i, 0.12 * i, 0.15 * i, 0.02);
            }
        }
        if (breathTicks % 5 != 0) return;
        for (LivingEntity e : foes(7)) {
            Vec3 to = e.getEyePosition().subtract(eye);
            if (to.normalize().dot(dir) < 0.8 || !hasLineOfSight(e)) continue;
            e.hurt(breathKind == Skill.BREATH_FIRE ? damageSources().onFire() : damageSources().mobAttack(this), dmg() * breathPower * 0.3f);
            switch (breathKind) {
                case BREATH_FIRE -> e.setSecondsOnFire(4);
                case BREATH_FROST -> {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() + 80, e.getTicksFrozen() + 60));
                }
                default -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
            }
        }
    }

    private boolean summon(Use u, LivingEntity t) {
        EntityType<?> type = EntityType.byString(u.arg()).orElse(null);
        if (type == null || !(level() instanceof ServerLevel sl)) return false;
        String me = getUUID().toString();
        int have = level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(24),
                m -> m.isAlive() && me.equals(m.getPersistentData().getString(SUMMONER_TAG))).size();
        if (have >= (int) u.range()) return false;
        pose(2, 30);
        int n = Math.min(u.count(), (int) u.range() - have);
        for (int i = 0; i < n; i++) {
            if (!(type.create(sl) instanceof Mob m)) continue;
            double a = random.nextDouble() * Math.PI * 2;
            m.moveTo(getX() + Math.cos(a) * 2, getY(), getZ() + Math.sin(a) * 2, random.nextFloat() * 360f, 0);
            m.getPersistentData().putString(SUMMONER_TAG, me);
            MobLevels.apply(m, Math.max(1, MobLevels.getLevel(this) - 4), MobLevels.Rank.NORMAL);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            m.setTarget(t);
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.5, m.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.2f, 0.8f);
        return true;
    }

    /** Взгляд василиска: кто смотрит ему в глаза — каменеет (3 взгляда подряд — оцепенение). */
    private boolean gaze(Use u) {
        boolean any = false;
        for (LivingEntity e : foes(u.range())) {
            if (!(e instanceof ServerPlayer p) || !hasLineOfSight(p)) continue;
            Vec3 to = getEyePosition().subtract(p.getEyePosition()).normalize();
            int stacks = p.getPersistentData().getInt("litrpg_gaze");
            long last = p.getPersistentData().getLong("litrpg_gaze_t");
            if (level().getGameTime() - last > 200) stacks = 0;
            if (p.getViewVector(1f).dot(to) > 0.9) {
                stacks++;
                any = true;
                p.getPersistentData().putLong("litrpg_gaze_t", level().getGameTime());
                if (stacks >= 3) {
                    stacks = 0;
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                    p.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 128));
                    p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
                    p.hurt(damageSources().magic(), dmg() * u.power());
                    p.displayClientMessage(Component.literal("Вы окаменели под взглядом василиска!").withStyle(ChatFormatting.GRAY), true);
                    playSound(SoundEvents.STONE_PLACE, 1.5f, 0.5f);
                } else {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, stacks));
                    p.displayClientMessage(Component.literal("Взгляд василиска! Отведите глаза (" + stacks + "/3)").withStyle(ChatFormatting.YELLOW), true);
                }
                p.getPersistentData().putInt("litrpg_gaze", stacks);
            }
        }
        if (any) pose(2, 16);
        return any;
    }

    private static BossProjectile.Kind kind(String arg) {
        return switch (arg) {
            case "web" -> BossProjectile.Kind.WEB;
            case "poison" -> BossProjectile.Kind.POISON;
            case "fire" -> BossProjectile.Kind.FIRE;
            case "ice" -> BossProjectile.Kind.ICE;
            case "shadow" -> BossProjectile.Kind.SHADOW;
            case "meteor" -> BossProjectile.Kind.METEOR;
            case "bomb" -> BossProjectile.Kind.BOMB;
            case "crystal" -> BossProjectile.Kind.CRYSTAL;
            case "quill" -> BossProjectile.Kind.QUILL;
            case "hex" -> BossProjectile.Kind.HEX;
            default -> BossProjectile.Kind.ROCK;
        };
    }

    private void shoot(BossProjectile.Kind kind, LivingEntity t, float damage, float speed, float yawOffset) {
        BossProjectile p = new BossProjectile(level(), this, kind, damage);
        p.setPos(getX(), getEyeY() - 0.2, getZ());
        double dx = t.getX() - getX(), dy = t.getY(0.5) - p.getY(), dz = t.getZ() - getZ();
        if (yawOffset != 0) {
            double a = Math.toRadians(yawOffset), c = Math.cos(a), s = Math.sin(a);
            double nx = dx * c - dz * s, nz = dx * s + dz * c;
            dx = nx;
            dz = nz;
        }
        if (kind.gravity > 0) dy += Math.sqrt(dx * dx + dz * dz) * kind.gravity * 2.2;
        p.shoot(dx, dy, dz, speed, 2f);
        level().addFreshEntity(p);
        if (yawOffset == 0) playSound(kind == BossProjectile.Kind.FIRE || kind == BossProjectile.Kind.METEOR ? SoundEvents.BLAZE_SHOOT
                : kind == BossProjectile.Kind.ROCK || kind == BossProjectile.Kind.BOMB ? SoundEvents.SNOWBALL_THROW : SoundEvents.LLAMA_SPIT, 1f, 0.8f);
    }

    private void arrow(LivingEntity t, float damage, float spread) {
        Arrow a = new Arrow(level(), this);
        a.setBaseDamage(damage / 1.6);
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        double dy = t.getY(0.33) - a.getY();
        a.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.18, dz, 1.6f, spread);
        level().addFreshEntity(a);
    }

    // ================= бой =================

    private float dmg() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private boolean isFoe(LivingEntity e) {
        if (e == this || !e.isAlive() || e instanceof WildMob w && w.getType() == getType()) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e == getTarget() || e instanceof Mob m && m.getTarget() == this;
    }

    private List<LivingEntity> foes(double r) {
        return level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> isFoe(e) && e.distanceTo(this) <= r + getBbWidth() / 2);
    }

    private List<WildMob> kin(double r) {
        return level().getEntitiesOfClass(WildMob.class, getBoundingBox().inflate(r), m -> m != this && m.getType() == getType() && m.isAlive());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float mult = 1f;
        if (leapTicks > 0 || swoopTicks > 0) mult *= burstPower;
        if (ambush) {
            mult *= 2f;
            ambush = false;
        }
        AttributeBoost boost = mult != 1f ? new AttributeBoost(this, mult) : null;
        boolean hit = super.doHurtTarget(target);
        if (boost != null) boost.restore();
        if (hit && target instanceof LivingEntity le) {
            applyHit(le);
            Species s = spec();
            if (s.has(Trait.DRAIN)) {
                heal(dmg() * mult * 0.4f);
                if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, le.getX(), le.getY(0.6), le.getZ(), 4, 0.3, 0.3, 0.3, 0.1);
            }
            if (s.has(Trait.ENGULF) && held == null) {
                held = le;
                heldTicks = 80;
                heldHealth = getHealth();
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
                if (le instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal("Трясина затягивает! Бейте, чтобы вырваться").withStyle(ChatFormatting.DARK_GREEN), true);
            }
        }
        return hit;
    }

    /** Временный множитель урона для одного удара (прыжок, засада). */
    private static final class AttributeBoost {
        private static final UUID ID = UUID.fromString("5b1c7a0e-2a51-4c33-9f0e-0c7f1b6a7d11");
        private final WildMob mob;

        AttributeBoost(WildMob mob, float mult) {
            this.mob = mob;
            var inst = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (inst != null) inst.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ID, "litrpg_burst", mult - 1,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_TOTAL));
        }

        void restore() {
            var inst = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (inst != null) inst.removeModifier(ID);
        }
    }

    private void applyHit(LivingEntity le) {
        Species s = spec();
        for (Species.Hit h : s.hits) {
            MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(h.effect());
            if (eff != null) le.addEffect(new MobEffectInstance(eff, h.duration(), h.amp()), this);
        }
        if (s.fire > 0) le.setSecondsOnFire(s.fire);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        Species s = spec();
        if (burrowTicks > 0 && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;   // под землёй не достать
        if (s.has(Trait.REFLECT) && source.getDirectEntity() instanceof Projectile && random.nextFloat() < 0.8f) {
            playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5f, 1.2f);
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.6), getZ(), 8, 0.4, 0.4, 0.4, 0.05);
            return false;
        }
        if (s.has(Trait.FRONT_GUARD) && source.getEntity() != null && source.getSourcePosition() != null) {
            Vec3 to = source.getSourcePosition().subtract(position()).normalize();
            Vec3 look = getLookAngle();
            if (to.x * look.x + to.z * look.z > 0.5) {
                amount *= 0.5f;
                playSound(SoundEvents.SHIELD_BLOCK, 1f, 0.7f);
            }
        }
        if (s.has(Trait.FLAMMABLE) && source.is(DamageTypeTags.IS_FIRE)) amount *= 2f;
        boolean r = super.hurt(source, amount);
        if (r) {
            if (isDormant()) wake(source.getEntity() instanceof LivingEntity le ? le : null);
            if (isInvisible() && burrowTicks <= 0) setInvisible(false);
            if (source.getEntity() instanceof LivingEntity attacker && s.has(Trait.PACK)) {
                for (WildMob a : kin(20)) if (a.getTarget() == null) a.setTarget(attacker);
            }
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        Species s = spec();
        if (!level().isClientSide && s.has(Trait.REVIVE) && !revived && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)) {
            // встаёт ещё раз: половина здоровья, мгновение неуязвимости
            revived = true;
            setHealth(getMaxHealth() * 0.5f);
            invulnerableTime = 60;
            pose(2, 40);
            playSound(SoundEvents.ZOMBIE_VILLAGER_CURE, 1.5f, 0.5f);
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 40, 0.8, 1.2, 0.8, 0.05);
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
                    p.displayClientMessage(Component.literal(s.ru + " собирает себя заново!").withStyle(ChatFormatting.DARK_AQUA), true);
                }
            }
            return;
        }
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            if (s.has(Trait.EXPLODE)) sl.explode(this, getX(), getY() + 0.5, getZ(), 2.5f, Level.ExplosionInteraction.NONE);
            if (s.has(Trait.SPLIT) && !getPersistentData().getBoolean(SPLIT_TAG)) {
                for (int i = 0; i < 2; i++) {
                    if (!(getType().create(sl) instanceof WildMob m)) continue;
                    m.moveTo(getX() + random.nextGaussian() * 0.4, getY() + 0.2, getZ() + random.nextGaussian() * 0.4, random.nextFloat() * 360f, 0);
                    m.getPersistentData().putBoolean(SPLIT_TAG, true);
                    MobLevels.apply(m, Math.max(1, MobLevels.getLevel(this) - 2), MobLevels.Rank.NORMAL);
                    m.setHealth(m.getMaxHealth() * 0.5f);
                    if (source.getEntity() instanceof LivingEntity le) m.setTarget(le);
                    sl.addFreshEntity(m);
                }
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (getPersistentData().getBoolean(SPLIT_TAG) && random.nextBoolean()) return;
        for (Species.Drop d : spec().drops) {
            if (random.nextFloat() > d.chance() + looting * 0.05f) continue;
            var item = ForgeRegistries.ITEMS.getValue(d.item());
            if (item == null) continue;
            int n = d.min() + random.nextInt(Math.max(1, d.max() - d.min() + 1)) + (looting > 0 ? random.nextInt(looting + 1) : 0);
            if (n > 0) spawnAtLocation(new ItemStack(item, n));
        }
    }

    // ================= свойства вида =================

    @Override
    public MobType getMobType() {
        return switch (spec().mobType) {
            case "undead" -> MobType.UNDEAD;
            case "arthropod" -> MobType.ARTHROPOD;
            default -> MobType.UNDEFINED;
        };
    }

    @Override
    public boolean fireImmune() {
        return spec().has(Trait.FIRE_IMMUNE) || super.fireImmune();
    }

    @Override
    public boolean canBreatheUnderwater() {
        return spec().has(Trait.WATER) || spec().mobType.equals("undead");
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource source) {
        return !spec().flying() && super.causeFallDamage(dist, mult, source);
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
        if (!spec().flying()) super.checkFallDamage(y, onGround, state, pos);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return !isPersistenceRequired();
    }

    @Override
    public int getAmbientSoundInterval() {
        return spec().hostile() ? 160 : 240;
    }

    private static SoundEvent sound(net.minecraft.resources.ResourceLocation id) {
        return id == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(id);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isDormant() || isInvisible() ? null : sound(spec().sounds[0]);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return sound(spec().sounds[1]);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound(spec().sounds[2]);
    }

    @Override
    public float getVoicePitch() {
        return spec().pitch * (0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    protected float getSoundVolume() {
        return spec().w > 1.5f ? 1.4f : 1f;
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL && this instanceof Enemy) {
            discard();
            return;
        }
        super.checkDespawn();
    }

    // ================= сохранение =================

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormant", isDormant());
        tag.putBoolean("Enraged", enraged);
        tag.putBoolean("Revived", revived);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKE, !tag.getBoolean("Dormant"));
        enraged = tag.getBoolean("Enraged");
        revived = tag.getBoolean("Revived");
    }
}
