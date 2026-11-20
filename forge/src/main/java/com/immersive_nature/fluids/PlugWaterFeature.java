// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids;

import it.unimi.dsi.fastutil.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;

public class PlugWaterFeature {
   private static final Pair<Boolean, Runnable> defTrue = Pair.of(true, null);
   private static final Direction[] dirs = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN};

   public static Set<BlockPos> findBlocks(ChunkAccess chunkAccess, int x1, int y1, int z1, int x2, int y2, int z2) {
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      HashSet<BlockPos> set = new HashSet<>();
      int minSectionY = chunkAccess.getMinSection();
      int maxSectionY = chunkAccess.getMaxSection();

      for (int i = minSectionY; i < maxSectionY; i++) {
         LevelChunkSection levelChunkSection = chunkAccess.getSection(chunkAccess.getSectionIndexFromSectionY(i));
         if (levelChunkSection.maybeHas(PlugWaterFeature::isFluidSource)) {
            BlockPos blockPos = SectionPos.of(chunkAccess.getPos(), i).origin();

            for (int y = y1; y < y2; y++) {
               for (int z = z1; z < z2; z++) {
                  for (int x = x1; x < x2; x++) {
                     BlockState blockState = levelChunkSection.getBlockState(x, y, z);
                     if (isFluidSource(blockState)) {
                        set.add(mutableBlockPos.setWithOffset(blockPos, x, y, z).immutable());
                     }
                  }
               }
            }
         }
      }

      return set;
   }

   public static void processChunk(LevelAccessor level, ChunkPos chunk, ChunkAccess chunkAccess) {
      int cx = chunk.getMinBlockX() >> 4;
      int cz = chunk.getMinBlockZ() >> 4;
      boolean hasXNeg = level.hasChunk(cx - 1, cz);
      boolean hasXPos = level.hasChunk(cx + 1, cz);
      boolean hasZNeg = level.hasChunk(cx, cz - 1);
      boolean hasZPos = level.hasChunk(cx, cz + 1);
      Set<BlockPos> set = findBlocks(chunkAccess, 0, 0, 0, 16, 16, 16);
      if (hasXNeg) {
         set.addAll(findBlocks(level.getChunk(cx - 1, cz), 15, 0, 0, 16, 16, 16));
      }

      if (hasXPos) {
         set.addAll(findBlocks(level.getChunk(cx + 1, cz), 0, 0, 0, 1, 16, 16));
      }

      if (hasZNeg) {
         set.addAll(findBlocks(level.getChunk(cx, cz - 1), 0, 0, 15, 16, 16, 16));
      }

      if (hasZPos) {
         set.addAll(findBlocks(level.getChunk(cx, cz + 1), 0, 0, 0, 16, 16, 1));
      }

      if (!set.isEmpty()) {
         int minx = chunk.getMinBlockX();
         int minz = chunk.getMinBlockZ();
         int maxx = chunk.getMaxBlockX();
         int maxz = chunk.getMaxBlockZ();
         MutableBlockPos mutableBlockPos = new MutableBlockPos();
         int sea = FFFluidUtils.seaLevel(level) - 5;
         HashSet<BlockPos> doneSet = new HashSet<>();
         Supplier<Pair<Boolean, Runnable>> testDo = () -> {
            if (mutableBlockPos.getX() < minx || mutableBlockPos.getX() > maxx) {
               return defTrue;
            } else if (mutableBlockPos.getZ() < minz || mutableBlockPos.getZ() > maxz) {
               return defTrue;
            } else if (set.contains(mutableBlockPos)) {
               return defTrue;
            } else {
               BlockState blockState = chunkAccess.getBlockState(mutableBlockPos);
               if (blockState.isAir() && blockState.getFluidState().isEmpty()) {
                  BlockPos immutable = mutableBlockPos.immutable();
                  doneSet.add(immutable);
                  return Pair.of(false, (Runnable)() -> fillBlock(chunkAccess, immutable, sea));
               } else {
                  return null;
               }
            }
         };
         List<Runnable> runs = new ArrayList<>();

         for (BlockPos blockPos : set) {
            if (!doneSet.contains(blockPos)) {
               boolean neighbourWater = false;
               runs.clear();

               for (Direction dir : dirs) {
                  mutableBlockPos.setWithOffset(blockPos, dir);
                  Pair<Boolean, Runnable> result = testDo.get();
                  if (result != null) {
                     if ((Boolean)result.first()) {
                        neighbourWater = true;
                     } else {
                        runs.add((Runnable)result.second());
                     }
                  }
               }

               if (neighbourWater) {
                  for (Runnable run : runs) {
                     run.run();
                  }
               }
            }
         }
      }
   }

   private static boolean isFluidSource(BlockState state) {
      FluidState fluid = state.getFluidState();
      return !fluid.isEmpty() && fluid.isSource() ? FlowingFluids.config.isFluidAllowed(fluid) : false;
   }

   private static void fillBlock(ChunkAccess chunk, BlockPos pos, int seaLevel) {
      Holder<Biome> biome = chunk.getNoiseBiome(pos.getX(), pos.getY(), pos.getZ());
      BlockState blockState;
      if (biome.is(BiomeTags.IS_NETHER)) {
         blockState = Blocks.NETHERRACK.defaultBlockState();
      } else if (biome.is(BiomeTags.IS_END)) {
         blockState = Blocks.END_STONE.defaultBlockState();
      } else if (pos.getY() < 0) {
         blockState = Blocks.DEEPSLATE.defaultBlockState();
      } else if (pos.getY() < seaLevel) {
         blockState = Blocks.STONE.defaultBlockState();
      } else if (!biome.is(BiomeTags.HAS_VILLAGE_DESERT) && !biome.is(BiomeTags.IS_BEACH) && !biome.is(BiomeTags.IS_OCEAN)) {
         blockState = Blocks.MUD.defaultBlockState();
      } else {
         blockState = Blocks.SAND.defaultBlockState();
      }

      if (FlowingFluids.config.announceWorldGenActions) {
         FlowingFluids.info("placed block during world gen: " + blockState + " at /tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
      }

      FlowingFluids.waterPluggedThisSession++;
      chunk.setBlockState(pos, blockState, false);
   }
}
