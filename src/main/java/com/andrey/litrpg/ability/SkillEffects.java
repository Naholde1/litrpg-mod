package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Длительные последствия навыков: оглушение и остановка времени, усмирение, ловушки,
 * временные стены, «откат во времени», обереги от смерти и метка «в бою»
 * (в бою нельзя менять навыки на панели).
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class SkillEffects {
    private SkillEffects() {}

    public static final String STUN_TAG = "litrpg_stun_until";
    public static final String CHARM_TAG = "litrpg_charm_until";
    public static final String WARD_TAG = "litrpg_ward_until";
    public static final String COMBAT_TAG = "litrpg_combat";
    /** Сколько тиков после удара игрок считается «в бою». */
    public static final int COMBAT_TICKS = 120;

    // ================= бой =================

    public static boolean inCombat(Player p) {
        return p.level().getGameTime() - p.getPersistentData().getLong(COMBAT_TAG) < COMBAT_TICKS;
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        long now = victim.level().getGameTime();
        if (victim instanceof ServerPlayer p && e.getSource().getEntity() instanceof LivingEntity && !(e.getSource().getEntity() instanceof Player)) {
            p.getPersistentData().putLong(COMBAT_TAG, now);
        }
        if (e.getSource().getEntity() instanceof ServerPlayer p && !(victim instanceof Player)) {
            p.getPersistentData().putLong(COMBAT_TAG, now);
            // удар будит усмирённого
            victim.getPersistentData().remove(CHARM_TAG);
        }
    }

    // ================= оглушение и усмирение =================

    /** Оглушить моба: замирает на месте. Боссы — втрое короче. */
    public static final String PHASE_TAG = "litrpg_phase_until";

    /** Призрачный облик: никакой урон не проходит, пока длится. */
    public static void phase(LivingEntity e, int ticks) {
        e.getPersistentData().putLong(PHASE_TAG, e.level().getGameTime() + ticks);
        if (e.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
        }
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void onPhaseAttack(net.minecraftforge.event.entity.living.LivingAttackEvent e) {
        if (e.getEntity().getPersistentData().getLong(PHASE_TAG) > e.getEntity().level().getGameTime()) e.setCanceled(true);
    }

    public static void stun(LivingEntity e, int ticks) {
        if (!(e instanceof Mob m) || ticks <= 0) return;
        if (m instanceof BossBase) ticks = Math.max(10, ticks * 3 / 10);
        long now = m.level().getGameTime();
        var data = m.getPersistentData();
        if (!data.contains(STUN_TAG)) {
            if (m.isNoAi()) return; // неподвижные от природы (жители-статуи и т.п.) не трогаем
            m.setNoAi(true);
        }
        data.putLong(STUN_TAG, Math.max(data.getLong(STUN_TAG), now + ticks));
        m.setTarget(null);
    }

    public static boolean stunned(LivingEntity e) {
        return e.getPersistentData().contains(STUN_TAG);
    }

    public static void charm(LivingEntity e, int ticks) {
        if (!(e instanceof Mob m)) return;
        if (m instanceof BossBase) ticks = ticks / 3;
        m.getPersistentData().putLong(CHARM_TAG, m.level().getGameTime() + ticks);
        m.setTarget(null);
        m.setAggressive(false);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (le.level().isClientSide || !(le instanceof Mob m)) return;
        var data = m.getPersistentData();
        if (data.contains(STUN_TAG)) {
            long now = m.level().getGameTime();
            if (now >= data.getLong(STUN_TAG)) {
                data.remove(STUN_TAG);
                m.setNoAi(false);
            } else if (now % 8 == 0 && m.level() instanceof ServerLevel sl) {
                double y = m.getY() + m.getBbHeight() + 0.3;
                for (int i = 0; i < 3; i++) {
                    double ang = now * 0.4 + i * Math.PI * 2 / 3;
                    sl.sendParticles(ParticleTypes.CRIT, m.getX() + Math.cos(ang) * 0.5, y, m.getZ() + Math.sin(ang) * 0.5, 1, 0, 0, 0, 0);
                }
            }
        }
        if (data.contains(CHARM_TAG) && m.level().getGameTime() >= data.getLong(CHARM_TAG)) data.remove(CHARM_TAG);
        else if (data.contains(CHARM_TAG) && m.tickCount % 20 == 0 && m.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, m.getX(), m.getY() + m.getBbHeight() + 0.3, m.getZ(), 1, 0.2, 0.1, 0.2, 0);
        }
    }

    /** Усмирённые и оглушённые не выбирают целью игроков и их союзников. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTarget(LivingChangeTargetEvent e) {
        LivingEntity self = e.getEntity();
        LivingEntity t = e.getNewTarget();
        if (t == null || self.level().isClientSide) return;
        var data = self.getPersistentData();
        if (data.contains(STUN_TAG)) {
            e.setCanceled(true);
            return;
        }
        if (data.contains(CHARM_TAG) && (t instanceof Player || Minions.ownerOf(t) != null)) e.setCanceled(true);
    }

    // ================= оберег =================

    public static void ward(LivingEntity e, int ticks) {
        e.getPersistentData().putLong(WARD_TAG, e.level().getGameTime() + ticks);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (le.level().isClientSide || !le.getPersistentData().contains(WARD_TAG)) return;
        long until = le.getPersistentData().getLong(WARD_TAG);
        le.getPersistentData().remove(WARD_TAG);
        if (le.level().getGameTime() > until) return;
        e.setCanceled(true);
        le.setHealth(Math.max(1f, le.getMaxHealth() * 0.3f));
        le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3));
        if (le.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, le.getX(), le.getY() + 1, le.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
            sl.playSound(null, le.getX(), le.getY(), le.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
        if (le instanceof ServerPlayer p) {
            StatsManager.message(p, Component.literal("Оберег спас вас от смерти!").withStyle(ChatFormatting.GOLD));
        }
    }

    // ================= ловушки =================

    private record Mine(ResourceKey<Level> dim, Vec3 pos, UUID owner, Ability ability, long expire) {}

    private static final List<Mine> MINES = new ArrayList<>();

    public static void mine(ServerPlayer p, Ability a, Vec3 pos) {
        // у одного игрока не больше пяти ловушек
        long own = MINES.stream().filter(m -> m.owner.equals(p.getUUID())).count();
        if (own >= 5) {
            for (Iterator<Mine> it = MINES.iterator(); it.hasNext(); ) {
                if (it.next().owner.equals(p.getUUID())) {
                    it.remove();
                    break;
                }
            }
        }
        MINES.add(new Mine(p.level().dimension(), pos, p.getUUID(), a, p.level().getGameTime() + a.duration));
    }

    // ================= стены =================

    private record WallBlock(ResourceKey<Level> dim, BlockPos pos, BlockState state, long expire) {}

    private static final Map<BlockPos, WallBlock> WALLS = new HashMap<>();

    public static boolean placeWall(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState cur = level.getBlockState(pos);
        if (!cur.isAir() && !cur.canBeReplaced()) return false;
        if (!cur.getFluidState().isEmpty()) return false;
        level.setBlock(pos, state, 3);
        WALLS.put(pos.immutable(), new WallBlock(level.dimension(), pos.immutable(), state, level.getGameTime() + ticks));
        return true;
    }

    private static void removeWall(ServerLevel level, WallBlock w) {
        if (level.isLoaded(w.pos) && level.getBlockState(w.pos).is(w.state.getBlock())) {
            level.setBlock(w.pos, Blocks.AIR.defaultBlockState(), 3);
            level.sendParticles(ParticleTypes.POOF, w.pos.getX() + 0.5, w.pos.getY() + 0.5, w.pos.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (WALLS.containsKey(e.getPos()) && !e.getPlayer().isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate e) {
        e.getAffectedBlocks().removeIf(WALLS::containsKey);
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent e) {
        for (WallBlock w : WALLS.values()) {
            ServerLevel level = e.getServer().getLevel(w.dim);
            if (level != null) removeWall(level, w);
        }
        WALLS.clear();
        MINES.clear();
        HISTORY.clear();
    }

    // ================= откат во времени =================

    private static final Map<UUID, ArrayDeque<Vec3>> HISTORY = new HashMap<>();

    /** Где игрок был ~3 секунды назад. */
    public static Vec3 past(ServerPlayer p) {
        ArrayDeque<Vec3> h = HISTORY.get(p.getUUID());
        return h == null || h.isEmpty() ? null : h.peekFirst();
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 5 != 0) return;
        ArrayDeque<Vec3> h = HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        if (!h.isEmpty() && h.peekLast().distanceToSqr(p.position()) > 64 * 64) h.clear(); // телепорт — история заново
        h.addLast(p.position());
        while (h.size() > 12) h.removeFirst();
        orbBuffs(p);
    }

    /** Сферы заклинателя дают усиления, пока висят над ним. */
    private static void orbBuffs(ServerPlayer p) {
        if (p.tickCount % 40 != 0) return;
        PlayerStats s = StatsManager.get(p);
        var c = s.getPlayerClass();
        if (c == null || !c.isInvoker() || s.getOrbs().isEmpty()) return;
        int[] n = new int[3];
        for (int o : s.getOrbs()) n[o]++;
        for (int i = 0; i < 3; i++) {
            if (n[i] == 0) continue;
            Ability sphere = Ability.byKey(c.invoke.spheres()[i]);
            if (sphere == null) continue;
            for (Ability.Fx fx : sphere.self) {
                p.addEffect(new MobEffectInstance(fx.effect(), 60, fx.amplifier() + (n[i] >= 3 ? 1 : 0), true, false, true));
            }
        }
    }

    // ================= обработка мира =================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 5 != 0) return;
        if (!WALLS.isEmpty()) {
            for (Iterator<WallBlock> it = WALLS.values().iterator(); it.hasNext(); ) {
                WallBlock w = it.next();
                ServerLevel level = server.getLevel(w.dim);
                if (level == null) {
                    it.remove();
                } else if (level.getGameTime() >= w.expire) {
                    removeWall(level, w);
                    it.remove();
                }
            }
        }
        if (!MINES.isEmpty()) {
            for (Iterator<Mine> it = MINES.iterator(); it.hasNext(); ) {
                Mine m = it.next();
                ServerLevel level = server.getLevel(m.dim);
                ServerPlayer owner = server.getPlayerList().getPlayer(m.owner);
                if (level == null || owner == null || level.getGameTime() >= m.expire || owner.level() != level) {
                    it.remove();
                    continue;
                }
                if (server.getTickCount() % 20 == 0) {
                    level.sendParticles(ParticleTypes.SMOKE, m.pos.x, m.pos.y + 0.1, m.pos.z, 1, 0.15, 0, 0.15, 0);
                }
                List<LivingEntity> foes = level.getEntitiesOfClass(LivingEntity.class, new AABB(m.pos, m.pos).inflate(1.6, 1.5, 1.6),
                        x -> AbilityHandler.isEnemy(owner, x));
                if (foes.isEmpty()) continue;
                it.remove();
                AbilityHandler.nova(owner, StatsManager.get(owner), m.ability, m.pos, m.ability.radius, 1f);
                level.sendParticles(ParticleTypes.EXPLOSION, m.pos.x, m.pos.y + 0.3, m.pos.z, 3, 0.6, 0.2, 0.6, 0);
                level.playSound(null, m.pos.x, m.pos.y, m.pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.3f);
            }
        }
    }
}
