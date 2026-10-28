package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Пожиратель миров (этап 5, ур. 47). Глотает целые миры — и бой переносится из одного осколка в другой.
 * <ol>
 *   <li>Пасть (100–70%): «Укус» (конус), «Поглощение» — 3 с тянет всех к себе, затем взрыв вокруг;
 *       «Разлом реальности» — полосы к троим; «Звёздная пыль» — круги под всеми.</li>
 *   <li>Ледяной осколок (70–40%): мороз копится у всех, кто дальше 4 блоков от костра (замедление и урон растут);
 *       метель каждые 8 с дует в новую сторону; костры гаснут по одному.</li>
 *   <li>Огненный осколок (40–15%): гейзеры лавы бьют из ям, огненный дождь, пол раскалён у ям.</li>
 *   <li>Пасть (<15%): «Голод» — Пожиратель тянет жизнь из всех, сильнее с каждой секундой. Добивайте!</li>
 * </ol>
 */
public class WorldEaterEntity extends StageBoss {
    /** 0 — пасть, 1 — лёд, 2 — огонь, 3 — снова пасть (голод). */
    private int world;
    private int worldTicks;
    private Direction wind = Direction.NORTH;
    private int doused;
    private final Map<UUID, Integer> frost = new HashMap<>();

    public WorldEaterEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Укус", 100, 14, 1).first(30)
                .when(t -> distanceTo(t) < 8)
                .begin(t -> cone(position(), dirTo(t), 8, 50, 14, 1.2f).color(Telegraph.PURPLE).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 7, 100, 0, 0xE0B040FF);
                    sound(SoundEvents.EVOKER_FANGS_ATTACK, 2f, 0.4f);
                });
        skill("Поглощение", 420, 60, 2).first(160)
                .begin(t -> {
                    circle(position(), 6, 60, 1.4f).color(Telegraph.PURPLE);
                    shoutAll("Поглощение! Пожиратель тянет всех — бегите прочь из круга!");
                })
                .during(left -> {
                    if (left % 2 != 0) return;
                    for (ServerPlayer p : players()) if (p.distanceTo(this) < 26) pullTo(p, position(), 0.08);
                    if (left % 6 == 0) fxRing(position(), 14, 2, 8, 0x90B040FF);
                })
                .act(t -> fxRing(position(), 0.5, 6, 12, 0xF0D080FF));
        skill("Разлом реальности", 240, 26, 2).first(80)
                .begin(t -> {
                    for (int i = 0; i < 3; i++) {
                        LivingEntity v = randomOther();
                        if (v == null) continue;
                        line(position(), dirTo(v), 26, 1.3, 26, 0.9f).color(Telegraph.PURPLE);
                    }
                })
                .act(t -> sound(SoundEvents.END_PORTAL_FRAME_FILL, 2f, 0.5f));
        skill("Звёздная пыль", 300, 30, 2).first(120)
                .begin(t -> {
                    for (ServerPlayer p : players()) circle(p.position(), 2.8, 30, 0.7f).color(Telegraph.PURPLE);
                    shoutAll("Звёздная пыль — разойдитесь!");
                })
                .act(t -> {
                    for (ServerPlayer p : players()) shootAt(BossProjectile.Kind.CRYSTAL, p.position(), 0f, 1.2f, 0f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 17.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 47; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 40000; }
    @Override public boolean fireImmune() { return true; }
    @Override protected int enrageTicks() { return 20 * 60 * 7; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.7f, 0.4f, 0.15f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Пожиратель проглотит вас вместе с миром: на 70% — ледяной осколок, на 40% — огненный.",
                "Во льду держитесь у костров (до 4 блоков) — вдали копится мороз. Метель сносит — упирайтесь.",
                "В огне не стойте у ям — гейзеры. На 15% — «Голод»: жизнь утекает у всех. Добивайте быстро.");
    }

    // ---------------- миры ----------------

    private AABB box(String key) {
        var a = arenaExtra.getAsJsonArray(key);
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    private AABB currentBox() {
        return switch (world) {
            case 1 -> box("ice_box");
            case 2 -> box("fire_box");
            default -> arenaBox.aabb();
        };
    }

    private void devour(int to) {
        AABB from = currentBox();
        world = to;
        worldTicks = 0;
        frost.clear();
        String key = to == 1 ? "ice" : to == 2 ? "fire" : "home";
        List<Vec3> spots = to == 1 ? points("ice_spots") : to == 2 ? points("fire_spots") : new ArrayList<>();
        if (spots.isEmpty()) {
            Vec3 h = point("home");
            for (int i = 0; i < 5; i++) spots.add(h.add(-6 + i * 3, 0, 10));
        }
        fxPillar(position(), 3, 10, 20, 0xE0B040FF);
        moveGroup(spots, from);
        Vec3 c = point(key);
        teleportTo(c.x, c.y, c.z);
        if (to == 0) clearAltArena();
        else setAltArena(currentBox(), c);
        if (to == 1) {
            doused = 0;
            lightBraziers(true);
            shout("Холодный мир. Вкусный.");
            shoutAll("Ледяной осколок! Держитесь у костров — вдали копится мороз.");
        } else if (to == 2) {
            shout("Этот мир ещё горит.");
            shoutAll("Огненный осколок! Гейзеры бьют из ям — не стойте рядом.");
        } else {
            shout("ГОЛОД!");
            shoutAll("Голод Пожирателя: жизнь утекает у всех, сильнее с каждой секундой. Добейте его!");
        }
        sound(SoundEvents.ENDER_DRAGON_GROWL, 2f, 0.5f);
    }

    private void lightBraziers(boolean lit) {
        for (Vec3 w : points("braziers")) {
            BlockPos p = BlockPos.containing(w);
            BlockState st = level().getBlockState(p);
            if (st.getBlock() instanceof CampfireBlock) level().setBlock(p, st.setValue(CampfireBlock.LIT, lit), 3);
        }
    }

    private void iceTick() {
        List<Vec3> braz = points("braziers");
        if (worldTicks % 400 == 399 && doused < braz.size() - 1) {
            Vec3 w = braz.get(doused++);
            BlockPos p = BlockPos.containing(w);
            BlockState st = level().getBlockState(p);
            if (st.getBlock() instanceof CampfireBlock) level().setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
            shoutAll("Метель задула костёр! Осталось: " + (braz.size() - doused));
            sound(SoundEvents.FIRE_EXTINGUISH, 2f, 0.6f);
        }
        if (worldTicks % 160 == 0) {
            wind = Direction.from2DDataValue(random.nextInt(4));
            shoutAll("Метель дует " + switch (wind) {
                case NORTH -> "на север";
                case SOUTH -> "на юг";
                case WEST -> "на запад";
                default -> "на восток";
            } + "!");
        }
        for (ServerPlayer p : players()) {
            p.push(wind.getStepX() * 0.035, 0, wind.getStepZ() * 0.035);
            p.hurtMarked = true;
        }
        if (worldTicks % 20 != 0) return;
        if (level() instanceof ServerLevel sl) {
            Vec3 c = point("ice");
            sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y + 4, c.z, 60, 10, 3, 10, 0.05);
        }
        for (ServerPlayer p : players()) {
            boolean warm = false;
            for (int i = doused; i < braz.size(); i++) if (braz.get(i).distanceTo(p.position()) < 4.5) warm = true;
            int f = frost.getOrDefault(p.getUUID(), 0);
            f = warm ? Math.max(0, f - 2) : Math.min(10, f + 1);
            frost.put(p.getUUID(), f);
            if (f > 0) {
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, Math.min(3, f / 3)));
                if (f >= 3) hit(p, 0.02f * f);
            }
            if (warm) Fx.ring(level(), p.position(), 0.2, 1.2, 10, 0x80FFD080);
        }
    }

    private void fireTick() {
        if (worldTicks % 120 == 40) {
            List<Vec3> pits = new ArrayList<>();
            Vec3 c = point("fire");
            for (int k = 0; k < 6; k++) {
                double a = Math.PI * 2 * k / 6 + 0.3;
                pits.add(new Vec3(c.x + Math.round(Math.cos(a) * 6) - 0.5, c.y, c.z + Math.round(Math.sin(a) * 6) - 0.5));
            }
            java.util.Collections.shuffle(pits, new java.util.Random(random.nextLong()));
            for (int i = 0; i < 3; i++) circle(pits.get(i), 3.5, 40, 1.0f).color(Telegraph.ORANGE)
                    .fx(z -> fxPillar(z, 1.5, 9, 16, 0xE0FF6010));
            shoutAll("Гейзеры! Прочь от размеченных ям!");
        }
        if (worldTicks % 200 == 100) {
            for (ServerPlayer p : players()) {
                Vec3 at = p.position();
                circle(at, 2.5, 30, 0.6f).color(Telegraph.RED);
                com.andrey.litrpg.util.Scheduler.later(10, () -> {
                    if (isAlive()) shootAt(BossProjectile.Kind.METEOR, at, 0f, 1.3f, 0f);
                });
            }
        }
    }

    private void hungerTick() {
        if (worldTicks % 20 != 0) return;
        float power = 0.02f + 0.004f * (worldTicks / 100);
        for (ServerPlayer p : players()) {
            hit(p, power);
            Fx.beam(level(), p.position().add(0, 1, 0), chest(), 0.15, 10, 0x90B040FF);
        }
    }

    @Override
    protected void bossTick() {
        if (fightTicks <= 0) return;
        worldTicks++;
        switch (world) {
            case 1 -> iceTick();
            case 2 -> fireTick();
            case 3 -> hungerTick();
            default -> { }
        }
    }

    @Override
    protected void onPhase(int phase) {
        devour(phase);
    }

    @Override
    protected boolean leashed() {
        return (world == 0 || world == 3) && super.leashed();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (level() instanceof ServerLevel sl) {
            for (String k : new String[]{"ice_box", "fire_box"}) {
                if (!arenaExtra.has(k)) continue;
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box(k), p -> p.isAlive() && !p.isCreative() && !p.isSpectator()))
                    if (!out.contains(p)) out.add(p);
            }
        }
        return out;
    }

    @Override
    protected List<LivingEntity> fighters() {
        if (world == 1 || world == 2) return new ArrayList<>(players());
        return super.fighters();
    }

    @Override
    public Vec3 arena() {
        return world == 1 ? point("ice") : world == 2 ? point("fire") : super.arena();
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (world == 1 || world == 2) {
            Vec3 h = point("home");
            List<Vec3> spots = new ArrayList<>();
            for (int i = 0; i < 5; i++) spots.add(h.add(-6 + i * 3, 0, 10));
            moveGroup(spots, currentBox());
        }
        clearAltArena();
        world = 0;
        frost.clear();
        lightBraziers(true);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        if (world == 1) return w == BossDamage.Weapon.FIRE ? 1.4f : w == BossDamage.Weapon.FROST ? 0.6f : 1f;
        if (world == 2) return w == BossDamage.Weapon.FROST ? 1.4f : w == BossDamage.Weapon.FIRE ? 0.6f : 1f;
        return w == BossDamage.Weapon.HOLY ? 1.2f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(0.95);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.RAVAGER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ENDER_DRAGON_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
