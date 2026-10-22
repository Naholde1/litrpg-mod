package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Места Мира Системы как в литРПГ: «Открыта локация!» с опытом при первом посещении
 * любого места карты и Обелиски Системы — ПКМ по камню-якорю делает обелиск точкой
 * возрождения.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Lore {
    private Lore() {}

    /** Название вида места для подзаголовка. */
    public static String kind(String type) {
        return switch (type) {
            case "city" -> "Город";
            case "dungeon" -> "Подземелье";
            case "wonder" -> "Чудо света";
            case "village" -> "Деревня";
            case "farm", "hamlet" -> "Хутор";
            case "fishing" -> "Рыбацкий посёлок";
            case "lumber" -> "Лесопилка";
            case "caravan" -> "Стоянка караванщиков";
            case "outpost" -> "Застава";
            case "ruins" -> "Руины";
            case "camp" -> "Лагерь";
            case "pirates" -> "Пиратский корабль";
            case "obelisk" -> "Обелиск Системы — точка возрождения";
            case "magetower" -> "Башня мага";
            case "nest" -> "Логово";
            case "witch" -> "Ведьмино логово";
            case "arena" -> "Арена — ударьте в колокол";
            case "star" -> "Место падения звезды";
            default -> "Место";
        };
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 40 != 7) return;
        if (!SystemWorld.isOverworld(p.level()) || p.isSpectator()) return;
        WorldMap map = WorldMap.get();
        PlayerStats s = StatsManager.get(p);
        for (WorldMap.Poi poi : map.pois) {
            if (poi.type().startsWith("nether") || poi.type().equals("spawn")) continue;
            double r = (poi.r() > 0 ? poi.r() : poi.type().equals("city") ? 90 : 30) + 12;
            double dx = p.getX() - poi.x(), dz = p.getZ() - poi.z();
            if (dx * dx + dz * dz > r * r) continue;
            if (!s.discover(poi.id())) continue;
            s.addStyle(com.andrey.litrpg.stats.Style.EXPLORE, 15);
            int lv = map.levelAt(poi.x(), poi.z());
            int xp = (poi.type().equals("city") || poi.type().equals("wonder") ? 60 : 25) + lv * 6;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(poi.name()).withStyle(ChatFormatting.GOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("✦ Открыта локация • " + kind(poi.type()))
                    .withStyle(ChatFormatting.YELLOW)));
            StatsManager.message(p, Component.literal("✦ Открыта локация: ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(poi.name()).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" (уровень земель " + lv + ", открыто мест: " + s.foundCount() + ")").withStyle(ChatFormatting.GRAY)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
            StatsManager.giveXp(p, xp);
            break; // не больше одного открытия за раз — титры не перекрывают друг друга
        }
    }

    /** Обелиск: ПКМ по камню-якорю — точка возрождения рядом с ним. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel sl)) return;
        BlockPos pos = e.getPos();
        if (!sl.getBlockState(pos).is(Blocks.LODESTONE) || p.getItemInHand(e.getHand()).is(Items.COMPASS)) return;
        if (sl.dimension() != Level.OVERWORLD || !SystemWorld.isOverworld(sl)) return;
        WorldMap.Poi ob = null;
        for (WorldMap.Poi poi : WorldMap.get().pois) {
            if (poi.type().equals("obelisk") && Math.abs(poi.x() - pos.getX()) <= 10 && Math.abs(poi.z() - pos.getZ()) <= 10) ob = poi;
        }
        if (ob == null) return;
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        BlockPos spot = pos.south();
        p.setRespawnPosition(Level.OVERWORLD, spot, 180f, true, false);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.4, 0.8, 0.4, 0.05);
        sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.6, 1.5, 0.6, 0.02);
        sl.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1f, 1f);
        StatsManager.message(p, Component.literal("[Система] ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(ob.name() + " запомнил вас. После гибели вы возродитесь здесь.").withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
