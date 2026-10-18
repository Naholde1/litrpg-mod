package com.andrey.litrpg.client;

import com.andrey.litrpg.network.QuestActionPacket;
import com.andrey.litrpg.quest.Quest;
import com.andrey.litrpg.quest.QuestLog;
import com.andrey.litrpg.rep.Faction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/** Доска поручений народа: открывается при разговоре с жителем (не торговцем). */
public class QuestBoardScreen extends SystemScreen {
    private final String factionKey;
    private final String npcName;
    private final List<Hit> hits = new ArrayList<>();

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    public QuestBoardScreen(String factionKey, String npcName) {
        super(Component.literal("Поручения"), 360, 262);
        this.factionKey = factionKey;
        this.npcName = npcName;
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        Faction f = Faction.byKey(factionKey);
        String title = "◆ ПОРУЧЕНИЯ: " + (f == null ? "?" : f.title.toUpperCase()) + " ◆";
        UiKit.window(g, left, top, W, H, title, font);
        if (f == null) return;
        int rep = ClientData.STATS.getRep(f);
        Faction.Tier tier = Faction.Tier.of(rep);
        int x = left + 10, y = top + 24;
        g.drawString(font, npcName + ": «Чем поможешь нашему народу?»", x, y, UiKit.TEXT, false);
        y += 11;
        String rt = "Отношение: " + tier.title + " (" + rep + ")";
        g.drawString(font, Component.literal(rt).withStyle(tier.color), x, y, 0xFFFFFFFF, false);
        y += 13;

        QuestLog log = ClientData.STATS.quests();
        List<Quest> offers = log.offers(factionKey);
        if (offers.isEmpty()) {
            g.drawString(font, "Новых поручений пока нет — загляните завтра.", x, y + 4, UiKit.MUTED, false);
            y += 18;
        }
        for (Quest q : offers) {
            int h = QuestUi.card(g, font, q, x, y, W - 70, false);
            button(g, mx, my, left + W - 56, y + 11, 46, 16, "Принять", 0x4CAF50,
                    () -> QuestUi.send(QuestActionPacket.Action.ACCEPT, factionKey, q.id));
            y += h + 4;
        }
        y += 2;
        g.fill(x, y, left + W - 10, y + 1, UiKit.FRAME_DIM);
        y += 5;
        g.drawString(font, "Ваши задания (" + log.active.size() + "/" + QuestLog.MAX_ACTIVE + ")", x, y, UiKit.GOLD, false);
        y += 12;
        for (Quest q : log.active) {
            if (y > top + H - 20) break;
            String line = q.title + " — " + QuestUi.goal(q);
            if (font.width(line) > W - 80) line = font.plainSubstrByWidth(line, W - 86) + "…";
            g.drawString(font, line, x, y + 3, QuestUi.sourceColor(q), false);
            if (q.type == Quest.Type.COLLECT && QuestUi.carried(q) >= q.need) {
                button(g, mx, my, left + W - 56, y, 46, 14, "Сдать", 0xFFB300,
                        () -> QuestUi.send(QuestActionPacket.Action.TURN_IN, "", q.id));
            }
            y += 16;
        }
    }

    private void button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, int accent, Runnable r) {
        boolean hov = UiKit.inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? 0xF0204A30 : 0xE0122636);
        UiKit.frame(g, x, y, w, h, 0xFF000000 | accent);
        g.drawString(font, label, x + w / 2 - font.width(label) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, h, r));
    }

    @Override
    protected boolean clickContent(double mx, double my, int button) {
        for (Hit h : hits) {
            if (UiKit.inside(mx, my, h.x, h.y, h.w, h.h)) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                h.action.run();
                return true;
            }
        }
        return false;
    }
}
