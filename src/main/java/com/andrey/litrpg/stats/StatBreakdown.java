package com.andrey.litrpg.stats;

import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.PlayerGear;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Единый расчёт характеристик игрока с разбивкой по источникам.
 * Им пользуются и бой/атрибуты (итог), и окно персонажа (откуда что взялось),
 * поэтому цифры в окне всегда совпадают с реальными.
 */
public final class StatBreakdown {
    private StatBreakdown() {}

    /** Одно слагаемое: источник и вклад. */
    public record Part(Component label, double value) {}

    /** Значение с разбивкой. */
    public static final class Value {
        public final String name;
        /** Формат: "%.0f", "%.1f%%" и т. п. */
        public final String format;
        public final List<Part> parts = new ArrayList<>();
        public double total;
        public double cap = Double.NaN;
        public double floor = Double.NaN;
        public String note = "";

        Value(String name, String format) {
            this.name = name;
            this.format = format;
        }

        Value add(Component label, double v) {
            if (Math.abs(v) > 1e-6) {
                parts.add(new Part(label, v));
                total += v;
            }
            return this;
        }

        Value add(String label, double v) {
            return add(Component.literal(label), v);
        }

        Value cap(double c) {
            cap = c;
            return this;
        }

        /** Итог с учётом предела. */
        public double value() {
            double v = total;
            if (!Double.isNaN(cap)) v = Math.min(v, cap);
            if (!Double.isNaN(floor)) v = Math.max(v, floor);
            return v;
        }

        public boolean capped() {
            return !Double.isNaN(cap) && total > cap;
        }

        public String text() {
            return String.format(format, value());
        }
    }

    public static final String[] STAT_NAMES = {"Сила", "Ловкость", "Выносливость", "Интеллект", "Удача"};

    private static Component cls(PlayerStats s) {
        return Component.literal("Класс: ").append(Component.translatable(s.getPlayerClass().nameKey()));
    }

    private static int styleWeight(PlayerStats s, Style st) {
        return s.getPlayerClass() == null ? 0 : s.getPlayerClass().styles.getOrDefault(st, 0);
    }

    // ================= основные характеристики =================

    public static Value stat(PlayerStats s, Stat st) {
        Value v = new Value(STAT_NAMES[st.ordinal()], "%.0f");
        v.add("Вложенные очки", s.getSpent(st));
        if (s.getPlayerClass() != null) v.add(Component.literal("Рост класса (").append(Component.translatable(s.getPlayerClass().nameKey()))
                .append(", ур. " + s.getLevel() + ")"), s.getPlayerClass().growth(st, s.getLevel()));
        v.add("Экипировка и наборы", s.gear().stats[st.ordinal()]);
        v.floor = 0;
        return v;
    }

    // ================= производные =================

    /** Здоровье сверх базовых 20. */
    public static Value health(PlayerStats s) {
        Value v = new Value("Здоровье", "%.0f");
        int lvl = s.getLevel();
        v.add("База", 20);
        v.add("Выносливость ×2", s.getStat(Stat.VITALITY) * 2.0);
        // роль танка: выносливость для него чуть ценнее (×2,5 вместо ×2)
        if (s.getRole() == Role.TANK) v.add("Выносливость (танк) ещё ×0.5", s.getStat(Stat.VITALITY) * 0.5);
        v.add("Уровень (+1 за уровень)", lvl - 1);
        int tank = tankWeight(s);
        if (tank > 0) v.add(cls(s), tank * (1 + lvl * 0.15));
        v.add("Экипировка", s.gear().get(GearBonus.HEALTH));
        return v;
    }

    /** Пассивная стойкость: весь входящий урон слабее на столько процентов. */
    public static Value resistance(PlayerStats s) {
        Value v = new Value("Стойкость к урону", "%.0f%%");
        if (s.getRole() == Role.TANK) v.add("Танк (пассивно)", 8);
        v.add("Выносливость ×0.1% (до 6%)", Math.min(s.getStat(Stat.VITALITY) * 0.1, 6.0));
        v.note = "Считается после брони. У хранителей танк и так получает вдвое меньше.";
        return v.cap(25);
    }

    /** Прибавка к урону в ближнем бою, % (от Силы). */
    public static Value meleeDamage(PlayerStats s) {
        Value v = new Value("Урон в ближнем бою", "+%.0f%%");
        v.add("Сила ×0.5%", s.getStat(Stat.STRENGTH) * 0.5);
        int melee = styleWeight(s, Style.MELEE);
        if (melee > 0) v.add(cls(s), melee * 2.0);
        v.note = "Сверх прибавки к урону самого оружия.";
        return v.cap(150);
    }

