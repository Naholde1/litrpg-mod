package com.andrey.litrpg.ability;

import com.andrey.litrpg.entity.ManaBoltEntity.Element;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.Stat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Навык. Все навыки описаны данными (tools/gen_classes.py → litrpg_classes.json: вид + параметры),
 * а исполняет их {@link AbilityHandler}. Классы ссылаются на навыки по ключу.
 */
public final class Ability {
    /** Что делает навык. */
    public enum Kind {
        DASH,           // рывок без урона
        DASH_STRIKE,    // рывок с уроном по пути
        LEAP_SLAM,      // прыжок и удар при приземлении
        NOVA,           // волна вокруг себя
        MULTI_NOVA,     // серия волн (вихрь, танец клинков)
        STRIKE,         // удар по цели под прицелом
        MULTI_STRIKE,   // серия ударов по одной цели
        BOLT,           // магический снаряд
        CHAIN,          // цепная молния
        LIGHTNING,      // удар молнии по цели
        METEOR,         // падение с неба в точку прицела
        ZONE,           // зона вокруг себя на время
        ZONE_AT,        // зона в точке прицела
        ARROWS,         // стрелы веером
        BARRAGE,        // очередь стрел
        ARROW_RAIN,     // дождь стрел
        BUFF_SELF,
        BUFF_ALLIES,
        HEAL,           // лечение себя и союзников
        HEAL_SELF,      // процент от максимума здоровья
        MEDITATE,       // мана и здоровье
        TAUNT,          // агрессия врагов + баффы/дебаффы
        PULL,           // притянуть цель
        BLINK,
        SHADOW_STEP,
        STEALTH,
        ON_HIT,         // зарядить оружие эффектом на N ударов
        MARK,           // метка: цель получает больше урона
        PURIFY,
        SUMMON,
        BEAM,           // луч: всё на линии
        CONE,           // конус перед собой (дыхание)
        VORTEX,         // воронка, стягивающая врагов
        LAUNCH,         // подбросить врагов вокруг
        PUSH,           // отбросить врагов перед собой
        STUN,           // оглушить цель
        STASIS,         // остановка времени в области
        WALL,           // временная стена из блоков
        SWAP,           // поменяться местами с целью
        RECALL,         // вернуться на 3 секунды назад
        GRAPPLE,        // крюк к блоку
        CHARM,          // усмирить врагов
        DOMINATE,       // подчинить моба
        REVEAL,         // подсветить и пометить врагов
        STEAL,          // украсть усиления цели
        RESET,          // сбросить перезарядки
        SACRIFICE,      // здоровье → мана
        MINE,           // невидимая ловушка
        TETHER,         // лечащая связь с союзником
        WARD,           // оберег от смерти на союзника
        SHIELD,         // щит раненому союзнику
        HEAL_TARGET,    // сильное лечение раненого союзника
        GLOBAL,         // молнии по всем врагам вокруг
        RALLY,          // телепорт к союзнику
        GATHER,         // собрать товарищей к себе
        ORB,            // сфера заклинателя (Инвокер)
        INVOKE          // сплести сферы в заклинание
    }

    /** Эффект при попадании для ON_HIT. */
    public enum OnHit { POISON, FIRE, FROST, LIFESTEAL, ARCANE, BLEED, STUN, MANA, CHAIN }

    public record Fx(MobEffect effect, int duration, int amplifier) {}

    private static final List<Ability> ALL = new ArrayList<>();
    private static final Map<String, Ability> BY_KEY = new HashMap<>();

    /** Навыки новичка (до выбора класса). */
    public static final String[] NOVICE;
    public static final int[] NOVICE_UNLOCK;
    /** Ячеек на панели навыков (клавиши Z X C V B G). */
    public static final int BAR = 6;

    public final String key;
    public final Kind kind;
    public final String icon;
    private int index;
    int mana = 20;
    int cooldown = 100;
    Element element = null;
    Stat scale = Stat.STRENGTH;
    float power = 1f;
    float radius = 4f;
    int duration = 200;
    int count = 1;
    boolean execute = false;
    boolean backstab = false;
    boolean lifesteal = false;
    boolean pierce = false;
    boolean reflect = false;
    boolean finisher = false;
    boolean perFoe = false;
    /** Доля здоровья вместо маны. */
    float hpCost = 0;
    /** Сколько приёмов серии даёт навык. */
    int combo = 0;
    OnHit onHit = null;
    String summon = null;
    String wall = null;
    final List<Fx> self = new ArrayList<>();
    final List<Fx> enemy = new ArrayList<>();

