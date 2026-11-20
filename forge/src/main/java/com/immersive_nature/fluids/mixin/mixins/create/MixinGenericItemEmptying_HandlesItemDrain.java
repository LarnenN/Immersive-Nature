// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins.create;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FlowingFluids;

@Pseudo
@Mixin({GenericItemEmptying.class})
public abstract class MixinGenericItemEmptying_HandlesItemDrain {
   @Inject(
      method = {"emptyItem"},
      at = {@At("RETURN")},
      remap = false
   )
   private static void ff$onEmptyItem(Level level, ItemStack stack, boolean simulate, CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir) {
      if (level != null
         && FlowingFluids.config.enableMod
         && FlowingFluids.config.create_waterWheelMode.needsFlow()
         && FlowingFluids.config.isFluidAllowed(((FluidStack)((Pair)cir.getReturnValue()).getFirst()).getFluid())) {
         if (stack.getItem() instanceof BucketItem) {
            int damage = stack.getDamageValue();
            if (damage == 0) {
               return;
            }

            ((FluidStack)((Pair)cir.getReturnValue()).getFirst()).setAmount(waterModified(8 - damage));
         } else if (stack.getItem() instanceof BottleItem) {
            int damage = stack.getDamageValue();
            if (damage == 0) {
               return;
            }

            ((FluidStack)((Pair)cir.getReturnValue()).getFirst()).setAmount(3 - damage);
         }
      }
   }

   @Unique
   private static int waterModified(int level) {
      if (level == 0) {
         return 0;
      } else {
         return level == 8 ? 1000 : 125 * level;
      }
   }
}
