package com.andrey.litrpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Простой предмет с подсказкой (ключ тайника и т.п.). */
public class TooltipItem extends Item {
    private final String tip;

    public TooltipItem(Properties props, String tip) {
        super(props);
        this.tip = tip;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(tip).withStyle(ChatFormatting.GRAY));
    }
}
