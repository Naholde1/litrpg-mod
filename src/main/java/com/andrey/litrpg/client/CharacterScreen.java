package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.event.CombatEvents;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.ProfessionPacket;
import com.andrey.litrpg.network.SpendPointPacket;
import com.andrey.litrpg.stats.ClassOffers;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Style;
import java.util.Map;
import com.andrey.litrpg.stats.Profession;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/** Окно персонажа с вкладками: Статус, Навыки, Профессии, Титулы. */
public class CharacterScreen extends SystemScreen {
    private static final String[] TABS = {"Статус", "Навыки", "Профессии", "Титулы"};

    private static int tab = 0;
    private int titleScroll = 0;
    private final List<Hit> hits = new ArrayList<>();
    private Component tooltip;

    private record Hit(int x, int y, int w, int h, Runnable action) {}

    public CharacterScreen() {
        super(Component.literal("Персонаж"), 380, 250);
    }

    @Override
    protected void renderContent(GuiGraphics g, int mx, int my, float pt) {
        hits.clear();
        tooltip = null;
        UiKit.window(g, left, top, W, H, "◆ СТАТУС ИГРОКА ◆", font);

        // вкладки
        int tx = left + 8;
        for (int i = 0; i < TABS.length; i++) {
            int tw = font.width(TABS[i]) + 16;
            boolean hov = UiKit.inside(mx, my, tx, top + 21, tw, 15);
            UiKit.tile(g, font, tx, top + 21, tw, 15, TABS[i], tab == i, hov, 0x4FC3F7);
            final int idx = i;
            hits.add(new Hit(tx, top + 21, tw, 15, () -> { tab = idx; titleScroll = 0; }));
            tx += tw + 4;
        }

        PlayerStats s = ClientData.STATS;
        int cx = left + 10, cy = top + 44;
        switch (tab) {
            case 0 -> status(g, s, cx, cy, mx, my);
            case 1 -> skills(g, s, cx, cy, mx, my);
            case 2 -> professions(g, s, cx, cy, mx, my);
            case 3 -> titles(g, s, cx, cy, mx, my);
        }

        if (tooltip != null) g.renderTooltip(font, font.split(tooltip, 220), mx, my);
    }

    // ---------------- Статус ----------------

    private void status(GuiGraphics g, PlayerStats s, int x, int y, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        String name = mc.player != null ? mc.player.getGameProfile().getName() : "?";
        g.drawString(font, name, x, y, 0xFFFFFFFF, false);
        String origin = (s.getRace() == null ? "—" : Component.translatable(s.getRace().nameKey()).getString())
                + " • " + (s.getPlayerClass() == null ? "Новичок" : Component.translatable(s.getPlayerClass().nameKey()).getString());
        g.drawString(font, origin, x, y + 11, s.getPlayerClass() == null ? UiKit.TEXT : 0xFF000000 | s.getPlayerClass().color, false);
        g.drawString(font, "Уровень " + s.getLevel(), x, y + 22, UiKit.GOLD, false);

        int bx = x + 175;
        int xpNeed = s.xpForNext();
        boolean max = s.getLevel() >= PlayerStats.MAX_LEVEL;
        g.drawString(font, "Опыт: " + (max ? "МАКС." : s.getXp() + " / " + xpNeed), bx, y, 0xFF88EE66, false);
        UiKit.bar(g, bx, y + 11, 180, 5, max ? 1f : s.getXp() / (float) xpNeed, 0xFF1F3A1F, 0xFF66DD44);
        g.drawString(font, "Мана: " + (int) s.getMana() + " / " + s.getMaxMana(), bx, y + 20, 0xFF5AA0F0, false);
        UiKit.bar(g, bx, y + 31, 180, 5, (float) (s.getMana() / Math.max(1, s.getMaxMana())), 0xFF1A2340, 0xFF3D8BFF);

        int sy = y + 46;
        g.fill(x - 2, sy - 4, left + W - 8, sy - 3, UiKit.FRAME_DIM);
        int pointsColor = s.getFreePoints() > 0 ? 0xFF7CFC00 : UiKit.MUTED;
        g.drawString(font, "Свободные очки: " + s.getFreePoints(), x, sy, pointsColor, false);
        if (s.getFreePoints() > 0) g.drawString(font, "Shift — по 5", x + 110, sy, UiKit.MUTED, false);

        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            int ry = sy + 14 + i * 17;
            Stat st = stats[i];
            g.drawString(font, Component.translatable(st.translationKey()), x, ry + 3, 0xFF000000 | st.color, false);
            String v = String.valueOf(s.getStat(st));
            g.drawString(font, v, x + 105 - font.width(v), ry + 3, 0xFFFFFFFF, false);
            if (UiKit.inside(mx, my, x, ry, 110, 14)) tooltip = Component.translatable(st.descKey());
            if (s.getFreePoints() > 0) {
                boolean hov = UiKit.inside(mx, my, x + 112, ry, 14, 14);
                g.fill(x + 112, ry, x + 126, ry + 14, hov ? 0xFF2E7D32 : 0xFF1B4D20);
                UiKit.frame(g, x + 112, ry, 14, 14, 0xFF7CFC00);
                g.drawString(font, "+", x + 117, ry + 3, 0xFFFFFFFF, false);
                final int idx = i;
                hits.add(new Hit(x + 112, ry, 14, 14, () -> spend(idx, hasShiftDown() ? 5 : 1)));
            }
        }

