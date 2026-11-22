package com.immersive_nature.forge.rainwater;

import com.mojang.brigadier.CommandDispatcher;
import com.immersive_nature.configs.CommonConfigs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import com.immersive_nature.fluids.FFFluidUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;

/**
 * Rain water: while it rains, water slowly gathers on open ground around players.
 * Immersive Nature's own implementation of the idea of the "Rain Water Fixed" mod by Link15.
 * With Flowing Fluids the water is finite, so it spreads into puddles and drains away.
 */
public final class RainWater {
    /** Per-player switch, stored in the player's persistent data. */
    private static final String DISABLED_TAG = "immersive_nature:rain_water_off";

    private RainWater() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(RainWater::onPlayerTick);
        MinecraftForge.EVENT_BUS.addListener(RainWater::onRegisterCommands);
    }

    private static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        if (!CommonConfigs.RAIN_WATER.get()) return;
        if (!(player.level() instanceof ServerLevel level) || !com.immersive_nature.util.NatureWeather.isRaining(level)) return;
        if (level.dimension() != Level.OVERWORLD && !CommonConfigs.RAIN_WATER_OTHER_DIMENSIONS.get()) return;
        if (player.getPersistentData().getBoolean(DISABLED_TAG)) return;

        RandomSource random = level.random;
        // Each try makes water with the base chance (1 in rain_water.chance) times the local amount:
        // 1 under normal rain, the storm's weather2.rain_water value under a Weather2 storm.
        // Amounts above 1 need several tries per tick (try i covers the part of the amount between i and i + 1).
        double max = Math.max(level.isRaining() ? 1 : 0, com.immersive_nature.util.NatureWeather.maxExtraRainWater.getAsDouble());
        int tries = (int) Math.ceil(max);
        int chance = CommonConfigs.RAIN_WATER_CHANCE.get();
        for (int i = 0; i < tries; i++) {
            if (random.nextInt(chance) != 0) continue;
            tryRainWater(level, player, random, i);
        }
    }

    private static void tryRainWater(ServerLevel level, ServerPlayer player, RandomSource random, int tryIndex) {
        int radius = CommonConfigs.RAIN_WATER_RADIUS.get();
        int x = player.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
        int z = player.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;

        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        // must actually be raining here: open sky and a rainy (not snowy or dry) biome
        if (!level.isRainingAt(pos)) return;
        double part = com.immersive_nature.util.NatureWeather.rainWaterAt(level, pos) - tryIndex;
        if (part <= 0 || (part < 1 && random.nextDouble() >= part)) return;
        if (!level.getBlockState(pos).isAir()) return;
        BlockState below = level.getBlockState(pos.below());
        int amount = CommonConfigs.RAIN_WATER_AMOUNT.get();

        // Rain lands on water: add the levels to it (Flowing Fluids spreads what does not fit)
        if (below.getFluidState().is(FluidTags.WATER)) {
            // rain also raises lakes, rivers and oceans, which can flood their shores (config: floods)
            if (below.getFluidState().isSource() && !CommonConfigs.RAIN_WATER_FLOODS.get()) return;
            FFFluidUtils.addAmountToFluidAtPosWithRemainderAndTrySpreadIfFull(level, pos.below(), Fluids.WATER, amount);
            return;
        }
        if (below.isAir() || !below.getFluidState().isEmpty()) return;

        // Exactly "amount" levels of water (8 = full block); it then flows by Flowing Fluids physics
        level.setBlock(pos, FFFluidUtils.getBlockForFluidByAmount(Fluids.WATER, amount), 3);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("disable_enable_rain_water")
                .requires(s -> s.hasPermission(4))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    if (!CommonConfigs.RAIN_WATER_CAN_DISABLE.get()) {
                        ctx.getSource().sendFailure(Component.translatable("immersive_nature.rain_water.cannot_disable"));
                        return 0;
                    }
                    boolean off = !player.getPersistentData().getBoolean(DISABLED_TAG);
                    player.getPersistentData().putBoolean(DISABLED_TAG, off);
                    ctx.getSource().sendSuccess(() -> Component.translatable(off ? "immersive_nature.rain_water.disabled" : "immersive_nature.rain_water.enabled"), false);
                    return 1;
                }));
    }
}
