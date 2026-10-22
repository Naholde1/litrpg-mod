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
    /** Расы игроков на сервере (для внешности). */
    public static final java.util.Map<java.util.UUID, Integer> RACES = new java.util.HashMap<>();

    public static com.andrey.litrpg.stats.Race raceOf(java.util.UUID id) {
        Integer r = RACES.get(id);
        com.andrey.litrpg.stats.Race[] v = com.andrey.litrpg.stats.Race.values();
        return r == null || r < 0 || r >= v.length ? null : v[r];
    }
    /** Товарищи по группе (обновляется раз в секунду). */
    public static java.util.List<com.andrey.litrpg.network.PartyPacket.Member> party = java.util.List.of();

    public static void update(CompoundTag data, int[] cooldowns) {
        if (data != null) STATS.load(data);
        java.util.Arrays.fill(COOLDOWN, 0);
        for (int i = 0; i + 1 < cooldowns.length; i += 2) {
            if (cooldowns[i] >= 0 && cooldowns[i] < COOLDOWN.length) COOLDOWN[cooldowns[i]] = cooldowns[i + 1];
        }
        received = true;
        // окна выбора закрываются, если выбор уже сделан (например, командой)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof OriginScreen && STATS.hasOrigin() && (STATS.getRole() != null || STATS.hasClass())) {
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
