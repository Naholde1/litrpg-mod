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
import net.minecraft.world.level.block.state.BlockState;
import com.andrey.litrpg.npc.Craft;
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
 * Закон городов «Мира Системы». Город можно разрушать и обворовывать — добыча
 * достаётся вору, а строители потом всё восстанавливают (CityRepairs). Но если
 * кто-то видел (стража — издалека, жители — то, что перед глазами), наказание
 * жёсткое и растёт: потеря репутации, долгий розыск, конфискация монет, стража.
 * Огонь и разлитые жидкости запрещены. Стража, горожане и мастера пополняются.
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

    public static final String DAMAGE = "litrpg_city_damage";
    public static final String DAMAGE_TIME = "litrpg_city_damage_time";
    public static final String THEFTS = "litrpg_thefts";
    public static final String THEFT_TIME = "litrpg_theft_time";

    /**
     * Кто видит игрока: стража замечает издалека, горожане и мастера — вблизи и только то,
     * что перед глазами. Красться (Shift), темнота и невидимость помогают остаться незамеченным.
     */
    public static RaceNpcEntity witness(ServerPlayer p) {
        if (p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY)) return null;
        ServerLevel sl = p.serverLevel();
        double k = p.isShiftKeyDown() ? 0.5 : 1.0;
        if (sl.getMaxLocalRawBrightness(p.blockPosition()) < 7) k *= 0.6;
        RaceNpcEntity best = null;
        double bestD = 1e9;
        for (RaceNpcEntity n : sl.getEntitiesOfClass(RaceNpcEntity.class, p.getBoundingBox().inflate(26))) {
            if (!n.isAlive() || n.npcType().role == com.andrey.litrpg.npc.NpcType.Role.CHIEF && n.distanceTo(p) > 10) continue;
            double range = (n.npcType().isGuard() ? 24 : 13) * k;
            double d = n.distanceTo(p);
            if (d > range || !n.hasLineOfSight(p)) continue;
            if (!n.npcType().isGuard() && d > 3) {
                // горожане смотрят перед собой: угол обзора ~140°
                var look = n.getViewVector(1f);
                var to = p.getEyePosition().subtract(n.getEyePosition()).normalize();
                if (look.dot(to) < -0.35) continue;
            }
            if (d < bestD) {
                bestD = d;
                best = n;
            }
        }
        return best;
    }

    /** Попался на краже или порче: жёсткое наказание, растущее с каждым разом. */
    public static void caught(ServerPlayer p, WorldMap.City city, RaceNpcEntity who, String what) {
        long now = p.level().getGameTime();
        var data = p.getPersistentData();
        int n = now - data.getLong(THEFT_TIME) > 20 * 60 * 15 ? 0 : data.getInt(THEFTS);
        n++;
        data.putInt(THEFTS, n);
        data.putLong(THEFT_TIME, now);
        Faction f = city.faction() == null ? Faction.KINGDOM : Faction.byKey(city.faction());
        int rep = n == 1 ? 20 : n == 2 ? 45 : 90;
        if (f != null) RepEvents.change(p, f, -rep);
        int seconds = n == 1 ? 180 : n == 2 ? 360 : 600;
        data.putLong(WANTED, Math.max(data.getLong(WANTED), now + seconds * 20L));
        data.putInt(OFFENSES, Math.max(4, data.getInt(OFFENSES)));
        data.putLong(LAST_OFFENSE, now);
        // штраф: стража изымает часть монет
        int fine = 0;
        if (n >= 2) {
            float part = n == 2 ? 0.25f : 0.5f;
            var inv = p.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var st = inv.getItem(i);
                if (st.is(com.andrey.litrpg.registry.ModItems.COPPER_COIN.get()) || st.is(com.andrey.litrpg.registry.ModItems.SILVER_COIN.get())
                        || st.is(com.andrey.litrpg.registry.ModItems.GOLD_COIN.get())) {
                    int take = (int) Math.ceil(st.getCount() * part);
                    fine += take;
                    st.shrink(take);
                }
            }
        }
        String shout = who.npcType().isGuard() ? "Стоять! Именем закона!" : p.getRandom().nextBoolean() ? "Вор! Держи вора!" : "Стража! Здесь вор!";
        Speech.say(who, shout, 80);
        who.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_NO, 1.5f, 0.8f);
        p.sendSystemMessage(Component.literal("[" + city.name() + "] ").withStyle(ChatFormatting.RED)
                .append(Component.literal(what + " Тебя заметил " + who.getName().getString() + "! Репутация −" + rep
                        + ", розыск на " + seconds / 60 + " мин." + (fine > 0 ? " Стража изъяла монет: " + fine + "." : ""))
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.NEUTRAL, 2f, 0.7f);
        alertGuards(p, city);
    }

    private static boolean isStash(ServerLevel sl, BlockPos pos) {
        return sl.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BaseContainerBlockEntity be
                && be.hasCustomName() && be.getCustomName().getString().contains("Тайник");
    }

    /**
     * Кража и порча: блок ломается с добычей (кроме уже восстановленного строителями),
     * город запоминает место — строители вернут. Если кто-то видел — жёсткое наказание.
     */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        WorldMap.City c = cityAt(sl, event.getPos());
        if (c == null) return;
        CityRepairs rep = CityRepairs.get(sl);
        BlockState orig = rep.original(event.getPos());
        if (orig != null && orig.isAir()) {
            rep.forget(event.getPos()); // это блок самого игрока
            return;
        }
        if (exempt(p)) return;
        if (isStash(sl, event.getPos())) return;
        if (secretWall(c, event.getPos(), event.getState())) {
            p.displayClientMessage(Component.literal("Кладка поддаётся — за ней пустота!").withStyle(ChatFormatting.GOLD), true);
            return;
        }
        BlockState st = event.getState();
        rep.record(sl, event.getPos(), st, c.id());
        if (rep.wasRestored(event.getPos())) {
            // восстановленное строителями второй раз не обогащает
            event.setCanceled(true);
            if (sl.getBlockEntity(event.getPos()) instanceof Container box) box.clearContent();
            sl.destroyBlock(event.getPos(), false, p);
        }
        RaceNpcEntity who = witness(p);
        if (who != null) {
            caught(p, c, who, "Ты разрушаешь и обворовываешь город!");
            return;
        }
        var data = p.getPersistentData();
        long now = sl.getGameTime();
        if (now - data.getLong(DAMAGE_TIME) > 20 * 60 * 3) {
            p.displayClientMessage(Component.literal("Никто не видел… пока.").withStyle(ChatFormatting.DARK_GRAY), true);
        }
        data.putLong(DAMAGE_TIME, now);
    }

    /** Треснувшая кладка у тайника сюжета: её можно разбить без наказания. */
    public static boolean secretWall(WorldMap.City c, BlockPos pos, BlockState st) {
        var key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(st.getBlock());
        if (key == null || !key.getPath().startsWith("cracked_")) return false;
        int[] a = WorldMap.get().anchor(c.id(), "crack");
        return a != null && Math.abs(pos.getX() - a[0]) <= 3 && Math.abs(pos.getZ() - a[2]) <= 3 && Math.abs(pos.getY() - a[1]) <= 3;
    }

    /** Самовольная стройка в городе роняет репутацию; строители со временем разберут чужое. */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || exempt(p)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        WorldMap.City c = cityAt(sl, event.getPos());
        if (c == null) return;
        CityRepairs.get(sl).record(sl, event.getPos(), event.getBlockSnapshot().getReplacedBlock(), c.id());
        Faction f = c.faction() == null ? Faction.KINGDOM : Faction.byKey(c.faction());
        if (f != null) RepEvents.change(p, f, -1);
        long now = sl.getGameTime();
        if (now - p.getPersistentData().getLong("litrpg_place_note") > 20 * 120) {
            p.getPersistentData().putLong("litrpg_place_note", now);
            p.displayClientMessage(Component.literal("Самовольная стройка в городе: −репутация. Строители уберут чужие постройки.")
                    .withStyle(ChatFormatting.GRAY), true);
        }
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
            // свой сундук и ничьи тайники открывать можно
            BlockState orig = CityRepairs.get(sl).original(event.getPos());
            if (orig != null && orig.isAir() || isStash(sl, event.getPos())) return;
            RaceNpcEntity who = witness(p);
            if (who != null) {
                event.setCanceled(true);
                caught(p, c, who, "Ты лезешь в чужой сундук!");
            }
        }
    }

    /** Нападение на горожан, стражу, жителей — сразу розыск. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || exempt(p)) return;
        LivingEntity t = event.getEntity();
        if (!(t instanceof RaceNpcEntity || t instanceof AbstractVillager || t instanceof IronGolem)) return;
        if (t instanceof RaceNpcEntity npc && (npc.getTarget() == p || npc.npcType().outlaw())) return; // самооборона, разбойники
        WorldMap.City c = cityAt(t);
        if (c == null) return;
        var data = p.getPersistentData();
        data.putInt(OFFENSES, Math.max(2, data.getInt(OFFENSES)));
        offense(p, c, "Нападение на жителей города!", 2);
    }

    /** Взрывы рушат город, но без добычи — и строители потом всё отстроят. */
    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !SystemWorld.isOverworld(sl)) return;
        CityRepairs rep = CityRepairs.get(sl);
        event.getAffectedBlocks().removeIf(pos -> {
            WorldMap.City c = WorldMap.get().cityAt(pos.getX(), pos.getZ());
            if (c == null) return false;
            BlockState st = sl.getBlockState(pos);
            if (st.isAir()) return true;
            BlockState orig = rep.original(pos);
            if (orig != null && orig.isAir()) return false; // постройки игроков рушатся как обычно
            rep.record(sl, pos, st, c.id());
            if (sl.getBlockEntity(pos) instanceof Container box) box.clearContent();
            sl.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            return true;
        });
    }

    // ================= население =================

    /** Раз в 2 с — ремонт; раз в 30 с: в городах с игроками пополняются стража, горожане и мастера. */
    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel sl)) return;
        if (!SystemWorld.isOverworld(sl)) return;
        if (sl.getGameTime() % 40 == 0) CityRepairs.get(sl).tick(sl);
        if (sl.getGameTime() % 600 != 0) return;
        for (WorldMap.City c : WorldMap.get().cities) {
            AABB box = new AABB(c.x() - c.r(), c.y() - 30, c.z() - c.r(), c.x() + c.r(), c.y() + 60, c.z() + c.r());
            if (sl.getEntitiesOfClass(ServerPlayer.class, box.inflate(64)).isEmpty()) continue;
            Faction f = c.faction() == null ? Faction.KINGDOM : Faction.byKey(c.faction());
            int wantGuards = c.r() >= 140 ? 10 : 6;
            int wantCitizens = c.r() >= 140 ? 22 : 12;
            var npcs = sl.getEntitiesOfClass(RaceNpcEntity.class, box);
            long guards = npcs.stream().filter(n -> n.npcType().isGuard()).count();
            long citizens = npcs.stream().filter(n -> n.npcType().role == NpcType.Role.CITIZEN).count();
            if (guards < wantGuards) spawn(sl, c, NpcType.guardOf(f), (int) Math.min(3, wantGuards - guards));
            if (citizens < wantCitizens) spawn(sl, c, NpcType.citizenOf(f), (int) Math.min(3, wantCitizens - citizens));
            restaff(sl, c, npcs);
        }
    }

    /** Мастерская без мастера получает нового (ученик занимает место). */
    private static void restaff(ServerLevel sl, WorldMap.City c, java.util.List<RaceNpcEntity> npcs) {
        for (WorldMap.Workshop w : WorldMap.get().workshopsOf(c.id())) {
            BlockPos pos = new BlockPos(w.x(), w.y(), w.z());
            if (!sl.isLoaded(pos)) continue;
            Craft craft = Craft.byKey(w.craft());
            if (craft == null) continue;
            boolean staffed = false;
            for (RaceNpcEntity n : npcs) {
                if (n.craft() == craft && n.distanceToSqr(pos.getCenter()) < (craft == Craft.BUILDER ? 80 * 80 : 14 * 14)) {
                    staffed = true;
                    break;
                }
            }
            if (staffed) continue;
            var type = NpcRegistry.TYPES.entrySet().stream().filter(e -> e.getKey().key.equals(w.npc().replace("litrpg:", "")))
                    .map(e -> e.getValue().get()).findFirst().orElse(null);
            if (type == null) continue;
            RaceNpcEntity e = type.create(sl);
            if (e == null) continue;
            e.setCraft(craft);
            e.moveTo(w.x() + 0.5, w.y(), w.z() + 0.5, sl.random.nextFloat() * 360, 0);
            e.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
            sl.addFreshEntity(e);
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
