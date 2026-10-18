package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.nbt.CompoundTag;

/** Копия характеристик игрока на клиенте (для интерфейса). */
public class ClientData {
    public static final PlayerStats STATS = new PlayerStats();
    /** Оставшиеся тики перезарядки. */
    public static final int[] COOLDOWN = new int[Ability.count()];
    public static boolean received = false;

    public static void update(CompoundTag data, int[] cooldowns) {
        if (data != null) STATS.load(data);
        for (int i = 0; i < COOLDOWN.length && i < cooldowns.length; i++) {
            COOLDOWN[i] = cooldowns[i];
        }
        received = true;
        // окна выбора закрываются, если выбор уже сделан (например, командой)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof OriginScreen && STATS.hasOrigin()) {
            mc.setScreen(null);
        } else if (mc.screen instanceof ClassChoiceScreen && STATS.hasClass()) {
            mc.setScreen(null);
        }
    }

    public static void tick() {
        for (int i = 0; i < COOLDOWN.length; i++) {
            if (COOLDOWN[i] > 0) COOLDOWN[i]--;
        }
    }
}
