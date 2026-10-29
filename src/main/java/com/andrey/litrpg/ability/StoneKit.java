package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Остальные приёмы Каменного тотемщика (столбы — в {@link Totems}).
 * <p>
 * Хватка земли тянет врага не к самому тотемщику, а к его ближайшему столбу — танкует
 * расстановкой. Гробница смыкает камни над врагами. Гранитная кожа трескается: каждый удар
 * по ней осыпает обидчика каменной крошкой. Каменный кулак сотрясает пол вокруг цели.
 */
public final class StoneKit {
    private static final int STONE = 0xD0B09070;
    private static final int EARTH = 0xC0806040;

    private StoneKit() {}

    static void register() {
        Special.put("tt_drag", StoneKit::drag);
        Special.put("tt_tomb", StoneKit::tomb);
        Special.put("tt_skin", StoneKit::skin);
        Special.put("tt_strike", StoneKit::fist);
    }

    private static void dust(ServerLevel sl, Vec3 at, int n) {
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, at.y + 0.3, at.z, n, 0.4, 0.2, 0.4, 0.1);
    }

    /** Хватка земли: каменная рука тащит врага к ближайшему столбу тотемщика (нет столбов — к нему самому). */
    private static boolean drag(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemy(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        Vec3 to = p.position();
        double best = Double.MAX_VALUE;
        Mob totem = null;
        for (Mob m : Totems.of(p)) {
            double d = m.distanceToSqr(t);
            if (d < best) {
                best = d;
                totem = m;
            }
        }
        if (totem != null) to = totem.position();
        Kit.pull(t, to, Math.min(2.4, 0.4 + t.position().distanceTo(to) * 0.16));
        Kit.dmg(p, a, t, Kit.power(s, a));
        Fx.beam(sl, t.position().add(0, 0.5, 0), to.add(0, 0.5, 0), 0.35, 10, EARTH);
        Fx.shards(sl, t.position(), 0.8, 5, 1.2, 14, STONE);
        dust(sl, t.position(), 20);
        Kit.taunt(p, t);
        Kit.sound(sl, t.position(), SoundEvents.ROOTED_DIRT_BREAK, 1.4f, 0.6f);
        return true;
    }

    /** Каменная гробница: камни смыкаются над врагами в точке прицела — оглушение и урон при раскрытии. */
    private static boolean tomb(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = AbilityHandler.lookPoint(p, 16);
        var list = Kit.enemiesIn(p, c, a.radius);
        if (list.isEmpty()) return AbilityHandler.noTarget(p);
        int life = Kit.dur(s, a);
        float dmg = AbilityHandler.base(s, a) * 1.2f;
        for (LivingEntity e : list) {
            Kit.stun(e, life);
            Fx.shards(sl, e.position(), 1.1, 12, e.getBbHeight() + 0.6, life, STONE);
            dust(sl, e.position(), 25);
        }
        Kit.sound(sl, c, SoundEvents.STONE_PLACE, 1.6f, 0.5f);
        Scheduler.later(life, () -> {
            for (LivingEntity e : list) {
                if (!e.isAlive() || !p.isAlive()) continue;
                Kit.dmg(p, a, e, dmg);
                Fx.burst(sl, e.position().add(0, 1, 0), 1.8, 10, 10, STONE);
                dust(sl, e.position(), 30);
            }
            Kit.sound(sl, c, SoundEvents.STONE_BREAK, 1.6f, 0.6f);
        });
        return true;
    }

    /** Гранитная кожа: бафы из данных, и пятая часть каждого удара вблизи отлетает обидчику крошкой. */
    private static boolean skin(ServerPlayer p, PlayerStats s, Ability a) {
        AbilityHandler.applySelf(a, p);
        int life = Kit.dur(s, a);
        Kit.until(p, Kit.REFLECT, 0.2f, life);
        Fx.shards(p.level(), p.position(), 1, 8, 1.6, life, STONE);
        dust(p.serverLevel(), p.position(), 30);
        Kit.sound(p.serverLevel(), p.position(), SoundEvents.DEEPSLATE_PLACE, 1.4f, 0.6f);
        return true;
    }

    /** Каменный кулак: удар по цели, и пол вокруг неё трескается — соседей задевает вполсилы. */
    private static boolean fist(ServerPlayer p, PlayerStats s, Ability a) {
        LivingEntity t = Kit.enemyNear(p, a.radius);
        if (t == null) return AbilityHandler.noTarget(p);
        ServerLevel sl = p.serverLevel();
        float dmg = Kit.power(s, a);
        Kit.dmg(p, a, t, dmg);
        for (LivingEntity e : Kit.enemiesIn(p, t.position(), 2.5)) if (e != t) Kit.dmg(p, a, e, dmg * 0.5f);
        Fx.ring(sl, t.position(), 0.2, 2.5, 10, STONE, 0.4f);
        Fx.shards(sl, t.position(), 2.2, 7, 0.9, 16, STONE);
        sl.levelEvent(2001, BlockPos.containing(t.position().subtract(0, 0.5, 0)), net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState()));
        Kit.sound(sl, t.position(), SoundEvents.STONE_HIT, 1.5f, 0.5f);
        return true;
    }
}
