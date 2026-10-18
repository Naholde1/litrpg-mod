package com.andrey.litrpg.util;

import com.andrey.litrpg.LitRPG;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntPredicate;

/** Отложенные и повторяющиеся задачи на серверном потоке (метеор, дождь стрел, аура). */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Scheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Scheduler() {}

    /** Выполнить один раз через delay тиков. */
    public static void later(int delay, Runnable r) {
        PENDING.add(new Task(delay, 1, 1, i -> { r.run(); return true; }));
    }

    /**
     * Повторять каждые period тиков, times раз. Задача получает номер
     * повтора и может вернуть false, чтобы остановиться раньше.
     */
    public static void repeat(int delay, int period, int times, IntPredicate body) {
        PENDING.add(new Task(delay, period, times, body));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!PENDING.isEmpty()) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.wait > 0) continue;
            boolean keep;
            try {
                keep = t.body.test(t.done);
            } catch (RuntimeException e) {
                LitRPG.LOGGER.error("Scheduled task failed", e);
                keep = false;
            }
            t.done++;
            if (!keep || t.done >= t.times) it.remove();
            else t.wait = t.period;
        }
        com.andrey.litrpg.stats.StatsManager.flushSync();
    }

    @SubscribeEvent
    public static void onStop(ServerStoppingEvent event) {
        TASKS.clear();
        PENDING.clear();
        com.andrey.litrpg.stats.StatsManager.clearSync();
    }

    private static final class Task {
        int wait;
        final int period;
        final int times;
        final IntPredicate body;
        int done = 0;

        Task(int delay, int period, int times, IntPredicate body) {
            this.wait = Math.max(1, delay);
            this.period = Math.max(1, period);
            this.times = times;
            this.body = body;
        }
    }
}
