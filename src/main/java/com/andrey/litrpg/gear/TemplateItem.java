package com.andrey.litrpg.gear;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Предмет, созданный по шаблону. Без свойств Системы получает их сам
 * при первом тике в инвентаре (уровень шаблона, редкость не ниже минимальной).
 */
public interface TemplateItem {
    ItemTemplates.Def def();

    default void ensureGear(ItemStack stack, Level level) {
        if (level.isClientSide || GearData.has(stack)) return;
        Rarity r = GearGen.rollRarity(level.random, 0.2f, false);
        if (r.ordinal() < def().minRarity.ordinal()) r = def().minRarity;
        GearGen.apply(stack, level.random, Math.max(1, def().tier + level.random.nextInt(3) - 1), r);
    }

    default void tick(ItemStack stack, Level level, Entity entity) {
        ensureGear(stack, level);
    }
}
