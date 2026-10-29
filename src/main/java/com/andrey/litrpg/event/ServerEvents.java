package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            StatsProvider provider = new StatsProvider();
            event.addCapability(LitRPG.id("stats"), provider);
        }
    }

    /** Возвращение из Края и любое другое пересоздание игрока — переносим всё (в подземелье не умирают). */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerStats old = StatsManager.get(original);
        PlayerStats now = StatsManager.get(event.getEntity());
        now.copyFrom(old);
        // страховка: если данные старого игрока всё же недоступны — берём резервную копию
        if (event.getEntity() instanceof ServerPlayer sp) StatsManager.restoreIfLost(sp);
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
            StatsManager.message(player, Component.literal("Инициализация завершена. Добро пожаловать в Подземелье Системы, ")
                    .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
                    .append("."));
            StatsManager.message(player, Component.literal("Пять этапов, пятнадцать хранителей. Соберите группу из 3–5 искателей и встаньте на печать у врат."));
            StatsManager.message(player, Component.literal("K — персонаж и навыки, Alt — рывок, Z X C V B G — навыки, Ctrl+1…5 — выбрать союзника для лечения.")
                    .withStyle(ChatFormatting.GRAY));
        }
        StatsManager.sync(player);
        if (s.getRole() == null) StatsManager.openOriginScreen(player);
        else if (s.canChooseClass()) StatsManager.offerClasses(player);
    }

    /** Резервная копия данных — при автосохранении и выходе, а не на каждое изменение. */
    @SubscribeEvent
    public static void onSave(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer p) StatsManager.backup(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) StatsManager.backup(p);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsManager.restoreIfLost(player);
            StatsManager.applyAttributes(player);
            player.setHealth(player.getMaxHealth());
            StatsManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
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
        double was = s.getResource();
        com.andrey.litrpg.stats.Resource r = s.resource();
        if (r.mirrorsHealth()) {
            // шкала класса «на здоровье» — это само здоровье
            s.setResource(100.0 * player.getHealth() / Math.max(1f, player.getMaxHealth()));
        } else if (r == com.andrey.litrpg.stats.Resource.RAGE) {
            // ярость не копится сама: вне боя она утекает
            if (!com.andrey.litrpg.ability.SkillEffects.inCombat(player)
                    && player.level().getGameTime() - s.lastCombat > com.andrey.litrpg.stats.Resource.RAGE_CALM_TICKS) {
                s.setResource(was - com.andrey.litrpg.stats.Resource.RAGE_DECAY);
            }
        } else if (was < s.getMaxResource() && s.getForm() == null
                && !com.andrey.litrpg.ability.Possession.active(player)) {
            // пока облик надет, шкала только тратится: иначе расход и восстановление
            // гасят друг друга и облик держится вечно
            s.setResource(was + s.getResourceRegen());
        }
        // облик держится, пока есть шкала: тратим и обновляем эффекты
        com.andrey.litrpg.ability.Forms.tick(player, s);
        // объявленный закон сам проверяет, кто его соблюдает
        com.andrey.litrpg.ability.Laws.tick(player);
        // Дух сидит в носителе, пока есть шкала
        com.andrey.litrpg.ability.Possession.tick(player, s);
        if (Math.abs(s.getResource() - was) > 0.001) StatsManager.syncMana(player);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob && event.getLevel() instanceof ServerLevel level) {
            MobLevels.assignLevel(mob, level);
        }
    }
}
