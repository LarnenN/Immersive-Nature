package com.ordana.immersive_weathering.erosion;

import com.ordana.immersive_weathering.ImmersiveWeathering;
import com.ordana.immersive_weathering.configs.CommonConfigs;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immersive Nature: water standing on soil slowly soaks into it.
 * Grass under water turns into coarse dirt, dirt into mud (configurable).
 * With Flowing Fluids each soak uses up one water level, so puddles dry into the ground.
 */
public final class WaterSoaking {

    private static final boolean FLOWING_FLUIDS = PlatHelper.isModLoaded("flowing_fluids");

    private record Rule(Block block, TagKey<Block> tag, Block result) {
        boolean matches(BlockState state) {
            return tag != null ? state.is(tag) : state.is(block);
        }
    }

    private static List<String> parsedFrom = null;
    private static List<Rule> rules = List.of();

    private WaterSoaking() {
    }

    public static void tick(BlockState water, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!CommonConfigs.WATER_SOAKING.get()) return;
        if (random.nextDouble() >= CommonConfigs.WATER_SOAKING_CHANCE.get()) return;

        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (!below.getFluidState().isEmpty()) return;

        Block result = null;
        for (Rule rule : rules()) {
            if (rule.matches(below)) {
                result = rule.result();
                break;
            }
        }
        if (result == null || below.is(result)) return;

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

    private static List<Rule> rules() {
        List<String> lines = CommonConfigs.WATER_SOAKING_TRANSFORMATIONS.get();
        if (!Objects.equals(lines, parsedFrom)) {
            List<Rule> parsed = new ArrayList<>();
            for (String line : lines) {
                Rule rule = parse(line);
                if (rule != null) parsed.add(rule);
            }
            rules = List.copyOf(parsed);
            parsedFrom = List.copyOf(lines);
        }
        return rules;
    }

    private static Rule parse(String line) {
        String[] parts = line.split(",");
        if (parts.length != 2) {
            ImmersiveWeathering.LOGGER.warn("Water soaking: ignoring \"{}\", expected \"from,to\"", line);
            return null;
        }
        String from = parts[0].trim();
        ResourceLocation toId = ResourceLocation.tryParse(parts[1].trim());
        Block to = toId == null ? null : BuiltInRegistries.BLOCK.getOptional(toId).orElse(null);
        if (to == null) {
            ImmersiveWeathering.LOGGER.warn("Water soaking: unknown block \"{}\" in \"{}\"", parts[1].trim(), line);
            return null;
        }
        if (from.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(from.substring(1));
            if (tagId == null) return null;
            return new Rule(null, TagKey.create(Registries.BLOCK, tagId), to);
        }
        ResourceLocation fromId = ResourceLocation.tryParse(from);
        Block block = fromId == null ? null : BuiltInRegistries.BLOCK.getOptional(fromId).orElse(null);
        if (block == null) {
            ImmersiveWeathering.LOGGER.warn("Water soaking: unknown block \"{}\" in \"{}\"", from, line);
            return null;
        }
        return new Rule(block, null, to);
    }
}
