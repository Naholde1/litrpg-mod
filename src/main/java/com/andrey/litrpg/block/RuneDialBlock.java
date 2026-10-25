package com.andrey.litrpg.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Рунный диск: на лицевой стороне один из шести символов (огонь, вода, луна, солнце, череп, око).
 * ПКМ — следующий символ. Загадки этапа ({@link com.andrey.litrpg.run.Mechanisms}) читают символы дисков.
 */
public class RuneDialBlock extends HorizontalDirectionalBlock {
    public static final IntegerProperty SYMBOL = IntegerProperty.create("symbol", 0, 5);
    public static final String[] NAMES = {"Огонь", "Вода", "Луна", "Солнце", "Череп", "Око"};

    public RuneDialBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SYMBOL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SYMBOL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level instanceof ServerLevel sl) {
            if (com.andrey.litrpg.run.Mechanisms.locked(sl, pos)) {
                level.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
                return InteractionResult.CONSUME;
            }
            BlockState next = st.setValue(SYMBOL, (st.getValue(SYMBOL) + 1) % 6);
            level.setBlock(pos, next, 3);
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.6f, 1.3f + st.getValue(SYMBOL) * 0.08f);
            player.displayClientMessage(Component.literal("Символ: " + NAMES[next.getValue(SYMBOL)]).withStyle(ChatFormatting.GOLD), true);
            com.andrey.litrpg.run.Mechanisms.changed(sl, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
