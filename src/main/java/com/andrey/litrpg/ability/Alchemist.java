package com.andrey.litrpg.ability;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEffects;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Алхимик: всё, что он делает, — склянка, которая летит по дуге и разбивается там, куда упала.
 * <p>
 * В общих видах брызги были цепным лечением, а кислота и дым — обычными зонами под ногами цели.
 * Здесь склянку видно в полёте, и работает она только там, где разбилась: брызги лечат всех
 * в лужице, кислота разъедает броню всё сильнее, пока в ней стоят, дым сбивает врагов с толку
 * и они теряют цель, зажигательная сначала вспыхивает, потом горит. Панацея не только лечит,
 * но и снимает всю дрянь.
 */
public final class Alchemist {
    private static final int GREEN = 0xE070E0A0;
    private static final int ACID = 0xC0A0E020;
    private static final int SMOKE = 0xA0707070;
    private static final int FIRE = 0xE0FF7020;
    /** Скорость полёта склянки: блоков за тик. */
    private static final double SPEED = 1.1;

    private Alchemist() {}

    static void register() {
        Special.put("al_splash", Alchemist::splash);
        Special.put("al_acid", Alchemist::acid);
        Special.put("al_smoke", Alchemist::smoke);
        Special.put("al_bomb", Alchemist::bomb);
        Special.put("al_panacea", Alchemist::panacea);
    }

