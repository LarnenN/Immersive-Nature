package com.ordana.immersive_weathering.mixins.forge;

import com.ordana.immersive_weathering.integration.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Immersive Nature fix for Dynamic Trees (MIT): thin mangrove aerial root blocks were drawn black,
 * because RootyBlock#getLightBlock copies the full mangrove log and blocks all light even when the
 * root block is thinner than a full block. See {@link DynamicTreesCompat#isThinAerialRoot}.
 * Skipped when Dynamic Trees is not installed.
 */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictrees.block.rooty.RootyBlock", remap = false)
public abstract class DTRootyLightMixin {

    // getLightBlock
    @Inject(method = "m_7753_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$thinRootsLetLightIn(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (DynamicTreesCompat.isThinAerialRoot(state)) cir.setReturnValue(1);
    }
}
