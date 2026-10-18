package com.andrey.litrpg.item;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/** Уникальный меч: свойства фиксированы и выдаются при первом тике в инвентаре. */
public class UniqueSwordItem extends SwordItem {
    private final String unique;

    public UniqueSwordItem(Tier tier, int damage, float speed, Properties props, String unique) {
        super(tier, damage, speed, props);
        this.unique = unique;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && !GearData.has(stack)) stack.setTag(GearGen.unique(unique).getTag());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
