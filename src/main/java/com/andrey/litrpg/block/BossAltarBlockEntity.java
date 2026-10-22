package com.andrey.litrpg.block;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.world.Dungeon;
import com.andrey.litrpg.world.DungeonSeal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Алтарь подземелья. Когда игрок подходит близко, призывает хранителя.
 * После его гибели «остывает» (по умолчанию 20 минут), затем хранитель
 * может явиться снова — подземелье можно проходить повторно.
 * Пока хранитель не повержен, Печать держит спустившихся внутри ({@link DungeonSeal}),
 * а вещи погибших хранит до победы и тогда выкладывает в Сундук павших.
 */
public class BossAltarBlockEntity extends BlockEntity {
    public static final int RESPAWN_TICKS = 20 * 60 * 20;

    private String dungeon = "";
    private long readyAt = 0;
    private UUID bossId;
    private final Set<UUID> greeted = new HashSet<>();
    /** Вещи погибших внутри — ждут победы над хранителем. */
    private final List<ItemStack> fallen = new ArrayList<>();
    /** Сундуки павших у Печати. */
    private final List<BlockPos> chests = new ArrayList<>();
    /** Подземелье вокруг Печати (кешируется: поворот шаблона узнаётся по залам). */
    private AABB inside;
    private Vec3 landing;
    /** Границы подземелья относительно Печати (из шаблона): x1 y1 z1 x2 y2 z2. */
    private int[] box;
    /** Ступень большого подземелья: после победы открываются рунные преграды этой ступени. */
    private int stage = -1;
    /** Печать держит только на арене и только пока хранитель пробуждён (большие подземелья). */
    private boolean arenaOnly;
    /** Открытые после победы рунные преграды (закроются, когда хранитель вернётся и рядом никого). */
    private final List<BlockPos> doors = new ArrayList<>();

