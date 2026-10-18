package com.andrey.litrpg.gear;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.Stat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Шаблоны предметов экипировки из файла /litrpg_items.json (его создаёт
 * tools/gen_gear_content.py). По шаблонам регистрируются предметы: украшения,
 * броня наборов, оружие. У шаблона есть «врождённые» свойства, которые есть
 * у предмета любой редкости, и принадлежность к набору.
 */
public final class ItemTemplates {
    /** Шаблон предмета. */
    public static final class Def {
        public String id;
        public String kind;
        public int tier;
        public Rarity minRarity = Rarity.COMMON;
        public String set;
        public String material;
        public float damage = 3;
        public float speed = -2.4f;
        public final Map<GearBonus, Float> implicitBonus = new EnumMap<>(GearBonus.class);
        public final int[] implicitStats = new int[Stat.values().length];
        public String passive;

        public boolean isJewel() {
            return switch (kind) {
                case "ring", "necklace", "belt", "bracelet", "charm", "back", "hands" -> true;
                default -> false;
            };
        }

        public boolean isArmor() {
            return switch (kind) {
                case "helmet", "chestplate", "leggings", "boots" -> true;
                default -> false;
            };
        }
    }

    /** Порог набора: сколько предметов нужно и что даёт. */
    public static final class SetTier {
        public int pieces;
        public final Map<GearBonus, Float> bonus = new EnumMap<>(GearBonus.class);
        public final int[] stats = new int[Stat.values().length];
        public Passive passive;
    }

    public static final class SetDef {
        public String id;
        public String title;
        public final List<String> items = new ArrayList<>();
        public final List<SetTier> tiers = new ArrayList<>();
    }

    public static final class Material {
        public String id;
        public int durability = 15;
        public int[] defense = {1, 3, 2, 1};
        public int enchant = 15;
        public float toughness = 0;
        public float knockback = 0;
        public String repair = "minecraft:leather";
        public String sound = "leather";
    }

    public static final List<Def> ITEMS = new ArrayList<>();
    public static final Map<String, Def> BY_ID = new LinkedHashMap<>();
    public static final Map<String, SetDef> SETS = new LinkedHashMap<>();
    public static final Map<String, Material> MATERIALS = new LinkedHashMap<>();
    /** Шаблоны украшений по уровню — для выпадения «мелочёвки». */
    public static final TreeMap<Integer, List<Def>> JEWELS_BY_TIER = new TreeMap<>();

    static {
        load();
    }

    private ItemTemplates() {}

    private static void load() {
        try (var in = LitRPG.class.getResourceAsStream("/litrpg_items.json")) {
            if (in == null) {
                LitRPG.LOGGER.error("litrpg_items.json not found");
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("materials")) {
                JsonObject o = el.getAsJsonObject();
                Material m = new Material();
                m.id = o.get("id").getAsString();
                m.durability = o.get("durability").getAsInt();
                var def = o.getAsJsonArray("defense");
                m.defense = new int[]{def.get(0).getAsInt(), def.get(1).getAsInt(), def.get(2).getAsInt(), def.get(3).getAsInt()};
                m.enchant = o.get("enchant").getAsInt();
                m.toughness = o.get("toughness").getAsFloat();
                m.knockback = o.get("knockback").getAsFloat();
                m.repair = o.get("repair").getAsString();
                m.sound = o.get("sound").getAsString();
                MATERIALS.put(m.id, m);
            }
            for (JsonElement el : root.getAsJsonArray("items")) {
                JsonObject o = el.getAsJsonObject();
                Def d = new Def();
                d.id = o.get("id").getAsString();
                d.kind = o.get("kind").getAsString();
                d.tier = o.get("tier").getAsInt();
                if (o.has("min")) d.minRarity = rarity(o.get("min").getAsString());
                if (o.has("set")) d.set = o.get("set").getAsString();
                if (o.has("material")) d.material = o.get("material").getAsString();
                if (o.has("damage")) d.damage = o.get("damage").getAsFloat();
                if (o.has("speed")) d.speed = o.get("speed").getAsFloat();
                if (o.has("passive")) d.passive = o.get("passive").getAsString();
                if (o.has("implicit")) readMods(o.getAsJsonObject("implicit"), d.implicitBonus, d.implicitStats);
                ITEMS.add(d);
                BY_ID.put(d.id, d);
                if (d.isJewel()) JEWELS_BY_TIER.computeIfAbsent(d.tier, k -> new ArrayList<>()).add(d);
            }
            for (JsonElement el : root.getAsJsonArray("sets")) {
                JsonObject o = el.getAsJsonObject();
                SetDef s = new SetDef();
                s.id = o.get("id").getAsString();
                s.title = o.get("title").getAsString();
                for (JsonElement it : o.getAsJsonArray("items")) s.items.add(it.getAsString());
                for (JsonElement t : o.getAsJsonArray("tiers")) {
                    JsonObject to = t.getAsJsonObject();
                    SetTier st = new SetTier();
                    st.pieces = to.get("pieces").getAsInt();
                    readMods(to.getAsJsonObject("mods"), st.bonus, st.stats);
                    if (to.has("passive")) st.passive = Passive.byKey(to.get("passive").getAsString());
                    s.tiers.add(st);
                }
                SETS.put(s.id, s);
            }
            LitRPG.LOGGER.info("Loaded {} item templates, {} sets", ITEMS.size(), SETS.size());
        } catch (Exception e) {
            LitRPG.LOGGER.error("Failed to load item templates", e);
        }
    }

    private static void readMods(JsonObject o, Map<GearBonus, Float> bonus, int[] stats) {
        for (var e : o.entrySet()) {
            Stat st = stat(e.getKey());
            if (st != null) {
                stats[st.ordinal()] += e.getValue().getAsInt();
                continue;
            }
            GearBonus b = GearBonus.byKey(e.getKey());
            if (b != null) bonus.merge(b, e.getValue().getAsFloat(), Float::sum);
            else LitRPG.LOGGER.warn("Unknown gear modifier {}", e.getKey());
        }
    }

    private static Stat stat(String key) {
        for (Stat s : Stat.values()) if (s.key.equals(key)) return s;
        return null;
    }

    private static Rarity rarity(String key) {
        for (Rarity r : Rarity.values()) if (r.key.equals(key)) return r;
        return Rarity.COMMON;
    }

    /** Случайный шаблон украшения уровня около level. */
    public static Def randomJewel(net.minecraft.util.RandomSource rnd, int level) {
        List<Def> pool = new ArrayList<>();
        for (var e : JEWELS_BY_TIER.headMap(level + 5, true).entrySet()) {
            // ближние к уровню — чаще
            int weight = Math.max(1, 6 - Math.abs(level - e.getKey()) / 4);
            for (int i = 0; i < weight; i++) pool.addAll(e.getValue());
        }
        if (pool.isEmpty()) pool.addAll(JEWELS_BY_TIER.isEmpty() ? Collections.emptyList() : JEWELS_BY_TIER.firstEntry().getValue());
        return pool.isEmpty() ? null : pool.get(rnd.nextInt(pool.size()));
    }
}
