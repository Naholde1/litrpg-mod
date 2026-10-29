package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearSet;
import com.andrey.litrpg.item.DaggerItem;
import com.andrey.litrpg.item.GearSetArmorItem;
import com.andrey.litrpg.item.HealthPotionItem;
import com.andrey.litrpg.item.JewelryItem;
import com.andrey.litrpg.item.LitArmorMaterial;
import com.andrey.litrpg.item.ManaPotionItem;
import com.andrey.litrpg.item.ScrollOfOblivionItem;
import com.andrey.litrpg.item.StaffItem;
import com.andrey.litrpg.item.UniqueSwordItem;
import com.andrey.litrpg.item.UpgradeStoneItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
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

    // ---- зелья и свитки (продаёт торговец; дорогие, с общим откатом) ----
    public static final RegistryObject<Item> HEALTH_POTION = ITEMS.register("health_potion",
            () -> new HealthPotionItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> MANA_POTION = ITEMS.register("mana_potion",
            () -> new ManaPotionItem(new Item.Properties().stacksTo(8)));
    public static final RegistryObject<Item> SCROLL_OF_OBLIVION = ITEMS.register("scroll_of_oblivion",
            () -> new ScrollOfOblivionItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    // ---- лавка привала: ключи и косметика ----
    public static final RegistryObject<Item> CACHE_KEY = ITEMS.register("cache_key",
            () -> new com.andrey.litrpg.item.TooltipItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),
                    "Носите с собой: следующий тайник отдаст вещь получше и ещё одну сверху. Ключ при этом ломается."));
    public static final RegistryObject<Item> TRAIL_SCROLL = ITEMS.register("trail_scroll",
            () -> new com.andrey.litrpg.item.CosmeticItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), false));
    public static final RegistryObject<Item> TITLE_SCROLL = ITEMS.register("title_scroll",
            () -> new com.andrey.litrpg.item.CosmeticItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC), true));
    // ---- трофеи на продажу ----
    public static final RegistryObject<Item> SOUL_SHARD = ITEMS.register("soul_shard",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BONE_SHARD = ITEMS.register("bone_shard",
            () -> new Item(new Item.Properties()));

    // ---- базовое оружие ----
    public static final RegistryObject<Item> APPRENTICE_STAFF = ITEMS.register("apprentice_staff",
            () -> new StaffItem(new Item.Properties().stacksTo(1), 4, 4.0f, 0.35f, 16,
                    "item.litrpg.apprentice_staff.tooltip"));
    public static final RegistryObject<Item> ROGUE_DAGGER = ITEMS.register("rogue_dagger",
            () -> new DaggerItem(Tiers.IRON, 2, new Item.Properties()));
    public static final RegistryObject<Item> SHADOW_DAGGER = ITEMS.register("shadow_dagger",
            () -> new DaggerItem(Tiers.DIAMOND, 3, new Item.Properties().rarity(Rarity.RARE)));
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
    // ---- камни для наковальни ----
    public static final RegistryObject<Item> ASCENSION_STONE = ITEMS.register("ascension_stone",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.RARE), "item.litrpg.ascension_stone.tooltip"));
    public static final RegistryObject<Item> REFORGE_STONE = ITEMS.register("reforge_stone",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.UNCOMMON), "item.litrpg.reforge_stone.tooltip"));
    public static final RegistryObject<Item> DIVINE_SPARK = ITEMS.register("divine_spark",
            () -> new UpgradeStoneItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant(), "item.litrpg.divine_spark.tooltip"));
    // ---- наборы брони: шлем, нагрудник, поножи, ботинки ----
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] GOBLIN_SET = armorSet("goblin", LitArmorMaterial.GOBLIN, GearSet.GOBLIN, 10, com.andrey.litrpg.gear.Rarity.UNCOMMON);
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] BONEWARDEN_SET = armorSet("bonewarden", LitArmorMaterial.BONEWARDEN, GearSet.BONEWARDEN, 20, com.andrey.litrpg.gear.Rarity.RARE);
    @SuppressWarnings("unchecked")
    public static final RegistryObject<Item>[] SHADOW_SET = armorSet("shadow", LitArmorMaterial.SHADOW, GearSet.SHADOW, 32, com.andrey.litrpg.gear.Rarity.EPIC);

    // ---- монеты ----
    public static final RegistryObject<Item> COPPER_COIN = ITEMS.register("copper_coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SILVER_COIN = ITEMS.register("silver_coin", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_COIN = ITEMS.register("gold_coin", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    // ---- предметы по шаблонам (украшения, наборы, оружие, добыча хранителей) — см. litrpg_items.json ----
    public static final java.util.Map<String, RegistryObject<Item>> TEMPLATE_ITEMS = new java.util.LinkedHashMap<>();

    static {
        for (var def : com.andrey.litrpg.gear.ItemTemplates.ITEMS) {
            TEMPLATE_ITEMS.put(def.id, ITEMS.register(def.id, () -> com.andrey.litrpg.item.TemplateItems.create(def)));
        }
    }

    public static final RegistryObject<Item> MANA_ORB = ITEMS.register("mana_orb",
            () -> new Item(new Item.Properties()));

    // ---- блоки подземелья ----
    public static final RegistryObject<Item> RELIC = ITEMS.register("relic",
            () -> new BlockItem(ModBlocks.RELIC.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RUNE_DIAL = ITEMS.register("rune_dial",
            () -> new BlockItem(ModBlocks.RUNE_DIAL.get(), new Item.Properties()));
    public static final RegistryObject<Item> MIRROR = ITEMS.register("mirror",
            () -> new BlockItem(ModBlocks.MIRROR.get(), new Item.Properties()));
    public static final RegistryObject<Item> CACHE = ITEMS.register("cache",
            () -> new BlockItem(ModBlocks.CACHE.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> RIFT = ITEMS.register("rift",
            () -> new BlockItem(ModBlocks.RIFT.get(), new Item.Properties().rarity(Rarity.EPIC)));

    // ---- яйца ----
    public static final RegistryObject<Item> TRADER_SPAWN_EGG = ITEMS.register("trader_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.TRADER, 0x3A2E28, 0xFFD040, new Item.Properties()));
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
    public static final RegistryObject<Item> GRAVE_KNIGHT_SPAWN_EGG = ITEMS.register("grave_knight_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.GRAVE_KNIGHT, 0x3A3A40, 0x8E44AD, new Item.Properties()));

    static {
        // яйца призыва существ бестиария
        for (com.andrey.litrpg.entity.wild.Species s : com.andrey.litrpg.entity.wild.Species.all()) {
            ITEMS.register(s.id + "_spawn_egg", () -> new ForgeSpawnEggItem(ModEntities.WILD.get(s.id), s.egg1, s.egg2, new Item.Properties()));
        }
        // яйца хранителей
        for (var e : ModEntities.BOSSES.entrySet()) {
            ITEMS.register(e.getKey() + "_spawn_egg", () -> new ForgeSpawnEggItem(e.getValue(), 0x1A1028, 0xC02040, new Item.Properties()));
        }
    }
}
