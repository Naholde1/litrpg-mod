package com.andrey.litrpg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Разлом: светящаяся пелена прохода на следующий этап (сам переход — по положению игрока, см. {@link com.andrey.litrpg.run.Run}). */
public class RiftBlock extends Block {
    public RiftBlock(Properties p) {
        super(p);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(),
                    pos.getZ() + r.nextDouble(), 0, 0.05, 0);
        }
        if (r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble(),
                    pos.getZ() + r.nextDouble(), 0, 0.02, 0);
        }
    }
}
