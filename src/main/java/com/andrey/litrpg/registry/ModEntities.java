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

    // ---- хранители больших подземелий (2.16) ----
    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.BronzebeardEntity>> BRONZEBEARD = ENTITY_TYPES.register("bronzebeard",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.BronzebeardEntity>of(com.andrey.litrpg.entity.raid.BronzebeardEntity::new, MobCategory.MONSTER)
                    .sized(1.8f, 4.0f).clientTrackingRange(10).build("bronzebeard"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.GhazEntity>> GHAZ = ENTITY_TYPES.register("ghaz",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.GhazEntity>of(com.andrey.litrpg.entity.raid.GhazEntity::new, MobCategory.MONSTER)
                    .sized(2.4f, 5.0f).clientTrackingRange(10).build("ghaz"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.TorbaldEntity>> TORBALD = ENTITY_TYPES.register("torbald",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.TorbaldEntity>of(com.andrey.litrpg.entity.raid.TorbaldEntity::new, MobCategory.MONSTER)
                    .sized(1.4f, 3.2f).clientTrackingRange(10).build("torbald"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.TwinEntity>> SOL_TWIN = ENTITY_TYPES.register("sol_twin",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.TwinEntity>of(com.andrey.litrpg.entity.raid.TwinEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 3.0f).fireImmune().clientTrackingRange(10).build("sol_twin"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.TwinEntity>> LUNA_TWIN = ENTITY_TYPES.register("luna_twin",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.TwinEntity>of(com.andrey.litrpg.entity.raid.TwinEntity::new, MobCategory.MONSTER)
                    .sized(0.9f, 3.0f).clientTrackingRange(10).build("luna_twin"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.ChronophageEntity>> CHRONOPHAGE = ENTITY_TYPES.register("chronophage",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.ChronophageEntity>of(com.andrey.litrpg.entity.raid.ChronophageEntity::new, MobCategory.MONSTER)
                    .sized(1.2f, 3.8f).clientTrackingRange(10).build("chronophage"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.raid.XalatharEntity>> XALATHAR = ENTITY_TYPES.register("xalathar",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.raid.XalatharEntity>of(com.andrey.litrpg.entity.raid.XalatharEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 4.2f).clientTrackingRange(10).build("xalathar"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.BossProjectile>> BOSS_PROJECTILE = ENTITY_TYPES.register("boss_projectile",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.BossProjectile>of(com.andrey.litrpg.entity.BossProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(5).build("boss_projectile"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.StoneTroll>> STONE_TROLL = ENTITY_TYPES.register("stone_troll",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.StoneTroll>of(com.andrey.litrpg.entity.Creatures.StoneTroll::new, MobCategory.MONSTER)
                    .sized(1.3f, 2.6f).clientTrackingRange(8).build("stone_troll"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.FrostWraith>> FROST_WRAITH = ENTITY_TYPES.register("frost_wraith",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.FrostWraith>of(com.andrey.litrpg.entity.Creatures.FrostWraith::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.0f).clientTrackingRange(8).build("frost_wraith"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.ForestWisp>> FOREST_WISP = ENTITY_TYPES.register("forest_wisp",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.ForestWisp>of(com.andrey.litrpg.entity.Creatures.ForestWisp::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.7f).clientTrackingRange(8).build("forest_wisp"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.AshImp>> ASH_IMP = ENTITY_TYPES.register("ash_imp",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.AshImp>of(com.andrey.litrpg.entity.Creatures.AshImp::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.2f).fireImmune().clientTrackingRange(8).build("ash_imp"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.BogLurker>> BOG_LURKER = ENTITY_TYPES.register("bog_lurker",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.BogLurker>of(com.andrey.litrpg.entity.Creatures.BogLurker::new, MobCategory.MONSTER)
                    .sized(1.2f, 0.9f).clientTrackingRange(8).build("bog_lurker"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.SandScorpion>> SAND_SCORPION = ENTITY_TYPES.register("sand_scorpion",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.SandScorpion>of(com.andrey.litrpg.entity.Creatures.SandScorpion::new, MobCategory.MONSTER)
                    .sized(1.5f, 0.9f).clientTrackingRange(8).build("sand_scorpion"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.Creatures.GraveKnight>> GRAVE_KNIGHT = ENTITY_TYPES.register("grave_knight",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.Creatures.GraveKnight>of(com.andrey.litrpg.entity.Creatures.GraveKnight::new, MobCategory.MONSTER)
                    .sized(0.7f, 2.1f).clientTrackingRange(8).build("grave_knight"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.RatEntity>> RAT = ENTITY_TYPES.register("rat",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.RatEntity>of((t, l) -> new com.andrey.litrpg.entity.RatEntity(t, l, false), MobCategory.CREATURE)
                    .sized(0.5f, 0.4f).clientTrackingRange(8).build("rat"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.RatEntity>> RAT_KING = ENTITY_TYPES.register("rat_king",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.RatEntity>of((t, l) -> new com.andrey.litrpg.entity.RatEntity(t, l, true), MobCategory.CREATURE)
                    .sized(1.0f, 0.8f).clientTrackingRange(8).build("rat_king"));

    /** Существа бестиария (tools/gen_bestiary.py → /litrpg_bestiary.json): по одному типу на вид. */
    public static final java.util.Map<String, RegistryObject<EntityType<com.andrey.litrpg.entity.wild.WildMob>>> WILD = new java.util.LinkedHashMap<>();

    static {
        com.andrey.litrpg.npc.NpcRegistry.init();
        for (com.andrey.litrpg.entity.wild.Species s : com.andrey.litrpg.entity.wild.Species.all()) {
            WILD.put(s.id, ENTITY_TYPES.register(s.id, () -> {
                EntityType.Builder<com.andrey.litrpg.entity.wild.WildMob> b = EntityType.Builder.of(s.hostile()
                        ? com.andrey.litrpg.entity.wild.WildMob.Hostile::new : com.andrey.litrpg.entity.wild.WildMob::new, MobCategory.MONSTER);
                if (s.has(com.andrey.litrpg.entity.wild.Species.Trait.FIRE_IMMUNE)) b.fireImmune();
                return b.sized(s.w, s.h).clientTrackingRange(s.w > 1.5f ? 10 : 8).build(s.id);
            }));
        }
    }
}
