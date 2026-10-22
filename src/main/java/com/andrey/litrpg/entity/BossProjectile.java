package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Снаряды боссов: валун, паутина, яд, проклятие, лёд, огонь, тень, метеор. */
public class BossProjectile extends ThrowableItemProjectile {
    public enum Kind {
        ROCK(0.04f, 2.0),
        WEB(0.015f, 0),
        POISON(0.0f, 0),
        HEX(0.0f, 0),
        ICE(0.01f, 0),
        FIRE(0.0f, 1.5),
        SHADOW(0.0f, 0),
        METEOR(0.06f, 3.5),
        BOMB(0.05f, 2.5),
        CRYSTAL(0.0f, 0),
        QUILL(0.0f, 0);

        public final float gravity;
        /** Радиус удара по области при попадании (0 — только в цель). */
        public final double splash;

        Kind(float gravity, double splash) {
            this.gravity = gravity;
            this.splash = splash;
        }

        Item item() {
            return switch (this) {
                case ROCK -> Items.COBBLESTONE;
                case WEB -> Items.COBWEB;
                case POISON -> Items.SLIME_BALL;
                case HEX -> Items.FERMENTED_SPIDER_EYE;
                case ICE -> Items.BLUE_ICE;
                case FIRE -> Items.FIRE_CHARGE;
                case SHADOW -> Items.ECHO_SHARD;
                case METEOR -> Items.MAGMA_BLOCK;
                case BOMB -> Items.TNT;
                case CRYSTAL -> Items.AMETHYST_SHARD;
                case QUILL -> Items.ARROW;
            };
        }

        ParticleOptions trail() {
            return switch (this) {
                case ROCK -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState());
                case WEB -> ParticleTypes.WHITE_ASH;
                case POISON -> ParticleTypes.ITEM_SLIME;
                case HEX -> ParticleTypes.WITCH;
                case ICE -> ParticleTypes.SNOWFLAKE;
                case FIRE -> ParticleTypes.FLAME;
                case SHADOW -> ParticleTypes.SCULK_SOUL;
                case METEOR -> ParticleTypes.LAVA;
                case BOMB -> ParticleTypes.SMOKE;
                case CRYSTAL -> ParticleTypes.END_ROD;
                case QUILL -> ParticleTypes.CRIT;
            };
        }
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(BossProjectile.class, EntityDataSerializers.INT);
    private float damage = 6f;
    private int life = 0;

    public BossProjectile(EntityType<? extends BossProjectile> type, Level level) {
        super(type, level);
    }

    public BossProjectile(Level level, LivingEntity owner, Kind kind, float damage) {
        super(ModEntities.BOSS_PROJECTILE.get(), owner, level);
        this.damage = damage;
        setKind(kind);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(KIND, 0);
    }

    public Kind kind() {
        int k = entityData.get(KIND);
        return k >= 0 && k < Kind.values().length ? Kind.values()[k] : Kind.ROCK;
    }

    public void setKind(Kind k) {
        entityData.set(KIND, k.ordinal());
        setItem(new ItemStack(k.item()));
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBBLESTONE;
    }

    @Override
    protected float getGravity() {
        return kind().gravity;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Kind k = kind();
            for (int i = 0; i < 2; i++) {
                level().addParticle(k.trail(), getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.1,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0, 0);
            }
            if (k == Kind.METEOR || k == Kind.FIRE) level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
        } else if (++life > 200) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        Entity e = hit.getEntity();
        if (isFriend(e)) return;
        if (e instanceof LivingEntity le && kind().splash <= 0) {
            le.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), damage);
            effect(le);
        }
    }

    private boolean isFriend(Entity e) {
        Entity owner = getOwner();
        if (e == owner) return true;
        if (owner instanceof com.andrey.litrpg.entity.wild.WildMob && e instanceof com.andrey.litrpg.entity.wild.WildMob) return true;
        return owner instanceof BossBase b && (b.isMinion(e) || e instanceof BossBase);
    }

    @Override
    protected boolean canHitEntity(Entity e) {
        return super.canHitEntity(e) && !isFriend(e);
    }

    private void effect(LivingEntity le) {
        switch (kind()) {
            case ROCK -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            case WEB -> {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 4));
                le.addEffect(new MobEffectInstance(MobEffects.JUMP, 70, 128)); // нельзя выпрыгнуть
            }
            case POISON -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 1));
            case HEX -> {
                le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 140, 1));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            case ICE -> {
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                le.setTicksFrozen(Math.min(le.getTicksRequiredToFreeze() + 120, le.getTicksFrozen() + 160));
            }
            case FIRE, METEOR -> le.setSecondsOnFire(5);
            case BOMB -> le.setDeltaMovement(le.getDeltaMovement().add(0, 0.4, 0));
            case CRYSTAL -> le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            case QUILL -> le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
            case SHADOW -> {
                le.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
                le.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(level() instanceof ServerLevel sl)) return;
        Kind k = kind();
        Vec3 p = position();
        if (k.splash > 0) {
            LivingEntity owner = getOwner() instanceof LivingEntity o ? o : null;
            for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(k.splash),
                    e -> !isFriend(e) && e.position().distanceTo(p) <= k.splash + 0.5)) {
                le.hurt(damageSources().mobProjectile(this, owner), damage);
                effect(le);
            }
            sl.sendParticles(k == Kind.ROCK ? k.trail() : k == Kind.BOMB ? ParticleTypes.LARGE_SMOKE : ParticleTypes.LAVA, p.x, p.y, p.z, 24, k.splash * 0.4, 0.3, k.splash * 0.4, 0.1);
            sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y + 0.3, p.z, k == Kind.METEOR || k == Kind.BOMB ? 3 : 1, 0.5, 0.2, 0.5, 0);
        }
        sl.sendParticles(k.trail(), p.x, p.y, p.z, 14, 0.3, 0.3, 0.3, 0.08);
        SoundEvent s = switch (k) {
            case ROCK -> SoundEvents.STONE_BREAK;
            case WEB -> SoundEvents.SPIDER_STEP;
            case POISON -> SoundEvents.SLIME_SQUISH;
            case HEX -> SoundEvents.WITCH_THROW;
            case ICE -> SoundEvents.GLASS_BREAK;
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case SHADOW -> SoundEvents.SCULK_SHRIEKER_SHRIEK;
            case METEOR, BOMB -> SoundEvents.GENERIC_EXPLODE;
            case CRYSTAL -> SoundEvents.AMETHYST_CLUSTER_BREAK;
            case QUILL -> SoundEvents.ARROW_HIT;
        };
        sl.playSound(null, p.x, p.y, p.z, s, SoundSource.HOSTILE, k == Kind.SHADOW ? 0.4f : 1f, 0.9f + random.nextFloat() * 0.2f);
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", damage);
        tag.putInt("Kind", kind().ordinal());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getFloat("Damage");
        entityData.set(KIND, tag.getInt("Kind"));
    }
}
