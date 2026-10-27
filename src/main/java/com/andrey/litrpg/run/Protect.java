/*
 * Восстановлено из собранного jar (litrpg-system-3.0.0): исходник этого файла
 * не попал в git — правило «run/» в .gitignore писалось для служебной папки
 * запуска в корне, но съедало и этот пакет. Код рабочий и переведён на
 * официальные имена Minecraft, но комментарии и javadoc в байт-коде не
 * сохраняются — их здесь нет. Правило в .gitignore исправлено.
 */
package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Protect {
    public static final TagKey<Block> BREAKABLE;

    private Protect() {
    }

    private static boolean guarded(Player p) {
        return p.level().dimension() == Run.DIM && !p.isCreative();
    }

    private static boolean breakable(BlockState st) {
        return st.is(BREAKABLE);
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        if (guarded(e.getEntity()) && !breakable(e.getState())) {
            e.setNewSpeed(0.0F);
        }

    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (guarded(e.getPlayer()) && !breakable(e.getState())) {
            e.setCanceled(true);
        }

    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        Entity var2 = e.getEntity();
        if (var2 instanceof Player p) {
            if (guarded(p)) {
                e.setCanceled(true);
            }
        }

    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onUse(PlayerInteractEvent.RightClickBlock e) {
        if (guarded(e.getEntity())) {
            ItemStack held = e.getItemStack();
            if (held.getItem() instanceof BlockItem || held.getItem() instanceof BucketItem) {
                e.setUseItem(Result.DENY);
            }

        }
    }

    @SubscribeEvent(
        priority = EventPriority.HIGHEST
    )
    public static void onUseItem(PlayerInteractEvent.RightClickItem e) {
        if (guarded(e.getEntity()) && e.getItemStack().getItem() instanceof BucketItem) {
            e.setCanceled(true);
        }

    }

    static {
        BREAKABLE = TagKey.create(Registries.BLOCK, LitRPG.id("breakable"));
    }
}
