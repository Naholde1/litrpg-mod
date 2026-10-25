package com.andrey.litrpg.gear;

import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Генерация предметов Системы: базовый предмет, редкость, характеристики, имя. */
public final class GearGen {
    private GearGen() {}

    /**
     * Веса редкостей от обычной до легендарной (без бонусов: 70% / 22% / 6% / 1.4% / 0.2%).
     * Божественные выпадают отдельным, крошечным броском.
     */
    private static final int[] BASE_WEIGHTS = {700, 220, 60, 14, 2};
    /** Насколько сильно бонус (элита, босс, удача) сдвигает редкость вверх. */
    private static final float BOOST_POWER = 0.6f;
    /** Шанс, что легендарный предмет окажется божественным (у босса, при boost 2.5). */
    private static final float DIVINE_FROM_LEGENDARY = 0.01f;

    /** Бросок редкости. boost: 0 — обычный моб, 0.6 — элита, 1.2 — чемпион, 2.5 — босс. */
    public static Rarity rollRarity(RandomSource rnd, float boost) {
        return rollRarity(rnd, boost, true);
    }

    /** @param divine может ли выпасть божественный предмет (у торговцев и в ремесле — нет) */
    public static Rarity rollRarity(RandomSource rnd, float boost, boolean divine) {
        double[] w = new double[BASE_WEIGHTS.length];
        double sum = 0;
        for (int i = 0; i < w.length; i++) {
            w[i] = BASE_WEIGHTS[i] * Math.pow(1 + boost * BOOST_POWER, i);
            sum += w[i];
        }
        double r = rnd.nextDouble() * sum;
        for (int i = 0; i < w.length; i++) {
            r -= w[i];
            if (r <= 0) {
                Rarity res = Rarity.values()[i];
                if (divine && res == Rarity.LEGENDARY && rnd.nextFloat() < DIVINE_FROM_LEGENDARY * (1 + boost * BOOST_POWER) / 2.5f) {
                    return Rarity.DIVINE;
                }
                return res;
            }
        }
        return Rarity.COMMON;
    }

    /** Случайный предмет уровня level и редкости rarity. */
    public static ItemStack random(RandomSource rnd, int level, Rarity rarity) {
        // треть предметов — из шаблонов мода (украшения, наборы, оружие рас)
        if (rnd.nextFloat() < 0.35f) {
            ItemStack t = templateNear(rnd, level, false);
            if (!t.isEmpty()) {
                apply(t, rnd, level, rarity.ordinal() < templateMin(t).ordinal() ? templateMin(t) : rarity);
                return t;
            }
        }
        float c = rnd.nextFloat();
        ItemStack base = c < 0.42f ? weaponBaseStack(rnd, level) : c < 0.82f ? armorBase(rnd, level) : jewelBase(rnd, level);
        apply(base, rnd, level, rarity);
        return base;
    }

    /**
     * Личная добыча: предмет под класс игрока (оружие его рода, броня, украшение) уровня level.
     * Танку чаще броня и щиты, магам — посохи, стрелкам — луки.
     */
    public static ItemStack personal(RandomSource rnd, int level, Rarity rarity, com.andrey.litrpg.stats.PlayerStats ps) {
        if (rnd.nextFloat() < 0.15f) {
            ItemStack t = templateNear(rnd, level, false);
            if (!t.isEmpty()) {
                apply(t, rnd, level, rarity.ordinal() < templateMin(t).ordinal() ? templateMin(t) : rarity);
                return t;
            }
        }
        var role = ps.getRole();
        float c = rnd.nextFloat();
        float wpn = role == com.andrey.litrpg.stats.Role.TANK ? 0.3f : 0.4f;
        ItemStack base;
        if (c < wpn) base = new ItemStack(classWeapon(rnd, level, ps));
        else if (c < 0.82f) {
            base = role == com.andrey.litrpg.stats.Role.TANK && rnd.nextInt(6) == 0 ? new ItemStack(Items.SHIELD) : armorBaseStack(rnd, level);
        } else base = jewelBase(rnd, level);
        apply(base, rnd, level, rarity);
        return base;
    }

