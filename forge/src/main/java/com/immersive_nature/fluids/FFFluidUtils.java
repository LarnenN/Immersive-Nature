// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids;

import com.google.common.collect.Collections2;
import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FFFluidUtils {
   private static final List<List<Direction>> HORIZONTAL_PERMUTATIONS = new ArrayList<>(Collections2.permutations(Plane.HORIZONTAL.stream().toList()));
   private static long lastFlowSoundTime = 0L;

   public static int seaLevel(LevelReader level) {
      int override = FlowingFluids.config.dimensionSeaLevelOverrides.getOrDefault(level.dimensionType().hashCode(), Integer.MIN_VALUE);
      if (override != Integer.MIN_VALUE) {
         return override;
      } else {
         int def = FlowingFluids.config.defaultSeaLevelOverride;
         return def != Integer.MIN_VALUE ? def : level.getSeaLevel();
      }
   }

   @NotNull
   public static ResourceLocation res(String fullPath) {
      return new ResourceLocation(fullPath);
   }

   @NotNull
   public static ResourceLocation res(String namespace, String path) {
      return new ResourceLocation(namespace, path);
   }

   public static boolean dimensionEvaporatesWaterVanilla(LevelReader level) {
      return level.dimensionType().ultraWarm();
   }

   public static boolean canFluidFlowToNeighbourFromPos(LevelAccessor accessor, BlockPos pos, FlowingFluid fluid, int amount) {
      for (Direction direction : Plane.HORIZONTAL) {
         if (canFluidFlowFromPosToDirection(fluid, amount, accessor, pos, direction)) {
            return true;
         }
      }

      return false;
   }

   public static FluidState getStateForFluidByAmount(Fluid fluid, int amount) {
      if (amount < 1) {
         return Fluids.EMPTY.defaultFluidState();
      } else if (fluid instanceof FlowingFluid flowing) {
         return amount >= 8 ? flowing.getSource(false) : flowing.getFlowing(amount, false);
      } else {
         return amount >= 8 ? fluid.defaultFluidState() : (FluidState)fluid.defaultFluidState().trySetValue(FlowingFluid.LEVEL, amount);
      }
   }

   public static BlockState getBlockForFluidByAmount(Fluid fluid, int amount) {
      return getStateForFluidByAmount(fluid, amount).createLegacyBlock();
   }

   public static boolean setFluidStateAtPosToNewAmount(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int newAmount) {
      if (newAmount < 1) {
         return removeAllFluidAtPos(levelAccessor, pos, fluid);
      } else {
         BlockState blockState = levelAccessor.getBlockState(pos);
         if (blockState.getBlock() instanceof LiquidBlockContainer liquidBlockContainer) {
            if (newAmount == 8) {
               return liquidBlockContainer.placeLiquid(levelAccessor, pos, blockState, getStateForFluidByAmount(fluid, newAmount));
            }

            if (blockState.getBlock() instanceof BucketPickup bucketPickup) {
               bucketPickup.pickupBlock(levelAccessor, pos, blockState);
               return true;
            }

            if (!blockState.canBeReplaced(fluid)) {
               return false;
            }
         }

         if (!blockState.isAir() && fluid instanceof FlowingFluid flowingFluid) {
            flowingFluid.beforeDestroyingBlock(levelAccessor, pos, blockState);
         }

         return levelAccessor.setBlock(pos, getStateForFluidByAmount(fluid, newAmount).createLegacyBlock(), 3);
      }
   }

   public static boolean removeAllFluidAtPos(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid) {
      BlockState blockState = levelAccessor.getBlockState(pos);
      if (blockState.getBlock() instanceof LiquidBlockContainer && blockState.getBlock() instanceof BucketPickup bucketPickup) {
         bucketPickup.pickupBlock(levelAccessor, pos, blockState);
         return true;
      } else {
         if (!blockState.isAir() && fluid instanceof FlowingFluid flowingFluid) {
            flowingFluid.beforeDestroyingBlock(levelAccessor, pos, blockState);
         }

         return levelAccessor.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
      }
   }

   public static int removeAmountFromFluidAtPosWithRemainder(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int removeAmount) {
      FluidState state = levelAccessor.getFluidState(pos);
      if (state.getType().isSame(fluid)) {
         int currentAmount = state.getAmount();
         if (currentAmount <= removeAmount) {
            removeAllFluidAtPos(levelAccessor, pos, fluid);
            return removeAmount - currentAmount;
         } else {
            setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, currentAmount - removeAmount);
            return 0;
         }
      } else {
         return removeAmount;
      }
   }

   public static int addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(LevelAccessor levelAccessor, BlockPos pos, FlowingFluid fluid, int addAmount) {
      Pair<Integer, Runnable> data = placeConnectedFluidAmountAndPlaceAction(levelAccessor, pos, addAmount, fluid);
      if ((Integer)data.first() != addAmount) {
         ((Runnable)data.second()).run();
         return (Integer)data.first();
      } else {
         return addAmount;
      }
   }

   public static int addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(
      LevelAccessor levelAccessor, BlockPos pos, FlowingFluid fluid, int addAmount, boolean canSpreadUp, boolean canSpreadDown
   ) {
      Pair<Integer, Runnable> data = placeConnectedFluidAmountAndPlaceAction(levelAccessor, pos, addAmount, fluid, 80, canSpreadUp, canSpreadDown);
      if ((Integer)data.first() != addAmount) {
         ((Runnable)data.second()).run();
         return (Integer)data.first();
      } else {
         return addAmount;
      }
   }

   public static int addAmountToFluidAtPosWithRemainder(LevelAccessor levelAccessor, BlockPos pos, Fluid fluid, int addAmount) {
      FluidState state = levelAccessor.getFluidState(pos);
      if (state.isEmpty() || state.getType().isSame(fluid)) {
         int currentAmount = state.getAmount();
         if (currentAmount == 8) {
            return addAmount;
         }

         if (currentAmount + addAmount <= 8) {
            if (setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, currentAmount + addAmount)) {
               return 0;
            }
         } else if (setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, 8)) {
            return currentAmount + addAmount - 8;
         }
      }

      return addAmount;
   }

   public static boolean canFluidFlowFromPosToDirection(FlowingFluid fluid, int amount, LevelAccessor levelAccessor, BlockPos fromPos, Direction direction) {
      BlockPos blockPos2 = fromPos.relative(direction);
      BlockState blockState2 = levelAccessor.getBlockState(blockPos2);
      FluidState fluidState2 = blockState2.getFluidState();
      return canFluidFlowFromPosToDirection(
         fluid, amount, levelAccessor, fromPos, levelAccessor.getBlockState(fromPos), direction, blockPos2, blockState2, fluidState2
      );
   }

   public static boolean canFluidFlowFromPosToDirection(
      FlowingFluid sourceFluid,
      int sourceAmount,
      BlockGetter blockGetter,
      BlockPos blockPos,
      BlockState blockState,
      Direction direction,
      BlockPos blockPos2,
      BlockState blockState2,
      FluidState fluidState2
   ) {
      return (
            fluidState2.canBeReplacedWith(blockGetter, blockPos2, sourceFluid, direction)
               || canFitIntoFluid(sourceFluid, fluidState2, direction, sourceAmount, blockState2)
         )
         && sourceFluid.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
         && sourceFluid.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
   }

   public static boolean canFluidFlowFromPosToDirectionFitOverride(
      FlowingFluid sourceFluid,
      BlockGetter blockGetter,
      BlockPos blockPos,
      BlockState blockState,
      Direction direction,
      BlockPos blockPos2,
      BlockState blockState2
   ) {
      return sourceFluid.canPassThroughWall(direction, blockGetter, blockPos, blockState, blockPos2, blockState2)
         && sourceFluid.canHoldFluid(blockGetter, blockPos2, blockState2, sourceFluid);
   }

   private static boolean canFitIntoFluid(Fluid thisFluid, FluidState fluidStateTo, Direction direction, int amount, BlockState blockStateTo) {
      if (fluidStateTo.isEmpty()) {
         return true;
      } else if (fluidStateTo.getType().isSame(thisFluid)) {
         return direction == Direction.DOWN ? fluidStateTo.getAmount() < 8 : fluidStateTo.getAmount() < amount;
      } else {
         return false;
      }
   }

   public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceAction(
      LevelAccessor levelAccessor, BlockPos blockPos, int amountToPlace, FlowingFluid fluid
   ) {
      return placeConnectedFluidAmountAndPlaceAction(levelAccessor, blockPos, amountToPlace, fluid, 80, true, true);
   }

   public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceAction(
      LevelAccessor levelAccessor, BlockPos blockPos, int amountToPlace, FlowingFluid fluid, int depth, boolean doUp, boolean doDown
   ) {
      return placeConnectedFluidAmountAndPlaceActionAndNotify(levelAccessor, blockPos, amountToPlace, fluid, depth, doUp, doDown, false);
   }

   public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceActionAndNotify(
      LevelAccessor levelAccessor, BlockPos blockPos, int amountToPlace, FlowingFluid fluid, int depth, boolean doUp, boolean doDown, boolean notifyListeners
   ) {
      FluidState originalState = levelAccessor.getFluidState(blockPos);
      int originalAmount = originalState.getAmount();
      if (originalState.getType().isSame(fluid) && originalAmount > 0) {
         return originalAmount + amountToPlace <= 8 ? Pair.of(0, (Runnable)() -> {
            setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount + amountToPlace);
            if (notifyListeners) {
               notifyListeners(levelAccessor, blockPos, Direction.DOWN, fluid, false);
            }
         }) : placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(levelAccessor, blockPos, amountToPlace, fluid, depth, doUp, doDown, null, notifyListeners);
      } else {
         return Pair.of(amountToPlace, null);
      }
   }

   @NotNull
   public static Pair<Integer, Runnable> placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(
      LevelAccessor levelAccessor,
      BlockPos blockPos,
      int amountToPlace,
      FlowingFluid fluid,
      int depth,
      boolean doUp,
      boolean doDown,
      @Nullable List<BlockPos> ignore,
      boolean notifyListeners
   ) {
      List<BlockPos> toCheck = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();
      List<Direction> dirs = new ArrayList<>();
      toCheck.add(blockPos);
      if (ignore != null) {
         seen.addAll(ignore);
      }

      Consumer<BlockPos> addSurroundingPositions = blockPos1 -> {
         for (Direction direction : getCardinalsShuffle(levelAccessor.getRandom())) {
            BlockPos offset = blockPos1.relative(direction);
            if (seen.add(offset)) {
               toCheck.add(offset);
               if (notifyListeners) {
                  dirs.add(direction);
               }
            }
         }

         if (doUp) {
            BlockPos up = blockPos1.above();
            if (seen.add(up)) {
               toCheck.add(up);
               if (notifyListeners) {
                  dirs.add(Direction.UP);
               }
            }
         }

         if (doDown) {
            BlockPos down = blockPos1.below();
            if (seen.add(down)) {
               toCheck.add(down);
               if (notifyListeners) {
                  dirs.add(Direction.DOWN);
               }
            }
         }
      };
      addSurroundingPositions.accept(blockPos);
      List<Runnable> onSuccessPlacers = new ArrayList<>();
      int amountLeftToPlace = amountToPlace;

      int i;
      for (i = ignore != null && ignore.contains(blockPos) ? 1 : 0; i < toCheck.size(); i++) {
         BlockPos pos = toCheck.get(i);
         if (toCheck.size() > depth) {
            break;
         }

         FluidState state = levelAccessor.getFluidState(pos);
         if (fluid.isSame(state.getType()) || state.isEmpty() && levelAccessor.getBlockState(pos).isAir()) {
            int space = 8 - state.getAmount();
            if (space > 0) {
               if (space >= amountLeftToPlace) {
                  int newAmount = state.getAmount() + amountLeftToPlace;
                  onSuccessPlacers.add(() -> setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, newAmount));
                  amountLeftToPlace = 0;
                  break;
               }

               onSuccessPlacers.add(() -> setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, 8));
               amountLeftToPlace -= space;
            }

            addSurroundingPositions.accept(pos);
         }
      }

      if (amountLeftToPlace == amountToPlace) {
         return Pair.of(amountToPlace, null);
      } else {
         int finalI = i;
         return Pair.of(amountLeftToPlace, (Runnable)() -> {
            onSuccessPlacers.forEach(Runnable::run);
            if (notifyListeners) {
               IntStream.range(0, Math.min(finalI, toCheck.size() - 1)).forEach(j -> notifyListeners(levelAccessor, toCheck.get(j), dirs.get(j), fluid, false));
            }
         });
      }
   }

   public static int collectConnectedFluidAmountAndRemove(
      LevelAccessor levelAccessor, BlockPos blockPos, int minAmountRequired, int maxAmountToFind, FlowingFluid fluid
   ) {
      Pair<Integer, Runnable> data = collectConnectedFluidAmountAndRemoveAction(levelAccessor, blockPos, minAmountRequired, maxAmountToFind, fluid);
      if ((Integer)data.first() != 0) {
         ((Runnable)data.second()).run();
         return (Integer)data.first();
      } else {
         return 0;
      }
   }

   public static Pair<Integer, Runnable> collectConnectedFluidAmountAndRemoveAction(
      LevelAccessor levelAccessor, BlockPos blockPos, int minAmountRequired, int maxAmountToFind, FlowingFluid fluid
   ) {
      return collectConnectedFluidAmountAndRemoveAction(levelAccessor, blockPos, minAmountRequired, maxAmountToFind, fluid, 40);
   }

   public static Pair<Integer, Runnable> collectConnectedFluidAmountAndRemoveAction(
      LevelAccessor levelAccessor, BlockPos blockPos, int minAmountRequired, int maxAmountToFind, FlowingFluid fluid, int depth
   ) {
      FluidState originalState = levelAccessor.getFluidState(blockPos);
      int originalAmount = originalState.getAmount();
      if (!originalState.getType().isSame(fluid) || originalAmount <= 0) {
         return Pair.of(0, null);
      } else if (originalAmount >= maxAmountToFind) {
         return Pair.of(maxAmountToFind, (Runnable)() -> setFluidStateAtPosToNewAmount(levelAccessor, blockPos, fluid, originalAmount - maxAmountToFind));
      } else {
         List<BlockPos> toCheck = new ArrayList<>();
         toCheck.add(blockPos);

         for (Direction direction : Direction.allShuffled(levelAccessor.getRandom())) {
            BlockPos offset = blockPos.relative(direction);
            toCheck.add(offset);
         }

         List<Runnable> onSuccessAirSetters = new ArrayList<>();
         int foundAmount = 0;

         for (int i = 0; i < toCheck.size(); i++) {
            BlockPos pos = toCheck.get(i);
            if (toCheck.size() > depth) {
               break;
            }

            FluidState state = levelAccessor.getFluidState(pos);
            if (fluid.isSame(state.getType())) {
               int amount = state.getAmount();
               if (amount > 0) {
                  foundAmount += amount;
                  if (foundAmount > maxAmountToFind) {
                     int finalLevel = foundAmount - maxAmountToFind;
                     onSuccessAirSetters.add(() -> setFluidStateAtPosToNewAmount(levelAccessor, pos, fluid, finalLevel));
                     foundAmount = maxAmountToFind;
                     break;
                  }

                  onSuccessAirSetters.add(() -> removeAllFluidAtPos(levelAccessor, pos, fluid));
                  if (foundAmount == maxAmountToFind) {
                     break;
                  }

                  for (Direction direction : Direction.allShuffled(levelAccessor.getRandom())) {
                     BlockPos offset = pos.relative(direction);
                     if (!toCheck.contains(offset)) {
                        toCheck.add(offset);
                     }
                  }
               }
            }
         }

         return foundAmount < minAmountRequired ? Pair.of(0, null) : Pair.of(foundAmount, (Runnable)() -> onSuccessAirSetters.forEach(Runnable::run));
      }
   }

   public static List<Direction> getCardinalsShuffle(RandomSource random) {
      return HORIZONTAL_PERMUTATIONS.get(random.nextInt(HORIZONTAL_PERMUTATIONS.size()));
   }

   private static boolean checkBlockIsNonDisplacer(Fluid fluid, BlockState state) {
      if (matchesAny(FlowingFluids.nonDisplacers.get(fluid), state::is)) {
         return true;
      } else if (matchesAny(FlowingFluids.nonDisplacerTags.get(fluid), state::is)) {
         return true;
      } else {
         String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
         return matchesAny(FlowingFluids.nonDisplacerIds.get(fluid), blockId::equals)
            ? true
            : matchesAny(FlowingFluids.nonDisplacerTagIds.get(fluid), tag -> state.getTags().anyMatch(it -> it.location().toString().equals(tag)));
      }
   }

   private static <T> boolean matchesAny(Iterable<T> items, Predicate<T> condition) {
      if (items == null) {
         return false;
      } else {
         for (T item : items) {
            if (condition.test(item)) {
               return true;
            }
         }

         return false;
      }
   }

   public static void displaceFluids(Level level, BlockPos pos, BlockState state, int flags, LevelChunk levelChunk, BlockState originalState) {
      if (!level.isClientSide() && FlowingFluids.config.enableMod && !FlowingFluids.isManeuveringFluids) {
         boolean isPistonMove = FlowingFluids.config.enablePistonPushing && (flags & 18) == 18;
         if ((FlowingFluids.config.enableDisplacement || isPistonMove)
            && !originalState.getFluidState().isEmpty()
            && originalState.getFluidState().getType() instanceof FlowingFluid flowSource
            && FlowingFluids.config.isFluidAllowed(flowSource)
            && (isPistonMove || !state.isAir())
            && state.getFluidState().isEmpty()
            && (flags & 64) != 64
            && (!(state.getBlock() instanceof LiquidBlockContainer) || !(originalState.getBlock() instanceof BucketPickup))
            && !checkBlockIsNonDisplacer(flowSource, state)) {
            FlowingFluids.isManeuveringFluids = true;

            try {
               int amountRemaining = originalState.getFluidState().getAmount();
               boolean didUp = false;
               boolean piston = isPistonMove && FlowingFluids.lastPistonMoveDirection != null;
               if (piston) {
                  if (FlowingFluids.lastPistonMoveDirection == Direction.UP) {
                     amountRemaining = displaceUpFar(level, pos, levelChunk, originalState, flowSource, amountRemaining);
                     didUp = true;
                  }

                  if (amountRemaining > 0) {
                     amountRemaining = displaceDeepSearch(level, pos, flowSource, amountRemaining, FlowingFluids.lastPistonMoveDirection, false);
                  }
               }

               if (FlowingFluids.config.enableDisplacement) {
                  if (!didUp && amountRemaining > 0) {
                     amountRemaining = displaceUpFar(level, pos, levelChunk, originalState, flowSource, amountRemaining);
                  }

                  if (amountRemaining > 0) {
                     amountRemaining = displaceDeepSearch(level, pos, flowSource, amountRemaining, FlowingFluids.lastPistonMoveDirection, piston);
                  }
               }

               if (FlowingFluids.config.displacementSounds.allow(piston)) {
                  boolean lava = flowSource.isSame(Fluids.LAVA);
                  level.playSound(
                     null,
                     pos,
                     amountRemaining > 0 ? (lava ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL) : (lava ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
               }
            } finally {
               FlowingFluids.isManeuveringFluids = false;
            }
         }
      }
   }

   private static int displaceDeepSearch(
      Level level, BlockPos pos, FlowingFluid flowSource, int amountRemaining, @Nullable Direction pistonDirection, boolean skipPistonDirection
   ) {
      ArrayList<BlockPos> positionsToIgnore = new ArrayList<>();
      positionsToIgnore.add(pos);
      if (pistonDirection != null) {
         if (skipPistonDirection) {
            positionsToIgnore.add(pos.relative(pistonDirection));
         } else {
            for (Direction direction : Direction.values()) {
               if (direction != pistonDirection) {
                  positionsToIgnore.add(pos.relative(direction));
               }
            }
         }
      }

      Pair<Integer, Runnable> result = placeConnectedFluidAmountAndPlaceActionCanIgnoreStart(
         level, pos, amountRemaining, flowSource, (int)(200.0F * FlowingFluids.config.displacementDepthMultiplier), true, true, positionsToIgnore, false
      );
      if ((Integer)result.first() < amountRemaining) {
         ((Runnable)result.second()).run();
         amountRemaining = (Integer)result.first();
      }

      return amountRemaining;
   }

   private static int displaceUpFar(Level level, BlockPos pos, LevelChunk levelChunk, BlockState originalState, FlowingFluid flowSource, int amountRemaining) {
      MutableBlockPos posTraversing = new MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
      int height = Math.min(levelChunk.getMaxBuildHeight(), pos.getY() + (int)(48.0F * FlowingFluids.config.displacementDepthMultiplier));

      while (amountRemaining > 0 && posTraversing.getY() < height) {
         posTraversing.move(Direction.UP);
         BlockState offsetState = level.getBlockState(posTraversing);
         if (offsetState.getFluidState().getType() instanceof FlowingFluid) {
            amountRemaining = addAmountToFluidAtPosWithRemainder(level, posTraversing, flowSource, amountRemaining);
         } else {
            if (!offsetState.isAir()) {
               break;
            }

            level.setBlock(posTraversing, originalState.getFluidState().createLegacyBlock(), 3);
            amountRemaining = 0;
         }
      }

      return amountRemaining;
   }

   public static boolean matchInfiniteBiomes(Holder<Biome> biome) {
      return FlowingFluids.infiniteBiomeTags.stream().anyMatch(biome::is) || FlowingFluids.infiniteBiomes.stream().anyMatch(biome::is);
   }

   public static void playFlowSound(Level level, BlockPos blockPos, Fluid fluid) {
      if (FlowingFluids.config.flowSoundChance != 0.0F) {
         if (!(level.getRandom().nextFloat() > FlowingFluids.config.flowSoundChance)) {
            long currentTime = level.getGameTime();
            if (lastFlowSoundTime + 60L <= currentTime) {
               boolean lava = fluid.isSame(Fluids.LAVA);
               level.playSound(null, blockPos, lava ? SoundEvents.LAVA_AMBIENT : SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
               lastFlowSoundTime = currentTime;
            }
         }
      }
   }

   public static void notifyListeners(LevelAccessor level, BlockPos pos, Direction direction, Fluid fluid, boolean downAlso) {
      if (!level.isClientSide() && level instanceof FFFlowListenerLevel flowListenerLevel) {
         Map<BlockPos, Set<BlockPos>> listeners = flowListenerLevel.ff$getFlowListenerPositions();
         Set<BlockPos> positions = listeners.get(pos);
         if (positions != null) {
            if (positions.isEmpty()) {
               listeners.remove(pos);
               return;
            }

            Iterator<BlockPos> iterator = positions.iterator();

            while (iterator.hasNext()) {
               BlockPos listenerPos = iterator.next();
               if (level.getBlockEntity(listenerPos) instanceof IFFFlowListener listener) {
                  listener.ff$acceptRecentFlow(pos, direction, fluid, downAlso);
               } else {
                  iterator.remove();
               }
            }
         }
      }
   }
}
