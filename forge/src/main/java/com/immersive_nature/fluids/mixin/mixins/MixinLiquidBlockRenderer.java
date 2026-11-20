// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.LiquidBlockRenderer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.config.FFConfig;

@Mixin(
   value = {LiquidBlockRenderer.class},
   priority = 1001
)
public abstract class MixinLiquidBlockRenderer {
   @ModifyVariable(
      method = {"tesselate"},
      at = @At(
         value = "INVOKE_ASSIGN",
         target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
         shift = Shift.AFTER
      ),
      ordinal = 0
   )
   private Vec3 ff$alterFlowDir(Vec3 value) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.hideFlowingTexture ? Vec3.ZERO : value;
   }

   @ModifyVariable(
      method = {"tesselate"},
      at = @At("STORE"),
      ordinal = 0
   )
   private float ff$f(float value, @Local(argsOnly = true) FluidState fluidState) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours
         ? (float)(FFConfig.waterLevelColours[fluidState.getAmount() - 1] >> 16 & 0xFF) / 255.0F
         : value;
   }

   @ModifyVariable(
      method = {"tesselate"},
      at = @At("STORE"),
      ordinal = 1
   )
   private float ff$g(float value, @Local(argsOnly = true) FluidState fluidState) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours
         ? (float)(FFConfig.waterLevelColours[fluidState.getAmount() - 1] >> 8 & 0xFF) / 255.0F
         : value;
   }

   @ModifyVariable(
      method = {"tesselate"},
      at = @At("STORE"),
      ordinal = 2
   )
   private float ff$h(float value, @Local(argsOnly = true) FluidState fluidState) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours
         ? (float)(FFConfig.waterLevelColours[fluidState.getAmount() - 1] & 0xFF) / 255.0F
         : value;
   }
}
