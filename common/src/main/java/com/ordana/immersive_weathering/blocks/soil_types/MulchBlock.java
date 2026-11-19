package com.ordana.immersive_weathering.blocks.soil_types;

import com.ordana.immersive_weathering.blocks.ModBlockProperties;
import com.ordana.immersive_weathering.reg.ModTags;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class MulchBlock extends Block implements SimpleWaterloggedBlock {
   public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
   public static final BooleanProperty SOAKED = ModBlockProperties.SOAKED;

   public MulchBlock(Properties settings) {
      super(settings);
      this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(WATERLOGGED, false)).setValue(SOAKED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> stateManager) {
      stateManager.add(new Property[]{WATERLOGGED, SOAKED});
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext context) {
      FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
      boolean bl = fluidState.getType() == Fluids.WATER;
      return (BlockState)Objects.requireNonNull(super.getStateForPlacement(context)).setValue(WATERLOGGED, bl);
   }

   public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
      if ((Boolean)state.getValue(WATERLOGGED)) {
         level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
      }

      return state;
   }

   public FluidState getFluidState(BlockState state) {
      return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
   }

   public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Block.box(0.0, 0.0, 0.0, 16.0, 17.0, 16.0);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
      return Shapes.block();
   }

   public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
      if (((Boolean)state.getValue(WATERLOGGED) || (Boolean)state.getValue(SOAKED)) && random.nextInt(25) == 1) {
         BlockPos blockpos = pos.below();
         BlockState blockstate = level.getBlockState(blockpos);
         if (!blockstate.canOcclude() || !blockstate.isFaceSturdy(level, blockpos, Direction.UP)) {
            double d0 = (double)pos.getX() + random.nextDouble();
            double d1 = (double)pos.getY() - 0.05;
            double d2 = (double)pos.getZ() + random.nextDouble();
            level.addParticle(ParticleTypes.DRIPPING_WATER, d0, d1, d2, 0.0, 0.0, 0.0);
         }
      }
   }

   public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
      entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall());
   }

   public boolean isRandomlyTicking(BlockState state) {
      return true;
   }

   public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
      if (!(Boolean)state.getValue(WATERLOGGED)) {
         boolean wet = level.isRainingAt(pos.above());
         boolean hot = level.dimensionType().ultraWarm();
         Holder<Biome> biome = level.getBiome(pos);
         if (biome.is(ModTags.WET)) {
            wet = true;
         }

         if (biome.is(ModTags.HOT)) {
            hot = true;
         }

         for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (!n.getFluidState().is(FluidTags.WATER) && !level.isRainingAt(pos.relative(d))) {
               if (n.is(ModTags.MAGMA_SOURCE) || n.getFluidState().is(FluidTags.LAVA)) {
                  hot = true;
               }
            } else {
               wet = true;
            }
         }

         boolean soaked = (Boolean)state.getValue(SOAKED);
         if (wet && !soaked) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(SOAKED, true));
         } else if (soaked && !wet && (hot || random.nextInt(5) == 0)) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(SOAKED, false));
         }
      }
   }
}
