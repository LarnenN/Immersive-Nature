package com.immersive_nature.erosion;

import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.util.BlockRules;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;


/**
 * Immersive Nature: water standing on soil slowly soaks into it.
 * Grass under water turns into coarse dirt, dirt into mud (configurable).
 * With Flowing Fluids each soak uses up one water level, so puddles dry into the ground.
 */
public final class WaterSoaking {

    private static final boolean FLOWING_FLUIDS = PlatHelper.isModLoaded("flowing_fluids");

    private static final BlockRules RULES = new BlockRules("Water soaking", () -> CommonConfigs.WATER_SOAKING_TRANSFORMATIONS.get(), 0.05);

    private WaterSoaking() {
    }

    public static void tick(BlockState water, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!CommonConfigs.WATER_SOAKING.get()) return;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (!below.getFluidState().isEmpty()) return;

        // Each rule has its own chance; one roll decides which result (if any) happens
        double roll = random.nextDouble();
        double sum = 0;
        Block result = null;
        for (BlockRules.Rule rule : RULES.get()) {
            if (!rule.matches(below) || below.is(rule.result())) continue;
            sum += rule.chance();
            if (roll < sum) {
                result = rule.result();
                break;
            }
        }
        if (result == null) return;

        BlockState newState = result.defaultBlockState();
        if (newState.hasProperty(SnowyDirtBlock.SNOWY) && below.hasProperty(SnowyDirtBlock.SNOWY)) {
            newState = newState.setValue(SnowyDirtBlock.SNOWY, below.getValue(SnowyDirtBlock.SNOWY));
        }
        level.setBlock(belowPos, newState, Block.UPDATE_ALL);

        if (FLOWING_FLUIDS && CommonConfigs.WATER_SOAKING_CONSUME.get()) {
            int lvl = water.getValue(LiquidBlock.LEVEL);
            if (lvl >= 8) return; // falling water, it keeps coming
            if (lvl == 7) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            } else {
                // level 0 = 8 water levels, level n = 8 - n levels: one level less
                level.setBlock(pos, water.setValue(LiquidBlock.LEVEL, lvl + 1), Block.UPDATE_ALL);
            }
        }
    }

    /** Grass dying under deep water: pick a result from the grass rules by their chances, null = keep vanilla dirt. */
    public static BlockState drownedGrassResult(BlockState grass, RandomSource random) {
        if (!CommonConfigs.WATER_SOAKING.get() || !CommonConfigs.WATER_SOAKING_GRASS_DIES_COARSE.get()) return null;
        double total = 0;
        for (BlockRules.Rule rule : RULES.get()) if (rule.matches(grass) && !grass.is(rule.result())) total += rule.chance();
        if (total <= 0) return null;
        double roll = random.nextDouble() * total;
        for (BlockRules.Rule rule : RULES.get()) {
            if (!rule.matches(grass) || grass.is(rule.result())) continue;
            roll -= rule.chance();
            if (roll < 0) return rule.result().defaultBlockState();
        }
        return null;
    }
}
