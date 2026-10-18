package com.andrey.litrpg.city;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.npc.NpcRegistry;
import com.andrey.litrpg.npc.NpcType;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.rep.RepEvents;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Закон городов «Мира Системы». В городе нельзя ломать и ставить блоки,
 * вскрывать чужие сундуки, поджигать и разливать жидкости. Нарушителю —
 * предупреждение, потеря репутации и розыск: стража (очень сильная)
 * нападает на него, пока срок розыска не истечёт или он не покинет город.
 * Взрывы в городе не разрушают постройки. Стража и горожане пополняются.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class CityLaw {
    public static final String WANTED = "litrpg_wanted_until";
    public static final String OFFENSES = "litrpg_offenses";
    public static final String LAST_OFFENSE = "litrpg_offense_time";

    private CityLaw() {}

    public static WorldMap.City cityAt(LivingEntity e) {
        if (!SystemWorld.isOverworld(e.level())) return null;
        return WorldMap.get().cityAt(e.getX(), e.getZ());
    }

    private static WorldMap.City cityAt(ServerLevel level, BlockPos pos) {
        if (!SystemWorld.isOverworld(level)) return null;
        return WorldMap.get().cityAt(pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    public static boolean isWanted(Player p) {
        return p.getPersistentData().getLong(WANTED) > p.level().getGameTime() && !p.isCreative() && !p.isSpectator();
    }

    private static boolean exempt(Player p) {
        return p.isCreative() || p.isSpectator() || p.hasPermissions(2) && p.getPersistentData().getBoolean("litrpg_builder");
    }

    /** Проступок в городе: предупреждение → штраф → розыск. */
    public static void offense(ServerPlayer p, WorldMap.City city, String what, int severity) {
        long now = p.level().getGameTime();
        var data = p.getPersistentData();
        int n = data.getInt(OFFENSES);
        if (now - data.getLong(LAST_OFFENSE) > 20 * 60 * 5) n = 0; // прощение через 5 минут
        n += severity;
        data.putInt(OFFENSES, n);
        data.putLong(LAST_OFFENSE, now);
        Faction f = city.faction() == null ? Faction.KINGDOM : Faction.byKey(city.faction());
        String guardName = city.name();
        if (n <= 1) {
            p.sendSystemMessage(Component.literal("[Стража " + guardName + "] ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(what + " Это предупреждение.").withStyle(ChatFormatting.YELLOW)));
            if (f != null) RepEvents.change(p, f, -5);
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.NEUTRAL, 1f, 0.8f);
            return;
        }
        int seconds = n >= 4 ? 300 : n >= 3 ? 180 : 60;
        if (f != null) RepEvents.change(p, f, n >= 3 ? -30 : -15);
        data.putLong(WANTED, now + seconds * 20L);
        p.sendSystemMessage(Component.literal("[Стража " + guardName + "] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(what + " Ты в розыске на " + seconds / 60 + " мин.! Беги из города — или сдавайся.")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2f, 0.8f);
        alertGuards(p, city);
    }

    private static void alertGuards(ServerPlayer p, WorldMap.City city) {
        for (RaceNpcEntity g : p.serverLevel().getEntitiesOfClass(RaceNpcEntity.class,
                new AABB(city.x() - city.r() - 32, p.getY() - 40, city.z() - city.r() - 32, city.x() + city.r() + 32, p.getY() + 40,
                        city.z() + city.r() + 32), e -> e.npcType().isGuard())) {
            g.setTarget(p);
        }
    }

    // ================= нарушения =================

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || exempt(p)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        WorldMap.City c = cityAt(sl, event.getPos());
        if (c == null) return;
        event.setCanceled(true);
        offense(p, c, "Ломать городские постройки запрещено!", 1);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || exempt(p)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        WorldMap.City c = cityAt(sl, event.getPos());
        if (c == null) return;
        event.setCanceled(true);
        p.containerMenu.sendAllDataToRemote();
        offense(p, c, "Строить в городе без разрешения нельзя!", 1);
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || exempt(p)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        WorldMap.City c = cityAt(sl, event.getPos());
        if (c == null) return;
        var item = event.getItemStack().getItem();
        if (item instanceof BucketItem || item instanceof FlintAndSteelItem || item instanceof FireChargeItem) {
            event.setCanceled(true);
            offense(p, c, "Огонь и разлитые жидкости в городе — преступление!", 2);
            return;
        }
        var be = sl.getBlockEntity(event.getPos());
        if (be instanceof Container && !(sl.getBlockState(event.getPos()).getBlock() instanceof EnderChestBlock)) {
            event.setCanceled(true);
            offense(p, c, "Это чужое имущество!", 1);
        }
    }

    /** Нападение на горожан, стражу, жителей — сразу розыск. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || exempt(p)) return;
        LivingEntity t = event.getEntity();
        if (!(t instanceof RaceNpcEntity || t instanceof AbstractVillager || t instanceof IronGolem)) return;
        if (t instanceof RaceNpcEntity npc && npc.getTarget() == p) return; // самооборона
        WorldMap.City c = cityAt(t);
        if (c == null) return;
        var data = p.getPersistentData();
        data.putInt(OFFENSES, Math.max(2, data.getInt(OFFENSES)));
        offense(p, c, "Нападение на жителей города!", 2);
    }

    /** Взрывы не разрушают городские постройки. */
    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !SystemWorld.isOverworld(sl)) return;
        event.getAffectedBlocks().removeIf(pos -> WorldMap.get().cityAt(pos.getX(), pos.getZ()) != null);
    }

    // ================= население =================

    /** Раз в 30 с: в городах, где есть игроки, пополняется стража и горожане. */
    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel sl) || sl.getGameTime() % 600 != 0) return;
        if (!SystemWorld.isOverworld(sl)) return;
        for (WorldMap.City c : WorldMap.get().cities) {
            AABB box = new AABB(c.x() - c.r(), c.y() - 30, c.z() - c.r(), c.x() + c.r(), c.y() + 60, c.z() + c.r());
            if (sl.getEntitiesOfClass(ServerPlayer.class, box.inflate(64)).isEmpty()) continue;
            Faction f = c.faction() == null ? Faction.KINGDOM : Faction.byKey(c.faction());
            int wantGuards = c.r() >= 140 ? 10 : 6;
            int wantCitizens = c.r() >= 140 ? 14 : 8;
            var npcs = sl.getEntitiesOfClass(RaceNpcEntity.class, box);
            long guards = npcs.stream().filter(n -> n.npcType().isGuard()).count();
            long citizens = npcs.stream().filter(n -> n.npcType().role == NpcType.Role.CITIZEN).count();
            if (guards < wantGuards) spawn(sl, c, NpcType.guardOf(f), (int) Math.min(3, wantGuards - guards));
            if (citizens < wantCitizens) spawn(sl, c, NpcType.citizenOf(f), (int) Math.min(3, wantCitizens - citizens));
        }
    }

    private static void spawn(ServerLevel sl, WorldMap.City c, NpcType type, int count) {
        for (int i = 0; i < count; i++) {
            for (int attempt = 0; attempt < 10; attempt++) {
                double a = sl.random.nextDouble() * Math.PI * 2;
                double r = c.r() * (0.15 + sl.random.nextDouble() * 0.6);
                int x = (int) (c.x() + Math.cos(a) * r), z = (int) (c.z() + Math.sin(a) * r);
                if (!sl.isLoaded(new BlockPos(x, c.y(), z))) continue;
                int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                if (Math.abs(y - c.y()) > 6) continue; // только на улицах, не на крышах
                BlockPos pos = new BlockPos(x, y, z);
                if (!sl.getBlockState(pos).isAir() || !sl.getBlockState(pos.above()).isAir()) continue;
                RaceNpcEntity e = NpcRegistry.TYPES.get(type).get().create(sl);
                if (e == null) break;
                e.moveTo(x + 0.5, y, z + 0.5, sl.random.nextFloat() * 360, 0);
                e.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
                e.restrictTo(new BlockPos(c.x(), c.y(), c.z()), (int) (c.r() * 0.85));
                sl.addFreshEntity(e);
                break;
            }
        }
    }

    /** Розыск снимается, если преступник далеко от городов. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 100 != 0) return;
        if (!isWanted(p)) return;
        WorldMap.City c = cityAt(p);
        if (c == null) {
            double best = 1e9;
            for (WorldMap.City city : WorldMap.get().cities) best = Math.min(best, Math.hypot(p.getX() - city.x(), p.getZ() - city.z()) - city.r());
            if (best > 120) {
                p.getPersistentData().remove(WANTED);
                StatsManager.message(p, Component.literal("Стража потеряла твой след.").withStyle(ChatFormatting.GRAY));
            }
        }
    }
}
