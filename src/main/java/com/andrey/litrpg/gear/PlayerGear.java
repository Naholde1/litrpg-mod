package com.andrey.litrpg.gear;

import com.andrey.litrpg.stats.Stat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Сводка надетой экипировки игрока: сумма характеристик, бонусов, пассивок и наборов. */
public final class PlayerGear {
    public final int[] stats = new int[Stat.values().length];
    public final Map<GearBonus, Float> bonus = new EnumMap<>(GearBonus.class);
    public final Set<Passive> passives = EnumSet.noneOf(Passive.class);
    public final Set<GearSet> fullSets = EnumSet.noneOf(GearSet.class);
    public final Map<GearSet, Integer> setPieces = new EnumMap<>(GearSet.class);

    public float get(GearBonus b) { return bonus.getOrDefault(b, 0f); }
    public void add(GearBonus b, float v) { bonus.merge(b, v, Float::sum); }
    public void addStat(Stat s, int v) { stats[s.ordinal()] += v; }
    public boolean has(Passive p) { return passives.contains(p); }
    public boolean hasSet(GearSet s) { return fullSets.contains(s); }

    public void clear() {
        java.util.Arrays.fill(stats, 0);
        bonus.clear();
        passives.clear();
        fullSets.clear();
        setPieces.clear();
    }

    public void addItem(GearData d) {
        for (int i = 0; i < stats.length; i++) stats[i] += d.stats[i];
        d.bonuses.forEach(this::add);
        passives.addAll(d.passives);
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.putIntArray("st", stats.clone());
        CompoundTag bo = new CompoundTag();
        bonus.forEach((b, v) -> bo.putFloat(b.key, v));
        t.put("bo", bo);
        ListTag pa = new ListTag();
        for (Passive p : passives) pa.add(StringTag.valueOf(p.key));
        t.put("pa", pa);
        CompoundTag sp = new CompoundTag();
        setPieces.forEach((s, n) -> sp.putInt(s.key, n));
        t.put("sets", sp);
        return t;
    }

    public void load(CompoundTag t) {
        clear();
        int[] st = t.getIntArray("st");
        for (int i = 0; i < Math.min(st.length, stats.length); i++) stats[i] = st[i];
        CompoundTag bo = t.getCompound("bo");
        for (String k : bo.getAllKeys()) {
            GearBonus b = GearBonus.byKey(k);
            if (b != null) bonus.put(b, bo.getFloat(k));
        }
        ListTag pa = t.getList("pa", Tag.TAG_STRING);
        for (int i = 0; i < pa.size(); i++) {
            Passive p = Passive.byKey(pa.getString(i));
            if (p != null) passives.add(p);
        }
        CompoundTag sp = t.getCompound("sets");
        for (String k : sp.getAllKeys()) {
            GearSet s = GearSet.byKey(k);
            if (s != null) {
                setPieces.put(s, sp.getInt(k));
                if (sp.getInt(k) >= 4) fullSets.add(s);
            }
        }
    }
}
