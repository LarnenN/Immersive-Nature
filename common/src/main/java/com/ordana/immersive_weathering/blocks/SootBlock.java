package com.ordana.immersive_weathering.blocks;

import com.ordana.immersive_weathering.reg.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class SootBlock extends MultifaceBlock {
   private final MultifaceSpreader spreader = new MultifaceSpreader(this);

   public SootBlock(Properties settings) {
      super(settings);
      this.registerDefaultState(this.defaultBlockState());
   }

   public MultifaceSpreader getSpreader() {
      return this.spreader;
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (!level.isRainingAt(pos)) {
         if (random.nextInt(50) == 0) {
            level.addParticle(
               (ParticleOptions)ModParticles.EMBERSPARK.get(),
               (double)pos.getX() + random.nextDouble(),
               (double)pos.getY() + random.nextDouble(),
               (double)pos.getZ() + random.nextDouble(),
               0.1,
               3.0,
               0.1
            );
         }

         if (random.nextInt(30) == 0) {
            level.addParticle(
               ParticleTypes.SMOKE,
               (double)pos.getX() + random.nextDouble(),
               (double)pos.getY() + random.nextDouble(),
               (double)pos.getZ() + random.nextDouble(),
               0.0,
               0.01,
               0.0
            );
         }
      }
   }
}
