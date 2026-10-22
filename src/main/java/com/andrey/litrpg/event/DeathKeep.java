package com.andrey.litrpg.event;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.Rarity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.DropRulesEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Гибель отнимает не всё: строка быстрого доступа остаётся при игроке, а надетое
 * (броня, вторая рука, оружие в руке, артефакты Curios) остаётся с шансом — чем ниже
 * редкость, тем вероятнее вещь выпадет вместе с рюкзаком. Вещи с проклятием утраты исчезают, как и раньше.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class DeathKeep {
    private DeathKeep() {}

    private static final String TAG = "litrpg_kept";
    private static final String LOST = "litrpg_lost";
    /** Строка быстрого доступа 0–8, броня 36–39, вторая рука 40. */
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 36, 37, 38, 39, 40};

    /** Шанс потерять надетую вещь по редкости (обычные вещи без редкости — как «Обычный»). */
    private static final float[] LOSS = {0.40f, 0.30f, 0.20f, 0.12f, 0.06f, 0.02f};

    public static float lossChance(ItemStack s) {
        GearData d = GearData.get(s);
        return LOSS[(d == null ? Rarity.COMMON : d.rarity).ordinal()];
    }

    private static boolean worn(Inventory inv, int slot, ItemStack s) {
        return slot >= 36 || slot == inv.selected && (s.isDamageableItem() || GearData.has(s));
    }

    private static boolean keepInventory(ServerPlayer p) {
        return p.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY);
    }

    private static void lost(ServerPlayer p, ItemStack s) {
        CompoundTag data = p.getPersistentData();
        ListTag list = data.getList(LOST, Tag.TAG_STRING);
        list.add(StringTag.valueOf(s.getHoverName().getString() + (s.getCount() > 1 ? " ×" + s.getCount() : "")));
        data.put(LOST, list);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isSpectator() || keepInventory(p)) return;
        Inventory inv = p.getInventory();
        ListTag kept = new ListTag();
        p.getPersistentData().remove(LOST);
        for (int slot : SLOTS) {
            ItemStack s = inv.getItem(slot);
            if (s.isEmpty() || EnchantmentHelper.hasVanishingCurse(s)) continue;
            if (worn(inv, slot, s) && p.getRandom().nextFloat() < lossChance(s)) {
                lost(p, s); // остаётся в инвентаре и выпадает вместе с рюкзаком
                continue;
            }
            CompoundTag t = s.save(new CompoundTag());
            t.putInt("KeptSlot", slot);
            kept.add(t);
            inv.setItem(slot, ItemStack.EMPTY);
        }
        // если игрок вышел с экрана смерти — вещи сохранятся вместе с ним
        p.getPersistentData().remove(TAG);
        if (!kept.isEmpty()) p.getPersistentData().put(TAG, kept);
    }

    /** Артефакты Curios остаются в слотах (Curios сам переносит их), но с тем же шансом выпадают. Правило — последнее подошедшее. */
    @SubscribeEvent
    public static void onCurioDrops(DropRulesEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || keepInventory(p)) return;
        e.addOverride(s -> !EnchantmentHelper.hasVanishingCurse(s), ICurio.DropRule.ALWAYS_KEEP);
        e.addOverride(s -> {
            if (EnchantmentHelper.hasVanishingCurse(s) || p.getRandom().nextFloat() >= lossChance(s)) return false;
            lost(p, s);
            return true;
        }, ICurio.DropRule.ALWAYS_DROP);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone e) {
        if (!e.isWasDeath() || !(e.getEntity() instanceof ServerPlayer now)) return;
        CompoundTag data = e.getOriginal().getPersistentData();
        now.getPersistentData().remove(TAG); // на случай, если данные уже перенесены — не дублировать
        now.getPersistentData().remove(LOST);
        if (data.contains(LOST)) {
            ListTag lost = data.getList(LOST, Tag.TAG_STRING);
            data.remove(LOST);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < lost.size(); i++) sb.append(i > 0 ? ", " : "").append(lost.getString(i));
            if (!lost.isEmpty()) now.sendSystemMessage(Component.literal("☠ При гибели слетело снаряжение: " + sb
                    + ". Ищите его там, где пали (в подземелье — в Сундуке павших у Печати).").withStyle(ChatFormatting.RED));
        }
        if (!data.contains(TAG)) return;
        ListTag kept = data.getList(TAG, Tag.TAG_COMPOUND);
        data.remove(TAG);
        Inventory inv = now.getInventory();
        for (int i = 0; i < kept.size(); i++) {
            CompoundTag t = kept.getCompound(i);
            ItemStack s = ItemStack.of(t);
            int slot = t.getInt("KeptSlot");
            if (inv.getItem(slot).isEmpty()) inv.setItem(slot, s);
            else if (!inv.add(s)) now.drop(s, false);
        }
    }
}
