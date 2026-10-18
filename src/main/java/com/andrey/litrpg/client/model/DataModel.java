package com.andrey.litrpg.client.model;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.Animated;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Модель босса, описанная данными: /assets/litrpg/boss_models/NAME.json
 * (её вместе с текстурой создаёт tools/gen_bosses.py). Части, кубы и
 * простые анимации: взгляд, шаг, покачивание, вращение, замах, поза навыка.
 */
public class DataModel<T extends net.minecraft.world.entity.Mob> extends EntityModel<T> {
    private record Anim(String type, int axis, float amp, float speed, float phase) {}

    private record Part(ModelPart part, float rx, float ry, float rz, float x, float y, float z, List<Anim> anims) {}

    private final ModelPart root;
    private final List<Part> parts = new ArrayList<>();
    public final float scale;
    public final float shadow;
    private float limbSwing, limbAmount, age, yaw, pitch, cast;
    private int pose;

    public DataModel(ModelPart root, String name) {
        this.root = root;
        JsonObject json = read(name);
        this.scale = json.has("scale") ? json.get("scale").getAsFloat() : 1f;
        this.shadow = json.has("shadow") ? json.get("shadow").getAsFloat() : 0.8f;
        Map<String, ModelPart> byName = new HashMap<>();
        Map<String, JsonObject> defs = new HashMap<>();
        for (JsonElement el : json.getAsJsonArray("parts")) {
            JsonObject p = el.getAsJsonObject();
            defs.put(p.get("name").getAsString(), p);
        }
        for (JsonElement el : json.getAsJsonArray("parts")) {
            JsonObject p = el.getAsJsonObject();
            ModelPart mp = resolve(p.get("name").getAsString(), defs, byName);
            float[] rot = floats(p, "rot");
            float[] piv = floats(p, "pivot");
            List<Anim> anims = new ArrayList<>();
            if (p.has("anim")) {
                for (JsonElement a : p.getAsJsonArray("anim")) {
                    JsonObject o = a.getAsJsonObject();
                    anims.add(new Anim(o.get("type").getAsString(), o.has("axis") ? "xyz".indexOf(o.get("axis").getAsString()) : 0,
                            o.has("amp") ? o.get("amp").getAsFloat() : 1f, o.has("speed") ? o.get("speed").getAsFloat() : 1f,
                            o.has("phase") ? o.get("phase").getAsFloat() : 0f));
                }
            }
            parts.add(new Part(mp, rot[0], rot[1], rot[2], piv[0], piv[1], piv[2], anims));
        }
    }

    private ModelPart resolve(String name, Map<String, JsonObject> defs, Map<String, ModelPart> cache) {
        ModelPart c = cache.get(name);
        if (c != null) return c;
        String parent = defs.get(name).get("parent").getAsString();
        ModelPart p = parent.equals("root") ? root.getChild(name) : resolve(parent, defs, cache).getChild(name);
        cache.put(name, p);
        return p;
    }

    private static float[] floats(JsonObject o, String key) {
        float[] out = new float[3];
        if (!o.has(key)) return out;
        JsonArray a = o.getAsJsonArray(key);
        for (int i = 0; i < 3 && i < a.size(); i++) out[i] = a.get(i).getAsFloat();
        return out;
    }

    private static JsonObject read(String name) {
        try (var in = LitRPG.class.getResourceAsStream("/assets/litrpg/boss_models/" + name + ".json")) {
            if (in == null) throw new IllegalStateException("no model " + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load boss model " + name, e);
        }
    }

    /** Геометрия для регистрации слоя модели. */
    public static LayerDefinition createLayer(String name) {
        JsonObject json = read(name);
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> made = new HashMap<>();
        made.put("root", mesh.getRoot());
        JsonArray tex = json.getAsJsonArray("tex");
        List<JsonObject> pending = new ArrayList<>();
        for (JsonElement el : json.getAsJsonArray("parts")) pending.add(el.getAsJsonObject());
        // родители описаны раньше детей, но на всякий случай — несколько проходов
        while (!pending.isEmpty()) {
            boolean progress = false;
            for (var it = pending.iterator(); it.hasNext(); ) {
                JsonObject p = it.next();
                PartDefinition parent = made.get(p.get("parent").getAsString());
                if (parent == null) continue;
                CubeListBuilder cubes = CubeListBuilder.create();
                for (JsonElement ce : p.getAsJsonArray("cubes")) {
                    JsonObject c = ce.getAsJsonObject();
                    float[] o = floats(c, "o");
                    float[] s = floats(c, "s");
                    JsonArray uv = c.getAsJsonArray("uv");
                    cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt());
                    float infl = c.has("infl") ? c.get("infl").getAsFloat() : 0f;
                    cubes.addBox(o[0], o[1], o[2], s[0], s[1], s[2], new CubeDeformation(infl));
                }
                float[] piv = floats(p, "pivot");
                float[] rot = floats(p, "rot");
                made.put(p.get("name").getAsString(), parent.addOrReplaceChild(p.get("name").getAsString(), cubes,
                        PartPose.offsetAndRotation(piv[0], piv[1], piv[2], rot[0], rot[1], rot[2])));
                it.remove();
                progress = true;
            }
            if (!progress) throw new IllegalStateException("bad parents in " + name);
        }
        return LayerDefinition.create(mesh, tex.get(0).getAsInt(), tex.get(1).getAsInt());
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbAmount, float partialTick) {
        if (entity instanceof Animated a) {
            this.cast = a.castAnim(partialTick);
            this.pose = a.castPose();
        } else {
            this.cast = 0;
            this.pose = 0;
        }
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbAmount, float age, float yaw, float pitch) {
        this.limbSwing = limbSwing;
        this.limbAmount = Math.min(1f, limbAmount);
        this.age = age;
        this.yaw = yaw * Mth.DEG_TO_RAD;
        this.pitch = pitch * Mth.DEG_TO_RAD;
        float swing = this.attackTime;
        for (Part p : parts) {
            float[] r = {p.rx, p.ry, p.rz};
            float dy = 0;
            for (Anim a : p.anims) {
                float v = switch (a.type) {
                    case "look" -> a.axis == 1 ? this.yaw * a.amp : this.pitch * a.amp;
                    case "walk" -> Mth.cos(this.limbSwing * 0.6662f * a.speed + a.phase) * 1.2f * this.limbAmount * a.amp;
                    case "walkabs" -> Math.abs(Mth.sin(this.limbSwing * 0.6662f * a.speed + a.phase)) * this.limbAmount * a.amp;
                    case "idle" -> Mth.sin(this.age * a.speed + a.phase) * a.amp;
                    case "spin" -> this.age * a.speed + a.phase;
                    case "attack" -> -Mth.sin(swing * Mth.PI) * a.amp;
                    case "cast" -> (pose == 1 ? this.cast : 0f) * a.amp;
                    case "cast2" -> (pose == 2 ? this.cast : 0f) * a.amp;
                    case "bob" -> {
                        dy += Mth.sin(this.age * a.speed + a.phase) * a.amp;
                        yield 0f;
                    }
                    default -> 0f;
                };
                if (!a.type.equals("bob")) r[a.axis] += v;
            }
            p.part.xRot = r[0];
            p.part.yRot = r[1];
            p.part.zRot = r[2];
            p.part.y = p.y + dy;
        }
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
