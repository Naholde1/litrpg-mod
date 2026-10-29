package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

/**
 * Оракул: знает, что будет, и успевает это поправить.
 * <p>
 * Дурной знак: помеченному не везёт — каждый четвёртый его удар уходит мимо, а сам он
 * получает больше. Щит предвидения закрывает от первого тяжёлого удара вдвое. «Переписать»
 * лечит всех, снимает дурное и на четыре секунды не даёт никому умереть. Полное видение
 * подсвечивает всех врагов вокруг сквозь стены.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Oracle {
    private static final int FATE = 0xD0F8E890;
    private static final int OMEN = 0xC0A060FF;
    private static final String OMEN_TAG = "litrpg_or_omen";
    private static final String FORESEEN = "litrpg_or_foreseen";

    private Oracle() {}

    static void register() {
        Special.put("or_mark", Oracle::omen);
        Special.put("or_shield", Oracle::foreseen);
        Special.put("or_fate", Oracle::rewrite);
        Special.put("or_vision", Oracle::vision);
    }

    private static boolean omen(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.TAKEN, Math.max(1.1f, a.power), life);
        Kit.until(t, OMEN_TAG, life);
        Fx.mark(p.level(), t, 0.7, 2, life, OMEN);
        Fx.orbit(p.level(), t, 0.8, 3, t.getBbHeight() + 0.3, 0.12, life, OMEN);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.ENDER_EYE_DEATH, 1f, 0.6f);
        return true;
    }

    /** Щит предвидения: обычный щит, и первый удар больше 15% здоровья союзника режется вдвое. */
    private static boolean foreseen(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.ally(p, a.radius);
        float amount = Kit.power(s, a) * AbilityHandler.healMult(s);
        t.setAbsorptionAmount(Math.max(t.getAbsorptionAmount(), amount));
        int life = Kit.dur(s, a);
        Kit.until(t, FORESEEN, life);
        Fx.shield(p.level(), t, 1.2, life, 0x50F8E890);
        Fx.orbit(p.level(), t, 1.1, 2, 1.2, 0.14, life, FATE);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.2f);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide) return;
        // дурной знак: каждый четвёртый удар помеченного мимо
        if (e.getSource().getEntity() instanceof LivingEntity att && Kit.active(att, OMEN_TAG) && att.getRandom().nextFloat() < 0.25f) {
            e.setCanceled(true);
            if (t.level() instanceof ServerLevel sl) Fx.ring(sl, t.position().add(0, 1, 0), 0.2, 1.2, 8, OMEN);
            return;
        }
        if (Kit.active(t, FORESEEN) && e.getAmount() > t.getMaxHealth() * 0.15f) {
            e.setAmount(e.getAmount() * 0.5f);
            Kit.clear(t, FORESEEN);
            if (t.level() instanceof ServerLevel sl) {
                Fx.burst(sl, t.position().add(0, 1, 0), 1.6, 10, 10, FATE);
                sl.playSound(null, t.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.4f);
            }
        }
    }

    /** Переписать: лечит всех вокруг, снимает вредное и на 4 секунды не даёт никому умереть. */
    private static boolean rewrite(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float heal = Dynamic.healAmount(s, a, 1f);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            Kit.heal(p, e, heal);
            for (MobEffectInstance m : new ArrayList<>(e.getActiveEffects())) {
                if (m.getEffect().getCategory() == MobEffectCategory.HARMFUL) e.removeEffect(m.getEffect());
            }
            SkillEffects.ward(e, 80);
            Fx.pillar(sl, e.position(), 0.7, 5, 20, FATE);
            Fx.runes(sl, e.position(), 1.2, 5, 80, FATE);
        }
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 16, 0x60F8E890);
        Kit.sound(sl, p.position(), SoundEvents.BEACON_POWER_SELECT, 1.4f, 1.8f);
        Kit.msg(p, "Судьба переписана: четыре секунды никто не умрёт.", ChatFormatting.GOLD);
        return true;
    }

    /** Полное видение: бафы отряду из данных и все враги в 30 блоках подсвечены сквозь стены. */
    private static boolean vision(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int life = Kit.dur(s, a);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) AbilityHandler.applySelf(a, e);
        int seen = 0;
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), 30)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, life, 0, false, false));
            seen++;
        }
        Fx.runes(sl, p.position(), 3, 9, 40, FATE);
        Fx.ring(sl, p.position(), 0.5, 30, 24, FATE, 0.3f);
        Kit.sound(sl, p.position(), SoundEvents.ENDER_EYE_LAUNCH, 1.4f, 0.6f);
        Kit.msg(p, "Видно врагов: " + seen, ChatFormatting.YELLOW);
        return true;
    }
}
