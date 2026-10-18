package com.andrey.litrpg.world.map;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «Мир Системы»: территории народов, рельеф, вода, города, дороги, плитки
 * построек. Данные — /litrpg_world.json (создаёт tools/gen_world.py и
 * tools/gen_mapbuild.py). Формулы ТОЧНО повторяют tools/worldmath.py.
 * <p>
 * Тяжёлые поля (высота, «вес управления», территория, вода) считаются
 * один раз сеткой 8×8 блоков и дальше берутся билинейно.
 */
public final class WorldMap {
    public static final int CELL = 8;
    public static final int HALF = 5120;
    public static final int N = HALF * 2 / CELL + 1;

    public record Profile(double base, double amp, double cell, double ridge, double tame) {}

    public static final class Territory {
        public String id, name, faction;
        public int color, levelBonus;
        public boolean cold;
        public double[][] seeds;
        public Profile profile;
        public String[] biomes;
        public int[] weights;
        public int weightSum;
    }

    public record City(String id, String name, String faction, int x, int z, int y, int r, int level, String guard, String desc,
                       int core, String biome) {}

    public record River(double[][] points, double width, double[] box) {}

    public record Road(String name, String style, double[][] points) {}

    public record Poi(String type, String name, String id, int x, int z, int y, int r, String extra) {}

    public record Tile(String dim, int cx, int cz, String template, int y) {}

    private static WorldMap instance;

    public final List<Territory> territories = new ArrayList<>();
    public Profile wild;
    public double wall0, wall1;
    public final List<double[]> seas = new ArrayList<>(), lakes = new ArrayList<>(), flats = new ArrayList<>();
    public final List<River> rivers = new ArrayList<>();
    public final List<City> cities = new ArrayList<>();
    public final List<Road> roads = new ArrayList<>();
    public final List<Poi> pois = new ArrayList<>();
    public final Map<Long, Tile> overworldTiles = new HashMap<>(), netherTiles = new HashMap<>();
    public double levelSlope = 40;
    public int levelMax = 60;
    /** Площадки Преисподней: x, z, радиус, высота пола. */
    public final List<double[]> netherFlats = new ArrayList<>();

    // сетка
    private float[] gridH, gridW;
    private byte[] gridT, gridWater;

    public static WorldMap get() {
        if (instance == null) {
            synchronized (WorldMap.class) {
                if (instance == null) {
                    WorldMap m = new WorldMap();
                    m.load();
                    long t0 = System.currentTimeMillis();
                    m.buildGrid();
                    LitRPG.LOGGER.info("World map grid built in {} ms", System.currentTimeMillis() - t0);
                    instance = m;
                }
            }
        }
        return instance;
    }

    // ================= шум (как в worldmath.py) =================

    static double h32(long ix, long iz, long seed) {
        int n = (int) (ix * 374761393L + iz * 668265263L + seed * 1442695041L);
        n = (n ^ (n >>> 13)) * 1274126177;
        n = n ^ (n >>> 16);
        return (n & 0xFFFF) / 65535.0;
    }

    static double smooth(double t) {
        return t * t * (3 - 2 * t);
    }

    public static double vn(double x, double z, double cell, int seed) {
        double gx = x / cell, gz = z / cell;
        double x0 = Math.floor(gx), z0 = Math.floor(gz);
        double fx = smooth(gx - x0), fz = smooth(gz - z0);
        long ix = (long) x0, iz = (long) z0;
        double a = h32(ix, iz, seed), b = h32(ix + 1, iz, seed);
        double c = h32(ix, iz + 1, seed), d = h32(ix + 1, iz + 1, seed);
        return (a + (b - a) * fx) * (1 - fz) + (c + (d - c) * fx) * fz;
    }

    static double fbm(double x, double z, double cell, int seed) {
        return vn(x, z, cell, seed) * 0.5 + vn(x, z, cell / 2.3, seed + 1) * 0.25 + vn(x, z, cell / 5.5, seed + 2) * 0.15
                + vn(x, z, cell / 13.0, seed + 3) * 0.1;
    }

