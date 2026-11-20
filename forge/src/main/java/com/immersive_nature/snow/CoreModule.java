package com.immersive_nature.snow;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.kiwi.AbstractModule;
import snownee.kiwi.KiwiGO;
import snownee.kiwi.KiwiModule;
import snownee.kiwi.KiwiModule.Name;
import snownee.kiwi.KiwiModule.NoItem;
import snownee.kiwi.loader.event.ClientInitEvent;
import snownee.kiwi.loader.event.InitEvent;
import snownee.kiwi.util.EnumUtil;
import com.immersive_nature.snow.block.EntitySnowLayerBlock;
import com.immersive_nature.snow.block.SnowFenceBlock;
import com.immersive_nature.snow.block.SnowFenceGateBlock;
import com.immersive_nature.snow.block.SnowSlabBlock;
import com.immersive_nature.snow.block.SnowStairsBlock;
import com.immersive_nature.snow.block.SnowWallBlock;
import com.immersive_nature.snow.block.entity.SnowBlockEntity;
import com.immersive_nature.snow.block.entity.SnowCoveredBlockEntity;
import com.immersive_nature.snow.entity.FallingSnowEntity;
import com.immersive_nature.snow.loot.NormalizeLoot;
import com.immersive_nature.snow.mixin.BlockAccess;

@KiwiModule
public class CoreModule extends AbstractModule {

	public static final TagKey<Block> SNOWY_SETTING = blockTag(SnowRealMagic.MODID, "snowy_setting");

	public static final TagKey<Block> CONTAINABLES = blockTag(SnowRealMagic.MODID, "containables");

	public static final TagKey<Block> NOT_CONTAINABLES = blockTag(SnowRealMagic.MODID, "not_containables");

	public static final TagKey<Block> OFFSET_Y = blockTag(SnowRealMagic.MODID, "offset_y");

	public static final TagKey<Block> CANNOT_ACCUMULATE_ON = blockTag(SnowRealMagic.MODID, "cannot_accumulate_on");

	public static final TagKey<Block> ENTITY_INSIDE = blockTag(SnowRealMagic.MODID, "entity_inside");

	@NoItem
	@Name("snow_layer")
	public static final KiwiGO<EntitySnowLayerBlock> TILE_BLOCK = go(() -> new EntitySnowLayerBlock(blockProp(Blocks.SNOW).dynamicShape()));

	@NoItem
	@Name("snowy_fence")
	public static final KiwiGO<Block> FENCE = go(() -> new SnowFenceBlock(blockProp(Blocks.OAK_FENCE).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@NoItem
	@Name("snowy_fence2")
	public static final KiwiGO<Block> FENCE2 = go(() -> new SnowFenceBlock(blockProp(Blocks.NETHER_BRICK_FENCE).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@NoItem
	@Name("snowy_stairs")
	public static final KiwiGO<Block> STAIRS = go(() -> new SnowStairsBlock(blockProp(Blocks.OAK_STAIRS).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@NoItem
	@Name("snowy_slab")
	public static final KiwiGO<Block> SLAB = go(() -> new SnowSlabBlock(blockProp(Blocks.OAK_SLAB).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@NoItem
	@Name("snowy_fence_gate")
	public static final KiwiGO<Block> FENCE_GATE = go(() -> new SnowFenceGateBlock(blockProp(Blocks.OAK_FENCE_GATE).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@NoItem
	@Name("snowy_wall")
	public static final KiwiGO<Block> WALL = go(() -> new SnowWallBlock(blockProp(Blocks.COBBLESTONE_WALL).mapColor(MapColor.SNOW)
			.sound(SoundType.SNOW)
			.randomTicks()));

	@Name("snow_layer")
	public static final KiwiGO<BlockEntityType<SnowBlockEntity>> TILE = blockEntity(SnowBlockEntity::new, null, TILE_BLOCK);

	@Name("snow_covered_block")
	public static final KiwiGO<BlockEntityType<SnowCoveredBlockEntity>> TEXTURE_TILE = blockEntity(
			SnowCoveredBlockEntity::new,
			null,
			FENCE,
			FENCE2,
			STAIRS,
			SLAB,
			FENCE_GATE,
			WALL);

	@Name("falling_snow")
	public static final KiwiGO<EntityType<FallingSnowEntity>> ENTITY = go(() -> EntityType.Builder.<FallingSnowEntity>of(
					FallingSnowEntity::new,
					MobCategory.MISC)
			.setCustomClientFactory((spawnEntity, world) -> new FallingSnowEntity(world))
			.sized(0.98F, 0.001F)
			.clientTrackingRange(10)
			.updateInterval(20)
			.build(SnowRealMagic.MODID + ".falling_snow"));

	@Name("snow_normalize")
	public static final KiwiGO<LootPoolEntryType> NORMALIZE = go(() -> new LootPoolEntryType(new NormalizeLoot.Serializer()));

	public static final GameRules.Key<IntegerValue> BLIZZARD_STRENGTH = GameRules.register(
			SnowRealMagic.MODID + ":blizzardStrength",
			GameRules.Category.MISC,
			IntegerValue.create(0));

	public static final GameRules.Key<IntegerValue> BLIZZARD_FREQUENCY = GameRules.register(
			SnowRealMagic.MODID + ":blizzardFrequency",
			GameRules.Category.MISC,
			IntegerValue.create(10000));

	public CoreModule() {
		decorators.remove(ForgeRegistries.BLOCKS);
	}

	@Override
	protected void init(InitEvent event) {
		event.enqueueWork(() -> {
			BlockBehaviour.StateArgumentPredicate<EntityType<?>> predicate = (blockState, blockGetter, blockPos, entityType) -> {
				final var below = blockPos.below();
				return blockState.getValue(BlockStateProperties.LAYERS) <= SnowCommonConfig.mobSpawningMaxLayers &&
						blockGetter.getBlockState(below).isValidSpawn(blockGetter, below, entityType);
			};
			((BlockAccess) Blocks.SNOW).getProperties().isValidSpawn(predicate);
			((BlockAccess) TILE_BLOCK.get()).getProperties().isValidSpawn(predicate);
		});
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	protected void clientInit(ClientInitEvent event) {
		Predicate<RenderType> blockRenderTypes = EnumUtil.BLOCK_RENDER_TYPES::contains;
		for (Supplier<? extends Block> block : List.of(TILE_BLOCK, FENCE, FENCE2, FENCE_GATE, SLAB, STAIRS, WALL))
			ItemBlockRenderTypes.setRenderLayer(block.get(), blockRenderTypes);
	}


}
