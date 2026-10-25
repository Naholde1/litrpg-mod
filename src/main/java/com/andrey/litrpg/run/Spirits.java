package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.SkillEffects;
import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Role;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
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
 * Гибель в подземелье. Игрок не умирает — он становится духом у своего тела (как в рейде):
 * <ul>
 *   <li>поддержка поднимает его — присесть у тела на 3 с (откат 90 с) или навыком воскрешения;</li>
 *   <li>пал в бою с хранителем — оживает сам, когда хранитель повержен;</li>
 *   <li>пал в бою с отрядом — оживает сам, когда бой стих (никто из живых не в бою 8 с);</li>
 *   <li>пали все — этап начинается заново ({@link Run#wipe}).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Spirits {
    private Spirits() {}

    public static final String FALLEN_TAG = "litrpg_fallen_spirit";
    private static final int REZ_TICKS = 60, REZ_COOLDOWN = 20 * 90;

    /** body — где лежит тело; boss — хранитель, в бою с которым пал (или null). */
    private record Spirit(Vec3 body, UUID boss, long since) {}

    private static final Map<UUID, Spirit> SPIRITS = new HashMap<>();
    private static final Map<UUID, Integer> CHANNEL = new HashMap<>();
    private static final Map<UUID, Long> REZ_CD = new HashMap<>();

    public static boolean isSpirit(ServerPlayer p) {
        return SPIRITS.containsKey(p.getUUID());
    }

    public static boolean isSpirit(UUID id) {
        return SPIRITS.containsKey(id);
    }

    /** Хранитель в бою рядом с точкой. */
    private static BossBase fightAt(ServerLevel sl, Vec3 at) {
        for (BossBase b : sl.getEntitiesOfClass(BossBase.class, new AABB(at, at).inflate(48, 24, 48))) {
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
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || p.isSpectator()) return;
        if (!Run.inDungeon(p)) return;
        e.setCanceled(true);
        fall(p);
    }

    /** Превратить игрока в духа у его тела. */
    public static void fall(ServerPlayer p) {
        p.setHealth(1f);
        p.clearFire();
        p.removeAllEffects();
        p.setTicksFrozen(0);
        BossBase boss = fightAt(p.serverLevel(), p.position());
        Vec3 body = Run.safeSpot(p);
        SPIRITS.put(p.getUUID(), new Spirit(body, boss == null ? null : boss.getUUID(), p.level().getGameTime()));
        p.getPersistentData().putBoolean(FALLEN_TAG, true);
        p.setGameMode(GameType.SPECTATOR);
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 60, 15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Вы пали").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(boss != null
                ? "Дух ждёт воскрешения — или победы над хранителем" : "Дух ждёт воскрешения — или конца боя").withStyle(ChatFormatting.GRAY)));
        for (ServerPlayer o : Run.party(p.server)) {
            if (o == p) continue;
            boolean healer = StatsManager.get(o).getRole() == Role.SUPPORT;
            o.sendSystemMessage(Component.literal("✝ " + p.getName().getString() + " пал. ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(healer ? "Встаньте у тела и присядьте на 3 с — воскресите!" : "Поддержка может его воскресить.")
                            .withStyle(healer ? ChatFormatting.GREEN : ChatFormatting.GRAY)));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5f, 0.7f);
        StatsManager.sync(p);
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
            if (sp.body.distanceTo(p.position()) > 2.8) continue;
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
                if (dead != null) revive(dead, 0.4f, "Вас поднял(а) " + p.getName().getString());
                p.displayClientMessage(Component.literal("Вы воскресили союзника").withStyle(ChatFormatting.GOLD), true);
            }
            return;
        }
        CHANNEL.remove(p.getUUID());
    }

    /** Навык воскрешения: поднять ближайшего павшего в радиусе r сразу. */
    public static boolean reviveNear(ServerPlayer by, double r) {
        UUID best = null;
        double bestD = r;
        for (Map.Entry<UUID, Spirit> en : SPIRITS.entrySet()) {
            double d = en.getValue().body.distanceTo(by.position());
            if (d <= bestD) {
                bestD = d;
                best = en.getKey();
            }
        }
        if (best == null) return false;
        ServerPlayer dead = by.server.getPlayerList().getPlayer(best);
        if (dead == null) return false;
        revive(dead, 0.5f, "Вас поднял(а) " + by.getName().getString());
        return true;
    }

    private static void spiritTick(ServerPlayer p, Spirit s) {
        ServerLevel sl = p.serverLevel();
        if (!Run.inDungeon(p)) {
            revive(p, 1f, null);
            return;
        }
        if (p.position().distanceTo(s.body) > 6) p.teleportTo(s.body.x, s.body.y + 1.5, s.body.z);
        long now = sl.getGameTime();
        if (now % 10 == 0) sl.sendParticles(ParticleTypes.SOUL, s.body.x, s.body.y + 0.3, s.body.z, 3, 0.3, 0.1, 0.3, 0.01);
        if (now % 20 != 0) return;
        if (s.boss != null) {
            // бой с хранителем: ждать его падения (или сброса этапа)
            if (!(sl.getEntity(s.boss) instanceof BossBase b) || !b.isAlive()) {
                revive(p, 0.6f, "Хранитель повержен — вы снова на ногах");
                return;
            }
            if (!b.inCombat()) {
                revive(p, 0.6f, "Бой окончен");
                return;
            }
            p.displayClientMessage(Component.literal("✝ Дух: ждём воскрешения или победы над хранителем…").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        // бой с отрядом: никто из живых не в бою 8 с — дух возвращается
        boolean fight = false;
        for (ServerPlayer o : Run.party(p.server)) {
            if (o != p && !isSpirit(o) && SkillEffects.inCombat(o)) fight = true;
        }
        long calm = p.getPersistentData().getLong("litrpg_spirit_calm");
        if (fight || now - s.since < 60) {
            p.getPersistentData().putLong("litrpg_spirit_calm", now);
            p.displayClientMessage(Component.literal("✝ Дух: ждём воскрешения или конца боя…").withStyle(ChatFormatting.GRAY), true);
        } else if (now - calm >= 160) {
            revive(p, 0.5f, "Бой стих — вы снова на ногах");
        } else {
            p.displayClientMessage(Component.literal("✝ Бой стихает… " + (160 - (now - calm)) / 20 + " с").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Вернуть духа к жизни у тела (hp — доля здоровья). */
    public static void revive(ServerPlayer p, float hp, String why) {
        Spirit s = SPIRITS.remove(p.getUUID());
        p.getPersistentData().remove(FALLEN_TAG);
        if (s != null && Run.inDungeon(p)) p.teleportTo(s.body.x, s.body.y, s.body.z);
        p.setGameMode(GameType.SURVIVAL);
        p.setHealth(Math.max(1f, p.getMaxHealth() * hp));
        PlayerStats st = StatsManager.get(p);
        st.setMana(Math.max(st.getMana(), st.getMaxMana() * 0.3));
        if (s == null) return;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
        ServerLevel sl = p.serverLevel();
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, s.body.x, s.body.y + 1, s.body.z, 60, 0.4, 0.8, 0.4, 0.3);
        sl.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.2f);
        if (why != null) {
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Воскрешён!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(why).withStyle(ChatFormatting.GREEN)));
        }
        StatsManager.sync(p);
    }

    /** Все духи — сразу в строй без эффектов (сброс этапа, переход). */
    public static void clearAll(net.minecraft.server.MinecraftServer server) {
        for (UUID id : java.util.List.copyOf(SPIRITS.keySet())) {
            SPIRITS.remove(id);
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.getPersistentData().remove(FALLEN_TAG);
                p.setGameMode(GameType.SURVIVAL);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && SPIRITS.remove(p.getUUID()) != null) {
            p.getPersistentData().remove(FALLEN_TAG);
            p.setGameMode(GameType.SURVIVAL);
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