    public static double sstep(double e0, double e1, double x) {
        if (e0 == e1) return x >= e1 ? 1 : 0;
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    public static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double l2 = dx * dx + dz * dz;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l2));
        double cx = ax + dx * t, cz = az + dz * t;
        return Math.hypot(px - cx, pz - cz);
    }

    public static double polyDist(double px, double pz, double[][] pts) {
        double best = 1e18;
        for (int i = 0; i < pts.length - 1; i++) {
            double d = segDist(px, pz, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1]);
            if (d < best) best = d;
        }
        return best;
    }

    // ================= расчёт (как в worldmath.World) =================

    private double[] warp(double x, double z) {
        double wx = x + 240 * (vn(x, z, 1000, 11) * 2 - 1) + 60 * (vn(x, z, 180, 13) * 2 - 1);
        double wz = z + 240 * (vn(x, z, 1000, 12) * 2 - 1) + 60 * (vn(x, z, 180, 14) * 2 - 1);
        return new double[]{wx, wz};
    }

    private double[] scores(double x, double z) {
        double[] w = warp(x, z);
        double[] out = new double[territories.size()];
        for (int i = 0; i < out.length; i++) {
            double s = 1e9;
            for (double[] sd : territories.get(i).seeds) {
                double v = Math.hypot(w[0] - sd[0], w[1] - sd[1]) / sd[2];
                if (v < s) s = v;
            }
            out[i] = s;
        }
        return out;
    }

    static double profHeight(Profile p, double x, double z, int seed) {
        double h = p.base + p.amp * (fbm(x, z, p.cell, seed) * 2 - 1);
        if (p.ridge > 0) {
            double r = 1 - Math.abs(vn(x, z, p.cell * 0.8, seed + 5) * 2 - 1);
            double r2 = 1 - Math.abs(vn(x, z, p.cell * 0.35, seed + 6) * 2 - 1);
            h += p.ridge * (r * r * r * 110 + r2 * r2 * 34);
        }
        return h;
    }

    /** {H, W, территория(-1 = дикие), вода(0 нет,1 река,2 озеро,3 море,4 берег)} */
    public double[] compute(double x, double z) {
        double[] sc = scores(x, z);
        double[] ws = new double[sc.length];
        double ww = Math.exp(-7 * 1.0);
        double tot = ww;
        int best = 0;
        for (int i = 0; i < sc.length; i++) {
            ws[i] = Math.exp(-7 * sc[i]);
            tot += ws[i];
            if (sc[i] < sc[best]) best = i;
        }
        double H = 0, W = 0;
        for (int i = 0; i < sc.length; i++) {
            double w = ws[i] / tot;
            if (w < 0.002) continue;
            H += w * profHeight(territories.get(i).profile, x, z, 100 + i * 17);
            W += w * territories.get(i).profile.tame;
        }
        double w0 = ww / tot;
        H += w0 * profHeight(wild, x, z, 977);
        W += w0 * wild.tame;
        int terr = sc.length > 0 && sc[best] < 1.0 ? best : -1;
        int water = 0;
        double shore = 0;
        for (int kind = 3; kind >= 2; kind--) {
            for (double[] l : kind == 3 ? seas : lakes) {
                double d = Math.hypot(x - l[0], z - l[1]) / l[2];
                d *= 1 + 0.18 * (vn(x, z, l[2] * 0.6, 31) * 2 - 1);
                if (d < 1.35) {
                    double k = sstep(1.35, 0.9, d);
                    double target = l[3] + (64.5 - l[3]) * Math.pow(Math.min(1.0, d), 2);
                    H = lerp(H, target, k);
                    W = Math.max(W, 0.92 * k);
                    if (d < 1.0) water = kind;
                    shore = Math.max(shore, k);
                }
            }
        }
        for (River rv : rivers) {
            if (x < rv.box[0] - 80 || x > rv.box[2] + 80 || z < rv.box[1] - 80 || z > rv.box[3] + 80) continue;
            double dr = polyDist(x, z, rv.points);
            double half = rv.width / 2;
            if (dr < half + 40) {
                double core = sstep(half + 40, half, dr);
                double t2 = dr < half ? 55 + 8 * Math.pow(dr / half, 2) : 63 + (dr - half) * 0.35;
                H = lerp(H, Math.min(H, t2), core);
                W = Math.max(W, 0.92 * core);
                if (dr < half && water == 0) water = 1;
            }
        }
        for (double[] f : flats) {
            double dd = Math.hypot(x - f[0], z - f[1]);
            if (dd < f[2] + 48) {
                double k = sstep(f[2] + 48, f[2], dd);
                H = lerp(H, f[3], k);
                W = lerp(W, 1.0, k);
                if (dd < f[2]) water = 0;
            }
        }
        double c = Math.max(Math.abs(x), Math.abs(z));
        if (c > wall0) {
            double k = sstep(wall0, wall1, c);
            double rr = 1 - Math.abs(vn(x, z, 160, 78) * 2 - 1);
            double wallH = 205 + 55 * vn(x, z, 140, 77) + 40 * rr * rr;
            H = lerp(H, wallH, k);
            W = lerp(W, 0.96, k);
            if (k > 0.3) water = 0;
        }
        if (water == 0 && shore > 0.05 && H < 66) water = 4;
        return new double[]{H, W, terr, water};
    }

    private void buildGrid() {
        int n = N * N;
        gridH = new float[n];
        gridW = new float[n];
        gridT = new byte[n];
        gridWater = new byte[n];
        // параллельно по строкам: расчёт независимый
        java.util.stream.IntStream.range(0, N).parallel().forEach(row -> {
            double z = -HALF + row * CELL;
            for (int col = 0; col < N; col++) {
                double x = -HALF + col * CELL;
                double[] r = compute(x, z);
                int i = row * N + col;
                gridH[i] = (float) r[0];
                gridW[i] = (float) r[1];
                gridT[i] = (byte) r[2];
                gridWater[i] = (byte) r[3];
            }
        });
    }

    // ================= быстрые запросы =================

    private static int clampIdx(int v) {
        return v < 0 ? 0 : Math.min(N - 1, v);
    }

    /** Билинейно: высота и вес управления рельефом. */
    public void sample(double x, double z, double[] out) {
        double gx = (x + HALF) / CELL, gz = (z + HALF) / CELL;
        int x0 = (int) Math.floor(gx), z0 = (int) Math.floor(gz);
        double fx = gx - x0, fz = gz - z0;
        int a = clampIdx(z0) * N + clampIdx(x0), b = clampIdx(z0) * N + clampIdx(x0 + 1);
        int c = clampIdx(z0 + 1) * N + clampIdx(x0), d = clampIdx(z0 + 1) * N + clampIdx(x0 + 1);
        out[0] = (gridH[a] + (gridH[b] - gridH[a]) * fx) * (1 - fz) + (gridH[c] + (gridH[d] - gridH[c]) * fx) * fz;
        out[1] = (gridW[a] + (gridW[b] - gridW[a]) * fx) * (1 - fz) + (gridW[c] + (gridW[d] - gridW[c]) * fx) * fz;
    }

    public double height(double x, double z) {
        double[] o = new double[2];
        sample(x, z, o);
        return o[0];
    }

    private int idx(double x, double z) {
        int col = clampIdx((int) Math.round((x + HALF) / CELL));
        int row = clampIdx((int) Math.round((z + HALF) / CELL));
        return row * N + col;
    }

    public int territoryAt(double x, double z) {
        return gridT[idx(x, z)];
    }

    public int waterAt(double x, double z) {
        return gridWater[idx(x, z)];
    }

    public Territory territory(int i) {
        return i >= 0 && i < territories.size() ? territories.get(i) : null;
    }

    /** Город, в «ядре» которого точка (там городской биом). */
    public City cityCoreAt(double x, double z) {
        for (City c : cities) {
            double dx = x - c.x, dz = z - c.z;
            if (dx * dx + dz * dz < (double) c.core * c.core) return c;
        }
        return null;
    }

    public City cityAt(double x, double z) {
        for (City c : cities) {
            double dx = x - c.x, dz = z - c.z;
            if (dx * dx + dz * dz < (double) c.r * c.r) return c;
        }
        return null;
    }

    /** Уровень монстров: от ближайшего города, плюс опасность земель. */
    public int levelAt(double x, double z) {
        double best = 1e9;
        for (City c : cities) {
            double v = c.level + Math.hypot(x - c.x, z - c.z) / levelSlope;
            if (v < best) best = v;
        }
        int t = territoryAt(x, z);
        if (t >= 0) best += territories.get(t).levelBonus;
        double c = Math.max(Math.abs(x), Math.abs(z));
        if (c > wall0) best += 20 * sstep(wall0, wall1, c);
        return (int) Math.max(1, Math.min(levelMax, Math.round(best)));
    }

    /** Насколько «опасно» место (0..1) — для плотности монстров. */
    public double danger(double x, double z) {
        return Math.max(0, Math.min(1, (levelAt(x, z) - 6) / 40.0));
    }

    public static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
    }

    public Tile tile(boolean nether, int cx, int cz) {
        return (nether ? netherTiles : overworldTiles).get(key(cx, cz));
    }

    public Poi poi(String id) {
        for (Poi p : pois) if (id.equals(p.id)) return p;
        return null;
    }

    // ================= загрузка =================

    private static double[][] points(JsonArray a) {
        double[][] out = new double[a.size()][];
        for (int i = 0; i < a.size(); i++) {
            JsonArray p = a.get(i).getAsJsonArray();
            out[i] = new double[p.size()];
            for (int j = 0; j < p.size(); j++) out[i][j] = p.get(j).getAsDouble();
        }
        return out;
    }

    private static Profile profile(JsonObject o) {
        return new Profile(o.get("base").getAsDouble(), o.get("amp").getAsDouble(), o.get("cell").getAsDouble(),
                o.get("ridge").getAsDouble(), o.get("tame").getAsDouble());
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : def;
    }

    private void load() {
        try (var in = LitRPG.class.getResourceAsStream("/litrpg_world.json")) {
            if (in == null) throw new IllegalStateException("litrpg_world.json not found");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray wall = root.getAsJsonArray("wall");
            wall0 = wall.get(0).getAsDouble();
            wall1 = wall.get(1).getAsDouble();
            for (JsonElement el : root.getAsJsonArray("territories")) {
                JsonObject o = el.getAsJsonObject();
                Territory t = new Territory();
                t.id = o.get("id").getAsString();
                t.name = o.get("name").getAsString();
                t.faction = str(o, "faction", null);
                t.color = o.get("color").getAsInt();
                t.levelBonus = o.has("level_bonus") ? o.get("level_bonus").getAsInt() : 0;
                t.cold = o.has("cold") && o.get("cold").getAsBoolean();
                t.seeds = points(o.getAsJsonArray("seeds"));
                t.profile = profile(o.getAsJsonObject("profile"));
                JsonArray bs = o.getAsJsonArray("biomes");
                t.biomes = new String[bs.size()];
                t.weights = new int[bs.size()];
                for (int i = 0; i < bs.size(); i++) {
                    t.biomes[i] = bs.get(i).getAsJsonArray().get(0).getAsString();
                    t.weights[i] = bs.get(i).getAsJsonArray().get(1).getAsInt();
                    t.weightSum += t.weights[i];
                }
                territories.add(t);
            }
            wild = profile(root.getAsJsonObject("wild").getAsJsonObject("profile"));
            for (double[] s : points(root.getAsJsonArray("seas"))) seas.add(s);
            for (double[] s : points(root.getAsJsonArray("lakes"))) lakes.add(s);
            for (double[] s : points(root.getAsJsonArray("flats"))) flats.add(s);
            for (JsonElement el : root.getAsJsonArray("rivers")) {
                JsonObject o = el.getAsJsonObject();
                double[] box = new double[4];
                JsonArray b = o.getAsJsonArray("box");
                for (int i = 0; i < 4; i++) box[i] = b.get(i).getAsDouble();
                rivers.add(new River(points(o.getAsJsonArray("points")), o.get("width").getAsDouble(), box));
            }
            for (JsonElement el : root.getAsJsonArray("cities")) {
                JsonObject o = el.getAsJsonObject();
                cities.add(new City(o.get("id").getAsString(), o.get("name").getAsString(), str(o, "faction", null),
                        o.get("x").getAsInt(), o.get("z").getAsInt(), o.get("y").getAsInt(), o.get("r").getAsInt(),
                        o.get("level").getAsInt(), str(o, "guard", null), str(o, "desc", ""),
                        o.has("core") ? o.get("core").getAsInt() : o.get("r").getAsInt(), str(o, "biome", "litrpg:city_lands")));
            }
            for (JsonElement el : root.getAsJsonArray("roads")) {
                JsonObject o = el.getAsJsonObject();
                roads.add(new Road(o.get("name").getAsString(), str(o, "style", "dirt"), points(o.getAsJsonArray("points"))));
            }
            for (JsonElement el : root.getAsJsonArray("pois")) {
                JsonObject o = el.getAsJsonObject();
                pois.add(new Poi(o.get("type").getAsString(), str(o, "name", ""), str(o, "id", ""), o.get("x").getAsInt(),
                        o.get("z").getAsInt(), o.has("y") ? o.get("y").getAsInt() : 0, o.has("r") ? o.get("r").getAsInt() : 0,
                        str(o, "extra", "")));
            }
            for (JsonElement el : root.getAsJsonArray("tiles")) {
                JsonObject o = el.getAsJsonObject();
                Tile t = new Tile(o.get("dim").getAsString(), o.get("cx").getAsInt(), o.get("cz").getAsInt(),
                        o.get("template").getAsString(), o.get("y").getAsInt());
                (t.dim.equals("nether") ? netherTiles : overworldTiles).put(key(t.cx, t.cz), t);
            }
            if (root.has("nether") && root.getAsJsonObject("nether").has("flats")) {
                for (double[] s : points(root.getAsJsonObject("nether").getAsJsonArray("flats"))) netherFlats.add(s);
            }
            JsonObject lv = root.getAsJsonObject("levels");
            levelSlope = lv.get("slope").getAsDouble();
            levelMax = lv.get("max").getAsInt();
            LitRPG.LOGGER.info("World map: {} territories, {} cities, {} pois, {} tiles", territories.size(), cities.size(),
                    pois.size(), overworldTiles.size() + netherTiles.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load world map", e);
        }
    }
}
