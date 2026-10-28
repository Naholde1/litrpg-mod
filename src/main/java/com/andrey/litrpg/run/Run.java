/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.SkillEffects;
import com.andrey.litrpg.block.CacheBlockEntity;
import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.event.MobLevels.Rank;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.PartyPacket;
import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Run {
    public static final ResourceKey<Level> DIM;
    public static final String PACK_TAG = "litrpg_pack";
    public static final String BOSS_TAG = "litrpg_stage_boss";
    public static final int MAX_PLAYERS = 5;
    private static final double LEASH = (double)36.0F;
    private static int countdown;
    private static String countdownWhat;
    private static int wipeTimer;
    private static final Map<UUID, Vec3> SAFE;
    private static final Map<String, BlockState> PARSED;

    private Run() {
    }

    public static ServerLevel level(MinecraftServer server) {
        return server.getLevel(DIM);
    }

    public static boolean inDungeon(Entity e) {
        return e.level().dimension() == DIM;
    }

    public static RunState state(MinecraftServer server) {
        return RunState.get(server);
    }

    public static List<ServerPlayer> party(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList();

        for(ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (inDungeon(p) && !p.isCreative()) {
                out.add(p);
            }
        }

        return out;
    }

    public static List<ServerPlayer> alive(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList();

        for(ServerPlayer p : party(server)) {
            if (!Spirits.isSpirit(p) && p.isAlive()) {
                out.add(p);
            }
        }

        return out;
    }

    public static Layout.Stage current(MinecraftServer server) {
        int n = state(server).stage;
        return n >= 1 && n <= 5 ? Layout.get().stage(n) : null;
    }

    public static int levelAt(ServerLevel level, BlockPos pos) {
        Layout.Stage s = Layout.get().stageAt((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
        return s == null ? 1 : (s.minLevel() + s.maxLevel()) / 2;
    }

    public static Vec3 safeSpot(ServerPlayer p) {
        Vec3 v = (Vec3)SAFE.get(p.getUUID());
        return v == null || p.onGround() && !(p.position().distanceTo(v) < 0.1) ? p.position() : v;
    }

    static Vec3 solidNear(ServerLevel level, Vec3 v) {
        BlockPos c = BlockPos.containing(v);

        for(int r = 0; r <= 10; ++r) {
            for(int dx = -r; dx <= r; ++dx) {
                for(int dz = -r; dz <= r; ++dz) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) {
                        for(int dy = 1; dy >= -2; --dy) {
                            BlockPos f = c.offset(dx, dy, dz);
                            if (level.getBlockState(f.below()).isSolidRender(level, f.below()) && level.getBlockState(f).isAir() && level.getBlockState(f.above()).isAir() && level.getFluidState(f.below()).isEmpty()) {
                                return Vec3.atBottomCenterOf(f);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    public static double hpScale(int players) {
        return players <= 3 ? Math.max(0.45, (double)players / (double)3.0F) : (double)1.0F + 0.3 * (double)(players - 3);
    }

    @SubscribeEvent
    public static void onStarted(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        GameRules rules = server.getGameRules();
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_NATURAL_REGENERATION)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_DOMOBSPAWNING)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_DO_PATROL_SPAWNING)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_DO_TRADER_SPAWNING)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_DOINSOMNIA)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_DOFIRETICK)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_MOBGRIEFING)).set(false, server);
        ((GameRules.BooleanValue)rules.getRule(GameRules.RULE_SHOWDEATHMESSAGES)).set(false, server);
        if (!FMLLoader.isProduction() && state(server).minPlayers > 1) {
            state(server).minPlayers = 1;
            state(server).setDirty();
        }

        countdown = -1;
        wipeTimer = -1;
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        Player var2 = e.getEntity();
        if (var2 instanceof ServerPlayer p) {
            p.server.execute(() -> placeOnLogin(p));
        }
    }

    private static void placeOnLogin(ServerPlayer p) {
        ServerLevel dl = level(p.server);
        if (dl != null) {
            if (p.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                p.setGameMode(GameType.SURVIVAL);
            }

            Layout.Stage cur;
            boolean var10000;
            label36: {
                label35: {
                    RunState st = state(p.server);
                    cur = current(p.server);
                    if (inDungeon(p)) {
                        if (cur == null) {
                            if (Layout.get().lobby.contains(p.position()) || st.stage == 6) {
                                break label35;
                            }
                        } else if (cur.box().contains(p.position())) {
                            break label35;
                        }
                    }

                    var10000 = false;
                    break label36;
                }

                var10000 = true;
            }

            boolean inside = var10000;
            if (!inside) {
                if (cur == null) {
                    teleport(p, Layout.get().lobbySpawn);
                } else {
                    teleport(p, cur.camp());
                    catchUp(p, cur);
                }

            }
        }
    }

    public static void teleport(ServerPlayer p, Layout.Spot s) {
        ServerLevel dl = level(p.server);
        if (dl != null) {
            p.teleportTo(dl, s.x(), s.y(), s.z(), s.yaw(), 0.0F);
            p.setDeltaMovement(Vec3.ZERO);
            p.fallDistance = 0.0F;
        }
    }

    private static void catchUp(ServerPlayer p, Layout.Stage s) {
        PlayerStats ps = StatsManager.get(p);
        int need = s.n() == 1 ? 1 : s.minLevel();
        if (ps.getLevel() < need) {
            int before = ps.getLevel();
            ps.setLevel(need);
            StatsManager.onLevelUp(p, before, need);
            StatsManager.message(p, Component.literal("Система подтянула вас до уровня этапа: " + need + ".").withStyle(ChatFormatting.GOLD));
        }

    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase == Phase.END) {
            MinecraftServer server = e.getServer();
            ServerLevel dl = level(server);
            if (dl != null) {
                long t = dl.getGameTime();
                if (t % 40L == 0L) {
                    for(ServerPlayer p : server.getPlayerList().getPlayers()) {
                        if (!inDungeon(p) && !p.isCreative() && !p.isSpectator()) {
                            placeOnLogin(p);
                        }
                    }
                }

                for(ServerPlayer p : party(server)) {
                    tickPlayer(p, t);
                }

                if (t % 4L == 0L) {
                    sendParty(server);
                }

                if (t % 5L == 0L) {
                    RunState st = state(server);
                    if (countdown >= 0) {
                        tickCountdown(server, st);
                    }

                    if (st.stage == 0) {
                        tickLobby(server, st);
                    } else {
                        Layout.Stage s = current(server);
                        if (s != null) {
                            tickPacks(dl, st, s);
                            tickBosses(dl, st, s);
                            Mechanisms.tick(dl, st, s);
                            tickWipe(server, st, s);
                            tickExit(server, st, s);
                        }
                    }
                }
            }
        }
    }

    private static void tickPlayer(ServerPlayer p, long t) {
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(5.0F);
        if (!Spirits.isSpirit(p)) {
            if (p.onGround() && !p.isInWater() && !p.isInLava()) {
                SAFE.put(p.getUUID(), p.position());
            }

            RunState st = state(p.server);
            Layout.Stage s = current(p.server);
            double floor = s != null ? (double)(s.box().y1() - 6) : (double)(Layout.get().lobby.y1() - 6);
            if (p.getY() < floor && SAFE.containsKey(p.getUUID())) {
                Vec3 v = (Vec3)SAFE.get(p.getUUID());
                p.teleportTo(v.x, v.y + 0.2, v.z);
                p.setDeltaMovement(Vec3.ZERO);
                p.fallDistance = 0.0F;
                p.hurt(p.damageSources().fellOutOfWorld(), p.getMaxHealth() * 0.2F);
                p.displayClientMessage(Component.literal("Пропасть отпустила вас… на этот раз.").withStyle(ChatFormatting.DARK_RED), true);
            }

            if (p.isInLava() && t % 5L == 0L) {
                Vec3 v = solidNear(p.serverLevel(), (Vec3)SAFE.getOrDefault(p.getUUID(), p.position()));
                if (v != null) {
                    p.teleportTo(v.x, v.y + 0.1, v.z);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.fallDistance = 0.0F;
                    p.clearFire();
                    p.hurt(p.damageSources().lava(), p.getMaxHealth() * 0.25F);
                    p.setRemainingFireTicks(40);
                    p.displayClientMessage(Component.literal("Лава обожгла вас — вы выбрались на камень.").withStyle(ChatFormatting.GOLD), true);
                }
            }

            if (t % 20L == 0L && !SkillEffects.inCombat(p) && p.getHealth() < p.getMaxHealth()) {
                p.heal(p.getMaxHealth() * 0.06F);
            }

            if (t % 20L == 0L && !SkillEffects.inCombat(p)) {
                PlayerStats ps = StatsManager.get(p);
                if (ps.getMana() < (double)ps.getMaxMana()) {
                    ps.setMana(ps.getMana() + (double)ps.getMaxMana() * 0.04);
                    StatsManager.syncMana(p);
                }
            }

            if (t % 10L == 0L) {
                Lore.tick(p, st, s);
            }

        }
    }

    private static void sendParty(MinecraftServer server) {
        List<ServerPlayer> party = party(server);
        if (!party.isEmpty()) {
            List<PartyPacket.Member> list = new ArrayList();

            for(ServerPlayer p : party) {
                PlayerStats ps = StatsManager.get(p);
                int role = ps.getRole() == null ? 0 : ps.getRole().ordinal() + 1;
                int color = ps.getPlayerClass() != null ? ps.getPlayerClass().color : (ps.getRole() != null ? ps.getRole().color : 16777215);
                int flags = (Spirits.isSpirit(p) ? 1 : 0) | (SkillEffects.inCombat(p) ? 2 : 0);
                list.add(new PartyPacket.Member(p.getUUID(), p.getGameProfile().getName(), p.getHealth(), p.getMaxHealth(), p.getAbsorptionAmount(), (float)(ps.getMana() / (double)Math.max(1, ps.getMaxMana())), role, color, flags));
            }

            PartyPacket pk = new PartyPacket(list, state(server).stage);

            for(ServerPlayer p : party) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pk);
            }

        }
    }

    private static void tickLobby(MinecraftServer server, RunState st) {
        if (countdown < 0) {
            List<ServerPlayer> all = party(server);
            if (!all.isEmpty()) {
                int at = 0;

                for(ServerPlayer p : all) {
                    if (Layout.get().lobbyStart.contains(p.position())) {
                        ++at;
                    }
                }

                if (at != 0) {
                    long t = level(server).getGameTime();
                    if (t % 60L == 0L) {
                        if (all.size() > 5) {
                            for(ServerPlayer p : all) {
                                p.displayClientMessage(Component.literal("Подземелье пускает не больше 5 искателей. Сейчас вас " + all.size() + ".").withStyle(ChatFormatting.RED), true);
                            }

                        } else if (all.size() < st.minPlayers) {
                            for(ServerPlayer p : all) {
                                int var10001 = st.minPlayers;
                                p.displayClientMessage(Component.literal("Врата откроются для группы от " + var10001 + " до 5 искателей. Сейчас вас " + all.size() + ".").withStyle(ChatFormatting.GOLD), true);
                            }

                        } else if (at >= all.size()) {
                            for(ServerPlayer p : all) {
                                if (StatsManager.get(p).getRole() == null) {
                                    p.displayClientMessage(Component.literal(p.getName().getString() + " ещё не выбрал роль (клавиша K).").withStyle(ChatFormatting.GOLD), true);
                                    return;
                                }
                            }

                            startCountdown("start", 5);
                        } else {
                            for(ServerPlayer p : all) {
                                p.displayClientMessage(Component.literal("Все должны встать на печать у врат: " + at + " из " + all.size()).withStyle(ChatFormatting.GOLD), true);
                            }

                        }
                    }
                }
            }
        }
    }

    private static void startCountdown(String what, int seconds) {
        countdown = seconds * 4;
        countdownWhat = what;
    }

    private static void tickCountdown(MinecraftServer server, RunState st) {
        if (countdown % 4 == 0 && countdown > 0) {
            for(ServerPlayer p : party(server)) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 25, 5));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(String.valueOf(countdown / 4)).withStyle(ChatFormatting.GOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(countdownWhat.equals("start") ? "Врата Подземелья открываются" : "Разлом затягивает вас").withStyle(ChatFormatting.GRAY)));
                p.playNotifySound((SoundEvent)SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.MASTER, 1.0F, 0.8F + (float)countdown * 0.02F);
            }
        }

        if (countdown-- <= 0) {
            countdown = -1;
            if (countdownWhat.equals("start")) {
                enterStage(server, 1);
            } else if (countdownWhat.equals("exit")) {
                if (st.stage >= 5) {
                    finish(server);
                } else {
                    enterStage(server, st.stage + 1);
                }
            }

        }
    }

    public static void enterStage(MinecraftServer server, int n) {
        RunState st = state(server);
        ServerLevel dl = level(server);
        Layout.Stage old = current(server);
        if (old != null) {
            clearEntities(dl, old);
        }

        st.stage = n;
        st.wipes = 0;
        st.stageStart = dl.getGameTime();
        st.partySize = Math.max(1, party(server).size());
        Layout.Stage s = current(server);
        resetStage(dl, st, s);
        Spirits.clearAll(server);

        for(ServerPlayer p : party(server)) {
            teleport(p, s.camp());
            restore(p);
            catchUp(p, s);
            p.connection.send(new ClientboundSetTitlesAnimationPacket(15, 80, 25));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Этап " + n + " · " + s.name()).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(s.subtitle()).withStyle(ChatFormatting.GRAY)));
            p.playNotifySound(SoundEvents.END_PORTAL_SPAWN, SoundSource.MASTER, 0.6F, 0.7F);
            StatsManager.message(p, Component.literal("Этап " + n + ": " + s.name() + " (уровни " + s.minLevel() + "–" + s.maxLevel() + "). Торговец у костра. Хранителей на этапе: " + s.bosses().size() + ".").withStyle(ChatFormatting.GOLD));
        }

        st.setDirty();
    }

    public static void wipe(MinecraftServer server) {
        RunState st = state(server);
        Layout.Stage s = current(server);
        if (s != null) {
            ServerLevel dl = level(server);
            ++st.wipes;
            clearEntities(dl, s);
            resetStage(dl, st, s);
            Spirits.clearAll(server);

            for(ServerPlayer p : party(server)) {
                teleport(p, s.camp());
                restore(p);
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Группа пала").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Этап начинается заново · попытка " + (st.wipes + 1)).withStyle(ChatFormatting.GRAY)));
                p.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.MASTER, 0.5F, 0.6F);
            }

            st.setDirty();
        }
    }

    public static void restore(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.removeAllEffects();
        p.clearFire();
        p.setTicksFrozen(0);
        StatsManager.applyAttributes(p);
        p.setHealth(p.getMaxHealth());
        PlayerStats ps = StatsManager.get(p);
        ps.setMana((double)ps.getMaxMana());
        ps.resetCooldowns();
        StatsManager.sync(p);
    }

    private static void clearEntities(ServerLevel dl, Layout.Stage s) {
        AABB box = s.box().aabb();

        for(Entity e : dl.getEntitiesOfClass(Entity.class, box, (x) -> !(x instanceof Player))) {
            if (!(e instanceof TraderEntity) && (e instanceof Mob || e instanceof ItemEntity || e instanceof Projectile || e instanceof ExperienceOrb)) {
                e.discard();
            }
        }

        RunState st = state(dl.getServer());

        for(List<UUID> l : st.packMobs.values()) {
            for(UUID u : l) {
                discard(dl, u);
            }
        }

        for(UUID u : st.bossMobs.values()) {
            discard(dl, u);
        }

    }

    private static void discard(ServerLevel dl, UUID u) {
        Entity e = dl.getEntity(u);
        if (e != null) {
            e.discard();
        }

    }

    private static void resetStage(ServerLevel dl, RunState st, Layout.Stage s) {
        // Сначала хранители, потом clearStage: он стирает bossMobs, и без этого старый
        // хранитель оставался на арене с прежним здоровьем и всем, что успел разрушить,
        // а рядом вставал второй. Группа видела «столбы не восстановились» и двух боссов.
        for (Layout.Boss b : s.bosses()) {
            BossBase boss = findBoss(dl, st, s, b);
            if (boss != null) {
                boss.resetAttempt();
            }
        }

        st.clearStage();
        // новая попытка начинается с чистой таблицы угрозы: иначе хранитель помнил,
        // кто больше всех надамажил в прошлом заходе, и с порога бежал к нему
        com.andrey.litrpg.event.Threat.clearAll();

        for(Layout.Gate g : s.gates()) {
            setGate(dl, st, g, g.open());
        }

        Mechanisms.reset(dl, st, s);

        for(Layout.Cache c : s.caches()) {
            BlockEntity var6 = dl.getBlockEntity(c.pos());
            if (var6 instanceof CacheBlockEntity be) {
                be.reset();
            }
        }

        ensureTrader(dl, s);
        st.setDirty();
    }

    private static void ensureTrader(ServerLevel dl, Layout.Stage s) {
        Vec3 at = s.trader().vec();
        if (dl.getEntitiesOfClass(TraderEntity.class, (new AABB(at, at)).inflate((double)24.0F)).isEmpty()) {
            TraderEntity t = (TraderEntity)((EntityType)ModEntities.TRADER.get()).create(dl);
            if (t != null) {
                t.moveTo(at.x, at.y, at.z, s.trader().yaw(), 0.0F);
                t.setYHeadRot(s.trader().yaw());
                t.setStage(s.n());
                dl.addFreshEntity(t);
            }
        }
    }

    public static boolean gateOpen(RunState st, Layout.Gate g) {
        return (Boolean)st.gates.getOrDefault(g.id(), g.open());
    }

    public static void setGate(ServerLevel dl, RunState st, Layout.Gate g, boolean open) {
        st.gates.put(g.id(), open);
        st.setDirty();
        BlockState fill = open ? Blocks.AIR.defaultBlockState() : parse(dl, g.block());

        for(BlockPos p : g.box().blocks()) {
            BlockState cur = dl.getBlockState(p);
            if (cur != fill) {
                dl.setBlock(p, fill, 18);
            }
        }

    }

    public static void gate(MinecraftServer server, String id, boolean open) {
        Layout.Stage s = current(server);
        if (s != null) {
            Layout.Gate g = s.gate(id);
            if (g != null) {
                RunState st = state(server);
                if (gateOpen(st, g) != open) {
                    ServerLevel dl = level(server);
                    setGate(dl, st, g, open);
                    Vec3 c = g.box().center();
                    dl.playSound((Player)null, c.x, c.y, c.z, open ? SoundEvents.PISTON_CONTRACT : SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.5F, 0.6F);
                    dl.sendParticles(ParticleTypes.CLOUD, c.x, c.y + (double)1.0F, c.z, 20, 1.2, (double)1.0F, 1.2, 0.02);
                }
            }
        }
    }

    static BlockState parse(ServerLevel dl, String s) {
        return (BlockState)PARSED.computeIfAbsent(s, (k) -> {
            try {
                return BlockStateParser.parseForBlock(dl.holderLookup(Registries.BLOCK), k, false).blockState();
            } catch (Exception var3) {
                LitRPG.LOGGER.warn("bad gate block {}", k);
                return Blocks.IRON_BARS.defaultBlockState();
            }
        });
    }

    private static void tickPacks(ServerLevel dl, RunState st, Layout.Stage s) {
        List<ServerPlayer> alive = alive(dl.getServer());

        for(Layout.Pack pack : s.packs()) {
            int var10000 = s.n();
            String key = var10000 + ":" + pack.id();
            if (!st.deadPacks.contains(pack.id())) {
                List<UUID> ids = (List)st.packMobs.get(pack.id());
                if (ids == null) {
                    if (!pack.ambush() && dl.isLoaded(pack.at().pos())) {
                        for(ServerPlayer p : alive) {
                            if (p.position().distanceTo(pack.at().vec()) < pack.r()) {
                                spawnPack(dl, st, s, pack, key);
                                break;
                            }
                        }
                    }
                } else {
                    List<Mob> mobs = new ArrayList();
                    boolean anyUnloaded = false;

                    for(UUID u : ids) {
                        Entity e = dl.getEntity(u);
                        if (e instanceof Mob) {
                            Mob m = (Mob)e;
                            if (m.isAlive()) {
                                mobs.add(m);
                                continue;
                            }
                        }

                        if (e == null && !st.packKilled(u)) {
                            anyUnloaded = true;
                        }
                    }

                    if (mobs.isEmpty() && !anyUnloaded) {
                        st.deadPacks.add(pack.id());
                        st.setDirty();
                        Mechanisms.packCleared(dl, st, s, pack.id());
                    } else {
                        LivingEntity target = null;

                        for(Mob m : mobs) {
                            LivingEntity var14 = m.getTarget();
                            if (var14 instanceof Player) {
                                Player pl = (Player)var14;
                                if (pl.isAlive() && !pl.isSpectator()) {
                                    target = m.getTarget();
                                }
                            }
                        }

                        for(Mob m : mobs) {
                            if (target != null && (m.getTarget() == null || !m.getTarget().isAlive())) {
                                m.setTarget(target);
                            }

                            if (m.position().distanceTo(pack.at().vec()) > (double)36.0F) {
                                evade(dl, m, pack);
                            }
                        }
                    }
                }
            }
        }

    }

    private static void evade(ServerLevel dl, Mob m, Layout.Pack pack) {
        Vec3 home = pack.at().vec();
        dl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + (double)0.5F, m.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
        Vec3 own = m.getPersistentData().contains("litrpg_home") ? new Vec3(m.getPersistentData().getDouble("litrpg_hx"), m.getPersistentData().getDouble("litrpg_hy"), m.getPersistentData().getDouble("litrpg_hz")) : home;
        m.teleportTo(own.x, own.y, own.z);
        m.setTarget((LivingEntity)null);
        m.setHealth(m.getMaxHealth());
        m.getNavigation().stop();
    }

    public static void spawnPackNow(ServerLevel dl, RunState st, Layout.Stage s, String packId) {
        Layout.Pack pack = s.pack(packId);
        if (pack != null && !st.deadPacks.contains(packId) && !st.packMobs.containsKey(packId)) {
            int var10004 = s.n();
            spawnPack(dl, st, s, pack, var10004 + ":" + packId);
            Vec3 c = pack.at().vec();
            dl.playSound((Player)null, c.x, c.y, c.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 0.7F);

            for(Layout.Mob mo : pack.mobs()) {
                dl.sendParticles(ParticleTypes.LARGE_SMOKE, mo.at().x(), mo.at().y() + (double)0.5F, mo.at().z(), 12, 0.3, (double)0.5F, 0.3, 0.02);
            }

        }
    }

    private static void spawnPack(ServerLevel dl, RunState st, Layout.Stage s, Layout.Pack pack, String key) {
        List<UUID> ids = new ArrayList();
        double hp = hpScale(st.partySize);
        int idx = -1;

        for(Layout.Mob mo : pack.mobs()) {
            ++idx;
            EntityType<?> type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(mo.type()));
            if (type != null) {
                Entity var13 = type.create(dl);
                if (var13 instanceof Mob) {
                    Mob m = (Mob)var13;
                    Vec3 var15 = mo.at().vec();
                    m.moveTo(var15.x, var15.y, var15.z, mo.at().yaw(), 0.0F);
                    m.setYHeadRot(mo.at().yaw());
                    m.finalizeSpawn(dl, dl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.SPAWNER, (SpawnGroupData)null, (CompoundTag)null);
                    m.getPersistentData().putString("litrpg_pack", key);
                    m.getPersistentData().putInt("litrpg_pidx", idx);
                    m.getPersistentData().putBoolean("litrpg_home", true);
                    m.getPersistentData().putDouble("litrpg_hx", var15.x);
                    m.getPersistentData().putDouble("litrpg_hy", var15.y);
                    m.getPersistentData().putDouble("litrpg_hz", var15.z);
                    if (pack.miniboss() && !mo.name().isEmpty()) {
                        m.getPersistentData().putBoolean("litrpg_miniboss", true);
                    }

                    m.setPersistenceRequired();
                    MobLevels.Rank rank = Rank.valueOf(mo.rank());
                    MobLevels.apply(m, mo.level(), rank, hp * (pack.miniboss() && !mo.name().isEmpty() ? (double)2.5F : (double)1.0F));
                    if (!mo.name().isEmpty()) {
                        m.setCustomName(Component.literal("[Ур. " + mo.level() + "] ").withStyle(ChatFormatting.RED).append(Component.literal(mo.name()).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})));
                        m.setCustomNameVisible(true);
                        m.getPersistentData().putBoolean("litrpg_autoname", false);
                    }

                    Minibosses.setup(m, mo, s);
                    dl.addFreshEntity(m);
                    ids.add(m.getUUID());
                    continue;
                }
            }

            LitRPG.LOGGER.warn("pack {}: unknown mob {}", key, mo.type());
        }

        st.packMobs.put(pack.id(), ids);
        st.setDirty();
    }

    @SubscribeEvent(
        priority = EventPriority.LOW
    )
    public static void onDeath(LivingDeathEvent e) {
        if (!e.isCanceled()) {
            Level var2 = e.getEntity().level();
            if (var2 instanceof ServerLevel) {
                ServerLevel dl = (ServerLevel)var2;
                if (dl.dimension() == DIM) {
                    LivingEntity dead = e.getEntity();
                    MinecraftServer server = dl.getServer();
                    RunState st = state(server);
                    Layout.Stage s = current(server);
                    if (s == null) {
                        return;
                    }

                    String pack = dead.getPersistentData().getString("litrpg_pack");
                    if (!pack.isEmpty() && pack.startsWith(s.n() + ":")) {
                        st.killedMobs.add(dead.getUUID());
                        st.setDirty();
                        Rewards.mobKilled(server, s, dead, pack.substring(pack.indexOf(58) + 1));
                        return;
                    }

                    String boss = dead.getPersistentData().getString("litrpg_stage_boss");
                    if (!boss.isEmpty() && boss.startsWith(s.n() + ":") && dead instanceof BossBase) {
                        BossBase bb = (BossBase)dead;
                        String id = boss.substring(boss.indexOf(58) + 1);
                        bossKilled(dl, st, s, s.boss(id), bb);
                    }

                    return;
                }
            }
        }

    }

    private static void tickBosses(ServerLevel dl, RunState st, Layout.Stage s) {
        List<ServerPlayer> party = party(dl.getServer());

        for(Layout.Boss b : s.bosses()) {
            if (!st.deadBosses.contains(b.id())) {
                UUID id = (UUID)st.bossMobs.get(b.id());
                Entity e = id == null ? null : dl.getEntity(id);
                if (e == null) {
                    if (id == null || dl.isLoaded(b.at().pos())) {
                        boolean near = false;

                        for(ServerPlayer p : party) {
                            if (p.position().distanceTo(b.at().vec()) < (double)80.0F) {
                                near = true;
                            }
                        }

                        if (near && dl.isLoaded(b.at().pos())) {
                            // хранитель мог уже стоять на арене (вайп стёр список, чанк
                            // выгружался) — тогда берём его, а не ставим второго
                            BossBase found = findBoss(dl, st, s, b);
                            if (found != null) {
                                st.bossMobs.put(b.id(), found.getUUID());
                                st.setDirty();
                            } else {
                                spawnBoss(dl, st, s, b);
                            }
                        }
                    }
                } else if (e instanceof BossBase) {
                    BossBase boss = (BossBase)e;
                    boolean fight = boss.inCombat();
                    CompoundFlag flag = Run.CompoundFlag.of(st, "boss_" + b.id());
                    if (fight && !flag.get()) {
                        flag.set(true);

                        for(String d : b.doors()) {
                            gate(dl.getServer(), d, false);
                        }
                    } else if (!fight && flag.get()) {
                        flag.set(false);

                        for(String d : b.doors()) {
                            gate(dl.getServer(), d, true);
                        }
                    }
                }
            }
        }

    }

    /** Хранитель этого этапа, если он уже есть в мире: по записи в состоянии или на арене. */
    private static BossBase findBoss(ServerLevel dl, RunState st, Layout.Stage s, Layout.Boss b) {
        UUID id = (UUID)st.bossMobs.get(b.id());
        Entity known = id == null ? null : dl.getEntity(id);
        if (known instanceof BossBase kb) {
            return kb;
        }
        AABB box = b.arena() != null ? b.arena().aabb().inflate((double)24.0F, (double)48.0F, (double)24.0F)
                : new AABB(b.at().vec(), b.at().vec()).inflate((double)48.0F);
        String tag = s.n() + ":" + b.id();
        for (BossBase boss : dl.getEntitiesOfClass(BossBase.class, box)) {
            if (tag.equals(boss.getPersistentData().getString("litrpg_stage_boss"))) {
                return boss;
            }
        }
        return null;
    }

    private static void spawnBoss(ServerLevel dl, RunState st, Layout.Stage s, Layout.Boss b) {
        EntityType<?> type = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(b.type()));
        if (type != null) {
            Entity var6 = type.create(dl);
            if (var6 instanceof BossBase) {
                BossBase boss = (BossBase)var6;
                Vec3 var7 = b.at().vec();
                boss.moveTo(var7.x, var7.y, var7.z, b.at().yaw(), 0.0F);
                boss.setYHeadRot(b.at().yaw());
                CompoundTag var10000 = boss.getPersistentData();
                int var10002 = s.n();
                var10000.putString("litrpg_stage_boss", var10002 + ":" + b.id());
                boss.setArena(b.arena(), b.at().pos(), b.extra());
                boss.scaleForParty(st.partySize);
                dl.addFreshEntity(boss);
                st.bossMobs.put(b.id(), boss.getUUID());
                st.setDirty();
                return;
            }
        }

        LitRPG.LOGGER.warn("stage {}: unknown boss {}", s.n(), b.type());
        st.deadBosses.add(b.id());
    }

    private static void bossKilled(ServerLevel dl, RunState st, Layout.Stage s, Layout.Boss b, BossBase boss) {
        if (b != null && !st.deadBosses.contains(b.id())) {
            st.deadBosses.add(b.id());
            st.setDirty();
            MinecraftServer server = dl.getServer();

            for(String d : b.doors()) {
                gate(server, d, true);
            }

            for(String u : b.unlock()) {
                gate(server, u, true);
            }

            AABB var10002 = boss.getBoundingBox().inflate((double)64.0F);
            Objects.requireNonNull(boss);

            for(Mob m : dl.getEntitiesOfClass(Mob.class, var10002, boss::isMinion)) {
                dl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + (double)0.5F, m.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
                m.discard();
            }

            Rewards.bossKilled(server, s, b, boss);
            Mechanisms.bossKilled(dl, st, s, b.id());
            boolean last = st.deadBosses.size() >= s.bosses().size();

            for(ServerPlayer p : party(server)) {
                Spirits.revive(p, 0.6F, (String)null);
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Хранитель повержен").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})));
                p.connection.send(new ClientboundSetSubtitleTextPacket(boss.getName().copy().withStyle(ChatFormatting.LIGHT_PURPLE)));
                p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 1.0F, 1.0F);
                if (last) {
                    StatsManager.message(p, Component.literal(s.n() >= 5 ? "Последний хранитель пал. Разлом ведёт наружу — к свету." : "Все хранители этапа повержены. Разлом открыт — путь на следующий этап.").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }

        }
    }

    private static void tickWipe(MinecraftServer server, RunState st, Layout.Stage s) {
        List<ServerPlayer> party = party(server);
        boolean anyAlive = false;

        for(ServerPlayer p : party) {
            if (!Spirits.isSpirit(p) && p.isAlive()) {
                anyAlive = true;
            }
        }

        if (!party.isEmpty() && !anyAlive) {
            if (wipeTimer < 0) {
                wipeTimer = 12;
            }

            if (--wipeTimer == 0) {
                wipeTimer = -1;
                wipe(server);
            }

        } else {
            wipeTimer = -1;
        }
    }

    private static void tickExit(MinecraftServer server, RunState st, Layout.Stage s) {
        if (countdown < 0 && st.deadBosses.size() >= s.bosses().size()) {
            for(ServerPlayer p : alive(server)) {
                if (s.exit().contains(p.position())) {
                    startCountdown("exit", 3);
                    return;
                }
            }

        }
    }

    private static void finish(MinecraftServer server) {
        RunState st = state(server);
        Layout.Stage old = current(server);
        if (old != null) {
            clearEntities(level(server), old);
        }

        st.stage = 6;
        st.setDirty();
        Spirits.clearAll(server);

        for(ServerPlayer p : party(server)) {
            teleport(p, Layout.get().lobbySpawn);
            restore(p);
        }

        Credits.play(server);
    }

    static {
        DIM = ResourceKey.create(Registries.DIMENSION, LitRPG.id("dungeon"));
        countdown = -1;
        countdownWhat = "";
        wipeTimer = -1;
        SAFE = new HashMap();
        PARSED = new HashMap();
    }

    static record CompoundFlag(RunState st, String key) {
        static CompoundFlag of(RunState st, String key) {
            return new CompoundFlag(st, key);
        }

        boolean get() {
            return this.st.mech("_flags").getBoolean(this.key);
        }

        void set(boolean v) {
            this.st.mech("_flags").putBoolean(this.key, v);
            this.st.setDirty();
        }
    }
}
