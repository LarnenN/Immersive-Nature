// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import it.unimi.dsi.fastutil.Pair;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin({WaterFluid.class})
public abstract class MixinWaterFluid extends FlowingFluid {
   @Unique
   private static Random random = new Random();

   @Shadow
   public abstract int getDropOff(LevelReader var1);

   @Shadow
   public abstract boolean isSame(Fluid var1);

   protected void randomTick(Level level, BlockPos blockPos, FluidState fluidState, RandomSource randomSource) {
      super.randomTick(level, blockPos, fluidState, randomSource);
      if (!level.isClientSide() && !fluidState.isEmpty() && FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(fluidState)) {
         if (!FlowingFluids.config.dontTickAtLocation(blockPos, level)) {
            int amount = fluidState.getAmount();
            if (!FlowingFluids.TWILIGHT_FOREST
               || amount != 1
               || !"twilightforest:twilight_portal".equals(BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockPos).getBlock()).toString())) {
               boolean isWithinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly
                  ? FFFluidUtils.seaLevel(level) == blockPos.getY() || FFFluidUtils.seaLevel(level) - 1 == blockPos.getY()
                  : FFFluidUtils.seaLevel(level) == blockPos.getY() && blockPos.getY() > 0;
               boolean isInfBiome = FFFluidUtils.matchInfiniteBiomes(level.getBiome(blockPos));
               if (amount < 8) {
                  boolean hasSkyLight = level.getBrightness(LightLayer.SKY, blockPos) > 0;
                  if (this.ff$tryBiomeFillOrDrain(level, blockPos, amount, level.random.nextFloat(), isInfBiome, isWithinInfBiomeHeights, hasSkyLight)) {
                     if (FlowingFluids.config.printRandomTicks) {
                        FlowingFluids.info("--- Water was filled by biome at " + blockPos + ". Chance: " + FlowingFluids.config.oceanRiverSwampRefillChance);
                     }

                     return;
                  }

                  if (this.ff$tryRainFill(level, blockPos, level.random.nextFloat(), isInfBiome, isWithinInfBiomeHeights)) {
                     if (FlowingFluids.config.printRandomTicks) {
                        FlowingFluids.info("--- Water was filled by rain at " + blockPos + ". Chance: " + FlowingFluids.config.rainRefillChance);
                     }

                     return;
                  }

                  if (this.ff$tryEvaporateNether(level, blockPos, amount, level.random.nextFloat())) {
                     if (FlowingFluids.config.printRandomTicks) {
                        FlowingFluids.info("--- Water was evaporated via Nether at " + blockPos + ". Chance: " + FlowingFluids.config.evaporationChanceV2);
                     }

                     return;
                  }

                  if (this.ff$tryEvaporate(level, blockPos, amount, level.random.nextFloat(), isInfBiome, isWithinInfBiomeHeights, hasSkyLight)
                     && FlowingFluids.config.printRandomTicks) {
                     FlowingFluids.info("--- Water was evaporated - non Nether at " + blockPos + ". Chance: " + FlowingFluids.config.evaporationChanceV2);
                  }
               } else if (this.ff$tryRainFill(level, blockPos, level.random.nextFloat(), isInfBiome, isWithinInfBiomeHeights)
                  && FlowingFluids.config.printRandomTicks) {
                  FlowingFluids.info("--- Water was filled by rain at " + blockPos + ". Chance: " + FlowingFluids.config.rainRefillChance);
               }
            }
         }
      }
   }

   @Unique
   private boolean ff$tryRainFill(Level level, BlockPos blockPos, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights) {
      if (chance < Math.min(FlowingFluids.config.rainRefillChance, FlowingFluids.config.evaporationChanceV2 / 3.0F)
         && level.isRaining()
         && level.canSeeSky(blockPos.above())
         && (!isInfBiome || !isWithinInfBiomeHeights)
         && !level.getBiome(blockPos).is(BiomeTags.HAS_VILLAGE_DESERT)) {
         int amount = level.isThundering() ? 2 : 1;
         Pair<Integer, Runnable> result = FFFluidUtils.placeConnectedFluidAmountAndPlaceAction(
            level, blockPos, amount, this, 40, FlowingFluids.config.rainFillsWaterHigherV2, false
         );
         if ((Integer)result.first() != amount) {
            ((Runnable)result.second()).run();
            return true;
         }
      }

      return false;
   }

   @Unique
   private boolean ff$tryBiomeFillOrDrain(
      Level level, BlockPos blockPos, int amount, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights, boolean hasSkyLight
   ) {
      if (FFFluidUtils.seaLevel(level) == blockPos.getY()) {
         if (chance < FlowingFluids.config.infiniteWaterBiomeNonConsumeChance
            || chance < FlowingFluids.config.oceanRiverSwampRefillChance
            || level.isRaining() && chance < FlowingFluids.config.rainRefillChance) {
            FluidState below = level.getFluidState(blockPos.below());
            if (below.getAmount() == 8 && below.is(FluidTags.WATER) && hasSkyLight && isInfBiome) {
               level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 2));
               return true;
            }
         }
      } else if (isWithinInfBiomeHeights && amount < 8 && chance < FlowingFluids.config.oceanRiverSwampRefillChance && isInfBiome && hasSkyLight) {
         level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount + 2));
         return true;
      }

      return false;
   }

   @Unique
   private boolean ff$tryEvaporate(
      Level level, BlockPos blockPos, int amount, float chance, boolean isInfBiome, boolean isWithinInfBiomeHeights, boolean hasSkyLight
   ) {
      if (!(chance < FlowingFluids.config.evaporationChanceV2)
         || (amount > this.getDropOff(level) || !level.getFluidState(blockPos.below()).isEmpty())
            && (!hasSkyLight || isInfBiome || FFFluidUtils.seaLevel(level) != blockPos.getY())) {
         return false;
      } else {
         level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
         return true;
      }
   }

   @Unique
   private boolean ff$tryEvaporateNether(Level level, BlockPos blockPos, int amount, float chance) {
      if (chance < FlowingFluids.config.evaporationNetherChance && level.getBiome(blockPos).is(BiomeTags.IS_NETHER)) {
         if (amount == 1) {
            level.setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
         } else {
            level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 3));
         }

         return true;
      } else {
         return false;
      }
   }

   @Inject(
      method = {"getSlopeFindDistance"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ff$modifySlopeDistance(LevelReader level, CallbackInfoReturnable<Integer> cir) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         cir.setReturnValue(Mth.clamp(FlowingFluids.config.waterFlowDistance, 1, 8));
      }
   }

   @Inject(
      method = {"getTickDelay"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void ff$modifyTickDelay(LevelReader level, CallbackInfoReturnable<Integer> cir) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         int value = Mth.clamp(FlowingFluids.config.waterTickDelay, 1, 255);
         if (FlowingFluids.config.tickDelaySpread > 0) {
            value -= random.nextInt(FlowingFluids.config.tickDelaySpread + 1);
         }

         cir.setReturnValue(value);
      }
   }

   @WrapWithCondition(
      method = {"animateTick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V"
      )}
   )
   private boolean ff$cancelWaterSounds(Level instance, double d, double e, double f, SoundEvent arg, SoundSource arg2, float g, float h, boolean bl) {
      return !FlowingFluids.config.enableMod;
   }
}
