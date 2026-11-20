package com.immersive_nature.data.block_growths;

import net.minecraft.world.level.block.state.BlockState;

public interface IConditionalGrowingBlock {

    boolean canGrow(BlockState state);
}
