package com.andrey.litrpg;

import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.entity.GoblinKingEntity;
import com.andrey.litrpg.entity.GoblinShamanEntity;
import com.andrey.litrpg.entity.LichEntity;
import com.andrey.litrpg.entity.SkeletonWardenEntity;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.registry.ModCreativeTabs;
import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(LitRPG.MODID)
public class LitRPG {
    public static final String MODID = "litrpg";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public LitRPG() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        LOOT_MODIFIERS.register(bus);

        bus.addListener(this::commonSetup);
        bus.addListener(this::registerCaps);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::registerSpawnPlacements);
    }

    public static final net.minecraftforge.registries.DeferredRegister<com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            net.minecraftforge.registries.DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    static {
        LOOT_MODIFIERS.register("chest_gear", () -> com.andrey.litrpg.gear.ChestLootModifier.CODEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MODID, path);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    private void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(PlayerStats.class);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GOBLIN.get(), GoblinEntity.createAttributes().build());
        event.put(ModEntities.GOBLIN_SHAMAN.get(), GoblinShamanEntity.createAttributes().build());
        event.put(ModEntities.SKELETON_WARDEN.get(), SkeletonWardenEntity.createAttributes().build());
        event.put(ModEntities.GOBLIN_KING.get(), GoblinKingEntity.createAttributes().build());
        event.put(ModEntities.LICH.get(), LichEntity.createAttributes().build());
        event.put(ModEntities.RUIN_GOLEM.get(), com.andrey.litrpg.entity.boss.RuinGolemEntity.createAttributes().build());
        event.put(ModEntities.ROTTING_ENT.get(), com.andrey.litrpg.entity.boss.RottingEntEntity.createAttributes().build());
        event.put(ModEntities.SPIDER_QUEEN.get(), com.andrey.litrpg.entity.boss.SpiderQueenEntity.createAttributes().build());
        event.put(ModEntities.SWAMP_HAG.get(), com.andrey.litrpg.entity.boss.SwampHagEntity.createAttributes().build());
        event.put(ModEntities.MINOTAUR.get(), com.andrey.litrpg.entity.boss.MinotaurEntity.createAttributes().build());
        event.put(ModEntities.FROST_GIANT.get(), com.andrey.litrpg.entity.boss.FrostGiantEntity.createAttributes().build());
        event.put(ModEntities.FLAME_LORD.get(), com.andrey.litrpg.entity.boss.FlameLordEntity.createAttributes().build());
        event.put(ModEntities.ARCHDEMON.get(), com.andrey.litrpg.entity.boss.ArchdemonEntity.createAttributes().build());
        com.andrey.litrpg.npc.NpcRegistry.attributes(event);
    }

    private void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        com.andrey.litrpg.npc.NpcRegistry.placements(event);
        event.register(ModEntities.GOBLIN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.GOBLIN_SHAMAN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
        event.register(ModEntities.SKELETON_WARDEN.get(), SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }
}
