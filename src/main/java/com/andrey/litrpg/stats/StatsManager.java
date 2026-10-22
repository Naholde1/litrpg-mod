package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.network.ModNetwork;
import com.andrey.litrpg.network.OpenClassChoicePacket;
import com.andrey.litrpg.network.OpenOriginPacket;
import com.andrey.litrpg.network.SyncStatsPacket;
import com.andrey.litrpg.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;

public class StatsManager {
    private static final UUID HEALTH_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a01");
    private static final UUID DAMAGE_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a02");
    private static final UUID SPEED_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a03");
    private static final UUID ATK_SPEED_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a04");
    private static final UUID ARMOR_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a05");
    private static final UUID KNOCKBACK_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a06");
    private static final UUID LUCK_ID = UUID.fromString("6f2b6a1e-1c55-4b8e-9d31-2a1f0c4e7a07");

    /** Резервная копия в постоянных данных игрока — переживает любые пересоздания сущности. */
    public static final String BACKUP_TAG = "litrpg_backup";

    public static PlayerStats get(Player player) {
        return player.getCapability(StatsProvider.STATS).orElseGet(PlayerStats::new);
    }

    public static MutableComponent system() {
        return Component.literal("[Система] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    public static void message(ServerPlayer player, Component text) {
        player.sendSystemMessage(system().append(Component.empty().append(text).withStyle(ChatFormatting.WHITE)));
    }

    /** Игроки, чьи данные изменились за этот тик: отправляются один раз в конце тика. */
    private static final java.util.Set<ServerPlayer> DIRTY = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /**
     * Синхронизация с клиентом. За один тик данные могут меняться много раз
     * (опыт, репутация, задания за одно убийство) — пакет уходит только один.
     */
    public static void sync(ServerPlayer player) {
        DIRTY.add(player);
    }

    /** Немедленная отправка (например, перед открытием окна, которому нужны свежие данные). */
    public static void syncNow(ServerPlayer player) {
        DIRTY.remove(player);
        PlayerStats s = get(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncStatsPacket(s, player.level().getGameTime()));
    }

    /** Вызывается в конце каждого серверного тика. */
    public static void flushSync() {
        if (DIRTY.isEmpty()) return;
        ServerPlayer[] list = DIRTY.toArray(new ServerPlayer[0]);
        DIRTY.clear();
        for (ServerPlayer p : list) if (!p.hasDisconnected()) syncNow(p);
    }

    public static void clearSync() {
        DIRTY.clear();
    }

    /** Лёгкая синхронизация: только мана (раз в секунду при восстановлении). */
    public static void syncMana(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new com.andrey.litrpg.network.ManaPacket(get(player).getMana()));
    }

    public static void backup(ServerPlayer player) {
        backup(player, get(player));
    }

