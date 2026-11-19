// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.config;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

public class FFConfig {
   public boolean flowToEdges = true;
   public boolean enableMod = true;
   public boolean enableDisplacement = true;
   public boolean enablePistonPushing = true;
   public float rainRefillChance = 0.3F;
   public float oceanRiverSwampRefillChance = 1.0F;
   public float evaporationChanceV2 = 1.0F;
   public float evaporationNetherChance = 1.0F;
   public boolean printRandomTicks = false;
   public boolean hideFlowingTexture = true;
   public FFConfig.LiquidHeight fullLiquidHeight = FFConfig.LiquidHeight.REGULAR;
   public float farmlandDrainWaterChance = 0.01F;
   public boolean debugWaterLevelColours = false;
   public FFConfig.WaterLogFlowMode waterLogFlowMode = FFConfig.WaterLogFlowMode.IN_FROM_TOP_ELSE_OUT;
   public int waterFlowDistance = 4;
   public int lavaFlowDistance = 2;
   public int lavaNetherFlowDistance = 4;
   public int waterTickDelay = 2;
   public int lavaTickDelay = 15;
   public int lavaNetherTickDelay = 5;
   public int randomTickLevelingDistance = 32;
   public float drinkWaterToBreedAnimalChance = 0.1F;
   public boolean encloseAllFluidOnWorldGen = true;
   public boolean announceWorldGenActions = false;
   public boolean easyPistonPump = true;
   public boolean waterFlowAffectsBoats = false;
   public boolean waterFlowAffectsEntities = true;
   public boolean waterFlowAffectsPlayers = false;
   public boolean waterFlowAffectsItems = true;
   public float infiniteWaterBiomeNonConsumeChance = 0.01F;
   public float infiniteWaterBiomeDrainSurfaceChance = 0.5F;
   public int minWaterLevelForIce = 4;
   public boolean rainFillsWaterHigherV2 = false;
   public int minLavaLevelForObsidian = 6;
   public boolean fastBiomeRefillAtSeaLevelOnly = false;
   public int playerBlockDistanceForFlowing = 0;
   public float concreteDrainsWaterChance = 0.5F;
   public float displacementDepthMultiplier = 1.0F;
   public FFConfig.DisplacementSounds displacementSounds = FFConfig.DisplacementSounds.BOTH;
   public float flowSoundChance = 0.15F;
   public int tickDelaySpread = 0;
   public FFConfig.AutoPerformance autoPerformanceMode = FFConfig.AutoPerformance.HIGH_QUALITY_DEFAULT;
   public int autoPerformanceUpdateRateSeconds = 10;
   public float autoPerformanceMSPTargetMultiplier = 0.9F;
   public boolean autoPerformanceShowMessages = true;
   public FFConfig.CreateWaterWheelMode create_waterWheelMode = FFConfig.CreateWaterWheelMode.REQUIRE_FLOW_OR_RIVER;
   public boolean create_infinitePipes = false;
   public int create_waterWheelFlowMaxTickInterval = 80;
   public ObjectOpenHashSet<String> fluidBlacklist = new ObjectOpenHashSet();
   public int defaultSeaLevelOverride = Integer.MIN_VALUE;
   public Int2IntOpenHashMap dimensionSeaLevelOverrides = new Int2IntOpenHashMap();
   public boolean hideStartMessage = false;
   public static int[] waterLevelColours = new int[]{
      ARGB32.color(255, 0, 0, 255),
      ARGB32.color(255, 0, 128, 255),
      ARGB32.color(255, 0, 255, 192),
      ARGB32.color(255, 0, 255, 0),
      ARGB32.color(255, 255, 255, 0),
      ARGB32.color(255, 255, 128, 0),
      ARGB32.color(255, 255, 0, 0),
      ARGB32.color(255, 255, 255, 255)
   };
   public static final ResourceLocation SERVER_CONFIG_PACKET_ID = FFFluidUtils.res("flowing_fluids:server_config_packet");

   public boolean isFluidAllowed(Fluid fluid) {
      if (fluid == null) {
         return false;
      } else {
         return !this.fluidBlacklist.isEmpty() && fluid != Fluids.EMPTY
            ? !this.fluidBlacklist.contains(BuiltInRegistries.FLUID.getKey(fluid).toString())
            : true;
      }
   }

