package com.andrey.litrpg.client;

import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.QuestActionPacket;
import com.andrey.litrpg.quest.Quest;
import com.andrey.litrpg.quest.QuestEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/** Общая отрисовка карточек заданий для окна персонажа и доски поручений. */
public final class QuestUi {
    private QuestUi() {}

    public static int sourceColor(Quest q) {
        return switch (q.source) {
            case STORY -> 0xFFFFD54F;
            case SYSTEM -> 0xFF8FE3FF;
            case FACTION -> q.faction == null ? 0xFFFFFFFF : 0xFF000000 | q.faction.color;
            case CITY -> 0xFFE8B070;
            case CHAIN -> 0xFFFF8A50;
        };
    }

    public static String rewardText(Quest q) {
        StringBuilder sb = new StringBuilder();
        if (q.xp > 0) sb.append(q.xp).append(" опыта");
        if (q.coins > 0) sb.append(sb.length() > 0 ? " • " : "").append(QuestEvents.coinText(q.coins));
        if (q.rep > 0) sb.append(" • репутация +").append(q.rep);
        if (q.gearRarity >= 0) sb.append(" • предмет: ").append(Rarity.byId(q.gearRarity).title.toLowerCase());
        if (!q.bonusItem.isEmpty() && q.bonusCount > 0) {
            Item it = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(q.bonusItem));
            if (it != null) sb.append(" • ").append(it.getDescription().getString()).append(" ×").append(q.bonusCount);
        }
        return sb.toString();
    }

    /** Сколько нужных предметов у игрока (для «Принести»). */
    public static int carried(Quest q) {
        var player = Minecraft.getInstance().player;
        Item it = q.item();
        return player == null || it == null ? 0 : player.getInventory().countItem(it);
    }

    public static float progress(Quest q) {
        if (q.type == Quest.Type.COLLECT) return Math.min(1f, carried(q) / (float) q.need);
        if (q.type == Quest.Type.LEVEL) return Math.min(1f, ClientData.STATS.getLevel() / (float) q.need);
        return Math.min(1f, q.have / (float) q.need);
    }

    public static String goal(Quest q) {
        if (q.type == Quest.Type.COLLECT) {
            Item it = q.item();
            return "Принесите " + (it == null ? q.target : it.getDescription().getString()) + ": " + Math.min(carried(q), q.need) + "/" + q.need;
        }
        if (q.type == Quest.Type.LEVEL) return "Уровень: " + Math.min(ClientData.STATS.getLevel(), q.need) + "/" + q.need;
        return q.goalText();
    }

    /**
     * Карточка задания. @return высота
     */
    public static int card(GuiGraphics g, Font font, Quest q, int x, int y, int w, boolean desc) {
        int h = desc ? 40 + Math.min(3, font.split(Component.literal(q.desc), w - 12).size()) * 10 : 38;
        g.fill(x, y, x + w, y + h, 0x700C1824);
        UiKit.frame(g, x, y, w, h, UiKit.FRAME_DIM);
        g.fill(x + 1, y + 1, x + 3, y + h - 1, sourceColor(q));
        int ty = y + 4;
        String tag = switch (q.source) {
            case STORY -> "Сюжет";
            case SYSTEM -> "Система";
            case FACTION -> q.faction == null ? "" : q.faction.title;
            case CITY -> q.giver + (q.placeName.isEmpty() ? "" : " • " + q.placeName);
            case CHAIN -> "★ Сюжет" + (q.placeName.isEmpty() ? "" : " • " + q.placeName);
        };
        // заголовок и метка не налезают друг на друга: метку ужимаем до трети ширины, заголовок — до остатка
        int tagMax = w / 3;
        if (font.width(tag) > tagMax) tag = font.plainSubstrByWidth(tag, tagMax - 6) + "…";
        String title = q.title;
        int titleMax = w - 20 - font.width(tag);
        if (font.width(title) > titleMax) title = font.plainSubstrByWidth(title, titleMax - 6) + "…";
        g.drawString(font, title, x + 7, ty, sourceColor(q), false);
        g.drawString(font, tag, x + w - 6 - font.width(tag), ty, UiKit.MUTED, false);
        ty += 11;
        if (desc) {
            var lines = font.split(Component.literal(q.desc), w - 12);
            for (int i = 0; i < Math.min(3, lines.size()); i++) {
                g.drawString(font, lines.get(i), x + 7, ty, UiKit.MUTED, false);
                ty += 10;
            }
        }
        String goal = goal(q);
        if (font.width(goal) > w - 100) goal = font.plainSubstrByWidth(goal, w - 106) + "…";
        g.drawString(font, goal, x + 7, ty, UiKit.TEXT, false);
        UiKit.bar(g, x + w - 86, ty + 2, 78, 4, progress(q), 0xFF1A2632, progress(q) >= 1f ? 0xFF66DD44 : 0xFF4FC3F7);
        ty += 11;
        String reward = rewardText(q);
        if (font.width(reward) > w - 14) reward = font.plainSubstrByWidth(reward, w - 20) + "…";
        g.drawString(font, reward, x + 7, ty, 0xFFE6C870, false);
        return h;
    }

    public static void send(QuestActionPacket.Action action, String source, String id) {
        ModNetwork.CHANNEL.sendToServer(new QuestActionPacket(action, source, id));
    }
}
