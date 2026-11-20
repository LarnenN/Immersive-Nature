// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.KelpBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({KelpBlock.class})
public abstract class MixinKelpBlock extends GrowingPlantHeadBlock {
   protected MixinKelpBlock() {
      super(null, null, null, false, 0.0);
   }

   @ModifyReturnValue(
      method = {"canGrowInto"},
      at = {@At("RETURN")}
   )
   boolean ff$canGrowInto(boolean original, @Local(argsOnly = true) BlockState state) {
      return original && FlowingFluids.config.enableMod && FlowingFluids.config.isWaterAllowed() ? state.getFluidState().getAmount() >= 8 : original;
   }
}
