// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFlowListenerLevel;
import traben.flowing_fluids.FFFluidUtils;

@Mixin({Level.class})
public abstract class MixinLevel implements FFFlowListenerLevel {
   @Unique
   Object2ObjectOpenHashMap<BlockPos, Set<BlockPos>> ff$flowListenerPositions = null;

   @Inject(
      method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   private void flowing_fluids$displaceFluids(
      BlockPos pos,
      BlockState state,
      int flags,
      int recursionLeft,
      CallbackInfoReturnable<Boolean> cir,
      @Local LevelChunk levelChunk,
      @Local(ordinal = 1) BlockState originalState
   ) {
      FFFluidUtils.displaceFluids((Level)(Object)this, pos, state, flags, levelChunk, originalState);
   }

   @Override
   public Map<BlockPos, Set<BlockPos>> ff$getFlowListenerPositions() {
      if (this.ff$flowListenerPositions == null) {
         this.ff$flowListenerPositions = new Object2ObjectOpenHashMap();
      }

      return this.ff$flowListenerPositions;
   }
}
