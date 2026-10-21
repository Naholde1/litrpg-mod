package com.andrey.litrpg.npc;

import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

/** Регистрация жителей рас: типы сущностей, атрибуты, яйца призыва, правила появления. */
public final class NpcRegistry {
    public static final Map<NpcType, RegistryObject<EntityType<RaceNpcEntity>>> TYPES = new EnumMap<>(NpcType.class);
    public static final Map<NpcType, RegistryObject<Item>> EGGS = new EnumMap<>(NpcType.class);

    private NpcRegistry() {}

    /** Вызывается из статической инициализации реестров (до регистрации). */
    public static void init() {
        if (!TYPES.isEmpty()) return;
        for (NpcType t : NpcType.values()) {
            float s = t.scale();
            TYPES.put(t, ModEntities.ENTITY_TYPES.register(t.key, () -> {
                var b = EntityType.Builder.<RaceNpcEntity>of((type, level) -> new RaceNpcEntity(type, level, t), MobCategory.CREATURE)
                        .sized(0.6f * Math.max(1f, s * 0.9f), 1.95f * s).clientTrackingRange(10);
                if (t.isGuard()) b.fireImmune();
                return b.build(t.key);
            }));
            int[] colors = eggColors(t);
            EGGS.put(t, ModItems.ITEMS.register(t.key + "_spawn_egg",
                    () -> new ForgeSpawnEggItem(TYPES.get(t), colors[0], colors[1], new Item.Properties())));
        }
    }

    private static int[] eggColors(NpcType t) {
        int base = switch (t.race) {
            case ELF -> 0x9BE38C;
            case DWARF -> 0xB08A4A;
            case ORC -> 0x5A8A3A;
            case BEASTKIN -> 0xC9A36B;
            case HUMAN -> t.outlaw() ? 0x3A3030 : 0x5D8AD2;
            default -> 0xA02818;
        };
        int spot = switch (t.role) {
            case WARRIOR -> 0xC03A3A;
            case ARCHER -> 0x3A8A3A;
            case MAGE -> 0x5A6AE0;
            case TRADER -> 0xF2C33A;
            case CHIEF -> 0xFFFFFF;
            case GUARD -> 0x2A2A2A;
            case CITIZEN -> 0xB0A080;
            case ARTISAN -> 0xE08A3A;
        };
        return new int[]{base, spot};
    }

    public static void attributes(EntityAttributeCreationEvent event) {
        TYPES.forEach((t, reg) -> event.put(reg.get(), RaceNpcEntity.createAttributes(t).build()));
    }

    public static void placements(SpawnPlacementRegisterEvent event) {
        TYPES.forEach((t, reg) -> event.register(reg.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mob::checkMobSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE));
    }
}
