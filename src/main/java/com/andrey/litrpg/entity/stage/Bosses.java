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

    private static <T extends BossBase> void add(ModEntities.BossReg r, String id, EntityType.EntityFactory<T> f, float w, float h, boolean fire,
                                                 Supplier<AttributeSupplier.Builder> attrs) {
        ALL.add(new Def(r.boss(id, f, w, h, fire), attrs));
        IDS.add(id);
    }

    public static void register(ModEntities.BossReg r) {
        // хранители добавляются по этапам (stage1..stage5)
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (Def d : ALL) e.put(d.type.get(), d.attrs.get().build());
    }
}
