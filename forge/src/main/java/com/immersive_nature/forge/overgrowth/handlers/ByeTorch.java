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
 * Torch die.
 */
public class ByeTorch extends AbstractTransformationHandler
{
    @Override
    public boolean transforms(final BlockState state)
    {
        return state.getBlock() == Blocks.TORCH;
    }
    @Override
    public boolean ready(final long worldTick)
    {
        return getCachedSetting() != 0 && worldTick % getCachedSetting() == 0;
    }

    @Override
    public ForgeConfigSpec.IntValue getMatchingSetting()
    {
        return Overgrowth.config.getServer().byetorch;
    }

    @Override
    public void transformBlock(final BlockPos relativePos, final LevelChunk chunk, final int chunkSection, final BlockState input)
    {
        final BlockPos worldPos = Utils.getWorldPos(chunk, chunkSection, relativePos);
        chunk.getLevel().destroyBlock(worldPos, false);
    }
}
