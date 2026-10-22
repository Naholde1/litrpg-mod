package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.wild.Species;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.LootRolls;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Арены испытаний: удар в колокол посреди ристалища зовёт три волны чудовищ уровня
 * этих земель (виды из бестиария, больше — если бойцов несколько). Выстоял — опыт,
 * монеты и шанс на снаряжение; ушли с арены или пали все — испытание провалено.
 * Новое испытание на той же арене — через 15 минут.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Arena {
    private Arena() {}

    private static final int WAVES = 3;
    private static final long COOLDOWN = 15 * 60 * 20;

    private static final class Trial {
        final WorldMap.Poi poi;
        final BlockPos center;
        final int level;
        final Set<UUID> fighters = new HashSet<>();
        final Set<UUID> mobs = new HashSet<>();
        int wave;
        long nextWaveAt;

        Trial(WorldMap.Poi poi, BlockPos center, int level) {
            this.poi = poi;
            this.center = center;
            this.level = level;
        }
    }

    private static final Map<String, Trial> ACTIVE = new HashMap<>();
    private static final Map<String, Long> READY_AT = new HashMap<>();

    @SubscribeEvent
    public static void onBell(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getLevel() instanceof ServerLevel sl)) return;
        BlockPos pos = e.getPos();
        if (!sl.getBlockState(pos).is(Blocks.BELL) || sl.dimension() != Level.OVERWORLD || !SystemWorld.isOverworld(sl)) return;
        WorldMap.Poi arena = null;
        for (WorldMap.Poi poi : WorldMap.get().pois) {
            if (poi.type().equals("arena") && Math.abs(poi.x() - pos.getX()) <= 4 && Math.abs(poi.z() - pos.getZ()) <= 4) arena = poi;
        }
        if (arena == null) return;
        long now = sl.getGameTime();
        if (ACTIVE.containsKey(arena.id())) {
            StatsManager.message(p, Component.literal("Испытание уже идёт — сражайтесь!").withStyle(ChatFormatting.RED));
            return;
        }
        long ready = READY_AT.getOrDefault(arena.id(), 0L);
        if (now < ready) {
            StatsManager.message(p, Component.literal("Ристалище ещё не готово. Новое испытание — через " + ((ready - now) / 1200 + 1) + " мин.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        Trial t = new Trial(arena, pos.below(), WorldMap.get().levelAt(arena.x(), arena.z()));
        for (ServerPlayer o : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(16, 8, 16), o -> !o.isSpectator())) {
            t.fighters.add(o.getUUID());
        }
        t.nextWaveAt = now + 60;
        ACTIVE.put(arena.id(), t);
        title(sl, t, Component.literal("Испытание арены").withStyle(ChatFormatting.GOLD),
                Component.literal("Уровень " + t.level + " • " + WAVES + " волны • готовьтесь!").withStyle(ChatFormatting.YELLOW));
        sl.playSound(null, pos, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 2f, 1f);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD
                || ACTIVE.isEmpty() || sl.getGameTime() % 20 != 0) return;
        long now = sl.getGameTime();
        for (Trial t : new ArrayList<>(ACTIVE.values())) {
            // бойцы: живы и в пределах арены
            List<ServerPlayer> here = new ArrayList<>();
            for (UUID id : t.fighters) {
                if (sl.getPlayerByUUID(id) instanceof ServerPlayer p && p.isAlive() && p.distanceToSqr(t.center.getX(), t.center.getY(), t.center.getZ()) < 40 * 40) {
                    here.add(p);
                }
            }
            if (here.isEmpty()) {
                finish(sl, t, false, here);
                continue;
            }
            t.mobs.removeIf(id -> {
                Entity m = sl.getEntity(id);
                return m == null || !m.isAlive();
            });
            if (!t.mobs.isEmpty()) continue;
            if (t.nextWaveAt == Long.MAX_VALUE) {
                // волна пала: последняя — победа, иначе передышка
                if (t.wave >= WAVES) {
                    finish(sl, t, true, here);
                    continue;
                }
                t.nextWaveAt = now + 100;
                title(sl, t, Component.literal("Волна " + t.wave + " повержена").withStyle(ChatFormatting.GREEN),
                        Component.literal("Следующая — через 5 секунд").withStyle(ChatFormatting.GRAY));
                continue;
            }
            if (now < t.nextWaveAt) continue;
            spawnWave(sl, t, here);
        }
    }

    private static void spawnWave(ServerLevel sl, Trial t, List<ServerPlayer> here) {
        t.wave++;
        RandomSource rnd = sl.random;
        List<Species> pool = new ArrayList<>();
        for (Species s : Species.all()) {
            if (s.hostile() && !s.flying() && s.place != Species.Place.UNDER && !s.has(Species.Trait.WATER) && Math.abs(s.level - t.level) <= 6) {
                pool.add(s);
            }
        }
        int count = 2 + t.wave + (here.size() - 1) * 2;
        for (int i = 0; i < count; i++) {
            EntityType<?> type;
            if (pool.isEmpty()) {
                type = EntityType.ZOMBIE;
            } else {
                Species s = pool.get(rnd.nextInt(pool.size()));
                type = ForgeRegistries.ENTITY_TYPES.getValue(LitRPG.id(s.id));
            }
            if (type == null) continue;
            Entity ent = type.create(sl);
            if (!(ent instanceof Mob mob)) continue;
            double a = rnd.nextDouble() * Math.PI * 2, d = 7 + rnd.nextDouble() * 5;
            double x = t.center.getX() + 0.5 + Math.cos(a) * d, z = t.center.getZ() + 0.5 + Math.sin(a) * d;
            mob.moveTo(x, t.center.getY() + 1, z, rnd.nextFloat() * 360, 0);
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null, null);
            mob.setPersistenceRequired();
            mob.setTarget(here.get(rnd.nextInt(here.size())));
            sl.addFreshEntity(mob);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, t.center.getY() + 1.5, z, 12, 0.3, 0.6, 0.3, 0.02);
            t.mobs.add(mob.getUUID());
        }
        t.nextWaveAt = Long.MAX_VALUE;
        title(sl, t, Component.literal("Волна " + t.wave + " из " + WAVES).withStyle(ChatFormatting.RED),
                Component.literal(t.mobs.size() + " противников").withStyle(ChatFormatting.GRAY));
        sl.playSound(null, t.center, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 1.5f, 1.2f);
    }

    private static void finish(ServerLevel sl, Trial t, boolean won, List<ServerPlayer> here) {
        ACTIVE.remove(t.poi.id());
        READY_AT.put(t.poi.id(), sl.getGameTime() + (won ? COOLDOWN : COOLDOWN / 3));
        for (UUID id : t.mobs) {
            Entity m = sl.getEntity(id);
            if (m != null) {
                sl.sendParticles(ParticleTypes.POOF, m.getX(), m.getY() + 1, m.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
                m.discard();
            }
        }
        if (!won) {
            for (UUID id : t.fighters) {
                if (sl.getPlayerByUUID(id) instanceof ServerPlayer p) {
                    StatsManager.message(p, Component.literal("Испытание арены провалено.").withStyle(ChatFormatting.RED));
                }
            }
            return;
        }
        RandomSource rnd = sl.random;
        for (ServerPlayer p : here) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Арена покорена!").withStyle(ChatFormatting.GOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(t.poi.name()).withStyle(ChatFormatting.YELLOW)));
            StatsManager.giveXp(p, t.level * 45 + 60);
            for (ItemStack c : LootRolls.coinStacks(t.level * 30 + rnd.nextInt(t.level * 20 + 1))) {
                if (!p.getInventory().add(c)) p.drop(c, false);
            }
            if (rnd.nextFloat() < 0.6f) {
                Rarity r = GearGen.rollRarity(rnd, 1.6f);
                ItemStack gear = GearGen.random(rnd, t.level, r);
                if (!p.getInventory().add(gear)) p.drop(gear, false);
                StatsManager.message(p, Component.literal("Награда арены: ").withStyle(ChatFormatting.GOLD).append(gear.getHoverName()));
            }
        }
        sl.playSound(null, t.center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, t.center.getX() + 0.5, t.center.getY() + 2, t.center.getZ() + 0.5, 80, 1.5, 1.5, 1.5, 0.3);
    }

    private static void title(ServerLevel sl, Trial t, Component title, Component sub) {
        for (UUID id : t.fighters) {
            if (sl.getPlayerByUUID(id) instanceof ServerPlayer p) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
                p.connection.send(new ClientboundSetTitleTextPacket(title));
                p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            }
        }
    }
}
