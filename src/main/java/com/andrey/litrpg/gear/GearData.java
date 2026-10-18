package com.andrey.litrpg.gear;

import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.item.GearSetArmorItem;
import com.andrey.litrpg.item.JewelryItem;
import com.andrey.litrpg.item.LichStaffItem;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TridentItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Свойства предмета Системы, хранятся в NBT стека под ключом {@value #TAG}.
 * Подходит для любого оружия, брони и украшений — и ванильных, и модовых.
 */
public final class GearData {
    public static final String TAG = "litrpg_gear";

    public enum Category { WEAPON, ARMOR, JEWEL }

    public Rarity rarity = Rarity.COMMON;
    public int level = 1;
    public final int[] stats = new int[Stat.values().length];
    public final Map<GearBonus, Float> bonuses = new EnumMap<>(GearBonus.class);
    public final List<Passive> passives = new ArrayList<>();
    public String ability = null;
    /** Ключ уникального предмета (для описания), или null. */
    public String unique = null;

    public int requiredLevel() { return Math.max(1, level - 3); }

    public Ability getAbility() { return ability == null ? null : Ability.byKey(ability); }

    public static Category category(ItemStack stack) {
        var item = stack.getItem();
        if (item instanceof JewelryItem) return Category.JEWEL;
        if (item instanceof ArmorItem || item instanceof ShieldItem) return Category.ARMOR;
        if (item instanceof TieredItem || item instanceof BowItem || item instanceof CrossbowItem
                || item instanceof TridentItem || item instanceof LichStaffItem) return Category.WEAPON;
        return null;
    }

    public static GearSet setOf(ItemStack stack) {
        return stack.getItem() instanceof GearSetArmorItem s ? s.set : null;
    }

    public static boolean has(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(TAG, Tag.TAG_COMPOUND);
    }

    public static GearData get(ItemStack stack) {
        if (!has(stack)) return null;
        GearData d = new GearData();
        CompoundTag t = stack.getTag().getCompound(TAG);
        d.rarity = Rarity.byId(t.getInt("r"));
        d.level = Math.max(1, t.getInt("lvl"));
        int[] st = t.getIntArray("st");
        for (int i = 0; i < Math.min(st.length, d.stats.length); i++) d.stats[i] = st[i];
        CompoundTag bo = t.getCompound("bo");
        for (String k : bo.getAllKeys()) {
            GearBonus b = GearBonus.byKey(k);
            if (b != null) d.bonuses.put(b, bo.getFloat(k));
        }
        ListTag pa = t.getList("pa", Tag.TAG_STRING);
        for (int i = 0; i < pa.size(); i++) {
            Passive p = Passive.byKey(pa.getString(i));
            if (p != null) d.passives.add(p);
        }
        if (t.contains("ab")) d.ability = t.getString("ab");
        if (t.contains("u")) d.unique = t.getString("u");
        return d;
    }

    public void write(ItemStack stack) {
        CompoundTag t = new CompoundTag();
        t.putInt("r", rarity.ordinal());
        t.putInt("lvl", level);
        t.putIntArray("st", stats.clone());
        CompoundTag bo = new CompoundTag();
        bonuses.forEach((b, v) -> bo.putFloat(b.key, v));
        t.put("bo", bo);
        ListTag pa = new ListTag();
        for (Passive p : passives) pa.add(StringTag.valueOf(p.key));
        t.put("pa", pa);
        if (ability != null) t.putString("ab", ability);
        if (unique != null) t.putString("u", unique);
        stack.getOrCreateTag().put(TAG, t);
    }

    /** Цена продажи в медных монетах (1 серебряная = 9 медных, 1 золотая = 81 медная). */
    public int value() {
        int[] mult = {1, 3, 8, 25, 80, 300};
        int base = 4 + level * 2 + bonuses.size() * 3 + passives.size() * 10;
        return base * mult[rarity.ordinal()];
    }

    /** «2 з 5 с 3 м» */
    public static String coins(int copper) {
        int g = copper / 81, s = copper % 81 / 9, c = copper % 9;
        StringBuilder sb = new StringBuilder();
        if (g > 0) sb.append(g).append(" з ");
        if (s > 0) sb.append(s).append(" с ");
        if (c > 0 || sb.length() == 0) sb.append(c).append(" м");
        return sb.toString().trim();
    }

    /** Грубая оценка «силы» предмета — для цены у торговцев. */
    public int power() {
        int p = level * 2;
        for (int v : stats) p += v * 3;
        p += bonuses.size() * 6 + passives.size() * 15 + (ability != null ? 25 : 0);
        return p;
    }
}
