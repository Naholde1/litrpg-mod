package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.entity.GoblinKingEntity;
import com.andrey.litrpg.entity.GoblinShamanEntity;
import com.andrey.litrpg.entity.LichEntity;
import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.entity.SkeletonWardenEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LitRPG.MODID);

    public static final RegistryObject<EntityType<GoblinEntity>> GOBLIN = ENTITY_TYPES.register("goblin",
            () -> EntityType.Builder.<GoblinEntity>of(GoblinEntity::new, MobCategory.MONSTER)
                    .sized(0.5f, 1.4f).clientTrackingRange(8).build("goblin"));

    public static final RegistryObject<EntityType<GoblinShamanEntity>> GOBLIN_SHAMAN = ENTITY_TYPES.register("goblin_shaman",
            () -> EntityType.Builder.<GoblinShamanEntity>of(GoblinShamanEntity::new, MobCategory.MONSTER)
                    .sized(0.5f, 1.4f).clientTrackingRange(8).build("goblin_shaman"));

    public static final RegistryObject<EntityType<SkeletonWardenEntity>> SKELETON_WARDEN = ENTITY_TYPES.register("skeleton_warden",
            () -> EntityType.Builder.<SkeletonWardenEntity>of(SkeletonWardenEntity::new, MobCategory.MONSTER)
                    .sized(0.75f, 2.4f).clientTrackingRange(8).build("skeleton_warden"));

    public static final RegistryObject<EntityType<GoblinKingEntity>> GOBLIN_KING = ENTITY_TYPES.register("goblin_king",
            () -> EntityType.Builder.<GoblinKingEntity>of(GoblinKingEntity::new, MobCategory.MONSTER)
                    .sized(1.1f, 3.4f).fireImmune().clientTrackingRange(10).build("goblin_king"));

    public static final RegistryObject<EntityType<LichEntity>> LICH = ENTITY_TYPES.register("lich",
            () -> EntityType.Builder.<LichEntity>of(LichEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 2.9f).fireImmune().clientTrackingRange(10).build("lich"));

    public static final RegistryObject<EntityType<ManaBoltEntity>> MANA_BOLT = ENTITY_TYPES.register("mana_bolt",
            () -> EntityType.Builder.<ManaBoltEntity>of(ManaBoltEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(4).updateInterval(10).build("mana_bolt"));

    static {
        com.andrey.litrpg.npc.NpcRegistry.init();
    }
}
