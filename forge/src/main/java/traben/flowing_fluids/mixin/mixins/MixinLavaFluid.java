// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.LavaFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin({LavaFluid.class})
public abstract class MixinLavaFluid extends FlowingFluid {
   @Unique
   private static Random random = new Random();

   @Shadow
   public abstract boolean isSame(Fluid var1);

   @Inject(
      method = {"canBeReplacedWith"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void flowing_fluids$removeHeightCheck(
      FluidState fluidState, BlockGetter blockGetter, BlockPos blockPos, Fluid fluid, Direction direction, CallbackInfoReturnable<Boolean> cir
   ) {
      if (FlowingFluids.config.enableMod && !(Boolean)cir.getReturnValue() && FlowingFluids.config.isFluidAllowed(this)) {
         cir.setReturnValue(fluid.isSame(Fluids.WATER));
      }
   }

   @Inject(
      method = {"beforeDestroyingBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ff$fizzOnlyForNonLava(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this) && this.isSame(state.getFluidState().getType())) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"getSlopeFindDistance"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ff$modifySlopeDistance(LevelReader level, CallbackInfoReturnable<Integer> cir) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         cir.setReturnValue(
            Mth.clamp(
               FFFluidUtils.dimensionEvaporatesWaterVanilla(level) ? FlowingFluids.config.lavaNetherFlowDistance : FlowingFluids.config.lavaFlowDistance, 1, 8
            )
         );
      }
   }

   @Inject(
      method = {"getTickDelay"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ff$modifyTickDelay(LevelReader level, CallbackInfoReturnable<Integer> cir) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         int value = Mth.clamp(
            FFFluidUtils.dimensionEvaporatesWaterVanilla(level) ? FlowingFluids.config.lavaNetherTickDelay : FlowingFluids.config.lavaTickDelay, 1, 255
         );
         if (FlowingFluids.config.tickDelaySpread > 0) {
            value -= random.nextInt(FlowingFluids.config.tickDelaySpread + 1);
         }

         cir.setReturnValue(value);
      }
   }

   @Inject(
      method = {"randomTick"},
      at = {@At("HEAD")}
   )
   private void ff$callSuper(Level level, BlockPos pos, FluidState state, RandomSource random, CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         super.randomTick(level, pos, state, random);
      }
   }

   @Inject(
      method = {"spreadTo"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/material/LavaFluid;fizz(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"
      )}
   )
   private void ff$consumeLevelStoneCreation(
      LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, FluidState fluidState, CallbackInfo ci
   ) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         BlockPos above = blockPos.above();
         FFFluidUtils.setFluidStateAtPosToNewAmount(levelAccessor, above, Fluids.LAVA, levelAccessor.getFluidState(above).getAmount() - 1);
      }
   }
}
