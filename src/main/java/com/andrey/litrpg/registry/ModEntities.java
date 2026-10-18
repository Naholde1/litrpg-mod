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

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.RuinGolemEntity>> RUIN_GOLEM = ENTITY_TYPES.register("ruin_golem",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.RuinGolemEntity>of(com.andrey.litrpg.entity.boss.RuinGolemEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 2.8f).clientTrackingRange(10).build("ruin_golem"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.RottingEntEntity>> ROTTING_ENT = ENTITY_TYPES.register("rotting_ent",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.RottingEntEntity>of(com.andrey.litrpg.entity.boss.RottingEntEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.6f).clientTrackingRange(10).build("rotting_ent"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.SpiderQueenEntity>> SPIDER_QUEEN = ENTITY_TYPES.register("spider_queen",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.SpiderQueenEntity>of(com.andrey.litrpg.entity.boss.SpiderQueenEntity::new, MobCategory.MONSTER)
                    .sized(2.2f, 1.6f).clientTrackingRange(10).build("spider_queen"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.SwampHagEntity>> SWAMP_HAG = ENTITY_TYPES.register("swamp_hag",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.SwampHagEntity>of(com.andrey.litrpg.entity.boss.SwampHagEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.4f).clientTrackingRange(10).build("swamp_hag"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.MinotaurEntity>> MINOTAUR = ENTITY_TYPES.register("minotaur",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.MinotaurEntity>of(com.andrey.litrpg.entity.boss.MinotaurEntity::new, MobCategory.MONSTER)
                    .sized(1.3f, 3.0f).clientTrackingRange(10).build("minotaur"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.FrostGiantEntity>> FROST_GIANT = ENTITY_TYPES.register("frost_giant",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.FrostGiantEntity>of(com.andrey.litrpg.entity.boss.FrostGiantEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 3.8f).clientTrackingRange(10).build("frost_giant"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.FlameLordEntity>> FLAME_LORD = ENTITY_TYPES.register("flame_lord",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.FlameLordEntity>of(com.andrey.litrpg.entity.boss.FlameLordEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.4f).fireImmune().clientTrackingRange(10).build("flame_lord"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.boss.ArchdemonEntity>> ARCHDEMON = ENTITY_TYPES.register("archdemon",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.boss.ArchdemonEntity>of(com.andrey.litrpg.entity.boss.ArchdemonEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 4.2f).fireImmune().clientTrackingRange(10).build("archdemon"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.BossProjectile>> BOSS_PROJECTILE = ENTITY_TYPES.register("boss_projectile",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.BossProjectile>of(com.andrey.litrpg.entity.BossProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(5).build("boss_projectile"));

    static {
        com.andrey.litrpg.npc.NpcRegistry.init();
    }
}
