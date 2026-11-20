package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.BiomesOPlentyCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Immersive Nature compat for Biomes O' Plenty: the maple leaf pile features of the Seasonal Forest
 * place Immersive Nature's leaf piles of the same leaves instead of Biomes O' Plenty's own piles.
 * Skipped when Biomes O' Plenty is not installed.
 */
@Pseudo
@Mixin(targets = {
        "biomesoplenty.worldgen.feature.misc.RedMapleLeafPileFeature",
        "biomesoplenty.worldgen.feature.misc.OrangeMapleLeafPileFeature",
        "biomesoplenty.worldgen.feature.misc.YellowMapleLeafPileFeature"
}, remap = false)
public abstract class BOPLeafPileFeatureMixin {

    // Feature#place -> WorldGenLevel#setBlock
    @Redirect(method = "m_142674_(Lnet/minecraft/world/level/levelgen/feature/FeaturePlaceContext;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/WorldGenLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            remap = false, require = 0)
    private boolean immersiveNature$ourLeafPiles(WorldGenLevel level, BlockPos pos, BlockState state, int flags) {
        return level.setBlock(pos, BiomesOPlentyCompat.replaceLeafPile(state, level.getRandom()), flags);
    }
}
