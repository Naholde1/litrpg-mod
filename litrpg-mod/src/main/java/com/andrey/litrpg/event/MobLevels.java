package com.andrey.litrpg.event;

import com.andrey.litrpg.entity.LitBoss;
import com.andrey.litrpg.registry.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;

import java.util.UUID;

/** Уровни мобов: чем дальше от точки спавна (и в «Изнанке»), тем сильнее. */
public class MobLevels {
    public static final String TAG = "litrpg_lvl";
    private static final UUID HP_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1101");
    private static final UUID DMG_ID = UUID.fromString("8a4d1f52-77b0-4c4e-a1c2-9b3e5d7f1102");

    public static int getLevel(LivingEntity e) {
        if (e instanceof LitBoss boss) return boss.getBossLevel();
        int lvl = e.getPersistentData().getInt(TAG);
        return lvl <= 0 ? 1 : lvl;
    }

    public static void assignLevel(Mob mob, ServerLevel level) {
        if (!(mob instanceof Enemy) || mob instanceof LitBoss) return;
        if (mob.getPersistentData().contains(TAG)) return;

        BlockPos spawn = level.getSharedSpawnPos();
        double dist = Math.sqrt(mob.blockPosition().distSqr(spawn));
        int lvl;
        if (level.dimension() == ModDimensions.SHADOW_REALM) {
            lvl = 10 + (int) (dist / 120.0);
        } else if (level.dimension() == Level.NETHER) {
            lvl = 15 + (int) (dist / 80.0);
        } else if (level.dimension() == Level.END) {
            lvl = 30;
        } else {
            lvl = 1 + (int) (dist / 200.0);
        }
        lvl += mob.getRandom().nextInt(3);
        lvl = Mth.clamp(lvl, 1, 60);

        mob.getPersistentData().putInt(TAG, lvl);
        applyScaling(mob, lvl);
    }

    private static void applyScaling(Mob mob, int lvl) {
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null && hp.getModifier(HP_ID) == null && lvl > 1) {
            hp.addPermanentModifier(new AttributeModifier(HP_ID, "litrpg_mob_hp", 0.12 * (lvl - 1),
                    AttributeModifier.Operation.MULTIPLY_BASE));
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null && dmg.getModifier(DMG_ID) == null && lvl > 1) {
            dmg.addPermanentModifier(new AttributeModifier(DMG_ID, "litrpg_mob_dmg", 0.07 * (lvl - 1),
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        if (!mob.hasCustomName()) {
            ChatFormatting color = lvl < 10 ? ChatFormatting.GRAY : lvl < 20 ? ChatFormatting.YELLOW
                    : lvl < 35 ? ChatFormatting.GOLD : ChatFormatting.RED;
            mob.setCustomName(Component.literal("[Ур. " + lvl + "] ").withStyle(color)
                    .append(Component.empty().append(mob.getType().getDescription()).withStyle(ChatFormatting.WHITE)));
            mob.setCustomNameVisible(false);
        }
    }

    /** Опыт Системы за убийство. */
    public static int xpReward(LivingEntity killed, int playerLevel) {
        if (killed instanceof LitBoss boss) return boss.getXpReward();
        int lvl = getLevel(killed);
        double base;
        if (killed instanceof Enemy) {
            base = 4 + killed.getMaxHealth() * 0.7;
            base *= 1.0 + 0.18 * (lvl - 1);
        } else {
            base = 1 + killed.getMaxHealth() * 0.1;
        }
        int diff = playerLevel - lvl;
        if (diff > 8) base *= 0.35;
        else if (diff < -5) base *= 1.4;
        return Math.max(1, (int) Math.round(base));
    }
}
