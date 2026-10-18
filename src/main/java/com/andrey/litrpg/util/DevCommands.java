package com.andrey.litrpg.util;

import com.andrey.litrpg.LitRPG;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Только для среды разработки (в собранном моде не работает): раз в секунду
 * выполняет команды из файла run/dev_commands.txt от имени сервера и удаляет файл.
 * Позволяет автоматически тестировать мод в запущенной игре.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DevCommands {
    private static final Path FILE = Path.of("dev_commands.txt");

    private DevCommands() {}

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || FMLLoader.isProduction()) return;
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0 || !Files.exists(FILE)) return;
        try {
            List<String> lines = Files.readAllLines(FILE, StandardCharsets.UTF_8);
            Files.delete(FILE);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                LitRPG.LOGGER.info("[dev] /{}", line);
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), line);
            }
        } catch (Exception e) {
            LitRPG.LOGGER.error("dev commands failed", e);
        }
    }
}
