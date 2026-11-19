// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.api;

import it.unimi.dsi.fastutil.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

public interface FlowingFluidsAPI {
   int VERSION = 3;
   int FLUID_LEVELS_PER_BLOCK = 8;

   static FlowingFluidsAPI getInstance(@NotNull String modIdRequestingAPI) {
      FlowingFluids.info("API requested by " + modIdRequestingAPI + ", version=3");
      return new FlowingFluidsApiImpl(modIdRequestingAPI);
   }

   boolean isModEnabled();

   boolean isModCurrentlyMovingFluids();

   boolean doesModifyThisFluid(@NotNull Fluid var1);

   boolean doesModifyThisFluid(@NotNull FluidState var1);

   void disableThisFluid(@NotNull Fluid var1, @Nullable MinecraftServer var2);

   void disableThisFluid(@NotNull FluidState var1, @Nullable MinecraftServer var2);

   void enableThisFluid(@NotNull Fluid var1, @Nullable MinecraftServer var2);

   void enableThisFluid(@NotNull FluidState var1, @Nullable MinecraftServer var2);

   FFConfig getConfig();

   void setConfig(@NotNull FFConfig var1, @Nullable MinecraftServer var2);

   int removeFluidAmountFromPos(@NotNull LevelAccessor var1, @NotNull BlockPos var2, @NotNull Fluid var3, int var4, int var5);

   int placeFluidAmountFromPos(@NotNull LevelAccessor var1, @NotNull BlockPos var2, @NotNull Fluid var3, int var4, boolean var5, boolean var6);

   void registerBlockTagThatWontDisplaceFluid(@NotNull Fluid var1, @NotNull TagKey<Block> var2);

   void registerBlockThatWontDisplaceFluid(@NotNull Fluid var1, @NotNull Block var2);

   void registerBlockTagStringIdThatWontDisplaceFluid(@NotNull Fluid var1, @NotNull String var2);

   void registerBlockStringIdThatWontDisplaceFluid(@NotNull Fluid var1, @NotNull String var2);

   void registerBiomeTagThatHasInfiniteWaterRefilling(@NotNull TagKey<Biome> var1);

   void registerBiomeThatHasInfiniteWaterRefilling(@NotNull ResourceKey<Biome> var1);

   boolean doesBiomeInfiniteWaterRefill(@NotNull Holder<Biome> var1);

   boolean modifyFluidAmountAtPos(@NotNull LevelAccessor var1, @NotNull BlockPos var2, @NotNull FlowingFluidsAPI.FluidAmountModifier var3);

   boolean modifyFluidAmountAtPos(@NotNull LevelAccessor var1, @NotNull BlockPos var2, @NotNull Fluid var3, int var4);

   public interface FluidAmountModifier {
      @NotNull
      Pair<Fluid, Integer> getNewAmount(@NotNull Fluid var1, int var2);
   }
}
