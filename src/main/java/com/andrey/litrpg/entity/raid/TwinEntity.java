package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Близнецы Сол и Луна (Шпиль Бездны, ур. 58). Идеи: «разведи» и «убей вместе».
 * Стоят ближе 10 блоков — Узы близнецов лечат обоих: двум танкам надо растащить их по краям.
 * Павший близнец не умирает: если второй не падёт за 15 с, первый встанет с половиной здоровья.
 * Сол — свет и огонь, Луна — холод и тьма.
 */
public class TwinEntity extends BossBase {
    private UUID partner;
    /** Пал и ждёт брата (тики). */
    private int fallen;

    public TwinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        boolean sol = isSol();
        if (sol) {
            skill("Солнечный удар", 110, 16, 1).first(40)
                    .when(t -> distanceTo(t) < 6)
                    .begin(t -> cone(position(), dirTo(t), 6, 40, 16, 1.0f).color(Telegraph.GOLD)
                            .then(e -> {
                                e.setSecondsOnFire(5);
                                BossDamage.sunder(e, 1);
                            }));
            skill("Луч рассвета", 200, 24, 1).first(100)
                    .begin(t -> {
                        LivingEntity o = randomOther();
                        line(position(), dirTo(o), 26, 1.4, 24, 0.85f).color(Telegraph.GOLD).then(e -> e.setSecondsOnFire(6))
                                .fx(c -> burst(ParticleTypes.END_ROD, c.add(dirTo(o).scale(10)).add(0, 1, 0), 60, 6, 0.05));
                    });
            skill("Вспышка", 260, 30, 2).first(160)
                    .begin(t -> circle(position(), 8, 30, 0.6f).color(Telegraph.GOLD)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0)))
                            .fx(c -> burst(ParticleTypes.FLASH, c.add(0, 2, 0), 2, 0.5, 0)));
            skill("Солнечное пламя", 420, 20, 2).first(260)
                    .act(t -> {
                        circle(randomSpot(12), 3.2, 90, 1.8f).color(Telegraph.GOLD).soak(2).then(e -> e.setSecondsOnFire(5))
                                .fx(c -> burst(ParticleTypes.EXPLOSION_EMITTER, c.add(0, 0.5, 0), 1, 0, 0));
                        shoutAll("Солнечное пламя — двое и больше в золотой круг!");
                    });
        } else {
            skill("Лунный серп", 110, 16, 1).first(40)
                    .when(t -> distanceTo(t) < 6)
                    .begin(t -> cone(position(), dirTo(t), 6, 40, 16, 1.0f).color(Telegraph.BLUE)
                            .then(e -> {
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
                                BossDamage.sunder(e, 1);
                            }));
            skill("Ледяные осколки", 90, 12, 1).silent()
                    .act(t -> {
                        for (int i = 0; i < 3; i++) shoot(BossProjectile.Kind.ICE, randomOther(), 0.3f, 1.3f, 4f);
                        sound(SoundEvents.GLASS_BREAK, 1.2f, 1.4f);
                    });
            skill("Звездопад", 280, 20, 2).first(140)
                    .act(t -> {
                        for (int i = 0; i < 8; i++) circle(randomSpot(16), 2.6, 40 + i * 6, 0.5f).color(Telegraph.BLUE)
                                .fx(c -> burst(ParticleTypes.END_ROD, c.add(0, 1, 0), 20, 1, 0.1));
                        sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 2f, 0.6f);
                    });
            skill("Затмение", 400, 30, 2).first(220)
                    .act(t -> {
                        hitAll(arena(), 30, 0.32f, e -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0)));
                        sound(SoundEvents.WARDEN_EMERGE, 2f, 0.8f);
                    });
        }
    }

    public boolean isSol() {
        return getType() == ModEntities.SOL_TWIN.get();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 58; }
    @Override public int getXpReward() { return 9000; }

    @Override
    protected float toughness() {
        return 3.0f;
    }

    @Override
    protected List<String> intro() {
        return !isSol() ? List.of() : List.of("Близнецов двое: Сол (свет) и Луна (тьма). Нужны два танка.",
                "Ближе 10 блоков друг к другу — Узы близнецов лечат обоих. Растащите их по краям арены.",
                "Павший близнец встанет через 15 с с половиной здоровья, если брат ещё жив. Добивайте их вместе!",
                "Святое бьёт Луну сильнее, холод — Сола; огонь Солу нипочём, как и холод Луне.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        if (isSol()) return w == BossDamage.Weapon.FROST ? 1.5f : w == BossDamage.Weapon.FIRE ? 0.2f : 1f;
        return w == BossDamage.Weapon.HOLY ? 1.5f : w == BossDamage.Weapon.FROST ? 0.2f : 1f;
    }

    private TwinEntity partner() {
        if (partner == null || !(level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(partner);
        return e instanceof TwinEntity t && t.isAlive() ? t : null;
    }

    @Override
    protected void onEngage() {
        if (!isSol() || partner() != null || !(level() instanceof ServerLevel sl)) return;
        TwinEntity luna = ModEntities.LUNA_TWIN.get().create(sl);
        if (luna == null) return;
        Vec3 at = randomSpot(8);
        luna.moveTo(at.x, at.y, at.z, 0, 0);
        luna.finalizeSpawn(sl, sl.getCurrentDifficultyAt(luna.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        luna.partner = getUUID();
        luna.partyScale = partyScale;
        if (home != null) luna.restrictTo(home, 24);
        luna.setTarget(randomOther());
        sl.addFreshEntity(luna);
        partner = luna.getUUID();
        burst(ParticleTypes.END_ROD, at.add(0, 1, 0), 60, 1, 0.1);
        shout("Сестра, проснись. Гости пришли.");
    }

    @Override
    protected boolean onLethal(DamageSource src) {
        TwinEntity p = partner();
        if (p == null || p.fallen > 0) return false;
        fallen = 300;
        setHealth(1f);
        immune = "Близнец пал и ждёт брата — добейте второго за 15 с!";
        shoutAll(getName().getString() + " пал! Добейте второго близнеца за 15 с, иначе он встанет.");
        burst(ParticleTypes.SOUL, position().add(0, 1, 0), 40, 1, 0.05);
        return true;
    }

    @Override
    protected boolean canCast() {
        return super.canCast() && fallen <= 0;
    }

    @Override
    protected void bossTick() {
        TwinEntity p = partner();
        if (fallen > 0) {
            fallen--;
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (p == null) {
                // брат пал следом — вместе уходят и они
                fallen = 0;
                immune = null;
                hurt(damageSources().genericKill(), Float.MAX_VALUE);
                return;
            }
            if (fallen == 0) {
                immune = null;
                setHealth(getMaxHealth() * 0.5f);
                shout("Я снова с тобой!");
                shoutAll(getName().getString() + " восстал — близнецов надо добивать вместе!");
            }
            return;
        }
        if (p != null && p.fallen <= 0 && tickCount % 20 == 0 && distanceTo(p) < 10) {
            heal(getMaxHealth() * 0.012f);
            Vec3 a = position().add(0, 2, 0), b = p.position().add(0, 2, 0);
            for (int i = 0; i <= 10; i++) burst(isSol() ? ParticleTypes.END_ROD : ParticleTypes.SOUL_FIRE_FLAME, a.lerp(b, i / 10.0), 1, 0, 0);
            if (isSol() && tickCount % 100 == 0) shoutAll("Узы близнецов лечат обоих — растащите их дальше 10 блоков!");
        }
    }

    @Override
    protected void onReset() {
        fallen = 0;
        TwinEntity p = partner();
        if (isSol() && p != null) {
            p.discard();
            partner = null;
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && isSol() && reason == RemovalReason.KILLED) {
            TwinEntity p = partner();
            if (p != null && p.isAlive()) p.hurt(damageSources().genericKill(), Float.MAX_VALUE);
        }
        super.remove(reason);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof TwinEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (partner != null) tag.putUUID("Partner", partner);
        tag.putInt("Fallen", fallen);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        partner = tag.hasUUID("Partner") ? tag.getUUID("Partner") : null;
        fallen = tag.getInt("Fallen");
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return isSol() ? SoundEvents.BLAZE_AMBIENT : SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BEACON_DEACTIVATE; }
    @Override public float getVoicePitch() { return isSol() ? 0.9f : 1.3f; }

    static boolean killing(DamageSource s) {
        return s.is(DamageTypes.GENERIC_KILL);
    }
}
