// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public interface FFBucketItem {
   int ff$emptyContents_AndGetRemainder(@Nullable Player var1, Level var2, BlockPos var3, @Nullable BlockHitResult var4, int var5, boolean var6);

   ItemStack ff$bucketOfAmount(ItemStack var1, int var2);

   Fluid ff$getFluid();
}
