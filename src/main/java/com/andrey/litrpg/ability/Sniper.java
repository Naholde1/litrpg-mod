package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Снайпер: готовится и бьёт один раз.
 * <p>
 * В общих видах выстрелы были самонаводящимися снарядами — куда ни целься, попадёшь. Здесь
 * выстрел мгновенный и идёт ровно туда, куда смотрит прицел, а верхняя часть цели — голова:
 * попал в неё — урон на треть больше. Неподвижность копит точность: каждая секунда
 * на месте добавляет урон (под «Прицелиться» и «Задержать дыхание» — заметно больше), а шаг
 * сбрасывает всё. Метка делает цель видимой сквозь стены и уязвимой к его выстрелам.
 * Капкан ждёт на полу, пока на него не наступят.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Sniper {
    /** С какого тика снайпер стоит неподвижно. */
    private static final String STILL = "litrpg_sn_still";
    private static final String LAST_X = "litrpg_sn_x";
    private static final String LAST_Z = "litrpg_sn_z";
    /** До какого тика действует прицеливание и до какого — задержка дыхания. */
    private static final String AIM = "litrpg_sn_aim";
    private static final String BREATH = "litrpg_sn_breath";
    /** Отмеченная цель и срок метки. */
    private static final String MARK = "litrpg_sn_mark";
    private static final String MARK_UNTIL = "litrpg_sn_mark_until";
    private static final int TRACER = 0xE0E0E8F0;
    private static final int HEAD = 0xF0FF4040;
    /** Попадание в голову. */
    private static final float HEAD_MULT = 1.35f;

    private Sniper() {}

    static void register() {
        Special.put("sn_shot", (p, s, a) -> shot(p, s, a, 1f));
        Special.put("sn_headshot", (p, s, a) -> shot(p, s, a, 1f));
        Special.put("sn_finisher", Sniper::finisher);
        Special.put("sn_pierce", Sniper::pierce);
        Special.put("sn_aim", (p, s, a) -> focus(p, a, AIM, "Прицел: стойте на месте — каждая секунда добавляет урон."));
        Special.put("sn_breath", (p, s, a) -> focus(p, a, BREATH, "Дыхание задержано: следующий выстрел — в полную силу."));
        Special.put("sn_mark", Sniper::mark);
        Special.put("sn_trap", Sniper::trap);
    }

    // ---------------- неподвижность ----------------

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        var d = p.getPersistentData();
        double dx = p.getX() - d.getDouble(LAST_X), dz = p.getZ() - d.getDouble(LAST_Z);
        if (dx * dx + dz * dz > 0.0016 || !d.contains(STILL)) d.putLong(STILL, p.level().getGameTime());
        d.putDouble(LAST_X, p.getX());
        d.putDouble(LAST_Z, p.getZ());
    }

    /** Сколько секунд снайпер не двигается. */
    private static float stillSeconds(ServerPlayer p) {
        return (p.level().getGameTime() - p.getPersistentData().getLong(STILL)) / 20f;
    }

    /**
     * Множитель от неподвижности: без настроя — до +20% за 4 секунды, под прицеливанием — до +45%,
     * с задержанным дыханием — сразу +30% и до +60%.
     */
    static float steady(ServerPlayer p) {
        long now = p.level().getGameTime();
        float still = stillSeconds(p);
        var d = p.getPersistentData();
        if (now < d.getLong(BREATH)) return 1.3f + Math.min(0.3f, still * 0.1f);
        if (now < d.getLong(AIM)) return 1f + Math.min(0.45f, still * 0.12f);
        return 1f + Math.min(0.2f, still * 0.05f);
    }

    private static boolean focus(ServerPlayer p, Ability a, String tag, String msg) {
        AbilityHandler.applySelf(a, p);
        p.getPersistentData().putLong(tag, p.level().getGameTime() + Math.max(100, a.duration));
        Fx.ring(p.level(), p.position().add(0, 0.05, 0), 0.6, 1.4, Math.max(100, a.duration), 0x80E0E8F0);
        AbilityHandler.sound(p, SoundEvents.SPYGLASS_USE, 1f, 1f);
        p.displayClientMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    // ---------------- выстрел ----------------

    /** Луч прицела: до стены или до дальности. */
    private static Vec3 rayEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(range));
        var hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Куда луч входит в цель; пусто — мимо. */
    private static Optional<Vec3> entry(ServerPlayer p, LivingEntity t, Vec3 end) {
        return t.getBoundingBox().inflate(0.3).clip(p.getEyePosition(), end);
    }

    /** Попал ли луч в голову: верхняя четверть роста цели. */
    private static boolean headHit(LivingEntity t, Vec3 at) {
        return at.y >= t.getY() + t.getBbHeight() * 0.75;
    }

    private static float markBonus(ServerPlayer p, LivingEntity t) {
        var d = p.getPersistentData();
        if (!d.hasUUID(MARK) || !d.getUUID(MARK).equals(t.getUUID()) || p.level().getGameTime() >= d.getLong(MARK_UNTIL)) return 1f;
        Ability m = Ability.byKey("sn_mark");
        return m == null ? 1.35f : Math.max(1.1f, m.power);
    }

    /** Мгновенный выстрел туда, куда смотрит прицел; голова — на треть больнее. */
    private static boolean shot(ServerPlayer p, PlayerStats s, Ability a, float mult) {
        LivingEntity t = AbilityHandler.lookEnemy(p, a.radius);
        Vec3 end = rayEnd(p, a.radius);
        ServerLevel sl = p.serverLevel();
        // трасса начинается чуть впереди глаз, иначе она закрывает стрелку весь обзор
        Vec3 from = p.getEyePosition().add(p.getViewVector(1f).scale(1.2)).subtract(0, 0.25, 0);
        if (t == null) {
            // промах — тоже выстрел: откат тратится, трасса видна
            Fx.beam(sl, from, end, 0.06, 6, TRACER);
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1f, 1.4f);
            return true;
        }
        Vec3 at = entry(p, t, end).orElse(t.position().add(0, t.getBbHeight() * 0.5, 0));
        boolean head = headHit(t, at);
        float dmg = AbilityHandler.base(s, a) * a.power * mult * steady(p) * markBonus(p, t) * (head ? HEAD_MULT : 1f);
        Fx.beam(sl, from, at, a.power >= 3 ? 0.16 : 0.08, a.power >= 3 ? 12 : 6, head ? HEAD : TRACER);
        sl.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, head ? 14 : 6, 0.1, 0.1, 0.1, 0.3);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), a.power >= 3 ? SoundEvents.TRIDENT_THROW : SoundEvents.CROSSBOW_SHOOT,
                SoundSource.PLAYERS, 1f, a.power >= 3 ? 0.7f : 1.3f);
        AbilityHandler.hurt(p, a, t, dmg);
        // неподвижность потрачена: следующий выстрел снова копит с нуля
        p.getPersistentData().putLong(STILL, p.level().getGameTime());
        p.getPersistentData().remove(BREATH);
        if (head) p.displayClientMessage(Component.literal(String.format("В голову! %.1f", dmg)).withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** Контрольный: тот же выстрел, но урон растёт с тем, сколько жизни цель уже потеряла (до втрое). */
    private static boolean finisher(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.lookEnemy(p, a.radius);
        float mult = t == null ? 1f : 1f + 2f * (1f - t.getHealth() / Math.max(1f, t.getMaxHealth()));
        return shot(p, s, a, mult);
    }

    /** Навылет: болт проходит сквозь всех на линии прицела; каждый следующий получает на 15% меньше. */
    private static boolean pierce(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 end = rayEnd(p, a.radius);
        AABB box = new AABB(eye, end).inflate(1);
        List<LivingEntity> line = new ArrayList<>();
        for (LivingEntity e : AbilityHandler.enemiesAround(p, box)) {
            if (e.getBoundingBox().inflate(0.4).clip(eye, end).isPresent()) line.add(e);
        }
        line.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        float dmg = AbilityHandler.base(s, a) * a.power * steady(p);
        for (LivingEntity e : line) {
            AbilityHandler.hurt(p, a, e, dmg * markBonus(p, e));
            dmg *= 0.85f;
        }
        Fx.beam(sl, eye.add(p.getViewVector(1f).scale(1.2)).subtract(0, 0.25, 0), end, 0.14, 10, TRACER);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1f, 1.2f);
        p.getPersistentData().putLong(STILL, p.level().getGameTime());
        if (line.size() > 1) p.displayClientMessage(Component.literal("Навылет: " + line.size() + " целей").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    // ---------------- метка и капкан ----------------

    /** Метка стрелка: цель светится сквозь стены, его выстрелы по ней сильнее. */
    private static boolean mark(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int dur = Math.max(100, a.duration);
        p.getPersistentData().putUUID(MARK, t.getUUID());
        p.getPersistentData().putLong(MARK_UNTIL, p.level().getGameTime() + dur);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, dur, 0, false, false));
        Fx.mark(p.level(), t, 0.5, 1, dur, HEAD);
        AbilityHandler.sound(p, SoundEvents.SPYGLASS_USE, 1f, 1.4f);
        p.displayClientMessage(Component.literal("Метка: " + t.getName().getString() + " — выстрелы по ней сильнее.").withStyle(ChatFormatting.GRAY), true);
        return true;
    }

    /** Капкан: ставится на пол под прицелом и ждёт; наступивший застревает и получает урон. */
    private static boolean trap(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 at = AbilityHandler.lookPoint(p, 10);
        if (at.distanceTo(p.position()) > 11) at = p.position();
        final Vec3 c = at;
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power;
        int life = Math.max(200, a.duration);
        UUID owner = p.getUUID();
        Fx.ring(sl, c.add(0, 0.05, 0), 0.4, 0.9, life, 0xC0B0A080);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.TRIPWIRE_ATTACH, SoundSource.PLAYERS, 1f, 0.7f);
        Scheduler.repeat(20, 5, life / 5, i -> {
            if (!p.isAlive() || !p.getUUID().equals(owner)) return false;
            List<LivingEntity> in = AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(1.3, 1, 1.3));
            if (in.isEmpty()) return true;
            for (LivingEntity e : in) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 6));
                AbilityHandler.hurt(p, a, e, dmg);
                Fx.ring(sl, e.position().add(0, 0.1, 0), 0.2, 1.2, 10, 0xE0B0A080);
            }
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 1.6f);
            sl.sendParticles(ParticleTypes.CRIT, c.x, c.y + 0.3, c.z, 12, 0.3, 0.2, 0.3, 0.2);
            p.displayClientMessage(Component.literal("Капкан захлопнулся!").withStyle(ChatFormatting.GRAY), true);
            return false;
        });
        return true;
    }
}
