package com.ordana.immersive_weathering.util;

import com.ordana.immersive_weathering.blocks.LeafPileBlock;
import com.ordana.immersive_weathering.entities.FallingLayerEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;

public final class SnowLeafCompat {
   private static final int[][] SPREAD = new int[][]{
      {0, 0, 4}, {1, 0, 3}, {-1, 0, 3}, {0, 1, 3}, {0, -1, 3}, {1, 1, 2}, {1, -1, 2}, {-1, 1, 2}, {-1, -1, 2}, {2, 0, 1}, {-2, 0, 1}, {0, 2, 1}, {0, -2, 1}
   };
   private static final int SPREAD_TOTAL = 28;
   private static final ThreadLocal<int[]> LAST_DIST = ThreadLocal.withInitial(() -> new int[1]);

   public static boolean isSnow(BlockState var0) {
      return var0 != null && var0.getBlock() instanceof SnowLayerBlock;
   }

   private static BlockPos findSnow(Level var0, BlockPos var1) {
      if (isSnow(var0.getBlockState(var1))) {
         return var1;
      } else {
         BlockPos var2 = var1.below();
         return isSnow(var0.getBlockState(var2)) ? var2 : null;
      }
   }

   public static boolean growOnSnow(Level var0, Block var1, BlockPos var2) {
      return findSnow(var0, var2) != null;
   }

   public static boolean landOnSnow(FallingLayerEntity var0, BlockState var1, BlockPos var2) {
      Level var3 = var0.level();
      if (var3 == null || var3.isClientSide || var1 == null || !(var1.getBlock() instanceof LeafPileBlock)) {
         return false;
      } else if (findSnow(var3, var2) == null) {
         return false;
      } else {
         var0.discard();
         return true;
      }
   }

   public static boolean placeLayer(Level var0, BlockPos var1, BlockState var2, int var3) {
      return var2 != null && var2.getBlock() instanceof LeafPileBlock && isSnow(var0.getBlockState(var1)) ? false : var0.setBlock(var1, var2, var3);
   }

   public static BlockPos spreadHeightmap(ServerLevel var0, Types var1, BlockPos var2) {
      int var3 = var0.random.nextInt(28);
      int[] var4 = SPREAD[0];

      for (int[] var8 : SPREAD) {
         var3 -= var8[2];
         if (var3 < 0) {
            var4 = var8;
            break;
         }
      }

      LAST_DIST.get()[0] = var4[0] * var4[0] + var4[1] * var4[1];
      BlockPos var9 = var4[0] == 0 && var4[1] == 0 ? var2 : var2.offset(var4[0], 0, var4[1]);
      return var0.getHeightmapPos(var1, var9);
   }

   public static int edgeHeight(int var0) {
      int[] var1 = LAST_DIST.get();
      int var2 = var1[0];
      var1[0] = 0;
      if (var2 <= 1) {
         return var0;
      } else {
         return var2 == 2 ? Math.max(1, Math.round((float)var0 * 2.0F / 3.0F)) : Math.max(1, var0 / 3);
      }
   }
}
