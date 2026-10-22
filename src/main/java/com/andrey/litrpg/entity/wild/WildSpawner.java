package com.andrey.litrpg.entity.wild;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.world.BloodMoon;
import com.andrey.litrpg.world.Zone;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Расселение бестиария. Раз в секунду возле каждого игрока Система выбирает место
 * в 24–52 блоках (под землёй — 16–36), смотрит землю (территорию карты или биом),
 * уровень зоны, время суток и подбирает вид, чья полоса уровней (базовый ±5)
 * накрывает уровень зоны. Ванильный природный спавн вне полосы вида отменяется —
 * поэтому у столицы бродят зомби 1–9 уровня, а в глуши — огры, виверны и химеры.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class WildSpawner {
    private WildSpawner() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.getGameTime() % 20 != 7 || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) return;
        if (level.dimension() == Level.END) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || level.random.nextFloat() > 0.6f) continue;
            try {
                trySpawn(level, p);
            } catch (Exception e) {
                LitRPG.LOGGER.warn("WildSpawner: {}", e.toString());
            }
        }
    }

    /** Дикие твари и призванные ими слуги не ранят друг друга (шальная стрела кентавра не стравит стаю волков). */
    @SubscribeEvent
    public static void onAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity a && wildSide(a) && wildSide(event.getEntity())
                && a != event.getEntity()) {
            event.setCanceled(true);
        }
    }

    private static boolean wildSide(net.minecraft.world.entity.LivingEntity e) {
        return e instanceof WildMob || e.getPersistentData().contains(WildMob.SUMMONER_TAG);
    }

    /** Ванильные мобы появляются только там, где уровень зоны попадает в их полосу. */
    @SubscribeEvent
    public static void onPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        MobSpawnType t = event.getSpawnType();
        if (t != MobSpawnType.NATURAL && t != MobSpawnType.CHUNK_GENERATION) return;
        Integer base = Species.band(event.getEntityType());
        if (base == null) return;
        ServerLevel level = event.getLevel().getLevel();
        int[] r = Zone.levels(level, event.getPos());
        int mid = (r[0] + r[1]) / 2;
        if (Math.abs(mid - base) > MobLevels.BAND + 1) event.setResult(Event.Result.DENY);
        // в средних землях ванильные мобы почти все отсеяны — без этого лимит забивали бы одни эндермены
        else if (event.getEntityType() == EntityType.ENDERMAN && event.getRandom().nextFloat() < 0.8f) event.setResult(Event.Result.DENY);
    }

    private static void trySpawn(ServerLevel level, ServerPlayer player) {
        RandomSource rnd = level.random;
        boolean nether = level.dimension() == Level.NETHER;
        int px = player.getBlockX(), pz = player.getBlockZ();
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
        boolean under = !nether && player.getY() < top - 8 && !level.canSeeSky(player.blockPosition().above());

        // плотность: не больше 7–12 тварей вокруг игрока
        int near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48, 24, 48),
                m -> m instanceof Enemy || m instanceof WildMob).size();
        int[] zr = Zone.levels(level, player.blockPosition());
        int cap = under ? 6 : 7 + Math.min(5, zr[1] / 10);
        if (near >= cap) return;

        double a = rnd.nextDouble() * Math.PI * 2;
        double d = under ? 16 + rnd.nextDouble() * 20 : 24 + rnd.nextDouble() * 28;
        int x = Mth.floor(px + Math.cos(a) * d), z = Mth.floor(pz + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        BlockPos pos = under || nether ? cave(level, x, player.getBlockY(), z, rnd, nether) : surface(level, x, z);
        if (pos == null) return;
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < 16 * 16) return;

        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        boolean settled = false;
        if (SystemWorld.isOverworld(level)) {
            WorldMap map = WorldMap.get();
            if (map.cityAt(x, z) != null) return;   // в городах — стража, не звери
            settled = map.settlementAt(x, z) != null;
        }
        int[] r = Zone.levels(level, pos);
        int L = r[0] + rnd.nextInt(Math.max(1, r[1] - r[0] + 1));
        String land = land(level, pos);
        boolean night = under || nether || level.isNight();

        List<Species.Spawn> cand = new ArrayList<>();
        int total = 0;
        for (Species.Spawn s : Species.SPAWNS) {
            if (Math.abs(s.level() - L) > MobLevels.BAND) continue;
            if (!s.lands().contains(land) && !s.lands().contains("any")) continue;
            boolean placeOk = under ? s.place() != Species.Place.SURFACE : s.place() != Species.Place.UNDER;
            if (!placeOk) continue;
            if (s.time() == Species.Time.NIGHT && !night || s.time() == Species.Time.DAY && night) continue;
            Species sp = Species.get(s.type().getPath());
            boolean hostile = sp == null || sp.hostile();
            if (hostile && (peaceful || settled)) continue;
            cand.add(s);
            total += s.weight();
        }
        if (total <= 0) return;
        int pick = rnd.nextInt(total);
        Species.Spawn row = cand.get(0);
        for (Species.Spawn s : cand) {
            pick -= s.weight();
            if (pick < 0) {
                row = s;
                break;
            }
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(row.type());
        if (type == null) return;
        Species sp = Species.of(type);
        boolean bloodMoon = BloodMoon.isActive(level);
        int n = row.min() + rnd.nextInt(Math.max(1, row.max() - row.min() + 1));
        for (int i = 0; i < n; i++) {
            BlockPos at = pos;
            if (i > 0) {
                int ox = pos.getX() + rnd.nextInt(7) - 3, oz = pos.getZ() + rnd.nextInt(7) - 3;
                at = under || nether ? new BlockPos(ox, pos.getY(), oz) : surface(level, ox, oz);
                if (at == null) continue;
            }
            if (!(type.create(level) instanceof Mob mob)) return;
            double y = at.getY() + (sp != null && sp.flying() ? 2 + rnd.nextInt(4) : 0);
            mob.moveTo(at.getX() + 0.5, y, at.getZ() + 0.5, rnd.nextFloat() * 360f, 0);
            if (!level.noCollision(mob) || !level.isUnobstructed(mob)) continue;
            int lvl = MobLevels.bandLevel(row.level(), L, rnd) + (bloodMoon ? 4 : 0);
            MobLevels.apply(mob, Mth.clamp(lvl, 1, 99), MobLevels.rollRank(mob, lvl, bloodMoon));
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
            level.addFreshEntityWithPassengers(mob);
        }
    }

    /** Точка на поверхности: не в воде, не на листве. */
    private static BlockPos surface(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) return null;
        BlockPos pos = new BlockPos(x, y, z);
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.getFluidState().isEmpty() || !level.getFluidState(pos).isEmpty()) return null;
        if (ground.is(net.minecraft.tags.BlockTags.LEAVES) || !ground.isFaceSturdy(level, pos.below(), Direction.UP)) return null;
        return pos;
    }

    /** Пещера (или ад): воздух в два блока над твёрдым полом, без неба и в темноте. */
    private static BlockPos cave(ServerLevel level, int x, int py, int z, RandomSource rnd, boolean nether) {
        for (int k = 0; k < 12; k++) {
            int y = py + rnd.nextInt(21) - 10;
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above())) continue;
            BlockState floor = level.getBlockState(pos.below());
            if (!floor.isFaceSturdy(level, pos.below(), Direction.UP) || !floor.getFluidState().isEmpty()) continue;
            if (!nether && (level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK, pos) > 7)) continue;
            if (nether && level.getBrightness(LightLayer.BLOCK, pos) > 11) continue;
            return pos;
        }
        return null;
    }

    /** Земля для таблицы расселения: территория Мира Системы, а в обычном мире — по биому. */
    public static String land(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.NETHER) return "nether";
        if (SystemWorld.isOverworld(level)) {
            WorldMap.Territory t = WorldMap.get().territory(WorldMap.get().territoryAt(pos.getX(), pos.getZ()));
            return t == null ? "wild" : t.id;
        }
        String b = level.getBiome(pos).unwrapKey().map(ResourceKey::location).map(Object::toString).orElse("");
        if (b.contains("frozen") || b.contains("ice") || b.contains("snowy_slopes") || b.contains("jagged")) return "frost";
        if (b.contains("taiga") || b.contains("grove") || b.contains("snowy")) return "beastkin";
        if (b.contains("peak") || b.contains("hills") || b.contains("stony") || b.contains("windswept")) return "dwarves";
        if (b.contains("desert") || b.contains("badlands") || b.contains("savanna")) return "orcs";
        if (b.contains("swamp") || b.contains("mangrove")) return "goblins";
        if (b.contains("forest") || b.contains("jungle") || b.contains("birch") || b.contains("dark")) return "elves";
        if (b.contains("plains") || b.contains("meadow") || b.contains("beach") || b.contains("river")) return "kingdom";
        return "wild";
    }
}
