package com.andrey.litrpg.gear;

import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.event.MobLevels;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.raid.Raider;

/** Семейства существ для бонусов «урон по…» и «защита от…». */
public final class Families {
    private Families() {}

    /** Множитель урона игрока по цели от бонусов экипировки. */
    public static float outgoing(PlayerGear g, LivingEntity t) {
        float pct = 0;
        if (t.getMobType() == MobType.UNDEAD) pct += g.get(GearBonus.DMG_UNDEAD);
        if (t.getMobType() == MobType.ARTHROPOD) pct += g.get(GearBonus.DMG_ARTHROPOD);
        if (t instanceof AbstractIllager || t instanceof Raider) pct += g.get(GearBonus.DMG_ILLAGER);
        if (t instanceof Animal || t instanceof net.minecraft.world.entity.animal.Wolf
                || t instanceof com.andrey.litrpg.entity.wild.WildMob w && w.spec().beast) pct += g.get(GearBonus.DMG_BEAST);
        if (isNether(t)) pct += g.get(GearBonus.DMG_NETHER);
        if (t instanceof LitBoss || MobLevels.getRank(t) != MobLevels.Rank.NORMAL) pct += g.get(GearBonus.DMG_ELITE);
        if (t instanceof AbstractVillager || t instanceof AbstractIllager) pct += g.get(GearBonus.DMG_HUMANOID);
        return 1f + pct / 100f;
    }

    /** Доля урона, которую срезает защита экипировки (0..0.75). */
    public static float incomingReduction(PlayerGear g, DamageSource src) {
        float pct = 0;
        if (src.getEntity() instanceof LivingEntity a) {
            if (a.getMobType() == MobType.UNDEAD) pct += g.get(GearBonus.RES_UNDEAD);
            if (a.getMobType() == MobType.ARTHROPOD) pct += g.get(GearBonus.RES_ARTHROPOD);
            if (a instanceof AbstractIllager || a instanceof Raider) pct += g.get(GearBonus.RES_ILLAGER);
        }
        if (src.is(DamageTypeTags.IS_FIRE)) pct += g.get(GearBonus.RES_FIRE);
        if (src.is(DamageTypes.FALL)) pct += g.get(GearBonus.RES_FALL);
        if (src.is(DamageTypeTags.IS_PROJECTILE)) pct += g.get(GearBonus.RES_PROJECTILE);
        if (src.is(DamageTypeTags.IS_EXPLOSION)) pct += g.get(GearBonus.RES_EXPLOSION);
        if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)) pct += g.get(GearBonus.RES_MAGIC);
        return Math.min(0.75f, pct / 100f);
    }

    public static boolean isNether(LivingEntity t) {
        return t instanceof AbstractPiglin || t instanceof Hoglin || t instanceof Blaze || t instanceof Ghast
                || t instanceof MagmaCube || t instanceof WitherSkeleton;
    }
}
