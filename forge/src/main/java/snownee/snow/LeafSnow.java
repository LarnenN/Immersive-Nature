// Immersive Nature addition to Snow! Real Magic (MIT, Snownee): snow and fallen leaves.
package snownee.snow;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.snow.block.SnowVariant;

/**
 * Snow replaces fallen leaf piles (Immersive Nature's and other mods') instead of
 * merging into them: placing snow removes the pile, and during snowfall piles
 * gradually turn into snow.
 */
public final class LeafSnow {
	public static final float CONVERT_CHANCE = 0.5F;
	private static TagKey<Block> tag;

	private LeafSnow() {
	}

	private static TagKey<Block> tag() {
		if (tag == null) {
			tag = CoreModule.blockTag(SnowRealMagic.MODID, "replaced_by_snow");
		}
		return tag;
	}

	public static boolean isLeafPile(BlockState state) {
		if (state == null) {
			return false;
		}
		Block block = state.getBlock();
		if (block == null || block instanceof SnowVariant) {
			return false;
		}
		if (state.is(tag())) {
			return true;
		}
		ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
		if (id == null) {
			return false;
		}
		String name = id.toString();
		return name.contains("leaf_pile") || name.contains("leaves_pile") || name.contains("fallen_leaves")
				|| name.contains("leaf_litter") || name.contains("leaf_carpet");
	}

	private static void removeLeaves(Level level, BlockPos pos) {
		level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
	}

	/**
	 * Called before snow layers are placed at pos.
	 * @return true if snow must not be placed there
	 */
	public static boolean prePlace(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (isLeafPile(state)) {
			if (!Hooks.canSnowSurvive(Blocks.SNOW.defaultBlockState(), level, pos)) {
				return true;
			}
			removeLeaves(level, pos);
			return false;
		}
		return Hooks.isForeignLayered(state);
	}

	/**
	 * Snowfall tick: a leaf pile at pos (or right above it) may turn into a snow layer.
	 * @return true if the tick was handled here
	 */
	public static boolean snowTick(ServerLevel level, BlockPos pos) {
		if (!level.isRaining() || SnowCommonConfig.snowAccumulationMaxLayers <= 0) {
			return false;
		}
		BlockPos target = null;
		if (isLeafPile(level.getBlockState(pos))) {
			target = pos;
		} else {
			BlockPos above = pos.above();
			if (isLeafPile(level.getBlockState(above))) {
				target = above;
			}
		}
		if (target == null) {
			return false;
		}
		if (level.random.nextFloat() > CONVERT_CHANCE) {
			return true;
		}
		if (!Hooks.canSnowSurvive(Blocks.SNOW.defaultBlockState(), level, target)) {
			return true;
		}
		removeLeaves(level, target);
		Hooks.placeNormalSnow(level, target, 1, 3);
		return true;
	}
}
