package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Minions;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Хардкорные события мира:
 *  — Кровавая луна: каждую 7-ю ночь и с шансом 12% в любую другую. Монстры на 4 уровня сильнее,
 *    элиты втрое чаще, и почти все приходят стаями;
 *  — Стаи: в опасных зонах ночные монстры появляются группами.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class BloodMoon {
    private static final String PACK_TAG = "litrpg_pack";

    private BloodMoon() {}

    public static boolean isActive(ServerLevel level) {
        return level.dimension() == Level.OVERWORLD && level.isNight() && Data.get(level).active;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD || level.getGameTime() % 40 != 0) return;
        Data data = Data.get(level);
        long day = level.getDayTime() / 24000L;
        long time = level.getDayTime() % 24000L;
        // решение принимается один раз на закате
        if (time >= 12500 && time < 13500 && data.decidedDay != day) {
            data.decidedDay = day;
            data.active = day > 0 && (day % 7 == 6 || level.random.nextFloat() < 0.12f);
            data.setDirty();
            if (data.active) announce(level);
        }
        if (data.active && !level.isNight()) {
            data.active = false;
            data.setDirty();
            for (ServerPlayer p : level.players()) {
                StatsManager.message(p, Component.literal("Кровавая луна зашла. Вы пережили эту ночь.").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Для тестов: начать Кровавую луну прямо сейчас (ставит ночь). */
    public static void force(ServerLevel level) {
        ServerLevel ow = level.getServer().overworld();
        long day = ow.getDayTime() / 24000L;
        ow.setDayTime(day * 24000L + 13500);
        Data data = Data.get(ow);
        data.decidedDay = day;
        data.active = true;
        data.setDirty();
        announce(ow);
    }

    private static void announce(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(20, 80, 30));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("КРОВАВАЯ ЛУНА").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(
                    Component.literal("Монстры сильнее, элиты чаще, и они идут стаями").withStyle(ChatFormatting.RED)));
            StatsManager.message(p, Component.literal("ВНИМАНИЕ: восходит Кровавая луна. Ищите укрытие или готовьтесь к бою.").withStyle(ChatFormatting.RED));
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.5f, 0.6f);
        }
    }

    /** Стаи: дополнительные монстры рядом с естественно появившимся. */
    @SubscribeEvent
    public static void onSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) return;
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob.getPersistentData().getBoolean(PACK_TAG) || Minions.ownerOf(mob) != null) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Zone zone = Zone.at(level, mob.blockPosition());
        boolean moon = isActive(level);
        float chance = moon ? 0.7f : zone.ordinal() >= Zone.WILDWOOD.ordinal() && zone.ordinal() <= Zone.ABYSS.ordinal()
                ? 0.12f + 0.06f * (zone.ordinal() - Zone.WILDWOOD.ordinal()) : 0f;
        if (level.random.nextFloat() >= chance) return;
        int extra = 1 + level.random.nextInt(moon ? 3 : 2);
        EntityType<?> type = mob.getType();
        BlockPos origin = mob.blockPosition();
        // добавляем на следующем тике, чтобы не вмешиваться в текущий спавн
        com.andrey.litrpg.util.Scheduler.later(1, () -> {
            for (int i = 0; i < extra; i++) {
                BlockPos at = origin.offset(level.random.nextInt(5) - 2, 0, level.random.nextInt(5) - 2);
                if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
                if (!(type.create(level) instanceof Mob m)) continue;
                m.getPersistentData().putBoolean(PACK_TAG, true);
                m.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360, 0);
                m.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.NATURAL, null, null);
                level.addFreshEntity(m);
            }
        });
    }

    /** Состояние луны хранится в сохранении мира. */
    public static final class Data extends SavedData {
        long decidedDay = -1;
        boolean active = false;

        static Data get(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(Data::load, Data::new, "litrpg_blood_moon");
        }

        static Data load(CompoundTag tag) {
            Data d = new Data();
            d.decidedDay = tag.getLong("day");
            d.active = tag.getBoolean("active");
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putLong("day", decidedDay);
            tag.putBoolean("active", active);
            return tag;
        }
    }
}
