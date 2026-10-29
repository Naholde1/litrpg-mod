package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Страж приливов (этап 3, ур. 24). Зал с колоннами и четырьмя помостами по углам.
 * <ul>
 *   <li>«Трезубец» — конус по танку с расколом брони; «Гарпун» — притягивает дальнего бойца к себе;</li>
 *   <li>«Волна» — вал идёт через весь зал от одной из стен: спасает только укрытие (колонна, помост) между вами и стеной;</li>
 *   <li>«Водоворот» — 3 с тянет всех к Стражу, затем взрыв вокруг него — бегите против течения;</li>
 *   <li>с 70%: утопленники-прислужники; с 40%: «Затопление» — зал заливает на 15 с, в воде — урон и замедление,
 *       спасают только помосты по углам (и они тесные).</li>
 * </ul>
 */
public class TideWardenEntity extends StageBoss {
    private int floodTicks;
    private int floodTimer;

    public TideWardenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        skill("Трезубец", 100, 16, 1).first(30)
                .when(t -> distanceTo(t) < 8)
                .begin(t -> cone(position(), dirTo(t), 8, 35, 16, 1.0f).color(Telegraph.BLUE).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 7, 70, 0, 0xE060C0FF);
                    sound(SoundEvents.TRIDENT_THROW, 2f, 0.6f);
                });
        skill("Гарпун", 280, 14, 1).first(100)
                .act(t -> {
                    LivingEntity v = farthest();
                    if (v == null) return;
                    Fx.tether(level(), this, v, 0.2, 16, 0xE060C0FF);
                    pullTo(v, position(), 2.2);
                    hit(v, 0.35f);
                    tip(v, "Гарпун Стража тянет вас к нему!", ChatFormatting.AQUA);
                    sound(SoundEvents.TRIDENT_RIPTIDE_1, 2f, 0.8f);
                });
        skill("Волна", 360, 40, 2).first(140).unstoppable()
                .begin(t -> wave())
                .act(t -> sound(SoundEvents.GENERIC_SPLASH, 3f, 0.4f));
        skill("Водоворот", 440, 60, 2).first(260)
                .begin(t -> {
                    circle(position(), 7, 60, 1.2f).color(Telegraph.BLUE);
                    shoutAll("Водоворот тянет к Стражу — бегите прочь против течения!");
                })
                .during(left -> {
                    if (left % 2 != 0) return;
                    for (ServerPlayer p : players()) if (p.distanceTo(this) < 22) pullTo(p, position(), 0.09);
                    if (left % 6 == 0) fxRing(position(), 12, 3, 8, 0xA040A0FF);
                })
                .act(t -> {
                    fxRing(position(), 0.5, 8, 12, 0xF0A0E0FF);
                    burst(ParticleTypes.SPLASH, position().add(0, 1, 0), 80, 3, 0.4);
                });
        skill("Пенный залп", 220, 20, 1).first(80)
                .act(t -> {
                    for (ServerPlayer p : players()) shoot(BossProjectile.Kind.ICE, p, 0.4f, 1.2f, 2f);
                    sound(SoundEvents.DROWNED_SHOOT, 2f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 44.0);
    }

    @Override public int getBossLevel() { return 24; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 40000; }
    @Override public boolean canBreatheUnderwater() { return true; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.7f, 0.4f};
    }

    @Override
    protected List<String> intro() {
        return List.of("«Волна» идёт от стены через весь зал — встаньте так, чтобы колонна или помост закрыли вас от этой стены.",
                "«Водоворот» тянет к Стражу — бегите от него до взрыва.",
                "С 40% зал затопит — тогда все на помосты по углам, в воде вы тонете.");
    }

    private LivingEntity farthest() {
        LivingEntity best = null;
        double d = 0;
        for (ServerPlayer p : players()) {
            double dd = p.distanceTo(this);
            if (dd > d) {
                d = dd;
                best = p;
            }
        }
        return best;
    }

    // ---------------- волна ----------------

    private void wave() {
        var b = arenaBox;
        if (b == null) return;
        int side = random.nextInt(4);
        Vec3 c = arena();
        double y = c.y;
        Vec3 from, dir;
        double len, half;
        switch (side) {
            case 0 -> { from = new Vec3(c.x, y, b.z1() + 1); dir = new Vec3(0, 0, 1); len = b.z2() - b.z1(); half = (b.x2() - b.x1()) / 2.0 + 1; }
            case 1 -> { from = new Vec3(c.x, y, b.z2()); dir = new Vec3(0, 0, -1); len = b.z2() - b.z1(); half = (b.x2() - b.x1()) / 2.0 + 1; }
            case 2 -> { from = new Vec3(b.x1() + 1, y, c.z); dir = new Vec3(1, 0, 0); len = b.x2() - b.x1(); half = (b.z2() - b.z1()) / 2.0 + 1; }
            default -> { from = new Vec3(b.x2(), y, c.z); dir = new Vec3(-1, 0, 0); len = b.x2() - b.x1(); half = (b.z2() - b.z1()) / 2.0 + 1; }
        }
        final Vec3 f = from, d = dir;
        line(from, dir, len, half, 40, 1.1f).color(Telegraph.BLUE)
                .spare(e -> sheltered(e, f, d))
                .then(e -> knockFrom(e, e.position().subtract(d), 1.2, 0.4));
        String[] names = {"с севера", "с юга", "с запада", "с востока"};
        shoutAll("Волна идёт " + names[side] + "! Укройтесь за колонной или помостом.");
    }

    /** Между игроком и стеной, откуда идёт волна, есть преграда. */
    private boolean sheltered(LivingEntity e, Vec3 from, Vec3 dir) {
        Vec3 eye = e.position().add(0, 1.0, 0);
        double back = (eye.x - from.x) * dir.x + (eye.z - from.z) * dir.z;
        Vec3 wall = eye.subtract(dir.scale(Math.max(1, back)));
        var hit = level().clip(new ClipContext(eye, wall, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(eye) < back - 1;
    }

    // ---------------- затопление ----------------

    private void flood() {
        if (!arenaExtra.has("flood")) return;
        var a = arenaExtra.getAsJsonArray("flood");
        int dur = 20 * 15;
        for (BlockPos p : BlockPos.betweenClosed(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(),
                a.get(3).getAsInt(), a.get(4).getAsInt(), a.get(5).getAsInt())) {
            if (level().getBlockState(p).isAir()) tempBlock(p.immutable(), Blocks.WATER.defaultBlockState(), dur);
        }
        floodTicks = dur;
        shoutAll("ЗАТОПЛЕНИЕ! Все на помосты по углам!");
        sound(SoundEvents.AMBIENT_UNDERWATER_ENTER, 3f, 0.5f);
        for (Vec3 p : points("platforms")) fxPillar(p, 1.5, 4, 40, 0xC0FFE080);
    }

    @Override
    protected void bossTick() {
        if (phase >= 2 && floodTicks <= 0) {
            floodTimer++;
            if (floodTimer >= 20 * 45) {
                floodTimer = 0;
                circle(arena(), 3, 60, 0f).color(Telegraph.BLUE);
                shoutAll("Вода прибывает! Через 3 с — затопление. На помосты!");
                com.andrey.litrpg.util.Scheduler.later(60, () -> {
                    if (isAlive() && engagedNow()) flood();
                });
            }
        }
        if (floodTicks > 0) {
            floodTicks--;
            if (floodTicks % 20 == 0) {
                double floor = arena().y;
                for (ServerPlayer p : players()) {
                    if (p.getY() < floor + 1.6 && p.isInWater()) {
                        hit(p, 0.14f);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                    }
                }
                if (level() instanceof ServerLevel sl) {
                    Vec3 c = arena();
                    sl.sendParticles(ParticleTypes.BUBBLE, c.x, c.y + 1, c.z, 60, 14, 0.5, 12, 0.1);
                }
            }
        }
    }

    private boolean engagedNow() {
        return fightTicks > 0 && !players().isEmpty();
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Утопленники, ко мне!");
            adds(EntityType.DROWNED, arena(), 4, 6, MobLevels.Rank.NORMAL, null);
        } else {
            shout("Море поднимается!");
            floodTimer = 20 * 45 - 60;
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        clearTemp();
        floodTicks = 0;
        floodTimer = 0;
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FROST ? 1.35f : w == BossDamage.Weapon.FIRE ? 0.6f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.DROWNED_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.DROWNED_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.DROWNED_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
