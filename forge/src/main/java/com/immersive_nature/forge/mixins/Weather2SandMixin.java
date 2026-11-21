package com.immersive_nature.forge.mixins;

import com.immersive_nature.blocks.LayerBlock;
import com.immersive_nature.integrations.forge.Weather2Compat;
import com.immersive_nature.reg.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather2 compat: sandstorms pile up Immersive Nature sand piles (1-7 layers, 8 = a sand block)
 * instead of Weather2's own sand layer. Skipped when Weather2 is not installed.
 */
@Pseudo
@Mixin(targets = "weather2.util.WeatherUtilBlock", remap = false)
public abstract class Weather2SandMixin {

    @ModifyVariable(method = "fillAgainstWallSmoothly(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/Vec3;FFFLnet/minecraft/world/level/block/Block;II)V",
            at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
    private static Block immersiveNature$fillWithOurSand(Block block) {
        return Weather2Compat.mapSandLayer(block);
    }

    @ModifyVariable(method = "trySpreadOnPos2(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/world/level/block/Block;I)I",
            at = @At("HEAD"), argsOnly = true, remap = false, require = 0)
    private static Block immersiveNature$spreadOurSand(Block block) {
        return Weather2Compat.mapSandLayer(block);
    }

    @Inject(method = "getHeightForAnyBlock(Lnet/minecraft/world/level/block/state/BlockState;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void immersiveNature$heightAny(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (immersiveNature$isOurPile(state.getBlock())) cir.setReturnValue(state.getValue(LayerBlock.LAYERS_8));
    }

    @Inject(method = "getHeightForLayeredBlock(Lnet/minecraft/world/level/block/state/BlockState;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void immersiveNature$heightLayered(BlockState state, CallbackInfoReturnable<Integer> cir) {
        if (immersiveNature$isOurPile(state.getBlock())) cir.setReturnValue(state.getValue(LayerBlock.LAYERS_8));
    }

    @Inject(method = "setBlockWithLayerState(Lnet/minecraft/world/level/block/Block;I)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void immersiveNature$layerState(Block block, int height, CallbackInfoReturnable<BlockState> cir) {
        if (!immersiveNature$isOurPile(block)) return;
        if (height >= 8) cir.setReturnValue(block == ModBlocks.RED_SAND_LAYER_BLOCK.get() ? Blocks.RED_SAND.defaultBlockState() : Blocks.SAND.defaultBlockState());
        else cir.setReturnValue(block.defaultBlockState().setValue(LayerBlock.LAYERS_8, Math.max(1, height)));
    }

    private static boolean immersiveNature$isOurPile(Block block) {
        return block == ModBlocks.SAND_LAYER_BLOCK.get() || block == ModBlocks.RED_SAND_LAYER_BLOCK.get();
    }
}
