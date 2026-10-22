package com.andrey.litrpg.city;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.network.OpenServicePacket;
import com.andrey.litrpg.npc.NpcTrades;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.npc.Wallet;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Постоялые дворы: в каждом городе — свой, с несколькими комнатами. Комнату снимают у хозяина
 * на сутки, неделю или месяц. Снятая комната — место игрока в городе: дверь открывается только ему,
 * внутри можно ломать и ставить блоки без гнева стражи, хранить вещи, спать; в каждой комнате —
 * сундук Края. Истёкшая комната ждёт хозяина ещё 3 суток, потом её может снять другой.
 * Комнаты описаны точками шаблона (anchors: inn_room_N_lo / _hi / _door).
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Inns {
    private Inns() {}

    public static final long DAY = 24000, GRACE = DAY * 3, MAX_AHEAD = DAY * 60;

    /** Названия постоялых дворов (те же — на вывесках в tools/mapbuild_inns.py). */
    public static final Map<String, String> NAMES = Map.of(
            "arcadia", "Золотой грифон", "haven", "Пьяный кракен", "sylvendar", "Лунный приют", "khazad", "Наковальня и кружка",
            "ghardum", "Кабаний клык", "wolfden", "Тёплая шкура", "ashgrad", "Тлеющий уголь", "dawnfort", "Рассветный рог",
            "northwatch", "Ледяной очаг", "blackbastion", "Чёрный ворон");

    public record Room(String city, int n, BlockPos lo, BlockPos hi, BlockPos door) {
        public boolean contains(BlockPos p) {
            return p.getX() >= lo.getX() && p.getX() <= hi.getX() && p.getY() >= lo.getY() && p.getY() <= hi.getY()
                    && p.getZ() >= lo.getZ() && p.getZ() <= hi.getZ();
        }

        boolean isDoor(BlockPos p) {
            return p.getX() == door.getX() && p.getZ() == door.getZ() && (p.getY() == door.getY() || p.getY() == door.getY() + 1);
        }

        String key() {
            return city + "#" + n;
        }
    }

    private static final Map<String, List<Room>> ROOMS = new HashMap<>();

    /** Комнаты постоялого двора города (из точек шаблона). */
    public static List<Room> rooms(String city) {
        return ROOMS.computeIfAbsent(city, id -> {
            List<Room> out = new ArrayList<>();
            WorldMap m = WorldMap.get();
            for (int n = 1; n <= 24; n++) {
                int[] lo = m.anchor(id, "inn_room_" + n + "_lo"), hi = m.anchor(id, "inn_room_" + n + "_hi"), d = m.anchor(id, "inn_room_" + n + "_door");
                if (lo == null || hi == null || d == null) break;
                out.add(new Room(id, n, new BlockPos(Math.min(lo[0], hi[0]), Math.min(lo[1], hi[1]), Math.min(lo[2], hi[2])),
                        new BlockPos(Math.max(lo[0], hi[0]), Math.max(lo[1], hi[1]), Math.max(lo[2], hi[2])), new BlockPos(d[0], d[1], d[2])));
            }
            return out;
        });
    }

    // ================= аренда (хранится в мире) =================

    public static final class Rents extends SavedData {
        private static final String NAME = "litrpg_inns";
        record Rent(UUID owner, String name, long until) {}

        final Map<String, Rent> rents = new HashMap<>();

        static Rents get(ServerLevel level) {
            ServerLevel ow = level.getServer().overworld();
            return ow.getDataStorage().computeIfAbsent(Rents::load, Rents::new, NAME);
        }

        static Rents load(CompoundTag tag) {
            Rents r = new Rents();
            CompoundTag all = tag.getCompound("rents");
            for (String k : all.getAllKeys()) {
                CompoundTag t = all.getCompound(k);
                r.rents.put(k, new Rent(t.getUUID("owner"), t.getString("name"), t.getLong("until")));
            }
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            CompoundTag all = new CompoundTag();
            rents.forEach((k, v) -> {
                CompoundTag t = new CompoundTag();
                t.putUUID("owner", v.owner);
                t.putString("name", v.name);
                t.putLong("until", v.until);
                all.put(k, t);
            });
            tag.put("rents", all);
            return tag;
        }

        /** Чья комната: действующая аренда или ещё не истёкшая отсрочка; иначе null (свободна). */
        Rent holder(Room r, long now) {
            Rent x = rents.get(r.key());
            if (x == null) return null;
            if (now > x.until + GRACE) {
                rents.remove(r.key());
                setDirty();
                return null;
            }
            return x;
        }
    }

    private static Room roomAt(Level level, BlockPos pos, boolean door) {
        if (!SystemWorld.isOverworld(level)) return null;
        WorldMap.City c = WorldMap.get().cityAt(pos.getX() + 0.5, pos.getZ() + 0.5);
        if (c == null) return null;
        for (Room r : rooms(c.id())) if (door ? r.isDoor(pos) : r.contains(pos)) return r;
        return null;
    }

    /** Игрок внутри своей снятой комнаты (срок не истёк): здесь он хозяин — закон города не трогает. */
    public static boolean ownsRoomAt(Player p, BlockPos pos) {
        if (!(p.level() instanceof ServerLevel sl)) return false;
        Room r = roomAt(sl, pos, false);
        if (r == null) return false;
        Rents.Rent x = Rents.get(sl).holder(r, sl.getGameTime());
        return x != null && x.owner.equals(p.getUUID()) && sl.getGameTime() <= x.until;
    }

    private static Room roomOf(ServerLevel sl, String city, UUID who) {
        long now = sl.getGameTime();
        Rents data = Rents.get(sl);
        for (Room r : rooms(city)) {
            Rents.Rent x = data.holder(r, now);
            if (x != null && x.owner.equals(who)) return r;
        }
        return null;
    }

    static String left(long ticks) {
        long hours = Math.max(0, ticks) / 1000;   // игровой час — 1000 тиков
        long d = hours / 24, h = hours % 24;
        return d > 0 ? d + " сут. " + h + " ч." : h + " ч.";
    }

    /** Цена суток в медяках: в городах выше уровнем дороже; репутация — скидка или наценка. */
    static int dayPrice(ServerPlayer p, RaceNpcEntity npc, WorldMap.City c) {
        float base = 8 + c.level() / 4f;
        return Math.max(1, Math.round(base * NpcTrades.repPriceMult(StatsManager.get(p).getRep(npc.npcType().faction))));
    }

    private static final int[][] TERMS = {{1, 1}, {7, 6}, {30, 22}};   // суток → цена в сутках

    // ================= окно хозяина =================

    public static void open(ServerPlayer p, RaceNpcEntity npc) {
        ServerLevel sl = p.serverLevel();
        String who = Services.shortName(npc);
        WorldMap.City c = SystemWorld.isOverworld(sl) ? WorldMap.get().cityAt(npc.getX(), npc.getZ()) : null;
        if (c == null || rooms(c.id()).isEmpty()) {
            StatsManager.message(p, Component.literal(who + ": «Все комнаты на ремонте, приходи позже».").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (StatsManager.get(p).getRep(npc.npcType().faction) < Faction.Tier.NEUTRAL.from) {
            StatsManager.message(p, Component.literal(who + ": «Таким, как ты, комнат не сдаю».").withStyle(ChatFormatting.RED));
            return;
        }
        long now = sl.getGameTime();
        Rents data = Rents.get(sl);
        Room mine = roomOf(sl, c.id(), p.getUUID());
        int day = dayPrice(p, npc, c);
        int money = Wallet.total(p);
        List<OpenServicePacket.Entry> rows = new ArrayList<>();
        if (mine != null) {
            Rents.Rent x = data.holder(mine, now);
            boolean active = now <= x.until;
            rows.add(new OpenServicePacket.Entry("show", "Твоя комната №" + mine.n(),
                    active ? "Срок: ещё " + left(x.until - now) : "Срок вышел! Комната ждёт тебя ещё " + left(x.until + GRACE - now),
                    "", "Где она?", active ? 0x4CAF50 : 0xE05040, true));
            for (int[] t : TERMS) {
                int price = day * t[1];
                boolean fits = Math.max(x.until, now) + t[0] * DAY - now <= MAX_AHEAD;
                rows.add(new OpenServicePacket.Entry("rent:" + t[0], "Продлить на " + t[0] + " " + days(t[0]),
                        fits ? "Комната №" + mine.n() + " останется за тобой" : "Дальше 60 суток вперёд не сдаю", GearData.coins(price), "Продлить", 0xFFB300,
                        fits && money >= price));
            }
        } else {
            int free = 0;
            for (Room r : rooms(c.id())) if (data.holder(r, now) == null) free++;
            for (int[] t : TERMS) {
                int price = day * t[1];
                rows.add(new OpenServicePacket.Entry("rent:" + t[0], "Снять комнату на " + t[0] + " " + days(t[0]),
                        free > 0 ? "Свободно комнат: " + free : "Все комнаты заняты", GearData.coins(price), "Снять",
                        0xFFB300, free > 0 && money >= price));
            }
        }
        for (Room r : rooms(c.id())) {
            Rents.Rent x = data.holder(r, now);
            String state = x == null ? "свободна" : x.owner.equals(p.getUUID()) ? "твоя" : now <= x.until ? "занята: " + x.name : "ждёт постояльца: " + x.name;
            rows.add(new OpenServicePacket.Entry("", "Комната №" + r.n(), state, "", "", x == null ? 0x4CAF50 : 0x8A8A8A, false));
        }
        String intro = who + ": «Добро пожаловать в «" + NAMES.getOrDefault(c.id(), "Приют путника") + "»! Снятая комната — твоё место в городе: "
                + "ставь и ломай что хочешь, храни вещи, спи. В каждой — сундук Края. Дверь откроется только тебе». Сутки — 20 минут игры.";
        Services.send(p, npc, "Постоялый двор", intro, rows);
    }

    private static String days(int n) {
        return n % 10 == 1 && n % 100 != 11 ? "сутки" : "суток";
    }

    public static void choose(ServerPlayer p, RaceNpcEntity npc, String key) {
        ServerLevel sl = p.serverLevel();
        WorldMap.City c = SystemWorld.isOverworld(sl) ? WorldMap.get().cityAt(npc.getX(), npc.getZ()) : null;
        if (c == null) return;
        long now = sl.getGameTime();
        Rents data = Rents.get(sl);
        Room mine = roomOf(sl, c.id(), p.getUUID());
        if (key.equals("show")) {
            if (mine != null) show(p, mine);
            return;
        }
        if (!key.startsWith("rent:")) return;
        int days;
        try {
            days = Integer.parseInt(key.substring(5));
        } catch (NumberFormatException e) {
            return;
        }
        int mult = 0;
        for (int[] t : TERMS) if (t[0] == days) mult = t[1];
        if (mult == 0) return;
        int price = dayPrice(p, npc, c) * mult;
        Room room = mine;
        if (room == null) {
            for (Room r : rooms(c.id())) {
                if (data.holder(r, now) == null) {
                    room = r;
                    break;
                }
            }
            if (room == null) {
                StatsManager.message(p, Component.literal("Свободных комнат нет.").withStyle(ChatFormatting.RED));
                return;
            }
        }
        Rents.Rent old = data.holder(room, now);
        long from = old == null ? now : Math.max(old.until, now);
        if (from + days * DAY - now > MAX_AHEAD) return;
        if (!Wallet.pay(p, price)) {
            StatsManager.message(p, Component.literal("Не хватает денег: нужно " + GearData.coins(price) + ".").withStyle(ChatFormatting.RED));
            return;
        }
        data.rents.put(room.key(), new Rents.Rent(p.getUUID(), p.getName().getString(), from + days * DAY));
        data.setDirty();
        Speech.say(npc, mine == null ? "Комната №" + room.n() + " — твоя. Ключ не потеряй!" : "Продлено! Живи на здоровье.", 60);
        npc.playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
        sl.playSound(null, p.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 1.4f);
        StatsManager.message(p, Component.literal("🛏 «" + NAMES.getOrDefault(c.id(), "Постоялый двор") + "», комната №" + room.n() + ": срок — "
                + left(from + days * DAY - now) + " Плата — " + GearData.coins(price) + ".").withStyle(ChatFormatting.GREEN));
        if (mine == null) show(p, room);
        open(p, npc);
    }

    /** Подсветить дверь своей комнаты на 10 секунд. */
    private static void show(ServerPlayer p, Room r) {
        // этажи: трапезная внизу (5 блоков), дальше по 4 блока на этаж с комнатами
        int[] inn = WorldMap.get().anchor(r.city(), "inn");
        int floor = inn == null ? 0 : 2 + Math.max(0, (r.door().getY() - inn[1] - 6) / 4);
        StatsManager.message(p, Component.literal("Комната №" + r.n() + (floor > 0 ? " — " + floor + "-й этаж" : "")
                + ", дверь подсвечена.").withStyle(ChatFormatting.AQUA));
        ServerLevel sl = p.serverLevel();
        Scheduler.repeat(0, 10, 20, i -> {
            if (!p.isAlive() || p.level() != sl) return false;
            sl.sendParticles(p, ParticleTypes.END_ROD, true, r.door().getX() + 0.5, r.door().getY() + 1, r.door().getZ() + 0.5, 6, 0.2, 0.6, 0.2, 0.01);
            return true;
        });
    }

    // ================= замки и порядок =================

    /** Дверь комнаты открывается только постояльцу. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onUseDoor(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getLevel() instanceof ServerLevel sl) || !(e.getEntity() instanceof ServerPlayer p)) return;
        if (!(sl.getBlockState(e.getPos()).getBlock() instanceof DoorBlock)) return;
        Room r = roomAt(sl, e.getPos(), true);
        if (r == null || p.isCreative() || p.isSpectator()) return;
        long now = sl.getGameTime();
        Rents.Rent x = Rents.get(sl).holder(r, now);
        if (x != null && x.owner.equals(p.getUUID()) && now <= x.until) return;
        e.setCanceled(true);
        e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
        String why = x == null ? "Комната №" + r.n() + " заперта. Сними её у хозяина постоялого двора."
                : x.owner.equals(p.getUUID()) ? "Срок аренды комнаты №" + r.n() + " истёк — продли у хозяина."
                : "Комната №" + r.n() + " занята. Здесь живёт " + x.name + ".";
        p.displayClientMessage(Component.literal("🔒 " + why).withStyle(ChatFormatting.GOLD), true);
        sl.playSound(null, e.getPos(), SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1.2f);
    }

    /** При входе — напомнить о сроке комнат, которые скоро истекут или уже истекли. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        Rents data = Rents.get(sl);
        for (var en : List.copyOf(data.rents.entrySet())) {
            Rents.Rent x = en.getValue();
            if (!x.owner.equals(p.getUUID())) continue;
            String city = en.getKey().substring(0, en.getKey().indexOf('#'));
            WorldMap.City c = WorldMap.get().city(city);
            String where = "«" + NAMES.getOrDefault(city, "Постоялый двор") + "»" + (c != null ? " (" + c.name() + ")" : "");
            if (now > x.until + GRACE) continue;
            if (now > x.until) {
                StatsManager.message(p, Component.literal("🛏 Срок комнаты в " + where + " истёк. Она ждёт тебя ещё " + left(x.until + GRACE - now)
                        + " — потом её сдадут другому.").withStyle(ChatFormatting.GOLD));
            } else if (x.until - now < DAY) {
                StatsManager.message(p, Component.literal("🛏 Комната в " + where + " снята ещё на " + left(x.until - now) + ".")
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
