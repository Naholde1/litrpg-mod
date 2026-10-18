package com.andrey.litrpg.item;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** Материалы наборов брони. */
public enum LitArmorMaterial implements ArmorMaterial {
    GOBLIN("goblin", 18, new int[]{2, 5, 4, 2}, 14, SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f,
            () -> Ingredient.of(ModItems.GOBLIN_EAR.get())),
    BONEWARDEN("bonewarden", 30, new int[]{3, 7, 6, 3}, 12, SoundEvents.ARMOR_EQUIP_IRON, 1.5f, 0.05f,
            () -> Ingredient.of(ModItems.BONE_SHARD.get())),
    SHADOW("shadow", 36, new int[]{3, 8, 6, 3}, 20, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.5f, 0.05f,
            () -> Ingredient.of(ModItems.SOUL_SHARD.get()));

    private static final int[] BASE_DURABILITY = {11, 16, 15, 13};

    private final String name;
    private final int durability;
    private final int[] defense; // ботинки, поножи, нагрудник, шлем
    private final int enchant;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockback;
    private final Supplier<Ingredient> repair;

    LitArmorMaterial(String name, int durability, int[] defense, int enchant, SoundEvent sound, float toughness,
                     float knockback, Supplier<Ingredient> repair) {
        this.name = name;
        this.durability = durability;
        this.defense = defense;
        this.enchant = enchant;
        this.sound = sound;
        this.toughness = toughness;
        this.knockback = knockback;
        this.repair = repair;
    }

    @Override public int getDurabilityForType(ArmorItem.Type type) { return BASE_DURABILITY[idx(type)] * durability; }
    @Override public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> defense[0];
            case LEGGINGS -> defense[1];
            case CHESTPLATE -> defense[2];
            case HELMET -> defense[3];
        };
    }
    @Override public int getEnchantmentValue() { return enchant; }
    @Override public SoundEvent getEquipSound() { return sound; }
    @Override public Ingredient getRepairIngredient() { return repair.get(); }
    @Override public String getName() { return LitRPG.MODID + ":" + name; }
    @Override public float getToughness() { return toughness; }
    @Override public float getKnockbackResistance() { return knockback; }

    private static int idx(ArmorItem.Type type) {
        return switch (type) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }
}
