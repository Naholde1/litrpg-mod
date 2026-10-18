package com.andrey.litrpg.party;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Группы игроков (до 6 человек). Опыт за убийства делится внутри группы,
 * а за каждого товарища рядом все получают +10% опыта.
 * Группы живут, пока работает сервер.
 */
public final class Party {
    public static final int MAX_SIZE = 6;
    public static final double SHARE_RANGE = 48;

    private static final Map<UUID, Party> BY_PLAYER = new HashMap<>();
    /** Приглашения: кого пригласили → в чью группу (лидер). */
    private static final Map<UUID, UUID> INVITES = new HashMap<>();

    private UUID leader;
    private final Set<UUID> members = new LinkedHashSet<>();

    private Party(UUID leader) {
        this.leader = leader;
        members.add(leader);
    }

    public UUID leader() { return leader; }
    public Set<UUID> members() { return members; }

    public static Party of(ServerPlayer p) { return BY_PLAYER.get(p.getUUID()); }

    public static Party getOrCreate(ServerPlayer p) {
        return BY_PLAYER.computeIfAbsent(p.getUUID(), Party::new);
    }

    public static void invite(ServerPlayer leader, ServerPlayer target) {
        getOrCreate(leader);
        INVITES.put(target.getUUID(), leader.getUUID());
    }

    /** @return группа, в которую вступил игрок, или null */
    public static Party accept(ServerPlayer p) {
        UUID leaderId = INVITES.remove(p.getUUID());
        if (leaderId == null) return null;
        Party party = BY_PLAYER.get(leaderId);
        if (party == null || party.members.size() >= MAX_SIZE) return null;
        leave(p);
        party.members.add(p.getUUID());
        BY_PLAYER.put(p.getUUID(), party);
        return party;
    }

    public static void leave(ServerPlayer p) {
        Party party = BY_PLAYER.remove(p.getUUID());
        if (party == null) return;
        party.members.remove(p.getUUID());
        if (party.members.isEmpty()) return;
        if (party.leader.equals(p.getUUID())) party.leader = party.members.iterator().next();
        if (party.members.size() == 1) BY_PLAYER.remove(party.members.iterator().next());
    }

    public List<ServerPlayer> online(MinecraftServer server) {
        List<ServerPlayer> list = new ArrayList<>();
        for (UUID id : members) {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) list.add(sp);
        }
        return list;
    }

    public static boolean sameParty(ServerPlayer a, ServerPlayer b) {
        Party pa = of(a);
        return pa != null && pa == of(b);
    }

    public static void clear() {
        BY_PLAYER.clear();
        INVITES.clear();
    }
}
