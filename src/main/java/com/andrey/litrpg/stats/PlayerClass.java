package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.ClassData;
import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.Passive;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Класс персонажа. Все классы описаны данными (tools/gen_classes.py → litrpg_classes.json):
 * роль, рост характеристик, стиль игры, восемь навыков с уровнями открытия, черта класса,
 * а у скрытых — условия, при которых Система их предложит. У «заклинателей сфер» вместо
 * списка навыков — три сферы, Призыв и таблица сочетаний (как у Инвокера).
 */
public final class PlayerClass {
    /** Сочетание сфер (по номерам сфер, отсортировано: «001») → навык. */
    public record Combo(String pattern, String skill, int level) {}

    public record Invoke(String[] spheres, String cast, List<Combo> combos) {
        public Combo find(String pattern) {
            for (Combo c : combos) if (c.pattern.equals(pattern)) return c;
            return null;
        }
    }

    private static final List<PlayerClass> ALL = new ArrayList<>();
    private static final Map<String, PlayerClass> BY_KEY = new HashMap<>();

    public final String key;
    public final int color;
    public final Role role;
    /** Растёт на 1 каждый уровень. */
    public final Stat primary;
    /** Растёт на 1 каждые 2 уровня. */
    public final Stat secondary;
    public final Map<Style, Integer> styles;
    public final String[] skills;
    public final int[] unlock;
    public final Map<GearBonus, Float> traitBonus;
    public final Set<Passive> traitPassives;
    /** Условия скрытого класса ('counter:undead>=60'); пусто — обычный класс. */
    public final List<String> hidden;
    public final Invoke invoke;
    /** Чем класс платит за способности. */
    public final Resource resource;
    private final int index;

    private PlayerClass(JsonObject o, int index) {
        this.index = index;
        key = o.get("key").getAsString();
        color = o.get("color").getAsInt();
        role = Role.byKey(o.get("role").getAsString());
        primary = Stat.valueOf(o.get("primary").getAsString());
        secondary = Stat.valueOf(o.get("secondary").getAsString());
        resource = o.has("resource") ? Resource.byKey(o.get("resource").getAsString()) : Resource.MANA;
        Map<Style, Integer> st = new EnumMap<>(Style.class);
        for (var e : o.getAsJsonObject("styles").entrySet()) {
            Style s = Style.byKey(e.getKey());
            if (s != null) st.put(s, e.getValue().getAsInt());
        }
        styles = Collections.unmodifiableMap(st);
        skills = strings(o.getAsJsonArray("skills"));
        JsonArray ul = o.getAsJsonArray("unlock");
        unlock = new int[ul.size()];
        for (int i = 0; i < unlock.length; i++) unlock[i] = ul.get(i).getAsInt();
        Map<GearBonus, Float> tb = new LinkedHashMap<>();
        Set<Passive> tp = EnumSet.noneOf(Passive.class);
        JsonObject trait = o.getAsJsonObject("trait");
        for (var e : trait.getAsJsonObject("bonus").entrySet()) {
            GearBonus b = GearBonus.byKey(e.getKey());
            if (b != null) tb.put(b, e.getValue().getAsFloat());
        }
        for (JsonElement e : trait.getAsJsonArray("passives")) {
            Passive p = Passive.byKey(e.getAsString());
            if (p != null) tp.add(p);
        }
        traitBonus = Collections.unmodifiableMap(tb);
        traitPassives = Collections.unmodifiableSet(tp);
        hidden = o.has("hidden") ? List.of(strings(o.getAsJsonArray("hidden"))) : List.of();
        if (o.has("invoke")) {
            JsonObject iv = o.getAsJsonObject("invoke");
            List<Combo> combos = new ArrayList<>();
            for (JsonElement e : iv.getAsJsonArray("combos")) {
                JsonArray a = e.getAsJsonArray();
                combos.add(new Combo(a.get(0).getAsString(), a.get(1).getAsString(), a.get(2).getAsInt()));
            }
            invoke = new Invoke(strings(iv.getAsJsonArray("spheres")), iv.get("cast").getAsString(), List.copyOf(combos));
        } else {
            invoke = null;
        }
    }

    private static String[] strings(JsonArray a) {
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsString();
        return out;
    }

    static {
        int i = 0;
        for (JsonElement e : ClassData.root().getAsJsonArray("classes")) {
            PlayerClass c = new PlayerClass(e.getAsJsonObject(), i++);
            ALL.add(c);
            BY_KEY.put(c.key, c);
        }
        // удалённые при переработке классы: у игроков, что их выбрали, — замена (tools/classes/pve.py)
        var root = ClassData.root();
        if (root.has("aliases")) {
            for (var en : root.getAsJsonObject("aliases").entrySet()) {
                PlayerClass to = BY_KEY.get(en.getValue().getAsString());
                if (to != null) BY_KEY.putIfAbsent(en.getKey(), to);
            }
        }
    }

    public int ordinal() { return index; }

    public static PlayerClass[] values() { return ALL.toArray(new PlayerClass[0]); }

    public static List<PlayerClass> all() { return Collections.unmodifiableList(ALL); }

    public static PlayerClass byKey(String key) { return key == null ? null : BY_KEY.get(key); }

    /** Сколько очков характеристики класс дал к этому уровню. */
    public int growth(Stat s, int level) {
        int v = 0;
        if (s == primary) v += level - 1;
        if (s == secondary) v += (level - 1) / 2;
        return v;
    }

    public boolean isHidden() { return !hidden.isEmpty(); }

    public boolean isInvoker() { return invoke != null; }

    /** Выполнены ли все условия скрытого класса. */
    public boolean hiddenUnlocked(PlayerStats s) {
        for (String cond : hidden) if (!check(s, cond)) return false;
        return true;
    }

    /** Условие вида 'style:night>=500', 'level>=20'. */
    private static boolean check(PlayerStats s, String cond) {
        String[] kv = cond.split(":", 2);
        String type = kv[0];
        String rest = kv.length > 1 ? kv[1] : "";
        int ge = rest.indexOf(">=");
        String name = ge >= 0 ? rest.substring(0, ge) : rest;
        int need = ge >= 0 ? Integer.parseInt(rest.substring(ge + 2)) : 0;
        if (type.startsWith("level>=")) return s.getLevel() >= Integer.parseInt(type.substring(7));
        if (type.equals("style")) {
            Style st = Style.byKey(name);
            return st != null && s.getStyle(st) >= need;
        }
        return false;
    }

    /** Уровень открытия навыка этого класса (999 — навыка в классе нет). */
    public int unlockOf(String skill) {
        for (int i = 0; i < skills.length; i++) if (skills[i].equals(skill)) return unlock[i];
        if (invoke != null) {
            for (Combo c : invoke.combos) if (c.skill.equals(skill)) return c.level;
        }
        return 999;
    }

    public String nameKey() { return "class.litrpg." + key; }
    public String descKey() { return "class.litrpg." + key + ".desc"; }
    public String hintKey() { return "class.litrpg." + key + ".hint"; }
    public String traitKey() { return "class.litrpg." + key + ".trait"; }
    public String traitDescKey() { return "class.litrpg." + key + ".trait.desc"; }
}