    /** Оружие рода, которым сражается класс (до выбора класса — по роли). */
    private static Item classWeapon(RandomSource rnd, int level, com.andrey.litrpg.stats.PlayerStats ps) {
        int t = level < 8 ? 0 : level < 20 ? 1 : level < 35 ? 2 : 3;
        var cls = ps.getPlayerClass();
        int melee = 1, ranged = 0, magic = 0, stealth = 0;
        if (cls != null) {
            melee = cls.styles.getOrDefault(com.andrey.litrpg.stats.Style.MELEE, 0);
            ranged = cls.styles.getOrDefault(com.andrey.litrpg.stats.Style.RANGED, 0);
            magic = cls.styles.getOrDefault(com.andrey.litrpg.stats.Style.MAGIC, 0);
            stealth = cls.styles.getOrDefault(com.andrey.litrpg.stats.Style.STEALTH, 0);
        } else if (ps.getRole() == com.andrey.litrpg.stats.Role.SUPPORT) magic = 2;
        int best = Math.max(Math.max(melee, ranged), Math.max(magic, stealth));
        if (best == ranged && ranged > 0) return rnd.nextInt(4) == 0 ? Items.CROSSBOW : Items.BOW;
        if (best == magic && magic > 0) return ModItems.APPRENTICE_STAFF.get();
        if (best == stealth && stealth > 0) return t < 2 ? ModItems.ROGUE_DAGGER.get() : ModItems.SHADOW_DAGGER.get();
        return rnd.nextInt(3) == 0 ? new Item[]{Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE}[t]
                : new Item[]{Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}[t];
    }

    /** Наделить существующий предмет свойствами (например, скрафченный кузнецом). */
    public static void apply(ItemStack stack, RandomSource rnd, int level, Rarity rarity) {
        GearData.Category cat = GearData.category(stack);
        if (cat == null) return;
        GearData d = new GearData();
        d.rarity = rarity;
        d.level = Math.max(1, level);
        float m = rarity.mult;
        int[] weights = switch (cat) {
            case WEAPON -> new int[]{3, 3, 1, 2, 1};
            case ARMOR -> new int[]{2, 2, 3, 2, 1};
            case JEWEL -> new int[]{2, 2, 2, 2, 2};
        };
        for (int i = 0; i < rarity.stats; i++) {
            int s = pick(rnd, weights);
            d.stats[s] += Math.max(1, Math.round((1 + level * 0.1f) * m * (0.7f + rnd.nextFloat() * 0.6f)));
        }
        List<GearBonus> pool = new ArrayList<>();
        for (GearBonus b : GearBonus.values()) if (b.fits(cat)) pool.add(b);
        for (int i = 0; i < rarity.bonuses && !pool.isEmpty(); i++) {
            GearBonus b = pool.remove(rnd.nextInt(pool.size()));
            d.bonuses.put(b, b.roll(level, m, rnd.nextFloat()));
        }
        List<Passive> passives = new ArrayList<>();
        for (Passive p : Passive.values()) if (!p.classOnly && p.weapon == (cat == GearData.Category.WEAPON)) passives.add(p);
        for (int i = 0; i < rarity.passives && !passives.isEmpty(); i++) d.passives.add(passives.remove(rnd.nextInt(passives.size())));
        if (rarity.ability && cat == GearData.Category.WEAPON) d.ability = ABILITIES[rnd.nextInt(ABILITIES.length)];
        // врождённые свойства шаблона есть у предмета любой редкости
        if (stack.getItem() instanceof TemplateItem t) {
            var def = t.def();
            def.implicitBonus.forEach((b, v) -> d.bonuses.merge(b, v, Float::sum));
            for (int i = 0; i < d.stats.length; i++) d.stats[i] += def.implicitStats[i];
            Passive ip = def.passive == null ? null : Passive.byKey(def.passive);
            if (ip != null && !d.passives.contains(ip)) d.passives.add(ip);
        }
        d.write(stack);
        stack.setHoverName(name(stack, rarity, rnd));
    }

    /** Мелкое украшение уровня около level (часто падает с монстров, основа торговли). */
    public static ItemStack trinket(RandomSource rnd, int level, float boost) {
        ItemTemplates.Def def = ItemTemplates.randomJewel(rnd, level);
        if (def == null) return ItemStack.EMPTY;
        ItemStack st = new ItemStack(com.andrey.litrpg.registry.ModItems.TEMPLATE_ITEMS.get(def.id).get());
        Rarity r = rollRarity(rnd, boost);
        if (r.ordinal() < def.minRarity.ordinal()) r = def.minRarity;
        apply(st, rnd, Math.max(1, def.tier + rnd.nextInt(3) - 1), r);
        return st;
    }

    /** Любой предмет-шаблон с уровнем не выше level+5. */
    public static ItemStack templateNear(RandomSource rnd, int level, boolean jewelsOnly) {
        java.util.List<ItemTemplates.Def> pool = new java.util.ArrayList<>();
        for (ItemTemplates.Def d : ItemTemplates.ITEMS) {
            if (d.boss == null && d.tier <= level + 5 && d.tier >= level - 15 && (!jewelsOnly || d.isJewel())) pool.add(d);
        }
        if (pool.isEmpty()) return ItemStack.EMPTY;
        ItemTemplates.Def def = pool.get(rnd.nextInt(pool.size()));
        return new ItemStack(com.andrey.litrpg.registry.ModItems.TEMPLATE_ITEMS.get(def.id).get());
    }

