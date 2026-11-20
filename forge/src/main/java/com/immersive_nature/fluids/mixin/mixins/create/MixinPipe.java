// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import com.immersive_nature.fluids.FlowingFluids;

@Pseudo
@Mixin({OpenEndedPipe.class})
public abstract class MixinPipe {
   @Shadow
   private Level world;
   @Shadow
   private BlockPos outputPos;

   @ModifyArg(
      method = {"removeFluidFromSpace"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
         ordinal = 1
      ),
      index = 1
   )
   private BlockState ff$modifyWaterRemoval(BlockState blockState) {
      if (FlowingFluids.config.enableMod
         && !FlowingFluids.config.create_infinitePipes
         && FlowingFluids.config.isFluidAllowed(blockState.getFluidState())
         && (Boolean)AllConfigs.server().fluids.pipesPlaceFluidSourceBlocks.get()) {
         this.world.getChunkAt(this.outputPos).getBlockTicks().schedule(new ScheduledTick(blockState.getBlock(), this.outputPos, this.world.getGameTime() + 5L, 99L));
         return Blocks.AIR.defaultBlockState();
      } else {
         return blockState;
      }
   }

   @WrapOperation(
      method = {"removeFluidFromSpace"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z"
      )},
      remap = false,
      require = 0
   )
   private boolean ff$disableImmediateRefillShortcut(Object potentiallyFilled, Object originalFluidState, Operation<Boolean> original) {
      boolean result = (Boolean)original.call(new Object[]{potentiallyFilled, originalFluidState});
      if (!result) {
         return false;
      } else if (!FlowingFluids.config.enableMod || FlowingFluids.config.create_infinitePipes) {
         return true;
      } else {
         return originalFluidState instanceof FluidState fluidState ? !FlowingFluids.config.isFluidAllowed(fluidState) : true;
      }
   }
}
