package com.andrey.litrpg.event;

import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.world.Zone;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

import java.util.UUID;

/**
 * Уровни, ранги и свойства мобов. У каждого вида свой уровень: базовый ±5
 * (таблица в /litrpg_bestiary.json — {@link com.andrey.litrpg.entity.wild.Species#band}).
 * Внутри полосы уровень тянется к уровню зоны; ночью монстры на 1–2 уровня сильнее.
 * Ранги: обычный, элитный (≈8%), чемпион (≈1.5%) со случайными свойствами.
 */
public class MobLevels {
    public static final String TAG = "litrpg_lvl";
    public static final String RANK_TAG = "litrpg_rank";
    public static final String AFFIX_TAG = "litrpg_affix";
    /** Уровень подземелья, в котором стоит моб (ставится в структуре). */
    public static final String DUNGEON_TAG = "litrpg_dlvl";
    /** Запас прочности сверх предела Майнкрафта (1024 ед. здоровья): во сколько раз делится входящий урон. */
    public static final String TOUGH_TAG = "litrpg_tough";
    private static final UUID HP_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1101");
    private static final UUID DMG_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1102");
    private static final UUID ARMOR_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1103");
    private static final UUID TOUGH_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1104");
    private static final UUID SPEED_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1105");
    private static final UUID KB_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1106");
    private static final UUID FOLLOW_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1107");

    public enum Rank {
        NORMAL("", ChatFormatting.WHITE, 1.0, 1.0, 1),
        ELITE("★ Элитный ", ChatFormatting.AQUA, 2.2, 1.35, 3),
        CHAMPION("✦ Чемпион ", ChatFormatting.LIGHT_PURPLE, 4.0, 1.7, 8);

        public final String prefix;
        public final ChatFormatting color;
        public final double hp;
        public final double dmg;
        public final int xpMult;

        Rank(String prefix, ChatFormatting color, double hp, double dmg, int xpMult) {
            this.prefix = prefix;
            this.color = color;
            this.hp = hp;
            this.dmg = dmg;
            this.xpMult = xpMult;
        }
    }

    /** Свойства элитных мобов и чемпионов. */
    public enum Affix {
        VAMPIRIC("Вампир"),
        EXPLOSIVE("Взрывной"),
        SWIFT("Быстрый"),
        REGENERATING("Регенерирующий"),
        STONESKIN("Каменнокожий"),
        BURNING("Пылающий"),
        VENOMOUS("Ядовитый"),
        THORNS("Шипастый");

        public final String title;

        Affix(String title) { this.title = title; }
    }

    public static int getLevel(LivingEntity e) {
        if (e instanceof LitBoss boss) return boss.getBossLevel();
        int lvl = e.getPersistentData().getInt(TAG);
        return lvl <= 0 ? 1 : lvl;
    }

    public static Rank getRank(LivingEntity e) {
        int r = e.getPersistentData().getInt(RANK_TAG);
        return r >= 0 && r < Rank.values().length ? Rank.values()[r] : Rank.NORMAL;
    }

    public static boolean hasAffix(LivingEntity e, Affix a) {
        return (e.getPersistentData().getInt(AFFIX_TAG) & (1 << a.ordinal())) != 0;
    }

    /** Полоса уровней вида: базовый ±5. */
    public static final int BAND = 5;

    /** Кому положен уровень: монстрам, нейтральным мобам, големам и зверям бестиария. */
    public static boolean hasLevels(Mob mob) {
        return mob instanceof Enemy || mob instanceof com.andrey.litrpg.npc.RaceNpcEntity || mob instanceof net.minecraft.world.entity.NeutralMob
                || mob instanceof net.minecraft.world.entity.animal.AbstractGolem || mob instanceof com.andrey.litrpg.entity.wild.WildMob;
    }

    /** Уровень вида в этой зоне: уровень зоны, зажатый в полосу вида (base±5). */
    public static int bandLevel(int base, int zoneLevel, RandomSource rnd) {
        return Mth.clamp(zoneLevel + rnd.nextInt(3) - 1, Math.max(1, base - BAND), base + BAND);
    }

