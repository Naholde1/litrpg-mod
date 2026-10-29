package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Стриж — дуэлянт без шкал: у него только откаты, инерция и отмены.
 * <p>
 * Общие виды движка этого не давали: серия была одним ударом «на троих», рывок — простым
 * перемещением, а «Три стороны» — обычной очередью на месте. Здесь серия действительно идёт
 * тремя взмахами и третий уносит вперёд, рывок обрывает начатое и возвращает серпы, а «Три
 * стороны» обходит цель кругом. В этом весь класс: он не стоит.
 */
public final class Swift {
    private static final int EDGE = 0xE048C9B0;
    /** Метка скольжения: пока держится, снаряды проходят мимо. */
    public static final String SLIDE_TAG = "litrpg_slide";
    /** Засечка на цели: до какого тика она держится. */
    public static final String NOTCH_TAG = "litrpg_notch";
    /** Сколько зарубок уже сделано — каждая добавляет цели получаемый урон. */
    public static final String NOTCH_N_TAG = "litrpg_notch_n";
    /** Призрачный шаг: до какого тика следующий удар идёт из-за плеча. */
    public static final String GHOST_TAG = "litrpg_ghost";
    /** «На грани»: до какого тика Стриж бьёт без пауз и получает больше. */
    public static final String EDGE_TAG = "litrpg_edge";

    private Swift() {}

    static void register() {
        Special.put("sw_sickles", Swift::sickles);
        Special.put("sw_dash", Swift::breakaway);
        Special.put("sw_crossing", Swift::crossing);
        Special.put("sw_storm", Swift::threeSides);
        Special.put("sw_slide", Swift::slide);
        Special.put("sw_kick", Swift::kick);
        Special.put("sw_wallbounce", Swift::wallBounce);
        Special.put("sw_flurry", Swift::flurry);
        Special.put("sw_mark", Swift::notch);
        Special.put("sw_hook", Swift::hook);
        Special.put("sw_ghost", Swift::ghostStep);
        Special.put("sw_edge", Swift::edge);
    }

