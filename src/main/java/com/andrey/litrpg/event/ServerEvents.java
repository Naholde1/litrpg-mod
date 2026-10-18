package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.entity.GoblinKingEntity;
import com.andrey.litrpg.entity.LichEntity;
import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.registry.ModDimensions;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.StatsProvider;
import com.andrey.litrpg.stats.Title;
import com.andrey.litrpg.world.Zone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class ServerEvents {
    private static final String ZONE_TAG = "litrpg_zone";

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            StatsProvider provider = new StatsProvider();
            event.addCapability(LitRPG.id("stats"), provider);
        }
    }

    /** Смерть, возвращение из Края и любое другое пересоздание игрока — переносим всё. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerStats old = StatsManager.get(original);
        PlayerStats now = StatsManager.get(event.getEntity());
        now.copyFrom(old);
        // страховка: если данные старого игрока всё же недоступны — берём резервную копию
        if (event.getEntity() instanceof ServerPlayer sp) StatsManager.restoreIfLost(sp);
        if (event.isWasDeath()) {
            now.loseXpPercent(0.10);
            now.setMana(now.getMaxMana() / 2.0);
        }
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (StatsManager.restoreIfLost(player)) {
            LitRPG.LOGGER.warn("Restored LitRPG data for {} from backup", player.getGameProfile().getName());
        }
        StatsManager.applyAttributes(player);
        PlayerStats s = StatsManager.get(player);
        if (!s.isWelcomed()) {
            s.setWelcomed(true);
            s.setMana(s.getMaxMana());
            StatsManager.message(player, Component.literal("Инициализация завершена. Добро пожаловать в мир Системы, ")
                    .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
                    .append("."));
            StatsManager.message(player, Component.literal("Убивай монстров, получай опыт и уровни. Каждый уровень даёт 3 очка характеристик."));
            StatsManager.message(player, Component.literal("K — персонаж, M — карта, Z/X/C/V/B — навыки."));
        }
        StatsManager.sync(player);
        if (!s.hasOrigin()) StatsManager.openOriginScreen(player);
        else if (s.canChooseClass()) StatsManager.offerClasses(player);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsManager.restoreIfLost(player);
            StatsManager.applyAttributes(player);
            player.setHealth(player.getMaxHealth());
            if (!event.isEndConquered()) {
                StatsManager.message(player, Component.literal("Вы погибли. Потеряно 10% текущего опыта.").withStyle(ChatFormatting.RED));
                StatsManager.count(player, Title.Counter.DEATHS, 1);
            }
            StatsManager.sync(player);
            PlayerStats s = StatsManager.get(player);
            if (!s.hasOrigin()) StatsManager.openOriginScreen(player);
            else if (s.canChooseClass() && !s.getOffers().isEmpty()) StatsManager.offerClasses(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (event.getTo() == ModDimensions.SHADOW_REALM) StatsManager.count(player, Title.Counter.REALM_VISITS, 1);
            StatsManager.applyAttributes(player);
            StatsManager.sync(player);
        }
    }

    /** Смена режима игры: модификаторы и данные должны остаться как были. */
    @SubscribeEvent
    public static void onGameMode(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsManager.restoreIfLost(player);
            player.server.execute(() -> {
                StatsManager.applyAttributes(player);
                StatsManager.sync(player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        PlayerStats s = StatsManager.get(player);
        if (s.getMana() < s.getMaxMana()) {
            s.setMana(s.getMana() + s.getManaRegenPerSecond());
            StatsManager.sync(player);
        }
        if (player.tickCount % 40 == 0) {
            racePassives(player, s);
            checkZone(player);
        }
    }

    private static void racePassives(ServerPlayer player, PlayerStats s) {
        Race race = s.getRace();
        if (race == null) return;
        switch (race) {
            case DWARF -> {
                if (player.getY() < 50) hidden(player, MobEffects.DIG_SPEED, 0);
            }
            case BEASTKIN -> hidden(player, MobEffects.JUMP, 0);
            case DEMONKIN -> {
                if (player.isOnFire() || player.isInLava()) hidden(player, MobEffects.FIRE_RESISTANCE, 0);
            }
            case ELF -> {
                if (player.level().isNight() || player.getY() < 40) hidden(player, MobEffects.NIGHT_VISION, 0, 300);
            }
            default -> {}
        }
    }

    private static void hidden(ServerPlayer p, MobEffect effect, int amp) {
        hidden(p, effect, amp, 100);
    }

    private static void hidden(ServerPlayer p, MobEffect effect, int amp, int duration) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur == null || cur.getDuration() < duration - 40) {
            p.addEffect(new MobEffectInstance(effect, duration, amp, true, false, false));
        }
    }

    private static void checkZone(ServerPlayer player) {
        Zone zone = Zone.at(player.serverLevel(), player.blockPosition());
        String prev = player.getPersistentData().getString(ZONE_TAG);
        if (zone.key.equals(prev)) return;
        player.getPersistentData().putString(ZONE_TAG, zone.key);
        if (prev.isEmpty()) return;
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(zone.nameKey()).withStyle(zone.color)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Уровень монстров: " + zone.minLevel + "–" + zone.maxLevel).withStyle(ChatFormatting.GRAY)));
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level) {
            MobLevels.assignLevel(mob, level);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || dead instanceof Player) return;

        if (dead instanceof LitBoss) {
            // опыт за босса получают все игроки рядом
            AABB area = dead.getBoundingBox().inflate(48);
            for (ServerPlayer p : dead.level().getEntitiesOfClass(ServerPlayer.class, area)) {
                StatsManager.message(p, Component.literal("Босс повержен: ")
                        .append(dead.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
                StatsManager.giveXp(p, MobLevels.xpReward(dead, StatsManager.get(p).getLevel()));
                if (dead instanceof GoblinKingEntity) StatsManager.count(p, Title.Counter.GOBLIN_KING, 1);
                if (dead instanceof LichEntity) StatsManager.count(p, Title.Counter.LICH, 1);
                StatsManager.count(p, Title.Counter.KILLS, 1);
            }
            return;
        }

        Entity src = event.getSource().getEntity();
        // убийство прислужником засчитывается хозяину
        if (src != null && com.andrey.litrpg.ability.Minions.owner(src) != null) src = com.andrey.litrpg.ability.Minions.owner(src);
        if (src instanceof ServerPlayer player) {
            PlayerStats s = StatsManager.get(player);
            int xp = MobLevels.xpReward(dead, s.getLevel());
            StatsManager.giveXp(player, xp);
            if (dead instanceof Enemy) {
                StatsManager.count(player, Title.Counter.KILLS, 1);
                if (dead instanceof GoblinEntity) StatsManager.count(player, Title.Counter.GOBLINS, 1);
                if (dead.getMobType() == MobType.UNDEAD) StatsManager.count(player, Title.Counter.UNDEAD, 1);
                if (MobLevels.getRank(dead) != MobLevels.Rank.NORMAL) StatsManager.count(player, Title.Counter.ELITES, 1);
                if (MobLevels.getLevel(dead) - s.getLevel() >= 10) StatsManager.count(player, Title.Counter.GIANT_KILLS, 1);
            }
        }
    }
}