    public BossAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BOSS_ALTAR_BE.get(), pos, state);
    }

    public Dungeon dungeon() {
        return Dungeon.byKey(dungeon);
    }

    public void setDungeon(Dungeon d) {
        dungeon = d.key;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BossAltarBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || level.getGameTime() % 10 != 0) return;
        Dungeon d = be.dungeon();
        if (d == null) return;
        long now = level.getGameTime();
        Vec3 c = Vec3.atBottomCenterOf(pos);

        // приветствие у входа в арену (снова — после того, как игрок ушёл)
        if (now % 600 == 0) be.greeted.removeIf(id -> {
            var pl = sl.getPlayerByUUID(id);
            return pl == null || pl.distanceToSqr(c) > 60 * 60;
        });
        if (now % 40 == 0) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(30, 16, 30))) {
                if (be.greeted.add(p.getUUID())) {
                    p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
                    p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(d.title)
                            .withStyle(s -> s.withColor(d.color).withBold(true))));
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Подземелье • рекомендуемый уровень " + d.level)
                            .withStyle(ChatFormatting.GRAY)));
                }
            }
        }

        if (!be.cleared() && (!be.arenaOnly || be.bossId != null) && be.bounds(sl)) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, be.inside,
                    p -> p.isAlive() && !p.isCreative() && !p.isSpectator())) {
                DungeonSeal.seal(p, be, be.inside, be.landing);
            }
        }

        Entity boss = be.bossId == null ? null : sl.getEntity(be.bossId);
        if (boss != null && boss.isAlive()) return;
        if (be.bossId != null && boss == null && sl.isLoaded(pos)) {
            // хранитель исчез без гибели (например, выгружен вдали) — ждём, вдруг вернётся
            if (now % 200 != 0) return;
            be.bossId = null;
        }
        if (now >= be.readyAt && !be.doors.isEmpty()
                && sl.getEntitiesOfClass(Player.class, new AABB(pos).inflate(64, 32, 64), p -> !p.isSpectator()).isEmpty()) {
            be.closeDoors(sl);
        }
        if (now < be.readyAt) return;
        if (now % 20 == 0) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 1.0, c.z, 3, 0.3, 0.2, 0.3, 0.01);
            sl.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.5, c.z, 6, 0.6, 0.5, 0.6, 0.4);
        }
        var players = sl.getEntitiesOfClass(Player.class, new AABB(pos).inflate(9, 5, 9), p -> !p.isSpectator() && !p.isCreative());
        if (players.isEmpty()) return;
        be.summon(sl, d, sl.getEntitiesOfClass(Player.class, new AABB(pos).inflate(24, 10, 24), p -> !p.isSpectator()).size());
    }

    private void summon(ServerLevel sl, Dungeon d, int players) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("litrpg", d.boss));
        if (type == null) return;
        Entity e = type.create(sl);
        if (!(e instanceof BossBase boss)) return;
        Vec3 spot = Vec3.atBottomCenterOf(worldPosition.above());
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            Vec3 t = new Vec3(worldPosition.getX() + 0.5 + Math.cos(a) * 4, worldPosition.getY(), worldPosition.getZ() + 0.5 + Math.sin(a) * 4);
            if (sl.noCollision(type.getAABB(t.x, t.y, t.z))) {
                spot = t;
                break;
            }
        }
        boss.moveTo(spot.x, spot.y, spot.z, sl.random.nextFloat() * 360, 0);
        boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(worldPosition), MobSpawnType.STRUCTURE, null, null);
        boss.setHome(worldPosition);
        boss.scaleForParty(Math.max(1, players));
        sl.addFreshEntity(boss);
        bossId = boss.getUUID();
        setChanged();

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(spot);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, spot.x, spot.y + 1, spot.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.SOUL, spot.x, spot.y + 1, spot.z, 40, 1.2, 1.2, 1.2, 0.05);
        sl.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.8f);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(32, 16, 32))) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("⚔ ").append(boss.getName()).append(" ⚔")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Хранитель подземелья • ур. " + boss.getBossLevel()
                    + (players > 1 ? " • усилен для группы (" + players + ")" : "")).withStyle(ChatFormatting.GOLD)));
        }
    }

    /** Хранитель повержен — алтарь остывает, печать снята, сундук павших открыт. */
    public void onBossDefeated() {
        bossId = null;
        if (level != null) readyAt = level.getGameTime() + RESPAWN_TICKS;
        setChanged();
        if (level instanceof ServerLevel sl) {
            DungeonSeal.release(sl, worldPosition);
            releaseFallen(sl);
            if (stage >= 0) openDoors(sl);
        }
    }

    /** Хранитель повержен и ещё не вернулся: печати нет, сундук павших открыт. */
    public boolean cleared() {
        return bossId == null && level != null && level.getGameTime() < readyAt;
    }

    public int fallenCount() {
        return fallen.size();
    }

    // ---------- границы подземелья ----------
    // Шаблон 41×41 (tools/gen_dungeons.py), Печать в точке (28, F, 28): подземелье — от -28 до +12 по x и z,
    // по высоте — от пола до 4 блоков под поверхностью. Шаблон бывает повёрнут — поворот узнаём по пустым залам.
    private static final int[][] HALLS = {{-8, -22}, {-22, -9}, {5, -22}};

    private boolean bounds(ServerLevel sl) {
        if (inside != null) return true;
        if (box != null && box.length == 6) {
            // шаблон мог лечь повёрнутым — берём границы, общие для всех поворотов
            int m = 0;
            for (int i : new int[]{0, 2, 3, 5}) m = Math.max(m, Math.abs(box[i]));
            if (arenaOnly) {
                inside = new AABB(worldPosition.getX() + box[0], worldPosition.getY() + box[1], worldPosition.getZ() + box[2],
                        worldPosition.getX() + box[3] + 1, worldPosition.getY() + box[4], worldPosition.getZ() + box[5] + 1);
            } else {
                inside = new AABB(worldPosition.getX() - m, worldPosition.getY() + box[1], worldPosition.getZ() - m,
                        worldPosition.getX() + m + 1, worldPosition.getY() + box[4], worldPosition.getZ() + m + 1);
            }
            landing = Vec3.atBottomCenterOf(worldPosition.above());
            return true;
        }
        if (!sl.isAreaLoaded(worldPosition, 30)) return false;
        Rotation rot = Rotation.NONE;
        for (Rotation r : Rotation.values()) {
            int air = 0;
            for (int[] h : HALLS) {
                if (sl.getBlockState(worldPosition.offset(new BlockPos(h[0], 1, h[1]).rotate(r))).isAir()) air++;
            }
            if (air >= 2) {
                rot = r;
                break;
            }
        }
        BlockPos a = worldPosition.offset(new BlockPos(-28, 0, -28).rotate(rot));
        BlockPos b = worldPosition.offset(new BlockPos(12, 0, 12).rotate(rot));
        inside = new AABB(Math.min(a.getX(), b.getX()), worldPosition.getY() - 3, Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()) + 1, worldPosition.getY() + 11, Math.max(a.getZ(), b.getZ()) + 1);
        BlockPos l = worldPosition.offset(new BlockPos(-8, 0, -24).rotate(rot));
        landing = new Vec3(l.getX() + 0.5, l.getY(), l.getZ() + 0.5);
        return true;
    }

    /** В шаблоне границы записаны списком чисел. */
    private static int[] boxFromList(CompoundTag tag) {
        if (!tag.contains("Box", Tag.TAG_LIST)) return null;
        ListTag l = tag.getList("Box", Tag.TAG_INT);
        if (l.size() != 6) return null;
        int[] b = new int[6];
        for (int i = 0; i < 6; i++) b[i] = l.getInt(i);
        return b;
    }

    /** Рунные преграды этой ступени рассыпаются — путь дальше открыт. */
    private void openDoors(ServerLevel sl) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-56, -24, -56), worldPosition.offset(56, 40, 56))) {
            BlockState st = sl.getBlockState(p);
            if (st.is(ModBlocks.SEAL_DOOR.get()) && st.getValue(SealDoorBlock.STAGE) == stage) {
                doors.add(p.immutable());
                sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                if (n++ % 4 == 0) sl.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.5);
            }
        }
        setChanged();
        if (n > 0) {
            sl.playSound(null, worldPosition, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 2f, 0.7f);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(56, 32, 56))) {
                p.sendSystemMessage(Component.literal("◆ Рунная преграда пала — путь дальше открыт.").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    private void closeDoors(ServerLevel sl) {
        BlockState st = ModBlocks.SEAL_DOOR.get().defaultBlockState().setValue(SealDoorBlock.STAGE, Math.max(0, stage));
        for (BlockPos p : doors) if (sl.getBlockState(p).isAir()) sl.setBlock(p, st, 3);
        doors.clear();
        setChanged();
    }

    public int stage() {
        return stage;
    }

    public boolean arenaOnly() {
        return arenaOnly;
    }

    public boolean awake() {
        return bossId != null;
    }

    // ---------- сундук павших ----------
    public void addFallen(List<ItemStack> items) {
        fallen.addAll(items);
        setChanged();
        if (level instanceof ServerLevel sl) {
            BlockPos c = chest(sl, 0);
            if (c != null) {
                sl.sendParticles(ParticleTypes.SOUL, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 25, 0.3, 0.4, 0.3, 0.03);
                sl.playSound(null, c, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS, 1.5f, 0.7f);
            }
        }
    }

    private static final int[][] CHEST_SPOTS = {{0, 3}, {3, 0}, {0, -3}, {-3, 0}, {3, 3}, {-3, 3}, {3, -3}, {-3, -3}};

    /** n-й сундук павших у Печати (ставится, если его нет). */
    private BlockPos chest(ServerLevel sl, int n) {
        chests.removeIf(p -> !(sl.getBlockEntity(p) instanceof ChestBlockEntity));
        if (n < chests.size()) return chests.get(n);
        for (int[] o : CHEST_SPOTS) {
            BlockPos p = worldPosition.offset(o[0], 0, o[1]);
            if (chests.contains(p) || !sl.getBlockState(p).canBeReplaced()
                    || !sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), Direction.UP)) continue;
            Direction face = Math.abs(o[0]) >= Math.abs(o[1]) ? (o[0] > 0 ? Direction.WEST : Direction.EAST)
                    : (o[1] > 0 ? Direction.NORTH : Direction.SOUTH);
            sl.setBlock(p, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, face).setValue(ChestBlock.TYPE, ChestType.SINGLE), 3);
            if (sl.getBlockEntity(p) instanceof ChestBlockEntity c) {
                c.setCustomName(Component.literal("Сундук павших").withStyle(ChatFormatting.DARK_PURPLE));
                c.getPersistentData().putLong(DungeonSeal.CHEST_TAG, worldPosition.asLong());
                c.setChanged();
                chests.add(p);
                setChanged();
                return p;
            }
        }
        return null;
    }

    /** Победа: вещи павших ложатся в сундуки (что не влезло — рядом на пол). */
    private void releaseFallen(ServerLevel sl) {
        if (fallen.isEmpty()) return;
        List<ItemStack> left = new ArrayList<>(fallen);
        fallen.clear();
        setChanged();
        for (int n = 0; n < CHEST_SPOTS.length && !left.isEmpty(); n++) {
            BlockPos p = chest(sl, n);
            if (p == null || !(sl.getBlockEntity(p) instanceof ChestBlockEntity c)) break;
            for (int i = 0; i < c.getContainerSize() && !left.isEmpty(); i++) {
                if (c.getItem(i).isEmpty()) c.setItem(i, left.remove(0));
            }
            c.setChanged();
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 30, 0.3, 0.5, 0.3, 0.2);
        }
        for (ItemStack s : left) {
            sl.addFreshEntity(new ItemEntity(sl, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, s));
        }
        sl.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.6f);
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(40, 16, 40))) {
            p.sendSystemMessage(Component.literal("☠ Сундук павших у Печати открыт — заберите вещи погибших.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public void describe(Player player) {
        Dungeon d = dungeon();
        if (d == null || level == null) return;
        long left = readyAt - level.getGameTime();
        String state = bossId != null ? "Хранитель пробуждён! Печать держит всех внутри."
                : left > 0 ? "Хранитель повержен. Вернётся через " + (left / 1200 + 1) + " мин."
                : "Хранитель дремлет. Подойдите ближе, если готовы.";
        player.sendSystemMessage(Component.literal("◆ " + d.title + " (ур. " + d.level + ")").withStyle(s -> s.withColor(d.color)));
        player.sendSystemMessage(Component.literal(d.desc).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(state).withStyle(left > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        if (!fallen.isEmpty()) {
            player.sendSystemMessage(Component.literal("☠ В Сундуке павших ждут вещи погибших: " + fallen.size()
                    + ". Он откроется после победы над хранителем.").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Dungeon", dungeon);
        if (box != null) tag.putIntArray("Box", box);
        tag.putInt("Stage", stage);
        tag.putBoolean("ArenaOnly", arenaOnly);
        tag.put("Doors", new LongArrayTag(doors.stream().mapToLong(BlockPos::asLong).toArray()));
        tag.putLong("ReadyAt", readyAt);
        if (bossId != null) tag.putUUID("BossId", bossId);
        ListTag items = new ListTag();
        for (ItemStack s : fallen) items.add(s.save(new CompoundTag()));
        tag.put("Fallen", items);
        tag.put("Chests", new LongArrayTag(chests.stream().mapToLong(BlockPos::asLong).toArray()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        dungeon = tag.getString("Dungeon");
        box = tag.contains("Box", Tag.TAG_INT_ARRAY) ? tag.getIntArray("Box") : boxFromList(tag);
        stage = tag.contains("Stage") ? tag.getInt("Stage") : -1;
        arenaOnly = tag.getBoolean("ArenaOnly");
        doors.clear();
        for (long l : tag.getLongArray("Doors")) doors.add(BlockPos.of(l));
        readyAt = tag.getLong("ReadyAt");
        bossId = tag.hasUUID("BossId") ? tag.getUUID("BossId") : null;
        fallen.clear();
        ListTag items = tag.getList("Fallen", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = ItemStack.of(items.getCompound(i));
            if (!s.isEmpty()) fallen.add(s);
        }
        chests.clear();
        for (long l : tag.getLongArray("Chests")) chests.add(BlockPos.of(l));
    }
}
