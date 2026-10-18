package com.andrey.litrpg.item;

import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.GearSet;
import com.andrey.litrpg.gear.Rarity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Предмет набора брони. Без свойств Системы получает их сам (минимум «Необычный»). */
public class GearSetArmorItem extends ArmorItem {
    public final GearSet set;
    private final int baseLevel;
    private final Rarity minRarity;

    public GearSetArmorItem(LitArmorMaterial material, Type type, Properties props, GearSet set, int baseLevel, Rarity minRarity) {
        super(material, type, props);
        this.set = set;
        this.baseLevel = baseLevel;
        this.minRarity = minRarity;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || GearData.has(stack)) return;
        Rarity r = GearGen.rollRarity(level.random, 0.4f, false);
        if (r.ordinal() < minRarity.ordinal()) r = minRarity;
        GearGen.apply(stack, level.random, baseLevel, r);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        GearData d = GearData.get(stack);
        return d != null && d.rarity.ordinal() >= Rarity.EPIC.ordinal();
    }
}
