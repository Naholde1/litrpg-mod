package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Магический снаряд со стихией: навыки игроков, посох Лича, атаки шамана и Лича.
 * Огонь и Тьма взрываются по площади (без разрушения блоков), Лёд замедляет,
 * Свет наносит двойной урон нежити.
 */
public class ManaBoltEntity extends Fireball {
    public enum Element { ARCANE, FIRE, FROST, HOLY, SHADOW, LIGHTNING, POISON, NATURE, EARTH }

    private static final EntityDataAccessor<Integer> ELEMENT =
            SynchedEntityData.defineId(ManaBoltEntity.class, EntityDataSerializers.INT);

    private float damage = 6.0f;
    private float splash = 0f;

    public ManaBoltEntity(EntityType<? extends ManaBoltEntity> type, Level level) {
        super(type, level);
    }

    public ManaBoltEntity(Level level, LivingEntity owner, double dx, double dy, double dz) {
        this(level, owner, dx, dy, dz, Element.ARCANE);
    }

    public ManaBoltEntity(Level level, LivingEntity owner, double dx, double dy, double dz, Element element) {
        super(ModEntities.MANA_BOLT.get(), owner, dx, dy, dz, level);
        setElement(element);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ELEMENT, 0);
    }

    public Element getElement() {
        int i = this.entityData.get(ELEMENT);
        Element[] v = Element.values();
        return i >= 0 && i < v.length ? v[i] : Element.ARCANE;
    }

    public void setElement(Element e) {
        this.entityData.set(ELEMENT, e.ordinal());
        setItem(switch (e) {
            case FIRE -> new ItemStack(Items.FIRE_CHARGE);
            case FROST -> new ItemStack(Items.SNOWBALL);
            case HOLY -> new ItemStack(Items.GLOWSTONE_DUST);
            case SHADOW -> new ItemStack(Items.ENDER_PEARL);
            case LIGHTNING -> new ItemStack(Items.PRISMARINE_CRYSTALS);
            case POISON -> new ItemStack(Items.SLIME_BALL);
            case NATURE -> new ItemStack(Items.SWEET_BERRIES);
            case EARTH -> new ItemStack(Items.FLINT);
            default -> new ItemStack(ModItems.MANA_ORB.get());
        });
        if (e == Element.FIRE || e == Element.SHADOW || e == Element.POISON) splash = 3.0f;
    }

    public void setDamage(float damage) { this.damage = damage; }
    public void setSplash(float radius) { this.splash = radius; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 100) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = getOwner();
        if (owner instanceof Enemy && target instanceof Enemy) return false;
        if (owner instanceof Player && target instanceof Player) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        hit(target, damage);
    }

    private void hit(Entity target, float amount) {
        Entity owner = getOwner();
        DamageSource src = damageSources().indirectMagic(this, owner);
        Element el = getElement();
        if (el == Element.HOLY && target instanceof LivingEntity le && le.getMobType() == MobType.UNDEAD) {
            amount *= 2f;
        }
        if (el == Element.FIRE) target.setSecondsOnFire(4);
        if (target.hurt(src, amount) && target instanceof LivingEntity le) {
            if (el == Element.FROST) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            if (el == Element.SHADOW) le.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            if (el == Element.POISON) le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            if (el == Element.NATURE) {
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            if (el == Element.EARTH) le.knockback(0.8, -getDeltaMovement().x, -getDeltaMovement().z);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) return;
        if (level() instanceof ServerLevel sl) {
            Element el = getElement();
            if (splash > 0) {
                Entity direct = result instanceof EntityHitResult ehr ? ehr.getEntity() : null;
                Entity owner = getOwner();
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(splash))) {
                    if (e == direct || e == owner || !canHitEntity(e)) continue;
                    hit(e, damage * 0.6f);
                }
                sl.sendParticles(el == Element.FIRE ? ParticleTypes.FLAME : el == Element.POISON ? ParticleTypes.ITEM_SLIME : ParticleTypes.SQUID_INK,
                        getX(), getY(), getZ(), 40, splash * 0.4, 0.4, splash * 0.4, 0.08);
                sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 2, 0.5, 0.5, 0.5, 0);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.4f);
            } else {
                ParticleOptions p = switch (el) {
                    case FROST -> ParticleTypes.SNOWFLAKE;
                    case HOLY -> ParticleTypes.END_ROD;
                    case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
                    case NATURE -> ParticleTypes.COMPOSTER;
                    case EARTH -> ParticleTypes.CRIT;
                    default -> ParticleTypes.WITCH;
                };
                sl.sendParticles(p, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Element", getElement().ordinal());
        tag.putFloat("Damage", damage);
        tag.putFloat("Splash", splash);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setElement(Element.values()[Math.floorMod(tag.getInt("Element"), Element.values().length)]);
        damage = tag.getFloat("Damage");
        splash = tag.getFloat("Splash");
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return switch (getElement()) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case HOLY -> ParticleTypes.END_ROD;
            case SHADOW -> ParticleTypes.SMOKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case POISON -> ParticleTypes.ITEM_SLIME;
            case NATURE -> ParticleTypes.COMPOSTER;
            case EARTH -> ParticleTypes.ASH;
            default -> ParticleTypes.ENCHANT;
        };
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
