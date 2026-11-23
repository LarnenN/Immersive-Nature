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
   private static final ResourceLocation SMOLDERING_BRANCH = new ResourceLocation(ImmersiveNature.MOD_ID, "smoldering_charred_branch");
   private static final ResourceLocation STRIPPED_SMOLDERING_BRANCH = new ResourceLocation(ImmersiveNature.MOD_ID, "stripped_smoldering_charred_branch");

   /** True for the branches of Immersive Nature's charred Dynamic Trees family. */
   private static final ResourceLocation CHARRED_FAMILY = new ResourceLocation(ImmersiveNature.MOD_ID, "charred");
   private static final ResourceLocation SMOLDERING_FAMILY = new ResourceLocation(ImmersiveNature.MOD_ID, "smoldering_charred");

   private static boolean isCharredFamily(Object name) {
      return CHARRED_FAMILY.equals(name) || SMOLDERING_FAMILY.equals(name);
   }

   private static Method call(Object target, String name, Class<?>... types) throws ReflectiveOperationException {
      String key = name + types.length;
      Method m = METHODS_BY_NAME.computeIfAbsent(target.getClass().getName() + "#" + key, k -> {
         try {
            return target.getClass().getMethod(name, types);
         } catch (NoSuchMethodException e) {
            return null;
         }
      });
      return m;
   }

   private static final Map<String, Method> METHODS_BY_NAME = new ConcurrentHashMap<>();

   /**
    * Root overlay color of a Dynamic Trees rooty block whose trunk above was charred. DT takes the root color
    * from the family of the branch above, so charred trunks turned the roots black; this keeps the color of the
    * tree that grew there (the species saved in the rooty block). Null when the trunk above is not charred.
    */
   @Nullable
   public static Integer charredRootColor(Object rooty, BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
      if (!LOADED || broken) return null;
      try {
         Method getFam = call(rooty, "getFamily", BlockState.class, net.minecraft.world.level.BlockGetter.class, BlockPos.class);
         Object family = getFam == null ? null : getFam.invoke(rooty, state, level, pos);
         if (family == null || !isCharredFamily(registryName(family))) return null;
         Object be = level.getBlockEntity(pos);
         if (be != null) {
            Method getSpecies = call(be, "getSpecies");
            Object species = getSpecies == null ? null : getSpecies.invoke(be);
            Method speciesFamily = species == null ? null : call(species, "getFamily");
            Object original = speciesFamily == null ? null : speciesFamily.invoke(species);
            Method valid = original == null ? null : call(original, "isValid");
            if (original != null && valid != null && (Boolean) valid.invoke(original) && !isCharredFamily(registryName(original))) {
               Method fromBark = call(rooty, "getColorFromBark");
               Method rootColor = call(original, "getRootColor", BlockState.class, boolean.class);
               if (fromBark != null && rootColor != null) return (Integer) rootColor.invoke(original, state, fromBark.invoke(rooty));
            }
         }
         return -1; // tree unknown: plain, untinted roots instead of black ones
      } catch (Throwable t) {
         return null;
      }
   }

   /**
    * A Dynamic Trees branch burned by fire: with the chance for its size (config charring.dynamic_trees) it becomes a
    * charred branch of the same thickness. Returns that state, or null if it burns away. Fire burns a block in two ways
    * (removes it, or replaces it with fire), so this is used for both.
    */
   @Nullable
   public static BlockState tryCharBranch(ServerLevel level, BlockPos pos, BlockState state) {
      BlockState charred = getCharredBranch(state);
      if (charred == null || !com.immersive_nature.configs.CommonConfigs.DT_BRANCH_CHARRING.get()) return null;
      int radius = getRadius(state);
      double chance = radius <= com.immersive_nature.configs.CommonConfigs.DT_THIN_MAX_RADIUS.get() ? com.immersive_nature.configs.CommonConfigs.DT_THIN_CHAR_CHANCE.get()
            : radius <= com.immersive_nature.configs.CommonConfigs.DT_MEDIUM_MAX_RADIUS.get() ? com.immersive_nature.configs.CommonConfigs.DT_MEDIUM_CHAR_CHANCE.get()
            : com.immersive_nature.configs.CommonConfigs.DT_THICK_CHAR_CHANCE.get();
      if (chance <= 0 || level.random.nextFloat() >= chance) return null;
      rememberTreeUnder(level, pos);
      if (level.random.nextFloat() < com.immersive_nature.configs.CommonConfigs.DT_SMOLDERING_CHANCE.get()) {
         BlockState lit = withSmoldering(charred, true);
         if (lit != null) return lit;
      }
      return charred;
   }

   private static final ClassValue<Boolean> ROOTY = new ClassValue<>() {
      @Override
      protected Boolean computeValue(Class<?> type) {
         for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if (c.getName().equals("com.ferreusveritas.dynamictrees.block.rooty.RootyBlock")) return true;
         }
         return false;
      }
   };

   /**
    * Called right before the trunk piece at branchPos chars. Dynamic Trees only keeps the species in a rooty block
    * when it differs from the family's usual one ("is_variant"); otherwise it reads the tree from the trunk above,
    * which is about to become charred. So the rooty block under it is switched to a variant that stores the species,
    * and its roots keep their color (see {@link #charredRootColor}).
    */
   public static void rememberTreeUnder(ServerLevel level, BlockPos branchPos) {
      if (!LOADED || broken) return;
      BlockPos pos = branchPos.below();
      BlockState rooty = level.getBlockState(pos);
      Block block = rooty.getBlock();
      if (!ROOTY.get(block.getClass())) return;
      try {
         Method getSpecies = call(block, "getSpecies", BlockState.class, LevelAccessor.class, BlockPos.class);
         Method setSpecies = call(block, "setSpecies", Level.class, BlockPos.class, getSpecies == null ? Object.class : getSpecies.getReturnType());
         if (getSpecies == null || setSpecies == null) return;
         Object species = getSpecies.invoke(block, rooty, level, pos);
         Method valid = species == null ? null : call(species, "isValid");
         if (valid == null || !(Boolean) valid.invoke(species)) return;
         var variant = block.getStateDefinition().getProperty("is_variant");
         if (variant instanceof net.minecraft.world.level.block.state.properties.BooleanProperty v && !rooty.getValue(v)) {
            rooty = rooty.setValue(v, true);
            level.setBlock(pos, rooty, 2 | 16);
         }
         setSpecies.invoke(block, level, pos, species);
         var be = level.getBlockEntity(pos);
         if (be != null) be.setChanged();
         level.sendBlockUpdated(pos, rooty, rooty, 2);
      } catch (Throwable t) {
         // leave the roots as they are
      }
   }

   private static Object registryName(Object entry) throws ReflectiveOperationException {
      Method m = call(entry, "getRegistryName");
      return m == null ? null : m.invoke(entry);
   }

   public static boolean isCharredBranch(Block block) {
      if (!LOADED) return false;
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.equals(CHARRED_BRANCH) || id.equals(STRIPPED_CHARRED_BRANCH) || id.equals(SMOLDERING_BRANCH) || id.equals(STRIPPED_SMOLDERING_BRANCH);
   }

   /** True for the glowing (smoldering) charred branches. */
   public static boolean isSmolderingBranch(Block block) {
      if (!LOADED) return false;
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
      return id.equals(SMOLDERING_BRANCH) || id.equals(STRIPPED_SMOLDERING_BRANCH);
   }

   /**
    * The same charred branch (same thickness, water, stripped or not), smoldering or put out;
    * null if the block is no charred branch.
    */
   @Nullable
   public static BlockState withSmoldering(BlockState state, boolean lit) {
      if (!isCharredBranch(state.getBlock())) return null;
      ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
      boolean stripped = id.equals(STRIPPED_CHARRED_BRANCH) || id.equals(STRIPPED_SMOLDERING_BRANCH);
      ResourceLocation targetId = lit ? (stripped ? STRIPPED_SMOLDERING_BRANCH : SMOLDERING_BRANCH) : (stripped ? STRIPPED_CHARRED_BRANCH : CHARRED_BRANCH);
      if (targetId.equals(id)) return state;
      Block target = BuiltInRegistries.BLOCK.get(targetId);
      if (target == Blocks.AIR) return null;
      return copyBranchProperties(state, target.defaultBlockState());
   }

   private static BlockState copyBranchProperties(BlockState from, BlockState to) {
      for (var prop : from.getProperties()) {
         var targetProp = to.getBlock().getStateDefinition().getProperty(prop.getName());
         if (targetProp != null) to = copyValue(from, prop, to, targetProp);
      }
      return to;
   }

   @SuppressWarnings({"unchecked", "rawtypes"})
   private static BlockState copyValue(BlockState from, net.minecraft.world.level.block.state.properties.Property prop, BlockState to, net.minecraft.world.level.block.state.properties.Property targetProp) {
      Comparable value = from.getValue(prop);
      if (targetProp.getPossibleValues().contains(value)) return to.setValue(targetProp, value);
      if (value instanceof Integer i && targetProp instanceof net.minecraft.world.level.block.state.properties.IntegerProperty ip) {
         int max = ip.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(8);
         int min = ip.getPossibleValues().stream().mapToInt(Integer::intValue).min().orElse(1);
         return to.setValue(ip, Math.max(min, Math.min(max, i)));
      }
      return to;
   }

   /** Flint and steel, fire charges and extinguishing items on charred branches (Moonlight's lightable logic). */
   public static final net.mehvahdjukaar.moonlight.api.block.ILightable CHARRED_BRANCH_LIGHT = new net.mehvahdjukaar.moonlight.api.block.ILightable() {
      @Override
      public boolean isLitUp(BlockState state) {
         return isSmolderingBranch(state.getBlock());
      }

      @Override
      public BlockState toggleLitState(BlockState state, boolean lit) {
         BlockState other = withSmoldering(state, lit);
         return other == null ? state : other;
      }
   };

   /** Light level of a smoldering charred branch (the same as a smoldering charred log). */
   public static final int SMOLDERING_LIGHT = 3;

   /**
    * Random tick of a charred branch, like the charred log: rain, a wet biome or water next to it put a smoldering
    * branch out; fire or magma next to it (and nothing wet) lights it again.
    */
   public static void charredBranchRandomTick(BlockState state, ServerLevel level, BlockPos pos) {
      int temperature = 0;
      boolean wet = state.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
      boolean wetBiome = level.getBiome(pos).is(com.immersive_nature.reg.ModTags.WET);
      for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
         BlockPos p = pos.relative(dir);
         BlockState n = level.getBlockState(p);
         boolean water = n.getFluidState().is(net.minecraft.tags.FluidTags.WATER);
         if (level.isRainingAt(p) || water) wet = true;
         if (level.isRainingAt(p) || wetBiome || water) temperature--;
         else if (n.is(com.immersive_nature.reg.ModTags.MAGMA_SOURCE) || n.is(BlockTags.FIRE)) temperature++;
      }
      boolean lit = isSmolderingBranch(state.getBlock());
      BlockState next = null;
      if ((temperature < 0 || wet) && lit) next = withSmoldering(state, false);
      else if (temperature > 0 && !lit) next = withSmoldering(state, true);
      if (next != null && next != state) level.setBlock(pos, next, 3);
   }

   /** Embers rising from a smoldering branch (client). */
   public static void smolderingBranchParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (!isSmolderingBranch(state.getBlock()) || random.nextInt(3) != 0) return;
      level.addParticle(com.immersive_nature.reg.ModParticles.EMBERSPARK.get(), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
            pos.getZ() + random.nextDouble(), 0.1D, 3D, 0.1D);
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

   /** Radius of a block with a "radius" property (Dynamic Trees branches), or 0. */
   public static int getRadius(BlockState state) {
      var radius = state.getBlock().getStateDefinition().getProperty("radius");
      return radius != null && state.getValue(radius) instanceof Integer r ? r : 0;
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
