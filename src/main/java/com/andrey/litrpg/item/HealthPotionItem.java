package com.andrey.litrpg.item;

import com.andrey.litrpg.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Эликсир жизни: 20% здоровья сразу и ещё 15% за 3 секунды. Пить долго (1,6 с), общий откат зелий — 60 с.
 * Спасает в беде, но не заменяет лекаря.
 */
public class HealthPotionItem extends Item {
    /** Общий откат зелий (тики). */
    public static final int COOLDOWN = 20 * 60;

    public HealthPotionItem(Properties props) {
        super(props);
    }

    /** Поставить откат на все зелья. */
    public static void cooldown(Player p) {
        p.getCooldowns().addCooldown(ModItems.HEALTH_POTION.get(), COOLDOWN);
        p.getCooldowns().addCooldown(ModItems.MANA_POTION.get(), COOLDOWN);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            sp.heal(sp.getMaxHealth() * 0.2f);
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1, false, true));
            sp.displayClientMessage(Component.literal("Эликсир жизни").withStyle(ChatFormatting.RED), true);
            level.playSound(null, sp.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1.1f);
            cooldown(sp);
        }
        if (entity instanceof Player p && !p.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE));
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.getHealth() >= player.getMaxHealth()) return InteractionResultHolder.fail(player.getItemInHand(hand));
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("20% здоровья сразу, +15% за 3 с").withStyle(ChatFormatting.RED));
        tip.add(Component.literal("Общий откат зелий — 60 с").withStyle(ChatFormatting.GRAY));
    }
}
