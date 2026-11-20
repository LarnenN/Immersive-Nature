package com.immersive_nature.reg;

import com.immersive_nature.NaturePlatform;
import com.immersive_nature.ImmersiveNature;
import com.immersive_nature.features.IcicleClusterFeature;
import com.immersive_nature.features.IcicleClusterFeatureConfig;
import net.mehvahdjukaar.moonlight.api.platform.RegHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.function.Supplier;

public class ModFeatures {

    public static final Supplier<Feature<IcicleClusterFeatureConfig>> ICICLE_FEATURE = RegHelper.registerFeature(
            ImmersiveNature.res("icicle_cluster"), () ->
                    new IcicleClusterFeature(IcicleClusterFeatureConfig.CODEC));

    public static void init() {

        //icy
        ResourceKey<PlacedFeature> icicles = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("icicles"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ModTags.ICY, icicles);

        ResourceKey<PlacedFeature> frost_patch = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("frost_patch"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.TOP_LAYER_MODIFICATION, ModTags.ICY, frost_patch);


        //soil patches
        ResourceKey<PlacedFeature> loam = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("loam"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_LOAM, loam);

        ResourceKey<PlacedFeature> silt = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("silt"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_SILT, silt);

        ResourceKey<PlacedFeature> silt_aquifer = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("silt_aquifer"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, BiomeTags.IS_OVERWORLD, silt_aquifer);

        ResourceKey<PlacedFeature> sandy_dirt = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("sandy_dirt"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_SANDY_DIRT, sandy_dirt);

        ResourceKey<PlacedFeature> earthen_clay = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("earthen_clay"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_EARTHEN_CLAY, earthen_clay);

        ResourceKey<PlacedFeature> permafrost = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("permafrost"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_PERMAFROST, permafrost);


        //dry lakebed
        ResourceKey<PlacedFeature> dry_lakebed = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("dry_lakebed"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_LAKEBED, dry_lakebed);

        ResourceKey<PlacedFeature> dry_lakebed_large = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("dry_lakebed_large"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.RAW_GENERATION, ModTags.HAS_LAKEBED, dry_lakebed_large);


        //ivy
        ResourceKey<PlacedFeature> ivy_patch = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("ivy_patch"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.VEGETAL_DECORATION, ModTags.HAS_IVY, ivy_patch);

        ResourceKey<PlacedFeature> dune_grass_patch = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("dune_grass_patch"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.VEGETAL_DECORATION, ModTags.HAS_DUNE_GRASS, dune_grass_patch);


        //moss
        ResourceKey<PlacedFeature> moss_patch = ResourceKey.create(Registries.PLACED_FEATURE, ImmersiveNature.res("moss_patch"));
        NaturePlatform.addFeatureToBiome(GenerationStep.Decoration.VEGETAL_DECORATION, ModTags.HAS_MOSS, moss_patch);


    }
}
