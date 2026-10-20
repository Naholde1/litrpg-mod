package com.andrey.litrpg.city;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.npc.NpcType;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.stats.Race;
import com.andrey.litrpg.util.Scheduler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Реплики жителей: облачко с текстом над головой (TextDisplay, едет на жителе)
 * на несколько секунд. Плюс имена мастеров по расам.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class Speech {
    public static final String BUBBLE_TAG = "litrpg_bubble";

    private Speech() {}

    /** Показать реплику над жителем на ticks тиков. */
    public static void say(Mob who, String text, int ticks) {
        if (!(who.level() instanceof ServerLevel sl) || !who.isAlive()) return;
        for (Entity p : who.getPassengers()) if (p.getTags().contains(BUBBLE_TAG)) p.discard();
        Display.TextDisplay d = EntityType.TEXT_DISPLAY.create(sl);
        if (d == null) return;
        CompoundTag t = new CompoundTag();
        t.putString("text", Component.Serializer.toJson(Component.literal(text)));
        t.putString("billboard", "center");
        t.putInt("line_width", 160);
        t.putInt("background", 0x90101820);
        t.putByte("shadow", (byte) 1);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, (float) (who.getBbHeight() * 0.32 + 0.45), 0));
        tr.put("scale", floats(0.55f, 0.55f, 0.55f));
        t.put("transformation", tr);
        try {
            d.load(withPos(t, who));
        } catch (Exception e) {
            LitRPG.LOGGER.warn("speech bubble failed", e);
            return;
        }
        d.addTag(BUBBLE_TAG);
        if (!sl.addFreshEntity(d)) return;
        d.startRiding(who, true);
        Scheduler.later(ticks, () -> {
            if (d.isAlive()) d.discard();
        });
    }

    private static CompoundTag withPos(CompoundTag t, Entity at) {
        ListTag pos = new ListTag();
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.getX()));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.getY() + at.getBbHeight()));
        pos.add(net.minecraft.nbt.DoubleTag.valueOf(at.getZ()));
        t.put("Pos", pos);
        return t;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }

    /** Облачка не должны переживать перезапуск сервера. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Display.TextDisplay d && d.getTags().contains(BUBBLE_TAG)
                && event.loadedFromDisk()) {
            event.setCanceled(true);
        }
    }

    // ---------------- что говорят ----------------

    private static final String[] CITIZEN = {
            "Слыхал? У кузнеца новые клинки.", "Стража сегодня злая — не балуй.", "Опять цены на хлеб подняли…",
            "Говорят, за стенами видели тролля.", "В таверне сегодня поют!", "Хорошая погода для ярмарки.",
            "Моя тётка видела Систему во сне.", "Только не ломай ничего — строители обидятся.", "Ты из искателей? Удачи тебе.",
            "Ночью из леса слышен вой…", "У травницы лучший чай в городе.", "Кто-то опять оставил ворота открытыми.",
            "Говорят, в старых руинах ещё есть сокровища.", "Не ходи ночью без факела.", "Мой брат ушёл с караваном и не вернулся."};
    private static final String[] GUARD = {
            "Проходи, не задерживайся.", "Порядок в городе — закон.", "Слежу за тобой, искатель.", "Нарушишь закон — пожалеешь.",
            "Спокойной службы не бывает.", "За стенами опасно. Держись дорог."};
    private static final String[] ORC_EXTRA = {"Сила — вот закон!", "Хорошая драка — лучший праздник.", "Мясо! Нужно больше мяса!"};
    private static final String[] ELF_EXTRA = {"Лес шепчет о переменах.", "Луна сегодня особенно ясна.", "Не руби живые деревья, чужак."};
    private static final String[] DWARF_EXTRA = {"Камень помнит всё.", "Хорошее пиво и крепкий молот — вот счастье.", "Гоблины опять в шахтах…"};
    private static final String[] BEAST_EXTRA = {"Чую грозу.", "Стая сильна, пока вместе.", "Добрая охота нынче."};
    private static final String[] DEMON_EXTRA = {"Пепел помнит прежний мир.", "Жар — это жизнь.", "Не смотри мне в глаза слишком долго."};

    public static String line(RaceNpcEntity npc, RandomSource r) {
        NpcType t = npc.npcType();
        if (t.isArtisan() && npc.craft() != null && r.nextInt(4) != 0) return pick(npc.craft().phrases, r);
        if (t.isGuard()) return pick(GUARD, r);
        if (r.nextInt(3) == 0) {
            String[] extra = switch (t.race) {
                case ORC -> ORC_EXTRA;
                case ELF -> ELF_EXTRA;
                case DWARF -> DWARF_EXTRA;
                case BEASTKIN -> BEAST_EXTRA;
                case DEMONKIN -> DEMON_EXTRA;
                default -> null;
            };
            if (extra != null) return pick(extra, r);
        }
        return pick(CITIZEN, r);
    }

    private static String pick(String[] a, RandomSource r) {
        return a[r.nextInt(a.length)];
    }

    // ---------------- имена ----------------

    private static final String[] HUMAN_M = {"Гаррет", "Освин", "Бертран", "Томас", "Элрик", "Мартин", "Годрик", "Ульрих", "Роланд", "Седрик", "Бенедикт", "Ансельм"};
    private static final String[] HUMAN_F = {"Мира", "Элоиза", "Агата", "Беатрис", "Ровена", "Марта", "Хильда", "Ирма", "Лисбет", "Аделина"};
    private static final String[] ELF_M = {"Аэлин", "Тауриэль", "Лиандир", "Фаэрон", "Элеандор", "Силваэн", "Каэлис", "Ирвандил"};
    private static final String[] ELF_F = {"Иллиэна", "Наэрис", "Лаэль", "Сильвара", "Аэрин", "Миэлла", "Таэлия"};
    private static final String[] DWARF_M = {"Торин", "Балин", "Гримли", "Двалин", "Бруни", "Харгрим", "Онар", "Дурин", "Кхарн", "Брок"};
    private static final String[] DWARF_F = {"Дагна", "Хельга", "Брунхильд", "Тора", "Гуннхильд", "Рагна"};
    private static final String[] ORC_M = {"Гром", "Урзаг", "Морк", "Гаррош", "Скарг", "Тхрок", "Бругг", "Нарзуг", "Дурга"};
    private static final String[] ORC_F = {"Шагра", "Ургха", "Гхола", "Мазра", "Ракха"};
    private static final String[] BEAST_M = {"Серый Клык", "Быстрая Лапа", "Рррауг", "Тёмная Грива", "Острый Нюх", "Буран"};
    private static final String[] BEAST_F = {"Белая Лань", "Мягкая Лапа", "Рыжая Тень", "Тихий Шаг", "Звонкий Ручей"};
    private static final String[] DEMON_M = {"Азгор", "Мальфас", "Зарвен", "Кассиэль", "Бельзар", "Оркус", "Вексар"};
    private static final String[] DEMON_F = {"Лилит", "Азара", "Сецира", "Морвена", "Иззара", "Кхалисса"};

    public static String name(Race race, boolean female, RandomSource r) {
        String[] a = switch (race) {
            case ELF -> female ? ELF_F : ELF_M;
            case DWARF -> female ? DWARF_F : DWARF_M;
            case ORC -> female ? ORC_F : ORC_M;
            case BEASTKIN -> female ? BEAST_F : BEAST_M;
            case DEMONKIN -> female ? DEMON_F : DEMON_M;
            default -> female ? HUMAN_F : HUMAN_M;
        };
        return pick(a, r);
    }
}
