// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.config.auto_perf;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;

public class PerformanceMetric {
   public static final Int2ObjectOpenHashMap<PerformanceMetric> metrics = new Int2ObjectOpenHashMap();
   final int level;
   int intervalsAtLevel = 0;
   private final float[] msptHistory = new float[100];

   public static void resetMetrics() {
      metrics.clear();

      for (PerformanceSpeed value : PerformanceSpeed.values()) {
         metrics.put(value.ordinal(), new PerformanceMetric(value.ordinal()));
      }
   }

   PerformanceMetric(int level) {
      this.level = level;
      Arrays.fill(this.msptHistory, -1.0F);
   }

   void tick(float mspt) {
      this.msptHistory[this.intervalsAtLevel % 100] = mspt;
      this.intervalsAtLevel++;
   }

   float averageMspt() {
      float total = 0.0F;
      int count = 0;

      for (float f : this.msptHistory) {
         if (f >= 0.0F) {
            total += f;
            count++;
         }
      }

      return count == 0 ? 0.0F : total / (float)count;
   }

   static {
      resetMetrics();
   }
}
