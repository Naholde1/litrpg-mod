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
            "Звери и лесные создания. Разведение и приручение животных повышают репутацию, бессмысленная охота — роняет.");

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
            case HUMAN -> this == KINGDOM ? 150 : this == ILLAGERS ? -100 : 0;
            case ELF -> this == WILDS ? 250 : this == KINGDOM ? 50 : this == UNDEAD ? -100 : 0;
            case DWARF -> this == KINGDOM ? 150 : this == GOBLINS ? -150 : 0;
            case ORC -> this == GOBLINS ? 300 : this == KINGDOM ? -150 : this == ILLAGERS ? 50 : 0;
            case BEASTKIN -> this == WILDS ? 400 : 0;
            case DEMONKIN -> this == PIGLINS ? 350 : this == KINGDOM ? -100 : this == UNDEAD ? 100 : 0;
        };
    }

    public static Faction byKey(String key) {
        for (Faction f : values()) if (f.key.equals(key)) return f;
        return null;
    }
}
