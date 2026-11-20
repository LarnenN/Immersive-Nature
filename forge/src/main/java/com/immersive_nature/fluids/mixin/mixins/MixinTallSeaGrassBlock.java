// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.TallSeagrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({TallSeagrassBlock.class})
public class MixinTallSeaGrassBlock {
   @Inject(
      method = {"canSurvive"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void flowing_fluids$removeHeightCheck(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
      if (FlowingFluids.config.enableMod
         && level instanceof LevelAccessor accessor
         && (Boolean)cir.getReturnValue()
         && FlowingFluids.config.isWaterAllowed()
         && state.getValue(TallSeagrassBlock.HALF) == DoubleBlockHalf.UPPER
         && FFFluidUtils.canFluidFlowToNeighbourFromPos(accessor, pos, Fluids.WATER, 8)) {
         cir.setReturnValue(false);
      }
   }
}
