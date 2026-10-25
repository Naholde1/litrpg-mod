package com.andrey.litrpg.registry;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LitRPG.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.litrpg"))
            .icon(() -> new ItemStack(ModItems.RIFT.get()))
            .displayItems((params, output) -> {
                for (RegistryObject<Item> item : ModItems.ITEMS.getEntries()) {
                    if (item != ModItems.MANA_ORB) {
                        output.accept(item.get());
                    }
                }
            })
            .build());
}
