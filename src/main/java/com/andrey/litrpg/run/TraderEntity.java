/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.gear.SellPrices;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TraderEntity extends PathfinderMob {
    private int stage = 1;

    public TraderEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.setCustomName(Component.literal("Торговец Привала").withStyle(ChatFormatting.GOLD));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, (double)40.0F).add(Attributes.MOVEMENT_SPEED, (double)0.0F);
    }

    public void setStage(int s) {
        this.stage = s;
    }

    public int stage() {
        return this.stage;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0F, 1.0F));
    }

    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        } else {
            if (player instanceof ServerPlayer) {
                ServerPlayer sp = (ServerPlayer)player;
                ItemStack held = sp.getMainHandItem();
                if (sp.isShiftKeyDown() && !held.isEmpty()) {
                    this.sell(sp, held);
                } else {
                    Shop.open(sp, this);
                }
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
    }

    private void sell(ServerPlayer p, ItemStack held) {
        int price = SellPrices.stack(held);
        if (price <= 0) {
            p.displayClientMessage(Component.literal("«Это мне ни к чему.»").withStyle(ChatFormatting.GRAY), true);
        } else {
            String name = held.getHoverName().getString();
            held.setCount(0);

            for(ItemStack c : RunLoot.coins(price)) {
                RunLoot.give(p, c);
            }

            this.level().playSound((Player)null, this.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 0.9F);
            StatsManager.message(p, Component.literal("Продано: " + name + " — " + Wallet.format(price)).withStyle(ChatFormatting.GOLD));
        }
    }

    public boolean hurt(DamageSource src, float amount) {
        return src.is(DamageTypes.GENERIC_KILL) && super.hurt(src, amount);
    }

    public boolean isPushable() {
        return false;
    }

    public boolean removeWhenFarAway(double d) {
        return false;
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.WANDERING_TRADER_AMBIENT;
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", this.stage);
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stage = Math.max(1, tag.getInt("Stage"));
    }
}
