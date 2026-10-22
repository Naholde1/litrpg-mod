package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.block.BossAltarBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Печать подземелья: кто спустился в подземелье, где хранитель ещё жив, не выйдет наружу,
 * пока хранитель не падёт (печать возвращает назад). Погибший внутри теряет выпавшие вещи —
 * их уносит в «Сундук павших» у Печати, а открыть его можно только после победы над хранителем.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DungeonSeal {
    private DungeonSeal() {}

    /** Пометка сундука павших (в данных сундука): позиция Печати. */
    public static final String CHEST_TAG = "litrpg_fallen";

    private record Seal(ResourceKey<Level> dim, BlockPos altar, AABB box, Vec3 landing) {}

    private static final Map<UUID, Seal> SEALS = new HashMap<>();
    private static final Map<UUID, Vec3> LAST = new HashMap<>();

    public static boolean sealed(ServerPlayer p) {
        return SEALS.containsKey(p.getUUID());
    }

    /** Игрок внутри незачищенного подземелья — печать держит его. */
    public static void seal(ServerPlayer p, BossAltarBlockEntity altar, AABB box, Vec3 landing) {
        Seal old = SEALS.put(p.getUUID(), new Seal(p.level().dimension(), altar.getBlockPos(), box, landing));
        if (old != null) return;
        LAST.put(p.getUUID(), p.position());
        Dungeon d = altar.dungeon();
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("⛓ Вход запечатан ⛓").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Выход откроется, когда падёт хранитель").withStyle(ChatFormatting.GRAY)));
        p.sendSystemMessage(Component.literal("⛓ Печать " + (d != null ? "подземелья «" + d.title + "»" : "подземелья")
                + " сомкнулась за спиной. Наружу не выйти, пока хранитель жив. Погибнешь — выпавшие вещи унесёт в Сундук павших у Печати.")
                .withStyle(ChatFormatting.RED));
        p.level().playSound(null, p.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.5f);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1f, 0.6f);
    }

    /** Хранитель повержен — печать снята со всех, кто внутри. */
    public static void release(ServerLevel sl, BlockPos altar) {
        for (UUID id : new ArrayList<>(SEALS.keySet())) {
            Seal s = SEALS.get(id);
            if (!s.altar.equals(altar) || s.dim != sl.dimension()) continue;
            SEALS.remove(id);
            LAST.remove(id);
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Подземелье зачищено").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Печать снята — выход открыт").withStyle(ChatFormatting.GREEN)));
                p.level().playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1f, 1.2f);
            }
        }
    }

    private static BossAltarBlockEntity altar(ServerLevel sl, BlockPos pos) {
        BlockEntity be = sl.isLoaded(pos) ? sl.getBlockEntity(pos) : null;
        return be instanceof BossAltarBlockEntity a ? a : null;
    }

    private static void drop(UUID id) {
        SEALS.remove(id);
        LAST.remove(id);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Seal s = SEALS.get(p.getUUID());
        if (s == null || !p.isAlive()) return;
        if (p.isCreative() || p.isSpectator()) {
            drop(p.getUUID());
            return;
        }
        boolean here = p.level().dimension() == s.dim;
        if (here && s.box.contains(p.position())) {
            if (p.onGround()) LAST.put(p.getUUID(), p.position());
            return;
        }
        ServerLevel sl = p.server.getLevel(s.dim);
        BossAltarBlockEntity a = sl == null ? null : altar(sl, s.altar);
        if (sl == null || (a == null && sl.isLoaded(s.altar)) || (a != null && a.cleared())) {
            drop(p.getUUID()); // Печати больше нет или подземелье уже зачищено
            return;
        }
        Vec3 back = LAST.getOrDefault(p.getUUID(), s.landing);
        if (!s.box.contains(back)) back = s.landing;
        p.stopRiding();
        if (here) p.teleportTo(back.x, back.y, back.z);
        else p.teleportTo(sl, back.x, back.y, back.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, back.x, back.y + 1, back.z, 20, 0.4, 0.8, 0.4, 0.02);
        sl.playSound(null, BlockPos.containing(back), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1f, 0.5f);
        p.displayClientMessage(Component.literal("⛓ Печать подземелья не выпускает — сначала одолей хранителя").withStyle(ChatFormatting.RED), true);
    }

    /** Вещи погибшего внутри уходят в Сундук павших у Печати. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        Seal s = SEALS.remove(p.getUUID());
        LAST.remove(p.getUUID());
        if (s == null || e.getDrops().isEmpty() || p.level().dimension() != s.dim) return;
        BossAltarBlockEntity a = altar(p.serverLevel(), s.altar);
        if (a == null || a.cleared()) return;
        List<ItemStack> items = new ArrayList<>();
        for (ItemEntity it : e.getDrops()) if (!it.getItem().isEmpty()) items.add(it.getItem().copy());
        e.getDrops().clear();
        if (items.isEmpty()) return;
        a.addFallen(items);
        p.sendSystemMessage(Component.literal("☠ Выпавшие вещи (" + items.size() + ") унесло в Сундук павших у Печати. "
                + "Он откроется, когда хранитель будет повержен.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        drop(e.getEntity().getUUID()); // при входе Печать снова найдёт игрока внутри
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        drop(e.getEntity().getUUID());
    }

    /** Печать, к которой привязан сундук павших; null — обычный сундук. */
    private static BossAltarBlockEntity chestAltar(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || !(sl.getBlockEntity(pos) instanceof ChestBlockEntity c)) return null;
        if (!c.getPersistentData().contains(CHEST_TAG)) return null;
        return altar(sl, BlockPos.of(c.getPersistentData().getLong(CHEST_TAG)));
    }

    @SubscribeEvent
    public static void onOpen(PlayerInteractEvent.RightClickBlock e) {
        BossAltarBlockEntity a = chestAltar(e.getLevel(), e.getPos());
        if (a == null || a.cleared() || e.getEntity().isCreative()) return;
        e.setCanceled(true);
        int n = a.fallenCount();
        e.getEntity().displayClientMessage(Component.literal("⛓ Сундук павших запечатан, пока жив хранитель"
                + (n > 0 ? " • внутри вещей: " + n : "")).withStyle(ChatFormatting.DARK_PURPLE), true);
        e.getLevel().playSound(null, e.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 0.8f);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        BossAltarBlockEntity a = chestAltar((Level) e.getLevel(), e.getPos());
        if (a == null || a.cleared() || e.getPlayer().isCreative()) return;
        e.setCanceled(true);
        e.getPlayer().displayClientMessage(Component.literal("⛓ Сундук павших скован Печатью").withStyle(ChatFormatting.DARK_PURPLE), true);
    }
}
