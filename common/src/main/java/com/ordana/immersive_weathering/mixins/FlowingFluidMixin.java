package com.ordana.immersive_weathering.mixins;

import com.ordana.immersive_weathering.integration.DynamicTreesCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Immersive Nature: water never flows into the Dynamic Trees root block under a mangrove trunk.
 * The block is thinner than a full block and water kept inside it was drawn as a black box,
 * so the water stays in the blocks around it instead.
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "canHoldFluid", at = @At("HEAD"), cancellable = true)
    private void immersiveNature$noWaterInMangroveRoots(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (DynamicTreesCompat.isAerialRooty(state.getBlock())) cir.setReturnValue(false);
    }
}
