package com.andrey.litrpg.client;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.network.PartyPacket;
import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.nbt.CompoundTag;

import java.util.List;
import java.util.UUID;

/** Копия характеристик игрока на клиенте (для интерфейса), рамки группы, выбранный союзник. */
public class ClientData {
    public static final PlayerStats STATS = new PlayerStats();
    /** Оставшиеся тики перезарядки. */
    public static final int[] COOLDOWN = new int[Ability.count()];
    public static boolean received = false;
    /** Участники группы (обновляется 5 раз в секунду). */
    public static List<PartyPacket.Member> party = List.of();
    /** Номер этапа (0 — Преддверие). */
    public static int stage = 0;
    /** Выбранный союзник для таргетных навыков (null — по взгляду). */
    public static UUID focus;
    /** Кто сейчас в облике: игрок → ключ облика. Нужно, чтобы рисовать зверя вместо человека. */
    public static final java.util.Map<UUID, String> FORMS = new java.util.HashMap<>();

    public static void update(CompoundTag data, int[] cooldowns) {
        if (data != null) STATS.load(data);
        java.util.Arrays.fill(COOLDOWN, 0);
        for (int i = 0; i + 1 < cooldowns.length; i += 2) {
            if (cooldowns[i] >= 0 && cooldowns[i] < COOLDOWN.length) COOLDOWN[cooldowns[i]] = cooldowns[i + 1];
        }
        received = true;
        // окна выбора закрываются, если выбор уже сделан (например, командой)
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.screen instanceof OriginScreen && STATS.getRole() != null) {
            mc.setScreen(null);
        } else if (mc.screen instanceof ClassChoiceScreen && STATS.hasClass()) {
            mc.setScreen(null);
        }
    }

    public static void setParty(PartyPacket p) {
        party = List.copyOf(p.members);
        stage = p.stage;
        if (focus != null && party.stream().noneMatch(m -> m.id().equals(focus))) focus = null;
    }

    public static void tick() {
        for (int i = 0; i < COOLDOWN.length; i++) {
            if (COOLDOWN[i] > 0) COOLDOWN[i]--;
        }
    }
}
