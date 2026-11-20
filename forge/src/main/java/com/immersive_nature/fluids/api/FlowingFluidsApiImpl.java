// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.api;

import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.config.FFConfig;

public class FlowingFluidsApiImpl implements FlowingFluidsAPI {
   private final String modid;

   FlowingFluidsApiImpl(String modIdRequestingAPI) {
      this.modid = modIdRequestingAPI;
   }

   @Override
   public boolean isModEnabled() {
      return this.getConfig().enableMod;
   }

   @Override
   public boolean isModCurrentlyMovingFluids() {
      return FlowingFluids.isManeuveringFluids;
   }

   @Override
   public boolean doesModifyThisFluid(@NotNull Fluid fluid) {
      return this.getConfig().isFluidAllowed(fluid);
   }

   @Override
   public boolean doesModifyThisFluid(@NotNull FluidState fluid) {
      return this.getConfig().isFluidAllowed(fluid);
   }

   @Override
   public void disableThisFluid(@NotNull Fluid fluid, @Nullable MinecraftServer server) {
      if (fluid != Fluids.EMPTY) {
         FlowingFluid flowing = this.fluidCheck(fluid, "disableThisFluid");
         if (flowing != null) {
            this.getConfig().fluidBlacklist.add(BuiltInRegistries.FLUID.getKey(flowing.getSource()).toString());
            this.getConfig().fluidBlacklist.add(BuiltInRegistries.FLUID.getKey(flowing.getFlowing()).toString());
            this.setConfig(this.getConfig(), server);
         }
      }
   }

   @Override
   public void disableThisFluid(@NotNull FluidState fluid, @Nullable MinecraftServer server) {
      this.disableThisFluid(fluid.getType(), server);
   }

   @Override
   public void enableThisFluid(@NotNull Fluid fluid, @Nullable MinecraftServer server) {
      if (fluid != Fluids.EMPTY) {
         FlowingFluid flowing = this.fluidCheck(fluid, "enableThisFluid");
         if (flowing != null) {
            this.getConfig().fluidBlacklist.remove(BuiltInRegistries.FLUID.getKey(flowing.getSource()).toString());
            this.getConfig().fluidBlacklist.remove(BuiltInRegistries.FLUID.getKey(flowing.getFlowing()).toString());
            this.setConfig(this.getConfig(), server);
         }
      }
   }

   @Override
   public void enableThisFluid(@NotNull FluidState fluid, @Nullable MinecraftServer server) {
      this.enableThisFluid(fluid.getType(), server);
   }

   @Override
   public FFConfig getConfig() {
      return FlowingFluids.config;
   }

   @Override
   public void setConfig(@NotNull FFConfig config, @Nullable MinecraftServer server) {
      FlowingFluids.config = config;
      FlowingFluids.saveConfig();
      if (server != null) {
         FlowingFluids.info("Flowing Fluids config was changed by the " + this.modid + " mod, and will be applied to the server.");
         server.getPlayerList().getPlayers().forEach(FlowingFluids::sendConfigToClient);
      } else {
         FlowingFluids.warn(
            "Flowing Fluids config was changed by the "
               + this.modid
               + " mod, but no server was provided. This will only modify the local config for single-player etc."
         );
      }
   }

   private boolean clientCheck(LevelAccessor level, String str) {
      if (level.isClientSide()) {
         FlowingFluids.warn("Flowing Fluids API: " + str + " was called on a client side level. This does nothing. Mod that called it: " + this.modid);
         return true;
      } else {
         return false;
      }
   }

   @Nullable
   private FlowingFluid fluidCheck(Fluid fluid, String str) {
      if (fluid instanceof FlowingFluid) {
         return (FlowingFluid)fluid;
      } else {
         FlowingFluids.warn("Flowing Fluids API: " + str + " was called on a non-flowing fluid. This is not allowed. Mod that called it: " + this.modid);
         return null;
      }
   }

