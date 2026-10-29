package com.andrey.litrpg.ability;

import com.andrey.litrpg.stats.PlayerStats;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Способности со своей механикой.
 * <p>
 * Остальные навыки собраны из общих видов движка — волна, снаряд, зона, — и этого хватает,
 * пока у способности нет собственной идеи. Но там, где идея есть («слуги лопаются», «тотем
 * стоит, пока его не разобьют»), общий вид её не исполняет: описание обещает одно, а в игре
 * происходит обычный сплеш. Такие способности пишутся здесь поимённо, по ключу навыка,
 * и подменяют общий вид целиком — вместе со своими эффектами.
 */
public final class Special {
    /** Что делает способность. Возвращает false, если применить её сейчас нельзя. */
    public interface Impl {
        boolean run(ServerPlayer p, PlayerStats s, Ability a);
    }

    private static final Map<String, Impl> BY_KEY = new HashMap<>();

    private Special() {}

    static void put(String key, Impl impl) {
        BY_KEY.put(key, impl);
    }

    /** Есть ли у навыка своя механика. */
    public static boolean has(Ability a) {
        return BY_KEY.containsKey(a.key);
    }

    /** Выполнить свою механику; null — её нет, работает общий вид. */
    public static Boolean run(ServerPlayer p, PlayerStats s, Ability a) {
        Impl impl = BY_KEY.get(a.key);
        return impl == null ? null : impl.run(p, s, a);
    }

    static {
        Summoner.register();
        Totems.register();
        Astromancer.register();
        Swift.register();
        Machinist.register();
        Berserker.register();
        Healer.register();
        Alchemist.register();
        Sniper.register();
        Warden.register();
        BloodKnight.register();
        Lawgiver.register();
        Symbiote.register();
        Graviton.register();
        MirrorKnight.register();
        StoneKit.register();
        Werebeast.register();
        Executioner.register();
        Plaguelord.register();
        Spirit.register();
        Shade.register();
        Pyromancer.register();
        Monk.register();
        SoulDrain.register();
        Chronologist.register();
        Oracle.register();
        Skald.register();
        Wellspring.register();
        DarkPriest.register();
    }

    /** Трогается при загрузке класса, чтобы статический блок отработал. */
    public static void init() {}
}
