package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Симбиот: делит чужую боль.
 * <p>
 * Нить жизни забирает половину урона союзника на симбиота, «Впитать» — весь урон на пять
 * секунд, общая сеть — по пятой части со всего отряда. «Общая боль» связывает его с врагом:
 * что получает симбиот, получает и помеченный. «Пересадка» лечит союзника плотью симбиота,
 * а «Разрыв нитей» рвёт все связи разом — и бьёт тем сильнее, чем их было больше.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Symbiote {
    private static final int BOND = 0xD0A070FF;
    private static final int FLESH = 0xD0E070A0;
    /** На симбиоте: связанный «общей болью» враг (UUID) и срок. */
    private static final String PAIN = "litrpg_sy_pain";

    private Symbiote() {}

    static void register() {
        Special.put("sy_link", (p, s, a) -> bind(p, s, a, 0.5f, false));
        Special.put("sy_absorb", (p, s, a) -> bind(p, s, a, 1.0f, true));
        Special.put("sy_web", Symbiote::web);
        Special.put("sy_share", Symbiote::share);
        Special.put("sy_graft", Symbiote::graft);
        Special.put("sy_rupture", Symbiote::rupture);
        Special.put("sy_strike", Symbiote::strike);
    }

    /** Связать союзника: доля его урона уходит симбиоту. «Впитать» — всё и с заметным щитом. */
    private static boolean bind(ServerPlayer p, PlayerStats s, Ability a, float part, boolean full) {
        LivingEntity t = AbilityHandler.allyTarget(p, a.radius, false);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(t, Kit.GUARD, part, life);
        t.getPersistentData().putUUID(Kit.GUARD + "_by", p.getUUID());
        ServerLevel sl = p.serverLevel();
        Fx.chains(sl, p, t, full ? 0.5 : 0.35, life, BOND);
        Fx.tether(sl, p, t, full ? 0.2 : 0.1, life, 0x80A070FF);
        if (full) {
            Fx.shield(sl, t, 1.3, life, 0x60A070FF);
            Kit.msg(p, t.getName().getString() + " под вашей кожей: весь урон — вам.", ChatFormatting.LIGHT_PURPLE);
        }
        Kit.sound(sl, t.position(), SoundEvents.SCULK_CLICKING, 1.2f, 0.8f);
        return true;
    }

    /** Общая сеть: бафы всем и нить к каждому — по пятой части урона любого из отряда уходит в симбиота. */
    private static boolean web(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int life = Kit.dur(s, a);
        for (LivingEntity e : Kit.alliesIn(p, p.position(), a.radius)) {
            AbilityHandler.applySelf(a, e);
            if (e == p) continue;
            Kit.until(e, Kit.GUARD, 0.2f, life);
            e.getPersistentData().putUUID(Kit.GUARD + "_by", p.getUUID());
            Fx.tether(sl, p, e, 0.07, life, 0x70A070FF);
        }
        Fx.runes(sl, p.position(), 3, 6, 40, BOND);
        Kit.sound(sl, p.position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, 0.6f, 1.6f);
        return true;
    }

    /** Общая боль: связать врага — всё, что получает симбиот, получает и он. */
    private static boolean share(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        Kit.until(p, PAIN, life);
        p.getPersistentData().putUUID(PAIN + "_t", t.getUUID());
        Fx.chains(p.level(), p, t, 0.4, life, FLESH);
        Fx.mark(p.level(), t, 0.6, 1, life, FLESH);
        Kit.sound(p.serverLevel(), t.position(), SoundEvents.SCULK_CATALYST_BLOOM, 1.2f, 0.8f);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Kit.active(p, PAIN) || !p.getPersistentData().hasUUID(PAIN + "_t")) return;
        if (e.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) return;
        if (p.serverLevel().getEntity(p.getPersistentData().getUUID(PAIN + "_t")) instanceof LivingEntity t && t.isAlive()) {
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().thorns(p), e.getAmount() * 1.25f);
        }
    }

    /** Пересадка: союзник лечится плотью симбиота — тот теряет пятую часть отданного. */
    private static boolean graft(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = AbilityHandler.allyTarget(p, a.radius, false);
        if (t == null) return AbilityHandler.noTarget(p);
        float heal = Kit.power(s, a) * AbilityHandler.healMult(s);
        float before = t.getHealth();
        Kit.heal(p, t, heal);
        float given = t.getHealth() - before;
        p.setHealth(Math.max(1f, p.getHealth() - given * 0.2f));
        ServerLevel sl = p.serverLevel();
        Fx.beam(sl, p.position().add(0, 1, 0), t.position().add(0, 1, 0), 0.3, 12, FLESH);
        Fx.spiral(sl, t, 0.7, 2, 2, 16, FLESH);
        Kit.sound(sl, t.position(), SoundEvents.HONEY_BLOCK_BREAK, 1.2f, 0.8f);
        return true;
    }

    /** Разрыв нитей: все нити симбиота лопаются; удар вокруг на +30% за каждую разорванную. */
    private static boolean rupture(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        int links = 0;
        for (LivingEntity e : Kit.alliesIn(p, p.position(), 40)) {
            if (Kit.active(e, Kit.GUARD) && e.getPersistentData().hasUUID(Kit.GUARD + "_by")
                    && p.getUUID().equals(e.getPersistentData().getUUID(Kit.GUARD + "_by"))) {
                Kit.clear(e, Kit.GUARD);
                Fx.beam(sl, e.position().add(0, 1, 0), p.position().add(0, 1, 0), 0.25, 10, BOND);
                links++;
            }
        }
        if (Kit.active(p, PAIN)) {
            Kit.clear(p, PAIN);
            links++;
        }
        float dmg = Kit.power(s, a) * (1f + 0.3f * links);
        List<LivingEntity> hit = Kit.enemiesIn(p, p.position(), a.radius);
        for (LivingEntity e : hit) Kit.dmg(p, a, e, dmg);
        Fx.burst(sl, p.position().add(0, 1, 0), a.radius, 16 + links * 4, 12, BOND);
        Fx.ring(sl, p.position(), 0.5, a.radius, 12, FLESH, 0.6f);
        Kit.sound(sl, p.position(), SoundEvents.SCULK_SHRIEKER_BREAK, 1.4f, 0.7f);
        if (links > 0) Kit.msg(p, "Разорвано нитей: " + links + " — удар сильнее на " + links * 30 + "%", ChatFormatting.LIGHT_PURPLE);
        return true;
    }

    /** Касание связи: удар по цели — и тот же удар наполовину по врагу, связанному «общей болью». */
    private static boolean strike(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        Kit.dmg(p, a, t, dmg);
        Fx.slash(sl, t.position().add(0, t.getBbHeight() * 0.6, 0), AbilityHandler.flatLook(p), 1.8, 110, -20, 8, BOND);
        if (Kit.active(p, PAIN) && sl.getEntity(p.getPersistentData().getUUID(PAIN + "_t")) instanceof LivingEntity o && o != t && o.isAlive()) {
            Kit.dmg(p, a, o, dmg * 0.5f);
            Fx.beam(sl, t.position().add(0, 1, 0), o.position().add(0, 1, 0), 0.12, 8, FLESH);
        }
        Kit.sound(sl, t.position(), SoundEvents.SLIME_ATTACK, 1f, 0.6f);
        return true;
    }
}
