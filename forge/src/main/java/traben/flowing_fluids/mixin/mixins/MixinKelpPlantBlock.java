// Flowing Fluids by Traben (https://github.com/Traben-0/flowing_fluids), LGPL-3.0. Built into Immersive Nature.
package traben.flowing_fluids.mixin.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.GrowingPlantBodyBlock;
import net.minecraft.world.level.block.KelpPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import traben.flowing_fluids.FFFluidUtils;
import traben.flowing_fluids.FlowingFluids;

@Mixin({KelpPlantBlock.class})
public abstract class MixinKelpPlantBlock extends GrowingPlantBodyBlock {
   protected MixinKelpPlantBlock(Properties properties, Direction growthDirection, VoxelShape shape, boolean scheduleFluidTicks) {
      super(properties, growthDirection, shape, scheduleFluidTicks);
   }

   public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
      boolean canSurvive = super.canSurvive(state, level, pos);
      if (canSurvive
         && FlowingFluids.config.enableMod
         && FlowingFluids.config.isWaterAllowed()
         && level instanceof LevelAccessor accessor
         && FFFluidUtils.canFluidFlowToNeighbourFromPos(accessor, pos, Fluids.WATER, 8)) {
         return false;
      }

      return canSurvive;
   }
}
