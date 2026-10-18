package com.andrey.litrpg.client;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Метки на карте. Хранятся в файле litrpg_waypoints.txt в папке игры. */
public class Waypoints {
    public record Waypoint(String world, String dim, int x, int z, String name) {}

    private static final List<Waypoint> ALL = new ArrayList<>();
    private static boolean loaded = false;

    private static Path file() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("litrpg_waypoints.txt");
    }

    /** Ключ мира: имя одиночного мира или адрес сервера. */
    private static String worldKey() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSingleplayerServer() != null) {
            return "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        }
        if (mc.getCurrentServer() != null) {
            return "mp:" + mc.getCurrentServer().ip;
        }
        return "unknown";
    }

    private static String dimKey() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : mc.level.dimension().location().toString();
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        ALL.clear();
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length < 5) continue;
                try {
                    ALL.add(new Waypoint(p[0], p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4]));
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void save() {
        List<String> lines = new ArrayList<>();
        for (Waypoint w : ALL) {
            lines.add(w.world() + "\t" + w.dim() + "\t" + w.x() + "\t" + w.z() + "\t" + w.name().replace("\t", " "));
        }
        try {
            Files.write(file(), lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static List<Waypoint> forCurrentDimension() {
        load();
        String wk = worldKey();
        String dk = dimKey();
        List<Waypoint> out = new ArrayList<>();
        for (Waypoint w : ALL) {
            if (w.world().equals(wk) && w.dim().equals(dk)) out.add(w);
        }
        return out;
    }

    public static void add(int x, int z) {
        load();
        int n = forCurrentDimension().size() + 1;
        ALL.add(new Waypoint(worldKey(), dimKey(), x, z, "Метка " + n));
        save();
    }

    /** Удаляет ближайшую метку в радиусе r блоков. */
    public static boolean removeNear(int x, int z, int r) {
        load();
        Waypoint best = null;
        double bestD = (double) r * r;
        for (Waypoint w : forCurrentDimension()) {
            double d = (double) (w.x() - x) * (w.x() - x) + (double) (w.z() - z) * (w.z() - z);
            if (d <= bestD) {
                bestD = d;
                best = w;
            }
        }
        if (best != null) {
            ALL.remove(best);
            save();
            return true;
        }
        return false;
    }

    public static void clearLoaded() {
        loaded = false;
    }
}
