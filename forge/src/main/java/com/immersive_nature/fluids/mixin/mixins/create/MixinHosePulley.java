// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.fluid.FluidHelper;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Pseudo
@Mixin({HosePulleyFluidHandler.class})
public abstract class MixinHosePulley {
   @Shadow(
      remap = false
   )
   private FluidDrainingBehaviour drainer;
   @Shadow(
      remap = false
   )
   private FluidFillingBehaviour filler;
   private static final String INSERT_METHOD_FF = "fill";

   @WrapOperation(
      method = {"drainInternal"},
      remap = false,
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;getDrainableFluid(Lnet/minecraft/core/BlockPos;)Lnet/minecraftforge/fluids/FluidStack;"
      )}
   )
   private FluidStack ff$modifyWaterRemoval1(FluidDrainingBehaviour instance, BlockPos blockPos, Operation<FluidStack> original) {
      if (FlowingFluids.config.enableMod) {
         Fluid fluid = ((FluidDrainingBehaviourAccessor)this.drainer).ff$getFluid();
         if (fluid == null) {
            Fluid newFluid = this.drainer.getWorld().getFluidState(blockPos).getType();
            if (FlowingFluids.config.isFluidAllowed(newFluid)) {
               if (((FluidDrainingBehaviourAccessor)this.drainer).ff$getFluid() == null) {
                  ((FluidDrainingBehaviourAccessor)this.drainer).ff$setFluid(FluidHelper.convertToStill(newFluid));
               }

               fluid = newFluid;
            }
         }

         if (fluid == Fluids.EMPTY) {
            return FluidStack.EMPTY;
         }

         if (FlowingFluids.config.isFluidAllowed(fluid)) {
            Fluid source = FluidHelper.convertToStill(fluid);
            return new FluidStack(source, 1000);
         }
      }

      return (FluidStack)original.call(new Object[]{instance, blockPos});
   }

   @WrapOperation(
      method = {"drainInternal"},
      remap = false,
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidDrainingBehaviour;pullNext(Lnet/minecraft/core/BlockPos;Z)Z"
      )}
   )
   private boolean ff$modifyWaterRemoval2(
      FluidDrainingBehaviour instance,
      BlockPos blockPos,
      boolean simulate,
      Operation<Boolean> original,
      @Share("foundLevels") LocalIntRef foundLevels,
      @Local(ordinal = 0,argsOnly = true) int maxAmount
   ) {
      foundLevels.set(8);
      if (FlowingFluids.config.enableMod) {
         Level world = this.drainer.getWorld();
         BlockState state = world.getBlockState(blockPos);
         FluidState fluidState = state.getFluidState();
         if (fluidState.isEmpty()) {
            return false;
         }

         if (FlowingFluids.config.isFluidAllowed(fluidState) && fluidState.getType() instanceof FlowingFluid flowing) {
            if (simulate) {
               return true;
            }

            ((FluidManipulationBehaviourAccessor)this.drainer).ff$PlayEffect(world, blockPos, flowing, true);
            this.drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY);
            if (this.drainer.isInfinite() && FluidHelper.isLava(flowing)) {
               this.drainer.blockEntity.award(AllAdvancements.HOSE_PULLEY_LAVA);
            }

            if (!FlowingFluids.config.create_infinitePipes && !this.drainer.isInfinite()) {
               Pair<Integer, Runnable> data = FFFluidUtils.collectConnectedFluidAmountAndRemoveAction(world, blockPos, 1, 8, flowing);
               Integer found = (Integer)data.first();
               if (found == 0) {
                  return false;
               }

               ((Runnable)data.second()).run();
               foundLevels.set(found);
               return true;
            }

            return true;
         }
      }

      return (Boolean)original.call(new Object[]{instance, blockPos, simulate});
   }

   @ModifyConstant(
      method = {"drainInternal"},
      constant = {@Constant(
         intValue = 1000,
         ordinal = 1
      )},
      remap = false
   )
   private int ff$modifyWaterRemoval3(int original, @Share("foundLevels") LocalIntRef foundLevels) {
      return this.waterModified(foundLevels.get());
   }

   @WrapOperation(
      method = {"fill"},
      remap = false,
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/fluids/transfer/FluidFillingBehaviour;tryDeposit(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/core/BlockPos;Z)Z"
      )}
   )
   private boolean ff$modifyWaterPlacing(
      FluidFillingBehaviour instance, Fluid fluid, BlockPos blockPos, boolean b, Operation<Boolean> original, @Share("placedLevels") LocalIntRef placedLevels
   ) {
      placedLevels.set(8);
      if (FlowingFluids.config.enableMod && !FlowingFluids.config.create_infinitePipes && FlowingFluids.config.isFluidAllowed(fluid)) {
         Level world = this.filler.getWorld();
         int remainder = FFFluidUtils.addAmountToFluidAtPosWithRemainder(world, blockPos.below(), fluid, 8);
         if (remainder == 8) {
            return false;
         } else {
            placedLevels.set(8 - remainder);
            return true;
         }
      } else {
         return (Boolean)original.call(new Object[]{instance, fluid, blockPos, b});
      }
   }

   @ModifyConstant(
      method = {"fill"},
      constant = {@Constant(
         intValue = 1000,
         ordinal = 1
      )},
      remap = false
   )
   private int ff$modifyWaterPlacing1(int original, @Share("placedLevels") LocalIntRef placedLevels) {
      return this.waterModified(placedLevels.get());
   }

   @ModifyConstant(
      method = {"fill"},
      constant = {@Constant(
         intValue = 1000,
         ordinal = 2
      )},
      remap = false
   )
   private int ff$modifyWaterPlacing2(int original, @Share("placedLevels") LocalIntRef placedLevels) {
      return this.waterModified(placedLevels.get());
   }

   @Inject(
      method = {"fill"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraftforge/fluids/FluidStack;shrink(I)V"
      )},
      remap = false
   )
   private void ff$modifyWaterPlacing3(
      FluidStack resource,
      FluidAction action,
      CallbackInfoReturnable<Integer> cir,
      @Share("placedLevels") LocalIntRef placedLevels,
      @Local(name = {"diff"}) LocalIntRef diff
   ) {
      if (placedLevels.get() != 8) {
         int newDiff = diff.get() + 1000;
         diff.set(newDiff - this.waterModified(placedLevels.get()));
      }
   }

   @Unique
   private int waterModified(int level) {
      if (level == 0) {
         return 0;
      } else {
         return level == 8 ? 1000 : 125 * level;
      }
   }
}
