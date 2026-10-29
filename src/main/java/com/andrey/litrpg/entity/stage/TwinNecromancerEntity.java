package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossProjectile;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Морра и Весса, сёстры-некромантки (этап 1, ур. 7). Два хранителя в одном бою.
 * Морра (юг) — повелительница костей: руки из-под земли, подъём мёртвых, саван-кокон, тёмные стрелы.
 * Весса (север) — кровавая ведьма: кровавая связь (вытягивает жизнь), кровавый дождь, жатва.
 * Вместе: каждые 35 с меняются местами; связь сестёр — кто ранен сильнее, получает вдвое меньше урона (бейте ровно);
 * павшую сестру выжившая поднимает за 10 с (прерывание оттягивает) — убейте обеих почти разом.
 * С половины здоровья — «Река душ»: огненные полосы поперёк зала.
 */
public class TwinNecromancerEntity extends StageBoss {
    private final boolean morra;
    private UUID partner;
    private boolean downed;
    private int resTicks;
    private int swapTicks = 700;
    private long resLock;

    public TwinNecromancerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, type == ModEntities.BOSSES.get("twin_vessa").get() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.PURPLE);
        this.morra = type != ModEntities.BOSSES.get("twin_vessa").get();
        if (morra) {
            skill("Костяные руки", 200, 24, 2).first(80)
                    .begin(t -> {
                        List<ServerPlayer> ps = new ArrayList<>(players());
                        java.util.Collections.shuffle(ps, new java.util.Random(random.nextLong()));
                        for (int i = 0; i < Math.min(3, ps.size()); i++) {
                            circle(ps.get(i).position(), 2.6, 24, 0.45f).color(Telegraph.PURPLE)
                                    .then(e -> e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5)));
                        }
                    })
                    .act(t -> sound(SoundEvents.SKELETON_HURT, 2f, 0.4f));
            skill("Подъём мёртвых", 560, 40, 2).first(200)
                    .act(t -> {
                        adds(EntityType.ZOMBIE, position(), 3, 6, MobLevels.Rank.NORMAL, null);
                        shoutAll("Морра поднимает мёртвых — прервите её в следующий раз!");
                        fxRing(position(), 1, 8, 16, 0xC090FF90);
                    });
            skill("Саван", 420, 20, 1).first(260)
                    .act(t -> {
                        LivingEntity v = randomOther();
                        if (v == null) return;
                        entomb(v, Blocks.COBWEB.defaultBlockState(), 160, 0.07f, true);
                        Fx.shield(level(), v, 1.4, 160, 0x90E8E8E8);
                        shoutAll(v.getName().getString() + " в саване — разрубите паутину!");
                    });
            skill("Тёмная стрела", 60, 10, 1).first(30).silent()
                    .act(t -> {
                        shoot(BossProjectile.Kind.SHADOW, t, 0.4f, 1.2f, 1f);
                        Fx.spiral(level(), this, 0.8, 2.4, 1.5, 10, 0xC08A30E0);
                    });
        } else {
            skill("Кровавая связь", 360, 16, 1).first(120)
                    .act(t -> {
                        LivingEntity v = randomOther();
                        if (v == null) return;
                        drainTarget = v.getUUID();
                        drainTicks = 160;
                        Fx.tether(level(), this, v, 0.35, 160, 0xE0D01020);
                        tip(v, "Весса пьёт вашу кровь — отбегите дальше 14 блоков!", ChatFormatting.RED);
                    });
            skill("Кровавый дождь", 260, 28, 2).first(80)
                    .begin(t -> {
                        for (int i = 0; i < 5; i++) circle(randomSpot(15), 3.2, 28, 0.55f).color(Telegraph.RED);
                    })
                    .act(t -> sound(SoundEvents.WEEPING_VINES_BREAK, 2f, 0.5f));
            skill("Жатва", 140, 16, 1).first(40)
                    .when(t -> distanceTo(t) < 9)
                    .begin(t -> cone(position(), dirTo(t), 9, 40, 16, 0.8f).color(Telegraph.RED))
                    .act(t -> fxSlash(dirTo(t), 8, 90, -20, 0xE0E02040));
        }
        skill("Река душ", 500, 40, 2).fromPhase(1).first(100).unstoppable()
                .begin(t -> {
                    Vec3 c = arena();
                    // полосы поперёк зала (вдоль x), через 4 блока; одна из трёх — безопасная
                    int safe = random.nextInt(3);
                    for (int i = -3; i <= 3; i++) {
                        if (Math.floorMod(i, 3) == safe) continue;
                        Vec3 from = new Vec3(c.x - 18, c.y, c.z + i * 4.5);
                        line(from, new Vec3(1, 0, 0), 36, 2.0, 40, 0.65f).color(Telegraph.BLUE);
                    }
                    shoutAll("Река душ выходит из берегов — встаньте на чистую полосу!");
                })
                .act(t -> sound(SoundEvents.SOUL_ESCAPE, 3f, 0.5f));
    }

    private UUID drainTarget;
    private int drainTicks;

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 500.0)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 7; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 4300; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f};
    }

    @Override
    protected List<String> intro() {
        if (!morra) return List.of();
        return List.of("Две сестры — бейте их ровно: та, что ранена сильнее, получает вдвое меньше урона.",
                "Павшую сестру выжившая поднимет за 10 с — прерывания оттягивают обряд. Убейте обеих почти разом.",
                "Морра — кости и мёртвые; Весса — кровь. Каждые 35 с они меняются местами.");
    }

    // ---------------- сестра ----------------

    /** Волны зовёт только одна из пары, иначе их было бы две разом. */
    @Override
    protected int waveTheme() {
        return morra ? super.waveTheme() : 0;
    }

    private TwinNecromancerEntity sister() {
        if (partner == null || !(level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(partner);
        return e instanceof TwinNecromancerEntity t && t.isAlive() ? t : null;
    }

    @Override
    protected void onArena() {
        // Морра призывает Вессу на северный помост
        if (!morra || partner != null || !(level() instanceof ServerLevel sl)) return;
        com.andrey.litrpg.util.Scheduler.later(2, () -> {
            if (partner != null || !isAlive()) return;
            TwinNecromancerEntity v = (TwinNecromancerEntity) ModEntities.BOSSES.get("twin_vessa").get().create(sl);
            if (v == null) return;
            Vec3 n = point("north");
            v.moveTo(n.x, n.y, n.z, 180, 0);
            v.setArena(arenaBox, net.minecraft.core.BlockPos.containing(n), arenaExtra);
            v.scaleForParty(partySize);
            v.partner = getUUID();
            partner = v.getUUID();
            sl.addFreshEntity(v);
        });
    }

    @Override
    protected void bossTick() {
        TwinNecromancerEntity sis = sister();
        // общая цель
        if (sis != null && getTarget() == null && sis.getTarget() != null) setTarget(sis.getTarget());
        if (downed) {
            getNavigation().stop();
            if (tickCount % 10 == 0) burst(ParticleTypes.SOUL, position().add(0, 0.5, 0), 6, 0.4, 0.02);
            return;
        }
        // обряд воскрешения павшей сестры
        if (sis != null && sis.downed) {
            getNavigation().stop();
            if (resTicks > 0 && --resTicks % 20 == 0) {
                Fx.tether(level(), this, sis, 0.3, 22, 0xD0A0FFA0);
                tipAll("Обряд воскрешения: " + resTicks / 20 + " с — прервите или добейте!");
            }
            if (resTicks <= 0) {
                sis.rise();
                shoutAll(getName().getString() + " поднимает сестру!");
            }
            return;
        }
        // кровавая связь
        if (drainTicks > 0 && --drainTicks % 20 == 0 && level() instanceof ServerLevel sl && sl.getEntity(drainTarget) instanceof LivingEntity v) {
            if (v.isAlive() && v.distanceTo(this) <= 14) {
                hit(v, 0.12f);
                heal(getMaxHealth() * 0.01f);
                if (sis != null) sis.heal(sis.getMaxHealth() * 0.01f);
            } else drainTicks = 0;
        }
        // обмен местами
        if (morra && sis != null && getTarget() != null && --swapTicks <= 0) {
            swapTicks = 700;
            Vec3 a = position(), b = sis.position();
            donut(a, 0.1, 4, 1, 0.3f).color(Telegraph.PURPLE);
            teleportTo(b.x, b.y, b.z);
            sis.teleportTo(a.x, a.y, a.z);
            fxPillar(a, 1.2, 5, 14, 0xD0A040FF);
            fxPillar(b, 1.2, 5, 14, 0xD0E02040);
            sound(SoundEvents.ENDERMAN_TELEPORT, 2f, 0.6f);
            shoutAll("Сёстры меняются местами!");
        }
    }

    private void tipAll(String text) {
        for (ServerPlayer p : players()) p.displayClientMessage(net.minecraft.network.chat.Component.literal(text).withStyle(ChatFormatting.GOLD), true);
    }

    /** Связь сестёр: та, что ранена сильнее, получает меньше. */
    @Override
    protected float powerMult() {
        return downed ? 0f : 1f;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        TwinNecromancerEntity sis = sister();
        if (sis != null && !sis.downed && getHealth() / getMaxHealth() < sis.getHealth() / sis.getMaxHealth() - 0.2f) {
            amount *= 0.5f;
            if (tickCount % 40 == 0) tip(source.getEntity(), "Связь сестёр: бейте ту, что здоровее!", ChatFormatting.GRAY);
        }
        super.actuallyHurt(source, amount);
    }

    @Override
    protected boolean onLethal(DamageSource src) {
        TwinNecromancerEntity sis = sister();
        if (sis != null && !sis.downed) {
            // пала первой: лежит, пока сестра читает обряд
            downed = true;
            immune = "Павшая сестра — добейте вторую, пока идёт обряд!";
            setHealth(1f);
            sis.resTicks = 200;
            fxPillar(position(), 1, 4, 20, 0xD0808080);
            shoutAll(getName().getString() + " пала! " + sis.getName().getString() + " начинает обряд воскрешения (10 с).");
            return true;
        }
        // вторая пала, пока первая лежит — обе гибнут
        if (sis != null && sis.downed) {
            sis.downed = false;
            sis.immune = null;
            if (sis.morra) {
                com.andrey.litrpg.util.Scheduler.later(1, () -> sis.kill());
            } else sis.discard();
        }
        return false;
    }

    private void rise() {
        downed = false;
        immune = null;
        setHealth(getMaxHealth() * 0.4f);
        fxPillar(position(), 1.5, 6, 20, 0xD0A0FFA0);
        sound(SoundEvents.ZOMBIE_VILLAGER_CURE, 2f, 0.7f);
    }

    /** Прерывание обряда воскрешения: +3 с (не чаще раза в 6 с). */
    @Override
    public boolean interrupt(Entity by) {
        TwinNecromancerEntity sis = sister();
        if (resTicks > 0 && sis != null && sis.downed && level().getGameTime() >= resLock) {
            resTicks += 60;
            resLock = level().getGameTime() + 120;
            shoutAll((by == null ? "Кто-то" : by.getName().getString()) + " сбивает обряд: +3 с");
            fxRing(position(), 0.5, 3, 10, 0xF0FFFFFF);
            return true;
        }
        return super.interrupt(by);
    }

    @Override
    protected boolean canCast() {
        TwinNecromancerEntity sis = sister();
        return super.canCast() && !downed && (sis == null || !sis.downed);
    }

    @Override
    protected void onPhase(int phase) {
        shout(morra ? "Сестра, они смеют…" : "Кровь за кровь!");
    }

    @Override
    protected void onReset() {
        super.onReset();
        downed = false;
        immune = null;
        resTicks = 0;
        drainTicks = 0;
        swapTicks = 700;
        TwinNecromancerEntity sis = sister();
        if (sis != null && morra) {
            sis.resetFightPublic();
        }
    }

    void resetFightPublic() {
        if (getHealth() < getMaxHealth() || downed) resetFight();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (partner != null) tag.putUUID("Partner", partner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Partner")) partner = tag.getUUID("Partner");
    }

    @Override
    public void remove(RemovalReason reason) {
        // Весса уходит вместе с Моррой (сброс этапа)
        if (morra && !level().isClientSide && reason.shouldDestroy()) {
            TwinNecromancerEntity sis = sister();
            if (sis != null) sis.discard();
        }
        super.remove(reason);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof TwinNecromancerEntity || super.isAlliedTo(other);
    }

    @Override
    protected void registerGoals() {
        meleeGoals(0.9);
    }

    @Override protected SoundEvent getAmbientSound() { return morra ? SoundEvents.EVOKER_AMBIENT : SoundEvents.WITCH_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return morra ? SoundEvents.EVOKER_HURT : SoundEvents.WITCH_HURT; }
    @Override protected SoundEvent getDeathSound() { return morra ? SoundEvents.EVOKER_DEATH : SoundEvents.WITCH_DEATH; }
}
