package com.andrey.litrpg.item;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.ItemTemplates;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.gear.TemplateItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/** Классы предметов, создаваемых по шаблонам, и их фабрика. */
public final class TemplateItems {
    private TemplateItems() {}

    private static boolean foil(ItemStack stack) {
        GearData d = GearData.get(stack);
        return d != null && d.rarity.ordinal() >= Rarity.EPIC.ordinal();
    }

    /** Создать предмет по шаблону. */
    public static Item create(ItemTemplates.Def def) {
        Item.Properties props = new Item.Properties();
        if (def.isJewel()) return new Jewel(props, def);
        if (def.isArmor()) {
            ArmorItem.Type type = switch (def.kind) {
                case "helmet" -> ArmorItem.Type.HELMET;
                case "chestplate" -> ArmorItem.Type.CHESTPLATE;
                case "leggings" -> ArmorItem.Type.LEGGINGS;
                default -> ArmorItem.Type.BOOTS;
            };
            return new Armor(material(def.material), type, props, def);
        }
        Tier tier = def.tier < 10 ? Tiers.IRON : def.tier < 25 ? Tiers.DIAMOND : Tiers.NETHERITE;
        int dmg = Math.round(def.damage);
        return switch (def.kind) {
            case "axe" -> new Axe(tier, dmg, def.speed, props, def);
            case "dagger" -> new Dagger(tier, dmg, props, def);
            case "staff" -> new Staff(props.stacksTo(1).durability(400 + def.tier * 20), def);
            default -> new Sword(tier, dmg, def.speed, props, def);
        };
    }

    // ---------- украшения ----------

    public static class Jewel extends JewelryItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Jewel(Item.Properties props, ItemTemplates.Def def) {
            super(props, Math.min(2, def.tier / 12));
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }
    }

    // ---------- броня ----------

    public static class Armor extends ArmorItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Armor(ArmorMaterial material, Type type, Item.Properties props, ItemTemplates.Def def) {
            super(material, type, props);
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }

        @Override public boolean isFoil(ItemStack stack) { return foil(stack); }
    }

    // ---------- оружие ----------

    public static class Sword extends SwordItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Sword(Tier tier, int dmg, float speed, Item.Properties props, ItemTemplates.Def def) {
            super(tier, dmg, speed, props);
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }

        @Override public boolean isFoil(ItemStack stack) { return foil(stack); }
    }

    public static class Axe extends AxeItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Axe(Tier tier, int dmg, float speed, Item.Properties props, ItemTemplates.Def def) {
            super(tier, dmg, speed, props);
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }

        @Override public boolean isFoil(ItemStack stack) { return foil(stack); }
    }

    public static class Dagger extends DaggerItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Dagger(Tier tier, int dmg, Item.Properties props, ItemTemplates.Def def) {
            super(tier, dmg, props);
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }

        @Override public boolean isFoil(ItemStack stack) { return foil(stack); }
    }

    public static class Staff extends LichStaffItem implements TemplateItem {
        private final ItemTemplates.Def def;

        public Staff(Item.Properties props, ItemTemplates.Def def) {
            super(props, 4 + def.tier / 10, 5f + def.tier * 0.4f, 0.5f, 14, "item.litrpg.template_staff.tooltip");
            this.def = def;
        }

        @Override public ItemTemplates.Def def() { return def; }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            tick(stack, level, entity);
        }

        @Override public boolean isFoil(ItemStack stack) { return foil(stack); }
    }

    // ---------- материалы брони из шаблонов ----------

    private static final Map<String, ArmorMaterial> MATERIALS = new HashMap<>();

    public static ArmorMaterial material(String id) {
        return MATERIALS.computeIfAbsent(id, k -> new DataMaterial(ItemTemplates.MATERIALS.get(k)));
    }

    private record DataMaterial(ItemTemplates.Material m) implements ArmorMaterial {
        @Override public int getDurabilityForType(ArmorItem.Type type) {
            int base = switch (type) { case BOOTS -> 13; case LEGGINGS -> 15; case CHESTPLATE -> 16; case HELMET -> 11; };
            return base * m.durability;
        }
        @Override public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) { case BOOTS -> m.defense[0]; case LEGGINGS -> m.defense[1]; case CHESTPLATE -> m.defense[2]; case HELMET -> m.defense[3]; };
        }
        @Override public int getEnchantmentValue() { return m.enchant; }
        @Override public SoundEvent getEquipSound() {
            return switch (m.sound) {
                case "iron" -> SoundEvents.ARMOR_EQUIP_IRON;
                case "chain" -> SoundEvents.ARMOR_EQUIP_CHAIN;
                case "gold" -> SoundEvents.ARMOR_EQUIP_GOLD;
                case "diamond" -> SoundEvents.ARMOR_EQUIP_DIAMOND;
                case "netherite" -> SoundEvents.ARMOR_EQUIP_NETHERITE;
                default -> SoundEvents.ARMOR_EQUIP_LEATHER;
            };
        }
        @Override public Ingredient getRepairIngredient() {
            Item it = ForgeRegistries.ITEMS.getValue(new ResourceLocation(m.repair));
            return it == null ? Ingredient.EMPTY : Ingredient.of(it);
        }
        @Override public String getName() { return LitRPG.MODID + ":" + m.id; }
        @Override public float getToughness() { return m.toughness; }
        @Override public float getKnockbackResistance() { return m.knockback; }
    }
}
