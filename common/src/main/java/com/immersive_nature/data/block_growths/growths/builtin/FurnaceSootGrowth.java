package com.immersive_nature.data.block_growths.growths.builtin;

import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.data.block_growths.TickSource;
import com.immersive_nature.reg.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Lit furnaces, smokers and blast furnaces leave soot where their smoke goes: on walls and ceilings
 * in a circle around the smoke rising from the opening (config charring.soot_radius / soot_height).
 */
public class FurnaceSootGrowth extends BuiltinBlockGrowth {

    public FurnaceSootGrowth(String name, @Nullable HolderSet<Block> owners, List<TickSource> sources, float chance) {
        super(name, owners, sources, chance);
    }

    @Override
    public void tryGrowing(BlockPos pos, BlockState state, ServerLevel level, Supplier<Holder<Biome>> biome) {
        if (!(state.getBlock() instanceof AbstractFurnaceBlock) || !state.getValue(AbstractFurnaceBlock.LIT)) return;
        int attempts = FireSootGrowth.sootAttempts(level.random, CommonConfigs.FURNACE_SOOT_CHANCE.get() * growthChance);
        if (attempts == 0) return;

        // smoke comes out of the opening and rises
        BlockPos front = pos.relative(state.getValue(AbstractFurnaceBlock.FACING));
        for (int i = 0; i < attempts; i++) FireSootGrowth.spawnSootInSmoke(level, front, CommonConfigs.SOOT_RADIUS.get(), CommonConfigs.SOOT_HEIGHT.get());
    }
}
