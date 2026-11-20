// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.immersive_nature.fluids.config.FFConfig;
import com.immersive_nature.fluids.networking.ForgePacketHandler;

public class FlowingFluids {
   public static final String MOD_ID = "immersive_nature";
   public static final Logger LOG = LoggerFactory.getLogger("immersive_nature/fluids");
   public static boolean isManeuveringFluids = false;
   public static boolean pistonTick = false;
   @Nullable
   public static Direction lastPistonMoveDirection = null;
   public static long debug_killFluidUpdatesUntilTime = 0L;
   public static int waterPluggedThisSession = 0;
   public static boolean CREATE = false;
   public static boolean TWILIGHT_FOREST = false;
   public static Map<Fluid, List<TagKey<Block>>> nonDisplacerTags = new Object2ObjectOpenHashMap();
   public static Map<Fluid, List<Block>> nonDisplacers = new Object2ObjectOpenHashMap();
   public static Map<Fluid, List<String>> nonDisplacerTagIds = new Object2ObjectOpenHashMap();
   public static Map<Fluid, List<String>> nonDisplacerIds = new Object2ObjectOpenHashMap();
   public static Set<TagKey<Biome>> infiniteBiomeTags = new HashSet<>();
   public static Set<ResourceKey<Biome>> infiniteBiomes = new HashSet<>();
   public static FFConfig config = new FFConfig();
   private static boolean showFirstMessage = true;

   public static void info(String str) {
      LOG.info("[Flowing Fluids] {}", str);
   }

   public static void warn(String str) {
      LOG.warn("[Flowing Fluids] {}", str);
   }

   public static void error(String str) {
      LOG.error("[Flowing Fluids] {}", str);
   }

   public static void start() {
      info("initialising.");
      infiniteBiomeTags.add(BiomeTags.IS_OCEAN);
      infiniteBiomeTags.add(BiomeTags.IS_RIVER);
      infiniteBiomeTags.add(BiomeTags.IS_BEACH);
      infiniteBiomes.add(Biomes.SWAMP);
      infiniteBiomes.add(Biomes.MANGROVE_SWAMP);
      ArrayList<Block> waterBlockList = new ArrayList<>();
      ArrayList<Block> lavaBlockList = new ArrayList<>();
      ArrayList<TagKey<Block>> waterBlockTagList = new ArrayList<>();
      ArrayList<String> waterBlockListIds = new ArrayList<>();
      waterBlockTagList.add(BlockTags.ICE);
      waterBlockList.add(Blocks.SPONGE);
      lavaBlockList.add(Blocks.OBSIDIAN);
      waterBlockListIds.add("twilightforest:twilight_portal");
      nonDisplacerTags.put(Fluids.WATER, waterBlockTagList);
      nonDisplacers.put(Fluids.WATER, waterBlockList);
      nonDisplacers.put(Fluids.LAVA, lavaBlockList);
      nonDisplacerIds.put(Fluids.WATER, waterBlockListIds);
      loadConfig();
      CREATE = isThisModLoaded("create");
      TWILIGHT_FOREST = isThisModLoaded("twilightforest");
   }

   public static void sendConfigToClient(ServerPlayer player) {
      if (showFirstMessage && !config.hideStartMessage) {
         player.sendSystemMessage(
            Component.literal("[Flowing Fluids mod loaded]\n")
               .withStyle(ChatFormatting.GOLD)
               .append(Component.literal("- Automatic performance handling is ").withStyle(ChatFormatting.WHITE))
               .append(
                  Component.literal(config.autoPerformanceMode.enabled() ? "enabled" : "disabled")
                     .withStyle(config.autoPerformanceMode.enabled() ? ChatFormatting.GREEN : ChatFormatting.RED)
               )
               .append(
                  Component.literal(
                        config.autoPerformanceMode.pretty() + ".\n- This will overwrite & handle performance settings automatically for you.\n- See "
                     )
                     .withStyle(ChatFormatting.WHITE)
               )
               .append(Component.literal("/flowing_fluids performance_and_presets").withStyle(ChatFormatting.GRAY))
               .append(
                  Component.literal(" to change this or access other ready-made gameplay and performance settings presets.").withStyle(ChatFormatting.WHITE)
               )
         );
         showFirstMessage = false;
      }

      ForgePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ForgePacketHandler.FFConfigPacket());
      info("- Sending server config to [" + player.getName().getString() + "]");
   }

   public static Path getConfigDirectory() {
      return FMLPaths.GAMEDIR.get().resolve(FMLPaths.CONFIGDIR.get());
   }

   public static boolean isThisModLoaded(String modId) {
      try {
         ModList list = ModList.get();
         if (list != null) {
            return list.isLoaded(modId);
         }

         LoadingModList list2 = LoadingModList.get();
         if (list2 != null) {
            return list2.getModFileById(modId) != null;
         }

         error("Forge ModList checking failed!");
      } catch (Exception var3) {
         error("Forge ModList checking failed, via exception!");
      }

      return false;
   }

   public static List<String> modsLoaded() {
      try {
         ModList list = ModList.get();
         if (list != null) {
            return list.getMods().stream().<String>map(IModInfo::getModId).toList();
         }

         LoadingModList list2 = LoadingModList.get();
         if (list2 != null) {
            return list2.getModFiles().stream().flatMap(it -> it.getMods().stream()).<String>map(IModInfo::getModId).toList();
         }

         error("Forge ModList checking failed!");
      } catch (Exception var2) {
         error("Forge ModList checking failed, via exception!");
      }

      return List.of();
   }

   public static boolean isForge() {
      return !isFabric();
   }

   public static boolean isFabric() {
      return false;
   }

   // the settings live in the "fluids" section of the Immersive Nature config
   public static void loadConfig() {
      com.immersive_nature.forge.PlatformConfigs.loadFluids();
   }

   public static void saveConfig() {
      com.immersive_nature.forge.PlatformConfigs.saveFluids();
   }
}
