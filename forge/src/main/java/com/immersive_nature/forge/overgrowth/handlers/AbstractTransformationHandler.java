// Ported from Overgrowth by ldtteam (https://github.com/ldtteam/Overgrowth), GPL-3.0. Modified for Immersive Nature.
package com.immersive_nature.forge.overgrowth.handlers;

import com.immersive_nature.forge.overgrowth.Overgrowth;
import com.immersive_nature.forge.overgrowth.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Abstract implementation.
 */
public abstract class AbstractTransformationHandler implements ITransformationHandler
{
    /**
     * Setting cache value.
     */
    public int cachedSetting = -1;

    /**
     * Get the cached setting value.
     * @return the cached setting.
     */
    public int getCachedSetting()
    {
        if (cachedSetting == -1)
        {
            cachedSetting = getMatchingSetting().get();
        }
        return cachedSetting;
    }
}
