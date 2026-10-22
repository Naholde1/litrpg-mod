package com.andrey.litrpg.ability;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.entity.ManaBoltEntity.Element;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Style;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Исполнение навыков. Все проверки идут на сервере. */
public class AbilityHandler {
    public static final String ONHIT_TAG = "litrpg_onhit";
    public static final String ONHIT_COUNT_TAG = "litrpg_onhit_n";
    public static final String MARK_TAG = "litrpg_marked_until";
    public static final String REFLECT_TAG = "litrpg_reflect_until";

    public static void tryUse(ServerPlayer player, int slot) {
        PlayerStats s = StatsManager.get(player);
        Ability a = s.barSkill(slot);
        if (a == null) {
            if (!s.hasClass() && s.getLevel() >= PlayerStats.CLASS_LEVEL) {
                player.displayClientMessage(Component.literal("Сначала выбери класс (клавиша K)").withStyle(ChatFormatting.GOLD), true);
            } else if (s.hasClass() && s.getPlayerClass().isInvoker() && slot >= 4) {
                player.displayClientMessage(Component.literal("Ячейка пуста: соберите три сферы и сплетите заклинание")
                        .withStyle(ChatFormatting.GRAY), true);
            } else if (s.hasClass()) {
                player.displayClientMessage(Component.literal("Ячейка пуста — поставьте навык (K → Навыки)").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        long now = player.level().getGameTime();
        int need = Ability.unlockLevel(s.getPlayerClass(), a);
        if (s.getLevel() < need) {
            player.displayClientMessage(Component.literal("Навык откроется на уровне " + need).withStyle(ChatFormatting.RED), true);
            return;
        }
        long end = s.getCooldownEnd(a);
        if (now < end) {
            player.displayClientMessage(Component.literal("Перезарядка: " + String.format("%.1f", (end - now) / 20.0) + " с")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        boolean free = player.isCreative();
        int cost = Math.round(a.mana * (1 - Math.min(40f, s.gear().get(com.andrey.litrpg.gear.GearBonus.MANA_COST)) / 100f));
        float hp = a.hpCost * player.getMaxHealth();
        if (!free && a.hpCost > 0 && player.getHealth() <= hp + 1) {
            player.displayClientMessage(Component.literal("Слишком мало здоровья!").withStyle(ChatFormatting.DARK_RED), true);
            return;
        }
        if (!free && s.getMana() < cost) {
            player.displayClientMessage(Component.literal("Недостаточно маны!").withStyle(ChatFormatting.BLUE), true);
            return;
        }
        // серия: добивание усилено накопленными приёмами
        boost = 1f;
        int spent = 0;
        if (a.finisher) {
            spent = s.getCombo(now);
            boost *= 1 + 0.35f * spent;
        }
        if (a.perFoe) {
            int foes = enemiesAround(player, player.getBoundingBox().inflate(6)).size();
            boost *= 1 + 0.2f * Math.min(5, Math.max(0, foes - 1));
        }
        boolean ok;
        try {
            ok = execute(player, s, a);
        } finally {
            boost = 1f;
        }
        if (ok) {
            if (!free) {
                s.setMana(s.getMana() - cost);
                if (a.hpCost > 0) player.setHealth(Math.max(1f, player.getHealth() - hp));
            }
            if (a.finisher && spent > 0) s.clearCombo();
            if (a.combo > 0) s.addCombo(a.combo, now);
            s.addStyle(Style.MAGIC, a.mana / 4);
            var g = s.gear();
            int cd = Math.round(a.cooldown * (1 - Math.min(50f, g.get(com.andrey.litrpg.gear.GearBonus.COOLDOWN)) / 100f)
                    * (1 - 0.1f * Math.min(4, com.andrey.litrpg.registry.ModEffects.level(player, com.andrey.litrpg.registry.ModEffects.FOCUS))));
            if (g.has(com.andrey.litrpg.gear.Passive.ARCANE_ECHO) && player.getRandom().nextFloat() < 0.15f) {
                cd = 0;
                player.displayClientMessage(Component.literal("Магическое эхо!").withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            s.setCooldownEnd(a, now + cd);
            StatsManager.sync(player);
        }
    }

    /** Только для dev-проверки: применить навык напрямую. @return сработал ли */
    public static boolean devExecute(ServerPlayer p, Ability a) {
        return execute(p, StatsManager.get(p), a);
    }

    /** Урон «от скорости»: во сколько раз вы быстрее обычного шага. */
    public static float speedMult(ServerPlayer p) {
        double v = p.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) / 0.1;
        return (float) Math.max(1.0, Math.min(3.0, v));
    }

    /** Прерывание: сбивает подготовку навыка хранителя, у обычных врагов — короткое оглушение. */
    private static boolean interrupt(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        float dmg = base(s, a) * a.power;
        if (dmg > 0) hurt(p, a, t, dmg);
        else applyFoe(a, t, p);
        ServerLevel level = p.serverLevel();
        if (t instanceof com.andrey.litrpg.entity.BossBase b) {
            boolean done = b.interrupt(p);
            com.andrey.litrpg.LitRPG.LOGGER.debug("[interrupt] {} by {}: casting={} done={} hp={}", b.bossId(), p.getName().getString(),
                    b.isCastingPublic(), done, b.getHealth());
            if (!done) {
                p.displayClientMessage(Component.literal(b.isCastingPublic() ? "Этот навык не прервать (или защита от прерывания)" : "Хранитель ничего не готовит")
                        .withStyle(ChatFormatting.GRAY), true);
            }
        } else {
            SkillEffects.stun(t, 30);
        }
        if (a.radius > 6) line(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.6, 0), ParticleTypes.CRIT);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, t.getX(), t.getY() + 1.5, t.getZ(), 20, 0.3, 0.4, 0.3, 0.2);
        sound(p, SoundEvents.SHIELD_BLOCK, 1f, 1.4f);
        return true;
    }

    /** Боевое воскрешение: поднять павшего (дух) рядом сразу, без долгого обряда. */
    private static boolean rez(ServerPlayer p, PlayerStats s, Ability a) {
        if (!com.andrey.litrpg.entity.BossFight.reviveNear(p, a.radius)) {
            p.displayClientMessage(Component.literal("Рядом нет павших союзников").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        applySelf(a, p);
        return true;
    }

    /** Множитель урона текущего навыка (серия, число врагов). */
    private static float boost = 1f;

    /** Способность предмета (Shift+ПКМ): тратит ману, перезарядку ведёт предмет. */
    public static boolean castFromItem(ServerPlayer player, PlayerStats s, Ability a) {
        if (!player.isCreative() && s.getMana() < a.mana) {
            player.displayClientMessage(Component.literal("Недостаточно маны!").withStyle(ChatFormatting.BLUE), true);
            return false;
        }
        if (!execute(player, s, a)) return false;
        if (!player.isCreative()) s.setMana(s.getMana() - a.mana);
        StatsManager.syncMana(player);
        return true;
    }

    private static boolean execute(ServerPlayer p, PlayerStats s, Ability a) {
        return switch (a.kind) {
            case DASH -> dash(p, a);
            case DASH_STRIKE -> dashStrike(p, s, a);
            case LEAP_SLAM -> leapSlam(p, s, a);
            case NOVA -> { nova(p, s, a, p.position(), a.radius, 1f); yield true; }
            case MULTI_NOVA -> multiNova(p, s, a);
            case STRIKE -> strike(p, s, a);
            case MULTI_STRIKE -> multiStrike(p, s, a);
            case BOLT -> bolt(p, s, a);
            case CHAIN -> chain(p, s, a);
            case LIGHTNING -> lightning(p, s, a);
            case METEOR -> meteor(p, s, a);
            case ZONE -> zone(p, s, a, p.position());
            case ZONE_AT -> zone(p, s, a, lookPoint(p, 32));
            case ARROWS -> arrows(p, s, a);
            case BARRAGE -> barrage(p, s, a);
            case ARROW_RAIN -> arrowRain(p, s, a);
            case BUFF_SELF -> buffSelf(p, a);
            case BUFF_ALLIES -> buffAllies(p, a);
            case HEAL -> heal(p, s, a);
            case HEAL_SELF -> healSelf(p, a);
            case MEDITATE -> meditate(p, s, a);
            case TAUNT -> taunt(p, a);
            case PULL -> pull(p, a);
            case BLINK -> blink(p, a);
            case SHADOW_STEP -> shadowStep(p, a);
            case STEALTH -> stealth(p, a);
            case ON_HIT -> onHit(p, a);
            case MARK -> mark(p, a);
            case PURIFY -> purify(p, s, a);
            case SUMMON -> Minions.summon(p, s, a);
            case BEAM -> beam(p, s, a);
            case CONE -> cone(p, s, a);
            case VORTEX -> vortex(p, s, a);
            case LAUNCH -> launch(p, s, a);
            case PUSH -> push(p, s, a);
            case STUN -> stun(p, s, a);
            case STASIS -> stasis(p, a);
            case WALL -> wall(p, a);
            case SWAP -> swap(p, s, a);
            case RECALL -> recall(p, a);
            case GRAPPLE -> grapple(p, a);
            case CHARM -> charm(p, a);
            case DOMINATE -> dominate(p, s, a);
            case REVEAL -> reveal(p, a);
            case STEAL -> steal(p, s, a);
            case RESET -> reset(p, s, a);
            case SACRIFICE -> sacrifice(p, s, a);
            case MINE -> mine(p, a);
            case TETHER -> tether(p, s, a);
            case WARD -> ward(p, a);
            case SHIELD -> shield(p, s, a);
            case HEAL_TARGET -> healTarget(p, s, a);
            case GLOBAL -> global(p, s, a);
            case RALLY -> rally(p, a);
            case GATHER -> gather(p, a);
            case ORB -> orb(p, s, a);
            case INVOKE -> invoke(p, s);
            case INTERRUPT -> interrupt(p, s, a);
            case REZ -> rez(p, s, a);
        };
    }

    // ================= урон =================

    /** Базовая сила навыка от его характеристики и уровня. */
    public static float base(PlayerStats s, Ability a) {
        float k = switch (a.scale) {
            case AGILITY -> 0.75f;
            case INTELLIGENCE -> 0.8f;
            case VITALITY -> 0.6f;
            default -> 0.7f;
        };
        float v = 4 + s.getStat(a.scale) * k + s.getLevel() * 0.5f;
        if (s.getRace() == Race.DEMONKIN) v *= 1.1f;
        return v * boost;
    }

    private static DamageSource source(ServerPlayer p, Ability a) {
        return a.element == null ? p.damageSources().playerAttack(p) : p.damageSources().indirectMagic(p, p);
    }

    /** Нанести урон навыка с учётом стихии. @return нанесён ли урон */
    private static boolean hurt(ServerPlayer p, Ability a, LivingEntity e, float dmg) {
        Element el = a.element;
        if (a.speedScale) dmg *= speedMult(p);
        if (el == Element.HOLY && e.getMobType() == MobType.UNDEAD) dmg *= 2f;
        if (el == Element.FIRE) e.setSecondsOnFire(4);
        boolean ok = e.hurt(source(p, a), dmg);
        if (ok && a.lifesteal && a.kind != Ability.Kind.STRIKE) p.heal(dmg * 0.2f);
        if (ok) {
            if (el == Element.FROST) e.setTicksFrozen(Math.max(e.getTicksFrozen(), 160));
            if (el == Element.POISON) e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            if (el == Element.SHADOW) e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            applyFoe(a, e, p);
        }
        return ok;
    }

    private static void applyFoe(Ability a, LivingEntity e, ServerPlayer p) {
        for (Ability.Fx fx : a.enemy) e.addEffect(new MobEffectInstance(fx.effect(), fx.duration(), fx.amplifier()), p);
    }

    private static void applySelf(Ability a, LivingEntity e) {
        for (Ability.Fx fx : a.self) e.addEffect(new MobEffectInstance(fx.effect(), fx.duration(), fx.amplifier()));
    }

    private static ParticleOptions particle(Element el) {
        if (el == null) return ParticleTypes.CRIT;
        return switch (el) {
            case FIRE -> ParticleTypes.FLAME;
            case FROST -> ParticleTypes.SNOWFLAKE;
            case HOLY -> ParticleTypes.END_ROD;
            case SHADOW -> ParticleTypes.SQUID_INK;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case POISON -> ParticleTypes.ITEM_SLIME;
            case NATURE -> ParticleTypes.COMPOSTER;
            case EARTH -> ParticleTypes.ASH;
            default -> ParticleTypes.ENCHANT;
        };
    }

    // ================= движение =================

    private static boolean dash(ServerPlayer p, Ability a) {
        Vec3 dir = flatLook(p).scale(a.radius);
        p.setDeltaMovement(dir.x, a.radius < 0 ? 0.6 : 0.3, dir.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false));
        if (!a.enemy.isEmpty()) {
            for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(4))) applyFoe(a, e, p);
        }
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 15, 0.3, 0.1, 0.3, 0.05);
        sound(p, SoundEvents.PHANTOM_FLAP, 1.0f, 1.4f);
        return true;
    }

    private static boolean dashStrike(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 dir = flatLook(p);
        if (a.phase) SkillEffects.phase(p, 20);
        p.setDeltaMovement(dir.x * a.radius, 0.25, dir.z * a.radius);
        p.hurtMarked = true;
        p.fallDistance = 0;
        float dmg = base(s, a) * a.power;
        ServerLevel level = p.serverLevel();
        List<Entity> hit = new ArrayList<>();
        Scheduler.repeat(1, 1, 8, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(1.3, 0.6, 1.3))) {
                if (hit.contains(e)) continue;
                hit.add(e);
                hurt(p, a, e, dmg);
                e.knockback(0.8, -dir.x, -dir.z);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
            }
            level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.3, p.getZ(), 4, 0.3, 0.1, 0.3, 0.02);
            return true;
        });
        sound(p, SoundEvents.RAVAGER_ATTACK, 1.0f, 1.3f);
        return true;
    }

    private static boolean leapSlam(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 dir = flatLook(p);
        p.setDeltaMovement(dir.x * 1.1, 0.95, dir.z * 1.1);
        p.hurtMarked = true;
        boolean[] done = {false};
        Scheduler.repeat(6, 1, 50, i -> {
            if (done[0] || !p.isAlive()) return false;
            p.fallDistance = 0;
            if (p.onGround()) {
                done[0] = true;
                nova(p, s, a, p.position(), a.radius, 1f);
                p.serverLevel().sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 5, 1.5, 0.2, 1.5, 0);
                sound(p, SoundEvents.GENERIC_EXPLODE, 0.9f, 1.1f);
                return false;
            }
            return true;
        });
        sound(p, SoundEvents.RAVAGER_ROAR, 0.7f, 1.4f);
        return true;
    }

    private static boolean blink(ServerPlayer p, Ability a) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        BlockHitResult bh = p.level().clip(new ClipContext(eye, eye.add(look.scale(a.radius)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 dest = bh.getType() == HitResult.Type.MISS ? eye.add(look.scale(a.radius)) : bh.getLocation().subtract(look.scale(0.8));
        BlockPos feet = BlockPos.containing(dest.x, dest.y - 1.0, dest.z);
        for (int dy = 0; dy < 4; dy++) {
            BlockPos at = feet.above(dy);
            if (p.level().noCollision(p, p.getBoundingBox().move(at.getX() + 0.5 - p.getX(), at.getY() - p.getY(), at.getZ() + 0.5 - p.getZ()))) {
                ServerLevel level = p.serverLevel();
                level.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.8, 0.3, 0.3);
                p.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                p.fallDistance = 0;
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.8, 0.3, 0.1);
                applySelf(a, p);
                sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.8f, 1.3f);
                return true;
            }
        }
        p.displayClientMessage(Component.literal("Некуда переместиться").withStyle(ChatFormatting.YELLOW), true);
        return false;
    }

    private static boolean shadowStep(ServerPlayer p, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        Vec3 behind = t.position().subtract(Vec3.directionFromRotation(0, t.getYRot()).scale(1.3));
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        p.teleportTo(behind.x, t.getY(), behind.z);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, t.getEyePosition());
        p.connection.teleport(behind.x, t.getY(), behind.z, p.getYRot(), p.getXRot());
        level.sendParticles(ParticleTypes.SQUID_INK, p.getX(), p.getY() + 1, p.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 0.6f, 0.6f);
        return true;
    }

    private static boolean pull(ServerPlayer p, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        Vec3 d = p.position().subtract(t.position());
        double dist = d.length();
        Vec3 v = d.normalize().scale(Math.min(2.2, 0.35 + dist * 0.12));
        t.setDeltaMovement(v.x, 0.35 + dist * 0.02, v.z);
        t.hurtMarked = true;
        applyFoe(a, t, p);
        if (t instanceof Mob m) com.andrey.litrpg.event.Threat.taunt(m, p);
        line(p.serverLevel(), p.getEyePosition(), t.position().add(0, 1, 0), ParticleTypes.SOUL_FIRE_FLAME);
        sound(p, SoundEvents.FISHING_BOBBER_RETRIEVE, 1.0f, 0.6f);
        return true;
    }

    // ================= удары =================

    public static void nova(ServerPlayer p, PlayerStats s, Ability a, Vec3 c, float r, float mult) {
        float dmg = base(s, a) * a.power * mult;
        AABB box = new AABB(c, c).inflate(r, 2.5, r);
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e))) {
            if (dmg > 0) {
                e.invulnerableTime = 0;
                hurt(p, a, e, dmg);
            } else {
                applyFoe(a, e, p);
            }
            Vec3 push = e.position().subtract(c).normalize().scale(0.6);
            e.push(push.x, 0.3, push.z);
            e.hurtMarked = true;
        }
        ServerLevel level = p.serverLevel();
        ParticleOptions part = particle(a.element);
        for (float rr = 1; rr <= r; rr += 1.5f) ring(level, c.add(0, 0.2, 0), rr, part, (int) (rr * 8));
        sound(p, a.element == Element.FROST ? SoundEvents.GLASS_BREAK : SoundEvents.GENERIC_EXPLODE, 0.6f, 1.4f);
    }

    private static boolean multiNova(ServerPlayer p, PlayerStats s, Ability a) {
        Scheduler.repeat(1, 5, a.count, i -> {
            if (!p.isAlive()) return false;
            nova(p, s, a, p.position(), a.radius, 1f);
            p.serverLevel().sendParticles(ParticleTypes.SWEEP_ATTACK, p.getX(), p.getY() + 1, p.getZ(), 4, a.radius / 2, 0.2, a.radius / 2, 0);
            return true;
        });
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, a.count * 5, 0, false, false));
        return true;
    }

    private static boolean strike(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        float dmg = base(s, a) * a.power;
        String note = null;
        if (a.execute && t.getHealth() < t.getMaxHealth() * 0.35f) {
            dmg *= 2.5f;
            note = "Казнь! x2.5";
        }
        if (a.backstab && isBehind(p, t)) {
            dmg *= 2f;
            note = "Удар в спину! x2";
        }
        if (hurt(p, a, t, dmg) && a.lifesteal) {
            p.heal(dmg * 0.5f);
            p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.5, p.getZ(), 3, 0.3, 0.3, 0.3, 0);
        }
        ServerLevel level = p.serverLevel();
        if (a.radius > 6) line(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.6, 0), particle(a.element));
        level.sendParticles(particle(a.element), t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1, 0, 0, 0, 0);
        if (note != null) {
            level.sendParticles(new DustParticleOptions(new Vector3f(0.8f, 0f, 0f), 1.5f), t.getX(), t.getY() + 1, t.getZ(), 30, 0.4, 0.5, 0.4, 0);
            p.displayClientMessage(Component.literal(note).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
        }
        sound(p, a.element == Element.HOLY ? SoundEvents.TRIDENT_THUNDER : SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
        return true;
    }

    private static boolean multiStrike(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        float dmg = base(s, a) * a.power;
        ServerLevel level = p.serverLevel();
        Scheduler.repeat(1, 3, a.count, i -> {
            double reach = Math.max(7, a.radius + 2);
            if (!p.isAlive() || !t.isAlive() || t.distanceToSqr(p) > reach * reach) return false;
            t.invulnerableTime = 0;
            hurt(p, a, t, dmg);
            level.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 1, t.getZ(), 6, 0.3, 0.4, 0.3, 0.2);
            level.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.5f, 1.2f + i * 0.04f);
            return true;
        });
        return true;
    }

    // ================= магия =================

    private static boolean bolt(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        ManaBoltEntity bolt = new ManaBoltEntity(level, p, look.x, look.y, look.z, a.element);
        bolt.setPos(p.getX() + look.x * 0.8, p.getEyeY() - 0.15 + look.y * 0.8, p.getZ() + look.z * 0.8);
        double speed = a.element == Element.EARTH ? 1.2 : a.power >= 1.5f ? 1.5 : 2.2;
        bolt.setDeltaMovement(look.scale(speed));
        bolt.xPower = look.x * 0.1;
        bolt.yPower = look.y * 0.1;
        bolt.zPower = look.z * 0.1;
        bolt.setDamage(base(s, a) * a.power);
        level.addFreshEntity(bolt);
        sound(p, a.element == Element.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.6f);
        return true;
    }

    private static boolean chain(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, 24);
        if (!isEnemy(p, t)) return noTarget(p);
        ServerLevel level = p.serverLevel();
        float dmg = base(s, a) * a.power;
        List<LivingEntity> hit = new ArrayList<>();
        Vec3 from = p.getEyePosition();
        LivingEntity cur = t;
        for (int i = 0; i < a.count && cur != null; i++) {
            hit.add(cur);
            Vec3 to = cur.position().add(0, cur.getBbHeight() * 0.6, 0);
            line(level, from, to, ParticleTypes.ELECTRIC_SPARK);
            cur.invulnerableTime = 0;
            hurt(p, a, cur, dmg);
            dmg *= 0.85f;
            from = to;
            LivingEntity next = null;
            double best = a.radius * a.radius;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(a.radius), e -> isEnemy(p, e))) {
                if (hit.contains(e)) continue;
                double d = e.distanceToSqr(cur);
                if (d < best) { best = d; next = e; }
            }
            cur = next;
        }
        sound(p, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.6f);
        return true;
    }

    private static boolean lightning(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        Vec3 at = t != null && isEnemy(p, t) ? t.position() : lookPoint(p, a.radius);
        strikeAt(p, s, a, at, 1f);
        return true;
    }

    private static void strikeAt(ServerPlayer p, PlayerStats s, Ability a, Vec3 at, float mult) {
        ServerLevel level = p.serverLevel();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        float dmg = base(s, a) * a.power * mult;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(2.5), e -> isEnemy(p, e))) {
            e.invulnerableTime = 0;
            hurt(p, a, e, dmg);
        }
    }

    private static boolean meteor(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 target = lookPoint(p, 32);
        ServerLevel level = p.serverLevel();
        ParticleOptions trail = a.element == Element.HOLY ? ParticleTypes.END_ROD
                : a.element == Element.SHADOW ? ParticleTypes.SQUID_INK
                : a.element == Element.EARTH ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.FLAME;
        level.playSound(null, target.x, target.y, target.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 2f, 0.5f);
        Scheduler.repeat(1, 1, 30, i -> {
            double h = 20 - i * (20.0 / 30);
            level.sendParticles(trail, target.x, target.y + h, target.z, 12, 0.6, 0.6, 0.6, 0.02);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, target.x, target.y + h + 1, target.z, 4, 0.4, 0.4, 0.4, 0.01);
            ring(level, target.add(0, 0.1, 0), a.radius, particle(a.element), 20);
            if (i == 29) {
                nova(p, s, a, target, a.radius, 1f);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, target.x, target.y, target.z, 2, 1, 0.2, 1, 0);
                level.playSound(null, target.x, target.y, target.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3f, 0.6f);
            }
            return true;
        });
        return true;
    }

    /** Зона: каждую секунду урон врагам; Свет и Природа ещё и лечат союзников. */
    private static boolean zone(ServerPlayer p, PlayerStats s, Ability a, Vec3 center) {
        ServerLevel level = p.serverLevel();
        float b = base(s, a);
        float dmg = b * a.power;
        boolean heals = a.element == Element.HOLY || a.element == Element.NATURE || a.power < 0;
        float healAmount = b * Math.abs(a.power) * 0.6f * healMult(s);
        int ticks = Math.max(1, a.duration / 20);
        ParticleOptions part = particle(a.element);
        Scheduler.repeat(1, 20, ticks, i -> {
            AABB box = new AABB(center, center).inflate(a.radius, 3, a.radius);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
                if (heals && isAlly(p, e)) {
                    e.heal(healAmount);
                } else if (dmg > 0 && isEnemy(p, e)) {
                    if (a.element == Element.LIGHTNING) continue;
                    e.invulnerableTime = 0;
                    hurt(p, a, e, dmg);
                }
            }
            if (a.element == Element.LIGHTNING) {
                List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e));
                if (!foes.isEmpty()) strikeAt(p, s, a, foes.get(p.getRandom().nextInt(foes.size())).position(), 1f);
            }
            ring(level, center.add(0, 0.15, 0), a.radius, part, (int) (a.radius * 6));
            level.sendParticles(part, center.x, center.y + 0.5, center.z, (int) (a.radius * 5), a.radius / 2, 0.5, a.radius / 2, 0.01);
            return true;
        });
        sound(p, heals ? SoundEvents.BEACON_ACTIVATE : SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.3f);
        return true;
    }

    private static boolean purify(ServerPlayer p, PlayerStats s, Ability a) {
        for (LivingEntity ally : alliesAround(p, a.radius)) {
            List<MobEffect> bad = new ArrayList<>();
            for (MobEffectInstance inst : ally.getActiveEffects()) {
                if (inst.getEffect().getCategory() == MobEffectCategory.HARMFUL) bad.add(inst.getEffect());
            }
            bad.forEach(ally::removeEffect);
            ally.clearFire();
            ally.heal(base(s, a) * 0.25f * healMult(s));
        }
        float dmg = base(s, a) * a.power;
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius))) {
            if (e.getMobType() == MobType.UNDEAD) {
                hurt(p, a, e, dmg);
                e.setSecondsOnFire(3);
            }
        }
        ring(p.serverLevel(), p.position().add(0, 0.2, 0), a.radius, ParticleTypes.END_ROD, 48);
        sound(p, SoundEvents.ZOMBIE_VILLAGER_CURE, 0.8f, 1.6f);
        return true;
    }

    // ================= стрелы =================

    private static Arrow arrow(ServerPlayer p, PlayerStats s, Ability a, float yawOffset, float speed) {
        ServerLevel level = p.serverLevel();
        Arrow arrow = new Arrow(level, p);
        arrow.shootFromRotation(p, p.getXRot(), p.getYRot() + yawOffset, 0f, speed, 1.0f);
        arrow.setBaseDamage(base(s, a) * 0.12 * a.power);
        arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        arrow.setCritArrow(true);
        if (a.pierce) arrow.setPierceLevel((byte) 5);
        for (Ability.Fx fx : a.enemy) arrow.addEffect(new MobEffectInstance(fx.effect(), fx.duration(), fx.amplifier()));
        level.addFreshEntity(arrow);
        return arrow;
    }

    private static boolean arrows(ServerPlayer p, PlayerStats s, Ability a) {
        float step = a.count > 1 ? Math.min(40f, a.count * 6f) / (a.count - 1) : 0;
        float start = -step * (a.count - 1) / 2f;
        float speed = a.power >= 2.5f ? 4.2f : 3.0f;
        for (int i = 0; i < a.count; i++) arrow(p, s, a, start + i * step, speed);
        sound(p, a.count > 1 ? SoundEvents.ARROW_SHOOT : SoundEvents.CROSSBOW_SHOOT, 1.0f, 0.9f);
        return true;
    }

    private static boolean barrage(ServerPlayer p, PlayerStats s, Ability a) {
        Scheduler.repeat(1, 4, a.count, i -> {
            if (!p.isAlive()) return false;
            arrow(p, s, a, (p.getRandom().nextFloat() - 0.5f) * 4f, 3.2f);
            sound(p, SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.3f);
            return true;
        });
        return true;
    }

    private static boolean arrowRain(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 target = lookPoint(p, 40);
        ServerLevel level = p.serverLevel();
        double dmg = base(s, a) * 0.15 * a.power;
        Scheduler.repeat(1, 2, a.duration / 2, i -> {
            for (int k = 0; k < 3; k++) {
                double ox = (p.getRandom().nextDouble() - 0.5) * a.radius * 2;
                double oz = (p.getRandom().nextDouble() - 0.5) * a.radius * 2;
                Arrow arrow = new Arrow(level, p);
                arrow.setPos(target.x + ox, target.y + 14, target.z + oz);
                arrow.setDeltaMovement(0, -2.5, 0);
                arrow.setBaseDamage(dmg);
                arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
                level.addFreshEntity(arrow);
            }
            return p.isAlive();
        });
        sound(p, SoundEvents.ARROW_SHOOT, 1.0f, 0.5f);
        return true;
    }

    // ================= баффы и поддержка =================

    private static boolean buffSelf(ServerPlayer p, Ability a) {
        applySelf(a, p);
        if (a.reflect) p.getPersistentData().putLong(REFLECT_TAG, p.level().getGameTime() + a.duration);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
        sound(p, SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 1.0f);
        return true;
    }

    private static boolean buffAllies(ServerPlayer p, Ability a) {
        for (LivingEntity ally : alliesAround(p, a.radius)) {
            applySelf(a, ally);
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, ally.getX(), ally.getY() + 1, ally.getZ(), 10, 0.4, 0.6, 0.4, 0.1);
        }
        ring(p.serverLevel(), p.position().add(0, 0.2, 0), a.radius, ParticleTypes.NOTE, 24);
        sound(p, SoundEvents.BEACON_POWER_SELECT, 1.0f, 1.5f);
        return true;
    }

    private static boolean heal(ServerPlayer p, PlayerStats s, Ability a) {
        float amount = base(s, a) * a.power * 0.8f * healMult(s);
        boolean any = false;
        for (LivingEntity ally : alliesAround(p, a.radius)) {
            if (ally.getHealth() < ally.getMaxHealth()) any = true;
            float before = ally.getHealth();
            ally.heal(ally == p ? amount : amount * 0.8f);
            com.andrey.litrpg.event.Threat.healed(p, ally, ally.getHealth() - before);
            ally.removeEffect(MobEffects.POISON);
            p.serverLevel().sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.2, ally.getZ(), 5, 0.4, 0.4, 0.4, 0.1);
        }
        if (!any) {
            p.displayClientMessage(Component.literal("Все здоровы").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        StatsManager.get(p).addStyle(Style.SUPPORT, 5);
        sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);
        return true;
    }

    private static boolean healSelf(ServerPlayer p, Ability a) {
        if (p.getHealth() >= p.getMaxHealth()) {
            p.displayClientMessage(Component.literal("Здоровье уже полное").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        p.heal(p.getMaxHealth() * a.power);
        applySelf(a, p);
        p.serverLevel().sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 6, 0.4, 0.4, 0.4, 0.1);
        sound(p, SoundEvents.PLAYER_BREATH, 1.0f, 1.0f);
        return true;
    }

    private static boolean meditate(ServerPlayer p, PlayerStats s, Ability a) {
        s.setMana(s.getMana() + s.getMaxMana() * a.power);
        p.heal(p.getMaxHealth() * 0.2f);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.5);
        sound(p, SoundEvents.BEACON_AMBIENT, 1.0f, 1.5f);
        return true;
    }

    private static boolean taunt(ServerPlayer p, Ability a) {
        applySelf(a, p);
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius))) {
            applyFoe(a, e, p);
            if (e instanceof Mob m) com.andrey.litrpg.event.Threat.taunt(m, p);
        }
        ring(p.serverLevel(), p.position(), a.radius * 0.6, ParticleTypes.ANGRY_VILLAGER, 24);
        sound(p, SoundEvents.RAID_HORN.get(), 0.8f, 1.4f);
        return true;
    }

    private static boolean stealth(ServerPlayer p, Ability a) {
        if (a.radius > 0) {
            for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius))) applyFoe(a, e, p);
        }
        Stealth.hide(p, a.duration);
        applySelf(a, p);
        p.serverLevel().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY() + 1, p.getZ(), 40, 1.5, 1.0, 1.5, 0.01);
        sound(p, SoundEvents.FIRE_EXTINGUISH, 1.0f, 0.7f);
        return true;
    }

    private static boolean onHit(ServerPlayer p, Ability a) {
        p.getPersistentData().putInt(ONHIT_TAG, a.onHit.ordinal());
        p.getPersistentData().putInt(ONHIT_COUNT_TAG, a.count + StatsManager.get(p).getLevel() / 10);
        applySelf(a, p);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.5, 0.4, 0.3);
        p.displayClientMessage(Component.translatable(a.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(": " + a.count + " ударов").withStyle(ChatFormatting.GRAY)), true);
        sound(p, SoundEvents.BREWING_STAND_BREW, 1.0f, 1.4f);
        return true;
    }

    private static boolean mark(ServerPlayer p, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        t.getPersistentData().putLong(MARK_TAG, p.level().getGameTime() + a.duration);
        t.addEffect(new MobEffectInstance(MobEffects.GLOWING, a.duration, 0));
        p.serverLevel().sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() + 0.5, t.getZ(), 20, 0.3, 0.3, 0.3, 0.1);
        sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 0.6f);
        return true;
    }

    // ================= новые виды (2.15) =================

    /** Лечение союзников: настоящий лекарь — только поддержка; гибриды (паладин, друид-боец) лечат вполсилы. */
    public static float healMult(PlayerStats s) {
        if (s.getRole() == null) return 1f;
        return switch (s.getRole()) {
            case SUPPORT -> 2.2f;
            case DAMAGE -> 0.55f;
            case TANK -> 0.45f;
        };
    }

    /** Луч: урон всем на линии до первого блока. */
    private static boolean beam(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(a.radius));
        BlockHitResult bh = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
        final Vec3 to = end;
        float dmg = base(s, a) * a.power;
        AABB box = new AABB(eye, to).inflate(1.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e))) {
            AABB bb = e.getBoundingBox().inflate(0.6);
            if (bb.clip(eye, to).isEmpty() && !bb.contains(eye)) continue;
            e.invulnerableTime = 0;
            hurt(p, a, e, dmg);
        }
        line(level, eye.add(0, -0.2, 0), to, particle(a.element));
        line(level, eye.add(0, -0.25, 0), to, ParticleTypes.END_ROD);
        sound(p, a.element == Element.FROST ? SoundEvents.GLASS_BREAK : SoundEvents.BEACON_DEACTIVATE, 0.8f, 1.8f);
        return true;
    }

    /** Конус перед собой. */
    private static boolean cone(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        float dmg = base(s, a) * a.power;
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius, 2, a.radius))) {
            Vec3 to = e.position().subtract(p.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.length() > a.radius || flat.lengthSqr() > 0.5 && flat.normalize().dot(dir) < 0.6) continue;
            e.invulnerableTime = 0;
            if (dmg > 0) hurt(p, a, e, dmg);
            else applyFoe(a, e, p);
        }
        ParticleOptions part = a.element == null ? ParticleTypes.CLOUD : particle(a.element);
        for (int i = 0; i < 40; i++) {
            double d = 1 + p.getRandom().nextDouble() * (a.radius - 1);
            double ang = (p.getRandom().nextDouble() - 0.5) * 1.6;
            Vec3 v = new Vec3(dir.x * Math.cos(ang) - dir.z * Math.sin(ang), 0, dir.x * Math.sin(ang) + dir.z * Math.cos(ang)).scale(d);
            level.sendParticles(part, p.getX() + v.x, p.getEyeY() - 0.4, p.getZ() + v.z, 1, 0.1, 0.2, 0.1, 0.01);
        }
        sound(p, a.element == Element.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.ENDER_DRAGON_FLAP, 0.9f, 1.2f);
        return true;
    }

    /** Воронка: стягивает врагов к точке. */
    private static boolean vortex(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 c = lookPoint(p, 32);
        float dmg = base(s, a) * a.power;
        ParticleOptions part = a.element == null ? ParticleTypes.PORTAL : particle(a.element);
        Scheduler.repeat(1, 2, Math.max(1, a.duration / 2), i -> {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(a.radius, 3, a.radius), e -> isEnemy(p, e))) {
                Vec3 v = c.subtract(e.position());
                if (v.length() > 0.8) {
                    Vec3 pull = v.normalize().scale(0.28);
                    e.setDeltaMovement(e.getDeltaMovement().scale(0.5).add(pull.x, 0.02, pull.z));
                    e.hurtMarked = true;
                }
                if (i % 10 == 0) {
                    if (dmg > 0) {
                        e.invulnerableTime = 0;
                        hurt(p, a, e, dmg);
                    } else {
                        applyFoe(a, e, p);
                    }
                }
            }
            double ang = i * 0.5;
            for (int k = 0; k < 4; k++) {
                double r = a.radius * (1 - ((i + k * 5) % 20) / 20.0);
                double aa = ang + k * Math.PI / 2;
                level.sendParticles(part, c.x + Math.cos(aa) * r, c.y + 0.3, c.z + Math.sin(aa) * r, 1, 0, 0.1, 0, 0);
            }
            return p.isAlive();
        });
        sound(p, SoundEvents.PORTAL_TRIGGER, 0.6f, 1.6f);
        return true;
    }

    /** Подбросить врагов вокруг. */
    private static boolean launch(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        float dmg = base(s, a) * a.power;
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius, 2, a.radius))) {
            e.invulnerableTime = 0;
            if (dmg > 0) hurt(p, a, e, dmg);
            else applyFoe(a, e, p);
            if (e instanceof com.andrey.litrpg.entity.BossBase b) b.interrupt(p);
            if (!(e instanceof com.andrey.litrpg.entity.BossBase)) {
                e.setDeltaMovement(e.getDeltaMovement().x * 0.3, 1.0, e.getDeltaMovement().z * 0.3);
                e.hurtMarked = true;
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 4, a.radius / 2, 0.1, a.radius / 2, 0);
        ring(level, p.position().add(0, 0.2, 0), a.radius, ParticleTypes.CLOUD, (int) (a.radius * 8));
        sound(p, SoundEvents.GENERIC_EXPLODE, 0.7f, 0.8f);
        return true;
    }

    /** Отбросить врагов перед собой. */
    private static boolean push(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 dir = flatLook(p);
        float dmg = base(s, a) * a.power;
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius, 2, a.radius))) {
            Vec3 to = e.position().subtract(p.position());
            Vec3 flat = new Vec3(to.x, 0, to.z);
            if (flat.length() > a.radius || flat.lengthSqr() > 1 && flat.normalize().dot(dir) < 0.35) continue;
            e.invulnerableTime = 0;
            if (dmg > 0) hurt(p, a, e, dmg);
            else applyFoe(a, e, p);
            if (e instanceof com.andrey.litrpg.entity.BossBase b) b.interrupt(p);
            Vec3 kb = flat.lengthSqr() < 0.01 ? dir : flat.normalize();
            e.setDeltaMovement(kb.x * 1.7, 0.45, kb.z * 1.7);
            e.hurtMarked = true;
        }
        for (int i = 1; i <= a.radius; i++) {
            Vec3 c = p.position().add(dir.scale(i)).add(0, 1, 0);
            level.sendParticles(a.element == null ? ParticleTypes.CLOUD : particle(a.element), c.x, c.y, c.z, 6, 0.6, 0.4, 0.6, 0.02);
        }
        sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 0.9f);
        return true;
    }

    /** Оглушить цель под прицелом. */
    private static boolean stun(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        float dmg = base(s, a) * a.power;
        if (dmg > 0) hurt(p, a, t, dmg);
        else applyFoe(a, t, p);
        SkillEffects.stun(t, a.duration);
        if (t instanceof com.andrey.litrpg.entity.BossBase b) b.interrupt(p);
        ServerLevel level = p.serverLevel();
        if (a.radius > 6) line(level, p.getEyePosition(), t.position().add(0, t.getBbHeight() * 0.6, 0), particle(a.element));
        level.sendParticles(ParticleTypes.FLASH, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
        sound(p, SoundEvents.ANVIL_LAND, 0.5f, 1.6f);
        return true;
    }

    /** Остановка времени в области. */
    private static boolean stasis(ServerPlayer p, Ability a) {
        ServerLevel level = p.serverLevel();
        Vec3 c = lookPoint(p, 32);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(a.radius, 3, a.radius), e -> isEnemy(p, e))) {
            SkillEffects.stun(e, a.duration);
            applyFoe(a, e, p);
        }
        Scheduler.repeat(1, 10, Math.max(1, a.duration / 10), i -> {
            ring(level, c.add(0, 0.2, 0), a.radius, ParticleTypes.END_ROD, (int) (a.radius * 6));
            ring(level, c.add(0, 2.2, 0), a.radius * 0.8, ParticleTypes.REVERSE_PORTAL, (int) (a.radius * 5));
            return true;
        });
        sound(p, SoundEvents.BEACON_DEACTIVATE, 1f, 0.5f);
        return true;
    }

    /** Временная стена перед собой. */
    private static boolean wall(ServerPlayer p, Ability a) {
        ServerLevel level = p.serverLevel();
        net.minecraft.world.level.block.Block block = a.wall == null ? null
                : net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation(a.wall));
        if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) block = net.minecraft.world.level.block.Blocks.COBBLESTONE;
        net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.LeavesBlock.PERSISTENT)) {
            state = state.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        }
        Vec3 dir = flatLook(p);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        Vec3 center = p.position().add(dir.scale(3));
        int half = Math.max(1, Math.round(a.radius / 2));
        int placed = 0;
        for (int w = -half; w <= half; w++) {
            Vec3 at = center.add(side.scale(w));
            BlockPos base = BlockPos.containing(at.x, p.getY(), at.z);
            for (int k = 0; k < 3 && level.getBlockState(base.below()).isAir(); k++) base = base.below();
            for (int h = 0; h < 3; h++) {
                if (SkillEffects.placeWall(level, base.above(h), state, a.duration)) placed++;
            }
        }
        if (placed == 0) {
            p.displayClientMessage(Component.literal("Здесь не поставить стену").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        sound(p, SoundEvents.STONE_PLACE, 1f, 0.6f);
        return true;
    }

    /** Поменяться местами с целью. */
    private static boolean swap(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookTarget(p, a.radius);
        if (t == null || t == p || t instanceof Player) return noTarget(p);
        Vec3 mine = p.position();
        Vec3 theirs = t.position();
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, mine.x, mine.y + 1, mine.z, 30, 0.3, 0.6, 0.3, 0.05);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, theirs.x, theirs.y + 1, theirs.z, 30, 0.3, 0.6, 0.3, 0.05);
        p.teleportTo(theirs.x, theirs.y, theirs.z);
        t.teleportTo(mine.x, mine.y, mine.z);
        p.fallDistance = 0;
        if (isEnemy(p, t)) {
            float dmg = base(s, a) * a.power;
            if (dmg > 0) hurt(p, a, t, dmg);
            else applyFoe(a, t, p);
        }
        applySelf(a, p);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 0.8f);
        return true;
    }

    /** Вернуться туда, где был 3 секунды назад. */
    private static boolean recall(ServerPlayer p, Ability a) {
        Vec3 back = SkillEffects.past(p);
        if (back == null) return false;
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.8, 0.3, 0.1);
        line(level, p.position().add(0, 1, 0), back.add(0, 1, 0), ParticleTypes.ELECTRIC_SPARK);
        p.teleportTo(back.x, back.y, back.z);
        p.fallDistance = 0;
        p.heal(p.getMaxHealth() * a.power);
        if (a.group) {
            // откат времени для всех рядом: раны союзников затягиваются
            for (LivingEntity ally : alliesAround(p, a.radius)) {
                if (ally == p) continue;
                float before = ally.getHealth();
                ally.heal(ally.getMaxHealth() * a.power * 0.6f);
                com.andrey.litrpg.event.Threat.healed(p, ally, ally.getHealth() - before);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, ally.getX(), ally.getY() + 1, ally.getZ(), 25, 0.3, 0.7, 0.3, 0.05);
            }
            ring(level, p.position().add(0, 0.2, 0), a.radius, ParticleTypes.END_ROD, (int) (a.radius * 6));
        }
        applySelf(a, p);
        sound(p, SoundEvents.CHORUS_FRUIT_TELEPORT, 1f, 1.4f);
        return true;
    }

    /** Крюк к блоку под прицелом. */
    private static boolean grapple(ServerPlayer p, Ability a) {
        Vec3 eye = p.getEyePosition();
        BlockHitResult bh = p.level().clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(a.radius)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (bh.getType() == HitResult.Type.MISS) {
            p.displayClientMessage(Component.literal("Крюку не за что зацепиться").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        Vec3 to = bh.getLocation().subtract(p.position());
        Vec3 v = to.normalize().scale(Math.min(3.2, 0.6 + to.length() * 0.16));
        p.setDeltaMovement(v.x, Math.max(0.4, v.y + 0.35), v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0, false, false));
        line(p.serverLevel(), eye.add(0, -0.3, 0), bh.getLocation(), ParticleTypes.CRIT);
        sound(p, SoundEvents.FISHING_BOBBER_THROW, 1f, 0.7f);
        return true;
    }

    /** Усмирить врагов вокруг. */
    private static boolean charm(ServerPlayer p, Ability a) {
        List<LivingEntity> foes = enemiesAround(p, p.getBoundingBox().inflate(a.radius));
        if (foes.isEmpty()) return noTarget(p);
        for (LivingEntity e : foes) {
            SkillEffects.charm(e, a.duration);
            applyFoe(a, e, p);
        }
        ring(p.serverLevel(), p.position().add(0, 0.5, 0), a.radius * 0.7, ParticleTypes.HEART, 20);
        sound(p, SoundEvents.ALLAY_AMBIENT_WITH_ITEM, 1f, 1.2f);
        return true;
    }

    /** Подчинить моба: на время он сражается за игрока. */
    private static boolean dominate(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t) || !(t instanceof Mob m)) return noTarget(p);
        if (m instanceof com.andrey.litrpg.entity.BossBase || m instanceof net.minecraft.world.entity.boss.wither.WitherBoss
                || m instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) {
            p.displayClientMessage(Component.literal("Воля хранителя слишком сильна").withStyle(ChatFormatting.RED), true);
            return false;
        }
        int lvl = com.andrey.litrpg.event.MobLevels.getLevel(m);
        if (lvl > s.getLevel() + 5) {
            p.displayClientMessage(Component.literal("Цель слишком сильна (ур. " + lvl + ")").withStyle(ChatFormatting.RED), true);
            return false;
        }
        Minions.dominate(m, p, a.duration);
        p.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 1, m.getZ(), 40, 0.4, 0.6, 0.4, 0.03);
        sound(p, SoundEvents.EVOKER_PREPARE_WOLOLO, 1f, 1f);
        return true;
    }

    /** Подсветить и пометить врагов вокруг. */
    private static boolean reveal(ServerPlayer p, Ability a) {
        long until = p.level().getGameTime() + a.duration;
        int n = 0;
        for (LivingEntity e : enemiesAround(p, p.getBoundingBox().inflate(a.radius))) {
            e.removeEffect(MobEffects.INVISIBILITY);
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, a.duration, 0));
            e.getPersistentData().putLong(MARK_TAG, until);
            applyFoe(a, e, p);
            n++;
        }
        ring(p.serverLevel(), p.position().add(0, 1, 0), 3, ParticleTypes.END_ROD, 30);
        p.displayClientMessage(Component.literal("Обнаружено врагов: " + n).withStyle(ChatFormatting.AQUA), true);
        sound(p, SoundEvents.EXPERIENCE_ORB_PICKUP, 1f, 0.5f);
        return true;
    }

    /** Украсть усиления цели. */
    private static boolean steal(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = lookEnemy(p, a.radius);
        if (!isEnemy(p, t)) return noTarget(p);
        List<MobEffectInstance> good = new ArrayList<>();
        for (MobEffectInstance inst : t.getActiveEffects()) {
            if (inst.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) good.add(inst);
        }
        for (MobEffectInstance inst : good) {
            t.removeEffect(inst.getEffect());
            p.addEffect(new MobEffectInstance(inst.getEffect(), Math.min(600, inst.getDuration()), inst.getAmplifier()));
        }
        float dmg = base(s, a) * a.power;
        if (dmg > 0) hurt(p, a, t, dmg);
        else applyFoe(a, t, p);
        applySelf(a, p);
        line(p.serverLevel(), t.position().add(0, 1, 0), p.position().add(0, 1, 0), ParticleTypes.WITCH);
        if (!good.isEmpty()) p.displayClientMessage(Component.literal("Украдено усилений: " + good.size()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        sound(p, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1f, 1.2f);
        return true;
    }

    /** Сбросить перезарядку остальных навыков. */
    private static boolean reset(ServerPlayer p, PlayerStats s, Ability a) {
        s.resetCooldowns();
        applySelf(a, p);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 60, 0.6, 1, 0.6, 1);
        sound(p, SoundEvents.BELL_RESONATE, 1f, 1.5f);
        return true;
    }

    /** Здоровье в обмен на ману. */
    private static boolean sacrifice(ServerPlayer p, PlayerStats s, Ability a) {
        float cost = p.getMaxHealth() * a.power;
        if (p.getHealth() <= cost + 1) {
            p.displayClientMessage(Component.literal("Слишком мало здоровья!").withStyle(ChatFormatting.DARK_RED), true);
            return false;
        }
        p.setHealth(p.getHealth() - cost);
        s.setMana(s.getMana() + s.getMaxMana() * a.power * 2);
        applySelf(a, p);
        p.serverLevel().sendParticles(new DustParticleOptions(new Vector3f(0.7f, 0f, 0.1f), 1.2f), p.getX(), p.getY() + 1, p.getZ(), 30, 0.4, 0.6, 0.4, 0);
        sound(p, SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH, 1f, 0.6f);
        return true;
    }

    /** Ловушка у ног. */
    private static boolean mine(ServerPlayer p, Ability a) {
        Vec3 at = p.position();
        SkillEffects.mine(p, a, at);
        p.serverLevel().sendParticles(particle(a.element), at.x, at.y + 0.1, at.z, 10, 0.3, 0, 0.3, 0.01);
        sound(p, SoundEvents.TRIPWIRE_ATTACH, 1f, 0.8f);
        return true;
    }

    /** Союзник под прицелом или самый раненый рядом. */
    private static LivingEntity allyTarget(ServerPlayer p, double r, boolean includeSelf) {
        LivingEntity look = lookTarget(p, r);
        if (look != null && look != p && isAlly(p, look) && !(look instanceof AbstractVillager)) return look;
        LivingEntity best = null;
        float bestFrac = 2f;
        for (LivingEntity e : alliesAround(p, r)) {
            if (e == p && !includeSelf || e instanceof AbstractVillager) continue;
            float frac = e.getHealth() / Math.max(1f, e.getMaxHealth());
            if (e == p) frac += 0.05f; // других — чуть охотнее
            if (frac < bestFrac) {
                bestFrac = frac;
                best = e;
            }
        }
        return best;
    }

    /** Лечащая связь с союзником. */
    private static boolean tether(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = allyTarget(p, a.radius, true);
        final LivingEntity target = t == null ? p : t;
        float amount = base(s, a) * a.power * 0.25f * healMult(s);
        ServerLevel level = p.serverLevel();
        applySelf(a, target);
        Scheduler.repeat(1, 20, Math.max(1, a.duration / 20), i -> {
            if (!p.isAlive() || !target.isAlive() || target.distanceToSqr(p) > a.radius * a.radius * 2.25) return false;
            float before = target.getHealth();
            target.heal(amount);
            com.andrey.litrpg.event.Threat.healed(p, target, target.getHealth() - before);
            if (target != p) line(level, p.position().add(0, 1.2, 0), target.position().add(0, 1.2, 0), ParticleTypes.HAPPY_VILLAGER);
            level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.6, target.getZ(), 1, 0.2, 0.1, 0.2, 0);
            return true;
        });
        sound(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.4f);
        return true;
    }

    /** Оберег от смерти на союзника. */
    private static boolean ward(ServerPlayer p, Ability a) {
        LivingEntity t = allyTarget(p, a.radius, true);
        if (t == null) t = p;
        SkillEffects.ward(t, a.duration);
        applySelf(a, t);
        p.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, t.getX(), t.getY() + 1.5, t.getZ(), 25, 0.3, 0.5, 0.3, 0.2);
        if (t != p) p.displayClientMessage(Component.literal("Оберег: ").append(t.getDisplayName()).withStyle(ChatFormatting.GOLD), true);
        sound(p, SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 1.3f);
        return true;
    }

    /** Щит раненому союзнику. */
    private static boolean shield(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = allyTarget(p, a.radius, true);
        if (t == null) t = p;
        float amount = base(s, a) * a.power * healMult(s);
        t.setAbsorptionAmount(Math.max(t.getAbsorptionAmount(), amount));
        applySelf(a, t);
        ring(p.serverLevel(), t.position().add(0, 1, 0), 0.9, ParticleTypes.END_ROD, 16);
        sound(p, SoundEvents.ARMOR_EQUIP_DIAMOND, 1f, 1.4f);
        return true;
    }

    /** Сильное лечение раненого союзника. */
    private static boolean healTarget(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = allyTarget(p, a.radius, true);
        if (t == null || t.getHealth() >= t.getMaxHealth()) {
            p.displayClientMessage(Component.literal("Все здоровы").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        float before = t.getHealth();
        t.heal(base(s, a) * a.power * healMult(s));
        com.andrey.litrpg.event.Threat.healed(p, t, t.getHealth() - before);
        applySelf(a, t);
        if (t != p) line(p.serverLevel(), p.position().add(0, 1.2, 0), t.position().add(0, 1.2, 0), ParticleTypes.END_ROD);
        p.serverLevel().sendParticles(ParticleTypes.HEART, t.getX(), t.getY() + 1.2, t.getZ(), 8, 0.4, 0.4, 0.4, 0.1);
        sound(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.9f);
        return true;
    }

    /** Молнии по всем врагам вокруг (не больше 12). */
    private static boolean global(ServerPlayer p, PlayerStats s, Ability a) {
        List<LivingEntity> foes = enemiesAround(p, p.getBoundingBox().inflate(a.radius, 12, a.radius));
        if (foes.isEmpty()) return noTarget(p);
        foes.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        float dmg = base(s, a) * a.power;
        ServerLevel level = p.serverLevel();
        for (int i = 0; i < Math.min(12, foes.size()); i++) {
            LivingEntity e = foes.get(i);
            Scheduler.later(1 + i * 2, () -> {
                if (!e.isAlive() || !p.isAlive()) return;
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(e.getX(), e.getY(), e.getZ());
                    bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                e.invulnerableTime = 0;
                hurt(p, a, e, dmg);
            });
        }
        return true;
    }

    /** Телепорт к ближайшему союзнику-игроку. */
    private static boolean rally(ServerPlayer p, Ability a) {
        ServerPlayer best = null;
        double bd = a.radius * a.radius;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.isSpectator()) continue;
            double d = o.distanceToSqr(p);
            if (d < bd) {
                bd = d;
                best = o;
            }
        }
        if (best == null) {
            p.displayClientMessage(Component.literal("Рядом нет союзников").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        p.serverLevel().sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.3, 0.8, 0.3, 0.3);
        p.teleportTo(best.getX(), best.getY(), best.getZ());
        p.fallDistance = 0;
        applySelf(a, p);
        sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1f);
        return true;
    }

    /** Собрать товарищей по группе к себе. */
    private static boolean gather(ServerPlayer p, Ability a) {
        int n = 0;
        for (ServerPlayer o : p.serverLevel().players()) {
            if (o == p || o.distanceToSqr(p) > a.radius * a.radius || !com.andrey.litrpg.party.Party.sameParty(p, o)) continue;
            o.teleportTo(p.getX(), p.getY(), p.getZ());
            o.fallDistance = 0;
            applySelf(a, o);
            n++;
        }
        if (n == 0) {
            p.displayClientMessage(Component.literal("Некого собрать — нужна группа (/party)").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        ring(p.serverLevel(), p.position().add(0, 0.2, 0), 2.5, ParticleTypes.PORTAL, 40);
        sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 0.8f);
        return true;
    }

    // ---------- заклинатель сфер ----------

    private static boolean orb(ServerPlayer p, PlayerStats s, Ability a) {
        var c = s.getPlayerClass();
        if (c == null || !c.isInvoker()) return false;
        String[] sp = c.invoke.spheres();
        for (int i = 0; i < sp.length; i++) {
            if (sp[i].equals(a.key)) {
                s.addOrb(i);
                break;
            }
        }
        p.serverLevel().sendParticles(particle(a.element), p.getX(), p.getY() + 2.2, p.getZ(), 8, 0.3, 0.1, 0.3, 0.01);
        sound(p, SoundEvents.AMETHYST_CLUSTER_PLACE, 0.6f, 1.2f + s.getOrbs().size() * 0.2f);
        return true;
    }

    private static boolean invoke(ServerPlayer p, PlayerStats s) {
        var c = s.getPlayerClass();
        if (c == null || !c.isInvoker()) return false;
        if (s.getOrbs().size() < 3) {
            p.displayClientMessage(Component.literal("Нужно три сферы").withStyle(ChatFormatting.YELLOW), true);
            return false;
        }
        int[] o = s.getOrbs().stream().mapToInt(Integer::intValue).sorted().toArray();
        var combo = c.invoke.find("" + o[0] + o[1] + o[2]);
        if (combo == null) return false;
        if (s.getLevel() < combo.level()) {
            p.displayClientMessage(Component.literal("Это сочетание откроется на уровне " + combo.level()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        s.pushInvoked(combo.skill());
        Ability spell = Ability.byKey(combo.skill());
        p.displayClientMessage(Component.literal("Призвано: ").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(spell.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE)), true);
        p.serverLevel().sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.5, p.getZ(), 40, 0.5, 0.6, 0.5, 0.6);
        sound(p, SoundEvents.EVOKER_CAST_SPELL, 0.8f, 1.6f);
        return true;
    }

    // ================= вспомогательное =================

    private static boolean noTarget(ServerPlayer p) {
        p.displayClientMessage(Component.literal("Нет цели").withStyle(ChatFormatting.YELLOW), true);
        return false;
    }

    public static boolean isBehind(Entity attacker, LivingEntity target) {
        Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
        Vec3 facing = Vec3.directionFromRotation(0, target.getYRot());
        return toAttacker.dot(facing) < -0.3;
    }

    public static boolean isAlly(ServerPlayer p, Entity e) {
        if (e == p || e instanceof Player) return true;
        if (Minions.ownerOf(e) != null) return true;
        if (e instanceof TamableAnimal t && t.isOwnedBy(p)) return true;
        if (e instanceof AbstractVillager) return true;
        return e instanceof IronGolem g && g.isPlayerCreated();
    }

    public static boolean isEnemy(ServerPlayer p, Entity e) {
        return e instanceof LivingEntity le && le.isAlive() && !isAlly(p, e) && !e.isSpectator()
                && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand);
    }

    static List<LivingEntity> enemiesAround(ServerPlayer p, AABB box) {
        return p.level().getEntitiesOfClass(LivingEntity.class, box, e -> isEnemy(p, e));
    }

    static List<LivingEntity> alliesAround(ServerPlayer p, double r) {
        return p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), e -> e.isAlive() && isAlly(p, e));
    }

    static Vec3 flatLook(ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        return dir.normalize();
    }

    /** Сущность под прицелом, не скрытая блоками. */
    public static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && e != p && !e.isSpectator(), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    /** Враг под прицелом: союзники на линии взгляда не мешают. */
    public static LivingEntity lookEnemy(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0f);
        Vec3 end = eye.add(look.scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, end, box, e -> isEnemy(p, e), range * range);
        return hit != null && hit.getEntity() instanceof LivingEntity le ? le : null;
    }

    /** Точка на земле, куда смотрит игрок. */
    static Vec3 lookPoint(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1.0f).scale(range));
        BlockHitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 v = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        LivingEntity t = lookEnemy(p, range);
        if (t != null && t.distanceToSqr(eye) < v.distanceToSqr(eye)) v = t.position();
        return v;
    }

    static void ring(ServerLevel level, Vec3 c, double r, ParticleOptions particle, int count) {
        for (int i = 0; i < count; i++) {
            double ang = Math.PI * 2 * i / count;
            level.sendParticles(particle, c.x + Math.cos(ang) * r, c.y, c.z + Math.sin(ang) * r, 1, 0, 0, 0, 0);
        }
    }

    static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle) {
        Vec3 d = to.subtract(from);
        int n = (int) Math.max(2, d.length() * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 pt = from.add(d.scale(i / (double) n));
            level.sendParticles(particle, pt.x, pt.y, pt.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    static void sound(ServerPlayer p, SoundEvent e, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), e, SoundSource.PLAYERS, vol, pitch);
    }
}
