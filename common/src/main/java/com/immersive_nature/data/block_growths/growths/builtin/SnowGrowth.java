package com.immersive_nature.data.block_growths.growths.builtin;

import com.immersive_nature.blocks.snowy.Snowy;
import com.immersive_nature.data.block_growths.TickSource;
import com.immersive_nature.reg.ModTags;
import com.immersive_nature.util.TemperatureManager;
import com.immersive_nature.util.WeatheringHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SnowGrowth extends BuiltinBlockGrowth {

    protected SnowGrowth(String name, @Nullable HolderSet<Block> owners, List<TickSource> sources, float chance) {
        super(name, owners, sources, chance);
    }

    @Override
    public @Nullable Iterable<Block> getOwners() {
        return new ArrayList<>(Snowy.NORMAL_TO_SNOWY.get().keySet());
    }

    @Override
    public void tryGrowing(BlockPos pos, BlockState state, ServerLevel level,  Supplier<Holder<Biome>> biome) {
        float chance = growthChance;
        if (com.immersive_nature.util.NatureWeather.isBlizzardAt(level, pos)) chance *= com.immersive_nature.configs.CommonConfigs.STORM_WEATHERING_MULTIPLIER.get().floatValue();
        if (!(chance >= 1 || level.random.nextFloat() < chance)) return;

        if (TemperatureManager.snowGrowthCanGrowSnowyBlock(pos, biome)) {
            var snowyBlock = Snowy.getSnowy(state);
            if (snowyBlock.isPresent() && level.getBrightness(LightLayer.BLOCK, pos.above()) <= 11) {
                level.setBlockAndUpdate(pos, snowyBlock.get());
                BlockPos downPos = pos.below();
                BlockState downBlock = level.getBlockState(downPos);

                if (WeatheringHelper.isRandomWeatheringPos(downPos)) {
                    var snowyBlock2 = Snowy.getSnowy(downBlock);
                    if (downBlock.is(ModTags.DOUBLE_SNOWABLE) && snowyBlock2.isPresent()) {
                        level.setBlockAndUpdate(pos.below(), snowyBlock2.get());
                    }
                }
            }
        }
    }


}
