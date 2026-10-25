package com.andrey.litrpg.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Опасная зона хранителя: сначала на земле проступает контур (у игроков есть время уйти),
 * затем зона «взрывается». Хранитель может двигаться и колдовать, пока зоны ждут своего часа.
 * Формы: круг, кольцо (безопасно в центре), конус, полоса. Зона «на разделение» делит урон
 * между всеми, кто в ней стоит, а если стоящих меньше нужного — бьёт по всем на арене.
 */
public final class Telegraph {
    public enum Shape { CIRCLE, RING, CONE, LINE }

    public static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(1f, 0.15f, 0.1f), 2.4f);
    public static final DustParticleOptions ORANGE = new DustParticleOptions(new Vector3f(1f, 0.55f, 0.1f), 2.4f);
    public static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.7f, 0.2f, 1f), 2.4f);
    public static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.35f, 0.75f, 1f), 2.4f);
    public static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.45f, 1f, 0.2f), 2.4f);
    public static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1f, 0.85f, 0.2f), 2.6f);

    final Shape shape;
    Vec3 center;
    /** Внешний радиус / длина. */
    final double r1;
    /** Внутренний радиус кольца / полуширина полосы / полуугол конуса (градусы). */
    final double r2;
    Vec3 dir = new Vec3(0, 0, 1);
    int left;
    final int total;
    float power;
    ParticleOptions color = RED;
    Consumer<LivingEntity> extra;
    Consumer<Vec3> fx;
    Predicate<LivingEntity> spare;
    /** Делится между стоящими; если их меньше soak — достаётся всем на арене. */
    int soak;
    Entity follow;
    int followUntil;
    /** Разметка уже отправлена клиентам (рисуется светящейся геометрией, {@link com.andrey.litrpg.fx.Fx}). */
    boolean sent;

    Telegraph(Shape shape, Vec3 center, double r1, double r2, int delay, float power) {
        this.shape = shape;
        this.center = center;
        this.r1 = r1;
        this.r2 = r2;
        this.left = delay;
        this.total = delay;
        this.power = power;
    }

    public Telegraph dir(Vec3 d) {
        Vec3 h = new Vec3(d.x, 0, d.z);
        this.dir = h.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : h.normalize();
        return this;
    }

    public Telegraph color(ParticleOptions c) { this.color = c; return this; }
    public Telegraph then(Consumer<LivingEntity> e) { this.extra = e; return this; }
    public Telegraph fx(Consumer<Vec3> f) { this.fx = f; return this; }
    /** Кого зона не трогает (например, стоящих на возвышении). */
    public Telegraph spare(Predicate<LivingEntity> p) { this.spare = p; return this; }
    public Telegraph soak(int players) { this.soak = Math.max(1, players); return this; }
    /** Зона ползёт за целью, пока до взрыва не останется lockTicks. */
    public Telegraph follow(Entity e, int lockTicks) { this.follow = e; this.followUntil = lockTicks; return this; }

    /** Центр зоны (у ползущей — где она сейчас). */
    public Vec3 center() {
        return center;
    }

    public boolean contains(Vec3 p) {
        double dx = p.x - center.x, dz = p.z - center.z;
        if (Math.abs(p.y - center.y) > 4) return false;
        double d2 = dx * dx + dz * dz;
        return switch (shape) {
            case CIRCLE -> d2 <= r1 * r1;
            case RING -> d2 <= r1 * r1 && d2 >= r2 * r2;
            case CONE -> {
                if (d2 > r1 * r1) yield false;
                if (d2 < 1) yield true;
                double d = Math.sqrt(d2);
                yield (dx * dir.x + dz * dir.z) / d >= Math.cos(Math.toRadians(r2));
            }
            case LINE -> {
                double along = dx * dir.x + dz * dir.z;
                double side = Math.abs(dx * dir.z - dz * dir.x);
                yield along >= -0.5 && along <= r1 && side <= r2;
            }
        };
    }

    /** Цвет разметки для клиента (по цвету частиц). */
    int argb() {
        if (color == ORANGE) return 0xB0FF8C1A;
        if (color == PURPLE) return 0xB0B040FF;
        if (color == BLUE) return 0xB060C0FF;
        if (color == GREEN) return 0xB070FF40;
        if (color == GOLD) return 0xB0FFD840;
        return 0xB0FF2A1A;
    }

    /** Отправить разметку зоны клиентам: заливка растёт к моменту взрыва. */
    void announce(ServerLevel sl) {
        sent = true;
        int c = soak > 0 ? 0xB0FFD840 : argb();
        if (follow != null) {
            com.andrey.litrpg.fx.Fx.follow(sl, follow, r1, Math.max(1, left - followUntil), c);
            return;
        }
        switch (shape) {
            case CIRCLE -> com.andrey.litrpg.fx.Fx.disc(sl, center, r1, 0, left, c);
            case RING -> com.andrey.litrpg.fx.Fx.disc(sl, center, r1, r2, left, c);
            case CONE -> com.andrey.litrpg.fx.Fx.cone(sl, center, dir, r1, r2, left, c);
            case LINE -> com.andrey.litrpg.fx.Fx.line(sl, center, dir, r1, r2, left, c);
        }
    }

    /** Взрыв зоны: вспышка на месте. */
    void burst(ServerLevel sl) {
        int c = argb() | 0xFF000000;
        switch (shape) {
            case CIRCLE -> {
                com.andrey.litrpg.fx.Fx.ring(sl, center, Math.max(0.5, r1 * 0.3), r1 + 0.5, 10, c);
                com.andrey.litrpg.fx.Fx.pillar(sl, center, r1 * 0.6, 2.5, 8, c & 0x80FFFFFF);
            }
            case RING -> com.andrey.litrpg.fx.Fx.ring(sl, center, r2, r1 + 0.5, 12, c);
            case CONE -> com.andrey.litrpg.fx.Fx.slash(sl, center.add(0, 0.6, 0), dir, r1, r2 * 2, 0, 8, c);
            case LINE -> com.andrey.litrpg.fx.Fx.beam(sl, center.add(0, 0.4, 0), center.add(dir.scale(r1)).add(0, 0.4, 0), r2 * 2, 8, c);
        }
    }

    /** Контур зоны частицами: чем ближе взрыв, тем гуще. */
    void draw(ServerLevel sl) {
        double y = center.y + 0.15;
        float urgency = 1f - (float) left / Math.max(1, total);
        switch (shape) {
            case CIRCLE, RING -> {
                circle(sl, r1, y, (int) (r1 * 3) + 4);
                if (shape == Shape.RING) circle(sl, r2, y, (int) (r2 * 3) + 4);
            }
            case CONE -> {
                double a0 = Math.atan2(dir.z, dir.x), half = Math.toRadians(r2);
                int n = (int) (r1 * 6);
                for (int i = 0; i <= n; i++) {
                    double t = r1 * i / n;
                    for (double a : new double[]{a0 - half, a0 + half}) {
                        sl.sendParticles(color, center.x + Math.cos(a) * t, y, center.z + Math.sin(a) * t, 1, 0, 0, 0, 0);
                    }
                }
                int arc = (int) (r1 * half * 3) + 4;
                for (int i = 0; i <= arc; i++) {
                    double a = a0 - half + 2 * half * i / arc;
                    double rr = r1 * Math.max(0.15, urgency);
                    sl.sendParticles(color, center.x + Math.cos(a) * r1, y, center.z + Math.sin(a) * r1, 1, 0, 0, 0, 0);
                    sl.sendParticles(color, center.x + Math.cos(a) * rr, y, center.z + Math.sin(a) * rr, 1, 0, 0, 0, 0);
                }
            }
            case LINE -> {
                Vec3 side = new Vec3(-dir.z, 0, dir.x).scale(r2);
                int n = (int) (r1 * 4);
                for (int i = 0; i <= n; i++) {
                    Vec3 p = center.add(dir.scale(r1 * i / n));
                    sl.sendParticles(color, p.x + side.x, y, p.z + side.z, 1, 0, 0, 0, 0);
                    sl.sendParticles(color, p.x - side.x, y, p.z - side.z, 1, 0, 0, 0, 0);
                    if (i <= n * urgency) sl.sendParticles(color, p.x, y, p.z, 1, side.x * 0.3, 0, side.z * 0.3, 0);
                }
            }
        }
    }

    private void circle(ServerLevel sl, double r, double y, int count) {
        if (r < 0.3) return;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            sl.sendParticles(color, center.x + Math.cos(a) * r, y, center.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
        }
    }

    /** Ход зон хранителя: рисуем, двигаем за целью, взрываем. */
    static void tick(BossBase boss, List<Telegraph> list) {
        if (list.isEmpty() || !(boss.level() instanceof ServerLevel sl)) return;
        List<Telegraph> done = new ArrayList<>();
        for (Telegraph t : new ArrayList<>(list)) {
            t.left--;
            if (t.follow != null && t.follow.isAlive() && t.left > t.followUntil) {
                t.center = new Vec3(t.follow.getX(), t.follow.getY(), t.follow.getZ());
            }
            if (!t.sent) t.announce(sl);
            // зона, что ползёт за целью, в последние тики застывает — разметка на месте
            if (t.follow != null && t.left == t.followUntil) {
                t.follow = null;
                t.announce(sl);
            }
            if (t.left % 8 == 0) t.draw(sl);
            if (t.left > 0) continue;
            done.add(t);
            t.burst(sl);
            boss.detonate(t);
        }
        list.removeAll(done);
    }
}
