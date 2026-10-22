package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearSet;
import com.andrey.litrpg.item.ArchmageStaffItem;
import com.andrey.litrpg.item.BossSummonItem;
import com.andrey.litrpg.item.GearSetArmorItem;
import com.andrey.litrpg.item.JewelryItem;
import com.andrey.litrpg.item.LitArmorMaterial;
import com.andrey.litrpg.item.UniqueSwordItem;
import com.andrey.litrpg.item.UpgradeStoneItem;
import net.minecraft.world.item.ArmorItem;
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
    private static final String[] PIECES = {"helmet", "chestplate", "leggings", "boots"};
    private static final ArmorItem.Type[] TYPES = {ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS};

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RegistryObject<Item>[] armorSet(String name, LitArmorMaterial mat, GearSet set, int lvl, com.andrey.litrpg.gear.Rarity min) {
        RegistryObject[] out = new RegistryObject[4];
        for (int i = 0; i < 4; i++) {
            ArmorItem.Type type = TYPES[i];
            out[i] = ITEMS.register(name + "_" + PIECES[i],
                    () -> new GearSetArmorItem(mat, type, new Item.Properties(), set, lvl, min));
        }
        return out;
    }

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
    // ---- украшения (слоты Curios) ----
    public static final RegistryObject<Item> COPPER_RING = ITEMS.register("copper_ring", () -> new JewelryItem(new Item.Properties(), 0));
    public static final RegistryObject<Item> SILVER_RING = ITEMS.register("silver_ring", () -> new JewelryItem(new Item.Properties(), 1));
    public static final RegistryObject<Item> GOLD_RING = ITEMS.register("gold_ring", () -> new JewelryItem(new Item.Properties(), 2));
    public static final RegistryObject<Item> JADE_AMULET = ITEMS.register("jade_amulet", () -> new JewelryItem(new Item.Properties(), 0));
    public static final RegistryObject<Item> RUBY_AMULET = ITEMS.register("ruby_amulet", () -> new JewelryItem(new Item.Properties(), 1));
    public static final RegistryObject<Item> STAR_AMULET = ITEMS.register("star_amulet", () -> new JewelryItem(new Item.Properties(), 2));
    public static final RegistryObject<Item> LEATHER_BELT = ITEMS.register("leather_belt", () -> new JewelryItem(new Item.Properties(), 1));
    // ---- уникальные предметы ----
    public static final RegistryObject<Item> KINGS_RING = ITEMS.register("kings_ring",
            () -> new JewelryItem(new Item.Properties().rarity(Rarity.EPIC), 1, "kings_ring"));
    public static final RegistryObject<Item> LICH_AMULET = ITEMS.register("lich_amulet",
            () -> new JewelryItem(new Item.Properties().rarity(Rarity.EPIC), 2, "lich_amulet"));
    public static final RegistryObject<Item> HEART_OF_SYSTEM = ITEMS.register("heart_of_system",
            () -> new JewelryItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), 2, "heart_of_system"));
    public static final RegistryObject<Item> DAWN_BLADE = ITEMS.register("dawn_blade",
            () -> new UniqueSwordItem(Tiers.DIAMOND, 5, -2.4f, new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "dawn_blade"));
    public static final RegistryObject<Item> FROST_SORROW = ITEMS.register("frost_sorrow",
            () -> new UniqueSwordItem(Tiers.NETHERITE, 7, -2.6f, new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "frost_sorrow"));
    public static final RegistryObject<Item> ARCHMAGE_STAFF = ITEMS.register("archmage_staff",
            () -> new ArchmageStaffItem(new Item.Properties().stacksTo(1).durability(1200).rarity(Rarity.EPIC).fireResistant()));
    // ---- камни для наковальни ----
    public static final RegistryObject<Item> ASCENSION_STONE = ITEMS.register("ascension_stone",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.RARE), "item.litrpg.ascension_stone.tooltip"));
    public static final RegistryObject<Item> REFORGE_STONE = ITEMS.register("reforge_stone",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.UNCOMMON), "item.litrpg.reforge_stone.tooltip"));
    // ---- наборы брони: шлем, нагрудник, поножи, ботинки ----
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] GOBLIN_SET = armorSet("goblin", LitArmorMaterial.GOBLIN, GearSet.GOBLIN, 10, com.andrey.litrpg.gear.Rarity.UNCOMMON);
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] BONEWARDEN_SET = armorSet("bonewarden", LitArmorMaterial.BONEWARDEN, GearSet.BONEWARDEN, 20, com.andrey.litrpg.gear.Rarity.RARE);
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] SHADOW_SET = armorSet("shadow", LitArmorMaterial.SHADOW, GearSet.SHADOW, 32, com.andrey.litrpg.gear.Rarity.EPIC);

    // ---- жетоны рас (материал для их снаряжения) и монеты ----
    public static final RegistryObject<Item> ELF_TOKEN = ITEMS.register("elf_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DWARF_TOKEN = ITEMS.register("dwarf_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ORC_TOKEN = ITEMS.register("orc_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BEAST_TOKEN = ITEMS.register("beast_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DEMON_TOKEN = ITEMS.register("demon_token", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));
    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- предметы по шаблонам (украшения, наборы, оружие) — см. litrpg_items.json ----
    public static final java.util.Map<String, RegistryObject<Item>> TEMPLATE_ITEMS = new java.util.LinkedHashMap<>();

    static {
        for (var def : com.andrey.litrpg.gear.ItemTemplates.ITEMS) {
            TEMPLATE_ITEMS.put(def.id, ITEMS.register(def.id, () -> com.andrey.litrpg.item.TemplateItems.create(def)));
        }
    }

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
    public static final RegistryObject<Item> RUIN_GOLEM_SPAWN_EGG = ITEMS.register("ruin_golem_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RUIN_GOLEM, 0x6E7B80, 0x5FD3E8, new Item.Properties()));
    public static final RegistryObject<Item> ROTTING_ENT_SPAWN_EGG = ITEMS.register("rotting_ent_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ROTTING_ENT, 0x4A3A24, 0x8DA34A, new Item.Properties()));
    public static final RegistryObject<Item> SPIDER_QUEEN_SPAWN_EGG = ITEMS.register("spider_queen_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SPIDER_QUEEN, 0x241A2E, 0xC0204A, new Item.Properties()));
    public static final RegistryObject<Item> SWAMP_HAG_SPAWN_EGG = ITEMS.register("swamp_hag_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SWAMP_HAG, 0x2E4A34, 0xB8D040, new Item.Properties()));
    public static final RegistryObject<Item> MINOTAUR_SPAWN_EGG = ITEMS.register("minotaur_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MINOTAUR, 0x6A4428, 0xD0B080, new Item.Properties()));
    public static final RegistryObject<Item> FROST_GIANT_SPAWN_EGG = ITEMS.register("frost_giant_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FROST_GIANT, 0x9ED8F0, 0xEAF6FF, new Item.Properties()));
    public static final RegistryObject<Item> FLAME_LORD_SPAWN_EGG = ITEMS.register("flame_lord_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FLAME_LORD, 0x3A1A10, 0xFF7A20, new Item.Properties()));
    public static final RegistryObject<Item> ARCHDEMON_SPAWN_EGG = ITEMS.register("archdemon_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ARCHDEMON, 0x5A0C14, 0xFF4020, new Item.Properties()));
    // ---- подземелья ----
    public static final RegistryObject<Item> RELIC = ITEMS.register("relic",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.RELIC.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SEAL_DOOR = ITEMS.register("seal_door",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.SEAL_DOOR.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> BOSS_ALTAR = ITEMS.register("boss_altar",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.BOSS_ALTAR.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> DIVINE_SPARK = ITEMS.register("divine_spark",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "item.litrpg.divine_spark.tooltip"));
    public static final RegistryObject<Item> STONE_TROLL_SPAWN_EGG = ITEMS.register("stone_troll_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.STONE_TROLL, 0x6E7B70, 0x8DA34A, new Item.Properties()));
    public static final RegistryObject<Item> FROST_WRAITH_SPAWN_EGG = ITEMS.register("frost_wraith_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FROST_WRAITH, 0x9ED8F0, 0xEAF6FF, new Item.Properties()));
    public static final RegistryObject<Item> FOREST_WISP_SPAWN_EGG = ITEMS.register("forest_wisp_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.FOREST_WISP, 0xC8F060, 0x4FBF5A, new Item.Properties()));
    public static final RegistryObject<Item> ASH_IMP_SPAWN_EGG = ITEMS.register("ash_imp_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.ASH_IMP, 0x5A2018, 0xFF7A20, new Item.Properties()));
    public static final RegistryObject<Item> BOG_LURKER_SPAWN_EGG = ITEMS.register("bog_lurker_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.BOG_LURKER, 0x3E5A3A, 0x90F070, new Item.Properties()));
    public static final RegistryObject<Item> SAND_SCORPION_SPAWN_EGG = ITEMS.register("sand_scorpion_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SAND_SCORPION, 0xC8A060, 0x6A4428, new Item.Properties()));
    public static final RegistryObject<Item> RAT_SPAWN_EGG = ITEMS.register("rat_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RAT, 0x5A4A40, 0xD89A9A, new Item.Properties()));
    public static final RegistryObject<Item> RAT_KING_SPAWN_EGG = ITEMS.register("rat_king_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.RAT_KING, 0x3A2E28, 0xFFD040, new Item.Properties()));
    /** Посылка из городского поручения: отнести мастеру. */
    public static final RegistryObject<Item> PARCEL = ITEMS.register("parcel",
            () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRAVE_KNIGHT_SPAWN_EGG = ITEMS.register("grave_knight_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRAVE_KNIGHT, 0x3A3A40, 0x8E44AD, new Item.Properties()));

    static {
        // яйца призыва существ бестиария
        for (com.andrey.litrpg.entity.wild.Species s : com.andrey.litrpg.entity.wild.Species.all()) {
            ITEMS.register(s.id + "_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.WILD.get(s.id), s.egg1, s.egg2, new Item.Properties()));
        }
    }
}
