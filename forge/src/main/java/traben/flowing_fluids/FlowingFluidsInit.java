// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import traben.flowing_fluids.config.FFCommands;
import traben.flowing_fluids.networking.ForgePacketHandler;

@Mod("flowing_fluids")
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
