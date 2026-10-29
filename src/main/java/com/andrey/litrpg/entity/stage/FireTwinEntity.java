package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Огненные близнецы (этап 4, ур. 37): Игнис — пламя-клинок, Синдер — пепел-чародейка.
 * <ul>
 *   <li>«Огненная нить» — между близнецами горит нить: кто её пересекает, горит;</li>
 *   <li>«Слияние пламени» — если близнецы 3 с стоят ближе 7 блоков, оба лечатся и навсегда становятся сильнее (+15% урона);
 *       танкам — держать их порознь;</li>
 *   <li>Игнис: «Огненный рывок» (полоса, огонь на полу), «Удар пламени» (конус), «Вспышка» (круг вокруг себя);</li>
 *   <li>Синдер: «Пепельный дождь» (круги под всеми, угли остаются), «Огненный шар», «Стена огня» через всю арену,
 *       «Зов брата» — Игнис прыгает к ней (снова разводите);</li>
 *   <li>павший первым тлеет, а выживший впитывает его пламя: +45% урона и жгучая аура. Убивайте почти разом.</li>
 * </ul>
 */
public class FireTwinEntity extends StageBoss {
    private final boolean ignis;
    private UUID partner;
    private int close;
    private int fury;
    private boolean smolder;
    private Vec3 dashTo;

