package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.DynamicTreesCompat;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Charred Dynamic Trees branches:
 * - have no leaves, so the tree would rot them away bit by bit; they stay as a burnt skeleton instead;
 * - don't burn again. A burnt branch removed by fire makes Dynamic Trees drop the whole tree above it,
 *   so a second burn would knock down the tree that just charred.
 * Skipped when Dynamic Trees is not installed.
 */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictrees.block.branch.BasicBranchBlock", remap = false)
public abstract class DTCharredRotMixin {

    @Inject(method = "checkForRot(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lcom/ferreusveritas/dynamictrees/tree/species/Species;IILnet/minecraft/util/RandomSource;FZ)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredNeverRots(CallbackInfoReturnable<Boolean> cir) {
        if (DynamicTreesCompat.isCharredBranch((Block) (Object) this)) cir.setReturnValue(false);
    }

    @Inject(method = "getFlammability(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredDoesNotBurn(CallbackInfoReturnable<Integer> cir) {
        if (DynamicTreesCompat.isCharredBranch((Block) (Object) this)) cir.setReturnValue(0);
    }

    @Inject(method = "getFireSpreadSpeed(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredDoesNotCatchFire(CallbackInfoReturnable<Integer> cir) {
        if (DynamicTreesCompat.isCharredBranch((Block) (Object) this)) cir.setReturnValue(0);
    }
}
