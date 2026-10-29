package com.andrey.litrpg.command;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Stat;
import com.andrey.litrpg.stats.StatsManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.BiConsumer;

/**
 * /litrpg — команды для тестов (уровень прав 2, нужны читы).
 * Примеры: /litrpg level set @s 30, /litrpg spawn litrpg:goblin 25 champion
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class LitRpgCommand {
    private static final SimpleCommandExceptionType BAD_VALUE =
            new SimpleCommandExceptionType(Component.literal("Неизвестное значение"));

    private static final SuggestionProvider<CommandSourceStack> STATS =
            (c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Stat.values()).map(s -> s.key), b);
    private static final SuggestionProvider<CommandSourceStack> CLASSES =
            (c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(PlayerClass.values()).map(s -> s.key), b);
    private static final SuggestionProvider<CommandSourceStack> RANKS =
            (c, b) -> SharedSuggestionProvider.suggest(new String[]{"normal", "elite", "champion"}, b);

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher(), event.getBuildContext());
    }

    private static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("litrpg").requires(s -> s.hasPermission(2));

        root.then(Commands.literal("info")
                .executes(c -> info(c.getSource(), c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> info(c.getSource(), EntityArgument.getPlayer(c, "player")))));

        root.then(Commands.literal("level")
                .then(Commands.literal("set").then(players().then(Commands.argument("level", IntegerArgumentType.integer(1, PlayerStats.MAX_LEVEL))
                        .executes(c -> each(c, (p, s) -> {
                            int before = s.getLevel();
                            s.setLevel(IntegerArgumentType.getInteger(c, "level"));
                            if (s.getLevel() > before) StatsManager.onLevelUp(p, before, s.getLevel());
                        })))))
                .then(Commands.literal("add").then(players().then(Commands.argument("levels", IntegerArgumentType.integer(1, 99))
                        .executes(c -> each(c, (p, s) -> {
                            int before = s.getLevel();
                            s.setLevel(before + IntegerArgumentType.getInteger(c, "levels"));
                            if (s.getLevel() > before) StatsManager.onLevelUp(p, before, s.getLevel());
                        }))))));

        root.then(Commands.literal("xp").then(players().then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(c -> {
                    int n = IntegerArgumentType.getInteger(c, "amount");
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) StatsManager.giveXp(p, n);
                    return 1;
                }))));

        root.then(Commands.literal("points").then(players().then(Commands.argument("amount", IntegerArgumentType.integer(0, 10000))
                .executes(c -> each(c, (p, s) -> s.setFreePoints(IntegerArgumentType.getInteger(c, "amount")))))));

        root.then(Commands.literal("stat").then(players()
                .then(Commands.argument("stat", StringArgumentType.word()).suggests(STATS)
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 10000))
                                .executes(c -> {
                                    Stat st = parse(Stat.values(), StringArgumentType.getString(c, "stat"), x -> x.key);
                                    return each(c, (p, s) -> s.setSpent(st, IntegerArgumentType.getInteger(c, "value")));
                                })))));

        root.then(Commands.literal("class")
                .then(Commands.literal("set").then(players()
                        .then(Commands.argument("class", StringArgumentType.word()).suggests(CLASSES)
                                .executes(c -> {
                                    PlayerClass cls = parse(PlayerClass.values(), StringArgumentType.getString(c, "class"), x -> x.key);
                                    return each(c, (p, s) -> StatsManager.chooseClass(p, cls, false));
                                }))))
                .then(Commands.literal("offer").then(players().executes(c -> each(c, (p, s) -> {
                    // сбросить класс и заново подобрать три по стилю (нужен 10+ уровень)
                    s.setPlayerClass(null);
                    s.setOffers(java.util.List.of());
                    if (s.getLevel() < PlayerStats.CLASS_LEVEL) s.setLevel(PlayerStats.CLASS_LEVEL);
                    StatsManager.sync(p);
                    StatsManager.offerClasses(p);
                }))))
                .then(Commands.literal("clear").then(players().executes(c -> each(c, (p, s) -> {
                    s.setPlayerClass(null);
                    s.setOffers(java.util.List.of());
                }))))
                // ранги навыков: поднять за очки класса (как кнопка «+» в окне) или сбросить
                .then(Commands.literal("rank").then(players()
                        .then(Commands.argument("skill", StringArgumentType.word())
                                .executes(c -> each(c, (p, s) -> s.rankUp(StringArgumentType.getString(c, "skill")))))))
                .then(Commands.literal("unrank").then(players().executes(c -> each(c, (p, s) -> s.resetRanks())))));

        root.then(Commands.literal("role").then(players()
                .then(Commands.argument("role", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(com.andrey.litrpg.stats.Role.values()).map(x -> x.key), b))
                        .executes(c -> {
                            var r = parse(com.andrey.litrpg.stats.Role.values(), StringArgumentType.getString(c, "role"), x -> x.key);
                            return each(c, (p, s) -> {
                                if (s.hasClass()) s.setPlayerClass(null);
                                StatsManager.chooseRole(p, r);
                                s.setOffers(java.util.List.of());
                            });
                        }))));

        root.then(Commands.literal("style").then(players()
                .then(Commands.argument("style", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(com.andrey.litrpg.stats.Style.values()).map(x -> x.key), b))
                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 1000000))
                                .executes(c -> {
                                    var st = parse(com.andrey.litrpg.stats.Style.values(), StringArgumentType.getString(c, "style"), x -> x.key);
                                    return each(c, (p, s) -> s.getStyles().put(st, IntegerArgumentType.getInteger(c, "value")));
                                })))));

        root.then(Commands.literal("mana").then(players().executes(c -> each(c, (p, s) -> s.setMana(s.getMaxMana())))));
        root.then(Commands.literal("cooldowns").then(players().executes(c -> each(c, (p, s) -> s.resetCooldowns()))));
        root.then(Commands.literal("respec").then(players().executes(c -> each(c, (p, s) -> s.respec()))));
        root.then(Commands.literal("reset").then(players().executes(c -> each(c, (p, s) -> {
            s.wipe();
            StatsManager.openOriginScreen(p);
        }))));
        root.then(Commands.literal("codex").then(players().executes(c -> {
            for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) StatsManager.giveCodex(p);
            return 1;
        })));

        root.then(Commands.literal("cast").then(players().then(Commands.argument("slot", IntegerArgumentType.integer(1, 9))
                .executes(c -> {
                    int slot = IntegerArgumentType.getInteger(c, "slot") - 1;
                    for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                        StatsManager.get(p).resetCooldowns();
                        com.andrey.litrpg.ability.AbilityHandler.tryUse(p, slot);
                    }
                    return 1;
                }))));

        root.then(Commands.literal("gear").then(players()
                .then(Commands.argument("level", IntegerArgumentType.integer(1, 99))
                        .then(Commands.argument("rarity", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(com.andrey.litrpg.gear.Rarity.values()).map(x -> x.key), b))
                                .executes(c -> giveGear(c, 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 36)).executes(c -> giveGear(c, IntegerArgumentType.getInteger(c, "count"))))))));

        root.then(Commands.literal("unique").then(players()
                .then(Commands.argument("item", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(com.andrey.litrpg.gear.GearGen.UNIQUES, b))
                        .executes(c -> {
                            String key = StringArgumentType.getString(c, "item");
                            for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
                                var st = com.andrey.litrpg.gear.GearGen.unique(key);
                                if (!p.getInventory().add(st)) p.drop(st, false);
                            }
                            return 1;
                        }))));

        root.then(Commands.literal("spawn")
                .then(Commands.argument("entity", ResourceArgument.resource(ctx, Registries.ENTITY_TYPE))
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 99))
                                .executes(c -> spawn(c, "normal"))
                                .then(Commands.argument("rank", StringArgumentType.word()).suggests(RANKS)
                                        .executes(c -> spawn(c, StringArgumentType.getString(c, "rank")))))));

        // ---- прохождение подземелья ----
        root.then(Commands.literal("run")
                .then(Commands.literal("status").executes(c -> {
                    var st = com.andrey.litrpg.run.Run.state(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Этап " + st.stage + ", попыток " + (st.wipes + 1) + ", группа " + st.partySize
                            + ", хранители " + st.deadBosses + ", отряды пали " + st.deadPacks.size() + ", мин. игроков " + st.minPlayers), false);
                    return 1;
                }))
                .then(Commands.literal("stage").then(Commands.argument("n", IntegerArgumentType.integer(1, 5)).executes(c -> {
                    com.andrey.litrpg.run.Run.enterStage(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "n"));
                    return 1;
                })))
                .then(Commands.literal("wipe").executes(c -> {
                    com.andrey.litrpg.run.Run.wipe(c.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("lobby").executes(c -> {
                    var st = com.andrey.litrpg.run.Run.state(c.getSource().getServer());
                    st.stage = 0;
                    st.clearStage();
                    for (ServerPlayer p : com.andrey.litrpg.run.Run.party(c.getSource().getServer())) {
                        com.andrey.litrpg.run.Run.teleport(p, com.andrey.litrpg.run.Layout.get().lobbySpawn);
                        com.andrey.litrpg.run.Run.restore(p);
                    }
                    return 1;
                }))
                .then(Commands.literal("minplayers").then(Commands.argument("n", IntegerArgumentType.integer(1, 5)).executes(c -> {
                    var st = com.andrey.litrpg.run.Run.state(c.getSource().getServer());
                    st.minPlayers = IntegerArgumentType.getInteger(c, "n");
                    st.setDirty();
                    c.getSource().sendSuccess(() -> Component.literal("Минимум игроков для начала: " + st.minPlayers), true);
                    return 1;
                })))
                .then(Commands.literal("claims").then(players().executes(c -> each(c, (p, s) -> s.clearClaims())))));

        d.register(root);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, ?> players() {
        return Commands.argument("players", EntityArgument.players());
    }

    private static int each(CommandContext<CommandSourceStack> c, BiConsumer<ServerPlayer, PlayerStats> action)
            throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "players");
        for (ServerPlayer p : players) {
            PlayerStats s = StatsManager.get(p);
            action.accept(p, s);
            StatsManager.applyAttributes(p);
            StatsManager.sync(p);
        }
        c.getSource().sendSuccess(() -> Component.literal("Готово: игроков — " + players.size()).withStyle(ChatFormatting.GREEN), true);
        return players.size();
    }

    private static <T> T parse(T[] values, String key, java.util.function.Function<T, String> keyOf) throws CommandSyntaxException {
        for (T v : values) if (keyOf.apply(v).equalsIgnoreCase(key)) return v;
        throw BAD_VALUE.create();
    }

    private static int spawn(CommandContext<CommandSourceStack> c, String rankKey) throws CommandSyntaxException {
        Holder.Reference<EntityType<?>> type = ResourceArgument.getSummonableEntityType(c, "entity");
        int lvl = IntegerArgumentType.getInteger(c, "level");
        MobLevels.Rank rank = switch (rankKey.toLowerCase()) {
            case "elite" -> MobLevels.Rank.ELITE;
            case "champion" -> MobLevels.Rank.CHAMPION;
            case "normal" -> MobLevels.Rank.NORMAL;
            default -> throw BAD_VALUE.create();
        };
        ServerLevel level = c.getSource().getLevel();
        Entity e = type.value().create(level);
        if (!(e instanceof Mob mob)) throw BAD_VALUE.create();
        var pos = c.getSource().getPosition();
        mob.moveTo(pos.x, pos.y, pos.z, level.random.nextFloat() * 360, 0);
        // уровень ставим до добавления в мир, чтобы автоматическое назначение его не перезаписало
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.COMMAND, null, null);
        MobLevels.apply(mob, lvl, rank);
        level.addFreshEntity(mob);
        c.getSource().sendSuccess(() -> Component.literal("Призван: ").append(mob.getDisplayName()), true);
        return 1;
    }

    private static int giveGear(CommandContext<CommandSourceStack> c, int count) throws CommandSyntaxException {
        var rarity = parse(com.andrey.litrpg.gear.Rarity.values(), StringArgumentType.getString(c, "rarity"), x -> x.key);
        int lvl = IntegerArgumentType.getInteger(c, "level");
        for (ServerPlayer p : EntityArgument.getPlayers(c, "players")) {
            for (int i = 0; i < count; i++) {
                var st = com.andrey.litrpg.gear.GearGen.random(p.getRandom(), lvl, rarity);
                if (!p.getInventory().add(st)) p.drop(st, false);
            }
        }
        return count;
    }

    private static int info(CommandSourceStack src, ServerPlayer p) {
        PlayerStats s = StatsManager.get(p);
        StringBuilder sb = new StringBuilder();
        sb.append(p.getGameProfile().getName()).append(": ур. ").append(s.getLevel())
                .append(", опыт ").append(s.getXp()).append('/').append(s.xpForNext())
                .append(", очки ").append(s.getFreePoints());
        sb.append("\nРоль: ").append(s.getRole() == null ? "—" : s.getRole().title);
        sb.append(", класс: ").append(s.getPlayerClass() == null ? "Новичок" : Component.translatable(s.getPlayerClass().nameKey()).getString());
        sb.append("\nХарактеристики:");
        for (Stat st : Stat.values()) {
            sb.append(' ').append(Component.translatable(st.translationKey()).getString()).append('=').append(s.getStat(st));
        }
        var g = s.gear();
        sb.append("\nЭкипировка: сила+").append(g.stats[0]).append(" ловк+").append(g.stats[1]).append(" вын+").append(g.stats[2])
                .append(" инт+").append(g.stats[3]).append(" удача+").append(g.stats[4]).append(", бонусы ").append(g.bonus)
                .append(", пассивки ").append(g.passives).append(", наборы ").append(g.setPieces);
        sb.append("\nСтиль:");
        for (var e : com.andrey.litrpg.stats.ClassOffers.topStyles(s, 10)) {
            sb.append(' ').append(Component.translatable(e.getKey().nameKey()).getString()).append(' ').append(e.getValue()).append('%');
        }
        String text = sb.toString();
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
