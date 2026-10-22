package com.andrey.litrpg.quest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.LootRolls;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.party.DamageTracker;
import com.andrey.litrpg.party.Party;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.rep.RepEvents;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Title;
import com.andrey.litrpg.world.Dungeon;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Прогресс заданий, выдача наград и действия игрока из окна заданий. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class QuestEvents {
    private QuestEvents() {}

    public static long day(ServerPlayer p) {
        return p.level().getDayTime() / 24000L;
    }

    // ================= прогресс =================

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || dead instanceof Player || dead instanceof LitBoss) return;
        ServerPlayer top = DamageTracker.topContributor(dead, event.getSource());
        if (top == null) return;
        // засчитывается убийце и его товарищам по группе рядом
        List<ServerPlayer> who = new ArrayList<>();
        who.add(top);
        Party party = Party.of(top);
        if (party != null) {
            for (ServerPlayer m : party.online(top.getServer())) {
                if (m != top && m.level() == dead.level() && m.distanceToSqr(dead) < 48 * 48) who.add(m);
            }
        }
        for (ServerPlayer p : who) {
            boolean changed = false;
            for (Quest q : StatsManager.get(p).quests().tracked()) {
                if (q.type != Quest.Type.KILL || q.isComplete()) continue;
                QuestTarget t = QuestTarget.byKey(q.target);
                // охота «у деревни»: считаются только твари рядом с местом
                if (q.hasPos && q.rad > 0 && dead.distanceToSqr(q.px + 0.5, dead.getY(), q.pz + 0.5) > (double) q.rad * q.rad) continue;
                boolean match = t != null ? t.matches(dead) : q.target.startsWith("species:")
                        && dead instanceof com.andrey.litrpg.entity.wild.WildMob w && w.spec().id.equals(q.target.substring(8));
                if (match) {
                    q.have++;
                    changed = true;
                    if (!q.isComplete()) {
                        p.displayClientMessage(Component.literal(q.title + ": " + q.have + "/" + q.need).withStyle(ChatFormatting.AQUA), true);
                    }
                }
            }
            if (changed) checkAll(p);
        }
    }

    /** Хранитель повержен (вызывается для всех игроков рядом). */
    public static void onBossKill(ServerPlayer p, String bossId) {
        boolean changed = false;
        for (Quest q : StatsManager.get(p).quests().tracked()) {
            if (q.type == Quest.Type.BOSS && q.target.equals(bossId) && !q.isComplete()) {
                q.have = q.need;
                changed = true;
            }
        }
        if (changed) checkAll(p);
    }

    /** Разговор с жителем народа: задание «Народы мира». */
    @SubscribeEvent
    public static void onTalk(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof com.andrey.litrpg.npc.RaceNpcEntity)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        boolean changed = false;
        for (Quest q : StatsManager.get(p).quests().tracked()) {
            if (q.type == Quest.Type.MEET && !q.isComplete()) {
                q.have = q.need;
                changed = true;
            }
        }
        if (changed) checkAll(p);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        PlayerStats s = StatsManager.get(p);
        if (!s.hasOrigin()) return;
        QuestLog log = s.quests();
        boolean changed = false;
        // сюжет
        if (log.story == null && log.storyStep < Story.length()) {
            log.story = Story.make(log.storyStep);
            announce(p, "Новая глава сюжета", log.story.title);
            changed = true;
        }
        // уровень
        for (Quest q : log.tracked()) {
            if (q.type == Quest.Type.LEVEL && !q.isComplete() && s.getLevel() >= q.need) {
                q.have = q.need;
                changed = true;
            }
        }
        // поручения Системы обновляются каждый игровой день
        long day = day(p);
        if (log.offersDay.getOrDefault(QuestLog.SYSTEM, -1L) != day) {
            log.offers.put(QuestLog.SYSTEM, QuestGen.system(p.getRandom(), s.getLevel(), log));
            log.offersDay.put(QuestLog.SYSTEM, day);
            if (p.tickCount > 200) p.displayClientMessage(Component.literal("Система: новые поручения ждут в окне заданий (K)").withStyle(ChatFormatting.AQUA), false);
            changed = true;
        }
        if (changed) checkAll(p);
    }

    /** Выдать награды за все выполненные задания (кроме «принести» — их сдают кнопкой). */
    public static void checkAll(ServerPlayer p) {
        QuestLog log = StatsManager.get(p).quests();
        if (log.story != null && log.story.isComplete() && log.story.type != Quest.Type.COLLECT) {
            Quest done = log.story;
            log.story = null;
            log.storyStep++;
            complete(p, done);
            if (log.storyStep >= Story.length()) {
                StatsManager.grantTitle(p, Title.CHOSEN_ONE);
                StatsManager.count(p, Title.Counter.STORY, 1);
            }
        }
        for (Quest q : new ArrayList<>(log.active)) {
            if (q.isComplete() && q.type != Quest.Type.COLLECT) {
                log.active.remove(q);
                complete(p, q);
            }
        }
        StatsManager.sync(p);
    }

    private static void complete(ServerPlayer p, Quest q) {
        QuestLog log = StatsManager.get(p).quests();
        log.completed++;
        StatsManager.count(p, Title.Counter.QUESTS, 1);
        announce(p, "Задание выполнено!", q.title);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.1f);
        StringBuilder sb = new StringBuilder("Награда: ");
        if (q.xp > 0) {
            StatsManager.giveXp(p, q.xp);
            sb.append(q.xp).append(" опыта");
        }
        if (q.coins > 0) {
            for (ItemStack c : LootRolls.coinStacks(q.coins)) give(p, c);
            sb.append(", ").append(coinText(q.coins));
        }
        if (q.rep != 0 && q.faction != null) {
            RepEvents.change(p, q.faction, q.rep);
            sb.append(", репутация «").append(q.faction.title).append("» +").append(q.rep);
        }
        if (q.gearRarity >= 0) {
            Rarity r = Rarity.byId(q.gearRarity);
            ItemStack gear = GearGen.random(p.getRandom(), Math.max(1, q.gearLevel), r);
            give(p, gear);
            sb.append(", предмет: ").append(gear.getHoverName().getString());
        }
        if (!q.bonusItem.isEmpty() && q.bonusCount > 0) {
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(q.bonusItem));
            if (it != null) {
                give(p, new ItemStack(it, q.bonusCount));
                sb.append(", ").append(it.getDescription().getString()).append(" ×").append(q.bonusCount);
            }
        }
        StatsManager.message(p, Component.literal("✔ " + q.title).withStyle(ChatFormatting.GREEN));
        StatsManager.message(p, Component.literal(sb.toString()).withStyle(ChatFormatting.GOLD));
        if (q.source == Quest.Source.CHAIN) StoryEngine.advance(p, q);
    }

    public static String coinText(int copper) {
        int g = copper / 81, s = copper % 81 / 9, c = copper % 9;
        StringBuilder b = new StringBuilder();
        if (g > 0) b.append(g).append(" зол. ");
        if (s > 0) b.append(s).append(" сер. ");
        if (c > 0 || b.length() == 0) b.append(c).append(" мед.");
        return b.toString().trim();
    }

    private static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private static void announce(ServerPlayer p, String title, String sub) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(ChatFormatting.GOLD)));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub).withStyle(ChatFormatting.WHITE)));
    }

    // ================= действия из окна =================

    /** Предложения народа (создаются раз в игровой день). */
    public static void ensureFactionOffers(ServerPlayer p, Faction f) {
        PlayerStats s = StatsManager.get(p);
        QuestLog log = s.quests();
        long day = day(p);
        if (log.offersDay.getOrDefault(f.key, -1L) != day) {
            log.offers.put(f.key, QuestGen.faction(p.getRandom(), s.getLevel(), f, log));
            log.offersDay.put(f.key, day);
        }
    }

    public static void accept(ServerPlayer p, String source, String id) {
        PlayerStats s = StatsManager.get(p);
        QuestLog log = s.quests();
        List<Quest> list = log.offers.get(source);
        if (list == null) return;
        Quest q = null;
        for (Quest o : list) if (o.id.equals(id)) q = o;
        if (q == null) return;
        if (q.source == Quest.Source.CHAIN) {
            Quest fresh = StoryEngine.refresh(p, q);
            list.remove(q);
            if (fresh == null) {
                StatsManager.message(p, Component.literal("Этот сюжет уже идёт или пройден.").withStyle(ChatFormatting.GRAY));
                StatsManager.sync(p);
                return;
            }
            q = fresh;
            list.add(q);
        } else if (log.regularCount() >= QuestLog.MAX_ACTIVE) {
            StatsManager.message(p, Component.literal("Слишком много заданий (максимум " + QuestLog.MAX_ACTIVE + "). Выполните или откажитесь от какого-нибудь.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (q.faction != null && s.getRep(q.faction) < Faction.Tier.UNFRIENDLY.from) {
            StatsManager.message(p, Component.literal("«" + q.faction.title + "» не доверяет вам поручений.").withStyle(ChatFormatting.RED));
            return;
        }
        list.remove(q);
        log.active.add(q);
        StatsManager.message(p, Component.literal("Принято задание: ").append(Component.literal(q.title).withStyle(ChatFormatting.GOLD)));
        CityQuests.onAccept(p, q);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        StatsManager.sync(p);
    }

    public static void abandon(ServerPlayer p, String id) {
        QuestLog log = StatsManager.get(p).quests();
        Quest gone = log.find(id);
        if (gone != null && gone != log.story) CityQuests.onAbandon(p, gone);
        if (log.active.removeIf(q -> q.id.equals(id))) {
            StatsManager.message(p, Component.literal("Задание отменено.").withStyle(ChatFormatting.GRAY));
            StatsManager.sync(p);
        }
    }

    /** Сдать предметы для задания «Принести». */
    public static void turnIn(ServerPlayer p, String id) {
        QuestLog log = StatsManager.get(p).quests();
        Quest q = log.find(id);
        if (q == null || q.type != Quest.Type.COLLECT) return;
        Item item = q.item();
        if (item == null) return;
        int have = p.getInventory().countItem(item);
        if (have < q.need) {
            StatsManager.message(p, Component.literal("Не хватает: " + item.getDescription().getString() + " " + have + "/" + q.need)
                    .withStyle(ChatFormatting.RED));
            return;
        }
        int left = q.need;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(item)) {
                int take = Math.min(left, st.getCount());
                st.shrink(take);
                left -= take;
            }
        }
        q.have = q.need;
        if (log.story == q) {
            log.story = null;
            log.storyStep++;
        } else {
            log.active.remove(q);
        }
        complete(p, q);
        StatsManager.sync(p);
    }

    /** Указать путь к подземелью, где живёт босс из задания. */
    public static void locate(ServerPlayer p, String id) {
        Quest q = StatsManager.get(p).quests().find(id);
        if (q != null && StoryEngine.locate(p, q)) return;
        if (q != null && q.hasPos) {
            CityQuests.waypoint(p, q);
            return;
        }
        if (q == null || q.type != Quest.Type.BOSS) return;
        Dungeon d = Dungeon.byBoss(q.target);
        if (d == null) return;
        var level = p.serverLevel();
        // в «Мире Системы» подземелья стоят на карте — координаты известны заранее
        if (com.andrey.litrpg.world.map.SystemWorld.is(level) || level.dimension() == net.minecraft.world.level.Level.NETHER) {
            for (var poi : com.andrey.litrpg.world.map.WorldMap.get().pois) {
                if (!poi.extra().equals(d.key) || !poi.type().endsWith("dungeon")) continue;
                boolean nether = poi.type().startsWith("nether");
                int dx = poi.x() - p.getBlockX(), dz = poi.z() - p.getBlockZ();
                int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
                StatsManager.message(p, Component.literal("◆ " + d.title + " (ур. " + d.level + "): ").withStyle(s -> s.withColor(d.color))
                        .append(Component.literal((nether ? "в Преисподней: " : "") + "X " + poi.x() + ", Z " + poi.z()
                                + (nether == (level.dimension() == net.minecraft.world.level.Level.NETHER)
                                ? " — " + dist + " бл. на " + direction(dx, dz) + ". Метка добавлена на карту." : ""))
                                .withStyle(ChatFormatting.WHITE)));
                if (nether == (level.dimension() == net.minecraft.world.level.Level.NETHER)) {
                    com.andrey.litrpg.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                            new com.andrey.litrpg.network.WaypointPacket(poi.x(), poi.z()));
                }
                return;
            }
        }
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            StatsManager.message(p, Component.literal("Подземелья находятся в Верхнем мире.").withStyle(ChatFormatting.RED));
            return;
        }
        var reg = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var holder = reg.getHolder(d.structure());
        if (holder.isEmpty()) return;
        StatsManager.message(p, Component.literal("Система ищет «" + d.title + "»...").withStyle(ChatFormatting.GRAY));
        var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder.get()),
                p.blockPosition(), 120, false);
        if (found == null) {
            StatsManager.message(p, Component.literal("Подземелье «" + d.title + "» не найдено поблизости.").withStyle(ChatFormatting.RED));
            return;
        }
        BlockPos pos = found.getFirst();
        int dx = pos.getX() - p.getBlockX(), dz = pos.getZ() - p.getBlockZ();
        int dist = (int) Math.sqrt((double) dx * dx + (double) dz * dz);
        StatsManager.message(p, Component.literal("◆ " + d.title + " (ур. " + d.level + "): ").withStyle(s -> s.withColor(d.color))
                .append(Component.literal("X " + pos.getX() + ", Z " + pos.getZ() + " — " + dist + " бл. на " + direction(dx, dz)
                        + ". Метка добавлена на карту.").withStyle(ChatFormatting.WHITE)));
        com.andrey.litrpg.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new com.andrey.litrpg.network.WaypointPacket(pos.getX(), pos.getZ()));
    }

    private static String direction(int dx, int dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        if (a < 0) a += 360;
        String[] dirs = {"север", "северо-восток", "восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад"};
        return dirs[(int) Math.round(a / 45) % 8];
    }
}
