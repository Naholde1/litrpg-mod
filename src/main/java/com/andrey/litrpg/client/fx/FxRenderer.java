package com.andrey.litrpg.client.fx;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.network.FxPacket;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Рисует визуальные эффекты ({@link Fx}): светящаяся геометрия с аддитивным смешением
 * (как молния — ярко в темноте подземелья), без записи глубины — эффекты не перекрывают друг друга.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
public final class FxRenderer {
    private FxRenderer() {}

    private static final class Effect {
        final FxPacket p;
        int age;

        Effect(FxPacket p) {
            this.p = p;
        }
    }

    private static final List<Effect> LIVE = new ArrayList<>();
    private static final int MAX = 600;

    public static void add(FxPacket p) {
        if (LIVE.size() >= MAX) LIVE.remove(0);
        LIVE.add(new Effect(p));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null) {
            LIVE.clear();
            return;
        }
        LIVE.removeIf(x -> ++x.age > x.p.duration);
    }

    /** Аддитивный слой без записи глубины и без отсечения граней. */
    private static final class Types extends RenderType {
        private Types() {
            super("x", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
        }

        static final RenderType GLOW = create("litrpg_fx_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144,
                false, true, CompositeState.builder()
                        .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                        .setWriteMaskState(COLOR_WRITE)
                        .setTransparencyState(LIGHTNING_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false));
        /** Разметка на земле: обычное прозрачное смешение (видно и на светлом полу). */
        static final RenderType ZONE = create("litrpg_fx_zone", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 262144,
                false, true, CompositeState.builder()
                        .setShaderState(POSITION_COLOR_SHADER)
                        .setWriteMaskState(COLOR_WRITE)
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .setOutputState(TRANSLUCENT_TARGET)
                        .createCompositeState(false));
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || LIVE.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        Camera cam = e.getCamera();
        Vec3 cp = cam.getPosition();
        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(-cp.x, -cp.y, -cp.z);
        Matrix4f m = ps.last().pose();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        float pt = e.getPartialTick();
        VertexConsumer zone = buf.getBuffer(Types.ZONE);
        for (Effect fx : LIVE) drawZone(zone, m, fx, pt, mc);
        buf.endBatch(Types.ZONE);
        VertexConsumer glow = buf.getBuffer(Types.GLOW);
        for (Effect fx : LIVE) drawGlow(glow, m, fx, pt, mc, cam);
        buf.endBatch(Types.GLOW);
        ps.popPose();
    }

    // ================= разметка на земле =================

    private static void drawZone(VertexConsumer vc, Matrix4f m, Effect fx, float pt, Minecraft mc) {
        FxPacket p = fx.p;
        float life = Mth.clamp((fx.age + pt) / Math.max(1, p.duration), 0, 1);
        int c = p.color;
        float a = (c >>> 24) / 255f;
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        switch (p.shape) {
            case Fx.DISC, Fx.FOLLOW -> {
                double x, y, z;
                float rad, inner = 0;
                if (p.shape == Fx.FOLLOW) {
                    Entity ent = mc.level.getEntity(p.a);
                    if (ent == null) return;
                    Vec3 v = ent.getPosition(pt);
                    x = v.x;
                    y = v.y;
                    z = v.z;
                    rad = p.p[0];
                } else {
                    x = p.p[0];
                    y = p.p[1];
                    z = p.p[2];
                    rad = p.p[3];
                    inner = p.p.length > 4 ? p.p[4] : 0;
                }
                float yy = (float) y + 0.04f;
                // заливка (бледная) и растущее ядро — «скоро взорвётся»
                annulus(vc, m, x, yy, z, inner, rad, 48, r, g, b, 0.16f * a);
                float grow = inner + (rad - inner) * easeIn(life);
                annulus(vc, m, x, yy + 0.005f, z, inner, grow, 48, r, g, b, 0.32f * a);
                // контур
                annulus(vc, m, x, yy + 0.01f, z, Math.max(0, rad - 0.12f), rad, 48, r, g, b, 0.85f * a);
                if (inner > 0) annulus(vc, m, x, yy + 0.01f, z, inner, inner + 0.12f, 48, r, g, b, 0.85f * a);
            }
            case Fx.CONE -> {
                double x = p.p[0], z = p.p[2];
                float y = p.p[1] + 0.04f, len = p.p[5], half = (float) Math.toRadians(p.p[6]);
                float a0 = (float) Math.atan2(p.p[4], p.p[3]);
                fan(vc, m, x, y, z, 0, len, a0 - half, a0 + half, 24, r, g, b, 0.16f * a);
                fan(vc, m, x, y + 0.005f, z, 0, len * easeIn(life), a0 - half, a0 + half, 24, r, g, b, 0.32f * a);
                fan(vc, m, x, y + 0.01f, z, len - 0.15f, len, a0 - half, a0 + half, 24, r, g, b, 0.85f * a);
                edge(vc, m, x, y + 0.01f, z, a0 - half, len, r, g, b, 0.85f * a);
                edge(vc, m, x, y + 0.01f, z, a0 + half, len, r, g, b, 0.85f * a);
            }
            case Fx.LINE -> {
                double x = p.p[0], z = p.p[2];
                float y = p.p[1] + 0.04f;
                float dx = p.p[3], dz = p.p[4], len = p.p[5], hw = p.p[6];
                float l2 = Mth.sqrt(dx * dx + dz * dz);
                if (l2 < 1e-3) return;
                dx /= l2;
                dz /= l2;
                rect(vc, m, x, y, z, dx, dz, 0, len, hw, r, g, b, 0.16f * a);
                rect(vc, m, x, y + 0.005f, z, dx, dz, 0, len * easeIn(life), hw, r, g, b, 0.3f * a);
                rect(vc, m, x, y + 0.01f, z, dx, dz, 0, len, hw, r, g, b, 0f); // пусто: контур ниже
                outline(vc, m, x, y + 0.01f, z, dx, dz, len, hw, r, g, b, 0.85f * a);
            }
            default -> { }
        }
    }

    private static float easeIn(float t) {
        return t * t * (3 - 2 * t);
    }

    // ================= светящееся =================

    private static void drawGlow(VertexConsumer vc, Matrix4f m, Effect fx, float pt, Minecraft mc, Camera cam) {
        FxPacket p = fx.p;
        float life = Mth.clamp((fx.age + pt) / Math.max(1, p.duration), 0, 1);
        int c = p.color;
        float a = (c >>> 24) / 255f;
        float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
        float fade = 1 - life;
        switch (p.shape) {
            case Fx.RING -> {
                double x = p.p[0], z = p.p[2];
                float y = p.p[1] + 0.06f;
                float rad = Mth.lerp(easeOut(life), p.p[3], p.p[4]);
                float thick = p.p.length > 5 ? p.p[5] : 0.35f;
                annulus(vc, m, x, y, z, Math.max(0, rad - thick), rad, 48, r, g, b, a * fade);
                wall(vc, m, x, y, z, rad, 0.5f + thick, 48, r, g, b, a * fade * 0.8f);
            }
            case Fx.DISC, Fx.CONE, Fx.LINE, Fx.FOLLOW -> {
                // вспышка в последние тики
                if (life > 0.92f && p.shape == Fx.DISC) {
                    float k = (life - 0.92f) / 0.08f;
                    wall(vc, m, p.p[0], p.p[1], p.p[2], p.p[3], 1.2f * k, 48, r, g, b, a * (1 - k));
                }
            }
            case Fx.BEAM -> {
                Vec3 s = new Vec3(p.p[0], p.p[1], p.p[2]), t = new Vec3(p.p[3], p.p[4], p.p[5]);
                float w = p.p[6] * (0.7f + 0.3f * Mth.sin((fx.age + pt) * 0.8f));
                beam(vc, m, s, t, w, cam, r, g, b, a * fade);
                beam(vc, m, s, t, w * 0.35f, cam, 1, 1, 1, a * fade * 0.8f);
            }
            case Fx.TETHER -> {
                Entity ea = mc.level.getEntity(p.a), eb = mc.level.getEntity(p.b);
                if (ea == null || eb == null) return;
                Vec3 s = ea.getPosition(pt).add(0, ea.getBbHeight() * 0.6, 0), t = eb.getPosition(pt).add(0, eb.getBbHeight() * 0.6, 0);
                float w = p.p[0] * (0.8f + 0.2f * Mth.sin((fx.age + pt) * 0.5f));
                beam(vc, m, s, t, w, cam, r, g, b, a * Math.min(1, fade * 4));
            }
            case Fx.PILLAR -> {
                a *= inside(cam, p.p[0], p.p[2], p.p[3]);
                float rise = Math.min(1, life * 5);
                wall(vc, m, p.p[0], p.p[1], p.p[2], p.p[3], p.p[4] * rise, 24, r, g, b, a * fade);
                wall(vc, m, p.p[0], p.p[1], p.p[2], p.p[3] * 0.45f, p.p[4] * rise, 16, 1, 1, 1, a * fade * 0.6f);
            }
            case Fx.SPHERE -> {
                float rad = Mth.lerp(easeOut(life), p.p[3], p.p[4]);
                a *= inside3(cam, p.p[0], p.p[1], p.p[2], rad);
                sphere(vc, m, p.p[0], p.p[1], p.p[2], rad, r, g, b, a * fade * 0.7f);
            }
            case Fx.SHIELD -> {
                Entity ent = mc.level.getEntity(p.a);
                if (ent == null) return;
                Vec3 v = ent.getPosition(pt);
                float pulse = (0.85f + 0.15f * Mth.sin((fx.age + pt) * 0.3f)) * inside3(cam, v.x, v.y + ent.getBbHeight() * 0.5, v.z, p.p[0]);
                float al = a * Math.min(1, fade * 5) * 0.35f * pulse;
                sphere(vc, m, v.x, v.y + ent.getBbHeight() * 0.5, v.z, p.p[0], r, g, b, al);
            }
            case Fx.SLASH -> {
                double x = p.p[0], z = p.p[2];
                float y = p.p[1];
                float a0 = (float) Math.atan2(p.p[4], p.p[3]);
                float rad = p.p[5], arc = (float) Math.toRadians(p.p[6]), tilt = (float) Math.toRadians(p.p.length > 7 ? p.p[7] : 0);
                float sweep = Math.min(1, life * 3.5f);
                float from = a0 - arc / 2, to = from + arc * sweep;
                crescent(vc, m, x, y, z, rad, from, to, tilt, a0, r, g, b, a * (life < 0.3f ? 1 : fade / 0.7f));
            }
            case Fx.MARK -> {
                Entity ent = mc.level.getEntity(p.a);
                if (ent == null) return;
                Vec3 v = ent.getPosition(pt);
                float sz = p.p[0];
                float spin = (fx.age + pt) * 0.08f;
                double yy = v.y + ent.getBbHeight() + 0.6 + 0.1 * Mth.sin((fx.age + pt) * 0.2f);
                int kind = p.p.length > 1 ? (int) p.p[1] : 0;
                mark(vc, m, v.x, yy, v.z, sz, spin, kind, cam, r, g, b, a * Math.min(1, fade * 4));
            }
            case Fx.SPIRAL -> {
                Vec3 v;
                if (p.a >= 0) {
                    Entity ent = mc.level.getEntity(p.a);
                    if (ent == null) return;
                    v = ent.getPosition(pt);
                } else v = new Vec3(p.p[3], p.p[4], p.p[5]);
                helix(vc, m, v.x, v.y, v.z, p.p[0], p.p[1] * Math.min(1, life * 2), p.p[2], (fx.age + pt) * 0.25f, r, g, b, a * fade);
            }
            default -> { }
        }
    }

    /** Камера внутри столба — почти прозрачно (иначе свой же эффект слепит). */
    private static float inside(Camera cam, double x, double z, float r) {
        Vec3 c = cam.getPosition();
        double d = Math.hypot(c.x - x, c.z - z);
        return d < r + 0.6 ? 0.12f : d < r + 2 ? (float) (0.12 + 0.88 * (d - r - 0.6) / 1.4) : 1f;
    }

    private static float inside3(Camera cam, double x, double y, double z, float r) {
        double d = cam.getPosition().distanceTo(new Vec3(x, y, z));
        return d < r + 0.4 ? 0.1f : d < r + 2 ? (float) (0.1 + 0.9 * (d - r - 0.4) / 1.6) : 1f;
    }

    private static float easeOut(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    // ================= примитивы =================

    private static void v(VertexConsumer vc, Matrix4f m, double x, double y, double z, float r, float g, float b, float a) {
        vc.vertex(m, (float) x, (float) y, (float) z).color(r, g, b, a).endVertex();
    }

    /** Плоское кольцо от r0 до r1 на высоте y. */
    private static void annulus(VertexConsumer vc, Matrix4f m, double x, float y, double z, float r0, float r1, int seg,
                                float r, float g, float b, float a) {
        if (a <= 0.003f || r1 <= r0) return;
        for (int i = 0; i < seg; i++) {
            double t0 = Math.PI * 2 * i / seg, t1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
            v(vc, m, x + c0 * r0, y, z + s0 * r0, r, g, b, a);
            v(vc, m, x + c0 * r1, y, z + s0 * r1, r, g, b, a);
            v(vc, m, x + c1 * r1, y, z + s1 * r1, r, g, b, a);
            v(vc, m, x + c1 * r0, y, z + s1 * r0, r, g, b, a);
        }
    }

    /** Сектор кольца (для конуса). */
    private static void fan(VertexConsumer vc, Matrix4f m, double x, float y, double z, float r0, float r1, float a0, float a1, int seg,
                            float r, float g, float b, float a) {
        if (a <= 0.003f || r1 <= r0) return;
        for (int i = 0; i < seg; i++) {
            double t0 = a0 + (a1 - a0) * i / seg, t1 = a0 + (a1 - a0) * (i + 1) / seg;
            double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
            v(vc, m, x + c0 * r0, y, z + s0 * r0, r, g, b, a);
            v(vc, m, x + c0 * r1, y, z + s0 * r1, r, g, b, a);
            v(vc, m, x + c1 * r1, y, z + s1 * r1, r, g, b, a);
            v(vc, m, x + c1 * r0, y, z + s1 * r0, r, g, b, a);
        }
    }

    private static void edge(VertexConsumer vc, Matrix4f m, double x, float y, double z, float ang, float len, float r, float g, float b, float a) {
        double dx = Math.cos(ang), dz = Math.sin(ang);
        double nx = -dz * 0.07, nz = dx * 0.07;
        v(vc, m, x + nx, y, z + nz, r, g, b, a);
        v(vc, m, x - nx, y, z - nz, r, g, b, a);
        v(vc, m, x + dx * len - nx, y, z + dz * len - nz, r, g, b, a);
        v(vc, m, x + dx * len + nx, y, z + dz * len + nz, r, g, b, a);
    }

    private static void rect(VertexConsumer vc, Matrix4f m, double x, float y, double z, float dx, float dz, float from, float to, float hw,
                             float r, float g, float b, float a) {
        if (a <= 0.003f || to <= from) return;
        double nx = -dz * hw, nz = dx * hw;
        v(vc, m, x + dx * from + nx, y, z + dz * from + nz, r, g, b, a);
        v(vc, m, x + dx * from - nx, y, z + dz * from - nz, r, g, b, a);
        v(vc, m, x + dx * to - nx, y, z + dz * to - nz, r, g, b, a);
        v(vc, m, x + dx * to + nx, y, z + dz * to + nz, r, g, b, a);
    }

    private static void outline(VertexConsumer vc, Matrix4f m, double x, float y, double z, float dx, float dz, float len, float hw,
                                float r, float g, float b, float a) {
        float t = 0.12f;
        rect(vc, m, x + -dz * (hw - t / 2), y, z + dx * (hw - t / 2), dx, dz, 0, len, t / 2, r, g, b, a);
        rect(vc, m, x + dz * (hw - t / 2), y, z - dx * (hw - t / 2), dx, dz, 0, len, t / 2, r, g, b, a);
        rect(vc, m, x, y, z, dx, dz, len - t, len, hw, r, g, b, a);
        rect(vc, m, x, y, z, dx, dz, 0, t, hw, r, g, b, a);
    }

    /** Вертикальная стенка-цилиндр, тающая кверху. */
    private static void wall(VertexConsumer vc, Matrix4f m, double x, double y, double z, float rad, float h, int seg,
                             float r, float g, float b, float a) {
        if (a <= 0.003f || h <= 0) return;
        for (int i = 0; i < seg; i++) {
            double t0 = Math.PI * 2 * i / seg, t1 = Math.PI * 2 * (i + 1) / seg;
            double c0 = Math.cos(t0) * rad, s0 = Math.sin(t0) * rad, c1 = Math.cos(t1) * rad, s1 = Math.sin(t1) * rad;
            v(vc, m, x + c0, y, z + s0, r, g, b, a);
            v(vc, m, x + c1, y, z + s1, r, g, b, a);
            v(vc, m, x + c1, y + h, z + s1, r, g, b, 0);
            v(vc, m, x + c0, y + h, z + s0, r, g, b, 0);
        }
    }

    /** Луч между точками: две скрещенные ленты, повёрнутые к камере. */
    private static void beam(VertexConsumer vc, Matrix4f m, Vec3 s, Vec3 t, float w, Camera cam, float r, float g, float b, float a) {
        if (a <= 0.003f) return;
        Vec3 dir = t.subtract(s);
        if (dir.lengthSqr() < 1e-4) return;
        Vec3 toCam = cam.getPosition().subtract(s.add(t).scale(0.5));
        Vec3 side = dir.cross(toCam);
        if (side.lengthSqr() < 1e-6) side = dir.cross(new Vec3(0, 1, 0));
        side = side.normalize().scale(w / 2);
        Vec3 up = dir.cross(side).normalize().scale(w / 2);
        for (Vec3 o : new Vec3[]{side, up}) {
            v(vc, m, s.x + o.x, s.y + o.y, s.z + o.z, r, g, b, a);
            v(vc, m, s.x - o.x, s.y - o.y, s.z - o.z, r, g, b, a);
            v(vc, m, t.x - o.x, t.y - o.y, t.z - o.z, r, g, b, a);
            v(vc, m, t.x + o.x, t.y + o.y, t.z + o.z, r, g, b, a);
        }
    }

    private static void sphere(VertexConsumer vc, Matrix4f m, double x, double y, double z, float rad, float r, float g, float b, float a) {
        if (a <= 0.003f || rad <= 0) return;
        int lat = 10, lon = 20;
        for (int i = 0; i < lat; i++) {
            double p0 = Math.PI * i / lat - Math.PI / 2, p1 = Math.PI * (i + 1) / lat - Math.PI / 2;
            for (int j = 0; j < lon; j++) {
                double t0 = Math.PI * 2 * j / lon, t1 = Math.PI * 2 * (j + 1) / lon;
                // ярче к краям (как мыльный пузырь)
                float edgeA = a * (0.4f + 0.6f * (float) Math.abs(Math.sin(p0 * 2 + 0.3)));
                v(vc, m, x + Math.cos(p0) * Math.cos(t0) * rad, y + Math.sin(p0) * rad, z + Math.cos(p0) * Math.sin(t0) * rad, r, g, b, edgeA);
                v(vc, m, x + Math.cos(p0) * Math.cos(t1) * rad, y + Math.sin(p0) * rad, z + Math.cos(p0) * Math.sin(t1) * rad, r, g, b, edgeA);
                v(vc, m, x + Math.cos(p1) * Math.cos(t1) * rad, y + Math.sin(p1) * rad, z + Math.cos(p1) * Math.sin(t1) * rad, r, g, b, edgeA);
                v(vc, m, x + Math.cos(p1) * Math.cos(t0) * rad, y + Math.sin(p1) * rad, z + Math.cos(p1) * Math.sin(t0) * rad, r, g, b, edgeA);
            }
        }
    }

    /** Взмах: полумесяц шириной, сходящей на нет к краям дуги. */
    private static void crescent(VertexConsumer vc, Matrix4f m, double x, float y, double z, float rad, float from, float to, float tilt,
                                 float mid, float r, float g, float b, float a) {
        if (a <= 0.003f || to <= from) return;
        int seg = 20;
        double ca = Math.cos(mid), sa = Math.sin(mid);
        for (int i = 0; i < seg; i++) {
            float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
            float t0 = from + (to - from) * u0, t1 = from + (to - from) * u1;
            float w0 = 0.15f + 0.85f * Mth.sin(u0 * Mth.PI), w1 = 0.15f + 0.85f * Mth.sin(u1 * Mth.PI);
            double[] o0 = arcPoint(t0, rad, tilt, mid, ca, sa), i0 = arcPoint(t0, rad - 0.6f * w0, tilt, mid, ca, sa);
            double[] o1 = arcPoint(t1, rad, tilt, mid, ca, sa), i1 = arcPoint(t1, rad - 0.6f * w1, tilt, mid, ca, sa);
            v(vc, m, x + i0[0], y + i0[1], z + i0[2], r, g, b, a * 0.4f);
            v(vc, m, x + o0[0], y + o0[1], z + o0[2], 1, 1, 1, a);
            v(vc, m, x + o1[0], y + o1[1], z + o1[2], 1, 1, 1, a);
            v(vc, m, x + i1[0], y + i1[1], z + i1[2], r, g, b, a * 0.4f);
        }
    }

    private static double[] arcPoint(float ang, float rad, float tilt, float mid, double ca, double sa) {
        double px = Math.cos(ang) * rad, pz = Math.sin(ang) * rad;
        // наклон: поворот вокруг оси взмаха (направление mid)
        double along = px * ca + pz * sa, across = -px * sa + pz * ca;
        double ny = across * Math.sin(tilt), nAcross = across * Math.cos(tilt);
        return new double[]{along * ca - nAcross * sa, ny, along * sa + nAcross * ca};
    }

    private static void mark(VertexConsumer vc, Matrix4f m, double x, double y, double z, float sz, float spin, int kind, Camera cam,
                             float r, float g, float b, float a) {
        if (a <= 0.003f) return;
        // ромб, повёрнутый к камере, + вращающееся кольцо
        Vec3 toCam = cam.getPosition().subtract(x, y, z).normalize();
        Vec3 side = toCam.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-6) side = new Vec3(1, 0, 0);
        side = side.normalize();
        Vec3 up = side.cross(toCam).normalize();
        float s = sz * 0.5f;
        if (kind == 3) {
            // капля (лечение): ромб + маленький круг
            quad(vc, m, x, y, z, side.scale(s * 0.6), up.scale(s), r, g, b, a);
        } else {
            quad(vc, m, x, y, z, side.scale(s), up.scale(s), r, g, b, a);
            quad(vc, m, x, y, z, side.scale(s * 0.45), up.scale(s * 0.45), 1, 1, 1, a);
        }
        if (kind == 2 || kind == 1) annulus(vc, m, x, (float) (y - sz), z, sz * 0.7f, sz * 0.85f, 24, r, g, b, a * 0.8f);
        // орбиты
        for (int k = 0; k < 3; k++) {
            double ang = spin + k * Math.PI * 2 / 3;
            Vec3 o = side.scale(Math.cos(ang) * sz).add(toCam.cross(up).normalize().scale(Math.sin(ang) * sz * 0.4));
            quad(vc, m, x + o.x, y + o.y - sz * 0.2, z + o.z, side.scale(0.06), up.scale(0.06), r, g, b, a);
        }
    }

    /** Ромб с центром (x,y,z) и полуосями sideV, upV. */
    private static void quad(VertexConsumer vc, Matrix4f m, double x, double y, double z, Vec3 sideV, Vec3 upV,
                             float r, float g, float b, float a) {
        v(vc, m, x + upV.x, y + upV.y, z + upV.z, r, g, b, a);
        v(vc, m, x + sideV.x, y + sideV.y, z + sideV.z, r, g, b, a);
        v(vc, m, x - upV.x, y - upV.y, z - upV.z, r, g, b, a);
        v(vc, m, x - sideV.x, y - sideV.y, z - sideV.z, r, g, b, a);
    }

    private static void helix(VertexConsumer vc, Matrix4f m, double x, double y, double z, float rad, float h, float turns, float spin,
                              float r, float g, float b, float a) {
        if (a <= 0.003f || h <= 0) return;
        int seg = (int) (24 * Math.max(1, turns));
        for (int strand = 0; strand < 2; strand++) {
            for (int i = 0; i < seg; i++) {
                float u0 = (float) i / seg, u1 = (float) (i + 1) / seg;
                double t0 = spin + strand * Math.PI + u0 * turns * Math.PI * 2, t1 = spin + strand * Math.PI + u1 * turns * Math.PI * 2;
                double x0 = x + Math.cos(t0) * rad, z0 = z + Math.sin(t0) * rad, x1 = x + Math.cos(t1) * rad, z1 = z + Math.sin(t1) * rad;
                float al0 = a * (1 - u0), al1 = a * (1 - u1);
                v(vc, m, x0, y + h * u0, z0, r, g, b, al0);
                v(vc, m, x0, y + h * u0 + 0.12, z0, r, g, b, al0);
                v(vc, m, x1, y + h * u1 + 0.12, z1, r, g, b, al1);
                v(vc, m, x1, y + h * u1, z1, r, g, b, al1);
            }
        }
    }

    /** Для отладки: сколько эффектов на экране. */
    public static int count() {
        return LIVE.size();
    }

    @SuppressWarnings("unused")
    private static Iterator<Effect> iter() {
        return LIVE.iterator();
    }
}
