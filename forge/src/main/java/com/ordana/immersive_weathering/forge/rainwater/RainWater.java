package com.ordana.immersive_weathering.forge.rainwater;

import com.mojang.brigadier.CommandDispatcher;
import com.ordana.immersive_weathering.configs.CommonConfigs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
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

        // A puddle that is not full yet: rain adds water levels to it until it is a full block
        if (below.getBlock() == Blocks.WATER) {
            int lvl = below.getValue(LiquidBlock.LEVEL);
            if (lvl == 0 || lvl >= 8) return; // already full, or falling water
            int levels = Math.min(8, (8 - lvl) + amount);
            level.setBlock(pos.below(), below.setValue(LiquidBlock.LEVEL, levels >= 8 ? 0 : 8 - levels), 3);
            return;
        }
        if (below.isAir() || !below.getFluidState().isEmpty()) return;

        BlockState water = Blocks.WATER.defaultBlockState();
        if (amount < 8) {
            // LiquidBlock level 8 - amount is a flowing water block holding that many levels
            water = water.setValue(LiquidBlock.LEVEL, 8 - amount);
        }
        level.setBlock(pos, water, 3);
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
                        ctx.getSource().sendFailure(Component.literal("Rain water can't be switched off on this server"));
                        return 0;
                    }
                    boolean off = !player.getPersistentData().getBoolean(DISABLED_TAG);
                    player.getPersistentData().putBoolean(DISABLED_TAG, off);
                    ctx.getSource().sendSuccess(() -> Component.literal(off ? "Rain water disabled" : "Rain water enabled"), false);
                    return 1;
                }));
    }
}
