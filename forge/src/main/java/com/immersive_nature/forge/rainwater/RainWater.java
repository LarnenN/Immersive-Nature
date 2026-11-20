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
        if (!(player.level() instanceof ServerLevel level) || !level.isRaining()) return;
        if (level.dimension() != Level.OVERWORLD && !CommonConfigs.RAIN_WATER_OTHER_DIMENSIONS.get()) return;
        if (player.getPersistentData().getBoolean(DISABLED_TAG)) return;

        RandomSource random = level.random;
        if (random.nextInt(CommonConfigs.RAIN_WATER_CHANCE.get()) != 0) return;

        int radius = CommonConfigs.RAIN_WATER_RADIUS.get();
        int x = player.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
        int z = player.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;

        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        // must actually be raining here: open sky and a rainy (not snowy or dry) biome
        if (!level.isRainingAt(pos)) return;
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
