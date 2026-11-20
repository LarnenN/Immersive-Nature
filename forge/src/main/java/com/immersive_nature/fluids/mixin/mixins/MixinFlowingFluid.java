// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanMap;
import it.unimi.dsi.fastutil.shorts.Short2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2IntMap;
import it.unimi.dsi.fastutil.shorts.Short2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Plane;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.immersive_nature.fluids.FFFlowListenerLevel;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;
import com.immersive_nature.fluids.config.FFConfig;

@Mixin({FlowingFluid.class})
public abstract class MixinFlowingFluid extends Fluid {
   @Unique
   private static short ffCacheKey(BlockPos sourcePos, BlockPos spreadPos) {
      int i = spreadPos.getX() - sourcePos.getX();
      int j = spreadPos.getZ() - sourcePos.getZ();
      return (short)((i + 128 & 0xFF) << 8 | j + 128 & 0xFF);
   }

   @Unique
   private static boolean ff$handleWaterLoggedFlowAndReturnIfHandled(
      Level level, BlockPos posFrom, FluidState fluidState, int amount, BlockState thisState, BlockPos posTo, int destFluidAmount, boolean flowingDown
   ) {
      boolean fromIsWaterloggable = thisState.getBlock() instanceof LiquidBlockContainer && thisState.getBlock() instanceof BucketPickup;
      if (!fromIsWaterloggable
         || (flowingDown ? !FlowingFluids.config.waterLogFlowMode.blocksFlowOutDown() : !FlowingFluids.config.waterLogFlowMode.blocksFlowOutSides())) {
         Block blockTo = level.getBlockState(posTo).getBlock();
         boolean toIsWaterloggable = blockTo instanceof LiquidBlockContainer && blockTo instanceof BucketPickup;
         if (toIsWaterloggable && FlowingFluids.config.waterLogFlowMode.blocksFlowIn(flowingDown)) {
            return true;
         } else if (!fromIsWaterloggable && !toIsWaterloggable) {
            return false;
         } else {
            int totalAmount = destFluidAmount + amount;
            if (totalAmount < 8) {
               return true;
            } else {
               if (toIsWaterloggable && fromIsWaterloggable) {
                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
               } else if (toIsWaterloggable) {
                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), totalAmount - 8);
                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
               } else {
                  if (destFluidAmount > 0) {
                     return true;
                  }

                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posFrom, fluidState.getType(), 0);
                  FFFluidUtils.setFluidStateAtPosToNewAmount(level, posTo, fluidState.getType(), 8);
               }

               return true;
            }
         }
      } else {
         return true;
      }
   }

   protected boolean isRandomlyTicking() {
      return FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this) ? true : super.isRandomlyTicking();
   }

   protected void randomTick(Level level, BlockPos pos, FluidState state, RandomSource random) {
      super.randomTick(level, pos, state, random);
      if (FlowingFluids.config.enableMod
         && FlowingFluids.config.randomTickLevelingDistance > 0
         && level.getChunkAt(pos).getFluidTicks().count() < 16
         && FlowingFluids.config.isFluidAllowed(this)
         && !level.getFluidState(pos.above()).getType().isSame(this)) {
         int amount = state.getAmount();
         if (amount <= this.getDropOff(level)) {
            return;
         }

         int amountLess = amount - 1;
         Direction randomDirection = Plane.HORIZONTAL.getRandomDirection(level.getRandom());
         BiConsumer<MutableBlockPos, MutableBlockPos> move;
         if (level.getRandom().nextBoolean()) {
            move = (mbp, up) -> {
               mbp.move(randomDirection);
               up.move(randomDirection);
            };
         } else {
            Direction offStep = level.getRandom().nextBoolean() ? randomDirection.getClockWise() : randomDirection.getCounterClockWise();
            RandomSource rand = level.getRandom();
            move = (mbp, up) -> {
               Direction dir = rand.nextBoolean() ? randomDirection : offStep;
               mbp.move(dir);
               up.move(dir);
            };
         }

         MutableBlockPos movingDir = pos.mutable();
         MutableBlockPos movingDirAbove = pos.above().mutable();

         for (int i = 0; i < FlowingFluids.config.randomTickLevelingDistance; i++) {
            move.accept(movingDir, movingDirAbove);
            BlockState stateDir = level.getBlockState(movingDir);
            if (!(stateDir.getBlock() instanceof LiquidBlock)) {
               return;
            }

            FluidState fluidStateDir = stateDir.getFluidState();
            if (!fluidStateDir.getType().isSame(this)) {
               return;
            }

            if (level.getFluidState(movingDirAbove).getType().isSame(this)) {
               return;
            }

            int amountDir = fluidStateDir.getAmount();
            if (amountDir > amount) {
               return;
            }

            if (amountDir < amountLess) {
               FFFluidUtils.setFluidStateAtPosToNewAmount(level, movingDir, this, amountDir + 1);
               FFFluidUtils.setFluidStateAtPosToNewAmount(level, pos, this, amountLess);
               return;
            }
         }
      }
   }

   @Shadow
   protected abstract int getDropOff(LevelReader var1);

   @Shadow
   protected abstract void spreadTo(LevelAccessor var1, BlockPos var2, BlockState var3, Direction var4, FluidState var5);

   @Shadow
   protected abstract int getSlopeFindDistance(LevelReader var1);

   @Shadow
   public abstract int getAmount(FluidState var1);

   @Shadow
   public abstract Fluid getFlowing();

   @Inject(
      method = {"getOwnHeight"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ff$differentRenderHeight(FluidState state, CallbackInfoReturnable<Float> cir) {
      if (FlowingFluids.config.enableMod
         && FlowingFluids.config.isFluidAllowed(state)
         && FlowingFluids.config.fullLiquidHeight != FFConfig.LiquidHeight.REGULAR) {
         cir.setReturnValue(switch (FlowingFluids.config.fullLiquidHeight) {
            case BLOCK -> (float)state.getAmount() / 8.0F;
            case SLAB -> (float)state.getAmount() / 16.0F;
            case CARPET -> 0.0625F;
            case REGULAR_LOWER_BOUND -> ((float)state.getAmount() - 0.9F) * 0.8888889F / 7.0F;
            case BLOCK_LOWER_BOUND -> ((float)state.getAmount() - 0.9F) / 7.0F;
            default -> (float)state.getAmount() / 9.0F;
         });
      }
   }

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ff$tickMixin(Level level, BlockPos blockPos, FluidState fluidState, CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(fluidState)) {
         ci.cancel();
         if (FlowingFluids.config.dontTickAtLocation(blockPos, level)) {
            level.scheduleTick(blockPos, this, 200 + level.random.nextInt(200));
            return;
         }

         if (FlowingFluids.debug_killFluidUpdatesUntilTime > 0L) {
            long diff = FlowingFluids.debug_killFluidUpdatesUntilTime - System.currentTimeMillis();
            if (diff > 0L) {
               return;
            }

            FlowingFluids.debug_killFluidUpdatesUntilTime = 0L;
         }

         FlowingFluids.isManeuveringFluids = true;
         int seaLevel = FFFluidUtils.seaLevel(level);
         boolean withinInfBiomeHeights = FlowingFluids.config.fastBiomeRefillAtSeaLevelOnly
            ? seaLevel == blockPos.getY() || seaLevel - 1 == blockPos.getY()
            : seaLevel == blockPos.getY() && blockPos.getY() > 0;
         boolean isWaterAndInfiniteBiome = fluidState.is(FluidTags.WATER)
            && withinInfBiomeHeights
            && FFFluidUtils.matchInfiniteBiomes(level.getBiome(blockPos))
            && level.getBrightness(LightLayer.SKY, blockPos) > 0;
         boolean dontConsumeWater = isWaterAndInfiniteBiome
            && seaLevel != blockPos.getY()
            && level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeNonConsumeChance;
         BlockState thisState = level.getBlockState(blockPos);
         boolean haveListeners = !((FFFlowListenerLevel)level).ff$getFlowListenerPositions().isEmpty();
         List<Pair<BlockPos, Direction>> forListeners = haveListeners ? new ArrayList<>() : null;
         boolean wentDown = false;
         int initAmount = fluidState.getAmount();

         try {
            BlockPos posDown = blockPos.below();
            int remainingAmount = this.flowing_fluids$checkAndFlowDown(level, blockPos, fluidState, thisState, posDown, level.getBlockState(posDown), initAmount);
            if (remainingAmount != initAmount) {
               if (haveListeners) {
                  forListeners.add(Pair.of(blockPos, Direction.DOWN));
               }

               wentDown = true;
            }

            if (remainingAmount <= 0) {
               return;
            }

            if (fluidState.getAmount() == 8 && thisState.liquid()) {
               BlockPos abovePos = blockPos.above();
               BlockState above = level.getBlockState(abovePos);
               if (above.liquid()) {
                  FluidState aboveF = above.getFluidState();
                  int aboveAmount = aboveF.getAmount();
                  if (aboveAmount > 0) {
                     FlowingFluid flow = (FlowingFluid)aboveF.getType();
                     if (FFFluidUtils.canFluidFlowFromPosToDirectionFitOverride(flow, level, abovePos, above, Direction.DOWN, blockPos, thisState)) {
                        Pair<Integer, Runnable> remainder = FFFluidUtils.placeConnectedFluidAmountAndPlaceActionAndNotify(
                           level, blockPos, aboveAmount, flow, 40, false, !FlowingFluids.pistonTick, haveListeners
                        );
                        if ((Integer)remainder.first() < aboveAmount) {
                           ((Runnable)remainder.second()).run();
                           if (!dontConsumeWater) {
                              FFFluidUtils.setFluidStateAtPosToNewAmount(level, abovePos, flow, (Integer)remainder.first());
                           }
                        }
                     }
                  }
               }
            }

            if (remainingAmount > this.getDropOff(level)) {
               Direction directionFinal = this.ff$flowToSides(level, blockPos, fluidState, remainingAmount, thisState);
               if (directionFinal != null && haveListeners) {
                  forListeners.add(Pair.of(blockPos, directionFinal));
               }
            } else if (FlowingFluids.config.flowToEdges) {
               Direction dir = this.flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, 1, true);
               if (dir != null) {
                  BlockPos pos = blockPos.relative(dir);
                  this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, 0, thisState, dir);
                  this.flowing_fluids$spreadTo2(level, pos, level.getBlockState(pos), dir, remainingAmount);
                  if (haveListeners) {
                     forListeners.add(Pair.of(blockPos, dir));
                  }
               }
            }
         } finally {
            if (isWaterAndInfiniteBiome) {
               if (seaLevel == blockPos.getY()) {
                  int amount = level.getFluidState(blockPos).getAmount();
                  if (amount > 0
                     && (amount <= this.getDropOff(level) || level.getRandom().nextFloat() < FlowingFluids.config.infiniteWaterBiomeDrainSurfaceChance)) {
                     FluidState below = level.getFluidState(blockPos.below());
                     if (below.getAmount() == 8 && below.is(FluidTags.WATER)) {
                        level.setBlockAndUpdate(blockPos, FFFluidUtils.getBlockForFluidByAmount(this, amount - 1));
                     }
                  }
               } else if (dontConsumeWater) {
                  level.setBlock(blockPos, thisState, 0);
               }
            }

            FlowingFluids.isManeuveringFluids = false;
            FlowingFluids.pistonTick = false;
            boolean hasListeners = haveListeners && !forListeners.isEmpty();
            if (initAmount > 4 && (hasListeners || level.getFluidState(blockPos).getAmount() != initAmount)) {
               FFFluidUtils.playFlowSound(level, blockPos, this);
            }

            if (hasListeners) {
               for (Pair<BlockPos, Direction> flow : forListeners) {
                  FFFluidUtils.notifyListeners(
                     level, (BlockPos)flow.left(), (Direction)flow.right(), this, wentDown && ((BlockPos)flow.left()).equals(blockPos)
                  );
               }
            }
         }
      }
   }

   @Unique
   private Direction ff$flowToSides(Level level, BlockPos blockPos, FluidState fluidState, int amount, BlockState thisState) {
      Direction dir = this.flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(level, blockPos, fluidState, amount, false);
      if (dir == null) {
         return null;
      } else {
         BlockPos posDir = blockPos.relative(dir);
         int destFluidAmount = level.getFluidState(posDir).getAmount();
         if (ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDir, destFluidAmount, false)) {
            return dir;
         } else {
            int difference = amount - destFluidAmount;
            int averageLevel = destFluidAmount + difference / 2;
            boolean hasRemainder = difference % 2 != 0;
            int toAmount;
            if (hasRemainder) {
               toAmount = averageLevel + 1;
            } else {
               toAmount = averageLevel;
            }

            FFFluidUtils.setFluidStateAtPosToNewAmount(level, blockPos, fluidState.getType(), averageLevel);
            FFFluidUtils.setFluidStateAtPosToNewAmount(level, posDir, fluidState.getType(), toAmount);
            return dir;
         }
      }
   }

   @Unique
   private int flowing_fluids$checkAndFlowDown(
      Level level, BlockPos blockPos, FluidState fluidState, BlockState thisState, BlockPos posDown, BlockState stateDown, int amount
   ) {
      FluidState downFState = level.getFluidState(posDown);
      if (this.flowing_fluids$canSpreadTo(
         fluidState.getType(), fluidState.getAmount(), level, blockPos, thisState, Direction.DOWN, posDown, stateDown, downFState
      )) {
         if (!downFState.isEmpty() && !downFState.getType().isSame(fluidState.getType())) {
            this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, amount - 1, thisState, Direction.DOWN);
            this.flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, 1);
            return amount - 1;
         }

         if (FlowingFluids.config.easyPistonPump && FlowingFluids.config.enablePistonPushing) {
            BlockState block = level.getBlockState(posDown.below());
            if (block.is(Blocks.MOVING_PISTON) && block.getValue(DirectionalBlock.FACING) == Direction.UP) {
               level.scheduleTick(blockPos, this, 10);
               FlowingFluids.pistonTick = true;
               return amount;
            }
         }

         int fluidDownAmount = downFState.getAmount();
         if (ff$handleWaterLoggedFlowAndReturnIfHandled(level, blockPos, fluidState, amount, thisState, posDown, fluidDownAmount, true)) {
            return level.getFluidState(blockPos).getAmount();
         }

         int amountDestCanAccept = Math.min(8 - fluidDownAmount, amount);
         if (amountDestCanAccept > 0) {
            int destNewAmount = fluidDownAmount + amountDestCanAccept;
            int sourceNewAmount = amount - amountDestCanAccept;
            this.flowing_fluids$setOrRemoveWaterAmountAt(level, blockPos, sourceNewAmount, thisState, Direction.DOWN);
            this.flowing_fluids$spreadTo2(level, posDown, stateDown, Direction.DOWN, destNewAmount);
            return sourceNewAmount;
         }
      }

      return amount;
   }

   @Unique
   private void flowing_fluids$setOrRemoveWaterAmountAt(Level level, BlockPos blockPos, int amount, BlockState thisState, Direction direction) {
      if (amount > 0) {
         this.flowing_fluids$spreadTo2(level, blockPos, thisState, direction, amount);
      } else {
         FFFluidUtils.removeAllFluidAtPos(level, blockPos, this);
      }
   }

   @Inject(
      method = {"getNewLiquid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void flowing_fluids$validateLiquidMixin(Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<FluidState> cir) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this)) {
         FluidState state = level.getFluidState(blockPos);
         cir.setReturnValue(FFFluidUtils.getStateForFluidByAmount(state.getType(), state.getAmount()));
      }
   }

   @Unique
   @Nullable
   private Direction flowing_fluids$getLowestSpreadableLookingFor4BlockDrops(
      Level level, BlockPos blockPos, FluidState fluidState, int amount, boolean requiresSlope
   ) {
      Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos = new Short2ObjectOpenHashMap();
      boolean anyFlowableNeighbours2LevelsLowerOrMore = requiresSlope;
      List<Direction> toSort = new ArrayList<>(FFFluidUtils.getCardinalsShuffle(level.random));
      Object2IntMap<Direction> fluidAmounts = new Object2IntOpenHashMap(4);

      for (Direction dir : toSort) {
         fluidAmounts.put(dir, level.getFluidState(blockPos.relative(dir)).getAmount());
      }

      toSort.sort(Comparator.comparingInt(fluidAmounts::getInt));
      List<Direction> directionsCanSpreadToSortedByAmount = new ArrayList<>();
      int amountMinusOne = amount - 1;
      BlockState blockState = level.getBlockState(blockPos);
      Fluid type = fluidState.getType();

      for (Direction direction : toSort) {
         BlockPos posDir = blockPos.relative(direction);
         Pair<BlockState, FluidState> statesDir = this.flowing_fluids$getSetPosCache(ffCacheKey(blockPos, posDir), level, statesAtPos, posDir);
         BlockState stateDir = (BlockState)statesDir.first();
         FluidState fluidStateDir = (FluidState)statesDir.second();
         int amountDir = fluidStateDir.getAmount();
         boolean canFlow = this.flowing_fluids$canSpreadToOptionallySameOrEmpty(
            type, amount, level, blockPos, blockState, direction, posDir, stateDir, fluidStateDir, requiresSlope
         );
         if (canFlow) {
            if (!anyFlowableNeighbours2LevelsLowerOrMore) {
               anyFlowableNeighbours2LevelsLowerOrMore = amountDir < amountMinusOne;
            }

            directionsCanSpreadToSortedByAmount.add(direction);
         }
      }

      if (directionsCanSpreadToSortedByAmount.isEmpty()) {
         return null;
      } else {
         boolean requiresSlopeWithOverride = requiresSlope || !anyFlowableNeighbours2LevelsLowerOrMore;
         Direction spreadDirection = this.flowing_fluids$getValidDirectionFromDeepSpreadSearch(
            level, blockPos, fluidState, amount, requiresSlopeWithOverride, directionsCanSpreadToSortedByAmount, statesAtPos
         );
         return spreadDirection == null && !requiresSlopeWithOverride ? directionsCanSpreadToSortedByAmount.get(0) : spreadDirection;
      }
   }

   @Unique
   @Nullable
   private Direction flowing_fluids$getValidDirectionFromDeepSpreadSearch(
      Level level,
      BlockPos blockPos,
      FluidState fluidState,
      int amount,
      boolean requiresSlope,
      List<Direction> directionsCanSpreadToSortedByAmount,
      Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos
   ) {
      int slopeFindDistance = this.getSlopeFindDistance(level);
      if (slopeFindDistance < 1) {
         return null;
      } else {
         Short2BooleanMap posCanFlowDown = new Short2BooleanOpenHashMap();
         posCanFlowDown.put(ffCacheKey(blockPos, blockPos), false);
         Short2IntMap slopeSearchCache = new Short2IntOpenHashMap();
         slopeSearchCache.defaultReturnValue(Integer.MAX_VALUE);
         Direction bestDirection = null;
         int minimalDistance = Integer.MAX_VALUE;

         for (Direction dir : directionsCanSpreadToSortedByAmount) {
            BlockPos posDir = blockPos.relative(dir);
            short key = ffCacheKey(blockPos, posDir);
            int distance;
            if (level.getFluidState(posDir).getAmount() >= amount - 1
               && !this.flowing_fluids$getSetFlowDownCache(key, level, posCanFlowDown, posDir, fluidState.getType(), requiresSlope)) {
               distance = this.flowing_fluids$getSlopeDistance(
                  level,
                  blockPos,
                  1,
                  dir.getOpposite(),
                  fluidState.getType(),
                  amount + 1,
                  posDir,
                  statesAtPos,
                  posCanFlowDown,
                  slopeSearchCache,
                  requiresSlope,
                  slopeFindDistance
               );
            } else {
               distance = 0;
            }

            if ((!requiresSlope || distance <= slopeFindDistance) && distance < minimalDistance) {
               minimalDistance = distance;
               bestDirection = dir;
            }
         }

         return bestDirection;
      }
   }

   @Unique
   protected int flowing_fluids$getSlopeDistance(
      LevelReader level,
      BlockPos sourcePosForKey,
      int distance,
      Direction fromDir,
      Fluid sourceFluid,
      int sourceAmount,
      BlockPos newPos,
      Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos,
      Short2BooleanMap posCanFlowDown,
      Short2IntMap slopeSearchCache,
      boolean forceSlopeDownSameOrEmpty,
      int slopeFindDistance
   ) {
      short currentKey = ffCacheKey(sourcePosForKey, newPos);
      int previousBestDistance = slopeSearchCache.getOrDefault(currentKey, Integer.MAX_VALUE);
      if (previousBestDistance <= distance) {
         return 1000;
      } else {
         slopeSearchCache.put(currentKey, distance);
         int smallest = 1000;
         int searchDistance = distance + 1;
         BlockState state = (BlockState)this.flowing_fluids$getSetPosCache(currentKey, level, statesAtPos, newPos).first();

         for (Direction searchDir : Plane.HORIZONTAL) {
            if (searchDir != fromDir) {
               BlockPos searchPos = newPos.relative(searchDir);
               short searchKey = ffCacheKey(sourcePosForKey, searchPos);
               Pair<BlockState, FluidState> searchStates = this.flowing_fluids$getSetPosCache(searchKey, level, statesAtPos, searchPos);
               if (this.flowing_fluids$canSpreadToOptionallySameOrEmpty(
                  sourceFluid,
                  sourceAmount,
                  level,
                  newPos,
                  state,
                  searchDir,
                  searchPos,
                  (BlockState)searchStates.first(),
                  (FluidState)searchStates.second(),
                  forceSlopeDownSameOrEmpty
               )) {
                  if (((FluidState)searchStates.second()).getAmount() < sourceAmount - 2
                     || this.flowing_fluids$getSetFlowDownCache(searchKey, level, posCanFlowDown, searchPos, sourceFluid, forceSlopeDownSameOrEmpty)) {
                     return searchDistance;
                  }

                  if (searchDistance < slopeFindDistance) {
                     int next = this.flowing_fluids$getSlopeDistance(
                        level,
                        sourcePosForKey,
                        searchDistance,
                        searchDir.getOpposite(),
                        sourceFluid,
                        sourceAmount,
                        searchPos,
                        statesAtPos,
                        posCanFlowDown,
                        slopeSearchCache,
                        forceSlopeDownSameOrEmpty,
                        slopeFindDistance
                     );
                     if (next < smallest) {
                        smallest = next;
                     }
                  }
               }
            }
         }

         return smallest;
      }
   }

   @Unique
   private Pair<BlockState, FluidState> flowing_fluids$getSetPosCache(
      short key, LevelReader level, Short2ObjectMap<Pair<BlockState, FluidState>> statesAtPos, BlockPos pos
   ) {
      return (Pair<BlockState, FluidState>)statesAtPos.computeIfAbsent(key, sx -> {
         BlockState blockState = level.getBlockState(pos);
         return Pair.of(blockState, blockState.getFluidState());
      });
   }

   @Unique
   private boolean flowing_fluids$getSetFlowDownCache(
      short key, LevelReader level, Short2BooleanMap boolAtPos, BlockPos pos, Fluid sourceFluid, boolean forceSlopeDownSameOrEmpty
   ) {
      return boolAtPos.computeIfAbsent(
         key,
         sx -> {
            BlockPos posDown = pos.below();
            return this.flowing_fluids$canSpreadToOptionallySameOrEmpty(
               sourceFluid,
               8,
               level,
               pos,
               level.getBlockState(pos),
               Direction.DOWN,
               posDown,
               level.getBlockState(posDown),
               level.getFluidState(posDown),
               forceSlopeDownSameOrEmpty
            );
         }
      );
   }

   @Unique
   protected void flowing_fluids$spreadTo2(LevelAccessor levelAccessor, BlockPos blockPos, BlockState blockState, Direction direction, int amount) {
      this.spreadTo(levelAccessor, blockPos, blockState, direction, FFFluidUtils.getStateForFluidByAmount(this, amount));
   }

   @Unique
   private boolean flowing_fluids$canSpreadToOptionallySameOrEmpty(
      Fluid sourceFluid,
      int sourceAmount,
      BlockGetter blockGetter,
      BlockPos blockPos,
      BlockState blockState,
      Direction direction,
      BlockPos blockPos2,
      BlockState blockState2,
      FluidState fluidState2,
      boolean enforceSameFluidOrEmpty
   ) {
      return enforceSameFluidOrEmpty && !fluidState2.isEmpty() && !fluidState2.getType().isSame(sourceFluid)
         ? false
         : this.flowing_fluids$canSpreadTo(sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2);
   }

   @Unique
   private boolean flowing_fluids$canSpreadTo(
      Fluid sourceFluid,
      int sourceAmount,
      BlockGetter blockGetter,
      BlockPos blockPos,
      BlockState blockState,
      Direction direction,
      BlockPos blockPos2,
      BlockState blockState2,
      FluidState fluidState2
   ) {
      return FFFluidUtils.canFluidFlowFromPosToDirection(
         (FlowingFluid)sourceFluid, sourceAmount, blockGetter, blockPos, blockState, direction, blockPos2, blockState2, fluidState2
      );
   }
}
