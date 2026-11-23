package com.immersive_nature.data.block_growths.growths.builtin;

import com.immersive_nature.configs.CommonConfigs;
import com.immersive_nature.data.block_growths.TickSource;
import com.immersive_nature.reg.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class FireSootGrowth extends BuiltinBlockGrowth {

    public FireSootGrowth(String name, @Nullable HolderSet<Block> owners, List<TickSource> sources, float chance) {
        super(name, owners, sources, chance);
    }

    @Override
    public void tryGrowing(BlockPos pos, BlockState state, ServerLevel level, Supplier<Holder<Biome>> biome) {
        if (sootAttempts(level.random, growthChance) == 0) return;

        if (level.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            spawnSootAboveFire(level, pos, 6);
        }
    }

    /**
     * How many times to try to leave soot this tick: on average chance x charring.soot_multiplier
     * (the whole part every time, the rest as a chance).
     */
    public static int sootAttempts(net.minecraft.util.RandomSource random, double chance) {
        double expected = chance * CommonConfigs.SOOT_MULTIPLIER.get();
        if (expected <= 0) return 0;
        int n = (int) expected;
        if (random.nextDouble() < expected - n) n++;
        return n;
    }

    public static void spawnSootAboveFire(ServerLevel level, BlockPos pos, int smokeHeight) {
        BlockPos sootPos = pos;
        for (int i = 0; i < smokeHeight; i++) {
            sootPos = sootPos.above();
            BlockState above = level.getBlockState(sootPos.above());
            if (Block.isFaceFull(above.getCollisionShape(level, sootPos.above()), Direction.DOWN)) {
                if (level.getBlockState(sootPos).isAir()) {
                    level.setBlock(sootPos, ModBlocks.SOOT.get().defaultBlockState().setValue(BlockStateProperties.UP, true), Block.UPDATE_CLIENTS);
                }
                return;
            }
        }
    }

    /**
     * Soot left by smoke rising from origin (a campfire, or the opening of a furnace): on a wall or a ceiling
     * somewhere inside a circle of the given radius around the smoke column, up to maxHeight blocks above origin.
     * The smoke stops at the first ceiling straight above origin, so no soot ends up above a roof.
     */
    public static void spawnSootInSmoke(ServerLevel level, BlockPos origin, int radius, int maxHeight) {
        var random = level.random;
        // how high the smoke rises: up to the first solid ceiling above the origin
        int limit = 0;
        for (int y = 1; y <= maxHeight; y++) {
            BlockPos p = origin.above(y);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && !s.is(ModBlocks.SOOT.get())) break;
            limit = y;
        }
        if (limit == 0) return;
        int dx, dz;
        do {
            dx = random.nextInt(radius * 2 + 1) - radius;
            dz = random.nextInt(radius * 2 + 1) - radius;
        } while (dx * dx + dz * dz > radius * radius);
        BlockPos pos = origin.offset(dx, 1 + random.nextInt(limit), dz);
        if (!canHoldSoot(level.getBlockState(pos))) return;

        Direction face = randomFullFace(level, pos, random);
        if (face == null) {
            // in the open air: the smoke rises along this column to the ceiling, if there is one in range
            for (int y = pos.getY() + 1; y <= origin.getY() + limit; y++) {
                BlockPos p = new BlockPos(pos.getX(), y, pos.getZ());
                if (!canHoldSoot(level.getBlockState(p))) return;
                if (Block.isFaceFull(level.getBlockState(p.above()).getCollisionShape(level, p.above()), Direction.DOWN)) {
                    pos = p;
                    face = Direction.UP;
                    break;
                }
            }
            if (face == null) return;
        }
        BlockState current = level.getBlockState(pos);
        var side = net.minecraft.world.level.block.PipeBlock.PROPERTY_BY_DIRECTION.get(face);
        if (current.is(ModBlocks.SOOT.get())) {
            if (!current.getValue(side)) level.setBlock(pos, current.setValue(side, true), Block.UPDATE_CLIENTS);
        } else {
            level.setBlock(pos, ModBlocks.SOOT.get().defaultBlockState().setValue(side, true), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean canHoldSoot(BlockState state) {
        return state.isAir() || state.is(ModBlocks.SOOT.get());
    }

    /** A random side of pos (ceiling or wall, not floor) that has a full block face to stick to, or null. */
    @Nullable
    private static Direction randomFullFace(ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        Direction[] dirs = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        int start = random.nextInt(dirs.length);
        for (int i = 0; i < dirs.length; i++) {
            Direction d = dirs[(start + i) % dirs.length];
            BlockPos n = pos.relative(d);
            if (Block.isFaceFull(level.getBlockState(n).getCollisionShape(level, n), d.getOpposite())) return d;
        }
        return null;
    }
}
