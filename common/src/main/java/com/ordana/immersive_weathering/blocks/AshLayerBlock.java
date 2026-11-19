package com.ordana.immersive_weathering.blocks;

import com.ordana.immersive_weathering.reg.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class AshLayerBlock extends SandLayerBlock {
   public static final BooleanProperty SMOLDERING = ModBlockProperties.SMOLDERING;

   public AshLayerBlock(Properties properties) {
      super(10129552, properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(SMOLDERING, false));
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder);
      builder.add(new Property[]{SMOLDERING});
   }

   public boolean isRandomlyTicking(BlockState state) {
      return true;
   }

   private static boolean isWet(Level level, BlockPos pos) {
      if (level.isRainingAt(pos.above())) {
         return true;
      } else {
         for (Direction d : Direction.values()) {
            if (d != Direction.UP && level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) {
               return true;
            }
         }

         return false;
      }
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      boolean wet = isWet(level, pos);
      if ((Boolean)state.getValue(SMOLDERING)) {
         if (wet || random.nextInt(3) == 0) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(SMOLDERING, false));
            if (wet) {
               level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F + random.nextFloat() * 0.4F);
            }
         }
      } else if (wet && random.nextInt(4) == 0) {
         this.removeOneLayer(state, pos, level);
      }
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      super.animateTick(state, level, pos, random);
      if ((Boolean)state.getValue(SMOLDERING)) {
         double top = (double)pos.getY() + (double)this.getLayers(state) / 8.0;
         if (random.nextInt(4) == 0) {
            level.addParticle(
               (ParticleOptions)ModParticles.EMBERSPARK.get(),
               (double)pos.getX() + random.nextDouble(),
               top,
               (double)pos.getZ() + random.nextDouble(),
               0.1,
               3.0,
               0.1
            );
         }

         if (random.nextInt(6) == 0) {
            level.addParticle(
               ParticleTypes.SMOKE, (double)pos.getX() + random.nextDouble(), top, (double)pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0
            );
         }

         if (random.nextInt(40) == 0) {
            level.playLocalSound(
               (double)pos.getX() + 0.5,
               top,
               (double)pos.getZ() + 0.5,
               SoundEvents.CAMPFIRE_CRACKLE,
               SoundSource.BLOCKS,
               0.3F + random.nextFloat() * 0.3F,
               random.nextFloat() * 0.7F + 0.6F,
               false
            );
         }
      }
   }

   @Override
   public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
      if ((Boolean)state.getValue(SMOLDERING) && !level.isClientSide && !entity.fireImmune() && entity instanceof LivingEntity le && !EnchantmentHelper.hasFrostWalker(le)) {
         entity.hurt(level.damageSources().hotFloor(), 1.0F);
      }

      super.stepOn(level, pos, state, entity);
   }

   public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
      return 10129552;
   }
}
