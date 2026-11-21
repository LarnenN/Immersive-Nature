package com.immersive_nature.snow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.immersive_nature.snow.CoreModule;
import com.immersive_nature.snow.Hooks;
import com.immersive_nature.erosion.WaterSoaking;
import com.immersive_nature.util.WeatheringHelper;
import net.minecraft.tags.FluidTags;

@Mixin(SpreadingSnowyDirtBlock.class)
public abstract class SpreadableSnowyDirtBlockMixin {

	@SuppressWarnings("deprecation")
	@Inject(at = @At("HEAD"), method = "randomTick", cancellable = true)
	private void srm_randomTickProxy(BlockState blockState, ServerLevel level, BlockPos blockPos, RandomSource random, CallbackInfo ci) {
		if (!canBeGrass(blockState, level, blockPos)) {
			if (!level.isAreaLoaded(blockPos, 3)) {
				return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
			}
			// Immersive Nature: grass drowned under water picks dirt / coarse dirt / mud by the water_soaking rules
			BlockState dead = Blocks.DIRT.defaultBlockState();
			if (level.getFluidState(blockPos.above()).is(FluidTags.WATER)) {
				BlockState soaked = WaterSoaking.drownedGrassResult(blockState, level.random);
				if (soaked != null) dead = soaked;
			}
			level.setBlockAndUpdate(blockPos, dead);
		} else {
			if (level.getMaxLocalRawBrightness(blockPos.above()) >= 9) {
				BlockState blockstate = ((Block) (Object) this).defaultBlockState();

				for (int i = 0; i < 4; ++i) {
					BlockPos blockPos2 = blockPos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
					BlockState target = level.getBlockState(blockPos2);
					BlockState result = null;
					if (target.is(Blocks.DIRT)) {
						result = blockstate;
					} else if (blockstate.is(Blocks.GRASS_BLOCK)) {
						// Immersive Nature: grass also spreads onto silt, sandy dirt, earthen clay, permafrost and rooted dirt
						result = WeatheringHelper.getGrassySoil(target).orElse(null);
					}
					if (result != null && canPropagate(blockstate, level, blockPos2)) {
						if (result.hasProperty(SnowyDirtBlock.SNOWY)) {
							result = result.setValue(SnowyDirtBlock.SNOWY, level.getBlockState(blockPos2.above()).is(CoreModule.SNOWY_SETTING));
						}
						level.setBlockAndUpdate(blockPos2, result);
					}
				}
			}

		}
		ci.cancel();
	}

	@Shadow
	public static boolean canBeGrass(BlockState p_220256_0_, LevelReader p_220256_1_, BlockPos p_220256_2_) {
		throw new IllegalStateException();
	}

	@Inject(at = @At("HEAD"), method = "canBeGrass", cancellable = true)
	private static void srm_canBeGrass(
			BlockState state,
			LevelReader viewableWorld,
			BlockPos blockPos,
			CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(Hooks.canGrassSurvive(state, viewableWorld, blockPos));
	}

	@Shadow
	public static boolean canPropagate(BlockState p_56828_, LevelReader p_56829_, BlockPos p_56830_) {
		throw new IllegalStateException();
	}
}
