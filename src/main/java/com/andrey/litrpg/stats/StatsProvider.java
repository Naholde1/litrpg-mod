package com.andrey.litrpg.stats;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StatsProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerStats> STATS = CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerStats stats = new PlayerStats();
    private LazyOptional<PlayerStats> optional = LazyOptional.of(() -> stats);

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap != STATS) return LazyOptional.empty();
        // после смерти Forge «оживляет» capability старого игрока, а LazyOptional
        // однажды инвалидированный уже не вернуть — создаём новый
        if (!optional.isPresent()) optional = LazyOptional.of(() -> stats);
        return optional.cast();
    }

    @Override
    public CompoundTag serializeNBT() {
        return stats.save();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        stats.load(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}
