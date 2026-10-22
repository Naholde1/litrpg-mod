package com.andrey.litrpg.city;

import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.OpenServicePacket;
import com.andrey.litrpg.npc.Craft;
import com.andrey.litrpg.npc.RaceNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;

/** Услуги жителей города в отдельном окне: круг телепорта и комнаты постоялого двора. */
public final class Services {
    private Services() {}

    public static void open(ServerPlayer p, RaceNpcEntity npc) {
        if (npc.craft() == Craft.TELEPORTER) Teleport.open(p, npc);
        else if (npc.craft() == Craft.HOSTELER) Inns.open(p, npc);
    }

    public static void choose(ServerPlayer p, RaceNpcEntity npc, String key) {
        if (npc.craft() == Craft.TELEPORTER) Teleport.choose(p, npc, key);
        else if (npc.craft() == Craft.HOSTELER) Inns.choose(p, npc, key);
    }

    static void send(ServerPlayer p, RaceNpcEntity npc, String title, String intro, List<OpenServicePacket.Entry> entries) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new OpenServicePacket(npc.getId(), title, intro, entries));
    }

    /** Короткое имя жителя без ремесла («Борис» из «Борис, маг порталов»). */
    static String shortName(RaceNpcEntity npc) {
        String n = npc.getName().getString();
        return n.indexOf(',') > 0 ? n.substring(0, n.indexOf(',')) : n;
    }
}
