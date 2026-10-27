/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

public class RunState extends SavedData {
    private static final String NAME = "litrpg_run";
    public int stage = 0;
    public int minPlayers = 3;
    public int wipes = 0;
    public long stageStart = 0L;
    public int partySize = 3;
    public final Set<String> deadPacks = new HashSet();
    public final Set<String> deadBosses = new HashSet();
    public final Map<String, List<UUID>> packMobs = new HashMap();
    public final Map<String, UUID> bossMobs = new HashMap();
    public final Map<String, Boolean> gates = new HashMap();
    public final Set<UUID> killedMobs = new HashSet();
    public final Map<String, CompoundTag> mechs = new HashMap();

    public RunState() {
    }

    public static RunState get(MinecraftServer server) {
        return (RunState)server.overworld().getDataStorage().computeIfAbsent(RunState::load, RunState::new, "litrpg_run");
    }

    public void clearStage() {
        this.deadPacks.clear();
        this.deadBosses.clear();
        this.packMobs.clear();
        this.bossMobs.clear();
        this.killedMobs.clear();
        this.gates.clear();
        this.mechs.clear();
        this.setDirty();
    }

    public boolean packKilled(UUID id) {
        return this.killedMobs.contains(id);
    }

    public CompoundTag mech(String id) {
        return (CompoundTag)this.mechs.computeIfAbsent(id, (k) -> new CompoundTag());
    }

    public CompoundTag save(CompoundTag tag) {
        tag.putInt("Stage", this.stage);
        tag.putInt("Min", this.minPlayers);
        tag.putInt("Wipes", this.wipes);
        tag.putLong("Start", this.stageStart);
        tag.putInt("Party", this.partySize);
        tag.put("DeadPacks", strings(this.deadPacks));
        tag.put("DeadBosses", strings(this.deadBosses));
        CompoundTag pm = new CompoundTag();
        this.packMobs.forEach((k, v) -> {
            ListTag l = new ListTag();

            for(UUID u : v) {
                l.add(NbtUtils.createUUID(u));
            }

            pm.put(k, l);
        });
        tag.put("PackMobs", pm);
        CompoundTag bm = new CompoundTag();
        this.bossMobs.forEach((k, v) -> bm.putUUID(k, v));
        tag.put("BossMobs", bm);
        CompoundTag g = new CompoundTag();
        this.gates.forEach(g::putBoolean);
        tag.put("Gates", g);
        CompoundTag m = new CompoundTag();
        this.mechs.forEach(m::put);
        tag.put("Mechs", m);
        ListTag km = new ListTag();

        for(UUID u : this.killedMobs) {
            km.add(NbtUtils.createUUID(u));
        }

        tag.put("Killed", km);
        return tag;
    }

    private static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();

        for(String s : set) {
            l.add(StringTag.valueOf(s));
        }

        return l;
    }

    private static RunState load(CompoundTag tag) {
        RunState r = new RunState();
        r.stage = tag.getInt("Stage");
        r.minPlayers = tag.contains("Min") ? tag.getInt("Min") : 3;
        r.wipes = tag.getInt("Wipes");
        r.stageStart = tag.getLong("Start");
        r.partySize = tag.contains("Party") ? tag.getInt("Party") : 3;

        for(Tag t : tag.getList("DeadPacks", 8)) {
            r.deadPacks.add(t.getAsString());
        }

        for(Tag t : tag.getList("DeadBosses", 8)) {
            r.deadBosses.add(t.getAsString());
        }

        CompoundTag pm = tag.getCompound("PackMobs");

        for(String k : pm.getAllKeys()) {
            List<UUID> l = new ArrayList();

            for(Tag t : pm.getList(k, 11)) {
                l.add(NbtUtils.loadUUID(t));
            }

            r.packMobs.put(k, l);
        }

        CompoundTag bm = tag.getCompound("BossMobs");

        for(String k : bm.getAllKeys()) {
            r.bossMobs.put(k, bm.getUUID(k));
        }

        CompoundTag g = tag.getCompound("Gates");

        for(String k : g.getAllKeys()) {
            r.gates.put(k, g.getBoolean(k));
        }

        CompoundTag m = tag.getCompound("Mechs");

        for(String k : m.getAllKeys()) {
            r.mechs.put(k, m.getCompound(k));
        }

        for(Tag t : tag.getList("Killed", 11)) {
            r.killedMobs.add(NbtUtils.loadUUID(t));
        }

        return r;
    }
}
