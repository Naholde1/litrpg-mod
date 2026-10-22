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
        /** Пояс высот рельефа карты, где растёт биом (как на атласе: долы внизу, пики наверху). */
        public int[] hmin, hmax;
        public int weightSum;
        /** Биом берега (пляж земли). */
        public String shore;
    }

    public record City(String id, String name, String faction, int x, int z, int y, int r, int level, String guard, String desc,
                       int core, String biome) {}

    public record River(double[][] points, double width, double[] box) {}

    public record Road(String name, String style, double[][] points) {}

    public record Poi(String type, String name, String id, int x, int z, int y, int r, String extra) {}

    public record Tile(String dim, int cx, int cz, String template, int y) {}

    /** Мастерская в городе: ремесло, кто там работает и где стоит мастер. */
    public record Workshop(String city, String craft, String npc, int x, int y, int z) {}

    private static WorldMap instance;

    public final List<Territory> territories = new ArrayList<>();
    public Profile wild;
    public double wall0, wall1;
    public final List<double[]> seas = new ArrayList<>(), lakes = new ArrayList<>(), flats = new ArrayList<>();
    public final List<River> rivers = new ArrayList<>();
    public final List<City> cities = new ArrayList<>();
    public final List<Road> roads = new ArrayList<>();
    public final List<Poi> pois = new ArrayList<>();
    public final List<Workshop> workshops = new ArrayList<>();
    public final Map<Long, Tile> overworldTiles = new HashMap<>(), netherTiles = new HashMap<>();
    public double levelSlope = 40, levelSafe = 0, levelFar = 1e9, levelFarSlope = 1e9, roadEase = 0, settleEase = 0;
    public int levelMax = 60;
    /** Особые места городов для сюжетов (тайники, двери, места встреч): город → имя → x, y, z. */
    public final Map<String, Map<String, int[]>> anchors = new HashMap<>();
    /** Отрезки дорог по клеткам 64×64 (для зон уровней): x1, z1, x2, z2. */
    private final Map<Long, List<double[]>> roadGrid = new HashMap<>();
    /** Поселения по клеткам 64×64 с запасом в 160 блоков — тихие окрестности. */
    private final Map<Long, List<Poi>> calmGrid = new HashMap<>();
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
            double bl = f.length > 4 ? f[4] : 48;
            double dd = Math.hypot(x - f[0], z - f[1]);
            if (dd < f[2] + bl) {
                double k = sstep(f[2] + bl, f[2], dd);
                // срез полуплоскостью: площадка только там, где (p - центр)·n < d (гавань, берег)
                if (f.length > 7) k *= sstep(f[7] + 5, f[7], (x - f[0]) * f[5] + (z - f[1]) * f[6]);
                if (k <= 0) continue;
                H = lerp(H, f[3], k);
                W = lerp(W, 1.0, k);
                if (dd < f[2] && k > 0.99 && f[3] >= 63) water = 0;
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

    /** Поселения, внутри которых не растёт лес мира (деревья и грибы не лезут во дворы). */
    private static final java.util.Set<String> SETTLEMENTS = java.util.Set.of("village", "hamlet", "farm", "lumber", "fishing", "caravan", "outpost", "camp",
            "obelisk", "magetower", "nest", "witch", "arena", "star");
    /** Места литРПГ: под ними поляна (без леса), но не «обжитой» биом. */
    public static final java.util.Set<String> LORE = java.util.Set.of("obelisk", "magetower", "nest", "witch", "arena", "star");
    private final Map<Long, List<Poi>> settleGrid = new HashMap<>();

    /** Поселение, на площадке которого точка (или null). */
    public Poi settlementAt(double x, double z) {
        List<Poi> l = settleGrid.get(key((int) Math.floor(x) >> 6, (int) Math.floor(z) >> 6));
        if (l == null) return null;
        for (Poi p : l) {
            double dx = x - p.x(), dz = z - p.z(), r = p.r() + 4;
            if (dx * dx + dz * dz < r * r) return p;
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

    public City city(String id) {
        for (City c : cities) if (c.id.equals(id)) return c;
        return null;
    }

    public List<Workshop> workshopsOf(String cityId) {
        List<Workshop> out = new ArrayList<>();
        for (Workshop w : workshops) if (w.city.equals(cityId)) out.add(w);
        return out;
    }

    /** Особое место сюжета города или null. */
    public int[] anchor(String city, String name) {
        Map<String, int[]> m = anchors.get(city);
        return m == null ? null : m.get(name);
    }

    /** Уровень от города без учёта дорог: пояс безопасности за стенами, потом рост, вдали — быстрее. */
    public double cityLevel(City c, double dist) {
        double d = Math.max(0, dist - c.r - levelSafe);
        return c.level + d / levelSlope + Math.max(0, d - levelFar) / levelFarSlope;
    }

    /** Насколько тише вдоль трактов (0..roadEase): у самой дороги рост уровня вдвое медленнее. */
    private double roadCalm(double x, double z) {
        List<double[]> segs = roadGrid.get(key((int) Math.floor(x) >> 6, (int) Math.floor(z) >> 6));
        if (segs == null) return 0;
        double best = 1e9;
        for (double[] s : segs) {
            double dx = s[2] - s[0], dz = s[3] - s[1];
            double l2 = dx * dx + dz * dz;
            double u = l2 < 1e-6 ? 0 : Math.max(0, Math.min(1, ((x - s[0]) * dx + (z - s[1]) * dz) / l2));
            best = Math.min(best, Math.hypot(x - s[0] - dx * u, z - s[1] - dz * u));
        }
        return roadEase * (1 - sstep(16, 64, best));
    }

    /** Насколько тише у поселений (0..settleEase). */
    private double settleCalm(double x, double z) {
        List<Poi> l = calmGrid.get(key((int) Math.floor(x) >> 6, (int) Math.floor(z) >> 6));
        if (l == null) return 0;
        double best = 0;
        for (Poi p : l) {
            double d = Math.hypot(x - p.x(), z - p.z()) - p.r();
            best = Math.max(best, settleEase * (1 - sstep(24, 150, d)));
        }
        return best;
    }

    /** Уровень монстров: от ближайшего города (вдоль дорог и у поселений — ниже), плюс опасность земель. */
    public int levelAt(double x, double z) {
        double best = 1e9;
        City near = null;
        for (City c : cities) {
            double v = cityLevel(c, Math.hypot(x - c.x, z - c.z));
            if (v < best) {
                best = v;
                near = c;
            }
        }
        if (near != null && best > near.level) {
            double calm = Math.max(roadCalm(x, z), settleCalm(x, z));
            best -= (best - near.level) * calm;
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
                t.hmin = new int[bs.size()];
                t.hmax = new int[bs.size()];
                for (int i = 0; i < bs.size(); i++) {
                    JsonArray b = bs.get(i).getAsJsonArray();
                    t.biomes[i] = b.get(0).getAsString();
                    t.weights[i] = b.get(1).getAsInt();
                    t.hmin[i] = b.size() > 2 ? b.get(2).getAsInt() : -999;
                    t.hmax[i] = b.size() > 3 ? b.get(3).getAsInt() : 999;
                    t.weightSum += t.weights[i];
                }
                t.shore = str(o, "shore", null);
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
            if (root.has("workshops")) {
                for (JsonElement el : root.getAsJsonArray("workshops")) {
                    JsonObject o = el.getAsJsonObject();
                    workshops.add(new Workshop(o.get("city").getAsString(), o.get("craft").getAsString(), o.get("npc").getAsString(),
                            o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()));
                }
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
            for (Poi p : pois) {
                if (p.r() <= 0 || !SETTLEMENTS.contains(p.type())) continue;
                int rr = p.r() + 8;
                for (int gx = (p.x() - rr) >> 6; gx <= (p.x() + rr) >> 6; gx++)
                    for (int gz = (p.z() - rr) >> 6; gz <= (p.z() + rr) >> 6; gz++)
                        settleGrid.computeIfAbsent(key(gx, gz), k -> new ArrayList<>()).add(p);
            }
            for (Poi p : pois) {
                if (p.r() <= 0 || !SETTLEMENTS.contains(p.type()) && !p.type().equals("hamlet")) continue;
                int rr = p.r() + 160;
                for (int gx = (p.x() - rr) >> 6; gx <= (p.x() + rr) >> 6; gx++)
                    for (int gz = (p.z() - rr) >> 6; gz <= (p.z() + rr) >> 6; gz++)
                        calmGrid.computeIfAbsent(key(gx, gz), k -> new ArrayList<>()).add(p);
            }
            for (Road rd : roads) {
                for (int i = 0; i + 1 < rd.points().length; i++) {
                    double[] a = rd.points()[i], b = rd.points()[i + 1];
                    double[] seg = {a[0], a[1], b[0], b[1]};
                    int x0 = (int) Math.floor(Math.min(a[0], b[0]) - 64) >> 6, x1 = (int) Math.floor(Math.max(a[0], b[0]) + 64) >> 6;
                    int z0 = (int) Math.floor(Math.min(a[1], b[1]) - 64) >> 6, z1 = (int) Math.floor(Math.max(a[1], b[1]) + 64) >> 6;
                    double len = Math.hypot(b[0] - a[0], b[1] - a[1]);
                    for (int gx = x0; gx <= x1; gx++)
                        for (int gz = z0; gz <= z1; gz++) {
                            // клетка рядом с отрезком (с запасом на диагональ клетки)
                            double cx = gx * 64 + 32, cz = gz * 64 + 32;
                            double u = len < 1e-6 ? 0 : Math.max(0, Math.min(1, ((cx - a[0]) * (b[0] - a[0]) + (cz - a[1]) * (b[1] - a[1])) / (len * len)));
                            if (Math.hypot(cx - a[0] - (b[0] - a[0]) * u, cz - a[1] - (b[1] - a[1]) * u) < 64 + 46)
                                roadGrid.computeIfAbsent(key(gx, gz), k -> new ArrayList<>()).add(seg);
                        }
                }
            }
            if (root.has("anchors")) {
                for (var e : root.getAsJsonObject("anchors").entrySet()) {
                    Map<String, int[]> m = new HashMap<>();
                    for (var a : e.getValue().getAsJsonObject().entrySet()) {
                        var arr = a.getValue().getAsJsonArray();
                        m.put(a.getKey(), new int[]{arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt()});
                    }
                    anchors.put(e.getKey(), m);
                }
            }
            JsonObject lv = root.getAsJsonObject("levels");
            levelSlope = lv.get("slope").getAsDouble();
            levelMax = lv.get("max").getAsInt();
            if (lv.has("safe")) levelSafe = lv.get("safe").getAsDouble();
            if (lv.has("far")) levelFar = lv.get("far").getAsDouble();
            if (lv.has("far_slope")) levelFarSlope = lv.get("far_slope").getAsDouble();
            if (lv.has("road_ease")) roadEase = lv.get("road_ease").getAsDouble();
            if (lv.has("settle_ease")) settleEase = lv.get("settle_ease").getAsDouble();
            LitRPG.LOGGER.info("World map: {} territories, {} cities, {} pois, {} tiles", territories.size(), cities.size(),
                    pois.size(), overworldTiles.size() + netherTiles.size());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load world map", e);
        }
    }
}
