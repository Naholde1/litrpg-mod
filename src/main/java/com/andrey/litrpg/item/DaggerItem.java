package com.andrey.litrpg.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Кинжал: быстрые удары, двойной урон в спину. */
public class DaggerItem extends SwordItem {
    public DaggerItem(Tier tier, int damage, Properties props) {
        super(tier, damage, -1.6f, props);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // удар в спину: цель смотрит от атакующего
        var toAttacker = attacker.position().subtract(target.position()).normalize();
        var facing = net.minecraft.world.phys.Vec3.directionFromRotation(0, target.getYRot());
        if (toAttacker.dot(facing) < -0.3) {
            target.invulnerableTime = 0;
            target.hurt(attacker.damageSources().mobAttack(attacker), getDamage() * 0.8f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.litrpg.dagger.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
