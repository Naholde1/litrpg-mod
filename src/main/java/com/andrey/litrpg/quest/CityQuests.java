package com.andrey.litrpg.quest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.city.Speech;
import com.andrey.litrpg.entity.RatEntity;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.npc.Craft;
import com.andrey.litrpg.npc.RaceNpcEntity;
import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.world.map.SystemWorld;
import com.andrey.litrpg.world.map.WorldMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Поручения горожан: у каждого мастера свои — по его ремеслу и о жизни города.
 * Отнести посылку другому мастеру, выгнать крыс из погреба, развести свиней на
 * заднем дворе, принести материалы, добраться до деревни с письмом, проучить
 * разбойников на дороге. Предложения мастера обновляются каждый игровой день.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class CityQuests {
    private static final String PEST_TAG = "litrpg_pest";
    private static final int MAX_BOARDS = 16;

    private CityQuests() {}

    public static String boardKey(RaceNpcEntity npc) {
        return "npc:" + npc.getUUID();
    }

    /** Предложения мастера (раз в игровой день). */
    public static void ensureOffers(ServerPlayer p, RaceNpcEntity npc) {
        QuestLog log = StatsManager.get(p).quests();
        String key = boardKey(npc);
        long day = QuestEvents.day(p);
        if (log.offersDay.getOrDefault(key, -1L) == day) return;
        log.offers.put(key, generate(p, npc, p.getRandom()));
        log.offersDay.put(key, day);
        // старые доски мастеров не копим
        List<String> boards = new ArrayList<>();
        for (String k : log.offersDay.keySet()) if (k.startsWith("npc:")) boards.add(k);
        if (boards.size() > MAX_BOARDS) {
            boards.sort((a, b) -> Long.compare(log.offersDay.get(a), log.offersDay.get(b)));
            for (int i = 0; i < boards.size() - MAX_BOARDS; i++) {
                log.offersDay.remove(boards.get(i));
                log.offers.remove(boards.get(i));
            }
        }
    }

    // ===================================================================
    //                          генерация
    // ===================================================================

    /** Контекст генерации: кто даёт, где, какого уровня игрок. */
    private static final class Ctx {
        ServerPlayer p;
        RaceNpcEntity npc;
        RandomSource r;
        QuestLog log;
        int level;
        WorldMap.City city;
        Faction faction;
        String giver;
        BlockPos home;
    }

    private static List<Quest> generate(ServerPlayer p, RaceNpcEntity npc, RandomSource r) {
        Ctx c = new Ctx();
        c.p = p;
        c.npc = npc;
        c.r = r;
        PlayerStats s = StatsManager.get(p);
        c.log = s.quests();
        c.city = SystemWorld.isOverworld(npc.level()) ? WorldMap.get().cityAt(npc.getX(), npc.getZ()) : null;
        c.level = Math.max(1, Math.max(s.getLevel(), c.city == null ? 1 : c.city.level()));
        c.faction = npc.npcType().faction;
        String full = npc.getName().getString();
        c.giver = full.contains(",") ? full.substring(0, full.indexOf(',')) : full;
        c.home = npc.hasRestriction() ? npc.getRestrictCenter() : npc.blockPosition();
        Craft craft = npc.craft();
        List<Supplier<Quest>> opts = new ArrayList<>(options(c, craft == null ? Craft.FARMER : craft));
        Collections.shuffle(opts, new java.util.Random(r.nextLong()));
        List<Quest> out = new ArrayList<>();
        for (Supplier<Quest> o : opts) {
            if (out.size() >= 3) break;
            Quest q = o.get();
            if (q == null) continue;
            boolean dup = false;
            for (Quest e : out) dup |= e.type == q.type && e.target.equals(q.target);
            if (!dup) out.add(q);
        }
        return out;
    }

    private static List<Supplier<Quest>> options(Ctx c, Craft craft) {
        int L = c.level;
        List<Supplier<Quest>> o = new ArrayList<>();
        switch (craft) {
            case BLACKSMITH -> {
                o.add(() -> collect(c, "minecraft:iron_ingot", 12, "Железо для горна", "Заказов много, а железа мало. Принеси слитки — заплачу честно."));
                o.add(() -> collect(c, "minecraft:coal", 32, "Уголь для горна", "Горн жрёт уголь, как дракон. Без угля город останется без гвоздей и подков."));
                o.add(() -> deliver(c, Craft.STABLEMASTER, "Подковы для конюшни", "Отнеси эти подковы, пока кони не охромели."));
                o.add(() -> deliver(c, Craft.WEAPONSMITH, "Заготовки клинков", "Тут заготовки для оружейника. Тяжёлые — смотри не урони."));
                o.add(() -> hunt(c, QuestTarget.CREEPER, 6, "Взрывоопасные гости", "Криперы бродят у складов угля. Если рванёт — полгорода сгорит!"));
                if (L >= 5) o.add(() -> collect(c, "minecraft:raw_iron", 16, "Руда с холмов", "Хорошая руда — половина хорошего клинка. Добудь сырого железа."));
            }
            case ARMORER -> {
                o.add(() -> collect(c, "minecraft:leather", 12, "Ремни для доспехов", "Латы без ремней — просто куча железа. Нужна добрая кожа."));
                o.add(() -> collect(c, "minecraft:iron_ingot", 16, "Металл для кольчуг", "Каждое кольцо кольчуги — из железа. А колец тысячи."));
                o.add(() -> deliver(c, Craft.BLACKSMITH, "Заказ у кузнеца", "Отнеси кузнецу мерки и задаток — он скуёт пластины."));
                o.add(() -> hunt(c, QuestTarget.SKELETON, 10, "Испытание лат", "Новые латы надо испытать. Скелеты стреляют метко — проверь на них."));
                if (L >= 15) o.add(() -> hunt(c, QuestTarget.ELITE, 2, "Достойный противник", "Покажи, на что способна моя броня: одолей элитного зверя."));
            }
            case WEAPONSMITH -> {
                o.add(() -> collect(c, "minecraft:iron_ingot", 10, "Сталь для клинков", "Клинку нужна сталь. Принеси железа."));
                o.add(() -> collect(c, "minecraft:flint", 12, "Кремень для точила", "Точильный круг стёрся. Нужен кремень."));
                o.add(() -> hunt(c, QuestTarget.ZOMBIE, 12, "Испытание клинка", "Лучшая проверка меча — бой. Мертвецы за стенами подойдут."));
                o.add(() -> deliver(c, Craft.BLACKSMITH, "Сломанные мечи", "Отнеси кузнецу сломанные мечи стражи — пусть перекуёт."));
                if (L >= 8) o.add(() -> hunt(c, QuestTarget.BANDIT, 5, "Разбойники на тракте", "Разбойники грабят моих покупателей на дорогах. Проучи их."));
                if (L >= 15) o.add(() -> collect(c, "minecraft:diamond", 2, "Алмазная кромка", "Для особого заказа нужны алмазы. Немного, но настоящие."));
            }
            case JEWELER -> {
                o.add(() -> collect(c, "minecraft:amethyst_shard", 12, "Аметистовые осколки", "Аметист хорошо держит силу Системы. Добудь осколков в жеодах."));
                o.add(() -> collect(c, "minecraft:gold_ingot", 6, "Золото для оправ", "Без золота кольцо — просто колечко. Принеси слитков."));
                o.add(() -> deliver(c, Craft.BANKER, "Ожерелье для менялы", "Это ожерелье заказала торговая палата. Неси осторожно!"));
                o.add(() -> visit(c, new String[]{"ruins", "outpost"}, "Потерянное кольцо", "Мой подмастерье обронил кольцо с печатью где-то у %s. Найди место!"));
                if (L >= 8) o.add(() -> collect(c, "minecraft:lapis_lazuli", 16, "Лазурит", "Лазурь нужна для оберегов от сглаза."));
                if (L >= 15) o.add(() -> collect(c, "minecraft:diamond", 2, "Камень для короны", "Знатный заказчик хочет алмазы. Помоги — не пожалеешь."));
            }
            case TAILOR -> {
                o.add(() -> collect(c, "minecraft:white_wool", 24, "Шерсть для сукна", "Зима близко, а шерсти нет. Остриги овец или купи у фермеров."));
                o.add(() -> collect(c, "minecraft:string", 16, "Нити", "Нужны крепкие нити. Пауки плетут лучшие."));
                o.add(() -> collect(c, "minecraft:blue_dye", 8, "Синяя краска", "Знать любит синее. Добудь краски."));
                o.add(() -> deliver(c, Craft.INNKEEPER, "Новые скатерти", "Отнеси в таверну новые скатерти и занавески."));
                o.add(() -> pests(c, 6, "Крысы на складе тканей", "Крысы прогрызли рулоны сукна! Выгони их со склада, пока не съели всё."));
            }
            case HERBALIST -> {
                o.add(() -> collect(c, "minecraft:poppy", 12, "Маки для настоек", "Маки — от бессонницы и боли. Набери на лугах."));
                o.add(() -> collect(c, "minecraft:dandelion", 12, "Одуванчики", "Одуванчиковый отвар чистит кровь. Собери цветов."));
                o.add(() -> collect(c, "minecraft:sweet_berries", 16, "Сладкие ягоды", "Ягоды для зимних сборов. Ищи в тайге."));
                o.add(() -> collect(c, "minecraft:red_mushroom", 8, "Красные грибы", "Красные грибы ядовиты — но в малых дозах лечат."));
                o.add(() -> deliver(c, Craft.HEALER, "Травы для лечебницы", "Отнеси этот сбор лекарю — раненых снова много."));
                o.add(() -> deliver(c, Craft.ALCHEMIST, "Корешки для алхимика", "Алхимик заказывал корень мандрагоры. Неси, только не нюхай."));
                o.add(() -> visit(c, new String[]{"village", "farm", "lumber", "hamlet"}, "Мази для деревни", "В %s хворает скот. Отнеси туда мази — путь неблизкий."));
                if (L >= 5) o.add(() -> collect(c, "minecraft:glow_berries", 8, "Светящиеся ягоды", "Светоягоды растут в пещерах. Говорят, от них видишь в темноте."));
            }
            case ALCHEMIST -> {
                o.add(() -> collect(c, "minecraft:spider_eye", 8, "Паучьи глаза", "Для ослабляющих зелий нужны паучьи глаза. Свежие!"));
                o.add(() -> collect(c, "minecraft:gunpowder", 10, "Порох", "Порох для… экспериментов. Не задавай вопросов."));
                o.add(() -> hunt(c, QuestTarget.SLIME, 8, "Слизь для зелий", "Слизни — ценное сырьё. Добудь их в болотах."));
                o.add(() -> deliver(c, Craft.HEALER, "Зелья для раненых", "Отнеси лекарю ящик зелий лечения. Аккуратно — стекло!"));
                if (L >= 4) o.add(() -> collect(c, "minecraft:slime_ball", 8, "Слизь", "Слизь нужна для клейких зелий."));
                if (L >= 8) o.add(() -> collect(c, "minecraft:glowstone_dust", 12, "Светокаменная пыль", "Пыль из Преисподней усиливает зелья."));
                if (L >= 10) o.add(() -> hunt(c, QuestTarget.WITCH, 2, "Конкурентки с болот", "Болотные ведьмы портят мне торговлю своими отварами. Разберись с ними."));
                if (L >= 18) o.add(() -> collect(c, "minecraft:blaze_powder", 4, "Огненный порошок", "Огненный порошок — сердце любого сильного зелья."));
            }
            case BAKER -> {
                o.add(() -> collect(c, "minecraft:wheat", 32, "Пшеница для муки", "Мельница стоит — пшеницы нет. Принеси снопов."));
                o.add(() -> collect(c, "minecraft:egg", 12, "Яйца для пирогов", "Пироги без яиц не поднимутся. Набери в курятниках."));
                o.add(() -> collect(c, "minecraft:sugar", 16, "Сахар", "Сладкие булки — гордость пекарни. Нужен сахар."));
                o.add(() -> pests(c, 7, "Крысы в амбаре", "Крысы жрут муку быстрее, чем я пеку! Выгони их из амбара."));
                o.add(() -> deliver(c, Craft.INNKEEPER, "Хлеб для таверны", "Отнеси в таверну корзину свежего хлеба, пока горячий."));
                o.add(() -> collect(c, "minecraft:bread", 12, "Помощь у печи", "Не успеваю! Испеки хлеба и принеси мне — продам на ярмарке."));
            }
            case BUTCHER -> {
                o.add(() -> breed(c, "minecraft:pig", 4, "Свиньи на заднем дворе", "Стадо поредело. Разведи свиней — морковь в помощь."));
                o.add(() -> breed(c, "minecraft:cow", 3, "Новые телята", "Коровам нужен присмотр. Разведи пару телят."));
                o.add(() -> collect(c, "minecraft:porkchop", 12, "Свинина", "Заказ на праздник — нужна свинина."));
                o.add(() -> hunt(c, QuestTarget.WOLF, 6, "Волки режут скот", "Волки задрали трёх овец за стенами. Отгони стаю."));
                o.add(() -> deliver(c, Craft.INNKEEPER, "Мясо для таверны", "Отнеси трактирщику окорок — он заказывал к ужину."));
            }
            case FISHMONGER -> {
                o.add(() -> collect(c, "minecraft:cod", 12, "Треска", "Лодки вернулись пустыми. Налови трески."));
                o.add(() -> collect(c, "minecraft:salmon", 8, "Лосось", "Лосось идёт на нерест — самое время."));
                o.add(() -> collect(c, "minecraft:ink_sac", 8, "Чернила спрутов", "Писарь платит за чернильные мешки. Спруты водятся в глубине."));
                o.add(() -> visit(c, new String[]{"fishing"}, "Улов из посёлка", "Рыбаки из %s задерживают улов. Сходи узнай, что стряслось."));
                o.add(() -> deliver(c, Craft.INNKEEPER, "Рыба к ужину", "Отнеси трактирщику корзину рыбы."));
                o.add(() -> collect(c, "minecraft:tropical_fish", 3, "Диковинная рыба", "Знатный гость хочет тропическую рыбу. Задача не из лёгких!"));
            }
            case INNKEEPER -> {
                o.add(() -> breed(c, "minecraft:pig", 4, "Свиньи на заднем дворе таверны", "Окорока кончаются! Разведи свиней на заднем дворе — морковь там же."));
                o.add(() -> pests(c, 6, "Крысы в погребе", "В погребе завелись крысы — грызут бочки и пугают служанок. Выгони их!"));
                o.add(() -> breed(c, "minecraft:chicken", 4, "Курятник за таверной", "Постояльцы хотят яичницу. Разведи кур."));
                o.add(() -> collect(c, "minecraft:cooked_porkchop", 10, "Ужин для постояльцев", "Кухарка слегла. Приготовь жареной свинины на всех."));
                o.add(() -> collect(c, "minecraft:pumpkin_pie", 6, "Тыквенные пироги", "Праздник урожая! Нужны тыквенные пироги."));
                o.add(() -> deliver(c, Craft.BUTCHER, "Список заказа", "Отнеси мяснику список заказа на неделю."));
                if (L >= 8) o.add(() -> hunt(c, QuestTarget.BANDIT, 4, "Неоплаченный счёт", "Разбойники пили в долг и сбежали на большую дорогу. Проучи их!"));
            }
            case BUILDER -> {
                o.add(() -> collect(c, "minecraft:stone_bricks", 48, "Камень для стен", "Стены латать нечем. Принеси каменных кирпичей."));
                o.add(() -> collect(c, "minecraft:oak_log", 32, "Брёвна для лесов", "Нужны брёвна для строительных лесов."));
                o.add(() -> collect(c, "minecraft:glass", 16, "Стекло для окон", "Какой-то хулиган перебил окна. Нужно стекло."));
                o.add(() -> hunt(c, QuestTarget.CREEPER, 5, "Кто взрывает стены?", "Криперы подрывают городские стены по ночам. Найди и обезвредь!"));
                o.add(() -> visit(c, new String[]{"outpost", "village"}, "Осмотр заставы", "Нужно осмотреть постройки в %s — говорят, крыша прохудилась."));
                o.add(() -> deliver(c, Craft.MINER, "Заказ камня", "Отнеси рудокопу заказ на камень для новой башни."));
            }
            case LEATHERWORKER -> {
                o.add(() -> collect(c, "minecraft:leather", 16, "Сырая кожа", "Без кожи нет ни сёдел, ни ремней."));
                o.add(() -> collect(c, "minecraft:rabbit_hide", 8, "Кроличьи шкурки", "Мягкие шкурки для перчаток."));
                o.add(() -> hunt(c, QuestTarget.WOLF, 5, "Волчьи шкуры", "Волчий мех ценится. Только не приноси мне волков живьём."));
                o.add(() -> deliver(c, Craft.STABLEMASTER, "Новое седло", "Отнеси конюху седло — заказ готов."));
                o.add(() -> deliver(c, Craft.ARMORER, "Кожаные подкладки", "Броннику нужны подкладки под латы."));
            }
            case FLETCHER -> {
                o.add(() -> collect(c, "minecraft:feather", 16, "Перья для стрел", "Без перьев стрела летит куда хочет."));
                o.add(() -> collect(c, "minecraft:flint", 12, "Кремнёвые наконечники", "Кремень — лучший наконечник для охотничьих стрел."));
                o.add(() -> collect(c, "minecraft:string", 12, "Тетивы", "Тетивы рвутся — нужны нити."));
                o.add(() -> breed(c, "minecraft:chicken", 4, "Свои перья", "Хочу свой курятник — перья нужны всегда."));
                o.add(() -> hunt(c, QuestTarget.SKELETON, 10, "Чужие стрелы", "Скелеты стреляют моими же стрелами! Разберись с ними."));
                if (L >= 8) o.add(() -> hunt(c, QuestTarget.PHANTOM, 3, "Ночные тени", "Фантомы не дают спать дозорным. Сбей парочку."));
            }
            case SCRIBE -> {
                o.add(() -> visit(c, new String[]{"city"}, "Письмо в далёкий город", "Это письмо нужно доставить в %s. Дорога долгая — будь осторожен."));
                o.add(() -> visit(c, new String[]{"village", "fishing", "lumber", "hamlet"}, "Весточка в деревню", "Отнеси письмо в %s — там ждут вестей от родни."));
                o.add(() -> collect(c, "minecraft:paper", 24, "Бумага", "Книги не пишутся на воздухе. Принеси бумаги."));
                o.add(() -> collect(c, "minecraft:ink_sac", 8, "Чернила", "Чернила кончились. Спруты водятся в реках и морях."));
                o.add(() -> deliver(c, Craft.BANKER, "Купчие для палаты", "Отнеси меняле купчие грамоты. Печати не сломай!"));
            }
            case FARMER -> {
                o.add(() -> breed(c, "minecraft:sheep", 3, "Ягнята", "Овцам нужен пригляд. Разведи пару ягнят."));
                o.add(() -> breed(c, "minecraft:cow", 3, "Телята", "Коровы заскучали. Разведи телят — пшеница в помощь."));
                o.add(() -> collect(c, "minecraft:carrot", 24, "Морковь", "Не успеваю собрать урожай! Принеси моркови."));
                o.add(() -> collect(c, "minecraft:potato", 24, "Картофель", "Картошку пора копать. Помоги!"));
                o.add(() -> pests(c, 6, "Крысы в амбаре", "Крысы в амбаре! Жрут зерно и пугают кур."));
                o.add(() -> hunt(c, QuestTarget.ZOMBIE, 8, "Нежить у полей", "По ночам у полей бродят мертвецы. Защити урожай."));
            }
            case HEALER -> {
                o.add(() -> collect(c, "minecraft:honey_bottle", 4, "Мёд для лекарств", "Мёд заживляет раны. Принеси бутылочки мёда."));
                o.add(() -> hunt(c, QuestTarget.UNDEAD, 12, "Покой для мёртвых", "Нежить ранит людей каждую ночь. Упокой их."));
                o.add(() -> hunt(c, QuestTarget.SPIDER, 8, "Яд для противоядия", "Чтобы сварить противоядие, нужен яд. Пауки им богаты."));
                o.add(() -> deliver(c, Craft.HERBALIST, "Рецепт для травницы", "Отнеси травнице мой рецепт — пусть соберёт травы."));
                if (L >= 5) o.add(() -> collect(c, "minecraft:glistering_melon_slice", 3, "Сверкающий арбуз", "Для зелья исцеления нужен сверкающий арбуз."));
                if (L >= 8) o.add(() -> collect(c, "minecraft:golden_carrot", 4, "Золотая морковь", "Золотая морковь возвращает зрение. Пациент ждёт."));
            }
            case BANKER -> {
                o.add(() -> collect(c, "minecraft:gold_ingot", 8, "Золотой запас", "Палате нужен золотой запас. Принеси слитков — заплачу монетой."));
                o.add(() -> collect(c, "minecraft:emerald", 4, "Изумруды для обмена", "Изумруды — валюта людей. Нужно немного для расчётов."));
                o.add(() -> hunt(c, QuestTarget.BANDIT, 6, "Ограбленный караван", "Разбойники ограбили наш караван. Верни честь торговой палаты!"));
                o.add(() -> visit(c, new String[]{"caravan"}, "Проверка каравана", "Караван у %s давно молчит. Узнай, всё ли в порядке."));
                o.add(() -> visit(c, new String[]{"city"}, "Векселя", "Векселя нужно доставить в %s. Доверяю только тебе."));
                o.add(() -> deliver(c, Craft.JEWELER, "Заказ ювелиру", "Отнеси ювелиру мешочек с камнями. Не открывай!"));
            }
            case STABLEMASTER -> {
                o.add(() -> breed(c, "minecraft:horse", 2, "Жеребята", "Нужны молодые кони для стражи. Разведи лошадей — золотые морковки помогут."));
                o.add(() -> collect(c, "minecraft:hay_block", 8, "Сено", "Сено на исходе, а зима близко."));
                o.add(() -> collect(c, "minecraft:apple", 8, "Яблоки для коней", "Кони обожают яблоки. Набери в садах."));
                o.add(() -> visit(c, new String[]{"farm", "village", "ruins", "hamlet"}, "Сбежавшая кобыла", "Моя лучшая кобыла сбежала. Говорят, её видели у %s."));
                o.add(() -> hunt(c, QuestTarget.WOLF, 5, "Волки у пастбища", "Волки пугают табун. Отгони их."));
            }
            case MINER -> {
                o.add(() -> collect(c, "minecraft:coal", 32, "Уголь", "Шахта даёт мало угля. Помоги добыть."));
                o.add(() -> collect(c, "minecraft:raw_copper", 24, "Медная руда", "Медь нужна для громоотводов и котлов."));
                o.add(() -> hunt(c, QuestTarget.SPIDER, 10, "Пауки в шахте", "В старой шахте завелись пауки. Шахтёры боятся спускаться."));
                o.add(() -> visit(c, new String[]{"ruins"}, "Старая шахта", "У %s есть старый вход в шахту. Разведай дорогу."));
                o.add(() -> deliver(c, Craft.BLACKSMITH, "Руда для кузницы", "Отнеси кузнецу образцы новой руды."));
                if (L >= 5) o.add(() -> collect(c, "minecraft:raw_iron", 12, "Железная руда", "Железная жила иссякла. Найди новую."));
            }
        }
        return o;
    }

    // ---------------- типы поручений ----------------

    private static Quest base(Ctx c, String title, String desc) {
        Quest q = new Quest();
        q.id = "q" + c.log.nextId++;
        q.source = Quest.Source.CITY;
        q.faction = c.faction;
        q.title = title;
        q.desc = "«" + desc + "» — " + c.giver;
        q.giver = c.giver;
        if (c.city != null) {
            q.place = c.city.id();
            q.placeName = c.city.name();
        }
        return q;
    }

    private static void reward(Ctx c, Quest q, float mult) {
        int L = c.level;
        q.xp = Math.round((50 + L * 26) * mult);
        q.coins = Math.round((8 + L * 3.5f) * mult);
        q.rep = 10 + c.r.nextInt(11);
        if (c.r.nextFloat() < 0.35f) {
            Rarity rr = GearGen.rollRarity(c.r, 0.4f + L * 0.006f, false);
            if (rr == Rarity.COMMON) rr = Rarity.UNCOMMON;
            q.gearRarity = rr.ordinal();
            q.gearLevel = L;
        } else if (c.npc.craft() != null && c.r.nextBoolean()) {
            String[] bonus = bonus(c.npc.craft());
            q.bonusItem = bonus[0];
            q.bonusCount = Integer.parseInt(bonus[1]);
        }
    }

    /** Подарок мастера из его товаров. */
    private static String[] bonus(Craft craft) {
        return switch (craft) {
            case BLACKSMITH, ARMORER, WEAPONSMITH -> new String[]{"minecraft:iron_ingot", "6"};
            case JEWELER -> new String[]{"minecraft:emerald", "2"};
            case TAILOR -> new String[]{"minecraft:white_wool", "8"};
            case HERBALIST -> new String[]{"minecraft:glow_berries", "6"};
            case ALCHEMIST -> new String[]{"litrpg:mana_potion", "2"};
            case BAKER -> new String[]{"minecraft:bread", "12"};
            case BUTCHER -> new String[]{"minecraft:cooked_beef", "8"};
            case FISHMONGER -> new String[]{"minecraft:cooked_salmon", "8"};
            case INNKEEPER -> new String[]{"minecraft:golden_carrot", "4"};
            case BUILDER -> new String[]{"minecraft:lantern", "4"};
            case LEATHERWORKER -> new String[]{"minecraft:leather", "8"};
            case FLETCHER -> new String[]{"minecraft:arrow", "32"};
            case SCRIBE -> new String[]{"minecraft:book", "4"};
            case FARMER -> new String[]{"minecraft:hay_block", "4"};
            case HEALER -> new String[]{"minecraft:golden_apple", "1"};
            case BANKER -> new String[]{"litrpg:silver_coin", "6"};
            case STABLEMASTER -> new String[]{"minecraft:saddle", "1"};
            case MINER -> new String[]{"minecraft:coal", "16"};
        };
    }

    private static Quest collect(Ctx c, String item, int count, String title, String desc) {
        if (ForgeRegistries.ITEMS.getValue(new ResourceLocation(item)) == null) return null;
        Quest q = base(c, title, desc);
        q.type = Quest.Type.COLLECT;
        q.target = item;
        q.need = count;
        reward(c, q, 0.8f);
        return q;
    }

    private static Quest hunt(Ctx c, QuestTarget t, int count, String title, String desc) {
        Quest q = base(c, title, desc);
        q.type = Quest.Type.KILL;
        q.target = t.key;
        q.need = count + c.level / 8;
        reward(c, q, t == QuestTarget.ELITE || t == QuestTarget.WITCH ? 1.3f : 1f);
        return q;
    }

    private static Quest breed(Ctx c, String entity, int count, String title, String desc) {
        Quest q = base(c, title, desc);
        q.type = Quest.Type.BREED;
        q.target = entity;
        q.need = count;
        reward(c, q, 0.9f);
        return q;
    }

    /** Отнести посылку мастеру другого ремесла в этом же городе. */
    private static Quest deliver(Ctx c, Craft to, String title, String desc) {
        if (c.city == null) return null;
        WorldMap.Workshop best = null;
        for (WorldMap.Workshop w : WorldMap.get().workshopsOf(c.city.id())) {
            if (!w.craft().equals(to.key)) continue;
            if (Math.abs(w.x() - c.home.getX()) + Math.abs(w.z() - c.home.getZ()) < 4) continue;
            if (best == null || c.r.nextBoolean()) best = w;
        }
        if (best == null) return null;
        Quest q = base(c, title, desc);
        q.type = Quest.Type.DELIVER;
        q.target = to.key;
        q.need = 1;
        q.where = dative(to) + " (" + to.workshop + ")";
        q.hasPos = true;
        q.px = best.x();
        q.py = best.y();
        q.pz = best.z();
        reward(c, q, 0.6f);
        return q;
    }

    /** Добраться до места рядом с городом (деревня, руины, застава, другой город). */
    private static Quest visit(Ctx c, String[] types, String title, String descFmt) {
        if (c.city == null) return null;
        int maxDist = 500 + c.level * 60;
        List<WorldMap.Poi> ok = new ArrayList<>();
        for (WorldMap.Poi poi : WorldMap.get().pois) {
            boolean typeOk = false;
            for (String t : types) typeOk |= poi.type().equals(t);
            if (!typeOk || poi.id().equals(c.city.id())) continue;
            double d = Math.hypot(poi.x() - c.city.x(), poi.z() - c.city.z());
            if (d > 120 && d < maxDist) ok.add(poi);
        }
        if (ok.isEmpty()) return null;
        WorldMap.Poi poi = ok.get(c.r.nextInt(ok.size()));
        String name = poi.name().isEmpty() ? "заброшенного места" : poi.name();
        Quest q = base(c, title, String.format(descFmt, name));
        q.type = Quest.Type.VISIT;
        q.target = poi.id();
        q.need = 1;
        q.where = name + " (X " + poi.x() + ", Z " + poi.z() + ")";
        q.hasPos = true;
        q.px = poi.x();
        q.py = poi.y();
        q.pz = poi.z();
        double d = Math.hypot(poi.x() - c.city.x(), poi.z() - c.city.z());
        reward(c, q, Math.min(2.0f, 0.8f + (float) (d / 700.0)));
        return q;
    }

    /** Крысы в погребе / амбаре / на складе мастера. */
    private static Quest pests(Ctx c, int count, String title, String desc) {
        Quest q = base(c, title, desc);
        q.type = Quest.Type.KILL;
        q.target = QuestTarget.RAT.key;
        q.need = count + (c.level >= 12 ? 1 : 0);
        q.where = "у мастерской: " + c.giver;
        q.hasPos = true;
        q.px = c.home.getX();
        q.py = c.home.getY();
        q.pz = c.home.getZ();
        reward(c, q, 0.9f);
        return q;
    }

    private static String dative(Craft c) {
        return switch (c) {
            case BLACKSMITH -> "кузнецу";
            case ARMORER -> "броннику";
            case WEAPONSMITH -> "оружейнику";
            case JEWELER -> "ювелиру";
            case TAILOR -> "портному";
            case HERBALIST -> "травнику";
            case ALCHEMIST -> "алхимику";
            case BAKER -> "пекарю";
            case BUTCHER -> "мяснику";
            case FISHMONGER -> "рыботорговцу";
            case INNKEEPER -> "трактирщику";
            case BUILDER -> "строителю";
            case LEATHERWORKER -> "кожевнику";
            case FLETCHER -> "мастеру луков";
            case SCRIBE -> "писарю";
            case FARMER -> "фермеру";
            case HEALER -> "лекарю";
            case BANKER -> "меняле";
            case STABLEMASTER -> "конюху";
            case MINER -> "рудокопу";
        };
    }

    // ===================================================================
    //                     принятие / отказ / прогресс
    // ===================================================================

    public static void onAccept(ServerPlayer p, Quest q) {
        if (q.source != Quest.Source.CITY) return;
        if (q.type == Quest.Type.DELIVER) {
            ItemStack parcel = new ItemStack(ModItems.PARCEL.get());
            parcel.getOrCreateTag().putString("litrpg_quest", q.id);
            parcel.setHoverName(Component.literal("Посылка: " + q.where).withStyle(ChatFormatting.GOLD));
            if (!p.getInventory().add(parcel)) p.drop(parcel, false);
            StatsManager.message(p, Component.literal("Вы получили посылку. Отнесите её " + q.where + ".").withStyle(ChatFormatting.YELLOW));
        }
        if (q.hasPos) waypoint(p, q);
    }

    public static void onAbandon(ServerPlayer p, Quest q) {
        if (q.type == Quest.Type.DELIVER) removeParcel(p, q.id);
    }

    private static boolean removeParcel(ServerPlayer p, String id) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.is(ModItems.PARCEL.get()) && st.hasTag() && id.equals(st.getTag().getString("litrpg_quest"))) {
                inv.setItem(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    public static void waypoint(ServerPlayer p, Quest q) {
        com.andrey.litrpg.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> p),
                new com.andrey.litrpg.network.WaypointPacket(q.px, q.pz));
        int dx = q.px - p.getBlockX(), dz = q.pz - p.getBlockZ();
        StatsManager.message(p, Component.literal("◆ " + q.title + ": X " + q.px + ", Z " + q.pz + " — "
                + (int) Math.hypot(dx, dz) + " бл. Метка добавлена на карту.").withStyle(ChatFormatting.AQUA));
    }

    /** Доставка: ПКМ по мастеру нужного ремесла в том же городе. */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getTarget() instanceof RaceNpcEntity npc)) return;
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND || npc.craft() == null) return;
        WorldMap.City city = SystemWorld.isOverworld(npc.level()) ? WorldMap.get().cityAt(npc.getX(), npc.getZ()) : null;
        for (Quest q : StatsManager.get(p).quests().active) {
            if (q.type != Quest.Type.DELIVER || q.isComplete() || !q.target.equals(npc.craft().key)) continue;
            if (!q.place.isEmpty() && (city == null || !city.id().equals(q.place))) continue;
            if (!removeParcel(p, q.id)) {
                StatsManager.message(p, Component.literal(npc.getName().getString() + ": «А где же посылка? Без неё не приму».")
                        .withStyle(ChatFormatting.GRAY));
                continue;
            }
            q.have = q.need;
            Speech.say(npc, "Спасибо! Давно ждал эту посылку.", 60);
            p.level().playSound(null, npc.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
            QuestEvents.checkAll(p);
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            return;
        }
    }

    /** Разведение животных. */
    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (!(event.getCausedByPlayer() instanceof ServerPlayer p) || event.getChild() == null) return;
        ResourceLocation id = EntityType.getKey(event.getChild().getType());
        boolean changed = false;
        for (Quest q : StatsManager.get(p).quests().tracked()) {
            if (q.type == Quest.Type.BREED && !q.isComplete() && q.target.equals(id.toString())) {
                q.have++;
                changed = true;
                if (!q.isComplete()) p.displayClientMessage(Component.literal(q.title + ": " + q.have + "/" + q.need).withStyle(ChatFormatting.AQUA), true);
            }
        }
        if (changed) QuestEvents.checkAll(p);
    }

    /** Поход к месту и появление крыс, когда игрок подходит к мастерской. */
    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 40 != 7) return;
        boolean changed = false;
        for (Quest q : StatsManager.get(p).quests().active) {
            if (q.isComplete() || !q.hasPos) continue;
            double dx = p.getX() - q.px, dz = p.getZ() - q.pz;
            double d2 = dx * dx + dz * dz;
            if (q.type == Quest.Type.VISIT && d2 < 16 * 16 && SystemWorld.isOverworld(p.level())) {
                q.have = q.need;
                changed = true;
                StatsManager.message(p, Component.literal("Вы добрались: " + q.where).withStyle(ChatFormatting.GREEN));
            } else if (q.type == Quest.Type.KILL && QuestTarget.RAT.key.equals(q.target) && d2 < 22 * 22
                    && Math.abs(p.getY() - q.py) < 12) {
                spawnPests(p, q);
            }
        }
        if (changed) QuestEvents.checkAll(p);
    }

    private static void spawnPests(ServerPlayer p, Quest q) {
        ServerLevel sl = p.serverLevel();
        BlockPos center = new BlockPos(q.px, q.py, q.pz);
        AABB box = new AABB(center).inflate(28, 10, 28);
        int alive = sl.getEntitiesOfClass(RatEntity.class, box, e -> e.getTags().contains(PEST_TAG)).size();
        int want = q.need - q.have - alive;
        if (want <= 0) return;
        boolean king = q.need >= 7 && q.need - q.have == 1 || q.need >= 7 && q.spawned == 0;
        for (int i = 0; i < Math.min(want, 8); i++) {
            EntityType<RatEntity> type = king && i == 0 ? ModEntities.RAT_KING.get() : ModEntities.RAT.get();
            RatEntity rat = type.create(sl);
            if (rat == null) return;
            BlockPos at = spot(sl, center, p.getRandom());
            rat.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getRandom().nextFloat() * 360, 0);
            rat.finalizeSpawn(sl, sl.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null, null);
            rat.addTag(PEST_TAG);
            sl.addFreshEntity(rat);
            q.spawned++;
        }
        sl.playSound(null, center, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.8f);
        p.displayClientMessage(Component.literal("Из щелей полезли крысы!").withStyle(ChatFormatting.RED), true);
    }

    private static BlockPos spot(ServerLevel sl, BlockPos c, RandomSource r) {
        for (int i = 0; i < 12; i++) {
            BlockPos b = c.offset(r.nextInt(7) - 3, 0, r.nextInt(7) - 3);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos t = b.above(dy);
                if (sl.getBlockState(t).isAir() && sl.getBlockState(t.above()).isAir() && sl.getBlockState(t.below()).isSolid()) return t;
            }
        }
        return c;
    }
}
