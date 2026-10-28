package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Левиафан (этап 3, ур. 27). Огромный бассейн посреди пещеры, вокруг — кольцевая дорожка.
 * <ul>
 *   <li>Всплывает у одного из четырёх краёв бассейна и бьётся оттуда ~16 с: «Удар хвоста» (конус), «Струя» (полоса
 *       через всю пещеру), «Укус» (по танку);</li>
 *   <li>«Нырок» — уходит под воду (неуязвим), кусает плывущих, затем выныривает у другого края — круг на месте выхода подбрасывает;</li>
 *   <li>«Зов глубин» — тянет всех к воде; «Медузы» — подручные на дорожке;</li>
 *   <li>на 60% и 25% — «ОТЛИВ»: вода уходит, Левиафан лежит на дне беспомощный (урон ×1,6) 20 с.
 *       Прыгайте в чашу и бейте — но успейте выбраться: прилив возвращается волной.</li>
 * </ul>
 */
public class LeviathanEntity extends StageBoss {
    /** 0 — над водой у края, 1 — под водой, 2 — отлив (на дне). */
    private int state;
    private int stateTicks;
    private int spot;

    public LeviathanEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
        skill("Удар хвоста", 120, 18, 1).first(40)
                .when(t -> state == 0 && distanceTo(t) < 11)
                .begin(t -> cone(position(), dirTo(t), 11, 65, 18, 1.0f).color(Telegraph.BLUE)
                        .then(e -> knockFrom(e, position(), 1.4, 0.5)))
                .act(t -> {
                    fxSlash(dirTo(t), 10, 130, 0, 0xE050A0FF);
                    sound(SoundEvents.GENERIC_SPLASH, 3f, 0.5f);
                });
        skill("Укус", 90, 12, 1).first(20)
                .when(t -> state == 0 && distanceTo(t) < 8)
                .begin(t -> circle(t.position(), 1.8, 12, 1.4f).color(Telegraph.RED).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> sound(SoundEvents.EVOKER_FANGS_ATTACK, 2f, 0.4f));
        skill("Струя", 200, 26, 2).first(90)
                .when(t -> state == 0)
                .begin(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    Vec3 d = dirTo(v);
                    jetDir = d;
                    line(position(), d, 40, 1.6, 26, 0.9f).color(Telegraph.BLUE).then(e -> knockFrom(e, position(), 1.0, 0.3));
                })
                .act(t -> {
                    if (jetDir == null) return;
                    fxBeam(chest(), chest().add(jetDir.scale(40)), 1.4, 14, 0xE080D0FF);
                    sound(SoundEvents.ELDER_GUARDIAN_CURSE, 1.5f, 0.6f);
                });
        skill("Зов глубин", 520, 50, 2).first(220)
                .when(t -> state == 0)
                .begin(t -> shoutAll("Зов глубин тянет к воде — держитесь подальше от края бассейна!"))
                .during(left -> {
                    if (left % 2 != 0) return;
                    Vec3 c = poolCenter();
                    for (ServerPlayer p : players()) if (!p.isInWater()) pullTo(p, new Vec3(c.x, p.getY(), c.z), 0.07);
                    if (left % 8 == 0) fxRing(c, 18, 6, 10, 0xA03070FF);
                })
                .act(t -> {
                    for (ServerPlayer p : players()) if (p.isInWater()) {
                        hit(p, 0.8f);
                        tip(p, "Левиафан утаскивает тех, кто в воде!", ChatFormatting.DARK_AQUA);
                    }
                });
        skill("Медузы", 600, 20, 2).first(300).fromPhase(1)
                .when(t -> state == 0)
                .act(t -> {
                    var jelly = ModEntities.WILD.get("jelly");
                    if (jelly == null) return;
                    for (Vec3 p : points("emerge")) {
                        Vec3 out = p.add(p.subtract(poolCenter()).normalize().scale(4)).add(0, 2, 0);
                        adds(jelly.get(), out, 1, 6, MobLevels.Rank.NORMAL, null);
                    }
                    shoutAll("Медузы ползут на дорожку — их яд жжёт!");
                });
    }

    private Vec3 jetDir;

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0)
                .add(Attributes.ATTACK_DAMAGE, 13.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.ARMOR, 10.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override public int getBossLevel() { return 27; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 20000; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.6f, 0.25f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Левиафан всплывает у края бассейна — бейте его с дорожки. Когда ныряет — уходите от круга, где он вынырнет.",
                "Не плавайте: в воде он кусает. «Зов глубин» тянет к воде — упирайтесь.",
                "На 60% и 25% — ОТЛИВ: вода уйдёт, прыгайте в чашу и бейте, пока он беспомощен. Прилив вернётся через 20 с!");
    }

    private Vec3 poolCenter() {
        return point("center");
    }

    private Vec3 spotPos(int i) {
        List<Vec3> e = points("emerge");
        return e.isEmpty() ? arena() : e.get(Math.floorMod(i, e.size()));
    }

    // ---------------- движение ----------------

    @Override
    protected void onEngage() {
        state = 0;
        stateTicks = 0;
        spot = random.nextInt(4);
        Vec3 s = spotPos(spot);
        teleportTo(s.x, s.y, s.z);
        splash(s);
        shout("Кто тревожит глубину?!");
    }

    private void splash(Vec3 at) {
        fxRing(at, 0.5, 7, 14, 0xE0A0E0FF);
        fxPillar(at, 2, 6, 10, 0xA060C0FF);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 2, at.z, 120, 2.5, 1, 2.5, 0.5);
        soundAt(at, SoundEvents.GENERIC_SPLASH, 3f, 0.4f);
    }

    private void dive() {
        state = 1;
        stateTicks = 0;
        immune = "Левиафан под водой";
        splash(position());
        Vec3 c = poolCenter();
        teleportTo(c.x, c.y, c.z);
        int next;
        do next = random.nextInt(4); while (next == spot && points("emerge").size() > 1);
        spot = next;
        Vec3 out = spotPos(spot);
        Vec3 ground = new Vec3(out.x, out.y + 1, out.z);
        circle(ground, 6, 80, 1.0f).color(Telegraph.BLUE).then(e -> {
            e.setDeltaMovement(e.getDeltaMovement().add(0, 1.1, 0));
            e.hurtMarked = true;
        });
        shoutAll("Левиафан нырнул! Он вынырнет в отмеченном круге.");
    }

    private void emerge() {
        state = 0;
        stateTicks = 0;
        immune = null;
        Vec3 s = spotPos(spot);
        teleportTo(s.x, s.y, s.z);
        splash(s);
    }

    // ---------------- отлив ----------------

    private void drain() {
        state = 2;
        stateTicks = 0;
        immune = null;
        zones.clear();
        if (arenaExtra.has("pool")) {
            var a = arenaExtra.getAsJsonArray("pool");
            for (BlockPos p : BlockPos.betweenClosed(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(),
                    a.get(3).getAsInt(), a.get(4).getAsInt(), a.get(5).getAsInt())) {
                if (level().getBlockState(p).is(Blocks.WATER)) breakBlock(p);
            }
        }
        Vec3 c = poolCenter();
        teleportTo(c.x, c.y, c.z);
        for (ServerPlayer p : players()) p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 6, 0));
        shoutAll("ОТЛИВ! Вода ушла — Левиафан беспомощен на дне. Прыгайте и бейте! Прилив через 20 с.");
        sound(SoundEvents.BUCKET_EMPTY, 3f, 0.3f);
        fxRing(c, 0.5, 16, 20, 0xC0FFFFFF);
    }

    private void refill() {
        restoreBroken();
    }

    private void tide() {
        Vec3 c = poolCenter();
        for (ServerPlayer p : players()) {
            if (p.getY() < c.y + 7.5 && Math.abs(p.getX() - c.x) < 17 && Math.abs(p.getZ() - c.z) < 17) {
                hit(p, 0.6f);
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.8, 0));
                p.hurtMarked = true;
                tip(p, "Прилив накрыл вас!", ChatFormatting.DARK_AQUA);
            }
        }
        refill();
        fxPillar(c, 15, 9, 20, 0xA040A0FF);
        sound(SoundEvents.AMBIENT_UNDERWATER_ENTER, 3f, 0.4f);
        dive();
    }

    @Override
    protected void bossTick() {
        setNoGravity(true);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        stateTicks++;
        LivingEntity t = getTarget();
        if (t != null) getLookControl().setLookAt(t, 30, 30);
        switch (state) {
            case 0 -> {
                Vec3 s = spotPos(spot);
                if (position().distanceToSqr(s) > 1) teleportTo(s.x, s.y, s.z);
                if (stateTicks > 20 * 16 && !isCasting() && fightTicks > 0) dive();
            }
            case 1 -> {
                if (stateTicks % 20 == 0) for (ServerPlayer p : players()) if (p.isInWater()) {
                    hit(p, 0.25f);
                    Fx.ring(level(), p.position(), 0.2, 2, 8, 0xC03050FF);
                }
                if (stateTicks >= 80) emerge();
            }
            case 2 -> {
                if (stateTicks % 10 == 0 && level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.FALLING_WATER, getX(), getY() + 3, getZ(), 20, 1.5, 1, 1.5, 0);
                if (stateTicks == 20 * 15) shoutAll("Вода возвращается! Выбирайтесь из чаши!");
                if (stateTicks >= 20 * 20) tide();
            }
            default -> { }
        }
    }

    @Override
    protected boolean canCast() {
        return state == 0 && super.canCast();
    }

    @Override
    protected void onPhase(int phase) {
        shout(phase == 1 ? "Море… уходит?!" : "Нет! Только не снова!");
        drain();
    }

    @Override
    protected boolean leashed() {
        return false;
    }

    @Override
    protected void onReset() {
        super.onReset();
        refill();
        state = 0;
        stateTicks = 0;
        immune = null;
        if (getHome() != null) teleportTo(getHome().getX() + 0.5, getHome().getY(), getHome().getZ() + 0.5);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        float m = state == 2 ? 1.6f : 1f;
        return w == BossDamage.Weapon.FROST ? m * 1.25f : w == BossDamage.Weapon.FIRE ? m * 0.7f : m;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 30f));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(4);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.ELDER_GUARDIAN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.ELDER_GUARDIAN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.ELDER_GUARDIAN_DEATH; }
    @Override public float getVoicePitch() { return 0.5f; }
}
