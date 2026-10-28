package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Жрица глубин (этап 3, финал, ур. 30). Святилище морских богов; под ним — стеклянный купол на дне моря.
 * <ol>
 *   <li>Святилище (100–65%): «Хлыст прилива» (конус), «Сферы глубин» (самонаводящиеся шары по всем), «Око бури»
 *       (круги молний под каждым), «Печать моря» — общий круг: встаньте в него втроём, иначе удар по всем.</li>
 *   <li>Купол (65–30%): вся группа на дне моря под стеклом. Каждые 8 с стекло трескается — на полу остаются
 *       лужи-«протечки» (до конца фазы), места всё меньше; давление глубины жжёт всех понемногу и всё сильнее.</li>
 *   <li>Святилище (<30%): «Глаз шторма» — по всему залу шторм, безопасен лишь светлый круг; прислужницы-утопленницы.</li>
 * </ol>
 */
public class DeepPriestessEntity extends StageBoss {
    /** 0 — святилище, 1 — купол, 2 — снова святилище. */
    private int part;
    private int domeTicks;

    public DeepPriestessEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        skill("Хлыст прилива", 110, 16, 1).first(30)
                .when(t -> distanceTo(t) < 9)
                .begin(t -> cone(position(), dirTo(t), 9, 50, 16, 1.0f).color(Telegraph.BLUE).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> fxSlash(dirTo(t), 8, 100, 15, 0xE040C0E0));
        skill("Сферы глубин", 240, 20, 1).first(70)
                .act(t -> {
                    for (ServerPlayer p : players()) shoot(BossProjectile.Kind.CRYSTAL, p, 0.45f, 0.9f, 4f);
                    sound(SoundEvents.CONDUIT_ATTACK_TARGET, 2f, 0.7f);
                });
        skill("Око бури", 300, 30, 2).first(120)
                .begin(t -> {
                    for (ServerPlayer p : players()) circle(p.position(), 3, 30, 0.7f).color(Telegraph.GOLD);
                    shoutAll("Око бури! Разойдитесь — молнии бьют под каждым.");
                })
                .act(t -> sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, 1.2f));
        skill("Печать моря", 520, 50, 2).first(200).unstoppable()
                .begin(t -> {
                    LivingEntity v = randomOther();
                    Vec3 at = v == null ? arena() : v.position();
                    int need = Math.min(3, Math.max(1, players().size()));
                    circle(at, 3.2, 50, 2.4f).color(Telegraph.GOLD).soak(need);
                    shoutAll("Печать моря! Встаньте в круг вместе (" + need + "), иначе удар разорвёт одного.");
                })
                .act(t -> sound(SoundEvents.ELDER_GUARDIAN_CURSE, 2f, 0.8f));
        skill("Глаз шторма", 420, 50, 2).fromPhase(2).first(60).unstoppable()
                .begin(t -> {
                    Vec3 safe = randomSpot(14);
                    Fx.pillar(level(), safe, 4, 6, 50, 0x90FFF0A0);
                    donut(safe, 4, 40, 50, 1.6f).color(Telegraph.BLUE);
                    shoutAll("Глаз шторма! Бегите в светлый круг — всё остальное накроет буря.");
                })
                .act(t -> {
                    fxRing(arena(), 4, 30, 16, 0xC03060FF);
                    sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 30; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 25000; }
    @Override protected int enrageTicks() { return 20 * 60 * 7; }
    @Override public boolean canBreatheUnderwater() { return true; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.65f, 0.3f};
    }

    @Override
    protected List<String> intro() {
        return List.of("«Печать моря» — встаньте в круг вместе, она делит удар. «Око бури» — разойдитесь.",
                "С 65% Жрица утянет всех на дно под стеклянный купол: стекло трескается, протечки остаются — не стойте в них. Бейте быстро: давление растёт.",
                "Под конец — «Глаз шторма»: спасает только светлый круг.");
    }

    // ---------------- купол ----------------

    private void enterDome() {
        part = 1;
        domeTicks = 0;
        shout("Спустимся к богам. Туда, где нет воздуха.");
        moveGroup(points("dome_spots"), arenaBox.aabb());
        Vec3 c = point("dome");
        teleportTo(c.x, c.y, c.z);
        setAltArena(domeBox(), c);
        for (ServerPlayer p : players()) p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 20 * 3, 0, false, false));
        shoutAll("Дно моря! Протечки в стекле — не стойте под ними. Давление растёт — спешите!");
        sound(SoundEvents.AMBIENT_UNDERWATER_ENTER, 3f, 0.4f);
    }

    private void leaveDome() {
        part = 2;
        hazards.clear();
        shout("Нет… море не отпускает так просто!");
        Vec3 altar = point("altar");
        List<Vec3> spots = new ArrayList<>();
        for (int i = 0; i < 5; i++) spots.add(altar.add(-6 + i * 3, -1, 10));
        moveGroup(spots, domeBox());
        clearAltArena();
        teleportTo(altar.x, altar.y, altar.z);
        adds(EntityType.DROWNED, altar, 4, 6, MobLevels.Rank.ELITE, null);
        stagger(60, "Жрицу выбросило из глубин — она оглушена!");
    }

    private AABB domeBox() {
        if (!arenaExtra.has("dome_box")) return new AABB(point("dome"), point("dome")).inflate(16);
        var a = arenaExtra.getAsJsonArray("dome_box");
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    private void domeTick() {
        domeTicks++;
        Vec3 c = point("dome");
        if (domeTicks % 160 == 40) {
            int n = 2 + domeTicks / 600;
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, d = 2 + random.nextDouble() * 9;
                Vec3 at = new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
                circle(at, 2.5, 30, 0.6f).color(Telegraph.BLUE).fx(z -> {
                    hazard(z, 2.4, 20 * 120, 0.12f, 0xA03080FF, e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1)));
                    if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.FALLING_WATER, z.x, z.y + 8, z.z, 40, 1, 0.5, 1, 0);
                });
            }
            sound(SoundEvents.GLASS_BREAK, 2.5f, 0.6f);
            shoutAll("Стекло трескается!");
        }
        if (domeTicks % 20 == 0) {
            float press = 0.02f + 0.006f * (domeTicks / 200);
            for (ServerPlayer p : players()) {
                hit(p, press);
                if (domeTicks % 100 == 0) Fx.ring(level(), p.position(), 0.3, 1.5, 10, 0x803050FF);
            }
        }
    }

    @Override
    protected void bossTick() {
        if (part == 1) domeTick();
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) enterDome();
        else if (phase == 2 && part == 1) leaveDome();
    }

    @Override
    protected boolean leashed() {
        return part != 1 && super.leashed();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, domeBox(),
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
    public Vec3 arena() {
        return part == 1 ? point("dome") : super.arena();
    }

    @Override
    protected void onReset() {
        super.onReset();
        if (part == 1) {
            Vec3 altar = point("altar");
            List<Vec3> spots = new ArrayList<>();
            for (int i = 0; i < 5; i++) spots.add(altar.add(-6 + i * 3, -1, 10));
            moveGroup(spots, domeBox());
        }
        part = 0;
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FIRE ? 0.7f : w == BossDamage.Weapon.HOLY ? 1.2f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.CONDUIT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.DROWNED_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }
    @Override public float getVoicePitch() { return 0.9f; }
}
