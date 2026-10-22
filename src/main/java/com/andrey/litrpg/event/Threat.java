package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Minions;
import com.andrey.litrpg.ability.Stealth;
import com.andrey.litrpg.entity.wild.Species;
import com.andrey.litrpg.entity.wild.WildMob;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.ThreatPacket;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Агр по урону (таблица угрозы, как в MMO). Каждый моб помнит, кто и сколько ему навредил:
 * урон даёт угрозу, лечение союзников в бою — половину, танки (стражи, воины, паладины)
 * и поднятый щит — больше. Моб бьёт того, у кого угроза выше, а сменить цель его заставит
 * только тот, кто перебьёт текущую угрозу на 10% вблизи или на 30% издалека.
 * «Насмешка», «Боевой клич» и «Рёв» сразу делают тебя первым в списке.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Threat {
    private Threat() {}

    /** Таблица угрозы одного моба: кто → сколько. */
    private static final class Table {
        final Map<UUID, Float> threat = new LinkedHashMap<>();
        long lastHit;
    }

    private static final Map<Mob, Table> TABLES = new WeakHashMap<>();
    private static final float MELEE_LEAD = 1.1f, RANGED_LEAD = 1.3f;
    private static final int FORGET_TICKS = 20 * 15, MAX_RANGE = 48;

    private static boolean tracked(Mob m) {
        if (m.level().isClientSide || Minions.ownerOf(m) != null) return false;
        if (m instanceof OwnableEntity own && own.getOwnerUUID() != null) return false;
        // падальщики нарочно выбирают самых израненных — у них свой расчёт
        return !(m instanceof WildMob w && w.spec().temper == Species.Temper.SCAVENGER);
    }

    /** Множитель угрозы атакующего: танки и щит держат врагов на себе. */
    private static float mult(LivingEntity who) {
        if (!(who instanceof ServerPlayer p)) return 1f;
        float k = 1f;
        PlayerClass c = StatsManager.get(p).getPlayerClass();
        if (c != null) k += 0.15f * c.styles.getOrDefault(Style.TANK, 0);
        if (p.isBlocking() || p.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem) k += 0.3f;
        return k;
    }

    private static void add(Mob m, LivingEntity who, float amount) {
        if (amount <= 0 || who == m || !tracked(m)) return;
        Table t = TABLES.computeIfAbsent(m, k -> new Table());
        t.threat.merge(who.getUUID(), amount, Float::sum);
        t.lastHit = m.level().getGameTime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent e) {
        if (!(e.getEntity() instanceof Mob m) || e.getAmount() <= 0) return;
        Entity src = e.getSource().getEntity();
        if (!(src instanceof LivingEntity who) || who instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        add(m, who, Math.min(e.getAmount(), m.getHealth() + 1) * mult(who));
        retarget(m);
    }

    /** Лечение в бою: мобы, которые дерутся с вылеченными, злятся на лекаря. */
    public static void healed(ServerPlayer healer, LivingEntity ally, float amount) {
        if (amount <= 0) return;
        for (Mob m : healer.serverLevel().getEntitiesOfClass(Mob.class, ally.getBoundingBox().inflate(32))) {
            Table t = TABLES.get(m);
            if (t != null && (t.threat.containsKey(ally.getUUID()) || m.getTarget() == ally)) add(m, healer, amount * 0.5f);
        }
    }

    /** Провокация: моб бросает всё и бьёт провокатора, а тот становится первым в таблице. */
    public static void taunt(Mob m, LivingEntity who) {
        if (!tracked(m)) {
            m.setTarget(who);
            return;
        }
        Table t = TABLES.computeIfAbsent(m, k -> new Table());
        float top = 0;
        for (float v : t.threat.values()) top = Math.max(top, v);
        t.threat.put(who.getUUID(), Math.max(t.threat.getOrDefault(who.getUUID(), 0f), top * RANGED_LEAD + 10));
        t.lastHit = m.level().getGameTime();
        m.setTarget(who);
    }

    private static LivingEntity resolve(Mob m, UUID id) {
        if (!(m.level() instanceof ServerLevel sl)) return null;
        Entity e = sl.getEntity(id);
        if (!(e instanceof LivingEntity le) || !le.isAlive() || le.level() != m.level() || le.distanceToSqr(m) > MAX_RANGE * MAX_RANGE) return null;
        if (le instanceof Player p && (p.isCreative() || p.isSpectator() || Stealth.isHidden(p))) return null;
        return le;
    }

    /** Кто сейчас должен быть целью: лидер по угрозе, если он обогнал нынешнюю цель с запасом. */
    private static LivingEntity wanted(Mob m, Table t, LivingEntity current) {
        LivingEntity best = null;
        float bestV = 0;
        for (Iterator<Map.Entry<UUID, Float>> it = t.threat.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            LivingEntity le = resolve(m, en.getKey());
            if (le == null) {
                // ушёл совсем (вышел из игры, исчез) — забываем; спрятавшийся или отошедший ещё вернётся
                if (m.level() instanceof ServerLevel sl && sl.getEntity(en.getKey()) == null
                        && sl.getServer().getPlayerList().getPlayer(en.getKey()) == null) it.remove();
                continue;
            }
            if (en.getValue() > bestV) {
                bestV = en.getValue();
                best = le;
            }
        }
        if (best == null || best == current) return best;
        if (current == null || !current.isAlive()) return best;
        float cur = t.threat.getOrDefault(current.getUUID(), 0f);
        float lead = current.distanceToSqr(m) <= 16 ? MELEE_LEAD : RANGED_LEAD;
        return bestV > cur * lead ? best : current;
    }

    private static void retarget(Mob m) {
        Table t = TABLES.get(m);
        if (t == null) return;
        LivingEntity cur = m.getTarget();
        LivingEntity want = wanted(m, t, cur);
        if (want == null || want == cur) return;
        m.setTarget(want);
        if (m.getTarget() == want && want instanceof ServerPlayer sp && cur != null) {
            sp.displayClientMessage(Component.literal("⚠ ").withStyle(ChatFormatting.RED)
                    .append(m.getDisplayName().copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(" переключается на тебя!").withStyle(ChatFormatting.RED)), true);
        }
    }

    /** Любая смена цели (ответ на удар, «ближайший враг») не может отнять моба у лидера угрозы. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (e.isCanceled() || !(e.getEntity() instanceof Mob m) || e.getNewTarget() == null) return;
        Table t = TABLES.get(m);
        if (t == null || t.threat.isEmpty()) return;
        LivingEntity want = wanted(m, t, e.getNewTarget());
        if (want != null && want != e.getNewTarget()) e.setNewTarget(want);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity().level().isClientSide) return;
        if (e.getEntity() instanceof Mob m) TABLES.remove(m);
        UUID id = e.getEntity().getUUID();
        for (Table t : TABLES.values()) t.threat.remove(id);
    }

    /** Для dev-команды devthreat: таблица угрозы моба. */
    public static String debug(Mob m) {
        Table t = TABLES.get(m);
        if (t == null || !(m.level() instanceof ServerLevel sl)) return "{}";
        StringBuilder b = new StringBuilder("{");
        t.threat.forEach((id, v) -> {
            Entity e = sl.getEntity(id);
            b.append(e == null ? id.toString().substring(0, 8) : e.getName().getString()).append('=').append(Math.round(v)).append(' ');
        });
        return b.append('}').toString();
    }

    /** Раз в полсекунды: смена цели по угрозе, забывание после боя, подсказка в рамке цели. */
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || TABLES.isEmpty()) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 10 != 0) return;
        List<Mob> drop = new ArrayList<>();
        for (var en : List.copyOf(TABLES.entrySet())) {
            Mob m = en.getKey();
            Table t = en.getValue();
            if (m == null || !m.isAlive() || m.isRemoved()) {
                drop.add(m);
                continue;
            }
            long idle = m.level().getGameTime() - t.lastHit;
            if (idle > FORGET_TICKS && m.getTarget() == null || idle > FORGET_TICKS * 4) {
                drop.add(m);
                continue;
            }
            retarget(m);
            sync(m, t);
        }
        for (Mob m : drop) TABLES.remove(m);
    }

    /** Участникам боя — кто у моба в цели и сколько у тебя угрозы относительно лидера. */
    private static void sync(Mob m, Table t) {
        float top = 0;
        for (float v : t.threat.values()) top = Math.max(top, v);
        if (top <= 0) return;
        LivingEntity target = m.getTarget();
        String name = target == null ? "" : target.getName().getString();
        for (var en : t.threat.entrySet()) {
            if (!(m.level() instanceof ServerLevel sl) || !(sl.getEntity(en.getKey()) instanceof ServerPlayer p)) continue;
            if (p.distanceToSqr(m) > 40 * 40) continue;
            int pct = Math.round(en.getValue() / top * 100);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p),
                    new ThreatPacket(m.getId(), name, target == p, pct));
        }
    }
}
