package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.DynamicTreesCompat;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fire burns a block either by removing it (handled through Moonlight's fire consume event in ModEvents) or by
 * replacing it with fire. The second way had no charring, and for Dynamic Trees it is the worst one: a branch
 * replaced by fire makes Dynamic Trees drop the whole tree above it as a burning falling tree. Here a branch that
 * would turn into fire can char instead, with the same chances.
 */
@Mixin(FireBlock.class)
public abstract class FireCharBranchMixin {

    @WrapOperation(method = "tryCatchFire", remap = false, require = 0,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean immersiveNature$charBranchInsteadOfFire(Level level, BlockPos pos, BlockState fire, int flags, Operation<Boolean> original) {
        if (level instanceof ServerLevel serverLevel) {
            BlockState charred = DynamicTreesCompat.tryCharBranch(serverLevel, pos, level.getBlockState(pos));
            if (charred != null) return original.call(level, pos, charred, flags);
        }
        return original.call(level, pos, fire, flags);
    }
}
