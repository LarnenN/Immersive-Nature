package com.immersive_nature.snow.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

/**
 * Collects the sub-models that make up a snow-covered block, so that they can be
 * emitted through the regular Forge getQuads path (no Fabric renderer needed).
 */
public class PartCollector implements RenderAPI {

	public record Part(BlockState state, BlockPos pos, BakedModel model, double yOffset) {
	}

	private final BlockState selfState;
	private final BakedModel unwrapped;
	private final ModelData data;
	public final List<Part> parts = new ArrayList<>(3);

	public PartCollector(BlockState selfState, BakedModel unwrapped, ModelData data) {
		this.selfState = selfState;
		this.unwrapped = unwrapped;
		this.data = data;
	}

	@Override
	public boolean translateYAndRender(
			BlockAndTintGetter world,
			BlockState state,
			BlockPos pos,
			@Nullable RenderType layer,
			Supplier<RandomSource> randomSupplier,
			boolean cullSides,
			BakedModel model,
			double yOffset) {
		if (layer != null && !model.getRenderTypes(state, randomSupplier.get(), data).contains(layer)) {
			return false;
		}
		if (state == selfState && model != SnowClient.cachedOverlayModel) {
			model = unwrapped;
		}
		parts.add(new Part(state, pos, model, yOffset));
		return true;
	}

}