        // производные
        int dx = x + 175, dy = sy + 14;
        double hp = mc.player != null ? mc.player.getMaxHealth() : 0;
        double atk = mc.player != null ? mc.player.getAttributeValue(Attributes.ATTACK_DAMAGE) : 0;
        double armor = mc.player != null ? mc.player.getAttributeValue(Attributes.ARMOR) : 0;
        double spd = mc.player != null ? mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1 * 100 : 100;
        String[] lines = {
                String.format("Здоровье: %.0f", hp),
                String.format("Урон оружием: %.1f", atk),
                String.format("Броня: %.1f", armor),
                String.format("Реген маны: %.1f/с", s.getManaRegenPerSecond()),
                String.format("Шанс крита: %.1f%%  (x%.2f)", CombatEvents.critChance(s) * 100, CombatEvents.critMultiplier(s)),
                String.format("Уклонение: %.1f%%", CombatEvents.dodgeChance(s) * 100),
                String.format("Скорость: %.0f%%", spd),
        };
        int[] colors = {0xFFFF6B6B, 0xFFE05A47, 0xFFBBBBBB, 0xFF5AA0F0, 0xFFFFD54F, 0xFF5BD16A, 0xFF5BD16A};
        for (int i = 0; i < lines.length; i++) g.drawString(font, lines[i], dx, dy + i * 11, colors[i], false);

