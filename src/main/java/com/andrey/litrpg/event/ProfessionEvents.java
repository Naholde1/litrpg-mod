package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.Profession;
import com.andrey.litrpg.stats.StatsManager;
import com.andrey.litrpg.stats.Title;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.brewing.PlayerBrewedPotionEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Профессии: опыт за занятие и бонусы от уровня. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class ProfessionEvents {

    public static void giveProfXp(ServerPlayer player, Profession p, int amount) {
        PlayerStats s = StatsManager.get(player);
        if (!s.knows(p)) return;
        int gained = s.addProfXp(p, amount);
        if (gained > 0) {
            StatsManager.message(player, Component.literal("Профессия ")
                    .append(Component.translatable(p.nameKey()).withStyle(ChatFormatting.GOLD))
                    .append(" достигла уровня " + s.getProfLevel(p) + "!"));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.VILLAGER_WORK_TOOLSMITH, SoundSource.PLAYERS, 1f, 1.2f);
        }
        StatsManager.sync(player);
    }

    /** Шанс бонуса в процентах от уровня профессии. */
    private static boolean roll(ServerPlayer player, Profession p, float perLevel) {
        int lvl = StatsManager.get(player).getProfLevel(p);
        return lvl > 0 && player.getRandom().nextFloat() < lvl * perLevel;
    }

    // ---------- Шахтёр и Травник ----------

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BlockState state = event.getState();
        BlockPos pos = event.getPos();

        if (state.is(Tags.Blocks.ORES)) {
            StatsManager.count(player, Title.Counter.ORES, 1);
            boolean deep = state.is(Tags.Blocks.ORES_DIAMOND) || state.is(Tags.Blocks.ORES_EMERALD)
                    || state.is(Tags.Blocks.ORES_NETHERITE_SCRAP);
            giveProfXp(player, Profession.MINER, deep ? 25 : 6);
            boolean silk = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, player.getMainHandItem()) > 0;
            if (!silk && roll(player, Profession.MINER, 0.01f)) {
                duplicateDrops(level, pos, state, player);
                player.displayClientMessage(Component.literal("Двойная добыча!").withStyle(ChatFormatting.GOLD), true);
            }
            int lvl = StatsManager.get(player).getProfLevel(Profession.MINER);
            if (lvl >= 10) {
                player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SPEED, 60,
                        lvl >= 35 ? 1 : 0, true, false, false));
            }
        } else if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            giveProfXp(player, Profession.HERBALIST, 3);
            if (roll(player, Profession.HERBALIST, 0.015f)) duplicateDrops(level, pos, state, player);
        } else if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.SAPLINGS)) {
            giveProfXp(player, Profession.HERBALIST, 1);
        }
    }

    private static void duplicateDrops(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, player.getMainHandItem()));
        for (ItemStack d : drops) Block.popResource(level, pos, d);
    }

    // ---------- Кузнец ----------

    @SubscribeEvent
    public static void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack out = event.getCrafting();
        if (!(out.getItem() instanceof TieredItem) && !(out.getItem() instanceof ArmorItem)) return;
        giveProfXp(player, Profession.BLACKSMITH, 10);
        int lvl = StatsManager.get(player).getProfLevel(Profession.BLACKSMITH);
        if (lvl <= 0 || player.getRandom().nextFloat() > 0.15f + lvl * 0.012f) return;
        // «Мастерская работа»: прочность и защита/острота в зависимости от уровня
        int tier = lvl >= 40 ? 3 : lvl >= 20 ? 2 : 1;
        out.enchant(Enchantments.UNBREAKING, tier);
        if (out.getItem() instanceof ArmorItem) {
            if (lvl >= 15) out.enchant(Enchantments.ALL_DAMAGE_PROTECTION, Math.min(4, tier));
        } else if (lvl >= 15) {
            out.enchant(Enchantments.SHARPNESS, Math.min(4, tier));
        }
        out.setHoverName(Component.literal("Мастерская работа: ").withStyle(ChatFormatting.GOLD)
                .append(out.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
        player.displayClientMessage(Component.literal("Мастерская работа!").withStyle(ChatFormatting.GOLD), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    // ---------- Алхимик ----------

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) giveProfXp(player, Profession.ALCHEMIST, 8);
    }

    @SubscribeEvent
    public static void onDrink(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getItem().getItem() instanceof PotionItem)) return;
        int lvl = StatsManager.get(player).getProfLevel(Profession.ALCHEMIST);
        if (lvl <= 0) return;
        float mult = 1f + lvl * 0.03f;
        List<MobEffectInstance> boosted = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().isBeneficial() && !e.isInfiniteDuration() && e.getDuration() > 40) {
                boosted.add(new MobEffectInstance(e.getEffect(), (int) (e.getDuration() * mult), e.getAmplifier(),
                        e.isAmbient(), e.isVisible(), e.showIcon()));
            }
        }
        for (MobEffectInstance e : boosted) player.forceAddEffect(e, null);
    }

    // ---------- Охотник ----------

    @SubscribeEvent
    public static void onAnimalDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal animal)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        giveProfXp(player, Profession.HUNTER, 5);
        if (!event.getDrops().isEmpty() && roll(player, Profession.HUNTER, 0.02f)) {
            List<ItemEntity> extra = new ArrayList<>();
            for (ItemEntity ie : event.getDrops()) {
                extra.add(new ItemEntity(animal.level(), ie.getX(), ie.getY(), ie.getZ(), ie.getItem().copy()));
            }
            event.getDrops().addAll(extra);
        }
    }
}
