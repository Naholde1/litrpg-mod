package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.block.BossAltarBlock;
import com.andrey.litrpg.block.BossAltarBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, LitRPG.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LitRPG.MODID);

    /** Печать подземелья: призывает хранителя, когда рядом игрок. Неразрушима. */
    public static final RegistryObject<Block> BOSS_ALTAR = BLOCKS.register("boss_altar",
            () -> new BossAltarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS)
                    .lightLevel(s -> 9).pushReaction(PushReaction.BLOCK)));

    /** Узел силы хранителя (ломается за пару секунд, не выпадает). */
    public static final RegistryObject<Block> RELIC = BLOCKS.register("relic",
            () -> new com.andrey.litrpg.block.RelicBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 1200.0f).noLootTable().sound(SoundType.AMETHYST).lightLevel(s -> 12)));

    /** Рунная преграда большого подземелья. Неразрушима. */
    public static final RegistryObject<Block> SEAL_DOOR = BLOCKS.register("seal_door",
            () -> new com.andrey.litrpg.block.SealDoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 7)
                    .pushReaction(PushReaction.BLOCK)));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<BossAltarBlockEntity>> BOSS_ALTAR_BE = BLOCK_ENTITIES.register("boss_altar",
            () -> BlockEntityType.Builder.of(BossAltarBlockEntity::new, BOSS_ALTAR.get()).build(null));
}
