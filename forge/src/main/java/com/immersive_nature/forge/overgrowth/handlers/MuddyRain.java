// Ported from Overgrowth by ldtteam (https://github.com/ldtteam/Overgrowth), GPL-3.0. Modified for Immersive Nature.
package com.immersive_nature.forge.overgrowth.handlers;

import com.immersive_nature.forge.overgrowth.Overgrowth;
import com.immersive_nature.forge.overgrowth.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.ForgeConfigSpec;


import static net.minecraft.world.level.block.Block.UPDATE_ALL_IMMEDIATE;

/**
 * Makes grass or dirt randomly to mud in the rain
 */
public class MuddyRain extends AbstractTransformationHandler
{
    @Override
    public ForgeConfigSpec.IntValue getMatchingSetting()
    {
        return Overgrowth.config.spreadmud;
    }

    @Override
    public boolean transforms(final BlockState state)
    {
        return state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.GRASS_BLOCK;
    }

    @Override
    public boolean ready(final long worldTick, final LevelChunk chunk)
    {
        return getCachedSetting() != 0 && chunk.getLevel().isRaining() && worldTick % getCachedSetting() == 0;
    }

    @Override
    public void transformBlock(final BlockPos relativePos, final LevelChunk chunk, final int chunkSection, final BlockState input)
    {
        final BlockPos worldPos = Utils.getWorldPos(chunk, chunkSection, relativePos);

        if (chunk.getLevel().canSeeSky(worldPos.above()))
        {
            chunk.getLevel().setBlock(worldPos, Blocks.MUD.defaultBlockState(), UPDATE_ALL_IMMEDIATE);
        }
    }
}
