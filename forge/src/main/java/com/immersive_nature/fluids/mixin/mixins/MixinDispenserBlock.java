// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.immersive_nature.fluids.FFBucketItem;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin(
   value = {DispenserBlock.class},
   priority = 2000
)
public class MixinDispenserBlock {
   @Shadow
   @Final
   public static Map<Item, DispenseItemBehavior> DISPENSER_REGISTRY;

   @Inject(
      method = {"registerBehavior"},
      at = {@At("TAIL")}
   )
   private static void ff$wrapBehaviour(ItemLike item, DispenseItemBehavior behavior, CallbackInfo ci) {
      if (item.asItem() instanceof FFBucketItem bucket) {
         if (bucket == Items.BUCKET) {
            DISPENSER_REGISTRY.put(
               item.asItem(),
               new DefaultDispenseItemBehavior() {
                  @NotNull
                  public ItemStack execute(BlockSource blockSource, ItemStack item) {
                     if (FlowingFluids.config.enableMod && item.getItem() instanceof FFBucketItem bucket) {
                        BlockPos blockPos = blockSource.getPos().relative((Direction)blockSource.getBlockState().getValue(DispenserBlock.FACING));
                        Level level = blockSource.getLevel();
                        FluidState fState = level.getFluidState(blockPos);
                        if (fState.getType() instanceof FlowingFluid flowingFluid
                           && FlowingFluids.config.isFluidAllowed(fState)
                           && fState.getAmount() > 0
                           && fState.getAmount() < 8) {
                           int found = FFFluidUtils.collectConnectedFluidAmountAndRemove(level, blockPos, 1, 8, flowingFluid);
                           if (found > 0) {
                              return bucket.ff$bucketOfAmount(flowingFluid.getBucket().getDefaultInstance(), found);
                           }

                           return item;
                        }
                     }

                     return behavior.dispense(blockSource, item);
                  }
               }
            );
         } else {
            DISPENSER_REGISTRY.put(
               item.asItem(),
               new DefaultDispenseItemBehavior() {
                  @NotNull
                  public ItemStack execute(BlockSource blockSource, ItemStack item) {
                     if (FlowingFluids.config.enableMod
                        && item.getItem() instanceof FFBucketItem bucket
                        && FlowingFluids.config.isFluidAllowed(bucket.ff$getFluid())) {
                        BlockPos blockPos = blockSource.getPos().relative((Direction)blockSource.getBlockState().getValue(DispenserBlock.FACING));
                        Level level = blockSource.getLevel();
                        FluidState fState = level.getFluidState(blockPos);
                        if (fState.getAmount() > 0 || item.getDamageValue() > 0) {
                           int amountInBucket = 8 - item.getDamageValue();
                           int remainder = bucket.ff$emptyContents_AndGetRemainder(null, level, blockPos, null, amountInBucket, false);
                           if (remainder != amountInBucket) {
                              ((DispensibleContainerItem)bucket).checkExtraContent(null, level, item, blockPos);
                              return bucket.ff$bucketOfAmount(item, remainder);
                           }

                           return item;
                        }
                     }

                     return behavior.dispense(blockSource, item);
                  }
               }
            );
         }
      }
   }
}
