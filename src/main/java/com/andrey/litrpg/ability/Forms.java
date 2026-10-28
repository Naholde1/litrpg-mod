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

    /** Нажата способность-облик: войти, сменить или выйти. */
    static boolean toggle(ServerPlayer p, PlayerStats s, Ability a) {
        if (a.key.equals(s.getForm())) {
            leave(p, s, "Облик снят.");
            return true;
        }
        String was = s.getForm();
        if (was != null) clear(p, Ability.byKey(was));
        s.setForm(a.key);
        apply(p, a);
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
    }

    /** Снять облик (смерть, смена класса, конец шкалы). */
    public static void leave(ServerPlayer p, PlayerStats s, String why) {
        String key = s.getForm();
        if (key == null) return;
        s.setForm(null);
        Ability a = Ability.byKey(key);
        if (a != null) clear(p, a);
        if (why != null) {
            p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FOX_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        StatsManager.syncMana(p);
    }

    private static void apply(ServerPlayer p, Ability a) {
        for (Ability.Fx fx : a.self) {
            p.addEffect(new MobEffectInstance(fx.effect(), REFRESH, fx.amplifier(), false, false, true));
        }
    }

    private static void clear(ServerPlayer p, Ability a) {
        if (a == null) return;
        for (Ability.Fx fx : a.self) p.removeEffect(fx.effect());
    }
}
