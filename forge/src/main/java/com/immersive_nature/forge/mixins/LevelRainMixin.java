package com.immersive_nature.forge.mixins;

import com.immersive_nature.integrations.forge.Weather2Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Weather2 compat: a raining Weather2 storm counts as rain for Level#isRainingAt on the server,
 * with the same sky, height and biome checks as vanilla rain (snowy biomes don't get rain).
 */
@Mixin(Level.class)
public abstract class LevelRainMixin {

    @Inject(method = "isRainingAt", at = @At("RETURN"), cancellable = true)
    private void immersiveNature$weather2Rain(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) return;
        Level self = (Level) (Object) this;
        if (!(self instanceof ServerLevel level) || !Weather2Compat.stormRainsAt(level, pos)) return;
        if (!level.canSeeSky(pos)) return;
        if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) return;
        if (level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.RAIN) cir.setReturnValue(true);
    }
}
