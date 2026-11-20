// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Boat.Status;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({Boat.class})
public abstract class MixinBoatFloating extends Entity {
   public MixinBoatFloating() {
      super(null, null);
   }

   @WrapOperation(
      method = {"getStatus"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/vehicle/Boat;isUnderwater()Lnet/minecraft/world/entity/vehicle/Boat$Status;"
      )}
   )
   private Status ff$float1(Boat instance, Operation<Status> original) {
      return FlowingFluids.config.enableMod && !FlowingFluids.config.waterFlowAffectsBoats ? null : (Status)original.call(new Object[]{instance});
   }

   @ModifyExpressionValue(
      method = {"checkInWater"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/util/Mth;ceil(D)I",
         ordinal = 1
      )}
   )
   private int ff$float2(int original, @Local(ordinal = 0) AABB aabb) {
      return FlowingFluids.config.enableMod && !FlowingFluids.config.waterFlowAffectsBoats ? Mth.ceil(aabb.maxY) : original;
   }
}
