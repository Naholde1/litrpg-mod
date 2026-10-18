package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;
import net.minecraft.nbt.CompoundTag;

/** Всё, что Система знает об игроке. */
public class PlayerStats {
    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;

    private int level = 1;
    private int xp = 0;
    private int freePoints = 0;
    private final int[] stats = new int[Stat.values().length];
    private double mana = 50;
    private boolean welcomed = false;
    /** Время (gameTime), когда способность снова станет доступна. Не сохраняется. */
    private final long[] cooldownEnd = new long[Ability.values().length];

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getFreePoints() { return freePoints; }
    public int getStat(Stat s) { return stats[s.ordinal()]; }
    public double getMana() { return mana; }
    public boolean isWelcomed() { return welcomed; }
    public void setWelcomed(boolean w) { welcomed = w; }

    public void setMana(double m) { mana = Math.max(0, Math.min(m, getMaxMana())); }

    public int getMaxMana() {
        return 50 + getStat(Stat.INTELLIGENCE) * 10 + (level - 1) * 2;
    }

    public double getManaRegenPerSecond() {
        return 1.0 + getStat(Stat.INTELLIGENCE) * 0.25 + level * 0.05;
    }

    public static int xpForNextLevel(int level) {
        return 60 + (int) Math.round(40 * Math.pow(level, 1.6));
    }

    public int xpForNext() { return xpForNextLevel(level); }

    /** @return сколько уровней получено */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        xp += amount;
        int gained = 0;
        while (level < MAX_LEVEL && xp >= xpForNext()) {
            xp -= xpForNext();
            level++;
            freePoints += POINTS_PER_LEVEL;
            gained++;
        }
        if (level >= MAX_LEVEL) xp = 0;
        return gained;
    }

    public void loseXpPercent(double percent) {
        xp = (int) Math.max(0, xp - xp * percent);
    }

    public boolean spendPoint(Stat s) {
        if (freePoints <= 0) return false;
        freePoints--;
        stats[s.ordinal()]++;
        return true;
    }

    public long getCooldownEnd(Ability a) { return cooldownEnd[a.ordinal()]; }
    public void setCooldownEnd(Ability a, long t) { cooldownEnd[a.ordinal()] = t; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("points", freePoints);
        for (Stat s : Stat.values()) tag.putInt(s.key, stats[s.ordinal()]);
        tag.putDouble("mana", mana);
        tag.putBoolean("welcomed", welcomed);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = tag.getInt("xp");
        freePoints = tag.getInt("points");
        for (Stat s : Stat.values()) stats[s.ordinal()] = tag.getInt(s.key);
        mana = tag.getDouble("mana");
        welcomed = tag.getBoolean("welcomed");
    }

    public void copyFrom(PlayerStats other) {
        load(other.save());
    }
}
