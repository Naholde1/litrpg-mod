package com.andrey.litrpg.city;

import com.andrey.litrpg.npc.Craft;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Память города о разрушениях: какой блок где стоял. Строители (мастера
 * ремесла «строитель») ходят к повреждённым местам и восстанавливают их;
 * если строителей нет — Система сама чинит старые повреждения.
 */
public class CityRepairs extends SavedData {
    private static final String NAME = "litrpg_city_repairs";
    /** Сколько ждать, прежде чем строители возьмутся за дело (тиков). */
    private static final long DELAY = 20 * 20;
    /** Без строителей город чинится сам через столько тиков. */
    private static final long SELF_REPAIR = 20 * 60 * 6;

    private record Entry(BlockPos pos, BlockState state, long time, String city) {}

    private record Task(long pos, BlockPos home, long since) {}

    private final Map<Long, Entry> entries = new LinkedHashMap<>();
    /** Места, которые строители уже восстановили: ломать их снова — без добычи (против бесконечной кражи). */
    private final java.util.Set<Long> restored = new java.util.HashSet<>();
    private final Map<RaceNpcEntity, Task> tasks = new WeakHashMap<>();

    public static CityRepairs get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(t -> load(level, t), CityRepairs::new, NAME);
    }

    // ---------------- запись ----------------

    /** Запомнить исходный блок (если это место ещё не помнится). */
    public void record(ServerLevel level, BlockPos pos, BlockState original, String city) {
        put(level, pos, original, city);
        // вторая половина двери / кровати / высокого растения
        if (original.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos other = original.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            put(level, other, level.getBlockState(other), city);
        } else if (original.hasProperty(BlockStateProperties.BED_PART) && original.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction f = original.getValue(BlockStateProperties.HORIZONTAL_FACING);
            BlockPos other = original.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT ? pos.relative(f) : pos.relative(f.getOpposite());
            put(level, other, level.getBlockState(other), city);
        }
    }

    private void put(ServerLevel level, BlockPos pos, BlockState st, String city) {
        long k = pos.asLong();
        if (entries.containsKey(k)) return;
        entries.put(k, new Entry(pos.immutable(), st, level.getGameTime(), city));
        setDirty();
    }

    /** Исходный блок на месте (или null, если место не повреждено). */
    public BlockState original(BlockPos pos) {
        Entry e = entries.get(pos.asLong());
        return e == null ? null : e.state;
    }

    public boolean wasRestored(BlockPos pos) {
        return restored.contains(pos.asLong());
    }

    public void forget(BlockPos pos) {
        if (entries.remove(pos.asLong()) != null) setDirty();
    }

    public int pending(String city) {
        int n = 0;
        for (Entry e : entries.values()) if (e.city.equals(city)) n++;
        return n;
    }

    // ---------------- восстановление ----------------

    public void tick(ServerLevel level) {
        if (entries.isEmpty()) return;
        long now = level.getGameTime();
        Map<String, List<Entry>> byCity = new HashMap<>();
        for (Entry e : entries.values()) byCity.computeIfAbsent(e.city, k -> new ArrayList<>()).add(e);
        for (var en : byCity.entrySet()) {
            WorldMap.City c = WorldMap.get().city(en.getKey());
            if (c == null) {
                en.getValue().forEach(e -> entries.remove(e.pos.asLong()));
                continue;
            }
            AABB box = new AABB(c.x() - c.r(), c.y() - 40, c.z() - c.r(), c.x() + c.r(), c.y() + 80, c.z() + c.r());
            List<RaceNpcEntity> builders = level.getEntitiesOfClass(RaceNpcEntity.class, box, n -> n.craft() == Craft.BUILDER && n.isAlive());
            if (builders.isEmpty()) {
                // строителей нет (или далеко) — Система латает старые раны города
                for (Entry e : en.getValue()) {
                    if (now - e.time > SELF_REPAIR && level.isLoaded(e.pos)) restore(level, e, null);
                }
                continue;
            }
            for (RaceNpcEntity b : builders) work(level, b, en.getValue(), now);
        }
    }

    private void work(ServerLevel level, RaceNpcEntity b, List<Entry> list, long now) {
        Task t = tasks.get(b);
        if (t != null) {
            Entry e = entries.get(t.pos);
            if (e == null) {
                finish(b, t);
                return;
            }
            double d = b.position().distanceTo(e.pos.getCenter());
            if (d < 4.5 || now - t.since > 20 * 30) {
                restore(level, e, b);
                finish(b, t);
            } else {
                b.getNavigation().moveTo(e.pos.getX() + 0.5, e.pos.getY(), e.pos.getZ() + 0.5, 0.9);
            }
            return;
        }
        Entry best = null;
        for (Entry e : list) {
            if (now - e.time < DELAY || !level.isLoaded(e.pos)) continue;
            boolean taken = false;
            for (Task o : tasks.values()) taken |= o.pos == e.pos.asLong();
            if (taken) continue;
            if (best == null || b.distanceToSqr(e.pos.getCenter()) < b.distanceToSqr(best.pos.getCenter())) best = e;
        }
        if (best == null) return;
        BlockPos home = b.hasRestriction() ? b.getRestrictCenter() : b.blockPosition();
        tasks.put(b, new Task(best.pos.asLong(), home, now));
        b.restrictTo(best.pos, 3);
        b.getNavigation().moveTo(best.pos.getX() + 0.5, best.pos.getY(), best.pos.getZ() + 0.5, 0.9);
        if (level.random.nextInt(3) == 0) {
            String[] lines = {"Опять кто-то ломал! Иду чинить.", "Сейчас заделаем.", "Ну что за варвары…", "Камень к камню!"};
            Speech.say(b, lines[level.random.nextInt(lines.length)], 60);
        }
    }

    private void finish(RaceNpcEntity b, Task t) {
        tasks.remove(b);
        b.restrictTo(t.home, 40);
    }

    private void restore(ServerLevel level, Entry e, RaceNpcEntity builder) {
        BlockState now = level.getBlockState(e.pos);
        // не замуровать игрока
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(e.pos).inflate(0.1))) {
            if (!e.state.isAir()) return;
        }
        entries.remove(e.pos.asLong());
        setDirty();
        if (now.equals(e.state)) return;
        if (!e.state.isAir() && restored.size() < 200000) restored.add(e.pos.asLong());
        if (e.state.isAir()) {
            // постройка игрока в городе — разобрать, вернув материалы
            level.destroyBlock(e.pos, true);
        } else {
            level.setBlock(e.pos, e.state, 3);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, e.state), e.pos.getX() + 0.5, e.pos.getY() + 0.5,
                    e.pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.05);
            level.playSound(null, e.pos, e.state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 0.9f);
        }
        if (builder != null) {
            builder.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, e.pos.getX() + 0.5, e.pos.getY() + 1, e.pos.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
        }
    }

    // ---------------- сохранение ----------------

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag l = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putLong("p", e.pos.asLong());
            t.put("s", NbtUtils.writeBlockState(e.state));
            t.putLong("t", e.time);
            t.putString("c", e.city);
            l.add(t);
        }
        tag.put("entries", l);
        tag.putLongArray("restored", restored.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }

    private static CityRepairs load(ServerLevel level, CompoundTag tag) {
        CityRepairs r = new CityRepairs();
        var blocks = level.holderLookup(Registries.BLOCK);
        for (Tag el : tag.getList("entries", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) el;
            BlockPos pos = BlockPos.of(t.getLong("p"));
            BlockState st = NbtUtils.readBlockState(blocks, t.getCompound("s"));
            r.entries.put(pos.asLong(), new Entry(pos, st, t.getLong("t"), t.getString("c")));
        }
        for (long v : tag.getLongArray("restored")) r.restored.add(v);
        return r;
    }
}
