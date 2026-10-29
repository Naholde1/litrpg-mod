package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Общий набор для способностей со своей механикой: цели, урон, лечение, толчки, оглушение
 * и метки-модификаторы урона со сроком.
 * <p>
 * Метки лежат в данных сущности: ключ — до какого тика действует, ключ + "_v" — величина.
 * Один обработчик урона читает их все: получаемый и наносимый урон, отражение, вампиризм,
 * перенаправление части урона защитнику. Так у десятков способностей нет своих обработчиков.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Kit {
    /** Цель получает больше (или меньше) урона: множитель. */
    public static final String TAKEN = "litrpg_k_taken";
    /** Сущность наносит больше (или меньше) урона: множитель. */
    public static final String DEALT = "litrpg_k_dealt";
    /** Доля полученного урона, которая возвращается бьющему. */
    public static final String REFLECT = "litrpg_k_reflect";
    /** Доля нанесённого урона, которая лечит бьющего. */
    public static final String LEECH = "litrpg_k_leech";
    /** Доля урона по цели, которую забирает на себя защитник (UUID в GUARD + "_by"). */
    public static final String GUARD = "litrpg_k_guard";

    private Kit() {}

    // ---------------- метки со сроком ----------------

    static long now(Entity e) {
        return e.level().getGameTime();
    }

    static void until(Entity e, String key, int ticks) {
        e.getPersistentData().putLong(key, now(e) + ticks);
    }

    static void until(Entity e, String key, float value, int ticks) {
        e.getPersistentData().putLong(key, now(e) + ticks);
        e.getPersistentData().putFloat(key + "_v", value);
    }

    static boolean active(Entity e, String key) {
        return now(e) < e.getPersistentData().getLong(key);
    }

    static float value(Entity e, String key, float def) {
        return active(e, key) ? e.getPersistentData().getFloat(key + "_v") : def;
    }

    static void clear(Entity e, String key) {
        e.getPersistentData().remove(key);
    }

    // ---------------- цели ----------------

    /** Враг под прицелом; промах — ближайший к линии взгляда. */
    static LivingEntity enemy(ServerPlayer p, double range) {
        LivingEntity t = AbilityHandler.lookEnemy(p, range);
        return t != null ? t : Dynamic.aimEnemy(p, range);
    }

    /** Враг под прицелом, а если его нет — ближайший в радиусе (для ударов вблизи). */
    static LivingEntity enemyNear(ServerPlayer p, double range) {
        LivingEntity t = enemy(p, range);
        return t != null ? t : nearest(p, p.position(), range);
    }

    static LivingEntity nearest(ServerPlayer p, Vec3 c, double r) {
        LivingEntity best = null;
        double bd = r * r;
        for (LivingEntity e : enemiesIn(p, c, r)) {
            double d = e.distanceToSqr(c);
            if (d < bd) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    /** Враги в шаре радиуса r. */
    static List<LivingEntity> enemiesIn(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r, Math.max(2.5, r * 0.6), r))) {
            if (e.position().distanceTo(c) <= r + e.getBbWidth() / 2) out.add(e);
        }
        return out;
    }

    /** Свои в шаре радиуса r (без духов и жителей). */
    static List<LivingEntity> alliesIn(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 3, r),
                e -> e.isAlive() && AbilityHandler.isAlly(p, e) && e.position().distanceTo(c) <= r)) {
            if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) continue;
            if (e instanceof ServerPlayer sp && com.andrey.litrpg.run.Spirits.isSpirit(sp)) continue;
            out.add(e);
        }
        return out;
    }

    /** Союзник: выбранный, под прицелом, самый раненый — или сам. */
    static LivingEntity ally(ServerPlayer p, double r) {
        LivingEntity t = AbilityHandler.allyTarget(p, r, true);
        return t == null ? p : t;
    }

    static boolean boss(Entity e) {
        return e instanceof BossBase;
    }

    // ---------------- действия ----------------

    /** Урон навыка; снимает ванильную неуязвимость, чтобы частые тики не терялись. */
    static boolean dmg(ServerPlayer p, Ability a, LivingEntity e, float amount) {
        e.invulnerableTime = 0;
        return AbilityHandler.hurt(p, a, e, amount);
    }

    static float power(PlayerStats s, Ability a) {
        return AbilityHandler.base(s, a) * a.power;
    }

    static void heal(ServerPlayer p, LivingEntity t, float amount) {
        Dynamic.doHeal(p, t, amount);
    }

    /** Длительность навыка с учётом ранга (зоны, связи, метки). */
    static int dur(PlayerStats s, Ability a) {
        return Math.max(20, Math.round(a.duration * s.rankDuration(a)));
    }

    static void pull(LivingEntity e, Vec3 to, double strength) {
        if (boss(e)) return;
        Vec3 d = to.subtract(e.position());
        if (d.lengthSqr() < 0.5) return;
        e.setDeltaMovement(d.normalize().scale(strength).add(0, 0.15, 0));
        e.hurtMarked = true;
    }

    static void push(LivingEntity e, Vec3 from, double strength, double up) {
        if (boss(e)) return;
        Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-3) d = new Vec3(1, 0, 0);
        e.setDeltaMovement(d.normalize().scale(strength).add(0, up, 0));
        e.hurtMarked = true;
    }

    static void taunt(ServerPlayer p, LivingEntity e) {
        if (e instanceof Mob m) com.andrey.litrpg.event.Threat.taunt(m, p);
    }

    static void stun(LivingEntity e, int ticks) {
        SkillEffects.stun(e, ticks);
    }

    static void sound(ServerLevel l, Vec3 at, SoundEvent ev, float vol, float pitch) {
        l.playSound(null, at.x, at.y, at.z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    static void msg(ServerPlayer p, String text, ChatFormatting color) {
        p.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    /** Чужие снаряды (не игроков) в коробке. */
    static List<Projectile> hostileShots(ServerLevel l, AABB box) {
        return l.getEntitiesOfClass(Projectile.class, box, pr -> !(pr.getOwner() instanceof net.minecraft.world.entity.player.Player)
                && !(pr.getOwner() != null && Minions.ownerOf(pr.getOwner()) != null));
    }

    // ---------------- модификаторы урона ----------------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        float amount = e.getAmount();
        Entity src = e.getSource().getEntity();
        amount *= value(t, TAKEN, 1f);
        if (src instanceof LivingEntity att) amount *= value(att, DEALT, 1f);
        // защитник забирает часть урона на себя
        if (active(t, GUARD) && t.getPersistentData().hasUUID(GUARD + "_by") && t.level() instanceof ServerLevel sl) {
            UUID by = t.getPersistentData().getUUID(GUARD + "_by");
            if (sl.getEntity(by) instanceof LivingEntity g && g.isAlive() && g != t && g.distanceTo(t) < 40) {
                float part = amount * value(t, GUARD, 0f);
                amount -= part;
                g.invulnerableTime = 0;
                g.hurt(g.damageSources().magic(), part);
            }
        }
        if (src instanceof LivingEntity att && att != t) {
            float refl = value(t, REFLECT, 0f);
            // шипы не отражаются: иначе два отражающих перекидывали бы урон бесконечно
            if (refl > 0 && !e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                    && !e.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
                att.invulnerableTime = 0;
                att.hurt(t.damageSources().thorns(t), amount * refl);
            }
            float leech = value(att, LEECH, 0f);
            if (leech > 0) att.heal(amount * leech);
        }
        e.setAmount(Math.max(0, amount));
    }
}
