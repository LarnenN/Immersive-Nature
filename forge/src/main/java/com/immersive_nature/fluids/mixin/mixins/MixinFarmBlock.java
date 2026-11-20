// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({FarmBlock.class})
public class MixinFarmBlock {
   @Inject(
      method = {"isNearWater"},
      at = {@At(
         value = "RETURN",
         ordinal = 0
      )}
   )
   private static void ff$drainWater(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) BlockPos blockPos) {
      if (FlowingFluids.config.enableMod
         && FlowingFluids.config.farmlandDrainWaterChance > 0.0F
         && FlowingFluids.config.isWaterAllowed()
         && level instanceof ServerLevel serverLevel
         && !FlowingFluids.config.dontTickAtLocation(blockPos, serverLevel)
         && serverLevel.random.nextFloat() <= FlowingFluids.config.farmlandDrainWaterChance) {
         FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER, 1);
      }
   }
}
