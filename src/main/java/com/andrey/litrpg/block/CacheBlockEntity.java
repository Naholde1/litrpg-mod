package com.andrey.litrpg.block;

import com.andrey.litrpg.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Тайник: кто уже забрал свою добычу (сбрасывается вместе с этапом).
 * Ценность «Tier»: 0 — обычный тайник, 1 — за загадкой, 2 — секретная комната (задаёт генератор карты).
 */
public class CacheBlockEntity extends BlockEntity {
    private final Set<UUID> opened = new HashSet<>();
    private int tier = 0;

    public CacheBlockEntity(BlockPos pos, BlockState st) {
        super(ModBlocks.CACHE_BE.get(), pos, st);
    }

    public void open(ServerPlayer p) {
        if (opened.contains(p.getUUID())) {
            p.displayClientMessage(Component.literal("Вы уже забрали отсюда свою долю.").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        if (!com.andrey.litrpg.run.RunLoot.cache(p, tier, worldPosition)) return;
        opened.add(p.getUUID());
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
        level.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.2f);
    }

    public void reset() {
        if (opened.isEmpty()) return;
        opened.clear();
        setChanged();
    }

    public int tier() {
        return tier;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag l = new ListTag();
        for (UUID u : opened) l.add(NbtUtils.createUUID(u));
        tag.put("Opened", l);
        tag.putInt("Tier", tier);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        opened.clear();
        for (Tag t : tag.getList("Opened", Tag.TAG_INT_ARRAY)) opened.add(NbtUtils.loadUUID(t));
        tier = tag.getInt("Tier");
    }
}
