package com.andrey.litrpg.city;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.network.OpenServicePacket;
import com.andrey.litrpg.npc.NpcTrades;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.npc.Wallet;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Круги телепорта: маг порталов в каждом городе переносит в любой другой город за плату.
 * Цена растёт с расстоянием и зависит от отношения народа мага. Заклинание длится 3 секунды —
 * сойдёшь с места или получишь удар, и оно сорвётся (деньги не берутся).
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Teleport {
    private Teleport() {}

    private static final int CAST = 60;

    private record Pending(String city, int price, Vec3 start, int npcId, long started) {}

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** Цена в медяках: 12 + 1 за каждые 30 блоков, со скидкой или наценкой по репутации. */
    public static int price(ServerPlayer p, RaceNpcEntity npc, WorldMap.City to) {
        double d = Math.hypot(to.x() - npc.getX(), to.z() - npc.getZ());
        float base = 12 + (float) d / 30f;
        return Math.max(1, Math.round(base * NpcTrades.repPriceMult(StatsManager.get(p).getRep(npc.npcType().faction))));
    }

    private static String side(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dz, dx));
        String[] names = {"восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад", "север", "северо-восток"};
        int k = (int) Math.floorMod(Math.round(a / 45.0), 8);
        return names[k];
    }

    public static void open(ServerPlayer p, RaceNpcEntity npc) {
        String who = Services.shortName(npc);
        if (!SystemWorld.isOverworld(p.level())) {
            StatsManager.message(p, Component.literal(who + ": «Нити Системы здесь не держат. Круги работают только в Мире Системы».").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (CityLaw.isWanted(p)) {
            Speech.say(npc, "Стража тебя ищет — я не стану тебя прятать!", 60);
            StatsManager.message(p, Component.literal(who + ": «Ты в розыске. Круг для тебя закрыт».").withStyle(ChatFormatting.RED));
            return;
        }
        int rep = StatsManager.get(p).getRep(npc.npcType().faction);
        if (rep < Faction.Tier.NEUTRAL.from) {
            StatsManager.message(p, Component.literal(who + ": «Я не открою круг для врага моего народа».").withStyle(ChatFormatting.RED));
            return;
        }
        WorldMap.City here = WorldMap.get().cityAt(npc.getX(), npc.getZ());
        List<WorldMap.City> list = new ArrayList<>(WorldMap.get().cities);
        list.removeIf(c -> here != null && c.id().equals(here.id()));
        list.sort(Comparator.comparingDouble(c -> Math.hypot(c.x() - npc.getX(), c.z() - npc.getZ())));
        int money = Wallet.total(p);
        List<OpenServicePacket.Entry> rows = new ArrayList<>();
        for (WorldMap.City c : list) {
            int price = price(p, npc, c);
            double d = Math.hypot(c.x() - npc.getX(), c.z() - npc.getZ());
            String detail = Math.round(d) + " бл., " + side(c.x() - npc.getX(), c.z() - npc.getZ()) + " • земли ур. " + c.level();
            rows.add(new OpenServicePacket.Entry("tp:" + c.id(), c.name(), detail, GearData.coins(price), "В путь", 0xB070FF, money >= price));
        }
        String intro = who + ": «Круги всех городов связаны нитями Системы. Встань в круг, назови город и не шевелись три вздоха»."
                + (here != null ? " Вы в городе " + here.name() + "." : "");
        Services.send(p, npc, "Круг телепорта", intro, rows);
        Speech.say(npc, "Куда держишь путь?", 50);
    }

    public static void choose(ServerPlayer p, RaceNpcEntity npc, String key) {
        if (!key.startsWith("tp:") || PENDING.containsKey(p.getUUID())) return;
        WorldMap.City to = WorldMap.get().city(key.substring(3));
        if (to == null || CityLaw.isWanted(p)) return;
        int price = price(p, npc, to);
        if (Wallet.total(p) < price) {
            StatsManager.message(p, Component.literal("Не хватает денег: нужно " + GearData.coins(price) + ".").withStyle(ChatFormatting.RED));
            return;
        }
        p.closeContainer();
        PENDING.put(p.getUUID(), new Pending(to.id(), price, p.position(), npc.getId(), p.level().getGameTime()));
        Speech.say(npc, "Держись! Нити тянут в " + to.name() + "…", 60);
        npc.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        p.displayClientMessage(Component.literal("✦ Круг пробуждается… не двигайся (3 с)").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    private static void cancel(ServerPlayer p, String why) {
        if (PENDING.remove(p.getUUID()) == null) return;
        p.displayClientMessage(Component.literal("✦ " + why + " — заклинание сорвалось").withStyle(ChatFormatting.RED), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    @SubscribeEvent
    public static void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) {
        PENDING.remove(e.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && PENDING.containsKey(p.getUUID()) && e.getAmount() > 0) cancel(p, "Удар сбил сосредоточение");
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        Pending t = PENDING.get(p.getUUID());
        if (t == null) return;
        ServerLevel sl = p.serverLevel();
        long age = sl.getGameTime() - t.started;
        if (!p.isAlive() || p.position().distanceToSqr(t.start) > 1.5 * 1.5) {
            cancel(p, "Ты сошёл с места");
            return;
        }
        // нити Системы сходятся к игроку
        double r = 1.6 - age / (double) CAST;
        for (int i = 0; i < 3; i++) {
            double a = (age * 0.35 + i * 2.094);
            sl.sendParticles(ParticleTypes.PORTAL, p.getX() + Math.cos(a) * r, p.getY() + 0.2 + age / 30.0, p.getZ() + Math.sin(a) * r, 2, 0, 0, 0, 0.05);
        }
        if (age % 10 == 0) sl.sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1, p.getZ(), 20, 0.6, 1, 0.6, 0.6);
        if (age < CAST) return;
        PENDING.remove(p.getUUID());
        WorldMap.City to = WorldMap.get().city(t.city);
        if (to == null) return;
        if (!Wallet.pay(p, t.price)) {
            p.displayClientMessage(Component.literal("Не хватает денег на перенос").withStyle(ChatFormatting.RED), true);
            return;
        }
        BlockPos dest = arrival(sl, to);
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 80, 0.4, 1, 0.4, 0.2);
        sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
        p.stopRiding();
        p.teleportTo(sl, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false));
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.getX() + 0.5, dest.getY() + 1, dest.getZ() + 0.5, 120, 0.5, 1.2, 0.5, 0.25);
        sl.playSound(null, dest, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.1f);
        sl.playSound(null, dest, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.7f);
        StatsManager.message(p, Component.literal("✦ Круг телепорта: " + to.name() + ". Плата — " + GearData.coins(t.price) + ".")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Точка прибытия: середина круга телепорта (или площадь, если круга нет). */
    static BlockPos arrival(ServerLevel sl, WorldMap.City c) {
        int[] a = WorldMap.get().anchor(c.id(), "portal");
        if (a == null) a = WorldMap.get().anchor(c.id(), "center");
        if (a != null) return new BlockPos(a[0], a[1], a[2]);
        sl.getChunk(c.x() >> 4, c.z() >> 4);
        return new BlockPos(c.x(), sl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c.x(), c.z()), c.z());
    }
}
