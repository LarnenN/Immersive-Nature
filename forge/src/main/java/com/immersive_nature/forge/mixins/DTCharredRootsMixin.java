package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Roots under a charred Dynamic Trees trunk keep the color of the tree that grew there instead of turning black.
 * See {@link DynamicTreesCompat#charredRootColor}. Skipped when Dynamic Trees is not installed.
 */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictrees.block.rooty.RootyBlock", remap = false)
public abstract class DTCharredRootsMixin {

    @Inject(method = "rootColor(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredRootColor(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Integer color = DynamicTreesCompat.charredRootColor(this, state, level, pos);
        if (color != null) cir.setReturnValue(color);
    }
}
