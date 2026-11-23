package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.DynamicTreesCompat;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dynamic Trees compat: a charred branch (left by fire) stays part of the tree it burnt on,
 * so it still connects to the living branches around it and falls with the tree.
 * Skipped when Dynamic Trees is not installed.
 */
@Pseudo
@Mixin(targets = "com.ferreusveritas.dynamictrees.block.branch.BranchBlock", remap = false)
public abstract class DTCharredBranchMixin {

    @Inject(method = "isSameTree(Lcom/ferreusveritas/dynamictrees/block/branch/BranchBlock;)Z",
            at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void immersiveNature$charredIsSameTree(@Coerce Block other, CallbackInfoReturnable<Boolean> cir) {
        if (other == null) return;
        if (DynamicTreesCompat.isCharredBranch((Block) (Object) this) || DynamicTreesCompat.isCharredBranch(other)) {
            cir.setReturnValue(true);
        }
    }

}
