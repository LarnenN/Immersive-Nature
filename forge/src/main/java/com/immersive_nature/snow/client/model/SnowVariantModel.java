package com.immersive_nature.snow.client.model;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import com.immersive_nature.snow.CoreModule;
import com.immersive_nature.snow.block.entity.SnowBlockEntity;
import com.immersive_nature.snow.client.SnowClientConfig;

public class SnowVariantModel extends BakedModelWrapper<BakedModel> {

	public static final ModelData USE_SNOW_VARIANT = ModelData.builder().build();
	private final BakedModel variantModel;

	public SnowVariantModel(BakedModel model, BakedModel variantModel) {
		super(model);
		this.variantModel = variantModel;
	}

	private BakedModel pick(@Nullable BlockState state, ModelData data) {
		if (state != null && SnowClientConfig.snowVariants && (data == USE_SNOW_VARIANT || data.has(SnowBlockEntity.OPTIONS))) {
			return variantModel;
		}
		return originalModel;
	}

	@Override
	public @NotNull List<BakedQuad> getQuads(
			@Nullable BlockState state,
			@Nullable Direction side,
			@NotNull RandomSource rand,
			@NotNull ModelData data,
			@Nullable RenderType renderType) {
		return pick(state, data).getQuads(state, side, rand, data, renderType);
	}

	@Override
	public @NotNull ChunkRenderTypeSet getRenderTypes(
			@NotNull BlockState state,
			@NotNull RandomSource rand,
			@NotNull ModelData data) {
		return pick(state, data).getRenderTypes(state, rand, data);
	}

	@Override
	public @NotNull ModelData getModelData(
			@NotNull BlockAndTintGetter blockView,
			@NotNull BlockPos pos,
			@NotNull BlockState state,
			@NotNull ModelData modelData) {
		if (SnowClientConfig.snowVariants && modelData != SnowVariantModel.USE_SNOW_VARIANT) {
			if (modelData.has(SnowBlockEntity.OPTIONS)) {
				return USE_SNOW_VARIANT;
			}
			if (state.hasProperty(DoublePlantBlock.HALF) && CoreModule.TILE_BLOCK.is(blockView.getBlockState(pos.below()))) {
				return USE_SNOW_VARIANT;
			}
		}
		return super.getModelData(blockView, pos, state, modelData);
	}
}
