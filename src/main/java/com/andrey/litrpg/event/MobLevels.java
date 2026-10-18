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
 * Уровни, ранги и свойства мобов. Уровень зависит от зоны (расстояние от
 * точки спавна, измерение), ночью монстры на 1–2 уровня сильнее.
 * Ранги: обычный, элитный (≈8%), чемпион (≈1.5%) со случайными свойствами.
 */
public class MobLevels {
    public static final String TAG = "litrpg_lvl";
    public static final String RANK_TAG = "litrpg_rank";
    public static final String AFFIX_TAG = "litrpg_affix";
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

    /** Кому положен уровень: монстрам, нейтральным мобам и големам. */
    public static boolean hasLevels(Mob mob) {
        return mob instanceof Enemy || mob instanceof com.andrey.litrpg.npc.RaceNpcEntity || mob instanceof net.minecraft.world.entity.NeutralMob
                || mob instanceof net.minecraft.world.entity.animal.AbstractGolem;
    }

    public static void assignLevel(Mob mob, ServerLevel level) {
        if (!hasLevels(mob) || mob instanceof LitBoss) return;
        if (mob.getPersistentData().contains(TAG)) {
            // мобы из старых версий мода показывали имя сквозь стены
            mob.setCustomNameVisible(false);
            return;
        }

        RandomSource rnd = mob.getRandom();
        Zone zone = Zone.at(level, mob.blockPosition());
        int lvl = zone.minLevel + rnd.nextInt(Math.max(1, zone.maxLevel - zone.minLevel + 1));
        if (level.isNight() && level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            lvl += rnd.nextInt(3);
        }
        boolean bloodMoon = com.andrey.litrpg.world.BloodMoon.isActive(level);
        if (bloodMoon) lvl += 4;
        Rank rank = Rank.NORMAL;
        float roll = mob instanceof Enemy ? rnd.nextFloat() / (bloodMoon ? 3f : 1f) : 1f;
        if (roll < 0.015f + zone.ordinal() * 0.004f) rank = Rank.CHAMPION;
        else if (roll < 0.08f + zone.ordinal() * 0.012f) rank = Rank.ELITE;
        apply(mob, Mth.clamp(lvl, 1, 99), rank);
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

        if (!mob.hasCustomName()) {
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
        if (killed instanceof Enemy || killed instanceof com.andrey.litrpg.npc.RaceNpcEntity) {
            base = 6 + killed.getMaxHealth() / getRank(killed).hp * 0.6;
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
