package com.andrey.litrpg.block;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.world.Dungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Алтарь подземелья. Когда игрок подходит близко, призывает хранителя.
 * После его гибели «остывает» (по умолчанию 20 минут), затем хранитель
 * может явиться снова — подземелье можно проходить повторно.
 */
public class BossAltarBlockEntity extends BlockEntity {
    public static final int RESPAWN_TICKS = 20 * 60 * 20;

    private String dungeon = "";
    private long readyAt = 0;
    private UUID bossId;
    private final Set<UUID> greeted = new HashSet<>();

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

        // приветствие у входа в арену
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

        Entity boss = be.bossId == null ? null : sl.getEntity(be.bossId);
        if (boss != null && boss.isAlive()) return;
        if (be.bossId != null && boss == null && sl.isLoaded(pos)) {
            // хранитель исчез без гибели (например, выгружен вдали) — ждём, вдруг вернётся
            if (now % 200 != 0) return;
            be.bossId = null;
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

    /** Хранитель повержен — алтарь остывает. */
    public void onBossDefeated() {
        bossId = null;
        if (level != null) readyAt = level.getGameTime() + RESPAWN_TICKS;
        setChanged();
    }

    public void describe(Player player) {
        Dungeon d = dungeon();
        if (d == null || level == null) return;
        long left = readyAt - level.getGameTime();
        String state = bossId != null ? "Хранитель пробуждён!"
                : left > 0 ? "Хранитель повержен. Вернётся через " + (left / 1200 + 1) + " мин."
                : "Хранитель дремлет. Подойдите ближе, если готовы.";
        player.sendSystemMessage(Component.literal("◆ " + d.title + " (ур. " + d.level + ")").withStyle(s -> s.withColor(d.color)));
        player.sendSystemMessage(Component.literal(d.desc).withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.literal(state).withStyle(left > 0 ? ChatFormatting.GREEN : ChatFormatting.GOLD));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Dungeon", dungeon);
        tag.putLong("ReadyAt", readyAt);
        if (bossId != null) tag.putUUID("BossId", bossId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        dungeon = tag.getString("Dungeon");
        readyAt = tag.getLong("ReadyAt");
        bossId = tag.hasUUID("BossId") ? tag.getUUID("BossId") : null;
    }
}
