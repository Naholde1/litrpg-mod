package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Голем-плавильщик (этап 4, ур. 34). Плавильня с четырьмя закалочными купелями.
 * <ul>
 *   <li>Жар: в бою голем раскаляется (0–100%). Чем жарче, тем сильнее жжёт всех вокруг; на 100% — «Расплав» каждые 3 с.</li>
 *   <li>Закалка: танк заводит голема в купель с водой — жар сбрасывается, голем оглушён и 8 с получает ×1,5 урона,
 *       а купель выкипает на 60 с.</li>
 *   <li>«Молот» — конус по танку; «Выплеск металла» — полосы к двоим, на полу остаётся жидкий металл;
 *       «Шлак» — бомбы по всем; с 50% жар растёт вдвое быстрее, «Раскалённый пол» — кольца вокруг голема.</li>
 * </ul>
 */
public class SmelterGolemEntity extends StageBoss {
    private float heat;
    private long quenchedUntil;
    private final long[] poolDry = new long[8];

    public SmelterGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        setPathfindingMalus(net.minecraft.world.level.pathfinder.BlockPathTypes.WATER, 0f);
        skill("Молот", 110, 18, 1).first(30)
                .when(t -> distanceTo(t) < 8)
                .begin(t -> cone(position(), dirTo(t), 8, 45, 18, 1.1f).color(Telegraph.ORANGE).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 7, 90, 30, 0xE0FF8020);
                    sound(SoundEvents.ANVIL_LAND, 2f, 0.5f);
                });
        skill("Выплеск металла", 260, 26, 2).first(100)
                .begin(t -> {
                    for (int i = 0; i < 2; i++) {
                        LivingEntity v = randomOther();
                        if (v == null) continue;
                        Vec3 d = dirTo(v);
                        line(position(), d, 24, 1.4, 26, 0.8f).color(Telegraph.ORANGE)
                                .fx(c -> {
                                    for (int k = 4; k <= 20; k += 4) hazard(c.add(d.scale(k)), 1.8, 20 * 10, 0.1f, 0xC0FF9020,
                                            e -> e.setSecondsOnFire(2));
                                });
                    }
                })
                .act(t -> sound(SoundEvents.LAVA_POP, 3f, 0.5f));
        skill("Шлак", 220, 20, 1).first(70)
                .act(t -> {
                    for (ServerPlayer p : players()) shootAt(BossProjectile.Kind.BOMB, p.position(), 0.5f, 0.9f, 3f);
                    sound(SoundEvents.BLAZE_SHOOT, 2f, 0.5f);
                });
        skill("Раскалённый пол", 380, 36, 2).fromPhase(1).first(80).unstoppable()
                .begin(t -> {
                    Vec3 c = position();
                    circle(c, 6, 36, 0.9f).color(Telegraph.ORANGE);
                    donut(c, 11, 17, 36, 0.9f).color(Telegraph.ORANGE);
                    shoutAll("Раскалённый пол! Стойте между кольцами — 6–11 блоков от голема.");
                })
                .act(t -> fxRing(position(), 1, 17, 14, 0xE0FF6010));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 16.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 44.0);
    }

    @Override public int getBossLevel() { return 34; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 70000; }
    @Override public boolean fireImmune() { return true; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Голем раскаляется — жар жжёт всех вокруг. На 100% — «Расплав», группа не выдержит.",
                "Танк: заведите голема в закалочную купель (вода в полу) — жар спадёт, голем будет оглушён и уязвим. Купель потом выкипит на минуту.",
                "Жидкий металл после «Выплеска» держится 10 с — не стойте в нём.");
    }

    // ---------------- жар и закалка ----------------

    @Override
    protected void bossTick() {
        if (fightTicks <= 0) return;
        float rate = phase >= 1 ? 0.1f : 0.06f;
        heat = Math.min(100, heat + rate);
        if (tickCount % 20 == 0) {
            float power = 0.015f + heat * 0.0012f;
            for (ServerPlayer p : players()) if (p.distanceTo(this) < 14) hit(p, power);
            if (level() instanceof ServerLevel sl) {
                int n = 4 + (int) (heat / 10);
                sl.sendParticles(heat > 70 ? ParticleTypes.LAVA : ParticleTypes.FLAME, getX(), getY() + 2, getZ(), n, 1, 1.2, 1, 0.02);
            }
            if (tickCount % 40 == 0) {
                Component bar = Component.literal("Жар голема: " + (int) heat + "%")
                        .withStyle(heat >= 80 ? ChatFormatting.RED : heat >= 50 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
                for (ServerPlayer p : players()) p.displayClientMessage(bar, true);
            }
            if (heat >= 100 && tickCount % 60 == 0) {
                fxRing(position(), 1, 30, 16, 0xF0FFA020);
                for (ServerPlayer p : players()) hit(p, 0.55f);
                shoutAll("РАСПЛАВ! Остудите голема в купели!");
                sound(SoundEvents.GENERIC_EXPLODE, 2f, 0.5f);
            }
        }
        checkQuench();
    }

    private void checkQuench() {
        if (tickCount % 5 != 0) return;
        List<Vec3> pools = points("pools");
        long now = level().getGameTime();
        for (int i = 0; i < pools.size(); i++) {
            Vec3 p = pools.get(i);
            if (now < poolDry[i] || p.distanceToSqr(getX(), p.y, getZ()) > 6.5 || Math.abs(getY() - p.y) > 2) continue;
            poolDry[i] = now + 20 * 60;
            heat = 0;
            quenchedUntil = now + 20 * 8;
            BlockPos c = BlockPos.containing(p);
            for (BlockPos b : BlockPos.betweenClosed(c.offset(-1, 0, -1), c.offset(1, 0, 1))) {
                tempBlock(b.immutable(), Blocks.AIR.defaultBlockState(), 20 * 60);
            }
            if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 1, p.z, 80, 1.5, 1.5, 1.5, 0.1);
            Fx.pillar(level(), p, 2, 6, 20, 0xC0FFFFFF);
            sound(SoundEvents.FIRE_EXTINGUISH, 3f, 0.5f);
            stagger(80, "Закалка! Голем остыл и оглушён — бейте, он уязвим 8 с!");
            return;
        }
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        float m = level().getGameTime() < quenchedUntil ? 1.5f : 1f;
        return w == BossDamage.Weapon.FROST ? m * 1.3f : w == BossDamage.Weapon.FIRE ? m * 0.5f : w == BossDamage.Weapon.BLUNT ? m * 1.2f : m;
    }

    @Override
    protected void onPhase(int phase) {
        shout("ГОРН… РАЗГОРАЕТСЯ!");
    }

    @Override
    protected void onReset() {
        super.onReset();
        clearTemp();
        heat = 0;
        quenchedUntil = 0;
        java.util.Arrays.fill(poolDry, 0);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(0.9);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
