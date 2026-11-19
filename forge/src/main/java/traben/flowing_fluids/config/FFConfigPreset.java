// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.config;

import java.util.ArrayList;
import java.util.List;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.auto_perf.FFAutoPerformance;

public abstract class FFConfigPreset {
   static final String XPLN_AFFECTS_ALL = "\nThis setting will affect all config settings.";
   static final String XPLN_AFFECTS_SOME = "\nThis setting will only change some config settings and will leave the rest as is.";
   public static final List<FFConfigPreset> registered = new ArrayList<>();
   public final String explanation;
   public final String commandName;

   protected FFConfigPreset(String explanation, String commandName) {
      this.explanation = explanation;
      this.commandName = commandName;
      registered.add(this);
   }

   public abstract void apply(FFConfig var1);

   static {
      FFAutoPerformance.registerPerformanceTypes();
      new FFConfigPreset(
         "The default configuration for Flowing Fluids.\nThis is equivalent to resetting the config.\nThis can also be considered the highest quality setting with no slowed down settings for performance.\nThis setting will affect all config settings.",
         "default"
      ) {
         @Override
         public void apply(FFConfig config) {
            FlowingFluids.config = new FFConfig();
         }
      };
      new FFConfigPreset(
         "The No Infinite Biomes configuration for Flowing Fluids.\nThis will disable all infinite water settings for biomes such as oceans, rivers and swamps.\nBut this will keep rain refilling settings.\nThis setting will only change some config settings and will leave the rest as is.",
         "infinite_biomes_off"
      ) {
         @Override
         public void apply(FFConfig config) {
            config.oceanRiverSwampRefillChance = 0.0F;
            config.infiniteWaterBiomeNonConsumeChance = 0.0F;
            config.infiniteWaterBiomeDrainSurfaceChance = 0.0F;
            config.fastBiomeRefillAtSeaLevelOnly = false;
            FlowingFluids.config = config;
         }
      };
      new FFConfigPreset(
         "The Infinite Biomes configuration for Flowing Fluids.\nThis will re-enable all infinite water settings for biomes such as oceans, rivers and swamps.\nThis setting will only change some config settings and will leave the rest as is.",
         "infinite_biomes_on"
      ) {
         @Override
         public void apply(FFConfig config) {
            FFConfig newConfig = new FFConfig();
            config.oceanRiverSwampRefillChance = newConfig.oceanRiverSwampRefillChance;
            config.infiniteWaterBiomeNonConsumeChance = newConfig.infiniteWaterBiomeNonConsumeChance;
            config.infiniteWaterBiomeDrainSurfaceChance = newConfig.infiniteWaterBiomeDrainSurfaceChance;
            config.fastBiomeRefillAtSeaLevelOnly = newConfig.fastBiomeRefillAtSeaLevelOnly;
            FlowingFluids.config = config;
         }
      };
      new FFConfigPreset(
         "The Rain Can Flood configuration for Flowing Fluids.\nThis will enable rain to fill water blocks higher than just source blocks, allowing rain to be capable of flooding areas.\nThis setting will only change some config settings and will leave the rest as is.",
         "rain_can_flood"
      ) {
         @Override
         public void apply(FFConfig config) {
            config.rainFillsWaterHigherV2 = true;
            FlowingFluids.config = config;
         }
      };
      new FFConfigPreset(
         "The No Gameplay Consumers configuration for Flowing Fluids.\nThis will disable all gameplay water consumption mechanics such as farmland drying, animals drinking to breed, and concrete drying.\nThis setting will only change some config settings and will leave the rest as is.",
         "gameplay_consumers_off"
      ) {
         @Override
         public void apply(FFConfig config) {
            config.farmlandDrainWaterChance = 0.0F;
            config.drinkWaterToBreedAnimalChance = 0.0F;
            config.concreteDrainsWaterChance = 0.0F;
            FlowingFluids.config = config;
         }
      };
      new FFConfigPreset(
         "The Gameplay Consumers configuration for Flowing Fluids.\nThis will enable all gameplay water consumption mechanics such as farmland drying, animals drinking to breed, and concrete drying.\nThis setting will only change some config settings and will leave the rest as is.",
         "gameplay_consumers_on"
      ) {
         @Override
         public void apply(FFConfig config) {
            FFConfig newConfig = new FFConfig();
            config.farmlandDrainWaterChance = newConfig.farmlandDrainWaterChance;
            config.drinkWaterToBreedAnimalChance = newConfig.drinkWaterToBreedAnimalChance;
            config.concreteDrainsWaterChance = newConfig.concreteDrainsWaterChance;
            FlowingFluids.config = config;
         }
      };
   }
}
