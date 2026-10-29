package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import java.util.List;

/**
 * Ведьма Тьмы (этап 2, финал, ур. 20). Хижина на сваях посреди болотной поляны; под ней — Изнанка.
 * <ol>
 *   <li>Поляна (100–60%): «Порча» на игрока (долгий урон, снимается лекарем — нет: только переживается),
 *       «Котёл» — бросает зелья (лужи), «Вороньё» — стая летучих мышей, «Метла» — полёт по прямой через поляну.</li>
 *   <li>Изнанка (60–30%): вся группа — в тёмном мире под хижиной. Вдали от огней (4 блока) тьма жжёт всё сильнее,
 *       огни гаснут по одному каждые 20 с, тени охотятся. Довести ведьму до 30% — и вас выбросит обратно.</li>
 *   <li>Поляна (<30%): «Полнолуние» — всё сразу, тьма пульсирует кольцами.</li>
 * </ol>
 */
public class DarkWitchEntity extends StageBoss {
    /** 0 — поляна, 1 — Изнанка, 2 — снова поляна. */
    private int part;
    private int underTicks;
    private int dousedWells;

    public DarkWitchEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Порча", 260, 14, 1).first(60)
                .act(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    v.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
                    Fx.mark(level(), v, 0.5, 1, 200, 0xD08A30E0);
                    Fx.tether(level(), this, v, 0.15, 22, 0xC08A30E0);
                    corrupted = v.getUUID();
                    corruptTicks = 200;
                    tip(v, "Порча! Отбегите от ведьмы дальше 12 блоков — связь порвётся.", ChatFormatting.DARK_PURPLE);
                });
        skill("Котёл", 200, 20, 2).first(100)
                .begin(t -> {
                    for (ServerPlayer p : players()) {
                        Vec3 at = p.position();
                        circle(at, 2.8, 20, 0.4f).color(Telegraph.PURPLE).fx(c -> hazard(c, 2.6, 20 * 10, 0.1f, 0xA060FF40, null));
                    }
                })
                .act(t -> {
                    for (ServerPlayer p : players()) shootAt(BossProjectile.Kind.POISON, p.position(), 0.1f, 1.0f, 3f);
                    sound(SoundEvents.WITCH_THROW, 2f, 0.7f);
                });
        skill("Вороньё", 520, 24, 2).first(200)
                .act(t -> {
                    var bat = ModEntities.WILD.get("bloodbat");
                    if (bat != null) adds(bat.get(), position().add(0, 2, 0), 4, 8, MobLevels.Rank.NORMAL, null);
                    shoutAll("Вороньё ведьмы! Сбейте их — они пьют кровь.");
                });
        skill("Метла", 340, 24, 1).first(160)
                .begin(t -> {
                    Vec3 dir = dirTo(t);
                    broomTo = safeSpot(position().add(dir.scale(24)));
                    line(position(), dir, broomTo.subtract(position()).horizontalDistance() + 2, 1.8, 24, 0.8f)
                            .color(Telegraph.PURPLE);
                })
                .act(t -> {
                    if (broomTo == null) return;
                    fxBeam(chest(), broomTo.add(0, 1, 0), 0.6, 10, 0xD08A30E0);
                    teleportTo(broomTo.x, broomTo.y, broomTo.z);
                    sound(SoundEvents.PHANTOM_FLAP, 2f, 0.6f);
                });
        skill("Полнолуние", 460, 30, 2).fromPhase(2).first(60).unstoppable()
                .begin(t -> {
                    Vec3 c = position();
                    boolean inner = random.nextBoolean();
                    if (inner) circle(c, 8, 30, 0.8f).color(Telegraph.PURPLE);
                    else donut(c, 8, 22, 30, 0.8f).color(Telegraph.PURPLE);
                    shoutAll(inner ? "Тьма у ведьмы — отойдите дальше 8 блоков!" : "Тьма вокруг — бегите к ведьме!");
                })
                .act(t -> fxRing(position(), 1, 22, 16, 0xE08A30E0));
    }

    private Vec3 broomTo;
    /** Кого держит «Порча» и сколько ей осталось: связь рвётся, если отбежать. */
    private java.util.UUID corrupted;
    private int corruptTicks;

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 800.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 20; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 24000; }
    @Override protected int enrageTicks() { return 20 * 60 * 7; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.6f, 0.3f};
    }

    @Override
    protected List<String> intro() {
        return List.of("С 60% ведьма утащит вас в Изнанку. Там держитесь у огней — тьма жжёт; огни гаснут по одному.",
                "Лужи из котла держатся 10 с. «Метла» — уходите с полосы.",
                "Под конец — «Полнолуние»: тьма то у ведьмы, то вокруг неё.");
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) enterUnder();
        else if (phase == 2 && part == 1) leaveUnder();
    }

    // ---------------- Изнанка ----------------

    private void enterUnder() {
        part = 1;
        underTicks = 0;
        dousedWells = 0;
        lightWells(true);
        shout("Пойдёмте… вниз. Туда, где темно.");
        List<Vec3> spots = new ArrayList<>();
        for (Vec3 w : points("wells")) spots.add(w.add(1.2, 0, 0));
        moveGroup(spots, arenaBox.aabb());
        Vec3 u = point("under");
        teleportTo(u.x, u.y, u.z);
        setAltArena(underBox(), u);
        var panther = ModEntities.WILD.get("shadow_panther");
        if (panther != null) adds(panther.get(), u, 2, 4, MobLevels.Rank.NORMAL, null);
        shoutAll("Изнанка! Держитесь у огней — во тьме вы горите. Огни гаснут каждые 20 с.");
    }

    private void leaveUnder() {
        part = 2;
        shout("Свет… слишком много света…");
        Vec3 hut = point("hut");
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 5; i++) spots.add(hut.add(-6 + i * 3, -3, 14));
        moveGroup(spots, underBox());
        clearAltArena();
        teleportTo(hut.x, hut.y, hut.z);
        clearAdds();
        lightWells(true);
        stagger(60, "Ведьму выбросило из Изнанки — она оглушена!");
    }

    private AABB underBox() {
        return arenaExtra.has("under_box") ? box(arenaExtra.getAsJsonArray("under_box"))
                : new AABB(point("under"), point("under")).inflate(18);
    }

    private static AABB box(com.google.gson.JsonArray a) {
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    private void lightWells(boolean lit) {
        for (Vec3 w : points("wells")) {
            BlockPos p = BlockPos.containing(w);
            BlockState st = level().getBlockState(p);
            if (st.getBlock() instanceof CampfireBlock) level().setBlock(p, st.setValue(CampfireBlock.LIT, lit), 3);
        }
    }

    private void underTick() {
        underTicks++;
        List<Vec3> wells = points("wells");
        if (underTicks % 400 == 0 && dousedWells < wells.size() - 1) {
            Vec3 w = wells.get(dousedWells++);
            BlockPos p = BlockPos.containing(w);
            BlockState st = level().getBlockState(p);
            if (st.getBlock() instanceof CampfireBlock) level().setBlock(p, st.setValue(CampfireBlock.LIT, false), 3);
            fxPillar(w, 1, 4, 20, 0xD0202020);
            shoutAll("Огонь погас! Осталось огней: " + (wells.size() - dousedWells));
            sound(SoundEvents.FIRE_EXTINGUISH, 2f, 0.6f);
        }
        if (underTicks % 20 != 0) return;
        float dark = 0.06f + 0.012f * (underTicks / 200);
        for (ServerPlayer p : players()) {
            boolean safe = false;
            for (int i = dousedWells; i < wells.size(); i++) if (wells.get(i).distanceTo(p.position()) < 4.5) safe = true;
            if (safe) {
                Fx.ring(level(), p.position(), 0.2, 1.2, 10, 0x80FFD080);
                continue;
            }
            hit(p, dark);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        }
        if (underTicks % 300 == 150) {
            var panther = ModEntities.WILD.get("shadow_panther");
            if (panther != null) adds(panther.get(), point("under"), 1, 4, MobLevels.Rank.NORMAL, null);
        }
    }

    @Override
    protected void bossTick() {
        if (part == 1) underTick();
        corruptTick();
    }

    /**
     * «Порча» — связь, а не просто проклятие: пока жертва рядом с ведьмой, иссушение тянет
     * из неё жизнь, но стоит отбежать на 12 блоков — связь рвётся и порча уходит. Раньше
     * нить была только картинкой, и разорвать её было нельзя ничем.
     */
    private void corruptTick() {
        if (corruptTicks <= 0 || corrupted == null) return;
        LivingEntity v = level() instanceof net.minecraft.server.level.ServerLevel sl
                && sl.getEntity(corrupted) instanceof LivingEntity le ? le : null;
        if (v == null || !v.isAlive()) {
            corrupted = null;
            corruptTicks = 0;
            return;
        }
        if (v.distanceTo(this) > 12) {
            v.removeEffect(MobEffects.WITHER);
            tip(v, "Связь порвана — порча ушла.", ChatFormatting.GREEN);
            corrupted = null;
            corruptTicks = 0;
            return;
        }
        corruptTicks--;
        if (corruptTicks % 20 == 0) Fx.tether(level(), this, v, 0.15, 22, 0xC08A30E0);
        if (corruptTicks <= 0) {
            v.removeEffect(MobEffects.WITHER);
            corrupted = null;
        }
    }

    @Override
    protected boolean leashed() {
        return part != 1 && super.leashed();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (arenaExtra.has("under_box") && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, box(arenaExtra.getAsJsonArray("under_box")),
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) if (!out.contains(p)) out.add(p);
        }
        return out;
    }

    @Override
    protected List<LivingEntity> fighters() {
        if (part == 1) return new ArrayList<>(players());
        return super.fighters();
    }

    @Override
    protected void onReset() {
        super.onReset();
        part = 0;
        lightWells(true);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.HOLY || w == BossDamage.Weapon.FIRE ? 1.3f : w == BossDamage.Weapon.MAGIC ? 0.85f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITCH_DEATH; }
    @Override public float getVoicePitch() { return 0.7f; }
}
