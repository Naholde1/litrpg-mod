package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.SyncStatsPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class StatsManager {
    private static final UUID HEALTH_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a01");
    private static final UUID DAMAGE_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a02");
    private static final UUID SPEED_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a03");
    private static final UUID ATK_SPEED_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a04");
    private static final UUID ARMOR_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a05");

    public static PlayerStats get(Player player) {
        return player.getCapability(StatsProvider.STATS).orElseGet(PlayerStats::new);
    }

    public static MutableComponent system() {
        return Component.literal("[Система] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    public static void message(ServerPlayer player, Component text) {
        player.sendSystemMessage(system().append(Component.empty().append(text).withStyle(ChatFormatting.WHITE)));
    }

    public static void sync(ServerPlayer player) {
        PlayerStats s = get(player);
        long now = player.level().getGameTime();
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncStatsPacket(s, now));
    }

    public static void applyAttributes(ServerPlayer player) {
        PlayerStats s = get(player);
        int str = s.getStat(Stat.STRENGTH);
        int agi = s.getStat(Stat.AGILITY);
        int vit = s.getStat(Stat.VITALITY);
        int lvl = s.getLevel();

        set(player, Attributes.MAX_HEALTH, HEALTH_ID, "litrpg_health",
                vit * 1.0 + (lvl - 1) / 2 * 1.0, AttributeModifier.Operation.ADDITION);
        set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "litrpg_damage",
                str * 0.4, AttributeModifier.Operation.ADDITION);
        set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "litrpg_speed",
                Math.min(agi * 0.008, 0.6), AttributeModifier.Operation.MULTIPLY_BASE);
        set(player, Attributes.ATTACK_SPEED, ATK_SPEED_ID, "litrpg_attack_speed",
                Math.min(agi * 0.015, 1.0), AttributeModifier.Operation.MULTIPLY_BASE);
        set(player, Attributes.ARMOR, ARMOR_ID, "litrpg_armor",
                Math.min(vit * 0.2, 10.0), AttributeModifier.Operation.ADDITION);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        s.setMana(s.getMana());
    }

    private static void set(ServerPlayer player, Attribute attr, UUID id, String name, double value,
                            AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) {
            inst.addPermanentModifier(new AttributeModifier(id, name, value, op));
        }
    }

    public static void giveXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        PlayerStats s = get(player);
        int before = s.getLevel();
        int gained = s.addXp(amount);
        player.displayClientMessage(Component.literal("+" + amount + " опыта").withStyle(ChatFormatting.GREEN), true);
        if (gained > 0) {
            onLevelUp(player, before, s.getLevel());
        }
        sync(player);
    }

    private static void onLevelUp(ServerPlayer player, int from, int to) {
        PlayerStats s = get(player);
        applyAttributes(player);
        player.setHealth(player.getMaxHealth());
        s.setMana(s.getMaxMana());

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("УРОВЕНЬ ПОВЫШЕН").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Уровень " + to + "  •  +" + (to - from) * PlayerStats.POINTS_PER_LEVEL + " очков характеристик")
                        .withStyle(ChatFormatting.YELLOW)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);

        message(player, Component.literal("Поздравляем! Достигнут уровень ")
                .append(Component.literal(String.valueOf(to)).withStyle(ChatFormatting.GOLD))
                .append(". Свободных очков: " + s.getFreePoints() + " (открой окно персонажа: K)"));

        for (Ability a : Ability.values()) {
            if (a.unlockLevel > from && a.unlockLevel <= to) {
                message(player, Component.literal("Получен навык: ")
                        .append(Component.translatable(a.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(" — ")
                        .append(Component.translatable(a.descKey())));
            }
        }
    }
}
