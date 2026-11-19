// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.flowing_fluids.FlowingFluids;

@Mixin({PlayerList.class})
public class MixinPlayerConnect {
   @Inject(
      method = {"placeNewPlayer"},
      at = {@At("TAIL")}
   )
   private void ff$disconnect(Connection netManager, ServerPlayer player, CallbackInfo ci) {
      FlowingFluids.sendConfigToClient(player);
   }
}
