package com.andrey.litrpg.item;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Посох Лича: стреляет магическими снарядами, тратит 5 маны. */
public class LichStaffItem extends Item {
    public LichStaffItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            PlayerStats s = StatsManager.get(sp);
            if (!sp.isCreative() && s.getMana() < 5) {
                sp.displayClientMessage(Component.literal("Недостаточно маны!").withStyle(ChatFormatting.BLUE), true);
                return InteractionResultHolder.fail(stack);
            }
            Vec3 look = sp.getLookAngle();
            ManaBoltEntity bolt = new ManaBoltEntity(level, sp, look.x, look.y, look.z);
            bolt.setPos(sp.getX() + look.x * 0.8, sp.getEyeY() - 0.15 + look.y * 0.8, sp.getZ() + look.z * 0.8);
            bolt.setDamage(9.0f + s.getStat(Stat.INTELLIGENCE) * 0.5f);
            level.addFreshEntity(bolt);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 1.2f);
            if (!sp.isCreative()) {
                s.setMana(s.getMana() - 5);
                StatsManager.sync(sp);
            }
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
            sp.getCooldowns().addCooldown(this, 12);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.litrpg.lich_staff.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
