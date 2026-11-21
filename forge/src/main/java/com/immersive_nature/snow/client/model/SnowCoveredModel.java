package com.immersive_nature.snow.client.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import com.immersive_nature.snow.block.entity.SnowBlockEntity;
import com.immersive_nature.snow.client.PartCollector;
import com.immersive_nature.snow.client.SnowClient;

/**
 * Renders a snow-covered block (camouflaged block + snow + overlay) with the plain Forge model API.
 */
public class SnowCoveredModel extends BakedModelWrapper<BakedModel> {

	private static final ModelProperty<BlockAndTintGetter> LEVEL = new ModelProperty<>();
	private static final ModelProperty<BlockPos> POS = new ModelProperty<>();
	private static final int STRIDE = 8; // DefaultVertexFormat.BLOCK, in ints
	private static final int COLOR = 3;

	/** The renderer asks for the 6 sides + null of one layer in a row, so the part list is reused. */
	private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);

	private static final class Cache {
		ModelData data;
		RenderType layer;
		List<PartCollector.Part> parts;
	}

	public SnowCoveredModel(BakedModel model) {
		super(model);
	}

	@Override
	public @NotNull ModelData getModelData(
			@NotNull BlockAndTintGetter level,
			@NotNull BlockPos pos,
			@NotNull BlockState state,
			@NotNull ModelData modelData) {
		return modelData.derive().with(LEVEL, level).with(POS, pos.immutable()).build();
	}

	private List<PartCollector.Part> parts(BlockState state, ModelData data, @Nullable RenderType layer) {
		Cache cache = CACHE.get();
		if (cache.data == data && cache.layer == layer && cache.parts != null) {
			return cache.parts;
		}
		BlockAndTintGetter level = data.get(LEVEL);
		BlockPos pos = data.get(POS);
		List<PartCollector.Part> parts;
		if (level == null || pos == null) {
			parts = List.of();
		} else {
			PartCollector collector = new PartCollector(state, originalModel, data);
			long seed = state.getSeed(pos);
			SnowClient.renderHook(
					level,
					pos,
					state,
					data.has(SnowBlockEntity.BLOCKSTATE) ? data.get(SnowBlockEntity.BLOCKSTATE) : Blocks.AIR.defaultBlockState(),
					data.has(SnowBlockEntity.OPTIONS) ? data.get(SnowBlockEntity.OPTIONS) : SnowClient.fallbackOptions,
					layer,
					() -> RandomSource.create(seed),
					true,
					collector);
			parts = collector.parts;
		}
		cache.data = data;
		cache.layer = layer;
		cache.parts = parts;
		return parts;
	}

	@Override
	public @NotNull ChunkRenderTypeSet getRenderTypes(
			@NotNull BlockState state,
			@NotNull RandomSource rand,
			@NotNull ModelData data) {
		List<PartCollector.Part> parts = parts(state, data, null);
		if (parts.isEmpty()) {
			return ChunkRenderTypeSet.none();
		}
		List<ChunkRenderTypeSet> sets = new ArrayList<>(parts.size());
		for (PartCollector.Part part : parts) {
			sets.add(part.model().getRenderTypes(part.state(), RandomSource.create(part.state().getSeed(part.pos())), data));
		}
		return ChunkRenderTypeSet.union(sets);
	}

	@Override
	public @NotNull List<BakedQuad> getQuads(
			@Nullable BlockState state,
			@Nullable Direction side,
			@NotNull RandomSource rand,
			@NotNull ModelData data,
			@Nullable RenderType renderType) {
		if (state == null) {
			return super.getQuads(null, side, rand, data, renderType);
		}
		List<PartCollector.Part> parts = parts(state, data, renderType);
		if (parts.isEmpty()) {
			return List.of();
		}
		BlockAndTintGetter level = data.get(LEVEL);
		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		List<BakedQuad> out = new ArrayList<>();
		for (PartCollector.Part part : parts) {
			BlockState partState = part.state();
			double yOffset = part.yOffset();
			if (side == Direction.DOWN && yOffset != 0 && partState.is(Blocks.SNOW)) { // snow on a slab
				continue;
			}
			RandomSource random = RandomSource.create(partState.getSeed(part.pos()));
			List<BakedQuad> quads = part.model().getQuads(partState, side, random, data, renderType);
			if (quads.isEmpty()) {
				continue;
			}
			Vec3 offset = partState.getOffset(level, part.pos());
			if (yOffset != 0) {
				offset = offset.add(0, yOffset, 0);
			}
			boolean moved = offset != Vec3.ZERO;
			for (BakedQuad quad : quads) {
				int color = -1;
				if (quad.getTintIndex() != -1) {
					color = blockColors.getColor(partState, level, part.pos(), quad.getTintIndex()) | 0xFF000000;
				}
				if (!moved && color == -1) {
					out.add(quad);
					continue;
				}
				int[] vertices = quad.getVertices().clone();
				// ARGB -> ABGR, as stored in the block vertex format
				int abgr = (color & 0xFF00FF00) | ((color >> 16) & 0xFF) | ((color & 0xFF) << 16);
				for (int i = 0; i < 4; ++i) {
					int base = i * STRIDE;
					vertices[base] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base]) + (float) offset.x);
					vertices[base + 1] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 1]) + (float) offset.y);
					vertices[base + 2] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[base + 2]) + (float) offset.z);
					vertices[base + COLOR] = abgr;
				}
				out.add(new BakedQuad(vertices, -1, quad.getDirection(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion()));
			}
		}
		return out;
	}

	@Override
	public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
		return getQuads(state, side, rand, ModelData.EMPTY, null);
	}

	@Override
	public ItemTransforms getTransforms() {
		return ItemTransforms.NO_TRANSFORMS;
	}

}
