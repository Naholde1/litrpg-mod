package com.andrey.litrpg.ability;

import com.andrey.litrpg.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Законы Законника. Он объявляет правило, и оно само следит за соблюдением: кто ему следует —
 * получает усиление, кто нарушил — теряет его и на пару секунд становится уязвимее.
 * <p>
 * Правило касается всего отряда, своих тоже. В этом и есть класс: он меняет не цифры, а то,
 * как группе приходится драться ближайшие пятнадцать секунд.
 */
public final class Laws {
    /** Что именно запрещено или предписано. */
    public enum Kind {
        /** Закон покоя: не бегать. */
        STILLNESS("Закон покоя", "не двигаться"),
        /** Закон воздержания: не применять способности. */
        TEMPERANCE("Закон воздержания", "не применять способности"),
        /** Закон возмездия: не отходить от врага. */
        RETRIBUTION("Закон возмездия", "держаться вплотную к врагу");

        public final String title;
        public final String demand;

        Kind(String title, String demand) {
            this.title = title;
            this.demand = demand;
        }

        public static Kind byKey(String key) {
            for (Kind k : values()) if (k.name().equalsIgnoreCase(key)) return k;
            return null;
        }
    }

    /** Насколько медленно надо двигаться, чтобы считаться стоящим. */
    private static final double STILL_SPEED = 0.06;
    /** Сколько секунд после применения способности считается нарушением воздержания. */
    private static final int CAST_MEMORY = 60;
    /** Насколько близко должен быть враг для закона возмездия. */
    private static final double CLOSE = 6;
    /** Кого закон достаёт. */
    private static final double REACH = 24;

    private record Law(Kind kind, long until) {}

    private static final Map<UUID, Law> ACTIVE = new HashMap<>();

    private Laws() {}

    /** Объявить закон: он действует, пока держится усиление способности. */
    static boolean declare(ServerPlayer p, Ability a) {
        Kind kind = Kind.byKey(a.law);
        if (kind == null) return false;
        long until = p.level().getGameTime() + AbilityHandler.dur(a);
        ACTIVE.put(p.getUUID(), new Law(kind, until));
        for (ServerPlayer o : nearby(p)) {
            o.displayClientMessage(Component.literal("§6" + kind.title + "§7: " + kind.demand
                    + " — иначе потеряешь усиление."), false);
        }
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    /** Раз в секунду: кто соблюдает — усиливается, кто нарушил — теряет и получает уязвимость. */
    public static void tick(ServerPlayer owner) {
        Law law = ACTIVE.get(owner.getUUID());
        if (law == null) return;
        if (owner.level().getGameTime() >= law.until()) {
            ACTIVE.remove(owner.getUUID());
            for (ServerPlayer o : nearby(owner)) {
                o.displayClientMessage(Component.literal(law.kind().title + " отменён.").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        for (ServerPlayer o : nearby(owner)) {
            if (obeys(o, law.kind())) {
                o.addEffect(new MobEffectInstance(ModEffects.MIGHT.get(), 40, 1, false, false, true));
                o.addEffect(new MobEffectInstance(ModEffects.BULWARK.get(), 40, 1, false, false, true));
            } else {
                o.removeEffect(ModEffects.MIGHT.get());
                o.removeEffect(ModEffects.BULWARK.get());
                o.addEffect(new MobEffectInstance(ModEffects.VULNERABILITY.get(), 40, 0, false, false, true));
                o.displayClientMessage(Component.literal("§c" + law.kind().title + " нарушен: " + law.kind().demand), true);
            }
        }
    }

    /** Соблюдает ли игрок действующий закон. */
    private static boolean obeys(ServerPlayer p, Kind kind) {
        return switch (kind) {
            case STILLNESS -> p.getDeltaMovement().horizontalDistance() < STILL_SPEED;
            case TEMPERANCE -> p.level().getGameTime() - p.getPersistentData().getLong(AbilityHandler.LAST_CAST_TAG) > CAST_MEMORY;
            case RETRIBUTION -> enemyNear(p);
        };
    }

    private static boolean enemyNear(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return true;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(CLOSE),
                e -> e.isAlive() && e instanceof Enemy && Minions.ownerOf(e) == null)) {
            if (m instanceof LivingEntity) return true;
        }
        return false;
    }

    private static java.util.List<ServerPlayer> nearby(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) return java.util.List.of();
        return sl.getEntitiesOfClass(ServerPlayer.class, p.getBoundingBox().inflate(REACH),
                o -> o.isAlive() && !o.isSpectator());
    }

    /** Законник ушёл — закон снимается. */
    public static void clear(ServerPlayer p) {
        ACTIVE.remove(p.getUUID());
    }

    /** Для отладки: какой закон сейчас объявлен. */
    public static String debug(ServerPlayer p) {
        Law l = ACTIVE.get(p.getUUID());
        return l == null ? "-" : l.kind().name() + " ещё " + (l.until() - p.level().getGameTime()) / 20 + "с";
    }
}
