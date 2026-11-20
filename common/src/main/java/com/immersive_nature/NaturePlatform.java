package com.immersive_nature;

import com.google.common.collect.ImmutableBiMap;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.FlowingFluid;
import net.mehvahdjukaar.moonlight.api.platform.configs.ConfigBuilder;
import org.jetbrains.annotations.Contract;

public class NaturePlatform {

    @Contract
    @ExpectPlatform
    public static FlowingFluid getFlowingFluid(LiquidBlock liquidBlock) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addFeatureToBiome(GenerationStep.Decoration step, TagKey<Biome> tagKey, ResourceKey<PlacedFeature> feature) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addExtraMossyBlocks(ImmutableBiMap.Builder<Block, Block> builder) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addExtraCrackedBlocks(ImmutableBiMap.Builder<Block, Block> builder) {
        throw new AssertionError();
    }

    // config sections of the platform-only parts (overgrowth, snow, fluids) in the one Immersive Nature config
    @ExpectPlatform
    public static void addCommonConfigs(ConfigBuilder builder) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void applyCommonConfigs() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void addClientConfigs(ConfigBuilder builder) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void applyClientConfigs() {
        throw new AssertionError();
    }
}
