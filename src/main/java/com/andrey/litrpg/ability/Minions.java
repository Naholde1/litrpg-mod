package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Призванные существа. Сражаются только с монстрами, не трогают игроков,
 * исчезают по таймеру, а опыт за их убийства получает хозяин.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Minions {
    public static final String OWNER_TAG = "litrpg_owner";
    public static final String EXPIRE_TAG = "litrpg_expire";
    /** Турель: не ходит, сама выбирает цель и бьёт очередями. Ключ навыка — чем именно бьёт. */
    public static final String TURRET_TAG = "litrpg_turret";
    /** Как часто турель стреляет и как далеко достаёт. */
    private static final int TURRET_PERIOD = 20;
    private static final double TURRET_RANGE = 22;
    private static final UUID HP_ID = UUID.fromString("c2b8f1a4-3d1e-4b9a-9f0e-7a6d5c4b3a21");
    private static final UUID DMG_ID = UUID.fromString("c2b8f1a4-3d1e-4b9a-9f0e-7a6d5c4b3a22");

    private Minions() {}

    public static UUID ownerOf(Entity e) {
        return e.getPersistentData().hasUUID(OWNER_TAG) ? e.getPersistentData().getUUID(OWNER_TAG) : null;
    }

    public static ServerPlayer owner(Entity e) {
        UUID id = ownerOf(e);
        if (id == null || !(e.level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(id);
    }

    static boolean summon(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel level = p.serverLevel();
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(a.summon));
        if (type == null) return false;
        int lvl = s.getLevel();
        for (int i = 0; i < a.count; i++) {
            Entity raw = type.create(level);
            if (!(raw instanceof Mob mob)) continue;
            double ang = Math.PI * 2 * i / a.count;
            mob.moveTo(p.getX() + Math.cos(ang) * 1.8, p.getY(), p.getZ() + Math.sin(ang) * 1.8, p.getYRot(), 0);
            mob.getPersistentData().putUUID(OWNER_TAG, p.getUUID());
            mob.getPersistentData().putLong(EXPIRE_TAG, level.getGameTime() + a.duration);
            // метка уровня, чтобы Система не назначила миньону уровень монстра
            mob.getPersistentData().putInt("litrpg_lvl", lvl);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            prepare(mob, p, lvl);
            if (a.turret) makeTurret(mob, a);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 0.5, mob.getZ(), 15, 0.4, 0.6, 0.4, 0.02);
        }
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1f, 1.2f);
        return true;
    }

    /**
     * Турель: стоит там, где поставили, и сама бьёт. Цели ищет штатный поиск прислужника,
     * а стреляет она отдельным тиком — обычные мобы бьют только вплотную.
     */
    private static void makeTurret(Mob mob, Ability a) {
        mob.getPersistentData().putString(TURRET_TAG, a.key);
        mob.setCustomName(Component.literal("Турель").withStyle(ChatFormatting.GOLD));
        mob.setCustomNameVisible(true);
        mob.setNoGravity(false);
        // ходить ей незачем: всё, что делает турель, делает её выстрел
        mob.goalSelector.removeAllGoals(g -> true);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0);
        AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) follow.setBaseValue(TURRET_RANGE);
    }

    /** Выстрел турели: раз в секунду по ближайшему врагу в пределах досягаемости. */
    private static void turretShot(Mob turret, ServerPlayer owner) {
        if (!(turret.level() instanceof ServerLevel sl)) return;
        Ability a = Ability.byKey(turret.getPersistentData().getString(TURRET_TAG));
        if (a == null) return;
        LivingEntity target = turret.getTarget();
        if (target == null || !target.isAlive() || turret.distanceTo(target) > TURRET_RANGE) {
            target = null;
            double best = TURRET_RANGE * TURRET_RANGE;
            for (Mob e : sl.getEntitiesOfClass(Mob.class, turret.getBoundingBox().inflate(TURRET_RANGE),
                    x -> x.isAlive() && x instanceof Enemy && ownerOf(x) == null)) {
                double d = e.distanceToSqr(turret);
                if (d < best && turret.hasLineOfSight(e)) {
                    best = d;
                    target = e;
                }
            }
            turret.setTarget(target);
        }
        if (target == null || !turret.hasLineOfSight(target)) return;
        float dmg = AbilityHandler.base(com.andrey.litrpg.stats.StatsManager.get(owner), a) * 0.6f;
        com.andrey.litrpg.fx.Fx.beam(sl, turret.getEyePosition(), target.position().add(0, target.getBbHeight() / 2, 0),
                0.18, 6, 0xC0FFD060);
        AbilityHandler.hurt(owner, a, target, dmg);
        sl.playSound(null, turret.getX(), turret.getY(), turret.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.7f, 1.4f);
    }

    /** Подчинить моба на время: он становится прислужником и уходит, когда воля ослабнет. */
    static void dominate(Mob mob, ServerPlayer owner, int ticks) {
        mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        mob.getPersistentData().putLong(EXPIRE_TAG, owner.level().getGameTime() + ticks);
        mob.setTarget(null);
        mob.setCustomName(Component.literal("Подчинённый " + owner.getGameProfile().getName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
        mob.targetSelector.removeAllGoals(g -> true);
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false,
                e -> e instanceof Enemy && ownerOf(e) == null));
    }

    private static void prepare(Mob mob, ServerPlayer owner, int lvl) {
        mob.setCustomName(Component.literal("Прислужник " + owner.getGameProfile().getName()).withStyle(ChatFormatting.AQUA));
        mob.setCustomNameVisible(false);
        mob.setPersistenceRequired();
        scale(mob, Attributes.MAX_HEALTH, HP_ID, 0.15 * lvl);
        scale(mob, Attributes.ATTACK_DAMAGE, DMG_ID, 0.10 * lvl);
        mob.setHealth(mob.getMaxHealth());
        int inf = MobEffectInstance.INFINITE_DURATION;
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0, false, false));

        if (mob instanceof Wolf wolf) {
            wolf.tame(owner);
            wolf.setOrderedToSit(false);
            return;
        }
        if (mob instanceof Vex vex) {
            vex.setOwner(null);
            vex.setLimitedLife(20 * 60);
        }
        if (mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET)); // не горят на солнце
            mob.setDropChance(EquipmentSlot.HEAD, 0f);
        }
        mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
        // монстры-прислужники: вместо охоты на игроков — охота на монстров
        mob.targetSelector.removeAllGoals(g -> true);
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, Mob.class, 10, true, false,
                e -> e instanceof Enemy && ownerOf(e) == null));
    }

    private static void scale(Mob mob, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, double mult) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, "litrpg_minion", mult, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** Прислужники не нападают на игроков и друг на друга. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        LivingEntity target = event.getNewTarget();
        if (target == null) return;
        if (ownerOf(self) != null && (target instanceof Player || ownerOf(target) != null)) {
            event.setCanceled(true);
        }
    }

    /** Тайм-аут и помощь хозяину: бьют того, кто бьёт хозяина или кого бьёт хозяин. */
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || e.tickCount % 20 != 0 || ownerOf(e) == null) return;
        long expire = e.getPersistentData().getLong(EXPIRE_TAG);
        ServerPlayer owner = owner(e);
        if (owner == null || e.level().getGameTime() > expire || owner.level() != e.level()) {
            ((ServerLevel) e.level()).sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
            e.discard();
            return;
        }
        // тотемы и станки вкопаны там, где их поставили: за хозяином они не ходят
        if (e instanceof com.andrey.litrpg.entity.TotemEntity) return;
        boolean turret = e.getPersistentData().contains(TURRET_TAG);
        if (turret) {
            // турель стоит там, где её поставили, и стреляет сама
            if (e instanceof Mob m) turretShot(m, owner);
            return;
        }
        if (e instanceof Mob m && e.distanceToSqr(owner) > 24 * 24) {
            m.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
        if (e instanceof Mob m && m.getTarget() == null) {
            LivingEntity foe = owner.getLastHurtByMob() != null ? owner.getLastHurtByMob() : owner.getLastHurtMob();
            if (foe != null && foe.isAlive() && foe != m && !(foe instanceof Player) && ownerOf(foe) == null) m.setTarget(foe);
        }
    }

    /** Хозяин не ранит своих прислужников случайными навыками. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        UUID owner = ownerOf(event.getEntity());
        if (owner != null && event.getSource().getEntity() instanceof Player p && p.getUUID().equals(owner)) {
            event.setCanceled(true);
        }
    }
}
