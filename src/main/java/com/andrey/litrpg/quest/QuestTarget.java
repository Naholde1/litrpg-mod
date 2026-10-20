package com.andrey.litrpg.quest;

import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.event.MobLevels;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.raid.Raider;

import java.util.function.Predicate;

/** Кого нужно убить в задании «Охота». */
public enum QuestTarget {
    ANY("monsters", "монстров", e -> e instanceof Enemy),
    UNDEAD("undead", "нежити", e -> e instanceof Enemy && e.getMobType() == MobType.UNDEAD),
    ZOMBIE("zombies", "зомби", e -> e instanceof Zombie),
    SKELETON("skeletons", "скелетов", e -> e instanceof AbstractSkeleton),
    SPIDER("spiders", "пауков", e -> e instanceof Spider),
    CREEPER("creepers", "криперов", e -> e instanceof Creeper),
    RAIDER("raiders", "разбойников-иллагеров", e -> e instanceof Raider && !(e instanceof Witch)),
    WITCH("witches", "ведьм", e -> e instanceof Witch),
    GOBLIN("goblins", "гоблинов", e -> e instanceof GoblinEntity),
    PHANTOM("phantoms", "фантомов", e -> e instanceof Phantom),
    SLIME("slimes", "слизней", e -> e instanceof Slime && !(e instanceof MagmaCube)),
    ENDER("endermen", "эндерменов", e -> e instanceof EnderMan),
    NETHER("nether", "тварей Преисподней", e -> e instanceof Blaze || e instanceof Ghast || e instanceof MagmaCube || e instanceof WitherSkeleton),
    ELITE("elites", "элитных монстров и чемпионов", e -> e instanceof Enemy && MobLevels.getRank(e) != MobLevels.Rank.NORMAL),
    RAT("rats", "крыс", e -> e instanceof com.andrey.litrpg.entity.RatEntity),
    WOLF("wolves", "волков", e -> e instanceof net.minecraft.world.entity.animal.Wolf w && !w.isTame()),
    BANDIT("bandits", "разбойников", e -> e instanceof net.minecraft.world.entity.monster.Pillager || e instanceof net.minecraft.world.entity.monster.Vindicator);

    public final String key;
    /** Название в родительном падеже множественного числа: «Убейте 10 …». */
    public final String plural;
    private final Predicate<LivingEntity> test;

    QuestTarget(String key, String plural, Predicate<LivingEntity> test) {
        this.key = key;
        this.plural = plural;
        this.test = test;
    }

    public boolean matches(LivingEntity e) {
        return test.test(e);
    }

    public static QuestTarget byKey(String key) {
        for (QuestTarget t : values()) if (t.key.equals(key)) return t;
        return null;
    }
}
