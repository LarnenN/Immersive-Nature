// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfigPreset;
import traben.flowing_fluids.config.auto_perf.FFAutoPerformance;
import traben.flowing_fluids.config.auto_perf.PerformanceSpeed;

@Mixin({MinecraftServer.class})
public abstract class MixinServer_AutoPerformance {
   @Unique
   private long lastSysTimeAdjusted = 0L;

   @Shadow
   public abstract float getAverageTickTime();

   @Inject(
      method = {"stopServer"},
      at = {@At("HEAD")}
   )
   private void ff$resetAutoPerformance(CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.autoPerformanceMode.enabled()) {
         FFAutoPerformance.resetAuto();
         this.lastSysTimeAdjusted = 0L;
      }
   }

   @Inject(
      method = {"tickServer"},
      at = {@At("TAIL")}
   )
   private void ff$auto_adjust(BooleanSupplier booleanSupplier, CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.autoPerformanceMode.enabled()) {
         long time = System.currentTimeMillis();
         if (time - this.lastSysTimeAdjusted >= (long)FlowingFluids.config.autoPerformanceUpdateRateSeconds * 1000L) {
            if (this.lastSysTimeAdjusted == 0L) {
               FFAutoPerformance.resetAuto();
            }

            this.lastSysTimeAdjusted = time;
            float msptGoal = 50.0F * FlowingFluids.config.autoPerformanceMSPTargetMultiplier;
            float mspt = this.getAverageTickTime();
            int initialLevel = FFAutoPerformance.level();
            int level;
            if ((double)mspt <= (double)msptGoal * 0.5) {
               if (initialLevel <= 0) {
                  FFAutoPerformance.tickUnchanged(mspt);
                  return;
               }

               level = initialLevel - 1;
               if ((double)mspt <= (double)msptGoal * 0.25) {
                  level--;
               }

               if ((double)mspt <= (double)msptGoal * 0.125) {
                  level--;
               }

               if ((double)mspt <= (double)msptGoal * 0.0625) {
                  level--;
               }

               if (level < 0) {
                  level = 0;
               }
            } else {
               if (!(mspt >= msptGoal)) {
                  FFAutoPerformance.tickUnchanged(mspt);
                  return;
               }

               if (initialLevel >= PerformanceSpeed.max()) {
                  FFAutoPerformance.tickUnchanged(mspt);
                  return;
               }

               level = initialLevel + 1;

               for (int multiplier = 2; multiplier <= 6; multiplier++) {
                  if (mspt >= msptGoal * (float)multiplier) {
                     level++;
                  }
               }

               if (level > PerformanceSpeed.max()) {
                  level = PerformanceSpeed.max();
               }
            }

            FFAutoPerformance.setLevel(level, mspt);
            FFConfigPreset preset = FFAutoPerformance.current();
            preset.apply(FlowingFluids.config);
            if (FlowingFluids.config.autoPerformanceShowMessages) {
               boolean isMaxed = level >= PerformanceSpeed.max();
               this.logSeverity(
                  isMaxed,
                  "[Flowing Fluids] Auto Performance Handling: {} mode's fluid tick rate {} to {} from {} | mspt {} / {} | {} (You can disable this logging in the settings)",
                  FlowingFluids.config.autoPerformanceMode.pretty2(),
                  level < initialLevel ? "increased" : "decreased",
                  preset.commandName.toLowerCase(),
                  FFAutoPerformance.getForModeAndLevel(FlowingFluids.config.autoPerformanceMode, initialLevel).commandName.toLowerCase(),
                  mspt,
                  50,
                  isMaxed
                     ? "Auto Performance cannot set the tick rate any slower, consider lowering your auto performance quality mode if this is still not enough"
                     : (
                        level <= 0
                           ? "Auto Performance is running at maximum tick rate, Nice :)"
                           : "Auto Performance still has wiggle room to speed up or slow down the tick rate of fluids"
                     )
               );
            }
         }
      }
   }

   @Unique
   private void logSeverity(boolean warn, String format, Object... arguments) {
      if (warn) {
         FlowingFluids.LOG.warn(format, arguments);
      } else {
         FlowingFluids.LOG.info(format, arguments);
      }
   }
}
