package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.StatsProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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

    @SubscribeEvent
    public static void attachCaps(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            StatsProvider provider = new StatsProvider();
            event.addCapability(LitRPG.id("stats"), provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        PlayerStats old = StatsManager.get(original);
        PlayerStats now = StatsManager.get(event.getEntity());
        now.copyFrom(old);
        if (event.isWasDeath()) {
            now.loseXpPercent(0.10);
            now.setMana(now.getMaxMana() / 2.0);
        }
        original.invalidateCaps();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsManager.applyAttributes(player);
            PlayerStats s = StatsManager.get(player);
            if (!s.isWelcomed()) {
                s.setWelcomed(true);
                s.setMana(s.getMaxMana());
                StatsManager.message(player, Component.literal("Инициализация завершена. Добро пожаловать, игрок ")
                        .append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.GOLD))
                        .append("."));
                StatsManager.message(player, Component.literal("Убивай монстров, получай опыт и уровни. Каждый уровень даёт 3 очка характеристик."));
                StatsManager.message(player, Component.literal("K — персонаж, M — карта мира, N — мини-карта, Z/X/C/V/B — навыки."));
                StatsManager.message(player, Component.literal("Скрафти «Ключ Системы», чтобы попасть в Изнанку — мир сильных монстров и боссов."));
            }
            StatsManager.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            StatsManager.applyAttributes(player);
            player.setHealth(player.getMaxHealth());
            if (!event.isEndConquered()) {
                StatsManager.message(player, Component.literal("Вы погибли. Потеряно 10% текущего опыта.").withStyle(ChatFormatting.RED));
            }
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

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        PlayerStats s = StatsManager.get(player);
        if (s.getMana() < s.getMaxMana()) {
            s.setMana(s.getMana() + s.getManaRegenPerSecond());
            StatsManager.sync(player);
        }
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
            // Опыт за босса получают все игроки рядом
            AABB area = dead.getBoundingBox().inflate(48);
            for (ServerPlayer p : dead.level().getEntitiesOfClass(ServerPlayer.class, area)) {
                StatsManager.message(p, Component.literal("Босс повержен: ")
                        .append(dead.getDisplayName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
                StatsManager.giveXp(p, MobLevels.xpReward(dead, StatsManager.get(p).getLevel()));
            }
            return;
        }

        Entity src = event.getSource().getEntity();
        if (src instanceof ServerPlayer player) {
            int xp = MobLevels.xpReward(dead, StatsManager.get(player).getLevel());
            if (dead instanceof GoblinEntity) xp += 2;
            StatsManager.giveXp(player, xp);
        }
    }
}
