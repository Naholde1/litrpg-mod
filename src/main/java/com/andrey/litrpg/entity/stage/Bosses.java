package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Хранители пяти этапов (по три на этап). Каждый — свой класс в этом пакете с собственными фазами,
 * механиками арены и визуальными эффектами. Модели — tools/gen_bosses.py.
 */
public final class Bosses {
    private Bosses() {}

    private record Def(RegistryObject<? extends EntityType<? extends BossBase>> type, Supplier<AttributeSupplier.Builder> attrs) {}

    private static final List<Def> ALL = new ArrayList<>();

    /** Модели и рендеры: id хранителя → имя модели (boss_models/NAME.json). */
    public static final List<String> IDS = new ArrayList<>();
    public static final java.util.Map<String, String> MODEL = new java.util.HashMap<>();

    private static <T extends BossBase> void add(ModEntities.BossReg r, String id, String model, EntityType.EntityFactory<T> f, float w, float h,
                                                 boolean fire, Supplier<AttributeSupplier.Builder> attrs) {
        ALL.add(new Def(r.boss(id, f, w, h, fire), attrs));
        IDS.add(id);
        MODEL.put(id, model);
    }

    /** Имя модели хранителя (у кого своей ещё нет — похожая). */
    public static String model(String id) {
        return MODEL.getOrDefault(id, id);
    }

    public static void register(ModEntities.BossReg r) {
        // ---- этап 1: Склеп Забытых ----
        add(r, "gatekeeper", "gatekeeper", GatekeeperEntity::new, 1.5f, 3.6f, false, GatekeeperEntity::createAttributes);
        add(r, "twin_morra", "twin_morra", TwinNecromancerEntity::new, 0.8f, 2.4f, false, TwinNecromancerEntity::createAttributes);
        add(r, "twin_vessa", "twin_vessa", TwinNecromancerEntity::new, 0.8f, 2.4f, false, TwinNecromancerEntity::createAttributes);
        add(r, "lich_king", "lich_king", LichKingEntity::new, 1.0f, 3.2f, true, LichKingEntity::createAttributes);
        // ---- этап 2: Паучья чаща ----
        add(r, "spider_queen", "spider_queen", SpiderQueenEntity::new, 2.2f, 1.6f, false, SpiderQueenEntity::createAttributes);
        add(r, "rot_ent", "rotting_ent", RotEntEntity::new, 1.4f, 3.6f, false, RotEntEntity::createAttributes);
        add(r, "dark_witch", "dark_witch", DarkWitchEntity::new, 0.8f, 2.6f, false, DarkWitchEntity::createAttributes);
        // ---- этап 3: Затопленный храм ----
        add(r, "tide_warden", "tide_warden", TideWardenEntity::new, 1.4f, 3.8f, false, TideWardenEntity::createAttributes);
        add(r, "leviathan", "leviathan", LeviathanEntity::new, 3.0f, 6.0f, false, LeviathanEntity::createAttributes);
        add(r, "deep_priestess", "deep_priestess", DeepPriestessEntity::new, 0.9f, 3.0f, false, DeepPriestessEntity::createAttributes);
        // ---- этап 4: Кузни Пепла ----
        add(r, "smelter_golem", "smelter_golem", SmelterGolemEntity::new, 2.2f, 4.4f, true, SmelterGolemEntity::createAttributes);
        add(r, "fire_twin_ignis", "fire_twin_ignis", FireTwinEntity::new, 1.0f, 3.2f, true, FireTwinEntity::createAttributes);
        add(r, "fire_twin_cinder", "fire_twin_cinder", FireTwinEntity::new, 0.8f, 2.6f, true, FireTwinEntity::createAttributes);
        add(r, "ash_lord", "ash_lord", AshLordEntity::new, 1.4f, 3.8f, true, AshLordEntity::createAttributes);
        // ---- этап 5: Шпиль Бездны ----
        add(r, "chronophage", "chronophage", ChronophageEntity::new, 1.6f, 3.6f, false, ChronophageEntity::createAttributes);
        add(r, "world_eater", "world_eater", WorldEaterEntity::new, 2.8f, 3.0f, true, WorldEaterEntity::createAttributes);
        add(r, "xalathar", "xalathar", XalatharEntity::new, 1.4f, 4.0f, true, XalatharEntity::createAttributes);
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (Def d : ALL) e.put(d.type.get(), d.attrs.get().build());
    }
}
