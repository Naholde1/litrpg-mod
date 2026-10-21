package com.andrey.litrpg.quest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.city.Speech;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.rep.RepEvents;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.util.Scheduler;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Исполнение сюжетов городов (CityStories) и засад (задания FIGHT): герои сюжета появляются у своих мест,
 * улики находятся, когда подходишь вплотную, отряды выходят, когда приближаешься, выбор делается кликом в чате.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class StoryEngine {
    public static final String NPC_TAG = "litrpg_story";
    private static final String NPC_PREFIX = "story:";
    private static final String FOE_PREFIX = "litrpg_foe:";

    private StoryEngine() {}

    // ===================================================================
    //                       предложения и шаги
    // ===================================================================

    /** Сюжет города на доске мастера: с начала или с того шага, где игрок остановился. */
    public static Quest offer(ServerPlayer p, String cityId) {
        CityStories.Chain c = CityStories.forCity(cityId);
        if (c == null) return null;
        QuestLog log = StatsManager.get(p).quests();
        if (log.chainsDone.contains(c.id)) return null;
        for (Quest q : log.active) if (c.id.equals(q.chain)) return null;
        return make(p, c, log.chainStep.getOrDefault(c.id, 0));
    }

    /** Перед принятием: предложение могло устареть (сюжет уже идёт или пройден, шаг сменился). */
    public static Quest refresh(ServerPlayer p, Quest offer) {
        CityStories.Chain c = CityStories.byId(offer.chain);
        if (c == null) return null;
        return offer(p, c.city);
    }

    static Quest make(ServerPlayer p, CityStories.Chain c, int idx) {
        idx = Math.max(0, Math.min(c.steps.length - 1, idx));
        CityStories.Step s = c.steps[idx];
        QuestLog log = StatsManager.get(p).quests();
        WorldMap.City city = WorldMap.get().city(c.city);
        Quest q = new Quest();
        q.id = "c" + log.nextId++;
        q.source = Quest.Source.CHAIN;
        q.faction = city == null || city.faction() == null ? Faction.KINGDOM : Faction.byKey(city.faction());
        q.chain = c.id;
        q.step = idx;
        q.type = s.type;
        q.title = c.title + ": " + s.title;
        q.desc = s.desc;
        q.giver = "★ Сюжет города";
        q.place = c.city;
        q.placeName = city == null ? "" : city.name();
        q.rad = s.rad;
        q.need = 1;
        int L = c.level;
        q.xp = 50 + L * 22;
        q.coins = 4 + L * 2;
        String anchor = s.anchor;
        if (s.type == Quest.Type.TALK) {
            CityStories.Npc n = c.npc(s.target);
            q.target = s.target;
            q.where = n == null ? s.target : n.name;
            anchor = n == null ? "" : n.anchor;
        } else {
            q.where = s.where;
        }
        if (s.type == Quest.Type.FIGHT) {
            q.extra = Math.max(L, Math.min(L + 6, StatsManager.get(p).getLevel())) + ";" + s.spawn;
            q.need = units(s.spawn).size();
            q.xp = Math.round(q.xp * 1.6f);
        } else if (s.type == Quest.Type.CHOICE) {
            StringBuilder sb = new StringBuilder();
            for (CityStories.Opt o : s.opts) sb.append(sb.length() > 0 ? "|" : "").append(o.label);
            q.extra = sb.toString();
            q.target = "";
        }
        int[] a = resolve(c.city, anchor);
        if (a != null) {
            q.hasPos = true;
            q.px = a[0];
            q.py = a[1];
            q.pz = a[2];
        }
        return q;
    }

    /** Место по имени: «имя», «имя+dx,dz», «world:имя». */
    public static int[] resolve(String city, String spec) {
        if (spec == null || spec.isEmpty()) return null;
        int dx = 0, dz = 0;
        String name = spec;
        int plus = offsetStart(spec);
        if (plus > 0) {
            name = spec.substring(0, plus);
            String[] d = spec.substring(spec.charAt(plus) == '+' ? plus + 1 : plus).split(",");
            try {
                dx = Integer.parseInt(d[0].trim());
                dz = Integer.parseInt(d[1].trim());
            } catch (Exception ignored) {
            }
        }
        if (name.contains(":")) {
            city = name.substring(0, name.indexOf(':'));
            name = name.substring(name.indexOf(':') + 1);
        }
        int[] a = WorldMap.get().anchor(city, name);
        if (a == null && !name.equals("center")) a = WorldMap.get().anchor(city, "center");
        if (a == null) {
            WorldMap.City c = WorldMap.get().city(city);
            if (c == null) return null;
            a = new int[]{c.x(), c.y(), c.z()};
        }
        return new int[]{a[0] + dx, a[1], a[2] + dz};
    }

    /** Где начинается сдвиг «+dx,dz» / «-dx,dz» после имени места. */
    private static int offsetStart(String spec) {
        for (int i = 1; i < spec.length(); i++) {
            char ch = spec.charAt(i);
            if (ch == '+' || ch == '-') return i;
        }
        return -1;
    }

    public static void onAccept(ServerPlayer p, Quest q) {
        CityStories.Chain c = CityStories.byId(q.chain);
        if (c == null) return;
        QuestLog log = StatsManager.get(p).quests();
        log.chainStep.put(c.id, q.step);
        if (q.step == 0) {
            p.sendSystemMessage(Component.literal("━━ " + c.title + " ━━").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            say(p, c, c.intro, 0);
        }
        brief(p, q);
    }

    /** Напомнить шаг: что делать и где. */
    private static void brief(ServerPlayer p, Quest q) {
        p.sendSystemMessage(Component.literal("★ " + q.title).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(" — " + q.desc).withStyle(ChatFormatting.GRAY)));
        if (q.type == Quest.Type.CHOICE) printChoices(p, q);
        else if (q.hasPos) CityQuests.waypoint(p, q);
    }

    /** Шаг выполнен (награда уже выдана): реплики, следующий шаг или финал. */
    public static void advance(ServerPlayer p, Quest q) {
        CityStories.Chain c = CityStories.byId(q.chain);
        if (c == null || q.step < 0 || q.step >= c.steps.length) return;
        CityStories.Step s = c.steps[q.step];
        QuestLog log = StatsManager.get(p).quests();
        int delay = say(p, c, s.say, 0);
        if (!s.give.isEmpty()) give(p, s.give);
        if (s.end) {
            finish(p, c, delay);
            return;
        }
        int next = q.step + 1;
        if (s.type == Quest.Type.CHOICE) {
            try {
                CityStories.Opt o = s.opts[Integer.parseInt(q.target)];
                next = o.next;
                delay = Math.max(delay, o.say.length * 30 + 10);
            } catch (Exception ignored) {
            }
        } else if (s.next >= 0) {
            next = s.next;
        }
        Quest n = make(p, c, next);
        log.active.add(n);
        log.chainStep.put(c.id, next);
        StatsManager.sync(p);
        Scheduler.later(delay + 20, () -> {
            if (p.hasDisconnected()) return;
            brief(p, n);
        });
    }

    /** Реплики по очереди, по полторы секунды. @return задержка до конца разговора. */
    private static int say(ServerPlayer p, CityStories.Chain c, String[] lines, int start) {
        int t = start;
        for (String line : lines) {
            final Component msg = line(c, line);
            Scheduler.later(t, () -> {
                if (!p.hasDisconnected()) p.sendSystemMessage(msg);
            });
            t += 30;
        }
        return t;
    }

    private static Component line(CityStories.Chain c, String line) {
        int i = line.indexOf(": ");
        if (i > 0) {
            String who = line.substring(0, i);
            String text = line.substring(i + 2);
            if (who.equals("*")) return Component.literal(text).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
            CityStories.Npc n = c.npc(who);
            if (n != null) {
                return Component.literal(n.name).withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(": «" + text + "»").withStyle(ChatFormatting.WHITE));
            }
        }
        return Component.literal(line).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /** Предмет-улика: «book:Название|Автор|стр.1|стр.2…» или «id*кол-во». */
    private static void give(ServerPlayer p, String spec) {
        ItemStack st;
        if (spec.startsWith("book:")) {
            String[] parts = spec.substring(5).split("\\|");
            st = new ItemStack(Items.WRITTEN_BOOK);
            var tag = st.getOrCreateTag();
            tag.putString("title", parts[0]);
            tag.putString("author", parts.length > 1 ? parts[1] : "?");
            ListTag pages = new ListTag();
            for (int i = 2; i < parts.length; i++) pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(parts[i]))));
            tag.put("pages", pages);
            tag.putBoolean("resolved", true);
        } else {
            String[] parts = spec.split("\\*");
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0]));
            if (item == null) return;
            st = new ItemStack(item, parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
        }
        StatsManager.message(p, Component.literal("Получено: ").withStyle(ChatFormatting.YELLOW).append(st.getHoverName()));
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private static void finish(ServerPlayer p, CityStories.Chain c, int delay) {
        QuestLog log = StatsManager.get(p).quests();
        log.chainsDone.add(c.id);
        log.chainStep.remove(c.id);
        int score = log.chainScore.getOrDefault(c.id, 0);
        boolean good = score >= 0;
        int t = say(p, c, good ? c.good : c.bad, delay + 10);
        Scheduler.later(t + 10, () -> {
            if (p.hasDisconnected()) return;
            int L = c.level;
            float k = good ? 1f : 0.6f;
            int xp = Math.round((300 + L * 110) * k);
            int coins = Math.round((40 + L * 16) * k);
            StatsManager.giveXp(p, xp);
            for (ItemStack st : com.andrey.litrpg.gear.LootRolls.coinStacks(coins)) give(p, st);
            WorldMap.City city = WorldMap.get().city(c.city);
            Faction f = city == null || city.faction() == null ? Faction.KINGDOM : Faction.byKey(city.faction());
            if (f != null) RepEvents.change(p, f, good ? 80 : 25);
            ItemStack reward = rewardItem(p, c, good);
            if (reward != null) give(p, reward);
            if (good && c.id.equals("wolfden_spirit")) whiteWolf(p);
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 60, 20));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    Component.literal("Сюжет завершён").withStyle(ChatFormatting.GOLD)));
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    Component.literal(c.title + (good ? "" : " (горькая концовка)")).withStyle(ChatFormatting.WHITE)));
            p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
            StatsManager.message(p, Component.literal("Награда за сюжет: " + xp + " опыта, " + QuestEvents.coinText(coins)
                    + (f != null ? ", репутация «" + f.title + "» +" + (good ? 80 : 25) : "")
                    + (reward != null ? ", " + reward.getHoverName().getString() : "")).withStyle(ChatFormatting.GOLD));
            // герои сюжета уходят по своим делам
            for (Entity e : p.serverLevel().getEntitiesOfClass(RaceNpcEntity.class, p.getBoundingBox().inflate(160),
                    n -> n.getTags().stream().anyMatch(tg -> tg.startsWith(NPC_PREFIX + c.id + ":")))) {
                e.discard();
            }
            StatsManager.sync(p);
        });
    }

    private static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st)) p.drop(st, false);
    }

    private static ItemStack rewardItem(ServerPlayer p, CityStories.Chain c, boolean good) {
        var reg = ModItems.TEMPLATE_ITEMS.get(c.reward);
        if (reg == null) return null;
        ItemStack st = new ItemStack(reg.get());
        Rarity r = good ? Rarity.EPIC : Rarity.RARE;
        GearGen.apply(st, p.getRandom(), Math.max(c.level + 4, StatsManager.get(p).getLevel()), r);
        if (GearData.get(st) == null) return st;
        int color = r == Rarity.EPIC ? 0xB060FF : 0x4FA3FF;
        st.setHoverName(Component.literal(c.rewardName).withStyle(s -> s.withColor(color).withItalic(false)));
        return st;
    }

    private static void whiteWolf(ServerPlayer p) {
        Wolf w = net.minecraft.world.entity.EntityType.WOLF.create(p.serverLevel());
        if (w == null) return;
        w.moveTo(p.getX() + 1, p.getY(), p.getZ() + 1, p.getYRot(), 0);
        w.tame(p);
        w.setCustomName(Component.literal("Белый Волк, дух стаи").withStyle(ChatFormatting.WHITE));
        w.setPersistenceRequired();
        var hp = w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(60);
        w.setHealth(60);
        p.serverLevel().addFreshEntity(w);
        StatsManager.message(p, Component.literal("Белый Волк теперь идёт за вами.").withStyle(ChatFormatting.AQUA));
    }

    // ===================================================================
    //                              выбор
    // ===================================================================

    private static void printChoices(ServerPlayer p, Quest q) {
        String[] opts = q.extra.split("\\|");
        p.sendSystemMessage(Component.literal("Ваше решение (нажмите на вариант):").withStyle(ChatFormatting.YELLOW));
        for (int i = 0; i < opts.length; i++) {
            final int n = i;
            MutableComponent c = Component.literal("  [" + (i + 1) + "] " + opts[i]).withStyle(s -> s.withColor(ChatFormatting.AQUA)
                    .withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/litrpg_choice " + q.chain + " " + n))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Выбрать — решение не отменить"))));
            p.sendSystemMessage(c);
        }
    }

    /** Кнопка «Где?» у выбора — повторить варианты. */
    public static boolean locate(ServerPlayer p, Quest q) {
        if (q.type != Quest.Type.CHOICE || q.chain.isEmpty()) return false;
        printChoices(p, q);
        return true;
    }

    public static void choose(ServerPlayer p, String chain, int n) {
        QuestLog log = StatsManager.get(p).quests();
        CityStories.Chain c = CityStories.byId(chain);
        if (c == null) return;
        for (Quest q : log.active) {
            if (!chain.equals(q.chain) || q.type != Quest.Type.CHOICE || q.isComplete()) continue;
            CityStories.Step s = c.steps[q.step];
            if (n < 0 || n >= s.opts.length) return;
            CityStories.Opt o = s.opts[n];
            q.target = String.valueOf(n);
            q.have = q.need;
            log.chainScore.merge(c.id, o.score, Integer::sum);
            if (o.rep != 0 && q.faction != null) RepEvents.change(p, q.faction, o.rep);
            p.sendSystemMessage(Component.literal("Вы решили: " + o.label).withStyle(ChatFormatting.YELLOW));
            say(p, c, o.say, 10);
            if (!o.gone.isEmpty()) {
                for (Entity e : p.serverLevel().getEntitiesOfClass(RaceNpcEntity.class, p.getBoundingBox().inflate(200),
                        e -> e.getTags().contains(NPC_PREFIX + c.id + ":" + o.gone))) {
                    p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 1, e.getZ(),
                            60, 0.4, 0.8, 0.4, 0.1);
                    e.discard();
                }
            }
            // реплики выбора прозвучат раньше, чем награда за шаг и следующий шаг
            Scheduler.later(o.say.length * 30 + 20, () -> {
                if (!p.hasDisconnected()) QuestEvents.checkAll(p);
            });
            return;
        }
        StatsManager.message(p, Component.literal("Сейчас выбирать нечего.").withStyle(ChatFormatting.GRAY));
    }

    // ===================================================================
    //                    герои сюжета и разговоры
    // ===================================================================

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof RaceNpcEntity npc)) return;
        String tag = null;
        for (String t : npc.getTags()) if (t.startsWith(NPC_PREFIX)) tag = t;
        if (tag == null) return;
        event.setCanceled(true);
        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) return;
        String[] parts = tag.substring(NPC_PREFIX.length()).split(":");
        if (parts.length < 2) return;
        CityStories.Chain c = CityStories.byId(parts[0]);
        if (c == null) return;
        CityStories.Npc n = c.npc(parts[1]);
        npc.getLookControl().setLookAt(p);
        for (Quest q : StatsManager.get(p).quests().active) {
            if (q.type == Quest.Type.TALK && c.id.equals(q.chain) && parts[1].equals(q.target) && !q.isComplete()) {
                q.have = q.need;
                p.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1f, npc.getVoicePitch());
                QuestEvents.checkAll(p);
                return;
            }
        }
        if (n != null) {
            Speech.say(npc, n.idle, 60);
            p.sendSystemMessage(Component.literal(n.name).withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(": «" + n.idle + "»").withStyle(ChatFormatting.WHITE)));
        }
    }

    /** Герой сюжета стоит у своего места, пока он нужен. */
    private static void ensureNpc(ServerPlayer p, CityStories.Chain c, CityStories.Npc n, Quest q) {
        ServerLevel sl = p.serverLevel();
        BlockPos at = new BlockPos(q.px, q.py, q.pz);
        if (!sl.isLoaded(at)) return;
        String tag = NPC_PREFIX + c.id + ":" + n.id;
        if (!sl.getEntitiesOfClass(RaceNpcEntity.class, new AABB(at).inflate(40, 24, 40), e -> e.getTags().contains(tag)).isEmpty()) return;
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(n.type));
        if (type == null || !(type.create(sl) instanceof RaceNpcEntity e)) return;
        BlockPos spot = stand(sl, at, 4);
        e.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
        e.finalizeSpawn(sl, sl.getCurrentDifficultyAt(spot), MobSpawnType.EVENT, null, null);
        e.setStoryName(n.name);
        e.restrictTo(spot, 3);
        e.addTag(NPC_TAG);
        e.addTag(tag);
        e.setPersistenceRequired();
        sl.addFreshEntity(e);
    }

    /** Ближайшее место, где можно стоять (воздух над твёрдым), рядом с точкой. */
    private static BlockPos stand(ServerLevel sl, BlockPos c, int r) {
        for (int rr = 0; rr <= r; rr++) {
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dz = -rr; dz <= rr; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != rr) continue;
                    for (int dy : new int[]{0, 1, -1, 2, -2, 3}) {
                        BlockPos b = c.offset(dx, dy, dz);
                        if (sl.getBlockState(b).getCollisionShape(sl, b).isEmpty() && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty()
                                && sl.getBlockState(b.below()).isSolidRender(sl, b.below()) && sl.getFluidState(b).isEmpty()) {
                            return b;
                        }
                    }
                }
            }
        }
        return c;
    }

    // ===================================================================
    //                     места, улики и засады
    // ===================================================================

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 20 != 11) return;
        if (!SystemWorld.isOverworld(p.serverLevel())) return;
        boolean changed = false;
        for (Quest q : new ArrayList<>(StatsManager.get(p).quests().active)) {
            if (q.isComplete() || !q.hasPos) continue;
            double dx = p.getX() - (q.px + 0.5), dy = p.getY() - q.py, dz = p.getZ() - (q.pz + 0.5);
            double d2 = dx * dx + dz * dz;
            switch (q.type) {
                case TALK -> {
                    CityStories.Chain c = CityStories.byId(q.chain);
                    if (c != null && d2 < 56 * 56) {
                        CityStories.Npc n = c.npc(q.target);
                        if (n != null) ensureNpc(p, c, n, q);
                    }
                }
                case SEARCH -> {
                    double r = Math.max(2, q.rad) + 0.8;
                    if (d2 < r * r && Math.abs(dy) < 3) {
                        q.have = q.need;
                        changed = true;
                        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
                        StatsManager.message(p, Component.literal("🔍 Вы осмотрели: " + q.where).withStyle(ChatFormatting.AQUA));
                    }
                }
                case VISIT -> {
                    if (q.rad > 0 && d2 < (q.rad + 1.0) * (q.rad + 1.0) && Math.abs(dy) < 4) {
                        q.have = q.need;
                        changed = true;
                        StatsManager.message(p, Component.literal("Вы добрались: " + q.where).withStyle(ChatFormatting.GREEN));
                    }
                }
                case FIGHT -> {
                    if (d2 < 30 * 30 && Math.abs(dy) < 18) spawnFoes(p, q);
                }
                default -> {
                }
            }
        }
        if (changed) QuestEvents.checkAll(p);
    }

    private record Unit(String type, String name, boolean boss) {}

    /** «тип*кол-во,тип@Имя!» → список бойцов по порядку (вожак — в конце). */
    static List<Unit> units(String spec) {
        List<Unit> out = new ArrayList<>();
        for (String part : spec.split(",")) {
            part = part.trim();
            if (part.isEmpty()) continue;
            boolean boss = part.endsWith("!");
            if (boss) part = part.substring(0, part.length() - 1);
            String name = "";
            int at = part.indexOf('@');
            if (at > 0) {
                name = part.substring(at + 1);
                part = part.substring(0, at);
            }
            int n = 1;
            int star = part.indexOf('*');
            if (star > 0) {
                n = Integer.parseInt(part.substring(star + 1));
                part = part.substring(0, star);
            }
            for (int i = 0; i < n; i++) out.add(new Unit(part, name, boss));
        }
        return out;
    }

    private static String foeTag(ServerPlayer p, Quest q) {
        return FOE_PREFIX + p.getStringUUID() + ":" + q.id;
    }

    private static void spawnFoes(ServerPlayer p, Quest q) {
        ServerLevel sl = p.serverLevel();
        BlockPos center = new BlockPos(q.px, q.py, q.pz);
        if (!sl.isLoaded(center)) return;
        String tag = foeTag(p, q);
        int alive = sl.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(48, 24, 48), e -> e.getTags().contains(tag)).size();
        int semi = q.extra.indexOf(';');
        int lvl = 1;
        try {
            lvl = Integer.parseInt(q.extra.substring(0, semi));
        } catch (Exception ignored) {
        }
        List<Unit> all = units(q.extra.substring(semi + 1));
        int from = q.have + alive;
        if (from >= all.size()) return;
        int made = 0;
        for (int i = from; i < all.size() && made < 6; i++) {
            Unit u = all.get(i);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(u.type));
            if (type == null) continue;
            Entity raw = type.create(sl);
            if (!(raw instanceof Mob m)) continue;
            BlockPos at = stand(sl, center.offset(p.getRandom().nextInt(7) - 3, 0, p.getRandom().nextInt(7) - 3), 4);
            m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            int l = u.boss ? lvl + 2 : lvl;
            if (m instanceof RaceNpcEntity npc && !u.name.isEmpty()) npc.setStoryName(u.name);
            MobLevels.apply(m, l, u.boss ? MobLevels.Rank.ELITE : MobLevels.Rank.NORMAL);
            if (!(m instanceof RaceNpcEntity) && !u.name.isEmpty()) {
                m.setCustomName(Component.literal("[Ур. " + l + "] ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(u.name).withStyle(ChatFormatting.RED)));
            }
            m.addTag(tag);
            m.setPersistenceRequired();
            m.setTarget(p);
            sl.addFreshEntity(m);
            q.spawned++;
            made++;
        }
        if (made > 0) {
            sl.playSound(null, center, SoundEvents.RAID_HORN.get(), SoundSource.HOSTILE, 0.6f, 1.3f);
            p.displayClientMessage(Component.literal("Враги! " + q.where).withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;
        for (String t : dead.getTags()) {
            if (!t.startsWith(FOE_PREFIX)) continue;
            String[] parts = t.substring(FOE_PREFIX.length()).split(":");
            if (parts.length < 2 || dead.getServer() == null) return;
            ServerPlayer p;
            try {
                p = dead.getServer().getPlayerList().getPlayer(UUID.fromString(parts[0]));
            } catch (IllegalArgumentException e) {
                return;
            }
            if (p == null) return;
            for (Quest q : StatsManager.get(p).quests().active) {
                if (!q.id.equals(parts[1]) || q.type != Quest.Type.FIGHT || q.isComplete()) continue;
                q.have++;
                if (!q.isComplete()) {
                    p.displayClientMessage(Component.literal(q.title + ": " + q.have + "/" + q.need).withStyle(ChatFormatting.AQUA), true);
                }
                QuestEvents.checkAll(p);
                return;
            }
            return;
        }
    }

    // ===================================================================
    //                              команды
    // ===================================================================

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        // выбор игрока (кликом в чате) — доступен всем
        event.getDispatcher().register(Commands.literal("litrpg_choice")
                .then(Commands.argument("chain", StringArgumentType.word())
                        .then(Commands.argument("n", IntegerArgumentType.integer(0, 9)).executes(ctx -> {
                            choose(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "chain"),
                                    IntegerArgumentType.getInteger(ctx, "n"));
                            return 1;
                        }))));
        // для проверки: поставить игрока на шаг сюжета / перенести к месту шага
        event.getDispatcher().register(Commands.literal("litrpg_story").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("chain", StringArgumentType.word())
                                .then(Commands.argument("step", IntegerArgumentType.integer(0, 30)).executes(ctx -> {
                                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                                    CityStories.Chain c = CityStories.byId(StringArgumentType.getString(ctx, "chain"));
                                    if (c == null) return 0;
                                    QuestLog log = StatsManager.get(p).quests();
                                    log.active.removeIf(q -> c.id.equals(q.chain));
                                    log.chainsDone.remove(c.id);
                                    Quest q = make(p, c, IntegerArgumentType.getInteger(ctx, "step"));
                                    log.active.add(q);
                                    onAccept(p, q);
                                    StatsManager.sync(p);
                                    return 1;
                                })))))
                .then(Commands.literal("tp").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    for (Quest q : StatsManager.get(p).quests().active) {
                        if (!q.chain.isEmpty() && q.hasPos) {
                            p.teleportTo(q.px + 0.5, q.py, q.pz + 0.5);
                            return 1;
                        }
                    }
                    return 0;
                })))
                .then(Commands.literal("done").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    for (Quest q : StatsManager.get(p).quests().active) {
                        if (!q.chain.isEmpty() && !q.isComplete() && q.type != Quest.Type.CHOICE) {
                            q.have = q.need;
                            QuestEvents.checkAll(p);
                            return 1;
                        }
                    }
                    return 0;
                })))
                .then(Commands.literal("talk").then(Commands.argument("player", EntityArgument.player()).executes(ctx -> {
                    // как ПКМ по ближайшему герою сюжета
                    ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
                    var list = p.serverLevel().getEntitiesOfClass(RaceNpcEntity.class, p.getBoundingBox().inflate(12),
                            e -> e.getTags().contains(NPC_TAG));
                    if (list.isEmpty()) return 0;
                    onInteract(new PlayerInteractEvent.EntityInteract(p, net.minecraft.world.InteractionHand.MAIN_HAND, list.get(0)));
                    return 1;
                })))
                .then(Commands.literal("check").executes(ctx -> {
                    // все ли места сюжетов есть на карте
                    int bad = 0;
                    for (CityStories.Chain c : CityStories.all()) {
                        for (CityStories.Step s : c.steps) {
                            String a = s.type == Quest.Type.TALK ? (c.npc(s.target) == null ? null : c.npc(s.target).anchor) : s.anchor;
                            if (a == null || a.isEmpty()) continue;
                            String name = offsetStart(a) > 0 ? a.substring(0, offsetStart(a)) : a;
                            String city = c.city;
                            if (name.contains(":")) {
                                city = name.substring(0, name.indexOf(':'));
                                name = name.substring(name.indexOf(':') + 1);
                            }
                            if (WorldMap.get().anchor(city, name) == null) {
                                bad++;
                                LitRPG.LOGGER.warn("story anchor missing: {} {} {}", c.id, city, name);
                            }
                        }
                    }
                    final int b = bad;
                    ctx.getSource().sendSuccess(() -> Component.literal("Нет мест: " + b), false);
                    return 1;
                })));
    }
}
