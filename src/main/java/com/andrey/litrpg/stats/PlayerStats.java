package com.andrey.litrpg.stats;

import com.andrey.litrpg.ability.Ability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/** Всё, что Система знает об игроке. */
public class PlayerStats {
    public static final int MAX_LEVEL = 50;
    public static final int POINTS_PER_LEVEL = 3;
    private static final int DATA_VERSION = 2;

    private int level = 1;
    private int xp = 0;
    private int freePoints = 0;
    /** Очки, вложенные игроком вручную. */
    private final int[] spent = new int[Stat.values().length];
    private double mana = 50;
    private boolean welcomed = false;
    /** Стартовый набор уже выдан (при первом выборе роли). */
    private boolean kitGiven = false;

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
    /** Полученные награды подземелья (опыт за моба, хранителя, загадку — один раз): ключи "s1:p3#2". */
    private final Set<String> claims = new java.util.HashSet<>();
    /** Очки стиля игры до выбора класса. */
    private final Map<Style, Integer> styles = new EnumMap<>(Style.class);
    /** Классы, которые Система предложила на 10 уровне. */
    private final java.util.List<PlayerClass> offers = new java.util.ArrayList<>();

    /** Сводка надетой экипировки. Не сохраняется — пересчитывается по инвентарю. */
    private final com.andrey.litrpg.gear.PlayerGear gear = new com.andrey.litrpg.gear.PlayerGear();

    /** Время (gameTime), когда способность снова станет доступна. Не сохраняется. */
    private final long[] cooldownEnd = new long[Ability.count()];

    // ---------- уровень и опыт ----------

    public int getLevel() { return level; }
    public int getXp() { return xp; }
    public int getFreePoints() { return freePoints; }
    public boolean isWelcomed() { return welcomed; }
    public void setWelcomed(boolean w) { welcomed = w; }
    public boolean isKitGiven() { return kitGiven; }
    public void setKitGiven(boolean k) { kitGiven = k; }

    /** Опыт до следующего уровня. Опыт этапов подземелья рассчитан по этой же формуле (run/Rewards). */
    public static int xpForNextLevel(int level) {
        return 100 + (int) Math.round(90 * Math.pow(level, 1.85));
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

    /** Итоговое значение: класс + экипировка + вложенные очки. */
    public int getStat(Stat s) {
        int v = spent[s.ordinal()];
        if (playerClass != null) v += playerClass.growth(s, level);
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

    /** Ресурс класса: мана, ярость, заряды, сферы, облик или здоровье. */
    public Resource resource() {
        return playerClass == null ? Resource.MANA : playerClass.resource;
    }

    public double getResource() { return Math.min(mana, getMaxResource()); }
    public void setResource(double v) { mana = Math.max(0, Math.min(v, getMaxResource())); }
    public int getMaxResource() { return Math.max(1, resource().max(this)); }
    public double getResourceRegen() { return resource().regen(this); }

    /** Когда класс был в бою (для ярости): игровое время последнего удара. */
    public long lastCombat;

    /** Ключ надетого облика (Оборотень, Дух) или null. */
    private String form;

    public String getForm() { return form; }
    public void setForm(String key) { form = key; }

    // старые имена: ими пользуется весь код навыков, менять их незачем
    public double getMana() { return getResource(); }
    public void setMana(double m) { setResource(m); }

    public int getMaxMana() { return getMaxResource(); }

    public double getManaRegenPerSecond() { return getResourceRegen(); }

    // ---------- роль, класс ----------

    public PlayerClass getPlayerClass() { return playerClass; }
    public Role getRole() { return role; }
    public void setRole(Role r) { role = r; }

    public void setPlayerClass(PlayerClass c) {
        playerClass = c;
        form = null;          // облик чужого класса не переносится
        java.util.Arrays.fill(bar, null);
        java.util.Arrays.fill(invoked, null);
        orbs.clear();
        combo = 0;
        if (c != null) {
            role = c.role;
            fillBar();
        }
        // шкала нового класса начинается по-своему: ярость — с нуля, её копят в бою,
        // остальные — полной, иначе после смены класса нечем драться
        mana = resource() == Resource.RAGE ? 0 : getMaxResource();
    }

    // ---------- панель навыков ----------

    /** Навык в ячейке панели: у новичка — его навыки, у заклинателя сфер — сферы, Призыв и два заклинания. */
    public Ability barSkill(int slot) {
        if (slot == Ability.DASH_SLOT) return Ability.byKey(Ability.DASH_KEY);
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
    /** Роль выбрана (выбирается при первом входе). */
    public boolean hasOrigin() { return role != null; }
    public boolean hasClass() { return playerClass != null; }

    // ---------- стиль игры и выбор класса ----------

    public static final int CLASS_LEVEL = 10;

    public int getStyle(Style st) { return styles.getOrDefault(st, 0); }
    public void addStyle(Style st, int n) { if (n > 0 && playerClass == null) styles.merge(st, n, Integer::sum); }
    public Map<Style, Integer> getStyles() { return styles; }

    public java.util.List<PlayerClass> getOffers() { return offers; }
    public void setOffers(java.util.List<PlayerClass> list) { offers.clear(); offers.addAll(list); }
    public boolean canChooseClass() { return playerClass == null && level >= CLASS_LEVEL; }

    // ---------- награды подземелья ----------

    /** @return true, если награда по ключу ещё не получена (и теперь отмечена) */
    public boolean claim(String key) { return claims.add(key); }
    public boolean claimed(String key) { return claims.contains(key); }
    public void clearClaims() { claims.clear(); }

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
        if (form != null) tag.putString("form", form);
        tag.putBoolean("welcomed", welcomed);
        tag.putBoolean("kit", kitGiven);
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

        ListTag cl = new ListTag();
        for (String c : claims) cl.add(StringTag.valueOf(c));
        tag.put("claims", cl);

        CompoundTag sty = new CompoundTag();
        styles.forEach((k, v) -> sty.putInt(k.key, v));
        tag.put("styles", sty);
        ListTag of = new ListTag();
        for (PlayerClass c : offers) of.add(StringTag.valueOf(c.key));
        tag.put("offers", of);
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
        form = tag.contains("form") ? tag.getString("form") : null;
        welcomed = tag.getBoolean("welcomed");
        kitGiven = tag.getBoolean("kit");
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

        claims.clear();
        ListTag cl = tag.getList("claims", Tag.TAG_STRING);
        for (int i = 0; i < cl.size(); i++) claims.add(cl.getString(i));

        styles.clear();
        CompoundTag sty = tag.getCompound("styles");
        for (Style st : Style.values()) if (sty.contains(st.key)) styles.put(st, sty.getInt(st.key));
        offers.clear();
        ListTag of = tag.getList("offers", Tag.TAG_STRING);
        for (int i = 0; i < of.size(); i++) {
            PlayerClass c = PlayerClass.byKey(of.getString(i));
            if (c != null) offers.add(c);
        }
        if (tag.contains("gear")) gear.load(tag.getCompound("gear"));
    }

    public com.andrey.litrpg.gear.PlayerGear gear() { return gear; }

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
