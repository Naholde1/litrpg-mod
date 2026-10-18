package com.andrey.litrpg.stats;

import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Подбор классов на 10 уровне. Каждый класс получает оценку — насколько
 * его стили совпадают со стилем игрока. Предлагаются два лучших и один
 * из следующих пяти (для разнообразия). Открытый скрытый класс всегда
 * занимает третье место.
 */
public final class ClassOffers {
    public static final int COUNT = 3;

    private ClassOffers() {}

    public static List<PlayerClass> generate(PlayerStats s, RandomSource rnd) {
        double total = 0;
        for (int v : s.getStyles().values()) total += v;
        final double sum = Math.max(1, total);

        List<PlayerClass> pool = new ArrayList<>();
        List<PlayerClass> hidden = new ArrayList<>();
        Map<PlayerClass, Double> score = new java.util.EnumMap<>(PlayerClass.class);
        for (PlayerClass c : PlayerClass.values()) {
            double weightSum = 0, sc = 0;
            for (var e : c.styles.entrySet()) {
                weightSum += e.getValue();
                sc += e.getValue() * (s.getStyle(e.getKey()) / sum);
            }
            // небольшой шум, чтобы при равном стиле предложения отличались
            score.put(c, sc / Math.max(1, weightSum) + rnd.nextDouble() * 0.02);
            if (c.isHidden()) {
                if (c.hiddenUnlocked(s)) hidden.add(c);
            } else {
                pool.add(c);
            }
        }
        pool.sort(Comparator.comparingDouble((PlayerClass c) -> score.get(c)).reversed());
        hidden.sort(Comparator.comparingDouble((PlayerClass c) -> score.get(c)).reversed());

        List<PlayerClass> result = new ArrayList<>();
        result.add(pool.get(0));
        result.add(pool.get(1));
        if (!hidden.isEmpty()) {
            result.add(hidden.get(0));
        } else {
            int span = Math.min(5, pool.size() - 2);
            result.add(pool.get(2 + rnd.nextInt(span)));
        }
        return result;
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
