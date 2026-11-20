// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({Minecraft.class})
public class MixinDisconnect {
   @Inject(
      method = {"clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V"},
      at = {@At("TAIL")}
   )
   private void ff$disconnect(CallbackInfo ci) {
      FlowingFluids.loadConfig();
   }
}
