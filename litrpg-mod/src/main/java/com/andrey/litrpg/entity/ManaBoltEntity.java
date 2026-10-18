package com.andrey.litrpg.entity;

import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Магический снаряд: навык игрока, посох Лича, атаки шамана и Лича. */
public class ManaBoltEntity extends Fireball {
    private float damage = 6.0f;

    public ManaBoltEntity(EntityType<? extends ManaBoltEntity> type, Level level) {
        super(type, level);
    }

    public ManaBoltEntity(Level level, LivingEntity owner, double dx, double dy, double dz) {
        super(ModEntities.MANA_BOLT.get(), owner, dx, dy, dz, level);
        setItem(new ItemStack(ModItems.MANA_ORB.get()));
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

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
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = getOwner();
        DamageSource src = damageSources().indirectMagic(this, owner);
        target.hurt(src, damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.WITCH, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.1);
            }
            discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.ENCHANT;
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
