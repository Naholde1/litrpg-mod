/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public final class Lore {
    private static final String SEEN = "litrpg_lore_seen";

    private Lore() {
    }

    static void tick(ServerPlayer p, RunState st, Layout.Stage s) {
        List<Layout.Lore> list = s == null ? Layout.get().lobbyLore : s.lore();
        if (!list.isEmpty()) {
            CompoundTag seen = p.getPersistentData().getCompound("litrpg_lore_seen");
            String var10000 = s == null ? "l" : "s" + s.n();
            String prefix = var10000 + ":" + st.wipes + ":" + st.stageStart + ":";

            for(int i = 0; i < list.size(); ++i) {
                Layout.Lore l = (Layout.Lore)list.get(i);
                if (l.box().contains(p.position())) {
                    String key = prefix + i;
                    if (!seen.getBoolean(key)) {
                        if (seen.size() > 400) {
                            seen = new CompoundTag();
                        }

                        seen.putBoolean(key, true);
                        p.getPersistentData().put("litrpg_lore_seen", seen);
                        if (!l.title().isEmpty()) {
                            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
                            p.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
                            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(l.title()).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.ITALIC})));
                        }

                        if (!l.text().isEmpty()) {
                            p.sendSystemMessage(Component.literal("✧ ").withStyle(ChatFormatting.DARK_AQUA).append(Component.literal(l.text()).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC})));
                        }
                    }
                }
            }

        }
    }
}