    /** Ранг по случаю: чем опаснее уровень, тем чаще элиты; в Кровавую луну — втрое чаще. */
    public static Rank rollRank(Mob mob, int lvl, boolean bloodMoon) {
        int tier = Math.min(7, lvl / 8);
        float roll = mob instanceof Enemy ? mob.getRandom().nextFloat() / (bloodMoon ? 3f : 1f) : 1f;
        if (roll < 0.015f + tier * 0.004f) return Rank.CHAMPION;
        if (roll < 0.08f + tier * 0.012f) return Rank.ELITE;
        return Rank.NORMAL;
    }

    public static void assignLevel(Mob mob, ServerLevel level) {
        if (!hasLevels(mob) || mob instanceof LitBoss) return;
        // стража городов: уровень 80 без масштабирования (она и так почти неуязвима)
        if (mob instanceof com.andrey.litrpg.npc.RaceNpcEntity npc && npc.npcType().isGuard()) {
            if (!mob.getPersistentData().contains(TAG)) {
                mob.getPersistentData().putInt(TAG, 80);
                npc.ensureName();
                npc.refreshName();
            }
            return;
        }
        if (mob.getPersistentData().contains(TAG)) {
            // мобы из старых версий мода показывали имя сквозь стены
            mob.setCustomNameVisible(false);
            return;
        }

        RandomSource rnd = mob.getRandom();
        // обитатели подземелий: уровень задан самим подземельем, элиты встречаются чаще
        int dlvl = mob.getPersistentData().getInt(DUNGEON_TAG);
        if (dlvl > 0) {
            mob.setPersistenceRequired();
            float r = rnd.nextFloat();
            Rank dr = r < 0.04f ? Rank.CHAMPION : r < 0.2f ? Rank.ELITE : Rank.NORMAL;
            apply(mob, Mth.clamp(dlvl - 1 + rnd.nextInt(3), 1, 99), dr);
            return;
        }
        int[] range = Zone.levels(level, mob.blockPosition());
        int lvl = range[0] + rnd.nextInt(Math.max(1, range[1] - range[0] + 1));
        if (level.isNight() && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            lvl += rnd.nextInt(3);
        }
        // свой уровень у каждого вида: зомби — 1–9, эндермен — 30–40, где бы ни появился
        Integer base = mob instanceof com.andrey.litrpg.npc.RaceNpcEntity npc && !npc.npcType().outlaw() ? null
                : com.andrey.litrpg.entity.wild.Species.band(mob.getType());
        if (base != null) lvl = bandLevel(base, lvl, rnd);
        boolean bloodMoon = com.andrey.litrpg.world.BloodMoon.isActive(level);
        if (bloodMoon) lvl += 4;
        apply(mob, Mth.clamp(lvl, 1, 99), rollRank(mob, lvl, bloodMoon));
    }

    /** Выставить уровень и ранг (в том числе командой /litrpg spawn). */
    public static void apply(Mob mob, int lvl, Rank rank) {
        RandomSource rnd = mob.getRandom();
        mob.getPersistentData().putInt(TAG, lvl);
        mob.getPersistentData().putInt(RANK_TAG, rank.ordinal());
        int affixes = 0;
        int count = rank == Rank.CHAMPION ? 2 : rank == Rank.ELITE ? 1 : 0;
        for (int i = 0; i < count; i++) affixes |= 1 << rnd.nextInt(Affix.values().length);
        mob.getPersistentData().putInt(AFFIX_TAG, affixes);
        applyScaling(mob, lvl, rank);
    }

