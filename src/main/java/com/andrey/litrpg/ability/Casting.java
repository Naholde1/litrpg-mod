package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.network.CastPacket;
import com.andrey.litrpg.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Заклинания с подготовкой (CAST_HEAL, CAST_BOLT): игрок стоит и «читает» — на экране полоса каста.
 * Шаг в сторону или прыжок сбивает заклинание (мана и перезарядка не тратятся). Нажать навык ещё раз — тоже отмена.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Casting {
    private Casting() {}

    private record Cast(Ability a, int total, Vec3 at, long started) {}

    private static final Map<UUID, Cast> CASTS = new HashMap<>();

    public static boolean isCasting(ServerPlayer p) {
        return CASTS.containsKey(p.getUUID());
    }

    public static boolean isCast(Ability a) {
        // Подъём павшего — обряд, а не кнопка: десять секунд на месте, как и у приседа
        // у тела. Иначе боевое воскрешение ничего не стоило бы, кроме откатa.
        return a.kind == Ability.Kind.CAST_HEAL || a.kind == Ability.Kind.CAST_BOLT || a.kind == Ability.Kind.REZ;
    }

    /** Начать подготовку. @return true — началась (или отменена повторным нажатием) */
    public static void start(ServerPlayer p, Ability a) {
        Cast cur = CASTS.get(p.getUUID());
        if (cur != null) {
            cancel(p, cur.a == a ? "Заклинание отменено" : null);
            if (cur.a == a) return;
        }
        int want = a.kind == Ability.Kind.REZ ? com.andrey.litrpg.run.Spirits.rezTicks(p) : a.duration;
        int ticks = Math.max(6, Math.round(want * castSpeed(p)));
        CASTS.put(p.getUUID(), new Cast(a, ticks, p.position(), p.level().getGameTime()));
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CastPacket(a.ordinal(), ticks));
        AbilityHandler.sound(p, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6f, 1.5f);
    }

    /** Скорость каста: сосредоточенность (эффект отряда) ускоряет на 10% за уровень. */
    private static float castSpeed(ServerPlayer p) {
        int f = com.andrey.litrpg.registry.ModEffects.level(p, com.andrey.litrpg.registry.ModEffects.FOCUS);
        return 1f - 0.1f * Math.min(4, f);
    }

    public static void cancel(ServerPlayer p, String why) {
        if (CASTS.remove(p.getUUID()) == null) return;
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CastPacket(-1, 0));
        if (why != null) p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Cast c = CASTS.get(p.getUUID());
        if (c == null) return;
        long t = p.level().getGameTime() - c.started;
        Vec3 now = p.position();
        double moved = Math.hypot(now.x - c.at.x, now.z - c.at.z);
        if (!p.isAlive() || p.isSpectator() || moved > 0.45 || now.y - c.at.y > 0.6) {
            cancel(p, "Заклинание сбито — двигаться нельзя");
            AbilityHandler.sound(p, SoundEvents.FIRE_EXTINGUISH, 0.6f, 1.6f);
            return;
        }
        if (t % 6 == 0) {
            int col = c.a.kind == Ability.Kind.CAST_HEAL ? 0xC060FF90 : Fx.color(c.a.element == null ? null : c.a.element.name());
            Fx.spiral(p.level(), p, 0.9, 2.2, 1.2, 8, col);
        }
        if (t >= c.total) {
            CASTS.remove(p.getUUID());
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new CastPacket(-1, 0));
            AbilityHandler.complete(p, c.a);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        CASTS.remove(e.getEntity().getUUID());
    }
}
