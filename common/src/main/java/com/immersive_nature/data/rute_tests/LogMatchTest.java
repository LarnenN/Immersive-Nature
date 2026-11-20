package com.immersive_nature.data.rute_tests;

import com.mojang.serialization.Codec;
import com.immersive_nature.reg.ModRuleTests;
import com.immersive_nature.util.WeatheringHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTestType;

public class LogMatchTest extends RuleTest {

    public static final LogMatchTest INSTANCE = new LogMatchTest();

    public static final Codec<LogMatchTest> CODEC = Codec.unit(() -> INSTANCE);

    @Override
    public boolean test(BlockState state, RandomSource random) {
        return WeatheringHelper.isLog(state);
    }

    @Override
    protected RuleTestType<LogMatchTest> getType() {
        return ModRuleTests.LOG_TEST.get();
    }

}
