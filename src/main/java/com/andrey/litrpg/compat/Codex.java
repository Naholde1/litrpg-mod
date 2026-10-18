package com.andrey.litrpg.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

/** «Кодекс Системы» — книга Patchouli. Без Patchouli возвращает пустой стек. */
public final class Codex {
    public static final String BOOK_ID = "litrpg:codex";

    private Codex() {}

    public static ItemStack create() {
        if (!ModList.get().isLoaded("patchouli")) return ItemStack.EMPTY;
        Item book = ForgeRegistries.ITEMS.getValue(new ResourceLocation("patchouli", "guide_book"));
        if (book == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(book);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("patchouli:book", BOOK_ID);
        return stack;
    }
}
