/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.registry.ModSounds;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class Credits {
    private static final String[][] LINES = new String[][]{{"ПОДЗЕМЕЛЬЕ ПРОЙДЕНО", "Ксал'Атар повержен. Шпиль Бездны рушится."}, {"Склеп Забытых", "Король-лич больше не встанет"}, {"Паучья чаща", "Ведьма Тьмы ушла в свою же тень"}, {"Затопленный храм", "Глубины вернули храм морю"}, {"Кузни Пепла", "Горны остыли навсегда"}, {"Шпиль Бездны", "Время снова течёт вперёд"}, {"LitRPG: Система", "Идея и игра — Андрей"}, {"Код, карты, хранители", "Claude"}, {"Спасибо за игру!", "Система довольна. Пока что."}};

    private Credits() {
    }

    public static void play(MinecraftServer server) {
        SoundEvent song = ModSounds.music("jane_hoodtrap");
        if (song != null) {
            for(ServerPlayer p : server.getPlayerList().getPlayers()) {
                p.playNotifySound(song, SoundSource.MUSIC, 1.0F, 1.0F);
            }
        }

        for(int i = 0; i < LINES.length; ++i) {
            String[] l = LINES[i];
            boolean first = i == 0;
            Scheduler.later(20 + i * 90, () -> {
                for(ServerPlayer p : server.getPlayerList().getPlayers()) {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 60, 15));
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(l[0]).withStyle(new ChatFormatting[]{first ? ChatFormatting.GOLD : ChatFormatting.YELLOW, ChatFormatting.BOLD})));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(l[1]).withStyle(ChatFormatting.GRAY)));
                    if (first) {
                        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 0.8F);
                    } else {
                        p.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.MASTER, 0.8F, 0.6F + 0.1F * (float)(LINES.length % 5));
                    }
                }

            });
        }

    }
}
