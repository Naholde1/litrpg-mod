package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Хронофаг (этап 5, ур. 44). Часовой зал: циферблат на полу, четверо песочных часов (узлы силы).
 * <ul>
 *   <li>«Стрелки» — две полосы-стрелки через весь зал крестом, следующий раз — повёрнуты на 45°;</li>
 *   <li>«Стазис» — двое застывают во времени (почти не двигаются, урон по ним идёт позже — разом);</li>
 *   <li>«Секунды» — шары времени в каждого; с 50% — «Эхо прошлого»: тени-подручные;</li>
 *   <li>«ОТКАТ» — каждые 40 с (с 25% — каждые 25 с) Хронофаг 8 с крутит время назад: если не разбить
 *       хоть одни песочные часы — он вернёт себе здоровье, а всех вернёт туда, где они стояли. Разбитые часы
 *       восстанавливаются через 30 с.</li>
 * </ul>
 */
public class ChronophageEntity extends StageBoss {
    private int rewindTimer;
    private int rewindLeft;
    private float rewindHp;
    private int lastRelics = -1;
    private boolean diagonal;
    private final Map<UUID, Vec3> anchors = new HashMap<>();
    private final Map<BlockPos, Long> restoreAt = new HashMap<>();

    public ChronophageEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        skill("Удар маятника", 100, 14, 1).first(30)
                .when(t -> distanceTo(t) < 7)
                .begin(t -> cone(position(), dirTo(t), 7, 60, 14, 1.1f).color(Telegraph.GOLD).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> fxSlash(dirTo(t), 6, 140, 0, 0xE0FFE080));
        skill("Стрелки", 260, 36, 2).first(90)
                .begin(t -> {
                    Vec3 c = arena();
                    double a0 = diagonal ? Math.PI / 4 : 0;
                    diagonal = !diagonal;
                    for (int k = 0; k < 2; k++) {
                        double a = a0 + k * Math.PI / 2;
                        Vec3 d = new Vec3(Math.cos(a), 0, Math.sin(a));
                        line(c.subtract(d.scale(22)), d, 44, 2.2, 36, 1.2f).color(Telegraph.GOLD);
                    }
                    shoutAll("Стрелки часов! Уходите с крестовины.");
                })
                .act(t -> sound(SoundEvents.BELL_BLOCK, 3f, 0.5f));
        skill("Стазис", 340, 20, 2).first(150)
                .act(t -> {
                    List<ServerPlayer> ps = new java.util.ArrayList<>(players());
                    java.util.Collections.shuffle(ps, new java.util.Random(random.nextLong()));
                    for (int i = 0; i < Math.min(2, ps.size()); i++) {
                        ServerPlayer p = ps.get(i);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 6));
                        p.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 3));
                        Fx.shield(level(), p, 1.2, 100, 0xA0FFE080);
                        com.andrey.litrpg.util.Scheduler.later(100, () -> {
                            if (p.isAlive() && isAlive()) {
                                hit(p, 0.6f);
                                Fx.ring(level(), p.position(), 0.3, 3, 10, 0xF0FFE080);
                            }
                        });
                        tip(p, "Стазис! Через 5 с время догонит вас — лекарь, готовьтесь.", ChatFormatting.GOLD);
                    }
                });
        skill("Секунды", 200, 18, 1).first(60)
                .act(t -> {
                    for (ServerPlayer p : players()) shoot(BossProjectile.Kind.CRYSTAL, p, 0.4f, 0.8f, 3f);
                    sound(SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.6f);
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 44; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 100000; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f, 0.25f};
    }

    @Override
    protected List<String> intro() {
        return List.of("«ОТКАТ»: 8 с Хронофаг крутит время назад. Разбейте хоть одни песочные часы — иначе он вернёт здоровье, а вас — на прежние места.",
                "Разбитые часы восстанавливаются через 30 с — распределите, кто бьёт какие.",
                "«Стрелки» — крест через весь зал. «Стазис» — урон догонит застывших через 5 с.");
    }

    // ---------------- откат ----------------

    private void beginRewind() {
        rewindLeft = 160;
        rewindHp = getHealth();
        anchors.clear();
        for (ServerPlayer p : players()) anchors.put(p.getUUID(), p.position());
        for (BlockPos p : liveRelics()) Fx.pillar(level(), Vec3.atBottomCenterOf(p), 0.8, 6, 160, 0xC0FFE080);
        shoutAll("ОТКАТ ВРЕМЕНИ! Разбейте песочные часы за 8 секунд!");
        sound(SoundEvents.BEACON_ACTIVATE, 3f, 0.5f);
    }

    private void finishRewind() {
        rewindLeft = 0;
        float back = Math.min(getMaxHealth(), Math.max(getHealth(), rewindHp + getMaxHealth() * 0.05f));
        setHealth(back);
        if (level() instanceof ServerLevel sl) {
            for (var e : anchors.entrySet()) {
                if (sl.getEntity(e.getKey()) instanceof ServerPlayer p && p.isAlive()) {
                    Fx.beam(level(), p.position().add(0, 1, 0), e.getValue().add(0, 1, 0), 0.3, 12, 0xC0FFE080);
                    p.teleportTo(e.getValue().x, e.getValue().y, e.getValue().z);
                    p.fallDistance = 0;
                }
            }
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2, getZ(), 80, 3, 2, 3, 0.1);
        }
        anchors.clear();
        fxRing(arena(), 22, 0.5, 20, 0xE0FFE080);
        shoutAll("Время повернулось вспять — Хронофаг вернул себе силы!");
        sound(SoundEvents.ZOMBIE_VILLAGER_CURE, 3f, 0.5f);
    }

    private void cancelRewind() {
        rewindLeft = 0;
        anchors.clear();
        stagger(60, "Песочные часы разбиты — откат сорван! Хронофаг оглушён.");
        sound(SoundEvents.GLASS_BREAK, 3f, 0.6f);
    }

    @Override
    protected void bossTick() {
        long now = level().getGameTime();
        // песочные часы: восстановление через 30 с
        for (var e : relics.entrySet()) {
            boolean alive = level().getBlockState(e.getKey()).is(e.getValue().getBlock());
            if (alive) continue;
            Long due = restoreAt.get(e.getKey());
            if (due == null) restoreAt.put(e.getKey(), now + 600);
            else if (now >= due) {
                level().setBlock(e.getKey(), e.getValue(), 3);
                restoreAt.remove(e.getKey());
                Fx.pillar(level(), Vec3.atBottomCenterOf(e.getKey()), 0.6, 3, 14, 0xC0FFE080);
            }
        }
        int alive = liveRelics().size();
        if (rewindLeft > 0 && lastRelics >= 0 && alive < lastRelics) cancelRewind();
        lastRelics = alive;
        if (fightTicks <= 0) return;
        if (rewindLeft > 0) {
            getNavigation().stop();
            if (--rewindLeft % 20 == 0) {
                for (ServerPlayer p : players()) p.displayClientMessage(Component.literal("Откат: " + rewindLeft / 20 + " с — бейте песочные часы!")
                        .withStyle(ChatFormatting.GOLD), true);
                fxRing(position(), 0.5, 6, 14, 0xA0FFE080);
            }
            if (rewindLeft == 0) finishRewind();
            return;
        }
        rewindTimer++;
        if (rewindTimer >= (phase >= 2 ? 500 : 800) && !isCasting()) {
            rewindTimer = 0;
            beginRewind();
        }
    }

    @Override
    protected boolean canCast() {
        return rewindLeft <= 0 && super.canCast();
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            shout("Ваше прошлое — моё.");
            var w = ModEntities.WILD.get("barrow_wight");
            if (w != null) adds(w.get(), arena(), 3, 5, MobLevels.Rank.ELITE, m -> m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0)));
            shoutAll("Эхо прошлого! Тени выходят из часов.");
        } else {
            shout("Время… кончается. Для вас.");
            addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        rewindLeft = 0;
        rewindTimer = 0;
        anchors.clear();
        restoreAt.clear();
        lastRelics = -1;
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.BLUNT ? 1.3f : w == BossDamage.Weapon.MAGIC ? 0.85f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_RESONATE; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.AMETHYST_BLOCK_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BEACON_DEACTIVATE; }
    @Override public float getVoicePitch() { return 0.6f; }
}
