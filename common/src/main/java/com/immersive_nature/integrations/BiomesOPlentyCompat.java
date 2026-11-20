package com.immersive_nature.integrations;

import com.immersive_nature.blocks.LeafPileBlock;
import com.immersive_nature.reg.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Immersive Nature compat for Biomes O' Plenty: its worldgen leaf piles (biomesoplenty:*_leaf_pile)
 * become Immersive Nature leaf piles of the matching leaves (biomesoplenty:*_leaves).
 */
public final class BiomesOPlentyCompat {

    private static Map<Block, Block> pileToOurs = null;

    private BiomesOPlentyCompat() {
    }

    private static Map<Block, Block> map() {
        if (pileToOurs == null) {
            Map<Block, Block> m = new IdentityHashMap<>();
            ModBlocks.LEAF_PILES.forEach((type, ours) -> {
                ResourceLocation leaves = BuiltInRegistries.BLOCK.getKey(type.leaves);
                if (!leaves.getNamespace().equals("biomesoplenty") || !leaves.getPath().endsWith("_leaves")) return;
                ResourceLocation theirPile = new ResourceLocation("biomesoplenty",
                        leaves.getPath().substring(0, leaves.getPath().length() - "_leaves".length()) + "_leaf_pile");
                BuiltInRegistries.BLOCK.getOptional(theirPile).ifPresent(b -> m.put(b, ours));
            });
            pileToOurs = m;
        }
        return pileToOurs;
    }

    /** Returns our leaf pile (1-3 layers) for a Biomes O' Plenty leaf pile, or the state unchanged. */
    public static BlockState replaceLeafPile(BlockState state, RandomSource random) {
        Block ours = map().get(state.getBlock());
        if (ours == null) return state;
        return ours.defaultBlockState().setValue(LeafPileBlock.LAYERS, 1 + random.nextInt(3));
    }
}
