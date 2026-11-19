// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import traben.flowing_fluids.FFBucketItem;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin({BucketItem.class})
public abstract class MixinBucketItem extends Item implements FFBucketItem {
   @Shadow
   @Final
   private Fluid content;

   public MixinBucketItem(Properties properties) {
      super(properties);
   }

   @Shadow
   public static ItemStack getEmptySuccessItem(ItemStack itemStack, Player player) {
      return null;
   }

   @Shadow
   public abstract void checkExtraContent(@Nullable Player var1, Level var2, ItemStack var3, BlockPos var4);

   @Shadow
   protected abstract void playEmptySound(@Nullable Player var1, LevelAccessor var2, BlockPos var3);

   @ModifyArg(
      method = {"use"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/BucketItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"
      ),
      index = 2
   )
   private net.minecraft.world.level.ClipContext.Fluid flowing_fluids$allowAnyFluid(net.minecraft.world.level.ClipContext.Fluid par3) {
      return FlowingFluids.config.enableMod && par3 == net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY
         ? net.minecraft.world.level.ClipContext.Fluid.ANY
         : par3;
   }

   @Inject(
      method = {"use"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void flowing_fluids$alterBehaviourIfPartial(
      Level level, Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir
   ) {
      if (FlowingFluids.config.enableMod && this.content instanceof FlowingFluid && FlowingFluids.config.isFluidAllowed(this.content)) {
         ItemStack heldBucket = player.getItemInHand(interactionHand);
         BlockHitResult blockHitResult = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.ANY);
         if (blockHitResult.getType() != Type.MISS && blockHitResult.getType() == Type.BLOCK) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            BlockPos blockPos2 = blockPos.relative(direction);
            if (level.mayInteract(player, blockPos) && player.mayUseItemAt(blockPos2, direction, heldBucket)) {
               BlockState blockState = level.getBlockState(blockPos);
               FluidState fluidState = level.getFluidState(blockPos);
               BlockPos blockPos3 = (!(blockState.getBlock() instanceof LiquidBlockContainer) || this.content != Fluids.WATER)
                     && (!this.content.isSame(fluidState.getType()) || fluidState.getAmount() >= 8)
                  ? blockPos2
                  : blockPos;
               int amount = 8 - heldBucket.getDamageValue();
               int remainder = this.ff$emptyContents_AndGetRemainder(player, level, blockPos3, blockHitResult, amount, false);
               if (remainder != amount) {
                  this.checkExtraContent(player, level, heldBucket, blockPos3);
                  if (player instanceof ServerPlayer) {
                     CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, blockPos3, heldBucket);
                  }

                  player.awardStat(Stats.ITEM_USED.get(this));
                  ItemStack resultBucket;
                  if (remainder == 0) {
                     resultBucket = getEmptySuccessItem(heldBucket, player);
                  } else {
                     resultBucket = this.ff$bucketOfAmount(heldBucket, remainder);
                  }

                  ItemStack itemStack2 = ItemUtils.createFilledResult(heldBucket, player, resultBucket);
                  cir.setReturnValue(InteractionResultHolder.sidedSuccess(itemStack2, level.isClientSide()));
               } else {
                  cir.setReturnValue(InteractionResultHolder.fail(heldBucket));
               }
            } else {
               cir.setReturnValue(InteractionResultHolder.fail(heldBucket));
            }
         } else {
            cir.setReturnValue(InteractionResultHolder.pass(heldBucket));
         }
      }
   }

   @Override
   public int ff$emptyContents_AndGetRemainder(
      @Nullable Player player, Level level, BlockPos blockPos, @Nullable BlockHitResult blockHitResult, int amount, boolean onlyModifyThatBlock
   ) {
      if (this.content instanceof FlowingFluid flowingFluid && FlowingFluids.config.isFluidAllowed(this.content)) {
         BlockState state = level.getBlockState(blockPos);
         FluidState fluidState = level.getFluidState(blockPos);
         boolean fluidIsSameAsContent = this.content.isSame(fluidState.getType());
         boolean canPlaceLiquidInPos = state.canBeReplaced(this.content) || state.isAir() || fluidIsSameAsContent;
         if (!canPlaceLiquidInPos && state.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(level, blockPos, state, this.content)) {
            if (amount != 8) {
               return amount;
            }

            container.placeLiquid(level, blockPos, level.getBlockState(blockPos), flowingFluid.getSource(false));
            this.playEmptySound(player, level, blockPos);
            return 0;
         }

         if (!canPlaceLiquidInPos) {
            if (blockHitResult == null) {
               return amount;
            }

            return this.ff$emptyContents_AndGetRemainder(
               player, level, blockHitResult.getBlockPos().relative(blockHitResult.getDirection()), null, amount, onlyModifyThatBlock
            );
         }

         if (FFFluidUtils.dimensionEvaporatesWaterVanilla(level) && this.content.isSame(Fluids.WATER)) {
            int i = blockPos.getX();
            int j = blockPos.getY();
            int k = blockPos.getZ();
            level.playSound(
               player, blockPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F
            );

            for (int l = 0; l < 8; l++) {
               level.addParticle(ParticleTypes.LARGE_SMOKE, (double)i + Math.random(), (double)j + Math.random(), (double)k + Math.random(), 0.0, 0.0, 0.0);
            }

            return 0;
         }

         if (!level.isClientSide() && level.getBlockState(blockPos).canBeReplaced(this.content) && !level.getBlockState(blockPos).liquid()) {
            level.destroyBlock(blockPos, true);
         }

         boolean success;
         int remainder;
         if (fluidIsSameAsContent) {
            int levelAtBlock = fluidState.getAmount();
            int total = levelAtBlock + amount;
            if (total > 8) {
               if (onlyModifyThatBlock) {
                  success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(this.content, 8), 11);
                  remainder = total - 8;
               } else {
                  remainder = FFFluidUtils.addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(level, blockPos, flowingFluid, amount);
                  success = remainder != amount;
               }
            } else {
               success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(this.content, total), 11);
               remainder = 0;
            }
         } else {
            success = level.setBlock(blockPos, FFFluidUtils.getBlockForFluidByAmount(this.content, amount), 11);
            remainder = 0;
         }

         if (success) {
            this.playEmptySound(player, level, blockPos);
         }

         return success ? remainder : amount;
      }

      return amount;
   }

   @Override
   public ItemStack ff$bucketOfAmount(ItemStack originalItemData, int amount) {
      if (amount == 0) {
         return Items.BUCKET.getDefaultInstance();
      } else if (!FlowingFluids.config.isFluidAllowed(this.content)) {
         return originalItemData;
      } else {
         ItemStack resultBucket = originalItemData.copy();
         resultBucket.setDamageValue(8 - amount);
         return resultBucket;
      }
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/level/material/Fluid;Lnet/minecraft/world/item/Item$Properties;)V"},
      at = {@At("TAIL")}
   )
   public void getMaxDamage(CallbackInfo ci) {
      this.maxDamage = 8;
   }

   public int getBarColor(ItemStack itemStack) {
      return FlowingFluids.config.enableMod && FlowingFluids.config.isFluidAllowed(this.content)
         ? this.content.defaultFluidState().createLegacyBlock().getMapColor(null, null).col
         : super.getBarColor(itemStack);
   }

   @Override
   public Fluid ff$getFluid() {
      return this.content;
   }
}
