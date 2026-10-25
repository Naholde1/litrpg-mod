package com.andrey.litrpg.client.music;

import java.util.Map;

/**
 * Музыка из файлов (assets/litrpg/sounds/music/*.ogg, громкость выровнена tools/gen_music.py).
 * Хранитель → трек; у кого трека нет — синтезированный фонк ({@link PhonkSynth}).
 */
public final class Tracks {
    private Tracks() {}

    /** id хранителя (путь сущности) → звук litrpg:music.NAME. */
    public static final Map<String, String> BOSS = Map.ofEntries(
            Map.entry("gatekeeper", "fight_club"),
            Map.entry("lich_king", "fear"),
            Map.entry("spider_queen", "blud"),
            Map.entry("rot_ent", "fragment"),
            Map.entry("dark_witch", "kurayami"),
            Map.entry("leviathan", "blue_lock"),
            Map.entry("deep_priestess", "ecstasy_of_gold"),
            Map.entry("smelter_golem", "woops"),
            Map.entry("fire_twin_ignis", "wutiwant"),
            Map.entry("fire_twin_cinder", "wutiwant"),
            Map.entry("ash_lord", "aura_of_glory"),
            Map.entry("chronophage", "next_opponent"),
            Map.entry("world_eater", "no_time_for_caution"),
            Map.entry("xalathar", "red"));

    public static final String LOBBY = "as_time_flies";
    public static final String CREDITS = "jane_hoodtrap";
}