   public boolean isFluidAllowed(FluidState fluid) {
      return this.isFluidAllowed(fluid.getType());
   }

   public boolean isWaterAllowed() {
      return this.isFluidAllowed(Fluids.WATER);
   }

   public boolean dontTickAtLocation(BlockPos pos, LevelAccessor level) {
      if (this.playerBlockDistanceForFlowing == 0) {
         return false;
      } else {
         int sqrDist = this.playerBlockDistanceForFlowing * this.playerBlockDistanceForFlowing;

         for (Player player2 : level.players()) {
            double i = player2.distanceToSqr((double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
            if (i < (double)sqrDist) {
               return false;
            }
         }

         return true;
      }
   }

   public FFConfig() {
   }

   public FFConfig(FriendlyByteBuf buffer) {
      this.setFromBuff(buffer);
   }

   public void setFromBuff(FriendlyByteBuf buffer) {
      FlowingFluids.info("- Decoding server config packet from server.");
      this.flowToEdges = buffer.readBoolean();
      this.enableMod = buffer.readBoolean();
      this.enableDisplacement = buffer.readBoolean();
      this.enablePistonPushing = buffer.readBoolean();
      this.rainRefillChance = buffer.readFloat();
      this.oceanRiverSwampRefillChance = buffer.readFloat();
      this.evaporationChanceV2 = buffer.readFloat();
      this.evaporationNetherChance = buffer.readFloat();
      this.printRandomTicks = buffer.readBoolean();
      this.hideFlowingTexture = buffer.readBoolean();
      this.fullLiquidHeight = (FFConfig.LiquidHeight)buffer.readEnum(FFConfig.LiquidHeight.class);
      this.farmlandDrainWaterChance = buffer.readFloat();
      this.debugWaterLevelColours = buffer.readBoolean();
      this.waterLogFlowMode = (FFConfig.WaterLogFlowMode)buffer.readEnum(FFConfig.WaterLogFlowMode.class);
      this.waterFlowDistance = buffer.readVarInt();
      this.lavaFlowDistance = buffer.readVarInt();
      this.lavaNetherFlowDistance = buffer.readVarInt();
      this.waterTickDelay = buffer.readVarInt();
      this.lavaTickDelay = buffer.readVarInt();
      this.lavaNetherTickDelay = buffer.readVarInt();
      this.randomTickLevelingDistance = buffer.readVarInt();
      this.drinkWaterToBreedAnimalChance = buffer.readFloat();
      this.encloseAllFluidOnWorldGen = buffer.readBoolean();
      this.announceWorldGenActions = buffer.readBoolean();
      this.easyPistonPump = buffer.readBoolean();
      this.waterFlowAffectsBoats = buffer.readBoolean();
      this.waterFlowAffectsEntities = buffer.readBoolean();
      this.waterFlowAffectsPlayers = buffer.readBoolean();
      this.waterFlowAffectsItems = buffer.readBoolean();
      this.infiniteWaterBiomeNonConsumeChance = buffer.readFloat();
      this.infiniteWaterBiomeDrainSurfaceChance = buffer.readFloat();
      this.minWaterLevelForIce = buffer.readVarInt();
      this.rainFillsWaterHigherV2 = buffer.readBoolean();
      this.minLavaLevelForObsidian = buffer.readVarInt();
      this.fastBiomeRefillAtSeaLevelOnly = buffer.readBoolean();
      this.playerBlockDistanceForFlowing = buffer.readVarInt();
      this.concreteDrainsWaterChance = buffer.readFloat();
      this.displacementDepthMultiplier = buffer.readFloat();
      this.displacementSounds = (FFConfig.DisplacementSounds)buffer.readEnum(FFConfig.DisplacementSounds.class);
      this.flowSoundChance = buffer.readFloat();
      this.tickDelaySpread = buffer.readVarInt();
      this.autoPerformanceMode = (FFConfig.AutoPerformance)buffer.readEnum(FFConfig.AutoPerformance.class);
      this.autoPerformanceUpdateRateSeconds = buffer.readVarInt();
      this.autoPerformanceMSPTargetMultiplier = buffer.readFloat();
      this.autoPerformanceShowMessages = buffer.readBoolean();
      this.create_waterWheelMode = (FFConfig.CreateWaterWheelMode)buffer.readEnum(FFConfig.CreateWaterWheelMode.class);
      this.create_infinitePipes = buffer.readBoolean();
      this.create_waterWheelFlowMaxTickInterval = buffer.readVarInt();
      int size = buffer.readVarInt();
      this.fluidBlacklist = new ObjectOpenHashSet(size);

      for (int i = 0; i < size; i++) {
         this.fluidBlacklist.add(buffer.readUtf());
      }

      this.defaultSeaLevelOverride = buffer.readVarInt();
      this.dimensionSeaLevelOverrides = new Int2IntOpenHashMap();
      int mapSize = buffer.readVarInt();

      for (int i = 0; i < mapSize; i++) {
         int key = buffer.readInt();
         int value = buffer.readInt();
         this.dimensionSeaLevelOverrides.put(key, value);
      }

      this.hideStartMessage = buffer.readBoolean();
   }

   public void encodeToByteBuffer(FriendlyByteBuf buffer) {
      FlowingFluids.info("- Encoding server config packet for client.");
      buffer.writeBoolean(this.flowToEdges);
      buffer.writeBoolean(this.enableMod);
      buffer.writeBoolean(this.enableDisplacement);
      buffer.writeBoolean(this.enablePistonPushing);
      buffer.writeFloat(this.rainRefillChance);
      buffer.writeFloat(this.oceanRiverSwampRefillChance);
      buffer.writeFloat(this.evaporationChanceV2);
      buffer.writeFloat(this.evaporationNetherChance);
      buffer.writeBoolean(this.printRandomTicks);
      buffer.writeBoolean(this.hideFlowingTexture);
      buffer.writeEnum(this.fullLiquidHeight);
      buffer.writeFloat(this.farmlandDrainWaterChance);
      buffer.writeBoolean(this.debugWaterLevelColours);
      buffer.writeEnum(this.waterLogFlowMode);
      buffer.writeVarInt(this.waterFlowDistance);
      buffer.writeVarInt(this.lavaFlowDistance);
      buffer.writeVarInt(this.lavaNetherFlowDistance);
      buffer.writeVarInt(this.waterTickDelay);
      buffer.writeVarInt(this.lavaTickDelay);
      buffer.writeVarInt(this.lavaNetherTickDelay);
      buffer.writeVarInt(this.randomTickLevelingDistance);
      buffer.writeFloat(this.drinkWaterToBreedAnimalChance);
      buffer.writeBoolean(this.encloseAllFluidOnWorldGen);
      buffer.writeBoolean(this.announceWorldGenActions);
      buffer.writeBoolean(this.easyPistonPump);
      buffer.writeBoolean(this.waterFlowAffectsBoats);
      buffer.writeBoolean(this.waterFlowAffectsEntities);
      buffer.writeBoolean(this.waterFlowAffectsPlayers);
      buffer.writeBoolean(this.waterFlowAffectsItems);
      buffer.writeFloat(this.infiniteWaterBiomeNonConsumeChance);
      buffer.writeFloat(this.infiniteWaterBiomeDrainSurfaceChance);
      buffer.writeVarInt(this.minWaterLevelForIce);
      buffer.writeBoolean(this.rainFillsWaterHigherV2);
      buffer.writeVarInt(this.minLavaLevelForObsidian);
      buffer.writeBoolean(this.fastBiomeRefillAtSeaLevelOnly);
      buffer.writeVarInt(this.playerBlockDistanceForFlowing);
      buffer.writeFloat(this.concreteDrainsWaterChance);
      buffer.writeFloat(this.displacementDepthMultiplier);
      buffer.writeEnum(this.displacementSounds);
      buffer.writeFloat(this.flowSoundChance);
      buffer.writeVarInt(this.tickDelaySpread);
      buffer.writeEnum(this.autoPerformanceMode);
      buffer.writeVarInt(this.autoPerformanceUpdateRateSeconds);
      buffer.writeFloat(this.autoPerformanceMSPTargetMultiplier);
      buffer.writeBoolean(this.autoPerformanceShowMessages);
      buffer.writeEnum(this.create_waterWheelMode);
      buffer.writeBoolean(this.create_infinitePipes);
      buffer.writeVarInt(this.create_waterWheelFlowMaxTickInterval);
      buffer.writeVarInt(this.fluidBlacklist.size());
      ObjectIterator var2 = this.fluidBlacklist.iterator();

      while (var2.hasNext()) {
         String fluid = (String)var2.next();
         buffer.writeUtf(fluid);
      }

      buffer.writeVarInt(this.defaultSeaLevelOverride);
      buffer.writeVarInt(this.dimensionSeaLevelOverrides.size());
      var2 = this.dimensionSeaLevelOverrides.int2IntEntrySet().iterator();

      while (var2.hasNext()) {
         Entry entry = (Entry)var2.next();
         buffer.writeInt(entry.getIntKey());
         buffer.writeInt(entry.getIntValue());
      }

      buffer.writeBoolean(this.hideStartMessage);
   }

   public static enum AutoPerformance {
      OFF,
      HIGH_QUALITY_DEFAULT,
      MEDIUM_QUALITY,
      LOW_QUALITY;

      public boolean enabled() {
         return this != OFF;
      }

      public String pretty() {
         return this == OFF ? "" : " with " + this.pretty2();
      }

      public String pretty2() {
         return switch (this) {
            case OFF -> "";
            case HIGH_QUALITY_DEFAULT -> "High Quality (Default)";
            case MEDIUM_QUALITY -> "Medium Quality";
            case LOW_QUALITY -> "Low Quality";
         };
      }
   }

   public static enum CreateWaterWheelMode {
      ALWAYS,
      REQUIRE_FLOW,
      REQUIRE_FLOW_OR_RIVER,
      REQUIRE_FLUID,
      REQUIRE_FULL_FLUID,
      RIVER_ONLY,
      REQUIRE_FLOW_OR_RIVER_OPPOSITE,
      REQUIRE_FLUID_OPPOSITE,
      REQUIRE_FULL_FLUID_OPPOSITE,
      ALWAYS_OPPOSITE,
      RIVER_ONLY_OPPOSITE;

      public boolean isCounterSpin() {
         return this.ordinal() > 5;
      }

      public boolean isRiver() {
         return this == REQUIRE_FLOW_OR_RIVER || this == REQUIRE_FLOW_OR_RIVER_OPPOSITE || this == RIVER_ONLY || this == RIVER_ONLY_OPPOSITE;
      }

      public boolean isRiverOnly() {
         return this == RIVER_ONLY || this == RIVER_ONLY_OPPOSITE;
      }

      public boolean needsFullFluid() {
         return this == REQUIRE_FULL_FLUID || this == REQUIRE_FULL_FLUID_OPPOSITE;
      }

      public boolean always() {
         return this == ALWAYS || this == ALWAYS_OPPOSITE;
      }

      public boolean needsFlow() {
         return this == REQUIRE_FLOW || this == REQUIRE_FLOW_OR_RIVER || this == REQUIRE_FLOW_OR_RIVER_OPPOSITE;
      }
   }

   public static enum DisplacementSounds {
      NONE,
      PISTON_ONLY,
      BLOCKS_ONLY,
      BOTH;

      public boolean allow(boolean isPiston) {
         return switch (this) {
            case NONE -> false;
            case BOTH -> true;
            case PISTON_ONLY -> isPiston;
            case BLOCKS_ONLY -> !isPiston;
         };
      }
   }

   public static enum LiquidHeight {
      REGULAR,
      REGULAR_LOWER_BOUND,
      BLOCK,
      BLOCK_LOWER_BOUND,
      SLAB,
      CARPET;
   }

   public static enum WaterLogFlowMode {
      ONLY_IN,
      ONLY_OUT,
      IN_FROM_TOP_ELSE_OUT,
      OUT_DOWN_ELSE_IN,
      IGNORE;

      public boolean blocksFlowOutDown() {
         return this == ONLY_IN || this == IGNORE;
      }

      public boolean blocksFlowIn(boolean down) {
         return down ? this == ONLY_OUT || this == IGNORE : this == ONLY_OUT || this == IN_FROM_TOP_ELSE_OUT || this == IGNORE;
      }

      public boolean blocksFlowOutSides() {
         return this == ONLY_IN || this == OUT_DOWN_ELSE_IN || this == IGNORE;
      }
   }
}
