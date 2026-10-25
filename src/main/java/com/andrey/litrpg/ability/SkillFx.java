package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Светящиеся эффекты навыков игроков ({@link Fx}) — поверх частиц самих навыков: волны, взмахи, лучи,
 * столбы света, щиты и знаки. Цвет — по стихии навыка (без стихии — по роли: сталь, лечение).
 */
final class SkillFx {
    private SkillFx() {}

    static int color(Ability a) {
        if (a.element != null) return Fx.color(a.element.name());
        return switch (a.kind) {
            case HEAL, HEAL_SELF, HEAL_TARGET, TETHER, WARD, SHIELD, REZ, MEDITATE -> Fx.color("HEAL");
            case TAUNT -> 0xD0FF5030;
            case BUFF_ALLIES, BUFF_SELF -> 0xC0FFD060;
            default -> 0xD0E8E8F0;
        };
    }

    static void play(ServerPlayer p, Ability a) {
        var l = p.level();
        int c = color(a);
        Vec3 feet = p.position(), chest = p.position().add(0, 1.1, 0);
        Vec3 dir = AbilityHandler.flatLook(p);
        switch (a.kind) {
            case NOVA, MULTI_NOVA, LAUNCH -> {
                Fx.ring(l, feet, 0.5, a.radius + 0.5, 10, c, 0.5f);
                Fx.sphere(l, chest, 0.4, Math.min(3, a.radius * 0.5), 8, c & 0x80FFFFFF);
            }
            case STRIKE, MULTI_STRIKE, DASH_STRIKE, STEAL, STUN -> Fx.slash(l, chest, dir, 2.6, 110, (p.tickCount % 2 == 0 ? 20 : -20), 6, c);
            case CONE -> Fx.slash(l, chest, dir, Math.max(3, a.radius), 70, 0, 8, c);
            case BEAM -> Fx.beam(l, p.getEyePosition().subtract(0, 0.3, 0), p.getEyePosition().add(p.getLookAngle().scale(a.radius)), 0.6, 8, c);
            case LEAP_SLAM -> Fx.ring(l, feet, 0.5, a.radius + 1, 14, c, 0.6f);
            case TAUNT -> {
                Fx.ring(l, feet, 0.5, a.radius, 12, c, 0.4f);
                Fx.mark(l, p, 0.5, 2, 30, c);
            }
            case BUFF_ALLIES, PURIFY, GATHER -> {
                Fx.pillar(l, feet, 0.8, 3, 14, c);
                Fx.ring(l, feet, 0.5, a.radius, 14, c, 0.25f);
            }
            case BUFF_SELF, ON_HIT, STEALTH, SACRIFICE -> Fx.spiral(l, p, 0.8, 2.3, 2, 14, c);
            case HEAL, HEAL_SELF -> {
                Fx.pillar(l, feet, 1.0, 3, 16, c);
                Fx.ring(l, feet, 0.3, a.kind == Ability.Kind.HEAL ? a.radius : 2, 14, c, 0.3f);
            }
            case SHIELD, WARD -> {
                LivingEntity t = Dynamic.healTarget(p, a.radius);
                Fx.shield(l, t, 1.1, Math.max(40, a.duration), c);
            }
            case TETHER -> {
                LivingEntity t = Dynamic.healTarget(p, a.radius);
                if (t != p) Fx.tether(l, p, t, 0.2, Math.max(20, a.duration), c);
            }
            case REZ -> Fx.pillar(l, feet, 1.5, 6, 30, 0xE0FFF8C0);
            case DASH, BLINK, SHADOW_STEP, RALLY, GRAPPLE, SWAP, RECALL -> Fx.ring(l, feet, 0.2, 1.4, 8, c, 0.3f);
            case METEOR, ZONE_AT, ARROW_RAIN, VORTEX, STASIS -> {
                Vec3 at = AbilityHandler.lookPoint(p, 32);
                Fx.ring(l, at, 0.3, Math.max(2, a.radius), 14, c, 0.4f);
                Fx.pillar(l, at, Math.max(1, a.radius * 0.4), 6, 12, c);
            }
            case ZONE -> Fx.ring(l, feet, 0.3, a.radius, Math.min(60, Math.max(10, a.duration)), c, 0.15f);
            case INTERRUPT -> {
                LivingEntity t = AbilityHandler.lookEnemy(p, a.radius);
                if (t != null) Fx.sphere(l, t.position().add(0, t.getBbHeight() * 0.6, 0), 0.2, 1.4, 6, 0xF0FFFFFF);
            }
            case MARK, REVEAL -> {
                LivingEntity t = AbilityHandler.lookEnemy(p, Math.max(16, a.radius));
                if (t != null) Fx.mark(l, t, 0.45, 2, Math.max(40, a.duration), 0xD0FF4040);
            }
            case GLOBAL, CHAIN, LIGHTNING -> Fx.sphere(l, chest, 0.5, 2.5, 8, c);
            case SUMMON -> Fx.pillar(l, feet.add(dir.scale(2)), 1, 2.5, 14, c);
            case WALL, PUSH, PULL -> Fx.slash(l, chest, dir, 3.5, 100, 0, 8, c);
            default -> { }
        }
    }
}