    public FireTwinEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, type == ModEntities.BOSSES.get("fire_twin_cinder").get() ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.RED);
        this.ignis = type != ModEntities.BOSSES.get("fire_twin_cinder").get();
        if (ignis) {
            skill("Удар пламени", 100, 14, 1).first(30)
                    .when(t -> distanceTo(t) < 7)
                    .begin(t -> cone(position(), dirTo(t), 7, 50, 14, 1.0f).color(Telegraph.ORANGE).then(e -> e.setSecondsOnFire(3)))
                    .act(t -> fxSlash(dirTo(t), 6, 110, -20, 0xE0FF6020));
            skill("Огненный рывок", 300, 22, 1).first(110)
                    .begin(t -> {
                        LivingEntity v = randomOther();
                        Vec3 d = dirTo(v == null ? t : v);
                        dashTo = safeSpot(position().add(d.scale(20)));
                        line(position(), d, dashTo.subtract(position()).horizontalDistance() + 2, 1.6, 22, 0.9f)
                                .color(Telegraph.ORANGE);
                    })
                    .act(t -> {
                        if (dashTo == null) return;
                        Vec3 from = position();
                        Vec3 d = dashTo.subtract(from);
                        for (int k = 3; k <= 18; k += 3) hazard(from.add(d.scale(k / 20.0)), 1.6, 20 * 8, 0.08f, 0xC0FF6010, e -> e.setSecondsOnFire(2));
                        fxBeam(from.add(0, 1, 0), dashTo.add(0, 1, 0), 1.2, 10, 0xE0FF8030);
                        teleportTo(dashTo.x, dashTo.y, dashTo.z);
                        sound(SoundEvents.BLAZE_SHOOT, 2f, 0.6f);
                    });
            skill("Вспышка", 360, 30, 2).first(180)
                    .begin(t -> circle(position(), 7, 30, 1.2f).color(Telegraph.ORANGE))
                    .act(t -> {
                        fxRing(position(), 0.5, 7, 12, 0xF0FFA040);
                        sound(SoundEvents.GENERIC_EXPLODE, 2f, 0.8f);
                    });
        } else {
            skill("Огненный шар", 120, 16, 1).first(40)
                    .act(t -> {
                        shoot(BossProjectile.Kind.FIRE, t, 0.6f, 1.2f, 1f);
                        sound(SoundEvents.GHAST_SHOOT, 1.5f, 1.2f);
                    });
            skill("Пепельный дождь", 280, 28, 2).first(90)
                    .begin(t -> {
                        for (ServerPlayer p : players()) circle(p.position(), 2.6, 28, 0.6f).color(Telegraph.ORANGE)
                                .fx(c -> hazard(c, 2.2, 20 * 12, 0.08f, 0xA0603020, null));
                        shoutAll("Пепельный дождь! Отбегите — угли останутся на полу.");
                    })
                    .act(t -> sound(SoundEvents.FIRECHARGE_USE, 2f, 0.5f));
            skill("Стена огня", 420, 40, 2).first(200).unstoppable()
                    .begin(t -> {
                        Vec3 c = arena();
                        double a = random.nextDouble() * Math.PI;
                        Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
                        line(c.subtract(d.scale(20)), d, 40, 2.5, 40, 1.2f).color(Telegraph.RED);
                        shoutAll("Стена огня делит арену — уходите с полосы!");
                    })
                    .act(t -> sound(SoundEvents.BLAZE_AMBIENT, 3f, 0.4f));
            skill("Зов брата", 900, 20, 2).first(500)
                    .when(t -> sibling() != null && !sibling().smolder)
                    .act(t -> {
                        FireTwinEntity s = sibling();
                        if (s == null) return;
                        Vec3 at = safeSpot(position().add(dirTo(t).scale(-2)));
                        fxBeam(s.position().add(0, 1, 0), at.add(0, 1, 0), 0.8, 12, 0xE0FFC040);
                        s.teleportTo(at.x, at.y, at.z);
                        shoutAll("Синдер зовёт брата — Игнис рядом с ней! Разведите их!");
                    });
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 44.0);
    }

    /** Волны зовёт только одна из пары, иначе их было бы две разом. */
    @Override
    protected int waveTheme() {
        return ignis ? super.waveTheme() : 0;
    }

    @Override public int getBossLevel() { return 37; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 40000; }
    @Override public boolean fireImmune() { return true; }

    @Override
    protected List<String> intro() {
        if (!ignis) return List.of();
        return List.of("Два танка: держите Игниса и Синдер дальше 7 блоков друг от друга — рядом они сливаются и крепнут навсегда.",
                "Между ними горит нить — не пересекайте её.",
                "Павший первым тлеет, а второй впитывает его пламя (+45% урона). Сбивайте здоровье ровно и добивайте почти разом.");
    }

    // ---------------- пара ----------------

    private FireTwinEntity sibling() {
        if (partner == null || !(level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(partner);
        return e instanceof FireTwinEntity t && t.isAlive() ? t : null;
    }

    @Override
    protected void onArena() {
        if (!ignis || partner != null || !(level() instanceof ServerLevel sl)) return;
        com.andrey.litrpg.util.Scheduler.later(2, () -> {
            if (partner != null || !isAlive()) return;
            FireTwinEntity c = (FireTwinEntity) ModEntities.BOSSES.get("fire_twin_cinder").get().create(sl);
            if (c == null) return;
            Vec3 n = point("north");
            c.moveTo(n.x, n.y, n.z, 0, 0);
            c.setArena(arenaBox, net.minecraft.core.BlockPos.containing(n), arenaExtra);
            c.scaleForParty(partySize);
            c.partner = getUUID();
            partner = c.getUUID();
            sl.addFreshEntity(c);
        });
    }

    @Override
    protected void bossTick() {
        FireTwinEntity sib = sibling();
        if (sib != null && getTarget() == null && sib.getTarget() != null) setTarget(sib.getTarget());
        if (smolder) {
            getNavigation().stop();
            if (tickCount % 10 == 0) burst(ParticleTypes.LARGE_SMOKE, position().add(0, 1, 0), 4, 0.4, 0.02);
            return;
        }
        if (fightTicks <= 0) return;
        if (sib != null && !sib.smolder && ignis) {
            // огненная нить
            if (tickCount % 10 == 0) {
                Fx.tether(level(), this, sib, 0.25, 12, 0xD0FF7020);
                Vec3 a = position().add(0, 1, 0), b = sib.position().add(0, 1, 0);
                for (ServerPlayer p : players()) {
                    if (distToSegment(p.position().add(0, 1, 0), a, b) < 1.3) {
                        hit(p, 0.18f);
                        p.setSecondsOnFire(2);
                        tip(p, "Вы в огненной нити!", ChatFormatting.GOLD);
                    }
                }
            }
            // слияние
            if (distanceTo(sib) < 7) {
                if (++close == 60) {
                    close = 0;
                    fury++;
                    sib.fury++;
                    heal(getMaxHealth() * 0.05f);
                    sib.heal(sib.getMaxHealth() * 0.05f);
                    fxPillar(position(), 2.5, 7, 20, 0xE0FFB040);
                    fxPillar(sib.position(), 2.5, 7, 20, 0xE0FFB040);
                    shoutAll("Слияние пламени! Близнецы сильнее (×" + String.format("%.2f", 1 + 0.15f * fury) + "). Разведите их!");
                    sound(SoundEvents.BLAZE_AMBIENT, 3f, 0.5f);
                }
                if (close % 20 == 10) for (ServerPlayer p : players()) p.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Близнецы рядом — разведите их!").withStyle(ChatFormatting.RED), true);
            } else close = 0;
        }
        // впитанное пламя павшего — жгучая аура
        if (fury >= 3 && (sib == null || sib.smolder) && tickCount % 20 == 0) {
            for (ServerPlayer p : players()) if (p.distanceTo(this) < 9) hit(p, 0.08f);
            fxRing(position(), 0.5, 9, 10, 0x90FF5010);
        }
    }

    private static double distToSegment(Vec3 p, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = ab.lengthSqr() < 1e-6 ? 0 : Math.max(0, Math.min(1, p.subtract(a).dot(ab) / ab.lengthSqr()));
        return p.distanceTo(a.add(ab.scale(t)));
    }

    @Override
    protected float powerMult() {
        return smolder ? 0f : 1f + 0.15f * fury;
    }

    @Override
    protected boolean onLethal(DamageSource src) {
        FireTwinEntity sib = sibling();
        if (sib != null && !sib.smolder) {
            smolder = true;
            immune = "Тлеет — добейте второго близнеца!";
            setHealth(1f);
            sib.fury += 3;
            sib.heal(sib.getMaxHealth() * 0.1f);
            fxPillar(position(), 1.2, 5, 20, 0xD0404040);
            fxBeam(position().add(0, 1, 0), sib.position().add(0, 1, 0), 1.0, 20, 0xE0FF8020);
            shoutAll(getName().getString() + " тлеет! " + sib.getName().getString() + " впитывает его пламя — урон +45%!");
            return true;
        }
        if (sib != null && sib.smolder) {
            sib.smolder = false;
            sib.immune = null;
            if (sib.ignis) com.andrey.litrpg.util.Scheduler.later(1, () -> sib.kill());
            else sib.discard();
        }
        return false;
    }

    @Override
    protected boolean canCast() {
        return super.canCast() && !smolder;
    }

    @Override
    protected void onReset() {
        super.onReset();
        smolder = false;
        immune = null;
        fury = 0;
        close = 0;
        FireTwinEntity sib = sibling();
        if (sib != null && ignis && (sib.getHealth() < sib.getMaxHealth() || sib.smolder)) sib.resetFightPublic();
    }

    void resetFightPublic() {
        resetFight();
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FROST ? 1.3f : w == BossDamage.Weapon.FIRE ? 0.4f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(ignis ? 1.15 : 0.9);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (partner != null) tag.putUUID("Partner", partner);
        tag.putInt("Fury", fury);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Partner")) partner = tag.getUUID("Partner");
        fury = tag.getInt("Fury");
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BLAZE_DEATH; }
    @Override public float getVoicePitch() { return ignis ? 0.7f : 1.2f; }
}
