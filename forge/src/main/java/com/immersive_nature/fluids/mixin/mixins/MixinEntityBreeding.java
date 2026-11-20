// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package com.immersive_nature.fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.immersive_nature.fluids.FFFluidUtils;
import com.immersive_nature.fluids.FlowingFluids;

@Mixin({BreedGoal.class})
public class MixinEntityBreeding {
   @Shadow
   @Final
   protected Animal animal;
   @Shadow
   @Nullable
   protected Animal partner;

   @Inject(
      method = {"breed"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void ff$drinkToBreed(CallbackInfo ci) {
      if (FlowingFluids.config.enableMod && FlowingFluids.config.drinkWaterToBreedAnimalChance > 0.0F && FlowingFluids.config.isWaterAllowed()) {
         if (this.animal.getRandom().nextFloat() < FlowingFluids.config.drinkWaterToBreedAnimalChance) {
            return;
         }

         Level world = this.animal.level();
         BlockPos pos = this.animal.blockPosition();

         for (BlockPos blockPos2 : BlockPos.betweenClosed(pos.offset(-8, -1, -8), pos.offset(8, 1, 8))) {
            BlockState state = world.getBlockState(blockPos2);
            if (world.getFluidState(blockPos2).is(FluidTags.WATER)) {
               FFFluidUtils.removeAmountFromFluidAtPosWithRemainder(world, blockPos2, Fluids.WATER, 1);
               world.playSound(
                  null, this.animal.getX(), this.animal.getY(), this.animal.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F
               );
               return;
            }

            if (state.is(Blocks.WATER_CAULDRON)) {
               LayeredCauldronBlock.lowerFillLevel(state, world, blockPos2);
               world.playSound(
                  null, this.animal.getX(), this.animal.getY(), this.animal.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1.0F, 1.0F
               );
               return;
            }
         }

         ci.cancel();
         this.animal.setAge(6000);
         this.animal.resetLove();

         assert this.partner != null;

         this.partner.setAge(6000);
         this.partner.resetLove();
         RandomSource rand = this.animal.getRandom();
         if (world instanceof ServerLevel server) {
            for (int i = 0; i < 8; i++) {
               server.sendParticles(
                  new DustParticleOptions(Vec3.fromRGB24(9999746).toVector3f(), 1.0F),
                  (double)pos.getX(),
                  (double)pos.getY(),
                  (double)pos.getZ(),
                  1,
                  (double)((0.5F - rand.nextFloat()) * 3.0F),
                  (double)(rand.nextFloat() * 2.0F),
                  (double)((0.5F - rand.nextFloat()) * 3.0F),
                  1.0
               );
            }

            server.playSound(
               null, this.animal.getX(), this.animal.getY(), this.animal.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.25F, 1.0F
            );
         }
      }
   }
}
