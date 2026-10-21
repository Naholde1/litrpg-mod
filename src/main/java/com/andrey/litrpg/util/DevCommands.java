package com.andrey.litrpg.util;

import com.andrey.litrpg.LitRPG;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Только для среды разработки (в собранном моде не работает): раз в секунду
 * выполняет команды из файла run/dev_commands.txt от имени сервера и удаляет файл.
 * Позволяет автоматически тестировать мод в запущенной игре.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DevCommands {
    private static final Path FILE = Path.of("dev_commands.txt");

    private DevCommands() {}

    /** Выгрузка ванильных биомов и настроек шума в run/dump (для генераторов карты). */
    public static void dump(MinecraftServer server) {
        try {
            var access = server.registryAccess();
            var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access);
            var gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            Path root = Path.of("dump");
            var biomes = access.registryOrThrow(net.minecraft.core.registries.Registries.BIOME);
            for (var e : biomes.entrySet()) {
                var json = net.minecraft.world.level.biome.Biome.DIRECT_CODEC.encodeStart(ops, e.getValue()).getOrThrow(false, s -> {});
                Path f = root.resolve("biome/" + e.getKey().location().getNamespace() + "/" + e.getKey().location().getPath() + ".json");
                Files.createDirectories(f.getParent());
                Files.writeString(f, gson.toJson(json), StandardCharsets.UTF_8);
            }
            var noise = access.registryOrThrow(net.minecraft.core.registries.Registries.NOISE_SETTINGS);
            for (var e : noise.entrySet()) {
                var json = net.minecraft.world.level.levelgen.NoiseGeneratorSettings.DIRECT_CODEC.encodeStart(ops, e.getValue()).getOrThrow(false, s -> {});
                Path f = root.resolve("noise_settings/" + e.getKey().location().getPath() + ".json");
                Files.createDirectories(f.getParent());
                Files.writeString(f, gson.toJson(json), StandardCharsets.UTF_8);
            }
            var df = access.registryOrThrow(net.minecraft.core.registries.Registries.DENSITY_FUNCTION);
            for (var e : df.entrySet()) {
                var json = net.minecraft.world.level.levelgen.DensityFunction.DIRECT_CODEC.encodeStart(ops, e.getValue()).getOrThrow(false, s -> {});
                Path f = root.resolve("density_function/" + e.getKey().location().getNamespace() + "/" + e.getKey().location().getPath() + ".json");
                Files.createDirectories(f.getParent());
                Files.writeString(f, gson.toJson(json), StandardCharsets.UTF_8);
            }
            var presets = access.registryOrThrow(net.minecraft.core.registries.Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST);
            for (var e : presets.entrySet()) {
                var json = net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList.DIRECT_CODEC.encodeStart(ops, e.getValue()).getOrThrow(false, s -> {});
                Path f = root.resolve("parameter_list/" + e.getKey().location().getPath() + ".json");
                Files.createDirectories(f.getParent());
                Files.writeString(f, gson.toJson(json), StandardCharsets.UTF_8);
            }
            LitRPG.LOGGER.info("[dev] dump done");
        } catch (Exception ex) {
            LitRPG.LOGGER.error("dump failed", ex);
        }
    }

    /** Вид сверху по настоящему генератору: высота поверхности и биом. */
    private static void render(MinecraftServer server, int size, int x0, int z0, int span) {
        var level = server.overworld();
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        var img = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_RGB);
        int[][] h = new int[size][size];
        long t0 = System.currentTimeMillis();
        java.util.stream.IntStream.range(0, size).parallel().forEach(py -> {
            for (int px = 0; px < size; px++) {
                int x = x0 + px * span / size, z = z0 + py * span / size;
                h[py][px] = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, rs);
            }
        });
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                int x = x0 + px * span / size, z = z0 + py * span / size;
                int y = h[py][px];
                var biome = level.getUncachedNoiseBiome(x >> 2, Math.max(y, 63) >> 2, z >> 2);
                int col = biome.value().getGrassColor(x, z);
                if (y < 63) col = 0x2050A0 + Math.max(0, Math.min(60, (y - 20))) * 0x000101;
                double shade = 1 + Math.max(-0.4, Math.min(0.4, (y - h[Math.max(0, py - 1)][Math.max(0, px - 1)]) * 0.06));
                if (y > 160) shade *= 1.15;
                int r = (int) Math.min(255, ((col >> 16) & 255) * shade), g = (int) Math.min(255, ((col >> 8) & 255) * shade),
                        b = (int) Math.min(255, (col & 255) * shade);
                if (y >= 63 && y > 175) r = g = b = Math.min(255, 200 + (y - 175));
                img.setRGB(px, py, (r << 16) | (g << 8) | b);
            }
        }
        try {
            Files.createDirectories(Path.of("dump"));
            javax.imageio.ImageIO.write(img, "png", Path.of("dump", "render.png").toFile());
        } catch (Exception e) {
            LitRPG.LOGGER.error("render failed", e);
        }
        LitRPG.LOGGER.info("[dev] render {}px in {} ms", size, System.currentTimeMillis() - t0);
    }

    /** Проверка площадок: настоящий рельеф (без загрузки чанков) под каждой площадкой против её высоты. */
    private static void terrain(MinecraftServer server) {
        var level = server.overworld();
        var gen = level.getChunkSource().getGenerator();
        var rs = level.getChunkSource().randomState();
        var map = com.andrey.litrpg.world.map.WorldMap.get();
        int bad = 0, total = 0;
        for (double[] f : map.flats) {
            if (f[3] < 63) continue; // гавани — под водой
            String name = "?";
            double best = 1e9;
            for (var p : map.pois) {
                double d = Math.hypot(p.x() - f[0], p.z() - f[1]);
                if (d < best) { best = d; name = p.id(); }
            }
            int n = 0, off = 0, worst = 0;
            int wx = 0, wz = 0;
            int r = (int) f[2] - 2;
            for (int dx = -r; dx <= r; dx += 4) {
                for (int dz = -r; dz <= r; dz += 4) {
                    if (dx * dx + dz * dz > r * r) continue;
                    if (f.length > 7 && dx * f[5] + dz * f[6] > f[7] - 3) continue;
                    int x = (int) f[0] + dx, z = (int) f[1] + dz;
                    int h = gen.getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, level, rs);
                    int dev = h - (int) f[3];
                    n++;
                    if (Math.abs(dev) > 1) off++;
                    if (Math.abs(dev) > Math.abs(worst)) { worst = dev; wx = x; wz = z; }
                }
            }
            total++;
            if (off > 0) bad++;
            LitRPG.LOGGER.info("[dev] terrain {} r={} y={}: {} точек, отклонений {}, худшее {} у {} {}", name, (int) f[2], (int) f[3], n, off, worst, wx, wz);
        }
        LitRPG.LOGGER.info("[dev] terrain done: площадок {}, с отклонениями {}", total, bad);
    }

    private static String lastBoard = "";

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FMLLoader.isProduction()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0 || !Files.exists(FILE)) return;
        try {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            Files.delete(FILE);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("devmap ")) {
                    String[] a = line.split(" ");
                    double[] r = com.andrey.litrpg.world.map.WorldMap.get().compute(Double.parseDouble(a[1]), Double.parseDouble(a[2]));
                    LitRPG.LOGGER.info("[dev] map {} {} -> H={} W={} T={} water={}", a[1], a[2], r[0], r[1], r[2], r[3]);
                    continue;
                }
                if (line.equals("devterrain")) {
                    terrain(server);
                    continue;
                }
                if (line.startsWith("devrender")) {
                    String[] a = line.split(" ");
                    int size = a.length > 1 ? Integer.parseInt(a[1]) : 400;
                    int x0 = a.length > 3 ? Integer.parseInt(a[2]) : -5000, z0 = a.length > 3 ? Integer.parseInt(a[3]) : -5000;
                    int span = a.length > 4 ? Integer.parseInt(a[4]) : 10000;
                    render(server, size, x0, z0, span);
                    continue;
                }
                if (line.equals("devsay")) {
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var list = p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.npc.RaceNpcEntity.class, p.getBoundingBox().inflate(16));
                    for (var n : list) com.andrey.litrpg.city.Speech.say(n, com.andrey.litrpg.city.Speech.line(n, p.getRandom()), 200);
                    LitRPG.LOGGER.info("[dev] say x{} -> displays {}", list.size(), p.serverLevel().getEntitiesOfClass(
                            net.minecraft.world.entity.Display.TextDisplay.class, p.getBoundingBox().inflate(20)).size());
                    continue;
                }
                if (line.startsWith("devtalk ") || line.startsWith("devaccept ")) {
                    // devtalk <ремесло> — ПКМ по ближайшему мастеру; devaccept <n> — принять n-е поручение с его доски
                    String[] a = line.split(" ");
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    if (a[0].equals("devtalk")) {
                        var list = p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.npc.RaceNpcEntity.class, p.getBoundingBox().inflate(96),
                                n -> n.craft() != null && n.craft().key.equals(a[1]));
                        list.sort(java.util.Comparator.comparingDouble(n -> n.distanceToSqr(p)));
                        if (list.isEmpty()) {
                            LitRPG.LOGGER.info("[dev] no {} nearby", a[1]);
                            continue;
                        }
                        var npc = list.get(0);
                        lastBoard = com.andrey.litrpg.quest.CityQuests.boardKey(npc);
                        p.teleportTo(npc.getX() + 1.5, npc.getY(), npc.getZ() + 1.5);
                        if (a.length > 2 && a[2].equals("trade")) npc.openTrade(p);
                        else p.interactOn(npc, net.minecraft.world.InteractionHand.MAIN_HAND);
                        LitRPG.LOGGER.info("[dev] talked to {} at {}", npc.getName().getString(), npc.blockPosition());
                    } else {
                        var offers = com.andrey.litrpg.stats.StatsManager.get(p).quests().offers(lastBoard);
                        int i = Integer.parseInt(a[1]);
                        if (i < offers.size()) {
                            var q = offers.get(i);
                            LitRPG.LOGGER.info("[dev] accept {} ({} {} {})", q.title, q.type, q.target, q.need);
                            com.andrey.litrpg.quest.QuestEvents.accept(p, lastBoard, q.id);
                        }
                    }
                    continue;
                }
                if (line.startsWith("devbreak ") || line.startsWith("devuse ")) {
                    // от лица игрока Dev: сломать блок / нажать ПКМ по блоку (проверка закона города)
                    String[] a = line.split(" ");
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var pos = new net.minecraft.core.BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
                    boolean ok;
                    if (a[0].equals("devbreak")) {
                        ok = p.gameMode.destroyBlock(pos);
                    } else {
                        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                                net.minecraft.core.Direction.UP, pos, false);
                        ok = p.gameMode.useItemOn(p, p.serverLevel(), p.getMainHandItem(),
                                net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction();
                    }
                    LitRPG.LOGGER.info("[dev] {} {} -> {} (block now {})", a[0], pos, ok, p.serverLevel().getBlockState(pos));
                    continue;
                }
                if (line.equals("devdump")) {
                    dump(server);
                    continue;
                }
                LitRPG.LOGGER.info("[dev] /{}", line);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), line);
            }
        } catch (Exception e) {
            LitRPG.LOGGER.error("dev commands failed", e);
        }
    }
}
