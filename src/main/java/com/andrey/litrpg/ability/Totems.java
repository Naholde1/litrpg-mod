package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEffects;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Тотемы Каменного тотемщика: предметы на арене, а не заклинание с таймером.
 * <p>
 * Поставленный столб стоит, пока его не разобьют — сам он не падает. Его бьют и мобы,
 * и хранители, и в этом весь класс: тотемщик держит арену расстановкой, а противник
 * может его расстановку сломать. Двух одинаковых тотемов не бывает: новый того же вида
 * заменяет прежний, так что выбор места — это решение, а не спам кнопки.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Totems {
    /** Вид тотема записан в метке; по ней же он и один на хозяина. */
    public static final String TAG = "litrpg_totem";
    /** Докуда достаёт аура: на весь отряд, а не на одного хозяина. */
    public static final double RADIUS = 12;

    /** Что тотем даёт вокруг себя. */
    public enum Kind {
        GUARD("Тотем защиты", 0xC05DADE2, "totem_guard"),
        FURY("Тотем ярости", 0xC0E74C3C, "totem_fury"),
        MENDING("Тотем восстановления", 0xC07CFC66, "totem_mending"),
        WIND("Тотем ветра", 0xC0F5E6A8, "totem_wind");

        public final String title;
        public final int color;
        /** Свой тип сущности: у каждого вида своя модель и текстура. */
        public final String entity;

        Kind(String title, int color, String entity) {
            this.title = title;
            this.color = color;
            this.entity = entity;
        }

        static Kind byName(String s) {
            for (Kind k : values()) if (k.name().equals(s)) return k;
            return null;
        }
    }

    private Totems() {}

    static void register() {
        Special.put("tt_totem_guard", (p, s, a) -> place(p, s, a, Kind.GUARD));
        Special.put("tt_totem_quake", (p, s, a) -> place(p, s, a, Kind.FURY));
        Special.put("tt_totem_ward", (p, s, a) -> place(p, s, a, Kind.MENDING));
        Special.put("tt_grove", (p, s, a) -> place(p, s, a, Kind.WIND));
        Special.put("tt_shatter", Totems::shatter);
    }

    /** Тотемы этого хозяина. */
    public static List<Mob> of(ServerPlayer p) {
        List<Mob> out = new ArrayList<>();
        if (!(p.level() instanceof ServerLevel sl)) return out;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64),
                e -> e.isAlive() && e instanceof com.andrey.litrpg.entity.TotemEntity
                        && p.getUUID().equals(Minions.ownerOf(e)))) {
            out.add(m);
        }
        return out;
    }

    /** Поставить тотем перед собой. Прежний такого же вида рассыпается. */
    private static boolean place(ServerPlayer p, PlayerStats s, Ability a, Kind kind) {
        ServerLevel sl = p.serverLevel();
        for (Mob old : of(p)) {
            if (kind.name().equals(old.getPersistentData().getString(TAG))) {
                Fx.ring(sl, old.position(), 0.2, 1.4, 10, kind.color);
                old.discard();
            }
        }
        Vec3 at = spot(p);
        var type = com.andrey.litrpg.registry.ModEntities.TOTEMS.get(kind.entity);
        Entity raw = type == null ? null : type.get().create(sl);
        if (!(raw instanceof Mob totem)) return false;
        totem.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
        totem.getPersistentData().putUUID(Minions.OWNER_TAG, p.getUUID());
        totem.getPersistentData().putString(TAG, kind.name());
        // сам не падает: срок жизни отодвинут далеко, уйти он может только от ударов
        totem.getPersistentData().putLong(Minions.EXPIRE_TAG, sl.getGameTime() + 20L * 60 * 60 * 24);
        totem.getPersistentData().putInt("litrpg_lvl", s.getLevel());
        totem.finalizeSpawn(sl, sl.getCurrentDifficultyAt(totem.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
        totem.setCustomName(Component.literal(kind.title).withStyle(ChatFormatting.AQUA));
        totem.setPersistenceRequired();
        totem.goalSelector.removeAllGoals(g -> true);
        totem.targetSelector.removeAllGoals(g -> true);
        AttributeInstance speed = totem.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(0);
        AttributeInstance hp = totem.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(30 + s.getLevel() * 2.0);
        totem.setHealth(totem.getMaxHealth());
        sl.addFreshEntity(totem);

        Fx.pillar(sl, at, 0.9, 4.5, 20, kind.color);
        Fx.ring(sl, at, 0.3, RADIUS, 24, kind.color);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.DEEPSLATE_PLACE, SoundSource.PLAYERS, 1.4f, 0.6f);
        p.displayClientMessage(Component.literal(kind.title + " поставлен — стоит, пока его не разобьют.")
                .withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Место под тотем: пара блоков перед хозяином, на твёрдом полу. */
    private static Vec3 spot(ServerPlayer p) {
        Vec3 dir = AbilityHandler.flatLook(p);
        Vec3 want = p.position().add(dir.scale(2.2));
        var pos = net.minecraft.core.BlockPos.containing(want);
        for (int d = 0; d <= 4; d++) {
            var c = pos.above(d <= 2 ? d : 2 - d);
            if (p.level().getBlockState(c).isAir() && !p.level().getBlockState(c.below()).isAir()) {
                return Vec3.atBottomCenterOf(c);
            }
        }
        return p.position();
    }

    /**
     * Обрушить столбы: все свои тотемы лопаются, и каждый бьёт осколками вокруг себя.
     * Так расстановка превращается в урон — но после этого арена пуста.
     */
    private static boolean shatter(ServerPlayer p, PlayerStats s, Ability a) {
        List<Mob> list = of(p);
        if (list.isEmpty()) {
            p.displayClientMessage(Component.literal("Обрушать нечего — тотемов нет.").withStyle(ChatFormatting.GRAY), true);
            return false;
        }
        float per = AbilityHandler.base(s, a) * a.power;
        for (Mob t : list) {
            Kind k = Kind.byName(t.getPersistentData().getString(TAG));
            int color = k == null ? 0xC0A0A0A0 : k.color;
            Fx.sphere(p.level(), t.position().add(0, 1, 0), 0.3, a.radius, 12, color);
            Fx.ring(p.level(), t.position(), 0.2, a.radius, 16, color);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, t.getBoundingBox().inflate(a.radius))) {
                AbilityHandler.hurt(p, a, e, per);
            }
            p.serverLevel().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.DEEPSLATE_BREAK, SoundSource.PLAYERS, 1.6f, 0.5f);
            t.discard();
        }
        p.displayClientMessage(Component.literal("Обрушено столбов: " + list.size()).withStyle(ChatFormatting.AQUA), true);
        return true;
    }

    /** Раз в секунду: аура на отряд и видимая граница круга. */
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        for (ServerLevel sl : server.getAllLevels()) {
            for (Mob t : sl.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(
                    -3.0E7, sl.getMinBuildHeight(), -3.0E7, 3.0E7, sl.getMaxBuildHeight(), 3.0E7),
                    e -> e.isAlive() && e instanceof com.andrey.litrpg.entity.TotemEntity)) {
                aura(sl, t);
            }
        }
    }

    private static void aura(ServerLevel sl, Mob totem) {
        Kind kind = Kind.byName(totem.getPersistentData().getString(TAG));
        if (kind == null) return;
        // граница круга видна всегда: без этого непонятно, докуда тотем достаёт
        Fx.ring(sl, totem.position(), RADIUS - 0.4, RADIUS, 22, kind.color);
        Fx.spiral(sl, totem, 0.6, 2.0, 1.5, 22, kind.color);
        MobEffect effect = switch (kind) {
            case GUARD -> ModEffects.BULWARK.get();
            case FURY -> ModEffects.MIGHT.get();
            case MENDING -> MobEffects.REGENERATION;
            case WIND -> MobEffects.MOVEMENT_SPEED;
        };
        for (Player pl : sl.players()) {
            if (!pl.isAlive() || pl.isSpectator() || pl.distanceTo(totem) > RADIUS) continue;
            pl.addEffect(new MobEffectInstance(effect, 40, kind == Kind.MENDING ? 0 : 1, false, false, true));
        }
    }
}