    private static void applyScaling(Mob mob, int lvl, Rank rank) {
        int l = lvl - 1;
        mod(mob, Attributes.MAX_HEALTH, HP_ID, (1 + 0.30 * l) * rank.hp - 1, AttributeModifier.Operation.MULTIPLY_BASE);
        AttributeInstance hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        double wanted = hpAttr == null ? 0 : hpAttr.getBaseValue() * (1 + 0.30 * l) * rank.hp;
        if (wanted > 1024) mob.getPersistentData().putFloat(TOUGH_TAG, (float) (wanted / 1024));
        else mob.getPersistentData().remove(TOUGH_TAG);
        mod(mob, Attributes.ATTACK_DAMAGE, DMG_ID, (1 + 0.12 * l) * rank.dmg - 1, AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.ARMOR, ARMOR_ID, Math.min(20, l * 0.5), AttributeModifier.Operation.ADDITION);
        mod(mob, Attributes.ARMOR_TOUGHNESS, TOUGH_ID, Math.min(10, l * 0.15), AttributeModifier.Operation.ADDITION);
        double speed = Math.min(0.25, l * 0.005) + (hasAffix(mob, Affix.SWIFT) ? 0.35 : 0);
        mod(mob, Attributes.MOVEMENT_SPEED, SPEED_ID, speed, AttributeModifier.Operation.MULTIPLY_BASE);
        mod(mob, Attributes.KNOCKBACK_RESISTANCE, KB_ID, Math.min(0.6, l * 0.01) + (rank != Rank.NORMAL ? 0.3 : 0),
                AttributeModifier.Operation.ADDITION);
        mod(mob, Attributes.FOLLOW_RANGE, FOLLOW_ID, Math.min(1.0, l * 0.02), AttributeModifier.Operation.MULTIPLY_BASE);
        mob.setHealth(mob.getMaxHealth());

        int inf = MobEffectInstance.INFINITE_DURATION;
        if (hasAffix(mob, Affix.REGENERATING)) mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, inf, 0, false, true));
        if (hasAffix(mob, Affix.STONESKIN)) mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, inf, 1, false, true));
        if (hasAffix(mob, Affix.BURNING)) mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0, false, true));
        if (rank != Rank.NORMAL) mob.setPersistenceRequired();

        if (mob instanceof com.andrey.litrpg.npc.RaceNpcEntity npc && !npc.personalName().isEmpty()) {
            npc.refreshName();
        } else if (!mob.hasCustomName()) {
            mob.setCustomName(displayName(mob, lvl, rank));
            // имя видно только при взгляде прямо на моба (как у ванильных именованных мобов)
            mob.setCustomNameVisible(false);
        }
    }

    public static MutableComponent displayName(Mob mob, int lvl, Rank rank) {
        ChatFormatting color = lvl < 10 ? ChatFormatting.GRAY : lvl < 20 ? ChatFormatting.YELLOW
                : lvl < 35 ? ChatFormatting.GOLD : lvl < 50 ? ChatFormatting.RED : ChatFormatting.DARK_RED;
        MutableComponent name = Component.literal("[Ур. " + lvl + "] ").withStyle(color);
        if (rank != Rank.NORMAL) {
            StringBuilder sb = new StringBuilder(rank.prefix);
            for (Affix a : Affix.values()) if (hasAffix(mob, a)) sb.append(a.title).append(' ');
            name.append(Component.literal(sb.toString()).withStyle(rank.color));
        }
        return name.append(Component.empty().append(mob.getType().getDescription())
                .withStyle(rank == Rank.NORMAL ? ChatFormatting.WHITE : rank.color));
    }

    private static void mod(Mob mob, Attribute attr, UUID id, double value, AttributeModifier.Operation op) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        if (value != 0) inst.addPermanentModifier(new AttributeModifier(id, "litrpg_mob", value, op));
    }

    /** Опыт Системы за убийство. */
    public static int xpReward(LivingEntity killed, int playerLevel) {
        if (killed instanceof LitBoss boss) return boss.getXpReward();
        int lvl = getLevel(killed);
        double base;
        if (killed instanceof Enemy || killed instanceof com.andrey.litrpg.npc.RaceNpcEntity
                || killed instanceof com.andrey.litrpg.entity.wild.WildMob w && w.spec().temper != com.andrey.litrpg.entity.wild.Species.Temper.TIMID) {
            base = 6 + killed.getMaxHealth() * Math.max(1f, killed.getPersistentData().getFloat(TOUGH_TAG)) / getRank(killed).hp * 0.6;
            base *= 1.0 + 0.22 * (lvl - 1);
            base *= getRank(killed).xpMult;
        } else {
            base = 1 + killed.getMaxHealth() * 0.1;
        }
        int diff = playerLevel - lvl;
        if (diff > 10) base *= 0.1;
        else if (diff > 5) base *= 0.5;
        else if (diff < -3) base *= 1.0 + Math.min(1.5, (-diff - 3) * 0.15);
        return Math.max(1, (int) Math.round(base));
    }
}
