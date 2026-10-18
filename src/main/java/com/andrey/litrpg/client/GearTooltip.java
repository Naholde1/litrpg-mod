package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.ability.Ability;
import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.GearSet;
import com.andrey.litrpg.gear.Passive;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.stats.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Подсказки предметов Системы и цветная рамка по редкости. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
public final class GearTooltip {
    private GearTooltip() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        GearData d = GearData.get(stack);
        if (d == null) return;
        List<Component> tip = event.getToolTip();
        int at = Math.min(1, tip.size());
        java.util.List<Component> lines = new java.util.ArrayList<>();

        lines.add(Component.literal("◆ " + d.rarity.title + " • ур. " + d.level).withStyle(d.rarity.color));
        int plv = ClientData.STATS.getLevel();
        if (plv < d.requiredLevel()) {
            lines.add(Component.literal("Требуется уровень " + d.requiredLevel()).withStyle(ChatFormatting.RED));
        }
        for (Stat s : Stat.values()) {
            int v = d.stats[s.ordinal()];
            if (v != 0) lines.add(Component.literal((v > 0 ? "+" : "") + v + " ").append(Component.translatable(s.translationKey()))
                    .withStyle(ChatFormatting.GREEN));
        }
        d.bonuses.forEach((b, v) -> lines.add(Component.literal(fmt(b, v)).withStyle(ChatFormatting.AQUA)));
        for (Passive p : d.passives) {
            lines.add(Component.literal("✦ " + p.title + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
                    .append(Component.literal(p.desc).withStyle(ChatFormatting.GRAY)));
        }
        Ability a = d.getAbility();
        if (a != null) {
            lines.add(Component.literal("⚡ Shift+ПКМ: ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(a.nameKey()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" (" + a.manaCost() + " маны)").withStyle(ChatFormatting.GRAY)));
            lines.add(Component.literal("   ").append(Component.translatable(a.descKey())).withStyle(ChatFormatting.DARK_GRAY));
        }
        GearSet set = GearData.setOf(stack);
        if (set != null) {
            int worn = ClientData.STATS.gear().setPieces.getOrDefault(set, 0);
            lines.add(Component.literal(set.title + " (" + worn + "/4)").withStyle(ChatFormatting.GOLD));
            lines.add(Component.literal(" 2: " + set.bonus2).withStyle(worn >= 2 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            lines.add(Component.literal(" 4: " + set.bonus4).withStyle(worn >= 4 ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        if (d.unique != null) {
            lines.add(Component.literal("«" + GearGen.lore(d.unique) + "»").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        tip.addAll(at, lines);
    }

    private static String fmt(GearBonus b, float v) {
        String num = b.percent ? String.valueOf(Math.round(v)) + "%" : (v == Math.round(v) ? String.valueOf(Math.round(v)) : String.valueOf(v));
        return "+" + num + " " + b.text;
    }

    /** Рамка подсказки окрашивается в цвет редкости. */
    @SubscribeEvent
    public static void onColor(RenderTooltipEvent.Color event) {
        GearData d = GearData.get(event.getItemStack());
        if (d == null) return;
        int c = d.rarity.rgb;
        int dark = ((c >> 17) & 0x7F) << 16 | ((c >> 9) & 0x7F) << 8 | ((c >> 1) & 0x7F);
        event.setBorderStart(0xFF000000 | c);
        event.setBorderEnd(0xFF000000 | dark);
        if (d.rarity == Rarity.DIVINE) {
            // божественные — переливаются
            float t = (System.currentTimeMillis() % 2000) / 2000f;
            int pulse = net.minecraft.util.Mth.hsvToRgb(t, 0.7f, 1f);
            event.setBorderStart(0xFF000000 | pulse);
        }
        event.setBackgroundStart(0xF0100A18);
        event.setBackgroundEnd(0xF0080510);
    }
}
