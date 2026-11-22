package com.immersive_nature.util;

import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Rain checks that also know about weather mods with their own local storms (Weather2).
 * Level#isRainingAt already includes them (see the forge LevelRainMixin); use {@link #isRaining}
 * instead of Level#isRaining as the quick "is it raining anywhere" check.
 */
public final class NatureWeather {
    /** Set by the platform when a weather mod is installed: true if one of its storms rains somewhere in the level. */
    public static Predicate<Level> extraRain = level -> false;
    /** Set by the platform: true if a weather mod storm rains or snows over this column. */
    public static java.util.function.BiPredicate<Level, net.minecraft.core.BlockPos> extraPrecipitationAt = (level, pos) -> false;

    /** Set by the platform: a weather mod sandstorm / snowstorm (blizzard) is over this column. */
    public static java.util.function.BiPredicate<Level, net.minecraft.core.BlockPos> extraSandstormAt = (level, pos) -> false;
    public static java.util.function.BiPredicate<Level, net.minecraft.core.BlockPos> extraSnowstormAt = (level, pos) -> false;

    /** Set by the platform: how much rain water a weather mod storm over this column makes (1 = normal rain), or -1 if no storm is there. */
    public static java.util.function.ToDoubleBiFunction<Level, net.minecraft.core.BlockPos> extraRainWaterAt = (level, pos) -> -1;
    /** Set by the platform: the highest rain water amount any weather mod storm can make (to know how many tries a tick needs). */
    public static java.util.function.DoubleSupplier maxExtraRainWater = () -> 0;

    private NatureWeather() {
    }

    public static boolean isRaining(Level level) {
        return level.isRaining() || extraRain.test(level);
    }

    /** A sandstorm here: rain in a sandstorm biome (desert, badlands...) or a Weather2 sandstorm. */
    public static boolean isSandstormAt(Level level, net.minecraft.core.BlockPos pos) {
        return (level.isRaining() && level.getBiome(pos).is(com.immersive_nature.reg.ModTags.HAS_SANDSTORM)) || extraSandstormAt.test(level, pos);
    }

    /** A real storm (not just weather): a thunderstorm, or a Weather2 sandstorm. Sand piles up faster then. */
    public static boolean isStrongSandstormAt(Level level, net.minecraft.core.BlockPos pos) {
        return extraSandstormAt.test(level, pos) || (level.isThundering() && level.getBiome(pos).is(com.immersive_nature.reg.ModTags.HAS_SANDSTORM));
    }

    /** A blizzard: a thunderstorm while it snows here, or a Weather2 snowstorm. Snow covers blocks faster then. */
    public static boolean isBlizzardAt(Level level, net.minecraft.core.BlockPos pos) {
        return extraSnowstormAt.test(level, pos) || (level.isThundering() && level.getBiome(pos).value().coldEnoughToSnow(pos));
    }

    /** Vanilla rain or snow, or a weather mod storm over this column (biome decides rain or snow). */
    public static boolean precipitatesAt(Level level, net.minecraft.core.BlockPos pos) {
        return level.isRaining() || extraPrecipitationAt.test(level, pos);
    }

    /** How much rain water falls on this column: 1 under vanilla rain, the storm's config value under a weather mod storm (the larger one if both), 0 if dry. */
    public static double rainWaterAt(Level level, net.minecraft.core.BlockPos pos) {
        double m = level.isRaining() ? 1 : 0;
        return Math.max(m, extraRainWaterAt.applyAsDouble(level, pos));
    }
}
