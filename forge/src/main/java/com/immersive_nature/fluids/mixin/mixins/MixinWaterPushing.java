// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({Entity.class})
public class MixinWaterPushing {
   private static final String METHOD_NAME = "updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V";

   @ModifyExpressionValue(
      method = {"updateFluidHeightAndDoFluidPushing(Ljava/util/function/Predicate;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;isPushedByFluid()Z"
      )},
      require = 0
   )
   private boolean ff$isPushed(boolean original) {
      if (!original) {
         return false;
      } else if (FlowingFluids.config.enableMod) {
         if ((Object)this instanceof Player) {
            return FlowingFluids.config.waterFlowAffectsPlayers;
         } else if ((Object)this instanceof Boat) {
            return FlowingFluids.config.waterFlowAffectsBoats;
         } else {
            return (Object)this instanceof ItemEntity ? FlowingFluids.config.waterFlowAffectsItems : FlowingFluids.config.waterFlowAffectsEntities;
         }
      } else {
         return true;
      }
   }
}
