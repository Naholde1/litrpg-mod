package com.andrey.litrpg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Узел силы на арене хранителя: филактерия лича, сердце стужи, якорь душ, кокон, горн…
 * Хранители питаются узлами (щит, воскрешение, лечение) — их надо разбить в бою.
 * После сброса боя хранитель восстанавливает узлы.
 */
public class RelicBlock extends Block {
    public enum Kind implements StringRepresentable {
        SOUL, FROST, FIRE, NATURE, WEB, RUNE, TIME, GOLD, VOID;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }

        ParticleOptions particle() {
            return switch (this) {
                case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
                case FROST -> ParticleTypes.SNOWFLAKE;
                case FIRE -> ParticleTypes.FLAME;
                case NATURE -> ParticleTypes.SPORE_BLOSSOM_AIR;
                case WEB -> ParticleTypes.WHITE_ASH;
                case RUNE -> ParticleTypes.ENCHANT;
                case TIME -> ParticleTypes.END_ROD;
                case GOLD -> ParticleTypes.WAX_ON;
                case VOID -> ParticleTypes.REVERSE_PORTAL;
            };
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);

    public RelicBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.SOUL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(KIND);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        ParticleOptions p = state.getValue(KIND).particle();
        level.addParticle(p, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.8, pos.getY() + 1.05,
                pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.8, 0, 0.03, 0);
    }
}
