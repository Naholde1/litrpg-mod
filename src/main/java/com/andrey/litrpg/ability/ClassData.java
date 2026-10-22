package com.andrey.litrpg.ability;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Данные классов и навыков из /litrpg_classes.json (пишет tools/gen_classes.py). */
public final class ClassData {
    private ClassData() {}

    private static JsonObject root;

    public static synchronized JsonObject root() {
        if (root == null) {
            try (var in = LitRPG.class.getResourceAsStream("/litrpg_classes.json")) {
                if (in == null) throw new IllegalStateException("litrpg_classes.json not found");
                root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (java.io.IOException e) {
                throw new IllegalStateException("cannot read litrpg_classes.json", e);
            }
        }
        return root;
    }
}
