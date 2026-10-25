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

/** Посох: стреляет магическими снарядами за ману (базовое оружие заклинателей). */
public class StaffItem extends Item {
    private final int manaCost;
    private final float baseDamage;
    private final float intScale;
    private final int cooldown;
    private final String tooltipKey;

    public StaffItem(Properties props) {
        this(props, 5, 9.0f, 0.5f, 12, "item.litrpg.lich_staff.tooltip");
    }

    /** Общий класс посохов: стоимость маны, базовый урон, множитель от Интеллекта, перезарядка. */
    public StaffItem(Properties props, int manaCost, float baseDamage, float intScale, int cooldown, String tooltipKey) {
        super(props);
        this.manaCost = manaCost;
        this.baseDamage = baseDamage;
        this.intScale = intScale;
        this.cooldown = cooldown;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer sp) {
            PlayerStats s = StatsManager.get(sp);
            if (!sp.isCreative() && s.getMana() < manaCost) {
                sp.displayClientMessage(Component.literal("Недостаточно маны!").withStyle(ChatFormatting.BLUE), true);
                return InteractionResultHolder.fail(stack);
            }
            Vec3 look = sp.getLookAngle();
            ManaBoltEntity bolt = new ManaBoltEntity(level, sp, look.x, look.y, look.z);
            bolt.setPos(sp.getX() + look.x * 0.8, sp.getEyeY() - 0.15 + look.y * 0.8, sp.getZ() + look.z * 0.8);
            bolt.setDamage(baseDamage + s.getStat(Stat.INTELLIGENCE) * intScale + s.getLevel() * 0.3f);
            level.addFreshEntity(bolt);
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.7f, 1.2f);
            if (!sp.isCreative()) {
                s.setMana(s.getMana() - manaCost);
                StatsManager.sync(sp);
            }
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
            sp.getCooldowns().addCooldown(this, cooldown);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
