package com.andrey.litrpg;

import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.registry.ModBlocks;
import com.andrey.litrpg.registry.ModCreativeTabs;
import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * LitRPG: Подземелье Системы. Вся игра — пять этапов одного большого подземелья (run/),
 * классы и навыки (stats/, ability/), хранители (entity/stage/), экипировка (gear/).
 */
@Mod(LitRPG.MODID)
public class LitRPG {
    public static final String MODID = "litrpg";
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    public LitRPG() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.andrey.litrpg.registry.ModWorldgen.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModBlocks.BLOCK_ENTITIES.register(bus);
        com.andrey.litrpg.registry.ModEffects.EFFECTS.register(bus);
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITY_TYPES.register(bus);
        ModCreativeTabs.TABS.register(bus);

        bus.addListener(this::commonSetup);
        bus.addListener(this::registerCaps);
        bus.addListener(this::registerAttributes);
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
        event.put(ModEntities.TRADER.get(), com.andrey.litrpg.run.TraderEntity.attributes().build());
        event.put(ModEntities.STONE_TROLL.get(), com.andrey.litrpg.entity.Creatures.StoneTroll.attributes().build());
        event.put(ModEntities.FROST_WRAITH.get(), com.andrey.litrpg.entity.Creatures.FrostWraith.attributes().build());
        event.put(ModEntities.FOREST_WISP.get(), com.andrey.litrpg.entity.Creatures.ForestWisp.attributes().build());
        event.put(ModEntities.ASH_IMP.get(), com.andrey.litrpg.entity.Creatures.AshImp.attributes().build());
        event.put(ModEntities.BOG_LURKER.get(), com.andrey.litrpg.entity.Creatures.BogLurker.attributes().build());
        event.put(ModEntities.SAND_SCORPION.get(), com.andrey.litrpg.entity.Creatures.SandScorpion.attributes().build());
        event.put(ModEntities.GRAVE_KNIGHT.get(), com.andrey.litrpg.entity.Creatures.GraveKnight.attributes().build());
        for (var s : com.andrey.litrpg.entity.wild.Species.all()) {
            event.put(ModEntities.WILD.get(s.id).get(), com.andrey.litrpg.entity.wild.WildMob.attributes(s).build());
        }
        com.andrey.litrpg.entity.stage.Bosses.attributes(event);
    }
}
