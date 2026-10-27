/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.block.MirrorBlock;
import com.andrey.litrpg.block.RuneDialBlock;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.util.Scheduler;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Mechanisms {
    private static final DustParticleOptions WARN = new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.1F), 1.6F);
    private static final DustParticleOptions LIGHT = new DustParticleOptions(new Vector3f(1.0F, 0.95F, 0.6F), 1.4F);
    private static final DustParticleOptions GOOD = new DustParticleOptions(new Vector3f(0.4F, 1.0F, 0.5F), 1.4F);
    private static final Map<Integer, Map<BlockPos, Layout.Mech>> BY_POS = new HashMap();

    private Mechanisms() {
    }

    private static Map<BlockPos, Layout.Mech> index(Layout.Stage s) {
        return (Map)BY_POS.computeIfAbsent(s.n(), (k) -> {
            Map<BlockPos, Layout.Mech> m = new HashMap();

            for(Layout.Mech mech : s.mechs()) {
                for(String key : new String[]{"plates", "levers", "dials", "mirrors", "bells", "buttons", "braziers", "valves", "show"}) {
                    if (mech.p().has(key)) {
                        JsonElement el = mech.p().get(key);
                        if (el.isJsonArray() && el.getAsJsonArray().size() > 0 && el.getAsJsonArray().get(0).isJsonArray()) {
                            for(JsonElement e : el.getAsJsonArray()) {
                                m.put(pos(e.getAsJsonArray()), mech);
                            }
                        } else if (el.isJsonArray()) {
                            m.put(pos(el.getAsJsonArray()), mech);
                        }
                    }
                }
            }

            return m;
        });
    }

    static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static List<BlockPos> positions(JsonObject p, String key) {
        List<BlockPos> out = new ArrayList();
        if (p.has(key)) {
            for(JsonElement e : p.getAsJsonArray(key)) {
                out.add(pos(e.getAsJsonArray()));
            }
        }

        return out;
    }

    private static List<String> strings(JsonObject p, String key) {
        List<String> out = new ArrayList();
        if (p.has(key)) {
            for(JsonElement e : p.getAsJsonArray(key)) {
                out.add(e.getAsString());
            }
        }

        return out;
    }

    private static int[] ints(JsonObject p, String key) {
        if (!p.has(key)) {
            return new int[0];
        } else {
            JsonArray a = p.getAsJsonArray(key);
            int[] out = new int[a.size()];

            for(int i = 0; i < out.length; ++i) {
                out[i] = a.get(i).getAsInt();
            }

            return out;
        }
    }

    private static int i(JsonObject p, String key, int def) {
        return p.has(key) ? p.get(key).getAsInt() : def;
    }

    private static float f(JsonObject p, String key, float def) {
        return p.has(key) ? p.get(key).getAsFloat() : def;
    }

    private static String s(JsonObject p, String key, String def) {
        return p.has(key) ? p.get(key).getAsString() : def;
    }

    private static Layout.Box box(JsonObject p, String key) {
        return p.has(key) ? Layout.Box.of(p.getAsJsonArray(key)) : null;
    }

    static void reset(ServerLevel dl, RunState st, Layout.Stage s) {
        for(Layout.Mech m : s.mechs()) {
            JsonObject p = m.p();
            switch (m.type()) {
                case "dials":
                    int[] start = ints(p, "start");
                    List<BlockPos> dials = positions(p, "dials");

                    for(int k = 0; k < dials.size(); ++k) {
                        BlockState bs = dl.getBlockState((BlockPos)dials.get(k));
                        if (bs.is((Block)ModBlocks.RUNE_DIAL.get())) {
                            dl.setBlock((BlockPos)dials.get(k), (BlockState)bs.setValue(RuneDialBlock.SYMBOL, k < start.length ? start[k] : 0), 3);
                        }
                    }
                    break;
                case "beam":
                    int[] mirrorStart = ints(p, "start");
                    List<BlockPos> mirrors = positions(p, "mirrors");

                    for(int k = 0; k < mirrors.size(); ++k) {
                        BlockState bs = dl.getBlockState((BlockPos)mirrors.get(k));
                        if (bs.is((Block)ModBlocks.MIRROR.get())) {
                            dl.setBlock((BlockPos)mirrors.get(k), (BlockState)bs.setValue(MirrorBlock.TURN, k < mirrorStart.length ? mirrorStart[k] : 0), 3);
                        }
                    }
                    break;
                case "sequence":
                case "split":
                    for(BlockPos l : positions(p, "levers")) {
                        setLever(dl, l, false);
                    }

                    for(BlockPos l : positions(p, "lamps")) {
                        setLamp(dl, l, false);
                    }
                    break;
                case "braziers":
                    for(BlockPos b : positions(p, "braziers")) {
                        setFire(dl, b, false);
                    }
                    break;
                case "switch":
                    for(BlockPos l : positions(p, "levers")) {
                        setLever(dl, l, false);
                    }
                    break;
                case "collapse":
                case "memory":
                    restoreFloor(dl, st, m);
                    break;
                case "valves":
                    setWater(dl, m, i(p, "startLevel", 0));
            }
        }

    }

    static void tick(ServerLevel dl, RunState st, Layout.Stage s) {
        long t = dl.getGameTime();
        List<ServerPlayer> alive = Run.alive(dl.getServer());
        Iterator var6 = s.mechs().iterator();

        while(true) {
            Layout.Mech m;
            CompoundTag d;
            boolean any;
            do {
                if (!var6.hasNext()) {
                    return;
                }

                m = (Layout.Mech)var6.next();
                d = st.mech(m.id());
                JsonObject p = m.p();
                Layout.Box near = box(p, "near");
                if (near == null) {
                    break;
                }

                any = false;

                for(ServerPlayer pl : alive) {
                    if (near.aabb().inflate((double)6.0F).contains(pl.position())) {
                        any = true;
                    }
                }
            } while(!any);

            switch (m.type()) {
                case "plates":
                    plates(dl, st, s, m, d, alive);
                    break;
                case "sequence":
                    sequenceTick(dl, m, d, t);
                    break;
                case "beam":
                    beamTick(dl, st, s, m, d, t);
                    break;
                case "memory":
                    memoryTick(dl, st, s, m, d, alive, t);
                    break;
                case "braziers":
                    braziersTick(dl, st, s, m, d, t);
                    break;
                case "area":
                    areaTick(dl, st, s, m, d, alive);
                    break;
                case "spikes":
                case "flames":
                case "crusher":
                    pulseTrap(dl, m, d, alive, t);
                    break;
                case "blades":
                    bladesTick(dl, m, alive, t);
                    break;
                case "arrows":
                    arrowsTick(dl, m, alive, t);
                    break;
                case "collapse":
                    collapseTick(dl, st, m, d, alive, t);
                    break;
                case "gas":
                    gasTick(dl, m, alive, t);
                    break;
                case "rhythm":
                    rhythmTick(dl, st, m, t);
                    break;
                case "current":
                    currentTick(dl, m, alive, t);
                    break;
                case "portal":
                    portalTick(dl, st, s, m, d, alive);
            }
        }
    }

    static void solve(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m) {
        CompoundTag d = st.mech(m.id());
        if (!d.getBoolean("solved")) {
            d.putBoolean("solved", true);
            st.setDirty();

            for(String g : strings(m.p(), "opens")) {
                Run.gate(dl.getServer(), g, true);
            }

            for(String g : strings(m.p(), "closes")) {
                Run.gate(dl.getServer(), g, false);
            }

            for(String pk : strings(m.p(), "spawns")) {
                Run.spawnPackNow(dl, st, s, pk);
            }

            Vec3 c = center(m);
            if (c != null) {
                dl.playSound((Player)null, c.x, c.y, c.z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 1.2F);
                dl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y + (double)1.0F, c.z, 40, (double)1.5F, (double)1.0F, (double)1.5F, 0.3);
            }

            String title = s(m.p(), "solved", "Загадка решена");
            if (m.p().has("xp") && m.p().get("xp").getAsBoolean()) {
                Rewards.puzzleSolved(dl.getServer(), s, m.id(), title);
            } else if (!title.isEmpty()) {
                for(ServerPlayer p : Run.party(dl.getServer())) {
                    p.displayClientMessage(Component.literal(title).withStyle(ChatFormatting.AQUA), true);
                }
            }

        }
    }

    private static Vec3 center(Layout.Mech m) {
        Layout.Box b = box(m.p(), "near");
        if (b != null) {
            return b.center();
        } else {
            for(String key : new String[]{"plates", "levers", "dials", "mirrors", "bells", "buttons", "braziers"}) {
                List<BlockPos> l = positions(m.p(), key);
                if (!l.isEmpty()) {
                    return Vec3.atCenterOf((Vec3i)l.get(0));
                }
            }

            return null;
        }
    }

    private static boolean solved(RunState st, Layout.Mech m) {
        return st.mech(m.id()).getBoolean("solved");
    }

    public static boolean locked(ServerLevel dl, BlockPos pos) {
        Layout.Stage s = Run.current(dl.getServer());
        if (s == null) {
            return false;
        } else {
            Layout.Mech m = (Layout.Mech)index(s).get(pos);
            return m != null && solved(Run.state(dl.getServer()), m);
        }
    }

    public static void changed(ServerLevel dl, BlockPos pos, Player who) {
        Layout.Stage s = Run.current(dl.getServer());
        if (s != null) {
            Layout.Mech m = (Layout.Mech)index(s).get(pos);
            if (m != null) {
                RunState st = Run.state(dl.getServer());
                if (!solved(st, m)) {
                    if (m.type().equals("dials")) {
                        List<BlockPos> dials = positions(m.p(), "dials");
                        int[] want = ints(m.p(), "answer");

                        for(int k = 0; k < dials.size(); ++k) {
                            BlockState bs = dl.getBlockState((BlockPos)dials.get(k));
                            if (!bs.is((Block)ModBlocks.RUNE_DIAL.get()) || k >= want.length || (Integer)bs.getValue(RuneDialBlock.SYMBOL) != want[k]) {
                                return;
                            }
                        }

                        solve(dl, st, s, m);
                    }

                }
            }
        }
    }

    static void packCleared(ServerLevel dl, RunState st, Layout.Stage s, String packId) {
        for(Layout.Mech m : s.mechs()) {
            if (m.type().equals("clear") && !solved(st, m)) {
                List<String> need = strings(m.p(), "packs");
                if (need.contains(packId)) {
                    boolean all = true;

                    for(String pk : need) {
                        if (!st.deadPacks.contains(pk)) {
                            all = false;
                        }
                    }

                    if (all) {
                        solve(dl, st, s, m);
                    }
                }
            }
        }

    }

    static void bossKilled(ServerLevel dl, RunState st, Layout.Stage s, String bossId) {
        for(Layout.Mech m : s.mechs()) {
            if (m.type().equals("boss") && s(m.p(), "boss", "").equals(bossId)) {
                solve(dl, st, s, m);
            }
        }

    }

    @SubscribeEvent(
        priority = EventPriority.HIGH
    )
    public static void onClick(PlayerInteractEvent.RightClickBlock e) {
        Level var2 = e.getLevel();
        if (var2 instanceof ServerLevel dl) {
            if (dl.dimension() == Run.DIM && e.getHand() == InteractionHand.MAIN_HAND) {
                Player var3 = e.getEntity();
                if (var3 instanceof ServerPlayer) {
                    ServerPlayer p = (ServerPlayer)var3;
                    if (!Spirits.isSpirit(p)) {
                        Layout.Stage s = Run.current(dl.getServer());
                        if (s == null) {
                            return;
                        }

                        BlockPos pos = e.getPos();
                        Layout.Mech m = (Layout.Mech)index(s).get(pos);
                        if (m == null) {
                            return;
                        }

                        RunState st = Run.state(dl.getServer());
                        CompoundTag d = st.mech(m.id());
                        BlockState bs = dl.getBlockState(pos);
                        switch (m.type()) {
                            case "sequence":
                                e.setCanceled(true);
                                if (solved(st, m)) {
                                    return;
                                }

                                if (pos.equals(pos(m.p().getAsJsonArray("show")))) {
                                    showSequence(dl, m, d);
                                    return;
                                }

                                if (!(bs.getBlock() instanceof LeverBlock)) {
                                    return;
                                }

                                List<BlockPos> levers = positions(m.p(), "levers");
                                int idx = levers.indexOf(pos);
                                int[] order = ints(m.p(), "order");
                                int step = d.getInt("step");
                                if (d.getLong("showUntil") > dl.getGameTime()) {
                                    return;
                                }

                                if (step < order.length && order[step] == idx) {
                                    setLever(dl, pos, true);
                                    d.putInt("step", step + 1);
                                    note(dl, pos, step);
                                    if (step + 1 >= order.length) {
                                        solve(dl, st, s, m);
                                    }
                                } else {
                                    fail(dl, st, s, m, p, "Не тот порядок! Механизм сбрасывается.");
                                    d.putInt("step", 0);

                                    for(BlockPos l : levers) {
                                        setLever(dl, l, false);
                                    }
                                }

                                st.setDirty();
                                break;
                            case "bells":
                                if (!(bs.getBlock() instanceof BellBlock) || solved(st, m)) {
                                    return;
                                }

                                List<BlockPos> bells = positions(m.p(), "bells");
                                int bellIdx = bells.indexOf(pos);
                                int[] bellOrder = ints(m.p(), "order");
                                int bellStep = d.getInt("step");
                                long last = d.getLong("last");
                                if (dl.getGameTime() - last > 200L) {
                                    bellStep = 0;
                                }

                                d.putLong("last", dl.getGameTime());
                                if (bellStep < bellOrder.length && bellOrder[bellStep] == bellIdx) {
                                    d.putInt("step", bellStep + 1);
                                    dl.sendParticles(GOOD, (double)pos.getX() + (double)0.5F, (double)pos.getY() + 1.2, (double)pos.getZ() + (double)0.5F, 12, 0.4, 0.3, 0.4, (double)0.0F);
                                    if (bellStep + 1 >= bellOrder.length) {
                                        solve(dl, st, s, m);
                                    }
                                } else {
                                    d.putInt("step", bellOrder.length > 0 && bellOrder[0] == bellIdx ? 1 : 0);
                                    dl.playSound((Player)null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.5F, 0.5F);
                                    p.displayClientMessage(Component.literal("Колокола звучат нестройно…").withStyle(ChatFormatting.GRAY), true);
                                }

                                st.setDirty();
                                break;
                            case "riddle":
                                if (!(bs.getBlock() instanceof ButtonBlock) || solved(st, m) || d.getBoolean("failed")) {
                                    return;
                                }

                                List<BlockPos> buttons = positions(m.p(), "buttons");
                                if (buttons.indexOf(pos) == i(m.p(), "answer", 0)) {
                                    solve(dl, st, s, m);
                                } else {
                                    d.putBoolean("failed", true);
                                    st.setDirty();
                                    fail(dl, st, s, m, p, s(m.p(), "wrong", "Неверно! Стены оживают…"));
                                    Scheduler.later(200, () -> {
                                        st.mech(m.id()).putBoolean("failed", false);
                                        st.setDirty();
                                    });
                                }
                                break;
                            case "split":
                                e.setCanceled(true);
                                if (!(bs.getBlock() instanceof LeverBlock) || solved(st, m)) {
                                    return;
                                }

                                List<BlockPos> timedLevers = positions(m.p(), "levers");
                                int leverIdx = timedLevers.indexOf(pos);
                                long now = dl.getGameTime();
                                d.putLong("t" + leverIdx, now);
                                setLever(dl, pos, true);
                                int window = i(m.p(), "window", 30);
                                boolean all = true;

                                for(int k = 0; k < timedLevers.size(); ++k) {
                                    if (now - d.getLong("t" + k) > (long)window) {
                                        all = false;
                                    }
                                }

                                if (all) {
                                    solve(dl, st, s, m);
                                } else {
                                    Scheduler.later(window, () -> {
                                        if (!solved(st, m)) {
                                            setLever(dl, pos, false);
                                        }

                                    });
                                }

                                st.setDirty();
                                break;
                            case "braziers":
                                e.setCanceled(true);
                                if (!(bs.getBlock() instanceof CampfireBlock) || solved(st, m) || (Boolean)bs.getValue(CampfireBlock.LIT)) {
                                    return;
                                }

                                setFire(dl, pos, true);
                                d.putLong(pos.toShortString(), dl.getGameTime());
                                dl.playSound((Player)null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                st.setDirty();
                                break;
                            case "switch":
                                if (solved(st, m)) {
                                    return;
                                }

                                solve(dl, st, s, m);
                                break;
                            case "valves":
                                e.setCanceled(true);
                                List<BlockPos> valves = positions(m.p(), "valves");
                                int valveIdx = valves.indexOf(pos);
                                int[] delta = ints(m.p(), "delta");
                                int levels = i(m.p(), "levels", 3);
                                int lv = Math.max(0, Math.min(levels, d.getInt("level") + (valveIdx < delta.length ? delta[valveIdx] : 1)));
                                d.putInt("level", lv);
                                st.setDirty();
                                setWater(dl, m, lv);
                                dl.playSound((Player)null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.5F, 0.6F);
                                p.displayClientMessage(Component.literal("Уровень воды: " + lv + " / " + levels).withStyle(ChatFormatting.AQUA), true);
                                if (lv == i(m.p(), "target", levels)) {
                                    solve(dl, st, s, m);
                                }
                        }

                        return;
                    }
                }

                return;
            }
        }

    }

    private static void fail(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, ServerPlayer who, String text) {
        for(ServerPlayer p : Run.party(dl.getServer())) {
            p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.RED), true);
        }

        dl.playSound((Player)null, who.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.BLOCKS, 1.2F, 0.6F);

        for(String pk : strings(m.p(), "ambush")) {
            Run.spawnPackNow(dl, st, s, pk);
        }

        float dmg = f(m.p(), "punish", 0.15F);
        if (dmg > 0.0F) {
            Vec3 c = center(m);

            for(ServerPlayer p : Run.alive(dl.getServer())) {
                if (c == null || p.position().distanceTo(c) < (double)16.0F) {
                    p.hurt(p.damageSources().magic(), p.getMaxHealth() * dmg);
                    dl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, p.getX(), p.getY() + (double)1.0F, p.getZ(), 15, 0.3, 0.6, 0.3, 0.05);
                }
            }
        }

    }

    private static void plates(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive) {
        if (!solved(st, m)) {
            List<BlockPos> plates = positions(m.p(), "plates");
            int on = 0;

            for(BlockPos pl : plates) {
                boolean taken = false;

                for(ServerPlayer p : alive) {
                    BlockPos at = p.blockPosition();
                    if (at.equals(pl) || at.equals(pl.above()) || at.below().equals(pl)) {
                        taken = true;
                    }
                }

                dl.sendParticles(taken ? GOOD : LIGHT, (double)pl.getX() + (double)0.5F, (double)pl.getY() + 1.1, (double)pl.getZ() + (double)0.5F, taken ? 4 : 1, 0.3, 0.05, 0.3, (double)0.0F);
                if (taken) {
                    ++on;
                }
            }

            int hold = i(m.p(), "hold", 20);
            int held = on >= plates.size() ? d.getInt("held") + 5 : 0;
            d.putInt("held", held);
            if (on > 0 && on < plates.size() && dl.getGameTime() % 40L == 0L) {
                for(ServerPlayer p : alive) {
                    if (p.position().distanceTo(Vec3.atCenterOf((Vec3i)plates.get(0))) < (double)24.0F) {
                        p.displayClientMessage(Component.literal("Плиты: " + on + " из " + plates.size() + " — нужны все сразу").withStyle(ChatFormatting.GOLD), true);
                    }
                }
            }

            if (held >= hold) {
                solve(dl, st, s, m);
            }

        }
    }

    private static void showSequence(ServerLevel dl, Layout.Mech m, CompoundTag d) {
        long now = dl.getGameTime();
        if (d.getLong("showUntil") <= now) {
            int[] order = ints(m.p(), "order");
            List<BlockPos> lamps = positions(m.p(), "lamps");
            List<BlockPos> levers = positions(m.p(), "levers");
            d.putInt("step", 0);

            for(BlockPos l : levers) {
                setLever(dl, l, false);
            }

            d.putLong("showUntil", now + 20L * (long)order.length + 30L);
            BlockPos bell = pos(m.p().getAsJsonArray("show"));
            dl.playSound((Player)null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.5F, 1.0F);

            for(int k = 0; k < order.length; ++k) {
                int idx = order[k];
                // нота зависит от места в последовательности; счётчику цикла в лямбду нельзя,
                // поэтому берём его копию (декомпилятор эту переменную потерял)
                int tone = k;
                Scheduler.later(20 + k * 20, () -> {
                    if (idx < lamps.size()) {
                        BlockPos lp = (BlockPos)lamps.get(idx);
                        setLamp(dl, lp, true);
                        note(dl, lp, tone);
                        dl.sendParticles(ParticleTypes.FLAME, (double)lp.getX() + (double)0.5F, (double)lp.getY() + 1.2, (double)lp.getZ() + (double)0.5F, 10, 0.2, 0.3, 0.2, 0.02);
                        Scheduler.later(14, () -> setLamp(dl, lp, false));
                    }

                });
            }

        }
    }

    private static void sequenceTick(ServerLevel dl, Layout.Mech m, CompoundTag d, long t) {
    }

    private static void note(ServerLevel dl, BlockPos pos, int step) {
        dl.playSound((Player)null, pos, (SoundEvent)SoundEvents.NOTE_BLOCK_CHIME.get(), SoundSource.BLOCKS, 1.2F, (float)Math.pow((double)2.0F, (double)(step * 2 - 6) / (double)12.0F));
    }

    private static void beamTick(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, long t) {
        BlockPos from = pos(m.p().getAsJsonArray("emitter"));
        Direction dir = Direction.byName(s(m.p(), "dir", "north"));
        BlockPos target = pos(m.p().getAsJsonArray("target"));
        boolean done = solved(st, m);
        BlockPos cur = from;
        Vec3 last = Vec3.atCenterOf(from);

        for(int step = 0; step < 96 && dir != null; ++step) {
            cur = cur.relative(dir);
            Vec3 c = Vec3.atCenterOf(cur);

            for(int k = 1; k <= 3; ++k) {
                Vec3 v = last.lerp(c, (double)k / (double)3.0F);
                dl.sendParticles(done ? GOOD : LIGHT, v.x, v.y, v.z, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            }

            last = c;
            if (cur.equals(target)) {
                dl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + (double)0.5F, c.z, 3, 0.2, 0.2, 0.2, 0.02);
                if (!done) {
                    solve(dl, st, s, m);
                }

                return;
            }

            BlockState bs = dl.getBlockState(cur);
            if (bs.is((Block)ModBlocks.MIRROR.get())) {
                boolean slash = MirrorBlock.slash(bs);
                Direction var10000;
                switch (dir) {
                    case NORTH -> var10000 = slash ? Direction.EAST : Direction.WEST;
                    case SOUTH -> var10000 = slash ? Direction.WEST : Direction.EAST;
                    case EAST -> var10000 = slash ? Direction.NORTH : Direction.SOUTH;
                    case WEST -> var10000 = slash ? Direction.SOUTH : Direction.NORTH;
                    default -> var10000 = null;
                }

                dir = var10000;
                dl.sendParticles(ParticleTypes.WAX_OFF, c.x, c.y, c.z, 2, 0.2, 0.2, 0.2, (double)0.0F);
            } else if (!bs.isAir() && bs.isSolidRender(dl, cur)) {
                dl.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 1, 0.1, 0.1, 0.1, 0.01);
                return;
            }
        }

    }

    private static void memoryTick(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive, long t) {
        if (!solved(st, m)) {
            JsonObject p = m.p();
            BlockPos o = pos(p.getAsJsonArray("origin"));
            int w = i(p, "w", 7);
            int h = i(p, "h", 9);
            List<int[]> path = new ArrayList();

            for(JsonElement e : p.getAsJsonArray("path")) {
                path.add(new int[]{e.getAsJsonArray().get(0).getAsInt(), e.getAsJsonArray().get(1).getAsInt()});
            }

            BlockState tile = Run.parse(dl, s(p, "tile", "minecraft:polished_deepslate"));
            BlockState glow = Run.parse(dl, s(p, "glow", "minecraft:sea_lantern"));
            BlockPos start = pos(p.getAsJsonArray("start"));
            long showUntil = d.getLong("showUntil");

            for(ServerPlayer pl : alive) {
                BlockPos under = pl.blockPosition().below();
                if (under.equals(start) && showUntil < t - 100L) {
                    d.putLong("showUntil", t + 80L);
                    showUntil = t + 80L;

                    for(int[] c : path) {
                        dl.setBlock(o.offset(c[0], 0, c[1]), glow, 2);
                    }

                    dl.playSound((Player)null, start, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.4F);
                }

                int dx = under.getX() - o.getX();
                int dz = under.getZ() - o.getZ();
                if (under.getY() == o.getY() && dx >= 0 && dz >= 0 && dx < w && dz < h) {
                    boolean onPath = false;

                    for(int[] c : path) {
                        if (c[0] == dx && c[1] == dz) {
                            onPath = true;
                        }
                    }

                    if (!onPath && dl.getBlockState(under).equals(tile)) {
                        dl.setBlock(under, Blocks.AIR.defaultBlockState(), 2);
                        dl.playSound((Player)null, under, SoundEvents.DEEPSLATE_BREAK, SoundSource.BLOCKS, 1.5F, 0.6F);
                        dl.sendParticles(ParticleTypes.CLOUD, (double)under.getX() + (double)0.5F, (double)under.getY() + (double)0.5F, (double)under.getZ() + (double)0.5F, 10, 0.3, 0.2, 0.3, 0.02);
                        d.putLong("broken_" + dx + "_" + dz, t);
                    }

                    if (dx == ((int[])path.get(path.size() - 1))[0] && dz == ((int[])path.get(path.size() - 1))[1]) {
                        solve(dl, st, s, m);
                    }
                }
            }

            if (showUntil > 0L && t >= showUntil && t < showUntil + 6L) {
                for(int[] c : path) {
                    dl.setBlock(o.offset(c[0], 0, c[1]), tile, 2);
                }
            }

            for(String k : List.copyOf(d.getAllKeys())) {
                if (k.startsWith("broken_") && t - d.getLong(k) >= 80L) {
                    String[] xz = k.substring(7).split("_");
                    dl.setBlock(o.offset(Integer.parseInt(xz[0]), 0, Integer.parseInt(xz[1])), tile, 2);
                    d.remove(k);
                }
            }

        }
    }

    private static void braziersTick(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, long t) {
        if (!solved(st, m)) {
            List<BlockPos> list = positions(m.p(), "braziers");
            int life = i(m.p(), "burn", 160);
            int lit = 0;

            for(BlockPos b : list) {
                BlockState bs = dl.getBlockState(b);
                if (bs.getBlock() instanceof CampfireBlock && (Boolean)bs.getValue(CampfireBlock.LIT)) {
                    if (t - d.getLong(b.toShortString()) > (long)life) {
                        setFire(dl, b, false);
                        dl.playSound((Player)null, b, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8F, 1.0F);
                    } else {
                        ++lit;
                    }
                }
            }

            if (lit >= list.size()) {
                solve(dl, st, s, m);
            }

        }
    }

    private static void areaTick(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive) {
        if (!solved(st, m)) {
            Layout.Box b = box(m.p(), "box");
            if (b != null) {
                int need = i(m.p(), "need", 1);
                int in = 0;

                for(ServerPlayer p : alive) {
                    if (b.contains(p.position())) {
                        ++in;
                    }
                }

                if (in >= need) {
                    solve(dl, st, s, m);
                }

            }
        }
    }

    private static void pulseTrap(ServerLevel dl, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive, long t) {
        JsonObject p = m.p();
        Layout.Box b = box(p, "box");
        if (b != null) {
            int period = i(p, "period", 60);
            int offset = i(p, "offset", 0);
            long phase = (t + (long)offset) % (long)period;
            boolean flames = m.type().equals("flames");
            if (phase >= (long)(period - 20)) {
                for(int k = 0; k < 6; ++k) {
                    double x = (double)b.x1() + dl.random.nextDouble() * (double)(b.x2() - b.x1() + 1);
                    double z = (double)b.z1() + dl.random.nextDouble() * (double)(b.z2() - b.z1() + 1);
                    dl.sendParticles((ParticleOptions)(flames ? ParticleTypes.SMALL_FLAME : WARN), x, (double)b.y1() + 0.1, z, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
                }
            }

            if (phase < 5L) {
                ParticleOptions fx = flames ? ParticleTypes.FLAME : (m.type().equals("crusher") ? ParticleTypes.CLOUD : ParticleTypes.CRIT);

                for(int k = 0; k < 14; ++k) {
                    double x = (double)b.x1() + dl.random.nextDouble() * (double)(b.x2() - b.x1() + 1);
                    double z = (double)b.z1() + dl.random.nextDouble() * (double)(b.z2() - b.z1() + 1);
                    dl.sendParticles(fx, x, (double)b.y1() + 0.3 + dl.random.nextDouble() * (flames ? (double)2.5F : (double)0.5F), z, 1, (double)0.0F, flames ? 0.1 : (double)0.0F, (double)0.0F, 0.02);
                }

                if (phase == 0L) {
                    Vec3 c = b.center();
                    SoundEvent snd = flames ? SoundEvents.BLAZE_SHOOT : (m.type().equals("crusher") ? SoundEvents.ANVIL_LAND : SoundEvents.TRIDENT_HIT_GROUND);
                    dl.playSound((Player)null, c.x, c.y, c.z, snd, SoundSource.BLOCKS, 1.0F, 0.8F);
                    float dmg = f(p, "dmg", 0.25F);

                    for(ServerPlayer pl : alive) {
                        if (b.aabb().inflate((double)0.0F, (double)1.0F, (double)0.0F).contains(pl.position())) {
                            pl.hurt(pl.damageSources().magic(), pl.getMaxHealth() * dmg);
                            if (flames) {
                                pl.setSecondsOnFire(3);
                            }

                            if (m.type().equals("spikes")) {
                                pl.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                            }
                        }
                    }
                }

            }
        }
    }

    private static void bladesTick(ServerLevel dl, Layout.Mech m, List<ServerPlayer> alive, long t) {
        JsonObject p = m.p();
        Layout.Box b = box(p, "box");
        if (b != null) {
            boolean alongX = s(p, "axis", "x").equals("x");
            int period = i(p, "period", 50);
            int offset = i(p, "offset", 0);
            double ph = (double)((t + (long)offset) % (long)period) / (double)period;
            double k = (double)0.5F - (double)0.5F * Math.cos(ph * Math.PI * (double)2.0F);
            double pos = alongX ? (double)b.x1() + k * (double)(b.x2() - b.x1() + 1) : (double)b.z1() + k * (double)(b.z2() - b.z1() + 1);

            for(int y = b.y1(); y <= b.y2(); ++y) {
                for(int s = 0; s < 4; ++s) {
                    double o = alongX ? (double)b.z1() + dl.random.nextDouble() * (double)(b.z2() - b.z1() + 1) : (double)b.x1() + dl.random.nextDouble() * (double)(b.x2() - b.x1() + 1);
                    dl.sendParticles(ParticleTypes.CRIT, alongX ? pos : o, (double)y + (double)0.5F, alongX ? o : pos, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
                }
            }

            if (t % 20L == 0L) {
                Vec3 c = b.center();
                dl.playSound((Player)null, c.x, c.y + (double)1.0F, c.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.BLOCKS, 0.7F, 0.6F);
            }

            float dmg = f(p, "dmg", 0.3F);

            for(ServerPlayer pl : alive) {
                if (b.aabb().contains(pl.position())) {
                    double my = alongX ? pl.getX() : pl.getZ();
                    if (Math.abs(my - pos) < 0.7 && !pl.getPersistentData().contains("litrpg_blade_" + m.id()) || Math.abs(my - pos) < 0.7 && t - pl.getPersistentData().getLong("litrpg_blade_" + m.id()) > 15L) {
                        pl.getPersistentData().putLong("litrpg_blade_" + m.id(), t);
                        pl.hurt(pl.damageSources().magic(), pl.getMaxHealth() * dmg);
                        Vec3 push = alongX ? new Vec3(Math.signum(Math.sin(ph * Math.PI * (double)2.0F)), 0.3, (double)0.0F) : new Vec3((double)0.0F, 0.3, Math.signum(Math.sin(ph * Math.PI * (double)2.0F)));
                        pl.push(push.x * 0.8, push.y, push.z * 0.8);
                        pl.hurtMarked = true;
                    }
                }
            }

        }
    }

    private static void arrowsTick(ServerLevel dl, Layout.Mech m, List<ServerPlayer> alive, long t) {
        JsonObject p = m.p();
        int period = i(p, "period", 40);
        int offset = i(p, "offset", 0);
        if ((t + (long)offset) % (long)period == 0L) {
            Direction dir = Direction.byName(s(p, "dir", "east"));
            if (dir != null) {
                for(BlockPos sp : positions(p, "spots")) {
                    Vec3 from = Vec3.atCenterOf(sp).add((double)dir.getStepX() * 0.7, (double)0.0F, (double)dir.getStepZ() * 0.7);
                    Arrow a = new Arrow(dl, from.x, from.y, from.z);
                    a.shoot((double)dir.getStepX(), 0.02, (double)dir.getStepZ(), 1.6F, 1.5F);
                    a.setBaseDamage((double)2.0F + (double)Run.levelAt(dl, sp) * (double)0.25F);
                    a.pickup = Pickup.DISALLOWED;
                    dl.addFreshEntity(a);
                    dl.playSound((Player)null, sp, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 0.7F, 1.2F);
                }

            }
        }
    }

    private static void collapseTick(ServerLevel dl, RunState st, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive, long t) {
        Layout.Box b = box(m.p(), "box");
        if (b != null) {
            BlockState tile = Run.parse(dl, s(m.p(), "tile", "minecraft:cracked_stone_bricks"));

            for(ServerPlayer pl : alive) {
                BlockPos under = pl.blockPosition().below();
                if (b.contains(under) && dl.getBlockState(under).equals(tile)) {
                    int var10000 = under.getX();
                    String k = "c_" + var10000 + "_" + under.getY() + "_" + under.getZ();
                    if (!d.contains(k)) {
                        d.putLong(k, t);
                        dl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, tile), (double)under.getX() + (double)0.5F, (double)(under.getY() + 1), (double)under.getZ() + (double)0.5F, 12, 0.3, 0.05, 0.3, 0.05);
                        dl.playSound((Player)null, under, SoundEvents.GRAVEL_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
                    }
                }
            }

            for(String k : List.copyOf(d.getAllKeys())) {
                if (k.startsWith("c_")) {
                    String[] c = k.substring(2).split("_");
                    BlockPos bp = new BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
                    long age = t - d.getLong(k);
                    if (age >= 20L && age < 25L && dl.getBlockState(bp).equals(tile)) {
                        dl.setBlock(bp, Blocks.AIR.defaultBlockState(), 2);
                        dl.playSound((Player)null, bp, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.5F);
                    }

                    if (age >= 120L) {
                        dl.setBlock(bp, tile, 2);
                        d.remove(k);
                    }
                }
            }

            st.setDirty();
        }
    }

    private static void restoreFloor(ServerLevel dl, RunState st, Layout.Mech m) {
        CompoundTag d = st.mech(m.id());
        JsonObject p = m.p();
        if (m.type().equals("collapse")) {
            Layout.Box b = box(p, "box");
            BlockState tile = Run.parse(dl, s(p, "tile", "minecraft:cracked_stone_bricks"));

            for(String k : d.getAllKeys()) {
                if (k.startsWith("c_")) {
                    String[] c = k.substring(2).split("_");
                    dl.setBlock(new BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])), tile, 2);
                }
            }

            if (b != null) {
                for(BlockPos bp : b.blocks()) {
                    if (dl.getBlockState(bp).isAir() && bp.getY() == b.y1()) {
                        dl.setBlock(bp, tile, 2);
                    }
                }
            }
        } else {
            BlockPos o = pos(p.getAsJsonArray("origin"));
            BlockState tile = Run.parse(dl, s(p, "tile", "minecraft:polished_deepslate"));

            for(int x = 0; x < i(p, "w", 7); ++x) {
                for(int z = 0; z < i(p, "h", 9); ++z) {
                    dl.setBlock(o.offset(x, 0, z), tile, 2);
                }
            }
        }

    }

    private static void gasTick(ServerLevel dl, Layout.Mech m, List<ServerPlayer> alive, long t) {
        Layout.Box b = box(m.p(), "box");
        if (b != null) {
            for(int k = 0; k < 10; ++k) {
                double x = (double)b.x1() + dl.random.nextDouble() * (double)(b.x2() - b.x1() + 1);
                double z = (double)b.z1() + dl.random.nextDouble() * (double)(b.z2() - b.z1() + 1);
                dl.sendParticles(ParticleTypes.ENTITY_EFFECT, x, (double)b.y1() + dl.random.nextDouble() * (double)2.0F, z, 0, 0.3, 0.8, 0.2, (double)1.0F);
            }

            if (t % 20L == 0L) {
                List<Layout.Box> safe = new ArrayList();
                if (m.p().has("safe")) {
                    for(JsonElement e : m.p().getAsJsonArray("safe")) {
                        safe.add(Layout.Box.of(e.getAsJsonArray()));
                    }
                }

                for(ServerPlayer pl : alive) {
                    if (b.contains(pl.position())) {
                        boolean ok = false;

                        for(Layout.Box sb : safe) {
                            if (sb.contains(pl.position())) {
                                ok = true;
                            }
                        }

                        if (!ok) {
                            pl.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                            pl.hurt(pl.damageSources().magic(), pl.getMaxHealth() * f(m.p(), "dmg", 0.04F));
                        }
                    }
                }

            }
        }
    }

    private static void currentTick(ServerLevel dl, Layout.Mech m, List<ServerPlayer> alive, long t) {
        Layout.Box b = box(m.p(), "box");
        if (b != null) {
            Direction dir = Direction.byName(m.p().has("dir") ? m.p().get("dir").getAsString() : "east");
            if (dir == null) {
                dir = Direction.EAST;
            }

            double force = (double)f(m.p(), "force", 0.1F);
            if (t % 4L == 0L) {
                for(int k = 0; k < 6; ++k) {
                    double x = (double)b.x1() + dl.random.nextDouble() * (double)(b.x2() - b.x1() + 1);
                    double z = (double)b.z1() + dl.random.nextDouble() * (double)(b.z2() - b.z1() + 1);
                    dl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, x, (double)b.y1() + 0.2, z, 0, (double)dir.getStepX(), 0.05, (double)dir.getStepZ(), 0.6);
                    dl.sendParticles(ParticleTypes.SPLASH, x, (double)b.y1() + 0.1, z, 1, 0.2, (double)0.0F, 0.2, (double)0.0F);
                }
            }

            for(ServerPlayer pl : alive) {
                if (b.contains(pl.position()) && (!pl.isShiftKeyDown() || !pl.onGround() || !(dl.random.nextFloat() < 0.5F))) {
                    pl.push((double)dir.getStepX() * force, (double)0.0F, (double)dir.getStepZ() * force);
                    pl.hurtMarked = true;
                }
            }

        }
    }

    private static void portalTick(ServerLevel dl, RunState st, Layout.Stage s, Layout.Mech m, CompoundTag d, List<ServerPlayer> alive) {
        Layout.Box b = box(m.p(), "box");
        if (b != null && m.p().has("to")) {
            JsonArray to = m.p().getAsJsonArray("to");
            double tx = to.get(0).getAsDouble() + (double)0.5F;
            double ty = to.get(1).getAsDouble();
            double tz = to.get(2).getAsDouble() + (double)0.5F;
            float yaw = to.size() > 3 ? to.get(3).getAsFloat() : 0.0F;

            for(ServerPlayer pl : alive) {
                if (b.aabb().inflate(0.3, (double)0.0F, 0.3).contains(pl.position())) {
                    dl.sendParticles(ParticleTypes.REVERSE_PORTAL, pl.getX(), pl.getY() + (double)1.0F, pl.getZ(), 30, 0.4, 0.8, 0.4, 0.1);
                    pl.teleportTo(dl, tx, ty, tz, yaw, 0.0F);
                    pl.fallDistance = 0.0F;
                    dl.sendParticles(ParticleTypes.REVERSE_PORTAL, tx, ty + (double)1.0F, tz, 30, 0.4, 0.8, 0.4, 0.1);
                    dl.playSound((Player)null, tx, ty, tz, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
                    if (!d.getBoolean("used")) {
                        d.putBoolean("used", true);
                        st.setDirty();

                        for(String pk : strings(m.p(), "spawns")) {
                            Run.spawnPackNow(dl, st, s, pk);
                        }
                    }
                }
            }

        }
    }

    private static void rhythmTick(ServerLevel dl, RunState st, Layout.Mech m, long t) {
        List<String> gates = strings(m.p(), "gates");
        int period = i(m.p(), "period", 120);
        int on = i(m.p(), "on", 60);

        for(int k = 0; k < gates.size(); ++k) {
            long ph = (t + (long)k * (long)period / (long)gates.size()) % (long)period;
            boolean bridge = ph < (long)on;
            Run.gate(dl.getServer(), (String)gates.get(k), !bridge);
        }

    }

    private static void setLever(ServerLevel dl, BlockPos pos, boolean on) {
        BlockState bs = dl.getBlockState(pos);
        if (bs.getBlock() instanceof LeverBlock && (Boolean)bs.getValue(LeverBlock.POWERED) != on) {
            dl.setBlock(pos, (BlockState)bs.setValue(LeverBlock.POWERED, on), 3);
            dl.playSound((Player)null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5F, on ? 0.6F : 0.5F);
        }

    }

    private static void setLamp(ServerLevel dl, BlockPos pos, boolean on) {
        BlockState bs = dl.getBlockState(pos);
        if (bs.getBlock() instanceof RedstoneLampBlock) {
            dl.setBlock(pos, (BlockState)bs.setValue(RedstoneLampBlock.LIT, on), 2);
        } else if (bs.getBlock() instanceof CampfireBlock) {
            setFire(dl, pos, on);
        }

    }

    private static void setFire(ServerLevel dl, BlockPos pos, boolean on) {
        BlockState bs = dl.getBlockState(pos);
        if (bs.getBlock() instanceof CampfireBlock && (Boolean)bs.getValue(CampfireBlock.LIT) != on) {
            dl.setBlock(pos, (BlockState)bs.setValue(CampfireBlock.LIT, on), 3);
        }

    }

    private static void setWater(ServerLevel dl, Layout.Mech m, int level) {
        Layout.Box b = box(m.p(), "water");
        if (b != null) {
            int step = i(m.p(), "step", 2);
            int top = b.y1() + level * step;

            for(BlockPos bp : b.blocks()) {
                BlockState bs = dl.getBlockState(bp);
                boolean want = bp.getY() < top;
                if (want && bs.isAir()) {
                    dl.setBlock(bp, Blocks.WATER.defaultBlockState(), 2);
                } else if (!want && bs.is(Blocks.WATER)) {
                    dl.setBlock(bp, Blocks.AIR.defaultBlockState(), 2);
                }
            }

        }
    }
}
