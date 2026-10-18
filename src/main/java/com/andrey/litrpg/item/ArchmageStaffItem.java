package com.andrey.litrpg.item;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Посох Архимага: мощный снаряд за ману и уникальные свойства. */
public class ArchmageStaffItem extends LichStaffItem {
    public ArchmageStaffItem(Properties props) {
        super(props, 6, 16.0f, 0.8f, 10, "item.litrpg.archmage_staff.tooltip");
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && !GearData.has(stack)) stack.setTag(GearGen.unique("archmage_staff").getTag());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