    public static void backup(ServerPlayer player, PlayerStats s) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.put(BACKUP_TAG, s.save());
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    /** Если capability пришла пустой, а резервная копия есть — восстанавливаем. */
    public static boolean restoreIfLost(ServerPlayer player) {
        PlayerStats s = get(player);
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(BACKUP_TAG)) return false;
        CompoundTag saved = persisted.getCompound(BACKUP_TAG);
        boolean looksEmpty = s.getLevel() <= 1 && s.getXp() == 0 && !s.hasOrigin();
        boolean backupBetter = saved.getInt("level") > 1 || saved.contains("race");
        if (looksEmpty && backupBetter) {
            s.load(saved);
            return true;
        }
        return false;
    }

    public static void applyAttributes(ServerPlayer player) {
        PlayerStats s = get(player);
        Race race = s.getRace();
        // все числа — из StatBreakdown (то же видит игрок в окне «Расчёт»)
        set(player, Attributes.MAX_HEALTH, HEALTH_ID, "litrpg_health", StatBreakdown.health(s).value() - 20, AttributeModifier.Operation.ADDITION);
        set(player, Attributes.ATTACK_DAMAGE, DAMAGE_ID, "litrpg_damage", StatBreakdown.weaponDamage(s).value(), AttributeModifier.Operation.ADDITION);
        set(player, Attributes.MOVEMENT_SPEED, SPEED_ID, "litrpg_speed", StatBreakdown.moveSpeed(s).value() / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
        set(player, Attributes.ATTACK_SPEED, ATK_SPEED_ID, "litrpg_attack_speed", StatBreakdown.attackSpeed(s).value() / 100.0,
                AttributeModifier.Operation.MULTIPLY_BASE);
        set(player, Attributes.ARMOR, ARMOR_ID, "litrpg_armor", StatBreakdown.armor(s).value(), AttributeModifier.Operation.ADDITION);
        set(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, "litrpg_knockback",
                race == Race.DWARF ? 0.35 : race == Race.ORC ? 0.2 : 0, AttributeModifier.Operation.ADDITION);
        set(player, Attributes.LUCK, LUCK_ID, "litrpg_luck", s.getStat(Stat.LUCK) * 0.1, AttributeModifier.Operation.ADDITION);

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
        s.setMana(s.getMana());
    }

    private static void set(ServerPlayer player, Attribute attr, UUID id, String name, double value,
                            AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) {
            inst.addPermanentModifier(new AttributeModifier(id, name, value, op));
        }
    }

    public static void giveXp(ServerPlayer player, int amount) {
        if (amount <= 0) return;
        PlayerStats s = get(player);
        int before = s.getLevel();
        int gained = s.addXp(amount);
        player.displayClientMessage(Component.literal("+" + amount + " опыта").withStyle(ChatFormatting.GREEN), true);
        if (gained > 0) {
            onLevelUp(player, before, s.getLevel());
        }
        sync(player);
    }

    public static void onLevelUp(ServerPlayer player, int from, int to) {
        PlayerStats s = get(player);
        applyAttributes(player);
        player.setHealth(player.getMaxHealth());
        s.setMana(s.getMaxMana());

        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("УРОВЕНЬ ПОВЫШЕН").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Уровень " + to + "  •  +" + (to - from) * PlayerStats.POINTS_PER_LEVEL + " очков характеристик")
                        .withStyle(ChatFormatting.YELLOW)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 0.8f);

        message(player, Component.literal("Поздравляем! Достигнут уровень ")
                .append(Component.literal(String.valueOf(to)).withStyle(ChatFormatting.GOLD))
                .append(". Свободных очков: " + s.getFreePoints() + " (окно персонажа: K)"));

        PlayerClass cls = s.getPlayerClass();
        java.util.List<String> keys = new java.util.ArrayList<>();
        if (cls == null) keys.addAll(java.util.List.of(Ability.NOVICE));
        else {
            keys.addAll(java.util.List.of(cls.skills));
            if (cls.isInvoker()) cls.invoke.combos().forEach(c -> keys.add(c.skill()));
        }
        for (String k : keys) {
            Ability a = Ability.byKey(k);
            int need = Ability.unlockLevel(cls, a);
            if (a != null && need > from && need <= to) {
                message(player, Component.literal("Открыт навык: ")
                        .append(Component.translatable(a.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE))
                        .append(" — ")
                        .append(Component.translatable(a.descKey())));
            }
        }
        if (cls != null && !cls.isInvoker()) {
            var left = s.fillBar();
            if (!left.isEmpty()) {
                message(player, Component.literal("Панель навыков заполнена. Новые навыки можно поставить вместо старых: K → «Навыки».")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (s.canChooseClass()) offerClasses(player);
    }

    // ---------- титулы ----------

    public static void count(ServerPlayer player, Title.Counter counter, int n) {
        PlayerStats s = get(player);
        s.addCounter(counter, n);
        for (Title t : Title.values()) {
            if (t.counter == counter && !s.hasTitle(t) && s.getCounter(counter) >= t.threshold) {
                grantTitle(player, t);
            }
        }
    }

    /** Разослать всем игрокам расы всех игроков (для внешности). */
    public static void broadcastRaces(net.minecraft.server.MinecraftServer server) {
        java.util.Map<java.util.UUID, Integer> map = new java.util.HashMap<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Race r = get(p).getRace();
            if (r != null) map.put(p.getUUID(), r.ordinal());
        }
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new com.andrey.litrpg.network.RacesPacket(map));
    }

    /** «+1 Сила, +2 Удача» — что даёт титул. */
    public static MutableComponent titleReward(Title t) {
        MutableComponent out = Component.empty();
        boolean first = true;
        for (Stat st : Stat.values()) {
            int v = t.bonus(st);
            if (v == 0) continue;
            if (!first) out.append(", ");
            out.append("+" + v + " ").append(Component.translatable(st.translationKey()));
            first = false;
        }
        return out;
    }

    public static void grantTitle(ServerPlayer player, Title t) {
        PlayerStats s = get(player);
        if (s.hasTitle(t)) return;
        s.addTitle(t);
        applyAttributes(player);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("ПОЛУЧЕН ТИТУЛ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("«").append(Component.translatable(t.nameKey())).append("»  ").withStyle(ChatFormatting.AQUA)
                        .append(titleReward(t).withStyle(ChatFormatting.GREEN))));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.0f);
        message(player, Component.literal("Получен титул ")
                .append(Component.literal("«").append(Component.translatable(t.nameKey())).append("»").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append(": ").append(Component.translatable(t.descKey())));
        message(player, Component.literal("Награда (навсегда): ").withStyle(ChatFormatting.GOLD).append(titleReward(t).withStyle(ChatFormatting.GREEN)));
        sync(player);
    }

    // ---------- раса и класс ----------

    public static void openOriginScreen(ServerPlayer player) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenOriginPacket());
    }

    public static void chooseOrigin(ServerPlayer player, Race race, boolean giveKit) {
        PlayerStats s = get(player);
        boolean first = !s.hasOrigin();
        s.setRace(race);
        if (first) com.andrey.litrpg.rep.RepEvents.initFor(player);
        broadcastRaces(player.server);
        applyAttributes(player);
        player.setHealth(player.getMaxHealth());
        s.setMana(s.getMaxMana());
        message(player, Component.literal("Раса выбрана: ")
                .append(Component.translatable(race.nameKey()).withStyle(ChatFormatting.GOLD)).append("."));
        if (first && giveKit) {
            giveStarterKit(player);
            message(player, Component.literal("Пока ты Новичок. До 10 уровня Система будет наблюдать за твоим стилем игры — "
                    + "сражайся, колдуй, стреляй, крадись или лечи, а на 10 уровне выбери один из трёх подходящих классов."));
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 1.2f);
        sync(player);
    }

    /** Выбор роли (в начале пути или у старых персонажей без роли). */
    public static void chooseRole(ServerPlayer player, Role role) {
        PlayerStats s = get(player);
        if (s.hasClass() || role == null) return;
        s.setRole(role);
        s.setOffers(java.util.List.of());
        message(player, Component.literal("Роль: ").append(Component.literal(role.title).withStyle(ChatFormatting.GOLD))
                .append(". На 10 уровне Система предложит классы этой роли."));
        applyAttributes(player);
        sync(player);
        if (s.canChooseClass()) offerClasses(player);
    }

    /** Сгенерировать (если ещё нет) предложения и открыть окно выбора класса. */
    public static void offerClasses(ServerPlayer player) {
        PlayerStats s = get(player);
        if (!s.canChooseClass()) return;
        if (s.getRole() == null) {
            openOriginScreen(player); // сначала роль
            return;
        }
        if (s.getOffers().isEmpty()) {
            s.setOffers(ClassOffers.generate(s, player.getRandom()));
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("ВЫБОР КЛАССА").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)));
            player.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("Система подобрала " + s.getOffers().size() + " классов под твой стиль").withStyle(ChatFormatting.GRAY)));
            for (PlayerClass c : s.getOffers()) {
                if (c.isHidden()) {
                    message(player, Component.literal("Обнаружен СКРЫТЫЙ класс: ").withStyle(ChatFormatting.LIGHT_PURPLE)
                            .append(Component.translatable(c.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
                }
            }
            message(player, Component.literal("Доступен выбор класса! Окно откроется само, а позже — клавиша K."));
            sync(player);
        }
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenClassChoicePacket());
    }

    public static void chooseClass(ServerPlayer player, PlayerClass cls, boolean giveKit) {
        PlayerStats s = get(player);
        boolean first = !s.hasClass();
        s.setPlayerClass(cls);
        s.setOffers(java.util.List.of());
        s.resetCooldowns();
        applyAttributes(player);
        player.setHealth(player.getMaxHealth());
        s.setMana(s.getMaxMana());
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable(cls.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Класс получен").withStyle(ChatFormatting.YELLOW)));
        message(player, Component.literal("Получен класс ")
                .append(Component.translatable(cls.nameKey()).withStyle(ChatFormatting.GOLD))
                .append(cls.isInvoker()
                        ? ". Z, X, C — сферы, V — Призыв (сплетает три сферы в заклинание), B и G — сплетённые заклинания."
                        : ". Восемь навыков открываются с 10 по 45 уровень; в бою работают только шесть на панели (Z X C V B G)."
                        + " Сменить их можно вне боя: K → «Навыки»."));
        if (first && giveKit) giveClassKit(player, cls);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        sync(player);
    }

    private static void giveClassKit(ServerPlayer player, PlayerClass cls) {
        int melee = cls.styles.getOrDefault(Style.MELEE, 0), ranged = cls.styles.getOrDefault(Style.RANGED, 0);
        int magic = cls.styles.getOrDefault(Style.MAGIC, 0), stealth = cls.styles.getOrDefault(Style.STEALTH, 0);
        if (ranged >= 3) {
            give(player, new ItemStack(Items.BOW));
            give(player, new ItemStack(Items.ARROW, 64));
        } else if (stealth >= 3) {
            give(player, new ItemStack(ModItems.ROGUE_DAGGER.get()));
        } else if (magic >= 2 && melee < 2) {
            give(player, new ItemStack(ModItems.APPRENTICE_STAFF.get()));
            give(player, new ItemStack(ModItems.MANA_POTION.get(), 4));
        } else {
            give(player, new ItemStack(ModItems.MANA_POTION.get(), 2));
        }
    }

    private static void giveStarterKit(ServerPlayer player) {
        give(player, new ItemStack(Items.STONE_SWORD));
        give(player, new ItemStack(Items.BOW));
        give(player, new ItemStack(Items.ARROW, 48));
        give(player, new ItemStack(Items.LEATHER_CHESTPLATE));
        give(player, new ItemStack(Items.BREAD, 12));
        give(player, new ItemStack(Items.TORCH, 16));
        giveCodex(player);
    }

    public static void giveCodex(ServerPlayer player) {
        ItemStack book = com.andrey.litrpg.compat.Codex.create();
        if (!book.isEmpty()) give(player, book);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