    private static Rarity templateMin(ItemStack st) {
        return st.getItem() instanceof TemplateItem t ? t.def().minRarity : Rarity.COMMON;
    }

    private static final String[] ABILITIES = {"fireball", "chain_lightning", "frost_nova", "blink", "meteor", "war_cry",
            "shadow_step", "smite", "lightning_strike", "whirlwind", "holy_light", "drain_life", "arrow_rain", "slam", "blade_dance"};

    private static int pick(RandomSource rnd, int[] weights) {
        int sum = 0;
        for (int w : weights) sum += w;
        int r = rnd.nextInt(sum);
        for (int i = 0; i < weights.length; i++) {
            r -= weights[i];
            if (r < 0) return i;
        }
        return 0;
    }

    // ---------------- базовые предметы ----------------

    private static Item weaponBase(RandomSource rnd, int level) {
        int t = level < 6 ? 0 : level < 14 ? 1 : level < 24 ? 2 : 3;
        int r = rnd.nextInt(100);
        if (r < 30) return new Item[]{Items.STONE_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}[t];
        if (r < 50) return new Item[]{Items.STONE_AXE, Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE}[t];
        if (r < 65) return Items.BOW;
        if (r < 73) return Items.CROSSBOW;
        if (r < 87) return t < 2 ? ModItems.ROGUE_DAGGER.get() : ModItems.SHADOW_DAGGER.get();
        return ModItems.APPRENTICE_STAFF.get();
    }

