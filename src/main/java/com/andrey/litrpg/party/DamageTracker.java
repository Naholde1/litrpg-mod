package com.andrey.litrpg.party;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Minions;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Учёт урона по мобу от каждого игрока (и его прислужников, питомцев, стрел).
 * Опыт делится по вкладу, поэтому моба, добитого огнём или падением, всё равно засчитают.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DamageTracker {
    private static final String TAG = "litrpg_dmg";

    private DamageTracker() {}

    /** Игрок, которому засчитывается урон из этого источника. */
    public static ServerPlayer creditOf(DamageSource src) {
        return creditOf(src.getEntity());
    }

    public static ServerPlayer creditOf(Entity e) {
        if (e instanceof ServerPlayer sp) return sp;
        if (e == null) return null;
        ServerPlayer owner = Minions.owner(e);
        if (owner != null) return owner;
        if (e instanceof OwnableEntity own && own.getOwner() instanceof ServerPlayer sp) return sp;
        return null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide || target instanceof Player || event.getAmount() <= 0) return;
        ServerPlayer p = creditOf(event.getSource());
        if (p == null) return;
        float dealt = Math.min(event.getAmount(), target.getHealth());
        CompoundTag tag = target.getPersistentData().getCompound(TAG);
        String key = p.getUUID().toString();
        tag.putFloat(key, tag.getFloat(key) + dealt);
        target.getPersistentData().put(TAG, tag);
    }

    /** Вклад каждого игрока, доли в сумме дают 1. Пусто — никто из игроков не бил. */
    public static Map<ServerPlayer, Float> shares(LivingEntity dead) {
        Map<ServerPlayer, Float> result = new LinkedHashMap<>();
        if (!(dead.level() instanceof ServerLevel sl)) return result;
        CompoundTag tag = dead.getPersistentData().getCompound(TAG);
        float total = 0;
        Map<UUID, Float> raw = new HashMap<>();
        for (String k : tag.getAllKeys()) {
            try {
                float v = tag.getFloat(k);
                raw.put(UUID.fromString(k), v);
                total += v;
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (total <= 0) return result;
        for (var e : raw.entrySet()) {
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(e.getKey());
            if (sp != null) result.put(sp, e.getValue() / total);
        }
        return result;
    }

    /** Игрок с наибольшим вкладом в убийство (для добычи). */
    public static ServerPlayer topContributor(LivingEntity dead, DamageSource src) {
        ServerPlayer direct = creditOf(src);
        if (direct != null) return direct;
        ServerPlayer top = null;
        float best = 0;
        for (var e : shares(dead).entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
        }
        return top;
    }

    /**
     * Раздать опыт за убийство: по вкладу, а внутри группы — поровну между
     * всеми участниками рядом с бонусом +10% за каждого товарища.
     * @return игрок с наибольшим вкладом (ему засчитываются титулы), или null
     */
    public static ServerPlayer distribute(LivingEntity dead) {
        Map<ServerPlayer, Float> shares = shares(dead);
        if (shares.isEmpty()) return null;
        // доли группы складываются и делятся на всех её членов поблизости
        Map<ServerPlayer, Float> finalShare = new LinkedHashMap<>();
        Map<Party, Float> partyPool = new HashMap<>();
        for (var e : shares.entrySet()) {
            Party party = Party.of(e.getKey());
            if (party == null) finalShare.merge(e.getKey(), e.getValue(), Float::sum);
            else partyPool.merge(party, e.getValue(), Float::sum);
        }
        for (var e : partyPool.entrySet()) {
            var near = e.getKey().online(dead.getServer()).stream()
                    .filter(p -> p.level() == dead.level() && p.distanceToSqr(dead) < Party.SHARE_RANGE * Party.SHARE_RANGE && p.isAlive())
                    .toList();
            if (near.isEmpty()) continue;
            float bonus = 1f + 0.10f * (near.size() - 1);
            for (ServerPlayer p : near) finalShare.merge(p, e.getValue() / near.size() * bonus, Float::sum);
        }
        ServerPlayer top = null;
        float best = -1;
        for (var e : finalShare.entrySet()) {
            ServerPlayer p = e.getKey();
            int full = MobLevels.xpReward(dead, StatsManager.get(p).getLevel());
            int xp = Math.max(1, Math.round(full * e.getValue()));
            StatsManager.giveXp(p, xp);
            if (shares.getOrDefault(p, 0f) > best) {
                best = shares.getOrDefault(p, 0f);
                top = p;
            }
        }
        return top;
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        Party.clear();
    }
}
