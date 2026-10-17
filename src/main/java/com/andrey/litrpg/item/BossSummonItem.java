package com.andrey.litrpg.item;

import com.andrey.litrpg.registry.ModDimensions;
import com.andrey.litrpg.registry.ModEntities;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BossSummonItem extends Item {
    public enum Boss { GOBLIN_KING, LICH }

    private final Boss boss;

    public BossSummonItem(Properties props, Boss boss) {
        super(props);
        this.boss = boss;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ServerLevel sl = (ServerLevel) level;

        if (boss == Boss.LICH && sl.dimension() != ModDimensions.SHADOW_REALM) {
            if (ctx.getPlayer() instanceof ServerPlayer sp) {
                StatsManager.message(sp, Component.literal("Филактерия откликается только в Изнанке.").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        EntityType<? extends Mob> type = boss == Boss.LICH ? ModEntities.LICH.get() : ModEntities.GOBLIN_KING.get();
        Mob mob = type.create(sl);
        if (mob == null) return InteractionResult.FAIL;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null, null);
        sl.addFreshEntity(mob);

        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }

        Component name = mob.getDisplayName();
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(64))) {
            StatsManager.message(p, Component.literal("ВНИМАНИЕ! Пробудился босс: ").withStyle(ChatFormatting.RED)
                    .append(name));
        }

        ItemStack stack = ctx.getItemInHand();
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String key = boss == Boss.LICH ? "item.litrpg.lich_phylactery.tooltip" : "item.litrpg.goblin_seal.tooltip";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
