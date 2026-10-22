package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Гхаз, Глубинный Червь (ур. 33). Идеи: «он под тобой» и «вырви товарища из пасти».
 * Каждые ~35 с уходит под землю (неуязвим) и трижды вырывается под игроками — бегите из-под кругов.
 * «Поглощение»: глотает союзника — тот умирает, если группа за 5 с не снимет с червя 5% здоровья.
 */
public class GhazEntity extends BossBase {
    private int burrow;
    private UUID swallowed;
    private int swallowTicks;
    private float swallowStart;

    public GhazEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Удар хвостом", 110, 16, 1).first(50)
                .when(t -> distanceTo(t) < 7)
                .begin(t -> cone(position(), dirTo(t), 7, 40, 16, 1.0f).then(e -> knockFrom(e, position(), 1.6, 0.5))
                        .fx(c -> sound(SoundEvents.RAVAGER_ATTACK, 2f, 0.5f)));
        skill("Кислотная струя", 220, 24, 1).first(100)
                .begin(t -> {
                    LivingEntity o = randomOther();
                    cone(position(), dirTo(o), 15, 25, 24, 0.55f).color(Telegraph.GREEN)
                            .then(e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2)))
                            .fx(c -> burst(ParticleTypes.ITEM_SLIME, c.add(dirTo(o).scale(6)).add(0, 1, 0), 60, 3, 0.1));
                });
        skill("Обвал сводов", 300, 20, 2).first(180)
                .act(t -> {
                    var rock = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());
                    for (int i = 0; i < 7; i++) circle(randomSpot(16), 2.8, 50, 0.55f).color(Telegraph.ORANGE)
                            .fx(c -> burst(rock, c.add(0, 2, 0), 40, 1.2, 0.1));
                    sound(SoundEvents.DEEPSLATE_BREAK, 2f, 0.4f);
                });
        skill("Поглощение", 480, 20, 1).first(300)
                .act(t -> {
                    LivingEntity o = randomOther();
                    if (o == null || o == getTarget() && fighters().size() > 1) return;
                    swallowed = o.getUUID();
                    swallowTicks = 100;
                    swallowStart = getHealth();
                    o.teleportTo(getX(), getY() + 1, getZ());
                    shout("ХРУМ!");
                    shoutAll(o.getName().getString() + " проглочен! Снимите с Гхаза 5% здоровья за 5 с, иначе он погибнет!");
                    sound(SoundEvents.GENERIC_EAT, 2f, 0.4f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 33; }
    @Override public int getXpReward() { return 4000; }

    @Override
    protected float toughness() {
        return 2.3f;
    }

    @Override
    protected List<String> intro() {
        return List.of("Гхаз уходит под землю (неуязвим) и вырывается под игроками трижды — круги ползут за вами, уходите в последний миг.",
                "«Поглощение» — червь глотает союзника. Снимите 5% его здоровья за 5 с, чтобы он выплюнул беднягу.",
                "Не стойте перед пастью: «Кислотная струя». Танк — держи хвост подальше от группы.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case SWORD, AXE -> 1.2f;
            case BLUNT -> 0.8f;
            default -> 1f;
        };
    }

    @Override
    protected boolean canCast() {
        return super.canCast() && swallowed == null;
    }

    @Override
    protected void bossTick() {
        if (getTarget() != null && fightTicks > 0 && fightTicks % 700 == 0 && burrow <= 0) dive();
        if (burrow > 0) {
            burrow--;
            getNavigation().stop();
            setDeltaMovement(Vec3.ZERO);
            if (burrow % 60 == 0 && burrow > 0) {
                LivingEntity o = randomOther();
                if (o != null) {
                    circle(o.position(), 3.5, 45, 0.9f).color(Telegraph.RED).follow(o, 15)
                            .then(e -> e.setDeltaMovement(e.getDeltaMovement().add(0, 1.0, 0)))
                            .fx(c -> {
                                teleportTo(c.x, c.y, c.z);
                                burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), c.add(0, 1, 0), 80, 1.5, 0.2);
                                sound(SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
                            });
                    tip(o, "Земля дрожит под вами — бегите!", ChatFormatting.RED);
                }
            }
            if (burrow == 0) surface();
        }
        if (swallowed != null && level() instanceof ServerLevel sl) {
            Entity e = sl.getEntity(swallowed);
            swallowTicks--;
            if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                swallowed = null;
            } else if (getHealth() < swallowStart - getMaxHealth() * 0.05f) {
                spit(le);
                stagger(60, "Гхаз выплюнул " + le.getName().getString() + " и оглушён!");
            } else if (swallowTicks <= 0) {
                hit(le, 3f);
                spit(le);
            } else {
                le.teleportTo(getX(), getY() + 1, getZ());
                le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                if (swallowTicks % 20 == 0) hit(le, 0.12f);
            }
        }
    }

    private void spit(LivingEntity le) {
        swallowed = null;
        le.teleportTo(getX() + getLookAngle().x * 4, getY() + 1, getZ() + getLookAngle().z * 4);
        le.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
        sound(SoundEvents.PLAYER_BURP, 2f, 0.4f);
    }

    private void dive() {
        burrow = 200;
        immune = "Гхаз под землёй!";
        setInvisible(true);
        burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState()), position().add(0, 1, 0), 100, 2, 0.2);
        sound(SoundEvents.ROOTED_DIRT_BREAK, 3f, 0.4f);
        shoutAll("Гхаз ушёл под землю! Следите за кругами под ногами.");
    }

    private void surface() {
        immune = null;
        setInvisible(false);
        Vec3 c = arena();
        teleportTo(c.x, c.y, c.z);
        circle(c, 6, 1, 0.5f).fx(x -> burst(ParticleTypes.EXPLOSION, x.add(0, 1, 0), 6, 2, 0));
    }

    @Override
    protected void onPhase(int phase) {
        shout("ГЛУБИНА... ГОЛОДНА...");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
    }

    @Override
    protected void onReset() {
        if (burrow > 0) {
            burrow = 0;
            surface();
        }
        swallowed = null;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }
    @Override public float getVoicePitch() { return 0.4f; }
}
