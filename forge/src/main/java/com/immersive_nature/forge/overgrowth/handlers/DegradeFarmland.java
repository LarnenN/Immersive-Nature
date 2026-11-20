// Ported from Overgrowth by ldtteam (https://github.com/ldtteam/Overgrowth), GPL-3.0. Modified for Immersive Nature.
package com.immersive_nature.forge.overgrowth.handlers;

import com.immersive_nature.forge.overgrowth.Overgrowth;
import com.immersive_nature.forge.overgrowth.utils.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.ForgeConfigSpec;


/**
 * Degrades farmland.
 */
public class DegradeFarmland extends AbstractTransformationHandler
{
    @Override
    public boolean transforms(final BlockState state)
    {
        return state.getBlock() == Blocks.FARMLAND;
    }

    @Override
    public boolean ready(final long worldTick)
    {
        return getCachedSetting() != 0 && worldTick % getCachedSetting() == 0;
    }

    @Override
    public ForgeConfigSpec.IntValue getMatchingSetting()
    {
        return Overgrowth.config.getServer().farmlanddegration;
    }

    @Override
    public void transformBlock(final BlockPos relativePos, final LevelChunk chunk, final int chunkSection, final BlockState input)
    {
        final BlockState upState = Utils.getBlockState(chunk, relativePos.above(), chunkSection);
        if (upState.isAir() || chunk.getLevel().getRandom().nextInt(100) < 1)
        {
            final BlockState hereState = Utils.getBlockState(chunk, relativePos, chunkSection);

            final BlockPos worldPos = Utils.getWorldPos(chunk, chunkSection, relativePos);
            FarmBlock.turnToDirt(null, hereState, chunk.getLevel(), worldPos);
        }
        else if (upState.getBlock() instanceof CropBlock)
        {
            final BlockPos worldPos = Utils.getWorldPos(chunk, chunkSection, relativePos.above());

            if (chunk.getLevel().getRandom().nextInt(100) < 10)
            {
                chunk.getLevel().destroyBlock(worldPos, true);
            }
            else
            {
                chunk.getLevel().setBlock(worldPos, upState.getBlock().defaultBlockState(), 3);
            }
        }
    }
}
