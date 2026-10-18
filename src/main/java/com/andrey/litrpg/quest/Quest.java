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
        MEET
    }

    public enum Source { STORY, SYSTEM, FACTION }

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
        return q;
    }
}
