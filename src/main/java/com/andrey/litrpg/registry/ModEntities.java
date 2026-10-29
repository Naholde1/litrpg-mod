package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.BossBase;
import com.andrey.litrpg.entity.ManaBoltEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LitRPG.MODID);

    public static final RegistryObject<EntityType<ManaBoltEntity>> MANA_BOLT = ENTITY_TYPES.register("mana_bolt",
            () -> EntityType.Builder.<ManaBoltEntity>of(ManaBoltEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(4).updateInterval(10).build("mana_bolt"));

    public static final RegistryObject<EntityType<com.andrey.litrpg.entity.BossProjectile>> BOSS_PROJECTILE = ENTITY_TYPES.register("boss_projectile",
            () -> EntityType.Builder.<com.andrey.litrpg.entity.BossProjectile>of(com.andrey.litrpg.entity.BossProjectile::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(5).build("boss_projectile"));

    /** Торговец привала: продаёт снаряжение этапа, зелья, меняет класс. */
    public static final RegistryObject<EntityType<com.andrey.litrpg.run.TraderEntity>> TRADER = ENTITY_TYPES.register("trader",
            () -> EntityType.Builder.<com.andrey.litrpg.run.TraderEntity>of(com.andrey.litrpg.run.TraderEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("trader"));

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

    /** Существа бестиария (tools/gen_bestiary.py → /litrpg_bestiary.json): по одному типу на вид. */
    public static final Map<String, RegistryObject<EntityType<com.andrey.litrpg.entity.wild.WildMob>>> WILD = new LinkedHashMap<>();

    /** Хранители этапов подземелья (id → тип); модель и текстура — boss_models/ID.json, textures/entity/boss/ID.png. */
    public static final Map<String, RegistryObject<EntityType<? extends BossBase>>> BOSSES = new LinkedHashMap<>();

    /**
     * Тотемы тотемщика: отдельный тип на каждый вид, потому что у каждого своя модель
     * и своя текстура (tools/gen_totems.py → boss_models/totem_*.json).
     */
    public static final Map<String, RegistryObject<EntityType<com.andrey.litrpg.entity.TotemEntity>>> TOTEMS = new LinkedHashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends BossBase> RegistryObject<EntityType<T>> boss(String id, EntityType.EntityFactory<T> f, float w, float h, boolean fire) {
        RegistryObject<EntityType<T>> ro = ENTITY_TYPES.register(id, () -> {
            EntityType.Builder<T> b = EntityType.Builder.of(f, MobCategory.MONSTER).sized(w, h).clientTrackingRange(12);
            if (fire) b.fireImmune();
            return b.build(id);
        });
        BOSSES.put(id, (RegistryObject) ro);
        return ro;
    }

    static {
        for (com.andrey.litrpg.entity.wild.Species s : com.andrey.litrpg.entity.wild.Species.all()) {
            WILD.put(s.id, ENTITY_TYPES.register(s.id, () -> {
                EntityType.Builder<com.andrey.litrpg.entity.wild.WildMob> b = EntityType.Builder.of(s.hostile()
                        ? com.andrey.litrpg.entity.wild.WildMob.Hostile::new : com.andrey.litrpg.entity.wild.WildMob::new, MobCategory.MONSTER);
                if (s.has(com.andrey.litrpg.entity.wild.Species.Trait.FIRE_IMMUNE)) b.fireImmune();
                return b.sized(s.w, s.h).clientTrackingRange(s.w > 1.5f ? 10 : 8).build(s.id);
            }));
        }
        com.andrey.litrpg.entity.stage.Bosses.register(ModEntities::boss);
        for (String id : new String[]{"totem_guard", "totem_fury", "totem_mending", "totem_wind", "machinist_turret"}) {
            TOTEMS.put(id, ENTITY_TYPES.register(id, () -> EntityType.Builder
                    .of(com.andrey.litrpg.entity.TotemEntity::new, MobCategory.MISC)
                    .sized(0.8f, 2.2f).clientTrackingRange(10).noSummon().build(id)));
        }
    }

    /** Регистратор хранителей (у каждого этапа — свой список в {@link com.andrey.litrpg.entity.stage.Bosses}). */
    public interface BossReg {
        <T extends BossBase> RegistryObject<EntityType<T>> boss(String id, EntityType.EntityFactory<T> f, float w, float h, boolean fire);
    }
}
