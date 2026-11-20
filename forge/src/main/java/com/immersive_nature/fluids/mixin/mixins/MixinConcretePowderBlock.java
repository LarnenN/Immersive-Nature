// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({ConcretePowderBlock.class})
public class MixinConcretePowderBlock {
   @ModifyExpressionValue(
      method = {"touchesLiquid"},
      require = 0,
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/state/BlockState;isFaceSturdy(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"
      )}
   )
   private static boolean ff$drainWater(boolean original, @Local(ordinal = 0) MutableBlockPos blockPos, @Local(ordinal = 0,argsOnly = true) BlockGetter getter) {
      if (!original
         && FlowingFluids.config.enableMod
         && FlowingFluids.config.concreteDrainsWaterChance > 0.0F
         && FlowingFluids.config.isWaterAllowed()
         && getter instanceof ServerLevel serverLevel
         && !FlowingFluids.config.dontTickAtLocation(blockPos, serverLevel)
         && serverLevel.random.nextFloat() <= FlowingFluids.config.concreteDrainsWaterChance) {
         FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(serverLevel, blockPos, Fluids.WATER, 1);
      }

      return original;
   }
}
