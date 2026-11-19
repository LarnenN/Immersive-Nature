// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins.sodium;

import java.util.Arrays;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.FluidRenderer;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.At.Shift;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

@Pseudo
@Mixin({FluidRenderer.class})
public abstract class MixinFluidRenderer2 {
   @Unique
   private static final ColorProvider<FluidState> ff$waterLevelColours = (worldSlice, blockPos, fluidState, modelQuadView, ints) -> Arrays.fill(
         ints, FFConfig.waterLevelColours[fluidState.getAmount() - 1]
      );

   @ModifyVariable(
      method = {"render"},
      at = @At(
         value = "INVOKE_ASSIGN",
         target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;",
         shift = Shift.AFTER
      ),
      ordinal = 0,
      require = 0
   )
   private Vec3 ff$alterFlowDir(Vec3 value) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.hideFlowingTexture ? Vec3.ZERO : value;
   }

   @ModifyVariable(
      method = {"render"},
      at = @At("HEAD"),
      ordinal = 2,
      require = 0,
      remap = false
   )
   private ColorProvider<FluidState> ff$alterColor(ColorProvider<FluidState> value) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.debugWaterLevelColours ? ff$waterLevelColours : value;
   }
}
