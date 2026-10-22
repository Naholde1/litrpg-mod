package com.andrey.litrpg.entity.boss;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Имир, Ледяной великан (ур. 38). Идея: «держись у огня». На арене копится Стужа: каждые 5 с +1,
 * на 10 — заморозка и тяжёлый урон. Сбросить Стужу можно только у горящего костра. Ледяное дыхание
 * Имира гасит костры — их нужно разжигать заново (огниво, огненный заряд). Сердца стужи (узлы силы)
 * каждые 30 с одевают его в ледяной щит.
 */
public class FrostGiantEntity extends BossBase {
    private final Map<UUID, Integer> chill = new HashMap<>();
    private int blizzard;

    public FrostGiantEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        skill("Ледяное дыхание", 260, 30, 2).first(140)
                .begin(t -> {
                    Vec3 d = dirTo(t);
                    cone(position(), d, 16, 35, 30, 0.7f).color(Telegraph.BLUE)
                            .then(e -> addChill(e, 3))
                            .fx(c -> {
                                int out = douseFires(c, d, 16, 35);
                                burst(ParticleTypes.SNOWFLAKE, c.add(d.scale(6)).add(0, 1.5, 0), 120, 4, 0.1);
                                sound(SoundEvents.POWDER_SNOW_STEP, 3f, 0.4f);
                                if (out > 0) shoutAll("Дыхание Имира погасило костров: " + out + ". Разожгите их снова!");
                            });
                });
        skill("Ледяная нова", 220, 34, 2).first(80)
                .begin(t -> circle(position(), 7.5, 34, 0.6f).color(Telegraph.BLUE).then(e -> addChill(e, 2))
                        .fx(c -> {
                            burst(ParticleTypes.SNOWFLAKE, c.add(0, 1, 0), 100, 4, 0.2);
                            sound(SoundEvents.GLASS_BREAK, 2f, 0.5f);
                        }));
        skill("Ледяные шипы", 200, 24, 1).first(180)
                .begin(t -> {
                    for (LivingEntity e : fighters()) {
                        if (e == getTarget()) continue;
                        line(position(), dirTo(e), 22, 1.3, 24, 0.7f).color(Telegraph.BLUE).then(x -> addChill(x, 1))
                                .fx(c -> sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 2f, 0.5f));
                    }
                });
        skill("Сокрушение льдом", 130, 18, 1).first(50)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 35, 18, 1.0f).then(e -> BossDamage.sunder(e, 1))
                        .fx(c -> sound(SoundEvents.ANVIL_LAND, 1.5f, 0.4f)));
        skill("Глыба льда", 120, 14, 1).silent()
                .act(t -> shoot(BossProjectile.Kind.ICE, randomOther(), 0.4f, 1.1f, 2f));
        skill("Буран", 600, 40, 2).fromPhase(1).first(200)
                .act(t -> {
                    blizzard = 20 * 15;
                    shoutAll("Буран! 15 с Стужа копится вдвое быстрее — держитесь у костров.");
                    sound(SoundEvents.ELYTRA_FLYING, 2f, 0.4f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override public int getBossLevel() { return 38; }
    @Override public int getXpReward() { return 4800; }

    @Override
    protected float toughness() {
        return 2.2f;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.6f, 0.3f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Стужа: каждые 5 с +1. На 10 — вы замерзаете. Сбросить Стужу можно у ГОРЯЩЕГО костра.",
                "«Ледяное дыхание» гасит костры. Разжигайте их огнивом или огненным зарядом.",
                "Сердца стужи (ледяные узлы) каждые 30 с дают Имиру щит. Разбейте их.",
                "Огонь бьёт Имира почти вдвое, кирки колют лёд, холод его не берёт.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return switch (w) {
            case FIRE -> 1.8f;
            case PICKAXE -> 1.3f;
            case FROST -> 0f;
            default -> 1f;
        };
    }

    private void addChill(LivingEntity e, int n) {
        int v = chill.getOrDefault(e.getUUID(), 0) + n;
        if (v >= 10) {
            chill.put(e.getUUID(), 0);
            hit(e, 0.7f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));
            e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 4));
            e.setTicksFrozen(e.getTicksRequiredToFreeze() + 100);
            burst(ParticleTypes.SNOWFLAKE, e.position().add(0, 1, 0), 40, 0.5, 0.05);
            tip(e, "Вы замёрзли!", ChatFormatting.AQUA);
            return;
        }
        chill.put(e.getUUID(), v);
        if (e instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("❄ Стужа: " + v + "/10" + (v >= 7 ? " — к костру!" : ""))
                    .withStyle(v >= 7 ? ChatFormatting.RED : ChatFormatting.AQUA), true);
        }
        e.setTicksFrozen(Math.min(e.getTicksRequiredToFreeze() - 1, v * 14));
    }

    private boolean nearFire(LivingEntity e) {
        for (BlockPos p : BlockPos.betweenClosed(e.blockPosition().offset(-3, -1, -3), e.blockPosition().offset(3, 2, 3))) {
            BlockState st = level().getBlockState(p);
            if (st.getBlock() instanceof CampfireBlock && st.getValue(CampfireBlock.LIT)) return true;
        }
        return e.isOnFire();
    }

    /** Погасить костры в конусе дыхания. */
    private int douseFires(Vec3 c, Vec3 dir, double len, double half) {
        int n = 0;
        BlockPos o = BlockPos.containing(c);
        int r = (int) len;
        for (BlockPos p : BlockPos.betweenClosed(o.offset(-r, -2, -r), o.offset(r, 4, r))) {
            BlockState st = level().getBlockState(p);
            if (!(st.getBlock() instanceof CampfireBlock) || !st.getValue(CampfireBlock.LIT)) continue;
            Vec3 to = Vec3.atCenterOf(p).subtract(c);
            double d = Math.sqrt(to.x * to.x + to.z * to.z);
            if (d > len || d > 0.5 && (to.x * dir.x + to.z * dir.z) / d < Math.cos(Math.toRadians(half))) continue;
            level().setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
            n++;
        }
        return n;
    }

    @Override
    protected void bossTick() {
        if (getTarget() == null) return;
        int every = blizzard > 0 ? 50 : 100;
        if (blizzard > 0) {
            blizzard--;
            if (blizzard % 4 == 0) burst(ParticleTypes.SNOWFLAKE, arena().add(0, 4, 0), 50, 12, 0.08);
        }
        if (fightTicks % 20 == 0) {
            for (LivingEntity e : fighters()) {
                if (nearFire(e)) {
                    int v = chill.getOrDefault(e.getUUID(), 0);
                    if (v > 0) {
                        chill.put(e.getUUID(), Math.max(0, v - 2));
                        e.setTicksFrozen(0);
                        if (e instanceof ServerPlayer p) {
                            p.displayClientMessage(Component.literal("🔥 Тепло: Стужа " + Math.max(0, v - 2) + "/10").withStyle(ChatFormatting.GOLD), true);
                        }
                    }
                } else if (fightTicks % every == 0) {
                    addChill(e, 1);
                }
            }
        }
        if (fightTicks > 0 && fightTicks % 600 == 0) {
            int hearts = relicsAlive();
            if (hearts > 0) {
                setBarrier(getMaxHealth() * 0.08f * hearts);
                shoutAll("Сердца стужи одели Имира в лёд (" + hearts + "). Разбейте сердца!");
                sound(SoundEvents.GLASS_PLACE, 2f, 0.4f);
            }
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        shoutAll("Сердце стужи разбито. Осталось: " + alive);
        stagger(60, null);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Зима... приходит за всеми.");
        } else {
            shout("Лёд... крепче стали!");
            summon(EntityType.STRAY, 3, 6, 36, null);
        }
        sound(SoundEvents.POLAR_BEAR_WARNING, 3f, 0.4f);
    }

    @Override
    protected void onReset() {
        blizzard = 0;
        chill.clear();
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity le) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        return hit;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.POLAR_BEAR_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.POLAR_BEAR_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.POLAR_BEAR_DEATH; }
    @Override public float getVoicePitch() { return 0.4f; }
}
