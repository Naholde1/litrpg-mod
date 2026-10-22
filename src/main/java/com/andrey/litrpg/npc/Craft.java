package com.andrey.litrpg.npc;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * Ремесло горожанина-мастера: что он продаёт, какие поручения даёт,
 * как работает (звук и частицы) и что приговаривает.
 */
public enum Craft {
    BLACKSMITH("blacksmith", "кузнец", "кузнечиха", "Кузница", SoundEvents.ANVIL_USE, ParticleTypes.LAVA,
            "Железо любит жар и терпение.", "Кому подковать коня? Кому наточить меч?", "Горн не гаснет с рассвета!",
            "Хороший клинок — это сотня ударов молота."),
    ARMORER("armorer", "бронник", "бронница", "Бронная мастерская", SoundEvents.ARMOR_EQUIP_IRON, ParticleTypes.CRIT,
            "Латы спасут тебе жизнь — если подобраны по размеру.", "Кольчуга, наручи, шлемы — всё на заказ!",
            "Без доброй брони за стены лучше не ходить."),
    WEAPONSMITH("weaponsmith", "оружейник", "оружейница", "Оружейная", SoundEvents.GRINDSTONE_USE, ParticleTypes.CRIT,
            "Клинок без заточки — просто железка.", "Мечи, топоры, копья — выбирай!", "Каждое лезвие проверено в бою."),
    JEWELER("jeweler", "ювелир", "ювелирша", "Ювелирная лавка", SoundEvents.AMETHYST_BLOCK_CHIME, ParticleTypes.END_ROD,
            "Камень без огранки — лишь галька.", "Кольца, амулеты, обереги — с силой Системы!", "Осторожно, не урони мою лупу…"),
    TAILOR("tailor", "портной", "портниха", "Портняжная мастерская", SoundEvents.WOOL_PLACE, ParticleTypes.CLOUD,
            "Плащ по фигуре — и в дорогу не стыдно.", "Шёлк из-за моря, шерсть с горных пастбищ!", "Опять моль добралась до сукна…"),
    HERBALIST("herbalist", "травник", "травница", "Травная лавка", SoundEvents.CROP_PLANTED, ParticleTypes.HAPPY_VILLAGER,
            "Полынь от лихорадки, мята от тоски.", "Свежие травы, только с утра собраны!", "Каждому корешку — свой час сбора."),
    ALCHEMIST("alchemist", "алхимик", "алхимица", "Алхимическая лаборатория", SoundEvents.BREWING_STAND_BREW, ParticleTypes.WITCH,
            "Не трогай синюю колбу. И зелёную тоже.", "Зелья маны, лечения, силы — всё по рецептам!", "Если что-то взорвётся — я ни при чём."),
    BAKER("baker", "пекарь", "пекарша", "Пекарня", SoundEvents.FURNACE_FIRE_CRACKLE, ParticleTypes.SMOKE,
            "Хлеб с пылу с жару!", "Пироги с яблоками — кто первый?", "Мука кончается, а крысы не дремлют…"),
    BUTCHER("butcher", "мясник", "мясница", "Мясная лавка", SoundEvents.PLAYER_ATTACK_STRONG, ParticleTypes.DAMAGE_INDICATOR,
            "Свежее мясо, утренний забой!", "Окорока, колбасы, рёбрышки!", "Свиньи на заднем дворе опять разбежались."),
    FISHMONGER("fishmonger", "рыботорговец", "рыбачка", "Рыбная лавка", SoundEvents.FISHING_BOBBER_SPLASH, ParticleTypes.SPLASH,
            "Треска, лосось, иглобрюх — лучший улов!", "Рыба свежая, ещё вчера плавала!", "Говорят, в озере видели огромную тень…"),
    INNKEEPER("innkeeper", "трактирщик", "трактирщица", "Таверна", SoundEvents.BOTTLE_FILL, ParticleTypes.NOTE,
            "Эля? Похлёбки? Или новостей?", "В таверне всегда найдётся место у огня.", "Кто опять не заплатил за комнату?!"),
    BUILDER("builder", "строитель", "строительница", "Гильдия строителей", SoundEvents.WOOD_PLACE, ParticleTypes.POOF,
            "Кто опять разломал стену?!", "Город строили века — а ломают за минуту.", "Камень к камню — и стоит город."),
    LEATHERWORKER("leatherworker", "кожевник", "кожевница", "Кожевенная мастерская", SoundEvents.ARMOR_EQUIP_LEATHER, ParticleTypes.SMOKE,
            "Кожу дубить — не мечом махать.", "Пояса, перчатки, сёдла — крепче не найдёшь!", "Запах? Привыкнешь."),
    FLETCHER("fletcher", "лучных дел мастер", "лучница", "Лучная мастерская", SoundEvents.ARROW_SHOOT, ParticleTypes.CRIT,
            "Стрела летит туда, куда смотрит сердце.", "Луки из тиса, стрелы с гусиным пером!", "Кур не видел? Перья кончаются."),
    SCRIBE("scribe", "писарь", "писарица", "Книжная лавка", SoundEvents.BOOK_PAGE_TURN, ParticleTypes.ENCHANT,
            "Карты, книги, письма — пишу на заказ.", "Знание — тоже оружие.", "Доставить письмо? Плачу честно."),
    FARMER("farmer", "фермер", "фермерша", "Ферма", SoundEvents.CROP_BREAK, ParticleTypes.HAPPY_VILLAGER,
            "Урожай нынче добрый!", "Овощи, яйца, молоко — с моих грядок.", "Крысы в амбаре — беда, а не год."),
    HEALER("healer", "лекарь", "лекарка", "Лечебница", SoundEvents.AMETHYST_BLOCK_RESONATE, ParticleTypes.HEART,
            "Раны промыть, травами залечить.", "Не геройствуй — приходи сюда живым.", "Нежить за стенами — и снова раненые…"),
    BANKER("banker", "меняла", "меняла", "Торговая палата", SoundEvents.CHAIN_PLACE, ParticleTypes.WAX_ON,
            "Золото любит счёт.", "Обмен монет, жетоны народов, ценные камни!", "Караваны задерживаются — плохой знак."),
    STABLEMASTER("stablemaster", "конюх", "конюшиха", "Конюшня", SoundEvents.HORSE_AMBIENT, ParticleTypes.POOF,
            "Конь в дороге — лучший друг.", "Сёдла, сено, подковы!", "Кобыла опять сбежала за ворота…"),
    MINER("miner", "рудокоп", "рудокопша", "Рудная лавка", SoundEvents.STONE_BREAK, ParticleTypes.ASH,
            "Руда сама себя не добудет.", "Уголь, железо, медь — с лучших жил!", "В старой шахте завелись пауки…"),
    // ---- 2.13.0: постоялые дворы и круги телепорта (новые — только в конец: номер ремесла синхронизируется) ----
    HOSTELER("hosteler", "хозяин постоялого двора", "хозяйка постоялого двора", "Постоялый двор", SoundEvents.WOODEN_DOOR_OPEN, ParticleTypes.NOTE,
            "Комнаты чистые, постели мягкие!", "Сундук Края в каждой комнате — ни один вор не доберётся.",
            "Путник, тебе нужен ночлег?", "Ключ от комнаты — только постояльцу."),
    TELEPORTER("teleporter", "маг порталов", "чародейка порталов", "Круг телепорта", SoundEvents.AMETHYST_BLOCK_RESONATE, ParticleTypes.PORTAL,
            "Круги всех городов связаны нитями Системы.", "Куда держишь путь? Плати — и ты уже там.",
            "Не шевелись, пока круг поёт.", "Однажды я отправил гнома в озеро. Больше такого не будет.");

    public final String key;
    public final String title;
    public final String titleF;
    public final String workshop;
    public final SoundEvent workSound;
    public final ParticleOptions workParticle;
    public final String[] phrases;

    Craft(String key, String title, String titleF, String workshop, SoundEvent workSound, ParticleOptions workParticle, String... phrases) {
        this.key = key;
        this.title = title;
        this.titleF = titleF;
        this.workshop = workshop;
        this.workSound = workSound;
        this.workParticle = workParticle;
        this.phrases = phrases;
    }

    public String titleCap() {
        return Character.toUpperCase(title.charAt(0)) + title.substring(1);
    }

    public static Craft byKey(String key) {
        for (Craft c : values()) if (c.key.equals(key)) return c;
        return null;
    }
}
