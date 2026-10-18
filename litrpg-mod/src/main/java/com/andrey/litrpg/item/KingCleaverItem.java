package com.andrey.litrpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Тесак Короля гоблинов: замедляет и иногда оглушает. */
public class KingCleaverItem extends SwordItem {
    public KingCleaverItem(Properties props) {
        super(Tiers.DIAMOND, 6, -2.8f, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
        if (attacker.getRandom().nextFloat() < 0.15f) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), attacker);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.litrpg.king_cleaver.tooltip").withStyle(ChatFormatting.GOLD));
    }
}