    /** Прибавка к урону стрелой, % (от Ловкости). */
    public static Value rangedDamage(PlayerStats s) {
        Value v = new Value("Урон стрелой", "+%.0f%%");
        v.add("Ловкость ×0.4%", s.getStat(Stat.AGILITY) * 0.4);
        int rg = styleWeight(s, Style.RANGED);
        if (rg > 0) v.add(cls(s), rg * 7.0);
        return v.cap(150);
    }

    public static int tankWeight(PlayerStats s) {
        return s.getPlayerClass() == null ? 0 : styleWeight(s, Style.TANK) + styleWeight(s, Style.MELEE) / 2;
    }

    /** Прибавка к урону оружием. */
    public static Value weaponDamage(PlayerStats s) {
        Value v = new Value("Урон оружием (прибавка)", "+%.1f");
        v.add("Сила ×0.4", s.getStat(Stat.STRENGTH) * 0.4);
        v.add("Экипировка", s.gear().get(GearBonus.DAMAGE));
        v.note = "К урону самого оружия. Удар кулаком — 1 ед. базы.";
        return v;
    }

    public static Value armor(PlayerStats s) {
        Value v = new Value("Броня (прибавка)", "+%.1f");
        v.add("Выносливость ×0.2 (до 12)", Math.min(s.getStat(Stat.VITALITY) * 0.2, 12.0));
        int tank = tankWeight(s);
        if (tank > 0) v.add(cls(s), tank * 0.8);
        v.add("Свойства экипировки", s.gear().get(GearBonus.ARMOR));
        v.note = "Прибавляется к броне надетых доспехов.";
        return v;
    }

    /** Скорость бега, % сверх обычной. */
    public static Value moveSpeed(PlayerStats s) {
        Value v = new Value("Скорость бега", "+%.0f%%");
        v.add("Ловкость ×0.6% (до 50%)", Math.min(s.getStat(Stat.AGILITY) * 0.6, 50));
        v.add("Экипировка", s.gear().get(GearBonus.MOVE_SPEED));
        return v;
    }

    public static Value attackSpeed(PlayerStats s) {
        Value v = new Value("Скорость атаки", "+%.0f%%");
        v.add("Ловкость ×1.2% (до 80%)", Math.min(s.getStat(Stat.AGILITY) * 1.2, 80));
        v.add("Экипировка", s.gear().get(GearBonus.ATTACK_SPEED));
        return v;
    }

    public static Value critChance(PlayerStats s) {
        Value v = new Value("Шанс крита", "%.1f%%");
        v.add("База", 5);
        v.add("Удача ×0.6%", s.getStat(Stat.LUCK) * 0.6);
        v.add("Ловкость ×0.2%", s.getStat(Stat.AGILITY) * 0.2);
        int st = styleWeight(s, Style.STEALTH), rg = styleWeight(s, Style.RANGED);
        if (st + rg > 0) v.add(cls(s), st * 3 + rg * 1.5);
        v.add("Экипировка", s.gear().get(GearBonus.CRIT_CHANCE));
        return v.cap(60);
    }

    public static Value critMultiplier(PlayerStats s) {
        Value v = new Value("Сила крита", "x%.2f");
        v.add("База", 1.5);
        v.add("Удача ×0.01", s.getStat(Stat.LUCK) * 0.01);
        v.add("Экипировка", s.gear().get(GearBonus.CRIT_DAMAGE) / 100.0);
        return v.cap(3.5);
    }

    public static Value dodge(PlayerStats s) {
        Value v = new Value("Уклонение", "%.1f%%");
        v.add("Ловкость ×0.3%", s.getStat(Stat.AGILITY) * 0.3);
        v.add("Удача ×0.2%", s.getStat(Stat.LUCK) * 0.2);
        int st = styleWeight(s, Style.STEALTH);
        if (st > 0) v.add(cls(s), st * 1.5);
        v.add("Экипировка", s.gear().get(GearBonus.DODGE));
        return v.cap(40);
    }

