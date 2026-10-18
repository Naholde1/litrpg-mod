package com.andrey.litrpg.compat.jei;

import com.andrey.litrpg.LitRPG;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * JEI: рецепты мода показываются автоматически, а здесь добавляются
 * страницы «Информация» — где добыть предмет и зачем он нужен.
 */
@JeiPlugin
public class LitJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return LitRPG.id("jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !id.getNamespace().equals(LitRPG.MODID)) continue;
            String key = "jei.litrpg." + id.getPath();
            if (I18n.exists(key)) {
                registration.addIngredientInfo(new ItemStack(item), VanillaTypes.ITEM_STACK, Component.translatable(key));
            }
        }
    }
}
