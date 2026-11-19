// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.config.auto_perf;

import traben.flowing_fluids.config.FFConfig;

public enum PerformanceQuality {
   DEFAULT,
   MEDIUM,
   LOW;

   public FFConfig.AutoPerformance toConfigType() {
      return switch (this) {
         case DEFAULT -> FFConfig.AutoPerformance.HIGH_QUALITY_DEFAULT;
         case MEDIUM -> FFConfig.AutoPerformance.MEDIUM_QUALITY;
         case LOW -> FFConfig.AutoPerformance.LOW_QUALITY;
      };
   }

   public String explain() {
      return switch (this) {
         case DEFAULT -> "\nThis will automatically improve performance over the default settings by only lowering fluid ticking speeds while maintaining the exact same fluid behaviour settings.\nConsider this a high/normal quality mode for fluids.";
         case MEDIUM -> "\nThis will automatically improve performance over the default settings by reducing the fluid behaviour settings to be less impactful than default.\nConsider this a medium quality mode for fluids.";
         case LOW -> "\nThis will automatically improve performance over the default settings by massively reducing the fluid behaviour settings to be as least impactful as reasonable.\nConsider this a low quality mode for fluids.";
      };
   }

   public void apply(FFConfig config) {
      switch (this) {
         case DEFAULT:
         default:
            break;
         case MEDIUM:
            config.playerBlockDistanceForFlowing = 160;
            config.waterFlowDistance = 3;
            config.lavaFlowDistance = 2;
            config.lavaNetherFlowDistance = 3;
            config.randomTickLevelingDistance = 24;
            config.displacementDepthMultiplier = 0.6666667F;
            break;
         case LOW:
            config.playerBlockDistanceForFlowing = 80;
            config.waterFlowDistance = 2;
            config.lavaFlowDistance = 1;
            config.lavaNetherFlowDistance = 2;
            config.randomTickLevelingDistance = 16;
            config.displacementDepthMultiplier = 0.33333334F;
      }
   }
}
