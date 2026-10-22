package com.andrey.litrpg.entity.raid;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Хронофаг, Пожиратель Времени (ур. 60). Идеи: «разбей часы» и «время вспять».
 * Пока целы песочные часы (узлы силы), каждые 30 с он откатывает себя на 10 с назад — урон за это время пропадает.
 * «Петля»: запоминает, где все стоят, и через 5 с возвращает туда — встаньте в безопасном месте заранее.
 * Песок времени (полосы), старение, тяжёлые стрелки часов по танку.
 */
public class ChronophageEntity extends BossBase {
    private final Deque<Float> history = new ArrayDeque<>();
    private final Map<UUID, Vec3> loop = new HashMap<>();
    private int loopTicks;

    public ChronophageEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW);
        skill("Стрелки часов", 110, 16, 1).first(40)
                .when(t -> distanceTo(t) < 6)
                .begin(t -> cone(position(), dirTo(t), 6, 45, 16, 1.05f).color(Telegraph.GOLD).then(e -> BossDamage.sunder(e, 2)));
        skill("Песок времени", 200, 24, 2).first(90)
                .begin(t -> {
                    for (int i = 0; i < 4; i++) {
                        double a = Math.PI / 2 * i + random.nextDouble() * 0.6;
                        line(position(), new Vec3(Math.cos(a), 0, Math.sin(a)), 28, 1.5, 24, 0.75f).color(Telegraph.GOLD)
                                .then(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3)));
                    }
                    sound(SoundEvents.SAND_BREAK, 2f, 0.5f);
                });
        skill("Ускоренное старение", 300, 16, 1).first(160)
                .act(t -> {
                    LivingEntity o = randomOther();
                    o.addEffect(new MobEffectInstance(MobEffects.WITHER, 160, 2));
                    o.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 160, 2));
                    tip(o, "Вы стареете на глазах — нужен лекарь!", ChatFormatting.GOLD);
                });
        skill("Петля", 700, 10, 2).first(260).unstoppable()
                .act(t -> {
                    loop.clear();
                    for (ServerPlayer p : players()) loop.put(p.getUUID(), p.position());
                    loopTicks = 100;
                    for (Vec3 v : loop.values()) circle(v, 1.2, 100, 0).color(Telegraph.GOLD);
                    shoutAll("Петля! Через 5 с все вернутся туда, где стоят сейчас. Если под вами опасность — бегите сразу после возврата!");
                    sound(SoundEvents.BEACON_POWER_SELECT, 2f, 0.6f);
                });
        skill("Остановка времени", 500, 30, 2).fromPhase(1).first(120)
                .act(t -> {
                    for (LivingEntity e : fighters()) {
                        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                        circle(e.position(), 3, 50, 0.6f).color(Telegraph.GOLD);
                    }
                    shoutAll("Время замерло — круги взорвутся, как только оно пойдёт снова!");
                });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 60; }
    @Override public int getXpReward() { return 10000; }

    @Override
    protected float toughness() {
        return 3.6f;
    }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f};
    }

    @Override
    protected float powerMult() {
        return phase > 0 ? 1.2f : 1f;
    }

    @Override
    protected List<String> intro() {
        return List.of("Песочные часы (узлы времени): пока целы, каждые 30 с Хронофаг откатывает свои раны на 10 с назад. Разбейте их!",
                "«Петля» — через 5 с вернёт всех на прежние места. «Песок времени» — полосы от него, уходите вбок.",
                "«Ускоренное старение» — лекарям сразу лечить помеченного.");
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.MAGIC ? 0.85f : 1f;
    }

    @Override
    protected void bossTick() {
        if (getTarget() == null) return;
        if (fightTicks % 20 == 0) {
            history.addLast(getHealth());
            while (history.size() > 10) history.removeFirst();
        }
        if (fightTicks > 0 && fightTicks % 600 == 0 && relicsAlive() > 0 && !history.isEmpty()) {
            float back = history.peekFirst();
            if (back > getHealth()) {
                setHealth(back);
                shout("Это... уже было.");
                shoutAll("Хронофаг откатил время — урон за 10 с пропал. Разбейте песочные часы!");
                burst(ParticleTypes.END_ROD, position().add(0, 2, 0), 80, 1.5, 0.1);
                sound(SoundEvents.BELL_RESONATE, 2f, 0.6f);
            }
        }
        if (loopTicks > 0 && --loopTicks == 0) {
            for (ServerPlayer p : players()) {
                Vec3 v = loop.get(p.getUUID());
                if (v != null) {
                    p.teleportTo(v.x, v.y, v.z);
                    burst(ParticleTypes.REVERSE_PORTAL, v.add(0, 1, 0), 20, 0.4, 0.05);
                }
            }
            sound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.5f);
            for (int i = 0; i < 4; i++) circle(randomSpot(14), 4, 30, 0.7f).color(Telegraph.GOLD);
        }
    }

    @Override
    protected void onRelicBroken(int alive) {
        shoutAll(alive > 0 ? "Песочные часы разбиты. Осталось: " + alive : "Все часы разбиты — время больше не откатится!");
        stagger(60, null);
    }

    @Override
    protected void onPhase(int phase) {
        shout("Ваше время... истекает.");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 1));
    }

    @Override
    protected void onReset() {
        history.clear();
        loop.clear();
        loopTicks = 0;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.05);
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.AMETHYST_BLOCK_CHIME; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.GLASS_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GLASS_BREAK; }
    @Override public float getVoicePitch() { return 0.5f; }
}
