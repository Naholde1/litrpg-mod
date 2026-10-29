package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Призыватель играет не собой, а армией — и его способности должны это делать по-настоящему.
 * <p>
 * До этого «Разрыв душ» был обычной волной вокруг хозяина, хотя в описании сказано, что
 * лопаются слуги. Теперь лопается каждый: сколько слуг, столько и волн, и в гуще врагов
 * они накладываются. Остальные три тоже работают с армией, а не рядом с ней.
 */
public final class Summoner {
    /** Насколько далеко призыватель чувствует своих. */
    private static final double REACH = 26;
    private static final int SOUL = 0xD0B388FF;
    private static final int SOUL_DEEP = 0xC06A2C8F;

    private Summoner() {}

    static void register() {
        Special.put("sm_burst", Summoner::burst);
        Special.put("sm_sacrifice", Summoner::expend);
        Special.put("sm_command", Summoner::command);
        Special.put("sm_shield", Summoner::soulShield);
    }

    /** Живые прислужники хозяина поблизости, ближние первыми. */
    public static List<Mob> minions(ServerPlayer p) {
        List<Mob> out = new ArrayList<>();
        if (!(p.level() instanceof ServerLevel sl)) return out;
        for (Mob m : sl.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(REACH),
                e -> e.isAlive() && p.getUUID().equals(Minions.ownerOf(e)))) {
            out.add(m);
        }
        out.sort(Comparator.comparingDouble(m -> m.distanceToSqr(p)));
        return out;
    }

    private static boolean none(ServerPlayer p, String why) {
        p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.GRAY), true);
        return false;
    }

    /**
     * Разрыв душ: каждый слуга лопается собственной волной на своём месте.
     * Чем больше армия, тем больнее — не потому, что множитель, а потому что
     * в толпе волны накрывают одних и тех же врагов по несколько раз.
     */
    private static boolean burst(ServerPlayer p, PlayerStats s, Ability a) {
        List<Mob> army = minions(p);
        if (army.isEmpty()) return none(p, "Некого разрывать — сначала призовите слуг.");
        ServerLevel sl = p.serverLevel();
        float per = AbilityHandler.base(s, a) * a.power;
        int hits = 0;
        for (Mob m : army) {
            Vec3 c = m.position().add(0, m.getBbHeight() * 0.5, 0);
            // своя вспышка у каждого: шар души, кольцо по земле и столб вверх
            Fx.sphere(sl, c, 0.3, a.radius, 12, SOUL);
            Fx.ring(sl, m.position(), 0.2, a.radius, 16, SOUL_DEEP);
            Fx.pillar(sl, m.position(), 0.5, 3.2, 12, SOUL);
            for (LivingEntity e : AbilityHandler.enemiesAround(p, m.getBoundingBox().inflate(a.radius))) {
                if (AbilityHandler.hurt(p, a, e, per)) hits++;
            }
            sl.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.1f, 0.7f);
            m.discard();
        }
        p.displayClientMessage(Component.literal("Разорвано слуг: " + army.size() + ", попаданий: " + hits)
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /**
     * Разменять: ближайший слуга рассыпается, и его остаток сил переходит хозяину.
     * Чем целее слуга, тем больше вернётся — добивать своих выгоднее целыми.
     */
    private static boolean expend(ServerPlayer p, PlayerStats s, Ability a) {
        List<Mob> army = minions(p);
        if (army.isEmpty()) return none(p, "Разменивать некого.");
        Mob m = army.get(0);
        float left = m.getHealth() / Math.max(1f, m.getMaxHealth());
        double back = s.getMaxResource() * a.power * (0.4 + 0.6 * left);
        s.setResource(s.getResource() + back);
        p.heal(p.getMaxHealth() * 0.08f * (0.4f + 0.6f * left));
        Fx.tether(p.level(), m, p, 0.3, 14, SOUL);
        Fx.spiral(p.level(), p, 0.8, 2.2, 2, 18, SOUL);
        Fx.ring(p.level(), m.position(), 0.2, 1.6, 12, SOUL_DEEP);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.6f);
        m.discard();
        p.displayClientMessage(Component.literal("Слуга разменян: " + Math.round(back) + " "
                + s.resource().genitive).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /**
     * Приказ: вся армия бросается на то, на что смотрит хозяин, и на время звереет.
     * Без этого слуги дрались сами по себе и в бою с хранителем расходились по углам.
     */
    private static boolean command(ServerPlayer p, PlayerStats s, Ability a) {
        List<Mob> army = minions(p);
        if (army.isEmpty()) return none(p, "Приказывать некому.");
        LivingEntity target = Dynamic.aimEnemy(p, 28);
        int dur = a.duration;
        for (Mob m : army) {
            if (target != null && target.isAlive()) m.setTarget(target);
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, dur, 1, false, false, true));
            m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, dur, 1, false, false, true));
            Fx.tether(p.level(), p, m, 0.22, 20, SOUL);
            Fx.follow(p.level(), m, 0.7, dur, SOUL_DEEP);
        }
        if (target != null) Fx.mark(p.level(), target, 0.8, 1, dur, SOUL);
        p.serverLevel().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.6f);
        p.displayClientMessage(Component.literal(target == null
                ? "Армия рвётся вперёд (" + army.size() + ")"
                : "Армия идёт на " + target.getName().getString() + " (" + army.size() + ")")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    /**
     * Щит из душ: слуги встают кольцом вокруг союзника и принимают часть ударов.
     * Чем больше армия, тем крепче щит — у призывателя защита тоже идёт от армии.
     */
    private static boolean soulShield(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Dynamic.healTarget(p, a.radius);
        int army = minions(p).size();
        float amount = AbilityHandler.base(s, a) * a.power * (1f + 0.35f * army);
        t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, a.duration, Math.min(4, army), false, false, true));
        t.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, a.duration, army >= 3 ? 1 : 0, false, false, true));
        Fx.shield(p.level(), t, 1.1, a.duration, SOUL);
        for (Mob m : minions(p)) Fx.tether(p.level(), m, t, 0.18, 24, SOUL_DEEP);
        p.serverLevel().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.SOUL_SAND_BREAK, SoundSource.PLAYERS, 1f, 1.4f);
        p.displayClientMessage(Component.literal("Щит из душ: " + Math.round(amount) + " (слуг " + army + ")")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }
}
