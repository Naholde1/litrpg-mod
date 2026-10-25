package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Хранитель этапа подземелья. Сверх {@link BossBase}:
 * <ul>
 *   <li>«живучесть» задаётся в эффективном здоровье для группы из троих ({@link #ehp()}), остальное — делитель урона;</li>
 *   <li>перенос всей группы в другой зал и обратно (смена локации в бою);</li>
 *   <li>помощники для эффектов (кольца, лучи, столбы) и призыва подручных нужного уровня.</li>
 * </ul>
 */
public abstract class StageBoss extends BossBase {
    protected StageBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level, color);
    }

    /** Сколько урона нужно нанести группе из троих, чтобы победить (для 4–5 — больше, см. Run.hpScale). */
    protected abstract float ehp();

    @Override
    protected float toughness() {
        return Math.max(1f, ehp() / Math.max(1f, getMaxHealth()));
    }

    /** Время до неистовства: 5 минут по умолчанию. */
    @Override
    protected int enrageTicks() {
        return 20 * 60 * 5;
    }

    // ---------------- перенос группы ----------------

    /** Перенести всех бойцов арены (и духов рядом) по точкам spots по кругу. */
    protected void moveGroup(List<Vec3> spots, net.minecraft.world.phys.AABB from) {
        if (!(level() instanceof ServerLevel sl) || spots.isEmpty()) return;
        int i = 0;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, from.inflate(4))) {
            if (p.isCreative()) continue;
            Vec3 to = spots.get(i++ % spots.size());
            Fx.pillar(sl, p.position(), 0.8, 3.5, 14, 0xC0A060FF);
            p.teleportTo(to.x, to.y + 0.1, to.z);
            p.fallDistance = 0;
            Fx.pillar(sl, to, 0.8, 3.5, 14, 0xC0A060FF);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 30, 0.4, 0.8, 0.4, 0.1);
        }
        soundAt(arena(), SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
    }

    // ---------------- эффекты ----------------

    protected void fxRing(Vec3 c, double r0, double r1, int ticks, int color) {
        Fx.ring(level(), c, r0, r1, ticks, color);
    }

    protected void fxPillar(Vec3 c, double r, double h, int ticks, int color) {
        Fx.pillar(level(), c, r, h, ticks, color);
    }

    protected void fxBeam(Vec3 a, Vec3 b, double w, int ticks, int color) {
        Fx.beam(level(), a, b, w, ticks, color);
    }

    protected void fxSlash(Vec3 dir, double r, double arc, double tilt, int color) {
        Fx.slash(level(), position().add(0, getBbHeight() * 0.55, 0), dir, r, arc, tilt, 8, color);
    }

    protected Vec3 chest() {
        return position().add(0, getBbHeight() * 0.6, 0);
    }

    protected static Vec3 chestOf(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    // ---------------- подручные ----------------

    /** Подручные уровня хранителя (здоровье — от размера группы). */
    protected <M extends Mob> int adds(EntityType<M> type, Vec3 at, int count, int max, MobLevels.Rank rank, Consumer<M> setup) {
        int lvl = Math.max(1, getBossLevel() - 1);
        return summonAt(type, at, count, max, lvl, m -> {
            MobLevels.apply(m, lvl, rank, com.andrey.litrpg.run.Run.hpScale(partySize));
            if (setup != null) setup.accept(m);
            Fx.pillar(level(), m.position(), 0.6, 2.5, 10, 0xB0A0FFA0);
        });
    }

    /** Живые подручные на арене. */
    protected int addsAlive() {
        return minions().size();
    }

    /** Убрать подручных (сброс, смерть). */
    protected void clearAdds() {
        for (Mob m : new ArrayList<>(minions())) m.discard();
    }

    @Override
    protected void onReset() {
        clearAdds();
    }

    /** Точка арены на полу (по сетке блоков). */
    protected BlockPos floorAt(Vec3 v) {
        return BlockPos.containing(v.x, v.y, v.z);
    }
}
