package com.andrey.litrpg.gear;

import com.andrey.litrpg.LitRPG;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Снаряжение не изнашивается. В подземелье нет наковален, починки и запасных щитов,
 * а урон хранителей огромный — щит игрока разлетался за один бой, и группа оставалась
 * без защиты в середине этапа. Поэтому всё, что имеет прочность, помечается ванильным
 * тегом {@code Unbreakable}: его уважают и бой, и блок щитом, и броня, и стрельба,
 * поэтому отдельных заглушек в коде урона не нужно.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Indestructible {
    private Indestructible() {}

    /** Бит «скрыть строку Неразрушимый» в подсказке предмета (ItemStack.TooltipPart.UNBREAKABLE). */
    private static final int HIDE_UNBREAKABLE = 4;

    /** Сделать предмет вечным. true — что-то изменилось. */
    public static boolean make(ItemStack stack) {
        if (stack.isEmpty() || stack.getMaxDamage() <= 0) return false;
        CompoundTag tag = stack.getOrCreateTag();
        boolean changed = false;
        if (!tag.getBoolean("Unbreakable")) {
            tag.putBoolean("Unbreakable", true);
            changed = true;
        }
        int flags = tag.getInt("HideFlags");
        if ((flags & HIDE_UNBREAKABLE) == 0) {
            tag.putInt("HideFlags", flags | HIDE_UNBREAKABLE);
            changed = true;
        }
        // уже изношенное снаряжение (из старых сохранений) восстанавливаем
        if (stack.getDamageValue() != 0) {
            stack.setDamageValue(0);
            changed = true;
        }
        return changed;
    }

    /** Весь инвентарь: 36 ячеек, 4 брони и левая рука. */
    public static void sweep(Player p) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) make(inv.getItem(i));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0) return;
        sweep(p);
    }

    /** Подобранное и скрафченное — сразу, чтобы в подсказке не мелькала прочность. */
    @SubscribeEvent
    public static void onPickup(PlayerEvent.ItemPickupEvent e) {
        make(e.getStack());
    }

    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        make(e.getCrafting());
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        sweep(e.getEntity());
    }
}
