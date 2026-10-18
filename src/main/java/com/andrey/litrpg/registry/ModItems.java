package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.item.BossSummonItem;
import com.andrey.litrpg.item.DaggerItem;
import com.andrey.litrpg.item.KingCleaverItem;
import com.andrey.litrpg.item.LichStaffItem;
import com.andrey.litrpg.item.ManaPotionItem;
import com.andrey.litrpg.item.ScrollOfOblivionItem;
import com.andrey.litrpg.item.SystemKeyItem;
import com.andrey.litrpg.item.TomeOfExperienceItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, LitRPG.MODID);

    public static final RegistryObject<Item> SYSTEM_KEY = ITEMS.register("system_key",
            () -> new SystemKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MANA_POTION = ITEMS.register("mana_potion",
            () -> new ManaPotionItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> TOME_OF_EXPERIENCE = ITEMS.register("tome_of_experience",
            () -> new TomeOfExperienceItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SCROLL_OF_OBLIVION = ITEMS.register("scroll_of_oblivion",
            () -> new ScrollOfOblivionItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SOUL_SHARD = ITEMS.register("soul_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> GOBLIN_EAR = ITEMS.register("goblin_ear",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> BONE_SHARD = ITEMS.register("bone_shard",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_SEAL = ITEMS.register("goblin_seal",
            () -> new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), BossSummonItem.Boss.GOBLIN_KING));
    public static final RegistryObject<Item> LICH_PHYLACTERY = ITEMS.register("lich_phylactery",
            () -> new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), BossSummonItem.Boss.LICH));

    public static final RegistryObject<Item> APPRENTICE_STAFF = ITEMS.register("apprentice_staff",
            () -> new LichStaffItem(new Item.Properties().stacksTo(1).durability(300), 4, 4.0f, 0.35f, 16,
                    "item.litrpg.apprentice_staff.tooltip"));
    public static final RegistryObject<Item> ROGUE_DAGGER = ITEMS.register("rogue_dagger",
            () -> new DaggerItem(Tiers.IRON, 2, new Item.Properties()));
    public static final RegistryObject<Item> SHADOW_DAGGER = ITEMS.register("shadow_dagger",
            () -> new DaggerItem(Tiers.DIAMOND, 3, new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> KING_CLEAVER = ITEMS.register("king_cleaver",
            () -> new KingCleaverItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> LICH_STAFF = ITEMS.register("lich_staff",
            () -> new LichStaffItem(new Item.Properties().stacksTo(1).durability(600).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> MANA_ORB = ITEMS.register("mana_orb",
            () -> new Item(new Item.Properties()));

    public static final RegistryObject<Item> GOBLIN_SPAWN_EGG = ITEMS.register("goblin_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOBLIN, 0x4E7A2E, 0x6B4A2B, new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_SHAMAN_SPAWN_EGG = ITEMS.register("goblin_shaman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOBLIN_SHAMAN, 0x4E7A2E, 0x6A2C8C, new Item.Properties()));
    public static final RegistryObject<Item> SKELETON_WARDEN_SPAWN_EGG = ITEMS.register("skeleton_warden_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SKELETON_WARDEN, 0x2B2B2B, 0x8A1C1C, new Item.Properties()));
    public static final RegistryObject<Item> GOBLIN_KING_SPAWN_EGG = ITEMS.register("goblin_king_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GOBLIN_KING, 0x3B6B1F, 0xE0B020, new Item.Properties()));
    public static final RegistryObject<Item> LICH_SPAWN_EGG = ITEMS.register("lich_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.LICH, 0x1A1028, 0x8F4FE0, new Item.Properties()));
}
