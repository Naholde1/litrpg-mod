package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.registry.ModEntities;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Король-лич (этап 1, финал, ур. 10). Три части боя, две локации.
 * <ol>
 *   <li>Тронный зал (100–65%): «Ледяная коса» по танку, «Град черепов» на всех, «Тёмный зов» — стягивает всех
 *       к себе и через полторы секунды взрывается (бегите прочь), рыцари смерти.</li>
 *   <li>Фиал (с 65%): Король переносит группу под тронный зал — на кольцо платформ над бездной — и становится неуязвим,
 *       пока цела филактерия в центре. Платформы рушатся по очереди, призраки нападают, «Вой Фиала» с каждым разом сильнее.</li>
 *   <li>Снова тронный зал: «Обрушение» — пол горит огнём душ всё шире, «Ледяная гробница» (союзники разбивают лёд),
 *       с 25% — «Последний приказ»: мёртвые встают все разом.</li>
 * </ol>
 */
public class LichKingEntity extends StageBoss {
    /** 0 — трон, 1 — Фиал, 2 — снова трон. */
    private int part;
    private int vaultTicks;
    private int howls;
    private boolean lastOrder;
    private final Map<BlockPos, BlockState> crumbled = new HashMap<>();
    private int crumbleIdx;

    public LichKingEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        skill("Ледяная коса", 120, 16, 1).first(40)
                .when(t -> part != 1 && distanceTo(t) < 8)
                .begin(t -> cone(position(), dirTo(t), 8, 50, 16, 1.0f).color(Telegraph.BLUE)
                        .then(e -> {
                            BossDamage.sunder(e, 1);
                            e.setTicksFrozen(Math.max(e.getTicksFrozen(), 200));
                        }))
                .act(t -> {
                    fxSlash(dirTo(t), 7, 110, 25, 0xE080D0FF);
                    sound(SoundEvents.GLASS_BREAK, 2f, 0.6f);
                });
        skill("Град черепов", 240, 30, 2).first(100)
                .when(t -> part != 1)
                .begin(t -> {
                    for (ServerPlayer p : players()) {
                        circle(p.position(), 3, 30, 0.55f).color(Telegraph.PURPLE);
                        circle(p.position().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4), 3, 36, 0.55f).color(Telegraph.PURPLE);
                    }
                })
                .act(t -> {
                    for (ServerPlayer p : players()) shootAt(BossProjectile.Kind.SHADOW, p.position(), 0.2f, 1.4f, 4f);
                    sound(SoundEvents.WITHER_SHOOT, 2f, 0.6f);
                });
        skill("Тёмный зов", 420, 36, 2).first(220).unstoppable()
                .when(t -> part != 1)
                .begin(t -> {
                    for (LivingEntity e : fighters()) {
                        pullTo(e, position(), 2.2);
                        Fx.tether(level(), this, e, 0.2, 14, 0xC08A30E0);
                    }
                    circle(position(), 7, 36, 0.9f).color(Telegraph.PURPLE);
                    shoutAll("«Тёмный зов» — вас тянет к Королю! Бегите прочь от него!");
                })
                .act(t -> {
                    fxRing(position(), 1, 8, 12, 0xF0A040FF);
                    fxPillar(position(), 3, 6, 10, 0xC0A040FF);
                    sound(SoundEvents.WARDEN_SONIC_BOOM, 2f, 0.7f);
                });
        skill("Ледяная гробница", 480, 20, 1).fromPhase(1).first(160)
                .when(t -> part == 2)
                .act(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    entomb(v, Blocks.PACKED_ICE.defaultBlockState(), 200, 0.09f, true);
                    shoutAll(v.getName().getString() + " во льду — разбейте гробницу!");
                    sound(SoundEvents.GLASS_PLACE, 2f, 0.5f);
                });
        skill("Обрушение", 260, 34, 2).fromPhase(1).first(60).unstoppable()
                .when(t -> part == 2)
                .begin(t -> {
                    for (int i = 0; i < 3 + phase; i++) {
                        Vec3 spot = i == 0 && t != null ? t.position() : randomSpot(18);
                        circle(spot, 3.5, 34, 0.6f).color(Telegraph.BLUE).fx(c -> soulFire(c, 3.2));
                    }
                })
                .act(t -> sound(SoundEvents.DEEPSLATE_BREAK, 3f, 0.4f));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 10; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 9500; }
    @Override protected int enrageTicks() { return 20 * 60 * 7; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.65f, 0.25f};
    }

    @Override
    protected List<String> intro() {
        return List.of("«Тёмный зов» тянет всех к Королю — сразу бегите прочь из круга.",
                "С 65% Король уведёт вас в Фиал. Разбейте филактерию в центре — пока она цела, он неуязвим. Не падайте!",
                "Вернувшись, следите за полом: огонь душ остаётся надолго. С 25% мёртвые встанут все разом.");
    }

    @Override
    protected void onEngage() {
        adds(ModEntities.GRAVE_KNIGHT.get(), position(), 2, 2, MobLevels.Rank.ELITE, null);
        shout("Склонитесь — и, может быть, я подниму вас снова.");
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) enterVault();
        else if (phase == 2 && !lastOrder) {
            lastOrder = true;
            shout("Встаньте. Все. Это последний приказ.");
            adds(EntityType.SKELETON, position(), 4, 8, MobLevels.Rank.NORMAL, null);
            adds(EntityType.ZOMBIE, position(), 2, 8, MobLevels.Rank.ELITE, null);
            fxRing(position(), 1, 18, 20, 0xC08A30E0);
        }
    }

    // ---------------- Фиал ----------------

    private Vec3 relicPos() {
        return point("vault");
    }

    private void enterVault() {
        part = 1;
        vaultTicks = 0;
        howls = 0;
        crumbleIdx = 0;
        immune = "Король неуязвим, пока цела филактерия в Фиале!";
        Vec3 v = relicPos();
        BlockPos rp = BlockPos.containing(v);
        if (!level().getBlockState(rp).is(ModBlocks.RELIC.get())) {
            level().setBlock(rp, ModBlocks.RELIC.get().defaultBlockState().setValue(com.andrey.litrpg.block.RelicBlock.KIND,
                    com.andrey.litrpg.block.RelicBlock.Kind.SOUL), 3);
        }
        shout("Вы хотели увидеть моё сердце? Смотрите!");
        moveGroup(points("platforms"), arenaBox.aabb());
        teleportTo(v.x + 1.5, v.y, v.z + 1.5);
        Fx.pillar(level(), v, 1.2, 12, 60, 0xD08A30E0);
        adds(ModEntities.FROST_WRAITH.get(), v, 2, 6, MobLevels.Rank.NORMAL, null);
        shoutAll("Фиал! Разбейте филактерию в центре (ломайте блок). Платформы рушатся — не стойте на треснувших.");
    }

    private void vaultTick() {
        vaultTicks++;
        Vec3 v = relicPos();
        getNavigation().stop();
        if (vaultTicks % 10 == 0) Fx.spiral(level(), this, 1.2, 4, 2, 12, 0xC08A30E0);
        if (vaultTicks % 300 == 150) adds(ModEntities.FROST_WRAITH.get(), v, 2, 6, MobLevels.Rank.NORMAL, null);
        // платформы рушатся по очереди (с предупреждением)
        List<Vec3> plats = points("platforms");
        if (!plats.isEmpty() && vaultTicks % 200 == 100) {
            Vec3 p = plats.get(crumbleIdx++ % plats.size());
            circle(p, 2.7, 40, 0f).color(Telegraph.ORANGE).fx(c -> crumble(c));
            shoutAll("Платформа трескается — уходите с неё!");
        }
        // вой Фиала: всё сильнее
        if (vaultTicks % 500 == 250) {
            howls++;
            for (ServerPlayer p : players()) hit(p, 0.2f + 0.12f * howls);
            fxRing(v, 1, 16, 16, 0xE0A040FF);
            sound(SoundEvents.WARDEN_ROAR, 2f, 0.5f);
            shoutAll("Вой Фиала! С каждым разом сильнее — скорее разбейте филактерию!");
        }
        if (!level().getBlockState(BlockPos.containing(v)).is(ModBlocks.RELIC.get())) leaveVault();
    }

    private void crumble(Vec3 c) {
        BlockPos b = BlockPos.containing(c.x, c.y - 1, c.z);
        for (BlockPos p : BlockPos.betweenClosed(b.offset(-3, 0, -3), b.offset(3, 0, 3))) {
            BlockState st = level().getBlockState(p);
            if (st.isAir() || p.distSqr(b) > 9 || st.is(Blocks.OBSIDIAN)) continue;
            crumbled.put(p.immutable(), st);
            level().setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
        burst(ParticleTypes.CLOUD, c, 30, 1.5, 0.05);
        sound(SoundEvents.DEEPSLATE_BRICKS_BREAK, 2f, 0.5f);
        com.andrey.litrpg.util.Scheduler.later(300, this::restoreCrumbled);
    }

    private void restoreCrumbled() {
        crumbled.forEach((p, st) -> level().setBlock(p, st, 3));
        crumbled.clear();
    }

    private void leaveVault() {
        part = 2;
        immune = null;
        restoreCrumbled();
        shout("Нет… НЕТ! Без сердца я всё ещё король!");
        AABB vaultBox = arenaExtra.has("vault_box") ? box(arenaExtra.getAsJsonArray("vault_box")) : new AABB(relicPos(), relicPos()).inflate(16);
        Vec3 th = point("throne");
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 5; i++) spots.add(th.add(-6 + i * 3, 0, 18));
        moveGroup(spots, vaultBox);
        teleportTo(th.x, th.y, th.z);
        clearAdds();
        stagger(80, "Филактерия разбита — Король оглушён!");
        fxPillar(th, 2, 10, 30, 0xF0FFFFFF);
    }

    private static AABB box(com.google.gson.JsonArray a) {
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    /** Огонь душ на полу (держится 40 с). */
    private void soulFire(Vec3 c, double r) {
        BlockPos b = BlockPos.containing(c);
        for (BlockPos p : BlockPos.betweenClosed(b.offset((int) -r, -1, (int) -r), b.offset((int) r, 1, (int) r))) {
            if (p.distSqr(b) > r * r) continue;
            BlockState below = level().getBlockState(p.below());
            if (level().getBlockState(p).isAir() && below.isSolidRender(level(), p.below())) {
                tempBlock(p.immutable(), Blocks.SOUL_FIRE.defaultBlockState(), 20 * 40);
            }
        }
        fxRing(c, 0.5, r + 0.5, 12, 0xD040C8E8);
    }

    @Override
    protected boolean leashed() {
        return part != 1 && super.leashed();
    }

    @Override
    protected List<LivingEntity> fighters() {
        if (part == 1) {
            Vec3 v = relicPos();
            return level().getEntitiesOfClass(LivingEntity.class, new AABB(v, v).inflate(16, 10, 16),
                    e -> e != this && e.isAlive() && !isMinion(e) && e instanceof ServerPlayer p && !p.isSpectator() && !p.isCreative());
        }
        return super.fighters();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (level() instanceof ServerLevel sl) {
            Vec3 v = relicPos();
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(v, v).inflate(18, 12, 18),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
                if (!out.contains(p)) out.add(p);
            }
        }
        return out;
    }

    @Override
    protected void bossTick() {
        if (part == 1) vaultTick();
    }

    @Override
    protected boolean canCast() {
        return part != 1 && super.canCast();
    }

    @Override
    protected void onReset() {
        super.onReset();
        part = 0;
        lastOrder = false;
        restoreCrumbled();
        Vec3 v = relicPos();
        BlockPos rp = BlockPos.containing(v);
        if (!level().getBlockState(rp).is(ModBlocks.RELIC.get())) {
            level().setBlock(rp, ModBlocks.RELIC.get().defaultBlockState().setValue(com.andrey.litrpg.block.RelicBlock.KIND,
                    com.andrey.litrpg.block.RelicBlock.Kind.SOUL), 3);
        }
    }

    @Override
    public void die(DamageSource src) {
        restoreCrumbled();
        super.die(src);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.HOLY || w == BossDamage.Weapon.FIRE ? 1.4f : w == BossDamage.Weapon.FROST ? 0.5f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(0.95);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITHER_SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITHER_SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
