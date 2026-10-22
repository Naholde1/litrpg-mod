package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Всё, что Система знает об игроке. */
public class PlayerStats {
    public static final int MAX_LEVEL = 100;
    public static final int POINTS_PER_LEVEL = 3;
    private static final int DATA_VERSION = 2;

    private int level = 1;
    private int xp = 0;
    private int freePoints = 0;
    /** Очки, вложенные игроком вручную. */
    private final int[] spent = new int[Stat.values().length];
    private double mana = 50;
    private boolean welcomed = false;

    private Race race = null;
    private Role role = null;
    private PlayerClass playerClass = null;
    /** Панель навыков: ключи навыков по ячейкам (у заклинателя сфер — не используется). */
    private final String[] bar = new String[Ability.BAR];
    /** Сферы заклинателя (номера 0–2, до трёх) и два сплетённых заклинания. */
    private final java.util.List<Integer> orbs = new java.util.ArrayList<>();
    private final String[] invoked = new String[2];
    /** Приёмы серии (не сохраняются). */
    private int combo = 0;
    private long comboTime = 0;
    private final Map<Profession, int[]> professions = new EnumMap<>(Profession.class); // {level, xp}
    private final Set<Title> titles = EnumSet.noneOf(Title.class);
    /** Открытые места Мира Системы (id из карты). */
    private final Set<String> found = new java.util.HashSet<>();
    private final Map<Title.Counter, Integer> counters = new EnumMap<>(Title.Counter.class);
    /** Очки стиля игры до выбора класса. */
    private final Map<Style, Integer> styles = new EnumMap<>(Style.class);
    /** Классы, которые Система предложила на 10 уровне. */
    private final java.util.List<PlayerClass> offers = new java.util.ArrayList<>();

    /** Сводка надетой экипировки. Не сохраняется — пересчитывается по инвентарю. */
    private final com.andrey.litrpg.gear.PlayerGear gear = new com.andrey.litrpg.gear.PlayerGear();
    /** Репутация с народами: -1000..1000. */
    private final Map<com.andrey.litrpg.rep.Faction, Integer> reputation = new EnumMap<>(com.andrey.litrpg.rep.Faction.class);

    /** Журнал заданий. */
    private final com.andrey.litrpg.quest.QuestLog quests = new com.andrey.litrpg.quest.QuestLog();

    /** Время (gameTime), когда способность снова станет доступна. Не сохраняется. */
    private final long[] cooldownEnd = new long[Ability.count()];

    // ---------- уровень и опыт ----------

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getFreePoints() { return freePoints; }
    public boolean isWelcomed() { return welcomed; }
    public void setWelcomed(boolean w) { welcomed = w; }

    /** Опыт до следующего уровня (с 2.15 — примерно вдвое медленнее, к сотому уровню — втрое). */
    public static int xpForNextLevel(int level) {
        return 100 + (int) Math.round(90 * Math.pow(level, 1.85));
    }

    public int xpForNext() { return xpForNextLevel(level); }

