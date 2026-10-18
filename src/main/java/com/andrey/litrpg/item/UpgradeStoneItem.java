package com.andrey.litrpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Камни для наковальни: Возвышение (редкость +1) и Перековка (новые свойства). */
public class UpgradeStoneItem extends Item {
    private final String tooltip;

    public UpgradeStoneItem(Properties props, String tooltip) {
        super(props);
        this.tooltip = tooltip;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.translatable(tooltip).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
