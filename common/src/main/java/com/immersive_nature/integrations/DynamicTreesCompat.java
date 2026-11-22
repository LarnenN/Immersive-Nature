package com.immersive_nature.integrations;

import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.util.WeatheringHelper;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DynamicTreesCompat {
   private static final boolean LOADED = PlatHelper.isModLoaded("dynamictrees");
   private static boolean broken = false;
   private static Method getDatabase;
   private static Method getSelector;
   private static Method selectorGet;
   private static Method selectionSpecies;
   private static Method plantSapling;
   private static Object nullSpecies;
   private static Map<?, ?> saplingReplacers;
   private static Method getSeedStack;
   private static Method canBeStripped;
   private static Method getFamily;
   private static Method primitiveLog;
   private static Class<?> leavesClass;
   private static final Map<String, String> TREE_ALIASES = Map.of("magic", "magic_poplar", "origin", "oak", "cypress", "cypress_willow");
   private static Method findSpeciesSloppy;
   private static final Map<String, Object> SPECIES_BY_NAME = new ConcurrentHashMap<>();

   /** Dynamic Trees aerial (mangrove) root blocks. */
   private static final ClassValue<Boolean> AERIAL_ROOTY = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals("com.ferreusveritas.dynamictrees.block.rooty.AerialRootsSoilProperties$RootRootyBlock")) return true;
         }
         return false;
      }
   };

   /**
    * Immersive Nature fix for Dynamic Trees: the aerial root block under a mangrove trunk copies the light
    * blocking of a full mangrove log, even when it is thinner than a full block (radius below 8). No light
    * then reaches its inside, and its sides, which sit inside the block space, are drawn black.
    * A thin root block lets light through like any other non-full block.
    */
   public static boolean isThinAerialRoot(BlockState state) {
      if (!LOADED || !AERIAL_ROOTY.get(state.getBlock().getClass())) return false;
      var radius = state.getBlock().getStateDefinition().getProperty("radius");
      return radius != null && state.getValue(radius) instanceof Integer r && r < 8;
   }

   /** Light saved in old chunks was computed with the old, fully blocking value: fix it on a random tick. */
   public static void onRandomTick(ServerLevel level, BlockPos pos, BlockState state) {
      if (isThinAerialRoot(state) && level.getRawBrightness(pos, 0) == 0) {
         level.getChunkSource().getLightEngine().checkBlock(pos);
      }
   }

   /** Dynamic Trees branch blocks (all families, thin and thick). */
   private static final ClassValue<Boolean> BRANCH = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals("com.ferreusveritas.dynamictrees.block.branch.BranchBlock")) return true;
         }
         return false;
      }
   };
   private static final ResourceLocation CHARRED_BRANCH = new ResourceLocation(ImmersiveNature.MOD_ID, "charred_branch");
   private static final ResourceLocation STRIPPED_CHARRED_BRANCH = new ResourceLocation(ImmersiveNature.MOD_ID, "stripped_charred_branch");

   /** True for the branches of Immersive Nature's charred Dynamic Trees family. */
   public static boolean isCharredBranch(Block block) {
      if (!LOADED) return false;
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.equals(CHARRED_BRANCH) || id.equals(STRIPPED_CHARRED_BRANCH);
   }

   /**
    * What a burning Dynamic Trees branch turns into: a charred branch of the same thickness (radius),
    * or null if the block is no Dynamic Trees branch (or already charred).
    */
   @Nullable
   public static BlockState getCharredBranch(BlockState state) {
      if (!LOADED || !BRANCH.get(state.getBlock().getClass()) || isCharredBranch(state.getBlock())) return null;
      var radius = state.getBlock().getStateDefinition().getProperty("radius");
      if (radius == null || !(state.getValue(radius) instanceof Integer r)) return null;
      Block charred = BuiltInRegistries.BLOCK.get(CHARRED_BRANCH);
      if (charred == Blocks.AIR) return null;
      BlockState result = charred.defaultBlockState();
      var target = charred.getStateDefinition().getProperty("radius");
      if (target instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ip) {
         int max = ip.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(8);
         int min = ip.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(1);
         result = result.setValue(ip, Math.max(min, Math.min(max, r)));
      }
      return result;
   }

   public static boolean isLoaded() {
      return LOADED && !broken;
   }

   private static boolean init() {
      if (getDatabase != null) {
         return true;
      } else {
         try {
            Class<?> dbs = Class.forName("com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases");
            Class<?> db = Class.forName("com.ferreusveritas.dynamictrees.worldgen.BiomeDatabase");
            Class<?> selector = Class.forName("com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors$SpeciesSelector");
            Class<?> selection = Class.forName("com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors$SpeciesSelection");
            Class<?> species = Class.forName("com.ferreusveritas.dynamictrees.tree.species.Species");
            Class<?> registry = Class.forName("com.ferreusveritas.dynamictrees.api.TreeRegistry");
            getSelector = db.getMethod("getSpecies", Holder.class);
            selectorGet = selector.getMethod("getSpecies", BlockPos.class, BlockState.class, RandomSource.class);
            selectionSpecies = selection.getMethod("getSpecies");
            plantSapling = species.getMethod("plantSapling", LevelAccessor.class, BlockPos.class, boolean.class);
            nullSpecies = species.getField("NULL_SPECIES").get(null);
            saplingReplacers = (Map<?, ?>)registry.getField("SAPLING_REPLACERS").get(null);
            getDatabase = dbs.getMethod("getDimensionalOrDefault", ResourceLocation.class);
            return true;
         } catch (Throwable var6) {
            broken = true;
            ImmersiveNature.LOGGER.error("Failed to set up Dynamic Trees compat, saplings will stay vanilla", var6);
            return false;
         }
      }
   }

   public static void onBlockGrown(ServerLevel level, BlockPos pos, BlockState placed) {
      if (isLoaded() && placed.is(BlockTags.SAPLINGS)) {
         if (!BuiltInRegistries.BLOCK.getKey(placed.getBlock()).getNamespace().startsWith("dynamictrees")) {
            if (init()) {
               try {
                  BlockPos groundPos = pos.below();
                  BlockState ground = level.getBlockState(groundPos);
                  Object species = biomeSpecies(level, pos, ground);
                  if (species == null) {
                     species = saplingReplacers.get(placed.getBlock());
                  }

                  if (species == null || species == nullSpecies) {
                     return;
                  }

                  level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
                  boolean ok = (Boolean)plantSapling.invoke(species, level, pos, false);
                  if (!ok && isNonVanillaGrass(ground)) {
                     level.setBlock(groundPos, Blocks.GRASS_BLOCK.defaultBlockState(), 3);
                     ok = (Boolean)plantSapling.invoke(species, level, pos, false);
                     if (!ok) {
                        level.setBlock(groundPos, ground, 3);
                     }
                  }
               } catch (Throwable var7) {
                  broken = true;
                  ImmersiveNature.LOGGER.error("Dynamic Trees compat failed, disabling it", var7);
                  if (level.getBlockState(pos).isAir()) {
                     level.setBlock(pos, placed, 3);
                  }
               }
            }
         }
      }
   }

   public static ItemStack toDynamicSeed(ItemStack stack) {
      if (isLoaded() && !stack.isEmpty() && init()) {
         try {
            Block block = Block.byItem(stack.getItem());
            if (block == Blocks.AIR) {
               return stack;
            }

            Object species = saplingReplacers.get(block);
            if (species == null || species == nullSpecies) {
               return stack;
            }

            if (getSeedStack == null) {
               getSeedStack = species.getClass().getMethod("getSeedStack", int.class);
            }

            if (getSeedStack.invoke(species, stack.getCount()) instanceof ItemStack s && !s.isEmpty()) {
               return s;
            }
         } catch (Throwable var5) {
            broken = true;
            ImmersiveNature.LOGGER.error("Dynamic Trees compat failed, disabling it", var5);
         }

         return stack;
      } else {
         return stack;
      }
   }

   @Nullable
   public static Item getBranchBark(BlockState state, Level level, BlockPos pos, Player player, ItemStack stack) {
      if (!isLoaded()) {
         return null;
      } else {
         Block block = state.getBlock();
         if (!isLoaded()) {
            return null;
         } else {
            try {
               if (canBeStripped == null) {
                  Class<?> branch = Class.forName("com.ferreusveritas.dynamictrees.block.branch.BranchBlock");
                  canBeStripped = branch.getMethod("canBeStripped", BlockState.class, Level.class, BlockPos.class, Player.class, ItemStack.class);
                  getFamily = branch.getMethod("getFamily");
                  primitiveLog = Class.forName("com.ferreusveritas.dynamictrees.tree.family.Family").getMethod("getPrimitiveLog");
               }

               if (!canBeStripped.getDeclaringClass().isInstance(block)) {
                  return null;
               }

               if (!(Boolean)canBeStripped.invoke(block, state, level, pos, player, stack)) {
                  return null;
               }

               Object family = getFamily.invoke(block);
               if (family == null) {
                  return null;
               }

               if (primitiveLog.invoke(family) instanceof Optional<?> o && o.isPresent() && o.get() instanceof Block logBlock) {
                  return WeatheringHelper.getBarkToStrip(logBlock.defaultBlockState());
               }
            } catch (Throwable var11) {
               broken = true;
               ImmersiveNature.LOGGER.error("Dynamic Trees compat failed, disabling it", var11);
            }

            return null;
         }
      }
   }

   public static boolean isDynamicLeaves(Block block) {
      if (!isLoaded()) {
         return false;
      } else {
         try {
            if (leavesClass == null) {
               leavesClass = Class.forName("com.ferreusveritas.dynamictrees.block.leaves.DynamicLeavesBlock");
            }

            return leavesClass.isInstance(block);
         } catch (Throwable var2) {
            return false;
         }
      }
   }

   public static ItemStack seedForTree(String treeName) {
      if (isLoaded() && init()) {
         try {
            Object species = SPECIES_BY_NAME.computeIfAbsent(treeName, n -> {
               n = TREE_ALIASES.getOrDefault(n, n);

               try {
                  if (findSpeciesSloppy == null) {
                     findSpeciesSloppy = Class.forName("com.ferreusveritas.dynamictrees.api.TreeRegistry").getMethod("findSpeciesSloppy", String.class);
                  }

                  Object sp = findSpeciesSloppy.invoke(null, n);
                  return sp == null ? nullSpecies : sp;
               } catch (Throwable var2x) {
                  return nullSpecies;
               }
            });
            if (species == nullSpecies) {
               return ItemStack.EMPTY;
            }

            if (getSeedStack == null) {
               getSeedStack = species.getClass().getMethod("getSeedStack", int.class);
            }

            Object seed = getSeedStack.invoke(species, 1);
            if (seed instanceof ItemStack) {
               return (ItemStack)seed;
            }
         } catch (Throwable var4) {
            broken = true;
            ImmersiveNature.LOGGER.error("Dynamic Trees compat failed, disabling it", var4);
         }

         return ItemStack.EMPTY;
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static Object biomeSpecies(ServerLevel level, BlockPos pos, BlockState ground) throws Exception {
      Holder<Biome> biome = level.getBiome(pos);
      Object db = getDatabase.invoke(null, level.dimension().location());
      if (db == null) {
         return null;
      } else {
         Object selector = getSelector.invoke(db, biome);
         if (selector == null) {
            return null;
         } else {
            Object selection = selectorGet.invoke(selector, pos, ground, level.random);
            if (selection == null) {
               return null;
            } else {
               Object species = selectionSpecies.invoke(selection);
               return species == nullSpecies ? null : species;
            }
         }
      }
   }

   private static boolean isNonVanillaGrass(BlockState ground) {
      Block b = ground.getBlock();
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
      return id.getNamespace().equals(ImmersiveNature.MOD_ID) && id.getPath().contains("grass");
   }
}
