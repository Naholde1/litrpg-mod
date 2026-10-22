package com.andrey.litrpg.gear;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * Ценность добычи, которую скупают торговцы и мастера (Shift+ПКМ): трофеи с монстров,
 * камни улучшения, свитки и ценные дары природы. Цена — в медяках за штуку
 * (9 м = 1 серебряк, 81 м = 1 золотой); дешёвая мелочь дробная — платят за всю стопку.
 * Итоговую цену меняют репутация (скидка торговца) и бонус «к цене продажи».
 */
public final class SellPrices {
    private SellPrices() {}

    private static final Map<ResourceLocation, Float> PRICE = new HashMap<>();

    private static void put(String id, float copper) {
        PRICE.put(new ResourceLocation(id.contains(":") ? id : "litrpg:" + id), copper);
    }

    static {
        // трофеи и предметы Системы
        put("goblin_ear", 2);
        put("bone_shard", 4);
        put("soul_shard", 14);
        put("mana_orb", 3);
        put("mana_potion", 8);
        put("goblin_seal", 120);
        put("lich_phylactery", 320);
        put("tome_of_experience", 45);
        put("scroll_of_oblivion", 70);
        put("reforge_stone", 50);
        put("ascension_stone", 200);
        put("system_key", 40);
        put("heart_of_system", 600);
        put("divine_spark", 900);
        for (String t : new String[]{"elf_token", "dwarf_token", "orc_token", "beast_token", "demon_token"}) put(t, 20);
        // добыча с существ
        put("minecraft:rotten_flesh", 0.2f);
        put("minecraft:bone", 0.5f);
        put("minecraft:string", 0.5f);
        put("minecraft:feather", 0.3f);
        put("minecraft:arrow", 0.15f);
        put("minecraft:leather", 1);
        put("minecraft:rabbit_hide", 0.5f);
        put("minecraft:rabbit_foot", 5);
        put("minecraft:spider_eye", 1);
        put("minecraft:gunpowder", 1);
        put("minecraft:slime_ball", 1);
        put("minecraft:magma_cream", 2);
        put("minecraft:ender_pearl", 6);
        put("minecraft:blaze_rod", 6);
        put("minecraft:blaze_powder", 2);
        put("minecraft:ghast_tear", 14);
        put("minecraft:phantom_membrane", 4);
        put("minecraft:ink_sac", 0.5f);
        put("minecraft:glow_ink_sac", 2);
        put("minecraft:prismarine_shard", 1);
        put("minecraft:prismarine_crystals", 1.5f);
        put("minecraft:nautilus_shell", 20);
        put("minecraft:scute", 10);
        put("minecraft:shulker_shell", 25);
        put("minecraft:dragon_breath", 10);
        put("minecraft:wither_skeleton_skull", 120);
        put("minecraft:skeleton_skull", 15);
        put("minecraft:zombie_head", 15);
        put("minecraft:creeper_head", 20);
        put("minecraft:totem_of_undying", 150);
        put("minecraft:nether_star", 500);
        put("minecraft:echo_shard", 25);
        // ценности
        put("minecraft:gold_nugget", 1);
        put("minecraft:gold_ingot", 9);
        put("minecraft:iron_nugget", 0.3f);
        put("minecraft:iron_ingot", 3);
        put("minecraft:raw_iron", 2);
        put("minecraft:raw_gold", 7);
        put("minecraft:copper_ingot", 0.5f);
        put("minecraft:emerald", 9);
        put("minecraft:diamond", 40);
        put("minecraft:netherite_scrap", 90);
        put("minecraft:netherite_ingot", 400);
        put("minecraft:amethyst_shard", 1);
        put("minecraft:lapis_lazuli", 1);
        put("minecraft:quartz", 1);
        put("minecraft:obsidian", 2);
        put("minecraft:golden_apple", 25);
        put("minecraft:enchanted_golden_apple", 300);
        put("minecraft:experience_bottle", 6);
        put("minecraft:name_tag", 20);
        put("minecraft:saddle", 15);
        put("minecraft:heart_of_the_sea", 150);
    }

    /** Ценность одной штуки в медяках (0 — торговцам не нужно). */
    public static float unit(ItemStack s) {
        if (s.isEmpty()) return 0;
        return PRICE.getOrDefault(ForgeRegistries.ITEMS.getKey(s.getItem()), 0f);
    }

    /** Ценность всей стопки в медяках без учёта репутации. */
    public static int stack(ItemStack s) {
        return Math.round(unit(s) * s.getCount());
    }
}
