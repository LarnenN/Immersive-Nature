// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin({BottleItem.class})
public class MixinBottleItem {
   @ModifyArg(
      method = {"use"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"
      ),
      index = 2
   )
   private Fluid flowing_fluids$allowAnyFluid(Fluid par3) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.isWaterAllowed() && par3 == Fluid.SOURCE_ONLY ? Fluid.ANY : par3;
   }

   @Inject(
      method = {"use"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
         ordinal = 1,
         shift = Shift.BEFORE
      )},
      cancellable = true
   )
   private void ff$drainWater(
      CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, @Local ItemStack itemStack, @Local(argsOnly = true) Level level, @Local BlockPos blockPos
   ) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isWaterAllowed()) {
         int foundAmount = FFFluidUtils.collectConnectedFluidAmountAndRemove(level, blockPos, 2, 3, Fluids.WATER);
         if (foundAmount == 0) {
            cir.setReturnValue(InteractionResultHolder.pass(itemStack));
         }
      }
   }
}
