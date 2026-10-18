package com.andrey.litrpg.item;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Кольца, амулеты и пояса — надеваются в слоты Curios. Без свойств Системы
 * (например, из творческого режима) предмет сам получает их при первом тике.
 */
public class JewelryItem extends Item {
    private final int tier;
    private final String unique;

    public JewelryItem(Properties props, int tier) {
        this(props, tier, null);
    }

    public JewelryItem(Properties props, int tier, String unique) {
        super(props.stacksTo(1));
        this.tier = tier;
        this.unique = unique;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || GearData.has(stack)) return;
        if (unique != null) {
            ItemStack u = GearGen.unique(unique);
            stack.setTag(u.getTag());
        } else {
            int lvl = 5 + tier * 12;
            GearGen.apply(stack, level.random, lvl, GearGen.rollRarity(level.random, tier * 0.3f, false));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        GearData d = GearData.get(stack);
        return d != null && d.rarity.ordinal() >= Rarity.EPIC.ordinal();
    }
}
