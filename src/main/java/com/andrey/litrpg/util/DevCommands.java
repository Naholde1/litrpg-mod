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
                if (line.startsWith("devcast ")) {
                    // devcast <ячейка> — применить навык с панели от лица Dev
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    int slot = Integer.parseInt(line.substring(8).trim());
                    var st = com.andrey.litrpg.stats.StatsManager.get(p);
                    var a = st.barSkill(slot);
                    double hp = p.getHealth();
                    double mana = st.getMana();
                    var foes = p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20),
                            m -> com.andrey.litrpg.ability.AbilityHandler.isEnemy(p, m));
                    foes.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p)));
                    if (!foes.isEmpty()) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, foes.get(0).getEyePosition());
                    com.andrey.litrpg.ability.AbilityHandler.tryUse(p, slot);
                    LitRPG.LOGGER.info("[dev] cast {} {} hp {}->{} mana {}->{} combo={} orbs={} invoked={}/{}", slot,
                            a == null ? "-" : a.key, hp, p.getHealth(), mana, st.getMana(), st.comboPoints(), st.getOrbs(),
                            st.getInvoked(0), st.getInvoked(1));
                    continue;
                }
                if (line.startsWith("devoffers")) {
                    // devoffers [класс,класс] — что Система предложит Dev и почему;
                    // через запятую можно перечислить классы, будто их уже взяли товарищи
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var st = com.andrey.litrpg.stats.StatsManager.get(p);
                    String busyArg = line.length() > 9 ? line.substring(9).trim() : "";
                    var busy = new java.util.HashSet<>(com.andrey.litrpg.stats.StatsManager.busyClasses(p));
                    if (!busyArg.isEmpty()) busy.addAll(java.util.Arrays.asList(busyArg.split(",")));
                    var list = com.andrey.litrpg.stats.ClassOffers.generate(st, p.getRandom(), busy);
                    for (var c : list) {
                        LitRPG.LOGGER.info("[dev] offer {} role={} score={} {}", c.key, c.role,
                                String.format("%.3f", com.andrey.litrpg.stats.ClassOffers.score(st, c)),
                                com.andrey.litrpg.stats.ClassOffers.reasons(st, c));
                    }
                    continue;
                }
                if (line.startsWith("devskills ")) {
                    // devskills <с> <по> — применить навыки с номерами [с, по) от лица Dev и записать сбои
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    String[] a = line.split(" ");
                    int from = Integer.parseInt(a[1]), to = Math.min(Integer.parseInt(a[2]), com.andrey.litrpg.ability.Ability.count());
                    int ok = 0, no = 0, fail = 0;
                    StringBuilder bad = new StringBuilder();
                    for (int i = from; i < to; i++) {
                        var ab = com.andrey.litrpg.ability.Ability.byId(i);
                        var near = p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(20),
                                m -> com.andrey.litrpg.ability.AbilityHandler.isEnemy(p, m));
                        near.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p)));
                        if (!near.isEmpty()) p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, near.get(0).getEyePosition());
                        try {
                            if (com.andrey.litrpg.ability.AbilityHandler.devExecute(p, ab)) ok++;
                            else { no++; bad.append(ab.key).append(' '); }
                        } catch (Throwable t) {
                            fail++;
                            LitRPG.LOGGER.error("[dev] skill {} failed", ab.key, t);
                        }
                        p.setHealth(p.getMaxHealth());
                    }
                    LitRPG.LOGGER.info("[dev] skills {}..{} ok={} declined={} failed={} declined: {}", from, to, ok, no, fail, bad);
                    continue;
                }
                if (line.startsWith("devshop")) {
                    // devshop [этап] — что предлагает торговец и почём
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    int st = line.length() > 8 ? Integer.parseInt(line.substring(8).trim()) : 1;
                    var held = p.getMainHandItem();
                    LitRPG.LOGGER.info("[dev] shop stage {} в руке {} цена продажи {}", st,
                            held.getHoverName().getString(), com.andrey.litrpg.gear.SellPrices.stack(held));
                    for (var o : com.andrey.litrpg.run.Shop.offers(p, st)) {
                        LitRPG.LOGGER.info("[dev]   offer {} {} {} {}", o.id(),
                                o.stack().isEmpty() ? "-" : o.stack().getHoverName().getString(),
                                o.price(), o.action().isEmpty() ? "" : o.action() + " / " + o.label());
                    }
                    continue;
                }
                if (line.startsWith("devability ")) {
                    // devability <ключ> — применить навык по имени, минуя панель
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var ab = com.andrey.litrpg.ability.Ability.byKey(line.substring(11).trim());
                    if (ab == null) {
                        LitRPG.LOGGER.info("[dev] ability: нет такого навыка");
                        continue;
                    }
                    var st = com.andrey.litrpg.stats.StatsManager.get(p);
                    var foes = p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                            p.getBoundingBox().inflate(20), m -> com.andrey.litrpg.ability.AbilityHandler.isEnemy(p, m));
                    foes.sort(java.util.Comparator.comparingDouble(m -> m.distanceToSqr(p)));
                    if (!foes.isEmpty()) {
                        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, foes.get(0).getEyePosition());
                    }
                    boolean ok = com.andrey.litrpg.ability.AbilityHandler.devExecute(p, ab);
                    LitRPG.LOGGER.info("[dev] ability {} своя={} сработал={}", ab.key,
                            com.andrey.litrpg.ability.Special.has(ab), ok);
                    continue;
                }
                if (line.equals("devbar")) {
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var st = com.andrey.litrpg.stats.StatsManager.get(p);
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < com.andrey.litrpg.ability.Ability.BAR; i++) {
                        var a = st.barSkill(i);
                        b.append(i).append('=').append(a == null ? "-" : a.key);
                        if (a != null && (a.form || a.turret)) b.append(a.form ? "[облик]" : "[турель]");
                        b.append(' ');
                    }
                    LitRPG.LOGGER.info("[dev] bar class={} role={} lvl={} {}", st.getPlayerClass() == null ? "-" : st.getPlayerClass().key,
                            st.getRole(), st.getLevel(), b);
                    continue;
                }
                if (line.equals("devwho")) {
                    // где Dev, держит ли его Печать подземелья и что у него в инвентаре
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    StringBuilder inv = new StringBuilder();
                    for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                        var s = p.getInventory().getItem(i);
                        if (!s.isEmpty()) inv.append(i).append('=').append(s.getHoverName().getString()).append(' ');
                    }
                    var ps = com.andrey.litrpg.stats.StatsManager.get(p);
                    LitRPG.LOGGER.info("[dev] who {} dim={} alive={} spirit={} stage={} class={} res={} {}/{} regen={} calm={}t form={} inv: {}",
                            p.blockPosition(), p.level().dimension().location(),
                            p.isAlive(), com.andrey.litrpg.run.Spirits.isSpirit(p), com.andrey.litrpg.run.Run.state(server).stage,
                            ps.getPlayerClass() == null ? "-" : ps.getPlayerClass().key,
                            ps.resource(), String.format("%.1f", ps.getResource()), ps.getMaxResource(),
                            String.format("%.2f", ps.getResourceRegen()),
                            p.level().getGameTime() - ps.lastCombat,
                            ps.getForm() == null ? "-" : ps.getForm(), inv);
                    continue;
                }
                if (line.equals("devthreat")) {
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    for (var m : p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(24),
                            m -> !(m instanceof com.andrey.litrpg.run.TraderEntity))) {
                        LitRPG.LOGGER.info("[dev] threat {} -> target {} table {}", m.getName().getString(),
                                m.getTarget() == null ? "-" : m.getTarget().getName().getString(), com.andrey.litrpg.event.Threat.debug(m));
                    }
                    continue;
                }
                if (line.startsWith("devtp ")) {
                    // devtp <этап> [boss|exit|x y z] — перенести Dev на этап (к хранителю, выходу или в точку)
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    String[] a = line.split(" ");
                    int n = Integer.parseInt(a[1]);
                    var st = com.andrey.litrpg.run.Run.state(server);
                    if (st.stage != n) com.andrey.litrpg.run.Run.enterStage(server, n);
                    var stage = com.andrey.litrpg.run.Layout.get().stage(n);
                    if (a.length == 3 && a[2].startsWith("boss")) {
                        int k = a[2].length() > 4 ? Integer.parseInt(a[2].substring(4)) : 0;
                        var b = stage.bosses().get(Math.min(k, stage.bosses().size() - 1));
                        p.teleportTo(b.at().x() + 6, b.at().y(), b.at().z());
                    } else if (a.length == 3 && a[2].equals("exit")) {
                        var c = stage.exit().center();
                        p.teleportTo(c.x, c.y, c.z);
                    } else if (a.length == 5) {
                        p.teleportTo(Double.parseDouble(a[2]) + 0.5, Double.parseDouble(a[3]), Double.parseDouble(a[4]) + 0.5);
                    }
                    LitRPG.LOGGER.info("[dev] tp stage {} -> {}", n, p.blockPosition());
                    continue;
                }
                if (line.equals("devrun")) {
                    var st = com.andrey.litrpg.run.Run.state(server);
                    LitRPG.LOGGER.info("[dev] run stage={} wipes={} party={} deadPacks={} deadBosses={} packMobs={} gates={} mechs={}",
                            st.stage, st.wipes, st.partySize, st.deadPacks, st.deadBosses, st.packMobs.keySet(), st.gates, st.mechs.keySet());
                    continue;
                }
                if (line.equals("devdump")) {
                    dump(server);
                    continue;
                }
                if (line.startsWith("devboss")) {
                    // хранители вокруг Dev: здоровье, щит, фаза, узлы силы, зоны, неистовство; и здоровье/роль Dev
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    var st = com.andrey.litrpg.stats.StatsManager.get(p);
                    LitRPG.LOGGER.info("[dev] Dev hp={}/{} armor={} role={} lvl={} mode={}", (int) p.getHealth(), (int) p.getMaxHealth(), p.getArmorValue(),
                            st.getRole(), st.getLevel(), p.gameMode.getGameModeForPlayer());
                    for (var b : p.serverLevel().getEntitiesOfClass(com.andrey.litrpg.entity.BossBase.class, p.getBoundingBox().inflate(64))) {
                        LitRPG.LOGGER.info("[dev]   boss {} {}", b.bossId(), b.debugState());
                    }
                    continue;
                }
                if (line.startsWith("devmobs")) {
                    // мобы вокруг Dev: вид, уровень, ранг, расстояние, состояние (для проверки бестиария)
                    var p = server.getPlayerList().getPlayerByName("Dev");
                    if (p == null) continue;
                    int r = line.length() > 8 ? Integer.parseInt(line.substring(8).trim()) : 64;
                    var mobs = p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(r));
                    LitRPG.LOGGER.info("[dev] mobs around Dev ({} at {} {} {}): {}", r, p.getBlockX(), p.getBlockY(), p.getBlockZ(), mobs.size());
                    for (var m : mobs) {
                        String st = m instanceof com.andrey.litrpg.entity.wild.WildMob w ? (w.isDormant() ? " dormant" : "") + (w.isInvisible() ? " invisible" : "") : "";
                        LitRPG.LOGGER.info("[dev]   {} lvl {} {} d={} hp={}/{} target={}{}", net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(m.getType()),
                                com.andrey.litrpg.event.MobLevels.getLevel(m), com.andrey.litrpg.event.MobLevels.getRank(m), (int) m.distanceTo(p),
                                (int) m.getHealth(), (int) m.getMaxHealth(), m.getTarget() == null ? "-" : m.getTarget().getName().getString(), st);
                    }
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
