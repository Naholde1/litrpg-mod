package com.andrey.litrpg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Рунная преграда: перекрывает путь дальше по большому подземелью, пока не пал хранитель
 * с той же ступенью ({@code stage}). Неразрушима.
 */
public class SealDoorBlock extends Block {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);

    public SealDoorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAGE);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(),
                    pos.getZ() + r.nextDouble(), 0, 0.05, 0);
        }
    }
}
