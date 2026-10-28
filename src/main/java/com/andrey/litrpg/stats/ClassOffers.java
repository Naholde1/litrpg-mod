package com.andrey.litrpg.stats;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Подбор классов на 10 уровне. Система оценивает каждый класс выбранной роли по трём признакам:
 * <ul>
 *   <li>стиль игры в первом этапе подземелья (ближний бой, стрельба, магия, лечение, защита, скрытность…) — 60%;</li>
 *   <li>во что вложены очки характеристик — 25%;</li>
 *   <li>совпал ли главный стиль игрока с главным стилем класса — 15%.</li>
 * </ul>
 * Совпадение стиля считается косинусом, а он редко опускается ниже половины: без возведения
 * в степень почти все классы роли набирали близкие оценки, и двое игроков получали одинаковые
 * предложения. Теперь разница между «почти подходит» и «подходит» заметна.
 * <p>
 * Класс уникален на группу: занятый товарищем — и даже просто предложенный ему, пока он не выбрал, —
 * другому не предлагается. Предлагается три класса: два лучших и один из следующих четырёх.
 */
public final class ClassOffers {
    public static final int COUNT = 3;

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

    /** Главный стиль класса совпал с главным стилем игрока (0, 0.45 или 1). */
    public static double signature(PlayerStats s, PlayerClass c) {
        Style top = null;
        int best = 0;
        for (Map.Entry<Style, Integer> e : c.styles.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
            }
        }
        if (top == null) return 0;
        List<Map.Entry<Style, Integer>> mine = topStyles(s, 3);
        if (mine.isEmpty()) return 0;
        if (mine.get(0).getKey() == top) return 1;
        for (int i = 1; i < mine.size(); i++) {
            if (mine.get(i).getKey() == top) return 0.45;
        }
        return 0;
    }

    public static double score(PlayerStats s, PlayerClass c) {
        // косинус стиля редко бывает ниже 0,5 — степень растягивает разницу
        double sharp = Math.pow(styleScore(s, c), 2.2);
        return sharp * 0.6 + statScore(s, c) * 0.25 + signature(s, c) * 0.15;
    }

    public static List<PlayerClass> generate(PlayerStats s, RandomSource rnd) {
        return generate(s, rnd, java.util.Set.of());
    }

    /**
     * Три класса на выбор. {@code busy} — ключи классов, которые заняты товарищами или
     * уже предложены им: один класс на группу, кто выбрал первым, тот его и занял.
     */
    public static List<PlayerClass> generate(PlayerStats s, RandomSource rnd, java.util.Set<String> busy) {
        Role role = s.getRole() != null ? s.getRole() : Role.DAMAGE;
        List<PlayerClass> pool = new ArrayList<>();
        List<PlayerClass> hidden = new ArrayList<>();
        Map<PlayerClass, Double> score = new HashMap<>();
        for (PlayerClass c : PlayerClass.all()) {
            // небольшой шум, чтобы при равном стиле предложения отличались
            score.put(c, score(s, c) + rnd.nextDouble() * 0.015);
            if (busy.contains(c.key)) continue;
            if (c.isHidden()) {
                if (c.hiddenUnlocked(s)) hidden.add(c);
            } else if (c.role == role) {
                pool.add(c);
            }
        }
        // если роль выбрана вся (маленьких ролей на большую группу не хватило) — не оставлять ни с чем
        if (pool.isEmpty() && hidden.isEmpty()) {
            for (PlayerClass c : PlayerClass.all()) {
                if (!c.isHidden() && c.role == role) pool.add(c);
            }
        }
        Comparator<PlayerClass> best = Comparator.comparingDouble((PlayerClass c) -> score.get(c)).reversed();
        pool.sort(best);
        hidden.sort(best);

        List<PlayerClass> result = new ArrayList<>(pool.subList(0, Math.min(2, pool.size())));
        List<PlayerClass> rest = new ArrayList<>(pool.subList(result.size(), Math.min(result.size() + 4, pool.size())));
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
