package com.immersive_nature.forge.mixins;

import com.immersive_nature.forge.LegacyIdRemapper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// old worlds: block entities saved under the ids of Immersive Weathering and Snow! Real Magic
@Mixin(BlockEntity.class)
public abstract class BlockEntityLegacyIdMixin {

    @ModifyVariable(method = "loadStatic", at = @At("HEAD"), argsOnly = true)
    private static CompoundTag immersive_nature$legacyId(CompoundTag tag) {
        return LegacyIdRemapper.fixBlockEntityTag(tag);
    }
}
