package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.AbilityHandler;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Боевая система: разница уровней, криты, уклонение, свойства элитных мобов.
 * Монстр на 10 уровней выше получает от игрока ~40% урона и бьёт почти вдвое сильнее.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class CombatEvents {

    /** Множитель урона игрока по цели в зависимости от разницы уровней. */
    public static float outgoingMult(int playerLevel, int targetLevel) {
        int diff = targetLevel - playerLevel;
        if (diff > 0) return Math.max(0.15f, 1f - 0.06f * diff);
        return Math.min(1.3f, 1f - 0.015f * diff);
    }

    /** Множитель урона монстра по игроку. */
    public static float incomingMult(int playerLevel, int mobLevel) {
        int diff = mobLevel - playerLevel;
        if (diff > 0) return Math.min(3.0f, 1f + 0.08f * diff);
        return Math.max(0.5f, 1f + 0.03f * diff);
    }

    /** Насколько класс игрока относится к стилю (0–4). */
    public static int classWeight(PlayerStats s, Style st) {
        return s.getPlayerClass() == null ? 0 : s.getPlayerClass().styles.getOrDefault(st, 0);
    }

    public static float critChance(PlayerStats s) {
        return (float) (com.andrey.litrpg.stats.StatBreakdown.critChance(s).value() / 100.0);
    }

    public static float critMultiplier(PlayerStats s) {
        return (float) com.andrey.litrpg.stats.StatBreakdown.critMultiplier(s).value();
    }

    public static float dodgeChance(PlayerStats s) {
        return (float) (com.andrey.litrpg.stats.StatBreakdown.dodge(s).value() / 100.0);
    }

    /** Насколько слабее урон по своим: случайная стрела в спину товарища не должна его валить. */
    private static final float FRIENDLY_FIRE = 0.15f;

    /** Снаряд игрока или его слуги (скелет некроманта, волк и прочие призванные). */
    private static boolean fromParty(Entity owner) {
        if (owner instanceof Player) return true;
        return owner != null && com.andrey.litrpg.ability.Minions.ownerOf(owner) != null;
    }

    /**
     * Стрелы и снаряды игроков не отскакивают от «неуязвимости после удара»: в группе по цели бьют часто,
     * и ванильные полсекунды неуязвимости отбрасывали почти каждую стрелу.
     * Снимаем неуязвимость дважды: в момент попадания снаряда (до всей логики урона — именно там
     * решается, пролетит стрела или отскочит) и в самом событии атаки, на случай чужих источников урона.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) return;
        if (!(hit.getEntity() instanceof LivingEntity target) || target instanceof Player) return;
        if (target.level().isClientSide) return;
        if (fromParty(event.getProjectile().getOwner())) target.invulnerableTime = 0;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onProjectileAttack(LivingAttackEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || target instanceof Player) return;
        DamageSource src = event.getSource();
        if (fromParty(src.getEntity()) && src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile) {
            target.invulnerableTime = 0;
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        DamageSource src = event.getSource();
        if (!(src.getEntity() instanceof LivingEntity attacker) || attacker instanceof Player) return;
        if (src.is(DamageTypeTags.BYPASSES_ARMOR) || src.is(DamageTypeTags.IS_EXPLOSION)) return;
        PlayerStats s = StatsManager.get(player);
        if (player.getRandom().nextFloat() < dodgeChance(s)) {
            event.setCanceled(true);
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8f, 1.6f);
            player.displayClientMessage(Component.literal("Уклонение!").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** Сильные твари с запасом прочности сверх 1024 ед. здоровья получают урон, делённый на него. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onToughHurt(LivingHurtEvent event) {
        float tough = event.getEntity().getPersistentData().getFloat(MobLevels.TOUGH_TAG);
        if (tough > 1f) event.setAmount(event.getAmount() / tough);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource src = event.getSource();
        float amount = event.getAmount();

        // урон хранителей: роль, броня и щит (BossDamage)
        if (src.is(com.andrey.litrpg.entity.BossDamage.BOSS)) {
            amount = com.andrey.litrpg.entity.BossDamage.mitigate(target, src, amount);
            event.setAmount(amount);
        }

        // эффекты отряда: Заслон (−10%/ур.), Уязвимость (+10%/ур.)
        int bw = com.andrey.litrpg.registry.ModEffects.level(target, com.andrey.litrpg.registry.ModEffects.BULWARK);
        int vu = com.andrey.litrpg.registry.ModEffects.level(target, com.andrey.litrpg.registry.ModEffects.VULNERABILITY);
        if (bw > 0 || vu > 0) {
            amount *= (1f - Math.min(0.6f, 0.1f * bw)) * (1f + 0.1f * vu);
            event.setAmount(amount);
        }

        // ---------- игрок получает урон ----------
        if (target instanceof ServerPlayer victim) {
            PlayerStats vs = StatsManager.get(victim);
            // свои по своим: в тесных коридорах шальная стрела или взмах по площади выносили товарища
            if (src.getEntity() != victim && fromParty(src.getEntity())) {
                event.setAmount(amount * FRIENDLY_FIRE);
                return;
            }
            if (src.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Player)) {
                amount *= incomingMult(vs.getLevel(), MobLevels.getLevel(attacker));
                if (victim.getPersistentData().getLong(AbilityHandler.REFLECT_TAG) > victim.level().getGameTime()) {
                    Scheduled.reflect(victim, attacker, amount * 0.5f);
                    amount *= 0.6f;
                }
                if (MobLevels.hasAffix(attacker, MobLevels.Affix.VAMPIRIC)) attacker.heal(amount * 0.5f);
                if (MobLevels.hasAffix(attacker, MobLevels.Affix.BURNING)) victim.setSecondsOnFire(4);
                if (MobLevels.hasAffix(attacker, MobLevels.Affix.VENOMOUS)) {
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                }
            }
            // пассивная стойкость: роль танка и выносливость (видно в окне «K»)
            amount *= 1f - (float) com.andrey.litrpg.stats.StatBreakdown.resistance(vs).value() / 100f;
            event.setAmount(amount);
            return;
        }

        // ---------- игрок наносит урон ----------
        if (!(src.getEntity() instanceof ServerPlayer player) || target instanceof Player) return;
        PlayerStats s = StatsManager.get(player);
        amount *= outgoingMult(s.getLevel(), MobLevels.getLevel(target));
        if (s.getRole() != null) amount *= s.getRole().damageMult();
        amount *= 1f + 0.1f * com.andrey.litrpg.registry.ModEffects.level(player, com.andrey.litrpg.registry.ModEffects.MIGHT);

        boolean melee = src.getDirectEntity() == player;
        boolean arrow = src.getDirectEntity() instanceof AbstractArrow;

        // характеристики идут в урон: Сила — ближний бой, Ловкость — стрельба.
        // Те же проценты показывает окно персонажа, поэтому цифры в окне и в бою не расходятся.
        if (melee) amount *= 1f + (float) com.andrey.litrpg.stats.StatBreakdown.meleeDamage(s).value() / 100f;
        if (arrow) amount *= 1f + (float) com.andrey.litrpg.stats.StatBreakdown.rangedDamage(s).value() / 100f;

        long markedUntil = target.getPersistentData().getLong(AbilityHandler.MARK_TAG);
        if (markedUntil > player.level().getGameTime()) amount *= 1.3f;

        if ((melee || arrow) && player.getRandom().nextFloat() < critChance(s)) {
            amount *= critMultiplier(s);
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(),
                        14, 0.3, 0.3, 0.3, 0.3);
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + target.getBbHeight() * 0.7, target.getZ(),
                        8, 0.3, 0.3, 0.3, 0.2);
            }
            player.displayClientMessage(Component.literal("Критический удар! " + String.format("%.1f", amount))
                    .withStyle(ChatFormatting.GOLD), true);
        }

        if (melee) {
            amount = applyOnHit(player, s, target, amount);
            if (MobLevels.hasAffix(target, MobLevels.Affix.THORNS)) {
                Scheduled.thorns(player, target, amount * 0.15f);
            }
        }
        event.setAmount(amount);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel sl)) return;
        if (MobLevels.hasAffix(dead, MobLevels.Affix.EXPLOSIVE)) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, dead.getX(), dead.getY() + 0.5, dead.getZ(), 1, 0, 0, 0, 0);
            sl.playSound(null, dead.getX(), dead.getY(), dead.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.5f, 0.9f);
            float dmg = 4 + MobLevels.getLevel(dead) * 0.5f;
            for (Player p : sl.getEntitiesOfClass(Player.class, dead.getBoundingBox().inflate(4))) {
                p.hurt(sl.damageSources().explosion(dead, dead), dmg);
            }
        }
    }

    /** Монстры подземелья не роняют ванильную добычу (личная добыча — run/RunLoot). */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (com.andrey.litrpg.run.Run.inDungeon(event.getEntity())) event.getDrops().clear();
    }

    @SubscribeEvent
    public static void onXpDrop(net.minecraftforge.event.entity.living.LivingExperienceDropEvent event) {
        if (com.andrey.litrpg.run.Run.inDungeon(event.getEntity())) event.setCanceled(true);
    }

    /** Заряды оружия от навыков (яд, огонь, вампиризм…). */
    private static float applyOnHit(ServerPlayer player, PlayerStats s, LivingEntity target, float amount) {
        var data = player.getPersistentData();
        int charges = data.getInt(AbilityHandler.ONHIT_COUNT_TAG);
        if (charges <= 0) return amount;
        Ability.OnHit[] kinds = Ability.OnHit.values();
        int k = data.getInt(AbilityHandler.ONHIT_TAG);
        if (k < 0 || k >= kinds.length) return amount;
        switch (kinds[k]) {
            case POISON -> target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1 + s.getLevel() / 20), player);
            case FIRE -> target.setSecondsOnFire(4);
            case FROST -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), player);
            case BLEED -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), player);
            case LIFESTEAL -> player.heal(amount * 0.25f);
            case STUN -> com.andrey.litrpg.ability.SkillEffects.stun(target, 10);
            case MANA -> {
                s.setMana(s.getMana() + 3 + s.getMaxMana() * 0.02);
                StatsManager.syncMana(player);
            }
            case CHAIN -> {
                for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4),
                        x -> x != target && com.andrey.litrpg.ability.AbilityHandler.isEnemy(player, x))) {
                    e.hurt(player.damageSources().indirectMagic(player, player), amount * 0.4f);
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.4, 0.3, 0.1);
                    }
                }
            }
            case ARCANE -> {
                amount += 2 + s.getStat(Stat.INTELLIGENCE) * 0.35f;
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.4, 0.3, 0.2);
                }
            }
        }
        data.putInt(AbilityHandler.ONHIT_COUNT_TAG, charges - 1);
        return amount;
    }

    /** Отражение урона шипами — откладываем на тик, чтобы не войти в рекурсию урона. */
    private static final class Scheduled {
        static void thorns(ServerPlayer player, Entity source, float dmg) {
            com.andrey.litrpg.util.Scheduler.later(1, () -> {
                if (player.isAlive()) player.hurt(player.damageSources().thorns(source), Mth.clamp(dmg, 0.5f, 20f));
            });
        }

        static void reflect(ServerPlayer player, LivingEntity attacker, float dmg) {
            com.andrey.litrpg.util.Scheduler.later(1, () -> {
                if (attacker.isAlive()) {
                    attacker.hurt(player.damageSources().thorns(player), dmg);
                    if (player.level() instanceof ServerLevel sl) {
                        sl.sendParticles(ParticleTypes.CRIT, attacker.getX(), attacker.getY() + 1, attacker.getZ(), 12, 0.3, 0.4, 0.3, 0.2);
                    }
                }
            });
        }
    }
}
