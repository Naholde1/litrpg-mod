package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.AbilityHandler;
import com.andrey.litrpg.ability.Stealth;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.block.CropBlock;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Наблюдение за стилем игры до 10 уровня: Система запоминает, чем игрок
 * занимается, и на 10 уровне предлагает подходящие классы.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class StyleEvents {

    private static void add(Player p, Style st, float amount) {
        if (p instanceof ServerPlayer sp) {
            PlayerStats s = StatsManager.get(sp);
            if (!s.hasClass()) s.addStyle(st, Math.max(1, Math.round(amount)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        DamageSource src = event.getSource();
        float amount = Math.min(event.getAmount(), target.getHealth() + 4);

        if (target instanceof ServerPlayer victim && src.getEntity() instanceof LivingEntity && !(src.getEntity() instanceof Player)) {
            add(victim, Style.TANK, amount);
            return;
        }
        if (!(src.getEntity() instanceof ServerPlayer p) || target instanceof Player) return;
        if (src.getDirectEntity() instanceof AbstractArrow || src.getDirectEntity() instanceof net.minecraft.world.entity.projectile.ThrownTrident) {
            add(p, Style.RANGED, amount);
        } else if (src.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC) || src.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            add(p, Style.MAGIC, amount);
        } else {
            add(p, Style.MELEE, amount);
        }
        if (p.isCrouching() || Stealth.isHidden(p) || p.isInvisible() || AbilityHandler.isBehind(p, target)) {
            add(p, Style.STEALTH, amount * 0.8f);
        }
        if (target.isOnFire()) add(p, Style.FIRE, amount * 0.7f);
    }

    @SubscribeEvent
    public static void onBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof Player p) add(p, Style.TANK, event.getBlockedDamage());
    }

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p)) return;
        LivingEntity dead = event.getEntity();
        if (dead.getMobType() == MobType.UNDEAD) add(p, Style.UNDEAD, 6);
        if (dead instanceof Animal) add(p, Style.NATURE, 2);
        if (dead instanceof Enemy && (p.isCrouching() || Stealth.isHidden(p))) add(p, Style.STEALTH, 5);
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() != null) add(event.getCausedByPlayer(), Style.NATURE, 10);
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        add(event.getTamer(), Style.NATURE, 20);
    }

    @SubscribeEvent
    public static void onFish(ItemFishedEvent event) {
        add(event.getEntity(), Style.NATURE, 5);
    }

    @SubscribeEvent
    public static void onHarvest(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock() instanceof CropBlock crop && crop.isMaxAge(event.getState())) {
            add(event.getPlayer(), Style.NATURE, 2);
        }
    }

    @SubscribeEvent
    public static void onIgnite(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && event.getPlacedBlock().is(net.minecraft.world.level.block.Blocks.FIRE)) {
            add(p, Style.FIRE, 3);
        }
    }

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        boolean gear = event.getCrafting().getItem() instanceof TieredItem || event.getCrafting().getItem() instanceof ArmorItem;
        add(event.getEntity(), Style.CRAFT, gear ? 5 : 1);
    }

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        add(event.getEntity(), Style.CRAFT, event.getSmelting().getCount());
    }

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        add(event.getEntity(), Style.CRAFT, 6);
        add(event.getEntity(), Style.SUPPORT, 2);
    }

    @SubscribeEvent
    public static void onAnvil(AnvilRepairEvent event) {
        add(event.getEntity(), Style.CRAFT, 5);
    }

    /** Зачарование и починка на наковальне тратят уровни опыта. */
    @SubscribeEvent
    public static void onXpSpent(net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange event) {
        if (event.getLevels() < 0) add(event.getEntity(), Style.CRAFT, -event.getLevels() * 4);
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        add(event.getEntity(), Style.SUPPORT, 5);
    }

    @SubscribeEvent
    public static void onConsume(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player p)) return;
        var item = event.getItem().getItem();
        if (item instanceof PotionItem) add(p, Style.SUPPORT, 8);
        else if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) add(p, Style.SUPPORT, 10);
    }
}
