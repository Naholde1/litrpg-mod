package com.andrey.litrpg.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

/**
 * Тотем тотемщика: вкопанный столб, а не существо.
 * <p>
 * Не ходит, не бьёт и никого не ищет — только стоит и держит ауру. Но у него есть
 * здоровье, и мобы с хранителями его бьют: сломать расстановку тотемщика можно,
 * и в этом весь смысл класса. Сам он не исчезает.
 * <p>
 * Метка {@link Enemy} нужна, чтобы мобы вообще считали столб достойной целью;
 * на игроков и их союзников он при этом не нападает — атаковать ему нечем.
 */
public class TotemEntity extends Mob implements Enemy {
    public TotemEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoAi(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    protected void registerGoals() {
        // целей нет: столб стоит
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {
        // столб не расталкивает: его ставят в выбранное место и он там остаётся
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // от падения, огня и прочей среды столб не крошится — только от ударов
        if (src.is(DamageTypes.FALL) || src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.DROWN)
                || src.is(DamageTypes.CACTUS) || src.is(DamageTypes.SWEET_BERRY_BUSH)) {
            return false;
        }
        return super.hurt(src, amount);
    }

    @Override
    public void setTarget(LivingEntity t) {
        // не отвлекается: цели у него не бывает
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }
}
