package com.immersive_nature.data.block_growths.growths.builtin;

import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.data.block_growths.TickSource;
import com.immersive_nature.reg.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class CampfireSootGrowth extends BuiltinBlockGrowth {

    public CampfireSootGrowth(String name, @Nullable HolderSet<Block> owners, List<TickSource> sources, float chance) {
        super(name, owners, sources, chance);
    }

    @Override
    public void tryGrowing(BlockPos pos, BlockState state, ServerLevel level, Supplier<Holder<Biome>> biome) {
        if (!(growthChance == 1 || level.random.nextFloat() < growthChance)) return;
        //we are accessing with tag so always check if this is campfire
        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            int height = state.getValue(CampfireBlock.SIGNAL_FIRE) ? Math.max(23, CommonConfigs.SOOT_HEIGHT.get()) : CommonConfigs.SOOT_HEIGHT.get();
            FireSootGrowth.spawnSootInSmoke(level, pos, CommonConfigs.SOOT_RADIUS.get(), height);
        }
    }

}
