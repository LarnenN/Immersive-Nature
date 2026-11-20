// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.PlugWaterFeature;

@Mixin({ChunkGenerator.class})
public abstract class MixinAfterChunkGen {
   @Inject(
      method = {"applyBiomeDecoration"},
      at = {@At("TAIL")}
   )
   private void ff$postGen(WorldGenLevel worldGenLevel, ChunkAccess chunkAccess, StructureManager structureManager, CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.encloseAllFluidOnWorldGen) {
         PlugWaterFeature.processChunk(worldGenLevel, chunkAccess.getPos(), chunkAccess);
      }
   }
}
