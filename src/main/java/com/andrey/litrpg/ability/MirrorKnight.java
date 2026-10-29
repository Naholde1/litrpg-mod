package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Зерцало: живёт чужими ударами.
 * <p>
 * «Копить» впитывает урон в стекло — сколько впитал, столько и лежит в запасе; «Вернуть сполна»
 * выплёскивает запас вокруг. Грань отражает половину ударов вблизи. Осколок отскакивает от
 * первой цели во вторую. В зеркальном зале враги бьют мимо — их урон теряется в отражениях.
 * «Расколоть» — веер осколков перед собой, оглушающий звоном.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class MirrorKnight {
    private static final int GLASS = 0xD0B0E8FF;
    private static final int SILVER = 0xE0E8F0FF;
    /** До какого тика копится урон и сколько накоплено. */
    private static final String STORE = "litrpg_zr_store";
    private static final String STORED = "litrpg_zr_stored";

    private MirrorKnight() {}

    static void register() {
        Special.put("zr_store", MirrorKnight::store);
        Special.put("zr_release", MirrorKnight::release);
        Special.put("zr_facet", MirrorKnight::facet);
        Special.put("zr_shard", MirrorKnight::shard);
        Special.put("zr_hall", MirrorKnight::hall);
        Special.put("zr_shatter", MirrorKnight::shatter);
    }

    private static boolean store(ServerPlayer p, PlayerStats s, Ability a) {
        AbilityHandler.applySelf(a, p);
        int life = Kit.dur(s, a);
        Kit.until(p, STORE, life);
        Fx.shield(p.level(), p, 1.2, life, 0x60B0E8FF);
        Fx.orbit(p.level(), p, 1.2, 4, 1.2, 0.15, life, SILVER);
        Kit.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.8f);
        Kit.msg(p, "Стекло впитывает удары — потом верните их сполна.", ChatFormatting.AQUA);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Kit.active(p, STORE)) return;
        var d = p.getPersistentData();
        d.putFloat(STORED, Math.min(p.getMaxHealth() * 3, d.getFloat(STORED) + e.getAmount()));
    }

    /** Вернуть сполна: удар вокруг — сила навыка плюс весь накопленный в стекле урон. */
    private static boolean release(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float stored = p.getPersistentData().getFloat(STORED);
        p.getPersistentData().putFloat(STORED, 0);
        Kit.clear(p, STORE);
        float dmg = Kit.power(s, a) + stored;
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius)) Kit.dmg(p, a, e, dmg);
        Fx.shards(sl, p.position(), a.radius, 18 + (int) Math.min(20, stored / 4), 2.2, 24, GLASS);
        Fx.sphere(sl, p.position().add(0, 1, 0), 0.5, a.radius, 12, SILVER);
        Kit.sound(sl, p.position(), SoundEvents.GLASS_BREAK, 1.5f, 0.6f);
        if (stored > 0) Kit.msg(p, String.format("Возвращено сполна: +%.0f урона из стекла", stored), ChatFormatting.AQUA);
        return true;
    }

    /** Грань: баф из данных и отражение половины каждого удара вблизи. */
    private static boolean facet(ServerPlayer p, PlayerStats s, Ability a) {
        AbilityHandler.applySelf(a, p);
        int life = Kit.dur(s, a);
        Kit.until(p, Kit.REFLECT, 0.5f, life);
        Fx.shield(p.level(), p, 1.25, life, 0x80E8F0FF);
        Fx.shards(p.level(), p.position(), 1.4, 6, 1.4, life, GLASS);
        Kit.sound(p.serverLevel(), p.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, 1.2f, 1f);
        return true;
    }

    /** Осколок: в цель под прицелом, затем отскакивает в ближайшего к ней врага (вполсилы). */
    private static boolean shard(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        Vec3 from = p.getEyePosition().subtract(0, 0.3, 0);
        Vec3 at = t.position().add(0, t.getBbHeight() * 0.6, 0);
        Fx.beam(sl, from, at, 0.14, 8, SILVER);
        Kit.dmg(p, a, t, dmg);
        Fx.shards(sl, t.position(), 0.8, 5, 1, 14, GLASS);
        LivingEntity next = null;
        double best = 64;
        for (LivingEntity e : Kit.enemiesIn(p, t.position(), 8)) {
            if (e == t) continue;
            double d = e.distanceToSqr(t);
            if (d < best) {
                best = d;
                next = e;
            }
        }
        if (next != null) {
            final LivingEntity n = next;
            Scheduler.later(4, () -> {
                if (!n.isAlive() || !p.isAlive()) return;
                Fx.beam(sl, at, n.position().add(0, n.getBbHeight() * 0.6, 0), 0.1, 8, GLASS);
                Kit.dmg(p, a, n, dmg * 0.5f);
            });
        }
        Kit.sound(sl, t.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1.4f, 1.4f);
        return true;
    }

    /** Зеркальный зал: враги в круге наносят на 30% меньше, свои — получают на 15% меньше. */
    private static boolean hall(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        int life = Kit.dur(s, a);
        Fx.runes(sl, c, a.radius, 6, life, GLASS);
        Kit.sound(sl, c, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.6f);
        Scheduler.repeat(0, 10, life / 10, i -> {
            if (!p.isAlive()) return false;
            for (LivingEntity e : Kit.enemiesIn(p, c, a.radius)) Kit.until(e, Kit.DEALT, 0.7f, 14);
            for (LivingEntity e : Kit.alliesIn(p, c, a.radius)) Kit.until(e, Kit.TAKEN, 0.85f, 14);
            if (i % 3 == 0) {
                // стеклянные «стены» зала то тут, то там
                double ang = sl.random.nextDouble() * Math.PI * 2;
                Vec3 at = c.add(Math.cos(ang) * a.radius * 0.8, 0, Math.sin(ang) * a.radius * 0.8);
                Fx.pillar(sl, at, 0.6, 3, 24, 0x60B0E8FF);
            }
            return true;
        });
        return true;
    }

    /** Расколоть: веер осколков перед собой; задетые оглушены звоном. */
    private static boolean shatter(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 dir = AbilityHandler.flatLook(p);
        float dmg = Kit.power(s, a);
        int st = Math.round(a.duration * s.rankDuration(a));
        for (LivingEntity e : Kit.enemiesIn(p, p.position(), a.radius + 1)) {
            Vec3 d = e.position().subtract(p.position()).multiply(1, 0, 1);
            if (d.lengthSqr() > 1 && d.normalize().dot(dir) < 0.5) continue;
            Kit.dmg(p, a, e, dmg);
            Kit.stun(e, st);
            Fx.shards(sl, e.position(), 0.7, 4, 1.2, 16, GLASS);
        }
        Fx.cone(sl, p.position(), dir, a.radius + 1, 60, 8, 0x80B0E8FF);
        Fx.burst(sl, p.position().add(dir.scale(1.5)).add(0, 1, 0), a.radius, 14, 10, SILVER);
        Kit.sound(sl, p.position(), SoundEvents.GLASS_BREAK, 1.5f, 1.2f);
        return true;
    }
}
