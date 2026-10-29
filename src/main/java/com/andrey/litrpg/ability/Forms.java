package com.andrey.litrpg.ability;

import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Resource;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Облики: усиление держится не по таймеру, а пока в шкале есть запас.
 * <p>
 * Оборотень носит волка, медведя и ворона, Дух — свою одержимость: это не «бафф на 14 секунд»,
 * а состояние, которое тратит шкалу каждую секунду и спадает, когда она кончилась. Повторное
 * нажатие той же кнопки снимает облик, нажатие другой — меняет его, двух сразу не бывает.
 */
public final class Forms {
    /** Сколько шкалы уходит в секунду, пока облик держится. */
    public static final double DRAIN = 4;
    /** На сколько тиков накладываются эффекты облика (обновляются каждую секунду). */
    private static final int REFRESH = 60;

    private Forms() {}

    /**
     * Нажата способность-облик: войти или сменить облик.
     * Снятие сюда не доходит — его разбирает {@link AbilityHandler}, потому что
     * отказ от формы не должен стоить ресурса и уходить в откат.
     */
    static boolean toggle(ServerPlayer p, PlayerStats s, Ability a) {
        String was = s.getForm();
        if (was != null) clear(p, Ability.byKey(was));
        s.setForm(a.key);
        apply(p, a);
        broadcast(p, a.key);
        p.serverLevel().sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1, p.getZ(), 30, 0.5, 0.7, 0.5, 0.05);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable(a.nameKey()).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(" — держится, пока есть шкала.").withStyle(ChatFormatting.GRAY)), true);
        return true;
    }

    /** Раз в секунду: тратим шкалу, обновляем эффекты, спадает при нуле. */
    public static void tick(ServerPlayer p, PlayerStats s) {
        String key = s.getForm();
        if (key == null) return;
        Ability a = Ability.byKey(key);
        if (a == null || s.resource() != Resource.FORM) {
            leave(p, s, null);
            return;
        }
        double left = s.getResource() - DRAIN;
        if (left <= 0) {
            s.setResource(0);
            leave(p, s, "Шкала кончилась — облик спал.");
            return;
        }
        s.setResource(left);
        apply(p, a);
        // повторяем раз в секунду: тот, кто только что подошёл, тоже должен увидеть зверя
        broadcast(p, key);
    }

    /** Снять облик (смерть, смена класса, конец шкалы). */
    public static void leave(ServerPlayer p, PlayerStats s, String why) {
        String key = s.getForm();
        if (key == null) return;
        s.setForm(null);
        Ability a = Ability.byKey(key);
        if (a != null) clear(p, a);
        broadcast(p, null);
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FOX_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        StatsManager.sync(p);   // клиенту нужен сам факт облика, а не только шкала
    }

    /** Сообщить всем, кто видит игрока, какой на нём облик (или что облика нет). */
    public static void broadcast(ServerPlayer p, String key) {
        com.andrey.litrpg.network.ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p),
                new com.andrey.litrpg.network.FormPacket(p.getUUID(), key));
    }

    private static void apply(ServerPlayer p, Ability a) {
        for (Ability.Fx fx : a.self) {
            p.addEffect(new MobEffectInstance(fx.effect(), REFRESH, fx.amplifier(), false, false, true));
        }
        show(p, a);
    }

    /**
     * Облик должен быть виден со стороны, а не только в цифрах: пока он держится,
     * вокруг игрока идут свои частицы, и каждый облик — свои.
     */
    private static void show(ServerPlayer p, Ability a) {
        var sl = p.serverLevel();
        var kind = a.key.substring(a.key.lastIndexOf('_') + 1);
        net.minecraft.core.particles.SimpleParticleType dust = switch (kind) {
            case "wolf" -> ParticleTypes.CLOUD;
            case "bear" -> ParticleTypes.CRIT;
            case "raven" -> ParticleTypes.SQUID_INK;
            default -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        int color = switch (kind) {
            case "wolf" -> 0xC0D8E8F0;
            case "bear" -> 0xC08B5A2B;
            case "raven" -> 0xC0303040;
            default -> 0xC0FFB040;
        };
        // кольцо под ногами и облачко по силуэту — видно и самому, и товарищам
        com.andrey.litrpg.fx.Fx.ring(sl, p.position().add(0, 0.1, 0), 0.5, 1.1, 22, color);
        sl.sendParticles(dust, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.35, 0.7, 0.35, 0.01);
    }

    private static void clear(ServerPlayer p, Ability a) {
        if (a == null) return;
        for (Ability.Fx fx : a.self) p.removeEffect(fx.effect());
    }
}
