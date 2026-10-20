package com.andrey.litrpg.quest;

import com.andrey.litrpg.rep.Faction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Задание: что сделать, сколько и какая награда. Хранится в журнале игрока
 * (NBT) и целиком уходит на клиент для окна заданий.
 */
public final class Quest {
    public enum Type {
        /** Убить N существ из категории target (QuestTarget). */
        KILL,
        /** Победить хранителя подземелья target (id босса). */
        BOSS,
        /** Принести N предметов target (id предмета) — сдаются кнопкой «Сдать». */
        COLLECT,
        /** Достичь уровня need. */
        LEVEL,
        /** Поговорить с жителем любого народа. */
        MEET,
        /** Отнести посылку мастеру ремесла target в город place. */
        DELIVER,
        /** Дойти до места (px, pz). */
        VISIT,
        /** Развести N животных target (id сущности). */
        BREED
    }

    public enum Source { STORY, SYSTEM, FACTION, CITY }

    public String id = "";
    public Source source = Source.SYSTEM;
    /** Народ, давший задание (для FACTION). */
    public Faction faction;
    public Type type = Type.KILL;
    public String target = "";
    public int need = 1;
    public int have = 0;
    public String title = "";
    public String desc = "";
    // награда
    public int xp;
    public int coins;
    public int rep;
    /** Редкость предмета-награды (ordinal) или -1. */
    public int gearRarity = -1;
    public int gearLevel = 1;
    /** Дополнительный предмет-награда (id) и количество. */
    public String bonusItem = "";
    public int bonusCount = 0;
    /** Городские задания: город (id), кто дал, куда идти и сколько тварей уже выпущено. */
    public String place = "";
    public String placeName = "";
    public String giver = "";
    public String where = "";
    public int px, py, pz;
    public boolean hasPos;
    public int spawned;

    /** Есть ли у задания точка на карте. */
    public boolean locatable() {
        return hasPos || type == Type.BOSS;
    }

    public boolean isComplete() {
        return have >= need;
    }

    public Item item() {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(target));
    }

    /** Строка цели для интерфейса. */
    public String goalText() {
        return switch (type) {
            case KILL -> {
                QuestTarget t = QuestTarget.byKey(target);
                yield "Убито " + (t == null ? target : t.plural) + ": " + Math.min(have, need) + "/" + need;
            }
            case BOSS -> "Победите хранителя подземелья";
            case COLLECT -> {
                Item it = item();
                yield "Принесите: " + (it == null ? target : it.getDescription().getString()) + " ×" + need;
            }
            case LEVEL -> "Достигните " + need + " уровня";
            case MEET -> "Поговорите с жителем любого народа";
            case DELIVER -> (have >= need ? "Доставлено" : "Отнесите посылку: " + where);
            case VISIT -> (have >= need ? "Место найдено" : "Доберитесь: " + where);
            case BREED -> "Разведено " + breedName() + ": " + Math.min(have, need) + "/" + need;
        };
    }

    public String breedName() {
        return switch (target) {
            case "minecraft:pig" -> "свиней";
            case "minecraft:cow" -> "коров";
            case "minecraft:sheep" -> "овец";
            case "minecraft:chicken" -> "кур";
            case "minecraft:horse" -> "лошадей";
            case "minecraft:rabbit" -> "кроликов";
            case "minecraft:goat" -> "коз";
            case "minecraft:bee" -> "пчёл";
            default -> "животных";
        };
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putString("id", id);
        t.putInt("src", source.ordinal());
        if (faction != null) t.putString("fac", faction.key);
        t.putInt("type", type.ordinal());
        t.putString("target", target);
        t.putInt("need", need);
        t.putInt("have", have);
        t.putString("title", title);
        t.putString("desc", desc);
        t.putInt("xp", xp);
        t.putInt("coins", coins);
        t.putInt("rep", rep);
        t.putInt("gear", gearRarity);
        t.putInt("glvl", gearLevel);
        if (!bonusItem.isEmpty()) {
            t.putString("bonus", bonusItem);
            t.putInt("bonusN", bonusCount);
        }
        if (!place.isEmpty()) t.putString("place", place);
        if (!placeName.isEmpty()) t.putString("placeN", placeName);
        if (!giver.isEmpty()) t.putString("giver", giver);
        if (!where.isEmpty()) t.putString("where", where);
        if (hasPos) t.putIntArray("pos", new int[]{px, py, pz});
        if (spawned > 0) t.putInt("spawned", spawned);
        return t;
    }

    public static Quest load(CompoundTag t) {
        Quest q = new Quest();
        q.id = t.getString("id");
        q.source = Source.values()[Math.max(0, Math.min(Source.values().length - 1, t.getInt("src")))];
        q.faction = t.contains("fac") ? Faction.byKey(t.getString("fac")) : null;
        q.type = Type.values()[Math.max(0, Math.min(Type.values().length - 1, t.getInt("type")))];
        q.target = t.getString("target");
        q.need = Math.max(1, t.getInt("need"));
        q.have = t.getInt("have");
        q.title = t.getString("title");
        q.desc = t.getString("desc");
        q.xp = t.getInt("xp");
        q.coins = t.getInt("coins");
        q.rep = t.getInt("rep");
        q.gearRarity = t.contains("gear") ? t.getInt("gear") : -1;
        q.gearLevel = t.getInt("glvl");
        q.bonusItem = t.getString("bonus");
        q.bonusCount = t.getInt("bonusN");
        q.place = t.getString("place");
        q.placeName = t.getString("placeN");
        q.giver = t.getString("giver");
        q.where = t.getString("where");
        int[] pos = t.getIntArray("pos");
        if (pos.length == 3) {
            q.hasPos = true;
            q.px = pos[0];
            q.py = pos[1];
            q.pz = pos[2];
        }
        q.spawned = t.getInt("spawned");
        return q;
    }
}
