package com.andrey.litrpg.quest;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Журнал заданий игрока: сюжет, взятые задания и предложения (Системы и народов). */
public final class QuestLog {
    /** Сколько обычных заданий можно держать одновременно. */
    public static final int MAX_ACTIVE = 5;
    /** Ключ предложений Системы (у народов — ключ народа). */
    public static final String SYSTEM = "system";

    public int storyStep = 0;
    public Quest story;
    public final List<Quest> active = new ArrayList<>();
    public final Map<String, List<Quest>> offers = new HashMap<>();
    /** День (gameTime / 24000), когда предложения были составлены. */
    public final Map<String, Long> offersDay = new HashMap<>();
    public int completed = 0;
    public int nextId = 1;

    public Quest find(String id) {
        if (story != null && story.id.equals(id)) return story;
        for (Quest q : active) if (q.id.equals(id)) return q;
        return null;
    }

    public List<Quest> offers(String key) {
        return offers.getOrDefault(key, List.of());
    }

    /** Все задания, по которым сейчас идёт прогресс. */
    public List<Quest> tracked() {
        List<Quest> out = new ArrayList<>(active);
        if (story != null) out.add(0, story);
        return out;
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putInt("step", storyStep);
        if (story != null) t.put("story", story.save());
        ListTag act = new ListTag();
        for (Quest q : active) act.add(q.save());
        t.put("active", act);
        CompoundTag off = new CompoundTag();
        for (var e : offers.entrySet()) {
            ListTag l = new ListTag();
            for (Quest q : e.getValue()) l.add(q.save());
            off.put(e.getKey(), l);
        }
        t.put("offers", off);
        CompoundTag days = new CompoundTag();
        offersDay.forEach(days::putLong);
        t.put("days", days);
        t.putInt("done", completed);
        t.putInt("next", nextId);
        return t;
    }

    public void load(CompoundTag t) {
        storyStep = t.getInt("step");
        story = t.contains("story") ? Quest.load(t.getCompound("story")) : null;
        active.clear();
        for (Tag q : t.getList("active", Tag.TAG_COMPOUND)) active.add(Quest.load((CompoundTag) q));
        offers.clear();
        CompoundTag off = t.getCompound("offers");
        for (String k : off.getAllKeys()) {
            List<Quest> l = new ArrayList<>();
            for (Tag q : off.getList(k, Tag.TAG_COMPOUND)) l.add(Quest.load((CompoundTag) q));
            offers.put(k, l);
        }
        offersDay.clear();
        CompoundTag days = t.getCompound("days");
        for (String k : days.getAllKeys()) offersDay.put(k, days.getLong(k));
        completed = t.getInt("done");
        nextId = Math.max(1, t.getInt("next"));
    }
}
