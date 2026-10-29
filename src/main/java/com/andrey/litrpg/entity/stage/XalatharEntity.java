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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ксал'Атар, Голос Бездны (этап 5, финал всего подземелья, ур. 50). Вершина Шпиля — площадка над пустотой.
 * <ol>
 *   <li>Вершина (100–66%): «Клинок Бездны» (конус), «Длань» — взрыв под игроком отбрасывает к краю, «Падающие звёзды»,
 *       «Щупальца» — подручные из пустоты.</li>
 *   <li>Крах (66–33%): внешнее кольцо площадки рушится в пустоту — места меньше; «Зов Бездны» 4 с тянет всех к краю.</li>
 *   <li>Сердце Бездны (<33%): группа в сердце пустоты — островок во тьме. «Затмение»: вне светлых кругов тьма жжёт;
 *       круги перемещаются. На 10% — «Конец всего»: встаньте в круг всей группой, иначе удар сотрёт вас.</li>
 * </ol>
 */
public class XalatharEntity extends StageBoss {
    private boolean heart;
    private int heartTicks;
    private boolean endCast;
    private final List<Vec3> lights = new ArrayList<>();

    public XalatharEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
        skill("Клинок Бездны", 100, 16, 1).first(30)
                .when(t -> distanceTo(t) < 9)
                .begin(t -> cone(position(), dirTo(t), 9, 50, 16, 1.3f).color(Telegraph.PURPLE).then(e -> BossDamage.sunder(e, 2)))
                .act(t -> {
                    fxSlash(dirTo(t), 8, 130, -15, 0xE0A040FF);
                    sound(SoundEvents.WITHER_SHOOT, 1.5f, 0.6f);
                });
        skill("Длань", 260, 24, 2).first(90)
                .begin(t -> {
                    LivingEntity v = randomOther();
                    if (v == null) return;
                    Vec3 at = v.position();
                    Vec3 c = arena();
                    circle(at, 3.5, 24, 0.7f).color(Telegraph.PURPLE).then(e -> {
                        Vec3 out = e.position().subtract(c);
                        knockFrom(e, e.position().subtract(new Vec3(out.x, 0, out.z).normalize()), 1.6, 0.5);
                    });
                    tip(v, "Длань Бездны под вами — отбросит к краю!", ChatFormatting.LIGHT_PURPLE);
                })
                .act(t -> sound(SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.5f));
        skill("Падающие звёзды", 300, 30, 2).first(120)
                .begin(t -> {
                    for (int i = 0; i < 5 + players().size(); i++) {
                        Vec3 at = i < players().size() ? players().get(i).position() : randomSpot(heart ? 10 : 14);
                        circle(at, 2.8, 30, 0.9f).color(Telegraph.GOLD);
                        com.andrey.litrpg.util.Scheduler.later(14, () -> {
                            if (isAlive()) shootAt(BossProjectile.Kind.METEOR, at, 0f, 1.4f, 0f);
                        });
                    }
                    shoutAll("Падающие звёзды!");
                })
                .act(t -> sound(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2f, 0.5f));
        skill("Щупальца", 600, 20, 2).first(240)
                .act(t -> {
                    var h = ModEntities.WILD.get("mire_horror");
                    if (h != null) adds(h.get(), arena(), 2, 4, MobLevels.Rank.ELITE, null);
                    shoutAll("Щупальца Бездны! Танк — подберите их.");
                });
        skill("Зов Бездны", 520, 80, 2).fromPhase(1).untilPhase(1).first(80)
                .begin(t -> shoutAll("Зов Бездны тянет к краю — идите к центру!"))
                .during(left -> {
                    if (left % 2 != 0 || heart) return;
                    Vec3 c = arena();
                    for (ServerPlayer p : players()) {
                        Vec3 out = p.position().subtract(c);
                        Vec3 d = new Vec3(out.x, 0, out.z);
                        if (d.lengthSqr() > 0.01) p.push(d.normalize().x * 0.06, 0, d.normalize().z * 0.06);
                        p.hurtMarked = true;
                    }
                    if (left % 10 == 0) fxRing(c, 2, 16, 10, 0x90A040FF);
                })
                .act(t -> { });
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1024.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 15.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 56.0);
    }

    @Override public int getBossLevel() { return 50; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 116000; }
    @Override protected int enrageTicks() { return 20 * 60 * 9; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.66f, 0.33f, 0.1f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Внизу — пустота. «Длань» отбрасывает к краю — стойте спиной к центру.",
                "На 66% край площадки рухнет, «Зов Бездны» потянет к обрыву — идите к центру.",
                "На 33% — Сердце Бездны: держитесь в светлых кругах. На 10% — «Конец всего»: вся группа в один круг!");
    }

    // ---------------- крах ----------------

    private void crumble() {
        if (!arenaExtra.has("ring")) return;
        var a = arenaExtra.getAsJsonArray("ring");
        Vec3 c = point("center");
        int y = a.get(1).getAsInt();
        for (BlockPos p : BlockPos.betweenClosed(a.get(0).getAsInt(), y - 4, a.get(2).getAsInt(), a.get(3).getAsInt(), y, a.get(5).getAsInt())) {
            double d = Math.hypot(p.getX() + 0.5 - c.x, p.getZ() + 0.5 - c.z);
            if (d <= 12.3) continue;
            BlockState st = level().getBlockState(p);
            if (st.isAir()) continue;
            breakBlock(p);
        }
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, y, c.z, 200, 14, 1, 14, 0.2);
        shoutAll("КРАХ! Край площадки рухнул в пустоту!");
        sound(SoundEvents.GENERIC_EXPLODE, 3f, 0.4f);
    }

    private void restore() {
        restoreBroken();
    }

    // ---------------- сердце ----------------

    private AABB heartBox() {
        var a = arenaExtra.getAsJsonArray("heart_box");
        return new AABB(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble(),
                a.get(3).getAsDouble() + 1, a.get(4).getAsDouble() + 1, a.get(5).getAsDouble() + 1);
    }

    private void enterHeart() {
        heart = true;
        heartTicks = 0;
        shout("Идёмте. Я покажу вам, откуда приходит тьма.");
        moveGroup(points("heart_spots"), arenaBox.aabb().inflate(0, 10, 0));
        Vec3 c = point("heart");
        teleportTo(c.x, c.y, c.z);
        setAltArena(heartBox(), c);
        moveLights();
        shoutAll("Сердце Бездны! Тьма жжёт — держитесь в светлых кругах, они перемещаются.");
    }

    private void moveLights() {
        lights.clear();
        Vec3 c = point("heart");
        int n = Math.max(2, players().size() - 1);
        double a0 = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = a0 + Math.PI * 2 * i / n;
            Vec3 at = new Vec3(c.x + Math.cos(a) * 6, c.y, c.z + Math.sin(a) * 6);
            lights.add(at);
            Fx.pillar(level(), at, 3.2, 8, 200, 0x80FFF0C0);
            Fx.disc(level(), at, 3.2, 0, 200, 0x60FFF0C0);
        }
    }

    private void heartTick() {
        heartTicks++;
        if (heartTicks % 200 == 0) {
            moveLights();
            shoutAll("Свет сместился!");
            sound(SoundEvents.BEACON_POWER_SELECT, 2f, 0.6f);
        }
        if (heartTicks % 20 != 0) return;
        float dark = 0.05f + 0.006f * (heartTicks / 200);
        for (ServerPlayer p : players()) {
            boolean lit = false;
            for (Vec3 l : lights) if (Math.hypot(p.getX() - l.x, p.getZ() - l.z) < 3.4) lit = true;
            if (lit) continue;
            hit(p, dark);
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        }
    }

    private void endOfAll() {
        endCast = true;
        Vec3 c = point("heart");
        int need = Math.max(1, players().size());
        circle(c.add(0, 0, 4), 3.6, 200, 0.8f * need).color(Telegraph.GOLD).soak(need).then(e -> e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0)));
        Fx.pillar(level(), c.add(0, 0, 4), 3.6, 12, 200, 0xA0FFF0A0);
        shoutAll("«КОНЕЦ ВСЕГО»! 10 секунд — вся группа в золотой круг! Только вместе вы переживёте удар.");
        shout("Всё вернётся в тишину.");
        sound(SoundEvents.WITHER_SPAWN, 3f, 0.5f);
    }

    @Override
    protected void bossTick() {
        if (heart) heartTick();
        if (heart && tickCount % 20 == 0) {
            for (ServerPlayer p : players()) p.displayClientMessage(Component.literal(lights.isEmpty() ? "" : "Держитесь в свете!")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }

    @Override
    protected void onPhase(int phase) {
        switch (phase) {
            case 1 -> {
                shout("Ваш мир на краю. Как и вы.");
                crumble();
            }
            case 2 -> enterHeart();
            default -> {
                if (!endCast) endOfAll();
            }
        }
    }

    @Override
    protected boolean leashed() {
        return !heart && super.leashed();
    }

    @Override
    protected List<ServerPlayer> players() {
        List<ServerPlayer> out = new ArrayList<>(super.players());
        if (arenaExtra.has("heart_box") && level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, heartBox(), p -> p.isAlive() && !p.isCreative() && !p.isSpectator()))
                if (!out.contains(p)) out.add(p);
        }
        return out;
    }

    @Override
    protected List<LivingEntity> fighters() {
        if (heart) return new ArrayList<>(players());
        return super.fighters();
    }

    @Override
    public Vec3 arena() {
        return heart ? point("heart") : super.arena();
    }

    @Override
    protected void onReset() {
        super.onReset();
        restore();
        if (heart) {
            Vec3 c = point("center");
            List<Vec3> spots = new ArrayList<>();
            for (int i = 0; i < 5; i++) spots.add(c.add(-6 + i * 3, 0, 10));
            moveGroup(spots, heartBox());
        }
        clearAltArena();
        heart = false;
        endCast = false;
        lights.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy() && reason != RemovalReason.KILLED) restore();
        super.remove(reason);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.HOLY ? 1.35f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WARDEN_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WARDEN_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WITHER_DEATH; }
    @Override public float getVoicePitch() { return 0.6f; }
}