    /** @return сколько уровней получено */
    public int addXp(int amount) {
        if (level >= MAX_LEVEL) return 0;
        if (race == Race.HUMAN) amount = (int) Math.ceil(amount * 1.10);
        amount = (int) Math.ceil(amount * (1 + gear.get(com.andrey.litrpg.gear.GearBonus.XP) / 100.0));
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

    /** Для команд: выставить уровень, пересчитав свободные очки. */
    public void setLevel(int newLevel) {
        newLevel = Math.max(1, Math.min(MAX_LEVEL, newLevel));
        int totalPoints = (newLevel - 1) * POINTS_PER_LEVEL;
        int used = 0;
        for (int v : spent) used += v;
        if (used > totalPoints) {
            java.util.Arrays.fill(spent, 0);
            used = 0;
        }
        level = newLevel;
        xp = 0;
        freePoints = totalPoints - used;
    }

    public void setXp(int xp) { this.xp = Math.max(0, Math.min(xp, xpForNext() - 1)); }
    public void setFreePoints(int p) { freePoints = Math.max(0, p); }

    public void loseXpPercent(double percent) {
        xp = (int) Math.max(0, xp - xp * percent);
    }

    // ---------- характеристики ----------

    /** Итоговое значение: раса + класс + титулы + вложенные очки. */
    public int getStat(Stat s) {
        int v = spent[s.ordinal()];
        if (race != null) v += race.bonus(s);
        if (playerClass != null) v += playerClass.growth(s, level);
        for (Title t : titles) v += t.bonus(s);
        v += gear.stats[s.ordinal()];
        return Math.max(0, v);
    }

    public int getSpent(Stat s) { return spent[s.ordinal()]; }
    public void setSpent(Stat s, int v) { spent[s.ordinal()] = Math.max(0, v); }

    public boolean spendPoint(Stat s) {
        if (freePoints <= 0) return false;
        freePoints--;
        spent[s.ordinal()]++;
        return true;
    }

    /** Сбросить вложенные очки (Свиток забвения, команда). */
    public void respec() {
        int total = 0;
        for (int i = 0; i < spent.length; i++) {
            total += spent[i];
            spent[i] = 0;
        }
        freePoints += total;
    }

    // ---------- мана ----------

    public double getMana() { return Math.min(mana, getMaxMana()); }
    public void setMana(double m) { mana = Math.max(0, Math.min(m, getMaxMana())); }

    public int getMaxMana() {
        return (int) Math.round(StatBreakdown.maxMana(this).value());
    }

    public double getManaRegenPerSecond() {
        return StatBreakdown.manaRegen(this).value();
    }

    // ---------- раса, класс ----------

    public Race getRace() { return race; }
    public PlayerClass getPlayerClass() { return playerClass; }
    public void setRace(Race r) { race = r; }
    public Role getRole() { return role; }
    public void setRole(Role r) { role = r; }

    public void setPlayerClass(PlayerClass c) {
        playerClass = c;
        java.util.Arrays.fill(bar, null);
        java.util.Arrays.fill(invoked, null);
        orbs.clear();
        combo = 0;
        if (c != null) {
            role = c.role;
            fillBar();
        }
    }

    // ---------- панель навыков ----------

    /** Навык в ячейке панели: у новичка — его навыки, у заклинателя сфер — сферы, Призыв и два заклинания. */
    public Ability barSkill(int slot) {
        if (slot < 0 || slot >= Ability.BAR) return null;
        PlayerClass c = playerClass;
        if (c == null) return slot < Ability.NOVICE.length ? Ability.byKey(Ability.NOVICE[slot]) : null;
        if (c.isInvoker()) {
            if (slot < 3) return Ability.byKey(c.invoke.spheres()[slot]);
            if (slot == 3) return Ability.byKey(c.invoke.cast());
            return Ability.byKey(invoked[slot - 4]);
        }
        return Ability.byKey(bar[slot]);
    }

    public String getBar(int slot) { return bar[slot]; }

    /** Поставить навык класса в ячейку (null — убрать). Навык уходит из прежней ячейки. */
    public boolean setBar(int slot, String key) {
        if (slot < 0 || slot >= Ability.BAR || playerClass == null || playerClass.isInvoker()) return false;
        if (key != null) {
            if (!isUnlocked(Ability.byKey(key))) return false;
            for (int i = 0; i < bar.length; i++) if (key.equals(bar[i])) bar[i] = bar[slot];
        }
        bar[slot] = key;
        return true;
    }

    /** Навык открыт: он есть у класса (или новичка) и уровень достаточен. */
    public boolean isUnlocked(Ability a) {
        return a != null && level >= Ability.unlockLevel(playerClass, a);
    }

    /** Разложить открытые навыки класса по пустым ячейкам. @return новые навыки, не поместившиеся на панель */
    public java.util.List<Ability> fillBar() {
        java.util.List<Ability> left = new java.util.ArrayList<>();
        if (playerClass == null || playerClass.isInvoker()) return left;
        for (String k : playerClass.skills) {
            Ability a = Ability.byKey(k);
            if (!isUnlocked(a) || java.util.Arrays.asList(bar).contains(k)) continue;
            int free = -1;
            for (int i = 0; i < bar.length && free < 0; i++) if (bar[i] == null) free = i;
            if (free >= 0) bar[free] = k;
            else left.add(a);
        }
        return left;
    }

    public java.util.List<Integer> getOrbs() { return orbs; }

    public void addOrb(int sphere) {
        orbs.add(sphere);
        while (orbs.size() > 3) orbs.remove(0);
    }

    public String getInvoked(int i) { return invoked[i]; }

    /** Положить сплетённое заклинание в первую ячейку, прежнее сдвинуть во вторую. */
    public void pushInvoked(String key) {
        if (key.equals(invoked[0])) return;
        if (key.equals(invoked[1])) invoked[1] = invoked[0];
        else invoked[1] = invoked[0];
        invoked[0] = key;
    }

    // ---------- приёмы серии ----------

    public int getCombo(long now) {
        if (now - comboTime > 200) combo = 0; // серия гаснет через 10 с
        return combo;
    }

    public void addCombo(int n, long now) {
        combo = Math.min(5, getCombo(now) + n);
        comboTime = now;
    }

    public void clearCombo() { combo = 0; }

    /** Для интерфейса: приёмы серии как есть. */
    public int comboPoints() { return combo; }
    /** Раса выбрана (выбирается при первом входе). */
    public boolean hasOrigin() { return race != null; }
    public boolean hasClass() { return playerClass != null; }

    // ---------- стиль игры и выбор класса ----------

    public static final int CLASS_LEVEL = 10;

    public int getStyle(Style st) { return styles.getOrDefault(st, 0); }
    public void addStyle(Style st, int n) { if (n > 0 && playerClass == null) styles.merge(st, n, Integer::sum); }
    public Map<Style, Integer> getStyles() { return styles; }

    public java.util.List<PlayerClass> getOffers() { return offers; }
    public void setOffers(java.util.List<PlayerClass> list) { offers.clear(); offers.addAll(list); }
    public boolean canChooseClass() { return playerClass == null && level >= CLASS_LEVEL; }

    // ---------- профессии ----------

    public boolean knows(Profession p) { return professions.containsKey(p); }
    public Set<Profession> getProfessions() { return professions.keySet(); }

    public boolean learn(Profession p) {
        if (knows(p) || professions.size() >= Profession.MAX_KNOWN) return false;
        professions.put(p, new int[]{1, 0});
        return true;
    }

    public void forget(Profession p) { professions.remove(p); }

    public int getProfLevel(Profession p) {
        int[] d = professions.get(p);
        return d == null ? 0 : d[0];
    }

    public int getProfXp(Profession p) {
        int[] d = professions.get(p);
        return d == null ? 0 : d[1];
    }

    public void setProfLevel(Profession p, int lvl) {
        int[] d = professions.get(p);
        if (d == null) return;
        d[0] = Math.max(1, Math.min(Profession.MAX_LEVEL, lvl));
        d[1] = 0;
    }

    /** @return сколько уровней профессии получено */
    public int addProfXp(Profession p, int amount) {
        int[] d = professions.get(p);
        if (d == null || d[0] >= Profession.MAX_LEVEL) return 0;
        d[1] += amount;
        int gained = 0;
        while (d[0] < Profession.MAX_LEVEL && d[1] >= Profession.xpForNext(d[0])) {
            d[1] -= Profession.xpForNext(d[0]);
            d[0]++;
            gained++;
        }
        return gained;
    }

    // ---------- титулы и счётчики ----------

    public Set<Title> getTitles() { return titles; }
    public boolean hasTitle(Title t) { return titles.contains(t); }

    /** @return true, если место открыто впервые */
    public boolean discover(String id) { return found.add(id); }

    public int foundCount() { return found.size(); }
    public void addTitle(Title t) { titles.add(t); }
    public void removeTitle(Title t) { titles.remove(t); }

    public int getCounter(Title.Counter c) { return counters.getOrDefault(c, 0); }
    public void addCounter(Title.Counter c, int n) { counters.merge(c, n, Integer::sum); }

    // ---------- перезарядки ----------

    public long getCooldownEnd(Ability a) { return cooldownEnd[a.ordinal()]; }
    public void setCooldownEnd(Ability a, long t) { cooldownEnd[a.ordinal()] = t; }
    public void resetCooldowns() { java.util.Arrays.fill(cooldownEnd, 0); }

    // ---------- сохранение ----------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("version", DATA_VERSION);
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("points", freePoints);
        CompoundTag st = new CompoundTag();
        for (Stat s : Stat.values()) st.putInt(s.key, spent[s.ordinal()]);
        tag.put("spent", st);
        tag.putDouble("mana", mana);
        tag.putBoolean("welcomed", welcomed);
        if (race != null) tag.putString("race", race.key);
        if (role != null) tag.putString("role", role.key);
        if (playerClass != null) tag.putString("class", playerClass.key);
        ListTag bl = new ListTag();
        for (String b : bar) bl.add(StringTag.valueOf(b == null ? "" : b));
        tag.put("bar", bl);
        tag.putIntArray("orbs", orbs.stream().mapToInt(Integer::intValue).toArray());
        ListTag il = new ListTag();
        for (String b : invoked) il.add(StringTag.valueOf(b == null ? "" : b));
        tag.put("invoked", il);
        tag.putInt("combo", combo);

        CompoundTag prof = new CompoundTag();
        professions.forEach((p, d) -> prof.putIntArray(p.key, d.clone()));
        tag.put("professions", prof);

        ListTag tl = new ListTag();
        for (Title t : titles) tl.add(StringTag.valueOf(t.key));
        tag.put("titles", tl);

        CompoundTag cnt = new CompoundTag();
        counters.forEach((c, v) -> cnt.putInt(c.key(), v));
        tag.put("counters", cnt);

        CompoundTag sty = new CompoundTag();
        styles.forEach((k, v) -> sty.putInt(k.key, v));
        tag.put("styles", sty);
        ListTag of = new ListTag();
        for (PlayerClass c : offers) of.add(StringTag.valueOf(c.key));
        tag.put("offers", of);
        CompoundTag rep = new CompoundTag();
        reputation.forEach((f, v) -> rep.putInt(f.key, v));
        tag.put("rep", rep);
        tag.put("quests", quests.save());
        ListTag fl = new ListTag();
        for (String f : found) fl.add(StringTag.valueOf(f));
        tag.put("found", fl);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = tag.getInt("xp");
        freePoints = tag.getInt("points");
        java.util.Arrays.fill(spent, 0);
        if (tag.contains("spent", Tag.TAG_COMPOUND)) {
            CompoundTag st = tag.getCompound("spent");
            for (Stat s : Stat.values()) spent[s.ordinal()] = st.getInt(s.key);
        } else {
            // данные версии 1.0.0: очки лежали прямо в корне
            for (Stat s : Stat.values()) spent[s.ordinal()] = tag.getInt(s.key);
        }
        mana = tag.getDouble("mana");
        welcomed = tag.getBoolean("welcomed");
        race = Race.byKey(tag.getString("race"));
        role = Role.byKey(tag.getString("role"));
        playerClass = PlayerClass.byKey(tag.getString("class"));
        if (role == null && playerClass != null) role = playerClass.role;
        java.util.Arrays.fill(bar, null);
        ListTag bl = tag.getList("bar", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(bl.size(), bar.length); i++) {
            String k = bl.getString(i);
            bar[i] = k.isEmpty() || Ability.byKey(k) == null ? null : k;
        }
        orbs.clear();
        for (int o : tag.getIntArray("orbs")) if (o >= 0 && o < 3) orbs.add(o);
        java.util.Arrays.fill(invoked, null);
        ListTag il = tag.getList("invoked", Tag.TAG_STRING);
        for (int i = 0; i < Math.min(il.size(), invoked.length); i++) {
            String k = il.getString(i);
            invoked[i] = k.isEmpty() || Ability.byKey(k) == null ? null : k;
        }
        combo = tag.getInt("combo");
        // персонажи до 2.15: навыки раскладываются на панель сами
        if (playerClass != null && !tag.contains("bar")) fillBar();

        professions.clear();
        CompoundTag prof = tag.getCompound("professions");
        for (String k : prof.getAllKeys()) {
            Profession p = Profession.byKey(k);
            int[] d = prof.getIntArray(k);
            if (p != null && d.length == 2) professions.put(p, d.clone());
        }

        found.clear();
        ListTag fl = tag.getList("found", Tag.TAG_STRING);
        for (int i = 0; i < fl.size(); i++) found.add(fl.getString(i));

        titles.clear();
        ListTag tl = tag.getList("titles", Tag.TAG_STRING);
        for (int i = 0; i < tl.size(); i++) {
            Title t = Title.byKey(tl.getString(i));
            if (t != null) titles.add(t);
        }

        counters.clear();
        CompoundTag cnt = tag.getCompound("counters");
        for (Title.Counter c : Title.Counter.values()) {
            if (cnt.contains(c.key())) counters.put(c, cnt.getInt(c.key()));
        }

        styles.clear();
        CompoundTag sty = tag.getCompound("styles");
        for (Style st : Style.values()) if (sty.contains(st.key)) styles.put(st, sty.getInt(st.key));
        offers.clear();
        ListTag of = tag.getList("offers", Tag.TAG_STRING);
        for (int i = 0; i < of.size(); i++) {
            PlayerClass c = PlayerClass.byKey(of.getString(i));
            if (c != null) offers.add(c);
        }
        reputation.clear();
        CompoundTag rep = tag.getCompound("rep");
        for (var f : com.andrey.litrpg.rep.Faction.values()) if (rep.contains(f.key)) reputation.put(f, rep.getInt(f.key));
        if (tag.contains("gear")) gear.load(tag.getCompound("gear"));
        quests.load(tag.getCompound("quests"));
    }

    public com.andrey.litrpg.quest.QuestLog quests() { return quests; }

    public com.andrey.litrpg.gear.PlayerGear gear() { return gear; }

    // ---------- репутация ----------

    public int getRep(com.andrey.litrpg.rep.Faction f) { return reputation.getOrDefault(f, 0); }
    public void setRep(com.andrey.litrpg.rep.Faction f, int v) { reputation.put(f, Math.max(-1000, Math.min(1000, v))); }
    public void addRep(com.andrey.litrpg.rep.Faction f, int v) { setRep(f, getRep(f) + v); }

    /** Данные для клиента: сохранение + сводка экипировки. */
    public CompoundTag saveForSync() {
        CompoundTag t = save();
        t.put("gear", gear.save());
        return t;
    }

    public void copyFrom(PlayerStats other) {
        load(other.save());
    }

    /** Полный сброс персонажа (команда /litrpg reset). */
    public void wipe() {
        load(new CompoundTag());
        level = 1;
        mana = getMaxMana();
        welcomed = true;
    }
}
