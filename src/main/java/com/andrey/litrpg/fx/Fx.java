package com.andrey.litrpg.fx;

import com.andrey.litrpg.network.FxPacket;
import com.andrey.litrpg.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Визуальные эффекты навыков и хранителей. Сервер шлёт форму всем, кто рядом (64 блока), клиент рисует
 * светящуюся геометрию (аддитивно, как молния): кольца, круги-разметки, конусы, полосы, лучи, столбы,
 * сферы, взмахи, связи между сущностями, щиты и знаки над головой.
 * <p>
 * Цвет — 0xAARRGGBB (альфа — яркость). Готовые цвета стихий — в {@link #color}.
 */
public final class Fx {
    private Fx() {}

    // формы
    public static final int RING = 0;      // волна: x y z, r0, r1, толщина
    public static final int DISC = 1;      // разметка круга: x y z, r, (внутр. r для кольца); заполняется к концу
    public static final int CONE = 2;      // разметка конуса: x y z, dx dz, длина, полуугол°
    public static final int LINE = 3;      // разметка полосы: x y z, dx dz, длина, полуширина
    public static final int BEAM = 4;      // луч: x1 y1 z1, x2 y2 z2, ширина
    public static final int PILLAR = 5;    // столб света: x y z, радиус, высота
    public static final int SPHERE = 6;    // вспышка-сфера: x y z, r0, r1
    public static final int SLASH = 7;     // взмах-полумесяц: x y z, dx dz, радиус, угол°, наклон
    public static final int TETHER = 8;    // связь между сущностями a и b: ширина
    public static final int SHIELD = 9;    // пузырь вокруг сущности a: радиус
    public static final int MARK = 10;     // знак над головой сущности a: размер, вид знака
    public static final int SPIRAL = 11;   // спираль вокруг сущности a (или точки): радиус, высота, витки
    public static final int FOLLOW = 12;   // разметка круга под сущностью a: r (ползёт за ней)

    public static final int NONE = -1;

    public static int argb(int a, int rgb) {
        return (a & 0xFF) << 24 | rgb & 0xFFFFFF;
    }

    /** Цвета стихий и ролей. */
    public static int color(String el) {
        if (el == null) return 0xC0E8E8FF;
        return switch (el) {
            case "FIRE" -> 0xD0FF7A20;
            case "FROST" -> 0xC060C8FF;
            case "HOLY" -> 0xD0FFE070;
            case "SHADOW" -> 0xD08A30E0;
            case "LIGHTNING" -> 0xD080B0FF;
            case "POISON" -> 0xC070E040;
            case "NATURE" -> 0xC050E070;
            case "EARTH" -> 0xC0B08850;
            case "ARCANE" -> 0xD0C060FF;
            case "BLOOD" -> 0xD0E02030;
            case "HEAL" -> 0xC060FF90;
            case "DANGER" -> 0xB0FF3020;
            case "WARN" -> 0xA0FF9020;
            default -> 0xC0E8E8FF;
        };
    }

    public static void send(Level level, Vec3 at, int shape, int duration, int color, int a, int b, float... p) {
        if (!(level instanceof ServerLevel sl)) return;
        FxPacket pk = new FxPacket(shape, duration, color, p, a, b);
        ModNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(at.x, at.y, at.z, 72, sl.dimension())), pk);
    }

    public static void ring(Level l, Vec3 c, double r0, double r1, int ticks, int color) {
        send(l, c, RING, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) r0, (float) r1, 0.35f);
    }

    public static void ring(Level l, Vec3 c, double r0, double r1, int ticks, int color, float thick) {
        send(l, c, RING, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) r0, (float) r1, thick);
    }

    public static void disc(Level l, Vec3 c, double r, double inner, int ticks, int color) {
        send(l, c, DISC, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) r, (float) inner);
    }

    public static void follow(Level l, Entity e, double r, int ticks, int color) {
        send(l, e.position(), FOLLOW, ticks, color, e.getId(), NONE, (float) r);
    }

    public static void cone(Level l, Vec3 c, Vec3 dir, double len, double halfDeg, int ticks, int color) {
        send(l, c, CONE, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) dir.x, (float) dir.z, (float) len, (float) halfDeg);
    }

    public static void line(Level l, Vec3 c, Vec3 dir, double len, double halfWidth, int ticks, int color) {
        send(l, c, LINE, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) dir.x, (float) dir.z, (float) len, (float) halfWidth);
    }

    public static void beam(Level l, Vec3 a, Vec3 b, double width, int ticks, int color) {
        send(l, a, BEAM, ticks, color, NONE, NONE, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, (float) width);
    }

    public static void pillar(Level l, Vec3 c, double r, double h, int ticks, int color) {
        send(l, c, PILLAR, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) r, (float) h);
    }

    public static void sphere(Level l, Vec3 c, double r0, double r1, int ticks, int color) {
        send(l, c, SPHERE, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) r0, (float) r1);
    }

    /** Взмах перед сущностью: направление dir, радиус, раствор, наклон (0 — горизонтально). */
    public static void slash(Level l, Vec3 c, Vec3 dir, double r, double arcDeg, double tilt, int ticks, int color) {
        send(l, c, SLASH, ticks, color, NONE, NONE, (float) c.x, (float) c.y, (float) c.z, (float) dir.x, (float) dir.z, (float) r, (float) arcDeg, (float) tilt);
    }

    public static void tether(Level l, Entity a, Entity b, double width, int ticks, int color) {
        send(l, a.position(), TETHER, ticks, color, a.getId(), b.getId(), (float) width);
    }

    public static void shield(Level l, Entity e, double r, int ticks, int color) {
        send(l, e.position(), SHIELD, ticks, color, e.getId(), NONE, (float) r);
    }

    /** Знак над головой: kind 0 — ромб, 1 — череп-крест, 2 — мишень, 3 — капля (лечение). */
    public static void mark(Level l, Entity e, double size, int kind, int ticks, int color) {
        send(l, e.position(), MARK, ticks, color, e.getId(), NONE, (float) size, kind);
    }

    public static void spiral(Level l, Entity e, double r, double h, double turns, int ticks, int color) {
        send(l, e.position(), SPIRAL, ticks, color, e.getId(), NONE, (float) r, (float) h, (float) turns, 0, 0, 0);
    }

    public static void spiralAt(Level l, Vec3 c, double r, double h, double turns, int ticks, int color) {
        send(l, c, SPIRAL, ticks, color, NONE, NONE, (float) r, (float) h, (float) turns, (float) c.x, (float) c.y, (float) c.z);
    }
}
