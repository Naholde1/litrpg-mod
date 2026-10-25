package com.andrey.litrpg.stats;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Подбор классов на 10 уровне. Система оценивает каждый класс выбранной роли сразу по нескольким признакам:
 * <ul>
 *   <li>стиль игры в первом этапе подземелья (ближний бой, стрельба, магия, лечение, защита, скрытность…) — 70%;</li>
 *   <li>во что вложены очки характеристик — 30%.</li>
 * </ul>
 * Предлагаются пять классов: три лучших, один из следующих шести (для разнообразия) и пятый —
 * открытый скрытый класс, а если его нет — ещё один из близких.
 */
public final class ClassOffers {
    public static final int COUNT = 5;

    private ClassOffers() {}

    private static double styleTotal(PlayerStats s) {
        double total = 0;
        for (int v : s.getStyles().values()) total += v;
        return Math.max(1, total);
    }

    /** Совпадение стиля игры (0..1): косинус между стилем игрока и стилем класса. */
    public static double styleScore(PlayerStats s, PlayerClass c) {
        double dot = 0, cw = 0, pw = 0;
        for (Style st : Style.values()) {
            double p = s.getStyle(st);
            double w = c.styles.getOrDefault(st, 0);
            dot += p * w;
            cw += w * w;
            pw += p * p;
        }
        if (cw <= 0 || pw <= 0) return 0;
        return dot / Math.sqrt(cw * pw);
    }

    /** Совпадение вложенных очков с ростом класса (0..1). */
    public static double statScore(PlayerStats s, PlayerClass c) {
        int total = 0;
        for (Stat st : Stat.values()) total += s.getSpent(st);
        if (total < 3) return 0.3; // очки почти не вложены — не судим
        double prim = s.getSpent(c.primary) / (double) total;
        double sec = s.getSpent(c.secondary) / (double) total;
        return Math.min(1, prim * 1.4 + sec * 0.7);
    }

    public static double score(PlayerStats s, PlayerClass c) {
        return styleScore(s, c) * 0.7 + statScore(s, c) * 0.3;
    }

    public static List<PlayerClass> generate(PlayerStats s, RandomSource rnd) {
        Role role = s.getRole() != null ? s.getRole() : Role.DAMAGE;
        List<PlayerClass> pool = new ArrayList<>();
        List<PlayerClass> hidden = new ArrayList<>();
        Map<PlayerClass, Double> score = new HashMap<>();
        for (PlayerClass c : PlayerClass.all()) {
            // небольшой шум, чтобы при равном стиле предложения отличались
            score.put(c, score(s, c) + rnd.nextDouble() * 0.03);
            if (c.isHidden()) {
                if (c.hiddenUnlocked(s)) hidden.add(c);
            } else if (c.role == role) {
                pool.add(c);
            }
        }
        Comparator<PlayerClass> best = Comparator.comparingDouble((PlayerClass c) -> score.get(c)).reversed();
        pool.sort(best);
        hidden.sort(best);

        List<PlayerClass> result = new ArrayList<>(pool.subList(0, Math.min(3, pool.size())));
        List<PlayerClass> rest = new ArrayList<>(pool.subList(result.size(), Math.min(result.size() + 6, pool.size())));
        if (!rest.isEmpty()) result.add(rest.remove(rnd.nextInt(rest.size())));
        if (!hidden.isEmpty()) result.add(hidden.get(0));
        else if (!rest.isEmpty()) result.add(rest.get(rnd.nextInt(rest.size())));
        return result;
    }

    /** Почему Система предложила класс — 2–3 коротких причины для окна выбора. */
    public static List<String> reasons(PlayerStats s, PlayerClass c) {
        List<String> out = new ArrayList<>();
        if (c.isHidden()) out.add("★ выполнено скрытое условие");
        double sum = styleTotal(s);
        c.styles.entrySet().stream()
                .filter(e -> s.getStyle(e.getKey()) / sum >= 0.08)
                .sorted(Comparator.comparingDouble((Map.Entry<Style, Integer> e) -> e.getValue() * s.getStyle(e.getKey())).reversed())
                .limit(2)
                .forEach(e -> out.add("стиль: " + net.minecraft.network.chat.Component.translatable(e.getKey().nameKey()).getString()
                        + " " + Math.round(100 * s.getStyle(e.getKey()) / sum) + "%"));
        if (statScore(s, c) >= 0.5) {
            out.add("очки в " + net.minecraft.network.chat.Component.translatable(c.primary.translationKey()).getString());
        }
        if (out.isEmpty()) out.add("Система видит в тебе задатки");
        return out;
    }

    /** Доли стилей игрока в процентах, по убыванию. */
    public static List<Map.Entry<Style, Integer>> topStyles(PlayerStats s, int limit) {
        double total = 0;
        for (int v : s.getStyles().values()) total += v;
        List<Map.Entry<Style, Integer>> list = new ArrayList<>();
        for (Style st : Style.values()) {
            int pct = total <= 0 ? 0 : (int) Math.round(100.0 * s.getStyle(st) / total);
            if (pct > 0) list.add(Map.entry(st, pct));
        }
        list.sort(Map.Entry.<Style, Integer>comparingByValue().reversed());
        return list.size() > limit ? list.subList(0, limit) : list;
    }
}
