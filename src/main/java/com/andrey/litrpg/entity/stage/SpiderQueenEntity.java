package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Матка пауков (этап 2, ур. 14). Логово — пещера с высоким сводом, мешки с яйцами у стен.
 * <ul>
 *   <li>«Укус» — конус по танку с ядом; «Ядовитый плевок» — снаряд, на месте попадания ядовитая лужа;</li>
 *   <li>«Кокон» — заматывает игрока в паутину (союзники рубят);</li>
 *   <li>«Под свод» — взбирается на потолок (неуязвима), сыплет сети и детёнышей, через 12 с обрушивается на игрока;</li>
 *   <li>мешки с яйцами (узлы силы) — каждые 25 с из каждого целого вылупляется паук; разбитый мешок оглушает её;</li>
 *   <li>с 40%: пол затягивает паутиной, она быстрее.</li>
 * </ul>
 */
public class SpiderQueenEntity extends StageBoss {
    private int ceilingTicks;
    private Vec3 dropAt;

    public SpiderQueenEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Укус", 100, 14, 1).first(30)
                .when(t -> ceilingTicks <= 0 && distanceTo(t) < 7)
                .begin(t -> cone(position(), dirTo(t), 7, 40, 14, 0.9f).color(Telegraph.GREEN)
                        .then(e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1))))
                .act(t -> fxSlash(dirTo(t), 6, 60, 0, 0xE070E040));
        skill("Ядовитый плевок", 160, 16, 1).first(60)
                .when(t -> ceilingTicks <= 0)
                .act(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    shoot(BossProjectile.Kind.POISON, v, 0.35f, 1.1f, 2f);
                    Vec3 at = v.position();
                    com.andrey.litrpg.util.Scheduler.later(14, () -> hazard(at, 2.8, 20 * 14, 0.12f, 0xB070E040,
                            e -> e.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0))));
                });
        skill("Кокон", 380, 18, 2).first(200)
                .when(t -> ceilingTicks <= 0)
                .act(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    entomb(v, Blocks.COBWEB.defaultBlockState(), 180, 0.06f, true);
                    Fx.shield(level(), v, 1.4, 180, 0x90F0F0F0);
                    shoutAll(v.getName().getString() + " в коконе — разрубите паутину!");
                });
        skill("Под свод", 900, 20, 2).first(400).unstoppable()
                .when(t -> ceilingTicks <= 0)
                .act(t -> climb());
        skill("Паучий выводок", 520, 26, 2).first(260)
                .when(t -> ceilingTicks <= 0)
                .act(t -> {
                    adds(EntityType.CAVE_SPIDER, position(), 4, 10, MobLevels.Rank.NORMAL, null);
                    shoutAll("Выводок Матки! Танк — соберите их, бойцы — по площади.");
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 700.0)
                .add(Attributes.ATTACK_DAMAGE, 9.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 14; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 9000; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.4f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Разбейте мешки с яйцами у стен — каждые 25 с из целых вылупляются пауки.",
                "Когда Матка уходит под свод — расходитесь и следите за кругом: она обрушится на одного из вас.",
                "Ядовитые лужи держатся долго — не стойте в них. Коконы — рубите.");
    }

    // ---------------- под сводом ----------------

    private void climb() {
        Vec3 top = point("ceiling");
        ceilingTicks = 240;
        immune = "Матка под сводом — не достать!";
        setNoGravity(true);
        fxBeam(position().add(0, 1, 0), top, 0.2, 20, 0xC0E0E0E0);
        teleportTo(top.x, top.y, top.z);
        getNavigation().stop();
        shoutAll("Матка уходит под свод! Расходитесь — сеть и детёныши сверху.");
        sound(SoundEvents.SPIDER_AMBIENT, 2.5f, 0.4f);
    }

    @Override
    protected void bossTick() {
        if (ceilingTicks <= 0) {
            hatch();
            return;
        }
        ceilingTicks--;
        setDeltaMovement(Vec3.ZERO);
        Vec3 top = point("ceiling");
        if (position().distanceTo(top) > 1) teleportTo(top.x, top.y, top.z);
        if (ceilingTicks % 60 == 30) {
            for (ServerPlayer p : players()) {
                circle(p.position(), 2.4, 30, 0.35f).color(Telegraph.PURPLE).fx(c -> webPatch(c, 2));
            }
            fxBeam(position(), arena(), 0.15, 10, 0xA0F0F0F0);
        }
        if (ceilingTicks == 160) adds(EntityType.CAVE_SPIDER, arena(), 3, 10, MobLevels.Rank.NORMAL, null);
        if (ceilingTicks == 50) {
            LivingEntity v = randomOther();
            dropAt = v == null ? arena() : v.position();
            circle(dropAt, 5, 50, 1.1f).color(Telegraph.RED).follow(v, 18).then(e -> knockFrom(e, dropAt, 1.3, 0.5));
            shoutAll("Матка падает на " + (v == null ? "арену" : v.getName().getString()) + "! Все — прочь из круга!");
        }
        if (ceilingTicks == 0) {
            Vec3 at = zones.isEmpty() ? dropAt : zones.get(zones.size() - 1).center();
            if (at == null) at = arena();
            setNoGravity(false);
            immune = null;
            teleportTo(at.x, at.y, at.z);
            fxRing(at, 0.5, 6, 12, 0xF0E0E0E0);
            sound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.6f);
            stagger(40, null);
        }
    }

    /** Мешки с яйцами: каждые 25 с из каждого целого — паук. */
    private void hatch() {
        if (fightTicks <= 0 || fightTicks % 500 != 250) return;
        int n = 0;
        for (BlockPos p : liveRelics()) {
            Vec3 v = Vec3.atBottomCenterOf(p);
            adds(EntityType.CAVE_SPIDER, v, 1, 12, MobLevels.Rank.NORMAL, null);
            Fx.ring(level(), v, 0.2, 2, 10, 0xC0F0F0F0);
            n++;
        }
        if (n > 0) shoutAll("Из мешков вылупились пауки (" + n + "). Разбейте мешки!");
    }

    private void webPatch(Vec3 c, int r) {
        BlockPos b = BlockPos.containing(c);
        for (BlockPos p : BlockPos.betweenClosed(b.offset(-r, 0, -r), b.offset(r, 0, r))) {
            if (p.distSqr(b) <= r * r && random.nextFloat() < 0.6f) tempBlock(p.immutable(), Blocks.COBWEB.defaultBlockState(), 20 * 20);
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        stagger(50, alive > 0 ? "Мешок разбит — Матка в ярости и оглушена! Осталось: " + alive : "Все мешки разбиты!");
        burst(ParticleTypes.ITEM_SLIME, position().add(0, 1, 0), 20, 1, 0.1);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Мои дети… вы заплатите!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
        for (int i = 0; i < 8; i++) webPatch(randomSpot(15), 2);
        shoutAll("Пол затянуло паутиной — рубите её или обходите.");
    }

    @Override
    protected boolean canCast() {
        return ceilingTicks <= 0 && super.canCast();
    }

    @Override
    protected void onReset() {
        super.onReset();
        ceilingTicks = 0;
        setNoGravity(false);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FIRE ? 1.4f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.1);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
