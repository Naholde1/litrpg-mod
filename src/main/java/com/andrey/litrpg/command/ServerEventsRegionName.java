package com.andrey.litrpg.command;

import com.andrey.litrpg.world.Zone;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Название места, где стоит игрок: город, земля народа или Пограничье. */
public final class ServerEventsRegionName {
    private ServerEventsRegionName() {}

    public static String of(ServerPlayer p) {
        if (!SystemWorld.isOverworld(p.serverLevel())) {
            return Component.translatable(Zone.at(p.serverLevel(), p.blockPosition()).nameKey()).getString();
        }
        WorldMap map = WorldMap.get();
        WorldMap.City c = map.cityAt(p.getX(), p.getZ());
        if (c != null) return c.name();
        double edge = Math.max(Math.abs(p.getX()), Math.abs(p.getZ()));
        if (edge > map.wall0 + 100) return "Край Мира";
        WorldMap.Territory t = map.territory(map.territoryAt(p.getX(), p.getZ()));
        return t != null ? t.name : "Пограничье";
    }
}
