package com.andrey.litrpg.entity;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Role;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;

/**
 * Урон хранителей «по-рейдовому». Сила навыка задаётся в долях «единицы» — примерного
 * здоровья бойца того же уровня. Броня и щит помогают умеренно, а главное — роль:
 * танк получает меньше половины. Поэтому удары по площади, которые танк выдерживает,
 * валят остальных за два-три попадания, и без танка и лекаря хранителя не одолеть.
 */
public final class BossDamage {
    private BossDamage() {}

    public static final ResourceKey<DamageType> BOSS = ResourceKey.create(Registries.DAMAGE_TYPE, LitRPG.id("boss"));

    /** Примерное здоровье бойца (не танка) на этом уровне. */
    public static float unit(int level) {
        return 20f + 2.6f * level;
    }

    public static DamageSource source(LivingEntity boss) {
        return new DamageSource(boss.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(BOSS), boss);
    }

    public static final String SUNDER = "litrpg_sunder";
    public static final String SUNDER_UNTIL = "litrpg_sunder_until";

    /** «Раскол брони»: каждый стак — +12% урона от хранителей на 20 с (танкам — повод меняться). */
    public static int sunder(LivingEntity e, int add) {
        var d = e.getPersistentData();
        long now = e.level().getGameTime();
        int stacks = d.getLong(SUNDER_UNTIL) > now ? d.getInt(SUNDER) : 0;
        stacks = Math.min(10, stacks + add);
        d.putInt(SUNDER, stacks);
        d.putLong(SUNDER_UNTIL, now + 400);
        return stacks;
    }

    /** Сколько урона хранителя дойдёт до цели (после роли, брони и щита). */
    public static float mitigate(LivingEntity victim, DamageSource src, float amount) {
        var d = victim.getPersistentData();
        if (d.getLong(SUNDER_UNTIL) > victim.level().getGameTime()) amount *= 1f + 0.12f * d.getInt(SUNDER);
        float armor = victim.getArmorValue();
        amount *= 1f - Math.min(0.5f, armor / (armor + 45f));
        if (victim instanceof ServerPlayer p) {
            PlayerStats s = StatsManager.get(p);
            if (s.getRole() == Role.TANK) amount *= 0.42f;
            if (p.isBlocking() && p.getUseItem().getItem() instanceof ShieldItem) {
                Vec3 from = src.getSourcePosition();
                if (from != null) {
                    Vec3 look = p.getViewVector(1f);
                    Vec3 to = from.subtract(p.position()).normalize();
                    if (look.x * to.x + look.z * to.z > 0.2) {
                        // щит не изнашивается (gear/Indestructible): в подземелье его негде починить
                        amount *= s.getRole() == Role.TANK ? 0.55f : 0.8f;
                    }
                }
            }
        }
        return amount;
    }

    public static boolean isTank(LivingEntity e) {
        return e instanceof ServerPlayer p && StatsManager.get(p).getRole() == Role.TANK;
    }

    // ---------- чем бьют хранителя ----------

    /** Род удара: от него зависят слабости и стойкости хранителей. */
    public enum Weapon {
        SWORD("клинки"), AXE("топоры"), PICKAXE("кирки и молоты"), BLUNT("дробящее"), ARROW("стрелы"), MAGIC("магия"), FIRE("огонь"),
        HOLY("святое"), FROST("холод"), OTHER("прочее");

        public final String title;

        Weapon(String title) {
            this.title = title;
        }
    }

    public static Weapon weapon(DamageSource src) {
        if (src.is(DamageTypeTags.IS_FIRE)) return Weapon.FIRE;
        if (src.is(DamageTypeTags.IS_FREEZING)) return Weapon.FROST;
        Entity direct = src.getDirectEntity();
        if (direct instanceof AbstractArrow) return Weapon.ARROW;
        if (src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC) || src.is(DamageTypes.SONIC_BOOM)) return Weapon.MAGIC;
        if (src.getEntity() instanceof Player p && direct == p) {
            ItemStack hand = p.getMainHandItem();
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, hand) > 0) return Weapon.FIRE;
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SMITE, hand) > 0) return Weapon.HOLY;
            if (hand.getItem() instanceof PickaxeItem) return Weapon.PICKAXE;
            if (hand.getItem() instanceof AxeItem) return Weapon.AXE;
            if (hand.getItem() instanceof SwordItem) return Weapon.SWORD;
            if (hand.getItem() instanceof ShovelItem || hand.isEmpty() || hand.is(Items.STICK)) return Weapon.BLUNT;
        }
        return Weapon.OTHER;
    }
}
