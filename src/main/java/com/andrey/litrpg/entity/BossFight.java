package com.andrey.litrpg.entity;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.Role;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Бой с хранителем: «вторые жизни» хранителей и воскрешение павших в бою (как боевой рес в MMO).
 * Игрок, павший в бою с хранителем, не умирает сразу — он становится духом у своего тела на 60 с.
 * Поддержка (роль) может поднять его: встать у тела и присесть на 3 секунды (перезарядка 90 с).
 * Если никто не воскресил, бой кончился или группа полегла — дух уходит, и это обычная смерть.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class BossFight {
    private BossFight() {}

    public static final String FALLEN_TAG = "litrpg_fallen_spirit";
    private static final int SPIRIT_TICKS = 20 * 60, REZ_TICKS = 60, REZ_COOLDOWN = 20 * 90;

    private record Spirit(ResourceKey<Level> dim, Vec3 body, long until, GameType mode) {}

    private static final Map<UUID, Spirit> SPIRITS = new HashMap<>();
    private static final Map<UUID, Integer> CHANNEL = new HashMap<>();
    private static final Map<UUID, Long> REZ_CD = new HashMap<>();
    private static final java.util.Set<UUID> RELEASED = new java.util.HashSet<>();

    public static boolean isSpirit(ServerPlayer p) {
        return SPIRITS.containsKey(p.getUUID());
    }

    /** Хранитель в бою рядом с точкой. */
    private static BossBase fightAt(ServerLevel sl, Vec3 at) {
        for (BossBase b : sl.getEntitiesOfClass(BossBase.class, new AABB(at, at).inflate(44, 20, 44))) {
            if (b.isAlive() && b.inCombat()) return b;
        }
        return null;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (e.getEntity() instanceof BossBase b) {
            if (b.tryLethal(e.getSource())) e.setCanceled(true);
            return;
        }
        if (!(e.getEntity() instanceof ServerPlayer p) || RELEASED.remove(p.getUUID())) return;
        if (p.isCreative() || p.isSpectator()) return;
        if (fightAt(p.serverLevel(), p.position()) == null) {
            LitRPG.LOGGER.info("[bossfight] {} died outside boss fight", p.getName().getString());
            return;
        }
        e.setCanceled(true);
        p.setHealth(1f);
        p.clearFire();
        p.removeAllEffects();
        Spirit s = new Spirit(p.level().dimension(), p.position(), p.level().getGameTime() + SPIRIT_TICKS, p.gameMode.getGameModeForPlayer());
        SPIRITS.put(p.getUUID(), s);
        p.getPersistentData().putBoolean(FALLEN_TAG, true);
        p.setGameMode(GameType.SPECTATOR);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 60, 15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Вы пали").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Дух ждёт воскрешения 60 с").withStyle(ChatFormatting.GRAY)));
        for (ServerPlayer o : p.serverLevel().getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(48))) {
            if (o == p) continue;
            boolean healer = StatsManager.get(o).getRole() == Role.SUPPORT;
            o.sendSystemMessage(Component.literal("✝ " + p.getName().getString() + " пал в бою. ")
                    .withStyle(ChatFormatting.RED)
                    .append(Component.literal(healer ? "Встаньте у тела и присядьте на 3 с — воскресите!" : "Поддержка может воскресить его (60 с).")
                            .withStyle(healer ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.7f);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Spirit s = SPIRITS.get(p.getUUID());
        if (s != null) {
            spiritTick(p, s);
            return;
        }
        // поддержка воскрешает: присела у тела павшего
        if (!p.isCrouching() || !p.isAlive() || p.isSpectator() || StatsManager.get(p).getRole() != Role.SUPPORT) {
            CHANNEL.remove(p.getUUID());
            return;
        }
        for (Map.Entry<UUID, Spirit> en : SPIRITS.entrySet()) {
            Spirit sp = en.getValue();
            if (sp.dim != p.level().dimension() || sp.body.distanceTo(p.position()) > 2.8) continue;
            long now = p.level().getGameTime();
            long cd = REZ_CD.getOrDefault(p.getUUID(), 0L);
            if (cd > now) {
                p.displayClientMessage(Component.literal("Воскрешение перезаряжается: " + (cd - now) / 20 + " с").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            int t = CHANNEL.merge(p.getUUID(), 1, Integer::sum);
            p.serverLevel().sendParticles(ParticleTypes.END_ROD, sp.body.x, sp.body.y + 0.5, sp.body.z, 3, 0.4, 0.4, 0.4, 0.02);
            p.displayClientMessage(Component.literal("Воскрешение: " + "█".repeat(t * 10 / REZ_TICKS) + "░".repeat(10 - t * 10 / REZ_TICKS))
                    .withStyle(ChatFormatting.GREEN), true);
            if (t >= REZ_TICKS) {
                CHANNEL.remove(p.getUUID());
                REZ_CD.put(p.getUUID(), now + REZ_COOLDOWN);
                ServerPlayer dead = p.server.getPlayerList().getPlayer(en.getKey());
                if (dead != null) revive(dead, sp, p);
            }
            return;
        }
        CHANNEL.remove(p.getUUID());
    }

    private static void spiritTick(ServerPlayer p, Spirit s) {
        ServerLevel sl = p.server.getLevel(s.dim);
        if (sl == null) {
            release(p, s);
            return;
        }
        if (p.level() != sl || p.position().distanceTo(s.body) > 5) {
            p.teleportTo(sl, s.body.x, s.body.y + 1.5, s.body.z, p.getYRot(), p.getXRot());
        }
        long now = sl.getGameTime();
        if (now % 10 == 0) sl.sendParticles(ParticleTypes.SOUL, s.body.x, s.body.y + 0.3, s.body.z, 3, 0.3, 0.1, 0.3, 0.01);
        if (now % 20 == 0) {
            long left = (s.until - now) / 20;
            p.displayClientMessage(Component.literal("✝ Дух: ждём воскрешения… " + Math.max(0, left) + " с").withStyle(ChatFormatting.GRAY), true);
            boolean alive = !sl.getEntitiesOfClass(ServerPlayer.class, new AABB(s.body, s.body).inflate(48, 24, 48),
                    o -> o.isAlive() && !o.isSpectator() && !o.isCreative()).isEmpty();
            if (now >= s.until || fightAt(sl, s.body) == null || !alive) release(p, s);
        }
    }

    private static void revive(ServerPlayer p, Spirit s, ServerPlayer by) {
        SPIRITS.remove(p.getUUID());
        p.getPersistentData().remove(FALLEN_TAG);
        ServerLevel sl = p.server.getLevel(s.dim);
        if (sl != null) p.teleportTo(sl, s.body.x, s.body.y, s.body.z, p.getYRot(), p.getXRot());
        p.setGameMode(s.mode == GameType.SPECTATOR ? GameType.SURVIVAL : s.mode);
        p.setHealth(p.getMaxHealth() * 0.4f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        if (sl != null) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, s.body.x, s.body.y + 1, s.body.z, 60, 0.4, 0.8, 0.4, 0.3);
            sl.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.2f);
        }
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Воскрешён!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Вас поднял(а) " + by.getName().getString()).withStyle(ChatFormatting.GREEN)));
        by.displayClientMessage(Component.literal("Вы воскресили " + p.getName().getString()).withStyle(ChatFormatting.GOLD), true);
    }

    /** Дух уходит — обычная смерть у тела. */
    private static void release(ServerPlayer p, Spirit s) {
        SPIRITS.remove(p.getUUID());
        p.getPersistentData().remove(FALLEN_TAG);
        ServerLevel sl = p.server.getLevel(s.dim);
        if (sl != null) p.teleportTo(sl, s.body.x, s.body.y, s.body.z, p.getYRot(), p.getXRot());
        p.setGameMode(s.mode == GameType.SPECTATOR ? GameType.SURVIVAL : s.mode);
        RELEASED.add(p.getUUID());
        p.hurt(p.damageSources().fellOutOfWorld(), Float.MAX_VALUE);
        RELEASED.remove(p.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            Spirit s = SPIRITS.get(p.getUUID());
            if (s != null) release(p, s);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && p.getPersistentData().getBoolean(FALLEN_TAG) && !SPIRITS.containsKey(p.getUUID())) {
            // сервер перезапускался, пока игрок был духом — возвращаем в обычный режим
            p.getPersistentData().remove(FALLEN_TAG);
            p.setGameMode(GameType.SURVIVAL);
        }
    }
}
