package com.andrey.litrpg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Зеркало на поворотной подставке. TURN 0 — «/», 1 — «\» (если смотреть сверху); ПКМ — повернуть.
 * Луч света ({@link com.andrey.litrpg.run.Mechanisms}) отражается: «/» меняет север↔восток и юг↔запад, «\» — север↔запад и юг↔восток.
 */
public class MirrorBlock extends Block {
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 1);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 15, 15);

    public MirrorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(TURN, 0));
    }

    /** «/» (true) или «\» (false). */
    public static boolean slash(BlockState st) {
        return st.getValue(TURN) == 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(TURN);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState st, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level instanceof ServerLevel sl) {
            if (com.andrey.litrpg.run.Mechanisms.locked(sl, pos)) return InteractionResult.CONSUME;
            level.setBlock(pos, st.setValue(TURN, 1 - st.getValue(TURN)), 3);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_STEP, SoundSource.BLOCKS, 1f, 1.4f);
            com.andrey.litrpg.run.Mechanisms.changed(sl, pos, player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