        if (s.getRace() != null) {
            UiKit.wrapped(g, font, Component.literal("✦ ").append(Component.translatable(s.getRace().passiveKey())),
                    x, top + H - 24, W - 20, 0xFFB8E0FF);
        }
    }

    private void spend(int stat, int amount) {
        if (ClientData.STATS.getFreePoints() > 0) {
            ModNetwork.CHANNEL.sendToServer(new SpendPointPacket(stat, amount));
        }
    }

    // ---------------- Навыки ----------------

    private void skills(GuiGraphics g, PlayerStats s, int x, int y, int mx, int my) {
        PlayerClass cls = s.getPlayerClass();
        int slots = cls == null ? Ability.NOVICE.length : Ability.SLOTS;
        int rowH = cls == null ? 38 : 34;
        for (int slot = 0; slot < slots; slot++) {
            Ability a = Ability.forSlot(cls, slot);
            if (a == null) continue;
            int ry = y + slot * rowH;
            int need = Ability.unlockLevel(cls, slot);
            boolean unlocked = s.getLevel() >= need;
            g.fill(x, ry, x + 28, ry + 28, unlocked ? 0xFF2A1E4A : 0xFF222222);
            UiKit.frame(g, x, ry, 28, 28, unlocked ? UiKit.FRAME : UiKit.FRAME_DIM);
            g.pose().pushPose();
            g.pose().translate(x + 14, ry + 10, 0);
            g.pose().scale(1.6f, 1.6f, 1f);
            g.drawString(font, a.icon, -font.width(a.icon) / 2, -3, unlocked ? 0xFFFFFFFF : 0xFF666666, false);
            g.pose().popPose();
            String key = ModKeys.ABILITIES[slot].getTranslatedKeyMessage().getString();
            g.drawString(font, key, x + 2, ry + 19, UiKit.MUTED, false);

            String title = Component.translatable(a.nameKey()).getString();
            g.drawString(font, title, x + 36, ry, unlocked ? 0xFFE0D0FF : 0xFF777777, false);
            String meta = unlocked ? a.manaCost() + " MP • " + String.format("%.1f", a.cooldown() / 20.0) + " с"
                    : "Откроется на уровне " + need;
            g.drawString(font, meta, x + 36 + font.width(title) + 8, ry, unlocked ? 0xFF5AA0F0 : 0xFFFF9800, false);
            UiKit.wrapped(g, font, Component.translatable(a.descKey()), x + 36, ry + 11, W - 56, UiKit.MUTED);
        }
        if (cls != null) return;

        // новичок: прогресс до выбора класса и стиль игры
        int ny = y + slots * rowH + 4;
        g.fill(x - 2, ny - 3, left + W - 8, ny - 2, UiKit.FRAME_DIM);
        if (s.canChooseClass()) {
            g.drawString(font, "Достигнут 10 уровень — выбери класс!", x, ny + 2, UiKit.GOLD, false);
            button(g, x + 230, ny - 2, 120, 16, "Выбрать класс", mx, my, 0xFF1F6F4A,
                    () -> minecraft.setScreen(new ClassChoiceScreen()));
        } else {
            g.drawString(font, "Класс откроется на " + PlayerStats.CLASS_LEVEL + " уровне. Система изучает твой стиль:",
                    x, ny + 2, UiKit.TEXT, false);
        }
        List<Map.Entry<Style, Integer>> top = ClassOffers.topStyles(s, 5);
        int sy = ny + 16;
        if (top.isEmpty()) g.drawString(font, "— пока ничего —", x, sy, UiKit.MUTED, false);
        for (int i = 0; i < top.size(); i++) {
            var e = top.get(i);
            int ry = sy + i * 12;
            g.drawString(font, Component.translatable(e.getKey().nameKey()), x, ry, UiKit.TEXT, false);
            UiKit.bar(g, x + 110, ry + 2, 150, 4, e.getValue() / 100f, 0xFF1A2340, 0xFF4FC3F7);
            g.drawString(font, e.getValue() + "%", x + 268, ry, UiKit.MUTED, false);
        }
    }

    // ---------------- Профессии ----------------

    private void professions(GuiGraphics g, PlayerStats s, int x, int y, int mx, int my) {
        g.drawString(font, "Освоено: " + s.getProfessions().size() + " / " + Profession.MAX_KNOWN
                + "   (Shift + клик по «Забыть» — сбросить профессию)", x, y, UiKit.MUTED, false);
        Profession[] all = Profession.values();
        for (int i = 0; i < all.length; i++) {
            Profession p = all[i];
            int ry = y + 14 + i * 36;
            boolean known = s.knows(p);
            g.fill(x, ry, x + W - 20, ry + 32, known ? 0x80183A52 : 0x600C1824);
            UiKit.frame(g, x, ry, W - 20, 32, known ? (0xFF000000 | p.color) : UiKit.FRAME_DIM);
            g.drawString(font, Component.translatable(p.nameKey()), x + 6, ry + 4, 0xFF000000 | p.color, false);
            UiKit.wrapped(g, font, Component.translatable(p.descKey()), x + 6, ry + 15, W - 120, UiKit.MUTED);
            int bx = x + W - 92;
            if (known) {
                int lvl = s.getProfLevel(p);
                g.drawString(font, "Ур. " + lvl, bx - 40, ry + 4, UiKit.GOLD, false);
                UiKit.bar(g, bx - 40, ry + 15, 34, 4, lvl >= Profession.MAX_LEVEL ? 1f
                        : s.getProfXp(p) / (float) Profession.xpForNext(lvl), 0xFF2A2A2A, 0xFF000000 | p.color);
                button(g, bx, ry + 8, 64, 16, "Забыть", mx, my, 0xFF6A2020, () -> {
                    if (hasShiftDown()) ModNetwork.CHANNEL.sendToServer(new ProfessionPacket(p, false));
                });
            } else if (s.getProfessions().size() < Profession.MAX_KNOWN) {
                button(g, bx, ry + 8, 64, 16, "Изучить", mx, my, 0xFF1F6F4A,
                        () -> ModNetwork.CHANNEL.sendToServer(new ProfessionPacket(p, true)));
            }
        }
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String label, int mx, int my, int color, Runnable r) {
        boolean hov = UiKit.inside(mx, my, x, y, w, h);
        g.fill(x, y, x + w, y + h, hov ? (color | 0xFF202020) : color);
        UiKit.frame(g, x, y, w, h, hov ? 0xFFFFFFFF : UiKit.FRAME_DIM);
        g.drawString(font, label, x + w / 2 - font.width(label) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
        hits.add(new Hit(x, y, w, h, r));
    }

    // ---------------- Титулы ----------------

    private void titles(GuiGraphics g, PlayerStats s, int x, int y, int mx, int my) {
        Title[] all = Title.values();
        g.drawString(font, "Получено: " + s.getTitles().size() + " / " + all.length + "   (колесо — прокрутка)", x, y, UiKit.MUTED, false);
        int rows = 8;
        titleScroll = Math.max(0, Math.min(titleScroll, all.length - rows));
        for (int i = 0; i < rows && i + titleScroll < all.length; i++) {
            Title t = all[i + titleScroll];
            int ry = y + 14 + i * 22;
            boolean has = s.hasTitle(t);
            g.fill(x, ry, x + W - 20, ry + 20, has ? 0x80301A52 : 0x600C1824);
            String name = "«" + Component.translatable(t.nameKey()).getString() + "»";
            g.drawString(font, name, x + 6, ry + 6, has ? 0xFFE0B0FF : 0xFF666666, false);
            StringBuilder b = new StringBuilder();
            for (Stat st : Stat.values()) {
                int v = t.bonus(st);
                if (v != 0) {
                    if (b.length() > 0) b.append(", ");
                    b.append('+').append(v).append(' ').append(Component.translatable(st.translationKey()).getString());
                }
            }
            String right = has ? b.toString() : s.getCounter(t.counter) + " / " + t.threshold;
            g.drawString(font, right, x + W - 26 - font.width(right), ry + 6, has ? 0xFF9CFF9C : 0xFF888888, false);
            if (UiKit.inside(mx, my, x, ry, W - 20, 20)) {
                tooltip = Component.translatable(t.descKey()).copy()
                        .append(Component.literal(has ? "\nБонус: " + b : "").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    // ---------------- ввод ----------------

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

    @Override
    protected boolean scrollContent(double mx, double my, double delta) {
        if (tab == 3) {
            titleScroll -= (int) Math.signum(delta);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ModKeys.CHARACTER.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