    private static void hitAt(ServerPlayer p, Ability a, Vec3 c, double r, float dmg, int color, double arc) {
        ServerLevel sl = p.serverLevel();
        Fx.slash(sl, c, p.getViewVector(1f), r, arc, 0, 7, color);
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r))) {
            AbilityHandler.hurt(p, a, e, dmg);
        }
    }

    /**
     * Парные серпы: три взмаха подряд, а третий — выпад на два блока вперёд.
     * Серия не доигрывается, если прервать её рывком, — на этом и строится игра.
     */
    private static boolean sickles(ServerPlayer p, PlayerStats s, Ability a) {
        float dmg = AbilityHandler.base(s, a) * a.power;
        ServerLevel sl = p.serverLevel();
        Scheduler.repeat(0, 4, 3, i -> {
            if (!p.isAlive()) return false;
            if (i == 2) {
                // выпад: Стриж уходит вперёд вместе с ударом
                Vec3 dir = AbilityHandler.flatLook(p);
                p.setDeltaMovement(dir.x * 0.9, 0.12, dir.z * 0.9);
                p.hurtMarked = true;
                hitAt(p, a, p.position().add(dir.scale(1.6)).add(0, 1, 0), a.radius + 0.8, dmg * 1.5f, 0xE0FFFFFF, 150);
                sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.1f);
            } else {
                hitAt(p, a, p.position().add(AbilityHandler.flatLook(p).scale(1.1)).add(0, 1, 0), a.radius, dmg, EDGE, 110);
                sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.9f, 1.4f + i * 0.2f);
            }
            return true;
        });
        return true;
    }

    /**
     * Срыв: рывок сквозь строй, который обрывает начатое и возвращает серпы.
     * Отмена анимации — не красивое слово: откат серии действительно сбрасывается.
     */
    private static boolean breakaway(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        Vec3 from = p.position();
        Vec3 to = from.add(dir.scale(5.5));
        float dmg = AbilityHandler.base(s, a) * a.power;
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(from, to).inflate(1.6))) {
            AbilityHandler.hurt(p, a, e, dmg);
        }
        p.setDeltaMovement(dir.x * 1.5, 0.18, dir.z * 1.5);
        p.hurtMarked = true;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 12, 4, false, false, false));
        Fx.beam(sl, from.add(0, 1, 0), to.add(0, 1, 0), 0.5, 8, EDGE);
        Fx.ring(sl, from, 0.2, 1.8, 8, EDGE);
        // серпы снова готовы: серия начинается заново
        Ability sickles = Ability.byKey("sw_sickles");
        if (sickles != null) s.setCooldownEnd(sickles, 0);
        p.getPersistentData().putLong(AbilityHandler.GCD_TAG, 0);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1f, 1.6f);
        return true;
    }

    /** Перекрестье: проход насквозь — Стриж оказывается за спиной и бьёт оттуда. */
    private static boolean crossing(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, 7);
        if (t == null) {
            p.displayClientMessage(Component.literal("Нет цели для прохода.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 behind = t.position().add(t.position().subtract(from).normalize().scale(1.6));
        p.teleportTo(behind.x, behind.y, behind.z);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        float dmg = AbilityHandler.base(s, a) * a.power * 1.6f;
        AbilityHandler.hurt(p, a, t, dmg);
        Fx.beam(sl, from.add(0, 1, 0), behind.add(0, 1, 0), 0.4, 8, EDGE);
        Fx.slash(sl, t.position().add(0, 1, 0), p.getViewVector(1f), 2.2, 160, 0, 8, 0xE0FFFFFF);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.3f);
        return true;
    }

    /**
     * Три стороны: шесть ударов с трёх точек вокруг цели — Стриж обходит её по кругу
     * быстрее, чем она успевает повернуться.
     */
    private static boolean threeSides(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, 10);
        if (t == null) {
            p.displayClientMessage(Component.literal("Некого обходить.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power * 0.6f;
        Vec3 c = t.position();
        Scheduler.repeat(0, 5, 6, i -> {
            if (!p.isAlive() || !t.isAlive()) return false;
            double ang = Math.PI * 2 * i / 3.0;
            Vec3 at = c.add(Math.cos(ang) * 2.0, 0, Math.sin(ang) * 2.0);
            p.teleportTo(at.x, at.y, at.z);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
            AbilityHandler.hurt(p, a, t, dmg);
            Fx.slash(sl, c.add(0, 1, 0), t.position().subtract(at).normalize(), 2.4, 140, 0, 6, EDGE);
            Fx.beam(sl, at.add(0, 1, 0), c.add(0, 1, 0), 0.22, 6, 0xE0FFFFFF);
            sl.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f + i * 0.1f);
            return true;
        });
        return true;
    }

    /** Скольжение: секунда, в которую снаряды проходят над головой. */
    private static boolean slide(ServerPlayer p, PlayerStats s, Ability a) {
        int ticks = Math.max(20, a.duration);
        p.getPersistentData().putLong(SLIDE_TAG, p.level().getGameTime() + ticks);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2, false, false, true));
        Vec3 dir = AbilityHandler.flatLook(p);
        p.setDeltaMovement(dir.x * 1.1, -0.1, dir.z * 1.1);
        p.hurtMarked = true;
        Fx.line(p.level(), p.position().add(0, 0.2, 0), dir, 4.5, 0.5, 10, EDGE);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 0.6f);
        p.displayClientMessage(Component.literal("Скольжение: снаряды идут над головой")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /**
     * Подсечка: нога под колено сбивает чтение. Если удалось прервать подготовку —
     * серпы тут же снова готовы и пауза между навыками снимается: за точное время
     * Стриж получает ход, а не просто немного урона.
     */
    private static boolean kick(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, Math.max(4, a.radius));
        if (t == null) {
            p.displayClientMessage(Component.literal("Некого подсекать.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        boolean wasCasting = t instanceof com.andrey.litrpg.entity.BossBase b && b.isCastingPublic();
        AbilityHandler.hurt(p, a, t, AbilityHandler.base(s, a) * a.power * 0.6f);
        boolean broke = t instanceof com.andrey.litrpg.entity.BossBase b2 && b2.interrupt(p);
        // сбит с ног: не прыгает, едва идёт и теряет шаг
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4, false, false, true));
        t.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false, false));
        if (t instanceof Mob m) m.getNavigation().stop();
        t.setDeltaMovement(t.getDeltaMovement().x * 0.1, t.getDeltaMovement().y, t.getDeltaMovement().z * 0.1);
        t.hurtMarked = true;
        Fx.slash(sl, t.position().add(0, 0.3, 0), p.getViewVector(1f), 2.0, 180, 80, 7, EDGE);
        Fx.ring(sl, t.position(), 0.2, 1.6, 8, EDGE);
        if (broke) {
            Ability sickles = Ability.byKey("sw_sickles");
            if (sickles != null) s.setCooldownEnd(sickles, 0);
            p.getPersistentData().putLong(AbilityHandler.GCD_TAG, 0);
            Fx.mark(sl, t, 0.9, 1, 20, 0xE0FFFFFF);
            p.displayClientMessage(Component.literal("Чтение сбито — серпы снова готовы")
                    .withStyle(ChatFormatting.GOLD), true);
        } else if (wasCasting) {
            p.displayClientMessage(Component.literal("Этот приём не сбить.").withStyle(ChatFormatting.GRAY), true);
        }
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, 0.8f);
        return true;
    }

    /** Твёрдая стена перед лицом в пределах len блоков — или null. */
    private static Vec3 wallAhead(ServerPlayer p, double len) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1f).scale(len));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.BLOCK ? hit.getLocation() : null;
    }

    /**
     * Отскок от стены: без стены приёма нет. Нужно подбежать к ней вплотную, и тогда
     * толчок переносит Стрижа за спину ближайшему врагу. Место на арене становится
     * ресурсом: там, где стен нет, этот навык молчит.
     */
    private static boolean wallBounce(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 wall = wallAhead(p, 3.0);
        if (wall == null) {
            p.displayClientMessage(Component.literal("Не от чего оттолкнуться — нужна стена впереди.")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        LivingEntity t = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity e : AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(Math.max(8, a.radius)))) {
            double d = e.distanceToSqr(p);
            if (d < best) {
                best = d;
                t = e;
            }
        }
        if (t != null) {
            Vec3 behind = t.position().add(t.position().subtract(wall).normalize().scale(1.7));
            p.teleportTo(behind.x, behind.y, behind.z);
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
            Fx.slash(sl, t.position().add(0, 1, 0), p.getViewVector(1f), 2.0, 140, 0, 7, EDGE);
            p.displayClientMessage(Component.literal("Отскок — вы за спиной").withStyle(ChatFormatting.AQUA), true);
        } else {
            // врага рядом нет: толчок просто уносит назад и вверх
            Vec3 away = from.subtract(wall).normalize();
            p.setDeltaMovement(away.x * 1.2, 0.75, away.z * 1.2);
            p.hurtMarked = true;
        }
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        Fx.beam(sl, wall, p.position().add(0, 1, 0), 0.35, 8, EDGE);
        Fx.ring(sl, from, 0.2, 1.6, 8, EDGE);
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.1f, 1.8f);
        return true;
    }

    /**
     * Вихрь серпов: три оборота вокруг себя, не сходя с места. Стриж буквально врастает
     * в пол на это время — его инерция гасится, — зато каждый круг шире и больнее
     * предыдущего, а третий добивает всех, кто остался в радиусе.
     */
    private static boolean flurry(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float dmg = AbilityHandler.base(s, a) * a.power;
        double r = Math.max(3, a.radius);
        Scheduler.repeat(0, 6, 3, i -> {
            if (!p.isAlive()) return false;
            // «не сходя с места»: оборот держит Стрижа на точке
            p.setDeltaMovement(0, Math.min(0, p.getDeltaMovement().y), 0);
            p.hurtMarked = true;
            double rad = r + i * 0.6;
            float mult = i == 2 ? 1.8f : 1f;
            Fx.slash(sl, p.position().add(0, 0.9 + i * 0.3, 0), p.getViewVector(1f), rad, 360, i * 20 - 20, 6,
                    i == 2 ? 0xE0FFFFFF : EDGE);
            Fx.ring(sl, p.position(), rad - 0.4, rad, 6, EDGE);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(rad))) {
                AbilityHandler.hurt(p, a, e, dmg * mult);
                if (i == 2) {
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
                }
            }
            sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                    1f, 1.2f + i * 0.3f);
            return true;
        });
        return true;
    }

    /**
     * Засечка: зарубка на броне. Пока держится, по цели больнее бьют все — и каждый
     * удар Стрижа добавляет новую зарубку, до пяти. Класс без шкал всё-таки копит:
     * копит он на чужой броне.
     */
    private static boolean notch(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, Math.max(8, a.radius));
        if (t == null) {
            p.displayClientMessage(Component.literal("Нет цели для засечки.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        long until = sl.getGameTime() + Math.max(60, a.duration);
        t.getPersistentData().putLong(NOTCH_TAG, until);
        t.getPersistentData().putInt(NOTCH_N_TAG, 1);
        t.getPersistentData().putLong(AbilityHandler.MARK_TAG, until);
        AbilityHandler.hurt(p, a, t, AbilityHandler.base(s, a) * a.power);
        Fx.mark(sl, t, 0.9, 0, (int) (until - sl.getGameTime()), EDGE);
        Fx.slash(sl, t.position().add(0, 1, 0), p.getViewVector(1f), 1.8, 120, 30, 8, 0xE0FFFFFF);
        sl.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.7f, 1.8f);
        p.displayClientMessage(Component.literal("Засечка на " + t.getName().getString()
                + " — бейте туда").withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** Добавить зарубку (вызывается из боя, когда Стриж бьёт отмеченного). */
    public static void deepen(LivingEntity t) {
        int n = Math.min(5, t.getPersistentData().getInt(NOTCH_N_TAG) + 1);
        t.getPersistentData().putInt(NOTCH_N_TAG, n);
        if (t.level() instanceof ServerLevel sl) Fx.mark(sl, t, 0.5 + n * 0.1, 0, 12, EDGE);
    }

    /** Насколько больнее бьют по отмеченной цели: по 6% за зарубку. */
    public static float notchMult(LivingEntity t) {
        if (t.level().getGameTime() >= t.getPersistentData().getLong(NOTCH_TAG)) return 1f;
        return 1f + 0.06f * t.getPersistentData().getInt(NOTCH_N_TAG);
    }

    /**
     * Зацеп: крюк за выступ. Нужен настоящий камень в прицеле — тогда Стриж уходит
     * наверх по тросу. В чистом поле зацепиться не за что, и это честная часть приёма.
     */
    private static boolean hook(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = wallAhead(p, Math.max(8, a.radius));
        if (to == null) {
            p.displayClientMessage(Component.literal("Не за что зацепиться — наведитесь на выступ.")
                    .withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position().add(0, 1, 0);
        Vec3 v = to.subtract(from);
        double len = Math.max(1, v.length());
        Vec3 pull = v.normalize().scale(Math.min(2.0, 0.5 + len * 0.1));
        p.setDeltaMovement(pull.x, Math.max(0.45, pull.y + 0.25), pull.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, false));
        Fx.beam(sl, from, to, 0.18, 14, EDGE);
        Fx.ring(sl, to, 0.1, 0.9, 12, 0xE0FFFFFF);
        sl.playSound(null, to.x, to.y, to.z, SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    /**
     * Призрачный шаг: Стриж исчезает, оказывается за плечом цели — и твари рядом
     * теряют его из вида. Следующий удар в эти три секунды идёт из-за спины и бьёт
     * больше чем вдвое: приём не про урон, а про то, чтобы его подготовить.
     */
    private static boolean ghostStep(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.aimEnemy(p, Math.max(8, a.radius));
        if (t == null) {
            p.displayClientMessage(Component.literal("Некому показать спину.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 behind = t.position().add(t.getLookAngle().multiply(-1, 0, -1).normalize().scale(1.6));
        p.teleportTo(behind.x, behind.y, behind.z);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, false));
        p.getPersistentData().putLong(GHOST_TAG, sl.getGameTime() + 60);
        // потерян из вида: кто бил по Стрижу, тот ищет его заново
        for (LivingEntity e : AbilityHandler.enemiesAround(p, p.getBoundingBox().inflate(16))) {
            if (e instanceof Mob m && m.getTarget() == p) m.setTarget(null);
        }
        Fx.beam(sl, from.add(0, 1, 0), behind.add(0, 1, 0), 0.3, 10, 0xC0303030);
        Fx.ring(sl, from, 0.2, 1.8, 10, 0xC0303030);
        Fx.spiral(sl, p, 0.8, 2.0, 2, 40, EDGE);
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.6f);
        p.displayClientMessage(Component.literal("Призрачный шаг — следующий удар из-за плеча")
                .withStyle(ChatFormatting.DARK_AQUA), true);
        return true;
    }

    /**
     * На грани: десять секунд ни одна анимация не доигрывает. Пауза между навыками
     * снимается совсем, откаты идут вдвое быстрее — но и урон по Стрижу вырастает
     * на четверть. Класс с 70% здоровья платит за это по-настоящему.
     */
    private static boolean edge(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int ticks = Math.max(60, a.duration);
        p.getPersistentData().putLong(EDGE_TAG, sl.getGameTime() + ticks);
        p.getPersistentData().putLong(AbilityHandler.GCD_TAG, 0);
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 2, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 2, false, false, true));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 1, false, false, true));
        Fx.shield(sl, p, 1.0, ticks, EDGE);
        Fx.spiral(sl, p, 1.1, 2.2, 3, ticks, EDGE);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.2f, 1.4f);
        p.displayClientMessage(Component.literal("На грани: без пауз, но урон по вам +25%")
                .withStyle(ChatFormatting.RED), true);
        return true;
    }

    /** Идёт ли «На грани»: пауза между навыками снята, откаты вдвое короче. */
    public static boolean onEdge(ServerPlayer p) {
        return p.level().getGameTime() < p.getPersistentData().getLong(EDGE_TAG);
    }

    /** Ждёт ли удар из-за плеча (и снять ожидание). */
    public static boolean takeGhost(ServerPlayer p) {
        if (p.level().getGameTime() >= p.getPersistentData().getLong(GHOST_TAG)) return false;
        p.getPersistentData().putLong(GHOST_TAG, 0);
        return true;
    }

    /** Идёт ли сейчас скольжение (снаряды мимо). */
    public static boolean sliding(ServerPlayer p) {
        return p.level().getGameTime() < p.getPersistentData().getLong(SLIDE_TAG);
    }
}
