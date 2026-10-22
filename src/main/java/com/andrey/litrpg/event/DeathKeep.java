package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Гибель не отнимает всё: надетая броня, вторая рука, строка быстрого доступа и
 * артефакты в слотах Curios остаются при игроке. Выпадает только рюкзак (кроме вещей
 * с проклятием утраты — те исчезают, как и раньше).
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DeathKeep {
    private DeathKeep() {}

    private static final String TAG = "litrpg_kept";
    /** Строка быстрого доступа 0–8, броня 36–39, вторая рука 40. */
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40};

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator()
                || p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        Inventory inv = p.getInventory();
        ListTag kept = new ListTag();
        for (int slot : SLOTS) {
            ItemStack s = inv.getItem(slot);
            if (s.isEmpty() || EnchantmentHelper.hasVanishingCurse(s)) continue;
            CompoundTag t = s.save(new CompoundTag());
            t.putInt("KeptSlot", slot);
            kept.add(t);
            inv.setItem(slot, ItemStack.EMPTY);
        }
        // если игрок вышел с экрана смерти — вещи сохранятся вместе с ним
        p.getPersistentData().remove(TAG);
        if (!kept.isEmpty()) p.getPersistentData().put(TAG, kept);
    }

    /** Артефакты Curios: остаются в слотах (Curios сам переносит их в нового игрока). */
    @SubscribeEvent
    public static void onCurioDrops(DropRulesEvent e) {
        if (e.getEntity() instanceof ServerPlayer) {
            e.addOverride(s -> !EnchantmentHelper.hasVanishingCurse(s), ICurio.DropRule.ALWAYS_KEEP);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer now)) return;
        CompoundTag data = e.getOriginal().getPersistentData();
        now.getPersistentData().remove(TAG); // на случай, если данные уже перенесены — не дублировать
        if (!data.contains(TAG)) return;
        ListTag kept = data.getList(TAG, Tag.TAG_COMPOUND);
        data.remove(TAG);
        Inventory inv = now.getInventory();
        for (int i = 0; i < kept.size(); i++) {
            CompoundTag t = kept.getCompound(i);
            ItemStack s = ItemStack.of(t);
            int slot = t.getInt("KeptSlot");
            if (inv.getItem(slot).isEmpty()) inv.setItem(slot, s);
            else if (!inv.add(s)) now.drop(s, false);
        }
    }
}