   @Override
   public int removeFluidAmountFromPos(@NotNull LevelAccessor level, @NotNull BlockPos startPos, @NotNull Fluid fluid, int minAmount, int maxAmount) {
      if (this.clientCheck(level, "removeFluidAmountFromPos")) {
         return 0;
      } else {
         FlowingFluid flowing = this.fluidCheck(fluid, "removeFluidAmountFromPos");
         return flowing == null ? 0 : FFFluidUtils.collectConnectedFluidAmountAndRemove(level, startPos, minAmount, maxAmount, flowing);
      }
   }

   @Override
   public int placeFluidAmountFromPos(
      @NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull Fluid fluid, int amount, boolean canSpreadUp, boolean canSpreadDown
   ) {
      if (this.clientCheck(level, "placeFluidAmountFromPos")) {
         return amount;
      } else {
         FlowingFluid flowing = this.fluidCheck(fluid, "placeFluidAmountFromPos");
         return flowing == null
            ? 0
            : FFFluidUtils.addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(level, pos, flowing, amount, canSpreadUp, canSpreadDown);
      }
   }

   @Override
   public void registerBlockTagThatWontDisplaceFluid(@NotNull Fluid fluid, @NotNull TagKey<Block> tag) {
      FlowingFluids.nonDisplacerTags.computeIfAbsent(fluid, a -> new ArrayList<>()).add(tag);
   }

   @Override
   public void registerBlockThatWontDisplaceFluid(@NotNull Fluid fluid, @NotNull Block block) {
      FlowingFluids.nonDisplacers.computeIfAbsent(fluid, a -> new ArrayList<>()).add(block);
   }

   @Override
   public void registerBlockTagStringIdThatWontDisplaceFluid(@NotNull Fluid fluid, @NotNull String tag) {
      FlowingFluids.nonDisplacerTagIds.computeIfAbsent(fluid, a -> new ArrayList<>()).add(tag);
   }

   @Override
   public void registerBlockStringIdThatWontDisplaceFluid(@NotNull Fluid fluid, @NotNull String block) {
      FlowingFluids.nonDisplacerIds.computeIfAbsent(fluid, a -> new ArrayList<>()).add(block);
   }

   @Override
   public void registerBiomeTagThatHasInfiniteWaterRefilling(@NotNull TagKey<Biome> tag) {
      FlowingFluids.infiniteBiomeTags.add(tag);
   }

   @Override
   public void registerBiomeThatHasInfiniteWaterRefilling(@NotNull ResourceKey<Biome> biome) {
      FlowingFluids.infiniteBiomes.add(biome);
   }

   @Override
   public boolean doesBiomeInfiniteWaterRefill(@NotNull Holder<Biome> biome) {
      return FFFluidUtils.matchInfiniteBiomes(biome);
   }

   @Override
   public boolean modifyFluidAmountAtPos(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull FlowingFluidsAPI.FluidAmountModifier fluidAmountModifier) {
      if (this.clientCheck(level, "modifyFluidAmountAtPos")) {
         return false;
      } else {
         FluidState fluid = level.getFluidState(pos);
         if (this.getConfig().isFluidAllowed(fluid)) {
            Fluid type = fluid.getType();
            int amount = fluid.getAmount();
            Pair<Fluid, Integer> data = fluidAmountModifier.getNewAmount(type, amount);
            return data.first() == type && data.second() == amount
               ? false
               : FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, (Fluid)data.first(), Mth.clamp((Integer)data.second(), 0, 8));
         } else {
            return false;
         }
      }
   }

   @Override
   public boolean modifyFluidAmountAtPos(@NotNull LevelAccessor level, @NotNull BlockPos pos, @NotNull Fluid type, int amount) {
      if (this.clientCheck(level, "modifyFluidAmountAtPos")) {
         return false;
      } else {
         return this.getConfig().isFluidAllowed(type) ? FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, type, Mth.clamp(amount, 0, 8)) : false;
      }
   }
}
