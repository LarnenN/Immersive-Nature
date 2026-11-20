// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import com.immersive_nature.fluids.config.FFCommands;
import com.immersive_nature.fluids.networking.ForgePacketHandler;

/** Fluids setup, called from ImmersiveNatureForge. */
public class FlowingFluidsInit {
   public FlowingFluidsInit() {
      ForgePacketHandler.init();
      FlowingFluids.start();
      MinecraftForge.EVENT_BUS.register(FlowingFluidsInit.class);
   }

   @SubscribeEvent
   public static void onRegisterCommandEvent(RegisterCommandsEvent event) {
      FlowingFluids.info("commands registered");
      FFCommands.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
   }
}
