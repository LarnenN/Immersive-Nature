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
 * Lit furnaces, smokers and blast furnaces leave soot where their smoke goes: on the wall right above
 * the opening and on the ceiling above the front.
 */
public class FurnaceSootGrowth extends BuiltinBlockGrowth {

    public FurnaceSootGrowth(String name, @Nullable HolderSet<Block> owners, List<TickSource> sources, float chance) {
        super(name, owners, sources, chance);
    }

    @Override
    public void tryGrowing(BlockPos pos, BlockState state, ServerLevel level, Supplier<Holder<Biome>> biome) {
        if (!(state.getBlock() instanceof AbstractFurnaceBlock) || !state.getValue(AbstractFurnaceBlock.LIT)) return;
        double chance = CommonConfigs.FURNACE_SOOT_CHANCE.get() * growthChance;
        if (chance <= 0 || level.random.nextFloat() >= chance) return;

        Direction facing = state.getValue(AbstractFurnaceBlock.FACING);
        BlockPos front = pos.relative(facing);
        if (level.random.nextBoolean()) {
            // smoke rises from the opening and sticks to the ceiling
            FireSootGrowth.spawnSootAboveFire(level, front, 4);
            return;
        }
        // soot on the wall right above the opening
        BlockPos sootPos = front.above();
        Direction toWall = facing.getOpposite();
        BlockPos wall = sootPos.relative(toWall);
        if (!Block.isFaceFull(level.getBlockState(wall).getCollisionShape(level, wall), facing)) return;
        BlockState current = level.getBlockState(sootPos);
        var side = PipeBlock.PROPERTY_BY_DIRECTION.get(toWall);
        if (current.is(ModBlocks.SOOT.get())) {
            if (!current.getValue(side)) level.setBlock(sootPos, current.setValue(side, true), Block.UPDATE_CLIENTS);
        } else if (current.isAir()) {
            level.setBlock(sootPos, ModBlocks.SOOT.get().defaultBlockState().setValue(side, true), Block.UPDATE_CLIENTS);
        }
    }
}
