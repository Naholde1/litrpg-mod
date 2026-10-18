package com.andrey.litrpg.rep;

import com.andrey.litrpg.stats.Race;
import net.minecraft.ChatFormatting;

/** Народы мира, с которыми у игрока есть репутация (-1000..1000). */
public enum Faction {
    KINGDOM("kingdom", "Королевство людей", 0x5DADE2,
            "Жители деревень и железные големы. Торговля и защита деревень повышают репутацию, убийства жителей — роняют."),
    GOBLINS("goblins", "Племена гоблинов", 0x6FA34A,
            "Дружелюбных гоблины не трогают. Можно задобрить, дав гоблину золотой слиток (ПКМ)."),
    PIGLINS("piglins", "Пиглины Преисподней", 0xE59866,
            "Подарки золотом и охота на хоглинов повышают репутацию. Дружелюбные пиглины не нападают даже без золотой брони."),
    ILLAGERS("illagers", "Иллагеры", 0x7F8C8D,
            "Разбойники и колдуны. Им нравится, когда страдают деревни. Дружелюбные иллагеры не нападают."),
    UNDEAD("undead", "Легион нежити", 0x8E44AD,
            "Мертвецы уважают некромантов и тех, кто сеет смерть среди живых. Дружелюбная нежить не нападает."),
    WILDS("wilds", "Дети природы", 0x27AE60,
            "Звери и лесные создания. Разведение и приручение животных повышают репутацию, бессмысленная охота — роняет."),
    ELVES("elves", "Эльфы Сильвендара", 0x9BE38C,
            "Древний лесной народ. Живут в рощах среди лесов. Не любят орков и демонидов, ценят защиту леса."),
    DWARVES("dwarves", "Подгорные кланы", 0xD08A4A,
            "Дварфы горных твердынь. Мастера стали и рун, ненавидят гоблинов. Уважают тех, кто торгует честно."),
    ORCS("orcs", "Орочья Орда", 0x6FA34A,
            "Военные лагеря в саваннах и пустошах. Уважают силу. Враги эльфов и дварфов."),
    BEASTKIN("beastkin", "Племена зверолюдов", 0xC9A36B,
            "Охотничьи стойбища в тайге и джунглях. Дружны с природой, не терпят браконьеров."),
    DEMONKIN("demonkin", "Культ Пепла", 0xC0405A,
            "Демониды и их святилища в бесплодных землях. Опасны, но платят адской серой за услуги.");

    public final String key;
    public final String title;
    public final int color;
    public final String desc;

    Faction(String key, String title, int color, String desc) {
        this.key = key;
        this.title = title;
        this.color = color;
        this.desc = desc;
    }

    /** Ступени отношения. */
    public enum Tier {
        HATED("Ненависть", -1000, ChatFormatting.DARK_RED),
        HOSTILE("Враждебность", -600, ChatFormatting.RED),
        UNFRIENDLY("Неприязнь", -200, ChatFormatting.GOLD),
        NEUTRAL("Нейтралитет", -50, ChatFormatting.YELLOW),
        FRIENDLY("Дружелюбие", 50, ChatFormatting.GREEN),
        HONORED("Уважение", 250, ChatFormatting.AQUA),
        REVERED("Почтение", 600, ChatFormatting.LIGHT_PURPLE);

        public final String title;
        public final int from;
        public final ChatFormatting color;

        Tier(String title, int from, ChatFormatting color) {
            this.title = title;
            this.from = from;
            this.color = color;
        }

        public static Tier of(int rep) {
            Tier t = HATED;
            for (Tier x : values()) if (rep >= x.from) t = x;
            return t;
        }
    }

    /** Стартовая репутация расы. */
    public int start(Race race) {
        if (race == null) return 0;
        return switch (race) {
            case HUMAN -> switch (this) { case KINGDOM -> 150; case ILLAGERS -> -100; case DWARVES, ELVES -> 50; default -> 0; };
            case ELF -> switch (this) { case ELVES -> 500; case WILDS -> 250; case BEASTKIN -> 100; case KINGDOM -> 50;
                case ORCS -> -300; case DEMONKIN -> -300; case UNDEAD -> -100; case DWARVES -> -50; default -> 0; };
            case DWARF -> switch (this) { case DWARVES -> 500; case KINGDOM -> 150; case GOBLINS -> -250; case ORCS -> -250;
                case ELVES -> -50; default -> 0; };
            case ORC -> switch (this) { case ORCS -> 500; case GOBLINS -> 300; case ILLAGERS -> 50; case KINGDOM -> -150;
                case ELVES -> -300; case DWARVES -> -250; default -> 0; };
            case BEASTKIN -> switch (this) { case BEASTKIN -> 500; case WILDS -> 400; case ELVES -> 100; default -> 0; };
            case DEMONKIN -> switch (this) { case DEMONKIN -> 500; case PIGLINS -> 350; case UNDEAD -> 100; case KINGDOM -> -100;
                case ELVES -> -300; default -> 0; };
        };
    }

    public static Faction byKey(String key) {
        for (Faction f : values()) if (f.key.equals(key)) return f;
        return null;
    }
}
