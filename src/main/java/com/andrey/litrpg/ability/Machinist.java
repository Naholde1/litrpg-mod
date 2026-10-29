package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.TotemEntity;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Механик: урон идёт от того, что он поставил.
 * <p>
 * Раньше турели были ванильными снеговиками, а «Разгон» ускорял самого Механика — хотя по описанию
 * разгоняются механизмы. Здесь станок — своя сущность: стоит, где поставили, крутит голову за целью,
 * его можно разбить. «Разгон» действительно вдвое ускоряет уже стоящие станки, а за разгон платят
 * перегревом. Масло сначала разливается — и только потом вспыхивает; мортира кладёт три снаряда
 * по отмеченной точке, от которых можно уйти.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Machinist {
    /** Метка станка: ключ навыка, которым он поставлен. */
    public static final String TAG = "litrpg_mc_turret";
    /** Станок полного арсенала: временный, в лимит не входит. */
    private static final String ARSENAL = "litrpg_mc_arsenal";
    /** Когда следующий выстрел. */
    private static final String NEXT = "litrpg_mc_next";
    /** До какого тика станок разогнан и до какого — перегрет. */
    private static final String FAST = "litrpg_mc_fast";
    private static final String HOT = "litrpg_mc_hot";
    /** Сколько обычных станков держит Механик одновременно. */
    private static final int LIMIT = 2;
    private static final double RANGE = 22;
    private static final int PERIOD = 20;
    private static final int GOLD = 0xE0FFC040;
    private static final int FIRE = 0xE0FF7A20;

    private Machinist() {}

    static void register() {
        Special.put("mc_turret", (p, s, a) -> placeTurret(p, s, a, spot(p, p.position().add(AbilityHandler.flatLook(p).scale(2.2))), false));
        Special.put("mc_arsenal", Machinist::arsenal);
        Special.put("mc_overclock", Machinist::overclock);
        Special.put("mc_oil", Machinist::oil);
        Special.put("mc_mortar", Machinist::mortar);
    }

    // ---------------- станки ----------------

    /** Свои станки хозяина рядом. */
    static List<Mob> turrets(ServerPlayer p) {
        List<Mob> out = new ArrayList<>();
        for (Mob m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64),
                e -> e.isAlive() && e instanceof TotemEntity && e.getPersistentData().contains(TAG)
                        && p.getUUID().equals(Minions.ownerOf(e)))) {
            out.add(m);
        }
        return out;
    }

    private static boolean placeTurret(ServerPlayer p, PlayerStats s, Ability a, Vec3 at, boolean arsenal) {
        ServerLevel sl = p.serverLevel();
        if (!arsenal) {
            // третий станок разбирает самый старый: держать арену сеткой из десяти турелей нельзя
            List<Mob> own = new ArrayList<>(turrets(p));
            own.removeIf(m -> m.getPersistentData().getBoolean(ARSENAL));
            own.sort(Comparator.comparingInt(m -> -m.tickCount));
            while (own.size() >= LIMIT) {
                Mob old = own.remove(0);
                Fx.ring(sl, old.position(), 0.2, 1.4, 10, GOLD);
                old.discard();
            }
        }
        var type = com.andrey.litrpg.registry.ModEntities.TOTEMS.get("machinist_turret");
        Entity raw = type == null ? null : type.get().create(sl);
        if (!(raw instanceof Mob t)) return false;
        t.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        t.getPersistentData().putUUID(Minions.OWNER_TAG, p.getUUID());
        t.getPersistentData().putString(TAG, a.key);
        t.getPersistentData().putBoolean(ARSENAL, arsenal);
        t.getPersistentData().putLong(Minions.EXPIRE_TAG, sl.getGameTime() + Math.max(200, a.duration));
        t.getPersistentData().putInt("litrpg_lvl", s.getLevel());
        t.finalizeSpawn(sl, sl.getCurrentDifficultyAt(t.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        t.setCustomName(Component.literal(arsenal ? "Станок арсенала" : "Турель").withStyle(ChatFormatting.GOLD));
        t.setPersistenceRequired();
        t.goalSelector.removeAllGoals(g -> true);
        t.targetSelector.removeAllGoals(g -> true);
        AttributeInstance speed = t.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0);
        AttributeInstance hp = t.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(25 + s.getLevel() * 1.6);
        t.setHealth(t.getMaxHealth());
        sl.addFreshEntity(t);
        Fx.pillar(sl, at, 0.7, 2.2, 14, GOLD);
        Fx.ring(sl, at, 0.2, 1.6, 10, GOLD);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.6f);
        return true;
    }

    /** Место под станок: точка на твёрдом полу рядом с желаемой. */
    private static Vec3 spot(ServerPlayer p, Vec3 want) {
        BlockPos pos = BlockPos.containing(want);
        for (int d = 0; d <= 4; d++) {
            BlockPos c = pos.above(d <= 2 ? d : 2 - d);
            if (p.level().getBlockState(c).isAir() && !p.level().getBlockState(c.below()).isAir()) return Vec3.atBottomCenterOf(c);
        }
        return p.position();
    }

    /** Полный арсенал: три станка разом — по кругу вокруг точки, куда смотрит Механик. */
    private static boolean arsenal(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 c = AbilityHandler.lookPoint(p, 18);
        for (Mob old : turrets(p)) {
            if (old.getPersistentData().getBoolean(ARSENAL)) old.discard();
        }
        for (int i = 0; i < 3; i++) {
            double ang = Math.PI * 2 * i / 3 + p.getYRot() * Math.PI / 180;
            placeTurret(p, s, a, spot(p, c.add(Math.cos(ang) * 4.5, 0, Math.sin(ang) * 4.5)), true);
        }
        Fx.ring(p.serverLevel(), c, 0.3, 5, 20, GOLD);
        p.displayClientMessage(Component.literal("Полный арсенал развёрнут.").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /**
     * Разгон: все станки рядом стреляют вдвое чаще и бьют сильнее. Механизмы не железные —
     * после разгона они остывают и молчат несколько секунд.
     */
    private static boolean overclock(ServerPlayer p, PlayerStats s, Ability a) {
        List<Mob> list = turrets(p);
        if (list.isEmpty()) {
            p.displayClientMessage(Component.literal("Разгонять нечего — станков нет.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        long now = p.level().getGameTime();
        int dur = Math.max(100, a.duration);
        for (Mob t : list) {
            t.getPersistentData().putLong(FAST, now + dur);
            t.getPersistentData().putLong(HOT, now + dur + 60);
            t.getPersistentData().putLong(NEXT, now);
            Fx.beam(p.level(), p.position().add(0, 1.2, 0), t.position().add(0, 1, 0), 0.15, 10, FIRE);
            Fx.spiral(p.level(), t, 0.7, 1.6, 2, dur, FIRE);
        }
        AbilityHandler.applySelf(a, p);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 1f, 1.8f);
        p.displayClientMessage(Component.literal("Разгон: станков " + list.size() + " — вдвое быстрее, потом перегрев.")
                .withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e instanceof TotemEntity t) || e.level().isClientSide || !e.getPersistentData().contains(TAG)) return;
        if (!(e.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        var data = e.getPersistentData();
        long fast = data.getLong(FAST), hot = data.getLong(HOT);
        boolean overclocked = now < fast;
        if (!overclocked && now < hot) {
            if (now % 10 == 0) sl.sendParticles(ParticleTypes.LARGE_SMOKE, e.getX(), e.getY() + 1.4, e.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
            return;
        }
        if (now < data.getLong(NEXT)) return;
        ServerPlayer owner = Minions.owner(e);
        if (owner == null) return;
        Ability a = Ability.byKey(data.getString(TAG));
        if (a == null) return;
        LivingEntity target = null;
        double best = RANGE * RANGE;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, new AABB(e.position(), e.position()).inflate(RANGE),
                x -> x.isAlive() && x instanceof Enemy && Minions.ownerOf(x) == null && !(x instanceof TotemEntity))) {
            double d = m.distanceToSqr(e);
            if (d < best && t.hasLineOfSight(m)) {
                best = d;
                target = m;
            }
        }
        if (target == null) {
            data.putLong(NEXT, now + 10);
            return;
        }
        data.putLong(NEXT, now + (overclocked ? PERIOD / 2 : PERIOD));
        // голова станка смотрит туда, куда бьёт
        Vec3 d = target.position().subtract(e.position());
        float yaw = (float) (Math.atan2(d.z, d.x) * 180 / Math.PI) - 90;
        t.setYRot(yaw);
        t.setYHeadRot(yaw);
        t.yBodyRot = yaw;
        float dmg = AbilityHandler.base(StatsManager.get(owner), a) * (overclocked ? 0.75f : 0.55f);
        Fx.beam(sl, e.position().add(0, 1.1, 0), target.position().add(0, target.getBbHeight() / 2, 0), overclocked ? 0.26 : 0.18, 6,
                overclocked ? FIRE : GOLD);
        AbilityHandler.hurt(owner, a, target, dmg);
        t.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        sl.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7f, overclocked ? 1.8f : 1.4f);
    }

    // ---------------- масло и мортира ----------------

    /**
     * Масло и искра: сначала лужа — скользко, враги вязнут; через полторы секунды искра,
     * вспышка и горящее пятно. Хорошо, когда в лужу успел затащить гарпуном.
     */
    private static boolean oil(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 24);
        double r = a.radius;
        float base = AbilityHandler.base(s, a) * a.power;
        Fx.disc(sl, c, r, 0, 30, 0x90302010);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.HONEY_BLOCK_PLACE, SoundSource.PLAYERS, 1.2f, 0.6f);
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r, 2, r))) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
        }
        int burn = Math.max(4, a.duration / 20);
        Scheduler.later(30, () -> {
            if (!p.isAlive()) return;
            Fx.sphere(sl, c.add(0, 0.5, 0), 0.4, r, 10, FIRE);
            Fx.disc(sl, c, r, 0, burn * 20, 0x80FF6010);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.5f, 0.8f);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r, 2, r))) {
                AbilityHandler.hurt(p, a, e, base);
                e.setSecondsOnFire(4);
            }
            Scheduler.repeat(20, 20, burn, i -> {
                if (!p.isAlive()) return false;
                sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, 20, r / 2, 0.1, r / 2, 0.02);
                for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r, 2, r))) {
                    AbilityHandler.hurt(p, a, e, base * 0.25f);
                    e.setSecondsOnFire(2);
                }
                return true;
            });
        });
        return true;
    }

    /** Мортира: метка на полу — и три навесных снаряда друг за другом по отмеченному месту. */
    private static boolean mortar(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 40);
        double r = a.radius;
        float dmg = AbilityHandler.base(s, a) * a.power * 0.55f;
        Fx.disc(sl, c, r, 0, 30, 0x90FFC040);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.6f, 1.8f);
        Scheduler.repeat(30, 10, 3, i -> {
            if (!p.isAlive()) return false;
            Vec3 at = c.add((sl.random.nextDouble() - 0.5) * r, 0, (sl.random.nextDouble() - 0.5) * r);
            Fx.pillar(sl, at, 0.6, 8, 6, GOLD);
            Fx.sphere(sl, at.add(0, 0.5, 0), 0.4, 3.2, 10, FIRE);
            sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 1.1f);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(at, at).inflate(3.2, 2, 3.2))) {
                AbilityHandler.hurt(p, a, e, dmg);
            }
            return true;
        });
        return true;
    }
}