    /**
     * Бросить склянку по дуге в точку; когда долетит — разбить. Полёт рисуется частицами
     * самой склянки, чтобы её видели и свои, и чужие.
     */
    private static void lob(ServerPlayer p, Vec3 to, ItemStack flask, Consumer<Vec3> onBreak) {
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.getEyePosition().subtract(0, 0.2, 0);
        double dist = from.distanceTo(to);
        int ticks = Math.max(4, (int) Math.ceil(dist / SPEED));
        double arc = Math.min(4, 0.6 + dist * 0.18);
        ItemParticleOption part = new ItemParticleOption(ParticleTypes.ITEM, flask);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 1f, 0.8f);
        Scheduler.repeat(1, 1, ticks, i -> {
            double t = (double) (i + 1) / ticks;
            Vec3 at = from.lerp(to, t).add(0, Math.sin(Math.PI * t) * arc, 0);
            sl.sendParticles(part, at.x, at.y, at.z, 2, 0.02, 0.02, 0.02, 0);
            if (i == ticks - 1) {
                sl.playSound(null, to.x, to.y, to.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1f, 1f);
                sl.sendParticles(part, to.x, to.y + 0.2, to.z, 14, 0.3, 0.2, 0.3, 0.12);
                onBreak.accept(to);
            }
            return true;
        });
    }

    /** Куда летит склянка: выбранный союзник, иначе точка под прицелом. */
    private static Vec3 healSpot(ServerPlayer p, Ability a) {
        var data = p.getPersistentData();
        if (data.hasUUID(com.andrey.litrpg.network.FocusPacket.FOCUS)) {
            LivingEntity t = Dynamic.healTarget(p, a.radius);
            if (t != p) return t.position();
        }
        LivingEntity look = AbilityHandler.lookTarget(p, a.radius);
        if (look != null && AbilityHandler.isAlly(p, look)) return look.position();
        Vec3 v = AbilityHandler.lookPoint(p, a.radius);
        return v.distanceTo(p.position()) < 1.5 ? p.position() : v;
    }

    private static List<LivingEntity> alliesAt(ServerPlayer p, Vec3 c, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r, 2, r),
                e -> e.isAlive() && AbilityHandler.isAlly(p, e) && e.position().distanceTo(c) <= r)) {
            if (e instanceof net.minecraft.world.entity.npc.AbstractVillager) continue;
            if (e instanceof ServerPlayer sp && com.andrey.litrpg.run.Spirits.isSpirit(sp)) continue;
            out.add(e);
        }
        return out;
    }

    private static List<LivingEntity> enemiesAt(ServerPlayer p, Vec3 c, double r) {
        return AbilityHandler.enemiesAround(p, new AABB(c, c).inflate(r, 2, r)).stream().filter(e -> e.position().distanceTo(c) <= r + 0.5).toList();
    }

    // ---------------- лечение ----------------

    /** Брызги: склянка разбивается у союзника и лечит всех в лужице — тем больше, чем меньше их там. */
    private static boolean splash(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = healSpot(p, a);
        float heal = Dynamic.healAmount(s, a, 1f);
        lob(p, to, new ItemStack(Items.SPLASH_POTION), c -> {
            List<LivingEntity> hit = alliesAt(p, c, 3.5);
            Fx.disc(p.level(), c, 3.5, 0, 14, GREEN);
            Fx.ring(p.level(), c.add(0, 0.1, 0), 0.3, 3.5, 12, GREEN);
            // на одного — полная сила, на толпу делится, но не меньше половины на каждого
            float each = hit.isEmpty() ? 0 : heal * Math.max(0.5f, 1.6f / hit.size());
            for (LivingEntity e : hit) Dynamic.doHeal(p, e, each);
        });
        return true;
    }

    /** Панацея: последняя склянка — лечит всех вокруг и снимает с них все вредные эффекты. */
    private static boolean panacea(ServerPlayer p, PlayerStats s, Ability a) {
        ServerLevel sl = p.serverLevel();
        float heal = Dynamic.healAmount(s, a, 1f);
        int cleaned = 0;
        for (LivingEntity e : alliesAt(p, p.position(), a.radius)) {
            Dynamic.doHeal(p, e, heal);
            for (MobEffectInstance m : new ArrayList<>(e.getActiveEffects())) {
                if (m.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                    e.removeEffect(m.getEffect());
                    cleaned++;
                }
            }
            e.clearFire();
            Fx.spiral(sl, e, 0.7, 2.2, 2, 20, GREEN);
        }
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.HONEY_BOTTLE)), p.getX(), p.getY() + 1.5, p.getZ(),
                30, 0.6, 0.4, 0.6, 0.15);
        Fx.ring(sl, p.position().add(0, 0.1, 0), 0.4, a.radius, 18, GREEN, 0.5f);
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.2f, 1.2f);
        if (cleaned > 0) p.displayClientMessage(Component.literal("Панацея сняла вредных эффектов: " + cleaned).withStyle(ChatFormatting.GREEN), true);
        return true;
    }

    // ---------------- склянки во врага ----------------

    /** Кислота: лужа разъедает броню — каждая секунда в ней добавляет уязвимость (до трёх ступеней). */
    private static boolean acid(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = AbilityHandler.lookPoint(p, 22);
        float per = AbilityHandler.base(s, a) * a.power * 0.3f;
        int secs = Math.max(3, a.duration / 20);
        double r = a.radius * 0.8;
        lob(p, to, new ItemStack(Items.LINGERING_POTION), c -> {
            ServerLevel sl = p.serverLevel();
            Fx.disc(sl, c, r, 0, secs * 20, 0x60A0E020);
            Fx.ring(sl, c.add(0, 0.1, 0), r - 0.3, r, secs * 20, ACID);
            Scheduler.repeat(0, 20, secs, i -> {
                if (!p.isAlive()) return false;
                sl.sendParticles(ParticleTypes.ITEM_SLIME, c.x, c.y + 0.2, c.z, 10, r / 2, 0.1, r / 2, 0.02);
                for (LivingEntity e : enemiesAt(p, c, r)) {
                    MobEffectInstance cur = e.getEffect(ModEffects.VULNERABILITY.get());
                    int lvl = cur == null ? 0 : Math.min(2, cur.getAmplifier() + 1);
                    e.addEffect(new MobEffectInstance(ModEffects.VULNERABILITY.get(), 80, lvl));
                    e.invulnerableTime = 0;
                    AbilityHandler.hurt(p, a, e, per);
                }
                return true;
            });
        });
        return true;
    }

    /** Дым: облако, в котором враги слепнут и теряют цель. Хранителя дым не обманет — только ослепит. */
    private static boolean smoke(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = AbilityHandler.lookPoint(p, 22);
        int secs = Math.max(3, a.duration / 20);
        double r = a.radius;
        lob(p, to, new ItemStack(Items.LINGERING_POTION), c -> {
            ServerLevel sl = p.serverLevel();
            Fx.disc(sl, c, r, 0, secs * 20, 0x50808080);
            Scheduler.repeat(0, 10, secs * 2, i -> {
                if (!p.isAlive()) return false;
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.5, c.z, 6, r / 2, 0.6, r / 2, 0.01);
                for (LivingEntity e : enemiesAt(p, c, r)) {
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    if (e instanceof Mob m && !(e instanceof com.andrey.litrpg.entity.BossBase)) {
                        m.setTarget(null);
                        m.getNavigation().stop();
                    }
                }
                // и тех, кто целился в стоящих в дыму, — тоже
                for (LivingEntity ally : alliesAt(p, c, r)) {
                    for (Mob m : sl.getEntitiesOfClass(Mob.class, ally.getBoundingBox().inflate(24),
                            m -> m.getTarget() == ally && !(m instanceof com.andrey.litrpg.entity.BossBase))) {
                        m.setTarget(null);
                    }
                }
                return true;
            });
            Fx.ring(sl, c.add(0, 0.1, 0), 0.3, r, 10, SMOKE);
        });
        return true;
    }

    /** Зажигательная: вспышка там, где разбилась, и горящее пятно на несколько секунд. */
    private static boolean bomb(ServerPlayer p, PlayerStats s, Ability a) {
        Vec3 to = AbilityHandler.lookPoint(p, 22);
        float burst = AbilityHandler.base(s, a) * a.power;
        int secs = Math.max(3, a.duration / 20);
        double r = a.radius;
        lob(p, to, new ItemStack(Items.SPLASH_POTION), c -> {
            ServerLevel sl = p.serverLevel();
            Fx.sphere(sl, c.add(0, 0.5, 0), 0.4, r, 10, FIRE);
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.5, c.z, 40, r / 2, 0.3, r / 2, 0.05);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.4f, 0.7f);
            for (LivingEntity e : enemiesAt(p, c, r)) {
                AbilityHandler.hurt(p, a, e, burst);
                e.setSecondsOnFire(4);
            }
            Fx.disc(sl, c, r * 0.8, 0, secs * 20, 0x70FF6010);
            Scheduler.repeat(20, 20, secs, i -> {
                if (!p.isAlive()) return false;
                sl.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, 14, r / 2.5, 0.1, r / 2.5, 0.02);
                for (LivingEntity e : enemiesAt(p, c, r * 0.8)) {
                    e.invulnerableTime = 0;
                    AbilityHandler.hurt(p, a, e, burst * 0.2f);
                }
                return true;
            });
        });
        return true;
    }
}
