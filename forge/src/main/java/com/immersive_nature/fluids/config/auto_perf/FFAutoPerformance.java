// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.config.auto_perf;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.Map;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.config.FFConfig;
import com.immersive_nature.fluids.config.FFConfigPreset;

public abstract class FFAutoPerformance {
   private static final Map<FFConfig.AutoPerformance, FFConfigPreset[]> autoPerformanceMap = new Object2ObjectOpenHashMap();
   private static int currentAutoPerformanceLevel = 0;

   public static void tickUnchanged(float mspt) {
      ((PerformanceMetric)PerformanceMetric.metrics.get(currentAutoPerformanceLevel)).tick(mspt);
   }

   public static int level() {
      return currentAutoPerformanceLevel;
   }

   public static void setLevel(int level, float mspt) {
      currentAutoPerformanceLevel = level;
      tickUnchanged(mspt);
   }

   public static FFConfigPreset current() {
      return getForModeAndLevel(FlowingFluids.config.autoPerformanceMode, currentAutoPerformanceLevel);
   }

   public static FFConfigPreset getForModeAndLevel(FFConfig.AutoPerformance mode, int level) {
      return autoPerformanceMap.get(mode)[level];
   }

   public static void resetAuto() {
      if (FlowingFluids.config.autoPerformanceMode.enabled()) {
         currentAutoPerformanceLevel = 0;
         getForModeAndLevel(FlowingFluids.config.autoPerformanceMode, 0).apply(FlowingFluids.config);
      }
   }

   public static void registerPerformanceTypes() {
      for (final PerformanceQuality quality : PerformanceQuality.values()) {
         ArrayList<FFConfigPreset> list = new ArrayList<>();

         for (final PerformanceSpeed speed : PerformanceSpeed.values()) {
            list.add(
               new FFConfigPreset(
                  "Auto performance: quality "
                     + quality
                     + ", speed "
                     + speed
                     + "."
                     + quality.explain()
                     + speed.explain()
                     + "\nThis setting will only change some config settings and will leave the rest as is.",
                  "~auto_perf_" + quality.name().toLowerCase() + "_" + speed.name().toLowerCase()
               ) {
                  @Override
                  public void apply(FFConfig config) {
                     FFAutoPerformance.resetPerformanceSettings(config);
                     speed.apply(config);
                     quality.apply(config);
                  }
               }
            );
         }

         autoPerformanceMap.put(quality.toConfigType(), list.toArray(new FFConfigPreset[0]));
      }
   }

   private static void resetPerformanceSettings(FFConfig config) {
      FFConfig newConfig = new FFConfig();
      config.waterTickDelay = newConfig.waterTickDelay;
      config.lavaNetherTickDelay = newConfig.lavaNetherTickDelay;
      config.lavaTickDelay = newConfig.lavaTickDelay;
      config.playerBlockDistanceForFlowing = newConfig.playerBlockDistanceForFlowing;
      config.waterFlowDistance = newConfig.waterFlowDistance;
      config.lavaFlowDistance = newConfig.lavaFlowDistance;
      config.lavaNetherFlowDistance = newConfig.lavaNetherFlowDistance;
      config.randomTickLevelingDistance = newConfig.randomTickLevelingDistance;
      config.displacementDepthMultiplier = newConfig.displacementDepthMultiplier;
      config.tickDelaySpread = newConfig.tickDelaySpread;
   }

   public static void report() {
      String str = "\n-----------------------\n Auto Performance Report: Mode (" + FlowingFluids.config.autoPerformanceMode + ")\n-----------------------";
      float total = 0.0F;
      int count = 0;
      ObjectIterator var3 = PerformanceMetric.metrics.values().iterator();

      while (var3.hasNext()) {
         PerformanceMetric metric = (PerformanceMetric)var3.next();
         float average = metric.averageMspt();
         total += average;
         if (average > 0.0F) {
            count++;
         }

         str = str
            + "\n Level "
            + metric.level
            + (metric.level == currentAutoPerformanceLevel ? " (current)" : "")
            + "\n - Average mspt: "
            + average
            + "\n - Intervals: "
            + metric.intervalsAtLevel
            + "\n-----------------------";
      }

      str = str + "\n Overall average mspt across all levels: " + (count == 0 ? 0.0F : total / (float)count);
      FlowingFluids.info(str + "\n-----------------------");
   }
}
