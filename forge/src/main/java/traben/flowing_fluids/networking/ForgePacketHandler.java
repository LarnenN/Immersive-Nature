// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.networking;

import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.simple.SimpleChannel;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;
import traben.flowing_fluids.config.FFConfig;

public class ForgePacketHandler {
   private static final int PROTOCOL_VERSION = 1;
   public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
      FFFluidUtils.res("flowing_fluids", "channel"), () -> String.valueOf(1), String.valueOf(1)::equals, String.valueOf(1)::equals
   );

   public static void init() {
      INSTANCE.registerMessage(
         0,
         ForgePacketHandler.FFConfigPacket.class,
         ForgePacketHandler.FFConfigPacket::encoder,
         ForgePacketHandler.FFConfigPacket::decoder,
         ForgePacketHandler.FFConfigPacket::messageConsumer
      );
   }

   public static class FFConfigPacket extends FFConfig {
      private transient boolean is_valid;

      public FFConfigPacket() {
      }

      public FFConfigPacket(FriendlyByteBuf buffer) {
         super(buffer);
      }

      public static ForgePacketHandler.FFConfigPacket decoder(FriendlyByteBuf buffer) {
         ForgePacketHandler.FFConfigPacket packet;
         if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
               FlowingFluids.info("- Server Config packet received");
               packet = new ForgePacketHandler.FFConfigPacket(buffer);
               packet.is_valid = true;
            } catch (Exception var3) {
               FlowingFluids.error("- Server Config packet decoding failed because:\n" + var3);
               var3.printStackTrace();
               packet = new ForgePacketHandler.FFConfigPacket();
               packet.is_valid = false;
            }
         } else {
            packet = new ForgePacketHandler.FFConfigPacket(buffer);
            packet.is_valid = false;
         }

         return packet;
      }

      public void messageConsumer(Supplier<Context> ctx) {
         if (this.is_valid) {
            FlowingFluids.config = this;
            FlowingFluids.info("- Server Config data received and synced");
            ctx.get().setPacketHandled(true);
         } else {
            FlowingFluids.error("- Server Config data received and failed to sync");
            throw new RuntimeException("[Flowing Fluids] - Server Config data received and failed to sync");
         }
      }

      public void encoder(FriendlyByteBuf buffer) {
         FlowingFluids.config.encodeToByteBuffer(buffer);
      }
   }
}
