package com.andrey.litrpg.party;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.PartyPacket;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** /party invite|accept|leave|list — доступно всем игрокам. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class PartyCommand {
    private PartyCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("party")
                .then(Commands.literal("invite").then(Commands.argument("player", EntityArgument.player()).executes(c -> {
                    ServerPlayer self = c.getSource().getPlayerOrException();
                    ServerPlayer target = EntityArgument.getPlayer(c, "player");
                    if (target == self) return 0;
                    Party.invite(self, target);
                    StatsManager.message(self, Component.literal("Приглашение отправлено: " + target.getGameProfile().getName()));
                    StatsManager.message(target, Component.literal(self.getGameProfile().getName() + " зовёт вас в группу. ")
                            .append(Component.literal("[Принять]").withStyle(Style.EMPTY.withColor(ChatFormatting.GREEN).withBold(true)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept")))));
                    return 1;
                })))
                .then(Commands.literal("accept").executes(c -> {
                    ServerPlayer self = c.getSource().getPlayerOrException();
                    Party party = Party.accept(self);
                    if (party == null) {
                        StatsManager.message(self, Component.literal("Нет приглашений (или группа заполнена).").withStyle(ChatFormatting.RED));
                        return 0;
                    }
                    for (ServerPlayer p : party.online(self.server)) {
                        StatsManager.message(p, Component.literal(self.getGameProfile().getName() + " вступает в группу. Опыт теперь общий, +10% за каждого товарища рядом.")
                                .withStyle(ChatFormatting.GREEN));
                    }
                    return 1;
                }))
                .then(Commands.literal("leave").executes(c -> {
                    ServerPlayer self = c.getSource().getPlayerOrException();
                    Party party = Party.of(self);
                    Party.leave(self);
                    StatsManager.message(self, Component.literal("Вы покинули группу."));
                    if (party != null) for (ServerPlayer p : party.online(self.server)) {
                        StatsManager.message(p, Component.literal(self.getGameProfile().getName() + " покидает группу.").withStyle(ChatFormatting.GRAY));
                    }
                    send(self, List.of());
                    return 1;
                }))
                .then(Commands.literal("list").executes(c -> {
                    ServerPlayer self = c.getSource().getPlayerOrException();
                    Party party = Party.of(self);
                    if (party == null) {
                        StatsManager.message(self, Component.literal("Вы не в группе. /party invite <игрок>"));
                        return 0;
                    }
                    StringBuilder sb = new StringBuilder("Группа: ");
                    for (ServerPlayer p : party.online(self.server)) {
                        sb.append(p.getGameProfile().getName()).append(" (ур. ").append(StatsManager.get(p).getLevel()).append(") ");
                    }
                    StatsManager.message(self, Component.literal(sb.toString()));
                    return 1;
                })));
    }

    /** Раз в секунду рассылаем состояние группы для панели в HUD. */
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().getTickCount() % 20 != 0) return;
        for (ServerPlayer self : event.getServer().getPlayerList().getPlayers()) {
            Party party = Party.of(self);
            if (party == null) continue;
            List<PartyPacket.Member> list = new ArrayList<>();
            for (ServerPlayer p : party.online(self.server)) {
                if (p == self) continue;
                PlayerStats s = StatsManager.get(p);
                list.add(new PartyPacket.Member(p.getGameProfile().getName(), p.getHealth(), p.getMaxHealth(),
                        (float) (s.getMana() / Math.max(1, s.getMaxMana())), s.getLevel(),
                        s.getPlayerClass() == null ? 0xB0BEC5 : s.getPlayerClass().color));
            }
            send(self, list);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Party.leave(p);
    }

    private static void send(ServerPlayer p, List<PartyPacket.Member> list) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new PartyPacket(list));
    }
}
