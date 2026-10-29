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

    // ---------------- лужи-опасности ----------------

    /** Опасное место на полу на время: каждую секунду бьёт стоящих (power — доля «единицы»). */
    protected record Hazard(Vec3 c, double r, long until, float power, int color, java.util.function.Consumer<LivingEntity> extra) {}

    protected final List<Hazard> hazards = new ArrayList<>();

    protected void hazard(Vec3 c, double r, int ticks, float power, int color, java.util.function.Consumer<LivingEntity> extra) {
        hazards.add(new Hazard(c, r, level().getGameTime() + ticks, power, color, extra));
        Fx.disc(level(), c, r, 0, ticks, color & 0x70FFFFFF);
    }

    private void tickHazards() {
        if (hazards.isEmpty()) return;
        long now = level().getGameTime();
        hazards.removeIf(h -> now >= h.until);
        if (now % 20 != 0) return;
        for (Hazard h : hazards) {
            for (LivingEntity e : enemiesAround(h.c, h.r + 1)) {
                double dx = e.getX() - h.c.x, dz = e.getZ() - h.c.z;
                if (dx * dx + dz * dz > h.r * h.r || Math.abs(e.getY() - h.c.y) > 3) continue;
                if (h.power > 0) hit(e, h.power);
                if (h.extra != null) h.extra.accept(e);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive()) {
            tickHazards();
            tickWaves();
        }
    }

    // ---------------- идейные волны ----------------

    /** Волны на 75% и 45% здоровья: у каждого этапа своя идея, а не просто «ещё мобы». */
    private static final float[] WAVE_AT = {0.75f, 0.45f};
    private static final String WAVE = "litrpg_wave";
    private static final String FUSE = "litrpg_wave_fuse";
    private int wavesDone = 0;
    private int waveTheme = 0;
    private long waveStarted = 0;
    /**
     * Сколько раз отработала {@link #runWave()} — она вызывается раз в 5 тиков, и считать время
     * волны по остатку от возраста нельзя: возраст попадает только в свой класс по модулю 5,
     * поэтому «каждую секунду» и «каждые две секунды» просто не наступали. Осколки Бездны
     * из-за этого не лечили хранителя вообще.
     */
    private int waveTicks = 0;

    /** Тема волн: по этапу (уровень хранителя 1–10 — первый этап и т.д.). Хранитель может отключить — вернуть 0. */
    protected int waveTheme() {
        return Math.max(1, Math.min(5, (getBossLevel() - 1) / 10 + 1));
    }

    private List<Mob> waveMobs() {
        List<Mob> out = new ArrayList<>();
        for (Mob m : minions()) if (m.getPersistentData().getBoolean(WAVE)) out.add(m);
        return out;
    }

    private void tell(String text) {
        for (ServerPlayer p : players()) {
            p.displayClientMessage(net.minecraft.network.chat.Component.literal(text).withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }

    /** Точки по кругу у края арены вокруг хранителя. */
    private List<Vec3> ring(int n, double r) {
        List<Vec3> out = new ArrayList<>();
        double off = random.nextDouble() * Math.PI;
        for (int i = 0; i < n; i++) {
            double a = off + Math.PI * 2 * i / n;
            Vec3 c = position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            // не в стену: если там камень — ближе к хранителю
            for (double k = 1; k > 0.3 && !level().getBlockState(BlockPos.containing(c.add(0, 0.5, 0))).isAir(); k -= 0.15) {
                c = position().add(Math.cos(a) * r * k, 0, Math.sin(a) * r * k);
            }
            out.add(c);
        }
        return out;
    }

    private void tickWaves() {
        if (!inCombat()) return;
        int theme = waveTheme();
        if (theme > 0 && wavesDone < WAVE_AT.length && getHealth() / getMaxHealth() <= WAVE_AT[wavesDone]) {
            wavesDone++;
            startWave(theme);
        }
        if (waveTheme > 0 && tickCount % 5 == 0) runWave();
    }

    private void startWave(int theme) {
        waveTheme = theme;
        waveStarted = level().getGameTime();
        waveTicks = 0;
        Consumer<Mob> tag = m -> m.getPersistentData().putBoolean(WAVE, true);
        switch (theme) {
            case 1 -> {
                for (Vec3 c : ring(4, 9)) adds(EntityType.SKELETON, c, 1, 14, MobLevels.Rank.NORMAL, m -> {
                    tag.accept(m);
                    m.setCustomName(net.minecraft.network.chat.Component.literal("Костяной страж"));
                });
                tell("Костяная стража! Пока она стоит — хранитель получает вдвое меньше.");
            }
            case 2 -> {
                for (Vec3 c : ring(3, 7)) adds(EntityType.SLIME, c, 1, 14, MobLevels.Rank.NORMAL, m -> {
                    tag.accept(m);
                    m.setSize(2, true);
                    m.setNoAi(true);
                    m.setCustomName(net.minecraft.network.chat.Component.literal("Кладка"));
                    Fx.disc(level(), m.position(), 1.6, 0, 120, 0xA080E040);
                });
                tell("Кладка! Разбейте яйца за шесть секунд — иначе вылупятся пауки.");
            }
            case 3 -> {
                for (Vec3 c : ring(4, 11)) adds(EntityType.DROWNED, c, 1, 14, MobLevels.Rank.NORMAL, m -> {
                    tag.accept(m);
                    m.setCustomName(net.minecraft.network.chat.Component.literal("Несущий прилив"));
                });
                tell("Несущие прилив идут к хранителю: каждый дошедший его лечит!");
            }
            case 4 -> {
                for (Vec3 c : ring(4, 8)) adds(EntityType.MAGMA_CUBE, c, 1, 14, MobLevels.Rank.NORMAL, m -> {
                    tag.accept(m);
                    m.setSize(2, true);
                    m.setCustomName(net.minecraft.network.chat.Component.literal("Пепельная бомба"));
                });
                tell("Пепельные бомбы! Рядом с вами они взрываются — разбегайтесь или бейте издалека.");
            }
            default -> {
                for (Vec3 c : ring(3, 6)) adds(EntityType.VEX, c.add(0, 2, 0), 1, 14, MobLevels.Rank.NORMAL, m -> {
                    tag.accept(m);
                    m.setCustomName(net.minecraft.network.chat.Component.literal("Осколок Бездны"));
                });
                tell("Осколки Бездны питают хранителя — каждый живой лечит его!");
            }
        }
        fxRing(position(), 0.5, 12, 20, 0xC0FFB040);
        level().playSound(null, blockPosition(), SoundEvents.RAID_HORN.get(), net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.8f);
    }

    private void runWave() {
        List<Mob> list = waveMobs();
        if (list.isEmpty()) {
            if (waveTheme > 0 && level().getGameTime() - waveStarted > 40) tell("Волна отбита.");
            waveTheme = 0;
            return;
        }
        long age = level().getGameTime() - waveStarted;
        waveTicks++;
        switch (waveTheme) {
            case 1 -> {
                if (waveTicks % 8 == 1) for (Mob m : list) Fx.chains(level(), this, m, 0.35, 42, 0xC0D0C8B0);
            }
            case 2 -> {
                if (age >= 120) {
                    for (Mob egg : list) {
                        adds(EntityType.CAVE_SPIDER, egg.position(), 3, 20, MobLevels.Rank.NORMAL, null);
                        level().playSound(null, egg.blockPosition(), SoundEvents.TURTLE_EGG_HATCH, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.6f);
                        egg.discard();
                    }
                }
            }
            case 3 -> {
                for (Mob m : list) {
                    m.setTarget(null);
                    m.getNavigation().moveTo(this, 1.0);
                    if (m.distanceTo(this) < 2.8) {
                        heal(getMaxHealth() * 0.05f);
                        fxBeam(chestOf(m), chest(), 0.3, 10, 0xC060C0FF);
                        level().playSound(null, blockPosition(), SoundEvents.DROWNED_AMBIENT_WATER, net.minecraft.sounds.SoundSource.HOSTILE, 1.5f, 0.6f);
                        tell("Прилив дошёл — хранитель лечится.");
                        m.discard();
                    }
                }
            }
            case 4 -> {
                long now = level().getGameTime();
                for (Mob m : list) {
                    long fuse = m.getPersistentData().getLong(FUSE);
                    if (fuse == 0) {
                        for (ServerPlayer p : players()) {
                            if (p.distanceTo(m) < 2.6) {
                                m.getPersistentData().putLong(FUSE, now + 20);
                                Fx.disc(level(), m.position(), 3, 0, 20, 0xB0FF5010);
                                break;
                            }
                        }
                    } else if (now >= fuse) {
                        Fx.sphere(level(), m.position().add(0, 0.5, 0), 0.4, 3, 10, 0xE0FF7020);
                        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.EXPLOSION, m.getX(), m.getY() + 0.5, m.getZ(), 1, 0, 0, 0, 0);
                        level().playSound(null, m.blockPosition(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 1.2f);
                        for (ServerPlayer p : players()) if (p.distanceTo(m) < 3) hit(p, 0.4f);
                        m.discard();
                    }
                }
            }
            default -> {
                if (waveTicks % 4 == 1) {
                    // по 0,5% за осколок в секунду: при 1% три осколка возвращали хранителю
                    // четверть здоровья за восемь секунд — отряд не успевал их добить
                    heal(getMaxHealth() * 0.005f * list.size());
                    for (Mob m : list) fxBeam(chestOf(m), chest(), 0.15, 18, 0xC0A060FF);
                }
            }
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // костяная стража держит щит, пока стоит
        if (waveTheme == 1 && !waveMobs().isEmpty() && source.getEntity() instanceof ServerPlayer) amount *= 0.5f;
        return super.hurt(source, amount);
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
        hazards.clear();
        wavesDone = 0;
        waveTheme = 0;
        waveTicks = 0;
    }

    /** Точка арены на полу (по сетке блоков). */
    protected BlockPos floorAt(Vec3 v) {
        return BlockPos.containing(v.x, v.y, v.z);
    }
}
