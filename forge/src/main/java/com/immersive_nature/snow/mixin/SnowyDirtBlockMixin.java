package com.immersive_nature.snow.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.immersive_nature.snow.Hooks;

@Mixin(SnowyDirtBlock.class)
public class SnowyDirtBlockMixin {

	@Inject(at = @At("HEAD"), method = "isSnowySetting", cancellable = true)
	private static void srm_isSnowySetting(BlockState state, CallbackInfoReturnable<Boolean> ci) {
		ci.setReturnValue(Hooks.isSnowySetting(state));
	}

}
