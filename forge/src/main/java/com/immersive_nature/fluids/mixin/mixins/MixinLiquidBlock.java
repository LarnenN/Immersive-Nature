// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({LiquidBlock.class})
public abstract class MixinLiquidBlock extends Block implements BucketPickup {
   @Shadow
   @Final
   protected FlowingFluid fluid;

   public MixinLiquidBlock() {
      super(null);
   }

   @ModifyExpressionValue(
      method = {"shouldSpreadLiquid"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/material/FluidState;is(Lnet/minecraft/tags/TagKey;)Z"
      )}
   )
   private boolean ff$consumeLevelObsidianOrCobbleCreation(boolean original, @Local(argsOnly = true) Level level, @Local(ordinal = 1) BlockPos blockPos) {
      if (original && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this.fluid)) {
         FluidState state = level.getFluidState(blockPos);
         FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, state.getType(), state.getAmount() - 1);
      }

      return original;
   }

   @WrapOperation(
      method = {"shouldSpreadLiquid"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/material/FluidState;isSource()Z"
      )}
   )
   private boolean ff$modifyObsidianCondition(FluidState instance, Operation<Boolean> original) {
      boolean source = (Boolean)original.call(new Object[]{instance});
      return !source
            && FlowingFluids.config.enableMod
            && FlowingFluids.config.isFluidAllowed(this.fluid)
            && instance.getAmount() >= FlowingFluids.config.minLavaLevelForObsidian
         ? true
         : source;
   }

   @Inject(
      method = {"pickupBlock"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ff$modifyBucket(
      CallbackInfoReturnable<ItemStack> cir,
      @Local(argsOnly = true,ordinal = 0) LevelAccessor levelAccessor,
      @Local(argsOnly = true,ordinal = 0) BlockPos blockPos
   ) {
      if (((ItemStack)cir.getReturnValue()).isEmpty() && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this.fluid)) {
         int level = FFFluidUtils.collectConnectedFluidAmountAndRemove(levelAccessor, blockPos, 1, 8, this.fluid);
         if (level > 0) {
            if (level >= 8) {
               cir.setReturnValue(new ItemStack(this.fluid.getBucket()));
            } else {
               ItemStack stack = new ItemStack(this.fluid.getBucket());
               stack.setDamageValue(8 - level);
               cir.setReturnValue(stack);
            }
         }
      }
   }
}
