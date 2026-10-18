package com.andrey.litrpg.gear;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.ability.AbilityHandler;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

/** Всё, что делает экипировка: пересчёт бонусов, пассивки, способности, улучшения. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class GearEvents {
    private static final String SIG_TAG = "litrpg_gear_sig";
    private static final String PHOENIX_TAG = "litrpg_phoenix_cd";

    private GearEvents() {}

    // ================= пересчёт надетого =================

    /** Все стеки, чьи свойства сейчас действуют на игрока. */
    public static List<ItemStack> equipped(Player p) {
        List<ItemStack> list = new ArrayList<>();
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            list.add(p.getItemBySlot(slot));
        }
        ItemStack main = p.getMainHandItem();
        if (GearData.category(main) == GearData.Category.WEAPON) list.add(main);
        ItemStack off = p.getOffhandItem();
        if (off.getItem() instanceof ShieldItem) list.add(off);
        CuriosApi.getCuriosInventory(p).ifPresent(inv -> {
            var h = inv.getEquippedCurios();
            for (int i = 0; i < h.getSlots(); i++) list.add(h.getStackInSlot(i));
        });
        return list;
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        List<ItemStack> eq = equipped(p);
        int sig = StatsManager.get(p).getLevel();
        for (ItemStack st : eq) sig = sig * 31 + (st.isEmpty() ? 0 : st.getItem().hashCode() * 7 + (st.hasTag() ? st.getTag().hashCode() : 0));
        if (p.getPersistentData().getInt(SIG_TAG) != sig || !p.getPersistentData().contains(SIG_TAG)) {
            p.getPersistentData().putInt(SIG_TAG, sig);
            recompute(p, eq);
        }
        if (p.tickCount % 40 == 0) passiveEffects(p, StatsManager.get(p).gear());
    }

    public static void recompute(ServerPlayer p, List<ItemStack> eq) {
        PlayerStats s = StatsManager.get(p);
        PlayerGear g = s.gear();
        g.clear();
        java.util.Map<String, java.util.Set<String>> setItems = new java.util.HashMap<>();
        for (ItemStack st : eq) {
            GearData d = GearData.get(st);
            if (d != null && s.getLevel() >= d.requiredLevel()) g.addItem(d);
            GearSet set = GearData.setOf(st);
            if (set != null) g.setPieces.merge(set, 1, Integer::sum);
            if (st.getItem() instanceof TemplateItem t && t.def().set != null) {
                setItems.computeIfAbsent(t.def().set, k -> new java.util.HashSet<>()).add(t.def().id);
            }
        }
        g.setPieces.forEach((set, n) -> set.apply(g, n));
        // наборы из шаблонов: считаются разные предметы набора
        setItems.forEach((id, items) -> {
            g.dataSets.put(id, items.size());
            ItemTemplates.SetDef def = ItemTemplates.SETS.get(id);
            if (def == null) return;
            for (ItemTemplates.SetTier tier : def.tiers) {
                if (items.size() < tier.pieces) continue;
                tier.bonus.forEach(g::add);
                for (int i = 0; i < tier.stats.length; i++) g.stats[i] += tier.stats[i];
                if (tier.passive != null) g.passives.add(tier.passive);
            }
        });
        StatsManager.applyAttributes(p);
        StatsManager.sync(p);
    }

    private static void passiveEffects(ServerPlayer p, PlayerGear g) {
        if (g.has(Passive.NIGHT_EYE)) hidden(p, MobEffects.NIGHT_VISION, 0, 300);
        if (g.has(Passive.SWIFTNESS)) hidden(p, MobEffects.MOVEMENT_SPEED, 0, 100);
        if (g.has(Passive.REGENERATION)) hidden(p, MobEffects.REGENERATION, 0, 100);
        if (g.has(Passive.FIREPROOF)) hidden(p, MobEffects.FIRE_RESISTANCE, 0, 100);
        if (g.has(Passive.WATER_BREATHING)) hidden(p, MobEffects.WATER_BREATHING, 0, 100);
        float regen = g.get(GearBonus.HEALTH_REGEN);
        if (regen > 0 && p.getHealth() < p.getMaxHealth()) p.heal(regen * 0.4f); // раз в 2 с = regen за 5 с
    }

    private static void hidden(ServerPlayer p, MobEffect effect, int amp, int duration) {
        MobEffectInstance cur = p.getEffect(effect);
        if (cur == null || cur.getDuration() < duration - 40) {
            p.addEffect(new MobEffectInstance(effect, duration, amp, true, false, false));
        }
    }

    // ================= бой =================

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) return;
        float amount = event.getAmount();

        // игрок получает урон
        if (target instanceof ServerPlayer victim) {
            PlayerStats vs = StatsManager.get(victim);
            PlayerGear g = vs.gear();
            if (event.getSource().is(DamageTypes.FALL) && g.has(Passive.FEATHERFALL)) {
                event.setCanceled(true);
                return;
            }
            if (g.hasSet(GearSet.BONEWARDEN) && event.getSource().getEntity() instanceof LivingEntity a && a.getMobType() == MobType.UNDEAD) {
                amount *= 0.75f;
            }
            amount *= 1f - Families.incomingReduction(g, event.getSource());
            if (g.has(Passive.MANA_SHIELD) && vs.getMana() > 0) {
                float absorbed = Math.min(amount * 0.2f, (float) vs.getMana() / 2f);
                vs.setMana(vs.getMana() - absorbed * 2);
                amount -= absorbed;
                StatsManager.syncMana(victim);
            }
            if (g.has(Passive.THORNS) && event.getSource().getEntity() instanceof LivingEntity attacker
                    && event.getSource().getDirectEntity() == attacker && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
                float dmg = amount * 0.25f;
                com.andrey.litrpg.util.Scheduler.later(1, () -> {
                    if (attacker.isAlive()) attacker.hurt(victim.damageSources().thorns(victim), dmg);
                });
            }
            event.setAmount(amount);
            return;
        }

        // игрок наносит урон
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || target instanceof Player) return;
        PlayerStats s = StatsManager.get(p);
        PlayerGear g = s.gear();
        boolean melee = event.getSource().getDirectEntity() == p;
        boolean arrow = event.getSource().getDirectEntity() instanceof AbstractArrow;
        boolean skill = !melee && !arrow;

        if (skill) amount *= 1 + g.get(GearBonus.SPELL_POWER) / 100f;
        if (arrow) amount *= 1 + g.get(GearBonus.ARROW_DAMAGE) / 100f;
        amount *= Families.outgoing(g, target);
        if (g.has(Passive.EXECUTIONER) && target.getHealth() < target.getMaxHealth() * 0.3f) amount *= 1.5f;
        if (g.has(Passive.GIANT_SLAYER) && MobLevels.getLevel(target) > s.getLevel()) amount *= 1.25f;
        if (g.has(Passive.BERSERKER) && p.getHealth() < p.getMaxHealth() * 0.4f) amount *= 1.3f;
        if (melee || arrow) {
            if (g.has(Passive.FLAME_TOUCH)) target.setSecondsOnFire(4);
            if (g.has(Passive.FROST_TOUCH)) target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), p);
            if (g.has(Passive.VENOM)) target.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), p);
            float steal = g.get(GearBonus.LIFESTEAL);
            if (steal > 0) p.heal(amount * steal / 100f);
            if (g.has(Passive.CHAIN_SPARK) && p.getRandom().nextFloat() < 0.15f) chainSpark(p, target, amount * 0.5f);
        }
        event.setAmount(amount);
    }

    private static void chainSpark(ServerPlayer p, LivingEntity first, float dmg) {
        ServerLevel level = p.serverLevel();
        com.andrey.litrpg.util.Scheduler.later(1, () -> {
            LivingEntity cur = first;
            List<LivingEntity> hit = new ArrayList<>();
            for (int i = 0; i < 3 && cur != null; i++) {
                hit.add(cur);
                LivingEntity next = null;
                double best = 49;
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cur.getBoundingBox().inflate(7),
                        e -> AbilityHandler.isEnemy(p, e))) {
                    if (hit.contains(e)) continue;
                    double d = e.distanceToSqr(cur);
                    if (d < best) { best = d; next = e; }
                }
                if (next != null) {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY() + 1, next.getZ(), 15, 0.3, 0.5, 0.3, 0.1);
                    next.invulnerableTime = 0;
                    next.hurt(p.damageSources().indirectMagic(p, p), dmg);
                }
                cur = next;
            }
            level.playSound(null, first.getX(), first.getY(), first.getZ(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 1.8f);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent event) {
        // Феникс: спасает от смерти раз в 10 минут
        if (event.getEntity() instanceof ServerPlayer p && StatsManager.get(p).gear().has(Passive.PHOENIX)) {
            long now = p.level().getGameTime();
            if (now >= p.getPersistentData().getLong(PHOENIX_TAG)) {
                event.setCanceled(true);
                p.setHealth(p.getMaxHealth() * 0.3f);
                p.removeAllEffects();
                p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
                p.getPersistentData().putLong(PHOENIX_TAG, now + 20 * 600);
                p.serverLevel().sendParticles(ParticleTypes.FLAME, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.0, 0.6, 0.1);
                p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1f, 1.2f);
                StatsManager.message(p, Component.literal("Феникс возрождает вас из пепла!").withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        // Пожиратель душ: лечение за убийство
        if (event.getSource().getEntity() instanceof ServerPlayer p && StatsManager.get(p).gear().has(Passive.SOUL_EATER)) {
            p.heal(p.getMaxHealth() * 0.15f);
            PlayerStats s = StatsManager.get(p);
            s.setMana(s.getMana() + s.getMaxMana() * 0.1);
            p.serverLevel().sendParticles(ParticleTypes.SOUL, event.getEntity().getX(), event.getEntity().getY() + 1,
                    event.getEntity().getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }
    }

    /** Скорость добычи блоков. */
    @SubscribeEvent
    public static void onBreakSpeed(net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed event) {
        float m = StatsManager.get(event.getEntity()).gear().get(GearBonus.MINING);
        if (m > 0) event.setNewSpeed(event.getNewSpeed() * (1 + m / 100f));
    }

    // ================= способности предметов =================

    /** Shift + ПКМ с оружием, у которого есть способность. */
    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.isShiftKeyDown()) return;
        ItemStack stack = event.getItemStack();
        GearData d = GearData.get(stack);
        if (d == null || d.getAbility() == null) return;
        event.setCanceled(true);
        PlayerStats s = StatsManager.get(p);
        if (s.getLevel() < d.requiredLevel()) {
            p.displayClientMessage(Component.literal("Нужен уровень " + d.requiredLevel()).withStyle(ChatFormatting.RED), true);
            return;
        }
        if (p.getCooldowns().isOnCooldown(stack.getItem())) return;
        Ability a = d.getAbility();
        if (AbilityHandler.castFromItem(p, s, a)) {
            p.getCooldowns().addCooldown(stack.getItem(), a.cooldown());
        }
    }

    // ================= наковальня =================

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        GearData d = GearData.get(left);
        if (d == null || d.unique != null) return;
        var rnd = net.minecraft.util.RandomSource.create(left.hashCode() * 31L + right.getCount());
        if (right.is(ModItems.ASCENSION_STONE.get()) && d.rarity != Rarity.DIVINE) {
            ItemStack out = left.copy();
            out.removeTagKey(GearData.TAG);
            out.resetHoverName();
            GearGen.apply(out, rnd, d.level, d.rarity.next());
            event.setOutput(out);
            event.setCost(8 + d.rarity.ordinal() * 6);
            event.setMaterialCost(1);
        } else if (right.is(ModItems.REFORGE_STONE.get())) {
            ItemStack out = left.copy();
            out.removeTagKey(GearData.TAG);
            out.resetHoverName();
            GearGen.apply(out, rnd, d.level, d.rarity);
            event.setOutput(out);
            event.setCost(4 + d.rarity.ordinal() * 3);
            event.setMaterialCost(1);
        }
    }
}
