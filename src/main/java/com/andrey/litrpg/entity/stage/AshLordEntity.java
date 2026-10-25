package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Повелитель Пепла (этап 4, финал, ур. 40). Тронный зал, пол которого лежит над озером лавы.
 * <ol>
 *   <li>Зал (100–30%): «Пепельный клинок» (конус), «Метеоры» по всем, «Огненный столп» ползёт за игроком,
 *       «Извержение» — то круг у него, то кольцо вокруг. На 75% и 55% — «Обвал»: три плиты пола рушатся в лаву (навсегда).</li>
 *   <li>Кальдера (<30%): Повелитель утягивает группу в кальдеру — островки в озере лавы. «Огненные волны» катятся
 *       от центра кольцами, «Метеоритный дождь» гуще, саламандры выползают на островки.</li>
 * </ol>
 * Упавший в лаву выбирается на ближайший камень, теряя четверть здоровья.
 */
public class AshLordEntity extends StageBoss {
    private boolean caldera;
    private final Map<BlockPos, BlockState> fallen = new HashMap<>();
    private final List<Integer> standing = new ArrayList<>();
    private int waveTicks;

    public AshLordEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
        skill("Пепельный клинок", 110, 16, 1).first(30)
                .when(t -> distanceTo(t) < 9)
                .begin(t -> cone(position(), dirTo(t), 9, 55, 16, 1.2f).color(Telegraph.ORANGE).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 8, 120, 10, 0xE0FF5010);
                    sound(SoundEvents.PLAYER_ATTACK_SWEEP, 2f, 0.5f);
                });
        skill("Метеоры", 240, 30, 2).first(80)
                .begin(t -> {
                    for (ServerPlayer p : players()) {
                        Vec3 at = p.position();
                        circle(at, 3.2, 30, 0.8f).color(Telegraph.RED);
                        com.andrey.litrpg.util.Scheduler.later(12, () -> {
                            if (isAlive()) shootAt(BossProjectile.Kind.METEOR, at, 0f, 1.4f, 0f);
                        });
                    }
                    shoutAll("Метеоры! Уходите из кругов.");
                })
                .act(t -> sound(SoundEvents.GENERIC_EXPLODE, 2f, 0.6f));
        skill("Огненный столп", 320, 20, 2).first(140)
                .begin(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    circle(v.position(), 3, 70, 1.0f).color(Telegraph.ORANGE).follow(v, 50)
                            .fx(c -> fxPillar(c, 3, 10, 16, 0xE0FF7010));
                    tip(v, "Огненный столп идёт за вами — уведите его от группы!", net.minecraft.ChatFormatting.GOLD);
                })
                .act(t -> sound(SoundEvents.BLAZE_SHOOT, 2f, 0.4f));
        skill("Извержение", 380, 34, 2).first(200)
                .begin(t -> {
                    Vec3 c = position();
                    if (random.nextBoolean()) {
                        circle(c, 9, 34, 1.3f).color(Telegraph.RED);
                        shoutAll("Извержение у Повелителя — прочь от него!");
                    } else {
                        donut(c, 5, 24, 34, 1.3f).color(Telegraph.RED);
                        shoutAll("Извержение вокруг — к Повелителю, ближе 5 блоков!");
                    }
                })
                .act(t -> fxRing(position(), 1, 24, 16, 0xE0FF4010));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 56.0);
    }

    @Override public int getBossLevel() { return 40; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 42000; }
    @Override protected int enrageTicks() { return 20 * 60 * 8; }
    @Override public boolean fireImmune() { return true; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.75f, 0.55f, 0.3f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Под полом — лава. На 75% и 55% Повелитель обрушит по три плиты — уходите с размеченных.",
                "«Огненный столп» ползёт за выбранным — уводите его от группы. «Извержение»: то круг у него, то кольцо вокруг.",
                "С 30% — кальдера: островки в лаве, огненные волны от центра. Прыгайте через кольца.");
    }

    // ---------------- обвал ----------------

    private void collapse(int count) {
        if (standing.isEmpty()) for (int i = 0; i < sectionCount(); i++) standing.add(i);
        Collections.shuffle(standing, new java.util.Random(random.nextLong()));
        int n = Math.min(count, standing.size());
        for (int k = 0; k < n; k++) {
            int idx = standing.remove(0);
            var a = arenaExtra.getAsJsonArray("sections").get(idx).getAsJsonArray();
            int x1 = a.get(0).getAsInt(), y = a.get(1).getAsInt(), z1 = a.get(2).getAsInt();
            int x2 = a.get(3).getAsInt(), z2 = a.get(5).getAsInt();
            Vec3 c = new Vec3((x1 + x2) / 2.0 + 0.5, y + 1, z1);
            line(c, new Vec3(0, 0, 1), z2 - z1 + 1, (x2 - x1 + 1) / 2.0, 70, 0.4f).color(Telegraph.RED)
                    .fx(z -> dropSection(x1, y, z1, x2, z2));
        }
        shoutAll("ОБВАЛ! Размеченные плиты рухнут в лаву — уходите!");
        sound(SoundEvents.GENERIC_EXPLODE, 2.5f, 0.4f);
    }

    private int sectionCount() {
        return arenaExtra.has("sections") ? arenaExtra.getAsJsonArray("sections").size() : 0;
    }

    private void dropSection(int x1, int y, int z1, int x2, int z2) {
        for (BlockPos p : BlockPos.betweenClosed(x1, y, z1, x2, y, z2)) {
            BlockState st = level().getBlockState(p);
            if (st.isAir()) continue;
            fallen.put(p.immutable(), st);
            level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, (x1 + x2) / 2.0, y, (z1 + z2) / 2.0, 60, (x2 - x1) / 2.0, 0.5, (z2 - z1) / 2.0, 0.1);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, (x1 + x2) / 2.0, y + 1, (z1 + z2) / 2.0, 40, (x2 - x1) / 2.0, 1, (z2 - z1) / 2.0, 0.02);
        }
        soundAt(new Vec3(x1, y, z1), SoundEvents.DEEPSLATE_BREAK, 3f, 0.4f);
    }

    private void restoreFloor() {
        for (var e : fallen.entrySet()) level().setBlock(e.getKey(), e.getValue(), 3);
        fallen.clear();
        standing.clear();
    }

    // ---------------- кальдера ----------------

    private AABB calderaBox() {
        var a = arenaExtra.getAsJsonArray("caldera_box");
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    private void enterCaldera() {
        caldera = true;
        waveTicks = 0;
        shout("Ниже. Ещё ниже. В сердце горна!");
        moveGroup(points("caldera_spots"), arenaBox.aabb().inflate(0, 6, 0));
        Vec3 c = point("caldera");
        teleportTo(c.x, c.y, c.z);
        shoutAll("Кальдера! Островки в лаве. Огненные волны пойдут от центра — прыгайте через кольца.");
        sound(SoundEvents.LAVA_AMBIENT, 3f, 0.4f);
    }

    private void calderaTick() {
        waveTicks++;
        Vec3 c = point("caldera");
        if (waveTicks % 300 == 60) {
            // волны: три кольца одно за другим
            for (int k = 0; k < 3; k++) {
                int delay = 30 + k * 18;
                donut(c, 4 + k * 6, 8 + k * 6, delay, 0.9f).color(Telegraph.RED);
            }
            shoutAll("Огненные волны! Кольца идут от центра одно за другим.");
        }
        if (waveTicks % 400 == 200) {
            var sal = ModEntities.WILD.get("salamander");
            if (sal != null) {
                List<Vec3> spots = points("caldera_spots");
                for (int i = 0; i < 2 && !spots.isEmpty(); i++) adds(sal.get(), spots.get(random.nextInt(spots.size())), 1, 4, MobLevels.Rank.NORMAL, null);
                shoutAll("Саламандры выползают на островки!");
            }
        }
        if (waveTicks % 20 == 0 && level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 10, 12, 0.2, 12, 0.1);
        }
    }

    @Override
    protected void bossTick() {
        if (caldera) calderaTick();
    }

    @Override
    protected void onPhase(int phase) {
        switch (phase) {
            case 1 -> {
                shout("Этот зал помнит, как плавился камень!");
                collapse(3);
            }
            case 2 -> {
                shout("Падайте же!");
                collapse(3);
            }
            default -> enterCaldera();
        }
    }

    @Override
    protected boolean leashed() {
        return !caldera && super.leashed();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (arenaExtra.has("caldera_box") && level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, calderaBox(),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) if (!out.contains(p)) out.add(p);
        }
        return out;
    }

    @Override
    protected List<LivingEntity> fighters() {
        if (caldera) return new ArrayList<>(players());
        return super.fighters();
    }

    @Override
    public Vec3 arena() {
        return caldera ? point("caldera") : super.arena();
    }

    @Override
    protected void onReset() {
        super.onReset();
        restoreFloor();
        if (caldera && arenaExtra.has("caldera_box")) {
            Vec3 t = point("throne");
            List<Vec3> spots = new ArrayList<>();
            for (int i = 0; i < 5; i++) spots.add(t.add(-6 + i * 3, 0, 16));
            moveGroup(spots, calderaBox());
        }
        caldera = false;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy() && reason != RemovalReason.KILLED) restoreFloor();
        super.remove(reason);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FROST ? 1.3f : w == BossDamage.Weapon.FIRE ? 0.4f : w == BossDamage.Weapon.HOLY ? 1.1f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
