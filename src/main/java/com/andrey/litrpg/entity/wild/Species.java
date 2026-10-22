package com.andrey.litrpg.entity.wild;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Вид существа из бестиария (/litrpg_bestiary.json, его пишет tools/gen_bestiary.py):
 * уровень (базовый ±5), где и когда водится, повадки, навыки, добыча. Там же —
 * таблица расселения для {@link WildSpawner} и уровни прочих (ванильных) мобов.
 */
public final class Species {
    public enum Temper { HOSTILE, TERRITORIAL, NEUTRAL, TIMID, SCAVENGER }

    public enum Time { ANY, DAY, NIGHT }

    public enum Place { SURFACE, UNDER, BOTH }

    public enum Trait {
        PACK, FLEE_LOW, KITE, BLIND, DISGUISE, STEALTH, DRAIN, REGEN, REFLECT, FRONT_GUARD, SPLIT, EXPLODE, REVIVE,
        ENRAGE, AURA_FIRE, AURA_FROST, ENGULF, SUN_BURN, FLAMMABLE, WATER, FIRE_IMMUNE
    }

    public enum Skill {
        BOLT, VOLLEY, ARROWS, LEAP, CHARGE, BREATH_FIRE, BREATH_FROST, BREATH_POISON, ROAR, HOWL, STOMP, TONGUE,
        BURROW, SUMMON, GAZE, SCREAM, BLINK, ROOTS, SWOOP
    }

    /** Навык вида: перезарядка (тики), сила (множитель урона), дальность, аргумент (вид снаряда, моб), число. */
    public record Use(Skill skill, int cd, float power, float range, String arg, int count) {}

    public record Hit(ResourceLocation effect, int duration, int amp) {}

    public record Drop(ResourceLocation item, int min, int max, float chance) {}

    /** Строка расселения: вид мода или существующая тварь. */
    public record Spawn(ResourceLocation type, int level, Set<String> lands, Time time, Place place, int min, int max, int weight) {}

    public String id, ru, plural, desc, particle, mobType;
    public int level, zone, groupMin, groupMax, weight, fire;
    public float w, h, hp, dmg, speed, armor, kb, fly, pitch;
    public Temper temper;
    public Time time;
    public Place place;
    public Set<String> lands = new HashSet<>();
    public EnumSet<Trait> traits = EnumSet.noneOf(Trait.class);
    public List<Use> skills = new ArrayList<>();
    public List<Hit> hits = new ArrayList<>();
    public List<Drop> drops = new ArrayList<>();
    public ResourceLocation[] sounds = new ResourceLocation[3];
    public int egg1, egg2;
    /** Зверь (для бонусов «урон по зверям» и заданий охотников). */
    public boolean beast;

    public boolean has(Trait t) {
        return traits.contains(t);
    }

    /** Враг (нападает сам) или зверь (только защищается / убегает). */
    public boolean hostile() {
        return temper == Temper.HOSTILE || temper == Temper.TERRITORIAL || temper == Temper.SCAVENGER;
    }

    public boolean flying() {
        return fly > 0;
    }

    // ================= загрузка =================

    private static final Map<String, Species> BY_ID = new LinkedHashMap<>();
    public static final List<Spawn> SPAWNS = new ArrayList<>();
    private static final Map<ResourceLocation, Integer> BANDS = new HashMap<>();
    private static final Map<EntityType<?>, Species> BY_TYPE = new IdentityHashMap<>();

    static {
        load();
    }

    public static Collection<Species> all() {
        return BY_ID.values();
    }

    public static Species get(String id) {
        return BY_ID.get(id);
    }

