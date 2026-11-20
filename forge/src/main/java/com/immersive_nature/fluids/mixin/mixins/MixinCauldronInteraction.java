// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.immersive_nature.fluids.FFBucketItem;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({Bootstrap.class})
public abstract class MixinCauldronInteraction {
   @Inject(
      method = {"bootStrap"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/cauldron/CauldronInteraction;bootStrap()V",
         shift = Shift.AFTER
      )}
   )
   private static void ff$bootStrap(CallbackInfo ci) {
      CauldronInteraction.EMPTY.compute(Items.LAVA_BUCKET, (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
            if (!allow(Fluids.LAVA)) {
               return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
            } else if (!level.isClientSide()) {
               if (!isUnderWater(level, blockPos) && itemStack.getDamageValue() == 0) {
                  player.setItemInHand(interactionHand, Items.BUCKET.getDefaultInstance());
                  player.awardStat(Stats.FILL_CAULDRON);
                  player.awardStat(Stats.ITEM_USED.get(itemStack.getItem()));
                  level.setBlockAndUpdate(blockPos, Blocks.LAVA_CAULDRON.defaultBlockState());
                  level.playSound(null, blockPos, SoundEvents.BUCKET_EMPTY_LAVA, SoundSource.BLOCKS, 1.0F, 1.0F);
                  level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
                  return result(true);
               } else {
                  return result(false);
               }
            } else {
               return result(true);
            }
         });
      CauldronInteraction.EMPTY
         .compute(
            Items.WATER_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                  if (!allow(Fluids.WATER)) {
                     return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
                  } else {
                     return !level.isClientSide()
                        ? fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack, Blocks.WATER_CAULDRON.defaultBlockState())
                        : result(true);
                  }
               }
         );
      CauldronInteraction.WATER
         .compute(
            Items.WATER_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                  if (!allow(Fluids.WATER)) {
                     return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
                  } else {
                     return !level.isClientSide()
                           && fillCauldron(blockState, level, blockPos, player, interactionHand, itemStack, Blocks.WATER_CAULDRON.defaultBlockState()) != result(true)
                        ? emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack, Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL)
                        : result(true);
                  }
               }
         );
      CauldronInteraction.WATER
         .compute(
            Items.LAVA_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow(Fluids.LAVA)
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
      CauldronInteraction.WATER
         .compute(
            Items.POWDER_SNOW_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow()
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
      CauldronInteraction.WATER
         .compute(
            Items.BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> {
                  if (!allow(Fluids.WATER)) {
                     return prev.interact(blockState, level, blockPos, player, interactionHand, itemStack);
                  } else {
                     return !level.isClientSide()
                        ? emptyCauldron(blockState, level, blockPos, player, interactionHand, itemStack, Items.WATER_BUCKET.getDefaultInstance(), SoundEvents.BUCKET_FILL)
                        : result(true);
                  }
               }
         );
      CauldronInteraction.LAVA
         .compute(
            Items.WATER_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow(Fluids.WATER)
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
      CauldronInteraction.LAVA
         .compute(
            Items.POWDER_SNOW_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow()
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
      CauldronInteraction.POWDER_SNOW
         .compute(
            Items.LAVA_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow(Fluids.LAVA)
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
      CauldronInteraction.POWDER_SNOW
         .compute(
            Items.WATER_BUCKET,
            (k, prev) -> (blockState, level, blockPos, player, interactionHand, itemStack) -> !allow(Fluids.WATER)
                     ? prev.interact(blockState, level, blockPos, player, interactionHand, itemStack)
                     : result(false)
         );
   }

   @Unique
   private static boolean isUnderWater(Level arg, BlockPos arg2) {
      FluidState fluidState = arg.getFluidState(arg2.above());
      return fluidState.is(FluidTags.WATER);
   }

   @Unique
   private static boolean allow(Fluid fluid) {
      return allow() && FlowingFluids.config.isFluidAllowed(fluid);
   }

   @Unique
   private static boolean allow() {
      return FlowingFluids.config.enableMod;
   }

   @Unique
   private static InteractionResult result(boolean pass) {
      return pass ? InteractionResult.SUCCESS : InteractionResult.FAIL;
   }

   @Unique
   private static InteractionResult fillCauldron(
      BlockState cauldron,
      Level level,
      BlockPos blockPos,
      Player player,
      InteractionHand interactionHand,
      ItemStack bucket,
      BlockState defaultFilledCauldronState
   ) {
      int bucketAmount = 8 - bucket.getDamageValue();
      int ogAmount = bucketAmount;
      int cauldronNeeds = cauldron.is(Blocks.CAULDRON) ? 3 : 3 - (Integer)cauldron.getValue(LayeredCauldronBlock.LEVEL);
      switch (cauldronNeeds) {
         case 1:
            if (bucketAmount >= 3) {
               bucketAmount -= 3;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
            }
            break;
         case 2:
            if (bucketAmount >= 6) {
               bucketAmount -= 6;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
            } else if (bucketAmount >= 3) {
               bucketAmount -= 3;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 2));
            }
            break;
         case 3:
            if (bucketAmount >= 8) {
               bucketAmount -= 8;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 3));
            } else if (bucketAmount >= 5) {
               bucketAmount -= 5;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 2));
            } else if (bucketAmount >= 2) {
               bucketAmount -= 2;
               level.setBlockAndUpdate(blockPos, (BlockState)defaultFilledCauldronState.setValue(LayeredCauldronBlock.LEVEL, 1));
            }
      }

      System.out
         .println(
            "Bucket amount after: "
               + bucketAmount
               + " (was "
               + ogAmount
               + ") cauldron was "
               + (cauldron.is(Blocks.CAULDRON) ? 0 : (Integer)cauldron.getValue(LayeredCauldronBlock.LEVEL))
               + " level, is now "
               + (level.getBlockState(blockPos).is(Blocks.CAULDRON) ? 0 : (Integer)level.getBlockState(blockPos).getValue(LayeredCauldronBlock.LEVEL))
         );
      if (bucketAmount != ogAmount) {
         player.setItemInHand(interactionHand, ((FFBucketItem)bucket.getItem()).ff$bucketOfAmount(bucket, bucketAmount));
         player.awardStat(Stats.FILL_CAULDRON);
         player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
         level.playSound(null, blockPos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(null, GameEvent.FLUID_PLACE, blockPos);
         return result(true);
      } else {
         return result(false);
      }
   }

   @Unique
   private static InteractionResult emptyCauldron(
      BlockState cauldron,
      Level level,
      BlockPos blockPos,
      Player player,
      InteractionHand interactionHand,
      ItemStack bucket,
      ItemStack defaultFilledBucket,
      SoundEvent soundEvent
   ) {
      int bucketAmount = bucket.is(Items.BUCKET) ? 0 : 8 - bucket.getDamageValue();
      int ogAmount = bucketAmount;
      int cauldronHas = (Integer)cauldron.getValue(LayeredCauldronBlock.LEVEL);
      switch (cauldronHas) {
         case 1:
            if (bucketAmount <= 6) {
               bucketAmount += 2;
               level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
            }
            break;
         case 2:
            if (bucketAmount <= 3) {
               bucketAmount += 5;
               level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
            } else if (bucketAmount <= 6) {
               bucketAmount += 2;
               level.setBlockAndUpdate(blockPos, (BlockState)cauldron.setValue(LayeredCauldronBlock.LEVEL, 1));
            }
            break;
         case 3:
            if (bucketAmount <= 0) {
               bucketAmount += 8;
               level.setBlockAndUpdate(blockPos, Blocks.CAULDRON.defaultBlockState());
            } else if (bucketAmount <= 3) {
               bucketAmount += 5;
               level.setBlockAndUpdate(blockPos, (BlockState)cauldron.setValue(LayeredCauldronBlock.LEVEL, 1));
            } else if (bucketAmount <= 6) {
               bucketAmount += 2;
               level.setBlockAndUpdate(blockPos, (BlockState)cauldron.setValue(LayeredCauldronBlock.LEVEL, 2));
            }
      }

      System.out
         .println(
            "Bucket amount after: "
               + bucketAmount
               + " (was "
               + ogAmount
               + ") cauldron was "
               + cauldronHas
               + " level, is now "
               + (level.getBlockState(blockPos).is(Blocks.CAULDRON) ? 0 : (Integer)level.getBlockState(blockPos).getValue(LayeredCauldronBlock.LEVEL))
         );
      if (bucketAmount != ogAmount) {
         player.setItemInHand(interactionHand, ((FFBucketItem)defaultFilledBucket.getItem()).ff$bucketOfAmount(defaultFilledBucket, bucketAmount));
         player.awardStat(Stats.USE_CAULDRON);
         player.awardStat(Stats.ITEM_USED.get(bucket.getItem()));
         level.playSound(null, blockPos, soundEvent, SoundSource.BLOCKS, 1.0F, 1.0F);
         level.gameEvent(null, GameEvent.FLUID_PICKUP, blockPos);
         return result(true);
      } else {
         return result(false);
      }
   }
}
