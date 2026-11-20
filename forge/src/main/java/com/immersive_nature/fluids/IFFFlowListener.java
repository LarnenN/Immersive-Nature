// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.material.Fluid;

public interface IFFFlowListener {
   void ff$acceptRecentFlow(BlockPos var1, Direction var2, Fluid var3, boolean var4);
}