    public static Species of(EntityType<?> type) {
        Species s = BY_TYPE.get(type);
        if (s == null) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(type);
            if (key == null || !key.getNamespace().equals(LitRPG.MODID)) return null;
            s = BY_ID.get(key.getPath());
            if (s != null) BY_TYPE.put(type, s);
        }
        return s;
    }

    /** Базовый уровень моба этого типа (или null — уровень по зоне, как раньше). */
    public static Integer band(EntityType<?> type) {
        Species s = of(type);
        if (s != null) return s.level;
        return BANDS.get(ForgeRegistries.ENTITY_TYPES.getKey(type));
    }

    private static void load() {
        try (var in = LitRPG.class.getResourceAsStream("/litrpg_bestiary.json")) {
            if (in == null) throw new IllegalStateException("no /litrpg_bestiary.json");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("species")) {
                Species s = parse(el.getAsJsonObject());
                BY_ID.put(s.id, s);
                SPAWNS.add(new Spawn(LitRPG.id(s.id), s.level, s.lands, s.time, s.place, s.groupMin, s.groupMax, s.weight));
                BANDS.put(LitRPG.id(s.id), s.level);
            }
            for (JsonElement el : root.getAsJsonArray("external")) {
                JsonObject o = el.getAsJsonObject();
                JsonArray g = o.getAsJsonArray("group");
                SPAWNS.add(new Spawn(new ResourceLocation(o.get("id").getAsString()), o.get("level").getAsInt(), strings(o.getAsJsonArray("lands")),
                        Time.valueOf(o.get("time").getAsString().toUpperCase()), Place.valueOf(o.get("place").getAsString().toUpperCase()),
                        g.get(0).getAsInt(), g.get(1).getAsInt(), o.get("weight").getAsInt()));
            }
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("bands").entrySet()) {
                BANDS.put(new ResourceLocation(e.getKey()), e.getValue().getAsInt());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load bestiary", e);
        }
        LitRPG.LOGGER.info("Bestiary: {} species, {} spawn rows", BY_ID.size(), SPAWNS.size());
    }

    private static Species parse(JsonObject o) {
        Species s = new Species();
        s.id = o.get("id").getAsString();
        s.ru = o.get("ru").getAsString();
        s.plural = o.get("plural").getAsString();
        s.desc = o.get("desc").getAsString();
        s.level = o.get("level").getAsInt();
        s.w = o.get("w").getAsFloat();
        s.h = o.get("h").getAsFloat();
        s.hp = o.get("hp").getAsFloat();
        s.dmg = o.get("dmg").getAsFloat();
        s.speed = o.get("speed").getAsFloat();
        s.armor = o.get("armor").getAsFloat();
        s.kb = o.get("kb").getAsFloat();
        s.fly = o.get("fly").getAsFloat();
        s.temper = Temper.valueOf(o.get("temper").getAsString().toUpperCase());
        s.zone = o.get("zone").getAsInt();
        s.time = Time.valueOf(o.get("time").getAsString().toUpperCase());
        s.place = Place.valueOf(o.get("place").getAsString().toUpperCase());
        JsonArray g = o.getAsJsonArray("group");
        s.groupMin = g.get(0).getAsInt();
        s.groupMax = g.get(1).getAsInt();
        s.weight = o.get("weight").getAsInt();
        s.lands = strings(o.getAsJsonArray("lands"));
        for (JsonElement t : o.getAsJsonArray("traits")) s.traits.add(Trait.valueOf(t.getAsString().toUpperCase()));
        for (JsonElement k : o.getAsJsonArray("skills")) {
            JsonObject u = k.getAsJsonObject();
            s.skills.add(new Use(Skill.valueOf(u.get("s").getAsString().toUpperCase()), u.get("cd").getAsInt(), u.get("p").getAsFloat(),
                    u.get("r").getAsFloat(), u.get("arg").getAsString(), u.get("n").getAsInt()));
        }
        for (JsonElement k : o.getAsJsonArray("hit")) {
            JsonArray a = k.getAsJsonArray();
            s.hits.add(new Hit(new ResourceLocation(a.get(0).getAsString()), a.get(1).getAsInt(), a.get(2).getAsInt()));
        }
        s.fire = o.get("fire").getAsInt();
        JsonArray snd = o.getAsJsonArray("sound");
        for (int i = 0; i < 3; i++) s.sounds[i] = snd.get(i).getAsString().isEmpty() ? null : new ResourceLocation(snd.get(i).getAsString());
        s.pitch = snd.get(3).getAsFloat();
        s.particle = o.get("particle").getAsString();
        for (JsonElement k : o.getAsJsonArray("drops")) {
            JsonArray a = k.getAsJsonArray();
            s.drops.add(new Drop(new ResourceLocation(a.get(0).getAsString()), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsFloat()));
        }
        JsonArray egg = o.getAsJsonArray("egg");
        s.egg1 = egg.get(0).getAsInt();
        s.egg2 = egg.get(1).getAsInt();
        s.mobType = o.get("mobtype").getAsString();
        s.beast = o.has("beast") && o.get("beast").getAsBoolean();
        return s;
    }

    private static Set<String> strings(JsonArray a) {
        Set<String> out = new HashSet<>();
        for (JsonElement e : a) out.add(e.getAsString());
        return out;
    }
}
