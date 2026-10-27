/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Layout {
    private static Layout INSTANCE;
    public final Spot lobbySpawn;
    public final Box lobby;
    public final Box lobbyStart;
    public final String lobbyGate;
    public final List<Gate> lobbyGates;
    public final List<Lore> lobbyLore;
    public final List<Stage> stages;
    private final Map<Long, Tile> tiles = new HashMap();

    private Layout(JsonObject o) {
        JsonObject lb = o.getAsJsonObject("lobby");
        this.lobbySpawn = Layout.Spot.of(lb.getAsJsonArray("spawn"));
        this.lobby = Layout.Box.of(lb.getAsJsonArray("box"));
        this.lobbyStart = Layout.Box.of(lb.getAsJsonArray("start"));
        this.lobbyGate = lb.has("gate") ? lb.get("gate").getAsString() : "";
        this.lobbyGates = gates(lb.getAsJsonArray("gates"));
        this.lobbyLore = lore(lb.getAsJsonArray("lore"));
        List<Stage> st = new ArrayList();

        for(JsonElement e : o.getAsJsonArray("stages")) {
            st.add(stage(e.getAsJsonObject()));
        }

        this.stages = Collections.unmodifiableList(st);

        for(JsonElement e : o.getAsJsonArray("tiles")) {
            JsonArray a = e.getAsJsonArray();
            Tile t = new Tile(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt(), a.get(3).getAsString());
            this.tiles.put(key(t.cx, t.cz), t);
        }

    }

    private static Stage stage(JsonObject o) {
        List<Pack> packs = new ArrayList();

        for(JsonElement e : arr(o, "packs")) {
            JsonObject p = e.getAsJsonObject();
            List<Mob> mobs = new ArrayList();

            for(JsonElement m : p.getAsJsonArray("mobs")) {
                JsonObject mo = m.getAsJsonObject();
                mobs.add(new Mob(mo.get("type").getAsString(), Layout.Spot.of(mo.getAsJsonArray("at")), mo.get("lvl").getAsInt(), str(mo, "rank", "NORMAL"), str(mo, "name", ""), str(mo, "role", "")));
            }

            packs.add(new Pack(p.get("id").getAsString(), Layout.Spot.of(p.getAsJsonArray("at")), p.has("r") ? p.get("r").getAsDouble() : (double)18.0F, List.copyOf(mobs), p.has("mini") && p.get("mini").getAsBoolean(), str(p, "loot", ""), p.has("ambush") && p.get("ambush").getAsBoolean()));
        }

        List<Boss> bosses = new ArrayList();
        int order = 0;

        for(JsonElement e : arr(o, "bosses")) {
            JsonObject b = e.getAsJsonObject();
            bosses.add(new Boss(b.get("id").getAsString(), b.get("type").getAsString(), Layout.Spot.of(b.getAsJsonArray("at")), Layout.Box.of(b.getAsJsonArray("arena")), strings(arr(b, "doors")), strings(arr(b, "unlock")), order++, b.has("extra") ? b.getAsJsonObject("extra") : new JsonObject()));
        }

        List<Mech> mechs = new ArrayList();

        for(JsonElement e : arr(o, "mechs")) {
            JsonObject m = e.getAsJsonObject();
            mechs.add(new Mech(m.get("id").getAsString(), m.get("type").getAsString(), m));
        }

        List<Cache> caches = new ArrayList();

        for(JsonElement e : arr(o, "caches")) {
            JsonArray a = e.getAsJsonArray();
            caches.add(new Cache(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()), a.size() > 3 ? a.get(3).getAsInt() : 0));
        }

        int[] lv = new int[]{o.getAsJsonArray("lvl").get(0).getAsInt(), o.getAsJsonArray("lvl").get(1).getAsInt()};
        return new Stage(o.get("n").getAsInt(), o.get("key").getAsString(), o.get("name").getAsString(), str(o, "sub", ""), lv[0], lv[1], Layout.Box.of(o.getAsJsonArray("box")), Layout.Spot.of(o.getAsJsonArray("camp")), Layout.Spot.of(o.getAsJsonArray("trader")), Layout.Box.of(o.getAsJsonArray("exit")), o.has("exitTo") ? Layout.Spot.of(o.getAsJsonArray("exitTo")) : null, List.copyOf(packs), List.copyOf(bosses), gates(arr(o, "gates")), List.copyOf(mechs), List.copyOf(caches), lore(arr(o, "lore")), str(o, "music", ""));
    }

    private static List<Gate> gates(JsonArray a) {
        List<Gate> out = new ArrayList();
        if (a != null) {
            for(JsonElement e : a) {
                JsonObject g = e.getAsJsonObject();
                out.add(new Gate(g.get("id").getAsString(), Layout.Box.of(g.getAsJsonArray("box")), g.get("block").getAsString(), g.has("open") && g.get("open").getAsBoolean()));
            }
        }

        return List.copyOf(out);
    }

    private static List<Lore> lore(JsonArray a) {
        List<Lore> out = new ArrayList();
        if (a != null) {
            for(JsonElement e : a) {
                JsonObject l = e.getAsJsonObject();
                out.add(new Lore(Layout.Box.of(l.getAsJsonArray("box")), str(l, "title", ""), str(l, "text", "")));
            }
        }

        return List.copyOf(out);
    }

    private static JsonArray arr(JsonObject o, String k) {
        return o.has(k) ? o.getAsJsonArray(k) : new JsonArray();
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) ? o.get(k).getAsString() : def;
    }

    private static List<String> strings(JsonArray a) {
        List<String> out = new ArrayList();

        for(JsonElement e : a) {
            out.add(e.getAsString());
        }

        return List.copyOf(out);
    }

    private static long key(int cx, int cz) {
        return (long)cx << 32 ^ (long)cz & 4294967295L;
    }

    public Tile tile(int cx, int cz) {
        return (Tile)this.tiles.get(key(cx, cz));
    }

    public Stage stage(int n) {
        for(Stage s : this.stages) {
            if (s.n == n) {
                return s;
            }
        }

        return null;
    }

    public Stage stageAt(double x, double y, double z) {
        for(Stage s : this.stages) {
            if (s.box.contains(x, y, z)) {
                return s;
            }
        }

        return null;
    }

    public static Layout get() {
        if (INSTANCE == null) {
            try {
                InputStream in = Layout.class.getResourceAsStream("/litrpg_dungeon.json");

                try {
                    if (in == null) {
                        throw new IllegalStateException("litrpg_dungeon.json missing");
                    }

                    INSTANCE = new Layout(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                    LitRPG.LOGGER.info("Dungeon layout: {} stages, {} tiles", INSTANCE.stages.size(), INSTANCE.tiles.size());
                } catch (Throwable var4) {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Throwable var3) {
                            var4.addSuppressed(var3);
                        }
                    }

                    throw var4;
                }

                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
                throw new RuntimeException("Cannot read dungeon layout", e);
            }
        }

        return INSTANCE;
    }

    public static record Box(int x1, int y1, int z1, int x2, int y2, int z2) {
        static Box of(JsonArray a) {
            int x1 = a.get(0).getAsInt();
            int y1 = a.get(1).getAsInt();
            int z1 = a.get(2).getAsInt();
            int x2 = a.get(3).getAsInt();
            int y2 = a.get(4).getAsInt();
            int z2 = a.get(5).getAsInt();
            return new Box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
        }

        public boolean contains(double x, double y, double z) {
            return x >= (double)this.x1 && x < (double)(this.x2 + 1) && y >= (double)this.y1 && y < (double)(this.y2 + 1) && z >= (double)this.z1 && z < (double)(this.z2 + 1);
        }

        public boolean contains(Vec3 v) {
            return this.contains(v.x, v.y, v.z);
        }

        public boolean contains(BlockPos p) {
            return this.contains((double)p.getX() + (double)0.5F, (double)p.getY() + (double)0.5F, (double)p.getZ() + (double)0.5F);
        }

        public AABB aabb() {
            return new AABB((double)this.x1, (double)this.y1, (double)this.z1, (double)(this.x2 + 1), (double)(this.y2 + 1), (double)(this.z2 + 1));
        }

        public Vec3 center() {
            return new Vec3((double)(this.x1 + this.x2 + 1) / (double)2.0F, (double)this.y1, (double)(this.z1 + this.z2 + 1) / (double)2.0F);
        }

        public Iterable<BlockPos> blocks() {
            return BlockPos.betweenClosed(this.x1, this.y1, this.z1, this.x2, this.y2, this.z2);
        }
    }

    public static record Spot(double x, double y, double z, float yaw) {
        static Spot of(JsonArray a) {
            return new Spot(a.get(0).getAsDouble() + (double)0.5F, a.get(1).getAsDouble(), a.get(2).getAsDouble() + (double)0.5F, a.size() > 3 ? a.get(3).getAsFloat() : 0.0F);
        }

        public Vec3 vec() {
            return new Vec3(this.x, this.y, this.z);
        }

        public BlockPos pos() {
            return BlockPos.containing(this.x, this.y, this.z);
        }
    }

    public static record Mob(String type, Spot at, int level, String rank, String name, String role) {
    }

    public static record Pack(String id, Spot at, double r, List<Mob> mobs, boolean miniboss, String loot, boolean ambush) {
    }

    public static record Boss(String id, String type, Spot at, Box arena, List<String> doors, List<String> unlock, int order, JsonObject extra) {
    }

    public static record Gate(String id, Box box, String block, boolean open) {
    }

    public static record Mech(String id, String type, JsonObject p) {
    }

    public static record Lore(Box box, String title, String text) {
    }

    public static record Cache(BlockPos pos, int tier) {
    }

    public static record Stage(int n, String key, String name, String subtitle, int minLevel, int maxLevel, Box box, Spot camp, Spot trader, Box exit, Spot exitTo, List<Pack> packs, List<Boss> bosses, List<Gate> gates, List<Mech> mechs, List<Cache> caches, List<Lore> lore, String music) {
        public Boss boss(String id) {
            for(Boss b : this.bosses) {
                if (b.id.equals(id)) {
                    return b;
                }
            }

            return null;
        }

        public Gate gate(String id) {
            for(Gate g : this.gates) {
                if (g.id.equals(id)) {
                    return g;
                }
            }

            return null;
        }

        public Pack pack(String id) {
            for(Pack p : this.packs) {
                if (p.id.equals(id)) {
                    return p;
                }
            }

            return null;
        }
    }

    public static record Tile(int cx, int cz, int y, String template) {
    }
}