    public static Value maxMana(PlayerStats s) {
        Value v = new Value("Мана", "%.0f");
        v.add("База", 40);
        v.add("Интеллект ×8", s.getStat(Stat.INTELLIGENCE) * 8);
        v.add("Уровень (+3 за уровень)", (s.getLevel() - 1) * 3);
        if (s.getPlayerClass() != null) {
            int c = (s.getPlayerClass().primary == Stat.INTELLIGENCE ? 30 : 0) + ("mage".equals(s.getPlayerClass().key) ? 20 : 0);
            v.add(cls(s), c);
        }
        v.add("Экипировка", Math.round(s.gear().get(GearBonus.MANA)));
        return v;
    }

    public static Value manaRegen(PlayerStats s) {
        Value v = new Value("Восстановление маны", "%.2f/с");
        double base = 0.8 + s.getStat(Stat.INTELLIGENCE) * 0.12 + s.getLevel() * 0.04;
        v.add("База", 0.8);
        v.add("Интеллект ×0.12", s.getStat(Stat.INTELLIGENCE) * 0.12);
        v.add("Уровень ×0.04", s.getLevel() * 0.04);
        double mult = 1;
        v.add("Экипировка (+" + Math.round(s.gear().get(GearBonus.MANA_REGEN)) + "%)",
                base * mult * s.gear().get(GearBonus.MANA_REGEN) / 100.0);
        return v;
    }

    public static Value xpBonus(PlayerStats s) {
        Value v = new Value("Бонус опыта", "+%.0f%%");
        v.add("Экипировка", s.gear().get(GearBonus.XP));
        return v;
    }

    public static Value lootBonus(PlayerStats s) {
        Value v = new Value("Шанс добычи", "+%.0f%%");
        v.add("Удача ×1%", s.getStat(Stat.LUCK));
        v.add("Экипировка", s.gear().get(GearBonus.LOOT));
        if (s.gear().has(com.andrey.litrpg.gear.Passive.TREASURE_HUNTER)) v.note = "«Кладоискатель»: шанс ещё ×2.";
        return v;
    }

    public static Value spellPower(PlayerStats s) {
        Value v = new Value("Сила навыков", "+%.0f%%");
        v.add("Экипировка", s.gear().get(GearBonus.SPELL_POWER));
        v.note = "Основа силы навыка: 4 + характеристика навыка × 0.6–0.8 + уровень × 0.5.";
        return v;
    }

    /** Все производные значения для окна. */
    public static List<Value> derived(PlayerStats s) {
        return List.of(health(s), weaponDamage(s), meleeDamage(s), rangedDamage(s), armor(s), resistance(s),
                critChance(s), critMultiplier(s), dodge(s), moveSpeed(s), attackSpeed(s), maxMana(s), manaRegen(s),
                spellPower(s), xpBonus(s), lootBonus(s));
    }

    /**
     * Что характеристика даёт прямо сейчас — подсказка в окне прокачки.
     * Строки собираются из тех же расчётов, что и бой, поэтому «Сила» в окне
     * всегда показывает ровно то, что действительно идёт в урон.
     */
    public static Component effect(PlayerStats s, Stat st) {
        String prefix = STAT_NAMES[st.ordinal()];
        MutableComponent out = Component.translatable(st.descKey()).copy().withStyle(ChatFormatting.GRAY);
        boolean any = false;
        for (Value v : derived(s)) {
            for (Part p : v.parts) {
                if (!p.label().getString().startsWith(prefix)) continue;
                if (!any) {
                    out.append(Component.literal("\nСейчас даёт:").withStyle(ChatFormatting.WHITE));
                    any = true;
                }
                out.append(Component.literal("\n • " + v.name + ": " + String.format(v.format, p.value()))
                        .withStyle(ChatFormatting.AQUA));
            }
        }
        if (!any) out.append(Component.literal("\nПока ничего не даёт").withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    /** Все бонусы экипировки, которые не вошли в основные строки. */
    public static List<Part> otherGear(PlayerGear g) {
        List<Part> out = new ArrayList<>();
        for (GearBonus b : GearBonus.values()) {
            switch (b) {
                case HEALTH, ARMOR, DAMAGE, ATTACK_SPEED, MOVE_SPEED, CRIT_CHANCE, CRIT_DAMAGE, DODGE, MANA, MANA_REGEN,
                        SPELL_POWER, XP, LOOT -> { continue; }
                default -> { }
            }
            float val = g.get(b);
            if (val != 0) out.add(new Part(Component.literal((b.percent ? "% " : "") + b.text), val));
        }
        return out;
    }
}
