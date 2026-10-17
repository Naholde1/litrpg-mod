package com.andrey.litrpg.item;

import com.andrey.litrpg.registry.ModDimensions;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Ключ Системы: переносит в Изнанку и обратно. */
public class SystemKeyItem extends Item {
    private static final String RETURN_TAG = "litrpg_return";

    public SystemKeyItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        MinecraftServer server = sp.getServer();
        if (server == null) return InteractionResultHolder.fail(stack);

        if (level.dimension() == ModDimensions.SHADOW_REALM) {
            goBack(sp, server);
        } else {
            goToRealm(sp, server);
        }
        sp.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.success(stack);
    }

    private void goToRealm(ServerPlayer sp, MinecraftServer server) {
        ServerLevel realm = server.getLevel(ModDimensions.SHADOW_REALM);
        if (realm == null) {
            StatsManager.message(sp, Component.literal("Изнанка недоступна (измерение не загружено).").withStyle(ChatFormatting.RED));
            return;
        }
        CompoundTag ret = new CompoundTag();
        ret.putString("dim", sp.level().dimension().location().toString());
        ret.putDouble("x", sp.getX());
        ret.putDouble("y", sp.getY());
        ret.putDouble("z", sp.getZ());
        sp.getPersistentData().put(RETURN_TAG, ret);

        int x = (int) Math.floor(sp.getX());
        int z = (int) Math.floor(sp.getZ());
        BlockPos pos = findSafe(realm, x, z);
        sp.teleportTo(realm, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, sp.getYRot(), sp.getXRot());
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        realm.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.6f, 1.4f);
        StatsManager.message(sp, Component.literal("Вы вошли в Изнанку. Монстры здесь от 10 уровня. Используйте ключ снова, чтобы вернуться.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void goBack(ServerPlayer sp, MinecraftServer server) {
        CompoundTag ret = sp.getPersistentData().getCompound(RETURN_TAG);
        ServerLevel target = null;
        double x, y, z;
        if (ret.contains("dim")) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(ret.getString("dim")));
            target = server.getLevel(key);
        }
        if (target == null) {
            target = server.overworld();
            BlockPos spawn = target.getSharedSpawnPos();
            BlockPos safe = findSafe(target, spawn.getX(), spawn.getZ());
            x = safe.getX() + 0.5; y = safe.getY(); z = safe.getZ() + 0.5;
        } else {
            x = ret.getDouble("x"); y = ret.getDouble("y"); z = ret.getDouble("z");
        }
        sp.teleportTo(target, x, y, z, sp.getYRot(), sp.getXRot());
        StatsManager.message(sp, Component.literal("Вы вернулись из Изнанки."));
    }

    private static BlockPos findSafe(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() + 1) {
            y = 80;
        }
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolid() || level.getBlockState(below).is(Blocks.WATER)) {
            // маленькая площадка, чтобы не упасть в воду или пустоту
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    level.setBlockAndUpdate(below.offset(dx, 0, dz), Blocks.DEEPSLATE_BRICKS.defaultBlockState());
        }
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos.above(), Blocks.AIR.defaultBlockState());
        return pos;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.litrpg.system_key.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