    private Ability(String key, Kind kind, String icon) {
        this.key = key;
        this.kind = kind;
        this.icon = icon;
    }

    public int ordinal() { return index; }
    public int manaCost() { return mana; }
    public int cooldown() { return cooldown; }
    public float hpCost() { return hpCost; }
    public String nameKey() { return "ability.litrpg." + key; }
    public String descKey() { return "ability.litrpg." + key + ".desc"; }
    public List<Fx> selfEffects() { return self; }

    public static Ability[] values() { return ALL.toArray(new Ability[0]); }
    public static int count() { return ALL.size(); }
    public static Ability byKey(String key) { return key == null ? null : BY_KEY.get(key); }

    public static Ability byId(int id) {
        return id >= 0 && id < ALL.size() ? ALL.get(id) : null;
    }

    /** Уровень открытия навыка у класса (у новичка — по списку новичка). */
    public static int unlockLevel(PlayerClass c, Ability a) {
        if (a == null) return 999;
        if (c == null) {
            for (int i = 0; i < NOVICE.length; i++) if (NOVICE[i].equals(a.key)) return NOVICE_UNLOCK[i];
            return 999;
        }
        return c.unlockOf(a.key);
    }

    // ---------------- загрузка ----------------

    static {
        JsonObject root = ClassData.root();
        NOVICE = strings(root.getAsJsonArray("novice"));
        JsonArray nu = root.getAsJsonArray("noviceUnlock");
        NOVICE_UNLOCK = new int[nu.size()];
        for (int i = 0; i < NOVICE_UNLOCK.length; i++) NOVICE_UNLOCK[i] = nu.get(i).getAsInt();
        for (JsonElement e : root.getAsJsonArray("skills")) {
            JsonObject o = e.getAsJsonObject();
            Ability a = new Ability(o.get("key").getAsString(), Kind.valueOf(o.get("kind").getAsString()), o.get("icon").getAsString());
            a.mana = o.get("mana").getAsInt();
            a.cooldown = o.get("cd").getAsInt();
            a.scale = Stat.valueOf(o.get("stat").getAsString());
            a.power = o.get("pow").getAsFloat();
            a.radius = o.get("rad").getAsFloat();
            a.duration = o.get("dur").getAsInt();
            a.count = o.get("cnt").getAsInt();
            if (o.has("el")) a.element = Element.valueOf(o.get("el").getAsString());
            if (o.has("flags")) {
                for (String f : strings(o.getAsJsonArray("flags"))) {
                    switch (f) {
                        case "exec" -> a.execute = true;
                        case "back" -> a.backstab = true;
                        case "steal" -> a.lifesteal = true;
                        case "pierce" -> a.pierce = true;
                        case "reflect" -> a.reflect = true;
                        case "finish" -> a.finisher = true;
                        case "perfoe" -> a.perFoe = true;
                        default -> { }
                    }
                }
            }
            if (o.has("onHit")) a.onHit = OnHit.valueOf(o.get("onHit").getAsString());
            if (o.has("summon")) a.summon = o.get("summon").getAsString();
            if (o.has("wall")) a.wall = o.get("wall").getAsString();
            if (o.has("hp")) a.hpCost = o.get("hp").getAsFloat();
            if (o.has("combo")) a.combo = o.get("combo").getAsInt();
            if (o.has("self")) fx(o.getAsJsonArray("self"), a.self);
            if (o.has("foe")) fx(o.getAsJsonArray("foe"), a.enemy);
            a.index = ALL.size();
            ALL.add(a);
            BY_KEY.put(a.key, a);
        }
    }

    private static void fx(JsonArray arr, List<Fx> out) {
        for (JsonElement e : arr) {
            JsonArray f = e.getAsJsonArray();
            MobEffect eff = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(f.get(0).getAsString()));
            if (eff != null) out.add(new Fx(eff, f.get(1).getAsInt(), f.get(2).getAsInt()));
        }
    }

    private static String[] strings(JsonArray a) {
        String[] out = new String[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsString();
        return out;
    }
}