    private static ItemStack armorBaseStack(RandomSource rnd, int level) {
        int slot = rnd.nextInt(4);
        // предметы наборов — в своих диапазонах уровней
        float set = rnd.nextFloat();
        if (set < 0.25f) {
            if (level >= 5 && level <= 16) return new ItemStack(ModItems.GOBLIN_SET[slot].get());
            if (level >= 12 && level <= 30) return new ItemStack(ModItems.BONEWARDEN_SET[slot].get());
            if (level >= 22) return new ItemStack(ModItems.SHADOW_SET[slot].get());
        }
        if (rnd.nextInt(20) == 0) return new ItemStack(Items.SHIELD);
        int t = level < 4 ? 0 : level < 9 ? 1 : level < 18 ? 2 : level < 28 ? 3 : 4;
        Item[][] table = {
                {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
                {Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
                {Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS},
                {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS},
                {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS},
        };
        return new ItemStack(table[t][slot]);
    }

    private static ItemStack weaponBaseStack(RandomSource rnd, int level) {
        return new ItemStack(weaponBase(rnd, level));
    }

    private static ItemStack armorBase(RandomSource rnd, int level) {
        return armorBaseStack(rnd, level);
    }

    private static ItemStack jewelBase(RandomSource rnd, int level) {
        int t = level < 10 ? 0 : level < 25 ? 1 : 2;
        int kind = rnd.nextInt(5);
        if (kind < 2) return new ItemStack(new Item[]{ModItems.COPPER_RING.get(), ModItems.SILVER_RING.get(), ModItems.GOLD_RING.get()}[t]);
        if (kind < 4) return new ItemStack(new Item[]{ModItems.JADE_AMULET.get(), ModItems.RUBY_AMULET.get(), ModItems.STAR_AMULET.get()}[t]);
        return new ItemStack(ModItems.LEATHER_BELT.get());
    }

    // ---------------- имена ----------------

    private static final String[][] EPITHETS = {
            {},
            {},
            {"Удачи", "Стойкости", "Охотника", "Странника", "Рассвета", "Ветра", "Бури", "Тени", "Пламени", "Инея", "Волка", "Ястреба"},
            {"Гнев титана", "Шёпот бездны", "Клятва стража", "Песнь клинка", "Сердце зимы", "Пепел войны", "Око бури", "Кровь дракона", "Зов луны"},
            {"Погибель королей", "Последний рассвет", "Длань судьбы", "Слеза богини", "Пламя Изнанки", "Вечная ночь", "Гроза пустошей"},
            {"Воля Системы", "Искра творения", "Венец мироздания", "Дыхание вечности", "Глас небес"},
    };

    public static MutableComponent name(ItemStack stack, Rarity rarity, RandomSource rnd) {
        Component base = stack.getItem().getName(stack);
        MutableComponent name = Component.empty().append(base);
        String[] pool = EPITHETS[rarity.ordinal()];
        if (pool.length > 0) name.append(" «" + pool[rnd.nextInt(pool.length)] + "»");
        if (rarity == Rarity.DIVINE) name = Component.literal("✦ ").append(name);
        return name.withStyle(s -> s.withColor(rarity.color).withItalic(false));
    }

    // ---------------- уникальные предметы ----------------

    public static ItemStack unique(String key) {
        ItemStack stack;
        GearData d = new GearData();
        d.unique = key;
        String title;
        switch (key) {
            case "dawn_blade" -> {
                stack = new ItemStack(ModItems.DAWN_BLADE.get());
                d.rarity = Rarity.LEGENDARY; d.level = 25;
                st(d, Stat.STRENGTH, 10); st(d, Stat.VITALITY, 6); st(d, Stat.LUCK, 4);
                d.bonuses.put(GearBonus.DAMAGE, 8f); d.bonuses.put(GearBonus.CRIT_CHANCE, 10f); d.bonuses.put(GearBonus.LIFESTEAL, 6f);
                d.passives.add(Passive.FLAME_TOUCH); d.passives.add(Passive.EXECUTIONER);
                d.ability = "smite";
                title = "Рассветный клинок";
            }
            case "frost_sorrow" -> {
                stack = new ItemStack(ModItems.FROST_SORROW.get());
                d.rarity = Rarity.DIVINE; d.level = 40;
                st(d, Stat.STRENGTH, 14); st(d, Stat.INTELLIGENCE, 10); st(d, Stat.VITALITY, 8);
                d.bonuses.put(GearBonus.DAMAGE, 14f); d.bonuses.put(GearBonus.CRIT_DAMAGE, 40f); d.bonuses.put(GearBonus.LIFESTEAL, 8f);
                d.passives.add(Passive.FROST_TOUCH); d.passives.add(Passive.SOUL_EATER);
                d.ability = "frost_nova";
                title = "Ледяная Скорбь";
            }
            case "kings_ring" -> {
                stack = new ItemStack(ModItems.KINGS_RING.get());
                d.rarity = Rarity.EPIC; d.level = 15;
                st(d, Stat.STRENGTH, 5); st(d, Stat.LUCK, 5);
                d.bonuses.put(GearBonus.LOOT, 25f); d.bonuses.put(GearBonus.XP, 10f);
                d.passives.add(Passive.TREASURE_HUNTER);
                title = "Перстень Короля гоблинов";
            }
            case "lich_amulet" -> {
                stack = new ItemStack(ModItems.LICH_AMULET.get());
                d.rarity = Rarity.LEGENDARY; d.level = 30;
                st(d, Stat.INTELLIGENCE, 12); st(d, Stat.VITALITY, 4);
                d.bonuses.put(GearBonus.MANA, 120f); d.bonuses.put(GearBonus.COOLDOWN, 15f); d.bonuses.put(GearBonus.SPELL_POWER, 20f);
                d.passives.add(Passive.MANA_SHIELD); d.passives.add(Passive.NIGHT_EYE);
                title = "Амулет Лича";
            }
            default -> {
                stack = new ItemStack(ModItems.HEART_OF_SYSTEM.get());
                d.unique = "heart_of_system";
                d.rarity = Rarity.DIVINE; d.level = 45;
                for (Stat s : Stat.values()) st(d, s, 6);
                d.bonuses.put(GearBonus.XP, 25f); d.bonuses.put(GearBonus.CRIT_CHANCE, 8f); d.bonuses.put(GearBonus.HEALTH, 30f);
                d.passives.add(Passive.PHOENIX); d.passives.add(Passive.REGENERATION);
                title = "Сердце Системы";
            }
        }
        d.write(stack);
        MutableComponent name = Component.literal((d.rarity == Rarity.DIVINE ? "✦ " : "") + title);
        stack.setHoverName(name.withStyle(s -> s.withColor(d.rarity.color).withItalic(false).withBold(d.rarity == Rarity.DIVINE)));
        return stack;
    }

    private static void st(GearData d, Stat s, int v) { d.stats[s.ordinal()] = v; }

    public static final String[] UNIQUES = {"dawn_blade", "frost_sorrow", "kings_ring", "lich_amulet", "heart_of_system"};

    /** Описание (лор) уникального предмета для подсказки. */
    public static String lore(String unique) {
        return switch (unique) {
            case "dawn_blade" -> "Выкован из первого луча солнца после Долгой Ночи.";
            case "frost_sorrow" -> "Клинок помнит каждую душу, что выпил. Их шёпот холоден.";
            case "archmage_staff" -> "Последний Архимаг ушёл в Изнанку. Его посох остался.";
            case "kings_ring" -> "Король гоблинов не снимал его даже во сне.";
            case "lich_amulet" -> "Внутри бьётся чужое сердце. Медленно.";
            case "heart_of_system" -> "Фрагмент самой Системы. Она смотрит на тебя.";
            default -> "";
        };
    }

    static ChatFormatting color(Rarity r) { return r.color; }
}
