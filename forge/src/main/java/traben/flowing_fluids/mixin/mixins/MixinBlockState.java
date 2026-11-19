// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FlowingFluids;

@Mixin({BlockStateBase.class})
public abstract class MixinBlockState extends StateHolder<Block, BlockState> {
   @Shadow
   @Final
   @Deprecated
   private boolean liquid;

   @Shadow
   public abstract FluidState getFluidState();

   protected MixinBlockState() {
      super(null, null, null);
   }

   @Inject(
      method = {"isRandomlyTicking"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void flowing_fluids$overrideRandomTickCheck(CallbackInfoReturnable<Boolean> cir) {
      if (this.liquid && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this.getFluidState())) {
         cir.setReturnValue(true);
      }
   }
}
