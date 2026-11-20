package com.immersive_nature.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Immersive Nature: with finite water (Flowing Fluids, erosion, evaporation) the water under a
 * lily pad can drain away without the lily pad getting a block update, leaving it lying on dry
 * ground. Lily pads now check from time to time that they still float on water and pop off if not.
 */
@Mixin(WaterlilyBlock.class)
public abstract class WaterlilyBlockMixin extends BushBlock {

    protected WaterlilyBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }
}
