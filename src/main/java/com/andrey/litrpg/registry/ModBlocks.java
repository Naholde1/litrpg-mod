package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.block.CacheBlock;
import com.andrey.litrpg.block.CacheBlockEntity;
import com.andrey.litrpg.block.MirrorBlock;
import com.andrey.litrpg.block.RiftBlock;
import com.andrey.litrpg.block.RuneDialBlock;
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

    /** Узел силы хранителя (ломается за пару секунд, не выпадает). */
    public static final RegistryObject<Block> RELIC = BLOCKS.register("relic",
            () -> new com.andrey.litrpg.block.RelicBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.0f, 1200.0f).noLootTable().sound(SoundType.AMETHYST).lightLevel(s -> 12)));

    /** Рунный диск загадки: ПКМ поворачивает символ. Неразрушим. */
    public static final RegistryObject<Block> RUNE_DIAL = BLOCKS.register("rune_dial",
            () -> new RuneDialBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 6)
                    .pushReaction(PushReaction.BLOCK)));

    /** Зеркало загадки света: ПКМ поворачивает. Неразрушимо. */
    public static final RegistryObject<Block> MIRROR = BLOCKS.register("mirror",
            () -> new MirrorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 4)
                    .pushReaction(PushReaction.BLOCK)));

    /** Тайник: каждый игрок забирает из него свою добычу один раз. */
    public static final RegistryObject<Block> CACHE = BLOCKS.register("cache",
            () -> new CacheBlock(BlockBehaviour.Properties.of().mapColor(MapColor.GOLD)
                    .strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 5)
                    .pushReaction(PushReaction.BLOCK)));

    /** Разлом — переход на следующий этап. */
    public static final RegistryObject<Block> RIFT = BLOCKS.register("rift",
            () -> new RiftBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA)
                    .strength(-1.0f, 3600000.0f).noLootTable().noCollission().noOcclusion().sound(SoundType.AMETHYST)
                    .lightLevel(s -> 15).pushReaction(PushReaction.BLOCK)));

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<CacheBlockEntity>> CACHE_BE = BLOCK_ENTITIES.register("cache",
            () -> BlockEntityType.Builder.of(CacheBlockEntity::new, CACHE.get()).build(null));
}
