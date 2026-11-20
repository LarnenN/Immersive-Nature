package com.immersive_nature.forge;

import com.google.common.collect.ImmutableBiMap;
import com.immersive_nature.integrations.IntegrationHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.FlowingFluid;

public class NaturePlatformImpl {

    public static void addExtraMossyBlocks(ImmutableBiMap.Builder<Block, Block> builder) {
        if (IntegrationHandler.quark) {
        }
    }

    public static void addExtraCrackedBlocks(ImmutableBiMap.Builder<Block, Block> builder) {
        if (IntegrationHandler.quark) {
        }
    }

    public static FlowingFluid getFlowingFluid(LiquidBlock block) {
        return block.getFluid();
    }


    public static void addFeatureToBiome(GenerationStep.Decoration step, TagKey<Biome> tagKey, ResourceKey<PlacedFeature> feature) {
    }


}
