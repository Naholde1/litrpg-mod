package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public class HudOverlay {
    private static final int MAP_SIZE = 100;

    public static void render(ForgeGui gui, GuiGraphics g, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator()) return;
        Font font = mc.font;
        PlayerStats s = ClientData.STATS;

        int x = 6;
        int y = 6;

        // --- мини-карта (если не стоит Xaero's Minimap) ---
        if (Minimap.enabled) {
            y += Minimap.render(g, x + 6, y + 8, MAP_SIZE, partialTick) + 8;
        }

        // --- панель уровня ---
        int panelW = 140;
        g.fill(x - 3, y - 3, x + panelW + 3, y + 33, 0xA0050A12);
        UiKit.frame(g, x - 3, y - 3, panelW + 6, 36, UiKit.FRAME_DIM);
        String lvl = "Ур. " + s.getLevel();
        g.drawString(font, lvl, x, y, UiKit.GOLD);
        String cls = s.getPlayerClass() != null ? Component.translatable(s.getPlayerClass().nameKey()).getString()
                : s.canChooseClass() ? "Выбор класса!" : "Новичок";
        g.drawString(font, cls, x + font.width(lvl) + 6, y, s.getPlayerClass() != null ? 0xFF000000 | s.getPlayerClass().color
                : s.canChooseClass() ? 0xFFFFD54F : 0xFFB0BEC5);
        if (s.getFreePoints() > 0) {
            String pts = "+" + s.getFreePoints();
            g.drawString(font, pts, x + panelW - font.width(pts), y, 0xFF7CFC00);
        }
        y += 11;
        float xpFrac = s.getLevel() >= PlayerStats.MAX_LEVEL ? 1f : s.getXp() / (float) s.xpForNext();
        UiKit.bar(g, x, y, panelW, 4, xpFrac, 0xFF1F3A1F, 0xFF66DD44);
        y += 7;
        com.andrey.litrpg.stats.Resource res = s.resource();
        float manaFrac = (float) (s.getResource() / Math.max(1, s.getMaxResource()));
        UiKit.bar(g, x, y, panelW, 5, manaFrac, res.back, res.color);
        y += 8;
        g.drawString(font, (int) s.getResource() + " / " + s.getMaxResource() + " " + res.tag, x, y, res.color, false);

        abilities(g, font, s, height);
        castBar(g, font, width, height, partialTick);
        target(g, font, mc, width);
        party(g, font, y + 14);
    }

    /** Панель навыков (снизу слева): шесть ячеек, над ними — сферы заклинателя или приёмы серии. */
    private static void abilities(GuiGraphics g, Font font, PlayerStats s, int height) {
        int slot = 22;
        int ax = 6;
        int ay = height - slot - 6;
        var cls = s.getPlayerClass();
        int accent = cls != null ? cls.color : 0x4FC3F7;
        int slots = Math.min(cls == null ? Ability.NOVICE.length : Ability.BAR, ModKeys.ABILITIES.length);
        com.andrey.litrpg.stats.Resource res = s.resource();
        for (int i = 0; i <= slots; i++) {
            boolean dash = i == slots;
            Ability a = s.barSkill(dash ? Ability.DASH_SLOT : i);
            int sx = ax + i * (slot + 4) + (dash ? 6 : 0);
            String key = (dash ? ModKeys.DASH : ModKeys.ABILITIES[i]).getTranslatedKeyMessage().getString();
            if (dash && key.toLowerCase().contains("alt")) key = "Alt";
            if (key.length() > 3) key = key.substring(0, 1);
            g.drawString(font, key, sx + 1, ay - 9, 0xFFBBBBBB);
            g.fill(sx - 1, ay - 1, sx + slot + 1, ay + slot + 1, 0xFF000000);
            if (a == null) {
                g.fill(sx, ay, sx + slot, ay + slot, 0x80202020);
                UiKit.frame(g, sx, ay, slot, slot, 0xFF333333);
                continue;
            }
            int need = Ability.unlockLevel(cls, a);
            boolean unlocked = s.getLevel() >= need;
            int cd = ClientData.COOLDOWN[a.ordinal()];
            boolean enoughMana = s.getResource() >= res.cost(a);

            int bg = !unlocked ? 0xFF303030 : enoughMana ? 0xFF2A1E4A : 0xFF1C1C34;
            g.fill(sx, ay, sx + slot, ay + slot, bg);
            UiKit.frame(g, sx, ay, slot, slot, unlocked && cd <= 0 && enoughMana ? 0xFF000000 | accent : 0xFF444444);

            int iconColor = !unlocked ? 0xFF666666 : enoughMana ? 0xFFFFFFFF : 0xFF7A8AFF;
            g.drawString(font, a.icon, sx + slot / 2 - font.width(a.icon) / 2, ay + 7, iconColor);

            if (unlocked && cd > 0) {
                float frac = cd / (float) Math.max(1, a.cooldown());
                int h = (int) (slot * Math.min(1f, frac));
                g.fill(sx, ay + slot - h, sx + slot, ay + slot, 0xB0000000);
                String sec = String.valueOf((cd + 19) / 20);
                g.drawString(font, sec, sx + slot / 2 - font.width(sec) / 2, ay + 7, 0xFFFF6060);
            }
            if (!unlocked) {
                String lv = String.valueOf(need);
                g.drawString(font, lv, sx + slot - font.width(lv) - 1, ay + slot - 8, 0xFFFF9800, false);
            }
        }
        int top = ay - 20;
        if (cls != null && cls.isInvoker()) {
            // сферы заклинателя
            var orbs = s.getOrbs();
            for (int i = 0; i < 3; i++) {
                int ox = ax + i * 12;
                g.fill(ox, top, ox + 9, top + 9, 0xFF000000);
                if (i < orbs.size()) {
                    Ability sp = Ability.byKey(cls.invoke.spheres()[orbs.get(i)]);
                    g.fill(ox + 1, top + 1, ox + 8, top + 8, orbColor(orbs.get(i)));
                    if (sp != null) g.drawString(font, sp.icon, ox + 5 - font.width(sp.icon) / 2, top + 1, 0xFFFFFFFF, false);
                } else {
                    g.fill(ox + 1, top + 1, ox + 8, top + 8, 0xFF202028);
                }
            }
        } else if (s.comboPoints() > 0) {
            for (int i = 0; i < 5; i++) {
                int ox = ax + i * 9;
                g.fill(ox, top + 2, ox + 7, top + 7, 0xFF000000);
                g.fill(ox + 1, top + 3, ox + 6, top + 6, i < s.comboPoints() ? 0xFFFFC23A : 0xFF3A3020);
            }
        }
    }

    private static int orbColor(int i) {
        return switch (i) {
            case 0 -> 0xFF4AA8FF;
            case 1 -> 0xFFC86CFF;
            default -> 0xFFFF8A2A;
        };
    }

    /**
     * Рамки группы (как в рейде): номер для Ctrl+1…5, роль, имя, здоровье с щитом, мана.
     * Выбранный союзник (цель таргетных лечений) — в золотой рамке; дух — серый крест.
     */
    private static void party(GuiGraphics g, Font font, int y) {
        var list = ClientData.party;
        if (list.size() <= 1 && ClientData.stage == 0 && list.stream().noneMatch(m -> m.hp() < m.maxHp())) {
            if (list.isEmpty()) return;
        }
        int x = 6, w = 128, h = 25;
        g.drawString(font, "ГРУППА  (Ctrl+1…5 — выбрать)", x, y, UiKit.FRAME, false);
        y += 11;
        for (int i = 0; i < list.size(); i++) {
            var m = list.get(i);
            boolean spirit = (m.flags() & 1) != 0, fight = (m.flags() & 2) != 0;
            boolean focus = m.id().equals(ClientData.focus);
            g.fill(x - 2, y - 2, x + w + 2, y + h - 1, focus ? 0xC0302810 : 0xA0050A12);
            UiKit.frame(g, x - 2, y - 2, w + 4, h + 1, focus ? 0xFFFFD54F : fight ? 0xFF6A2A2A : 0xFF1E3A4A);
            String num = String.valueOf(i + 1);
            g.drawString(font, num, x, y, 0xFF8AA4B8, false);
            int roleColor = switch (m.role()) {
                case 1 -> 0xFF5DADE2;
                case 2 -> 0xFFE74C3C;
                case 3 -> 0xFF58D68D;
                default -> 0xFF8A8A8A;
            };
            String roleIcon = switch (m.role()) {
                case 1 -> "⛨";
                case 2 -> "⚔";
                case 3 -> "✚";
                default -> "·";
            };
            g.drawString(font, roleIcon, x + 8, y, roleColor, false);
            String name = m.name();
            if (font.width(name) > w - 44) name = font.plainSubstrByWidth(name, w - 48) + "…";
            g.drawString(font, name, x + 18, y, spirit ? 0xFF808080 : 0xFF000000 | m.color(), false);
            float frac = spirit ? 0 : m.hp() / Math.max(1f, m.maxHp());
            String pct = spirit ? "✝" : Math.round(frac * 100) + "%";
            g.drawString(font, pct, x + w - font.width(pct), y, spirit ? 0xFFB0B0B0 : frac < 0.35f ? 0xFFFF5050 : 0xFFE0E0E0, false);
            int hpColor = frac > 0.6f ? 0xFF3CB043 : frac > 0.3f ? 0xFFD8B02A : 0xFFD03A3A;
            UiKit.bar(g, x, y + 10, w, 6, frac, 0xFF201010, hpColor);
            if (m.absorb() > 0 && !spirit) {
                int aw = (int) Math.min(w, w * m.absorb() / Math.max(1f, m.maxHp()));
                g.fill(x, y + 10, x + aw, y + 12, 0xD0FFFFFF);
            }
            UiKit.bar(g, x, y + 18, w, 2, spirit ? 0 : m.mana(), 0xFF1A2340, 0xFF3D8BFF);
            y += h + 3;
        }
    }

    /** Рамка цели: имя, уровень и здоровье моба под прицелом. */
    private static void target(GuiGraphics g, Font font, Minecraft mc, int width) {
        Entity e = mc.crosshairPickEntity;
        if (!(e instanceof LivingEntity le) || e instanceof Player || !le.isAlive() || le.isInvisible()) return;
        if (e instanceof com.andrey.litrpg.entity.Animated a && a.awakeAnim(1f) < 0.5f) return;   // мимик притворяется сундуком
        Component name = le.getDisplayName();
        Aggro ag = AGGRO.get(le.getId());
        boolean aggro = ag != null && mc.level != null && mc.level.getGameTime() - ag.time < 30 && !ag.target.isEmpty();
        String who = !aggro ? "" : ag.onMe ? "⚔ Агр: на тебе" : "⚔ Агр: " + ag.target;
        String pct = !aggro ? "" : "угроза " + ag.percent + "%";
        int w = Math.max(140, font.width(name) + 16);
        if (aggro) w = Math.max(w, font.width(who) + (int) (font.width(pct) * 0.7f) + 34 + 22);
        int x = width / 2 - w / 2;
        int y = 4;
        int h = aggro ? 34 : 24;
        g.fill(x, y, x + w, y + h, 0xB0050A12);
        UiKit.frame(g, x, y, w, h, aggro && ag.onMe ? 0xFFB03030 : UiKit.FRAME_DIM);
        if (aggro) {
            // агр: кого бьёт моб и твоя угроза относительно лидера
            g.drawString(font, who, x + 6, y + 23, ag.onMe ? 0xFFFF6A5A : 0xFFB8C8D8, false);
            int pw = font.width(pct);
            int barW = 34;
            UiKit.bar(g, x + w - 6 - barW, y + 25, barW, 4, ag.percent / 100f, 0xFF20140A,
                    ag.percent >= 100 ? 0xFFE04030 : ag.percent >= 80 ? 0xFFE0A030 : 0xFF6FB04A);
            g.pose().pushPose();
            g.pose().translate(x + w - 9 - barW - pw * 0.7f, y + 24.5f, 0);
            g.pose().scale(0.7f, 0.7f, 1f);
            g.drawString(font, pct, 0, 0, 0xFFB8C8D8, false);
            g.pose().popPose();
        }
        g.drawString(font, name, x + w / 2 - font.width(name) / 2, y + 3, 0xFFFFFFFF);
        float frac = le.getHealth() / Math.max(1f, le.getMaxHealth());
        int color = frac > 0.5f ? 0xFFD03A3A : frac > 0.25f ? 0xFFE0822A : 0xFFFF2020;
        UiKit.bar(g, x + 6, y + 14, w - 12, 6, frac, 0xFF2A0A0A, color);
        String hp = (int) Math.ceil(le.getHealth()) + " / " + (int) le.getMaxHealth();
        g.pose().pushPose();
        g.pose().translate(x + w / 2f, y + 14.5f, 0);
        g.pose().scale(0.6f, 0.6f, 1f);
        g.drawString(font, hp, -font.width(hp) / 2, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
    }

    // ---------------- полоса каста ----------------

    private static int castAbility = -1, castTotal, castStart;

    /** Сервер: начат каст навыка (или -1 — окончен/сбит). */
    public static void cast(int ability, int ticks) {
        castAbility = ability;
        castTotal = ticks;
        castStart = Minecraft.getInstance().player == null ? 0 : Minecraft.getInstance().player.tickCount;
    }

    private static void castBar(GuiGraphics g, Font font, int width, int height, float pt) {
        if (castAbility < 0 || Minecraft.getInstance().player == null) return;
        Ability a = Ability.byId(castAbility);
        if (a == null) return;
        float t = (Minecraft.getInstance().player.tickCount - castStart + pt) / Math.max(1, castTotal);
        if (t > 1.2f) {
            castAbility = -1;
            return;
        }
        int w = 160, x = width / 2 - w / 2, y = height - 74;
        g.fill(x - 3, y - 12, x + w + 3, y + 9, 0xB0050A12);
        UiKit.frame(g, x - 3, y - 12, w + 6, 21, 0xFF4FC3F7);
        String name = Component.translatable(a.nameKey()).getString();
        g.drawString(font, name, x + w / 2 - font.width(name) / 2, y - 10, 0xFFFFFFFF, false);
        UiKit.bar(g, x, y + 1, w, 5, Math.min(1f, t), 0xFF101828, 0xFFFFC040);
    }

    private record Aggro(String target, boolean onMe, int percent, long time) {}

    private static final java.util.Map<Integer, Aggro> AGGRO = new java.util.HashMap<>();

    /** Сервер сообщил агр моба (см. event/Threat). */
    public static void threat(int entityId, String target, boolean onMe, int percent) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (AGGRO.size() > 64) AGGRO.entrySet().removeIf(e -> mc.level.getGameTime() - e.getValue().time > 100);
        AGGRO.put(entityId, new Aggro(target, onMe, percent, mc.level.getGameTime()));
    }

    public static void bar(GuiGraphics g, int x, int y, int w, int h, float frac, int bg, int fg) {
        UiKit.bar(g, x, y, w, h, frac, bg, fg);
    }
}
