package com.ordana.immersive_weathering.data.block_growths.area_condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.ordana.immersive_weathering.data.block_growths.growths.ConfigurableBlockGrowth;
import com.ordana.immersive_weathering.util.StrOpt;
import com.ordana.immersive_weathering.util.AreaTemplates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.Optional;

record AreaCheck(int rX, int rY, int rZ, int requiredAmount, Optional<Integer> yOffset,
                 Optional<RuleTest> mustHavePredicate,
                 Optional<RuleTest> mustNotHavePredicate,
                 Optional<HolderSet<Block>> extraIncluded) implements AreaCondition {

    public static final String NAME = "generate_if_not_too_many";
    public static final Codec<AreaCheck> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("radiusX").forGetter(AreaCheck::rX),
            Codec.INT.fieldOf("radiusY").forGetter(AreaCheck::rY),
            Codec.INT.fieldOf("radiusZ").forGetter(AreaCheck::rZ),
            Codec.INT.fieldOf("requiredAmount").forGetter(AreaCheck::requiredAmount),
            StrOpt.of(Codec.INT, "yOffset").forGetter(AreaCheck::yOffset),
            StrOpt.of(RuleTest.CODEC, "must_have").forGetter(AreaCheck::mustHavePredicate),
            StrOpt.of(RuleTest.CODEC, "must_not_have").forGetter(AreaCheck::mustNotHavePredicate),
            StrOpt.of(RegistryCodecs.homogeneousList(Registries.BLOCK), "includes").forGetter(AreaCheck::extraIncluded)
    ).apply(instance, AreaCheck::new));
    static final AreaConditionType<AreaCheck> TYPE = new AreaConditionType<>(AreaCheck.CODEC, AreaCheck.NAME);

    @Override
    public AreaConditionType<AreaCheck> getType() {
        return TYPE;
    }

    @Override
    public boolean test(BlockPos pos, Level level, ConfigurableBlockGrowth config) {
        if (yOffset.isPresent()) pos = pos.above(yOffset.get());
        // Immersive Nature: same checks in the same order as before, without building and shuffling
        // a new position list and RandomSource every time (see AreaTemplates)
        RandomSource random = AreaTemplates.randomSource(Mth.getSeed(pos));
        Scan scan = new Scan(this, level, config, random);
        AreaTemplates.forEachRandomly(pos, rX, rY, rZ, scan);
        return !scan.failed && scan.hasRequirement;
    }

    private static final class Scan implements AreaTemplates.Visitor {
        final AreaCheck check;
        final Level level;
        final ConfigurableBlockGrowth config;
        final RandomSource random;
        int count = 0;
        boolean hasRequirement;
        boolean failed = false;

        Scan(AreaCheck check, Level level, ConfigurableBlockGrowth config, RandomSource random) {
            this.check = check;
            this.level = level;
            this.config = config;
            this.random = random;
            this.hasRequirement = check.mustHavePredicate.isEmpty();
        }

        @Override
        public boolean visit(BlockPos.MutableBlockPos p) {
            BlockState state = level.getBlockState(p);
            if (config.getPossibleBlocks().contains(state.getBlock()) ||
                    (check.extraIncluded.isPresent() && state.is(check.extraIncluded.get()))) count += 1;
            if (!hasRequirement &&
                    check.mustHavePredicate.get().test(state, random)) {
                hasRequirement = true;
                // if -1 means it can accept any number so we exit early
                if (check.requiredAmount == -1) {
                    return true;
                }
            } else if (check.mustNotHavePredicate.isPresent() && check.mustNotHavePredicate.get().test(state, random)) {
                failed = true;
                return true;
            }
            if (count >= check.requiredAmount) {
                failed = true;
                return true;
            }
            return false;
        }
    }

    @Override
    public int getMaxRange() {
        return Math.max(rX, Math.max(rY, rZ));
    }

}
