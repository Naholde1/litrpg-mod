package com.andrey.litrpg.city;

import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.world.Dungeon;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

/** Разговоры горожан: местные слухи и подсказки, где искать подземелья и чудеса. */
public final class Gossip {
    private Gossip() {}

    private static final String[] COMMON = {
            "Говорят, чем дальше от городов, тем злее твари. И их всё больше.",
            "Стража у нас суровая. Не трогай чужого — пожалеешь.",
            "Торговцы на тракте платят серебром тем, кто проводит караван.",
            "Ночью на дорогах фонари горят не просто так — держись света.",
            "У Края Мира горы до небес. Говорят, за ними — ничто.",
            "Слышал, в кузнях дварфов ковали клинки для самой Системы.",
            "Эльфы не любят, когда рубят их лес. А пауков — ещё больше.",
            "Если увидишь Кровавую луну — прячься. Или готовь оружие.",
            "Карта мира висит на стене в каждом большом городе. Взгляни!",
            "Старики говорят: Система следит за каждым шагом Избранных.",
    };

    public static void talk(ServerPlayer p, RaceNpcEntity npc) {
        var rnd = p.getRandom();
        String line = COMMON[rnd.nextInt(COMMON.length)];
        // слух о сюжете города, пока он не пройден
        WorldMap.City city = SystemWorld.isOverworld(p.serverLevel()) ? WorldMap.get().cityAt(npc.getX(), npc.getZ()) : null;
        var chain = city == null ? null : com.andrey.litrpg.quest.CityStories.forCity(city.id());
        boolean storyHint = chain != null && !com.andrey.litrpg.stats.StatsManager.get(p).quests().chainsDone.contains(chain.id) && rnd.nextFloat() < 0.35f;
        if (storyHint) {
            line = chain.hook + " (Сюжет «" + chain.title + "» — на досках мастеров.)";
        } else if (SystemWorld.isOverworld(p.serverLevel()) && rnd.nextFloat() < 0.6f) {
            String hint = hint(p, rnd.nextInt(3));
            if (hint != null) line = hint;
        }
        p.sendSystemMessage(Component.empty().append(npc.getName().copy().withStyle(ChatFormatting.GOLD))
                .append(Component.literal(": «" + line + "»").withStyle(ChatFormatting.WHITE)));
        npc.playSound(SoundEvents.VILLAGER_AMBIENT, 1f, npc.getVoicePitch());
        npc.getLookControl().setLookAt(p);
    }

    /** Подсказка о ближайшем подземелье, чуде света или поселении. */
    private static String hint(ServerPlayer p, int kind) {
        WorldMap map = WorldMap.get();
        WorldMap.Poi best = null;
        double bestD = 1e18;
        String want = kind == 0 ? "dungeon" : kind == 1 ? "wonder" : "village";
        for (WorldMap.Poi poi : map.pois) {
            if (!poi.type().equals(want)) continue;
            double d = Math.hypot(poi.x() - p.getX(), poi.z() - p.getZ());
            if (d > 120 && d < bestD) {
                bestD = d;
                best = poi;
            }
        }
        if (best == null) return null;
        String dir = direction(best.x() - p.getX(), best.z() - p.getZ());
        int dist = (int) Math.round(bestD / 50) * 50;
        return switch (want) {
            case "dungeon" -> {
                Dungeon d = Dungeon.byPoi(best.extra());
                yield "Слыхал? На " + dir + "е, шагов за " + dist + ", есть " + (d != null ? "подземелье «" + d.title + "» (опасность — ур. " + d.level + ")"
                        : "проклятое подземелье") + ". Хранитель там лютый.";
            }
            case "wonder" -> "Если пойдёшь на " + dir + ", шагов через " + dist + " увидишь " + best.name() + ". Такое не забудешь!";
            default -> "На " + dir + "е, шагов за " + dist + ", — " + best.name() + ". Там всегда рады путникам.";
        };
    }

    private static String direction(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz));
        if (a < 0) a += 360;
        String[] dirs = {"север", "северо-восток", "восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад"};
        return dirs[(int) Math.round(a / 45) % 8];
    }
}
